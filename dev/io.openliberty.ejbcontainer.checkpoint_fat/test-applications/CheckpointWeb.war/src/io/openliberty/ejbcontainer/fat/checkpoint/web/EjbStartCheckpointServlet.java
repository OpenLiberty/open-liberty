/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.ejbcontainer.fat.checkpoint.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;
import io.openliberty.ejbcontainer.fat.checkpoint.ejb.CheckpointLocal;
import io.openliberty.ejbcontainer.fat.checkpoint.ejb.CheckpointStatistics;

@WebServlet("/EjbStartCheckpointServlet")
@SuppressWarnings("serial")
public class EjbStartCheckpointServlet extends FATServlet {

    public void testEjbStartCheckpointInactive() throws Exception {
        // Only the @Startup Singleton bean classes should be initialized
        assertEquals("Wrong number of classes initialized", 8, CheckpointStatistics.getInitializedClassListSize());
        assert_8_StartupSingletonClassesInitialized();

        // Only the @Startup Singleton bean instances should be created; 1 each
        assertEquals("Wrong number of classes created", 8, CheckpointStatistics.getInstanceCountMapSize());
        assert_8_StartupSingletonClassInstancesCreated();

        // Access all beans and verify they are functional
        verifyAllBeans();

        // All bean classes should now be initialized
        assertEquals("Wrong number of classes initialized", 44, CheckpointStatistics.getInitializedClassListSize());
        assert_20_SingletonClassesInitialized();
        assert_24_StatelessClassesInitialized();

        // At least one instance per bean should now be created; more for those with a hard minimum poolSize
        assertEquals("Wrong number of classes created", 44, CheckpointStatistics.getInstanceCountMapSize());
        assert_20_SingletonClassInstancesCreated();
        assert_24_StatelessClassInstancesCreated();
    }

    public void testEjbStartCheckpointDeployment() throws Exception {
        // Only the @Startup Singleton bean classes should be initialized
        assertEquals("Wrong number of classes initialized", 8, CheckpointStatistics.getInitializedClassListSize());
        assert_8_StartupSingletonClassesInitialized();

        // Only the @Startup Singleton bean instances should be created; 1 each
        assertEquals("Wrong number of classes created", 8, CheckpointStatistics.getInstanceCountMapSize());
        assert_8_StartupSingletonClassInstancesCreated();

        verifyAllBeans();

        // All bean classes should now be initialized
        assertEquals("Wrong number of classes initialized", 44, CheckpointStatistics.getInitializedClassListSize());
        assert_20_SingletonClassesInitialized();
        assert_24_StatelessClassesInitialized();

        // At least one instance per bean should now be created; more for those with a hard minimum poolSize
        assertEquals("Wrong number of classes created", 44, CheckpointStatistics.getInstanceCountMapSize());
        assert_20_SingletonClassInstancesCreated();
        assert_24_StatelessClassInstancesCreated();
    }

    public void testEjbStartCheckpointApplications() throws Exception {
        // All Singleton and Stateless that don't disable StartAtAppStart bean classes should be initialized
        assertEquals("Wrong number of classes initialized", 32, CheckpointStatistics.getInitializedClassListSize());
        assert_16_SingletonClassesInitialized();
        assert_16_StartAtAppStatelessClassesInitialized();

        // All beans from above should be fully preloaded; no need to wait as preload should complete before application start completes
        assertEquals("Wrong number of classes created", 32, CheckpointStatistics.getInstanceCountMapSize());
        assert_16_SingletonClassInstancesCreated();
        assert_16_StartAtAppStatelessClassInstancesPreloaded();

        verifyAllBeans();

        // All bean classes should now be initialized
        assertEquals("Wrong number of classes initialized", 44, CheckpointStatistics.getInitializedClassListSize());
        assert_20_SingletonClassesInitialized();
        assert_24_StatelessClassesInitialized();

        // One instance per singleton should now be created; stateless that do not disable StartAtAppStart should
        // be fully preloaded; otherwise will be preloaded if there is a configured hard minimum pool size.
        assertEquals("Wrong number of classes created", 44, CheckpointStatistics.getInstanceCountMapSize());
        assert_20_SingletonClassInstancesCreated();
        assert_24_StatelessClassInstancesPreloaded();
    }

