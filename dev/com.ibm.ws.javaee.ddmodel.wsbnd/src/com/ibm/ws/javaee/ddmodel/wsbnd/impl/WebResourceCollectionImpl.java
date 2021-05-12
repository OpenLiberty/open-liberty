/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.wsbnd.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.web.common.WebResourceCollection;
import com.ibm.ws.javaee.ddmodel.wsbnd.internal.NestingUtils;

public class WebResourceCollectionImpl implements WebResourceCollection {

    private final List<Description> descriptions = new ArrayList<Description>();
    private final String webResourceName;
    private List<String> urlPatterns;
    private List<String> httpMethods;
    private List<String> httpMethodOmissions;

    /**
     * @param wrcConfig
     */
    public WebResourceCollectionImpl(Map<String, Object> config) {
        List<Map<String, Object>> descriptionConfigs = NestingUtils.nest("description", config);
        if (descriptionConfigs != null) {
            for (Map<String, Object> descriptionConfig : descriptionConfigs) {
                descriptions.add(new DescriptionImpl(descriptionConfig));
            }
        }

        String[] value = (String[]) config.get("url-pattern");
        if (value != null)
            urlPatterns = Arrays.asList(value);

        value = (String[]) config.get("http-method");
        if (value != null)
            httpMethods = Arrays.asList(value);

        value = (String[]) config.get("http-method-omission");
        if (value != null)
            httpMethodOmissions = Arrays.asList(value);

        webResourceName = (String) config.get("web-resource-name");

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.javaee.dd.common.Describable#getDescriptions()
     */
    @Override
    public List<Description> getDescriptions() {
        return descriptions;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.javaee.dd.web.common.WebResourceCollection#getWebResourceName()
     */
    @Override
    public String getWebResourceName() {
        return webResourceName;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.javaee.dd.web.common.WebResourceCollection#getURLPatterns()
     */
    @Override
    public List<String> getURLPatterns() {
        return urlPatterns;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.javaee.dd.web.common.WebResourceCollection#getHTTPMethods()
     */
    @Override
    public List<String> getHTTPMethods() {
        return httpMethods;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.javaee.dd.web.common.WebResourceCollection#getHTTPMethodOmissions()
     */
    @Override
    public List<String> getHTTPMethodOmissions() {
        return httpMethodOmissions;
    }

}
