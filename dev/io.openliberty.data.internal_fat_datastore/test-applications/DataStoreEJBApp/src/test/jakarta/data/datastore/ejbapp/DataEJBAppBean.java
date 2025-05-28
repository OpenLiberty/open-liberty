/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.datastore.ejbapp;

import static org.junit.Assert.assertEquals;

import java.util.function.Consumer;

import jakarta.ejb.Singleton;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;

@Singleton
public class DataEJBAppBean implements Consumer<String>, DataEJBAppBeanInterface {
    private static final String TEST_OBSERVES_STARTUP = //
                    "testStartupEventObserverInEJBApplicationUsesRepository";

    @Inject
    EJBAppDSRefRepo ejbAppRepo;

    @Override
    public void accept(String testName) {
        System.out.println("DataEJBAppBean is running " + testName);

        if (TEST_OBSERVES_STARTUP.equals(testName)) {
            // Database must already be populated by the startup method
        } else {
            assertEquals(false, ejbAppRepo.lookFor(testName).isPresent());

            EJBAppEntity e = EJBAppEntity.of(testName);
            e = ejbAppRepo.persist(e);
            assertEquals(testName, e.testName);
            assertEquals(testName.length(), e.nameLength);
        }

        EJBAppEntity e = ejbAppRepo.lookFor(testName).orElseThrow();
        assertEquals(testName, e.testName);
        assertEquals(testName.length(), e.nameLength);
    }

    /**
     * Add some data on startup.
     */
    @Override
    public void startup(@Observes Startup event) {
        System.out.println("DataEJBAppBean observed Startup");

        ejbAppRepo.persist(EJBAppEntity.of(TEST_OBSERVES_STARTUP));
    }
}
