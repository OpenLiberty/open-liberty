/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.cdi.jcdi.ejb;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.transaction.UserTransaction;

@Stateless
public class UserTranStatelessBean {
    @Inject
    private UserTranBean bean;

    public UserTransaction getUserTransaction() {
        UserTransaction userTran = bean.getUserTransaction();

        // Should be callable without an error.
        try {
            userTran.getStatus();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // .commit/.rollback might not be prevented, but they would ruin the
        // container-managed transaction, so that's an application error.

        return userTran;
    }
}
