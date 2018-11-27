/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wlp.repository.xml;

import javax.xml.bind.annotation.XmlAttribute;

public class DownloadItem {

    private String name = null;
    private String description = null;
    private String licenses = null;
    private String file = null;
    private String productId = null;
    private String productEdition = null;
    private String type = null;
    private String productLicenseType = null;
    private String productVersion = null;
    private String productInstallType = null;
    private String appliesTo = null;
    private String provideFeature = null;
    private long downloadSize = 0;

    /**
     * @return the name
     */
    @XmlAttribute(required = true)
    public String getName() {
        return name;
    }

    /**
     * @return the description
     */
    @XmlAttribute
    public String getDescription() {
        return description;
    }

    /**
     * @return the licenses
     */
    @XmlAttribute
    public String getLicenses() {
        return licenses;
    }

    /**
     * @return the file
     */
    @XmlAttribute(required = true)
    public String getFile() {
        return file;
    }

    /**
     * @return the productId
     */
    @XmlAttribute
    public String getProductId() {
        return productId;
    }

    /**
     * @return the productEdition
     */
    @XmlAttribute
    public String getProductEdition() {
        return productEdition;
    }

    /**
     * @return the type
     */
    @XmlAttribute(required = true)
    public String getType() {
        return type;
    }

    /**
     * @return the productLicenseType
     */
    @XmlAttribute
    public String getProductLicenseType() {
        return productLicenseType;
    }

    /**
     * @return the productVersion
     */
    @XmlAttribute
    public String getProductVersion() {
        return productVersion;
    }

    /**
     * @return the productInstallType
     */
    @XmlAttribute
    public String getProductInstallType() {
        return productInstallType;
    }

    /**
     * @return the appliesTo
     */
    @XmlAttribute
    public String getAppliesTo() {
        return appliesTo;
    }

    /**
     * @return the provideFeature
     */
    @XmlAttribute
    public String getProvideFeature() {
        return provideFeature;
    }

    /**
     * @return the downloadSize
     */
    @XmlAttribute(required = true)
    public long getDownloadSize() {
        return downloadSize;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param licenses the licenses to set
     */
    public void setLicenses(String licenses) {
        this.licenses = licenses;
    }

    /**
     * @param file the file to set
     */
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * @param productId the productId to set
     */
    public void setProductId(String productId) {
        this.productId = productId;
    }

    /**
     * @param productEdition the productEdition to set
     */
    public void setProductEdition(String productEdition) {
        this.productEdition = productEdition;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @param productLicenseType the productLicenseType to set
     */
    public void setProductLicenseType(String productLicenseType) {
        this.productLicenseType = productLicenseType;
    }

    /**
     * @param productVersion the productVersion to set
     */
    public void setProductVersion(String productVersion) {
        this.productVersion = productVersion;
    }

    /**
     * @param productInstallType the productInstallType to set
     */
    public void setProductInstallType(String productInstallType) {
        this.productInstallType = productInstallType;
    }

    /**
     * @param appliesTo the appliesTo to set
     */
    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }

    /**
     * @param provideFeature the provideFeature to set
     */
    public void setProvideFeature(String provideFeature) {
        this.provideFeature = provideFeature;
    }

    /**
     * @param size the downloadSize to set
     */
    public void setDownloadSize(long size) {
        this.downloadSize = size;
    }

}
