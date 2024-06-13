/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.util;

import static com.ibm.ws.kernel.feature.internal.util.ImageXMLConstants.*;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//Source restricted to java7.

public class ImageXML extends BaseXML {
    /**
     * Sort a list of images based on their names.
     *
     * Ignore case.
     *
     * @param images Images which are to be sorted.
     */
    public static void sort(List<ImageInfo> images) {
        Collections.sort(images, COMPARE);
    }

    private static Comparator<ImageInfo> COMPARE = new Comparator<ImageInfo>() {
        /**
         * Compare two images by their name.
         *
         * Use a case insensitive comparison.
         *
         * @param i0 Image information which is to be compared.
         * @param i1 Other image information Infoinition which is to be compared.
         *
         * @return The images compared by their name name.
         */
        @Override
        public int compare(ImageInfo i0, ImageInfo i1) {
            return i0.getName().compareToIgnoreCase(i1.getName());
        }
    };

    public static void write(File file, final Images images) throws Exception {
        write(file, new FailableConsumer<PrintWriter, Exception>() {
            @Override
            public void accept(PrintWriter pW) throws Exception {
                @SuppressWarnings("resource")
                ImagesXMLWriter xW = new ImagesXMLWriter(pW);
                try {
                    xW.write(images);
                } finally {
                    xW.flush();
                }
            }
        });
    }

    public static class ImagesXMLWriter extends BaseXMLWriter {
        public ImagesXMLWriter(PrintWriter pW) {
            super(pW);
        }

        public void write(Images images) {
            openElement(XML_OUTPUT_IMAGES);
            upIndent();

            for ( ImageInfo image : images.getImages().values() ) {
                write(image);
            }

            downIndent();
            closeElement(XML_OUTPUT_IMAGES);
        }

        public void write(ImageInfo image) {
            openElement(XML_OUTPUT_IMAGE);
            upIndent();

            printElement(XML_OUTPUT_NAME, image.getName());
            for ( String featureName : image.getFeatures() ) {
                printElement(XML_OUTPUT_FEATURE, featureName);
            }

            downIndent();
            closeElement(XML_OUTPUT_IMAGE);
        }
    }
}
