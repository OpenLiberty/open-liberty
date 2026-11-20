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

import javax.enterprise.context.Dependent;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import web.assertion.client.assertion.Hello;

/**
 * CDI client of the assertion endpoints: one method per {@code @Transactional} type. If {@code rollback} is
 * set the method throws {@link RollbackRequested}, a runtime exception, which rolls back the transaction that
 * the interceptor owns. Only ask for that when the bean's own transaction is the one in play.
 */
@Dependent
public class CdiTxBean {

    @Transactional(TxType.REQUIRED)
    public String required(Hello hello, String key, boolean rollback) {
        return call(hello, key, rollback);
    }

    @Transactional(TxType.REQUIRES_NEW)
    public String requiresNew(Hello hello, String key, boolean rollback) {
        return call(hello, key, rollback);
    }

    @Transactional(TxType.MANDATORY)
    public String mandatory(Hello hello, String key, boolean rollback) {
        return call(hello, key, rollback);
    }

    @Transactional(TxType.SUPPORTS)
    public String supports(Hello hello, String key, boolean rollback) {
        return call(hello, key, rollback);
    }

    @Transactional(TxType.NOT_SUPPORTED)
    public String notSupported(Hello hello, String key, boolean rollback) {
        return call(hello, key, rollback);
    }

    @Transactional(TxType.NEVER)
    public String never(Hello hello, String key, boolean rollback) {
        return call(hello, key, rollback);
    }

    private String call(Hello hello, String key, boolean rollback) {
        final String reply = hello.sayHelloToOther(key);
        if (rollback) {
            throw new RollbackRequested(reply);
        }
        return reply;
    }
}
