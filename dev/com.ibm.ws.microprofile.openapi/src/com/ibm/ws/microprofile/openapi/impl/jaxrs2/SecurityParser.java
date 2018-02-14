/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.openapi.impl.jaxrs2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.security.OAuthScope;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;
import org.eclipse.microprofile.openapi.models.security.Scopes;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import com.ibm.ws.microprofile.openapi.impl.model.security.OAuthFlowImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.OAuthFlowsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.ScopesImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.SecurityRequirementImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.SecuritySchemeImpl;

public class SecurityParser {

    public static Optional<List<SecurityRequirement>> getSecurityRequirements(org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement[] securityRequirementsApi) {
        if (securityRequirementsApi == null || securityRequirementsApi.length == 0) {
            return Optional.empty();
        }
        List<SecurityRequirement> securityRequirements = new ArrayList<>();
        for (org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement securityRequirementApi : securityRequirementsApi) {
            SecurityRequirement securityRequirement = new SecurityRequirementImpl();
            if (securityRequirementApi.scopes().length > 0) {
                securityRequirement.addScheme(securityRequirementApi.name(), Arrays.asList(securityRequirementApi.scopes()));
            } else {
                securityRequirement.addScheme(securityRequirementApi.name());
            }
            securityRequirements.add(securityRequirement);
        }
        if (securityRequirements.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(securityRequirements);
    }

    public static Optional<SecurityScheme> getSecurityScheme(org.eclipse.microprofile.openapi.annotations.security.SecurityScheme securityScheme) {
        if (securityScheme == null) {
            return Optional.empty();
        }
        SecurityScheme securitySchemeObject = new SecuritySchemeImpl();

        if (StringUtils.isNotBlank(securityScheme.ref())) {
            securitySchemeObject.setRef(securityScheme.ref());
        }
        if (StringUtils.isNotBlank(securityScheme.in().toString())) {
            securitySchemeObject.setIn(getIn(securityScheme.in().toString()));
        }
        if (StringUtils.isNotBlank(securityScheme.type().toString())) {
            securitySchemeObject.setType(getType(securityScheme.type().toString()));
        }

        if (StringUtils.isNotBlank(securityScheme.openIdConnectUrl())) {
            securitySchemeObject.setOpenIdConnectUrl(securityScheme.openIdConnectUrl());
        }
        if (StringUtils.isNotBlank(securityScheme.scheme())) {
            securitySchemeObject.setScheme(securityScheme.scheme());
        }

        if (StringUtils.isNotBlank(securityScheme.bearerFormat())) {
            securitySchemeObject.setBearerFormat(securityScheme.bearerFormat());
        }
        if (StringUtils.isNotBlank(securityScheme.description())) {
            securitySchemeObject.setDescription(securityScheme.description());
        }
        if (StringUtils.isNotBlank(securityScheme.securitySchemeName())) {
            ((SecuritySchemeImpl) securitySchemeObject).setSchemeName(securityScheme.securitySchemeName());
        }

        if (StringUtils.isNotBlank(securityScheme.apiKeyName())) {
            securitySchemeObject.setName(securityScheme.apiKeyName());
        }
        getOAuthFlows(securityScheme.flows()).ifPresent(securitySchemeObject::setFlows);
        return Optional.of(securitySchemeObject);
    }

    public static Optional<OAuthFlows> getOAuthFlows(org.eclipse.microprofile.openapi.annotations.security.OAuthFlows oAuthFlows) {
        if (isEmpty(oAuthFlows)) {
            return Optional.empty();
        }
        OAuthFlows oAuthFlowsObject = new OAuthFlowsImpl();
        getOAuthFlow(oAuthFlows.authorizationCode()).ifPresent(oAuthFlowsObject::setAuthorizationCode);
        getOAuthFlow(oAuthFlows.clientCredentials()).ifPresent(oAuthFlowsObject::setClientCredentials);
        getOAuthFlow(oAuthFlows.implicit()).ifPresent(oAuthFlowsObject::setImplicit);
        getOAuthFlow(oAuthFlows.password()).ifPresent(oAuthFlowsObject::setPassword);
        return Optional.of(oAuthFlowsObject);
    }

    public static Optional<OAuthFlow> getOAuthFlow(org.eclipse.microprofile.openapi.annotations.security.OAuthFlow oAuthFlow) {
        if (isEmpty(oAuthFlow)) {
            return Optional.empty();
        }
        OAuthFlow oAuthFlowObject = new OAuthFlowImpl();
        if (StringUtils.isNotBlank(oAuthFlow.authorizationUrl())) {
            oAuthFlowObject.setAuthorizationUrl(oAuthFlow.authorizationUrl());
        }
        if (StringUtils.isNotBlank(oAuthFlow.refreshUrl())) {
            oAuthFlowObject.setRefreshUrl(oAuthFlow.refreshUrl());
        }
        if (StringUtils.isNotBlank(oAuthFlow.tokenUrl())) {
            oAuthFlowObject.setTokenUrl(oAuthFlow.tokenUrl());
        }
        getScopes(oAuthFlow.scopes()).ifPresent(oAuthFlowObject::setScopes);
        return Optional.of(oAuthFlowObject);
    }

    public static Optional<Scopes> getScopes(OAuthScope[] scopes) {
        if (isEmpty(scopes)) {
            return Optional.empty();
        }
        Scopes scopesObject = new ScopesImpl();

        for (OAuthScope scope : scopes) {
            scopesObject.addScope(scope.name(), scope.description());
        }
        return Optional.of(scopesObject);
    }

    private static SecurityScheme.In getIn(String value) {
        return Arrays.stream(SecurityScheme.In.values()).filter(i -> i.toString().equals(value)).findFirst().orElse(null);
    }

    private static SecurityScheme.Type getType(String value) {
        return Arrays.stream(SecurityScheme.Type.values()).filter(i -> i.toString().equals(value)).findFirst().orElse(null);
    }

    private static boolean isEmpty(org.eclipse.microprofile.openapi.annotations.security.OAuthFlows oAuthFlows) {
        if (oAuthFlows == null) {
            return true;
        }
        if (!isEmpty(oAuthFlows.implicit())) {
            return false;
        }
        if (!isEmpty(oAuthFlows.authorizationCode())) {
            return false;
        }
        if (!isEmpty(oAuthFlows.clientCredentials())) {
            return false;
        }
        if (!isEmpty(oAuthFlows.password())) {
            return false;
        }
        return true;
    }

    private static boolean isEmpty(org.eclipse.microprofile.openapi.annotations.security.OAuthFlow oAuthFlow) {
        if (oAuthFlow == null) {
            return true;
        }
        if (!StringUtils.isBlank(oAuthFlow.authorizationUrl())) {
            return false;
        }
        if (!StringUtils.isBlank(oAuthFlow.refreshUrl())) {
            return false;
        }
        if (!StringUtils.isBlank(oAuthFlow.tokenUrl())) {
            return false;
        }
        if (!isEmpty(oAuthFlow.scopes())) {
            return false;
        }
        return true;
    }

    private static boolean isEmpty(OAuthScope[] scopes) {
        if (scopes == null || scopes.length == 0) {
            return true;
        }
        return false;
    }

}
