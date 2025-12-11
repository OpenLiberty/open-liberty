/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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

package com.ibm.ws.kernel.feature.internal.util;

import static com.ibm.ws.kernel.feature.internal.util.ImageXMLConstants.XML_INPUT_FEATURE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ImageReader {

    public static final String FEATURES_FILE_NAME = "features.xml";

    /**
     * Determine image directories as children of
     * a root images directory.
     *
     * @param rootFile The root of the images directories.
     *
     * @return Child image directories.
     */
    public static List<File[]> selectImageDirs(File rootFile) {
        File[] imageFiles = rootFile.listFiles();

        List<File[]> imageDirs = new ArrayList<>(imageFiles.length);
        for (File imageFile : imageFiles) {
            if (imageFile.isDirectory()) {
                File featuresFile = new File(imageFile, FEATURES_FILE_NAME);
                if (featuresFile.exists()) {
                    imageDirs.add(new File[] { imageFile, featuresFile });
                }
            }
        }

        return imageDirs;
    }

    public static Images readImages(Collection<File[]> imageDirs) throws IOException {
        Images images = new Images(imageDirs.size());

        for (File[] imageFiles : imageDirs) {
            File imageDir = imageFiles[0];
            File imageFile = imageFiles[1];

            String imageName = imageDir.getName();
            List<String> features = readFeatures(imageFile);
            images.put(new ImageInfo(imageName, features));
        }

        return images;
    }

    public static final String XML_INPUT_FEATURE_OPEN = "<" + XML_INPUT_FEATURE + ">";
    public static final String XML_INPUT_FEATURE_CLOSE = "</" + XML_INPUT_FEATURE + ">";

    public static List<String> readFeatures(File imageFile) throws IOException {
        List<String> features = new ArrayList<>();

        try (InputStream inputStream = new FileInputStream(imageFile);
                        InputStreamReader baseReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                        BufferedReader reader = new BufferedReader(baseReader)) {

            String nextLine;
            while ((nextLine = reader.readLine()) != null) {
                nextLine = nextLine.trim();
                if (nextLine.isEmpty()) {
                    continue;
                }
                if (!nextLine.startsWith(XML_INPUT_FEATURE_OPEN)) {
                    continue;
                } else if (!nextLine.endsWith(XML_INPUT_FEATURE_CLOSE)) {
                    continue;
                }

                int startPos = XML_INPUT_FEATURE_OPEN.length();
                int endPos = nextLine.length() - XML_INPUT_FEATURE_CLOSE.length();
                String feature = nextLine.substring(startPos, endPos);

                features.add(feature);
            }
        }

        return features;
    }
}
