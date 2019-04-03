/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.ibm.websphere.crypto.InvalidPasswordDecodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEProvider;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.config.xml.internal.nester.Nester;
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

    private final Map<String, SerializableProtectedString> certAliasInfo = new HashMap<String, SerializableProtectedString>();

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
        String fallback_keystore = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_FALLBACK_KEY_STORE_FILE;
        String res = null;
        try {
            res = null;
            res = cfgSvc.resolveString(fallback_keystore);
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

        this.isDefault = LibertyConstants.DEFAULT_KEYSTORE_REF_ID.equals(name);

        if (this.isDefault) {

            // if a type is specified in server.xml, and it's JKS, use the default location for the key.jks keystore; else we'll use PKCS12

            // check if we have an existing key.jks.  If so, use that instead of creating a PKCS12 keystore
            File f = new File(res);

            if (f.exists() && this.location != null && this.location.toLowerCase().endsWith("/key.p12")) {
                // use the JKS file instead, as it exists
                this.location = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_FALLBACK_KEY_STORE_FILE;
                specifiedType = Constants.KEYSTORE_TYPE_JKS;
                this.type = Constants.KEYSTORE_TYPE_JKS;
            }

            // check if they've specified a type, and create the corresponding key.jks or key.p12
            if (type.equals(Constants.KEYSTORE_TYPE_JKS) && this.location != null && this.location.toLowerCase().endsWith("/key.p12")) {
                this.location = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_FALLBACK_KEY_STORE_FILE;
                specifiedType = Constants.KEYSTORE_TYPE_JKS;
                this.type = Constants.KEYSTORE_TYPE_JKS;

            } else if (type.equals(Constants.KEYSTORE_TYPE_PKCS12) && this.location != null && this.location.toLowerCase().endsWith("/key.p12")) {
                this.location = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE;
                specifiedType = Constants.KEYSTORE_TYPE_PKCS12;
                this.type = Constants.KEYSTORE_TYPE_PKCS12;
            } else if (!type.equals(Constants.KEYSTORE_TYPE_JKS)) {
                if (this.location != null && (this.location.toLowerCase().endsWith(".jks"))) {
                    type = LibertyConstants.DEFAULT_FALLBACK_TYPE;
                    specifiedType = type;
                }
            }

            // This is the default key store.. some things we'll just fill in if they
            // are missing... we only do this for the default

            // if no type was specified for the default keytore in server.xml and we have no location, use PKCS12 as the default keystore
            if (this.location == null && specifiedType == null) {
                this.location = LibertyConstants.DEFAULT_OUTPUT_LOCATION + LibertyConstants.DEFAULT_KEY_STORE_FILE;
                specifiedType = this.type = Constants.KEYSTORE_TYPE_PKCS12;
            }

            if (password.isEmpty()) {
                String envPassword = System.getenv("keystore_password");
                if (envPassword != null && !envPassword.isEmpty()) {
                    Tr.audit(tc, "ssl.defaultKeyStore.env.password.CWPKI0820A");
                    password = new SerializableProtectedString(envPassword.toCharArray());
                } else {
                    Tr.info(tc, "ssl.defaultKeyStore.not.created.CWPKI0819I");
                    throw new IllegalArgumentException("Required keystore information is missing, must provide a password for the default keystore");
                }
            }
        } else {
            // this is not the default keystore, but a location has been specified.  If the keystore is JKS type, set the type to JKS
            if (this.location != null
                && (this.location.toUpperCase().endsWith(Constants.KEYSTORE_TYPE_JKS))) {
                specifiedType = Constants.KEYSTORE_TYPE_JKS;
                this.type = Constants.KEYSTORE_TYPE_JKS;
            }
        }

        if (specifiedType == null || location == null) {
            Tr.error(tc, "ssl.keystore.config.error");
            throw new IllegalArgumentException("Required keystore information is missing, must provide a location and type.");
        }

        if (getFileBased().booleanValue()) {
            // resolve paths now
            setLocation(this.location);
        }

        setUpInternalProperties();
        initializeKeyStore(true);
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

                if (!keyStoreType.equalsIgnoreCase(Constants.KEYSTORE_TYPE_JKS) && !keyStoreType.equalsIgnoreCase(Constants.KEYSTORE_TYPE_JCEKS)
                    && !keyStoreType.equalsIgnoreCase(Constants.KEYSTORE_TYPE_PKCS12)) {
                    setProperty(Constants.SSLPROP_KEY_STORE_FILE_BASED, Constants.FALSE);
                }

                if (keyStoreType.equalsIgnoreCase(Constants.KEYSTORE_TYPE_JAVACRYPTO)) {
                    setProperty(Constants.SSLPROP_TOKEN_ENABLED, Constants.TRUE);

                    // set appropriate provider for jvm vendor
                    if (JavaInfo.vendor().equals(JavaInfo.Vendor.IBM))
                        setProperty(Constants.SSLPROP_KEY_STORE_PROVIDER, IBMPKCS11Impl_PROVIDER_NAME);
                    else
                        setProperty(Constants.SSLPROP_KEY_STORE_PROVIDER, SUNPKCS11_PROVIDER_NAME);
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
    public synchronized KeyStore do_getKeyStore(boolean reinitialize, boolean createIfNotPresent) throws Exception {
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
                            return ks1;
                        } // end-storefile-exists

                        if (create || name.endsWith(LibertyConstants.DEFAULT_KEY_STORE_FILE)) {

                            long start = System.currentTimeMillis();
                            Tr.info(tc, "ssl.create.certificate.start");

                            File parentFile = kFile.getParentFile();
                            if (parentFile == null || parentFile.isDirectory() || parentFile.mkdirs()) {
                                try {
                                    String serverName = cfgSvc.getServerName();
                                    // Call Certificate factory to go create the certificate
                                    DefaultSSLCertificateCreator certCreator = DefaultSSLCertificateFactory.getDefaultSSLCertificateCreator();
                                    certCreator.createDefaultSSLCertificate(keyStoreLocation, password, DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                                                            new DefaultSubjectDN(genKeyHostName, serverName).getSubjectDN(),
                                                                            DefaultSSLCertificateCreator.DEFAULT_SIZE, DefaultSSLCertificateCreator.SIGALG);
                                } catch (IllegalArgumentException e) {
                                    // We can state that it is a password error because the keyStoreLocation is already known to be good
                                    // and the validity and DN are default values.
                                    Tr.error(tc, "ssl.create.certificate.password.error");
                                    throw e;
                                } catch (CertificateException e) {
                                    Tr.error(tc, "ssl.create.certificate.error", keyStoreLocation);
                                    throw e;
                                }

                                JSSEProvider jsseProvider = JSSEProviderFactory.getInstance();

                                ks1 = jsseProvider.getKeyStoreInstance(type, provider);

                                is = new URL("file:" + kFile.getCanonicalPath()).openStream();

                                // load the keystore
                                ks1.load(is, password.toCharArray());
                            } else {
                                Tr.error(tc, "ssl.create.certificate.error", keyStoreLocation);
                                throw new SSLException("KeyStore \"" + keyStoreLocation + "\" could not be created.");
                            }

                            Tr.audit(tc, "ssl.create.certificate.end", TimestampUtils.getElapsedTime(start), keyStoreLocation);
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
                    }

                    ks1 = JSSEProviderFactory.getInstance().getKeyStoreInstance(type, provider);

                    // if keyStore does not exists, if it is a default, or if create is
                    // true then load a new one.
                    if (null == is
                        && (create || (name != null && (name.endsWith(Constants.DEFAULT_KEY_STORE) || name.endsWith(Constants.DEFAULT_TRUST_STORE)
                                                        || name.endsWith(Constants.DEFAULT_ROOT_STORE) || name.endsWith(Constants.DEFAULT_DELETED_STORE)
                                                        || name.endsWith(Constants.DEFAULT_SIGNERS_STORE) || name.endsWith("LTPAKeys"))))) {
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
        if (myKeyStore == null || reinitialize) {
            myKeyStore = do_getKeyStore(reinitialize, createIfNotPresent);
        }

        return myKeyStore;
    }

    /**
     * Store the current information into the wrapped keystore.
     *
     * @throws Exception
     */
    public void store() throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "store");

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

            KeyStore ks = getKeyStore(false, false);

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
            }

            // we will likely have to store other types too
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception storing KeyStore; " + e);
            FFDCFilter.processException(e, getClass().getName(), "store", this);
            throw e;
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
                Tr.debug(tc, "Exception initializing KeyStore; " + e);
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
     *             - if the store is read only or not found
     * @throws KeyException
     *             - if an error happens updating the store with the cert
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

        try {
            KeyStore jKeyStore = getKeyStore(false, false);
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
                store();
            } catch (IOException e) {
                // Note: debug + ffdc in store() itself

                // on z/OS we have an issue where the certificate may be stored but the
                // alias
                // already exists in RACF so the keystore API will through an
                // IOException
                // we need to catch this condition on z/OS and if the certs is in the
                // keystore
                // prior after adding the certificate then we know the cert was actually
                // added and its not
                // a true failure. If the cert was not added then we need to rethrow the
                // exception.
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
        }

        // after adding the certificate, clear the keystore and SSL caches so it
        // reloads it.
        AbstractJSSEProvider.clearSSLContextCache();
        mgr.clearJavaKeyStoresFromKeyStoreMap();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "setCertificateEntry");
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

    protected void clearJavaKeyStore() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "clearJavaKeyStore");
        myKeyStore = null;
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
}
