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
package com.ibm.ws.ejbcontainer.interceptor.aroundTimeout_exc.ejb;

import java.util.concurrent.CountDownLatch;

public interface ATAppExInterface {
    public static final String AUTO_TIMER_INFO = "automaticTimerAppEx";

    public CountDownLatch getAutoTimerLatch();

    public CountDownLatch createSingleActionTimer(String info);
}
