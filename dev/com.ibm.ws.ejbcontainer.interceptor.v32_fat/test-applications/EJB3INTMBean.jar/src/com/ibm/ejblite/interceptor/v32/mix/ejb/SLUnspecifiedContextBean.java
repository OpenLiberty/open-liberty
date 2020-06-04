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

import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static javax.ejb.TransactionManagementType.CONTAINER;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionManagement;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.interceptor.Interceptors;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * SLUnspecifiedContextBean is a SLSB that contains a method for each of the lifecycle
 * callback event annotation. Each lifecycle callback interceptor method determines if
 * there is any transaction context when it is called and adds that result to the Results
 * bean for recording test results by calling the addTransactionContext method on the
 * Results bean. The string added to results is of the form
 * <p>
 * class_name.method_name:boolean
 * <p>
 * The boolean is true if the lifecycle method is executing without any transaction context
 * and false if there is a transaction context.
 */
@Stateless
@Local({ SLUnspecifiedLocal.class })
@TransactionManagement(CONTAINER)
@ExcludeDefaultInterceptors
@Interceptors({ UnspecifiedInterceptor.class })
public class SLUnspecifiedContextBean implements SLUnspecifiedLocal {
    private static final String LOGGER_CLASS_NAME = SLUnspecifiedContextBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(LOGGER_CLASS_NAME);

    /** Name of this class */
    private static final String CLASS_NAME = "SLUnspecifiedContextBean";

    @SuppressWarnings("unused")
    @PostConstruct
    private void postConstruct() {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            boolean unspecifiedTX = FATTransactionHelper.isUnspecifiedTransactionContext();
            results.addTransactionContext(CLASS_NAME, "postConstruct", unspecifiedTX);
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    @SuppressWarnings("unused")
    @PreDestroy
    private void preDestroy() {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            boolean unspecifiedTX = FATTransactionHelper.isUnspecifiedTransactionContext();
            results.addTransactionContext(CLASS_NAME, "preDestroy", unspecifiedTX);
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    @SuppressWarnings("unused")
    @PrePassivate
    @PostActivate
    /**
     * Throw EJBException since PostActivate or PrePassivate
     * should never be called on a SLSB.
     */
    private void postActivateOrPrePassivate() {
        throw new EJBException(CLASS_NAME + "Unexpected call to postActivateOrPrePassivate.");
    }

    /**
     * Throws RuntimeException to force EJB container to discard this SLSB instance.
     */
    @Override
    @TransactionAttribute(REQUIRED)
    public void discard() {
        svLogger.info(CLASS_NAME + ".discard throwing RuntimeException");
        throw new RuntimeException("to force discard of SLUnspecifiedContextBean");
    }

    @Override
    @TransactionAttribute(NOT_SUPPORTED)
    public boolean doNothing() {
        svLogger.info(CLASS_NAME + ".doNothing");
        return FATTransactionHelper.isTransactionLocal();
    }

    @Override
    @TransactionAttribute(REQUIRED)
    public boolean doNothing(SLUnspecifiedLocal bean) {
        svLogger.info(CLASS_NAME + ".doNothing( bean) ");
        bean.doNothing();
        return FATTransactionHelper.isTransactionGlobal();
    }

}
