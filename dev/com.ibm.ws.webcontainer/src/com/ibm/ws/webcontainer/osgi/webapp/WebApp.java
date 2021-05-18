/*******************************************************************************
 * Copyright (c) 2010, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

// PM96235       9/12/13     pmdinh          Catch NoClassDefFoundError during init of ServletContainerInitializers
// 148139(CD)/   10/17/14    bitonti         Security annotation processing not checking metadata-complete
// PI30335       12/16/14    wtlucy          Refactor injection code to allow for independent post construct invocation
// 150253(CD-STAB)
// 160846        03/13/15    lmoppenh        Add custom property to determine class initialization order.
// PI58875       06/06/16    pmdinh          Option to stop app from startup when there is exception in listener.  Also fix classLoader memo leak when app fails to start
//
package com.ibm.ws.webcontainer.osgi.webapp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebServlet;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.MetaDataService;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.webcontainer.osgi.collaborator.CollaboratorHelperImpl;
import com.ibm.ws.webcontainer.osgi.extension.DefaultExtensionProcessor;
import com.ibm.ws.webcontainer.osgi.extension.InvokerExtensionProcessor;
import com.ibm.ws.webcontainer.osgi.filter.WebAppFilterManagerImpl;
import com.ibm.ws.webcontainer.osgi.managed.WCManagedObjectImpl;
import com.ibm.ws.webcontainer.osgi.mbeans.GeneratePluginConfigMBean;
import com.ibm.ws.webcontainer.osgi.metadata.WebComponentMetaDataImpl;
import com.ibm.ws.webcontainer.servlet.DirectoryBrowsingServlet;
import com.ibm.ws.webcontainer.servlet.H2Handler;
import com.ibm.ws.webcontainer.servlet.ServletConfig;
import com.ibm.ws.webcontainer.servlet.WsocHandler;
import com.ibm.ws.webcontainer.util.DocumentRootUtils;
import com.ibm.ws.webcontainer.util.EntryResource;
import com.ibm.ws.webcontainer.util.MetaInfResourcesFileUtils;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfigurationProvider;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefBindingHelper;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;
import com.ibm.wsspi.injectionengine.ReferenceContext;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.annotation.AnnotationHelperManager;
import com.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper;
import com.ibm.wsspi.webcontainer.collaborator.IWebAppSecurityCollaborator;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.filter.IFilterConfig;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;
 
/**
 */
@SuppressWarnings("unchecked")
public class WebApp extends com.ibm.ws.webcontainer.webapp.WebApp implements ComponentNameSpaceConfigurationProvider
{
  private static final TraceComponent tc = Tr.register(WebApp.class,com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants.TR_GROUP, com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants.NLS_PROPS);
  protected static final String CLASS_NAME = "com.ibm.ws.webcontainer.osgi.webapp.WebApp";


  
  private final MetaDataService metaDataService;
  private volatile boolean namespacePopulated;
  private final ReferenceContext referenceContext;
  
  private J2EENameFactory  j2eeNameFactory;
  private ClassLoader moduleLoader;
  
  private WebAppConfiguration webAppConfig;
  private boolean extensionProcessingDisabled=false;
  private ManagedObjectService managedObjectService;  

  /**
   * Constructor.
   * 
   * @param name
   * @param parent
   * @param warDir
   */
  public WebApp(WebAppConfiguration webAppConfig,
                ClassLoader moduleLoader,
                ReferenceContext referenceContext,
                MetaDataService metaDataService,
                J2EENameFactory j2eeNameFactory,
                ManagedObjectService managedObjectService)
  {
    super(webAppConfig, null);
    this.webAppConfig = webAppConfig;
    this.moduleLoader = moduleLoader;
    this.referenceContext = referenceContext;
    this.metaDataService = metaDataService;
    this.j2eeNameFactory = j2eeNameFactory;
    this.managedObjectService = managedObjectService;
  }
  


