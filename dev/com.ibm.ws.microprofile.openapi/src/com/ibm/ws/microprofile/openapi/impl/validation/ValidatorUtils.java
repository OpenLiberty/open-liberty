/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.impl.validation;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent.Severity;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class ValidatorUtils {

    private static final TraceComponent tc = Tr.register(ValidatorUtils.class);

    public static <T> Optional<ValidationEvent> validateRequiredField(T value, String location, String fieldName) {
        boolean isValid = true;

        if (value == null) {
            isValid = false;
        } else {
            if (value instanceof String && String.valueOf(value).isEmpty()) {
                isValid = false;
            }
            if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) {
                isValid = false;
            }
        }

        if (!isValid) {
            final String message = Tr.formatMessage(tc, "requiredFieldMissing", location, fieldName);
            ValidationEvent event = new ValidationEvent(Severity.ERROR, location, message);
            return Optional.of(event);
        }
        return Optional.empty();
    }

    @FFDCIgnore({ MalformedURLException.class })
    public static boolean isValidURL(String urlStr) {
        try {
            @SuppressWarnings("unused")
            URL url = new URL(urlStr);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static boolean isValidURI(String uriStr) {
        try {
            @SuppressWarnings("unused")
            URI url = new URI(uriStr);
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static boolean flowsIsSet(OAuthFlows flows) {

        if (flows != null) {
            OAuthFlow implicit = flows.getImplicit();
            OAuthFlow authCode = flows.getAuthorizationCode();
            OAuthFlow clientCred = flows.getClientCredentials();
            OAuthFlow password = flows.getPassword();
            if (implicit != null) {
                if (StringUtils.isNotBlank(implicit.getAuthorizationUrl()) ||
                    StringUtils.isNotBlank(implicit.getRefreshUrl()) ||
                    implicit.getScopes() != null ||
                    implicit.getExtensions() != null) {
                    return true;
                }
            }
            if (authCode != null) {
                if (StringUtils.isNotBlank(authCode.getTokenUrl()) ||
                    StringUtils.isNotBlank(authCode.getAuthorizationUrl()) ||
                    StringUtils.isNotBlank(implicit.getRefreshUrl()) ||
                    authCode.getScopes() != null ||
                    authCode.getExtensions() != null) {
                    return true;
                }
            }
            if (clientCred != null) {
                if (StringUtils.isNotBlank(clientCred.getTokenUrl()) ||
                    StringUtils.isNotBlank(clientCred.getRefreshUrl()) ||
                    StringUtils.isNotBlank(clientCred.getAuthorizationUrl()) ||
                    clientCred.getScopes() != null ||
                    clientCred.getExtensions() != null) {
                    return true;
                }
            }
            if (password != null) {
                if (StringUtils.isNotBlank(password.getTokenUrl()) ||
                    StringUtils.isNotBlank(clientCred.getRefreshUrl()) ||
                    StringUtils.isNotBlank(clientCred.getAuthorizationUrl()) ||
                    password.getScopes() != null ||
                    password.getExtensions() != null) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }

    public static void referenceValidatorHelper(String reference, Object t, ValidationHelper helper, Context context, String key) {
        ReferenceValidator referenceValidator = ReferenceValidator.getInstance();
        Object component = referenceValidator.validate(helper, context, key, reference);
        if (!t.getClass().isInstance(component)) {
            final String message = Tr.formatMessage(tc, "referenceToObjectInvalid", reference, t.getClass().getName());
            helper.addValidationEvent(new ValidationEvent(Severity.ERROR, null, message));
        }
    }

}
