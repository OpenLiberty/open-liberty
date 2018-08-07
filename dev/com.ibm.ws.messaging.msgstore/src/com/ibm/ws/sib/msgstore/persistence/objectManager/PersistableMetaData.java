package com.ibm.ws.sib.msgstore.persistence.objectManager;

/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.objectManager.ManagedObject;
import com.ibm.ws.objectManager.ObjectManagerException;
import com.ibm.ws.objectManager.ObjectManagerState;
import com.ibm.ws.objectManager.SimplifiedSerialization;
import com.ibm.ws.objectManager.Token;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.persistence.TupleTypeEnum;
import com.ibm.ws.sib.utils.ras.SibTr;

public class PersistableMetaData extends ManagedObject implements SimplifiedSerialization
{
	// The initial version in Liberty was V1 so the V0 version should only be present in Traditional WAS.
	private static final long serialVersionUIDV0 = 7600601884916750783L;
	// V1 adds _redeliveredCount 
	// and _deliveryDelayTime in releases 8.5.6 and higher.
	private static final long serialVersionUIDV1 = 7600601884916130911L;
	// V2 adds _deliveryDelayTime.
	private static final long serialVersionUID = 2L;

    private static TraceComponent tc = SibTr.register(PersistableMetaData.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    private long _uniqueID;
    private long _streamID;
    private long _lockID = AbstractItem.NO_LOCK_ID;
    private long _referredID = AbstractItem.NO_ID;
    private long _sequence;
    private long _expiryTime;
    private int _redeliveredCount;
    private int _storageStrategy = AbstractItem.STORE_NEVER;
    private int _priority;
    private int _persistentSize;
    private int _maxDepth;
    private boolean _canExpireSilently;
    private String _type;
    private String _className;
    private byte[] _transactionId;
    private long _deliveryDelayTime;
    private transient boolean _deliveryDelayTimeIsSuspect = false;

    // As this flag is no longer used by the
    // persistence mechanism it doesn't need to
    // be persisted.
    private transient boolean _logicallyDeleted;

    // Tokens for the associated objects of this persistable
    private Token _streamListToken;
    private Token _itemListToken;
    private Token _rawDataToken;

    // Used to quickly check if a stream contains any
    // expirable items.
    private boolean _containsExpirables;

    // These are the entries for this persistable
    // in it's parents item and stream lists.
    private Token _streamListEntryToken;
    private Token _itemListEntryToken;

    // Defect 454302
    private transient long _estimatedLength = -1;
    private static long _estimatedLengthHeader;
    static
    {
        try
        {
            // Used to esimate the length of future attempts to
            // serialize the Object.
            class DummyOutputStream extends java.io.OutputStream
            {
                @Override
                public void write(int b) throws java.io.IOException {}
            } // class DummyOutputStream.

            DataOutputStream dataOutputStream = new DataOutputStream(new DummyOutputStream());

            // The classname is the major part of the header
            // created by the object manager layer.
            dataOutputStream.writeUTF(new PersistableMetaData().getClass().getName());

            _estimatedLengthHeader = dataOutputStream.size();

            dataOutputStream.close();
            dataOutputStream = null;

            // Lets give ourselves some legroom in case this
            // value isn't as accurate as we hoped. It is better
            // to "waste" 64 bytes here than under-estimate and
            // have to re-allocate and copy to a new larger array.
            _estimatedLengthHeader += 64;
        } catch (Exception e)
        {
            // No FFDC Code Needed.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(tc, "Exception caught initialising _estimatedLengthHeader!", e);
            // If we can't get an accurate number then lets give a number
            // that if anything is a bit of an over estimation as this should
            // hopefully avoid us under-sizing the array created at serialize
            // time and cut down on re-allocations.
            _estimatedLengthHeader = 150;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "_estimatedLengthHeader=" + _estimatedLengthHeader);
    } // static initializer.

    // Defect 454302
    // Empty constructor needed for de-serialization
    public PersistableMetaData() {}

    public PersistableMetaData(long uniqueID, long streamID, String type)
    {
        _uniqueID = uniqueID;
        _streamID = streamID;
        _type = type;
    }

    // Defect 542362
    // This constructor is used to create a new meta data object
    // from one which was previously added to the object manager
    // but then rolled back. 
    public PersistableMetaData(PersistableMetaData donor)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", "Donor=" + donor);

        // Take a copy of all the data on the donor persistable except
        // the Tokens as they should be null before this new persistable
        // is added to the file store.
        _uniqueID = donor._uniqueID;
        _streamID = donor._streamID;
        _type = donor._type;
        _lockID = donor._lockID;
        _referredID = donor._referredID;
        _sequence = donor._sequence;
        _expiryTime = donor._expiryTime;
        _storageStrategy = donor._storageStrategy;
        _priority = donor._priority;
        _persistentSize = donor._persistentSize;
        _maxDepth = donor._maxDepth;
        _canExpireSilently = donor._canExpireSilently;
        _type = donor._type;
        _className = donor._className;
        _transactionId = donor._transactionId;
        _containsExpirables = donor._containsExpirables;
        _redeliveredCount = donor._redeliveredCount;
        _deliveryDelayTime = donor._deliveryDelayTime;

        // Even take a copy of the transient data
        // as it is now our transient data.
        _logicallyDeleted = donor._logicallyDeleted;
        _estimatedLength = donor._estimatedLength;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>", this);
    }

