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
package com.ibm.ws.jsp.inmemory.webcontainerext;

import java.io.File;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.Iterator;
import java.util.List;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.inmemory.resource.InMemoryResources;
import com.ibm.ws.jsp.webcontainerext.JSPExtensionClassLoader;
import com.ibm.wsspi.jsp.context.JspClassloaderContext;

public class InMemoryJspClassLoader extends JSPExtensionClassLoader {
    private List resourcesList = null;
    private JspClassloaderContext jspClassloaderContext = null;
    private CodeSource codeSource = null;
    private PermissionCollection permissionCollection = null;
    private String className = null;

    public InMemoryJspClassLoader(URL[] urls,
                                  JspClassloaderContext jspClassloaderContext,
                                  String className,
                                  CodeSource codeSource,
                                  PermissionCollection permissionCollection,
                                  List resourcesList) {
        super(urls, jspClassloaderContext, className, codeSource, permissionCollection);
        this.resourcesList = resourcesList;
        this.className = className;
        this.codeSource = codeSource;
        this.permissionCollection = permissionCollection;
        this.jspClassloaderContext = jspClassloaderContext;
    }
    
    protected byte[] loadClassDataFromFile(String fileName) {
        byte[] classBytes = null;
        for (Iterator itr = resourcesList.iterator(); itr.hasNext();) {
            InMemoryResources inMemoryResources = (InMemoryResources)itr.next();
            classBytes = inMemoryResources.getClassBytes(fileName);
            if (classBytes != null)
                break;
        }
        return classBytes;
    }
    
    public Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (System.getSecurityManager() != null){
            final String tmpName = name;
            final boolean tmpResolve = resolve;
            try{
                return (Class) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                        public Object run() throws ClassNotFoundException {
                                return _loadClass(tmpName, tmpResolve);
                        }
                    });
            }catch (PrivilegedActionException pae){
                throw (ClassNotFoundException)pae.getException();
            }
        }
        else{
            return _loadClass(name, resolve);
        }
    }

    private Class _loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class clazz = null;

        clazz = findLoadedClass(name);
        if (clazz != null) {
            if (resolve)
                resolveClass(clazz);
            return (clazz);
        }

        int dot = name.lastIndexOf('.');
        if (System.getSecurityManager() != null) {
            if (dot >= 0) {
                try {
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
            clazz = jspClassloaderContext.getClassLoader().loadClass(name);
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
            byte[] cdata = loadClassDataFromFile(name);
            if (cdata != null) {
                if (System.getSecurityManager() != null) {
                    ProtectionDomain pd = new ProtectionDomain(codeSource, permissionCollection);
                    clazz = defClass(name, cdata, cdata.length, pd);
                }
                else {
                    clazz = defClass(name, cdata, cdata.length, null);
                }
            }
            else {
                if (jspClassloaderContext.getClassLoader() != null) {
                    clazz = jspClassloaderContext.getClassLoader().loadClass(classFile);
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
 
    private final Class defClass(String className, byte[] classData, int length, ProtectionDomain pd) {
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

}
