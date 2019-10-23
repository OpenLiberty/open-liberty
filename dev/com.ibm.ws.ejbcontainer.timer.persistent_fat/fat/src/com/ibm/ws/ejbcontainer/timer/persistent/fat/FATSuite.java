/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.persistent.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.ejbcontainer.timer.persistent.fat.tests.NoDatasourceTest;
import com.ibm.ws.ejbcontainer.timer.persistent.fat.tests.NoTablesTest;
import com.ibm.ws.ejbcontainer.timer.persistent.fat.tests.PersistentTimerCoreTest;

@RunWith(Suite.class)
@SuiteClasses({
                NoDatasourceTest.class,
                NoTablesTest.class,
                PersistentTimerCoreTest.class
})
public class FATSuite {
}
