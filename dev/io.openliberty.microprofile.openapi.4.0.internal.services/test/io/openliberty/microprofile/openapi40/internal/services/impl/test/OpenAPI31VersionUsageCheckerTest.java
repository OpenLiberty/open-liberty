/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.services.impl.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.PathItem;
import org.eclipse.microprofile.openapi.annotations.PathItemOperation;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callback;
import org.eclipse.microprofile.openapi.annotations.callbacks.CallbackOperation;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callbacks;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.extensions.Extensions;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.links.Link;
import org.eclipse.microprofile.openapi.annotations.links.LinkParameter;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.DependentRequired;
import org.eclipse.microprofile.openapi.annotations.media.DependentSchema;
import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.annotations.media.Encoding;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.PatternProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBodySchema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlow;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlows;
import org.eclipse.microprofile.openapi.annotations.security.OAuthScope;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirementsSet;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirementsSets;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.servers.ServerVariable;
import org.eclipse.microprofile.openapi.annotations.servers.Servers;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;
import org.jboss.jandex.DotName;
import org.junit.Test;

import io.openliberty.microprofile.openapi40.internal.services.impl.OpenAPI31VersionUsageChecker;
import io.openliberty.microprofile.openapi40.internal.services.impl.OpenAPI31VersionUsageChecker.New31Parameter;

public class OpenAPI31VersionUsageCheckerTest {

    // List extracted from MP OpenAPI API source with:
    // find api/src/main/java/org/eclipse/microprofile/openapi/annotations/ -iname '*.java' | grep -v '/enums/\|package-info' | xargs -l basename | sort | sed s/.java/.class,/
    private static final List<Class<? extends Annotation>> ALL_ANNOTATIONS = Arrays.asList(APIResponse.class,
                                                                                           APIResponseSchema.class,
                                                                                           APIResponses.class,
                                                                                           Callback.class,
                                                                                           CallbackOperation.class,
                                                                                           Callbacks.class,
                                                                                           Components.class,
                                                                                           Contact.class,
                                                                                           Content.class,
                                                                                           DependentRequired.class,
                                                                                           DependentSchema.class,
                                                                                           DiscriminatorMapping.class,
                                                                                           Encoding.class,
                                                                                           ExampleObject.class,
                                                                                           Extension.class,
                                                                                           Extensions.class,
                                                                                           ExternalDocumentation.class,
                                                                                           Header.class,
                                                                                           Info.class,
                                                                                           License.class,
                                                                                           Link.class,
                                                                                           LinkParameter.class,
                                                                                           OAuthFlow.class,
                                                                                           OAuthFlows.class,
                                                                                           OAuthScope.class,
                                                                                           OpenAPIDefinition.class,
                                                                                           Operation.class,
                                                                                           Parameter.class,
                                                                                           Parameters.class,
                                                                                           PathItem.class,
                                                                                           PathItemOperation.class,
                                                                                           PatternProperty.class,
                                                                                           RequestBody.class,
                                                                                           RequestBodySchema.class,
                                                                                           Schema.class,
                                                                                           SchemaProperty.class,
                                                                                           SecurityRequirement.class,
                                                                                           SecurityRequirements.class,
                                                                                           SecurityRequirementsSet.class,
                                                                                           SecurityRequirementsSets.class,
                                                                                           SecurityScheme.class,
                                                                                           SecuritySchemes.class,
                                                                                           Server.class,
                                                                                           Servers.class,
                                                                                           ServerVariable.class,
                                                                                           Tag.class,
                                                                                           Tags.class);

    @Test
    public void testAllParametersExist() {
        for (New31Parameter np : New31Parameter.values()) {
            Class<?> annoClazz = toClazz(np.getName());
            for (String param : np.getParameters()) {
                assertThat(annoClazz.getMethods(), hasItemInArray(hasProperty("name", equalTo(param))));
            }
        }
    }

    @Test
    public void testOuterAnnotationsList() {
        // Outer Annotations list should include all OpenAPI annotations that have:
        // - @Target non-blank
        // - One of the new parameters nested

        // Set of annotations which had parameters added in 3.1
        Set<Class<?>> newParamClasses = Arrays.stream(New31Parameter.values())
                                              .map(New31Parameter::getName)
                                              .map(OpenAPI31VersionUsageCheckerTest::toClazz)
                                              .collect(Collectors.toUnmodifiableSet());

        // Compute set of annotations which can be placed on Java elements and either are one of newParamClasses, or can have one nested somewhere within them
        List<DotName> expectedTopLevelAnnotations = ALL_ANNOTATIONS.stream()
                                                                   .filter(anno -> hasNonEmptyTarget(anno))
                                                                   .filter(anno -> hasNestedNewParameter(anno, newParamClasses))
                                                                   .map(DotName::createSimple)
                                                                   .collect(Collectors.toList());

        System.out.println("Expected top-level annotation list:");
        System.out.println(expectedTopLevelAnnotations.stream()
                                                      .map(DotName::withoutPackagePrefix)
                                                      .map(name -> name + ".class")
                                                      .collect(Collectors.joining(", ")));

        assertEquals(expectedTopLevelAnnotations, OpenAPI31VersionUsageChecker.OUTER_ANNOTATIONS);
    }

    /**
     * Checks whether an annotation class allows itself to be applied to a Java element.
     *
     * @param anno the annotation class
     * @return {@code true} if {@code anno} has no {@code @Target}, or has {@code @Target} with a non-empty value, otherwise {@code false}
     */
    private static boolean hasNonEmptyTarget(Class<? extends Annotation> anno) {
        Target target = anno.getAnnotation(Target.class);
        return target == null || target.value().length > 0;
    }

    /**
     * Checks whether an annotation class either has new parameters, or can have nested annotations with new parameters,
     *
     * @param clazz the annotation class to test
     * @param newParamClazzes the set of annotation classes with new parameters
     * @return {@code true} if {@code clazz} has new parameters or can have nested annotations with new parameters, otherwise {@code false}
     */
    private static boolean hasNestedNewParameter(Class<? extends Annotation> clazz, Set<Class<?>> newParamClazzes) {
        System.out.println("Checking " + clazz);
        // Is this class one of the new param classes?
        if (newParamClazzes.contains(clazz)) {
            return true;
        }

        // For each annotation parameter...
        for (Method m : clazz.getMethods()) {
            Class<?> returnType = m.getReturnType();

            // Does this parameter nest one of the new param classes?
            if (Annotation.class.isAssignableFrom(returnType)) {
                if (hasNestedNewParameter(returnType.asSubclass(Annotation.class), newParamClazzes)) {
                    return true;
                }
            }

            // Is this parameter an array of something that nests one of the new param classes?
            if (returnType.isArray() && Annotation.class.isAssignableFrom(returnType.getComponentType())) {
                Class<?> componentType = returnType.getComponentType();
                if (hasNestedNewParameter(componentType.asSubclass(Annotation.class), newParamClazzes)) {
                    return true;
                }
            }
        }
        System.out.println("No new nested parameters for " + clazz);
        return false;
    }

    private static Class<?> toClazz(DotName name) {
        try {
            return Class.forName(name.toString());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
