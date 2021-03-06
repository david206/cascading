/*
 * Copyright (c) 2007-2013 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cascading.platform.hadoop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import cascading.flow.FlowConnector;
import cascading.flow.FlowProcess;
import cascading.flow.FlowProps;
import cascading.flow.FlowSession;
import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.flow.hadoop.HadoopFlowProcess;
import cascading.flow.hadoop.planner.HadoopPlanner;
import cascading.platform.TestPlatform;
import cascading.scheme.Scheme;
import cascading.scheme.hadoop.TextDelimited;
import cascading.scheme.hadoop.TextLine;
import cascading.tap.SinkMode;
import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tap.hadoop.TemplateTap;
import cascading.tap.hadoop.util.Hadoop18TapUtil;
import cascading.tuple.Fields;
import cascading.util.Util;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MiniMRCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class HadoopPlatform is automatically loaded and injected into a {@link cascading.PlatformTestCase} instance
 * so that all *PlatformTest classes can be tested against Apache Hadoop.
 */
public class HadoopPlatform extends TestPlatform
  {
  private static final Logger LOG = LoggerFactory.getLogger( HadoopPlatform.class );

  public transient static MiniDFSCluster dfs;
  public transient static FileSystem fileSys;
  public transient static MiniMRCluster mr;
  public transient static JobConf jobConf;
  public transient static Map<Object, Object> properties = new HashMap<Object, Object>();

  public int numMapTasks = 4;
  public int numReduceTasks = 1;

  private String logger;

  public HadoopPlatform()
    {
    logger = System.getProperty( "log4j.logger" );
    }

  @Override
  public boolean isMapReduce()
    {
    return true;
    }

  public void setNumMapTasks( int numMapTasks )
    {
    if( numMapTasks > 0 )
      this.numMapTasks = numMapTasks;
    }

  public void setNumReduceTasks( int numReduceTasks )
    {
    if( numReduceTasks > 0 )
      this.numReduceTasks = numReduceTasks;
    }

  @Override
  public synchronized void setUp() throws IOException
    {
    if( jobConf != null )
      return;

    if( !isUseCluster() )
      {
      LOG.info( "not using cluster" );
      jobConf = new JobConf();
      }
    else
      {
      LOG.info( "using cluster" );

      if( Util.isEmpty( System.getProperty( "hadoop.log.dir" ) ) )
        System.setProperty( "hadoop.log.dir", "build/test/log" );

      if( Util.isEmpty( System.getProperty( "hadoop.tmp.dir" ) ) )
        System.setProperty( "hadoop.tmp.dir", "build/test/tmp" );

      new File( System.getProperty( "hadoop.log.dir" ) ).mkdirs();

      JobConf conf = new JobConf();

      conf.setInt( "mapred.job.reuse.jvm.num.tasks", -1 );

      dfs = new MiniDFSCluster( conf, 4, true, null );
      fileSys = dfs.getFileSystem();
      mr = new MiniMRCluster( 4, fileSys.getUri().toString(), 1, null, null, conf );

      jobConf = mr.createJobConf();

      jobConf.set( "mapred.child.java.opts", "-Xmx512m" );
      jobConf.setInt( "mapred.job.reuse.jvm.num.tasks", -1 );
      jobConf.setInt( "jobclient.completion.poll.interval", 50 );
      jobConf.setInt( "jobclient.progress.monitor.poll.interval", 50 );
      jobConf.setMapSpeculativeExecution( false );
      jobConf.setReduceSpeculativeExecution( false );
      }

    jobConf.setNumMapTasks( numMapTasks );
    jobConf.setNumReduceTasks( numReduceTasks );

    if( logger != null )
      properties.put( "log4j.logger", logger );

    FlowProps.setJobPollingInterval( properties, 10 ); // should speed up tests
    HadoopPlanner.copyJobConf( properties, jobConf );
    }

  @Override
  public Map<Object, Object> getProperties()
    {
    return new HashMap<Object, Object>( properties );
    }

  @Override
  public void tearDown()
    {
    }

  public JobConf getJobConf()
    {
    return new JobConf( jobConf );
    }

  @Override
  public FlowConnector getFlowConnector( Map<Object, Object> properties )
    {
    return new HadoopFlowConnector( properties );
    }

  @Override
  public FlowProcess getFlowProcess()
    {
    return new HadoopFlowProcess( FlowSession.NULL, getJobConf(), true );
    }

  @Override
  public void copyFromLocal( String inputFile ) throws IOException
    {
    if( !new File( inputFile ).exists() )
      throw new FileNotFoundException( "data file not found: " + inputFile );

    if( !isUseCluster() )
      return;

    Path path = new Path( inputFile );

    if( !fileSys.exists( path ) )
      FileUtil.copy( new File( inputFile ), fileSys, path, false, jobConf );
    }

  @Override
  public void copyToLocal( String outputFile ) throws IOException
    {
    if( !isUseCluster() )
      return;

    Path path = new Path( outputFile );

    if( !fileSys.exists( path ) )
      throw new FileNotFoundException( "data file not found: " + outputFile );

    if( new File( outputFile ).exists() )
      new File( outputFile ).delete();

    FileUtil.copy( fileSys, path, new File( outputFile ), false, jobConf );
    }

  @Override
  public boolean remoteExists( String outputFile ) throws IOException
    {
    return fileSys.exists( new Path( outputFile ) );
    }

  @Override
  public Tap getTap( Scheme scheme, String filename, SinkMode mode )
    {
    return new Hfs( scheme, filename, mode );
    }

  @Override
  public Tap getTextFile( Fields sourceFields, Fields sinkFields, String filename, SinkMode mode )
    {
    if( sourceFields == null )
      return new Hfs( new TextLine(), filename, mode );

    return new Hfs( new TextLine( sourceFields, sinkFields ), filename, mode );
    }

  @Override
  public Tap getDelimitedFile( Fields fields, boolean hasHeader, String delimiter, String quote, Class[] types, String filename, SinkMode mode )
    {
    return new Hfs( new TextDelimited( fields, hasHeader, delimiter, quote, types ), filename, mode );
    }

  @Override
  public Tap getDelimitedFile( Fields fields, boolean skipHeader, boolean writeHeader, String delimiter, String quote, Class[] types, String filename, SinkMode mode )
    {
    return new Hfs( new TextDelimited( fields, skipHeader, writeHeader, delimiter, quote, types ), filename, mode );
    }

  @Override
  public Tap getTemplateTap( Tap sink, String pathTemplate, int openThreshold )
    {
    return new TemplateTap( (Hfs) sink, pathTemplate, openThreshold );
    }

  @Override
  public Tap getTemplateTap( Tap sink, String pathTemplate, Fields fields, int openThreshold )
    {
    return new TemplateTap( (Hfs) sink, pathTemplate, fields, openThreshold );
    }

  @Override
  public Scheme getTestConfigDefScheme()
    {
    return new HadoopConfigDefScheme( new Fields( "line" ) );
    }

  @Override
  public Scheme getTestFailScheme()
    {
    return new HadoopFailScheme( new Fields( "line" ) );
    }

  @Override
  public Comparator getLongComparator( boolean reverseSort )
    {
    return new TestLongComparator( reverseSort );
    }

  @Override
  public Comparator getStringComparator( boolean reverseSort )
    {
    return new TestStringComparator( reverseSort );
    }

  @Override
  public String getHiddenTemporaryPath()
    {
    return Hadoop18TapUtil.TEMPORARY_PATH;
    }
  }