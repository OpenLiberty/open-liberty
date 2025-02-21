/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    static final Set<DockerImageName> expectedImages;

    static final String imageProperty = "fat.test.container.images";

    static {
        String images = System.getProperty(imageProperty);
        String[] imageList = images == null ? new String[] {} : images.replaceAll("\\s", "").split(",");

        final Set<DockerImageName> _expectedImages = ConcurrentHashMap.newKeySet();

        //Collect the set of images set in <test-bucket>/bnd.bnd
        for (String image : imageList) {
            _expectedImages.add(DockerImageName.parse(image));
        }

        //Add images from the fattest.simplicity project (tracked in fattest.simplicity/bnd.bnd)
        for (String image : DatabaseContainerType.images()) {
            _expectedImages.add(DockerImageName.parse(image));
        }

        //Add images from the testcontainers project (tracked in fattest.simplicity/bnd.bnd)
        for (String image : Arrays.asList("testcontainers/ryuk:0.9.0", "testcontainers/sshd:1.2.0", "testcontainers/vnc-recorder:1.3.0",
                                          "public.ecr.aws/docker/library/alpine:3.17")) {
            _expectedImages.add(DockerImageName.parse(image));
        }

        expectedImages = Collections.unmodifiableSet(_expectedImages);
    }

    public static void collectImage(DockerImageName image) {
        if (!expectedImages.contains(image)) {
            Log.info(c, "collectImage", "Found an unknown image: " + image);
            forgottenImages.add(image);
        }
    }

    public static void assertImages() throws IllegalStateException {
        if (forgottenImages.isEmpty())
            return;

        warnOrThrow(String.join(System.lineSeparator(), compareImages(expectedImages, forgottenImages)));

        if (forgottenImages.isEmpty())
            return;

        String formattedImageList = forgottenImages.toString().replace("[", "").replace("]", "");
        String message = "The testcontainer image(s) " + forgottenImages +
                         " were not found in the " + imageProperty + " property!" +
                         " To correct this, add " + imageProperty + ": " + formattedImageList +
                         " to the bnd.bnd file, or add <property name=\"" + imageProperty + "\" value=\"" + formattedImageList + "\" />" +
                         " to the build-test.xml file for this FAT so that a testcontainer image" +
                         " graph can be generated in the future.";

        IllegalStateException e = new IllegalStateException(message);
        Log.error(c, "assertImages", e);
        throw e;
    }

    /**
     * Compares the set of declared images against the collected set of forgotten images.
     * Finds and removes declared images from the forgotten image set where the developer forgot to update the version.
     * This is a common error path where a developer forgets to update to a newer version.
     *
     * @param  expected  - set of declared expected images - from bnd/build property
     * @param  forgotten - set of collected forgotten images - from source code
     * @return           - A well formatted message to be output to the developer
     */
    private static List<String> compareImages(Set<DockerImageName> expected, Set<DockerImageName> forgotten) {
        List<String> knownImagesNeedUpdate = new ArrayList<>();
        List<DockerImageName> imagesToRemove = new ArrayList<>();

        for (DockerImageName forgot : forgotten) {
            List<DockerImageName> known = expected.stream()
                            .filter(i -> updateImageVersionRequired(i, forgot))
                            .collect(Collectors.toList());

            if (known.isEmpty()) {
                continue;
            }

            String[] knownLocations = new String[] { "the " + imageProperty + " of the fattest.simplicity bnd.bnd/build-noship.xml file",
                                                     "the " + imageProperty + " of the test project bnd.bnd/build.xml file" };
            String[] forgotLocations = new String[] { "fattest.simplicity source code",
                                                      "test project source code" };

            if (known.size() == 1) {
                knownImagesNeedUpdate.add("The testcontainer image '" + forgot + "' used an unknown version."
                                          + " Update '" + known.get(0) + "' to '" + forgot + "' in either " + String.join(" or ", knownLocations) + "."
                                          + " Or update '" + forgot + "' to '" + known.get(0) + "' in either " + String.join(" or ", forgotLocations) + ".");
            } else {
                knownImagesNeedUpdate.add("The testcontainer image '" + forgot + "' used an unknown version."
                                          + " Update one of " + known + " to '" + forgot + "' in either " + String.join(" or ", knownLocations) + "."
                                          + " Or update '" + forgot + "' to one of " + known + " in either " + String.join(" or ", forgotLocations) + ".");
            }

            imagesToRemove.add(forgot);
        }

        forgotten.removeAll(imagesToRemove);
        return knownImagesNeedUpdate;
    }

    /**
     * Performs no action if the provided message is null or empty
     *
     * Throws a RuntimeException with provided message when running locally
     * to ensure failures are fixed.
     *
     * Logs a warning with provided message when running during build
     * so that OL and WL can be updated asynchronously.
     *
     * @param message - the message to either ignore, warn, or throw
     */
    private static void warnOrThrow(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        if (FATRunner.FAT_TEST_LOCALRUN) {
            throw new RuntimeException(message);
        } else {
            Log.warning(c, message);
        }
    }

    /**
     * Returns true iff the two images are the same except for their version
     *
     * @param  a - A docker image name
     * @param  b - Another docker image name
     * @return   - true if version needs update, false otherwise.
     */
    private static boolean updateImageVersionRequired(DockerImageName a, DockerImageName b) {
        if (a.equals(b))
            return false;

        return a.getUnversionedPart().equals(b.getUnversionedPart());
    }
}
