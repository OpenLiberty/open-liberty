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

package com.ibm.ws.sib.utils.ras;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.List;


import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ThreadLocalStack;


/**
 * This class provides an interface to Websphere trace logging functions
 * provided by com.ibm.websphere.ras.Tr. Trace logged using SibTr methods
 * print the associated Jetstream ME name for each trace message.
 */
public class SibTr {
  /* ************************************************************************** */
  /**
   * A Suppressor can be supplied on SibTr.info, SibTr.warning and SibTr.error
   * to determine if a message should be suppressed because it has already been
   * emitted.
   */
  /* ************************************************************************** */
  public static interface Suppressor
  {
    /* ************************************************************************** */
    /**
     * A Decision is the decision made by a suppressor. It includes:
     * <ul>
     * <li>The Type of the decision
     * <li>How long this decision lasts for (in minutes) (Note: This time
     *     period is intended for information only - the suppressor will be called
     *     again for every message - It is also only valid if the Type is
     *     <code>SUPPRESS_FOR_A_WHILE_AFTER_THIS</code> or <code>SUPPRESS_SOME_MORE_AFTER_THIS</code>.
     * <li>How many messages were suppressed (Note: This count is only valid
     *     if the Type is <code>SUPPRESS_SOME_MORE_AFTER_THIS</code>.
     * </ol>
     *
     */
    /* ************************************************************************** */
    public static class Decision
    {
      /** The message number of the message to which this decision applies */
      private final String _number;
      
      /** The variant to which this decision applies (null if it applies to all variants) */
      private final String _variant;

      /** Should we suppress this message */
      private boolean _suppressThisMessage;
      
      /** Should we report on earlier suppressed messages? */
      private boolean _reportEarlierSuppressedMessages;
      
      /** Will we be suppressing future messages? */
      private boolean _suppressFutureMessages;

      /** How many messages were suppressed */
      private int _suppressed;
      
      /** When was the message first suppressed */
      private long _firstSuppressed;
      
      /** When was the message last suppressed */
      private long _lastSuppressed;
      
      /** How long will messages be suppressed (in minutes) */
      private int _suppressionMinutes;
      
      /** How many milliseconds per minute */
      public static final int INFINITE = -1;
      
      /* -------------------------------------------------------------------------- */
      /* Decision constructor
      /* -------------------------------------------------------------------------- */
      /**
       * Construct a new default Decision.
       *
       * @param number  The message number to which this decision applies
       * @param variant The variant to which this decision applies (null if it applies to all variants)
       */
      private Decision(final String number, final String variant)
      {
        _number = number;
        _variant = variant;
        _suppressThisMessage = false;
        _reportEarlierSuppressedMessages = false;
        _suppressFutureMessages = false;
        _suppressed = 0;
        _firstSuppressed = 0;
        _lastSuppressed = 0;
        _suppressionMinutes = 0;
      }

      /* -------------------------------------------------------------------------- */
      /* makeDecision method
      /* -------------------------------------------------------------------------- */
      /**
       * Make a decision about the given the message number
       * 
       * @param messageNumber The message number
       * @return A decision (initially set to no suppression
       */
      public static Decision makeDecision(final String messageNumber)
      {
        return new Decision(messageNumber,null);
      }
      
      /* -------------------------------------------------------------------------- */
      /* makeDecision method
      /* -------------------------------------------------------------------------- */
      /**
       * Make a decision about the given the message number and variant
       * 
       * @param messageNumber The message number
       * @param variant       The variant of the message (the entire message text)
       * @return A decision (initially set to no suppression
       */
      public static Decision makeDecision(final String messageNumber, String variant)
      {
        return new Decision(messageNumber,variant);
      }
      
      /* -------------------------------------------------------------------------- */
      /* suppressThisMessage method
      /* -------------------------------------------------------------------------- */
      /**
       * Update the decision so that this instance of the message is suppressed
       */
      public void suppressThisMessage()
      {
        _suppressThisMessage = true;
      }
      
      /* -------------------------------------------------------------------------- */
      /* reportEarlierSuppressedMessages method
      /* -------------------------------------------------------------------------- */
      /**
       * Update the decision to record that earlier instances of the message were suppressed
       * (and that this should now be reported)
       * 
       * @param numberSuppressed The number of messages suppressed
       * @param first            The timestamp of the first message that was suppressed
       * @param last             The timestamp of the last message that was suppressed
       */
      public void reportEarlierSuppressedMessages(final int numberSuppressed, final long first, final long last)
      {
        _reportEarlierSuppressedMessages = true;
        _suppressed = numberSuppressed;
        _firstSuppressed = first;
        _lastSuppressed = last;
      }
      
      /* -------------------------------------------------------------------------- */
      /* suppressFutureMesages method
      /* -------------------------------------------------------------------------- */
      /**
       * Update the decision to record that future instances of the message will be
       * suppressed (and that this should now be reported)
       * 
       * @param minutes How long the messages will be suppressed. INFINITE if the messages will
       *                be suppressed indefinitely
       */
      public void suppressFutureMesages(final int minutes)
      {
        _suppressFutureMessages = true;
        _suppressionMinutes = minutes;
      }
            
