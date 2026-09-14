/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.config.fat.tests.visibility.war;

import static com.ibm.ws.microprofile.appConfig.test.utils.TestUtils.assertContains;
import static com.ibm.ws.microprofile.appConfig.test.utils.TestUtils.assertNotContains;
import static com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.VisibilityTestConstants.EAR_LIB_CONFIG_PROPERTY;
import static com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.VisibilityTestConstants.EAR_LIB_VALUE;
import static com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.VisibilityTestConstants.EJB_JAR_CONFIG_PROPERTY;
import static com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.VisibilityTestConstants.EJB_JAR_VALUE;
import static com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.VisibilityTestConstants.WAR_CONFIG_PROPERTY;
import static com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.VisibilityTestConstants.WAR_VALUE;

import java.util.Arrays;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Test;

import com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.EarLibBean;
import com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.EarLibDependentBean;
import com.ibm.ws.microprofile.config.fat.tests.visibility.ejbjar.VisibilityTestEjb;

import componenttest.app.FATServlet;

/**
 * Tests which config sources can be seen from different locations within an .ear
 * <p>
 * Each test uses one method of reading config properties from one location within the .ear
 */
@WebServlet("/testWar")
public class VisibilityTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    private VisibilityTestEjb testEjb;

    @Inject
    private EarLibBean earLibBean;

    @Inject
    private EarLibDependentBean earLibDependentBean;

    @Inject
    private Config injectedConfig;

    @Inject
    @ConfigProperty(name = WAR_CONFIG_PROPERTY)
    private Optional<String> warProperty;

    @Inject
    @ConfigProperty(name = EJB_JAR_CONFIG_PROPERTY)
    private Optional<String> ejbJarProperty;

    @Inject
    @ConfigProperty(name = EAR_LIB_CONFIG_PROPERTY)
    private Optional<String> earLibProperty;

    @PostConstruct
    private void initializeInjectedConfig() {
        // Initialize the injected Config object before running any tests
        // This should ensure it's initialized in the context of the .war, even if one of the EJB tests is the first to run
        // This is important because mpConfig-1.x uses an ApplicationScoped Config bean which will use whichever TCCL is active when it's initialized
        // and we want that to be consistent regardless of the order tests are run

        // Calling any method is sufficient to initialize the bean
        injectedConfig.getConfigSources();
    }

    @Test
    public void testGetConfigFromWar() {
        // GetConfig from this .war
        Config config = ConfigProvider.getConfig();

        // Expect to see properties from all locations visible on classpath
        assertContains(config, WAR_CONFIG_PROPERTY, WAR_VALUE);
        assertContains(config, EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(config, EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
    }

    @Test
    public void testGetConfigFromEjbJar() {
        // GetConfig from an ejb
        Config config = testEjb.getConfig();

        // Expect to see properties from all locations visible on classpath
        assertContains(config, EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(config, EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
        assertNotContains(config, WAR_CONFIG_PROPERTY);
    }

    @Test
    public void testGetConfigFromEarLib() {
        // getConfig called from app scoped ear lib jar
        Config config = earLibBean.getConfig();

        // Expect to see everything visible to the .war, since the TCCL is the war classloader when getConfig was called
        assertContains(config, EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(config, EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
        assertContains(config, WAR_CONFIG_PROPERTY, WAR_VALUE);

        // Same bean obtained via EJB
        config = testEjb.getEarLibBean().getConfig();

        // Expect to see everything visible to the .war
        // We get the bean via the EJB, but we call the bean ourselves, so it's the .war TCCL that's active
        assertContains(config, EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(config, EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
        assertContains(config, WAR_CONFIG_PROPERTY, WAR_VALUE);

        // Test with dependent-scoped ear lib jar
        config = earLibDependentBean.getConfig();

        // Expect same results as app-scoped bean
        // The bean creation has no impact when calling getConfig
        assertContains(config, EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(config, EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
        assertContains(config, WAR_CONFIG_PROPERTY, WAR_VALUE);

        // Test with dependent-scoped ear lib jar obtained via EJB
        config = testEjb.getEarLibDependentBean().getConfig();

        // Expect to see everything visible to the .war
        // We get the bean via the EJB, but we call the bean ourselves, so it's the .war TCCL that's active
        assertContains(config, EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(config, EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
        assertContains(config, WAR_CONFIG_PROPERTY, WAR_VALUE);
    }

    @Test
    public void testInjectedConfigFromWar() {
        // Config injected into .war
        Config config = injectedConfig;

        // Expect to see properties from all locations visible on war classpath
        assertContains(config, WAR_CONFIG_PROPERTY, WAR_VALUE);
        assertContains(config, EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(config, EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
    }

    @Test
    public void testInjectedConfigFromEjbJar() {
        // Config injected into EJB
        Config config = testEjb.getInjectedConfig();

        // Expect to see properties from all locations visible on EJB jar classpath
        assertContains(config, EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(config, EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);

        if (isMpConfig1x()) {
            // In mpConfig 1.x, the injected Config bean is ApplicationScoped, and reports the same properties everywhere
            // It gets initialized wherever it's first used (which is from the .war)
            assertContains(config, WAR_CONFIG_PROPERTY, WAR_VALUE);
        } else {
            // In mpConfig 2.0 and later, the injected Config bean is Dependent-scoped and will take its values from the EJB module TCCL
            assertNotContains(config, WAR_CONFIG_PROPERTY);
        }
    }

    @Test
    public void testInjectedConfigFromEarLib() {
        // Config injected into app scoped ear lib jar
        Config config = earLibBean.getInjectedConfig();

        // Expect to see everything visible to the .war, since the war TCCL was active when the bean was created
        assertContains(config, EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(config, EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
        assertContains(config, WAR_CONFIG_PROPERTY, WAR_VALUE);

        // Same bean obtained via EJB
        config = testEjb.getEarLibBean().getInjectedConfig();

        // Expect to see everything visible to the .war
        // The bean is app scoped, the injected config is not changed after the bean is created
        // The bean was created with the war TCCL
        assertContains(config, EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(config, EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
        assertContains(config, WAR_CONFIG_PROPERTY, WAR_VALUE);

        // Test with dependent-scoped ear lib jar
        config = earLibDependentBean.getInjectedConfig();

        // Expect same results as app-scoped bean
        // Expect to see everything visible to the .war, since the war TCCL was active when the bean was created
        assertContains(config, EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(config, EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
        assertContains(config, WAR_CONFIG_PROPERTY, WAR_VALUE);

        // Test with dependent-scoped ear lib jar obtained via EJB
        config = testEjb.getEarLibDependentBean().getInjectedConfig();

        // Expect to see only values visible to the EJB jar
        // Dependent scope means the bean will be created when the EJB instance is created, with the EJB TCCL active
        assertContains(config, EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(config, EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);

        if (isMpConfig1x()) {
            // In mpConfig 1.x, the injected Config bean is ApplicationScoped, and reports the same properties everywhere
            // It gets initialized wherever it's first used (which is from the .war)
            assertContains(config, WAR_CONFIG_PROPERTY, WAR_VALUE);
        } else {
            // In mpConfig 2.0 and later, the injected Config bean is Dependent-scoped and will take its values from the EJB module TCCL
            assertNotContains(config, WAR_CONFIG_PROPERTY);
        }
    }

    @Test
    public void testInjectedPropertyFromWar() {
        // Behaviour when injecting config properties is the same as when injecting the Config itself
        // Expect to see properties from all locations visible on war classpath
        assertContains(warProperty, WAR_CONFIG_PROPERTY, WAR_VALUE);
        assertContains(ejbJarProperty, EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(earLibProperty, EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
    }

    @Test
    public void testInjectedPropertyFromEjbJar() {
        // Behaviour when injecting config properties is the same as when injecting the Config itself
        // Expect to see properties from all locations visible on EJB jar classpath
        assertContains(testEjb.getEjbJarProperty(), EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(testEjb.getEarLibProperty(), EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
        assertNotContains(testEjb.getWarProperty(), WAR_CONFIG_PROPERTY);
    }

    @Test
    public void testInjectedPropertyFromEarLib() {
        // Behaviour when injecting config properties is the same as when injecting the Config itself

        // Properties injected into app-scoped ear lib jar
        // Expect to see everything visible to the .war, since the war TCCL was active when the bean was created
        assertContains(earLibBean.getEjbJarProperty(), EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(earLibBean.getEarLibProperty(), EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
        assertContains(earLibBean.getWarProperty(), WAR_CONFIG_PROPERTY, WAR_VALUE);

        // Properties injected into same bean, obtained via EJB
        // Expect to see everything visible to the .war, since the bean was created and injected when the .war TCCL was active
        assertContains(testEjb.getEarLibBean().getEjbJarProperty(), EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(testEjb.getEarLibBean().getEarLibProperty(), EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
        assertContains(testEjb.getEarLibBean().getWarProperty(), WAR_CONFIG_PROPERTY, WAR_VALUE);

        // Test with a dependent-scoped ear lib jar
        // Expect to see everything visible to the .war, since the war TTCL was active when the bean was created
        assertContains(earLibDependentBean.getEjbJarProperty(), EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(earLibDependentBean.getEarLibProperty(), EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
        assertContains(earLibDependentBean.getWarProperty(), WAR_CONFIG_PROPERTY, WAR_VALUE);

        // Expect to see only values visible to the EJB jar
        // Dependent scope means the bean will be created when the EJB instance is created, with the EJB TCCL active
        assertContains(testEjb.getEarLibDependentBean().getEjbJarProperty(), EJB_JAR_CONFIG_PROPERTY, EJB_JAR_VALUE);
        assertContains(testEjb.getEarLibDependentBean().getEarLibProperty(), EAR_LIB_CONFIG_PROPERTY, EAR_LIB_VALUE);
        assertNotContains(testEjb.getEarLibDependentBean().getWarProperty(), WAR_CONFIG_PROPERTY);
    }

    /**
     * Check whether we're testing mpConfig-1.x
     *
     * @return {@code true} if running with MP Config 1.x, otherwise {@code false}
     */
    private boolean isMpConfig1x() {
        // Check for the existence of Config.getValues methods, added in MP Config 2.0
        // It's a bit of a hack but it's the easiest way to check the feature version from within an app
        // and avoids lots of duplication in the tests
        boolean hasGetValues = Arrays.stream(Config.class.getMethods())
                                     .anyMatch(m -> m.getName().equals("getValues"));
        return !hasGetValues;
    }

}
