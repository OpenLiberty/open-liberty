/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.request.logging.data;

/**
 * Allows the request logging support to get access to implementers data.
 */
public interface DataProvider {

    /**
     * Returns the provider's name.
     *
     * @return The providers name.
     */
    public String getProviderName();

    /**
     * Returns a string representation of the implementors data at the time of the request.
     *
     * @return Returns a String representation of the implementors data.
     */
    public String getDataString();
}
