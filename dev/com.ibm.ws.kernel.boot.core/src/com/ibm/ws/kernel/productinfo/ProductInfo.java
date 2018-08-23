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
package com.ibm.ws.kernel.productinfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.FileUtils;
import com.ibm.ws.kernel.provisioning.ProductExtension;
import com.ibm.ws.kernel.provisioning.ProductExtensionInfo;

public class ProductInfo {
    public static final String VERSION_PROPERTY_DIRECTORY = "lib/versions";
    private static final String VERSION_PROPERTY_FILE_SUFFIX = ".properties";

    public static final String COM_IBM_WEBSPHERE_PRODUCTID_KEY = "com.ibm.websphere.productId";
    public static final String COM_IBM_WEBSPHERE_PRODUCTNAME_KEY = "com.ibm.websphere.productName";
    public static final String COM_IBM_WEBSPHERE_PRODUCTVERSION_KEY = "com.ibm.websphere.productVersion";
    public static final String COM_IBM_WEBSPHERE_PRODUCTEDITION_KEY = "com.ibm.websphere.productEdition";
    public static final String COM_IBM_WEBSPHERE_PRODUCTREPLACES_KEY = "com.ibm.websphere.productReplaces";
    public static final String COM_IBM_WEBSPHERE_LOG_REPLACED_PRODUCT = "com.ibm.websphere.logReplacedProduct";

