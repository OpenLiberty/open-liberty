/**
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.microprofile.openapi;

/**
 * Configurable properties in MicroProfile OpenAPI
 */
public final class OASConfig {

    private OASConfig(){}

    /**
     * Configuration property to specify the fully qualified name of the OASModelReader implementation.
     * 
     * @see org.eclipse.microprofile.openapi.OASModelReader
     */
    public static final String MODEL_READER = "mp.openapi.model.reader";

    /**
     * Configuration property to specify the fully qualified name of the OASFilter implementation.
     * 
     * @see org.eclipse.microprofile.openapi.OASFilter
     */
    public static final String FILTER = "mp.openapi.filter";

    /**
     * Configuration property to disable annotation scanning.
     * 
     */
    public static final String SCAN_DISABLE = "mp.openapi.scan.disable";

    /**
     * Configuration property to specify the list of packages to scan.
     * 
     */
    public static final String SCAN_PACKAGES = "mp.openapi.scan.packages";

    /**
     * Configuration property to specify the list of classes to scan.
     * 
     */
    public static final String SCAN_CLASSES = "mp.openapi.scan.classes";
    
    /**
     * Configuration property to specify the list of packages to exclude from scans.
     * 
     */
    public static final String SCAN_EXCLUDE_PACKAGES = "mp.openapi.scan.exclude.packages";
    
    /**
     * Configuration property to specify the list of classes to exclude from scans.
     * 
     */
    public static final String SCAN_EXCLUDE_CLASSES = "mp.openapi.scan.exclude.classes";

    /**
     * Configuration property to specify the list of global servers that provide connectivity information.
     * 
     */
    public static final String SERVERS = "mp.openapi.servers";

    /**
     * Prefix of the configuration property to specify an alternative list of servers to service all operations in a path.
     * 
     */
    public static final String SERVERS_PATH_PREFIX = "mp.openapi.servers.path.";

    /**
     * Prefix of the configuration property to specify an alternative list of servers to service an operation.
     * 
     */
    public static final String SERVERS_OPERATION_PREFIX = "mp.openapi.servers.operation.";

    /**
     * Recommended prefix for vendor specific configuration properties.
     * 
     */
    public static final String EXTENSIONS_PREFIX = "mp.openapi.extensions.";

}
