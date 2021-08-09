/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.web.impl.el;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.cdi.web.factories.WeldListenerFactory;
import com.ibm.wsspi.el.ELFactoryWrapperForCDI;

/**
 *
 */
@Component(name = "com.ibm.ws.cdi.web.el.WrappedELExpressionFactory", service = { ELFactoryWrapperForCDI.class }, immediate = true, property = { "service.vendor=IBM" })
public class WrappedELExpressionFactory extends ExpressionFactory implements ELFactoryWrapperForCDI {
    private ExpressionFactory wrapped;

    @Override
    public void setExpressionFactory(ExpressionFactory expressionFactory) {
        //Constructing WeldExpressionFactory directly is not quite the right way to do this
        //the docs say you should call BeanManager.wrapExpressionFactory()
        //but we're struggling to get hold of the current bean manager at this point... so we're cheating a little!
        this.wrapped = WeldListenerFactory.newWeldExpressionFactory(expressionFactory);
    }

    @Override
    public Object coerceToType(Object obj, Class<?> expectedType) throws ELException {
        return this.wrapped.coerceToType(obj, expectedType);
    }

    @Override
    public ValueExpression createValueExpression(ELContext context, String expression, Class<?> expectedType) throws NullPointerException, ELException {
        return this.wrapped.createValueExpression(context, expression, expectedType);
    }

    @Override
    public ValueExpression createValueExpression(Object instance, Class<?> expectedType) {
        return this.wrapped.createValueExpression(instance, expectedType);
    }

    @Override
    public MethodExpression createMethodExpression(ELContext context, String expression, Class<?> expectedReturnType,
                                                   Class<?>[] expectedParamTypes) throws ELException, NullPointerException {
        return this.wrapped.createMethodExpression(context, expression, expectedReturnType, expectedParamTypes);
    }

}
