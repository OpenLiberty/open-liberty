/*******************************************************************************
 * Copyright (c) 2005, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.websphere.crypto.InvalidPasswordDecodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEProvider;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.config.xml.nester.Nester;
import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;
import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateFactory;
import com.ibm.ws.crypto.certificateutil.DefaultSubjectDN;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.ssl.JSSEProviderFactory;
import com.ibm.ws.ssl.core.WSPKCSInKeyStore;
import com.ibm.ws.ssl.core.WSPKCSInKeyStoreList;
import com.ibm.ws.ssl.internal.KeystoreConfig;
import com.ibm.ws.ssl.internal.LibertyConstants;
import com.ibm.ws.ssl.provider.AbstractJSSEProvider;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.kernel.service.utils.TimestampUtils;

/**
 * KeyStore instance
 * <p>
 * This class represents a KeyStore configuration in the runtime.
 * </p>
 *
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public class WSKeyStore extends Properties {
    private static final long serialVersionUID = 7497108598211551343L;

    protected static final TraceComponent tc = Tr.register(WSKeyStore.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    protected static final WSPKCSInKeyStoreList pkcsStoreList = new WSPKCSInKeyStoreList();
    /**
     * Flag on whether a Warning has been issued when the default password is
     * found
     */
    private static boolean defaultKeyStoreWarningIssued = false;

    // TODO KeyStore is NOT serializable, this class is broken right now
    /** Java keystore wrapped by this class */
    private KeyStore myKeyStore = null;

    private String name = null;
    private String location = null;

    private String provider = JSSEProviderFactory.getInstance().getKeyStoreProvider();
    private String type = Constants.KEYSTORE_TYPE_PKCS12;
    private Boolean fileBased = Boolean.TRUE;
    private Boolean readOnly = Boolean.FALSE;
    private Boolean initializeAtStartup = Boolean.FALSE;
    private Boolean stashFile = Boolean.FALSE;
    private Map<String, String> customProps = null;
    private Boolean isDefault = false;
    private String genKeyHostName = null;
    private Long pollingRate = null;
    private String trigger = "disabled";

    private SerializableProtectedString password = null;

    private final transient KeystoreConfig cfgSvc;
    private final String KEY_STORE_POLLING_RATE = "pollingRate";
    private final String KEY_STORE_READ_ONLY = "readOnly";
    private final String KEY_STORE_FILE_BASED = "fileBased";
    private final String KEY_STORE_KEYENTRY = "keyEntry";
    private final String KEY_STORE_KEYENTRY_NAME = "name";
    private final String KEY_STORE_KEYENTRY_PASSWORD = "keyPassword";

    private static final String IBMPKCS11Impl_PROVIDER_NAME = "IBMPKCS11Impl";
    private static final String SUNPKCS11_PROVIDER_NAME = "SunPKCS11";

    private final String contextProvider = JSSEProviderFactory.getInstance().getContextProvider();

    /** SafKeyring prefixes **/
    private static final String PREFIX_SAFKEYRING = "safkeyring:";
    private static final String PREFIX_SAFKEYRINGHYBRID = "safkeyringhybrid:";
    private static final String PREFIX_SAFKEYRINGHW = "safkeyringhw:";
    private static final String PREFIX_SAFKEYRINGJCE = "safkeyringjce:";
    private static final String PREFIX_SAFKEYRINGJCEHYBRID = "safkeyringjcehybrid:";
    private static final String PREFIX_SAFKEYRINGJCECCA = "safkeyringjcecca:";

    /** Constant: controller root alias */
    static final String CONTROLLER_ROOT_KEY_ALIAS = "controllerRoot";
    /** Constant: member root alias */
    static final String MEMBER_ROOT_KEY_ALIAS = "memberRoot";
    /** Constant: server identity alias */
    static final String SERVER_IDENTITY_KEY_ALIAS = "serverIdentity";

    private final Map<String, SerializableProtectedString> certAliasInfo = new HashMap<String, SerializableProtectedString>();

    private static final SerializableProtectedString racfPass = new SerializableProtectedString(("password").toCharArray());

    /** Read/Write lock to prevent multiple processes writing the keystore file at the same time, or reading while we are writing. Added for acmeCA feature. **/
    private final ReadWriteLock rwKeyStoreLock = new ReentrantReadWriteLock();

    /**
     * Default constructor, will initialize default values.
     */
    public WSKeyStore() {
        cfgSvc = null;

        // initialize defaults
        setFileBased(true);

        String provider = JSSEProviderFactory.getInstance().getKeyStoreProvider();
        if (null != provider) {
            setProvider(provider);
        }
        setType(Constants.KEYSTORE_TYPE_PKCS12);
        setReadOnly(false);
        setInitializeAtStartup(false);
        setProperty(Constants.SSLPROP_KEY_STORE_CREATE_CMS_STASH, Constants.TRUE);
    }

    WSKeyStore(String _name) { // for testing
        this.name = _name;
        this.cfgSvc = null;
    }

    /**
     * Constructor using a provided name and array of properties.
     *
     * @param _name
     * @param properties
     * @throws Exception
     */
    public WSKeyStore(String _name, Dictionary<String, Object> properties, KeystoreConfig cfgSvc) throws Exception {
        this.name = _name;
        this.cfgSvc = cfgSvc;

        List<Map<String, Object>> keyEntryElements = Nester.nest(KEY_STORE_KEYENTRY, properties);
        saveAliasInformation(keyEntryElements);

        // Let's get the fully resolved path to the default JKS keystore
        String default_location = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE;
        String fallback_keystore = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_FALLBACK_KEY_STORE_FILE;
        String res = null;
        try {
            res = null;
            res = cfgSvc.resolveString(fallback_keystore);
            default_location = cfgSvc.resolveString((default_location));
        } catch (IllegalStateException e) {
            // ignore
        }

        String specifiedType = null;
        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            final Object oValue = properties.get(key);

            if (!(oValue instanceof String)) {
                if (key.equalsIgnoreCase(KEY_STORE_POLLING_RATE) &&
                    oValue instanceof Long) {
                    this.pollingRate = (Long) oValue;
                }
                if (key.equalsIgnoreCase(KEY_STORE_FILE_BASED) &&
                    oValue instanceof Boolean) {
                    this.fileBased = (Boolean) oValue;
                }
                if (key.equalsIgnoreCase(KEY_STORE_READ_ONLY) &&
                    oValue instanceof Boolean) {
                    this.readOnly = (Boolean) oValue;
                }
                continue;
            }

            final String value = (String) oValue;
            if (key.equalsIgnoreCase("location")) {
                this.location = value;
            } else if (key.equalsIgnoreCase("provider")) {
                this.provider = value;
            } else if (key.equalsIgnoreCase("type")) {
                this.type = value;
                specifiedType = value;

            } else if (key.equalsIgnoreCase("initializeAtStartup")) {
                this.initializeAtStartup = Boolean.valueOf(value);
            } else if (key.equalsIgnoreCase("createStashFileForCMS")) {
                this.stashFile = Boolean.valueOf(value);
            } else if (key.equalsIgnoreCase("id") && this.name == null) {
                this.name = value;
            } else if (key.equalsIgnoreCase("genKeyHostName") && this.genKeyHostName == null) {
                this.genKeyHostName = value;
            } else if (key.equalsIgnoreCase("updateTrigger")) {
                this.trigger = value;
            } else {
                // custom property
                if (null == this.customProps) {
                    this.customProps = new HashMap<String, String>();
                }
                this.customProps.put(key, value);
            }
        }

        Object o = properties.get("password");
        if (o != null) {
            if (o instanceof SerializableProtectedString) {
                this.password = (SerializableProtectedString) o;
            } else {
                // Vanishingly small chance this isn't a string. Take the CCE if it happens as evidence if it does
                this.password = new SerializableProtectedString(((String) o).toCharArray());
            }
        } else {
            // Upstream code expects the password to be non-null.
            this.password = SerializableProtectedString.EMPTY_PROTECTED_STRING;
        }

        if (this.type == null || this.location == null) {
            Tr.error(tc, "ssl.keystore.config.error");
            throw new IllegalArgumentException("Required keystore information is missing, must provide a location and type.");
        }

        if ((type.equals(Constants.KEYSTORE_TYPE_JCERACFKS) || type.equals(Constants.KEYSTORE_TYPE_JCECCARACFKS)
             || type.equals(Constants.KEYSTORE_TYPE_JCEHYBRIDRACFKS) || type.equals(Constants.KEYSTORE_TYPE_JAVACRYPTO))) {
            setFileBased(false);
        }

        this.isDefault = LibertyConstants.DEFAULT_KEYSTORE_REF_ID.equals(name);

        if (this.fileBased) {
            if (this.isDefault && !defaultFileExists(this.location, this.type, this.password)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "keystore is default");
                }

                // if a type is specified in server.xml, and it's JKS, use the default location for the key.jks keystore; else we'll use PKCS12
                // check if we have an existing key.jks.  If so, use that instead of creating a PKCS12 keystore
                File f = new File(res);

                //location from metatype sometimes has a extra slashes, need to normalize it
                this.location = this.location.replaceAll("/+", "/");

                if (f.exists() && this.location.toLowerCase().equals(default_location.toLowerCase())) {
                    // use the JKS file instead, as it exists
                    this.location = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_FALLBACK_KEY_STORE_FILE;
                    specifiedType = Constants.KEYSTORE_TYPE_JKS;
                    this.type = Constants.KEYSTORE_TYPE_JKS;
                }

                // check if they've specified a type, and create the corresponding key.jks or key.p12, or re-use the
                // specified one
                if (type.toUpperCase().equals(Constants.KEYSTORE_TYPE_JKS) && this.location.toLowerCase().endsWith("/key.p12")) {
                    this.location = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_FALLBACK_KEY_STORE_FILE;
                    specifiedType = Constants.KEYSTORE_TYPE_JKS;
                    this.type = Constants.KEYSTORE_TYPE_JKS;
                } else if (type.toUpperCase().equals(Constants.KEYSTORE_TYPE_PKCS12) && this.location.toLowerCase().endsWith("/key.p12")) {
                    specifiedType = Constants.KEYSTORE_TYPE_PKCS12;
                    this.type = Constants.KEYSTORE_TYPE_PKCS12;
                } else if (type.toUpperCase().equals(Constants.KEYSTORE_TYPE_PKCS12)) {
                    if (this.location.toLowerCase().endsWith(".jks")) {
                        this.type = LibertyConstants.DEFAULT_FALLBACK_TYPE;
                        specifiedType = type;
                    }
                } else if (type.toUpperCase().equals(Constants.KEYSTORE_TYPE_PKCS12) && this.location.toLowerCase().endsWith(".jks")) {
                    specifiedType = Constants.KEYSTORE_TYPE_JKS;
                    this.type = Constants.KEYSTORE_TYPE_JKS;
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "location: " + location + "type: " + type);
                }
            } else {
                // this is not the default keystore, but a location has been specified.  If the keystore is JKS type, set the type to JKS
                if (this.location.toUpperCase().endsWith(Constants.KEYSTORE_TYPE_JKS)) {
                    specifiedType = Constants.KEYSTORE_TYPE_JKS;
                    this.type = Constants.KEYSTORE_TYPE_JKS;
                }
            }

            if (password.isEmpty() && this.isDefault) {
                String envPassword = System.getenv("keystore_password");
                if (envPassword != null && !envPassword.isEmpty()) {
                    Tr.audit(tc, "ssl.defaultKeyStore.env.password.CWPKI0820A");
                    password = new SerializableProtectedString(envPassword.toCharArray());
                } else {
                    Tr.info(tc, "ssl.defaultKeyStore.not.created.CWPKI0819I");
                    throw new IllegalArgumentException("Required keystore information is missing, must provide a password for the default keystore");
                }
            }

            if (this.location != null) {
                // resolve paths now
                setLocation(this.location);
            }
        } else {
            if ((type.equals(Constants.KEYSTORE_TYPE_JCERACFKS) || type.equals(Constants.KEYSTORE_TYPE_JCECCARACFKS)
                 || type.equals(Constants.KEYSTORE_TYPE_JCEHYBRIDRACFKS))) {
                if (password == null || password.isEmpty())
                    password = racfPass;

                // adjust the location if needed
                location = processKeyringURL(location);
            }

        }

        setUpInternalProperties();
        initializeKeyStore(true);
    }

    /**
     * Return true if the keystore file exists and loads successfully, false otherwise.
     */
    private boolean defaultFileExists(String ksFile, String type, SerializableProtectedString ksPass) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "defaultFileExists", new Object[] { ksFile, type });
        }

        boolean exists = false;
        // check if the file from the configuration exists.
        if (ksFile != null && type != null) {
            File f = new File(ksFile);

            if (f.exists()) {
                //File exists lets try to load it
                KeyStore tmpKs;
                try {
                    tmpKs = KeyStore.getInstance(type);
                    InputStream is = new URL("file:" + f.getCanonicalPath()).openStream();

                    if (ksPass != null && !ksPass.isEmpty()) {
                        String pass = new String(ksPass.getChars());
                        pass = decodePassword(pass);
                        tmpKs.load(is, pass.toString().toCharArray());
                    } else {
                        tmpKs.load(is, null);
                    }
                    exists = true;

                } catch (Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception while trying to find out of the keystore file exists " + e.getMessage());
                    }
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "defaultFileExists", exists);
        }
        return exists;
    }

    /**
     * @param keyEntryElements
     */
    private void saveAliasInformation(List<Map<String, Object>> keyEntryElements) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "saveAliasInformation");
        }

        for (Map<String, Object> keyEntry : keyEntryElements) {
            String alias = (String) keyEntry.get(KEY_STORE_KEYENTRY_NAME);

            SerializableProtectedString certPassword;
            Object o = keyEntry.get(KEY_STORE_KEYENTRY_PASSWORD);
            if (o != null) {
                if (o instanceof SerializableProtectedString) {
                    certPassword = (SerializableProtectedString) o;
                } else {
                    // Vanishingly small chance this isn't a string. Take the CCE if it happens as evidence if it does
                    certPassword = new SerializableProtectedString(((String) o).toCharArray());
                }
            } else {
                // no password entry so don't bother to save the information
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "There is no password for certificate " + alias + " do not save it.");
                }
                continue;
            }

            if (alias != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Adding " + alias + " to key pwd map.");
                }
                certAliasInfo.put(alias, certPassword);
            }

        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "saveAliasInformation");
        }
        return;
    }

    /**
     * Determines if the location is rooted in the server's output location.
     *
     * @param location
     * @return true if the location is under the server's output location; false otherwise.
     */
    private boolean locationInOutputDir(String location) {

        String expandedOutputLocation = cfgSvc.resolveString(LibertyConstants.DEFAULT_OUTPUT_LOCATION);
        return location.startsWith(LibertyConstants.DEFAULT_OUTPUT_LOCATION)
               || location.startsWith(expandedOutputLocation);
    }

    /**
     * Set the physical location of this store to the input value.
     * This method will resolve all symbolic names in the location.
     * If location is just a file name, then we assume its location is
     * the LibertyConstants.DEFAULT_CONFIG_LOCATION.
     *
     * @param _location
     */
    private void setLocation(String _location) {

        String res = null;
        File resFile = null;

        boolean relativePath = true;
        boolean defaultPath = false;

        // try as "absolute" resource (contains symbol, or absolute path)
        try {
            res = cfgSvc.resolveString(_location);
            resFile = new File(res);
            relativePath = !resFile.isAbsolute();
        } catch (IllegalStateException e) {
            // ignore
        }

        if (resFile == null || (!resFile.isFile() && relativePath)) {
            // look for resource in server config location
            try {
                res = cfgSvc.resolveString(LibertyConstants.DEFAULT_CONFIG_LOCATION + _location);
                resFile = new File(res);
            } catch (IllegalStateException e) {
                // ignore
            }

            if (resFile == null || !resFile.isFile()) {
                // fall back to creating for resource in shared output location
                try {
                    res = cfgSvc.resolveString(LibertyConstants.DEFAULT_OUTPUT_LOCATION + _location);
                    resFile = new File(res);
                    defaultPath = true;
                } catch (IllegalStateException e) {
                    // ignore
                }
            }
        }

        // Work against the symbol in the original location
        // The original location may be been
        if (isDefault && (defaultPath || locationInOutputDir(_location))) {
            this.initializeAtStartup = true;
        }

        // reset location w/ resolved value
        // isDefault tested because the default path's file may not exists (and that's OK)
        if ((res != null && resFile.isFile()) || isDefault) {
            this.location = res;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found store under [" + location + "]");
            }
        } else {
            // If it wasn't found then it's likely going to trigger
            // the load.error later. Issue a warning to explain the file
            // could not be found.
            Tr.warning(tc, "ssl.keystore.not.found.warning", res, name);
        }
    }

    /**
     * Constructor.
     *
     * @param keyStore
     */
    private void setUpInternalProperties() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setUpInternalProperties");
        }

        if (getLocation() != null) {
            // KEYSTORE CUSTOM PROPERTIES
            Map<String, String> otherProps = getCustomProps();
            if (otherProps != null) {
                for (Entry<String, String> current : otherProps.entrySet()) {
                    setProperty(current.getKey(), current.getValue());
                }
            }

            // load as key store
            String keyStoreProvider = getProvider();
            if (keyStoreProvider != null) {
                setProperty(Constants.SSLPROP_KEY_STORE_PROVIDER, keyStoreProvider);
            }

            // KEYSTORE NAME
            String keyStoreName = getName();
            if (keyStoreName != null) {
                setProperty(Constants.SSLPROP_KEY_STORE_NAME, keyStoreName);
            }

            // KEYSTORE PASSWORD
            String keyStorePassword = getPassword();
            if (keyStorePassword != null) {
                setProperty(Constants.SSLPROP_KEY_STORE_PASSWORD, keyStorePassword);
            }

            // KEYSTORE LOCATION
            String keyStoreLocation = getLocation();
            if (keyStoreLocation != null) {
                setProperty(Constants.SSLPROP_KEY_STORE, keyStoreLocation);
            }

            // KEYSTORE IS FILE BASED
            if (getFileBased() != null) {
                setProperty(Constants.SSLPROP_KEY_STORE_FILE_BASED, getFileBased().toString());
            }

            // KEYSTORE TYPE
            String keyStoreType = getType();
            if (keyStoreType != null) {
                setProperty(Constants.SSLPROP_KEY_STORE_TYPE, keyStoreType);

                if (keyStoreType.equalsIgnoreCase(Constants.KEYSTORE_TYPE_JAVACRYPTO)) {
                    setProperty(Constants.SSLPROP_TOKEN_ENABLED, Constants.TRUE);

                    // set appropriate provider for jvm vendor
                    if (keyStoreProvider == null) {
                        if (contextProvider.equals(Constants.IBMJSSE2_NAME))
                            setProperty(Constants.SSLPROP_KEY_STORE_PROVIDER, IBMPKCS11Impl_PROVIDER_NAME);
                        else
                            setProperty(Constants.SSLPROP_KEY_STORE_PROVIDER, SUNPKCS11_PROVIDER_NAME);
                    }
                }
            }

            // KEYSTORE IS READ ONLY
            if (getReadOnly() != null)
                setProperty(Constants.SSLPROP_KEY_STORE_READ_ONLY, getReadOnly().toString());

            // KEYSTORE IS INITIALIZED AT STARTUP
            if (getInitializeAtStartup() != null)
                setProperty(Constants.SSLPROP_KEY_STORE_INITIALIZE_AT_STARTUP, getInitializeAtStartup().toString());

            // KEYSTORE STASH FOR CMS
            if (getStashFile() != null)
                setProperty(Constants.SSLPROP_KEY_STORE_CREATE_CMS_STASH, getStashFile().toString());

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setUpInternalProperties");
        }
    }

    /**
     * Set the keystore type to the input value.
     *
     * @param _type
     */
    private void setType(String _type) {
        this.type = _type;
        setProperty(Constants.SSLPROP_KEY_STORE_TYPE, _type);
    }

    /**
     * Set the provider for this keystore to the input value.
     *
     * @param _provider
     */
    private void setProvider(String _provider) {
        this.provider = _provider;
        setProperty(Constants.SSLPROP_KEY_STORE_PROVIDER, _provider);
    }

    /**
     * Set the flag on whether this keystore is filebased or not to the input
     * value.
     *
     * @param flag
     */
    private void setFileBased(Boolean flag) {
        this.fileBased = flag;
        setProperty(Constants.SSLPROP_KEY_STORE_FILE_BASED, flag.toString());
    }

    /**
     * Set the flag on whether this keystore is read only to the input flag.
     *
     * @param flag
     */
    private void setReadOnly(Boolean flag) {
        this.readOnly = flag;
        setProperty(Constants.SSLPROP_KEY_STORE_READ_ONLY, flag.toString());
    }

    /**
     * Set the flag on whether this keystore should initialize at startup.
     *
     * @param flag
     */
    private void setInitializeAtStartup(Boolean flag) {
        this.initializeAtStartup = flag;
        setProperty(Constants.SSLPROP_KEY_STORE_INITIALIZE_AT_STARTUP, flag.toString());
    }

    /**
     * Query the name of this keystore.
     *
     * @return String
     */
    public String getName() {
        return this.name;
    }

    /**
     * Query the location of this keystore.
     *
     * @return String
     */
    public String getLocation() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getLocation -> " + location);
        }
        return this.location;
    }

    /**
     * Query the password of this keystore.
     *
     * @return String
     */
    public String getPassword() {
        return new String(this.password.getChars());
    }

    /**
     * Query the provider of this keystore.
     *
     * @return String
     */
    public String getProvider() {
        return this.provider;
    }

    /**
     * Query the type of this keystore.
     *
     * @return String
     */
    public String getType() {
        return this.type;
    }

    /**
     * Query whether or not this keystore is file based.
     *
     * @return Boolean
     */
    public Boolean getFileBased() {
        return this.fileBased;
    }

    /**
     * Query whether or not this keystore is read only.
     *
     * @return Boolean
     */
    public Boolean getReadOnly() {
        return this.readOnly;
    }

    /**
     * Query whether or not this keystore should initialize at startup.
     *
     * @return Boolean
     */
    public Boolean getInitializeAtStartup() {
        return this.initializeAtStartup;
    }

    /**
     * Query whether or not this keystore is a stash file.
     *
     * @return Boolean
     */
    public Boolean getStashFile() {
        return this.stashFile;
    }

    /**
     * Query the monitor interval
     *
     * @return Boolean
     */
    public long getPollingRate() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getPollingRate returning " + pollingRate);
        return this.pollingRate;
    }

    /**
     * Query the file monitoring trigger
     *
     * @return Boolean
     */
    public String getTrigger() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getTrigger returning " + trigger);
        return this.trigger;
    }

    /**
     * Query the possible custom properties of this keystore.
     *
     * @return Map<String,String>
     */
    public Map<String, String> getCustomProps() {
        return this.customProps;
    }

    public SerializableProtectedString getKeyPassword() {
        if (!certAliasInfo.isEmpty()) {
            Map.Entry<String, SerializableProtectedString> entry = certAliasInfo.entrySet().iterator().next();
            return (entry.getValue());
        } else {
            return (password);
        }
    }

    /**
     * Get the key store wrapped by this object. Synchronized version of
     * getKeyStore().
     *
     * @param reinitialize
     * @param createIfNotPresent
     * @return KeyStore
     * @throws Exception
     */
    private synchronized KeyStore do_getKeyStore(boolean reinitialize, boolean createIfNotPresent) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "do_getKeyStore", new Object[] { Boolean.valueOf(reinitialize), Boolean.valueOf(createIfNotPresent) });

        final String storeFile = this.location;
        final boolean create = createIfNotPresent;

        try {
            myKeyStore = obtainKeyStore(storeFile, create);
        } catch (PrivilegedActionException e) {
            Exception ex = e.getException();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Cannot open keystore URL: " + storeFile + "; " + ex);

            //Return a more HW friendly message if this is PKCS11
            if (getType().equals(Constants.KEYSTORE_TYPE_JAVACRYPTO)) {
                String msg = ex.getMessage();
                if (msg == null) {
                    msg = ex.getCause().getMessage();
                }
                Tr.error(tc, "ssl.hwkeystore.load.error.CWPKI0814E", new Object[] { getName(), storeFile, msg });
            } else {
                Tr.error(tc, "ssl.keystore.load.error.CWPKI0033E", new Object[] { storeFile, ex.getMessage() });
            }

            // Caller creates FFDC
            Tr.warning(tc, "ssl.config.not.used.CWPKI0809W", new Object[] { this.name, this.name });
            throw ex;
        }

        if (isDefault)
            Tr.info(tc, "Successfully loaded default keystore: " + this.location + " of type: " + this.type);

        // Check to see if any certificates entries from the environment need to be added to the keystore
        if (!this.readOnly)
            addCertEntriesFromEnv();
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "The " + name + " keystore is read only will not look for an environment variable cert_" + name + " to be set");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "do_getKeyStore", myKeyStore);
        return myKeyStore;
    }

    protected KeyStore obtainKeyStore(final String storeFile, final boolean create) throws PrivilegedActionException {
        return AccessController.doPrivileged(new PrivilegedExceptionAction<KeyStore>() {
            @Override
            public KeyStore run() throws Exception {
                KeyStore ks1 = null;
                InputStream is = null;

                try {
                    String name = getProperty(Constants.SSLPROP_KEY_STORE_NAME);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Initializing KeyStore: " + name);
                    String password = decodePassword(getProperty(Constants.SSLPROP_KEY_STORE_PASSWORD));
                    String type = getProperty(Constants.SSLPROP_KEY_STORE_TYPE);
                    boolean fileBased = Boolean.parseBoolean(getProperty(Constants.SSLPROP_KEY_STORE_FILE_BASED));
                    String provider = getProperty(Constants.SSLPROP_KEY_STORE_PROVIDER);
                    boolean tokenEnabled = Boolean.parseBoolean(getProperty(Constants.SSLPROP_TOKEN_ENABLED));
                    String createStash = getProperty(Constants.SSLPROP_KEY_STORE_CREATE_CMS_STASH);
                    String keyStoreLocation = null;

                    if (fileBased && storeFile != null) {
                        keyStoreLocation = cfgSvc.resolveString(storeFile);
                        //Tr.info(tc, "File path for store: " + keyStoreLocation + " of type: " + type);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "File path for store: " + keyStoreLocation + " of type: " + type);

                        // Check if the filename exists as a File.
                        File kFile = new File(keyStoreLocation).getAbsoluteFile();

                        if (kFile.exists()) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "Loading keyStore (filebased)");

                            JSSEProvider jsseProvider = JSSEProviderFactory.getInstance();
                            ks1 = jsseProvider.getKeyStoreInstance(type, provider);

                            is = new URL("file:" + kFile.getCanonicalPath()).openStream();
                            // is = openKeyStore(keyStoreLocation);

                            // load the keystore

                            if (password.isEmpty() && (type.equalsIgnoreCase(Constants.KEYSTORE_TYPE_JCEKS) || type.equalsIgnoreCase(Constants.KEYSTORE_TYPE_JKS))) {
                                ks1.load(is, null);
                            } else {
                                ks1.load(is, password.toCharArray());
                            }

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Enumeration<String> e = ks1.aliases();
                                while (e.hasMoreElements()) {
                                    Tr.debug(tc, "alias: " + e.nextElement());
                                }
                            }
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "do_getKeyStore (initialized)");

                            /*
                             * Update the default certificate if necessary.
                             */
                            if (create || name.endsWith(LibertyConstants.DEFAULT_KEY_STORE_FILE)) {
                                try {
                                    DefaultSSLCertificateFactory.getDefaultSSLCertificateCreator().updateDefaultSSLCertificate(ks1, kFile, password);
                                } catch (CertificateException e) {
                                    Tr.error(tc, "ssl.update.certificate.error", keyStoreLocation);
                                    throw e;
                                }
                            }

                            return ks1;
                        } // end-storefile-exists

                        if (create || name.endsWith(LibertyConstants.DEFAULT_KEY_STORE_FILE)) {

                            boolean acmeFailure = false;
                            long start = System.currentTimeMillis();
                            Tr.info(tc, "ssl.create.certificate.start");

                            File parentFile = kFile.getParentFile();
                            if (parentFile == null || parentFile.isDirectory() || parentFile.mkdirs()) {
                                DefaultSSLCertificateCreator certCreator = null;
                                try {
                                    String serverName = cfgSvc.getServerName();
                                    List<String> san = new ArrayList<String>();
                                    String sanString = createCertSANInfo(genKeyHostName);
                                    if (sanString != null)
                                        san.add(sanString);

                                    // Call Certificate factory to go create the certificate
                                    certCreator = DefaultSSLCertificateFactory.getDefaultSSLCertificateCreator();
                                    certCreator.createDefaultSSLCertificate(keyStoreLocation, password, type, provider, DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                                                            new DefaultSubjectDN(genKeyHostName, serverName).getSubjectDN(),
                                                                            DefaultSSLCertificateCreator.DEFAULT_SIZE, DefaultSSLCertificateCreator.SIGALG,
                                                                            san);
                                } catch (IllegalArgumentException e) {
                                    // We can state that it is a password error because the keyStoreLocation is already known to be good
                                    // and the validity and DN are default values.
                                    Tr.error(tc, "ssl.create.certificate.password.error");
                                    throw e;
                                } catch (CertificateException e) {
                                    Tr.error(tc, "ssl.create.certificate.error", Tr.formatMessage(tc, "ssl.create.certificate.error.reason2", e.getMessage()));

                                    /*
                                     * Don't throw exception for ACME failures. We need the the keystore configuration
                                     * to be available so that updates to ACME configuration can still generate the certificate in the keystore. If we throw the exception, the
                                     * keystore configuration dosn't exist and ACME can't generate it.
                                     */
                                    acmeFailure = DefaultSSLCertificateCreator.TYPE_ACME.equals(certCreator.getType());
                                    if (!acmeFailure) {
                                        throw e;
                                    }
                                }

                                JSSEProvider jsseProvider = JSSEProviderFactory.getInstance();

                                ks1 = jsseProvider.getKeyStoreInstance(type, provider);

                                is = new URL("file:" + kFile.getCanonicalPath()).openStream();

                                // load the keystore
                                ks1.load(is, password.toCharArray());
                            } else {
                                Tr.error(tc, "ssl.create.certificate.error", Tr.formatMessage(tc, "ssl.create.certificate.error.reason1", keyStoreLocation));
                                throw new SSLException("KeyStore \"" + keyStoreLocation + "\" could not be created.");
                            }

                            if (!acmeFailure) {
                                Tr.audit(tc, "ssl.create.certificate.end", TimestampUtils.getElapsedTime(start), keyStoreLocation);
                            }
                        } else {
                            throw new SSLException("KeyStore \"" + keyStoreLocation + "\" does not exist.");
                        }

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "do_getKeyStore (loaded)");
                        return ks1;
                    } // end-filebased

                    // check if crypto is enabled...
                    if (tokenEnabled) {
                        // load crypto keystore
                        WSPKCSInKeyStore pKS = null;
                        pKS = pkcsStoreList.insert(type, storeFile, password, true, provider);

                        if (pKS != null) {
                            ks1 = pKS.getKS();
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "do_getKeyStore (created and initialized)");
                            return ks1;
                        }
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "do_getKeyStore (Could not get KeyStore from pkcsStoreList)");
                        throw new SSLException("Could not get KeyStore instance for hardware device.");
                    }

                    // non-filebased, non-hardware crypto...
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Loading keyStore (nonfilebased)");
                    try {
                        // check if it exists
                        is = openKeyStore(storeFile);
                    } catch (Exception e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "KeyStore does not exist");
                        FFDCFilter.processException(e, getClass().getName(), "do_getKeyStore", this);
                        throw e;
                    }

                    ks1 = JSSEProviderFactory.getInstance().getKeyStoreInstance(type, provider);

                    // if keyStore does not exists, if it is a default, or if create is
                    // true then load a new one.
                    if (null == is && (create)) {
                        ks1.load(null, password.toCharArray());
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "do_getKeyStore (loaded)");
                        return ks1;
                    }

                    ks1.load(is, password.toCharArray());

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Enumeration<String> e = ks1.aliases();
                        while (e.hasMoreElements()) {
                            Tr.debug(tc, "alias: " + e.nextElement());
                        }
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "do_getKeyStore (initialized)");
                    return ks1;
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }

        });
    }

    /**
     * Get the key store wrapped by this object.
     *
     * @param reinitialize
     * @param createIfNotPresent
     * @return KeyStore
     * @throws Exception
     */
    public KeyStore getKeyStore(boolean reinitialize, boolean createIfNotPresent) throws Exception {
        return getKeyStore(reinitialize, createIfNotPresent, false);
    }

    /**
     * Get the key store wrapped by this object.
     *
     * @param reinitialize       Reinitialize the keystore?
     * @param createIfNotPresent Create the keystore if not present?
     * @param clone              Return a clone of the keystore?
     * @return The keystore instance.
     * @throws Exception
     */
    private KeyStore getKeyStore(boolean reinitialize, boolean createIfNotPresent, boolean clone) throws Exception {
        /*
         * If we are getting the keyStore to create or reinitialize, we need a write lock now so we can
         * write or set the keyStore later. If we get a read lock now, we can't upgrade to a write lock
         * later (for example, when calling store or setCertificateEntry).
         */
        boolean write = myKeyStore == null || reinitialize || clone;
        if (write) {
            acquireWriteLock();
        } else {
            acquireReadLock();
        }
        try {
            if (write) {
                myKeyStore = do_getKeyStore(reinitialize, createIfNotPresent);
            }
            if (clone) {
                return cloneKeystore(myKeyStore);
            } else {
                return myKeyStore;
            }
        } finally {
            if (write) {
                releaseWriteLock();
            } else {
                releaseReadLock();
            }
        }
    }

    /**
     * Store the current information into the wrapped keystore.
     *
     * @throws Exception
     */
    public void store() throws Exception {
        store(null);
    }

    /**
     * Stores the provided keystore, if not null, otherwise stores
     * the current information in the wrapped keystore.
     *
     * Updating a clone and then storing it prevents a caller from getting a
     * keystore that we are actively changing.
     *
     * @param clonedKeyStore
     * @throws Exception
     */
    public void store(KeyStore clonedKeyStore) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "store");

        acquireWriteLock();

        try {
            String name = getProperty(Constants.SSLPROP_KEY_STORE_NAME);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Storing KeyStore " + name);

            String SSLKeyFile = getProperty(Constants.SSLPROP_KEY_STORE);
            String SSLKeyPassword = decodePassword(getProperty(Constants.SSLPROP_KEY_STORE_PASSWORD));
            String SSLKeyStoreType = getProperty(Constants.SSLPROP_KEY_STORE_TYPE);

            boolean readOnly = Boolean.parseBoolean(getProperty(Constants.SSLPROP_KEY_STORE_READ_ONLY));
            boolean fileBased = Boolean.parseBoolean(getProperty(Constants.SSLPROP_KEY_STORE_FILE_BASED));
            String SSLKeyStoreStash = getProperty(Constants.SSLPROP_KEY_STORE_CREATE_CMS_STASH);

            KeyStore ks = clonedKeyStore == null ? getKeyStore(false, false) : clonedKeyStore;

            if (ks != null && !readOnly) {
                if (fileBased) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Storing filebased keystore type " + SSLKeyStoreType);
                    String keyStoreLocation = SSLKeyFile;
                    String keyStorePassword = SSLKeyPassword;
                    final FileOutputStream fos = new FileOutputStream(keyStoreLocation);
                    try {
                        ks.store(fos, keyStorePassword.toCharArray());
                    } finally {
                        fos.close();
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Storing non-filebased keystore type " + SSLKeyStoreType);
                    String keyStoreLocation = SSLKeyFile;
                    String keyStorePassword = SSLKeyPassword;
                    URL ring = new URL(keyStoreLocation);
                    URLConnection ringConnect = ring.openConnection();
                    final OutputStream fos = ringConnect.getOutputStream();
                    try {
                        ks.store(fos, keyStorePassword.toCharArray());
                    } finally {
                        fos.close();
                    }
                }

                if (clonedKeyStore != null) {
                    myKeyStore = ks;
                }
            }

            // we will likely have to store other types too
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception storing KeyStore; " + e);
            FFDCFilter.processException(e, getClass().getName(), "store", this);
            throw e;
        } finally {
            releaseWriteLock();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "store");
    }

    /**
     * Initialize the wrapped keystore.
     *
     * @param reinitialize
     * @throws Exception
     */
    public void initializeKeyStore(boolean reinitialize) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "initializeKeyStore");

        try {
            String initAtStartup = getProperty(Constants.SSLPROP_KEY_STORE_INITIALIZE_AT_STARTUP);
            boolean createIfMissing = LibertyConstants.DEFAULT_KEYSTORE_REF_ID.equals(getProperty("id"));

            if (Boolean.parseBoolean(initAtStartup) || reinitialize) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Initializing keystore at startup.");
                getKeyStore(reinitialize, createIfMissing);
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception initializing KeyStore; " + e, e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "initializeKeyStore");
    }

    /**
     * Cycle through the keystore looking for expired certificates. Print a
     * warning for any certificates that are expired or will expire during the
     * input interval.
     *
     * @param daysBeforeExpireWarning
     * @param keyStoreName
     * @throws Exception
     */
    public void provideExpirationWarnings(int daysBeforeExpireWarning, String keyStoreName) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "provideExpirationWarnings", Integer.valueOf(daysBeforeExpireWarning));

        KeyStore keystore = getKeyStore(false, false);

        if (keystore != null) {
            try {
                Enumeration<String> e = keystore.aliases();
                if (e != null) {
                    for (; e.hasMoreElements();) {
                        String alias = e.nextElement();
                        if (null == alias)
                            continue;
                        Certificate[] cert_chain = keystore.getCertificateChain(alias);
                        if (null == cert_chain)
                            continue;
                        for (int i = 0; i < cert_chain.length; i++) {
                            printWarning(daysBeforeExpireWarning, keyStoreName, alias, (X509Certificate) cert_chain[i]);
                        }
                    }
                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Exception validating KeyStore expirations; " + e);
                FFDCFilter.processException(e, getClass().getName(), "provideExpirationWarnings", this);
                throw e;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "provideExpirationWarnings");
    }

    public boolean isCollectiveCertSubjectAltNamesExist(WSKeyStore wsKeystore, String ksName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "isCollectiveCertSubjectAltNamesExist " + ksName);
        KeyStore keystore = null;
        try {
            keystore = wsKeystore.getKeyStore(false, false, false);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception getting the keystore, " + e);
        }
        if (keystore != null) {
            try {
                Enumeration<String> e = keystore.aliases();
                if (e != null) {
                    for (; e.hasMoreElements();) {
                        String alias = e.nextElement();
                        if (null == alias)
                            continue;
                        Certificate[] cert_chain = keystore.getCertificateChain(alias);
                        if (null == cert_chain)
                            continue;
                        for (int i = 0; i < cert_chain.length; i++) {
                            if (isDefaultServerIdentityCert((X509Certificate) cert_chain[i], alias)) {
                                if (!isSanExist((X509Certificate) cert_chain[i])) {
                                    Tr.warning(tc, "ssl.san.warning.CWPKI0050W", new Object[] { alias, ksName });
                                    return false;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Exception checking certificate subject alternative names: " + e);
                FFDCFilter.processException(e, getClass().getName(), "isSubjectAltNamesExist", this);
                return false;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "isCollectiveCertSubjectAltNamesExist ", true);
        return true;
    }

    private boolean isDefaultServerIdentityCert(X509Certificate x509Certificate, String alias) {
        boolean result = false;
        String issuerDN = x509Certificate.getIssuerX500Principal().getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.debug(tc, "alias: " + alias + " issuer DN: " + issuerDN);
        if (issuerDN != null && (issuerDN.contains(MEMBER_ROOT_KEY_ALIAS) || issuerDN.contains(CONTROLLER_ROOT_KEY_ALIAS))) {
            String subjectDN = x509Certificate.getSubjectX500Principal().getName();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.debug(tc, "subject DN: " + subjectDN);
            if (!subjectDN.contains(MEMBER_ROOT_KEY_ALIAS) && !subjectDN.contains(CONTROLLER_ROOT_KEY_ALIAS)) {
                result = true;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "isDefaultServerIdentityCert ", result);
        return result;
    }

    private boolean isSanExist(X509Certificate x509Certificate) {
        boolean result = false;
        try {
            Collection<List<?>> san = x509Certificate.getSubjectAlternativeNames();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.debug(tc, "Certificate's Subject DN: " + x509Certificate.getSubjectX500Principal().getName() + " Subject Alternative Name: "
                             + (san != null ? "<NOT NULL>" : "<NULL>"));
            if (san != null)
                result = true;

        } catch (CertificateParsingException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception getting Certifificate subject alternative name, " + e);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "isSanExist ", result);
        return result;
    }

    /**
     * Print a warning about a certificate being expired or soon to be expired in
     * the keystore.
     *
     * @param daysBeforeExpireWarning
     * @param keyStoreName
     * @param alias
     * @param cert
     */
    public void printWarning(int daysBeforeExpireWarning, String keyStoreName, String alias, X509Certificate cert) {
        try {
            long millisDelta = ((((daysBeforeExpireWarning * 24L) * 60L) * 60L) * 1000L);
            long millisBeforeExpiration = cert.getNotAfter().getTime() - System.currentTimeMillis();
            long daysLeft = ((((millisBeforeExpiration / 1000L) / 60L) / 60L) / 24L);

            // cert is already expired
            if (millisBeforeExpiration < 0) {
                Tr.error(tc, "ssl.expiration.expired.CWPKI0017E", new Object[] { alias, keyStoreName });
            } else if (millisBeforeExpiration < millisDelta) {
                Tr.warning(tc, "ssl.expiration.warning.CWPKI0016W", new Object[] { alias, keyStoreName, Long.valueOf(daysLeft) });
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "The certificate with alias " + alias + " from keyStore " + keyStoreName + " has " + daysLeft + " days left before expiring.");
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception reading KeyStore certificates during expiration check; " + e);
            FFDCFilter.processException(e, getClass().getName(), "printWarning", this);
        }
    }

    /***
     * Decode an individual password property.
     *
     * @param password
     * @return String
     */
    public static String decodePassword(@Sensitive String password) {
        String decodedPassword = null;
        // first try to decode
        if (password == null || password.isEmpty())
            return password;

        try {
            decodedPassword = PasswordUtil.decode(password);

            if (decodedPassword != null && !WSKeyStore.defaultKeyStoreWarningIssued && decodedPassword.equals(Constants.DEFAULT_KEYSTORE_PASSWORD)) {
                Tr.warning(tc, "ssl.default.password.in.use.CWPKI0041W");
                defaultKeyStoreWarningIssued = true;
            }
        } catch (InvalidPasswordDecodingException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Password was not decoded.");
            decodedPassword = password;
            // password is not encoded
        } catch (Exception e) {
            // password is encoded, but could not be decoded
            FFDCFilter.processException(e, WSKeyStore.class.getName(), "decodePassword");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception decoding KeyStore password; " + e);
        }
        return decodedPassword;
    }

    /**
     * The purpose of this method is to open the passed in file which represents
     * the key store.
     *
     * @param fileName
     * @return InputStream
     * @throws MalformedURLException
     * @throws IOException
     */
    public static InputStream openKeyStore(String fileName) throws MalformedURLException, IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "openKeyStore: " + fileName);

        URL urlFile = null;

        // Check if the filename exists as a File.
        File kfile = new File(fileName);

        if (kfile.exists() && kfile.length() == 0) {
            throw new IOException("Keystore file exists, but is empty: " + fileName);
        } else if (!kfile.exists()) {
            urlFile = new URL(fileName);
        } else {
            // kfile exists
            urlFile = new URL("file:" + kfile.getCanonicalPath());
        }

        // Finally open the file.
        InputStream fis = urlFile.openStream();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "openKeyStore: " + (null != fis));
        return fis;
    }

    @Override
    public String toString() {
        Enumeration<?> e = propertyNames();
        StringBuilder buf = new StringBuilder(128);
        buf.append("WSKeyStore.toString() {\n");

        while (e.hasMoreElements()) {
            String propName = (String) e.nextElement();
            String value = getProperty(propName);

            if (propName.toLowerCase().indexOf("password") != -1) {
                buf.append(propName);
                buf.append('=');
                buf.append(SSLConfigManager.mask(value));
                buf.append('\n');
            } else {
                buf.append(propName);
                buf.append('=');
                buf.append(value);
                buf.append('\n');
            }
        }
        buf.append('}');
        return buf.toString();
    }

    /**
     * Set a new certificate into the keystore and save the updated store.
     *
     * @param alias
     * @param cert
     * @throws KeyStoreException
     *                               - if the store is read only or not found
     * @throws KeyException
     *                               - if an error happens updating the store with the cert
     */
    public void setCertificateEntry(String alias, Certificate cert) throws KeyStoreException, KeyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "setCertificateEntry", new Object[] { alias, cert });
        }
        if (Boolean.parseBoolean(getProperty(Constants.SSLPROP_KEY_STORE_READ_ONLY))) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Unable to update readonly store");
            }
            throw new KeyStoreException("Unable to add to read-only store");
        }
        final KeyStoreManager mgr = KeyStoreManager.getInstance();

        acquireWriteLock();
        try {
            KeyStore jKeyStore = getKeyStore(false, false, true);
            if (null == jKeyStore) {
                final String keyStoreLocation = getProperty(Constants.SSLPROP_KEY_STORE);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Cannot load the Java keystore at location \"" + keyStoreLocation + "\"");
                }
                throw new KeyStoreException("Cannot load the Java keystore at location \"" + keyStoreLocation + "\"");
            }

            // store the cert... errors are thrown if conflicts or errors occur
            jKeyStore.setCertificateEntry(alias, cert);

            try {
                store(jKeyStore);
            } catch (IOException e) {
                // Note: debug + ffdc in store() itself

                /*
                 * On z/OS we have an issue where the certificate may be stored but the
                 * alias already exists in RACF so the keystore API will throw an
                 * IOException.
                 *
                 * We need to catch this condition on z/OS and if the certs is in the
                 * keystore prior after adding the certificate then we know the cert was
                 * actually added and its not a true failure. If the cert was not added
                 * then we need to rethrow the exception.
                 */
                final String ksType = getProperty(Constants.SSLPROP_KEY_STORE_TYPE);
                if ((ksType.equals(Constants.KEYSTORE_TYPE_JCERACFKS) || ksType.equals(Constants.KEYSTORE_TYPE_JCECCARACFKS)
                     || ksType.equals(Constants.KEYSTORE_TYPE_JCEHYBRIDRACFKS))) {
                    KeyStore ks = getKeyStore(true, false);

                    if (mgr.checkIfSignerAlreadyExistsInTrustStore((X509Certificate) cert, ks)) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Certificate already exists in RACF: " + e.getMessage());
                        }
                    } else {
                        throw new KeyException(e.getMessage(), e);
                    }
                } else {
                    throw new KeyException(e.getMessage(), e);
                }
            }
        } catch (KeyStoreException kse) {
            throw kse;
        } catch (KeyException ke) {
            throw ke;
        } catch (Exception e) {
            throw new KeyException(e.getMessage(), e);
        } finally {
            releaseWriteLock();
        }

        // after adding the certificate, clear the keystore and SSL caches so it
        // reloads it.
        AbstractJSSEProvider.clearSSLContextCache();
        mgr.clearJavaKeyStoresFromKeyStoreMap();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "setCertificateEntry");
        }
    }

    /**
     * Set a new key entry into the keystore and save the updated store.
     *
     * @param alias
     * @param key
     * @param password
     * @param chain
     * @throws KeyStoreException
     *                               - if the store is read only or not found
     * @throws KeyException
     *                               - if an error happens updating the store with the cert
     */
    @Sensitive
    public void setKeyEntry(String alias, Key key, Certificate[] chain) throws KeyStoreException, KeyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "setKeyEntry", new Object[] { alias, chain });
        }
        if (Boolean.parseBoolean(getProperty(Constants.SSLPROP_KEY_STORE_READ_ONLY))) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Unable to update readonly store");
            }
            throw new KeyStoreException("Unable to add to read-only store");
        }
        final KeyStoreManager mgr = KeyStoreManager.getInstance();

        acquireWriteLock();
        try {
            KeyStore jKeyStore = getKeyStore(false, false, true);
            if (null == jKeyStore) {
                final String keyStoreLocation = getProperty(Constants.SSLPROP_KEY_STORE);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Cannot load the Java keystore at location \"" + keyStoreLocation + "\"");
                }
                throw new KeyStoreException("Cannot load the Java keystore at location \"" + keyStoreLocation + "\"");
            }

            SerializableProtectedString keyPWD = getKeyPassword(alias);
            if (keyPWD == null) {
                keyPWD = this.password;
            }

            // The password may be encoded (especially if loaded from the config)
            String decodedPassword = decodePassword(new String(keyPWD.getChars()));

            // store the key... errors are thrown if conflicts or errors occur
            jKeyStore.setKeyEntry(alias, key, decodedPassword.toCharArray(), chain);
            store(jKeyStore);
        } catch (KeyStoreException kse) {
            throw kse;
        } catch (KeyException ke) {
            throw ke;
        } catch (Exception e) {
            throw new KeyException(e.getMessage(), e);
        } finally {
            releaseWriteLock();
        }

        // after adding the key entry, clear the keystore and SSL caches so it
        // reloads it.
        AbstractJSSEProvider.clearSSLContextCache();
        mgr.clearJavaKeyStoresFromKeyStoreMap();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "setKeyEntry");
        }
    }

    public Key getKey(String alias, @Sensitive String keyPassword) throws KeyStoreException, CertificateException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getKey: " + alias);
        Key key = null;
        try {
            SerializableProtectedString keyPWD = null;
            KeyStore jKeyStore = getKeyStore(false, false);
            if (jKeyStore == null) {
                throw new KeyStoreException("The keystore [" + name + "] is not present in the configuration");
            }
            if (!jKeyStore.isKeyEntry(alias)) {
                throw new CertificateException("The alias [" + alias + "] is not present in the KeyStore as a key entry");
            } else {
                if (keyPassword != null) {
                    keyPWD = new SerializableProtectedString((keyPassword).toCharArray());
                } else {
                    keyPWD = getKeyPassword(alias);

                    if (keyPWD == null) {
                        keyPWD = this.password;
                    }
                }

                // The password may be encoded (especially if loaded from the config)
                String decodedPassword = decodePassword(new String(keyPWD.getChars()));
                key = jKeyStore.getKey(alias, decodedPassword.toCharArray());
            }
        } catch (CertificateException e) {
            throw e;
        } catch (KeyStoreException e) {
            throw e;
        } catch (Exception e) {
            Tr.error(tc, "ssl.key.error.CWPKI0812E", new Object[] { alias, this.name, e.getMessage() });
            throw new KeyStoreException("Unexpected error while loading the requested private key for alias [" + alias + "] from keystore: " + name, e);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getKey");
        return key;
    }

    /*
     * Return the certificate aliases in the keystore
     */
    public Enumeration<String> aliases() throws KeyStoreException, KeyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "aliases");
        Enumeration<String> aliases = null;
        try {
            KeyStore jKeyStore = getKeyStore(false, false);
            if (jKeyStore == null) {
                throw new KeyStoreException("The keystore [" + name + "] is not present in the configuration");
            }
            aliases = jKeyStore.aliases();
        } catch (KeyStoreException e) {
            throw e;
        } catch (Exception ex) {
            throw new KeyException(ex.getMessage(), ex);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "aliases: " + aliases);
        return aliases;
    }

    /*
     * return true if the entry is a key and false if not
     */
    public boolean isKeyEntry(String alias) throws KeyStoreException, KeyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "isKeyEntry: " + alias);
        boolean isKey = false;
        try {
            KeyStore jKeyStore = getKeyStore(false, false);
            if (jKeyStore == null) {
                throw new KeyStoreException("The keystore [" + name + "] is not present in the configuration");
            }
            isKey = jKeyStore.isKeyEntry(alias);
        } catch (KeyStoreException e) {
            throw e;
        } catch (Exception ex) {
            throw new KeyException(ex.getMessage(), ex);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "isKeyEntry: " + isKey);
        return isKey;
    }

    /**
     * Query the password of a key entry in this keystore.
     *
     * @param alias
     * @return
     */
    private SerializableProtectedString getKeyPassword(String alias) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getKeyPassword " + alias);

        SerializableProtectedString keyPass = certAliasInfo.get(alias);
        if (keyPass != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getKeyPassword entry found.");
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getKeyPassword -> null");
        }
        return keyPass;
    }

    private void addCertEntriesFromEnv() {
        //See if there are any certs from the env that need to be added to this keystore
        String key = "cert_" + name;
        CertificateEnvHelper certEnv = new CertificateEnvHelper();
        List<Certificate> certs = certEnv.getCertificatesForKeyStore(name);

        try {
            for (int i = 0; i < certs.size(); i++) {
                X509Certificate cert = (X509Certificate) certs.get(i);
                String subject = cert.getSubjectX500Principal().getName();
                String envKey = "cert_" + name;
                Tr.info(tc, "ssl.certificate.add.CWPKI0830I", new Object[] { subject, envKey, name });

                // add the certificate to the keystore with an alias format: envcert-[index]-cert_[keystorename]
                String alias = "envcert-" + String.valueOf(i) + "-" + key;
                setCertificateEntryNoStore(alias.toLowerCase(), cert);
            }

        } catch (Exception e) {
            String extendedMsg = e.getMessage();
            Tr.warning(tc, "ssl.environment.cert.error.CWPKI0826W", key, name, extendedMsg);
        }
    }

    /**
     * Set a new certificate into the keystore
     *
     * @param alias
     *
     * @param cert
     * @throws KeyStoreException
     *                               - if the store is read only or not found
     * @throws KeyException
     *                               - if an error happens updating the store with the cert
     */
    private void setCertificateEntryNoStore(String alias, Certificate cert) throws KeyStoreException, KeyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "setCertificateEntryNoStore", new Object[] { alias, cert });
        }
        acquireWriteLock();
        try {
            if (myKeyStore != null) {
                myKeyStore.setCertificateEntry(alias, cert);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Certificate " + alias + " set to keystore " + name);
                }
            }
        } catch (KeyStoreException kse) {
            throw kse;
        } catch (Exception e) {
            throw new KeyException(e.getMessage(), e);
        } finally {
            releaseWriteLock();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "setCertificateEntryNoStore");
        }
    }

    protected void clearJavaKeyStore() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "clearJavaKeyStore");
        acquireWriteLock();
        try {
            myKeyStore = null;
        } finally {
            releaseWriteLock();
        }
    }

    public static String getCannonicalPath(String location, Boolean fileBased) {
        String cannonicalLocation = location;
        if (fileBased) {
            //Try to create File object based on Location to get the cannonical path
            try {
                cannonicalLocation = new File(location).getCanonicalPath();
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Exception finding full file path. Setting back to default");
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getCannonicalLocation -> " + cannonicalLocation);
        }
        return cannonicalLocation;
    }

    private String createCertSANInfo(String hostname) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "createCertSANInfo: " + hostname);
        ArrayList<String> san = new ArrayList<String>();
        String buildSanString = null;
        String sanTag = "SAN=";

        try {
            if (hostname != null && !hostname.isEmpty()) {
                if (hostname.equalsIgnoreCase("localhost")) {
                    String host = InetAddress.getLocalHost().getCanonicalHostName();
                    san.add("dns:" + removeDots(host));
                }
                InetAddress addr;
                // get the InetAddress if there is one
                addr = getInetAddress(hostname);
                if (addr != null && addr.toString().startsWith("/"))
                    san.add("ip:" + hostname);
                else {
                    san.add("dns:" + removeDots(hostname));
                }
            } else {
                String host = InetAddress.getLocalHost().getCanonicalHostName();
                san.add("dns:" + removeDots(host));
            }
            String ipAddresses = DefaultSubjectDN.buildSanIpStringFromNetworkInterface();
            if (ipAddresses != null)
                san.add(ipAddresses);
        } catch (UnknownHostException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "createCertSANInfo exception -> " + e.getMessage());
            }
            // return null, do not set a SAN if there is a failure here
        }

        if (!san.isEmpty()) {
            for (String sanEntry : san) {
                if (buildSanString != null)
                    buildSanString = buildSanString + "," + sanEntry;
                else
                    buildSanString = sanTag + sanEntry;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "createCertSANInfo: " + buildSanString);
        return (buildSanString);

    }

    /**
    * Removes leading and trailing dots from a DNS string.
    * @param dnsString the DNS string to be processed
    * @return the DNS string with leading and trailing dots removed
    */
    String removeDots(String dnsString) {
        return dnsString.replaceAll("^\\.", "").replaceAll("\\.$", "");
    }
    
    /**
     * @param hostname
     * @return
     */
    private InetAddress getInetAddress(String hostname) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getInetAddress: " + hostname);
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName(hostname);
        } catch (Exception e) {
            //hostname likely does not resolve to an address
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getInetAddress: " + addr);
        return addr;
    }

    /*
     * checking for a valid dnsName value, the SAN entry is pretty specific.
     * Only alphas, digits, and hyphin are allowed. A period is allowed when domain are added.
     * No part of the domain name can start with a digit
     * The dnsName can not start or end with a period, and there can not be any empty component of the domain name
     */

    /*
     * Removing isGoodDNSName as we do not believe we need this any longer.
     * The check for the first character being numeric is invalid because this is allowed since RFC1123.
     * We will no longer validate hostnames before adding them into the SAN and will delegate that to the certificate creation tool
     *
     * public static boolean isGoodDNSName(String dnsName) {
     * if (tc.isEntryEnabled())
     * Tr.entry(tc, "isGoodDNSName", dnsName);
     * String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
     * String validCharsString = alpha + "0123456789-.";
     * // Make sure the first character is not a digit.
     * if (Character.isDigit(dnsName.charAt(0))) {
     * if (tc.isEntryEnabled())
     * Tr.exit(tc, "isGoodDNSName - dnsName starts with digit", false);
     * return false;
     * }
     * // make sure the dnsName does not start or end with a period
     * if (dnsName.charAt(0) == '.' || dnsName.charAt(dnsName.length() - 1) == '.') {
     * if (tc.isEntryEnabled())
     * Tr.exit(tc, "isGoodDNSName - dnsName starts or ends with a '.' ", false);
     * return false;
     * }
     * // Make sure there are no unacceptable characters in the dnsName
     * for (int i = 0; i < dnsName.length(); i++) {
     * char x = dnsName.charAt(i);
     * if (validCharsString.indexOf(x) < 0) {
     * if (tc.isEntryEnabled())
     * Tr.exit(tc, "isGoodDNSName - dnsName contains invalid character", false);
     * return false;
     * }
     * }
     * // look at the domain parts
     * for (int endIndex, startIndex = 0; startIndex < dnsName.length(); startIndex = endIndex + 1) {
     * endIndex = dnsName.indexOf('.', startIndex);
     * // getting part of the domain name
     * if (endIndex < 0) {
     * endIndex = dnsName.length();
     * }
     * // DNSName SubjectAltNames with empty components are not permitted
     * if ((endIndex - startIndex) < 1) {
     * if (tc.isEntryEnabled())
     * Tr.exit(tc, "isGoodDNSName - dnsName domain section is empty", false);
     * return false;
     * }
     * //DNSName components must begin with a letter A-Z or a-z
     * if (alpha.indexOf(dnsName.charAt(startIndex)) < 0) {
     * if (tc.isEntryEnabled())
     * Tr.exit(tc, "isGoodDNSName - dnsName domain part starts with a digit", false);
     * return false; //DNSName components must begin with a letter
     * }
     * }
     * if (tc.isEntryEnabled())
     * Tr.exit(tc, "isGoodDNSName", true);
     * return true;
     * }
     *
     *
     */

    /**
     * Acquire the writer lock. To be used to prevent concurrent keystore
     * fetching and storing. Must be used with releaseWriteLock
     */
    @Trivial
    private void acquireWriteLock() {
        rwKeyStoreLock.writeLock().lock();
    }

    /**
     * Release the writer lock. To be used to prevent concurrent keystore
     * fetching and storing. Must be used with acquireWriteLock
     */
    @Trivial
    private void releaseWriteLock() {
        rwKeyStoreLock.writeLock().unlock();
    }

    /**
     * Acquire the reader lock. To be used to prevent concurrent keystore
     * fetching and storing. Must be used with releaseReadLock
     */
    @Trivial
    private void acquireReadLock() {
        rwKeyStoreLock.readLock().lock();
    }

    /**
     * Release the read lock. To be used to prevent concurrent keystore
     * fetching and storing. Must be used with acquireReadLock
     */
    @Trivial
    private void releaseReadLock() {
        rwKeyStoreLock.readLock().unlock();
    }

    /**
     * Clone the keystore. If an exception occurs, return the original
     * keystore as "best effort" instead of failing.
     *
     * @param original
     * @return
     */
    @Trivial
    private KeyStore cloneKeystore(KeyStore original) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            original.store(baos, password.getChars());
            KeyStore clone = JSSEProviderFactory.getInstance().getKeyStoreInstance(type, provider);
            clone.load(new ByteArrayInputStream(baos.toByteArray()), password.getChars());
            return clone;
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "cloneKeystore hit an exception, will return original keystore.", t);
            }
        }
        return original;
    }

    public static String processKeyringURL(String safKeyringURL) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "processKeyringURL: " + safKeyringURL);
        String processedUrl = safKeyringURL;

        //Check the prefix first,  it may need to be converted first
        String replacePrefix = null;
        if (JavaInfo.majorVersion() >= 11) {
            if (processedUrl.startsWith(PREFIX_SAFKEYRING))
                replacePrefix = PREFIX_SAFKEYRINGJCE;
            else if (processedUrl.startsWith(PREFIX_SAFKEYRINGHYBRID))
                replacePrefix = PREFIX_SAFKEYRINGJCEHYBRID;
            else if (processedUrl.startsWith(PREFIX_SAFKEYRINGHW))
                replacePrefix = PREFIX_SAFKEYRINGJCECCA;
        } else {
            if (processedUrl.startsWith(PREFIX_SAFKEYRINGJCE))
                replacePrefix = PREFIX_SAFKEYRING;
            else if (processedUrl.startsWith(PREFIX_SAFKEYRINGJCEHYBRID))
                replacePrefix = PREFIX_SAFKEYRINGHYBRID;
            else if (processedUrl.startsWith(PREFIX_SAFKEYRINGJCECCA))
                replacePrefix = PREFIX_SAFKEYRINGHW;
        }

        if (replacePrefix != null) {
            int index = processedUrl.indexOf(":");
            String removedPrefix = processedUrl.substring(index + 1);
            processedUrl = replacePrefix + removedPrefix;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "processKeyringURL: " + processedUrl);

        return processedUrl;
    }

}
