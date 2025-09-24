/*******************************************************************************
 * Copyright (c) 2006, 2025 IBM Corporation and others.
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

package singleton.property.startup.ejb;

import java.util.logging.Logger;

import singleton.property.shared.BasicSingleton2;
import singleton.property.shared.StartupHelperSingleton;
import singleton.property.shared.TestData;

// @Startup
// @Singleton
public class StartupSingletonBean implements BasicSingleton2 {
    private static final String CLASSNAME = StartupSingletonBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASSNAME);

    // The amount of time to delay startup processing such that another
    // thread can attempt work and be forced to wait for startup
    // processing completion.
    private static final long STARTUP_DELAY = 10000;

    private StartupHelperSingleton ivHelper;

    // @PostConstruct
    public void postConstruct() {
        // Notify the test thread that we've started.
        svLogger.info("> postConstruct : notifying application");
        TestData.awaitStartupBarrier();

        // Wait for the test thread to verify that it cannot access the
        // helper until we've finished PostConstruct.
        svLogger.info("- postConstruct : waiting for application");
        TestData.awaitStartupBarrier();

        // This delay is here to allow another thread to attempt
        // work and be delayed until this finishes.
        svLogger.info("- postConstruct : delaying for 10 seconds waiting for application");
        try {
            Thread.sleep(STARTUP_DELAY);
        } catch (InterruptedException e) {
            //This should never never happen
            System.out.println("Thread.sleep failed... severe error.");
        }

        // Signal success by verifying that this thread is allowed to call
        // another bean even though the test thread cannot.
        ivHelper.setPostConstructRun(true);
        svLogger.info("< postConstruct : complete");
    }
}
