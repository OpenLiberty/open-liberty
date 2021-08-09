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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.jsp.tagext.TagFileInfo;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.inputsource.JspInputSourceContainerImpl;
import com.ibm.ws.jsp.taglib.TagLibraryInfoImpl;
import com.ibm.ws.jsp.translator.utils.NameMangler;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.resource.JspInputSource;
import com.ibm.wsspi.jsp.resource.translation.TagFileResources;

public class TagFileResourcesImpl extends ResourcesImpl implements TagFileResources {
    private static Logger logger;
    private static final String CLASS_NAME="com.ibm.ws.jsp.translator.resource.TagFileResourcesImpl";
    
    static{
            logger = Logger.getLogger("com.ibm.ws.jsp");
    }
    private boolean hasContainer = false;
    private String webinfClassRelativeUrl;
    private transient Container container;
    
    public TagFileResourcesImpl(JspInputSource inputSource, TagFileInfo tfi, JspOptions options, JspCoreContext context) {
        this.inputSource = inputSource;
        if (inputSource instanceof JspInputSourceContainerImpl) {
            hasContainer=true;
            container = ((JspInputSourceContainerImpl)inputSource).getContainer();
            //sourceFile = ((JspInputSourceContainerImpl)inputSource).
        } else if (inputSource.getAbsoluteURL().getProtocol().equals("file")) {
            sourceFile = new File(context.getRealPath(inputSource.getRelativeURL()));
        }
        else {
            String file = inputSource.getContextURL().getFile();
            sourceFile = new File(file.substring(file.indexOf("file:")+5, file.indexOf("!/")));
        }
        String tagFilePath = null;
        String tldOriginatorId = null;

        TagLibraryInfoImpl tli = (TagLibraryInfoImpl)tfi.getTagInfo().getTagLibrary();
        tldOriginatorId = tli.getOriginatorId();

        if (tfi.getPath().startsWith("/WEB-INF/tags")) {
            tagFilePath = tfi.getPath().substring(tfi.getPath().indexOf("/WEB-INF/tags") + 13);
        }
        else if (tfi.getPath().startsWith("/META-INF/tags")) {
            tagFilePath = tfi.getPath().substring(tfi.getPath().indexOf("/META-INF/tags") + 14);
        }
        tagFilePath = tagFilePath.substring(0, tagFilePath.lastIndexOf("/"));
        
        //PM70267 start
        if (tagFilePath.indexOf("-") > -1) {
            tagFilePath = NameMangler.handlePackageName(tagFilePath);
            tagFilePath = File.separatorChar + tagFilePath;                     
            tagFilePath = tagFilePath.replace('.', File.separatorChar);                 
        } else {
            tagFilePath = tagFilePath.replace('/', File.separatorChar);
        }
        //PM70267 end

        
        tagFilePath = tagFilePath.replace('/', File.separatorChar);
        tagFilePath = Constants.TAGFILE_PACKAGE_PATH + tldOriginatorId + tagFilePath;
        packageName = tagFilePath.replace(File.separatorChar, '.');

        className = tfi.getPath();
        className = className.substring(className.lastIndexOf('/') + 1);
        className = className.substring(0, className.indexOf(".tag"));
        className = NameMangler.mangleClassName(className);

        File generatedSourceDir = new File(options.getOutputDir().getPath() + File.separator + tagFilePath);
        String convertedName = generatedSourceDir.getPath() + File.separator + className;
        generatedSourceFile = new File(convertedName + ".java");
        classFile = new File(convertedName + ".class");

        if (hasContainer) {
            webinfClassRelativeUrl = "/WEB-INF/classes/" + tagFilePath;
        } else {
            String webinfClassFilePath = context.getRealPath("/WEB-INF/classes") + File.separator + tagFilePath;
            webinfClassFile = new File(webinfClassFilePath + File.separator + className + ".class");
        }

        keepgenerated = options.isKeepGenerated();
        keepGeneratedclassfiles = options.isKeepGeneratedclassfiles();
        if (hasContainer) {
            sourceFileTimestamp = inputSource.getLastModified();
        } else {
            sourceFileTimestamp = sourceFile.lastModified();
        }
    }

    public boolean isOutdated() {
        if (hasContainer) {
            if (System.getSecurityManager() != null) {                
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        if(generatedSourceFile.getParentFile().exists() == false){
                            boolean rc = generatedSourceFile.getParentFile().mkdirs();
                            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
                                logger.logp(Level.FINEST, CLASS_NAME, "isOutdated", (rc?"Created":"Unable to create") +" directory for generated source file ["+generatedSourceFile.getParentFile() +"]");
                            }
                        }
                        return null;
                    }
                });
            } else {
                if(generatedSourceFile.getParentFile().exists() == false){
                    boolean rc = generatedSourceFile.getParentFile().mkdirs();
                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
                        logger.logp(Level.FINEST, CLASS_NAME, "isOutdated", (rc?"Created":"Unable to create") +" directory for generated source file ["+generatedSourceFile.getParentFile() +"]");
                    }
                }
            }
            Entry webinfClassEntry = container.getEntry(webinfClassRelativeUrl);
            return ResourceUtil.isTagFileOutdated(sourceFileTimestamp, generatedSourceFile, classFile, webinfClassEntry, webinfClassFile);
        }
        return ResourceUtil.isTagFileOutdated(sourceFile, generatedSourceFile, classFile, webinfClassFile);
    }

    public void syncGeneratedSource() {
        long sourceFileLastModified = 0;
        if (hasContainer) {
            sourceFileLastModified = inputSource.getLastModified();
        } else {
            sourceFileLastModified = sourceFile.lastModified();
        }
        ResourceUtil.syncGeneratedSource(sourceFileLastModified, generatedSourceFile);
    }

    public void sync() {
        long sourceFileLastModified = 0;
        if (hasContainer) {
            sourceFileLastModified = inputSource.getLastModified();
        } else {
            sourceFileLastModified = sourceFile.lastModified();
        }
        ResourceUtil.syncTagFile(sourceFileLastModified, generatedSourceFile, classFile, keepgenerated, keepGeneratedclassfiles);
        sourceFileTimestamp = sourceFileLastModified;
    }
}
