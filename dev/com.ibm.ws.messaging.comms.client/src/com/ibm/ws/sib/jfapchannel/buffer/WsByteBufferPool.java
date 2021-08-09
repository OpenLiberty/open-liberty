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
package com.ibm.ws.sib.jfapchannel.buffer;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class is used by the JFap channel to get a reference to something that can allocate
 * WsByteBuffer's. The concrete implementation is different depending on whether we are running in 
 * the Portly client or not.
 * <p>
 * These concrete implementations will wrap any byte buffers in the local WsByteBuffer interface
 * which is used by the rest of the JFap channel. This allows the Portly client to implement its
 * own WsByteBuffer - which is nessecary as the CFW is not always present (for example when running
 * with a Sun JRE).
 * 
 * @author Gareth Matthews
 */
public abstract class WsByteBufferPool
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = WsByteBufferPool.class.getName();
   
   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(WsByteBufferPool.class, 
                                                           JFapChannelConstants.MSG_GROUP, 
                                                           JFapChannelConstants.MSG_BUNDLE);

   /** The singleton singleton instance of the right pool manager */
   private static WsByteBufferPool instance = null;
   
   /**
    * @return Returns the correct byte buffer pool manager.
    */
   public static WsByteBufferPool getInstance()
   {
      if (tc.isEntryEnabled()) SibTr.entry(tc, "getInstance");
      
      if (instance == null)
      {
         // Are we running in the Portly client?
         if (RuntimeInfo.isThinClient())
         {
            try
            {
               Class clazz = Class.forName(JFapChannelConstants.THIN_CLIENT_BUFFER_MANAGER_CLASS);
               instance = (WsByteBufferPool) clazz.newInstance();
            }
            catch (Exception e)
            {
               FFDCFilter.processException(e, CLASS_NAME + ".getInstance",
                                           JFapChannelConstants.WSBYTEBUFFERPOOL_GETINSTANCE_01, 
                                           JFapChannelConstants.THIN_CLIENT_BUFFER_MANAGER_CLASS);
               
               if (tc.isDebugEnabled()) SibTr.debug(tc, "Unable to instantiate thin client framework", e);
               
               // Nothin we can do throw this on...
               throw new SIErrorException(e);
            }
         }
         else
         {
            try
            {
               Class clazz = Class.forName(JFapChannelConstants.RICH_CLIENT_BUFFER_MANAGER_CLASS);
               instance = (WsByteBufferPool) clazz.newInstance();
            }
            catch (Exception e)
            {
               FFDCFilter.processException(e, CLASS_NAME + ".getInstance",
                                           JFapChannelConstants.WSBYTEBUFFERPOOL_GETINSTANCE_02, 
                                           JFapChannelConstants.RICH_CLIENT_BUFFER_MANAGER_CLASS);
               
               if (tc.isDebugEnabled()) SibTr.debug(tc, "Unable to instantiate rich client framework", e);
               
               // Nothin we can do throw this on...
               throw new SIErrorException(e);
            }
         }
      }
      
      if (tc.isEntryEnabled()) SibTr.exit(tc, "getInstance", instance);
      return instance;
   }
   
   /**
    * Allocates a buffer of the specified size from the underlying buffer pool manager.
    * 
    * @param size The size of the buffer.
    * @return Returns a reference to the newly allocated buffer.
    */
   public abstract WsByteBuffer allocate(int size);
   
   /**
    * Allocates a direct buffer of the specified size from the underlying buffer pool manager.
    * 
    * @param size The size of the buffer.
    * @return Returns a reference to the newly allocated buffer.
    */
   public abstract WsByteBuffer allocateDirect(int size);
   
   /**
    * Allocates a buffer that is backed by the specified byte[].
    * 
    * @param byteArray
    * @return Returns the newly allocated buffer.
    */
   public abstract WsByteBuffer wrap(byte[] byteArray);

   /**
    * Allocates a buffer that is backed by the specified byte[] starting at the specified offset in 
    * the byte[] for the specified number of bytes.
    * 
    * @param byteArray The byte[] to back the buffer with.
    * @param offset The offset in the byte[] for the buffer to start at.
    * @param length The number of bytes from the byte[] for the buffer to have access to.
    * 
    * @return Returns the newly allocated buffer.
    */
   public abstract WsByteBuffer wrap(byte[] byteArray, int offset, int length);
}
