/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package io.openliberty.ejbcontainer.fat.timer.auto.np.ejb;

import java.util.Properties;

public interface AutoCreatedTimerDriver {
    public void setup();

    public Properties getTimeoutResults(String resultsToGet);

    public Properties getTimerScheduleData(String beanWithTimer, String infoToMatchOn) throws Exception;

    public void driveCreationOfProgramaticTimer();

    public void waitForProgramaticTimer(long maxTimeToWait);

    public void clearAllTimers();
}
