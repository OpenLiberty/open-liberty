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
import com.ibm.ws.security.jaas.common.callback.AuthenticationHelper;
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
    public final static String REALM_DELIMITER = "/";
    public final static String TYPE_DELIMITER = ":";
    public final static String USER_TYPE_DELIMITER = "user:";
    public final static String EMPTY_STRING = new String("");

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
        String uniqueID = validateLTPAToken(token);
        return new ValidationResultImpl(uniqueID);
    }

    /**
     * <p>
     * This method validates an LTPA token and will return a uniqueID in
     * the format of realm/username. The username portion of the uniqueID
     * is a unique username from the registry (example, for LDAP, this is the DN).
     * If the token cannot be validated or is expired, a ValidationFailedException
     * will be thrown. Otherwise, the uniqueID is returned. Some helper
     * methods are provided to parse the realm and user uniqueid from the
     * uniqueid.
     *
     * @see getRealmFromUniqueID (uniqueID)
     * @see getUserFromUniqueID (uniqueID)
     *      </p>
     *
     *      The validateLTPAToken API requires a Java 2 Security permission,
     *      WebSphereRuntimePermission "validateLTPAToken".
     *
     * @param byte[] (LtpaToken or LtpaToken2)
     * @return String WebSphere uniqueID
     * @exception ValidationFailedException
     **/

    public static String validateLTPAToken(byte[] token) throws ValidationFailedException {
        String accessId = null;

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
                    accessId = recreatedToken.getAttributes("u")[0];
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
        return accessId;

    }

    /**
     * <p>
     * This method accepts the uniqueID returned from the validateLTPAToken method.
     * You can also use this method to parse the uniqueID returned from the
     * UserRegistry.getUniqueUserId (uid) method. It returns the unique userid
     * portion of this string. For an LDAP registry, this is the DN. For a
     * LocalOS registry, this is the LocalOS unique identifier.
     * </p>
     *
     * @param String WebSphere uniqueID
     * @return String registry uniqueID
     **/

    public static String getUserFromUniqueID(String uniqueID) {
        if (uniqueID == null) {
            return EMPTY_STRING;
        }
        if (uniqueID.startsWith(USER_TYPE_DELIMITER)) {
            uniqueID = uniqueID.trim();
            int realmDelimiterIndex = uniqueID.indexOf(REALM_DELIMITER);
            if (realmDelimiterIndex < 0) {
                return EMPTY_STRING;
            } else {
                return uniqueID.substring(realmDelimiterIndex + 1);
            }
        }
        return EMPTY_STRING;
    }

    /**
     * <p>
     * This method accepts the uniqueID returned from the validateLTPAToken method.
     * It returns the realm portion of this string. The realm can be used to
     * determine where the token came from.
     * </p>
     *
     * @param String WebSphere uniqueID
     * @return String realm
     **/

    public static String getRealmFromUniqueID(String uniqueID) {

        int index = uniqueID.indexOf(TYPE_DELIMITER);

        if (uniqueID.startsWith(USER_TYPE_DELIMITER)) {
            uniqueID = uniqueID.substring(index + 1);
        }
        return getRealm(uniqueID);
    }

    private static String getRealm(String realmSecurityName) {
        if (realmSecurityName == null)
            return EMPTY_STRING;

        realmSecurityName = realmSecurityName.trim();

        {
            int realmDelimiterIndex = realmSecurityName.indexOf(REALM_DELIMITER);
            if (realmDelimiterIndex < 0) {
                return null;
            }
            return realmSecurityName.substring(0, realmDelimiterIndex);
        }
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
            byte[] credToken = AuthenticationHelper.copyCredToken(ssoToken);
            token = tokenManager.recreateTokenFromBytes(credToken);
        }
        return token;
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
