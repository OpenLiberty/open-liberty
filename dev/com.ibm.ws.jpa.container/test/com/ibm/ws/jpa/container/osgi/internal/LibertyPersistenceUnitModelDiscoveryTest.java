/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.jpa.container.osgi.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.container.service.annocache.ContainerAnnotations;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.annocache.info.ClassInfo;
import com.ibm.wsspi.annocache.info.InfoStore;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets;

/**
 * Unit tests for persistence-unit-scoped model discovery.
 *
 */
@RunWith(JMock.class)
public class LibertyPersistenceUnitModelDiscoveryTest {
    private static final String DISCOVERABLE = "jakarta.persistence.spi.Discoverable";
    private static final String REPOSITORY = "jakarta.data.repository.Repository";

    private final Mockery mockery = new Mockery();

    @Test
    public void normalizesEntryPrefixForContainerAnnotationScanning() {
        Container root = mockery.mock(Container.class, "prefixRoot");

        PersistenceUnitModelDiscovery.Source source =
                        new PersistenceUnitModelDiscovery.Source(root, "/WEB-INF/classes", "root.war!/WEB-INF/classes");

        assertEquals("WEB-INF/classes/", source.getEntryPrefix());
    }

    @Test
    public void mergesExplicitAndXmlClassesInSpecifiedOrder() throws Exception {
        final Container root = mockery.mock(Container.class, "xmlRoot");
        final Entry defaultMapping = mockery.mock(Entry.class, "defaultMapping");
        final String defaultXml = "<entity-mappings package=\"example.defaultmodel\">"
                                  + "<entity class=\"DefaultEntity\"/>"
                                  + "<embeddable class=\"example.Shared\"/>"
                                  + "</entity-mappings>";
        final String additionalXml = "<entity-mappings>"
                                     + "<mapped-superclass class=\"example.Base\"/>"
                                     + "<entity class=\"example.Shared\"/>"
                                     + "<converter class=\"example.Converter\"/>"
                                     + "</entity-mappings>";

        mockery.checking(new Expectations() {
            {
                oneOf(root).getEntry("WEB-INF/classes/META-INF/orm.xml");
                will(returnValue(defaultMapping));
                oneOf(defaultMapping).adapt(InputStream.class);
                will(returnValue(stream(defaultXml)));
            }
        });

        PersistenceUnitModelDiscovery.Context context = context(
                        new PersistenceUnitModelDiscovery.Source(root, "/WEB-INF/classes/", "root.war!/WEB-INF/classes"),
                        false,
                        asList("example.Explicit", "example.Shared"),
                        singletonList("mappings/additional.xml"),
                        new PersistenceUnitModelDiscovery.MappingResourceAccess() {
                            @Override
                            public InputStream open(String resourceName) {
                                return "mappings/additional.xml".equals(resourceName) ? stream(additionalXml) : null;
                            }
                        });

        assertEquals(asList("example.Explicit",
                            "example.Shared",
                            "example.defaultmodel.DefaultEntity",
                            "example.Base",
                            "example.Converter"),
                     new LibertyPersistenceUnitModelDiscovery().discover(context).getAllClassNames());
    }

