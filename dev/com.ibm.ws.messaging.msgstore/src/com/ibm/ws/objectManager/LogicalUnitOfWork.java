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
 * <p>LogicalUnitOfWork identifies the atomic unit of work that must
 * either commit or backout completely.
 * 
 * @author IBM Corporation
 */
public class LogicalUnitOfWork
                implements java.io.Serializable, SimplifiedSerialization
{
    private static final Class cclass = LogicalUnitOfWork.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_TRAN);
    private static final long serialVersionUID = -3343809099276071578L;

    //!! Should realy be private. JQM accesses this. 
    public long identifier; // Fixed for this unit of work.
    protected byte[] XID; // Xopen XID.
    // The Xopen definition of an XID contains BQUAL maximum size 64, GTRID maximum size 64, their lengths (int)
    // and a format ID (int) that makes 64+64+8+8+8=152 bytes.
    protected static final int maximumXIDSize = 255; // Length is serialized as one unsigned byte 0-255.

    /**
     * Constructor
     * 
     * @param identifier as assigned by the ObjectManager to the unit of work.
     */
    protected LogicalUnitOfWork(long identifier)
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, "<init>", new Long(identifier));

        this.identifier = identifier;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, "<init>");
    } // LogicalUnitOfWork().

    /**
     * Constructor makes a LogicalUnitOfWork from the next bytes in a ByteBuffer.
     * 
     * @param dataInputStream containing the Token.
     */

    protected LogicalUnitOfWork(java.io.DataInputStream dataInputStream) throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, "<init>", "DataInputStream=" + dataInputStream);

        readObject(dataInputStream, null);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, "<init>");
    } // LogicalUnitOfwork().

    // --------------------------------------------------------------------------
    // Simplified serialization.
    // --------------------------------------------------------------------------

    private static final byte SimpleSerialVersion = 0;

    /**
     * @return the maximum serialized size in bytes
     */
    protected static long maximumSerializedSize()
    {
        return 1 // Version
        + 8 // Identifier.
        + 1 // XID length.
        + maximumXIDSize;

    } // maximumSerializedSize().

//  /* (non-Javadoc)
//   * @see com.ibm.ws.objectManager.SimplifiedSerialization#estimatedLength()
//   */
//  public long estimatedLength() {
//    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) 
//      trace.entry(this,cclass, "serializedBytesLength");
//    
//    int length = (int)maximumSerializedSize() - maximumXIDSize; // Length of the serialized form.
//    if (XID != null )
//      length = length + XID.length;
//    
//    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) 
//      trace.exit(this,cclass,
//                 "serializedBytesLength",
//                 new Integer(length));
//    return length;
//  } // estimatedLength().

    /**
     * @param ObjectManagerByteArrayOutputStream where the bytes are written.
     */
    protected void writeSerializedBytes(ObjectManagerByteArrayOutputStream buffer)
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "writeSerializedBytes",
                        new Object[] { buffer }
                            );

        buffer.write(SimpleSerialVersion);

        buffer.writeLong(identifier);

        if (XID == null)
        {
            buffer.write(0);
        }
        else
        { // Non null XID.
            buffer.write((byte) (XID.length >>> 0));
            buffer.write(XID, 0, XID.length);
        } // if (XID== null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "writeSerializedBytes",
                       new Object[] { buffer }
                            );
    } // writeSerializedBytes().

    /**
     * Simplified serialization.
     * 
     * @param java.io.DataOutputStream to write the serialized Object into.
     */
    public void writeObject(java.io.DataOutputStream dataOutputStream)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass, "writeObject"
                        , "dataOutputStream=" + dataOutputStream);

        try
        {
            dataOutputStream.writeByte(SimpleSerialVersion);
            dataOutputStream.writeLong(identifier);
            if (XID == null)
            {
                dataOutputStream.writeByte(0);
            }
            else
            { // Non null XID.
                dataOutputStream.writeByte(XID.length);
                dataOutputStream.write(XID);
            } // if (XID == null).

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "writeObject", exception, "1:177:1.9");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, "writeObject");
            throw new PermanentIOException(this, exception);
        } // catch (java.io.IOException exception).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, "writeObject");
    } // writeObject().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.ManagedObject#readObject(java.io.DataInputStream, com.ibm.ws.objectManager.ObjectManagerState)
     */
    public void readObject(java.io.DataInputStream dataInputStream,
                           ObjectManagerState objectManagerState)
                    throws ObjectManagerException {
        final String methodName = "readObject";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { dataInputStream, objectManagerState });

        try {
            byte version = dataInputStream.readByte();
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this, cclass,
                            methodName,
                            "version=" + version + "(byte)");

            identifier = dataInputStream.readLong();
            int XIDLength = dataInputStream.readUnsignedByte();
            if (XIDLength > 0)
            {
                XID = new byte[XIDLength];
                dataInputStream.read(XID);
            } // if (XIDLength > 0).

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:217:1.9");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           methodName,
                           "via PermanentIOException");
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // readObject().

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return new String("LogicalUnitOfWork"
                          + "(" + identifier + ")"
                          + "/" + Integer.toHexString(hashCode()));
    } // toString().
} // class LogicalUnitOfWork.
