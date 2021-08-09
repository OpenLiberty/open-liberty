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
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.interceptor.Interceptors;

@Local(StatefulEmptyLocal.class)
@Remote(StatefulEmptyRemote.class)
@Stateful
@Interceptors({ CLEmptyInterceptor.class })
@ExcludeDefaultInterceptors
public class StatefulEmptyBean {

    @PostConstruct
    void postConstruct() {
        PassivationTracker.clearAll();
        PassivationTracker.addMessage("StatefulEmptyBean.postConstruct");
    }

    @PrePassivate
    public void passivate() {
        PassivationTracker.addMessage("StatefulEmptyBean.passivate");
    }

    @PostActivate
    public void activate() {
        PassivationTracker.addMessage("StatefulEmptyBean.activate");
    }

    public void checkEmptyStart(String key) {

    }

    public void checkEmptyEnd(String key) {
        if (key.equals("Remote")) {
            PassivationTracker.compareMessages(PassivationTracker.SF_EMPTY_EXPECTED_RESULTS_REMOTE);
        } else {
            PassivationTracker.compareMessages(PassivationTracker.SF_EMPTY_EXPECTED_RESULTS_LOCAL);
        }
    }

    @Remove
    public void finish() {

    }
}
