/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package web.assertion.cmt;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.transaction.UserTransaction;

import web.assertion.client.assertion.Hello;

/**
 * Bean-managed EJB client: begins its own transaction, calls the endpoint, then commits or rolls back.
 * The caller's transaction, if any, is suspended while the bean runs.
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class BmtBean {

    @Resource
    private UserTransaction ut;

    public String call(Hello hello, String key, boolean rollback) throws Exception {
        ut.begin();
        final String reply;
        try {
            reply = hello.sayHelloToOther(key);
        } catch (RuntimeException e) {
            ut.rollback();
            throw e;
        }
        if (rollback) {
            ut.rollback();
        } else {
            ut.commit();
        }
        return reply;
    }
}
