/*
 * Copyright (c) 2007-2011 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
 *
 * Cascading is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cascading is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cascading.  If not, see <http://www.gnu.org/licenses/>.
 */

package cascading.stats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cascading.flow.Flow;
import cascading.management.ClientState;


/** Class FlowStats collects {@link Flow} specific statistics. */
public class FlowStats extends CascadingStats
  {
  final String appID;
  final String appName;
  final String appVersion;
  final String flowID;
  final List<StepStats> stepStatsList = new ArrayList<StepStats>();
  private FlowInfo flowInfo;

  public FlowStats( Flow flow, ClientState clientState )
    {
    super( flow.getName(), clientState );

    this.appID = flow.getAppID();
    this.appName = flow.getAppName();
    this.appVersion = flow.getAppVersion();
    this.flowID = flow.getID();
    this.flowInfo = new FlowInfo( flow );
    }

  public String getAppID()
    {
    return appID;
    }

  public String getAppName()
    {
    return appName;
    }

  public String getAppVersion()
    {
    return appVersion;
    }

  @Override
  public Object getID()
    {
    return flowID;
    }

  @Override
  public void markPending()
    {
    super.markPending();
    clientState.record( flowInfo );
    }

  @Override
  public void markRunning()
    {
    super.markRunning();
    clientState.record( flowInfo );
    }

  @Override
  public void markSuccessful()
    {
    super.markSuccessful();
    clientState.record( flowInfo );
    }

  @Override
  public void markSkipped()
    {
    super.markSkipped();
    clientState.record( flowInfo );
    }

  @Override
  public void markStopped()
    {
    super.markStopped();
    clientState.record( flowInfo );
    }

  @Override
  public void markFailed( Throwable throwable )
    {
    super.markFailed( throwable );
    clientState.record( flowInfo );
    }

  public void addStepStats( StepStats stepStats )
    {
    stepStatsList.add( stepStats );
    }

  /**
   * Method getStepStats returns the stepStats owned by this FlowStats.
   *
   * @return the stepStats (type List<StepStats>) of this FlowStats object.
   */
  public List<StepStats> getStepStats()
    {
    return stepStatsList;
    }

  /**
   * Method getStepsCount returns the number of steps this Flow executed.
   *
   * @return the stepsCount (type int) of this FlowStats object.
   */
  public int getStepsCount()
    {
    return stepStatsList.size();
    }

  @Override
  public Collection<String> getCounterGroups()
    {
    Set<String> results = new HashSet<String>();

    for( StepStats stepStats : stepStatsList )
      results.addAll( stepStats.getCounterGroups() );

    return results;
    }

  @Override
  public Collection<String> getCounterGroupsMatching( String regex )
    {
    Set<String> results = new HashSet<String>();

    for( StepStats stepStats : stepStatsList )
      results.addAll( stepStats.getCounterGroupsMatching( regex ) );

    return results;
    }

  @Override
  public Collection<String> getCountersFor( String group )
    {
    Set<String> results = new HashSet<String>();

    for( StepStats stepStats : stepStatsList )
      results.addAll( stepStats.getCountersFor( group ) );

    return results;
    }

  @Override
  public long getCounterValue( Enum counter )
    {
    long value = 0;

    for( StepStats stepStats : stepStatsList )
      value += stepStats.getCounterValue( counter );

    return value;
    }

  @Override
  public long getCounterValue( String group, String counter )
    {
    long value = 0;

    for( StepStats stepStats : stepStatsList )
      value += stepStats.getCounterValue( group, counter );

    return value;
    }

  @Override
  public void captureDetail()
    {
    for( StepStats stepStats : stepStatsList )
      stepStats.captureDetail();
    }

  @Override
  public Collection getChildren()
    {
    return getStepStats();
    }

  @Override
  protected String getStatsString()
    {
    return super.getStatsString() + ", stepsCount=" + getStepsCount();
    }

  @Override
  public String toString()
    {
    return "Flow{" + getStatsString() + '}';
    }
  }
