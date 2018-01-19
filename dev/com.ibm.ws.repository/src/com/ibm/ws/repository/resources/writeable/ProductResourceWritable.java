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
package com.ibm.ws.repository.resources.writeable;

import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.resources.ProductResource;

/**
 * Represents a Product Resource which can be uploaded to a repository.
 * <p>
 * This interface allows write access to fields which are specific to products.
 * <p>
 * Products represented by this interface can either be of type {@link ResourceType#INSTALL} or {@link ResourceType#ADDON}.
 */
public interface ProductResourceWritable extends ProductResource, ProductRelatedResourceWritable, ApplicableToProductWritable {

    /**
     * Sets the type of the product resource
     * <p>
     * Products can either be of type {@link ResourceType#INSTALL} or {@link ResourceType#ADDON}.
     *
     * @param type the type of the product
     */
    public void setType(ResourceType type);

}
