/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.services.impl;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callback;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callbacks;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.AnnotationValue.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.internal.OpenAPIVersion;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIVersionUsageChecker;
import io.openliberty.microprofile.openapi20.internal.utils.MessageConstants;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class OpenAPI31VersionUsageChecker implements OpenAPIVersionUsageChecker {

    private static final TraceComponent tc = Tr.register(OpenAPI31VersionUsageChecker.class);

    private static final OpenAPIVersion VERSION_31 = new OpenAPIVersion(3, 1);

    /**
     * The list of OpenAPI annotations we need to search under to find usages of new parameters.
     * <p>
     * This list has the OpenAPI annotation types which:
     * <ul>
     * <li>Can be placed on Java elements (e.g. classes, methods, parameters, etc)
     * <li>Either have new parameters or can have annotations with new parameters nested within them
     * </ul>
     */
    public static final List<DotName> OUTER_ANNOTATIONS;

    static {
        OUTER_ANNOTATIONS = Stream.of(APIResponse.class, APIResponses.class, Callback.class, Callbacks.class,
                                      OpenAPIDefinition.class, Parameter.class, Parameters.class, RequestBody.class,
                                      Schema.class, SchemaProperty.class)
                                  .map(DotName::createSimple)
                                  .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Reference list of the new parameters added to each annotation which are only applicable to OpenAPI 3.1
     * <p>
     * If any of these parameters are used when an OpenAPI version less than 3.1 is configured, we need to emit a warning.
     * <p>
     * Note: {@code Schema.constValue} and {@code Schema.examples} are new for OpenAPI 3.1 but we can translate them into something valid OpenAPI 3.0, so they're not included
     * here.
     * <p>
     * Note: {@code SchemaProperty.additionalProperties} was added in MP OpenAPI 4.0, but still applies to OpenAPI 3.0.
     */
    public static enum New31Parameter {
        INFO(Info.class, "summary"),
        COMPONENTS(Components.class, "pathItems"),
        OPENAPI(OpenAPIDefinition.class, "webhooks"),
        LICENSE(License.class, "identifier"),
        SCHEMA(Schema.class,
               "comment",
               "ifSchema", "thenSchema", "elseSchema", "dependentSchemas",
               "contains", "maxContains", "minContains", "prefixItems",
               "patternProperties", "dependentRequired", "propertyNames",
               "contentEncoding", "contentMediaType", "contentSchema"), // New but handled for 3.0: constValue, examples
        SCHEMA_PROPERTY(SchemaProperty.class,
                        "comment",
                        "ifSchema", "thenSchema", "elseSchema", "dependentSchemas",
                        "contains", "maxContains", "minContains", "prefixItems",
                        "patternProperties", "dependentRequired", "propertyNames",
                        "contentEncoding", "contentMediaType", "contentSchema"), // New but handled for 3.0: constValue, examples
                        ;

        private static Map<DotName, New31Parameter> byName = new HashMap<>();
        static {
            for (New31Parameter parameter : New31Parameter.values()) {
                byName.put(parameter.getName(), parameter);
            }
        }

        private final DotName name;
        private final List<String> parameters;

        private New31Parameter(Class<? extends Annotation> clazz, String... parameters) {
            name = DotName.createSimple(clazz);
            this.parameters = Collections.unmodifiableList(Arrays.asList(parameters));
        }

        public List<String> getParameters() {
            return parameters;
        }

        public DotName getName() {
            return name;
        }

        public static New31Parameter forName(DotName name) {
            return byName.get(name);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void checkAnnotations(IndexView index, OpenAPIVersion openAPIVersion) {

        if (openAPIVersion.compareTo(VERSION_31) >= 0) {
            // Nothing to check, all annotation parameters are valid
            return;
        }

        // Otherwise, version is < 3.1, check for use of parameters added in 3.1
        // Note: index.getAnnotations only returns annotations found on Java elements. It doesn't return annotations nested within other annotations.
        // We need to search for the outer annotations, and then recursively look through its nested annotations to find all usages of new parameters
        for (DotName outerAnnoName : OUTER_ANNOTATIONS) {
            for (AnnotationInstance outerAnnoInstance : index.getAnnotations(outerAnnoName)) {
                forEachNewParameterUsage(outerAnnoInstance, (annoInstance, parameterName) -> {
                    warnAbout(outerAnnoInstance.target(), annoInstance, parameterName, openAPIVersion);
                });
            }
        }
    }

    /**
     * Executes an action for each usage of a new parameter found nested under an annotation.
     * <p>
     * First, if {@code anno} has new parameters, {@code action} is called for each of those parameters which has a value assigned to it.
     * <p>
     * Secondly, recursively call this method for each annotation nested under {@code anno}
     *
     * @param anno the annotation to search under
     * @param action the action to run, accepts the annotation instance and the name of the annotation parameter
     */
    private void forEachNewParameterUsage(AnnotationInstance anno, BiConsumer<AnnotationInstance, String> action) {
        New31Parameter paramMetadata = New31Parameter.forName(anno.name());
        if (paramMetadata != null) {
            for (String paramName : paramMetadata.getParameters()) {
                if (anno.value(paramName) != null) {
                    action.accept(anno, paramName);
                }
            }
        }

        for (AnnotationValue value : anno.values()) {
            if (value.kind() == Kind.NESTED) {
                forEachNewParameterUsage(value.asNested(), action);
            } else if (value.kind() == Kind.ARRAY && value.componentKind() == Kind.NESTED) {
                for (AnnotationInstance instance : value.asNestedArray()) {
                    forEachNewParameterUsage(instance, action);
                }
            }
        }
    }

    /**
     * Emits a warning about an annotation parameter being used that isn't applicable to the configured OpenAPI version
     *
     * @param target the Java element that the outer annotation is applied to
     * @param annotation the annotation with the non-applicable parameter. May be applied to {@code target}, or may be nested within another annotation which is applied to
     *     {@code target}
     * @param parameterName the name of the non-applicable parameter
     * @param currentVersion the configured OpenAPI version
     */
    private void warnAbout(AnnotationTarget target, AnnotationInstance annotation, String parameterName, OpenAPIVersion currentVersion) {
        // CWWKO1687W: The {0} annotation element in the {1} class is ignored because it requires OpenAPI version {2} but the current OpenAPI version is {3}.
        Tr.warning(tc, MessageConstants.OPENAPI_ANNOTATION_TOO_NEW_CWWKO1687W,
                   annotation.name().withoutPackagePrefix() + "." + parameterName,
                   getContainingClassName(target),
                   "3.1",
                   currentVersion);
    }

    private String getContainingClassName(AnnotationTarget target) {
        try {
            return getContainingClass(target).name().toString();
        } catch (IllegalArgumentException e) {
            // Shouldn't happen, so allow an FFDC here
            return "Unknown";
        }
    }

    private ClassInfo getContainingClass(AnnotationTarget target) {
        ClassInfo clazz;
        switch (target.kind()) {
            case CLASS:
                clazz = target.asClass();
                break;
            case FIELD:
                clazz = target.asField().declaringClass();
                break;
            case METHOD:
                clazz = target.asMethod().declaringClass();
                break;
            case METHOD_PARAMETER:
                clazz = target.asMethodParameter().method().declaringClass();
                break;
            case RECORD_COMPONENT:
                clazz = target.asRecordComponent().declaringClass();
                break;
            case TYPE:
                clazz = getContainingClass(target.asType().enclosingTarget());
                break;
            default:
                // Shouldn't happen, all enum cases are covered
                throw new IllegalArgumentException("Annotation target kind is unknown: " + target);
        }
        return clazz;
    }

}
