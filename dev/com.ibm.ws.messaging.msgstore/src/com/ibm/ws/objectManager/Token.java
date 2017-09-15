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

import com.ibm.ws.objectManager.utils.Trace;
import com.ibm.ws.objectManager.utils.Tracing;

/**
 * <p>ManagedObject instantiates a handle to an object in an Object Store.
 * The ManagedObject uniquely identifies the stored object and its Object Store.
 * The managed objects are transactional.
 * 
 * This class is final because we want to prevent subclassses modifying the implementatoin of hashcode()
 * such that it provides non nunique values withing the JVM. Transaction and ObjectStore depend
 * on Tokens having unique hashcodes so that they can determine if they already have reference
 * to an instance of the same token. See also comments in InternalTransaction.
 * 
 * Implements externalizamle as well as SimplifiedSerialization becase logrecord andManagedObject may
 * serialize Tokens imbedded in other Objects.
 * 
 * @author IBM Corporation
 */
public final class Token
                implements java.io.Externalizable
                , SimplifiedSerialization
{
    private static final Class cclass = Token.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_OBJECTS);

    private static final long serialVersionUID = -6508104910374719410L;

    // The object is identified uniquely by the objectStoreIdentifier and the storedObjectIdentifier. 
    int objectStoreIdentifier; // The identifier of the ObjectStore.
    long storedObjectIdentifier; // A unique number. 

    // Weak reference that the garbage collector can reuse the memory occupied by the
    // underlying managedObject if no other references are made to it.
    // We dont extend weakReference like java.util.WeakHashMap.Entry, because this means that we
    // have to instantiate the underlying ManagedObject each time we instantiate its Token.
    protected transient java.lang.ref.WeakReference managedObjectReference;
    protected transient ObjectStore objectStore; // The ObjectStore storing the ManagedObject.

    /**
     * Constructor
     * 
     * @param managedObject which the token handles.
     * @param objectStore intowhich the ManagedObject is allocated.
     * @param storedObjectIdentifier of the ManagedObject.
     */
    protected Token(ManagedObject underlyingObject,
                    ObjectStore objectStore,
                    long storedObjectIdentifier) {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "<init>"
                        , new Object[] { underlyingObject, objectStore, new Long(storedObjectIdentifier) });

        this.objectStore = objectStore;
        this.storedObjectIdentifier = storedObjectIdentifier;
        objectStoreIdentifier = objectStore.getIdentifier();
        underlyingObject.owningToken = this; // PM22584 Set the owning token first
        this.managedObjectReference = new java.lang.ref.WeakReference(underlyingObject);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "<init>");
    } // Token().

    /**
     * Constructor, used to make a reference to a null underlying object.
     * See Token.restore() and ObjectMAnagerState.saveClonedState().
     * 
     * @param objectStore into which the ManagedObject is allocated.
     * @param storedObjectIdentifier of the ManagedObject.
     */
    protected Token(ObjectStore objectStore,
                    long storedObjectIdentifier) {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "<init>"
                        , " objectStore=" + objectStore + "(ObjectStore)"
                          + " storedObjectIdentifier=" + storedObjectIdentifier + "(long)"
                            );

        managedObjectReference = null;
        this.objectStore = objectStore;
        this.storedObjectIdentifier = storedObjectIdentifier;
        objectStoreIdentifier = objectStore.getIdentifier();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "<init>"
                            );
    } // Token().

    /**
     * Give back the current token if there is already one known to the
     * Object Store for the same ManagedObject.
     * 
     * @return Token already known to the ObjectStore.
     */
    protected Token current()
    {
        final String methodName = "current";
        Token currentToken;
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , methodName
                          + toString()
                            );

        currentToken = objectStore.like(this);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , methodName
                       , new Object[] { currentToken, Integer.toHexString(currentToken.hashCode()) });
        return currentToken;
    } // current().

    /**
     * Find the ManagedObject handled by this token.
     * 
     * @return ManagedObject the underlying ManagedObject represented by the token.
     * @throws ObjectManagerException
     */
    public final ManagedObject getManagedObject()
                    throws ObjectManagerException {
//    final String methodName = "getManagedObject";
//    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//      trace.entry(this,
//                  cclass,
//                  methodName);

        // Get the object if is already in memory.
        ManagedObject managedObject = null;
        if (managedObjectReference != null)
            managedObject = (ManagedObject) managedObjectReference.get();

        if (managedObject == null) { // See if we can avoid synchronizing.
            synchronized (this) {
                if (managedObjectReference != null)
                    managedObject = (ManagedObject) managedObjectReference.get();
                if (managedObject == null) {
                    managedObject = objectStore.get(this);
                    if (managedObject != null) {
                        managedObject.owningToken = this; // PM22584 Set the owning token first
                        managedObjectReference = new java.lang.ref.WeakReference(managedObject);
                    } // if (managedObject != null). 
                } // if (managedObject == null).
            } // synchronize (this).
        } // if (managedObject == null).

//    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//      trace.exit(this,
//                 cclass,
//                 methodName,
//                 new Object[] {managedObject}); 
        return managedObject;
    } // getManagedObject(); 

    /**
     * Associate a new ManagedObject with this token. If there is already
     * a managedObject associated with the Token it is replaced by making it a clone of the
     * new ManagedObject so that existing references to the old ManagedObject becone
     * refrences to the new one.
     * 
     * @param ManagedObject to be associated with this Token.
     * @return ManagedObject now associated with this Token.
     * @throws ObjectManagerException
     */
    protected synchronized ManagedObject setManagedObject(ManagedObject managedObject)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "setManagedObject"
                          + "managedObject=" + managedObject + "(ManagedObject)"
                          + toString()
                            );

        ManagedObject managedObjectSet; // For return.
        // Has any ManagedObject ever been associated with tis Token?
        if (managedObjectReference == null) {
            // Nothing currently known so use the version given.
            managedObject.owningToken = this; // PM22584 Set the owning token first
            managedObjectReference = new java.lang.ref.WeakReference(managedObject);
            managedObjectSet = managedObject;
        } else {
            ManagedObject existingManagedObject = (ManagedObject) managedObjectReference.get();
            if (existingManagedObject == null) { // Is it still accessible? 
                managedObject.owningToken = this; // PM22584 Set the owning token first
                managedObjectReference = new java.lang.ref.WeakReference(managedObject);
                managedObjectSet = managedObject;
            } else { // In memory and accessible.    
                // During recovery another object already recovered may have refered 
                // to this one causing it to already be resident in memory. 
                // Replace what we already have with this version.
                existingManagedObject.becomeCloneOf(managedObject);
                managedObjectSet = existingManagedObject;
            } // if (existingManagedObject == null).
        } // if(managedObjectReference == null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "setManagedObject"
                       , new Object[] { managedObjectSet });
        return managedObjectSet;
    } // setManagedObject(). 

    /**
     * Make the token and any ManagedObject it refers to invalid.
     * Used at shutdown to prevent accidental use of a ManagedObject in the ObjectManager that
     * instantiated it or any other Object Manager.
     */
    void invalidate()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "invalidate"
                            );

        // Prevent any attempt to load the object.
        objectStore = null;

        // If the ManagedObject is already in memory access it, otherwise there is nothing
        // we need to do to it.
        if (managedObjectReference != null) {
            ManagedObject managedObject = (ManagedObject) managedObjectReference.get();
            if (managedObject != null)
                managedObject.state = ManagedObject.stateError;
        } // if (managedObjectReference != null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "invalidate"
                            );
    } // invalidate().

    /**
     * @return Returns the storedObjectIdentifier.
     */
    public long getStoredObjectIdentifier()
    {
        return storedObjectIdentifier;
    }

    /**
     * What ObjectStore stores this ManagedObject.
     * 
     * @return the ObjectStore that stores this ManagedObject.
     */
    public final ObjectStore getObjectStore()
    {
        return objectStore;
    } // getObjectStore().

    // --------------------------------------------------------------------------
    // Simplified serialization.
    // --------------------------------------------------------------------------

    private static final byte simpleSerialVersion = 0;

    // The serialized size of this.
    public static long maximumSerializedSize()
    {
        // Bytes in the serialized form.   
        return 1 // byte version.
        + 4 // int objectStoreIdentifier.
        + 8; // long storeObjectIdentifier.      
    }

