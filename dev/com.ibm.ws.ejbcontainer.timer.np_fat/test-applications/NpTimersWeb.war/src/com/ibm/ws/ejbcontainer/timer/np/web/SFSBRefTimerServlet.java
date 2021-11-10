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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Timer;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.timer.np.ejb.SimpleSFSBLocal;

@WebServlet("/SFSBRefTimerServlet")
@SuppressWarnings("serial")
public class SFSBRefTimerServlet extends AbstractServlet {
    private final static String CLASSNAME = SFSBRefTimerServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    @EJB(beanInterface = SimpleSFSBLocal.class)
    private SimpleSFSBLocal ivBean;

    @Override
    protected void clearAllTimers() {
        if (ivBean != null) {
            ivBean.clearAllTimers();
        }
    }

    /**
     * This test case creates an SFSB and then creates timers to multiple timer
     * SLSBs which are referenced in the SFSB. The SFSB is then passivated and
     * re-activated.
     * <br/>
     * This test passes the test if the timers referenced in the SFSB are still
     * active and correct after passivation/activation.
     */
    @Test
    public void testTimerReferencesSurvivePassivation() {
        final String method = "testTimerReferencesSurvivePassivation";

        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, method, "creating 4 timers via SFSB");
        }

        // create a bunch of timers
        Timer timer1 = ivBean.createTimer("timer1");
        Timer timer2 = ivBean.createTimer("timer2");
        Timer timer3 = ivBean.createTimer("timer3");
        Timer timer4 = ivBean.createTimer("timer4");
        assertEquals("Problem occurred while creating timers", 4, ivBean.getAllTimers().size());

        // for the heck of it, cancel one of them - this will also verify
        // passivation occurred
        ivBean.resetPassivationFlag();
        ivBean.cancelTimer("timer3");
        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, method, "canceled 1 timer via SFSB, leaving 3");
        }
        assertTrue("Did not passivate", ivBean.hasBeenPassivated());

        // verify that the timer collection and current timer are correct
        Timer curTimer = ivBean.getCurrentTimer();
        Collection<Timer> timers = ivBean.getAllTimers();
        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, method, "SFSB.ivCurrentTimer == " + curTimer);
            svLogger.logp(Level.FINEST, CLASSNAME, method, "Timer collection returned from SFSB after passivation:");
            for (Timer t : timers) {
                svLogger.logp(Level.FINEST, CLASSNAME, method, "\ttimer: {0}", new Object[] { t });
            }
        }
        assertEquals("SFSB Current timer is incorrect", timer4, curTimer);

        assertEquals("Unexpected number of timers in SFSB timer collection.", 3, timers.size());
        assertTrue("SFSB did not maintain reference to timer1.", timers.contains(timer1));
        assertTrue("SFSB did not maintain reference to timer2.", timers.contains(timer2));
        assertFalse("SFSB maintained reference to timer3, when it should have been removed.",
                    timers.contains(timer3));
        assertTrue("SFSB did not maintain reference to timer4.", timers.contains(timer4));
    }

    /**
     * This test case creates an SFSB and then creates timers to multiple timer
     * SLSBs which are referenced in the SFSB. The SFSB is then passivated and
     * re-activated.
     * <br/>
     * This test passes the test if the timers referenced in the SFSB are still
     * active and correct after passivation/activation.
     * NOTE: this method is the same as above but makes no checks based on the
     * SFSB's ivTimer's field (which is a Collection<Timer>
     */
    @Test
    public void testTimerReferencesSurvivePassivationNoCollections() {
        final String method = "testTimerReferencesSurvivePassivation";

        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, method, "creating 4 timers via SFSB");
        }

        // create a bunch of timers
        ivBean.createTimer("timer1");
        ivBean.createTimer("timer2");
        ivBean.createTimer("timer3");
        Timer timer4 = ivBean.createTimer("timer4");
        assertEquals("Problem occurred while creating timers", 4, ivBean.getAllTimers().size());

        // for the heck of it, cancel one of them - this will also verify
        // passivation occurred
        ivBean.resetPassivationFlag();
        ivBean.cancelTimer("timer3");
        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, method, "canceled 1 timer via SFSB, leaving 3");
        }
        assertTrue("Did not passivate", ivBean.hasBeenPassivated());

        // verify that the timer collection and current timer are correct
        Timer curTimer = ivBean.getCurrentTimer();
        if (svLogger.isLoggable(Level.FINEST)) {
            svLogger.logp(Level.FINEST, CLASSNAME, method, "SFSB.ivCurrentTimer == " + curTimer);
            svLogger.logp(Level.FINEST, CLASSNAME, method, "Timer collection returned from SFSB after passivation:");
        }
        assertEquals("SFSB Current timer is incorrect", timer4, curTimer);
        assertEquals("SFSB Current timer has incorrect info", "timer4", ivBean.getCurrentTimerInfo());
    }
}
