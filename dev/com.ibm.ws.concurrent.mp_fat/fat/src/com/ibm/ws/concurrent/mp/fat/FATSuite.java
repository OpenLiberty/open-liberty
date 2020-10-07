/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                MPConcurrentTest.class,
                MPConcurrentConfigTest.class,
                MPConcurrentJAXRSTest.class,
                MPConcurrentTxTest.class,
                MPContextProp1_1_Test.class,
                MPConcurrentCDITest.class // moved last because @RepeatTests, when added to this class, interferes with test classes that run after it // TODO
})
public class FATSuite {
}
