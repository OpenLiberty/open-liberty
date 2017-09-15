/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.taglib;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.jsp.tagext.TagFileInfo;
import javax.servlet.jsp.tagext.TagInfo;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfigurationManager;
import com.ibm.ws.jsp.configuration.JspXmlExtConfig;
import com.ibm.ws.jsp.inputsource.JspInputSourceContainerImpl;
import com.ibm.ws.jsp.translator.JspTranslator;
import com.ibm.ws.jsp.translator.JspTranslatorFactory;
import com.ibm.ws.jsp.translator.optimizedtag.OptimizedTag;
import com.ibm.ws.jsp.translator.optimizedtag.OptimizedTagConfig;
import com.ibm.ws.jsp.translator.optimizedtag.OptimizedTagConfigParser;
import com.ibm.ws.jsp.translator.utils.NameMangler;
import com.ibm.ws.jsp.translator.utils.TagFileId;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.tagfilescan.TagFileScanResult;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.jsp.context.translation.JspTranslationContext;
import com.ibm.wsspi.jsp.resource.JspInputSource;
import com.ibm.wsspi.jsp.resource.translation.TagFileResources;
import com.ibm.wsspi.webcontainer.WCCustomProperties;

public class TagLibraryCache extends Hashtable<String, Object> {

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3256719585204975926L;
    static private Logger logger;
    private static final String CLASS_NAME="com.ibm.ws.jsp.taglib.TagLibraryCache";
    static{
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }

    static final String TAGFILE_SCAN_ID = "TagFileScan";

    public static final int ABS_URI = 0;
    public static final int ROOT_REL_URI = 1;
    public static final int NOROOT_REL_URI = 2;
    private JspTranslationContext ctxt = null;
    private TldParser tldParser = null;
    private Map tagClassMap = new HashMap();
    private Map tagFileResourcesMap = new HashMap();
    private Map implicitTagLibPrefixMap = new HashMap();
    private Map optimizedTagConfigMap = null;
    private Map<String, String> looseLibMap = null;
    private List<String> eventListenerList = new ArrayList<String>();
    private List<String> tagListForInjection = new ArrayList<String>();
    private JspConfigurationManager configManager = null;
    //private String outputDir = null;
    private Map tagFileLockMap = null;
    private JspOptions jspOptions = null; //396002
    private Container container = null;
    
    private Map locationsCache = null;

    //AbstractJspModC.java uses this constructor
    public TagLibraryCache(JspTranslationContext ctxt,
                           Map webxmlTagLibMap,
                           JspOptions jspOptions,
                           JspConfigurationManager configManager,
                           Map<String, Object> globalMap,
                           Map implicitMap,
                           Map optimizedTagMap) throws JspCoreException {
        this(ctxt,
        null, //container
        webxmlTagLibMap,
        jspOptions,
        configManager,
        globalMap,
        implicitMap,
        optimizedTagMap,
        null);
    }

    public TagLibraryCache(JspTranslationContext ctxt,
                           Map webxmlTagLibMap,
                           JspOptions jspOptions,
                           JspConfigurationManager configManager,
                           Map<String, Object> globalMap,
                           Map implicitMap,
                           Map optimizedTagMap,
                           WebAppConfiguration webAppConfig) throws JspCoreException {
        this(ctxt,
        null, //container
        webxmlTagLibMap,
        jspOptions,
        configManager,
        globalMap,
        implicitMap,
        optimizedTagMap,
        webAppConfig);
    }
    
    public TagLibraryCache(Container cont,
                           Map webxmlTagLibMap,
                           JspOptions jspOptions,
                           JspConfigurationManager configManager,
                           Map<String, Object> globalMap,
                           Map implicitMap,
                           Map optimizedTagMap,
                           WebAppConfiguration webAppConfig) throws JspCoreException {
        this(null,//context
        cont, 
        webxmlTagLibMap,
        jspOptions,
        configManager,
        globalMap,
        implicitMap,
        optimizedTagMap,
        webAppConfig);
    }
    
    private TagLibraryCache(JspTranslationContext ctxt,
                           Container cont,
                           Map webxmlTagLibMap,
                           JspOptions jspOptions,
                           JspConfigurationManager configManager,
                           Map<String, Object> globalMap,
                           Map implicitMap,
                           Map optimizedTagMap,
                           WebAppConfiguration webAppConfig) throws JspCoreException {
        this.ctxt = ctxt;
        this.configManager = configManager;
        this.jspOptions = jspOptions; // 396002
        //outputDir = jspOptions.getOutputDir().getPath();
        if (cont!=null) {
            container = cont;    
        } else {
            container = ctxt.getServletContext().getModuleContainer(); //null if no loose container
        }

        if (webAppConfig != null && webAppConfig.getJspCachedLocations() != null) {
            locationsCache = (HashMap<JspInputSource, CachedLocationsWrapper>) webAppConfig.getJspCachedLocations();
        } else
            locationsCache = new HashMap<JspInputSource, CachedLocationsWrapper>();
        
        // defect 208800
        if (jspOptions.getLooseLibMap() != null) {
            looseLibMap = jspOptions.getLooseLibMap();
        }
        tldParser = new TldParser(ctxt, configManager, false);
        //Not caching this because there is a particular case that uses ctxt.
        //It is not as bad because we are just looping against known locations.
        List loadedLocations = loadWebXmlMap(webxmlTagLibMap);
        
        //PK69220 - start
        if(logger.isLoggable(Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME, "TagLibraryCache", "disableTldSearch is set to: " + jspOptions.isDisableTldSearch());
        }
        if (!jspOptions.isDisableTldSearch()) {
        //PK69220 - end
            if (!locationsCache.isEmpty()) {
                useCachedLocations();
            } else {
                loadLibJarMap(loadedLocations);
                loadWebInfMap("/WEB-INF", loadedLocations);
                loadSharedLibMap(loadedLocations);
                if (webAppConfig != null)
                    webAppConfig.setJspCachedLocations(locationsCache);
            }
            
            if (ctxt!=null) {
                //don't need to load these if we've been passed a container since we're just gathering classes for injection
                loadWebInfTagFiles("/WEB-INF/tags");
            }
        } //PK69220

        //this is called once (depends on having a JSP application context)
        //PK68590 start
        if (looseLibMap != null) {
            for (Map.Entry<String, String> looseEntry : looseLibMap.entrySet()) {
                loadLooseLibTagFiles(looseEntry.getValue(), loadedLocations, looseEntry.getKey());  //PM07608	//PM03123
            }
        }
        //PK68590 end

        if (jspOptions.isUseImplicitTagLibs() &&
             (jspOptions.getTranslationContextClass() == null 
              	|| (jspOptions.getTranslationContextClass() != null && // 415289
              	    jspOptions.getTranslationContextClass().equals(Constants.IN_MEMORY_TRANSLATION_CONTEXT_CLASS)))) { 
            for (Map.Entry<String, Object> mapEntry : globalMap.entrySet()) {
                String uri = mapEntry.getKey();
                if (!containsKey(uri)) {
                    Object o = mapEntry.getValue();
                    if (o instanceof TagLibraryInfoImpl) {
                        TagLibraryInfoImpl impl = (TagLibraryInfoImpl)o;
                        TagLibraryInfoImpl tli = impl.copy("");
                        put(uri, tli);
                    }
                    else if (o instanceof TagLibraryInfoProxy) {
                        put(uri, o);
                    }
                }
            }
            implicitTagLibPrefixMap.putAll(implicitMap);
        }

        //depends on ctxt
        if (jspOptions.isUseOptimizedTags()) {
            JspInputSource inputSource = getInputSource(container, "/WEB-INF/optimizedtags.xml", null, null);
            try {
                InputStream is = inputSource.getInputStream();
                if (is != null) {
                    OptimizedTagConfigParser optimizedTagConfigParser = new OptimizedTagConfigParser(ctxt);
                    optimizedTagConfigMap = optimizedTagConfigParser.parse(is);
                }
                else {
                    optimizedTagConfigMap = new HashMap();
                }
            }
            catch (IOException e) {
                optimizedTagConfigMap = new HashMap();
            }
            optimizedTagConfigMap.putAll(optimizedTagMap);
        }
    }
    
