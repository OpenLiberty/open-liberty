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

package com.ibm.ws.microprofile.openapi.impl.parser.processors;

import static com.ibm.ws.microprofile.openapi.impl.parser.util.RefUtils.computeRefFormat;
import static com.ibm.ws.microprofile.openapi.impl.parser.util.RefUtils.isAnExternalRefFormat;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;

import com.ibm.ws.microprofile.openapi.impl.parser.ResolverCache;
import com.ibm.ws.microprofile.openapi.impl.parser.models.RefFormat;

public class SchemaProcessor {
    private final ResolverCache cache;
    private final ExternalRefProcessor externalRefProcessor;

    public SchemaProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
    }

    public void processSchema(Schema schema) {
        if (schema != null) {
            if (schema.getRef() != null) {
                processReferenceSchema(schema);
            } else {
                processSchemaType(schema);
            }
        }
    }

    public static boolean isComposedSchema(Schema schema) {
        if (schema.getAllOf() != null)
            return true;
        if (schema.getAnyOf() != null)
            return true;
        if (schema.getOneOf() != null)
            return true;
        return false;
    }

    public void processSchemaType(Schema schema) {

        if (schema.getType() == SchemaType.ARRAY) {
            processArraySchema(schema);
        } else if (isComposedSchema(schema)) {
            processComposedSchema(schema);
        }

        if (schema.getProperties() != null) {
            processPropertySchema(schema);
        }
        if (schema.getNot() != null) {
            processNotSchema(schema);
        }
        if (schema.getAdditionalProperties() != null) {
            processAdditionalProperties(schema);

        }

    }

    private void processAdditionalProperties(Schema schema) {

        if (schema.getAdditionalProperties() != null && schema.getAdditionalProperties() instanceof Schema) {
            Schema addProps = (Schema) schema.getAdditionalProperties();
            if (addProps.getRef() != null) {
                processReferenceSchema(addProps);
            } else {
                processSchemaType(addProps);
            }
        }
    }

    private void processNotSchema(Schema schema) {

        if (schema.getNot() != null) {
            if (schema.getNot().getRef() != null) {
                processReferenceSchema(schema.getNot());
            } else {
                processSchemaType(schema.getNot());
            }
        }
    }

    public void processPropertySchema(Schema schema) {
        if (schema.getRef() != null) {
            processReferenceSchema(schema);
        }

        Map<String, Schema> properties = schema.getProperties();
        if (properties != null) {
            for (Map.Entry<String, Schema> propertyEntry : properties.entrySet()) {
                Schema property = propertyEntry.getValue();
                if (property.getType() == SchemaType.ARRAY) {
                    processArraySchema(property);
                }
                if (property.getRef() != null) {
                    processReferenceSchema(property);
                }
            }
        }
    }

    public void processComposedSchema(Schema composedSchema) {
        if (composedSchema.getAllOf() != null) {
            final List<Schema> schemas = composedSchema.getAllOf();
            if (schemas != null) {
                for (Schema schema : schemas) {
                    if (schema.getRef() != null) {
                        processReferenceSchema(schema);
                    } else {
                        processSchemaType(schema);
                    }
                }
            }
        }
        if (composedSchema.getOneOf() != null) {
            final List<Schema> schemas = composedSchema.getOneOf();
            if (schemas != null) {
                for (Schema schema : schemas) {
                    if (schema.getRef() != null) {
                        processReferenceSchema(schema);
                    } else {
                        processSchemaType(schema);
                    }
                }
            }
        }
        if (composedSchema.getAnyOf() != null) {
            final List<Schema> schemas = composedSchema.getAnyOf();
            if (schemas != null) {
                for (Schema schema : schemas) {
                    if (schema.getRef() != null) {
                        processReferenceSchema(schema);
                    } else {
                        processSchemaType(schema);
                    }
                }
            }
        }

    }

    public void processArraySchema(Schema arraySchema) {

        final Schema items = arraySchema.getItems();
        if (items.getRef() != null) {
            processReferenceSchema(items);
        } else {
            processSchemaType(items);
        }
    }

    /*
     * public Schema processReferenceSchema(Schema schema){
     * RefFormat refFormat = computeRefFormat(schema.get$ref());
     * String $ref = schema.get$ref();
     * Schema newSchema = cache.loadRef($ref, refFormat, Schema.class);
     * return newSchema;
     * }
     */

    private void processReferenceSchema(Schema schema) {
        /*
         * if this is a URL or relative ref:
         * 1) we need to load it into memory.
         * 2) shove it into the #/definitions
         * 3) update the RefModel to point to its location in #/definitions
         */
        RefFormat refFormat = computeRefFormat(schema.getRef());
        String $ref = schema.getRef();

        if (isAnExternalRefFormat(refFormat)) {
            final String newRef = externalRefProcessor.processRefToExternalSchema($ref, refFormat);

            if (newRef != null) {
                schema.setRef(newRef);
            }
        }
    }

}