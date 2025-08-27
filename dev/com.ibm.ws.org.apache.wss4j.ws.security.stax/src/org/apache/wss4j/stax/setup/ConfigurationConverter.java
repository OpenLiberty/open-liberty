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
package org.apache.wss4j.stax.setup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;

import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.JasyptPasswordEncryptor;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.Loader;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSConstants.UsernameTokenPasswordType;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.wss4j.stax.validate.Validator;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.XMLSecurityConstants.Action;

/**
 * This utility class converts between a Map<String, Object> and a WSSSecurityProperties class
 */
public final class ConfigurationConverter {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(ConfigurationConverter.class);

    private ConfigurationConverter() {
        // complete
    }

    public static WSSSecurityProperties convert(Map<String, Object> config) {
        WSSSecurityProperties properties = new WSSSecurityProperties();

        if (config == null) {
            return properties;
        }

        parseActions(config, properties);
        parseUserProperties(config, properties);
        parseCallback(config, properties);
        parseCrypto(config, properties);
        parseBooleanProperties(config, properties);
        parseNonBooleanProperties(config, properties);

        return properties;
    }

    public static void parseActions(
        Map<String, Object> config,
        WSSSecurityProperties properties
    ) {
        String action = getString(ConfigurationConstants.ACTION, config);

        String actionToParse = action;
        if (actionToParse == null) {
            return;
        }
        actionToParse = actionToParse.trim();
        if (actionToParse.length() == 0) {
            return;
        }
        String[] single = actionToParse.split("\\s");
        List<Action> actions = new ArrayList<>();
        for (int i = 0; i < single.length; i++) {
            if (single[i].equals(ConfigurationConstants.USERNAME_TOKEN)) {
                actions.add(WSSConstants.USERNAMETOKEN);
            } else if (single[i].equals(ConfigurationConstants.SIGNATURE)) {
                actions.add(WSSConstants.SIGNATURE);
            } else if (single[i].equals(ConfigurationConstants.ENCRYPT)
                || single[i].equals(ConfigurationConstants.ENCRYPTION)) {
                actions.add(WSSConstants.ENCRYPTION);
            } else if (single[i].equals(ConfigurationConstants.SAML_TOKEN_UNSIGNED)) {
                actions.add(WSSConstants.SAML_TOKEN_UNSIGNED);
            } else if (single[i].equals(ConfigurationConstants.SAML_TOKEN_SIGNED)) {
                actions.add(WSSConstants.SAML_TOKEN_SIGNED);
            } else if (single[i].equals(ConfigurationConstants.TIMESTAMP)) {
                actions.add(WSSConstants.TIMESTAMP);
            } else if (single[i].equals(ConfigurationConstants.USERNAME_TOKEN_SIGNATURE)) {
                actions.add(WSSConstants.USERNAMETOKEN_SIGNED);
            } else if (single[i].equals(ConfigurationConstants.SIGNATURE_DERIVED)) {
                actions.add(WSSConstants.SIGNATURE_WITH_DERIVED_KEY);
            } else if (single[i].equals(ConfigurationConstants.ENCRYPT_DERIVED)
                || single[i].equals(ConfigurationConstants.ENCRYPTION_DERIVED)) {
                actions.add(WSSConstants.ENCRYPTION_WITH_DERIVED_KEY);
            } else if (single[i].equals(ConfigurationConstants.SIGNATURE_WITH_KERBEROS_TOKEN)) {
                actions.add(WSSConstants.SIGNATURE_WITH_KERBEROS_TOKEN);
            } else if (single[i].equals(ConfigurationConstants.ENCRYPT_WITH_KERBEROS_TOKEN)
                || single[i].equals(ConfigurationConstants.ENCRYPTION_WITH_KERBEROS_TOKEN)) {
                actions.add(WSSConstants.ENCRYPTION_WITH_KERBEROS_TOKEN);
            } else if (single[i].equals(ConfigurationConstants.KERBEROS_TOKEN)) {
                actions.add(WSSConstants.KERBEROS_TOKEN);
            } else if (single[i].equals(ConfigurationConstants.CUSTOM_TOKEN)) {
                actions.add(WSSConstants.CUSTOM_TOKEN);
            } /* else if (single[i].equals(ConfigurationConstants.USERNAME_TOKEN_NO_PASSWORD)) {
                actions.add(WSConstants.UT_NOPASSWORD);
            } */
        }

        boolean sigConf =
                decodeBooleanConfigValue(ConfigurationConstants.ENABLE_SIGNATURE_CONFIRMATION, false, config);
        if (sigConf) {
            actions.add(WSSConstants.SIGNATURE_CONFIRMATION);
        }

        properties.setActions(actions);
    }

