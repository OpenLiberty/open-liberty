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

package com.ibm.ws.sib.processor.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.admin.JsAdminUtils;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsDestinationAddressFactory;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIMessageHandle;

public class SIMPUtils
{

  public static final int UUID_LENGTH_8 = 8;
  public static final int UUID_LENGTH_12 = 12;

  /**
   * Initialise trace for the component.
   */
  private static final TraceComponent tc =
    SibTr.register(
      SIMPUtils.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /**
   * Return a string padded (or truncated) to the specified length
   * @param s
   * @param length
   * @return A padded String
   */
  public static String pad(String s, int length)
  {
    if (s.length() < length)
    {
      StringBuffer a = new StringBuffer(length);
      a.append(s);
      for (int i = s.length(); i < length; i++)
      {
        a = a.append(" ");
      }
      return a.toString();
    }
    else if (s.length() > length)
    {
      return s.substring(0, length);
    }
    else
    {
      return s;
    }
  }

  public static SIBUuid12 createSIBUuid12(String s)
  {
    return new SIBUuid12(pad(s, UUID_LENGTH_12).getBytes());
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.MPCoreConnection#createJsSystemDestinationAddress(java.lang.String, com.ibm.ws.sib.utils.SIBUuid8)
   */
  public static JsDestinationAddress createJsSystemDestinationAddress(
    String prefix,
    SIBUuid8 meUuid)
  {
    JsDestinationAddressFactory addressFactory =
      (JsDestinationAddressFactory) MessageProcessor.getSingletonInstance(
        SIMPConstants.JS_DESTINATION_ADDRESS_FACTORY);
    return addressFactory.createJsSystemDestinationAddress(prefix, meUuid);
  }

  public static JsDestinationAddress createJsSystemDestinationAddress(
    String prefix,
    SIBUuid8 meUuid,
    String busName)
  {
    JsDestinationAddressFactory addressFactory =
      (JsDestinationAddressFactory) MessageProcessor.getSingletonInstance(
        SIMPConstants.JS_DESTINATION_ADDRESS_FACTORY);
    return addressFactory.createJsSystemDestinationAddress(
      prefix,
      meUuid,
      busName);
  }

  public static JsDestinationAddress createJsDestinationAddress(
    String name,
    SIBUuid8 meUuid)
  {
    JsDestinationAddressFactory addressFactory =
      (JsDestinationAddressFactory) MessageProcessor.getSingletonInstance(
        SIMPConstants.JS_DESTINATION_ADDRESS_FACTORY);
    return addressFactory.createJsDestinationAddress(name, meUuid != null, meUuid);
  }

  public static JsDestinationAddress createJsDestinationAddress(
    String name,
    SIBUuid8 meUuid,
    String busName)
  {
    JsDestinationAddressFactory addressFactory =
      (JsDestinationAddressFactory) MessageProcessor.getSingletonInstance(
        SIMPConstants.JS_DESTINATION_ADDRESS_FACTORY);
    return addressFactory.createJsDestinationAddress(
      name,
      meUuid != null,
      meUuid,
      busName);
  }
  
  public static JsDestinationAddress createJsDestinationAddress(
    String name,
    String busName,
    boolean localOnly)
  {
    JsDestinationAddressFactory addressFactory =
      (JsDestinationAddressFactory) MessageProcessor.getSingletonInstance(
        SIMPConstants.JS_DESTINATION_ADDRESS_FACTORY);
    return addressFactory.createJsDestinationAddress(
      name,
      localOnly,
      null,
      busName);
  }
		  

  /**
   * @param destinationName
   * @return a new SIBUuid based on the destination name
   */
  public static SIBUuid8 parseME(String destinationName)
  {
    //System dests are of the form _S<Prefix>_<MEId>
    //Temporary dests are of the form _Q/_T<Prefix>_<MEId><TempdestId>
    SIBUuid8 meUuid = null;
    if (destinationName != null
      && (destinationName.startsWith(SIMPConstants.SYSTEM_DESTINATION_PREFIX)
        || (destinationName
          .startsWith(SIMPConstants.TEMPORARY_PUBSUB_DESTINATION_PREFIX))
        || destinationName.startsWith(
          SIMPConstants.TEMPORARY_QUEUE_DESTINATION_PREFIX)))
    {
      int index =
        destinationName.indexOf(SIMPConstants.SYSTEM_DESTINATION_SEPARATOR, 2);

      if (index > 1)
      {
        String meSubstring;
        if (destinationName.startsWith(SIMPConstants.SYSTEM_DESTINATION_PREFIX))
        {
          meSubstring = destinationName.substring(index + 1);
        }
        else //Must be a temp destination
        {
          int startPoint = index + 1;
          int finishPoint = startPoint + 16;
          meSubstring = destinationName.substring(startPoint, finishPoint);
        }
        meUuid = new SIBUuid8(meSubstring);
      }
    }
    return meUuid;
  }

  /**
   * Used to extract the destination prefix component of a full temporary
   * destination name.
   * @param destinationName
   * @return temporary destination prefix
   */
  public static String parseTempPrefix(String destinationName)
  {
    //Temporary dests are of the form _Q/_T<Prefix>_<MEId><TempdestId>
    String prefix = null;
    if (destinationName != null
      && (destinationName
        .startsWith(SIMPConstants.TEMPORARY_PUBSUB_DESTINATION_PREFIX))
      || destinationName.startsWith(
        SIMPConstants.TEMPORARY_QUEUE_DESTINATION_PREFIX))
    {
      int index =
        destinationName.indexOf(SIMPConstants.SYSTEM_DESTINATION_SEPARATOR, 2);

      if (index > 1)
      {
        prefix = destinationName.substring(2, index);
      }
    }
    return prefix;
  }

  public static String getStackTrace(Throwable exc)
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter pw = new PrintWriter(baos);
    exc.printStackTrace(pw);
    pw.flush();
    String stackTrace = baos.toString();
    // Having got the stack trace string, process it to remove any whitespace other
    // than single space characters.
    char[] traceChars = new char[stackTrace.length()];
    stackTrace.getChars(0, stackTrace.length(), traceChars, 0);

    boolean notSpace = true;
    int pos = 0;
    for (int i = 0; i < stackTrace.length(); i++)

      // Convert more than one space in a row into a single space.
      if (Character.isSpaceChar(traceChars[i]))
      {
        if (notSpace)
          traceChars[pos++] = traceChars[i];
        notSpace = false;
      }

    // Otherwise keep non-space characters; treat the first whitespace character
    // as a space (so that we dont end up having text1\ntext2 -> text1text2
    else
    {
      if (!Character.isWhitespace(traceChars[i]))
      {
        notSpace = true;
        traceChars[pos++] = traceChars[i];
      }
      else if (notSpace)
      {
        traceChars[pos++] = ' ';
        notSpace = false;
      }
    }

    // Return the processed string.
    return new String(traceChars, 0, pos);
  }
  
  public static String longArrayToString(long array[])
  {
    String arrayStr = "[";
    
    if (array != null)
      for (int i = 0; i < array.length; i++)
      {
        if (i == 0)
          arrayStr = arrayStr + array[i];
        else
          arrayStr = arrayStr + "," + array[i]; 
      }
    
    arrayStr = arrayStr + "]";
    
    return arrayStr;

  }

  public static String messageHandleArrayToString(SIMessageHandle array[])
  {
    String arrayStr = "[";
    
    if (array != null)
      for (int i = 0; i < array.length; i++)
      {
        if (i == 0)
          arrayStr = arrayStr + array[i].toString();
        else
          arrayStr = arrayStr + "," + array[i].toString(); 
      }
    
    arrayStr = arrayStr + "]";
    
    return arrayStr;

  }
  
  /** Utility method to take the meUUID and get a name from admin for it
   *  ONLY USE THIS WHEN THE ME NAME IS FOR INFORMATION ONLY
   *  e.g. where it's for an exception insert, or a user message.
   *
   * @param meUUID the uuid of the messaging engine
   * @return the name of the messaging engine
   */
  public static String getMENameFromUuid(String meUUID)
  {
    String meName = JsAdminUtils.getMENameByUuidForMessage(meUUID);
    
    if (meName == null)
      meName = meUUID;
    
    return meName;
  }
  
  /**
   * The key we use to lookup/insert a streamInfo object is based off the remoteMEuuid +
   * the gatheringTargetDestUuid. This second value is null for standard consumer and set
   * to a destinationUuid (which could be an alias) for gathering consumers. In this way
   * we have seperate streams per consumer type.
   */
  public static String getRemoteGetKey(SIBUuid8 remoteUuid, SIBUuid12 gatheringTargetDestUuid)
  {
    String key = null;
    if (gatheringTargetDestUuid!=null)
      key = remoteUuid.toString()+gatheringTargetDestUuid.toString();
    else
      key = remoteUuid.toString()+SIMPConstants.DEFAULT_CONSUMER_SET;
    return key;
  }
  
  /**
   * Set up guaranteed delivery message properties. These are compulsory properties
   * on a control message and are therefore set throughout the code. The method
   * makes it easier to cope with new properties in the message.
   * 
   * @param msg  ControlMessage on which to set properties.
   */
  public static void setGuaranteedDeliveryProperties(ControlMessage msg,
                                                     SIBUuid8 sourceMEUuid,
                                                     SIBUuid8 targetMEUuid,
                                                     SIBUuid12 streamId,
                                                     SIBUuid12 gatheringTargetDestUuid,
                                                     SIBUuid12 targetDestUuid,
                                                     ProtocolType protocolType,
                                                     byte protocolVersion)
  { 
    // Remote to local message properties
    msg.setGuaranteedSourceMessagingEngineUUID(sourceMEUuid);
    msg.setGuaranteedTargetMessagingEngineUUID(targetMEUuid); 
    msg.setGuaranteedStreamUUID(streamId);
    msg.setGuaranteedGatheringTargetUUID(gatheringTargetDestUuid);
    msg.setGuaranteedTargetDestinationDefinitionUUID(targetDestUuid);
    if (protocolType != null)
      msg.setGuaranteedProtocolType(protocolType);
    msg.setGuaranteedProtocolVersion(protocolVersion);
  }
  
  /**
   * Set up guaranteed delivery message properties. These are optional properties
   * on a jsmessage. The method makes it easier to cope with new properties in the 
   * message.
   * 
   * @param msg  JsMessage on which to set properties.
   */
  public static void setGuaranteedDeliveryProperties(JsMessage msg,
                                                     SIBUuid8 sourceMEUuid,
                                                     SIBUuid8 targetMEUuid,
                                                     SIBUuid12 streamId,
                                                     SIBUuid12 gatheringTargetDestUuid,
                                                     SIBUuid12 targetDestUuid,
                                                     ProtocolType protocolType,
                                                     byte protocolVersion)
  { 
    // Remote to local message properties
    msg.setGuaranteedSourceMessagingEngineUUID(sourceMEUuid);
    msg.setGuaranteedTargetMessagingEngineUUID(targetMEUuid); 
    msg.setGuaranteedStreamUUID(streamId);
    msg.setGuaranteedGatheringTargetUUID(gatheringTargetDestUuid);
    msg.setGuaranteedTargetDestinationDefinitionUUID(targetDestUuid);
    if (protocolType != null)
      msg.setGuaranteedProtocolType(protocolType);
    msg.setGuaranteedProtocolVersion(protocolVersion);
  }
}
