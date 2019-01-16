/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.msgstore.test.persistence.objectManager;

/*
 * Change activity:
 *
 * Reason          Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 * 542362          07/08/08 gareth   Handle rollback of addToStore()
 * ============================================================================
 */

import java.io.File;
import java.util.Properties;

import junit.framework.TestSuite;

import com.ibm.ws.objectManager.ObjectManager;
import com.ibm.ws.objectManager.ObjectManagerException;
import com.ibm.ws.objectManager.ObjectStore;
import com.ibm.ws.objectManager.SingleFileObjectStore;
import com.ibm.ws.objectManager.Transaction;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.persistence.TupleTypeEnum;
import com.ibm.ws.sib.msgstore.persistence.objectManager.PersistableImpl;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.test.transactions.NullPersistentItem;
import com.ibm.ws.sib.msgstore.transactions.impl.MSDelegatingLocalTransaction;

/**
 * This test checks that a PersistableImpl is capable of having add/update/remove called
 * on it twice in succession. This is meant to duplicate the behaviour where a problem
 * is encountered during writing to the object manager and the transaction is rolled back.
 * 
 * As the transaction only has a handle to the managed objects and not the persistable it
 * cannot rollback any changes in the persistable object itself so it needs to be able to
 * handle multiple calls to add/update/remove itself.
 */
public class RolledBackPersistableTest extends MessageStoreTestCase {
    public RolledBackPersistableTest(String name) {
        super(name);

        //turnOnTrace("com.ibm.ws.sib.msgstore.persistence.objectManager.Persistable*=all");
    }

    /**
     * This is a Object Manager specific test so no persistence
     * choice is provided.
     */
    public static TestSuite suite() {
        TestSuite suite = new TestSuite(RolledBackPersistableTest.class);

        return suite;
    }

    public void testRolledBackPermanentPersistable() {
        print("|-----------------------------------------------------");
        print("| RolledBackPermanentPersistableTest:");
        print("|------------------------------------");
        print("|");

        ObjectManager objectManager = null;
        ObjectStore permStore = null;
        ObjectStore tempStore = null;
        Transaction tran = null;

        MSDelegatingLocalTransaction local = null;
        ItemStream root = null;
        MessageStoreImpl messageStore = null;
        AbstractItemLink link = null;

        try {
            // Find the paths to use for our OM files
            Properties props = System.getProperties();

            String logFile = "RBLog";
            String permFile = "RBPermStore";

            // As a persistable needs a working AbstractItemLink to be
            // successfully tested we need to start up a message store 
            // and steal a link from an item. The fact that the link 
            // isn't "the right one" for the persistable we test doesn't
            // matter as long as it returns us some persistent data when
            // we ask for it.
            try {
                messageStore = (MessageStoreImpl) createAndStartMessageStore(true, PERSISTENCE);

                local = (MSDelegatingLocalTransaction) messageStore.getTransactionFactory().createLocalTransaction();

                root = createPersistentRootItemStream(messageStore);

                NullPersistentItem message = new NullPersistentItem("RolledBackPermanentPersistableTest");

                root.addItem(message, local);

                local.commit();

                link = (AbstractItemLink) messageStore._getMembership(message);

                print("| Got link for Persistable");
            } catch (Exception e) {
                print("| Get link for Persistable   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown during MessageStore.stop(): " + e.getMessage());
            }

            String logDirectory = messageStore.getProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX + MessageStoreConstants.PROP_OBJECT_MANAGER_LOG_FILE_PREFIX,
                                                           MessageStoreConstants.PROP_OBJECT_MANAGER_LOG_FILE_PREFIX_DEFAULT);
            String permDirectory = messageStore.getProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX + MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_PREFIX,
                                                            MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_PREFIX_DEFAULT);

            // Create a persistable to use for the tests
            PersistableImpl persistable = new PersistableImpl(1L, 2L, TupleTypeEnum.ITEM);
            print("| Persistable created");

            persistable.setAbstractItemLink(link);
            print("| Set link in Persistable");

            // Start the ObjectManager and create the ObjectStores
            try {
                objectManager = new ObjectManager(logDirectory + File.separator + logFile, ObjectManager.LOG_FILE_TYPE_CLEAR);

                if (objectManager.warmStarted()) {
                    permStore = objectManager.getObjectStore(permDirectory + File.separator + permFile);
                } else {
                    permStore = new SingleFileObjectStore(permDirectory + File.separator + permFile, objectManager, ObjectStore.STRATEGY_KEEP_ALWAYS);
                }

                print("| ObjectManager started");
            } catch (Exception e) {
                print("| ObjectManager start   !!!FAILED!!!");
                e.printStackTrace();
                fail("ObjectManager failed to start!");
            }