    // Just the @Startup Singleton beans
    private void assert_8_StartupSingletonClassesInitialized() {
        assertTrue("Startup bean SGCheckpointBeanA not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanA"));
        assertTrue("Startup bean SGCheckpointBeanB not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanB"));
        assertTrue("Startup bean SGCheckpointBeanG not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanG"));
        assertTrue("Startup bean SGCheckpointBeanH not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanH"));
        assertTrue("Startup bean SGCheckpointBeanM not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanM"));
        assertTrue("Startup bean SGCheckpointBeanN not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanN"));
        assertTrue("Startup bean SGCheckpointBeanS not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanS"));
        assertTrue("Startup bean SGCheckpointBeanT not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanT"));
    }

    // Just the Singleton beans that do not explicitly disable start-at-app-start
    private void assert_16_SingletonClassesInitialized() {
        assertTrue("Singleton bean SGCheckpointBeanA not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanA"));
        assertTrue("Singleton bean SGCheckpointBeanB not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanB"));
        assertTrue("Singleton bean SGCheckpointBeanC not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanC"));
        assertTrue("Singleton bean SGCheckpointBeanD not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanD"));
        assertTrue("Singleton bean SGCheckpointBeanG not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanG"));
        assertTrue("Singleton bean SGCheckpointBeanH not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanH"));
        assertTrue("Singleton bean SGCheckpointBeanI not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanI"));
        assertTrue("Singleton bean SGCheckpointBeanJ not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanJ"));
        assertTrue("Singleton bean SGCheckpointBeanM not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanM"));
        assertTrue("Singleton bean SGCheckpointBeanN not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanN"));
        assertTrue("Singleton bean SGCheckpointBeanO not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanO"));
        assertTrue("Singleton bean SGCheckpointBeanP not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanP"));
        assertTrue("Singleton bean SGCheckpointBeanS not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanS"));
        assertTrue("Singleton bean SGCheckpointBeanT not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanT"));
        assertTrue("Singleton bean SGCheckpointBeanU not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanU"));
        assertTrue("Singleton bean SGCheckpointBeanV not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanV"));
    }

    // All Singleton beans in the application
    private void assert_20_SingletonClassesInitialized() {
        assertTrue("Singleton bean SGCheckpointBeanA not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanA"));
        assertTrue("Singleton bean SGCheckpointBeanB not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanB"));
        assertTrue("Singleton bean SGCheckpointBeanC not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanC"));
        assertTrue("Singleton bean SGCheckpointBeanD not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanD"));
        assertTrue("Singleton bean SGCheckpointBeanE not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanE"));
        assertTrue("Singleton bean SGCheckpointBeanG not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanG"));
        assertTrue("Singleton bean SGCheckpointBeanH not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanH"));
        assertTrue("Singleton bean SGCheckpointBeanI not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanI"));
        assertTrue("Singleton bean SGCheckpointBeanJ not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanJ"));
        assertTrue("Singleton bean SGCheckpointBeanK not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanK"));
        assertTrue("Singleton bean SGCheckpointBeanM not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanM"));
        assertTrue("Singleton bean SGCheckpointBeanN not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanN"));
        assertTrue("Singleton bean SGCheckpointBeanO not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanO"));
        assertTrue("Singleton bean SGCheckpointBeanP not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanP"));
        assertTrue("Singleton bean SGCheckpointBeanQ not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanQ"));
        assertTrue("Singleton bean SGCheckpointBeanS not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanS"));
        assertTrue("Singleton bean SGCheckpointBeanT not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanT"));
        assertTrue("Singleton bean SGCheckpointBeanU not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanU"));
        assertTrue("Singleton bean SGCheckpointBeanV not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanV"));
        assertTrue("Singleton bean SGCheckpointBeanW not initialized", CheckpointStatistics.isClassInitialized("SGCheckpointBeanW"));
    }

    // Just the Stateless beans that do not explicitly disable start-at-app-start
    private void assert_16_StartAtAppStatelessClassesInitialized() {
        assertTrue("Stateless bean SLCheckpointBeanA not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanA"));
        assertTrue("Stateless bean SLCheckpointBeanB not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanB"));
        assertTrue("Stateless bean SLCheckpointBeanC not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanC"));
        assertTrue("Stateless bean SLCheckpointBeanD not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanD"));
        assertTrue("Stateless bean SLCheckpointBeanG not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanG"));
        assertTrue("Stateless bean SLCheckpointBeanH not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanH"));
        assertTrue("Stateless bean SLCheckpointBeanI not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanI"));
        assertTrue("Stateless bean SLCheckpointBeanJ not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanJ"));
        assertTrue("Stateless bean SLCheckpointBeanM not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanM"));
        assertTrue("Stateless bean SLCheckpointBeanN not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanN"));
        assertTrue("Stateless bean SLCheckpointBeanO not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanO"));
        assertTrue("Stateless bean SLCheckpointBeanP not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanP"));
        assertTrue("Stateless bean SLCheckpointBeanS not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanS"));
        assertTrue("Stateless bean SLCheckpointBeanT not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanT"));
        assertTrue("Stateless bean SLCheckpointBeanU not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanU"));
        assertTrue("Stateless bean SLCheckpointBeanV not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanV"));
    }

    // All Stateless beans in the application
    private void assert_24_StatelessClassesInitialized() {
        assertTrue("Stateless bean SLCheckpointBeanA not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanA"));
        assertTrue("Stateless bean SLCheckpointBeanB not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanB"));
        assertTrue("Stateless bean SLCheckpointBeanC not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanC"));
        assertTrue("Stateless bean SLCheckpointBeanD not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanD"));
        assertTrue("Stateless bean SLCheckpointBeanE not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanE"));
        assertTrue("Stateless bean SLCheckpointBeanF not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanF"));
        assertTrue("Stateless bean SLCheckpointBeanG not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanG"));
        assertTrue("Stateless bean SLCheckpointBeanH not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanH"));
        assertTrue("Stateless bean SLCheckpointBeanI not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanI"));
        assertTrue("Stateless bean SLCheckpointBeanJ not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanJ"));
        assertTrue("Stateless bean SLCheckpointBeanK not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanK"));
        assertTrue("Stateless bean SLCheckpointBeanL not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanL"));
        assertTrue("Stateless bean SLCheckpointBeanM not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanM"));
        assertTrue("Stateless bean SLCheckpointBeanN not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanN"));
        assertTrue("Stateless bean SLCheckpointBeanO not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanO"));
        assertTrue("Stateless bean SLCheckpointBeanP not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanP"));
        assertTrue("Stateless bean SLCheckpointBeanQ not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanQ"));
        assertTrue("Stateless bean SLCheckpointBeanR not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanR"));
        assertTrue("Stateless bean SLCheckpointBeanS not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanS"));
        assertTrue("Stateless bean SLCheckpointBeanT not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanT"));
        assertTrue("Stateless bean SLCheckpointBeanU not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanU"));
        assertTrue("Stateless bean SLCheckpointBeanV not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanV"));
        assertTrue("Stateless bean SLCheckpointBeanW not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanW"));
        assertTrue("Stateless bean SLCheckpointBeanX not initialized", CheckpointStatistics.isClassInitialized("SLCheckpointBeanX"));
    }

    // Just the @Startup Singleton beans
    private void assert_8_StartupSingletonClassInstancesCreated() {
        // All singleton beans will have exactly one instance created
        assertEquals("Wrong number of bean instances for SGCheckpointBeanA", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanA"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanB", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanB"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanG", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanG"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanH", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanH"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanM", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanM"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanN", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanN"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanS", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanS"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanT", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanT"));
    }

    // Just the Singleton beans that do not explicitly disable start-at-app-start
    private void assert_16_SingletonClassInstancesCreated() {
        // All singleton beans will have exactly one instance created
        assertEquals("Wrong number of bean instances for SGCheckpointBeanA", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanA"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanB", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanB"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanC", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanC"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanD", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanD"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanG", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanG"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanH", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanH"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanI", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanI"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanJ", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanJ"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanM", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanM"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanN", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanN"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanO", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanO"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanP", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanP"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanS", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanS"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanT", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanT"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanU", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanU"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanV", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanV"));
    }

    // All Singleton beans in the application
    private void assert_20_SingletonClassInstancesCreated() {
        // All singleton beans will have exactly one instance created
        assertEquals("Wrong number of bean instances for SGCheckpointBeanA", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanA"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanB", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanB"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanC", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanC"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanD", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanD"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanE", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanE"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanG", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanG"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanH", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanH"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanI", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanI"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanJ", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanJ"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanK", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanK"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanM", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanM"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanN", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanN"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanO", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanO"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanP", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanP"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanQ", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanQ"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanS", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanS"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanT", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanT"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanU", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanU"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanV", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanV"));
        assertEquals("Wrong number of bean instances for SGCheckpointBeanW", 1, CheckpointStatistics.getInstanceCount("SGCheckpointBeanW"));
    }

    // Just the Stateless beans that do not explicitly disable start-at-app-start
    private void assert_16_StartAtAppStatelessClassInstancesPreloaded() {
        // Verifies the StartAtApp stateless beans are preloaded during application start; instance count should be exact
        // and there is no need to wait on a CountDownLatch since preload should complete before application start completes
        assertEquals("Wrong number of bean instances for SLCheckpointBeanA", 50, CheckpointStatistics.getInstanceCount("SLCheckpointBeanA"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanB", 20, CheckpointStatistics.getInstanceCount("SLCheckpointBeanB"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanC", 50, CheckpointStatistics.getInstanceCount("SLCheckpointBeanC"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanD", 20, CheckpointStatistics.getInstanceCount("SLCheckpointBeanD"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanG", 50, CheckpointStatistics.getInstanceCount("SLCheckpointBeanG"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanH", 20, CheckpointStatistics.getInstanceCount("SLCheckpointBeanH"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanI", 50, CheckpointStatistics.getInstanceCount("SLCheckpointBeanI"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanJ", 20, CheckpointStatistics.getInstanceCount("SLCheckpointBeanJ"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanM", 50, CheckpointStatistics.getInstanceCount("SLCheckpointBeanM"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanN", 20, CheckpointStatistics.getInstanceCount("SLCheckpointBeanN"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanO", 50, CheckpointStatistics.getInstanceCount("SLCheckpointBeanO"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanP", 20, CheckpointStatistics.getInstanceCount("SLCheckpointBeanP"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanS", 50, CheckpointStatistics.getInstanceCount("SLCheckpointBeanS"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanT", 20, CheckpointStatistics.getInstanceCount("SLCheckpointBeanT"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanU", 50, CheckpointStatistics.getInstanceCount("SLCheckpointBeanU"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanV", 20, CheckpointStatistics.getInstanceCount("SLCheckpointBeanV"));
    }

    // All Stateless beans in the application; default preloading
    private void assert_24_StatelessClassInstancesCreated() throws Exception {
        // Default behavior for stateless beans is to not preload, so there will be one per bean except for those beans
        // with a configured hard minimum poolSize; wait for those beans with a hard minimum as preload is on first use.
        // Note: the count for preloaded beans may vary by one, depending on whether the method call creates before preload starts
        assertEquals("Wrong number of bean instances for SLCheckpointBeanA", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanA"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanB", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanB"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanC", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanC"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanD", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanD"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanE", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanE"));
        int instanceCount = CheckpointStatistics.getInstanceCountAfterPreload("SLCheckpointBeanF");
        assertTrue("Wrong number of bean instances for SLCheckpointBeanF : " + instanceCount, instanceCount == 20 || instanceCount == 21);
        assertEquals("Wrong number of bean instances for SLCheckpointBeanG", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanG"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanH", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanH"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanI", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanI"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanJ", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanJ"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanK", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanK"));
        instanceCount = CheckpointStatistics.getInstanceCountAfterPreload("SLCheckpointBeanL");
        assertTrue("Wrong number of bean instances for SLCheckpointBeanL : " + instanceCount, instanceCount == 20 || instanceCount == 21);
        assertEquals("Wrong number of bean instances for SLCheckpointBeanM", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanM"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanN", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanN"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanO", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanO"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanP", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanP"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanQ", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanQ"));
        instanceCount = CheckpointStatistics.getInstanceCountAfterPreload("SLCheckpointBeanR");
        assertTrue("Wrong number of bean instances for SLCheckpointBeanR : " + instanceCount, instanceCount == 20 || instanceCount == 21);
        assertEquals("Wrong number of bean instances for SLCheckpointBeanS", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanS"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanT", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanT"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanU", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanU"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanV", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanV"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanW", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanW"));
        instanceCount = CheckpointStatistics.getInstanceCountAfterPreload("SLCheckpointBeanX");
        assertTrue("Wrong number of bean instances for SLCheckpointBeanX : " + instanceCount, instanceCount == 20 || instanceCount == 21);
    }

    // All Stateless beans in the application; extra checkpoint preloading
    private void assert_24_StatelessClassInstancesPreloaded() throws Exception {
        // Checkpoint behavior for stateless beans is to preload all beans, except those with StartAtAppStart explicitly disabled.
        // Verify the beans without StartAtAppStart disabled will be fully preloaded and the remaining will only be preloaded if
        // there is a hard minimum pool size configured (a wait will be required since they preload on first use).
        // Note: the count for preload on first use beans may vary by one, depending on whether the method call creates before preload starts
        assertEquals("Wrong number of bean instances for SLCheckpointBeanA", 50, CheckpointStatistics.getInstanceCount("SLCheckpointBeanA"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanB", 20, CheckpointStatistics.getInstanceCount("SLCheckpointBeanB"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanC", 50, CheckpointStatistics.getInstanceCount("SLCheckpointBeanC"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanD", 20, CheckpointStatistics.getInstanceCount("SLCheckpointBeanD"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanE", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanE"));
        int instanceCount = CheckpointStatistics.getInstanceCountAfterPreload("SLCheckpointBeanF");
        assertTrue("Wrong number of bean instances for SLCheckpointBeanF : " + instanceCount, instanceCount == 20 || instanceCount == 21);
        assertEquals("Wrong number of bean instances for SLCheckpointBeanG", 50, CheckpointStatistics.getInstanceCount("SLCheckpointBeanG"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanH", 20, CheckpointStatistics.getInstanceCount("SLCheckpointBeanH"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanI", 50, CheckpointStatistics.getInstanceCount("SLCheckpointBeanI"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanJ", 20, CheckpointStatistics.getInstanceCount("SLCheckpointBeanJ"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanK", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanK"));
        instanceCount = CheckpointStatistics.getInstanceCountAfterPreload("SLCheckpointBeanL");
        assertTrue("Wrong number of bean instances for SLCheckpointBeanL : " + instanceCount, instanceCount == 20 || instanceCount == 21);
        assertEquals("Wrong number of bean instances for SLCheckpointBeanM", 50, CheckpointStatistics.getInstanceCount("SLCheckpointBeanM"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanN", 20, CheckpointStatistics.getInstanceCount("SLCheckpointBeanN"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanO", 50, CheckpointStatistics.getInstanceCount("SLCheckpointBeanO"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanP", 20, CheckpointStatistics.getInstanceCount("SLCheckpointBeanP"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanQ", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanQ"));
        instanceCount = CheckpointStatistics.getInstanceCountAfterPreload("SLCheckpointBeanR");
        assertTrue("Wrong number of bean instances for SLCheckpointBeanR : " + instanceCount, instanceCount == 20 || instanceCount == 21);
        assertEquals("Wrong number of bean instances for SLCheckpointBeanS", 50, CheckpointStatistics.getInstanceCount("SLCheckpointBeanS"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanT", 20, CheckpointStatistics.getInstanceCount("SLCheckpointBeanT"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanU", 50, CheckpointStatistics.getInstanceCount("SLCheckpointBeanU"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanV", 20, CheckpointStatistics.getInstanceCount("SLCheckpointBeanV"));
        assertEquals("Wrong number of bean instances for SLCheckpointBeanW", 1, CheckpointStatistics.getInstanceCount("SLCheckpointBeanW"));
        instanceCount = CheckpointStatistics.getInstanceCountAfterPreload("SLCheckpointBeanX");
        assertTrue("Wrong number of bean instances for SLCheckpointBeanX : " + instanceCount, instanceCount == 20 || instanceCount == 21);
    }

    private void verifyAllBeans() {
        lookupLocal("CheckpointEJB/SGCheckpointBeanA").verify();
        lookupLocal("CheckpointEJB/SGCheckpointBeanB").verify();
        lookupLocal("CheckpointEJB/SGCheckpointBeanC").verify();
        lookupLocal("CheckpointEJB/SGCheckpointBeanD").verify();
        lookupLocal("CheckpointEJB/SGCheckpointBeanE").verify();
        lookupLocal("CheckpointEJB/SLCheckpointBeanA").verify();
        lookupLocal("CheckpointEJB/SLCheckpointBeanB").verify();
        lookupLocal("CheckpointEJB/SLCheckpointBeanC").verify();
        lookupLocal("CheckpointEJB/SLCheckpointBeanD").verify();
        lookupLocal("CheckpointEJB/SLCheckpointBeanE").verify();
        lookupLocal("CheckpointEJB/SLCheckpointBeanF").verify();

        lookupLocal("CheckpointOtherEJB/SGCheckpointBeanG").verify();
        lookupLocal("CheckpointOtherEJB/SGCheckpointBeanH").verify();
        lookupLocal("CheckpointOtherEJB/SGCheckpointBeanI").verify();
        lookupLocal("CheckpointOtherEJB/SGCheckpointBeanJ").verify();
        lookupLocal("CheckpointOtherEJB/SGCheckpointBeanK").verify();
        lookupLocal("CheckpointOtherEJB/SLCheckpointBeanG").verify();
        lookupLocal("CheckpointOtherEJB/SLCheckpointBeanH").verify();
        lookupLocal("CheckpointOtherEJB/SLCheckpointBeanI").verify();
        lookupLocal("CheckpointOtherEJB/SLCheckpointBeanJ").verify();
        lookupLocal("CheckpointOtherEJB/SLCheckpointBeanK").verify();
        lookupLocal("CheckpointOtherEJB/SLCheckpointBeanL").verify();

        lookupLocal("CheckpointWeb/SGCheckpointBeanM").verify();
        lookupLocal("CheckpointWeb/SGCheckpointBeanN").verify();
        lookupLocal("CheckpointWeb/SGCheckpointBeanO").verify();
        lookupLocal("CheckpointWeb/SGCheckpointBeanP").verify();
        lookupLocal("CheckpointWeb/SGCheckpointBeanQ").verify();
        lookupLocal("CheckpointWeb/SLCheckpointBeanM").verify();
        lookupLocal("CheckpointWeb/SLCheckpointBeanN").verify();
        lookupLocal("CheckpointWeb/SLCheckpointBeanO").verify();
        lookupLocal("CheckpointWeb/SLCheckpointBeanP").verify();
        lookupLocal("CheckpointWeb/SLCheckpointBeanQ").verify();
        lookupLocal("CheckpointWeb/SLCheckpointBeanR").verify();

        lookupLocal("CheckpointOtherWeb/SGCheckpointBeanS").verify();
        lookupLocal("CheckpointOtherWeb/SGCheckpointBeanT").verify();
        lookupLocal("CheckpointOtherWeb/SGCheckpointBeanU").verify();
        lookupLocal("CheckpointOtherWeb/SGCheckpointBeanV").verify();
        lookupLocal("CheckpointOtherWeb/SGCheckpointBeanW").verify();
        lookupLocal("CheckpointOtherWeb/SLCheckpointBeanS").verify();
        lookupLocal("CheckpointOtherWeb/SLCheckpointBeanT").verify();
        lookupLocal("CheckpointOtherWeb/SLCheckpointBeanU").verify();
        lookupLocal("CheckpointOtherWeb/SLCheckpointBeanV").verify();
        lookupLocal("CheckpointOtherWeb/SLCheckpointBeanW").verify();
        lookupLocal("CheckpointOtherWeb/SLCheckpointBeanX").verify();
    }

    private static CheckpointLocal lookupLocal(String beanName) {
        try {
            return (CheckpointLocal) new InitialContext().lookup("java:app/" + beanName);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}
