/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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
package com.ibm.ws.webserver.plugin.runtime.interfaces;

/**
 *
 */
public interface PluginConfigRequester {

    String OBJECT_NAME = "WebSphere:feature=PluginUtility,name=PluginConfigRequester";

    public boolean generateClusterPlugin(String cluster);

    public boolean generateAppServerPlugin();

}
