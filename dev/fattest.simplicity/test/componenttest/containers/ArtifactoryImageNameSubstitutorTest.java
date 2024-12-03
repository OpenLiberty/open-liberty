/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
package componenttest.containers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy;
import org.testcontainers.dockerclient.UnixSocketClientProviderStrategy;
import org.testcontainers.utility.DockerImageName;

@SuppressWarnings("deprecation")
public class ArtifactoryImageNameSubstitutorTest {

    private static ArtifactoryRegistry artifactoryRegistry;
    private static final String TESTREGISTRY = "example.com";
    private static final String FORCE_EXTERNAL = "fat.test.artifactory.force.external.repo";

    private static DockerClientFactory dockerClientFactory;

    private static final File TC_CONFIG_FILE = new File(System.getProperty("user.home"), ".testcontainers.properties");

    @BeforeClass
    public static void setup() throws Exception {
        //Delete ~./testcontainer.properties to avoid test failures due to out dated properties
        if (TC_CONFIG_FILE.exists() && TC_CONFIG_FILE.isFile()) {
            assertTrue(TC_CONFIG_FILE.delete());
        }

        //Set a registry system property for mock Artifactory usage
        System.setProperty("fat.test.artifactory.docker.server", TESTREGISTRY);

        //Generate the singleton ArtifactoryRegistry class
        artifactoryRegistry = ArtifactoryRegistry.instance();
        setArtifactoryRegistry(TESTREGISTRY);

        //Generate the singleton DockerClientFactory class
        dockerClientFactory = DockerClientFactory.instance();
        setDockerClientStrategy(new UnixSocketClientProviderStrategy());
    }

    /**
     * Ensure the logic to determine synthetic images only returns true for:
     *
     * local registry + testcontainer repository + latest version
     * OR
     * sha256 repository
     *
     * @throws Exception
     */
    @Test
    public void testSyntheticImages() throws Exception {
        Map<DockerImageName, Boolean> testMap = new HashMap<>();

        testMap.put(DockerImageName.parse("quay.io/testcontainers/ryuk:1.0.0"), false);
        testMap.put(DockerImageName.parse("quay.io/testcontainers/ryuk:latest"), false);
        testMap.put(DockerImageName.parse("quay.io/kyleaure/oracle-xe:1.0.0"), false);
        testMap.put(DockerImageName.parse("quay.io/kyleaure/oracle-xe:latest"), false);
        testMap.put(DockerImageName.parse("quay.io/openliberty:1.0.0"), false);
        testMap.put(DockerImageName.parse("quay.io/openliberty:latest"), false);

        testMap.put(DockerImageName.parse("localhost/testcontainers/ryuk:1.0.0"), false);
        testMap.put(DockerImageName.parse("localhost/testcontainers/ryuk:latest"), true);
        testMap.put(DockerImageName.parse("localhost/kyleaure/oracle-xe:1.0.0"), false);
        testMap.put(DockerImageName.parse("localhost/kyleaure/oracle-xe:latest"), false);
        testMap.put(DockerImageName.parse("localhost/openliberty:1.0.0"), false);
        testMap.put(DockerImageName.parse("localhost/openliberty:latest"), false);

        testMap.put(DockerImageName.parse("testcontainers/ryuk:1.0.0"), false);
        testMap.put(DockerImageName.parse("testcontainers/ryuk:latest"), false);
        testMap.put(DockerImageName.parse("kyleaure/oracle-xe:1.0.0"), false);
        testMap.put(DockerImageName.parse("kyleaure/oracle-xe:latest"), false);
        testMap.put(DockerImageName.parse("openliberty:1.0.0"), false);
        testMap.put(DockerImageName.parse("openliberty:latest"), false);

        testMap.put(DockerImageName.parse("sha256:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"), true);
        testMap.put(DockerImageName.parse("aes:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"), false);
        testMap.put(DockerImageName.parse("xor:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"), false);

        Method isSyntheticImage = getIsSyntheticImage();

        for (Entry<DockerImageName, Boolean> entry : testMap.entrySet()) {
            assertEquals(entry.getValue(), isSyntheticImage.invoke(null, entry.getKey()));
        }
    }

    // Priority 1: If we are using a synthetic image do not substitute nor cache
    @Test
    public void testSyntheticImageNotModified() throws Exception {
        setArtifactoryRegistryAvailable(false);
        DockerImageName expected;

        expected = DockerImageName.parse("localhost/testcontainers/ryuk:latest");
        assertEquals(expected, new ArtifactoryImageNameSubstitutor().apply(expected));

        expected = DockerImageName.parse("sha256:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b");
        assertEquals(expected, new ArtifactoryImageNameSubstitutor().apply(expected));
    }

    // Priority 2: If the image is known to only exist in an Artifactory registry
    @Test
    public void testArtifactoryRegistryModifification() throws Exception {
        setArtifactoryRegistryAvailable(false);
        DockerImageName testImage;

        testImage = DockerImageName.parse("docker-na-public.artifactory.swg-devops.com/kyleaure/oracle-xe:1.0.0");
        try {
            new ArtifactoryImageNameSubstitutor().apply(testImage);
            fail("Should not have allowed artifactory registry.");
        } catch (RuntimeException e) {
            //pass
        } catch (Throwable t) {
            fail("Wrong throwable caught" + t.getMessage());
        }
    }

