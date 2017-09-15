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
package com.ibm.ws.jsp.translator.visitor.validator;

//Created for feature LIDB4147-9 "Integrate Unified Expression Language"  2006/08/14  Scott Johnson

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.jsp.tagext.FunctionInfo;

import org.apache.jasper.compiler.ELNode;
import org.w3c.dom.Element;

import com.ibm.ws.jsp.taglib.TagLibraryInfoImpl;
import com.ibm.ws.jsp.translator.JspTranslationException;

class FVVisitor extends ELNode.Visitor {
	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.visitor.validator.ELValidator";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}

    ValidateResult result;
    Element jspElement;
    HashMap prefixToUriMap;

    FVVisitor(Element jspElement, ValidateResult result, HashMap prefixToUriMap) {
        this.jspElement = jspElement;
        this.result=result;
        this.prefixToUriMap = prefixToUriMap;
    }

    public void visit(ELNode.Function func) throws JspTranslationException {
        String prefix = func.getPrefix();
        String function = func.getName();
        String uri = null;

        if (prefix!=null && prefixToUriMap!=null) {                 
            uri = (String) prefixToUriMap.get(prefix);
            if (uri == null) {
                uri = jspElement.getNamespaceURI();  // 245645.1
            }

            if (uri == null) {
                if (prefix == null) {
                    throw new JspTranslationException("jsp.error.noFunctionPrefix " + function);					
                } else {
                    throw new JspTranslationException("jsp.error.attribute.invalidPrefix " + prefix);					
                }
            }
            TagLibraryInfoImpl taglib = (TagLibraryInfoImpl) result.getTagLibMap().get(uri);
            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                logger.logp(Level.FINER, CLASS_NAME, "FVVisitor.visit","uri= ["+uri+"]");
                logger.logp(Level.FINER, CLASS_NAME, "FVVisitor.visit","taglib= ["+taglib+"]");
            }
            if (taglib == null)  // 245645.1
                throw new JspTranslationException("jsp.error.el.function.not.found", new Object[] { function, uri });  // 245645.1
            FunctionInfo funcInfo = null;
            funcInfo = taglib.getFunction(function);
            if (funcInfo == null) {
                throw new JspTranslationException("jsp.error.el.function.not.found", new Object[] { function, uri });
            }
            // Skip TLD function uniqueness check. Done by Schema ?
            func.setUri(uri);
            func.setFunctionInfo(funcInfo);
            processSignature(func);
        }
    }
    private void processSignature(ELNode.Function func)
    	throws JspTranslationException {
		func.setMethodName(getMethod(func));
		func.setParameters(getParameters(func));
	}
    /**
     * Get the method name from the signature.
     */
    private String getMethod(ELNode.Function func) throws JspTranslationException {
        FunctionInfo funcInfo = func.getFunctionInfo();
        String signature = funcInfo.getFunctionSignature();

        int start = signature.indexOf(' ');
        if (start < 0) {
            throw new JspTranslationException("jsp.error.tld.fn.invalid.signature " + func.getPrefix()+" "+ func.getName());					
        }
        int end = signature.indexOf('(');
        if (end < 0) {
            throw new JspTranslationException("jsp.error.tld.fn.invalid.signature.parenexpected " + func.getPrefix()+" "+ func.getName());					
        }
        return signature.substring(start + 1, end).trim();
    }

    /**
     * Get the parameters types from the function signature.
     * 
     * @return An array of parameter class names
     */
    private String[] getParameters(ELNode.Function func)
            throws JspTranslationException {
        FunctionInfo funcInfo = func.getFunctionInfo();
        String signature = funcInfo.getFunctionSignature();
        ArrayList params = new ArrayList();
        // Signature is of the form
        // <return-type> S <method-name S? '('
        // < <arg-type> ( ',' <arg-type> )* )? ')'
        int start = signature.indexOf('(') + 1;
        boolean lastArg = false;
        while (true) {
            int p = signature.indexOf(',', start);
            if (p < 0) {
                p = signature.indexOf(')', start);
                if (p < 0) {
	                throw new JspTranslationException("jsp.error.tld.fn.invalid.signature " + func.getPrefix()+" "+ func.getName());					
                }
                lastArg = true;
            }
            String arg = signature.substring(start, p).trim();
            if (!"".equals(arg)) {
                params.add(arg);
            }
            if (lastArg) {
                break;
            }
            start = p + 1;
        }
        return (String[]) params.toArray(new String[params.size()]);
    }
}
