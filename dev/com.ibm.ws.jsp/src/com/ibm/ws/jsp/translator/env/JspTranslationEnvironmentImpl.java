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
package com.ibm.ws.jsp.translator.env;

import com.ibm.ws.jsp.translator.utils.NameMangler;
import com.ibm.wsspi.jsp.compiler.JspCompilerFactory;
import com.ibm.wsspi.jsp.context.JspClassloaderContext;
import com.ibm.wsspi.jsp.context.translation.JspTranslationEnvironment;
import com.ibm.wsspi.jsp.resource.JspInputSourceFactory;
import com.ibm.wsspi.jsp.resource.translation.JspResourcesFactory;

public class JspTranslationEnvironmentImpl implements JspTranslationEnvironment {
    private String outputDir = null;
    private String contextRoot = null;
    private JspInputSourceFactory jspInputSourceFactory = null;
    private JspResourcesFactory jspResourcesFactory = null;
    private JspClassloaderContext jspClassloaderContext = null;
    private JspCompilerFactory jspCompilerFactory = null;
    
    public JspTranslationEnvironmentImpl(String outputDir, 
                                         String contextRoot, 
                                         JspInputSourceFactory jspInputSourceFactory,
                                         JspResourcesFactory jspResourcesFactory, 
                                         JspClassloaderContext jspClassloaderContext,
                                         JspCompilerFactory jspCompilerFactory) {
        this.outputDir = outputDir;
        this.contextRoot = contextRoot;
        this.jspInputSourceFactory = jspInputSourceFactory;
        this.jspResourcesFactory = jspResourcesFactory; 
        this.jspClassloaderContext = jspClassloaderContext;
        this.jspCompilerFactory = jspCompilerFactory;    
    }
    
    public String mangleClassName(String jspFileName) {
        return (NameMangler.mangleClassName(jspFileName));
    }

    public String getOutputDir() {
        return outputDir;
    }

    public String getContextRoot() {
        return contextRoot;
    }
    
    public JspInputSourceFactory getDefaultJspInputSourceFactory() {
        return jspInputSourceFactory; 
    }

    public JspResourcesFactory getDefaultJspResourcesFactory() {
        return jspResourcesFactory;
    }
    
    public JspClassloaderContext getDefaultJspClassloaderContext() {
        return jspClassloaderContext;
    }

    public JspCompilerFactory getDefaultJspCompilerFactory() {
        return jspCompilerFactory;
    }
}
