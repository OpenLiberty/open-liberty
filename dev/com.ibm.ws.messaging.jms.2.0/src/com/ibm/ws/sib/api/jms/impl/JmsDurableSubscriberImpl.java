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

import javax.jms.InvalidDestinationException;
import javax.jms.InvalidSelectorException;
import javax.jms.JMSException;
import javax.jms.JMSSecurityException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.ws.sib.api.jms.JmsInternalsFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectorDomain;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionAlreadyExistsException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;

/**
 * A subclass of TopicSubscriberImpl for consumers which are connected
 * to Durable Subscriptions.
 */
public class JmsDurableSubscriberImpl extends JmsTopicSubscriberImpl
{
  //************************** TRACE INITIALISATION ***************************

  private static TraceComponent tc = SibTr.register(JmsDurableSubscriberImpl.class, ApiJmsConstants.MSG_GROUP_INT, ApiJmsConstants.MSG_BUNDLE_INT);

  // ***************************** CONSTANTS **********************************

  /**
   * State constants to control the creation loop.
   */
  private static byte NOT_TRIED = 1;
  private static byte COMPLETE = 2;
  private static byte REQUEST_ALTER = 3;
  private static byte TRY_CREATE = 4;

  /**
   * This flag turns on some println statements. Should be disabled when checked
   * into CMVC.
   */
  static final boolean DEVT_DEBUG = false;

  //***************************** CONSTRUCTORS ********************************

  /**
   * @param coreConnection
   * @param dest
   * @param sessionLock
   * @param selector
   * @param noLocal
   * @param newSession
   * @throws JMSException
   */
  public JmsDurableSubscriberImpl(SICoreConnection coreConnection, JmsSessionImpl newSession, ConsumerProperties newProps) throws JMSException {
    // Calling the parent constructor will eventually cause the createCoreConsumer
    // method to be called. We override that method here to connect to the durable
    // subscription rather than create a non-durable subscriber for the topic.
    super(coreConnection, newSession, newProps);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "JmsDurableSubscriberImpl");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "JmsDurableSubscriberImpl");
  }

