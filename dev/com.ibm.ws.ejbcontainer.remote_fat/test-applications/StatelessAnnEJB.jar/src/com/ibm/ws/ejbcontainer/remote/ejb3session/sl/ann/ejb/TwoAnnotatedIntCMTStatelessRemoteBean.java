/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb;

import static javax.ejb.TransactionAttributeType.MANDATORY;
import static javax.ejb.TransactionAttributeType.NEVER;
import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import static javax.ejb.TransactionAttributeType.SUPPORTS;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.CreateException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;

/**
 * Bean implementation class for Enterprise Bean: BasicCMTStatelessRemote and
 * annotated interface AnnotatedCMTStatelessRemote
 **/
@Stateless
public class TwoAnnotatedIntCMTStatelessRemoteBean implements AnnotatedCMTStatelessRemote, AnnotatedCMTStatelessRemote2 {
    /** New Line character for this system. **/
    public static final String NL = System.getProperty("line.separator", "\n");
    private static final String CLASS_NAME = TwoAnnotatedIntCMTStatelessRemoteBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Resource
    private SessionContext ivContext;

    @Override
    public void tx_Default() {
        svLogger.info("Method tx_Default called successfully");
    }

    @Override
    @TransactionAttribute(REQUIRED)
    public void tx_Required() {
        svLogger.info("Method tx_Required called successfully");
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public void tx_NotSupported() {
        svLogger.info("Method tx_NotSupported called successfully");
    }

    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public void tx_RequiresNew() {
        svLogger.info("Method tx_RequiresNew called successfully");
    }

    @Override
    @TransactionAttribute(SUPPORTS)
    public void tx_Supports() {
        svLogger.info("Method tx_Supports called successfully");
    }

    @Override
    @TransactionAttribute(NEVER)
    public void tx_Never() {
        svLogger.info("Method tx_Never called successfully");
    }

    @Override
    @TransactionAttribute(MANDATORY)
    public void tx_Mandatory() {
        svLogger.info("Method tx_Mandatory called successfully");
    }

    @Override
    public void tx_ADefault() {
        svLogger.info("Method tx_ADefault called successfully");
    }

    @Override
    @TransactionAttribute(REQUIRED)
    public void tx_ARequired() {
        svLogger.info("Method tx_ARequired called successfully");
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public void tx_ANotSupported() {
        svLogger.info("Method tx_ANotSupported called successfully");
    }

    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public void tx_ARequiresNew() {
        svLogger.info("Method tx_ARequiresNew called successfully");
    }

    @Override
    @TransactionAttribute(SUPPORTS)
    public void tx_ASupports() {
        svLogger.info("Method tx_ASupports called successfully");
    }

    @Override
    @TransactionAttribute(NEVER)
    public void tx_ANever() {
        svLogger.info("Method tx_ANever called successfully");
    }

    @Override
    @TransactionAttribute(MANDATORY)
    public void tx_AMandatory() {
        svLogger.info("Method tx_AMandatory called successfully");
    }

    public TwoAnnotatedIntCMTStatelessRemoteBean() {}

    public void ejbCreate() throws CreateException {}

    public void ejbRemove() {}

    public void ejbActivate() {}

    public void ejbPassivate() {}

    public void setSessionContext(SessionContext sc) {
        ivContext = sc;
    }
}