/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.restfulWS.client;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Other components can implement this interface to wrap the task used for async client requests.
 */
public interface ClientAsyncTaskWrapper {

    Runnable wrap(Runnable r);

    <T> Callable<T> wrap(Callable<T> c);

    @FFDCIgnore(RuntimeException.class)
    default <T> Supplier<T> wrap(Supplier<T> s) {
        // Turn the supplier into a callable to wrap it and then back again
        // Implementors should override this with a more efficient implementation that wraps the supplier directly
        Callable<T> c = wrap((Callable<T>) s::get);
        return () -> {
            try {
                return c.call();
            } catch (RuntimeException e) {
                // Pass through any runtime exceptions
                throw e;
            } catch (Exception e) {
                // Won't have any non-runtime exception thrown from the supplier, but may have from the wrapper
                // Rethrowing it as a runtime exception is the best we can do here
                throw new RuntimeException(e);
            }
        };
    }

}