      /* -------------------------------------------------------------------------- */
      /* emitSuppressedMessagesMessageIfNecessary method
      /* -------------------------------------------------------------------------- */
      /**
       * Emit a "Suppressed messages" message if the decision is to emit such a message
       * 
       * @param tc The trace component to be used when emitting the message
       */
      public void emitSuppressedMessagesMessageIfNecessary(final TraceComponent tc)
      {
        if (_reportEarlierSuppressedMessages)
        {
          DateFormat format = DateFormat.getDateTimeInstance();

          if (_variant == null)
          {
            if (_suppressed == 1)
            {
              Tr.info(tc
                     ,"A_MESSAGE_SUPPRESSED_EARLIER_CWSIU0101"
                     ,new Object[] {_number,format.format(_lastSuppressed)}
                     );
            }
            else
            {
              Tr.info(tc
                     ,"MESSAGES_SUPPRESSED_EARLIER_CWSIU0102"
                     ,new Object[] { _number, _suppressed, format.format(_firstSuppressed), format.format(_lastSuppressed)}
                     );
            }
          }
          else
          {
            if (_suppressed == 1)
            {
              Tr.info(tc
                      ,"SOME_A_MESSAGE_SUPPRESSED_EARLIER_CWSIU0103"
                      ,new Object[] { _variant, format.format(_lastSuppressed)}
                      );
            }
            else
            {
              Tr.info(tc
                     ,"SOME_MESSAGES_SUPPRESSED_EARLIER_CWSIU0104"
                     ,new Object[] { _variant, _suppressed, format.format(_firstSuppressed), format.format(_lastSuppressed)}
                     );
            }
          }
        }
      }
      
      /* -------------------------------------------------------------------------- */
      /* isSuppressThisMessage method
      /* -------------------------------------------------------------------------- */
      /**
       * @return true if the message display should be suppressed
       */
      public boolean isSuppressThisMessage()
      {
        return _suppressThisMessage;
      }

      /* -------------------------------------------------------------------------- */
      /* emitSuppressingFollowingMessagesMessageIfNecessary method
      /* -------------------------------------------------------------------------- */
      /**
       * Emit a "Future messages will be suppressed" message if the decision is to emit such a message
       * 
       * @param tc The trace component to be used when emitting the message
       */
      public void emitSuppressingFollowingMessagesMessageIfNecessary(final TraceComponent tc)
      {
        if (_suppressFutureMessages)
        {
          if (_variant == null)
          {
            if (_suppressionMinutes != INFINITE)
            {
              Tr.info(tc
                     ,"FUTURE_MESSAGES_SUPPRESSED_CWSIU0002"
                     ,new Object[] { _number, Integer.valueOf(_suppressionMinutes) }
                     );
            }
            else
            {
              Tr.info(tc
                      ,"ALL_MESSAGES_SUPPRESSED_CWSIU0003"
                      ,new Object[] { _number }
                      );
            }
          }
          else
          {
            if (_suppressionMinutes != INFINITE)
            {
              Tr.info(tc
                     ,"SOME_FUTURE_MESSAGES_SUPPRESSED_CWSIU0005"
                     ,new Object[] { _variant, Integer.valueOf(_suppressionMinutes) }
                     );
            }
            else
            {
              Tr.info(tc
                     ,"SOME_ALL_MESSAGES_SUPPRESSED_CWSIU0006"
                     ,new Object[] { _variant }
                     );
            }
          }
        }
      }
    }

    /* -------------------------------------------------------------------------- */
    /* suppress method
    /* -------------------------------------------------------------------------- */
    /**
     * Determine if a message should be suppressed
     *
     * @param msgkey The message key of the message that might need to be suppressed
     * @param formattedMessage The actual message text resolved for NLS, inserts etc.
     * @return true if the message should be suppressed, false if it should be emitted
     */
    public Decision suppress(String msgkey, String formattedMessage);

    /** All messages with the same message key will be suppressed until the end of the JVM */
    public static final Suppressor ALL_AFTER_FIRST = new AllAfterFirstSuppressor();
    /** All messages with the same message key AND identical inserts will be suppressed until the end of the JVM */
    public static final Suppressor ALL_AFTER_FIRST_SIMILAR_INSERTS = new AllAfterFirstSimilarInsertsSuppressor();
    /** All messages with the same message key will be suppressed for a while (default 30 minutes) */
    public static final Suppressor ALL_FOR_A_WHILE = new AllForAWhileSuppressor();
    /** All messages with the same message key and inserts will for suppressed for a while (default 30 minutes) */
    public static final Suppressor ALL_FOR_A_WHILE_SIMILAR_INSERTS = new AllForAWhileSimilarInsertsSuppressor();
  }

  private static ThreadLocalStack<String> threadLocalStack = new ThreadLocalStack<String>();
  private static final String DEFAULT_ME_NAME = ":";
  public static final int MAX_TO_FORMAT = 0x100000; // Never format more than 1Mb of bytes for an array/slice

  /**
   * Get Jetstream ME name to be printed in trace message
   *
   * @param o The object whose identityHashCode should also be appended
   * @return the Name of messaging engine if set, default name otherwise
   */

  public static String getMEName(Object o) {
        String meName;

    if (!threadLocalStack.isEmpty()) {
      meName = threadLocalStack.peek();
    } else {
      meName = DEFAULT_ME_NAME;
    }

    String str = "";

    if (o != null) {
      str = "/" + Integer.toHexString(System.identityHashCode(o));
    }
  // Defect 91793 to avoid SIBMessage in Systemout.log starting with [:] 
    return "";
  }

  /**
   * Get the full class name so that it can be printed in trace messages
   * @param tc The TraceComponent whose class name is required
   * @return String The full class name
   */
  private static String getFullClassName(TraceComponent tc)
  {
    return "("+tc.getName()+")";
  }

  /**
   * Push the current setting on to the stack and adopt the new setting supplied.
   * <p>
   * @param jsme JsMessagingEngine
   */

  @SuppressWarnings("unchecked")
  public static void push (Object jsme) {

    // Messing around required to avoid this build component placing a
    // dependency on the Admin JsMessagingEngine class

    Class klass = jsme.getClass();

    try {
      Method getBusName = klass.getMethod("getBusName",new Class[]{});
      Method getName = klass.getMethod("getName",new Class[]{});

      String bus = (String)getBusName.invoke(jsme);
      String name = (String)getName.invoke(jsme);

      threadLocalStack.push(bus + ":" + name);

    } catch (Exception e) {
      // No FFDC code needed
      exception(Tr.register(SibTr.class,"", ""),e);
    }
  }

