/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.embeddable;

import java.util.Map;
import java.util.concurrent.Future;

/**
 * An embedded Liberty server instance.
 * <p>
 * Server instances are obtained using the {@link ServerBuilder}. The methods of this interface
 * can be used to manage and control the server instance.
 * <p>
 * There are some limitations when working with an embedded server: environment variables
 * are not checked, and <code>jvm.options</code> and <code>server.env</code> files are not read.
 * Management of the JVM and environment is assumed to be managed by the caller.
 * <p>
 * <code>bootstrap.properties</code> and <code>server.xml</code> files will be read as normal.
 * <p>
 * Consumers of this SPI must not implement this interface. A server instance is thread safe.
 * 
 * @see ServerBuilder#build()
 */
public interface Server {

    /**
     * Result of a start or stop operation. Calling {@link Future#get()} on
     * the return result will block until the operation is complete (the server
     * has finished starting, stopping, or the operation has failed). This
     * can then be used to query the outcome.
     * <p>
     * This provides an integer return code, which will match those documented
     * for command line invocation, and a <code>Throwable</code>, if an exception
     * occurred while executing the command. If a <code>Throwable</code> is returned,
     * it will have a translated message suitable for display to end users.
     * <p>
     * Consumers of this SPI must not implement this interface.
     */
    static interface Result {
        /**
         * Convenience method.
         * 
         * @return true if the operation was successful.
         */
        boolean successful();

        /**
         * Check the return code value of the operation. The values will match those documented
         * in the infocenter for command line invocation
         * 
         * @return an integer return code value. In summary:
         *         <ul><li>0 for success,</li><li>20 for bad arguments, </li><li>> 20 for other error conditions.</li></ul>
         * 
         * @see <a
         *      href="http://pic.dhe.ibm.com/infocenter/wasinfo/v8r5/topic/com.ibm.websphere.wlp.nd.multiplatform.doc/ae/rwlp_command_server.html">Liberty&nbsp;profile:&nbsp;server&nbsp;command&nbsp;options</a>
         */
        int getReturnCode();

        /**
         * @return a <code>ServerException</code> with a translated message if an exception occurred while processing the operation, or null.
         */
        ServerException getException();
    }

    /**
     * Start the Liberty server instance using the given arguments. The arguments used
     * here are the same as would be passed to the command line.
     * 
     * @param arguments Command line arguments for start
     * @return a Future representing pending completion of the start operation
     */
    Future<Result> start(String... arguments);

    /**
     * Start the Liberty server instance using the given arguments. The arguments used
     * here are the same as would be passed to the command line.
     * 
     * @param props Additional properties that will supersede any values read from
     *            <code>bootstrap.properties</code> or <code>System.properties</code>
     * @param arguments Command line arguments for start
     * @return a Future representing pending completion of the start operation
     */
    Future<Result> start(Map<String, String> props, String... arguments);

    /**
     * Stop the Liberty server instance.
     * 
     * @param arguments Command line arguments for stop
     * @return a Future representing pending completion of the stop operation
     */
    Future<Result> stop(String... arguments);

    /**
     * @return true if this server instance is running
     */
    boolean isRunning();
}
