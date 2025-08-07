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

package org.apache.wss4j.dom.handler;

import java.security.Provider;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;

import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.EncryptionActionToken;
import org.apache.wss4j.common.SignatureActionToken;
import org.apache.wss4j.common.bsp.BSPEnforcer;
import org.apache.wss4j.common.bsp.BSPRule;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.crypto.AlgorithmSuite;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.SOAPConstants;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.validate.Validator;
import org.apache.xml.security.encryption.Serializer;

/**
 * This class holds per request data.
 */
public class RequestData {

    private Object msgContext;
    private SOAPConstants soapConstants;
    private String actor;
    private String username;
    private String pwType = WSConstants.PASSWORD_DIGEST; // Make this the default when no password type is given.
    private Crypto sigVerCrypto;
    private Crypto decCrypto;
    private SignatureActionToken signatureToken;
    private EncryptionActionToken encryptionToken;
    private WSSConfig wssConfig;
    private List<byte[]> signatureValues = new ArrayList<>();
    private WSSecHeader secHeader;
    private int derivedKeyIterations = 1000;
    private boolean useDerivedKeyForMAC = true;
    private CallbackHandler callback;
    private CallbackHandler attachmentCallbackHandler;
    private boolean enableRevocation;
    private boolean requireSignedEncryptedDataElements;
    private ReplayCache timestampReplayCache;
    private ReplayCache nonceReplayCache;
    private ReplayCache samlOneTimeUseReplayCache;
    private Collection<Pattern> subjectDNPatterns = new ArrayList<>();
    private Collection<Pattern> issuerDNPatterns = new ArrayList<>();
    private final List<BSPRule> ignoredBSPRules = new LinkedList<>();
    private boolean appendSignatureAfterTimestamp;
    private int originalSignatureActionPosition;
    private AlgorithmSuite algorithmSuite;
    private AlgorithmSuite samlAlgorithmSuite;
    private boolean disableBSPEnforcement;
    private boolean allowRSA15KeyTransportAlgorithm;
    private boolean addUsernameTokenNonce;
    private boolean addUsernameTokenCreated;
    private Certificate[] tlsCerts;
    private PasswordEncryptor passwordEncryptor;
    private String derivedKeyTokenReference;
    private boolean use200512Namespace = true;
    private final List<String> audienceRestrictions = new ArrayList<>();
    private boolean requireTimestampExpires;
    private boolean storeBytesInAttachment;
    private Serializer encryptionSerializer;
    private WSDocInfo wsDocInfo;
    private Provider signatureProvider;

    /**
     * Whether to add an InclusiveNamespaces PrefixList as a CanonicalizationMethod
     * child when generating Signatures using WSConstants.C14N_EXCL_OMIT_COMMENTS.
     * The default is true.
     */
    private boolean addInclusivePrefixes = true;

    /**
     * Set the timestamp precision mode. If set to <code>true</code> then use
     * timestamps with milliseconds, otherwise omit the milliseconds. As per XML
     * Date/Time specification the default is to include the milliseconds.
     */
    private boolean precisionInMilliSeconds = true;

    private boolean enableSignatureConfirmation;

    /**
     * If set to true then the timestamp handling will throw an exception if the
     * timestamp contains an expires element and the semantics are expired.
     *
     * If set to false, no exception will be thrown, even if the semantics are
     * expired.
     */
    private boolean timeStampStrict = true;

    /**
     * If this value is not null, then username token handling will throw an
     * exception if the password type of the Username Token does not match this value
     */
    private String requiredPasswordType;

    /**
     * This variable controls whether a UsernameToken with no password element is allowed.
     * The default value is "false". Set it to "true" to allow deriving keys from UsernameTokens
     * or to support UsernameTokens for purposes other than authentication.
     */
    private boolean allowUsernameTokenNoPassword;

    /**
     * The time in seconds between creation and expiry for a Timestamp. The default
     * is 300 seconds (5 minutes).
     */
    private int timeStampTTL = 300;

    /**
     * The time in seconds in the future within which the Created time of an incoming
     * Timestamp is valid. The default is 60 seconds.
     */
    private int timeStampFutureTTL = 60;

    /**
     * The time in seconds between creation and expiry for a UsernameToken Created
     * element. The default is 300 seconds (5 minutes).
     */
    private int utTTL = 300;

