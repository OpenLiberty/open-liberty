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
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveIOException;
import com.ibm.ws.repository.resources.internal.ProductResourceImpl;
import com.ibm.ws.repository.resources.writeable.ProductRelatedResourceWritable;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;

public abstract class ProductRelatedParser<T extends ProductRelatedResourceWritable> extends ParserBase implements Parser<T> {

    protected static class AssetInformation {
        private final Logger _logger = Logger.getLogger(AssetInformation.class.getName());
        protected ResourceType type;
        private ProductInfo websphereProductInfo;
        private final List<ProductInfo> infosList = new ArrayList<ProductInfo>();
        protected String provideFeature;
        protected String requireFeature;
        protected String genericRequirements;
        protected String packagedJava;
        public String appliesTo;
        public String laLocation;
        public String liLocation;
        public File fileWithLicensesIn;

        /**
         * Populates the {@link #websphereProductInfo} and {@link #infosList} objects with the properties file that has most of the info about the product archive
         *
         * @param archiveContainingProductInfos
         * @param rootDir
         * @throws ProductInfoParseException
         * @throws IOException
         */
        protected void addProductInfos(ZipFile archiveContainingProductInfos, String rootDir, File archiveFile) throws ProductInfoParseException, IOException {
            Pattern versionPropertiesPattern = Pattern.compile(rootDir + "lib/versions/[^/]*\\.properties$");
            Enumeration<? extends ZipEntry> archiveEntries = archiveContainingProductInfos.entries();
            while (archiveEntries.hasMoreElements()) {
                ZipEntry entry = archiveEntries.nextElement();
                if (versionPropertiesPattern.matcher(entry.getName()).matches()) {
                    _logger.log(Level.INFO, "Attempting to read product version info from {0}", entry.getName());
                    Reader propertyReader = new InputStreamReader(archiveContainingProductInfos.getInputStream(entry));
                    ProductInfo productInfo = ProductInfo.parseProductInfo(propertyReader, archiveFile);
                    infosList.add(productInfo);
                    if ("com.ibm.websphere.appserver".equals(productInfo.getId())) {
                        websphereProductInfo = productInfo;
                    }
                }
            }
        }

    }

    protected abstract AssetInformation extractInformationFromAsset(File archive,
                                                                    ArtifactMetadata metadata) throws ProductInfoParseException, IOException, PrivilegedActionException;

