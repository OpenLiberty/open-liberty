/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.app_exception.ann.ejb;

import static javax.ejb.TransactionAttributeType.MANDATORY;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;

@Stateless
@Local(RTExLocalInterface.class)
public class RTExceptionBean {

    private static final String CLASSNAME = RTExceptionBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASSNAME);

    @Resource
    private EJBContext ivContext;

    @EJB
    private RTExLocalInterface ivBean;

    @TransactionAttribute(REQUIRES_NEW)
    public ResultObject test(int i) {
        try {
            ivBean.throwException(i);
        } catch (Throwable t) {
            ResultObject r = new ResultObject(ivContext.getRollbackOnly(), t);
            svLogger.info("--> BEAN INFO: " + r);
            return r;
        }
        throw new Error("Did not throw an exception.");

    }

    @TransactionAttribute(MANDATORY)
    public void throwException(int i) {
        switch (i) {
            case 0:
                throw new RTExceptionA();
            case 1:
                throw new RTExceptionB();
            case 2:
                throw new RTExceptionC();
            case 3:
                throw new RTExceptionD();
            case 4:
                throw new RTExceptionDefaults();
            case 5:
                throw new RTExceptionDefaultsSub();
        }
    }
}