    public Token getStreamListEntryToken()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getStreamListEntryToken");
            SibTr.exit(this, tc, "getStreamListEntryToken", "return=" + _streamListEntryToken);
        }
        return _streamListEntryToken;
    }

    public void setStreamListEntryToken(Token token)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setStreamListEntryToken", "Token=" + token);

        _streamListEntryToken = token;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setStreamListEntryToken");
    }

    public Token getItemListEntryToken()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getItemListEntryToken");
            SibTr.exit(this, tc, "getItemListEntryToken", "return=" + _itemListEntryToken);
        }
        return _itemListEntryToken;
    }

    public void setItemListEntryToken(Token token)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setItemListEntryToken", "Token=" + token);

        _itemListEntryToken = token;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setItemListEntryToken");
    }

    public Token getStreamListToken()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getStreamListToken");
            SibTr.exit(this, tc, "getStreamListToken", "return=" + _streamListToken);
        }
        return _streamListToken;
    }

    public void setStreamListToken(Token token)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setStreamListToken", "Token=" + token);

        _streamListToken = token;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setStreamListToken", "");
    }

    public Token getItemListToken()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getItemListToken");
            SibTr.exit(this, tc, "getItemListToken", "return=" + _itemListToken);
        }
        return _itemListToken;
    }

    public void setItemListToken(Token token)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setItemListToken", "Token=" + token);

        _itemListToken = token;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setItemListToken");
    }

    public Token getRawDataToken()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getRawDataToken");
            SibTr.exit(this, tc, "getRawDataToken", "return=" + _rawDataToken);
        }
        return _rawDataToken;
    }

    public void setRawDataToken(Token token)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setRawDataToken", "Token=" + token);

        _rawDataToken = token;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setRawDataToken");
    }

    public void setCanExpireSilently(boolean canExpireSilently)
    {
        _canExpireSilently = canExpireSilently;
    }

    public boolean canExpireSilently()
    {
        return _canExpireSilently;
    }

    public void setClassName(String className)
    {
        _className = className;
    }

    public String getClassName()
    {
        return _className;
    }

    public void setContainsExpirables(boolean containsExpirables)
    {
        _containsExpirables = containsExpirables;
    }

    public boolean containsExpirables()
    {
        return _containsExpirables;
    }

    public void setExpiryTime(long expiryTime)
    {
        _expiryTime = expiryTime;
    }

    public long getExpiryTime()
    {
        return _expiryTime;
    }

    public void setRedeliveredCount(int redeliveredCount)
    {
        _redeliveredCount = redeliveredCount;
    }

    public int getRedeliveredCount()
    {
        return _redeliveredCount;
    }

    public void setLockID(long lockID)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setLockID", "LockID=" + lockID);

        _lockID = lockID;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setLockID");
    }

    public long getLockID()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getLockID");
            SibTr.exit(this, tc, "getLockID", "return=" + _lockID);
        }
        return _lockID;
    }

    public void setLogicallyDeleted(boolean logicallyDeleted)
    {
        _logicallyDeleted = logicallyDeleted;
    }

    public boolean isLogicallyDeleted()
    {
        return _logicallyDeleted;
    }

    public void setMaxDepth(int maxDepth)
    {
        _maxDepth = maxDepth;
    }

    public int getMaxDepth()
    {
        return _maxDepth;
    }

    public void setPersistentSize(int persistentSize)
    {
        _persistentSize = persistentSize;
    }

    public int getPersistentSize()
    {
        return _persistentSize;
    }

    public void setPriority(int priority)
    {
        _priority = priority;
    }

    public int getPriority()
    {
        return _priority;
    }

    public void setReferredID(long referredID)
    {
        _referredID = referredID;
    }

    public long getReferredID()
    {
        return _referredID;
    }

    public void setSequence(long sequence)
    {
        _sequence = sequence;
    }

    public long getSequence()
    {
        return _sequence;
    }

    public void setStorageStrategy(int storageStrategy)
    {
        _storageStrategy = storageStrategy;
    }

    public int getStorageStrategy()
    {
        return _storageStrategy;
    }

    public long getStreamId()
    {
        return _streamID;
    }

    public void setTransactionId(byte[] transactionId)
    {
        _transactionId = transactionId;
    }

    public byte[] getTransactionId()
    {
        return _transactionId;
    }

    public TupleTypeEnum getTupleType()
    {
        return TupleTypeEnum.getInstance(_type);
    }

    public long getUniqueId()
    {
        return _uniqueID;
    }

    @Override
    public String toString()
    {
        // Defect 292187
        // include the super implementation to ensure
        // inclusion of the object id.
        StringBuffer buffer = new StringBuffer(super.toString());

        buffer.append("(PersistableMetaData[ uniqueId: ");
        buffer.append(_uniqueID);
        buffer.append(", containingStreamId: ");
        buffer.append(_streamID);
        buffer.append(", className: ");
        buffer.append(_className);
        buffer.append(", dataSize: ");
        buffer.append(_persistentSize);
        buffer.append(", storageStrategy: ");
        buffer.append(_storageStrategy);
        buffer.append(", tupleType: ");
        buffer.append(_type);
        buffer.append(", maximumDepth: ");
        buffer.append(_maxDepth);
        buffer.append(", priority: ");
        buffer.append(_priority);
        buffer.append(", sequence: ");
        buffer.append(_sequence);
        buffer.append(", canExpireSilently: ");
        buffer.append(_canExpireSilently);
        buffer.append(", lockId: ");
        buffer.append(_lockID);
        buffer.append(", referredId: ");
        buffer.append(_referredID);
        buffer.append(", expiryTime: ");
        buffer.append(_expiryTime);
        buffer.append(", logicallyDeleted: ");
        buffer.append(_logicallyDeleted);
        buffer.append(", transactionId: ");
        buffer.append(Arrays.toString(_transactionId));
        buffer.append(", rawDataToken: ");
        buffer.append(_rawDataToken);
        buffer.append(", itemListToken: ");
        buffer.append(_itemListToken);
        buffer.append(", streamListToken: ");
        buffer.append(_streamListToken);
        buffer.append(", redeliveredCount: ");
        buffer.append(_redeliveredCount);
        buffer.append(", deliveryDelayTime: ");
        buffer.append(_deliveryDelayTime);
        buffer.append(" ])");

        return buffer.toString();
    }

    /**
     * Replace the state of this object with the same object in some other state.
     * Used to restore the before image if a transaction rolls back.
     * 
     * @param other is the object this object is to become a clone of.
     */
    @Override
    public void becomeCloneOf(ManagedObject clone)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "becomeCloneOf", "Clone=" + clone);

        PersistableMetaData theOther = (PersistableMetaData) clone;

        _uniqueID = theOther._uniqueID;
        _streamID = theOther._streamID;
        _lockID = theOther._lockID;
        _referredID = theOther._referredID;
        _sequence = theOther._sequence;
        _expiryTime = theOther._expiryTime;
        _storageStrategy = theOther._storageStrategy;
        _priority = theOther._priority;
        _persistentSize = theOther._persistentSize;
        _maxDepth = theOther._maxDepth;
        _canExpireSilently = theOther._canExpireSilently;
        _logicallyDeleted = theOther._logicallyDeleted;
        _type = theOther._type;
        _className = theOther._className;
        _transactionId = theOther._transactionId;
        _streamListToken = theOther._streamListToken;
        _itemListToken = theOther._itemListToken;
        _rawDataToken = theOther._rawDataToken;
        _containsExpirables = theOther._containsExpirables;
        _redeliveredCount = theOther._redeliveredCount;
        _deliveryDelayTime = theOther._deliveryDelayTime;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "becomeCloneOf");
    }

    // Defect 454302
    /*************************************************************************/
    /* SimplifiedSerialization */
    /*************************************************************************/

    /**
     * @return The combined length of all DataSlice objects contained
     *         in this object.
     */
    @Override
    public long estimatedLength()
    {
        if (_estimatedLength == -1)
        {
            // Start with the value for the header that our
            // superclass will create and then add on the size
            // for our header:
            //   Superclass header
            //   long   _serialVersionUID      = 8 bytes
            _estimatedLength = _estimatedLengthHeader + 8;

            // Add on the sizes for our member variables:
            //
            //   long    _uniqueID             = 8
            //   long    _streamID             = 8
            //   long    _lockID               = 8
            //   long    _referredID           = 8
            //   long    _sequence             = 8
            //   long    _expiryTime           = 8
            //   long    _deliveryDelayTime    = 8
            _estimatedLength += 56;

            //   int     _storageStrategy      = 4
            //   int     _priority             = 4
            //   int     _persistentSize       = 4
            //   int     _maxDepth             = 4
            //   int     _redeliveredCount     = 4
            _estimatedLength += 20;

            //   boolean _canExpireSilently    = 1
            //   boolean _containsExpirables   = 1
            _estimatedLength += 2;

            // For the strings we are using writeUTF so we are
            // being pessimistic and saying the length will be
            // 3 * the number of characters
            //   String  _type                 = _type.length * 3
            //   String  _className            = _className.length * 3
            if (_type != null)
            {
                _estimatedLength += (_type.length() * 3);
            }

            if (_className != null)
            {
                _estimatedLength += (_className.length() * 3);
            }

            //   int     _transactionId.length = 4
            //   byte[]  _transactionId        = _transactionId.length
            _estimatedLength += 4;
            if (_transactionId != null)
            {
                _estimatedLength += _transactionId.length;
            }

            //   boolean _streamList exists?   = 1
            //   boolean _itemList exists?     = 1
            //   boolean _rawData exists?      = 1
            //   boolean _streamEntry exists?  = 1
            //   boolean _itemEntry exists?    = 1
            //   Token   _streamListToken      = Token.maximumSerializedBytes()
            //   Token   _itemListToken        = Token.maximumSerializedBytes()
            //   Token   _rawDataToken         = Token.maximumSerializedBytes()
            //   Token   _streamListEntryToken = Token.maximumSerializedBytes()
            //   Token   _itemListEntryToken   = Token.maximumSerializedBytes()
            _estimatedLength += 5;
            if (_streamListToken != null)
            {
                _estimatedLength += Token.maximumSerializedSize();
            }
            if (_itemListToken != null)
            {
                _estimatedLength += Token.maximumSerializedSize();
            }
            if (_rawDataToken != null)
            {
                _estimatedLength += Token.maximumSerializedSize();
            }
            if (_streamListEntryToken != null)
            {
                _estimatedLength += Token.maximumSerializedSize();
            }
            if (_itemListEntryToken != null)
            {
                _estimatedLength += Token.maximumSerializedSize();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "_estimatedLength=" + _estimatedLength);
        return _estimatedLength;
    }

    /**
     * Simplified serialization.
     * 
     * @param dataOutputStream
     *            to write the serialized data to.
     * 
     * @exception ObjectManagerException
     * @exception IOException
     */
    @Override
    public void writeObject(DataOutputStream dataOutputStream) throws ObjectManagerException, IOException
    {
        super.writeObject(dataOutputStream);

        // Write out our serialVersionUID
        dataOutputStream.writeLong(serialVersionUID);

        dataOutputStream.writeLong(_uniqueID);
        dataOutputStream.writeLong(_streamID);
        dataOutputStream.writeLong(_lockID);
        dataOutputStream.writeLong(_referredID);
        dataOutputStream.writeLong(_sequence);
        dataOutputStream.writeLong(_expiryTime);
        dataOutputStream.writeInt(_redeliveredCount);
        dataOutputStream.writeInt(_storageStrategy);
        dataOutputStream.writeInt(_priority);
        dataOutputStream.writeInt(_persistentSize);
        dataOutputStream.writeInt(_maxDepth);

        dataOutputStream.writeBoolean(_canExpireSilently);
        dataOutputStream.writeBoolean(_containsExpirables);

        // If the string variables aren't null then
        // write the flag and then write them to the
        // stream.
        if (_type != null)
        {
            dataOutputStream.writeBoolean(true);
            dataOutputStream.writeUTF(_type);
        }
        else
        {
            dataOutputStream.writeBoolean(false);
        }
        if (_className != null)
        {
            dataOutputStream.writeBoolean(true);
            dataOutputStream.writeUTF(_className);
        }
        else
        {
            dataOutputStream.writeBoolean(false);
        }

        // We need to write out the length of the tran ID
        // before we write the bytes so that we can read
        // it back in successfully
        if (_transactionId != null)
        {
            dataOutputStream.writeInt(_transactionId.length);
            dataOutputStream.write(_transactionId, 0, _transactionId.length);
        }
        else
        {
            dataOutputStream.writeInt(-1);
        }

        // Call the Tokens to write themselves out but
        // only if they exist. If the don't then we need
        // to write a boolean to the stream so that we
        // don't try and read back in non-existent tokens.
        if (_streamListToken != null)
        {
            dataOutputStream.writeBoolean(true);
            _streamListToken.writeObject(dataOutputStream);
        }
        else
        {
            dataOutputStream.writeBoolean(false);
        }

        if (_itemListToken != null)
        {
            dataOutputStream.writeBoolean(true);
            _itemListToken.writeObject(dataOutputStream);
        }
        else
        {
            dataOutputStream.writeBoolean(false);
        }

        if (_rawDataToken != null)
        {
            dataOutputStream.writeBoolean(true);
            _rawDataToken.writeObject(dataOutputStream);
        }
        else
        {
            dataOutputStream.writeBoolean(false);
        }

        if (_streamListEntryToken != null)
        {
            dataOutputStream.writeBoolean(true);
            _streamListEntryToken.writeObject(dataOutputStream);
        }
        else
        {
            dataOutputStream.writeBoolean(false);
        }

        if (_itemListEntryToken != null)
        {
            dataOutputStream.writeBoolean(true);
            _itemListEntryToken.writeObject(dataOutputStream);
        }
        else
        {
            dataOutputStream.writeBoolean(false);
        }

        // write the _deliveryDelayTime
        dataOutputStream.writeLong(_deliveryDelayTime);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "bytes written==" + dataOutputStream.size());
    }

    /**
     * Simplified deserialization.
     * 
     * @param dataInputStream
     *            containing the serialized Object.
     * @param objectManagerState
     *            of the objectManager reconstructing the serialized Object.
     * 
     * @exception ObjectManagerException
     * @exception IOException
     */
    @Override
    public void readObject(DataInputStream dataInputStream, ObjectManagerState objectManagerState) throws ObjectManagerException, IOException
    {
    	final String methodName = "readObject";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, methodName, "bytes available==" + dataInputStream.available());

        super.readObject(dataInputStream, objectManagerState);

        long serialVersionUIDRead = dataInputStream.readLong();
       
        _uniqueID = dataInputStream.readLong();
        _streamID = dataInputStream.readLong();
        _lockID = dataInputStream.readLong();
        _referredID = dataInputStream.readLong();
        _sequence = dataInputStream.readLong();
        _expiryTime = dataInputStream.readLong();

        // redeliveredCount was added for serialVersionUIDV1 and later.
        // otherwise set it to 0.
        if (serialVersionUIDRead == serialVersionUIDV0)
          _redeliveredCount = 0;
        else
          _redeliveredCount = dataInputStream.readInt();

        _storageStrategy = dataInputStream.readInt();
        _priority = dataInputStream.readInt();
        _persistentSize = dataInputStream.readInt();
        _maxDepth = dataInputStream.readInt();

        _canExpireSilently = dataInputStream.readBoolean();
        _containsExpirables = dataInputStream.readBoolean();

        // Read the strings in but only if they were
        // not null on the write.
        boolean read = dataInputStream.readBoolean();
        if (read)
        {
            _type = dataInputStream.readUTF();
        }
        read = dataInputStream.readBoolean();
        if (read)
        {
            _className = dataInputStream.readUTF();
        }

        // In order to read in the tran ID we need to read
        // its length so we can create the byte array.
        int length = dataInputStream.readInt();

        if (length != -1)
        {
            _transactionId = new byte[length];

            dataInputStream.readFully(_transactionId, 0, length);
        }

        // Call the Tokens to read themselves in but
        // only if the flag preceding them says that
        // they exist in the stream.
        read = dataInputStream.readBoolean();
        if (read)
        {
            _streamListToken = Token.restore(dataInputStream, objectManagerState);
        }
        read = dataInputStream.readBoolean();
        if (read)
        {
            _itemListToken = Token.restore(dataInputStream, objectManagerState);
        }
        read = dataInputStream.readBoolean();
        if (read)
        {
            _rawDataToken = Token.restore(dataInputStream, objectManagerState);
        }
        read = dataInputStream.readBoolean();
        if (read)
        {
            _streamListEntryToken = Token.restore(dataInputStream, objectManagerState);
        }
        read = dataInputStream.readBoolean();
        if (read)
        {
            _itemListEntryToken = Token.restore(dataInputStream, objectManagerState);
        }

        // _deliveryDelayTime was added for serialVersionUID=2 and serialVersionUIDV1 
        // in releases 8.5.6 and higher.
        if (serialVersionUIDRead == serialVersionUIDV0) {
        	_deliveryDelayTime = 0;
        } else if (serialVersionUIDRead == serialVersionUIDV1) {
        	
        	// _deliveryDelayTime was initially added without changing serialVersionUID, so 
        	// PersistableMetaData was stored using serialVersionUIDV1 and may or may not contain 
        	// _deliveryDelayTime. If there are fewer than 8 bytes remaining in the dataInputStream 
        	// then we can safely assume the _deliveryDelayTime was not stored.
            if (dataInputStream.available() >= 8 && _className.equals("com.ibm.ws.sib.processor.impl.store.items.MessageItem")) {
                _deliveryDelayTime = dataInputStream.readLong();
                // We could have just read garbage from spare space in the ObjectStore.
                _deliveryDelayTimeIsSuspect = true;
            }
            else 
        	    _deliveryDelayTime = 0;  
        } else {
        	_deliveryDelayTime = dataInputStream.readLong();
        }

        // Reset _estimatedLength so that it is re-calculated
        // when it is needed.
        _estimatedLength = -1;
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, methodName, "serialVersionUIDRead="+serialVersionUIDRead+" bytes available==" + dataInputStream.available());
    }

    public void setDeliveryDelayTime(long deliveryDelayTime) {
        this._deliveryDelayTime = deliveryDelayTime;

    }

    public long getDeliveryDelayTime() {
        return this._deliveryDelayTime;
    }

	public boolean getDeliveryDelayTimeIsSuspect() {
		return _deliveryDelayTimeIsSuspect;
	}

    /*************************************************************************/
    /* SimplifiedSerialization */
    /*************************************************************************/
}
