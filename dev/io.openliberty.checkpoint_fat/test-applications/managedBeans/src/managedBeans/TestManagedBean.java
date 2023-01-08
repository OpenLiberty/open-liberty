/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package managedBeans;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.transaction.UserTransaction;

@ManagedBean
public class TestManagedBean {

    private String value = null;
    private static int destroyed = 0;

    @Resource
    UserTransaction userTran;

    public TestManagedBean() {
    }

    @PostConstruct
    public void initialize() {
        synchronized (this) {
            if (userTran != null) {
                value = "TestManagedBean.INITIAL_VALUE";
            } else {
                value = "TestManagedBean.NO_USER_TRAN";
            }
        }
    }

    @PreDestroy
    public void destroy() {
        synchronized (this) {
            destroyed++;
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public UserTransaction getUserTransaction() {
        return userTran;
    }

    public static synchronized int getDestroyCount() {
        return destroyed;
    }

}
