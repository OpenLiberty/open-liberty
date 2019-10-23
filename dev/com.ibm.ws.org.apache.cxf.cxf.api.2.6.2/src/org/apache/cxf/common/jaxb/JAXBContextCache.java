/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.common.jaxb;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.CacheMap;
import org.apache.cxf.common.util.CachedClass;
import org.apache.cxf.common.util.StringUtils;

/**
 * 
 */
public final class JAXBContextCache {  
    
    /**
     * Return holder of the context, classes, etc...
     * Do NOT hold onto these strongly as that can lock the JAXBContext and Set<Class> objects
     * into memory.  It preferred to grab the context and classes (if needed) from this object
     * immediately after the call to getCachedContextAndSchemas and then discard it.  The
     * main purpose of this class is to hold onto the context/set strongly until the caller 
     * has a chance to copy those into a place where they can hold onto it strongly as
     * needed.
     */
    public static final class CachedContextAndSchemas {
        private final JAXBContext context;
        private final Set<Class<?>> classes;
        private final WeakReference<CachedContextAndSchemasInternal> ccas;
        private CachedContextAndSchemas(JAXBContext context, Set<Class<?>> classes, CachedContextAndSchemasInternal i) {
            this.context = context;
            this.classes = classes;
            ccas = new WeakReference<CachedContextAndSchemasInternal>(i);
        }
        private CachedContextAndSchemas(CachedContextAndSchemasInternal i) {
            this.context = i.getContext();
            this.classes = i.getClasses();
            ccas = new WeakReference<CachedContextAndSchemasInternal>(i);
        }
        public JAXBContext getContext() {
            return context;
        }
        public Set<Class<?>> getClasses() {
            return classes;
        }
        public Collection<DOMSource> getSchemas() {
            CachedContextAndSchemasInternal i = ccas.get();
            if (i != null) {
                return i.getSchemas();
            }
            return null;
        }

        public void setSchemas(Collection<DOMSource> schemas) {
            CachedContextAndSchemasInternal i = ccas.get();
            if (i != null) {
                i.setSchemas(schemas);
            }
        }
        
    }
    private static final class CachedContextAndSchemasInternal {
        private final WeakReference<JAXBContext> context;
        private final WeakReference<Set<Class<?>>> classes;
        private Collection<DOMSource> schemas;

        CachedContextAndSchemasInternal(JAXBContext context, Set<Class<?>> classes) {
            this.context = new WeakReference<JAXBContext>(context);
            this.classes = new WeakReference<Set<Class<?>>>(classes);
        }

        public JAXBContext getContext() {
            return context.get();
        }
        public Set<Class<?>> getClasses() {
            return classes.get();
        }

        public Collection<DOMSource> getSchemas() {
            return schemas;
        }

        public void setSchemas(Collection<DOMSource> schemas) {
            this.schemas = schemas;
        }
    }   
    
    private static final Map<Set<Class<?>>, CachedContextAndSchemasInternal> JAXBCONTEXT_CACHE
        = new CacheMap<Set<Class<?>>, CachedContextAndSchemasInternal>();

    private static final Map<Package, CachedClass> OBJECT_FACTORY_CACHE
        = new CacheMap<Package, CachedClass>(); 
    
    private JAXBContextCache() {
        //utility class
    }
    
    /**
     * Clear any caches to make sure new contexts are created
     */
    public static void clearCaches() {
        synchronized (JAXBCONTEXT_CACHE) {
            JAXBCONTEXT_CACHE.clear();
        }
        synchronized (OBJECT_FACTORY_CACHE) {
            OBJECT_FACTORY_CACHE.clear();
        }
    }

    public static void scanPackages(Set<Class<?>> classes) {
        JAXBUtils.scanPackages(classes, OBJECT_FACTORY_CACHE);
    }
    
    public static CachedContextAndSchemas getCachedContextAndSchemas(Class<?> cls) throws JAXBException {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(cls);
        scanPackages(classes);
        return JAXBContextCache.getCachedContextAndSchemas(classes, null, null, null, false);
    }
    
    public static CachedContextAndSchemas getCachedContextAndSchemas(String pkg,
                                                                     Map<String, Object> props,
                                                                     ClassLoader loader) 
        throws JAXBException {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        addPackage(classes, pkg, loader);
        return getCachedContextAndSchemas(classes, null, props, null, true);
    }
    
