/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS.client;

import java.util.concurrent.Callable;

/**
 * Other components can implement this interface to wrap the task used for async client requests.
 */
public interface ClientAsyncTaskWrapper {
    
    Runnable wrap(Runnable r);
    
    <T> Callable<T> wrap(Callable<T> c);

}