  /**
   * Pop and adopt the new setting off the stack
   */

  public static void pop () {
    threadLocalStack.pop();
  }


  /**
   * Register a named component in the specified group with trace manager.
   * <p>
   * A component must register with the trace manager before it can use the
   * services provided by the convenience methods of this class. Components may
   * register multiple times with the same name, but the trace manager ensures
   * that such registrations return the same unique <code>TraceComponent</code>
   * associated with that name.
   * <p>
   * @param aClass a valid <code>Class</code> to register a component for with
   *        the trace manager. The className is used as the name in the
  *         registration process.
   * @param group the name of the group that the named component is a member of.
   *        Null is allowed. If null is passed, the name is not added to a
   *        group. Once added to a group, there is no corresponding mechanism
   *        to remove a component from a group.
   * @param resourceBundleName the name of the message properties file to use
   *        when providing national language support for messages logged by
   *        this component. All messages for this component must be found in
   *        this file. If null is passed, the current message file name is not
   *        changed.
   * @return the <code>TraceComponent</code> corresponding to the name of the
   *         specified class.
   */
  public static TraceComponent register( Class<?> aClass, String group,
                                         String resourceBundleName) {
    return Tr.register(aClass, group, resourceBundleName);
  }

  /**
   * If Audit level logging is enabled, forward a message event of type Audit
   * to all registered <code>TraceEventListener</code>s. Each registered
   * listener will determine whether to log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes no substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   */
  public static final void audit(TraceComponent tc, String msgKey) {
    audit (tc, msgKey, null);
  }

  /**
   * If Audit level logging is enabled, forward a message event of type Audit
   * to all registered <code>TraceEventListener</code>s. Each registered
   * listener will determine whether to log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   * @param objs an <code>Object</code> or array of <code>Objects</code> to
   *        include as substitution text in the message. The number of objects
   *        passed must equal the number of substitution parameters the message
   *        expects. Null is tolerated.
   */
  public static final void audit(TraceComponent tc, String msgKey, Object objs) {
    SibMessage.audit(getMEName(null), tc, msgKey, objs);
  }

  /**
   * If debug level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward a Debug event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msg text to include in the event. No translation or conversion is
   *        performed.
   */
  public static final void debug(TraceComponent tc, String msg) {
    debug(null, tc, msg);
  }

  /**
   * If debug level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward a Debug event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msg text to include in the event. No translation, conversion or
   *        formatting is performed.
   * @param objs an <code>Object</code> or array of <code>Objects</code>.
   *        toString() is called on each object and the results are appended to
   *        the message.
   */
  public static final void debug(TraceComponent tc, String msg, Object objs) {
    debug(null, tc, msg, objs);
  }

  /**
   * If debug level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward a Debug event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param o handle of the object making the trace call
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msg text to include in the event. No translation or conversion is
   *        performed.
   */
  public static final void debug(Object o, TraceComponent tc, String msg) {
    Tr.debug(tc, getFullClassName(tc)+" "+getMEName(o) + " " + msg);
  }

  /**
   * If debug level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward a Debug event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param o handle of the object making the trace call
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.                                                    2
   * @param msg text to include in the event. No translation, conversion or
   *        formatting is performed.
   * @param objs an <code>Object</code> or array of <code>Objects</code>.
   *        toString() is called on each object and the results are appended to
   *        the message.
   */
  public static final void debug(Object o, TraceComponent tc, String msg, Object objs) {
    Tr.debug(tc, getFullClassName(tc)+" "+getMEName(o) + " " + msg, objs);
  }

  /**
   * If dump level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward a Dump event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msg text to include in the event. No translation or conversion is
   *        performed.
   */
  public static final void dump(TraceComponent tc, String msg) {
    dump(null, tc, msg);
  }

  /**
   * If dump level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward a Dump event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msg text to include in the event. No translation, conversion or
   *        formatting is performed.
   * @param objs an <code>Object</code> or array of <code>Objects</code>.
   *        toString() is called on each object and the results are appended to
   *        the message.
   */
  public static final void dump(TraceComponent tc, String msg, Object objs) {
    dump(null, tc, msg, objs);
  }

  /**
   * If dump level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward a Dump event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param o handle of the object making the trace call
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msg text to include in the event. No translation or conversion is
   *        performed.
   */
  public static final void dump(Object o, TraceComponent tc, String msg) {
    Tr.dump(tc, getFullClassName(tc)+" "+getMEName(o) + " " + msg);
  }

  /**
   * If dump level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward a Dump event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param o handle of the object making the trace call
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msg text to include in the event. No translation, conversion or
   *        formatting is performed.
   * @param objs an <code>Object</code> or array of <code>Objects</code>.
   *        toString() is called on each object and the results are appended to
   *        the message.
   */
  public static final void dump(Object o, TraceComponent tc, String msg, Object objs) {
    Tr.dump(tc, getFullClassName(tc)+" "+getMEName(o) + " " + msg, objs);
  }

  /**
   * Unconditionally forward an Error message event to all registered
   * <code>TraceEventListener</code>s.  Each <code>TraceEventListener</code>
   * will determine whether to log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes no substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   */
  public static final void error(TraceComponent tc, String msgKey) {
    error(tc, msgKey, null);
  }

  /**
   * Unconditionally forward an Error message event to all registered
   * <code>TraceEventListener</code>s.
   * Each <code>TraceEventListener</code> will determine whether to log or
   * ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   * @param objs an <code>Object</code> or array of <code>Objects</code> to
   *        include as substitution text in the message. The number of objects
   *        passed must equal the number of substitution parameters the message
   *        expects.
   */
  public static final void error(TraceComponent tc, String msgKey, Object objs) {
    SibMessage.error(getMEName(null), tc, msgKey, objs);
  }

