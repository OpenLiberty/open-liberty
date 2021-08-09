package com.ibm.ws.objectManager;

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

import com.ibm.ws.objectManager.utils.FFDC;
import com.ibm.ws.objectManager.utils.FFDCImpl;
import com.ibm.ws.objectManager.utils.NLS;
import com.ibm.ws.objectManager.utils.NLSImpl;
import com.ibm.ws.objectManager.utils.Trace;
import com.ibm.ws.objectManager.utils.TraceFactory;
import com.ibm.ws.objectManager.utils.TraceFactoryImpl;
import com.ibm.ws.objectManager.utils.Tracing;

/**
 * <p>ObjectManager a handle to the state of the ObjectManager
 * 
 * @author IBM Corporation
 */
public class ObjectManager
{
    public static final NLS nls = new NLSImpl(ObjectManagerConstants.MSG_BUNDLE);
    public static final TraceFactory traceFactory = new TraceFactoryImpl(nls);
    public static final FFDC ffdc = new FFDCImpl();

    private static final Class cclass = ObjectManager.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(ObjectManager.class,
                                                                     ObjectManagerConstants.MSG_GROUP);

    public static final boolean gatherStatistics = true; // Built for statistics if true.
    public static final boolean testInterfaces = true; // Built with test interfaces enabled. 

    // The states of all ObjectManagers active in this JVM, indexed by logFileName. 
    protected static final java.util.HashMap objectManagerStates = new java.util.HashMap();
    // Persistent and transient state of this ObjectManager.
    protected ObjectManagerState objectManagerState = null;

    /*---------------------- Define the state machine (begin) ----------------------*/
    // States returned by getState().
    /**
     * <code>stateError</code> A state error has occurred.
     * <code>stateOpeningLog</code> Opening the Log file.
     * <code>stateReplayingLog</code> Replaying the log.
     * <code>stateWarmStarted</code> Warm Started.
     * <code>stateColdStarted</code> Cold Started.
     * <code>stateShutdownStarted</code> // Shut down started.
     * <code>stateStopped</code> // Shut down.
     */
    public static final int stateError = ObjectManagerState.stateError;
    public static final int stateOpeningLog = ObjectManagerState.stateOpeningLog;
    public static final int stateReplayingLog = ObjectManagerState.stateReplayingLog;
    public static final int stateWarmStarted = ObjectManagerState.stateWarmStarted;
    public static final int stateColdStarted = ObjectManagerState.stateColdStarted;
    public static final int stateShutdownStarted = ObjectManagerState.stateShutdownStarted;
    public static final int stateStopped = ObjectManagerState.stateStopped;
    /*---------------------- Define the state machine (end) ------------------------*/

    /**
     * Use <code>LOG_FILE_TYPE_FILE</code> to create the ObjectManager with a file based log.
     */
    public static final int LOG_FILE_TYPE_FILE = 0;
    /**
     * Use <code>LOG_FILE_TYPE_NONE</code> to create the ObjectManager with no log.
     */
    public static final int LOG_FILE_TYPE_NONE = 1;
    /**
     * Use <code>LOG_FILE_TYPE_CLEAR</code> to create the ObjectManager with a file based log
     * and empty ObjectStores. All transactions are backed out and then the Object Stores are
     * cleared.
     */
    public static final int LOG_FILE_TYPE_CLEAR = 2;
    public static final String[] logFileTypeNames = { "FILE", "NONE", "CLEAR" };

