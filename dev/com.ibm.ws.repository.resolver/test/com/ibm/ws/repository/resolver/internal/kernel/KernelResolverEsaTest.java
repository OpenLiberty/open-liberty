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

import static com.ibm.ws.repository.resolver.internal.kernel.FeatureResourceMatcher.featureResource;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.osgi.framework.Version;

import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.repository.common.enums.InstallPolicy;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.resources.writeable.EsaResourceWritable;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;

public class KernelResolverEsaTest {

    @Test
    public void testSymbolicName() {
        EsaResourceWritable esa = WritableResourceFactory.createEsa(null);
        esa.setProvideFeature("com.example.featureA");

        KernelResolverEsa resolverEsa = new KernelResolverEsa(esa);
        assertThat(resolverEsa.getSymbolicName(), is("com.example.featureA"));
    }

    @Test
    public void testEmptyEsa() {
        EsaResourceWritable esa = WritableResourceFactory.createEsa(null);
        esa.setProvideFeature("com.example.featureA");

        KernelResolverEsa resolverEsa = new KernelResolverEsa(esa);

        assertThat(resolverEsa.getSymbolicName(), is("com.example.featureA"));
        assertThat(resolverEsa.getFeatureName(), is("com.example.featureA"));

        assertThat(resolverEsa.getConstituents(SubsystemContentType.FEATURE_TYPE), is(empty()));
        assertThat(resolverEsa.getIbmShortName(), is(nullValue()));
        assertThat(resolverEsa.isCapabilitySatisfied(Collections.<ProvisioningFeatureDefinition> emptyList()), is(true));
        assertThat(resolverEsa.isAutoFeature(), is(false));
        assertThat(resolverEsa.isSingleton(), is(false));
        assertThat(resolverEsa.getVisibility(), is(com.ibm.ws.kernel.feature.Visibility.PUBLIC)); // Esas always public
        assertThat(resolverEsa.getProcessTypes(), is(EnumSet.of(ProcessType.SERVER)));
    }

    @Test
    public void testBundleRepositoryType() {
        EsaResourceWritable esa = WritableResourceFactory.createEsa(null);

        KernelResolverEsa resolverEsa = new KernelResolverEsa(esa);
        assertThat(resolverEsa.getBundleRepositoryType(), is(""));
    }

    @Test
    public void testConstituents() {
        EsaResourceWritable esa = WritableResourceFactory.createEsa(null);
        esa.setProvideFeature("com.example.featureA");
        esa.addRequireFeatureWithTolerates("com.example.featureB-1.0", Arrays.asList("1.1", "1.5"));

        KernelResolverEsa resolverEsa = new KernelResolverEsa(esa);
        assertThat(resolverEsa.getConstituents(SubsystemContentType.FEATURE_TYPE), contains(featureResource("com.example.featureB-1.0", "1.1", "1.5")));
    }

    @Test
    public void testIbmShortName() {
        EsaResourceWritable esa = WritableResourceFactory.createEsa(null);
        esa.setProvideFeature("com.example.featureA");
        esa.setShortName("featureA");

        KernelResolverEsa resolverEsa = new KernelResolverEsa(esa);
        assertThat(resolverEsa.getIbmShortName(), is("featureA"));
    }

    @Test
    public void testCapabilitySatisfied() {
        EsaResourceWritable featureA = WritableResourceFactory.createEsa(null);
        featureA.setProvideFeature("com.example.featureA");

        EsaResourceWritable featureB = WritableResourceFactory.createEsa(null);
        featureB.setProvideFeature("com.example.featureB");

        EsaResourceWritable autoFeature = WritableResourceFactory.createEsa(null);
        autoFeature.setProvideFeature("com.example.autoFeature");
        autoFeature.setInstallPolicy(InstallPolicy.WHEN_SATISFIED);
        autoFeature.setProvisionCapability("osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=com.example.featureA))\","
                                           + "osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=com.example.featureB))\"");

        KernelResolverEsa resolverFeatureA = new KernelResolverEsa(featureA);
        KernelResolverEsa resolverFeatureB = new KernelResolverEsa(featureB);
        KernelResolverEsa resolverAutoFeature = new KernelResolverEsa(autoFeature);

        assertThat(resolverFeatureA.isCapabilitySatisfied(Collections.<ProvisioningFeatureDefinition> emptySet()), is(true));
        assertThat(resolverFeatureB.isCapabilitySatisfied(Collections.<ProvisioningFeatureDefinition> emptySet()), is(true));
        assertThat(resolverAutoFeature.isCapabilitySatisfied(Collections.<ProvisioningFeatureDefinition> emptySet()), is(false));
        assertThat(resolverAutoFeature.isCapabilitySatisfied(Arrays.<ProvisioningFeatureDefinition> asList(resolverFeatureA, resolverFeatureB)), is(true));

        assertThat(resolverAutoFeature.findFeaturesSatisfyingCapability(Arrays.asList(resolverFeatureA, resolverFeatureB, resolverAutoFeature)),
                   Matchers.<ProvisioningFeatureDefinition> containsInAnyOrder(resolverFeatureA, resolverFeatureB));
    }

