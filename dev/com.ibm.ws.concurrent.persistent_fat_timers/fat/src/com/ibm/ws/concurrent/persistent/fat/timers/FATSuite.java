/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.timers;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

@RunWith(Suite.class)
@SuiteClasses({
    PersistentExecutorTimersTest.class,
    PersistentExecutorTimersWithFailoverEnabledTest.class
    })
public class FATSuite {
    @BeforeClass
    public static void beforeSuite() throws Exception {
        //Allows local tests to switch between using a local docker client, to using a remote docker client. 
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();
    }
}