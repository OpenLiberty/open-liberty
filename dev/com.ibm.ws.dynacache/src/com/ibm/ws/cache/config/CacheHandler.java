/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.xml.sax.Attributes;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.EntryInfo;
import com.ibm.ws.cache.ServerCache;

public class CacheHandler extends ElementHandler implements CacheEntryReceiver, SkipCacheAttributeReceiver {

    private static TraceComponent tc = Tr.register(CacheHandler.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    protected String displayName;
    protected String description;
    private final ArrayList cacheInstances;
    private final ArrayList cacheEntries;
    private ArrayList groups;
    private final HashMap appContext;
    private String appName;
    private String filename;
    private String skipCacheAttribute;

    //-------------------------------------------------------------
    // This array lists invalid class / Component Type combinations
    //-------------------------------------------------------------
    String[][] invalidClassComponentType = { { "command", "parameter" }, {
                                                                          "command", "session" }, {
                                                                                                   "command", "cookie" }, {
                                                                                                                           "command", "attribute" }, {
                                                                                                                                                      "command", "header" },
                                            {
                                             "command", "pathinfo" }, {
                                                                       "command", "servletpath" }, {
                                                                                                    "command", "locale" }, {
                                                                                                                            "command", "body" }, {
                                                                                                                                                  "command", "SOAPAction" },
                                            {
                                             "command", "SOAPEnvelope" }, {
                                                                           "command", "serviceOperation" }, {
                                                                                                             "command", "serviceOperationParameter" }, {
                                                                                                                                                        "command", "requestType" },
                                            {
                                             "command", "portletSession" }, {
                                                                             "command", "portletWindowId" }, {
                                                                                                              "command", "portletMode" }, {
                                                                                                                                           "command", "portletWindowState" },
                                            {
                                             "command", "sessionId" }, {

                                            "webservice", "portletSession" }, {
                                                                               "webservice", "portletWindowId" }, {
                                                                                                                   "webservice", "portletMode" },
                                            {
                                             "webservice", "portletWindowState" }, {

                                            "servlet", "method" }, {
                                                                    "servlet", "field" }, {
                                                                                           "servlet", "body" }, {
                                                                                                                 "servlet", "SOAPAction" }, {
                                                                                                                                             "servlet", "SOAPEnvelope" },
                                            {
                                             "servlet", "serviceOperation" }, {
                                                                               "servlet", "serviceOperationParameter" }, {
                                                                                                                          "servlet", "portletSession" },
                                            {
                                             "servlet", "portletWindowId" }, {
                                                                              "servlet", "portletMode" }, {
                                                                                                           "servlet", "portletWindowState" }, {

                                            "static", "method" }, {
                                                                   "static", "field" }, {
                                                                                         "static", "body" }, {
                                                                                                              "static", "SOAPAction" }, {
                                                                                                                                         "static", "SOAPEnvelope" },
                                            {
                                             "static", "serviceOperation" }, {
                                                                              "static", "serviceOperationParameter" }, {
                                                                                                                        "static", "portletSession" },
                                            {
                                             "static", "portletWindowId" }, {
                                                                             "static", "portletMode" }, {
                                                                                                         "static", "portletWindowState" }, {
                                                                                                                                            "static", "sessionId" },

                                            { "portlet", "method" },
                                            { "portlet", "field" },
                                            { "portlet", "body" },
                                            { "portlet", "SOAPAction" },
                                            { "portlet", "SOAPEnvelope" },
                                            { "portlet", "SOAPHeaderEntry" },
                                            { "portlet", "operation" },
                                            { "portlet", "part" },
                                            { "portlet", "serviceOperation" },
                                            { "portlet", "serviceOperationParameter" },
                                            { "portlet", "cookie" },
                                            { "portlet", "pathinfo" },
                                            { "portlet", "servletpath" },
                                            { "portlet", "tiles_attribute" }

    };
    //-------------------------------------------------------------

    //-------------------------------------------------------------
    // This array lists invalid class / property value combinations
    //-------------------------------------------------------------
    String[][] invalidClassProperty = {
                                       { "servlet", "delay-invalidations" },
                                       { "portlet", "delay-invalidations" },
                                       { "portlet", "edgecacheable" },
                                       { "portlet", "edgeable" },
                                       { "portlet", "externalcache" },
                                       { "portlet", "alternate_url" },
                                       { "command", "edgeable" },
                                       { "command", "externalcache" },
                                       { "command", "consume-subfragments" },
                                       { "command", "do-not-consume" },
                                       { "webservice", "delay-invalidations" },
                                       { "webservice", "edgecacheable" },
                                       { "webservice", "edgeable" },
                                       { "webservice", "externalcache" }
    };

    //-------------------------------------------------------------

    public CacheHandler(String appName, HashMap appContext) {
        // appName and context may be null for global entries...
        this.appName = appName;
        this.appContext = appContext;
        cacheEntries = new ArrayList();
        cacheInstances = new ArrayList(); //MSI
    }

    public void finished() {
        Iterator it = cacheEntries.iterator();
        if (skipCacheAttribute != null) {
            while (it.hasNext()) {
                ConfigEntry ce = (ConfigEntry) it.next();
                if (ce.instanceName == null)
                    ce.skipCacheAttribute = skipCacheAttribute;
                ce.appName = appName;
            }
        }
    }

    public void addCacheEntry(ConfigEntry ce) {
        ce.appName = this.appName;
        cacheEntries.add(ce);
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public ArrayList getEntries() {
        return cacheEntries;
    }

    public ArrayList getInstances() { //MSI begin
        return cacheInstances;
    } //MSI end

    public void addRules(RuleHandler ruleHandler) {
        ruleHandler.addRule("display-name", new DisplayNameHandler());
        ruleHandler.addRule("description", new DescriptionHandler());
        ruleHandler.addRule("cache-instance", new CacheInstanceHandler()); //MSI
        ruleHandler.addRule("cache-entry", new CacheEntryHandler(this));
        ruleHandler.addRule("group", new GroupHandler());
        ruleHandler.addRule("skip-cache-attribute", new SkipCacheAttributeHandler(this));
    }

    private class DisplayNameHandler extends ElementHandler {
        public void finished() {
            displayName = getCharacters();
        }

        public String toString() {
            return "<display-name>";
        }
    }

    private class DescriptionHandler extends ElementHandler {
        public void finished() {
            description = getCharacters();
        }

        public String toString() {
            return "<description>";
        }
    }

    private class CacheInstanceHandler extends ElementHandler implements CacheEntryReceiver, SkipCacheAttributeReceiver { //MSI-begin
        protected String name;
        protected ArrayList configEntries = new ArrayList();
        protected String skipCacheAttribute;

        public CacheInstanceHandler() {
            reset();
        }

        public String toString() {
            return "<cache-instance>";
        }

        public void reset() {
            name = null;
            configEntries = new ArrayList();
            skipCacheAttribute = null;
        }

        public void startElement(String uri, String name, String qName, Attributes attrs) {
            this.name = attrs.getValue("name");

            if (this.name == null)
                Tr.error(tc, "DYNA0030E", new Object[] { "cache-instance", "name" });
        }

        public void finished() {
            CacheInstance ci = new CacheInstance();
            ci.name = name;
            Iterator it = configEntries.iterator();
            while (it.hasNext()) {
                ConfigEntry ce = (ConfigEntry) it.next();
                ce.instanceName = name;
                ce.skipCacheAttribute = skipCacheAttribute;
            }

            ci.configEntries = (ConfigEntry[]) configEntries.toArray(new ConfigEntry[0]);
            cacheEntries.addAll(configEntries);
            cacheInstances.add(ci);
            reset();
        }

        public void addRules(RuleHandler ruleHandler) {
            ruleHandler.addRule("cache-entry", new CacheEntryHandler(this));
            ruleHandler.addRule("skip-cache-attribute", new SkipCacheAttributeHandler(this));
        }

        public void addCacheEntry(ConfigEntry ce) {
            configEntries.add(ce);
        }

        public void addSkipCacheAttribute(String s) {
            skipCacheAttribute = s;
        }

    }

    private class CacheEntryHandler extends ElementHandler implements PropertyReceiver {

        CacheEntryReceiver receiver;
        protected String className;
        protected String name; //convenience, since most cache entries will have only 1 name
        protected HashSet allNames;
        protected int sharingPolicy;
        protected boolean sharingSet;
        protected HashMap properties;
        protected ArrayList cacheIds = new ArrayList();
        protected ArrayList dependencyIds = new ArrayList();
        protected ArrayList invalidations = new ArrayList();

        public CacheEntryHandler(CacheEntryReceiver receiver) {
            reset();
            this.receiver = receiver;
        }

        public String toString() {
            return "<cache-entry>";
        }

        public void reset() {
            className = null;
            name = null;
            allNames = new HashSet();
            sharingPolicy = EntryInfo.NOT_SHARED;
            sharingSet = false;
            properties = new HashMap();
            cacheIds = new ArrayList();
            invalidations = new ArrayList();
            dependencyIds = new ArrayList();
            appName = null;
        }

        public void finished() {
            ConfigEntry ce = new ConfigEntry();
            ce.className = className;
            ce.name = generateName(name);
            HashSet newNames = new HashSet();
            Iterator it = allNames.iterator();
            //TODO: should we remove allNames?
            while (it.hasNext())
                newNames.add(generateName((String) it.next()));
            ce.allNames = newNames;
            if (sharingSet)
                ce.sharingPolicy = sharingPolicy;
            else
                ce.sharingPolicy = ServerCache.getSharingPolicy();
            ce.properties = properties;
            ce.cacheIds = (CacheId[]) cacheIds.toArray(new CacheId[0]);
            ce.dependencyIds = (DependencyId[]) dependencyIds.toArray(new DependencyId[0]);
            ce.invalidations = (Invalidation[]) invalidations.toArray(new Invalidation[0]);
            ce.appName = appName;
            //cacheEntries.add(ce);
            receiver.addCacheEntry(ce);
            reset();
        }

        private String generateName(String name) {
            // don't prefix servletimpl
            String appPrefix = null;
            boolean isClass = name.indexOf(".class") != -1;
            //if name includes .class, then we are done
            if (isClass) {
                return getAppPrefixedName(name);
            }
            if (className.equalsIgnoreCase("command")) {
                if (!isClass)
                    return name + ".class";
                return name;
            }
            return getAppPrefixedName(name);
        }

        private String getAppPrefixedName(String name) {
            String appPrefix = null;
            if (appContext != null)
                appPrefix = (String) appContext.get(className);
            if (appPrefix != null) {
                if (!appPrefix.endsWith("/")) {
                    appPrefix = appPrefix + "/";
                }
                if (name.startsWith("/")) {
                    name = name.substring(1);
                }
                return appPrefix + name;
            }
            return name;
        }

        public void addRules(RuleHandler ruleHandler) {
            ruleHandler.addRule("class", new ClassHandler());
            ruleHandler.addRule("name", new NameHandler());
            ruleHandler.addRule("sharing-policy", new SharingPolicyHandler());
            ruleHandler.addRule("property", new PropertyHandler(this));
            ruleHandler.addRule("cache-id", new CacheIdHandler());
            ruleHandler.addRule("dependency-id", new DependencyIdHandler());
            ruleHandler.addRule("invalidation", new InvalidationHandler());
        }

        public void addProperty(Property p) { //ST-begin
            if (properties == null)
                properties = new HashMap();
            properties.put(p.name, p);
        } //ST-end

        private class ClassHandler extends ElementHandler {
            public void finished() {
                className = getCharacters();
            }

            public String toString() {
                return "<class>";
            }
        }

        private class NameHandler extends ElementHandler {
            public void finished() {
                String tmp = getCharacters();
                if (name == null) {
                    name = tmp;
                }
                allNames.add(tmp);
            }

            public String toString() {
                return "<name>";
            }
        }

        private class SharingPolicyHandler extends ElementHandler {
            public void finished() {

                String policy = getCharacters().toLowerCase();
                if (policy.equals("not-shared"))
                    sharingPolicy = EntryInfo.NOT_SHARED;
                else if (policy.equals("shared-push"))
                    sharingPolicy = EntryInfo.SHARED_PUSH;
                else if (policy.equals("shared-pull"))
                    sharingPolicy = EntryInfo.SHARED_PULL;
                else if (policy.equals("shared-push-pull")) {
                    sharingPolicy = EntryInfo.SHARED_PUSH_PULL;
                } else
                    Tr.error(tc, "DYNA0027E", new Object[] { policy });

                sharingSet = true;
            }

            public String toString() {
                return "<sharing-policy>";
            }
        }

        class CacheIdHandler extends ElementHandler implements PropertyReceiver, ComponentReceiver {
            private String idGenerator;
            private String metaDataGenerator;
            private int timeout;
            private int inactivity; // CPF-Inactivity
            private int priority;
            private HashMap properties;
            private ArrayList components;

            private boolean invalidId = false;

            public CacheIdHandler() {
                reset();
            }

            public String toString() {
                return "<cache-id>";
            }

            public void addRules(RuleHandler ruleHandler) {
                ruleHandler.addRule("display-name", new DisplayNameHandler());
                ruleHandler.addRule("component", new ComponentHandler(this));
                ruleHandler.addRule("idgenerator", new IdGeneratorHandler());
                ruleHandler.addRule("metadatagenerator", new MetaDataGeneratorHandler());
                ruleHandler.addRule("timeout", new TimeoutHandler());
                ruleHandler.addRule("inactivity", new InactivityHandler()); // CPF-Inactivity
                ruleHandler.addRule("priority", new PriorityHandler());
                ruleHandler.addRule("property", new PropertyHandler(this));
            }

            public void reset() {
                idGenerator = metaDataGenerator = null;
                timeout = 0;
                priority = 0;
                inactivity = 0; // CPF-Inactivity
                properties = new HashMap();
                components = new ArrayList();
                invalidId = false;
            }

            public void finished() {
                if (invalidId == false) {
                    CacheId cacheid = new CacheId();
                    cacheid.idGenerator = idGenerator;
                    cacheid.metaDataGenerator = metaDataGenerator;
                    cacheid.timeout = timeout;
                    cacheid.inactivity = inactivity; // CPF-Inactivity
                    cacheid.priority = priority;
                    cacheid.components = (Component[]) components.toArray(new Component[0]);
                    cacheid.properties = properties;
                    cacheIds.add(cacheid);
                }
                reset();
            }

            public void addProperty(Property p) { //ST-begin
                if (properties == null)
                    properties = new HashMap();
                properties.put(p.name, p);
            } //ST-end

            public void addComponent(Component component) {
                if (components == null)
                    components = new ArrayList();
                if (idGenerator != null) {
                    Tr.error(tc, "DYNA0028E", new Object[] { component.id, idGenerator });
                    return;
                }
                components.add(component);
            }

            public void setInvalidId(boolean invalidId) {
                this.invalidId = invalidId;
            }

            private class IdGeneratorHandler extends ElementHandler {
                public void finished() {
                    if (components.size() != 0) {
                        Tr.error(tc, "DYNA0029E", new Object[] { getCharacters(), ((Component) components.get(0)).id });
                        return;
                    }
                    idGenerator = getCharacters();
                }

                public String toString() {
                    return "<idgenerator>";
                }
            }

            private class MetaDataGeneratorHandler extends ElementHandler {
                public void finished() {
                    metaDataGenerator = getCharacters();
                }

                public String toString() {
                    return "<metadatagenerator>";
                }

            }

            private class TimeoutHandler extends ElementHandler {
                public void finished() {
                    String time = getCharacters();
                    try {
                        timeout = Integer.valueOf(time).intValue();
                        if (com.ibm.ws.cache.TimeLimitDaemon.UNIT_TEST_INACTIVITY) {
                            System.out.println("TimeoutHandler() timeoutset to " + timeout);
                        }
                    } catch (Exception ex) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.CacheHandler.finished", "282", this);
                        Tr.error(tc, "dynacache.timeout", new Object[] { "<cache-id>", time });
                    }
                }

                public String toString() {
                    return "<timeout>";
                }

            }

            // CPF-Inactivity
            private class InactivityHandler extends ElementHandler {
                public void finished() {
                    String time = getCharacters();
                    try {
                        inactivity = Integer.valueOf(time).intValue();
                        if (com.ibm.ws.cache.TimeLimitDaemon.UNIT_TEST_INACTIVITY) {
                            System.out.println("InactivityHandler() inactivity set to " + timeout);
                        }
                    } catch (Exception ex) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.CacheHandler.finished", "438", this);
                        Tr.error(tc, "dynacache.timeout", new Object[] { "<cache-id>", time });
                        inactivity = -1;
                    }
                }

                public String toString() {
                    return "<inactivity>";
                }

            }

            private class PriorityHandler extends ElementHandler {
                public void finished() {
                    String pri = getCharacters();
                    try {
                        priority = Integer.valueOf(pri).intValue();
                    } catch (Exception ex) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.CacheHandler.finished", "298", this);
                        Tr.error(tc, "dynacache.priority", new Object[] { "<cache-id>", pri });
                    }
                }

                public String toString() {
                    return "<priority>";
                }
            }

        }

