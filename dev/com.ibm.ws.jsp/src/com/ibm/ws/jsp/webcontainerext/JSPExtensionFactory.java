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
package com.ibm.ws.jsp.webcontainerext;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ExpressionFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.javaee.dd.webext.Attribute;
import com.ibm.ws.javaee.dd.webext.WebExt;
import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JSPStrBufferFactory;
import com.ibm.ws.jsp.JSPStrBufferImpl;
import com.ibm.ws.jsp.JspShim;
import com.ibm.ws.jsp.configuration.JspXmlExtConfig;
import com.ibm.ws.jsp.taglib.GlobalTagLibraryCache;
import com.ibm.ws.jsp.translator.visitor.generator.GeneratorUtilsExtFactory;
import com.ibm.ws.jsp.translator.visitor.validator.ElValidatorExtFactory;
import com.ibm.ws.jsp.webcontainerext.ws.PrepareJspHelperFactory;
import com.ibm.ws.jsp.webxml.JspConfiguratorHelper;
import com.ibm.ws.webcontainer.WebContainer;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.el.ELFactoryWrapperForCDI;
import com.ibm.wsspi.jsp.context.JspClassloaderContext;
import com.ibm.wsspi.jsp.taglib.config.GlobalTagLibConfig;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.extension.ExtensionFactory;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

@Component(configurationPid="com.ibm.ws.jsp.2.2",
   configurationPolicy=ConfigurationPolicy.REQUIRE,
   property="service.vendor=IBM")
public class JSPExtensionFactory extends AbstractJSPExtensionFactory implements ExtensionFactory {
    static protected Logger logger;
    private static final String CLASS_NAME="com.ibm.ws.jsp.webcontainerext.JSPExtensionFactory";
    static {
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }

    private volatile boolean osgiAppsCanProvideJSTL;
    
    private volatile Properties defaultProperties = new Properties();

    private final AtomicServiceReference<ELFactoryWrapperForCDI> expressionFactoryService = new AtomicServiceReference<ELFactoryWrapperForCDI>("ExpressionFactoryService"); //cdi wraps this

    @Reference
    private WsLocationAdmin locationService;
    @Reference
    private PrepareJspHelperFactory prepareJspHelperFactory;
    @Reference
    private ElValidatorExtFactory elValidatorExtFactory;
    @Reference
    private GeneratorUtilsExtFactory generatorUtilsExtFactory;
    @Reference
    private JspVersionFactory jspVersionFactory;
    @Reference
    private ClassLoadingService classLoadingService;
    private BundleContext bundleContext;
    
    /**
     * Active JSPExtensionFactory instance. May be null between deactivate and activate
     * calls.
     */
    private static final AtomicReference<JSPExtensionFactory> instance = new AtomicReference<JSPExtensionFactory>();
    
    /**
     * Inject an <code>WrapperExpressionFactory</code> service instance.
     * 
     * @param expressionFactoryService
     *            an expressionFactory service to wrap the default ExpressionFactory
     */
    @Reference(cardinality=ReferenceCardinality.OPTIONAL, policyOption=ReferencePolicyOption.GREEDY)
    protected void setExpressionFactoryService(ServiceReference<ELFactoryWrapperForCDI> expressionFactoryService) {
        this.expressionFactoryService.setReference(expressionFactoryService);
    }

    /**
     * Remove the <code>WrapperExpressionFactory</code> service instance.
     * 
     * @param expressionFactoryService
     *            an expressionFactory service to wrap the default ExpressionFactory
     */
    protected void unsetExpressionFactoryService(ServiceReference<ELFactoryWrapperForCDI> expressionFactoryService) {
        this.expressionFactoryService.unsetReference(expressionFactoryService);
    }
    
    public static ELFactoryWrapperForCDI getWrapperExpressionFactory() {
        JSPExtensionFactory thisService = instance.get();
        if (thisService != null) {
            ELFactoryWrapperForCDI wrapperExpressionFactory = thisService.expressionFactoryService.getService();
            if (wrapperExpressionFactory!=null) {
                wrapperExpressionFactory.setExpressionFactory(ExpressionFactory.newInstance());
            }
            return wrapperExpressionFactory;
        }
        return null;
    }

