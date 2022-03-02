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

    public static final String[][] VERSION_EXPECTATIONS = new String[][] {
        new String[] { "Product name:", "Open Liberty" },
        new String[] { "Product version:", BuildProperties.GA_VERSION },
        new String[] { "Product editions:", "Open" }
    };
    
    // TODO: Is there a better way to know which features should be present?
    //       Having these be hard coded is painful.

    public static final String[] DEFAULT_REQUIRED_FEATURES = { ".*<feature>jsp-2.3</feature>.*" };

    public static final String[] JAVAEE8_REQUIRED_FEATURES = {  ".*<feature>javaee-8.0</feature>.*" };
    public static final String[] WEBPROFILE8_REQUIRED_FEATURES = { ".*<feature>webProfile-8.0</feature>.*" };
    public static final String[] MICROPROFILE4_REQUIRED_FEATURES = { ".*<feature>microProfile-4.1</feature>.*" };
    
    public static final String[] JAKARTAEE9_REQUIRED_FEATURES = { ".*<feature>jakartaee-9.1</feature>.*" };
    public static final String[] WEBPROFILE9_REQUIRED_FEATURES = { ".*<feature>webProfile-9.1</feature>.*" };
    public static final String[] MICROPROFILE5_REQUIRED_FEATURES = { ".*<feature>microProfile-5.0</feature>.*" };

    public static final String[][] REQUIRED_FEATURES = {
            DEFAULT_REQUIRED_FEATURES,
            DEFAULT_REQUIRED_FEATURES,
            DEFAULT_REQUIRED_FEATURES,
            DEFAULT_REQUIRED_FEATURES,
            
            DEFAULT_REQUIRED_FEATURES,

            JAVAEE8_REQUIRED_FEATURES,
            WEBPROFILE8_REQUIRED_FEATURES,
            MICROPROFILE4_REQUIRED_FEATURES,

            JAKARTAEE9_REQUIRED_FEATURES,
            WEBPROFILE9_REQUIRED_FEATURES,
            MICROPROFILE5_REQUIRED_FEATURES
    };

    // The kernel image cannot be started:
    //
    // java.lang.AssertionError: Disallowed console messages [
    //     C:/dev/repos-pub/o-l/dev/openliberty.build.image_fat/build/test/iServer/
    //     OPEN_LIBERTY_KERNEL/wlp/usr/servers/defaultServer/logs/console.log ]:
    // [ 1 ]:  [ [ERROR   ] CWWKF0001E: A feature definition could not be found for jsp-2.3 ]

    // Redundant with the extra messages code.
    // Retaining for future use.

    public static final boolean[] SUPPORTS_DEFAULT_STARTUP = {
          true, true, true, true,
          false, // Temporarily disabled: See issue #20248
          true, true, true,
          true, true, true
    };
    
    // See open-liberty issue #20239:
    // Problems schema validating default configurations 
    //
    // Several profile default server configurations fail
    // schema validation.
    //
    // The following error occurs:
    //
    // org.xml.sax.SAXParseException:
    // cos-nonambig: contextService and WC[##any]
    // (or elements from their substitution group)
    // violate "Unique Particle Attribution". During
    // validation against this schema, ambiguity would be
    // created for those two particles.

    public static final boolean[] SUPPORTS_SCHEMAGEN = {
          true, true, true, true,
          false,
          false, true, false,
          false, true, false
    };
    
    // Extra allowed messages:
    //
    // [2/22/22 18:06:24:224 EST] 00000024
    // com.ibm.ws.security.registry.basic.internal.BasicRegistry W
    // CWWKS3103W: There are no users defined for the BasicRegistry
    // configuration of ID com.ibm.ws.security.registry.basic.config[basic].

    public static final String NO_USERS_MESSAGE = "CWWKS3103W";

    public static final String[] EXTRA_CONSOLE_MESSAGE = {
          null, null, null, null,
          null,
          NO_USERS_MESSAGE, null, null,
          NO_USERS_MESSAGE, null, null
    };
    
    static {
        List<Object[]> testData = new ArrayList<Object[]>(SERVER_IMAGES.length);

        for ( int imageNo = 0; imageNo < SERVER_IMAGES.length; imageNo++ ) {
            BuildImages.ImageType imageType = SERVER_IMAGES[imageNo];
            String[][] versionExpectations = VERSION_EXPECTATIONS;
            String[] requiredFeatures = REQUIRED_FEATURES[imageNo];
            boolean supportsDefaultStartup = SUPPORTS_DEFAULT_STARTUP[imageNo];
            boolean supportsSchemaGen = SUPPORTS_SCHEMAGEN[imageNo];
            String extraConsoleMessage = EXTRA_CONSOLE_MESSAGE[imageNo];
            
            testData.add( new Object[] {
                    imageType,
                    versionExpectations,
                    requiredFeatures,
                    supportsDefaultStartup,
                    supportsSchemaGen,
                    extraConsoleMessage } );
        }

        TEST_DATA = testData;
    }
}
