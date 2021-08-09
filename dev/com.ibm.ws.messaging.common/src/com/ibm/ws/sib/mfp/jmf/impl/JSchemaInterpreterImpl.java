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

package com.ibm.ws.sib.mfp.jmf.impl;

import java.math.BigInteger;

import com.ibm.ws.sib.mfp.jmf.JMFRegistry;
import com.ibm.ws.sib.mfp.jmf.JMFMessage;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.util.ArrayUtil;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * I mplementation of the JSchemaInterpreter interface for version 1 of JMF
 */

public final class JSchemaInterpreterImpl implements JSchemaInterpreter {
  private static TraceComponent tc = JmfTr.register(JSchemaInterpreterImpl.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  /**
   * Implementation of decode
   */
  public JMFMessage decode(JSchema schema, byte[] contents, int offset, int length)
      throws JMFMessageCorruptionException {
    return new JSMessageImpl(schema, contents, offset, length, true);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.mfp.jmf.impl.JSchemaInterpreter#checkSchemata(byte[], int)
   */
  public long[] checkSchemata(byte[] contents, int offset)
      throws JMFMessageCorruptionException {
    int numUnknown = 0;
    long[] unknownSchemata = null;
    try {
      //find out how many schemata there are
      int nSchemata = ArrayUtil.readShort(contents, offset);
      // Make sure the size looks valid before we allocate space and start reading
      if (nSchemata < 0 || offset+2 + nSchemata*8 > contents.length) {
        JMFMessageCorruptionException jmce =  new JMFMessageCorruptionException(
            "Bad schemata length: " + nSchemata + " at offset " + offset);
        FFDCFilter.processException(jmce, "com.ibm.ws.sib.mfp.jmf.impl.JSchemaInterpreterImpl.checkSchemata", "98", this,
            new Object[] { MfpConstants.DM_BUFFER, contents, Integer.valueOf(0), Integer.valueOf(contents.length) });
        throw jmce;
      }
      offset += 2;
      unknownSchemata = new long[nSchemata];
      for (int i = 0; i < nSchemata; i++) {
        //read the schema id
        long schemaId = ArrayUtil.readLong(contents, offset);
        offset += 8;
        //try to look up the schema
        JMFSchema schema = JMFRegistry.instance.retrieve(schemaId);
        //if we don't find the schema, record the id in the array to be
        //returned
        if(schema == null){
          unknownSchemata[numUnknown] = schemaId;
          numUnknown++;
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.jmf.impl.JSchemaInterpreterImpl.checkSchemata", "101", this,
          new Object[] { MfpConstants.DM_BUFFER, contents, Integer.valueOf(0), Integer.valueOf(contents.length) });
      // If we get an ArrayIndexOutOfBounds error we have tried to read beyond the end
      // of the supplied contents buffer.  This means the data provided must be invalid.
      throw new JMFMessageCorruptionException("Attempt to read beyond end of data buffer", e);
    }
    //copy the unknown ids in to an array of the right size
    if(numUnknown > 0){
      long[] result = new long[numUnknown];
      System.arraycopy(unknownSchemata, 0, result, 0, numUnknown);
      return result;
    }
    else
    {
      return new long[0];
    }
  }

  /**
   * Implementation of newMessage
   */
  public JMFMessage newMessage(JSchema schema) {
    return new JSMessageImpl(schema);
  }

  /**
   * Implementation of reEncode
   */
  public JMFMessage reEncode(JMFMessage currMessage) {
    return currMessage; // ok for now
  }

  /**
   * Method to retrieve (and possibly construct) the MessageMap for a particular
   * multiChoice code
   */
  static public synchronized MessageMap getMessageMap(JSchema sfs, BigInteger code) {
    // This method must be synchronized since some of the work done when constructing
    // a new MessageMap is not thread safe.
    MessageMapTable maps = (MessageMapTable)sfs.getInterpreterCache(JMFRegistry.JMF_ENCODING_VERSION);
    if (maps == null) {
      BigInteger count = ((JSType)sfs.getJMFType()).getMultiChoiceCount();
      maps = new MessageMapTable(count);
      sfs.setInterpreterCache(JMFRegistry.JMF_ENCODING_VERSION, maps);
    }
    MessageMap ans = maps.getMap(code);
    if (ans == null)
      maps.set(ans = new MessageMap(sfs, code));
    return ans;
  }

  /**
   * Method to retrieve (and possibly construct) the MessageMap for a particular
   * combination of choices.
   */
  static public MessageMap getMessageMap(JSchema sfs, int[] choices) throws JMFUninitializedAccessException {
    BigInteger multiChoice = MessageMap.getMultiChoice(choices, sfs);
    return getMessageMap(sfs, multiChoice);
  }
}
