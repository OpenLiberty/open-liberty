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

import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;

import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.common.EncryptionActionToken;
import org.apache.wss4j.common.SignatureActionToken;
import org.apache.wss4j.common.SignatureEncryptionActionToken;
import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.crypto.AlgorithmSuite;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.JasyptPasswordEncryptor;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.Loader;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.token.SignatureConfirmation;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.xml.security.encryption.params.KeyDerivationParameters;
import org.w3c.dom.Document;

/**
 * Extracted from WSDoAllReceiver and WSDoAllSender
 * Extended to all passwordless UsernameTokens and configurable identities.
 */
public abstract class WSHandler {
    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(WSHandler.class);
    protected Map<String, Crypto> cryptos = new ConcurrentHashMap<>();

    /**
     * Performs all defined security actions to set-up the SOAP request.
     *
     * @param doc   the request as DOM document
     * @param reqData a data storage to pass values around between methods
     * @param actions a list holding the actions to do in the order defined
     *                in the deployment file or property, plus an optional
     *                associated SecurityActionToken object for that Action
     * @throws WSSecurityException
     */
    protected void doSenderAction(
            Document doc,
            RequestData reqData,
            List<HandlerAction> actions,
            boolean isRequest
    ) throws WSSecurityException {

        WSSConfig wssConfig = reqData.getWssConfig();
        if (wssConfig == null) {
            wssConfig = WSSConfig.getNewInstance();
            reqData.setWssConfig(wssConfig);
        }

        if (reqData.getWsDocInfo() == null) {
            WSDocInfo wsDocInfo = new WSDocInfo(doc);
            reqData.setWsDocInfo(wsDocInfo);
        }

        Object mc = reqData.getMsgContext();
        reqData.setEncodePasswords(
            decodeBooleanConfigValue(mc, WSHandlerConstants.USE_ENCODED_PASSWORDS, false)
        );
        reqData.setPrecisionInMilliSeconds(
            decodeBooleanConfigValue(mc, WSHandlerConstants.TIMESTAMP_PRECISION, true)
        );
        reqData.setAddInclusivePrefixes(
            decodeBooleanConfigValue(mc, WSHandlerConstants.ADD_INCLUSIVE_PREFIXES, true)
        );
        reqData.setEnableSignatureConfirmation(
            decodeBooleanConfigValue(mc, WSHandlerConstants.ENABLE_SIGNATURE_CONFIRMATION, false)
        );
        reqData.setTimeStampTTL(decodeTimeToLive(reqData, true));

        String actor = getString(WSHandlerConstants.ACTOR, mc);
        reqData.setActor(actor);

        boolean mu = decodeBooleanConfigValue(mc, WSHandlerConstants.MUST_UNDERSTAND, true);
        WSSecHeader secHeader = new WSSecHeader(actor, mu, doc);
        secHeader.insertSecurityHeader();
        reqData.setSecHeader(secHeader);
        reqData.setSoapConstants(WSSecurityUtil.getSOAPConstants(doc.getDocumentElement()));

        // Load CallbackHandler
        if (reqData.getCallbackHandler() == null) {
            CallbackHandler passwordCallbackHandler = getPasswordCallbackHandler(reqData);
            reqData.setCallbackHandler(passwordCallbackHandler);
        }

        if (!reqData.isStoreBytesInAttachment()) {
            boolean storeBytesInAttachment =
                decodeBooleanConfigValue(mc, WSHandlerConstants.STORE_BYTES_IN_ATTACHMENT, false);
            reqData.setStoreBytesInAttachment(storeBytesInAttachment);
        }

        // Perform configuration
        boolean encryptionFound = false;
        for (HandlerAction actionToDo : actions) {
            if (actionToDo.getAction() == WSConstants.SC) {
                reqData.setEnableSignatureConfirmation(true);
            } else if ((actionToDo.getAction() == WSConstants.UT
                || actionToDo.getAction() == WSConstants.UT_NOPASSWORD)
                && actionToDo.getActionToken() == null) {
                decodeUTParameter(reqData);
                if (actionToDo.getAction() == WSConstants.UT_NOPASSWORD) {
                    reqData.setPwType(null);
                }
            } else if (actionToDo.getAction() == WSConstants.UT_SIGN
                && actionToDo.getActionToken() == null) {
                decodeUTParameter(reqData);
                decodeSignatureParameter(reqData);
            } else if ((actionToDo.getAction() == WSConstants.SIGN
                || actionToDo.getAction() == WSConstants.DKT_SIGN)
                && actionToDo.getActionToken() == null) {
                SignatureActionToken actionToken = reqData.getSignatureToken();
                if (actionToken == null) {
                    actionToken = new SignatureActionToken();
                    reqData.setSignatureToken(actionToken);
                }
                if (actionToken.getCrypto() == null) {
                    actionToken.setCrypto(loadSignatureCrypto(reqData));
                }
                decodeSignatureParameter(reqData);
                if (encryptionFound && reqData.isStoreBytesInAttachment()) {
                    LOG.warn("Turning off storeBytesInAttachment as we have encryption before signature."
                             + " The danger here is that the actual encryption bytes will not be signed");
                    reqData.setStoreBytesInAttachment(false);
                }
            } else if (actionToDo.getAction() == WSConstants.ST_SIGNED
                && actionToDo.getActionToken() == null) {
                decodeSignatureParameter(reqData);
            } else if ((actionToDo.getAction() == WSConstants.ENCR
                || actionToDo.getAction() == WSConstants.DKT_ENCR)
                && actionToDo.getActionToken() == null) {
                encryptionFound = true;
                EncryptionActionToken actionToken = reqData.getEncryptionToken();
                if (actionToken == null) {
                    actionToken = new EncryptionActionToken();
                    reqData.setEncryptionToken(actionToken);
                }
                if (actionToken.getCrypto() == null) {
                    actionToken.setCrypto(loadEncryptionCrypto(reqData));
                }
                decodeEncryptionParameter(reqData);
            }
        }

        /*
         * If after all the parsing no Signature parts defined, set here a
         * default set. This is necessary because we add SignatureConfirmation
         * and therefore the default (Body) must be set here. The default setting
         * in WSSignEnvelope doesn't work because the vector is not empty anymore.
         */
        SignatureActionToken signatureToken = reqData.getSignatureToken();
        if (signatureToken == null) {
            signatureToken = new SignatureActionToken();
            reqData.setSignatureToken(signatureToken);
        }
        if (signatureToken.getParts().isEmpty()) {
            signatureToken.getParts().add(WSSecurityUtil.getDefaultEncryptionPart(doc));
        }
        /*
         * If SignatureConfirmation is enabled and this is a response then
         * insert SignatureConfirmation elements, note their wsu:id in the signature
         * parts. They will be signed automatically during a (probably) defined
         * SIGN action.
         */
        if (reqData.isEnableSignatureConfirmation() && !isRequest) {
            String done =
                (String)getProperty(reqData.getMsgContext(), WSHandlerConstants.SIG_CONF_DONE);
            if (done == null) {
                wssConfig.getAction(WSConstants.SC).execute(this, null, reqData);
            }
        }

        // See if the Signature and Timestamp actions (in that order) are defined, and if
        // the Timestamp is to be signed. In this case we need to swap the actions, as the
        // Timestamp must appear in the security header first for signature creation to work.
        List<HandlerAction> actionsToPerform = actions;
        HandlerAction signingAction = getSignatureActionThatSignsATimestamp(actions, reqData);

        if (signingAction != null) {
            actionsToPerform = new ArrayList<>(actions);

            // Find TimestampAction
            int timestampIndex = -1;
            for (int i = 0; i < actionsToPerform.size(); i++) {
                if (actionsToPerform.get(i).getAction() == WSConstants.TS) {
                    timestampIndex = i;
                    break;
                }
            }

            int signatureIndex = actionsToPerform.indexOf(signingAction);
            if (timestampIndex >= 0) {
                actionsToPerform.set(signatureIndex, actionsToPerform.get(timestampIndex));
                actionsToPerform.set(timestampIndex, signingAction);
            }
            reqData.setAppendSignatureAfterTimestamp(true);
            reqData.setOriginalSignatureActionPosition(signatureIndex);
        }

        /*
         * Here we have all necessary information to perform the requested
         * action(s).
         */
        for (HandlerAction actionToDo : actionsToPerform) {
            LOG.debug("Performing Action: {}", actionToDo.getAction());

            if (WSConstants.NO_SECURITY != actionToDo.getAction()) {
                wssConfig.getAction(actionToDo.getAction()).execute(
                    this, actionToDo.getActionToken(), reqData);
            }
        }

        /*
         * If this is a request then store all signature values. Add ours to
         * already gathered values because of chained handlers, e.g. for
         * other actors.
         */
        if (reqData.isEnableSignatureConfirmation()
            && isRequest && !reqData.getSignatureValues().isEmpty()) {
            @SuppressWarnings("unchecked")
            Set<Integer> savedSignatures =
                (Set<Integer>)getProperty(reqData.getMsgContext(), WSHandlerConstants.SEND_SIGV);
            if (savedSignatures == null) {
                savedSignatures = new HashSet<>();
                setProperty(
                    reqData.getMsgContext(), WSHandlerConstants.SEND_SIGV, savedSignatures
                );
            }
            for (byte[] signatureValue : reqData.getSignatureValues()) {
                savedSignatures.add(Arrays.hashCode(signatureValue));
            }
        }
    }

