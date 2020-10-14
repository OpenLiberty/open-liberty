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

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Represents a Product contained in this installation
 */
public class Product {

    private static final TraceComponent tc = Tr.register(Product.class);

    /**
     * Default string for any unspecified values.
     */
    protected static final String UNKNOWN = "UNKNOWN";

    /**
     * Directory name under /lib where Product files live.
     */
    protected static final String VERSION_DIR_NAME = "versions";

    /**
     * The Product ID for the base application server product (Product ID, not PID)
     */
    protected static final String BASE_PRODUCTID = "com.ibm.websphere.appserver";

    /**
     * Extension for properties files
     */
    protected static final String PROPERTIES_FILE = ".properties";

    /**
     * The product ID (not to be confused with the PID). This is something like com.ibm.websphere.appserver where PID is
     * more like 5655-W65. Thus a dot qualified name vs. an IBM number.
     */
    protected final String productID;

    /**
     * Property that defines the product ID
     */
    protected static final String PRODUCTID = "com.ibm.websphere.productId";

    /**
     * The owner of the product (e.g. IBM).
     */
    private final String owner;

    /**
     * Property that defines the owner.
     */
    protected static final String OWNER = "com.ibm.websphere.productOwner";

    /**
     * The name of the product.
     */
    private final String name;

    /**
     * Property that defines the name.
     */
    protected static final String NAME = "com.ibm.websphere.productName";

    /**
     * The version of the product.
     */
    private final String version;

    /**
     * Property that defines the version.
     */
    protected static final String VERSION = "com.ibm.websphere.productVersion";

    /**
     * The IBM product ID number (e.g. 5655-123).
     */
    private final String pid;

    /**
     * Property that defines the PID.
     */
    protected static final String PID = "com.ibm.websphere.productPID";

    /**
     * Some qualifier for the product name.
     */
    private final String qualifier;

    /**
     * Property that defines the qualifier.
     */
    protected static final String QUALIFIER = "com.ibm.websphere.productQualifier";

    /**
     * Name of the properties file for the product this product replaces.
     */
    private final String replaces;

    /**
     * Property that defines the replacement.
     */
    protected static final String REPLACES = "com.ibm.websphere.productReplaces";

    /**
     * Flag to indicate if we are a GSSP Product.
     */
    private final String gssp;

    /**
     * Property that defines the GSSP.
     */
    protected static final String GSSP = "com.ibm.websphere.gssp";

    /**
     * Default base product ID
     */
    protected static final String DEFAULT_BASE_PRODUCTID = "com.ibm.websphere.appserver.zos";

    /**
     * Default base product owner.
     */
    protected static final String DEFAULT_BASE_PROD_OWNER = "IBM";

    /**
     * Default base product name.
     */
    protected static final String DEFAULT_BASE_PROD_NAME = "WAS FOR Z/OS";

    /**
     * Default base product version.
     */
    protected static final String DEFAULT_BASE_PROD_VERSION = "8.5";

    /**
     * Default base PID.
     */
    protected static final String DEFAULT_BASE_PID = "5655-W65";

    /**
     * Default base product qualifier.
     */
    protected static final String DEFAULT_BASE_PROD_QUALIFIER = "WAS Z/OS";

    /**
     * Default base product gssp value.
     */
    protected static final String DEFAULT_BASE_PROD_GSSP = "true";

    /**
     * Ebcdic representation of the product owner in byte array form.
     */
    private byte[] ownerBytes;

    /**
     * Ebcdic representation of the product name in byte array form.
     */
    private byte[] nameBytes;

    /**
     * Ebcdic representation of the product version in byte array form.
     */
    private byte[] versionBytes;

    /**
     * Ebcdic representation of the product ID (PID) in byte array form.
     */
    private byte[] pidBytes;

    /**
     * Ebcdic representation of the product qualifier in byte array form.
     */
    private byte[] qualifierBytes;

    /**
     * Product owner max length as specified by the IFAUSAGE service.
     */
    protected static final int PRODUCT_OWNER_MAX_LENGTH = 16;

    /**
     * Product name max length as specified by the IFAUSAGE service.
     */
    protected static final int PRODUCT_NAME_MAX_LENGTH = 16;

    /**
     * Product version max length as specified by the IFAUSAGE service.
     */
    protected static final int PRODUCT_VERSION_MAX_LENGTH = 8;