  /**
   * Forward an Error message event to all registered
   * <code>TraceEventListener</code>s if permitted by the Suppressor.
   * Each <code>TraceEventListener</code> will then
   * determine whether to log or ignore the forward event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param s the Suppressor that will determine if this message should be
   *        suppressed or not.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes no substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   */
  public static final void error(TraceComponent tc, Suppressor s, String msgKey) {
    error(tc, s, msgKey, null);
  }

  /**
   * Forward an Error message event to all registered
   * <code>TraceEventListener</code>s if permitted by the Suppressor.
   * Each <code>TraceEventListener</code> will then
   * determine whether to log or ignore the forward event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param s the Suppressor that will determine if this message should be
   *        suppressed or not.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   * @param objs an <code>Object</code> or array of <code>Objects</code> to
   *        include as substitution text in the message. The number of objects
   *        passed must equal the number of substitution parameters the message
   *        expects.
   */
  public static final void error(TraceComponent tc, Suppressor s, String msgKey, Object objs) {
      SibMessage.SuppressableError(s,getMEName(null), tc, msgKey, objs);
  }

  /**
   * If event level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward an Event event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msg text to include in the event. No translation or conversion is
   *        performed.
   */
  public static final void event(TraceComponent tc, String msg) {
    event(null, tc, msg);
  }

  /**
   * If event level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward an Event event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msg text to include in the event. No translation, conversion or
   *        formatting is performed.
   * @param objs an <code>Object</code> or array of <code>Objects</code>.
   *        toString() is called on each object and the results are appended to
   *        the message.
   */
  public static final void event(TraceComponent tc, String msg, Object objs) {
    event(null, tc, msg, objs);
  }

  /**
   * If event level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward an Event event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param o handle of the object making the trace call
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msg text to include in the event. No translation or conversion is
   *        performed.
   */
  public static final void event(Object o, TraceComponent tc, String msg) {
    Tr.event(tc, getFullClassName(tc)+" "+getMEName(o) + " " + msg);
  }

  /**
   * If event level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward an Event event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param o handle of the object making the trace call
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msg text to include in the event. No translation, conversion or
   *        formatting is performed.
   * @param objs an <code>Object</code> or array of <code>Objects</code>.
   *        toString() is called on each object and the results are appended to
   *        the message.
   */
  public static final void event(Object o, TraceComponent tc, String msg, Object objs) {
    Tr.event(tc, getFullClassName(tc)+" "+getMEName(o) + " " + msg, objs);
  }

  /**
   * If entry level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward an Entry event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param methodName the name of the method being entered.
   */
  public static final void entry(TraceComponent tc, String methodName) {
    entry(null, tc, methodName);
  }

  /**
   * If entry level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward an Entry event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param methodName the name of the method being entered.
   * @param obj an <code>Object</code> or array of <code>Objects</code>.
   *        toString() is called on each object and the results are appended to
   *        the methodName.
   */
  public static final void entry(TraceComponent tc, String methodName, Object obj) {
    entry(null, tc, methodName, obj);
  }

  /**
   * If entry level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward an Entry event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param o handle of the object making the trace call
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param methodName the name of the method being entered.
   */
  public static final void entry(Object o, TraceComponent tc, String methodName) {
    Tr.entry(tc, methodName + " " + getFullClassName(tc)+" "+ getMEName(o));
  }

  /**
   * If entry level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward an Entry event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param o handle of the object making the trace call
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param methodName the name of the method being entered.
   * @param obj an <code>Object</code> or array of <code>Objects</code>.
   *        toString() is called on each object and the results are appended to
   *        the methodName.
   */
  public static final void entry(Object o, TraceComponent tc, String methodName, Object obj) {
    Tr.entry(tc, methodName + " " + getFullClassName(tc)+" "+getMEName(o), obj);
  }

  /**
   * If exit level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward an Exit event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param methodName the name of the method being exited.
   */
  public static final void exit(TraceComponent tc, String methodName) {
    exit(null, tc, methodName);
  }

  /**
   * If exit level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward an Exit event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param methodName the name of the method logging the method exit event.
   * @param objs an <code>Object</code> or array of <code>Objects</code>.
   *        toString() is called on each object and the results are appended to
   *        the methodName.
   */
  public static final void exit(TraceComponent tc, String methodName, Object objs) {
    exit(null, tc, methodName, objs);
  }

  /**
   * If exit level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward an Exit event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param o handle of the object making the trace call
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param methodName the name of the method being exited.
   */
  public static final void exit(Object o, TraceComponent tc, String methodName) {
    Tr.exit(tc, methodName + " " + getFullClassName(tc)+" "+getMEName(o));
  }

  /**
   * If exit level diagnostic trace is enabled for the specified
   * <code>TraceComponent</code>, forward an Exit event to all registered
   * TraceEventListeners. Each registered listener will determine whether to
   * log or ignore the event.
   * <p>
   * @param o handle of the object making the trace call
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param methodName the name of the method logging the method exit event.
   * @param objs an <code>Object</code> or array of <code>Objects</code>.
   *        toString() is called on each object and the results are appended to
   *        the methodName.
   */
  public static final void exit(Object o, TraceComponent tc, String methodName, Object objs) {
    Tr.exit(tc, methodName + " " + getFullClassName(tc)+" "+getMEName(o), objs);
  }

  /**
   * Unconditionally forward a Fatal message event to all registered
   * <code>TraceEventListener</code>s. Each <code>TraceEventListener</code>
   * will determine whether to log or ignore the event.
   * <p>
   * In response to a fatal event, all objects that implement the
   * <code>TraceCallback</code> interface and have registered with the
   * <code>ComponentManager</code> to be called back in the case of a fatal
   * event will have their <code>exitCallback()</code> method called.
   * <p>
   * In the current implementation, the current process is exited and the
   * contents of the trace ring buffer, if any, along with a stack backtrace
   * are dumped to the current trace destination.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes no substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   */
  public static final void fatal(TraceComponent tc, String msgKey) {
    fatal(tc, msgKey, null);
  }

