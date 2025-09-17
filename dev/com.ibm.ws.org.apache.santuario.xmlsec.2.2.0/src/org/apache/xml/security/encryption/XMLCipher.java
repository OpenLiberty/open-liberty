/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.xml.security.encryption;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.keyresolver.KeyResolverException;
import org.apache.xml.security.keys.keyresolver.KeyResolverSpi;
import org.apache.xml.security.keys.keyresolver.implementations.EncryptedKeyResolver;
import org.apache.xml.security.signature.XMLSignatureException;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.transforms.InvalidTransformException;
import org.apache.xml.security.transforms.TransformationException;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.ElementProxy;
import org.apache.xml.security.utils.EncryptionConstants;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <code>XMLCipher</code> encrypts and decrypts the contents of
 * <code>Document</code>s, <code>Element</code>s and <code>Element</code>
 * contents. It was designed to resemble <code>javax.crypto.Cipher</code> in
 * order to facilitate understanding of its functioning.
 *
 */
public class XMLCipher {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(XMLCipher.class);

    /** Triple DES EDE (192 bit key) in CBC mode */
    public static final String TRIPLEDES =
        EncryptionConstants.ALGO_ID_BLOCKCIPHER_TRIPLEDES;

    /** AES 128 Cipher */
    public static final String AES_128 =
        EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128;

    /** AES 256 Cipher */
    public static final String AES_256 =
        EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256;

    /** AES 192 Cipher */
    public static final String AES_192 =
        EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES192;

    /** AES 128 GCM Cipher */
    public static final String AES_128_GCM =
        EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128_GCM;

    /** AES 192 GCM Cipher */
    public static final String AES_192_GCM =
        EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES192_GCM;

    /** AES 256 GCM Cipher */
    public static final String AES_256_GCM =
        EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256_GCM;

    /** SEED 128 Cipher */
    public static final String SEED_128 =
        EncryptionConstants.ALGO_ID_BLOCKCIPHER_SEED128;

    /** CAMELLIA 128 Cipher */
    public static final String CAMELLIA_128 =
        EncryptionConstants.ALGO_ID_BLOCKCIPHER_CAMELLIA128;

    /** CAMELLIA 192 Cipher */
    public static final String CAMELLIA_192 =
        EncryptionConstants.ALGO_ID_BLOCKCIPHER_CAMELLIA192;

    /** CAMELLIA 256 Cipher */
    public static final String CAMELLIA_256 =
        EncryptionConstants.ALGO_ID_BLOCKCIPHER_CAMELLIA256;

    /** RSA 1.5 Cipher */
    public static final String RSA_v1dot5 =
        EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSA15;

    /** RSA OAEP Cipher */
    public static final String RSA_OAEP =
        EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP;

    /** RSA OAEP Cipher */
    public static final String RSA_OAEP_11 =
        EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP_11;

    /** DIFFIE_HELLMAN Cipher */
    public static final String DIFFIE_HELLMAN =
        EncryptionConstants.ALGO_ID_KEYAGREEMENT_DH;

    /** Triple DES EDE (192 bit key) in CBC mode KEYWRAP*/
    public static final String TRIPLEDES_KeyWrap =
        EncryptionConstants.ALGO_ID_KEYWRAP_TRIPLEDES;

    /** AES 128 Cipher KeyWrap */
    public static final String AES_128_KeyWrap =
        EncryptionConstants.ALGO_ID_KEYWRAP_AES128;

    /** AES 256 Cipher KeyWrap */
    public static final String AES_256_KeyWrap =
        EncryptionConstants.ALGO_ID_KEYWRAP_AES256;

    /** AES 192 Cipher KeyWrap */
    public static final String AES_192_KeyWrap =
        EncryptionConstants.ALGO_ID_KEYWRAP_AES192;

    /** CAMELLIA 128 Cipher KeyWrap */
    public static final String CAMELLIA_128_KeyWrap =
        EncryptionConstants.ALGO_ID_KEYWRAP_CAMELLIA128;

    /** CAMELLIA 192 Cipher KeyWrap */
    public static final String CAMELLIA_192_KeyWrap =
        EncryptionConstants.ALGO_ID_KEYWRAP_CAMELLIA192;

    /** CAMELLIA 256 Cipher KeyWrap */
    public static final String CAMELLIA_256_KeyWrap =
        EncryptionConstants.ALGO_ID_KEYWRAP_CAMELLIA256;

    /** SEED 128 Cipher KeyWrap */
    public static final String SEED_128_KeyWrap =
        EncryptionConstants.ALGO_ID_KEYWRAP_SEED128;

    /** SHA1 Cipher */
    public static final String SHA1 =
        Constants.ALGO_ID_DIGEST_SHA1;

    /** SHA256 Cipher */
    public static final String SHA256 =
        MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256;

    /** SHA512 Cipher */
    public static final String SHA512 =
        MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA512;

    /** RIPEMD Cipher */
    public static final String RIPEMD_160 =
        MessageDigestAlgorithm.ALGO_ID_DIGEST_RIPEMD160;

    /** XML Signature NS */
    public static final String XML_DSIG =
        Constants.SignatureSpecNS;

    /** N14C_XML */
    public static final String N14C_XML =
        Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS;

    /** N14C_XML with comments*/
    public static final String N14C_XML_WITH_COMMENTS =
        Canonicalizer.ALGO_ID_C14N_WITH_COMMENTS;

    /** N14C_XML exclusive */
    public static final String EXCL_XML_N14C =
        Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS;

    /** N14C_XML exclusive with comments*/
    public static final String EXCL_XML_N14C_WITH_COMMENTS =
        Canonicalizer.ALGO_ID_C14N_EXCL_WITH_COMMENTS;

    /** N14C_PHYSICAL preserve the physical representation*/
    public static final String PHYSICAL_XML_N14C =
        Canonicalizer.ALGO_ID_C14N_PHYSICAL;

    /** Base64 encoding */
    public static final String BASE64_ENCODING =
        org.apache.xml.security.transforms.Transforms.TRANSFORM_BASE64_DECODE;

    /** ENCRYPT Mode */
    public static final int ENCRYPT_MODE = Cipher.ENCRYPT_MODE;

    /** DECRYPT Mode */
    public static final int DECRYPT_MODE = Cipher.DECRYPT_MODE;

    /** UNWRAP Mode */
    public static final int UNWRAP_MODE = Cipher.UNWRAP_MODE;

    /** WRAP Mode */
    public static final int WRAP_MODE = Cipher.WRAP_MODE;

    private static final String ENC_ALGORITHMS = TRIPLEDES + "\n" +
        AES_128 + "\n" + AES_256 + "\n" + AES_192 + "\n" + RSA_v1dot5 + "\n" +
        RSA_OAEP + "\n" + RSA_OAEP_11 + "\n" + TRIPLEDES_KeyWrap + "\n" +
        AES_128_KeyWrap + "\n" + AES_256_KeyWrap + "\n" + AES_192_KeyWrap + "\n" +
        AES_128_GCM + "\n" + AES_192_GCM + "\n" + AES_256_GCM + "\n" + SEED_128 + "\n" +
        CAMELLIA_128 + "\n" + CAMELLIA_192 + "\n" + CAMELLIA_256 + "\n" +
        CAMELLIA_128_KeyWrap + "\n" + CAMELLIA_192_KeyWrap + "\n" + CAMELLIA_256_KeyWrap + "\n" +
        SEED_128_KeyWrap + "\n";

    private static final Set<String> SUPPORTED_ALGORITHMS;

    private static final boolean HAVE_FUNCTIONAL_IDENTITY_TRANSFORMER = haveFunctionalIdentityTransformer();

    /** Cipher created during initialisation that is used for encryption */
    private Cipher contextCipher;

    /** Mode that the XMLCipher object is operating in */
    private int cipherMode = Integer.MIN_VALUE;

    /** URI of algorithm that is being used for cryptographic operation */
    private final String algorithm;

    /** Cryptographic provider requested by caller */
    private final String requestedJCEProvider;

    /** Used for creation of DOM nodes in WRAP and ENCRYPT modes */
    private Document contextDocument;

    /** Instance of factory used to create XML Encryption objects */
    private final Factory factory;

    /** Serializer class for going to/from UTF-8 */
    private final Serializer serializer;

    /** Local copy of user's key */
    private Key key;

    /** Local copy of the kek (used to decrypt EncryptedKeys during a
     *  DECRYPT_MODE operation */
    private Key kek;

    // The EncryptedKey being built (part of a WRAP operation) or read
    // (part of an UNWRAP operation)
    private EncryptedKey ek;

    // The EncryptedData being built (part of a WRAP operation) or read
    // (part of an UNWRAP operation)
    private EncryptedData ed;

    private boolean secureValidation = true;

    private String digestAlg;

    /** List of internal KeyResolvers for DECRYPT and UNWRAP modes. */
    private List<KeyResolverSpi> internalKeyResolvers;

    static {
        Set<String> encryptionAlgorithms = new HashSet<>();
        encryptionAlgorithms.add(TRIPLEDES);
        encryptionAlgorithms.add(AES_128);
        encryptionAlgorithms.add(AES_256);
        encryptionAlgorithms.add(AES_192);
        encryptionAlgorithms.add(AES_128_GCM);
        encryptionAlgorithms.add(AES_192_GCM);
        encryptionAlgorithms.add(AES_256_GCM);
        encryptionAlgorithms.add(SEED_128);
        encryptionAlgorithms.add(CAMELLIA_128);
        encryptionAlgorithms.add(CAMELLIA_192);
        encryptionAlgorithms.add(CAMELLIA_256);
        encryptionAlgorithms.add(RSA_v1dot5);
        encryptionAlgorithms.add(RSA_OAEP);
        encryptionAlgorithms.add(RSA_OAEP_11);
        encryptionAlgorithms.add(TRIPLEDES_KeyWrap);
        encryptionAlgorithms.add(AES_128_KeyWrap);
        encryptionAlgorithms.add(AES_256_KeyWrap);
        encryptionAlgorithms.add(AES_192_KeyWrap);
        encryptionAlgorithms.add(CAMELLIA_128_KeyWrap);
        encryptionAlgorithms.add(CAMELLIA_192_KeyWrap);
        encryptionAlgorithms.add(CAMELLIA_256_KeyWrap);
        encryptionAlgorithms.add(SEED_128_KeyWrap);
        SUPPORTED_ALGORITHMS = Collections.unmodifiableSet(encryptionAlgorithms);
    }

    /**
     * Creates a new <code>XMLCipher</code>.
     *
     * @param transformation    the name of the transformation, e.g.,
     *                          <code>XMLCipher.TRIPLEDES</code>. If null the XMLCipher can only
     *                          be used for decrypt or unwrap operations where the encryption method
     *                          is defined in the <code>EncryptionMethod</code> element.
     * @param provider          the JCE provider that supplies the transformation,
     *                          if null use the default provider.
     * @param digestMethod      An optional digestMethod to use.
     * @param serializer        A Serializer instance to use
     */
    private XMLCipher(
        String transformation,
        String provider,
        String digestMethod,
        Serializer serializer
    ) throws XMLEncryptionException {
        LOG.debug("Constructing XMLCipher...");

        factory = new Factory();

        algorithm = transformation;
        requestedJCEProvider = provider;
        digestAlg = digestMethod;

        this.serializer = serializer;

        if (transformation != null) {
            contextCipher = constructCipher(transformation, digestMethod);
        }
    }

    private static Serializer createSerializer(boolean secureValidation) throws XMLEncryptionException {
        return createSerializer(null, secureValidation);
    }

    private static Serializer createSerializer(String canonAlg, boolean secureValidation) throws XMLEncryptionException {
        String c14nAlg = canonAlg != null ? canonAlg : Canonicalizer.ALGO_ID_C14N_PHYSICAL;

        try {
            if (HAVE_FUNCTIONAL_IDENTITY_TRANSFORMER) {
                return new TransformSerializer(c14nAlg, secureValidation);
            }
            return new DocumentSerializer(c14nAlg, secureValidation);
        } catch (InvalidCanonicalizerException | TransformerConfigurationException e) {
            throw new XMLEncryptionException(e);
        }
    }

    /**
     * Checks to ensure that the supplied algorithm is valid.
     *
     * @param algorithm the algorithm to check.
     * @return true if the algorithm is valid, otherwise false.
     * @since 1.0.
     */
    private static boolean isValidEncryptionAlgorithm(String algorithm) {
        return SUPPORTED_ALGORITHMS.contains(algorithm);
    }

    /**
     * Validate the transformation argument of getInstance or getProviderInstance
     *
     * @param transformation the name of the transformation, e.g.,
     *   <code>XMLCipher.TRIPLEDES</code> which is shorthand for
     *   &quot;http://www.w3.org/2001/04/xmlenc#tripledes-cbc&quot;
     */
    private static void validateTransformation(String transformation) {
        if (null == transformation) {
            throw new NullPointerException("Transformation unexpectedly null...");
        }
        if (!isValidEncryptionAlgorithm(transformation)) {
            LOG.warn("Algorithm non-standard, expected one of " + ENC_ALGORITHMS);
        }
    }

    /**
     * Returns an <code>XMLCipher</code> that implements the specified
     * transformation and operates on the specified context document.
     * <p>
     * If the default provider package supplies an implementation of the
     * requested transformation, an instance of Cipher containing that
     * implementation is returned. If the transformation is not available in
     * the default provider package, other provider packages are searched.
     * <p>
     * <b>NOTE<sub>1</sub>:</b> The transformation name does not follow the same
     * pattern as that outlined in the Java Cryptography Extension Reference
     * Guide but rather that specified by the XML Encryption Syntax and
     * Processing document. The rational behind this is to make it easier for a
     * novice at writing Java Encryption software to use the library.
     * <p>
     * <b>NOTE<sub>2</sub>:</b> <code>getInstance()</code> does not follow the
     * same pattern regarding exceptional conditions as that used in
     * <code>javax.crypto.Cipher</code>. Instead, it only throws an
     * <code>XMLEncryptionException</code> which wraps an underlying exception.
     * The stack trace from the exception should be self explanatory.
     *
     * @param transformation the name of the transformation, e.g.,
     *   <code>XMLCipher.TRIPLEDES</code> which is shorthand for
     *   &quot;http://www.w3.org/2001/04/xmlenc#tripledes-cbc&quot;
     * @throws XMLEncryptionException
     * @return the XMLCipher
     * @see javax.crypto.Cipher#getInstance(java.lang.String)
     */
    public static XMLCipher getInstance(String transformation) throws XMLEncryptionException {
        LOG.debug("Getting XMLCipher with transformation");
        validateTransformation(transformation);
        return new XMLCipher(transformation, null, null, createSerializer(true));
    }

