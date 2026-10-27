/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.ejb.jar;

import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;

import io.openliberty.event.logging.ejb.fat.ejb.jar.api.AsyncBeanInJarRemote;

/**
 * Async bean in the standalone EJB JAR. Uses a REMOTE interface so the
 * driver WAR (a separate application) can invoke it across class loaders.
 * Context info (standalone JAR): EJBEventJar | AsyncBeanInJar | doAsync
 */
@Stateless
public class AsyncBeanInJar implements AsyncBeanInJarRemote {

    private static final Logger log = Logger.getLogger(AsyncBeanInJar.class.getName());

    @Override
    @Asynchronous
    public Future<Void> doAsync() throws Exception {
        log.info("AsyncBeanInJar.doAsync() invoked");
        return new AsyncResult<>(null);
    }
}
