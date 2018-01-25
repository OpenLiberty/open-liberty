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

import java.util.Collection;

import com.ibm.ws.repository.resources.ProductRelatedResource;

/**
 * Represents a product-like (either a product or a tool) resource in the repository.
 * <p>
 * This interface allows write access to fields which are specific to product-like things.
 */
public interface ProductRelatedResourceWritable extends ProductRelatedResource, RepositoryResourceWritable, WebDisplayable {

    /**
     * Sets the productId for the resource
     *
     * @param productId the product id
     */
    public void setProductId(String productId);

    /**
     * Sets the edition of the product
     *
     * @param edition the edition of the product
     */
    public void setProductEdition(String edition);

    /**
     * Sets the install type if the product (e.g. "Archive")
     *
     * @param productInstallType the install type
     */
    public void setProductInstallType(String productInstallType);

    /**
     * Sets the version of the product
     *
     * @param version the product version
     */
    public void setProductVersion(String version);

    /**
     * Sets the features included in this product
     *
     * @param provideFeature the symbolic names of features provided by this product
     */
    public void setProvideFeature(Collection<String> provideFeature);

    /**
     * Sets the features that this product depends on
     *
     * @param requireFeature the symbolic names of features required by this product
     */
    public void setRequireFeature(Collection<String> requireFeature);

    /**
     * Sets the collection of OSGi requirements this product has
     *
     * @param genericRequirements The OSGi requirements for this product
     */
    public void setGenericRequirements(String genericRequirements);

    /**
     * Sets the version information for the Java packaged with this product
     *
     * @param packagedJava The version information for the packaged Java
     */
    public void setPackagedJava(String packagedJava);

}