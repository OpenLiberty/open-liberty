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

package com.ibm.ws.config.schemagen.internal;

import java.util.regex.Pattern;

interface SchemaGenConstants {
    /**
     * Strings for trace and nls messages (for those classes w/in the bundle that
     * use Tr)
     */
    String TR_GROUP = "config";
    String NLS_PROPS = "com.ibm.ws.config.internal.resources.ConfigMessages";

    /** Property identifying prefix of configuration key */
    String CFG_CONFIG_PREFIX = "config.";

    /** Property defines an attribute to identify an instance id for factory-based configuration */
    String CFG_INSTANCE_ID = "id";

    /** This is the URI for the ibm: namespace we use for IBM extensions to metatype. */
    String METATYPE_EXTENSION_URI = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0";

    /** This is the URI for the ibmui: namespace we use for IBM UI extensions to metatype. */
    String METATYPE_UI_EXTENSION_URI = "http://www.ibm.com/xmlns/appservers/osgi/metatype/ui/v1.0.0";

    String CFG_REFERENCE_SUFFIX = "Ref";

    String UNIQUE_PREFIX = "UNIQUE_";
    String VAR_IN_USE = "WLP_VAR_IN_USE";

    String VAR_OPEN = "${";
    String VAR_CLOSE = "}";
    Pattern COMMA_PATTERN = Pattern.compile("\\s*,\\s*");

    /** Used to prefix a kernel bundle location. */
    String BUNDLE_LOC_KERNEL_TAG = "kernel@";

    /** Used to prefix a feature bundle location. */
    String BUNDLE_LOC_FEATURE_TAG = "feature@";

    /** Bundle location product extension tag. */
    String BUNDLE_LOC_PROD_EXT_TAG = "productExtension:";
}
