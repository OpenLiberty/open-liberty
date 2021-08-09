/*==============================================================================
 * Copyright (c) 2012,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *==============================================================================
 */
package com.ibm.ws.sib.msgstore.test;

import java.util.List;

import com.ibm.js.test.LoggingTestCase;
import com.ibm.ws.sib.admin.JsHealthState;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Configuration;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemReference;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.ReferenceStream;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;

import static java.lang.Math.max;

//import com.ibm.websphere.ws.sib.unittest.ras.Trace;
public abstract class MessageStoreTestCase extends LoggingTestCase {
    //public final static String DATABASE_PERSISTENCE = MessageStoreConstants.PERSISTENT_MESSAGE_STORE_CLASS_DATABASE;
    public final static String OBJECTMANAGER_PERSISTENCE = MessageStoreConstants.PERSISTENT_MESSAGE_STORE_CLASS_OBJECTMANAGER;
    public final static boolean NEW_LOCKING = true;
    public final static boolean OLD_LOCKING = false;

    // Changing the spilling & dispatching decision making from using the 'persistent size'
    // to the 'in memory size' of the Item means that every test Item needs to provide an 
    // inMemorySize which is suitably plausible so as not to change the tet results.
    // We've used a first pass estimate of the inMemory:persistent size for a 'standard' message
    // being 4:1, but we may refine it. So that we don't have to change umpteen unit test 
    // Items everytime, we'll make them use a multiple of their old persistentDataSize and define
    // the multiplier here as a single constant.
    public final static int ITEM_SIZE_MULTIPLIER = com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink.MEMORY_SIZE_MULTIPLIER;

    protected String PERSISTENCE;

    public MessageStoreTestCase(String arg0) {
        super(arg0);
    }

    /**
     * Assert that the two abstract items are equivalent (ie appear to
     * be the same under all tests EXCEPT identity).
     * 
     * @param item1
     * @param item2
     */
    public final void assertEquivalent(AbstractItem item1, AbstractItem item2) {
        assertEquals("Silent expiry should be the same", item1.canExpireSilently(), item2.canExpireSilently());
        assertEquals("defferred persistence should be the same", item1.deferDataPersistence(), item2.deferDataPersistence());
        assertEquals("class should be the same", item1.getClass(), item2.getClass());
        assertEquals("maximum time in store should be the same", item1.getMaximumTimeInStore(), item2.getMaximumTimeInStore());
        assertEquals("priority should be the same", item1.getPriority(), item2.getPriority());
        assertEquals("storage strategy should be the same", item1.getStorageStrategy(), item2.getStorageStrategy());
    }

    protected final MessageStore createMessageStore(boolean clean, String persistence) {
        MessageStore messageStore = null;
        try {
            messageStore = MessageStore.createInstance();
            Configuration configuration = Configuration.createBasicConfiguration();
            configuration.setObjectManagerLogDirectory("build");
            configuration.setObjectManagerPermanentStoreDirectory("build");
            configuration.setObjectManagerTemporaryStoreDirectory("build");

            configuration.setCleanPersistenceOnStart(clean);
            messageStore.initialize(configuration);

            if (persistence != null) {
                ((MessageStoreImpl) messageStore).setCustomProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX
                                                                    + MessageStoreConstants.PROP_PERSISTENT_MESSAGE_STORE_CLASS
                                                                    , persistence);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail("failed to initialize message store: " + e);
        }
        return messageStore;
    }

    protected final MessageStore createAndStartPreviouslyUnhealthyMessageStore(boolean clean) {
        MessageStore messageStore = null;

        try {
            messageStore = MessageStore.createInstance();
            // make the message store unhealthy
            messageStore.reportLocalError();
            messageStore = createAndStartMessageStoreImpl(clean, messageStore, null);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail("failed to create message store: " + e);
        }

        return messageStore;
    }

    protected final MessageStore createAndStartMessageStore(boolean clean, String persistence) {
        MessageStore messageStore = null;

        try {
            messageStore = MessageStore.createInstance();
            messageStore = createAndStartMessageStoreImpl(clean, messageStore, persistence);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail("failed to create message store: " + e);
        }

        return messageStore;
    }

