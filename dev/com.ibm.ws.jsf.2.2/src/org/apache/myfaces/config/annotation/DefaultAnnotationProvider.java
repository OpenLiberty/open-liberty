/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.config.annotation;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.bean.ManagedBean;
import javax.faces.component.FacesComponent;
import javax.faces.component.behavior.FacesBehavior;
import javax.faces.context.ExternalContext;
import javax.faces.convert.FacesConverter;
import javax.faces.event.NamedEvent;
import javax.faces.render.FacesBehaviorRenderer;
import javax.faces.render.FacesRenderer;
import javax.faces.validator.FacesValidator;
import javax.faces.view.facelets.FaceletsResourceResolver;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.spi.AnnotationProvider;
import org.apache.myfaces.spi.AnnotationProviderFactory;
import org.apache.myfaces.util.ContainerUtils;
import org.apache.myfaces.config.util.GAEUtils;
import org.apache.myfaces.config.util.JarUtils;
import org.apache.myfaces.shared.util.StringUtils;
import org.apache.myfaces.view.facelets.util.Classpath;

/**
 * 
 * @since 2.0.2
 * @author Leonardo Uribe
 */
public class DefaultAnnotationProvider extends AnnotationProvider
{
    private static final Logger log = Logger.getLogger(DefaultAnnotationProvider.class.getName());
    
    /**
     * Servlet context init parameter which defines which packages to scan
     * for beans, separated by commas.
     */
    @JSFWebConfigParam(since="2.0")
    public static final String SCAN_PACKAGES = "org.apache.myfaces.annotation.SCAN_PACKAGES";

    /**
     * <p>Prefix path used to locate web application classes for this
     * web application.</p>
     */
    private static final String WEB_CLASSES_PREFIX = "/WEB-INF/classes/";
    
    /**
     * <p>Prefix path used to locate web application libraries for this
     * web application.</p>
     */
    private static final String WEB_LIB_PREFIX = "/WEB-INF/lib/";
    
    private static final String META_INF_PREFIX = "META-INF/";

    private static final String FACES_CONFIG_SUFFIX = ".faces-config.xml";

    /**
     * <p>Resource path used to acquire implicit resources buried
     * inside application JARs.</p>
     */
    private static final String FACES_CONFIG_IMPLICIT = "META-INF/faces-config.xml";
    
    private final _ClassByteCodeAnnotationFilter _filter;

    /**
     * This set contains the annotation names that this AnnotationConfigurator is able to scan
     * in the format that is read from .class file.
     */
    private static Set<String> byteCodeAnnotationsNames;

    static
    {
        Set<String> bcan = new HashSet<String>(10, 1f);
        bcan.add("Ljavax/faces/component/FacesComponent;");
        bcan.add("Ljavax/faces/component/behavior/FacesBehavior;");
        bcan.add("Ljavax/faces/convert/FacesConverter;");
        bcan.add("Ljavax/faces/validator/FacesValidator;");
        bcan.add("Ljavax/faces/render/FacesRenderer;");
        bcan.add("Ljavax/faces/bean/ManagedBean;");
        bcan.add("Ljavax/faces/event/NamedEvent;");
        //bcan.add("Ljavax/faces/event/ListenerFor;");
        //bcan.add("Ljavax/faces/event/ListenersFor;");
        bcan.add("Ljavax/faces/render/FacesBehaviorRenderer;");
        bcan.add("Ljavax/faces/view/facelets/FaceletsResourceResolver;");

        byteCodeAnnotationsNames = Collections.unmodifiableSet(bcan);
    }
    
    private static final Set<Class<? extends Annotation>> JSF_ANNOTATION_CLASSES;
    
    static
    {
        Set<Class<? extends Annotation>> bcan = new HashSet<Class<? extends Annotation>>(10, 1f);
        bcan.add(FacesComponent.class);
        bcan.add(FacesBehavior.class);
        bcan.add(FacesConverter.class);
        bcan.add(FacesValidator.class);
        bcan.add(FacesRenderer.class);
        bcan.add(ManagedBean.class);
        bcan.add(NamedEvent.class);
        bcan.add(FacesBehaviorRenderer.class);
        bcan.add(FaceletsResourceResolver.class);
        JSF_ANNOTATION_CLASSES = Collections.unmodifiableSet(bcan);
    }
    
    public DefaultAnnotationProvider()
    {
        super();
        _filter = new _ClassByteCodeAnnotationFilter();
    }
    
