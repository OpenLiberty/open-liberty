/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.v32.fat.statefullifecycletx;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.LocalHome;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateful
@LocalHome(StatefulLifecycleTx2xHome.class)
@SuppressWarnings("serial")
public class StatefulLifecycleTxRequiresNewMethod2xBean extends AbstractStatefulLifecycleTxBean implements SessionBean {
    private static final Logger logger = Logger.getLogger(StatefulLifecycleTxRequiresNewMethod2xBean.class.getName());

    @Override
    public void setSessionContext(SessionContext context) {
    }

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void postConstruct() {
        logger.info("postConstruct");
        checkGlobalUOW("PostConstruct", false);
    }

    // @Init - implicit
    public void ejbCreate() {
        logger.info("ejbCreate");
        checkLocalUOW("Init");
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void ejbPassivate() {
        logger.info("ejbPassivate");
        checkGlobalUOW("PrePassivate", false);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void ejbActivate() {
        logger.info("ejbActivate");
        checkGlobalUOW("PostActivate", false);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void ejbRemove() {
        logger.info("ejbRemove");
        checkGlobalUOW("PreDestroy", false);
    }
}
