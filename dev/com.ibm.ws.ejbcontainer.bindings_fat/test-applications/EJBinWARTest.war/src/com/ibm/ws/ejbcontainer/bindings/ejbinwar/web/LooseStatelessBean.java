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
package com.ibm.ws.ejbcontainer.bindings.ejbinwar.web;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import com.ibm.ws.ejbcontainer.bindings.ejbinwar.ejb.BasicSingletonInterface;
import com.ibm.ws.ejbcontainer.bindings.ejbinwar.ejb.BasicSingletonInterfaceRemote;
import com.ibm.ws.ejbcontainer.bindings.ejbinwar.intf.BasicLooseSingletonInterfaceRemote;
import com.ibm.ws.ejbcontainer.bindings.ejbinwar.intf.BasicLooseStatelessInterface;
import com.ibm.ws.ejbcontainer.bindings.ejbinwar.intf.BasicLooseStatelessInterfaceRemote;

@Stateless
@Local(BasicLooseStatelessInterface.class)
@Remote(BasicLooseStatelessInterfaceRemote.class)
public class LooseStatelessBean {
    private final static String CLASSNAME = LooseStatelessBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static InitialContext ivctx;

    @Resource
    EJBContext ejbctx;

    @EJB(name = "core/remote/javaCompLookup/beanInsideWar")
    BasicLooseSingletonInterfaceRemote remoteLooseSing;

    public boolean callVerifyRemoteJavaCompLookup() throws NamingException {
        svLogger.info("--> Newing up InitialContext...");
        ivctx = new InitialContext();

        svLogger.info("--> Looking up bean using: java:comp/env/core/remote/javaCompLookup/beanInsideWar");
        remoteLooseSing = (BasicLooseSingletonInterfaceRemote) ivctx.lookup("java:comp/env/core/remote/javaCompLookup/beanInsideWar");
        svLogger.info("--> Lookup successful.");

        boolean result = false;
        result = remoteLooseSing.verifyLooseSingletonBeanLookup();
        svLogger.info("--> The result of remoteLooseSing.verifyLooseSingletonBeanLookup() = " + result);
        return result;
    }

    public boolean callVerifyGlobalLocalLookup() throws NamingException {
        svLogger.info("--> Newing up InitialContext...");
        ivctx = new InitialContext();

        svLogger.info("--> Looking up bean using: ejblocal:EJBinWARTestApp/EJBBean.jar/SingletonBean#com.ibm.ws.ejbcontainer.bindings.ejbinwar.ejb.BasicSingletonInterface");
        BasicSingletonInterface localStandaloneSing = (BasicSingletonInterface) ivctx.lookup("ejblocal:EJBinWARTestApp/EJBBean.jar/SingletonBean#com.ibm.ws.ejbcontainer.bindings.ejbinwar.ejb.BasicSingletonInterface");
        svLogger.info("--> Lookup successful.");

        boolean result = false;
        result = localStandaloneSing.verifyStandaloneEJBLookup();
        svLogger.info("--> The result of localStandaloneSing.verifyStandaloneEJBLookup() = " + result);
        return result;
    }

    public boolean callVerifyGlobalRemoteLookup() throws NamingException {
        svLogger.info("--> Newing up InitialContext...");
        ivctx = new InitialContext();

        svLogger.info("--> Looking up generic object using: ejb/EJBinWARTestApp/EJBBean.jar/SingletonBean#com.ibm.ws.ejbcontainer.bindings.ejbinwar.ejb.BasicSingletonInterfaceRemote");
        Object o = ivctx.lookup("ejb/EJBinWARTestApp/EJBBean.jar/SingletonBean#com.ibm.ws.ejbcontainer.bindings.ejbinwar.ejb.BasicSingletonInterfaceRemote");

        svLogger.info("--> Narrowing the object to BasicSingletonInterfaceRemote... ");
        BasicSingletonInterfaceRemote remoteStandaloneSing = (BasicSingletonInterfaceRemote) PortableRemoteObject.narrow(o, BasicSingletonInterfaceRemote.class);

        boolean result = false;
        result = remoteStandaloneSing.verifyStandaloneEJBLookup();
        svLogger.info("--> The result of remoteStandaloneSing.verifyStandaloneEJBLookup() = " + result);
        return result;
    }
}
