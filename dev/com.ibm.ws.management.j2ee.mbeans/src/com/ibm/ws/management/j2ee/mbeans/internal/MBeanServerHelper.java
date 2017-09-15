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
package com.ibm.ws.management.j2ee.mbeans.internal;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 *
 */
public class MBeanServerHelper {

    private static final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    public static List<String> queryObjectName(final ObjectName objectName) {
        //Query the set of object instances
        Set<ObjectName> objectNameSet = mbeanServer.queryNames(objectName, null);

        final List<String> returnedObjectNames = new ArrayList<String>();
        for (ObjectName server : objectNameSet) {
            returnedObjectNames.add(server.toString());
        }

        return returnedObjectNames;
    }
}