        private class InvalidationHandler extends ElementHandler implements ComponentReceiver {

            private String invalidationGenerator;
            private final ArrayList components = new ArrayList();

            private boolean invalidId = false;

            public InvalidationHandler() {}

            public String toString() {
                return "<invalidation>";
            }

            public void reset() {
                components.clear();
                invalidationGenerator = null;
                invalidId = false;
            }

            public void finished() {
                if (invalidId == false) {
                    Invalidation invalidation = new Invalidation();
                    invalidation.baseName = getCharacters();
                    invalidation.invalidationGenerator = invalidationGenerator;
                    invalidation.components = (Component[]) components.toArray(new Component[0]);
                    invalidations.add(invalidation);
                }
                reset();
            }

            public void addRules(RuleHandler ruleHandler) {
                ruleHandler.addRule("invalidationgenerator", new InvalidationGeneratorHandler());
                ruleHandler.addRule("component", new ComponentHandler(this));
            }

            public void addComponent(Component component) {
                if (invalidationGenerator != null) {
                    Tr.error(tc, "DYNA0028E", new Object[] { component.id, invalidationGenerator });
                    return;
                }
                components.add(component);
            }

            public void setInvalidId(boolean invalidId) {
                this.invalidId = invalidId;
            }

            private class InvalidationGeneratorHandler extends ElementHandler {
                public void finished() {
                    if (components.size() != 0) {
                        Tr.error(tc, "DYNA0029E", new Object[] { getCharacters(), ((Component) components.get(0)).id });
                        return;
                    }
                    invalidationGenerator = getCharacters();
                }

