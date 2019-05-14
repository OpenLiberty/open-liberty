/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

import static javax.ejb.TransactionAttributeType.MANDATORY;
import static javax.ejb.TransactionAttributeType.NEVER;
import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import static javax.ejb.TransactionAttributeType.SUPPORTS;
import static javax.ejb.TransactionManagementType.CONTAINER;

import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.Init;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionManagement;

/**
 * Bean implementation class for Enterprise Bean: CMTStatefulLocal
 **/
@Stateful(name = "CompCMTStateful")
@Remote(CMTStatefulRemote.class)
@RemoteHome(CMTStatefulEJBRemoteHome.class)
@TransactionManagement(CONTAINER)
public class CompCMTStatefulBean {
    private final static String CLASSNAME = CompCMTStatefulBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** New Line character for this system. **/
    public static final String NL = System.getProperty("line.separator", "\n");

    public void tx_Default() {
        svLogger.info("Method tx_Default called successfully");
    }

    @TransactionAttribute(REQUIRED)
    public void tx_Required() {
        svLogger.info("Method tx_Required called successfully");
    }

    @TransactionAttribute(NOT_SUPPORTED)
    public void tx_NotSupported() {
        svLogger.info("Method tx_NotSupported called successfully");
    }

    @TransactionAttribute(REQUIRES_NEW)
    public void tx_RequiresNew() {
        svLogger.info("Method tx_RequiresNew called successfully");
    }

    @TransactionAttribute(SUPPORTS)
    public void tx_Supports() {
        svLogger.info("Method tx_Supports called successfully");
    }

    @TransactionAttribute(NEVER)
    public void tx_Never() {
        svLogger.info("Method tx_Never called successfully");
    }

    @TransactionAttribute(MANDATORY)
    public void tx_Mandatory() {
        svLogger.info("Method tx_Mandatory called successfully");
    }

    public CompCMTStatefulBean() {
    }

    @Init
    public void ejbCreate() throws CreateException {
    }
}