    @Test
    public void discoversCustomAnnotationRepositoryAndPackageInLexicalOrder() throws Exception {
        final String customAnnotation = TestModelAnnotation.class.getName();
        final Container root = mockery.mock(Container.class, "annotationRoot");
        final Entry moduleInfo = mockery.mock(Entry.class, "moduleInfo");
        final ContainerAnnotations annotations = mockery.mock(ContainerAnnotations.class);
        final AnnotationTargets_Targets targets = mockery.mock(AnnotationTargets_Targets.class);
        final InfoStore infoStore = mockery.mock(InfoStore.class);
        final ClassInfo annotationInfo = mockery.mock(ClassInfo.class);
        final Set<String> classAnnotations = new LinkedHashSet<String>(asList(customAnnotation, REPOSITORY));

        mockery.checking(new Expectations() {
            {
                oneOf(root).getEntry("META-INF/orm.xml");
                will(returnValue(null));
                oneOf(root).adapt(ContainerAnnotations.class);
                will(returnValue(annotations));
                oneOf(annotations).setAppName("test-app");
                oneOf(annotations).setModName("test-module");
                oneOf(annotations).setIsUnnamedMod(false);
                oneOf(annotations).setIsLightweight(true);
                oneOf(annotations).setEntryPrefix(null);
                oneOf(annotations).setUseJandex(false);
                oneOf(annotations).setClassLoader(with(any(ClassLoader.class)));
                oneOf(annotations).getTargets();
                will(returnValue(targets));
                oneOf(annotations).getInfoStore();
                will(returnValue(infoStore));

                oneOf(targets).getClassAnnotations(AnnotationTargets_Targets.POLICY_SEED);
                will(returnValue(classAnnotations));
                oneOf(targets).getPackageAnnotations(AnnotationTargets_Targets.POLICY_SEED);
                will(returnValue(singleton(customAnnotation)));

                oneOf(root).getEntry("module-info.class");
                will(returnValue(moduleInfo));
                oneOf(moduleInfo).adapt(InputStream.class);
                will(returnValue(LibertyPersistenceUnitModelDiscoveryTest.class
                                .getResourceAsStream("LibertyPersistenceUnitModelDiscoveryTest$AnnotatedDescriptor.class")));

                oneOf(annotations).openInfoStore();
                oneOf(infoStore).getDelayableClassInfo(customAnnotation);
                will(returnValue(annotationInfo));
                oneOf(annotationInfo).isArtificial();
                will(returnValue(false));
                oneOf(annotationInfo).isDeclaredAnnotationPresent(DISCOVERABLE);
                will(returnValue(true));
                oneOf(targets).getAnnotatedClasses(customAnnotation, AnnotationTargets_Targets.POLICY_SEED);
                will(returnValue(new LinkedHashSet<String>(asList("example.Zebra", "example.Alpha"))));
                oneOf(targets).getAnnotatedPackages(customAnnotation, AnnotationTargets_Targets.POLICY_SEED);
                will(returnValue(singleton("example.model")));
                oneOf(targets).getAnnotatedClasses(REPOSITORY, AnnotationTargets_Targets.POLICY_SEED);
                will(returnValue(singleton("example.RepositoryType")));
                oneOf(targets).getAnnotatedPackages(REPOSITORY, AnnotationTargets_Targets.POLICY_SEED);
                will(returnValue(emptySet()));
                oneOf(annotations).closeInfoStore();
            }
        });

        PersistenceUnitModelDiscovery.Context context = context(
                        new PersistenceUnitModelDiscovery.Source(root, null, "root.jar"),
                        true,
                        emptyList(),
                        emptyList(),
                        noMappings());

        assertEquals(asList("example.Alpha",
                            "example.RepositoryType",
                            "example.Zebra",
                            "example.model.package-info",
                            "module-info"),
                     new LibertyPersistenceUnitModelDiscovery().discover(context).getAllClassNames());
    }

    @Test
    public void missingExplicitMappingFailsWithPersistenceUnitIdentity() {
        final Container root = mockery.mock(Container.class, "missingMappingRoot");
        mockery.checking(new Expectations() {
            {
                oneOf(root).getEntry("META-INF/orm.xml");
                will(returnValue(null));
            }
        });

        PersistenceUnitModelDiscovery.Context context = context(
                        new PersistenceUnitModelDiscovery.Source(root, null, "root.jar"),
                        false,
                        emptyList(),
                        singletonList("missing.xml"),
                        noMappings());

        PersistenceUnitModelDiscovery.DiscoveryException failure;
        try {
            new LibertyPersistenceUnitModelDiscovery().discover(context);
            throw new AssertionError("Expected the missing mapping resource to fail discovery");
        } catch (PersistenceUnitModelDiscovery.DiscoveryException expected) {
            failure = expected;
        }
        assertTrue(failure.getMessage().contains("application=test-app"));
        assertTrue(failure.getMessage().contains("persistence-unit=test-pu"));
        assertTrue(failure.getMessage().contains("resource=missing.xml"));
    }

