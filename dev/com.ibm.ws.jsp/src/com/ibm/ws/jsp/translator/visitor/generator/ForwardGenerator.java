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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.translator.utils.JspTranslatorUtil;

public class ForwardGenerator extends CodeGeneratorBase {
    
    public void startGeneration(int section, JavaCodeWriter writer) throws JspCoreException {
    }

    public void endGeneration(int section, JavaCodeWriter writer)  throws JspCoreException {
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            boolean isFragment = false;
            if (writer instanceof FragmentHelperClassWriter.FragmentWriter)
                isFragment = true;
                
            boolean isLiteral = true;
            String page = element.getAttribute("page");
            if (JspTranslatorUtil.isExpression(page) || 
                JspTranslatorUtil.isELInterpreterInput(page, jspConfiguration))
                isLiteral = false;
            
            //PK65013 - start
            String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
            if (isTagFile && jspOptions.isModifyPageContextVariable()) {
            	pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
            }
            //PK65013 - end
            
            if (page.equals("")) {
                HashMap jspAttributes = (HashMap)persistentData.get("jspAttributes");
                if (jspAttributes != null) {
                    ArrayList jspAttributeList = (ArrayList)jspAttributes.get(element);
            
                    for (Iterator itr = jspAttributeList.iterator(); itr.hasNext();) {
                        AttributeGenerator.JspAttribute jspAttribute = (AttributeGenerator.JspAttribute)itr.next();
                        if (jspAttribute.getName().equals("page")) {
                            page = jspAttribute.getVarName();
                            isLiteral = false;
                            break;
                        }
                    }
                }
            }
            else {
                page = GeneratorUtils.attributeValue(page, false, String.class, jspConfiguration, isTagFile, pageContextVar); //PK65013
            }

            writeDebugStartBegin(writer);            
            writer.println("if (true) {");
            writer.print(pageContextVar+".forward("+page); //PK65013
            
            HashMap jspParams = (HashMap)persistentData.get("jspParams");
            if (jspParams != null) {
                ArrayList jspParamList = (ArrayList)jspParams.get(element);
                if (jspParamList != null) {
                    String separator;
                    if (isLiteral) 
                        separator = page.indexOf('?') > 0 ? "\"&\"" : "\"?\"";
                    else 
                        separator = "((" + page + ").indexOf('?')>0? '&': '?')";
                        
                    for (Iterator itr = jspParamList.iterator(); itr.hasNext();) {
                        ParamGenerator.JspParam jspParam = (ParamGenerator.JspParam)itr.next();
                        writer.print(" + ");
                        writer.print(separator);
                        writer.print(" + ");
                        writer.print(jspParam.getName());
                        writer.print("+ \"=\" + ");
                        writer.print(jspParam.getValue());
                        separator = "\"&\"";
                    }
                }
            }

            writer.print(");");
            writer.println();
            if (isTagFile || isFragment) {
            	// begin 242714: enhance error reporting for SkipPageException.
                //writer.println("throw new javax.servlet.jsp.SkipPageException();");
            	writer.println("throw new com.ibm.ws.jsp.runtime.WsSkipPageException(\"Tag file or fragment returned from forward to [" + page.replace ('\"', '\'')+"]. Remainder of page not evaluated\");");
            	// end 242714: enhance error reporting for SkipPageException.
            }
            else {
                if (persistentData.get("methodNesting") == null) {
                    persistentData.put("methodNesting", new Integer(0));
                }
                int methodNesting =  ((Integer)persistentData.get("methodNesting")).intValue();
                writer.println((methodNesting > 0) ? "return true;" : "return;");
            }
            writer.println("}");
            writeDebugStartEnd(writer);
        }
    }
}
