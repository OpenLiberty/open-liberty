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

import java.util.Collection;

import javax.ejb.Timer;

public interface SimpleTimerLocal {

    public Timer createTimer(String info);

    public Collection<Timer> getTimers();

    public Collection<String> getInfoOfAllTimers();

    public void clearAllTimers();
}
