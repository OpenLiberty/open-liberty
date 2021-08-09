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

import java.util.logging.Logger;

import javax.ejb.Remote;
import javax.ejb.TransactionAttribute;

/**
 * Bean implementation class for Enterprise Bean: AltCMTStatefulLocal
 **/
@Remote(AltCMTStatefulRemote.class)
public class AltCMTStatefulBean {
    private final static String CLASSNAME = AltCMTStatefulBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** New Line character for this system. **/
    public static final String NL = System.getProperty("line.separator", "\n");

    public void txDefault() {
        svLogger.info("Method txDefault called successfully");
    }

    @TransactionAttribute(REQUIRED)
    public void txRequired() {
        svLogger.info("Method txRequired called successfully");
    }

    @TransactionAttribute(NOT_SUPPORTED)
    public void txNotSupported() {
        svLogger.info("Method txNotSupported called successfully");
    }

    @TransactionAttribute(REQUIRES_NEW)
    public void txRequiresNew() {
        svLogger.info("Method txRequiresNew called successfully");
    }

    @TransactionAttribute(SUPPORTS)
    public void txSupports() {
        svLogger.info("Method txSupports called successfully");
    }

    @TransactionAttribute(NEVER)
    public void txNever() {
        svLogger.info("Method txNever called successfully");
    }

    @TransactionAttribute(MANDATORY)
    public void txMandatory() {
        svLogger.info("Method txMandatory called successfully");
    }

    public AltCMTStatefulBean() {
    }
}
