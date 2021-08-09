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
package com.ibm.ws.jsp.translator.optimizedtag.impl;

import com.ibm.ws.jsp.translator.optimizedtag.OptimizedTag;
import com.ibm.ws.jsp.translator.optimizedtag.OptimizedTagContext;
import com.ibm.ws.jsp.JspOptions;  //PK65013
import com.ibm.ws.jsp.Constants; //PK65013

public class JSTLIfOptimizedTag implements OptimizedTag {
    private String var = null;
    private String test = null;
    private String scope = null;
    
    public boolean doOptimization(OptimizedTagContext context) {
        boolean optimize = true;
        
        if (context.isJspAttribute("var")) {
            optimize = false;
        }
        else if (context.isJspAttribute("test")) {
            optimize = false;
        }
        
        return optimize;
    }

    public void generateImports(OptimizedTagContext context) {
    }

    public void generateDeclarations(OptimizedTagContext context) {
    }

    public void generateStart(OptimizedTagContext context) {
    	//PK65013 - start
        String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
        JspOptions jspOptions = context.getJspOptions(); 
    	if (jspOptions != null) {
            if (context.isTagFile() && jspOptions.isModifyPageContextVariable()) {
                pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
            }
        }
        //PK65013 - end
        
        String condV = context.createTemporaryVariable();
        context.writeSource("boolean " + condV + " = " + test + ";");
        if (context.hasAttribute("var")) {
            String scopeStr = "PageContext.PAGE_SCOPE";
            if (context.hasAttribute("scope")) {
                if ("request".equals(scope)) {
                    scopeStr = "PageContext.REQUEST_SCOPE";
                }
                else if ("session".equals(scope)) {
                    scopeStr = "PageContext.SESSION_SCOPE";
                }
                else if ("application".equals(scope)) {
                    scopeStr = "PageContext.APPLICATION_SCOPE";
                }
            }
            //PK65013 change pageContext variable to customizable one.
            context.writeSource(pageContextVar+".setAttribute(" + var + ", new Boolean(" + condV + ")," + scopeStr + ");");
        }
        context.writeSource("if (" + condV + ") {");
    }

    public void generateEnd(OptimizedTagContext context) {
        context.writeSource("}");
    }

    public void setAttribute(String attrName, Object attrValue) {
        if (attrName.equals("test")) {
            test = (String)attrValue;
        }
        else if (attrName.equals("var")) {
            var = (String)attrValue;
        }
        else if (attrName.equals("scope")) {
            scope = (String)attrValue;
        }
    }

    public boolean canGenTagInMethod(OptimizedTagContext context) {
        return true;
    }
}
