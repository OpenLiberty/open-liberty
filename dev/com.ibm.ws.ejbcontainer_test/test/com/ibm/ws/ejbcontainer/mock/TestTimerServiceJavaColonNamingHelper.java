/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mock;

import javax.ejb.TimerService;

import com.ibm.ws.ejbcontainer.osgi.internal.naming.TimerServiceJavaColonNamingHelper;

public class TestTimerServiceJavaColonNamingHelper extends TimerServiceJavaColonNamingHelper {

    private boolean timerServiceActive = false;
    private final TimerService timerService;

    public TestTimerServiceJavaColonNamingHelper(TimerService context) {
        timerService = context;
    }

    public void setTimerServiceActive(boolean timerServiceActive) {
        this.timerServiceActive = timerServiceActive;
    }

    @Override
    protected boolean isTimerServiceActive() {
        return timerServiceActive;
    }

    @Override
    protected TimerService getTimerService() {
        return timerService;
    }
}
