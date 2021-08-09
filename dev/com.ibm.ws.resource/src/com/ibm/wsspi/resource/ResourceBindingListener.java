/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.resource;

/**
 * Callback notified when a binding is needed for a resource access.
 *
 * <p>Implementations should be registered in the OSGi service registry. If
 * multiple listeners are registered, they are called in
 * org.osgi.framework.Constants.SERVICE_RANKING order. The last call to {@link ResourceBinding#setBindingName} will be used.
 *
 * <p>If errors occur, the org.osgi.framework.Constants.SERVICE_DESCRIPTION
 * property should be used to identify this service.
 */
public interface ResourceBindingListener
{
    /**
     * Notification that a binding is being selected. The binding object is
     * valid for the duration of this method invocation only. This method might
     * be called concurrently from multiple threads.
     *
     * @param binding the binding
     */
    void binding(ResourceBinding binding);
}