  @Override
  public String toString()
  {
    WebModuleMetaData mmd = config == null ? null : config.getMetaData();
    return mmd == null ? super.toString() : super.toString() + '[' + mmd.getJ2EEName() + ']';
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.ws.webcontainer.webapp.WebApp#createDispatchContext()
   */
  public WebAppDispatcherContext createDispatchContext()
  {
    return new com.ibm.ws.webcontainer.osgi.webapp.WebAppDispatcherContext(this);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.ws.webcontainer.webapp.WebApp#getInvokerExtensionProcessor()
   */
  protected ExtensionProcessor getInvokerExtensionProcessor()
  {
    return new InvokerExtensionProcessor(this, this.config.getInvokerAttributes());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.ws.webcontainer.webapp.WebApp#getLoginProcessor()
   */
  @Override
  public ExtensionProcessor getLoginProcessor()
  {
      //do not cache the processor in the webcontainer code, always return the result from security
      IWebAppSecurityCollaborator secCollab = this.collabHelper.getSecurityCollaborator();
      loginProcessor = secCollab.getFormLoginExtensionProcessor(this);
      return loginProcessor;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.ws.webcontainer.webapp.WebApp#getLogoutProcessor()
   */
  @Override
  public ExtensionProcessor getLogoutProcessor()
  {
      //do not cache the processor in the webcontainer code, always return the result from security
      IWebAppSecurityCollaborator secCollab = this.collabHelper.getSecurityCollaborator();
      logoutProcessor = secCollab.getFormLogoutExtensionProcessor(this);

      return logoutProcessor;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.ws.webcontainer.webapp.WebApp#getNodeName()
   */
  public String getNodeName()
  {
    // TODO Auto-generated method stub
    return "default_node";
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.ws.webcontainer.webapp.WebApp#getServerInfo()
   */
  public String getServerInfo()
  {      
      return "SMF WebContainer";
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.ws.webcontainer.webapp.WebApp#getServerName()
   */
  public String getServerName()
  {
    // TODO Auto-generated method stub
    return "SMF WebContainer";
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.ws.webcontainer.webapp.WebApp#getWebExtensionProcessor()
   */
  public com.ibm.ws.webcontainer.extension.WebExtensionProcessor getWebExtensionProcessor()
  {
    // TODO Auto-generated method stub
    return new com.ibm.ws.webcontainer.osgi.extension.WebExtensionProcessor(this);
  }

  @Override
  protected void commonInitializationStart(com.ibm.ws.webcontainer.webapp.WebAppConfiguration config,
                                           com.ibm.ws.container.DeployedModule moduleConfig) throws Throwable
  {
    referenceContext.process();

    super.commonInitializationStart(config, moduleConfig);
    
    Dictionary<String,String> headers = webAppConfig.getBundleHeaders();
    if (headers != null) {
        String implicitMappingDisabledHeader = headers.get("IBM-Web-Extension-Processing-Disabled");
        extensionProcessingDisabled = Boolean.valueOf(implicitMappingDisabledHeader).booleanValue();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "commonInitializationStart", "ExtensionProcessingDisabled-->" + extensionProcessingDisabled + ", header value = "+ implicitMappingDisabledHeader);
        }
    }

  }

  @Override
  protected void processDynamicInjectionMetaData(Class<?> klass) throws InjectionException
  {
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
          logger.logp(Level.FINE, CLASS_NAME, "processDynamicInjectionMetaData", "MetadataComplete  = " + config.isMetadataComplete() + ", Class-->" + klass.getName());
      }
      if (!config.isMetadataComplete())
    {
      List<Class<?>> injectionClasses = Collections.<Class<?>>singletonList(klass);
      if (referenceContext.isProcessDynamicNeeded(injectionClasses)) {
          ComponentNameSpaceConfiguration compNSConfig = createComponentNameSpaceConfiguration(Collections.<Class<?>>singletonList(klass), false);
          referenceContext.processDynamic(compNSConfig);
      }    
    }
  }

  @Override
  public ComponentNameSpaceConfiguration getComponentNameSpaceConfiguration()
  {
    return createComponentNameSpaceConfiguration(getInjectionClasses(), true);
  }

  private ComponentNameSpaceConfiguration createComponentNameSpaceConfiguration(List<Class<?>> injectionClasses, boolean xml)
  {
    J2EEName j2eeName = getModuleMetaData().getJ2EEName();
    ComponentNameSpaceConfiguration compNSConfig = new ComponentNameSpaceConfiguration(config.getDisplayName(), j2eeName);
    compNSConfig.setClassLoader(moduleLoader);
    compNSConfig.setModuleMetaData(getModuleMetaData());
    compNSConfig.setMetaDataComplete(config.isMetadataComplete());
    compNSConfig.setInjectionClasses(injectionClasses);

    if (xml)
    {
      JNDIEnvironmentRefType.setAllRefs(compNSConfig, config.getAllRefs());
    }
    JNDIEnvironmentRefBindingHelper.setAllBndAndExt(compNSConfig,
                                                    config.getAllRefBindings(),
                                                    config.getEnvEntryValues(),
                                                    config.getResourceRefConfigList(null));

    return compNSConfig;
  }

  private List<Class<?>> getInjectionClasses() {
    if (config.isMetadataComplete()) {
      return Collections.emptyList();
    }
    
    Set<String> classNames = new TreeSet<String>();
    
    try {
      WebAppInjectionClassList injectionClassList = container.adapt(WebAppInjectionClassList.class);
      classNames.addAll(injectionClassList.getClassNames());
    } catch (UnableToAdaptException e) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        Tr.debug(tc, "getInjectionClasses: failed to adapt to WebAppInjectionClassList ", e);
      }
    }

    for (Iterator<IServletConfig> it = config.getServletInfos(); it.hasNext();) {
      IServletConfig servletInfo = it.next();
      addInjectionClassName(classNames, servletInfo.getClassName());
    }

    for (Iterator<IFilterConfig> it = config.getFilterInfos(); it.hasNext();) {
      IFilterConfig filterInfo = it.next();
      addInjectionClassName(classNames, filterInfo.getClassName());
    }

    classNames.addAll(config.getListeners());
    
    try {
      WebAnnotations webAnnotations = getWebAnnotations();
      AnnotationTargets_Targets webModuleTargets = webAnnotations.getAnnotationTargets();

      // d95160: Servlet and Filter classes are obtained only from SEED (non-metadata-complete) regions.
      classNames.addAll(webModuleTargets.getAnnotatedClasses(WebServlet.class.getName(), AnnotationTargets_Targets.POLICY_SEED));
      classNames.addAll(webModuleTargets.getAnnotatedClasses(WebFilter.class.getName(), AnnotationTargets_Targets.POLICY_SEED));

    } catch (Exception e) {
      // debug and swallow
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        Tr.debug(tc, "getInjectionClasses: got exception looking for annotations of servlets and filters", e);
      }
    }

    return getInjectionClasses(classNames);
  }

  private void addInjectionClassName(Set<String> classNames, String className)
  {
    if (className != null)
    {
      classNames.add(className);
    }
  }

  @FFDCIgnore(Throwable.class)
  private List<Class<?>> getInjectionClasses(Set<String> classNames)
  {
    List<Class<?>> classes = new ArrayList<Class<?>>();
    for (String className : classNames)
    {
      try
      {
        classes.add(moduleLoader.loadClass(className));
      }
      catch (Throwable t)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
          Tr.debug(tc, "getInjectionClasses: failed to load " + className, t);
        }
      }
    }

    return classes;
  }

  
  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.ws.webcontainer.webapp.WebApp#getResource(java.lang.String)
   */
  public URL getResource(String p) throws MalformedURLException
  {
    String rPath = null;

   /*
     * The spec states the resource must start with a / so if one isn't there we
     * prepend one.
     */
    // Begin 263020
    if (p.charAt(0) != '/' && p.charAt(0) != '\\')
    {
      if (prependSlashToResource)
      {
        logger.logp(Level.WARNING, CLASS_NAME, "getResource", "resource.path.has.to.start.with.slash");
        rPath = "/" + p;
      }
      else
      {
        throw new MalformedURLException(nls.getString("resource.path.has.to.start.with.slash"));
      }
    }
    else
    {
      rPath = p;
    }
    // End 263020
    
    if (container != null) {
        //Can not call getEntry with / on the root container
        if (p.equals("/")) {
            Collection<URL> urls = container.getURLs();
            Iterator<URL> it = urls.iterator();
            if (it.hasNext()) {
                URL url = it.next();  //get the first url
                
                if (url.getProtocol().equals("file")) { 
                    File file = new File(url.getFile());
                    if (file.exists() && !file.isDirectory()){  //might be a file:/xxx.war
                        return new URL("jar", "", -1, url + "!/");  //return jar:file:/xxx.war!/
                    }
                } 

                return url;
            }
        } else {
            Entry entry = container.getEntry(rPath);
            if (entry != null) {
                return entry.getResource();
            } else {
                return getDocumentRootUtils(rPath).getURL(rPath,metaInfCache);
            }
        }
    }

    String uri = getRealPath(rPath);
    if (uri == null)
    {
      return null;
    }

    java.io.File checkFile = new java.io.File(uri);
    if (!checkFile.exists())
    {
      if (!WCCustomProperties.SKIP_META_INF_RESOURCES_PROCESSING)
      {
        if (useMetaInfCache)
        {
          if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
          {
            logger.logp(Level.FINE, CLASS_NAME, "getResource", "trying META-INF/resources cache");
          }
          synchronized (metaInfCache)
          {
            if (metaInfCache.containsKey(rPath))
            {
              URL cachedURL = metaInfCache.get(rPath);
              if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
              {
                logger.logp(Level.FINE, CLASS_NAME, "getResource", "got cached META-INF name->[{0}], URL->[{1}]", new Object[] { rPath, cachedURL });
              }
              return cachedURL;
            }
          }
        }
        // URL otherURL = getWebInfLibClassloader().getResource(metaInfPath);
        MetaInfResourcesFileUtils metaInfFileUtil = new MetaInfResourcesFileUtils(this);
        metaInfFileUtil.findInMetaInfResource(rPath);
        URL metaInfURL = metaInfFileUtil.getURL();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
        {
          logger.logp(Level.FINE, CLASS_NAME, "getResource", "file did not exist, trying META-INF/resources");
          logger.logp(Level.FINE, CLASS_NAME, "getResource", "metaInfPath->/META-INF/resources" + rPath);
        }
        if (useMetaInfCache)
        {
          if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
          {
            logger.logp(Level.FINE, CLASS_NAME, "getResource", "adding to META-INF cache name->[{0}], URL->[{1}]", new Object[] { rPath, metaInfURL });
          }
          synchronized (metaInfCache)
          {
            metaInfCache.put(rPath, metaInfURL);
          }
        }
        return metaInfURL;

      }
      else
      {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
        {
          logger.logp(Level.FINE, CLASS_NAME, "getResource", "skipping META-INF/resources processing");
        }
        return null;
      }
    }
    return checkFile.toURL();

  }
  
