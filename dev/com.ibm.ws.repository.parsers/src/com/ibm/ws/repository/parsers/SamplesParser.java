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
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.ws.repository.common.enums.LicenseType;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveException;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveIOException;
import com.ibm.ws.repository.parsers.internal.ManifestInfo;
import com.ibm.ws.repository.resources.writeable.SampleResourceWritable;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;

public class SamplesParser extends ParserBase implements Parser<SampleResourceWritable> {

    /** {@inheritDoc} */
    @Override
    public SampleResourceWritable parseFileToResource(File assetFile, File metadataFile, String contentUrl) throws RepositoryException {

        ArtifactMetadata artifactMetadata = explodeArtifact(assetFile, metadataFile);

        // Throw an exception if there is no metadata and properties, we get the name and readme from it
        if (artifactMetadata == null) {
            throw new RepositoryArchiveException("Unable to find sibling metadata zip for " + assetFile.getName()
                                                 + " so do not have the required information", assetFile);
        }

        // parse the jar manifest
        JarFile jar;
        try {
            jar = new JarFile(assetFile);
        } catch (IOException e) {
            throw new RepositoryArchiveIOException(e.getMessage(), assetFile, e);
        }
        ManifestInfo mi = ManifestInfo.parseManifest(jar);

        SampleResourceWritable resource = WritableResourceFactory.createSample(null, mi.getType());
        setCommonFieldsFromSideZip(artifactMetadata, resource);

        // set fields from manifest info
        resource.setProviderName(mi.getProviderName());
        resource.setAppliesTo(mi.getAppliesTo());
        resource.setRequireFeature(mi.getRequiredFeature());

        // upload the content locally or from DHE if there is a properties
        // file telling us where it lives
        addContent(resource, assetFile, assetFile.getName(), artifactMetadata, contentUrl);

        /*
         * Find the short name from the JAR, it is based on the server name of the sample. The server
         * will be a directory in the wlp/usr/servers directory (or another root usr dir defined by
         * archiveRoot) and have a server.xml inside it. The server name is the directory name so do
         * a regex that effectively gets a group in the following form:
         * wlp/usr/server/{group}/server.xml
         */
        String archiveRoot = mi.getArchiveRoot();
        Pattern sampleShortNamePattern = Pattern.compile(archiveRoot + "servers/([^/]*)/server.xml");
        try {
            ZipFile zip = new ZipFile(assetFile);
            @SuppressWarnings("unchecked")
            Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zip.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement().getName();
                Matcher shortNameMatcher = sampleShortNamePattern.matcher(entryName);
                if (shortNameMatcher.matches()) {
                    resource.setShortName(shortNameMatcher.group(1));
                    break;
                }
            }
            zip.close();
        } catch (IOException e) {
            throw new RepositoryArchiveIOException(e.getMessage(), assetFile, e);
        }

        if (null != artifactMetadata && null != artifactMetadata.licenseFiles
            && !artifactMetadata.licenseFiles.isEmpty()) {
            // for samples the "license" is really a list of files that will be
            // downloaded and should be in English only, if not something is
            // wrong so bomb out
            if (artifactMetadata.licenseFiles.size() > 1) {
                throw new RepositoryArchiveException("There were too many licenses associated with "
                                                     + assetFile.getName()
                                                     + ". An English only license is expected but had: "
                                                     + artifactMetadata.licenseFiles, assetFile);
            }
            resource.setLicenseType(LicenseType.UNSPECIFIED);
            resource.addLicense(
                                artifactMetadata.licenseFiles.iterator().next(),
                                Locale.ENGLISH);

            try {
                processLAandLI(assetFile, resource, mi.getManifest());
            } catch (IOException e) {
                throw new RepositoryArchiveIOException(e.getMessage(), assetFile, e);
            }
        }
        return resource;
    }
}
