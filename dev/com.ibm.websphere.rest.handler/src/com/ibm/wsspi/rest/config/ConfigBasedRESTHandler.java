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
package com.ibm.wsspi.rest.config;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

// TODO When ready to GA, make this class be SPI in the following feature:
// com.ibm.websphere.features.internal.webapp/com.ibm.websphere.appserver.restHandler-1.0.feature
/**
 * Partial implementation of RESTHandler for API that is based on server configuration.
 * API can have either of the following forms:
 * <ul>
 * <li><code>/ibm/api/apiRoot/{elementName}</code></li>
 * <li><code>/ibm/api/apiRoot/{elementName}/{uid}</code></li>
 * </ul>
 * Implementations should be registered in the service registry as <code>com.ibm.wsspi.rest.handler.RESTHandler</code>,
 * with the service property <code>com.ibm.wsspi.rest.handler.root</code> set to the API root,
 * as outlined in RESTHandler.
 */
public abstract class ConfigBasedRESTHandler implements RESTHandler {
    private static final TraceComponent tc = Tr.register(ConfigBasedRESTHandler.class);

    /**
     * Indicates whether or not to filter by the specified configuration property name if included as a query parameter.
     * For example, the following would filter for dataSources with jndiName of "jdbc/ds1" if this method returns true
     * for the jndiName property,
     *
     * <pre>
     * /ibm/api/config/dataSource?jndiName=jdbc%2Fds1
     * </pre>
     *
     * In general implementations of this method should return false for names of known query parameters that have other purposes
     * and true for names of configuration properties that can be used to narrow the scope of the request.
     *
     * @param name possible property name that is included as a query parameter
     * @return true to filter. Otherwise false. The default implementation of this method always returns false.
     */
    public boolean filterBy(String name) {
        return false;
    }

    /**
     * Returns the most deeply nested element name.
     *
     * @param configDisplayId config.displayId
     * @return the most deeply nested element name. Null if there are not any nested elements.
     */
    private static String getDeepestNestedElementName(String configDisplayId) {
        int start = configDisplayId.lastIndexOf("]/");
        if (start > 1) {
            int end = configDisplayId.indexOf('[', start += 2);
            if (end > start)
                return configDisplayId.substring(start, end);
        }
        return null;
    }

    /**
     * Compute the unique identifier from the id and config.displayId.
     * If a top level configuration element has an id, the id is the unique identifier.
     * Otherwise, the config.displayId is the unique identifier.
     *
     * @param configDisplayId config.displayId of configuration element.
     * @param id id of configuration element. Null if none.
     * @return the unique identifier (uid)
     */
    @Trivial
    private static String getUID(String configDisplayId, String id) {
        return id == null || configDisplayId.matches(".*/.*\\[.*\\].*") ? configDisplayId : id;
    }

    /**
     * Collects response information for an error condition.
     *
     * @param request the REST API request.
     * @param uid identifier that is unique per instance of the configuration element type. Null if unavailable.
     * @param errorMessage error message.
     * @return implementation-specific object that is used to track response information for the specified error.
     * @throws IOException
     */
    public abstract Object handleError(RESTRequest request, String uid, String errorMessage);

    /**
     * Common implementation of handleRequest which locates configured instances matching the request,
     * delegating each to handleSingleInstance, and, after all of which have completed, invokes createResponse.
     *
     * @see com.ibm.wsspi.rest.handler.RESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)
     */
    @Override
    public final void handleRequest(RESTRequest request, RESTResponse response) throws IOException {
        String path = request.getPath();
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "handleRequest", path); // /apiRoot/{elementName}/{uid}

        // TODO would like to do the following, but cannot figure out how to get path parameters with %2F (/) in values from being considered separate path parameter values
        //String uid = request.getPathVariable("uid");
        //String elementName = request.getPathVariable("elementName");

        String apiRoot = "/validator";
        String uid = null;
        int endElementName = path.indexOf('/', apiRoot.length() + 1);
        if (endElementName < 0) { // uid not specified
            endElementName = path.length();
        } else {
            uid = URLDecoder.decode(path.substring(endElementName + 1), "UTF-8");
            if (uid.length() == 0)
                uid = null;
        }
        String elementName = URLDecoder.decode(path.substring(apiRoot.length() + 1, endElementName), "UTF-8");

        StringBuilder filter = new StringBuilder("(&");
        if (uid != null && (uid.startsWith(elementName + "[default-") || uid.matches(".*/.*\\[.*\\].*")))
            filter.append(FilterUtils.createPropertyFilter("config.displayId", uid));
        else if (elementName.length() > 0) {
            // If an element name was specified, ensure the filter matches the requested elementName exactly
            filter.append("(|(config.displayId=*" + elementName + "[*)(config.displayId=*" + elementName + "))"); // TODO either check elementName for invalid chars or use something similar to createPropertyFilter, but preserving the * characters
            if (uid != null)
                filter.append(FilterUtils.createPropertyFilter("id", uid));
        } else {
            filter.append("(config.displayId=*)");
            if (uid != null)
                filter.append(FilterUtils.createPropertyFilter("id", uid));
        }

