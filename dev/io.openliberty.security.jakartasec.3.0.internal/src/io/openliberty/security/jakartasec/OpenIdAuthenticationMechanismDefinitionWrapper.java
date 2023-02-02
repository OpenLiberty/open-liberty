/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.security.jakartasec;

import static io.openliberty.security.jakartasec.JakartaSec30Constants.DEFAULT_TOKEN_MIN_VALIDITY;
import static io.openliberty.security.jakartasec.JakartaSec30Constants.EMPTY_DEFAULT;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.javaeesec.identitystore.ELHelper;

import io.openliberty.security.jakartasec.el.ELUtils;
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
     * @param baseURL The baseURL is an optional variable for the redirectURL and is constructed using information the incoming HTTP request.
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
        return ELUtils.evaluateStringAttribute("providerURI", oidcMechanismDefinition.providerURI(), EMPTY_DEFAULT, immediateOnly);
    }

    private String evaluateClientId(boolean immediateOnly) {
        return ELUtils.evaluateStringAttribute("clientId", oidcMechanismDefinition.clientId(), EMPTY_DEFAULT, immediateOnly);
    }

    @SuppressWarnings("static-access")
    @Sensitive
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

            issueWarningMessage("clientSecret", ELHelper.OBFUSCATED_STRING, EMPTY_DEFAULT);

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
            String redirectUri = ELUtils.evaluateStringAttribute("redirectURI", oidcMechanismDefinition.redirectURI(), JakartaSec30Constants.BASE_URL_DEFAULT, immediateOnly);

            // re-process the result of redirectUri in-case it returns another el expression containing ${baseURL}
            // we don't normally do this, but this is a special case found in jakarta security 3.0 tck requirements
            if (redirectUri != null && redirectUri.contains(JakartaSec30Constants.BASE_URL_VARIABLE)) {
                redirectUri = ELUtils.evaluateStringAttribute("redirectURI", redirectUri, JakartaSec30Constants.BASE_URL_DEFAULT, immediateOnly);
            }
            return redirectUri;
        } finally {
            elHelper.removeValue(JakartaSec30Constants.BASE_URL_VARIABLE);
        }
    }

    private Boolean evaluateRedirectToOriginalResource(boolean immediateOnly) {
        return ELUtils.evaluateBooleanAttribute("redirectToOriginalResourceExpression", oidcMechanismDefinition.redirectToOriginalResource(), false,
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

            String[] defaultScope = new String[] { OpenIdConstant.OPENID_SCOPE, OpenIdConstant.EMAIL_SCOPE, OpenIdConstant.PROFILE_SCOPE }; /* Default value from spec. */

            issueWarningMessage("scopeExpression", scopeExpression, Arrays.toString(defaultScope));

            return defaultScope;
        }
    }

    private String evaluateResponseType(boolean immediateOnly) {
        return ELUtils.evaluateStringAttribute("responseType", oidcMechanismDefinition.responseType(), OpenIdConstant.CODE, immediateOnly);
    }

    private String evaluateResponseMode(boolean immediateOnly) {
        return ELUtils.evaluateStringAttribute("responseMode", oidcMechanismDefinition.responseMode(), EMPTY_DEFAULT, immediateOnly);
    }

    @SuppressWarnings("static-access")
    @FFDCIgnore(IllegalArgumentException.class)
    private PromptType[] evaluatePrompt(boolean immediateOnly) {
        String promptExpression = oidcMechanismDefinition.promptExpression();
        PromptType[] promptArray = oidcMechanismDefinition.prompt();

        try {
            return processPrompt(promptExpression, promptArray, immediateOnly);
        } catch (IllegalArgumentException e) {
            if (immediateOnly && elHelper.isDeferredExpression(promptExpression)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "promptExpression",
                             "Returning null since promptExpression is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            issueWarningMessage("promptExpression", promptExpression, EMPTY_DEFAULT);

            return new PromptType[] {}; /* Default value from spec. */
        }
    }

    @SuppressWarnings("static-access")
    @FFDCIgnore(IllegalArgumentException.class)
    private DisplayType evaluateDisplay(boolean immediateOnly) {
        String displayExpression = oidcMechanismDefinition.displayExpression();
        try {
            return processDisplay(displayExpression, oidcMechanismDefinition.display(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (immediateOnly && elHelper.isDeferredExpression(displayExpression)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "displayExpression",
                             "Returning null since displayExpression is a deferred expression and this is called on initialization.");
                }
                return null;
            }

            issueWarningMessage("displayExpression", displayExpression, DisplayType.PAGE.toString());

            return DisplayType.PAGE; /* Default value from spec. */
        }
    }

    private Boolean evaluateUseNonce(boolean immediateOnly) {
        return ELUtils.evaluateBooleanAttribute("useNonceExpression", oidcMechanismDefinition.useNonce(), true,
                                                oidcMechanismDefinition.useNonceExpression(), immediateOnly);
    }

    private Boolean evaluateUseSession(boolean immediateOnly) {
        return ELUtils.evaluateBooleanAttribute("useSessionExpression", oidcMechanismDefinition.useSession(), true,
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
        return ELUtils.evaluateIntegerAttribute("jwksConnectTimeoutExpression", oidcMechanismDefinition.jwksConnectTimeout(), 500,
                                                oidcMechanismDefinition.jwksConnectTimeoutExpression(), immediateOnly);
    }

    private Integer evaluateJwksReadTimeout(boolean immediateOnly) {
        return ELUtils.evaluateIntegerAttribute("jwksReadTimeoutExpression", oidcMechanismDefinition.jwksReadTimeout(), 500,
                                                oidcMechanismDefinition.jwksReadTimeoutExpression(), immediateOnly);
    }

    private Boolean evaluateTokenAutoRefresh(boolean immediateOnly) {
        return ELUtils.evaluateBooleanAttribute("tokenAutoRefreshExpression", oidcMechanismDefinition.tokenAutoRefresh(), false,
                                                oidcMechanismDefinition.tokenAutoRefreshExpression(), immediateOnly);
    }

    private Integer evaluateTokenMinValidity(boolean immediateOnly) {
        return evaluateNonNegativeInteger("tokenMinValidity", oidcMechanismDefinition.tokenMinValidity(), DEFAULT_TOKEN_MIN_VALIDITY,
                                          "tokenMinValidityExpression", oidcMechanismDefinition.tokenMinValidityExpression(), immediateOnly);
    }

    private Integer evaluateNonNegativeInteger(String attributeName, int attribute, int attributeDefault,
                                               String attributeExpressionName, String attributeExpression, boolean immediateOnly) {
        Integer value = ELUtils.evaluateIntegerAttribute(attributeExpressionName, attribute, attributeDefault, attributeExpression, immediateOnly);
        if (value != null && value < 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                String attributeNameForWarning = attributeExpression.isEmpty() ? attributeName : attributeExpressionName;
                Tr.warning(tc, "JAKARTASEC_WARNING_OIDC_MECH_CONFIG_NEGATIVE_INT", new Object[] { attributeNameForWarning, value, attributeDefault });
            }
            value = attributeDefault;
        }
        return value;
    }

    private void issueWarningMessage(String attributeName, Object valueProvided, Object attributeDefault) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
            Tr.warning(tc, "JAKARTASEC_WARNING_OIDC_MECH_CONFIG", new Object[] { attributeName, valueProvided, attributeDefault });
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

    @Sensitive
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
                sb.append(" ").append(promptType.toString().toLowerCase());
            } else {
                sb.append(promptType.toString().toLowerCase());
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

    /**
     * Validate and return the {@link PromptType}s from either
     * the EL expression or the direct prompt setting.
     *
     * Similar helper classes are in ElHelper class directly, but did not want to import PromptType into
     * the ElHelper's project.
     *
     * @param promptExpression The EL expression .
     * @param prompt The non-EL value.
     * @param immediateOnly Return null if the value is a deferred EL expression.
     *
     * @return The validated useFor types.
     */
    protected PromptType[] processPrompt(String promptExpression, PromptType[] prompt, boolean immediateOnly) {
        PromptType[] result = null;
        boolean immediate = false;

        /*
         * The expression language value takes precedence over the direct setting.
         */
        if (promptExpression == null || promptExpression.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "promptExpression not provided, return promptType");
            }
            result = prompt;
            immediate = true; // no expression
        } else {
            /*
             * Evaluate the EL expression to get the value.
             */
            Object obj = elHelper.evaluateElExpression(promptExpression);
            if (obj instanceof PromptType[]) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processPrompt (promptType): " + obj);
                }
                result = (PromptType[]) obj;
                immediate = elHelper.isImmediateExpression(promptExpression);
            } else if (obj instanceof String) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processPrompt", "promptExpression evaluated to a String, attempt to split and compare to PromptType enum options: " + obj);
                }
                String[] splitReturn = ((String) obj).split(" ");
                Set<PromptType> types = new HashSet<PromptType>(splitReturn.length);
                for (String split : splitReturn) {
                    switch (PromptType.fromString(split)) {
                        case CONSENT:
                            types.add(PromptType.CONSENT);
                            break;
                        case LOGIN:
                            types.add(PromptType.LOGIN);
                            break;
                        case SELECT_ACCOUNT:
                            types.add(PromptType.SELECT_ACCOUNT);
                            break;
                        case NONE:
                            types.add(PromptType.NONE);
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid value provided in the promptExpression: " + split);
                    }
                }
                result = types.toArray(new PromptType[types.size()]);
                immediate = elHelper.isImmediateExpression(promptExpression);
            } else {
                throw new IllegalArgumentException("Expected 'promptExpression' to evaluate to an array of PromptType enum values: " + promptExpression);
            }
        }

        return (immediateOnly && !immediate) ? null : result;
    }

    /**
     * Validate and return the {@link DisplayType} from either
     * the EL expression or the direct prompt setting.
     *
     * Similar helper classes are in ElHelper class directly, but did not want to import DisplayType into
     * the ElHelper's project.
     *
     * @param displayExpression The EL expression.
     * @param display The non-EL value.
     * @param immediateOnly Return null if the value is a deferred EL expression.
     *
     * @return The validated useFor types.
     */
    protected DisplayType processDisplay(String displayExpression, DisplayType display, boolean immediateOnly) {
        DisplayType result = null;
        boolean immediate = false;

        /*
         * The expression language value takes precedence over the direct setting.
         */
        if (displayExpression == null || displayExpression.isEmpty()) {
            result = display;
            immediate = true; // no expression
        } else {
            /*
             * Evaluate the EL expression to get the value.
             */
            Object obj = elHelper.evaluateElExpression(displayExpression);
            if (obj instanceof DisplayType) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processPrompt (promptType): " + obj);
                }
                result = (DisplayType) obj;
                immediate = elHelper.isImmediateExpression(displayExpression);
            } else if (obj instanceof String) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processDisplay", "displayExpression evaluated to a String, compare to DisplayType enum options: " + obj);
                }
                String displayReturn = (String) obj;
                switch (DisplayType.fromString(displayReturn)) {
                    case PAGE:
                        result = DisplayType.PAGE;
                        break;
                    case POPUP:
                        result = DisplayType.POPUP;
                        break;
                    case TOUCH:
                        result = DisplayType.TOUCH;
                        break;
                    case WAP:
                        result = DisplayType.WAP;
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid value provided in the displayExpression: " + displayReturn);
                }

                immediate = elHelper.isImmediateExpression(displayExpression);
            } else {
                throw new IllegalArgumentException("Expected 'displayExpression' to evaluate to a DisplayType enum value: " + displayExpression);
            }
        }

        return (immediateOnly && !immediate) ? null : result;
    }
}