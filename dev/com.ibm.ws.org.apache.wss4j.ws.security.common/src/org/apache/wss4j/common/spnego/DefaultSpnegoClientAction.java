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

package org.apache.wss4j.common.spnego;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

/**
 * This class represents a PrivilegedAction implementation to obtain a (SPNEGO) service ticket from a
 * Kerberos Key Distribution Center.
 */
public class DefaultSpnegoClientAction implements SpnegoClientAction {
    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(DefaultSpnegoClientAction.class);

    private String serviceName;
    private GSSContext secContext;
    private boolean mutualAuth;
    private boolean isUsernameServiceNameForm;

    /**
     * Whether to enable mutual authentication or not.
     */
    public void setMutualAuth(boolean mutualAuthentication) {
        mutualAuth = mutualAuthentication;
    }

    /**
     * The Service Name
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Obtain a service ticket
     */
    public byte[] run() {
        try {
            GSSManager gssManager = GSSManager.getInstance();
            Oid oid = new Oid("1.3.6.1.5.5.2");

            GSSName gssService = gssManager.createName(serviceName, isUsernameServiceNameForm
                                                       ? GSSName.NT_USER_NAME : GSSName.NT_HOSTBASED_SERVICE);
            secContext = gssManager.createContext(gssService, oid, null, GSSContext.DEFAULT_LIFETIME);

            secContext.requestMutualAuth(mutualAuth);
            secContext.requestCredDeleg(Boolean.FALSE);

            byte[] token = new byte[0];
            return secContext.initSecContext(token, 0, token.length);
        } catch (GSSException e) {
            LOG.debug("Error in obtaining a Kerberos token", e);
        }

        return null;
    }

    /**
     * Get the GSSContext that was created after a service ticket was obtained
     */
    public GSSContext getContext() {
        return secContext;
    }

    @Override
    public void setUserNameServiceForm(boolean isUsernameServiceNameForm) {
        this.isUsernameServiceNameForm = isUsernameServiceNameForm;
    }

}
