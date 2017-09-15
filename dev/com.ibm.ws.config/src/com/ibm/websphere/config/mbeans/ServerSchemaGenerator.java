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
 * This MBean allows callers to generate the schema pertaining to server.xml.
 * 
 * <p>
 * The ObjectName for this MBean is {@value #OBJECT_NAME}.
 * <p>
 * 
 * @ibm-api
 * 
 */
public interface ServerSchemaGenerator {

    /**
     * A String representing the {@link javax.management.ObjectName} that this MXBean maps to.
     */
    public static final String OBJECT_NAME = "WebSphere:name=com.ibm.ws.config.serverSchemaGenerator";

    /**
     * Key for the return code of the schema generation. The value
     * is an integer that will be either {@link #RETURN_CODE_OK} or {@link #RETURN_CODE_ERROR}.
     */
    public static final String KEY_RETURN_CODE = "keyReturnCode";

    /**
     * Key for the merged (systemOut and systemErr) output of the schema generation. The value
     * is a java.lang.String
     */
    public static final String KEY_OUTPUT = "keyOutput";

    /**
     * Key for the file path of the generated schema. The value
     * is a java.lang.String and represents the absolute location of the generated schema in the server's file system.
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
     * Generates a schema for the current runtime bundles. This is often not the desired output, since it only includes the currently
     * load bundles. Users that are interested in the schema for the full set of installed features should use {@link #generateInstallSchema(String, String, String, String)}.
     * 
     * @return a String containing the generated schema for the current runtime.
     */
    public String generate();

    /**
     * Creates a local file containing a schema for the current runtime bundles, generated with the given options. It is
     * the responsibility of the user to delete this generated file after it is used, since the server will only delete
     * it during the next server startup.
     * 
     * @param schemaVersion a string that indicates the schema version of the generated schema
     * @param outputVersion a string that indicates the output version of the generated schema
     * @param encoding a string that indicates the encoding to be used during generation
     * @param locale a string that indicates the locale to be used during generation
     * @return a map containing keys {@link #KEY_RETURN_CODE}, {@link #KEY_OUTPUT} and {@link #KEY_FILE_PATH}
     */
    public Map<String, Object> generateServerSchema(String schemaVersion, String outputVersion, String encoding, String locale);

    /**
     * Creates a local file containing a schema for the installed features, generated with the given options. It is
     * the responsibility of the user to delete this generated file after it is used, since the server will only delete
     * it during the next server startup.
     * 
     * @param schemaVersion a string that indicates the schema version of the generated schema
     * @param outputVersion a string that indicates the output version of the generated schema
     * @param encoding a string that indicates the encoding to be used during generation
     * @param locale a string that indicates the locale to be used during generation
     * @return a map containing keys {@link #KEY_RETURN_CODE}, {@link #KEY_OUTPUT} and {@link #KEY_FILE_PATH}
     */
    public Map<String, Object> generateInstallSchema(String schemaVersion, String outputVersion, String encoding, String locale);

    /**
     * Creates a local file containing a schema for the installed features, generated with the given options. It is
     * the responsibility of the user to delete this generated file after it is used, since the server will only delete
     * it during the next server startup.
     * 
     * @param schemaVersion a string that indicates the schema version of the generated schema
     * @param outputVersion a string that indicates the output version of the generated schema
     * @param encoding a string that indicates the encoding to be used during generation
     * @param locale a string that indicates the locale to be used during generation
     * @param compactOutput a boolean value that indicates if the schema output will contain any indenting white spaces, new line feeds or XML comments
     * @return a map containing keys {@link #KEY_RETURN_CODE}, {@link #KEY_OUTPUT} and {@link #KEY_FILE_PATH}
     */
    public Map<String, Object> generateInstallSchema(String schemaVersion, String outputVersion, String encoding, String locale, boolean compactOutput);

}
