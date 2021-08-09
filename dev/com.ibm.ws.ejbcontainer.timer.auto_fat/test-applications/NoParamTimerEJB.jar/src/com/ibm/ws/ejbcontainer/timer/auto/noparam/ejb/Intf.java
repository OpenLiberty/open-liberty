/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.timer.auto.noparam.ejb;

import java.util.concurrent.CountDownLatch;

public interface Intf {
    CountDownLatch startTimer();

    public void clearAllTimers();

    boolean isTimeoutExecuted(int index);

    boolean isScheduleExecuted(int index);
}
