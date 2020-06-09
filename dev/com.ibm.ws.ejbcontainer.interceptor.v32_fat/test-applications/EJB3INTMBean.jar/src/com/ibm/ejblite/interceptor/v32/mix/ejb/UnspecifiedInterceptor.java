/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejblite.interceptor.v32.mix.ejb;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.interceptor.InvocationContext;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * UnspecifiedInterceptor is an interceptor class that contains a method for each of
 * the lifecycle callback event annotation. Each lifecycle callback interceptor method
 * determines if there is any transaction context when it is called and adds that result
 * to the Results bean for recording test results by calling the addTransactionContext
 * method on the Results bean. The string added to results is of the form
 * <p>
 * class_name.method_name:boolean
 * <p>
 * The boolean is true if the lifecycle method is executing without any transaction
 * context and false if there is a transaction context.
 */
public class UnspecifiedInterceptor implements Serializable {
    private static final long serialVersionUID = -4411554629906496862L;

    /** Name of this class */
    private static final String CLASS_NAME = "UnspecifiedInterceptor";

    @PostConstruct
    private void postConstruct(InvocationContext inv) {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            boolean unspecifiedTX = FATTransactionHelper.isUnspecifiedTransactionContext();
            results.addTransactionContext(CLASS_NAME, "postConstruct", unspecifiedTX);
            inv.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    @PreDestroy
    void preDestroy(InvocationContext inv) {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            boolean unspecifiedTX = FATTransactionHelper.isUnspecifiedTransactionContext();
            results.addTransactionContext(CLASS_NAME, "preDestroy", unspecifiedTX);
            inv.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    @PostActivate
    protected void postActivate(InvocationContext inv) {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            boolean unspecifiedTX = FATTransactionHelper.isUnspecifiedTransactionContext();
            results.addTransactionContext(CLASS_NAME, "postActivate", unspecifiedTX);
            inv.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    @PrePassivate
    public void prePassivate(InvocationContext inv) {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            boolean unspecifiedTX = FATTransactionHelper.isUnspecifiedTransactionContext();
            results.addTransactionContext(CLASS_NAME, "prePassivate", unspecifiedTX);
            inv.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }
}
