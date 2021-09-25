/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.web;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.ejbcontainer.timer.np.ejb.AnnotationTxLocal;
import com.ibm.ws.ejbcontainer.timer.np.ejb.SingletonAnnotationTxLocal;

import componenttest.custom.junit.runner.Mode;

/**
 * This test case class contains tests that verify the proper behavior of
 * non-persistent timers defined for a singleton session bean using only
 * annotations - no XML, not implementing TimedObject.
 */
@WebServlet("/SingletonAnnotationTxImplServlet")
@SuppressWarnings("serial")
@Mode(Mode.TestMode.FULL) // Stateless version is LITE
public class SingletonAnnotationTxImplServlet extends AbstractAnnotationTxServlet {

    @Override
    @EJB(beanInterface = SingletonAnnotationTxLocal.class)
    protected void setIVBean(AnnotationTxLocal bean) {
        ivBean = bean;
    }

    @Override
    protected void clearAllTimers() {
        if (ivBean != null) {
            ivBean.clearAllTimers();
        }
    }
}
