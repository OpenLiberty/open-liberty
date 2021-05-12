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

public interface AutoCreatedTimerA {
    public void seconds_exact(Timer timer);

    public void seconds_interval(Timer timer);

    public void minutes_exact(Timer timer);

    public void minutes_interval(Timer timer);

    public void hours_range(Timer timer);

    public void dayOfWeek_range(Timer timer);

    public void dayOfWeek_list(Timer timer);

    public void dayOfMonth_exact(Timer timer);

    public void month_exact(Timer timer);

    public void month_range(Timer timer);

    public void year_exact(Timer timer);

    public void dayOfMonth_negative(Timer timer);

    public void dayOfMonth_thirdSundaySyntax(Timer timer);

    public void timezone_set(Timer timer);

    public void rangeAndList(Timer timer);

    public void multipleAttributesCombined(Timer timer);

    public void atSchedulesMethod(Timer timer);

    public void createProgramaticTimer();

    public void waitForProgramaticTimer(long maxTimeToWait);

    public boolean getPersistentStatus(String infoToMatchOn) throws Exception;

    public Properties getTimerData(String infoToMatchOn) throws Exception;

    public void clearAllTimers();
}
