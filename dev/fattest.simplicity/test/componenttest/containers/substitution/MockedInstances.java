/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers.substitution;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.utility.DockerImageName;

import componenttest.containers.registry.ArtifactoryRegistry;
import componenttest.containers.registry.InternalRegistry;

public class MockedInstances {

    /**
     * Must be called in a try-with-resources block
     */
    public static MockedStatic<ArtifactoryRegistry> artifactoryRegistry(String registry, Throwable setupException, boolean isAvailable) {
        // Create a mocked instance
        ArtifactoryRegistry instance = Mockito.mock(ArtifactoryRegistry.class);
        //Field methods
        when(instance.getRegistry()).thenReturn(registry);
        when(instance.getSetupException()).thenReturn(setupException);
        when(instance.isRegistryAvailable()).thenReturn(isAvailable);

        //Passthrough methods
        when(instance.supportsRegistry(any(DockerImageName.class))).thenCallRealMethod();
        when(instance.supportsRepository(any(DockerImageName.class))).thenCallRealMethod();
        when(instance.getMirrorRepository(any(DockerImageName.class))).thenCallRealMethod();

        // Mock the InternalRegistry.instance() static method
        MockedStatic<ArtifactoryRegistry> mockInternalRegistry = Mockito.mockStatic(ArtifactoryRegistry.class);
        mockInternalRegistry.when(ArtifactoryRegistry::instance).thenReturn(instance);
        return mockInternalRegistry;
    }

    /**
     * Must be called in a try-with-resources block
     */
    public static MockedStatic<InternalRegistry> internalRegistry(String registry, Throwable setupException, boolean isAvailable) {
        // Create a mocked instance
        InternalRegistry instance = Mockito.mock(InternalRegistry.class);
        //Field methods
        when(instance.getRegistry()).thenReturn(registry);
        when(instance.getSetupException()).thenReturn(setupException);
        when(instance.isRegistryAvailable()).thenReturn(isAvailable);

        //Passthrough methods
        when(instance.supportsRegistry(any(DockerImageName.class))).thenCallRealMethod();
        when(instance.supportsRepository(any(DockerImageName.class))).thenCallRealMethod();
        when(instance.getMirrorRepository(any(DockerImageName.class))).thenCallRealMethod();

        // Mock the InternalRegistry.instance() static method
        MockedStatic<InternalRegistry> mockInternalRegistry = Mockito.mockStatic(InternalRegistry.class);
        mockInternalRegistry.when(InternalRegistry::instance).thenReturn(instance);

        return mockInternalRegistry;
    }

    /**
     * Must be called in a try-with-resources block
     */
    public static MockedStatic<DockerClientFactory> dockerClientFactory(Class<? extends DockerClientProviderStrategy> providerStrategyClass) {
        // Create a mocked instance
        // Note: last matcher wins
        DockerClientFactory instance = Mockito.mock(DockerClientFactory.class);
        when(instance.isUsing(any())).thenReturn(Boolean.FALSE);
        when(instance.isUsing(providerStrategyClass)).thenReturn(Boolean.TRUE);

        // Mock the DockerClientFactory.instance() static method
        MockedStatic<DockerClientFactory> mockDockerClientFactory = Mockito.mockStatic(DockerClientFactory.class);
        mockDockerClientFactory.when(DockerClientFactory::instance).thenReturn(instance);

        return mockDockerClientFactory;
    }
}
