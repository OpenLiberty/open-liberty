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

package com.ibm.ws.ejbcontainer.timer.auto.npTimer.ejb;

import java.util.Properties;

import javax.ejb.Timer;

public interface AutoCreatedTimerNI {
    public void noInfoTimer(Timer timer);

    public Properties getTimerData(String infoToMatchOn);

    public void clearAllTimers();
}
