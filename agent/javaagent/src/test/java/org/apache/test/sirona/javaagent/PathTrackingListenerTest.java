/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.test.sirona.javaagent;

import org.apache.sirona.configuration.ioc.IoCs;
import org.apache.sirona.javaagent.AgentArgs;
import org.apache.sirona.javaagent.JavaAgentRunner;
import org.apache.sirona.pathtracking.test.ExtendedInMemoryPathTrackingDataStore;
import org.apache.sirona.store.DataStoreFactory;
import org.apache.sirona.tracking.PathTracker;
import org.apache.sirona.tracking.PathTrackingEntry;
import org.apache.sirona.tracking.PathTrackingInvocationListener;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
@RunWith(JavaAgentRunner.class)
public class PathTrackingListenerTest
{

    @Test
    @AgentArgs(value = "",
               sysProps = "project.build.directory=${project.build.directory}|sirona.agent.debug=${sirona.agent.debug}|org.apache.sirona.configuration.sirona.properties=${project.build.directory}/test-classes/pathtracking/sirona.properties")
    public void simpleTest()
        throws Exception
    {

        App app = new App();
        app.beer();

        DataStoreFactory dataStoreFactory = IoCs.findOrCreateInstance( DataStoreFactory.class );

        ExtendedInMemoryPathTrackingDataStore ptds =
            ExtendedInMemoryPathTrackingDataStore.class.cast( dataStoreFactory.getPathTrackingDataStore() );

        Map<String, Set<PathTrackingEntry>> all = ptds.retrieveAll();

        Assert.assertTrue( !all.isEmpty() );

        // test only one Thread so only one trackingId
        Assert.assertEquals( 1, all.size() );

        List<PathTrackingEntry> entries = new ArrayList<PathTrackingEntry>( all.values().iterator().next() );

        // so we have 4 entries constructor is ignored!

        Assert.assertEquals( 4, entries.size() );

        for ( PathTrackingEntry entry : entries )
        {
            System.out.println( "entry:" + entry );
        }

        PathTrackingEntry entry = entries.get( 0 );

        Assert.assertEquals( "beer", entry.getMethodName() );

        Assert.assertEquals( "org.apache.test.sirona.javaagent.App", entry.getClassName() );

        Assert.assertEquals( 1, entry.getLevel() );

        entry = entries.get( 1 );

        Assert.assertEquals( "foo", entry.getMethodName() );

        Assert.assertEquals( "org.apache.test.sirona.javaagent.App", entry.getClassName() );

        Assert.assertEquals( 2, entry.getLevel() );

        // there is Thread.sleep( 500 ) so we can be sure a minimum for that

        Assert.assertTrue( entry.getExecutionTime() >= 500 * 1000000 );

        entry = entries.get( 2 );

        Assert.assertEquals( "pub", entry.getMethodName() );

        Assert.assertEquals( "org.apache.test.sirona.javaagent.App", entry.getClassName() );

        Assert.assertEquals( 3, entry.getLevel() );

        Assert.assertTrue( entry.getExecutionTime() >= 100 * 1000000 );

        entry = entries.get( 3 );

        Assert.assertEquals( "bar", entry.getMethodName() );

        Assert.assertEquals( "org.apache.test.sirona.javaagent.App", entry.getClassName() );

        Assert.assertEquals( 4, entry.getLevel() );

        Assert.assertTrue( entry.getExecutionTime() >= 300 * 1000000 );

        // we have only one here
        PathTrackingInvocationListener listener = PathTracker.getPathTrackingInvocationListeners()[0];

        MockPathTrackingInvocationListener mock = MockPathTrackingInvocationListener.class.cast( listener );

        System.out.println( "mock.startPathCallCount: " + mock.startPathCallCount );

        Assert.assertEquals( 1, mock.startPathCallCount );

        Assert.assertEquals( 1, mock.endPathCallCount );

        Assert.assertEquals( 3, mock.enterMethodCallCount );

        Assert.assertEquals( 3, mock.exitMethodCallCount );


    }

}