        // Filter by query parameters
        for (Map.Entry<String, String[]> param : request.getParameterMap().entrySet()) {
            String key = param.getKey();
            if (!filterBy(key) || "_".equals(key)) // Workaround for unwanted _ parameter that is appended by API Discovery
                continue;
            String[] values = param.getValue();
            if (values.length > 1)
                filter.append("(|");
            for (String value : values)
                filter.append(FilterUtils.createPropertyFilter(key, value));
            if (values.length > 1)
                filter.append(')');
        }
        filter.append(')');

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "filter", filter);

        BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference<ConfigurationAdmin> configAdminRef = bundleContext.getServiceReference(ConfigurationAdmin.class);
        ConfigurationAdmin configAdmin = bundleContext.getService(configAdminRef);

        Configuration[] configurations;
        try {
            configurations = configAdmin.listConfigurations(filter.toString());
        } catch (InvalidSyntaxException x) {
            configurations = null; // same error handling as not found
        }

        TreeMap<String, Dictionary<String, Object>> configMap = new TreeMap<String, Dictionary<String, Object>>();

        if (configurations != null)
            for (Configuration c : configurations) {
                Dictionary<String, Object> props = c.getProperties();
                configMap.put((String) props.get("config.displayId"), props);
            }

        Object result;
        if (uid == null) { // apply to all instances of element type
            ArrayList<Object> results = new ArrayList<Object>();
            for (Map.Entry<String, Dictionary<String, Object>> entry : configMap.entrySet()) {
                // Filter out entries for nested configurations that we aren't trying to return. Example: dataSource[ds1]/connectionManager[default-0]
                String configDisplayId = entry.getKey();
                String nestedElementName = getDeepestNestedElementName(configDisplayId);
                if (nestedElementName == null || nestedElementName.equals(elementName)) {
                    Dictionary<String, Object> configProps = entry.getValue();
                    String uniqueId = configDisplayId.endsWith("]") ? getUID(configDisplayId, (String) configProps.get("id")) : null;
                    String id = (String) configProps.get("id");
                    Object r = handleSingleInstance(request, uniqueId, id == null || isGenerated(id) ? null : id, configProps);
                    if (r != null)
                        results.add(r);
                }
            }
            result = results;
        } else if (configMap.isEmpty()) {
            result = null;
        } else if (configMap.size() == 1) {
            Map.Entry<String, Dictionary<String, Object>> entry = configMap.firstEntry();
            String configDisplayId = entry.getKey();
            Dictionary<String, Object> configProps = entry.getValue();
            String uniqueId = getUID(configDisplayId, (String) configProps.get("id"));
            if (uid.equals(uniqueId)) { // require the correct uid
                String id = (String) configProps.get("id");
                result = handleSingleInstance(request, uid, id == null || isGenerated(id) ? null : id, entry.getValue());
            } else // TODO need correct error message
                result = handleError(request, null, "Unique identifier " + uid + " is not valid. Expected: " + uniqueId);
        } else {
            result = handleError(request, null, "multiple found");
        }

        // TODO better message for instance not found?
        if (result == null)
            result = handleError(request, uid, "Did not find any configured instances of " + elementName + " matching the request");

        populateResponse(response, result);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "handleRequest");
    }

    /**
     * Collects response information for a single configuration element instance.
     *
     * @param request the REST API request
     * @param uid identifier that is unique per instance of the configuration element type. Null if configuration element type is a singleton (only allows a single instance).
     * @param id the id attribute of the configuration element instance, if one is specified. Otherwise null.
     * @param configProps name/value pairs representing the configuration of an instance.
     * @return implementation-specific object that is used to track response information for the specified.
     * @throws IOException
     */
    public abstract Object handleSingleInstance(RESTRequest request, String uid, String id, Dictionary<String, Object> configProps) throws IOException;

    /**
     * Returns true if the specified id matches a pattern that indicates it is a generated id for a nested element.
     * Example: transaction//com.ibm.ws.jdbc.dataSource(dataSource)[default-0]
     *
     * @return true if generated, otherwise false.
     */
    private static boolean isGenerated(String id) {
        return id.matches(".*//.*\\[.*\\].*");
    }

    /**
     * Populates the response based on the previously computed information for 0 or more configuration element instances,
     * or error conditions.
     *
     * @param response response to populate.
     * @param responseInfo can be any of the following <ul>
     *            <li>Single response information, as generated by <code>handleSingleInstance</code>.</li>
     *            <li>Error information, as generated by <code>handleError</code>.</li>
     *            <li>ArrayList of response information, as generated by <code>handleSingleInstance</code>
     *            and/or <code>handleError</code>, for 0 or more configuration element instances.
     *            This is used when the {uid} path parameter is omitted.</li>
     *            </ul>
     * @throws IOException
     */
    public abstract void populateResponse(RESTResponse response, Object responseInfo) throws IOException;
}