    @Override
    public Map<Class<? extends Annotation>, Set<Class<?>>> getAnnotatedClasses(ExternalContext ctx)
    {
        Map<Class<? extends Annotation>,Set<Class<?>>> map = new HashMap<Class<? extends Annotation>, Set<Class<?>>>();
        Collection<Class<?>> classes = null;

        //1. Scan for annotations on /WEB-INF/classes
        try
        {
            classes = getAnnotatedWebInfClasses(ctx);
        }
        catch (IOException e)
        {
            throw new FacesException(e);
        }

        for (Class<?> clazz : classes)
        {
            processClass(map, clazz);
        }
        
        //2. Scan for annotations on classpath
        String jarAnnotationFilesToScanParam = MyfacesConfig.getCurrentInstance(ctx).getGaeJsfAnnotationsJarFiles();
        jarAnnotationFilesToScanParam = jarAnnotationFilesToScanParam != null ? 
                jarAnnotationFilesToScanParam.trim() : null;
        if (ContainerUtils.isRunningOnGoogleAppEngine(ctx) && 
            jarAnnotationFilesToScanParam != null &&
            jarAnnotationFilesToScanParam.length() > 0)
        {
            // Skip call AnnotationProvider.getBaseUrls(ctx), and instead use the value of the config parameter
            // to find which classes needs to be scanned for annotations
            classes = getGAEAnnotatedMetaInfClasses(ctx, jarAnnotationFilesToScanParam);
        }
        else
        {
            try
            {
                AnnotationProvider provider
                        = AnnotationProviderFactory.getAnnotationProviderFactory(ctx).getAnnotationProvider(ctx);
                classes = getAnnotatedMetaInfClasses(ctx, provider.getBaseUrls(ctx));
            }
            catch (IOException e)
            {
                throw new FacesException(e);
            }
        }
        
        for (Class<?> clazz : classes)
        {
            processClass(map, clazz);
        }
        
        //3. Scan on myfaces-impl for annotations available on myfaces-impl.
        //Also scan jar including META-INF/standard-faces-config.xml
        //(myfaces-impl jar file)
        // -= Leonardo Uribe =- No annotations in MyFaces jars, code not
        // necessary, because all config is already in standard-faces-config.xml
        //URL url = getClassLoader().getResource(STANDARD_FACES_CONFIG_RESOURCE);
        //if (url == null)
        //{
        //    url = getClass().getClassLoader().getResource(STANDARD_FACES_CONFIG_RESOURCE);
        //}
        //classes = getAnnotatedMyfacesImplClasses(ctx, url);
        //for (Class<?> clazz : classes)
        //{
        //    processClass(map, clazz);
        //}
        
        return map;
    }
    
    @Override
    public Set<URL> getBaseUrls() throws IOException
    {
        Set<URL> urlSet = new HashSet<URL>();
        
        //This usually happens when maven-jetty-plugin is used
        //Scan jars looking for paths including META-INF/faces-config.xml
        Enumeration<URL> resources = getClassLoader().getResources(FACES_CONFIG_IMPLICIT);
        while (resources.hasMoreElements())
        {
            urlSet.add(resources.nextElement());
        }

        //Scan files inside META-INF ending with .faces-config.xml
        URL[] urls = Classpath.search(getClassLoader(), META_INF_PREFIX, FACES_CONFIG_SUFFIX);
        for (int i = 0; i < urls.length; i++)
        {
            urlSet.add(urls[i]);
        }
        
        return urlSet;
    }
    
    @Override
    public Set<URL> getBaseUrls(ExternalContext context) throws IOException
    {
        String jarFilesToScanParam = MyfacesConfig.getCurrentInstance(context).getGaeJsfJarFiles();
        jarFilesToScanParam = jarFilesToScanParam != null ? jarFilesToScanParam.trim() : null;
        if (ContainerUtils.isRunningOnGoogleAppEngine(context) && 
            jarFilesToScanParam != null &&
            jarFilesToScanParam.length() > 0)
        {
            Set<URL> urlSet = new HashSet<URL>();
            
            //This usually happens when maven-jetty-plugin is used
            //Scan jars looking for paths including META-INF/faces-config.xml
            Enumeration<URL> resources = getClassLoader().getResources(FACES_CONFIG_IMPLICIT);
            while (resources.hasMoreElements())
            {
                urlSet.add(resources.nextElement());
            }
            
            Collection<URL> urlsGAE = GAEUtils.searchInWebLib(
                    context, getClassLoader(), jarFilesToScanParam, META_INF_PREFIX, FACES_CONFIG_SUFFIX);
            if (urlsGAE != null)
            {
                urlSet.addAll(urlsGAE);
            }
            return urlSet;
        }
        else
        {
            return getBaseUrls();
        }
    }

