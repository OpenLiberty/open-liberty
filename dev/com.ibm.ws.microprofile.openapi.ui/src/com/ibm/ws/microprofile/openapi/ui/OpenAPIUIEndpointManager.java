/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package com.ibm.ws.microprofile.openapi.ui;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class OpenAPIUIEndpointManager {

    private static final TraceComponent tc = Tr.register(OpenAPIUIEndpointManager.class);

    private static final String OPEN_API_ENDPOINT_CONFIG_NAME = "mp.openapi.extensions.liberty.path";
    private static final String OPEN_API_ENDPOINT_PATH = "/openapi";

    private static final String OPEN_API_UI_VAR_NAME = "openAPIUIPATH";
    private static final String OPEN_API_UI_CONFIG_NAME = "mp.openapi.extensions.liberty.ui.path";
    private static final String OPEN_API_UI_PATH = "/ui";

    private static final Pattern PATH_PATTERN = Pattern.compile("^(/[\\w./_-]*)?$");
    private static final Map<String, String> CONFLICTING_PATHS_MAP = Stream.of(
                                                                               new AbstractMap.SimpleEntry<>("/ibm/api", "OpenAPI"),
                                                                               new AbstractMap.SimpleEntry<>("/health", "mpHealth"),
                                                                               new AbstractMap.SimpleEntry<>("/metrics", "mpMetrics")).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    private String uiPath = null;
    private String dataPath = null;

    private OpenAPIWABConfigManager openAPIWABConfigManager;

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        //Default UI Path is /openapi/ui
        uiPath = OPEN_API_ENDPOINT_PATH + OPEN_API_UI_PATH;

        if (ProductInfo.getBetaEdition()) {
            Config config = ConfigProvider.getConfig(OpenAPIUIEndpointManager.class.getClassLoader());
            if (config.getOptionalValue("open_api_path_enabled", Boolean.class).orElse(false)) {
                resolvePathFromConfig(config);
                if (!validatePath(uiPath, dataPath)) {
                    uiPath = dataPath + OPEN_API_UI_PATH;
                    Tr.warning(tc,"CWWKO1751W_OPEN_API_PATH_UPDATE_FAILED");
                } else if(!uiPath.equals(OPEN_API_ENDPOINT_PATH + OPEN_API_UI_PATH)){
                    Tr.info(tc,"CWWKO1750I_OPEN_API_PATH_UPDATE",uiPath);
                }
            }
        }
        openAPIWABConfigManager = new OpenAPIWABConfigManager(context, OPEN_API_UI_VAR_NAME, uiPath, "OpenAPI UI");
        openAPIWABConfigManager.activate();
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {
        openAPIWABConfigManager.deactivate();
    }

    /**
     * Validate the OpenAPI path to ensure it is Valid, contains only valid characters and mets certain rules.
     *
     * Reserved Path is a related entitiy where the path and the reserved path cannot be the same*
     *
     * @param path
     * @param reservedPath
     * @return
     */
    public static boolean validatePath(String path, String reservedPath) {
        boolean valid = true;
        try {
            //temporarily construct a URL using provided path to use the URL validation code.
            new URL("https://localhost" + path);
        } catch (MalformedURLException e) {
            Tr.warning(tc, "CWWKO1754W_OPEN_API_UI_PATH_INVALID",e.getLocalizedMessage());
            return false;
        }
        if (path.equals(reservedPath)) {
            Tr.warning(tc, "CWWKO1752W_OPEN_API_PATH_CONFLICT", path);
            valid = false;
        }
        //check that path is made up of valid characters
        if (!PATH_PATTERN.matcher(path).matches()) {
            Tr.warning(tc, "CWWKO1754W_OPEN_API_UI_PATH_INVALID", path);
            valid = false;
            //check against other feature paths to see if a potential conflict might occur
        }

        // check that no segment is just `/.` or `/..`, `/.....` should be left as is as it has no special meaning
        ArrayList<String> segments = new ArrayList<>(Arrays.asList(path.split("/")));
        if (segments.contains(".") || segments.contains("..")) {
            Tr.warning(tc, "CWWKO1755W_OPEN_API_UI_PATH_SEGMENT_INVALID");
            valid = false;
        }

        // Check if there are potential conflicts with other Liberty/MicroProfile features and warn if there is a match.
        if (CONFLICTING_PATHS_MAP.containsKey(path)) {
            //Just warn that might conflict with other features as they might not be active, so not a cause for failure.
            //If there is a conflict WebContainer will error for the second feature that starts.
            Tr.warning(tc, "CWWKO1753W_OPEN_API_UI_PATH_POTENTIAL_CONFLICT", CONFLICTING_PATHS_MAP.get(path));
        }

        return valid;
    }

    /**
     * Resolve the provided path to check and modify the path to meet a basic path structure
     *
     * @param path
     * @return
     */
    public static String resolvePath(String path) {
        // Add a forward slash if the path does not already start with one.
        if (!path.startsWith("/")) {
            path = '/' + path;
        }
        //in case someone specifies for example`////foo//bar` which will get through, keep running until all potential double `//` are removed
        while (path.contains("//")) {
            path = path.replace("//", "/");
        }
        // Remove trailing slash if the path contains one.
        if (path.endsWith("/") && !path.equals("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private void resolvePathFromConfig(Config config) {
        //Process dataPath first as the value is used by UI Path if value does not exist
        dataPath = resolvePath(config.getOptionalValue(OPEN_API_ENDPOINT_CONFIG_NAME, String.class).orElse(OPEN_API_ENDPOINT_PATH));
        uiPath = resolvePath(config.getOptionalValue(OPEN_API_UI_CONFIG_NAME, String.class).orElse(dataPath + OPEN_API_UI_PATH));
    }

}
