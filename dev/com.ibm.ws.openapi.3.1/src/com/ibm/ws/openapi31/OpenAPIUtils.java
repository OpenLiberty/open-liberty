/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.openapi31;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.servers.Server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.openapi.impl.core.util.Json;
import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.info.InfoImpl;
import com.ibm.ws.microprofile.openapi.impl.model.servers.ServerImpl;
import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIParser;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

public class OpenAPIUtils {

    private static final String OA_INFO_TITLE = "Liberty REST APIs";
    private static final String OA_INFO_DESCRIPTION = "Discover REST APIs available within Liberty";
    private static final String OA_INFO_VERSION = "1.0.0";

    private static final TraceComponent tc = Tr.register(OpenAPIUtils.class);

    public static boolean isDebugEnabled(TraceComponent tc) {
        return TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
    }

    public static boolean isEventEnabled(TraceComponent tc) {
        return TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled();
    }

    public static boolean isDumpEnabled(TraceComponent tc) {
        return TraceComponent.isAnyTracingEnabled() && tc.isDumpEnabled();
    }

    public static String getOsgiServiceErrorMessage(Class<?> aClass, String serviceName) {
        return TraceNLS.getFormattedMessage(aClass,
                                            TraceConstants.TRACE_BUNDLE_CORE,
                                            "OSGI_SERVICE_ERROR",
                                            new Object[] { serviceName },
                                            "CWWKO1600E: The {0} OSGi service is not available.");
    }

    public static String normalizeContextRoot(String contextRoot) {
        //Remove any trailing / or /* characters
        if (contextRoot == null || contextRoot.trim().isEmpty()) {
            // No-op
        } else if ("/*".equals(contextRoot)) {
            contextRoot = "/"; // special case : context root is '/'
        } else if (contextRoot.endsWith("/*")) {
            contextRoot = contextRoot.substring(0, contextRoot.length() - 2);
        } else if (contextRoot.endsWith("/")) {
            contextRoot = contextRoot.substring(0, contextRoot.length() - 1);
        }

        return contextRoot;
    }

    @Trivial
    @FFDCIgnore(JsonProcessingException.class)
    public static String getSerializedJsonDocument(final OpenAPI openapi) {
        String oasResult = null;
        try {
            oasResult = Json.mapper().writeValueAsString(openapi);
        } catch (JsonProcessingException e) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed to serialize OpenAPI docuemnt: " + e.getMessage());
            }
        }
        return oasResult;
    }

    /**
     * Determine whether context root needs to be added (by checking global server and the first path only)
     */
    public static boolean isContextRootNeeded(final OpenAPI openapi, String contextRoot) {

        if (OpenAPIUtils.isGlobalServerSpecified(openapi)) {
            //If server(s) is specified then users know what they are doing, so don't do anything
            return false;
        }

        Paths paths = openapi.getPaths();
        if (paths != null && !paths.isEmpty()) {
            String path = paths.keySet().iterator().next();
            List<Server> pathServers = paths.get(path).getServers();
            //if path doesn't already start with context root and doesn't specify any server then add context root
            if (!path.startsWith(contextRoot) && (pathServers == null || pathServers.isEmpty())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine whether OpenAPI has any global server(s)
     */
    public static boolean isGlobalServerSpecified(final OpenAPI openapi) {
        return openapi.getServers() != null && !openapi.getServers().isEmpty();
    }

    /**
     * @param server
     * @return
     */
    public static Server copyServer(Server server) {
        Server s = new ServerImpl();
        s.setDescription(server.getDescription());
        s.setExtensions(server.getExtensions());
        s.setVariables(server.getVariables());
        s.setUrl(server.getUrl());
        return s;
    }

    public static String stringify(List<?> list) {
        return list == null ? "null" : list.stream().map(i -> i == null ? "null" : i.toString()).collect(Collectors.joining(", ", "[", "]"));
    }

    public static OpenAPI createDefaultOpenAPI() {
        return new OpenAPIImpl().info(new InfoImpl().title(OpenAPIUtils.OA_INFO_TITLE).description(OpenAPIUtils.OA_INFO_DESCRIPTION).version(OpenAPIUtils.OA_INFO_VERSION)).servers(Collections.emptyList());
    }

    public static Info ensureValidInfo(Info info) {
        if (info == null) {
            info = new InfoImpl();
        }
        if (info.getTitle() == null || info.getTitle().isEmpty()) {
            info.setTitle(OA_INFO_TITLE);
        }
        if (info.getVersion() == null || info.getVersion().isEmpty()) {
            info.setVersion(OA_INFO_VERSION);
        }
        return info;
    }

    public static <T> T getOptionalValue(Config config, String propertyName, Class<T> propertyType, T defaultValue) {
        return getOptionalValue(config, propertyName, propertyType, defaultValue, null);
    }

    public static <T> T getOptionalValue(Config config, String propertyName, Class<T> propertyType, T defaultValue, Predicate<? super T> filter) {
        try {
            Optional<T> optional = config.getOptionalValue(propertyName, propertyType);
            if (filter != null) {
                optional = optional.filter(filter);
            }
            return optional.orElse(defaultValue);
        } catch (IllegalArgumentException e) {
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Failed to read config: " + e.getMessage());
            }
        }
        return defaultValue;
    }

    public static Set<String> getConfigPropAsSet(Config config, String propertyName) {
        String[] configValues = getOptionalValue(config, propertyName, String[].class, null);
        if (configValues == null || configValues.length == 0) {
            return Collections.emptySet();
        } else {
            Set<String> configPropSet = new HashSet<>();
            for (String s : configValues) {
                if (s != null && StringUtils.isNotBlank(s)) {
                    configPropSet.add(s);
                }
            }
            if (configPropSet.isEmpty()) {
                return Collections.emptySet();
            }
            return configPropSet;
        }
    }

    @FFDCIgnore({ IOException.class })
    public static String getAPIDocFromFile(File file) {
        try {
            return FileUtils.readFileToString(file, StandardCharsets.UTF_8); //YAML document's format has to be preserved
        } catch (IOException ioe) {
            if (isEventEnabled(tc)) {
                Tr.event(tc, "Exception when reading: " + ioe.getMessage());
            }
        }

        return null;
    }

    public static OpenAPI parseOpenAPI(String openapi) {
        if (openapi == null) {
            return null;
        }
        try {
            SwaggerParseResult parseResult = new OpenAPIParser().readContents(openapi, null, null, null);
            if (parseResult != null) {
                return parseResult.getOpenAPI();
            }
        } catch (Exception se) {
            if (isEventEnabled(tc)) {
                Tr.event(tc, "Exception occurred while trying to read document : \n exception=" + se);
            }
        }
        return null;
    }

    /**
     * Copy the specification extensions available (at OpenAPI object level) from one OpenAPI to another
     *
     * @param copyFrom - OpenAPI object to copy the extensions from
     * @param copyTo - OpenAPI object to copy the extensions to
     */
    public static void copyOpenAPIObjectExtensions(OpenAPI copyFrom, OpenAPI copyTo) {
        if (copyFrom == null || copyTo == null) {
            return; //no-op
        }

        Map<String, Object> extensionsToCopy = copyFrom.getExtensions();
        if (extensionsToCopy != null) {
            for (String key : extensionsToCopy.keySet()) {
                copyTo.addExtension(key, extensionsToCopy.get(key));
            }
        }

    }

}
