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
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import web.assertion.client.assertion.Hello;

/**
 * Container-managed EJB client of the assertion endpoints: one method per transaction attribute. Each calls
 * {@code sayHelloToOther} and, if {@code rollback} is set, marks the transaction rollback-only. Only ask for
 * that when the bean's own transaction is the one in play; it would otherwise poison the caller's.
 */
@Stateless
public class CmtBean {

    @Resource
    private SessionContext ctx;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String required(Hello hello, String key, boolean rollback) {
        return call(hello, key, rollback);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String requiresNew(Hello hello, String key, boolean rollback) {
        return call(hello, key, rollback);
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public String mandatory(Hello hello, String key, boolean rollback) {
        return call(hello, key, rollback);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String supports(Hello hello, String key, boolean rollback) {
        return call(hello, key, rollback);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String notSupported(Hello hello, String key, boolean rollback) {
        return call(hello, key, rollback);
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public String never(Hello hello, String key, boolean rollback) {
        return call(hello, key, rollback);
    }

    private String call(Hello hello, String key, boolean rollback) {
        final String reply = hello.sayHelloToOther(key);
        if (rollback) {
            ctx.setRollbackOnly();
        }
        return reply;
    }
}
