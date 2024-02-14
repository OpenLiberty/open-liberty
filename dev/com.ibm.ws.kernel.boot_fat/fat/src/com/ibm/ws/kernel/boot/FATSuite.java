/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
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
package com.ibm.ws.kernel.boot;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.kernel.boot.commandline.CreateCommandTest;
import com.ibm.ws.kernel.boot.commandline.DumpCommandTest;
import com.ibm.ws.kernel.boot.commandline.PauseResumeCommandTest;
import com.ibm.ws.kernel.boot.commandline.StartCommandTest;
import com.ibm.ws.kernel.boot.commandport.ServerCommandPortTest;
import com.ibm.ws.kernel.boot.internal.commands.LogLevelPropertyTest;
import com.ibm.ws.kernel.boot.internal.commands.PackageCommandTest;
import com.ibm.ws.kernel.boot.internal.commands.PackageLooseContentsTest;
import com.ibm.ws.kernel.boot.internal.commands.PackageLooseRunnableTest;
import com.ibm.ws.kernel.osgi.OSGiEmbedManagerTest;
import com.ibm.ws.kernel.provisioning.KernelChangeTest;
import com.ibm.ws.kernel.provisioning.ProvisioningTest;
import com.ibm.ws.kernel.service.ServerEndpointControlMBeanTest;
import com.ibm.wsspi.kernel.embeddable.EmbeddedServerAddProductExtensionMultipleTest;
import com.ibm.wsspi.kernel.embeddable.EmbeddedServerAddProductExtensionTest;
import com.ibm.wsspi.kernel.embeddable.EmbeddedServerMergeProductExtensionTest;
import com.ibm.wsspi.kernel.embeddable.EmbeddedServerTest;

/**
 * Collection of tests exercising server function
 */
@RunWith(Suite.class)
/*
 * The classes specified in the @SuiteClasses annotation
 * below should only be mainline test cases that complete
 * in a combined total of 5 minutes or less.
 */
@SuiteClasses({
                EmbeddedServerTest.class,
                EmbeddedServerAddProductExtensionTest.class,
                EmbeddedServerAddProductExtensionMultipleTest.class,
                ProvisioningTest.class,
                KernelChangeTest.class,
                ServerEnvTest.class,
                ServerStartTest.class,
                ServerStopTest.class,
                ServerStartAsServiceTest.class,
                ShutdownTest.class,
                ServerCommandPortTest.class,
                DumpCommandTest.class,
                PackageCommandTest.class,
                PackageLooseRunnableTest.class,
                PackageLooseContentsTest.class,
                // PackageLooseFilterTest.class, // Recreates issue 15724; disabled until that is fixed.
                LogLevelPropertyTest.class,
                CreateCommandTest.class,
                StartCommandTest.class,
                ServerClasspathTest.class,
                ServerStartJVMOptionsTest.class,
                ServerStartJavaEnvironmentVariablesTest.class,
                PauseResumeCommandTest.class,
                EmbeddedServerMergeProductExtensionTest.class,
                ServerEndpointControlMBeanTest.class,
                OSGiEmbedManagerTest.class,
                ServerCleanTest.class,
                VerboseLogTest.class
})
public class FATSuite {
    // Empty
}
