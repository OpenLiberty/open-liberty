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
package com.ibm.ws.ejbcontainer.timer.np.ejb;

import static javax.ejb.LockType.READ;

import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;

@Singleton
@Local
public class SingletonAnnotationTxBean extends AbstractAnnotationTxBean implements SingletonAnnotationTxLocal {

    @Override
    @Lock(READ) // So timers may run while waiting
    public void waitForTimer(long maxWaitTime) {
        super.waitForTimer(maxWaitTime);
    }

    @Override
    @Timeout
    @Lock(READ) // So timer may run while waiting
    public void myTimeout(Timer timer) {
        super.myTimeout(timer);
    }
}