    /**
     * PID max length as specified by the IFAUSAGE service.
     */
    protected static final int PRODUCT_ID_MAX_LENGTH = 8;

    /**
     * Product qualifier max length as specified by the IFAUSAGE service.
     */
    protected static final int PRODUCT_QUALIFIER_MAX_LENGTH = 8;

    /**
     * Specifies whether or not the product has been registered successfully.
     */
    private boolean registered;

    /**
     * Constructor.
     */
    protected Product(Properties prop) {
        productID = check(prop.getProperty(Product.PRODUCTID));
        owner = check(prop.getProperty(Product.OWNER));
        name = check(prop.getProperty(Product.NAME));
        version = check(prop.getProperty(Product.VERSION));
        pid = check(prop.getProperty(Product.PID));
        qualifier = check(prop.getProperty(Product.QUALIFIER));
        // Replaces is supposed to be null if not set, so don't check() it.
        replaces = prop.getProperty(Product.REPLACES);
        gssp = prop.getProperty(Product.GSSP, "false");
    }

    /**
     * Constructor.
     */
    protected Product(String productID, String owner, String name, String version, String pid, String qualifier, String replaces, String gssp) {
        this.productID = check(productID);
        this.owner = check(owner);
        this.name = check(name);
        this.version = check(version);
        this.pid = check(pid);
        this.qualifier = check(qualifier);
        // Replaces is supposed to be null if not set, so don't check() it.
        this.replaces = replaces;
        this.gssp = (gssp == null) ? "false" : gssp;
    }

    /**
     * @return the product ID (not the PID which is different)
     */
    @Trivial
    protected String productID() {
        return productID;
    }

    /**
     * @return the owner
     */
    @Trivial
    protected String owner() {
        return owner;
    }

    /**
     * @return the name
     */
    @Trivial
    protected String name() {
        return name;
    }

    /**
     * @return the version
     */
    @Trivial
    protected String version() {
        return version;
    }

    /**
     * @return the gssp
     */
    @Trivial
    protected String gssp() {
        return gssp;
    }

    /**
     * This fetches the Version number and Release number from the version string. It assumes the
     * version is something like 8.5.0.0 and will return "8.5". Basically everything up to but not including
     * the second dot. If there is no second dot (or first dot for that matter) you get the whole version string.
     *
     * @return the Version and Release from the version string
     */
    protected String versionRelease() {
        String[] parts = version.split("\\.");
        // split guaranteed to either return an array (or NPE if the input param was null, which it isn;t in this case
        if (parts.length > 1) {
            return parts[0] + "." + parts[1];
        } else {
            // only one part, or no parts..just return the original string
            return version;
        }
    }

    /**
     * @return the pid
     */
    @Trivial
    protected String pid() {
        return pid;
    }

    /**
     * @return the qualifier
     */
    @Trivial
    protected String qualifier() {
        return qualifier;
    }

    /**
     * @return the properties file name of the name this product replaces
     */
    @Trivial
    public String replaces() {
        return replaces;
    }

    /**
     * Tests a property value for null and sets to UNKNOWN if appropriate.
     *
     * @param s The string to check
     * @return The same string or UNKNOWN if it was null
     */
    @Trivial
    protected String check(String s) {
        String t;
        if (s == null) {
            t = UNKNOWN;
        } else {
            t = s;
        }
        return t;
    }

    /**
     * Obtains a byte array representation of the validated and translated product owner.
     *
     * @return The product owner in bytes.
     */
    protected byte[] getOwnerBytes() {
        return (ownerBytes = (ownerBytes != null) ? ownerBytes : validateAndTranslate(owner, PRODUCT_OWNER_MAX_LENGTH, "owner"));
    }

    /**
     * Obtains a byte array representation of the validated and translated product name.
     *
     * @return The product name in bytes.
     */
    protected byte[] getNameBytes() {
        return (nameBytes = (nameBytes != null) ? nameBytes : validateAndTranslate(name, PRODUCT_NAME_MAX_LENGTH, "name"));
    }

