/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.security.oidcclientcore.authentication;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException.ValidationResult;
import io.openliberty.security.oidcclientcore.storage.CookieBasedStorage;
import io.openliberty.security.oidcclientcore.storage.OidcClientStorageConstants;
import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;
import io.openliberty.security.oidcclientcore.storage.SessionBasedStorage;

public class JakartaOidcAuthenticationResponseValidator extends AuthenticationResponseValidator {

    public static final TraceComponent tc = Tr.register(JakartaOidcAuthenticationResponseValidator.class);

    private final OidcClientConfig oidcClientConfig;

    public JakartaOidcAuthenticationResponseValidator(HttpServletRequest request, HttpServletResponse response, OidcClientConfig oidcClientConfig) {
        super(request, response);
        this.oidcClientConfig = oidcClientConfig;
        instantiateStorage(oidcClientConfig);
    }

    private void instantiateStorage(OidcClientConfig config) {
        if (oidcClientConfig.isUseSession()) {
            this.storage = new SessionBasedStorage(request);
        } else {
            this.storage = new CookieBasedStorage(request, response);
        }
    }

    /**
     * Performs validation according to https://jakarta.ee/specifications/security/3.0/jakarta-security-spec-3.0.html#authentication-dialog.
     * Specifically:
     * <ul>
     * <li>If the request (without request parameters) doesn't match the redirectURI, or does not match the stored original URL
     * (without request parameters) in case AuthenticationMechanismDefinition.redirectToOriginalResource is set to true, reply
     * with CredentialValidationResult.NOT_VALIDATED_RESULT.</li>
     * <li>If there is no State value stored, reply with CredentialValidationResult.NOT_VALIDATED_RESULT.</li>
     * <li>If the State value in the request does not match the State value stored, reply with
     * CredentialValidationResult.INVALID_RESULT.</li>
     * <li>If the request contains a parameter error, reply with CredentialValidationResult.INVALID_RESULT.</li>
     * </ul>
     * If none of the above listed additional conditions apply, the request is taken to be a valid callback and the authentication
     * between the end-user (caller) and the OpenID Connect Provider is considered to have been successful. The authentication
     * mechanism must now move to [the next step of the OpenID Connect flow] and mark this internally by clearing the stored State
     * value (remove it from the HTTP session or Cookie).
     */
    @Override
    public void validateResponse() throws AuthenticationResponseException {
        String state = getAndVerifyStateValue();
        checkRequestAgainstRedirectUri(state);
        checkForErrorParameter();
    }

    /**
     * If there is no State value stored, reply with CredentialValidationResult.NOT_VALIDATED_RESULT. If the State value in the
     * request does not match the State value stored, reply with CredentialValidationResult.INVALID_RESULT.
     */
    String getAndVerifyStateValue() throws AuthenticationResponseException {
        String stateParameter = request.getParameter(AuthorizationRequestParameters.STATE);
        if (stateParameter == null || stateParameter.isEmpty()) {
            String nlsMessage = Tr.formatMessage(tc, "CALLBACK_MISSING_STATE_PARAMETER");
            throw new AuthenticationResponseException(ValidationResult.INVALID_RESULT, oidcClientConfig.getClientId(), nlsMessage);
        }
        String clientSecret = null;
        ProtectedString clientSecretProtectedString = oidcClientConfig.getClientSecret();
        if (clientSecretProtectedString != null) {
            clientSecret = new String(clientSecretProtectedString.getChars());
        }
        // TODO - Decide on clock skew value to use
        verifyState(stateParameter, oidcClientConfig.getClientId(), clientSecret, 300L, OidcClientStorageConstants.DEFAULT_STATE_STORAGE_LIFETIME_SECONDS);
        return stateParameter;
    }

    @Override
    public String getStoredStateValue(String stateParameter) throws AuthenticationResponseException {
        String stateStorageName = OidcStorageUtils.getStateStorageKey(stateParameter);
        String storedState = storage.get(stateStorageName);
        if (storedState == null) {
            String nlsMessage = Tr.formatMessage(tc, "STATE_VALUE_IN_CALLBACK_NOT_STORED", stateParameter);
            throw new AuthenticationResponseException(ValidationResult.NOT_VALIDATED_RESULT, oidcClientConfig.getClientId(), nlsMessage);
        }
        return storedState;
    }

    /**
     * If the request (without request parameters) doesn't match the redirectURI, or does not match the stored original URL
     * (without request parameters) in case AuthenticationMechanismDefinition.redirectToOriginalResource is set to true, reply
     * with CredentialValidationResult.NOT_VALIDATED_RESULT.
     */
    void checkRequestAgainstRedirectUri(String state) throws AuthenticationResponseException {
        String requestUrl = request.getRequestURL().toString();

        String configuredRedirectUri = oidcClientConfig.getRedirectURI();
        if (requestUrl.equals(configuredRedirectUri)) {
            return;
        }

        if (oidcClientConfig.isRedirectToOriginalResource()) {
            String originalReqUrl = storage.get(OidcStorageUtils.getOriginalReqUrlStorageKey(state));
            if (originalReqUrl == null) {
                throwExceptionForRedirectUriDoesNotMatch(requestUrl, originalReqUrl);
            }
            String originalReqUrlWithoutQueryParams = originalReqUrl.split(Pattern.quote("?"))[0];
            if (!originalReqUrlWithoutQueryParams.equals(requestUrl)) {
                throwExceptionForRedirectUriDoesNotMatch(requestUrl, originalReqUrlWithoutQueryParams);
            }
            return;
        }

        throwExceptionForRedirectUriDoesNotMatch(requestUrl, configuredRedirectUri);
    }

    void throwExceptionForRedirectUriDoesNotMatch(String requestUrl, String expectedUrl) throws AuthenticationResponseException {
        String nlsMessage = Tr.formatMessage(tc, "CALLBACK_URL_DOES_NOT_MATCH_REDIRECT_URI", requestUrl, expectedUrl, oidcClientConfig.getClientId());
        throw new AuthenticationResponseException(ValidationResult.NOT_VALIDATED_RESULT, oidcClientConfig.getClientId(), nlsMessage);
    }

    /**
     * If the request contains a parameter error, reply with CredentialValidationResult.INVALID_RESULT.
     */
    void checkForErrorParameter() throws AuthenticationResponseException {
        String errorParameter = request.getParameter("error");
        if (errorParameter != null) {
            String nlsMessage = Tr.formatMessage(tc, "CALLBACK_URL_INCLUDES_ERROR_PARAMETER", errorParameter);
            throw new AuthenticationResponseException(ValidationResult.INVALID_RESULT, oidcClientConfig.getClientId(), nlsMessage);
        }
    }

}