    protected Collection<Class<?>> getAnnotatedMetaInfClasses(ExternalContext ctx, Set<URL> urls)
    {
        if (urls != null && !urls.isEmpty())
        {
            List<Class<?>> list = new ArrayList<Class<?>>();
            for (URL url : urls)
            {
                try
                {
                    JarFile jarFile = getJarFile(url);
                    if (jarFile != null)
                    {
                        archiveClasses(jarFile, list);
                    }
                }
                catch(IOException e)
                {
                    log.log(Level.SEVERE, "cannot scan jar file for annotations:"+url, e);
                }
            }
            return list;
        }
        return Collections.emptyList();
    }
    
    protected Collection<Class<?>> getGAEAnnotatedMetaInfClasses(ExternalContext context, String filter)
    {
        if (!filter.equals("none"))
        {
            String[] jarFilesToScan = StringUtils.trim(StringUtils.splitLongString(filter, ','));
            Set<String> paths = context.getResourcePaths(WEB_LIB_PREFIX);
            if (paths != null)
            {
                List<Class<?>> list = new ArrayList<Class<?>>();
                for (Object pathObject : paths)
                {
                    String path = (String) pathObject;
                    if (path.endsWith(".jar") && GAEUtils.wildcardMatch(path, jarFilesToScan, GAEUtils.WEB_LIB_PREFIX))
                    {
                        // GAE does not use WAR format, so the app is just uncompressed in a directory
                        // What we need here is just take the path of the file, and open the file as a
                        // jar file. Then, if the jar should be scanned, try to find the required file.
                        try
                        {
                            URL jarUrl = new URL("jar:" + context.getResource(path).toExternalForm() + "!/"); 
                            JarFile jarFile = JarUtils.getJarFile(jarUrl);
                            if (jarFile != null)
                            {
                                archiveClasses(jarFile, list);
                            }
                        }
                        catch(IOException e)
                        {
                            log.log(Level.SEVERE, 
                                    "IOException when reading jar file for annotations using filter: "+filter, e);
                        }
                    }
                }
                return list;
            }
        }
        return Collections.emptyList();
    }

    protected Collection<Class<?>> getAnnotatedMyfacesImplClasses(ExternalContext ctx, URL url)
    {
        return Collections.emptyList();
        /*
        try
        {
            List<Class<?>> list = new ArrayList<Class<?>>();
            JarFile jarFile = getJarFile(url);
            if (jarFile == null)
            {
                return list;
            }
            else
            {
                return archiveClasses(ctx, jarFile, list);
            }
        }
        catch(IOException e)
        {
            throw new FacesException("cannot scan jar file for annotations:"+url, e);
        }*/
    }

    protected Collection<Class<?>> getAnnotatedWebInfClasses(ExternalContext ctx) throws IOException
    {
        String scanPackages = ctx.getInitParameter(SCAN_PACKAGES);
        if (scanPackages != null)
        {
            try
            {
                return packageClasses(ctx, scanPackages);
            }
            catch (ClassNotFoundException e)
            {
                throw new FacesException(e);
            }
            catch (IOException e)
            {
                throw new FacesException(e);
            }
        }
        else
        {
            return webClasses(ctx);
        }
    }
    
    /**
     * <p>Return a list of the classes defined within the given packages
     * If there are no such classes, a zero-length list will be returned.</p>
     *
     * @param scanPackages the package configuration
     *
     * @exception ClassNotFoundException if a located class cannot be loaded
     * @exception IOException if an input/output error occurs
     */
    private List<Class<?>> packageClasses(final ExternalContext externalContext,
            final String scanPackages) throws ClassNotFoundException, IOException
    {

        List<Class<?>> list = new ArrayList<Class<?>>();

        String[] scanPackageTokens = scanPackages.split(",");
        for (String scanPackageToken : scanPackageTokens)
        {
            if (scanPackageToken.toLowerCase().endsWith(".jar"))
            {
                URL jarResource = externalContext.getResource(WEB_LIB_PREFIX
                        + scanPackageToken);
                String jarURLString = "jar:" + jarResource.toString() + "!/";
                URL url = new URL(jarURLString);
                JarFile jarFile = ((JarURLConnection) url.openConnection())
                        .getJarFile();

                archiveClasses(jarFile, list);
            }
            else
            {
                List<Class> list2 = new ArrayList<Class>();
                _PackageInfo.getInstance().getClasses(list2, scanPackageToken);
                for (Class c : list2)
                {
                    list.add(c);                    
                }
            }
        }
        return list;
    }    
    
