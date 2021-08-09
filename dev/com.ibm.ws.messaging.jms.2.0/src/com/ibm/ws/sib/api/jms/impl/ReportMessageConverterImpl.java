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
package com.ibm.ws.sib.api.jms.impl;

import javax.jms.JMSException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIApiConstants;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.api.jms.JmsInternalsFactory;
import com.ibm.ws.sib.api.jms.JmsSharedUtils;
import com.ibm.ws.sib.api.jms.ReportMessageConverter;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIBusMessage;

public class ReportMessageConverterImpl implements ReportMessageConverter
{
  private static JmsSharedUtils utils = null;

  private static TraceComponent tc = SibTr.register(ReportMessageConverterImpl.class,ApiJmsConstants.MSG_GROUP_INT,ApiJmsConstants.MSG_BUNDLE_INT);

  //byte representations of the SIApiConstants Byte object constants
  public final static byte byte_REPORT_NO_DATA           = SIApiConstants.REPORT_NO_DATA.byteValue();
  public final static byte byte_REPORT_WITH_DATA         = SIApiConstants.REPORT_WITH_DATA.byteValue();
  public final static byte byte_REPORT_WITH_FULL_DATA    = SIApiConstants.REPORT_WITH_FULL_DATA.byteValue();
  public final static byte byte_REPORT_EXPIRY            = SIApiConstants.REPORT_EXPIRY.byteValue();
  public final static byte byte_REPORT_EXCEPTION         = SIApiConstants.REPORT_EXCEPTION.byteValue();
  public final static byte byte_REPORT_COA               = SIApiConstants.REPORT_COA.byteValue();
  public final static byte byte_REPORT_COD               = SIApiConstants.REPORT_COD.byteValue();
  public final static byte byte_REPORT_PAN               = SIApiConstants.REPORT_PAN.byteValue();
  public final static byte byte_REPORT_NAN               = SIApiConstants.REPORT_NAN.byteValue();

  /**
   * This method converts the Integer Report Message property values between the
   * byte set specfied for MQC and the int set given for the MFP constants.
   *
   * @param propName Name of the property to convert
   * @param propValue Value of property to convert to
   * @param coreMsg The message to perform the property conversion on
   */
  public void setIntegerReportOption(String propName, int propValue, SIBusMessage coreMsg)throws JMSException  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setIntegerReportOption", new Object[]{propName, propValue, coreMsg});

