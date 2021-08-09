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
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveIOException;
import com.ibm.ws.repository.resources.writeable.ProductRelatedResourceWritable;

public abstract class ProductRelatedJarParser<T extends ProductRelatedResourceWritable> extends ProductRelatedParser<T> {

    public abstract ResourceType getType(String contentType, File archive);

    /**
     * {@inheritDoc}
     *
     * @throws PrivilegedActionException
     * @throws IOException
     * @throws ProductInfoParseException
     *
     * @throws RepositoryArchiveIOException
     */
    @Override
    protected AssetInformation extractInformationFromAsset(final File archive, ArtifactMetadata metadata) throws PrivilegedActionException, ProductInfoParseException, IOException {
        AssetInformation assetInformation = new AssetInformation();
        JarFile jarFile = null;
        try {
            jarFile = AccessController.doPrivileged(new PrivilegedExceptionAction<JarFile>() {

                @Override
                public JarFile run() throws IOException {
                    return new JarFile(archive);
                }

            });
            Manifest manifest = jarFile.getManifest();
            Attributes mainAttributes = manifest.getMainAttributes();
            String archiveRoot = mainAttributes.getValue("Archive-Root");
            archiveRoot = archiveRoot != null ? archiveRoot : "";
            assetInformation.addProductInfos(jarFile, archiveRoot, archive);

            // See what type of archive this is
            String contentType = mainAttributes.getValue("Archive-Content-Type");
            ResourceType type = getType(contentType, archive);
            assetInformation.type = type;
            assetInformation.provideFeature = mainAttributes.getValue("Provide-Feature");

            if (type.equals(ResourceType.ADDON)) {
                // Only needed for addon, not product
                assetInformation.requireFeature = mainAttributes.getValue("Require-Feature");
                assetInformation.appliesTo = mainAttributes.getValue("Applies-To");
            }
            assetInformation.laLocation = mainAttributes.getValue(LA_HEADER_PRODUCT);
            assetInformation.liLocation = mainAttributes.getValue(LI_HEADER_PRODUCT);
            assetInformation.fileWithLicensesIn = archive;
        } finally {
            if (jarFile != null) {
                jarFile.close();
            }
        }
        return assetInformation;
    }
}