    private JspInputSource getInputSource(Container container, String taglibLocation, URLStreamHandler streamHandler, URL url) {
        JspInputSource inputSource = null;
        //streamHandler is most likely null
        if(container!=null){ //If given a container, then search that container for the tag
            inputSource = new JspInputSourceContainerImpl(container, taglibLocation, streamHandler, null);
        } else if (ctxt!=null) {
            inputSource = ctxt.getJspInputSourceFactory().createJspInputSource(url, taglibLocation); 
        } 
  
        return inputSource;
    }
    
    //only called when you have a context
    public void loadWebInfTagFiles() throws JspCoreException {
        loadWebInfTagFiles("/WEB-INF/tags");
    }

    private List loadWebXmlMap(Map webxmlTagLibMap) throws JspCoreException {
        String originatorId = "webinf";
        List loadedLocations = new ArrayList();
        for (Iterator itr = webxmlTagLibMap.keySet().iterator(); itr.hasNext();) {
            String taglibUri = (String)itr.next();
            if (containsKey(taglibUri) == false) {
                String taglibLocation = (String)webxmlTagLibMap.get(taglibUri);
                TagLibraryInfoImpl tli = null;
                String loadedLocation = taglibLocation;
                if (taglibLocation.endsWith(".jar")) {
                    tli = loadTaglibTldFromJar(taglibLocation);
                    loadedLocation = loadedLocation + "/META-INF/taglib.tld";
                }
                else {
                    
                    JspInputSource inputSource = getInputSource(container, taglibLocation, null, null);
                    //tli = loadSerializedTld(inputSource, inputSource);
                    //if (tli == null) {
                    try {
                        tli = tldParser.parseTLD(inputSource, originatorId);
                        //if (tli != null) {
                        //    serializeTld(inputSource, (TagLibraryInfoImpl)tli);
                        //}
                    }
                    catch (JspCoreException e) {
                        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                            logger.logp(Level.WARNING, CLASS_NAME, "loadWebXmlMap", "jsp warning failed to load tld at ["+taglibLocation+"]");
                        }
                    }
                    //}
                }
                if (tli == null) {
                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                        logger.logp(Level.WARNING, CLASS_NAME, "loadWebXmlMap", "jsp warning failed to load tld at ["+taglibLocation+"]");
                    }
                }
                else {
                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                        logger.logp(Level.FINE, CLASS_NAME, "loadWebXmlMap", "webxml tld loaded for {0}", taglibLocation);
                    }
                    put(taglibUri, tli);
                    tli.setURI(taglibUri);
                    loadedLocations.add(loadedLocation);
                    eventListenerList.addAll(tldParser.getEventListenerList());
                    tagListForInjection.addAll(tldParser.getParsedTagsList());
                }
            }
        }
        return loadedLocations;
    }

    private void loadLibJarMapRecursivelyForContainer(List loadedLocations, Container container) throws UnableToAdaptException {
        for (com.ibm.wsspi.adaptable.module.Entry subEntry : container) {
            Container subEntryContainer = subEntry.adapt(Container.class);
            //TODO: this could potentially be a jar represented by a directory on disk
            if (subEntryContainer!=null && (WCCustomProperties.APPEND_METAINF_RESOURCES_IN_LOOSE_LIB ? (subEntry.getSize()==0) : (subEntryContainer.isRoot() == false))) { //PM99163 a directory
                loadLibJarMapRecursivelyForContainer(loadedLocations, subEntryContainer);
            } else {
                String libEntryString = subEntry.getPath();
                if (libEntryString.endsWith(".jar")) {
                    loadTldsFromJar(subEntryContainer, libEntryString, loadedLocations, null);
                }
            }
        }
    }
    
    private void loadLibJarMap(List loadedLocations) {
        if (container!=null) {
            com.ibm.wsspi.adaptable.module.Entry libDir = container.getEntry("/WEB-INF/lib");
            if (libDir!=null) {
                try {
                    Container libDirContainer = libDir.adapt(Container.class);
                    if (libDirContainer!=null) {
                        loadLibJarMapRecursivelyForContainer(loadedLocations, libDirContainer);
                    }
                } catch (UnableToAdaptException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        } else {
            // No need to search META-INF resources
            Set libSet = ctxt.getResourcePaths("/WEB-INF/lib",false);
            if (libSet != null) {
                Iterator it = libSet.iterator();
                while (it.hasNext()) {
                    String resourcePath = (String) it.next();
                    if (resourcePath.endsWith(".jar")) {
                        loadTldsFromJar(resourcePath, loadedLocations);
                    }
                }
            }
        }
    }

    protected void loadTldsFromJar(String resourcePath, List loadedLocations) {
        JspInputSource inputSource = getInputSource(container, resourcePath, null, null);
        Container jarContainer = ((JspInputSourceContainerImpl)inputSource).getContainer();
        loadTldsFromJar(jarContainer, resourcePath, loadedLocations, null);
    }
    //called from AbstractJspModC, no need to cache
    public void loadTldsFromJar(URL url, String resourcePath, List loadedLocations, JspXmlExtConfig webAppConfig) {
        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME, "loadTldsFromJar", "url ["+url+"]"+"resourcePath ["+resourcePath+"] loadedLocations ["+loadedLocations+"] webAppConfig ["+webAppConfig+"]" );
        }
        JarFile jarFile = null;
        InputStream stream = null;
        String name = null;
        try {
            JarURLConnection conn = (JarURLConnection)url.openConnection();
            conn.setUseCaches(false);
            jarFile = conn.getJarFile();

            String originatorId = jarFile.getName();
            originatorId = originatorId.substring(0, originatorId.indexOf(".jar"));
            if (originatorId.indexOf(File.separatorChar) != -1)
                originatorId = originatorId.substring(originatorId.lastIndexOf(File.separatorChar)+1);
            originatorId = NameMangler.mangleString(originatorId);

            Enumeration entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = (JarEntry) entries.nextElement();
                name = entry.getName();
                if (name.startsWith("META-INF/") &&
                    name.endsWith(".tld") &&
                    loadedLocations.contains(resourcePath + "/" + name) == false) {
                    stream = jarFile.getInputStream(entry);
                    JspInputSource tldInputSource = getInputSource(container, name, null, url);
                    //TagLibraryInfoImpl tli = loadSerializedTld(tldInputSource, inputSource);

                    //if (tli == null) {
                    try {
                        TagLibraryInfoImpl tli = tldParser.parseTLD(tldInputSource, stream, originatorId);
                        
                        //516822 - If no URI is defined in the tag, we still want to load it in case it has listeners
                        //use the resourcePath + "/" + name as the key
                        String uri = null;
                        if (tli.getReliableURN() != null && tli.getReliableURN().trim().equals("") == false) {
                            uri = tli.getReliableURN();
                        } else {
                            uri = resourcePath + "/" + name;
                        }
                        
                        //if (tli.getReliableURN() != null && tli.getReliableURN().trim().equals("") == false) {
                            tli.setURI(uri);
                            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                                logger.logp(Level.FINE, CLASS_NAME, "loadTldsFromJar", "webAppConfig is "+webAppConfig);
                            }
                            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE) && webAppConfig!=null){
                                logger.logp(Level.FINE, CLASS_NAME, "loadTldsFromJar", "tli URN is "+ uri+
                                                                                                                                           " :webAppConfig.getTagLibMap() is "+webAppConfig.getTagLibMap()+
                                                                                                                                           " :webAppConfig.getTagLibMap().containsKey(uri) is "+webAppConfig.getTagLibMap().containsKey(uri)+
                                                                                                                                           " :containsKey(uri) is "+containsKey(uri) );
                            }
                            if ((webAppConfig!=null && webAppConfig.getTagLibMap().containsKey(uri)==false)
                                    || (webAppConfig==null && containsKey(uri) == false)) {
                                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                                    logger.logp(Level.FINE, CLASS_NAME, "loadTldsFromJar", "jar tld loaded for {0}", uri);
                                }
                                put(uri, tli);
                                //serializeTld(tldInputSource, tli);
                                eventListenerList.addAll(tldParser.getEventListenerList());
                                tagListForInjection.addAll(tldParser.getParsedTagsList());
                                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                                    logger.logp(Level.FINE, CLASS_NAME, "loadTldsFromJar", "tldParser.getEventListenerList() ["+ tldParser.getEventListenerList()+"]");
                                }
                            }
                        //}
                    }
                    catch (JspCoreException e) {
                        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                            logger.logp(Level.WARNING, CLASS_NAME, "loadTldsFromJar", "jsp error failed to load tld in jar. uri = ["+resourcePath+"]", e);
                        }
                    }
                    //}

                    stream.close();
                    stream = null;
                }
            }
        }
        catch (Exception e) {
            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                logger.logp(Level.WARNING, CLASS_NAME, "loadTldsFromJar", "jsp error failed to load tld in jar. uri = ["+resourcePath+"]", e);
            }
        }
        finally {
            if (stream != null) {
                try {
                    stream.close();
                }
                catch (Throwable t) {}
            }
            if (jarFile != null) {
                try {
                    jarFile.close();
                }
                catch (Throwable t) {}
            }
        }
    }

    public void loadTldsFromJar(Container jarContainer, String resourcePath, List loadedLocations, JspXmlExtConfig webAppConfig) {
        loadTldsFromJar(jarContainer, null, resourcePath, loadedLocations, webAppConfig);
    }

    private void loadTldsFromJar(Container jarContainer, String jarPath, String resourcePath, List loadedLocations, JspXmlExtConfig webAppConfig) {
    	if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
        	logger.logp(Level.FINE, CLASS_NAME, "loadTldsFromJar", "jar path ["+jarContainer.getPath()+"]"+"resourcePath ["+resourcePath+"] loadedLocations ["+loadedLocations+"] webAppConfig ["+webAppConfig+"]" );
        }
        
        try {
            com.ibm.wsspi.adaptable.module.Entry entry = jarContainer.adapt(com.ibm.wsspi.adaptable.module.Entry.class);
            String originatorId = entry != null ? entry.getName() : jarPath;
            int jarIndex = originatorId.indexOf(".jar");
            if (jarIndex>-1) {
                originatorId = originatorId.substring(0, jarIndex);
            }
            if (originatorId.indexOf('/') != -1)
                originatorId = originatorId.substring(originatorId.lastIndexOf('/')+1);
            originatorId = NameMangler.mangleString(originatorId);

            com.ibm.wsspi.adaptable.module.Entry metaInfEntry = jarContainer.getEntry("/META-INF/");
            Container metaInfContainer;
            if(metaInfEntry != null){
                metaInfContainer = metaInfEntry.adapt(Container.class);
                if(metaInfContainer != null){
                    loadTldsFromContainerRecursive(metaInfContainer, resourcePath, loadedLocations, webAppConfig, originatorId);
                }
            }
            
        }
        catch (Exception e) {
            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                logger.logp(Level.WARNING, CLASS_NAME, "loadTldsFromJar", "jsp error failed to load tld in jar. uri = ["+resourcePath+"]", e);
            }
        }
    }
    
    private void loadTldsFromContainerRecursive(Container dirContainer, String resourcePath, List loadedLocations, JspXmlExtConfig webAppConfig, String originatorId){
        InputStream stream = null;
        String name = null;
        
        try {
            for (com.ibm.wsspi.adaptable.module.Entry entry : dirContainer) {
                // Recurse into sub-directories
                Container subEntryContainer = entry.adapt(Container.class);
                //TODO: this could potentially be a jar represented by a directory on disk
                if (subEntryContainer!=null && entry.getSize()==0) { //a directory
                    loadTldsFromContainerRecursive(subEntryContainer, resourcePath, loadedLocations, webAppConfig, originatorId);
                }
                else {    
                    name = entry.getName();
                    if (name.endsWith(".tld") &&
                        loadedLocations.contains(resourcePath + "/" + name) == false) {
                        stream = entry.adapt(InputStream.class);
                        JspInputSource tldInputSource = getInputSource(dirContainer, name, null, null);
                        if (!locationsCache.containsKey(tldInputSource))
                            locationsCache.put(tldInputSource, new CachedLocationsWrapper(entry, originatorId));
                        try {
                            TagLibraryInfoImpl tli = tldParser.parseTLD(tldInputSource, stream, originatorId);
    
                            //516822 - If no URI is defined in the tag, we still want to load it in case it has listeners
                            //use the resourcePath + "/" + name as the key
                            String uri = null;
                            if (tli.getReliableURN() != null && tli.getReliableURN().trim().equals("") == false) {
                                uri = tli.getReliableURN();
                            } else {
                                uri = resourcePath + "/" + name;
                            }
    
                            //if (tli.getReliableURN() != null && tli.getReliableURN().trim().equals("") == false) {
                            tli.setURI(uri);
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                                logger.logp(Level.FINE, CLASS_NAME, "loadTldsFromContainerRecursive", "webAppConfig is " + webAppConfig);
                            }
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE) && webAppConfig != null) {
                                logger.logp(Level.FINE, CLASS_NAME, "loadTldsFromContainerRecursive", "tli URN is " + uri  + " :webAppConfig.getTagLibMap() is "
                                            + webAppConfig.getTagLibMap() + " :webAppConfig.getTagLibMap().containsKey(uri) is " + webAppConfig.getTagLibMap().containsKey(uri) 
                                            +  " :containsKey(uri) is " + containsKey(uri));
                            }
                            if ((webAppConfig != null && webAppConfig.getTagLibMap().containsKey(uri) == false)
                                    || (webAppConfig == null && containsKey(uri) == false)) {
                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                                    logger.logp(Level.FINE, CLASS_NAME, "loadTldsFromContainerRecursive", "jar tld loaded for {0}", uri);
                                }
                                put(uri, tli);
                                eventListenerList.addAll(tldParser.getEventListenerList());
                                tagListForInjection.addAll(tldParser.getParsedTagsList());
                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                                    logger.logp(Level.FINE, CLASS_NAME, "loadTldsFromContainerRecursive", "tldParser.getEventListenerList() [" + tldParser.getEventListenerList() + "]");
                                }
                            }
                        } catch (JspCoreException e) {
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.WARNING)) {
                                logger.logp(Level.WARNING, CLASS_NAME, "loadTldsFromContainerRecursive", "jsp error failed to load tld in jar. uri = [" + resourcePath + "]", e);
                            }
                        }
    
                        stream.close();
                        stream = null;
                    }
                }
            }
        } catch (Exception e) {
            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                logger.logp(Level.WARNING, CLASS_NAME, "loadTldsFromContainerRecursive", "jsp error failed to load tld in jar. uri = ["+resourcePath+"]", e);
            }
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Throwable t) {
                }
            }
        }
    }

    private void loadWebInfMap(String webInfPath, List loadedLocations) {
    	// No need to search META-INF resources
        if (ctxt!=null) {
            Set libSet = ctxt.getResourcePaths(webInfPath,false);
            if (libSet!=null) {
                Iterator it = libSet.iterator();
                while (it.hasNext()) {
                    String resourcePath = (String) it.next();
                    loadWebInfMapHelper(resourcePath, loadedLocations);
                }
            }
        } else {
            boolean directory = false;
            com.ibm.wsspi.adaptable.module.Entry entry = container.getEntry(webInfPath);
            if (entry!=null) {
                Container subEntryContainer;
                try {
                    subEntryContainer = entry.adapt(Container.class);
                } catch (UnableToAdaptException e1) {
                    // TODO Auto-generated catch block
                    // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                    // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
                    return;
                }
                
                //Process if it's a directory. If webInfPath is a jar, it was already processed by loadTldsFromJar
                if (subEntryContainer!=null && entry.getSize()==0 && !webInfPath.endsWith(".jar")) { //PI83486
                    directory = true;
    
                    for (com.ibm.wsspi.adaptable.module.Entry subEntry:subEntryContainer) {
                        loadWebInfMap(subEntry.getPath(), loadedLocations);
                    }
                }
                if (!directory) {
                    String resourcePath = entry.getPath();
                    loadWebInfMapHelper(resourcePath, loadedLocations);
                }
            }
        }
    }
    
    private void loadWebInfMapHelper(String resourcePath, List loadedLocations) {
        if (resourcePath.endsWith(".tld") && loadedLocations.contains(resourcePath) == false) {
            String tldOriginatorId = "webinf";
            try {
                JspInputSource inputSource = getInputSource(container, resourcePath, null, null);
                if (!locationsCache.containsKey(inputSource))
                    locationsCache.put(inputSource, new CachedLocationsWrapper(tldOriginatorId));
                //TagLibraryInfoImpl tli = loadSerializedTld(inputSource, inputSource);
                //if (tli == null) {
                TagLibraryInfoImpl tli = tldParser.parseTLD(inputSource, tldOriginatorId);
                //516822 - If no URI is defined in the tag, we still want to load it in case it has listeners
                //use the resourcePath as the key
                String uri = null;
                if (tli.getReliableURN() != null && tli.getReliableURN().trim().equals("") == false) {
                    uri = tli.getReliableURN();
                } else {
                    uri = resourcePath;
                }
                //if (tli.getReliableURN() != null && tli.getReliableURN().trim().equals("") == false) {
                    tli.setURI(uri);
                    if (containsKey(uri) == false) {
                        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                            logger.logp(Level.FINE, CLASS_NAME, "loadWebInfMap", "webinf tld loaded for {0}", uri);
                        }
                        put(uri, tli);
                        //serializeTld(inputSource, tli);
                        eventListenerList.addAll(tldParser.getEventListenerList());
                        tagListForInjection.addAll(tldParser.getParsedTagsList());
                    }
                //}
                //}
            }
            catch (JspCoreException e) {
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                    logger.logp(Level.WARNING, CLASS_NAME, "loadWebInfMap", "webinf tld failed to load for resourcePath =[" + resourcePath+ "]", e);
                }
            }
        }
        else if (resourcePath.endsWith("/")) {
            loadWebInfMap(resourcePath.substring(0, resourcePath.lastIndexOf('/')), loadedLocations);
        }
    }
    
    private void getFilesFromContainer(Set<String> libSet, Container container) {
        if (container!=null) {
            for (com.ibm.wsspi.adaptable.module.Entry subEntry : container) {
                        libSet.add(subEntry.getPath());
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    //Loads the Tag files in tagsDir. This function will call itself recursively for subdirectories.
    //If there is a problem parsing implicit.tld, the tag and tagx files in this directory will be not be loaded.
    private void loadWebInfTagFiles(String tagsDir) throws JspCoreException {
        // No need to search META-INF resources
        Set<String> libSet = null;
        Container c =ctxt.getServletContext().getModuleContainer();
        
        //Add the files root to this directory to libset
        if (c!=null) {
            libSet = new HashSet<String>();
            com.ibm.wsspi.adaptable.module.Entry e = c.getEntry(tagsDir);
            if (e!=null) {
                try {
                    getFilesFromContainer(libSet, e.adapt(Container.class)); //Get the fires in this folder (NOT recursive)
                } catch (UnableToAdaptException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        } else {
            libSet = (Set<String>) ctxt.getResourcePaths(tagsDir,false);
        }
        ArrayList<TagFileInfo> list = new ArrayList<TagFileInfo>();
        TagLibraryInfoImpl tli = null;
        //516671 - if there is an exception reading in the implicit.tld, we need to throw an exception at translation time.
        //therefore, we will not put any of the tags in the map and the app will get an error saying it can't find the tags.
        boolean succeeded=true;
        if (libSet != null) {
            ImplicitTldParser ImplicitTldParser = new ImplicitTldParser(ctxt, configManager, false);
            for (String resourcePath : libSet) {
                if (c==null && (resourcePath.endsWith("/"))) { // null container && directory
                    loadWebInfTagFiles(resourcePath.substring(0, resourcePath.lastIndexOf('/')));
                } else { //container exists, could still be a directory 
                    com.ibm.wsspi.adaptable.module.Entry thisEntry = container.getEntry(resourcePath); 
                    Container thisEntryContainer;
                    try {
                        thisEntryContainer = thisEntry.adapt(Container.class);
                    } catch (UnableToAdaptException ex) {
                        throw new IllegalStateException(ex);
                    }
                    if(thisEntryContainer!=null && thisEntry.getSize()==0){ //This is a directory
                        loadWebInfTagFiles(resourcePath); //Recursion: Call ourself with the sub directory.
                    }
                    else{ //Not a directory
                        if (resourcePath.endsWith("/implicit.tld")) { // must be named implicit.tld
                            try {
                                JspInputSource inputSource = ctxt.getJspInputSourceFactory().createJspInputSource(resourcePath);
                                tli = ImplicitTldParser.parseTLD(inputSource, "webinf");
                                if (tli != null) {
                                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                                        logger.logp(Level.FINE, CLASS_NAME, "loadWebInfTagFiles", "Got TagLibraryInfoImpl for [{0}], TLD file [{1}]", new Object[]{tagsDir,resourcePath});
                                    }
                                }
                            }
                            catch (JspCoreException e) {
                                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                                    logger.logp(Level.WARNING, CLASS_NAME, "loadWebInfTagFiles", "webinf tagfile directory tld failed to load for resourcePath =[" + resourcePath+ "]", e);
                                    logger.logp(Level.WARNING, CLASS_NAME, "loadWebInfTagFiles", "tagfiles in " + tagsDir + " are not loaded");
                                }
                                //Exception occurred, don't add any of the tags in this particular directory (tagsDir). Sub directories will still be loaded.
                                succeeded=false;
                            }
                        }
                    }
                }
            }
            
            if (succeeded) { //if there is an exception parsing the implicit.tld - don't load any of the tags so we throw a translation exception during runtime
                JspInputSource inputSource = ctxt.getJspInputSourceFactory().createJspInputSource(tagsDir);
                TagLibraryInfoImpl implicitTli = new ImplicitTagLibraryInfoImpl(tagsDir, inputSource);

                if (tli!=null) { // configure the implicit taglib with information from implicit.tld, if any
                    // 512316 - need to set tlib and jsp version in implicitTlibefore adding tag info to the map 
                    if (tli.getRequiredVersion()!=null)
                        implicitTli.setRequiredVersion(tli.getRequiredVersion());
                    if (tli.getTlibversion()!=null)
                        implicitTli.setTlibversion(tli.getTlibversion());
                }
                for (Iterator it = libSet.iterator(); it.hasNext();) {
                    String resourcePath = (String) it.next();
                    if (resourcePath.endsWith(".tag") || resourcePath.endsWith(".tagx")) {
                        try {
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)) {
                                logger.logp(Level.FINEST, CLASS_NAME, "loadWebInfTagFiles", "about to do tagfilescan for = ["+resourcePath+"]");
                            }
                            JspInputSource tagFileInputSource = ctxt.getJspInputSourceFactory().copyJspInputSource(implicitTli.getInputSource(), resourcePath);
                            JspTranslator jspTranslator = JspTranslatorFactory.getFactory().createTranslator(TAGFILE_SCAN_ID,
                                                                                                             tagFileInputSource,
                                                                                                             ctxt,
                                                                                                             configManager.createJspConfiguration(),
                                                                                                             jspOptions,  // 396002
                                                                                                             implicitTagLibPrefixMap);

                            JspVisitorInputMap  inputMap = new JspVisitorInputMap();
                            inputMap.put("TagLibraryInfo", implicitTli);
                            String name = resourcePath.substring(resourcePath.lastIndexOf('/')+1);
                            name = name.substring(0, name.indexOf(".tag"));
                            inputMap.put("TagFileName", name);
                            inputMap.put("TagFilePath", resourcePath);

                            HashMap results = jspTranslator.processVisitors(inputMap);
                            TagFileScanResult result = (TagFileScanResult)results.get("TagFileScan");
                            TagFileInfo tfi = new TagFileInfo(name, resourcePath, result.getTagInfo());
                            list.add(tfi);
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)) {
                                logger.logp(Level.FINEST, CLASS_NAME, "loadWebInfTagFiles", "TagLibraryCache TagFileInfo tfi= ["+tfi+"]");
                            }
                        }
                        catch (JspCoreException e) {
                            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                                logger.logp(Level.WARNING, CLASS_NAME, "loadWebInfTagFiles", "webinf tagfile failed to scan =[" + resourcePath+ "]", e);
                            }
                        }
                    }
                }
                if (list.size() > 0) {
                    implicitTli.setTagFiles(list);
                    list.clear();
                    implicitTli.setFunctions(Collections.emptyList());
                    implicitTli.setTags(Collections.emptyList());

                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                        logger.logp(Level.FINE, CLASS_NAME, "loadWebInfTagFiles", "Adding ImplicitTagLibraryInfoImpl for [{0}]", tagsDir);
                        logger.logp(Level.FINE, CLASS_NAME, "loadWebInfTagFiles", "  ImplicitTagLibraryInfoImpl=[{0}]", implicitTli);
                    }
                    put(tagsDir, implicitTli);
                }
                else if (tli!=null) {
                    put(tagsDir, tli); // just put the info from implicit.tld even though there are no tag files
                }
            }
        }
    }


    private TagLibraryInfoImpl loadTaglibTldFromJar(String uri) {
        TagLibraryInfoImpl tli = null;

        if (looseLibMap != null && looseLibMap.containsKey(uri)) {
            String tldLocation = (String)looseLibMap.get(uri);
            try {
                URL looseLibURL = new File(tldLocation).toURL();
                //new JspInputSourceContainerImpl(container, , relativeURL, urlStreamHandler)
                JspInputSource inputSource = ctxt.getJspInputSourceFactory().createJspInputSource(looseLibURL, "META-INF/taglib.tld");
                tli = tldParser.parseTLD(inputSource, "webinf");
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                    logger.logp(Level.FINE, CLASS_NAME, "loadTaglibTldFromJar", "tld loaded for [{0}]", uri);
                }
            }
            catch (JspCoreException e) {
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                    logger.logp(Level.WARNING, CLASS_NAME, "loadTaglibTldFromJar", "jsp error failed to parse loose library tld . location = ["+tldLocation+"]", e);
                }
            }
            catch (MalformedURLException e) {
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                    logger.logp(Level.WARNING, CLASS_NAME, "loadTaglibTldFromJar", "jsp error failed to parse loose library tld . location = ["+tldLocation+"]", e);
                }
            }
        }
        else {
            InputStream stream = null;
            try {
                com.ibm.wsspi.adaptable.module.Entry jarEntry = container.getEntry(uri);
                Container jarContainer = jarEntry.adapt(Container.class);
                com.ibm.wsspi.adaptable.module.Entry metaInfTldEntry = jarContainer.getEntry("META-INF/taglib.tld");
                if (metaInfTldEntry != null) {
                    stream = metaInfTldEntry.adapt(InputStream.class);
                    String originatorId = jarEntry.getName();
                    originatorId = originatorId.substring(0, originatorId.indexOf(".jar"));
                    if (originatorId.indexOf('/') != -1)
                        originatorId = originatorId.substring(originatorId.lastIndexOf('/')+1);
                    originatorId = NameMangler.mangleString(originatorId);

                    JspInputSource tldInputSource = getInputSource(jarContainer, "META-INF/taglib.tld", null, null);

                    //tli = loadSerializedTld(tldInputSource, inputSource);
                    //if (tli == null) {
                    tli = tldParser.parseTLD(tldInputSource, stream, originatorId);
                    if (tli != null) {
                        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                            logger.logp(Level.FINE, CLASS_NAME, "loadTaglibTldFromJar", "tld loaded for [{0}]", uri);
                        }
                        //serializeTld(tldInputSource, tli);
                    }
                    //}
                }
            }
            catch (JspCoreException e) {
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                    logger.logp(Level.WARNING, CLASS_NAME, "loadTaglibTldFromJar", "jsp error failed to parse tld in jar. uri = ["+uri+"]", e);
                }
            }
            catch (UnableToAdaptException e) {
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                    logger.logp(Level.WARNING, CLASS_NAME, "loadTaglibTldFromJar", "jsp error failed to parse tld in jar. uri = ["+uri+"]", e);
                }
            }
            finally {
                if (stream != null) {
                    try {
                        stream.close();
                    }
                    catch (Throwable t) {}
                }
            }
        }
        return tli;
    }
    
    //PK68590 start
    private void loadLooseLibTagFiles(String looseLibDir, List loadedLocations, String looseKey) {    //PM07608  //PM03123
        if(logger.isLoggable(Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME, "loadLooseLibTagFiles", "looseLibDir {0}", looseLibDir);
        }
        
        File looseLibDirFile = new File(looseLibDir);
        String [] list = looseLibDirFile.list();
        
        if (list == null){               						//PM03123  null if not denote a directory (i.e this is a file)
    		list = new String[1];								
    		list[0] = looseLibDir;								
    	}														//PM03123
        
        if (list != null) {
            TagLibraryInfoImpl tli = null;
            for (int j = 0; j < list.length; j++) {
                String resourcePath = (String) list[j];
                if (resourcePath.endsWith(".tld")) {
                    if(logger.isLoggable(Level.FINE)){
                            logger.logp(Level.FINE, CLASS_NAME, "loadLooseLibTagFiles", "tld located {0}", resourcePath);
                        }
                        try {
                            URL looseLibURL = new File(looseLibDir).toURL();
                            JspInputSource inputSource = ctxt.getJspInputSourceFactory().createJspInputSource(looseLibURL, resourcePath);
                            tli = tldParser.parseTLD(inputSource, "webinf");
                            if(logger.isLoggable(Level.FINE)){
                                logger.logp(Level.FINE, CLASS_NAME, "loadLooseLibTagFiles", "tli {0}", tli);
                            }
                        }
                        catch (JspCoreException e) {
                            if(logger.isLoggable(Level.WARNING)){
                                logger.logp(Level.WARNING, CLASS_NAME, "loadLooseLibTagFiles", "jsp error failed to parse loose library tld . location = ["+looseLibDir+"]", e);
                            }
                        }
                        catch (MalformedURLException e) {
                            if(logger.isLoggable(Level.WARNING)){
                                logger.logp(Level.WARNING, CLASS_NAME, "loadLooseLibTagFiles", "jsp error failed to parse loose library tld . location = ["+looseLibDir+"]", e);
                            }
                        }

                        if (tli != null) {
                            String looseLibURN = tli.getReliableURN();
                            //PM07608
                        	if (looseLibURN == null){
                        		looseKey = looseKey.replace('\\', '/');        // use URI defined in RAD looseLibConfig xml file
                        		looseLibURN = looseKey + "/" + resourcePath;
                        		if(logger.isLoggable(Level.FINE)){
                        			logger.logp(Level.FINE, CLASS_NAME, "loadLooseLibTagFiles", "jsp failed to find a uri sub-element in ["+resourcePath+"], default uri to \""+ looseLibURN+"\"");
                        		}
                        	}
                        	//PM07608
                            if(logger.isLoggable(Level.FINE)){
                                logger.logp(Level.FINE, CLASS_NAME, "loadLooseLibTagFiles", "Adding TagLibraryInfoImpl for [{0}]", looseLibURN);
                            }
                            
                            put(looseLibURN, tli);
                        }                  
                }
                //PM03123 - start
                else if (resourcePath.endsWith(".jar")){
                	if(logger.isLoggable(Level.FINE)){
                        logger.logp(Level.FINE, CLASS_NAME, "loadLooseLibTagFiles", "looseLibDir is a jar");
                    }
                    loadTldsFromJar(resourcePath, loadedLocations);
                }
                //PM03123 - end 
                else {
                    //not a tld...maybe it's a directory
                    StringBuffer nestedDir = new StringBuffer(looseLibDir);
                    if (looseLibDir.endsWith("/") || looseLibDir.endsWith("\\")) {
                        nestedDir.append(resourcePath);
                    } else {
                        nestedDir.append("/");
                        nestedDir.append(resourcePath);                     
                    }
                    if (new File(nestedDir.toString()).isDirectory()) {
                    	loadLooseLibTagFiles(nestedDir.toString(), loadedLocations, looseKey);    //PM07608		//PM03123
                    }
                }
            }
        }
    }
    //PK68590 end    

    public synchronized TagLibraryInfoImpl getTagLibraryInfo(String uri, String prefix, String jspUri) {
        int type = uriType(uri);
        TagLibraryInfoImpl tli = null;
        if (containsKey(uri)) {
            Object o = get(uri);
            if (o instanceof TagLibraryInfoImpl) {
                TagLibraryInfoImpl impl = (TagLibraryInfoImpl)o;
                tli = impl.copy(prefix);
            }
            else if (o instanceof TagLibraryInfoProxy) {
                TagLibraryInfoProxy proxy = (TagLibraryInfoProxy)o;
                tli = proxy.getTagLibraryInfoImpl(prefix);
                if(tli != null){    // defect 220310: ensure tli is not null.
                    put(uri, tli);
                }
            }
        }
        else {
            if (type == ROOT_REL_URI || type == NOROOT_REL_URI) {
                if (uri.endsWith(".jar")) {
                    TagLibraryInfoImpl impl = loadTaglibTldFromJar(uri);
                    if (impl != null) {
                        impl.setURI(uri);
                        put(uri, impl);
                        tli = impl.copy(prefix);
                    }
                }
                else {
                    try {
                        TagLibraryInfoImpl impl = null;
                        String path = uri;
                        if (type == NOROOT_REL_URI) {
                            path = jspUri.substring(0, jspUri.lastIndexOf("/") + 1);
                            path = path + uri;
                        }
                        JspInputSource tldInputSource = getInputSource(container, path, null, null);
                        impl = tldParser.parseTLD(tldInputSource, "webinf");
                        if (impl != null) {
                            impl.setURI(uri);
                            put(uri, impl);
                            tli = impl.copy(prefix);
                        }
                    }
                    catch (JspCoreException e) {
                       if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                            logger.logp(Level.WARNING, CLASS_NAME, "getTagLibraryInfo", "jsp error failed to parse tld in WEB-INF. uri = ["+uri+"]", e);
                       }
                    }
                }
            }
        }
        return (tli);
    }

    public synchronized TagFileResources getTagFileResources(TagFileResources tagFileResources) {
        String key = "";
        if (container!=null) { //ctxt.getServletContext().getModuleContainer()!=null) {
            key = tagFileResources.getInputSource().getRelativeURL();
        } else {
            key = tagFileResources.getInputSource().getAbsoluteURL().toExternalForm();
        }
        if (tagFileResourcesMap.containsKey(key) == false) {
            tagFileResourcesMap.put(key, tagFileResources);
            return (tagFileResources);
        }
        else {
            return (TagFileResources)tagFileResourcesMap.get(key);
        }
    }

    public synchronized TagClassInfo getTagClassInfo(TagInfo tag) {
        return (TagClassInfo)tagClassMap.get(tag.getTagClassName());
    }

    public synchronized void addTagClassInfo(TagInfo ti, Class tagClass) {
        tagClassMap.put(ti.getTagClassName(), new TagClassInfo(tagClass));
    }

    public synchronized void addTagFileClassInfo(TagFileInfo tfi) {
        tagClassMap.put(tfi.getTagInfo().getTagClassName(), new TagFileClassInfo(tfi.getTagInfo()));
    }

    public synchronized void reloadTld(String tldFilePath, long timestamp) throws JspCoreException {
        String tliKey = null;
        for (Iterator itr = this.keySet().iterator(); itr.hasNext();) {
            String key = (String)itr.next();
            Object o = get(key);
            if (o instanceof TagLibraryInfoImpl) {
                TagLibraryInfoImpl tli = (TagLibraryInfoImpl)o;
                if (tli.getTldFilePath() != null && tli.getTldFilePath().equals(tldFilePath)) {
                    tliKey = key;
                    break;
                }
            }
        }

        if (tliKey != null) {
            TagLibraryInfoImpl oldTli = (TagLibraryInfoImpl)get(tliKey);
            if (oldTli.getLoadedTimestamp() < timestamp) {
                TagLibraryInfoImpl tli = tldParser.parseTLD(oldTli.getInputSource(), "webinf");
                tli.setURI(oldTli.getURI());
                put(tliKey, tli);
                oldTli = null;
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                    logger.logp(Level.FINE, CLASS_NAME, "reloadTld", "tld [{0}] reloaded", tldFilePath);
                }
            }
        }
    }

    public synchronized TagLibraryInfoImpl reloadImplicitTld(String tagDir) throws JspCoreException {
        TagLibraryInfoImpl tli  = null;
        if (containsKey(tagDir)) {
            remove(tagDir);
            loadWebInfTagFiles(tagDir);
            tli = (TagLibraryInfoImpl)get(tagDir);
        }
        return tli;
    }

    public Map getImplicitTagLibPrefixMap() {
        return implicitTagLibPrefixMap;
    }

    public List<String> getEventListenerList() {
        return eventListenerList;
    }
    
    public List<String> getTagsList() {
        return tagListForInjection;
    }

    public synchronized OptimizedTag getOptimizedTag(String tlibUri, String tlibVersion, String shortName) {
        OptimizedTag optTag = null;

        String key = tlibUri + tlibVersion + shortName;
        if (optimizedTagConfigMap.containsKey(key)) {
            OptimizedTagConfig optTagConfig = (OptimizedTagConfig)optimizedTagConfigMap.get(key);
            try {
                optTag = (OptimizedTag)optTagConfig.getOptClass().newInstance();
            }
            catch (InstantiationException e) {
                logger.logp(Level.WARNING, CLASS_NAME, "getOptimizedTag", "failed to instantiate optimized tag [" + optTagConfig.getOptClass() +"]", e);
            }
            catch (IllegalAccessException e) {
                logger.logp(Level.WARNING, CLASS_NAME, "getOptimizedTag", "Illegal access of optimized tag [" + optTagConfig.getOptClass() +"]", e);
            }
        }

        return (optTag);
    }

    public synchronized Object getTagFileLock(List tagFileIdList) {
        Object lock = null;

        if (tagFileLockMap == null) {
            tagFileLockMap = new HashMap();
        }

        for (Iterator itr = tagFileIdList.iterator(); itr.hasNext();) {
            TagFileId tagFileId = (TagFileId) itr.next();
            Object o = tagFileLockMap.get(tagFileId.toString());
            if (o != null) {
                lock = o;
                break;
            }
        }

        if (lock == null) {
            lock = new Object();

            for (Iterator itr = tagFileIdList.iterator(); itr.hasNext();) {
                TagFileId tagFileId = (TagFileId) itr.next();
                tagFileLockMap.put(tagFileId.toString(), lock);
            }
        }

        return lock;
    }

    public synchronized void releaseTagFileLock(List tagFileIdList) {
        for (Iterator itr = tagFileIdList.iterator(); itr.hasNext();) {
            TagFileId tagFileId = (TagFileId) itr.next();
            tagFileLockMap.remove(tagFileId.toString());
        }
    }

    /*private TagLibraryInfoImpl loadSerializedTld(JspInputSource source, JspInputSource comparisonSource) {
        TagLibraryInfoImpl tli = null;

        String sourceFile = source.getAbsoluteURL().toExternalForm().replace('\\', '_');
        sourceFile = sourceFile.replace('/', '_');
        File serialixedTliFile = new File(outputDir + File.separator +
                                          NameMangler.mangleString(sourceFile) +
                                          ".ser");
        if (serialixedTliFile.exists()) {
            if (serialixedTliFile.lastModified() >= comparisonSource.getLastModified()) {
                ObjectInputStream ois = null;
                FileInputStream fis = null;

                try {
                    fis = new FileInputStream(serialixedTliFile);
                    ois = new ObjectInputStream(fis);
                    tli = (TagLibraryInfoImpl)ois.readObject();
                    if (tli != null)
                        System.out.println("tld loaded from " + serialixedTliFile.getPath());
                }
                catch (Exception e) {}
                finally {
                    try {
                        if (fis != null)
                            fis.close();
                        if (ois != null)
                            ois.close();
                    }
                    catch (IOException e){}
                }
            }
        }

        return tli;
    }

    private void serializeTld(JspInputSource source, TagLibraryInfoImpl tli) {

        String sourceFile = source.getAbsoluteURL().toExternalForm().replace('\\', '_');
        sourceFile = sourceFile.replace('/', '_');
        File serialixedTliFile = new File(outputDir + File.separator +
                                          NameMangler.mangleString(sourceFile) +
                                          ".ser");
        System.out.println("serialixedTliFile = " + serialixedTliFile.getPath());
        ObjectOutputStream oos = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(serialixedTliFile);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(tli);
            System.out.println("tld serialized to " + serialixedTliFile.getPath());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (fos != null)
                    fos.close();
                if (oos != null)
                    oos.close();
            }
            catch (IOException e){}
        }

    }*/

    private static int uriType(String uri) {
        if (uri.indexOf(':') != -1) {
            return ABS_URI;
        }
        else if (uri.startsWith("/")) {
            return ROOT_REL_URI;
        }
        else {
            return NOROOT_REL_URI;
        }
    }

    private void loadSharedLibMap(List loadedLocations) {
        if (container!=null) {
            try {
                NonPersistentCache overlayCache = container.adapt(NonPersistentCache.class);
                WebModuleInfo moduleInfo = (WebModuleInfo) overlayCache.getFromCache(WebModuleInfo.class);
                if (moduleInfo != null) {
                    ApplicationInfo appInfo = moduleInfo.getApplicationInfo();
                    Container appContainer = appInfo.getContainer();
                    SharedLibClassesContainerInfo sharedLibInfo = appContainer.adapt(SharedLibClassesContainerInfo.class);
                    if (sharedLibInfo != null) {
                        for (ContainerInfo containerInfo : sharedLibInfo.getCommonLibraryClassesContainerInfo()) {
                            String resourcePath = containerInfo.getName();
                            if (resourcePath.lastIndexOf('/') != -1) {
                                resourcePath = resourcePath.substring(0, resourcePath.lastIndexOf('/'));
                            }
                            loadTldsFromJar(containerInfo.getContainer(), containerInfo.getName(), resourcePath, loadedLocations, null);
                        }
                        for (ContainerInfo containerInfo : sharedLibInfo.getSharedLibraryClassesContainerInfo()) {
                            String resourcePath = containerInfo.getName();
                            if (resourcePath.lastIndexOf('/') != -1) {
                                resourcePath = resourcePath.substring(0, resourcePath.lastIndexOf('/'));
                            }
                            loadTldsFromJar(containerInfo.getContainer(), containerInfo.getName(), resourcePath, loadedLocations, null);
                        }
                    }
                }
            } catch (UnableToAdaptException e) {
                e.getCause();
            }
        }
    }
    
    private void useCachedLocations() {
        
        Iterator<Map.Entry<JspInputSource, CachedLocationsWrapper>> iterator = locationsCache.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<JspInputSource, CachedLocationsWrapper> pair = (Map.Entry<JspInputSource, CachedLocationsWrapper>) iterator.next();
            InputStream newInputStream = null;
            String uri = null;
            try {
                TagLibraryInfoImpl tli = null;
                
                if (pair.getValue().getEntry() != null) {
                    newInputStream = ((com.ibm.wsspi.adaptable.module.Entry) pair.getValue().getEntry()).adapt(InputStream.class);
                    tli = tldParser.parseTLD(pair.getKey(), newInputStream, (String) pair.getValue().getOriginatorId());
                    
                } else
                    tli = tldParser.parseTLD(pair.getKey(), (String) pair.getValue().getOriginatorId());
                
                if (tli.getReliableURN() != null && tli.getReliableURN().trim().equals("") == false) {
                    uri = tli.getReliableURN();
                } else {
                    uri = pair.getKey().getRelativeURL();
                }
                
                tli.setURI(uri);
                
                if (containsKey(uri) == false) {
                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                        logger.logp(Level.FINE, CLASS_NAME, "useCachedLocations", "TLD file loaded for {0}", uri);
                    }
                    put(uri, tli);
                    eventListenerList.addAll(tldParser.getEventListenerList());
                    tagListForInjection.addAll(tldParser.getParsedTagsList());
                }
            } catch (JspCoreException e) {
                logger.logp(Level.WARNING, CLASS_NAME, "useCachedLocations", "jsp.parse.tld.exception", new Object[] {uri,e});
            } catch (UnableToAdaptException e) {
                logger.logp(Level.WARNING, CLASS_NAME, "useCachedLocations", "jsp.parse.tld.exception", new Object[] {uri,e});
            } finally {
                try {
                    if (newInputStream != null)
                        newInputStream.close();
                } catch (IOException e) {
                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                        logger.logp(Level.FINE, CLASS_NAME, "useCachedLocations", "A problem occurred while closing newInputStream.", e);
                    }
                }
            }
        }
    }

    private class CachedLocationsWrapper {
        private com.ibm.wsspi.adaptable.module.Entry entry = null;
        private String originatorId = null;
        
        CachedLocationsWrapper(com.ibm.wsspi.adaptable.module.Entry entry, String originatorId) {
            this.entry = entry;
            this.originatorId = originatorId;
        }
        
        CachedLocationsWrapper(String originatorId) {
            this.originatorId = originatorId;
        }
        
        public com.ibm.wsspi.adaptable.module.Entry getEntry() {
            return entry;
        }

        public String getOriginatorId() {
            return originatorId;
        }
    }

}
