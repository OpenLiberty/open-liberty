/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.impl;

import java.util.Iterator;
import java.util.ServiceLoader;

import componenttest.common.apiservices.Bootstrap;

public class LibertyServerFactoryDelegate {

    private static final LibertyServerFactoryDelegate factoryDelegate;

    static {
        ServiceLoader<LibertyServerFactoryDelegate> loader = ServiceLoader.load(LibertyServerFactoryDelegate.class, LibertyServerFactoryDelegate.class.getClassLoader());
        Iterator<LibertyServerFactoryDelegate> iter = loader.iterator();
        if (iter.hasNext()) {
            factoryDelegate = iter.next();
        } else {
            factoryDelegate = new LibertyServerFactoryDelegate();
        }
    }

    static LibertyServer createLibertyServer(String serverName, Bootstrap b, boolean deleteServerDirIfExist, boolean usePreviouslyConfigured,
                                             LibertyServerFactory.WinServiceOption winServiceOption) throws Exception {
        return factoryDelegate.newLibertyServer(serverName, b, deleteServerDirIfExist, usePreviouslyConfigured, winServiceOption);
    }

    public LibertyServer newLibertyServer(String serverName, Bootstrap b, boolean deleteServerDirIfExist, boolean usePreviouslyConfigured,
                                          LibertyServerFactory.WinServiceOption winServiceOption) throws Exception {
        return new LibertyServer(serverName, b, deleteServerDirIfExist, usePreviouslyConfigured, winServiceOption);
    }
}
