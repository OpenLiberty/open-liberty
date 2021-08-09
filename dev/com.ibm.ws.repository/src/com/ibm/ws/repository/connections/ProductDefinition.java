/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.connections;

/**
 * These objects define information about an installed product runtime and specify what resolved resources will install into.
 */
public interface ProductDefinition {

    /**
     * @return the ID of the product
     */
    public String getId();

    /**
     * @return the version of the product
     */
    public String getVersion();

    /**
     * @return the install type of the product
     */
    public String getInstallType();

    /**
     * @return the license type of the product
     */
    public String getLicenseType();

    /**
     * @return the edition of the product
     */
    public String getEdition();

}
