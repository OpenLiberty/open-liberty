/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.openapi31;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIParser;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent.Severity;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidator;
import com.ibm.ws.microprofile.openapi.impl.validation.ValidatorUtils;
import com.ibm.wsspi.openapi31.OASProvider;

public class OASProviderWrapper {

    private static final TraceComponent tc = Tr.register(OASProviderWrapper.class,com.ibm.ws.openapi31.TraceConstants.TRACE_GROUP, com.ibm.ws.openapi31.TraceConstants.TRACE_BUNDLE_CORE);

    private final OASProvider provider;

    private final boolean isWebProvider;
    private Boolean isValid = null;

    private OpenAPI document = null;

    private final Set<String> jsonPathKeys = new HashSet<>();

    public OASProviderWrapper(OASProvider provider) {
        this.provider = provider;
        this.isWebProvider = provider instanceof OpenAPIWebProvider;
    }

    public OASProvider getOpenAPIProvider() {
        return this.provider;
    }

    public synchronized OpenAPI getOpenAPI() {
        if (this.document != null) {
            return this.document;
        }
        OpenAPI doc = this.provider.getOpenAPIModel();
        if (doc == null) {
            String openAPIString = this.provider.getOpenAPIDocument();
            if (openAPIString != null && !openAPIString.isEmpty()) {
                try {
                    SwaggerParseResult result = new OpenAPIParser().readContents(openAPIString, null, null, null);
                    if (result != null && result.getOpenAPI() != null) {
                        doc = result.getOpenAPI();
                    }
                } catch (Exception e) {
                    Tr.error(tc, "OPENAPI_FILE_PARSE_ERROR", provider.getContextRoot());
                }
            }
        }
        if (doc == null && !(this.provider instanceof OpenAPIWebProvider)) {
            Tr.warning(tc, "OPENAPI_IS_NULL", provider);
        }
        this.document = doc;
        return this.document;
    }

    public Set<String> getJsonPathKeys() {
        return this.jsonPathKeys;
    }

    public boolean getIsWebProvider() {
        return this.isWebProvider;
    }

    public boolean isPublic() {
        return provider.isPublic();
    }

    /**
     * @return
     */
    public String getContextRoot() {
        return provider.getContextRoot();
    }

    /**
     * @return
     */
    public synchronized boolean validate() {
        if (this.isValid == null)
            validateDocument(this.document);
        return this.isValid;
    }

    @Trivial
    private void validateDocument(OpenAPI document) {
        this.isValid = true;
        final OASValidator validator = new OASValidator();
        final OASValidationResult result = validator.validate(document);
        final StringBuilder sbError = new StringBuilder();
        final StringBuilder sbWarnings = new StringBuilder();
        if (result.hasEvents()) {
            result.getEvents().stream().forEach(v -> {
                final String message = ValidatorUtils.formatMessage("validationMessage", v.message, v.location);
                if (v.severity == Severity.ERROR) {
                    sbError.append("\n - " + message);
                } else if (v.severity == Severity.WARNING) {
                    sbWarnings.append("\n - " + message);
                }
            });

            String errors = sbError.toString();
            if (!errors.isEmpty()) {
                this.isValid = false;
                Tr.error(tc, "OPENAPI_DOCUMENT_VALIDATION_ERROR", errors + "\n");
            }

            String warnings = sbWarnings.toString();
            if (!warnings.isEmpty()) {
                Tr.warning(tc, "OPENAPI_DOCUMENT_VALIDATION_WARNING", warnings + "\n");
            }
        }
    }

}