    /**
     * The time in seconds in the future within which the Created time of an incoming
     * UsernameToken is valid. The default is 60 seconds.
     */
    private int utFutureTTL = 60;

    /**
     * This variable controls whether types other than PasswordDigest or PasswordText
     * are allowed when processing UsernameTokens.
     *
     * By default this is set to false so that the user doesn't have to explicitly
     * reject custom token types in the callback handler.
     */
    private boolean handleCustomPasswordTypes;

    /**
     * This variable controls whether (wsse) namespace qualified password types are
     * accepted when processing UsernameTokens.
     *
     * By default this is set to false.
     */
    private boolean allowNamespaceQualifiedPasswordTypes;

    /**
     * Whether the password should be treated as a binary value.  This
     * is needed to properly handle password equivalence for UsernameToken
     * passwords.  Binary passwords are Base64 encoded so they can be
     * treated as strings in most places, but when the password digest
     * is calculated or a key is derived from the password, the password
     * will be Base64 decoded before being used. This is most useful for
     * hashed passwords as password equivalents.
     *
     * See https://issues.apache.org/jira/browse/WSS-239
     */
    private boolean encodePasswords;

    /**
     * Whether to validate the SubjectConfirmation requirements of a received SAML Token
     * (sender-vouches or holder-of-key). The default is true.
     */
    private boolean validateSamlSubjectConfirmation = true;

    private boolean expandXopInclude;

    public Object getMsgContext() {
        return msgContext;
    }

    public void setMsgContext(Object msgContext) {
        this.msgContext = msgContext;
    }

    public SOAPConstants getSoapConstants() {
        return soapConstants;
    }

