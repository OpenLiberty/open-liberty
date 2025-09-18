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

import java.util.Collection;
import java.util.List;

import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.model.SymmetricBinding;

/**
 * Validate a SymmetricBinding policy.
 */
public class SymmetricBindingPolicyValidator extends AbstractBindingPolicyValidator {

    /**
     * Return true if this SecurityPolicyValidator implementation is capable of validating a
     * policy defined by the AssertionInfo parameter
     */
    public boolean canValidatePolicy(AssertionInfo assertionInfo) {
        return assertionInfo.getAssertion() != null
            && (SP12Constants.SYMMETRIC_BINDING.equals(assertionInfo.getAssertion().getName())
                || SP11Constants.SYMMETRIC_BINDING.equals(assertionInfo.getAssertion().getName()));
    }

    /**
     * Validate policies.
     */
    public void validatePolicies(PolicyValidatorParameters parameters, Collection<AssertionInfo> ais) {
        boolean hasDerivedKeys =
            parameters.getResults().getActionResults().containsKey(WSConstants.DKT);

        for (AssertionInfo ai : ais) {
            SymmetricBinding binding = (SymmetricBinding)ai.getAssertion();
            ai.setAsserted(true);

            // Check the protection order
            if (!checkProtectionOrder(binding, parameters.getAssertionInfoMap(), ai,
                                      parameters.getResults().getResults())) {
                continue;
            }

            // Check various properties of the binding
            if (!checkProperties(binding, ai, parameters.getAssertionInfoMap(), parameters.getResults(),
                                 parameters.getSignedResults(), parameters.getMessage())) {
                continue;
            }

            // Check various tokens of the binding
            if (!checkTokens(binding, ai, parameters.getAssertionInfoMap(), hasDerivedKeys,
                             parameters.getSignedResults(), parameters.getEncryptedResults())) {
                continue;
            }
        }
    }

    /**
     * Check various tokens of the binding
     */
    private boolean checkTokens(
        SymmetricBinding binding,
        AssertionInfo ai,
        AssertionInfoMap aim,
        boolean hasDerivedKeys,
        List<WSSecurityEngineResult> signedResults,
        List<WSSecurityEngineResult> encryptedResults
    ) {
        if (binding.getEncryptionToken() != null) {
            PolicyUtils.assertPolicy(aim, binding.getEncryptionToken().getName());
            if (!checkDerivedKeys(
                binding.getEncryptionToken(), hasDerivedKeys, signedResults, encryptedResults
            )) {
                ai.setNotAsserted("Message fails the DerivedKeys requirement");
                return false;
            }
            assertDerivedKeys(binding.getEncryptionToken().getToken(), aim);
        }

        if (binding.getSignatureToken() != null) {
            PolicyUtils.assertPolicy(aim, binding.getSignatureToken().getName());
            if (!checkDerivedKeys(
                binding.getSignatureToken(), hasDerivedKeys, signedResults, encryptedResults
            )) {
                ai.setNotAsserted("Message fails the DerivedKeys requirement");
                return false;
            }
            assertDerivedKeys(binding.getSignatureToken().getToken(), aim);
        }

        if (binding.getProtectionToken() != null) {
            PolicyUtils.assertPolicy(aim, binding.getProtectionToken().getName());
            if (!checkDerivedKeys(
                binding.getProtectionToken(), hasDerivedKeys, signedResults, encryptedResults
            )) {
                ai.setNotAsserted("Message fails the DerivedKeys requirement");
                return false;
            }
            assertDerivedKeys(binding.getProtectionToken().getToken(), aim);
        }

        return true;
    }

}
