/*******************************************************************************
 * Copyright (c) 2017,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.config.internal;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONArtifact;
import com.ibm.json.java.JSONObject;
import com.ibm.json.java.OrderedJSONObject;
import com.ibm.websphere.config.WSConfigurationHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * Validates configured resources
 */
@Component(name = "com.ibm.ws.rest.handler.config",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { RESTHandler.class },
           property = { "com.ibm.wsspi.rest.handler.context.root=/ibm/api", "com.ibm.wsspi.rest.handler.root=/config" }) // TODO switch to /openapi/platform
public class ConfigRESTHandler implements RESTHandler {
    private static final TraceComponent tc = Tr.register(ConfigRESTHandler.class);

    @Reference
    private ConfigurationAdmin configAdmin;

    @Reference
    private WSConfigurationHelper configHelper;

    private ComponentContext context;

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
    }

    /**
     * Validates configuration of a resource and returns the result as a JSON object.
     *
     * @param uid       unique identifier.
     * @param config    configuration of a resource instance.
     * @param processed configurations that have already been processed -- to prevent stack overflow from circular dependencies in errant config.
     * @return JSON representing the configuration. Null if not an external configuration element.
     * @throws IOException
     */
    private JSONObject getConfigInfo(String uid, Dictionary<String, Object> config, Set<String> processed) throws IOException {
        String configDisplayId = (String) config.get("config.displayId");

        boolean isFactoryPid;
        String configElementName;
        if (isFactoryPid = configDisplayId.endsWith("]")) { // factory pid
            int end = configDisplayId.lastIndexOf('[');
            int begin = configDisplayId.lastIndexOf('/', end) + 1;
            configElementName = configDisplayId.substring(begin, end);
        } else
            configElementName = configDisplayId; // singleton pid

        if (configElementName.indexOf('.') >= 0)
            return null;

        //Get pid to use with config service
        String servicePid = isFactoryPid ? (String) config.get("service.factoryPid") : (String) config.get("service.pid");

        String metaTypeElementName = configHelper.getMetaTypeElementName(servicePid);
        // if the element's name is internal no config should be added for that element
        if (metaTypeElementName != null && metaTypeElementName.equalsIgnoreCase("internal"))
            return null;

        JSONObject json = new OrderedJSONObject();
        json.put("configElementName", configElementName);
        if (isFactoryPid)
            json.put("uid", uid);

        if (!processed.add(configDisplayId)) {
            json.put("error", "Circular dependency in configuration.");
            return json;
        }

        boolean registryEntryExists = configHelper.registryEntryExists(servicePid);

        // Mapping of flat config prefix (like properties.0) to map of flattened config prop names/values
        SortedMap<String, SortedMap<String, Object>> flattened = new TreeMap<String, SortedMap<String, Object>>();

        // Mapping of pid to list of flat config prefixes which are of that pid type
        SortedMap<String, SortedSet<String>> flattenedPids = new TreeMap<String, SortedSet<String>>();

        // TODO app defined resources
        SortedSet<String> keys = new TreeSet<String>();
        for (java.util.Enumeration<String> en = config.keys(); en.hasMoreElements();) {
            String key = en.nextElement();
            //don't display items starting with config. or service. or ibm.extends (added by config service)
            if (key.startsWith("config.") || key.startsWith("service.") || key.startsWith("ibm.extends")) {
                continue;
            }

            String metaTypeName = configHelper.getMetaTypeAttributeName(servicePid, key);
            if (key.equals("id")) { //always add id
                keys.add(key);
            } else if ((metaTypeName != null && !metaTypeName.equalsIgnoreCase("internal")) || !registryEntryExists) {
                // add attributes with a name that is not internal or any attributes if there is an error in the config
                keys.add(key);
            } else {
                int prefixEnd = -1;
                StringBuilder prefix = new StringBuilder();
                String suffix = key;
                // flat config prefixes should match [<anything>.<numbers>.]+
                // For example:  child.0.grandchild.0.value   prefix="child.0.grandchild.0." suffix="value"
                while ((prefixEnd = nthIndexOf(suffix, ".", 2) + 1) > 0 && suffix.length() >= prefixEnd) {
                    String possiblePrefix = suffix.substring(0, prefixEnd);
                    if (!possiblePrefix.matches(".*\\.\\d+\\."))
                        break;
                    prefix.append(possiblePrefix);
                    suffix = suffix.substring(prefixEnd);
                }
                if (prefix.length() > 0) {
                    // It is probably a flattened config attribute
                    if ("config.referenceType".equals(suffix)) {
                        String flattenedPid = (String) config.get(key);
                        SortedSet<String> f = flattenedPids.get(flattenedPid);
                        if (f == null)
                            flattenedPids.put(flattenedPid, f = new TreeSet<String>());
                        f.add(prefix.toString());
                    } else {
                        // It is probably a flattened config attribute.
                        SortedMap<String, Object> f = flattened.get(prefix.toString());
                        if (f == null)
                            flattened.put(prefix.toString(), f = new TreeMap<String, Object>());
                        f.put(suffix, config.get(key));
                    }
                } else {
                    // Add MetaTypes with null name.  This includes child first config and invalid config.
                    if (metaTypeName == null) {
                        keys.add(key);
                    }
                }
            }
        }

        // These properties intentionally placed first
        if (keys.remove("id")) {
            String id = (String) config.get("id");
            if (!isGenerated(id))
                json.put("id", id);
        }
        if (keys.remove("jndiName"))
            json.put("jndiName", config.get("jndiName"));

        if (!registryEntryExists) { //registry entry doesn't exist - config service can't find the specified pid
            json.put("error", "Check that the spelling is correct and that the right features are enabled for this configuration.");
        }

        for (String key : keys)
            json.put(key, getJSONValue(config.get(key), processed));

        for (Map.Entry<String, SortedSet<String>> entry : flattenedPids.entrySet()) {
            String pid = entry.getKey();
            JSONArray list = new JSONArray();
            String prefix = null;
            for (String flatConfigPrefix : entry.getValue()) {
                JSONObject j = new OrderedJSONObject();
                SortedMap<String, Object> flattenedConfigProps = flattened.get(prefix = flatConfigPrefix);
                if (flattenedConfigProps != null)
                    for (Map.Entry<String, Object> prop : flattenedConfigProps.entrySet())
                        j.put(prop.getKey(), getJSONValue(prop.getValue(), processed));
                list.add(j);
            }
            // TODO would be better to get the flattened config element name from config internals rather than hardcoding/approximating it
            String name = (String) config.get(prefix + ".resourceAdapterConfig.id");
            if (name == null) {
                String baseAlias = prefix.replaceAll("\\.\\d+\\.", "");
                name = configHelper.aliasFor(pid, baseAlias);
            }
            json.put(name, list);
        }

        // API for this configuration element instance
        if (servicePid != null) {
            ServiceReference<?>[] refs;
            String filter = FilterUtils.createPropertyFilter("com.ibm.wsspi.rest.handler.config.pid", servicePid);
            try {
                refs = context.getBundleContext().getServiceReferences((String) null, filter);
            } catch (InvalidSyntaxException x) {
                refs = null;
            }
            if (refs != null) {
                SortedSet<String> apiRoots = new TreeSet<String>();
                for (ServiceReference<?> ref : refs) {
                    String root = (String) ref.getProperty(RESTHandler.PROPERTY_REST_HANDLER_ROOT);
                    if (root != null)
                        apiRoots.add(root);
                }
                JSONArray api = new JSONArray();
                for (String root : apiRoots) {
                    StringBuilder r = new StringBuilder("/ibm/api");
                    if (root.charAt(0) != '/')
                        r.append('/');
                    r.append(root);
                    if (root.charAt(root.length() - 1) != '/')
                        r.append('/');
                    r.append(configElementName);
                    if (isFactoryPid)
                        r.append('/').append(URLEncoder.encode(uid, "UTF-8"));
                    api.add(r.toString());
                }
                if (!api.isEmpty())
                    json.put("api", api);
            }
        }

        return json;
    }

    /**
     * Returns the most deeply nested element name.
     *
     * @param configDisplayId config.displayId
     * @return the most deeply nested element name. Null if there are not any nested elements.
     */
    private static final String getDeepestNestedElementName(String configDisplayId) {
        int start = configDisplayId.lastIndexOf("]/");
        if (start > 1) {
            int end = configDisplayId.indexOf('[', start += 2);
            if (end > start)
                return configDisplayId.substring(start, end);
        }
        return null;
    }

    /**
     * Converts the specified value to one that can be included in JSON
     *
     * @param value     the value to convert
     * @param processed configurations that have already been processed -- to prevent stack overflow from circular dependencies in errant config.
     * @return a String, primitive wrapper, JSONArray, or JSONObject.
     * @throws IOException
     */
    @Trivial // generates too much trace
    private Object getJSONValue(Object value, Set<String> processed) throws IOException {
        if (value instanceof String) {
            String s = (String) value;
            if (s.matches(".*_\\d+")) {
                // If a value ends with _<numbers> assume it's a PID and try to look it up
                Configuration[] c;
                try {
                    String filter = FilterUtils.createPropertyFilter("service.pid", s);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "getJSONValue pid filter", filter);
                    c = configAdmin.listConfigurations(filter);
                } catch (InvalidSyntaxException x) {
                    throw new RuntimeException(x);
                }
                if (c != null) {
                    Dictionary<String, Object> props = c[0].getProperties();
                    String uid = getUID((String) props.get("config.displayId"), (String) props.get("id"));
                    Object configInfo = getConfigInfo(uid, props, new HashSet<String>(processed));
                    if (configInfo != null)
                        value = configInfo;
                }
            }
        } else if (value instanceof Number || value instanceof Boolean || value instanceof Character)
            ; // common paths - no special handling
        else if (value instanceof SerializableProtectedString)
            value = "******"; // hide passwords
        else if (value.getClass().isArray()) { // list supplied as an array for positive cardinality
            JSONArray a = new JSONArray();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++)
                a.add(getJSONValue(Array.get(value, i), processed));
            value = a;
        } else if (value instanceof Collection) { // list supplied as a Vector for negative cardinality
            JSONArray a = new JSONArray();
            for (Object o : (Collection<?>) value)
                a.add(getJSONValue(o, processed));
            value = a;
        } else
            value = value.toString(); // TODO any special handling here? Example: com.ibm.wsspi.kernel.service.utils.OnErrorUtil$OnError

        return value;
    }

    /**
     * Compute the unique identifier from the id and config.displayId.
     * If a top level configuration element has an id, the id is the unique identifier.
     * Otherwise, the config.displayId is the unique identifier.
     *
     * @param configDisplayId config.displayId of configuration element.
     * @param id              id of configuration element. Null if none.
     * @return the unique identifier (uid)
     */
    @Trivial
    private static final String getUID(String configDisplayId, String id) {
        return id == null || configDisplayId.matches(".*/.*\\[.*\\].*") ? configDisplayId : id;
    }

    /**
     * @see com.ibm.wsspi.rest.handler.RESTHandler#handleRequest(com.ibm.wsspi.rest.handler.RESTRequest, com.ibm.wsspi.rest.handler.RESTResponse)
     */
    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) throws IOException {
        String path = request.getPath(); // /config/dataSource/{uid}
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "handleRequest", path);

        String uid = null;
        int endElementName = path.indexOf('/', 8);
        if (endElementName < 0) { // uid missing
            endElementName = path.length();
        } else {
            uid = URLDecoder.decode(path.substring(endElementName + 1), "UTF-8");
            if (uid.length() == 0)
                uid = null;
        }

        String elementName = path.length() < 8 ? "" : URLDecoder.decode(path.substring(8, endElementName), "UTF-8");

        StringBuilder filter = new StringBuilder("(&");
        if (uid != null && (uid.startsWith(elementName + "[default-") || uid.matches(".*/.*\\[.*\\].*")))
            filter.append(FilterUtils.createPropertyFilter("config.displayId", uid));
        else if (elementName.length() > 0) {
            // If an element name was specified, ensure the filter matches the requested elementName exactly
            filter.append("(|(config.displayId=*" + elementName + "[*)" +
                          "(config.displayId=*" + elementName + "))"); // TODO either check elementName for invalid chars or use something similar to createPropertyFilter, but preserving the * characters
            if (uid != null)
                filter.append(FilterUtils.createPropertyFilter("id", uid));
        } else {
            filter.append("(config.displayId=*)"); // TODO either check elementName for invalid chars or use something similar to createPropertyFilter, but preserving the * characters
            if (uid != null)
                filter.append(FilterUtils.createPropertyFilter("id", uid));
        }

        // Filter by query parameters
        for (Map.Entry<String, String[]> param : request.getParameterMap().entrySet()) {
            String key = param.getKey();
            if ("_".equals(key)) // Workaround for unwanted _ parameter that is appended by API Discovery
                continue;
            String[] values = param.getValue();
            if (values.length > 1)
                filter.append("(|");
            for (String value : values)
                filter.append(FilterUtils.createPropertyFilter(param.getKey(), value));
            if (values.length > 1)
                filter.append(')');
        }
        filter.append(')');

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "filter", filter);

        TreeMap<String, Dictionary<String, Object>> configMap = new TreeMap<String, Dictionary<String, Object>>();

        Configuration[] configurations;
        try {
            configurations = configAdmin.listConfigurations(filter.toString());
        } catch (InvalidSyntaxException x) {
            configurations = null; // same error handling as not found
        }

        if (configurations != null)
            for (Configuration c : configurations) {
                Dictionary<String, Object> props = c.getProperties();
                configMap.put((String) props.get("config.displayId"), props);
            }

        JSONArtifact json;
        if (uid == null) { // return all instances of element type
            JSONArray results = new JSONArray();
            for (Map.Entry<String, Dictionary<String, Object>> entry : configMap.entrySet()) {
                // Filter out entries for nested configurations that we aren't trying to return. Example: dataSource[ds1]/connectionManager[default-0]
                String configDisplayId = entry.getKey();
                String nestedElementName = getDeepestNestedElementName(configDisplayId);
                if (nestedElementName == null || nestedElementName.equals(elementName)) {
                    Dictionary<String, Object> configProps = entry.getValue();
                    String uniqueId = getUID(configDisplayId, (String) configProps.get("id"));
                    // TODO should we return null uniqueId to indicate singleton?
                    JSONObject j = getConfigInfo(uniqueId, configProps, new HashSet<String>());
                    if (j != null)
                        results.add(j);
                }
            }
            json = results;
        } else if (configMap.isEmpty()) {
            json = null;
        } else if (configMap.size() == 1) {
            Map.Entry<String, Dictionary<String, Object>> entry = configMap.firstEntry();
            String configDisplayId = entry.getKey();
            Dictionary<String, Object> configProps = entry.getValue();
            String uniqueId = getUID(configDisplayId, (String) configProps.get("id"));
            if (uid.equals(uniqueId)) // require the correct uid
                json = getConfigInfo(uid, entry.getValue(), new HashSet<String>());
            else // TODO need correct error message
                json = toJSONObject("error", "Unique identifier " + uid + " is not valid. Expected: " + uniqueId);
        } else {
            json = toJSONObject("error", "multiple found"); // TODO: message
        }

        // TODO better message for instance not found?
        if (json == null)
            json = toJSONObject("uid", uid, "error", "Did not find any configured instances of " + elementName + " matching the request");

        //response.setStatus(statusCode);
        String jsonString = json.serialize(true);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getOutputStream().write(jsonString.getBytes("UTF-8"));

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "handleRequest", jsonString);
    }

    /**
     * Returns true if the specified id matches a pattern that indicates it is a generated id for a nested element.
     * Example: transaction//com.ibm.ws.jdbc.dataSource(dataSource)[default-0]
     *
     * @return true if generated, otherwise false.
     */
    private static final boolean isGenerated(String id) {
        return id.matches(".*//.*\\[.*\\].*");
    }

    /**
     * Populates an ordered JSON object with one or more key/value pairs.
     *
     * @param args even number of alternating keys/values.
     * @return ordered JSON object including the specified key/value pairs.
     */
    @Trivial
    private JSONObject toJSONObject(Object... args) {
        OrderedJSONObject json = new OrderedJSONObject();
        for (int i = 0; i < args.length; i += 2)
            json.put(args[i], args[i + 1]);
        return json;
    }

    @Trivial
    private static int nthIndexOf(String str, String substr, int n) {
        int pos = str.indexOf(substr);
        while (--n > 0 && pos != -1)
            pos = str.indexOf(substr, pos + 1);
        return pos;
    }
}