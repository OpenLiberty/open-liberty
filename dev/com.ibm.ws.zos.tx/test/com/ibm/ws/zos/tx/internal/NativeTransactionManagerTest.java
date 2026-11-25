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
package com.ibm.ws.zos.tx.internal;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.transaction.HeuristicMixedException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.States;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.tx.jta.OnePhaseXAResource;
import com.ibm.tx.jta.XAResourceFactory;
import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.tx.jta.embeddable.impl.EmbeddableUserTransactionImpl;
import com.ibm.tx.jta.impl.XidImpl;
import com.ibm.tx.util.ByteArray;
import com.ibm.tx.util.TMHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.LocalTransaction.InconsistentLocalTranException;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.LocalTransaction.RolledbackException;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereUserTransaction;
import com.ibm.ws.tx.rrs.RRSXAResourceFactory;
import com.ibm.ws.zos.tx.internal.rrs.BeginTransactionReturnType;
import com.ibm.ws.zos.tx.internal.rrs.ExpressInterestReturnType;
import com.ibm.ws.zos.tx.internal.rrs.PrepareAgentURReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RRSServices;
import com.ibm.ws.zos.tx.internal.rrs.RegisterResMgrReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RegistryException;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveLogNameReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveRMMetadataReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveSideInformationFastReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveSideInformationReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveURDataReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveURInterestReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveWorkIdentifierReturnType;
import com.ibm.ws.zos.tx.internal.rrs.SetExitInformationReturnType;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.tx.UOWEventListener;

import test.common.SharedOutputManager;

/**
 * Unit tests for the native transaction manager. These test the Java portion of
 * RRS resource manager integration, and do not exercise the native code at all.
 */
