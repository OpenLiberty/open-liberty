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
package com.ibm.ws.ejbcontainer.timer.np.shared;

import java.io.Serializable;
import java.util.Date;

public interface LifecycleIntf {
    // The delay to wait for a timer to fire immediately.
    long DELAY = 400;

    // The duration used to schedule a timer that should fire after an
    // application has stopped.  In other words, at least as long as it will
    // take to stop the application.
    long STOP_DURATION = 5000;

    long MAX_WAIT = 2 * 60 * 1000;

    String INFO_POST_CONSTRUCT = "PostConstruct";
    String INFO_PRE_DESTROY = "PreDestroy";
    String INFO_PREP_PRE_DESTROY = "PrepForPreDestroy";
    String INFO_ASYNC_PREP_PRE_DESTROY = "AsyncPrepForPreDestroy";

    Date createTimer(long duration, Serializable info);

    void createTimerAsync(long duration, Serializable info);
}
