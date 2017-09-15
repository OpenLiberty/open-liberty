/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal;

import java.util.regex.Pattern;

public interface XMLConfigConstants {
    /**
     * Strings for trace and nls messages (for those classes w/in the bundle that
     * use Tr)
     */
    String NLS_PROPS = "com.ibm.ws.config.internal.resources.ConfigMessages", TR_GROUP = "config";
    String NLS_OPTIONS = "com.ibm.ws.config.internal.resources.ConfigOptions";

    String FEATURE_CHANGING_TOPIC = "com/ibm/ws/kernel/feature/internal/FeatureManager/FEATURE_CHANGING";
    String FEATURE_CHANGE_TOPIC = "com/ibm/ws/kernel/feature/internal/FeatureManager/FEATURE_CHANGE";

    /** Possible config source values */
    String CFG_CONFIG_SOURCE_FILE = "file";

    /** Property identifying source of configuration update */
    String CFG_CONFIG_SOURCE = "config.source";

    /** Property identifying prefix of configuration key */
    String CFG_CONFIG_PREFIX = "config.";

    /** Property identifying the service prefix */
    String CFG_SERVICE_PREFIX = "service.";

    String CFG_SERVICE_PID = "service.pid";

    /** Internal property identifying an instance id for factory-based configuration */
    String CFG_CONFIG_INSTANCE_ID = CFG_CONFIG_PREFIX + "id";

    /** Internal property identifying config references */
    String CFG_CONFIG_REFERENCES = CFG_CONFIG_PREFIX + "references";

    /** Property defines an attribute to identify an instance id for factory-based configuration */
    String CFG_INSTANCE_ID = "id";

    char INSTANCE_DELIMITER = '-';

    String CFG_CONFIG_REF = "ref";

    /** This is the URI for the ibm: namespace we use for IBM extensions to metatype. */
    String METATYPE_EXTENSION_URI = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0";

    /** This is the URI for the ibmui: namespace we use for IBM UI extensions to metatype. */
    String METATYPE_UI_EXTENSION_URI = "http://www.ibm.com/xmlns/appservers/osgi/metatype/ui/v1.0.0";

    String CFG_REFERENCE_SUFFIX = "Ref";

    String DEFAULT_CONFIG_HEADER = "IBM-Default-Config";

    String CONFIG_ENABLED_ATTRIBUTE = "configurationEnabled";

    String VAR_OPEN = "${";
    String VAR_CLOSE = "}";
    Pattern VAR_PATTERN = Pattern.compile("\\Q" + VAR_OPEN + "\\E(.+?)\\Q" + VAR_CLOSE + "\\E");
    Pattern COMMA_PATTERN = Pattern.compile("\\s*,\\s*");
    Pattern PARENTHESIS_PATTERN = Pattern.compile("\\s*;\\s*");
    Pattern EQUALS_PATTERN = Pattern.compile("\\s=\\s*");

    String CFG_PARENT_PID = "config.parentPID";

    /** Used to prefix a kernel bundle location. */
    String BUNDLE_LOC_KERNEL_TAG = "kernel@";

    /** Used to prefix a feature bundle location. */
    String BUNDLE_LOC_FEATURE_TAG = "feature@";

    /** Used to prefix a connector module metatype bundle location. */
    String BUNDLE_LOC_CONNECTOR_TAG = "ConnectorModuleMetatype@";

    /** Bundle location reference tag. */
    String BUNDLE_LOC_REFERENCE_TAG = "reference:";

    /** Bundle location product extension tag. */
    String BUNDLE_LOC_PROD_EXT_TAG = "productExtension:";

    /** Product name property that identifies the core Liberty product. */
    String CORE_PRODUCT_NAME = "core";

    /** Display ID attribute */
    String CFG_CONFIG_INSTANCE_DISPLAY_ID = "config.displayId";
}
