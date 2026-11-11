/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.ejb.ear;

import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;

/**
 * Stateless session bean with an @Asynchronous method on a LOCAL interface.
 * Invoked from within the same EAR; triggers a websphere.ejb.async event.
 * Context info: EJBEventApp | EJBEventEJB | AsyncBean | doAsync
 */
@Stateless
public class AsyncBean implements AsyncBeanLocal {

    private static final Logger log = Logger.getLogger(AsyncBean.class.getName());

    @Override
    @Asynchronous
    public Future<Void> doAsync() throws Exception {
        log.info("AsyncBean.doAsync() invoked");
        return new AsyncResult<>(null);
    }

    @Override
    @Asynchronous
    public Future<Void> doAsyncFail() throws EJBEventTestException {
        log.info("AsyncBean.doAsyncFail() invoked");
        throw new EJBEventTestException("expected failure from AsyncBean.doAsyncFail");
    }

    @Override
    public String doLocal() {
        log.info("AsyncBean.doLocal() invoked");
        return "local";
    }
}
