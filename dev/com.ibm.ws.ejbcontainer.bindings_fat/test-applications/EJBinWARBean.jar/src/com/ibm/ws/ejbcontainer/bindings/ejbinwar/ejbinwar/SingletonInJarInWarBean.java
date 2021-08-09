/*******************************************************************************
 * Copyright (c) 2010, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.ejbinwar.ejbinwar;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.ws.ejbcontainer.bindings.ejbinwar.intf.BasicLooseSingletonInterface;
import com.ibm.ws.ejbcontainer.bindings.ejbinwar.intf.BasicSingletonInJarInWarInterface;
import com.ibm.ws.ejbcontainer.bindings.ejbinwar.intf.BasicSingletonInJarInWarInterfaceRemote;

@Singleton
@Startup
@Local(BasicSingletonInJarInWarInterface.class)
@Remote(BasicSingletonInJarInWarInterfaceRemote.class)
public class SingletonInJarInWarBean {
    private final static String CLASSNAME = SingletonInJarInWarBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @EJB(name = "core/local/javaCompLookup/looseSingletonInsideWar")
    BasicLooseSingletonInterface looseSingleton;

    public boolean callVerifyLookup() throws NamingException {
        svLogger.info("--> Creating InitialContext...");
        InitialContext ctx = new InitialContext();
        svLogger.info("--> Looking up bean using: java:comp/env/core/local/javaCompLookup/looseSingletonInsideWar");
        looseSingleton = (BasicLooseSingletonInterface) ctx.lookup("java:comp/env/core/local/javaCompLookup/looseSingletonInsideWar");
        svLogger.info("--> Lookup successful.");

        boolean result = false;
        result = looseSingleton.verifyLooseSingletonBeanLookup();
        svLogger.info("--> The result of looseSingleton.verifyLooseSingletonBeanLookup() = " + result);
        return result;
    }
}
