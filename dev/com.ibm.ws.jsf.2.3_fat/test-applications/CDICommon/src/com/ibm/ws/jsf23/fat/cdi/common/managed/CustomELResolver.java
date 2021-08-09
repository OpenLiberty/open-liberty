/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.cdi.common.managed;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.inject.Inject;

import com.ibm.ws.jsf23.fat.cdi.common.beans.TestCustomBean;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.FieldBean;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.ManagedBeanType;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.MethodBean;

/**
 * Custom EL resolver that tests field and method injection. No constructor injection.
 */
public class CustomELResolver extends ELResolver {

    /*
     * (non-Javadoc)
     *
     * @see javax.el.ELResolver#getValue(javax.el.ELContext, java.lang.Object, java.lang.Object)
     */
    // Field Injected Bean
    @Inject
    @ManagedBeanType
    private FieldBean _fieldBean;
    //private final FieldBean _fieldBean = null;

    private MethodBean _methodBean = null;

    String _postConstruct = ":PostConstructNotCalled:";

    @PostConstruct
    public void start() {
        _postConstruct = ":PostConstructCalled:";
    }

    @PreDestroy
    public void stop() {
        System.out.println("CustomELResolver preDestroy called.");
    }

    //Method Injected bean
    @Inject
    public void setMethodBean(MethodBean bean) {
        _methodBean = bean;
    }

    /**
     * Looks to see if the value to be resolved is from the TestCustomBean. If it is the bean it is
     * updated with information which indicates whether or not the various injections have passed.
     */
    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        System.out.println("CustomELResolver:getValue() base = " + base + ", property = " + property);
        String outcome = null;
        if (base != null) {
            if (base != null && base.getClass().getName().equals("com.ibm.ws.jsf23.fat.cdi.common.beans.TestCustomBean")) {
                //System.out.println("CustomELResolver:getValue() match found");

                outcome = ":CustomELResolver:";
                outcome += _postConstruct;
                if (_fieldBean != null) {
                    outcome += _fieldBean.getData();
                } else {
                    outcome += ":FieldInjectionFailed:";
                }

                if (_methodBean == null)
                    outcome += ":MethodInjectionFailed:";
                else
                    outcome += _methodBean.getData();

                ((TestCustomBean) base).setData(outcome);

                outcome = ((TestCustomBean) base).getData();

                context.setPropertyResolved(true);
            }
        }
        return outcome;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.el.ELResolver#getType(javax.el.ELContext, java.lang.Object, java.lang.Object)
     */
    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        //System.out.println("CustomELResolver:getType: " + property.getClass());
        return String.class;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.el.ELResolver#setValue(javax.el.ELContext, java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        System.out.println("CustomELResolver:setValue: " + property.getClass());
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.el.ELResolver#isReadOnly(javax.el.ELContext, java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.el.ELResolver#getFeatureDescriptors(javax.el.ELContext, java.lang.Object)
     */
    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.el.ELResolver#getCommonPropertyType(javax.el.ELContext, java.lang.Object)
     */
    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return null;
    }

}
