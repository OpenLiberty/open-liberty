/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.ejb.jar.api;

import java.util.concurrent.Future;

import javax.ejb.Asynchronous;
import javax.ejb.Remote;

/**
 * Remote async interface for {@link io.openliberty.event.logging.ejb.fat.ejb.jar.AsyncBeanInJar}.
 * Lives in the shared {@code api} sub-package so both the EJB JAR and the
 * driver WAR can include this package independently via ShrinkHelper.
 */
@Remote
public interface AsyncBeanInJarRemote {
    @Asynchronous
    Future<Void> doAsync() throws Exception;
}