  /**
   * Unconditionally forward a Fatal message event to all registered
   * <code>TraceEventListener</code>s. Each <code>TraceEventListener</code>
   * will determine whether to log or ignore the event.
   * <p>
   * In response to a fatal event, all objects that implement the
   * <code>TraceCallback</code> interface and have registered with the
   * <code>ComponentManager</code> to be called back in the case of a fatal
   * event will have their <code>exitCallback()</code> method called.
   * <p>
   * In the current implementation, the current process is exited and the
   * contents of the trace ring buffer, if any, along with a stack backtrace
   * are dumped to the current trace destination.
   *
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   * with.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   * @param objs an <code>Object</code> or array of <code>Objects</code> to
   *        include as substitution text in the message. The number of objects
   *        passed must equal the number of substitution parameters the message
   *        expects.
   */
  public static final void fatal(TraceComponent tc, String msgKey, Object objs) {
    SibMessage.fatal(getMEName(null), tc, msgKey, objs);
  }

  /**
   * Unconditionally forward an Info message event to all registered
   * <code>TraceEventListener</code>s. Each <code>TraceEventListener</code>
   * will determine whether to log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes no substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   */
  public static final void info(TraceComponent tc, String msgKey) {
    info(tc, msgKey, null);
  }

  /**
   * Unconditionally forward an Info message event to all registered
   * <code>TraceEventListener</code>s.  Each <code>TraceEventListener</code>
   * will determine whether to log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes no substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   * @param objs an <code>Object</code> or array of <code>Objects</code> to
   *        include as substitution text in the message. The number of objects
   *        passed must equal the number of substitution parameters the message]
   *        expects.
   */
  public static final void info(TraceComponent tc, String msgKey, Object objs) {
    SibMessage.info(getMEName(null), tc, msgKey, objs);
  }

  /**
   * Forward an Info message event to all registered
   * <code>TraceEventListener</code>s if permitted by the Suppressor.
   * Each <code>TraceEventListener</code> will then
   * determine whether to log or ignore the forward event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param s the Suppressor that will determine if this message should be
   *        suppressed or not.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes no substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   */
  public static final void info(TraceComponent tc, Suppressor s, String msgKey) {
    info(tc, s, msgKey, null);
  }

  /**
   * Forward an Info message event to all registered
   * <code>TraceEventListener</code>s if permitted by the Suppressor.
   * Each <code>TraceEventListener</code> will then
   * determine whether to log or ignore the forward event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param s the Suppressor that will determine if this message should be
   *        suppressed or not.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes no substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   * @param objs an <code>Object</code> or array of <code>Objects</code> to
   *        include as substitution text in the message. The number of objects
   *        passed must equal the number of substitution parameters the message]
   *        expects.
   */
  public static final void info(TraceComponent tc, Suppressor s, String msgKey, Object objs) {
      SibMessage.SuppressableInfo(s,getMEName(null), tc, msgKey, objs);
  }

  /**
   * If Service level logging is enabled, forward a message event of type
   * Service to all registered <code>TraceEventListener</code>s. Each
   * registered listener will determine whether to log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes no substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   */
  public static final void service(TraceComponent tc, String msgKey) {
    service(tc, msgKey, null);
  }

  /**
   * If Service level logging is enabled, forward a message event of type
   * Service to all registered <code>TraceEventListener</code>s. Each registered
   * listener will determine whether to log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   * @param objs an <code>Object</code> or array of <code>Objects</code> to
   *        include as substitution text in the message. The number of objects
   *        passed must equal the number of substitution parameters the message
   *        expects. Null is tolerated.
   */
  public static final void service(TraceComponent tc, String msgKey, Object objs) {
    SibMessage.service(getMEName(null), tc, msgKey, objs);
  }

  /**
   * If Warning level logging is enabled, forward a message event of type
   * Warning to all registered <code>TraceEventListener</code>s. Each registered
   * listener will determine whether to log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes no substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   */
  public static final void warning(TraceComponent tc, String msgKey) {
    warning(tc, msgKey, null);
  }

  /**
   * If Warning level logging is enabled, forward a message event of type
   * Warning to all registered <code>TraceEventListener</code>s. Each registered
   * listener will determine whether to log or ignore the event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   * @param objs an <code>Object</code> or array of <code>Objects</code> to
   *        include as substitution text in the message. The number of objects
   *        passed must equal the number of substitution parameters the message
   *        expects.
   */
  public static final void warning(TraceComponent tc, String msgKey, Object objs) {
    SibMessage.warning(getMEName(null), tc, msgKey, objs);
  }

  /**
   * Forward an Warning message event to all registered
   * <code>TraceEventListener</code>s if permitted by the Suppressor.
   * Each <code>TraceEventListener</code> will then
   * determine whether to log or ignore the forward event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param s the Suppressor that will determine if this message should be
   *        suppressed or not.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes no substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   */
  public static final void warning(TraceComponent tc, Suppressor s, String msgKey) {
    warning(tc, s, msgKey, null);
  }

  /**
   * Forward an Warning message event to all registered
   * <code>TraceEventListener</code>s if permitted by the Suppressor.
   * Each <code>TraceEventListener</code> will then
   * determine whether to log or ignore the forward event.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param s the Suppressor that will determine if this message should be
   *        suppressed or not.
   * @param msgKey the message key identifying an NLS message for this event.
   *        This message takes substitution parameters and must be in the
   *        resource bundle currently associated with the
   *        <code>TraceComponent</code>.
   * @param objs an <code>Object</code> or array of <code>Objects</code> to
   *        include as substitution text in the message. The number of objects
   *        passed must equal the number of substitution parameters the message
   *        expects.
   */
  public static final void warning(TraceComponent tc, Suppressor s, String msgKey, Object objs) {
      SibMessage.SuppressableWarning(s,getMEName(null), tc, msgKey, objs);
 }

