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

package com.ibm.ws.microprofile.openapi.impl.jaxrs2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.ibm.ws.microprofile.openapi.impl.core.converter.ModelConverters;
import com.ibm.ws.microprofile.openapi.impl.core.converter.ResolvedSchema;
import com.ibm.ws.microprofile.openapi.impl.core.util.AnnotationsUtils;
import com.ibm.ws.microprofile.openapi.impl.core.util.Json;
import com.ibm.ws.microprofile.openapi.impl.core.util.ParameterProcessor;
import com.ibm.ws.microprofile.openapi.impl.core.util.PathUtils;
import com.ibm.ws.microprofile.openapi.impl.core.util.ReflectionUtils;
import com.ibm.ws.microprofile.openapi.impl.jaxrs2.ext.OpenAPIExtension;
import com.ibm.ws.microprofile.openapi.impl.jaxrs2.ext.OpenAPIExtensions;
import com.ibm.ws.microprofile.openapi.impl.jaxrs2.util.ReaderUtils;
import com.ibm.ws.microprofile.openapi.impl.model.ComponentsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.OperationImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.callbacks.CallbackImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.ContentImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.MediaTypeImpl;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.RequestBodyImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponseImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponsesImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.SecuritySchemeImpl;

public class Reader {
    //private static final Logger LOGGER = LoggerFactory.getLogger(Reader.class);

    public static final String DEFAULT_MEDIA_TYPE_VALUE = "*/*";
    public static final String DEFAULT_DESCRIPTION = "default response";

    private Application application;
    private final OpenAPI openAPI;
    private final Components components;
    private final Paths paths;
    private final Set<Tag> openApiTags;

    private static final String GET_METHOD = "get";
    private static final String POST_METHOD = "post";
    private static final String PUT_METHOD = "put";
    private static final String DELETE_METHOD = "delete";
    private static final String PATCH_METHOD = "patch";
    private static final String TRACE_METHOD = "trace";
    private static final String HEAD_METHOD = "head";
    private static final String OPTIONS_METHOD = "options";

    public Reader() {
        this.openAPI = new OpenAPIImpl();
        paths = new PathsImpl();
        openApiTags = new LinkedHashSet<>();
        components = new ComponentsImpl();

    }

    public Reader(OpenAPI openAPI) {
        if (openAPI != null) {
            this.openAPI = openAPI;
        } else {
            this.openAPI = new OpenAPIImpl();
            openAPI = this.openAPI;
        }
        if (openAPI.getPaths() != null) {
            this.paths = openAPI.getPaths();
        } else {
            this.paths = new PathsImpl();
        }
        openApiTags = new LinkedHashSet<>();
        if (openAPI.getTags() != null)
            openApiTags.addAll(openAPI.getTags());
        if (openAPI.getComponents() != null) {
            this.components = openAPI.getComponents();
        } else {
            this.components = new ComponentsImpl();
        }
    }

    public OpenAPI getOpenAPI() {
        return openAPI;
    }

    /**
     * Scans a single class for Swagger annotations - does not invoke ReaderListeners
     */
    public OpenAPI read(Class<?> cls) {
        return read(cls, resolveApplicationPath());
    }