    private HandlerAction getSignatureActionThatSignsATimestamp(
        List<HandlerAction> actions, RequestData reqData
    ) {
        for (HandlerAction action : actions) {
            // Only applies if a Signature is before a Timestamp
            if (action.getAction() == WSConstants.TS) {
                return null;
            } else if (action.getAction() == WSConstants.SIGN) {
                if (action.getActionToken() != null
                    && ((SignatureEncryptionActionToken)action.getActionToken()).getParts() != null) {
                    for (WSEncryptionPart encP
                        : ((SignatureEncryptionActionToken)action.getActionToken()).getParts()) {
                        if (WSConstants.WSU_NS.equals(encP.getNamespace())
                            && "Timestamp".equals(encP.getName())) {
                            return action;
                        }
                    }
                } else {
                    for (WSEncryptionPart encP : reqData.getSignatureToken().getParts()) {
                        if (WSConstants.WSU_NS.equals(encP.getNamespace())
                            && "Timestamp".equals(encP.getName())) {
                            return action;
                        }
                    }
                }
            }
        }
        return null;
    }

    protected void doReceiverAction(List<Integer> actions, RequestData reqData)
        throws WSSecurityException {

        WSSConfig wssConfig = reqData.getWssConfig();
        if (wssConfig == null) {
            wssConfig = WSSConfig.getNewInstance();
            reqData.setWssConfig(wssConfig);
        }

        Object mc = reqData.getMsgContext();
        boolean enableSigConf =
            decodeBooleanConfigValue(mc, WSHandlerConstants.ENABLE_SIGNATURE_CONFIRMATION, false);
        reqData.setEnableSignatureConfirmation(
            enableSigConf || actions.contains(WSConstants.SC)
        );
        reqData.setTimeStampStrict(
            decodeBooleanConfigValue(mc, WSHandlerConstants.TIMESTAMP_STRICT, true)
        );
        reqData.setRequiredPasswordType(decodePasswordType(reqData));

        reqData.setTimeStampTTL(decodeTimeToLive(reqData, true));
        reqData.setTimeStampFutureTTL(decodeFutureTimeToLive(reqData, true));
        reqData.setUtTTL(decodeTimeToLive(reqData, false));
        reqData.setUtFutureTTL(decodeFutureTimeToLive(reqData, false));

        reqData.setHandleCustomPasswordTypes(
            decodeBooleanConfigValue(mc, WSHandlerConstants.HANDLE_CUSTOM_PASSWORD_TYPES, false)
        );
        reqData.setEncodePasswords(
            decodeBooleanConfigValue(mc, WSHandlerConstants.USE_ENCODED_PASSWORDS, false)
        );
        reqData.setAllowNamespaceQualifiedPasswordTypes(
            decodeBooleanConfigValue(mc, WSHandlerConstants.ALLOW_NAMESPACE_QUALIFIED_PASSWORD_TYPES, false)
        );
        reqData.setAllowUsernameTokenNoPassword(
            decodeBooleanConfigValue(mc, WSHandlerConstants.ALLOW_USERNAMETOKEN_NOPASSWORD, false)
        );
        reqData.setValidateSamlSubjectConfirmation(
            decodeBooleanConfigValue(mc, WSHandlerConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION, true)
        );

        boolean bspCompliant =
            decodeBooleanConfigValue(mc, WSHandlerConstants.IS_BSP_COMPLIANT, true);
        if (!bspCompliant) {
            reqData.setDisableBSPEnforcement(true);
        }

        // Load CallbackHandler
        if (reqData.getCallbackHandler() == null) {
            CallbackHandler passwordCallbackHandler = getPasswordCallbackHandler(reqData);
            reqData.setCallbackHandler(passwordCallbackHandler);
        }

        if (actions.contains(WSConstants.SIGN) || actions.contains(WSConstants.ST_SIGNED)
            || actions.contains(WSConstants.ST_UNSIGNED)) {
            decodeSignatureParameter2(reqData);
        }

        if (actions.contains(WSConstants.ENCR)) {
            decodeDecryptionParameter(reqData);
        }
        reqData.setRequireSignedEncryptedDataElements(
            decodeBooleanConfigValue(
                mc, WSHandlerConstants.REQUIRE_SIGNED_ENCRYPTED_DATA_ELEMENTS, false
            )
        );
        reqData.setRequireTimestampExpires(
            decodeBooleanConfigValue(mc, WSHandlerConstants.REQUIRE_TIMESTAMP_EXPIRES, false)
        );
    }

