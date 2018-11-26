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
package com.ibm.ws.microprofile.openapi.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;
import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

public class CustomCSSWABUpdater {

    private static final TraceComponent tc = Tr.register(CustomCSSWABUpdater.class);

    public static final String HEADER_CSS_URL_KEY = "header-css-url";
    public static final String HEADER_CSS_CONTENT_KEY = "header-css-content";

    protected static final String PATH_CSS_CUSTOM_HEADER = "css/custom-header.css";
    protected static final String PATH_CSS_DEFAULT_HEADER = "css/default-header.css";
    private static final String PATH_CSS_IMAGES_CUSTOM_LOGO = "css/images/custom-logo.png";
    private static final String PATH_IMAGES_CUSTOM_LOGO = "images/custom-logo.png";
    private static final String EXPECTED_BACKGROUND_IMAGE = "background-image: url(" + PATH_IMAGES_CUSTOM_LOGO + ");";

    public void restoreDefaults() {
        try {
            Map<String, Object> filesToUpdate = new HashMap<String, Object>();
            filesToUpdate.put(PATH_CSS_CUSTOM_HEADER, PATH_CSS_DEFAULT_HEADER);
            //Update OpenAPI UI bundles
            OpenAPIUIBundlesUpdater.updateResources(filesToUpdate, true);
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Updated the OpenAPI UI bundles with the default CSS file");
            }
        } catch (Exception e) {
            //Restoring default values shouldn't fail.
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Exception restoring default CSS headers in WAB", e.getClass().getName(), e.getLocalizedMessage());
            }
        }
    }

    @FFDCIgnore({ Exception.class })
    public void update(Map<String, Object> cssData) {
        Object cssUrlObj = cssData.get(HEADER_CSS_URL_KEY);
        try {
            Map<String, Object> filesToUpdate = new HashMap<String, Object>();

            //Add CSS content
            String cssContent = (String) cssData.get(HEADER_CSS_CONTENT_KEY);
            filesToUpdate.put(PATH_CSS_CUSTOM_HEADER, cssContent);

            //Process background image
            if (cssContent.contains("background-image:")) {
                if (cssUrlObj != null && cssUrlObj instanceof String) {
                    processBackgroundImage((String) cssUrlObj, cssContent, filesToUpdate);
                }
            }

            //Update OpenAPI UI bundles
            OpenAPIUIBundlesUpdater.updateResources(filesToUpdate, false);
            if (OpenAPIUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Updated the OpenAPI UI bundles with the custom CSS file " + cssUrlObj);
            }
        } catch (Exception e) {
            //Issue a warning (with details of the exception) and restore default values of OpenAPI UI.
            Tr.warning(tc, "CUSTOM_CSS_NOT_PROCESSED", cssUrlObj.toString(), e.getClass().getName(), e.getLocalizedMessage());
            restoreDefaults();
        }
    }

    private void processBackgroundImage(String cssUrl, String cssContent,
                                        Map<String, Object> filesToUpdate) throws URISyntaxException, IOException {
        //Current restriction : only one image is supported and the relevant property in CSS file has to be 'background-image: url(images/custom-logo.png);'
        if (!cssContent.contains(EXPECTED_BACKGROUND_IMAGE)) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "UNSUPPORTED_CSS_VALUE", "background-image", EXPECTED_BACKGROUND_IMAGE));
        }

        String parentPath = PathUtils.getParent(cssUrl);
        if (!parentPath.endsWith("/")) {
            parentPath = parentPath.concat("/");
        }

        String backgroundImagePath = parentPath.concat(PATH_IMAGES_CUSTOM_LOGO);
        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(tc, "backgroundImagePath=" + backgroundImagePath);
        }

        Object image = null;
        if (backgroundImagePath.startsWith("http:") || backgroundImagePath.startsWith("https:")) {
            image = new URL(backgroundImagePath);
        } else {
            image = new File(new URI(backgroundImagePath));
        }

        validateImage(image); //ensure that the specified image is valid and exists
        filesToUpdate.put(PATH_CSS_IMAGES_CUSTOM_LOGO, image);
        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Added image to map at " + PATH_CSS_IMAGES_CUSTOM_LOGO);
        }
    }

    private void validateImage(Object img) throws IOException {
        if (img != null) {
            if (img instanceof File) {
                File aFile = (File) img;
                if (FileUtils.fileExists(aFile) && FileUtils.fileIsFile(aFile) && FileUtils.getInputStream(aFile) != null) {
                    if (OpenAPIUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "File is valid :" + aFile.getAbsolutePath());
                    }
                    return;
                }
                throw new IllegalArgumentException(Tr.formatMessage(tc, "INVALID_CSS_BACKGROUND_IMAGE", aFile.getAbsolutePath()));
            } else if (img instanceof URL) {
                URL url = (URL) img;
                if (OpenAPIUtils.getUrlAsStream(url, "image/png") != null) {
                    if (OpenAPIUtils.isEventEnabled(tc)) {
                        Tr.event(tc, "Image at URL is valid :" + url);
                    }
                    return;
                }
                throw new IllegalArgumentException(Tr.formatMessage(tc, "INVALID_CSS_BACKGROUND_IMAGE", url));
            }
        }
    }

    public void serverStopping() {
        OpenAPIUIBundlesUpdater.serverStopping();
    }
}
