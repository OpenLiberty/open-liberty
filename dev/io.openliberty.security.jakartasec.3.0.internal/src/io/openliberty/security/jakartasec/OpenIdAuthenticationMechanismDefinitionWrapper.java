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
package io.openliberty.security.jakartasec;

import java.util.LinkedHashSet;
import java.util.Set;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.javaeesec.identitystore.ELHelper;

import io.openliberty.security.oidcclientcore.client.ClaimsMappingConfig;
import io.openliberty.security.oidcclientcore.client.LogoutConfig;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.DisplayType;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.authentication.mechanism.http.openid.PromptType;

/**
 * A wrapper class that offers convenience methods for retrieving configuration
 * from an {@link OpenIdAuthenticationMechanismDefinition} instance.
 *
 * <p/>
 * The methods in this class will evaluate any EL expressions provided in the
 * {@link OpenIdAuthenticationMechanismDefinition} first and if no EL expressions are provided,
 * return the literal value instead.
 */
public class OpenIdAuthenticationMechanismDefinitionWrapper implements OidcClientConfig {

    private static final TraceComponent tc = Tr.register(OpenIdAuthenticationMechanismDefinitionWrapper.class);

    private static final String EMPTY_DEFAULT = "";

    private final OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition;

    private final String providerURI;

    private final OpenIdProviderMetadataWrapper providerMetadataWrapper;

    private final String clientId;

    private final ProtectedString clientSecret;

    private final ClaimsDefinitionWrapper claimsDefinitionWrapper;

    private final LogoutDefinitionWrapper logoutDefinitionWrapper;

    private final String redirectURI;

    private final Boolean redirectToOriginalResource;

    private final String[] scope;

    private final String responseType;

    private final String responseMode;

    private final PromptType[] prompt;

    private final DisplayType display;

    private final Boolean useNonce;

    private final Boolean useSession;

    private final String[] extraParameters;

    private final Integer jwksConnectTimeout;

    private final Integer jwksReadTimeout;

    private final Boolean tokenAutoRefresh;

    private final Integer tokenMinValidity;

    private final ELHelper elHelper;

    private final String constructedBaseURL;

    /**
     * Create a new instance of an {@link OpenIdAuthenticationMechanismDefinitionWrapper} that will provide
     * convenience methods to access configuration from the {@link OpenIdAuthenticationMechanismDefinition}
     * instance.
     *
     * @param oidcMechanismDefinition The {@link OpenIdAuthenticationMechanismDefinition} to wrap.
     * @param baseURL                 The baseURL is an optional variable for the redirectURL and is constructed using information the incoming HTTP request.
     */
    @Sensitive
    public OpenIdAuthenticationMechanismDefinitionWrapper(OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition, String baseURL) {
        if (oidcMechanismDefinition == null) {
            throw new IllegalArgumentException("The OpenIdAuthenticationMechanismDefinition cannot be null.");
        }
        this.oidcMechanismDefinition = oidcMechanismDefinition;
        this.elHelper = new ELHelper();
        this.constructedBaseURL = baseURL;

        /*
         * Evaluate the configuration. The values will be non-null if the setting is NOT
         * a deferred EL expression. If it is a deferred EL expression, we will dynamically
         * evaluate it at call time.
         */
        providerURI = evaluateProviderURI(true);
        providerMetadataWrapper = new OpenIdProviderMetadataWrapper(oidcMechanismDefinition.providerMetadata());
        clientId = evaluateClientId(true);
        clientSecret = evaluateClientSecret(true);
        claimsDefinitionWrapper = new ClaimsDefinitionWrapper(oidcMechanismDefinition.claimsDefinition());
        logoutDefinitionWrapper = new LogoutDefinitionWrapper(oidcMechanismDefinition.logout());
        redirectURI = evaluateRedirectURI(true);
        redirectToOriginalResource = evaluateRedirectToOriginalResource(true);
        scope = evaluateScope(true);
        responseType = evaluateResponseType(true);
        responseMode = evaluateResponseMode(true);
        prompt = evaluatePrompt(true);
        display = evaluateDisplay(true);
        useNonce = evaluateUseNonce(true);
        useSession = evaluateUseSession(true);
        extraParameters = evaluateExtraParameters(true);
        jwksConnectTimeout = evaluateJwksConnectTimeout(true);
        jwksReadTimeout = evaluateJwksReadTimeout(true);
        tokenAutoRefresh = evaluateTokenAutoRefresh(true);
        tokenMinValidity = evaluateTokenMinValidity(true);
    }

