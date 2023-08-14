/*
 * Copyright (c) 2014,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.transport.iiop.spi;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;

public interface SubsystemFactory {

    default void register(ReadyListener listener, Map<String, Object> properties, List<IIOPEndpoint> endpoints) {
        listener.readyChanged(this, true);
    }

    default void unregister(ReadyListener listener) {
        listener.readyChanged(this, false);
    }

    default Policy getTargetPolicy(ORB orb, Map<String, Object> properties, Map<String, Object> extraConfig) throws Exception {
        return null;
    }

    default Policy getClientPolicy(ORB orb, Map<String, Object> properties) throws Exception {
        return null;
    }

    default String getInitializerClassName(boolean endpoint) {
        return null;
    }

    default void addTargetORBInitProperties(Properties initProperties, Map<String, Object> orbProperties, List<IIOPEndpoint> endpoints, Map<String, Object> extraProperties) {}

    default void addTargetORBInitArgs(Map<String, Object> targetProperties, List<String> args) {}

    default void addClientORBInitProperties(Properties initProperties, Map<String, Object> orbProperties) {}

    default void addClientORBInitArgs(Map<String, Object> clientProperties, List<String> args) {}

}