                public String toString() {
                    return "<invalidationGenerator>";
                }
            }

        }

        private class PropertyHandler extends ElementHandler {

            PropertyReceiver receiver;
            String name;
            ArrayList excludeList = new ArrayList(); //ST

            public PropertyHandler(PropertyReceiver receiver) {
                this.receiver = receiver;
            }

            public String toString() {
                return "<property>";
            }

            public void addRules(RuleHandler ruleHandler) { //ST-begin
                ruleHandler.addRule("exclude", new ExcludeHandler());
            }

            public void reset() {
                name = null;
                excludeList.clear();
            }

            public void startElement(String uri, String name, String qName, Attributes attrs) {
                this.name = attrs.getValue("name") != null ? attrs.getValue("name").toLowerCase() : null;

                if (this.name == null)
                    Tr.error(tc, "DYNA0030E", new Object[] { "property", "name" });
            }

            public void finished() {
                if (name != null) {

                    checkInvalidClassProperty(className, name);

                    //---------------------------------------------------------
                    //  DTD allows edgecacheable or edgeable but internally    
                    //  we use edgeable.                                       
                    //---------------------------------------------------------
                    if (this.name.equals("edgecacheable")) {
                        this.name = "edgeable";
                    }
                    //---------------------------------------------------------

                    Property p = new Property(); //ST-begin
                    p.name = name;

                    //primary-storage
                    if (p.name.equals("primary-storage")) {
                        String value = getCharacters();
                        if (value.equals("memory") || value.equals("disk")) {
                            p.value = value;
                        } else {
                            p.value = "memory";
                            Tr.error(tc, "DYNA1032E", new Object[] { value });
                        }
                    } else {
                        p.value = getCharacters();
                    }

                    p.excludeList = (String[]) excludeList.toArray(new String[0]);
                    receiver.addProperty(p);
                    reset();
                } //ST-end
            }

