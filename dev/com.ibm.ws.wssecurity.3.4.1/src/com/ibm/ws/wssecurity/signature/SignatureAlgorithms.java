/*******************************************************************************
 * Copyright (c) 2020,2023 IBM Corporation and others.
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
package com.ibm.ws.wssecurity.signature;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.wss4j.policy.SP11Constants;
//import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.model.AbstractBinding;
//import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;
//import org.apache.cxf.ws.security.policy.model.Binding;
import org.apache.wss4j.policy.model.AlgorithmSuite;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;

public class SignatureAlgorithms {

    private static final TraceComponent tc = Tr.register(SignatureAlgorithms.class, WSSecurityConstants.TR_GROUP, WSSecurityConstants.TR_RESOURCE_BUNDLE);

    static final String rsa_sha1 = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
    static final String rsa_sha256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    static final String rsa_sha384 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384";
    static final String rsa_sha512 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";
    static final String hmac_sha1 = "http://www.w3.org/2000/09/xmldsig#hmac-sha1";
    static final String hmac_sha256 = "http://www.w3.org/2001/04/xmldsig-more#hmac-sha256";
    static final String hmac_sha384 = "http://www.w3.org/2001/04/xmldsig-more#hmac-sha384";
    static final String hmac_sha512 = "http://www.w3.org/2001/04/xmldsig-more#hmac-sha512";

    static Map<String, String> RSA_MAP = new HashMap<String, String>();
    static {
        RSA_MAP.put("sha1", rsa_sha1);
        RSA_MAP.put("sha256", rsa_sha256);
        RSA_MAP.put("sha384", rsa_sha384);
        RSA_MAP.put("sha512", rsa_sha512);
    }

    static Map<String, String> HMAC_MAP = new HashMap<String, String>();
    static {
        HMAC_MAP.put("sha1", hmac_sha1);
        HMAC_MAP.put("sha256", hmac_sha256);
        HMAC_MAP.put("sha384", hmac_sha384);
        HMAC_MAP.put("sha512", hmac_sha512);
    }

    public static void setAlgorithm(SoapMessage message, String method) {
        if (message == null || method == null || method.isEmpty()) {
            return;
        }
        String algorithm = method.toLowerCase();
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        AlgorithmSuite algorithmSuite = getAlgorithmSuite(aim);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "AlgorithmSuite ", new Object[] { algorithmSuite, method });
        }
        if (algorithmSuite != null) {
            if (RSA_MAP.containsKey(algorithm)) {
                algorithmSuite.getAlgorithmSuiteType().setAsymmetricSignature(RSA_MAP.get(algorithm));
                //algorithmSuite.setAsymmetricSignature(RSA_MAP.get(algorithm)); //v3
            }
            if (HMAC_MAP.containsKey(algorithm)) {
                algorithmSuite.getAlgorithmSuiteType().setSymmetricSignature(HMAC_MAP.get(algorithm));
                //algorithmSuite.setSymmetricSignature(HMAC_MAP.get(algorithm)); //v3
            }
        }
    }
    public static AbstractBinding getAbstractBinding(AssertionInfoMap aim, String binding) {
        Collection<AssertionInfo> ais = null;
        AbstractBinding absBinding = null;
        if ("transport".equals(binding)) {
            ais = getMatchingAssertionInfo(aim, SP12Constants.TRANSPORT_BINDING);
            if (ais == null) {
                ais = getMatchingAssertionInfo(aim, SP11Constants.TRANSPORT_BINDING);
            }
        } else if("asymmetric".equals(binding)) {
            ais = getMatchingAssertionInfo(aim, SP12Constants.ASYMMETRIC_BINDING);
            if (ais == null) {
                ais = getMatchingAssertionInfo(aim, SP11Constants.ASYMMETRIC_BINDING);
            }
        } else if ("symmetric".equals(binding)) {
            ais = getMatchingAssertionInfo(aim, SP12Constants.SYMMETRIC_BINDING);
            if (ais == null) {
                ais = getMatchingAssertionInfo(aim, SP11Constants.SYMMETRIC_BINDING);
            }
        }
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                absBinding = (AbstractBinding) ai.getAssertion();
            }
        }
        return absBinding;
    }
    public static Collection<AssertionInfo> getMatchingAssertionInfo(AssertionInfoMap aim, QName qname) {
        return aim.get(qname);

    }

    public static AlgorithmSuite getAlgorithmSuite(AssertionInfoMap aim) {
        AbstractBinding binding = null;
        binding = getAbstractBinding(aim, "transport");
        if (binding == null) {
            binding = getAbstractBinding(aim, "asymmetric");
            if (binding == null) {
                binding = getAbstractBinding(aim, "symmetric");
            }
        }
        if (binding != null) {
            return binding.getAlgorithmSuite();
        }
        return null;
    }
}
