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

public class JSTLOutOptimizedTag implements OptimizedTag {
    String value = null;
    String escapeXml = null;
    String defaultValue = null;

    public boolean canGenTagInMethod(OptimizedTagContext context) {
        return true;
    }

    public boolean doOptimization(OptimizedTagContext context) {
        boolean optimize = true;
        
        if (context.hasBody() || context.hasJspBody()) {
            optimize = false;
        }
        else if (context.isJspAttribute("value")) {
            optimize = false;
        }
        else if (context.isJspAttribute("escapeXml")) {
            optimize = false;
        }
        else if (context.isJspAttribute("defaultValue")) {
            optimize = false;
        }
        
        return optimize;
    }

    public void setAttribute(String attrName, Object attrValue) {
        if (attrName.equals("value")) {
            value = (String)attrValue;
        }
        else if (attrName.equals("escapeXml")) {
            escapeXml = (String)attrValue;
        }
        else if (attrName.equals("default")) {
            defaultValue = (String)attrValue;
        }
    }
    
    public void generateImports(OptimizedTagContext context) {
    }

    public void generateDeclarations(OptimizedTagContext context) {
    }

    public void generateStart(OptimizedTagContext context) {
        
        if (context.hasAttribute("escapeXml")) {
            if (escapeXml.equalsIgnoreCase("true")) {
                context.writeSource("boolean escapeXml = true;");
            }
            else if (escapeXml.equalsIgnoreCase("false")) {
                context.writeSource("boolean escapeXml = false;");
            }
            else {
                context.writeSource("boolean escapeXml = "+escapeXml+";");
            }
        }
        else {
            context.writeSource("boolean escapeXml = true;");
        }
        
        String valueV = context.createTemporaryVariable();
        context.writeSource("Object " + valueV + " = " + value + ";");
        String defaultValueV = context.createTemporaryVariable();
        context.writeSource("Object " + defaultValueV + " = " + defaultValue + ";");
        context.writeSource("if ("+valueV+" != null) {");
        context.writeSource("   com.ibm.ws.jsp.translator.optimizedtag.impl.JSTLOutUtil.writeOut(out, "+valueV+", escapeXml);");
        context.writeSource("} else if ("+defaultValueV+" != null) {");
        context.writeSource("   com.ibm.ws.jsp.translator.optimizedtag.impl.JSTLOutUtil.writeOut(out, "+defaultValueV+", escapeXml);");
        context.writeSource("} else {");
        context.writeSource("   out.write(\"\");");
        context.writeSource("}");
    }
    
    public void generateEnd(OptimizedTagContext context) {
    }

}
