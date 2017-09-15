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
package com.ibm.ws.jsp.inmemory.resource;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.jsp.Constants;
import com.ibm.wsspi.jsp.context.JspCoreContext;
import com.ibm.wsspi.jsp.context.translation.JspTranslationEnvironment;
import com.ibm.wsspi.jsp.resource.JspInputSource;
import com.ibm.wsspi.jsp.resource.translation.JspResources;

public class InMemoryJspResources implements JspResources, InMemoryResources {
    protected JspInputSource inputSource = null;
    protected File sourceFile = null;
    protected long sourceFileTimestamp = 0;
    protected File generatedSourceFile = null;
    protected String className = null;
    protected String packageName = null;
    protected CharArrayWriter generatedSourceWriter = null;
    protected Map<String,byte[]> classBytesMap = new HashMap<String,byte[]>();
    
    public InMemoryJspResources(JspInputSource inputSource, JspCoreContext context, JspTranslationEnvironment env) {
        this.inputSource = inputSource;
        String jspUri = inputSource.getRelativeURL();
        sourceFile = new File(context.getRealPath(jspUri));
        packageName=Constants.JSP_FIXED_PACKAGE_NAME;
        className = env.mangleClassName(jspUri);
        generatedSourceFile = new File(System.getProperty("java.io.tmpdir")+File.separator+sourceFile.getName()+".gen");
        generatedSourceWriter = new CharArrayWriter();
    }

    public String getClassName() {
        return className;
    }

    public File getGeneratedSourceFile() {
        return generatedSourceFile;
    }

    public Writer getGeneratedSourceWriter() {
        return generatedSourceWriter;
    }
    
    public char[] getGeneratedSourceChars() {
        return generatedSourceWriter.toCharArray();
    }
    
    public JspInputSource getInputSource() {
        return inputSource;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean isExternallyTranslated() {
        return false;
    }

    public boolean isOutdated() {
        if (sourceFile.lastModified() != sourceFileTimestamp)
            return true;
        else 
            return false;
    }

    public void sync() {
        sourceFileTimestamp = sourceFile.lastModified();
        generatedSourceWriter.reset();
    }
    
    public void setCurrentRequest(HttpServletRequest request) {}

    public byte[] getClassBytes(String className) {
        return classBytesMap.get(className);
    }

    public void setClassBytes(byte[] bytes, String className) {
        classBytesMap.put(className, bytes);
    }
}
