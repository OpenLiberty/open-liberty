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

package org.apache.wss4j.common.token;

import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.bsp.BSPEnforcer;
import org.apache.wss4j.common.bsp.BSPRule;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.cert.X509Certificate;

/**
 * PKIPath Security Token.
 */
public class PKIPathSecurity extends BinarySecurity {
    public static final String PKI_TYPE = WSS4JConstants.X509TOKEN_NS + "#X509PKIPathv1";

    /**
     * Constructor.
     *
     * @param elem The PKIPath element to process
     * @param bspEnforcer a BSPEnforcer instance to enforce BSP rules
     * @throws WSSecurityException
     */
    public PKIPathSecurity(Element elem, BSPEnforcer bspEnforcer) throws WSSecurityException {
        super(elem, bspEnforcer);
        if (!PKI_TYPE.equals(getValueType())) {
            bspEnforcer.handleBSPRule(BSPRule.R5214);
        }
    }

    /**
     * Constructor.
     */
    public PKIPathSecurity(Document doc) {
        super(doc);
        setValueType(PKI_TYPE);
    }

    /**
     * get the X509Certificate array.
     *
     * @param crypto
     * @return array of certificates
     * @throws WSSecurityException
     */
    public X509Certificate[] getX509Certificates(Crypto crypto)
        throws WSSecurityException {
        byte[] data = getToken();
        if (data == null) {
            return null;
        }
        if (crypto == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noSigCryptoFile");
        }
        return crypto.getCertificatesFromBytes(data);
    }

    /**
     * set the X509Certificate array.
     *
     * @param certs
     * @param crypto
     * @throws WSSecurityException
     */
    public void setX509Certificates(
        X509Certificate[] certs,
        Crypto crypto
    ) throws WSSecurityException {
        if (certs == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noCert");
        }
        byte[] data = crypto.getBytesFromCertificates(certs);
        setToken(data);
    }

    public static String getType() {
        return PKI_TYPE;
    }
}
