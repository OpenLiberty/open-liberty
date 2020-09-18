/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.security.jwt.internal.TraceConstants;
import com.ibm.ws.security.jwt.utils.JwtUtils;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

@Component(service = JwtConfigUtil.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, name = "jwtConfigUtil", property = "service.vendor=IBM")
public class JwtConfigUtil {

    private static final TraceComponent tc = Tr.register(JwtConfigUtil.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    // Tells us if the message for a call to a beta method has been issued
    private static List<String> issuedBetaMessageForConfigs = new ArrayList<String>();

    private static final String KEY_JWT_SERVICE = "jwtComponent";
    private static ConcurrentServiceReferenceMap<String, JwtConfig> jwtServiceRef = new ConcurrentServiceReferenceMap<String, JwtConfig>(KEY_JWT_SERVICE);

    public static synchronized void setJwtService(ConcurrentServiceReferenceMap<String, JwtConfig> jwtServiceRefMap) {
        jwtServiceRef = jwtServiceRefMap;
    }

    public static synchronized ConcurrentServiceReferenceMap<String, JwtConfig> getJwtService() {
        return jwtServiceRef;
    }

    @Sensitive
    public static String processProtectedString(Map<String, Object> props, String cfgKey) {
        String secret;
        Object o = props.get(cfgKey);
        if (o != null) {
            if (o instanceof SerializableProtectedString) {
                secret = new String(((SerializableProtectedString) o).getChars());
            } else {
                secret = (String) o;
            }
        } else {
            secret = null;
        }
        // decode
        secret = PasswordUtil.passwordDecode(secret);
        return secret;
    }

    public static String getSignatureAlgorithm(String configId, Map<String, Object> props, String sigAlgAttrName) {
        String defaultSignatureAlgorithm = "RS256";
        String signatureAlgorithm = JwtUtils.trimIt((String) props.get(sigAlgAttrName));
        boolean isBetaEnabled = ProductInfo.getBetaEdition();
        if (!isBetaEnabled && isBetaAlgorithm(signatureAlgorithm)) {
            if (!isBetaMessageIssuedForConfig(configId)) {
                Tr.warning(tc, "BETA_SIGNATURE_ALGORITHM_USED", new Object[] { configId, signatureAlgorithm, defaultSignatureAlgorithm });
                issuedBetaMessageForConfigs.add(configId);
            }
            signatureAlgorithm = defaultSignatureAlgorithm;
        }
        if (signatureAlgorithm == null) {
            signatureAlgorithm = defaultSignatureAlgorithm;
        }
        return signatureAlgorithm;
    }

    private static boolean isBetaAlgorithm(String algorithm) {
        if (algorithm == null) {
            return false;
        }
        return !algorithm.matches("RS256|HS256");
    }

    private static boolean isBetaMessageIssuedForConfig(String configId) {
        return issuedBetaMessageForConfigs.contains(configId);
    }

}