    private final MessageStore createAndStartMessageStoreImpl(boolean clean, MessageStore messageStore, String persistence) {
        try {
            Configuration configuration = Configuration.createBasicConfiguration();

            configuration.setCleanPersistenceOnStart(clean);
            configuration.setObjectManagerLogDirectory("build");
            configuration.setObjectManagerPermanentStoreDirectory("build");
            configuration.setObjectManagerTemporaryStoreDirectory("build");
            messageStore.initialize(configuration);

            if (persistence != null) {
                ((MessageStoreImpl) messageStore).setCustomProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX
                                                                    + MessageStoreConstants.PROP_PERSISTENT_MESSAGE_STORE_CLASS
                                                                    , persistence);
            }

            messageStore.start();

            JsHealthState state = messageStore.getHealthState();
            if (!state.isOK()) {
                System.err.println("Startup Exceptions:");
                List<Exception> exceptions = messageStore.getStartupExceptions();

                for (Exception exception : exceptions) {
                    exception.printStackTrace(System.err);
                }

                fail("Failed to start message store. Health State: " + state);
            }

            messageStore.expirerStart();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail("failed to initialize message store: " + e);
        }
        return messageStore;
    }

    protected final void stopMessageStore(MessageStore messageStore) {
        try {
            messageStore.stop(0);
            messageStore.destroy();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail("failed to close message store: " + e);
        }
    }

    protected final ItemStream createNonPersistentRootItemStream(MessageStore messageStore) {
        ItemStream itemStream = null;
        try {
            itemStream = new ItemStream();
            ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
            messageStore.add(itemStream, localtran);
            localtran.commit();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail("failed to create root item stream: " + e);
        }
        return itemStream;
    }

    protected final ItemStream createPersistentRootItemStream(MessageStore messageStore) {
        ItemStream itemStream = null;
        try {
            itemStream = new PersistentItemStream();
            ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();
            messageStore.add(itemStream, localtran);
            localtran.commit();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail("failed to create root item stream: " + e);
        }
        return itemStream;
    }

    public static void print(String message) {
        LoggingTestCase.print(message);
    }

    private static final int LINE_WIDTH = 55;

    public static void printStarHead(String title) {
        final int midPoint = title.length() / 2;
        final String left = title.substring(0, midPoint);
        final String right = title.substring(midPoint);
        final int leftWidth = LINE_WIDTH / 2;
        final int rightWidth = leftWidth + LINE_WIDTH % 2;
        final String line = String.format("%" + leftWidth + "s%-" + rightWidth + "s", left, right);
        // replace any spaces not next to a word boundary with asterisks
        print(line.replaceAll("(?<!\\b) (?!\\b)", "*"));
    }

    public static void printStarLine(Object...objects) {
        printStarLine0(" *", objects);
    }

    public static void printSuccess(Object... objects) {
        printStarLine0(" - SUCCESS *", objects);
    }

    public static void printFailed(Object... objects) {
        printStarLine0(" - FAILED  *", objects);
    }

    private static void printStarLine0(String right, Object... objects) {
        final StringBuilder sb = new StringBuilder();
        for (Object o: objects) sb.append(o);
        final String s = sb.toString();
        final int contentWidth = LINE_WIDTH - right.length() - 2;
        print(String.format("* %-" + contentWidth + "s%s" , s.substring(max(0, s.length() - contentWidth)), right));
    }

/*
 * protected final void turnOnTrace()
 * {
 * turnOnTrace(false, "com.ibm.ws.sib.*=all=enabled");
 * }
 * 
 * protected final void turnOnTrace(String traceSpec)
 * {
 * turnOnTrace(false, traceSpec);
 * }
 * 
 * protected final void turnOnTrace(boolean console, String traceSpec)
 * {
 * String destination = ManagerAdmin.file;
 * if (console)
 * {
 * destination = ManagerAdmin.stdout;
 * }
 * ManagerAdmin.configureClientTrace(traceSpec,
 * destination,
 * "trace",
 * true,
 * ManagerAdmin.BASIC,
 * false);
 * }
 * 
 * protected final void turnOffTrace()
 * {
 * ManagerAdmin.setTraceState("*=info");
 * }
 * 
 * protected final void configureTrace(String traceSpec)
 * {
 * // eg "com.ibm.ws.sib.*=all=enabled"
 * ManagerAdmin.setTraceState(traceSpec);
 * }
 */
    protected final void setPersistence(String persistence) {
        PERSISTENCE = persistence;
    }

    public static class PersistentItem extends Item {
        public PersistentItem() {
            super();
        }

        @Override
        public int getStorageStrategy() {
            return STORE_ALWAYS;
        }
    }

    public static class PersistentItemStream extends ItemStream {
        public PersistentItemStream() throws MessageStoreException {
            super();
        }

        @Override
        public int getStorageStrategy() {
            return STORE_ALWAYS;
        }
    }

    public static class PersistentRefStream extends ReferenceStream {
        public PersistentRefStream() throws MessageStoreException {
            super();
        }

        @Override
        public int getStorageStrategy() {
            return STORE_ALWAYS;
        }
    }

    public static class PersistentRef extends ItemReference {
        public PersistentRef() {
            super();
        }

        public PersistentRef(Item i) {
            super(i);
        }

        @Override
        public int getStorageStrategy() {
            return STORE_ALWAYS;
        }
    }
}