//************************* IMPLEMENTATION METHODS **************************

  /**
   * This method overrides the createCoreConsumer method in JmsMsgConsumerImpl
   * to connect to a durable subscription rather than a regular consumer session.
   *
   * Note: Care should be taken when altering this method signature to ensure
   *       that it matches the method in JmsMsgConsumerImpl.
   */
  protected ConsumerSession createCoreConsumer(SICoreConnection _coreConn, ConsumerProperties _props) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "createCoreConsumer", new Object[]{_coreConn, _props});

    if (DEVT_DEBUG) System.out.println("Overidden create!");

    ConsumerSession dcs = null;

    // Determine the correct subscription name to use (concatenation
    // of clientID and subName.
    String clientID = _props.getClientID();
    String subName = _props.getSubName();
    String durableSubHome = _props.getDurableSubscriptionHome();
    String coreSubscriptionName = JmsInternalsFactory.getSharedUtils().getCoreDurableSubName(clientID, subName);

    if (DEVT_DEBUG) System.out.println("SUBSCRIBE: "+coreSubscriptionName);
    if (DEVT_DEBUG) System.out.println(_props.debug());

    // Retrieve the destination object for repeated use.
    JmsDestinationImpl cccDest = (JmsDestinationImpl)_props.getJmsDestination();

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
      // Trace out all the parameters to the create call.
      SibTr.debug(this, tc, "subscriptionName: "+coreSubscriptionName+", destName: "+cccDest.getDestName()+", discrim: "+cccDest.getDestDiscrim()+", selector: "+_props.getSelector());
      SibTr.debug(this, tc, "readAhead: "+_props.readAhead()+", supportsMultiple: "+_props.supportsMultipleConsumers()+", noLocal: "+_props.noLocal()+", durableSubHome: "+_props.getDurableSubscriptionHome());

      // Check the values for supportsMultiple and readAhead to
      // see if they conflict. This may happen if the user has explicitly
      // set readAhead ON, then cloned their application server. This might
      // result in all the messages being streamed to a single consumer
      // rather than being shared equally across them. (192474).

      // The concerning situations are where both supportsMultiple and
      // readAhead are set to the same thing. By using XOR (^) and
      // comparing for false we can see if they are set to the same thing.
      if (!(_props.supportsMultipleConsumers() ^ _props.readAhead())) {
        // They are both set to the same thing.
        if (_props.supportsMultipleConsumers()) {
          // They are both set to true. This means all the messages might
          // be streamed to a single consumer.
          SibTr.debug(this, tc, "WARNING: shareDurableSubs and readAhead are both ON."
                       +  " This could lead to all messages being streamed to a single consumer, which is inefficient.");
        }
        else {
          // They are both set to false. This means that we are not
          // streaming messages to the consumer, even though we are
          // guaranteed that there is only one. This might not give
          // optimum performance.
          SibTr.debug(this, tc, "WARNING: shareDurableSubs and readAhead are both OFF."
                        + "  This prevents the readAhead optimisation from taking place to pass messages pre-emptively to the single consumer."
                        + "  Performance would be improved if readAhead was DEFAULT or ON");
        }
      }
    } // end of stuff only done if Trace enabled

    /*
     * We are about to attempt to create or attach the durable subscription.
     *
     * If the parameters we pass in do not match the ones that were used to
     * create the subscription then we will need to alter the subscription,
     * which is done by delete and re-create.
     *
     * For an alter request we have the following behaviour;
     *   createDurableSubscription throws SIDestinationAlreadyExistsException
     *   deleteDurableSubscription
     *   createDurableSubscription
     *
     * The following do-while loop handles the retry of the create.
     *
     * d245910 - The original code would throw an exception to the application
     *   if a create/connect sequence failed. This has been changed to retry.
     *
     */
    byte create_state = NOT_TRIED;

    // Need to create a destination address object for this subscriber call.
    SIDestinationAddress sida = cccDest.getConsumerSIDestinationAddress();

    // Need to create a selection criteria object for this subscriber call.
    SelectionCriteria selectionCriteria = null;
    try {
      selectionCriteria = selectionCriteriaFactory.createSelectionCriteria(cccDest.getDestDiscrim(), // discriminator (topic)
                                                                           _props.getSelector(),     // selector string
                                                                           SelectorDomain.JMS        // selector domain
                                                                           );
    }
    catch(SIErrorException sice) {
      // No FFDC code needed
      // SIErrorException is described as a "should never happen" indicator.
      // No further detail is given in the javadoc for createSelectionCriteria,
      // so ExceptionReceived seems appropriate for this case.
      // d238447 FFDC Review. Generate FFDC for this case.
      throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                      "EXCEPTION_RECEIVED_CWSIA0221",
                                                      new Object[] {sice, "JmsDurableSubscriberImpl.createCoreConsumer (#10)"},
                                                      sice,
                                                      "JmsDurableSubscriberImpl.createCoreConsumer#10",
                                                      this,
                                                      tc
                                                     );
    }

    // This do-while loop handles the retry.
    do {
      try {

        // This block is used if we are going round the loop for a second time
        // to do the subscription altering.
        if (create_state == REQUEST_ALTER) {
          // We have been through the loop once already, and the create call
          // failed due to a DestAlreadyExists exception.
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Try to delete subscription: "+coreSubscriptionName);
          // Delete the subscription
          _coreConn.deleteDurableSubscription(coreSubscriptionName, durableSubHome);
          // Now we need to create it again.
          create_state = TRY_CREATE;
        }

        // This block is used when going round the loop for a second time
        // to do the durable sub create.
        if (create_state == TRY_CREATE) {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Try to create subscription: "+coreSubscriptionName);
          _coreConn.createDurableSubscription(coreSubscriptionName,
                                              durableSubHome,
                                              sida,
                                              selectionCriteria,
                                              _props.supportsMultipleConsumers(),
                                              _props.noLocal(),
                                              null // alternateUserID
                                              );
        }


        // Attempt to connect to the durable subscription. Note that this
        // is separate to the act of creation.
        dcs = _coreConn.createConsumerSessionForDurableSubscription(coreSubscriptionName,
                                                                    durableSubHome,
                                                                    sida,
                                                                    selectionCriteria,
                                                                    _props.supportsMultipleConsumers(),
                                                                    _props.noLocal(),
                                                                    null,
                                                                    _props.readAhead(),
                                                                    Reliability.NONE,
                                                                    false,  // bifurcatable
                                                                    null    // alternateUserID
                                                                    );
        // The operation succeeded as we expected.
        create_state = COMPLETE;
      }
      catch (SIDurableSubscriptionMismatchException daee) {
        // No FFDC code needed
        // Name of this durable subscription clashes with an existing destination.
        // (ie we just attempted to alter the subscription)
        if (create_state != REQUEST_ALTER) {
          // This is the first time we have seen this exception type.
          // It most likely means that we need to alter the subscription.
          create_state = REQUEST_ALTER;
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Exception received from createDurableSubscription: ", daee);
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Now try to alter the subscription");
        }
        else {
          // d222942 review
          // I don't think we can ever get to this code block (create_state == REQUEST_ALTER)
          // since create_state is changed from REQUEST_ALTER to TRY_CREATE after the call to
          // deleteSubscription, which has to have succeeded before a call to
          // createConsumerSessionForDurableSubscription would be attempted.
          // d245910 Matt and JBK agree we can't get here with current code, but rather than
          // delete the block, we'll leave it here but with a stronger debug message. This
          // should protect us against future code changes that might bring this case into play.
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "SHOULDN'T HAVE GOT HERE, PLEASE REPORT/INVESTIGATE");

          // NB. Must throw exception here to avoid infinite looping!
          // d238447 FFDC Review. Generate FFDC for this case.
          throw (JMSException) JmsErrorUtils.newThrowable(InvalidDestinationException.class,
                                                          "EXCEPTION_RECEIVED_CWSIA0221",
                                                          new Object[] {daee, "JmsDurableSubscriberImpl.createCoreConsumer (#1)"},
                                                          daee,
                                                          "JmsDurableSubscriberImpl.createCoreConsumer#1",
                                                          this,
                                                          tc
                                                         );
        }
      }
      catch(SIConnectionUnavailableException oce) {
        // No FFDC code needed
        // Method invoked on a closed connection
        // d222942 review
        // Looks like this could happen as part of 'normal' operation if
        // the connection was closed from one thread whilst another was
        // calling createDurableSubscriber.
        // New message for connection closed during method.
        // NB. Must throw exception here to avoid infinite looping!
        // d238447 FFDC review. Since this can happen during normal operation, we shouldn't generate an FFDC.
        throw (JMSException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                        "CONN_CLOSED_CWSIA0222",
                                                        null,
                                                        oce,
                                                        null, // null probeId = no FFDC.
                                                        this,
                                                        tc
                                                       );
      }
      catch(SIDestinationLockedException dle) {
        // No FFDC code needed
        // Destination is not accepting consumers - probably means that there is
        // already an active subscriber for this durable subscription
        // NB. Must throw exception here to avoid infinite looping!
        // d238447 FFDC review. App error, not internal, so no FFDC.
        throw (JMSException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                        "DEST_LOCKED_CWSIA0223",
                                                        null,
                                                        dle,
                                                        null, // null probeId = no FFDC.
                                                        this,
                                                        tc
                                                       );
      }
      catch(SIDurableSubscriptionNotFoundException dnfe) {
        // No FFDC code needed
        // The destination cannot be found (no durable subscription).
        // At this point we try to create it.
        if (create_state != TRY_CREATE) {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "The durable subscription could not be found - create it");
          create_state = TRY_CREATE;
        }
        else {
          // d222942 review
          // To get here implies that createDurableSubscription succeeded, and then
          // createConsumerSessionForDurableSubscription failed with a notFoundException.
          // d245910: This can happen when other threads are also calling unsubscribe, as in the testcase
          // written for d245910. ThreadA does the create, then gets switched out, ThreadB does an
          // unsubscribe, ThreadA switches back in and finds the subscription gone, so ends up here.
          // Using the same logic as for SIDurableSubscriptionAlreadyExistsException, it makes
          // sense to reset to NOT_TRIED and start over again.
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "The durable subscription was not found after create. Resetting to NOT_TRIED");
          create_state = NOT_TRIED;
        }
      }
      catch(SINotAuthorizedException nae) {
        // No FFDC code needed
        // Not Authorized
        // d238447 FFDC Review. Not an internal error, no FFDC required.
        // NB. Must throw exception here to avoid infinite looping!
        throw (JMSException) JmsErrorUtils.newThrowable(JMSSecurityException.class,
                                                        "NOT_AUTH_CWSIA0224",
                                                        null,
                                                        nae,
                                                        null, // null probeId = no FFDC
                                                        this,
                                                        tc
                                                       );
      }
      catch(SISelectorSyntaxException nae) {
        // No FFDC code needed
        // Invalid selector
        // d238447 FFDC Review. Not an internal error, no FFDC required.
        // NB. Must throw exception here to avoid infinite looping!
        throw (JMSException) JmsErrorUtils.newThrowable(InvalidSelectorException.class,
                                                        "BAD_SELECT_CWSIA0225",
                                                        null,
                                                        nae,
                                                        null,
                                                        this,
                                                        tc
                                                       );
      }
      catch (SIDurableSubscriptionAlreadyExistsException saee) {
        // No FFDC code needed
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
          SibTr.debug(this, tc, "Subscription already exists - this may be a timing issue with multiple clients,"
                               +" since the first time we tried to connect to it, it didn't exist!"
                               +" Resetting create_state to NOT_TRIED");
        }
        // d222942 review
        // Not sure I like the logic here - we are effectively throwing an exception
        // at the application that is the result of our implementation issues. Better
        // would be to reset this client to the beginning of the connect/create/alter
        // cycle, with the probable outcome that the app' will get a 'subscription in use'
        // exception which is more palatable. There exists the possibility that this client
        // will connect to a new subscription created by a different client, but only if
        // both subscriptions are identical, in which case it doesn't matter which
        // client created it.
        // Code used to be.............
        // NB. Must throw exception here to avoid infinite looping!
        //          throw (JMSException) JmsErrorUtils.newThrowable(
        //           InvalidSelectorException.class,
        //           "EXCEPTION_RECEIVED_CWSIA0221",
        //           new Object[] {saee, "JmsDurableSubscriberImpl.createCoreConsumer (#8)"},
        //           saee,
        //           "JmsDurableSubscriberImpl.createCoreConsumer#8",
        //           this,
        //           tc
        //          );
        // d245910 As discussed above, reset to not-tried and go around again.
        create_state = NOT_TRIED;
      }
      catch (SINotPossibleInCurrentConfigurationException npcc) {
        // No FFDC code needed
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "The topicSpace is non-permanent or does not exist.");
        // d238447 FFDC Review. Not an internal error, no FFDC required.
        // NB. Must throw exception here to avoid infinite looping!
        throw (JMSException) JmsErrorUtils.newThrowable(InvalidDestinationException.class,
                                                        "BAD_TOPICSPACE_CWSIA0226",
                                                        null,
                                                        npcc,
                                                        null, // null probeId = no FFDC
                                                        this,
                                                        tc
                                                       );
      }
      catch (SIException sice) {
        // No FFDC code needed
        // Misc other exception (Store, Comms, Core).
        // d238447 FFDC review. FFDC generation seems reasonable for this case.
        // NB. Must throw exception here to avoid infinite looping!
        throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                        "EXCEPTION_RECEIVED_CWSIA0221",
                                                        new Object[] {sice, "JmsDurableSubscriberImpl.createCoreConsumer (#9)"},
                                                        sice,
                                                        "JmsDurableSubscriberImpl.createCoreConsumer#9",
                                                        this,
                                                        tc
                                                       );
      }

      // If the first createDurableSubscription call failed, then at this
      // point create_state will be REQUEST_ALTER, and we will loop round
      // again. (Presumably it may alse be NOT_TRIED).

      // If the operation succeeded for either reason (direct success or
      // success after alter) then the state will be COMPLETE.

      // Any other outcome will result in an exception being thrown so
      // we will not leave this loop in the regular fashion.
    } while (create_state != COMPLETE);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createCoreConsumer(SICoreConnection, ConsumerProperties)",  dcs);
    return dcs;
  }
}