    /**
     * Create a handle for the ObjectManagerState and initialise it of necessary.
     * 
     * @param logFileName of the transaction log file,
     *            all instances of the ObjectManager must use exactly the same log file name.
     * @throws ObjectManagerException
     */
    public ObjectManager(String logFileName)
        throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "<init>"
                        , logFileName
                            );

        initialise(logFileName, LOG_FILE_TYPE_FILE, null, null);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "<init>"
                            );
    } // ObjectManager().

    /**
     * Create a handle for the ObjectManagerState and initialise it of necessary.
     * 
     * @param logFileName of the transaction log file,
     *            all instances of the ObjectManager must use exactly the same log file name.
     * @param logFileType one of LOG_FILE_TYPE_XXX.
     * @throws ObjectManagerException
     * @deprecated use ObjectManager(logFileName,logFileType,null,null) instead.
     */
    public ObjectManager(String logFileName,
                         int logFileType)
        throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "<init>"
                        , new Object[] { logFileName, new Integer(logFileType), logFileTypeNames[logFileType] }
                            );

        initialise(logFileName, logFileType, null, null);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "<init>"
                            );
    } // ObjectManager().

    /**
     * Create a handle for the ObjectManagerState and initialise it of necessary.
     * 
     * @param logFileName of the transaction log file, all instances of the ObjectManager must use exactly the same log
     *            file name.
     * @param logFileType one of LOG_FILE_TYPE_XXX.
     * @param objectStoreLocations referring ObjectStore name to their disk location, filename.
     *            May be null.
     * @throws ObjectManagerException
     * @deprecated use ObjectManager(logFileName,logFileType,objectStoreLocations,null) instead.
     */
    public ObjectManager(String logFileName,
                         int logFileType,
                         java.util.Map objectStoreLocations)
        throws ObjectManagerException {
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { logFileName,
                                                                new Integer(logFileType),
                                                                logFileTypeNames[logFileType],
                                                                objectStoreLocations });

        initialise(logFileName, logFileType, objectStoreLocations, null);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // ObjectManager().

    /**
     * Create a handle for the ObjectManagerState and initialise it of necessary.
     * 
     * @param logFileName of the transaction log file, all instances of the ObjectManager must use exactly the same log
     *            file name.
     * @param logFileType one of LOG_FILE_TYPE_XXX.
     * @param objectStoreLocations referring ObjectStore name to their disk location, filename.
     *            May be null.
     * @param callbacks an array of ObjectManagerEventCallback to be registered with the ObjectManager
     *            before it starts, may be null.
     * @throws ObjectManagerException
     */
    public ObjectManager(String logFileName,
                         int logFileType,
                         java.util.Map objectStoreLocations,
                         ObjectManagerEventCallback[] callbacks)
        throws ObjectManagerException {
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { logFileName,
                                                                new Integer(logFileType),
                                                                logFileTypeNames[logFileType],
                                                                objectStoreLocations,
                                                                callbacks });

        initialise(logFileName, logFileType, objectStoreLocations, callbacks);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // ObjectManager().

    /**
     * Create a handle for the ObjectManagerState and initialise it of necessary.
     * 
     * @param logFileName of the transaction log file,
     *            all instances of the ObjectManager must use exactly the same log file name.
     * @param logFileType one of LOG_FILE_TYPE_XXX.
     * @param objectStoreLocations to map ObjectStore names to their disk locations.
     * @param callbacks an array of ObjectManagerEventCallback to be registered with the ObjectManager
     *            before it starts, may be null.
     * @throws ObjectManagerException
     */
    protected void initialise(String logFileName,
                              int logFileType,
                              java.util.Map objectStoreLocations,
                              ObjectManagerEventCallback[] callbacks)
                    throws ObjectManagerException {
        final String methodName = "initialise";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { logFileName,
                                      new Integer(logFileType),
                                      objectStoreLocations,
                                      callbacks });

        if (objectStoreLocations == null)
            objectStoreLocations = new java.util.HashMap();

        // Repeat attempts to find or create an ObjectManagerState.
        for (;;) {
            // Look for existing ObjectManagerState.
            synchronized (objectManagerStates) {
                objectManagerState = (ObjectManagerState) objectManagerStates.get(logFileName);
                if (objectManagerState == null) { // None known, so make one.
                    objectManagerState = createObjectManagerState(logFileName,
                                                                  logFileType,
                                                                  objectStoreLocations,
                                                                  callbacks);
                    objectManagerStates.put(logFileName, objectManagerState);
                }
            } // synchronized (objectManagerStates).

            synchronized (objectManagerState) {
                if (objectManagerState.state == ObjectManagerState.stateColdStarted
                    || objectManagerState.state == ObjectManagerState.stateWarmStarted) {
                    // We are ready to go.
                    break;

                } else {
                    // Wait for the ObjectManager state to become usable or terminate. 
                    try {
                        objectManagerState.wait(); // Let some other thread initialise the ObjectManager .  

                    } catch (InterruptedException exception) {
                        // No FFDC Code Needed.
                        ObjectManager.ffdc.processException(cclass, methodName, exception, "1:260:1.28");

                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this, cclass, methodName, exception);
                        throw new UnexpectedExceptionException(this,
                                                               exception);
                    } // catch (InterruptedException exception).
                } // if (objectManagerState.state...
            } // synchronized (objectManagerState)
        } // for (;;).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // initialise().

    /**
     * Instantiate the ObjectManagerState. A subclass of ObjectManager should override this method if a subclass of
     * ObjecManagerState is required.
     * 
     * @param logFileName of the transaction log file.
     * @param logFileType one of LOG_FILE_TYPE_XXX.
     * @param objectStoreLocations referring ObjectStore name to their disk location, filename.
     *            May be null.
     * @param callbacks an array of ObjectManagerEventCallback to be registered with the ObjectManager
     *            before it starts, may be null.
     * 
     * @return ObjectManagerState instantiated.
     * @throws ObjectManagerException
     */
    protected ObjectManagerState createObjectManagerState(String logFileName,
                                                          int logFileType,
                                                          java.util.Map objectStoreLocations,
                                                          ObjectManagerEventCallback[] callbacks)
                    throws ObjectManagerException {
        return new ObjectManagerState(logFileName,
                                      this,
                                      logFileType,
                                      objectStoreLocations,
                                      callbacks);
    } // createObjectManagerState().

    /**
     * returns true if the objectManager was warm started.
     * 
     * @return boolean true if warm started.
     */
    public final boolean warmStarted()
    {
        final String methodName = "warmStarted";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        boolean isWarmStarted = false;
        if (objectManagerState.state == ObjectManagerState.stateWarmStarted)
            isWarmStarted = true;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Boolean(isWarmStarted));
        return isWarmStarted;
    } // warmStarted().

    /**
     * Terminates the ObjectManager.
     * 
     * @throws ObjectManagerException
     */
    public final void shutdown()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "shutdown"
                            );

        objectManagerState.shutdown();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "shutdown"
                            );
    } // End of shutdown.

    /**
     * Terminates the ObjectManager, without taking a checkpoint.
     * The allows the ObjectManager to be restarted as if it had crashed
     * and is only intended for testing emergency restart.
     * 
     * @throws ObjectManagerException
     */
    public final void shutdownFast()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "shutdownFast"
                            );

        if (!testInterfaces) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "shutdownFast"
                           , "via InterfaceDisabledException"
                                );
            throw new InterfaceDisabledException(this
                                                 , "shutdownFast");
        } // if (!testInterfaces).

        objectManagerState.shutdownFast();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "shutdownFast"
                            );
    } // End of shutdownFast.

    /**
     * Waits for one checkpoint to complete.
     * 
     * @throws ObjectManagerException
     */
    public final void waitForCheckpoint()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "waitForCheckpoint"
                            );

        if (!testInterfaces) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "waitForCheckpoint via InterfaceDisabledException"
                                );
            throw new InterfaceDisabledException(this
                                                 , "waitForCheckpoint");
        } // if (!testInterfaces).   

        objectManagerState.waitForCheckpoint(true);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "waitForCheckpoint"
                            );
    } // End of waitForCheckpoint.

    /**
     * @return int the current state for the ObjectManagerState. Defined above.
     * @throws ObjectManagerException
     */
    public int getState()
                    throws ObjectManagerException {
        final String methodName = "getState";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName);

        int stateToReturn = objectManagerState.getObjectManagerStateState();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName, new Object[] { new Integer(stateToReturn),
                                                               ObjectManagerState.stateNames[stateToReturn] });
        return stateToReturn;
    } // getState().

    /*
     * @returns String the name of the log file.
     */
    public String getLogFileName()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "getLogFileSize"
                            );

        String logFileName = objectManagerState.logFileName;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "getLogFileSize"
                       , new Object[] { logFileName });
        return logFileName;
    } // getLogFileName().

    /*
     * @returns long the current Log filesize in bytes.
     * 
     * @throws ObjectManagerException
     */
    public long getLogFileSize()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "getLogFileSize"
                            );

        long logFileSize = objectManagerState.logOutput.getLogFileSize();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "getLogFileSize"
                       , new Object[] { new Long(logFileSize) });
        return logFileSize;
    } // getLogFileSize().

    public long getLogFileSpaceLeft()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "getLogFileSpaceLeft"
                            );

        long logFileSpaceLeft = objectManagerState.logOutput.getLogFileSpaceLeft();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "getLogFileSpaceLeft"
                       , "returns"
                         + " logFileSpaceLeft=" + logFileSpaceLeft + "(long)"
                            );
        return logFileSpaceLeft;
    } // getLogFileSpaceLeft()

    /*
     * @param long the new Log filesize in bytes.
     */
    public void setLogFileSize(long newSize)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "seLogFileSize"
                            );

        objectManagerState.logOutput.setLogFileSize(newSize);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "setLogFileSize"
                            );
    } // setLogFileSize().

    /*
     * When enabled writes to the log will throw LogFileFullException.
     * 
     * @param boolean if true subsequent writes to the log throw LogFileFullException.
     * if false subsequent writes may succeed.
     */
    public void simulateLogOutputFull(boolean isFull)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "simulateLogOutputFull"
                        , "isFull=" + isFull + "(boolean)"
                            );

        if (!testInterfaces) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "simulateLogOutputFull"
                           , "via InterfaceDisabledException"
                                );
            throw new InterfaceDisabledException(this
                                                 , "simulateLogOutputFull");
        } // if (!testInterfaces).   

        ((FileLogOutput) objectManagerState.logOutput).simulateLogOutputFull(isFull);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "simulateLogOutputFull"
                            );
    } // simulateLogFull().

    /**
     * Factory method to crate a new transaction for use with the ObjectManager.
     * 
     * @return Transaction the new transaction.
     * @throws ObjectManagerException
     */
    public final Transaction getTransaction()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "getTransaction"
                            );
        // If the log is full introduce a delay for a checkpoiunt before allowing the 
        // application to proceed.
        objectManagerState.transactionPacing();
        Transaction transaction = objectManagerState.getTransaction();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "getTransaction"
                       , "returns transaction=" + transaction + "(Transaction)"
                            );
        return transaction;
    } // getTransaction().

    /**
     * Locate a transaction registered with this ObjectManager.
     * with the same XID as the one passed.
     * If a null XID is passed this will return any registered transaction with a null XID.
     * 
     * @param XID Xopen identifier.
     * @return Transaction identified by the XID.
     * @throws ObjectManagerException
     */
    public final Transaction getTransactionByXID(byte[] XID)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "getTransactionByXID"
                        , "XIDe=" + XID + "(byte[]"
                            );

        Transaction transaction = objectManagerState.getTransactionByXID(XID);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "getTransactionByXID"
                       , "returns transaction=" + transaction + "(Transaction)"
                            );
        return transaction;
    } // End of method getTransactionByXID.

    /**
     * Create an iterator over all transactions known to this ObjectManager.
     * The iterator returned is safe against concurrent modification of the set of transactions,
     * new transactions created after the iterator is created may not be covered by the iterator.
     * 
     * @return java.util.Iterator over Transactions.
     * @throws ObjectManagerException
     */
    public final java.util.Iterator getTransactionIterator()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "getTransactionIterator"
                            );
        java.util.Iterator transactionIterator = objectManagerState.getTransactionIterator();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "getTransactionIterator"
                       , "returns transactionIterator" + transactionIterator + "(java.util.Iterator)"
                            );
        return transactionIterator;
    } // End of method getTransactionIterator.

    /**
     * Locate an ObjectStore used by this objectManager.
     * 
     * @param objectStoreName of the ObjectStore.
     * @return ObjectStore found matching the identifier.
     * @throws ObjectManagerException
     */
    public final ObjectStore getObjectStore(String objectStoreName)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "getObjectStore"
                        , "objectStoreName=" + objectStoreName + "(String)"
                            );

        ObjectStore objectStore = objectManagerState.getObjectStore(objectStoreName);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "getObjectStore"
                       , "returns objectStore=" + objectStore + "(ObjectStore)"
                            );
        return objectStore;
    } // End of method getObjectStore.

    /**
     * Create an iterator over all ObjectStores known to this ObjectManager.
     * 
     * @return java.util.Iterator over Transactions.
     * @throws ObjectManagerException
     */
    public final java.util.Iterator getObjectStoreIterator()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "getObjectStoreIterator"
                            );
        java.util.Iterator objectStoreIterator = objectManagerState.getObjectStoreIterator();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "getObjectStoreIterator"
                       , new Object[] { objectStoreIterator }
                            );
        return objectStoreIterator;
    } // getObjectStoreIterator().

    /**
     * Print a dump of the state.
     * 
     * @param printWriter to be written to.
     */
    public void print(java.io.PrintWriter printWriter)
    {
        printWriter.println("ObjectManager"
                            + "(String)");
        printWriter.println();
        objectManagerState.logOutput.print(printWriter);
    } // print().

    /**
     * Create a named ManagedObject by name within this objectManager.
     * 
     * @param name The name of the ManagedObject to be added.
     * @param token of the named ManagedObject.
     * @param transaction The transaction which scopes the naming.
     * @return Token of any existing ManagedObject associated with this name or null.
     * @throws ObjectManagerException
     */
    public final Token putNamedObject(String name
                                      , Token token
                                      , Transaction transaction
                    )
                                    throws ObjectManagerException
    {
        final String methodName = "putNamedObject";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , methodName
                        , new Object[] { name, token, transaction }
                            );

        Token tokenOut = null; // For return.

        // See if there is a definitive namedObjdects tree. 
        if (objectManagerState.namedObjects == null) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , methodName
                           , "via NoRestartablebjectStoresAvailableException"
                                );
            throw new NoRestartableObjectStoresAvailableException(this);
        } // if (objectManagerState.namedObjects == null).

        // Loop over all of the ObjectStores.
        java.util.Iterator objectStoreIterator = objectManagerState.objectStores.values().iterator();
        while (objectStoreIterator.hasNext()) {
            ObjectStore objectStore = (ObjectStore) objectStoreIterator.next();

            // Don't bother with ObjectStores that can't be used for recovery.
            if (objectStore.getContainsRestartData()) {

                // Locate any existing copy.
                Token namedObjectsToken = new Token(objectStore,
                                                    ObjectStore.namedObjectTreeIdentifier.longValue());
                // Swap for the definitive Token, if there is one.
                namedObjectsToken = objectStore.like(namedObjectsToken);

                TreeMap namedObjectsTree = (TreeMap) namedObjectsToken.getManagedObject();
                tokenOut = (Token) namedObjectsTree.put(name
                                                        , token
                                                        , transaction
                                );

            } // if (objectStore.getContainsRestartData()).
        } // While objectStoreIterator.hasNext().  

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , methodName
                       , new Object[] { tokenOut });
        return tokenOut;
    } // putNamedObject().

    /**
     * Locate a Token by name within this objectManager.
     * 
     * @param name The name of the Token to be located.
     * @param transaction controlling visibility of the named ManagedObject.
     * @return Token of the named ManagedObject or null.
     * @throws ObjectManagerException
     */
    public final Token getNamedObject(String name
                                      , Transaction transaction)
                    throws ObjectManagerException
    {
        final String methodName = "getNamedObject";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , methodName
                        , new Object[] { name, transaction }
                            );

        // Is the definitive tree assigned?
        if (objectManagerState.namedObjects == null) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , methodName
                           , "via NoRestartableObjectStoresAvailableException"
                                );
            throw new NoRestartableObjectStoresAvailableException(this);
        } // if (objectManagerState.namedObjects == null).

        TreeMap namedObjectsTree = (TreeMap) objectManagerState.namedObjects.getManagedObject();
        Token token = (Token) namedObjectsTree.get(name, transaction);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , methodName
                       , new Object[] { token }
                            );
        return token;
    } // getNamedObject().

    /**
     * Remove a named ManagedObject locatable by name within this objectManager.
     * 
     * @param name The name of the ManagedObject to be removed.
     * @param transaction The transaction which scopes the naming.
     * @return Token of any existing ManagedObject associated with this name or null.
     * @throws ObjectManagerException
     */
    public final Token removeNamedObject(String name
                                         , Transaction transaction)
                    throws ObjectManagerException {
        final String methodName = "removeNamedObject";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , methodName
                        , new Object[] { name, transaction }
                            );

        Token tokenOut = null; // For return.

        // See if there is a definitive namedObjdects tree. 
        if (objectManagerState.namedObjects == null) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , methodName
                           , "via NoRestartablebjectStoresAvailableException"
                                );
            throw new NoRestartableObjectStoresAvailableException(this);
        } // if (objectManagerState.namedObjects == null).

        // Loop over all of the ObjectStores.
        java.util.Iterator objectStoreIterator = objectManagerState.objectStores.values().iterator();
        while (objectStoreIterator.hasNext()) {
            ObjectStore objectStore = (ObjectStore) objectStoreIterator.next();

            // Don't bother with ObjectStores that can't be used for recovery.
            if (objectStore.getContainsRestartData()) {

                // Locate any existing copy.
                Token namedObjectsToken = new Token(objectStore,
                                                    ObjectStore.namedObjectTreeIdentifier.longValue());
                // Swap for the definitive Token, if there is one.
                namedObjectsToken = objectStore.like(namedObjectsToken);

                TreeMap namedObjectsTree = (TreeMap) namedObjectsToken.getManagedObject();
                tokenOut = (Token) namedObjectsTree.remove(name, transaction);

            } // if (objectStore.getContainsRestartData()).
        } // While objectStoreIterator.hasNext().

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , methodName
                       , new Object[] { tokenOut });
        return tokenOut;
    } // removeNamedObject().

    /**
     * @return Collection of named object names.
     * @throws ObjectManagerException
     */
    public final Collection getNamedObjectNames()
                    throws ObjectManagerException
    {
        final String methodName = "getNamedObjectNames";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , methodName);

        // Is the definitive tree assigned?
        if (objectManagerState.namedObjects == null) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , methodName
                           , "via NoRestartableObjectStoresAvailableException"
                                );
            throw new NoRestartableObjectStoresAvailableException(this);
        } // if (objectManagerState.namedObjects == null).

        TreeMap namedObjectsTree = (TreeMap) objectManagerState.namedObjects.getManagedObject();
        Collection namedObjectNames = namedObjectsTree.keyCollection();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , methodName
                       , new Object[] { namedObjectNames }
                            );
        return namedObjectNames;
    } // getNamedObjectNames().

    /**
     * Create a named ManagedObject by name within this objectManager.
     * 
     * @param persistentTransactionsPerCheckpoint the number of persistent transactions that can commit or backout before
     *            a checkpoint is written in the log. A high number results in long restart times a low number impacts
     *            runtime performance.
     * @param nonPersistentTransactionsPerCheckpoint the number of transactions that can commit or rollback before disk
     *            based objects stores are written to disk. A high number may result in high memory usage, a low number may
     *            result in excessive disk activity.
     * @param transaction controling the update.
     * @throws ObjectManagerException
     */
    public final void setTransactionsPerCheckpoint(long persistentTransactionsPerCheckpoint
                                                   , long nonPersistentTransactionsPerCheckpoint
                                                   , Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "setTransactionsPerCheckpoint"
                        , new Object[] { new Long(persistentTransactionsPerCheckpoint)
                                        , new Long(nonPersistentTransactionsPerCheckpoint)
                                        , transaction }
                            );

        transaction.lock(objectManagerState);
        objectManagerState.persistentTransactionsPerCheckpoint = persistentTransactionsPerCheckpoint;
        objectManagerState.nonPersistentTransactionsPerCheckpoint = nonPersistentTransactionsPerCheckpoint;
        // saveClonedState does not update the defaultStore.
        transaction.replace(objectManagerState);
        // Save the updates in the restartable ObjectStores. 
        objectManagerState.saveClonedState(transaction);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "setTransactionsPerCheckpoint"
                            );
    } // setTransactionsPerCheckpoint().

    /**
     * @param transaction controling visibility.
     * @return long the maximumActiveTransactions.
     * 
     */
    protected long getMaximumActiveTransactions(Transaction transaction)
    {
        return objectManagerState.maximumActiveTransactions;
    }

    /**
     * Change the maximum active transactrions that the ObjectManager will allow to start. If this call reduces the
     * maximum then existing transactions continue but no new ones are allowed until the total has fallen below the new
     * maximum.
     * 
     * @param maximumActiveTransactions to set.
     * @param transaction controling the update.
     * @throws ObjectManagerException
     */
    public final void setMaximumActiveTransactions(int maximumActiveTransactions
                                                   , Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "setMaximumActiveTransactions"
                        , new Object[] { new Integer(maximumActiveTransactions)
                                        , transaction }
                            );

        transaction.lock(objectManagerState);
        objectManagerState.maximumActiveTransactions = maximumActiveTransactions;
        // saveClonedState does not update the defaultStore.
        transaction.replace(objectManagerState);
        // Save the updates in the restartable ObjectStores. 
        objectManagerState.saveClonedState(transaction);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "setMaximumActiveTransactions"
                            );
    } // setMaximumActiveTransactions().

    /*
     * Write a LogRecord to the transaction log.
     * 
     * @param logRecord to be written.
     * 
     * @param reservedDelta the change in the number of bytes reserved in the log
     * additional to the length of the logRecord itself.
     * 
     * @param flush true if the logRecord must be written to disk before we return.
     * 
     * @returns long the logSequenceNumber of the LogRecord written.
     * 
     * @deprecated see writeLogRecord(logRecord,boolean);
     */
    public long writeLogRecord(LogRecord logRecord,
                               long reservedDelta,
                               boolean flush)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "writeLogRecord"
                        , new Object[] { logRecord, new Long(reservedDelta), new Boolean(flush) }
                            );

        long logSequenceNumber = objectManagerState.logOutput.writeNext(logRecord
                                                                        , reservedDelta
                                                                        , true
                                                                        , flush
                        );

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "writeLogRecord"
                       , new Object[] { new Long(logSequenceNumber) }
                            );
        return logSequenceNumber;
    } // writeLogRecord().

    /*
     * Write a LogRecord to the transaction log.
     * 
     * @returns long the logSequenceNumber of the LogRecord written.
     * 
     * @deprecated see writeLogRecord(logRecord,boolean);
     */
    public long writeLogRecord(LogRecord logRecord)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "writeLogRecord"
                            );

        long logSequenceNumber = objectManagerState.logOutput.writeNext(logRecord
                                                                        , 0
                                                                        , true
                                                                        , false
                        );

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "writeLogRecord"
                       , "returns logSequenceNumber=" + logSequenceNumber + "(long)"
                            );
        return logSequenceNumber;
    } // method writeLogRecord().

    /*
     * Forces spooled output to hardened storage.
     * 
     * @deprecated see writeLogRecord(logRecord,Boolean);
     */
    public void flushLog()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this
                        , cclass
                        , "flushLog"
                            );

        ((FileLogOutput) objectManagerState.logOutput).flush();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "flushLog"
                            );
    } // method flushLog().

    /**
     * Capture statistics.
     * 
     * @param name The name of the component to capture statistics from.
     * @return java.util.Map the captured statistics.
     * @throws ObjectManagerException
     */
    public java.util.Map captureStatistics(String name
                    )
                                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "captureStatistics",
                        new Object[] { name });

        java.util.Map statistics = new java.util.HashMap(); // To be returned.

        synchronized (objectManagerState) {
            if (!(objectManagerState.state == ObjectManagerState.stateColdStarted)
                && !(objectManagerState.state == ObjectManagerState.stateWarmStarted)) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "addEntry"
                               , new Object[] { new Integer(objectManagerState.state), ObjectManagerState.stateNames[objectManagerState.state] }
                                    );
                throw new InvalidStateException(this, objectManagerState.state, ObjectManagerState.stateNames[objectManagerState.state]);
            }

            if (name.equals("*")) {
                statistics.putAll(objectManagerState.logOutput.captureStatistics());
                statistics.putAll(captureStatistics());

            } else if (name.equals("LogOutput")) {
                statistics.putAll(objectManagerState.logOutput.captureStatistics());

            } else if (name.equals("ObjectManager")) {
                statistics.putAll(captureStatistics());

            } else {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "captureStatistics",
                               new Object[] { name });
                throw new StatisticsNameNotFoundException(this,
                                                          name);
            } // if (name.equals...
        } // synchronized (objectManagerState).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "captureStatistics"
                       , statistics
                            );
        return statistics;
    } // End of method captureStatistics().

    /*
     * Builds a set of properties containing the current statistics.
     * 
     * @return java.util.Map the statistics.
     */
    private java.util.Map captureStatistics()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "captureStatistics"
                            );

        java.util.Map statistics = new java.util.HashMap();
        statistics.putAll(objectManagerState.captureStatistics());

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "captureStatistics"
                       , statistics
                            );
        return statistics;
    } // method captureStatistics().

    // Defect 495856, 496893
    public void registerEventCallback(ObjectManagerEventCallback callback)
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, "registerEventCallback", callback);

        objectManagerState.registerEventCallback(callback);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, "registerEventCallback");
    }
} // class ObjectManager.
