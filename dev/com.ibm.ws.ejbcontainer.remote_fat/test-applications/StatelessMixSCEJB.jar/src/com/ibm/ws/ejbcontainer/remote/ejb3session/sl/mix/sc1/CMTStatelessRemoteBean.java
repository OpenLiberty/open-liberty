/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.sc1;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

import java.util.logging.Logger;

import javax.ejb.Remote;
import javax.ejb.TransactionAttribute;

import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.sc2.CMTStatelessRemote;

/**
 * Bean implementation class for Enterprise Bean: CMTStatelessRemoteBean
 **/
@Remote(CMTStatelessRemote.class)
public class CMTStatelessRemoteBean {
    private static final String CLASS_NAME = CMTStatelessRemoteBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @TransactionAttribute(REQUIRES_NEW)
    public void tx_RequiresNew() {
        svLogger.info("Method tx_RequiresNew called successfully");
    }

    public CMTStatelessRemoteBean() {}
}