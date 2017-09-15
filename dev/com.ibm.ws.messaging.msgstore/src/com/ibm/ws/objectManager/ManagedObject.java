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

import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;

import com.ibm.ws.objectManager.utils.Trace;
import com.ibm.ws.objectManager.utils.Tracing;

/**
 * <p>Objects managed by the resource manager (ObjectManager)
 * subclass ManagedObject.
 * The objects that subclass managed object are serializable and capable of being
 * stored in an object store under transactional control.
 * 
 * Classes extending managed object may choose to implement simplified serialization.
 * ManagedObject implements the SimplifiedSerialization methods but does not declare
 * implements SimplifiedSerialization.
 * 
 * @author IBM Corporation
 */

public abstract class ManagedObject
                implements java.io.Serializable
                , java.lang.Cloneable
{
    private static final Class cclass = ManagedObject.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_OBJECTS);
    private static final long serialVersionUID = 1212101998694878938L;

    /*---------------------- Define the state machine (begin) ----------------------*/
    /**
     * <code>stateError</code> StateErrorExceeption has been thrown,
     * no more operations on the ManagdObject are allowed.
     * <code>stateConstructed</code> the ManagedObject has never been part of a transaction.
     * <code>stateAdded</code> The ManagedObject is being added as part of a transaction.
     * <code>stateLocked</code> The ManagedObject is locked as part of a transaction.
     * <code>stateReplaced</code> The ManagedObject is being updated as part of a transaction.
     * <code>stateToBeDeleted</code> The ManagedObject will be deleted if the transaction commits.
     * <code>stateMustBeDeleted</code> The ManagedObject will be deleted regardless,
     * of whether the transactions commits or backs out.
     * <code>stateDeleted</code> The ManagedObject has been deleted and is not part of a transaction.
     * <code>stateReady</code> Tthe ManagedObject is not part of a transaction.
     */

    // Tracks the lifecycle of the object.
    public static final int stateError = 0; // A state error has occured.
    public static final int stateConstructed = 1; // Not yet part of a transaction.
    public static final int stateAdded = 2; // Added.
    public static final int stateLocked = 3; // Intent to replace.
    public static final int stateReplaced = 4; // Replaced.
    public static final int stateToBeDeleted = 5; // ToBeDeleted. 
    public static final int stateMustBeDeleted = 6; // Will be deleted regardless of the transaction outcome.
    public static final int stateDeleted = 7; // Deleted.
    public static final int stateReady = 8; // Ready for next action.

    // The names of the states for diagnostic purposes.
    static final String stateNames[] = { "Error"
                                        , "Constructed"
                                        , "Added"
                                        , "Locked"
                                        , "Replaced"
                                        , "ToBeDeleted"
                                        , "MustBeDeleted"
                                        , "Deleted"
                                        , "Ready"
    };

    // What happens when this ManagedObject becomes added within a transaction.
    static final int nextStateForAdd[] = { stateError
                                          , stateAdded
                                          , stateError
                                          , stateError
                                          , stateError
                                          , stateError
                                          , stateError
                                          , stateError
                                          , stateError // Allow transition from Ready during recovery.
    };

    // What happens when this ManagedObject becomes locked within a transaction.
    static final int nextStateForLock[] = { stateError
                                           , stateConstructed
                                           , stateAdded
                                           , stateLocked
                                           , stateReplaced
                                           , stateToBeDeleted
                                           , stateMustBeDeleted
                                           , stateError
                                           , stateLocked
    };

    // What happens when this ManagedObject becomes un locked within a transaction.
    static final int nextStateForUnlock[] = { stateError
                                             , stateConstructed
                                             , stateAdded
                                             , stateReady
                                             , stateError
                                             , stateToBeDeleted
                                             , stateMustBeDeleted
                                             , stateDeleted
                                             , stateReady
    };

    // What happens when this ManagedObject becomes replaced within a transaction.
    // Existing objects must be locked before they can be replaced.
    static final int nextStateForReplace[] = { stateError
                                              , stateError
                                              , stateAdded
                                              , stateReplaced
                                              , stateReplaced
                                              , stateError
                                              , stateError
                                              , stateError
                                              , stateError
    };

    // What happens when this ManagedObject is optimitically replaced within a transaction.
    static final int nextStateForOptimisticReplace[] = { stateError
                                                        , stateConstructed // Because clone() in linked list does not add() the cloned list.
                                                        , stateAdded
                                                        , stateLocked
                                                        , stateReplaced
                                                        , stateToBeDeleted
                                                        , stateMustBeDeleted
                                                        , stateDeleted
                                                        , stateReady
    };

    // What happens when this ManagedObject becomes deleted within a transaction.
    static final int nextStateForDelete[] = { stateError
                                             , stateError
                                             , stateMustBeDeleted
                                             , stateToBeDeleted
                                             , stateToBeDeleted
                                             , stateToBeDeleted // if replayed during recovery of checkpoint.
                                             , stateToBeDeleted // if replayed during recovery of checkpoint.
                                             , stateError
                                             , stateToBeDeleted
    };

    // What happens when this ManagedObject commits its transaction.
    static final int nextStateForCommit[] = { stateError
                                             , stateError
                                             , stateReady
                                             , stateReady
                                             , stateReady
                                             , stateDeleted
                                             , stateDeleted
                                             , stateError
                                             , stateError // Should not have been included in the transaction.     
    };

    // What happens when this OptimisticReplaced ManagedObject commits its transaction.
    static final int nextStateForOptimisticReplaceCommit[] = { stateError
                                                              , stateError
                                                              , stateAdded
                                                              , stateLocked
                                                              , stateReplaced
                                                              , stateToBeDeleted
                                                              , stateMustBeDeleted
                                                              , stateDeleted
                                                              , stateReady
    };

    // What happens when this ManagedObject backs out back its transaction.
    static final int nextStateForBackout[] = { stateError
                                              , stateError
                                              , stateDeleted
                                              , stateReady
                                              , stateReady
                                              , stateReady
                                              , stateDeleted
                                              , stateError
                                              , stateError // Should not have been included in the transaction.  
    };

    // What happens when this OptimisticReplaced ManagedObject backs out its transaction.
    static final int nextStateForOptimisticReplaceBackout[] = { stateError
                                                               , stateError
                                                               , stateAdded
                                                               , stateLocked
                                                               , stateReplaced
                                                               , stateToBeDeleted
                                                               , stateMustBeDeleted
                                                               , stateDeleted
                                                               , stateReady
    };

    protected transient int state; // The current state of the ManagedObject.
    // The previous state, not refereneced but will appear in a dump.
    private transient int previousState;
    // Lock for the above states. 
    private transient Object stateLock = new Object();
    /*---------------------- Define the state machine (end) ------------------------*/

    // The Token which owns this ManagedObject.
    protected transient Token owningToken;
    // Container for the transaction to which this object is locked.
    private transient TransactionLock transactionLock = new TransactionLock(null);
    // Incremented each time anotherLock is taken under the same transaction.
    private transient int numberOfLocksTaken = 0;
    private transient int threadsWaitingForLock = 0;
    //TODO May be can delete the following and use serialized bytes and the ObjectStore version instead?
    //      except for non persistent objects!
    protected transient ManagedObject beforeImmage = null;
    // Allows becomeCloneOf to determine whether it is being driven at backout.
    protected transient boolean backingOut = false;
    // The updateSequence for this ManagedObject when it was last commited as part of a transaction
    // or when the log was forced as part of a checkpoint. 
    // This is the version or the Object that can safely be written to the ObjectStore. 
    private transient long forcedUpdateSequence = 0;
    // These bytes are given to the ObjectStore, when the current contents in the
    // store can be safely updated.
    private transient ObjectManagerByteArrayOutputStream latestSerializedBytes;
    // The maximum size of any of the versions of the serialized ManagedObject to be written
    // to the ObjectStore. This includes any versions already written. It is reset to zero 
    // once we write the final version of the ManagedObject.
    transient int latestSerializedSize = 0;
    transient int latestSerializedSizeDelta = 0;
    // Lock for the above serialized bytes and size. 
    private transient ForcedUpdateSequenceLock forcedUpdateSequenceLock = new ForcedUpdateSequenceLock();

    private class ForcedUpdateSequenceLock {}

    // The number of times the object has been serialized since it was loaded.
    private transient long updateSequence = 0;
    // The index in the ObjectStore cache, if any.
    protected transient int objectStoreCacheIndex;

    private static final Hashtable _genericConstructors = new Hashtable(10);

    /**
     * Default constructor
     */
    public ManagedObject()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this
                        , cclass
                        , "<init>"
                            );
        state = stateConstructed; // Not yet part of a transaction. 
        previousState = -1; // No previous state.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this
                       , cclass
                       , "<init>"
                       , "state=" + state + "(int) " + stateNames[state] + "(String)"
                            );
    } // End of default Constructor().

    /**
     * Convert serialized bytes back into a managed object.
     * 
     * @param serializedBytes the bytes that have been formed by previously serializing
     *            the ManagedObject.
     * @param objectManagerState of the objectManager reconstructing the ManagedObject.
     * @return ManagedObject that is deserialized.
     * @throws ObjectManagerException
     */
    protected static final ManagedObject restoreFromSerializedBytes(byte serializedBytes[],
                                                                    ObjectManagerState objectManagerState)
                    throws ObjectManagerException {
        String methodName = "restoreFromSerializedBytes";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(cclass,
                        methodName,
                        new Object[] { serializedBytes,
                                      objectManagerState });

        ManagedObject managedObjectToReturn = null;

        java.io.ByteArrayInputStream byteArrayInputStream = new java.io.ByteArrayInputStream(serializedBytes);

        // Discover what to deserialize and how to deserialize it.
        int objectSignature;
        java.io.DataInputStream dataInputStream = new java.io.DataInputStream(byteArrayInputStream);
        try {
            objectSignature = dataInputStream.readInt();

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(cclass, "restoreFromSerializedBytes", exception, "1:310:1.34");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(cclass,
                           methodName,
                           exception);
            throw new PermanentIOException(cclass,
                                           exception);
        } // catch.
        switch (objectSignature) {
            case SimplifiedSerialization.signature_DefaultSerialization:
                managedObjectToReturn = restoreSerializedDefault(byteArrayInputStream, objectManagerState);
                break;

            case SimplifiedSerialization.signature_ConcurrentSublist_Link:
                managedObjectToReturn = new ConcurrentSubList.Link();
                ((ConcurrentSubList.Link) managedObjectToReturn).readObject(dataInputStream
                                                                            , objectManagerState
                                );
                break;

            case SimplifiedSerialization.signature_ConcurrentSublist:
                managedObjectToReturn = new ConcurrentSubList();
                ((ConcurrentSubList) managedObjectToReturn).readObject(dataInputStream
                                                                       , objectManagerState
                                );
                break;

            case SimplifiedSerialization.signature_LinkedList_Link:
                managedObjectToReturn = new LinkedList.Link();
                ((LinkedList.Link) managedObjectToReturn).readObject(dataInputStream
                                                                     , objectManagerState
                                );
                break;

            case SimplifiedSerialization.signature_LinkedList:
                managedObjectToReturn = new LinkedList();
                ((LinkedList) managedObjectToReturn).readObject(dataInputStream
                                                                , objectManagerState
                                );
                break;

            case SimplifiedSerialization.signature_TreeMap:
                managedObjectToReturn = new TreeMap();
                ((TreeMap) managedObjectToReturn).readObject(dataInputStream
                                                             , objectManagerState
                                );
                break;

            case SimplifiedSerialization.signature_TreeMap_Entry:
                managedObjectToReturn = new TreeMap.Entry();
                ((TreeMap.Entry) managedObjectToReturn).readObject(dataInputStream
                                                                   , objectManagerState
                                );
                break;

            case SimplifiedSerialization.signature_ConcurrentLinkedList:
                managedObjectToReturn = new ConcurrentLinkedList();
                ((ConcurrentLinkedList) managedObjectToReturn).readObject(dataInputStream
                                                                          , objectManagerState
                                );
                break;

            case SimplifiedSerialization.signature_ObjectManagerState:
                managedObjectToReturn = new ObjectManagerState();
                ((ObjectManagerState) managedObjectToReturn).readObject(dataInputStream
                                                                        , objectManagerState
                                );
                break;

            case SimplifiedSerialization.signature_Generic:
                final String className;
                try
                {
                    className = dataInputStream.readUTF();
                } catch (java.io.IOException exception)
                {
                    // No FFDC Code Needed.
                    ObjectManager.ffdc.processException(cclass, methodName, exception, "1:390:1.34");
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(cclass, methodName, exception);
                    throw new PermanentIOException(cclass, exception);
                } // catch. 

                try
                {
                    Constructor constructor = (Constructor) _genericConstructors.get(className);
                    if (constructor == null)
                    {
                        // Defect 609434
                        // If we need a constructor then create one in a doPrivileged block
                        // to allow us to set access to the constructor with system level
                        // privileges.
                        constructor = (Constructor) AccessController.doPrivileged(new PrivilegedExceptionAction()
                        {
                            public Object run() throws Exception
                            {
                                Class classToInstantiate = Class.forName(className);
                                Constructor retval = classToInstantiate.getDeclaredConstructor(new Class[0]);
                                retval.setAccessible(true);
                                _genericConstructors.put(className, retval);
                                return retval;
                            }
                        });
                    }
                    managedObjectToReturn = (ManagedObject) constructor.newInstance(new Object[0]);
                } catch (java.security.PrivilegedActionException exception)
                {
                    // No FFDC Code Needed.
                    Throwable cause = exception.getCause();
                    if (cause instanceof java.lang.ClassNotFoundException)
                    {
                        ObjectManager.ffdc.processException(cclass, methodName, cause, "1:424:1.34");
                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(cclass, methodName, cause);
                        throw new com.ibm.ws.objectManager.ClassNotFoundException(cclass, (java.lang.ClassNotFoundException) cause);
                    }
                    else if (cause instanceof Exception)
                    {
                        ObjectManager.ffdc.processException(cclass, methodName, cause, "1:430:1.34");
                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(cclass, methodName, cause);
                        throw new UnexpectedExceptionException(cclass, (Exception) cause);
                    }
                    else
                    {
                        ObjectManager.ffdc.processException(cclass, methodName, cause, "1:436:1.34");
                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(cclass, methodName, cause);
                        throw (Error) cause;
                    }
                } catch (Exception exception)
                {
                    // No FFDC Code Needed.
                    ObjectManager.ffdc.processException(cclass, methodName, exception, "1:444:1.34");
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(cclass, methodName, "via UnexpectedExceptionException");
                    throw new UnexpectedExceptionException(cclass, exception);
                } // catch.  

                // readObject is allowed to Throw java.io.IOException because it is a public interface
                // and may not want to throw ObjectManagerException. 
                try {
                    ((SimplifiedSerialization) managedObjectToReturn).readObject(dataInputStream,
                                                                                 objectManagerState);
                } catch (java.io.IOException exception) {
                    // No FFDC Code Needed.
                    ObjectManager.ffdc.processException(cclass,
                                                        methodName,
                                                        exception,
                                                        "1:459:1.34");

                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(cclass,
                                   methodName,
                                   "via PermanentIOException");
                    throw new PermanentIOException(cclass, exception);
                } // try. 
                break;

            default:
                ObjectSignatureNotFoundException objectSignatureNotFoundException = new ObjectSignatureNotFoundException(cclass, objectSignature);
                ObjectManager.ffdc.processException(cclass, methodName, objectSignatureNotFoundException, "1:471:1.34");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
                    trace.bytes(cclass,
                                serializedBytes,
                                0, Math.min(serializedBytes.length, 1000)
                                    );
                    trace.exit(cclass,
                               methodName,
                               objectSignatureNotFoundException);
                } // if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()).

                throw objectSignatureNotFoundException;
        } // Switch (objectSignature).                        

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(cclass,
                       methodName,
                       new Object[] { managedObjectToReturn });
        return managedObjectToReturn;
    } // restoreFromSerializedBytes().

    /**
     * Convert serialized bytes back into a managed object.
     * 
     * @param byteArrayInputStream from which the serializable ManagedObject
     *            is to be read.
     * @param objectManagerState of the objectManager reconstructing the ManagedObject.
     * @return ManagedObject that is deserialized.
     * @throws ObjectManagerException
     */
    protected static final ManagedObject restoreSerializedDefault(java.io.ByteArrayInputStream byteArrayInputStream,
                                                                  ObjectManagerState objectManagerState)
                    throws ObjectManagerException {
        final String methodName = "restoreSerializedDefault";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(cclass,
                        methodName,
                        new Object[] { byteArrayInputStream,
                                      objectManagerState });

        ManagedObject managedObjectToReturn = null;

        try {
            // This type of ObjectInputStream ensures that any Tokens referenced by the ManagedObject will
            // be replaced with any equivalent Tokens already in memory.   

            ManagedObjectInputStream objectInputStream = new ManagedObjectInputStream(byteArrayInputStream
                                                                                      , objectManagerState
                            );
            managedObjectToReturn = (ManagedObject) objectInputStream.readObject();

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(cclass, methodName, exception, "1:524:1.34");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(cclass,
                           methodName,
                           exception);
            throw new PermanentIOException(cclass,
                                           exception);

        } catch (java.lang.ClassNotFoundException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(cclass, methodName, exception, "1:535:1.34");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(cclass,
                           methodName,
                           exception);
            throw new com.ibm.ws.objectManager.ClassNotFoundException(cclass, exception);
        } // catch java.lang.ClassNotFoundException.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(cclass,
                       methodName,
                       managedObjectToReturn);
        return managedObjectToReturn;
    } // restoreSerializedDefault().

    /**
     * @return Token for this ManagedObject. This may be used to get back the full ManagedObject.
     */
    public final Token getToken()
    {
        return owningToken;
    } // getToken().

    /**
     * @return long the current ObjectManagerState transactionUnlockSequence
     * @throws NullPointerException if the ManagedObject is not allocated to an ObjectStore.
     */
    protected final long getTransactionUnlockSequence()
    {
        return owningToken.objectStore.getObjectManagerState().getGlobalTransactionUnlockSequence();
    }

    /**
     * Get the serialized bytes representing this ManagedObject,
     * these will be given to the logger and potentially to the ObjectStore.
     * 
     * @return ObjectManagerByteArrayOutputStream containing the serialized bytes currently
     *         representing this ManagedObject.
     * @throws ObjectManagerException
     */
    protected ObjectManagerByteArrayOutputStream getSerializedBytes()
                    throws ObjectManagerException
    {
        final String methodName = "getSerializedBytes";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        // The serialized image of the ManagedObject that will become the commited version.
        // The objectStore and log keep their own reference to the bytes so any change must replace 
        // this buffer with another one and not update it in place.
        // These bytes are the ones written to the log, before the transaction outcome is known.

        ObjectManagerByteArrayOutputStream byteArrayOutputStream;
        long estimatedLength;

        try {
            if (this instanceof SimplifiedSerialization) {
                estimatedLength = estimatedLength();
                byteArrayOutputStream = owningToken.objectStore.getObjectManagerState()
                                .getbyteArrayOutputStreamFromPool((int) estimatedLength);
                java.io.DataOutputStream dataOutputStream = new java.io.DataOutputStream(byteArrayOutputStream);
                int signature = getSignature();
                dataOutputStream.writeInt(signature);
                if (signature == SimplifiedSerialization.signature_Generic) {
                    dataOutputStream.writeUTF(getClass().getName());
                } // if (signature == SimplifiedSerialization.signature_Generic).

                ((SimplifiedSerialization) this).writeObject(dataOutputStream);

            } else {
                estimatedLength = 1024;
                byteArrayOutputStream = owningToken.objectStore.getObjectManagerState()
                                .getbyteArrayOutputStreamFromPool((int) estimatedLength);
                byteArrayOutputStream.writeInt(SimplifiedSerialization.signature_DefaultSerialization);

                java.io.ObjectOutputStream objectOutputStream = new java.io.ObjectOutputStream(byteArrayOutputStream);
                objectOutputStream.writeObject(this);
                objectOutputStream.close();
            }
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:619:1.34");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           exception);
            throw new PermanentIOException(this, exception);
        } // catch.

        reserveSpaceInStore(byteArrayOutputStream);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { byteArrayOutputStream,
                                     new Long(updateSequence),
                                     new Integer(byteArrayOutputStream.size()),
                                     new Long(estimatedLength) });
        return byteArrayOutputStream;
    } // getSerializedBytes().

    /**
     * @param byteArrayOutputStream containing the serialized bytes for which space is to be reserved.
     * @throws ObjectManagerException
     */
    void reserveSpaceInStore(ObjectManagerByteArrayOutputStream byteArrayOutputStream)
                    throws ObjectManagerException {
        // Adjust the space reserved in the ObjectStore to reflect what we just serialized
        // and will eventually give to the ObjectStore.
        // We reserve the largest size even the ManagedObject may have become smaller because
        // there may be a larger version of this Object still about to commit.
        int currentSerializedSize = byteArrayOutputStream.getCount() + owningToken.objectStore.getAddSpaceOverhead();
        if (currentSerializedSize > latestSerializedSize) {
            latestSerializedSizeDelta = currentSerializedSize - latestSerializedSize;
            owningToken.objectStore.reserve(latestSerializedSizeDelta, true);
            latestSerializedSize = currentSerializedSize;
        } else {
            latestSerializedSizeDelta = 0;
        } // if (currentSerializedSize > latestSerializedSize).
    } // reserveSpaceInStore().

    /**
     * Get the length of the serialized bytes representing this ManagedObject.
     * 
     * @return long number of bytes currently representing this ManagedObject.
     * @throws ObjectManagerException
     */
    protected long getSerializedBytesLength()
                    throws ObjectManagerException
    {
        final String methodName = "getSerializedBytesLength";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        // Mimic the action of getSerializedBytes but don't actuallly create the bytes.
        // Used to esimate the length of future attempts to serialize the Object.
        class DummyOutputStream extends java.io.OutputStream {

            public void write(int b)
                            throws java.io.IOException
            {}

        } // class DummyOutputStream.
        java.io.DataOutputStream dataOutputStream = new java.io.DataOutputStream(new DummyOutputStream());

        try {
            if (this instanceof SimplifiedSerialization) {
                int signature = getSignature();
                dataOutputStream.writeInt(signature);
                if (signature == SimplifiedSerialization.signature_Generic) {
                    dataOutputStream.writeUTF(getClass().getName());
                } // if (signature == SimplifiedSerialization.signature_Generic).

                ((SimplifiedSerialization) this).writeObject(dataOutputStream);

            } else {
                dataOutputStream.writeInt(SimplifiedSerialization.signature_DefaultSerialization);

                java.io.ObjectOutputStream objectOutputStream = new java.io.ObjectOutputStream(dataOutputStream);
                objectOutputStream.writeObject(this);
                objectOutputStream.close();
            }
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:707:1.34");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           exception);
            throw new PermanentIOException(this,
                                           exception);
        } // catch.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(cclass,
                       methodName,
                       new Object[] { dataOutputStream,
                                     new Long(dataOutputStream.size()) });
        return dataOutputStream.size();
    } // getSerializedBytesLength().

    /**
     * @return long the current updateSequence.
     */
    protected long getUpdateSequence()
    {
        return updateSequence;
    }

    /**
     * Free the updated serialized bytes and return them.
     * Used by the ObjectStore if it uses the serialized form of the ManagedObject.
     * Another transaction is allowed to make new modifications to this ManagedObject.
     * The user of this ManagedObject must hold a lock to prevent changes to this ManagedObject
     * until it has achieved a constent state and captured its serialized bytes.
     * We give their reference to the ObjectStore, so any change to the ManagedObject must create
     * a new serializedBytes buffer, not just update the existing one, because the
     * ObjectStore may be writing it.
     * 
     * @return ObjectManagerByteArrayOutputStream containing the current commited
     *         serializedBytes representing the ManagedObject or null
     *         if they have already been freed or were never set.
     * @throws ObjectManagerException
     */
    protected final ObjectManagerByteArrayOutputStream freeLatestSerializedBytes()
                    throws ObjectManagerException {
        final String methodName = "freeLatestSerializedBytes";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        // Make a safe reference to the serializedBytes before we clear it.
        ObjectManagerByteArrayOutputStream serializedBytesToReturn;
        synchronized (forcedUpdateSequenceLock) {
            serializedBytesToReturn = latestSerializedBytes;
            latestSerializedBytes = null;
            // If we are freeing the latest version of the serialized bytes then also release space
            // on the ObjectStore because we must have written the serialzed ManagedObject.
            if (updateSequence == forcedUpdateSequence) {
                if (serializedBytesToReturn != null)
                    serializedBytesToReturn.setReleaseSize(latestSerializedSize);
                latestSerializedSize = 0;
            } // if(updateSequence == forcedUpdateSequence).
        } //  synchronized (forcedUpdateSequenceLock).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { serializedBytesToReturn });
        return serializedBytesToReturn;
    } // freeLatestSerialzedBytes().

    /**
     * <p>
     * Driven when the object is locked within a transaction.
     * This is an intent lock, indicating an intention to update the object.
     * The lock is lifted after commit or backout of the transaction.
     * 
     * @param newTransactionLock being associated with this ManagedObject.
     *            <\p>
     * @throws ObjectManagerException
     */
    protected void lock(TransactionLock newTransactionLock)
                    throws ObjectManagerException {
        final String methodName = "lock";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, newTransactionLock);

        // Already locked to the requesting Transaction?
        while (transactionLock != newTransactionLock) {

            synchronized (this) {
                if (!transactionLock.isLocked()) {
                    setState(nextStateForLock); // Make the state change. 
                    // Not yet part of a transaction. 
                    transactionLock = newTransactionLock;
                    // Create the before immage. 
                    try {
                        // TODO Don't realy need this if we are stateAdded
                        beforeImmage = (ManagedObject) this.clone();
                    } catch (java.lang.CloneNotSupportedException exception) {
                        // No FFDC Code Needed.
                        ObjectManager.ffdc.processException(this, cclass, "lock", exception, "1:807:1.34");
                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this, cclass, methodName, exception);
                        throw new UnexpectedExceptionException(this, exception);
                    } // catch CloneNotSupportedException.
                } else if (transactionLock == newTransactionLock) {
                    // Do nothing another thread got the lock after we passed the while() above
                    // but before we hit the synchronized block. 
                } else {
                    // Already part of another transaction.
                    // throw new AlreadyLockedException(this
                    //                                 );
                    threadsWaitingForLock++; // Count the waiting threads.
                    try {
                        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                            trace.debug(this, cclass, methodName, new Object[] { "About to wait()",
                                                                                transactionLock.getLockingTransaction(),
                                                                                new Integer(threadsWaitingForLock) });
                        wait();

                    } catch (InterruptedException exception) {
                        // No FFDC Code Needed.
                        ObjectManager.ffdc.processException(this, cclass, "lock", exception, "1:829:1.34");
                        threadsWaitingForLock--; // Count the waiting threads. 
                        // Just bail out without taking the lock. 
                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this
                                       , cclass
                                       , methodName
                                       , exception);
                        throw new UnexpectedExceptionException(this
                                                               , exception);
                    } // catch (InterruptedException...
                    threadsWaitingForLock--; // Count the waiting threads. 
                } // if ( transactionLock == null...
            } // synchronized(this).  
        } // while (lockingTransaction != internalTransaction).

        numberOfLocksTaken++;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // lock().

    /**
     * <p>
     * Reverse the action of lock().
     * The result from wasLocked will no longer apply after this method has executed.
     * <\p>
     * 
     * @throws ObjectManagerException
     */
    private final synchronized void unlock()
                    throws ObjectManagerException
    {
        final String methodName = "unlock";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { transactionLock });

        setState(nextStateForUnlock); // Make the state change. 

        transactionLock = new TransactionLock(null);
        numberOfLocksTaken = 0;
        notify(); // Wake up one waiter.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // unlock().

    /**
     * <p>
     * Tests to see if this ManagedObject is locked by a given transaction.
     * Not synchronized because whoever calls this must be holding a corser lock in order that the
     * result will be useful.
     * <\p>
     * 
     * @param transaction testing the lock, may be null.
     * @return true if the ManagedObject is locked by the transaction,
     *         or if the ManagedObject is not locked by any transaction and thr transaction is null.
     */
    public boolean lockedBy(Transaction transaction)
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "lockedBy",
                        new Object[] { transaction });
        InternalTransaction internalTransaction = null;
        if (transaction != null)
            internalTransaction = transaction.internalTransaction;
        if (transactionLock.getLockingTransaction() == internalTransaction)
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "lockedBy",
                           "returns true");
            return true;
        } else {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "lockedBy",
                           "returns false");
            return false;
        }
    } // lockedBy().

    /**
     * <p>
     * Tests to see if this ManagedObject is locked by a given transaction. Not synchronized because whoever calls this
     * must be holding a corser lock in order that the result will be useful. <\p>
     * 
     * @param internalTransaction testing the lock.
     * @return boolean true if the lock is held by the InternalTransaction.
     */
    protected boolean lockedBy(InternalTransaction internalTransaction)
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this
                        , cclass
                        , "lockedBy"
                        , internalTransaction
                            );
        if (transactionLock.getLockingTransaction() == internalTransaction)
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this
                           , cclass
                           , "lockedBy"
                           , "returns true"
                                );
            return true;
        } else {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this
                           , cclass
                           , "lockedBy"
                           , "returns false"
                                );
            return false;
        }
    } // lockedBy().

    /**
     * @return boolean true if the transactionLock is locked, to a transaction.
     */
    protected boolean isLocked()
    {
        return transactionLock.isLocked();
    }

    /**
     * @param unlockPoint the sequence in the past at which the lock is tested.
     * @return boolean true if the transactionLock was locked, to a transaction
     *         at or after the unlockPoint.
     */
    protected boolean wasLocked(long unlockPoint)
    {
        return transactionLock.wasLocked(unlockPoint);
    }

    /**
     * @return TransactionLock in effect.
     */
    protected TransactionLock getTransactionLock()
    {
        return transactionLock;
    }

    /**
     * @return Transaction locking this ManagedObject or null if it is not locked.
     */
    public final Transaction getLockingTransaction() {
        Transaction lockingTransaction = null;
        InternalTransaction internalTransaction = transactionLock.getLockingTransaction();
        if (internalTransaction != null)
            lockingTransaction = internalTransaction.getExternalTransaction();

        return lockingTransaction;
    } // getLockingTransaction().

    /**
     * Driven when the transaction is getting ready to add this ManagedObject
     * to an object store.
     * The after image has not been captured so the ManagedObject can still be changed.
     * 
     * A subclass may override this method to do its own replace processing before the log
     * has been written.
     * 
     * @param transaction controling the addition.
     * @throws ObjectManagerException
     */
    public void preAdd(Transaction transaction)
                    throws ObjectManagerException {
        final String methodName = "preAdd";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { transaction });

        // Whoever is calling us ought to hold a lock on the ManagedObject.
        testState(nextStateForAdd); // Test the state change.
        // We take a lock, only the owning transaction may now make non optimistic updates and 
        // all of those operations are now synchgronized on the transaction. 
        // TODO Should have a variant of lock that does not create a before image.
        lock(transaction.internalTransaction.getTransactionLock());
        // Bump the updateSequence to indicate the current version of the ManagedObject.
        // The caller is protected by the transaction lock.
        // The updateSequence is only meaningful while that lock is held.
        updateSequence++;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // preAdd().

    /**
     * Driven when the object added to an object store within a transaction.
     * 
     * A subclass may override this method to do its own add processing after the log has been written.
     * 
     * @param transaction controling the addition.
     * @param logged true if transaction logging of the add was successful.
     * @throws ObjectManagerException
     */
    public void postAdd(Transaction transaction,
                        boolean logged)
                    throws ObjectManagerException {
        final String methodName = "postAdd";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { transaction,
                                                                new Boolean(logged) });

        // Note that we do not add the object to the object store here as we have not
        // yet forced the log to disk.

        if (logged) {
            setState(nextStateForAdd); // Make the state change.

        } else {
            // The transaction did not succeed in logging the add, revert to the way we were
            // if we were the one that took the lock.
            if (numberOfLocksTaken == 1)
                unlock();
            owningToken.objectStore.reserve(-latestSerializedSizeDelta, false);
            latestSerializedSize = latestSerializedSize - latestSerializedSizeDelta;
        } // if (logged).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // postAdd().

    /**
     * Driven when the transaction is getting ready to replace a ManagedObject in an object store The object must have
     * previously been locked. The after image has not been captured so the ManagedObject can still be changed.
     * 
     * A subclass may override this method to do its own replace processing before the log has been written.
     * 
     * @param transaction controling the replacement.
     * @throws ObjectManagerException
     */
    public void preReplace(Transaction transaction)
                    throws ObjectManagerException
    {
        final String methodName = "preReplace";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { transaction });

        // A subclass may override this method to do its own replace processing before the log
        // has been written.

        // Check the transaction holds the lock.  
        if (transactionLock.getLockingTransaction() != transaction.internalTransaction) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName, new Object[] { transaction,
                                                                   transactionLock });
            throw new InvalidTransactionException(this
                                                  , transaction.internalTransaction
                                                  , transactionLock);
        } // if (transactionLock...

        testState(nextStateForReplace); // Test the state change.
        // Bump the updateSequence to indicate the current version of the ManagedObject.
        // The caller is protected by the transaction lock.
        // The updateSequence is only meaningful while that lock is held.
        updateSequence++;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // preReplace().

    /**
     * Driven when the object is replaced in an object store within a transaction. The object must have previously been
     * locked.
     * 
     * A subclass may override this method to do its owd replace processing after the log has been written.
     * 
     * @param transaction controling the replacement.
     * @param logged true if transaction logging of the replace was successful.
     * @throws ObjectManagerException
     */
    public void postReplace(Transaction transaction,
                            boolean logged)
                    throws ObjectManagerException
    {
        final String methodName = "postReplace";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { transaction,
                                                                new Boolean(logged) });

        // Note that we do not replace the object to the object store here as we have not
        // yet forced the log to disk and we dont keep a before image in the ObjecStore. 
        if (logged) {
            setState(nextStateForReplace); // Make the state change.
        } else {
            owningToken.objectStore.reserve(-latestSerializedSizeDelta, false);
            latestSerializedSize = latestSerializedSize - latestSerializedSizeDelta;
        } // if(logged).  
          // preReplace did not take a lock so no need to consider unlocking it.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // postReplace().

    /**
     * Driven when the object is optimistically replaced in an object store within a transaction. This differs from a
     * standard replace in that the ManagedObject need not be locked. There is no before immage taken and it is up to the
     * user of the optimisticReplace to perform any compensating action should the transaction back out. The after image
     * has not been captured so the ManagedObject can still be changed.
     * 
     * A subclass may override this method to do its own optimisticReplace processing before the log has been written.
     * 
     * @param transaction controling the replacement.
     * @throws ObjectManagerException
     */
    public void preOptimisticReplace(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this
                        , cclass
                        , "preOptimisticReplace"
                        , new Object[] { transaction }
                            );

        testState(nextStateForOptimisticReplace); // Test the state change.
        // Bump the updateSequence to indicate the current version of the ManagedObject.
        // The caller must have synchronized on a lock on some Object to protect the 
        // ManagedObject from change while we serialize it. 
        // The updateSequence is only meaningful while that lock is held.
        updateSequence++;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "preOptimisticReplace");
    } // preOptimisticReplace().

    /**
     * Driven when the object is optimistically replaced in an object store
     * within a transaction.
     * 
     * A subclass may override this method to do its own optimisticReplace processing after the log
     * has been written.
     * 
     * @param transaction controling the replacement.
     * @param logged true if transaction logging of the optimisticReplace was successful.
     * @throws ObjectManagerException
     */
    public void postOptimisticReplace(Transaction transaction,
                                      boolean logged)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "postOptimisticReplace",
                        new Object[] { transaction,
                                      new Boolean(logged) });
        if (logged) {
            setState(nextStateForOptimisticReplace); // Make the state change.
        } else {
            owningToken.objectStore.reserve(-latestSerializedSizeDelta, false);
            latestSerializedSize = latestSerializedSize - latestSerializedSizeDelta;
        } // if (logged).  

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "postOptimisticReplace");
    } // postOptimisticReplace().

    /**
     * A subclass may override this method to do its own optimisticReplace processing after the log
     * has been written.For the ManagedObject to get this call it must have been on the Notify
     * list when Transaction.optimisticReplace() was invoked.
     * 
     * @param transaction controling the replacement.
     * @throws ObjectManagerException
     */
    public void optimisticReplaceLogged(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this
                        , cclass
                        , "optimisticReplaceLogged"
                        , "transaction=" + transaction + "(Transaction)"
                            );

        // By default does nothing, unless overridden. 

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this
                       , cclass
                       , "optimisticReplaceLogged"
                            );
    } // optimisticReplaceLogged().

    /**
     * Driven when the object is deleted from an object store within a transaction.
     * 
     * A subclass may override this method to do its own delete processing defore the log
     * has been written.
     * 
     * @param transaction controling the deletion.
     * @throws ObjectManagerException
     * 
     */
    public void preDelete(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "preDelete",
                        new Object[] { transaction });

        // Whoever is calling us ought to hold a lock on the ManagedObject.
        testState(nextStateForDelete); // Test the state change.
        // We take a lock, only the owning transaction may now make non optimistic updates and 
        // all of those operations are now synchronized on the transaction. 
        // TODO Should have a variant of lock that does not create a before image.
        lock(transaction.internalTransaction.getTransactionLock()); // Make sure we are allowed to delete. 
        // Bump the updateSequence to indicate the current version of the ManagedObject.
        // The caller is protected by the transaction lock.
        // The updateSequence is only meaningful while that lock is held.
        updateSequence++;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this
                       , cclass
                       , "preDelete"
                            );
    } // delete().

    /**
     * Driven when the object is deleted from an object store within a transaction.
     * 
     * A subclass may override this method to do its own delete processing after the log
     * has been written.
     * 
     * @param transaction controling the deletion.
     * @param logged true if transaction logging of the delete was successful.
     * @throws ObjectManagerException
     */
    public void postDelete(Transaction transaction,
                           boolean logged)
                    throws ObjectManagerException {
        final String methodName = "postDelete";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { transaction,
                                                                new Boolean(logged) });

        if (logged) {
            setState(nextStateForDelete); // Make the state change.

        } else {
            // The transaction did not succeed in logging the delete, revert to the way we were
            // if we were the one that took the lock.     
            if (numberOfLocksTaken == 1)
                unlock();
            owningToken.objectStore.reserve(-latestSerializedSizeDelta, false);
            latestSerializedSize = latestSerializedSize - latestSerializedSizeDelta;
        } // if (logged).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this
                       , cclass
                       , methodName
                            );
    } // postDelete().

    /**
     * Driven just before the object changes to prepared state within a transaction,
     * if requested. This is only redriven at recovery time if the the ManagedObject
     * explicitly re-requests this at recovery time. Any ManagedObject requesting this
     * callback should override this method.
     * 
     * @param transaction causing the prepare.
     * @throws ObjectManagerException
     */
    public void prePrepare(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "prePrepare",
                        new Object[] { transaction });

        // By default does nothing, unless overridden. 

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this
                       , cclass
                       , "prePrepare"
                            );
    } // End of prePrepare method.

    // See InternalTransaction.prepare().
