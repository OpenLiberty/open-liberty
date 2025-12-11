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

import java.util.function.BiConsumer;

import jakarta.ejb.Singleton;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;

@Singleton
public class DataEJBAppBean implements BiConsumer<String, String>, DataEJBAppBeanInterface {
    private static final String TEST_OBSERVES_STARTUP = //
                    "testStartupEventObserverInEJBApplicationUsesRepository";

    @Inject
    EJBAppDSDRepo ejbAppDSDRepo;

    @Inject
    EJBAppDSRefRepo ejbAppDSRefRepo;

    /**
     * Runs a test using the specified repository.
     *
     * @param testName name of the test to run
     * @param repo     repository to use: EJBAppDSDRepo or EJBAppDSRefRepo
     */
    @Override
    public void accept(String testName, String repo) {
        System.out.println("DataEJBAppBean is running " + testName +
                           " with repository " + repo);

        if (EJBAppDSDRepo.class.getSimpleName().equals(repo))
            testDSDRepo(testName);
        else if (EJBAppDSRefRepo.class.getSimpleName().equals(repo))
            testDSRefRepo(testName);
        else
            throw new IllegalArgumentException(repo);
    }

    /**
     * Add some data on startup.
     */
    @Override
    public void startup(@Observes Startup event) {
        System.out.println("DataEJBAppBean observed Startup");

        ejbAppDSRefRepo.persist(EJBAppEntity.of(TEST_OBSERVES_STARTUP));
    }

    /**
     * Run a test against the repository that defines and uses a
     * DataSourceDefinition.
     *
     * @param testName name of the test.
     */
    private void testDSDRepo(String testName) {
        assertEquals(false, ejbAppDSDRepo.seek(testName).isPresent());

        EJBAppEntity e = EJBAppEntity.of(testName);
        e = ejbAppDSDRepo.writeToDatabase(e);
        assertEquals(testName, e.testName);
        assertEquals(testName.length(), e.nameLength);

        e = ejbAppDSDRepo.seek(testName).orElseThrow();
        assertEquals(testName, e.testName);
        assertEquals(testName.length(), e.nameLength);
    }

    /**
     * Run a test against the repository that defines and uses a
     * Resource reference to a data source.
     *
     * @param testName name of the test.
     */
    private void testDSRefRepo(String testName) {
        if (TEST_OBSERVES_STARTUP.equals(testName)) {
            // Database must already be populated by the startup method
        } else {
            assertEquals(false, ejbAppDSRefRepo.lookFor(testName).isPresent());

            EJBAppEntity e = EJBAppEntity.of(testName);
            e = ejbAppDSRefRepo.persist(e);
            assertEquals(testName, e.testName);
            assertEquals(testName.length(), e.nameLength);
        }

        EJBAppEntity e = ejbAppDSRefRepo.lookFor(testName).orElseThrow();
        assertEquals(testName, e.testName);
        assertEquals(testName.length(), e.nameLength);
    }
}
