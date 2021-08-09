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
package com.ibm.ejblite.interceptor.v32.xml.ejb;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.ejb.EJBException;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * SFUnspecifiedContextBean is a SFSB that contains a method for each of the
 * lifecycle callback event annotation. Each lifecycle callback interceptor
 * method determines if there is any transaction context when it is called and
 * adds that result to the Results bean for recording test results by calling the
 * addTransactionContext method on the Results bean. The string added to results is of
 * the form
 * <p>
 * class_name.method_name:boolean
 * <p>
 * The boolean is true if the lifecycle method is executing without any
 * transaction context and false if there is a transaction context.
 */
public class SFUnspecifiedContextBean implements SFUnspecifiedLocal, Serializable {

    /**
     * Definitions for the logger
     */
    private final static String CLASSNAME = SFUnspecifiedContextBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final long serialVersionUID = 8671561022850790328L;

    /** Name of this class */
    private static final String CLASS_NAME = "SFUnspecifiedContextBean";

    public void postConstruct() {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            boolean unspecifiedTX = FATTransactionHelper.isUnspecifiedTransactionContext();
            results.addTransactionContext(CLASS_NAME, "postConstruct",
                                          unspecifiedTX);
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    protected void preDestroy() {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            boolean unspecifiedTX = FATTransactionHelper.isUnspecifiedTransactionContext();
            results.addTransactionContext(CLASS_NAME, "preDestroy", unspecifiedTX);
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    void postActivate() {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            boolean unspecifiedTX = FATTransactionHelper.isUnspecifiedTransactionContext();
            results.addTransactionContext(CLASS_NAME, "postActivate",
                                          unspecifiedTX);
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    @SuppressWarnings("unused")
    private void prePassivate() {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            boolean unspecifiedTX = FATTransactionHelper.isUnspecifiedTransactionContext();
            results.addTransactionContext(CLASS_NAME, "prePassivate",
                                          unspecifiedTX);
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    /**
     * Throws RuntimeException to force EJB container to discard this SLSB
     * instance.
     */
    @Override
    public boolean destroy() {
        svLogger.info(CLASS_NAME + ".destroy");
        return FATTransactionHelper.isTransactionLocal();
    }

    /**
     * Throws RuntimeException to force EJB container to discard this SLSB
     * instance.
     */
    @Override
    public boolean remove() {
        svLogger.info(CLASS_NAME + ".remove");
        return FATTransactionHelper.isTransactionGlobal();
    }

    @Override
    public boolean doNothing() {
        svLogger.info(CLASS_NAME + ".doNothing");
        return FATTransactionHelper.isTransactionGlobal();
    }

}
