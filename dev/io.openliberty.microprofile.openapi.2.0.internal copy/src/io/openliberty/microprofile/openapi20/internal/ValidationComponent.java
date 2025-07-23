/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal;

import java.util.List;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult;
import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult.ValidationEvent;
import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult.ValidationEvent.Severity;
import io.openliberty.microprofile.openapi20.internal.services.OASValidator;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIModelOperations;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIVersionConfig;
import io.openliberty.microprofile.openapi20.internal.utils.MessageConstants;
import io.openliberty.microprofile.openapi20.internal.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.internal.validation.ValidatorUtils;

/**
 * Calls {@link OASValidator} services to validate a model and reports the validation result in the log.
 * <p>
 * The {@code OASValidator} to use is determined based on the OpenAPI spec version used by the model.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = ValidationComponent.class)
public class ValidationComponent {

    private static final TraceComponent tc = Tr.register(ValidationComponent.class);

    private static final String VALIDATOR_REF = "validator";

    protected ComponentContext ctx;

    @Reference(name = VALIDATOR_REF)
    protected volatile List<ServiceReference<OASValidator>> validators;

    @Reference
    protected OpenAPIModelOperations modelOperations;

    @Reference
    protected OpenAPIVersionConfig versionConfig;

    @Activate
    private void activate(ComponentContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Validate an Open API model and report the results in the messages log
     *
     * @param model the model to validate
     */
    public void validateAndReportErrors(OpenAPI model) {
        OASValidationResult result = validateModel(model);
        reportResults(result);
    }

    /**
     * Validate an OpenAPI model
     *
     * @param model the model to validate
     * @return the validation result
     */
    public OASValidationResult validateModel(OpenAPI model) {
        model = modelOperations.shallowCopy(model);
        versionConfig.applyConfig(model);

        String modelVersion = model.getOpenapi();

        if (modelVersion == null) {
            modelVersion = ""; // Avoid nulls
        }

        // Find and call a validator for the the model version
        OASValidationResult result = null;
        for (ServiceReference<OASValidator> ref : validators) {
            String validatorVersion = (String) ref.getProperty("openapi.version");
            if (modelVersion.startsWith(validatorVersion)) {
                OASValidator validator = ctx.locateService(VALIDATOR_REF, ref);
                result = validator.validate(model);
                break;
            }
        }

        // If we didn't find a validator, report error for invalid version
        if (result == null) {
            result = new OASValidationResult();
            String message = ValidatorUtils.formatMessage(ValidationMessageConstants.OPENAPI_VERSION_INVALID, modelVersion);
            result.getEvents().add(new ValidationEvent(Severity.ERROR, "#", message));
        }
        return result;
    }

    private void reportResults(OASValidationResult result) {
        final StringBuilder sbError = new StringBuilder();
        final StringBuilder sbWarnings = new StringBuilder();
        if (result.hasEvents()) {
            result.getEvents().stream().forEach(v -> {
                final String message = ValidatorUtils.formatMessage(ValidationMessageConstants.VALIDATION_MESSAGE, v.message, v.location);
                if (v.severity == Severity.ERROR) {
                    sbError.append("\n - " + message);
                } else if (v.severity == Severity.WARNING) {
                    sbWarnings.append("\n - " + message);
                }
            });

            String errors = sbError.toString();
            if (!errors.isEmpty()) {
                Tr.error(tc, MessageConstants.OPENAPI_DOCUMENT_VALIDATION_ERROR, errors + "\n");
            }

            String warnings = sbWarnings.toString();
            if (!warnings.isEmpty()) {
                Tr.warning(tc, MessageConstants.OPENAPI_DOCUMENT_VALIDATION_WARNING, warnings + "\n");
            }
        }
    }

}
