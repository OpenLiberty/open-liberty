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

import javax.ejb.Asynchronous;
import javax.ejb.Remote;

/**
 * REMOTE async interface for {@link AsyncBeanInWar}.
 * This WAR scenario intentionally uses a REMOTE interface for async,
 * contrasting with the EAR scenario which uses a local interface.
 */
@Remote
public interface AsyncBeanInWarRemote {
    @Asynchronous
    Future<Void> doAsync() throws Exception;
}
