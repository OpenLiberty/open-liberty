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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jsp.Constants;
import com.ibm.wsspi.jsp.context.JspClassloaderContext;

public class JSPExtensionClassLoader extends URLClassLoader {
    static class Delegation {
        // This is only used to place a non-class loader class on the call stack which is loaded from a bundle.
        // This is needed as a workaround for defect 89337.
        @Trivial
        static Class<?> loadClass(String className, boolean resolve, JSPExtensionClassLoader loader) throws ClassNotFoundException {
            return loader.loadClass0(className, resolve);
        }
    }

    private static final TraceComponent tc = Tr.register(JSPExtensionClassLoader.class);
    private PermissionCollection permissionCollection = null;
    private CodeSource codeSource = null;
    private String className = null;
    private ClassLoader parent = null;
    private JspClassloaderContext jspClassloaderContext = null;
    private final ProtectionDomain defaultPD;
    private Map<String,ProtectionDomain> pdCache = new HashMap<String,ProtectionDomain>();

    public JSPExtensionClassLoader(URL[] urls,
                                   JspClassloaderContext jspClassloaderContext,
                                   String className,
                                   CodeSource codeSource,
                                   PermissionCollection permissionCollection) {
        super(urls, jspClassloaderContext.getClassLoader());
        this.jspClassloaderContext = jspClassloaderContext;
        this.permissionCollection = permissionCollection;
        this.codeSource = codeSource;
        this.className = className;
        this.parent = jspClassloaderContext.getClassLoader();
        defaultPD = new ProtectionDomain(codeSource, permissionCollection);
    }

