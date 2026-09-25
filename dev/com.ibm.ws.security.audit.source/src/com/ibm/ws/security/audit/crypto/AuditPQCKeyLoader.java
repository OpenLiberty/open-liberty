/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit.crypto;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import com.ibm.ws.common.encoder.Base64Coder;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * Loads and saves an ML-KEM-768 key pair stored in a single combined PEM file.
 *
 * <p>Uses the native IBMJCEPlus provider (IBM Semeru JDK 8 SR8+) — no Bouncy Castle
 * dependency.</p>
 *
 * <p>File format — two standard PEM blocks in sequence:</p>
 * <pre>
 * -----BEGIN PRIVATE KEY----- // pragma: allowlist secret
 * <PKCS#8 DER of ML-KEM-768 private key, Base64>
 * -----END PRIVATE KEY-----
 * -----BEGIN PUBLIC KEY-----
 * <SubjectPublicKeyInfo DER of ML-KEM-768 public key, Base64>
 * -----END PUBLIC KEY-----
 * </pre>
 *
 * <p>Use {@link #generateAndSave(String)} once to create the key file,
 * then {@link #loadKeyPair(String)} at runtime to load it.</p>
 */
public class AuditPQCKeyLoader {

    private static final TraceComponent tc = Tr.register(AuditPQCKeyLoader.class, null, "com.ibm.ejs.resources.security");

    private static final String ML_KEM_768    = "ML-KEM-768";
    private static final String ML_DSA_65     = "ML-DSA-65";

    private static final String PEM_PRIV_BEGIN = "-----BEGIN PRIVATE KEY-----"; // pragma: allowlist secret
    private static final String PEM_PRIV_END   = "-----END PRIVATE KEY-----"; // pragma: allowlist secret
    private static final String PEM_PUB_BEGIN  = "-----BEGIN PUBLIC KEY-----"; // pragma: allowlist secret
    private static final String PEM_PUB_END    = "-----END PUBLIC KEY-----"; // pragma: allowlist secret

    /** Utility class — no instantiation. */
    private AuditPQCKeyLoader() {}

    // -----------------------------------------------------------------------
    // Load
    // -----------------------------------------------------------------------

    /**
     * Loads an ML-KEM-768 key pair from a combined PEM file using the default provider.
     *
     * @param filePath path to the combined PEM file
     * @return KeyPair containing the ML-KEM-768 private and public keys
     * @throws PQCException if the file cannot be read or the keys cannot be reconstructed
     */
    public static KeyPair loadKeyPair(String filePath) throws PQCException {
        return loadKeyPair(filePath, ML_KEM_768);
    }

    /**
     * Loads an ML-DSA-65 key pair from a combined PEM file using the default provider.
     *
     * @param filePath path to the combined PEM file
     * @return KeyPair containing the ML-DSA-65 private and public keys
     * @throws PQCException if the file cannot be read or the keys cannot be reconstructed
     */
    public static KeyPair loadMLDSAKeyPair(String filePath) throws PQCException {
        return loadKeyPair(filePath, ML_DSA_65);
    }

    /**
     * Loads a PQC key pair from a combined PEM file using the specified algorithm.
     *
     * @param filePath path to the combined PEM file
     * @param algorithm the PQC algorithm (e.g., ML-KEM-768, ML-DSA-65)
     * @return KeyPair containing the private and public keys
     * @throws PQCException if the file cannot be read or the keys cannot be reconstructed
     */
    private static KeyPair loadKeyPair(String filePath, String algorithm) throws PQCException {
        if (tc.isDebugEnabled()) Tr.debug(tc, "loadKeyPair: loading " + algorithm + " key pair from " + filePath);

        String privateB64 = null;
        String publicB64  = null;

        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            try {
                privateB64 = extractPemBlock(br, PEM_PRIV_BEGIN, PEM_PRIV_END);
                publicB64  = extractPemBlock(br, PEM_PUB_BEGIN,  PEM_PUB_END);
            } finally {
                br.close();
            }
        } catch (IOException e) {
            throw new PQCException("Failed to read PQC key file: " + filePath + " — " + e.getMessage(), e);
        }

        if (privateB64 == null) throw new PQCException("No PRIVATE KEY block found in: " + filePath);
        if (publicB64  == null) throw new PQCException("No PUBLIC KEY block found in: "  + filePath);

        try {
            // For ML-DSA and ML-KEM, use the base algorithm name (not the variant like ML-DSA-65)
            // Java 26+ expects "ML-DSA" or "ML-KEM" as the algorithm name
            String baseAlgorithm = algorithm;
            if (algorithm.startsWith("ML-DSA-") || algorithm.startsWith("ML-KEM-")) {
                // Extract base algorithm: "ML-DSA-65" -> "ML-DSA", "ML-KEM-768" -> "ML-KEM"
                int dashIndex = algorithm.lastIndexOf('-');
                if (dashIndex > 0) {
                    baseAlgorithm = algorithm.substring(0, dashIndex);
                }
            }
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "loadKeyPair: using base algorithm '" + baseAlgorithm + "' for KeyFactory (from '" + algorithm + "')");
            }
            
            // Use default provider (Java 26+ native PQC support)
            KeyFactory kf = KeyFactory.getInstance(baseAlgorithm);

            // Strip whitespace/newlines before decoding — Base64Coder requires clean input
            byte[]     privDer    = Base64Coder.base64Decode(stripWhitespace(privateB64).getBytes("UTF-8"));
            PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privDer));

            byte[]    pubDer    = Base64Coder.base64Decode(stripWhitespace(publicB64).getBytes("UTF-8"));
            PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(pubDer));

            if (tc.isDebugEnabled()) Tr.debug(tc, "loadKeyPair: loaded " + algorithm + " key pair successfully");
            return new KeyPair(publicKey, privateKey);

        } catch (Exception e) {
            throw new PQCException("Failed to reconstruct " + algorithm + " key pair from: " + filePath + " — " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Generate and save
    // -----------------------------------------------------------------------

    /**
     * Generates a fresh ML-KEM-768 key pair and writes it to {@code filePath}
     * in combined PEM format.  Run once on the server to seed the key file.
     *
     * @param filePath destination file path (will be created or overwritten)
     * @throws PQCException if key generation or file writing fails
     */
    public static void generateAndSave(String filePath) throws PQCException {
        generateAndSave(filePath, ML_KEM_768);
    }

    /**
     * Generates a fresh ML-DSA-65 key pair and writes it to {@code filePath}
     * in combined PEM format.  Run once on the server to seed the key file.
     *
     * @param filePath destination file path (will be created or overwritten)
     * @throws PQCException if key generation or file writing fails
     */
    public static void generateAndSaveMLDSA(String filePath) throws PQCException {
        generateAndSave(filePath, ML_DSA_65);
    }

    /**
     * Generates a fresh PQC key pair and writes it to {@code filePath}
     * in combined PEM format.
     *
     * @param filePath destination file path (will be created or overwritten)
     * @param algorithm the PQC algorithm (e.g., ML-KEM-768, ML-DSA-65)
     * @throws PQCException if key generation or file writing fails
     */
    private static void generateAndSave(String filePath, String algorithm) throws PQCException {
        if (tc.isDebugEnabled()) Tr.debug(tc, "generateAndSave: generating " + algorithm + " key pair -> " + filePath);

        try {
            // Use default provider (Java 26+ native PQC support)
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
            KeyPair kp = kpg.generateKeyPair();

            // Base64Coder.base64Encode produces a flat byte[] with no line breaks — wrap at 64 chars
            String privB64 = wrapBase64(new String(Base64Coder.base64Encode(kp.getPrivate().getEncoded())));
            String pubB64  = wrapBase64(new String(Base64Coder.base64Encode(kp.getPublic().getEncoded())));

            BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
            try {
                bw.write(PEM_PRIV_BEGIN); bw.newLine();
                bw.write(privB64);        bw.newLine();
                bw.write(PEM_PRIV_END);   bw.newLine();
                bw.write(PEM_PUB_BEGIN);  bw.newLine();
                bw.write(pubB64);         bw.newLine();
                bw.write(PEM_PUB_END);    bw.newLine();
            } finally {
                bw.close();
            }

            if (tc.isDebugEnabled()) Tr.debug(tc, "generateAndSave: written to " + filePath);

        } catch (Exception e) {
            throw new PQCException("Failed to generate or save " + algorithm + " key pair: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Removes all whitespace and newlines from a Base64 string before decoding. */
    private static String stripWhitespace(String s) {
        return s.replaceAll("\\s", "");
    }

    /** Inserts a newline every 64 characters to produce standard PEM line wrapping. */
    private static String wrapBase64(String flat) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < flat.length(); i += 64) {
            sb.append(flat, i, Math.min(i + 64, flat.length()));
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Reads lines from {@code br} until it finds the given begin/end markers,
     * returning the Base64 body between them (markers excluded).
     * Returns null if the begin marker is never found.
     */
    private static String extractPemBlock(BufferedReader br, String beginMarker, String endMarker)
            throws IOException {
        String line;
        // Scan for begin marker
        while ((line = br.readLine()) != null) {
            if (line.trim().equals(beginMarker)) break;
        }
        if (line == null) return null; // begin marker not found

        // Collect body lines until end marker
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            if (line.trim().equals(endMarker)) break;
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
}

// Made with Bob
