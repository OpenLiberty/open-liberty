/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.csiv2.config.tss;

import java.lang.reflect.Constructor;

import org.omg.CSIIOP.AS_ContextSec;

import com.ibm.ws.transport.iiop.security.config.tss.TSSASMechConfig;

/**
 * Represents the authentication layer configuration for authenticating with LTPA token.
 * It is set as the CompoundSecMech'as_context_mech when building the IOR.
 */
public class ServerLTPAMechConfigFactory {

    private static final String IMPL_CLASS = "com.ibm.ws.security.csiv2.server.config.tss.ServerLTPAMechConfig";
    private static Constructor<?> cons = null;

    /**
     * @param name serverLTPAMechConfig implementation class name to be instanciated.
     * @param context
     */
    public static TSSASMechConfig getServerLTPAMechConfig(AS_ContextSec context) throws Exception {
        if (cons == null) {
            Class<?> implClass = Class.forName(IMPL_CLASS);
            @SuppressWarnings("rawtypes")
            Class[] types = new Class[] { AS_ContextSec.class };
            cons = implClass.getConstructor(types);
        }
        return (TSSASMechConfig) cons.newInstance(new Object[] { context });
    }
}
