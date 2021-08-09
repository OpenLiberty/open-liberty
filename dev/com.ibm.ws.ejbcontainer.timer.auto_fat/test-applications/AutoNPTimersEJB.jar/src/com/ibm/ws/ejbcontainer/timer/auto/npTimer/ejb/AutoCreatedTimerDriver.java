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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

public interface AutoCreatedTimerDriver {
    public void setup();

    public Properties getTimeoutResults(String resultsToGet);

    public Properties getTimerScheduleData(String beanWithTimer, String infoToMatchOn) throws Exception;

    public boolean getTimerPersistentStatus(String beanWithTimer, String infoToMatchOn) throws Exception;

    public boolean didSingletonStartupBeanSeeItsTimer();

    public boolean didNIBeanFindItsTimer();

    public boolean didNIBeanHaveNullInfo();

    public void driveCreationOfProgramaticTimer();

    public void waitForProgramaticTimer(long maxTimeToWait);

    public HashSet<String> getTimerInfos(String beanToGetTimerInfosFor);

    public HashMap<String, HashSet<Class<?>>> getTimerMethodToInvokingClassMap();

    public void clearAllTimers();
}
