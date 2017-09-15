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
package com.ibm.ws.sib.mfp.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.ibm.ws.sib.mfp.JsMessageHandle;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.util.ArrayUtil;
import com.ibm.ws.sib.utils.HexString;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 *  JsMessageHandleImpl implements JsMessageHandle and hence
 *  SIMessageHandle.
 */
final class JsMessageHandleImpl implements JsMessageHandle, Serializable {

  private final static long serialVersionUID = 1L;

  // Version number for flattening/restoring compatibility
  private final static short versionNumber = 1;

  private final static TraceComponent tc = SibTr.register(JsMessageHandleImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  /* Instance variables */
  private transient SIBUuid8 uuid;  // Has to be transient because SIBUuid8 is not serializable.
  private Long value;               // This can never be null because a message defaults to 0.
  private transient int hashcode;   // Only set if hashCode is called.

  /* **************************************************************************/
  /* Constructors                                                             */
  /* **************************************************************************/

  /**
   *  Constructor for a new JsMessageHandleImpl
   *  This constructor should only be called by the JsMessageHandleFactoryImpl
   *  create methods and JsHdrsImpl.getMessageHandle().
   *  A JsMessageHandleImpl should never be instantiated directly.
   *
   *  @param uuid        The SystemMessageSourceUuid of a message.
   *  @param value       The Long SystemMessageValue of the same message.
   *
   */
  JsMessageHandleImpl(SIBUuid8 uuid, Long value) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<init>", new Object[] {uuid, value});
    this.uuid = uuid;
    this.value = value;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
  }

