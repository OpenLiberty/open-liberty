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
package com.ibm.ws.jsp.translator.compiler;

import java.io.File;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.jsp.JspOptions;
import com.ibm.wsspi.jsp.compiler.JspCompiler;
import com.ibm.wsspi.jsp.compiler.JspCompilerFactory;
import com.ibm.wsspi.jsp.context.JspClassloaderContext;

public class JspCompilerFactoryImpl implements JspCompilerFactory {
    static protected Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.compiler.JspCompilerFactoryImpl";
    static {
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }
    private static final String WAS_ROOT_BASE = System.getProperty("was.install.root");
    
    private String absouluteContextRoot;
    private JspClassloaderContext classloaderContext;
    private JspOptions options;
    
    private String classpath = null;
	boolean useOptimizedClasspath = false;
    
    public JspCompilerFactoryImpl(String absouluteContextRoot, JspClassloaderContext classloaderContext, JspOptions options) {
        this.absouluteContextRoot = absouluteContextRoot;
        this.classloaderContext = classloaderContext;
        this.options = options;
        
        logger.logp(Level.FINE, CLASS_NAME, "JspCompilerFactoryImpl", "jspCompileClasspath: "+options.getJspCompileClasspath());
        
        // WAS_ROOT_BASE will be null during WAS build complilations - since there is no WAS installation
        if (WAS_ROOT_BASE!=null) {
            classpath = classloaderContext.getOptimizedClassPath();
            if (options.getJspCompileClasspath() != null) {
                useOptimizedClasspath = true;
            }
        }
        else {
            // if WAS_ROOT_BASE is null, set classpath to the full classpath and force flag to true.  This
            // will tell the compiler to NOT retry a failed, single-file compilation.
            classpath = classloaderContext.getClassPath()+ File.pathSeparatorChar + options.getOutputDir().getPath();
            useOptimizedClasspath=true;
        }
    }
    
    public JspCompiler createJspCompiler() {
        if (options.isUseJikes()) {
            return new JikesJspCompiler(absouluteContextRoot, classloaderContext, options, classpath, useOptimizedClasspath);
        }
        else {
            return new StandardJspCompiler(classloaderContext, options, classpath, useOptimizedClasspath);
        }
    }
}
