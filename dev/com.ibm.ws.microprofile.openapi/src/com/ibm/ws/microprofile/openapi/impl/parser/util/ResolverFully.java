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

package com.ibm.ws.microprofile.openapi.impl.parser.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;

import com.ibm.ws.microprofile.openapi.impl.model.media.SchemaImpl;
import com.ibm.ws.microprofile.openapi.impl.parser.processors.SchemaProcessor;

public class ResolverFully {
    //private static final Logger LOGGER = LoggerFactory.getLogger(ResolverFully.class);

    private final boolean aggregateCombinators;

    public ResolverFully() {
        this(true);
    }

    public ResolverFully(boolean aggregateCombinators) {
        this.aggregateCombinators = aggregateCombinators;
    }

    private Map<String, Schema> schemas;
    private final Map<String, Schema> resolvedModels = new HashMap<>();
    private Map<String, Example> examples;

    public void resolveFully(OpenAPI openAPI) {
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            schemas = openAPI.getComponents().getSchemas();
            if (schemas == null) {
                schemas = new HashMap<>();
            }
        }

        if (openAPI.getComponents() != null && openAPI.getComponents().getExamples() != null) {
            examples = openAPI.getComponents().getExamples();
            if (examples == null) {
                examples = new HashMap<>();
            }
        }

