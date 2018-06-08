/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.filter.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.xml.internal.nester.Nester;

/**
 * Represents security configurable options for authentication filter.
 */
public class AuthFilterConfig {
    private static final TraceComponent tc = Tr.register(AuthFilterConfig.class);
    public static final String KEY_ID = "id";

    public static final String KEY_WEB_APP = "webApp";

    static final String KEY_REQUEST_URL = "requestUrl";
    public static final String KEY_URL_PATTERN = "urlPattern";

    static final String KEY_REMOTE_ADDRESS = "remoteAddress";
    public static final String KEY_IP = "ip";

    static final String KEY_HOST = "host";
    public static final String KEY_NAME = "name";

    static final String KEY_USER_AGENT = "userAgent";
    public static final String KEY_AGENT = "agent";

    public static final String KEY_MATCH_TYPE = "matchType";
    public static final String MATCH_TYPE_EQUALS = "equals";
    public static final String MATCH_TYPE_CONTAINS = "contains";
    public static final String MATCH_TYPE_NOT_CONTAIN = "notContain";
    public static final String MATCH_TYPE_GREATER_THAN = "greaterThan";
    public static final String MATCH_TYPE_LESS_THAN = "lessThan";

    private String id;
    private List<Properties> webApps = null;
    private List<Properties> requestUrls = null;
    private List<Properties> remoteAddresses = null;
    private List<Properties> hosts = null;
    private List<Properties> userAgents = null;
    private boolean hasFilter = false;

    /**
     * @param props
     */
    public AuthFilterConfig(Map<String, Object> props) {
        processFlatConfig(props);
    }

    /**
     * @param props
     */
    protected void processFlatConfig(Map<String, Object> props) {
        if (props == null || props.isEmpty())
            return;
        id = (String) props.get(KEY_ID);
        if (id == null || id.length() == 0) {
            Tr.error(tc, "AUTH_FILTER_MISSING_ID", props.toString());
            return;
        }

        //get all nested elements for authFilter
        Map<String, List<Map<String, Object>>> authFilterNestedElements = Nester.nest(props, KEY_WEB_APP, KEY_REQUEST_URL, KEY_REMOTE_ADDRESS, KEY_HOST, KEY_USER_AGENT);

        if (!authFilterNestedElements.isEmpty()) {
            webApps = processElementProps(authFilterNestedElements, KEY_WEB_APP, KEY_NAME, KEY_MATCH_TYPE);
            requestUrls = processElementProps(authFilterNestedElements, KEY_REQUEST_URL, KEY_URL_PATTERN, KEY_MATCH_TYPE);
            remoteAddresses = processElementProps(authFilterNestedElements, KEY_REMOTE_ADDRESS, KEY_IP, KEY_MATCH_TYPE);
            hosts = processElementProps(authFilterNestedElements, KEY_HOST, KEY_NAME, KEY_MATCH_TYPE);
            userAgents = processElementProps(authFilterNestedElements, KEY_USER_AGENT, KEY_AGENT, KEY_MATCH_TYPE);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "id: " + id);
            Tr.debug(tc, "webApps: " + webApps);
            Tr.debug(tc, "requestUrls: " + requestUrls);
            Tr.debug(tc, "remoteAddresses: " + remoteAddresses);
            Tr.debug(tc, "hosts: " + hosts);
            Tr.debug(tc, "userAgents: " + userAgents);
        }
        hasFilter = hasAnyFilterConfig();
    }

    public List<Properties> processElementProps(Map<String, List<Map<String, Object>>> listOfNestedElements, String elementName, String... attrKeys) {
        List<Properties> listOfProps = new ArrayList<Properties>();
        List<Map<String, Object>> listOfElementMaps = listOfNestedElements.get(elementName);
        if (listOfElementMaps != null && !listOfElementMaps.isEmpty()) {
            for (Map<String, Object> elementProps : listOfElementMaps) {
                Properties properties = getElementProperties(elementProps, elementName, attrKeys);
                if (properties != null && !properties.isEmpty()) {
                    listOfProps.add(properties);
                }
            }
        }
        return listOfProps;
    }

    /**
     * Get properties from the given element and/or it's subElements.
     * Ignore system generated props, add the user props to the given Properties object
     * 
     * @param configProps props from the config
     * @param elementName the element being processed
     */
    private Properties getElementProperties(Map<String, Object> configProps, String elementName, String... attrKeys) {
        Properties properties = new Properties();
        for (String attrKey : attrKeys) {
            String value = (String) configProps.get(attrKey);
            if (value != null && value.length() > 0) {
                value = (String) getValue(value);
                properties.put(attrKey, value);
            }
        }

        if (properties.isEmpty() || properties.size() != attrKeys.length) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                //TODO: NLS warning msg 
                Tr.debug(tc, "The authFilter element " + elementName + " specified in the server.xml file is missing one or more of these attributes "
                             + printAttrKeys(attrKeys));
            }
            return null;
        } else
            return properties;
    }

    /**
     * @param attrKeys
     * @return
     */
    private String printAttrKeys(String... attrKeys) {
        StringBuffer strBuff = new StringBuffer();
        strBuff.append("(");
        for (String attrKey : attrKeys) {
            strBuff.append(attrKey);
            strBuff.append(", ");
        }
        int currentIndex = strBuff.lastIndexOf(",");
        strBuff.delete(currentIndex, currentIndex + 2);
        strBuff.append(")");
        return strBuff.toString();
    }

    private Object getValue(Object value) {
        if (value instanceof String) {
            return ((String) value).trim();
        }
        return value;
    }

    protected boolean hasAnyFilterConfig() {
        boolean result = false;
        if ((webApps != null && !webApps.isEmpty()) ||
            (requestUrls != null && !requestUrls.isEmpty()) ||
            (remoteAddresses != null && !remoteAddresses.isEmpty()) ||
            (hosts != null && !hosts.isEmpty()) ||
            (userAgents != null && !userAgents.isEmpty())) {
            result = true;
        } else {
            Tr.info(tc, "AUTH_FILTER_NOT_CONFIG");
        }
        return result;
    }

    public String getId() {
        return id;
    }

    public List<Properties> getWebApps() {
        return webApps;
    }

    public List<Properties> getRequestUrls() {
        return requestUrls;
    }

    public List<Properties> getHosts() {
        return hosts;
    }

    public List<Properties> getRemoteAddresses() {
        return remoteAddresses;
    }

    public List<Properties> getUserAgents() {
        return userAgents;
    }

    public boolean hasFilterConfig() {
        return hasFilter;
    }
}
