/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package com.ibm.ws.repository.resolver;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;

import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.connections.ProductDefinition;
import com.ibm.ws.repository.resolver.internal.kernel.KernelResolverEsa;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.SampleResource;
import com.ibm.ws.repository.resources.writeable.EsaResourceWritable;
import com.ibm.ws.repository.resources.writeable.SampleResourceWritable;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;

/**
 * Unit tests the basic components of {@link RepositoryResolver}
 */
public class RepositoryResolverTest {

    @Test
    public void testProcessNames() {
        SampleResourceWritable sampleA = WritableResourceFactory.createSample(null, ResourceType.PRODUCTSAMPLE);
        sampleA.setShortName("sampleA");
        sampleA.setRequireFeature(Arrays.asList("com.example.dependencyA", "com.example.dependencyB"));

        SampleResourceWritable sampleB = WritableResourceFactory.createSample(null, ResourceType.OPENSOURCE);
        sampleB.setShortName("sampleB");

        EsaResourceWritable featureA = WritableResourceFactory.createEsa(null);
        featureA.setProvideFeature("com.example.featureA");
        featureA.setShortName("featureA");

        EsaResourceWritable featureB = WritableResourceFactory.createEsa(null);
        featureB.setProvideFeature("com.example.featureB");
        featureB.setShortName("featureB");

        RepositoryResolver resolver = testResolver().withSample(sampleA, sampleB)
                                                    .withFeature(featureA, featureB)
                                                    .build();

        resolver.initResolve();
        resolver.processNames(Arrays.asList("sampleA", "featureA", "wibble"));

        assertThat(resolver.samplesToInstall, contains((SampleResource) sampleA));
        assertThat(resolver.requestedFeatureNames, containsInAnyOrder("featureA", "wibble"));
        assertThat(resolver.featureNamesToResolve, containsInAnyOrder("featureA", "wibble", "com.example.dependencyA", "com.example.dependencyB"));

        // Check that sample names are treated case-insensitively
        resolver.initResolve();
        resolver.processNames(Arrays.asList("SAmplEA", "featureA", "wibble"));

        assertThat(resolver.samplesToInstall, contains((SampleResource) sampleA));
        assertThat(resolver.requestedFeatureNames, containsInAnyOrder("featureA", "wibble"));
        assertThat(resolver.featureNamesToResolve, containsInAnyOrder("featureA", "wibble", "com.example.dependencyA", "com.example.dependencyB"));
    }

