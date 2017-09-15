/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl;

import com.ibm.ws.sib.jfapchannel.JFapByteBuffer;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;

/**
 * This class is designed to be used by the JFap channel internally when it needs to send data.
 * 
 * @author Gareth Matthews
 */
public class InternalJFapByteBuffer extends JFapByteBuffer
{
   /**
    * Used for constructing a buffer that has no initial payload. This data can simply be sent as
    * it is, for added to using the putXXX() methods.
    */
   public InternalJFapByteBuffer()
   {
      super();
   }
   
   /**
    * Used for constructing ping responses from a ping request. The buffer is preloaded with the
    * data passed in. Note that the data is not copied from the buffer. Once the data is sent the
    * buffer will be automatically released. This buffer is read-only when using this constructor.
    * 
    * @param buffer
    */
   public InternalJFapByteBuffer(WsByteBuffer buffer)
   {
      reset();
      receivedBuffer = buffer;
   }
}