    @Override
    public T parseFileToResource(File archive, File metadataFile, String contentUrl) throws RepositoryException {

        ArtifactMetadata artifactMetadata = explodeArtifact(archive, metadataFile);

        // Read all the contents we need out of the archive and metadata files
        AssetInformation assetInformation;
        try {
            assetInformation = extractInformationFromAsset(archive, artifactMetadata);
        } catch (IOException e) {
            throw new RepositoryArchiveIOException(e.getMessage(), archive, e);
        } catch (ProductInfoParseException e) {
            throw new RepositoryArchiveIOException(e.getMessage(), archive, e);
        } catch (PrivilegedActionException e) {
            throw new RepositoryArchiveIOException(e.getCause().getMessage(), archive, e.getCause());
        }

        @SuppressWarnings("unchecked")
        T resource = (T) WritableResourceFactory.createResource(null, assetInformation.type);

        addContent(resource, archive, archive.getName(), artifactMetadata, contentUrl);

        // Both product and addon use this
        String provFeat = assetInformation.provideFeature;
        if (provFeat != null) {
            List<String> providesList = new ArrayList<String>();
            // We need to split the required features, from a String of comma seperated
            // features into a List of String, where each String is a feature name.
            StringTokenizer featuresTokenizer = new StringTokenizer(provFeat, ",");
            while (featuresTokenizer.hasMoreElements()) {
                String nextFeature = (String) featuresTokenizer.nextElement();
                providesList.add(nextFeature);
            }
            resource.setProvideFeature(providesList);
        }

        if (assetInformation.type.equals(ResourceType.ADDON)) {
            String reqFeat = assetInformation.requireFeature;
            if (reqFeat != null) {
                List<String> requiresList = new ArrayList<String>();
                // We need to split the required features, from a String of comma seperated
                // features into a List of String, where each String is a feature name.
                StringTokenizer featuresTokenizer = new StringTokenizer(reqFeat, ",");
                while (featuresTokenizer.hasMoreElements()) {
                    String nextFeature = (String) featuresTokenizer.nextElement();
                    requiresList.add(nextFeature);
                }
                resource.setRequireFeature(requiresList);
            }

            // Only needed for addon
            ((ProductResourceImpl) resource).setAppliesTo(assetInformation.appliesTo);
        }

        // Work out which props file to use
        // We have found the base props file and stored in websphereProductInfo var, now find
        // if any replace it and build up a list of product infos in the order they are replace.
        // We create a list as the main product info file may not define all the properties
        // and if they don't we want to use the properties from the one that they replace.

        boolean keepLooking = true;
        List<ProductInfo> orderedProductInfos = new ArrayList<ProductInfo>();
        orderedProductInfos.add(assetInformation.websphereProductInfo);
        while (keepLooking) {
            keepLooking = false;
            for (ProductInfo pi : assetInformation.infosList) {
                String replaceId = pi.getReplacesId();
                if (replaceId != null && replaceId.equals(assetInformation.websphereProductInfo.getId())) {
                    assetInformation.websphereProductInfo = pi;
                    orderedProductInfos.add(0, pi);
                    keepLooking = true;
                    break;
                }
            }
        }

        // Product install type takes a default if not set in the properties file
        String productInstallType = null;
        if (assetInformation.websphereProductInfo != null) {
            for (ProductInfo productInfo : orderedProductInfos) {
                String version = productInfo.getVersion();
                if (version != null && !version.isEmpty()) {
                    resource.setProductVersion(version);
                    break;
                }
            }
            for (ProductInfo productInfo : orderedProductInfos) {
                String id = productInfo.getId();
                if (id != null && !id.isEmpty()) {
                    resource.setProductId(id);
                    break;
                }
            }
            for (ProductInfo productInfo : orderedProductInfos) {
                String edition = productInfo.getEdition();
                if (edition != null && !edition.isEmpty()) {
                    resource.setProductEdition(edition);
                    break;
                }
            }
            for (ProductInfo productInfo : orderedProductInfos) {
                productInstallType = productInfo.getProperty("com.ibm.websphere.productInstallType");
                if (productInstallType != null && !productInstallType.isEmpty()) {
                    break;
                }
            }
        }

        // Handle things with defaults for when the properties file is missing
        if (productInstallType == null || productInstallType.isEmpty()) {
            // This came from a JAR so default to archive
            productInstallType = "Archive";
        }
        resource.setProductInstallType(productInstallType);

        resource.setProviderName("IBM");

        resource.setDisplayPolicy(DisplayPolicy.VISIBLE);
        resource.setWebDisplayPolicy(DisplayPolicy.VISIBLE);

        // This should be called after setDisplayPolicy and setWebDisplayPolicy as this can override the values set above
        setCommonFieldsFromSideZip(artifactMetadata, resource);

        //Set the generic requirements from the properties file. Currently only used for platform requirements on zips with java assets for WDT
        if (assetInformation.genericRequirements != null && !assetInformation.genericRequirements.isEmpty()) {
            String requirements = assetInformation.genericRequirements;
            resource.setGenericRequirements(requirements);
        }

        //sets the version and platform info for the java packaged with the product
        if (assetInformation.packagedJava != null && !assetInformation.packagedJava.isEmpty()) {
            resource.setPackagedJava(assetInformation.packagedJava);
        }

        attachLicenseData(artifactMetadata, resource);
        try {
            processLAandLI(assetInformation.fileWithLicensesIn, resource, assetInformation.laLocation, assetInformation.liLocation);
        } catch (IOException e) {
            throw new RepositoryArchiveIOException(e.getMessage(), archive, e);
        }

        return resource;

    }

}
