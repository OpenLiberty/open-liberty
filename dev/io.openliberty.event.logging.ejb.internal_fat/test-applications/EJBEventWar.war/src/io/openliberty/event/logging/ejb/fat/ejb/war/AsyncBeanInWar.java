/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.ejb.war;

import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;

/**
 * Async bean embedded in the standalone WAR. Uses a REMOTE async interface
 * (contrast with the EAR which uses local).
 * Context info (standalone WAR): EJBEventWar | AsyncBeanInWar | doAsync
 */
@Stateless
public class AsyncBeanInWar implements AsyncBeanInWarRemote {

    private static final Logger log = Logger.getLogger(AsyncBeanInWar.class.getName());

    @Override
    @Asynchronous
    public Future<Void> doAsync() throws Exception {
        log.info("AsyncBeanInWar.doAsync() invoked");
        return new AsyncResult<>(null);
    }
}
