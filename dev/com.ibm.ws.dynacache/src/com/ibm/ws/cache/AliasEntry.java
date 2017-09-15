/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.ibm.ws.cache.util.ObjectSizer;
import com.ibm.ws.cache.util.SerializationUtility;

public class AliasEntry extends CacheEntry {

    private static final long serialVersionUID = 9023191210978622958L;

    public static final String ADD_ALIAS = "AddAlias";
    public static final String REMOVE_ALIAS = "RemoveAlias";

    public AliasEntry() {}

    public AliasEntry(
                      Object id,
                      Object value,
                      int sharingPolicy,
                      Object[] aliasArray) {
        this.id = id;
        this.sharingPolicy = sharingPolicy;
        this.aliasList = aliasArray;
        setValue(value);
    }

    public Object[] getAliasArray() {
        return aliasList;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        if (!prepareForSerialization()) {
            // Should never see this. If you do it is because
            // some previos code is missing a call to
            // prepareForSerialization().
            throw new IOException("Object not serializable: " + (value == null ? "null object" : value.getClass().getName()));
        }
        if (serializedId != null) {
            out.writeInt(serializedId.length);
            out.write(serializedId);
        } else {
            out.writeInt(-1);
        }
        if (serializedValue != null) {
            out.writeInt(serializedValue.length);
            out.write(serializedValue);
        } else {
            out.writeInt(-1);
        }
        out.writeInt(timeLimit);
        out.writeLong(expirationTime);
        out.writeLong(timeStamp);
        out.writeLong(drsClock);
        out.writeInt(sharingPolicy);
        if (serializedAliasList != null) {
            out.writeInt(serializedAliasList.length);
            for (int i = 0; i < serializedAliasList.length; i++) {
                byte[] aid = (byte[]) serializedAliasList[i];
                out.writeInt(aid.length);
                out.write(aid);
            }
        } else {
            out.writeInt(-1);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        try {
            int keyLength = in.readInt();
            if (keyLength > 0) {
                serializedId = new byte[keyLength];
                in.readFully(serializedId);
            } else {
                serializedId = null;
            }

            if (serializedId != null) {
                try {
                    id = SerializationUtility.deserialize(serializedId, cacheName);
                } catch (Exception ex) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.AliasEntry.readExternal", "366", this);
                }
            } else {
                id = null;
            }

            int len = in.readInt();
            if (len > 0) {
                serializedValue = new byte[len];
                in.readFully(serializedValue);
            } else {
                serializedValue = null;
            }
            value = null;
            timeLimit = in.readInt();
            expirationTime = in.readLong();
            timeStamp = in.readLong();
            drsClock = in.readLong();
            sharingPolicy = in.readInt();
            int size = in.readInt();
            if (size > 0) {
                serializedAliasList = new Object[size];
                for (int i = 0; i < size; i++) {
                    len = in.readInt();
                    serializedAliasList[i] = new byte[len];
                    in.readFully((byte[]) serializedAliasList[i]);
                }
            } else {
                serializedAliasList = null;
            }
            if (serializedAliasList != null) {
                aliasList = new Object[serializedAliasList.length];
                try {

                    for (int i = 0; i < serializedAliasList.length; i++) {
                        aliasList[i] = SerializationUtility.deserialize((byte[]) serializedAliasList[i], cacheName);
                    }
                    serializedAliasList = null;
                } catch (Exception ex) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.AliasEntry.readExternal", "428", this);
                    aliasList = EMPTY_OBJECT_ARRAY;
                }
            } else {
                aliasList = EMPTY_OBJECT_ARRAY;
            }
        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.AliasEntry.readExternal", "468", this);
        }
    }

    /**
     * @return estimate (serialized) size of AliasEntry. It is called by DRS to calculate the payload.
     */
    @Override
    public long getSerializedSize() {
        long totalSize = 0;
        if (this.serializedId != null) {
            totalSize += ObjectSizer.getSize(this.serializedId);
        }
        if (this.serializedValue != null) {
            totalSize += ObjectSizer.getSize(this.serializedValue);
        }
        if (this.serializedAliasList != null) {
            if (this.serializedAliasList.length > 0) {
                for (int i = 0; i < this.serializedAliasList.length; i++) {
                    totalSize += ObjectSizer.getSize(this.serializedAliasList[i]);
                }
            }
        }
        return totalSize;
    }
}
