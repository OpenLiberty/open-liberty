/*******************************************************************************
 * Copyright (c) 1997, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.mediator;

import java.rmi.RemoteException;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MediatorException;
import com.ibm.oauth.core.api.oauth20.mediator.OAuth20Mediator;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.PasswordCheckFailedException;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.websphere.security.WSSecurityException;

public class ResourceOwnerValidationMediator implements OAuth20Mediator {
    private static TraceComponent tc = Tr.register(ResourceOwnerValidationMediator.class,
            "OAuth20Provider", "com.ibm.ws.security.oauth20.internal.resources.OAuthMessages");
    private static final String INVALID = "invalid_resource_owner_credential";

    // private ResourceBundle resBundle = ResourceBundle.getBundle(Constants.RESOURCE_BUNDLE, Locale.getDefault());

    private UserRegistry reg = null;
    private static final String FLOW_PASSWORD = "password";

    @Override
    public void init(OAuthComponentConfiguration config) {
        final String methodName = "init";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName);
        }

        try {
            reg = com.ibm.wsspi.security.registry.RegistryHelper.getUserRegistry(null);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "init: getUserRegistry returned:" + (reg != null ? "not null" : "null"));
            }
        } catch (WSSecurityException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to get user registry for resource owner validation", e);
            }
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

    @Override
    public void mediateAuthorize(AttributeList attributeList)
            throws OAuth20MediatorException {
        // TODO Auto-generated method stub

    }

    @Override
    public void mediateAuthorizeException(AttributeList attributeList,
            OAuthException exception) throws OAuth20MediatorException {
        // TODO Auto-generated method stub

    }

    @Override
    public void mediateResource(AttributeList attributeList)
            throws OAuth20MediatorException {
        // TODO Auto-generated method stub

    }

    @Override
    public void mediateResourceException(AttributeList attributeList,
            OAuthException exception) throws OAuth20MediatorException {
        // TODO Auto-generated method stub

    }

    @Override
    public void mediateToken(AttributeList attributeList) throws OAuth20MediatorException {
        final String methodName = "mediateToken";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName);
        }

        if (FLOW_PASSWORD.equals(attributeList.getAttributeValueByName("grant_type"))) {
            String username = attributeList.getAttributeValueByName("username");
            String password = attributeList.getAttributeValueByName("password");
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "mediateToken: Username and Password is " + username + password);
            }
            try {
                if (reg == null) { // If reg was not initialized before this call
                    reg = com.ibm.wsspi.security.registry.RegistryHelper.getUserRegistry(null);
                    if (reg == null) { // check is reg is still null
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "getUserRegistry returned null.");
                        }
                        throw new OAuth20MediatorException(INVALID, (new Throwable("getUserRegistry returned null")));
                    }
                }
                reg.checkPassword(username, password);
            } catch (PasswordCheckFailedException e) {
                throw new OAuth20MediatorException(INVALID, e);
            } catch (CustomRegistryException e) {
                throw new OAuth20MediatorException(INVALID, e);
            } catch (RemoteException e) {
                throw new OAuth20MediatorException(INVALID, e);
            } catch (WSSecurityException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to get user registry for resource owner validation", e);
                }
                throw new OAuth20MediatorException(INVALID, e);
            }
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

    @Override
    public void mediateTokenException(AttributeList attributeList, OAuthException exception)
            throws OAuth20MediatorException {
        final String methodName = "mediateTokenException";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName);
        }

        if ("password".equals(attributeList.getAttributeValueByName("grant_type"))) {
            // clear sensitive data
            attributeList.setAttribute("access_token", OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE, new String[0]);
            attributeList.setAttribute("refresh_token", OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE, new String[0]);
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

}