//  /* (non-Javadoc)
//   * @see com.ibm.ws.objectManager.SimplifiedSerialization#estimatedLength()
//   */
//  public long estimatedLength() {
//    return maximumSerializedSize(); 
//  } // estimatedLength().

    /**
     * Simplified serialization.
     * 
     * @param buffer where the bytes are written.
     */
    protected final void writeSerializedBytes(ObjectManagerByteArrayOutputStream buffer) {
        buffer.write(simpleSerialVersion);
        buffer.writeInt(objectStoreIdentifier);
        buffer.writeLong(storedObjectIdentifier);
    } // writeSerializedBytes().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.SimplifiedSerialization#writeObject(java.io.DataOutputStream)
     */
    public final void writeObject(java.io.DataOutputStream dataOutputStream)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "writeObject"
                        , "dataOutputStream=" + dataOutputStream + "(java.io.DataOutputStream)"
                            );

        try {
            dataOutputStream.writeByte(simpleSerialVersion);
            dataOutputStream.writeInt(objectStoreIdentifier);
            dataOutputStream.writeLong(storedObjectIdentifier);

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "writeObject", exception, "1:326:1.14");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "writeObject",
                           exception);
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "writeObject"
                            );
    } // writeObject().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ManagedObject#readObject(java.io.DataInputStream, com.ibm.ws.objectManager.ObjectManagerState)
     */
    public final void readObject(java.io.DataInputStream dataInputStream,
                                 ObjectManagerState objectManagerState)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "readObject",
                        "dataInputStream="
                                        + dataInputStream
                                        + "(java.io.DataInputStream)"
                                        + " objectManagerState="
                                        + objectManagerState
                                        + "(ObjectManagerState)");

        // Use restore() in place of this to get the correct single instance of the token.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       "readObject",
                       "via UnsupportedOperationException");
        throw new UnsupportedOperationException();

    } // readObject().

    /**
     * Recover the token described by a dataInputStream and resolve it ot the definitive token.
     * 
     * @param dataInputStream containing the serialized Token.
     * @param objectManagerState of the objectManager reconstructing the Token.
     * @return Token matching the next bytes in the dataInputStream.
     * @throws ObjectManagerException
     */
    public static final Token restore(java.io.DataInputStream dataInputStream,
                                      ObjectManagerState objectManagerState)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(cclass
                        , "restore"
                        , new Object[] { dataInputStream, objectManagerState });

        int objectStoreIdentifier;
        long storedObjectIdentifier;
        try {
            byte version = dataInputStream.readByte();
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(cclass,
                            "restore",
                            new Object[] { new Byte(version) });

            objectStoreIdentifier = dataInputStream.readInt();
            storedObjectIdentifier = dataInputStream.readLong();

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(cclass, "restore", exception, "1:400:1.14");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(cclass,
                           "restore",
                           "via PermanentIOException");
            throw new PermanentIOException(cclass,
                                           exception);
        } // catch (java.io.IOException exception).

        Token tokenToReturn = new Token(objectManagerState.getObjectStore(objectStoreIdentifier),
                                        storedObjectIdentifier);

        // Swap for the definitive Token.
        // TODO should have a smarter version of like() which takes a storedObjectIdentifier, 
        //      instead of requiring a new Token().
        tokenToReturn = tokenToReturn.current();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(cclass
                       , "restore"
                       , "returns token=" + tokenToReturn + "(Token)"
                            );
        return tokenToReturn;
    } // restore(). 

    // --------------------------------------------------------------------------
    // implements java.io.Externalizable.
    // --------------------------------------------------------------------------

    private static final byte externalVersion = simpleSerialVersion;

    /**
     * No argument constructor.
     * Public so that java.io.Externalizable can use it.
     */
    public Token() {} // Token().

    /*
     * (non-Javadoc)
     * 
     * @see java.util.io.Externalizable.writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(java.io.ObjectOutput objectOutput)
                    throws java.io.IOException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "writeExternal"
                        , "objectOutput=" + objectOutput + "(java.io.ObjectOutput)"
                            );
        objectOutput.writeByte(externalVersion);
        objectOutput.writeInt(objectStoreIdentifier);
        objectOutput.writeLong(storedObjectIdentifier);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "writeExternal"
                            );
    } // Of writeExternal().

    /*
     * (non-Javadoc)
     * 
     * @see java.util.io.Externalizable.readExternal(java.io.ObjectInput)
     */
    public void readExternal(java.io.ObjectInput objectInput
                    )
                                    throws java.io.IOException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "readExternal"
                        , "objectInput=" + objectInput + "(java.io.ObjectInput)"
                            );

        byte version = objectInput.readByte();
        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
            trace.debug(cclass,
                        "readExternal",
                        "version=" + version + "(byte)");

        objectStoreIdentifier = objectInput.readInt();
        storedObjectIdentifier = objectInput.readLong();

        // Re-establish the objectStore storing the ManagedObject.
        try {
            objectStore = ((ManagedObjectInputStream) objectInput).objectManagerState.getObjectStore(objectStoreIdentifier);

        } catch (ObjectManagerException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "readExternal", exception, "1:488:1.14");
            java.io.IOException ioException = new java.io.IOException();

            // We cannot always invoke initCause, because JCLRM does not support this. 
            // ioException.initCause(exception);
            try {
                java.lang.reflect.Method initCauseMethod = Throwable.class.getMethod("initCause",
                                                                                     new Class[] { Throwable.class });
                initCauseMethod.invoke(this,
                                       new Object[] { exception });
            } catch (Exception exception2) {
                // No FFDC Code Needed.
                if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
                    trace.event(this,
                                cclass,
                                "readExternal",
                                exception2);
            }

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "readExternal"
                           , new Object[] { exception, new Integer(objectStoreIdentifier) }
                                );
            throw ioException;
        } // catch (ObjectManagerException exception).

        // ManagedObjectInputStream resolves the Token constructed into the definitive token.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "readExternal"
                            );
    } // readExternal().

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public final String toString()
    {
        return new String("Token"
                          + "(" + objectStoreIdentifier + "/" + storedObjectIdentifier + ")"
                          + "/" + Integer.toHexString(hashCode()));
    } // toString().
} // class Token.
