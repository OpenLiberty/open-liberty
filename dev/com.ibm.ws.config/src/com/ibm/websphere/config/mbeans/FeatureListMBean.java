/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.config.mbeans;

import java.util.Map;

/**
 * This MBean allows users to generate the feature list pertaining to the Liberty server.
 * <p>
 * The ObjectName for this MBean is {@value #OBJECT_NAME}.
 * <p>
 * 
 * @ibm-api
 */
public interface FeatureListMBean {

    /**
     * A String representing the {@link javax.management.ObjectName} that this MBean maps to.
     */
    public static final String OBJECT_NAME = "WebSphere:name=com.ibm.websphere.config.mbeans.FeatureListMBean";

    /**
     * Key for the merged (systemOut and systemErr) output of the feature list generation. The value
     * is a java.lang.String
     */
    public static final String KEY_OUTPUT = "keySystemOut";

    /**
     * Key for the return code of the feature list generation. The value
     * is an integer that will be either {@link #RETURN_CODE_OK} or {@link #RETURN_CODE_ERROR}.
     */
    public static final String KEY_RETURN_CODE = "keyReturnCode";

    /**
     * Key for the file path of the generated feature list. The value
     * is a java.lang.String and represents the absolute location of the generated feature list in the server's file system.
     */
    public static final String KEY_FILE_PATH = "keyFilePath";

    /**
     * Value of {@link #KEY_RETURN_CODE} when the schema generation is successful.
     */
    public static final int RETURN_CODE_OK = 0;

    /**
     * Value of {@link #KEY_RETURN_CODE} when an error is encountered during schema generation.
     */
    public static final int RETURN_CODE_ERROR = 21;

    /**
     * Creates a local XML file containing the server feature list, generated with the given options. It is
     * the responsibility of the user to delete this generated file after it is used, since the server will only delete
     * it during the next server startup.
     * 
     * @param encoding a String that specifies the character set to use when writing the feature list xml file.
     * @param locale a String which specifies the language to use when writing the feature list. This consists of the ISO-639 two-letter lowercase language code, optionally
     *            followed by an underscore and the ISO-3166 uppercase two-letter country code.
     * @param productExt a String which specifies the product extension name whose features are to be listed. If the product extension is installed in the default user location,
     *            use the keyword: usr. If this option is not specified, the action is taken on Liberty core.
     * @return a map containing keys {@link #KEY_OUTPUT}, {@link #KEY_RETURN_CODE}, and {@link #KEY_FILE_PATH}
     */
    public Map<String, Object> generate(String encoding, String locale, String productExt);

}
