/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.backwards.utilities;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.MethodExpressionActionListener;
import javax.faces.event.MethodExpressionValueChangeListener;
import javax.faces.event.ValueChangeEvent;

import org.apache.jasper.el.ELContextImpl;

import com.ibm.ws.jsf22.fat.backwards.beans.MethodExpressionBean;

/**
 * Helper class to invoke valueChangeListener and processAction methods
 * from MethodExpressionValueChangeListener and MethodExpressionActionListener respectively.
 */
public class MethodExpressionHelper {

    private final FacesContext facesContext = FacesContext.getCurrentInstance();

    public MethodExpressionHelper() {}

    public void testProcessValueChange(ValueChangeEvent event, String varMapperName, String expression) throws AbortProcessingException {
        ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
        ELContext elContext = new ELContextImpl(factory);

        MethodExpressionBean meb = new MethodExpressionBean();
        ValueExpression mebVE = factory.createValueExpression(meb, MethodExpressionBean.class);
        elContext.getVariableMapper().setVariable(varMapperName, mebVE);

        MethodExpression myMethodExpression = factory.createMethodExpression(elContext, expression, Void.class, new Class<?>[] { String.class });

        MethodExpressionValueChangeListener meValueChangeListener = new MethodExpressionValueChangeListener(myMethodExpression);
        meValueChangeListener.processValueChange(event);
    }

    public void testProcessAction(ActionEvent event, String varMapperName, String expression) throws AbortProcessingException {
        ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
        ELContext elContext = new ELContextImpl(factory);

        MethodExpressionBean meb = new MethodExpressionBean();
        ValueExpression mebVE = factory.createValueExpression(meb, MethodExpressionBean.class);
        elContext.getVariableMapper().setVariable(varMapperName, mebVE);

        MethodExpression myMethodExpression = factory.createMethodExpression(elContext, expression, Void.class, new Class<?>[] { String.class });

        MethodExpressionActionListener meActionListener = new MethodExpressionActionListener(myMethodExpression);
        meActionListener.processAction(event);
    }

    public void testNullPointerException() throws AbortProcessingException {
        throw new NullPointerException();
    }

}
