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
package org.apache.wss4j.stax.securityEvent;

import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.securityToken.KerberosServiceSecurityToken;

public class KerberosTokenSecurityEvent extends IssuedTokenSecurityEvent<KerberosServiceSecurityToken> {

    private String issuerName;

    public KerberosTokenSecurityEvent() {
        super(WSSecurityEventConstants.KERBEROS_TOKEN);
    }

    public String getIssuerName() {
        return issuerName; //todo return ((KerberosServiceSecurityToken)getSecurityToken()).???();
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }

    public boolean isKerberosV5ApReqToken11() {
        String type = getSecurityToken().getKerberosTokenValueType();
        return WSSConstants.NS_KERBEROS5_AP_REQ.equals(type)
                || WSSConstants.NS_KERBEROS5_AP_REQ1510.equals(type)
                || WSSConstants.NS_KERBEROS5_AP_REQ4120.equals(type);
    }

    public boolean isGssKerberosV5ApReqToken11() {
        String type = getSecurityToken().getKerberosTokenValueType();
        return WSSConstants.NS_GSS_KERBEROS5_AP_REQ.equals(type)
                || WSSConstants.NS_GSS_KERBEROS5_AP_REQ1510.equals(type)
                || WSSConstants.NS_GSS_KERBEROS5_AP_REQ4120.equals(type);
    }

    public String getKerberosTokenValueType() {
        return getSecurityToken().getKerberosTokenValueType();
    }
}
