/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.ejb.first;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;

/**
 * A local singleton startup bean (EJB session bean) that requires the container to
 * execute a method within a new transaction context when the bean is created.
 */
@Singleton
@Startup
@LocalBean
public class InitNewTxBean1 {
    private static final Logger logger = Logger.getLogger(InitNewTxBean1.class.getName());

    @PostConstruct
    @TransactionAttribute(REQUIRES_NEW)
    public void initTx1() {
        logger.info("---- InitTx1 invoked ----");
    }
}
