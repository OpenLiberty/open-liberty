/*******************************************************************************
 * Copyright (c) 2005, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.io.async;

import java.security.AccessController;
import java.security.PrivilegedAction;

//import java.util.MissingResourceException;
//import java.util.ResourceBundle;
//import java.util.logging.Level;

class AsyncProperties {

    // No need to use properties files right now, when running in WAS or the JDK
    static final String aio_handle_unavailable = "Unable to get socket handle";
    static final String aio_invalid_mode = "Mode must be one of 'r' or 'rw' ";
    static final String aio_internal_error = "Async IO Internal Error";
    static final String aio_direct_buffers_only = "Only direct buffers allowed";
    static final String aio_error_setting_buffer_position = "Failed to set new buffer position";
    static final String aio_operation_failed = "Async IO operation failed";
    static final String aio_operation_cancelled = "Async operation was cancelled";
    static final String aio_operation_timedout = "Async operation timed out";
    static final String aio_failed_to_set_buffer_position = "Failed to set new buffer position to {0}";

    // # Sets the maximum number of handler threads that can be active in the system
    // # at any one time
    // # Condition: maxHandlers > 0
    // com.ibm.io.async.HandlerPool.maxHandlers = 10
    static int maxHandlers = 10;

    // # Sets the minimum number of threads that should be blocked waiting on the
    // # completion of async IO events.
    // # Condition: minEventHandlers <= maxHandlers
    // com.ibm.io.async.HandlerPool.minEventHandlers = 1
    static int minEventHandlers = 1;

    // # Sets the maximum number of threads that should be blocked waiting on the
    // # completion of async IO events.
    // # Condition: minEventHanders <= maxEventHandlers
    // com.ibm.io.async.HandlerPool.maxEventHandlers = 10
    // static int maxEventHandlers = 10;

    // # Sets the timeout value for threads blocked on the async completion port.
    // # If a thread wakes after a timeout it will return to Java and re-enter the
    // # completion port blocking subject to the handler pool policy.
    // # Condition: integer number of milliseconds
    // com.ibm.io.async.ResultHandler.completionTimeout = 20000
    static int completionTimeout = 0;

    // # Sets the length of time the timer thread should sleep between pulling things off the queue.
    // # Condition: integer number of milliseconds
    static int timerSleepTime = 1024;

    // # Sets the number of items the timer should take off the queue at one time.
    // # Condition: integer number of elements to remove
    static int timerBatchSize = 500;

    // # Sets the number of queues to be used by the timer.
    // # Condition: integer number of queues to use
    static int numTimerQueues = 2;

    // # Sets if the timer should sleep everytime after reading elements off of the queue.
    // # Condition: true = always sleep between queue reads
    static boolean timerSleepAlways = true;

    // # Sets the maximum number of threads that can wait on events concurrently.
    static Integer maxThreadsWaitingForEvents = null;

    // # Sets if timeouts should be disabled.
    // # Condition: true = disable timeouts

    static boolean disableTimeouts = false;

    // # Sets the name of the natives library containing the default provider calls.
    static String libraryName = "ibmaio";

    // # defines the default completion key pool size
    static int COMPLETION_KEY_POOL_SIZE_DEFAULT = 4096;

    // # Sets the size of the completion key pool.
    // # Condition: integer pool size
    static String sCompKeyPoolSize = null;

    // Sets the maximum number of completed IO Events that can be returned on a batched request
    static int maximumBatchedEvents = 32;

    static {
        AccessController.doPrivileged(new PrivSysPropCheck());
    }

    static class PrivSysPropCheck implements PrivilegedAction<Object> {
        /** Constructor */
        public PrivSysPropCheck() {
            // nothing to do
        }

        @Override
        public Object run() {
            // TODO convert from sys props to bundle props
            if (System.getProperty("com.ibm.io.async.AIOTimerSleepTime") != null) {
                timerSleepTime = Integer.parseInt(System.getProperty("com.ibm.io.async.AIOTimerSleepTime"));
            }

            if (System.getProperty("com.ibm.io.async.AIOTimerBatchSize") != null) {
                timerBatchSize = Integer.parseInt(System.getProperty("com.ibm.io.async.AIOTimerBatchSize"));
            }

            if (System.getProperty("com.ibm.io.async.AIOTimerNumQueues") != null) {
                numTimerQueues = Integer.parseInt(System.getProperty("com.ibm.io.async.AIOTimerNumQueues"));
            }

            timerSleepAlways = Boolean.parseBoolean(System.getProperty("com.ibm.io.async.AIOTimerSleepAlways"));

            maxThreadsWaitingForEvents = Integer.getInteger("com.ibm.io.async.AIOMaxThreadsWaitingForEvents", null);

            disableTimeouts = Boolean.parseBoolean(System.getProperty("com.ibm.io.async.AIODisableTimeouts"));

            if (System.getProperty("com.ibm.io.async.AIOCompKeyPoolSize") != null) {
                sCompKeyPoolSize = System.getProperty("com.ibm.io.async.AIOCompKeyPoolSize");
            }

            if (System.getProperty("com.ibm.io.async.AIOMaxBatchedCompletedEvents") != null) {
                maximumBatchedEvents = Integer.parseInt(System.getProperty("com.ibm.io.async.AIOMaxBatchedCompletedEvents"));
            }

            if (System.getProperty("com.ibm.io.async.AIOHandlerCompletionTimeout") != null) {
                completionTimeout = Integer.parseInt(System.getProperty("com.ibm.io.async.AIOHandlerCompletionTimeout"));
            }

            return null;
        }
    }

    // # Defines the number of overlapped structures that will be cached by the natives
    // # This represents the largest number of _simultaneous_ outstanding operations
    // # any calls beyond this number will have native working memory allocated on demand.
    // com.ibm.io.async.AsyncLibrary.maxIdentifiers = 256
    static int maxIdentifiers = 256;

    // # Sets the name of the class providing the async behaviour to file channels.
    // # Condition: Class name for class with no-arg constructor that implements IAsyncProvider
    // com.ibm.io.async.AsyncFileChannel.providerName=com.ibm.io.async.AsyncLibrary
    // #com.ibm.io.async.AsyncFileChannel.providerName=com.ibm.io.async.FileProvider
    // #com.ibm.io.async.AsyncFileChannel.providerName=com.ibm.io.async.java.JavaProvider

    // # Set the trace level for the subsystem
    // # Condition: 0 = off, 1 = tracing, 2 = verbose tracing
    // com.ibm.io.async.Trace.level = 0
    // static int TraceLevel = 0;

    // ***************************************************************************************

    // Code for using property files to set the above variables

    // ***************************************************************************************

    //	private static final String SETTING_BUNDLE_NAME = "com.ibm.io.async.asyncio"; //$NON-NLS-1$
    //	private static final String MESSAGE_BUNDLE_NAME = "com.ibm.io.async.asyncmsg"; //$NON-NLS-1$
    //
    // private static final ResourceBundle SETTINGS_BUNDLE = getBundle(SETTING_BUNDLE_NAME);
    // private static final ResourceBundle MESSAGE_BUNDLE = getBundle(MESSAGE_BUNDLE_NAME);
    //
    // static private ResourceBundle getBundle(String name) {
    // try {
    // return ResourceBundle.getBundle(name);
    // } catch (MissingResourceException exception) {
    // return null;
    // }
    // }
    //
    // static String getMessage(String key) {
    // return key;
    // /* TO-DO: make NLS work for AIO
    // try {
    // return MESSAGE_BUNDLE.getString(key);
    // } catch (MissingResourceException e) {
    // return '!' + key + '!';
    // }
    // */
    // }
    //
    // static String getString(String key) {
    // return getString(key, key);
    // }
    //
    // static String getString(String key, String defaultValue) {
    // if (SETTINGS_BUNDLE == null) {
    //			Trace.logger.logp(Level.WARNING, "AsyncProperties", "getString", "Missing bundle. Using default value for \"{0}\" which is \"{1}\"", new Object[] {key, defaultValue}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    // return defaultValue;
    // }
    // try {
    // return SETTINGS_BUNDLE.getString(key);
    // } catch (MissingResourceException exception) {
    //			Trace.logger.logp(Level.WARNING, "AsyncProperties", "getString", "Missing resource. Using default value for \"{0}\" which is \"{1}\"", new Object[] {key, defaultValue}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    // return defaultValue;
    // }
    // }
    //
    // static int getInt(String key, int defaultValue) {
    // if (SETTINGS_BUNDLE == null) {
    //			Trace.logger.logp(Level.WARNING, "AsyncProperties", "getInt", "Missing bundle. Using default value for \"{0}\" which is \"{1}\"", new Object[] {key, new Integer(defaultValue)}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    // return defaultValue;
    // }
    // try {
    // String strValue = SETTINGS_BUNDLE.getString(key);
    // return Integer.parseInt(strValue);
    // } catch (MissingResourceException exception) {
    //			Trace.logger.logp(Level.WARNING, "AsyncProperties", "getInt", "Missing resource. Using default value for \"{0}\" which is \"{1}\"", new Object[] {key, new Integer(defaultValue)}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    // return defaultValue;
    // } catch (NumberFormatException exception) {
    //			Trace.logger.logp(Level.WARNING, "AsyncProperties", "getInt", "Invalid number. Using default value for \"{0}\" which is \"{1}\"", new Object[] {key, new Integer(defaultValue)}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    // return defaultValue;
    // }
    // }

}
