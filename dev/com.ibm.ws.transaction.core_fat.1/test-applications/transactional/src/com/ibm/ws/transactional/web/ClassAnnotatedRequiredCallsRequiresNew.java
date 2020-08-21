/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transactional.web;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.transaction.UserTransaction;

@Transactional(TxType.REQUIRED)
public class ClassAnnotatedRequiredCallsRequiresNew {

    @Inject
    UserTransaction ut;

    public int tryGetStatus(ClassAnnotatedRequiresNewTestBean bean, TestContext tc) throws Throwable {
        bean.basicRequiresNewNoLists(tc, null);
        return ut.getStatus();
    }

}