public class NativeTransactionManagerTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=info:zTransaction=all:Transaction=all");
    @Rule
    public TestRule managerRule = outputMgr;

    private static final Class<?> c = NativeTransactionManagerTest.class;

    /** The resource manager name to use when registering with the fake RRS */
    private static final String rmName = "BBG.DEFAULT.UNITTESTRMNAME.IBM  ";

    /** The resource manager name in EBCDIC byte array form */
    private static final byte[] rmNameBytes = makePaddedEBCDICBytes(rmName, 32);

    /** The resource manager name registry token */
    private static final byte[] rmNameRegistryToken = new byte[] {
                                                                   (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                                                                   (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08,
                                                                   (byte) 0x09, (byte) 0x10, (byte) 0x11, (byte) 0x12,
                                                                   (byte) 0x13, (byte) 0x14, (byte) 0x15, (byte) 0x16,
                                                                   (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                                                                   (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08,
                                                                   (byte) 0x09, (byte) 0x10, (byte) 0x11, (byte) 0x12,
                                                                   (byte) 0x13, (byte) 0x14, (byte) 0x15, (byte) 0x16,
                                                                   (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                                                                   (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08,
                                                                   (byte) 0x09, (byte) 0x10, (byte) 0x11, (byte) 0x12,
                                                                   (byte) 0x13, (byte) 0x14, (byte) 0x15, (byte) 0x16,
                                                                   (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                                                                   (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08,
                                                                   (byte) 0x09, (byte) 0x10, (byte) 0x11, (byte) 0x12,
                                                                   (byte) 0x13, (byte) 0x14, (byte) 0x15, (byte) 0x16 };

    /** The resource manager token to return during register resource manager */
    private static final byte[] rmToken = new byte[] {
                                                       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                       (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                                                       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                       (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                                                       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                       (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                                                       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                       (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                       (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 };

    /** The resource manager registry token to return during register resource manager */
    private static final byte[] rmRegistryToken = new byte[] {
                                                               (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                               (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                               (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                               (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                                                               (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                               (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                               (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                               (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                                                               (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                               (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                               (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                               (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                                                               (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                               (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                               (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                               (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, };

    /** A random number generator which we'll use to create IDs. */
    private static final Random generator = new Random();

    /** Constant to use for register XAResource to be driven with low priority (driven last). */
    private static final int XARES_LOW_PRIORITY = -10;

    /** Constant to use for register XAResource to be driven with high priority (driven first). */
    @SuppressWarnings("unused")
    private static final int XARES_HIGH_PRIORITY = 10;

    /** The XAResourceFactory for the RRS XAResource. */
    private static final String XARES_FACTORY_RRS = new String("com.ibm.ws.zos.tx.internal.NativeTransactionManager");

    /** The XAResourceFactory for the mock XAResource. */
    private static final String XARES_FACTORY_MOCK = new String("com.ibm.ws.zos.tx.internal.NoOpXAResourceFactory");

    /** Recursively deletes a directory. */
    private static void recursiveDelete(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if ((files != null) && (files.length > 0)) {
                for (int x = 0; x < files.length; x++) {
                    recursiveDelete(files[x]);
                }
            }
        }

        f.delete();
    }

    /** Pre test setup. */
    @BeforeClass
    public static void preTestSetup() throws Exception {
        // Delete old transaction logs.
        File logsDir = new File("logs");
        if ((logsDir != null) && (logsDir.exists()) && (logsDir.isDirectory())) {
            recursiveDelete(logsDir);
        }
        System.setProperty("com.ibm.tx.config.ConfigurationProviderClassName", "com.ibm.tx.jta.config.DefaultConfigurationProvider");
        // Make sure we can get to the global and local transaction managers before running any tests.
        // The 2 objects created as part of the calls below are the objects that are used for the entire run.
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        assertTrue("A EmbeddableWebSphereTransactionManager instance should have been returned.", tm != null);
        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        assertTrue("A LocalTransactionCurrent instance should be have been returned.", ltcTm != null);
        EmbeddableWebSphereUserTransaction userTx = EmbeddableUserTransactionImpl.instance();
        assertTrue("A EmbeddableUserTransactionImpl instance should be have been returned.", userTx != null);

        // Start the transaction manager (recovery/etc). This is done after getting the embeddable TM instances to prevent
        // package conflicts.
        TMHelper.start(true);
        TMHelper.checkTMState();
    }

    /**
     * Tests resource manager deregistration when an exception happens at any time after successful
     * RM registration during activation.
     *
     * @throws Exception
     */
    @Test
    public void testActivationWithException() throws Exception {
        Log.info(c, "testActivationWithException", "Entry.");

        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final RRSServices rrsServices = mockObjects.getRRSServices();
        final ContextManager contextManager = mockObjects.getContextManager();
        final BundleContext bundleContext = mockObjects.getBundleContext();
        final WsLocationAdmin locationAdmin = mockObjects.getLocationAdmin();
        final WsResource rmNameLog = mockObjects.getLognameResource();
        final String rmNameLogFilename = "rrs/tx/rmData.log";
        NativeTransactionManager tm = new NativeTransactionManager();
        tm.setContextManager(contextManager);
        tm.setRRSServices(rrsServices);
        tm.setLocationAdmin(mockObjects.getLocationAdmin());
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(NativeTransactionManager.SHUTDOWN_TIMEOUT_PROPERTY, new Long(15000L));
        props.put(NativeTransactionManager.RMNAME_PREFIX_PROPERTY, "DEAFULT");

        final Sequence activationSequence = mockery.sequence("activationWithExcSequence");
        mockery.checking(new Expectations() {
            {
                // Do the bundle registration thing.
                oneOf(bundleContext).registerService(with(any(String[].class)), with(any(Object.class)), with(any(Dictionary.class)));
                inSequence(activationSequence);
                will(returnValue(null));

                // The server is going to want to read the RM name from the RM log.
                oneOf(locationAdmin).getServerWorkareaResource(with(rmNameLogFilename));
                inSequence(activationSequence);
                will(returnValue(rmNameLog));
                oneOf(rmNameLog).exists();
                inSequence(activationSequence);
                will(returnValue(true));

                try {
                    oneOf(rmNameLog).get();
                    inSequence(activationSequence);
                    will(returnValue(new ByteArrayInputStream(rmName.getBytes())));
                } catch (IOException ioe) {
                    // Nothing
                }

                // Register with registration services.
                oneOf(rrsServices).registerResourceManager(with(RRSServices.CRG_UNREG_EOM), with(new byte[16]), with(any(byte[].class)), with(any(byte[].class)));
                inSequence(activationSequence);
                will(returnValue(new RegisterResMgrReturnType(0, rmNameBytes, rmNameRegistryToken, rmToken, rmRegistryToken, 0, 0, 0, 0)));
                // Initialize the context manager
                oneOf(contextManager).initialize(with(rmRegistryToken));
                inSequence(activationSequence);
                // The call to setExitInformation will throw a runtimeException.
                oneOf(rrsServices).setExitInformation(with(rmNameRegistryToken), with(rmRegistryToken), with(any(Boolean.TYPE)));
                inSequence(activationSequence);
                will(throwException(new RuntimeException()));
                // Resource manager deregistration should follow.
                oneOf(rrsServices).unregisterResourceManager(with(rmNameRegistryToken), with(rmRegistryToken));
                inSequence(activationSequence);
                will(returnValue(RRSServices.CRG_OK));
            }
        });

        // Activation should throw a runtimeException.
        try {
            tm.activate(bundleContext, props);
            fail("Activation should have thrown a RuntimeException.");
        } catch (RuntimeException re) {
            // Expected.
        }

        Log.info(c, "testActivationWithException", "Exit.");
    }

    /**
     * Tests resource manager deregistration an when exception happens at any time during deactivation.
     *
     * @throws Exception
     */
    @Test
    public void testDeactivationWithException() throws Exception {
        Log.info(c, "testDeactivationWithException", "Entry.");

        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final RRSServices rrsServices = mockObjects.getRRSServices();
        final ContextManager contextManager = mockObjects.getContextManager();

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        NativeTransactionManager tm = createNativeTranMgr(mockObjects);

        final Sequence deactivationSequence = mockery.sequence("deactivationWithExcSequence");
        mockery.checking(new Expectations() {
            {
                // Deactivate the transaction manager.
                oneOf(contextManager).destroyContextManager(with(15000L));
                inSequence(deactivationSequence);
                will(throwException(new RuntimeException()));
                oneOf(rrsServices).unregisterResourceManager(with(rmNameRegistryToken), with(rmRegistryToken));
                inSequence(deactivationSequence);
                will(returnValue(RRSServices.CRG_OK));

            }
        });

        // Deactivation should throw a runtimeException.
        try {
            tm.deactivate();
            fail("Activation should have thrown a RuntimeException.");
        } catch (RuntimeException re) {
            // Expected.
        }

        Log.info(c, "testDeactivationWithException", "Exit.");
    }

    /** Test that we can create an XAResource. */
    @Test
    public void testCreateXAResource() throws Exception {
        Log.info(c, "testCreateXAResource", "Entry.");

        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        RRSXAResourceFactory xaResFactory = createNativeTranMgr(mockObjects);
        Serializable info = xaResFactory.getXAResourceInfo((Xid) null);
        assertTrue(info != null);
        XAResource xares = xaResFactory.getXAResource(info);
        assertTrue(xares != null);

        Log.info(c, "testCreateXAResource", "Exit.");
    }

    /** Test that the XAResource can be registered with the transaction manager. */
    @Test
    public void testRegisterResourceInfo() throws Exception {
        Log.info(c, "testRegisterResourceInfo", "Entry.");

        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        RRSXAResourceFactory xaResFactory = createNativeTranMgr(mockObjects);
        Serializable info = xaResFactory.getXAResourceInfo((Xid) null);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        assertTrue(recoveryId > 0);

        Log.info(c, "testRegisterResourceInfo", "Exit.");
    }

    //========================================================================
    // UOWEvent callback verification tests for global transactions.
    //========================================================================

    /**
     * Test that the core transaction manager invokes the native transaction
     * manager for UOWEvent POST_BEGIN and POST_END.
     * Transaction is begun and committed using the UserTransaction interface.
     */
    @Test
    public void testUserTxUOWEventPostBeginAndPostEndOnCommit() throws Exception {
        Log.info(c, "testUserTxUOWEventPostBeginAndPostEndOnCommit", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        mockery.assertIsSatisfied();

        EmbeddableWebSphereUserTransaction ut = EmbeddableUserTransactionImpl.instance();
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        boolean pendingTxOutcome = true;
        try {
            // Begin Tx.
            addUOWEventPostBeginExpectations(mockery, mockObjects);
            ut.begin();
            mockery.assertIsSatisfied();

            // Commit Tx.
            addGlobalTxCompletionExpectations(mockery, mockObjects);
            addUOWEventPostEndExpectations(mockery, mockObjects);
            ut.commit();
            pendingTxOutcome = false;
            mockery.assertIsSatisfied();
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);

            if (pendingTxOutcome) {
                ut.commit();
            }
        }

        Log.info(c, "testUserTxUOWEventPostBeginAndPostEndOnCommit", "Exit.");
    }

    /**
     * Test that the core transaction manager invokes the native transaction
     * manager for UOWEvent POST_BEGIN SUSPEND, RESUME and POST_END.
     * Transaction is begun and rolled back using the UserTransaction interface.
     */
    @Test
    public void testUserTxUOWEventPostBeginSuspendResumePostEndOnRollback() throws Exception {
        Log.info(c, "testUserTxUOWEventPostBeginSuspendResumePostEndOnRollback", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        mockery.assertIsSatisfied();

        EmbeddableWebSphereUserTransaction ut = EmbeddableUserTransactionImpl.instance();
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        boolean pendingTxOutcome = true;
        try {
            // Begin Tx.
            addUOWEventPostBeginExpectations(mockery, mockObjects);
            ut.begin();
            mockery.assertIsSatisfied();

            // Suspend Tx.
            addUOWEventSuspendExpectations(mockery, mockObjects);
            Transaction tx = tm.suspend();
            mockery.assertIsSatisfied();

            // Resume Tx.
            addUOWEventResumeExpectations(mockery, mockObjects);
            tm.resume(tx);
            mockery.assertIsSatisfied();

            // Rollback Tx.
            addGlobalTxCompletionExpectations(mockery, mockObjects);
            addUOWEventPostEndExpectations(mockery, mockObjects);
            ut.rollback();
            pendingTxOutcome = false;
            mockery.assertIsSatisfied();
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);

            if (pendingTxOutcome) {
                ut.rollback();
            }
        }

        Log.info(c, "testUserTxUOWEventPostBeginSuspendResumePostEndOnRollback", "Exit.");
    }

    /**
     * Test that the core transaction manager invokes the native transaction
     * manager for UOWEvent POST_BEGIN and POST_END.
     * Transaction is begun and committed using the UserTransaction interface.
     */
    @Test
    public void testTxUOWEventPostBeginAndPostEndOnCommit() throws Exception {
        Log.info(c, "testTxUOWEventPostBeginAndPostEndOnCommit", "Entry");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        mockery.assertIsSatisfied();

        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        boolean pendingTxOutcome = true;
        try {
            // Begin Tx.
            addUOWEventPostBeginExpectations(mockery, mockObjects);
            tm.begin();
            mockery.assertIsSatisfied();

            // Commit Tx.
            addGlobalTxCompletionExpectations(mockery, mockObjects);
            addUOWEventPostEndExpectations(mockery, mockObjects);
            tm.commit();
            pendingTxOutcome = false;
            mockery.assertIsSatisfied();
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);

            if (pendingTxOutcome) {
                tm.commit();
            }
        }

        Log.info(c, "testTxUOWEventPostBeginAndPostEndOnCommit", "Exit.");
    }

    /**
     * Test that the core transaction manager invokes the native transaction
     * manager for UOWEvent POST_BEGIN SUSPEND, RESUME and POST_END.
     * Transaction is begun and rolled back using the (Embeddable)
     * TransactionManager interface.
     */
    @Test
    public void testTxUOWEventPostBeginSuspendResumePostEndOnRollback() throws Exception {
        Log.info(c, "testTxUOWEventPostBeginSuspendResumePostEndOnRollback", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        mockery.assertIsSatisfied();

        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        boolean pendingTxOutcome = true;
        try {
            // Begin Tx.
            addUOWEventPostBeginExpectations(mockery, mockObjects);
            tm.begin();
            mockery.assertIsSatisfied();

            // Suspend Tx.
            addUOWEventSuspendExpectations(mockery, mockObjects);
            Transaction tx = tm.suspend();
            mockery.assertIsSatisfied();

            // Resume Tx.
            addUOWEventResumeExpectations(mockery, mockObjects);
            tm.resume(tx);
            mockery.assertIsSatisfied();

            // Rollback Tx.
            addGlobalTxCompletionExpectations(mockery, mockObjects);
            addUOWEventPostEndExpectations(mockery, mockObjects);
            tm.rollback();
            pendingTxOutcome = false;
            mockery.assertIsSatisfied();
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);

            if (pendingTxOutcome) {
                tm.rollback();
            }
        }

        Log.info(c, "testTxUOWEventPostBeginSuspendResumePostEndOnRollback", "Exit.");
    }

    //========================================================================
    // UOWEvent callback verification tests for local transactions.
    //========================================================================

    /**
     * Test that the core transaction manager invokes the native transaction
     * manager for UOWEvent POST_BEGIN and POST_END.
     * The local transaction is begun and completed (Resolver = Application).
     */
    @Test
    public void testLTCUOWEventPostBeginAndPostEndOnComplete() throws Exception {
        Log.info(c, "testLTCUOWEventPostBeginAndPostEndOnComplete", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        mockery.assertIsSatisfied();

        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        ((UOWCurrent) ltcTm).setUOWEventListener(nativeTM);

        boolean pendingTxOutcome = true;
        try {
            // Begin Local Tx. Use all defaults for activity (false),
            // unresolved action (rollback), and container at boundary (false)
            addUOWEventPostBeginExpectations(mockery, mockObjects);
            ltcTm.begin();
            mockery.assertIsSatisfied();

            // Complete Local Tx. Resolver = Application.
            addUOWEventPostEndExpectations(mockery, mockObjects);
            ltcTm.cleanup();
            pendingTxOutcome = false;
            mockery.assertIsSatisfied();
        } finally {
            ((UOWCurrent) ltcTm).unsetUOWEventListener(nativeTM);

            if (pendingTxOutcome) {
                ltcTm.cleanup();
            }
        }

        Log.info(c, "testLTCUOWEventPostBeginAndPostEndOnComplete", "Exit.");
    }

    /**
     * Test that the core transaction manager invokes the native transaction
     * manager for UOWEvent POST_BEGIN, SUSPEND, RESUME, and POST_END.
     * The local transaction is begun and cleaned up (Resolver = Application).
     */
    @Test
    public void testLTCUOWEventPostBeginAndPostEndOnCleanup() throws Exception {
        Log.info(c, "testLTCUOWEventPostBeginAndPostEndOnCleanup", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        mockery.assertIsSatisfied();

        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        ((UOWCurrent) ltcTm).setUOWEventListener(nativeTM);

        boolean pendingTxOutcome = true;
        try {
            // Begin Local Tx. Use defaults for unresolved action (rollback),
            // and container at boundary (false).
            addUOWEventPostBeginExpectations(mockery, mockObjects);
            ltcTm.begin(false);
            mockery.assertIsSatisfied();

            // Suspend local Tx.
            addUOWEventSuspendExpectations(mockery, mockObjects);
            LocalTransactionCoordinator suspendedTx = ltcTm.suspend();
            mockery.assertIsSatisfied();

            // Resume local Tx.
            addUOWEventResumeExpectations(mockery, mockObjects);
            ltcTm.resume(suspendedTx);
            mockery.assertIsSatisfied();

            // Cleanup local Tx.
            addUOWEventPostEndExpectations(mockery, mockObjects);
            ltcTm.cleanup();
            pendingTxOutcome = false;
            mockery.assertIsSatisfied();
        } finally {
            ((UOWCurrent) ltcTm).unsetUOWEventListener(nativeTM);

            if (pendingTxOutcome) {
                ltcTm.cleanup();
            }
        }

        Log.info(c, "testLTCUOWEventPostBeginAndPostEndOnCleanup", "Exit.");
    }

    /**
     * Test that the core transaction manager invokes the native transaction
     * manager for UOWEvent POST_BEGIN and POST_END.
     * The local transaction is begun and ended.
     * The end action (Resolver = ContainerAtBoundary) will go through the
     * complete path.
     */
    @Test
    public void testLTCUOWEventPostBeginSuspendResumeAndPostEndOnEnd() throws Exception {
        Log.info(c, "testLTCUOWEventPostBeginSuspendResumeAndPostEndOnEnd", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        mockery.assertIsSatisfied();

        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        ((UOWCurrent) ltcTm).setUOWEventListener(nativeTM);

        boolean pendingTxOutcome = true;
        try {
            // Begin local Tx. No defaults. Activity = false, unresolved action = rollback,
            // and container at boundary = true.
            addUOWEventPostBeginExpectations(mockery, mockObjects);
            ltcTm.begin(false, true, true);
            mockery.assertIsSatisfied();

            // End the local transaction (complete path).
            addUOWEventPostEndExpectations(mockery, mockObjects);
            ltcTm.end(LocalTransactionCoordinator.EndModeCommit);
            pendingTxOutcome = false;
            mockery.assertIsSatisfied();
        } finally {
            ((UOWCurrent) ltcTm).unsetUOWEventListener(nativeTM);

            if (pendingTxOutcome) {
                ltcTm.end(LocalTransactionCoordinator.EndModeCommit);
            }
        }

        Log.info(c, "testLTCUOWEventPostBeginSuspendResumeAndPostEndOnEnd", "Exit.");
    }

    //========================================================================
    // ATR4END tests
    //========================================================================

    /** Test that we can enlist our XAResource in a transaction. */
    @Test
    public void testGlobalTran() throws Exception {
        Log.info(c, "testGlobalTran", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Create the identifiers that will be used by the transaction we start.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        this.addEndExpectations(mockery, mockObjects.getRRSServices(), mockObjects.getContextManager(), urToken, urID, context, RRSServices.ATR_COMMIT_ACTION, RRSServices.ATR_OK);

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // OK now these three lines are the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.commit();
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testGlobalTran", "Exit.");
    }

    /** Test ATREND commit failure ATR_BACKED_OUT. */
    @Test
    public void testEndFailure() throws Exception {
        Log.info(c, "testEndFailure", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Create the identifiers that will be used by the transaction we start.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        this.addEndExpectations(mockery, mockObjects.getRRSServices(), mockObjects.getContextManager(), urToken, urID, context, RRSServices.ATR_COMMIT_ACTION,
                                RRSServices.ATR_BACKED_OUT);

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            boolean exceptionThrown = false;
            try {
                tm.commit();
            } catch (RollbackException rbe) {
                exceptionThrown = true;
            }

            assertTrue(exceptionThrown);
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testEndFailure", "Exit.");
    }

    /** Test ATREND commit heuristic. */
    @Test
    public void testEndHeuristic() throws Exception {
        Log.info(c, "testEndHeuristic", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Create the identifiers that will be used by the transaction we start.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        this.addEndExpectations(mockery, mockObjects.getRRSServices(), mockObjects.getContextManager(), urToken, urID, context, RRSServices.ATR_COMMIT_ACTION,
                                RRSServices.ATR_COMMITTED_OUTCOME_MIXED);

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            boolean exceptionThrown = false;
            try {
                tm.commit();
            } catch (HeuristicMixedException hme) {
                exceptionThrown = true;
            }

            assertTrue(exceptionThrown);
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testEndHeuristic", "Exit.");
    }

    /** Test ATREND rollback. */
    @Test
    public void testEndRollback() throws Exception {
        Log.info(c, "testEndRollback", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Create the identifiers that will be used by the transaction we start.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        this.addEndExpectations(mockery, mockObjects.getRRSServices(), mockObjects.getContextManager(), urToken, urID, context, RRSServices.ATR_ROLLBACK_ACTION,
                                RRSServices.ATR_OK);

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.rollback();
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testEndRollback", "Exit.");
    }

    /** Test ATREND rollback with heuristic. */
    @Test
    public void testEndRollbackHeuristic() throws Exception {
        Log.info(c, "testEndRollbackHeuristic", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Create the identifiers that will be used by the transaction we start.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        this.addEndExpectations(mockery, mockObjects.getRRSServices(), mockObjects.getContextManager(), urToken, urID, context, RRSServices.ATR_ROLLBACK_ACTION,
                                RRSServices.ATR_BACKED_OUT_OUTCOME_MIXED);

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.rollback(); // Embeddable TM does not report heuristic on rollback.
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testEndRollbackHeuristic", "Exit.");
    }

    // TODO: Test ATREND with invalid UR token (tran backed out asynchronously).

    //========================================================================
    // ATR4ADCT tests
    //========================================================================
    /** Test ATR4ADCT */
    @Test
    public void testCommitSuspendedTran() throws Exception {
        Log.info(c, "testCommitSuspendedTran", "Entry.");

        // Mock environment for native methods.
        final boolean callForget = true;
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        addADCTExpectations(mockery, mockObjects, RRSServices.ATR_OK, callForget);

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            UOWCoordinator coord = ((UOWCurrent) tm).getUOWCoord();
            Transaction tran = tm.suspend(); // Get tran off the thread so we can't call ATREND.
            tran.commit();
            nativeTM.UOWEvent(coord, UOWEventListener.POST_END, null); // Pretend TM drove commit.
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testCommitSuspendedTran", "Exit.");
    }

    /** Test ATR4ADCT */
    @Test
    public void testCommitSuspendedTranReadOnly() throws Exception {
        Log.info(c, "testCommitSuspendedTranReadOnly", "Entry.");

        // Mock environment for native methods.
        final boolean callForget = false;
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        addADCTExpectations(mockery, mockObjects, RRSServices.ATR_FORGET, callForget);

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            UOWCoordinator coord = ((UOWCurrent) tm).getUOWCoord();
            Transaction tran = tm.suspend(); // Get tran off the thread so we can't call ATREND.
            tran.commit();
            nativeTM.UOWEvent(coord, UOWEventListener.POST_END, null); // Pretend TM drove commit.
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testCommitSuspendedTranReadOnly", "Exit.");
    }

    /** Test ATR4ADCT backout failure. */
    @Test
    public void testCommitSuspendedTranFailBackout() throws Exception {
        Log.info(c, "testCommitSuspendedTranFailBackout", "Entry.");

        // Mock environment for native methods.
        final boolean callForget = false;
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        addADCTExpectations(mockery, mockObjects, RRSServices.ATR_BACKED_OUT, callForget);

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            UOWCoordinator coord = ((UOWCurrent) tm).getUOWCoord();
            Transaction tran = tm.suspend(); // Get tran off the thread so we can't call ATREND.
            boolean caughtException = false;
            try {
                tran.commit();
            } catch (RollbackException rbe) {
                caughtException = true;
            }
            nativeTM.UOWEvent(coord, UOWEventListener.POST_END, null); // Pretend TM drove commit.
            assertTrue(caughtException);
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testCommitSuspendedTranFailBackout", "Exit.");
    }

    /** Test ATR4ADCT heuristic failure. */
    @Test
    public void testCommitSuspendedTranFailHeuristic() throws Exception {
        Log.info(c, "testCommitSuspendedTranFailHeuristic", "Entry.");

        // Mock environment for native methods.
        final boolean callForget = true;
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        addADCTExpectations(mockery, mockObjects, RRSServices.ATR_COMMITTED_OUTCOME_MIXED, callForget);

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            UOWCoordinator coord = ((UOWCurrent) tm).getUOWCoord();
            Transaction tran = tm.suspend(); // Get tran off the thread so we can't call ATREND.
            boolean caughtException = false;
            try {
                tran.commit();
            } catch (HeuristicMixedException hme) {
                caughtException = true;
            }
            nativeTM.UOWEvent(coord, UOWEventListener.POST_END, null); // Pretend TM drove commit.
            assertTrue(caughtException);
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testCommitSuspendedTranFailHeuristic", "Exit.");
    }

    //========================================================================
    // ATR4ACMT tests
    //========================================================================
    /** Test ATR4ACMT */
    @Test
    public void testTwoPhaseCommit() throws Exception {
        Log.info(c, "testTwoPhaseCommit", "Entry.");

        // Mock environment for native methods.
        final boolean callForget = true;
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        addACMTExpectations(mockery, mockObjects, RRSServices.ATR_OK, callForget);

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        int mockRecoveryId = tm.registerResourceInfo(XARES_FACTORY_MOCK, new String("testTwoPhaseCommit"), XARES_LOW_PRIORITY);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.enlist(mockObjects.getMockXAResource(), mockRecoveryId);
            tm.commit();
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testTwoPhaseCommit", "Exit.");
    }

    /** Test ATR4ACMT heuristic */
    @Test
    public void testTwoPhaseCommitHeuristic() throws Exception {
        Log.info(c, "testTwoPhaseCommitHeuristic", "Entry.");

        // Mock environment for native methods.
        final boolean callForget = true;
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        addACMTExpectations(mockery, mockObjects, RRSServices.ATR_COMMITTED_OUTCOME_MIXED, callForget);

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        int mockRecoveryId = tm.registerResourceInfo(XARES_FACTORY_MOCK, new String("testTwoPhaseCommit"), XARES_LOW_PRIORITY);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.enlist(mockObjects.getMockXAResource(), mockRecoveryId);
            boolean caughtException = false;
            try {
                tm.commit();
            } catch (HeuristicMixedException hme) {
                caughtException = true;
            }
            assertTrue(caughtException);
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testTwoPhaseCommitHeuristic", "Exit.");
    }

    /** Test ATR4ACMT UR_STATE_ERROR, application backout, in-forget, backed out (asynchronous backout). */
    @Test
    public void testTwoPhaseCommitAsynchronousBackout() throws Exception {
        Log.info(c, "testTwoPhaseCommitAsynchronousBackout", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();
        final byte[] uriToken = generateURIToken();
        final byte[] uriRegistryToken = generateURIRegistryToken();
        final byte[] ciRegistryToken = generateCIRegistryToken();
        final Sequence commitSequence = mockery.sequence("commitSequence");

        mockery.checking(new Expectations() {
            {
                final RRSServices rrsServices = mockObjects.getRRSServices();
                final ContextManager contextManager = mockObjects.getContextManager();
                final XAResource mockXares = mockObjects.getMockXAResource();

                // Tran manager is going to check to see if context manager is initialized.
                oneOf(contextManager).isInitialized();
                inSequence(commitSequence);
                will(returnValue(true));

                // Context services will create a new context on the begin.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(commitSequence);

                // Begin the transaction
                oneOf(rrsServices).beginTransaction(with(RRSServices.ATR_GLOBAL_MODE));
                inSequence(commitSequence);
                will(returnValue(new BeginTransactionReturnType(0, urToken, urID)));
                oneOf(rrsServices).setWorkIdentifier(with(urToken), with(any(byte[].class)));
                inSequence(commitSequence);
                will(returnValue(0));
                oneOf(contextManager).getCurrentContext();
                inSequence(commitSequence);
                will(returnValue(context));

                // Enlist the mock resource
                oneOf(mockXares).start(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(commitSequence);
                oneOf(mockXares).end(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(commitSequence);

                // End the transaction.  Prepare RRS.
                oneOf(rrsServices).expressURInterest(with(rmRegistryToken), with(context.getContextToken()), with(RRSServices.ATR_PRESUMED_ABORT),
                                                     with(any(byte[].class)) /* nonPData */,
                                                     with(any(byte[].class)) /* pdata */, with(aNull(byte[].class)) /* xid */, with(aNull(byte[].class)) /* parentUrToken */);
                inSequence(commitSequence);
                will(returnValue(new ExpressInterestReturnType(RRSServices.ATR_OK, uriToken, uriRegistryToken, null /* curCtxToken */, null /* urid */, null /* nonPData */, null /*
                                                                                                                                                                                   * diagArea
                                                                                                                                                                                   */, RRSServices.ATR_GLOBAL_MODE, null /*
                                                                                                                                                                                                                          * urToken
                                                                                                                                                                                                                          */)));
                oneOf(rrsServices).setSyncpointControls(with(uriRegistryToken), with(RRSServices.ATR_PREPARE_OK), with(RRSServices.ATR_COMMIT_OK),
                                                        with(RRSServices.ATR_BACKOUT_OK),
                                                        with(RRSServices.ATR_SDSRM));
                inSequence(commitSequence);
                will(returnValue(RRSServices.ATR_OK));
                oneOf(rrsServices).prepareAgentUR(with(uriRegistryToken), with(context.getContextRegistryToken()), with(rmRegistryToken), with(RRSServices.ATR_DEFER_IMPLICIT));
                inSequence(commitSequence);
                will(returnValue(new PrepareAgentURReturnType(RRSServices.ATR_OK, RRSServices.CTX_OK, ciRegistryToken)));

                // Finish off the mock XAResource.
                atMost(1).of(mockXares).prepare(with(any(Xid.class)));
                inSequence(commitSequence);
                will(returnValue(XAResource.XA_OK));
                atMost(1).of(mockXares).commit(with(any(Xid.class)), with(any(Boolean.TYPE)));
                inSequence(commitSequence);

                // Commit RRS -- notice the asynchronous backout and recover.
                int[] asynchronousSideDataArray = new int[] { RRSServices.ATR_BACKOUT_REQUIRED,
                                                              RRSServices.ATR_SDSRM_INITIATED,
                                                              RRSServices.ATR_RESOLVED_BY_INSTALLATION,
                                                              RRSServices.ATR_TERM_SYNCPOINT,
                                                              RRSServices.ATR_IMMEDIATE_BACKOUT,
                                                              RRSServices.ATR_COMMITTED,
                                                              RRSServices.ATR_HEURISTIC_MIX };
                int[] asynchronousSideDataValues = new int[] { RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                               RRSServices.ATR_SIDE_VALUE_SET,
                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET };
                oneOf(rrsServices).commitAgentUR(with(uriRegistryToken), with(ciRegistryToken), with(RRSServices.ATR_DEFER_EXPLICIT));
                inSequence(commitSequence);
                will(returnValue(RRSServices.ATR_UR_STATE_ERROR));
                oneOf(rrsServices).retrieveURData(with(uriToken), with(RRSServices.ATR_EXTENDED_STATES));
                inSequence(commitSequence);
                will(returnValue(new RetrieveURDataReturnType(RRSServices.ATR_OK, urID, RRSServices.ATR_IN_FORGET, urToken)));
                oneOf(rrsServices).retrieveSideInformation(with(uriRegistryToken), with(asynchronousSideDataArray));
                inSequence(commitSequence);
                will(returnValue(new RetrieveSideInformationReturnType(0, asynchronousSideDataValues)));
                oneOf(rrsServices).forgetAgentURInterest(uriRegistryToken, RRSServices.ATR_DEFER);
                inSequence(commitSequence);

                // Clean up the context
                // Suspend the transaction
                oneOf(contextManager).isInitialized();
                inSequence(commitSequence);
                will(returnValue(true));
                oneOf(contextManager).suspend(with(any(UOWCoordinator.class)));
                inSequence(commitSequence);

                oneOf(contextManager).isInitialized();
                inSequence(commitSequence);
                will(returnValue(true));
                oneOf(contextManager).end(with(any(UOWCoordinator.class)));
                inSequence(commitSequence);
            }
        });

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        int mockRecoveryId = tm.registerResourceInfo(XARES_FACTORY_MOCK, new String("testTwoPhaseCommit"), XARES_LOW_PRIORITY);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.enlist(mockObjects.getMockXAResource(), mockRecoveryId);
            boolean caughtException = false;
            try {
                tm.commit();
            } catch (HeuristicMixedException hme) {
                caughtException = true;
            }
            assertTrue(caughtException);
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testTwoPhaseCommitAsynchronousBackout", "Exit.");
    }

    /** Test ATR4ACMT UR_STATE_ERROR, resolved-in-doubt, in-forget, committed */
    @Test
    public void testTwoPhaseCommitAsynchronousCommit() throws Exception {
        Log.info(c, "testTwoPhaseCommitAsynchronousCommit", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();
        final byte[] uriToken = generateURIToken();
        final byte[] uriRegistryToken = generateURIRegistryToken();
        final byte[] ciRegistryToken = generateCIRegistryToken();
        final Sequence commitSequence = mockery.sequence("commitSequence");

        mockery.checking(new Expectations() {
            {
                final RRSServices rrsServices = mockObjects.getRRSServices();
                final ContextManager contextManager = mockObjects.getContextManager();
                final XAResource mockXares = mockObjects.getMockXAResource();

                // Tran manager is going to check to see if context manager is initialized.
                oneOf(contextManager).isInitialized();
                inSequence(commitSequence);
                will(returnValue(true));

                // Context services will create a new context on the begin.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(commitSequence);

                // Begin the transaction
                oneOf(rrsServices).beginTransaction(with(RRSServices.ATR_GLOBAL_MODE));
                inSequence(commitSequence);
                will(returnValue(new BeginTransactionReturnType(0, urToken, urID)));
                oneOf(rrsServices).setWorkIdentifier(with(urToken), with(any(byte[].class)));
                inSequence(commitSequence);
                will(returnValue(0));
                oneOf(contextManager).getCurrentContext();
                inSequence(commitSequence);
                will(returnValue(context));

                // Enlist the mock resource
                oneOf(mockXares).start(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(commitSequence);
                oneOf(mockXares).end(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(commitSequence);

                // End the transaction.  Prepare RRS.
                oneOf(rrsServices).expressURInterest(with(rmRegistryToken), with(context.getContextToken()), with(RRSServices.ATR_PRESUMED_ABORT),
                                                     with(any(byte[].class)) /* nonPData */,
                                                     with(any(byte[].class)) /* pdata */, with(aNull(byte[].class)) /* xid */, with(aNull(byte[].class)) /* parentUrToken */);
                inSequence(commitSequence);
                will(returnValue(new ExpressInterestReturnType(RRSServices.ATR_OK, uriToken, uriRegistryToken, null /* curCtxToken */, null /* urid */, null /* nonPData */, null /*
                                                                                                                                                                                   * diagArea
                                                                                                                                                                                   */, RRSServices.ATR_GLOBAL_MODE, null /*
                                                                                                                                                                                                                          * urToken
                                                                                                                                                                                                                          */)));
                oneOf(rrsServices).setSyncpointControls(with(uriRegistryToken), with(RRSServices.ATR_PREPARE_OK), with(RRSServices.ATR_COMMIT_OK),
                                                        with(RRSServices.ATR_BACKOUT_OK),
                                                        with(RRSServices.ATR_SDSRM));
                inSequence(commitSequence);
                will(returnValue(RRSServices.ATR_OK));
                oneOf(rrsServices).prepareAgentUR(with(uriRegistryToken), with(context.getContextRegistryToken()), with(rmRegistryToken), with(RRSServices.ATR_DEFER_IMPLICIT));
                inSequence(commitSequence);
                will(returnValue(new PrepareAgentURReturnType(RRSServices.ATR_OK, RRSServices.CTX_OK, ciRegistryToken)));

                // Finish off the mock XAResource.
                atMost(1).of(mockXares).prepare(with(any(Xid.class)));
                inSequence(commitSequence);
                will(returnValue(XAResource.XA_OK));
                atMost(1).of(mockXares).commit(with(any(Xid.class)), with(any(Boolean.TYPE)));
                inSequence(commitSequence);

                // Commit RRS -- notice the asynchronous backout and recover.
                int[] asynchronousSideDataArray = new int[] { RRSServices.ATR_BACKOUT_REQUIRED,
                                                              RRSServices.ATR_SDSRM_INITIATED,
                                                              RRSServices.ATR_RESOLVED_BY_INSTALLATION,
                                                              RRSServices.ATR_TERM_SYNCPOINT,
                                                              RRSServices.ATR_IMMEDIATE_BACKOUT,
                                                              RRSServices.ATR_COMMITTED,
                                                              RRSServices.ATR_HEURISTIC_MIX };
                int[] asynchronousSideDataValues = new int[] { RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                               RRSServices.ATR_SIDE_VALUE_SET,
                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                               RRSServices.ATR_SIDE_VALUE_SET,
                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET };
                oneOf(rrsServices).commitAgentUR(with(uriRegistryToken), with(ciRegistryToken), with(RRSServices.ATR_DEFER_EXPLICIT));
                inSequence(commitSequence);
                will(returnValue(RRSServices.ATR_UR_STATE_ERROR));
                oneOf(rrsServices).retrieveURData(with(uriToken), with(RRSServices.ATR_EXTENDED_STATES));
                inSequence(commitSequence);
                will(returnValue(new RetrieveURDataReturnType(RRSServices.ATR_OK, urID, RRSServices.ATR_IN_FORGET, urToken)));
                oneOf(rrsServices).retrieveSideInformation(with(uriRegistryToken), with(asynchronousSideDataArray));
                inSequence(commitSequence);
                will(returnValue(new RetrieveSideInformationReturnType(0, asynchronousSideDataValues)));
                oneOf(rrsServices).forgetAgentURInterest(uriRegistryToken, RRSServices.ATR_DEFER);
                inSequence(commitSequence);

                // Clean up the context
                // Suspend the transaction
                oneOf(contextManager).isInitialized();
                inSequence(commitSequence);
                will(returnValue(true));
                oneOf(contextManager).suspend(with(any(UOWCoordinator.class)));
                inSequence(commitSequence);

                oneOf(contextManager).isInitialized();
                inSequence(commitSequence);
                will(returnValue(true));
                oneOf(contextManager).end(with(any(UOWCoordinator.class)));
                inSequence(commitSequence);
            }
        });

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        int mockRecoveryId = tm.registerResourceInfo(XARES_FACTORY_MOCK, new String("testTwoPhaseCommit"), XARES_LOW_PRIORITY);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.enlist(mockObjects.getMockXAResource(), mockRecoveryId);
            tm.commit();
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testTwoPhaseCommitAsynchronousCommit", "Exit.");
    }

    //========================================================================
    // ATR4APRP tests (basic test covered in ATR4ACMT)
    //========================================================================
    /** Test ATR4APRP when prepare votes forget */
    @Test
    public void testTwoPhaseCommitPrepareVoteForget() throws Exception {
        Log.info(c, "testTwoPhaseCommitPrepareVoteForget", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        addAPRPExpectations(mockery, mockObjects, RRSServices.ATR_FORGET, true /* Commit XARES */, false /* Don't call backout */);

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        int mockRecoveryId = tm.registerResourceInfo(XARES_FACTORY_MOCK, new String("testTwoPhaseCommit"), XARES_LOW_PRIORITY);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.enlist(mockObjects.getMockXAResource(), mockRecoveryId);
            tm.commit();
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testTwoPhaseCommitPrepareVoteForget", "Exit.");
    }

    /** Test ATR4APRP when prepare votes backout */
    @Test
    public void testTwoPhaseCommitPrepareVoteBackout() throws Exception {
        Log.info(c, "testTwoPhaseCommitPrepareVoteBackout", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        addAPRPExpectations(mockery, mockObjects, RRSServices.ATR_BACKED_OUT, false /* backout XARES */, false /* Don't call backout */);

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        int mockRecoveryId = tm.registerResourceInfo(XARES_FACTORY_MOCK, new String("testTwoPhaseCommit"), XARES_LOW_PRIORITY);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.enlist(mockObjects.getMockXAResource(), mockRecoveryId);
            boolean caughtException = false;
            try {
                tm.commit();
            } catch (RollbackException rbe) {
                caughtException = true;
            }
            assertTrue(caughtException);
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testTwoPhaseCommitPrepareVoteBackout", "Exit.");
    }

    /** Test ATR4APRP when prepare has a heuristic. */
    @Test
    public void testTwoPhaseCommitPrepareVoteHeuristic() throws Exception {
        Log.info(c, "testTwoPhaseCommitPrepareVoteHeuristic", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        addAPRPExpectations(mockery, mockObjects, RRSServices.ATR_BACKED_OUT_OUTCOME_MIXED, false /* backout XARES */, false /* Don't call backout */);

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        int mockRecoveryId = tm.registerResourceInfo(XARES_FACTORY_MOCK, new String("testTwoPhaseCommit"), XARES_LOW_PRIORITY);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.enlist(mockObjects.getMockXAResource(), mockRecoveryId);
            boolean caughtException = false;
            try {
                tm.commit();
            } catch (HeuristicMixedException hme) {
                caughtException = true;
            }
            assertTrue(caughtException);
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testTwoPhaseCommitPrepareVoteHeuristic", "Exit.");
    }

    //========================================================================
    // ATR4ABAK tests (basic test covered in ATR4ACMT)
    //========================================================================
    /** Test ATR4ABAK normal case. */
    @Test
    public void testBackoutSuspendedTran() throws Exception {
        Log.info(c, "testBackoutSuspendedTran", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        addABAKExpectations(mockery, mockObjects, RRSServices.ATR_OK);

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            UOWCoordinator coord = ((UOWCurrent) tm).getUOWCoord();
            Transaction tran = tm.suspend(); // Get tran off the thread so we can't call ATREND.
            tran.rollback();
            nativeTM.UOWEvent(coord, UOWEventListener.POST_END, null); // Pretend TM drove commit.
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testBackoutSuspendedTran", "Exit.");
    }

    /** Test ATR4ABAK heuristic case. */
    @Test
    public void testBackoutSuspendedTranHeuristic() throws Exception {
        Log.info(c, "testBackoutSuspendedTranHeuristic", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        addABAKExpectations(mockery, mockObjects, RRSServices.ATR_BACKED_OUT_OUTCOME_MIXED);

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            UOWCoordinator coord = ((UOWCurrent) tm).getUOWCoord();
            Transaction tran = tm.suspend(); // Get tran off the thread so we can't call ATREND.
            tran.rollback(); // Can't throw a heuristic on backout.
            nativeTM.UOWEvent(coord, UOWEventListener.POST_END, null); // Pretend TM drove commit.
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testBackoutSuspendedTranHeuristic", "Exit.");
    }

    /** Test ATR4ABAK after a successful prepare (another XARES votes rollabck). */
    @Test
    public void testBackoutSuspendedTranPrepareBackout() throws Exception {
        Log.info(c, "testBackoutSuspendedTranPrepareBackout", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();
        final byte[] uriToken = generateURIToken();
        final byte[] uriRegistryToken = generateURIRegistryToken();
        final byte[] ciRegistryToken = generateCIRegistryToken();
        final Sequence backoutSequence = mockery.sequence("backoutSequence");
        final String mockInFlightState = "in-flight";
        final String mockBackedOutState = "backed-out";
        final States mockXaState = mockery.states("mockXaState").startsAs(mockInFlightState);

        mockery.checking(new Expectations() {
            {
                final ContextManager contextManager = mockObjects.getContextManager();
                final RRSServices rrsServices = mockObjects.getRRSServices();
                final XAResource mockXares = mockObjects.getMockXAResource();

                // Tran manager is going to check to see if context manager is initialized.
                oneOf(contextManager).isInitialized();
                inSequence(backoutSequence);
                will(returnValue(true));

                // Context services will create a new context on the begin.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(backoutSequence);

                // Begin the transaction
                oneOf(rrsServices).beginTransaction(with(RRSServices.ATR_GLOBAL_MODE));
                inSequence(backoutSequence);
                will(returnValue(new BeginTransactionReturnType(0, urToken, urID)));
                oneOf(rrsServices).setWorkIdentifier(with(urToken), with(any(byte[].class)));
                inSequence(backoutSequence);
                will(returnValue(0));
                oneOf(contextManager).getCurrentContext();
                inSequence(backoutSequence);
                will(returnValue(context));

                // Enlist the mock resource
                oneOf(mockXares).start(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(backoutSequence);

                // Suspend the transaction
                oneOf(contextManager).isInitialized();
                inSequence(backoutSequence);
                will(returnValue(true));
                oneOf(contextManager).suspend(with(any(UOWCoordinator.class)));
                inSequence(backoutSequence);

                // End the XAResource
                oneOf(mockXares).end(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(backoutSequence);

                // Prepare the RRS resource
                oneOf(rrsServices).expressURInterest(with(rmRegistryToken), with(context.getContextToken()), with(RRSServices.ATR_PRESUMED_ABORT),
                                                     with(any(byte[].class)) /* nonPData */,
                                                     with(any(byte[].class)) /* pdata */, with(aNull(byte[].class)) /* xid */, with(aNull(byte[].class)) /* parentUrToken */);
                inSequence(backoutSequence);
                will(returnValue(new ExpressInterestReturnType(RRSServices.ATR_OK, uriToken, uriRegistryToken, null /* curCtxToken */, null /* urid */, null /* nonPData */, null /*
                                                                                                                                                                                   * diagArea
                                                                                                                                                                                   */, RRSServices.ATR_GLOBAL_MODE, null /*
                                                                                                                                                                                                                          * urToken
                                                                                                                                                                                                                          */)));
                oneOf(rrsServices).setSyncpointControls(with(uriRegistryToken), with(RRSServices.ATR_PREPARE_OK), with(RRSServices.ATR_COMMIT_OK),
                                                        with(RRSServices.ATR_BACKOUT_OK),
                                                        with(RRSServices.ATR_SDSRM));
                inSequence(backoutSequence);
                will(returnValue(RRSServices.ATR_OK));

                oneOf(rrsServices).prepareAgentUR(with(uriRegistryToken), with(context.getContextRegistryToken()), with(rmRegistryToken), with(RRSServices.ATR_DEFER_IMPLICIT));
                inSequence(backoutSequence);
                will(returnValue(new PrepareAgentURReturnType(RRSServices.ATR_OK, RRSServices.CTX_OK, ciRegistryToken)));

                // Resolve the XA resource.  We support the tran manager calling prepare or commit.
                atMost(1).of(mockXares).prepare(with(any(Xid.class)));
                inSequence(backoutSequence);
                when(mockXaState.is(mockInFlightState));
                will(throwException(new XAException(XAException.XA_RBROLLBACK)));
                then(mockXaState.is(mockBackedOutState));

                atMost(1).of(mockXares).commit(with(any(Xid.class)), with(true));
                inSequence(backoutSequence);
                when(mockXaState.is(mockInFlightState));
                will(throwException(new XAException(XAException.XA_RBROLLBACK)));
                then(mockXaState.is(mockBackedOutState));

                oneOf(contextManager).getCurrentContext();
                inSequence(backoutSequence);
                will(returnValue(context));
                oneOf(rrsServices).backoutAgentUR(with(uriRegistryToken), with(ciRegistryToken), with(RRSServices.ATR_DEFER_EXPLICIT));
                inSequence(backoutSequence);
                when(mockXaState.is(mockBackedOutState));
                will(returnValue(RRSServices.ATR_OK));

                oneOf(rrsServices).forgetAgentURInterest(with(uriRegistryToken), with(RRSServices.ATR_DEFER));
                inSequence(backoutSequence);
                when(mockXaState.is(mockBackedOutState));
                will(returnValue(RRSServices.ATR_OK));

                // Clean up the context
                oneOf(contextManager).isInitialized();
                inSequence(backoutSequence);
                will(returnValue(true));
                oneOf(contextManager).end(with(any(UOWCoordinator.class)));
                inSequence(backoutSequence);
            }
        });

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        int mockRecoveryId = tm.registerResourceInfo(XARES_FACTORY_MOCK, new String("testBackoutSuspendedTranPrepareBackout"), XARES_LOW_PRIORITY);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.enlist(mockObjects.getMockXAResource(), mockRecoveryId);
            UOWCoordinator coord = ((UOWCurrent) tm).getUOWCoord();
            Transaction tran = tm.suspend(); // Get tran off the thread so we can't call ATREND.
            boolean caughtException = false;
            try {
                tran.commit();
            } catch (RollbackException rbe) {
                caughtException = true;
            }
            nativeTM.UOWEvent(coord, UOWEventListener.POST_END, null); // Pretend TM drove commit.
            assertTrue(caughtException);
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testBackoutSuspendedTranPrepareBackout", "Exit.");
    }

    /** Test ATR4ABAK UR_STATE_ERROR, application backout, in-forget, backed out (asynchronous backout). */
    @Test
    public void testBackoutAsynchronousBackout() throws Exception {
        Log.info(c, "testBackoutAsynchronousBackout", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();
        final byte[] uriToken = generateURIToken();
        final byte[] uriRegistryToken = generateURIRegistryToken();
        final byte[] ciRegistryToken = generateCIRegistryToken();
        final Sequence backoutSequence = mockery.sequence("backoutSequence");

        mockery.checking(new Expectations() {
            {
                final ContextManager contextManager = mockObjects.getContextManager();
                final RRSServices rrsServices = mockObjects.getRRSServices();
                final XAResource mockXares = mockObjects.getMockXAResource();

                // Tran manager is going to check to see if context manager is initialized.
                oneOf(contextManager).isInitialized();
                inSequence(backoutSequence);
                will(returnValue(true));

                // Context services will create a new context on the begin.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(backoutSequence);

                // Begin the transaction
                oneOf(rrsServices).beginTransaction(with(RRSServices.ATR_GLOBAL_MODE));
                inSequence(backoutSequence);
                will(returnValue(new BeginTransactionReturnType(0, urToken, urID)));
                oneOf(rrsServices).setWorkIdentifier(with(urToken), with(any(byte[].class)));
                inSequence(backoutSequence);
                will(returnValue(0));
                oneOf(contextManager).getCurrentContext();
                inSequence(backoutSequence);
                will(returnValue(context));

                // Enlist the mock resource
                oneOf(mockXares).start(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(backoutSequence);
                oneOf(mockXares).end(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(backoutSequence);

                // End the transaction.  Prepare RRS.
                oneOf(rrsServices).expressURInterest(with(rmRegistryToken), with(context.getContextToken()), with(RRSServices.ATR_PRESUMED_ABORT),
                                                     with(any(byte[].class)) /* nonPData */,
                                                     with(any(byte[].class)) /* pdata */, with(aNull(byte[].class)) /* xid */, with(aNull(byte[].class)) /* parentUrToken */);
                inSequence(backoutSequence);
                will(returnValue(new ExpressInterestReturnType(RRSServices.ATR_OK, uriToken, uriRegistryToken, null /* curCtxToken */, null /* urid */, null /* nonPData */, null /*
                                                                                                                                                                                   * diagArea
                                                                                                                                                                                   */, RRSServices.ATR_GLOBAL_MODE, null /*
                                                                                                                                                                                                                          * urToken
                                                                                                                                                                                                                          */)));
                oneOf(rrsServices).setSyncpointControls(with(uriRegistryToken), with(RRSServices.ATR_PREPARE_OK), with(RRSServices.ATR_COMMIT_OK),
                                                        with(RRSServices.ATR_BACKOUT_OK),
                                                        with(RRSServices.ATR_SDSRM));
                inSequence(backoutSequence);
                will(returnValue(RRSServices.ATR_OK));
                oneOf(rrsServices).prepareAgentUR(with(uriRegistryToken), with(context.getContextRegistryToken()), with(rmRegistryToken), with(RRSServices.ATR_DEFER_IMPLICIT));
                inSequence(backoutSequence);
                will(returnValue(new PrepareAgentURReturnType(RRSServices.ATR_OK, RRSServices.CTX_OK, ciRegistryToken)));

                // Finish off the mock XAResource.  Make it backout.
                atMost(1).of(mockXares).prepare(with(any(Xid.class)));
                inSequence(backoutSequence);
                will(throwException(new XAException(XAException.XA_RBROLLBACK)));
                atMost(1).of(mockXares).commit(with(any(Xid.class)), with(any(Boolean.TYPE)));
                inSequence(backoutSequence);
                will(throwException(new XAException(XAException.XA_RBROLLBACK)));

                // Backout RRS -- notice the asynchronous backout and recover.
                int[] asynchronousSideDataArray = new int[] { RRSServices.ATR_BACKOUT_REQUIRED,
                                                              RRSServices.ATR_SDSRM_INITIATED,
                                                              RRSServices.ATR_RESOLVED_BY_INSTALLATION,
                                                              RRSServices.ATR_TERM_SYNCPOINT,
                                                              RRSServices.ATR_IMMEDIATE_BACKOUT,
                                                              RRSServices.ATR_COMMITTED,
                                                              RRSServices.ATR_HEURISTIC_MIX };
                int[] asynchronousSideDataValues = new int[] { RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                               RRSServices.ATR_SIDE_VALUE_SET,
                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET };
                oneOf(contextManager).getCurrentContext();
                inSequence(backoutSequence);
                will(returnValue(context));
                oneOf(rrsServices).backoutAgentUR(with(uriRegistryToken), with(ciRegistryToken), with(RRSServices.ATR_DEFER_EXPLICIT));
                inSequence(backoutSequence);
                will(returnValue(RRSServices.ATR_UR_STATE_ERROR));
                oneOf(rrsServices).retrieveURData(with(uriToken), with(RRSServices.ATR_EXTENDED_STATES));
                inSequence(backoutSequence);
                will(returnValue(new RetrieveURDataReturnType(RRSServices.ATR_OK, urID, RRSServices.ATR_IN_FORGET, urToken)));
                oneOf(rrsServices).retrieveSideInformation(with(uriRegistryToken), with(asynchronousSideDataArray));
                inSequence(backoutSequence);
                will(returnValue(new RetrieveSideInformationReturnType(0, asynchronousSideDataValues)));
                oneOf(rrsServices).forgetAgentURInterest(with(uriRegistryToken), with(RRSServices.ATR_DEFER));
                inSequence(backoutSequence);
                will(returnValue(RRSServices.ATR_OK));

                // Clean up the context
                // Suspend the transaction
                oneOf(contextManager).isInitialized();
                inSequence(backoutSequence);
                will(returnValue(true));
                oneOf(contextManager).suspend(with(any(UOWCoordinator.class)));
                inSequence(backoutSequence);

                oneOf(contextManager).isInitialized();
                inSequence(backoutSequence);
                will(returnValue(true));
                oneOf(contextManager).end(with(any(UOWCoordinator.class)));
                inSequence(backoutSequence);
            }
        });

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        int mockRecoveryId = tm.registerResourceInfo(XARES_FACTORY_MOCK, new String("testTwoPhaseCommit"), XARES_LOW_PRIORITY);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.enlist(mockObjects.getMockXAResource(), mockRecoveryId);
            boolean caughtException = false;
            try {
                tm.commit();
            } catch (RollbackException rbe) {
                caughtException = true;
            }
            assertTrue(caughtException);
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testBackoutAsynchronousBackout", "Exit.");
    }

    //========================================================================
    // ATR4ABEG tests (basic test covered several times over)
    //========================================================================
    /** Test ATR4ABEG error. */
    @Test
    public void testBeginGlobalTranError() throws Exception {
        Log.info(c, "testBeginGlobalTranError", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final Sequence beginSequence = mockery.sequence("beginSequence");

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        mockery.checking(new Expectations() {
            {
                ContextManager contextManager = mockObjects.getContextManager();
                RRSServices rrsServices = mockObjects.getRRSServices();

                // Tran manager is going to check to see if context manager is initialized.
                oneOf(contextManager).isInitialized();
                inSequence(beginSequence);
                will(returnValue(true));

                // Context services will create a new context on the begin.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(beginSequence);

                // Begin the transaction
                oneOf(rrsServices).beginTransaction(with(RRSServices.ATR_GLOBAL_MODE));
                inSequence(beginSequence);
                will(returnValue(new BeginTransactionReturnType(RRSServices.ATR_UR_STATE_ERROR, null, null)));

                // Suspend the transaction
                oneOf(contextManager).isInitialized();
                inSequence(beginSequence);
                will(returnValue(true));
                oneOf(contextManager).suspend(with(any(UOWCoordinator.class)));
                inSequence(beginSequence);

                // Clean up the context
                oneOf(contextManager).isInitialized();
                inSequence(beginSequence);
                will(returnValue(true));
                oneOf(contextManager).end(with(any(UOWCoordinator.class)));
                inSequence(beginSequence);
            }
        });

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            boolean caughtException = false;
            try {
                tm.enlist(xares, recoveryId);
            } catch (Exception e) {
                caughtException = true;
            }
            assertTrue(caughtException);
            caughtException = false;
            try {
                tm.commit();
            } catch (RollbackException rbe) {
                caughtException = true;
            }
            assertTrue(caughtException);
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testBeginGlobalTranError", "Exit.");
    }

    //========================================================================
    // ATR4SWID tests
    //========================================================================
    /** Test ATR4SWID error. */
    @Test
    public void testSetWorkIdError() throws Exception {
        Log.info(c, "testSetWorkIdError", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final Sequence swidSequence = mockery.sequence("swidSequence");

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();

        mockery.checking(new Expectations() {
            {
                ContextManager contextManager = mockObjects.getContextManager();
                RRSServices rrsServices = mockObjects.getRRSServices();

                // Tran manager is going to check to see if context manager is initialized.
                oneOf(contextManager).isInitialized();
                inSequence(swidSequence);
                will(returnValue(true));

                // Context services will create a new context on the begin.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(swidSequence);

                // Begin the transaction
                oneOf(rrsServices).beginTransaction(with(RRSServices.ATR_GLOBAL_MODE));
                inSequence(swidSequence);
                will(returnValue(new BeginTransactionReturnType(RRSServices.ATR_OK, urToken, urID)));
                oneOf(rrsServices).setWorkIdentifier(with(urToken), with(any(byte[].class)));
                inSequence(swidSequence);
                will(returnValue(RRSServices.ATR_UR_TOKEN_INV));

                // Suspend the transaction
                oneOf(contextManager).isInitialized();
                inSequence(swidSequence);
                will(returnValue(true));
                oneOf(contextManager).suspend(with(any(UOWCoordinator.class)));
                inSequence(swidSequence);

                // Clean up the context
                oneOf(contextManager).isInitialized();
                inSequence(swidSequence);
                will(returnValue(true));
                oneOf(contextManager).end(with(any(UOWCoordinator.class)));
                inSequence(swidSequence);
            }
        });

        // Create and initialize the transaction managers, and XAResource.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        XAResource xares = nativeTM.getXAResource(info);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            boolean caughtException = false;
            try {
                tm.enlist(xares, recoveryId);
            } catch (Exception e) {
                caughtException = true;
            }
            assertTrue(caughtException);
            caughtException = false;
            try {
                tm.commit();
            } catch (RollbackException rbe) {
                caughtException = true;
            }
            assertTrue(caughtException);
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testSetWorkIdError", "Exit.");
    }

    // TODO: Express interest tests (context gone, general errors)

    //========================================================================
    // Local transaction tests.
    //========================================================================

    /**
     * Test normal enlist for cleanup and cleanup processing. No danglers.
     * LTC Resolver = Application
     */
    @Test
    public void testLTCNormalEnlistForCleanup() throws Exception {
        Log.info(c, "testLTCNormalEnlistForCleanup", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final Context context = generateContext();
        final OnePhaseXAResource mockOnePhaseXAResource = mockObjects.getOnePhaseXAResource();

        // Set the restart and common LTC expectations.
        addRestartExpectations(mockery, mockObjects);
        addLTCCommonExpectations(mockery, mockObjects, context);

        // Get the local and native transaction managers.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        ((UOWCurrent) ltcTm).setUOWEventListener(nativeTM);

        // Test:
        try {
            // Begin LTC. Defaults: Resolver = Application. Unresolved Action = Rollback.
            ltcTm.begin(false);
            LocalTransactionCoordinator ltc = ltcTm.getLocalTranCoord();
            OnePhaseXAResource natvOnePhaseXAResource = (OnePhaseXAResource) nativeTM.getOnePhaseXAResource((UOWCoordinator) ltc);

            // Enlist resources. Simulate connection obtained/used.
            ltc.enlistForCleanup(natvOnePhaseXAResource);
            nativeTM.enlist((UOWCoordinator) ltc, natvOnePhaseXAResource);
            ltc.enlistForCleanup(mockOnePhaseXAResource);

            // Delist resources. Simulate application call to commit/rollback the connection.
            ltc.delistFromCleanup(natvOnePhaseXAResource);
            nativeTM.delist((UOWCoordinator) ltc, natvOnePhaseXAResource);
            ltc.delistFromCleanup(mockOnePhaseXAResource);

            // LTC ends. Nothing to cleanup. No RRS activity.
            ltc.end(LocalTransactionCoordinator.EndModeCommit);
        } finally {
            ((UOWCurrent) ltcTm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our maps are clean and that expectations are met.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testLTCNormalEnlistForCleanup", "Exit.");
    }

    /**
     * Test enlist for cleanup with a dangler(s). LTC Resolver = Application.
     * Dangler(s) should be rolled back.
     */
    @Test
    public void testLTCDanglerCleanupWithUnresolvedActionRollback() throws Exception {
        Log.info(c, "testLTCDanglerCleanupWithUnresolvedActionRollback", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final RRSServices rrsServices = mockObjects.getRRSServices();
        final Context context = generateContext();
        final OnePhaseXAResource mockOnePhaseXAResource = mockObjects.getOnePhaseXAResource();

        // Set the restart and common LTC expectations.
        addRestartExpectations(mockery, mockObjects);
        addLTCCommonExpectations(mockery, mockObjects, context);

        // Set test specific expectations for end of LTC.
        mockery.checking(new Expectations() {
            {
                // End of LTC. cleanup will detect dangler and roll it back. RRS.endUR should be called.
                oneOf(mockOnePhaseXAResource).rollback(with(any(Xid.class)));
                oneOf(mockOnePhaseXAResource).getResourceName();
                oneOf(mockObjects.getContextManager()).getCurrentContext();
                will(returnValue(context));
                oneOf(rrsServices).retrieveSideInformationFast(with(context.getContextToken()), with(0));
                will(returnValue(new RetrieveSideInformationFastReturnType(RRSServices.ATR_OK, RRSServices.ATR_LOCAL_MODE_MASK)));
                oneOf(rrsServices).endUR(RRSServices.ATR_ROLLBACK_ACTION, null);
                will(returnValue(RRSServices.ATR_OK));
            }
        });

        // Get the local and native transaction managers.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        ((UOWCurrent) ltcTm).setUOWEventListener(nativeTM);

        // Test.
        try {
            boolean caughtExpectedException = false;

            // Begin LTC. Explicit: Resolver = Application. Unresolved Action = Rollback.
            ltcTm.begin(false, false, false);
            LocalTransactionCoordinator ltc = ltcTm.getLocalTranCoord();
            OnePhaseXAResource natvOnePhaseXAResource = (OnePhaseXAResource) nativeTM.getOnePhaseXAResource((UOWCoordinator) ltc);

            // Enlist resource. Simulate connection obtained/used.
            ltc.enlistForCleanup(natvOnePhaseXAResource);
            nativeTM.enlist((UOWCoordinator) ltc, natvOnePhaseXAResource);
            ltc.enlistForCleanup(mockOnePhaseXAResource);

            // LTC Ends. Dangling enlistments should be rolled back.
            try {
                ltc.end(LocalTransactionCoordinator.EndModeCommit);
            } catch (com.ibm.ws.LocalTransaction.RolledbackException rbe) {
                caughtExpectedException = true;
            }
            assertTrue(caughtExpectedException);
        } finally {
            ((UOWCurrent) ltcTm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our maps are clean and that expectations are met.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testLTCDanglerCleanupWithUnresolvedActionRollback", "Exit.");
    }

    /**
     * Test enlist for cleanup with a dangler. LTC Resolver = Application.
     * Dangler(s) should be committed.
     */
    @Test
    public void testLTCDanglerCleanupWithUnresolvedActionCommit() throws Exception {
        Log.info(c, "testLTCDanglerCleanupWithUnresolvedActionCommit", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final RRSServices rrsServices = mockObjects.getRRSServices();
        final Context context = generateContext();
        final OnePhaseXAResource mockOnePhaseXAResource = mockObjects.getOnePhaseXAResource();

        // Set the restart and common LTC expectations.
        addRestartExpectations(mockery, mockObjects);
        addLTCCommonExpectations(mockery, mockObjects, context);

        // Set test specific expectations for end of LTC.
        mockery.checking(new Expectations() {
            {
                // End of LTC. cleanup will detect dangler and commit it. RRS.endUR should be called.
                oneOf(mockOnePhaseXAResource).commit(with(any(Xid.class)), with(any(Boolean.TYPE)));
                oneOf(mockObjects.getContextManager()).getCurrentContext();
                will(returnValue(context));
                oneOf(rrsServices).retrieveSideInformationFast(with(context.getContextToken()), with(0));
                will(returnValue(new RetrieveSideInformationFastReturnType(RRSServices.ATR_OK, RRSServices.ATR_LOCAL_MODE_MASK)));
                oneOf(rrsServices).endUR(RRSServices.ATR_COMMIT_ACTION, null);
                will(returnValue(RRSServices.ATR_OK));
            }
        });

        // Get the local and native transaction managers.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        ((UOWCurrent) ltcTm).setUOWEventListener(nativeTM);

        // Test.
        try {
            boolean caughtException = false;

            // Begin LTC. Explicit: Resolver = Application. Unresolved Action = Commit.
            ltcTm.begin(false, true, false);
            LocalTransactionCoordinator ltc = ltcTm.getLocalTranCoord();
            UOWCoordinator uowc = (UOWCoordinator) ltcTm.getLocalTranCoord();
            OnePhaseXAResource natvOnePhaseXAResource = (OnePhaseXAResource) nativeTM.getOnePhaseXAResource(uowc);

            // Enlist resource. Simulate connection obtained/used.
            ltc.enlistForCleanup(natvOnePhaseXAResource);
            nativeTM.enlist(uowc, natvOnePhaseXAResource);
            ltc.enlistForCleanup(mockOnePhaseXAResource);

            // LTC Ends. Dangling enlistments should be committed. No exception expected.
            try {
                ltc.end(LocalTransactionCoordinator.EndModeCommit);
            } catch (Throwable t) {
                caughtException = true;
            }
            assertTrue(!caughtException);
        } finally {
            ((UOWCurrent) ltcTm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our maps are clean and that expectations are met.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testLTCDanglerCleanupWithUnresolvedActionCommit", "Exit.");
    }

    /**
     * Test enlist for cleanup with multiple danglers. LTC Resolver = Application.
     * Danglers should be rolled back.
     */
    @Test
    public void testLTCMultipleDanglerCleanupWithUnresolvedActionRollback() throws Exception {
        Log.info(c, "testLTCMultipleDanglerCleanupWithUnresolvedActionRollback", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final RRSServices rrsServices = mockObjects.getRRSServices();
        final Context context = generateContext();
        final OnePhaseXAResource mockOnePhaseXAResource = mockObjects.getOnePhaseXAResource();

        // Set the restart and common LTC expectations.
        addRestartExpectations(mockery, mockObjects);
        addLTCCommonExpectations(mockery, mockObjects, context);

        // Set test specific expectations for end of LTC.
        mockery.checking(new Expectations() {
            {
                // End of LTC. cleanup will detect dangler and roll them back. RRS.endUR should be called.
                exactly(5).of(mockOnePhaseXAResource).rollback(with(any(Xid.class)));
                exactly(5).of(mockOnePhaseXAResource).getResourceName();
                oneOf(mockObjects.getContextManager()).getCurrentContext();
                will(returnValue(context));
                oneOf(rrsServices).retrieveSideInformationFast(with(context.getContextToken()), with(0));
                will(returnValue(new RetrieveSideInformationFastReturnType(RRSServices.ATR_OK, RRSServices.ATR_LOCAL_MODE_MASK)));
                oneOf(rrsServices).endUR(RRSServices.ATR_ROLLBACK_ACTION, null);
                will(returnValue(RRSServices.ATR_OK));
            }
        });

        // Get the local and native transaction managers.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        ((UOWCurrent) ltcTm).setUOWEventListener(nativeTM);

        // Test.
        try {
            boolean caughtExpectedException = false;

            // Begin LTC. Explicit: Resolver = Application. Unresolved Action = Rollback.
            ltcTm.begin(false, false, false);
            LocalTransactionCoordinator ltc = ltcTm.getLocalTranCoord();
            UOWCoordinator uowc = (UOWCoordinator) ltcTm.getLocalTranCoord();
            OnePhaseXAResource natvOnePhaseXAResource = (OnePhaseXAResource) nativeTM.getOnePhaseXAResource(uowc);

            // Enlist resources. Simulate connection obtained/used.
            for (int i = 0; i < 5; i++) {
                nativeTM.enlist(uowc, natvOnePhaseXAResource);
                ltc.enlistForCleanup(mockOnePhaseXAResource);
                ltc.enlistForCleanup(natvOnePhaseXAResource);
            }

            // LTC Ends. Dangling enlistments should be rolled back.
            try {
                ltc.end(LocalTransactionCoordinator.EndModeCommit);
            } catch (com.ibm.ws.LocalTransaction.RolledbackException rbe) {
                caughtExpectedException = true;
            }
            assertTrue(caughtExpectedException);
        } finally {
            ((UOWCurrent) ltcTm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our maps are clean and that expectations are met.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testLTCMultipleDanglerCleanupWithUnresolvedActionRollback", "Exit.");
    }

    /**
     * Test enlist for cleanup with multiple danglers and ATR4END failure.
     * LTC Resolver = Application.
     * Danglers should be committed. ATR4END failure on last native enlistment should be
     * reported by the core local transaction manager.
     */
    @Test
    public void testLTCMultipleDanglerCleanupWithUnresolvedActionCommitAndBadRcFromATR4END() throws Exception {
        Log.info(c, "testLTCMultipleDanglerCleanupWithUnresolvedActionCommitAndBadRcFromATR4END", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final RRSServices rrsServices = mockObjects.getRRSServices();
        final Context context = generateContext();
        final OnePhaseXAResource mockOnePhaseXAResource = mockObjects.getOnePhaseXAResource();

        // Set the restart and common LTC expectations.
        addRestartExpectations(mockery, mockObjects);
        addLTCCommonExpectations(mockery, mockObjects, context);

        // Set test specific expectations for end of LTC.
        mockery.checking(new Expectations() {
            {
                // End of LTC. cleanup will detect dangler and commit them. RRS.endUR should be called.
                exactly(5).of(mockOnePhaseXAResource).commit(with(any(Xid.class)), with(any(Boolean.TYPE)));
                oneOf(mockObjects.getContextManager()).getCurrentContext();
                will(returnValue(context));
                oneOf(rrsServices).retrieveSideInformationFast(with(context.getContextToken()), with(0));
                will(returnValue(new RetrieveSideInformationFastReturnType(RRSServices.ATR_OK, RRSServices.ATR_LOCAL_MODE_MASK)));
                oneOf(rrsServices).endUR(RRSServices.ATR_COMMIT_ACTION, null);
                will(returnValue(RRSServices.ATR_UNEXPECTED_ERROR));
            }
        });

        // Get the local and native transaction managers.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        ((UOWCurrent) ltcTm).setUOWEventListener(nativeTM);

        // Test.
        try {
            boolean caughtExpectedException = false;

            // Begin LTC. Explicit: Resolver = Application. Unresolved Action = Commit.
            ltcTm.begin(false, true, false);
            LocalTransactionCoordinator ltc = ltcTm.getLocalTranCoord();
            UOWCoordinator uowc = (UOWCoordinator) ltcTm.getLocalTranCoord();
            OnePhaseXAResource natvOnePhaseXAResource = (OnePhaseXAResource) nativeTM.getOnePhaseXAResource(uowc);

            // Enlist resources. Simulate connection obtained/used.
            for (int i = 0; i < 5; i++) {
                ltc.enlistForCleanup(natvOnePhaseXAResource);
                nativeTM.enlist(uowc, natvOnePhaseXAResource);
                ltc.enlistForCleanup(mockOnePhaseXAResource);
            }

            // LTC Ends. Dangling enlistments should be committed.
            try {
                ltc.end(LocalTransactionCoordinator.EndModeCommit);
            } catch (InconsistentLocalTranException ilte) {
                caughtExpectedException = true;
                final String[] failedResources = ilte.getFailingResources();
                assertTrue(failedResources.length == 1);
                assertTrue(failedResources[0].equals("NativeLocalXAResource"));
            }
            assertTrue(caughtExpectedException);
        } finally {
            ((UOWCurrent) ltcTm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our maps are clean and that expectations are met.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testLTCMultipleDanglerCleanupWithUnresolvedActionCommitAndBadRcFromATR4END", "Exit.");
    }

    /**
     * Test enlist for cleanup with multiple danglers and no native TM knowledge of the UOW.
     * LTC Resolver = Application.
     * Danglers should be rolled back.
     */
    @Test
    public void testLTCMultipleDanglerCleanupWithUnresolvedActionRollbackAndNativeTMLostUOW() throws Exception {
        Log.info(c, "testLTCMultipleDanglerCleanupWithUnresolvedActionRollbackAndNativeTMLostUOW", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final ContextManager contextManager = mockObjects.getContextManager();
        final OnePhaseXAResource mockOnePhaseXAResource = mockObjects.getOnePhaseXAResource();

        // Set the restart and common LTC expectations.
        addRestartExpectations(mockery, mockObjects);

        // Set test specific expectations for end of LTC.
        mockery.checking(new Expectations() {
            {
                // NatvTxManagerImpl.UOWEvent: POST_BEGIN: Check if context manager is intialized.
                oneOf(contextManager).isInitialized();
                will(returnValue(true));

                // NatvTxManagerImpl.UOWEvent: POST_BEGIN: Calls begin on context manager.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));

                // End of LTC. cleanup will detects dangler and calls endUR.
                exactly(5).of(mockOnePhaseXAResource).rollback(with(any(Xid.class)));
                exactly(5).of(mockOnePhaseXAResource).getResourceName();

                // ATR4END will never get called. We do not know about the UOW.

                // Clean up the context
                oneOf(contextManager).isInitialized();
                will(returnValue(true));
                oneOf(contextManager).end(with(any(UOWCoordinator.class)));
            }
        });

        // Get the local and native transaction managers.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        ((UOWCurrent) ltcTm).setUOWEventListener(nativeTM);

        // Test.
        try {
            boolean caughtExpectedException = false;

            // Begin LTC. Explicit: Resolver = Application. Unresolved Action = Rollback.
            ltcTm.begin(false, false, false);
            LocalTransactionCoordinator ltc = ltcTm.getLocalTranCoord();
            UOWCoordinator uowc = (UOWCoordinator) ltcTm.getLocalTranCoord();
            OnePhaseXAResource natvOnePhaseXAResource = (OnePhaseXAResource) nativeTM.getOnePhaseXAResource(uowc);

            // Enlist resources. Simulate connection obtained/used. Simulate connection obtained/used. Do not enlist with with
            // native TM to simulate that we do not know about the UOW when cleanup is processed.
            for (int i = 0; i < 5; i++) {
                ltc.enlistForCleanup(natvOnePhaseXAResource);
                ltc.enlistForCleanup(mockOnePhaseXAResource);
            }

            // LTC Ends. Dangling enlistments should be rolled back.
            try {
                ltc.end(LocalTransactionCoordinator.EndModeCommit);
            } catch (InconsistentLocalTranException ilte) {
                caughtExpectedException = true;
                final String[] failedResources = ilte.getFailingResources();
                assertTrue(failedResources.length == 5);
                for (int i = 0; i < 5; i++) {
                    assertTrue(failedResources[i].equals("NativeLocalXAResource"));
                }
            }
            assertTrue(caughtExpectedException);
        } finally {
            ((UOWCurrent) ltcTm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our maps are clean and that expectations are met.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testLTCMultipleDanglerCleanupWithUnresolvedActionRollbackAndNativeTMLostUOW", "Exit.");
    }

    /**
     * Test enlist for cleanup with multiple danglers and unresolved action rollback.
     * LTC Resolver = Application.
     * For this test the native TM has no knowledge of one and the resource enlistments,
     * and there is a bad return code from ATR4END. Danglers should be rolled back.
     */
    @Test
    public void testLTCMultipleDanglerCleanupWithUnresolvedActionCommitAndNativeTMLostAResourceEntryAndATR4ENDBadRc() throws Exception {
        Log.info(c, "testLTCMultipleDanglerCleanupWithUnresolvedActionCommitAndNativeTMLostAResourceEntryAndATR4ENDBadRc", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final RRSServices rrsServices = mockObjects.getRRSServices();
        final Context context = generateContext();
        final OnePhaseXAResource mockOnePhaseXAResource = mockObjects.getOnePhaseXAResource();

        // Set the restart and common LTC expectations.
        addRestartExpectations(mockery, mockObjects);
        addLTCCommonExpectations(mockery, mockObjects, context);

        // Set test specific expectations for end of LTC.
        mockery.checking(new Expectations() {
            {
                // End of LTC. cleanup will detect dangler and roll them back. RRS.endUR should be called.
                exactly(5).of(mockOnePhaseXAResource).rollback(with(any(Xid.class)));
                exactly(5).of(mockOnePhaseXAResource).getResourceName();
                oneOf(mockObjects.getContextManager()).getCurrentContext();
                will(returnValue(context));
                oneOf(rrsServices).retrieveSideInformationFast(with(context.getContextToken()), with(0));
                will(returnValue(new RetrieveSideInformationFastReturnType(RRSServices.ATR_OK, RRSServices.ATR_LOCAL_MODE_MASK)));
                oneOf(rrsServices).endUR(RRSServices.ATR_ROLLBACK_ACTION, null);
                will(returnValue(RRSServices.ATR_UNEXPECTED_ERROR));
            }
        });

        // Get the local and native transaction managers.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        ((UOWCurrent) ltcTm).setUOWEventListener(nativeTM);

        // Test.
        try {
            boolean caughtExpectedException = false;

            // Begin LTC. Explicit: Resolver = Application. Unresolved Action = Rollback.
            ltcTm.begin(false, false, false);
            LocalTransactionCoordinator ltc = ltcTm.getLocalTranCoord();
            UOWCoordinator uowc = (UOWCoordinator) ltcTm.getLocalTranCoord();

            // Enlist resources. Simulate connection obtained/used. Do not enlist 3 resource with native TM
            for (int i = 0; i < 5; i++) {
                OnePhaseXAResource natvOnePhaseXAResource = (OnePhaseXAResource) nativeTM.getOnePhaseXAResource(uowc);
                ltc.enlistForCleanup(natvOnePhaseXAResource);
                if (i != 2) {
                    nativeTM.enlist(uowc, natvOnePhaseXAResource);
                }
                ltc.enlistForCleanup(mockOnePhaseXAResource);
            }

            // LTC Ends. Dangling enlistments should be rolled back. There should be 2 failed
            // native resources: 3rd (unknown) and the last (5th - ATR4END failure).
            try {
                ltc.end(LocalTransactionCoordinator.EndModeCommit);
            } catch (InconsistentLocalTranException ilte) {
                caughtExpectedException = true;
                final String[] failedResources = ilte.getFailingResources();
                assertTrue(failedResources.length == 2);
                for (int i = 0; i < 2; i++) {
                    assertTrue(failedResources[i].equals("NativeLocalXAResource"));
                }
            }
            assertTrue(caughtExpectedException);
        } finally {
            ((UOWCurrent) ltcTm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our maps are clean and that expectations are met.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testLTCMultipleDanglerCleanupWithUnresolvedActionCommitAndNativeTMLostAResourceEntryAndATR4ENDBadRc", "Exit.");
    }

    /**
     * Test resource enlistment for cleanup in a local transaction marked for rollback.
     * LTC Resolver = Application.
     */
    @Test
    public void testLTCResourceEnlistForCleanupInLTMarkedForRollback() throws Exception {
        Log.info(c, "testLTCMultipleDanglerCleanupWithUnresolvedActionCommitAndNativeTMLostAResourceEntryAndATR4ENDBadRc", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final ContextManager contextManager = mockObjects.getContextManager();

        // Set the restart and common LTC expectations.
        addRestartExpectations(mockery, mockObjects);

        // Set test specific expectations for end of LTC.
        mockery.checking(new Expectations() {
            {
                // NatvTxManagerImpl.UOWEvent: POST_BEGIN: Check if context manager is intialized.
                oneOf(contextManager).isInitialized();
                will(returnValue(true));

                // NatvTxManagerImpl.UOWEvent: POST_BEGIN: Calls begin on context manager.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));

                // NatvTxManagerImpl.UOWEvent: POST_END: Calls begin on context manager.
                oneOf(contextManager).isInitialized();
                will(returnValue(true));
                oneOf(contextManager).end(with(any(UOWCoordinator.class)));
            }
        });

        // Get the local and native transaction managers.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        ((UOWCurrent) ltcTm).setUOWEventListener(nativeTM);

        // Test:
        try {
            boolean caughtExpectedException = false;

            // Begin LTC. Defaults: Resolver = Container At boundary. Unresolved Action = Commit.
            ltcTm.begin(false, true, false);
            LocalTransactionCoordinator ltc = ltcTm.getLocalTranCoord();
            UOWCoordinator uowc = (UOWCoordinator) ltcTm.getLocalTranCoord();
            OnePhaseXAResource natvOnePhaseXAResource = (OnePhaseXAResource) nativeTM.getOnePhaseXAResource(uowc);

            // Mark the transaction for rollback.
            ltc.setRollbackOnly();

            // Enlist native resource with TM.
            try {
                ltc.enlistForCleanup(natvOnePhaseXAResource);
            } catch (IllegalStateException ise) {
                caughtExpectedException = true;
            }
            assertTrue(caughtExpectedException);

            // Enlist native resource with native TM.
            caughtExpectedException = false;
            try {
                nativeTM.enlist(uowc, natvOnePhaseXAResource);
            } catch (IllegalStateException ise) {
                caughtExpectedException = true;
            }
            assertTrue(caughtExpectedException);

            // LTC ends(commit).
            caughtExpectedException = false;
            try {
                ltc.end(LocalTransactionCoordinator.EndModeCommit);
            } catch (RolledbackException rbe) {
                caughtExpectedException = true;
            }
            assertTrue(caughtExpectedException);
        } finally {
            ((UOWCurrent) ltcTm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our maps are clean and that expectations are met.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testLTCMultipleDanglerCleanupWithUnresolvedActionCommitAndNativeTMLostAResourceEntryAndATR4ENDBadRc", "Exit.");
    }

    /**
     * Test resource enlist and commit completion processing.
     * LTC Resolver = Container at boundary
     */
    @Test
    public void testLTCResourceEnlistAndCommit() throws Exception {
        Log.info(c, "testLTCResourceEnlistAndCommit", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final RRSServices rrsServices = mockObjects.getRRSServices();
        final Context context = generateContext();
        final OnePhaseXAResource mockOnePhaseXAResource = mockObjects.getOnePhaseXAResource();

        // Set the restart and common LTC expectations.
        addRestartExpectations(mockery, mockObjects);
        addLTCCommonExpectations(mockery, mockObjects, context);

        // Set test specific expectations for end of LTC.
        mockery.checking(new Expectations() {
            {
                // Mock XA resource action. Native TM does not know anything CONTAINER_AT_BOUNDARY transactions.
                oneOf(mockOnePhaseXAResource).start(with(any(Xid.class)), with(any(Integer.TYPE)));
                oneOf(mockOnePhaseXAResource).commit(with(any(Xid.class)), with(any(Boolean.TYPE)));
                oneOf(mockObjects.getContextManager()).getCurrentContext();
                will(returnValue(context));
                oneOf(rrsServices).retrieveSideInformationFast(with(context.getContextToken()), with(0));
                will(returnValue(new RetrieveSideInformationFastReturnType(RRSServices.ATR_OK, RRSServices.ATR_LOCAL_MODE_MASK)));
                oneOf(rrsServices).endUR(RRSServices.ATR_COMMIT_ACTION, null);
                will(returnValue(RRSServices.ATR_OK));
            }
        });

        // Get the local and native transaction managers.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        ((UOWCurrent) ltcTm).setUOWEventListener(nativeTM);

        // Test:
        try {
            // Begin LTC. Defaults: Resolver = Container At boundary. Unresolved Action = Commit.
            ltcTm.begin(false, true, true);
            LocalTransactionCoordinator ltc = ltcTm.getLocalTranCoord();
            UOWCoordinator uowc = (UOWCoordinator) ltcTm.getLocalTranCoord();
            OnePhaseXAResource natvOnePhaseXAResource = (OnePhaseXAResource) nativeTM.getOnePhaseXAResource(uowc);

            // Enlist resources. Simulate connection obtained/used.
            ltc.enlist(natvOnePhaseXAResource);
            nativeTM.enlist(uowc, natvOnePhaseXAResource);
            ltc.enlist(mockOnePhaseXAResource);

            // LTC ends(commit). No errors.
            ltc.end(LocalTransactionCoordinator.EndModeCommit);
        } finally {
            ((UOWCurrent) ltcTm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our maps are clean and that expectations are met.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testLTCResourceEnlistAndCommit", "Exit.");
    }

    /**
     * Test multiple resource enlist and rollback completion processing.
     * LTC Resolver = Container at boundary
     */
    @Test
    public void testLTCMultipleResourceEnlistAndRollback() throws Exception {
        Log.info(c, "testLTCMultipleResourceEnlistAndRollback", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final RRSServices rrsServices = mockObjects.getRRSServices();
        final Context context = generateContext();
        final OnePhaseXAResource mockOnePhaseXAResource = mockObjects.getOnePhaseXAResource();

        // Set the restart and common LTC expectations.
        addRestartExpectations(mockery, mockObjects);
        addLTCCommonExpectations(mockery, mockObjects, context);

        // Set test specific expectations for end of LTC.
        mockery.checking(new Expectations() {
            {
                // Mock XA resource action. Native TM does not know anything CONTAINER_AT_BOUNDARY transactions.
                exactly(5).of(mockOnePhaseXAResource).start(with(any(Xid.class)), with(any(Integer.TYPE)));
                exactly(5).of(mockOnePhaseXAResource).rollback(with(any(Xid.class)));
                oneOf(mockObjects.getContextManager()).getCurrentContext();
                will(returnValue(context));
                oneOf(rrsServices).retrieveSideInformationFast(with(context.getContextToken()), with(0));
                will(returnValue(new RetrieveSideInformationFastReturnType(RRSServices.ATR_OK, RRSServices.ATR_LOCAL_MODE_MASK)));
                oneOf(rrsServices).endUR(RRSServices.ATR_ROLLBACK_ACTION, null);
                will(returnValue(RRSServices.ATR_OK));
            }
        });

        // Get the local and native transaction managers.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        ((UOWCurrent) ltcTm).setUOWEventListener(nativeTM);

        // Test:
        try {
            // Begin LTC. Defaults: Resolver = Container At boundary. Unresolved Action = Rollback.
            ltcTm.begin(false, false, true);
            LocalTransactionCoordinator ltc = ltcTm.getLocalTranCoord();
            UOWCoordinator uowc = (UOWCoordinator) ltcTm.getLocalTranCoord();
            OnePhaseXAResource natvOnePhaseXAResource = (OnePhaseXAResource) nativeTM.getOnePhaseXAResource(uowc);

            // Enlist resources. Simulate connection obtained/used.
            for (int i = 0; i < 5; i++) {
                ltc.enlist(natvOnePhaseXAResource);
                nativeTM.enlist(uowc, natvOnePhaseXAResource);
                ltc.enlist(mockOnePhaseXAResource);
            }

            // LTC ends(rollback). No errors. No RRS activity.
            ltc.end(LocalTransactionCoordinator.EndModeRollBack);
        } finally {
            ((UOWCurrent) ltcTm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our maps are clean and that expectations are met.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testLTCMultipleResourceEnlistAndRollback", "Exit.");
    }

    /**
     * Test multiple resource enlist and ATR4END failure during commit completion processing.
     * LTC Resolver = Container at boundary
     */
    @Test
    public void testLTCMultipleResourceEnlistAndCommitWithBadRcFromATR4END() throws Exception {
        Log.info(c, "testLTCMultipleResourceEnlistAndCommitWithBadRcFromATR4END", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final RRSServices rrsServices = mockObjects.getRRSServices();
        final Context context = generateContext();
        final OnePhaseXAResource mockOnePhaseXAResource = mockObjects.getOnePhaseXAResource();

        // Set the restart and common LTC expectations.
        addRestartExpectations(mockery, mockObjects);
        addLTCCommonExpectations(mockery, mockObjects, context);

        // Set test specific expectations for end of LTC.
        mockery.checking(new Expectations() {
            {
                // Mock XA resource action. Native TM does not know anything CONTAINER_AT_BOUNDARY transactions.
                exactly(5).of(mockOnePhaseXAResource).start(with(any(Xid.class)), with(any(Integer.TYPE)));
                exactly(5).of(mockOnePhaseXAResource).commit(with(any(Xid.class)), with(any(Boolean.TYPE)));
                oneOf(mockObjects.getContextManager()).getCurrentContext();
                will(returnValue(context));
                oneOf(rrsServices).retrieveSideInformationFast(with(context.getContextToken()), with(0));
                will(returnValue(new RetrieveSideInformationFastReturnType(RRSServices.ATR_OK, RRSServices.ATR_LOCAL_MODE_MASK)));
                oneOf(rrsServices).endUR(RRSServices.ATR_COMMIT_ACTION, null);
                will(returnValue(RRSServices.ATR_UNEXPECTED_ERROR));
            }
        });

        // Get the local and native transaction managers.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        ((UOWCurrent) ltcTm).setUOWEventListener(nativeTM);

        // Test:
        try {
            boolean caughtExpectedException = false;

            // Begin LTC. Defaults: Resolver = Container At boundary. Unresolved Action = Rollback.
            ltcTm.begin(false, true, true);
            LocalTransactionCoordinator ltc = ltcTm.getLocalTranCoord();
            UOWCoordinator uowc = (UOWCoordinator) ltcTm.getLocalTranCoord();
            OnePhaseXAResource natvOnePhaseXAResource = (OnePhaseXAResource) nativeTM.getOnePhaseXAResource(uowc);

            // Enlist resources. Simulate connection obtained/used.
            for (int i = 0; i < 5; i++) {
                ltc.enlist(natvOnePhaseXAResource);
                nativeTM.enlist(uowc, natvOnePhaseXAResource);
                ltc.enlist(mockOnePhaseXAResource);
            }

            // LTC ends(commit). BAD RC from ATR4END.
            try {
                ltc.end(LocalTransactionCoordinator.EndModeCommit);
            } catch (InconsistentLocalTranException ilte) {
                caughtExpectedException = true;
                final String[] failedResources = ilte.getFailingResources();
                assertTrue(failedResources.length == 1);
                assertTrue(failedResources[0].equals("NativeLocalXAResource"));
            }
            assertTrue(caughtExpectedException);
        } finally {
            ((UOWCurrent) ltcTm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our maps are clean and that expectations are met.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testLTCMultipleResourceEnlistAndCommitWithBadRcFromATR4END", "Exit.");
    }

    /**
     * Test multiple resource enlist and commit after the transaction is marked for
     * rollback.
     * LTC Resolver = Container at boundary
     */
    @Test
    public void testLTCMultipleResourceEnlistAndCommitAfterTranMarkedForRollback() throws Exception {
        Log.info(c, "testLTCMultipleResourceEnlistAndCommitAfterTranMarkedForRollback", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final RRSServices rrsServices = mockObjects.getRRSServices();
        final Context context = generateContext();
        final OnePhaseXAResource mockOnePhaseXAResource = mockObjects.getOnePhaseXAResource();

        // Set the restart and common LTC expectations.
        addRestartExpectations(mockery, mockObjects);
        addLTCCommonExpectations(mockery, mockObjects, context);

        // Set test specific expectations for end of LTC.
        mockery.checking(new Expectations() {
            {
                // Mock XA resource action. Native TM does not know anything CONTAINER_AT_BOUNDARY transactions.
                exactly(5).of(mockOnePhaseXAResource).start(with(any(Xid.class)), with(any(Integer.TYPE)));
                exactly(5).of(mockOnePhaseXAResource).rollback(with(any(Xid.class)));
                oneOf(mockObjects.getContextManager()).getCurrentContext();
                will(returnValue(context));
                oneOf(rrsServices).retrieveSideInformationFast(with(context.getContextToken()), with(0));
                will(returnValue(new RetrieveSideInformationFastReturnType(RRSServices.ATR_OK, RRSServices.ATR_LOCAL_MODE_MASK)));
                oneOf(rrsServices).endUR(RRSServices.ATR_ROLLBACK_ACTION, null);
                will(returnValue(RRSServices.ATR_OK));
            }
        });

        // Get the local and native transaction managers.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        ((UOWCurrent) ltcTm).setUOWEventListener(nativeTM);

        // Test:
        try {
            boolean caughtExpectedException = false;

            // Begin LTC. Defaults: Resolver = Container At boundary. Unresolved Action = Commit.
            ltcTm.begin(false, true, true);
            LocalTransactionCoordinator ltc = ltcTm.getLocalTranCoord();
            UOWCoordinator uowc = (UOWCoordinator) ltcTm.getLocalTranCoord();
            OnePhaseXAResource natvOnePhaseXAResource = (OnePhaseXAResource) nativeTM.getOnePhaseXAResource(uowc);

            // Enlist resources. Simulate connection obtained/used.
            for (int i = 0; i < 5; i++) {
                ltc.enlist(natvOnePhaseXAResource);
                nativeTM.enlist(uowc, natvOnePhaseXAResource);
                ltc.enlist(mockOnePhaseXAResource);
            }

            // Mark the LT for rollback.
            ltc.setRollbackOnly();

            // LTC ends(commit). BAD RC from ATR4END.
            try {
                ltc.end(LocalTransactionCoordinator.EndModeCommit);
            } catch (RolledbackException rbe) {
                caughtExpectedException = true;
            }
            assertTrue(caughtExpectedException);
        } finally {
            ((UOWCurrent) ltcTm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our maps are clean and that expectations are met.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testLTCMultipleResourceEnlistAndCommitAfterTranMarkedForRollback", "Exit.");
    }

    /**
     * Test resource enlistment in a local transaction marked for rollback.
     * LTC Resolver = Container at boundary
     */
    @Test
    public void testLTCResourceEnlistInLTMarkedForRollback() throws Exception {
        Log.info(c, "testLTCResourceEnlistInLTMarkedForRollback", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);
        final ContextManager contextManager = mockObjects.getContextManager();

        // Set the restart and common LTC expectations.
        addRestartExpectations(mockery, mockObjects);

        // Set test specific expectations for end of LTC.
        mockery.checking(new Expectations() {
            {
                // NatvTxManagerImpl.UOWEvent: POST_BEGIN: Check if context manager is intialized.
                oneOf(contextManager).isInitialized();
                will(returnValue(true));

                // NatvTxManagerImpl.UOWEvent: POST_BEGIN: Calls begin on context manager.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));

                // NatvTxManagerImpl.UOWEvent: POST_END: Calls begin on context manager.
                oneOf(contextManager).isInitialized();
                will(returnValue(true));
                oneOf(contextManager).end(with(any(UOWCoordinator.class)));
            }
        });

        // Get the local and native transaction managers.
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        ((UOWCurrent) ltcTm).setUOWEventListener(nativeTM);

        // Test:
        try {
            boolean caughtExpectedException = false;

            // Begin LTC. Defaults: Resolver = Container At boundary. Unresolved Action = Commit.
            ltcTm.begin(false, true, true);
            LocalTransactionCoordinator ltc = ltcTm.getLocalTranCoord();
            UOWCoordinator uowc = (UOWCoordinator) ltcTm.getLocalTranCoord();
            OnePhaseXAResource natvOnePhaseXAResource = (OnePhaseXAResource) nativeTM.getOnePhaseXAResource(uowc);

            // Mark the transaction for rollback.
            ltc.setRollbackOnly();

            // Enlist native resource with TM.
            try {
                ltc.enlist(natvOnePhaseXAResource);
            } catch (IllegalStateException ise) {
                caughtExpectedException = true;
            }
            assertTrue(caughtExpectedException);

            // Enlist native resource with native TM.
            caughtExpectedException = false;
            try {
                nativeTM.enlist(uowc, natvOnePhaseXAResource);
            } catch (IllegalStateException ise) {
                caughtExpectedException = true;
            }
            assertTrue(caughtExpectedException);

            // LTC ends(commit).
            caughtExpectedException = false;
            try {
                ltc.end(LocalTransactionCoordinator.EndModeCommit);
            } catch (RolledbackException rbe) {
                caughtExpectedException = true;
            }
            assertTrue(caughtExpectedException);
        } finally {
            ((UOWCurrent) ltcTm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our maps are clean and that expectations are met.
        assertTrue(isTranMapEmpty(nativeTM));
        mockery.assertIsSatisfied();

        Log.info(c, "testLTCResourceEnlistInLTMarkedForRollback", "Exit.");
    }

    //========================================================================
    // Recovery-type tests
    //========================================================================
    /**
     * Start a two-phase commit, but deactivate the RRS TM after prepare is
     * called on the RRS XAResource. Verify that the XAResource throws
     * a retry exception.
     */
    @Test
    public void testDeactivateInDoubt() throws Exception {
        Log.info(c, "testDeactivateInDoubt", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.
        addRestartExpectations(mockery, mockObjects);

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();
        final byte[] uriToken = generateURIToken();
        final byte[] uriRegistryToken = generateURIRegistryToken();
        final byte[] ciRegistryToken = generateCIRegistryToken();
        final Sequence recoverySequence = mockery.sequence("recoverySequence");

        mockery.checking(new Expectations() {
            {
                final ContextManager contextManager = mockObjects.getContextManager();
                final RRSServices rrsServices = mockObjects.getRRSServices();
                final XAResource mockXares = mockObjects.getMockXAResource();

                // Tran manager is going to check to see if context manager is initialized.
                oneOf(contextManager).isInitialized();
                inSequence(recoverySequence);
                will(returnValue(true));

                // Context services will create a new context on the begin.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(recoverySequence);

                // Begin the transaction
                oneOf(rrsServices).beginTransaction(with(RRSServices.ATR_GLOBAL_MODE));
                inSequence(recoverySequence);
                will(returnValue(new BeginTransactionReturnType(0, urToken, urID)));
                oneOf(rrsServices).setWorkIdentifier(with(urToken), with(any(byte[].class)));
                inSequence(recoverySequence);
                will(returnValue(0));
                oneOf(contextManager).getCurrentContext();
                inSequence(recoverySequence);
                will(returnValue(context));

                // Enlist the mock resource
                oneOf(mockXares).start(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(recoverySequence);
                oneOf(mockXares).end(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(recoverySequence);

                // End the transaction.  Prepare RRS.
                oneOf(rrsServices).expressURInterest(with(rmRegistryToken), with(context.getContextToken()), with(RRSServices.ATR_PRESUMED_ABORT),
                                                     with(any(byte[].class)) /* nonPData */,
                                                     with(any(byte[].class)) /* pdata */, with(aNull(byte[].class)) /* xid */, with(aNull(byte[].class)) /* parentUrToken */);
                inSequence(recoverySequence);
                will(returnValue(new ExpressInterestReturnType(RRSServices.ATR_OK, uriToken, uriRegistryToken, null /* curCtxToken */, null /* urid */, null /* nonPData */, null /*
                                                                                                                                                                                   * diagArea
                                                                                                                                                                                   */, RRSServices.ATR_GLOBAL_MODE, null /*
                                                                                                                                                                                                                          * urToken
                                                                                                                                                                                                                          */)));
                oneOf(rrsServices).setSyncpointControls(with(uriRegistryToken), with(RRSServices.ATR_PREPARE_OK), with(RRSServices.ATR_COMMIT_OK),
                                                        with(RRSServices.ATR_BACKOUT_OK),
                                                        with(RRSServices.ATR_SDSRM));
                inSequence(recoverySequence);
                will(returnValue(RRSServices.ATR_OK));
                oneOf(rrsServices).prepareAgentUR(with(uriRegistryToken), with(context.getContextRegistryToken()), with(rmRegistryToken), with(RRSServices.ATR_DEFER_IMPLICIT));
                inSequence(recoverySequence);
                will(returnValue(new PrepareAgentURReturnType(RRSServices.ATR_OK, RRSServices.CTX_OK, ciRegistryToken)));

                // Deactivate the transaction manager.
                oneOf(contextManager).destroyContextManager(with(0L));
                inSequence(recoverySequence);
                oneOf(rrsServices).unregisterResourceManager(with(rmNameRegistryToken), with(rmRegistryToken));
                inSequence(recoverySequence);
                will(returnValue(RRSServices.CRG_OK));

                // Finish off the mock XAResource.
                atMost(1).of(mockXares).prepare(with(any(Xid.class)));
                inSequence(recoverySequence);
                will(returnValue(XAResource.XA_OK));
                atMost(1).of(mockXares).commit(with(any(Xid.class)), with(any(Boolean.TYPE)));
                inSequence(recoverySequence);
            }
        });

        // Create and initialize the transaction managers, and XAResource.
        long millisToWaitShutdown = 500L;
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Map<String, Object> nativeTMProps = new HashMap<String, Object>();
        nativeTMProps.put(NativeTransactionManager.SHUTDOWN_TIMEOUT_PROPERTY, new Long(millisToWaitShutdown));
        nativeTM.updateConfig(nativeTMProps); // Tell TM to only wait 1/2 second before shutting down.
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        DeactivateInDoubtXAResourceWrapper xares = new DeactivateInDoubtXAResourceWrapper(nativeTM.getXAResource(info), nativeTM, (UOWCurrent) tm);
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        int mockRecoveryId = tm.registerResourceInfo(XARES_FACTORY_MOCK, new String("testTwoPhaseCommit"), XARES_LOW_PRIORITY);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.enlist(mockObjects.getMockXAResource(), mockRecoveryId);
            tm.commit();
        } finally {
            ((UOWCurrent) tm).unsetUOWEventListener(nativeTM);
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(xares.testPassed == true);
        assertTrue(isTranMapEmpty(nativeTM) == false); // Tran will not recover.
        mockery.assertIsSatisfied();

        Log.info(c, "testDeactivateInDoubt", "Exit.");
    }

    /** XAResource used to wrap RRS XAResource for testDeactivateInDoubt test. */
    public class DeactivateInDoubtXAResourceWrapper implements XAResource {
        private final XAResource xares;
        private final NativeTransactionManager nativeTM;
        private final UOWCurrent uowCurrent;
        private boolean commitAttempted = false;
        private boolean testPassed = false;

        DeactivateInDoubtXAResourceWrapper(XAResource xares, NativeTransactionManager nativeTM, UOWCurrent uowCurrent) {
            this.xares = xares;
            this.nativeTM = nativeTM;
            this.uowCurrent = uowCurrent;
        }

        /** {@inheritDoc} */
        @Override
        public void commit(Xid xid, boolean onePhase) throws XAException {
            if (commitAttempted == false) {
                commitAttempted = true;
                try {
                    xares.commit(xid, onePhase);
                } catch (XAException xae) {
                    if ((xae.errorCode == XAException.XA_RETRY) && (xae.getCause() instanceof IllegalStateException)) {
                        testPassed = true;
                    }
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public void end(Xid xid, int flags) throws XAException {
            xares.end(xid, flags);
        }

        /** {@inheritDoc} */
        @Override
        public void forget(Xid xid) throws XAException {
            if (testPassed == false) {
                throw new XAException("Should not be called for forget");
            }
        }

        /** {@inheritDoc} */
        @Override
        public int getTransactionTimeout() throws XAException {
            return xares.getTransactionTimeout();
        }

        /** {@inheritDoc} */
        @Override
        public boolean isSameRM(XAResource theXAResource) throws XAException {
            return xares.isSameRM(theXAResource);
        }

        /** {@inheritDoc} */
        @Override
        public int prepare(Xid xid) throws XAException {
            int prepareRC = xares.prepare(xid);
            // Deactivate the TM
            uowCurrent.unsetUOWEventListener(nativeTM);
            nativeTM.deactivate();
            return prepareRC;
        }

        /** {@inheritDoc} */
        @Override
        public Xid[] recover(int flag) throws XAException {
            return xares.recover(flag);
        }

        /** {@inheritDoc} */
        @Override
        public void rollback(Xid xid) throws XAException {
            throw new XAException("Should not be called for rollback");
        }

        /** {@inheritDoc} */
        @Override
        public boolean setTransactionTimeout(int seconds) throws XAException {
            return xares.setTransactionTimeout(seconds);
        }

        /** {@inheritDoc} */
        @Override
        public void start(Xid xid, int flags) throws XAException {
            xares.start(xid, flags);
        }
    }

    /**
     * Start a two-phase commit, but restart the embedded TM after both
     * resources are prepared (when the first one is called for commit). Allow
     * the transaction to go through recovery.
     */
//    @Test(timeout = 90000)
    public void testRecoveryCommit() throws Exception {
        Log.info(c, "testRecoveryCommit", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.  This is the initial
        // restart which should have no work to do.
        addRestartExpectations(mockery, mockObjects);

        // Make some IDs that we'll use to identify the transaction that we're
        // going to start.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();
        final byte[] uriToken = generateURIToken();
        final byte[] uriRegistryToken = generateURIRegistryToken();
        final byte[] ciRegistryToken = generateCIRegistryToken();
        final Sequence recoverySequence = mockery.sequence("recoverySequence");

        // Adding the expectations for the first part of the test -- up to the
        // point where we prepare the transaction.
        mockery.checking(new Expectations() {
            {
                final ContextManager contextManager = mockObjects.getContextManager();
                final RRSServices rrsServices = mockObjects.getRRSServices();
                final XAResource mockXares = mockObjects.getMockXAResource();

                // Tran manager is going to check to see if context manager is initialized.
                oneOf(contextManager).isInitialized();
                inSequence(recoverySequence);
                will(returnValue(true));

                // Context services will create a new context on the begin.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(recoverySequence);

                // Begin the transaction
                oneOf(rrsServices).beginTransaction(with(RRSServices.ATR_GLOBAL_MODE));
                inSequence(recoverySequence);
                will(returnValue(new BeginTransactionReturnType(0, urToken, urID)));
                oneOf(rrsServices).setWorkIdentifier(with(urToken), with(any(byte[].class)));
                inSequence(recoverySequence);
                will(returnValue(0));
                oneOf(contextManager).getCurrentContext();
                inSequence(recoverySequence);
                will(returnValue(context));

                // Enlist the mock resource
                oneOf(mockXares).start(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(recoverySequence);
                oneOf(mockXares).end(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(recoverySequence);

                // End the transaction.  Prepare RRS.
                oneOf(rrsServices).expressURInterest(with(rmRegistryToken), with(context.getContextToken()), with(RRSServices.ATR_PRESUMED_ABORT),
                                                     with(any(byte[].class)) /* nonPData */,
                                                     with(any(byte[].class)) /* pdata */, with(aNull(byte[].class)) /* xid */, with(aNull(byte[].class)) /* parentUrToken */);
                inSequence(recoverySequence);
                will(returnValue(new ExpressInterestReturnType(RRSServices.ATR_OK, uriToken, uriRegistryToken, null /* curCtxToken */, null /* urid */, null /* nonPData */, null /*
                                                                                                                                                                                   * diagArea
                                                                                                                                                                                   */, RRSServices.ATR_GLOBAL_MODE, null /*
                                                                                                                                                                                                                          * urToken
                                                                                                                                                                                                                          */)));
                oneOf(rrsServices).setSyncpointControls(with(uriRegistryToken), with(RRSServices.ATR_PREPARE_OK), with(RRSServices.ATR_COMMIT_OK),
                                                        with(RRSServices.ATR_BACKOUT_OK),
                                                        with(RRSServices.ATR_SDSRM));
                inSequence(recoverySequence);
                will(returnValue(RRSServices.ATR_OK));
                oneOf(rrsServices).prepareAgentUR(with(uriRegistryToken), with(context.getContextRegistryToken()), with(rmRegistryToken), with(RRSServices.ATR_DEFER_IMPLICIT));
                inSequence(recoverySequence);
                will(returnValue(new PrepareAgentURReturnType(RRSServices.ATR_OK, RRSServices.CTX_OK, ciRegistryToken)));

                // Prepare the mock XAResource.  We need to return OK to prevent
                // the tran from doing a one-phase optimization on the commit,
                // which causes the TM to skip logging the first XAResource.
                atMost(1).of(mockXares).prepare(with(any(Xid.class)));
                inSequence(recoverySequence);
                will(returnValue(XAResource.XA_OK));
                atMost(1).of(mockXares).commit(with(any(Xid.class)), with(any(Boolean.TYPE)));
                inSequence(recoverySequence);

                // At this point the TM will be restarted and we'll log the
                // expected expectations at that point.
            }
        });

        // Create and initialize the transaction managers, and XAResource.
        long millisToWaitShutdown = 500L;
        NativeTransactionManager newNativeTM = null;
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Map<String, Object> nativeTMProps = new HashMap<String, Object>();
        nativeTMProps.put(NativeTransactionManager.SHUTDOWN_TIMEOUT_PROPERTY, new Long(millisToWaitShutdown));
        nativeTM.updateConfig(nativeTMProps); // Tell TM to only wait 1/2 second before shutting down.
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
//        TxBundleTools txBundleTools = new TxBundleTools();
//        txBundleTools.start(mockObjects.getTxMockBundleContext());
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        RestartTMInDoubtXAResourceWrapper xares = new RestartTMInDoubtXAResourceWrapper(this, nativeTM.getXAResource(info), nativeTM, (UOWCurrent) tm, mockery, mockObjects, urID, true /*
                                                                                                                                                                                         * Commit
                                                                                                                                                                                         */, false /*
                                                                                                                                                                                                    * Not
                                                                                                                                                                                                    * heuristic
                                                                                                                                                                                                    */, false /*
                                                                                                                                                                                                               * No
                                                                                                                                                                                                               * extra
                                                                                                                                                                                                               * trans
                                                                                                                                                                                                               * for
                                                                                                                                                                                                               * recovery
                                                                                                                                                                                                               */);
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        int mockRecoveryId = tm.registerResourceInfo(XARES_FACTORY_MOCK, new String("testTwoPhaseCommit"), XARES_LOW_PRIORITY);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.enlist(mockObjects.getMockXAResource(), mockRecoveryId);
            tm.commit();
        } finally {
            newNativeTM = xares.getRecoveryNativeTM();
            if ((newNativeTM != null) && (newNativeTM.isActive() == true)) {
                ((UOWCurrent) tm).unsetUOWEventListener(newNativeTM);
            }
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(newNativeTM != null);
        assertTrue(newNativeTM.isActive());
        assertTrue(xares.testPassed == true);
        assertTrue(isTranMapEmpty(nativeTM) == false); // Tran will not recover.
        assertTrue(isTranMapEmpty(newNativeTM) == true); // Tran will recover here.
        mockery.assertIsSatisfied();

        // Let the tran manager shut down cleanly.
        try {
            TMHelper.shutdown(1);
//            txBundleTools.stop(mockObjects.getTxMockBundleContext());
        } finally {
            startTM(10, true);
        }

        Log.info(c, "testRecoveryCommit", "Exit.");
    }

    /**
     * Start a two-phase commit, but after RRS is prepared, another XAResource
     * will vote backout. Restart the embedded TM while the RRS resource is
     * being called for backout and allow the XAResource to recover and backout.
     */
//    @Test(timeout = 90000)
    public void testRecoveryBackout() throws Exception {
        Log.info(c, "testRecoveryBackout", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.  This is the initial
        // restart which should have no work to do.
        addRestartExpectations(mockery, mockObjects);

        // Make some IDs that we'll use to identify the transaction that we're
        // going to start.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();
        final byte[] uriToken = generateURIToken();
        final byte[] uriRegistryToken = generateURIRegistryToken();
        final byte[] ciRegistryToken = generateCIRegistryToken();
        final Sequence recoverySequence = mockery.sequence("recoverySequence");

        // Adding the expectations for the first part of the test -- up to the
        // point where we prepare the transaction.
        mockery.checking(new Expectations() {
            {
                final ContextManager contextManager = mockObjects.getContextManager();
                final RRSServices rrsServices = mockObjects.getRRSServices();
                final XAResource mockXares = mockObjects.getMockXAResource();

                // Tran manager is going to check to see if context manager is initialized.
                oneOf(contextManager).isInitialized();
                inSequence(recoverySequence);
                will(returnValue(true));

                // Context services will create a new context on the begin.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(recoverySequence);

                // Begin the transaction
                oneOf(rrsServices).beginTransaction(with(RRSServices.ATR_GLOBAL_MODE));
                inSequence(recoverySequence);
                will(returnValue(new BeginTransactionReturnType(0, urToken, urID)));
                oneOf(rrsServices).setWorkIdentifier(with(urToken), with(any(byte[].class)));
                inSequence(recoverySequence);
                will(returnValue(0));
                oneOf(contextManager).getCurrentContext();
                inSequence(recoverySequence);
                will(returnValue(context));

                // Enlist the mock resource
                oneOf(mockXares).start(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(recoverySequence);
                oneOf(mockXares).end(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(recoverySequence);

                // End the transaction.  Prepare RRS.
                oneOf(rrsServices).expressURInterest(with(rmRegistryToken), with(context.getContextToken()), with(RRSServices.ATR_PRESUMED_ABORT),
                                                     with(any(byte[].class)) /* nonPData */,
                                                     with(any(byte[].class)) /* pdata */, with(aNull(byte[].class)) /* xid */, with(aNull(byte[].class)) /* parentUrToken */);
                inSequence(recoverySequence);
                will(returnValue(new ExpressInterestReturnType(RRSServices.ATR_OK, uriToken, uriRegistryToken, null /* curCtxToken */, null /* urid */, null /* nonPData */, null /*
                                                                                                                                                                                   * diagArea
                                                                                                                                                                                   */, RRSServices.ATR_GLOBAL_MODE, null /*
                                                                                                                                                                                                                          * urToken
                                                                                                                                                                                                                          */)));
                oneOf(rrsServices).setSyncpointControls(with(uriRegistryToken), with(RRSServices.ATR_PREPARE_OK), with(RRSServices.ATR_COMMIT_OK),
                                                        with(RRSServices.ATR_BACKOUT_OK),
                                                        with(RRSServices.ATR_SDSRM));
                inSequence(recoverySequence);
                will(returnValue(RRSServices.ATR_OK));
                oneOf(rrsServices).prepareAgentUR(with(uriRegistryToken), with(context.getContextRegistryToken()), with(rmRegistryToken), with(RRSServices.ATR_DEFER_IMPLICIT));
                inSequence(recoverySequence);
                will(returnValue(new PrepareAgentURReturnType(RRSServices.ATR_OK, RRSServices.CTX_OK, ciRegistryToken)));

                // Prepare the mock XAResource.  We will return backout to force
                // a backout.
                atMost(1).of(mockXares).prepare(with(any(Xid.class)));
                inSequence(recoverySequence);
                will(throwException(new XAException(XAException.XA_RBROLLBACK)));
                atMost(1).of(mockXares).commit(with(any(Xid.class)), with(any(Boolean.TYPE)));
                inSequence(recoverySequence);
                will(throwException(new XAException(XAException.XA_RBROLLBACK)));

                // At this point the TM will be restarted and we'll log the
                // expected expectations at that point.
            }
        });

        // Create and initialize the transaction managers, and XAResource.
        long millisToWaitShutdown = 500L;
        NativeTransactionManager newNativeTM = null;
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Map<String, Object> nativeTMProps = new HashMap<String, Object>();
        nativeTMProps.put(NativeTransactionManager.SHUTDOWN_TIMEOUT_PROPERTY, new Long(millisToWaitShutdown));
        nativeTM.updateConfig(nativeTMProps); // Tell TM to only wait 1/2 second before shutting down.
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
//        TxBundleTools txBundleTools = new TxBundleTools();
//        txBundleTools.start(mockObjects.getTxMockBundleContext());
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();

        RestartTMInDoubtXAResourceWrapper xares = new RestartTMInDoubtXAResourceWrapper(this, nativeTM.getXAResource(info), nativeTM, (UOWCurrent) tm, mockery, mockObjects, urID, false /*
                                                                                                                                                                                          * Backout
                                                                                                                                                                                          */, false /*
                                                                                                                                                                                                     * Not
                                                                                                                                                                                                     * heuristic
                                                                                                                                                                                                     */, false /*
                                                                                                                                                                                                                * No
                                                                                                                                                                                                                * extra
                                                                                                                                                                                                                * trans
                                                                                                                                                                                                                * for
                                                                                                                                                                                                                * recovery
                                                                                                                                                                                                                */);
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        int mockRecoveryId = tm.registerResourceInfo(XARES_FACTORY_MOCK, new String("testTwoPhaseCommit"), XARES_LOW_PRIORITY);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        boolean caughtException = false;
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.enlist(mockObjects.getMockXAResource(), mockRecoveryId);
            try {
                tm.commit();
            } catch (RollbackException rbe) {
                caughtException = true;
            }
        } finally {
            newNativeTM = xares.getRecoveryNativeTM();
            if ((newNativeTM != null) && (newNativeTM.isActive() == true)) {
                ((UOWCurrent) tm).unsetUOWEventListener(newNativeTM);
            }
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(caughtException);
        assertTrue(newNativeTM != null);
        assertTrue(newNativeTM.isActive());
        assertTrue(xares.testPassed == true);
        assertTrue(isTranMapEmpty(nativeTM) == false); // Tran will not recover.
        assertTrue(isTranMapEmpty(newNativeTM) == true); // Tran will recover here.
        mockery.assertIsSatisfied();

        // Let the tran manager shut down cleanly.
        try {
            TMHelper.shutdown(1);
//            txBundleTools.stop(mockObjects.getTxMockBundleContext());
        } finally {
            startTM(10, true);
        }

        Log.info(c, "testRecoveryBackout", "Exit.");
    }

    /**
     * Start a two-phase commit, but restart the embedded TM after both
     * resources are prepared (when the first one is called for commit). Allow
     * the transaction to go through recovery. The transaction will report a
     * heuristic.
     */
//    @Test(timeout = 90000)
    public void testRecoveryCommitHeuristic() throws Exception {
        Log.info(c, "testRecoveryCommitHeuristic", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.  This is the initial
        // restart which should have no work to do.
        addRestartExpectations(mockery, mockObjects);

        // Make some IDs that we'll use to identify the transaction that we're
        // going to start.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();
        final byte[] uriToken = generateURIToken();
        final byte[] uriRegistryToken = generateURIRegistryToken();
        final byte[] ciRegistryToken = generateCIRegistryToken();
        final Sequence recoverySequence = mockery.sequence("recoverySequence");

        // Adding the expectations for the first part of the test -- up to the
        // point where we prepare the transaction.
        mockery.checking(new Expectations() {
            {
                final ContextManager contextManager = mockObjects.getContextManager();
                final RRSServices rrsServices = mockObjects.getRRSServices();
                final XAResource mockXares = mockObjects.getMockXAResource();

                // Tran manager is going to check to see if context manager is initialized.
                oneOf(contextManager).isInitialized();
                inSequence(recoverySequence);
                will(returnValue(true));

                // Context services will create a new context on the begin.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(recoverySequence);

                // Begin the transaction
                oneOf(rrsServices).beginTransaction(with(RRSServices.ATR_GLOBAL_MODE));
                inSequence(recoverySequence);
                will(returnValue(new BeginTransactionReturnType(0, urToken, urID)));
                oneOf(rrsServices).setWorkIdentifier(with(urToken), with(any(byte[].class)));
                inSequence(recoverySequence);
                will(returnValue(0));
                oneOf(contextManager).getCurrentContext();
                inSequence(recoverySequence);
                will(returnValue(context));

                // Enlist the mock resource
                oneOf(mockXares).start(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(recoverySequence);
                oneOf(mockXares).end(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(recoverySequence);

                // End the transaction.  Prepare RRS.
                oneOf(rrsServices).expressURInterest(with(rmRegistryToken), with(context.getContextToken()), with(RRSServices.ATR_PRESUMED_ABORT),
                                                     with(any(byte[].class)) /* nonPData */,
                                                     with(any(byte[].class)) /* pdata */, with(aNull(byte[].class)) /* xid */, with(aNull(byte[].class)) /* parentUrToken */);
                inSequence(recoverySequence);
                will(returnValue(new ExpressInterestReturnType(RRSServices.ATR_OK, uriToken, uriRegistryToken, null /* curCtxToken */, null /* urid */, null /* nonPData */, null /*
                                                                                                                                                                                   * diagArea
                                                                                                                                                                                   */, RRSServices.ATR_GLOBAL_MODE, null /*
                                                                                                                                                                                                                          * urToken
                                                                                                                                                                                                                          */)));
                oneOf(rrsServices).setSyncpointControls(with(uriRegistryToken), with(RRSServices.ATR_PREPARE_OK), with(RRSServices.ATR_COMMIT_OK),
                                                        with(RRSServices.ATR_BACKOUT_OK),
                                                        with(RRSServices.ATR_SDSRM));
                inSequence(recoverySequence);
                will(returnValue(RRSServices.ATR_OK));
                oneOf(rrsServices).prepareAgentUR(with(uriRegistryToken), with(context.getContextRegistryToken()), with(rmRegistryToken), with(RRSServices.ATR_DEFER_IMPLICIT));
                inSequence(recoverySequence);
                will(returnValue(new PrepareAgentURReturnType(RRSServices.ATR_OK, RRSServices.CTX_OK, ciRegistryToken)));

                // Prepare the mock XAResource.  We need to return OK to prevent
                // the tran from doing a one-phase optimization on the commit,
                // which causes the TM to skip logging the first XAResource.
                atMost(1).of(mockXares).prepare(with(any(Xid.class)));
                inSequence(recoverySequence);
                will(returnValue(XAResource.XA_OK));
                atMost(1).of(mockXares).commit(with(any(Xid.class)), with(any(Boolean.TYPE)));
                inSequence(recoverySequence);

                // At this point the TM will be restarted and we'll log the
                // expected expectations at that point.
            }
        });

        // Create and initialize the transaction managers, and XAResource.
        long millisToWaitShutdown = 500L;
        NativeTransactionManager newNativeTM = null;
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Map<String, Object> nativeTMProps = new HashMap<String, Object>();
        nativeTMProps.put(NativeTransactionManager.SHUTDOWN_TIMEOUT_PROPERTY, new Long(millisToWaitShutdown));
        nativeTM.updateConfig(nativeTMProps); // Tell TM to only wait 1/2 second before shutting down.
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
//        TxBundleTools txBundleTools = new TxBundleTools();
//        txBundleTools.start(mockObjects.getTxMockBundleContext());
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        RestartTMInDoubtXAResourceWrapper xares = new RestartTMInDoubtXAResourceWrapper(this, nativeTM.getXAResource(info), nativeTM, (UOWCurrent) tm, mockery, mockObjects, urID, true /*
                                                                                                                                                                                         * Commit
                                                                                                                                                                                         */, true /*
                                                                                                                                                                                                   * Heuristic
                                                                                                                                                                                                   */, false /*
                                                                                                                                                                                                              * No
                                                                                                                                                                                                              * extra
                                                                                                                                                                                                              * trans
                                                                                                                                                                                                              * for
                                                                                                                                                                                                              * recovery.
                                                                                                                                                                                                              */);
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        int mockRecoveryId = tm.registerResourceInfo(XARES_FACTORY_MOCK, new String("testTwoPhaseCommit"), XARES_LOW_PRIORITY);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.enlist(mockObjects.getMockXAResource(), mockRecoveryId);
            tm.commit(); // No heuristic thrown because that happens during restart/recovery.
        } finally {
            newNativeTM = xares.getRecoveryNativeTM();
            if ((newNativeTM != null) && (newNativeTM.isActive() == true)) {
                ((UOWCurrent) tm).unsetUOWEventListener(newNativeTM);
            }
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(newNativeTM != null);
        assertTrue(newNativeTM.isActive());
        assertTrue(xares.testPassed == true);
        assertTrue(isTranMapEmpty(nativeTM) == false); // Tran will not recover.
        assertTrue(isTranMapEmpty(newNativeTM) == true); // Tran will recover here.
        mockery.assertIsSatisfied();

        // Let the tran manager shut down cleanly.
        try {
            TMHelper.shutdown(1);
//            txBundleTools.stop(mockObjects.getTxMockBundleContext());
        } finally {
            startTM(10, true);
        }

        Log.info(c, "testRecoveryCommitHeuristic", "Exit.");
    }

    /**
     * Start a two-phase commit, but restart the embedded TM after both
     * resources are prepared (when the first one is called for commit). Allow
     * the transaction to go through recovery. Inject extra XIDs during RRS
     * recovery and make sure the native TM responds to complete them.
     */
//    @Test(timeout = 90000)
    public void testRecoveryCommitAndBackoutExtras() throws Exception {
        Log.info(c, "testRecoveryCommitAndBackoutExtras", "Entry.");

        // Mock environment for native methods.
        final Mockery mockery = new JUnit4Mockery();
        final MockObjectSet mockObjects = new MockObjectSet(mockery);

        // Add the restart sequence to the mockery object.  This is the initial
        // restart which should have no work to do.
        addRestartExpectations(mockery, mockObjects);

        // Make some IDs that we'll use to identify the transaction that we're
        // going to start.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();
        final byte[] uriToken = generateURIToken();
        final byte[] uriRegistryToken = generateURIRegistryToken();
        final byte[] ciRegistryToken = generateCIRegistryToken();
        final Sequence recoverySequence = mockery.sequence("recoverySequence");

        // Adding the expectations for the first part of the test -- up to the
        // point where we prepare the transaction.
        mockery.checking(new Expectations() {
            {
                final ContextManager contextManager = mockObjects.getContextManager();
                final RRSServices rrsServices = mockObjects.getRRSServices();
                final XAResource mockXares = mockObjects.getMockXAResource();

                // Tran manager is going to check to see if context manager is initialized.
                oneOf(contextManager).isInitialized();
                inSequence(recoverySequence);
                will(returnValue(true));

                // Context services will create a new context on the begin.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(recoverySequence);

                // Begin the transaction
                oneOf(rrsServices).beginTransaction(with(RRSServices.ATR_GLOBAL_MODE));
                inSequence(recoverySequence);
                will(returnValue(new BeginTransactionReturnType(0, urToken, urID)));
                oneOf(rrsServices).setWorkIdentifier(with(urToken), with(any(byte[].class)));
                inSequence(recoverySequence);
                will(returnValue(0));
                oneOf(contextManager).getCurrentContext();
                inSequence(recoverySequence);
                will(returnValue(context));

                // Enlist the mock resource
                oneOf(mockXares).start(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(recoverySequence);
                oneOf(mockXares).end(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(recoverySequence);

                // End the transaction.  Prepare RRS.
                oneOf(rrsServices).expressURInterest(with(rmRegistryToken), with(context.getContextToken()), with(RRSServices.ATR_PRESUMED_ABORT),
                                                     with(any(byte[].class)) /* nonPData */,
                                                     with(any(byte[].class)) /* pdata */, with(aNull(byte[].class)) /* xid */, with(aNull(byte[].class)) /* parentUrToken */);
                inSequence(recoverySequence);
                will(returnValue(new ExpressInterestReturnType(RRSServices.ATR_OK, uriToken, uriRegistryToken, null /* curCtxToken */, null /* urid */, null /* nonPData */, null /*
                                                                                                                                                                                   * diagArea
                                                                                                                                                                                   */, RRSServices.ATR_GLOBAL_MODE, null /*
                                                                                                                                                                                                                          * urToken
                                                                                                                                                                                                                          */)));
                oneOf(rrsServices).setSyncpointControls(with(uriRegistryToken), with(RRSServices.ATR_PREPARE_OK), with(RRSServices.ATR_COMMIT_OK),
                                                        with(RRSServices.ATR_BACKOUT_OK),
                                                        with(RRSServices.ATR_SDSRM));
                inSequence(recoverySequence);
                will(returnValue(RRSServices.ATR_OK));
                oneOf(rrsServices).prepareAgentUR(with(uriRegistryToken), with(context.getContextRegistryToken()), with(rmRegistryToken), with(RRSServices.ATR_DEFER_IMPLICIT));
                inSequence(recoverySequence);
                will(returnValue(new PrepareAgentURReturnType(RRSServices.ATR_OK, RRSServices.CTX_OK, ciRegistryToken)));

                // Prepare the mock XAResource.  We need to return OK to prevent
                // the tran from doing a one-phase optimization on the commit,
                // which causes the TM to skip logging the first XAResource.
                atMost(1).of(mockXares).prepare(with(any(Xid.class)));
                inSequence(recoverySequence);
                will(returnValue(XAResource.XA_OK));
                atMost(1).of(mockXares).commit(with(any(Xid.class)), with(any(Boolean.TYPE)));
                inSequence(recoverySequence);

                // At this point the TM will be restarted and we'll log the
                // expected expectations at that point.
            }
        });

        // Create and initialize the transaction managers, and XAResource.
        long millisToWaitShutdown = 500L;
        NativeTransactionManager newNativeTM = null;
        NativeTransactionManager nativeTM = createNativeTranMgr(mockObjects);
        Map<String, Object> nativeTMProps = new HashMap<String, Object>();
        nativeTMProps.put(NativeTransactionManager.SHUTDOWN_TIMEOUT_PROPERTY, new Long(millisToWaitShutdown));
        nativeTM.updateConfig(nativeTMProps); // Tell TM to only wait 1/2 second before shutting down.
        Serializable info = nativeTM.getXAResourceInfo((Xid) null);
//        TxBundleTools txBundleTools = new TxBundleTools();
//        txBundleTools.start(mockObjects.getTxMockBundleContext());
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        RestartTMInDoubtXAResourceWrapper xares = new RestartTMInDoubtXAResourceWrapper(this, nativeTM.getXAResource(info), nativeTM, (UOWCurrent) tm, mockery, mockObjects, urID, true /*
                                                                                                                                                                                         * Commit
                                                                                                                                                                                         */, false /*
                                                                                                                                                                                                    * Not
                                                                                                                                                                                                    * heuristic
                                                                                                                                                                                                    */, true /*
                                                                                                                                                                                                              * Inject
                                                                                                                                                                                                              * extra
                                                                                                                                                                                                              * trans
                                                                                                                                                                                                              * for
                                                                                                                                                                                                              * recovery
                                                                                                                                                                                                              */);
        int recoveryId = tm.registerResourceInfo(XARES_FACTORY_RRS, info);
        int mockRecoveryId = tm.registerResourceInfo(XARES_FACTORY_MOCK, new String("testTwoPhaseCommit"), XARES_LOW_PRIORITY);
        ((UOWCurrent) tm).setUOWEventListener(nativeTM);
        nativeTM.setRMName(rmName);

        // Run the test.
        try {
            tm.begin();
            tm.enlist(xares, recoveryId);
            tm.enlist(mockObjects.getMockXAResource(), mockRecoveryId);
            tm.commit();
        } finally {
            newNativeTM = xares.getRecoveryNativeTM();
            if ((newNativeTM != null) && (newNativeTM.isActive() == true)) {
                ((UOWCurrent) tm).unsetUOWEventListener(newNativeTM);
            }
        }

        // Make sure our mock objects were driven as expected.
        assertTrue(newNativeTM != null);
        assertTrue(newNativeTM.isActive());
        assertTrue(xares.testPassed == true);
        assertTrue(isTranMapEmpty(nativeTM) == false); // Tran will not recover.
        assertTrue(isTranMapEmpty(newNativeTM) == true); // Tran will recover here.
        mockery.assertIsSatisfied();

        // Let the tran manager shut down cleanly.
        try {
            TMHelper.shutdown(1);
//            txBundleTools.stop(mockObjects.getTxMockBundleContext());
        } finally {
            startTM(10, true);
        }

        Log.info(c, "testRecoveryCommitAndBackoutExtras", "Exit.");
    }

    /** XAResource used to wrap RRS XAResource for testRecoveryCommit and testRecoveryBackout tests. */
    public static class RestartTMInDoubtXAResourceWrapper implements XAResource {
        private final NativeTransactionManagerTest test; // The test case
        private static Xid xid = null; // The tran that is being processed.
        private final XAResource xares; // The XAResource that we're wrapping.
        private final NativeTransactionManager nativeTM; // The initial native TM instance.
        private NativeTransactionManager newNativeTM; // The native TM created to do recovery;
        private final Mockery mockery; // The JMock environment.
        private final MockObjectSet mockObjects; // The initial set of mock objects.
        private final UOWCurrent uowCurrent; // The object we use to register event listeners with the base TM.
        private final byte[] urid; // The URID for the transaction which we'll recover.
        private boolean startedTran = false;
        private boolean restartedTM = false;
        private boolean testPassed = false;
        private boolean expectCommit = true; // Should we be expecting commit or backout.
        private final boolean heuristicOnRestart; // Should the tran be heuristic on restart.
        private final boolean addExtraRollbackTrans; // Should we add some extra transactions on recovery.

        RestartTMInDoubtXAResourceWrapper(NativeTransactionManagerTest test, XAResource xares, NativeTransactionManager nativeTM, UOWCurrent uowCurrent, Mockery mockery,
                                          MockObjectSet mockObjects, byte[] urid, boolean expectCommit, boolean heuristicOnRestart, boolean addExtraRollbackTrans) {
            this.test = test;
            RestartTMInDoubtXAResourceWrapper.xid = null;
            this.xares = xares;
            this.nativeTM = nativeTM;
            this.newNativeTM = null;
            this.mockery = mockery;
            this.mockObjects = mockObjects;
            this.urid = urid;
            this.uowCurrent = uowCurrent;
            this.expectCommit = expectCommit; // Set to true if commit, false if backout.
            this.heuristicOnRestart = heuristicOnRestart;
            this.addExtraRollbackTrans = addExtraRollbackTrans;
        }

        /** {@inheritDoc} */
        @Override
        public void commit(Xid xid, boolean onePhase) throws XAException {
            if (expectCommit == true) {
                processOutcome(xid, true);
            } else {
                throw new XAException("XAResource was driven for incorrect outcome");
            }
        }

        @SuppressWarnings("unchecked")
        public void processOutcome(Xid xid, final boolean commit) throws XAException {
            // The mainline resource will have started the transaction.
            if (startedTran) {
                if (restartedTM == false) {
                    restartedTM = true;

                    // Restart the TM and friends.
                    uowCurrent.unsetUOWEventListener(nativeTM); // Need to unregister listener before base TM stops.
                    try {
                        TMHelper.shutdown(1); // Shut down the base transaction manager.

                        // Next we unregister the native TM.  This will cause the
                        // context manager to be destroyed and unregister with RRS.
                        final Sequence seq = mockery.sequence("restartRecoverySequence");
                        mockery.checking(new Expectations() {
                            {
                                ContextManager cm = mockObjects.getContextManager();
                                RRSServices rrs = mockObjects.getRRSServices();
                                oneOf(cm).destroyContextManager(with(any(Long.TYPE)));
                                inSequence(seq);
                                oneOf(rrs).unregisterResourceManager(with(any(byte[].class)), with(any(byte[].class)));
                                inSequence(seq);
                                will(returnValue(RRSServices.CRG_OK));
                            }
                        });

                        nativeTM.deactivate(); // Shut down the native TM.

                        // Create the IDs that we'll supply to the native TM
                        // during restart.  Some like the URID and XID are the
                        // same as before, but the tokens are all new.
                        byte[] xidBytes = ((XidImpl) xid).toBytes();
                        final Context context = test.generateContext();
                        final byte[] urToken = test.generateURToken();
                        final byte[] uriToken = test.generateURIToken();
                        final byte[] uriRegistryToken = test.generateURIRegistryToken();
                        int tranState = (heuristicOnRestart) ? (expectCommit ? RRSServices.ATR_IN_COMMIT : RRSServices.ATR_IN_BACKOUT) : RRSServices.ATR_IN_DOUBT;
                        RRSTranRecoveryData recData = new RRSTranRecoveryData(xidBytes, urid, uriToken, uriRegistryToken, urToken, context.getContextToken(), context.getContextRegistryToken(), tranState, heuristicOnRestart);
                        RRSTranRecoveryData[] recDataArray = new RRSTranRecoveryData[] { recData };
                        if (addExtraRollbackTrans) {
                            recDataArray = new RRSTranRecoveryData[4];
                            recDataArray[0] = recData;
                            for (int x = 1; x < 4; x++) {
                                byte[] curXidBytes = new byte[xidBytes.length];
                                System.arraycopy(xidBytes, 0, curXidBytes, 0, xidBytes.length);
                                curXidBytes[12] = (byte) (curXidBytes[12] + x); // Make a new gtrid
                                curXidBytes[13] = (byte) (curXidBytes[13] + x); // Make a new gtrid
                                Context curCtx = test.generateContext();
                                recDataArray[x] = new RRSTranRecoveryData(curXidBytes, /* Xid */
                                                test.generateURID(), /* URID */
                                                test.generateURIToken(), /* URI Token */
                                                test.generateURIRegistryToken(), /* URI Registry token */
                                                test.generateURToken(), /* UR Token */
                                                curCtx.getContextToken(), /* Context token */
                                                curCtx.getContextRegistryToken(), /* Context registry token */
                                                RRSServices.ATR_IN_BACKOUT, /* State is in-backout */
                                                false); /* Not heuristic */
                            }
                        }

                        // Now the native TM should restart.  It's going to do recovery.
                        // We pass the restart IDs to the native tran manager.
                        test.addRestartExpectations(mockery, seq, mockObjects, new RRSTranRecoveryData[] { recData });
                        final NativeTransactionManager newNativeTM = test.createNativeTranMgr(mockObjects); // Create a new native TM.
                        this.newNativeTM = newNativeTM;

                        // Now the base TM should restart.  It's also going to do recovery.
                        mockery.checking(new Expectations() {
                            {
                                // Async data in case it is heuristic.
                                int[] asynchronousSideDataArray = new int[] { RRSServices.ATR_BACKOUT_REQUIRED,
                                                                              RRSServices.ATR_SDSRM_INITIATED,
                                                                              RRSServices.ATR_RESOLVED_BY_INSTALLATION,
                                                                              RRSServices.ATR_TERM_SYNCPOINT,
                                                                              RRSServices.ATR_IMMEDIATE_BACKOUT,
                                                                              RRSServices.ATR_COMMITTED,
                                                                              RRSServices.ATR_HEURISTIC_MIX };
                                int[] asynchronousSideDataValues = new int[] { RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                                               RRSServices.ATR_SIDE_VALUE_NOT_SET,
                                                                               (expectCommit ? RRSServices.ATR_SIDE_VALUE_SET : RRSServices.ATR_SIDE_VALUE_NOT_SET),
                                                                               (heuristicOnRestart ? RRSServices.ATR_SIDE_VALUE_SET : RRSServices.ATR_SIDE_VALUE_NOT_SET) };

                                // Recovery for base TM uses the bundle context to look up services
                                // for the XAResource factories.  There are two -- one for the
                                // native TM, and one for the mock XAResource we used to force
                                // a two phase commit.
                                BundleContext txMockBundleContext = mockObjects.getTxMockBundleContext();
                                ServiceReference[] xaResourceFactoryRefs = new ServiceReference[] { mockObjects.getXaResourceFactoryRef() };
                                oneOf(txMockBundleContext).getServiceReferences(with(XAResourceFactory.class.getCanonicalName()),
                                                                                with(NativeTransactionManager.class.getCanonicalName()));
                                inSequence(seq);
                                will(returnValue(xaResourceFactoryRefs));
                                oneOf(txMockBundleContext).getService(with(xaResourceFactoryRefs[0]));
                                inSequence(seq);
                                will(returnValue(newNativeTM));
                                RRSServices rrsServices = mockObjects.getRRSServices();
                                if (commit == false) { // If backing out, will do it here.
                                    ContextManager cm = mockObjects.getContextManager();
                                    oneOf(cm).getCurrentContext();
                                    inSequence(seq);
                                    will(returnValue(context));
                                    oneOf(rrsServices).backoutAgentUR(uriRegistryToken, null, RRSServices.ATR_DEFER_EXPLICIT);
                                    inSequence(seq);
                                    will(returnValue(heuristicOnRestart ? RRSServices.ATR_UR_STATE_ERROR : RRSServices.ATR_OK));
                                    if (heuristicOnRestart) {
                                        oneOf(rrsServices).retrieveURData(with(uriToken), with(RRSServices.ATR_EXTENDED_STATES));
                                        inSequence(seq);
                                        will(returnValue(new RetrieveURDataReturnType(RRSServices.ATR_OK, urid, RRSServices.ATR_IN_COMMIT, urToken)));
                                        oneOf(rrsServices).retrieveSideInformation(with(uriRegistryToken), with(asynchronousSideDataArray));
                                        inSequence(seq);
                                        will(returnValue(new RetrieveSideInformationReturnType(RRSServices.ATR_OK, asynchronousSideDataValues)));
                                    }
                                    oneOf(rrsServices).forgetAgentURInterest(uriRegistryToken, RRSServices.ATR_DEFER);
                                    inSequence(seq);
                                    will(returnValue(RRSServices.ATR_OK));
                                }
                                oneOf(txMockBundleContext).getServiceReferences(with(XAResourceFactory.class.getCanonicalName()),
                                                                                with(NoOpXAResourceFactory.class.getCanonicalName()));
                                inSequence(seq);
                                will(returnValue(xaResourceFactoryRefs));
                                oneOf(txMockBundleContext).getService(with(xaResourceFactoryRefs[0]));
                                inSequence(seq);
                                will(returnValue(new NoOpXAResourceFactory()));

                                // Commit the native TM's resource.
                                if (commit) {
                                    oneOf(txMockBundleContext).getServiceReferences(with(XAResourceFactory.class.getCanonicalName()),
                                                                                    with(NativeTransactionManager.class.getCanonicalName()));
                                    inSequence(seq);
                                    will(returnValue(xaResourceFactoryRefs));
                                    oneOf(txMockBundleContext).getService(with(xaResourceFactoryRefs[0]));
                                    inSequence(seq);
                                    will(returnValue(newNativeTM));

                                    oneOf(rrsServices).commitAgentUR(uriRegistryToken, null, RRSServices.ATR_DEFER_EXPLICIT);
                                    inSequence(seq);
                                    will(returnValue(heuristicOnRestart ? RRSServices.ATR_UR_STATE_ERROR : RRSServices.ATR_OK));
                                    if (heuristicOnRestart) {
                                        oneOf(rrsServices).retrieveURData(with(uriToken), with(RRSServices.ATR_EXTENDED_STATES));
                                        inSequence(seq);
                                        will(returnValue(new RetrieveURDataReturnType(RRSServices.ATR_OK, urid, RRSServices.ATR_IN_COMMIT, urToken)));
                                        oneOf(rrsServices).retrieveSideInformation(with(uriRegistryToken), with(asynchronousSideDataArray));
                                        inSequence(seq);
                                        will(returnValue(new RetrieveSideInformationReturnType(RRSServices.ATR_OK, asynchronousSideDataValues)));
                                    } else {
                                        oneOf(rrsServices).forgetAgentURInterest(uriRegistryToken, RRSServices.ATR_DEFER);
                                        inSequence(seq);
                                        will(returnValue(RRSServices.ATR_OK));
                                    }

                                    oneOf(txMockBundleContext).getServiceReferences(with(XAResourceFactory.class.getCanonicalName()),
                                                                                    with(NoOpXAResourceFactory.class.getCanonicalName()));
                                    inSequence(seq);
                                    will(returnValue(xaResourceFactoryRefs));
                                    oneOf(txMockBundleContext).getService(with(xaResourceFactoryRefs[0]));
                                    inSequence(seq);
                                    will(returnValue(new NoOpXAResourceFactory()));

                                    if (heuristicOnRestart) {
                                        oneOf(rrsServices).forgetAgentURInterest(uriRegistryToken, RRSServices.ATR_DEFER);
                                        inSequence(seq);
                                        will(returnValue(RRSServices.ATR_OK));
                                    }
                                    // Normally the mock XAResource would be committed here, but
                                    // because of the way we implemented the mock XAResourceFactory
                                    // the commit becomes a no-op and is never passed to the mock
                                    // XAResource object.
                                }

                                // Base TM calls end of tran.  There is still a UOWEvent
                                // listener registered and therefore it will be called for
                                // end.  This will happen after we've returned to the main
                                // test case.
                                ContextManager cm = mockObjects.getContextManager();
                                oneOf(cm).isInitialized();
                                inSequence(seq);
                                will(returnValue(true));
                                oneOf(cm).end(with(any(UOWCoordinator.class)));
                                inSequence(seq);
                            }
                        });
                    } catch (Exception e) {
                        throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
                    } finally {
                        try {
                            startTM(10, true); // Create a new base TM, wait for recovery to complete.
                        } catch (Exception e) {
                            throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
                        }
                    }

                    try {
                        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
                        ((UOWCurrent) tm).setUOWEventListener(newNativeTM);

                        // Base TM should call commit on native TM.  We are done.
                        testPassed = true;
                    } catch (Exception e) {
                        throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
                    }

                    // -------------------------------------------------------------
                    // We're going to return with no error here.  The mock framework
                    // is set up to expect RRS commit to be called, and that will
                    // happen from the restart/recovery TM.
                    // By returning here, the original call to commit or backout
                    // will return successfully.
                    // -------------------------------------------------------------

                } else {
                    throw (XAException) new XAException(XAException.XAER_RMERR).initCause(new Exception("Second commit call on XAResource"));
                }
            } else {
                throw (XAException) new XAException(XAException.XAER_RMERR).initCause(new Exception("Restart XAResource called for commit out of context."));
            }
        }

        /** {@inheritDoc} */
        @Override
        public void end(Xid xid, int flags) throws XAException {
            xares.end(xid, flags);
        }

        /** {@inheritDoc} */
        @Override
        public void forget(Xid xid) throws XAException {
            xares.forget(xid);
        }

        /** {@inheritDoc} */
        @Override
        public int getTransactionTimeout() throws XAException {
            return xares.getTransactionTimeout();
        }

        /** {@inheritDoc} */
        @Override
        public boolean isSameRM(XAResource theXAResource) throws XAException {
            return xares.isSameRM(theXAResource);
        }

        /** {@inheritDoc} */
        @Override
        public int prepare(Xid xid) throws XAException {
            return xares.prepare(xid);
        }

        /** {@inheritDoc} */
        @Override
        public Xid[] recover(int flag) throws XAException {
            throw new XAException("Should not be called for recover");
        }

        /** {@inheritDoc} */
        @Override
        public void rollback(Xid xid) throws XAException {
            if (expectCommit == false) {
                processOutcome(xid, false);
            } else {
                throw new XAException("XAResource was driven for incorrect outcome");
            }
        }

        /** {@inheritDoc} */
        @Override
        public boolean setTransactionTimeout(int seconds) throws XAException {
            return xares.setTransactionTimeout(seconds);
        }

        /** {@inheritDoc} */
        @Override
        public void start(Xid xid, int flags) throws XAException {
            if (RestartTMInDoubtXAResourceWrapper.xid != null) {
                throw (XAException) new XAException(XAException.XAER_RMERR).initCause(new IllegalStateException("Xid was already set"));
            }
            startedTran = true;
            RestartTMInDoubtXAResourceWrapper.xid = xid;
            xares.start(xid, flags);
        }

        /** Gets the native TM created during commit to do recovery. */
        private NativeTransactionManager getRecoveryNativeTM() {
            return this.newNativeTM;
        }
    }

    //========================================================================
    // Utility methods
    //========================================================================

    /**
     * Starts the TM. This method should only be called after a TMHelper.shutdown().
     */
    public static void startTM(int waitCount, boolean waitForRecovery) throws Exception {
        int count = 1;

        while (count <= 10) {
            TMHelper.start(waitForRecovery);
            try {
                TMHelper.checkTMState();
            } catch (NotSupportedException nse) {
                count++;
                Thread.sleep(1000);
            }
            break;
        }
    }

    /**
     * Creates a native transaction manager / XA resource factory.
     *
     * @param mockObjects The set of mock objects used by these test cases.
     *
     * @return A NativeTransactionManager which has been initialized with the
     *         mock ContextManager and RRSServices objects. The resource manager
     *         name needs to be set before any transactions can be processed.
     */
    private NativeTransactionManager createNativeTranMgr(MockObjectSet mockObjects) {
        NativeTransactionManager tm = new NativeTransactionManager();
        tm.setContextManager(mockObjects.getContextManager());
        tm.setRRSServices(mockObjects.getRRSServices());
        tm.setLocationAdmin(mockObjects.getLocationAdmin());
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(NativeTransactionManager.SHUTDOWN_TIMEOUT_PROPERTY, new Long(15000L));
        props.put(NativeTransactionManager.RMNAME_PREFIX_PROPERTY, "DEAFULT");
        tm.activate(mockObjects.getBundleContext(), props);
        return tm;
    }

    /**
     * Takes a java.lang.String and converts it to a byte array of the
     * specified length in the EBCDIC code page. If the string length is less
     * than the specified length, it is padded on the right with EBCDIC blanks.
     *
     * @param str    The string to convert to EBCDIC.
     * @param length The desired length of the resulting byte array.
     *
     * @return The EBCDIC byte array representation of the input string.
     */
    private static byte[] makePaddedEBCDICBytes(String str, int length) {
        byte[] retBytes = new byte[length];
        for (int x = 0; x < length; x++) {
            retBytes[x] = (byte) 0x40;
        }

        try {
            if (str != null) {
                byte[] ebcdicBytes = str.getBytes("Cp1047");
                System.arraycopy(ebcdicBytes, 0, retBytes, 0, str.length());
            }
        } catch (Throwable t) {
            throw new RuntimeException("String conversion error. EBCDIC codepage", t);
        }

        return retBytes;
    }

    private void addABAKExpectations(Mockery mockery, final MockObjectSet mockObjects, final int abakReturnCode) throws RegistryException {
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();
        final Context emptyContext = generateContext();
        final byte[] uriToken = generateURIToken();
        final byte[] uriRegistryToken = generateURIRegistryToken();
        final RRSServices rrsServices = mockObjects.getRRSServices();
        final ContextManager contextManager = mockObjects.getContextManager();
        final Sequence backoutSequence = mockery.sequence("backoutSequence");

        mockery.checking(new Expectations() {
            {
                // Tran manager is going to check to see if context manager is initialized.
                oneOf(contextManager).isInitialized();
                inSequence(backoutSequence);
                will(returnValue(true));

                // Context services will create a new context on the begin.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(backoutSequence);

                // Begin the transaction
                oneOf(rrsServices).beginTransaction(with(RRSServices.ATR_GLOBAL_MODE));
                inSequence(backoutSequence);
                will(returnValue(new BeginTransactionReturnType(0, urToken, urID)));
                oneOf(rrsServices).setWorkIdentifier(with(urToken), with(any(byte[].class)));
                inSequence(backoutSequence);
                will(returnValue(0));
                oneOf(contextManager).getCurrentContext();
                inSequence(backoutSequence);
                will(returnValue(context));

                // Suspend the transaction
                oneOf(contextManager).isInitialized();
                inSequence(backoutSequence);
                will(returnValue(true));
                oneOf(contextManager).suspend(with(any(UOWCoordinator.class)));
                inSequence(backoutSequence);

                // End the transaction
                oneOf(contextManager).getCurrentContext();
                inSequence(backoutSequence);
                will(returnValue(emptyContext));
                oneOf(rrsServices).expressURInterest(with(rmRegistryToken), with(context.getContextToken()), with(RRSServices.ATR_PRESUMED_ABORT),
                                                     with(any(byte[].class)) /* nonPData */,
                                                     with(any(byte[].class)) /* pdata */, with(aNull(byte[].class)) /* xid */, with(aNull(byte[].class)) /* parentUrToken */);
                inSequence(backoutSequence);
                will(returnValue(new ExpressInterestReturnType(RRSServices.ATR_OK, uriToken, uriRegistryToken, null /* curCtxToken */, null /* urid */, null /* nonPData */, null /*
                                                                                                                                                                                   * diagArea
                                                                                                                                                                                   */, RRSServices.ATR_GLOBAL_MODE, null /*
                                                                                                                                                                                                                          * urToken
                                                                                                                                                                                                                          */)));
                oneOf(rrsServices).setSyncpointControls(with(uriRegistryToken), with(RRSServices.ATR_PREPARE_OK), with(RRSServices.ATR_COMMIT_OK),
                                                        with(RRSServices.ATR_BACKOUT_OK),
                                                        with(RRSServices.ATR_SDSRM));
                inSequence(backoutSequence);
                will(returnValue(RRSServices.ATR_OK));

                oneOf(rrsServices).backoutAgentUR(with(uriRegistryToken), with(aNull(byte[].class)), with(RRSServices.ATR_DEFER_EXPLICIT));
                inSequence(backoutSequence);
                will(returnValue(abakReturnCode));

                oneOf(rrsServices).forgetAgentURInterest(with(uriRegistryToken), with(RRSServices.ATR_DEFER));
                inSequence(backoutSequence);
                will(returnValue(RRSServices.ATR_OK));

                // Clean up the context
                oneOf(contextManager).isInitialized();
                inSequence(backoutSequence);
                will(returnValue(true));
                oneOf(contextManager).end(with(any(UOWCoordinator.class)));
                inSequence(backoutSequence);
            }
        });
    }

    private void addAPRPExpectations(Mockery mockery, final MockObjectSet mockObjects, final int aprpReturnCode, final boolean xaresCommit,
                                     final boolean callBackout) throws Exception {
        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();
        final byte[] uriToken = generateURIToken();
        final byte[] uriRegistryToken = generateURIRegistryToken();
        final byte[] ciRegistryToken = (aprpReturnCode == RRSServices.ATR_OK) ? generateCIRegistryToken() : null;
        final Sequence prepareSequence = mockery.sequence("prepareSequence");

        mockery.checking(new Expectations() {
            {
                final RRSServices rrsServices = mockObjects.getRRSServices();
                final ContextManager contextManager = mockObjects.getContextManager();
                final XAResource mockXares = mockObjects.getMockXAResource();

                // Tran manager is going to check to see if context manager is initialized.
                oneOf(contextManager).isInitialized();
                inSequence(prepareSequence);
                will(returnValue(true));

                // Context services will create a new context on the begin.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(prepareSequence);

                // Begin the transaction
                oneOf(rrsServices).beginTransaction(with(RRSServices.ATR_GLOBAL_MODE));
                inSequence(prepareSequence);
                will(returnValue(new BeginTransactionReturnType(0, urToken, urID)));
                oneOf(rrsServices).setWorkIdentifier(with(urToken), with(any(byte[].class)));
                inSequence(prepareSequence);
                will(returnValue(0));
                oneOf(contextManager).getCurrentContext();
                inSequence(prepareSequence);
                will(returnValue(context));

                // Enlist the mock resource
                oneOf(mockXares).start(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(prepareSequence);
                oneOf(mockXares).end(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(prepareSequence);

                // End the transaction
                oneOf(rrsServices).expressURInterest(with(rmRegistryToken), with(context.getContextToken()), with(RRSServices.ATR_PRESUMED_ABORT),
                                                     with(any(byte[].class)) /* nonPData */,
                                                     with(any(byte[].class)) /* pdata */, with(aNull(byte[].class)) /* xid */, with(aNull(byte[].class)) /* parentUrToken */);
                inSequence(prepareSequence);
                will(returnValue(new ExpressInterestReturnType(RRSServices.ATR_OK, uriToken, uriRegistryToken, null /* curCtxToken */, null /* urid */, null /* nonPData */, null /*
                                                                                                                                                                                   * diagArea
                                                                                                                                                                                   */, RRSServices.ATR_GLOBAL_MODE, null /*
                                                                                                                                                                                                                          * urToken
                                                                                                                                                                                                                          */)));
                oneOf(rrsServices).setSyncpointControls(with(uriRegistryToken), with(RRSServices.ATR_PREPARE_OK), with(RRSServices.ATR_COMMIT_OK),
                                                        with(RRSServices.ATR_BACKOUT_OK),
                                                        with(RRSServices.ATR_SDSRM));
                inSequence(prepareSequence);
                will(returnValue(RRSServices.ATR_OK));
                oneOf(rrsServices).prepareAgentUR(with(uriRegistryToken), with(context.getContextRegistryToken()), with(rmRegistryToken), with(RRSServices.ATR_DEFER_IMPLICIT));
                inSequence(prepareSequence);
                will(returnValue(new PrepareAgentURReturnType(aprpReturnCode, RRSServices.CTX_OK, ciRegistryToken)));

                atMost(1).of(mockXares).prepare(with(any(Xid.class)));
                inSequence(prepareSequence);
                will(returnValue(XAResource.XA_OK));
                if (xaresCommit) {
                    oneOf(mockXares).commit(with(any(Xid.class)), with(any(Boolean.TYPE)));
                    inSequence(prepareSequence);
                } else {
                    oneOf(mockXares).rollback(with(any(Xid.class)));
                    inSequence(prepareSequence);
                }

                if (callBackout) {
                    oneOf(contextManager).getCurrentContext();
                    inSequence(prepareSequence);
                    will(returnValue(context));
                    oneOf(rrsServices).backoutAgentUR(with(uriRegistryToken), ciRegistryToken, with(RRSServices.ATR_DEFER_IMPLICIT));
                    inSequence(prepareSequence);
                    will(returnValue(RRSServices.ATR_OK));
                }

                // Clean up the context
                // Suspend the transaction
                oneOf(contextManager).isInitialized();
                inSequence(prepareSequence);
                will(returnValue(true));
                oneOf(contextManager).suspend(with(any(UOWCoordinator.class)));
                inSequence(prepareSequence);

                oneOf(contextManager).isInitialized();
                inSequence(prepareSequence);
                will(returnValue(true));
                oneOf(contextManager).end(with(any(UOWCoordinator.class)));
                inSequence(prepareSequence);
            }
        });
    }

    private void addACMTExpectations(Mockery mockery, MockObjectSet mockObjects, final int acmtReturnCode, final boolean callForget) throws Exception {
        final RRSServices rrsServices = mockObjects.getRRSServices();
        final ContextManager contextManager = mockObjects.getContextManager();
        final XAResource mockXares = mockObjects.getMockXAResource();

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();
        final byte[] uriToken = generateURIToken();
        final byte[] uriRegistryToken = generateURIRegistryToken();
        final byte[] ciRegistryToken = generateCIRegistryToken();
        final Sequence commitSequence = mockery.sequence("commitSequence");

        mockery.checking(new Expectations() {
            {
                // Tran manager is going to check to see if context manager is initialized.
                oneOf(contextManager).isInitialized();
                inSequence(commitSequence);
                will(returnValue(true));

                // Context services will create a new context on the begin.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(commitSequence);

                // Begin the transaction
                oneOf(rrsServices).beginTransaction(with(RRSServices.ATR_GLOBAL_MODE));
                inSequence(commitSequence);
                will(returnValue(new BeginTransactionReturnType(0, urToken, urID)));
                oneOf(rrsServices).setWorkIdentifier(with(urToken), with(any(byte[].class)));
                inSequence(commitSequence);
                will(returnValue(0));
                oneOf(contextManager).getCurrentContext();
                inSequence(commitSequence);
                will(returnValue(context));

                // Enlist the mock resource
                oneOf(mockXares).start(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(commitSequence);
                oneOf(mockXares).end(with(any(Xid.class)), with(any(Integer.TYPE)));
                inSequence(commitSequence);

                // End the transaction
                oneOf(rrsServices).expressURInterest(with(rmRegistryToken), with(context.getContextToken()), with(RRSServices.ATR_PRESUMED_ABORT),
                                                     with(any(byte[].class)) /* nonPData */,
                                                     with(any(byte[].class)) /* pdata */, with(aNull(byte[].class)) /* xid */, with(aNull(byte[].class)) /* parentUrToken */);
                inSequence(commitSequence);
                will(returnValue(new ExpressInterestReturnType(RRSServices.ATR_OK, uriToken, uriRegistryToken, null /* curCtxToken */, null /* urid */, null /* nonPData */, null /*
                                                                                                                                                                                   * diagArea
                                                                                                                                                                                   */, RRSServices.ATR_GLOBAL_MODE, null /*
                                                                                                                                                                                                                          * urToken
                                                                                                                                                                                                                          */)));
                oneOf(rrsServices).setSyncpointControls(with(uriRegistryToken), with(RRSServices.ATR_PREPARE_OK), with(RRSServices.ATR_COMMIT_OK),
                                                        with(RRSServices.ATR_BACKOUT_OK),
                                                        with(RRSServices.ATR_SDSRM));
                inSequence(commitSequence);
                will(returnValue(RRSServices.ATR_OK));
                oneOf(rrsServices).prepareAgentUR(with(uriRegistryToken), with(context.getContextRegistryToken()), with(rmRegistryToken), with(RRSServices.ATR_DEFER_IMPLICIT));
                inSequence(commitSequence);
                will(returnValue(new PrepareAgentURReturnType(RRSServices.ATR_OK, RRSServices.CTX_OK, ciRegistryToken)));
                atMost(1).of(mockXares).prepare(with(any(Xid.class)));
                inSequence(commitSequence);
                will(returnValue(XAResource.XA_OK));
                atMost(1).of(mockXares).commit(with(any(Xid.class)), with(any(Boolean.TYPE)));
                inSequence(commitSequence);
                oneOf(rrsServices).commitAgentUR(with(uriRegistryToken), with(ciRegistryToken), with(RRSServices.ATR_DEFER_EXPLICIT));
                inSequence(commitSequence);
                will(returnValue(acmtReturnCode));
                if (callForget) {
                    oneOf(rrsServices).forgetAgentURInterest(with(uriRegistryToken), with(RRSServices.ATR_DEFER));
                    inSequence(commitSequence);
                    will(returnValue(RRSServices.ATR_OK));
                }

                // Clean up the context
                // Suspend the transaction
                oneOf(contextManager).isInitialized();
                inSequence(commitSequence);
                will(returnValue(true));
                oneOf(contextManager).suspend(with(any(UOWCoordinator.class)));
                inSequence(commitSequence);

                oneOf(contextManager).isInitialized();
                inSequence(commitSequence);
                will(returnValue(true));
                oneOf(contextManager).end(with(any(UOWCoordinator.class)));
                inSequence(commitSequence);
            }
        });
    }

    private void addADCTExpectations(Mockery mockery, MockObjectSet mockObjects, final int adctReturnCode, final boolean callForget) throws RegistryException {
        final byte[] urToken = generateURToken();
        final byte[] urID = generateURID();
        final Context context = generateContext();
        final Context emptyContext = generateContext();
        final byte[] uriToken = generateURIToken();
        final byte[] uriRegistryToken = generateURIRegistryToken();
        final Sequence commitSequence = mockery.sequence("commitSequence");
        final RRSServices rrsServices = mockObjects.getRRSServices();
        final ContextManager contextManager = mockObjects.getContextManager();

        mockery.checking(new Expectations() {
            {
                // Tran manager is going to check to see if context manager is initialized.
                oneOf(contextManager).isInitialized();
                inSequence(commitSequence);
                will(returnValue(true));

                // Context services will create a new context on the begin.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(commitSequence);

                // Begin the transaction
                oneOf(rrsServices).beginTransaction(with(RRSServices.ATR_GLOBAL_MODE));
                inSequence(commitSequence);
                will(returnValue(new BeginTransactionReturnType(0, urToken, urID)));
                oneOf(rrsServices).setWorkIdentifier(with(urToken), with(any(byte[].class)));
                inSequence(commitSequence);
                will(returnValue(0));
                oneOf(contextManager).getCurrentContext();
                inSequence(commitSequence);
                will(returnValue(context));

                // Suspend the transaction
                oneOf(contextManager).isInitialized();
                inSequence(commitSequence);
                will(returnValue(true));
                oneOf(contextManager).suspend(with(any(UOWCoordinator.class)));
                inSequence(commitSequence);

                // End the transaction
                oneOf(contextManager).getCurrentContext();
                inSequence(commitSequence);
                will(returnValue(emptyContext));

                oneOf(rrsServices).expressURInterest(with(rmRegistryToken), with(context.getContextToken()), with(RRSServices.ATR_PRESUMED_ABORT),
                                                     with(any(byte[].class)) /* nonPData */,
                                                     with(any(byte[].class)) /* pdata */, with(aNull(byte[].class)) /* xid */, with(aNull(byte[].class)) /* parentUrToken */);
                inSequence(commitSequence);

                will(returnValue(new ExpressInterestReturnType(RRSServices.ATR_OK, uriToken, uriRegistryToken, null /* curCtxToken */, null /* urid */, null /* nonPData */, null /*
                                                                                                                                                                                   * diagArea
                                                                                                                                                                                   */, RRSServices.ATR_GLOBAL_MODE, null /*
                                                                                                                                                                                                                          * urToken
                                                                                                                                                                                                                          */)));
                oneOf(rrsServices).setSyncpointControls(with(uriRegistryToken), with(RRSServices.ATR_PREPARE_OK), with(RRSServices.ATR_COMMIT_OK),
                                                        with(RRSServices.ATR_BACKOUT_OK),
                                                        with(RRSServices.ATR_SDSRM));
                inSequence(commitSequence);
                will(returnValue(RRSServices.ATR_OK));

                oneOf(rrsServices).delegateCommitAgentUR(with(uriRegistryToken), with(RRSServices.ATR_DEFER_EXPLICIT), with(RRSServices.ATR_STANDARD_COMMIT_MASK));
                inSequence(commitSequence);
                will(returnValue(adctReturnCode));

                if (callForget) {
                    oneOf(rrsServices).forgetAgentURInterest(with(uriRegistryToken), with(RRSServices.ATR_DEFER));
                    inSequence(commitSequence);
                    will(returnValue(RRSServices.ATR_OK));
                }
                // Clean up the context
                oneOf(contextManager).isInitialized();
                inSequence(commitSequence);
                will(returnValue(true));
                oneOf(contextManager).end(with(any(UOWCoordinator.class)));
                inSequence(commitSequence);
            }
        });
    }

    /**
     * Adds expectations for a transaction which calls ATREND.
     */
    private void addEndExpectations(Mockery mockery, final RRSServices rrsServices, final ContextManager contextManager, final byte[] urToken, final byte[] urID,
                                    final Context context, final int endAction, final int endReturnCode) {
        // -------------------------------------------------------------------
        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.  The test format is:
        //
        // tm.begin();
        // tm.enlist(xares);
        // tm.commit();
        // -------------------------------------------------------------------
        final Sequence endSequence = mockery.sequence("endSequence");
        mockery.checking(new Expectations() {
            {
                // Tran manager is going to check to see if context manager is initialized.
                oneOf(contextManager).isInitialized();
                inSequence(endSequence);
                will(returnValue(true));

                // Context services will create a new context on the begin.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(endSequence);

                // Begin the transaction
                oneOf(rrsServices).beginTransaction(with(RRSServices.ATR_GLOBAL_MODE));
                inSequence(endSequence);
                will(returnValue(new BeginTransactionReturnType(0, urToken, urID)));
                oneOf(rrsServices).setWorkIdentifier(with(urToken), with(any(byte[].class)));
                inSequence(endSequence);
                will(returnValue(0));
                oneOf(contextManager).getCurrentContext();
                inSequence(endSequence);
                will(returnValue(context));

                // End the transaction
                oneOf(contextManager).getCurrentContext();
                inSequence(endSequence);
                will(returnValue(context));
                oneOf(rrsServices).endUR(endAction, urToken);
                inSequence(endSequence);
                will(returnValue(endReturnCode));

                // Clean up the context
                oneOf(contextManager).isInitialized();
                inSequence(endSequence);
                will(returnValue(true));
                oneOf(contextManager).suspend(with(any(UOWCoordinator.class)));
                inSequence(endSequence);
                oneOf(contextManager).isInitialized();
                inSequence(endSequence);
                will(returnValue(true));
                oneOf(contextManager).end(with(any(UOWCoordinator.class)));
                inSequence(endSequence);
            }
        });
    }

    /**
     * Adds the RRS restart expectations sequence to the mockery object.
     *
     * @param mockery     The junit mockery object to add the expectations to.
     * @param mockObjects The set of mock objects used by the expectations.
     */
    private void addRestartExpectations(Mockery mockery, final MockObjectSet mockObjects) throws RegistryException {
        final Sequence restartSequence = mockery.sequence("restartSequence");
        addRestartExpectations(mockery, restartSequence, mockObjects, null);
    }

    /**
     * Adds the RRS restart expectations sequence to the mockery object.
     *
     * @param mockery          The junit mockery object to add the expectations to.
     * @param restartSequence  The sequence to add expectations to.
     * @param mockObjects      The set of mock objects used by the expectations.
     * @param tranRecoveryData An array of recovery data to return on the recovery calls.
     */
    @SuppressWarnings("unchecked")
    private void addRestartExpectations(Mockery mockery, final Sequence restartSequence, final MockObjectSet mockObjects,
                                        final RRSTranRecoveryData[] transToRecover) throws RegistryException {
        final boolean areXidsToRecover = ((transToRecover == null) || (transToRecover.length == 0)) ? false : true;
        final RRSServices rrsServices = mockObjects.getRRSServices();
        final ContextManager contextManager = mockObjects.getContextManager();
        final BundleContext bc = mockObjects.getBundleContext();
        final WsLocationAdmin locAdmin = mockObjects.getLocationAdmin();
        final WsResource rmNameLog = mockObjects.getLognameResource();
        final String rmNameLogFilename = "rrs/tx/rmData.log";

        // Tell JMock that we expect begin context to be called, and tell it
        // what to return.
        mockery.checking(new Expectations() {
            {
                // Do the bundle registration thing.
                oneOf(bc).registerService(with(any(String[].class)), with(any(Object.class)), with(any(Dictionary.class)));
                inSequence(restartSequence);
                will(returnValue(null));
                // The server is going to want to read the RM name from the RM log.
                oneOf(locAdmin).getServerWorkareaResource(with(rmNameLogFilename));
                inSequence(restartSequence);
                will(returnValue(rmNameLog));
                oneOf(rmNameLog).exists();
                inSequence(restartSequence);
                will(returnValue(true));

                try {
                    oneOf(rmNameLog).get();
                    inSequence(restartSequence);
                    will(returnValue(new ByteArrayInputStream(rmName.getBytes())));
                } catch (IOException ioe) {
                    // Nothing
                }

                // Register with registration services.
                oneOf(rrsServices).registerResourceManager(with(RRSServices.CRG_UNREG_EOM), with(new byte[16]), with(any(byte[].class)), with(any(byte[].class)));
                inSequence(restartSequence);
                will(returnValue(new RegisterResMgrReturnType(0, rmNameBytes, rmNameRegistryToken, rmToken, rmRegistryToken, 0, 0, 0, 0)));
                // Initialize the context manager
                oneOf(contextManager).initialize(with(rmRegistryToken));
                inSequence(restartSequence);
                // Set our exits with context services and RRS.
                oneOf(rrsServices).setExitInformation(with(rmNameRegistryToken), with(rmRegistryToken), with(any(Boolean.TYPE)));
                inSequence(restartSequence);
                will(returnValue(new SetExitInformationReturnType(0, true)));
                // Set local tran as the default mode for new URs.
                oneOf(rrsServices).setEnvironment(with(aNull(byte[].class)),
                                                  with(aNonNull(int[].class)),
                                                  with(aNonNull(int[].class)),
                                                  with(aNonNull(int[].class)));
                inSequence(restartSequence);
                will(returnValue(0));
                // Logname exchange.
                oneOf(rrsServices).retrieveLogName(with(rmRegistryToken));
                inSequence(restartSequence);
                will(returnValue(new RetrieveLogNameReturnType(0, new byte[0], new byte[0])));
                oneOf(rrsServices).setLogName(with(rmRegistryToken), with(any(byte[].class)));
                inSequence(restartSequence);
                will(returnValue(0));
                // Begin restart
                oneOf(rrsServices).beginRestart(with(rmRegistryToken));
                inSequence(restartSequence);
                will(returnValue(0));
                // Iterate over restart URs.
                for (int x = 0; x < (areXidsToRecover ? transToRecover.length : 0); x++) {
                    oneOf(rrsServices).retrieveURInterest(with(rmRegistryToken));
                    inSequence(restartSequence);
                    will(returnValue(new RetrieveURInterestReturnType(RRSServices.ATR_OK, transToRecover[x].ctxToken, transToRecover[x].uriToken, transToRecover[x].uriRegistryToken, transToRecover[x].urid, RRSServices.ATR_SDSRM, transToRecover[x].rrsState, new byte[16] /*
                                                                                                                                                                                                                                                                               * pdata
                                                                                                                                                                                                                                                                               */)));
                    oneOf(rrsServices).retrieveWorkIdentifier(with(transToRecover[x].uriRegistryToken));
                    inSequence(restartSequence);
                    will(returnValue(new RetrieveWorkIdentifierReturnType(RRSServices.ATR_OK, transToRecover[x].xidBytes)));
                    oneOf(rrsServices).retrieveSideInformation(transToRecover[x].uriRegistryToken, new int[] { RRSServices.ATR_HEURISTIC_MIX });
                    inSequence(restartSequence);
                    will(returnValue(new RetrieveSideInformationReturnType(RRSServices.ATR_OK, new int[] { (transToRecover[x].isHeuristic) ? RRSServices.ATR_SIDE_VALUE_SET : RRSServices.ATR_SIDE_VALUE_NOT_SET })));
                    int response = ((transToRecover[x].rrsState == RRSServices.ATR_IN_DOUBT)
                                    || (transToRecover[x].isHeuristic)) ? RRSServices.ATR_RESPOND_CONTINUE : RRSServices.ATR_RESPOND_COMPLETE;
                    oneOf(rrsServices).respondToRetrievedInterest(with(transToRecover[x].uriRegistryToken), with(response), with(any(byte[].class)));
                    inSequence(restartSequence);
                    will(returnValue(RRSServices.ATR_OK));
                }
                // No more URs to recover.
                oneOf(rrsServices).retrieveURInterest(with(rmRegistryToken));
                inSequence(restartSequence);
                will(returnValue(new RetrieveURInterestReturnType(RRSServices.ATR_NO_MORE_INCOMPLETE_INTERESTS, null, null, null, null, 0, 0, null)));
                // End restart
                oneOf(rrsServices).endRestart(with(rmRegistryToken));
                inSequence(restartSequence);
                will(returnValue(0));
                //Call to setRMMetadata. No metada logged. Log one.
                oneOf(rrsServices).retrieveRMMetadata(with(rmRegistryToken));
                inSequence(restartSequence);
                will(returnValue(new RetrieveRMMetadataReturnType(0, new byte[0])));
                oneOf(locAdmin).getServerName();
                inSequence(restartSequence);
                will(returnValue("MyServerName"));
                oneOf(locAdmin).getServerId();
                inSequence(restartSequence);
                will(returnValue(new UUID(with(any(Long.TYPE)), with(any(Long.TYPE)))));
                oneOf(locAdmin).resolveString(with(any(String.class)));
                inSequence(restartSequence);
                will(returnValue("MyServerConfigDir"));
                oneOf(rrsServices).setRMMetadata(with(rmRegistryToken), with(any(byte[].class)));
                inSequence(restartSequence);
                will(returnValue(0));
            }
        });
    }

    private byte[] generateCIRegistryToken() {
        return generateRandomByteArray(64);
    }

    /**
     * Adds LTC common expectations.
     *
     * @param mockery     The junit mockery object to add the expectations to.
     * @param mockObjects The mock objects.
     */
    private void addLTCCommonExpectations(Mockery mockery, final MockObjectSet mockObjects, final Context context) {
        final ContextManager contextManager = mockObjects.getContextManager();

        mockery.checking(new Expectations() {
            {
                // NatvTxManagerImpl.UOWEvent: POST_BEGIN: Check if context manager is intialized.
                oneOf(contextManager).isInitialized();
                will(returnValue(true));

                // NatvTxManagerImpl.UOWEvent: POST_BEGIN: Calls begin on context manager.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));

                // LocalTranManagerImpl.enlist: Gets the current context on the thread.
                oneOf(contextManager).getCurrentContext();
                will(returnValue(context));

                // Expectations for local transaction end calls (cleanup/complete) at the end
                // of the LTC boundary will be provided by each test individually.
                // This will include resource calls for commit or rollback and ATREND calls when
                // needed.

                // NatvTxManagerImpl.UOWEvent: POST_END: Calls begin on context manager.
                oneOf(contextManager).isInitialized();
                will(returnValue(true));
                oneOf(contextManager).end(with(any(UOWCoordinator.class)));
            }
        });
    }

    /**
     * UOWEvent POST_BEGIN expectations.
     *
     * @param mockery     The junit mockery object to add the expectations to.
     * @param mockObjects The mock objects.
     */
    private void addUOWEventPostBeginExpectations(Mockery mockery, final MockObjectSet mockObjects) {
        final ContextManager contextManager = mockObjects.getContextManager();
        final Sequence uowEventPostBeginSequence = mockery.sequence("uowEventPostBeginSequence");

        mockery.checking(new Expectations() {
            {
                // Native TM check to see in the context has been initialized.
                oneOf(contextManager).isInitialized();
                will(returnValue(true));
                inSequence(uowEventPostBeginSequence);

                // Native TM call to the context manager to put a context on the thread.
                oneOf(contextManager).begin(with(any(UOWCoordinator.class)));
                inSequence(uowEventPostBeginSequence);
            }
        });
    }

    /**
     * UOWEvent SUSPEND expectations.
     *
     * @param mockery     The junit mockery object to add the expectations to.
     * @param mockObjects The mock objects.
     */
    private void addUOWEventSuspendExpectations(Mockery mockery, final MockObjectSet mockObjects) {
        final ContextManager contextManager = mockObjects.getContextManager();
        final Sequence uowEventSuspendSequence = mockery.sequence("uowEventSuspendSequence");

        mockery.checking(new Expectations() {
            {
                // Native TM check to see in the context has been initialized.
                oneOf(contextManager).isInitialized();
                will(returnValue(true));
                inSequence(uowEventSuspendSequence);

                // Native TM call to the context manager to suspend the current context on the thread.
                oneOf(contextManager).suspend(with(any(UOWCoordinator.class)));
                inSequence(uowEventSuspendSequence);
            }
        });
    }

    /**
     * UOWEvent RESUME expectations.
     *
     * @param mockery     The junit mockery object to add the expectations to.
     * @param mockObjects The mock objects.
     */
    private void addUOWEventResumeExpectations(Mockery mockery, final MockObjectSet mockObjects) {
        final ContextManager contextManager = mockObjects.getContextManager();
        final Sequence uowEventResumeSequence = mockery.sequence("uowEventResumeSequence");

        mockery.checking(new Expectations() {
            {
                // Native TM check to see in the context has been initialized.
                oneOf(contextManager).isInitialized();
                will(returnValue(true));
                inSequence(uowEventResumeSequence);

                // Native TM call to the context manager to put suspended context back on the thread.
                oneOf(contextManager).resume(with(any(UOWCoordinator.class)));
                inSequence(uowEventResumeSequence);
            }
        });
    }

    /**
     * UOWEvent POST_END expectations.
     *
     * @param mockery     The junit mockery object to add the expectations to.
     * @param mockObjects The mock objects.
     */
    private void addUOWEventPostEndExpectations(Mockery mockery, final MockObjectSet mockObjects) {
        final ContextManager contextManager = mockObjects.getContextManager();
        final Sequence uowEventPostEndSequence = mockery.sequence("uowEventPostEndSequence");

        mockery.checking(new Expectations() {
            {
                // Native TM check to see in the context has been initialized.
                oneOf(contextManager).isInitialized();
                will(returnValue(true));
                inSequence(uowEventPostEndSequence);

                // Native TM call to the context manager to "end" the context on the thread.
                oneOf(contextManager).end(with(any(UOWCoordinator.class)));
                inSequence(uowEventPostEndSequence);
            }
        });
    }

    /**
     * Global tx normal completion expectations.
     *
     * @param mockery     The junit mockery object to add the expectations to.
     * @param mockObjects The mock objects.
     */
    private void addGlobalTxCompletionExpectations(Mockery mockery, final MockObjectSet mockObjects) {
        final ContextManager contextManager = mockObjects.getContextManager();
        final Sequence globalTxCompletionSequence = mockery.sequence("globalTxCompletionSequence");

        mockery.checking(new Expectations() {
            {
                // Native TM check to see in the context has been initialized.
                oneOf(contextManager).isInitialized();
                will(returnValue(true));
                inSequence(globalTxCompletionSequence);

                // Native TM call to the context manager to suspend the current context on the thread.
                oneOf(contextManager).suspend(with(any(UOWCoordinator.class)));
                inSequence(globalTxCompletionSequence);
            }
        });
    }

    private byte[] generateURToken() {
        return generateRandomByteArray(16);
    }

    private byte[] generateURID() {
        return generateRandomByteArray(16);
    }

    private byte[] generateURIToken() {
        return generateRandomByteArray(16);
    }

    private byte[] generateURIRegistryToken() {
        return generateRandomByteArray(64);
    }

    private byte[] generateRandomByteArray(int size) {
        byte[] bytes = new byte[size];
        generator.nextBytes(bytes);
        return bytes;
    }

    private Context generateContext() {
        return new Context() {
            private final byte[] ctxToken = generateRandomByteArray(16);
            private final byte[] ctxRegistryToken = generateRandomByteArray(64);
            private byte[] ciRegistryToken = null;

            @Override
            public synchronized byte[] getContextToken() {
                return ctxToken;
            }

            @Override
            public byte[] getContextRegistryToken() {
                return ctxRegistryToken;
            }

            @Override
            public byte[] getContextInterestRegistryToken() {
                byte[] ciRegistryToken = null;
                if (this.ciRegistryToken != null) {
                    ciRegistryToken = new byte[this.ciRegistryToken.length];
                    System.arraycopy(this.ciRegistryToken, 0, ciRegistryToken, 0, ciRegistryToken.length);
                }
                return ciRegistryToken;
            }

            @Override
            public void setContextInterestRegistryToken(byte[] token) {
                if (token != null) {
                    ciRegistryToken = new byte[token.length];
                    System.arraycopy(token, 0, ciRegistryToken, 0, token.length);
                } else {
                    ciRegistryToken = null;
                }
            }
        };
    }

    private boolean isTranMapEmpty(NativeTransactionManager tm) {
        Map<?, ?> globalTxMap = tm.getGlobalTxMap();
        Map<?, ?> localTxMap = tm.getLocalTxMap();
        Map<String, HashMap<ByteArray, RestartURData>> restartRMMap = tm.getRestartedRMMap();

        boolean globalMapEmpty = (globalTxMap == null) || (globalTxMap.isEmpty());
        boolean localMapEmpty = (localTxMap == null) || (localTxMap.isEmpty());
        boolean restartMapEmpty = true;
        for (Map<ByteArray, RestartURData> restartTxMap : restartRMMap.values()) {
            if (restartTxMap != null && !restartTxMap.isEmpty()) {
                restartMapEmpty = false;
                break;
            }
        }

        return (globalMapEmpty && localMapEmpty && restartMapEmpty);
    }

    /**
     * Class encapsulating data about transactions which RRS will recover.
     */
    private static class RRSTranRecoveryData {
        private final byte[] xidBytes;
        private final byte[] urid;
        private final byte[] uriToken;
        private final byte[] uriRegistryToken;
        @SuppressWarnings("unused")
        private final byte[] urToken;
        private final byte[] ctxToken;
        @SuppressWarnings("unused")
        private final byte[] ctxRegistryToken;
        final int rrsState;
        final boolean isHeuristic;

        private RRSTranRecoveryData(byte[] xidBytes, byte[] urid, byte[] uriToken, byte[] uriRegistryToken, byte[] urToken, byte[] ctxToken, byte[] ctxRegistryToken, int rrsState,
                                    boolean isHeuristic) {
            this.xidBytes = xidBytes;
            this.urid = urid;
            this.uriToken = uriToken;
            this.uriRegistryToken = uriRegistryToken;
            this.urToken = urToken;
            this.ctxToken = ctxToken;
            this.ctxRegistryToken = ctxRegistryToken;
            this.rrsState = rrsState;
            this.isHeuristic = isHeuristic;
        }
    }

    /**
     * Class encapsulating the mock objects that most tests require.
     */
    private static class MockObjectSet {
        private final RRSServices rrsServices;
        private final ContextManager contextManager;
        private final XAResource mockXares;
        private final BundleContext bc;
        private final WsLocationAdmin locationAdmin;
        private final WsResource resource;
        private final OnePhaseXAResource onePhaseXAResource;
        private final BundleContext txMockBundleContext;

        @SuppressWarnings("rawtypes")
        private final ServiceReference xaResourceFactoryRef;

        private MockObjectSet(Mockery mockery) {
            rrsServices = mockery.mock(RRSServices.class);
            contextManager = mockery.mock(ContextManager.class);
            mockXares = mockery.mock(XAResource.class);
            bc = mockery.mock(BundleContext.class);
            locationAdmin = mockery.mock(WsLocationAdmin.class);
            resource = mockery.mock(WsResource.class);
            onePhaseXAResource = mockery.mock(OnePhaseXAResource.class);
            txMockBundleContext = mockery.mock(BundleContext.class, "txMockBundleContext");
            xaResourceFactoryRef = mockery.mock(ServiceReference.class);
        }

        private RRSServices getRRSServices() {
            return rrsServices;
        }

        private ContextManager getContextManager() {
            return contextManager;
        }

        private XAResource getMockXAResource() {
            return mockXares;
        }

        private BundleContext getBundleContext() {
            return bc;
        }

        private WsLocationAdmin getLocationAdmin() {
            return locationAdmin;
        }

        private WsResource getLognameResource() {
            return resource;
        }

        private OnePhaseXAResource getOnePhaseXAResource() {
            return onePhaseXAResource;
        }

        private BundleContext getTxMockBundleContext() {
            return txMockBundleContext;
        }

        @SuppressWarnings("rawtypes")
        private ServiceReference getXaResourceFactoryRef() {
            return xaResourceFactoryRef;
        }
    }
}