/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.provisioning;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;

/**
 * Product Extension.
 *
 * Product Extension may be specified by 3 separate means:
 * 1) Embedder SPI;
 * 2) WLP_PRODUCT_EXT_DIR environment variable; and
 * 3) etc/extensions.
 *
 * The order listed is the order of override when gathering the list of ProductExtensionInfo.
 */
public class ProductExtension {

    public static final String PRODUCT_EXTENSION_DIR = "etc/extensions";

    public static final String PRODUCT_EXTENSIONS_FILE_EXTENSION = ".properties";

    public static final String PRODUCT_EXTENSIONS_INSTALL = "com.ibm.websphere.productInstall";

    public static final String PRODUCT_EXTENSIONS_ID = "com.ibm.websphere.productId";

    private static final String PRODUCT_EXTENSIONS_ENV = "WLP_PRODUCT_EXT_DIR";

    private static FileFilter PROPERTIESFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return file.getName().endsWith(PRODUCT_EXTENSIONS_FILE_EXTENSION);
        }
    };

    private static File getExtensionDir(File installDir) {
        return new File(installDir, PRODUCT_EXTENSION_DIR);
    }

    public static List<ProductExtensionInfo> getProductExtensions() {
        return getProductExtensions(Utils.getInstallDir());
    }

    private static File getFileFromDirectory(File dir, String path) {
        StringBuilder sBuilder = new StringBuilder(dir.getAbsolutePath());
        for (String p : path.split("/")) {
            sBuilder.append(File.separator).append(p);
        }

        return new File(sBuilder.toString());
    }

    /**
     * Get a list of configured product extensions.
     *
     * Merge any Product Extensions from:
     * 1) Embedder SPI;
     * 2) WLP_PRODUCT_EXT_DIR environment variable; and
     * 3) etc/extensions.
     *
     * The order listed is the order of override when gathering the list of ProductExtensionInfo.
     *
     * @param installDir File representing the install path.
     * @return List of ProductExtensionInfo objects.
     */
    public static List<ProductExtensionInfo> getProductExtensions(File installDir) {
        ArrayList<ProductExtensionInfo> productList = new ArrayList<ProductExtensionInfo>();
        Set<String> extensionsSoFar = new HashSet<String>();

        // Get the embedder SPI product extensions
        HashMap<String, Properties> embedderProductExtensions = getExtraProductExtensions();
        if (embedderProductExtensions != null) {
            ProductExtensionInfo prodInfo;
            for (Entry<String, Properties> entry : embedderProductExtensions.entrySet()) {
                String name = entry.getKey();
                Properties featureProperties = entry.getValue();
                if (ExtensionConstants.USER_EXTENSION.equalsIgnoreCase(name) == false) {
                    String installLocation = featureProperties.getProperty(ProductExtension.PRODUCT_EXTENSIONS_INSTALL);
                    String productId = featureProperties.getProperty(ProductExtension.PRODUCT_EXTENSIONS_ID);
                    prodInfo = new ProductExtensionInfoImpl(name, productId, installLocation);
                    productList.add(prodInfo);
                    extensionsSoFar.add(name);
                }
            }
        }

        // Apply any product extensions specified from the WLP_PRODUCT_EXT_DIR environment variable
        String extensionEnv = System.getenv(PRODUCT_EXTENSIONS_ENV);
        if (extensionEnv != null) {
            File productExtensionEnvDir = new File(extensionEnv);
            if (!!!productExtensionEnvDir.isAbsolute()) {
                productExtensionEnvDir = getFileFromDirectory(installDir.getParentFile(), extensionEnv);
            }

            // Extensions added by the embedder SPI will override extensions from the WLP_PRODUCT_EXT_DIR env of the same name.
            String envData = mergeExtensions(productList, extensionsSoFar, productExtensionEnvDir);

            // Push Env Product Extension information found via Env to FrameworkManager for issuing a Message.
            System.clearProperty(BootstrapConstants.ENV_PRODUCT_EXTENSIONS_ADDED_BY_ENV);
            if (!!!envData.isEmpty()) {
                System.setProperty(BootstrapConstants.ENV_PRODUCT_EXTENSIONS_ADDED_BY_ENV, envData);
            }
        }

        // Get the installed etc/extensions.
        File productExtensionsDir = getExtensionDir(installDir);

        // Extensions added by the embedder SPI or WLP_PRODUCT_EXT_DIR
        // will override extensions of the same name that exist in the install root.
        mergeExtensions(productList, extensionsSoFar, productExtensionsDir);

        return productList;
    }

    private static String mergeExtensions(ArrayList<ProductExtensionInfo> productList, Set<String> extensionsSoFar, File mergeProductExtensionDir) {
        StringBuffer mergedProdInfo = new StringBuffer();

        if (mergeProductExtensionDir.exists()) {
            File[] productPropertiesFiles = mergeProductExtensionDir.listFiles(PROPERTIESFilter);

            // Iterate over all the *.properties files in the product extensions dir.
            for (File file : productPropertiesFiles) {
                String fileName = file.getName();

                // Get the product name.
                String productName = fileName.substring(0, fileName.indexOf(PRODUCT_EXTENSIONS_FILE_EXTENSION));

                // Skip a file called just .properties
                if (0 != productName.length()) {
                    if (ExtensionConstants.USER_EXTENSION.equalsIgnoreCase(productName) == false) {
                        // Extensions added by previous calls will override later merges.
                        if (extensionsSoFar.contains(productName) == false) {
                            // Read data in .properties file.
                            ProductExtensionInfo prodInfo;
                            try {
                                prodInfo = loadExtensionInfo(productName, file);
                                if (prodInfo != null) {
                                    productList.add(prodInfo);
                                    extensionsSoFar.add(productName);
                                    mergedProdInfo.append(prodInfo.getName() + "\n" + prodInfo.getProductID() + "\n" + prodInfo.getLocation() + "\n");
                                }
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            }

        }
        return mergedProdInfo.toString();
    }

    private static HashMap<String, Properties> getExtraProductExtensions() {
        HashMap<String, Properties> extraProductExtensions = null;
        String embededData = System.getProperty(BootstrapConstants.ENV_PRODUCT_EXTENSIONS_ADDED_BY_EMBEDDER);
        if (embededData != null) {
            String[] extensions = embededData.split("\n");
            for (int i = 0; (i < extensions.length) && ((i + 3) <= extensions.length); i = i + 3) {
                Properties props = new Properties();
                props.setProperty("com.ibm.websphere.productId", extensions[i + 1]);
                props.setProperty("com.ibm.websphere.productInstall", extensions[i + 2]);
                if (extraProductExtensions == null) {
                    extraProductExtensions = new HashMap<String, Properties>();
                }
                extraProductExtensions.put(extensions[i], props);
            }
        }
        return extraProductExtensions;
    }

    /**
     * Find and return a particular configured product extension.
     *
     * @return
     */
    public static ProductExtensionInfo getProductExtension(String extensionName) throws IOException {
        ProductExtensionInfo productExtensionInfo = null;

        List<ProductExtensionInfo> productExtensionList = getProductExtensions(Utils.getInstallDir());

        for (ProductExtensionInfo currentProductExtension : productExtensionList) {
            if (currentProductExtension.getName().equalsIgnoreCase(extensionName)) {
                productExtensionInfo = currentProductExtension;
                break;
            }
        }

        return productExtensionInfo;
    }

    private static ProductExtensionInfo loadExtensionInfo(String productName, File extensionFile) throws IOException {
        if (extensionFile.isFile()) {
            Properties featureProperties = new Properties();
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(extensionFile);
                featureProperties.load(fileInputStream);
                String installLocation = featureProperties.getProperty(PRODUCT_EXTENSIONS_INSTALL);
                String productId = featureProperties.getProperty(PRODUCT_EXTENSIONS_ID);

                ServiceFingerprint.put(extensionFile);

                return new ProductExtensionInfoImpl(productName, productId, installLocation);
            } finally {
                Utils.tryToClose(fileInputStream);
            }
        }
        return null;
    }

    /**
     * Product extension information.
     */
    public static class ProductExtensionInfoImpl implements ProductExtensionInfo {
        final String productName;

        final String location;

        final String productId;

        /**
         * Constructor.
         *
         * @param productName
         * @param location
         * @param productId
         */
        public ProductExtensionInfoImpl(String productName, String productId, String location) {
            this.productName = productName;
            this.productId = productId;
            this.location = location;
        }

        @Override
        public String getName() {
            return productName;
        }

        @Override
        public String getLocation() {
            return location;
        }

        @Override
        public String getProductID() {
            return productId;
        }
    }
}
