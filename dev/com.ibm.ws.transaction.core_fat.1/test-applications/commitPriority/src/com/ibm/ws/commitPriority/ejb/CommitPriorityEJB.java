/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.commitPriority.ejb;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import com.ibm.ws.commitPriority.common.CommitPriorityLocal;
import com.ibm.ws.commitPriority.common.CommitPriorityTestUtils;

/**
 * Session Bean implementation class Test
 */
@Stateless
@Named("ejb")
@TransactionManagement(value = TransactionManagementType.BEAN)
public class CommitPriorityEJB implements CommitPriorityLocal {

    @Resource(name = "jdbc/derby1")
    DataSource ds1;

    @Resource(name = "jdbc/derby2")
    DataSource ds2;

    @Resource(name = "jdbc/derby3")
    DataSource ds3;

    @Resource
    private UserTransaction ut;

    @Inject
    private @Named("managedbeaninejb") CommitPriorityLocal cdi;

    @Override
    public String testMethod() throws Exception {

        String result = CommitPriorityTestUtils.test(ut, ds1, ds2, ds3);

        return result += cdi.testMethod();
    }
}