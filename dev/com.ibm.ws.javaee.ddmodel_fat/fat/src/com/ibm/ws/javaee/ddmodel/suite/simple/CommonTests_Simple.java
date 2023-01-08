/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.suite.simple;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.javaee.ddmodel.suite.CommonTests;
import com.ibm.ws.javaee.ddmodel.suite.util.FailableBiConsumer;

import componenttest.topology.impl.LibertyServer;

public abstract class CommonTests_Simple extends CommonTests {

    protected static WebArchive createSimpleWar(boolean useWebExt) {
        WebArchive simpleWar = ShrinkWrap.create(WebArchive.class, "Simple.war");
        simpleWar.addPackage("servlettest.web");
        addResource(simpleWar, "WEB-INF/web.xml", DEFAULT_SUFFIX);
        if ( useWebExt ) {
            addResource(simpleWar, "WEB-INF/ibm-web-ext.xml", DEFAULT_SUFFIX);
        }
        return simpleWar;
    }

    protected static EnterpriseArchive createSimpleEar(
            Class<?> testClass, boolean useWebExt, boolean useAppExt) {
        
        String earName = "Simple.ear";

        EnterpriseArchive testEar = ShrinkWrap.create(EnterpriseArchive.class, earName);

        testEar.addAsModules( createSimpleWar(useWebExt) );

        String resourcePath = ( useAppExt ? "META-INF/application_ext.xml" : "META-INF/application_noext.xml" );
        String targetPath = "META-INF/application.xml";
        
        if ( useAppExt ) {
            addResource(testEar, earName, resourcePath, DEFAULT_SUFFIX, targetPath);
        }
        
        return testEar;
    };    
    
    protected static final boolean USE_WEB_EXT = true;
    protected static final boolean USE_APP_EXT = true;

    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpSimpleWarNoExt =
        (Class<?> testClass, LibertyServer server) -> {
            setUpSimpleWar(testClass, server, !USE_WEB_EXT);
        };
            
    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpSimpleWarExt =
        (Class<?> testClass, LibertyServer server) -> {
            setUpSimpleWar(testClass, server, USE_WEB_EXT);
    };            

    protected static void setUpSimpleWar(Class<?> testClass, LibertyServer server,
            boolean useWebExt)
        throws Exception {

        ShrinkHelper.exportAppToServer( server,
                                        createSimpleWar(useWebExt),
                                        DeployOptions.SERVER_ONLY );
    }

    //

    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpSimpleEar_NoExt =
        (Class<?> testClass, LibertyServer server) -> {
            setUpSimpleEar(testClass, server, !USE_WEB_EXT, !USE_APP_EXT);
    };
                
    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpSimpleEar_WebExt =
        (Class<?> testClass, LibertyServer server) -> {
            setUpSimpleEar(testClass, server, USE_WEB_EXT, !USE_APP_EXT);
    };
                        
    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpSimpleEar_AppExt =
        (Class<?> testClass, LibertyServer server) -> {
            setUpSimpleEar(testClass, server, !USE_WEB_EXT, USE_APP_EXT);
    };
                                
    protected static FailableBiConsumer<Class<?>, LibertyServer> setUpSimpleEar_WebAppExt =
        (Class<?> testClass, LibertyServer server) -> {
            setUpSimpleEar(testClass, server, USE_WEB_EXT, USE_APP_EXT);
    };

    protected static void setUpSimpleEar(
        Class<?> testClass, LibertyServer server,
        boolean useWebExt, boolean useAppExt) throws Exception {
        ShrinkHelper.exportAppToServer( server,
                                        createSimpleEar(testClass, useWebExt, useAppExt),
                                        DeployOptions.SERVER_ONLY );
    }

    //

    public static final String HELLO_TEST = "testHello";
    public static final String HELLO_LINE = "Hello";
    
    public static final String SIMPLE_CONTEXT_ROOT = "Simple";

    public static final String WEB_CONTEXT_ROOT = "web";
    public static final String APP_CONTEXT_ROOT = "app";
    public static final String CFG_CONTEXT_ROOT = "cfg";
    public static final String EXT_CONTEXT_ROOT = "ext";
    public static final String EXP_CONTEXT_ROOT = "exp";

    protected static void testHello(Class<?> testClass, String contextRoot)
        throws MalformedURLException {

        test(testClass, HELLO_TEST, contextRoot, HELLO_LINE, null);
    }
    
    protected static void testHelloNotFound(Class<?> testClass, String contextRoot)
        throws MalformedURLException {

        test(testClass, HELLO_TEST, contextRoot, HELLO_LINE, FileNotFoundException.class);
    }
}