  /**
   * {@inheritDoc}
   */
  @FFDCIgnore(IOException.class) //This can happen if a non-existent file is requested.
  public InputStream getResourceAsStream(String path) {
      
      try {
          if (container != null){
              Entry entry = container.getEntry(path);
              if(entry != null){
                  return entry.adapt(InputStream.class);
              } else {
                  DocumentRootUtils dru = getDocumentRootUtils(path);
                  
                  //The cache doesn't work for non-URLs (as it only caches URLs) so skip it for now in this case
                  dru.handleDocumentRoots(path, null);
                  return dru.getInputStream();
              }
          }
      
          URL url = getResource(path);
          if (url == null)
              return null;
          URLConnection conn = url.openConnection();
          return conn.getInputStream();
      } catch (MalformedURLException e) {
          return null;
      } catch (IOException e) {
          return null;
      } catch (UnableToAdaptException e) {
          return null;
      }
  }

  public String getRealPath(String path) {
      return getRealPath(path, true);
  }

  
  /*
   * (non-Javadoc)
   * 
   * @see javax.servlet.ServletContext#getRealPath(java.lang.String)
   */
  public String getRealPath(String path, boolean checkDocRoot)
  {
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
          logger.entering(CLASS_NAME, "getRealPath", new Object[]{path, checkDocRoot});
      
       
    String basePath = null;

    if (path == null || path.equals("")) {
        path = "/";
    }
      
    if(container != null){
        
        boolean pathIsBackslash = false;
        if (path.contains("\\")) {
            if (path.equals("\\")) {
                pathIsBackslash = true;
            }
            path=path.replace("\\", "/");
        }
        
        if (path.equals("/")) {
            String physicalPath = container.getPhysicalPath();
            
            if (physicalPath == null) { // loose app might return null here
                return null;
            }        

            File file = new File(physicalPath);
            if (!file.exists() || !file.isDirectory()){ //might be a xxx.war, so return null if it is an archive
                return null;
            }
            
            if(pathIsBackslash == true){
                // add backslash back on to the end so there will be a \ at the end.
                // Need to do this to maintain compatibility with twas
                // PM70146
                physicalPath = physicalPath + "\\"; 
                return physicalPath;
            }          
            
            //getAbsolutePath always return a file path without slash in the end, even the file is a directory.
            return file.getAbsolutePath();
        }
        
        try {
            Entry entry = container.getEntry(path);
            if(entry != null){
                String s = entry.getPhysicalPath();
                // if passed in path ends in separator, then make sure return value does also
                if ( (s != null) && (path.length()>1 && path.endsWith("/") &&(s.charAt(s.length() - 1) != File.separatorChar)) ) {
                    s = s + File.separator;
                }
                return s;

            } else {
                if (checkDocRoot == false) {
                    return null;
                }
                
                DocumentRootUtils dru = getDocumentRootUtils(path);
                
                if (dru != null) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                        logger.logp(Level.FINE, CLASS_NAME, "getRealPath", "obtained dru");
                    
                    dru.handleDocumentRoots(path, WCCustomProperties.CHECK_EDR_IN_GET_REAL_PATH);
                    EntryResource er = dru.getMatchedEntryResource();

                    if (er != null) {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME, "getRealPath", "obtained er: " + er);
                        Entry ety = er.getEntry();
                        if (ety != null) {
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                                logger.logp(Level.FINE, CLASS_NAME, "getRealPath", "obtained entry: " + ety);
                            
                            String s = ety.getPhysicalPath();
                            if ((s != null) && (s.length()> 0)) {
                                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                                    logger.logp(Level.FINE, CLASS_NAME, "getRealPath", "getPhysicalPath() is: " + s);
                                // if passed in path ends in separator, then make sure return value does also
                                if (  (path.endsWith("/"))
                                    &&(s.charAt(s.length() - 1) != File.separatorChar)) {
                                    s = s + File.separator;
                                }
                                return s;
                            } else {
                                // to be TWAS compliant, return container path if we are not checking EDRs and can't find the real path
                                //if the app was extracted, we could return the physical path
                                if (!WCCustomProperties.CHECK_EDR_IN_GET_REAL_PATH || WCCustomProperties.GET_REAL_PATH_RETURNS_QUALIFIED_PATH) {
                                    basePath = container.getPhysicalPath();
                                }
                            }
                        }
                    }
                    else{
                        File matchedFile = dru.getMatchedFile();
                        if (matchedFile != null){
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                                logger.logp(Level.FINE, CLASS_NAME, "getRealPath", "obtained file [" + matchedFile.getAbsolutePath() +"]"); 
                            return matchedFile.getAbsolutePath();
                        }
                    }
                }
            }
        } catch(IllegalArgumentException e){
            logger.logp(Level.FINE, CLASS_NAME, "getRealPath", "caught IllegalArgument Exception. path: " + path + "  Exception: " + e);
            return null;
        } catch(IOException e){
            logger.logp(Level.FINE, CLASS_NAME, "getRealPath", "caught IOException processing document root getReadPath: " + path + "  Exception: " + e);

            // to be TWAS compliant, return container path if we are not checking EDRs and can't find the real path
            // but return null if we are checking EDRs and can't find the real path
            if (!WCCustomProperties.GET_REAL_PATH_RETURNS_QUALIFIED_PATH) {
                 return null;
            }
            //if the app was extracted, we could return this...
            basePath = container.getPhysicalPath();
         }
        
    }
    
    if (basePath == null) {
        return null;
    }

    basePath = basePath.replace('\\', '/');
    
    if (File.separatorChar!='/') {
        basePath = basePath.replace('/', File.separatorChar);
        path = path.replace('/', File.separatorChar);
    }

    String realPath = "";
    
    if (path.charAt(0) == File.separatorChar) { 
        realPath = basePath + path;
    } else {
        realPath = basePath + File.separatorChar + path;
    }
    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
        logger.logp(Level.INFO, CLASS_NAME, "getRealPath", "returning path: " + realPath);
    }
    return realPath;
  }

   
  /*
   * (non-Javadoc)
   * 
   * @see javax.servlet.ServletContext#getResourcePaths(java.lang.String)
   */
  public Set getResourcePaths(String path, boolean searchMetaInf)
  {
    java.util.HashSet set = new java.util.HashSet();

    if ((WCCustomProperties.SKIP_META_INF_RESOURCES_PROCESSING) && (searchMetaInf == true)){

        searchMetaInf = false;
        logger.logp(Level.FINE, CLASS_NAME, "getResourcePaths", "override searchMetaInf to false becuase of custom property");
    }
    
    
    if (container != null){
        try {
            addResourcePaths(set, container, path, searchMetaInf);
        } catch (UnableToAdaptException e) {
            throw new IllegalStateException(e);
        }
        
        // PM21451 Start
        // search the static doc roots and include a search of meta-inf resources if boolean parameter is true
        // 96723: Hard code a "false" parameter to avoid searching META-INF resources a second time. The
        // resource paths are already added by Liberty specific container code.
        set.addAll(getStaticDocumentRootUtils().getResourcePaths(path,false));

        // look at the JSP doc roots but don't search meta-inf resources this time (meta-inf resources are common
        // to both doc roots).
        set.addAll(getJSPDocumentRootUtils().getResourcePaths(path,false));
        // PM21451 End
        
        return set;
    }

    java.io.File root = new java.io.File(documentRoot + path);

    if (root.exists())
    {
      java.io.File[] fileList = root.listFiles();
      if (fileList != null)
      {
        for (int i = 0; i < fileList.length; i++)
        {
          String resourcePath = fileList[i].getPath();
          resourcePath = resourcePath.substring(documentRoot.length());
          resourcePath = resourcePath.replace('\\', '/');
          if (fileList[i].isDirectory())
          {
            if (resourcePath.endsWith("/") == false)
            {
              resourcePath += "/";
            }
          }
          set.add(resourcePath);
        }
      }
    }
    return (set);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.ibm.ws.webcontainer.webapp.WebApp#getDefaultExtensionProcessor(com.
   * ibm.ws.webcontainer.webapp.WebApp, java.util.HashMap)
   */
  protected ExtensionProcessor getDefaultExtensionProcessor(com.ibm.ws.webcontainer.webapp.WebApp app, HashMap map)
  {
    return new com.ibm.ws.webcontainer.osgi.extension.DefaultExtensionProcessor(this, map);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.ibm.ws.webcontainer.webapp.WebApp#getInvokerExtensionProcessor(com.
   * ibm.ws.webcontainer.webapp.WebApp)
   */
  protected InvokerExtensionProcessor getInvokerExtensionProcessor(com.ibm.ws.webcontainer.webapp.WebApp app)
  {
    return new com.ibm.ws.webcontainer.osgi.extension.InvokerExtensionProcessor(app, config.getInvokerAttributes());
  }

  @Override
  protected Object loadListener(String className) throws InjectionException, Throwable
  {
    Object listener = null;
    try {
        // If a serialized version of the listener exists create a new object from it by using Beans.instantiate
        // otherwise use a ManagedObject factory to create it for cdi 1.2 support.       
        final String serializedName = className.replace('.','/').concat(".ser");
        final ClassLoader loader = getClassLoader();
        ManagedObject mo=null;
        InputStream is = (InputStream)AccessController.doPrivileged
            (new PrivilegedAction() {
                  public Object run() {
                      return loader.getResourceAsStream(serializedName);
                  }
            });
        if (is!=null) {
            listener = super.loadListener(className);
            mo = injectAndPostConstruct(listener); 
        } else {  
            Class<?> listenerClass = getClassLoader().loadClass(className);        
            mo = injectAndPostConstruct(listenerClass);
            listener = mo.getObject();
        }     
        cdiContexts.put(listener, mo);
    } catch (ClassNotFoundException exc) {
        // some exception, log error.
        logError("Failed to load listener: " + className, exc);        
        if (WCCustomProperties.STOP_APP_STARTUP_ON_LISTENER_EXCEPTION) { //PI58875
            throw exc;
        }
    }
    return listener;
  }
  
  @Override
  public <T extends EventListener> T  createListener(Class<T> classToCreate) throws ServletException
  {
      logger.logp(Level.FINE, CLASS_NAME, "createListener", "called for class: " + classToCreate);

      return super.createListener(classToCreate);
  }
  
  //createFilter only called when adding/creating programmatically
  @Override
  public <T extends Filter> T createFilter(Class<T> classToCreate) throws ServletException {
      
      logger.logp(Level.FINE, CLASS_NAME, "createFilter", "called for class: " + classToCreate);
      return super.createFilter(classToCreate);
  }
 
  //createServlet only called when adding/creating programmatically
  @Override
  public <T extends Servlet> T createServlet(Class<T> classToCreate) throws ServletException {
      logger.logp(Level.FINE, CLASS_NAME, "createServlet", "called for class: " + classToCreate);
      
      return super.createServlet(classToCreate);
  }
  
  @Override
  protected void initializeFilterManager()
  {
    if (filterManager == null)
    {
      filterManager = new WebAppFilterManagerImpl(config, this);
      filterManager.init();
    }
  }

  protected void initializeServletContextFacades()
  {
    // TODO Auto-generated method stub

  }

  /**
   * Tell if servlet-container-initializer (SCI) annotation processing is to be done on the
   * class of a specified initializer.  Provide the answer as a side effect.  If processing
   * is to be done, add the initializer to the specified initializer list.  If processing is
   * not to be done, do not add the initializer.
   * 
   * @param sci The candidate servlet container initializer.
   * @param scis Storage for initializers which are to be processed.
   */
  protected void determineWhetherToAddScis(ServletContainerInitializer sci, List<ServletContainerInitializer> scis) {
      // SCIs from DS are already added      

      if (acceptAnnotationsFrom(sci.getClass().getName(), DO_ACCEPT_PARTIAL, DO_NOT_ACCEPT_EXCLUDED)) {
          scis.add(sci);
      }
  }

  private WebAnnotations getWebAnnotations() throws UnableToAdaptException {
      return AnnotationsBetaHelper.getWebAnnotations( getModuleContainer() );
  }

  /*
   * F743-31926
   */
  @Override
  protected void scanForHandlesTypesClasses(
      com.ibm.ws.container.DeployedModule deployedModule,
      HashMap<ServletContainerInitializer, Class[]> handleTypesMap,
      HashMap<ServletContainerInitializer, HashSet<Class<?>>> onStartupMap) {

      String methodName = "scanForHandlesTypesClasses";

      boolean enableTrace =
          ( com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE) );

      WebAnnotations webAppAnnos;
      AnnotationTargets_Targets annoTargets;
      InfoStore infoStore;
      try {
          webAppAnnos = getWebAnnotations();
          annoTargets = webAppAnnos.getAnnotationTargets();
          infoStore = webAppAnnos.getInfoStore();
      } catch ( UnableToAdaptException e ) {
          return; // FFDC
      }

      boolean didOpen = false;

      try {
          for ( Map.Entry<ServletContainerInitializer, Class[]> sciEntry : handleTypesMap.entrySet() ) {
              ServletContainerInitializer sci = sciEntry.getKey();
              Class[] handledTypes = sciEntry.getValue();
              Set<Class<?>> startupTypes = onStartupMap.get(sci);
    
              for ( Class<?> handledType : handledTypes ) {
                  String handledTypeName = handledType.getName();

                  boolean isAnnotation = handledType.isAnnotation();
                  if ( enableTrace ) {
                      logger.logp(Level.FINE, CLASS_NAME, methodName, "Handled Type      [ {0} ]", handledTypeName);
                      logger.logp(Level.FINE, CLASS_NAME, methodName,"  isAnnotation     [ {0} ]", Boolean.valueOf(isAnnotation));
                  }

                  if ( isAnnotation ) {
                      boolean isClassAnnotation = WebAppSCIHelper.isClassTarget(handledType);
                      boolean isInheritedAnnotation = isClassAnnotation && handledType.isAnnotationPresent(java.lang.annotation.Inherited.class);
                      boolean isMethodAnnotation = WebAppSCIHelper.isMethodTarget(handledType);

                      if ( enableTrace ) {
                          logger.logp(Level.FINE, CLASS_NAME, methodName,"  isClassAnno      [ {0} ]", Boolean.valueOf(isClassAnnotation));
                          logger.logp(Level.FINE, CLASS_NAME, methodName,"  isInherited      [ {0} ]", Boolean.valueOf(isInheritedAnnotation));
                          logger.logp(Level.FINE, CLASS_NAME, methodName,"  isMethodAnno     [ {0} ]", Boolean.valueOf(isMethodAnnotation));
                      }

                      if ( isClassAnnotation ) {
                          // d95160: Injection classes are obtained from metadata-complete and metadata-incomplete
                          //         regions, but not from excluded regions.

                          // Note: This is different from TWAS, which also obtains injection classes from EXCLUDED regions.

                          Set<String> targetClassNames;
                          if ( isInheritedAnnotation ) {
                              targetClassNames = annoTargets.getAllInheritedAnnotatedClasses(handledTypeName,
                                                                                                AnnotationTargets_Targets.POLICY_SEED_AND_PARTIAL);
                          } else {
                              targetClassNames = annoTargets.getAnnotatedClasses(handledTypeName,
                                                                                    AnnotationTargets_Targets.POLICY_SEED_AND_PARTIAL);
                          }

                          String classReason = "Selection on class annotation [ " + handledTypeName + " ]";
                          for ( String targetClassName : targetClassNames ) {
                              addClassToHandlesTypesStartupSet(targetClassName, startupTypes, classReason);
                          }                      
                      }

                      if ( isMethodAnnotation ) {
                          Set<String> testedClassNames = new HashSet<String>();
                          Set<String> foundClassNames = new HashSet<String>();

                          // d95160: Injection classes are obtained from metadata-complete and metadata-incomplete
                          //         regions, but not from excluded regions.

                          // Note: This is different from TWAS, which also obtains injection classes from EXCLUDED regions.

                          Set<String> targetClassNames = annoTargets.getClassesWithMethodAnnotation(handledTypeName,
                                                                                                    AnnotationTargets_Targets.POLICY_SEED_AND_PARTIAL);

                          for ( String targetClassName : targetClassNames ) {
                              if ( testedClassNames.contains(targetClassName) ) {
                                  continue;
                              } else {
                                  testedClassNames.add(targetClassName);
                              }

                              foundClassNames.add(targetClassName);

                              // d95160: An API should be added to obtain subclasses from a specified region,
                              //         as with the annotations access.

                              Set<String> targetSubclassNames = annoTargets.getSubclassNames(targetClassName);

                              for ( String targetSubclassName : targetSubclassNames ) {
                                  if ( testedClassNames.contains(targetSubclassName)) { 
                                      continue;
                                  } else {
                                      testedClassNames.add(targetSubclassName);
                                  }

                                  if ( !didOpen ) {
                                      try {
                                          webAppAnnos.openInfoStore();
                                      } catch ( UnableToAdaptException e ) {
                                          return; // FFDC
                                      }
                                      didOpen = true;
                                  }

                                  ClassInfo subclassInfo = infoStore.getDelayableClassInfo(targetSubclassName);
                                  if ( WebAppSCIHelper.anyMethodHasAnnotation(subclassInfo, handledTypeName) ) {
                                      foundClassNames.add(targetSubclassName);
                                  }
                              }
                          }

                          String methodReason = "Selection on method annotation [ " + handledTypeName + " ]";
                          for ( String foundClassName : foundClassNames ) {
                              addClassToHandlesTypesStartupSet(foundClassName, startupTypes, methodReason);                          
                          }
                      }

                  } else {
                      if ( enableTrace ) {
                          logger.logp(Level.FINE, CLASS_NAME, methodName, "Selection Type: [ {0} ]", handledType.getName() );
                      }

                      // add the @HandlesTypes param class for init only if excludeAllHandledTypesClasses=false
                      if (!WCCustomProperties.EXCLUDE_ALL_HANDLED_TYPES_CLASSES) {
                          String actualClassReason = "Selection of handlesType class [ " + handledTypeName + " ]";
                          addClassToHandlesTypesStartupSet(handledTypeName, startupTypes, actualClassReason);
                      }
                      // if @HandlesTypes param is an interface look for implementors, otherwise look for subclasses
                      if ( ((com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets) annoTargets).isInterface(handledTypeName) ) {
                          String interfaceReason = "Selection on interface [ " + handledTypeName + " ]";
                          Set<String> implementerClassNames = annoTargets.getAllImplementorsOf(handledTypeName);
                          for ( String implementerClassName : implementerClassNames ) {
                              addClassToHandlesTypesStartupSet(implementerClassName, startupTypes, interfaceReason);
                          }
                      } else {
                          String classesReason = "Selection on sub-classes of [ " + handledTypeName + " ]";
                          for ( String targetClassName : annoTargets.getSubclassNames(handledTypeName)) {
                              addClassToHandlesTypesStartupSet(targetClassName, startupTypes, classesReason);
                          }
                      }
                  }
              }
          }

      } finally {
          if ( didOpen ) {
              try {
                  webAppAnnos.closeInfoStore();
              } catch ( UnableToAdaptException e ) {
                  // FFDC
              }
          }
      }
  }

  protected void addClassToHandlesTypesStartupSet(String targetClassName, Set<Class<?>> handlesTypesOnStartupSet, String reasonText) {
      String methodName = "addClassToHandlesTypesStartupSet";
      
      boolean enableTrace = ( com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable (Level.FINE) );

      int servletSpecLevel = com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel();
      //check if excluded because the subclasses includes this... and could be more than we want
      //might need to exclude something else like javax.* or java*
      if(servletSpecLevel <=  com.ibm.ws.webcontainer.osgi.WebContainer.SPEC_LEVEL_40){
          if ((targetClassName.startsWith("java.")) || (targetClassName.startsWith("javax."))) {
                  if ( enableTrace ) {
                        logger.logp(Level.FINE, CLASS_NAME, methodName,
                                    "Internal class, {0}, is not added to the ServletContainerInitializers [Servlet Spec Level: {2}] in the application: {1}.",
                                    new Object[] { targetClassName, this.config.getDisplayName(), servletSpecLevel});
                  }
                      return;
          }
      } else {
              if ((targetClassName.startsWith("java.")) || (targetClassName.startsWith("jakarta."))) {
                  if ( enableTrace ) {
                        logger.logp(Level.FINE, CLASS_NAME, methodName,
                                    "Internal class, {0}, is not added to the ServletContainerInitializers [Servlet Spec Level: {2}] in the application: {1}.",
                                    new Object[] { targetClassName, this.config.getDisplayName(), servletSpecLevel });
                  }
                      return;
              }
      }

      try {
          WebAnnotations webAppAnnotations = getWebAnnotations();
          AnnotationTargets_Targets table = webAppAnnotations.getAnnotationTargets();
          //means that the scanner went from an included jar to an excluded jar (because of class heirarchy)
          //in order to get the complete set of scan data.  
          //this object itself should be excluded
          if (table.isExcludedClassName(targetClassName)) {
              if ( enableTrace ) { 
                  logger.logp(Level.FINE, CLASS_NAME, methodName,
                              "Class, {0}, exists in a fragment that has been excluded in the application: {1}",
                              new Object[] { targetClassName, this.config.getDisplayName() });
              }
              return;
          }
      } catch (UnableToAdaptException e) {
          if (enableTrace) {
              logger.logp(Level.FINE, CLASS_NAME, methodName, "caught UnableToAdaptException: " + e);
          }
          return;
      }
      
      Class<?> targetInitializerClass;
      try {
          if ( enableTrace ) { 
              logger.logp(Level.FINE, CLASS_NAME, methodName,"InitializeClassInHandlesTypesStartup: {0} ", WCCustomProperties.INITIALIZE_CLASS_IN_HANDLES_TYPES_STARTUP);
          }
          targetInitializerClass = Class.forName(targetClassName,
                                           WCCustomProperties.INITIALIZE_CLASS_IN_HANDLES_TYPES_STARTUP, //160846
                                           ThreadContextHelper.getContextClassLoader());
      } catch ( ClassNotFoundException e ) {
          targetInitializerClass = null;
                          
          if (WCCustomProperties.LOG_SERVLET_CONTAINER_INITIALIZER_CLASSLOADER_ERRORS) {
              logger.logp(Level.WARNING, CLASS_NAME, methodName,
                          "exception.occurred.while.initializing.ServletContainerInitializers.class.lookup",
                          new Object[] { targetClassName, this.config.getDisplayName() });
              
          } else if ( enableTrace ) { 
              logger.logp(Level.FINE, CLASS_NAME, methodName,
                          "exception.occurred.while.initializing.ServletContainerInitializers.class.lookup",
                          new Object[] { targetClassName, this.config.getDisplayName() });
          }
      } catch (NoClassDefFoundError e){   //PM96235
          targetInitializerClass = null;
          
          if (WCCustomProperties.LOG_SERVLET_CONTAINER_INITIALIZER_CLASSLOADER_ERRORS) {
              logger.logp(Level.WARNING, CLASS_NAME, methodName,
                          "exception.occurred.while.initializing.ServletContainerInitializers.class.lookup",
                          new Object[] { targetClassName, this.config.getDisplayName() });
              
          } else if ( enableTrace ) { 
              logger.logp(Level.FINE, CLASS_NAME, methodName,
                          "exception.occurred.while.initializing.ServletContainerInitializers.class.lookup",
                          new Object[] { targetClassName, this.config.getDisplayName() });
          }   
      } //PM96235 - end
                      
      if ( targetInitializerClass != null ) {
          if (handlesTypesOnStartupSet.contains(targetInitializerClass)) {
              if ( enableTrace ) { 
                  logger.logp(Level.FINE, CLASS_NAME, methodName, "{0} already in onStartup set for {1} : {2}",
                      new Object[] { targetClassName, this.config.getDisplayName(), reasonText });
              }
          } else {
              handlesTypesOnStartupSet.add(targetInitializerClass);
              if ( enableTrace ) { 
                  logger.logp(Level.FINE, CLASS_NAME, methodName,
                              "Adding initializer [ {0} ] to [ {1} ] [ {2} ]",
                              new Object[] { targetClassName, this.config.getDisplayName(), reasonText });
              }
          }
      }          
  }

  public WebComponentMetaData getWebAppCmd()
  {
    return ((WebAppConfiguration)config).getDefaultComponentMetaData();
  }

  public WebComponentMetaData createComponentMetaData(String servletName) throws MetaDataException
  {
    WebComponentMetaDataImpl wccmd = new WebComponentMetaDataImpl(config.getMetaData());
    J2EEName moduleJ2EEName = getModuleMetaData().getJ2EEName();
    wccmd.setJ2EEName(j2eeNameFactory.create(moduleJ2EEName.getApplication(), moduleJ2EEName.getModule(), servletName));
    metaDataService.fireComponentMetaDataCreated(wccmd);
    return wccmd;
  }
  protected WebComponentMetaData[] internalServletMetaData = new WebComponentMetaData[internalServletList.length];

  protected ServletConfig createConfig(String servletName, int internalIndex) throws ServletException
  {
    ServletConfig sconfig = new ServletConfig(servletName, getWebAppConfig());
    WebComponentMetaData wccmd = internalServletMetaData[internalIndex];
    if (wccmd == null) {
        WebComponentMetaDataImpl wccmdImpl = new WebComponentMetaDataImpl(config.getMetaData());
        J2EEName moduleJ2EEName = getModuleMetaData().getJ2EEName();
        wccmdImpl.setJ2EEName(j2eeNameFactory.create(moduleJ2EEName.getApplication(), moduleJ2EEName.getModule(), servletName));
        wccmd = internalServletMetaData[internalIndex] = wccmdImpl;
    }
    sconfig.setMetaData(wccmd);
    return sconfig;
  }


  @Override
  public Servlet getSimpleFileServlet()
  {
      if (defaultExtProc==null)
          defaultExtProc= new DefaultExtensionProcessor(this,getConfiguration().getFileServingAttributes());
      return defaultExtProc;
  }

  @Override
  public Servlet getDirectoryBrowsingServlet() {
      if (directoryBrowsingServlet==null) {
          directoryBrowsingServlet = new DirectoryBrowsingServlet();
      } 
      return directoryBrowsingServlet;
  }
  
  @Override
  public boolean getExtensionProcessingDisabled() {
      // Introduced to support the REST connector.
      return this.extensionProcessingDisabled;
  }
  
  protected ICollaboratorHelper createCollaboratorHelper(com.ibm.ws.container.DeployedModule moduleConfig)
  {
    // TODO Auto-generated method stub
    return new CollaboratorHelperImpl(this, moduleConfig);
  }

  public boolean isNamespacePopulated()
  {
    return namespacePopulated;
  }

  public void setNamespacePopulated(boolean namespacePopulated)
  {
    this.namespacePopulated = namespacePopulated;
  }
  
  
  // Method introduced for CDI 1.2 support - need to allow CDI to create the object 
  // in order to support constructor injection. 
  //
  // Note: 3 versions of ManagedObjectService and ManagedObjectFactory exist:
  //
  // Default (neither cdi 1.0 or cdi 1.2 enabled) - creates a new instance.
  // cdi 1.0 enabled : creates new instance then wraps it in a managed object.
  // cdi 1.2 enabled : creates an instance allowing for constructor injection and then wraps it in a managed object
  //
  // As a result this code can be common to each scenario.
  //
  public ManagedObject inject(Class<?> Klass) throws InjectionException
  {
      return inject(Klass, null);
  }
  
  public ManagedObject inject(Class<?> Klass, ClassLoader cl) throws InjectionException
  {
      ManagedObject<?> r = null;    
      final String METHOD_NAME="inject(class)";
      
      if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
          logger.entering(CLASS_NAME, METHOD_NAME, Klass);
      }
      try { 
          if (null != managedObjectService){
              
              // Make sure the class is know to the injection engine
              processDynamicInjectionMetaData(Klass);
              ManagedObject<?> mo;
             
              try{

                  ModuleMetaData moduleMetaData = getModuleMetaData();
                  ManagedObjectFactory<?> mof = managedObjectService.createManagedObjectFactory(moduleMetaData, Klass, false);     

                  mo = mof.createManagedObject();

              } // New path to handle a class outside of war
              catch (IllegalStateException ise){
                  
                  if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                      logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "IllegalStateException from managed object service:" + ise );
                   }   
                  
                  // Managed object service failed to create the managed object so, if CDI was enabled, CDI will
                  // not work for the object being created. Instead create an internal managed object so that other
                  // injections will work.
                 if (cl == null){
                      cl = getClassLoader();
                  }    
                  mo = new WCManagedObjectImpl(java.beans.Beans.instantiate(cl, Klass.getName()));
                  
               }
              r = mo;
              mo.inject(this.referenceContext);
          }
      } catch (InjectionException iex) {
          throw iex;
      } catch (Exception ex) {
          throw new InjectionException(ex.getCause() != null ? ex.getCause() : ex);
      } 
      if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
          logger.exiting(CLASS_NAME, METHOD_NAME,r);
      }
      return r;
  }
  
  /**
   * This method injects the target which is typically a servlet, filter or a event listener and returns the ManagedObject associated with it. 
   * The ManagedObject is in turn released in the destroy/constextDestroyed lifecycle method of the target.
   * 
   * @param target the filter, servlet or listener that is being injected into 
   * @return  This method will either return the managed object if the injection was successful or throw the exception that resulted during the inject
   * @throws InjectionException if there was an error during the injection
   */
  public ManagedObject inject(final Object target) throws InjectionException
  {
      final String METHOD_NAME="inject";
      
       if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
           logger.entering(CLASS_NAME, METHOD_NAME, target);
       }
      
        ManagedObject r = null;  
        try {
            if (null != managedObjectService){
                
                // Make sure the class is know to the injection engine
                processDynamicInjectionMetaData(target.getClass());
                ManagedObject mo;
               
                try {
                    ModuleMetaData moduleMetaData = getModuleMetaData();
                    ManagedObjectFactory<Object> mof = (ManagedObjectFactory<Object>) managedObjectService.createManagedObjectFactory(moduleMetaData, target.getClass(), false);
                    mo = mof.createManagedObject(target, null);
                } catch (ManagedObjectException moe){
                    
                    if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "IllegalStateException from managed object service:" + moe );
                     }   
                    
                     mo = new WCManagedObjectImpl(target);
                 }
              
                r = mo;
                mo.inject(this.referenceContext);
           }
        } catch (ManagedObjectException e) {
            throw new InjectionException(e.getCause());
        }
        
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, METHOD_NAME, r);
        }
        return r;
  }
  
  /**
   * This method injects the target object, then immediately performs PostConstruct operations
   * 
   * @param target the filter, servlet or listener that is being injected into 
   * @return  This method will either return the managed object if the injection was successful or throw the exception that resulted during the inject or the PostConstruct
   * the injection
   * @throws InjectionException if there was an error during the injection
   */
  public ManagedObject injectAndPostConstruct(final Object target) throws InjectionException
  {
      final String METHOD_NAME="injectAndPostConstruct";
      
       if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
           logger.entering(CLASS_NAME, METHOD_NAME, target);
       }
       
       // PI30335: split inject logic from injectAndPostConstruct
       ManagedObject r = inject(target);
    
        // after injection then PostConstruct annotated methods on the host object needs to be invoked.
        Throwable t = this.invokeAnnotTypeOnObjectAndHierarchy(target, ANNOT_TYPE.POST_CONSTRUCT);
        if (null != t){
            // log exception - and process InjectionExceptions so the error will be returned to the client as an error. 
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Exception caught during post construct processing: " + t);
            }
            if ( t instanceof InjectionException) {
                InjectionException ie = (InjectionException) t;
                throw ie;
            } else {
                // According to spec, can't proceed if invoking PostContruct(s) threw exceptions
                RuntimeException re = new RuntimeException(t);
                throw re;
            }
        }
        
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, METHOD_NAME, r);
        }
        return r;
  }
  
  @Override
  protected <T> T createAsManageObject(final Class<?> Klass) throws ServletException {
      ManagedObject<?> mo = null;
      try {
          if (System.getSecurityManager()!=null) {
              mo = AccessController.doPrivileged(new PrivilegedExceptionAction<ManagedObject<?>>() {
                  @Override
                  public ManagedObject<?> run() throws Exception {
                      return injectAndPostConstruct(Klass);
                  }
              });
          } else { 
              mo = injectAndPostConstruct(Klass);
          }
          cdiContexts.put(mo.getObject(), mo);
          return ((T)mo.getObject());
      } catch (Exception e) {
          throw new ServletException(e);
      }
  }   

  //public ManagedObject injectAndPostConstruct(Class<?> Klass, ClassLoader cl) throws InjectionException
  public ManagedObject injectAndPostConstruct(Class<?> Klass) throws InjectionException
  { 
      return injectAndPostConstruct(Klass, null);
  }
  
  public ManagedObject injectAndPostConstruct(Class<?> Klass, ClassLoader cl) throws InjectionException
  {
      final String METHOD_NAME="injectAndPostConstruct(class)";
      
       if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
           logger.entering(CLASS_NAME, METHOD_NAME, Klass);
       }
       
       // PI30335: split inject logic from injectAndPostConstruct
       ManagedObject r = inject(Klass, cl);
    
        // after injection then PostConstruct annotated methods on the host object needs to be invoked.
        Throwable t = this.invokeAnnotTypeOnObjectAndHierarchy(r.getObject(), ANNOT_TYPE.POST_CONSTRUCT);
        if (null != t){
            // log exception - and process InjectionExceptions so the error will be returned to the client as an error. 
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Exception caught during post construct processing: " + t);
            }
            if ( t instanceof InjectionException) {
                InjectionException ie = (InjectionException) t;
                throw ie;
            } else {
                // According to spec, can't proceed if invoking PostContruct(s) threw exceptions
                RuntimeException re = new RuntimeException(t);
                throw re;
            }
        }
        
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, METHOD_NAME, r);
        }
        return r;
  }

  public void performPreDestroy(Object target) throws InjectionException
  {
    // after injection then PostConstruct annotated methods on the host object needs to be invoked.
    Throwable t = this.invokeAnnotTypeOnObjectAndHierarchy(target, ANNOT_TYPE.PRE_DESTROY); 
    if (t != null) {
        // log exception - could be from user's code - and move on 
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "inject", "Exception caught during pre destroy processing: " + t);
        }
    }
    
  }

  
  public void destroy() {
      //PI58875
      if (super.getDestroyed().booleanValue()){
          if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
              logger.logp(Level.FINE, CLASS_NAME, "destroy", "WebApp is already destroyed. RETURN. this ->" + this);

          return;
      }
      
      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
          logger.logp(Level.FINE, CLASS_NAME, "destroy", "osgi.WebApp.destroy entered. this -> "+ this);
      
      super.destroy();
      
      AnnotationHelperManager.removeInstance(this);
      
  }    
      
  public void setOrderedLibPaths(List<String> orderedLibPaths) {
      this.orderedLibPaths = orderedLibPaths;
  }
  
  private DocumentRootUtils getDocumentRootUtils(String uri) {

      boolean useJSPRoot=false;
      DocumentRootUtils docRoot=null;

      // Only need to check if there is a Doc Root of one sort or the other.
      if (staticDocRoot.hasDocRoot() || jspDocRoot.hasDocRoot()) {

              // See if the resourcece would map to a jsp request processor.
              RequestProcessor requestProcessor = requestMapper.map(uri);

              if (requestProcessor!=null) {

                  try {

                      Class jspProcessorClass = Class.forName("com.ibm.ws.jsp.webcontainerext.AbstractJSPExtensionProcessor");                            
                      useJSPRoot = jspProcessorClass.isInstance(requestProcessor);

                      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                          logger.logp(Level.FINE, CLASS_NAME, "useJSPDocRoot", "useJSPRoot = " + useJSPRoot + ", request Processor is " + requestProcessor.getClass().getName());
                      }

                  } catch (ClassNotFoundException cnfe) {

                      useJSPRoot=false;

                      if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                          logger.logp(Level.FINE, CLASS_NAME, "useJSPDocRoot", "useJSPRoot = " + useJSPRoot + ", ClassNotFoundException.", cnfe);
                      }
                  }
              }    
      }
      if (useJSPRoot) {
          docRoot = getJSPDocumentRootUtils();                
      } else {
          docRoot = getStaticDocumentRootUtils();             
      }

      return docRoot;
  }
  
  // PM21451 Add convenience method
  public DocumentRootUtils getJSPDocumentRootUtils() {
      return new DocumentRootUtils(this,jspDocRoot.getedrSearchPath(),jspDocRoot.getpfedrSearchPath());
  }

  public DocumentRootUtils getStaticDocumentRootUtils() {
      return new DocumentRootUtils(this,staticDocRoot.getedrSearchPath(),staticDocRoot.getpfedrSearchPath());
  }
  
  /*
   * (non-Javadoc)
   * 
   * New method added for HttpSessionIdListener in Servlet 3.1, always return false in
   * this Servlet 3.0 implementation.
   * 
   * @see com.ibm.ws.webcontainer.webapp.WebApp#isHttpSessionIdListener()
   */
  protected boolean isHttpSessionIdListener(Object listener) {
      return false;
  }
  
  @Override
  protected void checkForSessionIdListenerAndAdd(Object listener){
     return; // do nothing for Servlet 3.0.
  }
  
  public void registerWebSocketHandler(WsocHandler wsocServHandler) {
      return; // NO-OP
  }
  
  public WsocHandler getWebSocketHandler() {
      return null; // NO-OP
  }
  
  public H2Handler getH2Handler() {
      return null; // NO-OP
  }   
}
