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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;

public class JSTLSetUtil {

    public static void setProperty(PageContext pageContext, Object target, Object result, String property) throws JspException {
        if (target instanceof Map) {
            if (result == null)
                 ((Map) target).remove(property);
            else
                 ((Map) target).put(property, result);
        }
        else {
            try {
                PropertyDescriptor pd[] = Introspector.getBeanInfo(target.getClass()).getPropertyDescriptors();
                boolean succeeded = false;
                for (int i = 0; i < pd.length; i++) {
                    if (pd[i].getName().equals(property)) {
                        Method m = pd[i].getWriteMethod();
                        if (m == null) {
                            throw new JspException("");
                        }
                        if (result != null) {
                            try {
                                m.invoke(target, new Object[] { convertToExpectedType(pageContext, result, m.getParameterTypes()[0])});
                            }
                            catch (javax.servlet.jsp.el.ELException ex) {
                                throw new JspTagException(ex);
                            }
                        }
                        else {
                            m.invoke(target, new Object[] { null });
                        }
                        succeeded = true;
                    }
                }
                if (!succeeded) {
                    throw new JspTagException("");
                }
            }
            catch (IllegalAccessException ex) {
                throw new JspException(ex);
            }
            catch (IntrospectionException ex) {
                throw new JspException(ex);
            }
            catch (InvocationTargetException ex) {
                throw new JspException(ex);
            }
        }
    }
    
    private static Object convertToExpectedType(PageContext pageContext, final Object value, Class expectedType )
    throws javax.servlet.jsp.el.ELException {
        ExpressionEvaluator evaluator = pageContext.getExpressionEvaluator();
        return evaluator.evaluate( "${result}", expectedType,
        new VariableResolver() {
            public Object resolveVariable( String pName )
            throws ELException {
                return value;
            }
        }, null );
    }
    
}
