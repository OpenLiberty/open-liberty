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
import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspOptions;  //PK65013

public class TsxRepeatOptimizedTag implements OptimizedTag {
    private String index = null;
    private String start = null;
    private String end = null;
    private boolean indexProvided = false;
    
    public boolean canGenTagInMethod(OptimizedTagContext context) {
        return true;
    }

    public boolean doOptimization(OptimizedTagContext context) {
        return true;
    }

    public void generateImports(OptimizedTagContext context) {
    }

    public void generateDeclarations(OptimizedTagContext context) {
    	//PK65013 - start
        String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
        JspOptions jspOptions = context.getJspOptions(); 
    	if (jspOptions != null) {
            if (context.isTagFile() && jspOptions.isModifyPageContextVariable()) {
                pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
            }
        }
        //PK65013 - end
        
        context.writeDeclaration("throwExceptionDef",   
        "private static Boolean throwException = null;");
             
        context.writeDeclaration("throwExceptionMethod",   
        "private boolean throwException() {\n" +
        "    if (throwException != null) return throwException.booleanValue();\n" +
        "    String initParamIgnoreException =(String)(getServletConfig().getInitParameter(\"jsp.repeatTag.ignoreException\"));\n" +
        "    if ((initParamIgnoreException != null) && (initParamIgnoreException.toLowerCase().equals(\"true\"))){\n" +
        "        throwException = new Boolean(false);\n" +
        "    }\n" +
        "    else {\n" +
        "        throwException = new Boolean(true);\n" +
        "    }\n" +
        "    return throwException.booleanValue();\n" +
        "}"
        );
        
        //PK65013 change pageContext variable to customizable one.
        context.writeDeclaration("createIndexMgr",   
        "private void createIndexMgr(PageContext "+pageContextVar+") {\n" +
        "    if ("+pageContextVar+".getAttribute(\"TSXDefinedIndexManager\", PageContext.PAGE_SCOPE) == null) {\n"+
        "        "+pageContextVar+".setAttribute(\"TSXDefinedIndexManager\", new com.ibm.ws.jsp.tsx.tag.DefinedIndexManager(), PageContext.PAGE_SCOPE);\n"+
        "    }\n"+
        "}"
        );
        //PK65013 change pageContext variable to customizable one.
        context.writeDeclaration("createRepeatStack",   
        "private void createRepeatStack(PageContext "+pageContextVar+") {\n" +
        "    if ("+pageContextVar+".getAttribute(\"TSXRepeatStack\", PageContext.PAGE_SCOPE) == null) {\n"+
        "        "+pageContextVar+".setAttribute(\"TSXRepeatStack\", new java.util.Stack(), PageContext.PAGE_SCOPE);\n"+
        "    }\n"+
        "}"
        );
        //PK65013 change pageContext variable to customizable one.
        context.writeDeclaration("createRepeatLookup",   
        "private void createRepeatLookup(PageContext "+pageContextVar+") {\n" +
        "    if ("+pageContextVar+".getAttribute(\"TSXRepeatLookup\", PageContext.PAGE_SCOPE) == null) {\n"+
        "        "+pageContextVar+".setAttribute(\"TSXRepeatLookup\", new java.util.Hashtable(), PageContext.PAGE_SCOPE);\n"+
        "    }\n"+
        "}"
        );
    }

