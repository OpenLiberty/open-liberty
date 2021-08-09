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
package com.ibm.ws.repository.resources;

import java.util.Collection;

import org.osgi.resource.Requirement;

/**
 * Represents a product-like (either a product or a tool) resource in the repository.
 * <p>
 * This interface allows read access to fields which are specific to product-like things.
 */
public interface ProductRelatedResource extends RepositoryResource {

    /**
     * Gets the productId for the resource.
     *
     * @return the product id, or null if it has not been set
     */
    public String getProductId();

    /**
     * Gets the edition of the product, if applicable.
     *
     * @return the edition of the product, or null if it is not set
     */
    public String getProductEdition();

    /**
     * Gets the install type of the product (e.g. "Archive")
     *
     * @return the install type, or null if it is not set
     */
    public String getProductInstallType();

    /**
     * Gets the version of the product
     *
     * @return the product version, or null if it is not set
     */
    public String getProductVersion();

    /**
     * Gets the features included in this product
     *
     * @return the features provided by this product, or null if not set
     */
    public Collection<String> getProvideFeature();

    /**
     * Gets the features that this product depends on
     *
     * @return the features required by this product, or null if not set
     */
    public Collection<String> getRequireFeature();

    /**
     * Gets the collection of OSGi requirements this product has
     *
     * @return the collection of OSGi requirements applicable to this product, or null if not set
     */
    public Collection<Requirement> getGenericRequirements();

    /**
     * Gets the version information for the Java packaged with this product
     *
     * @return The Java version information, or null if this product is not packaged with Java
     */
    public String getPackagedJava();

}
