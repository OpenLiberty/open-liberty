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
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.zip.ZipFile;

import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.resources.writeable.ProductResourceWritable;

public class ProductZipParser extends ProductRelatedParser<ProductResourceWritable> {

    private static final String PROP_GENERIC_REQUIREMENTS = "genericRequirements";
    private static final String PROP_PACKAGED_JAVA = "packagedJava";

//    TODO
//    public ProductZIPParser(RepositoryConnection loginInfoResource) {
//        super(loginInfoResource);
//    }

    /**
     * {@inheritDoc}
     *
     * @throws PrivilegedActionException
     * @throws IOException
     * @throws ProductInfoParseException
     */
    @Override
    protected AssetInformation extractInformationFromAsset(final File archive, final ArtifactMetadata metadata) throws PrivilegedActionException, ProductInfoParseException, IOException {

        // Create the asset information
        AssetInformation assetInformtion = new AssetInformation();
        ZipFile zipFile = null;
        try {
            zipFile = AccessController.doPrivileged(new PrivilegedExceptionAction<ZipFile>() {

                @Override
                public ZipFile run() throws IOException {
                    return new ZipFile(archive);
                }
            });
            assetInformtion.addProductInfos(zipFile, "wlp/", archive);
            assetInformtion.type = ResourceType.INSTALL;
            assetInformtion.provideFeature = metadata.getProperty("provideFeature");
            assetInformtion.laLocation = "wlp/lafiles/LA";
            assetInformtion.liLocation = "wlp/lafiles/LI";
            assetInformtion.fileWithLicensesIn = archive;
            assetInformtion.genericRequirements = metadata.getProperty(PROP_GENERIC_REQUIREMENTS);
            assetInformtion.packagedJava = metadata.getProperty(PROP_PACKAGED_JAVA);
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }
        return assetInformtion;
    }
}
