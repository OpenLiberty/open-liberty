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

package com.ibm.ws.repository.connections.liberty;

import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.repository.connections.ProductDefinition;

/**
 * A {@link ProductDefinition} that is backed by a {@link ProductInfo} object. If the underlying {@link ProductInfo} changes then this class will reflect those changes.
 */
public class ProductInfoProductDefinition implements ProductDefinition {

    private final ProductInfo productInfo;

    /**
     * @param productInfo the {@link ProductInfo} that will supply all the information about the product
     */
    public ProductInfoProductDefinition(ProductInfo productInfo) {
        super();
        this.productInfo = productInfo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.repository.resolver.ProductDefinition#getId()
     */
    @Override
    public String getId() {
        return this.productInfo.getId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.repository.resolver.ProductDefinition#getVersion()
     */
    @Override
    public String getVersion() {
        return this.productInfo.getVersion();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.repository.resolver.ProductDefinition#getInstallType()
     */
    @Override
    public String getInstallType() {
        return this.productInfo.getProperty("com.ibm.websphere.productInstallType");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.repository.resolver.ProductDefinition#getLicenseType()
     */
    @Override
    public String getLicenseType() {
        return this.productInfo.getProperty("com.ibm.websphere.productLicenseType");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.repository.resolver.ProductDefinition#getEdition()
     */
    @Override
    public String getEdition() {
        return this.productInfo.getEdition();
    }

}
