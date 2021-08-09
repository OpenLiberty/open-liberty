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

/**
 * A simplified form of java.io.Serializable.
 * 
 * ManagedObjects may implement SimplifiedSerialization to produce more compact serialized Objects, and also allow the
 * ObjectManager to allocate buffer space for the serialized Objects in a more efficiently. Unlike java.io.Serializable,
 * SimplifiedSerialization requires that <code>readObject</code> and <code>writeObject</code> be implemented and
 * invoke the SuperClass. There is no requirement to write a serialVersionUID but it is good practice to include a
 * version in the serialized data. It is recommended that the SuperClass is invoked immedately after reading the version
 * of the serialized object.
 * <p>
 * ManagedObjects implementing SimplifiedSerialization may also implement the method
 * <code>long ManagedObject.estimatedLength()</code>. which gives a hint as to how much buffer space should be
 * allocated to contain the serialized ManagedObject. If this method is not implemented the ObjectManager assumes that
 * the serialzedObject is small and adds only 10-100 bytes to the base size. If the computation of the actual size, is
 * complex it may be better to compute the size of a modest overestimate of the this can be done quickly.
 * <p>
 * The method <code>long ManagedObject.getSerializedBytesLength()</code> can be used to get an accurate value for the
 * serialized size, it does so by serializing the Object but without allocating the actual buffer to save the result. It
 * is relatively expensive to invoke this method each time an Object is serialized, however a static initialiser can be
 * used to create a baseline figure for the serialized size and the correct value computed from that.
 * <p>
 * Token implements the method <code>long Token.maximumSerializedBytes()</code> which gives the maximum number of
 * bytes that a <code>Token.WriteObject(dataOutputStream)</code> invocation will add to the serialzed size. A token
 * written in this way can be restored using <code>Token restore(java.io.DataInputStream,ObjectManagerState)</code>.
 */
public interface SimplifiedSerialization
{
    static final int signature_DefaultSerialization = 0;
    // static final int signature_Token = 1; 
    // static final int signature_ManagedObject = 2;
    static final int signature_LogicalUnitOfWork = 3;
    static final int signature_ObjectManagerState = 4;
    static final int signature_LinkedList = 5;
    static final int signature_LinkedList_Link = 6;
    static final int signature_ConcurrentLinkedList = 7;
    static final int signature_ConcurrentSublist = 8;
    static final int signature_ConcurrentSublist_Link = 9;
    static final int signature_TreeMap = 10;
    static final int signature_TreeMap_Entry = 11;
    // Generic, non specific ManagedObject.
    static final int signature_Generic = 12;

    // getSignature is not part of the public interface because only ObjectManager ManagedObjects
    // can use the abreviated signature.
//  /**
//   * @return int the Signature of the Object for SimplifiedSerialization.
//   */
//  int getSignature();

//  /* 
//   * Gives an estimate of the size of the serialized form of the Object. An over estimate
//   * means that buffer space is wasted. An under estimate means that larger buffers might have
//   * to be allocated and the data copied. If an exact size cannot be given then it is best to 
//   * return a conservative over estimate.
//   *  
//   * @return long the estimated number of bytes in the SimplifiedSerialization form.
//   */
//  long estimatedLength(); 

    /**
     * Simplified serialization.
     * 
     * @param dataOutputStream to write the serialized tata to.
     *            to serialize the Object to.
     * @exception ObjectManagerException.
     */
    void writeObject(java.io.DataOutputStream dataOutputStream)
                    throws ObjectManagerException, java.io.IOException;

    /**
     * Simplified deserialization.
     * 
     * @param dataInputStream containing the serialized Object.
     * @param objectManagerState of the objectManager reconstructing the serialized Object.
     * @exception ObjectManagerException.
     */
    void readObject(java.io.DataInputStream dataInputStream,
                    ObjectManagerState objectManagerState)
                    throws ObjectManagerException, java.io.IOException;

} // Of interface SimplifiedSerialization.