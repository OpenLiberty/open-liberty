/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides a means to add key-value pairs to log and trace records.
 * <p>
 * There are two ways to add key-value pairs to the LogRecordContext. The one
 * you should use depends on whether the value is fixed, or variable.
 * 
 * <p>
 * <strong>Fixed value extensions</strong>
 * <p>
 * When you want to add an extension with a fixed value use the addExtension
 * method. As an example, the following code uses addExtension to register an
 * extension.
 * 
 * <pre>
 * <code>
 * Logger myLogger = Logger.getLogger("myLogger");
 * LogRecordContext.addExtension("someName", "someValue");
 * // This message will have the someName=someValue extension added to it
 * logger.info("some message");
 * LogRecordContext.removeExtension("someName");
 * // This message will not have the extension added to it
 * logger.info("some other message");
 * </code>
 * </pre>
 * 
 * <p>
 * <strong>Variable value extensions</strong>
 * <p>
 * When you want to add an extension with a callback method to compute the
 * extension value use the registerExtension method. As an example, the
 * following code uses registerExtension to add an extension that provides the
 * ThreadId of the current thread.
 * <p>
 * 
 * <pre>
 * <code>
 * import com.ibm.websphere.logging.hpel.LogRecordContext;
 * 
 * public class ThreadIdExtension {
 * 
 * // a strong reference to the LogRecordContext.Extension to make
 * // sure it is not garbage collected
 * private final static LogRecordContext.Extension extension = new LogRecordContext.Extension() {
 * public String getValue() {
 * return Long.toString(Thread.currentThread().getId());
 * }
 * };
 * 
 * public static void init() {
 * LogRecordContext.registerExtension("ThreadId", extension);
 * }
 * 
 * public static void destroy() {
 * LogRecordContext.unregisterExtension("ThreadId");
 * }
 * }
 * </code>
 * </pre>
 * 
 * <p>
 * <strong>Using the extensions</strong>
 * <p>
 * Log handlers can use the key-value pairs in log and trace output by calling
 * the getExtensions(Map<String,String>) method. The getExtensions method first
 * calls the getValue method for each LogRecordContext.Extension, adding the
 * resultant key-value pairs to the Map. getExtensions then copies the fixed
 * key-value pairs into the Map. An example log handler could be written as
 * follows:
 * 
 * <pre>
 * <code>
 * import java.io.PrintWriter;
 * import java.util.HashMap;
 * import java.util.Map;
 * import java.util.logging.Handler;
 * import java.util.logging.LogRecord;
 * 
 * import com.ibm.websphere.logging.hpel.LogRecordContext;
 * 
 * public class MyHandler extends Handler {
 * 
 * private PrintWriter printWriter;
 * 
 * MyHandler(PrintWriter printWriter) {
 * this.printWriter = printWriter;
 * }
 * 
 * public void close() {
 * printWriter.close();
 * }
 * 
 * public void flush() {
 * printWriter.flush();
 * }
 * 
 * public void publish(LogRecord record) {
 * Map<String, String> context = new HashMap<String, String>();
 * 
 * // get the extension keys/values
 * LogRecordContext.getExtensions(context);
 * 
 * String s;
 * for (Entry<String, String> entry : context.entrySet()) {
 * s += "[" + entry.getKey() + "=" + entry.getValue() + "]";
 * }
 * s += record.getMessage();
 * 
 * printWriter.println(s);
 * }
 * 
 * }
 * </code>
 * </pre>
 * 
 * Note that the binary logging handlers call
 * <code>LogRecordContext.getExtensions</code> and store the resultant key-value
 * pairs in the log and trace data repositories. This information can then be
 * accessed via the com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord
 * getExtensions method. Extension information can also be used for filtering
 * log and trace records via the binaryLog command line tool's
 * --includeExtensions option.
 * 
 * @ibm-api
 */
public class LogRecordContext {

    /**
     * Call back interface to retrieve current extension value.
     */
    public interface Extension {
        /**
         * Returns current value of this extension.
         * 
         * @return String value of this extension
         */
        String getValue();
    }

    private final static ThreadLocal<HashMap<String, String>> extensions = new ThreadLocal<HashMap<String, String>>() {
        @Override
        protected HashMap<String, String> initialValue() {
            return new HashMap<String, String>();
        }
    };

    /* Map of registered extension */
    private final static Map<String, WeakReference<Extension>> extensionMap = new HashMap<String, WeakReference<Extension>>();
    private final static ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final static Lock r = rwl.readLock();
    private final static Lock w = rwl.writeLock();
    public static final String PTHREADID = "thread";
    private final static ThreadLocal<Boolean> recursion = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    private final static Extension THREAD_NAME_EXTENSION = new Extension() {
        @Override
        public String getValue() {
            return Thread.currentThread().getName();
        }
    };

