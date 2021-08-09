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
import com.ibm.ws.repository.resources.writeable.AdminScriptResourceWritable;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;

public class AdminScriptParser extends ParserBase implements Parser<AdminScriptResourceWritable> {

    public static final String PROP_LANGUAGE = "script.language";

    /** {@inheritDoc} */
    @Override
    public AdminScriptResourceWritable parseFileToResource(File assetFile, File metadataFile, String contentUrl) throws RepositoryException {

        ArtifactMetadata artifactMetadata = explodeArtifact(assetFile, metadataFile);

        // Throw an exception if there is no metadata and properties, we get the name and readme from it
        if (artifactMetadata == null) {
            throw new RepositoryArchiveException("Unable to find sibling metadata zip for " + assetFile.getName()
                                                 + " so do not have the required information", assetFile);
        }

        AdminScriptResourceWritable resource = WritableResourceFactory.createAdminScript(null);

        setCommonFieldsFromSideZip(artifactMetadata, resource);

        checkPropertySet(PROP_PROVIDER_NAME, artifactMetadata);
        checkPropertySet(PROP_LANGUAGE, artifactMetadata);

        // Provider and script resources are stored in the properties file
        resource.setProviderName(getProviderName(artifactMetadata));
        resource.setScriptLanguage(artifactMetadata.getProperty(PROP_LANGUAGE));
        resource.setAppliesTo(getAppliesTo(artifactMetadata));
        resource.setRequireFeature(getRequiresFeature(artifactMetadata));

        // Attach the content (main attachment) to the resource
        addContent(resource, assetFile, assetFile.getName(), artifactMetadata, contentUrl);

        return resource;
    }
}
