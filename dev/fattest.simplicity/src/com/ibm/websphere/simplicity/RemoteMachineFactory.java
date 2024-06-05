/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.simplicity;

import java.util.Iterator;
import java.util.ServiceLoader;

public class RemoteMachineFactory {

    private static final RemoteMachineFactory factory;

    static {
        ServiceLoader<RemoteMachineFactory> loader = ServiceLoader.load(RemoteMachineFactory.class, RemoteMachineFactory.class.getClassLoader());
        Iterator<RemoteMachineFactory> iter = loader.iterator();
        if (iter.hasNext()) {
            factory = iter.next();
        } else {
            factory = new RemoteMachineFactory();
        }
    }

    static Machine createRemoteMachine(ConnectionInfo connInfo) throws Exception {
        return factory.newRemoteMachine(connInfo);
    }

    public Machine newRemoteMachine(ConnectionInfo connInfo) throws Exception {
        return new Machine(connInfo);
    }
}
