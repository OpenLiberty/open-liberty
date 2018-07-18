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
package com.ibm.ws.repository.resolver.internal.kernel;

import static com.ibm.ws.repository.resolver.internal.kernel.KernelResolverResultMatcher.result;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.ibm.ws.kernel.feature.internal.FeatureResolverImpl;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;
import com.ibm.ws.repository.common.enums.InstallPolicy;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.writeable.EsaResourceWritable;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;

/**
 * Test doing resolution using {@link FeatureResolver} and {@link EsaResource}
 */
public class FeatureResolverTest {

    @Test
    public void testSingleFeatureResolution() {
        FeatureResolver resolver = new FeatureResolverImpl();
        KernelResolverRepository repo = new KernelResolverRepository(null, null);

        EsaResourceWritable esa = WritableResourceFactory.createEsa(null);
        esa.setProvideFeature("com.example.featureA");
        esa.setVisibility(Visibility.PUBLIC);
        repo.addFeature(esa);

        Result result = resolver.resolveFeatures(repo, Arrays.asList("com.example.featureA"), Collections.<String> emptySet(), false);

        assertThat(result, is(result().withResolvedFeatures("com.example.featureA")));
    }

    @Test
    public void testDependencyResolution() {
        FeatureResolver resolver = new FeatureResolverImpl();
        KernelResolverRepository repo = new KernelResolverRepository(null, null);

        EsaResourceWritable featureA = WritableResourceFactory.createEsa(null);
        featureA.setProvideFeature("com.example.featureA");
        featureA.setVisibility(Visibility.PUBLIC);
        repo.addFeature(featureA);

        EsaResourceWritable featureB = WritableResourceFactory.createEsa(null);
        featureB.setProvideFeature("com.example.featureB");
        featureB.setVisibility(Visibility.PUBLIC);
        featureB.addRequireFeatureWithTolerates("com.example.featureA", Collections.<String> emptyList());
        repo.addFeature(featureB);

        Result result = resolver.resolveFeatures(repo, Arrays.asList("com.example.featureB"), Collections.<String> emptySet(), false);

        assertThat(result, is(result().withResolvedFeatures("com.example.featureA", "com.example.featureB")));
    }

    @Test
    public void testToleratesChoosesPreferred() {
        FeatureResolver resolver = new FeatureResolverImpl();
        KernelResolverRepository repo = new KernelResolverRepository(null, null);

        EsaResourceWritable featureA10 = WritableResourceFactory.createEsa(null);
        featureA10.setProvideFeature("com.example.featureA-1.0");
        featureA10.setVisibility(Visibility.PUBLIC);
        repo.addFeature(featureA10);

        EsaResourceWritable featureA11 = WritableResourceFactory.createEsa(null);
        featureA11.setProvideFeature("com.example.featureA-1.1");
        featureA11.setVisibility(Visibility.PUBLIC);
        repo.addFeature(featureA11);

        EsaResourceWritable featureB = WritableResourceFactory.createEsa(null);
        featureB.setProvideFeature("com.example.featureB");
        featureB.setVisibility(Visibility.PUBLIC);
        featureB.addRequireFeatureWithTolerates("com.example.featureA-1.0", Arrays.asList("1.1"));
        repo.addFeature(featureB);

        Result result = resolver.resolveFeatures(repo, Arrays.asList("com.example.featureB"), Collections.<String> emptySet(), false);

        assertThat(result, is(result().withResolvedFeatures("com.example.featureA-1.0", "com.example.featureB")));
    }

    @Test
    public void testToleratesUsedWhenPreferredMissing() {
        FeatureResolver resolver = new FeatureResolverImpl();
        KernelResolverRepository repo = new KernelResolverRepository(null, null);

        EsaResourceWritable featureA11 = WritableResourceFactory.createEsa(null);
        featureA11.setProvideFeature("com.example.featureA-1.1");
        featureA11.setVisibility(Visibility.PUBLIC);
        repo.addFeature(featureA11);

        EsaResourceWritable featureB = WritableResourceFactory.createEsa(null);
        featureB.setProvideFeature("com.example.featureB");
        featureB.setVisibility(Visibility.PUBLIC);
        featureB.addRequireFeatureWithTolerates("com.example.featureA-1.0", Arrays.asList("1.1"));
        repo.addFeature(featureB);

        Result result = resolver.resolveFeatures(repo, Arrays.asList("com.example.featureB"), Collections.<String> emptySet(), false);

        assertThat(result, is(result().withResolvedFeatures("com.example.featureA-1.1", "com.example.featureB")));
    }

