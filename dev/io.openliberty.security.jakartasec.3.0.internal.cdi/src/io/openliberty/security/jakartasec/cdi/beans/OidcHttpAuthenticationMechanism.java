/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.cdi.beans;

import java.util.Properties;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider;

import io.openliberty.security.jakartasec.JakartaSec30Constants;
import io.openliberty.security.jakartasec.OpenIdAuthenticationMechanismDefinitionWrapper;
import io.openliberty.security.oidcclientcore.client.Client;
import io.openliberty.security.oidcclientcore.client.ClientManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Default
@ApplicationScoped
public class OidcHttpAuthenticationMechanism implements HttpAuthenticationMechanism {

    private ModulePropertiesProvider mpp = null;
    private OpenIdAuthenticationMechanismDefinitionWrapper oidcMechanismDefinitionWrapper;

    public OidcHttpAuthenticationMechanism() {
        setOpenIdAuthenticationMechanismDefinition();
    }

    private void setOpenIdAuthenticationMechanismDefinition() {
        mpp = getModulePropertiesProvider();
        Properties props = mpp.getAuthMechProperties(OidcHttpAuthenticationMechanism.class);
        this.oidcMechanismDefinitionWrapper = new OpenIdAuthenticationMechanismDefinitionWrapper((OpenIdAuthenticationMechanismDefinition) props
                        .get(JakartaSec30Constants.OIDC_ANNOTATION));
    }

    @SuppressWarnings("unchecked")
    protected ModulePropertiesProvider getModulePropertiesProvider() {
        Instance<ModulePropertiesProvider> modulePropertiesProivderInstance = getCDI().select(ModulePropertiesProvider.class);
        if (modulePropertiesProivderInstance != null) {
            return modulePropertiesProivderInstance.get();
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    @FFDCIgnore(IllegalStateException.class)
    protected CDI getCDI() {
        try {
            return CDI.current();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    @Sensitive
    public OidcHttpAuthenticationMechanism(@Sensitive OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition) {
        this.oidcMechanismDefinitionWrapper = new OpenIdAuthenticationMechanismDefinitionWrapper(oidcMechanismDefinition);
    }

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        Client client = ClientManager.getClientFor(oidcMechanismDefinitionWrapper);

        // If isAuthenticationRequired
        //   status = createAuthenticationRequest - client.startFlow();
        // Else if isCallbackRequest
        //   status = processCallback - client.continueFlow();
        // Else if isAuthenticationSessionEstablished
        //   status = processTokenExpirationIfNeeded - client.processExpiredToken() / logout();

        return status;
    }

    @Override
    public void cleanSubject(HttpServletRequest request,
                             HttpServletResponse response,
                             HttpMessageContext httpMessageContext) {

    }

}
