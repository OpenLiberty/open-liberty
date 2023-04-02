/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.timer.np.ejb;

import java.util.Collection;

import javax.ejb.Timer;

public interface SimpleSFSBLocal {

    public Timer createTimer(String info);

    public Timer cancelTimer(String info);

    public Collection<Timer> getAllTimers();

    public Timer getCurrentTimer();

    public String getCurrentTimerInfo();

    public boolean hasBeenPassivated();

    public void resetPassivationFlag();

    public void clearAllTimers();
}
