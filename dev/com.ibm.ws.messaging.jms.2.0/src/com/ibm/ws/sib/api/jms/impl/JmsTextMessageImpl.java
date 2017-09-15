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

import java.io.UnsupportedEncodingException;

import javax.jms.TextMessage;
import javax.jms.JMSException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.mfp.JsJmsTextMessage;
import com.ibm.ws.sib.mfp.MessageCreateFailedException;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author matrober
 *
 * This is an implementation class for the JetStream Feb Prototype, which provides
 * TextMessage functionality.
 */
public class JmsTextMessageImpl extends JmsMessageImpl implements TextMessage
{

  /**
   * Assigned at version 1.19
   */
  private static final long serialVersionUID = -5270175349556840160L;

  // ******************* PRIVATE STATE VARIABLES *******************

  /**
   * MFP message object representing a JMS TextMessage
   * Note: Do not initialise this to null here, otherwise it will overwrite
   *       the setup done by instantiateMessage!
   */
  private JsJmsTextMessage txtMsg;

    // *************************** TRACE INITIALIZATION **************************
  private static TraceComponent tc = SibTr.register(JmsTextMessageImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

  // ************************ CONSTRUCTORS *************************

  public JmsTextMessageImpl() throws JMSException {
    // Calling the superclass no-args constructor in turn leads to the
    // instantiateMessage method being called, which we override to return
    // a text message.
    super();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsTextMessageImpl");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsTextMessageImpl");
  }


  /**
   * JmsTextMessageImpl
   * This constructor crates a TextMessage with the payload set to the given string
   *
   * @param txt       Text to use as the payload of the message.
   */
  public JmsTextMessageImpl(String txt) throws JMSException {
    this();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsTextMessageImpl", txt);

    txtMsg.setText(txt);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsTextMessageImpl");
  }


  /**
   * This constructor is used by the JmsMessage.inboundJmsInstance method (static)
   * in order to provide the inbound message path from MFP component to JMS component.
   */
  JmsTextMessageImpl(JsJmsTextMessage newMsg, JmsSessionImpl newSess) {
    // Pass this object to the parent class so that it can keep a reference.
    super(newMsg, newSess);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsTextMessageImpl", new Object[]{newMsg, newSess});

    // Store the reference we are given, and inform the parent class.
    txtMsg = newMsg;
    messageClass = CLASS_TEXT;

    // Note that we do NOT initialize the defaults for inbound messages.
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsTextMessageImpl");
  }

  /**
   * Construct a jetstream jms message from a (possibly non-jetstream) vanilla jms message.
   */
  JmsTextMessageImpl(TextMessage textMessage) throws JMSException {
    // copy message headers and properties.
    super(textMessage);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsTextMessageImpl", textMessage);

    // set-up this class's state (i.e. do what this() does).
    // nothing to do.

    // copy text.
    setText(textMessage.getText());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsTextMessageImpl");
  }

    // ********************* INTERFACE METHODS ***********************

  /**
   * @see javax.jms.TextMessage#setText(String)
   */
  public void setText(String txt) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
      if ((txt == null) || txt.length() < 257) {
        SibTr.entry(this, tc, "setText", txt);
      }
      else {
        SibTr.entry(this, tc, "setText", new Object[]{txt.length(), txt.substring(0,200)+"..."});
      }
    }

    checkBodyWriteable("setText");
    txtMsg.setText(txt);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setText");
  }

  /**
   * @see javax.jms.TextMessage#getText()
   */
  public String getText() throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getText");

    String txt;
    try {
      txt = txtMsg.getText();
    }
    catch (UnsupportedEncodingException e) {
      // No FFDC Code Needed
      throw (JMSException) JmsErrorUtils.newThrowable(
              JMSException.class,
              "EXCEPTION_RECEIVED_CWSIA0022",
              new Object[] {e, "JmsTextMessageImpl.getText"},
              e, "JmsTextMessageImpl#3", this, tc);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
      if ((txt == null) || txt.length() < 257) {
        SibTr.exit(this, tc, "getText", txt);
      }
      else {
        SibTr.exit(this, tc, "getText", new Object[]{txt.length(), txt.substring(0,200)+"..."});
      }
    }
    return txt;
  }

  /**
   * Adds the message body content to the toString of the parent class.
   * Note that this method does not need to keep a cache of the body
   * data, because it is already a string!
   */
  public String toString() {
    String val = super.toString();
    try {
      String thisText = getText();
      if (thisText == null) {
        thisText = "<null>";
      }
      else {
        if (thisText.length() > 100) {
          thisText = thisText.substring(0, 100)+" ...";
        }
      }
      val += "\n"+thisText;
    }
    catch (JMSException e) {
      // No FFDC code needed
      // Ignore any exception here.
    }
    return val;
  }

  /**
   * hashCode
   * Use the hashcode of the underlying MFP message, which means that equals
   * could return true for 2 TextMessages wrapping the same underlying message.
   * This has been the case for the WAS6 releases, so presumably that is what is wanted.
   *
   * @return int The hashcode for this message.
   */
  public int hashCode() {
    // Defend against possible errors...
    if (txtMsg == null) return 0;
    return txtMsg.hashCode();
  }

  //******************** IMPLEMENTATION METHODS **********************

  /**
   * @see com.ibm.ws.sib.api.jms.impl.JmsMessageImpl#instantiateMessage()
   */
  protected JsJmsMessage instantiateMessage() throws Exception {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "instantiateMessage");

    // Create a new message object.
    JsJmsTextMessage newMsg = null;
    try {
      newMsg = jmfact.createJmsTextMessage();
      messageClass = CLASS_TEXT;
    }
    catch(MessageCreateFailedException e) {
      // No FFDC code needed
      // d238447 FFDC review. Don't call process throwable in this case
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "exception caught: ",e);
      throw e;
    }

    // Do any other reference storing here (for subclasses)
    txtMsg = newMsg;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "instantiateMessage",  newMsg);
    return newMsg;
  }

}
