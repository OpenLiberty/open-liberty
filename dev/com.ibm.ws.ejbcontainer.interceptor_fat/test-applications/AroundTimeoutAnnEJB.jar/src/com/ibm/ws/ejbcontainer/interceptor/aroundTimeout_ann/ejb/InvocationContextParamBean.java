/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_ann.ejb;

import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;

@Stateless
public class InvocationContextParamBean extends InvocationContextAbstractBean {
    private static final Logger svLogger = Logger.getLogger(InvocationContextParamBean.class.getName());

    public static Timer svTimeoutTimer;

    @Timeout
    public void timeout(Timer timer) {
        svLogger.info("timeout: " + timer);
        svTimeoutTimer = timer;
        svTimerLatch.countDown();
    }
}
