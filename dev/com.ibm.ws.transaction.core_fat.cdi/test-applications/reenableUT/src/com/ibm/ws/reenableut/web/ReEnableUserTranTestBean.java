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
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.transaction.UserTransaction;

public class ReEnableUserTranTestBean {

    @Resource
    private UserTransaction ut;

    @Inject
    private ReEnableUserTranTestBean2 bean;

    @Transactional(TxType.NOT_SUPPORTED)
    public boolean checkReEnablementNotSupportedFromNotSupported() throws Exception {
        return bean.checkNotSupportedReEnablesUserTran(true);
    }

    @Transactional(TxType.NEVER)
    public boolean checkReEnablementNotSupportedFromNever() throws Exception {
        return bean.checkNotSupportedReEnablesUserTran(true);
    }

    @Transactional(TxType.REQUIRED)
    public boolean checkReEnablementNotSupportedFromRequired(boolean fixed) throws Exception {
        return bean.checkNotSupportedReEnablesUserTran(fixed);
    }

    @Transactional(TxType.REQUIRES_NEW)
    public boolean checkReEnablementNotSupportedFromRequiresNew(boolean fixed) throws Exception {
        return bean.checkNotSupportedReEnablesUserTran(fixed);
    }

    @Transactional(TxType.SUPPORTS)
    public boolean checkReEnablementNotSupportedFromSupports(boolean fixed) throws Exception {
        return bean.checkNotSupportedReEnablesUserTran(fixed);
    }

    @Transactional(TxType.MANDATORY)
    public boolean checkReEnablementNotSupportedFromMandatory(boolean fixed) throws Exception {
        return bean.checkNotSupportedReEnablesUserTran(fixed);
    }

    @Transactional(TxType.NEVER)
    public boolean checkReEnablementNeverFromNever() throws Exception {
        return bean.checkNeverReEnablesUserTran(true);
    }

    @Transactional(TxType.NOT_SUPPORTED)
    public boolean checkReEnablementNeverFromNotSupported() throws Exception {
        return bean.checkNeverReEnablesUserTran(true);
    }

    @Transactional(TxType.SUPPORTS)
    public boolean checkReEnablementNeverFromSupports(boolean fixed) throws Exception {
        return bean.checkNeverReEnablesUserTran(fixed);
    }
}