    if(propName.equals(ApiJmsConstants.REPORT_EXCEPTION_PROPERTY)) {
      Byte newVal = null;
      switch(propValue) {
        case ApiJmsConstants.MQRO_EXCEPTION:
          newVal = SIApiConstants.REPORT_NO_DATA;
          break;
        case ApiJmsConstants.MQRO_EXCEPTION_WITH_DATA:
          newVal = SIApiConstants.REPORT_WITH_DATA;
          break;
        case ApiJmsConstants.MQRO_EXCEPTION_WITH_FULL_DATA:
          newVal = SIApiConstants.REPORT_WITH_FULL_DATA;
          break;
        default:
          // d238447 FFDC review. No FFDC required.
          throw (JMSException) JmsErrorUtils.newThrowable(
              JMSException.class,
              "INVALID_VALUE_CWSIA0321",
              new Object[] { propName, "" + propValue },
              tc);
      }
      coreMsg.setReportException(newVal);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "REPORT_EXCEPTION set to : "+newVal);
    }

    else if(propName.equals(ApiJmsConstants.REPORT_EXPIRATION_PROPERTY)) {
      Byte newVal = null;
      switch(propValue) {
        case ApiJmsConstants.MQRO_EXPIRATION:
          newVal = SIApiConstants.REPORT_NO_DATA;
          break;
        case ApiJmsConstants.MQRO_EXPIRATION_WITH_DATA:
          newVal = SIApiConstants.REPORT_WITH_DATA;
          break;
        case ApiJmsConstants.MQRO_EXPIRATION_WITH_FULL_DATA:
          newVal = SIApiConstants.REPORT_WITH_FULL_DATA;
          break;
        default:
          // d238447 FFDC review, no FFDC required.
          throw (JMSException) JmsErrorUtils.newThrowable(
              JMSException.class,
              "INVALID_VALUE_CWSIA0321",
              new Object[] { propName, ""+propValue },
              tc
          );
      }
      coreMsg.setReportExpiry(newVal);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "REPORT_EXPIRATION set to : "+newVal);
    }

    else if(propName.equals(ApiJmsConstants.REPORT_COA_PROPERTY)) {
      Byte newVal = null;
      switch(propValue) {
        case ApiJmsConstants.MQRO_COA:
          newVal = SIApiConstants.REPORT_NO_DATA;
          break;
        case ApiJmsConstants.MQRO_COA_WITH_DATA:
          newVal = SIApiConstants.REPORT_WITH_DATA;
          break;
        case ApiJmsConstants.MQRO_COA_WITH_FULL_DATA:
          newVal = SIApiConstants.REPORT_WITH_FULL_DATA;
          break;
        default:
          // d238447 FFDC review, no FFDC required.
          throw (JMSException) JmsErrorUtils.newThrowable(
              JMSException.class,
              "INVALID_VALUE_CWSIA0321",
              new Object[] { propName, ""+propValue },
              tc);
      }
      coreMsg.setReportCOA(newVal);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "REPORT_COA set to : "+newVal);
    }

    else if(propName.equals(ApiJmsConstants.REPORT_COD_PROPERTY)) {
      Byte newVal = null;
      switch(propValue) {
        case ApiJmsConstants.MQRO_COD:
          newVal = SIApiConstants.REPORT_NO_DATA;
          break;
        case ApiJmsConstants.MQRO_COD_WITH_DATA:
          newVal = SIApiConstants.REPORT_WITH_DATA;
          break;
        case ApiJmsConstants.MQRO_COD_WITH_FULL_DATA:
          newVal = SIApiConstants.REPORT_WITH_FULL_DATA;
          break;
        default:
          // d238447 FFDC review, no FFDC required.
          throw (JMSException) JmsErrorUtils.newThrowable(
              JMSException.class,
              "INVALID_VALUE_CWSIA0321",
              new Object[] { propName, ""+propValue },
              tc);
      }
      coreMsg.setReportCOD(newVal);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "REPORT_COD set to : "+newVal);
    }

    else if(propName.equals(ApiJmsConstants.REPORT_PAN_PROPERTY)) {
      Boolean newVal = null;
      switch(propValue) {
        case ApiJmsConstants.MQRO_PAN:
          newVal = Boolean.valueOf(true);
          break;
        case ApiJmsConstants.MQRO_NONE:
          newVal = Boolean.valueOf(false);
          break;
        default:
          // d238447 FFDC review, no FFDC required.
          throw (JMSException) JmsErrorUtils.newThrowable(
              JMSException.class,
              "INVALID_VALUE_CWSIA0321",
              new Object[] { propName, ""+propValue },
              tc
          );
      }
      coreMsg.setReportPAN(newVal);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "REPORT_PAN set to : "+newVal);
    }

    else if(propName.equals(ApiJmsConstants.REPORT_NAN_PROPERTY)) {
      Boolean newVal = null;
      switch(propValue) {
        case ApiJmsConstants.MQRO_NAN:
          newVal = Boolean.valueOf(true);
          break;
        case ApiJmsConstants.MQRO_NONE:
          newVal = Boolean.valueOf(false);
          break;
        default:
          // d238447 FFDC review, no FFDC required.
          throw (JMSException) JmsErrorUtils.newThrowable(
              JMSException.class,
              "INVALID_VALUE_CWSIA0321",
              new Object[] { propName, ""+propValue },
              tc
          );
      }
      coreMsg.setReportNAN(newVal);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "REPORT_NAN set to : "+newVal);
    }

    else if(propName.equals(ApiJmsConstants.REPORT_MSGID_PROPERTY)) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.debug(this, tc, "propName equals REPORT_MSGID_PROPERTY");
      Boolean newVal = null;
      switch(propValue) {
        case ApiJmsConstants.MQRO_PASS_MSG_ID:
          newVal = Boolean.valueOf(true);
          break;
        case ApiJmsConstants.MQRO_NEW_MSG_ID:
          newVal = Boolean.valueOf(false);
          break;
        default:
          // d238447 FFDC review, no FFDC required.
          throw (JMSException) JmsErrorUtils.newThrowable(
              JMSException.class,
              "INVALID_VALUE_CWSIA0321",
              new Object[] { propName, ""+propValue },
              tc
          );
      }
      coreMsg.setReportPassMsgId(newVal);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "REPORT_MSGID set to : "+newVal);
    }

    else if(propName.equals(ApiJmsConstants.REPORT_CORRELID_PROPERTY)) {
      Boolean newVal = null;
      switch(propValue) {
        case ApiJmsConstants.MQRO_PASS_CORREL_ID:
          newVal = Boolean.valueOf(true);
          break;
        case ApiJmsConstants.MQRO_COPY_MSG_ID_TO_CORREL_ID:
          newVal = Boolean.valueOf(false);
          break;
        default:
          // d238447 FFDC review, no FFDC required.
          throw (JMSException) JmsErrorUtils.newThrowable(
              JMSException.class,
              "INVALID_VALUE_CWSIA0321",
              new Object[] { propName, ""+propValue },
              tc
          );
      }
      coreMsg.setReportPassCorrelId(newVal);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "REPORT_CORRELID set to : "+newVal);
    }

    else if(propName.equals(ApiJmsConstants.REPORT_DISCARD_PROPERTY)) {
      Boolean newVal = null;
      switch(propValue) {
        case ApiJmsConstants.MQRO_DISCARD_MSG:
          newVal = Boolean.valueOf(true);
          break;
        case ApiJmsConstants.MQRO_NONE:
          newVal = Boolean.valueOf(false);
          break;
        default:
          // d238447 FFDC review, no FFDC required.
          throw (JMSException) JmsErrorUtils.newThrowable(
              JMSException.class,
              "INVALID_VALUE_CWSIA0321",
              new Object[] { propName, ""+propValue },
              tc
          );
      }
      coreMsg.setReportDiscardMsg(newVal);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "REPORT_DISCARD set to : "+newVal);
    }

    else if(propName.equals(ApiJmsConstants.FEEDBACK_PROPERTY)) {
      // need to intialise the sharedUtils?
      if (utils == null) utils = JmsInternalsFactory.getSharedUtils();
      Integer newVal = utils.convertMQFeedbackToJS(propValue);
      coreMsg.setReportFeedback(newVal);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "FEEDBACK_PROPERTY set to : "+newVal);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setIntegerReportOption");
  }


  /**
   * Given a Report Message property, this method performs a conversion between
   * the MQC and MFP constants and then returns the object result.
   *
   * @param propName Name of property to convert
   * @param coreMsg The message on which to perform the conversion
   * @return Object
   */
  public Object getReportOption(String propName, SIBusMessage coreMsg) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getReportOption", new Object[]{propName, coreMsg});
    Object result = null;

    if(propName.equals(ApiJmsConstants.REPORT_EXCEPTION_PROPERTY)) {
      Byte value = coreMsg.getReportException();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Byte returned from core message getReportException():" + value);
      if(value != null) {
        byte propValue = value.byteValue();
        if (propValue == byte_REPORT_NO_DATA) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_EXCEPTION);
        }
        else if (propValue == byte_REPORT_WITH_DATA) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_EXCEPTION_WITH_DATA);
        }
        else if(propValue == byte_REPORT_WITH_FULL_DATA) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_EXCEPTION_WITH_FULL_DATA);
        }
        else {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.debug(this, tc, "Unexpected property value received: "+value);
          result = null;
        }
      }
    }

    else if(propName.equals(ApiJmsConstants.REPORT_EXPIRATION_PROPERTY)) {
      Byte value = coreMsg.getReportExpiry();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Byte returned from core message getReportExpiry():" + value);
      if(value != null) {
        byte propValue = value.byteValue();
        if (propValue == byte_REPORT_NO_DATA) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_EXPIRATION);
        }
        else if (propValue == byte_REPORT_WITH_DATA) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_EXPIRATION_WITH_DATA);
        }
        else if(propValue == byte_REPORT_WITH_FULL_DATA) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_EXPIRATION_WITH_FULL_DATA);
        }
        else {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Unexpected property value received: "+value);
          result = null;
        }
      }
    }

    else if(propName.equals(ApiJmsConstants.REPORT_COA_PROPERTY)) {
      Byte value = coreMsg.getReportCOA();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Byte returned from core message getReportCOA():" + value);
      if(value != null) {
        byte propValue = value.byteValue();
        if (propValue == byte_REPORT_NO_DATA) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_COA);
        }
        else if (propValue == byte_REPORT_WITH_DATA) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_COA_WITH_DATA);
        }
        else if(propValue == byte_REPORT_WITH_FULL_DATA) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_COA_WITH_FULL_DATA);
        }
        else {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Unexpected property value received: "+value);
          result = null;
        }
      }
    }

    else if(propName.equals(ApiJmsConstants.REPORT_COD_PROPERTY)) {
      Byte value = coreMsg.getReportCOD();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Byte returned from core message getReportCOD():" + value);
      if(value != null) {
        byte propValue = value.byteValue();
        if (propValue == byte_REPORT_NO_DATA) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_COD);
        }
        else if (propValue == byte_REPORT_WITH_DATA) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_COD_WITH_DATA);
        }
        else if(propValue == byte_REPORT_WITH_FULL_DATA) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_COD_WITH_FULL_DATA);
        }
        else {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Unexpected property value received: "+value);
          result = null;
        }
      }
    }

    else if(propName.equals(ApiJmsConstants.REPORT_PAN_PROPERTY)) {
      Boolean value = coreMsg.getReportPAN();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Boolean returned from core message getReportPAN():" + value);
      if(value != null) {
        boolean propValue = value.booleanValue();
        if (propValue) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_PAN);
        }
        else {
          result = Integer.valueOf(ApiJmsConstants.MQRO_NONE);
        }
      }
    }

    else if(propName.equals(ApiJmsConstants.REPORT_NAN_PROPERTY)) {
      Boolean value = coreMsg.getReportNAN();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Boolean returned from core message getReportNAN():" + value);
      if(value != null) {
        boolean propValue = value.booleanValue();
        if (propValue) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_NAN);
        }
        else {
          result = Integer.valueOf(ApiJmsConstants.MQRO_NONE);
        }
      }
    }

    else if(propName.equals(ApiJmsConstants.REPORT_MSGID_PROPERTY)) {
      Boolean value = coreMsg.getReportPassMsgId();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Boolean returned from core message getReportPassMsgId():" + value);
      if(value != null) {
        boolean propValue = value.booleanValue();
        if (propValue) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_PASS_MSG_ID);
        }
        else {
          result = Integer.valueOf(ApiJmsConstants.MQRO_NEW_MSG_ID);
        }
      }
    }

    else if(propName.equals(ApiJmsConstants.REPORT_CORRELID_PROPERTY)) {
      Boolean value = coreMsg.getReportPassCorrelId();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Boolean returned from core message getReportPassCorrelId():" + value);
      if(value != null) {
        boolean propValue = value.booleanValue();
        if (propValue) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_PASS_CORREL_ID);
        }
        else {
          result = Integer.valueOf(ApiJmsConstants.MQRO_COPY_MSG_ID_TO_CORREL_ID);
        }
      }
    }

    else if(propName.equals(ApiJmsConstants.REPORT_DISCARD_PROPERTY)) {
      Boolean value = coreMsg.getReportDiscardMsg();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Boolean returned from core message getReportDiscardMsg():" + value);
      if(value != null) {
        boolean propValue = value.booleanValue();
        if (propValue) {
          result = Integer.valueOf(ApiJmsConstants.MQRO_DISCARD_MSG);
        }
        else {
          result = Integer.valueOf(ApiJmsConstants.MQRO_NONE);
        }
      }
    }

    else if(propName.equals(ApiJmsConstants.FEEDBACK_PROPERTY)) {
      Integer value = coreMsg.getReportFeedback();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Byte returned from core message getReportFeedback():" + value);
      if(value != null) {
        // Need to initialise the sharedUtils?
        if (utils == null) {
          try {
            utils = JmsInternalsFactory.getSharedUtils();
          } catch (JMSException e) {
            // No FFDC code needed
            FFDCFilter.processException(e,"com.ibm.ws.sib.api.jms.impl.ReportMessageConverter","getReportOption#1"
                                       ,new Object[] {propName, coreMsg});
            return null; // Since we are unable to convert the value (consistent
                         // with original failure mode,
            //   but I think I would prefer to throw an exception).
          }
        }
        result = utils.convertJSFeedbackToMQ(value.intValue());
      }
    }

    else if(propName.equals(ApiJmsConstants.MSG_TYPE_PROPERTY)) {
//    TODO Support JMS_IBM_MsgType properly - d256740
      Integer value = coreMsg.getReportFeedback();
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Value returned from core message getReportFeedback():" + value);
      if(value != null) {
        int propValue = value.intValue();
        if(propValue != ApiJmsConstants.MQFB_NONE) {
          result = Integer.valueOf(ApiJmsConstants.MQMT_REPORT);
        }
        else {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.debug(this, tc, "byte returned from getReportFeedback() is not one of the expected values");
          result = null;
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getReportOption",  result);
    return result;
  }
}