    /**
     * Returns an <code>XMLCipher</code> that implements the specified
     * transformation, operates on the specified context document and serializes
     * the document with the specified serializer before it
     * encrypts the document.
     * <p>
     *
     * @param serializer        A custom Serializer instance
     * @param transformation    the name of the transformation
     * @return the XMLCipher
     * @throws XMLEncryptionException
     */
    public static XMLCipher getInstance(Serializer serializer, String transformation) throws XMLEncryptionException {
        LOG.debug("Getting XMLCipher with transformation");
        validateTransformation(transformation);
        return new XMLCipher(transformation, null, null, serializer);
    }

    /**
     * Returns an <code>XMLCipher</code> that implements the specified
     * transformation, operates on the specified context document and serializes
     * the document with the specified canonicalization algorithm before it
     * encrypts the document.
     * <p>
     *
     * @param transformation	the name of the transformation
     * @param canon		the name of the c14n algorithm, if <code>null</code> use
     *                          standard serializer
     * @return the XMLCipher
     * @throws XMLEncryptionException
     */
    public static XMLCipher getInstance(String transformation, String canon)
        throws XMLEncryptionException {
        LOG.debug("Getting XMLCipher with transformation and c14n algorithm");
        validateTransformation(transformation);
        return new XMLCipher(transformation, null, null, createSerializer(canon, true));
    }

    /**
     * Returns an <code>XMLCipher</code> that implements the specified
     * transformation, operates on the specified context document and serializes
     * the document with the specified canonicalization algorithm before it
     * encrypts the document.
     * <p>
     *
     * @param transformation    the name of the transformation
     * @param canon             the name of the c14n algorithm, if <code>null</code> use
     *                          standard serializer
     * @param digestMethod      An optional digestMethod to use
     * @return the XMLCipher
     * @throws XMLEncryptionException
     */
    public static XMLCipher getInstance(String transformation, String canon, String digestMethod)
        throws XMLEncryptionException {
        LOG.debug("Getting XMLCipher with transformation and c14n algorithm");
        validateTransformation(transformation);
        return new XMLCipher(transformation, null, digestMethod, createSerializer(canon, true));
    }

    /**
     * Returns an <code>XMLCipher</code> that implements the specified
     * transformation and operates on the specified context document.
     *
     * @param transformation    the name of the transformation
     * @param provider          the JCE provider that supplies the transformation
     * @return the XMLCipher
     * @throws XMLEncryptionException
     */
    public static XMLCipher getProviderInstance(String transformation, String provider)
        throws XMLEncryptionException {
        LOG.debug("Getting XMLCipher with transformation and provider");
        if (null == provider) {
            throw new NullPointerException("Provider unexpectedly null..");
        }
        validateTransformation(transformation);
        return new XMLCipher(transformation, provider, null, createSerializer(true));
    }

    /**
     * Returns an <code>XMLCipher</code> that implements the specified
     * transformation, operates on the specified context document and serializes
     * the document with the specified canonicalization algorithm before it
     * encrypts the document.
     * <p>
     *
     * @param transformation	the name of the transformation
     * @param provider          the JCE provider that supplies the transformation
     * @param canon		the name of the c14n algorithm, if <code>null</code> use standard
     *                          serializer
     * @return the XMLCipher
     * @throws XMLEncryptionException
     */
    public static XMLCipher getProviderInstance(
        String transformation, String provider, String canon
    ) throws XMLEncryptionException {
        LOG.debug("Getting XMLCipher with transformation, provider and c14n algorithm");
        if (null == provider) {
            throw new NullPointerException("Provider unexpectedly null..");
        }
        validateTransformation(transformation);
        return new XMLCipher(transformation, provider, null, createSerializer(canon, true));
    }

    /**
     * Returns an <code>XMLCipher</code> that implements the specified
     * transformation, operates on the specified context document and serializes
     * the document with the specified canonicalization algorithm before it
     * encrypts the document.
     * <p>
     *
     * @param transformation    the name of the transformation
     * @param provider          the JCE provider that supplies the transformation
     * @param canon             the name of the c14n algorithm, if <code>null</code> use standard
     *                          serializer
     * @param digestMethod      An optional digestMethod to use
     * @return the XMLCipher
     * @throws XMLEncryptionException
     */
    public static XMLCipher getProviderInstance(
        String transformation, String provider, String canon, String digestMethod
    ) throws XMLEncryptionException {
        LOG.debug("Getting XMLCipher with transformation, provider and c14n algorithm");
        if (null == provider) {
            throw new NullPointerException("Provider unexpectedly null..");
        }
        validateTransformation(transformation);
        return new XMLCipher(transformation, provider, digestMethod, createSerializer(canon, true));
    }

    /**
     * Returns an <code>XMLCipher</code> that implements the specified
     * transformation, operates on the specified context document and serializes
     * the document with the specified Serializer before it encrypts the document.
     * <p>
     *
     * @param serializer        A custom serializer instance to use
     * @param transformation    the name of the transformation
     * @param provider          the JCE provider that supplies the transformation
     * @param digestMethod      An optional digestMethod to use
     * @return the XMLCipher
     * @throws XMLEncryptionException
     */
    public static XMLCipher getProviderInstance(
        Serializer serializer, String transformation, String provider, String digestMethod
    ) throws XMLEncryptionException {
        LOG.debug("Getting XMLCipher with transformation, provider and c14n algorithm");
        if (null == provider) {
            throw new NullPointerException("Provider unexpectedly null..");
        }
        validateTransformation(transformation);
        return new XMLCipher(transformation, provider, digestMethod, serializer);
    }

    /**
     * Returns an <code>XMLCipher</code> that implements no specific
     * transformation, and can therefore only be used for decrypt or
     * unwrap operations where the encryption method is defined in the
     * <code>EncryptionMethod</code> element.
     *
     * @return The XMLCipher
     * @throws XMLEncryptionException
     */
    public static XMLCipher getInstance() throws XMLEncryptionException {
        LOG.debug("Getting XMLCipher with no arguments");
        return new XMLCipher(null, null, null, createSerializer(true));
    }

    /**
     * Returns an <code>XMLCipher</code> that implements no specific
     * transformation, and can therefore only be used for decrypt or
     * unwrap operations where the encryption method is defined in the
     * <code>EncryptionMethod</code> element.
     *
     * Allows the caller to specify a provider that will be used for
     * cryptographic operations.
     *
     * @param provider          the JCE provider that supplies the transformation
     * @return the XMLCipher
     * @throws XMLEncryptionException
     */
    public static XMLCipher getProviderInstance(String provider) throws XMLEncryptionException {
        LOG.debug("Getting XMLCipher with provider");
        return new XMLCipher(null, provider, null, createSerializer(true));
    }

    /**
     * Initializes this cipher with a key.
     * <p>
     * The cipher is initialized for one of the following four operations:
     * encryption, decryption, key wrapping or key unwrapping, depending on the
     * value of opmode.
     *
     * For WRAP and ENCRYPT modes, this also initialises the internal
     * EncryptedKey or EncryptedData (with a CipherValue)
     * structure that will be used during the ensuing operations.  This
     * can be obtained (in order to modify KeyInfo elements etc. prior to
     * finalising the encryption) by calling
     * {@link #getEncryptedData} or {@link #getEncryptedKey}.
     *
     * @param opmode the operation mode of this cipher (this is one of the
     *   following: ENCRYPT_MODE, DECRYPT_MODE, WRAP_MODE or UNWRAP_MODE)
     * @param key
     * @see javax.crypto.Cipher#init(int, java.security.Key)
     * @throws XMLEncryptionException
     */
    public void init(int opmode, Key key) throws XMLEncryptionException {
        LOG.debug("Initializing XMLCipher...");

        ek = null;
        ed = null;

        switch (opmode) {

        case ENCRYPT_MODE :
            LOG.debug("opmode = ENCRYPT_MODE");
            ed = createEncryptedData(CipherData.VALUE_TYPE, "NO VALUE YET");
            break;
        case DECRYPT_MODE :
            LOG.debug("opmode = DECRYPT_MODE");
            break;
        case WRAP_MODE :
            LOG.debug("opmode = WRAP_MODE");
            ek = createEncryptedKey(CipherData.VALUE_TYPE, "NO VALUE YET");
            break;
        case UNWRAP_MODE :
            LOG.debug("opmode = UNWRAP_MODE");
            break;
        default :
            LOG.error("Mode unexpectedly invalid");
            throw new XMLEncryptionException("Invalid mode in init");
        }

        cipherMode = opmode;
        this.key = key;
    }

    /**
     * Set whether secure validation is enabled or not. The default is false.
     */
    public void setSecureValidation(boolean secureValidation) {
        this.secureValidation = secureValidation;
    }

    /**
     * This method is used to add a custom {@link KeyResolverSpi} to an XMLCipher.
     * These KeyResolvers are used in KeyInfo objects in DECRYPT and
     * UNWRAP modes.
     *
     * @param keyResolver
     */
    public void registerInternalKeyResolver(KeyResolverSpi keyResolver) {
        if (internalKeyResolvers == null) {
            internalKeyResolvers = new ArrayList<>();
        }
        internalKeyResolvers.add(keyResolver);
    }

    /**
     * Get the EncryptedData being built
     * <p>
     * Returns the EncryptedData being built during an ENCRYPT operation.
     * This can then be used by applications to add KeyInfo elements and
     * set other parameters.
     *
     * @return The EncryptedData being built
     */
    public EncryptedData getEncryptedData() {
        // Sanity checks
        LOG.debug("Returning EncryptedData");
        return ed;
    }

    /**
     * Get the EncryptedData being build
     *
     * Returns the EncryptedData being built during an ENCRYPT operation.
     * This can then be used by applications to add KeyInfo elements and
     * set other parameters.
     *
     * @return The EncryptedData being built
     */
    public EncryptedKey getEncryptedKey() {
        // Sanity checks
        LOG.debug("Returning EncryptedKey");
        return ek;
    }

    /**
     * Set a Key Encryption Key.
     * <p>
     * The Key Encryption Key (KEK) is used for encrypting/decrypting
     * EncryptedKey elements.  By setting this separately, the XMLCipher
     * class can know whether a key applies to the data part or wrapped key
     * part of an encrypted object.
     *
     * @param kek The key to use for de/encrypting key data
     */

    public void setKEK(Key kek) {
        this.kek = kek;
    }

    /**
     * Martial an EncryptedData
     *
     * Takes an EncryptedData object and returns a DOM Element that
     * represents the appropriate <code>EncryptedData</code>
     * <p>
     * <b>Note:</b> This should only be used in cases where the context
     * document has been passed in via a call to doFinal.
     *
     * @param encryptedData EncryptedData object to martial
     * @return the DOM <code>Element</code> representing the passed in
     * object
     */
    public Element martial(EncryptedData encryptedData) {
        return factory.toElement(encryptedData);
    }

    /**
     * Martial an EncryptedData
     *
     * Takes an EncryptedData object and returns a DOM Element that
     * represents the appropriate <code>EncryptedData</code>
     *
     * @param context The document that will own the returned nodes
     * @param encryptedData EncryptedData object to martial
     * @return the DOM <code>Element</code> representing the passed in
     * object
     */
    public Element martial(Document context, EncryptedData encryptedData) {
        contextDocument = context;
        return factory.toElement(encryptedData);
    }

    /**
     * Martial an EncryptedKey
     *
     * Takes an EncryptedKey object and returns a DOM Element that
     * represents the appropriate <code>EncryptedKey</code>
     *
     * <p>
     * <b>Note:</b> This should only be used in cases where the context
     * document has been passed in via a call to doFinal.
     *
     * @param encryptedKey EncryptedKey object to martial
     * @return the DOM <code>Element</code> representing the passed in
     * object
     */
    public Element martial(EncryptedKey encryptedKey) {
        return factory.toElement(encryptedKey);
    }

    /**
     * Martial an EncryptedKey
     *
     * Takes an EncryptedKey object and returns a DOM Element that
     * represents the appropriate <code>EncryptedKey</code>
     *
     * @param context The document that will own the created nodes
     * @param encryptedKey EncryptedKey object to martial
     * @return the DOM <code>Element</code> representing the passed in
     * object
     */
    public Element martial(Document context, EncryptedKey encryptedKey) {
        contextDocument = context;
        return factory.toElement(encryptedKey);
    }

    /**
     * Martial a ReferenceList
     *
     * Takes a ReferenceList object and returns a DOM Element that
     * represents the appropriate <code>ReferenceList</code>
     *
     * <p>
     * <b>Note:</b> This should only be used in cases where the context
     * document has been passed in via a call to doFinal.
     *
     * @param referenceList ReferenceList object to martial
     * @return the DOM <code>Element</code> representing the passed in
     * object
     */
    public Element martial(ReferenceList referenceList) {
        return factory.toElement(referenceList);
    }

    /**
     * Martial a ReferenceList
     *
     * Takes a ReferenceList object and returns a DOM Element that
     * represents the appropriate <code>ReferenceList</code>
     *
     * @param context The document that will own the created nodes
     * @param referenceList ReferenceList object to martial
     * @return the DOM <code>Element</code> representing the passed in
     * object
     */
    public Element martial(Document context, ReferenceList referenceList) {
        contextDocument = context;
        return factory.toElement(referenceList);
    }

