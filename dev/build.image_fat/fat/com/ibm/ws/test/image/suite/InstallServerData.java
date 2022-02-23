/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.image.suite;

import static com.ibm.ws.test.image.build.BuildImages.ImageType;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.test.image.build.BuildProperties;
import com.ibm.ws.test.image.build.BuildImages;

public class InstallServerData {
    protected static final List<Object[]> TEST_DATA;

    public static final BuildImages.ImageType[] SERVER_IMAGES = {
            ImageType.OPEN_LIBERTY,
            ImageType.OPEN_LIBERTY_BETA,
            ImageType.OPEN_LIBERTY_ALL,
            ImageType.OPEN_LIBERTY_DEV,
    
            ImageType.OPEN_LIBERTY_KERNEL,

            ImageType.OPEN_LIBERTY_JAVAEE8,
            ImageType.OPEN_LIBERTY_WEB8,
            ImageType.OPEN_LIBERTY_MICRO8,

            ImageType.OPEN_LIBERTY_JAKARTAEE9,
            ImageType.OPEN_LIBERTY_WEB9,
            ImageType.OPEN_LIBERTY_MICRO9
    };

    public static String[][] VERSION_EXPECTATIONS = new String[][] {
        new String[] { "Product name:", "Open Liberty" },
        new String[] { "Product version:", BuildProperties.GA_VERSION },
        new String[] { "Product editions:", "Open" }
    };
    
    public static String[] REQUIRED_FEATURES = { ".*<feature>jsp-2.3</feature>.*" };    
    
    static {
        List<Object[]> testData = new ArrayList<Object[]>(SERVER_IMAGES.length);

        for ( BuildImages.ImageType imageType : SERVER_IMAGES ) {
            testData.add( new Object[] { imageType, VERSION_EXPECTATIONS, REQUIRED_FEATURES } );
        }

        TEST_DATA = testData;
    }
}
