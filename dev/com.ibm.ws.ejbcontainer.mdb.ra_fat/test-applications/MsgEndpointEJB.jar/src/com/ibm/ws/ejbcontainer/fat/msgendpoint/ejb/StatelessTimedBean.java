/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.fat.msgendpoint.ejb;

import java.io.Serializable;

import javax.annotation.Resource;
import javax.ejb.CreateException;
import javax.ejb.SessionContext;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;

public class StatelessTimedBean {
    @Resource
    private SessionContext ivContext;

    /** Required default constructor. **/
    public StatelessTimedBean() {
    }

    public void setSessionContext(SessionContext sc) {
        ivContext = sc;
    }

    public void ejbCreate() throws CreateException {
    }

    public void ejbRemove() {
    }

    /** Never called for Stateless Session Bean. **/
    public void ejbActivate() {
    }

    /** Never called for Stateless Session Bean. **/
    public void ejbPassivate() {
    }

    @Timeout
    public void timeout(Timer timer) {
    }

    /**
     * Utility method that may be used to create a Timer when a Timer is
     * required to perform a test, but cannot be created directly by
     * the bean performing the test. For example, if the bean performing
     * the test does not implement the TimedObject interface. <p>
     *
     * Local interface only! <p>
     *
     * Used by test : {@link TimerMDBOperationsTest#test01} <p>
     *
     * @param info info parameter passed through to the createTimer call
     *
     * @return Timer created with 1 minute duration and specified info.
     **/
    public Timer createTimer(Serializable info) {
        TimerConfig tCfg = new TimerConfig();
        tCfg.setInfo(info);
        tCfg.setPersistent(false);
        Timer timer = ivContext.getTimerService().createSingleActionTimer(60000, tCfg);
        return timer;
    }
}