/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.reenableut.web;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.transaction.UserTransaction;

public class ReEnableUserTranTestBean2 {

    @Resource
    private UserTransaction ut;

    @Transactional(TxType.NOT_SUPPORTED)
    public boolean checkNotSupportedReEnablesUserTran(boolean fixed) throws Exception {

        System.out.println("checkNotSupportedReEnablesUserTran(" + fixed + ")");
        return testLogic(fixed);
    }

    @Transactional(TxType.NEVER)
    public boolean checkNeverReEnablesUserTran(boolean fixed) throws Exception {

        System.out.println("checkNeverReEnablesUserTran(" + fixed + ")");
        return testLogic(fixed);
    }

    private boolean testLogic(boolean fixed) throws Exception {
        if (!fixed) {
            try {
                ut.getStatus();
                return false;
            } catch (IllegalStateException e) {
                return true;
            }
        } else {
            ut.getStatus();
            return true;
        }
    }
}