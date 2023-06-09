/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.microprofile.openapi.internal.common;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.*;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true, configurationPid = "io.openliberty.microprofile.openapi")
public class OpenAPIEndpointManager {

    private static final TraceComponent tc = Tr.register(OpenAPIEndpointManager.class);

    private static final String OPEN_API_DOC_VAR_NAME = "openAPIDOCPATH";
    private static final String OPEN_API_DOC_ENDPOINT_CONFIG_NAME = "docPath";
    private static final String OPEN_API_DOC_ENDPOINT_PATH = "/openapi";

    private static final String OPEN_API_UI_VAR_NAME = "openAPIUIPATH";
    private static final String OPEN_API_UI_CONFIG_NAME = "uiPath";
    private static final String OPEN_API_UI_PATH = "/ui";

    private static final Pattern PATH_PATTERN = Pattern.compile("^(/[\\w./_-]*)?$");
    private static final Map<String, String> CONFLICTING_PATHS_MAP = Stream.of(
                                                                               new AbstractMap.SimpleEntry<>("/ibm/api", "OpenAPI"),
                                                                               new AbstractMap.SimpleEntry<>("/health", "mpHealth"),
                                                                               new AbstractMap.SimpleEntry<>("/metrics", "mpMetrics")).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    public enum EndpointId {
        UI,
        DOCUMENT
    };

    private String uiPath = null;
    private String docPath = null;

    private WABConfigManager uiWabConfigManager;
    private WABConfigManager docWabConfigManager;

    @Activate
    @Modified
    protected void activate(BundleContext context, Map<String, Object> properties) {
        docPath = OPEN_API_DOC_ENDPOINT_PATH;
        uiPath = docPath + OPEN_API_UI_PATH;

        if (ProductInfo.getBetaEdition()) {
            //check for system property `open_api_path_enabled` as additional guide - getBoolean returns `true` if the value exists and is set to `true`, if the value is `false`
            if (Boolean.getBoolean("open_api_path_enabled")) {
                resolvePathFromProperties(properties);
            }
        }
        //If One of the WAB Config Managers exists then we are modifying the configuration therefore we need to unregister the existing WABs
        if(uiWabConfigManager != null){
            deactivate();
        }
        uiWabConfigManager = new WABConfigManager(context, OPEN_API_UI_VAR_NAME, uiPath, "OpenAPI UI");
        docWabConfigManager = new WABConfigManager(context, OPEN_API_DOC_VAR_NAME, docPath, "OpenAPI Doc");
        uiWabConfigManager.register();
        docWabConfigManager.register();
    }

    @Deactivate
    protected void deactivate() {
        uiWabConfigManager.unregister();
        docWabConfigManager.unregister();
    }

    private void resolvePathFromProperties(Map<String, Object> properties) {
        docPath = (String) properties.get(OPEN_API_DOC_ENDPOINT_CONFIG_NAME);
        docPath = resolvePath(docPath);
        //If path is not valid revert to default value before we process the UI Path
        if (!validatePath(docPath, EndpointId.DOCUMENT)) {
            Tr.error(tc, "OPEN_API_DOC_PATH_UPDATE_FAILED_CWWKO1671E");
            docPath = OPEN_API_DOC_ENDPOINT_PATH;
        }

        if (!docPath.equals(OPEN_API_DOC_ENDPOINT_PATH)) {
            Tr.info(tc, "OPEN_API_DOC_PATH_UPDATE_CWWKO1669I", docPath);
        }

        if (properties.containsKey(OPEN_API_UI_CONFIG_NAME)) {
            uiPath = (String) properties.get(OPEN_API_UI_CONFIG_NAME);
            uiPath = resolvePath(uiPath);
            if (uiPath.equals(docPath)) {
                Tr.error(tc, "OPEN_API_UI_PATH_CONFLICT_CWWKO1672E", uiPath);
                uiPath = docPath + OPEN_API_UI_PATH;
            } else if (!validatePath(uiPath, EndpointId.UI)) {
                Tr.error(tc, "OPEN_API_UI_PATH_UPDATE_FAILED_CWWKO1670E");
                uiPath = docPath + OPEN_API_UI_PATH;
            }
        } else {
            uiPath = docPath + OPEN_API_UI_PATH;
        }

        if (!uiPath.equals(OPEN_API_DOC_ENDPOINT_PATH + OPEN_API_UI_PATH)) {
            Tr.info(tc, "OPEN_API_UI_PATH_UPDATE_CWWKO1668I", docPath);
        }
    }
    /**
     * Validate the OpenAPI path to ensure it is Valid, contains only valid characters and mets certain rules.
     *
     * @param path Path to be validated
     * @param id   Which OpenAPI bundle we are validating the path for.
     * @return
     */
    public static boolean validatePath(String path, EndpointId id) {
        boolean valid = true;

        //check that path is made up of valid characters
        if (!PATH_PATTERN.matcher(path).matches()) {
            if (id.equals(EndpointId.UI)) {
                Tr.error(tc, "OPEN_API_UI_PATH_INVALID_CWWKO1675E", path);
            } else {
                Tr.error(tc, "OPEN_API_DOC_PATH_INVALID_CWWKO1676E", path);
            }
            valid = false;
        }

        // check that no segment is just `/.` or `/..`, `/.....` should be left as is as it has no special meaning
        ArrayList<String> segments = new ArrayList<>(Arrays.asList(path.split("/")));
        if (segments.contains(".") || segments.contains("..")) {
            Tr.error(tc, "OPEN_API_PATH_SEGMENT_INVALID_CWWKO1677E");
            valid = false;
        }

        // Check if there are potential conflicts with other Liberty/MicroProfile features and warn if there is a match.
        if (CONFLICTING_PATHS_MAP.containsKey(path)) {
            //Just warn that might conflict with other features as they might not be active, so not a cause for failure.
            //If there is a conflict WebContainer will error for the second feature that starts.
            if (id.equals(EndpointId.UI)) {
                Tr.warning(tc, "OPEN_API_UI_PATH_POTENTIAL_CONFLICT_CWWKO1673W", CONFLICTING_PATHS_MAP.get(path));
            } else {
                Tr.warning(tc, "OPEN_API_DOC_PATH_POTENTIAL_CONFLICT_CWWKO1674W", CONFLICTING_PATHS_MAP.get(path));
            }
        }

        return valid;
    }

    /**
     * Resolve the provided path to check and modify the path to meet a basic path structure requirements
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



}
