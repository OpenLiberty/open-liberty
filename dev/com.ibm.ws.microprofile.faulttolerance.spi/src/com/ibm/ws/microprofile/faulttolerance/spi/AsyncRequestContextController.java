/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.spi;

/**
 * Used to activate and deactivate the request context.
 * <p>
 * activateContext() should return an ActivatedContext object to deactivate the request context.
 * <p>
 * Example:
 *
 * <pre>
 * <code>
 *     &#64;Inject
 *     private final AsyncRequestContextController asyncRequestContext;
 *
 *     public void executeAsyncMethod() {
 *          ActivatedContext requestContext = asyncRequestContext.activateContext();
 *          try {
 *              // Do something while request context is active
 *          } finally {
 *              requestContext.deactivate();
 *          }
 *     }
 * </code>
 * </pre>
 */
public interface AsyncRequestContextController {

    /**
     * Activates the request context on the current thread.
     * <p>
     * If the request context is already active, this method does nothing and returns an {@link ActivatedContext} which does
     * nothing in its {@link ActivatedContext#deactivate()} method.
     *
     * @return {@link ActivatedContext} which must be used to deactivate the context again.
     */
    public ActivatedContext activateContext();

    /**
     * Represents a request context started with {@link AsyncRequestContextController#activateContext()}.
     */
    public interface ActivatedContext {

        /**
         * Deactivates the request context linked to this object.
         */
        public void deactivate();

    }
}