            private class ExcludeHandler extends ElementHandler { //ST-begin

                public String toString() {
                    return "<exclude>";
                }

                public void finished() {
                    String value = getCharacters();

                    if (name.equals("consume-subfragments")) {
                        boolean isClass = value.indexOf(".class") != -1;
                        if (isClass) {
                            value = getAppPrefixedName(value);
                        }
                    }

                    excludeList.add(value);
                }
            }
        }

        private class ComponentHandler extends ElementHandler implements MethodReceiver, FieldReceiver {
            private String type;
            private String id;
            private boolean ignoreValue;
            private boolean multipleIds;
            private Method method;
            private Field field;
            private int index = -1;
            private boolean required;
            private HashMap values;
            private HashMap notValues;
            private ArrayList valueRanges;
            private ArrayList notValueRanges;

            private boolean invalidComponent = false;

            private final ComponentReceiver receiver;

            public ComponentHandler(ComponentReceiver receiver) {
                this.receiver = receiver;
                reset();
            }

            public String toString() {
                return "<component>";
            }

            public void reset() {
                type = null;
                id = null;
                ignoreValue = false;
                multipleIds = false;
                method = null;
                index = -1;
                required = true;
                values = new HashMap();
                notValues = new HashMap();
                valueRanges = new ArrayList();
                notValueRanges = new ArrayList();
                invalidComponent = false;
            }

