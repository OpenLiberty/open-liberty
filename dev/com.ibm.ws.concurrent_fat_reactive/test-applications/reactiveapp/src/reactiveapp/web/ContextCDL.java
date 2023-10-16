/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package reactiveapp.web;

import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;

/**
 * A CountDownLatch that can check if it has access to JNDI Context
 */
public interface ContextCDL {

    public void countDown();

    public void checkContext() throws NamingException;

    boolean await(long timeout, TimeUnit unit) throws InterruptedException;
}
