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
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.media.XML;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;

import com.ibm.ws.microprofile.openapi.impl.core.util.Json;
import com.ibm.ws.microprofile.openapi.impl.model.ComponentsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.SchemaImpl;
import com.ibm.ws.microprofile.openapi.impl.parser.processors.SchemaProcessor;

public class InlineModelResolver {
    private OpenAPI openAPI;
    private boolean skipMatches;
    //static Logger LOGGER = LoggerFactory.getLogger(InlineModelResolver.class);

    Map<String, Schema> addedModels = new HashMap<>();
    Map<String, String> generatedSignature = new HashMap<>();

    public void flatten(OpenAPI openAPI) {
        this.openAPI = openAPI;

        if (openAPI.getComponents() != null) {

            if (openAPI.getComponents().getSchemas() == null) {
                openAPI.getComponents().setSchemas(new HashMap<>());
            }
        }

        // operations
        Map<String, PathItem> paths = openAPI.getPaths();
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new ComponentsImpl());
        }
        Map<String, Schema> models = openAPI.getComponents().getSchemas();

        if (paths != null) {
            for (String pathname : paths.keySet()) {
                PathItem path = paths.get(pathname);

                for (Operation operation : path.readOperations()) {
                    RequestBody body = operation.getRequestBody();

                    if (body != null) {
                        if (body.getContent() != null) {
                            Map<String, MediaType> content = body.getContent();
                            for (String key : content.keySet()) {
                                if (content.get(key) != null) {
                                    MediaType mediaType = content.get(key);
                                    if (mediaType.getSchema() != null) {
                                        Schema model = mediaType.getSchema();
                                        if (model.getProperties() != null && model.getProperties().size() > 0) {
                                            flattenProperties(model.getProperties(), pathname);
                                            String modelName = resolveModelName(model.getTitle(), "body");
                                            mediaType.setSchema(new SchemaImpl().ref(modelName));
                                            addGenerated(modelName, model);
                                            openAPI.getComponents().addSchema(modelName, model);

                                        } else if (model.getType() == SchemaType.ARRAY) {
                                            Schema am = model;
                                            Schema inner = am.getItems();

                                            if (inner.getType() == SchemaType.OBJECT) {
                                                Schema op = inner;
                                                if (op.getProperties() != null && op.getProperties().size() > 0) {
                                                    flattenProperties(op.getProperties(), pathname);
                                                    String modelName = resolveModelName(op.getTitle(), "body");
                                                    Schema innerModel = objectmodelFromProperty(op, modelName);
                                                    String existing = matchGenerated(innerModel);
                                                    if (existing != null) {
                                                        am.setItems(new SchemaImpl().ref(existing));
                                                    } else {
                                                        am.setItems(new SchemaImpl().ref(modelName));
                                                        addGenerated(modelName, innerModel);
                                                        openAPI.getComponents().addSchema(modelName, innerModel);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    List<Parameter> parameters = operation.getParameters();
                    if (parameters != null) {
                        for (Parameter parameter : parameters) {
                            if (parameter.getSchema() != null) {
                                Schema model = parameter.getSchema();
                                if (model.getProperties() != null) {
                                    if (model.getType() == null || "object".equals(model.getType())) {
                                        if (model.getProperties() != null && model.getProperties().size() > 0) {
                                            flattenProperties(model.getProperties(), pathname);
                                            String modelName = resolveModelName(model.getTitle(), parameter.getName());
                                            parameter.setSchema(new SchemaImpl().ref(modelName));
                                            addGenerated(modelName, model);
                                            openAPI.getComponents().addSchema(modelName, model);
                                        }
                                    }
                                } else if (model.getType() == SchemaType.ARRAY) {
                                    Schema am = model;
                                    Schema inner = am.getItems();

                                    if (inner.getType() == SchemaType.OBJECT) {
                                        Schema op = inner;
                                        if (op.getProperties() != null && op.getProperties().size() > 0) {
                                            flattenProperties(op.getProperties(), pathname);
                                            String modelName = resolveModelName(op.getTitle(), parameter.getName());
                                            Schema innerModel = objectmodelFromProperty(op, modelName);
                                            String existing = matchGenerated(innerModel);
                                            if (existing != null) {
                                                am.setItems(new SchemaImpl().ref(existing));
                                            } else {
                                                am.setItems(new SchemaImpl().ref(modelName));
                                                addGenerated(modelName, innerModel);
                                                openAPI.getComponents().addSchema(modelName, innerModel);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Map<String, APIResponse> responses = operation.getResponses();
                    if (responses != null) {
                        for (String key : responses.keySet()) {
                            APIResponse response = responses.get(key);
                            if (response.getContent() != null) {
                                Map<String, MediaType> content = response.getContent();
                                for (String name : content.keySet()) {
                                    if (content.get(name) != null) {
                                        MediaType media = content.get(name);
                                        if (media.getSchema() != null) {
                                            Schema property = media.getSchema();
                                            if (property.getType() == SchemaType.OBJECT) {
                                                Schema op = property;
                                                if (op.getProperties() != null && op.getProperties().size() > 0) {
                                                    String modelName = resolveModelName(op.getTitle(), "inline_response_" + key);
                                                    Schema model = objectmodelFromProperty(op, modelName);
                                                    String existing = matchGenerated(model);
                                                    if (existing != null) {
                                                        media.setSchema(this.makeRefProperty(existing, property));
                                                    } else {
                                                        media.setSchema(this.makeRefProperty(modelName, property));
                                                        addGenerated(modelName, model);
                                                        openAPI.getComponents().addSchema(modelName, model);
                                                    }
                                                }
                                            } else if (property.getType() == SchemaType.ARRAY) {
                                                Schema ap = property;
                                                Schema inner = ap.getItems();

                                                if (inner.getType() == SchemaType.OBJECT) {
                                                    Schema op = inner;
                                                    if (op.getProperties() != null && op.getProperties().size() > 0) {
                                                        flattenProperties(op.getProperties(), pathname);
                                                        String modelName = resolveModelName(op.getTitle(),
                                                                                            "inline_response_" + key);
                                                        Schema innerModel = objectmodelFromProperty(op, modelName);
                                                        String existing = matchGenerated(innerModel);
                                                        if (existing != null) {
                                                            ap.setItems(this.makeRefProperty(existing, op));
                                                        } else {
                                                            ap.setItems(this.makeRefProperty(modelName, op));
                                                            addGenerated(modelName, innerModel);
                                                            openAPI.getComponents().addSchema(modelName, innerModel);
                                                        }
                                                    }
                                                }
                                            } else if (property.getAdditionalProperties() != null && property.getAdditionalProperties() instanceof Schema) {

                                                Schema innerProperty = (Schema) property.getAdditionalProperties();
                                                if (innerProperty.getType() == SchemaType.OBJECT) {
                                                    Schema op = innerProperty;
                                                    if (op.getProperties() != null && op.getProperties().size() > 0) {
                                                        flattenProperties(op.getProperties(), pathname);
                                                        String modelName = resolveModelName(op.getTitle(),
                                                                                            "inline_response_" + key);
                                                        Schema innerModel = objectmodelFromProperty(op, modelName);
                                                        String existing = matchGenerated(innerModel);
                                                        if (existing != null) {
                                                            property.setAdditionalProperties(new SchemaImpl().ref(existing));
                                                        } else {
                                                            property.setAdditionalProperties(new SchemaImpl().ref(modelName));
                                                            addGenerated(modelName, innerModel);
                                                            openAPI.getComponents().addSchema(modelName, innerModel);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // definitions
        if (models != null) {
            List<String> modelNames = new ArrayList<String>(models.keySet());
            for (String modelName : modelNames) {
                Schema model = models.get(modelName);
                if (model.getProperties() != null) {
                    Map<String, Schema> properties = model.getProperties();
                    flattenProperties(properties, modelName);
                    fixStringModel(model);
                } else if (model.getType() == SchemaType.ARRAY) {
                    Schema m = model;
                    Schema inner = m.getItems();
                    if (inner.getType() == SchemaType.OBJECT) {
                        Schema op = inner;
                        if (op.getProperties() != null && op.getProperties().size() > 0) {
                            String innerModelName = resolveModelName(op.getTitle(), modelName + "_inner");
                            Schema innerModel = objectmodelFromProperty(op, innerModelName);
                            String existing = matchGenerated(innerModel);
                            if (existing == null) {
                                openAPI.getComponents().addSchema(innerModelName, innerModel);
                                addGenerated(innerModelName, innerModel);
                                m.setItems(new SchemaImpl().ref(innerModelName));
                            } else {
                                m.setItems(new SchemaImpl().ref(existing));
                            }
                        }
                    }
                } else if (SchemaProcessor.isComposedSchema(model)) {
                    Schema composedSchema = model;
                    List<Schema> list = null;
                    if (composedSchema.getAllOf() != null) {
                        list = composedSchema.getAllOf();
                    } else if (composedSchema.getAnyOf() != null) {
                        list = composedSchema.getAnyOf();
                    } else if (composedSchema.getOneOf() != null) {
                        list = composedSchema.getOneOf();
                    }
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).getProperties() != null) {
                            flattenProperties(list.get(i).getProperties(), modelName);
                        }
                    }
                }
            }
        }
    }

    /**
     * This function fix models that are string (mostly enum). Before this fix, the example
     * would look something like that in the doc: "\"example from def\""
     *
     * @param m Model implementation
     */
    private void fixStringModel(Schema m) {
        if (m.getType() != null && m.getType().equals("string") && m.getExample() != null) {
            String example = m.getExample().toString();
            if (example.substring(0, 1).equals("\"") &&
                example.substring(example.length() - 1).equals("\"")) {
                m.setExample(example.substring(1, example.length() - 1));
            }
        }
    }

    private String resolveModelName(String title, String key) {
        if (title == null) {
            return uniqueName(key);
        } else {
            return uniqueName(title);
        }
    }

    public String matchGenerated(Schema model) {
        if (this.skipMatches) {
            return null;
        }
        String json = Json.pretty(model);
        if (generatedSignature.containsKey(json)) {
            return generatedSignature.get(json);
        }
        return null;
    }

    public void addGenerated(String name, Schema model) {
        generatedSignature.put(Json.pretty(model), name);
    }

    public String uniqueName(String key) {
        int count = 0;
        boolean done = false;
        key = key.replaceAll("[^a-z_\\.A-Z0-9 ]", ""); // FIXME: a parameter
        // should not be
        // assigned. Also declare
        // the methods parameters
        // as 'final'.
        while (!done) {
            String name = key;
            if (count > 0) {
                name = key + "_" + count;
            }
            if (openAPI.getComponents().getSchemas() == null) {
                return name;
            } else if (!openAPI.getComponents().getSchemas().containsKey(name)) {
                return name;
            }
            count += 1;
        }
        return key;
    }

    public void flattenProperties(Map<String, Schema> properties, String path) {
        if (properties == null) {
            return;
        }
        Map<String, Schema> propsToUpdate = new HashMap<>();
        Map<String, Schema> modelsToAdd = new HashMap<>();
        for (String key : properties.keySet()) {
            Schema property = properties.get(key);
            if (property.getType() == SchemaType.OBJECT && property.getProperties() != null
                && property.getProperties().size() > 0) {

                Schema op = property;

                String modelName = resolveModelName(op.getTitle(), path + "_" + key);
                Schema model = objectmodelFromProperty(op, modelName);

                String existing = matchGenerated(model);

                if (existing != null) {
                    propsToUpdate.put(key, new SchemaImpl().ref(existing));
                } else {
                    propsToUpdate.put(key, new SchemaImpl().ref(modelName));
                    modelsToAdd.put(modelName, model);
                    addGenerated(modelName, model);
                    openAPI.getComponents().addSchema(modelName, model);
                }
            } else if (property.getType() == SchemaType.ARRAY) {
                Schema ap = property;
                Schema inner = ap.getItems();

                if (inner.getType() == SchemaType.OBJECT) {
                    Schema op = inner;
                    if (op.getProperties() != null && op.getProperties().size() > 0) {
                        flattenProperties(op.getProperties(), path);
                        String modelName = resolveModelName(op.getTitle(), path + "_" + key);
                        Schema innerModel = objectmodelFromProperty(op, modelName);
                        String existing = matchGenerated(innerModel);
                        if (existing != null) {
                            ap.setItems(new SchemaImpl().ref(existing));
                        } else {
                            ap.setItems(new SchemaImpl().ref(modelName));
                            addGenerated(modelName, innerModel);
                            openAPI.getComponents().addSchema(modelName, innerModel);
                        }
                    }
                }
            } else if (property.getAdditionalProperties() != null && property.getAdditionalProperties() instanceof Schema) {
                Schema inner = (Schema) property.getAdditionalProperties();

                if (inner.getType() == SchemaType.OBJECT) {
                    Schema op = inner;
                    if (op.getProperties() != null && op.getProperties().size() > 0) {
                        flattenProperties(op.getProperties(), path);
                        String modelName = resolveModelName(op.getTitle(), path + "_" + key);
                        Schema innerModel = objectmodelFromProperty(op, modelName);
                        String existing = matchGenerated(innerModel);
                        if (existing != null) {
                            property.setAdditionalProperties(new SchemaImpl().ref(existing));
                        } else {
                            property.setAdditionalProperties(new SchemaImpl().ref(modelName));
                            addGenerated(modelName, innerModel);
                            openAPI.getComponents().addSchema(modelName, innerModel);
                        }
                    }
                }
            }
        }
        if (propsToUpdate.size() > 0) {
            for (String key : propsToUpdate.keySet()) {
                properties.put(key, propsToUpdate.get(key));
            }
        }
        for (String key : modelsToAdd.keySet()) {
            openAPI.getComponents().addSchema(key, modelsToAdd.get(key));
            this.addedModels.put(key, modelsToAdd.get(key));
        }
    }

    @SuppressWarnings("static-method")
    public Schema arraymodelFromProperty(Schema object, @SuppressWarnings("unused") String path) {
        String description = object.getDescription();
        String example = null;

        Object obj = object.getExample();
        if (obj != null) {
            example = obj.toString();
        }

        Schema inner = object.getItems();
        if (inner.getType() == SchemaType.ARRAY) {
            Schema model = new SchemaImpl().type(SchemaType.ARRAY);
            model.setDescription(description);
            model.setExample(example);
            model.setItems(object.getItems());
            return model;
        }

        return null;
    }

    public Schema objectmodelFromProperty(Schema objectSchema, String path) {
        String description = objectSchema.getDescription();
        String example = null;

        Object obj = objectSchema.getExample();
        if (obj != null) {
            example = obj.toString();
        }
        String name = ((SchemaImpl) objectSchema).getName();
        XML xml = objectSchema.getXml();
        Map<String, Schema> properties = objectSchema.getProperties();

        Schema model = new SchemaImpl();//TODO Verify this!
        model.setDescription(description);
        model.setExample(example);
        ((SchemaImpl) model).setName(name);
        model.setXml(xml);

        if (properties != null) {
            flattenProperties(properties, path);
            model.setProperties(properties);
        }

        return model;
    }

    @SuppressWarnings("static-method")
    public Schema schemaModelFromProperty(Schema object, @SuppressWarnings("unused") String path) {
        String description = object.getDescription();
        String example = null;

        Object obj = object.getExample();
        if (obj != null) {
            example = obj.toString();
        }

        Schema model = new SchemaImpl().type(SchemaType.ARRAY);
        model.setDescription(description);
        model.setExample(example);

        if (object.getAdditionalProperties() != null && object.getAdditionalProperties() instanceof Schema) {
            model.setItems((Schema) object.getAdditionalProperties());
        }
        return model;
    }

    /**
     * Make a RefProperty
     *
     * @param ref new property name
     * @param property Property
     * @return
     */
    public Schema makeRefProperty(String ref, Schema property) {
        Schema newProperty = new SchemaImpl().ref(ref);

        this.copyVendorExtensions(property, newProperty);
        return newProperty;
    }

    /**
     * Copy vendor extensions from Property to another Property
     *
     * @param source source property
     * @param target target property
     */
    public void copyVendorExtensions(Schema source, Schema target) {
        if (source.getExtensions() != null) {
            Map<String, Object> vendorExtensions = source.getExtensions();
            for (String extName : vendorExtensions.keySet()) {
                target.addExtension(extName, vendorExtensions.get(extName));
            }
        }
    }

    public boolean isSkipMatches() {
        return skipMatches;
    }

    public void setSkipMatches(boolean skipMatches) {
        this.skipMatches = skipMatches;
    }

}