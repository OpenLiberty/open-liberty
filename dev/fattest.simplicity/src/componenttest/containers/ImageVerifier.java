/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.containers;

import java.util.Arrays;
import java.util.HashSet;

import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.database.container.DatabaseContainerType;

/**
 * This class will verify that the test suite project has configured the fat.test.container.images
 * property in bnd.bnd or test-suite.xml
 */
public final class ImageVerifier {
    static final Class<?> c = ImageVerifier.class;

    static final HashSet<String> forgottenImages = new HashSet<>();
    static final HashSet<String> knownImages = new HashSet<>();
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
        knownImages.addAll(Arrays.asList("testcontainers/ryuk:0.3.3"));

        Log.info(c, "<init>", "KJA1017 knownImages: " + knownImages.toString());
    }

    public static DockerImageName collectImage(DockerImageName image) {
        Log.info(c, "collectImage", "KJA1017 image: " + image.asCanonicalNameString());
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

        IllegalStateException e = new IllegalStateException("Used testcontainer image(s) " + forgottenImages.toString() +
                                                            " were not found in the " + imageProperty + " property!" +
                                                            " To correct this, add " + imageProperty + ": " + forgottenImages.toString().replace("[", "").replace("]", "") +
                                                            " to the bnd.bnd file for this FAT so that a testcontainer image" +
                                                            " graph can be generated in the future.");
        Log.error(c, "assertImages", e);
        throw e;
    }
}
