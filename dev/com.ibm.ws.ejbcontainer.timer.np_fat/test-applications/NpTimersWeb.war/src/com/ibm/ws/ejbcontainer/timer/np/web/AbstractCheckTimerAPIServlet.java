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

import javax.ejb.EJBException;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.timer.np.ejb.AnnotationTxLocal;
import com.ibm.ws.ejbcontainer.timer.np.ejb.AnnotationTxLocal.TstName;

@SuppressWarnings("serial")
public abstract class AbstractCheckTimerAPIServlet extends AbstractServlet {

    protected AnnotationTxLocal ivBean;

    /**
     * This test verifies the proper return values of the javax.ejb.Timer
     * methods as they pertain to non-persistent timers.
     */
    @Test
    public void testTimerAPI() {
        try {
            ivBean.executeTest(TstName.TEST_TIMER_API);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }
    }

    /**
     * This test verifies the proper return values of the javax.ejb.TimerService
     * methods as they pertain to non-persistent timers.
     */
    @Test
    public void testTimerServiceAPI() {
        try {
            ivBean.executeTest(TstName.TEST_TIMER_SERVICE_API);
        } catch (EJBException ex) {
            FATHelper.checkForAssertion(ex);
        }
    }

    protected abstract void setIVBean(AnnotationTxLocal bean);

}
