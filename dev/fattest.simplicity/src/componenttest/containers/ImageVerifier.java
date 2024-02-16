/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.database.container.DatabaseContainerType;

/**
 * This class will verify that the test suite project has configured the fat.test.container.images
 * property in bnd.bnd or test-suite.xml
 */
public final class ImageVerifier {
    static final Class<?> c = ImageVerifier.class;

    static final Set<String> forgottenImages = ConcurrentHashMap.newKeySet();
    static final Set<String> knownImages = ConcurrentHashMap.newKeySet();

    static final String imageProperty = "fat.test.container.images";

    static {
        String images = System.getProperty(imageProperty);

        if (images != null) {
            //Add images from the test project
            knownImages.addAll(Arrays.asList(images.replaceAll("\\s", "").split(",")));
        }

        //Add images from the fattest.simplicity project
        knownImages.addAll(DatabaseContainerType.images());

        //Add images from the testcontainers project
        knownImages.addAll(Arrays.asList("testcontainers/ryuk:0.6.0", "testcontainers/sshd:1.1.0", "testcontainers/vnc-recorder:1.3.0"));
    }

    public static DockerImageName collectImage(DockerImageName image) {
        return collectImage(image, null);
    }

    public static DockerImageName collectImage(DockerImageName image, DockerImageName output) {
        if (!knownImages.contains(image.asCanonicalNameString())) {
            Log.info(c, "collectImage", "Found an undocumented image: " + image.asCanonicalNameString());
            forgottenImages.add(image.asCanonicalNameString());
        }

        return output != null ? output : image;
    }

    public static void assertImages() throws IllegalStateException {
        if (forgottenImages.isEmpty())
            return;

        //Prevent a build break because of a testcontainer image update.
        //This will allow us to update OL and WL asynchronously in the future.
        for (String image : forgottenImages) {
            if (image.startsWith("testcontainers")) {
                Log.warning(c,
                            "A testcontainer image used an unknown version."
                               + " This means the version of testcontainers in Open Liberty was updated and WebSphere Liberty needs to be updated,"
                               + " or WebSphere Liberty has already been updated and the version of testcontainers in Open Liberty needs to be updated. "
                               + " Image: " + image);
                forgottenImages.remove(image);
            }
        }

        if (forgottenImages.isEmpty())
            return;

        IllegalStateException e = new IllegalStateException("Used testcontainer image(s) " + forgottenImages.toString() +
                                                            " were not found in the " + imageProperty + " property!" +
                                                            " To correct this, add " + imageProperty + ": " + forgottenImages.toString().replace("[", "").replace("]", "") +
                                                            " to the bnd.bnd file for this FAT so that a testcontainer image" +
                                                            " graph can be generated in the future.");
        Log.error(c, "assertImages", e);
        throw e;
    }
}
