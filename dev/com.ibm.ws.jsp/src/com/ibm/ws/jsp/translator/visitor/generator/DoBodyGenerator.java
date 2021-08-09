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
package com.ibm.ws.jsp.translator.visitor.generator;

import org.w3c.dom.Node;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;

public class DoBodyGenerator extends PageTranslationTimeGenerator {
    private MethodWriter bodyWriter = new MethodWriter();
    
    public DoBodyGenerator() {
        super(new String[] {"varReader", "var", "scope"});
    }
    
    public void startGeneration(int section, JavaCodeWriter writer) throws JspCoreException {}

    public void endGeneration(int section, JavaCodeWriter writer)  throws JspCoreException {
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            writeDebugStartBegin(writer);
            writer.println("((org.apache.jasper.runtime.JspContextWrapper) this.jspContext).syncBeforeInvoke();");

            // Invoke body
            String varReaderAttr = getAttributeValue("varReader");
            String varAttr = getAttributeValue("var");
            if (varReaderAttr != null || varAttr != null) {
                writer.println("_jspx_sout = new java.io.StringWriter();");
            }
            else {
                writer.println("_jspx_sout = null;");
            }
            writer.println("if (getJspBody() != null) getJspBody().invoke(_jspx_sout);");

            // Store varReader in appropriate scope
            if (varReaderAttr != null || varAttr != null) {
                String scopeName = getAttributeValue("scope");
                //PK65013 - start
                String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
                if (isTagFile && jspOptions.isModifyPageContextVariable()) {
                    pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
                }
                //PK65013 - end
                writer.print(pageContextVar+".setAttribute("); //PK65013
                if (varReaderAttr != null) {
                    writer.print(GeneratorUtils.quote(varReaderAttr));
                    writer.print(", new java.io.StringReader(_jspx_sout.toString())");
                }
                else {
                    writer.print(GeneratorUtils.quote(varAttr));
                    writer.print(", _jspx_sout.toString()");
                }
                if (scopeName != null) {
                    writer.print(", ");
                    writer.print(getScopeConstant(scopeName));
                }
                writer.print(");");
                writer.println();
            }
            // Restore EL context
            writer.println("jspContext.getELContext().putContext(JspContext.class,getJspContext());"); // 393110
            writeDebugStartEnd(writer);
            
            writer.printMultiLn(bodyWriter.toString());
        }
    }
    
    public JavaCodeWriter getWriterForChild(int section, Node childElement) throws JspCoreException {
        JavaCodeWriter writerForChild = super.getWriterForChild(section, childElement);
        if (writerForChild == null) {
            if (section == CodeGenerationPhase.METHOD_SECTION) {
                writerForChild = bodyWriter;
            }
        }
        
        return writerForChild;
    }
    
    private String getScopeConstant(String scope) {
        String scopeName = "PageContext.PAGE_SCOPE"; // Default to page

        if ("request".equals(scope)) {
            scopeName = "PageContext.REQUEST_SCOPE";
        }
        else if ("session".equals(scope)) {
            scopeName = "PageContext.SESSION_SCOPE";
        }
        else if ("application".equals(scope)) {
            scopeName = "PageContext.APPLICATION_SCOPE";
        }

        return scopeName;
    }
}
