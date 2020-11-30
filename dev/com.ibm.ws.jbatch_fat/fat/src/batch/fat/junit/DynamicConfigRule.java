/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.junit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * The DynamicConfigRule takes a list of server.xml file names and runs the
 * test(s) multiple times, once for each server.xml file. The server.xml updates
 * are applied dynamically (i.e the server is not restarted).
 *
 */
@RunWith(FATRunner.class)
public class DynamicConfigRule implements TestRule {

    public static void log(String method, String msg) {
        Log.info(DynamicConfigRule.class, method, msg);
    }

    private Callable<?> initialSetup;

    private Callable<?> finalTearDown;

    private Callable<?> afterEach;

    private LibertyServer server;

    /**
     * The list of server.xml config files to run the tests against.
     */
    private final List<String> serverXmlFileNames = new ArrayList<String>();

    /**
     * @param server The LibertyServer to use
     *
     * @return this
     */
    public DynamicConfigRule setServer(LibertyServer server) {
        this.server = server;
        return this;
    }

    /**
     * @param initialSetup Executed prior to iterating thru the server xmls.
     *
     * @return this
     */
    public DynamicConfigRule setInitialSetup(Callable<?> initialSetup) {
        this.initialSetup = initialSetup;
        return this;
    }

    /**
     * @param finalTearDown Executed after iterating thru ALL server xmls.
     *
     * @return this
     */
    public DynamicConfigRule setFinalTearDown(Callable<?> finalTearDown) {
        this.finalTearDown = finalTearDown;
        return this;
    }

    /**
     * @param afterEach Executed after each iteration of the server.xmls
     *
     * @return this
     */
    public DynamicConfigRule setAfterEach(Callable<?> afterEach) {
        this.afterEach = afterEach;
        return this;
    }

    /**
     * @param serverXmlFileName Add a server.xml file to the list
     *
     * @return this
     */
    public DynamicConfigRule addServerXml(String serverXmlFileName) {
        this.serverXmlFileNames.add(serverXmlFileName);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Statement apply(Statement base, Description desc) {
        return new DynamicConfigStatement(base);
    }

    /**
     * Statement class - performs the before/after operations around a
     * call to the base Statement's evaulate() method (which runs the test).
     */
    protected class DynamicConfigStatement extends Statement {

        /**
         * A reference to the Statement that this Statement wraps around.
         */
        private final Statement base;

        /**
         * CTOR.
         *
         * @param base The Statement that this Statement wraps around.
         */
        public DynamicConfigStatement(Statement base) {
            this.base = base;
        }

        /**
         * This method is called by the test runner in order to execute the test.
         *
         * Before/After logic is embedded here around a call to base.evaluate(),
         * which processes the Statement chain (for any other @Rules that have been
         * applied) until at last the test method is executed.
         *
         */
        @Override
        public void evaluate() throws Throwable {

            log("evaluate", "running initial setup");

            if (initialSetup != null) {
                initialSetup.call();
            }

            try {
                iterateServerXmls();

            } finally {

                if (finalTearDown != null) {
                    finalTearDown.call();
                }
            }
        }

        /**
         * Iterate thru all server.xml and run the suite of tests against each.
         */
        protected void iterateServerXmls() throws Throwable {

            for (String serverXmlFileName : serverXmlFileNames) {

                log("evaluate", "setting server.xml to " + serverXmlFileName);

                server.setMarkToEndOfLog();
                server.setServerConfigurationFile(serverXmlFileName);
                server.waitForConfigUpdateInLogUsingMark(null);

                try {
                    base.evaluate();
                } finally {
                    // TODO: if a test fails will I be able to run the other configs
                    //       and still report the test failure?
                }

                if (afterEach != null) {
                    afterEach.call();
                }
            }
        }

    }

}
