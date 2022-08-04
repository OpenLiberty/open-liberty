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

import componenttest.topology.database.container.DatabaseContainerType;

/**
 * This class will verify that the test suite project has configured the fat.test.container.images
 * property in bnd.bnd or test-suite.xml
 */
public class ImageVerifier {

    private static final HashSet<String> knownImages = new HashSet<>();
    public static final String imageProperty = "fat.test.container.images";

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
    }

    public static boolean hasImage(DockerImageName image) {
        return knownImages.contains(image.asCanonicalNameString());
    }

    public static boolean hasImage(String image) {
        return knownImages.contains(image);
    }

}