    /*
     * If deferred expression is processed during initialization, return null so the expression can be re-evaluated
     * again later.
     */

    private String evaluateProviderURI(boolean immediateOnly) {
        return evaluateStringAttribute("providerURI", oidcMechanismDefinition.providerURI(), EMPTY_DEFAULT, immediateOnly);
    }

    private String evaluateClientId(boolean immediateOnly) {
        return evaluateStringAttribute("clientId", oidcMechanismDefinition.clientId(), EMPTY_DEFAULT, immediateOnly);
    }

    @SuppressWarnings("static-access")
    @FFDCIgnore(IllegalArgumentException.class)
    private ProtectedString evaluateClientSecret(boolean immediateOnly) {
        String result;
        String clientSecret = oidcMechanismDefinition.clientSecret();
        try {
            result = elHelper.processString("clientSecret", clientSecret, immediateOnly, true);
        } catch (IllegalArgumentException e) {
            if (immediateOnly && elHelper.isDeferredExpression(clientSecret)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "clientSecret", "Returning null since clientSecret is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            issueWarningMessage("clientSecret", EMPTY_DEFAULT);

            result = EMPTY_DEFAULT; /* Default value from spec. */
        }
        return (result == null) ? null : new ProtectedString(result.toCharArray());
    }

    /**
     * Evaluate the redirectURI which may include the special baseURL variable. The baseURL is
     * constructed using information the incoming HTTP request.
     *
     * @param immediateOnly
     * @return
     */
    private String evaluateRedirectURI(boolean immediateOnly) {
        try {
            elHelper.addValue(JakartaSec30Constants.BASE_URL_VARIABLE, constructedBaseURL, false);
            return evaluateStringAttribute("redirectURI", oidcMechanismDefinition.redirectURI(), JakartaSec30Constants.BASE_URL_DEFAULT, immediateOnly);
        } finally {
            elHelper.removeValue(JakartaSec30Constants.BASE_URL_VARIABLE);
        }
    }

    private Boolean evaluateRedirectToOriginalResource(boolean immediateOnly) {
        return evaluateBooleanAttribute("redirectToOriginalResourceExpression", oidcMechanismDefinition.redirectToOriginalResource(), false,
                                        oidcMechanismDefinition.redirectToOriginalResourceExpression(), immediateOnly);
    }

    @SuppressWarnings("static-access")
    @FFDCIgnore(IllegalArgumentException.class)
    private String[] evaluateScope(boolean immediateOnly) {
        String scopeExpression = oidcMechanismDefinition.scopeExpression();
        try {
            return elHelper.processStringArray("scopeExpression", scopeExpression, oidcMechanismDefinition.scope(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (immediateOnly && elHelper.isDeferredExpression(scopeExpression)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "scopeExpression", "Returning null since scopeExpression is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            issueWarningMessage("scopeExpression", EMPTY_DEFAULT);

            return new String[] { OpenIdConstant.OPENID_SCOPE, OpenIdConstant.EMAIL_SCOPE, OpenIdConstant.PROFILE_SCOPE }; /* Default value from spec. */
        }
    }

    private String evaluateResponseType(boolean immediateOnly) {
        return evaluateStringAttribute("responseType", oidcMechanismDefinition.responseType(), OpenIdConstant.CODE, immediateOnly);
    }

    private String evaluateResponseMode(boolean immediateOnly) {
        return evaluateStringAttribute("responseMode", oidcMechanismDefinition.responseMode(), EMPTY_DEFAULT, immediateOnly);
    }

    @SuppressWarnings("static-access")
    @FFDCIgnore(IllegalArgumentException.class)
    private PromptType[] evaluatePrompt(boolean immediateOnly) {
        String promptExpression = oidcMechanismDefinition.promptExpression();
        try {
            return elHelper.processGeneric("promptExpression", promptExpression, oidcMechanismDefinition.prompt(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (immediateOnly && elHelper.isDeferredExpression(promptExpression)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "promptExpression",
                             "Returning null since promptExpression is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            issueWarningMessage("promptExpression", EMPTY_DEFAULT);

            return new PromptType[] {}; /* Default value from spec. */
        }
    }

    @SuppressWarnings("static-access")
    @FFDCIgnore(IllegalArgumentException.class)
    private DisplayType evaluateDisplay(boolean immediateOnly) {
        String displayExpression = oidcMechanismDefinition.displayExpression();
        try {
            return elHelper.processGeneric("displayExpression", displayExpression, oidcMechanismDefinition.display(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (immediateOnly && elHelper.isDeferredExpression(displayExpression)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "displayExpression",
                             "Returning null since displayExpression is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            issueWarningMessage("displayExpression", EMPTY_DEFAULT);

            return DisplayType.PAGE; /* Default value from spec. */
        }
    }

    private Boolean evaluateUseNonce(boolean immediateOnly) {
        return evaluateBooleanAttribute("useNonceExpression", oidcMechanismDefinition.useNonce(), true,
                                        oidcMechanismDefinition.useNonceExpression(), immediateOnly);
    }

    private Boolean evaluateUseSession(boolean immediateOnly) {
        return evaluateBooleanAttribute("useSessionExpression", oidcMechanismDefinition.useSession(), true,
                                        oidcMechanismDefinition.useSessionExpression(), immediateOnly);
    }

    @SuppressWarnings("static-access")
    @FFDCIgnore(IllegalArgumentException.class)
    private String[] evaluateExtraParameters(boolean immediateOnly) {
        String extraParametersExpression = oidcMechanismDefinition.extraParametersExpression();
        try {
            return elHelper.processStringArray("extraParametersExpression", extraParametersExpression, oidcMechanismDefinition.extraParameters(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (immediateOnly && elHelper.isDeferredExpression(extraParametersExpression)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "extraParametersExpression", "Returning null since extraParametersExpression is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAKARTASEC_WARNING_OIDC_MECH_CONFIG", new Object[] { "extraParametersExpression", "" });
            }
            return new String[] {}; /* Default value from spec. */
        }
    }

    private Integer evaluateJwksConnectTimeout(boolean immediateOnly) {
        return evaluateIntegerAttribute("jwksConnectTimeoutExpression", oidcMechanismDefinition.jwksConnectTimeout(), 500,
                                        oidcMechanismDefinition.jwksConnectTimeoutExpression(), immediateOnly);
    }

    private Integer evaluateJwksReadTimeout(boolean immediateOnly) {
        return evaluateIntegerAttribute("jwksReadTimeoutExpression", oidcMechanismDefinition.jwksReadTimeout(), 500,
                                        oidcMechanismDefinition.jwksReadTimeoutExpression(), immediateOnly);
    }

    private Boolean evaluateTokenAutoRefresh(boolean immediateOnly) {
        return evaluateBooleanAttribute("tokenAutoRefreshExpression", oidcMechanismDefinition.tokenAutoRefresh(), false,
                                        oidcMechanismDefinition.tokenAutoRefreshExpression(), immediateOnly);
    }

    private Integer evaluateTokenMinValidity(boolean immediateOnly) {
        return evaluateIntegerAttribute("tokenMinValidityExpression", oidcMechanismDefinition.tokenMinValidity(), 10000,
                                        oidcMechanismDefinition.tokenMinValidityExpression(), immediateOnly);
    }

    @SuppressWarnings("static-access")
    @FFDCIgnore(IllegalArgumentException.class)
    private String evaluateStringAttribute(String attributeName, String attribute, String attributeDefault, boolean immediateOnly) {
        try {
            return elHelper.processString(attributeName, attribute, immediateOnly);
        } catch (IllegalArgumentException e) {
            if (immediateOnly && elHelper.isDeferredExpression(attribute)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, attributeName, "Returning null since " + attributeName + " is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            issueWarningMessage(attributeName, attributeDefault);

            return attributeDefault;
        }
    }

    @SuppressWarnings("static-access")
    @FFDCIgnore(IllegalArgumentException.class)
    private Boolean evaluateBooleanAttribute(String attributeName, boolean attribute, boolean attributeDefault, String attributeExpression, boolean immediateOnly) {
        try {
            return elHelper.processBoolean(attributeName, attributeExpression, attribute, immediateOnly);
        } catch (IllegalArgumentException e) {
            if (immediateOnly && elHelper.isDeferredExpression(attributeExpression)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, attributeName, "Returning null since " + attributeName + " is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            issueWarningMessage(attributeName, attributeDefault);

            return attributeDefault;
        }
    }

    @SuppressWarnings("static-access")
    @FFDCIgnore(IllegalArgumentException.class)
    private Integer evaluateIntegerAttribute(String attributeName, int attribute, int attributeDefault, String attributeExpression, boolean immediateOnly) {
        try {
            return elHelper.processInt(attributeName, attributeExpression, attribute, immediateOnly);
        } catch (IllegalArgumentException e) {
            if (immediateOnly && elHelper.isDeferredExpression(attributeExpression)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, attributeName, "Returning null since " + attributeName + " is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            issueWarningMessage(attributeName, attributeDefault);

            return attributeDefault;
        }
    }

    private void issueWarningMessage(String attributeName, Object attributeDefault) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
            Tr.warning(tc, "JAKARTASEC_WARNING_OIDC_MECH_CONFIG", new Object[] { attributeName, attributeDefault });
        }
    }

    @Override
    public String getProviderURI() {
        return (providerURI != null) ? providerURI : evaluateProviderURI(false);
    }

    @Override
    public OidcProviderMetadata getProviderMetadata() {
        return providerMetadataWrapper;
    }

    @Override
    public String getClientId() {
        return (clientId != null) ? clientId : evaluateClientId(false);
    }

    @Override
    public ProtectedString getClientSecret() {
        return (clientSecret != null) ? clientSecret : evaluateClientSecret(false);
    }

    @Override
    public ClaimsMappingConfig getClaimsMappingConfig() {
        return claimsDefinitionWrapper;
    }

    @Override
    public LogoutConfig getLogoutConfig() {
        return logoutDefinitionWrapper;
    }

    @Override
    public String getRedirectURI() {
        return (redirectURI != null) ? redirectURI : evaluateRedirectURI(false);
    }

    @Override
    public boolean isRedirectToOriginalResource() {
        return (redirectToOriginalResource != null) ? redirectToOriginalResource : evaluateRedirectToOriginalResource(false);
    }

    // TODO: Optimize for performance
    @Override
    public Set<String> getScope() {
        Set<String> scopeSet = new LinkedHashSet<String>();
        String[] tempScopes = (scope != null) ? scope : evaluateScope(false);

        for (String tempScope : tempScopes) {
            scopeSet.add(tempScope);
        }

        return scopeSet;
    }

    @Override
    public String getResponseType() {
        return (responseType != null) ? responseType : evaluateResponseType(false);
    }

    @Override
    public String getResponseMode() {
        return (responseMode != null) ? responseMode : evaluateResponseMode(false);
    }

    /*
     * Convert PrompType[] to expected
     * "Space delimited, case sensitive list of ASCII string values that specifies whether the Authorization Server prompts the End-User for reauthentication and consent."
     * per https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest
     */
    @Override
    public String getPromptParameter() {
        PromptType[] promptTypes = (prompt != null) ? prompt : evaluatePrompt(false);

        StringBuffer sb = new StringBuffer();
        for (PromptType promptType : promptTypes) {
            if (sb.length() != 0) {
                sb.append(" ").append(promptType);
            } else {
                sb.append(promptType);
            }
        }

        return sb.toString();
    }

    /*
     * Convert DisplayType to expected
     * "ASCII string value that specifies how the Authorization Server displays the authentication and consent user interface pages to the End-User."
     * per https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest
     */
    @Override
    public String getDisplayParameter() {
        return (display != null) ? display.name().toLowerCase() : evaluateDisplay(false).name().toLowerCase();
    }

    @Override
    public boolean isUseNonce() {
        return (useNonce != null) ? useNonce : evaluateUseNonce(false);
    }

    @Override
    public boolean isUseSession() {
        return (useSession != null) ? useSession : evaluateUseSession(false);
    }

    @Override
    public String[] getExtraParameters() {
        return (extraParameters != null) ? extraParameters : evaluateExtraParameters(false);
    }

    @Override
    public int getJwksConnectTimeout() {
        return (jwksConnectTimeout != null) ? jwksConnectTimeout : evaluateJwksConnectTimeout(false);
    }

    @Override
    public int getJwksReadTimeout() {
        return (jwksReadTimeout != null) ? jwksReadTimeout : evaluateJwksReadTimeout(false);
    }

    @Override
    public boolean isTokenAutoRefresh() {
        return (tokenAutoRefresh != null) ? tokenAutoRefresh : evaluateTokenAutoRefresh(false);
    }

    @Override
    public int getTokenMinValidity() {
        return (tokenMinValidity != null) ? tokenMinValidity : evaluateTokenMinValidity(false);
    }

}