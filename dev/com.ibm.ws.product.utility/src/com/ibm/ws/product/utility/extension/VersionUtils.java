/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.extension;

import java.io.File;
import java.util.Map;

import com.ibm.ws.kernel.productinfo.DuplicateProductInfoException;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.kernel.productinfo.ProductInfoReplaceException;
import com.ibm.ws.product.utility.CommandUtils;

/**
 *
 */
public class VersionUtils {
    /**
     * This method will create a map of product ID to the VersionProperties for that product.
     * 
     * @param ex The execution context that this is being run in
     * @return A map of product IDs to VersionProperties.
     * @throws VersionParsingException If something goes wrong parsing the version files
     */
    protected static Map<String, ProductInfo> getAllProductInfo(File wlpInstallationDirectory) throws VersionParsingException {
        File versionPropertyDirectory = new File(wlpInstallationDirectory, ProductInfo.VERSION_PROPERTY_DIRECTORY);
        if (!versionPropertyDirectory.exists()) {
            throw new VersionParsingException(CommandUtils.getMessage("ERROR_NO_PROPERTIES_DIRECTORY", versionPropertyDirectory.getAbsoluteFile()));
        }
        if (!versionPropertyDirectory.isDirectory()) {
            throw new VersionParsingException(CommandUtils.getMessage("ERROR_NOT_PROPERTIES_DIRECTORY", versionPropertyDirectory.getAbsoluteFile()));
        }
        if (!versionPropertyDirectory.canRead()) {
            throw new VersionParsingException(CommandUtils.getMessage("ERROR_UNABLE_READ_PROPERTIES_DIRECTORY", versionPropertyDirectory.getAbsoluteFile()));
        }

        Map<String, ProductInfo> productIdToVersionPropertiesMap;
        try {
            productIdToVersionPropertiesMap = ProductInfo.getAllProductInfo(wlpInstallationDirectory);
        } catch (IllegalArgumentException e) {
            throw new VersionParsingException(CommandUtils.getMessage("ERROR_UNABLE_READ_PROPERTIES_DIRECTORY", versionPropertyDirectory.getAbsoluteFile()));
        } catch (ProductInfoParseException e) {
            String missingKey = e.getMissingKey();
            if (missingKey != null) {
                throw new VersionParsingException(CommandUtils.getMessage("version.missing.key", missingKey, e.getFile().getAbsoluteFile()));
            }
            throw new VersionParsingException(CommandUtils.getMessage("ERROR_UNABLE_READ_FILE", e.getFile().getAbsoluteFile(), e.getCause().getMessage()));
        } catch (DuplicateProductInfoException e) {
            throw new VersionParsingException(CommandUtils.getMessage("version.duplicated.productId",
                                                                      ProductInfo.COM_IBM_WEBSPHERE_PRODUCTID_KEY,
                                                                      e.getProductInfo1().getFile().getAbsoluteFile(),
                                                                      e.getProductInfo2().getFile().getAbsoluteFile()));
        } catch (ProductInfoReplaceException e) {
            ProductInfo productInfo = e.getProductInfo();
            String replacesId = productInfo.getReplacesId();
            if (replacesId.equals(productInfo.getId())) {
                throw new VersionParsingException(CommandUtils.getMessage("version.replaced.product.can.not.itself", productInfo.getFile().getAbsoluteFile()));
            }
            throw new VersionParsingException(CommandUtils.getMessage("version.replaced.product.not.exist", replacesId, productInfo.getFile().getAbsoluteFile()));
        }

        if (productIdToVersionPropertiesMap.isEmpty()) {
            throw new VersionParsingException(CommandUtils.getMessage("ERROR_NO_PROPERTIES_FILE", versionPropertyDirectory.getAbsoluteFile()));
        }
        return productIdToVersionPropertiesMap;
    }

    /**
     * This exception indicates that something went wrong whilst parsing the properties files.
     */
    protected static class VersionParsingException extends Exception {

        /**  */
        private static final long serialVersionUID = 2079114457028206162L;

        /**
         * Create the exception with the message. This message must be translated and suitable for printing directly to the user console.
         * 
         * @param message The message which must be translated and suitable for printing directly to the user console.
         */
        protected VersionParsingException(String message) {
            super(message);
        }

    }

}