    public void generateStart(OptimizedTagContext context) {
//    	PK65013 - start
        String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
        JspOptions jspOptions = context.getJspOptions(); 
    	if (jspOptions != null) {
            if (context.isTagFile() && jspOptions.isModifyPageContextVariable()) {
                pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
            }
        }
        //PK65013 - end

        context.writeSource("createIndexMgr("+pageContextVar+");");
        context.writeSource("createRepeatStack("+pageContextVar+");");
        context.writeSource("createRepeatLookup("+pageContextVar+");");
        
        if (indexProvided == false) {
            index = context.createTemporaryVariable();
        }
        else {
            index = index.substring(1, index.length() - 1);
        }
        
        if (start == null) {
            start = "0";
        }
        else if (start.charAt(0) == '\"') {
            start = start.substring(1, start.length() - 1);
        }
        
        if (end == null) {
            end = Integer.toString(Integer.MAX_VALUE);
        }
        else if (end.charAt(0) == '\"') {
            end = end.substring(1, end.length() - 1);
        }
        //PK65013 change pageContext variable to customizable one.
        context.writeSource("((com.ibm.ws.jsp.tsx.tag.DefinedIndexManager) "+pageContextVar+".getAttribute(\"TSXDefinedIndexManager\", PageContext.PAGE_SCOPE)).addIndex(\""+index+"\");");
        
        context.writeSource("((java.util.Stack)"+pageContextVar+".getAttribute(\"TSXRepeatStack\", PageContext.PAGE_SCOPE)).push(\""+index+"\");");
        
        context.writeSource("for (int " + index + " = " + start + "; " + index + " <= " + end + "; " + index + "++) {");
        context.writeSource("    ((java.util.Hashtable)"+pageContextVar+".getAttribute(\"TSXRepeatLookup\", PageContext.PAGE_SCOPE)).put(\""+index+"\", new Integer("+index+"));");
        
        context.writeSource("    out = "+pageContextVar+".pushBody();");
        context.writeSource("    try {");
    }

    public void generateEnd(OptimizedTagContext context) {
    	//PK65013 - start
        String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
        JspOptions jspOptions = context.getJspOptions(); 
    	if (jspOptions != null) {
            if (context.isTagFile() && jspOptions.isModifyPageContextVariable()) {
                pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
            }
        }
        //PK65013 - end

        String bufferName = context.createTemporaryVariable();
    	//PK65013 change pageContext variable to customizable one.
        context.writeSource("        if ("+pageContextVar+".findAttribute(\"TSXBreakRepeat\") != null){");
        //      begin 221381: need to remove the request scope attr for break repeat to allow multiple tsx:repeat tags.
        context.writeSource("            out = "+pageContextVar+".popBody();");	//221381: pop current out object when breaking out of loop.
        context.writeSource("            "+pageContextVar+".removeAttribute(\"TSXBreakRepeat\", PageContext.REQUEST_SCOPE); // prepare for next repeat tag.");
        // end 22381
        context.writeSource("            break;");
        context.writeSource("        }");
        context.writeSource("        String " + bufferName + " = ((javax.servlet.jsp.tagext.BodyContent)out).getString();");
        context.writeSource("        out = "+pageContextVar+".popBody();");
        context.writeSource("        out.write("+bufferName+");");
        context.writeSource("    }");
        context.writeSource("    catch(ArrayIndexOutOfBoundsException ae) {");
        context.writeSource("        out = "+pageContextVar+".popBody();");
        context.writeSource("        break;");
        context.writeSource("    }");
        context.writeSource("    catch(Exception e) {");
        context.writeSource("        out = "+pageContextVar+".popBody();");
        context.writeSource("        if (throwException()){");
        context.writeSource("            throw  e;");
        context.writeSource("        } else {");
        context.writeSource("            out.println(\"Exception: \" + e);");
        context.writeSource("        }");
        context.writeSource("    }");
        context.writeSource("}");
        
        context.writeSource("((java.util.Stack)"+pageContextVar+".getAttribute(\"TSXRepeatStack\", PageContext.PAGE_SCOPE)).pop();");
        context.writeSource("((com.ibm.ws.jsp.tsx.tag.DefinedIndexManager) "+pageContextVar+".getAttribute(\"TSXDefinedIndexManager\", PageContext.PAGE_SCOPE)).removeIndex(\""+index+"\");");
    }

    public void setAttribute(String attrName, Object attrValue) {
        if (attrName.equals("index")) {
            index = (String)attrValue;
            indexProvided = true;
        }
        else if (attrName.equals("start")) {
            start = (String)attrValue;
        }
        else if (attrName.equals("end")) {
            end = (String)attrValue;
        }
    }
}
