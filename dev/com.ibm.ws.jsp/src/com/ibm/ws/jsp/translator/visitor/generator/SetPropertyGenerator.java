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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Attr;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.translator.utils.JspTranslatorUtil;

public class SetPropertyGenerator extends PageTranslationTimeGenerator {

	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.visitor.generator.SetPropertyGenerator";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}
    public SetPropertyGenerator() {
        super(new String[] {"name", "property", "param"});
    }
    
    public void startGeneration(int section, JavaCodeWriter writer) throws JspCoreException {}

    public void endGeneration(int section, JavaCodeWriter writer)  throws JspCoreException {
		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
			logger.logp(Level.FINEST, CLASS_NAME, "endGeneration","section = ["+section+"]");
		}
        //PK65013 start
        String pageContextVar = Constants.JSP_PAGE_CONTEXT_ORIG;
        if (isTagFile && jspOptions.isModifyPageContextVariable()) {
            pageContextVar = Constants.JSP_PAGE_CONTEXT_NEW;
        }
        //PK65013 - end
        if (section == CodeGenerationPhase.METHOD_SECTION) {
            String name = getAttributeValue("name");
            String property = getAttributeValue("property");
            String param = getAttributeValue("param");
            Attr valueAttr = element.getAttributeNode("value");
            String value = null;
            if (valueAttr != null) {
                value = valueAttr.getValue();
            }
            String expressionValue = null;
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
    			logger.logp(Level.FINEST, CLASS_NAME, "endGeneration","name = ["+name+"]");
    			logger.logp(Level.FINEST, CLASS_NAME, "endGeneration","property = ["+property+"]");
    			logger.logp(Level.FINEST, CLASS_NAME, "endGeneration","param = ["+param+"]");
    			logger.logp(Level.FINEST, CLASS_NAME, "endGeneration","valueAttr = ["+valueAttr+"]");
    			logger.logp(Level.FINEST, CLASS_NAME, "endGeneration","value = ["+value+"]");
    		}
            
            if (value == null) {
                HashMap jspAttributes = (HashMap)persistentData.get("jspAttributes");
                if (jspAttributes != null) {
                    ArrayList jspAttributeList = (ArrayList)jspAttributes.get(element);
            
                    for (Iterator itr = jspAttributeList.iterator(); itr.hasNext();) {
                        AttributeGenerator.JspAttribute jspAttribute = (AttributeGenerator.JspAttribute)itr.next();
                        if (jspAttribute.getName().equals("value")) {
                            value = expressionValue = jspAttribute.getVarName();
                        }
                    }
                }
            }
            else {
                expressionValue = GeneratorUtils.attributeValue(value, false, String.class, jspConfiguration, isTagFile, pageContextVar); //PK65013
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
        			logger.logp(Level.FINEST, CLASS_NAME, "endGeneration","expressionValue = ["+expressionValue+"]");
        		}
            }

            writeDebugStartBegin(writer);            
            if ("*".equals(property)) {
                //PK65013
                writer.println(
                    "org.apache.jasper.runtime.JspRuntimeLibrary.introspect("
                        + pageContextVar+".findAttribute("
                        + "\""
                        + name
                        + "\"), request);");
            }
            else if (value == null) {
                if (param == null)
                    param = property; // default to same as property
                //PK65013 change pageContext variable to customizable one.
                writer.println(
                    "org.apache.jasper.runtime.JspRuntimeLibrary.introspecthelper("
                        + pageContextVar+".findAttribute(\""
                        + name
                        + "\"), \""
                        + property
                        + "\", request.getParameter(\""
                        + param
                        + "\"), "
                        + "request, \""
                        + param
                        + "\", false);");
            }
            else if (JspTranslatorUtil.isExpression(value)) {
                //PK65013 change pageContext variable to customizable one.
                writer.println(
                    "org.apache.jasper.runtime.JspRuntimeLibrary.handleSetProperty("
                        + pageContextVar+".findAttribute(\""
                        + name
                        + "\"), \""
                        + property
                        + "\",");
                writer.print(expressionValue);
                writer.println(");");
            }
            else if (JspTranslatorUtil.isELInterpreterInput(value, jspConfiguration)) {
        		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
        			logger.logp(Level.FINEST, CLASS_NAME, "endGeneration","after call to isELInterpreterInput");
        		}
                // We've got to resolve the very call to the interpreter
                // at runtime since we don't know what type to expect
                // in the general case; we thus can't hard-wire the call
                // into the generated code.  (XXX We could, however,
                // optimize the case where the bean is exposed with
                // <jsp:useBean>, much as the code here does for
                // getProperty.)

                // The following holds true for the arguments passed to
                // JspRuntimeLibrary.handleSetPropertyExpression():
                // - 'pageContext' is a VariableResolver.
                // - 'this' (either the generated Servlet or the generated tag
                //   handler for Tag files) is a FunctionMapper.
                
                //PK65013 change pageContext variable to customizable one.
        		writer.println(
                    "org.apache.jasper.runtime.JspRuntimeLibrary.handleSetPropertyExpression("
                        + pageContextVar+".findAttribute(\""
                        + name
                        + "\"), \""
                        + property
                        + "\", "
                        + GeneratorUtils.quote(value)
                        + ", "
                        + pageContextVar+", _jspx_fnmap);");
                /*
                                    + "(javax.servlet.jsp.el.VariableResolver) pageContext, "
                                    + "(javax.servlet.jsp.el.FunctionMapper) this );");
                */
            }
            else {
                //PK65013
                writer.println(
                    "org.apache.jasper.runtime.JspRuntimeLibrary.introspecthelper("
                        + pageContextVar+".findAttribute(\""
                        + name
                        + "\"), \""
                        + property
                        + "\", "
                        + expressionValue
                        + ", null, null, false);");
            }
            writeDebugStartEnd(writer);            
        }
        
    }
}
