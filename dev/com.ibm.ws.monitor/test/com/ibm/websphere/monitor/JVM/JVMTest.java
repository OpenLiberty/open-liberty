/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.monitor.JVM;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.monitors.helper.JvmMonitorHelper;

public class JVMTest {

    private static JvmMonitorHelper _jvmHelper;

    @Before
    public void setup() {
        _jvmHelper = new JvmMonitorHelper();
    }

    @After
    public void tearDown() {
        _jvmHelper = null;
    }

    @Test
    public void testJVMData() {
        assertTrue("JVM UpTime is showing incorrect value", (_jvmHelper.getUptime() > 0));
        assertTrue("JVM Total Memory Size is showing incorrect value", (_jvmHelper.getCommitedHeapMemoryUsage() > 0));
        assertTrue("JVM UsedMemory is showing incorrect value", (_jvmHelper.getUsedHeapMemoryUsage() > 0));

        //For test purpose we do System.gc() and make sure GC Count to be >0;
        System.gc();

        //If collection count is undefined for Garbage collector, we might get -1 from getGCCollectionCount
        //Will ignore those cases
        long l = _jvmHelper.getGCCollectionCount();
        System.out.println("GC Collection Count Reported was " + l);
        if (l != -1) {
            assertTrue("JVM GC Count is showing incorrect value", (_jvmHelper.getGCCollectionCount() > 0));
        }

        //61757:Commenting out check of Collection Time
        //We see very small value manytimes (2 or 4 mSec)
        //Also this is approx value and possible to have less than 1ms, which may report 0.        
        //Validation of GC Count is appropriate, as we do System.gc() and then check if Collection Count > 0

        //61757-Start
        //        //If collection time is undefined for Garbage collector, we might get -1 from getGCCollectionTime
        //        //Will ignore those cases        
        //        l = _jvmHelper.getGCCollectionTime();
        //        System.out.println("GC Collection Time Reported was " + l);
        //        if (l != -1) {
        //            assertTrue("JVM GC Time is showing incorrect value", (_jvmHelper.getGCCollectionTime() > 0));
        //        }
        //61757-Stop

    }

    @Test
    public void testHeapSize() {
        assertTrue("JVM HeapSize is showing incorrect value", (_jvmHelper.getCommitedHeapMemoryUsage() >= _jvmHelper.getUsedHeapMemoryUsage()));
        assertTrue("JVM Settings are showing incorrect values", (_jvmHelper.getMaxHeapMemorySettings() > _jvmHelper.getInitHeapMemorySettings()));
        assertTrue("JVM UpTime is showing incorrect value", (_jvmHelper.getUptime() > 0));
    }

}
