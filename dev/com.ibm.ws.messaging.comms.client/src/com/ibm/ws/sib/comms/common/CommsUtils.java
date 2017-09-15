/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.common;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.HandshakeProperties;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIBusMessage;

/**
 * This class contains many common utilities required by many comms components.
 * <p>
 * For this class to be used it must first be initialised. It needs this so that it can save away
 * a reference to the WsByteBuffer pool manager. As such, the initialisation methods pass in 
 * a reference to the connection manager. In the case where clients and servers run in the same JVM
 * the pool manager is retrieved from whoever called initialise first - probably the client.
 * <p>
 * There is great scope for commoning up other methods in this class aswell as adding the ability
 * to pool the comms strings and lists.
 * 
 * @author Gareth Matthews
 */
public class CommsUtils
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = CommsUtils.class.getName();
   
   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(CommsUtils.class, 
                                                           CommsConstants.MSG_GROUP, 
                                                           CommsConstants.MSG_BUNDLE);
   
   /** The NLS reference */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);
   
   /**
    * This method will get a runtime property from the sib.properties file.
    * 
    * @param property The property key used to look up in the file.
    * @param defaultValue The default value if the property is not in the file.
    * @return Returns the property value.
    */
   public static String getRuntimeProperty(String property, String defaultValue)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getRuntimeProperty", new Object[] {property, defaultValue});
                                           
      String runtimeProp = RuntimeInfo.getPropertyWithMsg(property, defaultValue);
                                           
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getRuntimeProperty", runtimeProp);
      
      return runtimeProp;
   }

   /**
    * This method will get a runtime property from the sib.properties file as a boolean.
    * 
    * @param property The property key used to look up in the file.
    * @param defaultValue The default value if the property is not in the file.
    * @return Returns the property value.
    */
   public static boolean getRuntimeBooleanProperty(String property, String defaultValue)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getRuntimeBooleanProperty", new Object[] {property, defaultValue});
                                           
      boolean runtimeProp = Boolean.valueOf(RuntimeInfo.getPropertyWithMsg(property, defaultValue)).booleanValue();
                                           
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getRuntimeBooleanProperty", ""+runtimeProp);
      
      return runtimeProp;
   }
   
   /**
    * This method will get a runtime property from the sib.properties file and will convert the 
    * value (if set) to an int. If the property in the file was set to something that was not
    * parseable as an integer, then the default value will be returned.
    * 
    * @param property The property key used to look up in the file.
    * @param defaultValue The default value if the property is not in the file.
    * @return Returns the property value.
    */
   public static int getRuntimeIntProperty(String property, String defaultValue)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getRuntimeIntProperty", new Object[] {property, defaultValue});
      
      // Note that we parse the default value outside of the try / catch so that if we muck
      // up then we blow up. Customer settable properties however, we do not want to blow if they
      // screw up. 
      int runtimeProp = Integer.parseInt(defaultValue);
      
      try
      {
         runtimeProp = Integer.parseInt(RuntimeInfo.getPropertyWithMsg(property, defaultValue));
      }
      catch (NumberFormatException e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".getRuntimeIntProperty" ,
                                     CommsConstants.COMMSUTILS_GETRUNTIMEINT_01);
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "NumberFormatException: ", e);
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getRuntimeIntProperty", ""+runtimeProp);
      return runtimeProp;
   }
   
   /**
    * This method will get a runtime property from the sib.properties file and will convert the 
    * value (if set) to an double. If the property in the file was set to something that was not
    * parseable as an double, then the default value will be returned.
    * 
    * @param property The property key used to look up in the file.
    * @param defaultValue The default value if the property is not in the file.
    * @return Returns the property value.
    */
   public static double getRuntimeDoubleProperty(String property, String defaultValue)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getRuntimeDoubleProperty", new Object[] {property, defaultValue});
      
      // Note that we parse the default value outside of the try / catch so that if we muck
      // up then we blow up. Customer settable properties however, we do not want to blow if they
      // screw up. 
      double runtimeProp = Double.parseDouble(defaultValue);
      
      try
      {
         runtimeProp = Double.parseDouble(RuntimeInfo.getPropertyWithMsg(property, defaultValue));
      }
      catch (NumberFormatException e)
      {
         FFDCFilter.processException(e, CLASS_NAME + ".getRuntimeDoubleProperty",
                                     CommsConstants.COMMSUTILS_GETRUNTIMEDOUBLE_01);
         
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "NumberFormatException: ", e);
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getRuntimeDoubleProperty", ""+runtimeProp);
      return runtimeProp;
   }

   /**
    * This method is used on API calls when checking to see if the call is supported for the current
    * FAP level. Before making a call in a method that is only supported in a particular FAP version
    * call this method passing in the lowest FAP version this method is supported in. If the current
    * negotiated FAP version is lower than this then an SIIncorrectCallException will be thrown.
    * 
    * @param handShakeProps The handshake properties where the FAP level lives
    * @param fapLevel The lowest fap level to accept on this call.
    * 
    * @throws SIIncorrectCallException if the current FAP level is lower than the allowed minimum
    *         for this call.
    */
   public static void checkFapLevel(HandshakeProperties handShakeProps, short fapLevel) 
      throws SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "checkFapLevel", ""+fapLevel);
      
      short actualFapVersion = handShakeProps.getFapLevel();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Actual FAP Level: ", ""+actualFapVersion);
      
      if (fapLevel > actualFapVersion)
      {
         throw new SIIncorrectCallException(
            nls.getFormattedMessage("CALL_NOT_SUPPORTED_AT_FAP_LEVEL_SICO0101", 
                                    new Object[] {"" + actualFapVersion}, 
                                    "CALL_NOT_SUPPORTED_AT_FAP_LEVEL_SICO0101")
         );
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "checkFapLevel");
   }
   
   /**
    * Determines whether a conversation requires the use of optimized transactions.
    * Optimized transactions are required if the following criteria are met:
    * <ul>
    * <li>The conversation is using FAP level 5 or better.</li>    
    * <li>The conversation has negotiated the requires optimized transactions
    *     capability.</li>
    * </ul>  
    * @param conversation The conversation to test.
    * @return true if the conversation requires the use of optimized transactions or
    * false otherwise.
    */
   public static boolean requiresOptimizedTransaction(Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "requiresOptimizedTransaction", conversation);      
      final HandshakeProperties handshakeProperties = conversation.getHandshakeProperties();
      final boolean result =
         (handshakeProperties.getFapLevel() >= JFapChannelConstants.FAP_VERSION_5) &&
         ((handshakeProperties.getCapabilites() & CommsConstants.CAPABILITIY_REQUIRES_OPTIMIZED_TX) != 0);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "requiresOptimizedTransaction", ""+result);
      return result;
   }
   
   /**
    * Determines whether a message is recoverable compared to the supplied maxUnrecoverableReliability. 
    *
    * @param mess the message to check.
    * 
    * @para maxUnrecoverableReliability the most reliable reliability that is considered unrecoverable in the context in which this method is executed. 
    *
    * @return true for any message which is more recoverable than maxUnrecoverableReliability, otherwise a false is returned.
    */
   public static boolean isRecoverable(final SIBusMessage mess, final Reliability maxUnrecoverableReliability)
   {
      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "isRecoverable", new Object[] {mess, maxUnrecoverableReliability});

      final Reliability messageReliability = mess.getReliability();

      if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Message Reliability: ", messageReliability);

      final boolean recoverable = messageReliability.compareTo(maxUnrecoverableReliability) > 0;

      if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "isRecoverable", recoverable);
      return recoverable;
   }
}