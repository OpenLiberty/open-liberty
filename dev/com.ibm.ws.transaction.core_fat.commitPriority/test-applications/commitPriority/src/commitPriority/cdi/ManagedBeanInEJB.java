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
package commitPriority.cdi;

import javax.annotation.Resource;
import javax.inject.Named;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.transaction.UserTransaction;

import commitPriority.common.CommitPriorityLocal;
import commitPriority.common.CommitPriorityTestUtils;

/**
 * CDI Bean implementation class Test
 */
@Transactional(value = TxType.NOT_SUPPORTED)
@Named("managedbeaninejb")
public class ManagedBeanInEJB implements CommitPriorityLocal {

    @Resource(name = "jdbc/derby10")
    DataSource ds1;

    @Resource(name = "jdbc/derby11")
    DataSource ds2;

    @Resource(name = "jdbc/derby12")
    DataSource ds3;

    @Resource
    private UserTransaction ut;

    @Override
    public String testMethod() throws Exception {

        return CommitPriorityTestUtils.test(ut, ds1, ds2, ds3);
    }
}