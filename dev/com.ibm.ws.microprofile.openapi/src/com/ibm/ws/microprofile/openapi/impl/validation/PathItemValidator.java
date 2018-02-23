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
package com.ibm.ws.microprofile.openapi.impl.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

/**
 *
 */
public class PathItemValidator extends TypeValidator<PathItem> {

    private static final TraceComponent tc = Tr.register(PathItemValidator.class);

    private static final PathItemValidator INSTANCE = new PathItemValidator();

    public static PathItemValidator getInstance() {
        return INSTANCE;
    }

    private PathItemValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, PathItem t) {

        if (t != null) {

            String ref = t.getRef();
            if (ref != null && ref.startsWith("#")) {
                final String message = Tr.formatMessage(tc, "pathItemInvalidRef", ref, key);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            if (key.contains("{$")) {
                //Path within a Callback can contain variables (e.g. {$request.query.callbackUrl}/data ) which shouldn't be validated since they are not path params
                if (OpenAPIUtils.isDebugEnabled(tc)) {
                    Tr.debug(tc, "Path contains variables. Skip validation: " + key);
                }
                return;
            }

            validateParameters(helper, context, key, t);
        }
    }

    private void validateParameters(ValidationHelper helper, Context context, String pathStr, PathItem path) {
        Set<String> definedSharedPathParameters = new HashSet<String>(), definedSharedQueryParameters = new HashSet<String>(),
                        definedSharedHeaderParameters = new HashSet<String>(),
                        definedSharedCookieParameters = new HashSet<String>();
        List<Parameter> sharedParameters = path.getParameters();
        if (sharedParameters != null) {
            for (Parameter param : sharedParameters) {

                Parameter parameter = param;
                String reference = parameter.getRef();
                if (reference != null && !reference.isEmpty()) {
                    Object componentItem = ReferenceValidator.getInstance().validate(helper, context, null, reference);
                    if (parameter.getClass().isInstance(componentItem)) {
                        parameter = (Parameter) componentItem;
                    }
                }

                if (isPathParameter(parameter)) {
                    if (!parameter.getRequired()) { //Path parameters must have the 'required' property set to true
                        final String message = Tr.formatMessage(tc, "pathItemRequiredField", parameter.getName(), pathStr);
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                    }
                }

                if ((isPathParameter(parameter) && !definedSharedPathParameters.add(parameter.getName()))
                    || (isQueryParameter(parameter) && !definedSharedQueryParameters.add(parameter.getName()))
                    || (isHeaderParameter(parameter) && !definedSharedHeaderParameters.add(parameter.getName()))
                    || (isCookieParameter(parameter) && !definedSharedCookieParameters.add(parameter.getName()))) {
                    final String message = Tr.formatMessage(tc, "pathItemDuplicate", pathStr, parameter.getIn(), parameter.getName());
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                }
            }
        }

        Set<String> declaredPathParameters = validatePathAndRetrievePathParams(helper, context, pathStr);

        if (!declaredPathParameters.containsAll(definedSharedPathParameters)) {
            Set<String> undeclaredParameters = new HashSet<String>(definedSharedPathParameters);
            undeclaredParameters.removeAll(declaredPathParameters);
            boolean isMultiple = undeclaredParameters.size() > 1;
            final String message;
            if (isMultiple) {
                message = Tr.formatMessage(tc, "pathItemParameterNotDeclaredMultiple", pathStr, undeclaredParameters.size(), undeclaredParameters);
            } else {
                message = Tr.formatMessage(tc, "pathItemParameterNotDeclaredSingle", pathStr, undeclaredParameters);
            }
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
        }

        final Map<PathItem.HttpMethod, Operation> operationMap = path.readOperationsMap();

        if (operationMap != null) {
            for (PathItem.HttpMethod httpMethod : operationMap.keySet()) {
                Operation operation = operationMap.get(httpMethod);
                validateOperationParameters(helper, context, operation, declaredPathParameters, definedSharedPathParameters, pathStr, httpMethod.toString());
            }
        }
    }

    private void validateOperationParameters(ValidationHelper helper, Context context, Operation operation, Set<String> declaredPathParameters,
                                             Set<String> definedSharedPathParams, String path, String operationType) {
        Set<String> definedPathParameters = new HashSet<String>(), definedQueryParameters = new HashSet<String>(),
                        definedHeaderParameters = new HashSet<String>(), definedCookieParameters = new HashSet<String>();

        List<Parameter> parameters = operation.getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            for (Parameter param : parameters) {
                if (param != null) {

                    Parameter parameter = param;
                    String reference = parameter.getRef();
                    if (reference != null && !reference.isEmpty()) {
                        Object componentItem = ReferenceValidator.getInstance().validate(helper, context, null, reference);
                        if (parameter.getClass().isInstance(componentItem)) {
                            parameter = (Parameter) componentItem;
                        }
                    }

                    if (isPathParameter(parameter)) {
                        if (!parameter.getRequired()) {//Path parameters must have the 'required' property set to true
                            final String message = Tr.formatMessage(tc, "pathItemOperationRequiredField", parameter.getName(), operationType, path);
                            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                        }
                    }

                    if ((isPathParameter(parameter) && !definedPathParameters.add(parameter.getName()))
                        || (isQueryParameter(parameter) && !definedQueryParameters.add(parameter.getName()))
                        || (isHeaderParameter(parameter) && !definedHeaderParameters.add(parameter.getName()))
                        || (isCookieParameter(parameter) && !definedCookieParameters.add(parameter.getName()))) {
                        final String message = Tr.formatMessage(tc, "pathItemOperationDuplicate", operationType, path, parameter.getIn(), parameter.getName());
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                    }
                } else {
                    final String message = Tr.formatMessage(tc, "pathItemOperationNullParameter", operationType, path);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                }
            }
        }

        if (!declaredPathParameters.containsAll(definedPathParameters)) {
            Set<String> undeclaredParameters = new HashSet<String>(definedPathParameters);
            undeclaredParameters.removeAll(declaredPathParameters);
            boolean isMultiple = undeclaredParameters.size() > 1;
            final String message;
            if (isMultiple) {
                message = Tr.formatMessage(tc, "pathItemOperationParameterNotDeclaredMultiple", operationType, path, undeclaredParameters.size(), undeclaredParameters);
            } else {
                message = Tr.formatMessage(tc, "pathItemOperationParameterNotDeclaredSingle", operationType, path, undeclaredParameters);
            }
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
        }

        for (String declaredParam : declaredPathParameters) {
            if (!definedSharedPathParams.contains(declaredParam) && !definedPathParameters.contains(declaredParam)) {
                final String message = Tr.formatMessage(tc, "pathItemOperationNoPathParameterDeclared", operationType, path, declaredParam);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }
        }
    }

    /**
     * @param parameter
     * @return true if the input is a cookie parameter
     */
    private boolean isCookieParameter(Parameter parameter) {
        return In.COOKIE == parameter.getIn();
    }

    /**
     * @param parameter
     * @return true if the input is a header parameter
     */
    private boolean isHeaderParameter(Parameter parameter) {
        return In.HEADER == parameter.getIn();
    }

    /**
     * @param parameter
     * @return true if the input is a query parameter
     */
    private boolean isQueryParameter(Parameter parameter) {
        return In.QUERY == parameter.getIn();
    }

    /**
     * @param parameter
     * @return true if the input is a path parameter
     */
    private boolean isPathParameter(Parameter parameter) {
        return In.PATH == parameter.getIn();
    }

    /**
     * Validate the path and extract path parameters
     */
    private Set<String> validatePathAndRetrievePathParams(ValidationHelper helper, Context context, String pathStr) {
        String pathToCheck = pathStr;
        Set<String> pathParameters = new HashSet<String>();

        while (pathToCheck.contains("{")) {
            if (!pathToCheck.contains("}")) {
                final String message = Tr.formatMessage(tc, "pathItemInvalidFormat", pathStr);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                return pathParameters;
            }
            int firstIndex = pathToCheck.indexOf("{");
            int lastIndex = pathToCheck.indexOf("}");

            if (firstIndex > lastIndex) {
                final String message = Tr.formatMessage(tc, "pathItemInvalidFormat", pathStr);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                return pathParameters;
            }

            String parameter = pathToCheck.substring(firstIndex + 1, lastIndex);

            if (parameter.isEmpty() || parameter.contains("{") || parameter.contains("/")) {
                final String message = Tr.formatMessage(tc, "pathItemInvalidFormat", pathStr);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                return pathParameters;
            }

            pathParameters.add(parameter);
            pathToCheck = pathToCheck.substring(lastIndex + 1);
        }

        if (pathToCheck.contains("}")) {
            final String message = Tr.formatMessage(tc, "pathItemInvalidFormat", pathStr);
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            return pathParameters;
        }

        return pathParameters;
    }
}
