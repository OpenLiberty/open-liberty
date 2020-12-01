/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.containers;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.log.Log;

/**
 * An image name substituter is configured in testcontainers.properties and will transform docker image names.
 * Here we use it to apply a private registry prefix so that in remote builds we use an internal mirror
 * of Docker Hub, instead of downloading from Docker Hub in each build which causes rate limiting issues.
 */
public class ArtifactoryImageNameSubstitutor extends ImageNameSubstitutor {

    private static final Class<?> c = ArtifactoryImageNameSubstitutor.class;

    @Override
    public DockerImageName apply(DockerImageName original) {
        if (ExternalTestServiceDockerClientStrategy.useRemoteDocker()) {
            // Using remote docker, need to substitute image name to use private registry
            String privateImage = getPrivateRegistry() + '/' + original.asCanonicalNameString();
            Log.info(c, "apply", "Swapping docker image name from " + original.asCanonicalNameString() + " --> " + privateImage);
            return DockerImageName.parse(privateImage).asCompatibleSubstituteFor(original);
        } else {
            return original;
        }
    }

    @Override
    protected String getDescription() {
        return "private artifactory registry substitutor";
    }

    private static String getPrivateRegistry() {
        String artifactoryServer = System.getProperty("fat.test.artifactory.download.server");
        if (artifactoryServer == null)
            throw new IllegalStateException("No private registry configured. System property 'fat.test.artifactory.download.server' was null.");
        if (artifactoryServer.startsWith("na.") || artifactoryServer.startsWith("eu."))
            artifactoryServer = artifactoryServer.substring(3);
        return "wasliberty-docker-remote." + artifactoryServer;
    }

}
