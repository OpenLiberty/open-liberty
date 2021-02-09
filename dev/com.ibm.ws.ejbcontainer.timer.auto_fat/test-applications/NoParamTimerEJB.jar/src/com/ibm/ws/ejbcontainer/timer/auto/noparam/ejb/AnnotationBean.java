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

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timeout;

@Singleton
public class AnnotationBean extends AbstractBean implements Intf {
    @Timeout
    public void timeout() {
        setTimeoutExecuted(0);
    }

    @Schedule(hour = "*", minute = "*", second = "*/10", persistent = false)
    public void schedule() {
        setScheduleExecuted(0);
    }
}
