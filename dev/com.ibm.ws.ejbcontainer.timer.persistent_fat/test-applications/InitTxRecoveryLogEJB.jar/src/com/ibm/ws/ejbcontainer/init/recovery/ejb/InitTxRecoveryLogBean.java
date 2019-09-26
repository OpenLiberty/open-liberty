/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.init.recovery.ejb;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;

@Singleton
@Startup
@LocalBean
public class InitTxRecoveryLogBean {
    private static final Logger logger = Logger.getLogger(InitTxRecoveryLogBean.class.getName());

    @PostConstruct
    @TransactionAttribute(REQUIRES_NEW)
    public void initTxRecoverLogs() {
        logger.info("---- Transaction Recovery Logs Initialized ----");
    }
}
