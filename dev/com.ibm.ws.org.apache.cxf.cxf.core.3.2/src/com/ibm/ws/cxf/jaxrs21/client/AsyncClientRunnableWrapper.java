/*******************************************************************************
 * Copyright (c) 2018-2019 IBM Corporation and others.
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
package com.ibm.ws.cxf.jaxrs21.client;

import org.apache.cxf.message.Message;

/**
 * Implementations of this interface will wrap the JAX-RS Async Client
 * runnable with a new runnable (or return the original runnable). For
 * example, an implementation could wrap the runnable to ensure that it
 * has the necessary Java EE contexts when the runnable is executed on
 * another thread.
 */
public interface AsyncClientRunnableWrapper {

    void prepare(Message message);

    Runnable wrap(Message message, Runnable runnable);
}
