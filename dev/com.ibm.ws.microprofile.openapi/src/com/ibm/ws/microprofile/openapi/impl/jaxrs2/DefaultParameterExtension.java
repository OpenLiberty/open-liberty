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

package com.ibm.ws.microprofile.openapi.impl.jaxrs2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.ibm.ws.microprofile.openapi.impl.core.util.Json;
import com.ibm.ws.microprofile.openapi.impl.core.util.ParameterProcessor;
import com.ibm.ws.microprofile.openapi.impl.jaxrs2.ext.AbstractOpenAPIExtension;
import com.ibm.ws.microprofile.openapi.impl.jaxrs2.ext.OpenAPIExtension;
import com.ibm.ws.microprofile.openapi.impl.jaxrs2.ext.OpenAPIExtensions;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.CookieParameterImpl;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.HeaderParameterImpl;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.ParameterImpl;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.PathParameterImpl;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.QueryParameterImpl;

public class DefaultParameterExtension extends AbstractOpenAPIExtension {
    private static String QUERY_PARAM = "query";
    private static String HEADER_PARAM = "header";
    private static String COOKIE_PARAM = "cookie";
    private static String PATH_PARAM = "path";
    private static String FORM_PARAM = "form";

    final ObjectMapper mapper = Json.mapper();

    @Override
    public ResolvedParameter extractParameters(List<Annotation> annotations,
                                               Type type,
                                               Set<Type> typesToSkip,
                                               Components components,
                                               javax.ws.rs.Consumes classConsumes,
                                               javax.ws.rs.Consumes methodConsumes,
                                               boolean includeRequestBody,
                                               Iterator<OpenAPIExtension> chain) {
        if (shouldIgnoreType(type, typesToSkip)) {
            return new ResolvedParameter();
        }

        List<Parameter> parameters = new ArrayList<>();
        Parameter parameter = null;
        ResolvedParameter extractParametersResult = new ResolvedParameter();
        for (Annotation annotation : annotations) {
            if (annotation instanceof QueryParam) {
                QueryParam param = (QueryParam) annotation;
                Parameter qp = new QueryParameterImpl();
                qp.setName(param.value());
                parameter = qp;
            } else if (annotation instanceof PathParam) {
                PathParam param = (PathParam) annotation;
                Parameter pp = new PathParameterImpl();
                pp.setName(param.value());
                parameter = pp;
            } else if (annotation instanceof HeaderParam) {
                HeaderParam param = (HeaderParam) annotation;
                Parameter pp = new HeaderParameterImpl();
                pp.setName(param.value());
                parameter = pp;
            } else if (annotation instanceof CookieParam) {
                CookieParam param = (CookieParam) annotation;
                Parameter pp = new CookieParameterImpl();
                pp.setName(param.value());
                parameter = pp;
            } else if (annotation instanceof org.eclipse.microprofile.openapi.annotations.parameters.Parameter) {
                if (((org.eclipse.microprofile.openapi.annotations.parameters.Parameter) annotation).hidden()) {
                    extractParametersResult.parameters = parameters;
                    return extractParametersResult;
                }
                if (parameter == null) {
                    Parameter pp = new ParameterImpl();
                    parameter = pp;
                }
            } else {
                if (handleAdditionalAnnotation(parameters, annotation, type, typesToSkip, classConsumes, methodConsumes, components, includeRequestBody)) {
                    extractParametersResult.parameters.addAll(parameters);
                    return extractParametersResult;
                }
            }
        }

        if (parameter != null && parameter.getIn() != null) {
            parameters.add(parameter);
        } else if (includeRequestBody) {
            Parameter unknownParameter = ParameterProcessor.applyAnnotations(
                                                                             null,
                                                                             type,
                                                                             annotations,
                                                                             components,
                                                                             classConsumes == null ? new String[0] : classConsumes.value(),
                                                                             methodConsumes == null ? new String[0] : methodConsumes.value());
            if (unknownParameter != null) {
                if (unknownParameter.getIn() != null) {
                    extractParametersResult.parameters.add(unknownParameter);
                } else { // return as request body
                    extractParametersResult.requestBody = unknownParameter;
                }
            }
        }
        for (Parameter p : parameters) {
            if (ParameterProcessor.applyAnnotations(
                                                    p,
                                                    type,
                                                    annotations,
                                                    components,
                                                    classConsumes == null ? new String[0] : classConsumes.value(),
                                                    methodConsumes == null ? new String[0] : methodConsumes.value()) != null) {
                extractParametersResult.parameters.add(p);
            }
        }
        return extractParametersResult;
    }

