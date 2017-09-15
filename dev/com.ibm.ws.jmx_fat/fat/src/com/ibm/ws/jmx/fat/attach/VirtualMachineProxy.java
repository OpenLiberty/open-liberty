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
package com.ibm.ws.jmx.fat.attach;

import java.io.IOException;
import java.util.Properties;

/**
 *
 */
public interface VirtualMachineProxy {

    public void detach() throws IOException;

    public String id();

    public Properties getAgentProperties() throws IOException;

    public Properties getSystemProperties() throws IOException;

    public void loadAgent(String agent) throws IOException;

    public void loadAgent(String agent, String options) throws IOException;

    public void loadAgentLibrary(String agentLibrary) throws IOException;

    public void loadAgentLibrary(String agentLibrary, String options) throws IOException;

    public void loadAgentPath(String agentPath) throws IOException;

    public void loadAgentPath(String agentPath, String options) throws IOException;

    public String toString();

}
