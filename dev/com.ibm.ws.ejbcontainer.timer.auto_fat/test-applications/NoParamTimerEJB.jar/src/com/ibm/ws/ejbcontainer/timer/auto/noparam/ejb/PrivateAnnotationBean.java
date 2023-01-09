/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.timer.auto.noparam.ejb;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timeout;

@Singleton
public class PrivateAnnotationBean extends AbstractBean implements Intf {
    @Timeout
    private void timeout() {
        setTimeoutExecuted(0);
    }

    @Schedule(hour = "*", minute = "*", second = "*/10", persistent = false)
    private void schedule() {
        setScheduleExecuted(0);
    }
}
