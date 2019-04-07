/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client;

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
