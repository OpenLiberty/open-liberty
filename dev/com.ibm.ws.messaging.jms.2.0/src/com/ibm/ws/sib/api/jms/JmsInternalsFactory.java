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
package com.ibm.ws.sib.api.jms;

import javax.jms.JMSException;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class provides an entry point for the objects that can be created for this
 * component.
 *
 * This class is specifically NOT tagged as ibm-spi because by definition it is not
 * intended for use by either customers or ISV's.
 */
public abstract class JmsInternalsFactory
{

  // *************************** TRACE INITIALIZATION **************************
  private static TraceComponent tcInt =
    Tr.register(
      JmsInternalsFactory.class,
      ApiJmsConstants.MSG_GROUP_INT,
      ApiJmsConstants.MSG_BUNDLE_INT);

  private static TraceNLS nls = TraceNLS.getTraceNLS(ApiJmsConstants.MSG_BUNDLE_INT);

  /**
   * The singleton instance of the shared utils object.
   */
  private static JmsSharedUtils jsuInstance = null;

  /**
   * Singleton instance of the dest encoding utils object.
   */
  private static MessageDestEncodingUtils mdeuInstance = null;

  /**
   * Singleton instance of the Report Message Converter object.
   */
  private static ReportMessageConverter rmcInstance = null;


  /**
   * Used to obtain the singleton instance of the JmsSharedUtils interface.
   *
   * @return JmsSharedUtils
   * @throws JMSException If the shared utils impl class can't be loaded - generates FFDC.
   */
  public static final JmsSharedUtils getSharedUtils() throws JMSException
  {

    if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled())
      SibTr.entry(tcInt, "getSharedUtils");

    if (jsuInstance == null)
    {

      try
      {
        Class cls =
          Class.forName("com.ibm.ws.sib.api.jms.impl.JmsSharedUtilsImpl");
        jsuInstance = (JmsSharedUtils) cls.newInstance();

      } catch (Exception e)
      {
        // No FFDC code needed

        // d238447 Add an FFDC for this problem
        FFDCFilter.processException(e, "JmsInternalsFactory.getSharedUtils", "getSharedUtils1");

        // Cannot use JmsErrorUtils because it's in the impl package.
        if (TraceComponent.isAnyTracingEnabled() && tcInt.isDebugEnabled())
          SibTr.debug(tcInt, "Unable to instantiate JmsSharedUtilsImpl", e);
        if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled())
          SibTr.exit(tcInt, "getSharedUtils");

        jsuInstance = null;

        JMSException jmse = new JMSException(nls.getFormattedMessage("UNABLE_TO_CREATE_FACTORY_CWSIA0201",
                                              new Object[] {"JmsSharedUtilsImpl", "sib.api.jmsImpl.jar"},
                                              "!!!Unable to instantiate JmsSharedUtils"));
        jmse.initCause(e);
        jmse.setLinkedException(e);
        throw jmse;

      }//try

    }//if

    if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled())
      SibTr.exit(tcInt, "getSharedUtils");
    return jsuInstance;

  }

  /**
   * @return MessageDestEncodingUtils
   * @throws JMSException if the msg dest encoding utils impl class can't be loaded - throws FFDC.
   */
  public static MessageDestEncodingUtils getMessageDestEncodingUtils() throws JMSException
  {
    if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled()) SibTr.entry(tcInt, "getMessageDestEncodingUtils");

    if (mdeuInstance == null)
    {

      try
      {
        Class cls =
          Class.forName("com.ibm.ws.sib.api.jms.impl.MsgDestEncodingUtilsImpl");
        mdeuInstance = (MessageDestEncodingUtils) cls.newInstance();

      } catch (Exception e)
      {
        // No FFDC code needed

        // d238447 Add an FFDC for this problem
        FFDCFilter.processException(e, "JmsInternalsFactory.getMessageDestEncodingUtils",
                                    "getMessageDestEncodingUtils1");

        // Cannot use JmsErrorUtils because it's in the impl package.
        if (TraceComponent.isAnyTracingEnabled() && tcInt.isDebugEnabled())
          SibTr.debug(tcInt, "Unable to instantiate MsgDestEncodingUtilsImpl", e);
        if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled())
          SibTr.exit(tcInt, "getMessageDestEncodingUtils");

        jsuInstance = null;

        JMSException jmse = new JMSException(nls.getFormattedMessage("UNABLE_TO_CREATE_FACTORY_CWSIA0201",
                                              new Object[] {"MsgDestEncodingUtilsImpl", "sib.api.jmsImpl.jar"},
                                              "!!!Unable to instantiate DestEncodingUtils"));
        jmse.initCause(e);
        jmse.setLinkedException(e);
        throw jmse;

      }//try

    }//if

    if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled()) SibTr.exit(tcInt, "getMessageDestEncodingUtils");
    return mdeuInstance;

  }//getMessageDestEncodingUtils


  /**
   * Used to obtain the singleton instance of the ReportMessageConverter interface.
   *
   * @return ReportMessageConverter
   * @throws JMSException If the ReportMessageConverter impl class can't be loaded - generates FFDC.
   */
  public static final ReportMessageConverter getReportMessageConverter() throws JMSException
  {

    if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled())
      SibTr.entry(tcInt, "getReportMessageConverter");

    if (rmcInstance == null)
    {

      try
      {
        Class cls =
          Class.forName("com.ibm.ws.sib.api.jms.impl.ReportMessageConverterImpl");
        rmcInstance = (ReportMessageConverter) cls.newInstance();

      } catch (Exception e)
      {

        FFDCFilter.processException(e, "JmsInternalsFactory.getReportMessageConverter", "getReportMessageConverter1");

        // Cannot use JmsErrorUtils because it's in the impl package.
        if (TraceComponent.isAnyTracingEnabled() && tcInt.isDebugEnabled())
          SibTr.debug(tcInt, "Unable to instantiate ReportMessageConverterImpl", e);
        if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled())
          SibTr.exit(tcInt, "getReportMessageConverter");

        rmcInstance = null;

        JMSException jmse = new JMSException(nls.getFormattedMessage("UNABLE_TO_CREATE_FACTORY_CWSIA0201",
                                              new Object[] {"ReportMessageConverterImpl", "sib.api.jmsImpl.jar"},
                                              "!!!Unable to instantiate ReportMessageConverter"));
        jmse.initCause(e);
        jmse.setLinkedException(e);
        throw jmse;

      }//try

    }//if

    if (TraceComponent.isAnyTracingEnabled() && tcInt.isEntryEnabled())
      SibTr.exit(tcInt, "getReportMessageConverter");
    return rmcInstance;

  }

}
