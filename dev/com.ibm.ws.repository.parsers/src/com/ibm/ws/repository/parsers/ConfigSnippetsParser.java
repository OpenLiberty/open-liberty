/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.repository.parsers;

import java.io.File;

import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.resources.writeable.ConfigSnippetResourceWritable;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;

public class ConfigSnippetsParser extends ParserBase implements Parser<ConfigSnippetResourceWritable> {

    /** {@inheritDoc} */
    @Override
    public ConfigSnippetResourceWritable parseFileToResource(File assetFile, File metadataFile, String contentUrl) throws RepositoryException {

        ArtifactMetadata artifactMetadata = explodeArtifact(assetFile, metadataFile);

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