    /**
     * Obtains a byte array representation of the validated and translated product version.
     *
     * WAS is eligible for Getting Started Sub Capacity Pricing (GSSP). To prevent the
     * capacity pricing tool (IFAURP) from mistaking us for a usage-based pricing
     * product (normal for products that call IFAUSAGE) we check the gssp property
     * which determines if we are eligible for GSSP. If true, we specify NOTUSAGE as the
     * version, and the version from the properties file is ignored but is in the message
     * to avoid exposing our own silliness to customers.
     *
     * @return The product version in bytes.
     */
    protected byte[] getVersionBytes() {
        return (versionBytes = (gssp.equals("true")) ? validateAndTranslate("NOTUSAGE", PRODUCT_VERSION_MAX_LENGTH,
                                                                            "version") : (versionBytes != null) ? versionBytes : validateAndTranslate(version,
                                                                                                                                                      PRODUCT_VERSION_MAX_LENGTH,
                                                                                                                                                      "version"));
    }

    /**
     * Obtains a byte array representation of the validated and translated PID.
     *
     * @return The PID in bytes.
     */
    protected byte[] getPidBytes() {
        return (pidBytes = (pidBytes != null) ? pidBytes : validateAndTranslate(pid, PRODUCT_ID_MAX_LENGTH, "id"));
    }

    /**
     * Obtains a byte array representation of the validated and translated product qualifier.
     *
     * @return The product name in bytes.
     */
    protected byte[] getQualifierBytes() {
        return (qualifierBytes = (qualifierBytes != null) ? qualifierBytes : validateAndTranslate(qualifier, PRODUCT_QUALIFIER_MAX_LENGTH, "qualifier"));
    }

    /**
     * Sets the registered flag if this product has been registered with the z/OS system.
     *
     * @param registered True if successfully registered with the z/SO system. False otherwise.
     */
    @Trivial
    protected void setRegistered(boolean registered) {
        this.registered = registered;
    }

    /**
     * Retrieves the flag that indicates whether or not this product registered with the z/OS system successfully.
     *
     * @return True if the product successfully registered with the z/OS system. False otherwise.
     */
    protected boolean getRegistered() {
        return registered;
    }

    /**
     * Validate an IFAUSAGE parameter string and convert to an EBCDIC byte array
     *
     * @param str            The Java String for the parameter. Should be blank padded to the right length, if not we'll
     *                           take care of it. Long strings will be truncated.
     * @param requiredLength The right length for this IFAUSAGE paramater
     * @param parm           What parameter are we dealing with? Used for tracing.
     * @return an EBCDIC byte array of the required length or null if we had troubles
     */
    @Trivial
    protected byte[] validateAndTranslate(String str, int requiredLength, String parm) {

        // Declare the return value
        byte[] retVal = null;

        // If you didn't give us a parameter (null or empty string) just set up a single blank
        // and let the native code pad it out
        if ((str == null) || (str.length() == 0)) {
            retVal = convertToEbcdic(" ");
            return retVal;
        }

        // You passed a string.  Good for you.  How long is it?
        int strlen = str.length();

        // If its too long, truncate it and trace to explain to puzzled stack products
        if (strlen > requiredLength) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "RegistrationProcessor::validateAndTranslate, truncating parm " +
                             parm + " from (" + str + ") to (" + str.substring(0, requiredLength) + ")");
            str = str.substring(0, requiredLength);
        }

        // Convert to EBCDIC so MVS can understand
        retVal = convertToEbcdic(str);

        return retVal;
    }

    /**
     * Convert the input string to EBCDIC and handle exceptions
     *
     * @param str A string to convert
     * @return The string, in EBCDIC or a null if conversion failed
     */
    @Trivial
    private byte[] convertToEbcdic(String str) {
        byte[] retVal = null;
        try {
            retVal = str.getBytes("Cp1047");
        } catch (UnsupportedEncodingException uee) {
            // FFDC automatically inserted, null return value already set
        }
        return retVal;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString());
        sb.append(" [Owner: ").append((owner == null) ? "NULL" : owner);
        sb.append(", Name: ").append((name == null) ? "NULL" : name);
        sb.append(", Version: ").append((version == null) ? "NULL" : version);
        sb.append(", ProductId: ").append((productID == null) ? "NULL" : productID);
        sb.append(", PID: ").append((pid == null) ? "NULL" : pid);
        sb.append(", Qualifier: ").append((qualifier == null) ? "NULL" : qualifier);
        sb.append(", Replaces: ").append((replaces == null) ? "NULL" : replaces);
        sb.append(", GSSP: ").append((gssp == null) ? "false" : gssp).append("]");
        return sb.toString();
    }
}
