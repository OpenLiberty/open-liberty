/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.metagen;

/**
 * Contains configuration property keys (and some accepted values) that
 * should be used when generating the metatype.
 */
public class MetaGenConstants {
    /**
     * Key that indicates the RAR is embedded in an application.
     */
    public static final String KEY_APP_THAT_EMBEDS_RAR = "rar.embedded.in.app";
    /**
     * Key for RAR Container property (run time only)
     */
    public static final String KEY_RAR_CONTAINER = "rar.container";
    /**
     * Key for RAR Classloader property (run time only)
     */
    public static final String KEY_RAR_CLASSLOADER = "rar.classloader";
    /**
     * Key for RAR deployment descriptor property (run time only)
     */
    public static final String RAR_DEPLOYMENT_DESCRIPTOR = "rar.dd";
    /**
     * Key for suffix overrides based on both interface and implementation class
     */
    public static final String KEY_SUFFIX_OVERRIDES_BY_BOTH = "suffix.overrides.by.both";
    /**
     * Key for suffix overrides based on implementation class
     */
    public static final String KEY_SUFFIX_OVERRIDES_BY_IMPL = "suffix.overrides.by.impl";
    /**
     * Key for suffix overrides based on interface class
     */
    public static final String KEY_SUFFIX_OVERRIDES_BY_INTERFACE = "suffix.overrides.by.interface";
    /**
     * Key for whether or not to process annotations in the RAR
     */
    public static final String KEY_USE_ANNOTATIONS = "use.annotations";
    /**
     * Key for Generation Mode Property
     */
    public static final String KEY_GENERATION_MODE = "mode";
    /**
     * Value for Generation Mode (RAR)
     */
    public static final String VALUE_GENERATION_MODE_RAR = "rar";
    /**
     * Value for Generator Mode (Explicit)
     */
    public static final String VALUE_GENERATION_MODE_EXPLICIT = "explicit";
    public static final String VALUE_GENERATION_MODE_ANNOTATION = "annotation";
    /**
     * Key for Adapter Names Property
     */
    public static final String KEY_ADAPTER_NAME = "adapter.name";
    /**
     * Key for RAR Path Property
     */
    public static final String KEY_RAR_PATH = "rar.path";
    /**
     * Key for ra.xml Path Property
     */
    public static final String KEY_RA_XML_PATH = "ra.xml.path";
    /**
     * Key for wlp-ra.xml Paths Property
     */
    public static final String KEY_WLP_RA_XML_PATHS = "wlp.ra.xml.path";
    /**
     * Key for Metatype Output Path Property
     */
    public static final String KEY_METATYPE_OUTPUT_PATH = "metatype.output.path";
    /**
     * Key for Metatype Input Path Property
     */
    public static final String KEY_METATYPE_INPUT_PATH = "metatype.input.path";
    /**
     * Key for NLS Output File Path
     */
    public static final String KEY_NLS_OUTPUT_FILE = "nls.output.file";
    /**
     * Key for NLS Input File Path
     */
    public static final String KEY_NLS_INPUT_FILE = "nls.input.file";
    /**
     * Key for Translate
     */
    public static final String KEY_TRANSLATE = "translate";

    /**
     * Key for RAR module name (run time only)
     */
    public static final String KEY_MODULE_NAME = "module.name";

}