    // Priority 3: If a public registry was explicitly set on an image, do not substitute
    @Test
    public void testExplicitRegistryNotModified() throws Exception {
        setArtifactoryRegistryAvailable(false);
        DockerImageName expected;

        expected = DockerImageName.parse("quay.io/testcontainers/ryuk:1.0.0");
        assertEquals(expected, new ArtifactoryImageNameSubstitutor().apply(expected));
    }

    // Priority 4: Always use Artifactory if using remote docker host.
    @Test
    public void testDockerClientStrategy() throws Exception {
        DockerImageName input;
        DockerImageName expected;

        //Using our remote docker host
        setArtifactoryRegistryAvailable(true);
        setDockerClientStrategy(new EnvironmentAndSystemPropertyClientProviderStrategy());

        input = DockerImageName.parse("kyleaure/oracle-xe:1.0.0");
        expected = DockerImageName.parse("example.com/wasliberty-infrastructure-docker/kyleaure/oracle-xe:1.0.0");
        assertEquals(expected, new ArtifactoryImageNameSubstitutor().apply(input));

        //Using local docker host
        setArtifactoryRegistryAvailable(false);
        setDockerClientStrategy(new UnixSocketClientProviderStrategy());

        expected = DockerImageName.parse("kyleaure/oracle-xe:1.0.0");
        assertEquals(expected, new ArtifactoryImageNameSubstitutor().apply(expected));

        //Using our remote docker host, but no artifactory registry set should throw exception
        setArtifactoryRegistryAvailable(false);
        setDockerClientStrategy(new EnvironmentAndSystemPropertyClientProviderStrategy());

        input = DockerImageName.parse("kyleaure/oracle-xe:1.0.0");
        try {
            new ArtifactoryImageNameSubstitutor().apply(input);
            fail("Should have thrown a RuntimeException");
        } catch (RuntimeException e) {
            //passed
        } catch (Throwable e) {
            fail("Wrong throwable caught");
        }
    }

    // Priority 5: System property artifactory.force.external.repo (NOTE: only honor this property if set to true)
    // Priority 6: If Artifactory registry is available assume user wants to use that.
    @Test
    public void testSystemPropertyModified() throws Exception {
        DockerImageName input;
        DockerImageName expected;

        //Force external does force
        setDockerClientStrategy(new UnixSocketClientProviderStrategy());
        System.setProperty(FORCE_EXTERNAL, "true");
        setArtifactoryRegistryAvailable(false);

        expected = DockerImageName.parse("openliberty:1.0.0");
        assertEquals(expected, new ArtifactoryImageNameSubstitutor().apply(expected));

        //Force external use and Artifactory is ignored
        setDockerClientStrategy(new UnixSocketClientProviderStrategy());
        System.setProperty(FORCE_EXTERNAL, "true");
        setArtifactoryRegistryAvailable(true);

        expected = DockerImageName.parse("openliberty:1.0.0");
        assertEquals(expected, new ArtifactoryImageNameSubstitutor().apply(expected));

        //No force, with Artifactory, use Artifactory
        setDockerClientStrategy(new UnixSocketClientProviderStrategy());
        System.setProperty(FORCE_EXTERNAL, "false");
        setArtifactoryRegistryAvailable(true);

        input = DockerImageName.parse("openliberty:1.0.0");
        expected = DockerImageName.parse("example.com/wasliberty-infrastructure-docker/openliberty:1.0.0");
        assertEquals(expected, new ArtifactoryImageNameSubstitutor().apply(input));

        //No force, no Artifactory, default behavior
        setDockerClientStrategy(new UnixSocketClientProviderStrategy());
        System.setProperty(FORCE_EXTERNAL, "false");
        setArtifactoryRegistryAvailable(false);

        expected = DockerImageName.parse("openliberty:1.0.0");
        assertEquals(expected, new ArtifactoryImageNameSubstitutor().apply(expected));

    }

    private static void setDockerClientStrategy(DockerClientProviderStrategy strategy) throws Exception {
        Field strategyField = DockerClientFactory.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(dockerClientFactory, strategy);
    }

    private static void setArtifactoryRegistry(String registry) throws Exception {
        Field registryField = ArtifactoryRegistry.class.getDeclaredField("registry");
        registryField.setAccessible(true);
        registryField.set(artifactoryRegistry, registry);
    }

    private static void setArtifactoryRegistryAvailable(boolean isArtifactoryAvailable) throws Exception {
        Field isArtifactoryAvailableField = ArtifactoryRegistry.class.getDeclaredField("isArtifactoryAvailable");
        isArtifactoryAvailableField.setAccessible(true);
        isArtifactoryAvailableField.set(artifactoryRegistry, isArtifactoryAvailable);
    }

    private static Method getIsSyntheticImage() throws Exception {
        Method method = ArtifactoryImageNameSubstitutor.class.getDeclaredMethod("isSyntheticImage", DockerImageName.class);
        method.setAccessible(true);
        return method;
    }

}
