/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.test.scan.samples.unused;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification_Direct_WAR;
import com.ibm.ws.annocache.test.scan.Test_Base;
import com.ibm.ws.annocache.test.utils.TestLocalization;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;

public class AcmeAnnuityWeb_AcmeAnnuityWeb_war_Data {
    public static final String EAR_NAME = "AcmeAnnuityWeb.ear";
    public static final String EAR_SIMPLE_NAME = "AcmeAnnuityWeb";

    public static final String EAR_LIB_PATH = File.separator + "lib";

    public static final String WAR_NAME = "AcmeAnnuityWeb.war";
    public static final String WAR_SIMPLE_NAME = "AcmeAnnuityWeb";

    public static final String WAR_MANIFEST_ROOT_PATH = "AcmeAnnuityWeb.war.manifest.jars";
    public static final List<String> WAR_MANIFEST_CHILD_PATHS;

    static {
        WAR_MANIFEST_CHILD_PATHS = new ArrayList<String>();

        WAR_MANIFEST_CHILD_PATHS.add("AcmeAnnuityCommon.jar");
        WAR_MANIFEST_CHILD_PATHS.add("AcmeCommon.jar");
        WAR_MANIFEST_CHILD_PATHS.add("AcmeAnnuityEJB3Stubs.jar");
        WAR_MANIFEST_CHILD_PATHS.add("AcmeAnnuityEJB2xStubs.jar");
        WAR_MANIFEST_CHILD_PATHS.add("AcmeAnnuityEJB3JAXWStubs.jar");
        WAR_MANIFEST_CHILD_PATHS.add("AcmeAnnuityEJB3JAXRPCStubs.jar");
        WAR_MANIFEST_CHILD_PATHS.add("AcmeAnnuityEJB2xJAXRPCStubs.jar");
    }

    // Special detection code for installations which can/cannot detect
    // the prefix resolver.  (Currently, Sun and Mac cannot load this class;
    // IBM can load it.)

    // That causes there to be different results in the scan results table:
    //
    // When PrefixResolver can be loaded, superclass and interfaces information is
    // available for it.  When PrefixResolver cannot be loaded, the interfaces
    // information cannot be loaded, and the details for PrefixResolver are
    // absent from the table.
    //
    // TODO: Is AcmeAnnuityWeb only valid when PrefixResolver can be loaded?
    //       The failure to load the class indicates that the application may not
    //       be valid except when the load is successful.  Alternative, the reference
    //       may be in a class which is not used.

    public static final String PREFIX_RESOLVER_CLASS_NAME = "org.apache.xml.utils.PrefixResolver";

    public static final boolean DETECTED_RESOLVER = detectResolver();

    // Use 'alt_' in front of 'cache' for storage for now, when the prefix resolver can
    // be loaded.

    public static final String RESOLVER_PREFIX = "alt_";

    public static boolean detectResolver() {
        Class<?> prefixResolverClass;

        try {
            prefixResolverClass = Class.forName(PREFIX_RESOLVER_CLASS_NAME,
                                                false, // No need to initialize it.
                                                AcmeAnnuityWeb_AcmeAnnuityWeb_war_Data.class.getClassLoader());
        } catch (Throwable th) {
            prefixResolverClass = null;
        }

        return (prefixResolverClass != null);
    }

    //

    public static ClassSourceImpl_Specification_Direct_WAR createClassSourceSpecification(ClassSourceImpl_Factory factory) {
        ClassSourceImpl_Specification_Direct_WAR warSpecification =
            factory.newWARDirectSpecification(EAR_SIMPLE_NAME, WAR_SIMPLE_NAME, Test_Base.JAVAEE_MOD_CATEGORY_NAME);

        String earPath = TestLocalization.putIntoData(EAR_NAME) + '/';

        String warPath = TestLocalization.putInto(earPath, WAR_NAME) + '/';
        warSpecification.setModulePath(warPath);

        String manifestRootPath = TestLocalization.putInto(earPath, WAR_MANIFEST_ROOT_PATH) + '/';
        List<String> manifestPaths = TestLocalization.putInto(manifestRootPath, WAR_MANIFEST_CHILD_PATHS);
        warSpecification.addManifestPaths(manifestPaths);

        warSpecification.setRootClassLoader( AcmeAnnuityWeb_AcmeAnnuityWeb_war_Data.class.getClassLoader() );

        return warSpecification;
    }
}