        if (openAPI.getPaths() != null) {
            for (String pathname : openAPI.getPaths().keySet()) {
                PathItem pathItem = openAPI.getPaths().get(pathname);
                resolvePath(pathItem);
            }
        }
    }

    public void resolvePath(PathItem pathItem) {
        for (Operation op : pathItem.readOperations()) {
            // inputs
            if (op.getParameters() != null) {
                for (Parameter parameter : op.getParameters()) {
                    if (parameter.getSchema() != null) {
                        Schema resolved = resolveSchema(parameter.getSchema());
                        if (resolved != null) {
                            parameter.setSchema(resolved);
                        }
                    }
                    if (parameter.getContent() != null) {
                        Map<String, MediaType> content = parameter.getContent();
                        for (String key : content.keySet()) {
                            if (content.get(key) != null && content.get(key).getSchema() != null) {
                                Schema resolvedSchema = resolveSchema(content.get(key).getSchema());
                                if (resolvedSchema != null) {
                                    content.get(key).setSchema(resolvedSchema);
                                }
                            }
                        }
                    }
                }
            }

            if (op.getCallbacks() != null) {
                Map<String, Callback> callbacks = op.getCallbacks();
                for (String name : callbacks.keySet()) {
                    Callback callback = callbacks.get(name);
                    if (callback != null) {
                        for (String callbackName : callback.keySet()) {
                            PathItem path = callback.get(callbackName);
                            if (path != null) {
                                resolvePath(path);
                            }

                        }
                    }
                }
            }

            if (op.getRequestBody() != null && op.getRequestBody().getContent() != null) {
                Map<String, MediaType> content = op.getRequestBody().getContent();
                for (String key : content.keySet()) {
                    if (content.get(key) != null && content.get(key).getSchema() != null) {
                        Schema resolved = resolveSchema(content.get(key).getSchema());
                        if (resolved != null) {
                            content.get(key).setSchema(resolved);
                        }
                    }
                }
            }
            // responses
            if (op.getResponses() != null) {
                for (String code : op.getResponses().keySet()) {
                    APIResponse response = op.getResponses().get(code);
                    if (response.getContent() != null) {
                        Map<String, MediaType> content = response.getContent();
                        for (String mediaType : content.keySet()) {
                            if (content.get(mediaType).getSchema() != null) {
                                Schema resolved = resolveSchema(content.get(mediaType).getSchema());
                                response.getContent().get(mediaType).setSchema(resolved);
                            }
                            if (content.get(mediaType).getExamples() != null) {
                                Map<String, Example> resolved = resolveExample(content.get(mediaType).getExamples());
                                response.getContent().get(mediaType).setExamples(resolved);

                            }
                        }
                    }
                }
            }
        }
    }

    public Schema resolveSchema(Schema schema) {
        if (schema.getRef() != null) {
            String ref = schema.getRef();
            ref = ref.substring(ref.lastIndexOf("/") + 1);
            Schema resolved = schemas.get(ref);
            if (resolved == null) {
                //LOGGER.error("unresolved model " + ref);
                return schema;
            }
            if (this.resolvedModels.containsKey(ref)) {
                //LOGGER.debug("avoiding infinite loop");
                return this.resolvedModels.get(ref);
            }
            this.resolvedModels.put(ref, schema);

            Schema model = resolveSchema(resolved);

            // if we make it without a resolution loop, we can update the reference
            this.resolvedModels.put(ref, model);
            return model;
        }

        if (schema.getType() == SchemaType.ARRAY) {
            if (schema.getItems().getRef() != null) {
                schema.setItems(resolveSchema(schema.getItems()));
            } else {
                schema.setItems(schema.getItems());
            }

            return schema;
        }

        if (schema.getType() == SchemaType.OBJECT) {
            if (schema.getProperties() != null) {
                Map<String, Schema> updated = new LinkedHashMap<>();
                for (String propertyName : schema.getProperties().keySet()) {
                    Schema innerProperty = schema.getProperties().get(propertyName);
                    // reference check
                    if (schema != innerProperty) {
                        Schema resolved = resolveSchema(innerProperty);
                        updated.put(propertyName, resolved);
                    }
                }
                schema.setProperties(updated);
            }
            return schema;
        }

        if (SchemaProcessor.isComposedSchema(schema)) {
            Schema composedSchema = schema;
            if (aggregateCombinators) {
                Schema model = SchemaTypeUtil.createSchema(schema.getType().toString(), schema.getFormat());
                Set<String> requiredProperties = new HashSet<>();
                if (schema.getAllOf() != null) {
                    for (Schema innerModel : schema.getAllOf()) {
                        Schema resolved = resolveSchema(innerModel);
                        Map<String, Schema> properties = resolved.getProperties();
                        if (resolved.getProperties() != null) {
                            for (String key : properties.keySet()) {
                                Schema prop = resolved.getProperties().get(key);
                                model.addProperty(key, resolveSchema(prop));
                            }
                            if (resolved.getRequired() != null) {
                                for (int i = 0; i < resolved.getRequired().size(); i++) {
                                    if (resolved.getRequired().get(i) != null) {
                                        requiredProperties.add(resolved.getRequired().get(i).toString());
                                    }
                                }
                            }
                        }
                        if (requiredProperties.size() > 0) {
                            model.setRequired(new ArrayList<>(requiredProperties));
                        }
                        if (schema.getExtensions() != null) {
                            Map<String, Object> extensions = schema.getExtensions();
                            for (String key : extensions.keySet()) {
                                model.addExtension(key, schema.getExtensions().get(key));
                            }
                        }
                        return model;
                    }

                } else if (composedSchema.getOneOf() != null) {
                    Schema resolved;
                    List<Schema> list = new ArrayList<>();
                    for (Schema innerModel : composedSchema.getOneOf()) {
                        resolved = resolveSchema(innerModel);
                        list.add(resolved);
                    }
                    composedSchema.setOneOf(list);

                } else if (composedSchema.getAnyOf() != null) {
                    Schema resolved;
                    List<Schema> list = new ArrayList<>();
                    for (Schema innerModel : composedSchema.getAnyOf()) {
                        resolved = resolveSchema(innerModel);
                        list.add(resolved);
                    }
                    composedSchema.setAnyOf(list);
                }

                return composedSchema;
            } else {
                // User don't want to aggregate composed schema, we only solve refs
                if (composedSchema.getAllOf() != null)
                    composedSchema.allOf(composedSchema.getAllOf().stream().map(this::resolveSchema).collect(Collectors.toList()));
                else if (composedSchema.getOneOf() != null)
                    composedSchema.oneOf(composedSchema.getOneOf().stream().map(this::resolveSchema).collect(Collectors.toList()));
                else if (composedSchema.getAnyOf() != null)
                    composedSchema.anyOf(composedSchema.getAnyOf().stream().map(this::resolveSchema).collect(Collectors.toList()));
                return composedSchema;
            }
        }

        if (schema.getProperties() != null) {
            Schema model = schema;
            Map<String, Schema> updated = new LinkedHashMap<>();
            Map<String, Schema> properties = model.getProperties();
            for (String propertyName : properties.keySet()) {
                Schema property = model.getProperties().get(propertyName);
                Schema resolved = resolveSchema(property);
                updated.put(propertyName, resolved);
            }

            for (String key : updated.keySet()) {
                Schema property = updated.get(key);

                if (property.getType() == SchemaType.OBJECT) {
                    if (property.getProperties() != model.getProperties()) {
                        if (property.getType() == null) {
                            property.setType(SchemaType.OBJECT);
                        }
                        model.addProperty(key, property);
                    } else {
                        //LOGGER.debug("not adding recursive properties, using generic object");
                        Schema newSchema = new SchemaImpl().type(SchemaType.OBJECT);
                        model.addProperty(key, newSchema);
                    }
                }

            }
            return model;
        }

        return schema;
    }

    public Map<String, Example> resolveExample(Map<String, Example> examples) {

        Map<String, Example> resolveExamples = examples;

        if (examples != null) {

            for (String name : examples.keySet()) {
                if (examples.get(name).getRef() != null) {
                    String ref = examples.get(name).getRef();
                    ref = ref.substring(ref.lastIndexOf("/") + 1);
                    Example sample = this.examples.get(ref);
                    resolveExamples.replace(name, sample);
                }
            }
        }

        return resolveExamples;

    }
}
