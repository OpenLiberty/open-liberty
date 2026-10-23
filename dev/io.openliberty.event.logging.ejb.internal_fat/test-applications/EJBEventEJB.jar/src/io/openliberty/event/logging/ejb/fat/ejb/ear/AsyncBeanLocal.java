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

import javax.ejb.Asynchronous;
import javax.ejb.Local;

/**
 * Local interface for {@link AsyncBean}. The async method is declared on the
 * local interface so this EAR scenario tests LOCAL async invocations.
 */
@Local
public interface AsyncBeanLocal {
    @Asynchronous
    Future<Void> doAsync() throws Exception;

    /** Always throws a checked application exception, which the caller sees as the cause of an ExecutionException. */
    @Asynchronous
    Future<Void> doAsyncFail() throws EJBEventTestException;

    /** Plain (synchronous) local business method; not timed, so produces no event log entry. */
    String doLocal();
}
