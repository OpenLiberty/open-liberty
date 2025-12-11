/*******************************************************************************
 * Copyright (c) 2005, 2024 IBM Corporation and others.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ssl.core.WSPKCSInKeyStore;
import com.ibm.ws.ssl.core.WSPKCSInKeyStoreList;

/**
 * KeyStore configuration manager
 * <p>
 * This class handles the configuring, loading/reloading, verifying, etc. of
 * KeyStore objects in the runtime.
 * </p>
 *
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public class KeyStoreManager {
    protected static final TraceComponent tc = Tr.register(KeyStoreManager.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    private static class Singleton {
        static final KeyStoreManager INSTANCE = new KeyStoreManager();
    }

    private final Map<String, WSKeyStore> keyStoreMap = new HashMap<String, WSKeyStore>();
    private static WSPKCSInKeyStoreList pkcsStoreList = new WSPKCSInKeyStoreList();

    /** HEX character list */
    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /**
     * Do nothing constructor, used to enforce singleton model.
     */
    private KeyStoreManager() {
    }

    /**
     * Access the singleton instance of the key store manager.
     *
     * @return KeyStoreManager
     */
    public static KeyStoreManager getInstance() {
        return Singleton.INSTANCE;
    }

    /**
     * Load the provided list of keystores from the configuration.
     *
     * @param config
     */
    public void loadKeyStores(Map<String, WSKeyStore> config) {
        // now process each keystore in the provided config
        for (Entry<String, WSKeyStore> current : config.entrySet()) {
            try {
                String name = current.getKey();
                WSKeyStore keystore = current.getValue();
                addKeyStoreToMap(name, keystore);
            } catch (Exception e) {
                FFDCFilter.processException(e, getClass().getName(), "loadKeyStores", new Object[] { this, config });
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Error loading keystore; " + current.getKey() + " " + e);
                }
            }
        }
    }

    /***
     * Adds the keyStore to the keyStoreMap.
     *
     * @param keyStoreName
     * @param ks
     * @throws Exception
     ***/
    public void addKeyStoreToMap(String keyStoreName, WSKeyStore ks) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "addKeyStoreToMap", "name=" + keyStoreName + ", ks=" + ks);

        if (keyStoreMap.containsKey(keyStoreName))
            keyStoreMap.remove(keyStoreName);
        keyStoreMap.put(keyStoreName, ks);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "addKeyStoreToMap");
    }

    /***
     * Iterates through trusted certificate entries to ensure the signer does not
     * already exist.
     *
     * @param signer
     * @param trustStore
     * @return boolean
     ***/
    public boolean checkIfSignerAlreadyExistsInTrustStore(X509Certificate signer, KeyStore trustStore) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "checkIfSignerAlreadyExistsInTrustStore");

        try {
            String signerDigest = generateDigest(CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA256, signer);
            if (signerDigest == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "checkIfSignerAlreadyExistsInTrustStore -> false (could not generate digest)");
                return false;
            }

            Enumeration<String> aliases = trustStore.aliases();

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();

                if (trustStore.containsAlias(alias)) {
                    X509Certificate cert = (X509Certificate) trustStore.getCertificate(alias);

                    String certDigest = generateDigest(CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA256, cert);

                    if (signerDigest.equals(certDigest)) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            Tr.exit(tc, "checkIfSignerAlreadyExistsInTrustStore -> true (digest matches)");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "checkIfSignerAlreadyExistsInTrustStore", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception checking if signer already exists; " + e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "checkIfSignerAlreadyExistsInTrustStore -> false (no digest matches)");
        return false;
    }

    /***
     * Returns the WSKeyStore object given the keyStoreName.
     *
     * @param keyStoreName
     * @return WSKeyStore
     ***/
    public WSKeyStore getKeyStore(String keyStoreName) {
        WSKeyStore ks = keyStoreMap.get(keyStoreName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (ks != null) {
                Tr.debug(tc, "Returning a keyStore for name: " + keyStoreName);
            } else {
                Tr.debug(tc, "Cannot find a keyStore for name: " + keyStoreName);
            }
        }

        return ks;
    }

    /***
     * Returns a String[] of all WSKeyStore aliases for this process.
     *
     * @return String[]
     ***/
    public String[] getKeyStoreAliases() {
        Set<String> set = keyStoreMap.keySet();
        return set.toArray(new String[set.size()]);
    }

    /**
     * @return total number of unique keystore entries in map. (Filter out duplicates representing same keystore).
     */
    public int getKeyStoreCount() {
        // each keystore usually has two entries in map, it's name and it's service reference name.
        // traverse the map to find the unique ones based on name, and return the count of that.
        HashSet uniqueIds = new HashSet();
        Iterator<Entry<String, WSKeyStore>> it = keyStoreMap.entrySet().iterator();
        while (it.hasNext()) {
            uniqueIds.add(it.next().getValue().getName());
        }
        return uniqueIds.size();
    }

    /**
     * Fetch the keystore based on the input parameters.
     *
     * @param name
     * @param type
     *                      the type of keystore
     * @param provider
     *                      provider associated with the key store
     * @param fileName
     *                      location of the key store file
     * @param password
     *                      used to access the key store
     * @param create
     * @param sslConfig
     * @return resulting key store
     * @throws Exception
     */
    public KeyStore getKeyStore(String name, String type, String provider, String fileName, String password, boolean create, SSLConfig sslConfig) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getKeyStore", new Object[] { name, type, provider, fileName, Boolean.valueOf(create), SSLConfigManager.mask(password) });

        if (name != null && !create) {
            WSKeyStore keystore = keyStoreMap.get(name);

            if (keystore != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "getKeyStore (from WSKeyStore)");
                return keystore.getKeyStore(false, false);
            }
        }

        KeyStore keyStore = null;
        InputStream inputStream = null;
        boolean not_finished = true;
        int retry_count = 0;
        boolean fileBased = true;
        List<String> keyStoreTypes = new ArrayList<String>();

        // Loop until flag indicates a key store was found or failure occured.
        while (not_finished) {
            // Get a base instance of the keystore based on the type and or the
            // provider.
            if (Constants.KEYSTORE_TYPE_JCERACFKS.equals(type) || Constants.KEYSTORE_TYPE_JCECCARACFKS.equals(type) || Constants.KEYSTORE_TYPE_JCEHYBRIDRACFKS.equals(type))
                fileBased = false;
            else
                fileBased = true;

            // Get a base instance of the keystore based on the type and or the
            // provider.
            char[] passphrase = null;
            keyStore = KeyStore.getInstance(type);
            // Convert the key store password into a char array.
            if (password != null) {
                passphrase = WSKeyStore.decodePassword(password).toCharArray();
            }

            // Open the file specified by the input parms as the keystore file.
            try {
                if (Constants.KEYSTORE_TYPE_JAVACRYPTO.equals(type)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Creating PKCS11 keystore.");

                    WSPKCSInKeyStore pKS = pkcsStoreList.insert(type, fileName, password, false, provider);

                    if (pKS != null) {
                        keyStore = pKS.getKS();
                        not_finished = false;
                    }
                } else if (null == fileName) {
                    keyStore.load(null, passphrase);
                    not_finished = false;
                } else {
                    File f = new File(fileName);

                    FileExistsAction action = new FileExistsAction(f);
                    Boolean fileExists = AccessController.doPrivileged(action);

                    if (!fileExists.booleanValue() && fileBased) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "getKeyStore created new KeyStore: " + fileName);
                        }
                        keyStore.load(null, passphrase);
                        not_finished = false;
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "getKeyStore created a new inputStream: " + fileName);
                        }
                        // Access the keystore input stream from a File or URL
                        inputStream = getInputStream(fileName, create);
                        keyStore.load(inputStream, passphrase);
                        not_finished = false;
                    }
                }
            } catch (IOException e) {
                // Check for well known error conditions.
                if (e.getMessage().equalsIgnoreCase("Invalid keystore format") || e.getMessage().indexOf("DerInputStream.getLength()") != -1) {
                    if (retry_count == 0) {
                        String alias = "unknown";
                        if (sslConfig != null) {
                            alias = sslConfig.getProperty(Constants.SSLPROP_ALIAS);
                        }

                        Tr.warning(tc, "ssl.keystore.type.invalid.CWPKI0018W", new Object[] { type, alias });
                        keyStoreTypes = new ArrayList<String>(Security.getAlgorithms("KeyStore"));
                    }

                    // Limit the number of retries.
                    if (retry_count >= keyStoreTypes.size()) {
                        throw e;
                    }

                    // Adjust the type for another try.
                    // We'll go through all available types.
                    type = keyStoreTypes.get(retry_count++);
                    if (type.equals("PKCS11") || type.equals("IBMCMSKS")) {
                        type = keyStoreTypes.get(retry_count++);
                    }

                } else {
                    // Unknown error condition.
                    throw e;
                }
            } finally {
                if (inputStream != null)
                    inputStream.close();
            }
        } // end while

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getKeyStore (from SSLConfig properties)");
        return keyStore;
    }

    /**
     * Open the provided filename as a keystore, creating if it doesn't exist and
     * the input create flag is true.
     *
     * @param fileName
     * @param create
     * @return InputStream
     * @throws MalformedURLException
     * @throws IOException
     */
    public InputStream getInputStream(String fileName, boolean create) throws MalformedURLException, IOException {
        try {
            GetKeyStoreInputStreamAction action = new GetKeyStoreInputStreamAction(fileName, create);
            return AccessController.doPrivileged(action);
        } catch (PrivilegedActionException e) {
            Exception ex = e.getException();
            FFDCFilter.processException(e, getClass().getName(), "getInputStream", new Object[] { fileName, Boolean.valueOf(create), this });
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore; " + ex);

            if (ex instanceof MalformedURLException)
                throw (MalformedURLException) ex;
            else if (ex instanceof IOException)
                throw (IOException) ex;

            throw new IOException(ex.getMessage());
        }
    }

    /**
     * This class is used to enable the code to read keystores.
     */
    private static class GetKeyStoreInputStreamAction implements PrivilegedExceptionAction<InputStream> {
        private String file = null;
        private boolean createStream = false;

        /**
         * Constructor.
         *
         * @param fileName
         * @param create
         */
        public GetKeyStoreInputStreamAction(String fileName, boolean create) {
            file = fileName;
            createStream = create;
        }

        @Override
        public InputStream run() throws MalformedURLException, IOException {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.entry(tc, "GetKeyStoreInputStreamAction.run: " + file);

            InputStream fis = null;
            URL urlFile = null;

            // Check if the filename exists as a File.
            File kfile = new File(file);

            if (createStream && !kfile.exists()) {
                if (!kfile.createNewFile()) {
                    throw new IOException("Unable to create file");
                }
                urlFile = kfile.toURI().toURL();
            } else {
                if (kfile.exists() && kfile.length() == 0) {
                    throw new IOException("Keystore file exists, but is empty: " + file);
                } else if (!kfile.exists()) {
                    // kfile does not exist as a File, treat as URL
                    urlFile = new URL(file);
                } else {
                    // kfile exists as a File
                    urlFile = kfile.toURI().toURL();
                }
            }

            // Finally open the file.
            fis = urlFile.openStream();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "GetKeyStoreInputStreamAction.run");
            return fis;
        }
    }

    /**
     * Open the provided filename as an outputstream.
     *
     * @param fileName
     * @return OutputStream
     * @throws MalformedURLException
     * @throws IOException
     */
    public OutputStream getOutputStream(String fileName) throws MalformedURLException, IOException {
        try {
            GetKeyStoreOutputStreamAction action = new GetKeyStoreOutputStreamAction(fileName);
            return AccessController.doPrivileged(action);
        } catch (PrivilegedActionException e) {
            Exception ex = e.getException();
            FFDCFilter.processException(e, getClass().getName(), "getOutputStream", new Object[] { fileName, this });
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore; " + ex);

            if (ex instanceof MalformedURLException)
                throw (MalformedURLException) ex;
            else if (ex instanceof IOException)
                throw (IOException) ex;

            throw new IOException(ex.getMessage());
        }
    }

    /**
     * This class is used to enable the code to read keystores.
     */
    private static class GetKeyStoreOutputStreamAction implements PrivilegedExceptionAction<OutputStream> {
        private String file = null;

        /**
         * Constructor.
         *
         * @param fileName
         */
        public GetKeyStoreOutputStreamAction(String fileName) {
            file = fileName;
        }

        @Override
        public OutputStream run() throws MalformedURLException, IOException {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.entry(tc, "GetKeyStoreOutputStreamAction.run: " + file);

            OutputStream fos = null;

            if (file.startsWith("safkeyring://")) {
                URL ring = new URL(file);
                URLConnection ringConnect = ring.openConnection();
                fos = ringConnect.getOutputStream();
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "GetKeyStoreOutputStreamAction.run (safkeyring)");
                return fos;
            }

            try {
                URL conversionURL = new URL(file);
                file = conversionURL.getFile();

                while (file.startsWith("/")) {
                    file = file.substring(1);
                }
            } catch (MalformedURLException e) {
                // it must be a file path already. just let it continue..
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "File path for OutputStream: " + file);

            // Check if the filename exists as a File.
            File kfile = new File(file);

            if (kfile.exists() && !kfile.canWrite()) {
                // kfile exists, but cannot write to it.
                throw new IOException("Cannot write to KeyStore file: " + file);
            }
            // kfile exists, updating it.
            fos = new FileOutputStream(kfile);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "GetKeyStoreOutputStreamAction.run");
            return fos;
        }
    }

    /**
     * This class is used to check if file exists.
     */
    private static class FileExistsAction implements PrivilegedAction<Boolean> {
        private File file = null;

        /**
         * Constructor.
         *
         * @param input_file
         */
        public FileExistsAction(File input_file) {
            file = input_file;
        }

        @Override
        public Boolean run() {
            try {
                return Boolean.valueOf(file.exists());
            } catch (Exception e) {
                // it must be a file path already. just let it continue..
                return Boolean.FALSE;
            }
        }
    }

    /***
     * This method is used to create a digest on an X509Certificate
     * as the "fingerprint".
     *
     * @param algorithmName
     * @param cert
     * @return String
     ***/
    public String generateDigest(String algorithmName, X509Certificate cert) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "generateDigest: " + algorithmName);

        String rc = null;
        if (cert != null) {
            try {
                MessageDigest md = CryptoUtils.getMessageDigest(algorithmName);
                md.update(cert.getEncoded());
                byte data[] = md.digest();
                StringBuilder buffer = new StringBuilder(3 * data.length);
                int i = 0;
                buffer.append(HEX_CHARS[(data[i] >> 4) & 0xF]);
                buffer.append(HEX_CHARS[(data[i] % 16) & 0xF]);
                for (++i; i < data.length; i++) {
                    buffer.append(':');
                    buffer.append(HEX_CHARS[(data[i] >> 4) & 0xF]);
                    buffer.append(HEX_CHARS[(data[i] % 16) & 0xF]);
                }
                rc = buffer.toString();
            } catch (NoClassDefFoundError e) {
                // no ffdc needed, this is for PAC.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Error finding a class: " + e);
            } catch (Exception e) {
                FFDCFilter.processException(e, getClass().getName(), "generateDigest", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Error generating digest: " + e);
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Ignoring null certificate");
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "generateDigest: " + rc);
        return rc;
    }

    /***
     * This method is used to clear KeyStore configurations when the entire config
     * is being reloaded.
     ***/
    public void clearKSMap() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Clearing keystore maps");
        synchronized (keyStoreMap) {
            keyStoreMap.clear();
        }
    }

    /***
     * This method is used to clear a specific KeyStore configuration when adding
     * a signer to it.
     ***/
    public void clearKeyStoreFromMap(String keyStoreName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "clearKeyStoreFromMap: " + keyStoreName);
        synchronized (keyStoreMap) {
            keyStoreMap.remove(keyStoreName);
        }
    }

    /***
     * This method is used to clear the Java KeyStores held within the WSKeyStores
     * in the KeyStoreMap. It's called after a federation.
     ***/
    public void clearJavaKeyStoresFromKeyStoreMap() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "clearJavaKeyStoresFromKeyStoreMap");
        synchronized (keyStoreMap) {
            for (Entry<String, WSKeyStore> entry : keyStoreMap.entrySet()) {
                WSKeyStore ws = entry.getValue();

                if (ws != null)
                    ws.clearJavaKeyStore();
            }
        }
    }

    /***
     * This method is used to clear the Java KeyStores held within the WSKeyStores
     * in the KeyStoreMap based on the files
     ***/
    public void clearJavaKeyStoresFromKeyStoreMap(Collection<File> modifiedFiles) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "clearJavaKeyStoresFromKeyStoreMap ", new Object[] { modifiedFiles });

        String filePath = null;
        for (File modifiedKeystoreFile : modifiedFiles) {
            try {
                filePath = modifiedKeystoreFile.getCanonicalPath();
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Exception comparing file path.");
                continue;
            }

            findKeyStoreInMapAndClear(filePath);
        }
    }

    /***
     * This method is used to clear the Java KeyStores held within the WSKeyStores
     * in the KeyStoreMap based on the files
     ***/
    public void findKeyStoreInMapAndClear(String keyStorePath) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "findKeyStoreInMapAndClear ", new Object[] { keyStorePath });

        for (Entry<String, WSKeyStore> entry : keyStoreMap.entrySet()) {
            WSKeyStore ws = entry.getValue();
            if (WSKeyStore.getCannonicalPath(ws.getLocation(), ws.getFileBased()).equals(keyStorePath)) {
                ws.clearJavaKeyStore();
            }
        }
    }

    /***
     * Expands the ${hostname} with the node's hostname.
     *
     * @param subjectDN
     * @param nodeHostName
     * @return String
     ***/
    public static String expandHostNameVariable(String subjectDN, String nodeHostName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "expandHostNameVariable", new Object[] { subjectDN, nodeHostName });

        String expandedSubjectDN = subjectDN;
        int index1 = subjectDN.indexOf("${hostname}");

        if (index1 != -1) {
            String firstPart = subjectDN.substring(0, index1);
            String lastPart = subjectDN.substring(index1 + "${hostname}".length());
            // String.substring always returns non-null
            if (!firstPart.equals("") && !lastPart.equals(""))
                expandedSubjectDN = firstPart + nodeHostName + lastPart;
            else if (!firstPart.equals(""))
                expandedSubjectDN = firstPart + nodeHostName;
            else if (!lastPart.equals(""))
                expandedSubjectDN = nodeHostName + lastPart;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "expandHostNameVariable -> " + expandedSubjectDN);
        return expandedSubjectDN;
    }

    /**
     * Remove the last slash, if present, from the input string and return the
     * result.
     *
     * @param inputString
     * @return String
     */
    public static String stripLastSlash(String inputString) {
        if (null == inputString) {
            return null;
        }
        String rc = inputString.trim();
        int len = rc.length();
        if (0 < len) {
            char lastChar = rc.charAt(len - 1);
            if ('/' == lastChar || '\\' == lastChar) {
                rc = rc.substring(0, len - 1);
            }
        }

        return rc;
    }

    /**
     * Returns the java keystore object based on the keystore name passed in.
     *
     * @param keyStoreName
     * @return KeyStore
     * @throws Exception
     */
    public KeyStore getJavaKeyStore(String keyStoreName) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getJavaKeyStore: " + keyStoreName);

        if (keyStoreName == null || keyStoreName.trim().isEmpty()) {
            throw new SSLException("No keystore name provided.");
        }

        KeyStore javaKeyStore = null;
        WSKeyStore ks = keyStoreMap.get(keyStoreName);
        if (ks != null) {
            javaKeyStore = ks.getKeyStore(false, false);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getJavaKeyStore: " + javaKeyStore);
        return javaKeyStore;
    }

    /**
     * Returns the java keystore object based on the keystore name passed in. A
     * null value is returned if no existing store matchs the provided name.
     *
     * @param keyStoreName
     * @return WSKeyStore
     * @throws SSLException
     *                          - if the input name is null
     */
    public WSKeyStore getWSKeyStore(String keyStoreName) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getWSKeyStore: " + keyStoreName);

        if (keyStoreName == null) {
            throw new SSLException("No keystore name provided.");
        }

        WSKeyStore ks = keyStoreMap.get(keyStoreName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getWSKeyStore: " + ks);
        return ks;
    }

}