    @Trivial
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return (loadClass(name, false));
    }

    @Trivial
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return Delegation.loadClass(name, resolve, this);
    }

    Class<?> loadClass0(String name, boolean resolve) throws ClassNotFoundException {
        if (System.getSecurityManager() != null){
            final String tmpName = name;
            final boolean tmpResolve = resolve;
            try{
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                        public Class<?> run() throws ClassNotFoundException {
                                return _loadClass(tmpName, tmpResolve, true);
                        }
                    });
            }catch (PrivilegedActionException pae){
                throw (ClassNotFoundException)pae.getException();
            }
        }
        else{
            return _loadClass(name, resolve, false);
        }
    }

    private Class<?> _loadClass(String name, boolean resolve, final boolean checkPackageAccess) throws ClassNotFoundException {
        Class<?> clazz = null;

        clazz = findLoadedClass(name);
        if (clazz != null) {
            if (resolve)
                resolveClass(clazz);
            return (clazz);
        }

        int dot = name.lastIndexOf('.');
        if (checkPackageAccess) {
            if (dot >= 0) {
                try {
                    //PK50834 switching from using securityManager variable to direct call to System
                    System.getSecurityManager().checkPackageAccess(name.substring(0, dot));
                }
                catch (SecurityException se) {
                    String error = "Security Violation, attempt to use " + "Restricted Class: " + name;
                    throw new ClassNotFoundException(error);
                }
            }
        }

        // Class is in a package other than the ones we know; delegate to thread context class loader
        if (name.startsWith(Constants.JSP_PACKAGE_PREFIX) == false &&
            name.startsWith(Constants.JSP_FIXED_PACKAGE_NAME) == false &&
            name.startsWith(Constants.OLD_JSP_PACKAGE_NAME) == false &&
            name.startsWith(Constants.TAGFILE_PACKAGE_NAME) == false) {
            clazz = parent.loadClass(name);
            if (resolve)
                resolveClass(clazz);
            return clazz;
        }
        else {
            String classFile = null;

            if (name.startsWith(Constants.JSP_FIXED_PACKAGE_NAME + "." + className)) {
                classFile = name.substring(Constants.JSP_FIXED_PACKAGE_NAME.length() + 1) + ".class";
            }
            else if (name.startsWith(Constants.OLD_JSP_PACKAGE_NAME+ "." + className)) {
                classFile = name.substring(Constants.OLD_JSP_PACKAGE_NAME.length() + 1) + ".class";
            }
            else {
                classFile = name.replace('.', File.separatorChar) + ".class";
            }
            byte[] cdata = loadClassDataFromFile(classFile);
            if (cdata != null) {
                    
                //PK71207 start
                // add try/catch block to catch an Error like java.lang.LinkageError if two threads are trying to call defineClass with the same class name.
                // We will try to find the loaded class again as if that error occurs means it should be loaded by the first thread.

                try {
                    ProtectionDomain pd = getClassSpecificProtectionDomain(classFile, checkPackageAccess);//new ProtectionDomain(codeSource, permissionCollection);
                    clazz = defClass(name, cdata, cdata.length, pd);
                }
                catch (Error e) {
                    clazz = findLoadedClass(name);
                    if (clazz != null) {
                        if (resolve)
                            resolveClass(clazz);
                        return (clazz);
                    }
                    cdata = loadClassDataFromFile(classFile); 
                    if (cdata==null) {
                        if (parent != null) {
                            clazz = parent.loadClass(classFile);
                            if (clazz != null) {
                                if (resolve)
                                    resolveClass(clazz);
                                return clazz;
                            }
                        }
                    }
                    throw e;
                }
                //PK71207 end
            }
            else {
                if (parent != null) {
                    clazz = parent.loadClass(classFile);
                }
            }
            if (clazz != null) {
                if (resolve)
                    resolveClass(clazz);
                return clazz;
            }
        }

        throw new ClassNotFoundException(name);
    }

    private final Class<?> defClass(String className, byte[] classData, int length, ProtectionDomain pd) {
        if (jspClassloaderContext.isPredefineClassEnabled()) {
            classData = jspClassloaderContext.predefineClass(className, classData);
        }
        if (pd != null) {
            return defineClass(className, classData, 0, classData.length, pd);
        }
        else {
            return defineClass(className, classData, 0, classData.length);
        }
    }

    /**
     * Load JSP class data from file.
     */
    protected byte[] loadClassDataFromFile(String fileName) {
        byte[] classBytes = null;
        try {
            InputStream in = getResourceAsStream(fileName);
            if (in == null) {
                return null;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte buf[] = new byte[1024];
            for (int i = 0;(i = in.read(buf)) != -1;)
                baos.write(buf, 0, i);
            in.close();
            baos.close();
            classBytes = baos.toByteArray();
        }
        catch (Exception ex) {
            return null;
        }
        return classBytes;
    }

    public URL getResource(String name) {
        URL resourceURL = findResource(name);
        if (resourceURL != null) {
            return resourceURL;
        }
        return parent.getResource(name);
    }

    public InputStream getResourceAsStream(String name) {
        try {
            URL resourceURL = getResource(name);
            if (resourceURL == null) {
                return null;
            }
            return resourceURL.openStream();
        }
        catch (java.net.MalformedURLException malURL) {
            return null;
        }
        catch (IOException io) {
            return null;
        }
    }
    
    private ProtectionDomain getClassSpecificProtectionDomain(String classFile, boolean useDoPriv) {
        ProtectionDomain pd;
        URL classUrl = getResource(classFile);
        URL codeLocation = null;
        String codeLocationString = null;
        if (classUrl != null) {
            String urlString = classUrl.toExternalForm();
            int x = urlString.lastIndexOf(classFile);
            if (x > -1) {
                final String finalCodeLocationString = codeLocationString = urlString.substring(0, x);
                codeLocation = AccessController.doPrivileged(new PrivilegedAction<URL>() {

                    @Override
                    public URL run() {
                        try {
                            return new URL(finalCodeLocationString);
                        } catch (MalformedURLException ex) {
                            return null;
                        }
                    }
                });
            }
        }
            
        if (codeLocation != null && codeLocationString != null) {
            synchronized(pdCache) {
                pd = pdCache.get(codeLocationString);
                if (pd == null) {
                    ClassLoader tccl = null;
                    try {
                        tccl = (ClassLoader)AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                            public Object run() {
                                    return Thread.currentThread().getContextClassLoader();
                            }
                        });
                    }catch (PrivilegedActionException pae)
                        {
                            if (tc.isDebugEnabled()) Tr.debug(tc, "Failed to get the ContextClassLoader." + pae);
                        }
                    
                    if (tccl !=null && tccl instanceof BundleReference) {
                        Bundle b = ((BundleReference) tccl).getBundle();
                        if (b.getHeaders("Web-ContextPath") != null) {
                            pd = b.adapt(ProtectionDomain.class);
                            if (tc.isDebugEnabled()) Tr.debug(tc, "WAB ProtectionDomain obtained" + pd);
                        }
                    }
                    if (pd == null) {
                        pd = new ProtectionDomain(new CodeSource(codeLocation, codeSource == null ? null : codeSource.getCertificates()), permissionCollection);
                    }
                    pdCache.put(codeLocationString, pd);
                }
            }
        } else {
            pd = defaultPD;
        }

        return pd;
    }
}
