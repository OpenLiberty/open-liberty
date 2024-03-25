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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import io.openliberty.microprofile.openapi.internal.common.services.OpenAPIEndpointProvider;

@Component(configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true, configurationPid = "io.openliberty.microprofile.openapi")
public class OpenAPIEndpointManager implements OpenAPIEndpointProvider {

    private static final TraceComponent tc = Tr.register(OpenAPIEndpointManager.class);

    private static final String OPEN_API_DOC_VAR_NAME = "openAPIDOCPATH";
    private static final String OPEN_API_DOC_ENDPOINT_CONFIG_NAME = "docPath";
    private static final String OPEN_API_DOC_ENDPOINT_PATH = "/openapi";

    private static final String OPEN_API_UI_VAR_NAME = "openAPIUIPATH";
    private static final String OPEN_API_UI_CONFIG_NAME = "uiPath";
    private static final String OPEN_API_UI_PATH = "/ui";

    private static final Pattern PATH_PATTERN = Pattern.compile("^(/[\\w./_-]*)?$");

    public enum EndpointId {
        UI,
        DOCUMENT
    };

    private String uiPath = null;
    private String docPath = null;

    private WABConfigManager uiWabConfigManager;
    private WABConfigManager docWabConfigManager;

    @Override
    public String getOpenAPIUIUrl() {
        return this.uiPath;
    }

    @Override
    public String getOpenAPIDocUrl() {
        return this.docPath;
    }

    @Activate
    protected void activate(BundleContext context, Map<String, Object> properties) {
        docPath = OPEN_API_DOC_ENDPOINT_PATH;
        uiPath = docPath + OPEN_API_UI_PATH;

        getPathsFromProperties(properties);

        uiWabConfigManager = new WABConfigManager(context, OPEN_API_UI_VAR_NAME, "OpenAPI UI");
        docWabConfigManager = new WABConfigManager(context, OPEN_API_DOC_VAR_NAME, "OpenAPI Doc");
        uiWabConfigManager.setPath(uiPath);
        docWabConfigManager.setPath(docPath);
    }

    @Modified
    protected void modified(Map<String, Object> properties) {
        getPathsFromProperties(properties);
        uiWabConfigManager.setPath(uiPath);
        docWabConfigManager.setPath(docPath);
    }

    @Deactivate
    protected void deactivate() {
        uiWabConfigManager.close();
        docWabConfigManager.close();
    }

    /**
     * Set {@link #docPath} and {@link #uiPath} based on the component properties
     *
     * @param properties the component properties
     */
    private void getPathsFromProperties(Map<String, Object> properties) {
        docPath = (String) properties.get(OPEN_API_DOC_ENDPOINT_CONFIG_NAME);
        docPath = normalizePath(docPath);
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
            uiPath = normalizePath(uiPath);
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
            Tr.info(tc, "OPEN_API_UI_PATH_UPDATE_CWWKO1668I", uiPath);
        }
    }

    /**
     * Validates the OpenAPI path, emits a warning for each validation check failed.
     *
     * @param path Path to be validated
     * @param id   Which OpenAPI bundle we are validating the path for.
     * @return {@code true} if the path is valid, otherwise {@code false}
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

        return valid;
    }

    /**
     * Normalize the provided path, removing empty path segments and ensuring it starts with a slash and doesn't end with one.
     *
     * @param path the path
     * @return the normalized path
     */
    public static String normalizePath(String path) {
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
