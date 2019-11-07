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
package com.ibm.ejb2x.ejbinwar.web;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.NamingException;

import com.ibm.ejb2x.ejbinwar.intf.BasicLooseStatelessInterface;
import com.ibm.ejb2x.ejbinwar.intf.BasicLooseStatelessInterfaceRemote;
import com.ibm.ejb2x.ejbinwar.intf.Comp2xViewStatefulLocal;
import com.ibm.ejb2x.ejbinwar.intf.Comp2xViewStatefulLocalHome;
import com.ibm.ejb2x.ejbinwar.intf.Comp2xViewStatefulRemote;
import com.ibm.ejb2x.ejbinwar.intf.Comp2xViewStatefulRemoteHome;
import com.ibm.ejb2x.ejbinwar.intf.XMLComp2xViewStatefulLocal;
import com.ibm.ejb2x.ejbinwar.intf.XMLComp2xViewStatefulLocalHome;
import com.ibm.ejb2x.ejbinwar.intf.XMLComp2xViewStatefulRemote;
import com.ibm.ejb2x.ejbinwar.intf.XMLComp2xViewStatefulRemoteHome;

@Stateless
@Local(BasicLooseStatelessInterface.class)
@Remote(BasicLooseStatelessInterfaceRemote.class)
public class LooseStatelessBean {
    private final static String CLASSNAME = LooseStatelessBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @Resource
    EJBContext ejbctx;

    @EJB(name = "core/localComp2xStatefulHome")
    Comp2xViewStatefulLocalHome compLocalHome;

    public boolean callVerifyComp2xStatefulLocalLookup() throws NamingException, CreateException {
        svLogger.info("--> Looking up bean from EJBContext using: core/localComp2xStatefulHome");
        compLocalHome = (Comp2xViewStatefulLocalHome) ejbctx.lookup("core/localComp2xStatefulHome");

        svLogger.info("--> Creating bean...");
        Comp2xViewStatefulLocal compLocalBean = compLocalHome.create();

        boolean result = false;
        result = compLocalBean.verifyComp2xStatefulLookup();
        svLogger.info("--> The result of compLocalBean.verifyComp2xStatefulLookup() = " + result);
        return result;
    }

    @EJB(name = "core/remoteComp2xStatefulHome")
    Comp2xViewStatefulRemoteHome compRemoteHome;

    public boolean callVerifyComp2xStatefulRemoteLookup() throws NamingException, RemoteException, CreateException {
        svLogger.info("--> Looking up bean from EJBContext using: core/remoteComp2xStatefulHome");
        compRemoteHome = (Comp2xViewStatefulRemoteHome) ejbctx.lookup("core/remoteComp2xStatefulHome");

        svLogger.info("--> Creating bean...");
        Comp2xViewStatefulRemote compRemoteBean = compRemoteHome.create();

        boolean result = false;
        result = compRemoteBean.verifyComp2xStatefulLookup();
        svLogger.info("--> The result of compRemoteBean.verifyComp2xStatefulLookup() = " + result);
        return result;
    }

    @EJB(name = "core/localXMLComp2xStatefulHome")
    XMLComp2xViewStatefulLocalHome xmlCompLocalHome;

    public boolean callVerifyXMLComp2xStatefulLocalLookup() throws NamingException, CreateException {
        svLogger.info("--> Looking up bean from EJBContext using: core/localXMLComp2xStatefulHome");
        XMLComp2xViewStatefulLocalHome home = (XMLComp2xViewStatefulLocalHome) ejbctx.lookup("core/localXMLComp2xStatefulHome");

        svLogger.info("--> Creating bean...");
        XMLComp2xViewStatefulLocal xmlCompLocalBean = home.create();

        boolean result = false;
        result = xmlCompLocalBean.verifyXMLComp2xStatefulLookup();
        svLogger.info("--> The result of xmlCompLocalBean.verifyXMLComp2xStatefulLookup() = " + result);
        return result;
    }

    @EJB(name = "core/remoteXMLComp2xStatefulHome")
    XMLComp2xViewStatefulRemoteHome xmlCompRemoteHome;

    public boolean callVerifyXMLComp2xStatefulRemoteLookup() throws NamingException, RemoteException, CreateException {
        svLogger.info("--> Looking up bean from EJBContext using: core/remoteXMLComp2xStatefulHome");
        XMLComp2xViewStatefulRemoteHome home = (XMLComp2xViewStatefulRemoteHome) ejbctx.lookup("core/remoteXMLComp2xStatefulHome");

        svLogger.info("--> Creating bean...");
        XMLComp2xViewStatefulRemote xmlCompRemoteBean = home.create();

        boolean result = false;
        result = xmlCompRemoteBean.verifyXMLComp2xStatefulLookup();
        svLogger.info("--> The result of xmlCompRemoteBean.verifyXMLComp2xStatefulLookup() = " + result);
        return result;
    }
}
