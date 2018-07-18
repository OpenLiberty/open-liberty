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

import static com.ibm.ws.repository.resolver.internal.kernel.KernelResolverEsaMatcher.resolverEsaWrapping;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.jmock.Mockery;
import org.junit.Test;
import org.osgi.framework.Version;

import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.repository.common.enums.InstallPolicy;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.resolver.ResolverTestUtils;
import com.ibm.ws.repository.resources.writeable.EsaResourceWritable;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;

/**
 * Unit tests for {@link KernelResolverRepository}
 */
public class KernelResolverRepositoryTest {

    @Test
    public void testSymbolicNameLookup() {
        EsaResourceWritable esa = WritableResourceFactory.createEsa(null);
        esa.setProvideFeature("com.example.featureA");

        KernelResolverRepository repo = new KernelResolverRepository(null, null);
        repo.addFeature(esa);

        assertThat(repo.getFeature("com.example.featureA"), is(resolverEsaWrapping(esa)));
        assertThat(repo.getFeature("wibble"), is(nullValue()));
    }

    @Test
    public void testPublicEsaLookup() {
        EsaResourceWritable publicEsa = WritableResourceFactory.createEsa(null);
        publicEsa.setProvideFeature("com.example.publicFeature");
        publicEsa.setShortName("publicFeature");
        publicEsa.setVisibility(Visibility.PUBLIC);

        KernelResolverRepository repo = new KernelResolverRepository(null, null);
        repo.addFeature(publicEsa);

        // Lookup by symbolic name works case insensitively
        assertThat(repo.getFeature("com.example.publicFeature"), is(resolverEsaWrapping(publicEsa)));
        assertThat(repo.getFeature("com.EXAMPLE.publicfeature"), is(resolverEsaWrapping(publicEsa)));

        // Looking up public feature works case insensitively
        assertThat(repo.getFeature("publicFeature"), is(resolverEsaWrapping(publicEsa)));
        assertThat(repo.getFeature("PUBLICfeature"), is(resolverEsaWrapping(publicEsa)));
    }

    @Test
    public void testPrivateEsaLookup() {
        EsaResourceWritable privateEsa = WritableResourceFactory.createEsa(null);
        privateEsa.setProvideFeature("com.example.privateFeature");
        privateEsa.setShortName("privateFeature");
        privateEsa.setVisibility(Visibility.PRIVATE);

        KernelResolverRepository repo = new KernelResolverRepository(null, null);
        repo.addFeature(privateEsa);

        // Lookup by symbolic name works case insensitively
        assertThat(repo.getFeature("com.example.privateFeature"), is(resolverEsaWrapping(privateEsa)));
        assertThat(repo.getFeature("com.EXAMPLE.privatefeature"), is(resolverEsaWrapping(privateEsa)));

        // Looking up by short name works case insensitively
        assertThat(repo.getFeature("privateFeature"), is(resolverEsaWrapping(privateEsa)));
        assertThat(repo.getFeature("PRIVATEfeature"), is(resolverEsaWrapping(privateEsa)));
    }

    @Test
    public void testProtectedEsaLookup() {
        EsaResourceWritable protectedEsa = WritableResourceFactory.createEsa(null);
        protectedEsa.setProvideFeature("com.example.protectedFeature");
        protectedEsa.setShortName("protectedFeature");
        protectedEsa.setVisibility(Visibility.PROTECTED);

        KernelResolverRepository repo = new KernelResolverRepository(null, null);
        repo.addFeature(protectedEsa);

        // Lookup by symbolic name works case insensitively
        assertThat(repo.getFeature("com.example.protectedFeature"), is(resolverEsaWrapping(protectedEsa)));
        assertThat(repo.getFeature("com.EXAMPLE.protectedfeature"), is(resolverEsaWrapping(protectedEsa)));

        // Looking up by short name works case insensitively
        assertThat(repo.getFeature("protectedFeature"), is(resolverEsaWrapping(protectedEsa)));
        assertThat(repo.getFeature("PROTECTEDfeature"), is(resolverEsaWrapping(protectedEsa)));
    }

    @Test
    public void testInstallEsaLookup() {
        EsaResourceWritable installEsa = WritableResourceFactory.createEsa(null);
        installEsa.setProvideFeature("com.example.installFeature");
        installEsa.setShortName("installFeature");
        installEsa.setVisibility(Visibility.INSTALL);

        KernelResolverRepository repo = new KernelResolverRepository(null, null);
        repo.addFeature(installEsa);

        // Lookup by symbolic name works case insensitively
        assertThat(repo.getFeature("com.example.installFeature"), is(resolverEsaWrapping(installEsa)));
        assertThat(repo.getFeature("com.EXAMPLE.installFeature"), is(resolverEsaWrapping(installEsa)));

        // Looking up by short name works case insensitively
        assertThat(repo.getFeature("installFeature"), is(resolverEsaWrapping(installEsa)));
        assertThat(repo.getFeature("INSTALLfeature"), is(resolverEsaWrapping(installEsa)));
    }