    private static FileFilter versionFileFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return file.getName().endsWith(VERSION_PROPERTY_FILE_SUFFIX);
        }
    };

    public static Map<String, ProductInfo> getAllProductInfo() throws ProductInfoParseException, DuplicateProductInfoException, ProductInfoReplaceException {
        return getAllProductInfo(Utils.getInstallDir());
    }

    public static Map<String, ProductInfo> getAllProductInfo(File installDir) throws ProductInfoParseException, DuplicateProductInfoException, ProductInfoReplaceException {

        // Get core product version files.
        File versionPropertyDirectory = new File(installDir, VERSION_PROPERTY_DIRECTORY);
        File[] coreFiles = versionPropertyDirectory.listFiles();
        if (coreFiles == null) {
            throw new IllegalArgumentException(versionPropertyDirectory.toString());
        }

        ArrayList<File> list = new ArrayList<File>();
        list.addAll(Arrays.asList(coreFiles));

        // Get usr product extension version files.
        File[] userExtFiles = getUserExtensionVersionFiles(installDir);
        if (userExtFiles != null) {
            list.addAll(Arrays.asList(userExtFiles));
        }

        // Get product extension version files.
        Map<String, File[]> prodExtFiles = getVersionFilesByProdExtension(installDir);
        for (Map.Entry<String, File[]> entry : prodExtFiles.entrySet()) {
            list.addAll(Arrays.asList(entry.getValue()));
        }

        // Process found version files.
        HashMap<String, ProductInfo> productInfos = new HashMap<String, ProductInfo>();

        for (File file : list) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(VERSION_PROPERTY_FILE_SUFFIX)) {
                ProductInfo productInfo;
                try {
                    productInfo = parseProductInfo(new FileReader(file), file);
                } catch (FileNotFoundException e) {
                    throw new ProductInfoParseException(file, e);
                }

                ProductInfo existing = productInfos.put(productInfo.getId(), productInfo);
                if (existing != null) {
                    throw new DuplicateProductInfoException(existing, productInfo);
                }
            }
        }

        // Determine product replacements.
        for (Map.Entry<String, ProductInfo> entry : productInfos.entrySet()) {
            ProductInfo productInfo = entry.getValue();
            String replacesId = productInfo.getReplacesId();
            if (replacesId != null) {
                ProductInfo replaces = productInfos.get(replacesId);
                if (replaces == null || replaces == productInfo) {
                    throw new ProductInfoReplaceException(productInfo);
                }

                productInfo.replaces = replaces;
                replaces.replacedBy = productInfo;
            }
        }

        return productInfos;
    }

    public static ProductInfo parseProductInfo(Reader inReader, File file) throws ProductInfoParseException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(inReader);
            Properties properties = new Properties();
            properties.load(reader);

            String id = (String) properties.get(COM_IBM_WEBSPHERE_PRODUCTID_KEY);
            if (id == null) {
                throw new ProductInfoParseException(file, COM_IBM_WEBSPHERE_PRODUCTID_KEY);
            }

            String name = properties.getProperty(COM_IBM_WEBSPHERE_PRODUCTNAME_KEY);
            if (name == null) {
                throw new ProductInfoParseException(file, COM_IBM_WEBSPHERE_PRODUCTNAME_KEY);
            }

            String version = properties.getProperty(COM_IBM_WEBSPHERE_PRODUCTVERSION_KEY);
            if (version == null) {
                throw new ProductInfoParseException(file, COM_IBM_WEBSPHERE_PRODUCTVERSION_KEY);
            }

            String edition = properties.getProperty(COM_IBM_WEBSPHERE_PRODUCTEDITION_KEY);
            if (edition == null) {
                throw new ProductInfoParseException(file, COM_IBM_WEBSPHERE_PRODUCTEDITION_KEY);
            }

            return new ProductInfo(file, id, name, version, edition, properties);

        } catch (IOException e) {
            throw new ProductInfoParseException(file, e);
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (Exception e) {
                }
        }
    }

    private final File file;
    private final String id;
    private final String name;
    private final String version;
    private final String edition;
    private final Properties properties;
    private ProductInfo replaces;
    private ProductInfo replacedBy;

    private ProductInfo(File file, String id, String name, String version, String edition, Properties properties) {
        this.file = file;
        this.id = id;
        this.name = name;
        this.version = version;
        this.edition = edition;
        this.properties = properties;
    }

    public File getFile() {
        return file;
    }

    public String getDisplayName() {
        return name + ' ' + version;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getEdition() {
        return edition;
    }

    public String getReplacesId() {
        return properties.getProperty(COM_IBM_WEBSPHERE_PRODUCTREPLACES_KEY);
    }

    public ProductInfo getReplaces() {
        return replaces;
    }

    public ProductInfo getReplacedBy() {
        return replacedBy;
    }

    public boolean isReplacedProductLogged() {
      return "true".equalsIgnoreCase(properties.getProperty("com.ibm.websphere.logReplacedProduct"));
    }

    public String getProperty(String key) {
        if (key == null) {
            return null;
        }

        String value = properties.getProperty(key);
        if (value == null && replaces != null) {
            value = replaces.getProperty(key);
        }

        return value;
    }

    /**
     * Retrieves the product extension jar bundles located in the installation's usr directory.
     *
     * @return The array of product extension jar bundles in the default (usr) location.
     */
    public static File[] getUserExtensionVersionFiles(File installDir) {

        File[] versionFiles = null;
        String userDirLoc = System.getenv(BootstrapConstants.ENV_WLP_USER_DIR);
        File userDir = (userDirLoc != null) ? new File(userDirLoc) : ((installDir != null) ? new File(installDir, "usr") : null);

        if (userDir != null && userDir.exists()) {
            File userExtVersionDir = new File(userDir, "extension/lib/versions");
            if (userExtVersionDir.exists()) {
                versionFiles = userExtVersionDir.listFiles(versionFileFilter);
            }
        }

        return versionFiles;
    }

    /**
     * Retrieves the product extension jar bundles pointed to by the properties file in etc/extensions.
     *
     * @return The array of product extension jar bundles
     */
    public static Map<String, File[]> getVersionFilesByProdExtension(File installDir) {
        Map<String, File[]> versionFiles = new TreeMap<String, File[]>();
        Iterator<ProductExtensionInfo> productExtensions = ProductExtension.getProductExtensions().iterator();

        while (productExtensions != null && productExtensions.hasNext()) {
            ProductExtensionInfo prodExt = productExtensions.next();
            String prodExtName = prodExt.getName();

            if (0 != prodExtName.length()) {
                String prodExtLocation = prodExt.getLocation();
                if (prodExtLocation != null) {
                    String normalizedProdExtLoc = FileUtils.normalize(prodExtLocation);
                    if (FileUtils.pathIsAbsolute(normalizedProdExtLoc) == false) {
                        String parentPath = installDir.getParentFile().getAbsolutePath();
                        normalizedProdExtLoc = FileUtils.normalize(parentPath + "/" + prodExtLocation + "/");
                    }

                    File prodExtVersionDir = new File(normalizedProdExtLoc, VERSION_PROPERTY_DIRECTORY);
                    if (prodExtVersionDir.exists()) {
                        versionFiles.put(prodExtName, prodExtVersionDir.listFiles(versionFileFilter));
                    }
                }
            }
        }

        return versionFiles;
    }
}
