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

import static com.ibm.ws.microprofile.openapi.impl.parser.util.RefUtils.computeDefinitionName;
import static com.ibm.ws.microprofile.openapi.impl.parser.util.RefUtils.computeRefFormat;
import static com.ibm.ws.microprofile.openapi.impl.parser.util.RefUtils.isAnExternalRefFormat;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import com.ibm.ws.microprofile.openapi.impl.model.ComponentsImpl;
import com.ibm.ws.microprofile.openapi.impl.parser.ResolverCache;
import com.ibm.ws.microprofile.openapi.impl.parser.models.RefFormat;

public final class ExternalRefProcessor {
    //private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ExternalRefProcessor.class);

    private final ResolverCache cache;
    private final OpenAPI openAPI;

    public ExternalRefProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.openAPI = openAPI;
    }

    public String processRefToExternalSchema(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final Schema schema = cache.loadRef($ref, refFormat, Schema.class);

        if (schema == null) {
            // stop!  There's a problem.  retain the original ref
            //LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
            //       "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new ComponentsImpl());
        }
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();

        if (schemas == null) {
            schemas = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, schemas.keySet());

        Schema existingModel = schemas.get(possiblyConflictingDefinitionName);

        if (existingModel != null) {
            //LOGGER.debug("A model for " + existingModel + " already exists");
            if (existingModel.getRef() != null) {
                // use the new model
                existingModel = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingModel == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addSchema(newRef, schema);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (schema.getRef() != null) {
                RefFormat format = computeRefFormat(schema.getRef());
                if (isAnExternalRefFormat(format)) {
                    schema.setRef(processRefToExternalSchema(schema.getRef(), format));
                } else {
                    processRefToExternalSchema(file + schema.getRef(), RefFormat.RELATIVE);
                }
            }
            //Loop the properties and recursively call this method;
            Map<String, Schema> subProps = schema.getProperties();
            if (subProps != null) {
                for (Map.Entry<String, Schema> prop : subProps.entrySet()) {
                    if (prop.getValue().getRef() != null) {
                        processRefProperty(prop.getValue(), file);
                    } else if (prop.getValue().getType() == SchemaType.ARRAY) {
                        Schema arrayProp = prop.getValue();
                        if (arrayProp.getItems().getRef() != null) {
                            processRefProperty(arrayProp.getItems(), file);
                        }
                    } else if (prop.getValue().getAdditionalProperties() != null) {
                        Schema mapProp = prop.getValue();
                        if (mapProp.getAdditionalProperties() instanceof Schema) {
                            Schema additionalProps = (Schema) mapProp.getAdditionalProperties();
                            if (additionalProps.getRef() != null) {
                                processRefProperty(additionalProps, file);
                            } else if (additionalProps.getType() == SchemaType.ARRAY &&
                                       additionalProps.getItems().getRef() != null) {
                                processRefProperty(additionalProps.getItems(), file);
                            }
                        }
                    }
                }
            }
            if (schema.getType() == SchemaType.ARRAY && schema.getItems().getRef() != null) {
                processRefProperty(schema.getItems(), file);
            }
        }

        return newRef;
    }

    public String processRefToExternalResponse(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final APIResponse response = cache.loadRef($ref, refFormat, APIResponse.class);

        if (response == null) {
            // stop!  There's a problem.  retain the original ref
            //LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
            //          "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new ComponentsImpl());
        }
        Map<String, APIResponse> responses = openAPI.getComponents().getResponses();

        if (responses == null) {
            responses = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, responses.keySet());

        APIResponse existingResponse = responses.get(possiblyConflictingDefinitionName);

        if (existingResponse != null) {
            //LOGGER.debug("A model for " + existingResponse + " already exists");
            if (existingResponse.getRef() != null) {
                // use the new model
                existingResponse = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingResponse == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addResponse(newRef, response);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (response.getRef() != null) {
                RefFormat format = computeRefFormat(response.getRef());
                if (isAnExternalRefFormat(format)) {
                    response.setRef(processRefToExternalResponse(response.getRef(), format));
                } else {
                    processRefToExternalResponse(file + response.getRef(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalRequestBody(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final RequestBody body = cache.loadRef($ref, refFormat, RequestBody.class);

        if (body == null) {
            // stop!  There's a problem.  retain the original ref
            //LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
            //           "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new ComponentsImpl());
        }
        Map<String, RequestBody> bodies = openAPI.getComponents().getRequestBodies();

        if (bodies == null) {
            bodies = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, bodies.keySet());

        RequestBody existingBody = bodies.get(possiblyConflictingDefinitionName);

        if (existingBody != null) {
            //LOGGER.debug("A model for " + existingBody + " already exists");
            if (existingBody.getRef() != null) {
                // use the new model
                existingBody = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingBody == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addRequestBody(newRef, body);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (body.getRef() != null) {
                RefFormat format = computeRefFormat(body.getRef());
                if (isAnExternalRefFormat(format)) {
                    body.setRef(processRefToExternalRequestBody(body.getRef(), format));
                } else {
                    processRefToExternalRequestBody(file + body.getRef(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalHeader(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final Header header = cache.loadRef($ref, refFormat, Header.class);

        if (header == null) {
            // stop!  There's a problem.  retain the original ref
            //LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
            //          "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new ComponentsImpl());
        }
        Map<String, Header> headers = openAPI.getComponents().getHeaders();

        if (headers == null) {
            headers = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, headers.keySet());

        Header existingHeader = headers.get(possiblyConflictingDefinitionName);

        if (existingHeader != null) {
            //LOGGER.debug("A model for " + existingHeader + " already exists");
            if (existingHeader.getRef() != null) {
                // use the new model
                existingHeader = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingHeader == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addHeader(newRef, header);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (header.getRef() != null) {
                RefFormat format = computeRefFormat(header.getRef());
                if (isAnExternalRefFormat(format)) {
                    header.setRef(processRefToExternalHeader(header.getRef(), format));
                } else {
                    processRefToExternalHeader(file + header.getRef(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalSecurityScheme(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final SecurityScheme securityScheme = cache.loadRef($ref, refFormat, SecurityScheme.class);

        if (securityScheme == null) {
            // stop!  There's a problem.  retain the original ref
            //LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
            //           "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new ComponentsImpl());
        }
        Map<String, SecurityScheme> securitySchemeMap = openAPI.getComponents().getSecuritySchemes();

        if (securitySchemeMap == null) {
            securitySchemeMap = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, securitySchemeMap.keySet());

        SecurityScheme existingSecurityScheme = securitySchemeMap.get(possiblyConflictingDefinitionName);

        if (existingSecurityScheme != null) {
            //LOGGER.debug("A model for " + existingSecurityScheme + " already exists");
            if (existingSecurityScheme.getRef() != null) {
                // use the new model
                existingSecurityScheme = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingSecurityScheme == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addSecurityScheme(newRef, securityScheme);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (securityScheme.getRef() != null) {
                RefFormat format = computeRefFormat(securityScheme.getRef());
                if (isAnExternalRefFormat(format)) {
                    securityScheme.setRef(processRefToExternalSecurityScheme(securityScheme.getRef(), format));
                } else {
                    processRefToExternalSecurityScheme(file + securityScheme.getRef(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalLink(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final Link link = cache.loadRef($ref, refFormat, Link.class);

        if (link == null) {
            // stop!  There's a problem.  retain the original ref
            //LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
            //            "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new ComponentsImpl());
        }
        Map<String, Link> links = openAPI.getComponents().getLinks();

        if (links == null) {
            links = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, links.keySet());

        Link existingLink = links.get(possiblyConflictingDefinitionName);

        if (existingLink != null) {
            //LOGGER.debug("A model for " + existingLink + " already exists");
            if (existingLink.getRef() != null) {
                // use the new model
                existingLink = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingLink == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addLink(newRef, link);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (link.getRef() != null) {
                RefFormat format = computeRefFormat(link.getRef());
                if (isAnExternalRefFormat(format)) {
                    link.setRef(processRefToExternalLink(link.getRef(), format));
                } else {
                    processRefToExternalLink(file + link.getRef(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalExample(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final Example example = cache.loadRef($ref, refFormat, Example.class);

        if (example == null) {
            // stop!  There's a problem.  retain the original ref
            //LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
            //            "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new ComponentsImpl());
        }
        Map<String, Example> examples = openAPI.getComponents().getExamples();

        if (examples == null) {
            examples = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, examples.keySet());

        Example existingExample = examples.get(possiblyConflictingDefinitionName);

        if (existingExample != null) {
            //LOGGER.debug("A model for " + existingExample + " already exists");
            if (existingExample.getRef() != null) {
                // use the new model
                existingExample = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingExample == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addExample(newRef, example);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (example.getRef() != null) {
                RefFormat format = computeRefFormat(example.getRef());
                if (isAnExternalRefFormat(format)) {
                    example.setRef(processRefToExternalExample(example.getRef(), format));
                } else {
                    processRefToExternalExample(file + example.getRef(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalParameter(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final Parameter parameter = cache.loadRef($ref, refFormat, Parameter.class);

        if (parameter == null) {
            // stop!  There's a problem.  retain the original ref
            //LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
            //            "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new ComponentsImpl());
        }
        Map<String, Parameter> parameters = openAPI.getComponents().getParameters();

        if (parameters == null) {
            parameters = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, parameters.keySet());

        Parameter existingParameters = parameters.get(possiblyConflictingDefinitionName);

        if (existingParameters != null) {
            //LOGGER.debug("A model for " + existingParameters + " already exists");
            if (existingParameters.getRef() != null) {
                // use the new model
                existingParameters = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingParameters == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addParameter(newRef, parameter);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (parameter.getRef() != null) {
                RefFormat format = computeRefFormat(parameter.getRef());
                if (isAnExternalRefFormat(format)) {
                    parameter.setRef(processRefToExternalParameter(parameter.getRef(), format));
                } else {
                    processRefToExternalParameter(file + parameter.getRef(), RefFormat.RELATIVE);
                }
            }
        }

        return newRef;
    }

    public String processRefToExternalCallback(String $ref, RefFormat refFormat) {
        String renamedRef = cache.getRenamedRef($ref);
        if (renamedRef != null) {
            return renamedRef;
        }

        final Callback callback = cache.loadRef($ref, refFormat, Callback.class);

        if (callback == null) {
            // stop!  There's a problem.  retain the original ref
            //LOGGER.warn("unable to load model reference from `" + $ref + "`.  It may not be available " +
            //            "or the reference isn't a valid model schema");
            return $ref;
        }
        String newRef;

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new ComponentsImpl());
        }
        Map<String, Callback> callbacks = openAPI.getComponents().getCallbacks();

        if (callbacks == null) {
            callbacks = new LinkedHashMap<>();
        }

        final String possiblyConflictingDefinitionName = computeDefinitionName($ref, callback.keySet());

        Callback existingCallback = callbacks.get(possiblyConflictingDefinitionName);

        if (existingCallback != null) {
            //LOGGER.debug("A model for " + existingCallback + " already exists");
            if (existingCallback.get("$ref").getRef() != null) {
                // use the new model
                existingCallback = null;
            }
        }
        newRef = possiblyConflictingDefinitionName;
        cache.putRenamedRef($ref, newRef);

        if (existingCallback == null) {
            // don't overwrite existing model reference
            openAPI.getComponents().addCallback(newRef, callback);
            cache.addReferencedKey(newRef);

            String file = $ref.split("#/")[0];
            if (callback.get("$ref") != null) {
                if (callback.get("$ref").getRef() != null) {
                    RefFormat format = computeRefFormat(callback.get("$ref").getRef());
                    if (isAnExternalRefFormat(format)) {
                        callback.get("$ref").setRef(processRefToExternalCallback(callback.get("$ref").getRef(), format));
                    } else {
                        processRefToExternalCallback(file + callback.get("$ref").getRef(), RefFormat.RELATIVE);
                    }
                }
            }
        }

        return newRef;
    }

    private void processRefProperty(Schema subRef, String externalFile) {
        RefFormat format = computeRefFormat(subRef.getRef());
        if (isAnExternalRefFormat(format)) {
            String $ref = constructRef(subRef, externalFile);
            subRef.setRef($ref);
            if ($ref.startsWith("."))
                processRefToExternalSchema($ref, RefFormat.RELATIVE);
            else {
                processRefToExternalSchema($ref, RefFormat.URL);
            }

        } else {
            processRefToExternalSchema(externalFile + subRef.getRef(), RefFormat.RELATIVE);
        }
    }

    protected String constructRef(Schema refProperty, String rootLocation) {
        String ref = refProperty.getRef();
        return join(rootLocation, ref);
    }

    public static String join(String source, String fragment) {
        try {
            boolean isRelative = false;
            if (source.startsWith("/") || source.startsWith(".")) {
                isRelative = true;
            }
            URI uri = new URI(source);

            if (!source.endsWith("/") && (fragment.startsWith("./") && "".equals(uri.getPath()))) {
                uri = new URI(source + "/");
            } else if ("".equals(uri.getPath()) && !fragment.startsWith("/")) {
                uri = new URI(source + "/");
            }
            URI f = new URI(fragment);

            URI resolved = uri.resolve(f);

            URI normalized = resolved.normalize();
            if (Character.isAlphabetic(normalized.toString().charAt(0)) && isRelative) {
                return "./" + normalized.toString();
            }
            return normalized.toString();
        } catch (Exception e) {
            return source;
        }
    }

}