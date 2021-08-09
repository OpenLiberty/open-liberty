/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp23.translator.visitor.validator;

import javax.el.ELException;
import javax.el.ExpressionFactory;

import org.apache.jasper.compiler.ELNode.Nodes;
import org.apache.jasper.el.ELContextImpl;
import org.w3c.dom.Element;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.translator.JspTranslationException;
import com.ibm.ws.jsp.translator.visitor.validator.ElValidatorExt;
import com.ibm.ws.jsp.translator.visitor.validator.ValidateResult;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;

public class ELValidatorExtImpl implements ElValidatorExt {

    public ELValidatorExtImpl() {}

    @Override
    public void validateElFunction(Nodes el, Element jspElement, String expr,
                                   ValidateResult result, ClassLoader loader, JspConfiguration jspConfiguration) throws JspCoreException {

        try {
            // Need the thread context classloader (TCCL) to be on an application class loader 
            // for jsp processing to find such things as the ExpressionFactory.
            // Therefore, set to passed in class loader if we are not using it already
            ClassLoader origCl = ThreadContextHelper.getContextClassLoader();
            if (loader != origCl) {
                ThreadContextHelper.setClassLoader(loader);
            } else {
                origCl = null;
            }
            ELContextImpl ctx = new ELContextImpl();
            ctx.setFunctionMapper(com.ibm.ws.jsp.translator.visitor.validator.ELValidator.getFunctionMapper(el, loader, result));
            com.ibm.ws.jsp.translator.visitor.validator.ELValidator.validateEL(jspConfiguration.getExpressionFactory(), ctx, el, expr);
            if (origCl != null) {
                // and back again if need be
                ThreadContextHelper.setClassLoader(origCl);
            }

        } catch (ELException e) {
            throw new JspTranslationException(jspElement, "jsp.error.el.function.cannot.parse", new Object[] { expr });
        }

    }

    @Override
    public void prepareExpression(Nodes el, String expr, ValidateResult result, ClassLoader loader, JspConfiguration jspConfiguration) throws JspTranslationException {

        ClassLoader origCl = ThreadContextHelper.getContextClassLoader();
        try {
            if (loader != origCl) {
                ThreadContextHelper.setClassLoader(loader);
            } else {
                origCl = null;
            }
            ELContextImpl ctx = new ELContextImpl();
            ctx.setFunctionMapper(com.ibm.ws.jsp.translator.visitor.validator.ELValidator.getFunctionMapper(el, loader, result));
            ExpressionFactory ef = jspConfiguration.getExpressionFactory();
            try {
                ef.createValueExpression(ctx, expr, Object.class);
            } catch (ELException e) {

            }
        } finally {
            if (origCl != null) {
                // and back again if need be
                ThreadContextHelper.setClassLoader(origCl);
            }
        }
    }
}
