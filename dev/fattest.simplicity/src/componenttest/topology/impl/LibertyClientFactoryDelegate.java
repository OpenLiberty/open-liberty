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

public class LibertyClientFactoryDelegate {

    private static final LibertyClientFactoryDelegate factoryDelegate;

    static {
        ServiceLoader<LibertyClientFactoryDelegate> loader = ServiceLoader.load(LibertyClientFactoryDelegate.class, LibertyClientFactoryDelegate.class.getClassLoader());
        Iterator<LibertyClientFactoryDelegate> iter = loader.iterator();
        if (iter.hasNext()) {
            factoryDelegate = iter.next();
        } else {
            factoryDelegate = new LibertyClientFactoryDelegate();
        }
    }

    static LibertyClient createLibertyClient(String clientName, Bootstrap b) throws Exception {
        return factoryDelegate.newLibertyClient(clientName, b);
    }

    public LibertyClient newLibertyClient(String clientName, Bootstrap b) throws Exception {
        return new LibertyClient(clientName, b);
    }
}