  /*
   * Byte array formatting methods
   */

  private final static String ls = System.lineSeparator();
  private final static String DEAD_CHAR = ".";

  private static String pad (String s, int l) {
    return pad(s,l, null);
  }

  private static String pad (String s, int l, String p) {
    String rc;

    if (p == null) p = "0";

    if (s.length() < l) {
      StringBuffer sb = new StringBuffer();
      for (int i=0; i < l - s.length(); i++) sb.append(p);
      rc = sb.toString()+s;
    } else rc = s.substring(s.length()-l);

    return rc;
  }

  private static String dup (int i) {
    return "                          " + i + " duplicate line(s) suppressed"+ls;
  }

  /**
   * If debug level tracing is enabled then trace a byte array using formatted
   * output with offsets. Duplicate output lines are suppressed to save space.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param data the byte array to be traced
   */

  public static void bytes (TraceComponent tc, byte[] data) {
    int length = 0;
    if (data != null) length = data.length;
    bytes(null, tc, data, 0, length, "");
  }

  /**
   * If debug level tracing is enabled then trace a byte array using formatted
   * output with offsets. Duplicate output lines are suppressed to save space.
   * <p>
   * @param o handle of the object making the trace call
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param data the byte array to be traced
   */

  public static void bytes (Object o, TraceComponent tc, byte[] data) {
    int length = 0;
    if (data != null) length = data.length;
    bytes(o, tc, data, 0, length, "");
  }

  /**
   * If debug level tracing is enabled then trace a byte array using formatted
   * output with offsets. Duplicate output lines are suppressed to save space.
   * Tracing of the byte array starts at the specified position and continues
   * to the end of the byte array.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param data the byte array to be traced
   * @param start position to start tracing the byte array
   */

  public static void bytes (TraceComponent tc, byte[] data, int start) {
    int length = 0;
    if (data != null) length = data.length;
    bytes(null, tc, data, start, length, "");
  }

  /**
   * If debug level tracing is enabled then trace a byte array using formatted
   * output with offsets. Duplicate output lines are suppressed to save space.
   * Tracing of the byte array starts at the specified position and continues
   * to the end of the byte array.
   * <p>
   * @param o handle of the object making the trace call
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param data the byte array to be traced
   * @param start position to start tracing the byte array
   */

  public static void bytes (Object o, TraceComponent tc, byte[] data, int start) {
    int length = 0;
    if (data != null) length = data.length;
    bytes(o, tc, data, start, length, "");
  }

  /**
   * If debug level tracing is enabled then trace a byte array using formatted
   * output with offsets. Duplicate output lines are suppressed to save space.
   * Tracing of the byte array starts at the specified position and continues
   * for the specified number of bytes.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param data the byte array to be traced
   * @param start position to start tracing the byte array
   * @param count of bytes from start position that should be traced
   */

  public static void bytes (TraceComponent tc, byte[] data, int start, int count) {
    bytes(null, tc, data, start, count, "");
  }

  /**
   * If debug level tracing is enabled then trace a byte array using formatted
   * output with offsets. Duplicate output lines are suppressed to save space.
   * Tracing of the byte array starts at the specified position and continues
   * for the specified number of bytes.
   * <p>
   * @param o handle of the object making the trace call
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param data the byte array to be traced
   * @param start position to start tracing the byte array
   * @param count of bytes from start position that should be traced
   */

  public static void bytes (Object o, TraceComponent tc, byte[] data, int start, int count) {
    bytes(o, tc, data, start, count, "");
  }

  /**
   * If debug level tracing is enabled then trace a byte array using formatted
   * output with offsets. Duplicate output lines are suppressed to save space.
   * Tracing of the byte array starts at the specified position and continues
   * for the specified number of bytes. A comment is provided which describes
   * the data being traced.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param data the byte array to be traced
   * @param start position to start tracing the byte array
   * @param count of bytes from start position that should be traced
   * @param comment a comment to be associated with the traced byte array indicating the
   *        type of data being traced
   */

  public static void bytes (TraceComponent tc, byte[] data, int start, int count, String comment) {
    bytes(null, tc, data, start, count, comment);
  }

  /**
   * If debug level tracing is enabled then trace a byte array using formatted
   * output with offsets. Duplicate output lines are suppressed to save space.
   * Tracing of the byte array starts at the specified position and continues
   * for the specified number of bytes. A comment is provided which describes
   * the data being traced.
   * <p>
   * @param o handle of the object making the trace call
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param data the byte array to be traced
   * @param start position to start tracing the byte array
   * @param count of bytes from start position that should be traced
   * @param comment a comment to be associated with the traced byte array indicating the
   *        type of data being traced
   */

  public static void bytes (Object o, TraceComponent tc, byte[] data, int start, int count, String comment) {
    StringBuffer sb = new StringBuffer();

    if (comment == null) comment = "";
    sb.append(getFullClassName(tc)+" "+getMEName(o) + " " + comment + ls);

    if (data != null)
      sb.append(formatBytes(data, start, count));
    else
      sb.append("data is null");

    Tr.debug(tc,sb.toString());
  }

  /**
   * Produce a formatted view of a DataSlice.  Duplicate output lines are
   * suppressed to save space.
   * <p>
   * @param slice The DataSlice to be formatted
   * @return the formatted data slice
   */

  public static String formatSlice(DataSlice slice)
  {
    StringBuilder sb = new StringBuilder(256);
    formatSliceToSB(sb, slice, Integer.MAX_VALUE);
    return sb.toString();
  }


