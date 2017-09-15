/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import test.server.BaseHttpTest;

public class TestRunner extends BaseHttpTest {

    private BundleContext bundleContext;
    private final List<Test> tests = new ArrayList<Test>();

    @Override
    protected void activate(ComponentContext componentContext) throws Exception {
        super.activate(componentContext);

        bundleContext = componentContext.getBundleContext();
        ServiceReference ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        ConfigurationAdmin configAdmin = (ConfigurationAdmin) bundleContext.getService(ref);

        addTest(new SingletonServerTest("test.config.singleton.server"));
        addTest(new SingletonServerMetaTypeTest("test.config.singleton.server.metatype"));
        addTest(new SingletonMetaTypeTest("test.config.singleton.metatype"));

        addTest(new FactorySimpleTest("test.config.factory.simple", 1));
        addTest(new FactoryMetaTypeTest("test.config.factory.metatype"));

        addTest(new VariableTest("test.config.variable"));

        addTest(new SingletonAliasTest("test.config.singleton.alias"));
        addTest(new FactoryAliasTest("test.config.factory.alias", 2));

        addTest(new SharedAliasTest("test.config.simpsons.alias", "Homer", "Simpson"));
        addTest(new SharedAliasTest("test.config.griffins.alias", "Peter", "Griffin"));

        addTest(new NestedConfigElementTest("test.config.nested.elements", configAdmin));
        addTest(new NestedConfigComplexElementTest("test.config.complex.nested.elements", configAdmin));

        addTest(new PasswordTypeTest("test.config.password"));

        addTest(new FinalTypeTest("test.config.final"));

        NestedSingletonTest nestedSingletonTest = new NestedSingletonTest("test.config.nested.managed", "test.config.nested.result", configAdmin);
        nestedSingletonTest.setExpectedProperties(NestedSingletonTest.getExpectedProperties());
        addTest(nestedSingletonTest);

        NestedResultTest nestedResultTest = new NestedResultTest("test.config.nested.result.metatype", 2);
        nestedResultTest.setExpectedProperties(NestedSingletonTest.getMetatypeExpectedProperties());

        addTest(nestedResultTest);

        tests.add(new ConfigurationAdminTest("test.config.configAdmin", configAdmin));

        addTest(new ConfigurationPluginTest("test.config.plugin"));

        registerServlet("/parser-test", new TestVerifierServlet(), null, null);

        System.out.println("Parser test servlet started");
    }

    private void addTest(Test test) {
        Hashtable properties = new Hashtable();
        properties.put(Constants.SERVICE_PID, test.getName());
        tests.add(test);
        bundleContext.registerService(test.getServiceClasses(), test, properties);
    }

    class TestVerifierServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        @Override
        public void doGet(HttpServletRequest rq, HttpServletResponse rsp) throws IOException {
            int failedCount = 0;
            boolean failed = false;
            for (Test test : tests) {
                Throwable t = test.getException();
                if (t != null) {
                    failed = true;
                    System.out.println("Test " + test.getName() + " failed:");
                    t.printStackTrace();
                    failedCount++;
                }
            }
            PrintWriter pw = rsp.getWriter();
            rsp.setContentType("text/plain");
            if (failed) {
                pw.println(failedCount + " of " + tests.size() + " tests " + "FAILED, check messages.log for information");
            } else {
                pw.println("OK");
            }
        }

    }

}
