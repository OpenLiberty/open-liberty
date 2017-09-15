/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.osgi.mbeans;

public interface GeneratePluginConfig {
	public static final String DEFAULT_VIRTUAL_HOST_GROUP = "default_host";
        public static final String DEFAULT_NODE_NAME = "default_node";
	
	public void generateDefaultPluginConfig();
	
	public void generatePluginConfig(String root, String name);

	public long getConnectTimeout();
	
	public long getIoTimeout();
	
	public boolean getExtendedHandshake();
	
	public boolean getWaitForContinue();
}
