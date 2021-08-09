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

import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveException;
import com.ibm.ws.repository.resources.writeable.ConfigSnippetResourceWritable;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;

public class ConfigSnippetsParser extends ParserBase implements Parser<ConfigSnippetResourceWritable> {

    /** {@inheritDoc} */
    @Override
    public ConfigSnippetResourceWritable parseFileToResource(File assetFile, File metadataFile, String contentUrl) throws RepositoryException {

        ArtifactMetadata artifactMetadata = explodeArtifact(assetFile, metadataFile);

        // Throw an exception if there is no metadata and properties, we get various required data from there
        if (artifactMetadata == null) {
            throw new RepositoryArchiveException("Unable to find sibling metadata zip for " + assetFile.getName()
                                                 + " so do not have the required information", assetFile);
        }
        // create the empty ConfigSnippetResource
        ConfigSnippetResourceWritable resource = WritableResourceFactory.createConfigSnippet(null);

        // Start setting fields
        setCommonFieldsFromSideZip(artifactMetadata, resource);

        checkPropertySet(PROP_APPLIES_TO_EDITIONS, artifactMetadata);
        checkPropertySet(PROP_APPLIES_TO_MIN_VERSION, artifactMetadata);
        checkPropertySet(PROP_PROVIDER_NAME, artifactMetadata);
        checkPropertySet(PROP_REQUIRE_FEATURE, artifactMetadata);

        resource.setAppliesTo(getAppliesTo(artifactMetadata));
        resource.setRequireFeature(getRequiresFeature(artifactMetadata));
        resource.setProviderName(getProviderName(artifactMetadata));

        // Attach the main attachment to the resource
        addContent(resource, assetFile, assetFile.getName(), artifactMetadata, contentUrl);

        return resource;
    }
}
