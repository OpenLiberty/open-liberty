/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.ejbcontainer.timer.np.fat.tests.NpTimerLifecycleTest;
import com.ibm.ws.ejbcontainer.timer.np.fat.tests.NpTimerOperationsTest;

@RunWith(Suite.class)
@SuiteClasses({
                NpTimerLifecycleTest.class,
                NpTimerOperationsTest.class
})
public class FATSuite {
}
