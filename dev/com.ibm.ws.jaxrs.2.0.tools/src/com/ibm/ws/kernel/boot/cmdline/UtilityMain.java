/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.cmdline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.ibm.ws.kernel.provisioning.AbstractResourceRepository;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.NameBasedLocalBundleRepository;
import com.ibm.ws.kernel.provisioning.VersionUtility;

/**
 * Utility Main: evaluates the manifest header of the utility jar, and constructs
 * an apprpp
 */
public class UtilityMain {
    private static final String DEFAULT_BUNDLE_VERSION = "0.0.0";
    private static File installDir = null;
    public static void main(String[] args) {
        try {
            internal_main(args);
        } catch (InvocationTargetException e) {
            Throwable ite = e.getTargetException();
            if (ite != null) {
                ite.printStackTrace();
            } else {
                e.printStackTrace();
            }
            System.exit(ExitCode.ERROR_UNKNOWN_EXCEPTION_CMD);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(ExitCode.ERROR_UNKNOWN_EXCEPTION_CMD);
        }
    }

    /**
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws PrivilegedActionException 
     */
    public static void internal_main(String[] args) throws IOException, ClassNotFoundException,
                    SecurityException, NoSuchMethodException, InstantiationException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException, PrivilegedActionException {
        // The sole element of the classpath should be the jar that was launched.. 
        String jarName = System.getProperty("java.class.path");

        // Get the Attributes from the jar file
        JarFile jarFile = new JarFile(new File(jarName));
        Attributes a = jarFile.getManifest().getMainAttributes();
        jarFile.close();

        // Look for the attributes we need to find the real launch target: 

        // Command-Class redirects us to the real main
        String commandClass = a.getValue("Command-Class");

        // Require-Bundle tells us what should be on that real main's classpath
        String requiredBundles = a.getValue("Require-Bundle");        

        // Some tools do have an assumption about what the parent classloader should be.
        // If set to bootstrap,
        String parentCL = a.getValue("Parent-ClassLoader");

        // A list of packages that will be loaded first by the nested/created PackageDelegateClassLoader, 
        // the value should be like javax.xml.ws, javax.xml.jaxb
        String parentLastPackages = a.getValue("Parent-Last-Package");

        //true is to add the <JAVA_HOME>/lib/tools.jar in the classpath, other values will not
        String requireCompiler = a.getValue("Require-Compiler");

        boolean bootstrapCLParent = false;
        boolean compilerTools = false;

        if ("bootstrap".equals(parentCL)) {
            bootstrapCLParent = true;
        }
        if ("true".equals(requireCompiler)) {
            compilerTools = true;
        }

        // Parse the list of required bundles.
        List<LaunchManifest.RequiredBundle> rbs = LaunchManifest.parseRequireBundle(requiredBundles);

        // Get the repository for bundles in the install
        BundleRepositoryRegistry.initializeDefaults(null, false);
        AbstractResourceRepository repo = new NameBasedLocalBundleRepository(getInstallDir());
        
        // Collect the list of jars required for the command.. 
        List<URL> urls = new ArrayList<URL>();
        for (LaunchManifest.RequiredBundle rb : rbs) {
        	
        	
        	
            String bundleVersion = rb.getAttribute("version");
            bundleVersion = (null != bundleVersion) ? bundleVersion : DEFAULT_BUNDLE_VERSION;
            
            
            File f = repo.selectResource(rb.getAttribute("location"),
                                         rb.getSymbolicName(),
                                         VersionUtility.stringToVersionRange(bundleVersion));
            
            if (f != null) {
                urls.add(f.toURI().toURL());
            }
        }

        //support for embedded libraries in bundle
        List<URL> embeddedLibUrls = new LinkedList<URL>();
        for (URL jarFileUrl: urls) {
            embeddedLibUrls.addAll(getBundleEmbeddedLibs(jarFileUrl));
        }
        urls.addAll(embeddedLibUrls);

        // If the Require-Compiler is true, ensure that a compiler is available.
        if (compilerTools) {
            File toolsFile = Utils.getJavaTools();
            if (toolsFile != null) {
                urls.add(toolsFile.toURI().toURL());
            } else if (!isCompilerAvailable()) {
            	if(javaVersion() <= 8) {
                    error("error.sdkRequired", System.getProperty("java.home"));
            	} else {
            		error("error.compilerMissing");
            	}
                System.exit(ExitCode.ERROR_BAD_JAVA_VERSION);
                return;
            }
        }
        
        Thread currentThread = Thread.currentThread();
        final ClassLoader originalContextLoader = currentThread.getContextClassLoader();
        final URL[] urlArray = urls.toArray(new URL[urls.size()]);
        final List<String> parentLastPackageList = LaunchManifest.parseHeaderList(parentLastPackages);
        final ClassLoader parentLoader = bootstrapCLParent ? null : Utils.class.getClassLoader();
        final ClassLoader cl = new PackageDelegateClassLoader(urlArray, parentLoader, parentLastPackageList);
        currentThread.setContextClassLoader(cl);
        try {
            Class<?> clazz = cl.loadClass(commandClass);
            Method m = clazz.getMethod("main", args.getClass());
            m.invoke(null, (Object) args);
        } finally {
            currentThread.setContextClassLoader(originalContextLoader);
        }
    }