    public void setSoapConstants(SOAPConstants soapConstants) {
        this.soapConstants = soapConstants;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPwType() {
        return pwType;
    }

    public void setPwType(String pwType) {
        this.pwType = pwType;
    }

    public Crypto getSigVerCrypto() {
        return sigVerCrypto;
    }

    public void setSigVerCrypto(Crypto sigVerCrypto) {
        this.sigVerCrypto = sigVerCrypto;
    }

    public Crypto getDecCrypto() {
        return decCrypto;
    }

    public void setDecCrypto(Crypto decCrypto) {
        this.decCrypto = decCrypto;
    }

    /**
     * @return Returns the wssConfig.
     */
    public WSSConfig getWssConfig() {
        return wssConfig;
    }

    /**
     * @param wssConfig The wssConfig to set.
     */
    public void setWssConfig(WSSConfig wssConfig) {
        this.wssConfig = wssConfig;
    }

    /**
     * @return Returns the list of stored signature values.
     */
    public List<byte[]> getSignatureValues() {
        return signatureValues;
    }

    /**
     * @return Returns the secHeader.
     */
    public WSSecHeader getSecHeader() {
        return secHeader;
    }

    /**
     * @param secHeader The secHeader to set.
     */
    public void setSecHeader(WSSecHeader secHeader) {
        this.secHeader = secHeader;
    }

    /**
     * Set the derived key iterations. Default is 1000.
     * @param iterations The number of iterations to use when deriving a key
     */
    public void setDerivedKeyIterations(int iterations) {
        derivedKeyIterations = iterations;
    }

    /**
     * Get the derived key iterations.
     * @return The number of iterations to use when deriving a key
     */
    public int getDerivedKeyIterations() {
        return derivedKeyIterations;
    }

    /**
     * Whether to use the derived key for a MAC.
     * @param useMac Whether to use the derived key for a MAC.
     */
    public void setUseDerivedKeyForMAC(boolean useMac) {
        useDerivedKeyForMAC = useMac;
    }

    /**
     * Whether to use the derived key for a MAC.
     * @return Whether to use the derived key for a MAC.
     */
    public boolean isUseDerivedKeyForMAC() {
        return useDerivedKeyForMAC;
    }

    /**
     * Set whether to enable CRL checking or not when verifying trust in a certificate.
     * @param enableRevocation whether to enable CRL checking
     */
    public void setEnableRevocation(boolean enableRevocation) {
        this.enableRevocation = enableRevocation;
    }

    /**
     * Get whether to enable CRL checking or not when verifying trust in a certificate.
     * @return whether to enable CRL checking
     */
    public boolean isRevocationEnabled() {
        return enableRevocation;
    }

    /**
     * @return whether EncryptedData elements are required to be signed
     */
    public boolean isRequireSignedEncryptedDataElements() {
        return requireSignedEncryptedDataElements;
    }

    /**
     * Configure the engine to verify that EncryptedData elements
     * are in a signed subtree of the document. This can be used to
     * prevent some wrapping based attacks when encrypt-before-sign
     * token protection is selected.
     *
     * @param requireSignedEncryptedDataElements
     */
    public void setRequireSignedEncryptedDataElements(boolean requireSignedEncryptedDataElements) {
        this.requireSignedEncryptedDataElements = requireSignedEncryptedDataElements;
    }

    /**
     * Sets the CallbackHandler used for this request
     * @param cb
     */
    public void setCallbackHandler(CallbackHandler cb) {
        callback = cb;
    }

    /**
     * Returns the CallbackHandler used for this request.
     * @return the CallbackHandler used for this request.
     */
    public CallbackHandler getCallbackHandler() {
        return callback;
    }

    public CallbackHandler getAttachmentCallbackHandler() {
        return attachmentCallbackHandler;
    }

    public void setAttachmentCallbackHandler(CallbackHandler attachmentCallbackHandler) {
        this.attachmentCallbackHandler = attachmentCallbackHandler;
    }

    /**
     * Get the Validator instance corresponding to the QName
     * @param qName the QName with which to find a Validator instance
     * @return the Validator instance corresponding to the QName
     * @throws WSSecurityException
     */
    public Validator getValidator(QName qName) throws WSSecurityException {
        // Check the custom Validator Map first
        if (getMsgContext() instanceof Map<?,?>) {
            @SuppressWarnings("unchecked")
            Map<QName, Validator> validatorMap =
                (Map<QName, Validator>)((Map<?,?>)getMsgContext()).get(ConfigurationConstants.VALIDATOR_MAP);
            if (validatorMap != null && validatorMap.containsKey(qName)) {
                return validatorMap.get(qName);
            }
        }
        if (wssConfig != null) {
            return wssConfig.getValidator(qName);
        }
        return null;
    }

    /**
     * Set the replay cache for Timestamps
     */
    public void setTimestampReplayCache(ReplayCache newCache) {
        timestampReplayCache = newCache;
    }

    /**
     * Get the replay cache for Timestamps
     * @throws WSSecurityException
     */
    public ReplayCache getTimestampReplayCache() throws WSSecurityException {
        return timestampReplayCache;
    }

    /**
     * Set the replay cache for Nonces
     */
    public void setNonceReplayCache(ReplayCache newCache) {
        nonceReplayCache = newCache;
    }

    /**
     * Get the replay cache for Nonces
     * @throws WSSecurityException
     */
    public ReplayCache getNonceReplayCache() throws WSSecurityException {
        return nonceReplayCache;
    }

    /**
     * Set the replay cache for SAML2 OneTimeUse Assertions
     */
    public void setSamlOneTimeUseReplayCache(ReplayCache newCache) {
        samlOneTimeUseReplayCache = newCache;
    }

    /**
     * Get the replay cache for SAML2 OneTimeUse Assertions
     * @throws WSSecurityException
     */
    public ReplayCache getSamlOneTimeUseReplayCache() throws WSSecurityException {
        return samlOneTimeUseReplayCache;
    }

    /**
     * Set the Signature Subject Cert Constraints
     */
    public void setSubjectCertConstraints(Collection<Pattern> subjectCertConstraints) {
        if (subjectCertConstraints != null) {
            subjectDNPatterns.addAll(subjectCertConstraints);
        }
    }

    /**
     * Get the Signature Subject Cert Constraints
     */
    public Collection<Pattern> getSubjectCertConstraints() {
        return subjectDNPatterns;
    }

    /**
     * Get the Signature Issuer DN Cert Constraints
     * @return
     */
    public Collection<Pattern> getIssuerDNPatterns() {
        return issuerDNPatterns;
    }
    /**
     * Set the Signature Issuer DN Cert Constraints
     *
     */
    public void setIssuerDNPatterns(Collection<Pattern> issuerDNPatterns) {
        this.issuerDNPatterns = issuerDNPatterns;
    }

    /**
     * Set the Audience Restrictions
     */
    public void setAudienceRestrictions(List<String> audienceRestrictions) {
        if (audienceRestrictions != null) {
            this.audienceRestrictions.addAll(audienceRestrictions);
        }
    }

    /**
     * Get the Audience Restrictions
     */
    public List<String> getAudienceRestrictions() {
        return audienceRestrictions;
    }

    public void setIgnoredBSPRules(List<BSPRule> bspRules) {
        ignoredBSPRules.clear();
        ignoredBSPRules.addAll(bspRules);
    }

    public BSPEnforcer getBSPEnforcer() {
        if (disableBSPEnforcement) {
            return new BSPEnforcer(true);
        }
        return new BSPEnforcer(ignoredBSPRules);
    }

    public boolean isAppendSignatureAfterTimestamp() {
        return appendSignatureAfterTimestamp;
    }

    public void setAppendSignatureAfterTimestamp(boolean appendSignatureAfterTimestamp) {
        this.appendSignatureAfterTimestamp = appendSignatureAfterTimestamp;
    }

    public AlgorithmSuite getAlgorithmSuite() {
        return algorithmSuite;
    }

    public void setAlgorithmSuite(AlgorithmSuite algorithmSuite) {
        this.algorithmSuite = algorithmSuite;
    }

    public AlgorithmSuite getSamlAlgorithmSuite() {
        return samlAlgorithmSuite;
    }

    public void setSamlAlgorithmSuite(AlgorithmSuite samlAlgorithmSuite) {
        this.samlAlgorithmSuite = samlAlgorithmSuite;
    }

    public int getOriginalSignatureActionPosition() {
        return originalSignatureActionPosition;
    }

    public void setOriginalSignatureActionPosition(int originalSignatureActionPosition) {
        this.originalSignatureActionPosition = originalSignatureActionPosition;
    }

    public boolean isDisableBSPEnforcement() {
        return disableBSPEnforcement;
    }

    public void setDisableBSPEnforcement(boolean disableBSPEnforcement) {
        this.disableBSPEnforcement = disableBSPEnforcement;
    }

    public boolean isAllowRSA15KeyTransportAlgorithm() {
        return allowRSA15KeyTransportAlgorithm;
    }

    public void setAllowRSA15KeyTransportAlgorithm(boolean allowRSA15KeyTransportAlgorithm) {
        this.allowRSA15KeyTransportAlgorithm = allowRSA15KeyTransportAlgorithm;
    }

    public Certificate[] getTlsCerts() {
        return tlsCerts;
    }

    public void setTlsCerts(Certificate[] tlsCerts) {
        this.tlsCerts = tlsCerts;
    }

    public PasswordEncryptor getPasswordEncryptor() {
        return passwordEncryptor;
    }

    public void setPasswordEncryptor(PasswordEncryptor passwordEncryptor) {
        this.passwordEncryptor = passwordEncryptor;
    }

    public SignatureActionToken getSignatureToken() {
        return signatureToken;
    }

    public void setSignatureToken(SignatureActionToken signatureToken) {
        this.signatureToken = signatureToken;
    }

    public EncryptionActionToken getEncryptionToken() {
        return encryptionToken;
    }

    public void setEncryptionToken(EncryptionActionToken encryptionToken) {
        this.encryptionToken = encryptionToken;
    }

    public String getDerivedKeyTokenReference() {
        return derivedKeyTokenReference;
    }

    public void setDerivedKeyTokenReference(String derivedKeyTokenReference) {
        this.derivedKeyTokenReference = derivedKeyTokenReference;
    }

    public boolean isUse200512Namespace() {
        return use200512Namespace;
    }

    public void setUse200512Namespace(boolean use200512Namespace) {
        this.use200512Namespace = use200512Namespace;
    }

    public boolean isRequireTimestampExpires() {
        return requireTimestampExpires;
    }

    public void setRequireTimestampExpires(boolean requireTimestampExpires) {
        this.requireTimestampExpires = requireTimestampExpires;
    }

    public boolean isValidateSamlSubjectConfirmation() {
        return validateSamlSubjectConfirmation;
    }

    public void setValidateSamlSubjectConfirmation(boolean validateSamlSubjectConfirmation) {
        this.validateSamlSubjectConfirmation = validateSamlSubjectConfirmation;
    }

    public boolean isAllowNamespaceQualifiedPasswordTypes() {
        return allowNamespaceQualifiedPasswordTypes;
    }

    public void setAllowNamespaceQualifiedPasswordTypes(boolean allowNamespaceQualifiedPasswordTypes) {
        this.allowNamespaceQualifiedPasswordTypes = allowNamespaceQualifiedPasswordTypes;
    }

    public int getUtFutureTTL() {
        return utFutureTTL;
    }

    public void setUtFutureTTL(int utFutureTTL) {
        this.utFutureTTL = utFutureTTL;
    }

    public boolean isHandleCustomPasswordTypes() {
        return handleCustomPasswordTypes;
    }

    public void setHandleCustomPasswordTypes(boolean handleCustomPasswordTypes) {
        this.handleCustomPasswordTypes = handleCustomPasswordTypes;
    }

    public int getUtTTL() {
        return utTTL;
    }

    public void setUtTTL(int utTTL) {
        this.utTTL = utTTL;
    }

    public int getTimeStampTTL() {
        return timeStampTTL;
    }

    public void setTimeStampTTL(int timeStampTTL) {
        this.timeStampTTL = timeStampTTL;
    }

    public int getTimeStampFutureTTL() {
        return timeStampFutureTTL;
    }

    public void setTimeStampFutureTTL(int timeStampFutureTTL) {
        this.timeStampFutureTTL = timeStampFutureTTL;
    }

    public boolean isAllowUsernameTokenNoPassword() {
        return allowUsernameTokenNoPassword;
    }

    public void setAllowUsernameTokenNoPassword(boolean allowUsernameTokenNoPassword) {
        this.allowUsernameTokenNoPassword = allowUsernameTokenNoPassword;
    }

    public boolean isTimeStampStrict() {
        return timeStampStrict;
    }

    public void setTimeStampStrict(boolean timeStampStrict) {
        this.timeStampStrict = timeStampStrict;
    }

    public boolean isAddInclusivePrefixes() {
        return addInclusivePrefixes;
    }

    public void setAddInclusivePrefixes(boolean addInclusivePrefixes) {
        this.addInclusivePrefixes = addInclusivePrefixes;
    }

    public boolean isPrecisionInMilliSeconds() {
        return precisionInMilliSeconds;
    }

    public void setPrecisionInMilliSeconds(boolean precisionInMilliSeconds) {
        this.precisionInMilliSeconds = precisionInMilliSeconds;
    }

    public boolean isEnableSignatureConfirmation() {
        return enableSignatureConfirmation;
    }

    public void setEnableSignatureConfirmation(boolean enableSignatureConfirmation) {
        this.enableSignatureConfirmation = enableSignatureConfirmation;
    }

    public String getRequiredPasswordType() {
        return requiredPasswordType;
    }

    public void setRequiredPasswordType(String requiredPasswordType) {
        this.requiredPasswordType = requiredPasswordType;
    }

    public boolean isEncodePasswords() {
        return encodePasswords;
    }

    public void setEncodePasswords(boolean encodePasswords) {
        this.encodePasswords = encodePasswords;
    }

    public boolean isStoreBytesInAttachment() {
        return storeBytesInAttachment;
    }

    public void setStoreBytesInAttachment(boolean storeBytesInAttachment) {
        this.storeBytesInAttachment = storeBytesInAttachment;
    }

    public boolean isExpandXopInclude() {
        return expandXopInclude;
    }

    public void setExpandXopInclude(boolean expandXopInclude) {
        this.expandXopInclude = expandXopInclude;
    }

    public Serializer getEncryptionSerializer() {
        return encryptionSerializer;
    }

    public void setEncryptionSerializer(Serializer encryptionSerializer) {
        this.encryptionSerializer = encryptionSerializer;
    }

    public boolean isAddUsernameTokenCreated() {
        return addUsernameTokenCreated;
    }

    public void setAddUsernameTokenCreated(boolean addUsernameTokenCreated) {
        this.addUsernameTokenCreated = addUsernameTokenCreated;
    }

    public boolean isAddUsernameTokenNonce() {
        return addUsernameTokenNonce;
    }

    public void setAddUsernameTokenNonce(boolean addUsernameTokenNonce) {
        this.addUsernameTokenNonce = addUsernameTokenNonce;
    }

    public WSDocInfo getWsDocInfo() {
        return wsDocInfo;
    }

    public void setWsDocInfo(WSDocInfo wsDocInfo) {
        this.wsDocInfo = wsDocInfo;
    }

    public Provider getSignatureProvider() {
        return signatureProvider;
    }

    /**
     * Set a security Provider instance to use for Signature
     */
    public void setSignatureProvider(Provider signatureProvider) {
        this.signatureProvider = signatureProvider;
    }
}
