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
package com.ibm.ws.ejbcontainer.timer.np.config.retry.ejb;

import java.util.Properties;

/**
 * Remote interface for the TimerRetryTest client container test
 * to drive work in the server process. <p>
 **/
public interface TimerRetryDriver {
    public void forceOneFailure(String testName);

    public void forceEverythingToFail(String testName, int retries);

    public void forceEverythingToFailIntervalTimer(String testName, int retries);

    public void forceEverythingToFailCalendarTimer(String testName, int retries);

    public void forceTwoFailures(String testName);

    public void forceRetrysAndRegularSchedulesToOverlap(String testName, int retries);

    public Properties getResults();

    public void waitForTimersAndCancel(long cancelDelay);
}
