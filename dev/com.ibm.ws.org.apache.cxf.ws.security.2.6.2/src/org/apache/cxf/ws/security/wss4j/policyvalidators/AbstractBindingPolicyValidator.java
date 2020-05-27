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

package org.apache.cxf.ws.security.wss4j.policyvalidators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.EncryptionToken;
import org.apache.cxf.ws.security.policy.model.Layout;
import org.apache.cxf.ws.security.policy.model.ProtectionToken;
import org.apache.cxf.ws.security.policy.model.SignatureToken;
import org.apache.cxf.ws.security.policy.model.SymmetricAsymmetricBindingBase;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.cxf.ws.security.policy.model.TokenWrapper;
import org.apache.cxf.ws.security.policy.model.UsernameToken;
import org.apache.cxf.ws.security.policy.model.X509Token;
import org.apache.neethi.Assertion;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.message.token.Timestamp;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * Some abstract functionality for validating a security binding.
 */
public abstract class AbstractBindingPolicyValidator implements BindingPolicyValidator {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractBindingPolicyValidator.class);
    private static final QName SIG_QNAME = new QName(WSConstants.SIG_NS, WSConstants.SIG_LN);
    
    /**
     * Validate a Timestamp
     * @param includeTimestamp whether a Timestamp must be included or not
     * @param transportBinding whether the Transport binding is in use or not
     * @param signedResults the signed results list
     * @param message the Message object
     * @return whether the Timestamp policy is valid or not
     */
    protected boolean validateTimestamp(
        boolean includeTimestamp,
        boolean transportBinding,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults,
        Message message
    ) {
        List<WSSecurityEngineResult> timestampResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.TS, timestampResults);
        
        // Check whether we received a timestamp and compare it to the policy
        if (includeTimestamp && timestampResults.size() != 1) {
            return false;
        } else if (!includeTimestamp) {
            if (timestampResults.isEmpty()) {
                return true;
            }
            return false;
        }
        
        // At this point we received a (required) Timestamp. Now check that it is integrity protected.
        if (transportBinding) {
            return true;
        } else if (!signedResults.isEmpty()) {
            Timestamp timestamp = 
                (Timestamp)timestampResults.get(0).get(WSSecurityEngineResult.TAG_TIMESTAMP);
            for (WSSecurityEngineResult signedResult : signedResults) {
                List<WSDataRef> dataRefs = 
                    CastUtils.cast((List<?>)signedResult.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
                for (WSDataRef dataRef : dataRefs) {
                    if (timestamp.getElement() == dataRef.getProtectedElement()) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Validate the entire header and body signature property.
     */
    protected boolean validateEntireHeaderAndBodySignatures(
        List<WSSecurityEngineResult> signedResults
    ) {
        for (WSSecurityEngineResult signedResult : signedResults) {
            List<WSDataRef> dataRefs = 
                    CastUtils.cast((List<?>)signedResult.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
            for (WSDataRef dataRef : dataRefs) {
                String xpath = dataRef.getXpath();
                if (xpath != null) {
                    String[] nodes = xpath.split("/");
                    // envelope/Body || envelope/Header/header || envelope/Header/wsse:Security/header
                    if (nodes.length == 5 && nodes[3].contains("Security")) {
                        continue;
                    } else if (nodes.length < 3 || nodes.length > 4) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Validate the layout assertion. It just checks the LaxTsFirst and LaxTsLast properties
     */
    protected boolean validateLayout(
        boolean laxTimestampFirst,
        boolean laxTimestampLast,
        List<WSSecurityEngineResult> results
    ) {
        if (laxTimestampFirst) {
            if (results.isEmpty()) {
                return false;
            }
            Integer firstAction = (Integer)results.get(0).get(WSSecurityEngineResult.TAG_ACTION);
            if (firstAction.intValue() != WSConstants.TS) {
                return false;
            }
        } else if (laxTimestampLast) {
            if (results.isEmpty()) {
                return false;
            }
            Integer lastAction = 
                (Integer)results.get(results.size() - 1).get(WSSecurityEngineResult.TAG_ACTION);
            if (lastAction.intValue() != WSConstants.TS) {
                return false;
            }
        }
        return true;
        
    }
    
    /**
     * Check various properties set in the policy of the binding
     */
    protected boolean checkProperties(
        SymmetricAsymmetricBindingBase binding, 
        AssertionInfo ai,
        AssertionInfoMap aim,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults,
        Message message
    ) {
        // Check the AlgorithmSuite
        AlgorithmSuitePolicyValidator algorithmValidator = new AlgorithmSuitePolicyValidator(results);
        if (!algorithmValidator.validatePolicy(ai, binding.getAlgorithmSuite())) {
            return false;
        }
        
        // Check the IncludeTimestamp
        if (!validateTimestamp(binding.isIncludeTimestamp(), false, results, signedResults, message)) {
            String error = "Received Timestamp does not match the requirements";
            notAssertPolicy(aim, SP12Constants.INCLUDE_TIMESTAMP, error);
            ai.setNotAsserted(error);
            return false;
        }
        assertPolicy(aim, SP12Constants.INCLUDE_TIMESTAMP);
        
        // Check the Layout
        Layout layout = binding.getLayout();
        boolean timestampFirst = layout.getValue() == SPConstants.Layout.LaxTimestampFirst;
        boolean timestampLast = layout.getValue() == SPConstants.Layout.LaxTimestampLast;
        if (!validateLayout(timestampFirst, timestampLast, results)) {
            String error = "Layout does not match the requirements";
            notAssertPolicy(aim, SP12Constants.LAYOUT, error);
            ai.setNotAsserted(error);
            return false;
        }
        assertPolicy(aim, SP12Constants.LAYOUT);
        
        // Check the EntireHeaderAndBodySignatures property
        if (binding.isEntireHeadersAndBodySignatures()
            && !validateEntireHeaderAndBodySignatures(signedResults)) {
            String error = "OnlySignEntireHeadersAndBody does not match the requirements";
            ai.setNotAsserted(error);
            return false;
        }
        
        // Check whether the signatures were encrypted or not
        if (binding.isSignatureProtection() && !isSignatureEncrypted(results)) {
            ai.setNotAsserted("The signature is not protected");
            return false;
        }
        
        return true;
    }
    
    /**
     * Check the Protection Order of the binding
     */
    protected boolean checkProtectionOrder(
        SymmetricAsymmetricBindingBase binding, 
        AssertionInfo ai,
        List<WSSecurityEngineResult> results
    ) {
        if (binding.getProtectionOrder() == SPConstants.ProtectionOrder.EncryptBeforeSigning) {
            if (!binding.isSignatureProtection() && isSignedBeforeEncrypted(results)) {
                ai.setNotAsserted("Not encrypted before signed");
                return false;
            }
        } else if (isEncryptedBeforeSigned(results)) {
            ai.setNotAsserted("Not signed before encrypted");
            return false;
        }
        return true;
    }
    
    /**
     * Check to see if a signature was applied before encryption.
     * Note that results are stored in the reverse order.
     */
    private boolean isSignedBeforeEncrypted(List<WSSecurityEngineResult> results) {
        boolean signed = false;
        for (WSSecurityEngineResult result : results) {
            Integer actInt = (Integer)result.get(WSSecurityEngineResult.TAG_ACTION);
            List<WSDataRef> el = 
                CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
            
            // Don't count an endorsing signature
            if (actInt.intValue() == WSConstants.SIGN && el != null
                && !(el.size() == 1 && el.get(0).getName().equals(SIG_QNAME))) {
                signed = true;
            }
            if (actInt.intValue() == WSConstants.ENCR && el != null) {
                if (signed) {
                    return true;
                }
                return false;
            }
        }
        return false;
    }
    
    /**
     * Check to see if encryption was applied before signature.
     * Note that results are stored in the reverse order.
     */
    private boolean isEncryptedBeforeSigned(List<WSSecurityEngineResult> results) {
        boolean encrypted = false;
        for (WSSecurityEngineResult result : results) {
            Integer actInt = (Integer)result.get(WSSecurityEngineResult.TAG_ACTION);
            List<WSDataRef> el = 
                CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
            
            if (actInt.intValue() == WSConstants.ENCR && el != null) {
                encrypted = true;
            }
            // Don't count an endorsing signature
            if (actInt.intValue() == WSConstants.SIGN && el != null
                && !(el.size() == 1 && el.get(0).getName().equals(SIG_QNAME))) {
                if (encrypted) {
                    return true;
                }
                return false;
            }
        }
        return false;
    }
    
    /**
     * Check the derived key requirement.
     */
    protected boolean checkDerivedKeys(
        TokenWrapper tokenWrapper, 
        boolean hasDerivedKeys,
        List<WSSecurityEngineResult> signedResults,
        List<WSSecurityEngineResult> encryptedResults
    ) {
        Token token = tokenWrapper.getToken();
        //Policy specifies RequireDerivedKeys, but the message does not have DerivedKeyToken
        LOG.log(Level.FINE, "checkDerivedKeys, token requires derived keys = "
                + token.isDerivedKeys() + ", message has derived keys = " + hasDerivedKeys);
        // If derived keys are not required then just return
        if (!((token instanceof X509Token || token instanceof UsernameToken) 
            && token.isDerivedKeys())) {
            return true;
        }
        // derived keys are required but if the message does not have them, 
        // then check to see whether the encrypted results and/or signed results are
        // empty
        if (tokenWrapper instanceof EncryptionToken 
            && !hasDerivedKeys && !encryptedResults.isEmpty()) {
            return false;
        } else if (tokenWrapper instanceof SignatureToken
            && !hasDerivedKeys && !signedResults.isEmpty()) {
            return false;
        } else if (tokenWrapper instanceof ProtectionToken
            && !hasDerivedKeys && !(signedResults.isEmpty() && encryptedResults.isEmpty())) {
            return false;
        }
        return true;
    }
    
    /**
     * Check whether the primary Signature (and all SignatureConfirmation) elements were encrypted
     */
    protected boolean isSignatureEncrypted(List<WSSecurityEngineResult> results) {
        boolean foundPrimarySignature = false;
        for (int i = results.size() - 1; i >= 0; i--) {
            WSSecurityEngineResult result = results.get(i);
            Integer actInt = (Integer)result.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.SIGN && !foundPrimarySignature) {
                foundPrimarySignature = true;
                String sigId = (String)result.get(WSSecurityEngineResult.TAG_ID);
                
                Boolean isSigEncrypted = false;                
                // Is ID does not exist for the <ds:Signature> elem, check if Signature elem was encrypted, 
                // do not attempt to match the IDs.
                if (sigId == null || sigId.length() <= 0) {
                    isSigEncrypted = isSigElemEncrypted(results);
                } else {
                    isSigEncrypted = isIdEncrypted(sigId, results);
                }
                
                if (!isSigEncrypted) {
                    return false;
                }
            } else if (actInt.intValue() == WSConstants.SC) {
                String sigId = (String)result.get(WSSecurityEngineResult.TAG_ID);
                if (sigId == null || !isIdEncrypted(sigId, results)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Return true if the given id was encrypted
     */
    private boolean isIdEncrypted(String sigId, List<WSSecurityEngineResult> results) {
        for (WSSecurityEngineResult wser : results) {
            Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.ENCR) {
                List<WSDataRef> el = 
                    CastUtils.cast((List<?>)wser.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
                if (el != null) {
                    for (WSDataRef r : el) {
                        Element protectedElement = r.getProtectedElement();
                        if (protectedElement != null) {
                            String id = protectedElement.getAttribute("Id");
                            String wsuId = protectedElement.getAttributeNS(WSConstants.WSU_NS, "Id");
                            if (sigId.equals(id) || sigId.equals(wsuId)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Returns true if the Signature element was encrypted
     */
    private boolean isSigElemEncrypted(List<WSSecurityEngineResult> results) {

        for (WSSecurityEngineResult wser : results) {
            Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.ENCR) {
                List<WSDataRef> el = 
                    CastUtils.cast((List<?>)wser.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
                if (el != null) {
                    for (WSDataRef r : el) {
                        Element protectedElement = r.getProtectedElement();
                                                
                        if (protectedElement != null) {
                            String elemName = protectedElement.getTagName();
                            if (elemName.endsWith(":Signature")) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    
    protected void assertPolicy(AssertionInfoMap aim, Assertion token) {
        Collection<AssertionInfo> ais = aim.get(token.getName());
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == token) {
                    ai.setAsserted(true);
                }
            }    
        }
    }
    
    protected boolean assertPolicy(AssertionInfoMap aim, QName q) {
        Collection<AssertionInfo> ais = aim.get(q);
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }    
            return true;
        }
        return false;
    }
    
    protected void notAssertPolicy(AssertionInfoMap aim, QName q, String msg) {
        Collection<AssertionInfo> ais = aim.get(q);
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                ai.setNotAsserted(msg);
            }    
        }
    }
}
