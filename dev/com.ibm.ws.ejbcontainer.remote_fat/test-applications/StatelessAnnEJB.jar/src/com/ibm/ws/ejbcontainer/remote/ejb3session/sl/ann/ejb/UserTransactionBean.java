/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.transaction.UserTransaction;

@Stateless
@Remote(UserTransactionRemote.class)
@TransactionManagement(TransactionManagementType.BEAN)
public class UserTransactionBean {
    private static final Logger svLogger = Logger.getLogger(UserTransactionBean.class.getName());

    @EJB
    private UserTransactionRemote ivBean;

    @Resource
    private UserTransaction ivUserTran;

    public void test() {
        // The ORB performs different serialization depending on the size of the
        // arguments, so try 1024 through 1MB.
        for (int i = 0; i <= 10; i++) {
            int size = 1024 << i;
            svLogger.info("trying " + size + " bytes");
            ivBean.test(new byte[size], ivUserTran);
        }
    }

    public UserTransaction test(byte[] big, UserTransaction ut) {
        return ut;
    }
}