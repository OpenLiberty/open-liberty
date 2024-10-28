/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.junit;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.log.Log;
import batch.fat.util.BatchFATHelper;
import componenttest.annotation.CheckpointTest;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jbatch.test.FatUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * The DynamicConfigRuleCheckpoint takes a list of server.xml file names and runs the
 * test(s) multiple times, once for each server.xml file. The server.xml updates
 * are applied dynamically (i.e the server is not restarted).
 *
 */
@RunWith(FATRunner.class)
public class DynamicConfigRuleCheckpoint implements TestRule {

    public static void log(String method, String msg) {
        Log.info(DynamicConfigRuleCheckpoint.class, method, msg);
    }

    /**
     * A function that does setup of a {@code LibertyServer}.
     *
     * @param <T>
     */
    @FunctionalInterface
    public static interface ServerSetup<T extends Throwable> {
        /**
         * Setup the server
         * @return      the checkpointed server
         * @throws T
         */
        public LibertyServer call() throws T;
    }

    /**
     * A function that does some action.
     *
     * @param <T>
     */
    @FunctionalInterface
    public static interface Action<T extends Throwable> {
        /**
         * Perform some action
         */
        public void call() throws T;
    }

    private ServerSetup<?> initialSetup;

    private Action<?> finalTearDown;
    
    private Action<?> beforeEach;

    private LibertyServer server;

    /**
     * The list of server.xml config files to run the tests against.
     */
    private final List<String> serverXmlFileNames = new ArrayList<String>();

    /**
     * Sets the mandatory function that does server setup for the normal and checkpoint mode for the test
     *
     * @param  <T>
     * @param  serverSetup
     * @return
     */
    public <T extends Throwable> DynamicConfigRuleCheckpoint setInitialSetup(ServerSetup<T> initialSetup) {
        this.initialSetup = initialSetup;
        return this;
    }

    /**
     * @param finalTearDown Executed after iterating thru ALL server xmls.
     *
     * @return this
     */
    public <T extends Throwable> DynamicConfigRuleCheckpoint setFinalTearDown(Action<T> finalTearDown) {
        this.finalTearDown = finalTearDown;
        return this;
    }

    /**
     * @param beforeEach Executed before each iteration of the server.xmls
     *
     * @return this
     */
    public <T extends Throwable> DynamicConfigRuleCheckpoint setBeforeEach(Action<T> beforeEach) {
        this.beforeEach = beforeEach;
        return this;
    }

    /**
     * @param serverXmlFileName Add a server.xml file to the list
     *
     * @return this
     */
    public DynamicConfigRuleCheckpoint addServerXml(String serverXmlFileName) {
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
            assertNotNull("initialSetup must not be null.", initialSetup);
            assertNotNull("beforeEach must not be null.", beforeEach);
            assertNotNull("finalTearDown must not be null.", finalTearDown);
            log("evaluate", "running initial setup");

            server = initialSetup.call();

            try {
                iterateServerXmls();

            } finally {
                finalTearDown.call();
            }
        }

        /**
         * Iterate thru all server.xml and run the suite of tests against each.
         */
        protected void iterateServerXmls() throws Throwable {

            server.setMarkToEndOfLog();
            for (String serverXmlFileName : serverXmlFileNames) {

                log("evaluate", "setting server.xml to " + serverXmlFileName);

                server.setServerConfigurationFile(serverXmlFileName);
                server.checkpointRestore();

                beforeEach.call();

                try {
                    base.evaluate();
                } finally {
                    // TODO: if a test fails will I be able to run the other configs
                    //       and still report the test failure?
                }

                server.stopServer();
            }
        }

    }

}