            try {
                tran = objectManager.getTransaction();

                persistable.addToStore(tran, permStore);

                print("| Added persistable to permanent store");
            } catch (Exception e) {
                print("| Add persistable to permanent store   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught adding persistable to permanent store");
            }

            try {
                tran.backout(false);

                print("| Rolled back add of persistable to permanent store");
            } catch (Exception e) {
                print("| Rollback add of persistable to permanent store   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught rolling back add of persistable to permanent store");
            }

            try {
                tran = objectManager.getTransaction();

                persistable.addToStore(tran, permStore);

                print("| Added persistable to permanent store - 2nd Attempt");
            } catch (Exception e) {
                print("| Add persistable to permanent store - 2nd Attempt   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught adding persistable to permanent store - 2nd Attempt!");
            }

            try {
                tran.commit(false);

                print("| Committed add of persistable to permanent store");
            } catch (Exception e) {
                print("| Commit of add of persistable to permanent store   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught rolling back add of persistable to permanent store!");
            }

            try {
                tran = objectManager.getTransaction();

                persistable.updateDataOnly(tran, permStore);

                print("| Updated persistable");
            } catch (Exception e) {
                print("| Update persistable   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught updating persistable!");
            }

            try {
                tran.backout(false);

                print("| Rolled back update of persistable");
            } catch (Exception e) {
                print("| Rollback of update of persistable   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught rolling back update of persistable!");
            }

            try {
                tran = objectManager.getTransaction();

                persistable.updateDataOnly(tran, permStore);

                print("| Updated persistable - 2nd Attempt");
            } catch (Exception e) {
                print("| Update persistable - 2nd Attempt   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught updating persistable - 2nd Attempt!");
            }

            try {
                tran.commit(false);

                print("| Committed update of persistable");
            } catch (Exception e) {
                print("| Commit of update of persistable   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught committing update of persistable!");
            }

            try {
                tran = objectManager.getTransaction();

                persistable.setLockID(123L);

                persistable.updateMetaDataOnly(tran);

                print("| Updated persistable lock ID");
            } catch (Exception e) {
                print("| Update persistable lock ID   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught updating persistable lock ID!");
            }

            try {
                tran.backout(false);

                print("| Rolled back update of persistable lock ID");
            } catch (Exception e) {
                print("| Rollback of update of persistable lock ID   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught rolling back update of persistable lock ID!");
            }

            try {
                tran = objectManager.getTransaction();

                persistable.setLockID(456L);

                persistable.updateMetaDataOnly(tran);

                print("| Updated persistable lock ID - 2nd Attempt");
            } catch (Exception e) {
                print("| Update persistable lock ID - 2nd Attempt   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught updating persistable lock ID - 2nd Attempt!");
            }

            try {
                tran.commit(false);

                print("| Committed update of persistable lock ID");
            } catch (Exception e) {
                print("| Commit of update of persistable lock ID   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught committing update of persistable lock ID!");
            }

            try {
                tran = objectManager.getTransaction();

                persistable.removeFromStore(tran);

                print("| Removed persistable from permanent store");
            } catch (Exception e) {
                print("| Remove persistable from permanent store   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught removing persistable from permanent store!");
            }

            try {
                tran.backout(false);

                print("| Rolled back remove of persistable from permanent store");
            } catch (Exception e) {
                print("| Rollback of remove of persistable from permanent store   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught rolling back remove of persistable from permanent store!");
            }

            try {
                tran = objectManager.getTransaction();

                persistable.removeFromStore(tran);

                print("| Removed persistable from permanent store - 2nd Attempt");
            } catch (Exception e) {
                print("| Remove persistable from permanent store - 2nd Attempt   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught removing persistable from permanent store - 2nd Attempt!");
            }

            try {
                tran.commit(false);

                print("| Committed remove of persistable from permanent store");
            } catch (Exception e) {
                print("| Commit of remove of persistable from permanent store   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught committing remove of persistable from permanent store!");
            }

