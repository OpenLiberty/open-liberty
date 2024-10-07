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
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;

/**
 * This class will verify that the test suite project has configured the fat.test.container.images
 * property in bnd.bnd or test-suite.xml
 */
public final class ImageVerifier {
    static final Class<?> c = ImageVerifier.class;

    static final Set<DockerImageName> forgottenImages = ConcurrentHashMap.newKeySet();
    static final Set<DockerImageName> knownImages;

    static final String imageProperty = "fat.test.container.images";

    static {
        String images = System.getProperty(imageProperty);
        String[] imageList = images == null ? new String[] {} : images.replaceAll("\\s", "").split(",");

        final Set<DockerImageName> expectedImages = ConcurrentHashMap.newKeySet();

        //Collect the set of images set in <test-bucket>/bnd.bnd
        for (String image : imageList) {
            expectedImages.add(DockerImageName.parse(image));
        }

        //Add images from the fattest.simplicity project (tracked in fattest.simplicity/bnd.bnd)
        for (String image : DatabaseContainerType.images()) {
            expectedImages.add(DockerImageName.parse(image));
        }

        //Add images from the testcontainers project (tracked in fattest.simplicity/bnd.bnd)
        for (String image : Arrays.asList("testcontainers/ryuk:0.9.0", "testcontainers/sshd:1.2.0", "testcontainers/vnc-recorder:1.3.0", "alpine:3.17")) {
            expectedImages.add(DockerImageName.parse(image));
        }

        knownImages = Collections.unmodifiableSet(expectedImages);
    }

    public static DockerImageName collectImage(DockerImageName image) {
        return collectImage(image, null);
    }

    public static DockerImageName collectImage(DockerImageName image, DockerImageName output) {
        if (!knownImages.contains(image)) {
            Log.info(c, "collectImage", "Found an undocumented image: " + image);
            forgottenImages.add(image);
        }

        return output != null ? output : image;
    }

    public static void assertImages() throws IllegalStateException {
        if (forgottenImages.isEmpty())
            return;

        String knownImagesNeedUpdate = "";

        // Find compatible known images that are out of date, likely an image was updated by the testcontainer library that we have to update.
        for (DockerImageName image : forgottenImages) {
            Optional<DockerImageName> needsUpdate = knownImages.stream().filter(i -> updateImageVersionRequired(i, image)).findFirst();
            if (needsUpdate.isPresent()) {
                knownImagesNeedUpdate += "A testcontainer image used an unknown version. Update " + needsUpdate.get() + " to " + image
                                         + " in the fattest.simplicity source code and/or the " + imageProperty
                                         + " property of the fattest.simplicity/bnd.bnd file";
                forgottenImages.remove(image);
            }

        }

        if (!knownImagesNeedUpdate.isEmpty()) {
            if (FATRunner.FAT_TEST_LOCALRUN) {
                // Throw a failure here during local run so it gets updated
                throw new RuntimeException(knownImagesNeedUpdate);
            } else {
                // Only throw a warning here, so that OL and WL can be updated asynchronously
                Log.warning(c, knownImagesNeedUpdate);
            }
        }

        if (forgottenImages.isEmpty())
            return;

        IllegalStateException e = new IllegalStateException("Used testcontainer image(s) " + forgottenImages +
                                                            " were not found in the " + imageProperty + " property!" +
                                                            " To correct this, add " + imageProperty + ": " + forgottenImages.toString().replace("[", "").replace("]", "") +
                                                            " to the bnd.bnd file for this FAT so that a testcontainer image" +
                                                            " graph can be generated in the future.");
        Log.error(c, "assertImages", e);
        throw e;
    }

    /**
     * Returns true iff the two images are equal except for their version
     */
    private static boolean updateImageVersionRequired(DockerImageName a, DockerImageName b) {
        if (a.equals(b))
            return false;

        return a.getUnversionedPart().equals(b.getUnversionedPart());
    }
}
