/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.compiler.utils;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;

import com.ibm.ws.artifact.url.WSJarURLConnection;
import com.ibm.wsspi.jsp.resource.translation.JspResources;

public class JspFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    // List to keep track of the JarFile objects that need to be closed after a compilation is done
    private List<JarFile> jarFilesOfWsjars = new ArrayList<JarFile>();
        
    private ClassLoader classLoader;
    private final WebModuleClassLoaderPackageFinder finder;
    Map<String, LinkedList<JspFileObject>> compiledTagFiles = new HashMap<String, LinkedList<JspFileObject>>();

    static final protected Logger logger = Logger.getLogger("com.ibm.ws.jsp");
    private static final String CLASS_NAME = "com.ibm.ws.jsp.translator.compiler.utils.JspFileManager";
    private boolean areTagFiles = false;

    public JspFileManager(JavaFileManager fileManager, ClassLoader classLoader) {
        super(fileManager);
        this.classLoader = classLoader;
        finder = new WebModuleClassLoaderPackageFinder();
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "JspFileManager", "Creating JspFileManager with classloader = " + classLoader);
        }
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, 
                                         Set<Kind> kinds, boolean recurse) throws IOException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "list", "Looking for classes in package = " + packageName + " from location = " + location.getName());
        }
        Iterable<JavaFileObject> results = new ArrayList<JavaFileObject>();
        
        if (location == StandardLocation.PLATFORM_CLASS_PATH) // let standard manager handle
            results = super.list(location, packageName, kinds, recurse);
        else if (location == StandardLocation.CLASS_PATH && kinds.contains(JavaFileObject.Kind.CLASS)) {
            results = super.list(location, packageName, kinds, recurse);
            /* 
             * If it is empty, try to look for the classes in the custom finder 
             * which contains application and Liberty specific classes.
             * results will be empty if are processing classes that are not part
             * of the java package.
             */
            if (!results.iterator().hasNext())
                results = finder.find(packageName, recurse);
        }
        return results;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof JspFileObject) {
            String binaryName = ((JspFileObject) file).getBinaryName();
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "inferBinaryName", "JspFileObject.getBinaryName()  = " + binaryName);
            }
            return binaryName;
        } else {
            return super.inferBinaryName(location, file);
        }
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className,
                                               Kind kind, FileObject sibling) throws IOException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getJavaFileForOutput", "className  = " + className + "sibling.getName() = " + sibling.getName());
        }

        /* 
         * If areTagFiles is true, we want to store the .class files
         * in the folder location that matches its fully qualified class name
         * relative to the outputDirectory.
         * Basically JspOptions.getOutputDir()/packageName/className.class 
         */
        if (areTagFiles) {
            return super.getJavaFileForOutput(location, className, kind, sibling);
        } else {
            /*
             * If we are here, it means that we are storing .class files relative
             * to the path of were the source code is located... therefore, the
             * class file needs to be placed in this folder and the package name
             * directory structure is not necessary.
             */
            String classNameNoPackage = className.substring(className.lastIndexOf('.')+1);
            return super.getJavaFileForOutput(location, classNameNoPackage, kind, sibling);
        }
        
    }

    public void addDependencies(JspResources[] dependencies){
        if (dependencies != null)
            for (JspResources resource : dependencies) {
                if (compiledTagFiles.containsKey(resource.getPackageName())) {
                    compiledTagFiles.get(resource.getPackageName()).add(new JspFileObject(resource, Kind.CLASS));
                } else {
                    LinkedList<JspFileObject> firstCompiledTagFileOfPackage = new LinkedList<JspFileObject>();
                    firstCompiledTagFileOfPackage.add(new JspFileObject(resource, Kind.CLASS));
                    compiledTagFiles.put(resource.getPackageName(), firstCompiledTagFileOfPackage);
                }
            }
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        return classLoader;
    }

    public void setAreTagFiles(boolean areTagFiles) {
        this.areTagFiles  = areTagFiles;
    }

    @Override
    public void close() throws IOException {
        for (JarFile jarFile : jarFilesOfWsjars) {
            jarFile.close();
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "close", "closing jar [" + jarFile.getName() + "]");
            }
        }
        super.close();
    }

    private class WebModuleClassLoaderPackageFinder {

        private List<BundleWiring> bundleWiringOfActiveBundles = new ArrayList<BundleWiring>();

        /**
         * This method helps getting JspFileObjects from dependencies (compiled tag files), Bundles, folders, and/or jar files. 
         * @param packageName Package name of the package to find.
         * @param recursive
         * @return
         * @throws IOException
         */
        public List<JavaFileObject> find(String packageName, boolean recursive) throws IOException {
            String javaPackageName = packageName.replace('.', '/');
            
            List<JavaFileObject> result = new ArrayList<JavaFileObject>();
            
            Collection<URL> urlCollection = getClassLoaderResources(javaPackageName);

            /*
             * If we don't find anything in the classloader, we check if we find
             * something in our list of dependencies. Our dependencies should be
             * tag files (as java classes).
             */
            if (urlCollection.isEmpty()) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME + "$WebModuleClassLoaderPackageFinder", "find", "Nothing was found in the classloader after recovery. Checking if we find any JspFileObject from our dependencies list for packageName = " + packageName);
                }
                
                if (compiledTagFiles.containsKey(packageName)) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME + "$WebModuleClassLoaderPackageFinder", "find", "Dependencies were found for packageName = " + packageName);
                    }
                    result.addAll(compiledTagFiles.get(packageName));
                }
            }

            Iterator<URL> urlIterator = urlCollection.iterator();
            while (urlIterator.hasNext()) {
                result.addAll(listUnder(packageName, urlIterator.next(), recursive));
            }
            return result;
        }
        
        /**
         * Equivalent to classLoader.getResources(String javaPackageName) but
         * making sure the returned URLs are not repeated. classLoader.getResources
         * can return a list with the resources repeated; we avoid this with this
         * method.
         * @param javaPackageName
         * @return A Collection<URL> without duplicated URL representing the returned value of classLoader.getResources(String javaPackageName)
         * @throws IOException
         */
        private Collection<URL> getClassLoaderResources(String javaPackageName) throws IOException {
            
            /*
             * Appending a trailing slash as method getResources behaves inconsistently without it...
             * Without a trailing slash, sometimes, the Enumeration is empty when it should not.
             */
            Enumeration<URL> urlEnumeration = classLoader.getResources(javaPackageName + '/');
            
            /* classloader.getResources is returning each location twice for some odd reason
             * Using LinkedHashMap as the order needs to be preserved; i.e. compilation
             * with special classloading policies need this ordering (parent_last and parent_first).
             */
            Map<String, URL> hashMap = new LinkedHashMap<String, URL>();
            
            /*
             * Making sure we don't have repeated URLs...
             */
            while (urlEnumeration.hasMoreElements()) { 
                // one URL for each jar or bundle on the classpath that has the given package
                URL packageFolderURL = urlEnumeration.nextElement();
                hashMap.put(packageFolderURL.getFile(), packageFolderURL);
            }
            
            return hashMap.values();
        }

        /**
         * Depending on the location of the packageName, this method help determine if the resource if in a Jar, Bundle, or folder.
         * Based in the location, it will return a collection of the JspFileObjects that match the package name.
         * @param packageName
         * @param packageFolderURL
         * @param recursive
         * @return
         */
        private Collection<JavaFileObject> listUnder(String packageName, URL packageFolderURL, boolean recursive) {
            
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME + "$WebModuleClassLoaderPackageFinder", "listUnder", "Looking for packageName = " + packageName + "in URL = " + packageFolderURL);
            }
            
            final File directory = new File(packageFolderURL.getFile());
            
            /*
             * We only care about directories, bundles and jars. Files are not in the equation
             * as we are working we packages... and not classes directly.
             */
            boolean isDirectory = false;
            if (System.getSecurityManager() != null) {
                isDirectory = java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<Boolean>() {
                        public Boolean run() {
                            return directory.isDirectory();
                        }
                    });
            } else {
                isDirectory = directory.isDirectory();
            }
            
            if (isDirectory) { 
                // useful for when there are references to classes in WEB-INF/classes
                return processDir(packageName, directory, recursive);
            } else if (packageFolderURL.getProtocol().equals("bundleresource") || packageFolderURL.getProtocol().equals("jarentry")) {
                // meaning that this URL is a bundle
                return processBundle(packageName, recursive);
            } else { 
                // browse a jar file
                return processJar(packageFolderURL, recursive);
            }
        }

        /**
         * If a package name has its classes in a OSGi Bundle, this method creates one JspFileObject per class in the package.
         * @param packageFolderURL
         * @param recursive
         * @return
         */
        private Collection<JavaFileObject> processBundle(String packageName, boolean recursive) {
            List<JavaFileObject> result = new ArrayList<JavaFileObject>();
            // Getting all the BundleWiring objects this way to avoid getting the active bundles repeated times during one compilation
            for (final BundleWiring bundleWiring : getBundleWiringOfActiveBundles()) {
                Bundle bundle = bundleWiring.getBundle();
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME + "$WebModuleClassLoaderPackageFinder", "processBundle", "Creating JspFileObjects of the classes in bundle = " + bundle.getSymbolicName());
                }

                Collection<String> classesInBundle = bundleWiring.listResources(packageName.replace('.', '/'), "*" + Kind.CLASS.extension, BundleWiring.LISTRESOURCES_LOCAL);
                for (String location : classesInBundle) {
                    String binaryName = location.replace('/', '.').replaceAll(Kind.CLASS.extension + "$", "");
                    URI resourceURI = null;
                    try {
                        resourceURI = bundle.getResource(location).toURI(); //getResource should never be null here (we know the location exists, we just need to get the appropriate URI of the .class)
                        String protocol = resourceURI.toURL().getProtocol();
                        result.add(new JspFileObject(binaryName, resourceURI, protocol));
                    } catch (MalformedURLException e) {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                            logger.logp(Level.FINE, CLASS_NAME + "$WebModuleClassLoaderPackageFinder", "processBundle", "There was a problem getting the URL of the URI representing binaryName = " + binaryName, e);
                        }
                        //Recovering; it has to be a bundleresource
                        result.add(new JspFileObject(binaryName, resourceURI, "bundleresource"));
                    } catch (URISyntaxException e) {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                            logger.logp(Level.FINE, CLASS_NAME + "$WebModuleClassLoaderPackageFinder", "processBundle", "There was a problem getting  the URI representing binaryName = " + binaryName, e);
                        }
                    }
                }
            }
            return result;
        }

        /**
         * Get the the BundleWiring object of each active bundle in Liberty.
         */
        private List<BundleWiring> getBundleWiringOfActiveBundles() {
            if (bundleWiringOfActiveBundles.isEmpty()) {
                BundleContext jspBundleContext = null;
                if (System.getSecurityManager() != null) {
                    jspBundleContext = java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedAction<BundleContext>() {
                            public BundleContext run() {
                                return FrameworkUtil.getBundle(this.getClass()).getBundleContext();
                            }
                        });
                } else {
                    jspBundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
                }
                
                /*
                 * Get all the active bundles to create the corresponding JspFileObject objects
                 * with the classes needed to compile JSPs.
                 */
                Bundle[] activeBundles = jspBundleContext.getBundles();
                
                for(final Bundle bundle : activeBundles){
                    BundleWiring bundleWiring = null;
                    if(System.getSecurityManager() != null) {
                        bundleWiring = java.security.AccessController.doPrivileged(
                            new java.security.PrivilegedAction<BundleWiring>() {
                                public BundleWiring run() {
                                    return bundle.adapt(BundleWiring.class);
                                }
                            });
                    } else {
                        bundleWiring = bundle.adapt(BundleWiring.class);
                    }
                    bundleWiringOfActiveBundles.add(bundleWiring);
                }
            }
            return bundleWiringOfActiveBundles;
        }

        /**
         * If a package name has its classes in a Jar, this method creates one JspFileObject per class in the package.
         * This method can process jar and wsjar protocols. Wsjar is a jar in essence. See {@link WSJarURLConnection}
         * @param packageFolderURL
         * @param recursive
         * @return
         */
        private List<JavaFileObject> processJar(URL packageFolderURL, boolean recursive) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME + "$WebModuleClassLoaderPackageFinder", "processJar", "Processing jar = " + packageFolderURL + " recursive = " + recursive);
            }
            List<JavaFileObject> result = new ArrayList<JavaFileObject>();
            
            // Since we are always looking for packages (folders) the second element of the array exists
            String packageName = packageFolderURL.getPath().split("!/")[1];
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME + "$WebModuleClassLoaderPackageFinder", "processJar", "packageName = " + packageName);
            }
            
            JarFile jarFile = null;
            try {
                URLConnection urlConnection = packageFolderURL.openConnection();
                Enumeration<JarEntry> entryEnum = null;
                URI jarFileURI = null;
                String protocol = packageFolderURL.getProtocol();
                if (protocol.equals("wsjar")) {
                    WSJarURLConnection wsJarUrlConnection = (WSJarURLConnection) urlConnection;
                    final File fileLocationOfJar = wsJarUrlConnection.getFile();
                    if(System.getSecurityManager() != null) {
                        jarFile = java.security.AccessController.doPrivileged(
                            new java.security.PrivilegedAction<JarFile>() {
                                public JarFile run() {
                                    try {
                                        return new JarFile(fileLocationOfJar);
                                    } catch (IOException e) {
                                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                                            logger.logp(Level.FINE, CLASS_NAME + "$WebModuleClassLoaderPackageFinder", "processJar", "IOException in doPriviledged processing jar.", e);
                                        }
                                    }
                                    return null;
                                }
                            });
                    } else {
                        jarFile = new JarFile(fileLocationOfJar);
                    }
                    jarFileURI = fileLocationOfJar.toURI();
                    if (jarFile != null)
                        entryEnum = jarFile.entries();
                    else //if null, return an empty enumeration
                        entryEnum = new Vector<JarEntry>().elements();
                } else {
                    JarURLConnection jarConn = (JarURLConnection) urlConnection;
                    jarFileURI = jarConn.getJarFileURL().toURI();
                    jarFile = jarConn.getJarFile();
                    entryEnum = jarFile.entries();
                }

                Pattern classesInPackage = Pattern.compile(packageName + "[^/]*" + Kind.CLASS.extension);
                Matcher m = classesInPackage.matcher("");
                while (entryEnum.hasMoreElements()) {
                    JarEntry jarEntry = entryEnum.nextElement();
                    String fileName = jarEntry.getName();
                    // We only care about .class files within the jar
                    // using regex or something to make sure we have packageName + / + className.class
                    if (m.reset(fileName).matches()) {
                        String binaryName = fileName.replace('/', '.').replaceAll(Kind.CLASS.extension + "$", "");
                        JspFileObject jspFileObject = new JspFileObject(binaryName, jarFileURI, protocol);
                        result.add(jspFileObject);
                        if (protocol.equals("wsjar"))
                            jarFilesOfWsjars.add(jspFileObject.getJarFile());
                    }
                }
            }
            catch (IOException e) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME + "$WebModuleClassLoaderPackageFinder", "processJar", "IOException processing jar.", e);
                }
            } catch (URISyntaxException e) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME + "$WebModuleClassLoaderPackageFinder", "processJar", "Unable to get URI of jar file.", e);
                }
            } finally {
                try {
                    if (jarFile != null) jarFile.close();
                } catch (IOException e) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME + "$WebModuleClassLoaderPackageFinder", "processJar", "Unable to close jarFile.", e);
                    }
                }
            }
            return result;
        }

        /**
         * If a package name has its classes in a folder, this method creates one JspFileObject per class in the package.
         * @param packageName
         * @param directory
         * @param recursive
         * @return
         */
        private List<JavaFileObject> processDir(String packageName, final File directory, boolean recursive) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME + "$WebModuleClassLoaderPackageFinder", "processDir", "Processing directory = " + directory + " looking for classes in packageName = " + packageName);
            }
            List<JavaFileObject> result = new ArrayList<JavaFileObject>();
            File[] childFiles = directory.listFiles();
            
            if(System.getSecurityManager() != null) {
                childFiles = java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<File[]>() {
                        public File[] run() {
                            return directory.listFiles();
                        }
                    });
            } else {
                childFiles = directory.listFiles();
            }

            if(childFiles != null) // If we are here, childFiles shouldn't be null. Sanity check.
                for (final File childFile : childFiles) {
                    boolean isFile = false;
                    if (System.getSecurityManager() != null) {
                        isFile = java.security.AccessController.doPrivileged(
                            new java.security.PrivilegedAction<Boolean>() {
                                public Boolean run() {
                                    return childFile.isFile();
                                }
                            });
                    } else {
                        isFile = childFile.isFile();
                    }
                    
                    if (isFile && childFile.getName().endsWith(Kind.CLASS.extension)) {
                        String binaryName = packageName + "." + childFile.getName().replaceAll(Kind.CLASS.extension + "$", "");
                        String protocol = "file";
                        try {
                            // Should be file, but I want to avoid hardcoding the string
                            protocol = childFile.toURI().toURL().getProtocol();
                        } catch (MalformedURLException e) {
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                                logger.logp(Level.FINE, CLASS_NAME + "$WebModuleClassLoaderPackageFinder", "processDir", "There was a problem getting the URL protocol; using file as the protocol ", e);
                            }
                        }
                        result.add(new JspFileObject(binaryName, childFile.toURI(), protocol));
                    } else if (recursive && childFile.isDirectory()) {
                        result.addAll(processDir(packageName + "." + childFile.getName(), childFile, recursive));
                    }
                }
            return result;
        }
    }
}
