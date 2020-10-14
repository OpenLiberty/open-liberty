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
package com.ibm.ws.zos.registration.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.provisioning.ProductExtension;
import com.ibm.ws.kernel.provisioning.ProductExtensionInfo;
import com.ibm.ws.zos.core.NativeService;
import com.ibm.ws.zos.core.internal.CoreBundleActivator;
import com.ibm.ws.zos.jni.NativeMethodManager;

/**
 * Finds the products defined in lib/versions and builds Product objects to represent them
 */
public class ProductManager {

    private static final TraceComponent tc = Tr.register(ProductManager.class);

    /**
     * The map of all installed products. The key is the product ID (not the PID)
     */
    protected HashMap<String, Product> productMap = new HashMap<String, Product>();

    /**
     * The map of all base products. The key is the product ID (not the PID)
     */
    protected HashMap<String, Product> baseProductMap = new HashMap<String, Product>();

    /**
     * An instance of ProductRegistrationImpl. Only non-static so unit test can overload the native call
     */
    protected ProductRegistrationImpl productRegistrationImpl;

    /**
     * Number of successful product registrations.
     */
    private int successfullRegistrationCount;

    /**
     * Constructor
     *
     * Initializes the native interface to product registration
     */
    public ProductManager(NativeMethodManager nativeMethodManager) {

        productRegistrationImpl = new ProductRegistrationImpl();
        productRegistrationImpl.initialize(nativeMethodManager);

    }

