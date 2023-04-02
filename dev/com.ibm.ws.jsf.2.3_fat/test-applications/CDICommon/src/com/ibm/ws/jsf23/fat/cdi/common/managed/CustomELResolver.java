/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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

import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.FieldBean;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.ManagedBeanType;
import com.ibm.ws.jsf23.fat.cdi.common.beans.injected.MethodBean;
import com.ibm.ws.jsf23.fat.cdi.common.beans.jsf23.TestCustomBean;

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

        boolean className40Found = false;
        String outcome = null;
        String className = "com.ibm.ws.jsf23.fat.cdi.common.beans.jsf23.TestCustomBean";
        String classNameFaces40 = "com.ibm.ws.jsf23.fat.cdi.common.beans.faces40.TestCustomBean";

        if (base != null) {
            String baseClassName = base.getClass().getName();
            className40Found = baseClassName.equals(classNameFaces40);

            if (baseClassName.equals(className) || className40Found) {
                //System.out.println("CustomELResolver:getValue() match found");

                outcome = ":CustomELResolver:";
                outcome += _postConstruct;
                if (_fieldBean != null) {
                    outcome += _fieldBean.getData();
                } else {
                    outcome += ":FieldInjectionFailed:";
                }

                if (_methodBean == null) {
                    outcome += ":MethodInjectionFailed:";
                } else {
                    outcome += _methodBean.getData();
                }

                if (className40Found) {
                    ((com.ibm.ws.jsf23.fat.cdi.common.beans.faces40.TestCustomBean) base).setData(outcome);
                } else {
                    ((TestCustomBean) base).setData(outcome);
                }

                if (className40Found) {
                    outcome = ((com.ibm.ws.jsf23.fat.cdi.common.beans.faces40.TestCustomBean) base).getData();
                } else {
                    outcome = ((TestCustomBean) base).getData();
                }

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