    @Test
    public void testIsAutoFeature() {
        EsaResourceWritable esa = WritableResourceFactory.createEsa(null);
        KernelResolverEsa resolverEsa = new KernelResolverEsa(esa);

        assertThat(resolverEsa.isAutoFeature(), is(false));

        esa.setInstallPolicy(InstallPolicy.WHEN_SATISFIED);
        assertThat(resolverEsa.isAutoFeature(), is(true));

        esa.setInstallPolicy(InstallPolicy.MANUAL);
        assertThat(resolverEsa.isAutoFeature(), is(false));
    }

    @Test
    public void testIsSingleton() {
        EsaResourceWritable esa = WritableResourceFactory.createEsa(null);

        KernelResolverEsa resolverEsa = new KernelResolverEsa(esa);
        assertThat(resolverEsa.isSingleton(), is(false));

        esa.setSingleton("true");
        assertThat(resolverEsa.isSingleton(), is(true));

        esa.setSingleton("false");
        assertThat(resolverEsa.isSingleton(), is(false));
    }

    @Test
    public void testGetFeatureName() {
        EsaResourceWritable featureA = WritableResourceFactory.createEsa(null);
        featureA.setProvideFeature("com.example.featureA");

        EsaResourceWritable featureB = WritableResourceFactory.createEsa(null);
        featureB.setProvideFeature("com.example.featureB");
        featureB.setShortName("featureB");

        KernelResolverEsa resolverFeatureA = new KernelResolverEsa(featureA);
        KernelResolverEsa resolverFeatureB = new KernelResolverEsa(featureB);

        assertThat(resolverFeatureA.getFeatureName(), is("com.example.featureA"));
        assertThat(resolverFeatureB.getFeatureName(), is("featureB"));
    }

    @Test
    public void testGetVisibility() {
        EsaResourceWritable publicFeature = WritableResourceFactory.createEsa(null);
        publicFeature.setProvideFeature("com.example.publicFeature");
        publicFeature.setVisibility(Visibility.PUBLIC);

        EsaResourceWritable privateFeature = WritableResourceFactory.createEsa(null);
        privateFeature.setProvideFeature("com.example.privateFeature");
        privateFeature.setVisibility(Visibility.PRIVATE);

        EsaResourceWritable protectedFeature = WritableResourceFactory.createEsa(null);
        protectedFeature.setProvideFeature("com.example.protectedFeature");
        protectedFeature.setVisibility(Visibility.PROTECTED);

        EsaResourceWritable installFeature = WritableResourceFactory.createEsa(null);
        installFeature.setProvideFeature("com.example.installFeature");
        installFeature.setVisibility(Visibility.INSTALL);

        KernelResolverEsa resolverPublicFeature = new KernelResolverEsa(publicFeature);
        KernelResolverEsa resolverPrivateFeature = new KernelResolverEsa(privateFeature);
        KernelResolverEsa resolverProtectedFeature = new KernelResolverEsa(protectedFeature);
        KernelResolverEsa resolverInstallFeature = new KernelResolverEsa(installFeature);

        // Note: no matter the visibility of the EsaResource, the ResolverEsa should always report as PUBLIC
        // this is because we allow installation of private and protected resources by name, even though they're
        // not permitted in the server.xml.
        assertThat(resolverPublicFeature.getVisibility(), is(com.ibm.ws.kernel.feature.Visibility.PUBLIC));
        assertThat(resolverPrivateFeature.getVisibility(), is(com.ibm.ws.kernel.feature.Visibility.PUBLIC));
        assertThat(resolverProtectedFeature.getVisibility(), is(com.ibm.ws.kernel.feature.Visibility.PUBLIC));
        assertThat(resolverInstallFeature.getVisibility(), is(com.ibm.ws.kernel.feature.Visibility.PUBLIC));
    }

    @Test
    public void testVersion() {
        EsaResourceWritable esa = WritableResourceFactory.createEsa(null);
        KernelResolverEsa resolverEsa = new KernelResolverEsa(esa);
        assertThat(resolverEsa.getVersion(), is(Version.emptyVersion));

        EsaResourceWritable esa2 = WritableResourceFactory.createEsa(null);
        esa2.setVersion("1.0");
        KernelResolverEsa resolverEsa2 = new KernelResolverEsa(esa2);
        assertThat(resolverEsa2.getVersion(), is(Version.valueOf("1.0.0")));

        EsaResourceWritable esa3 = WritableResourceFactory.createEsa(null);
        esa3.setVersion("4.8.2");
        KernelResolverEsa resolverEsa3 = new KernelResolverEsa(esa3);
        assertThat(resolverEsa3.getVersion(), is(Version.valueOf("4.8.2")));

        EsaResourceWritable esa4 = WritableResourceFactory.createEsa(null);
        esa2.setVersion("wibble");
        KernelResolverEsa resolverEsa4 = new KernelResolverEsa(esa4);
        assertThat(resolverEsa4.getVersion(), is(Version.emptyVersion));
    }

}