    public static CachedContextAndSchemas getCachedContextAndSchemas(final Set<Class<?>> classes,
                                                                     String defaultNs,
                                                                     Map<String, Object> props,
                                                                     Collection<Object> typeRefs,
                                                                     boolean exact)
        throws JAXBException {

        for (Class<?> clz : classes) {
            if (clz.getName().endsWith("ObjectFactory")
                && checkObjectFactoryNamespaces(clz)) {
                // kind of a hack, but ObjectFactories may be created with empty
                // namespaces
                defaultNs = null;
            }
        }

        Map<String, Object> map = new HashMap<String, Object>();
        if (defaultNs != null) {
            map.put("com.sun.xml.bind.defaultNamespaceRemap", defaultNs);
        }
        if (props != null) {
            map.putAll(props);
        }
        CachedContextAndSchemasInternal cachedContextAndSchemasInternal = null;

        synchronized (JAXBCONTEXT_CACHE) {
            JAXBContext context = null;
            if (typeRefs == null || typeRefs.isEmpty()) {
                if (exact) {
                    cachedContextAndSchemasInternal = JAXBCONTEXT_CACHE.get(classes);
                } else {
                    for (Map.Entry<Set<Class<?>>, CachedContextAndSchemasInternal> k : JAXBCONTEXT_CACHE.entrySet()) {
                        Set<Class<?>> key = k.getKey();
                        if (key != null && key.containsAll(classes)) {
                            cachedContextAndSchemasInternal = k.getValue();
                            break;
                        }
                    }
                }
                if (cachedContextAndSchemasInternal != null) {
                    context = cachedContextAndSchemasInternal.getContext();
                    if (context == null) {
                        JAXBCONTEXT_CACHE.remove(cachedContextAndSchemasInternal.getClasses());
                        cachedContextAndSchemasInternal = null;
                    } else {
                        return new CachedContextAndSchemas(cachedContextAndSchemasInternal);
                    }
                }        
            }
            if (context == null) {
                try {
                    context = createContext(classes, map, typeRefs);
                } catch (JAXBException ex) {
                    // load jaxb needed class and try to create jaxb context 
                    boolean added = addJaxbObjectFactory(ex, classes);
                    while (cachedContextAndSchemasInternal == null && added) {
                        try {
                            context = AccessController.doPrivileged(new PrivilegedExceptionAction<JAXBContext>() {
                                public JAXBContext run() throws Exception {
                                    return JAXBContext.newInstance(classes
                                                              .toArray(new Class[classes.size()]), null);
                                }
                            });
                        } catch (PrivilegedActionException e) {
                            throw ex;
                        }
                    }
                    if (context == null) {
                        throw ex;
                    }
                }
            }
            cachedContextAndSchemasInternal = new CachedContextAndSchemasInternal(context, classes);
            if (typeRefs == null || typeRefs.isEmpty()) {
                JAXBCONTEXT_CACHE.put(classes, cachedContextAndSchemasInternal);
            }            

            return new CachedContextAndSchemas(context, classes, cachedContextAndSchemasInternal);
        }
            
    }
    