    @Test
    public void testCreateInstallListWithTolerates() {
        EsaResourceWritable featureA = WritableResourceFactory.createEsa(null);
        featureA.setProvideFeature("com.example.featureA-1.0");
        featureA.addRequireFeatureWithTolerates("com.example.featureB-1.0", Arrays.asList("1.5", "2.0"));
        featureA.addRequireFeatureWithTolerates("com.example.featureD", Collections.<String> emptyList());

        EsaResourceWritable featureB = WritableResourceFactory.createEsa(null);
        featureB.setProvideFeature("com.example.featureB-2.0");
        featureB.addRequireFeatureWithTolerates("com.example.featureC-1.0", Arrays.asList("1.2", "1.5"));

        EsaResourceWritable featureC12 = WritableResourceFactory.createEsa(null);
        featureC12.setProvideFeature("com.example.featureC-1.2");
        featureC12.addRequireFeatureWithTolerates("com.example.featureD", Collections.<String> emptyList());

        EsaResourceWritable featureC15 = WritableResourceFactory.createEsa(null);
        featureC15.setProvideFeature("com.example.featureC-1.5");
        featureC15.addRequireFeatureWithTolerates("com.example.featureD", Collections.<String> emptyList());

        EsaResourceWritable featureD = WritableResourceFactory.createEsa(null);
        featureD.setProvideFeature("com.example.featureD");

        EsaResourceWritable featureE = WritableResourceFactory.createEsa(null);
        featureE.setProvideFeature("com.example.featureE");

        RepositoryResolver resolver = testResolver().withResolvedFeature(featureA, featureB, featureC12, featureC15, featureD, featureE).build();

        List<RepositoryResource> installList = resolver.createInstallList("com.example.featureA-1.0");
        assertThat(installList, contains(featureD, // Note that featureD is first because featureC depends on it, even though featureA also depends directly on it
                                         featureC12, // Note we get both featureC-1.2 and 1.5 as both are tolerated and both are resolved features
                                         featureC15,
                                         featureB, // Note we've picked featureB-2.0 and not worried that featureB-1.0 and 1.5 aren't present
                                         featureA));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateInstallList() {
        EsaResourceWritable featureA = WritableResourceFactory.createEsa(null);
        featureA.setProvideFeature("com.example.featureA");
        featureA.addRequireFeatureWithTolerates("com.example.featureB", Collections.<String> emptyList());
        featureA.addRequireFeatureWithTolerates("com.example.featureD", Collections.<String> emptyList());

        EsaResourceWritable featureB = WritableResourceFactory.createEsa(null);
        featureB.setProvideFeature("com.example.featureB");
        featureB.addRequireFeatureWithTolerates("com.example.featureC", Collections.<String> emptyList());

        EsaResourceWritable featureC = WritableResourceFactory.createEsa(null);
        featureC.setProvideFeature("com.example.featureC");
        featureC.addRequireFeatureWithTolerates("com.example.featureD", Collections.<String> emptyList());

        EsaResourceWritable featureD = WritableResourceFactory.createEsa(null);
        featureD.setProvideFeature("com.example.featureD");

        EsaResourceWritable featureE = WritableResourceFactory.createEsa(null);
        featureE.setProvideFeature("com.example.featureE");

        SampleResourceWritable sampleA = WritableResourceFactory.createSample(null, ResourceType.OPENSOURCE);
        sampleA.setShortName("sampleA");
        sampleA.setRequireFeature(Arrays.asList("com.example.featureC", "com.example.featureE"));

        RepositoryResolver resolver = testResolver().withResolvedFeature(featureA, featureB, featureC, featureD, featureE).build();

        assertThat(resolver.createInstallList(featureA.getProvideFeature()), contains((RepositoryResource) featureD, featureC, featureB, featureA));
        assertThat(resolver.createInstallList(featureC.getProvideFeature()), contains((RepositoryResource) featureD, featureC));
        assertThat(resolver.createInstallList(sampleA), contains(Matchers.<RepositoryResource> is(featureD),
                                                                 anyOf(Matchers.<RepositoryResource> is(featureC), Matchers.<RepositoryResource> is(featureE)),
                                                                 anyOf(Matchers.<RepositoryResource> is(featureC), Matchers.<RepositoryResource> is(featureE)),
                                                                 Matchers.<RepositoryResource> is(sampleA)));
    }

    @Test
    public void testCreateInstallListAutoFeature() {
        EsaResourceWritable featureA = WritableResourceFactory.createEsa(null);
        featureA.setProvideFeature("com.example.featureA");
        featureA.addRequireFeatureWithTolerates("com.example.featureB", Collections.<String> emptyList());

        EsaResourceWritable featureB = WritableResourceFactory.createEsa(null);
        featureB.setProvideFeature("com.example.featureB");

        EsaResourceWritable autoFeature = WritableResourceFactory.createEsa(null);
        autoFeature.setProvideFeature("com.example.autoFeature");
        autoFeature.setProvisionCapability("osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=com.example.featureA))\","
                                           + "osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=com.example.featureB))\"");

        RepositoryResolver resolver = testResolver().withResolvedFeature(featureA, featureB, autoFeature).build();

        assertThat(resolver.createInstallList(autoFeature.getProvideFeature()), contains((RepositoryResource) featureB, featureA, autoFeature));
    }

    /**
     * Test creating an install list where one a feature with a tolerated dependency is installed but the install list needs to include the other tolerated dependency
     */
    @Test
    public void testCreateInstallListToleratesPartiallyInstalled() {
        MockFeature base10 = new MockFeature("com.example.base-1.0");
        MockFeature base20 = new MockFeature("com.example.base-2.0");

        MockFeature featureA = new MockFeature("com.example.featureA");
        featureA.addDependency("com.example.internalFeatureA-1.0", "2.0");

        MockFeature internalA10 = new MockFeature("com.example.internalFeatureA-1.0");
        internalA10.addDependency("com.example.base-1.0");

        EsaResourceWritable internalA20 = WritableResourceFactory.createEsa(null);
        internalA20.setProvideFeature("com.example.internalFeatureA-2.0");
        internalA20.addRequireFeatureWithTolerates("com.example.base-2.0", Collections.emptyList());

        EsaResourceWritable featureB = WritableResourceFactory.createEsa(null);
        featureB.setProvideFeature("com.example.featureB-1.0");
        featureB.addRequireFeatureWithTolerates("com.example.base-2.0", Collections.emptyList());

        RepositoryResolver resolver = testResolver().withResolvedInstalledFeature(base20, featureA)
                                                    .withInstalledFeature(base10, internalA10)
                                                    .withResolvedFeature(internalA20, featureB)
                                                    .build();

        assertThat(resolver.createInstallList("com.example.featureA"), contains(internalA20));
    }

    private static ResolverBuilder testResolver() {
        return new ResolverBuilder();
    }

    private static class ResolverBuilder {
        List<ProvisioningFeatureDefinition> installedFeatures = new ArrayList<>();
        List<EsaResource> repoFeatures = new ArrayList<>();
        List<SampleResource> repoSamples = new ArrayList<>();
        Map<String, ProvisioningFeatureDefinition> resolvedFeatures = new HashMap<>();

        public ResolverBuilder withFeature(EsaResource... esa) {
            repoFeatures.addAll(Arrays.asList(esa));
            return this;
        }

        public ResolverBuilder withSample(SampleResource... samples) {
            repoSamples.addAll(Arrays.asList(samples));
            return this;
        }

        public ResolverBuilder withResolvedFeature(EsaResource... esas) {
            repoFeatures.addAll(Arrays.asList(esas));
            for (EsaResource esa : esas) {
                resolvedFeatures.put(esa.getProvideFeature(), new KernelResolverEsa(esa));
            }
            return this;
        }

        public ResolverBuilder withInstalledFeature(ProvisioningFeatureDefinition... definitions) {
            installedFeatures.addAll(Arrays.asList(definitions));
            return this;
        }

        public ResolverBuilder withResolvedInstalledFeature(ProvisioningFeatureDefinition... definitions) {
            installedFeatures.addAll(Arrays.asList(definitions));
            for (ProvisioningFeatureDefinition feature : definitions) {
                resolvedFeatures.put(feature.getSymbolicName(), feature);
            }
            return this;
        }

        public RepositoryResolver build() {
            RepositoryResolver resolver = new RepositoryResolver(installedFeatures, repoFeatures, repoSamples);
            resolver.initResolve();
            resolver.initializeResolverRepository(Collections.<ProductDefinition> emptySet());
            resolver.resolvedFeatures = resolvedFeatures;
            return resolver;
        }
    }
}
