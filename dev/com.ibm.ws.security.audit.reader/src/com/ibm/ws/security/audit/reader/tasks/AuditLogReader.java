/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.audit.reader.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.security.audit.encryption.AuditEncryptionImpl;
import com.ibm.ws.security.audit.encryption.AuditSigningImpl;
import com.ibm.ws.security.audit.reader.utils.CommandUtils;

public class AuditLogReader {
    private static Logger theLogger = Logger.getLogger(AuditLogReader.class.getName());

    private static TraceComponent tc = Tr.register(AuditLogReader.class, "AUDIT", "com.ibm.ws.security.audit.reader");

    private static FileWriter outputFile = null;

    private final static String begin = "<auditRecord>";
    private final static String end = "</auditRecord>";
    private final static String newline = "\n";
    private final static String signatureOpenTag = "<signature>";
    private final static String signatureCloseTag = "</signature>";

    private static String signingCertAlias = new String();
    private static String signingKeyStoreLocation = new String();
    private static String signingKeyStoreName = new String();
    private static String encryptedSignerSharedKey = new String();

    private static String encCertAlias = new String();
    private static String encKeyStoreLocation = new String();
    private static String encKeyStoreName = new String();
    private static String encSharedKey = new String();
    private static boolean debugEnabled = false;

    public static String getReport(String fileName, String outputLocation,
                                   String encrypted, String encKeyStoreLoc, String encKeyStorePassword, String encKeyStoreType,
                                   String signed, String signingKeyStoreLoc, String signingKeyStorePassword, String signingKeyStoreType,
                                   boolean isDebug) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getReport: fileName: " + fileName + " outputLocation: " + outputLocation);

        if (isDebug) {
            debugEnabled = true;
            Handler[] handlers = Logger.getLogger("").getHandlers();
            for (int index = 0; index < handlers.length; index++) {
                handlers[index].setLevel(Level.FINE);
            }
            theLogger.setLevel(Level.FINE);
            theLogger.fine("fileName: " + fileName + " outputLocation: " + outputLocation);
        }

        encKeyStoreLocation = encKeyStoreLoc;
        signingKeyStoreLocation = signingKeyStoreLoc;

