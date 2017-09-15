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
package com.ibm.wsspi.sib.core;

/**
 * SIMessageHandle is an opaque handle which unique identifies an SIBusMessage.
 * An SIMessageHandle can not be manipulated in any way.
 *
 */
public interface SIMessageHandle {
  
  /**
   *  Returns the SIMessageHandles System Message ID.
   *  
   *  @return String The SystemMessageID of the SIMessageHandle.
   */
  
  public String getSystemMessageId();
  

  /**
   *  Flattens the SIMessageHandle to a byte array.
   *  This flattened format can be restored into a SIMessageHandle by using
   *  com.ibm.wsspi.sib.core.SIMessageHandleRestorer.restoreFromBytes(byte [] data)
   *
   *  This byte[] will not have a length greater than 64. This allows SPI users to 
   *  plan sufficient space for storage of the byte[] should they need it. 
   *
   *  @see com.ibm.wsspi.sib.core.SIMessageHandleRestorer#restoreFromBytes(byte[])
   *  @return byte[] The flattened SIMessageHandle.
   */
  
  public byte[] flattenToBytes(); 

  /**
   *  Flattens the SIMessageHandle to a String.
   *  This flattened format can be restored into a SIMessageHandle by using
   *  com.ibm.wsspi.sib.core.SIMessageHandleRestorer.restoreFromString(String data)
   *  
   *  This String will not have a length greater than 128.  This allows SPI users to 
   *  plan sufficient space for storage of the String should they need it. 
   *
   *  @see com.ibm.wsspi.sib.core.SIMessageHandleRestorer#restoreFromString(String) 
   *  @return String The flattened SIMessageHandle.
   */
  
  public String flattenToString(); 

}
