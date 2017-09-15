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

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;

public class InvokeGenerator extends PageTranslationTimeGenerator {
    
    public InvokeGenerator() {
        super(new String[] {"fragment", "varReader", "var", "scope"});
    }
    
    
    public void startGeneration(int section, JavaCodeWriter writer) throws JspCoreException {}

    public void endGeneration(int section, JavaCodeWriter writer) throws JspCoreException {
        //PK65013 - start
        String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
        if (isTagFile && jspOptions.isModifyPageContextVariable()) {
            pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
        }
        //PK65013 - end
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            writeDebugStartBegin(writer);
            writer.println("((org.apache.jasper.runtime.JspContextWrapper) this.jspContext).syncBeforeInvoke();");

            // Invoke fragment
            String varReaderAttr = getAttributeValue("varReader");
            String varAttr = getAttributeValue("var");
            
            if (varReaderAttr != null || varAttr != null) {
                writer.println("_jspx_sout = new java.io.StringWriter();");
            }
            else {
                writer.println("_jspx_sout = null;");
            }
            writer.print("if (");
            writer.print(GeneratorUtils.toGetterMethod(getAttributeValue("fragment")));
            writer.println(" != null) {");
            writer.print(GeneratorUtils.toGetterMethod(getAttributeValue("fragment")));
            writer.print(".invoke(_jspx_sout);");
            writer.println("}");
            writer.println();     

            // Store varReader in appropriate scope
            if (varReaderAttr != null || varAttr != null) {
                String scopeName = getAttributeValue("scope");
                //PK65013 change pageContext variable to customizable one.
                writer.print(pageContextVar+".setAttribute(");
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
                writer.println(");");
            }
            // Restore EL context
            writer.println("jspContext.getELContext().putContext(JspContext.class,getJspContext());"); // 393110
            
            writeDebugStartEnd(writer);
        }
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