    /**
     * <p>Return a list of classes to examine from the specified JAR archive.
     * If this archive has no classes in it, a zero-length list is returned.</p>
     *
     * @param context <code>ExternalContext</code> instance for
     *  this application
     * @param jar <code>JarFile</code> for the archive to be scanned
     *
     * @exception ClassNotFoundException if a located class cannot be loaded
     */
    private List<Class<?>> archiveClasses(JarFile jar, List<Class<?>> list)
    {
        // Accumulate and return a list of classes in this JAR file
        ClassLoader loader = ClassUtils.getContextClassLoader();
        if (loader == null)
        {
            loader = this.getClass().getClassLoader();
        }
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements())
        {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory())
            {
                continue; // This is a directory
            }
            String name = entry.getName();
            if (name.startsWith("META-INF/"))
            {
                continue; // Attribute files
            }
            if (!name.endsWith(".class"))
            {
                continue; // This is not a class
            }

            DataInputStream in = null;
            boolean couldContainAnnotation = false;
            try
            {
                in = new DataInputStream(jar.getInputStream(entry));
                couldContainAnnotation = _filter
                        .couldContainAnnotationsOnClassDef(in,
                                byteCodeAnnotationsNames);
            }
            catch (IOException e)
            {
                // Include this class - we can't scan this class using
                // the filter, but it could be valid, so we need to
                // load it using the classLoader. Anyway, log a debug
                // message.
                couldContainAnnotation = true;
                if (log.isLoggable(Level.FINE))
                {
                    log.fine("IOException when filtering class " + name
                            + " for annotations");
                }
            }
            finally
            {
                if (in != null)
                {
                    try
                    {
                        in.close();
                    }
                    catch (IOException e)
                    {
                        // No Op
                    }
                }
            }

