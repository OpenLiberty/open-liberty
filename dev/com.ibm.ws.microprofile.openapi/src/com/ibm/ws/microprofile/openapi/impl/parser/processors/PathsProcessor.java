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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;

import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIResolver;
import com.ibm.ws.microprofile.openapi.impl.parser.ResolverCache;
import com.ibm.ws.microprofile.openapi.impl.parser.models.RefFormat;

public class PathsProcessor {

    private final OpenAPI openAPI;
    private final ResolverCache cache;
    private final OpenAPIResolver.Settings settings;
    private final ParameterProcessor parameterProcessor;
    private final OperationProcessor operationProcessor;

    public PathsProcessor(ResolverCache cache, OpenAPI openAPI) {
        this(cache, openAPI, new OpenAPIResolver.Settings());
    }

    public PathsProcessor(ResolverCache cache, OpenAPI openAPI, OpenAPIResolver.Settings settings) {
        this.openAPI = openAPI;
        this.cache = cache;
        this.settings = settings;
        parameterProcessor = new ParameterProcessor(cache, openAPI);
        operationProcessor = new OperationProcessor(cache, openAPI);
    }

    public void processPaths() {
        final Map<String, PathItem> pathMap = openAPI.getPaths();

        if (pathMap == null) {
            return;
        }

        for (String pathStr : pathMap.keySet()) {
            PathItem pathItem = pathMap.get(pathStr);

            addParametersToEachOperation(pathItem);

            if (pathItem.getRef() != null) {
                RefFormat refFormat = computeRefFormat(pathItem.getRef());
                PathItem resolvedPath = cache.loadRef(pathItem.getRef(), refFormat, PathItem.class);

                // TODO: update references to the parent location

                String pathRef = pathItem.getRef().split("#")[0];
                updateLocalRefs(resolvedPath, pathRef);

                if (resolvedPath != null) {
                    //we need to put the resolved path into swagger object
                    openAPI.path(pathStr, resolvedPath);
                    pathItem = resolvedPath;
                }
            }

            //at this point we can process this path
            final List<Parameter> processedPathParameters = parameterProcessor.processParameters(pathItem.getParameters());
            pathItem.setParameters(processedPathParameters);

            addParametersToEachOperation(pathItem);

            final Map<PathItem.HttpMethod, Operation> operationMap = pathItem.readOperationsMap();

            for (PathItem.HttpMethod httpMethod : operationMap.keySet()) {
                Operation operation = operationMap.get(httpMethod);
                operationProcessor.processOperation(operation);
            }
        }
    }

    private void addParametersToEachOperation(PathItem pathItem) {
        if (settings.addParametersToEachOperation()) {
            List<Parameter> parameters = pathItem.getParameters();

            if (parameters != null) {
                // add parameters to each operation
                List<Operation> operations = pathItem.readOperations();
                if (operations != null) {
                    for (Operation operation : operations) {
                        List<Parameter> parametersToAdd = new ArrayList<>();
                        List<Parameter> existingParameters = operation.getParameters();
                        for (Parameter parameterToAdd : parameters) {
                            boolean matched = false;
                            for (Parameter existingParameter : existingParameters) {
                                if (parameterToAdd.getIn() != null && parameterToAdd.getIn().equals(existingParameter.getIn()) &&
                                    parameterToAdd.getName().equals(existingParameter.getName())) {
                                    matched = true;
                                }
                            }
                            if (!matched) {
                                parametersToAdd.add(parameterToAdd);
                            }
                        }
                        if (parametersToAdd.size() > 0) {
                            operation.getParameters().addAll(0, parametersToAdd);
                        }
                    }
                }
            }
            // remove the shared parameters
            pathItem.setParameters(null);
        }
    }

    protected void updateLocalRefs(PathItem path, String pathRef) {
        if (path.getParameters() != null) {
            List<Parameter> params = path.getParameters();
            for (Parameter param : params) {
                updateLocalRefs(param, pathRef);
            }
        }
        List<Operation> ops = path.readOperations();
        for (Operation op : ops) {
            if (op.getParameters() != null) {
                for (Parameter param : op.getParameters()) {
                    updateLocalRefs(param, pathRef);
                }
            }
            if (op.getResponses() != null) {
                for (APIResponse response : op.getResponses().values()) {
                    updateLocalRefs(response, pathRef);
                }
            }
            if (op.getRequestBody() != null) {
                updateLocalRefs(op.getRequestBody(), pathRef);
            }
            if (op.getCallbacks() != null) {
                Map<String, Callback> callbacks = op.getCallbacks();
                for (String name : callbacks.keySet()) {
                    Callback callback = callbacks.get(name);
                    if (callback != null) {
                        for (String callbackName : callback.keySet()) {
                            PathItem pathItem = callback.get(callbackName);
                            updateLocalRefs(pathItem, pathRef);
                        }
                    }
                }
            }
        }
    }

    protected void updateLocalRefs(APIResponse response, String pathRef) {
        if (response.getContent() != null) {
            Map<String, MediaType> content = response.getContent();
            for (String key : content.keySet()) {
                MediaType mediaType = content.get(key);
                if (mediaType.getSchema() != null) {
                    updateLocalRefs(mediaType.getSchema(), pathRef);
                }
            }
        }
    }

    protected void updateLocalRefs(Parameter param, String pathRef) {
        if (param.getSchema() != null) {
            updateLocalRefs(param.getSchema(), pathRef);
        }
        if (param.getContent() != null) {
            Map<String, MediaType> content = param.getContent();
            for (String key : content.keySet()) {
                MediaType mediaType = content.get(key);
                if (mediaType.getSchema() != null) {
                    updateLocalRefs(mediaType.getSchema(), pathRef);
                }
            }
        }

    }

    protected void updateLocalRefs(RequestBody body, String pathRef) {
        if (body.getContent() != null) {
            Map<String, MediaType> content = body.getContent();
            for (String key : content.keySet()) {
                MediaType mediaType = content.get(key);
                if (mediaType.getSchema() != null) {
                    updateLocalRefs(mediaType.getSchema(), pathRef);
                }
            }
        }
    }

    protected void updateLocalRefs(Schema model, String pathRef) {
        if (model.getRef() != null) {
            if (isLocalRef(model.getRef())) {
                model.setRef(computeLocalRef(model.getRef(), pathRef));
            }
        } else if (model.getProperties() != null) {
            // process properties
            if (model.getProperties() != null) {
                Map<String, Schema> properties = model.getProperties();
                for (String key : properties.keySet()) {
                    Schema property = properties.get(key);
                    if (property != null) {
                        updateLocalRefs(property, pathRef);
                    }
                }
            }
        } else if (SchemaProcessor.isComposedSchema(model)) {
            if (model.getAllOf() != null) {
                for (Schema innerModel : model.getAllOf()) {
                    updateLocalRefs(innerModel, pathRef);
                }
            }
            if (model.getAnyOf() != null) {
                for (Schema innerModel : model.getAnyOf()) {
                    updateLocalRefs(innerModel, pathRef);
                }
            }
            if (model.getOneOf() != null) {
                for (Schema innerModel : model.getOneOf()) {
                    updateLocalRefs(innerModel, pathRef);
                }
            }
        } else if (model.getType() == SchemaType.ARRAY) {
            if (model.getItems() != null) {
                updateLocalRefs(model.getItems(), pathRef);
            }
        }
    }

    protected boolean isLocalRef(String ref) {
        if (ref.startsWith("#")) {
            return true;
        }
        return false;
    }

    protected String computeLocalRef(String ref, String prefix) {
        return prefix + ref;
    }
}