    /**
     * Find and register for any products we can get information about
     */
    public void start(BundleContext bundleContext) {
        // Find the 'versions' directory
        String versionPath = CoreBundleActivator.firstNotNull(bundleContext.getProperty(BootstrapConstants.LOC_INTERNAL_LIB_DIR), "")
                             + Product.VERSION_DIR_NAME;

        // Find any properties files in the versions directory
        ArrayList<String> files;
        String[] productFiles = getProductFiles(versionPath);
        if (productFiles == null) {
            files = new ArrayList<String>();
        } else {
            files = new ArrayList<String>(Arrays.asList(productFiles));
        }

        // Look for properties files in product extensions
        Iterator<ProductExtensionInfo> productExtensions = ProductExtension.getProductExtensions().iterator();
        while (productExtensions != null && productExtensions.hasNext()) {
            ProductExtensionInfo prodExt = productExtensions.next();
            String prodExtName = prodExt.getName();

            if (0 != prodExtName.length()) {
                String prodExtLocation = prodExt.getLocation();

                if (prodExtLocation != null && 0 != prodExtLocation.length()) {
                    // Check for absolute path
                    if ((prodExtLocation.charAt(0) == '/') == false) {
                        // Find the install dir and append the relative product extension path
                        String installPath = CoreBundleActivator.firstNotNull(bundleContext.getProperty(BootstrapConstants.LOC_PROPERTY_INSTALL_DIR), "");
                        File installParent = new File(installPath).getParentFile();
                        prodExtLocation = installParent.getAbsolutePath() + "/" + prodExtLocation;
                    }

                    final File prodExtVersionDir = new File(prodExtLocation, "lib/" + Product.VERSION_DIR_NAME);

                    boolean dirExists = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                        @Override
                        public Boolean run() {
                            return prodExtVersionDir.exists();
                        }
                    });

                    if (dirExists) {
                        String[] prodExtVersionFiles = getProductFiles(prodExtVersionDir.getAbsolutePath());
                        files.addAll(Arrays.asList(prodExtVersionFiles));
                    }
                }
            }
        }

        // If we have any files to process
        if ((files != null) && (files.size() > 0)) {

            // Create Product objects, put 'em in the HashMap and remove any that have been replaced by another.
            buildProducts(files);
        }

        // Register base products.
        registerBaseProducts();

        // Register all other products.
        registerProducts();

        // Print a registration summary message.
        if (isServerAuthorizedToDeregister(bundleContext)) {
            Tr.info(tc, "PRODUCT_REGISTRATION_SUMMARY_AUTHORIZED", successfullRegistrationCount);
        } else {
            Tr.info(tc, "PRODUCT_REGISTRATION_SUMMARY_NOT_AUTHORIZED", successfullRegistrationCount);
        }
    }

    /**
     * The core is going away...clean up
     *
     * @param bundleContext Our bundle context
     */
    public void stop(BundleContext bundleContext) {

        // Determine if the server is authorized for IFAUSAGE product deregistration.
        // If the server is not authorized there is nothing to do.
        if (isServerAuthorizedToDeregister(bundleContext)) {
            // Deregister any stack products first. Then the base products.
            deregisterProducts();
            deregisterBaseProducts();
        }
    }

    /**
     * Register all the base products in the baseProductMap with z/OS.
     * All of these products will have a direct line of replacement to
     * com.ibm.websphere.appserver. The order does not matter here.
     */
    protected void registerBaseProducts() {

        if (baseProductMap.isEmpty()) {
            // No base product was found. Create the default and add it to the map
            Product base = new Product(Product.DEFAULT_BASE_PRODUCTID, Product.DEFAULT_BASE_PROD_OWNER, Product.DEFAULT_BASE_PROD_NAME, Product.DEFAULT_BASE_PROD_VERSION, Product.DEFAULT_BASE_PID, Product.DEFAULT_BASE_PROD_QUALIFIER, null, Product.DEFAULT_BASE_PROD_GSSP);

            baseProductMap.put(base.productID, base);
        }

        // Loop through all the products in the map
        for (Product p : baseProductMap.values()) {

            // Register the product
            productRegistrationImpl.registerProduct(p);
            if (p.getRegistered()) {
                successfullRegistrationCount += 1;
            }
        }
    }

    /**
     * Register all the products in the productMap with z/OS
     */
    protected void registerProducts() {

        // If we have anything to do
        if (!productMap.isEmpty()) {

            // Loop through all the products in the map
            for (Product p : productMap.values()) {

                // Register the product
                productRegistrationImpl.registerProduct(p);
                if (p.getRegistered()) {
                    successfullRegistrationCount += 1;
                }
            }
        }
    }

    /**
     * Deregister the base server product with z/OS
     */
    protected void deregisterBaseProducts() {
        if (!baseProductMap.isEmpty()) {
            for (Product p : baseProductMap.values()) {
                productRegistrationImpl.deregisterProduct(p);
            }

            baseProductMap.clear();
        }
    }

    /**
     * Deregister all the products in the productMap with z/OS.
     */
    protected void deregisterProducts() {
        if (!productMap.isEmpty()) {
            for (Product p : productMap.values()) {
                productRegistrationImpl.deregisterProduct(p);
            }

            productMap.clear();
        }
    }

    /**
     * Determines if the server is authorized to use IFAUSAGE to deregister a product.
     * The server is deemed authorized if:
     * 1. The angel is up.
     * 2. The server was started after the angel.
     * 3. The server profile to access authorized services under group PRODMGR has been created and enabled.
     *
     * @param bundleContext The bundle context.
     * @return True if the server is allowed to call IFAUSAGE=DEREGISTER. False otherwise.
     */
    protected boolean isServerAuthorizedToDeregister(BundleContext bundleContext) {
        boolean authorized = false;
        String filter = "(&(native.service.name=IFADEREG)(is.authorized=true))";

        try {
            ServiceReference<?>[] nativeServices = bundleContext.getServiceReferences(NativeService.class.getCanonicalName(), filter);
            if (nativeServices != null && nativeServices.length > 0) {
                authorized = true;
            }
        } catch (InvalidSyntaxException ise) {
            // If we cannot determine that we can use the IFAUSAGE service. Default to false.
            // This exception should really never happen as it pertains to the filter syntax.
        }

        return authorized;
    }

    /**
     * Finds any properties files in the versions directory
     *
     * @param versionPath The full path to the versions directory
     * @return an array of strings, each string a file name (no path) of a properties file in the versions directory
     */
    protected String[] getProductFiles(String versionPath) {

        /**
         * An internal class to filter to just .properties files
         */
        final class PropertiesFilter implements FilenameFilter {

            private final static String propFiles = ".properties";

            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(propFiles))
                    return true;
                else
                    return false;
            }
        }

        final String vp = versionPath;
        String files[] = AccessController.doPrivileged(new PrivilegedAction<String[]>() {
            @Override
            public String[] run() {
                File versionDir = new File(vp);
                FilenameFilter filter = new PropertiesFilter();
                String files[] = versionDir.list(filter);
                return files;
            }
        });

        // Save as full path
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                files[i] = versionPath + "/" + files[i];
            }
        }

        return files;
    }

    /**
     * Populates the product HashMap with Product objects based on the contents of the versions directory
     *
     * @param versionPath The path to the versions directory
     * @param files       an array of properties file names in the versions directory. Must be non-null and non-empty.
     */
    protected void buildProducts(List<String> files) {

        // A Map of products that replace other products. The key is the id of the product being replaced.
        // The value is an ArrayList of products that replace the key
        HashMap<String, ArrayList<Product>> replacesMap = new HashMap<String, ArrayList<Product>>();

        // For each file
        for (String file : files) {

            // Go read the file and create a Product object
            Product product = processFile(file);

            // If that worked put the resulting Product in the HashMap
            if (product != null) {

                // Set the product into the HashMap
                productMap.put(product.productID(), product);

                // If it replaces something, remember that
                if (product.replaces() != null) {

                    if (replacesMap.containsKey(product.replaces())) {
                        // There is already an entry for the replaced product
                        // Add this product to the replacement list
                        replacesMap.get(product.replaces()).add(product);

                    } else {
                        // This is the first entry for the replaced product
                        // Create a new list of replacement products and add it to the map
                        ArrayList<Product> replacementProducts = new ArrayList<Product>();
                        replacementProducts.add(product);
                        replacesMap.put(product.replaces(), replacementProducts);
                    }
                }
            }
        }

        // Find the base product entry using hard coded name
        Product base = productMap.get(Product.BASE_PRODUCTID);

        // A base was included, so we can go ahead and build our tree of replacement products
        resolveBaseProducts(base, replacesMap);

        // Remove any remaining stack products that are replaced by others.
        resolveStackProducts(replacesMap);
    }

    /**
     * Reads a properties file and fetches the Product information from it
     *
     * @param filename The full path to the properties file to read
     * @return A Product object populated from the properties file (or null if there were problems)
     */
    protected Product processFile(String filename) {

        Product product = null;

        // Go read the file
        Properties prop = readProperties(filename);

        // If we got properties out of it, go process ours
        if (prop != null) {
            product = new Product(prop);
        }
        return product;
    }

    /**
     * Reads the properties from the file and handles errors
     *
     * @param filename The properties file to read
     * @return A properties object read from the file
     */
    protected Properties readProperties(String filename) {

        FileInputStream fis = getPropertiesFile(filename);
        Properties prop = null;
        if (fis != null) {
            prop = loadProperties(fis);
            closePropertiesFile(fis);
        }
        return prop;
    }

    /**
     * Get the properties file
     *
     * @param filename The properties file
     * @return A FileInputStream or null
     */
    protected FileInputStream getPropertiesFile(String filename) {
        final String fn = filename;
        FileInputStream fis = AccessController.doPrivileged(new PrivilegedAction<FileInputStream>() {
            @Override
            public FileInputStream run() {
                FileInputStream theFile = null;
                // Read Properties from the file
                try {
                    theFile = new FileInputStream(fn);
                } catch (FileNotFoundException fnfe) {
                    // oh well..just skip it
                }
                return theFile;
            }
        });
        return fis;
    }

    /**
     * Load the properties from the file
     *
     * @param fis A FileInputStream
     * @return A properties object populated from the file (or null)
     */
    protected Properties loadProperties(FileInputStream fis) {
        Properties prop = null;
        try {
            prop = new Properties();
            prop.load(fis);
        } catch (IOException ioe) {

        }
        return prop;
    }

    /**
     * Close the file, handle exceptions
     *
     * @param fis The file to close
     */
    protected void closePropertiesFile(FileInputStream fis) {
        try {
            fis.close();

        } catch (IOException ioe) {

        }
    }

    /**
     * This method is designed to be called recursively and traverses a virtual tree of product replacements
     * starting with the first product passed in. This ensures that we only process product replacements with
     * a direct line to the original base product.
     *
     * Products that are replaced are removed from the productMap (we do not need to register them). Products that
     * replace others (in a direct path to the original base) but are not replaced are removed from the productMap and added to the
     * baseProductMap. We will register those products first. Lastly, for products that are left over in the replacesMap,
     * we do not need to do anything as they are already in the productMap and will be registered as stack products.
     *
     * @param currentBaseProduct The product to search for replacements to in replacesMap
     * @param replacesMap        The map of replacement products. The key is the product filename being replaced
     */
    protected void resolveBaseProducts(Product currentBaseProduct, HashMap<String, ArrayList<Product>> replacesMap) {

        if (!replacesMap.isEmpty() && currentBaseProduct != null) {

            if (!replacesMap.containsKey(currentBaseProduct.productID)) {
                // No one is replacing this base product, and we can be certain that
                // it has a direct line to the com.ibm.websphere.appserver since that
                // was the first product passed in. Add it to the list of base products
                // and remove it from the regular product list.

                productMap.remove(currentBaseProduct.productID);
                baseProductMap.put(currentBaseProduct.productID, currentBaseProduct);

            } else {

                // Remove replaced product from list (no longer need to register it)
                productMap.remove(currentBaseProduct.productID);

                // Iterate over the list of products that replace this one.
                ArrayList<Product> replacementProducts = replacesMap.get(currentBaseProduct.productID);
                for (Product replacementProduct : replacementProducts) {

                    // Make call with new replacement product and work up the tree
                    resolveBaseProducts(replacementProduct, replacesMap);
                }
            }
        }
    }

    /**
     * This method is called after resolveBaseProducts and compresses any product replacements
     * in the remaining stack products to register. The logic here is just to remove any products
     * that are being replaced. We can guarantee that the replacement product exists in productMap,
     * since it is the one that caused the product being replaced to be put into the replacesMap.
     * Iterate over the set of replaced products and remove them from the product map.
     *
     * @param replacesMap The map of replacement products. The key is the product filename being replaced
     */
    protected void resolveStackProducts(HashMap<String, ArrayList<Product>> replacesMap) {
        Set<String> productsBeingReplaced = replacesMap.keySet();

        for (String productBeingReplaced : productsBeingReplaced) {

            productMap.remove(productBeingReplaced);
        }

    }
}