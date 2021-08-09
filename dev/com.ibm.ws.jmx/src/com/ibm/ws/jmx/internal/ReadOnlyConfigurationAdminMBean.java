/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.internal;

import java.io.IOException;

import javax.management.openmbean.TabularData;

/**
 *
 */
public interface ReadOnlyConfigurationAdminMBean {

    public String getBundleLocation(String pid) throws IOException;

    public String[][] getConfigurations(String filter) throws IOException;

    public String getFactoryPid(String pid) throws IOException;

    public String getFactoryPidForLocation(String pid, String location) throws IOException;

    public TabularData getProperties(String pid) throws IOException;

    public TabularData getPropertiesForLocation(String pid, String location) throws IOException;
}
