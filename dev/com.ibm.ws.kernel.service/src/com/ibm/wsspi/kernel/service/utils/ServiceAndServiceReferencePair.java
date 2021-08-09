/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import org.osgi.framework.ServiceReference;

/**
 * A simple interface to associate a DS instance of a Service, with the ServiceReference that created it.
 * <br>
 * Handy for when you want to query properties on the ServiceReference for a given Service.
 * 
 * @param <T> The type of the Service
 */
public interface ServiceAndServiceReferencePair<T> {
    /**
     * Get the Service instance for this pair.
     * 
     * @return
     */
    public T getService();

    /**
     * Get the ServiceReference instance for this pair.
     * 
     * @return
     */
    public ServiceReference<T> getServiceReference();
}