    /**
     * Scans a set of classes for both ReaderListeners and OpenAPI annotations. All found listeners will
     * be instantiated before any of the classes are scanned for OpenAPI annotations - so they can be invoked
     * accordingly.
     *
     * @param classes a set of classes to scan
     * @return the generated OpenAPI definition
     */
    public OpenAPI read(Set<Class<?>> classes) {
        Set<Class<?>> sortedClasses = new TreeSet<>(new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> class1, Class<?> class2) {
                if (class1.equals(class2)) {
                    return 0;
                } else if (class1.isAssignableFrom(class2)) {
                    return -1;
                } else if (class2.isAssignableFrom(class1)) {
                    return 1;
                }
                return class1.getName().compareTo(class2.getName());
            }
        });
        sortedClasses.addAll(classes);

        for (Class<?> cls : sortedClasses) {
            read(cls, resolveApplicationPath());
        }
        return openAPI;
    }

    public OpenAPI read(Set<Class<?>> classes, Map<String, Object> resources) {
        return read(classes);
    }

    protected String resolveApplicationPath() {
        if (application != null) {
            ApplicationPath applicationPath = application.getClass().getAnnotation(ApplicationPath.class);
            if (applicationPath != null) {
                if (StringUtils.isNotBlank(applicationPath.value())) {
                    return applicationPath.value();
                }
            }
            // look for inner application, e.g. ResourceConfig
            try {
                Application innerApp = application;
                Method m = application.getClass().getMethod("getApplication", null);
                while (m != null) {
                    Application retrievedApp = (Application) m.invoke(innerApp, null);
                    if (retrievedApp == null) {
                        break;
                    }
                    if (retrievedApp.getClass().equals(innerApp.getClass())) {
                        break;
                    }
                    innerApp = retrievedApp;
                    applicationPath = innerApp.getClass().getAnnotation(ApplicationPath.class);
                    if (applicationPath != null) {
                        if (StringUtils.isNotBlank(applicationPath.value())) {
                            return applicationPath.value();
                        }
                    }
                    m = innerApp.getClass().getMethod("getApplication", null);
                }
            } catch (NoSuchMethodException e) {
                // no inner application found
            } catch (Exception e) {
                // no inner application found
            }
        }
        return "";
    }

    public OpenAPI read(Class<?> cls, String parentPath) {

        List<org.eclipse.microprofile.openapi.annotations.security.SecurityScheme> apiSecurityScheme = ReflectionUtils.getRepeatableAnnotations(cls,
                                                                                                                                                org.eclipse.microprofile.openapi.annotations.security.SecurityScheme.class);
        List<org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement> apiSecurityRequirements = ReflectionUtils.getRepeatableAnnotations(cls,
                                                                                                                                                           org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement.class);
        List<org.eclipse.microprofile.openapi.annotations.servers.Server> apiServers = ReflectionUtils.getRepeatableAnnotations(cls,
                                                                                                                                org.eclipse.microprofile.openapi.annotations.servers.Server.class);
        List<org.eclipse.microprofile.openapi.annotations.callbacks.Callback> apiCallbacks = ReflectionUtils.getRepeatableAnnotations(cls,
                                                                                                                                      org.eclipse.microprofile.openapi.annotations.callbacks.Callback.class);

        ExternalDocumentation apiExternalDocs = ReflectionUtils.getAnnotation(cls, ExternalDocumentation.class);
        org.eclipse.microprofile.openapi.annotations.tags.Tag[] apiTags = ReflectionUtils.getRepeatableAnnotationsArray(cls,
                                                                                                                        org.eclipse.microprofile.openapi.annotations.tags.Tag.class);
        org.eclipse.microprofile.openapi.annotations.tags.Tags tagsAnnotation = ReflectionUtils.getAnnotation(cls,
                                                                                                              org.eclipse.microprofile.openapi.annotations.tags.Tags.class);

        javax.ws.rs.Consumes classConsumes = ReflectionUtils.getAnnotation(cls, javax.ws.rs.Consumes.class);
        javax.ws.rs.Produces classProduces = ReflectionUtils.getAnnotation(cls, javax.ws.rs.Produces.class);

        // class security schemes
        if (apiSecurityScheme != null) {
            for (org.eclipse.microprofile.openapi.annotations.security.SecurityScheme securitySchemeAnnotation : apiSecurityScheme) {
                Optional<SecurityScheme> securityScheme = SecurityParser.getSecurityScheme(securitySchemeAnnotation);
                if (securityScheme.isPresent()) {
                    Map<String, SecurityScheme> securitySchemeMap = new HashMap<>();
                    if (StringUtils.isNotBlank(((SecuritySchemeImpl) securityScheme.get()).getSchemeName())) {
                        securitySchemeMap.put(((SecuritySchemeImpl) securityScheme.get()).getSchemeName(), securityScheme.get());
                        if (components.getSecuritySchemes() != null && components.getSecuritySchemes().size() != 0) {
                            components.getSecuritySchemes().putAll(securitySchemeMap);
                        } else {
                            components.setSecuritySchemes(securitySchemeMap);
                        }
                    }
                }
            }
        }

        // class security requirements
        List<SecurityRequirement> classSecurityRequirements = new ArrayList<>();
        if (apiSecurityRequirements != null) {
            Optional<List<SecurityRequirement>> requirementsObject = SecurityParser.getSecurityRequirements(
                                                                                                            apiSecurityRequirements.toArray(new org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement[apiSecurityRequirements.size()]));
            if (requirementsObject.isPresent()) {
                classSecurityRequirements = requirementsObject.get();
            }
        }

        List<org.eclipse.microprofile.openapi.models.servers.Server> classServers = new ArrayList<>();
        if (apiServers != null) {
            Optional<List<org.eclipse.microprofile.openapi.models.servers.Server>> serversObject = AnnotationsUtils.getServers(
                                                                                                                               apiServers.toArray(new org.eclipse.microprofile.openapi.annotations.servers.Server[apiServers.size()]));
            if (serversObject.isPresent()) {
                classServers = serversObject.get();
            }
        }

        Map<String, Callback> classCallbacks = null;
        if (apiCallbacks != null) {
            Map<String, Callback> callbacks = new LinkedHashMap<>();
            for (org.eclipse.microprofile.openapi.annotations.callbacks.Callback classCallback : apiCallbacks) {
                Map<String, Callback> currentCallbacks = getCallbacks(classCallback);
                callbacks.putAll(currentCallbacks);
            }
            if (callbacks.size() > 0) {
                classCallbacks = callbacks;
            }
        }

        // class tags, consider only name to add to class operations
        final Set<String> classTags = new LinkedHashSet<>();
        if (apiTags != null) {
            AnnotationsUtils.getTags(apiTags, false).ifPresent(tags -> tags.stream().map(t -> t.getName()).forEach(t -> classTags.add(t)));
        }
        if (tagsAnnotation != null && tagsAnnotation.refs() != null) {
            classTags.addAll(Stream.of(tagsAnnotation.refs()).filter(StringUtils::isNotBlank).collect(Collectors.toList()));
        }

        // class external docs
        Optional<org.eclipse.microprofile.openapi.models.ExternalDocumentation> classExternalDocumentation = AnnotationsUtils.getExternalDocumentation(apiExternalDocs);

        // class path
        final javax.ws.rs.Path apiPath = ReflectionUtils.getAnnotation(cls, javax.ws.rs.Path.class);

        JavaType classType = TypeFactory.defaultInstance().constructType(cls);
        BeanDescription bd = Json.mapper().getSerializationConfig().introspect(classType);

        final List<Parameter> globalParameters = new ArrayList<>();

        // look for constructor-level annotated properties
        globalParameters.addAll(ReaderUtils.collectConstructorParameters(cls, components, classConsumes));

        // look for field-level annotated properties
        globalParameters.addAll(ReaderUtils.collectFieldParameters(cls, components, classConsumes));

        // iterate class methods
        Method methods[] = cls.getMethods();
        for (Method method : methods) {
            if (isOperationHidden(method)) {
                continue;
            }
            AnnotatedMethod annotatedMethod = bd.findMethod(method.getName(), method.getParameterTypes());
            javax.ws.rs.Produces methodProduces = ReflectionUtils.getAnnotation(method, javax.ws.rs.Produces.class);
            javax.ws.rs.Consumes methodConsumes = ReflectionUtils.getAnnotation(method, javax.ws.rs.Consumes.class);

            if (ReflectionUtils.isOverriddenMethod(method, cls)) {
                continue;
            }
            javax.ws.rs.Path methodPath = ReflectionUtils.getAnnotation(method, javax.ws.rs.Path.class);

            String operationPath = ReaderUtils.getPath(apiPath, methodPath, parentPath);

            // skip if path is the same as parent, e.g. for @ApplicationPath annotated application
            // extending resource config.
            if (ignoreOperationPath(operationPath, parentPath)) {
                continue;
            }

            Map<String, String> regexMap = new LinkedHashMap<>();
            operationPath = PathUtils.parsePath(operationPath, regexMap);
            if (operationPath != null) {

                Operation operation = parseMethod(
                                                  method,
                                                  globalParameters,
                                                  methodProduces,
                                                  classProduces,
                                                  methodConsumes,
                                                  classConsumes,
                                                  classSecurityRequirements,
                                                  classExternalDocumentation,
                                                  classTags,
                                                  classServers,
                                                  classCallbacks);
                if (operation != null) {
                    PathItem pathItemObject;
                    if (openAPI.getPaths() != null && openAPI.getPaths().get(operationPath) != null) {
                        pathItemObject = openAPI.getPaths().get(operationPath);
                    } else {
                        pathItemObject = new PathItemImpl();
                    }

                    String httpMethod = ReaderUtils.extractOperationMethod(operation, method, OpenAPIExtensions.chain());
                    if (StringUtils.isBlank(httpMethod)) {
                        continue;
                    }
                    setPathItemOperation(pathItemObject, httpMethod, operation);

                    List<Parameter> operationParameters = new ArrayList<>();
                    Annotation[][] paramAnnotations = ReflectionUtils.getParameterAnnotations(method);
                    if (annotatedMethod == null) { // annotatedMethod not null only when method with 0-2 parameters
                        Type[] genericParameterTypes = method.getGenericParameterTypes();
                        for (int i = 0; i < genericParameterTypes.length; i++) {
                            final Type type = TypeFactory.defaultInstance().constructType(genericParameterTypes[i], cls);
                            ResolvedParameter resolvedParameter = getParameters(type, Arrays.asList(paramAnnotations[i]), operation, classConsumes, methodConsumes);
                            for (Parameter p : resolvedParameter.parameters) {
                                operationParameters.add(p);
                            }
                            if (resolvedParameter.requestBody != null) {
                                processRequestBody(
                                                   resolvedParameter.requestBody,
                                                   operation,
                                                   methodConsumes,
                                                   classConsumes,
                                                   operationParameters,
                                                   paramAnnotations[i],
                                                   type, method.getAnnotation(org.eclipse.microprofile.openapi.annotations.parameters.RequestBody.class));
                            }
                        }
                    } else {
                        for (int i = 0; i < annotatedMethod.getParameterCount(); i++) {
                            AnnotatedParameter param = annotatedMethod.getParameter(i);
                            final Type type = TypeFactory.defaultInstance().constructType(param.getParameterType(), cls);
                            ResolvedParameter resolvedParameter = getParameters(type, Arrays.asList(paramAnnotations[i]), operation, classConsumes, methodConsumes);
                            for (Parameter p : resolvedParameter.parameters) {
                                operationParameters.add(p);
                            }
                            if (resolvedParameter.requestBody != null) {
                                processRequestBody(
                                                   resolvedParameter.requestBody,
                                                   operation,
                                                   methodConsumes,
                                                   classConsumes,
                                                   operationParameters,
                                                   paramAnnotations[i],
                                                   type, method.getAnnotation(org.eclipse.microprofile.openapi.annotations.parameters.RequestBody.class));
                            }
                        }
                    }

                    if (operationParameters.size() > 0) {
                        for (Parameter operationParameter : operationParameters) {
                            operation.addParameter(operationParameter);
                        }
                    }

                    paths.addPathItem(operationPath, pathItemObject);
                    if (openAPI.getPaths() != null) {
                        this.paths.putAll(openAPI.getPaths());
                    }

                    openAPI.setPaths(this.paths);
                }
            }
        }

        // add tags from class to definition tags
        AnnotationsUtils.getTags(apiTags, true).ifPresent(tags -> openApiTags.addAll(tags));

        if (!openApiTags.isEmpty()) {
            Set<Tag> tagsSet = new LinkedHashSet<>();
            if (openAPI.getTags() != null) {
                for (Tag tag : openAPI.getTags()) {
                    if (tagsSet.stream().noneMatch(t -> t.getName().equals(tag.getName()))) {
                        tagsSet.add(tag);
                    }
                }
            }
            for (Tag tag : openApiTags) {
                if (tagsSet.stream().noneMatch(t -> t.getName().equals(tag.getName()))) {
                    tagsSet.add(tag);
                }
            }
            openAPI.setTags(new ArrayList<>(tagsSet));
        }

        // Process OpenAPIDefinition annotation last since it takes precedence over any conflicting items specified elsewhere.
        // If there are more than one openAPIDefinition annotation is present across the app, there is no guarantee which one is picked.
        handleOpenAPIDefinition(ReflectionUtils.getAnnotation(cls, OpenAPIDefinition.class));

        // if no components object is defined in openApi instance passed by client, set openAPI.components to resolved components (if not empty)
        //Note: this must be done after invoking 'handleOpenAPIDefinition' method so the 'components' from OpenAPIDefinition would have been processed.
        if (!isEmptyComponents(components) && openAPI.getComponents() == null) {
            openAPI.setComponents(components);
        }

        return openAPI;
    }

    /**
     * @param openAPIDefinition OpenAPI information from OpenAPIDefinition annotation. Overrides other definitions.
     */
    private void handleOpenAPIDefinition(OpenAPIDefinition openAPIDefinition) {
        if (openAPIDefinition != null) {
            // info
            AnnotationsUtils.getInfo(openAPIDefinition.info()).ifPresent(info -> openAPI.setInfo(info));

            // OpenApiDefinition security requirements
            SecurityParser.getSecurityRequirements(openAPIDefinition.security()).ifPresent(s -> openAPI.setSecurity(s));

            // OpenApiDefinition external docs
            AnnotationsUtils.getExternalDocumentation(openAPIDefinition.externalDocs()).ifPresent(docs -> openAPI.setExternalDocs(docs));

            // OpenApiDefinition tags
            AnnotationsUtils.getTags(openAPIDefinition.tags(), false).ifPresent(tags -> openApiTags.addAll(tags));

            // OpenApiDefinition servers
            AnnotationsUtils.getServers(openAPIDefinition.servers()).ifPresent(servers -> openAPI.setServers(servers));

            // handle components from the OpenAPIDefinition annotation
            handleComponentsAnnotation(openAPIDefinition.components());
        }
    }

    private void handleComponentsAnnotation(org.eclipse.microprofile.openapi.annotations.Components annotationComponents) {
        if (annotationComponents == null) {
            return;
        }

        // headers
        Optional<Map<String, org.eclipse.microprofile.openapi.models.headers.Header>> headers = AnnotationsUtils.getHeaders(annotationComponents.headers());
        if (headers.isPresent()) {
            if (components.getHeaders() == null) {
                components.setHeaders(headers.get());
            } else {
                components.getHeaders().putAll(headers.get());
            }
        }

        // responses
        Optional<APIResponses> responses = AnnotationsUtils.getApiResponses(annotationComponents.responses(), null, null, components, false);
        if (responses.isPresent()) {
            if (components.getResponses() == null) {
                components.setResponses(responses.get());
            } else {
                components.getResponses().putAll(responses.get());
            }
        }

        // parameters
        Optional<List<Parameter>> parameters = getParametersListFromAnnotation(annotationComponents.parameters(), null, null, null);
        if (parameters.isPresent()) {
            for (org.eclipse.microprofile.openapi.models.parameters.Parameter parameter : parameters.get()) {
                components.addParameter(parameter.getName(), parameter);
            }
        }

        // examples
        Map<String, Example> exampleMap = new HashMap<>();
        for (ExampleObject exampleObject : annotationComponents.examples()) {
            AnnotationsUtils.getExample(exampleObject).ifPresent(example -> exampleMap.put(exampleObject.name(), example));
        }
        if (exampleMap.size() > 0) {
            if (components.getExamples() == null) {
                components.setExamples(exampleMap);
            } else {
                components.getExamples().putAll(exampleMap);
            }
        }

        // requestBodies
        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        for (org.eclipse.microprofile.openapi.annotations.parameters.RequestBody requestBody : annotationComponents.requestBodies()) {
            OperationParser.getRequestBody(requestBody, null, null, components).ifPresent(request -> requestBodyMap.put(requestBody.name(), request));
        }
        if (requestBodyMap.size() > 0) {
            if (components.getRequestBodies() == null) {
                components.setRequestBodies(requestBodyMap);
            } else {
                components.getRequestBodies().putAll(requestBodyMap);
            }
        }

        // securitySchemes
        Map<String, SecurityScheme> securitySchemeMap = new HashMap<>();
        for (org.eclipse.microprofile.openapi.annotations.security.SecurityScheme securityScheme : annotationComponents.securitySchemes()) {
            SecurityParser.getSecurityScheme(securityScheme).ifPresent(security -> securitySchemeMap.put(securityScheme.securitySchemeName(), security));
        }
        if (securitySchemeMap.size() > 0) {
            if (components.getSecuritySchemes() == null) {
                components.setSecuritySchemes(securitySchemeMap);
            } else {
                components.getSecuritySchemes().putAll(securitySchemeMap);
            }
        }

        // links
        Map<String, Link> linksMap = AnnotationsUtils.getLinks(annotationComponents.links());
        if (linksMap.size() > 0) {
            if (components.getLinks() == null) {
                components.setLinks(linksMap);
            } else {
                components.getLinks().putAll(linksMap);
            }
        }

        // callbacks
        Map<String, Callback> callbackMap = new LinkedHashMap<>();
        for (org.eclipse.microprofile.openapi.annotations.callbacks.Callback callback : annotationComponents.callbacks()) {
            callbackMap.putAll(getCallbacks(callback));
        }
        if (callbackMap.size() > 0) {
            if (components.getCallbacks() == null) {
                components.setCallbacks(callbackMap);
            } else {
                components.getCallbacks().putAll(callbackMap);
            }
        }

        // schemas
        for (org.eclipse.microprofile.openapi.annotations.media.Schema schema : annotationComponents.schemas()) {
            Optional<? extends Schema> optSchema = AnnotationsUtils.getSchema(schema, components);
            if (optSchema.isPresent()) {
                components.addSchema(schema.name(), optSchema.get());
            }
        }
    }

    protected Content processContent(Content content, Schema schema, Consumes methodConsumes, Consumes classConsumes) {
        if (content == null) {
            content = new ContentImpl();
        }
        if (methodConsumes != null) {
            for (String value : methodConsumes.value()) {
                setMediaTypeToContent(schema, content, value);
            }
        } else if (classConsumes != null) {
            for (String value : classConsumes.value()) {
                setMediaTypeToContent(schema, content, value);
            }
        } else {
            setMediaTypeToContent(schema, content, DEFAULT_MEDIA_TYPE_VALUE);
        }
        return content;
    }

    protected void processRequestBody(Parameter requestBodyParameter, Operation operation,
                                      Consumes methodConsumes, Consumes classConsumes,
                                      List<Parameter> operationParameters,
                                      Annotation[] paramAnnotations, Type type, org.eclipse.microprofile.openapi.annotations.parameters.RequestBody methododRequestBody) {
        if (operation.getRequestBody() == null) {
            org.eclipse.microprofile.openapi.annotations.parameters.RequestBody requestBodyAnnotation = getRequestBody(Arrays.asList(paramAnnotations));

            if (requestBodyAnnotation == null) {
                if (methododRequestBody != null) {
                    requestBodyAnnotation = methododRequestBody;
                }
            }

            if (requestBodyAnnotation != null) {
                Optional<RequestBody> optionalRequestBody = OperationParser.getRequestBody(requestBodyAnnotation, classConsumes, methodConsumes, components);
                if (optionalRequestBody.isPresent()) {
                    RequestBody requestBody = optionalRequestBody.get();
                    if (StringUtils.isBlank(requestBody.getRef()) &&
                        (requestBody.getContent() == null || requestBody.getContent().isEmpty())) {
                        if (requestBodyParameter.getSchema() != null) {
                            Content content = processContent(requestBody.getContent(), requestBodyParameter.getSchema(), methodConsumes, classConsumes);
                            requestBody.setContent(content);
                        }
                    } else if (StringUtils.isBlank(requestBody.getRef()) &&
                               requestBody.getContent() != null &&
                               !requestBody.getContent().isEmpty()) {
                        if (requestBodyParameter.getSchema() != null) {
                            for (MediaType mediaType : requestBody.getContent().values()) {
                                if (mediaType.getSchema().getType() == null) {
                                    mediaType.getSchema().setType(requestBodyParameter.getSchema().getType());
                                }
                            }
                        }
                    }

                    operation.setRequestBody(requestBody);
                }
            } else {
                boolean isRequestBodyEmpty = true;
                RequestBody requestBody = new RequestBodyImpl();
                if (StringUtils.isNotBlank(requestBodyParameter.getRef())) {
                    requestBody.setRef(requestBodyParameter.getRef());
                    isRequestBodyEmpty = false;
                }
                if (StringUtils.isNotBlank(requestBodyParameter.getDescription())) {
                    requestBody.setDescription(requestBodyParameter.getDescription());
                    isRequestBodyEmpty = false;
                }
                if (Boolean.TRUE.equals(requestBodyParameter.getRequired())) {
                    requestBody.setRequired(requestBodyParameter.getRequired());
                    isRequestBodyEmpty = false;
                }

                if (requestBodyParameter.getSchema() != null) {
                    Content content = processContent(null, requestBodyParameter.getSchema(), methodConsumes, classConsumes);
                    requestBody.setContent(content);
                    isRequestBodyEmpty = false;
                }
                if (!isRequestBodyEmpty) {
                    operation.setRequestBody(requestBody);
                }
            }
        }
    }

    private org.eclipse.microprofile.openapi.annotations.parameters.RequestBody getRequestBody(List<Annotation> annotations) {
        if (annotations == null) {
            return null;
        }
        for (Annotation a : annotations) {
            if (a instanceof org.eclipse.microprofile.openapi.annotations.parameters.RequestBody) {
                return (org.eclipse.microprofile.openapi.annotations.parameters.RequestBody) a;
            }
        }
        return null;
    }

    private void setMediaTypeToContent(Schema schema, Content content, String value) {
        MediaType mediaTypeObject = new MediaTypeImpl();
        mediaTypeObject.setSchema(schema);
        content.addMediaType(value, mediaTypeObject);
    }

    public Operation parseMethod(
                                 Method method,
                                 List<Parameter> globalParameters) {
        JavaType classType = TypeFactory.defaultInstance().constructType(method.getDeclaringClass());
        return parseMethod(
                           classType.getClass(),
                           method,
                           globalParameters,
                           null,
                           null,
                           null,
                           null,
                           new ArrayList<>(),
                           Optional.empty(),
                           new HashSet<>(),
                           null,
                           null);
    }

    public Operation parseMethod(
                                 Method method,
                                 List<Parameter> globalParameters,
                                 Produces methodProduces,
                                 Produces classProduces,
                                 Consumes methodConsumes,
                                 Consumes classConsumes,
                                 List<SecurityRequirement> classSecurityRequirements,
                                 Optional<org.eclipse.microprofile.openapi.models.ExternalDocumentation> classExternalDocs,
                                 Set<String> classTags,
                                 List<org.eclipse.microprofile.openapi.models.servers.Server> classServers,
                                 Map<String, Callback> classCallbacks) {
        JavaType classType = TypeFactory.defaultInstance().constructType(method.getDeclaringClass());
        return parseMethod(
                           classType.getClass(),
                           method,
                           globalParameters,
                           methodProduces,
                           classProduces,
                           methodConsumes,
                           classConsumes,
                           classSecurityRequirements,
                           classExternalDocs,
                           classTags,
                           classServers,
                           classCallbacks);
    }

    private Operation parseMethod(
                                  Class<?> cls,
                                  Method method,
                                  List<Parameter> globalParameters,
                                  Produces methodProduces,
                                  Produces classProduces,
                                  Consumes methodConsumes,
                                  Consumes classConsumes,
                                  List<SecurityRequirement> classSecurityRequirements,
                                  Optional<org.eclipse.microprofile.openapi.models.ExternalDocumentation> classExternalDocs,
                                  Set<String> classTags,
                                  List<org.eclipse.microprofile.openapi.models.servers.Server> classServers,
                                  Map<String, Callback> classCallbacks) {
        Operation operation = new OperationImpl();

        org.eclipse.microprofile.openapi.annotations.Operation apiOperation = ReflectionUtils.getAnnotation(method, org.eclipse.microprofile.openapi.annotations.Operation.class);

        List<org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement> apiSecurity = ReflectionUtils.getRepeatableAnnotations(method,
                                                                                                                                               org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement.class);
        List<org.eclipse.microprofile.openapi.annotations.callbacks.Callback> apiCallbacks = ReflectionUtils.getRepeatableAnnotations(method,
                                                                                                                                      org.eclipse.microprofile.openapi.annotations.callbacks.Callback.class);
        List<Server> apiServers = ReflectionUtils.getRepeatableAnnotations(method, Server.class);
        List<org.eclipse.microprofile.openapi.annotations.tags.Tag> apiTags = ReflectionUtils.getRepeatableAnnotations(method,
                                                                                                                       org.eclipse.microprofile.openapi.annotations.tags.Tag.class);
        org.eclipse.microprofile.openapi.annotations.tags.Tags tagsAnnotation = ReflectionUtils.getAnnotation(method,
                                                                                                              org.eclipse.microprofile.openapi.annotations.tags.Tags.class);
        List<org.eclipse.microprofile.openapi.annotations.parameters.Parameter> apiParameters = ReflectionUtils.getRepeatableAnnotations(method,
                                                                                                                                         org.eclipse.microprofile.openapi.annotations.parameters.Parameter.class);
        List<org.eclipse.microprofile.openapi.annotations.responses.APIResponse> apiResponses = ReflectionUtils.getRepeatableAnnotations(method,
                                                                                                                                         org.eclipse.microprofile.openapi.annotations.responses.APIResponse.class);
        // TODO extensions
        List<Extension> apiExtensions = ReflectionUtils.getRepeatableAnnotations(method, Extension.class);
        ExternalDocumentation apiExternalDocumentation = ReflectionUtils.getAnnotation(method, ExternalDocumentation.class);

        // callbacks
        Map<String, Callback> callbacks = new LinkedHashMap<>();
        if (apiCallbacks != null) {
            for (org.eclipse.microprofile.openapi.annotations.callbacks.Callback methodCallback : apiCallbacks) {
                Map<String, Callback> currentCallbacks = getCallbacks(methodCallback);
                callbacks.putAll(currentCallbacks);
            }
        }
        if (!callbacks.isEmpty()) {
            operation.setCallbacks(callbacks);
        } else {
            operation.setCallbacks(classCallbacks);
        }

        // security
        if (apiSecurity != null && apiSecurity.size() > 0) {
            Optional<List<SecurityRequirement>> requirementsObject = SecurityParser.getSecurityRequirements(apiSecurity.toArray(new org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement[apiSecurity.size()]));
            if (requirementsObject.isPresent()) {
                requirementsObject.get().stream().filter(r -> operation.getSecurity() == null || !operation.getSecurity().contains(r)).forEach(operation::addSecurityRequirement);
            }
        } else {
            classSecurityRequirements.forEach(operation::addSecurityRequirement);
        }

        // servers
        if (apiServers != null && apiServers.size() > 0) {
            AnnotationsUtils.getServers(apiServers.toArray(new Server[apiServers.size()])).ifPresent(servers -> servers.forEach(operation::addServer));
        } else if (classServers != null && classServers.size() > 0) {
            operation.setServers(classServers);
        }

        // external docs
        AnnotationsUtils.getExternalDocumentation(apiExternalDocumentation).ifPresent(operation::setExternalDocs);

        // method tags
        if ((apiTags != null && !apiTags.isEmpty()) || (tagsAnnotation != null && ArrayUtils.isNotEmpty(tagsAnnotation.refs()))) {
            Stream<String> operationTags = Stream.empty();
            if (apiTags != null) {
                operationTags = apiTags.stream().filter(t -> StringUtils.isNotBlank(t.name()) || StringUtils.isNotBlank(t.ref())).map(t -> {
                    if (StringUtils.isNotBlank(t.ref())) {
                        return t.ref();
                    } else {
                        return t.name();
                    }
                });
                AnnotationsUtils.getTags(apiTags.toArray(new org.eclipse.microprofile.openapi.annotations.tags.Tag[apiTags.size()]),
                                         true).ifPresent(tags -> openApiTags.addAll(tags));
            }
            if (tagsAnnotation != null && ArrayUtils.isNotEmpty(tagsAnnotation.refs())) {
                operationTags = Stream.concat(operationTags, Stream.of(tagsAnnotation.refs()).filter(StringUtils::isNotBlank));
            }
            operationTags.distinct().forEach(operation::addTag);
        }

        // class tags after tags defined as field of @Operation
        else if (classTags != null) {
            operation.setTags(new ArrayList<>(classTags));
        }

        // parameters
        if (globalParameters != null) {
            for (Parameter globalParameter : globalParameters) {
                operation.addParameter(globalParameter);
            }
        }
        if (apiParameters != null) {
            getParametersListFromAnnotation(
                                            //OperationParser.getParametersList(
                                            apiParameters.toArray(new org.eclipse.microprofile.openapi.annotations.parameters.Parameter[apiParameters.size()]),
                                            classConsumes,
                                            methodConsumes,
                                            operation).ifPresent(p -> p.forEach(operation::addParameter));
        }

        // apiResponses
        if (apiResponses != null) {
            OperationParser.getApiResponses(apiResponses.toArray(new org.eclipse.microprofile.openapi.annotations.responses.APIResponse[apiResponses.size()]), classProduces,
                                            methodProduces,
                                            components).ifPresent(responses -> {
                                                if (operation.getResponses() == null) {
                                                    operation.setResponses(responses);
                                                } else {
                                                    responses.forEach(operation.getResponses()::addApiResponse);
                                                }
                                            });
        }

        // operation id
        if (StringUtils.isBlank(operation.getOperationId())) {
            operation.setOperationId(method.getName());
        }

        if (apiOperation != null) {
            setOperationObjectFromApiOperationAnnotation(operation, apiOperation, methodProduces, classProduces, methodConsumes, classConsumes);
        }

        // external docs of class if not defined in annotation of method or as field of Operation annotation
        if (operation.getExternalDocs() == null) {
            classExternalDocs.ifPresent(operation::setExternalDocs);
        }

        // handle return type, add as response in case.
        Type returnType = method.getGenericReturnType();
        if (!shouldIgnoreClass(returnType.getTypeName())) {
            ResolvedSchema resolvedSchema = ModelConverters.getInstance().resolveAnnotatedType(returnType, new ArrayList<>(), "");
            if (resolvedSchema.schema != null) {
                Schema returnTypeSchema = resolvedSchema.schema;
                Content content = new ContentImpl();
                MediaType mediaType = new MediaTypeImpl().schema(returnTypeSchema);
                AnnotationsUtils.applyTypes(classConsumes == null ? new String[0] : classConsumes.value(),
                                            methodConsumes == null ? new String[0] : methodConsumes.value(), content, mediaType);
                if (operation.getResponses() == null) {
                    operation.responses(
                                        new APIResponsesImpl().defaultValue(
                                                                            new APIResponseImpl().description(DEFAULT_DESCRIPTION).content(content)));
                }
                if (operation.getResponses().getDefault() != null &&
                    StringUtils.isBlank(operation.getResponses().getDefault().getRef()) &&
                    operation.getResponses().getDefault().getContent() == null) {
                    operation.getResponses().getDefault().content(content);
                }
                Map<String, Schema> schemaMap = resolvedSchema.referencedSchemas;
                if (schemaMap != null) {
                    schemaMap.forEach((key, schema) -> components.addSchema(key, schema));
                }

            }
        }
        if (operation.getResponses() == null || operation.getResponses().size() == 0) {
            APIResponse apiResponseObject = new APIResponseImpl();
            apiResponseObject.setDescription(DEFAULT_DESCRIPTION);
            operation.setResponses(new APIResponsesImpl().defaultValue(apiResponseObject));

        }
        return operation;
    }

    private boolean shouldIgnoreClass(String className) {
        if (StringUtils.isBlank(className)) {
            return true;
        }
        boolean ignore = false;
        ignore = ignore || className.startsWith("javax.ws.rs.");
        ignore = ignore || className.equalsIgnoreCase("void");
        return ignore;
    }

    private Map<String, Callback> getCallbacks(org.eclipse.microprofile.openapi.annotations.callbacks.Callback apiCallback) {
        Map<String, Callback> callbackMap = new HashMap<>();
        if (apiCallback == null) {
            return callbackMap;
        }
        Callback callbackObject = new CallbackImpl();
        PathItem pathItemObject = new PathItemImpl();
        for (org.eclipse.microprofile.openapi.annotations.callbacks.CallbackOperation callbackOperation : apiCallback.operations()) {
            Operation callbackNewOperation = new OperationImpl();
            setOperationObjectFromApiOperationAnnotation(callbackNewOperation, callbackOperation);
            setPathItemOperation(pathItemObject, callbackOperation.method(), callbackNewOperation);
        }

        callbackObject.addPathItem(apiCallback.callbackUrlExpression(), pathItemObject);
        callbackMap.put(apiCallback.name(), callbackObject);

        return callbackMap;
    }

    private void setPathItemOperation(PathItem pathItemObject, String method, Operation operation) {
        switch (method) {
            case POST_METHOD:
                pathItemObject.POST(operation);
                break;
            case GET_METHOD:
                pathItemObject.GET(operation);
                break;
            case DELETE_METHOD:
                pathItemObject.DELETE(operation);
                break;
            case PUT_METHOD:
                pathItemObject.PUT(operation);
                break;
            case PATCH_METHOD:
                pathItemObject.PATCH(operation);
                break;
            case TRACE_METHOD:
                pathItemObject.TRACE(operation);
                break;
            case HEAD_METHOD:
                pathItemObject.HEAD(operation);
                break;
            case OPTIONS_METHOD:
                pathItemObject.OPTIONS(operation);
                break;
            default:
                // Do nothing here
                break;
        }
    }

    private void setOperationObjectFromApiOperationAnnotation(
                                                              Operation operation,
                                                              org.eclipse.microprofile.openapi.annotations.Operation apiOperation,
                                                              Produces methodProduces,
                                                              Produces classProduces,
                                                              Consumes methodConsumes,
                                                              Consumes classConsumes) {
        if (StringUtils.isNotBlank(apiOperation.summary())) {
            operation.setSummary(apiOperation.summary());
        }
        if (StringUtils.isNotBlank(apiOperation.description())) {
            operation.setDescription(apiOperation.description());
        }
        if (StringUtils.isNotBlank(apiOperation.operationId())) {
            operation.setOperationId(getOperationId(apiOperation.operationId()));
        }
        if (apiOperation.deprecated()) {
            operation.setDeprecated(apiOperation.deprecated());
        }
    }

    private void setOperationObjectFromApiOperationAnnotation(Operation operation, org.eclipse.microprofile.openapi.annotations.callbacks.CallbackOperation callbackOp) {
        if (StringUtils.isNotBlank(callbackOp.summary())) {
            operation.setSummary(callbackOp.summary());
        }
        if (StringUtils.isNotBlank(callbackOp.description())) {
            operation.setDescription(callbackOp.description());
        }
        AnnotationsUtils.getExternalDocumentation(callbackOp.externalDocs()).ifPresent(operation::setExternalDocs);
        getParametersListFromAnnotation(callbackOp.parameters(), null, null, operation).ifPresent(p -> p.forEach(operation::addParameter));
        SecurityParser.getSecurityRequirements(callbackOp.security()).ifPresent(operation::setSecurity);
        OperationParser.getApiResponses(callbackOp.responses(), null, null, components).ifPresent(operation::setResponses);
        // TODO: Request body has to be processed.
    }

    protected String getOperationId(String operationId) {
        boolean operationIdUsed = existOperationId(operationId);
        String operationIdToFind = null;
        int counter = 0;
        while (operationIdUsed) {
            operationIdToFind = String.format("%s_%d", operationId, ++counter);
            operationIdUsed = existOperationId(operationIdToFind);
        }
        if (operationIdToFind != null) {
            operationId = operationIdToFind;
        }
        return operationId;
    }

    private boolean existOperationId(String operationId) {
        if (openAPI == null) {
            return false;
        }
        if (openAPI.getPaths() == null || openAPI.getPaths().isEmpty()) {
            return false;
        }
        for (PathItem path : openAPI.getPaths().values()) {
            String pathOperationId = extractOperationIdFromPathItem(path);
            if (operationId.equalsIgnoreCase(pathOperationId)) {
                return true;
            }

        }
        return false;
    }

    protected Optional<List<Parameter>> getParametersListFromAnnotation(org.eclipse.microprofile.openapi.annotations.parameters.Parameter[] parameters, Consumes classConsumes,
                                                                        Consumes methodConsumes,
                                                                        Operation operation) {
        if (parameters == null) {
            return Optional.empty();
        }
        List<Parameter> parametersObject = new ArrayList<>();
        for (org.eclipse.microprofile.openapi.annotations.parameters.Parameter parameter : parameters) {

            ResolvedParameter resolvedParameter = getParameters(ParameterProcessor.getParameterType(parameter), Collections.singletonList(parameter), operation, classConsumes,
                                                                methodConsumes);
            parametersObject.addAll(resolvedParameter.parameters);
        }
        if (parametersObject.size() == 0) {
            return Optional.empty();
        }
        return Optional.of(parametersObject);
    }

    protected ResolvedParameter getParameters(Type type, List<Annotation> annotations, Operation operation, javax.ws.rs.Consumes classConsumes,
                                              javax.ws.rs.Consumes methodConsumes) {
        final Iterator<OpenAPIExtension> chain = OpenAPIExtensions.chain();
        if (!chain.hasNext()) {
            return new ResolvedParameter();
        }
        //LOGGER.debug("getParameters for {}", type);
        Set<Type> typesToSkip = new HashSet<>();
        final OpenAPIExtension extension = chain.next();
        //LOGGER.debug("trying extension {}", extension);

        final ResolvedParameter extractParametersResult = extension.extractParameters(annotations, type, typesToSkip, components, classConsumes, methodConsumes, true, chain);
        return extractParametersResult;
    }

    private String extractOperationIdFromPathItem(PathItem path) {
        if (path.getGET() != null) {
            return path.getGET().getOperationId();
        } else if (path.getPOST() != null) {
            return path.getPOST().getOperationId();
        } else if (path.getPUT() != null) {
            return path.getPUT().getOperationId();
        } else if (path.getDELETE() != null) {
            return path.getDELETE().getOperationId();
        } else if (path.getOPTIONS() != null) {
            return path.getOPTIONS().getOperationId();
        } else if (path.getHEAD() != null) {
            return path.getHEAD().getOperationId();
        } else if (path.getPATCH() != null) {
            return path.getPATCH().getOperationId();
        }
        return "";
    }

    private boolean isEmptyComponents(Components components) {
        if (components == null) {
            return true;
        }
        if (components.getSchemas() != null && components.getSchemas().size() > 0) {
            return false;
        }
        if (components.getSecuritySchemes() != null && components.getSecuritySchemes().size() > 0) {
            return false;
        }
        if (components.getCallbacks() != null && components.getCallbacks().size() > 0) {
            return false;
        }
        if (components.getExamples() != null && components.getExamples().size() > 0) {
            return false;
        }
        if (components.getExtensions() != null && components.getExtensions().size() > 0) {
            return false;
        }
        if (components.getHeaders() != null && components.getHeaders().size() > 0) {
            return false;
        }
        if (components.getLinks() != null && components.getLinks().size() > 0) {
            return false;
        }
        if (components.getParameters() != null && components.getParameters().size() > 0) {
            return false;
        }
        if (components.getRequestBodies() != null && components.getRequestBodies().size() > 0) {
            return false;
        }
        if (components.getResponses() != null && components.getResponses().size() > 0) {
            return false;
        }
        return true;
    }

    protected boolean isOperationHidden(Method method) {
        org.eclipse.microprofile.openapi.annotations.Operation apiOperation = ReflectionUtils.getAnnotation(method, org.eclipse.microprofile.openapi.annotations.Operation.class);
        if (apiOperation != null && apiOperation.hidden()) {
            return true;
        }
        return false;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    protected boolean ignoreOperationPath(String path, String parentPath) {

        if (StringUtils.isBlank(path) && StringUtils.isBlank(parentPath)) {
            return true;
        } else if (StringUtils.isNotBlank(path) && StringUtils.isBlank(parentPath)) {
            return false;
        } else if (StringUtils.isBlank(path) && StringUtils.isNotBlank(parentPath)) {
            return false;
        }
        if (parentPath != null && !"".equals(parentPath) && !"/".equals(parentPath)) {
            if (!parentPath.startsWith("/")) {
                parentPath = "/" + parentPath;
            }
            if (parentPath.endsWith("/")) {
                parentPath = parentPath.substring(0, parentPath.length() - 1);
            }
        }
        if (path != null && !"".equals(path) && !"/".equals(path)) {
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
        }
        if (path.equals(parentPath)) {
            return true;
        }
        return false;
    }
}
