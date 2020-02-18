/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.v1;

/**
 * Service to obtain the instance of the Catalog.
 */
public interface ICatalogService {

    /**
     * Returns the Catalog instance. References to the instance should not be cached.
     * 
     * @return ICatalog instance. Will not return {@code null}.
     */
    ICatalog getCatalog();

}
