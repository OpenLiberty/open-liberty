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
package com.ibm.ws.jsp.translator.resource;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.inputsource.JspInputSourceContainerImpl;
import com.ibm.ws.jsp.translator.utils.NameMangler;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.resource.JspInputSource;
import com.ibm.wsspi.jsp.resource.translation.JspResources;

public class JspResourcesContainerImpl extends ResourcesImpl implements JspResources {
    private static Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.resource.JspResourcesImpl";
    static{
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }

    private JspInputSourceContainerImpl inputSource;
    private String packageName;
    private String webinfClassRelativeUrl;
    private Container container;
    private long sourceFileTimestamp;

    public JspResourcesContainerImpl(JspInputSource inputSource, JspOptions options, JspCoreContext context) {
        this.inputSource = (JspInputSourceContainerImpl)inputSource;
        container = this.inputSource.getContainer();
        String jspUri = inputSource.getRelativeURL();
        /*Entry sourceEntry = container.getEntry(jspUri);
        if (inputSource.getAbsoluteURL().getProtocol().equals("file")) {
            sourceFile = new File(context.getRealPath(jspUri));
        }
        else {
            try {
                sourceFile = File.createTempFile("jsp", NameMangler.mangleClassName(jspUri));
                sourceFile.deleteOnExit();
            }
            catch (IOException e1) {
                logger.logp(Level.WARNING, CLASS_NAME, "JspResourcesImpl", "Error creating temp file for jsp [" + jspUri +"]", e1);
            }
        }*/
        try {
            URL outURL = options.getOutputDir().toURL();
            String outURI = outURL.toString();
            if (jspUri.charAt(0) != '/') {
                jspUri = "/"+jspUri;
            }

            File generatedSourceDir = null;
            String convertedName = null;
            String webinfClassFilePath = null;
            if (options.isUseFullPackageNames()==false) {
                packageName=Constants.JSP_FIXED_PACKAGE_NAME;
                String unmangledOutURI = outURI;
                if (unmangledOutURI.endsWith("/") ) {
                    unmangledOutURI = unmangledOutURI + jspUri.substring(1,jspUri.lastIndexOf("/")+1);
                }
                else {
                    unmangledOutURI = unmangledOutURI + jspUri.substring(0,jspUri.lastIndexOf("/")+1);
                }

                URL unmangledOutURL = new URL(unmangledOutURI);
                generatedSourceDir = new File(unmangledOutURL.getFile());
                className = NameMangler.mangleClassName(jspUri);
                convertedName = generatedSourceDir.getPath() + File.separator + className;
                generatedSourceFile = new File(convertedName + ".java");
                classFile = new File(convertedName + ".class");

                //webinfClassFilePath = context.getRealPath("/WEB-INF/classes") + jspUri.substring(0,jspUri.lastIndexOf("/")+1);
                //webinfClassFile = new File(webinfClassFilePath + File.separator + className + ".class");
                webinfClassRelativeUrl="/WEB-INF/classes"+jspUri.substring(0,jspUri.lastIndexOf("/")+1) + "/" + className + ".class";
                
            }
            else {
                packageName=jspUri.substring(0,jspUri.lastIndexOf("/")+1);
                if ( !packageName.equals("/") ) {
                    packageName = NameMangler.handlePackageName (packageName);
                    packageName=Constants.JSP_PACKAGE_PREFIX+"."+packageName.substring(0,packageName.length()-1);
                }
                else {
                    packageName=Constants.JSP_PACKAGE_PREFIX;
                }
                String packageDir = packageName.replace('.','/');

                if (outURI.endsWith("/") )
                    outURI = outURI + packageDir;
                else
                    outURI = outURI + "/"+packageDir;

                outURL = new URL(outURI);
                generatedSourceDir = new File(outURL.getFile());
                className = NameMangler.mangleClassName(jspUri);
                convertedName = generatedSourceDir.getPath() + File.separator + className;
                generatedSourceFile = new File(convertedName + ".java");
                classFile = new File(convertedName + ".class");
                //webinfClassFilePath = context.getRealPath("/WEB-INF/classes") + "/"+packageDir;
                //webinfClassFile = new File(webinfClassFilePath + File.separator + className + ".class");
                webinfClassRelativeUrl = "/WEB-INF/classes/"+packageDir + "/" + className + ".class";
            }

            keepgenerated = options.isKeepGenerated();
            keepGeneratedclassfiles = options.isKeepGeneratedclassfiles();

            sourceFileTimestamp = inputSource.getLastModified();

            //TODO: is this the right behavior?
            //no, just check if the classFile timeStamp is >= the sourceFileTimestamp
            /*if (classFile.exists()) {
                classFile.setLastModified(sourceFileTimestamp);
            }*/
            /*if (inputSource.getAbsoluteURL().getProtocol().equals("file") == false) {
                if (classFile.exists()) {
                    sourceFile.setLastModified(classFile.lastModified());
                }
            }*/
        }
        catch (MalformedURLException e) {
            com.ibm.ws.ffdc.FFDCFilter.processException( e, "com.ibm.ws.jsp.translator.utils.JspFilesImpl.init", "45", this);
            logger.logp(Level.WARNING, CLASS_NAME, "JspResourcesImpl", "Error creating temp directory for jsp [" + jspUri +"]", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getClassName() {
        return className;
    }

    /** {@inheritDoc} */
    @Override
    public File getGeneratedSourceFile() {
        return generatedSourceFile;
    }

    /** {@inheritDoc} */
    @Override
    public JspInputSource getInputSource() {
        return inputSource;
    }

    /** {@inheritDoc} */
    @Override
    public String getPackageName() {
        return packageName;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExternallyTranslated() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOutdated() {
        File nullSourceFile = null;
        Entry containerEntry = inputSource.getInputSourceEntry();
        Entry webinfClassEntry = container.getEntry(webinfClassRelativeUrl);
        
        //If the inputSource is not a container entry
        if(containerEntry == null){
            long lastModified = inputSource.getLastModified();
            return ResourceUtil.isOutdated(lastModified, null, nullSourceFile, generatedSourceFile, classFile, webinfClassEntry, null);
        }
        return ResourceUtil.isOutdated(containerEntry, nullSourceFile, generatedSourceFile, classFile, webinfClassEntry, null);
    }

    /** {@inheritDoc} */
    @Override
    public void setCurrentRequest(HttpServletRequest arg0) {
        
    }

    /** {@inheritDoc} */
    @Override
    public void sync() {
        Entry e = container.getEntry(inputSource.getRelativeURL());
        if (e!=null) {
            sourceFileTimestamp = e.getLastModified();
        }
        ResourceUtil.sync(sourceFileTimestamp, generatedSourceFile, classFile, className, keepgenerated, keepGeneratedclassfiles);
        //sourceFileTimestamp = sourceFile.lastModified();
    }
}
