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
package io.openliberty.microprofile.openapi20.validation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;
import io.smallrye.openapi.runtime.io.server.ServerConstant;

/**
 *
 */
public class ServerValidator extends TypeValidator<Server> {

    private static final TraceComponent tc = Tr.register(ServerValidator.class);

    private static final ServerValidator INSTANCE = new ServerValidator();

    public static ServerValidator getInstance() {
        return INSTANCE;
    }

    private ServerValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Server t) {
        String url = t.getUrl();

        if (url != null) {
            Set<String> variables = validateURL(helper, context, url);
            //url can be relative - so shouldn't check for URL format here
            validateServerVariables(helper, context, variables, t);
        } else {
            ValidatorUtils.validateRequiredField(url, context, ServerConstant.PROP_URL).ifPresent(helper::addValidationEvent);

        }

    }

    /**
     * Ensures that all the serverVariables are defined
     *
     * @param helper the helper to send validation messages
     * @param variables the set of variables to validate
     */
    private void validateServerVariables(ValidationHelper helper, Context context, Set<String> variables, Server t) {
        Map<String, ServerVariable> serverVariables = t.getVariables();

        for (String variable : variables) {
            if (serverVariables == null || !serverVariables.containsKey(variable)) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.SERVER_VARIABLE_NOT_DEFINED, variable);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(ServerConstant.PROP_VARIABLES), message));
            }
        }
    }

    /**
     * Validate the url and extract server variables parameters
     */
    private Set<String> validateURL(ValidationHelper helper, Context context, String url) {
        String pathToCheck = url;
        Set<String> serverVariables = new HashSet<String>();

        while (pathToCheck.contains("{")) {
            if (!pathToCheck.contains("}")) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.SERVER_INVALID_URL, pathToCheck);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(ServerConstant.PROP_URL), message));
                return serverVariables;
            }
            int firstIndex = pathToCheck.indexOf("{");
            int lastIndex = pathToCheck.indexOf("}");

            if (firstIndex > lastIndex) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.SERVER_INVALID_URL, pathToCheck);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(ServerConstant.PROP_URL), message));
                return serverVariables;
            }

            String variable = pathToCheck.substring(firstIndex + 1, lastIndex);

            if (variable.isEmpty() || variable.contains("{") || variable.contains("/")) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.SERVER_INVALID_URL, pathToCheck);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(ServerConstant.PROP_URL), message));
                return serverVariables;
            }

            serverVariables.add(variable);
            pathToCheck = pathToCheck.substring(lastIndex + 1);
        }

        if (pathToCheck.contains("}")) {
            final String message = Tr.formatMessage(tc, ValidationMessageConstants.SERVER_INVALID_URL, pathToCheck);
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(ServerConstant.PROP_URL), message));
            return serverVariables;
        }

        return serverVariables;
    }
}
