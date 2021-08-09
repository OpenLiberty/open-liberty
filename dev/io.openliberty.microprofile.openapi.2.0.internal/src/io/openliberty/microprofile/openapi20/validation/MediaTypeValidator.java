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

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent.Severity;

/**
 *
 */
public class MediaTypeValidator extends TypeValidator<MediaType> {

    private static final TraceComponent tc = Tr.register(MediaTypeValidator.class);

    private static final MediaTypeValidator INSTANCE = new MediaTypeValidator();

    public static MediaTypeValidator getInstance() {
        return INSTANCE;
    }

    private MediaTypeValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, MediaType t) {
        Object example = t.getExample();
        Map<String, Example> examples = t.getExamples();
        if ((example != null) && (examples != null && !examples.isEmpty())) {
            final String message = Tr.formatMessage(tc, ValidationMessageConstants.MEDIA_TYPE_EXAMPLE_OR_EXAMPLES);
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
        }

        //encoding - A map between a property name and its encoding information. The key, being the property name, MUST exist in the schema as a property.
        Map<String, Encoding> encoding = t.getEncoding();
        if (encoding != null && !encoding.isEmpty()) {
            Set<String> encodingProperties = encoding.keySet();

            Schema schema = t.getSchema();
            if (schema != null) {

                String ref = schema.getRef();
                if (StringUtils.isNotBlank(ref)) { //if $ref is set, go to the class specified in $ref and look at the properties

                    ReferenceValidator referenceValidator = ReferenceValidator.getInstance();
                    Object component = referenceValidator.validate(helper, context, key, ref);
                    if (!schema.getClass().isInstance(component)) {
                        final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_TO_OBJECT_INVALID, ref);
                        helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
                    } else {
                        Schema componentSchema = (Schema) component;
                        Map<String, Schema> schemaProperties = componentSchema != null ? componentSchema.getProperties() : null;

                        for (String encodingProperty : encodingProperties) {
                            if (schemaProperties == null || !schemaProperties.containsKey(encodingProperty)) {
                                final String message = Tr.formatMessage(tc, ValidationMessageConstants.MEDIA_TYPE_ENCODING_PROPERTY, encodingProperty);
                                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                            }
                        }
                    }
                } else if (schema.getProperties() != null) { //if $ref is not set, but properties map of schema in mediaType are set, then look there

                    for (String encodingProperty : encodingProperties) {
                        if (!schema.getProperties().containsKey(encodingProperty)) {
                            final String message = Tr.formatMessage(tc, ValidationMessageConstants.MEDIA_TYPE_ENCODING_PROPERTY, encodingProperty);
                            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                        }
                    }
                } else { //if neither $ref nor properties are set, then throw a validation error
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.MEDIA_TYPE_EMPTY_SCHEMA);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                }
            } else { //if there is encoding specified in the mediaType model, but no schema, throw a validation error

                final String message = Tr.formatMessage(tc, ValidationMessageConstants.MEDIA_TYPE_EMPTY_SCHEMA);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }
        }
    }
}
