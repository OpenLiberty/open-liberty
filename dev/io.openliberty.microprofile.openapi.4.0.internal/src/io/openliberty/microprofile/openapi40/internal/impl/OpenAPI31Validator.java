/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.impl;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult;
import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult.ValidationEvent;
import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult.ValidationEvent.Severity;
import io.openliberty.microprofile.openapi20.internal.services.OASValidator;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.internal.validation.OASValidator30Impl;
import io.openliberty.microprofile.openapi20.internal.validation.ValidatorUtils;
import io.openliberty.microprofile.openapi40.internal.validation.OpenAPIDefinition31Validator;
import io.openliberty.microprofile.openapi40.internal.validation.Operation31Validator;
import io.openliberty.microprofile.openapi40.internal.validation.PathItem31Validator;
import io.openliberty.microprofile.openapi40.internal.validation.Reference31Validator;
import io.openliberty.microprofile.openapi40.internal.validation.Schema31Validator;
import io.openliberty.microprofile.openapi40.internal.validation.ServerVariable31Validator;

@Component(service = OASValidator.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = "openapi.version=3.1")
public class OpenAPI31Validator extends OASValidator30Impl {

    private static final TraceComponent tc = Tr.register(OpenAPI31Validator.class);

    @Override
    public OASValidationResult validate(OpenAPI model) {
        ValidationOperation31 validator = new ValidationOperation31(model);
        return validator.run();
    }

    public class ValidationOperation31 extends ValidationOperation {

        public ValidationOperation31(OpenAPI model) {
            super(model);
        }

        @Override
        public void visitOpenAPI(Context context) {
            OpenAPIDefinition31Validator.getInstance().validate(this, context, context.getModel());
        }

        @Override
        public Operation visitOperation(Context context, Operation operation) {
            Operation31Validator.getInstance().validate(this, context, operation);
            return operation;
        }

        @Override
        public ServerVariable visitServerVariable(Context context, String key, ServerVariable sv) {
            ServerVariable31Validator.getInstance().validate(this, context, key, sv);
            return sv;
        }

        @Override
        public PathItem visitPathItem(Context context, String key, PathItem item) {
            PathItem31Validator.getInstance().validate(this, context, key, item);
            return item;
        }

        @Override
        public <T> T validateReference(Context context, String key, String ref, Class<T> clazz) {
            Object component = validateReference(context, key, ref);
            if (component == null) {
                // We didn't get an object back
                // This could be because the reference was invalid, or because it was external, or because we didn't know how to validate it
                // Any relevant validation events will have been raised, there's nothing more for us to do
                return null;
            } else if (clazz.isInstance(component)) {
                return clazz.cast(component);
            } else {
                final String message = ValidatorUtils.formatMessage(ValidationMessageConstants.REFERENCE_TO_OBJECT_INVALID, ref);
                addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
                return null;
            }
        }

        @Override
        public Schema visitSchema(Context context, Schema schema) {
            Schema31Validator.getInstance().validate(this, context, schema);
            return schema;
        }

        @Override
        public Schema visitSchema(Context context, String key, Schema schema) {
            Schema31Validator.getInstance().validate(this, context, key, schema);
            return schema;
        }

        @Override
        public Object validateReference(Context context, String key, String ref) {
            return Reference31Validator.getInstance().validate(this, context, ref);
        }
    }
}
