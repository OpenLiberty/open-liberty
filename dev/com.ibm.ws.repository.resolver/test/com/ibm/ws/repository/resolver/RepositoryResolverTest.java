/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resolver;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;

import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.resolver.RepositoryResolutionException.MissingRequirement;
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
    public void testMaxDistanceComparator() {
        EsaResourceWritable featureA = WritableResourceFactory.createEsa(null);
        featureA.setProvideFeature("com.example.featureA");

        EsaResourceWritable featureB = WritableResourceFactory.createEsa(null);
        featureB.setProvideFeature("com.example.featureB");

        EsaResourceWritable featureC = WritableResourceFactory.createEsa(null);
        featureC.setProvideFeature("com.example.featureC");

        SampleResourceWritable sampleA = WritableResourceFactory.createSample(null, ResourceType.PRODUCTSAMPLE);
        sampleA.setShortName("sampleA");

        Map<String, Integer> distanceMap = new HashMap<>();
        distanceMap.put("com.example.featureA", 1);
        distanceMap.put("com.example.featureB", 2);

        Comparator<RepositoryResource> comparator = RepositoryResolver.byMaxDistance(distanceMap);

        // B has a greater distance than A, so it should come first when sorting, so A should be considered greater. A > B
        assertThat(comparator.compare(featureA, featureB), greaterThan(0));

        // sampleA is not a feature, it should come last in sorting so it should be greater. featureA < sampleA
        assertThat(comparator.compare(featureA, sampleA), lessThan(0));

        // featureC is not listed in the distanceMap, it should come last in sorting so it should be greater. A < C
        assertThat(comparator.compare(featureA, featureC), lessThan(0));

        // sampleA and featureC should be considered equal, as neither is is in the distance map. featureC == sampleA
        assertThat(comparator.compare(featureC, sampleA), equalTo(0));
    }

    @Test
    public void testPopulateMaxDistanceMap() {
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

        RepositoryResolver resolver = testResolver().withResolvedFeature(featureA, featureB, featureC, featureD, featureE).build();

        Map<String, Integer> distanceMap = new HashMap<>();
        resolver.populateMaxDistanceMap(distanceMap, "com.example.featureA", 0, new HashSet<ProvisioningFeatureDefinition>(), new ArrayList<MissingRequirement>());

        assertThat(distanceMap, hasEntry("com.example.featureA", 0));
        assertThat(distanceMap, hasEntry("com.example.featureB", 1));
        assertThat(distanceMap, hasEntry("com.example.featureC", 2));
        assertThat(distanceMap, hasEntry("com.example.featureD", 3)); // Note that featureD has distance of 3, even though featureA depends directly on featureD
        assertThat(distanceMap.entrySet(), hasSize(4));

        distanceMap = new HashMap<>();
        resolver.populateMaxDistanceMap(distanceMap, "com.example.featureC", 0, new HashSet<ProvisioningFeatureDefinition>(), new ArrayList<MissingRequirement>());
        assertThat(distanceMap, hasEntry("com.example.featureC", 0));
        assertThat(distanceMap, hasEntry("com.example.featureD", 1));
        assertThat(distanceMap.entrySet(), hasSize(2));
    }

    @Test
    public void testPopulateMaxDistanceMapTolerates() {
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

        Map<String, Integer> distanceMap = new HashMap<>();
        resolver.populateMaxDistanceMap(distanceMap, "com.example.featureA-1.0", 0, new HashSet<ProvisioningFeatureDefinition>(), new ArrayList<MissingRequirement>());

        assertThat(distanceMap, hasEntry("com.example.featureA-1.0", 0));
        assertThat(distanceMap, hasEntry("com.example.featureB-2.0", 1)); // Note that we've picked featureB-2.0 from the tolerated versions
        assertThat(distanceMap, hasEntry("com.example.featureC-1.2", 2)); // Note that we've picked featureC-1.2 rather than featureC-1.5 because 1.2 is earlier in the tolerates list
        assertThat(distanceMap, hasEntry("com.example.featureD", 3)); // Note that featureD has distance of 3, even though featureA depends directly on featureD
        assertThat(distanceMap.entrySet(), hasSize(4));

        distanceMap = new HashMap<>();
        resolver.populateMaxDistanceMap(distanceMap, "com.example.featureC-1.5", 0, new HashSet<ProvisioningFeatureDefinition>(), new ArrayList<MissingRequirement>());

        assertThat(distanceMap, hasEntry("com.example.featureC-1.5", 0));
        assertThat(distanceMap, hasEntry("com.example.featureD", 1));
        assertThat(distanceMap.entrySet(), hasSize(2));
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

        public RepositoryResolver build() {
            RepositoryResolver resolver = new RepositoryResolver(installedFeatures, repoFeatures, repoSamples);
            resolver.initResolve();
            resolver.resolvedFeatures = resolvedFeatures;
            return resolver;
        }
    }
}