    @Test
    public void testGetConfiguredTolerates() {
        EsaResourceWritable esa = WritableResourceFactory.createEsa(null);
        esa.setProvideFeature("com.example.featureA");

        KernelResolverRepository repo = new KernelResolverRepository(null, null);
        repo.addFeature(esa);

        // GetConfiguredTolerates should always return an empty list
        assertThat(repo.getConfiguredTolerates("com.example.featureA"), is(empty()));
        assertThat(repo.getConfiguredTolerates("wibble"), is(empty()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetAutoFeatures() {
        KernelResolverRepository repo = new KernelResolverRepository(null, null);

        EsaResourceWritable featureA = WritableResourceFactory.createEsa(null);
        featureA.setProvideFeature("com.example.featureA");
        repo.addFeature(featureA);

        EsaResourceWritable featureB = WritableResourceFactory.createEsa(null);
        featureB.setProvideFeature("com.example.featureB");
        repo.addFeature(featureB);

        EsaResourceWritable autoFeatureA = WritableResourceFactory.createEsa(null);
        autoFeatureA.setProvideFeature("com.example.autoFeatureA");
        autoFeatureA.setInstallPolicy(InstallPolicy.WHEN_SATISFIED);
        repo.addFeature(autoFeatureA);

        EsaResourceWritable autoFeatureB = WritableResourceFactory.createEsa(null);
        autoFeatureB.setProvideFeature("com.example.autoFeatureB");
        autoFeatureB.setInstallPolicy(InstallPolicy.WHEN_SATISFIED);
        repo.addFeature(autoFeatureB);

        assertThat(repo.getAutoFeatures(), containsInAnyOrder(resolverEsaWrapping(autoFeatureA), resolverEsaWrapping(autoFeatureB)));
    }

    @Test
    public void testAddMultipleFeatures() {
        EsaResourceWritable featureA = WritableResourceFactory.createEsa(null);
        featureA.setProvideFeature("com.example.featureA");

        EsaResourceWritable featureB = WritableResourceFactory.createEsa(null);
        featureB.setProvideFeature("com.example.featureB");

        EsaResourceWritable featureAduplicate = WritableResourceFactory.createEsa(null);
        featureAduplicate.setProvideFeature("com.example.featureA");
        featureAduplicate.setShortName("featureA");

        KernelResolverRepository repo = new KernelResolverRepository(null, null);
        repo.addFeatures(Arrays.asList(featureA, featureB, featureAduplicate));

        assertThat(repo.getFeature("com.example.featureA"), is(resolverEsaWrapping(featureA)));
        assertThat(repo.getFeature("com.example.featureB"), is(resolverEsaWrapping(featureB)));

        // Note that featureAduplicate should not have been added since it has the same symbolic
        // name as featureA. Even though it's short name doesn't clash, it shouldn't be there.
        assertThat(repo.getFeature("featureA"), is(nullValue()));
    }

    @Test
    public void testMultipleVersions() {
        EsaResourceWritable featureA_10 = WritableResourceFactory.createEsa(null);
        featureA_10.setProvideFeature("com.example.featureA-1.0");
        featureA_10.setVersion("1.0");

        EsaResourceWritable featureA_11 = WritableResourceFactory.createEsa(null);
        featureA_11.setProvideFeature("com.example.featureA-1.0");
        featureA_11.setVersion("1.1");

        KernelResolverRepository repo = new KernelResolverRepository(null, null);
        repo.addFeatures(Arrays.asList(featureA_10, featureA_11));

        // The latest version should be returned
        assertThat(repo.getFeature("com.example.featureA-1.0"), is(resolverEsaWrapping(featureA_11)));
    }

    @Test
    public void testMultipleVersionsPreferred() {
        EsaResourceWritable featureA_10 = WritableResourceFactory.createEsa(null);
        featureA_10.setProvideFeature("com.example.featureA-1.0");
        featureA_10.setVersion("1.0");

        EsaResourceWritable featureA_11 = WritableResourceFactory.createEsa(null);
        featureA_11.setProvideFeature("com.example.featureA-1.0");
        featureA_11.setVersion("1.1");

        KernelResolverRepository repo = new KernelResolverRepository(null, null);
        repo.addFeatures(Arrays.asList(featureA_10, featureA_11));
        repo.setPreferredVersion("com.example.featureA-1.0", "1.0");

        // The preferred version should be returned
        assertThat(repo.getFeature("com.example.featureA-1.0"), is(resolverEsaWrapping(featureA_10)));

        repo.clearPreferredVersions();

        // After clearing the preferred versions, the latest version should be returned again
        assertThat(repo.getFeature("com.example.featureA-1.0"), is(resolverEsaWrapping(featureA_11)));
    }

    @Test
    public void testAwkwardVersions() {
        EsaResourceWritable featureA_19 = WritableResourceFactory.createEsa(null);
        featureA_19.setProvideFeature("com.example.featureA-1.0");
        featureA_19.setVersion("1.9");

        EsaResourceWritable featureA_110 = WritableResourceFactory.createEsa(null);
        featureA_110.setProvideFeature("com.example.featureA-1.0");
        featureA_110.setVersion("1.10");

        KernelResolverRepository repo = new KernelResolverRepository(null, null);
        repo.addFeatures(Arrays.asList(featureA_19, featureA_110));
        repo.setPreferredVersion("com.example.featureA-1.0", "1.9");

        // The preferred version should be returned
        assertThat(repo.getFeature("com.example.featureA-1.0"), is(resolverEsaWrapping(featureA_19)));

        repo.clearPreferredVersions();

        // After clearing the preferred versions, the latest version should be returned again
        // 1.10 should be considered later than 1.9
        assertThat(repo.getFeature("com.example.featureA-1.0"), is(resolverEsaWrapping(featureA_110)));
    }

    @Test
    public void testInvalidVersions() {
        EsaResourceWritable featureA_1 = WritableResourceFactory.createEsa(null);
        featureA_1.setProvideFeature("com.example.featureA-1.0");
        featureA_1.setVersion("1.0");

        EsaResourceWritable featureA_wibble = WritableResourceFactory.createEsa(null);
        featureA_wibble.setProvideFeature("com.example.featureA-1.0");
        featureA_wibble.setVersion("wibble");

        KernelResolverRepository repo = new KernelResolverRepository(null, null);
        repo.addFeatures(Arrays.asList(featureA_wibble, featureA_1));

        // The feature with the valid version should be returned
        // The invalid version should not cause an exception
        assertThat(repo.getFeature("com.example.featureA-1.0"), is(resolverEsaWrapping(featureA_1)));
    }

    @Test
    public void testInstalledPreferred() {
        EsaResourceWritable esa = WritableResourceFactory.createEsa(null);
        esa.setProvideFeature("com.example.featureA");
        esa.setVersion("1.0.1");

        Mockery mockery = new Mockery();
        ProvisioningFeatureDefinition installedFeature = ResolverTestUtils.mockSimpleFeatureDefinition(mockery, "com.example.featureA", Version.valueOf("1.0.0"), null);

        KernelResolverRepository repo = new KernelResolverRepository(null, null);

        // With just the esa added, it should be returned
        repo.addFeature(esa);
        assertThat(repo.getFeature("com.example.featureA"), is(resolverEsaWrapping(esa)));

        // After adding the installed feature, it should be returned instead
        repo.addFeature(installedFeature);
        assertThat(repo.getFeature("com.example.featureA"), is(installedFeature));

        // Upon adding the esa again, we should still get the installed feature
        repo.addFeature(esa);
        assertThat(repo.getFeature("com.example.featureA"), is(installedFeature));

        // Even if we prefer the version from the repo, we should still get the installed feature
        repo.setPreferredVersion("com.example.featureA", "1.0.1");
        assertThat(repo.getFeature("com.example.featureA"), is(installedFeature));
    }

    /**
     * Occasionally, an installed feature will have a featureName that isn't either it's short name or its symbolic name.
     * <p>
     * E.g., when it's a user feature, the featureName can be "usr:myFeature-1.0".
     */
    @Test
    public void testGetFeatureByFeatureName() {
        Mockery mockery = new Mockery();
        ProvisioningFeatureDefinition installedFeature = ResolverTestUtils.mockSimpleFeatureDefinition(mockery, "com.example.featureA", Version.valueOf("1.0.0"), "featureA",
                                                                                                       "foo:featureA");

        KernelResolverRepository repo = new KernelResolverRepository(null, null);
        repo.addFeature(installedFeature);
        assertThat(repo.getFeature("foo:featureA"), is(installedFeature));
    }

}