    protected boolean checkReceiverResults(
        List<WSSecurityEngineResult> wsResult, List<Integer> actions
    ) {
        int size = actions.size();
        int ai = 0;
        for (WSSecurityEngineResult result : wsResult) {
            final Integer actInt = (Integer) result.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt != null) {
                int act = actInt;
                if (act == WSConstants.SC || act == WSConstants.BST) {
                    continue;
                }

                if (ai >= size || actions.get(ai++) != act) {
                    return false;
                }
            }
        }

        return ai == size;
    }

    protected boolean checkReceiverResultsAnyOrder(
        List<WSSecurityEngineResult> wsResult, List<Integer> actions
    ) {
        List<Integer> recordedActions = new ArrayList<>(actions.size());
        for (Integer action : actions) {
            recordedActions.add(action);
        }

        for (WSSecurityEngineResult result : wsResult) {
            final Integer actInt = (Integer) result.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt != null) {
                int act = actInt;
                if (act == WSConstants.SC || act == WSConstants.BST) {
                    continue;
                } else if (act == WSConstants.ENCR
                    && (result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS) == null
                        || ((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS)).isEmpty())) {
                    continue;
                }


                if (!recordedActions.remove(actInt)) {
                    return false;
                }
            }
        }

        return recordedActions.isEmpty();
    }

    @SuppressWarnings("unchecked")
    protected void checkSignatureConfirmation(
        RequestData reqData,
        WSHandlerResult handlerResults
    ) throws WSSecurityException {
        LOG.debug("Check Signature confirmation");
        //
        // First get all Signature values stored during sending the request
        //
        Set<Integer> savedSignatures =
            (Set<Integer>) getProperty(reqData.getMsgContext(), WSHandlerConstants.SEND_SIGV);
        //
        // Now get all results that hold a SignatureConfirmation element from
        // the current run of receiver (we can have more than one run: if we
        // have several security header blocks with different actors/roles)
        //
        List<WSSecurityEngineResult> sigConf =
            handlerResults.getActionResults().get(WSConstants.SC);
        //
        // now loop over all SignatureConfirmation results and check:
        // - if there is a signature value and no Signature value generated in request: error
        // - if there is a signature value and no matching Signature value found: error
        //
        //  If a matching value found: remove from vector of stored signature values
        //
        if (sigConf != null) {
            for (WSSecurityEngineResult result : sigConf) {
                SignatureConfirmation sc =
                    (SignatureConfirmation)result.get(
                        WSSecurityEngineResult.TAG_SIGNATURE_CONFIRMATION
                    );

                if (sc != null && sc.getSignatureValue() != null) {
                    if (savedSignatures == null || savedSignatures.isEmpty()) {
                        //
                        // If there are no stored signature values, and we've received a
                        // SignatureConfirmation element then throw an Exception
                        //
                        if (sc.getSignatureValue().length != 0) {
                            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "empty",
                                 new Object[] {"Received a SignatureConfirmation element, but there are no stored"
                                 + " signature values"}
                            );
                        }
                    } else {
                        Integer hash = Arrays.hashCode(sc.getSignatureValue());
                        if (savedSignatures.contains(hash)) {
                            savedSignatures.remove(hash);
                        } else {
                            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                new Object[] {"Received a SignatureConfirmation element, but there are no matching"
                                + " stored signature values"}
                            );
                        }
                    }
                }
            }
        }

        //
        // the set holding the stored Signature values must be empty, otherwise we have an error
        //
        if (savedSignatures != null && !savedSignatures.isEmpty()) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                          new Object[] {"Check Signature confirmation: the stored signature values"
                                                        + " list is not empty"}
            );
        }
    }

    protected void decodeUTParameter(RequestData reqData)
        throws WSSecurityException {
        Object mc = reqData.getMsgContext();

        String type = getString(WSHandlerConstants.PASSWORD_TYPE, mc);
        if (type != null) {
            if (WSConstants.PW_TEXT.equals(type)) {
                reqData.setPwType(WSConstants.PASSWORD_TEXT);
            } else if (WSConstants.PW_DIGEST.equals(type)) {
                reqData.setPwType(WSConstants.PASSWORD_DIGEST);
            } else if (WSConstants.PW_NONE.equals(type)) {
                reqData.setPwType(null);
            } else {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                        "empty", new Object[] {"Unknown password type encoding: " + type});
            }
        }

        boolean addNonce =
            decodeBooleanConfigValue(mc, WSHandlerConstants.ADD_USERNAMETOKEN_NONCE, false);
        reqData.setAddUsernameTokenNonce(addNonce);

        boolean addCreated =
            decodeBooleanConfigValue(mc, WSHandlerConstants.ADD_USERNAMETOKEN_CREATED, false);
        reqData.setAddUsernameTokenCreated(addCreated);

        String derivedMAC = getString(WSHandlerConstants.USE_DERIVED_KEY_FOR_MAC, mc);
        boolean useDerivedKeyForMAC = Boolean.parseBoolean(derivedMAC);
        if (useDerivedKeyForMAC) {
            reqData.setUseDerivedKeyForMAC(useDerivedKeyForMAC);
        }

        String iterations = getString(WSHandlerConstants.DERIVED_KEY_ITERATIONS, mc);
        if (iterations != null) {
            try {
                int iIterations = Integer.parseInt(iterations);
                reqData.setDerivedKeyIterations(iIterations);
            } catch (NumberFormatException e) {
                LOG.warn("Error in configuring a derived key iteration count: " + e.getMessage());
            }
        }
    }

    // Convert various Signature configuration into a single SignatureActionToken to be set on
    // the RequestData object
    protected void decodeSignatureParameter(RequestData reqData)
        throws WSSecurityException {
        Object mc = reqData.getMsgContext();
        String signatureUser = getString(WSHandlerConstants.SIGNATURE_USER, mc);

        SignatureActionToken actionToken = reqData.getSignatureToken();
        if (actionToken == null) {
            actionToken = new SignatureActionToken();
            reqData.setSignatureToken(actionToken);
        }

        if (signatureUser != null) {
            actionToken.setUser(signatureUser);
        } else {
            actionToken.setUser(reqData.getUsername());
        }

        String keyId = getString(WSHandlerConstants.SIG_KEY_ID, mc);
        if (keyId != null) {
            Integer id = WSHandlerConstants.getKeyIdentifier(keyId);
            if (id == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                        "empty",
                        new Object[] {"WSHandler: Signature: unknown key identification"}
                );
            }
            int tmp = id;
            if (!(tmp == WSConstants.ISSUER_SERIAL
                    || tmp == WSConstants.ISSUER_SERIAL_QUOTE_FORMAT
                    || tmp == WSConstants.BST_DIRECT_REFERENCE
                    || tmp == WSConstants.X509_KEY_IDENTIFIER
                    || tmp == WSConstants.SKI_KEY_IDENTIFIER
                    || tmp == WSConstants.THUMBPRINT_IDENTIFIER
                    || tmp == WSConstants.ENCRYPTED_KEY_SHA1_IDENTIFIER
                    || tmp == WSConstants.KEY_VALUE)) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                        "empty",
                        new Object[] {"WSHandler: Signature: illegal key identification"}
                );
            }
            actionToken.setKeyIdentifierId(tmp);
        }
        String algo = getString(WSHandlerConstants.SIG_ALGO, mc);
        actionToken.setSignatureAlgorithm(algo);

        String derivedKeyReference = getString(WSHandlerConstants.DERIVED_TOKEN_REFERENCE, mc);
        actionToken.setDerivedKeyTokenReference(derivedKeyReference);

        String derivedKeyIdentifier = getString(WSHandlerConstants.DERIVED_TOKEN_KEY_ID, mc);
        if (derivedKeyIdentifier != null) {
            Integer id = WSHandlerConstants.getKeyIdentifier(derivedKeyIdentifier);
            actionToken.setDerivedKeyIdentifier(id);
        }

        String derivedKeyLength = getString(WSHandlerConstants.DERIVED_SIGNATURE_KEY_LENGTH, mc);
        if (derivedKeyLength != null) {
            try {
                int dKL = Integer.parseInt(derivedKeyLength);
                if (dKL > 0) {
                    actionToken.setDerivedKeyLength(dKL);
                }
            } catch (NumberFormatException e) {
                LOG.warn("Error in configuring a derived key length: " + e.getMessage());
            }
        }

        String digestAlgo = getString(WSHandlerConstants.SIG_DIGEST_ALGO, mc);
        actionToken.setDigestAlgorithm(digestAlgo);

        String c14nAlgo = getString(WSHandlerConstants.SIG_C14N_ALGO, mc);
        actionToken.setC14nAlgorithm(c14nAlgo);

        boolean use200512Namespace =
            decodeBooleanConfigValue(mc, WSHandlerConstants.USE_2005_12_NAMESPACE, true);
        reqData.setUse200512Namespace(use200512Namespace);

        String parts = getString(WSHandlerConstants.SIGNATURE_PARTS, mc);
        if (parts != null) {
            splitEncParts(true, parts, actionToken.getParts(), reqData);
        }
        parts = getString(WSHandlerConstants.OPTIONAL_SIGNATURE_PARTS, mc);
        if (parts != null) {
            splitEncParts(false, parts, actionToken.getParts(), reqData);
        }

        boolean useSingleCert =
            decodeBooleanConfigValue(mc, WSHandlerConstants.USE_SINGLE_CERTIFICATE, true);
        actionToken.setUseSingleCert(useSingleCert);

        boolean includeToken =
            decodeBooleanConfigValue(mc, WSHandlerConstants.INCLUDE_SIGNATURE_TOKEN, false);
        actionToken.setIncludeToken(includeToken);

        if (!reqData.isExpandXopInclude()) {
            boolean expandXOP =
                decodeBooleanConfigValue(
                    reqData.getMsgContext(), WSHandlerConstants.EXPAND_XOP_INCLUDE, false
            );
            reqData.setExpandXopInclude(expandXOP);
        }
    }

    protected void decodeAlgorithmSuite(RequestData reqData) throws WSSecurityException {
        Object mc = reqData.getMsgContext();
        if (mc == null || reqData.getAlgorithmSuite() != null) {
            return;
        }

        AlgorithmSuite algorithmSuite = new AlgorithmSuite();

        String signatureAlgorithm = getString(WSHandlerConstants.SIG_ALGO, mc);
        if (signatureAlgorithm != null && signatureAlgorithm.length() != 0) {
            algorithmSuite.addSignatureMethod(signatureAlgorithm);
        }
        String signatureDigestAlgorithm = getString(WSHandlerConstants.SIG_DIGEST_ALGO, mc);
        if (signatureDigestAlgorithm != null && !signatureDigestAlgorithm.isEmpty()) {
            algorithmSuite.addDigestAlgorithm(signatureDigestAlgorithm);
        }

        String encrAlgorithm = getString(WSHandlerConstants.ENC_SYM_ALGO, mc);
        if (encrAlgorithm != null && !encrAlgorithm.isEmpty()) {
            algorithmSuite.addEncryptionMethod(encrAlgorithm);
        }
        String transportAlgorithm = getString(WSHandlerConstants.ENC_KEY_TRANSPORT, mc);
        if (transportAlgorithm != null && !transportAlgorithm.isEmpty()) {
            algorithmSuite.addKeyWrapAlgorithm(transportAlgorithm);
        }

        String keyAgreementMethodAlgorithm = getString(WSHandlerConstants.ENC_KEY_AGREEMENT_METHOD, mc);
        if (keyAgreementMethodAlgorithm != null && !keyAgreementMethodAlgorithm.isEmpty()) {
            algorithmSuite.addKeyAgreementMethodAlgorithm(keyAgreementMethodAlgorithm);
        }

        String keyDerivationAlgorithm = getString(WSHandlerConstants.ENC_KEY_DERIVATION_FUNCTION, mc);
        if (keyDerivationAlgorithm != null && !keyDerivationAlgorithm.isEmpty()) {
            algorithmSuite.addDerivedKeyAlgorithm(keyDerivationAlgorithm);
        }

        reqData.setAlgorithmSuite(algorithmSuite);
    }

    // Convert various Encryption configuration into a single EncryptionActionToken to be set on
    // the RequestData object
    protected void decodeEncryptionParameter(RequestData reqData)
        throws WSSecurityException {
        Object mc = reqData.getMsgContext();

        EncryptionActionToken actionToken = reqData.getEncryptionToken();
        if (actionToken == null) {
            actionToken = new EncryptionActionToken();
            reqData.setEncryptionToken(actionToken);
        }
        //
        // If the following parameters are no used (they return null) then the
        // default values of WSS4J are used.
        //
        String encKeyId = getString(WSHandlerConstants.ENC_KEY_ID, mc);
        if (encKeyId != null) {
            Integer id = WSHandlerConstants.getKeyIdentifier(encKeyId);
            if (id == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                        "empty",
                        new Object[] {"WSHandler: Encryption: unknown key identification"}
                );
            }
            int tmp = id;
            actionToken.setKeyIdentifierId(tmp);
            if (!(tmp == WSConstants.ISSUER_SERIAL
                    || tmp == WSConstants.ISSUER_SERIAL_QUOTE_FORMAT
                    || tmp == WSConstants.X509_KEY_IDENTIFIER
                    || tmp == WSConstants.SKI_KEY_IDENTIFIER
                    || tmp == WSConstants.BST_DIRECT_REFERENCE
                    || tmp == WSConstants.THUMBPRINT_IDENTIFIER
                    || tmp == WSConstants.ENCRYPTED_KEY_SHA1_IDENTIFIER)) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                        "empty",
                        new Object[] {"WSHandler: Encryption: illegal key identification"}
                );
            }
        }
        String encSymAlgo = getString(WSHandlerConstants.ENC_SYM_ALGO, mc);
        actionToken.setSymmetricAlgorithm(encSymAlgo);

        String encKeyTransport =
            getString(WSHandlerConstants.ENC_KEY_TRANSPORT, mc);
        actionToken.setKeyTransportAlgorithm(encKeyTransport);

        String encKeyAgreementMethod =
                getString(WSHandlerConstants.ENC_KEY_AGREEMENT_METHOD, mc);
        actionToken.setKeyAgreementMethodAlgorithm(encKeyAgreementMethod);

        String encKeyDerivationAlgorithm =
                getString(WSHandlerConstants.ENC_KEY_DERIVATION_FUNCTION, mc);
        actionToken.setKeyDerivationFunction(encKeyDerivationAlgorithm);

        Object obj = getProperty(mc, WSHandlerConstants.ENC_KEY_DERIVATION_PARAMS);
        if (obj instanceof KeyDerivationParameters) {
            actionToken.setKeyDerivationParameters((KeyDerivationParameters)obj);
        }

        String derivedKeyReference = getString(WSHandlerConstants.DERIVED_TOKEN_REFERENCE, mc);
        actionToken.setDerivedKeyTokenReference(derivedKeyReference);

        String derivedKeyIdentifier = getString(WSHandlerConstants.DERIVED_TOKEN_KEY_ID, mc);
        if (derivedKeyIdentifier != null) {
            Integer id = WSHandlerConstants.getKeyIdentifier(derivedKeyIdentifier);
            actionToken.setDerivedKeyIdentifier(id);
        }

        String derivedKeyLength = getString(WSHandlerConstants.DERIVED_ENCRYPTION_KEY_LENGTH, mc);
        if (derivedKeyLength != null) {
            try {
                int dKL = Integer.parseInt(derivedKeyLength);
                if (dKL > 0) {
                    actionToken.setDerivedKeyLength(dKL);
                }
            } catch (NumberFormatException e) {
                LOG.warn("Error in configuring a derived key length: " + e.getMessage());
            }
        }

        boolean use200512Namespace =
            decodeBooleanConfigValue(mc, WSHandlerConstants.USE_2005_12_NAMESPACE, true);
        reqData.setUse200512Namespace(use200512Namespace);

        boolean getSecretKeyFromCallbackHandler =
            decodeBooleanConfigValue(mc, WSHandlerConstants.GET_SECRET_KEY_FROM_CALLBACK_HANDLER, false);
        actionToken.setGetSymmetricKeyFromCallbackHandler(getSecretKeyFromCallbackHandler);

        String digestAlgo = getString(WSHandlerConstants.ENC_DIGEST_ALGO, mc);
        actionToken.setDigestAlgorithm(digestAlgo);

        String mgfAlgo = getString(WSHandlerConstants.ENC_MGF_ALGO, mc);
        actionToken.setMgfAlgorithm(mgfAlgo);

        String encSymEncKey = getString(WSHandlerConstants.ENC_SYM_ENC_KEY, mc);
        if (encSymEncKey != null) {
            boolean encSymEndKeyBoolean = Boolean.parseBoolean(encSymEncKey);
            actionToken.setEncSymmetricEncryptionKey(encSymEndKeyBoolean);
        }

        String encUser = getString(WSHandlerConstants.ENCRYPTION_USER, mc);
        if (encUser != null) {
            actionToken.setUser(encUser);
        } else {
            actionToken.setUser(reqData.getUsername());
        }
        if (actionToken.isEncSymmetricEncryptionKey() && actionToken.getUser() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                    "empty",
                    new Object[] {"WSHandler: Encryption: no username"});
        }

        handleSpecialUser(reqData);

        String encParts = getString(WSHandlerConstants.ENCRYPTION_PARTS, mc);
        if (encParts != null) {
            splitEncParts(true, encParts, actionToken.getParts(), reqData);
        }
        encParts = getString(WSHandlerConstants.OPTIONAL_ENCRYPTION_PARTS, mc);
        if (encParts != null) {
            splitEncParts(false, encParts, actionToken.getParts(), reqData);
        }

        boolean includeToken =
            decodeBooleanConfigValue(mc, WSHandlerConstants.INCLUDE_ENCRYPTION_TOKEN, false);
        actionToken.setIncludeToken(includeToken);
    }

    /**
     * Decode the TimeToLive parameter for either a Timestamp or a UsernameToken Created element,
     * depending on the boolean argument
     */
    public int decodeTimeToLive(RequestData reqData, boolean timestamp) {
        String tag = WSHandlerConstants.TTL_TIMESTAMP;
        if (!timestamp) {
            tag = WSHandlerConstants.TTL_USERNAMETOKEN;
        }
        String ttl = getString(tag, reqData.getMsgContext());
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

    /**
     * Decode the FutureTimeToLive parameter for either a Timestamp or a UsernameToken Created
     * element, depending on the boolean argument
     */
    protected int decodeFutureTimeToLive(RequestData reqData, boolean timestamp) {
        String tag = WSHandlerConstants.TTL_FUTURE_TIMESTAMP;
        if (!timestamp) {
            tag = WSHandlerConstants.TTL_FUTURE_USERNAMETOKEN;
        }
        String ttl = getString(tag, reqData.getMsgContext());
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

    protected String decodePasswordType(RequestData reqData) throws WSSecurityException {
        String type = getString(WSHandlerConstants.PASSWORD_TYPE, reqData.getMsgContext());
        if (type != null) {
            if (WSConstants.PW_TEXT.equals(type)) {
                return WSConstants.PASSWORD_TEXT;
            } else if (WSConstants.PW_DIGEST.equals(type)) {
                return WSConstants.PASSWORD_DIGEST;
            }
        }
        return null;
    }

    protected boolean decodeBooleanConfigValue(
        Object messageContext, String configTag, boolean defaultToTrue
    ) throws WSSecurityException {

        String value = getString(configTag, messageContext);

        if (value == null) {
            return defaultToTrue;
        }
        if ("0".equals(value) || "false".equals(value)) {
            return false;
        }
        if ("1".equals(value) || "true".equals(value)) {
            return true;
        }

        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                "empty",
                new Object[] {"WSHandler: illegal " + configTag + " parameter"}
        );
    }

    /**
     * Hook to allow subclasses to load their Signature creation Crypto however they see
     * fit.
     *
     * @param requestData the RequestData object
     * @return a Crypto instance to use for Signature creation
     */
    public Crypto loadSignatureCrypto(RequestData requestData) throws WSSecurityException {
        return
            loadCrypto(
                WSHandlerConstants.SIG_PROP_FILE,
                WSHandlerConstants.SIG_PROP_REF_ID,
                requestData
            );
    }

    /**
     * Hook to allow subclasses to load their Signature verification Crypto however they see
     * fit.
     *
     * @param requestData the RequestData object
     * @return a Crypto instance to use for Signature verification
     */
    public Crypto loadSignatureVerificationCrypto(RequestData requestData)
        throws WSSecurityException {
        return
            loadCrypto(
                WSHandlerConstants.SIG_VER_PROP_FILE,
                WSHandlerConstants.SIG_VER_PROP_REF_ID,
                requestData
            );
    }

    /**
     * Hook to allow subclasses to load their Decryption Crypto however they see
     * fit.
     *
     * @param requestData the RequestData object
     * @return a Crypto instance to use for Decryption creation/verification
     */
    protected Crypto loadDecryptionCrypto(RequestData requestData) throws WSSecurityException {
        return
            loadCrypto(
                WSHandlerConstants.DEC_PROP_FILE,
                WSHandlerConstants.DEC_PROP_REF_ID,
                requestData
            );
    }

    /**
     * Hook to allow subclasses to load their Encryption Crypto however they see
     * fit.
     *
     * @param requestData the RequestData object
     * @return a Crypto instance to use for Encryption creation/verification
     */
    protected Crypto loadEncryptionCrypto(RequestData requestData) throws WSSecurityException {
        return
            loadCrypto(
                WSHandlerConstants.ENC_PROP_FILE,
                WSHandlerConstants.ENC_PROP_REF_ID,
                requestData
            );
    }

    /**
     * Load a Crypto instance. Firstly, it tries to use the cryptoPropertyRefId tag to retrieve
     * a Crypto object via a custom reference Id. Failing this, it tries to load the crypto
     * instance via the cryptoPropertyFile tag.
     *
     * @param requestData the RequestData object
     * @return a Crypto instance to use for Encryption creation/verification
     */
    protected Crypto loadCrypto(
        String cryptoPropertyFile,
        String cryptoPropertyRefId,
        RequestData requestData
    ) throws WSSecurityException {
        Object mc = requestData.getMsgContext();
        Crypto crypto = null;

        //
        // Try the Property Ref Id first
        //
        String refId = getString(cryptoPropertyRefId, mc);
        if (refId != null) {
            crypto = cryptos.get(refId);
            if (crypto == null) {
                Object obj = getProperty(mc, refId);
                if (obj instanceof Properties) {
                    crypto = CryptoFactory.getInstance((Properties)obj,
                                                       Loader.getClassLoader(CryptoFactory.class),
                                                       getPasswordEncryptor(requestData));
                    cryptos.put(refId, crypto);
                } else if (obj instanceof Crypto) {
                    // No need to cache this as it's already loaded
                    crypto = (Crypto)obj;
                }
            }
            if (crypto == null) {
                LOG.warn("The Crypto reference " + refId + " specified by "
                    + cryptoPropertyRefId + " could not be loaded"
                );
            }
        }

        //
        // Now try loading the properties file
        //
        if (crypto == null) {
            String propFile = getString(cryptoPropertyFile, mc);
            if (propFile != null) {
                crypto = cryptos.get(propFile);
                if (crypto == null) {
                    crypto = loadCryptoFromPropertiesFile(propFile, requestData);
                    cryptos.put(propFile, crypto);
                }
                if (crypto == null) {
                    LOG.warn(
                         "The Crypto properties file " + propFile + " specified by "
                         + cryptoPropertyFile + " could not be loaded or found"
                    );
                }
            }
        }

        return crypto;
    }

    /**
     * A hook to allow subclass to load Crypto instances from property files in a different
     * way.
     * @param propFilename The property file name
     * @param reqData The RequestData object
     * @return A Crypto instance that has been loaded
     */
    protected Crypto loadCryptoFromPropertiesFile(
        String propFilename,
        RequestData reqData
    ) throws WSSecurityException {
        ClassLoader classLoader = this.getClassLoader();
        Properties properties = CryptoFactory.getProperties(propFilename, classLoader);
        return
            CryptoFactory.getInstance(
                properties, classLoader, getPasswordEncryptor(reqData)
            );
    }

    /**
     * Get a CallbackHandler instance. First try to get an instance via the
     * callbackHandlerRef on the message context. Failing that, try to load a new
     * instance of the CallbackHandler via the callbackHandlerClass argument.
     *
     * @param callbackHandlerClass The class name of the CallbackHandler instance
     * @param callbackHandlerRef The reference name of the CallbackHandler instance
     * @param requestData The RequestData which supplies the message context
     * @return a CallbackHandler instance
     * @throws WSSecurityException
     */
    public CallbackHandler getCallbackHandler(
        String callbackHandlerClass,
        String callbackHandlerRef,
        RequestData requestData
    ) throws WSSecurityException {
        Object mc = requestData.getMsgContext();
        CallbackHandler cbHandler = (CallbackHandler) getOption(callbackHandlerRef);
        if (cbHandler == null) {
            cbHandler = (CallbackHandler) getProperty(mc, callbackHandlerRef);
        }
        if (cbHandler == null) {
            String callback = getString(callbackHandlerClass, mc);
            if (callback != null) {
                cbHandler = loadCallbackHandler(callback);
            }
        }
        return cbHandler;
    }

    /**
     * Get a CallbackHandler instance to obtain passwords.
     * @param reqData The RequestData which supplies the message context
     * @return the CallbackHandler instance to obtain passwords.
     * @throws WSSecurityException
     */
    public CallbackHandler getPasswordCallbackHandler(RequestData reqData)
        throws WSSecurityException {
        return
            getCallbackHandler(
                WSHandlerConstants.PW_CALLBACK_CLASS,
                WSHandlerConstants.PW_CALLBACK_REF,
                reqData
            );
    }

    /**
     * Load a CallbackHandler instance.
     * @param callbackHandlerClass The class name of the CallbackHandler instance
     * @return a CallbackHandler instance
     * @throws WSSecurityException
     */
    private CallbackHandler loadCallbackHandler(
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
                    + callbackHandlerClass
                    }
            );
        }
        return cbHandler;
    }

    protected PasswordEncryptor getPasswordEncryptor(RequestData requestData) {
        PasswordEncryptor passwordEncryptor = requestData.getPasswordEncryptor();
        if (passwordEncryptor == null) {
            Object o = getOption(WSHandlerConstants.PASSWORD_ENCRYPTOR_INSTANCE);
            if (o instanceof PasswordEncryptor) {
                passwordEncryptor = (PasswordEncryptor) o;
            }
        }
        if (passwordEncryptor == null) {
            Object mc = requestData.getMsgContext();
            Object o = getProperty(mc, WSHandlerConstants.PASSWORD_ENCRYPTOR_INSTANCE);
            if (o instanceof PasswordEncryptor) {
                passwordEncryptor = (PasswordEncryptor) o;
            }
        }
        if (passwordEncryptor == null) {
            CallbackHandler callbackHandler = requestData.getCallbackHandler();
            if (callbackHandler != null) {
                passwordEncryptor = new JasyptPasswordEncryptor(callbackHandler);
            }
        }

        return passwordEncryptor;
    }

    /**
     * Get a password callback (WSPasswordCallback object) from a CallbackHandler instance
     * @param username The username to supply to the CallbackHandler
     * @param doAction The action to perform
     * @param callbackHandler The CallbackHandler instance
     * @param requestData The RequestData which supplies the message context
     * @return the WSPasswordCallback object containing the password
     * @throws WSSecurityException
     */
    public WSPasswordCallback getPasswordCB(
         String username,
         int doAction,
         CallbackHandler callbackHandler,
         RequestData requestData
    ) throws WSSecurityException {

        if (callbackHandler != null) {
            return performPasswordCallback(callbackHandler, username, doAction);
        } else {
            //
            // If a callback isn't configured then try to get the password
            // from the message context
            //
            String password = getPassword(requestData.getMsgContext());
            if (password == null) {
                String err = "provided null or empty password";
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                        "empty",
                        new Object[] {"WSHandler: application " + err});
            }
            WSPasswordCallback pwCb = constructPasswordCallback(username, doAction);
            pwCb.setPassword(password);
            return pwCb;
        }
    }

    /**
     * Perform a callback on a CallbackHandler instance
     * @param cbHandler the CallbackHandler instance
     * @param username The username to supply to the CallbackHandler
     * @param doAction The action to perform
     * @return a WSPasswordCallback instance
     * @throws WSSecurityException
     */
    private WSPasswordCallback performPasswordCallback(
        CallbackHandler cbHandler,
        String username,
        int doAction
    ) throws WSSecurityException {

        WSPasswordCallback pwCb = constructPasswordCallback(username, doAction);
        Callback[] callbacks = new Callback[1];
        callbacks[0] = pwCb;
        //
        // Call back the application to get the password
        //
        try {
            cbHandler.handle(callbacks);
        } catch (Exception e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e,
                    "empty", new Object[] {"WSHandler: password callback failed"});
        }
        return pwCb;
    }

    /**
     * Construct a WSPasswordCallback instance
     * @param username The username
     * @param doAction The action to perform
     * @return a WSPasswordCallback instance
     * @throws WSSecurityException
     */
    private WSPasswordCallback constructPasswordCallback(
        String username,
        int doAction
    ) throws WSSecurityException {

        int reason;

        switch (doAction) {
        case WSConstants.UT:
        case WSConstants.UT_SIGN:
            reason = WSPasswordCallback.USERNAME_TOKEN;
            break;
        case WSConstants.SIGN:
            reason = WSPasswordCallback.SIGNATURE;
            break;
        case WSConstants.DKT_SIGN:
            reason = WSPasswordCallback.SECRET_KEY;
            break;
        case WSConstants.ENCR:
            reason = WSPasswordCallback.SECRET_KEY;
            break;
        case WSConstants.DKT_ENCR:
            reason = WSPasswordCallback.SECRET_KEY;
            break;
        default:
            reason = WSPasswordCallback.UNKNOWN;
            break;
        }
        return new WSPasswordCallback(username, reason);
    }

    private void splitEncParts(boolean required, String tmpS,
                               List<WSEncryptionPart> parts, RequestData reqData)
        throws WSSecurityException {
        WSEncryptionPart encPart = null;
        String[] rawParts = tmpS.split(";");

        for (String rawPart : rawParts) {
            String[] partDef = rawPart.split("}");

            if (partDef.length == 1) {
                LOG.debug("single partDef: '{}'", partDef[0]);
                encPart =
                    new WSEncryptionPart(partDef[0].trim(),
                            reqData.getSoapConstants().getEnvelopeURI(),
                            "Content");
            } else if (partDef.length == 2) {
                String mode = partDef[0].trim().substring(1);
                String element = partDef[1].trim();
                encPart = new WSEncryptionPart(element, mode);
            } else if (partDef.length == 3) {
                String mode = partDef[0].trim();
                if (mode.length() <= 1) {
                    mode = "Content";
                } else {
                    mode = mode.substring(1);
                }
                String nmSpace = partDef[1].trim();
                if (nmSpace.length() <= 1) {
                    nmSpace = reqData.getSoapConstants().getEnvelopeURI();
                } else {
                    nmSpace = nmSpace.substring(1);
                    if (nmSpace.equals(WSConstants.NULL_NS)) {
                        nmSpace = null;
                    }
                }
                String element = partDef[2].trim();
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                        "partDefs: '" + mode + "' ,'" + nmSpace + "' ,'" + element + "'"
                    );
                }
                encPart = new WSEncryptionPart(element, nmSpace, mode);
            } else {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                        "empty",
                        new Object[] {"WSHandler: wrong part definition: " + tmpS});
            }
            encPart.setRequired(required);
            parts.add(encPart);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleSpecialUser(RequestData reqData) {
        EncryptionActionToken actionToken = reqData.getEncryptionToken();
        if (actionToken == null
            || !WSHandlerConstants.USE_REQ_SIG_CERT.equals(actionToken.getUser())) {
            return;
        }
        List<WSHandlerResult> results =
            (List<WSHandlerResult>) getProperty(
                reqData.getMsgContext(), WSHandlerConstants.RECV_RESULTS
            );
        if (results == null) {
            return;
        }
        /*
         * Scan the results for a matching actor. Use results only if the
         * receiving Actor and the sending Actor match.
         */
        for (WSHandlerResult rResult : results) {
            String hActor = rResult.getActor();
            if (!WSSecurityUtil.isActorEqual(reqData.getActor(), hActor)) {
                continue;
            }
            List<WSSecurityEngineResult> wsSecEngineResults = rResult.getResults();
            /*
             * Scan the results for the first Signature action. Use the
             * certificate of this Signature to set the certificate for the
             * encryption action :-).
             */
            for (WSSecurityEngineResult wser : wsSecEngineResults) {
                Integer wserAction = (Integer) wser.get(WSSecurityEngineResult.TAG_ACTION);
                if (wserAction != null && wserAction.intValue() == WSConstants.SIGN) {
                    X509Certificate cert =
                        (X509Certificate)wser.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
                    actionToken.setCertificate(cert);
                    return;
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    protected void decodeSignatureParameter2(RequestData reqData)
        throws WSSecurityException {
        if (reqData.getSigVerCrypto() == null) {
            reqData.setSigVerCrypto(loadSignatureVerificationCrypto(reqData));
        }
        if (reqData.getSigVerCrypto() == null) {
            reqData.setSigVerCrypto(loadSignatureCrypto(reqData));
        }
        boolean enableRevocation =
            decodeBooleanConfigValue(
                reqData.getMsgContext(), WSHandlerConstants.ENABLE_REVOCATION, false
            );
        reqData.setEnableRevocation(enableRevocation);

        String certConstraints =
            getString(WSHandlerConstants.SIG_SUBJECT_CERT_CONSTRAINTS, reqData.getMsgContext());
        if (certConstraints != null) {
            String certConstraintsSeparator =
                getString(WSHandlerConstants.SIG_CERT_CONSTRAINTS_SEPARATOR, reqData.getMsgContext());
            if (certConstraintsSeparator == null || certConstraintsSeparator.isEmpty()) {
                certConstraintsSeparator = ",";
            }
            Collection<Pattern> subjectCertConstraints = getCertConstraints(certConstraints, certConstraintsSeparator);
            reqData.setSubjectCertConstraints(subjectCertConstraints);
        }
        String issuerCertConstraintsStringValue =
            getString(WSHandlerConstants.SIG_ISSUER_CERT_CONSTRAINTS, reqData.getMsgContext());
        if (issuerCertConstraintsStringValue != null) {
            String certConstraintsSeparator =
                getString(WSHandlerConstants.SIG_CERT_CONSTRAINTS_SEPARATOR, reqData.getMsgContext());
            if (certConstraintsSeparator == null || certConstraintsSeparator.isEmpty()) {
                certConstraintsSeparator = ",";
            }
            Collection<Pattern> issuerCertConstraints =
                getCertConstraints(issuerCertConstraintsStringValue, certConstraintsSeparator);
            reqData.setIssuerDNPatterns(issuerCertConstraints);
        }

        String value = getString(WSHandlerConstants.EXPAND_XOP_INCLUDE_FOR_SIGNATURE, reqData.getMsgContext());
        boolean expandXOP = false;
        if (value != null) {
            expandXOP =
                decodeBooleanConfigValue(
                    reqData.getMsgContext(), WSHandlerConstants.EXPAND_XOP_INCLUDE_FOR_SIGNATURE, true
                );
        } else {
            expandXOP =
                decodeBooleanConfigValue(
                    reqData.getMsgContext(), WSHandlerConstants.EXPAND_XOP_INCLUDE, true
            );
        }
        reqData.setExpandXopInclude(expandXOP);
    }

    private Collection<Pattern> getCertConstraints(String certConstraints, String separator) throws WSSecurityException {
        String[] certConstraintsList = certConstraints.split(separator);
        if (certConstraintsList != null && certConstraintsList.length > 0) {
            Collection<Pattern> certConstraintsCollection =
                new ArrayList<>(certConstraintsList.length);
            for (String certConstraint : certConstraintsList) {
                try {
                    certConstraintsCollection.add(Pattern.compile(certConstraint.trim()));
                } catch (PatternSyntaxException ex) {
                    LOG.debug(ex.getMessage(), ex);
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
                }
            }

            return certConstraintsCollection;
        }
        return Collections.emptyList();
    }

    /*
     * Set and check the decryption specific parameters, if necessary
     * take over signature crypto instance.
     */
    protected void decodeDecryptionParameter(RequestData reqData)
        throws WSSecurityException {
        if (reqData.getDecCrypto() == null) {
            reqData.setDecCrypto(loadDecryptionCrypto(reqData));
        }

        boolean allowRsa15 =
            decodeBooleanConfigValue(
                reqData.getMsgContext(), WSHandlerConstants.ALLOW_RSA15_KEY_TRANSPORT_ALGORITHM,
                false
            );
        reqData.setAllowRSA15KeyTransportAlgorithm(allowRsa15);
    }

    /**
     * Looks up key first via {@link #getOption(String)} and if not found
     * there, via {@link #getProperty(Object, String)}
     *
     * @param key the key to search for. May not be null.
     * @param mc the message context to search.
     * @return the value found.
     * @throws IllegalArgumentException if <code>key</code> is null.
     */
    public String getString(String key, Object mc) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        String s = getStringOption(key);
        if (s != null) {
            return s;
        }
        if (mc == null) {
            throw new IllegalArgumentException("Message context cannot be null");
        }
        return (String) getProperty(mc, key);
    }


    /**
     * Returns the option on <code>name</code>.
     *
     * @param key the non-null key of the option.
     * @return the option on <code>key</code> if <code>key</code>
     *  exists and is of type java.lang.String; otherwise null.
     */
    public String getStringOption(String key) {
        Object o = getOption(key);
        if (o instanceof String) {
            return (String) o;
        } else {
            return null;
        }
    }

    /**
     * Returns the classloader to be used for loading the callback class
     * @return class loader
     */
    public ClassLoader getClassLoader() {
        try {
            return Loader.getTCL();
        } catch (Exception ex) {
            return null;
        }
    }

    public abstract Object getOption(String key);
    public abstract Object getProperty(Object msgContext, String key);

    public abstract void setProperty(Object msgContext, String key,
            Object value);


    public abstract String getPassword(Object msgContext);

    public abstract void setPassword(Object msgContext, String password);
}