    @Test
    public void unresolvedAnnotationMetadataFailsWithPersistenceUnitIdentity() throws Exception {
        final String unresolvedAnnotation = "example.UnresolvedAnnotation";
        final Container root = mockery.mock(Container.class, "unresolvedAnnotationRoot");
        final ContainerAnnotations annotations = mockery.mock(ContainerAnnotations.class, "unresolvedAnnotations");
        final AnnotationTargets_Targets targets = mockery.mock(AnnotationTargets_Targets.class, "unresolvedTargets");
        final InfoStore infoStore = mockery.mock(InfoStore.class, "unresolvedInfoStore");

        mockery.checking(new Expectations() {
            {
                oneOf(root).getEntry("META-INF/orm.xml");
                will(returnValue(null));
                oneOf(root).adapt(ContainerAnnotations.class);
                will(returnValue(annotations));
                oneOf(annotations).setAppName("test-app");
                oneOf(annotations).setModName("test-module");
                oneOf(annotations).setIsUnnamedMod(false);
                oneOf(annotations).setIsLightweight(true);
                oneOf(annotations).setEntryPrefix(null);
                oneOf(annotations).setUseJandex(false);
                oneOf(annotations).setClassLoader(with(any(ClassLoader.class)));
                oneOf(annotations).getTargets();
                will(returnValue(targets));
                oneOf(annotations).getInfoStore();
                will(returnValue(infoStore));
                oneOf(targets).getClassAnnotations(AnnotationTargets_Targets.POLICY_SEED);
                will(returnValue(singleton(unresolvedAnnotation)));
                oneOf(targets).getPackageAnnotations(AnnotationTargets_Targets.POLICY_SEED);
                will(returnValue(emptySet()));
                oneOf(root).getEntry("module-info.class");
                will(returnValue(null));
                oneOf(annotations).openInfoStore();
                oneOf(infoStore).getDelayableClassInfo(unresolvedAnnotation);
                will(returnValue(null));
                oneOf(annotations).closeInfoStore();
            }
        });

        PersistenceUnitModelDiscovery.Context context = context(
                        new PersistenceUnitModelDiscovery.Source(root, null, "unresolved.jar"),
                        true,
                        emptyList(),
                        emptyList(),
                        noMappings());

        PersistenceUnitModelDiscovery.DiscoveryException failure;
        try {
            new LibertyPersistenceUnitModelDiscovery().discover(context);
            throw new AssertionError("Expected unresolved annotation metadata to fail discovery");
        } catch (PersistenceUnitModelDiscovery.DiscoveryException expected) {
            failure = expected;
        }
        assertTrue(failure.getMessage().contains("application=test-app"));
        assertTrue(failure.getMessage().contains("persistence-unit=test-pu"));
        assertTrue(failure.getMessage().contains(unresolvedAnnotation));
        assertTrue(failure.getMessage().contains("resource=unresolved.jar"));
    }

    @Test
    public void rootExclusionStillScansReferencedArchivesInDescriptorOrder() throws Exception {
        final Container root = mockery.mock(Container.class, "excludedRoot");
        mockery.checking(new Expectations() {
            {
                oneOf(root).getEntry("META-INF/orm.xml");
                will(returnValue(null));
            }
        });

        PersistenceUnitModelDiscovery.Source first = annotatedSource(
                        "firstJar",
                        new LinkedHashSet<String>(asList("example.Beta", "example.Alpha")));
        PersistenceUnitModelDiscovery.Source second = annotatedSource(
                        "secondJar",
                        new LinkedHashSet<String>(asList("example.Alpha", "example.Gamma")));

        PersistenceUnitModelDiscovery.Context context = new PersistenceUnitModelDiscovery.Context(
                        "test-app",
                        "test-module",
                        "test-pu",
                        new PersistenceUnitModelDiscovery.Source(root, null, "root.jar"),
                        false,
                        asList(first, second),
                        singletonList("example.Explicit"),
                        emptyList(),
                        noMappings(),
                        new NoClassLoadingClassLoader(),
                        false);

        assertEquals(asList("example.Explicit", "example.Alpha", "example.Beta", "example.Gamma"),
                     new LibertyPersistenceUnitModelDiscovery().discover(context).getAllClassNames());
    }