            // Stop the object manager and and clean up the files if 
            // we have completed the test successfully.
            try {
                if (objectManager != null) {
                    objectManager.shutdown();
                    objectManager = null;

                    print("| ObjectManager shutdown");
                }

                File file = new File(logDirectory + File.separator + logFile);
                if (file.exists()) {
                    file.delete();
                }

                file = new File(permDirectory + File.separator + permFile);
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                print("| ObjectManager shutdown   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught shutting down ObjectManager!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception caught!");
        } finally {
            if (messageStore != null) {
                messageStore.stop(0);
            }

            if (objectManager != null) {
                try {
                    objectManager.shutdown();

                    print("| ObjectManager shutdown");
                } catch (ObjectManagerException ome) {
                    print("| ObjectManager shutdown   !!!FAILED!!!");
                    ome.printStackTrace();
                }
            }

            print("|");
            print("|------------------------ END ------------------------");
        }
    }

    public void testRolledBackTemporaryPersistable() {
        print("|-----------------------------------------------------");
        print("| RolledBackTemporaryPersistableTest:");
        print("|------------------------------------");
        print("|");

        ObjectManager objectManager = null;
        ObjectStore permStore = null;
        ObjectStore tempStore = null;
        Transaction tran = null;

        MSDelegatingLocalTransaction local = null;
        ItemStream root = null;
        MessageStoreImpl messageStore = null;
        AbstractItemLink link = null;

        try {
            // Find the paths to use for our OM files
            Properties props = System.getProperties();

            String logFile = "RBLog";
            String tempFile = "RBTempStore";

            // As a persistable needs a working AbstractItemLink to be
            // successfully tested we need to start up a message store 
            // and steal a link from an item. The fact that the link 
            // isn't "the right one" for the persistable we test doesn't
            // matter as long as it returns us some persistent data when
            // we ask for it.
            try {
                messageStore = (MessageStoreImpl) createAndStartMessageStore(true, PERSISTENCE);

                local = (MSDelegatingLocalTransaction) messageStore.getTransactionFactory().createLocalTransaction();

                root = createPersistentRootItemStream(messageStore);

                NullPersistentItem message = new NullPersistentItem("RolledBackTemporaryPersistableTest");

                root.addItem(message, local);

                local.commit();

                link = (AbstractItemLink) messageStore._getMembership(message);

                print("| Got link for Persistable");
            } catch (Exception e) {
                print("| Get link for Persistable   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown during MessageStore.stop(): " + e.getMessage());
            }

            String logDirectory = messageStore.getProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX + MessageStoreConstants.PROP_OBJECT_MANAGER_LOG_FILE_PREFIX,
                                                           MessageStoreConstants.PROP_OBJECT_MANAGER_LOG_FILE_PREFIX_DEFAULT);
            String tempDirectory = messageStore.getProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX + MessageStoreConstants.PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_PREFIX,
                                                            MessageStoreConstants.PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_PREFIX_DEFAULT);

            // Create a persistable to use for the tests
            PersistableImpl persistable = new PersistableImpl(1L, 2L, TupleTypeEnum.ITEM);
            print("| Persistable created");

            persistable.setAbstractItemLink(link);
            print("| Set link in Persistable");

            // Start the ObjectManager and create the ObjectStores
            try {
                objectManager = new ObjectManager(logDirectory + File.separator + logFile, ObjectManager.LOG_FILE_TYPE_CLEAR);

                if (objectManager.warmStarted()) {
                    tempStore = objectManager.getObjectStore(tempDirectory + File.separator + tempFile);
                } else {
                    tempStore = new SingleFileObjectStore(tempDirectory + File.separator + tempFile, objectManager, ObjectStore.STRATEGY_KEEP_UNTIL_NEXT_OPEN);
                }

                print("| ObjectManager started");
            } catch (Exception e) {
                print("| ObjectManager start   !!!FAILED!!!");
                e.printStackTrace();
                fail("ObjectManager failed to start!");
            }

            try {
                tran = objectManager.getTransaction();

                persistable.addToStore(tran, tempStore);

                print("| Added persistable to temporary store");
            } catch (Exception e) {
                print("| Add persistable to temporary store   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught adding persistable to temporary store");
            }

            try {
                tran.backout(false);

                print("| Rolled back add of persistable to temporary store");
            } catch (Exception e) {
                print("| Rollback add of persistable to temporary store   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught rolling back add of persistable to temporary store");
            }

            try {
                tran = objectManager.getTransaction();

                persistable.addToStore(tran, tempStore);

                print("| Added persistable to temporary store - 2nd Attempt");
            } catch (Exception e) {
                print("| Add persistable to temporary store - 2nd Attempt   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught adding persistable to temporary store - 2nd Attempt!");
            }

            try {
                tran.commit(false);

                print("| Committed add of persistable to temporary store");
            } catch (Exception e) {
                print("| Commit of add of persistable to temporary store   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught rolling back add of persistable to temporary store!");
            }

            try {
                tran = objectManager.getTransaction();

                persistable.updateDataOnly(tran, tempStore);

                print("| Updated persistable");
            } catch (Exception e) {
                print("| Update persistable   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught updating persistable!");
            }

            try {
                tran.backout(false);

                print("| Rolled back update of persistable");
            } catch (Exception e) {
                print("| Rollback of update of persistable   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught rolling back update of persistable!");
            }

            try {
                tran = objectManager.getTransaction();

                persistable.updateDataOnly(tran, tempStore);

                print("| Updated persistable - 2nd Attempt");
            } catch (Exception e) {
                print("| Update persistable - 2nd Attempt   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught updating persistable - 2nd Attempt!");
            }

            try {
                tran.commit(false);

                print("| Committed update of persistable");
            } catch (Exception e) {
                print("| Commit of update of persistable   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught committing update of persistable!");
            }

            try {
                tran = objectManager.getTransaction();

                persistable.setLockID(123L);

                persistable.updateMetaDataOnly(tran);

                print("| Updated persistable lock ID");
            } catch (Exception e) {
                print("| Update persistable lock ID   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught updating persistable lock ID!");
            }

            try {
                tran.backout(false);

                print("| Rolled back update of persistable lock ID");
            } catch (Exception e) {
                print("| Rollback of update of persistable lock ID   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught rolling back update of persistable lock ID!");
            }

            try {
                tran = objectManager.getTransaction();

                persistable.setLockID(456L);

                persistable.updateMetaDataOnly(tran);

                print("| Updated persistable lock ID - 2nd Attempt");
            } catch (Exception e) {
                print("| Update persistable lock ID - 2nd Attempt   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught updating persistable lock ID - 2nd Attempt!");
            }

            try {
                tran.commit(false);

                print("| Committed update of persistable lock ID");
            } catch (Exception e) {
                print("| Commit of update of persistable lock ID   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught committing update of persistable lock ID!");
            }

            try {
                tran = objectManager.getTransaction();

                persistable.removeFromStore(tran);

                print("| Removed persistable from temporary store");
            } catch (Exception e) {
                print("| Remove persistable from temporary store   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught removing persistable from temporary store!");
            }

            try {
                tran.backout(false);

                print("| Rolled back remove of persistable from temporary store");
            } catch (Exception e) {
                print("| Rollback of remove of persistable from temporary store   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught rolling back remove of persistable from temporary store!");
            }

            try {
                tran = objectManager.getTransaction();

                persistable.removeFromStore(tran);

                print("| Removed persistable from temporary store - 2nd Attempt");
            } catch (Exception e) {
                print("| Remove persistable from temporary store - 2nd Attempt   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught removing persistable from temporary store - 2nd Attempt!");
            }

            try {
                tran.commit(false);

                print("| Committed remove of persistable from temporary store");
            } catch (Exception e) {
                print("| Commit of remove of persistable from temporary store   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught committing remove of persistable from temporary store!");
            }

            // Stop the object manager and and clean up the files if 
            // we have completed the test successfully.
            try {
                if (objectManager != null) {
                    objectManager.shutdown();
                    objectManager = null;

                    print("| ObjectManager shutdown");
                }

                File file = new File(logDirectory + File.separator + logFile);
                if (file.exists()) {
                    file.delete();
                }

                file = new File(tempDirectory + File.separator + tempFile);
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                print("| ObjectManager shutdown   !!!FAILED!!!");
                e.printStackTrace();
                fail("Exception caught shutting down ObjectManager!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception caught!");
        } finally {
            if (messageStore != null) {
                messageStore.stop(0);
            }

            if (objectManager != null) {
                try {
                    objectManager.shutdown();

                    print("| ObjectManager shutdown");
                } catch (ObjectManagerException ome) {
                    print("| ObjectManager shutdown   !!!FAILED!!!");
                    ome.printStackTrace();
                }
            }

            print("|");
            print("|------------------------ END ------------------------");
        }
    }
}
