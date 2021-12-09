/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.web.config;

import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
//import com.ibm.ws.cache.Scheduler;
import com.ibm.ws.cache.config.CacheHandler;
import com.ibm.ws.cache.config.CacheInstance;
import com.ibm.ws.cache.config.CacheProcessor;
import com.ibm.ws.cache.config.ConfigEntry;
import com.ibm.ws.cache.config.ConfigErrorHandler;
import com.ibm.ws.cache.config.RuleHandler;
import com.ibm.ws.cache.servlet.ServletCacheEngine;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.util.ObjectPool;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;
//import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

@Component(service = ConfigManager.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM")
public class ConfigManager {

    private static TraceComponent tc = Tr.register(ConfigManager.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    static final String CACHE_SPEC = "cachespec.xml";
    static final String CACHE_SPEC_DTD = "cachespec.dtd";
    static final String CACHE_SPEC_XSD = "cachespec.xsd";

    static final String WEB_INF_CACHESPEC_XML = "/WEB-INF/cachespec.xml";
    static final String META_INF_CACHESPEC_XML = "/META-INF/cachespec.xml";

    public static final String SIMPLE_FILE_SERVLET = "com.ibm.ws.webcontainer.servlet.SimpleFileServlet.class";
    public static final String DEFAULT_EXTENSION_PROCESSOR_IMPL = "com.ibm.ws.webcontainer.osgi.extension.DefaultExtensionProcessor.class";
    private static StringBuilder cachespecDTDContents = new StringBuilder();
    private static StringBuilder cachespecXSDContents = new StringBuilder();

    private List<CacheInstance> globalCacheInstances = new ArrayList<CacheInstance>();
    private List<ConfigEntry> globalCacheEntries = new ArrayList<ConfigEntry>();
    private HashMap<String, ConfigEntry> entryCache = new HashMap<String, ConfigEntry>();
    private HashMap<String, ConfigEntry> servletConfigEntries = new HashMap<String, ConfigEntry>();
    private MultiProcessorPool processorPool = new MultiProcessorPool();

    static private ConfigManager instance;

    static public ConfigManager getInstance() {
        return instance;
    }

    @Activate
    protected void activate(BundleContext bc) {
/*
    	WsLocationAdmin locAdmin = Scheduler.getLocationAdmin();
    	File installRoot = new File(locAdmin.resolveString("${wlp.install.dir}"));
    	File cachespec_xsd = new File(installRoot, "dev/api/ibm/schema/cachespec.xsd");
    	BufferedReader in = null;
    	try { 
    		in = new BufferedReader( new FileReader (cachespec_xsd));		 
    		String         inputLine = null;
    		while( ( inputLine = in.readLine() ) != null ) {
    			cachespecXSDContents.append( inputLine );
    		}
    		in.close();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "activate: cachespecXSDContents loaded length="+cachespecXSDContents.length() );
            }

    	} catch (IOException e) {
    		try {
    			if (in != null) in.close();
    		} catch (IOException e1) {
    			FFDCFilter.processException(e1, this.getClass().getName() + ".activated()", "91");
    		}    		
    	}
  */  	    	
    	BufferedReader in = null;
        InputStreamReader isr = null;
        in = null;
        if (cachespecDTDContents.length() == 0) {// Retrieve cachespec.dtd
            try {
                URL u = bc.getBundle().getEntry("/META-INF/" + CACHE_SPEC_DTD);
                
                if (u != null) {
                  isr = new InputStreamReader(u.openStream(), StandardCharsets.UTF_8);
                  in = new BufferedReader(isr);
                  String inputLine;
                  while ((inputLine = in.readLine()) != null)
                    cachespecDTDContents.append(inputLine);
                  in.close();
                } 
            } catch (IOException e) {
                try {
                    if (isr != null) isr.close();
                    if (in != null) in.close();
                } catch (IOException e1) {
                    FFDCFilter.processException(e1, this.getClass().getName() + ".activated()", "93");
                }
            }
        }

        instance = this;
    }

    @Deactivate
    protected void deactivate() {
        globalCacheEntries.clear();
        globalCacheInstances.clear();
        entryCache.clear();
        servletConfigEntries.clear();
        processorPool.removePools();
        instance = null;
    }

    // Used by servlet/jsp cache only
    public ConfigEntry getServletConfigEntry(Object s, String uri, String contextRoot) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getServletCacheEntry() uri=" + uri + " contextRoot=" + contextRoot);
        }

        Class sClazz = s.getClass();
        String clazzName = sClazz.getName() + ".class";
        ConfigEntry ce = null;
        String key;
        if (uri != null)
            ce = (ConfigEntry) servletConfigEntries.get(uri);
        if (ce == null) {
            key = contextRoot == null ? clazzName : (contextRoot + "/" + clazzName);
            ce = (ConfigEntry) servletConfigEntries.get(key);
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "using config entry: " + ce);

        return ce;
    }

    public ConfigEntry getConfigEntry(Object s, String name, String contextRoot) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getCacheEntry() name=" + name + " contextRoot=" + contextRoot);
        }
        Class sClazz = s.getClass();
        String entryCacheKey = sClazz.getName() + (name != null ? name : "null") + (contextRoot != null ? contextRoot : "null");
        ConfigEntry ce = (ConfigEntry) entryCache.get(entryCacheKey);
        if (ce == null) {
            String clazzName = sClazz.getName() + ".class";
            String prefixedClazzName = contextRoot == null ? clazzName : (contextRoot + "/" + clazzName);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Searching config entries for 1of2 " + name);
                Tr.debug(tc, "Searching config entries for 2of2 " + prefixedClazzName);
            }
            synchronized (globalCacheEntries) {
                for (int i = 0; i < globalCacheEntries.size() && ce == null; i++) {
                    ConfigEntry te = (ConfigEntry) globalCacheEntries.get(i);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "checking " + te.className + " " + te.name + ": " + te.allNames);
                    // Note: prefixedClazzName will be the original clazzName when passed contextRoot is null.
                    if (te.allNames.contains(name) || te.allNames.contains(prefixedClazzName)) {
                        ce = (ConfigEntry) te.clone();
                        synchronized (entryCache) {
                            entryCache.put(entryCacheKey, ce);
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "getCacheEntry: entryCacheKey=" + entryCacheKey + " cacheEntry=" + ce.toString());
                        }
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Config Entry cached using key " + entryCacheKey);
                    }
                }
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "using config entry: " + ce);
        }
        return ce;
    }

    public ConfigEntry getConfigEntry(Object s, String name) {
        return getConfigEntry(s, name, null);
    }

    public CacheProcessor getCacheProcessor(ConfigEntry ce) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getCacheProcessor");
        if (ce == null)
            return null;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "iClassName=" + ce.iClassName);
        CacheProcessor cp;
        if (ce.iClassName == ConfigEntry.STATIC || ce.iClassName == ConfigEntry.PORTLET)
            cp = (CacheProcessor) processorPool.pools[ConfigEntry.SERVLET].remove();
        else
            cp = (CacheProcessor) processorPool.pools[ce.iClassName].remove();
        cp.reset(ce);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getCacheProcessor: " + cp);
        return cp;
    }

    public void returnCacheProcessor(CacheProcessor cp) {
        ConfigEntry configEntry = cp.getConfigEntry();
        if (configEntry.iClassName == ConfigEntry.STATIC || configEntry.iClassName == ConfigEntry.PORTLET)
            processorPool.pools[ConfigEntry.SERVLET].add(cp);
        else
            processorPool.pools[configEntry.iClassName].add(cp);
    }

    /**
     * loadConfig
     * 
     * @param fileName
     *            full path to module file to load
     * @param warFile
     *            true if module is a war file
     * @param isModule
     *            true if the filename passed in point to a module file used to determine if WEB-INF/cachespec.xml or
     *            META-INF/cachespec.xml should be appended.
     * @param appName
     *            unique string which identifies this application
     * @param appContext
     *            application specific context data based on the cache entry class. for a servlet class, this will be
     *            web context root
     */

    public boolean loadConfig(ServletContext sc, String appName, HashMap appContext) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "loadConfig()", new Object[] { sc, appName, appContext });
        
        URL u = null;
        
        try {
            u = sc.getResource(WEB_INF_CACHESPEC_XML);
            if (null == u) {
                u = sc.getResource(META_INF_CACHESPEC_XML);
            }            
        } catch (Exception ex ) {
        	if (tc.isDebugEnabled())
                Tr.debug(tc, "dynacache config error", ex);	        	
        }
              
        if (null != u) {
            ConfigFileEntry cf = new ConfigFileEntry();
            cf.appContext = appContext;
            cf.appName = appName;
            CacheHandler cacheHandler = new CacheHandler(appName, appContext);
            String configFileName = u.toString();
            String fileName = u.toString();
            InputStream in = null;
            try {
                XMLReader xmlreader = null;
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();
                xmlreader = saxParser.getXMLReader();

                RuleHandler ruleHandler = new RuleHandler();
                ruleHandler.addRule("cache", cacheHandler);
                //xmlreader.setFeature("http://xml.org/sax/features/validation", true);
                xmlreader.setContentHandler(ruleHandler);
                xmlreader.setErrorHandler(new ConfigErrorHandler(u));
                if (determineCachespecXSD(u)) {
                	//xmlreader.setEntityResolver(new CacheSpecEntityResolverXSD());
                } else {
                    xmlreader.setFeature("http://apache.org/xml/features/validation/schema", true);
                 	xmlreader.setEntityResolver(new CacheSpecEntityResolver());
                } 	
                cacheHandler.setFilename(configFileName);
                in = u.openStream();
                InputSource is = new InputSource(in);
                xmlreader.parse(is);
                updateCacheInstances(cf, cacheHandler, configFileName);
                updateEntries(cf, cacheHandler, configFileName);
            } catch (SAXParseException exception) {
                com.ibm.ws.ffdc.FFDCFilter.processException(exception, "com.ibm.ws.cache.config.ConfigManager.loadConfig", "295", this);
                String msg = exception.getMessage();
                String line = Integer.toString(exception.getLineNumber());
                String col = Integer.toString(exception.getColumnNumber());
                Tr.error(tc, "DYNA0045E", new Object[] { msg, fileName, line, col });
                Tr.error(tc, "DYNA0043E", new Object[] { fileName + " (" + configFileName + ")" });
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "dynacache config error", exception);
            } catch (IOException ex) {
                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.ConfigManager.loadConfig", "303", this);
                com.ibm.ws.ffdc.FFDCFilter.processException(unwindException(ex), "com.ibm.ws.cache.config.ConfigManager.loadConfig", "304", this);
                Tr.error(tc, "DYNA0046E", new Object[] { ex.getMessage(), fileName });
                Tr.error(tc, "DYNA0043E", new Object[] { fileName + " (" + configFileName + ")" });
                ArrayList configEntries = cacheHandler.getEntries();
                if (configEntries != null) {
                    Iterator it = configEntries.iterator();
                    while (it.hasNext()) {
                        ConfigEntry cEntry = (ConfigEntry) it.next();
                    }
                }
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "dynacache config error", ex);
            } catch (Exception ex) {
                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.ConfigManager.loadConfig", "308", this);
                Tr.error(tc, "DYNA0043E", new Object[] { fileName + " (" + configFileName + ")" });
                ArrayList configEntries = cacheHandler.getEntries();
                if (configEntries != null) {
                    Iterator it = configEntries.iterator();
                    while (it.hasNext()) {
                        ConfigEntry cEntry = (ConfigEntry) it.next();
                    }
                }
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "dynacache config error", ex);

            } finally {
                try {
                    if (in != null) in.close();
                } catch (IOException e) {
                    FFDCFilter.processException(e, this.getClass().getName() + ".loadConfig()", "294");
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "loadConfig()", true);
        return true;
    }
    
    private boolean determineCachespecXSD(URL u) {

    	BufferedReader in = null;
    	InputStreamReader isr = null;
    	in = null;
		boolean schema=false;
		
    	try {
    		if (u != null) {
    			isr = new InputStreamReader(u.openStream(), StandardCharsets.UTF_8);
    			in = new BufferedReader(isr);
    			String inputLine;

    			while ((inputLine = in.readLine()) != null) {
    				if (inputLine.contains("cachespec.xsd")) {
    					schema=true;
    					break;
    				} else if (inputLine.contains("cachespec.dtd")) {
    					schema=false;
    					break;
    				}
    			}
    			in.close();
    			if (schema)
    			   cachespecDTDContents = cachespecXSDContents;
    		} 
    	} catch (IOException e) {
    		try {
    			if (isr != null) isr.close();
    			if (in != null) in.close();
    		} catch (IOException e1) {
    			FFDCFilter.processException(e1, this.getClass().getName() + ".determineCachespec()", "93");
    		}
    	}
    	return schema;
    }    

    private void updateEntries(ConfigFileEntry configFileEntry, CacheHandler cacheHandler, String configFileName) {

        configFileEntry.configEntries = cacheHandler.getEntries();
        WebAppConfig webAppConfig = null;
        if (configFileEntry.appContext != null) {
            webAppConfig = (WebAppConfig) configFileEntry.appContext.get("webAppConfiguration");
        }
        Set<Map.Entry<String, List<String>>> servletMappingEntries = null;
        if (null != webAppConfig) {
            servletMappingEntries = webAppConfig.getServletMappings().entrySet();
        }

        Iterator<ConfigEntry> it = configFileEntry.configEntries.iterator();
        while (it.hasNext()) {

            ConfigEntry cEntry = (ConfigEntry) it.next();

            if (null != servletMappingEntries)
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Servlet caching enabled for " + webAppConfig.getContextRoot());
                }
            ServletCacheEngine.contextRootsWithCachespecXMLs.add(webAppConfig.getContextRoot());

            if (!preProcessEntry(cEntry)) {
                it.remove();
            } else {
                cEntry.appName = configFileEntry.appName;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "cache policy:" + cEntry);
                }

                // Is a static policy exists, save the URI for which
                // webcontainer must use
                // SimpleFileServlet instead of DefaultExtensionProcessor.
                try {
                    if (cEntry.className.equalsIgnoreCase("static") || cEntry.name.endsWith(SIMPLE_FILE_SERVLET)) {
                        String contextRoot = null;
                        if (configFileEntry.appContext != null) {
                            contextRoot = (String) configFileEntry.appContext.get("servlet");
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "updateEntries: contextRoot:" + contextRoot);
                            }
                            if (contextRoot != null) {
                                if (contextRoot.endsWith("/") && contextRoot.length() > 2) {
                                    contextRoot = contextRoot.substring(0, contextRoot.length() - 1);
                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, "updateEntries: invoking addStaticContentPolicy:" + contextRoot);
                                    }
                                }
                                ServletCacheEngine.addStaticContentPolicy(contextRoot);
                            }
                        }

                        if (contextRoot == null) {
                            int pos = cEntry.name.indexOf("/", 1);
                            if (pos == -1) {
                                ServletCacheEngine.addStaticContentPolicy("/");
                            } else {
                                contextRoot = cEntry.name.substring(0, pos);
                                if (!contextRoot.startsWith("/"))
                                    contextRoot = "/" + contextRoot;
                                ServletCacheEngine.addStaticContentPolicy(contextRoot);
                            }
                        }

                        // In WASv7 the impl class for static resources was
                        // changed from
                        // com.ibm.ws.webcontainer.servlet.SimpleFileServlet To
                        // com.ibm.ws.webcontainer.extension.DefaultExtensionProcessorImpl
                        // Therefore in order to preserve backward compatibility
                        // we have to
                        // store
                        // com.ibm.ws.webcontainer.extension.DefaultExtensionProcessorImpl
                        // as an alternative name, so that both
                        // SimpleFileServlet (pre WAS7)and
                        // DefaultExtensionProcessorImpl(post WAS7) work when
                        // ConfigEntries are
                        // retrieved in getServletCacheEntry(Object, String,
                        // String)
                        String altCEName = String.copyValueOf(cEntry.name.toCharArray());
                        String alternateName = altCEName.replace(SIMPLE_FILE_SERVLET, DEFAULT_EXTENSION_PROCESSOR_IMPL);
                        cEntry.allNames.add(alternateName);
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Added " + alternateName + " to " + cEntry.name + " alternative names list: ");
                        }
                    } // end of if loop
                } catch (Exception ex) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.ConfigManager.checkConfig", "110", this);
                }
            }
        }

        // now that we have read the new config file, update
        // the global list of cache entries
        globalCacheEntries.addAll(configFileEntry.configEntries);

        // build config entries hashtable for JAX-RPC client
        HashMap<String, ConfigEntry> newServletConfigEntries = null;

        for (int i = 0; i < globalCacheEntries.size(); i++) {
            ConfigEntry tmpConfigEntry = (ConfigEntry) globalCacheEntries.get(i);
            if (tmpConfigEntry.iClassName == ConfigEntry.SERVLET || tmpConfigEntry.iClassName == ConfigEntry.WEB_SERVICE
                    || tmpConfigEntry.iClassName == ConfigEntry.STATIC) {
                if (newServletConfigEntries == null)
                    newServletConfigEntries = new HashMap<String, ConfigEntry>();
                if (tmpConfigEntry != null) {
                    Iterator ite = tmpConfigEntry.allNames.iterator();
                    while (ite.hasNext()) {
                        ConfigEntry cloneConfigEntry = (ConfigEntry) tmpConfigEntry.clone();
                        String name = (String) ite.next();
                        cloneConfigEntry.name = name;
                        newServletConfigEntries.put(name, cloneConfigEntry);
                        if (tmpConfigEntry.iClassName == ConfigEntry.STATIC) {
                            Tr.debug(tc, "ServletConfigEntry " + name);
                        }
                    }
                }

            }
        }

        if (newServletConfigEntries == null)
            servletConfigEntries.clear();
        else
            servletConfigEntries = newServletConfigEntries;

        Tr.debug(tc, "servletConfigEntries: \n" + servletConfigEntries.toString());

        // clear cooperating cache processor cache entries
        synchronized (entryCache) {
            entryCache.clear();
            if (tc.isDebugEnabled())
                Tr.entry(tc, "updateEntries entryCache cleared");
        }

        // lose the reference to the webapp configuration
        configFileEntry.appContext.remove("webAppConfiguration");

        Tr.info(tc, "DYNA0047I", new Object[] { configFileName });
        return;
    }

    private void updateCacheInstances(ConfigFileEntry cf, CacheHandler cacheHandler, String configFileName) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "updateCacheInstance");

        cf.cacheInstances = cacheHandler.getInstances();

        Iterator it = cf.cacheInstances.iterator();
        while (it.hasNext()) {
            CacheInstance cInstance = (CacheInstance) it.next();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "cache instance:" + cInstance);
            }
        }

        // now that we have read the new config file, update
        // the global list of cache instances
        globalCacheInstances.addAll(cf.cacheInstances);
        Tr.info(tc, "DYNA0062I", new Object[] { configFileName });

        if (tc.isEntryEnabled())
            Tr.exit(tc, "updateCacheInstance");
        return;
    }

    public List<CacheInstance> getCacheInstances() {
        return globalCacheInstances;
    }

    public List<ConfigEntry> getEntries() {
        return globalCacheEntries;
    }

    /**
     * Used to get the configuration entries belonging to a specified cache instance To get the base cache's
     * configuration entries pass null for the instanceName
     */
    public List<ConfigEntry> getCacheEntries(String instanceName) {
        ArrayList<ConfigEntry> cacheEntries = new ArrayList<ConfigEntry>();

        synchronized (globalCacheEntries) {
            Iterator<ConfigEntry> it = globalCacheEntries.iterator();
            if (instanceName == null) {
                while (it.hasNext()) {
                    ConfigEntry ce = it.next();
                    if (ce.instanceName == null)
                        cacheEntries.add(ce);
                }
            } else {
                while (it.hasNext()) {
                    ConfigEntry ce = it.next();
                    if (ce.instanceName != null && ce.instanceName.equals(instanceName))
                        cacheEntries.add(ce);
                }
            }
        }
        return cacheEntries;
    }

    public void deleteAppNameCacheEntries(String appName) {

        if (tc.isDebugEnabled())
            Tr.entry(tc, "deleteAppNameCacheEntries appName=" + appName);
        int i = 0;
        synchronized (globalCacheEntries) {
            Iterator<ConfigEntry> it = globalCacheEntries.iterator();

            while (it.hasNext()) {
                ConfigEntry ce = (ConfigEntry) it.next();
                if (ce.appName != null && ce.appName.equals(appName)) {
                    it.remove();
                    i++;
                }
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Deleted " + i + " cache entries");
        }

        synchronized (entryCache) {
            Iterator<Map.Entry<String, ConfigEntry>> it = entryCache.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ConfigEntry> pairs = it.next();
                String key = (String) pairs.getKey();
                ConfigEntry ce = (ConfigEntry) pairs.getValue();

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "deleteAppNameCacheEntries() " + key + "=" + ce.toString());
                }

                if (ce.appName != null && ce.appName.equals(appName)) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "deleteAppNameCacheEntries() removing cacheEntry=" + ce.name + " appName=" + appName + " key=" + key);
                    it.remove();
                }
            }
        }

        if (tc.isDebugEnabled())
            Tr.exit(tc, "deleteAppNameCacheEntries " + appName);
    }

    /**
     * Used to validate and preprocess (e.g. properties) configuration entries
     */
    protected boolean preProcessEntry(ConfigEntry configEntry) {
        boolean valid = true;
        if (configEntry.className.equalsIgnoreCase("servlet"))
            configEntry.iClassName = ConfigEntry.SERVLET;
        else if (configEntry.className.equalsIgnoreCase("command"))
            configEntry.iClassName = ConfigEntry.COMMAND;
        else if (configEntry.className.equalsIgnoreCase("webservice"))
            configEntry.iClassName = ConfigEntry.WEB_SERVICE;
        else if (configEntry.className.equalsIgnoreCase("static"))
            configEntry.iClassName = ConfigEntry.STATIC;
        else {
            Tr.error(tc, "DYNA0050E", new Object[] { configEntry.className, "class" });
            valid = false;
        }
        if (valid) {
            CacheProcessor cp = getCacheProcessor(configEntry);
            valid = cp.preProcess(configEntry);
            returnCacheProcessor(cp);
        }
        return valid;
    }

    static class CacheSpecEntityResolver implements org.xml.sax.EntityResolver {
        public InputSource resolveEntity(String publicId, String systemId) {
            if (tc.isDebugEnabled())
                 Tr.debug(tc, "CacheSpecEntityResolver: ", cachespecDTDContents.toString());
            return new InputSource(new StringReader(cachespecDTDContents.toString()));
        }
    }
    
    static class CacheSpecEntityResolverXSD implements org.xml.sax.EntityResolver {
        public InputSource resolveEntity(String publicId, String systemId) {
            if (tc.isDebugEnabled())
                 Tr.debug(tc, "CacheSpecEntityResolver: ", cachespecXSDContents.toString());
            return new InputSource(new StringReader(cachespecXSDContents.toString()));
        }
    }

    static class CacheProcessorPool extends ObjectPool {
        Class myClass;

        public CacheProcessorPool(int size, String className) {
            super("CommandCacheProcessorPool", size);
            try {
                myClass = ConfigManager.class.getClassLoader().loadClass(className);
            } catch (ClassNotFoundException ex) {
                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.ConfigManager.CacheProcessorPool", "378", this);
            }
        }

        protected Object createObject() {
            try {
                return myClass.newInstance();
            } catch (Exception ex) {
                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.ConfigManager.CacheProcessorPool", "382", this);
            }
            return null;
        }

        public boolean add(CacheProcessor cp) {
            cp.reset(null);
            return super.add(cp);
        }

    }

    static class MultiProcessorPool {

        CacheProcessorPool pools[] = new CacheProcessorPool[16];

        MultiProcessorPool() {
            addPool(ConfigEntry.SERVLET, "com.ibm.ws.cache.servlet.FragmentCacheProcessor");
            addPool(ConfigEntry.WEB_SERVICE, "com.ibm.ws.cache.servlet.WebServicesCacheProcessor");
            addPool(ConfigEntry.COMMAND, "com.ibm.ws.cache.command.CommandCacheProcessor");
        }

        public void removePools() {
            pools[ConfigEntry.SERVLET] = null;
            pools[ConfigEntry.WEB_SERVICE] = null;
            pools[ConfigEntry.COMMAND] = null;
        }

        public void addPool(int index, String className) {
            try {
                pools[index] = new CacheProcessorPool(100, className);
            } catch (Exception ex) {
                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.config.ConfigManager.MultiProcessorPool", "402", this);
            }
        }
    }

    public static Throwable unwindException(Throwable th) {
        if (th.getCause() != null) {
            return unwindException(th.getCause());
        }
        return th;
    }

}

class ConfigFileEntry {
    String appName;
    HashMap appContext;
    ArrayList<ConfigEntry> configEntries;
    ArrayList<CacheInstance> cacheInstances;
}