    /**
     * Adds additional annotation processing support
     *
     * @param parameters
     * @param annotation
     * @param type
     * @param typesToSkip
     */

    private boolean handleAdditionalAnnotation(List<Parameter> parameters, Annotation annotation,
                                               final Type type, Set<Type> typesToSkip, javax.ws.rs.Consumes classConsumes,
                                               javax.ws.rs.Consumes methodConsumes, Components components, boolean includeRequestBody) {
        boolean processed = false;
        if (BeanParam.class.isAssignableFrom(annotation.getClass())) {
            // Use Jackson's logic for processing Beans
            final BeanDescription beanDesc = mapper.getSerializationConfig().introspect(constructType(type));
            final List<BeanPropertyDefinition> properties = beanDesc.findProperties();

            for (final BeanPropertyDefinition propDef : properties) {
                final AnnotatedField field = propDef.getField();
                final AnnotatedMethod setter = propDef.getSetter();
                final AnnotatedMethod getter = propDef.getGetter();
                final List<Annotation> paramAnnotations = new ArrayList<Annotation>();
                final Iterator<OpenAPIExtension> extensions = OpenAPIExtensions.chain();
                Type paramType = null;

                // Gather the field's details
                if (field != null) {
                    paramType = field.getType();

                    for (final Annotation fieldAnnotation : field.annotations()) {
                        if (!paramAnnotations.contains(fieldAnnotation)) {
                            paramAnnotations.add(fieldAnnotation);
                        }
                    }
                }

                // Gather the setter's details but only the ones we need
                if (setter != null) {
                    // Do not set the param class/type from the setter if the values are already identified
                    if (paramType == null) {
                        // paramType will stay null if there is no parameter
                        paramType = setter.getParameterType(0);
                    }

                    for (final Annotation fieldAnnotation : setter.annotations()) {
                        if (!paramAnnotations.contains(fieldAnnotation)) {
                            paramAnnotations.add(fieldAnnotation);
                        }
                    }
                }

                // Gather the getter's details but only the ones we need
                if (getter != null) {
                    // Do not set the param class/type from the getter if the values are already identified
                    if (paramType == null) {
                        paramType = getter.getType();
                    }

                    for (final Annotation fieldAnnotation : getter.annotations()) {
                        if (!paramAnnotations.contains(fieldAnnotation)) {
                            paramAnnotations.add(fieldAnnotation);
                        }
                    }
                }

                if (paramType == null) {
                    continue;
                }

                // Re-process all Bean fields and let the default swagger-jaxrs/swagger-jersey-jaxrs processors do their thing
                List<Parameter> extracted = extensions.next().extractParameters(
                                                                                paramAnnotations,
                                                                                paramType,
                                                                                typesToSkip,
                                                                                components,
                                                                                classConsumes,
                                                                                methodConsumes,
                                                                                includeRequestBody,
                                                                                extensions).parameters;

                for (Parameter p : extracted) {
                    if (ParameterProcessor.applyAnnotations(
                                                            p,
                                                            paramType,
                                                            paramAnnotations,
                                                            components,
                                                            classConsumes == null ? new String[0] : classConsumes.value(),
                                                            methodConsumes == null ? new String[0] : methodConsumes.value()) != null) {
                        parameters.add(p);
                    }
                }

                processed = true;
            }
        }
        return processed;
    }

    @Override
    protected boolean shouldIgnoreClass(Class<?> cls) {
        return cls.getName().startsWith("javax.ws.rs.");
    }

}