    /**
     * Encrypts an <code>Element</code> and replaces it with its encrypted
     * counterpart in the context <code>Document</code>, that is, the
     * <code>Document</code> specified when one calls
     * {@link #getInstance(String) getInstance}.
     *
     * @param element the <code>Element</code> to encrypt.
     * @return the context <code>Document</code> with the encrypted
     *   <code>Element</code> having replaced the source <code>Element</code>.
     *  @throws Exception
     */
    private Document encryptElement(Element element) throws Exception{
        LOG.debug("Encrypting element...");
        if (null == element) {
            throw new XMLEncryptionException("empty", "Element unexpectedly null...");
        }
        if (cipherMode != ENCRYPT_MODE) {
            throw new XMLEncryptionException("empty", "XMLCipher unexpectedly not in ENCRYPT_MODE...");
        }

        if (algorithm == null) {
            throw new XMLEncryptionException("empty", "XMLCipher instance without transformation specified");
        }
        encryptData(contextDocument, element, false);

        Element encryptedElement = factory.toElement(ed);

        Node sourceParent = element.getParentNode();
        sourceParent.replaceChild(encryptedElement, element);

        return contextDocument;
    }

    /**
     * Encrypts a <code>NodeList</code> (the contents of an
     * <code>Element</code>) and replaces its parent <code>Element</code>'s
     * content with this the resulting <code>EncryptedType</code> within the
     * context <code>Document</code>, that is, the <code>Document</code>
     * specified when one calls
     * {@link #getInstance(String) getInstance}.
     *
     * @param element the <code>NodeList</code> to encrypt.
     * @return the context <code>Document</code> with the encrypted
     *   <code>NodeList</code> having replaced the content of the source
     *   <code>Element</code>.
     * @throws Exception
     */
    private Document encryptElementContent(Element element) throws /* XMLEncryption */Exception {
        LOG.debug("Encrypting element content...");
        if (null == element) {
            throw new XMLEncryptionException("empty", "Element unexpectedly null...");
        }
        if (cipherMode != ENCRYPT_MODE) {
            throw new XMLEncryptionException("empty", "XMLCipher unexpectedly not in ENCRYPT_MODE...");
        }

        if (algorithm == null) {
            throw new XMLEncryptionException("empty", "XMLCipher instance without transformation specified");
        }
        encryptData(contextDocument, element, true);

        Element encryptedElement = factory.toElement(ed);

        removeContent(element);
        element.appendChild(encryptedElement);

        return contextDocument;
    }

    /**
     * Process a DOM <code>Document</code> node. The processing depends on the
     * initialization parameters of {@link #init(int, Key) init()}.
     *
     * @param context the context <code>Document</code>.
     * @param source the <code>Document</code> to be encrypted or decrypted.
     * @return the processed <code>Document</code>.
     * @throws Exception to indicate any exceptional conditions.
     */
    public Document doFinal(Document context, Document source) throws /* XMLEncryption */Exception {
        LOG.debug("Processing source document...");
        if (null == source) {
            throw new XMLEncryptionException("empty", "Source document unexpectedly null...");
        }
        return doFinal(context, source.getDocumentElement());
    }

    /**
     * Process a DOM <code>Element</code> node. The processing depends on the
     * initialization parameters of {@link #init(int, Key) init()}.
     *
     * @param context the context <code>Document</code>.
     * @param element the <code>Element</code> to be encrypted.
     * @return the processed <code>Document</code>.
     * @throws Exception to indicate any exceptional conditions.
     */
    public Document doFinal(Document context, Element element) throws /* XMLEncryption */Exception {
        LOG.debug("Processing source element...");
        if (null == context) {
            throw new XMLEncryptionException("empty", "Context document unexpectedly null...");
        }
        if (null == element) {
            throw new XMLEncryptionException("empty", "Source element unexpectedly null...");
        }

        contextDocument = context;

        Document result = null;

        switch (cipherMode) {
        case DECRYPT_MODE:
            result = decryptElement(element);
            break;
        case ENCRYPT_MODE:
            result = encryptElement(element);
            break;
        case UNWRAP_MODE:
        case WRAP_MODE:
            break;
        default:
            throw new XMLEncryptionException(new IllegalStateException());
        }

        return result;
    }

    /**
     * Process the contents of a DOM <code>Element</code> node. The processing
     * depends on the initialization parameters of
     * {@link #init(int, Key) init()}.
     *
     * @param context the context <code>Document</code>.
     * @param element the <code>Element</code> which contents is to be
     *   encrypted.
     * @param content
     * @return the processed <code>Document</code>.
     * @throws Exception to indicate any exceptional conditions.
     */
    public Document doFinal(Document context, Element element, boolean content)
        throws /* XMLEncryption*/ Exception {
        LOG.debug("Processing source element...");
        if (null == context) {
            throw new XMLEncryptionException("empty", "Context document unexpectedly null...");
        }
        if (null == element) {
            throw new XMLEncryptionException("empty", "Source element unexpectedly null...");

        }

        contextDocument = context;

        Document result = null;

        switch (cipherMode) {
        case DECRYPT_MODE:
            if (content) {
                result = decryptElementContent(element);
            } else {
                result = decryptElement(element);
            }
            break;
        case ENCRYPT_MODE:
            if (content) {
                result = encryptElementContent(element);
            } else {
                result = encryptElement(element);
            }
            break;
        case UNWRAP_MODE:
        case WRAP_MODE:
            break;
        default:
            throw new XMLEncryptionException(new IllegalStateException());
        }

        return result;
    }

    /**
     * Returns an <code>EncryptedData</code> interface. Use this operation if
     * you want to have full control over the contents of the
     * <code>EncryptedData</code> structure.
     *
     * This does not change the source document in any way.
     *
     * @param context the context <code>Document</code>.
     * @param element the <code>Element</code> that will be encrypted.
     * @return the <code>EncryptedData</code>
     * @throws Exception
     */
    public EncryptedData encryptData(Document context, Element element) throws
        /* XMLEncryption */Exception {
        return encryptData(context, element, false);
    }

    /**
     * Returns an <code>EncryptedData</code> interface. Use this operation if
     * you want to have full control over the serialization of the element
     * or element content.
     *
     * This does not change the source document in any way.
     *
     * @param context the context <code>Document</code>.
     * @param type a URI identifying type information about the plaintext form
     *    of the encrypted content (may be <code>null</code>)
     * @param serializedData the serialized data
     * @return the <code>EncryptedData</code>
     * @throws Exception
     */
    public EncryptedData encryptData(
        Document context, String type, InputStream serializedData
    ) throws Exception {
        LOG.debug("Encrypting element...");
        if (null == context) {
            throw new XMLEncryptionException("empty", "Context document unexpectedly null...");
        }
        if (null == serializedData) {
            throw new XMLEncryptionException("empty", "Serialized data unexpectedly null...");
        }
        if (cipherMode != ENCRYPT_MODE) {
            throw new XMLEncryptionException("empty", "XMLCipher unexpectedly not in ENCRYPT_MODE...");
        }

        return encryptData(context, null, type, serializedData);
    }

    /**
     * Returns an <code>EncryptedData</code> interface. Use this operation if
     * you want to have full control over the contents of the
     * <code>EncryptedData</code> structure.
     *
     * This does not change the source document in any way.
     *
     * @param context the context <code>Document</code>.
     * @param element the <code>Element</code> that will be encrypted.
     * @param contentMode <code>true</code> to encrypt element's content only,
     *    <code>false</code> otherwise
     * @return the <code>EncryptedData</code>
     * @throws Exception
     */
    public EncryptedData encryptData(
        Document context, Element element, boolean contentMode
    ) throws /* XMLEncryption */ Exception {
        LOG.debug("Encrypting element...");
        if (null == context) {
            throw new XMLEncryptionException("empty", "Context document unexpectedly null...");
        }
        if (null == element) {
            throw new XMLEncryptionException("empty", "Element unexpectedly null...");
        }
        if (cipherMode != ENCRYPT_MODE) {
            throw new XMLEncryptionException("empty", "XMLCipher unexpectedly not in ENCRYPT_MODE...");
        }

        String type = contentMode ? EncryptionConstants.TYPE_CONTENT : EncryptionConstants.TYPE_ELEMENT;
        return encryptData(context, element, type, null);
    }

