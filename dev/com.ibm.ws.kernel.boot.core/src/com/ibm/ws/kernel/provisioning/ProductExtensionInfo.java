/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package com.ibm.ws.kernel.provisioning;

/**
 * Product extension interface.
 */
public interface ProductExtensionInfo {

    /**
     * Retrieves the name of the product extension.
     * 
     * @return The name of the product extension.
     */
    public String getName();

    /**
     * Retrieves the location of the product extension.
     * 
     * @return The location of the product extension.
     */
    public String getLocation();

    /**
     * Retrieves the product extension ID.
     * 
     * @return The product ID.
     */
    public String getProductID();
}