  /**
   * Produce a formatted view of a DataSlice.  Duplicate output lines are
   * suppressed to save space.
   * <p>
   * @param sb the StringBuilder to the end of which the slice will be formatted
   * @param slice The DataSlice to be formatted
   * @param max The maximum amount of data to format for the slice
   * @return the formatted data slice
   */
  private static void formatSliceToSB(StringBuilder sb, DataSlice slice, int max)
  {
    if (slice != null)
    {
      if (slice.getBytes() != null)
      {
        formatBytesToSB(sb, slice.getBytes(), slice.getOffset(), slice.getLength(), true, max);
      }
      else
      {
        sb.append("empty slice"+ls);
      }
    }
    else
    {
      sb.append("slice is null"+ls);
    }
  }

  /**
   * Produce a formatted view of a list of DataSlices.  Duplicate output lines are
   * suppressed to save space.
   * <p>
   * @param slices The list of DataSlices to be formatted
   * @return the formatted data slice
   */

  public static String formatSlices(List<DataSlice> slices)
  {
    return formatSlices(slices, MAX_TO_FORMAT);
  }


  /**
   * Produce a formatted view of a list of DataSlices.  Duplicate output lines are
   * suppressed to save space.
   * <p>
   * @param slices The list of DataSlices to be formatted
   * @return the formatted data slice
   * @param max maximun number of bytes that should be formatted for each slice.
   */

  public static String formatSlices(List<DataSlice> slices, int max)
  {
    if (slices != null)
    {
      StringBuilder builder = new StringBuilder();

      if (slices.size() != 0)
      {
        int number = 1;
        int sliceCount = slices.size();

        for(DataSlice slice : slices)
        {
          builder.append("List<DataSlice>@");
          builder.append(Integer.toHexString(System.identityHashCode(slices)));
          builder.append(" Slice ");
          builder.append(number++);
          builder.append(" of ");
          builder.append(sliceCount);

          // OK, now get the slice added into the builder
          builder.append(" :" + ls + "  ");
          formatSliceToSB(builder, slice, max);
          builder.append(ls);
        }
      }
      else
      {
        builder.append("List<DataSlice>@");
        builder.append(Integer.toHexString(System.identityHashCode(slices)));
        builder.append(" has no slices");
      }

      return builder.toString();
    }
    else
    {
      return "List<DataSlice> is null";
    }
  }

  /**
   * Produce a formatted view of a byte array.  Duplicate output lines are
   * suppressed to save space.
   * <p>
   * @param data the byte array to be formatted
   * @return the formatted byte array, or an empty StringBuffer if null was passed in.
   */
  public static String formatBytes (byte[] data) {
    // If the data exists, format it.
    if (data != null) {
      return formatBytes(data, 0, data.length, true);
    }
    // Otherwise, return an empty String as the overloaded method would have done.
    else {
      return "";
    }
  }

  /**
   * Produce a formatted view of a byte array.  Duplicate output lines are
   * suppressed to save space.
   * Formatting of the byte array starts at the specified position and continues
   * for the specified number of bytes or the end of the data.
   * <p>
   * @param data the byte array to be formatted
   * @param start position to start formatting the byte array
   * @param count of bytes from start position that should be formatted
   * @return the formatted byte array
   */

  public static String formatBytes (byte[] data, int start, int count) {
    return formatBytes(data, start, count, true);
  }

  /**
   * Produce a formatted view of a limited portion of a byte array.
   * Duplicate output lines are suppressed to save space.
   * Formatting of the byte array starts at the specified position and continues
   * for count, max, or the end of the data, whichever occurs first.
   * <p>
   * @param data the byte array to be formatted
   * @param start position to start formatting the byte array
   * @param count of bytes from start position that should be formatted
   * @param max maximun number of bytes from start position that should be formatted,
   *            regardless of the value of length.
   * @return the formatted byte array
   */

  public static String formatBytes (byte[] data, int start, int count, int max) {
    StringBuilder sb = new StringBuilder(256);
    sb.append(ls);
    formatBytesToSB(sb, data, start, count, true, max);
    return sb.toString();
  }

  /**
   * Produce a formatted view of a byte array.  Duplicate output lines are
   * suppressed to save space.
   * Formatting of the byte array starts at the specified position and continues
   * for the specified number of bytes or the end of the data.
   * The bytes will also be converted to into it's String equivalent using the
   * platform default character set. This will be displayed if the
   * <code>displayCharRepresentation</code> flag is set to true.
   * <p>
   * @param data the byte array to be formatted
   * @param start position to start formatting the byte array
   * @param count of bytes from start position that should be formatted
   * @param displayCharRepresentations Whether to display the character representation
   * @return the formatted byte array
   */

  public static String formatBytes (byte[] data, int start, int count, boolean displayCharRepresentations) {
    StringBuilder sb = new StringBuilder(256);
    sb.append(ls);
    // Just call the method which does the real work, requesting it to prepend a line-separator
    formatBytesToSB(sb, data, start, count, true, count);
    return sb.toString();
  }

  /**
   * Produce a StringBuilder containing a formatted view of a byte array.
   * Duplicate output lines are suppressed to save space.
   * Formatting of the byte array starts at the specified position and continues
   * for the specified number of bytes or the end of the data.
   * The bytes will also be converted to into it's String equivalent using the
   * platform default character set. This will be displayed if the
   * <code>displayCharRepresentation</code> flag is set to true.
   * <p>
   * @param sb the StringBuilder to the end of which the formatted byte array will be added
   * @param data the byte array to be formatted
   * @param start position to start formatting the byte array
   * @param countRequested of bytes from start position that should be formatted
   * @param displayCharRepresentations Whether to display the character representation
   * @param max maximun number of bytes that should be formatted for the byte array
   */

