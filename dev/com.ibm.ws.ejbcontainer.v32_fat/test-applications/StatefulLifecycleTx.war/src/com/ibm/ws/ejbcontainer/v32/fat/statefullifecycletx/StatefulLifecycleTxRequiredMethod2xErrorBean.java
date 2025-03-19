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

import javax.ejb.LocalHome;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateful
@LocalHome(StatefulLifecycleTx2xHome.class)
@SuppressWarnings("serial")
public class StatefulLifecycleTxRequiredMethod2xErrorBean extends AbstractStatefulLifecycleTxBean implements SessionBean {
    @Override
    public void setSessionContext(SessionContext context) {
    }

    // @Init - implicit
    public void ejbCreate() {
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void ejbPassivate() {
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void ejbActivate() {
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void ejbRemove() {
    }
}