    @Test
    public void testToleratesUsedWhenConflictingVersionsRequired() {
        FeatureResolver resolver = new FeatureResolverImpl();
        KernelResolverRepository repo = new KernelResolverRepository(null, null);

        EsaResourceWritable featureA10 = WritableResourceFactory.createEsa(null);
        featureA10.setProvideFeature("com.example.featureA-1.0");
        featureA10.setVisibility(Visibility.PUBLIC);
        featureA10.setSingleton("true");
        repo.addFeature(featureA10);

        EsaResourceWritable featureA11 = WritableResourceFactory.createEsa(null);
        featureA11.setProvideFeature("com.example.featureA-1.1");
        featureA11.setVisibility(Visibility.PUBLIC);
        featureA11.setSingleton("true");
        repo.addFeature(featureA11);

        EsaResourceWritable featureB = WritableResourceFactory.createEsa(null);
        featureB.setProvideFeature("com.example.featureB");
        featureB.setVisibility(Visibility.PUBLIC);
        featureB.addRequireFeatureWithTolerates("com.example.featureA-1.0", Arrays.asList("1.1"));
        repo.addFeature(featureB);

        EsaResourceWritable featureC = WritableResourceFactory.createEsa(null);
        featureC.setProvideFeature("com.example.featureC");
        featureC.setVisibility(Visibility.PUBLIC);
        featureC.addRequireFeatureWithTolerates("com.example.featureA-1.1", Collections.<String> emptyList());
        repo.addFeature(featureC);

        Result result = resolver.resolveFeatures(repo, Arrays.asList("com.example.featureB", "com.example.featureC"), Collections.<String> emptySet(), false);

        assertThat(result, is(result().withResolvedFeatures("com.example.featureA-1.1", "com.example.featureB", "com.example.featureC")));
    }

    @Test
    public void testToleratesNotUsedWhenNotSingleton() {
        FeatureResolver resolver = new FeatureResolverImpl();
        KernelResolverRepository repo = new KernelResolverRepository(null, null);

        // Note featureA-1.0 and 1.1 are not singletons so can resolve together
        EsaResourceWritable featureA10 = WritableResourceFactory.createEsa(null);
        featureA10.setProvideFeature("com.example.featureA-1.0");
        featureA10.setVisibility(Visibility.PUBLIC);
        repo.addFeature(featureA10);

        EsaResourceWritable featureA11 = WritableResourceFactory.createEsa(null);
        featureA11.setProvideFeature("com.example.featureA-1.1");
        featureA11.setVisibility(Visibility.PUBLIC);
        repo.addFeature(featureA11);

        EsaResourceWritable featureB = WritableResourceFactory.createEsa(null);
        featureB.setProvideFeature("com.example.featureB");
        featureB.setVisibility(Visibility.PUBLIC);
        featureB.addRequireFeatureWithTolerates("com.example.featureA-1.0", Arrays.asList("1.1"));
        repo.addFeature(featureB);

        EsaResourceWritable featureC = WritableResourceFactory.createEsa(null);
        featureC.setProvideFeature("com.example.featureC");
        featureC.setVisibility(Visibility.PUBLIC);
        featureC.addRequireFeatureWithTolerates("com.example.featureA-1.1", Collections.<String> emptyList());
        repo.addFeature(featureC);

        Result result = resolver.resolveFeatures(repo, Arrays.asList("com.example.featureB", "com.example.featureC"), Collections.<String> emptySet(), false);

        assertThat(result, is(result().withResolvedFeatures("com.example.featureA-1.1", "com.example.featureA-1.0", "com.example.featureB", "com.example.featureC")));
    }

    @Test
    public void testAutofeatureResolved() {
        FeatureResolver resolver = new FeatureResolverImpl();
        KernelResolverRepository repo = new KernelResolverRepository(null, null);

        EsaResourceWritable featureA = WritableResourceFactory.createEsa(null);
        featureA.setProvideFeature("com.example.featureA");
        featureA.setVisibility(Visibility.PUBLIC);
        repo.addFeature(featureA);

        EsaResourceWritable featureB = WritableResourceFactory.createEsa(null);
        featureB.setProvideFeature("com.example.featureB");
        featureB.setVisibility(Visibility.PUBLIC);
        repo.addFeature(featureB);

        EsaResourceWritable autoFeature = WritableResourceFactory.createEsa(null);
        autoFeature.setProvideFeature("com.example.autoFeature");
        autoFeature.setInstallPolicy(InstallPolicy.WHEN_SATISFIED);
        autoFeature.setProvisionCapability("osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=com.example.featureA))\","
                                           + "osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=com.example.featureB))\"");
        repo.addFeature(autoFeature);

        // Test we get the autofeature when requesting featureA and featureB
        Result result = resolver.resolveFeatures(repo, Arrays.asList("com.example.featureA", "com.example.featureB"), Collections.<String> emptySet(), false);
        assertThat(result, is(result().withResolvedFeatures("com.example.featureA", "com.example.featureB", "com.example.autoFeature")));

        // Test we don't get the autofeature when requesting only featureB
        Result result2 = resolver.resolveFeatures(repo, Arrays.asList("com.example.featureB"), Collections.<String> emptySet(), false);
        assertThat(result2, is(result().withResolvedFeatures("com.example.featureB")));
    }

}
