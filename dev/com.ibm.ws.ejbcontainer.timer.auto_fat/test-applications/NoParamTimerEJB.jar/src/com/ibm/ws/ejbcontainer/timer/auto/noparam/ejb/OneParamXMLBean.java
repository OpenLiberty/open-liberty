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

import javax.ejb.Timer;

public class OneParamXMLBean extends AbstractBean implements Intf {
    public void timeout(Timer timer) {
        setTimeoutExecuted(0, timer);
    }

    public void schedule(Timer timer) {
        setScheduleExecuted(0, timer);
    }
}