  /**
   *  Constructor for a new JsMessageHandleImpl
   *  This constructor should only be called by the JsMessageHandleFactoryImpl
   *  create methods and JsHdrsImpl.getMessageHandle().
   *  A JsMessageHandleImpl should never be instantiated directly.
   *  Thus constructor sets the uuid to be null.
   *
   *  @param value       The long SystemMessageValue of the same message.
   *
   */
  JsMessageHandleImpl(long value) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<init>", new Object[] {Long.valueOf(value)});
    this.uuid = null;
    this.value = Long.valueOf(value);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
  }


  /* **************************************************************************/
  /* Get methods                                                              */
  /* **************************************************************************/

  /*
   *  Get the SystemMessageSourceUuid from the JsMessageHandle.
   *
   *  Javadoc description supplied by SIMessageHandle interface.
   */
  public final SIBUuid8 getSystemMessageSourceUuid() {
    return uuid;
  }


  /*
   *  Get the name of the SystemMessageValue from the JsMessageHandle.
   *
   *  Javadoc description supplied by JsMessageHandle interface.
   */
  public final long getSystemMessageValue() {
    return value.longValue();
  }


  /* **************************************************************************/
  /* Equals & hashCode methods                                                */
  /* **************************************************************************/

  /**
   *  Override of java.lang.Object.equals(Object)
   *  For description, see java.lang.Object.equals(Object)
   *
   *  @return boolean true if the Object given is an instance of JsMessageHandleImpl
   *                  with attributes equivalent to this instance.
   */
  public final boolean equals(Object obj) {

    /* Do the easy checks first */
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (obj.getClass() != this.getClass()) {
      return false;
    }

    if (obj.hashCode() != this.hashCode()) {
      return false;
    }

    /* Now start on the content */
    final JsMessageHandleImpl other = (JsMessageHandleImpl)obj;

    /* Check the value - easy as it can never be null                   */
    if (!value.equals(other.value)) return false;

    /* Finally the dodgiest one.... which could be null                 */
    /* SIBUuid8.equals(x) copes OK if x is null - but do we trust it?   */
    if (uuid == other.uuid) {
      return true;
    }
    if (  (uuid != null)
       && (uuid.equals(other.uuid))
       ) {
      return true;
    }
    else {
      return false;
    }

  }


  /**
   *  Override of java.lang.Object.hashCode()
   *  For description, see java.lang.Object.hashCode()
   *
   *  @return int The hashcode for the JsMessageHandleImpl
   */
  public final int hashCode() {

    /* If the hashcode happens to be 0 we will have to calculate it every     */
    /* time, but it's not likely to happen often.                             */
    if (hashcode == 0) {

      long hashval = 0;

      if (uuid != null) {
        hashval = uuid.hashCode();
      }
      hashval = hashval + value.longValue();
      hashval = hashval % (long)Integer.MAX_VALUE;

      hashcode = (int)hashval;
    }

    return hashcode;

  }


  /* **************************************************************************/
  /* toString                                                                 */
  /* **************************************************************************/

  public String toString() {
    StringBuilder buff = new StringBuilder( (uuid == null) ? "null" : uuid.toString());
    buff.append(MfpConstants.MESSAGE_HANDLE_SEPARATOR);
    buff.append(value.toString());
    return new String(buff);
  }

  /* **************************************************************************/
  /* Serialization and de-serialization methods                               */
  /* **************************************************************************/

  /**
   * The writeObject method for serialization.
   * <p>
   * writeObject calls the defaultWriteObject to serialize the existing class,
   * however the meId variable is not serialized as it is transient (only
   * because SIBUuid8 is not Serializable) so must be written explicitly.
   *
   * @param out The ObjectOutputStream used for serialization.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "writeObject");

    /* Call the default writeObject */
    out.defaultWriteObject();

    /* Get the flattened version of the ME Id field */
    byte[] id = null;
    if (uuid != null) {
      id = uuid.toByteArray();
    }

    /* Write it out to the ObjectOutputStream */
    out.writeObject(id);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "writeObject");
  }

  /**
   * The readObject method for serialization.
   * <p>
   * readObject calls the defaultReadObject to de-serialize the existing class,
   * however the meId variable is not recovered automatically as it is transient.
   * The meId is read explicitly from the inputStream.
   *
   * @param in The ObjectInputStream used for serialization.
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "readObject");

    /* Call the default readObject */
    in.defaultReadObject();

    /* Recover the flattened version of the message */
    byte[] id = (byte [])in.readObject();
    if (id != null) {
      uuid = new SIBUuid8(id);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "readObject");
  }


  /**
   *  Returns the SIMessageHandles System Message ID.
   *
   *  @return String The SystemMessageID of the SIMessageHandle.
   */
  public String getSystemMessageId() {
    //based on com.ibm.ws.sib.mfp.impl.JsHdrsImpl
    if (uuid != null) {
      StringBuilder buff = new StringBuilder(uuid.toString());
      buff.append(MfpConstants.MESSAGE_HANDLE_SEPARATOR);
      buff.append(value);
      return new String(buff);
    }
    else {
      return null;
    }
  }

  /**
   *  Flattens the SIMessageHandle to a byte array.
   *  This flattened format can be restored into a SIMessageHandle by using
   *  com.ibm.wsspi.sib.core.SIMessageHandleRestorer.restoreFromBytes(byte [] data)
   *
   *  Format of flattened bytes is:
   *
   *    (short) versionNumber
   *    (int) length of uuid byteArray (0 if uuid is null)
   *    uuid byteArray (if not null)
   *    (long) value
   *
   *  @see com.ibm.wsspi.sib.core.SIMessageHandleRestorer#restoreFromBytes(byte[])
   *  @return byte[] The flattened SIMessageHandle.
   */

  public byte[] flattenToBytes() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "flattenToBytes");

    int uuidBytesLength = 0;
    byte [] uuidBytes = null;

    // check if uuid is null
    if (uuid != null)
    {
      uuidBytes = uuid.toByteArray();
      uuidBytesLength = uuidBytes.length;
    }

    int totalArraySize = ArrayUtil.SHORT_SIZE + ArrayUtil.INT_SIZE + uuidBytesLength + ArrayUtil.LONG_SIZE;
    byte [] flattenedBytes = new byte [totalArraySize];
    int offset = 0;

    // write the version number
    ArrayUtil.writeShort(flattenedBytes, offset ,versionNumber);
    offset += ArrayUtil.SHORT_SIZE;

    // write the length of the uuid byteArray (which will be 0 if the uuid is null)
    ArrayUtil.writeInt(flattenedBytes, offset, uuidBytesLength);
    offset += ArrayUtil.INT_SIZE;

    if (uuid != null)
    {
      // if the uuid is not null, write the uuid byteArray
      ArrayUtil.writeBytes(flattenedBytes, offset, uuidBytes);
      offset += uuidBytesLength;
    }

    // write the value. Comment on value declaration above states that this will never be null.
    ArrayUtil.writeLong(flattenedBytes, offset, value.longValue());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "flattenToBytes");
    return flattenedBytes;
  }

  /**
   *  Flattens the SIMessageHandle to a String.
   *  This flattened format can be restored into a SIMessageHandle by using
   *  com.ibm.wsspi.sib.core.SIMessageHandleRestorer.restoreFromString(String data)
   *
   *  @see com.ibm.wsspi.sib.core.SIMessageHandleRestorer#restoreFromString(String)
   *  @return String The flattened SIMessageHandle.
   */
  public String flattenToString() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "flattenToString");

    StringBuffer buffer = new StringBuffer();
    byte [] bytes = this.flattenToBytes();
    HexString.binToHex(bytes, 0, bytes.length, buffer);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "flattenToString");
    return new String(buffer);
  }


}
