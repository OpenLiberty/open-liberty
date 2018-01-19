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

public class PaxParser extends ProductRelatedParser<ProductResourceWritable> {

    /**
     * {@inheritDoc}
     * 
     * @throws PrivilegedActionException
     * @throws IOException
     * @throws ProductInfoParseException
     */
    @Override
    protected AssetInformation extractInformationFromAsset(File archive, final ArtifactMetadata metadata) throws PrivilegedActionException, ProductInfoParseException, IOException {
        // Create the asset information
        AssetInformation assetInformtion = new AssetInformation();
        ZipFile zipFile = null;
        try {
            zipFile = AccessController.doPrivileged(new PrivilegedExceptionAction<ZipFile>() {

                @Override
                public ZipFile run() throws IOException {
                    return new ZipFile(metadata.getArchive());
                }

            });
            assetInformtion.addProductInfos(zipFile, "wlp/", archive);
            assetInformtion.type = ResourceType.INSTALL;
            assetInformtion.provideFeature = metadata.getProperty("provideFeature");
            assetInformtion.laLocation = "wlp/lafiles_text/LA";
            assetInformtion.liLocation = "wlp/lafiles_text/LI";
            assetInformtion.fileWithLicensesIn = metadata.getArchive();
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
        }
        return assetInformtion;
    }

}
