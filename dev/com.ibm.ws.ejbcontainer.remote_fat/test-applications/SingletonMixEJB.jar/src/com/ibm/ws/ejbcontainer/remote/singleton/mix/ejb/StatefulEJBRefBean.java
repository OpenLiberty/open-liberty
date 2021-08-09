/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.singleton.mix.ejb;

import static javax.ejb.TransactionAttributeType.NEVER;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.interceptor.ExcludeDefaultInterceptors;

import com.ibm.ws.ejbcontainer.remote.singleton.mix.shared.StatefulEJBRefLocal;
import com.ibm.ws.ejbcontainer.remote.singleton.mix.shared.StatefulEJBRefRemote;

@Local(StatefulEJBRefLocal.class)
@Remote(StatefulEJBRefRemote.class)
@Stateful
@ExcludeDefaultInterceptors
public class StatefulEJBRefBean {
    private static final String CLASSNAME = StatefulEJBRefBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private int passCount = 0;
    private int actCount = 0;

    @EJB(beanName = "PassieSingletonBean")
    PassieSingleton singleLocal;

    @EJB(beanName = "PassieSingletonBean")
    PassieSingletonRemote singleRemote;

    @PostConstruct
    public void create() {
        passCount = 0;
        actCount = 0;
    }

    @PrePassivate
    public void passivate() {
        svLogger.info("SFSB Passivating");
        passCount++;
    }

    @PostActivate
    public void activate() {
        svLogger.info("SFSB Activating");
        actCount++;
    }

    // This must be the first method called on the bean when testing local business ref
    public boolean testLocalSingleStart() {
        svLogger.info("Checking passivate count: " + passCount);
        boolean pass = (passCount == 1);
        svLogger.info("Checking activate count: " + actCount);
        boolean act = (actCount == 1);

        boolean comp = false;

        if (singleLocal != null) {
            // Initialize local ref.  This will activate and passivate the bean
            singleLocal.setStringValue("testLocalSingle");
            String strVal = singleLocal.getStringValue();

            svLogger.info("Comparing string value from singleton bean: " + strVal);
            comp = (strVal.equals("testLocalSingle"));
        } else {
            svLogger.severe("singleLocal is null");
        }

        return (pass && act && comp);
    }

    // testLocalSingleStart should always be called before this method to ensure the counts are correct
    @TransactionAttribute(NEVER)
    public boolean testLocalSingleEnd() {
        // Make sure the bean was activated and passivated twice at this point
        svLogger.info("Checking passivate count: " + passCount);
        boolean pass = (passCount == 2);
        svLogger.info("Checking activate count: " + actCount);
        boolean act = (actCount == 2);

        boolean comp = false;

        // Make sure it is the same local ref
        if (singleLocal != null) {
            String strVal = singleLocal.getStringValue();
            svLogger.info("Returned value is: " + strVal);
            comp = (strVal.equals("testLocalSingle"));
        } else {
            svLogger.severe("singleLocal is null");
        }

        passCount = 0;
        actCount = 0;

        return (pass && act && comp);
    }

    // This must be the first method called on the bean when testing remote business ref
    public boolean testRemoteSingleStart() {
        svLogger.info("Checking passivate count: " + passCount);
        boolean pass = (passCount == 1);
        svLogger.info("Checking activate count: " + actCount);
        boolean act = (actCount == 1);

        boolean comp = false;

        if (singleRemote != null) {
            // Initialize remote ref.  This will activate and passivate the bean
            singleRemote.setStringValue("testRemoteSingle");
            String strVal = singleRemote.getStringValue();

            svLogger.info("Comparing string value from singleton bean: " + strVal);
            comp = (strVal.equals("testRemoteSingle"));
        } else {
            svLogger.severe("singleRemote is null");
        }

        return (pass && act && comp);
    }

    // testRemoteSingleStart should always be called before this method to ensure the counts are correct
    @TransactionAttribute(NEVER)
    public boolean testRemoteSingleEnd() {
        // Make sure the bean was activated and passivated twice at this point
        svLogger.info("Checking passivate count: " + passCount);
        boolean pass = (passCount == 2);
        svLogger.info("Checking activate count: " + actCount);
        boolean act = (actCount == 2);

        boolean comp = false;

        // Make sure it is the same remote ref
        if (singleRemote != null) {
            String strVal = singleRemote.getStringValue();
            svLogger.info("Returned value is: " + strVal);
            comp = (strVal.equals("testRemoteSingle"));
        } else {
            svLogger.severe("singleRemote is null");
        }

        passCount = 0;
        actCount = 0;

        return (pass && act && comp);
    }

    @Remove
    public void finish() {
    }
}