            if (couldContainAnnotation)
            {
                name = name.substring(0, name.length() - 6); // Trim ".class"
                Class<?> clazz = null;
                try
                {
                    clazz = loader.loadClass(name.replace('/', '.'));
                }
                catch (NoClassDefFoundError e)
                {
                    // Skip this class - we cannot analyze classes we cannot load
                }
                catch (Exception e)
                {
                    // Skip this class - we cannot analyze classes we cannot load
                }
                if (clazz != null)
                {
                    list.add(clazz);
                }
            }
        }
        return list;

    }
    
    /**
     * <p>Return a list of the classes defined under the
     * <code>/WEB-INF/classes</code> directory of this web
     * application.  If there are no such classes, a zero-length list
     * will be returned.</p>
     *
     * @param externalContext <code>ExternalContext</code> instance for
     *  this application
     *
     * @exception ClassNotFoundException if a located class cannot be loaded
     */
    private List<Class<?>> webClasses(ExternalContext externalContext)
    {
        List<Class<?>> list = new ArrayList<Class<?>>();
        webClasses(externalContext, WEB_CLASSES_PREFIX, list);
        return list;
    }

    /**
     * <p>Add classes found in the specified directory to the specified
     * list, recursively calling this method when a directory is encountered.</p>
     *
     * @param externalContext <code>ExternalContext</code> instance for
     *  this application
     * @param prefix Prefix specifying the "directory path" to be searched
     * @param list List to be appended to
     *
     * @exception ClassNotFoundException if a located class cannot be loaded
     */
    private void webClasses(ExternalContext externalContext, String prefix,
            List<Class<?>> list)
    {

        ClassLoader loader = getClassLoader();

        Set<String> paths = externalContext.getResourcePaths(prefix);
        if(paths == null)
        {
            return; //need this in case there is no WEB-INF/classes directory
        }
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("webClasses(" + prefix + ") - Received " + paths.size()
                    + " paths to check");
        }

        String path = null;

        if (paths.isEmpty())
        {
            if (log.isLoggable(Level.WARNING))
            {
                log
                        .warning("AnnotationConfigurator does not found classes "
                                + "for annotations in "
                                + prefix
                                + " ."
                                + " This could happen because maven jetty plugin is used"
                                + " (goal jetty:run). Try configure "
                                + SCAN_PACKAGES + " init parameter "
                                + "or use jetty:run-exploded instead.");
            }
        }
        else
        {
            for (Object pathObject : paths)
            {
                path = (String) pathObject;
                if (path.endsWith("/"))
                {
                    webClasses(externalContext, path, list);
                }
                else if (path.endsWith(".class"))
                {
                    DataInputStream in = null;
                    boolean couldContainAnnotation = false;
                    try
                    {
                        in = new DataInputStream(externalContext
                                .getResourceAsStream(path));
                        couldContainAnnotation = _filter
                                .couldContainAnnotationsOnClassDef(in,
                                        byteCodeAnnotationsNames);
                    }
                    catch (IOException e)
                    {
                        // Include this class - we can't scan this class using
                        // the filter, but it could be valid, so we need to
                        // load it using the classLoader. Anyway, log a debug
                        // message.
                        couldContainAnnotation = true;
                        if (log.isLoggable(Level.FINE))
                        {
                            log.fine("IOException when filtering class " + path
                                    + " for annotations");
                        }
                    }
                    finally
                    {
                        if (in != null)
                        {
                            try
                            {
                                in.close();
                            }
                            catch (IOException e)
                            {
                                // No Op
                            }
                        }
                    }

                    if (couldContainAnnotation)
                    {
                        //Load it and add it to list for later processing
                        path = path.substring(WEB_CLASSES_PREFIX.length()); // Strip prefix
                        path = path.substring(0, path.length() - 6); // Strip suffix
                        path = path.replace('/', '.'); // Convert to FQCN

                        Class<?> clazz = null;
                        try
                        {
                            clazz = loader.loadClass(path);
                        }
                        catch (NoClassDefFoundError e)
                        {
                            // Skip this class - we cannot analyze classes we cannot load
                        }
                        catch (Exception e)
                        {
                            // Skip this class - we cannot analyze classes we cannot load
                        }
                        if (clazz != null)
                        {
                            list.add(clazz);
                        }
                    }
                }
            }
        }
    }
    
    private JarFile getJarFile(URL url) throws IOException
    {
        URLConnection conn = url.openConnection();
        conn.setUseCaches(false);
        conn.setDefaultUseCaches(false);

        JarFile jarFile;
        if (conn instanceof JarURLConnection)
        {
            jarFile = ((JarURLConnection) conn).getJarFile();
        }
        else
        {
            jarFile = _getAlternativeJarFile(url);
        }
        return jarFile;
    }
    

    /**
     * taken from org.apache.myfaces.view.facelets.util.Classpath
     * 
     * For URLs to JARs that do not use JarURLConnection - allowed by the servlet spec - attempt to produce a JarFile
     * object all the same. Known servlet engines that function like this include Weblogic and OC4J. This is not a full
     * solution, since an unpacked WAR or EAR will not have JAR "files" as such.
     */
    private static JarFile _getAlternativeJarFile(URL url) throws IOException
    {
        String urlFile = url.getFile();

        // Trim off any suffix - which is prefixed by "!/" on Weblogic
        int separatorIndex = urlFile.indexOf("!/");

        // OK, didn't find that. Try the less safe "!", used on OC4J
        if (separatorIndex == -1)
        {
            separatorIndex = urlFile.indexOf('!');
        }

        if (separatorIndex != -1)
        {
            String jarFileUrl = urlFile.substring(0, separatorIndex);
            // And trim off any "file:" prefix.
            if (jarFileUrl.startsWith("file:"))
            {
                jarFileUrl = jarFileUrl.substring("file:".length());
            }

            return new JarFile(jarFileUrl);
        }

        return null;
    }
        
    private ClassLoader getClassLoader()
    {
        ClassLoader loader = ClassUtils.getContextClassLoader();
        if (loader == null)
        {
            loader = this.getClass().getClassLoader();
        }
        return loader;
    }
    
    private void processClass(Map<Class<? extends Annotation>,Set<Class<?>>> map, Class<?> clazz)
    {
        Annotation[] annotations = clazz.getAnnotations();
        for (Annotation anno : annotations)
        {
            Class<? extends Annotation> annotationClass = anno.annotationType();
            if (JSF_ANNOTATION_CLASSES.contains(annotationClass))
            {
                Set<Class<?>> set = map.get(annotationClass);
                if (set == null)
                {
                    set = new HashSet<Class<?>>();
                    set.add(clazz);
                    map.put(annotationClass, set);
                }
                else
                {
                    set.add(clazz);
                }

            }
        }
    }
}