        try {
            processLog(fileName, outputLocation, encrypted, encKeyStoreLocation, encKeyStorePassword, encKeyStoreType, signed, signingKeyStoreLocation,
                       signingKeyStorePassword, signingKeyStoreType);
            outputFile.close();
            BufferedReader br = new BufferedReader(new FileReader(outputLocation));
            if (br.readLine() == null) {
                File f = new File(outputLocation);
                f.deleteOnExit();
            }
            br.close();

            if (tc.isEntryEnabled())
                Tr.exit(tc, "getReport");
            return "done";
        } catch (Exception e) {
            outputFile.close();
            BufferedReader br = new BufferedReader(new FileReader(outputLocation));
            if (br.readLine() == null) {
                File f = new File(outputLocation);
                f.deleteOnExit();
            }
            br.close();

            throw e;
        }

    }

    public static void processLog(String filename, String outputLocation, String encrypted, String encKeyStoreLocation, String encKeyStorePassword, String encKeyStoreType,
                                  String signed, String signingKeyStoreLocation, String signingKeyStorePassword, String signingKeyStoreType) throws Exception {

        try {

            FileReader file_reader = new FileReader(filename);
            BufferedReader buffered_reader = new BufferedReader(file_reader);

            if (debugEnabled) {
                theLogger.fine("processLog: filename: " + filename + " outputLocation: " + outputLocation + " encrypted: " + encrypted +
                               " encKeyStoreLocation: " + encKeyStoreLocation + " encKeyStoreType: " + encKeyStoreType +
                               " signed: " + signed + " signingKeyStoreLocation: " + signingKeyStoreLocation + " signingKeyStoreType: " +
                               signingKeyStoreType);
            }
            if (tc.isEntryEnabled())
                Tr.entry(tc, "processLog: filename: " + filename + " outputLocation: " + outputLocation + " encrypted: " + encrypted +
                             " encKeyStoreLocation: " + encKeyStoreLocation + " encKeyStoreType: " + encKeyStoreType +
                             " signed: " + signed + " signingKeyStoreLocation: " + signingKeyStoreLocation + " signingKeyStoreType: " +
                             signingKeyStoreType);

            String s = null;

            outputFile = new FileWriter(outputLocation);

            boolean encryptedLog = false;
            boolean signedLog = false;

            // Check to see if our file is encrypted or not
            while ((s = buffered_reader.readLine()) != null) {

                if (s.contains("<EncryptionInformation>")) {
                    encryptedLog = true;
                }
                if (s.contains("<SigningInformation>")) {
                    signedLog = true;
                }

                if (s.contains("<encryptionKeyStore>")) {
                    int ends = s.indexOf("</encryptionKeyStore>");
                    String f = s.substring(23, ends);
                    if (debugEnabled)
                        theLogger.fine("encryption keystore location from audit log: " + f);
                    if (encKeyStoreLocation == null || encKeyStoreLocation.isEmpty() || encKeyStoreLocation.length() == 0)
                        encKeyStoreLocation = f;
                    if (debugEnabled)
                        theLogger.fine("using encKeyStoreLocation from audit log: " + encKeyStoreLocation);

                }

                if (s.contains("<signingKeyStore>")) {
                    int ends = s.indexOf("</signingKeyStore>");

                    String f = s.substring(20, ends);
                    if (debugEnabled)
                        theLogger.fine("signing keystore location from audit log: " + f);
                    if (signingKeyStoreLocation == null || signingKeyStoreLocation.isEmpty() || signingKeyStoreLocation.length() == 0)
                        signingKeyStoreLocation = f;
                    if (debugEnabled)
                        theLogger.fine("using signingKeyStoreLocation from audit log: " + signingKeyStoreLocation);

                }

            }

            if (encryptedLog == true && signedLog == false && (encrypted == null || encrypted.equals("false"))) {
                if (debugEnabled) {
                    theLogger.fine("The audit log, " + filename + ", is encrypted but the --encrypted argument was either not specified or was specified and set to false.");
                }
                String msg = CommandUtils.getMessage("audit.MismatchingEncrypt", filename);
                throw new Exception(msg);

            }

            if (encryptedLog == false && signedLog == true && (signed == null || signed.equals("false"))) {
                if (debugEnabled) {
                    theLogger.fine("The audit log, " + filename + ", is signed but the --signed argument was either not specified or was specified and set to false.");
                }
                String msg = CommandUtils.getMessage("audit.MismatchingSign", filename);
                throw new Exception(msg);

            }

            if (encryptedLog == true && signedLog == true) {
                if ((encrypted == null || encrypted.equals("false")) || (signed == null || signed.equals("false"))) {
                    if (debugEnabled) {
                        theLogger.fine("The audit log, " + filename
                                       + ", is encrypted and signed, but either the --signed or -- encrypted argument was either not specfiied or was specified and set to false.");
                    }

                    String msg = CommandUtils.getMessage("audit.MismatchingEncryptSign", filename);
                    throw new Exception(msg);

                }
            }

            buffered_reader.close();

            if (debugEnabled)
                theLogger.fine("signedLog: " + signedLog + " encryptedLog: " + encryptedLog);

            if (tc.isDebugEnabled())
                Tr.debug(tc, "signedLog: " + signedLog + " encryptedLog: " + encryptedLog);

            if (signedLog || encryptedLog) {
                try {
                    getEncryptionAndSigningData(filename, signedLog, encryptedLog);
                } catch (Exception e) {
                    throw e;
                }
            }

            if (signedLog && !encryptedLog) {

                // And then let's get our public key
                Key publicKey = null;
                try {
                    publicKey = getPublicKey(signingKeyStoreType, signingKeyStoreLocation, signingKeyStorePassword, signingCertAlias);
                } catch (Exception e) {
                    throw e;
                }

                // Now that I have my key, decrypt the encrypted shared key

                AuditSigningImpl as = null;
                try {

                    as = new AuditSigningImpl(signingKeyStoreName, signingKeyStoreLocation, signingKeyStoreType, null, signingKeyStorePassword, signingCertAlias);
                } catch (Exception ase) {
                    throw new Exception(ase);
                }

                byte[] yy = Base64Coder.base64Decode(encryptedSignerSharedKey.getBytes("UTF8"));

                byte[] sk = encryptedSignerSharedKey.getBytes();
                String x = new String(sk);
                byte[] decryptedSharedKey = as.decryptSharedKey(yy, publicKey);
                String z = new String(decryptedSharedKey);

                // Read our signed audit records
                try {
                    file_reader = new FileReader(filename);
                } catch (java.io.FileNotFoundException fnf) {
                    throw fnf;
                }

                processRecord(file_reader, signedLog, encryptedLog, null, null);

            } // if (signedLog && !encryptedLog)

            if (encryptedLog && !signedLog) {

                if (encKeyStorePassword == null || encKeyStorePassword.length() == 0) {
                    String msg = CommandUtils.getMessage("audit.NoKeyStorePasswordValue", encKeyStoreLocation);
                    throw new Exception(msg);
                }

                // Let's get our public key
                Key publicKey = null;
                try {
                    publicKey = getPublicKey(encKeyStoreType, encKeyStoreLocation, encKeyStorePassword, encCertAlias);
                } catch (Exception e) {
                    throw e;
                }

                // Now that I have my key, decrypt the encrypted shared key

                AuditEncryptionImpl ae = null;
                try {
                    ae = new AuditEncryptionImpl(encKeyStoreName, encKeyStoreLocation, encKeyStoreType, null, encKeyStorePassword, encCertAlias);
                } catch (Exception aee) {
                    throw new Exception(aee);
                }

                byte[] yy = Base64Coder.base64Decode(encSharedKey.getBytes("UTF8"));
                byte[] sk = encSharedKey.getBytes();
                String x = new String(sk);
                byte[] decryptedSharedKey = ae.decryptSharedKey(yy, publicKey);
                String z = new String(decryptedSharedKey);

                // Read our encrypted audit records
                try {
                    file_reader = new FileReader(filename);
                } catch (java.io.FileNotFoundException fnf) {
                    throw fnf;
                }

                processRecord(file_reader, signedLog, encryptedLog, decryptedSharedKey, ae);

            }

            if (encryptedLog && signedLog) {

                if (encKeyStorePassword == null || encKeyStorePassword.length() == 0) {
                    String msg = CommandUtils.getMessage("audit.NoKeyStorePasswordValue", encKeyStoreLocation);
                    throw new Exception(msg);
                } else if (signingKeyStorePassword == null || signingKeyStorePassword.length() == 0) {
                    String msg = CommandUtils.getMessage("audit.NoKeyStorePasswordValue", signingKeyStoreLocation);
                    throw new Exception(msg);
                }

                // And then let's get our public key for our signed records

                Key publicKey = null;
                try {
                    publicKey = getPublicKey(signingKeyStoreType, signingKeyStoreLocation, signingKeyStorePassword, signingCertAlias);
                } catch (Exception e) {
                    if (debugEnabled)
                        theLogger.fine("exception getting public key for our signed records" + e.getMessage());
                    throw e;
                }

                // Now that I have my key, decrypt the encrypted shared key

                AuditSigningImpl as = null;
                try {

                    as = new AuditSigningImpl(signingKeyStoreName, signingKeyStoreLocation, signingKeyStoreType, null, signingKeyStorePassword, signingCertAlias);
                } catch (Exception ase) {
                    throw new Exception(ase);
                }

                byte[] yy = Base64Coder.base64Decode(encryptedSignerSharedKey.getBytes("UTF8"));

                byte[] sk = encryptedSignerSharedKey.getBytes();
                byte[] decryptedSignedSharedKey = as.decryptSharedKey(yy, publicKey);

                // Let's get our public key for our encrypted records
                try {
                    publicKey = getPublicKey(encKeyStoreType, encKeyStoreLocation, encKeyStorePassword, encCertAlias);
                } catch (Exception e) {
                    if (debugEnabled)
                        theLogger.fine("exception getting public key for our encrypted records" + e.getMessage());

                    throw e;
                }

                // Now that I have my key, decrypt the encrypted shared key

                AuditEncryptionImpl ae = null;
                if (debugEnabled)
                    theLogger.fine("encKeyStoreName: " + encKeyStoreName + " encKeyStoreLocation: " + encKeyStoreLocation + " encKeyStoreType: " +
                                   encKeyStoreType + "encCertAlias: " + encCertAlias);
                try {
                    ae = new AuditEncryptionImpl(encKeyStoreName, encKeyStoreLocation, encKeyStoreType, null, encKeyStorePassword, encCertAlias);
                } catch (Exception aee) {
                    if (debugEnabled)
                        theLogger.fine("exception getting newing up an AuditEncryptionImpl" + aee.getMessage());

                    throw new Exception(aee);
                }
                // Now that I have my key, decrypt the encrypted shared key

                yy = Base64Coder.base64Decode(encSharedKey.getBytes("UTF8"));

                if (debugEnabled)
                    theLogger.fine("Was able to base64Decode the encrypted shared key");

                byte[] decryptedSharedKey = ae.decryptSharedKey(yy, publicKey);

                if (debugEnabled)
                    theLogger.fine("Was able to decrypt shared key");

                // Read our signed and encrypted audit records

                if (debugEnabled)
                    theLogger.fine("filename: " + filename);

                try {
                    file_reader = new FileReader(filename);
                } catch (java.io.FileNotFoundException fnf) {
                    if (debugEnabled)
                        theLogger.fine("File " + filename + " not found." + fnf.getMessage());

                    throw fnf;
                }

                processRecord(file_reader, signedLog, encryptedLog, decryptedSharedKey, ae);
            }

            if (!encryptedLog && !signedLog) {

                try {
                    file_reader = new FileReader(filename);
                } catch (java.io.FileNotFoundException fnf) {
                    throw fnf;
                }

                buffered_reader = new BufferedReader(file_reader);

                // We're past the prolog, and beginning with records
                while ((s = buffered_reader.readLine()) != null) {
                    parseRecord(s);
                }
                buffered_reader.close();
            }
        } catch (java.net.UnknownHostException uhe) {
            String msg = CommandUtils.getMessage("security.audit.UnknownHost", null);
            msg = msg.concat(" ").concat(uhe.getMessage());
            throw new Exception(msg);
        } catch (java.security.KeyStoreException ke) {
            String msg = CommandUtils.getMessage("security.audit.KeyStoreException", null);
            msg = msg.concat(" ").concat(ke.getMessage());
            throw new Exception(msg);
        } catch (java.security.NoSuchProviderException nspe) {
            String msg = CommandUtils.getMessage("security.audit.NoSuchProviderException", null);
            msg = msg.concat(" ").concat(nspe.getMessage());
            throw new Exception(msg);
        } catch (java.net.MalformedURLException mue) {
            String msg = CommandUtils.getMessage("security.audit.MalformedURLException", null);
            msg = msg.concat(" ").concat(mue.getMessage());
            throw new Exception(msg);
        } catch (java.security.cert.CertificateException ce) {
            String msg = CommandUtils.getMessage("security.audit.CertificateException", null);
            msg = msg.concat(" ").concat(ce.getMessage());
            throw new Exception(msg);
        } catch (java.security.NoSuchAlgorithmException nsae) {
            String msg = CommandUtils.getMessage("security.audit.NoSuchAlgorithmException", null);
            msg = msg.concat(" ").concat(nsae.getMessage());
            throw new Exception(msg);
        } catch (java.io.FileNotFoundException fnf) {
            throw fnf;
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            throw e;
        }
    }

    public static void processRecord(FileReader file_reader, boolean signedLog, boolean encryptedLog,
                                     byte[] decryptedSharedKey, AuditEncryptionImpl ae) throws Exception {

        int inByte;
        String auditRecord = new String();
        String capturedRecord = new String();
        boolean startOfRecord = false;
        int num_captured_records = 0;
        String rec = null;
        if (debugEnabled) {
            theLogger.fine("processRecord: decryptedSharedKey: " + decryptedSharedKey);
            theLogger.fine("processRecord: file_reader: " + file_reader.toString());
        }
        try {
            do {
                inByte = file_reader.read();
                if (inByte != -1) {
                    if (debugEnabled)
                        theLogger.fine("processRecord: inByte: " + inByte);

                    auditRecord = auditRecord.concat(Character.toString((char) inByte));

                    if (startOfRecord) {
                        capturedRecord = capturedRecord.concat(Character.toString((char) inByte));
                    }
                    if (!startOfRecord && auditRecord.contains(begin)) {
                        // then, starting with the next byte, we're looking at our audit record
                        startOfRecord = true;
                        capturedRecord = "";
                    }
                    if (auditRecord.contains(end)) {
                        // then we've reached the end of an audit record
                        if (!startOfRecord) {
                            if (debugEnabled)
                                theLogger.fine("WARNING: reached an end before a begin: " + auditRecord);
                            Tr.audit(tc, "WARNING: reached an end before a begin: " + auditRecord);
                            auditRecord = "";
                            capturedRecord = "";
                            continue;
                        }

                        startOfRecord = false;
                        int lenRec = capturedRecord.length();

                        if (capturedRecord.contains(begin)) {
                            if (debugEnabled)
                                theLogger.fine("WARNING: reached a begin before the end: " + auditRecord);
                            Tr.audit(tc, "WARNING: reached a begin before the end: " + auditRecord);
                            auditRecord = "";
                            capturedRecord = "";
                            continue;
                        }

                        capturedRecord = capturedRecord.substring(0, lenRec - 14);
                        capturedRecord = capturedRecord.trim();
                        num_captured_records++;
                        auditRecord = "";

                        byte[] bites = capturedRecord.getBytes("UTF8");

                        if (bites.length % 4 != 0) {
                            if (debugEnabled)
                                theLogger.fine("capturedRecord length: " + bites.length + " capturedRecord: " + capturedRecord);
                        }

                        byte[] decodedRecord = Base64Coder.base64Decode(capturedRecord.getBytes("UTF8"));

                        if (signedLog && !encryptedLog) {

                            // If the record is just signed and not encrypted, throw away the signature and process
                            // the record itself
                            rec = new String(decodedRecord);
                            int index3 = rec.indexOf(signatureOpenTag);
                            rec = rec.substring(0, index3);

                        } else if (!signedLog && encryptedLog) {

                            // Recreate the shared key
                            String algorithm = CryptoUtils.getEncryptionAlgorithm();

                            if (debugEnabled) {
                                theLogger.fine("processRecord: recreate shared key with algoritm: " + algorithm);
                            }
                            javax.crypto.spec.SecretKeySpec recreatedSharedKey = new javax.crypto.spec.SecretKeySpec(decryptedSharedKey, algorithm);

                            byte[] decryptedRecord = ae.decrypt(decodedRecord, recreatedSharedKey);
                            if (decryptedRecord != null) {
                                rec = new String(decryptedRecord);
                            }
                        } else if (signedLog && encryptedLog) {

                            String parsedSigRecord = new String();
                            byte[] strippedRecord = null;
                            for (int i = 0; i < decodedRecord.length; i++) {
                                parsedSigRecord = parsedSigRecord.concat(Character.toString((char) decodedRecord[i]));
                                if (parsedSigRecord.contains(signatureOpenTag)) {
                                    strippedRecord = new byte[parsedSigRecord.length() - signatureOpenTag.length()];
                                    System.arraycopy(decodedRecord, 0, strippedRecord, 0, parsedSigRecord.length() - signatureOpenTag.length());
                                    break;
                                }
                            }
                            String algorithm = CryptoUtils.getEncryptionAlgorithm();

                            if (debugEnabled)
                                theLogger.fine("processRecord: recreate shared key with algorithm: " + algorithm);

                            // Recreate the shared key
                            javax.crypto.spec.SecretKeySpec recreatedSharedKey = new javax.crypto.spec.SecretKeySpec(decryptedSharedKey, algorithm);

                            // Decrypt the record
                            if (tc.isDebugEnabled()) {
                                byte[] rkey = ((java.security.Key) recreatedSharedKey).getEncoded();
                            }

                            byte[] decryptedRecord = ae.decrypt(strippedRecord, recreatedSharedKey);
                            rec = new String(decryptedRecord);
                        }

                        parseRecord(rec);
                    }

                }

            } while (inByte != -1);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            throw e;
        }
    }

    public static Key getPublicKey(String keyStoreType, String keyStoreLocation,
                                   String keyStorePassword,
                                   String certAlias) throws java.security.KeyStoreException, java.security.NoSuchProviderException, java.net.MalformedURLException, java.io.IOException, java.security.cert.CertificateException, java.security.NoSuchAlgorithmException, java.lang.Exception {

        KeyStore ks = null;
        Key publicKey = null;
        InputStream is = null;
        X509Certificate c = null;

        try {

            ks = KeyStore.getInstance(keyStoreType);
        } catch (java.security.KeyStoreException ke) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", ke.getMessage());
            if (debugEnabled)
                theLogger.fine("Exception opening keystore: " + ke.getMessage());
            throw ke;
        }

        try {
            is = openKeyStore(keyStoreLocation);
        } catch (java.net.MalformedURLException me) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore: malformed URL.", me.getMessage());
            if (debugEnabled)
                theLogger.fine("Exception opening keystore: malformed URL: " + me.getMessage());
            throw me;
        } catch (java.io.IOException ioe) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", ioe.getMessage());
            if (debugEnabled)
                theLogger.fine("Exception opening keystore: " + ioe.getMessage());
            throw ioe;
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Successfully opened the keystore at " + keyStoreLocation);
        if (debugEnabled)
            theLogger.fine("Successfully opened the keystore at: " + keyStoreLocation);

        try {
            ks.load(is, keyStorePassword.toCharArray());
        } catch (java.security.cert.CertificateException ce) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "CertificateException while loading keystore.", ce.getMessage());
            if (debugEnabled)
                theLogger.fine("CertificateException while loading keystore: " + ce.getMessage());
            throw ce;
        } catch (java.io.IOException ioe) {
            String msg = CommandUtils.getMessage("security.audit.ErrorLoadingKeystore", keyStoreLocation);
            msg = msg.concat(" ").concat(ioe.getMessage());
            if (tc.isDebugEnabled())
                Tr.debug(tc, "IOException while loading keystore.", ioe.getMessage());
            if (debugEnabled)
                theLogger.fine("IOException while loading keystore: " + msg);
            throw new IOException(msg);
        } catch (java.security.NoSuchAlgorithmException ae) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "NoSuchAlgorithmException while loading keystore: no such algorithm", ae.getMessage());
            if (debugEnabled)
                theLogger.fine("NoSuchAlgorithmException while loading keystore:  no such algorithm: " + ae.getMessage());
            throw ae;
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Successfully loaded the keystore at " + keyStoreLocation);
        if (debugEnabled)
            theLogger.fine("Successfully loaded the keystore at: " + keyStoreLocation);

        try {
            c = (X509Certificate) ks.getCertificate(certAlias);
        } catch (java.security.KeyStoreException ke) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception getting certficate from keystore.", ke.getMessage());
            if (debugEnabled)
                theLogger.fine("Exception getting certificate from keystore: " + ke.getMessage());
            throw ke;
        }

        if (c == null) {
            if (debugEnabled)
                theLogger.fine("Failed to get certificate " + certAlias + " from the keystore " + keyStoreLocation);
            String msg = CommandUtils.getMessage("security.audit.CannotFindCertificate", certAlias, keyStoreLocation);
            throw new Exception(msg);
        } else {
            if (debugEnabled)
                theLogger.fine("Succeeded getting the certificate " + certAlias + " from the keystore " + keyStoreLocation);
        }
        publicKey = c.getPublicKey();
        if (debugEnabled)
            theLogger.fine("returning public key");
        return (publicKey);

    }

    public static void getEncryptionAndSigningData(String filename, boolean signedLog, boolean encryptedLog) throws Exception {
        boolean foundSignerCertAlias = false;
        boolean foundSignerKeyStore = false;
        boolean foundSignerKeyStoreName = false;
        boolean foundEncryptedSignerSharedKey = false;

        boolean foundEncryptionCertAlias = false;
        boolean foundEncryptionKeyStore = false;
        boolean foundEncryptedSharedKey = false;

        String s = null;

        try {
            FileReader file_reader = new FileReader(filename);
            BufferedReader buffered_reader = new BufferedReader(file_reader);

            while ((s = buffered_reader.readLine()) != null) {

                if (s.contains("signingCertAlias")) {
                    foundSignerCertAlias = true;
                    int index1 = s.indexOf(">");
                    int index2 = s.indexOf("</signingCertAlias>");
                    signingCertAlias = s.substring(index1 + 1, index2);
                } else if (s.contains("signingKeyStore")) {
                    foundSignerKeyStore = true;
                    int index1 = s.indexOf(">");
                    int index2 = s.indexOf("</signingKeyStore>");
                    String sksl = s.substring(index1 + 1, index2);
                } else if (s.contains("signingSharedKey")) {
                    foundEncryptedSignerSharedKey = true;
                    int index1 = s.indexOf(">");
                    int index2 = s.indexOf("</signingSharedKey");
                    encryptedSignerSharedKey = s.substring(index1 + 1, index2);
                }
                if (s.contains("encryptionCertAlias")) {
                    foundEncryptionCertAlias = true;
                    int index1 = s.indexOf(">");
                    int index2 = s.indexOf("</encryptionCertAlias>");
                    encCertAlias = s.substring(index1 + 1, index2);
                } else if (s.contains("encryptionKeyStore")) {
                    foundEncryptionKeyStore = true;
                    int index1 = s.indexOf(">");
                    int index2 = s.indexOf("</encryptionKeyStore>");
                    String eksl = s.substring(index1 + 1, index2);
                } else if (s.contains("encryptedSharedKey")) {
                    foundEncryptedSharedKey = true;
                    int index1 = s.indexOf(">");
                    int index2 = s.indexOf("</encryptedSharedKey");
                    encSharedKey = s.substring(index1 + 1, index2);
                }

                if (signedLog && !encryptedLog) {

                    if (foundSignerCertAlias && foundSignerKeyStore && foundEncryptedSignerSharedKey) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "signingKeyStoreLocation: " + signingKeyStoreLocation +
                                         " signingCertAlias: " + signingCertAlias);
                        if (debugEnabled)
                            theLogger.fine("signingKeyStoreLocation: " + signingKeyStoreLocation + " signingCertAlias: " + signingCertAlias);
                        break;
                    }
                } else if (!signedLog && encryptedLog) {
                    if (foundEncryptionCertAlias && foundEncryptionKeyStore && foundEncryptedSharedKey) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "encCertAlias: " + encCertAlias +
                                         " encKeyStoreLocation: " + encKeyStoreLocation);
                        if (debugEnabled)
                            theLogger.fine("encCertAlias: " + encCertAlias + " encKeyStoreLocation: " + encKeyStoreLocation);
                        break;
                    }
                } else if (signedLog && encryptedLog) {
                    if (foundEncryptionCertAlias && foundEncryptionKeyStore && foundEncryptedSharedKey &&
                        foundSignerCertAlias && foundSignerKeyStore && foundEncryptedSignerSharedKey) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "encCertAlias: " + encCertAlias +
                                         " encKeyStoreLocation: " + encKeyStoreLocation);
                        if (debugEnabled)
                            theLogger.fine("encCertAlias: " + encCertAlias + " encKeyStoreLocation: " + encKeyStoreLocation);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, " signingKeyStoreLocation: " + signingKeyStoreLocation +
                                         "signingCertAlias: " + signingCertAlias);
                        if (debugEnabled)
                            theLogger.fine("signingKeyStoreLocation: " + signingKeyStoreLocation + " signingCertAlias: " + signingCertAlias);

                        break;
                    }
                }

            }
            file_reader.close();
            buffered_reader.close();
        } catch (IOException e) {
            throw e;
        } catch (IllegalArgumentException iae) {
            throw iae;
        }

    }

    public static void parseRecord(String s) throws Exception {

        try {
            outputFile.write(s);
            outputFile.write("\n");
        } catch (IOException e) {
            throw e;
        }

    }

    /*** openKeyStore method to open keystore in the form of a file or a url ***/
    protected static InputStream openKeyStore(String fileName) throws MalformedURLException, IOException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "openKeyStore" + fileName);;
        try {
            //String expandedFilename = KeyStoreManager.getInstance().expand(fileName);
            OpenKeyStoreAction action = new OpenKeyStoreAction(fileName);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openKeyStore");
            return (InputStream) java.security.AccessController.doPrivileged(action);
        } catch (java.security.PrivilegedActionException e) {
            Exception ex = e.getException();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception opening keystore.", new Object[] { ex });
            if (debugEnabled)
                theLogger.fine("Exception opening keystore: " + ex.getMessage());

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
    static class OpenKeyStoreAction implements java.security.PrivilegedExceptionAction {
        private String file = null;

        public OpenKeyStoreAction(String fileName) {
            file = fileName;
        }

        @Override
        public Object run() throws MalformedURLException, IOException {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "OpenKeyStoreAction.run");

            InputStream fis = null;
            URL urlFile = null;

            // Check if the filename exists as a File.
            File kfile = new File(file);

            if (kfile.exists() && kfile.length() == 0) {
                if (debugEnabled)
                    theLogger.fine("Keystore file exists, but is empty: " + file);
                throw new IOException("Keystore file exists, but is empty: " + file);
            } else if (!kfile.exists()) {
                // kfile does not exist as a File, treat as URL
                urlFile = new URL(file);
            } else {
                // kfile exists as a File
                urlFile = new URL("file:" + kfile.getCanonicalPath());
            }

            // Finally open the file.
            fis = urlFile.openStream();
            if (tc.isEntryEnabled())
                Tr.exit(tc, "OpenKeyStoreAction.run");
            return fis;
        }
    }
}