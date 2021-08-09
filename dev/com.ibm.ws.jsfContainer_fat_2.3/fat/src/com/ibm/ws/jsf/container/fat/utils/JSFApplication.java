/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat.utils;

import java.util.Arrays;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf.container.fat.FATSuite;

import componenttest.topology.impl.LibertyServer;

public class JSFApplication implements TestRule {
    protected static final Class<?> c = JSFApplication.class;
    private LibertyServer server;

    public JSFApplication(LibertyServer server) {
        this.server = server;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new InstallAppStatement(base, description, server);
    }

    public class InstallAppStatement extends Statement {
        private final Statement base;
        private final Description description;
        private WebArchive mojarraApp;
        private WebArchive myfacesApp;
        private LibertyServer server;

        public InstallAppStatement(Statement base, Description description, LibertyServer server) {
            this.base = base;
            this.description = description;
            this.server = server;
        }

        @Override
        public void evaluate() throws Throwable {
            String appName = null;
            String[] pkgs = null;

            /* Determine the web archive name and packages. */
            WebArchiveInfo classWebArchiveInfo = description.getTestClass().getAnnotation(WebArchiveInfo.class);
            if (classWebArchiveInfo != null) {
                appName = classWebArchiveInfo.name();
                pkgs = classWebArchiveInfo.pkgs();
            }

            /* Override the web archive name and package if specified on the test method. */
            WebArchiveInfo methodWebArchiveInfo = description.getAnnotation(WebArchiveInfo.class);
            if (methodWebArchiveInfo != null) {
                appName = methodWebArchiveInfo.name();
                pkgs = methodWebArchiveInfo.pkgs();
            }

            Log.info(c, description.getMethodName(), "appName = " + appName);
            Log.info(c, description.getMethodName(), "pkgs = " + Arrays.toString(pkgs));

            /* Determine which libraries to test with. */
            UseImplementation classAnnotation = description.getTestClass().getAnnotation(UseImplementation.class);
            JSFImplementation defaultImpl = (classAnnotation == null) ? JSFImplementation.BOTH : classAnnotation.value();

            /* Override which libraries to test with if specified on the test method. */
            UseImplementation implAnnotation = description.getAnnotation(UseImplementation.class);
            JSFImplementation impl = (implAnnotation == null) ? defaultImpl : implAnnotation.value();

            Log.info(c, description.getMethodName(), "defaultImpl = " + defaultImpl);
            Log.info(c, description.getMethodName(), "impl = " + impl);

            /* Construct the web archives. */
            String archiveName = appName + ".war";
            try {
                if (impl == JSFImplementation.BOTH || impl == JSFImplementation.MOJARRA) {
                    mojarraApp = ShrinkWrap.create(WebArchive.class, archiveName).addPackages(false, pkgs);
                    mojarraApp = FATSuite.addMojarra(mojarraApp);
                    mojarraApp = (WebArchive) ShrinkHelper.addDirectory(mojarraApp, "publish/files/permissions");
                    mojarraApp = (WebArchive) ShrinkHelper.addDirectory(mojarraApp, "test-applications/" + appName + "/resources");
                }

                if (impl == JSFImplementation.BOTH || impl == JSFImplementation.MYFACES) {
                    myfacesApp = ShrinkWrap.create(WebArchive.class, archiveName).addPackages(false, pkgs);
                    myfacesApp = FATSuite.addMyFaces(myfacesApp);
                    myfacesApp = (WebArchive) ShrinkHelper.addDirectory(myfacesApp, "publish/files/permissions");
                    myfacesApp = (WebArchive) ShrinkHelper.addDirectory(myfacesApp, "test-applications/" + appName + "/resources");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            /* Evaluate the tests. */
            if (impl == JSFImplementation.BOTH || impl == JSFImplementation.MOJARRA) {
                ShrinkHelper.exportToServer(server, "dropins", mojarraApp);
                server.addInstalledAppForValidation(appName);
                Log.info(c, description.getMethodName(), "STARTED MOJARRA TEST");
                base.evaluate();
                server.removeInstalledAppForValidation(appName);
            }

            if (impl == JSFImplementation.BOTH || impl == JSFImplementation.MYFACES) {
                ShrinkHelper.exportToServer(server, "dropins", myfacesApp);
                server.addInstalledAppForValidation(appName);
                Log.info(c, description.getMethodName(), "STARTED MYFACES TEST");
                base.evaluate();
                server.removeInstalledAppForValidation(appName);
            }
        }
    }
}
