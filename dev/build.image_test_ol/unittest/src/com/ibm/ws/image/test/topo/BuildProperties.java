/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.image.test.topo;

public class BuildProperties {
    public static final String CLASS_NAME = BuildProperties.class.getSimpleName();

    public static void log(String message) {
        System.out.println(message);
    }
    
    // e.g. 17.0.0.1 or 2017.0.0.1

    public static final String GA_VERSION_PROPERTY_NAME = "liberty.service.version";
    public static final String BETA_VERSION_PROPERTY_NAME = "liberty.beta.version";
    public static final String GA_VERSION = System.getProperty(GA_VERSION_PROPERTY_NAME);
    public static final String BETA_VERSION = System.getProperty(BETA_VERSION_PROPERTY_NAME);

    static {
        log("GA version [ " + GA_VERSION_PROPERTY_NAME + " ]: [ " + GA_VERSION + " ]");
        log("Beta version [ " + BETA_VERSION_PROPERTY_NAME + " ]: [ " + BETA_VERSION + " ]");
    }

    public static final String CREATE_IM_REPO_PROPERTY_NAME = "create.im.repo";    
    public static final boolean CREATE_IM_REPO = Boolean.getBoolean(CREATE_IM_REPO_PROPERTY_NAME);

    public static final String BUILD_LICENSE_ZIP_PROPERTY_NAME = "build.license.zip";    
    public static final boolean BUILD_LICENSE_ZIP = Boolean.getBoolean(BUILD_LICENSE_ZIP_PROPERTY_NAME);

    public static final String BUILD_ALWAYS_PROPERTY_NAME = "build.always";
    public static final boolean BUILD_ALWAYS = Boolean.getBoolean(BUILD_ALWAYS_PROPERTY_NAME);

    public static final String IFIX_VERSION_BASE_PROPERTY_NAME = "ifix.version.base";    
    public static final String IFIX_VERSION_BASE = System.getProperty(IFIX_VERSION_BASE_PROPERTY_NAME);
    public static final boolean IFIX_BUILD = ( IFIX_VERSION_BASE != null );
    
    static {
        log("Create IM repository [ " + CREATE_IM_REPO_PROPERTY_NAME + " ]: [ " + CREATE_IM_REPO + " ]");
        log("Build license zip [ " + BUILD_LICENSE_ZIP_PROPERTY_NAME + " ]: [ " + BUILD_LICENSE_ZIP + " ]");
        log("Build always [ " + BUILD_ALWAYS_PROPERTY_NAME + " ]: [ " + BUILD_ALWAYS + " ]");
        log("IFIX version base [ " + IFIX_VERSION_BASE_PROPERTY_NAME + " ]: [ " + IFIX_VERSION_BASE + " ]");
    }
}
