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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.tools.SimpleJavaFileObject;

import com.ibm.wsspi.jsp.resource.translation.JspResources;

public class JspFileObject extends SimpleJavaFileObject {
    
    static final protected Logger logger = Logger.getLogger("com.ibm.ws.jsp");
    private static final String CLASS_NAME = "com.ibm.ws.jsp.translator.compiler.utils.JspFileObject";
    
    private URI uri;
    
    private final File source;
    private String binaryName;
    private boolean isJar = false;
    private String protocol = "";
    private String javaEncoding = "UTF-8"; //This is the encoding by default
    private JarFile jarFile = null; 

    /**
     * Create a JspFileObject for a class file.
     * @param binaryName
     * @param uri
     * @param protocol
     */
    JspFileObject(final String binaryName, final URI uri, String protocol) {
        super(uri, Kind.CLASS);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "JspFileObject", "Creating JspFileObject. binaryName = " + binaryName + " uri = " + uri + " protocol = " + protocol);
        }
        this.binaryName = binaryName;
        this.uri = uri;
        this.source = null;
        this.protocol = protocol;
        if (protocol.equals("jar"))
            this.isJar = true;
        else if (protocol.equals("wsjar")) {
            this.isJar = true;
            try {
                //I need to do this here of wsjar because I need to close JarFiles when the compilation finishes
                if (System.getSecurityManager() != null) {
                    this.jarFile = java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedAction<JarFile>() {
                            public JarFile run() {
                                try {
                                    return new JarFile(uri.toURL().getFile());
                                } catch (IOException e) {
                                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                                        logger.logp(Level.FINE, CLASS_NAME, "JspFileObject", "IOException in doPriviledged creating JarFile.", e);
                                    }
                                }
                                return null;
                            }
                        });
                } else {
                    this.jarFile = new JarFile(uri.toURL().getFile());
                }
            } catch (MalformedURLException e) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "JspFileObject", "Unable to get URI of wsjar file.", e);
                }
            } catch (IOException e) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "JspFileObject", "IOException processing jar.", e);
                }
            }
        }
    }

    /**
     * Create a JspFileObject for a class or source with encoding UTF-8.
     * @param source
     * @param kind
     */
    public JspFileObject(final JspResources source, final Kind kind) {
        this(source, kind, null);
    }

    /**
     * Create a JspFileObject to represent a class or source code.
     * @param source
     * @param kind
     * @param javaEncoding If the kind is source code, it is important to define the encoding. If null, UTF-8 is used.
     */
    public JspFileObject(final JspResources source, final Kind kind, final String javaEncoding) {
        super(jspResourceSourceOrClassURI(source, kind), kind);
        this.source = source.getGeneratedSourceFile();
        this.binaryName = source.getPackageName() + '.' + source.getClassName();
        this.uri = jspResourceSourceOrClassURI(source, kind);
        if (javaEncoding != null)
            this.javaEncoding = javaEncoding;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "JspFileObject", "Creating JspFileObject. source = " + this.source + " uri = " + this.uri + " binaryName = " + binaryName + " Kind = " + kind);
        }
    }
    
    private static URI jspResourceSourceOrClassURI(JspResources source, Kind kind) {
        if (kind.equals(Kind.CLASS))
            return new File(source.getGeneratedSourceFile().getAbsolutePath().replaceAll(Kind.SOURCE.extension + "$", Kind.CLASS.extension)).toURI();
        else //means that it is a source file
            return source.getGeneratedSourceFile().toURI();
    }

    @Override
    public CharSequence getCharContent(final boolean ignoreEncodingErrors) 
                    throws UnsupportedOperationException, IOException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "getCharContent", "Reading file = " + this.source);
        }
        if (source == null)
            throw new UnsupportedOperationException("Trying to read a file that does not contain source code.");
        
        FileInputStream jspSourceInputStream;
        if (System.getSecurityManager() != null) {
            jspSourceInputStream = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<FileInputStream>() {
                    public FileInputStream run() {
                        try {
                            return new FileInputStream(source);
                        } catch (FileNotFoundException e) {
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                                logger.logp(Level.FINE, CLASS_NAME, "getCharContent", "There was a problem getting the FileInputStream of source = " + source, e);
                            }
                        }
                        return null;
                    }
                });
        } else {
            jspSourceInputStream = new FileInputStream(source);
        }
        
        if (jspSourceInputStream == null)
            throw new IOException("There was a problem getting the FileInputStream of source = [" + source + "]");
        
        InputStreamReader file = new InputStreamReader(jspSourceInputStream, javaEncoding);
        BufferedReader reader = new BufferedReader(file);
        StringBuffer sourceStringBuffer = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null)
            sourceStringBuffer.append(line).append('\n'); //appending new line char should not be of any benefit... it feels correct
        
        try {
            reader.close();
        } catch (IOException ex) {
            /*
             * Catching IOE only from close. If there was a problem closing reader,
             * we should try to return sourceStringBuffer as it probably
             * has all the content already.
             */
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "getCharContent", "There was a problem closing reader", ex);
            }
        }
        return sourceStringBuffer;
    }

    @Override
    public InputStream openInputStream() throws MalformedURLException, IOException {
    
        JarFile jarFile = null;
        if (this.isJar) {
            if (protocol.equals("wsjar"))
                jarFile = this.jarFile;
            else {
                JarURLConnection jarUrlConnection = (JarURLConnection)uri.toURL().openConnection();
                jarFile = jarUrlConnection.getJarFile();
            }
            
            JarEntry jarEntry = jarFile.getJarEntry(binaryName.replace('.', '/') + Kind.CLASS.extension);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "openInputStream", "Opening the stream of jarEntry = " + jarEntry + " from jar = " + jarFile);
            }
            return jarFile.getInputStream(jarEntry);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "openInputStream", "Opening the stream of uri = " + uri);
        }
        return uri.toURL().openStream();
    }

    public String getBinaryName() {
        return binaryName;
    }

    public JarFile getJarFile() {
        return this.jarFile;
    }
}
