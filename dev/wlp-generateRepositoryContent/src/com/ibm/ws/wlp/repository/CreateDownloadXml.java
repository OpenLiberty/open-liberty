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
package com.ibm.ws.wlp.repository;

import org.apache.tools.ant.BuildException;

import com.ibm.ws.wlp.repository.xml.DownloadItem;
import com.ibm.ws.wlp.repository.xml.DownloadXml;

/**
 * This ant task will add the information based on ANT properties to the repository download.xml file
 */
public class CreateDownloadXml extends DownloadXmlGenerator {

    private String licenseType;
    private String name;
    private String licenses;
    private String filePath;
    private String archiveSize;
    private String productId;
    private String productEdition;
    private String productVersion;
    private String appliesTo;
    private String provideFeature;
    private String type;
    private String installType;
    private String description;

    @Override
    public void execute() throws BuildException {
        DownloadXml downloadXml = this.parseDownloadXml();

        // If we weren't supplied a download xml file then exit now
        if (downloadXml == null) {
            return;
        }

        DownloadItem downloadItem = new DownloadItem();
        downloadItem.setName(this.name);
        downloadItem.setLicenses(this.licenses);
        downloadItem.setFile(this.filePath);
        downloadItem.setProductId(this.productId);
        downloadItem.setProductEdition(this.productEdition);
        downloadItem.setType(this.type);
        downloadItem.setProductLicenseType(this.licenseType);
        downloadItem.setProductVersion(this.productVersion);
        downloadItem.setProductInstallType(this.installType);
        downloadItem.setDownloadSize(Long.parseLong(this.archiveSize));
        downloadItem.setAppliesTo(this.appliesTo);
        downloadItem.setProvideFeature(this.provideFeature);
        downloadItem.setDescription(this.description);
        downloadXml.getDownloadItems().add(downloadItem);
        this.writeDownloadXml(downloadXml);
    }

    /**
     * @param licenseType the licenseType to set
     */
    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param licenses the licenses to set
     */
    public void setLicenses(String licenses) {
        this.licenses = licenses;
    }

    /**
     * @param filePath the filePath to set
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * @param archiveSize the archiveSize to set
     */
    public void setArchiveSize(String archiveSize) {
        this.archiveSize = archiveSize;
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
     * @param productVersion the productVersion to set
     */
    public void setProductVersion(String productVersion) {
        this.productVersion = productVersion;
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
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @param installType the installType to set
     */
    public void setInstallType(String installType) {
        this.installType = installType;
    }

    /**
     * @param description the description to set
     */
    @Override
    public void setDescription(String description) {
        this.description = description;
    }

}