    @Activate
    protected void activate(ComponentContext ctx, Map<Object, Object> properties, BundleContext bundleContext) {
        expressionFactoryService.activate(ctx);
        this.bundleContext = bundleContext;

        modified(properties);
        // set default impl as it's almost identical to WAS impl WASJSPStrBufferImpl
        JSPStrBufferFactory.set(JSPStrBufferImpl.class);

        instance.set(this);
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        // Clear this as the active instance
        instance.compareAndSet(this, null);
        expressionFactoryService.deactivate(ctx);
    }

    @Modified
    protected void modified(Map<Object, Object> properties) {
        Boolean osgiAppsCanProvideJSTL = (Boolean) properties.get("osgiAppsCanProvideJSTL");
        this.osgiAppsCanProvideJSTL = osgiAppsCanProvideJSTL == null? false: osgiAppsCanProvideJSTL;
        defaultProperties = new Properties();
        for(Map.Entry<Object, Object> entry : properties.entrySet()) {
            Object keyObj = entry.getKey();
            String key = (keyObj == null) ? null : getOrigPropName(String.valueOf(keyObj));
            Object valueObj = entry.getValue();
            String value = (valueObj == null) ? null : String.valueOf(valueObj);

            if (key != null && value != null)
                defaultProperties.put(key, value);
        }
    }
    
    private final static HashMap<String, String> FullyQualifiedPropertiesMap = new HashMap<String, String>();
    static { 
        JSPExtensionFactory.FullyQualifiedPropertiesMap.put("keepGenerated", "keepgenerated");
    }
    
    private String getOrigPropName(String newKey) {
        String s = JSPExtensionFactory.FullyQualifiedPropertiesMap.get(newKey);
        if (s==null) {
            return newKey;
        } else {
            return s;
        }
    }

    protected JspXmlExtConfig createConfig(IServletContext webapp) {
        synchronized (this) {
            JspShim.setDefaultJspFactory();
        }
        JspXmlExtConfig jspExtConfig = null;
        try {
            Container adaptableContainer = webapp.getModuleContainer();
            jspExtConfig = adaptableContainer.adapt(JspXmlExtConfig.class);
            if (jspExtConfig == null) {
                jspExtConfig = new JspConfiguratorHelper(null);
            }
            
            Properties propsFromWebXml = null;
            
            WebExt webExt = adaptableContainer.adapt(WebExt.class);
            if (webExt!=null) {
                List<Attribute> jspAttributeInWebExt = webExt.getJspAttributes();
                
                if (jspAttributeInWebExt!=null) {
                    propsFromWebXml = new Properties();
                    propsFromWebXml.putAll(defaultProperties);
                    for (Attribute a:jspAttributeInWebExt) {
                        propsFromWebXml.put(a.getName(), a.getValue());
                    }
                }
            }        
                
            WebAppConfig wac = webapp.getWebAppConfig();
            boolean isJCDIEnabled = wac.isJCDIEnabled();
            jspExtConfig.setJCDIEnabledForRuntimeCheck(isJCDIEnabled);
            
            //If we found properties in the web.xml, use those rather than just the defaults
            if (propsFromWebXml != null) {
                jspExtConfig.getJspOptions().populateOptions(propsFromWebXml);
            } else {
                jspExtConfig.getJspOptions().populateOptions(defaultProperties);
            }
            
            //The system property javax.servlet.context.tempdir is used to set the scratchdir option on a server-wide basis. (See WebApp.java)
            //The JSP engine scratchdir parameter takes precedence over this system property. 
            //Try scratchdir parameter, then com.ibm.websphere.servlet.temp.dir, then java.io.tmpdir
            File outputDir = (File) webapp.getAttribute(Constants.TMP_DIR); //javax.servlet.context.tempdir
            String scratchdir = jspExtConfig.getJspOptions().getScratchDir();
            if (scratchdir != null) {
                outputDir = new File(getTempDirectory(scratchdir, webapp, true, true));
            }
            if (outputDir == null) {
                String dir = System.getProperty("com.ibm.websphere.servlet.temp.dir");
                if (dir == null)
                    dir = System.getProperty("java.io.tmpdir");
                outputDir = new File(dir);
            }
            jspExtConfig.getJspOptions().setOutputDir(outputDir.getCanonicalFile());
            webapp.setAttribute(Constants.TMP_DIR, outputDir); //javax.servlet.context.tempdir
            logger.logp(Level.FINE, CLASS_NAME, "createConfig", "Output dir is:" + outputDir.getPath());
            
            //PK93292: ALLOW THE USE OF WEBSPHERE VARIABLES IN EXTENDEDDOCUMENTROOT JSP ATTRIBUTE.
            String extendedDocumentRoot = jspExtConfig.getJspOptions().getExtendedDocumentRoot();
            if(extendedDocumentRoot!=null){
                String expanded = extendedDocumentRoot;
                try{
                    expanded = resolveString(extendedDocumentRoot);
                    jspExtConfig.getJspOptions().setExtendedDocumentRoot(expanded);
                }catch (Exception e){
                    // TODO: This should probably be a warning, with an nls. 
                    logger.logp(Level.FINE, CLASS_NAME, "createConfig", "varaible expansion failed for extendedDocumentRoot", e);
                }
            }
            

            
        }
        catch (IOException e) {
            FFDCFilter.processException(e, "com.ibm.ws.jsp.webcontainerext.JSPExtensionFactory.createConfig", "299");
        }
        catch (UnableToAdaptException e) {
            FFDCFilter.processException(e, "com.ibm.ws.jsp.webcontainerext.JSPExtensionFactory.createConfig", "303");
        }
        return jspExtConfig;

    }
    // begin PK31450
    public String getTempDirectory(IServletContext webapp) {
        return getTempDirectory(webapp, true);
    }



