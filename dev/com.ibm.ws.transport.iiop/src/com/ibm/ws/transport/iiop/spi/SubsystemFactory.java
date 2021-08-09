/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.iiop.spi;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;

public abstract class SubsystemFactory {

    public void register(ReadyListener listener, Map<String, Object> properties, List<IIOPEndpoint> endpoints) {
        listener.readyChanged(this, true);
    }

    public void unregister(ReadyListener listener) {
        listener.readyChanged(this, false);
    }

    public Policy getTargetPolicy(ORB orb, Map<String, Object> properties, Map<String, Object> extraConfig) throws Exception {
        return null;
    }

    public Policy getClientPolicy(ORB orb, Map<String, Object> properties) throws Exception {
        return null;
    }

    public String getInitializerClassName(boolean endpoint) {
        return null;
    }

    public void addTargetORBInitProperties(Properties initProperties, Map<String, Object> orbProperties, List<IIOPEndpoint> endpoints, Map<String, Object> extraProperties) {}

    public void addTargetORBInitArgs(Map<String, Object> targetProperties, List<String> args) {}

    public void addClientORBInitProperties(Properties initProperties, Map<String, Object> orbProperties) {}

    public void addClientORBInitArgs(Map<String, Object> clientProperties, List<String> args) {}

}
