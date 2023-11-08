/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.inmemory.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.data.repository.Repository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanBuilder;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.build.compatible.spi.Types;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.AnnotationMember;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.types.Type;

/**
 * A fake Jakarta Data provider extension that only produces a single repository class,
 * which is because it doesn't have a real implementation and is only for tests
 * that register a Jakarta Data provider as a CDI extension.
 */
public class CompositeBuildCompatibleExtension implements BuildCompatibleExtension {

    // map of data store to list of repository class name
    private final Map<String, List<String>> repositoryClassNames = new HashMap<>();

    /**
     * Identify classes that are annotated with Repository
     * and determine which apply to this provider.
     */
    @Enhancement(withAnnotations = Repository.class, types = Object.class, withSubtypes = true)
    public void enhancement(ClassInfo repositoryClassInfo) {

        AnnotationInfo repositoryAnnotationInfo = repositoryClassInfo.annotation(Repository.class);

        // First, check for explicit configuration to use this provider:
        @SuppressWarnings({ "deprecation", "removal" }) // Work around bug where WELD lacks doPrivileged.
        AnnotationMember providerMember = java.security.AccessController.doPrivileged((java.security.PrivilegedAction<AnnotationMember>) () -> //
        repositoryAnnotationInfo.member("provider"));

        String provider = providerMember.asString();
        boolean provideRepository = "Composites Mock Data Provider".equals(provider);

        // Otherwise, if the provider is not explicitly specified,
        // then look for an entity annotation that this provider handles:
        if (!provideRepository && "".equals(provider))
            // The entity class is on one of the super interfaces: DataRepository<MyEntityClass, MyIdClass>
            for (Type supertype : repositoryClassInfo.superInterfaces())
                if (supertype.isParameterizedType()) {
                    List<Type> typeVarTypes = supertype.asParameterizedType().typeArguments();
                    if (!typeVarTypes.isEmpty()) {
                        // The entity type is the first type parameter (and the id type is second).
                        Type entityType = typeVarTypes.get(0);
                        if (entityType.isClass()) {
                            ClassInfo entityTypeInfo = entityType.asClass().declaration();
                            if (entityTypeInfo.hasAnnotation(anno -> CompositeEntity.class.getName().equals(anno.name()))) {
                                provideRepository = true;
                                break;
                            }
                        }
                    }
                }

        if (provideRepository) {
            // Identify which data store to use.
            // This mock provider is in-memory and doesn't care, so we just print it.
            @SuppressWarnings({ "deprecation", "removal" }) // Work around bug where WELD lacks doPrivileged.
            AnnotationMember dataStoreMember = java.security.AccessController.doPrivileged((java.security.PrivilegedAction<AnnotationMember>) () -> //
            repositoryAnnotationInfo.member("dataStore"));

            String dataStore = dataStoreMember.asString();
            System.out.println("During enhancement, found " + repositoryClassInfo + " with dataStore of " + dataStore + ".");

            List<String> list = repositoryClassNames.get(dataStore);
            if (list == null)
                repositoryClassNames.put(dataStore, list = new ArrayList<>());
            list.add(repositoryClassInfo.name());
        }
    }

    /**
     * Register beans for repositories.
     */
    @Synthesis
    public void synthesis(Types types, SyntheticComponents synth) throws ClassNotFoundException {
        for (String dataStore : repositoryClassNames.keySet())
            for (String repoClassName : repositoryClassNames.get(dataStore)) {
                @SuppressWarnings("unchecked")
                Class<Object> repoClass = (Class<Object>) Class.forName(repoClassName);
                @SuppressWarnings({ "deprecation", "removal" }) // Work around bug where WELD lacks doPrivileged.
                SyntheticBeanBuilder<Object> builder = java.security.AccessController.doPrivileged((java.security.PrivilegedAction<SyntheticBeanBuilder<Object>>) () -> //
                synth
                                .addBean(repoClass)
                                .name(repoClassName)
                                .type(types.ofClass(repoClassName))
                                .scope(ApplicationScoped.class)
                                .withParam("dataStore", dataStore)
                                .createWith(CompositeBeanCreator.class));
                System.out.println("Registered " + repoClassName + " bean with " + builder);
            }
    }
}