    static {
        extensionMap.put(PTHREADID, new WeakReference<Extension>(
                        THREAD_NAME_EXTENSION));
    }

    /**
     * Adds an extension key/value to the context.
     * 
     * @param extensionName
     *            String extensionName key name for the new extension
     * @param extensionValue
     *            String extensionValue key value for the new extension
     * @throws IllegalArgumentException
     *             if parameter <code>extensionName</code> or
     *             <code>extensionValue</code> are <code>null</code>
     */
    public static void addExtension(String extensionName, String extensionValue) {
        if (extensionName == null || extensionValue == null) {
            throw new IllegalArgumentException(
                            "Neither 'extensionName' nor 'extensionValue' parameter can be null. Extension Name="
                                            + extensionName
                                            + " Extension Value="
                                            + extensionValue);
        }
        extensions.get().put(extensionName, extensionValue);
    }

    /**
     * Removes an extension key/value from the context
     * 
     * @param extensionName
     *            String extensionName associated with the registered extension.
     * @throws IllegalArgumentException
     *             if parameter <code>extensionName</code> is <code>null</code>.
     */
    public static boolean removeExtension(String extensionName) {
        if (extensionName == null) {
            throw new IllegalArgumentException(
                            "Parameter 'extensionName' can not be null");
        }
        return extensions.get().remove(extensionName) != null;
    }

    /**
     * Registers new context extension. To avoid memory leaks Extensions are
     * stored as weak references. It means that caller need to keep strong
     * reference (a static field for example) to keep that extension in the
     * registration map.
     * 
     * @param key
     *            String key to associate with the registered extension
     * @param extension
     *            {@link Extension} implementation returning extension runtime
     *            values
     * @throws IllegalArgumentException
     *             if parameter <code>key</code> or <code>extension</code> are
     *             <code>null</code>; or if <code>key</code> already has
     *             extension associated with it.
     */
    public static void registerExtension(String key, Extension extension) {
        if (key == null || extension == null) {
            throw new IllegalArgumentException(
                            "Neither 'key' nor 'extension' parameter can be null.");
        }
        w.lock();
        try {
            if (extensionMap.containsKey(key)) {
                throw new IllegalArgumentException("Extension with the key "
                                                   + key + " is registered already");
            }
            extensionMap.put(key, new WeakReference<Extension>(extension));
        } finally {
            w.unlock();
        }
    }

    /**
     * Removes context extension registration.
     * 
     * @param key
     *            String key associated with the registered extension.
     * @return <code>true</code> if key had extension associated with it.
     * @throws IllegalArgumentException
     *             if parameter <code>key</code> is <code>null</code>.
     */
    public static boolean unregisterExtension(String key) {
        if (key == null) {
            throw new IllegalArgumentException(
                            "Parameter 'key' can not be null");
        }
        w.lock();
        try {
            return extensionMap.remove(key) != null;
        } finally {
            w.unlock();
        }
    }

    /**
     * Retrieves values for all registered context extensions.
     * 
     * @param map
     *            {@link Map} instance to populate with key-value pairs of the
     *            context extensions.
     * @throws IllegalArgumentException
     *             if parameter <code>map</code> is <code>null</code>
     */
    public static void getExtensions(Map<String, String> map)
                    throws IllegalArgumentException {
        if (map == null) {
            throw new IllegalArgumentException(
                            "Parameter 'map' can not be null.");
        }
        if (recursion.get()) {
            return;
        }
        recursion.set(Boolean.TRUE);
        LinkedList<String> cleanup = new LinkedList<String>();
        r.lock();
        try {
            for (Map.Entry<String, WeakReference<Extension>> entry : extensionMap
                            .entrySet()) {
                Extension extension = entry.getValue().get();
                if (extension == null) {
                    cleanup.add(entry.getKey());
                } else {
                    String value = extension.getValue();
                    if (value != null) {
                        map.put(entry.getKey(), value);
                    }
                }
            }
        } finally {
            r.unlock();
            recursion.set(Boolean.FALSE);
        }
        if (cleanup.size() > 0) {
            w.lock();
            try {
                for (String key : cleanup) {
                    WeakReference<Extension> extension = extensionMap
                                    .remove(key);
                    if (extension != null && extension.get() != null) {
                        // Special case! Somebody has put new extension for this
                        // key after we released
                        // read lock and before we took write lock. We need to
                        // put it back.
                        extensionMap.put(key, extension);
                    }
                }
            } finally {
                w.unlock();
            }
        }
        if (extensions.get() != null) {
            for (Entry<String, String> entry : extensions.get().entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
        }

    }

}