//  /**
//   * Driven when the object changes to prepared state within a transaction.
//   * Subclasses of ManagedObject may not override this method they should request 
//   * PrePrepare and postPrepare be drivven and use them. 
//   */
//  protected void prepare(Transaction transaction)
//            throws ObjectManagerException
//  {
//    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())  
//      trace.entry(this
//                 ,"prepare"
//                 ,"transaction="+transaction+"(Transaction)"
//                 );
//    
//    // Doees nothing, cannot be overridden. 
// 
//    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//      trace.exit(this
//                ,"prepare"
//                );
//  }                                              // End of prepare method.

    /**
     * Driven just before the object commits within a transaction,
     * if requested. This is only redriven at recovery time if the the ManagedObject
     * explicitly requests this at recovery time.
     * 
     * @param transaction causing the commit.
     * @throws ObjectManagerException
     */
    public void preCommit(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "preCommit",
                        new Object[] { transaction });

        // By default does nothing, unless overridden.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this
                       , cclass
                       , "preCommit"
                            );
    } // preCommit().

    /**
     * Driven when the object changes to commited state within a transaction.
     * Cannot be overriden by a subclass of ManagedObject.
     * 
     * @param transaction commiting the update.
     * @param serializedBytes the bytes that were given to the
     *            transaction when the last update was made or null if the object has been deleted,
     *            or just locked and not changed.
     * @param savedSequenceNumber when the update was made and which has now been saved.
     * @param requiresCurrentCheckpoint true if the managed object must be updated as part of the current checkpoint.
     * @throws ObjectManagerException
     */
    protected void commit(Transaction transaction
                          , ObjectManagerByteArrayOutputStream serializedBytes
                          , long savedSequenceNumber
                          , boolean requiresCurrentCheckpoint
                    )
                                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "commit",
                        new Object[] { transaction,
                                      serializedBytes,
                                      new Long(savedSequenceNumber),
                                      new Boolean(requiresCurrentCheckpoint) });
        // No need to synchronize when we copy the entry state as only the locking transaction can cause a state change. 
        int entryState = state;
        setState(nextStateForCommit); // Make the state change. 

        // Update the object in its store. 
        // We do this only after the add/replace/delete records has been written and forced
        // to the log as part of commit processing.
        switch (entryState) {
            case stateAdded:
            case stateReplaced:
                synchronized (forcedUpdateSequenceLock) {
                    // Now the object to give the ObjectStore, if we have a newer version.     
                    if (savedSequenceNumber > forcedUpdateSequence) {
                        forcedUpdateSequence = savedSequenceNumber;
                        latestSerializedBytes = serializedBytes;
                        owningToken.objectStore.add(this, requiresCurrentCheckpoint);
                    } // if (savedSequenceNumber > forcedUpdateSequence).
                } // synchronized (forcedUpdateSequenceNumberLock).
                break;

            case stateToBeDeleted:
            case stateMustBeDeleted:
                // An OptimisticReplace for another transaction might have been executed and commited after our delete but
                // but before this commit, in which case the commited version could legitimately be greater than the 
                // one that deleted the Object.

                // Taking the forcedUpdateSequenceLock, ensures that any OptimisticReplace 
                // updates see our state is now deleted.
                int tempLatestSerializedSize;
                synchronized (forcedUpdateSequenceLock) {
                    if (savedSequenceNumber > forcedUpdateSequence) {
                        forcedUpdateSequence = savedSequenceNumber;
                        // The forcedSerializedBytes passed in may not be null if another OptimisticReplace happened to this
                        // ManagedObject after Delete had been logged. This happens when two links are deleted in a LinkedList 
                        // under one transaction.
                        // latestSerializedBytes might be null if they have been written by the store.
                        if (latestSerializedBytes != null) {
                            owningToken.objectStore.getObjectManagerState().returnByteArrayOutputStreamToPool(latestSerializedBytes);
                            latestSerializedBytes = null;
                        } // if (latestSerializedBytes != null).
                    } // if (savedSequenceNumber > forcedUpdateSequenceNumber).

                    // Remove the object from its object store. If an OptimisticReplace committed before we committed
                    // the delete, then the log version will have already been bumped up, so delete the object
                    // irrespective of whether it was changed by an optimisticReplace in another transaction after
                    // we deleted it in this transaction. Also a checkpoint may have already noticed that the
                    // logSequence number has moved on.
                    owningToken.objectStore.remove(owningToken,
                                                   requiresCurrentCheckpoint);
                    tempLatestSerializedSize = latestSerializedSize;
                    latestSerializedSize = 0;
                } // synchronized (forcedUpdateSequenceLock).
                  // following statement moved out of lock in PM41418 to prevent deadlock
                owningToken.objectStore.reserve(-tempLatestSerializedSize, false);
                break;

        } // switch.

        beforeImmage = null; // Help garbage collector.
        numberOfLocksTaken = 0;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "commit");
    } // commit().

    /**
     * Driven when the transaction changes to commited state for a
     * ManagedObject that was optimistically replaced.
     * Cannot be overriden my a subclass of ManagedObject.
     * 
     * @param transaction performing the commit.
     * @param serializedBytes the stream containing the bytes that were given to the
     *            transaction when the last update was made.
     * @param savedSequenceNumber when the update was made and which has now been saved.
     * @param requiresCurrentCheckpoint true if the managed object must be updated as part of the current checkpoint.
     * @throws ObjectManagerException
     */
    protected void optimisticReplaceCommit(Transaction transaction,
                                           ObjectManagerByteArrayOutputStream serializedBytes,
                                           long savedSequenceNumber,
                                           boolean requiresCurrentCheckpoint)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this
                        , cclass
                        , "optimisticReplaceCommit"
                        , new Object[] { transaction, serializedBytes, new Long(savedSequenceNumber), new Boolean(requiresCurrentCheckpoint) }
                            );

        //!! could do the test for locking transaction here instead of in Transaction and have just one
        //!! commit emethod in ManagedObject.
        setState(nextStateForOptimisticReplaceCommit);
        // Update the object in its store, if we now have a newer version.
        // It might be that some other transaction has already commited a higher version than this. 
        // We do this only after the optimisticReplace has been written and forced
        // to the log as part of commit processing.            

        // Thread 1                      Thread 2
        // Transaction 1                 Transaction 2
        //
        // Ready                  V0
        // Delete-->ToBeDeleteted V1
        // OptimisticReplace      V2                              // Caused by deleteion of another item in LinkedList?     
        //                               OptimisticReplace V3
        // (Optimistic replace of other Objects negate the deletion of this one.)
        // Log Commit
        //                               Log Commit
        // Remove From Store      V2
        //                               No action needed on Store

        synchronized (forcedUpdateSequenceLock) {
            if (savedSequenceNumber > forcedUpdateSequence) {
                forcedUpdateSequence = savedSequenceNumber;

                // Did we commit an OptimisticReplace after some other transaction has subsequently deleted us.
                if (state != stateDeleted) {
                    // Thread 2 in the scenario above. 
                    // Some other thread has deleted this ManagedObject and commited its transaction.        
                    latestSerializedBytes = serializedBytes;
                    owningToken.objectStore.add(this, requiresCurrentCheckpoint);
                } // if (state != stateDeleted).
            } // if (logsequenceNumber > forcedUpdateSequence).
        } // synchronized (forcedUpdateSequenceLock)  

        //  No unlocking to do since the object is not locked for OptimisticReplace.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this
                       , cclass
                       , "optimisticReplaceCommit"
                            );
    } // End of optimisticReplaceCommit method.

    /**
     * Driven just after the object commits within a transaction,
     * if requested. This is only redriven at recovery time if the the ManagedObject
     * explicitly requests this at recovery time.
     * 
     * @param transaction causing the commit.
     * @throws ObjectManagerException
     */
    public void postCommit(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this
                        , cclass
                        , "postCommit"
                        , new Object[] { transaction });

        // By default does nothing, unless overridden. 

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this
                       , cclass
                       , "postCommit"
                            );
    } // postCommit().

    /**
     * Driven just before the object rolls back within a transaction,
     * if requested. This is only redriven at recovery time if the the ManagedObject
     * explicitly requests this at recovery time.
     * 
     * @param transaction causing the backout.
     * @throws ObjectManagerException
     */
    public void preBackout(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "preBackout",
                        new Object[] { transaction });

        // By default does nothing, unless overridden.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this
                       , cclass
                       , "preBackout"
                            );
    } // preBackout().

    /**
     * Driven when the object is backed out within a transaction.
     * 
     * @param transaction performing the backout.
     * @param savedSequenceNumber the sequenceNumber when the update was made and which has now been saved.
     * @param requiresCurrentCheckpoint true if the managed object must be updated as part of the current checkpoint.
     * @throws ObjectManagerException
     */
    protected void backout(Transaction transaction,
                           long savedSequenceNumber,
                           boolean requiresCurrentCheckpoint)
                    throws ObjectManagerException {
        final String methodName = "backout";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { transaction,
                                                                new Boolean(requiresCurrentCheckpoint) });

        // No need to synchronize when we copy the entry state as only the locking transaction can cause a state change.
        int entryState = state;
        setState(nextStateForBackout); // Make the state change.

        // Revert to the original state of the object in its store and in memory.
        // We do this only after the add/replace/delete records has been written and forced
        // to the log as part of backout processing.
        switch (entryState) {
            case stateAdded:
            case stateMustBeDeleted:
                // Remove the allocated or added object from its object store.
                // An optimistic update could have already replaced this even though this
                // transaction has not done its add as part of commit. The optimistic logic must 
                // be cope with this eventuality.  
                // Similarly a checkpoint could have written the object to the ObjectStore.
                // Make sure another thread does not come along with old information later on.
                // Taking the forcedUpdateSequenceLock, ensures that any OptimisticReplace 
                // updates see our state is now deleted.
                int tempLatestSerializedSize;
                synchronized (forcedUpdateSequenceLock) {
                    if (savedSequenceNumber > forcedUpdateSequence) {
                        forcedUpdateSequence = savedSequenceNumber;
                        // latestSerializedBytes might be null if they have been written by the store.
                        if (latestSerializedBytes != null) {
                            owningToken.objectStore.getObjectManagerState().returnByteArrayOutputStreamToPool(latestSerializedBytes);
                            latestSerializedBytes = null;
                        } // if (latestSerializedBytes != null).
                    } // if (savedSequenceNumber > forcedUpdateSequence).

                    owningToken.objectStore.remove(owningToken,
                                                   requiresCurrentCheckpoint);
                    tempLatestSerializedSize = latestSerializedSize;
                    latestSerializedSize = 0;
                } // synchronized (forcedUpdateSequenceLock).
                  // following statement moved out of lock in PM41418 to prevent deadlock
                owningToken.objectStore.reserve(-tempLatestSerializedSize, false);
                break;

            case stateReplaced:
                // Put this object back the way it was. 
                backingOut = true;
                becomeCloneOf(beforeImmage);
                backingOut = false;
                break;

        // case stateToBeDeleted:
        // Nothing to do if we were going to delete the ManagedObject, since we have backed out
        // we simply dont delete it.  
        } // switch.

        beforeImmage = null; // Help garbage collector.
        numberOfLocksTaken = 0;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // backout().

    /**
     * Driven when the ManageObject is backed out after an optimistic replace within a transaction.
     * 
     * @param transaction performing the backout.
     * @param serializedBytes containing the latest form of the serializedBytes after the backout has been performed by
     *            the Optimistic replace.
     * @param savedSequenceNumber that the serialised bytes were saved at.
     * @param requiresCurrentCheckpoint true if the managed object must be updated as part of the current checkpoint.
     * @throws ObjectManagerException
     */
    protected void optimisticReplaceBackout(Transaction transaction
                                            , ObjectManagerByteArrayOutputStream serializedBytes
                                            , long savedSequenceNumber
                                            , boolean requiresCurrentCheckpoint
                    )
                                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this
                        , cclass
                        , "optimisticReplaceBackout"
                        , new Object[] { transaction, serializedBytes, new Long(savedSequenceNumber), new Boolean(requiresCurrentCheckpoint) }
                            );
        // The compensation logic for the optimistic update will correct the backed out update,
        // we now need to give this corrected state to the ObjectStore.

        setState(nextStateForOptimisticReplaceBackout);
        synchronized (forcedUpdateSequenceLock) {
            if (savedSequenceNumber > forcedUpdateSequence) {
                forcedUpdateSequence = savedSequenceNumber;

                // The serializedBytes we have been passed are the ones we are left with after any compensating
                // action has been performed, for eacmple at preBackout time.

                // Did we commit an OptimisticReplace after some other transaction has subsequently deleted us.
                if (state != stateDeleted) {
                    // Thread 2 in the scenario in optimisticReplaceCommit above. 
                    // Some other thread has deleted this ManagedObject and commited its transaction.
                    latestSerializedBytes = serializedBytes;
                    owningToken.objectStore.add(this, requiresCurrentCheckpoint);
                } // if (state != stateDeleted).
            } // if (savesSequenceNumber > forcedUpdateSequence).
        } // synchronized (forcedUpdateSequenceLock).

        //  No unlocking to do since the object is not locked for OptimisticReplace.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "optimisticReplaceBackout");
    } // optimisticReplaceBackout().

    /**
     * Driven just after the object rolls back within a transaction,
     * if requested. This is only redriven at recovery time if the the ManagedObject
     * explicitly requests this at recovery time.
     * 
     * @param transaction causing the backout.
     * @throws ObjectManagerException
     */
    public void postBackout(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this
                        , cclass
                        , "postBackout"
                        , new Object[] { transaction }
                            );

        // By default does nothing, unless overridden. 

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this
                       , cclass
                       , "postBackout"
                            );
    } // postBackout().

    /**
     * Called by the Transaction after the ObjectManager when it has written a checkpointStartLogRecord.
     * The ManagedObject can assume that all log records up to and including logSequenceNumber
     * are now safely hardened to disk. On this assumption we can write after images to the ObjectStores.
     * 
     * @param internalTransaction performing the checkpoint.
     * @param serializedBytes containing the bytes that were given to the transaction
     *            when the last update was made or null if the object has been deleted.
     * @param savedSequenceNumber when the update was made and which has now been saved.
     * @throws ObjectManagerException
     */
    protected void checkpoint(InternalTransaction internalTransaction,
                              ObjectManagerByteArrayOutputStream serializedBytes,
                              long savedSequenceNumber)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "checkpoint",
                        new Object[] { internalTransaction,
                                      serializedBytes,
                                      new Long(savedSequenceNumber) });

        if (lockedBy(internalTransaction)) { // Locking update?   
            switch (state) {
                case stateAdded:
                    synchronized (forcedUpdateSequenceLock) {
                        // Now give the ManagedObnject to the ObjectStore, if we have a newer version.     
                        if (savedSequenceNumber > forcedUpdateSequence) {
                            forcedUpdateSequence = savedSequenceNumber;
                            latestSerializedBytes = serializedBytes;
                            // TODO It is possible that nothing has changed since the last time we checkpointed this
                            //      if the transaction is long running so the addition of the same thing  to the Object
                            //      store might be pointless.
                            owningToken.objectStore.add(this, true);
                        } // if (savedSequenceNumber > forcedUpdateSequence). 
                    } // synchronized (forcedUpdateSequenceLock).
                    break;

                case stateReplaced:
                    // The replaced image is written as part of the TransactionCheckpointLogRecord and must 
                    // not be given to the ObjectStore because we dont yet know whether it will commit.
                    break;

                case stateToBeDeleted:
                case stateMustBeDeleted:
                    synchronized (forcedUpdateSequenceLock) {
                        if (savedSequenceNumber > forcedUpdateSequence) {
                            if (serializedBytes != null) {
                                // We have to consider including toBeDeleted Objects because they have not yet been deleted. 
                                // The forcedSerializedBytes passed in may not be null if another OptimisticReplace happened to this
                                // ManagedObject after Delete had been logged. This happens when two links are deleted in a LinkedList 
                                // under one transaction. If the serialized bytes are null we leave the ObjectStore version alone and 
                                // let another transaction place its version in the store. We will only delete them when the
                                // transaction commits.
                                forcedUpdateSequence = savedSequenceNumber;
                                latestSerializedBytes = serializedBytes;
                                owningToken.objectStore.add(this, true);
                            } // if (serializedBytes != null). 
                        } // if (savedSequenceNumber > forcedUpdateSequence).
                    } // synchronized (forcedUpdateSequenceLock).
                    break;

            } // switch.

        } else { // OptimisticReplace update.
            synchronized (forcedUpdateSequenceLock) {
                if (savedSequenceNumber > forcedUpdateSequence) {
                    forcedUpdateSequence = savedSequenceNumber;
                    // Did we checkpoint an OptimisticReplace after some other transaction has subsequently deleted us.
                    if (state != stateDeleted) {
                        latestSerializedBytes = serializedBytes;
                        owningToken.objectStore.add(this, true);
                    } // if (state != stateDeleted).
                } // if (savedSequenceNumber > forcedUpdateSequence).
            } // synchronized (forcedUpdateSequenceLock).
        } // if (lockedBy(transaction)).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "checkpoint");
    } // checkpoint().

    /**
     * Driven just after the ObjectManager finishes replaying the log,
     * but before it backs out any incomplete tranmsactions and starts to make
     * forward progress.
     * 
     * @param transaction for which recovery is now completed.
     * @throws ObjectManagerException
     */
    public void recoveryCompleted(Transaction transaction)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "recoveryCompleted",
                        new Object[] { transaction });

        // By default does nothing, unless overridden. 

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "recoveryCompleted");
    } // recoveryCompleted().

    /**
     * Replace the state of this object with the same object in some other state.
     * Used for to restore the before image if a transaction rolls back.
     * 
     * Unless overriden this replaces all non static and non final fields with those from the
     * other ManagedObject.
     * 
     * @param other is the object this object is to become a clone of.
     * @throws ObjectManagerException
     */
    public void becomeCloneOf(ManagedObject other)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "becomeCloneOf",
                        new Object[] { other });

        Class thisClass = getClass();
        Class otherClass = other.getClass();
        while (!thisClass.getName()
                        .equals("com.ibm.ws.objectManager.ManagedObject")) {
            java.lang.reflect.Field[] fields = thisClass.getDeclaredFields();
            try {
                java.lang.reflect.AccessibleObject.setAccessible(fields,
                                                                 true);
            } catch (SecurityException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    "becomeCloneOf",
                                                    exception,
                                                    "1:1887:1.34");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "becomeCloneOf",
                               "via UnexpectedExceptionException");
                throw new UnexpectedExceptionException(this,
                                                       exception);
            } // try...

            try {
                for (int i = 0; i < fields.length; i++) {
                    int modifier = fields[i].getModifiers();
                    if (!java.lang.reflect.Modifier.isFinal(modifier) && !java.lang.reflect.Modifier.isStatic(modifier)) {
                        fields[i].set(this,
                                      fields[i].get(other));
                    } // if (!(java.lang.reflect.Modifier.isFinal(modifier)...
                } // for (int i=0; i<fields.length; i++).

            } catch (IllegalAccessException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    "becomeCloneOf",
                                                    exception,
                                                    "1:1913:1.34");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "becomeCloneOf",
                               "via UnexpectedExceptionException");
                throw new UnexpectedExceptionException(this,
                                                       exception);
            } // catch (IllegalAccessException exception).

            // Step up the class hierarchy.
            thisClass = thisClass.getSuperclass();
            otherClass = otherClass.getSuperclass();

        } // while (!thisClass.getName().equals("com.ibm.ws.objectManager.ManagedObject")).
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "becomeCloneOf");
    } // becomeCloneOf().

    /**
     * @return int the current state of this ManagedObject.
     * @throws ObjectManagerException
     */
    public int getState()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this
                        , cclass
                        , "getState"
                            );

        int stateToReturn = state;
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this
                       , cclass
                       , "getState"
                       , "returns statetoReturn=" + stateToReturn + "(int) " + stateNames[stateToReturn] + "(String)"
                            );
        return stateToReturn;
    } // getState().

    /**
     * Test a state transition.
     * 
     * @param nextState maps the current state to the new one.
     * @throws InvalidStateException if the transition is invalid.
     */
    private void testState(int[] nextState)
                    throws InvalidStateException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "testState",
                        new Object[] { nextState,
                                      new Integer(state),
                                      stateNames[state] });

        int newState = nextState[state]; // Make the state change.       

        if (newState == stateError) {
            InvalidStateException invalidStateException = new InvalidStateException(this, state, stateNames[state]);
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "testState"
                           , new Object[] { invalidStateException, new Integer(newState), stateNames[newState] }
                                );
            throw invalidStateException;
        } // if (state == stateError).  

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "testState");
    } // testState().

    /**
     * Makes a state transition.
     * 
     * @param nextState maps the current stte to the new one.
     * 
     * @throws StateErrorException if the transition is invalid.
     */
    private void setState(int[] nextState)
                    throws StateErrorException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "setState",
                        new Object[] { nextState,
                                      new Integer(state),
                                      stateNames[state] });

        synchronized (stateLock) {
            previousState = state; // Capture the previous state for dump. 
            state = nextState[state]; // Make the state change.       
        } // synchronized (stateLock)  

        if (state == stateError) {
            StateErrorException stateErrorException = new StateErrorException(this, previousState, stateNames[previousState]);
            ObjectManager.ffdc.processException(this, cclass, "setState", stateErrorException, "1:2016:1.34");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "setState",
                           new Object[] { stateErrorException,
                                         new Integer(state),
                                         stateNames[state] });
            throw stateErrorException;
        } // if (state == stateError).  

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this
                       , cclass
                       , "setState"
                       , "state=" + state + "(int) " + stateNames[state] + "(String)"
                            );
    } // setState().

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        if (owningToken == null)
            return new String("ManagedObject"
                              + "(null/null)"
                              + "/" + stateNames[state]
                              + "/" + Integer.toHexString(hashCode()));
        else
            return new String("ManagedObject"
                              + "(" + owningToken.objectStoreIdentifier + "/" + owningToken.storedObjectIdentifier + ")"
                              + "/" + stateNames[state]
                              + "/" + Integer.toHexString(hashCode()));
    } // toString().

    // --------------------------------------------------------------------------
    // Simplified serialization.
    // --------------------------------------------------------------------------

    /**
     * The number of bytes serialized size of this Object will not exceed.
     * 
     * @return long an overestimate of the serialized form of this Object.
     */
    protected static long maximumSerializedSize()
    {
        return 4 // int  SimplifiedSerialization.signature_XXX
        + 1 // byte Version. 
        ;
    } // maximumSerializedSize().

    private static final byte simpleSerialVersion = 0;

    /**
     * Provide the identifier of the Object for simplifiedSerialisation. Overriden if
     * a non Generic ManagedObject is being serialized.
     * 
     * Not part of SimplifiedSerialization so that only members of the ObjectManager package can override it.
     * 
     * @return int the signature identifying the ManagedObject class.
     */
    int getSignature()
    {
        return SimplifiedSerialization.signature_Generic;
    } // getSignature().

    /**
     * @return long the length in bytes of the SimplifiedSerialization form of the Object.
     * @see com.ibm.ws.objectManager.SimplifiedSerialization
     */
    public long estimatedLength() {
        // Most Objects are at least 64 bytes in length.
        return 64;
    } // estimatedLength().

    /*
     * (non-Javadoc)
     * 
     * @see SimplifiedSerialization.writeObject(java.io.DataInputStream)
     */
    protected void writeObject(java.io.DataOutputStream dataOutputStream)
                    throws ObjectManagerException, java.io.IOException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this
                        , cclass
                        , "writeObject"
                        , new Object[] { dataOutputStream }
                            );

        try {
            dataOutputStream.writeByte(simpleSerialVersion);

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "writeObject", exception, "1:2111:1.34");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this
                           , cclass
                           , "writeObject"
                           , "via PermanentIOException"
                                );
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        // We cannot check size as we are subclassed.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "writeObject");
    } // writeObject().

    /*
     * (non-Javadoc)
     * 
     * @see SimplifiedSerialization.readObject(java.io.DataInputStream,ObjectManagerState)
     */
    protected void readObject(java.io.DataInputStream dataInputStream
                              , ObjectManagerState objectManagerState
                    )
                                    throws ObjectManagerException, java.io.IOException
    {
        final String methodName = "readObject";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, methodName, new Object[] { dataInputStream,
                                                                objectManagerState });

        try {
            byte version = dataInputStream.readByte();
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this, cclass, methodName, new Object[] { "version:2147", new Byte(version) });

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:2151:1.34");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           new Object[] { exception });
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        state = stateReady; // Initial state.
        previousState = -1; // No previous state.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // readObject().

    // --------------------------------------------------------------------------
    // implements java.io.Serializable
    // --------------------------------------------------------------------------  

    /**
     * Customized deserialization. Notice that subclasses do not have to call this method
     * in their super class because the deserialisation of the subclass need not concern
     * itself with restoring the state of the super class.
     * 
     * @param objectInputStream containing the serialized form of the Object.
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * 
     */
    private void readObject(java.io.ObjectInputStream objectInputStream)
                    throws java.io.IOException
                    , java.lang.ClassNotFoundException
    {
        final String methodName = "readObject";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { objectInputStream });

        objectInputStream.defaultReadObject();

        threadsWaitingForLock = 0;
        forcedUpdateSequence = 0;
        forcedUpdateSequenceLock = new ForcedUpdateSequenceLock();
        transactionLock = new TransactionLock(null);
        state = stateReady; // Initial state.
        previousState = -1; // No previous state.
        stateLock = new Object();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // readObject().
} // class ManagedObject.