    private EncryptedData encryptData(
        Document context, Element element, String type, InputStream serializedData
    ) throws /* XMLEncryption */ Exception {
        contextDocument = context;

        if (algorithm == null) {
            throw new XMLEncryptionException("empty", "XMLCipher instance without transformation specified");
        }
        if (element != null && element.getParentNode() == null) {
            throw new XMLEncryptionException("empty", "The element can't be serialized as it has no parent");
        }

        byte[] serializedOctets = null;
        if (serializedData == null) {
            if (EncryptionConstants.TYPE_CONTENT.equals(type)) {
                if (element == null) {
                    throw new XMLEncryptionException("empty", "Cannot encrypt null element");
                }
                NodeList children = element.getChildNodes();
                if (null != children) {
                    serializedOctets = serializer.serializeToByteArray(children);
                } else {
                    throw new XMLEncryptionException("empty", "Element has no content.");
                }
            } else {
                serializedOctets = serializer.serializeToByteArray(element);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Serialized octets:\n" + new String(serializedOctets, StandardCharsets.UTF_8));
            }
        }

        byte[] encryptedBytes = null;

        // Now create the working cipher if none was created already
        Cipher c;
        if (contextCipher == null) {
            c = constructCipher(algorithm, null);
        } else {
            c = contextCipher;
        }
        // Now perform the encryption

        int ivLen = JCEMapper.getIVLengthFromURI(algorithm) / 8;
        byte[] iv = XMLSecurityConstants.generateBytes(ivLen);
        try {
            AlgorithmParameterSpec paramSpec = constructBlockCipherParameters(algorithm, iv);
            c.init(cipherMode, key, paramSpec);
        } catch (InvalidKeyException ike) {
            throw new XMLEncryptionException(ike);
        }

        try {
            if (serializedData != null) {
                int numBytes;
                byte[] buf = new byte[8192];
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    while ((numBytes = serializedData.read(buf)) != -1) {
                        byte[] data = c.update(buf, 0, numBytes);
                        if (data != null) {
                            baos.write(data);
                        }
                    }
                    baos.write(c.doFinal());
                    encryptedBytes = baos.toByteArray();
                }
            } else {
                encryptedBytes = c.doFinal(serializedOctets);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Expected cipher.outputSize = " +
                        Integer.toString(c.getOutputSize(serializedOctets.length)));
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Actual cipher.outputSize = "
                             + Integer.toString(encryptedBytes.length));
            }
        } catch (IllegalStateException | IllegalBlockSizeException
                | BadPaddingException | UnsupportedEncodingException e) {
            throw new XMLEncryptionException(e);
        }

        // Get IV from Cipher Object. If this is null (see BouncyCastle issue BJA-473) then use
        // the original IV that was generated
        if (c.getIV() != null) {
            iv = c.getIV();
        }
        // Now build up to a properly XML Encryption encoded octet stream
        byte[] finalEncryptedBytes = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, finalEncryptedBytes, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, finalEncryptedBytes, iv.length, encryptedBytes.length);
        String base64EncodedEncryptedOctets = XMLUtils.encodeToString(finalEncryptedBytes);

        LOG.debug("Encrypted octets:\n{}", base64EncodedEncryptedOctets);
        LOG.debug("Encrypted octets length = {}", base64EncodedEncryptedOctets.length());

        try {
            CipherData cd = ed.getCipherData();
            CipherValue cv = cd.getCipherValue();
            cv.setValue(base64EncodedEncryptedOctets);

            if (type != null) {
                ed.setType(new URI(type).toString());
            }
            EncryptionMethod method =
                factory.newEncryptionMethod(new URI(algorithm).toString());
            method.setDigestAlgorithm(digestAlg);
            ed.setEncryptionMethod(method);
        } catch (URISyntaxException ex) {
            throw new XMLEncryptionException(ex);
        }
        return ed;
    }

    /**
     * Build an <code>AlgorithmParameterSpec</code> instance used to initialize a <code>Cipher</code> instance
     * for block cipher encryption and decryption.
     *
     * @param algorithm the XML encryption algorithm URI
     * @param iv the initialization vector
     * @return the newly constructed AlgorithmParameterSpec instance, appropriate for the
     *         specified algorithm
     */
    private AlgorithmParameterSpec constructBlockCipherParameters(String algorithm, byte[] iv) {
        return XMLCipherUtil.constructBlockCipherParameters(algorithm, iv);
    }

    /**
     * Returns an <code>EncryptedData</code> interface. Use this operation if
     * you want to load an <code>EncryptedData</code> structure from a DOM
     * structure and manipulate the contents.
     *
     * @param context the context <code>Document</code>.
     * @param element the <code>Element</code> that will be loaded
     * @throws XMLEncryptionException
     * @return the <code>EncryptedData</code>
     */
    public EncryptedData loadEncryptedData(Document context, Element element)
        throws XMLEncryptionException {
        LOG.debug("Loading encrypted element...");
        if (null == context) {
            throw new XMLEncryptionException("empty", "Context document unexpectedly null...");
        }
        if (null == element) {
            throw new XMLEncryptionException("empty", "Element unexpectedly null...");
        }
        if (cipherMode != DECRYPT_MODE) {
            throw new XMLEncryptionException("empty", "XMLCipher unexpectedly not in DECRYPT_MODE...");
        }

        contextDocument = context;
        ed = factory.newEncryptedData(element);

        return ed;
    }

    /**
     * Returns an <code>EncryptedKey</code> interface. Use this operation if
     * you want to load an <code>EncryptedKey</code> structure from a DOM
     * structure and manipulate the contents.
     *
     * @param context the context <code>Document</code>.
     * @param element the <code>Element</code> that will be loaded
     * @return the <code>EncryptedKey</code>
     * @throws XMLEncryptionException
     */
    public EncryptedKey loadEncryptedKey(Document context, Element element)
        throws XMLEncryptionException {
        LOG.debug("Loading encrypted key...");
        if (null == context) {
            throw new XMLEncryptionException("empty", "Context document unexpectedly null...");
        }
        if (null == element) {
            throw new XMLEncryptionException("empty", "Context document unexpectedly null...");
        }
        if (cipherMode != UNWRAP_MODE && cipherMode != DECRYPT_MODE) {
            throw new XMLEncryptionException("empty",
                "XMLCipher unexpectedly not in UNWRAP_MODE or DECRYPT_MODE..."
            );
        }

        contextDocument = context;
        ek = factory.newEncryptedKey(element);
        return ek;
    }

    /**
     * Returns an <code>EncryptedKey</code> interface. Use this operation if
     * you want to load an <code>EncryptedKey</code> structure from a DOM
     * structure and manipulate the contents.
     *
     * Assumes that the context document is the document that owns the element
     *
     * @param element the <code>Element</code> that will be loaded
     * @return the <code>EncryptedKey</code>
     * @throws XMLEncryptionException
     */
    public EncryptedKey loadEncryptedKey(Element element) throws XMLEncryptionException {
        return loadEncryptedKey(element.getOwnerDocument(), element);
    }

    /**
     * Encrypts a key to an EncryptedKey structure
     *
     * @param doc the Context document that will be used to general DOM
     * @param key Key to encrypt (will use previously set KEK to
     * perform encryption
     * @return the <code>EncryptedKey</code>
     * @throws XMLEncryptionException
     */
    public EncryptedKey encryptKey(Document doc, Key key) throws XMLEncryptionException {
        return encryptKey(doc, key, null, null);
    }

    /**
     * Encrypts a key to an EncryptedKey structure
     *
     * @param doc the Context document that will be used to general DOM
     * @param key Key to encrypt (will use previously set KEK to
     * perform encryption
     * @param mgfAlgorithm The xenc11 MGF Algorithm to use
     * @param oaepParams The OAEPParams to use
     * @return the <code>EncryptedKey</code>
     * @throws XMLEncryptionException
     */
    public EncryptedKey encryptKey(
        Document doc,
        Key key,
        String mgfAlgorithm,
        byte[] oaepParams
    ) throws XMLEncryptionException {
        return encryptKey(doc, key, mgfAlgorithm, oaepParams, null);
    }

    /**
     * Encrypts a key to an EncryptedKey structure
     *
     * @param doc the Context document that will be used to general DOM
     * @param key Key to encrypt (will use previously set KEK to
     * perform encryption
     * @param mgfAlgorithm The xenc11 MGF Algorithm to use
     * @param oaepParams The OAEPParams to use
     * @param random The SecureRandom instance to use when initializing the Cipher
     * @return the <code>EncryptedKey</code>
     * @throws XMLEncryptionException
     */
    public EncryptedKey encryptKey(
        Document doc,
        Key key,
        String mgfAlgorithm,
        byte[] oaepParams,
        SecureRandom random
    ) throws XMLEncryptionException {
        LOG.debug("Encrypting key ...");

        if (null == key) {
            throw new XMLEncryptionException("empty", "Key unexpectedly null...");
        }
        if (cipherMode != WRAP_MODE) {
            throw new XMLEncryptionException("empty", "XMLCipher unexpectedly not in WRAP_MODE...");
        }
        if (algorithm == null) {
            throw new XMLEncryptionException("empty", "XMLCipher instance without transformation specified");
        }

        contextDocument = doc;

        byte[] encryptedBytes = null;
        Cipher c;

        if (contextCipher == null) {
            // Now create the working cipher
            c = constructCipher(algorithm, null);
        } else {
            c = contextCipher;
        }
        // Now perform the encryption

        try {
            // Should internally generate an IV
            // todo - allow user to set an IV
            OAEPParameterSpec oaepParameters =
                constructOAEPParameters(
                    algorithm, digestAlg, mgfAlgorithm, oaepParams
                );
            if (random != null) {
                if (oaepParameters == null) {
                    c.init(Cipher.WRAP_MODE, this.key, random);
                } else {
                    c.init(Cipher.WRAP_MODE, this.key, oaepParameters, random);
                }
            } else {
                if (oaepParameters == null) {
                    c.init(Cipher.WRAP_MODE, this.key);
                } else {
                    c.init(Cipher.WRAP_MODE, this.key, oaepParameters);
                }
            }
            encryptedBytes = c.wrap(key);
        } catch (InvalidKeyException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            throw new XMLEncryptionException(e);
        }

        String base64EncodedEncryptedOctets = XMLUtils.encodeToString(encryptedBytes);
        LOG.debug("Encrypted key octets:\n{}", base64EncodedEncryptedOctets);
        LOG.debug("Encrypted key octets length = {}", base64EncodedEncryptedOctets.length());

        CipherValue cv = ek.getCipherData().getCipherValue();
        cv.setValue(base64EncodedEncryptedOctets);

        try {
            EncryptionMethod method = factory.newEncryptionMethod(new URI(algorithm).toString());
            method.setDigestAlgorithm(digestAlg);
            method.setMGFAlgorithm(mgfAlgorithm);
            method.setOAEPparams(oaepParams);
            ek.setEncryptionMethod(method);
        } catch (URISyntaxException ex) {
            throw new XMLEncryptionException(ex);
        }
        return ek;
    }

    /**
     * Decrypt a key from a passed in EncryptedKey structure
     *
     * @param encryptedKey Previously loaded EncryptedKey that needs
     * to be decrypted.
     * @param algorithm Algorithm for the decrypted key
     * @return a key corresponding to the given type
     * @throws XMLEncryptionException
     */
    public Key decryptKey(EncryptedKey encryptedKey, String algorithm)
        throws XMLEncryptionException {
        LOG.debug("Decrypting key from previously loaded EncryptedKey...");

        if (cipherMode != UNWRAP_MODE) {
            throw new XMLEncryptionException("empty", "XMLCipher unexpectedly not in UNWRAP_MODE...");
        }

        if (algorithm == null) {
            throw new XMLEncryptionException("empty", "Cannot decrypt a key without knowing the algorithm");
        }

        if (key == null) {
            LOG.debug("Trying to find a KEK via key resolvers");

            KeyInfo ki = encryptedKey.getKeyInfo();
            if (ki != null) {
                ki.setSecureValidation(secureValidation);
                try {
                    String keyWrapAlg = encryptedKey.getEncryptionMethod().getAlgorithm();
                    String keyType = JCEMapper.getJCEKeyAlgorithmFromURI(keyWrapAlg);
                    if ("RSA".equals(keyType) || "EC".equals(keyType)) {
                        key = ki.getPrivateKey();
                    } else {
                        key = ki.getSecretKey();
                    }
                }
                catch (Exception e) {
                    LOG.debug(e.getMessage(), e);
                }
            }
            if (key == null) {
                LOG.error("XMLCipher::decryptKey unable to resolve a KEK");
                throw new XMLEncryptionException("empty", "Unable to decrypt without a KEK");
            }
        }

        // Obtain the encrypted octets
        XMLCipherInput cipherInput = new XMLCipherInput(encryptedKey);
        cipherInput.setSecureValidation(secureValidation);
        byte[] encryptedBytes = cipherInput.getBytes();

        String jceKeyAlgorithm = JCEMapper.getJCEKeyAlgorithmFromURI(algorithm);
        LOG.debug("JCE Key Algorithm: {}", jceKeyAlgorithm);

        Cipher c;
        if (contextCipher == null) {
            // Now create the working cipher
            c =
                constructCipher(
                    encryptedKey.getEncryptionMethod().getAlgorithm(),
                    encryptedKey.getEncryptionMethod().getDigestAlgorithm()
                );
        } else {
            c = contextCipher;
        }

        Key ret;

        try {
            EncryptionMethod encMethod = encryptedKey.getEncryptionMethod();
            OAEPParameterSpec oaepParameters =
                constructOAEPParameters(
                    encMethod.getAlgorithm(), encMethod.getDigestAlgorithm(),
                    encMethod.getMGFAlgorithm(), encMethod.getOAEPparams()
                );
            if (oaepParameters == null) {
                c.init(Cipher.UNWRAP_MODE, key);
            } else {
                c.init(Cipher.UNWRAP_MODE, key, oaepParameters);
            }
            ret = c.unwrap(encryptedBytes, jceKeyAlgorithm, Cipher.SECRET_KEY);
        } catch (InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new XMLEncryptionException(e);
        }
        LOG.debug("Decryption of key type {} OK", algorithm);

        return ret;
    }

    /**
     * Construct an OAEPParameterSpec object from the given parameters
     */
    private OAEPParameterSpec constructOAEPParameters(
        String encryptionAlgorithm,
        String digestAlgorithm,
        String mgfAlgorithm,
        byte[] oaepParams
    ) {
        if (XMLCipher.RSA_OAEP.equals(encryptionAlgorithm)
            || XMLCipher.RSA_OAEP_11.equals(encryptionAlgorithm)) {

            String jceDigestAlgorithm = "SHA-1";
            if (digestAlgorithm != null) {
                jceDigestAlgorithm = JCEMapper.translateURItoJCEID(digestAlgorithm);
            }

            PSource.PSpecified pSource = PSource.PSpecified.DEFAULT;
            if (oaepParams != null) {
                pSource = new PSource.PSpecified(oaepParams);
            }

            MGF1ParameterSpec mgfParameterSpec = new MGF1ParameterSpec("SHA-1");
            if (XMLCipher.RSA_OAEP_11.equals(encryptionAlgorithm)) {
                if (EncryptionConstants.MGF1_SHA224.equals(mgfAlgorithm)) {
                    mgfParameterSpec = new MGF1ParameterSpec("SHA-224");
                } else if (EncryptionConstants.MGF1_SHA256.equals(mgfAlgorithm)) {
                    mgfParameterSpec = new MGF1ParameterSpec("SHA-256");
                } else if (EncryptionConstants.MGF1_SHA384.equals(mgfAlgorithm)) {
                    mgfParameterSpec = new MGF1ParameterSpec("SHA-384");
                } else if (EncryptionConstants.MGF1_SHA512.equals(mgfAlgorithm)) {
                    mgfParameterSpec = new MGF1ParameterSpec("SHA-512");
                }
            }
            return new OAEPParameterSpec(jceDigestAlgorithm, "MGF1", mgfParameterSpec, pSource);
        }

        return null;
    }

    /**
     * Construct a Cipher object
     */
    private Cipher constructCipher(String algorithm, String digestAlgorithm) throws XMLEncryptionException {
        String jceAlgorithm = JCEMapper.translateURItoJCEID(algorithm);
        LOG.debug("JCE Algorithm = {}", jceAlgorithm);

        Cipher c;
        try {
            if (requestedJCEProvider == null) {
                c = Cipher.getInstance(jceAlgorithm);
            } else {
                c = Cipher.getInstance(jceAlgorithm, requestedJCEProvider);
            }
        } catch (NoSuchAlgorithmException nsae) {
            // Check to see if an RSA OAEP MGF-1 with SHA-1 algorithm was requested
            // Some JDKs don't support RSA/ECB/OAEPPadding
            c = constructCipher(algorithm, digestAlgorithm, nsae);
        } catch (NoSuchProviderException | NoSuchPaddingException e) {
            throw new XMLEncryptionException(e);
        }

        return c;
    }

    private Cipher constructCipher(String algorithm, String digestAlgorithm, Exception nsae) throws XMLEncryptionException {
        if (!XMLCipher.RSA_OAEP.equals(algorithm)) {
            throw new XMLEncryptionException(nsae);
        }

        if (digestAlgorithm == null
            || MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA1.equals(digestAlgorithm)) {
            try {
                if (requestedJCEProvider == null) {
                    return Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
                } else {
                    return Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding", requestedJCEProvider);
                }
            } catch (Exception ex) {
                throw new XMLEncryptionException(ex);
            }
        } else if (MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA224.equals(digestAlgorithm)) {
            try {
                if (requestedJCEProvider == null) {
                    return Cipher.getInstance("RSA/ECB/OAEPWithSHA-224andMGF1Padding");
                } else {
                    return Cipher.getInstance("RSA/ECB/OAEPWithSHA-224andMGF1Padding", requestedJCEProvider);
                }
            } catch (Exception ex) {
                throw new XMLEncryptionException(ex);
            }
        } else if (MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256.equals(digestAlgorithm)) {
            try {
                if (requestedJCEProvider == null) {
                    return Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
                } else {
                    return Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", requestedJCEProvider);
                }
            } catch (Exception ex) {
                throw new XMLEncryptionException(ex);
            }
        } else if (MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA384.equals(digestAlgorithm)) {
            try {
                if (requestedJCEProvider == null) {
                    return Cipher.getInstance("RSA/ECB/OAEPWithSHA-384AndMGF1Padding");
                } else {
                    return Cipher.getInstance("RSA/ECB/OAEPWithSHA-384AndMGF1Padding", requestedJCEProvider);
                }
            } catch (Exception ex) {
                throw new XMLEncryptionException(ex);
            }
        } else if (MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA512.equals(digestAlgorithm)) {
            try {
                if (requestedJCEProvider == null) {
                    return Cipher.getInstance("RSA/ECB/OAEPWithSHA-512AndMGF1Padding");
                } else {
                    return Cipher.getInstance("RSA/ECB/OAEPWithSHA-512AndMGF1Padding", requestedJCEProvider);
                }
            } catch (Exception ex) {
                throw new XMLEncryptionException(ex);
            }
        } else {
            throw new XMLEncryptionException(nsae);
        }
    }

    /**
     * Decrypt a key from a passed in EncryptedKey structure.  This version
     * is used mainly internally, when  the cipher already has an
     * EncryptedData loaded.  The algorithm URI will be read from the
     * EncryptedData
     *
     * @param encryptedKey Previously loaded EncryptedKey that needs
     * to be decrypted.
     * @return a key corresponding to the given type
     * @throws XMLEncryptionException
     */
    public Key decryptKey(EncryptedKey encryptedKey) throws XMLEncryptionException {
        return decryptKey(encryptedKey, ed.getEncryptionMethod().getAlgorithm());
    }

    /**
     * Removes the contents of a <code>Node</code>.
     *
     * @param node the <code>Node</code> to clear.
     */
    private static void removeContent(Node node) {
        while (node.hasChildNodes()) {
            node.removeChild(node.getFirstChild());
        }
    }

    /**
     * Decrypts <code>EncryptedData</code> in a single-part operation.
     *
     * @param element the <code>EncryptedData</code> to decrypt.
     * @return the <code>Node</code> as a result of the decrypt operation.
     * @throws XMLEncryptionException
     */
    private Document decryptElement(Element element) throws XMLEncryptionException {
        LOG.debug("Decrypting element...");

        if (element == null) {
            throw new XMLEncryptionException("empty", "Cannot decrypt null element");
        } else if (element.getParentNode() == null) {
            throw new XMLEncryptionException("empty", "The element can't be serialized as it has no parent");
        }

        if (cipherMode != DECRYPT_MODE) {
            throw new XMLEncryptionException("empty", "XMLCipher unexpectedly not in DECRYPT_MODE...");
        }

        byte[] octets = decryptToByteArray(element);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Decrypted octets:\n" + new String(octets));
        }

        Node sourceParent = element.getParentNode();
        try {
            Node decryptedNode = serializer.deserialize(octets, sourceParent);

            // The de-serialiser returns a node whose children we need to take on.
            if (sourceParent != null && Node.DOCUMENT_NODE == sourceParent.getNodeType()) {
                // If this is a content decryption, this may have problems
                contextDocument.removeChild(contextDocument.getDocumentElement());
                contextDocument.appendChild(decryptedNode);
            } else if (sourceParent != null) {
                sourceParent.replaceChild(decryptedNode, element);
            }
        } catch (IOException ex) {
            throw new XMLEncryptionException(ex);
        }

        return contextDocument;
    }

    /**
     *
     * @param element
     * @return the <code>Node</code> as a result of the decrypt operation.
     * @throws XMLEncryptionException
     */
    private Document decryptElementContent(Element element) throws XMLEncryptionException {
        Element e =
            (Element) element.getElementsByTagNameNS(
                EncryptionConstants.EncryptionSpecNS,
                EncryptionConstants._TAG_ENCRYPTEDDATA
            ).item(0);

        if (null == e) {
            throw new XMLEncryptionException("empty", "No EncryptedData child element.");
        }

        return decryptElement(e);
    }

    /**
     * Decrypt an EncryptedData element to a byte array.
     *
     * When passed in an EncryptedData node, returns the decryption
     * as a byte array.
     *
     * Does not modify the source document.
     * @param element
     * @return the bytes resulting from the decryption
     * @throws XMLEncryptionException
     */
    public byte[] decryptToByteArray(Element element) throws XMLEncryptionException {
        LOG.debug("Decrypting to ByteArray...");

        if (cipherMode != DECRYPT_MODE) {
            throw new XMLEncryptionException("empty", "XMLCipher unexpectedly not in DECRYPT_MODE...");
        }

        EncryptedData encryptedData = factory.newEncryptedData(element);
        String encMethodAlgorithm = encryptedData.getEncryptionMethod().getAlgorithm();

        if (key == null) {
            KeyInfo ki = encryptedData.getKeyInfo();
            if (ki != null) {
                try {
                    // Add an EncryptedKey resolver
                    EncryptedKeyResolver resolver = new EncryptedKeyResolver(encMethodAlgorithm, kek, internalKeyResolvers);
                    ki.registerInternalKeyResolver(resolver);
                    ki.setSecureValidation(secureValidation);
                    key = ki.getSecretKey();
                } catch (KeyResolverException kre) {
                    LOG.debug(kre.getMessage(), kre);
                }
            }

            if (key == null) {
                LOG.error(
                    "XMLCipher::decryptElement unable to resolve a decryption key"
                );
                throw new XMLEncryptionException("empty", "encryption.nokey");
            }
        }

        // Obtain the encrypted octets
        XMLCipherInput cipherInput = new XMLCipherInput(encryptedData);
        cipherInput.setSecureValidation(secureValidation);
        byte[] encryptedBytes = cipherInput.getBytes();

        // Now create the working cipher
        String jceAlgorithm =
            JCEMapper.translateURItoJCEID(encMethodAlgorithm);
        LOG.debug("JCE Algorithm = {}", jceAlgorithm);

        Cipher c;
        try {
            if (requestedJCEProvider == null) {
                c = Cipher.getInstance(jceAlgorithm);
            } else {
                c = Cipher.getInstance(jceAlgorithm, requestedJCEProvider);
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException e) {
            throw new XMLEncryptionException(e);
        }

        int ivLen = JCEMapper.getIVLengthFromURI(encMethodAlgorithm) / 8;
        byte[] ivBytes = new byte[ivLen];

        // You may be able to pass the entire piece in to IvParameterSpec
        // and it will only take the first x bytes, but no way to be certain
        // that this will work for every JCE provider, so lets copy the
        // necessary bytes into a dedicated array.

        System.arraycopy(encryptedBytes, 0, ivBytes, 0, ivLen);

        String blockCipherAlg = algorithm;
        if (blockCipherAlg == null) {
            blockCipherAlg = encMethodAlgorithm;
        }
        AlgorithmParameterSpec paramSpec = constructBlockCipherParameters(blockCipherAlg, ivBytes);

        try {
            c.init(cipherMode, key, paramSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new XMLEncryptionException(e);
        }

        try {
            return c.doFinal(encryptedBytes, ivLen, encryptedBytes.length - ivLen);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new XMLEncryptionException(e);
        }
    }

    /*
     * Expose the interface for creating XML Encryption objects
     */

    /**
     * Creates an <code>EncryptedData</code> <code>Element</code>.
     *
     * The newEncryptedData and newEncryptedKey methods create fairly complete
     * elements that are immediately useable.  All the other create* methods
     * return bare elements that still need to be built upon.
     *<p>
     * An EncryptionMethod will still need to be added however
     *
     * @param type Either REFERENCE_TYPE or VALUE_TYPE - defines what kind of
     * CipherData this EncryptedData will contain.
     * @param value the Base 64 encoded, encrypted text to wrap in the
     *   <code>EncryptedData</code> or the URI to set in the CipherReference
     * (usage will depend on the <code>type</code>
     * @return the <code>EncryptedData</code> <code>Element</code>.
     *
     * <!--
     * <EncryptedData Id[OPT] Type[OPT] MimeType[OPT] Encoding[OPT]>
     *     <EncryptionMethod/>[OPT]
     *     <ds:KeyInfo>[OPT]
     *         <EncryptedKey/>[OPT]
     *         <AgreementMethod/>[OPT]
     *         <ds:KeyName/>[OPT]
     *         <ds:RetrievalMethod/>[OPT]
     *         <ds:[MUL]/>[OPT]
     *     </ds:KeyInfo>
     *     <CipherData>[MAN]
     *         <CipherValue/> XOR <CipherReference/>
     *     </CipherData>
     *     <EncryptionProperties/>[OPT]
     * </EncryptedData>
     * -->
     * @throws XMLEncryptionException
     */
    public EncryptedData createEncryptedData(int type, String value) throws XMLEncryptionException {
        EncryptedData result = null;
        CipherData data = null;

        if (CipherData.REFERENCE_TYPE == type) {
            CipherReference cipherReference = factory.newCipherReference(value);
            data = factory.newCipherData(type);
            data.setCipherReference(cipherReference);
            result = factory.newEncryptedData(data);
        } else if (CipherData.VALUE_TYPE == type) {
            CipherValue cipherValue = factory.newCipherValue(value);
            data = factory.newCipherData(type);
            data.setCipherValue(cipherValue);
            result = factory.newEncryptedData(data);
        }

        return result;
    }

    /**
     * Creates an <code>EncryptedKey</code> <code>Element</code>.
     *
     * The newEncryptedData and newEncryptedKey methods create fairly complete
     * elements that are immediately useable.  All the other create* methods
     * return bare elements that still need to be built upon.
     *<p>
     * An EncryptionMethod will still need to be added however
     *
     * @param type Either REFERENCE_TYPE or VALUE_TYPE - defines what kind of
     * CipherData this EncryptedData will contain.
     * @param value the Base 64 encoded, encrypted text to wrap in the
     *   <code>EncryptedKey</code> or the URI to set in the CipherReference
     * (usage will depend on the <code>type</code>
     * @return the <code>EncryptedKey</code> <code>Element</code>.
     *
     * <!--
     * <EncryptedKey Id[OPT] Type[OPT] MimeType[OPT] Encoding[OPT]>
     *     <EncryptionMethod/>[OPT]
     *     <ds:KeyInfo>[OPT]
     *         <EncryptedKey/>[OPT]
     *         <AgreementMethod/>[OPT]
     *         <ds:KeyName/>[OPT]
     *         <ds:RetrievalMethod/>[OPT]
     *         <ds:[MUL]/>[OPT]
     *     </ds:KeyInfo>
     *     <CipherData>[MAN]
     *         <CipherValue/> XOR <CipherReference/>
     *     </CipherData>
     *     <EncryptionProperties/>[OPT]
     * </EncryptedData>
     * -->
     * @throws XMLEncryptionException
     */
    public EncryptedKey createEncryptedKey(int type, String value) throws XMLEncryptionException {
        EncryptedKey result = null;
        CipherData data = null;

        if (CipherData.REFERENCE_TYPE == type) {
            CipherReference cipherReference = factory.newCipherReference(value);
            data = factory.newCipherData(type);
            data.setCipherReference(cipherReference);
            result = factory.newEncryptedKey(data);
        } else if (CipherData.VALUE_TYPE == type) {
            CipherValue cipherValue = factory.newCipherValue(value);
            data = factory.newCipherData(type);
            data.setCipherValue(cipherValue);
            result = factory.newEncryptedKey(data);
        }

        return result;
    }

    /**
     * Create an AgreementMethod object
     *
     * @param algorithm Algorithm of the agreement method
     * @return a new <code>AgreementMethod</code>
     */
    public AgreementMethod createAgreementMethod(String algorithm) {
        return factory.newAgreementMethod(algorithm);
    }

    /**
     * Create a CipherData object
     *
     * @param type Type of this CipherData (either VALUE_TUPE or
     * REFERENCE_TYPE)
     * @return a new <code>CipherData</code>
     */
    public CipherData createCipherData(int type) {
        return factory.newCipherData(type);
    }

    /**
     * Create a CipherReference object
     *
     * @param uri The URI that the reference will refer
     * @return a new <code>CipherReference</code>
     */
    public CipherReference createCipherReference(String uri) {
        return factory.newCipherReference(uri);
    }

    /**
     * Create a CipherValue element
     *
     * @param value The value to set the ciphertext to
     * @return a new <code>CipherValue</code>
     */
    public CipherValue createCipherValue(String value) {
        return factory.newCipherValue(value);
    }

    /**
     * Create an EncryptionMethod object
     *
     * @param algorithm Algorithm for the encryption
     * @return a new <code>EncryptionMethod</code>
     */
    public EncryptionMethod createEncryptionMethod(String algorithm) {
        return factory.newEncryptionMethod(algorithm);
    }

    /**
     * Create an EncryptionProperties element
     * @return a new <code>EncryptionProperties</code>
     */
    public EncryptionProperties createEncryptionProperties() {
        return factory.newEncryptionProperties();
    }

    /**
     * Create a new EncryptionProperty element
     * @return a new <code>EncryptionProperty</code>
     */
    public EncryptionProperty createEncryptionProperty() {
        return factory.newEncryptionProperty();
    }

    /**
     * Create a new ReferenceList object
     * @param type ReferenceList.DATA_REFERENCE or ReferenceList.KEY_REFERENCE
     * @return a new <code>ReferenceList</code>
     */
    public ReferenceList createReferenceList(int type) {
        return factory.newReferenceList(type);
    }

    /**
     * Create a new Transforms object
     * <p>
     * <b>Note</b>: A context document <i>must</i> have been set
     * elsewhere (possibly via a call to doFinal).  If not, use the
     * createTransforms(Document) method.
     * @return a new <code>Transforms</code>
     */
    public Transforms createTransforms() {
        return factory.newTransforms();
    }

    /**
     * Create a new Transforms object
     *
     * Because the handling of Transforms is currently done in the signature
     * code, the creation of a Transforms object <b>requires</b> a
     * context document.
     *
     * @param doc Document that will own the created Transforms node
     * @return a new <code>Transforms</code>
     */
    public Transforms createTransforms(Document doc) {
        return factory.newTransforms(doc);
    }

    /**
     *
     */
    private class Factory {
        /**
         * @param algorithm
         * @return a new AgreementMethod
         */
        AgreementMethod newAgreementMethod(String algorithm)  {
            return new AgreementMethodImpl(algorithm);
        }

        /**
         * @param type
         * @return a new CipherData
         *
         */
        CipherData newCipherData(int type) {
            return new CipherDataImpl(type);
        }

        /**
         * @param uri
         * @return a new CipherReference
         */
        CipherReference newCipherReference(String uri)  {
            return new CipherReferenceImpl(uri);
        }

        /**
         * @param value
         * @return a new CipherValue
         */
        CipherValue newCipherValue(String value) {
            return new CipherValueImpl(value);
        }

        /*
        CipherValue newCipherValue(byte[] value) {
            return new CipherValueImpl(value);
        }
         */

        /**
         * @param data
         * @return a new EncryptedData
         */
        EncryptedData newEncryptedData(CipherData data) {
            return new EncryptedDataImpl(data);
        }

        /**
         * @param data
         * @return a new EncryptedKey
         */
        EncryptedKey newEncryptedKey(CipherData data) {
            return new EncryptedKeyImpl(data);
        }

        /**
         * @param algorithm
         * @return a new EncryptionMethod
         */
        EncryptionMethod newEncryptionMethod(String algorithm) {
            return new EncryptionMethodImpl(algorithm);
        }

        /**
         * @return a new EncryptionProperties
         */
        EncryptionProperties newEncryptionProperties() {
            return new EncryptionPropertiesImpl();
        }

        /**
         * @return a new EncryptionProperty
         */
        EncryptionProperty newEncryptionProperty() {
            return new EncryptionPropertyImpl();
        }

        /**
         * @param type ReferenceList.DATA_REFERENCE or ReferenceList.KEY_REFERENCE
         * @return a new ReferenceList
         */
        ReferenceList newReferenceList(int type) {
            return new ReferenceListImpl(type);
        }

        /**
         * @return a new Transforms
         */
        Transforms newTransforms() {
            return new TransformsImpl();
        }

        /**
         * @param doc
         * @return a new Transforms
         */
        Transforms newTransforms(Document doc) {
            return new TransformsImpl(doc);
        }

        /**
         * @param element
         * @return a new CipherData
         * @throws XMLEncryptionException
         */
        CipherData newCipherData(Element element) throws XMLEncryptionException {
            if (null == element) {
                throw new NullPointerException("element is null");
            }

            int type = 0;
            Element e = null;
            if (element.getElementsByTagNameNS(
                EncryptionConstants.EncryptionSpecNS,
                EncryptionConstants._TAG_CIPHERVALUE).getLength() > 0
            ) {
                type = CipherData.VALUE_TYPE;
                e = (Element) element.getElementsByTagNameNS(
                    EncryptionConstants.EncryptionSpecNS,
                    EncryptionConstants._TAG_CIPHERVALUE).item(0);
            } else if (element.getElementsByTagNameNS(
                EncryptionConstants.EncryptionSpecNS,
                EncryptionConstants._TAG_CIPHERREFERENCE).getLength() > 0) {
                type = CipherData.REFERENCE_TYPE;
                e = (Element) element.getElementsByTagNameNS(
                    EncryptionConstants.EncryptionSpecNS,
                    EncryptionConstants._TAG_CIPHERREFERENCE).item(0);
            }

            CipherData result = newCipherData(type);
            if (type == CipherData.VALUE_TYPE) {
                result.setCipherValue(newCipherValue(e));
            } else if (type == CipherData.REFERENCE_TYPE) {
                result.setCipherReference(newCipherReference(e));
            }

            return result;
        }

        /**
         * @param element
         * @return a new CipherReference
         * @throws XMLEncryptionException
         *
         */
        CipherReference newCipherReference(Element element) throws XMLEncryptionException {

            Attr uriAttr =
                element.getAttributeNodeNS(null, EncryptionConstants._ATT_URI);
            CipherReference result = new CipherReferenceImpl(uriAttr);

            // Find any Transforms
            NodeList transformsElements =
                element.getElementsByTagNameNS(
                    EncryptionConstants.EncryptionSpecNS, EncryptionConstants._TAG_TRANSFORMS);
            Element transformsElement = (Element) transformsElements.item(0);

            if (transformsElement != null) {
                LOG.debug("Creating a DSIG based Transforms element");
                try {
                    result.setTransforms(new TransformsImpl(transformsElement));
                } catch (XMLSecurityException e) {
                    throw new XMLEncryptionException(e);
                }
            }

            return result;
        }

        /**
         * @param element
         * @return a new CipherValue
         */
        CipherValue newCipherValue(Element element) {
            String value = XMLUtils.getFullTextChildrenFromNode(element);

            return newCipherValue(value);
        }

        /**
         * @param element
         * @return a new EncryptedData
         * @throws XMLEncryptionException
         *
         */
        EncryptedData newEncryptedData(Element element) throws XMLEncryptionException {
            EncryptedData result = null;

            NodeList dataElements =
                element.getElementsByTagNameNS(
                    EncryptionConstants.EncryptionSpecNS, EncryptionConstants._TAG_CIPHERDATA);

            // Need to get the last CipherData found, as earlier ones will
            // be for elements in the KeyInfo lists

            Element dataElement =
                (Element) dataElements.item(dataElements.getLength() - 1);

            CipherData data = newCipherData(dataElement);

            result = newEncryptedData(data);

            result.setId(element.getAttributeNS(null, EncryptionConstants._ATT_ID));
            result.setType(element.getAttributeNS(null, EncryptionConstants._ATT_TYPE));
            result.setMimeType(element.getAttributeNS(null, EncryptionConstants._ATT_MIMETYPE));
            result.setEncoding(element.getAttributeNS(null, Constants._ATT_ENCODING));

            Element encryptionMethodElement =
                (Element) element.getElementsByTagNameNS(
                    EncryptionConstants.EncryptionSpecNS,
                    EncryptionConstants._TAG_ENCRYPTIONMETHOD).item(0);
            if (null != encryptionMethodElement) {
                result.setEncryptionMethod(newEncryptionMethod(encryptionMethodElement));
            }

            // BFL 16/7/03 - simple implementation
            // TODO: Work out how to handle relative URI

            Element keyInfoElement =
                (Element) element.getElementsByTagNameNS(
                    Constants.SignatureSpecNS, Constants._TAG_KEYINFO).item(0);
            if (null != keyInfoElement) {
                KeyInfo ki = newKeyInfo(keyInfoElement);
                result.setKeyInfo(ki);
            }

            Element encryptionPropertiesElement =
                (Element) element.getElementsByTagNameNS(
                    EncryptionConstants.EncryptionSpecNS,
                    EncryptionConstants._TAG_ENCRYPTIONPROPERTIES).item(0);
            if (null != encryptionPropertiesElement) {
                result.setEncryptionProperties(
                    newEncryptionProperties(encryptionPropertiesElement)
                );
            }

            return result;
        }

        /**
         * @param element
         * @return a new EncryptedKey
         * @throws XMLEncryptionException
         */
        EncryptedKey newEncryptedKey(Element element) throws XMLEncryptionException {
            EncryptedKey result = null;
            NodeList dataElements =
                element.getElementsByTagNameNS(
                    EncryptionConstants.EncryptionSpecNS, EncryptionConstants._TAG_CIPHERDATA);
            Element dataElement =
                (Element) dataElements.item(dataElements.getLength() - 1);

            CipherData data = newCipherData(dataElement);
            result = newEncryptedKey(data);

            result.setId(element.getAttributeNS(null, EncryptionConstants._ATT_ID));
            result.setType(element.getAttributeNS(null, EncryptionConstants._ATT_TYPE));
            result.setMimeType(element.getAttributeNS(null, EncryptionConstants._ATT_MIMETYPE));
            result.setEncoding(element.getAttributeNS(null, Constants._ATT_ENCODING));
            result.setRecipient(element.getAttributeNS(null, EncryptionConstants._ATT_RECIPIENT));

            Element encryptionMethodElement =
                (Element) element.getElementsByTagNameNS(
                    EncryptionConstants.EncryptionSpecNS,
                    EncryptionConstants._TAG_ENCRYPTIONMETHOD).item(0);
            if (null != encryptionMethodElement) {
                result.setEncryptionMethod(newEncryptionMethod(encryptionMethodElement));
            }

            Element keyInfoElement =
                (Element) element.getElementsByTagNameNS(
                    Constants.SignatureSpecNS, Constants._TAG_KEYINFO).item(0);
            if (null != keyInfoElement) {
                KeyInfo ki = newKeyInfo(keyInfoElement);
                result.setKeyInfo(ki);
            }

            Element encryptionPropertiesElement =
                (Element) element.getElementsByTagNameNS(
                    EncryptionConstants.EncryptionSpecNS,
                    EncryptionConstants._TAG_ENCRYPTIONPROPERTIES).item(0);
            if (null != encryptionPropertiesElement) {
                result.setEncryptionProperties(
                    newEncryptionProperties(encryptionPropertiesElement)
                );
            }

            Element referenceListElement =
                (Element) element.getElementsByTagNameNS(
                    EncryptionConstants.EncryptionSpecNS,
                    EncryptionConstants._TAG_REFERENCELIST).item(0);
            if (null != referenceListElement) {
                result.setReferenceList(newReferenceList(referenceListElement));
            }

            Element carriedNameElement =
                (Element) element.getElementsByTagNameNS(
                    EncryptionConstants.EncryptionSpecNS,
                    EncryptionConstants._TAG_CARRIEDKEYNAME).item(0);
            if (null != carriedNameElement) {
                result.setCarriedName(carriedNameElement.getFirstChild().getNodeValue());
            }

            return result;
        }

        /**
         * @param element
         * @return a new KeyInfo
         * @throws XMLEncryptionException
         */
        KeyInfo newKeyInfo(Element element) throws XMLEncryptionException {
            try {
                KeyInfo ki = new KeyInfo(element, null);
                ki.setSecureValidation(secureValidation);
                if (internalKeyResolvers != null) {
                    int size = internalKeyResolvers.size();
                    for (int i = 0; i < size; i++) {
                        ki.registerInternalKeyResolver(internalKeyResolvers.get(i));
                    }
                }
                return ki;
            } catch (XMLSecurityException xse) {
                throw new XMLEncryptionException(xse, "KeyInfo.error");
            }
        }

        /**
         * @param element
         * @return a new EncryptionMethod
         */
        EncryptionMethod newEncryptionMethod(Element element) {
            String encAlgorithm = element.getAttributeNS(null, EncryptionConstants._ATT_ALGORITHM);
            EncryptionMethod result = newEncryptionMethod(encAlgorithm);

            Element keySizeElement =
                (Element) element.getElementsByTagNameNS(
                    EncryptionConstants.EncryptionSpecNS,
                    EncryptionConstants._TAG_KEYSIZE).item(0);
            if (null != keySizeElement) {
                result.setKeySize(
                    Integer.parseInt(
                        keySizeElement.getFirstChild().getNodeValue()));
            }

            Element oaepParamsElement =
                (Element) element.getElementsByTagNameNS(
                    EncryptionConstants.EncryptionSpecNS,
                    EncryptionConstants._TAG_OAEPPARAMS).item(0);
            if (null != oaepParamsElement) {
                String oaepParams = oaepParamsElement.getFirstChild().getNodeValue();
                result.setOAEPparams(XMLUtils.decode(oaepParams.getBytes(StandardCharsets.UTF_8)));
            }

            Element digestElement =
                (Element) element.getElementsByTagNameNS(
                    Constants.SignatureSpecNS, Constants._TAG_DIGESTMETHOD).item(0);
            if (digestElement != null) {
                String digestAlgorithm = digestElement.getAttributeNS(null, "Algorithm");
                result.setDigestAlgorithm(digestAlgorithm);
            }

            Element mgfElement =
                (Element) element.getElementsByTagNameNS(
                    EncryptionConstants.EncryptionSpec11NS, EncryptionConstants._TAG_MGF).item(0);
            if (mgfElement != null && !XMLCipher.RSA_OAEP.equals(algorithm)) {
                String mgfAlgorithm = mgfElement.getAttributeNS(null, "Algorithm");
                result.setMGFAlgorithm(mgfAlgorithm);
            }

            // TODO: Make this mess work
            // <any namespace='##other' minOccurs='0' maxOccurs='unbounded'/>

            return result;
        }

        /**
         * @param element
         * @return a new EncryptionProperties
         */
        EncryptionProperties newEncryptionProperties(Element element) {
            EncryptionProperties result = newEncryptionProperties();

            result.setId(element.getAttributeNS(null, EncryptionConstants._ATT_ID));

            NodeList encryptionPropertyList =
                element.getElementsByTagNameNS(
                    EncryptionConstants.EncryptionSpecNS,
                    EncryptionConstants._TAG_ENCRYPTIONPROPERTY);
            int length = encryptionPropertyList.getLength();
            for (int i = 0; i < length; i++) {
                Node n = encryptionPropertyList.item(i);
                if (null != n) {
                    result.addEncryptionProperty(newEncryptionProperty((Element) n));
                }
            }

            return result;
        }

        /**
         * @param element
         * @return a new EncryptionProperty
         */
        EncryptionProperty newEncryptionProperty(Element element) {
            EncryptionProperty result = newEncryptionProperty();

            result.setTarget(element.getAttributeNS(null, EncryptionConstants._ATT_TARGET));
            result.setId(element.getAttributeNS(null, EncryptionConstants._ATT_ID));
            // TODO: Make this lot work...
            // <anyAttribute namespace="http://www.w3.org/XML/1998/namespace"/>

            // TODO: Make this work...
            // <any namespace='##other' processContents='lax'/>

            return result;
        }

        /**
         * @param element
         * @return a new ReferenceList
         */
        ReferenceList newReferenceList(Element element) {
            int type = 0;
            if (null != element.getElementsByTagNameNS(
                EncryptionConstants.EncryptionSpecNS,
                EncryptionConstants._TAG_DATAREFERENCE).item(0)) {
                type = ReferenceList.DATA_REFERENCE;
            } else if (null != element.getElementsByTagNameNS(
                EncryptionConstants.EncryptionSpecNS,
                EncryptionConstants._TAG_KEYREFERENCE).item(0)) {
                type = ReferenceList.KEY_REFERENCE;
            }

            ReferenceList result = new ReferenceListImpl(type);
            NodeList list = null;
            if (ReferenceList.DATA_REFERENCE == type) {
                list =
                    element.getElementsByTagNameNS(
                        EncryptionConstants.EncryptionSpecNS,
                        EncryptionConstants._TAG_DATAREFERENCE);
                int drLength = list.getLength();
                for (int i = 0; i < drLength; i++) {
                    String uri = ((Element) list.item(i)).getAttributeNS(null, "URI");
                    result.add(result.newDataReference(uri));
                }
            } else if (ReferenceList.KEY_REFERENCE == type) {
                list =
                    element.getElementsByTagNameNS(
                        EncryptionConstants.EncryptionSpecNS,
                        EncryptionConstants._TAG_KEYREFERENCE);
                int krLength = list.getLength();
                for (int i = 0; i < krLength; i++) {
                    String uri = ((Element) list.item(i)).getAttributeNS(null, "URI");
                    result.add(result.newKeyReference(uri));
                }
            }

            return result;
        }

        /**
         * @param encryptedData
         * @return the XML Element form of that EncryptedData
         */
        Element toElement(EncryptedData encryptedData) {
            return ((EncryptedDataImpl) encryptedData).toElement();
        }

        /**
         * @param encryptedKey
         * @return the XML Element form of that EncryptedKey
         */
        Element toElement(EncryptedKey encryptedKey) {
            return ((EncryptedKeyImpl) encryptedKey).toElement();
        }

        /**
         * @param referenceList
         * @return the XML Element form of that ReferenceList
         */
        Element toElement(ReferenceList referenceList) {
            return ((ReferenceListImpl) referenceList).toElement();
        }

        private class AgreementMethodImpl implements AgreementMethod {
            private byte[] kaNonce;
            private List<Element> agreementMethodInformation;
            private KeyInfo originatorKeyInfo;
            private KeyInfo recipientKeyInfo;
            private String algorithmURI;

            /**
             * @param algorithm
             */
            public AgreementMethodImpl(String algorithm) {
                agreementMethodInformation = new LinkedList<>();
                URI tmpAlgorithm = null;
                try {
                    tmpAlgorithm = new URI(algorithm);
                } catch (URISyntaxException ex) {
                    throw (IllegalArgumentException)
                            new IllegalArgumentException().initCause(ex);
                }
                algorithmURI = tmpAlgorithm.toString();
            }

            /** {@inheritDoc} */
            @Override
            public byte[] getKANonce() {
                return kaNonce;
            }

            /** {@inheritDoc} */
            @Override
            public void setKANonce(byte[] kanonce) {
                kaNonce = kanonce;
            }

            /** {@inheritDoc} */
            @Override
            public Iterator<Element> getAgreementMethodInformation() {
                return agreementMethodInformation.iterator();
            }

            /** {@inheritDoc} */
            @Override
            public void addAgreementMethodInformation(Element info) {
                agreementMethodInformation.add(info);
            }

            /** {@inheritDoc} */
            @Override
            public void revoveAgreementMethodInformation(Element info) {
                agreementMethodInformation.remove(info);
            }

            /** {@inheritDoc} */
            @Override
            public KeyInfo getOriginatorKeyInfo() {
                return originatorKeyInfo;
            }

            /** {@inheritDoc} */
            @Override
            public void setOriginatorKeyInfo(KeyInfo keyInfo) {
                originatorKeyInfo = keyInfo;
            }

            /** {@inheritDoc} */
            @Override
            public KeyInfo getRecipientKeyInfo() {
                return recipientKeyInfo;
            }

            /** {@inheritDoc} */
            @Override
            public void setRecipientKeyInfo(KeyInfo keyInfo) {
                recipientKeyInfo = keyInfo;
            }

            /** {@inheritDoc} */
            @Override
            public String getAlgorithm() {
                return algorithmURI;
            }
        }

        private class CipherDataImpl implements CipherData {
            private static final String valueMessage =
                "Data type is reference type.";
            private static final String referenceMessage =
                "Data type is value type.";
            private CipherValue cipherValue;
            private CipherReference cipherReference;
            private int cipherType = Integer.MIN_VALUE;

            /**
             * @param type
             */
            public CipherDataImpl(int type) {
                cipherType = type;
            }

            /** {@inheritDoc} */
            @Override
            public CipherValue getCipherValue() {
                return cipherValue;
            }

            /** {@inheritDoc} */
            @Override
            public void setCipherValue(CipherValue value) throws XMLEncryptionException {

                if (cipherType == REFERENCE_TYPE) {
                    throw new XMLEncryptionException(new UnsupportedOperationException(valueMessage));
                }

                cipherValue = value;
            }

            /** {@inheritDoc} */
            @Override
            public CipherReference getCipherReference() {
                return cipherReference;
            }

            /** {@inheritDoc} */
            @Override
            public void setCipherReference(CipherReference reference) throws
            XMLEncryptionException {
                if (cipherType == VALUE_TYPE) {
                    throw new XMLEncryptionException(
                        new UnsupportedOperationException(referenceMessage)
                    );
                }

                cipherReference = reference;
            }

            /** {@inheritDoc} */
            @Override
            public int getDataType() {
                return cipherType;
            }

            Element toElement() {
                Element result =
                    XMLUtils.createElementInEncryptionSpace(
                        contextDocument, EncryptionConstants._TAG_CIPHERDATA
                    );
                if (cipherType == VALUE_TYPE) {
                    result.appendChild(((CipherValueImpl) cipherValue).toElement());
                } else if (cipherType == REFERENCE_TYPE) {
                    result.appendChild(((CipherReferenceImpl) cipherReference).toElement());
                }

                return result;
            }
        }

        private class CipherReferenceImpl implements CipherReference {
            private String referenceURI;
            private Transforms referenceTransforms;
            private Attr referenceNode;

            /**
             * @param uri
             */
            public CipherReferenceImpl(String uri) {
                /* Don't check validity of URI as may be "" */
                referenceURI = uri;
                referenceNode = null;
            }

            /**
             * @param uri
             */
            public CipherReferenceImpl(Attr uri) {
                referenceURI = uri.getNodeValue();
                referenceNode = uri;
            }

            /** {@inheritDoc} */
            @Override
            public String getURI() {
                return referenceURI;
            }

            /** {@inheritDoc} */
            @Override
            public Attr getURIAsAttr() {
                return referenceNode;
            }

            /** {@inheritDoc} */
            @Override
            public Transforms getTransforms() {
                return referenceTransforms;
            }

            /** {@inheritDoc} */
            @Override
            public void setTransforms(Transforms transforms) {
                referenceTransforms = transforms;
            }

            Element toElement() {
                Element result =
                    XMLUtils.createElementInEncryptionSpace(
                        contextDocument, EncryptionConstants._TAG_CIPHERREFERENCE
                    );
                result.setAttributeNS(null, EncryptionConstants._ATT_URI, referenceURI);
                if (null != referenceTransforms) {
                    result.appendChild(((TransformsImpl) referenceTransforms).toElement());
                }

                return result;
            }
        }

        private class CipherValueImpl implements CipherValue {
            private String cipherValue;

            /**
             * @param value
             */
            public CipherValueImpl(String value) {
                cipherValue = value;
            }

            /** {@inheritDoc} */
            @Override
            public String getValue() {
                return cipherValue;
            }

            /** {@inheritDoc} */
            @Override
            public void setValue(String value) {
                cipherValue = value;
            }

            Element toElement() {
                Element result =
                    XMLUtils.createElementInEncryptionSpace(
                        contextDocument, EncryptionConstants._TAG_CIPHERVALUE
                    );
                result.appendChild(contextDocument.createTextNode(cipherValue));

                return result;
            }
        }

        private class EncryptedDataImpl extends EncryptedTypeImpl implements EncryptedData {

            /**
             * @param data
             */
            public EncryptedDataImpl(CipherData data) {
                super(data);
            }

            Element toElement() {
                Element result =
                    ElementProxy.createElementForFamily(
                        contextDocument, EncryptionConstants.EncryptionSpecNS,
                        EncryptionConstants._TAG_ENCRYPTEDDATA
                    );

                if (null != super.getId()) {
                    result.setAttributeNS(null, EncryptionConstants._ATT_ID, super.getId());
                }
                if (null != super.getType()) {
                    result.setAttributeNS(null, EncryptionConstants._ATT_TYPE, super.getType());
                }
                if (null != super.getMimeType()) {
                    result.setAttributeNS(
                        null, EncryptionConstants._ATT_MIMETYPE, super.getMimeType()
                    );
                }
                if (null != super.getEncoding()) {
                    result.setAttributeNS(
                        null, EncryptionConstants._ATT_ENCODING, super.getEncoding()
                    );
                }
                if (null != super.getEncryptionMethod()) {
                    result.appendChild(
                        ((EncryptionMethodImpl)super.getEncryptionMethod()).toElement()
                    );
                }
                if (null != super.getKeyInfo()) {
                    result.appendChild(super.getKeyInfo().getElement().cloneNode(true));
                }

                result.appendChild(((CipherDataImpl) super.getCipherData()).toElement());
                if (null != super.getEncryptionProperties()) {
                    result.appendChild(((EncryptionPropertiesImpl)
                        super.getEncryptionProperties()).toElement());
                }

                return result;
            }
        }

        private class EncryptedKeyImpl extends EncryptedTypeImpl implements EncryptedKey {
            private String keyRecipient;
            private ReferenceList referenceList;
            private String carriedName;

            /**
             * @param data
             */
            public EncryptedKeyImpl(CipherData data) {
                super(data);
            }

            /** {@inheritDoc} */
            @Override
            public String getRecipient() {
                return keyRecipient;
            }

            /** {@inheritDoc} */
            @Override
            public void setRecipient(String recipient) {
                keyRecipient = recipient;
            }

            /** {@inheritDoc} */
            @Override
            public ReferenceList getReferenceList() {
                return referenceList;
            }

            /** {@inheritDoc} */
            @Override
            public void setReferenceList(ReferenceList list) {
                referenceList = list;
            }

            /** {@inheritDoc} */
            @Override
            public String getCarriedName() {
                return carriedName;
            }

            /** {@inheritDoc} */
            @Override
            public void setCarriedName(String name) {
                carriedName = name;
            }

            Element toElement() {
                Element result =
                    ElementProxy.createElementForFamily(
                        contextDocument, EncryptionConstants.EncryptionSpecNS,
                        EncryptionConstants._TAG_ENCRYPTEDKEY
                    );

                if (null != super.getId()) {
                    result.setAttributeNS(null, EncryptionConstants._ATT_ID, super.getId());
                }
                if (null != super.getType()) {
                    result.setAttributeNS(null, EncryptionConstants._ATT_TYPE, super.getType());
                }
                if (null != super.getMimeType()) {
                    result.setAttributeNS(
                        null, EncryptionConstants._ATT_MIMETYPE, super.getMimeType()
                    );
                }
                if (null != super.getEncoding()) {
                    result.setAttributeNS(null, Constants._ATT_ENCODING, super.getEncoding());
                }
                if (null != getRecipient()) {
                    result.setAttributeNS(
                        null, EncryptionConstants._ATT_RECIPIENT, getRecipient()
                    );
                }
                if (null != super.getEncryptionMethod()) {
                    result.appendChild(((EncryptionMethodImpl)
                        super.getEncryptionMethod()).toElement());
                }
                if (null != super.getKeyInfo()) {
                    result.appendChild(super.getKeyInfo().getElement().cloneNode(true));
                }
                result.appendChild(((CipherDataImpl) super.getCipherData()).toElement());
                if (null != super.getEncryptionProperties()) {
                    result.appendChild(((EncryptionPropertiesImpl)
                        super.getEncryptionProperties()).toElement());
                }
                if (referenceList != null && !referenceList.isEmpty()) {
                    result.appendChild(((ReferenceListImpl)getReferenceList()).toElement());
                }
                if (null != carriedName) {
                    Element element =
                        ElementProxy.createElementForFamily(
                            contextDocument,
                            EncryptionConstants.EncryptionSpecNS,
                            EncryptionConstants._TAG_CARRIEDKEYNAME
                        );
                    Node node = contextDocument.createTextNode(carriedName);
                    element.appendChild(node);
                    result.appendChild(element);
                }

                return result;
            }
        }

        private abstract class EncryptedTypeImpl {
            private String id;
            private String type;
            private String mimeType;
            private String encoding;
            private EncryptionMethod encryptionMethod;
            private KeyInfo keyInfo;
            private CipherData cipherData;
            private EncryptionProperties encryptionProperties;

            /**
             * Constructor.
             * @param data
             */
            protected EncryptedTypeImpl(CipherData data) {
                cipherData = data;
            }

            /**
             *
             * @return the Id
             */
            public String getId() {
                return id;
            }

            /**
             *
             * @param id
             */
            public void setId(String id) {
                this.id = id;
            }

            /**
             *
             * @return the type
             */
            public String getType() {
                return type;
            }

            /**
             *
             * @param type
             */
            public void setType(String type) {
                if (type == null || type.length() == 0) {
                    this.type = null;
                } else {
                    URI tmpType = null;
                    try {
                        tmpType = new URI(type);
                    } catch (URISyntaxException ex) {
                        throw (IllegalArgumentException)
                                new IllegalArgumentException().initCause(ex);
                    }
                    this.type = tmpType.toString();
                }
            }

            /**
             *
             * @return the MimeType
             */
            public String getMimeType() {
                return mimeType;
            }
            /**
             *
             * @param type
             */
            public void setMimeType(String type) {
                mimeType = type;
            }

            /**
             *
             * @return the encoding
             */
            public String getEncoding() {
                return encoding;
            }

            /**
             *
             * @param encoding
             */
            public void setEncoding(String encoding) {
                if (encoding == null || encoding.length() == 0) {
                    this.encoding = null;
                } else {
                    URI tmpEncoding = null;
                    try {
                        tmpEncoding = new URI(encoding);
                    } catch (URISyntaxException ex) {
                        throw (IllegalArgumentException)
                                new IllegalArgumentException().initCause(ex);
                    }
                    this.encoding = tmpEncoding.toString();
                }
            }

            /**
             *
             * @return the EncryptionMethod
             */
            public EncryptionMethod getEncryptionMethod() {
                return encryptionMethod;
            }

            /**
             *
             * @param method
             */
            public void setEncryptionMethod(EncryptionMethod method) {
                encryptionMethod = method;
            }

            /**
             *
             * @return the KeyInfo
             */
            public KeyInfo getKeyInfo() {
                return keyInfo;
            }

            /**
             *
             * @param info
             */
            public void setKeyInfo(KeyInfo info) {
                keyInfo = info;
            }

            /**
             *
             * @return the CipherData
             */
            public CipherData getCipherData() {
                return cipherData;
            }

            /**
             *
             * @return the EncryptionProperties
             */
            public EncryptionProperties getEncryptionProperties() {
                return encryptionProperties;
            }

            /**
             *
             * @param properties
             */
            public void setEncryptionProperties(EncryptionProperties properties) {
                encryptionProperties = properties;
            }
        }

        private class EncryptionMethodImpl implements EncryptionMethod {
            private String algorithm;
            private int keySize = Integer.MIN_VALUE;
            private byte[] oaepParams;
            private List<Element> encryptionMethodInformation;
            private String digestAlgorithm;
            private String mgfAlgorithm;

            /**
             * Constructor.
             * @param algorithm
             */
            public EncryptionMethodImpl(String algorithm) {
                URI tmpAlgorithm = null;
                try {
                    tmpAlgorithm = new URI(algorithm);
                } catch (URISyntaxException ex) {
                    throw (IllegalArgumentException)
                            new IllegalArgumentException().initCause(ex);
                }
                this.algorithm = tmpAlgorithm.toString();
                encryptionMethodInformation = new LinkedList<>();
            }

            /** {@inheritDoc} */
            @Override
            public String getAlgorithm() {
                return algorithm;
            }

            /** {@inheritDoc} */
            @Override
            public int getKeySize() {
                return keySize;
            }

            /** {@inheritDoc} */
            @Override
            public void setKeySize(int size) {
                keySize = size;
            }

            /** {@inheritDoc} */
            @Override
            public byte[] getOAEPparams() {
                return oaepParams;
            }

            /** {@inheritDoc} */
            @Override
            public void setOAEPparams(byte[] params) {
                oaepParams = params;
            }

            /** {@inheritDoc} */
            @Override
            public void setDigestAlgorithm(String digestAlgorithm) {
                this.digestAlgorithm = digestAlgorithm;
            }

            /** {@inheritDoc} */
            @Override
            public String getDigestAlgorithm() {
                return digestAlgorithm;
            }

            /** {@inheritDoc} */
            @Override
            public void setMGFAlgorithm(String mgfAlgorithm) {
                this.mgfAlgorithm = mgfAlgorithm;
            }

            /** {@inheritDoc} */
            @Override
            public String getMGFAlgorithm() {
                return mgfAlgorithm;
            }

            /** {@inheritDoc} */
            @Override
            public Iterator<Element> getEncryptionMethodInformation() {
                return encryptionMethodInformation.iterator();
            }

            /** {@inheritDoc} */
            @Override
            public void addEncryptionMethodInformation(Element info) {
                encryptionMethodInformation.add(info);
            }

            /** {@inheritDoc} */
            @Override
            public void removeEncryptionMethodInformation(Element info) {
                encryptionMethodInformation.remove(info);
            }

            Element toElement() {
                Element result =
                    XMLUtils.createElementInEncryptionSpace(
                        contextDocument, EncryptionConstants._TAG_ENCRYPTIONMETHOD
                    );
                result.setAttributeNS(null, EncryptionConstants._ATT_ALGORITHM, algorithm);
                if (keySize > 0) {
                    result.appendChild(
                        XMLUtils.createElementInEncryptionSpace(
                            contextDocument, EncryptionConstants._TAG_KEYSIZE
                    ).appendChild(contextDocument.createTextNode(String.valueOf(keySize))));
                }
                if (null != oaepParams) {
                    Element oaepElement =
                        XMLUtils.createElementInEncryptionSpace(
                            contextDocument, EncryptionConstants._TAG_OAEPPARAMS
                        );
                    oaepElement.appendChild(contextDocument.createTextNode(
                        XMLUtils.encodeToString(oaepParams)));
                    result.appendChild(oaepElement);
                }
                if (digestAlgorithm != null) {
                    Element digestElement =
                        XMLUtils.createElementInSignatureSpace(contextDocument, Constants._TAG_DIGESTMETHOD);
                    digestElement.setAttributeNS(null, "Algorithm", digestAlgorithm);
                    digestElement.setAttributeNS(
                        Constants.NamespaceSpecNS,
                        "xmlns:" + ElementProxy.getDefaultPrefix(Constants.SignatureSpecNS),
                        Constants.SignatureSpecNS
                    );
                    result.appendChild(digestElement);
                }
                if (mgfAlgorithm != null) {
                    Element mgfElement =
                        XMLUtils.createElementInEncryption11Space(
                            contextDocument, EncryptionConstants._TAG_MGF
                        );
                    mgfElement.setAttributeNS(null, "Algorithm", mgfAlgorithm);
                    mgfElement.setAttributeNS(
                        Constants.NamespaceSpecNS,
                        "xmlns:" + ElementProxy.getDefaultPrefix(EncryptionConstants.EncryptionSpec11NS),
                        EncryptionConstants.EncryptionSpec11NS
                    );
                    result.appendChild(mgfElement);
                }
                Iterator<Element> itr = encryptionMethodInformation.iterator();
                while (itr.hasNext()) {
                    result.appendChild(itr.next());
                }

                return result;
            }
        }

        private class EncryptionPropertiesImpl implements EncryptionProperties {
            private String id;
            private List<EncryptionProperty> encryptionProperties;

            /**
             * Constructor.
             */
            public EncryptionPropertiesImpl() {
                encryptionProperties = new LinkedList<>();
            }

            /** {@inheritDoc} */
            @Override
            public String getId() {
                return id;
            }

            /** {@inheritDoc} */
            @Override
            public void setId(String id) {
                this.id = id;
            }

            /** {@inheritDoc} */
            @Override
            public Iterator<EncryptionProperty> getEncryptionProperties() {
                return encryptionProperties.iterator();
            }

            /** {@inheritDoc} */
            @Override
            public void addEncryptionProperty(EncryptionProperty property) {
                encryptionProperties.add(property);
            }

            /** {@inheritDoc} */
            @Override
            public void removeEncryptionProperty(EncryptionProperty property) {
                encryptionProperties.remove(property);
            }

            Element toElement() {
                Element result =
                    XMLUtils.createElementInEncryptionSpace(
                        contextDocument, EncryptionConstants._TAG_ENCRYPTIONPROPERTIES
                    );
                if (null != id) {
                    result.setAttributeNS(null, EncryptionConstants._ATT_ID, id);
                }
                Iterator<EncryptionProperty> itr = getEncryptionProperties();
                while (itr.hasNext()) {
                    result.appendChild(((EncryptionPropertyImpl)itr.next()).toElement());
                }

                return result;
            }
        }

        private class EncryptionPropertyImpl implements EncryptionProperty {
            private String target;
            private String id;
            private Map<String, String> attributeMap = new HashMap<>();
            private List<Element> encryptionInformation;

            /**
             * Constructor.
             */
            public EncryptionPropertyImpl() {
                encryptionInformation = new LinkedList<>();
            }

            /** {@inheritDoc} */
            @Override
            public String getTarget() {
                return target;
            }

            /** {@inheritDoc} */
            @Override
            public void setTarget(String target) {
                if (target == null || target.length() == 0) {
                    this.target = null;
                } else if (target.charAt(0) == '#') {
                    /*
                     * This is a same document URI reference. Do not parse,
                     * because it has no scheme.
                     */
                    this.target = target;
                } else {
                    URI tmpTarget = null;
                    try {
                        tmpTarget = new URI(target);
                    } catch (URISyntaxException ex) {
                        throw (IllegalArgumentException)
                                new IllegalArgumentException().initCause(ex);
                    }
                    this.target = tmpTarget.toString();
                }
            }

            /** {@inheritDoc} */
            @Override
            public String getId() {
                return id;
            }

            /** {@inheritDoc} */
            @Override
            public void setId(String id) {
                this.id = id;
            }

            /** {@inheritDoc} */
            @Override
            public String getAttribute(String attribute) {
                return attributeMap.get(attribute);
            }

            /** {@inheritDoc} */
            @Override
            public void setAttribute(String attribute, String value) {
                attributeMap.put(attribute, value);
            }

            /** {@inheritDoc} */
            @Override
            public Iterator<Element> getEncryptionInformation() {
                return encryptionInformation.iterator();
            }

            /** {@inheritDoc} */
            @Override
            public void addEncryptionInformation(Element info) {
                encryptionInformation.add(info);
            }

            /** {@inheritDoc} */
            @Override
            public void removeEncryptionInformation(Element info) {
                encryptionInformation.remove(info);
            }

            Element toElement() {
                Element result =
                    XMLUtils.createElementInEncryptionSpace(
                        contextDocument, EncryptionConstants._TAG_ENCRYPTIONPROPERTY
                    );
                if (null != target) {
                    result.setAttributeNS(null, EncryptionConstants._ATT_TARGET, target);
                }
                if (null != id) {
                    result.setAttributeNS(null, EncryptionConstants._ATT_ID, id);
                }

                if (!attributeMap.isEmpty()) {
                    for (Entry<String, String> entry : attributeMap.entrySet()) {
                        result.setAttributeNS(Constants.XML_LANG_SPACE_SpecNS,
                                              entry.getKey(), entry.getValue());
                    }
                }

                if (!encryptionInformation.isEmpty()) {
                    for (Element element : encryptionInformation) {
                        result.appendChild(element);
                    }
                }

                return result;
            }
        }

        private class TransformsImpl extends org.apache.xml.security.transforms.Transforms
            implements Transforms {

            /**
             * Construct Transforms
             */
            public TransformsImpl() {
                super(contextDocument);
            }

            /**
             *
             * @param doc
             */
            public TransformsImpl(Document doc) {
                if (doc == null) {
                    throw new RuntimeException("Document is null");
                }

                setDocument(doc);
                setElement(createElementForFamilyLocal(
                        this.getBaseNamespace(), this.getBaseLocalName()
                    )
                );
            }

            /**
             *
             * @param element
             * @throws XMLSignatureException
             * @throws InvalidTransformException
             * @throws XMLSecurityException
             * @throws TransformationException
             */
            public TransformsImpl(Element element)
                throws XMLSignatureException, InvalidTransformException,
                    XMLSecurityException, TransformationException {
                super(element, "");
            }

            /**
             *
             * @return the XML Element form of that Transforms
             */
            public Element toElement() {
                if (getDocument() == null) {
                    setDocument(contextDocument);
                }

                return getElement();
            }

            /** {@inheritDoc} */
            @Override
            public org.apache.xml.security.transforms.Transforms getDSTransforms() {
                return this;
            }

            // Over-ride the namespace
            /** {@inheritDoc} */
            @Override
            public String getBaseNamespace() {
                return EncryptionConstants.EncryptionSpecNS;
            }
        }

        private class ReferenceListImpl implements ReferenceList {
            private Class<?> sentry;
            private List<Reference> references;

            /**
             * Constructor.
             * @param type
             */
            public ReferenceListImpl(int type) {
                if (type == ReferenceList.DATA_REFERENCE) {
                    sentry = DataReference.class;
                } else if (type == ReferenceList.KEY_REFERENCE) {
                    sentry = KeyReference.class;
                } else {
                    throw new IllegalArgumentException();
                }
                references = new LinkedList<>();
            }

            /** {@inheritDoc} */
            @Override
            public void add(Reference reference) {
                if (!reference.getClass().equals(sentry)) {
                    throw new IllegalArgumentException();
                }
                references.add(reference);
            }

            /** {@inheritDoc} */
            @Override
            public void remove(Reference reference) {
                if (!reference.getClass().equals(sentry)) {
                    throw new IllegalArgumentException();
                }
                references.remove(reference);
            }

            /** {@inheritDoc} */
            @Override
            public int size() {
                return references.size();
            }

            /** {@inheritDoc} */
            @Override
            public boolean isEmpty() {
                return references.isEmpty();
            }

            /** {@inheritDoc} */
            @Override
            public Iterator<Reference> getReferences() {
                return references.iterator();
            }

            Element toElement() {
                Element result =
                    ElementProxy.createElementForFamily(
                        contextDocument,
                        EncryptionConstants.EncryptionSpecNS,
                        EncryptionConstants._TAG_REFERENCELIST
                    );
                Iterator<Reference> eachReference = references.iterator();
                while (eachReference.hasNext()) {
                    Reference reference = eachReference.next();
                    result.appendChild(((ReferenceImpl) reference).toElement());
                }
                return result;
            }

            /** {@inheritDoc} */
            @Override
            public Reference newDataReference(String uri) {
                return new DataReference(uri);
            }

            /** {@inheritDoc} */
            @Override
            public Reference newKeyReference(String uri) {
                return new KeyReference(uri);
            }

            /**
             * <code>ReferenceImpl</code> is an implementation of
             * <code>Reference</code>.
             *
             * @see Reference
             */
            private abstract class ReferenceImpl implements Reference {
                private String uri;
                private List<Element> referenceInformation;

                ReferenceImpl(String uri) {
                    this.uri = uri;
                    referenceInformation = new LinkedList<>();
                }

                /** {@inheritDoc} */
                @Override
                public abstract String getType();

                /** {@inheritDoc} */
                @Override
                public String getURI() {
                    return uri;
                }

                /** {@inheritDoc} */
                @Override
                public Iterator<Element> getElementRetrievalInformation() {
                    return referenceInformation.iterator();
                }

                /** {@inheritDoc} */
                @Override
                public void setURI(String uri) {
                    this.uri = uri;
                }

                /** {@inheritDoc} */
                @Override
                public void removeElementRetrievalInformation(Element node) {
                    referenceInformation.remove(node);
                }

                /** {@inheritDoc} */
                @Override
                public void addElementRetrievalInformation(Element node) {
                    referenceInformation.add(node);
                }

                /**
                 * @return the XML Element form of that Reference
                 */
                public Element toElement() {
                    String tagName = getType();
                    Element result =
                        ElementProxy.createElementForFamily(
                            contextDocument,
                            EncryptionConstants.EncryptionSpecNS,
                            tagName
                        );
                    result.setAttributeNS(null, EncryptionConstants._ATT_URI, uri);

                    // TODO: Need to martial referenceInformation
                    // Figure out how to make this work..
                    // <any namespace="##other" minOccurs="0" maxOccurs="unbounded"/>

                    return result;
                }
            }

            private class DataReference extends ReferenceImpl {

                DataReference(String uri) {
                    super(uri);
                }

                /** {@inheritDoc} */
                @Override
                public String getType() {
                    return EncryptionConstants._TAG_DATAREFERENCE;
                }
            }

            private class KeyReference extends ReferenceImpl {

                KeyReference(String uri) {
                    super(uri);
                }

                /** {@inheritDoc} */
                @Override
                public String getType() {
                    return EncryptionConstants._TAG_KEYREFERENCE;
                }
            }
        }
    }

    private static boolean haveFunctionalIdentityTransformer() {
        final String xml =
                "<a:e1 xmlns:a=\"a\" xmlns:b=\"b\">"
                        + "<a xmlns=\"a\" xmlns:b=\"b\"/>"
                        + "</a:e1>";

        try {
            final javax.xml.transform.dom.DOMResult domResult = new javax.xml.transform.dom.DOMResult();
            final javax.xml.transform.TransformerFactory transformerFactory =
                    javax.xml.transform.TransformerFactory.newInstance();
            transformerFactory.newTransformer().transform(
                    new javax.xml.transform.stream.StreamSource(
                            new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))), domResult);

            boolean result = false;
            if (domResult.getNode().getFirstChild().getFirstChild().hasAttributes()
                && domResult.getNode().getFirstChild().getFirstChild().getAttributes().getLength() >= 1) {
                result = "http://www.w3.org/2000/xmlns/".equals(
                    domResult.getNode().getFirstChild().getFirstChild().getAttributes().item(1).getNamespaceURI());
            }
            LOG.debug("Have functional IdentityTransformer: {}", result);
            return result;

        } catch (Exception e) {
            LOG.debug(e.getMessage(), e);
            return false;
        }
    }
}
