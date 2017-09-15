/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.visitor.validator;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.translator.visitor.JspVisitor;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.JspVisitorResult;
import com.ibm.ws.jsp.translator.visitor.configuration.JspVisitorUsage;
import com.ibm.wsspi.jsp.context.JspCoreContext;

public class TagCountVisitor extends JspVisitor {
    private Map countMap = null;
    private String id = null;
    private int count = 0; 
    
    public TagCountVisitor(JspVisitorUsage visitorUsage, 
            JspConfiguration jspConfiguration, 
            JspCoreContext context, 
            HashMap resultMap, 
            JspVisitorInputMap inputMap)  throws JspCoreException {
        super(visitorUsage, jspConfiguration, context, resultMap, inputMap);
        countMap = new HashMap();
        id = visitorUsage.getJspVisitorDefinition().getId();
    }

    public JspVisitorResult getResult() throws JspCoreException {
        return new TagCountResult(id, countMap);
    }
    
    protected void visitCustomTagEnd(Element jspElement) throws JspCoreException {
        countMap.put(jspElement, new Integer(count++)); //PK41783
    }
    
    protected void visitAttributeDirectiveEnd(Element jspElement) throws JspCoreException {}
    protected void visitAttributeDirectiveStart(Element jspElement) throws JspCoreException {}
    protected void visitCDataTag(CDATASection cdata) throws JspCoreException {}
    protected void visitCustomTagStart(Element jspElement) throws JspCoreException {}
    protected void visitIncludeDirectiveEnd(Element jspElement) throws JspCoreException {}
    protected void visitIncludeDirectiveStart(Element jspElement) throws JspCoreException {}
    protected void visitJspAttributeEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspAttributeStart(Element jspElement) throws JspCoreException {}
    protected void visitJspBodyEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspBodyStart(Element jspElement) throws JspCoreException {}
    protected void visitJspDeclarationEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspDeclarationStart(Element jspElement) throws JspCoreException {}
    protected void visitJspDoBodyEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspDoBodyStart(Element jspElement) throws JspCoreException {}
    protected void visitJspElementEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspElementStart(Element jspElement) throws JspCoreException {}
    protected void visitJspExpressionEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspExpressionStart(Element jspElement) throws JspCoreException {}
    protected void visitJspFallbackEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspFallbackStart(Element jspElement) throws JspCoreException {}
    protected void visitJspForwardEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspForwardStart(Element jspElement) throws JspCoreException {}
    protected void visitJspGetPropertyEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspGetPropertyStart(Element jspElement) throws JspCoreException {}
    protected void visitJspIncludeEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspIncludeStart(Element jspElement) throws JspCoreException {}
    protected void visitJspInvokeEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspInvokeStart(Element jspElement) throws JspCoreException {}
    protected void visitJspOutputEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspOutputStart(Element jspElement) throws JspCoreException {}
    protected void visitJspParamEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspParamsEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspParamsStart(Element jspElement) throws JspCoreException {}
    protected void visitJspParamStart(Element jspElement) throws JspCoreException {}
    protected void visitJspPluginEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspPluginStart(Element jspElement) throws JspCoreException {}
    protected void visitJspRootEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspRootStart(Element jspElement) throws JspCoreException {}
    protected void visitJspScriptletEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspScriptletStart(Element jspElement) throws JspCoreException {}
    protected void visitJspSetPropertyEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspSetPropertyStart(Element jspElement) throws JspCoreException {}
    protected void visitJspTextEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspTextStart(Element jspElement) throws JspCoreException {}
    protected void visitJspUseBeanEnd(Element jspElement) throws JspCoreException {}
    protected void visitJspUseBeanStart(Element jspElement) throws JspCoreException {}
    protected void visitPageDirectiveEnd(Element jspElement) throws JspCoreException {}
    protected void visitPageDirectiveStart(Element jspElement) throws JspCoreException {}
    protected void visitTagDirectiveEnd(Element jspElement) throws JspCoreException {}
    protected void visitTagDirectiveStart(Element jspElement) throws JspCoreException {}
    protected void visitUninterpretedTagEnd(Element jspElement) throws JspCoreException {}
    protected void visitUninterpretedTagStart(Element jspElement) throws JspCoreException {}
    protected void visitVariableDirectiveEnd(Element jspElement) throws JspCoreException {}
    protected void visitVariableDirectiveStart(Element jspElement) throws JspCoreException {}
}
