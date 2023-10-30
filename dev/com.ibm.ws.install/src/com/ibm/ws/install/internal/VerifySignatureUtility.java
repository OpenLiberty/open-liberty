/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.install.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallConstants.VerifyOption;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.kernel.boot.cmdline.Utils;

/**
 *
 */
public class VerifySignatureUtility {

    private static final Logger logger = InstallLogUtils.getInstallLogger();
    private static final File LIBERTY_KEY = new File(Utils.getInstallDir(), "lib/versions/public_key/libertyKey.asc");
    private static final String UbuntuServerURL = "https://keyserver.ubuntu.com/pks/lookup?op=get&options=mr&search=";
    private String defaultKeyID = null;

    private final ProgressBar progressBar = ProgressBar.getInstance();

    private String getLibertyKeyID() throws InstallException {
        if (defaultKeyID != null) {
            return defaultKeyID;
        }
        File propertiesFile = new File(Utils.getInstallDir(), "lib/versions/openliberty.properties");
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(propertiesFile)) {
            properties.load(input);
            defaultKeyID = properties.getProperty("com.ibm.websphere.productPublicKeyId");
        } catch (IOException e) {
            // openliberty.properties file is missing or invalidly formatted
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getMessage("ERROR_COULD_NOT_DETERMINE_RUNTIME_PROPERTIES_FILE", propertiesFile.getAbsolutePath()));
        }

        return defaultKeyID;
    }

    public boolean isKeyValid(File pubKey, String keyID) throws InstallException {
        boolean isValid = false;

        try (FileInputStream fis = new FileInputStream(pubKey); InputStream keyIn = new BufferedInputStream(fis)) {
            PGPPublicKeyRing pgpPubKeyRing = new PGPPublicKeyRing(PGPUtil.getDecoderStream(keyIn), new JcaKeyFingerprintCalculator());
            PGPPublicKey publicKey = pgpPubKeyRing.getPublicKey();
            isValid = validatePublicKey(publicKey, keyID);
        } catch (IOException e) {
            logger.warning(e.getMessage());
            if (keyID.equals(getLibertyKeyID())) {
                throw new InstallException(e.getMessage());
            }
        } catch (InstallException e) {
            if (keyID.equals(getLibertyKeyID())) {
                throw e;
            } else {
                logger.warning(e.getMessage());
            }
        }
        return isValid;
    }

    /**
     * @param publicKey
     * @param expectedKeyID
     * @throws InstallException
     */
    protected boolean validatePublicKey(PGPPublicKey publicKey, String expectedKeyID) throws InstallException {
        //16 hex char.. pad with 0 in the beginning so the length is always 16
        String keyID = String.format("0x%016x", publicKey.getKeyID()).toUpperCase();

        //check if key id match the one we downloaded
        if (!keyID.contains(expectedKeyID.toUpperCase())) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_INAUTHENTIC_PUBLIC_KEY", keyID, expectedKeyID));
        }

        //Check if the key is revoked
        if (publicKey.hasRevocation()) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_REVOKED_PUBLIC_KEY", keyID));
        }

        //Check the expiry
        if (publicKey.getValidSeconds() > 0) { //0 mean no expiry date
            Instant expiryDate = publicKey.getCreationTime().toInstant().plusSeconds(publicKey.getValidSeconds());
            if (expiryDate.isBefore(Instant.now())) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_EXPIRED_PUBLIC_KEY", keyID, expiryDate));
            }
        }
        return true;
    }

    /*
     * /
     * Throws InstallException only when we failed to download Liberty key. For user's key, we will handle that later.
     */
    public List<File> downloadPublicKeys(Collection<Map<String, String>> keys, VerifyOption verify, Map<String, Object> envMap) throws InstallException {
        Map<String, String> allKeys = getPublicKeyURL(keys, verify);
        List<File> downloadedKeys = new ArrayList<>();

        for (Entry<String, String> key : allKeys.entrySet()) {
            URLConnection conn;
            try {
                logger.fine("Downloading key... " + key.getValue());
                URL keyUrl = new URL(key.getValue());
                Proxy proxy;
                if (envMap.get("https.proxyHost") != null) {
                    proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress((String) envMap.get("https.proxyHost"), Integer.parseInt((String) envMap.get("https.proxyPort"))));
                } else if (envMap.get("http.proxyHost") != null) {
                    proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress((String) envMap.get("http.proxyHost"), Integer.parseInt((String) envMap.get("http.proxyPort"))));
                } else {
                    proxy = Proxy.NO_PROXY;
                }
                conn = keyUrl.openConnection(proxy);
                conn.setConnectTimeout(10000);

                try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream())) {
                    File tempFile = File.createTempFile("signature", ".asc", Utils.getInstallDir());
                    tempFile.deleteOnExit(); //Delete when JVM exits
                    try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                        byte[] dataBuffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                            fileOutputStream.write(dataBuffer, 0, bytesRead);
                        }
                        if (isKeyValid(tempFile, key.getKey())) {
                            downloadedKeys.add(tempFile);
                        }
                    }
                }
            } catch (IOException e) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_DOWNLOAD_KEY_FROM_KEY_URL", e.getMessage()));

            }
        }

        return downloadedKeys;
    }

    protected Map<String, String> getPublicKeyURL(Collection<Map<String, String>> keys, VerifyOption verify) throws InstallException {
        Map<String, String> pubKeyUrls = new HashMap();
        try {
            if (isKeyValid(LIBERTY_KEY, getLibertyKeyID())) {
                pubKeyUrls.put(getLibertyKeyID(), LIBERTY_KEY.toURI().toURL().toString());
            }
        } catch (InstallException | MalformedURLException e) {
            logger.warning(e.getMessage());
            String liberty_keyID = System.getProperty("com.ibm.ws.install.libertyKeyID", getLibertyKeyID());
            String PUBKEY_URL = UbuntuServerURL + liberty_keyID;
            pubKeyUrls.put(liberty_keyID, PUBKEY_URL);
        }

        //get users public keys
        getUserPubKey(keys, pubKeyUrls);
        return pubKeyUrls;
    }

    /**
     * @param keys
     * @param pubKeyUrls
     */
    protected void getUserPubKey(Collection<Map<String, String>> keys, Map<String, String> pubKeyUrls) {
        for (Map<String, String> keyMap : keys) {
            String keyURL = keyMap.get(InstallConstants.KEYURL_QUALIFIER);
            String keyID = keyMap.get(InstallConstants.KEYID_QUALIFIER);

            if (keyID == null && keyURL == null) {
                logger.fine("Found the property for keyid and/or keyurl, but the value was not specified.");
                break;
            } else if (keyID == null) {
                logger.warning(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_KEYID_NOT_PROVIDED", keyURL));
            } else {
                try {
                    pubKeyUrls.put(keyID, getValidKeyURL(keyURL, keyID));
                } catch (InstallException e) {
                    logger.warning(e.getMessage());
                }
            }
        }
    }

    /**
     * @param keyURL
     * @param keyID
     * @throws InstallException
     */
    protected String getValidKeyURL(String keyURL, String keyID) throws InstallException {
        if (keyURL == null) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_KEYURL_NOT_PROVIDED", keyID));
        }

        if (!InstallUtils.isURL(keyURL)) {
            File f = new File(keyURL);
            if (!f.exists()) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_DOWNLOAD_KEY_FROM_KEY_URL", keyURL));
            }
            try {
                keyURL = f.toURI().toURL().toString();
            } catch (MalformedURLException e) {
                throw new InstallException(e.getMessage());
            }
        } else {
            if (!keyURL.toLowerCase().startsWith("https") && !keyURL.toLowerCase().startsWith("http") && !keyURL.toLowerCase().startsWith("file")) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_KEYURL_UNSUPPORTED_PROTOCOL", keyURL));
            }
        }

        return keyURL;
    }

    /**
     * Verify the signatures of the features
     *
     * @param ESA artifacts downloaded from Maven repository
     * @throws InstallException
     * @throws Excetion
     */
    public void verifySignatures(Collection<File> artifacts, List<File> pubKeys, List<File> failedFeatures) throws InstallException {

        logger.info(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_STARTING_VERIFY"));
        PGPPublicKeyRingCollection pgpPubRingCollection = getRingCollection(pubKeys);

        //check if the public key was found
        Iterator<PGPPublicKeyRing> iterator = pgpPubRingCollection.getKeyRings();

        StringBuilder str = new StringBuilder();
        str.append("Available public keyIDs: ");
        while (iterator.hasNext()) {
            PGPPublicKey publicKey = iterator.next().getPublicKey();
            String keyID = String.format("%x", publicKey.getKeyID());
            str.append(keyID + "\t");
        }
        logger.fine(str.toString());

        double increment = progressBar.getMethodIncrement("verifyFeatures") / (artifacts.size());
        for (File f : artifacts) {
            String esa_path = f.getAbsolutePath();
            String sig_path = esa_path + ".asc";
            try {
                logger.fine(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("STATE_VERIFYING", f.getName()));
                if (!isValidSignature(esa_path, sig_path, pgpPubRingCollection)) {
                    failedFeatures.add(f);
                } else {
                    logger.fine(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_VERIFIED_FEATURE", f.getName()));
                }
                progressBar.updateProgress(increment);

            } catch (IOException | PGPException e) {
                logger.fine(e.getMessage());
                failedFeatures.add(f);
            }
        }
        progressBar.manuallyUpdate();

    }

    /**
     * @param pubKeys
     * @param pgpPubRingCollection
     * @return
     * @throws InstallException
     */
    private PGPPublicKeyRingCollection getRingCollection(List<File> pubKeys) throws InstallException {
        try {
            PGPPublicKeyRingCollection pgpPubRingCollection = new PGPPublicKeyRingCollection(new ArrayList<PGPPublicKeyRing>());
            // Read and import all public keys to the key ring
            for (File key : pubKeys) {
                try (InputStream keyIn = new BufferedInputStream(new FileInputStream(key))) {
                    PGPPublicKeyRing pgpPubKeyRing = new PGPPublicKeyRing(PGPUtil.getDecoderStream(keyIn), new JcaKeyFingerprintCalculator());
                    pgpPubRingCollection = PGPPublicKeyRingCollection.addPublicKeyRing(pgpPubRingCollection,
                                                                                       pgpPubKeyRing);
                }
            }
            return pgpPubRingCollection;

        } catch (IOException e) {
            throw new InstallException(e.getMessage());
        }
    }

    /*
     * verify the signature against the file fileName.
     */
    private boolean isValidSignature(
                                     String fileName,
                                     String sig_path,
                                     PGPPublicKeyRingCollection pgpPubRingCollection) throws IOException, PGPException {

        // Read signature file
        PGPSignatureList signatureList = getSignatureList(fileName, sig_path);
        boolean isVerified = false;

        if (signatureList != null) {
            for (int i = 0; i < signatureList.size() && !isVerified; i++) {
                PGPSignature sig = signatureList.get(i);
                logger.fine(String.format("Key ID used in signature: %x", sig.getKeyID()));

                // Check if the key ID that created the signature exists in our public key
                // collection
                PGPPublicKey pubKey = pgpPubRingCollection.getPublicKey(sig.getKeyID());
                if (pubKey == null) {
                    logger.fine(String.format("Public key ID %x was not found.", sig.getKeyID()));
                } else {
                    logger.fine(String.format("Public key ID used: %x", pubKey.getKeyID()));
                    isVerified = verifySignature(fileName, sig, pubKey);
                }
            }
        }

        return isVerified;
    }

    /**
     * @param fileName
     * @param sig
     * @param pubKey
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    private boolean verifySignature(String fileName, PGPSignature sig, PGPPublicKey pubKey) throws IOException, FileNotFoundException, PGPException {
        sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider(new BouncyCastleProvider()), pubKey);

        // Read file to verify
        try (InputStream dIn = new BufferedInputStream(new FileInputStream(fileName))) {
            int ch;
            while ((ch = dIn.read()) >= 0) {
                sig.update((byte) ch);
            }
        }

        return sig.verify();
    }

    /**
     * @param fileName
     * @param sig_path
     * @param signatureList
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    private PGPSignatureList getSignatureList(String fileName, String sig_path) throws IOException, FileNotFoundException, PGPException {
        PGPSignatureList signatureList = null;
        try (InputStream sigIn = new BufferedInputStream(new FileInputStream(sig_path));
                        InputStream decoderStream = PGPUtil.getDecoderStream(sigIn)) {

            JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(decoderStream);

            Object o;
            while ((o = pgpFact.nextObject()) != null) {
                if (o instanceof PGPCompressedData) {
                    PGPCompressedData c1 = (PGPCompressedData) o;

                    pgpFact = new JcaPGPObjectFactory(c1.getDataStream());

                    signatureList = (PGPSignatureList) pgpFact.nextObject();
                } else {
                    signatureList = (PGPSignatureList) o;
                }

                if (signatureList.isEmpty()) {
                    logger.fine("The PGP signature could not be processed for the following : " + fileName);
                }
            }
        }
        return signatureList;
    }

}