    private static String format(String key, Object... args) {
        String string = Utils.getResourceBundleString(key);
        return args == null || args.length == 0 ? string : MessageFormat.format(string, args);
    }

    private static void error(String key, Object... args) {
        System.err.println(format(key, args));
    }
    
    /**
     * Support for embedded libs in a bundle. 
     * @param jarFileUrl
     * @return
     * @throws IOException
     * @throws PrivilegedActionException 
     */
    private static List<URL> getBundleEmbeddedLibs( final URL jarFileUrl) throws IOException, PrivilegedActionException {
    	return AccessController.doPrivileged(new PrivilegedExceptionAction<List<URL>>() {
            public List<URL> run()
                            throws Exception
            {
            	
            	JarFile jarFile = new JarFile(URLDecoder.decode(jarFileUrl.getFile(),"utf-8"));    	   	
		        List<URL> libs = new LinkedList<URL>();
		        Attributes manifest = jarFile.getManifest().getMainAttributes();
		
		        String rawBundleClasspath = manifest.getValue("Bundle-ClassPath");
		        if (rawBundleClasspath != null) {
		
		                Set<String> allValidBundleClassPaths = new HashSet<String>();
		
		                String[] bundleClassPaths = rawBundleClasspath.split(",");
		                for (String bundleClassPath : bundleClassPaths) {
		                        bundleClassPath = bundleClassPath.trim();
		                        if (!bundleClassPath.equals(".") && bundleClassPath.endsWith(".jar")) {
		                                allValidBundleClassPaths.add(bundleClassPath);
		                        }
		                }
		
		                Enumeration<JarEntry> entries = jarFile.entries();
		                File tempDir = createTempDir("was-cmdline");
		                tempDir.deleteOnExit();
		                
		                int MAX_READ_SIZE = 512;
		                int offset = 0, read = 0;
		                byte[] buff = new byte[MAX_READ_SIZE];
		
		                while (entries.hasMoreElements()) {
		                        JarEntry jarEntry = entries.nextElement();
		                        String entryName = jarEntry.getName();
		
		                        if (allValidBundleClassPaths.contains(entryName)) {
		                                String fileName = entryName;
		
		                                int index;
		                                while ((index = fileName.indexOf("/")) != -1) {
		                                        fileName = fileName.substring(index + 1);
		                                }
		
		                                InputStream stream = jarFile.getInputStream(jarEntry);
		                                File file = new File(tempDir, fileName);
		                                file.deleteOnExit();
		
		                                FileOutputStream fileStream = new FileOutputStream(file);
		                                while ((read = stream.read(buff, offset, MAX_READ_SIZE)) > 0) {
		                                        fileStream.write(buff, 0, read);
		                                }
		
		                                fileStream.flush();
		                                fileStream.close();
		                                stream.close();
		
		                                libs.add(file.toURI().toURL());
		                        }
		                }
		        }
		        jarFile.close();
		        
		        
		        
		        return libs;
		    }
		});
		        
    }
    
    //Re-write createTempDir since nio is not supported in JDK6.
    public static File createTempDir(String baseName) {
    	File baseDir = new File(System.getProperty("java.io.tmpdir"));
    	   baseName = baseName+System.currentTimeMillis() + "-";

    	  for (int counter = 0; counter < 1000; counter++) {
    	    File tempDir = new File(baseDir, baseName + counter);
    	    if (tempDir.mkdir()) {
    	      return tempDir;
    	    }
    	  }
    	  throw new IllegalStateException("Failed to create directory within "
    	      + 1000 + " attempts (tried "
    	      + baseName + "0 to " + baseName + (1000 - 1) + ')');
    }
    
    public static File getInstallDir() {
        if (installDir == null) {
            URL url = Utils.class.getProtectionDomain().getCodeSource().getLocation();

            if (url.getProtocol().equals("file")) {
                // Got the file for the command line launcher, this lives in lib
                try {
                    if (url.getAuthority() != null) {
                        url = new URL("file://" + url.toString().substring("file:".length()));
                    }

                    File f = new File(url.toURI());
                    // The parent of the jar is lib, so the parent of the parent is the install.
                    installDir = f.getParentFile().getParentFile();
                } catch (MalformedURLException e) {
                    // Not sure we can get here so ignore.
                } catch (URISyntaxException e) {
                    // Not sure we can get here so ignore.
                }
            }
        }

        return installDir;
    }
    
    private static boolean isCompilerAvailable() {
        try {
            return javax.tools.ToolProvider.getSystemJavaCompiler() != null;
        } catch (Exception e) {
            // On JDK 6-8 the ToolProvider interface is in the rt.jar and therefore always available.
            // On JDK 9 the ToolProvider interface is in the "java.compiler" module which may not
            // be available at runtime and result in a CNFE.
            return false;
        }
    }

    private static int javaVersion() {
        String version = System.getProperty("java.version");
        String[] versionElements = version.split("\\D"); // split on non-digits

        // Pre-JDK 9 the java.version is 1.MAJOR.MINOR
        // Post-JDK 9 the java.version is MAJOR.MINOR
        int i = Integer.valueOf(versionElements[0]) == 1 ? 1 : 0;
        return Integer.valueOf(versionElements[i]);
    }
}