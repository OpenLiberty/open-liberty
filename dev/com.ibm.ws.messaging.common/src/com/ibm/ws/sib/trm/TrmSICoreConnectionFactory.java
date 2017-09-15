/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/*
 * The Topology Routing & Management client sub-component.
 *
 * This is the factory class which is used to obtain a new instance of a
 * SICoreConnection object.
 */

package com.ibm.ws.sib.trm;

import com.ibm.wsspi.sib.core.SICoreConnectionFactory;

public abstract class TrmSICoreConnectionFactory implements SICoreConnectionFactory {

    private static final String className = TrmSICoreConnectionFactory.class.getName();
    private static final String CLIENT_FACTORY_IMPL = "com.ibm.ws.sib.trm.client.TrmSICoreConnectionFactoryImpl";
    private static TrmSICoreConnectionFactory instance = null;

    static {

        try {
            Class cls = Class.forName(CLIENT_FACTORY_IMPL);
            instance = (TrmSICoreConnectionFactory) cls.newInstance();
        } catch (Exception e) {

        }
    }

    public static TrmSICoreConnectionFactory getInstance() {
        return instance;
    }
}
