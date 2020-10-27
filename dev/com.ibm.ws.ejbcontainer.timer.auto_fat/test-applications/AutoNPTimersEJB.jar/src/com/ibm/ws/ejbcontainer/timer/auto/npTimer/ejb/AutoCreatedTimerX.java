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

public interface AutoCreatedTimerX {
    public void seconds_range(Timer timer);

    public void seconds_list(Timer timer);

    public void minutes_range(Timer timer);

    public void minutes_list(Timer timer);

    public void hours_list(Timer timer);

    public void dayOfWeek_exact(Timer timer);

    public void dayOfMonth_range(Timer timer);

    public void month_list(Timer timer);

    public void dayOfMonth_last(Timer timer);

    public void multipleSettingsDontConflict(Timer timer);

    public Properties getTimerData(String infoToMatchOn);

    public void clearAllTimers();
}
