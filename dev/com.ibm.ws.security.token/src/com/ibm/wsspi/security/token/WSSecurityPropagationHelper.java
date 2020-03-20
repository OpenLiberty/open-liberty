/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.token;

import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.WebSphereRuntimePermission;
import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.websphere.security.auth.ValidationFailedException;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.ws.security.token.internal.ValidationResultImpl;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.ltpa.Token;

/**
 * This class provides some helper methods to retrieve the user information from an LTPA token.
 *
 * @author IBM Corporation
 * @version 5.1.1
 * @since 5.1.1
 * @ibm-spi
 **/

public class WSSecurityPropagationHelper {
    private static final TraceComponent tc = Tr.register(WSSecurityPropagationHelper.class);
    private static final AtomicServiceReference<TokenManager> tokenManagerRef = new AtomicServiceReference<TokenManager>("tokenManager");
    private final static WebSphereRuntimePermission VALIDATE_TOKEN = new WebSphereRuntimePermission("validateLTPAToken");

    /**
     * <p>
     * This method validates an LTPA token and will return a ValidationResult object.
     *
     * If the token cannot be validated, is expired or null, a ValidationFailedException
     * will be thrown.
     * </p>
     * The validateToken API requires a Java 2 Security permission,
     * WebSphereRuntimePermission "validateLTPAToken".
     *
     * @param byte[] (LtpaToken2)
     * @return ValidationResult
     * @exception ValidationFailedException
     **/
    public static ValidationResult validateToken(byte[] token) throws ValidationFailedException {
        ValidationResult validationResult = null;
        java.lang.SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Performing Java 2 Security Permission Check ...");
                Tr.debug(tc, "Expecting : " + VALIDATE_TOKEN.toString());
            }
            sm.checkPermission(VALIDATE_TOKEN);
        }

        if (token != null) {
            try {
                Token recreatedToken = recreateTokenFromBytes(token);
                if (recreatedToken != null) {
                    String accessId = recreatedToken.getAttributes("u")[0];
                    String realms[] = recreatedToken.getAttributes(AttributeNameConstants.WSCREDENTIAL_REALM);
                    String realm = null;
                    if (realms != null)
                        realm = realms[0];

                    validationResult = new ValidationResultImpl(accessId, realm);
                }
            } catch (WSSecurityException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "validateLTPAToken caught exception: " + e.getMessage());
                }
                throw new ValidationFailedException(e.getLocalizedMessage());
            }
        } else {
            throw new ValidationFailedException("Invalid token, token returned from validation is null.");
        }
        return validationResult;

    }

    /**
     * @param ssoToken
     * @return
     * @throws InvalidTokenException
     * @throws TokenExpiredException
     */
    private static Token recreateTokenFromBytes(byte[] ssoToken) throws InvalidTokenException, TokenExpiredException {
        Token token = null;
        TokenManager tokenManager = tokenManagerRef.getService();
        if (tokenManager != null) {
//            byte[] credToken = AuthenticationHelper.copyCredToken(ssoToken);
            byte[] credToken = copyCredToken(ssoToken);
            token = tokenManager.recreateTokenFromBytes(credToken);

        }
        return token;
    }

    /**
     * This code is from AuthenticationHelper
     * Create a copy of the specified byte array.
     *
     * @param credToken
     * @return A copy of the specified byte array, or null if the input was null.
     */
    private static byte[] copyCredToken(byte[] credToken) {

        if (credToken == null) {
            return null;
        }

        final int LEN = credToken.length;
        if (LEN == 0) {
            return new byte[LEN];
        }

        byte[] newCredToken = new byte[LEN];
        System.arraycopy(credToken, 0, newCredToken, 0, LEN);

        return newCredToken;
    }

    protected void setTokenManager(ServiceReference<TokenManager> ref) {
        tokenManagerRef.setReference(ref);
    }

    protected void unsetTokenManager(ServiceReference<TokenManager> ref) {
        tokenManagerRef.unsetReference(ref);
    }

    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        tokenManagerRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        tokenManagerRef.deactivate(cc);
    }
}