    private PersistenceUnitModelDiscovery.Context context(PersistenceUnitModelDiscovery.Source root,
                                                           boolean discoverRoot,
                                                           java.util.List<String> explicitClasses,
                                                           java.util.List<String> mappingFiles,
                                                           PersistenceUnitModelDiscovery.MappingResourceAccess mappings) {
        return new PersistenceUnitModelDiscovery.Context("test-app",
                                                         "test-module",
                                                         "test-pu",
                                                         root,
                                                         discoverRoot,
                                                         emptyList(),
                                                         explicitClasses,
                                                         mappingFiles,
                                                         mappings,
                                                         new NoClassLoadingClassLoader(),
                                                         false);
    }

    private PersistenceUnitModelDiscovery.Source annotatedSource(String label, final Set<String> classNames) throws Exception {
        final String annotationName = TestModelAnnotation.class.getName();
        final Container container = mockery.mock(Container.class, label + "Container");
        final ContainerAnnotations annotations = mockery.mock(ContainerAnnotations.class, label + "Annotations");
        final AnnotationTargets_Targets targets = mockery.mock(AnnotationTargets_Targets.class, label + "Targets");
        final InfoStore infoStore = mockery.mock(InfoStore.class, label + "InfoStore");
        final ClassInfo annotationInfo = mockery.mock(ClassInfo.class, label + "AnnotationInfo");

        mockery.checking(new Expectations() {
            {
                oneOf(container).adapt(ContainerAnnotations.class);
                will(returnValue(annotations));
                oneOf(annotations).setAppName("test-app");
                oneOf(annotations).setModName("test-module");
                oneOf(annotations).setIsUnnamedMod(false);
                oneOf(annotations).setIsLightweight(true);
                oneOf(annotations).setEntryPrefix(null);
                oneOf(annotations).setUseJandex(false);
                oneOf(annotations).setClassLoader(with(any(ClassLoader.class)));
                oneOf(annotations).getTargets();
                will(returnValue(targets));
                oneOf(annotations).getInfoStore();
                will(returnValue(infoStore));
                oneOf(targets).getClassAnnotations(AnnotationTargets_Targets.POLICY_SEED);
                will(returnValue(singleton(annotationName)));
                oneOf(targets).getPackageAnnotations(AnnotationTargets_Targets.POLICY_SEED);
                will(returnValue(emptySet()));
                oneOf(container).getEntry("module-info.class");
                will(returnValue(null));
                oneOf(annotations).openInfoStore();
                oneOf(infoStore).getDelayableClassInfo(annotationName);
                will(returnValue(annotationInfo));
                oneOf(annotationInfo).isArtificial();
                will(returnValue(false));
                oneOf(annotationInfo).isDeclaredAnnotationPresent(DISCOVERABLE);
                will(returnValue(true));
                oneOf(targets).getAnnotatedClasses(annotationName, AnnotationTargets_Targets.POLICY_SEED);
                will(returnValue(classNames));
                oneOf(targets).getAnnotatedPackages(annotationName, AnnotationTargets_Targets.POLICY_SEED);
                will(returnValue(emptySet()));
                oneOf(annotations).closeInfoStore();
            }
        });

        return new PersistenceUnitModelDiscovery.Source(container, null, label + ".jar");
    }

    private static PersistenceUnitModelDiscovery.MappingResourceAccess noMappings() {
        return new PersistenceUnitModelDiscovery.MappingResourceAccess() {
            @Override
            public InputStream open(String resourceName) {
                return null;
            }
        };
    }

    private static InputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(UTF_8));
    }

    private static <T> java.util.List<T> singletonList(T value) {
        return Collections.singletonList(value);
    }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    private @interface TestModelAnnotation {
    }

    @TestModelAnnotation
    private static final class AnnotatedDescriptor {
    }

    private static final class NoClassLoadingClassLoader extends ClassLoader {
        NoClassLoadingClassLoader() {
            super(null);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            throw new AssertionError("Discovery attempted to load " + name);
        }
    }
}