    private static boolean checkObjectFactoryNamespaces(Class<?> clz) {
        for (Method meth : clz.getMethods()) {
            XmlElementDecl decl = meth.getAnnotation(XmlElementDecl.class);
            if (decl != null
                && XmlElementDecl.GLOBAL.class.equals(decl.scope())
                && StringUtils.isEmpty(decl.namespace())) {
                return true;
            }
        }

        return false;
    }
    
    
    private static JAXBContext createContext(final Set<Class<?>> classes,
                                      final Map<String, Object> map,
                                      Collection<Object> typeRefs)
        throws JAXBException {
        JAXBContext ctx;
        if (typeRefs != null && !typeRefs.isEmpty()) {
            Class<?> fact = null;
            String pfx = "com.sun.xml.bind.";
            try {
                fact = ClassLoaderUtils.loadClass("com.sun.xml.bind.v2.ContextFactory",
                                                  JAXBContextCache.class);
            } catch (Throwable t) {
                try {
                    fact = ClassLoaderUtils.loadClass("com.sun.xml.internal.bind.v2.ContextFactory",
                                                      JAXBContextCache.class);
                    pfx = "com.sun.xml.internal.bind.";
                } catch (Throwable t2) {
                    //ignore
                }
            }
            if (fact != null) {
                for (Method m : fact.getMethods()) {
                    if ("createContext".equals(m.getName())
                        && m.getParameterTypes().length == 9) {
                        try {
                            return (JAXBContext)m.invoke(null,
                                     classes.toArray(new Class[classes.size()]),
                                     typeRefs,
                                     map.get(pfx + "subclassReplacements"),
                                     map.get(pfx + "defaultNamespaceRemap"),
                                     map.get(pfx + "c14n") == null
                                         ? Boolean.FALSE
                                             : map.get(pfx + "c14n"),
                                     map.get(pfx + "v2.model.annotation.RuntimeAnnotationReader"),
                                     map.get(pfx + "XmlAccessorFactory") == null
                                         ? Boolean.FALSE
                                             : map.get(pfx + "XmlAccessorFactory"),
                                     map.get(pfx + "treatEverythingNillable") == null
                                         ? Boolean.FALSE : map.get(pfx + "treatEverythingNillable"),
                                     map.get("retainReferenceToInfo") == null
                                         ? Boolean.FALSE : map.get("retainReferenceToInfo"));
                        } catch (Throwable e) {
                            //ignore
                        }
                    }
                }
            }
        }
        try {
            ctx = AccessController.doPrivileged(new PrivilegedExceptionAction<JAXBContext>() {
                public JAXBContext run() throws Exception {
                    return JAXBContext.newInstance(classes.toArray(new Class[classes.size()]), map);
                }
            });
        } catch (PrivilegedActionException e2) {
            if (e2.getException() instanceof JAXBException) {
                JAXBException ex = (JAXBException)e2.getException();
                if (map.containsKey("com.sun.xml.bind.defaultNamespaceRemap")
                    && ex.getMessage() != null
                    && ex.getMessage().contains("com.sun.xml.bind.defaultNamespaceRemap")) {
                    map.put("com.sun.xml.internal.bind.defaultNamespaceRemap",
                            map.remove("com.sun.xml.bind.defaultNamespaceRemap"));
                    ctx = JAXBContext.newInstance(classes.toArray(new Class[classes.size()]), map);
                } else {
                    throw ex;
                }
            } else {
                throw new RuntimeException(e2.getException());
            }
        }
        return ctx;
    }
    // Now we can not add all the classes that Jaxb needed into JaxbContext,
    // especially when
    // an ObjectFactory is pointed to by an jaxb @XmlElementDecl annotation
    // added this workaround method to load the jaxb needed ObjectFactory class
    private static boolean addJaxbObjectFactory(JAXBException e1, Set<Class<?>> classes) {
        boolean added = false;
        java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
        java.io.PrintStream pout = new java.io.PrintStream(bout);
        e1.printStackTrace(pout);
        String str = new String(bout.toByteArray());
        Pattern pattern = Pattern.compile("(?<=There's\\sno\\sObjectFactory\\swith\\san\\s"
                                          + "@XmlElementDecl\\sfor\\sthe\\selement\\s\\{)\\S*(?=\\})");
        java.util.regex.Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            String pkgName = JAXBUtils.namespaceURIToPackage(matcher.group());
            try {
                Class<?> clz = JAXBContextCache.class.getClassLoader()
                    .loadClass(pkgName + "." + "ObjectFactory");

                if (!classes.contains(clz)) {
                    classes.add(clz);
                    added = true;
                }
            } catch (ClassNotFoundException e) {
                // do nothing
            }

        }
        return added;
    }

    public static void addPackage(Set<Class<?>>  classes, String pkg, ClassLoader loader) {
        try {
            classes.add(Class.forName(pkg + ".ObjectFactory", false, loader));
        } catch (Exception ex) {
            //ignore
        }
        try {
            InputStream ins = loader.getResourceAsStream("/" + pkg.replace('.', '/') + "/jaxb.index");
            BufferedReader reader = new BufferedReader(new InputStreamReader(ins, "UTF-8"));
            if (!StringUtils.isEmpty(pkg)) {
                pkg += ".";
            }
    
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                if (line.indexOf("#") != -1) {
                    line = line.substring(0, line.indexOf("#"));
                }
                if (!StringUtils.isEmpty(line)) {
                    try {
                        Class<?> ncls = Class.forName(pkg + line, false, loader);
                        classes.add(ncls);
                    } catch (Exception e) {
                        // ignore
                    }
                }
                line = reader.readLine();
            }
        } catch (Exception ex) {
            //ignore
        }
    }

}
