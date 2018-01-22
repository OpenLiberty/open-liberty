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
 * An immutable POJO implementation of {@link ProductDefinition}.
 */
public class SimpleProductDefinition implements ProductDefinition {

    private final String installType;
    private final String id;
    private final String version;
    private final String licenseType;
    private final String edition;

    /**
     * @param id the ID of the product
     * @param version the version of the product
     * @param installType the install type of the product
     * @param licenseType the licenseType of the product
     * @param edition the edition of the product
     */
    public SimpleProductDefinition(String id, String version, String installType, String licenseType, String edition) {
        super();
        this.installType = installType;
        this.id = id;
        this.version = version;
        this.licenseType = licenseType;
        this.edition = edition;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.repository.resolver.ProductDefinition#getId()
     */
    @Override
    public String getId() {
        return this.id;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.repository.resolver.ProductDefinition#getVersion()
     */
    @Override
    public String getVersion() {
        return this.version;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.repository.resolver.ProductDefinition#getInstallType()
     */
    @Override
    public String getInstallType() {
        return this.installType;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.repository.resolver.ProductDefinition#getLicenseType()
     */
    @Override
    public String getLicenseType() {
        return this.licenseType;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.repository.resolver.ProductDefinition#getEdition()
     */
    @Override
    public String getEdition() {
        return this.edition;
    }

}