    public String getTempDirectory(IServletContext webapp, boolean checkZOSFlag) {
        // LIBERTY specific code
        String sr = com.ibm.ws.webcontainer.osgi.WebContainer.getTempDirectory();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) /* MD15600 */
            logger.logp(Level.FINE, CLASS_NAME, "getTempDirectory", "Using.[{0}].as.server.root", sr); /* MD15600 */
        if (sr == null) { /* MD15600 */
            return sr; /* MD15600 */
        } /* MD15600 */

        return getTempDirectory(sr, webapp, false, checkZOSFlag);
    }
    public String getTempDirectory(String dirRoot, IServletContext webapp, boolean override, boolean checkZOSFlag) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.logp(Level.FINEST, CLASS_NAME, "getTempDirectory", "dirRoot-->" + dirRoot + " override --> " + override + " checkZOSFlag --> "
                    + checkZOSFlag);
        }
        WebAppConfig config = webapp.getWebAppConfig();
        StringBuilder dir = new StringBuilder(dirRoot);

        if (!(dir.charAt(dir.length() - 1) == java.io.File.separatorChar)) {
            dir.append(java.io.File.separator);
        }

        // begin 247392, part 2
        if (checkZOSFlag && !WebContainer.isDefaultTempDir()) {
            // END PK31450
            // Begin 257796, part 1
            dir.append(getNodeName()).append(java.io.File.separator).append(getServerName().replace(' ', '_')).append(
                    "_" + WebContainer.getWebContainer().getPlatformHelper().getServerID());
            if (WebContainer.getTempDir() == null) {
                WebContainer.setTempDir(dir.toString());
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, "getTempDirectory", "ZOS temp dir is:" + WebContainer.getTempDir());
            }
            dir.append(java.io.File.separator).append(getApplicationName(webapp).replace(' ', '_')).append(java.io.File.separator).append(
                    config.getModuleName().replace(' ', '_'));
            // End 257796, part 1
        } else
            dir.append(getTempDirChildren(webapp));

        // defect 112137 begin - don't replace spaces with underscores
        // java.io.File tmpDir = new java.io.File(dir.toString().replace(' ',
        // '_'));
        java.io.File tmpDir = new java.io.File(dir.toString());
        // defect 112137 end

        if (!tmpDir.exists()) {
            // 117050 OS/400 support for servers running under two different
            // profile
            if (System.getProperty("os.name").equals("OS/400")) {
                int nodeIndex = tmpDir.toString().indexOf(getNodeName());
                nodeIndex = nodeIndex + getNodeName().length();
                String nodeDir = tmpDir.toString().substring(0, nodeIndex);
                java.io.File tempNodeDir = new java.io.File(nodeDir);
                if (!tempNodeDir.exists()) {
                    tempNodeDir.mkdirs();
                    String cmd = "/usr/bin/chown QEJBSVR " + nodeDir;
                    try {
                        Runtime runtime = Runtime.getRuntime();
                        Process process = runtime.exec(cmd);
                        process.waitFor();
                        if (process.exitValue() != 0) {
                            // pk435011
                            logger.logp(Level.SEVERE, CLASS_NAME, "getTempDirectory", "chown.failed", new Object[] { cmd,
                                    Integer.valueOf(process.exitValue()).toString() });
                        }
                    } catch (Exception e) {
                        com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(e, CLASS_NAME + ".getTempDirectory", "991", this);
                        // e.printStackTrace(); @283348D
                    }
                }
            }
            boolean success = tmpDir.mkdirs();
            if (success == false) {
                // pk435011
                logger.logp(Level.SEVERE, CLASS_NAME, "getTempDirectory", "failed.to.create.temp.directory", tmpDir.toString());
            }
        }

        if (tmpDir.canRead() == false || tmpDir.canWrite() == false) {
            if (override) {
                // pk435011
                logger.logp(Level.SEVERE, CLASS_NAME, "getTempDirectory", "unable.to.use.specified.temp.directory", new Object[] { tmpDir.toString(),
                        tmpDir.canRead(), tmpDir.canWrite() });
            } else {
                // pk435011
                logger.logp(Level.SEVERE, CLASS_NAME, "getTempDirectory", "unable.to.use.default.temp.directory", new Object[] { tmpDir.toString(),
                        tmpDir.canRead(), tmpDir.canWrite() });
            }
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getTempDirectory", "directory --> " + tmpDir.getAbsolutePath());
        }
        return tmpDir.getAbsolutePath();
    }

    /**
     * @param webapp 
     * @return
     */
    public String getServerName()
    {
      // TODO Auto-generated method stub
      return "SMF WebContainer";
    }

    /**
     * @return
     */
    private String getApplicationName(IServletContext webapp) {
        // TODO Auto-generated method stub
        return webapp.getWebAppConfig().getApplicationName();
    }

    /**
     * @param webapp 
     * @return
     */
    private String getNodeName() {
        
        return "default_node";
    }

    public String getTempDirChildren(IServletContext webapp) {
        StringBuilder dir = new StringBuilder();

        // SDJ D99077 - use Uri of web module in constructing temp dir, not the
        // web module's display name
        // defect 113620 - replace spaces with underscores starting with
        // servername
        dir.append(getNodeName()).append(java.io.File.separator).append(getServerName().replace(' ', '_')).append(java.io.File.separator).append(
                getApplicationName(webapp).replace(' ', '_')).append(java.io.File.separator).append(webapp.getWebAppConfig().getModuleName().replace(' ', '_'));

        return dir.toString();
    }
    
    protected JspClassloaderContext createJspClassloaderContext(IServletContext webapp, JspXmlExtConfig webAppConfig) {
        final ClassLoader loader;
        ClassLoader appLoader = webapp.getClassLoader();
        if (!osgiAppsCanProvideJSTL && (appLoader instanceof BundleReference)) {
            Bundle systemBundle = bundleContext.getBundle(org.osgi.framework.Constants.SYSTEM_BUNDLE_LOCATION);
            FrameworkWiring fw = systemBundle.adapt(FrameworkWiring.class);
            Collection<BundleCapability> caps = fw.findProviders(new RequirementImpl());
            if (!caps.isEmpty()) {
                BundleCapability bc = caps.iterator().next();
                Bundle b = bc.getRevision().getBundle();
                BundleWiring bw = b.adapt(BundleWiring.class);
                if (bw != null) {
                    ClassLoader cl = bw.getClassLoader();
                    loader = classLoadingService.unify(cl, appLoader);
                } else {
                    throw new IllegalStateException("jstl facade bundle is unresolved");
                }
            } else {
                throw new IllegalStateException("jstl facade bundle can not be located from its capability");
            }
        } else {
            loader = appLoader;
        }
        Container adaptableContainer = webapp.getModuleContainer();
        StringBuilder classPath=new StringBuilder();
        if (adaptableContainer!=null) {
            //retrieve it from the webapp
            Entry libDirEntry = adaptableContainer.getEntry("/WEB-INF/lib");
            Entry classPathEntry = adaptableContainer.getEntry("/WEB-INF/classes");
            if (classPathEntry!=null) {
            }
            //classPath.append(classPathEntry.getRealPath());
            if (libDirEntry!=null) { //it exists - must be a directory
                try {
                    Container libDirContainer = libDirEntry.adapt(Container.class);
                    /*Iterator<Entry> libEntries = libDirContainer.iterator();
                    while (libEntries.hasNext()) {
                        Entry libEntry = libEntries.next();
                        if (libEntry.getPath().endsWith(".jar")) {
                            //TODO: not sure what to do here ... not sure if we even need to generate the classPath
                        }
                    }*/
                    Collection<URL> libUrls = libDirContainer.getURLs();
                    for (URL libC:libUrls) {
                        if (libC.getPath()!=null && libC.getPath().endsWith(".jar")) {
                            //TODO: what about a directory ending in .jar?
                            //classPath.append(libC.getPath());
                        }

                    }
                } catch (UnableToAdaptException e) {
                    throw new IllegalStateException(e);
                }
            }
        } else {
            String base = webapp.getRealPath("/") + File.separatorChar;
            String webinf = base + "WEB-INF" + File.separatorChar;
            classPath.append(base).append(File.pathSeparator).append(webinf).append("classes");
            File libDir = new File(webinf + "lib");
            if (libDir.exists()) {
                String[] jarFiles = libDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".jar");
                    }
                });
                for (int i = 0; i < jarFiles.length; i++) {
                    classPath.append(File.pathSeparator).append(webinf).append("lib").append(File.separator).append(jarFiles[i]);
                }
            }
        }
        final String cp = classPath.toString();
        return new JspClassloaderContext() {
            public ClassLoader getClassLoader() { return loader;}
            public String getClassPath() { return cp;}
            public String getOptimizedClassPath() { return getClassPath();};
            public boolean isPredefineClassEnabled() {return false;}
            public byte[] predefineClass(String className, byte[] classData) {return classData;}
        };
    }
    
    private static class RequirementImpl implements Requirement {

        /* (non-Javadoc)
         * @see org.osgi.resource.Requirement#getNamespace()
         */
        @Override
        public String getNamespace() {
            return "com.ibm.ws.jsp.jstl.facade";
        }

        /* (non-Javadoc)
         * @see org.osgi.resource.Requirement#getDirectives()
         */
        @Override
        public Map<String, String> getDirectives() {
            return Collections.emptyMap();
        }

        /* (non-Javadoc)
         * @see org.osgi.resource.Requirement#getAttributes()
         */
        @Override
        public Map<String, Object> getAttributes() {
            return Collections.emptyMap();
        }

        /* (non-Javadoc)
         * @see org.osgi.resource.Requirement#getResource()
         */
        @Override
        public Resource getResource() {
            return null;
        }
        
        @Override
        public boolean equals(Object o) {
                if (o == this)
                        return true;
                if (!(o instanceof Requirement))
                        return false;
                Requirement c = (Requirement)o;
                return c.getNamespace().equals(getNamespace())
                                && c.getAttributes().isEmpty()
                                && c.getDirectives().isEmpty()
                                && c.getResource() == null;
        }
        
        @Override 
        public int hashCode() {
            return getNamespace().hashCode();
        }

    }

    protected ExtensionProcessor createProcessor(IServletContext webapp,
                                                 JspXmlExtConfig webAppConfig,
                                                 JspClassloaderContext jspClassloaderContext) throws Exception {
        JSPExtensionProcessor processor = new JSPExtensionProcessor(webapp, webAppConfig, globalTagLibraryCache, jspClassloaderContext);
        processor.startPreTouch(prepareJspHelperFactory);
        return processor;
    }

    public static synchronized GlobalTagLibraryCache getGlobalTagLibraryCache() {
        if (globalTagLibraryCache == null) {
            createGlobalTagLibraryCache();
        }
        return globalTagLibraryCache;
    }

    /**
     * DS method for setting (providing) a global tag lib config
     */
    @Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
    protected void setGlobalTagLibConfig(GlobalTagLibConfig globalTagLibConfig) {
            getGlobalTagLibraryCache().addGlobalTagLibConfig(globalTagLibConfig);
    }

    /**
     * DS method for removing a global tag lib config
     */
    protected void unsetGlobalTagLibConfig(GlobalTagLibConfig globalTagLibConfig) {
    }
        
    public static ElValidatorExtFactory getElValidatorExtFactory() {
        JSPExtensionFactory inst = instance.get();
        return inst == null? null: inst.elValidatorExtFactory;
    }

    public static GeneratorUtilsExtFactory getGeneratorUtilsExtFactory() {
        JSPExtensionFactory inst = instance.get();
        return inst == null? null: inst.generatorUtilsExtFactory;
    }
    
    public static JspVersionFactory getJspVersionFactory() {
        JSPExtensionFactory inst = instance.get();
        return inst == null? null: inst.jspVersionFactory;
    }    

    public String resolveString(String x) {
        return locationService.resolveString(x);
    }
}

