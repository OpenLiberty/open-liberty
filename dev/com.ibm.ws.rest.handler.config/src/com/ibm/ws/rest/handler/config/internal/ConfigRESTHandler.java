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
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
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
import com.ibm.websphere.config.ConfigEvaluatorException;
import com.ibm.websphere.config.WSConfigurationHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.rest.config.ConfigBasedRESTHandler;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * Validates configured resources
 */
@Component(name = "com.ibm.ws.rest.handler.config",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { RESTHandler.class },
           property = { RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=/config" })
public class ConfigRESTHandler extends ConfigBasedRESTHandler {
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

    @Override
    public boolean filterBy(String name) {
        return true;
    }

    @Override
    public final String getAPIRoot() {
        return "/config";
    }

    /**
     * Validates configuration of a resource and returns the result as a JSON object.
     *
     * @param uid unique identifier.
     * @param config configuration of a resource instance.
     * @param processed configurations that have already been processed -- to prevent stack overflow from circular dependencies in errant config.
     * @return JSON representing the configuration. Null if not an external configuration element.
     * @throws IOException
     */
    private JSONObject getConfigInfo(String uid, Dictionary<String, Object> config, Set<String> processed, Locale locale) throws IOException {
        String configDisplayId = (String) config.get("config.displayId");

        boolean isAppDefined;
        boolean isFactoryPid;
        String configElementName;
        if (isFactoryPid = configDisplayId.endsWith("]")) { // factory pid
            isAppDefined = configDisplayId.contains("[java:");
            int end = configDisplayId.lastIndexOf('[');
            int begin = configDisplayId.lastIndexOf('/', end) + 1;
            configElementName = configDisplayId.substring(begin, end);
        } else {
            int slash = configDisplayId.lastIndexOf('/');
            if (isFactoryPid = isAppDefined = (slash >= 0))
                configElementName = configDisplayId.substring(slash + 1); // factory pid for config that is nested under app-defined resource
            else
                configElementName = configDisplayId; // singleton pid
        }

        if (configElementName.indexOf('.') >= 0 && !configElementName.startsWith("properties."))
            return null;

        //Get pid to use with config service
        String servicePid = isFactoryPid ? (String) config.get("service.factoryPid") : (String) config.get("service.pid");
        String extendsSourcePid = isFactoryPid ? (String) config.get("ibm.extends.source.factoryPid") : (String) config.get("ibm.extends.source.pid");

        if (!isAppDefined) { // App-defined connectionFactory creates a single entry and uses the internal supertype pid
            String metaTypeElementName = configHelper.getMetaTypeElementName(extendsSourcePid == null ? servicePid : extendsSourcePid);
            // if the element's name is internal, no config should be added for that element
            if (metaTypeElementName != null && metaTypeElementName.equalsIgnoreCase("internal"))
                return null;
        }

        JSONObject json = new OrderedJSONObject();
        json.put("configElementName", configElementName);
        if (isFactoryPid)
            json.put("uid", uid);

        if (!processed.add(configDisplayId)) {
            json.put("error", Tr.formatMessage(tc, locale, "CWWKO1530_CIRCULAR_DEPENDENCY", configElementName));
            return json;
        }

        boolean registryEntryExists = configHelper.registryEntryExists(servicePid);

        if (isAppDefined)
            try {
                Dictionary<String, Object> defaults = configHelper.getMetaTypeDefaultProperties(extendsSourcePid == null ? servicePid : extendsSourcePid);
                if (defaults != null) {
                    Hashtable<String, Object> merged = new Hashtable<String, Object>();
                    for (Enumeration<String> keys = defaults.keys(); keys.hasMoreElements();) {
                        String key = keys.nextElement();
                        merged.put(key, defaults.get(key));
                    }
                    for (Enumeration<String> keys = config.keys(); keys.hasMoreElements();) {
                        String key = keys.nextElement();
                        Object value = config.get(key);
                        // app-defined resources store custom property values as String, which might need conversion
                        if (value instanceof String)
                            value = configHelper.convert(extendsSourcePid == null ? servicePid : extendsSourcePid, key, (String) value);
                        merged.put(key, value);
                    }
                    config = merged;
                }
            } catch (ConfigEvaluatorException x) {
                throw new RuntimeException(x);
            }

        // Mapping of flat config prefix (like properties.0) to map of flattened config prop names/values
        SortedMap<String, SortedMap<String, Object>> flattened = new TreeMap<String, SortedMap<String, Object>>();

        // Mapping of pid to list of flat config prefixes which are of that pid type
        SortedMap<String, SortedSet<String>> flattenedPids = new TreeMap<String, SortedSet<String>>();

        SortedSet<String> keys = new TreeSet<String>();
        for (java.util.Enumeration<String> en = config.keys(); en.hasMoreElements();) {
            String key = en.nextElement();
            // Don't display items starting with config. or service. or ibm.extends (added by config service)
            // Also don't display items added by app-defined resources
            if (key.startsWith("config.") || key.startsWith("service.") || key.startsWith("ibm.extends") ||
                key.equals("creates.objectClass") || key.equals("jndiName.unique")) {
                continue;
            }

            String metaTypeName = configHelper.getMetaTypeAttributeName(extendsSourcePid == null ? servicePid : extendsSourcePid, key);
            if ("id".equals(key) && "library".equals(configElementName)) {
                // Work around the <library> element marking its id attribute as internal when its
                // id is actually a configurable external.
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

        // Look for child-first nested config elements.
        // This style of config has been discontinued but is still in use by some existing config elements,
        // which need to be handled specially here.
        if ("resourceAdapter".equals(configElementName)) {
            String childFirstFilter = "(config.parentPID=" + config.get("service.pid") + ')';
            Configuration[] childFirstConfigs;
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "child first pid filter", childFirstFilter);
                childFirstConfigs = configAdmin.listConfigurations(childFirstFilter);
            } catch (InvalidSyntaxException x) {
                throw new RuntimeException(x);
            }
            if (childFirstConfigs != null)
                for (Configuration c : childFirstConfigs) {
                    Dictionary<String, Object> props = c.getProperties();
                    String childConfigDisplayId = (String) props.get("config.displayId");
                    int start = configDisplayId.length() + 1;
                    String childElementName = childConfigDisplayId.substring(start, childConfigDisplayId.indexOf('[', start));
                    keys.add(childElementName);
                    config.put(childElementName, Collections.singleton(props.get("service.pid")));
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
            json.put("error", Tr.formatMessage(tc, locale, "CWWKO1531_NOT_FOUND", configElementName));
        }

        for (String key : keys) {
            Integer cardinality = configHelper.getMetaTypeAttributeCardinality(extendsSourcePid == null ? servicePid : extendsSourcePid, key);
            json.put(key, getJSONValue(config.get(key), cardinality, processed, locale));
        }

        for (Map.Entry<String, SortedSet<String>> entry : flattenedPids.entrySet()) {
            String pid = entry.getKey();
            boolean registryEntryExistsForFlattenedConfig = configHelper.registryEntryExists(pid);
            JSONArray list = new JSONArray();
            String prefix = null;
            for (String flatConfigPrefix : entry.getValue()) {
                JSONObject j = new OrderedJSONObject();
                SortedMap<String, Object> flattenedConfigProps = flattened.get(prefix = flatConfigPrefix);
                if (flattenedConfigProps != null)
                    for (Map.Entry<String, Object> prop : flattenedConfigProps.entrySet()) {
                        String key = prop.getKey();
                        String metaTypeName = configHelper.getMetaTypeAttributeName(pid, key);
                        Integer cardinality = configHelper.getMetaTypeAttributeCardinality(pid, key);
                        if (metaTypeName == null // add unknown attributes added by the user
                            || !metaTypeName.equalsIgnoreCase("internal") // add externalized attributes
                            || !registryEntryExistsForFlattenedConfig) { // or all attributes if there is an error in the config
                            Object value = prop.getValue();

                            // App-defined JCA resources store custom property values as String, which might need conversion
                            if (isAppDefined && value instanceof String)
                                value = configHelper.convert(pid, key, (String) value);

                            j.put(key, getJSONValue(value, cardinality, processed, locale));
                        }
                    }
                list.add(j);
            }
            // TODO would be better to get the flattened config element name from config internals rather than hardcoding/approximating it
            String name = (String) config.get(prefix + ".resourceAdapterConfig.id");
            if (name == null) {
                String baseAlias = prefix.replaceAll("\\.\\d+\\.", "");
                name = configHelper.aliasFor(pid, baseAlias);
            }
            if (list.size() == 1) {
                String flatAttrName = prefix.substring(0, prefix.indexOf('.'));
                Integer cardinality = configHelper.getMetaTypeAttributeCardinality(extendsSourcePid == null ? servicePid : extendsSourcePid, flatAttrName);

                // App-defined JCA resources use a supertype pid that lacks cardinality information for flattened properties
                if (isAppDefined && cardinality == null && "properties".equals(flatAttrName)
                    && servicePid != null && servicePid.startsWith("com.ibm.ws.jca.") && servicePid.endsWith(".supertype"))
                    cardinality = 1;

                if (cardinality != null && cardinality >= -1 && cardinality <= 1)
                    json.put(name, list.get(0));
                else
                    json.put(name, list);
            } else
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
     * Converts the specified value to one that can be included in JSON
     *
     * @param value the value to convert
     * @param cardinality cardinality of the metatype AD attribute (if any) that defines this value.
     * @param processed configurations that have already been processed -- to prevent stack overflow from circular dependencies in errant config.
     * @return a String, primitive wrapper, JSONArray, or JSONObject.
     * @throws IOException
     */
    @Trivial // generates too much trace
    private Object getJSONValue(Object value, Integer cardinality, Set<String> processed, Locale locale) throws IOException {
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
                    Object configInfo = getConfigInfo(uid, props, new HashSet<String>(processed), locale);
                    if (configInfo != null)
                        value = configInfo;
                }
            }
        } else if (value instanceof Number || value instanceof Boolean)
            ; // common paths - no special handling
        else if (value instanceof SerializableProtectedString)
            value = "******"; // hide passwords
        else if (value.getClass().isArray()) { // list supplied as an array for positive cardinality
            int length = Array.getLength(value);
            if (length == 1 && Integer.valueOf(1).equals(cardinality))
                value = getJSONValue(Array.get(value, 0), null, processed, locale);
            else {
                JSONArray a = new JSONArray();
                for (int i = 0; i < length; i++)
                    a.add(getJSONValue(Array.get(value, i), null, processed, locale));
                value = a;
            }
        } else if (value instanceof Collection) { // list supplied as a Vector for negative cardinality
            Collection<?> list = (Collection<?>) value;
            int length = list.size();
            if (length == 1 && Integer.valueOf(-1).equals(cardinality))
                value = getJSONValue(list.iterator().next(), null, processed, locale);
            else {
                JSONArray a = new JSONArray();
                for (Object o : list)
                    a.add(getJSONValue(o, null, processed, locale));
                value = a;
            }
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
     * @param id id of configuration element. Null if none.
     * @return the unique identifier (uid)
     */
    @Trivial
    private static final String getUID(String configDisplayId, String id) {
        return id == null || configDisplayId.matches(".*/.*\\[.*\\].*") ? configDisplayId : id;
    }

    @Override
    public Object handleError(RESTRequest request, String uid, String errorMessage) {
        if (uid == null)
            return toJSONObject("error", errorMessage);
        else
            return toJSONObject("uid", uid, "error", errorMessage);
    }

    @Override
    public Object handleSingleInstance(RESTRequest request, String uid, String id, Dictionary<String, Object> configProps) throws IOException {
        return getConfigInfo(uid, configProps, new HashSet<String>(), request.getLocale());
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

    @Override
    @Trivial
    public void populateResponse(RESTResponse response, Object responseInfo) throws IOException {
        JSONArtifact json;
        if (responseInfo instanceof JSONArtifact)
            json = (JSONArtifact) responseInfo;
        else if (responseInfo instanceof List) {
            JSONArray ja = new JSONArray();
            for (Object info : (List<?>) responseInfo)
                if (info instanceof JSONArtifact)
                    ja.add(info);
                else
                    throw new IllegalArgumentException(info.toString()); // should be unreachable
            json = ja;
        } else
            throw new IllegalArgumentException(responseInfo.toString()); // should be unreachable

        String jsonString = json.serialize(true);

        /*
         * com.ibm.json.java.JSONArtifact.serialize() escapes / with \\/.
         * The list of special characters in proper JSON data is:
         * \b Backspace (ascii code 08)
         * \f Form feed (ascii code 0C)
         * \n New line
         * \r Carriage return
         * \t Tab
         * \" Double quote
         * \\ Backslash character
         *
         * Therefore, we will remove this extraneous formatting.
         */
        jsonString = jsonString.replaceAll("\\\\/", "/");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "populateResponse", jsonString);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getOutputStream().write(jsonString.getBytes("UTF-8"));
    }

    /**
     * Restricts use of the config end-point to GET requests only.
     * All other requests will respond with a 405 - method not allowed error.
     *
     * {@inheritDoc}
     */
    @Override
    public final void handleRequest(RESTRequest request, RESTResponse response) throws IOException {
        if (!"GET".equals(request.getMethod())) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Request method was " + request.getMethod() + " but the config endpoint is restricted to GET requests only.");
            }
            response.setResponseHeader("Accept", "GET");
            response.sendError(405); // Method Not Allowed
            return;
        }

        super.handleRequest(request, response);
    }

    @Override
    public boolean requireAdministratorRole() {
        return false;
    }
}