  private static void formatBytesToSB (StringBuilder sb, byte[] data, int start, int countRequested, boolean displayCharRepresentations, int max) {

    if (max > MAX_TO_FORMAT) max = MAX_TO_FORMAT;                    // Ensure we can't be asked to format a completely bonkers amount of data
    int count = (countRequested <= max + 16) ? countRequested : max; // Determine how many bytes to format (giving 16 bytes leeway)

    if (data != null) {
      int len = data.length;
      sb.append("Array length = 0x"+Integer.toHexString(len)+" ("+len+"), displaying bytes from "+start+" for "+count);
      if (count < countRequested) {
        sb.append(" ("+countRequested+" bytes requested)");
      }
      sb.append(ls+ls);
      if (displayCharRepresentations)
        sb.append("        offset        : 0 1 2 3  4 5 6 7  8 9 A B  C D E F     0 2 4 6 8 A C E " + ls);
      else
        sb.append("        offset        : 0 1 2 3  4 5 6 7  8 9 A B  C D E F" + ls);

      int t;
      boolean skip;
      int suppress = 0;
      int end = start + count;
      String c[] = new String[16];  // Current line bytes
      String p[] = new String[16];  // Previous line bytes
      String str[] = new String[16];// The string representation

      for (int j=0; j < 16; j++) {
        c[j] = null;
        str[j] = null;
      }

      for (int i=0; i < len; i = i+16) {
        skip = true;

        for (int j=0; j < 16; j++) {
          t = i +  j;
          if ((t >= start) && (t < end) && (t < len)) {
            c[j] = pad(Integer.toHexString(data[t]),2);
            // Strip out some known 'bad-guys' (these are consistent across ASCII / EBCIDIC)
            // and replace them with the dead character
            if (c[j].equalsIgnoreCase("00") ||      // Null
                c[j].equalsIgnoreCase("09") ||      // Tab
                c[j].equalsIgnoreCase("0a") ||      // LF
                c[j].equalsIgnoreCase("0b") ||      // VertTab
                c[j].equalsIgnoreCase("0c") ||      // FF
                c[j].equalsIgnoreCase("0d") ||      // CR
                c[j].equalsIgnoreCase("07"))        // Bell
            {
               str[j] = DEAD_CHAR;
            }
            else
            {
               str[j] = new String(data, t, 1);     // Conversion is done here using the default
                                                    // character set of the platform
            }
            skip = false;
          }
          else {
            c[j] = "  ";
            str[j] = DEAD_CHAR;
          }
        }

        if (skip) {
          if (suppress > 0)
            sb.append(dup(suppress));
          suppress = 0;
          c[0] = null;              // Force a line difference
        }
        else {
          if (c[0].equals(p[0]) && c[1].equals(p[1]) && c[2].equals(p[2]) && c[3].equals(p[3]) &&
              c[4].equals(p[4]) && c[5].equals(p[5]) && c[6].equals(p[6]) && c[7].equals(p[7]) &&
              c[8].equals(p[8]) && c[9].equals(p[9]) && c[10].equals(p[10]) && c[11].equals(p[11]) &&
              c[12].equals(p[12]) && c[13].equals(p[13]) && c[14].equals(p[14]) && c[15].equals(p[15])) {
            suppress++;
          }
          else {
            if (suppress > 0) sb.append(dup(suppress));
            sb.append("0x"+pad(Integer.toHexString(i),8)+" ("+pad(Integer.valueOf(i).toString(),8," ")+") : ");
            sb.append(c[0]+c[1]+c[2]+c[3]+" "+c[4]+c[5]+c[6]+c[7]+" "+c[8]+c[9]+c[10]+c[11]+" "+c[12]+c[13]+c[14]+c[15]);
            if (displayCharRepresentations) {
              sb.append("  | ");
              sb.append(str[0]+str[1]+str[2]+str[3]+str[4]+str[5]+str[6]+str[7]+str[8]+str[9]+str[10]+str[11]+str[12]+str[13]+str[14]+str[15]);
            }
            sb.append(ls);
            for (int j=0; j < 16; j++) p[j] = c[j];
            suppress = 0;
          }
        }
      }

      if (suppress > 0) sb.append(dup(suppress));
    }

    // If the number of bytes formatted was fewer than requested, say so.
    if (count < countRequested) {
      sb.append("Suppressed remaining " + (countRequested-count) + " bytes." + ls);
    }
  }

  /**
   * If event level trace is enabled then trace an Exception and its associated
   * stack trace and any linked exceptions.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param e the Exception object to be traced
   */

  public static void exception (TraceComponent tc, Exception e) {
    exception(null, tc, (Throwable)e);
  }

  /**
   * If event level trace is enabled then trace an Exception and its associated
   * stack trace and any linked exceptions.
   * <p>
   * @param o handle of the object making the trace call
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param e the Exception object to be traced
   */

  public static void exception (Object o, TraceComponent tc, Exception e) {
    exception(o, tc, (Throwable)e);
  }

  /**
   * If event level trace is enabled then trace a Throwable and its associated
   * stack trace and any linked exceptions.
   * <p>
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param t the Throwable object to be traced
   */

  public static void exception (TraceComponent tc, Throwable t) {
    exception(null, tc, t);
  }

  /**
   * If event level trace is enabled then trace a Throwable and its associated
   * stack trace and any linked exceptions.
   * <p>
   * @param o handle of the object making the trace call
   * @param tc the non-null <code>TraceComponent</code> the event is associated
   *        with.
   * @param t the Throwable object to be traced
   */

  public static void exception (Object o, TraceComponent tc, Throwable t) {
     StringBuffer sb = new StringBuffer();
     sb.append(getFullClassName(tc)+" "+getMEName(o) + " " + "Tracing exception:" + ls);
     if (t != null) {
        CharArrayWriter caw = new CharArrayWriter();
        PrintWriter pw = new PrintWriter(caw);

        t.printStackTrace(pw);
        pw.flush();
        sb.append(caw.toString());

     }
     else {
        sb.append("exception argument was null");
     }
     Tr.event(tc,sb.toString());
  }

}