    public static void parseUserProperties(
        Map<String, Object> config,
        WSSSecurityProperties properties
    ) {
        String user = getString(ConfigurationConstants.USER, config);
        properties.setTokenUser(user);

        String actor = getString(ConfigurationConstants.ACTOR, config);
        properties.setActor(actor);

        String encUser = getString(ConfigurationConstants.ENCRYPTION_USER, config);
        if (encUser == null) {
            encUser = user;
        }
        properties.setEncryptionUser(encUser);
        if (ConfigurationConstants.USE_REQ_SIG_CERT.equals(encUser)) {
            properties.setUseReqSigCertForEncryption(true);
        }

        String sigUser = getString(ConfigurationConstants.SIGNATURE_USER, config);
        if (sigUser == null) {
            sigUser = user;
        }
        properties.setSignatureUser(sigUser);
    }

    public static void parseCrypto(
        Map<String, Object> config,
        WSSSecurityProperties properties
    ) {
        Object passwordEncryptorObj =
            config.get(ConfigurationConstants.PASSWORD_ENCRYPTOR_INSTANCE);
        PasswordEncryptor passwordEncryptor = null;
        if (passwordEncryptorObj instanceof PasswordEncryptor) {
            passwordEncryptor = (PasswordEncryptor)passwordEncryptorObj;
        }
        if (passwordEncryptor == null) {
            CallbackHandler callbackHandler = properties.getCallbackHandler();
            if (callbackHandler != null) {
                passwordEncryptor = new JasyptPasswordEncryptor(callbackHandler);
            }
        }

        String sigPropRef = getString(ConfigurationConstants.SIG_PROP_REF_ID, config);
        boolean foundSigRef = false;
        if (sigPropRef != null) {
            Object sigRef = config.get(sigPropRef);
            if (sigRef instanceof Crypto) {
                foundSigRef = true;
                properties.setSignatureCrypto((Crypto)sigRef);
            } else if (sigRef instanceof Properties) {
                foundSigRef = true;
                properties.setSignatureCryptoProperties((Properties)sigRef, passwordEncryptor);
            }
            if (foundSigRef && properties.getSignatureUser() == null) {
                properties.setSignatureUser(getDefaultX509Identifier(properties, true));
            }
        }

        if (!foundSigRef) {
            String sigPropFile = getString(ConfigurationConstants.SIG_PROP_FILE, config);
            if (sigPropFile != null) {
                try {
                    Properties sigProperties =
                        CryptoFactory.getProperties(sigPropFile, getClassLoader());
                    properties.setSignatureCryptoProperties(sigProperties, passwordEncryptor);
                    if (properties.getSignatureUser() == null) {
                        properties.setSignatureUser(getDefaultX509Identifier(properties, true));
                    }
                } catch (WSSecurityException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }

        String sigVerPropRef = getString(ConfigurationConstants.SIG_VER_PROP_REF_ID, config);
        boolean foundSigVerRef = false;
        if (sigVerPropRef != null) {
            Object sigVerRef = config.get(sigVerPropRef);
            if (sigVerRef instanceof Crypto) {
                foundSigVerRef = true;
                properties.setSignatureVerificationCrypto((Crypto)sigVerRef);
            } else if (sigVerRef instanceof Properties) {
                foundSigVerRef = true;
                properties.setSignatureVerificationCryptoProperties((Properties)sigVerRef, passwordEncryptor);
            }
        }

        if (!foundSigVerRef) {
            String sigPropFile = getString(ConfigurationConstants.SIG_VER_PROP_FILE, config);
            if (sigPropFile != null) {
                try {
                    Properties sigProperties =
                        CryptoFactory.getProperties(sigPropFile, getClassLoader());
                    properties.setSignatureVerificationCryptoProperties(sigProperties, passwordEncryptor);
                } catch (WSSecurityException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }

        String encPropRef = getString(ConfigurationConstants.ENC_PROP_REF_ID, config);
        boolean foundEncRef = false;
        if (encPropRef != null) {
            Object encRef = config.get(encPropRef);
            if (encRef instanceof Crypto) {
                foundEncRef = true;
                properties.setEncryptionCrypto((Crypto)encRef);
            } else if (encRef instanceof Properties) {
                foundEncRef = true;
                properties.setEncryptionCryptoProperties((Properties)encRef, passwordEncryptor);
            }
        }

        if (!foundEncRef) {
            String encPropFile = getString(ConfigurationConstants.ENC_PROP_FILE, config);
            if (encPropFile != null) {
                try {
                    Properties encProperties =
                        CryptoFactory.getProperties(encPropFile, getClassLoader());
                    properties.setEncryptionCryptoProperties(encProperties, passwordEncryptor);
                } catch (WSSecurityException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }

        String decPropRef = getString(ConfigurationConstants.DEC_PROP_REF_ID, config);
        boolean foundDecRef = false;
        if (decPropRef != null) {
            Object decRef = config.get(decPropRef);
            if (decRef instanceof Crypto) {
                foundDecRef = true;
                properties.setDecryptionCrypto((Crypto)decRef);
            } else if (decRef instanceof Properties) {
                foundDecRef = true;
                properties.setDecryptionCryptoProperties((Properties)decRef, passwordEncryptor);
            }
        }

        if (!foundDecRef) {
            String encPropFile = getString(ConfigurationConstants.DEC_PROP_FILE, config);
            if (encPropFile != null) {
                try {
                    Properties encProperties =
                        CryptoFactory.getProperties(encPropFile, getClassLoader());
                    properties.setDecryptionCryptoProperties(encProperties, passwordEncryptor);
                } catch (WSSecurityException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    public static String getDefaultX509Identifier(
        WSSSecurityProperties properties, boolean signature
    ) {
        try {
            Crypto crypto = null;
            if (signature) {
                crypto = properties.getSignatureCrypto();
            } else {
                crypto = properties.getEncryptionCrypto();
            }
            if (crypto != null) {
                return crypto.getDefaultX509Identifier();
            }
        } catch (WSSecurityException e) {
            LOG.debug(e.getMessage(), e);
        }
        return null;
    }

    public static void parseCallback(
        Map<String, Object> config,
        WSSSecurityProperties properties
    ) {
        Object pwPropRef = config.get(ConfigurationConstants.PW_CALLBACK_REF);
        if (pwPropRef instanceof CallbackHandler) {
            properties.setCallbackHandler((CallbackHandler)pwPropRef);
        } else {
            String pwCallback = getString(ConfigurationConstants.PW_CALLBACK_CLASS, config);
            if (pwCallback != null) {
                try {
                    CallbackHandler pwCallbackHandler = loadCallbackHandler(pwCallback);
                    properties.setCallbackHandler(pwCallbackHandler);
                } catch (WSSecurityException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }

        Object samlPropRef = config.get(ConfigurationConstants.SAML_CALLBACK_REF);
        if (samlPropRef instanceof CallbackHandler) {
            properties.setSamlCallbackHandler((CallbackHandler)samlPropRef);
        } else {
            String samlCallback = getString(ConfigurationConstants.SAML_CALLBACK_CLASS, config);
            if (samlCallback != null) {
                try {
                    CallbackHandler samlCallbackHandler = loadCallbackHandler(samlCallback);
                    properties.setSamlCallbackHandler(samlCallbackHandler);
                } catch (WSSecurityException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Load a CallbackHandler instance.
     * @param callbackHandlerClass The class name of the CallbackHandler instance
     * @return a CallbackHandler instance
     * @throws WSSecurityException
     */
    public static CallbackHandler loadCallbackHandler(
        String callbackHandlerClass
    ) throws WSSecurityException {

        Class<? extends CallbackHandler> cbClass = null;
        CallbackHandler cbHandler = null;
        try {
            cbClass =
                Loader.loadClass(getClassLoader(),
                                 callbackHandlerClass,
                                 CallbackHandler.class);
        } catch (ClassNotFoundException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e,
                    "empty",
                    new Object[] {"WSHandler: cannot load callback handler class: "
                    + callbackHandlerClass}
            );
        }
        try {
            cbHandler = cbClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e,
                    "empty",
                    new Object[] {"WSHandler: cannot create instance of callback handler: "
                    + callbackHandlerClass}
            );
        }
        return cbHandler;
    }

    private static ClassLoader getClassLoader() {
        try {
            return Loader.getTCL();
        } catch (Exception ex) {
            return null;
        }
    }

    public static void parseBooleanProperties(
        Map<String, Object> config,
        WSSSecurityProperties properties
    ) {
        //outbound sigConf is configured as an Action, see parseActions()
        boolean sigConf =
            decodeBooleanConfigValue(ConfigurationConstants.ENABLE_SIGNATURE_CONFIRMATION, false, config);
        properties.setEnableSignatureConfirmationVerification(sigConf);

        boolean mustUnderstand =
            decodeBooleanConfigValue(ConfigurationConstants.MUST_UNDERSTAND, true, config);
        properties.setMustUnderstand(mustUnderstand);

        boolean bspCompliant =
            decodeBooleanConfigValue(ConfigurationConstants.IS_BSP_COMPLIANT, true, config);
        properties.setDisableBSPEnforcement(!bspCompliant);

        boolean inclPrefixes =
            decodeBooleanConfigValue(ConfigurationConstants.ADD_INCLUSIVE_PREFIXES, true, config);
        properties.setAddExcC14NInclusivePrefixes(inclPrefixes);

        boolean nonce =
            decodeBooleanConfigValue(ConfigurationConstants.ADD_USERNAMETOKEN_NONCE, false, config);
        properties.setAddUsernameTokenNonce(nonce);

        boolean created =
            decodeBooleanConfigValue(ConfigurationConstants.ADD_USERNAMETOKEN_CREATED, false, config);
        properties.setAddUsernameTokenCreated(created);

        boolean customPasswordTypes =
            decodeBooleanConfigValue(ConfigurationConstants.HANDLE_CUSTOM_PASSWORD_TYPES, false, config);
        properties.setHandleCustomPasswordTypes(customPasswordTypes);

        boolean allowNoPassword =
            decodeBooleanConfigValue(ConfigurationConstants.ALLOW_USERNAMETOKEN_NOPASSWORD, false, config);
        properties.setAllowUsernameTokenNoPassword(allowNoPassword);

        boolean enableRevocation =
            decodeBooleanConfigValue(ConfigurationConstants.ENABLE_REVOCATION, false, config);
        properties.setEnableRevocation(enableRevocation);

        boolean singleCert =
            decodeBooleanConfigValue(ConfigurationConstants.USE_SINGLE_CERTIFICATE, true, config);
        properties.setUseSingleCert(singleCert);

        boolean derivedKeyMAC =
            decodeBooleanConfigValue(ConfigurationConstants.USE_DERIVED_KEY_FOR_MAC, true, config);
        properties.setUseDerivedKeyForMAC(derivedKeyMAC);

        boolean timestampStrict =
            decodeBooleanConfigValue(ConfigurationConstants.TIMESTAMP_STRICT, true, config);
        properties.setStrictTimestampCheck(timestampStrict);

        boolean allowRSA15 =
            decodeBooleanConfigValue(ConfigurationConstants.ALLOW_RSA15_KEY_TRANSPORT_ALGORITHM, false, config);
        properties.setAllowRSA15KeyTransportAlgorithm(allowRSA15);

        boolean validateSamlSubjectConf =
            decodeBooleanConfigValue(ConfigurationConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, true, config);
        properties.setValidateSamlSubjectConfirmation(validateSamlSubjectConf);

        boolean includeSignatureToken =
            decodeBooleanConfigValue(ConfigurationConstants.INCLUDE_SIGNATURE_TOKEN, false, config);
        properties.setIncludeSignatureToken(includeSignatureToken);

        boolean includeEncryptionToken =
            decodeBooleanConfigValue(ConfigurationConstants.INCLUDE_ENCRYPTION_TOKEN, false, config);
        properties.setIncludeEncryptionToken(includeEncryptionToken);

        boolean encryptSymmetricEncryptionKey =
            decodeBooleanConfigValue(ConfigurationConstants.ENC_SYM_ENC_KEY, true, config);
        properties.setEncryptSymmetricEncryptionKey(encryptSymmetricEncryptionKey);

        boolean use200512Namespace =
            decodeBooleanConfigValue(ConfigurationConstants.USE_2005_12_NAMESPACE, true, config);
        properties.setUse200512Namespace(use200512Namespace);

        boolean requireTimestampExpires =
            decodeBooleanConfigValue(ConfigurationConstants.REQUIRE_TIMESTAMP_EXPIRES, false, config);
        properties.setRequireTimestampExpires(requireTimestampExpires);
    }

    public static void parseNonBooleanProperties(
        Map<String, Object> config,
        WSSSecurityProperties properties
    ) {
        String pwType = getString(ConfigurationConstants.PASSWORD_TYPE, config);
        if ("PasswordDigest".equals(pwType)) {
            properties.setUsernameTokenPasswordType(UsernameTokenPasswordType.PASSWORD_DIGEST);
        } else if ("PasswordText".equals(pwType)) {
            properties.setUsernameTokenPasswordType(UsernameTokenPasswordType.PASSWORD_TEXT);
        } else if ("PasswordNone".equals(pwType)) {
            properties.setUsernameTokenPasswordType(UsernameTokenPasswordType.PASSWORD_NONE);
        }

        String signatureKeyIdentifier = getString(ConfigurationConstants.SIG_KEY_ID, config);
        WSSecurityTokenConstants.KeyIdentifier convSigKeyIdentifier =
            convertKeyIdentifier(signatureKeyIdentifier);
        if (convSigKeyIdentifier != null) {
            properties.setSignatureKeyIdentifier(convSigKeyIdentifier);
        }

        String sigAlgo = getString(ConfigurationConstants.SIG_ALGO, config);
        properties.setSignatureAlgorithm(sigAlgo);

        String sigDigestAlgo = getString(ConfigurationConstants.SIG_DIGEST_ALGO, config);
        properties.setSignatureDigestAlgorithm(sigDigestAlgo);

        String sigC14nAlgo = getString(ConfigurationConstants.SIG_C14N_ALGO, config);
        properties.setSignatureCanonicalizationAlgorithm(sigC14nAlgo);

        Object sigParts = config.get(ConfigurationConstants.SIGNATURE_PARTS);
        configureParts(sigParts, properties, sigDigestAlgo, true, true);

        sigParts = config.get(ConfigurationConstants.OPTIONAL_SIGNATURE_PARTS);
        configureParts(sigParts, properties, sigDigestAlgo, false, true);

        String iterations = getString(ConfigurationConstants.DERIVED_KEY_ITERATIONS, config);
        if (iterations != null) {
            int iIterations = Integer.parseInt(iterations);
            properties.setDerivedKeyIterations(iIterations);
        }

        String encKeyIdentifier = getString(ConfigurationConstants.ENC_KEY_ID, config);
        WSSecurityTokenConstants.KeyIdentifier convEncKeyIdentifier =
            convertKeyIdentifier(encKeyIdentifier);
        if (convEncKeyIdentifier != null) {
            properties.setEncryptionKeyIdentifier(convEncKeyIdentifier);
        }

        Object encParts = config.get(ConfigurationConstants.ENCRYPTION_PARTS);
        configureParts(encParts, properties, null, true, false);

        encParts = config.get(ConfigurationConstants.OPTIONAL_ENCRYPTION_PARTS);
        configureParts(encParts, properties, null, false, false);

        String encSymcAlgo = getString(ConfigurationConstants.ENC_SYM_ALGO, config);
        properties.setEncryptionSymAlgorithm(encSymcAlgo);

        String encKeyTransport = getString(ConfigurationConstants.ENC_KEY_TRANSPORT, config);
        properties.setEncryptionKeyTransportAlgorithm(encKeyTransport);

        String encDigestAlgo = getString(ConfigurationConstants.ENC_DIGEST_ALGO, config);
        properties.setEncryptionKeyTransportDigestAlgorithm(encDigestAlgo);

        String encMGFAlgo = getString(ConfigurationConstants.ENC_MGF_ALGO, config);
        properties.setEncryptionKeyTransportMGFAlgorithm(encMGFAlgo);

        // Subject Cert Constraints
        String certConstraints =
            getString(ConfigurationConstants.SIG_SUBJECT_CERT_CONSTRAINTS, config);
        if (certConstraints != null) {
            String certConstraintsSeparator =
                getString(ConfigurationConstants.SIG_CERT_CONSTRAINTS_SEPARATOR, config);
            if (certConstraintsSeparator == null || certConstraintsSeparator.isEmpty()) {
                certConstraintsSeparator = ",";
            }
            Collection<Pattern> subjectCertConstraints =
                getCertConstraints(certConstraints, certConstraintsSeparator);
            properties.setSubjectCertConstraints(subjectCertConstraints);
        }
        // Subject Cert Constraints
        String issuerCertConstraintsString =
            getString(ConfigurationConstants.SIG_ISSUER_CERT_CONSTRAINTS, config);
        if (issuerCertConstraintsString != null) {
            String certConstraintsSeparator =
                getString(ConfigurationConstants.SIG_CERT_CONSTRAINTS_SEPARATOR, config);
            if (certConstraintsSeparator == null || certConstraintsSeparator.isEmpty()) {
                certConstraintsSeparator = ",";
            }
            Collection<Pattern> issuerCertConstraints =
                getCertConstraints(certConstraints, certConstraintsSeparator);
            properties.setIssuerDNConstraints(issuerCertConstraints);
        }

        properties.setUtTTL(decodeTimeToLive(config, false));
        properties.setUtFutureTTL(decodeFutureTimeToLive(config, false));
        properties.setTimestampTTL(decodeTimeToLive(config, true));
        properties.setTimeStampFutureTTL(decodeFutureTimeToLive(config, true));

        @SuppressWarnings("unchecked")
        final Map<QName, Validator> validatorMap =
            (Map<QName, Validator>)config.get(ConfigurationConstants.VALIDATOR_MAP);
        if (validatorMap != null) {
            for (Map.Entry<QName, Validator> entry : validatorMap.entrySet()) {
                properties.addValidator(entry.getKey(), entry.getValue());
            }
        }

        ReplayCache nonceCache =    //NOPMD
            (ReplayCache)config.get(ConfigurationConstants.NONCE_CACHE_INSTANCE);
        if (nonceCache != null) {
            properties.setNonceReplayCache(nonceCache);
        }

        ReplayCache timestampCache = //NOPMD
            (ReplayCache)config.get(ConfigurationConstants.TIMESTAMP_CACHE_INSTANCE);
        if (timestampCache != null) {
            properties.setTimestampReplayCache(timestampCache);
        }

        ReplayCache samlOneTimeUseCache = //NOPMD
            (ReplayCache)config.get(ConfigurationConstants.SAML_ONE_TIME_USE_CACHE_INSTANCE);
        if (samlOneTimeUseCache != null) {
            properties.setSamlOneTimeUseReplayCache(samlOneTimeUseCache);
        }

        String derivedSignatureKeyLength = getString(ConfigurationConstants.DERIVED_SIGNATURE_KEY_LENGTH, config);
        if (derivedSignatureKeyLength != null) {
            int sigLength = Integer.parseInt(derivedSignatureKeyLength);
            properties.setDerivedSignatureKeyLength(sigLength);
        }

        String derivedEncryptionKeyLength = getString(ConfigurationConstants.DERIVED_ENCRYPTION_KEY_LENGTH, config);
        if (derivedEncryptionKeyLength != null) {
            int encLength = Integer.parseInt(derivedEncryptionKeyLength);
            properties.setDerivedEncryptionKeyLength(encLength);
        }

        String derivedTokenReference = getString(ConfigurationConstants.DERIVED_TOKEN_REFERENCE, config);
        WSSConstants.DerivedKeyTokenReference convertedDerivedTokenReference =
            convertDerivedReference(derivedTokenReference);
        if (convertedDerivedTokenReference != null) {
            properties.setDerivedKeyTokenReference(convertedDerivedTokenReference);
        }

        String derivedKeyIdentifier = getString(ConfigurationConstants.DERIVED_TOKEN_KEY_ID, config);
        WSSecurityTokenConstants.KeyIdentifier convertedDerivedKeyIdentifier =
            convertKeyIdentifier(derivedKeyIdentifier);
        if (convertedDerivedKeyIdentifier != null) {
            properties.setDerivedKeyKeyIdentifier(convertedDerivedKeyIdentifier);
        }
    }

    private static Collection<Pattern> getCertConstraints(String certConstraints, String certConstraintsSeparator) {
        String[] certConstraintsList = certConstraints.split(certConstraintsSeparator);
        if (certConstraintsList != null && certConstraintsList.length > 0) {
            Collection<Pattern> certConstraintsCollection =
                new ArrayList<>(certConstraintsList.length);
            for (String certConstraint : certConstraintsList) {
                try {
                    certConstraintsCollection.add(Pattern.compile(certConstraint.trim()));
                } catch (PatternSyntaxException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }

            return certConstraintsCollection;
        }
        return Collections.emptyList();
    }

    private static void configureParts(Object secureParts, WSSSecurityProperties properties,
                                       String digestAlgo, boolean required, boolean signature) {
        if (secureParts != null) {
            if (secureParts instanceof String) {
                List<SecurePart> parts = new ArrayList<>();
                splitEncParts((String)secureParts, parts, WSSConstants.NS_SOAP11);
                for (SecurePart part : parts) {
                    part.setRequired(required);
                    if (signature) {
                        part.setDigestMethod(digestAlgo);
                        properties.addSignaturePart(part);
                    } else {
                        properties.addEncryptionPart(part);
                    }
                }
            } else if (secureParts instanceof List<?>) {
                List<?> sigPartsList = (List<?>)secureParts;
                for (Object obj : sigPartsList) {
                    if (obj instanceof SecurePart) {
                        SecurePart securePart = (SecurePart)obj;
                        securePart.setRequired(required);
                        if (signature) {
                            securePart.setDigestMethod(digestAlgo);
                            properties.addSignaturePart(securePart);
                        } else {
                            properties.addEncryptionPart(securePart);
                        }
                    }
                }
            }
        }
    }

    public static WSSConstants.DerivedKeyTokenReference convertDerivedReference(String derivedTokenReference) {
        if ("EncryptedKey".equals(derivedTokenReference)) {
           return WSSConstants.DerivedKeyTokenReference.EncryptedKey;
        } else if ("DirectReference".equals(derivedTokenReference)) {
            return WSSConstants.DerivedKeyTokenReference.DirectReference;
        } else if ("SecurityContextToken".equals(derivedTokenReference)) {
            return WSSConstants.DerivedKeyTokenReference.SecurityContextToken;
        }
        return null;
    }

    public static WSSecurityTokenConstants.KeyIdentifier convertKeyIdentifier(String keyIdentifier) {
        if ("IssuerSerial".equals(keyIdentifier)) {
           return WSSecurityTokenConstants.KeyIdentifier_IssuerSerial;
        } else if ("DirectReference".equals(keyIdentifier)) {
            return WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE;
        } else if ("X509KeyIdentifier".equals(keyIdentifier)) {
            return WSSecurityTokenConstants.KeyIdentifier_X509KeyIdentifier;
        } else if ("Thumbprint".equals(keyIdentifier)) {
            return WSSecurityTokenConstants.KEYIDENTIFIER_THUMBPRINT_IDENTIFIER;
        } else if ("SKIKeyIdentifier".equals(keyIdentifier)) {
            return WSSecurityTokenConstants.KeyIdentifier_SkiKeyIdentifier;
        } else if ("EncryptedKeySHA1".equals(keyIdentifier)) {
            return WSSecurityTokenConstants.KEYIDENTIFIER_ENCRYPTED_KEY_SHA1_IDENTIFIER;
        } else if ("EncryptedKey".equals(keyIdentifier)) {
            return WSSecurityTokenConstants.KeyIdentifier_EncryptedKey;
        } else if ("KeyValue".equals(keyIdentifier)) {
            return WSSecurityTokenConstants.KeyIdentifier_KeyValue;
        } else if ("KerberosSHA1".equals(keyIdentifier)) {
            return WSSecurityTokenConstants.KEYIDENTIFIER_KERBEROS_SHA1_IDENTIFIER;
        }
        return null;
    }

    private static int decodeTimeToLive(Map<String, Object> config, boolean timestamp) {
        String tag = ConfigurationConstants.TTL_TIMESTAMP;
        if (!timestamp) {
            tag = ConfigurationConstants.TTL_USERNAMETOKEN;
        }
        String ttl = getString(tag, config);
        int defaultTimeToLive = 300;
        if (ttl != null) {
            try {
                int ttlI = Integer.parseInt(ttl);
                if (ttlI < 0) {
                    return defaultTimeToLive;
                }
                return ttlI;
            } catch (NumberFormatException e) {
                return defaultTimeToLive;
            }
        }
        return defaultTimeToLive;
    }

    private static int decodeFutureTimeToLive(Map<String, Object> config, boolean timestamp) {
        String tag = ConfigurationConstants.TTL_FUTURE_TIMESTAMP;
        if (!timestamp) {
            tag = ConfigurationConstants.TTL_FUTURE_USERNAMETOKEN;
        }
        String ttl = getString(tag, config);
        int defaultFutureTimeToLive = 60;
        if (ttl != null) {
            try {
                int ttlI = Integer.parseInt(ttl);
                if (ttlI < 0) {
                    return defaultFutureTimeToLive;
                }
                return ttlI;
            } catch (NumberFormatException e) {
                return defaultFutureTimeToLive;
            }
        }
        return defaultFutureTimeToLive;
    }

    private static String getString(String tag, Map<String, Object> config) {
        Object value = config.get(tag);
        if (value instanceof String) {
            return (String)value;
        }
        return null;
    }

    private static boolean decodeBooleanConfigValue(
        String tag, boolean defaultToTrue, Map<String, Object> config
    ) {
        String value = getString(tag, config);

        if ("0".equals(value) || "false".equals(value)) {
            return false;
        }
        if ("1".equals(value) || "true".equals(value)) {
            return true;
        }

        return defaultToTrue;
    }

    private static void splitEncParts(String tmpS, List<SecurePart> parts, String soapNS) {
        SecurePart encPart = null;
        String[] rawParts = tmpS.split(";");

        for (int i = 0; i < rawParts.length; i++) {
            String[] partDef = rawParts[i].split("}");

            if (partDef.length == 1) {
                QName qname = new QName(soapNS, partDef[0].trim());
                encPart = new SecurePart(qname, SecurePart.Modifier.Content);
            } else if (partDef.length == 2) {
                String mode = partDef[0].trim().substring(1);
                String element = partDef[1].trim();

                if ("Content".equals(mode)) {
                    encPart = new SecurePart(element, SecurePart.Modifier.Content);
                } else {
                    encPart = new SecurePart(element, SecurePart.Modifier.Element);
                }
            } else if (partDef.length == 3) {
                String mode = partDef[0].trim();
                if (mode.length() <= 1) {
                    mode = "Content";
                } else {
                    mode = mode.substring(1);
                }
                String nmSpace = partDef[1].trim();
                if (nmSpace.length() <= 1) {
                    nmSpace = soapNS;
                } else {
                    nmSpace = nmSpace.substring(1);
                    if ("Null".equals(nmSpace)) {
                        nmSpace = null;
                    }
                }
                String element = partDef[2].trim();

                QName qname = new QName(nmSpace, element);
                if ("Content".equals(mode)) {
                    encPart = new SecurePart(qname, SecurePart.Modifier.Content);
                } else {
                    encPart = new SecurePart(qname, SecurePart.Modifier.Element);
                }
            }

            parts.add(encPart);
        }
    }

}
