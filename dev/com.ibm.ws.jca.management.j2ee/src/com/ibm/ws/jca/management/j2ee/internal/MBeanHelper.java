/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.management.j2ee.internal;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * MBeanHelper offers a collection of static methods that are used by most MBeans mainly for cleanup,
 * testing & logging tasks.
 */
public class MBeanHelper {

    /////////////////////////////////// Variables used in tracing. ///////////////////////////////////
    private static final TraceComponent tc = Tr.register(MBeanHelper.class, "RRA");
    private static final String className = "MBeanHelper : ";

    ///////////////////////////////////// The constructor method /////////////////////////////////////
    protected MBeanHelper() {}

    ///////////////////////////////////////// Helper Methods /////////////////////////////////////////
    /**
     * toObnString takes in any string value and replaces any of the reserved ObjectName
     * chars with periods (.). The 4 reserved ObjectName chars are:<br>
     * : = , "
     * 
     * @param s The string to be converted to an ObjectName-safe string.
     */
    public static String toObnString(String s) {
        if (s != null && !s.isEmpty())
            return s.replace(':', '.').replace('=', '.').replace(',', '.').replace('"', '.');
        return s;
    }

    /**
     * isMbeanExist() checks {@code MBeanServer} to see if provided MBean already exists.
     * This method should be used for testing and debugging proposes only.
     * This method should not be used in shipped code.
     * 
     * @param objName String representation of the {@code ObjectName} used to search the {@code MBeanServer}.
     * @param callingClass Optional String representation of the calling class name to be used for logging.
     * @param callingMethod Optional String representation of the calling method name to be used for logging.
     */
    public static boolean isMbeanExist(String objName, String callingClass, String callingMethod) {
        final String methodName = "isMbeanExist()";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final String cClass = (callingClass != null && !callingClass.isEmpty()) ? callingClass : "callingClass";
        final String cMethod = (callingMethod != null && !callingMethod.isEmpty()) ? callingMethod : "callingMethod";

        ObjectName obnToCompare = null;
        try {
            obnToCompare = new ObjectName(objName);
        } catch (Exception e) {
            // This path should not happen because the calling class is building the ObjectName and know it's valid.
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, cClass + ": " + cMethod + " is using " + className + ": " + methodName
                             + ": Creating ObjectName failed."
                             + "\n " + e.toString());
            return false;
        }
        Set<ObjectInstance> s = mbs.queryMBeans(obnToCompare, null);
        if (s.size() == 0) { // This is the right path that should happen.
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, cClass + ": " + cMethod + " is using " + className + ": " + methodName
                             + ": s.size(): " + s.size() + ", No duplicate MBean Found");
            return false;
        }
        if (trace && tc.isDebugEnabled())
            Tr.debug(tc, cClass + ": " + cMethod + " is using " + className + ": " + methodName
                         + ": searching for: " + objName);
        for (ObjectInstance bean : s)
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "**  Found a duplicate MBean: " + bean.getObjectName().toString());
        if (trace && tc.isDebugEnabled())
            Tr.debug(tc, cClass + ": " + cMethod + " is using " + className + ": " + methodName
                         + ": The Mbean already exist in the MBean server.");
        return true;
    }

    /**
     * logLoudAndClear Log the provided text in a very distinct way making it easy to find it in the trace.log
     * This method should be used for testing and debugging proposes only.
     * This method should not be used in shipped code.
     * 
     * @param textToLog String representation of the test needed to be logged in a distinct way.
     * @param callingClass Optional String representation of the calling class name to be used for logging.
     * @param callingMethod Optional String representation of the calling method name to be used for logging.
     */
    public static void logLoudAndClear(String textToLog, String callingClass, String callingMethod) {
        final String methodName = "logLoudAndClear";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        final String cClass = (callingClass != null && !callingClass.isEmpty()) ? callingClass : "callingClass";
        final String cMethod = (callingMethod != null && !callingMethod.isEmpty()) ? callingMethod : "callingMethod";
        if (trace && tc.isDebugEnabled())
            Tr.debug(tc, "#################################################################################################"
                         + "\n #################################################################################################"
                         + "#################################################################################################"
                         + "\n #################################################################################################"
                         + "#################################################################################################"
                         + "\n #################################################################################################"
                         + "#################################################################################################"
                         + "\n #################################################################################################"
                         + "#################################################################################################"
                         + "\n #################################################################################################"
                         + "#################################################################################################"
                         + "\n #################################################################################################"
                         + "#################################################################################################"
                         + "\n #################################################################################################"
                         + "#################################################################################################"
                         + "\n #################################################################################################"
                         + "#################################################################################################"
                         + "\n "
                         + "\n "
                         + cClass + ": " + cMethod + " is using " + className + ": " + methodName + " \n "
                         + textToLog
                         + "\n "
                         + "\n "
                         + "#################################################################################################"
                         + "\n #################################################################################################"
                         + "#################################################################################################"
                         + "\n #################################################################################################"
                         + "#################################################################################################"
                         + "\n #################################################################################################"
                         + "#################################################################################################"
                         + "\n #################################################################################################"
                         + "#################################################################################################"
                         + "\n #################################################################################################"
                         + "#################################################################################################"
                         + "\n #################################################################################################"
                         + "#################################################################################################"
                         + "\n #################################################################################################"
                         + "#################################################################################################"
                         + "\n #################################################################################################"
                         + "#################################################################################################");
    }
}
