/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.monitor;

import java.util.Map;

public interface MonitorManager {

    public boolean registerMonitor(Object monitor);

    public boolean registerMonitor(Object monitor, Map<String, Object> config);

    public boolean unregisterMonitor(Object monitor);

    //RTCD 89497-Update for non Excluded classes list
    public void updateNonExcludedClassesSet(String className);

    // public <T> T getAttribute(String monitorName, String attributeName);

    //    public boolean enableMonitor(String name);
    //
    //    public boolean isEnabled(String name);
    //
    //    public boolean disableMonitor(String name);

}
