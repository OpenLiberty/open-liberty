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
package com.ibm.ws.repository.parsers;

import java.io.File;

import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveException;
import com.ibm.ws.repository.resources.writeable.ApplicableToProductWritable;
import com.ibm.ws.repository.resources.writeable.ProductRelatedResourceWritable;
import com.ibm.ws.repository.resources.writeable.RepositoryResourceWritable;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;

public class GenericParser extends ParserBase implements Parser<RepositoryResourceWritable> {

    /** Key for a property stating what type of asset this is. Can be one of {@link ResourceType} enum values. */
    public static String ASSET_TYPE_PROPERTY_KEY = "assetType";
    /** Key for a property stating what this asset applies to, only relevant for some asset types. */
    public static String APPLIES_TO_PROPERTY_KEY = "appliesTo";

    /** {@inheritDoc} */
    @Override
    public RepositoryResourceWritable parseFileToResource(File assetFile, File metadataFile, String contentUrl) throws RepositoryException {

        File artifactFile = null;
        ArtifactMetadata metadata = null;
        if (assetFile.getName().endsWith("metadata.zip")) {
            metadata = explodeZip(assetFile);
            // No artifactFile to set, that's fine we just won't have a CRC or file size
        } else {
            metadata = explodeArtifact(assetFile, metadataFile);
            artifactFile = assetFile;
        }

        // Make sure all the required fields are set
        if (metadata == null) {
            throw new RepositoryArchiveException("Unable to find sibling metadata zip for " + assetFile.getName()
                                                 + " so do not have the required information", assetFile);
        }

        checkPropertySet(ASSET_TYPE_PROPERTY_KEY, metadata);
        ResourceType type = ResourceType.valueOf(metadata.getProperty(ASSET_TYPE_PROPERTY_KEY));
        RepositoryResourceWritable resource = WritableResourceFactory.createResource(null, type);
        setCommonFieldsFromSideZip(metadata, resource);
        resource.setProviderName("IBM");
        resource.setProviderUrl("http://www.ibm.com");
        attachLicenseData(metadata, resource);

        if (resource instanceof ProductRelatedResourceWritable) {
            setProductDetails((ProductRelatedResourceWritable) resource, metadata);
        }

        String appliesTo = metadata.getProperty(APPLIES_TO_PROPERTY_KEY);
        if (appliesTo != null && resource instanceof ApplicableToProductWritable) {
            ((ApplicableToProductWritable) resource).setAppliesTo(appliesTo);
        }

        addContent(resource, artifactFile, assetFile.getName(), metadata, contentUrl);
        processIcons(metadata, resource);

        return resource;
    }

    private static void setProductDetails(ProductRelatedResourceWritable resource, ArtifactMetadata metadata) throws RepositoryException {
        String version = metadata.getProperty("productVersion");
        String id = metadata.getProperty("productId");
        String editionString = metadata.getProperty("productEdition");

        if (id == null && version == null && editionString == null) {
            // nothing to do
            return;
        }

        if (id == null || version == null || editionString == null) {
            throw new RepositoryException("If one of id, version and edition are set in the metadata, then all 3 must be set"
                                          + "id was: " + id + ", version was: " + version + ", edition was: " + editionString);
        }

        String upperCaseEditionString = editionString.toUpperCase();
        try {
            ParserBase.Edition.valueOf(upperCaseEditionString);
        } catch (IllegalArgumentException e) {
            throw new RepositoryException("The edition specified in the metadata (" + editionString + ")was not known");
        }

        resource.setProductId(id);
        resource.setProductVersion(version);
        resource.setProductEdition(upperCaseEditionString);
    }

}