            public void startElement(String uri, String name, String qName, Attributes attrs) {
                type = attrs.getValue("type");
                id = attrs.getValue("id");
                try {
                    String ignore = attrs.getValue("ignore-value");
                    if (ignore != null)
                        ignoreValue = Boolean.valueOf(ignore).booleanValue();
                } catch (Exception ex) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.CacheHandler.startElement", "403", this);
                    Tr.error(tc, "DYNA0032E", new Object[] { "ignore-value", attrs.getValue("ignore-value") });
                }
                try {
                    String multiIds = attrs.getValue("multipleIDs");
                    if (multiIds != null)
                        multipleIds = Boolean.valueOf(multiIds).booleanValue();
                } catch (Exception ex) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.CacheHandler.startElement", "403", this);
                    Tr.error(tc, "DYNA0032E", new Object[] { "multipleIDs", attrs.getValue("multipleIDs") });
                }
                if (id == null)
                    Tr.error(tc, "DYNA0030E", new Object[] { "component", "id" });
                if (type == null)
                    Tr.error(tc, "DYNA0030E", new Object[] { "component", "type" });
            }

            public void addMethod(Method m) {
                if (method != null || field != null) {
                    Tr.error(tc, "DYNA0031E", new Object[] { "method", m.name });
                    return;
                }
                method = m;
            }

            public void addField(Field f) {
                if (method != null || field != null) {
                    Tr.error(tc, "DYNA0031E", new Object[] { "field", f.name });
                    return;
                }
                field = f;
            }

            public void addRules(RuleHandler ruleHandler) {
                ruleHandler.addRule("method", new MethodHandler(this));
                ruleHandler.addRule("field", new FieldHandler(this));
                ruleHandler.addRule("index", new IndexHandler());
                ruleHandler.addRule("required", new RequiredHandler());
                ruleHandler.addRule("value", new ValueHandler());
                ruleHandler.addRule("not-value", new NotValueHandler());
            }

            public void finished() {
                if (invalidComponent == false) {
                    if (id == null || type == null)
                        return;
                    Component c = new Component();
                    c.type = type;
                    c.id = id;
                    c.ignoreValue = ignoreValue;
                    c.multipleIds = multipleIds;
                    c.method = method;
                    c.field = field;
                    c.index = index;
                    c.required = required;
                    c.values = values;
                    c.notValues = notValues;
                    c.valueRanges = valueRanges;
                    c.notValueRanges = notValueRanges;
                    c.validate();
                    receiver.addComponent(c);

                    checkInvalidClassComponentType(className, type);
                } else {
                    receiver.setInvalidId(true);
                }
                reset();
            }

            private class MethodHandler extends ElementHandler implements MethodReceiver, FieldReceiver {

                MethodReceiver receiver;
                Method method = null;
                Field field = null;
                int index = -1;

                public MethodHandler(MethodReceiver receiver) {
                    this.receiver = receiver;
                }

                public String toString() {
                    return "<method>";
                }

                public void addRules(RuleHandler ruleHandler) {
                    ruleHandler.addRule("method", new MethodHandler(this));
                    ruleHandler.addRule("field", new FieldHandler(this));
                    ruleHandler.addRule("index", new IndexHandler());
                }

                public void addMethod(Method m) {
                    if (method != null || field != null) {
                        Tr.error(tc, "DYNA0031E", new Object[] { "method", m.name });
                        return;
                    }
                    method = m;
                }

                public void addField(Field f) {
                    if (method != null || field != null) {
                        Tr.error(tc, "DYNA0031E", new Object[] { "field", f.name });
                        return;
                    }
                    field = f;
                }

                public void finished() {
                    Method newMethod = new Method();
                    newMethod.name = getCharacters();
                    newMethod.method = method;
                    newMethod.field = field;
                    newMethod.index = index;
                    receiver.addMethod(newMethod);
                }
            }

            private class FieldHandler extends ElementHandler implements MethodReceiver, FieldReceiver {

                FieldReceiver receiver;
                Method method = null;
                Field field = null;
                int index = -1;

                public FieldHandler(FieldReceiver receiver) {
                    this.receiver = receiver;
                }

                public String toString() {
                    return "<field>";
                }

                public void addRules(RuleHandler ruleHandler) {
                    ruleHandler.addRule("method", new MethodHandler(this));
                    ruleHandler.addRule("field", new FieldHandler(this));
                    ruleHandler.addRule("index", new IndexHandler());
                }

                public void addMethod(Method m) {
                    if (method != null || field != null) {
                        Tr.error(tc, "DYNA0031E", new Object[] { "method", m.name });
                        return;
                    }
                    method = m;
                }

                public void addField(Field f) {
                    if (method != null || field != null) {
                        Tr.error(tc, "DYNA0031E", new Object[] { "field", f.name });
                        return;
                    }
                    field = f;
                }

                public void finished() {
                    Field newField = new Field();
                    newField.name = getCharacters();
                    newField.method = method;
                    newField.field = field;
                    newField.index = index;
                    receiver.addField(newField);
                }
            }

            private class IndexHandler extends ElementHandler {
                public void finished() {
                    String ind = getCharacters();
                    try {
                        index = Integer.valueOf(ind).intValue();
                    } catch (Exception ex) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.CacheHandler.finished", "298", this);
                        Tr.error(tc, "dynacache.priority", new Object[] { "<component>", ind });
                    }
                }

                public String toString() {
                    return "<index>";
                }
            }

            private class RequiredHandler extends ElementHandler {
                public void finished() {
                    String req = getCharacters();
                    try {
                        if (req != null)
                            required = Boolean.valueOf(req).booleanValue();
                    } catch (Exception ex) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.CacheHandler.finished", "547", this);
                        Tr.error(tc, "DYNA0032E", new Object[] { "required", req });
                    }
                }

                public String toString() {
                    return "<required>";
                }
            }

            private class ValueHandler extends ElementHandler implements RangeReceiver {

                ArrayList ranges = new ArrayList();

                public String toString() {
                    return "<value>";
                }

                public void addRules(RuleHandler ruleHandler) {
                    ruleHandler.addRule("range", new RangeHandler(this));
                }

                public void addRange(Range r) {
                    ranges.add(r);
                }

                public void finished() {

                    if (!getCharacters().equals("")) {
                        Value val = new Value();
                        val.value = getCharacters();
                        values.put(val.value, val);
                    }
                    if (ranges != null) {
                        Iterator it = ranges.iterator();
                        while (it.hasNext())
                            valueRanges.add(it.next());
                    }
                    reset();
                }

                public void reset() {
                    ranges.clear();
                }

            }

            private class NotValueHandler extends ElementHandler implements RangeReceiver {

                ArrayList ranges = new ArrayList();

                public String toString() {
                    return "<not-value>";
                }

                public void addRules(RuleHandler ruleHandler) {
                    ruleHandler.addRule("range", new RangeHandler(this));
                }

                public void addRange(Range r) {
                    ranges.add(r);
                }

                public void finished() {
                    if (!getCharacters().equals("")) {
                        NotValue notVal = new NotValue();
                        notVal.notValue = getCharacters();
                        notValues.put(notVal.notValue, notVal);
                    }
                    if (ranges != null) {
                        Iterator it = ranges.iterator();
                        while (it.hasNext())
                            notValueRanges.add(it.next());
                    }
                    reset();
                }

                public void reset() {
                    ranges.clear();
                }
            }

            private class RangeHandler extends ElementHandler {

                private String low;
                private String high;

                private final RangeReceiver receiver;

                public RangeHandler(RangeReceiver receiver) {
                    this.receiver = receiver;
                    reset();
                }

                public void startElement(String uri, String name, String qName, Attributes attrs) {
                    low = attrs.getValue("low");
                    high = attrs.getValue("high");

                    if (low == null)
                        Tr.error(tc, "DYNA0030E", new Object[] { "range", "low" });
                    if (high == null)
                        Tr.error(tc, "DYNA0030E", new Object[] { "range", "high" });
                }

                public String toString() {
                    return "<range>";
                }

                public void finished() {
                    Range range = new Range();

                    try {
                        range.low = Integer.parseInt(low);
                        range.high = Integer.parseInt(high);
                        if (range.low <= range.high)
                            receiver.addRange(range);
                        else {
                            Tr.error(tc, "DYNA1031E", new Object[] { "'" + low + "'", "'" + high + "'" });
                            invalidComponent = true;
                        }
                    } catch (Exception ex) {
                        Tr.error(tc, "DYNA1030E", new Object[] { "'" + low + "'", "'" + high + "'" });
                        invalidComponent = true;
                    }
                    reset();
                }

                public void reset() {
                    low = null;
                    high = null;
                }
            }

        }

        private class DependencyIdHandler extends ElementHandler implements ComponentReceiver {

            ArrayList components = new ArrayList();

            private boolean invalidId = false;

            public DependencyIdHandler() {}

            public String toString() {
                return "<dependency-id>";
            }

            public void addRules(RuleHandler ruleHandler) {
                ruleHandler.addRule("component", new ComponentHandler(this));
            }

            public void addComponent(Component component) {
                if (components == null)
                    components = new ArrayList();
                components.add(component);
            }

            public void setInvalidId(boolean invalidId) {
                this.invalidId = invalidId;
            }

            public void reset() {
                components.clear();
                invalidId = false;
            }

            public void finished() {
                if (invalidId == false) {
                    DependencyId g = new DependencyId();
                    g.baseName = getCharacters().trim();
                    g.components = (Component[]) components.toArray(new Component[0]);
                    dependencyIds.add(g);
                }
                reset();
            }
        }

    }

    private class GroupHandler extends ElementHandler {

        public GroupHandler() {
            reset();
        }

        public String toString() {
            return "<group>";
        }

        public void reset() {}

        public void finished() {}

        public void addRules(RuleHandler ruleHandler) {
            ruleHandler.addRule("description", new DescriptionHandler());
        }

        public void startElement(String uri, String name, String qName, Attributes attrs) {
            String nameFromAttrs = attrs.getValue("name");
            if (nameFromAttrs == null)
                Tr.error(tc, "DYNA0030E", new Object[] { "group", "name" });
        }

    }

    private class SkipCacheAttributeHandler extends ElementHandler {

        SkipCacheAttributeReceiver receiver = null;

        public SkipCacheAttributeHandler(SkipCacheAttributeReceiver receiver) {
            this.receiver = receiver;
        }

        public String toString() {
            return "<skip-cache-attribute>";
        }

        public void finished() {
            receiver.addSkipCacheAttribute(getCharacters());
        }
    }

    private interface PropertyReceiver {
        public void addProperty(Property P);
    }

    private interface ComponentReceiver {
        public void addComponent(Component component);

        public void setInvalidId(boolean invalidId);
    }

    private interface RangeReceiver {
        public void addRange(Range range);
    }

    private interface MethodReceiver {
        public void addMethod(Method method);
    }

    private interface FieldReceiver {
        public void addField(Field field);
    }

    //-------------------------------------------------------------------------------
    //   This method is called by the Properity handler method.  An error
    //   meesage is sent to the log if an invalid class/proprty combination
    //   is found.
    //-------------------------------------------------------------------------------
    private void checkInvalidClassProperty(String className, String propertyName) {
        if (className != null && propertyName != null) {
            boolean isInvalidCombination = walkInvalidList(className, propertyName, invalidClassProperty);
            if (isInvalidCombination) {
                Tr.error(tc, "DYNA0050E", new Object[] { "'" + propertyName + "'", "'" + className + "'" });
                Tr.error(tc, "dynacache.configerror" /* DYNA0022E */
                         , new Object[] { filename });
            }
        }
    }

    //-------------------------------------------------------------------------------

    //-------------------------------------------------------------------------------
    //   This method is called by the Component handler method.  An error
    //   meesage is sent to the log if an invalid class/componentType combination
    //   is found.
    //-------------------------------------------------------------------------------
    private void checkInvalidClassComponentType(String className, String componentType) {
        if (className != null && componentType != null) {
            boolean isInvalidCombination = walkInvalidList(className, componentType, invalidClassComponentType);
            if (isInvalidCombination) {
                Tr.error(tc, "DYNA0050E", new Object[] { "'" + componentType + "'", "'" + className + "'" });
                Tr.error(tc, "dynacache.configerror" /* DYNA0022E */
                         , new Object[] { filename });
            }
        }
    }

    //-------------------------------------------------------------------------------

    private boolean walkInvalidList(String className, String name, String[][] list) {
        boolean isInvalidCombination = false;

        for (int i = 0; i != list.length; i++) {
            if (className.equalsIgnoreCase(list[i][0])) {
                if (name.equalsIgnoreCase(list[i][1])) {
                    isInvalidCombination = true;
                    break;
                }
            }
        }
        return isInvalidCombination;
    }

    public void addSkipCacheAttribute(String s) {
        skipCacheAttribute = s;
    }

}

interface CacheEntryReceiver {
    public void addCacheEntry(ConfigEntry c);
}

interface SkipCacheAttributeReceiver {
    public void addSkipCacheAttribute(String s);
}
