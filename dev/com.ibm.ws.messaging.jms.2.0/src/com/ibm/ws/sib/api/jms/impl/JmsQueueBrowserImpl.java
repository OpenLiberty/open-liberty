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

import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

import javax.jms.InvalidDestinationException;
import javax.jms.InvalidSelectorException;
import javax.jms.JMSException;
import javax.jms.JMSSecurityException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsQueue;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.api.jms.JmsInternalConstants;
import com.ibm.ws.sib.api.jms.JmsInternalsFactory;
import com.ibm.ws.sib.api.jms.service.JmsServiceFacade;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.BrowserSession;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;
import com.ibm.wsspi.sib.core.SelectorDomain;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;

/**
 * @author matrober
 * 
 *         To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
public class JmsQueueBrowserImpl implements QueueBrowser, ApiJmsConstants, JmsInternalConstants
{

    //*************************** TRACE INITIALIZATION **************************
    private static TraceComponent tc = SibTr.register(JmsQueueBrowserImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    // ********************* STATE VARIABLES **********************
    private JmsSessionImpl theSession = null;
    private JmsDestinationImpl theDestination = null;
    private String theSelector = null;
    private String jsDestName = null;
    private SICoreConnection coreConnection = null;

    /**
     * The state will either be STARTED or CLOSED (no STOPPED). All changes
     * to the state should sync on the enums object.
     */
    private int state = STARTED;

    /**
     * This variable used as an aid to the eager startup required to ensure that
     * we have access to the destination etc.
     */
    private boolean createdFirst = false;
    private Enumeration firstEnumeration = null;

    /**
     * This object stores references to the various enumerations that have been
     * created. Note that the firstEnumeration is added up front (so the vector
     * will have size 1 before _and_ after the first call to getEnumeration).
     */
    private List<EnumImpl> enums = null;

    /**
     * Factory to create objects used on calls that require a selector.
     */
    private SelectionCriteriaFactory selectionCriteriaFactory = null;

    // ********************** CONSTRUCTORS ************************

    /**
     * This constructor is called by the Session.createBrowser(Dest, String)
     * method in order to create a queue browser object. It throws a JMSException
     * if it was not possible to create the browser for any reason.
     * 
     * @param newSession JMS Session against which the browser is being created
     * @param newDest Destination for which it is being created
     * @param newSelector Selector that should be applied to it
     * @throws JMSException If something goes wrong.
     */
    public JmsQueueBrowserImpl(JmsSessionImpl newSession, JmsDestinationImpl newDest, String newSelector) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsQueueBrowserImpl", new Object[] { newSession, newDest, newSelector });

        theSession = newSession;
        theDestination = newDest;
        theSelector = newSelector;

        // We can assume that the destination is not null as that checking will have
        // been done by the createBrowser method.
        jsDestName = theDestination.getConsumerDestName();

        // Initialise the storage for the enumerations.
        enums = new Vector<EnumImpl>();

        // Get a core connection.
        coreConnection = newSession.getCoreConnection();

        // Get a selection criteria factory for later use.
        try {
            selectionCriteriaFactory = JmsServiceFacade.getSelectionCriteriaFactory();
        } catch (SIErrorException e) {
            // No FFDC code needed
            // d238447 FFDC review, ffdc ok here.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0085",
                                                            new Object[] { e, "JmsMsgConsumerImpl.constructor" },
                                                            e,
                                                            "JmsMsgConsumerImpl.constructor#1",
                                                            this,
                                                            tc
                            );
        }

        // We must now go about the business of creating the first browser session
        // in order to confirm that the destination exists etc.
        try {
            firstEnumeration = instantiateBrowser();
            createdFirst = true;
        } catch (JMSException jmse) {
            // No FFDC code needed
            // d238447 FFDC review: instantiateBrowser generates FFDCs, so don't call processTrowable here
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "instantiateBrowser generated ", jmse);
            throw jmse;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsQueueBrowserImpl");
    }

    // ******************** INTERFACE METHODS *********************

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.QueueBrowser#getQueue()
     */
    @Override
    public Queue getQueue() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getQueue");
        Queue q = (Queue) theDestination;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getQueue", q);
        return q;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.QueueBrowser#getMessageSelector()
     */
    @Override
    public String getMessageSelector() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMessageSelector");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMessageSelector", theSelector);

        //if selector is null or empty return null
        if (theSelector == null || theSelector.trim().isEmpty())
            return null;

        return theSelector;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.QueueBrowser#getEnumeration()
     */
    @Override
    public Enumeration getEnumeration() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getEnumeration");
        Enumeration vEnum = null;

        // Synchronize here so that you can't create an enumeration while the
        // browser is being closed.
        synchronized (enums) {

            // By the time we have synchronized on enums we have control of the
            // state of the object.
            if (state == CLOSED) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Called getEnumeration on a closed Browser");
                // d238447 FFDC Review. App error, no FFDC required.
                throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                "BROWSER_CLOSED_CWSIA0142",
                                                                null,
                                                                tc
                                );
            }

            if (createdFirst) {
                // If we haven't yet given the user the upfront one we created.
                if (firstEnumeration != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Using the enumeration that was created by the constructor");
                    // Pass it ready to be returned.
                    vEnum = firstEnumeration;

                    // Stop us using it again.
                    firstEnumeration = null;
                }
                else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Creating a new enumeration");
                    // Need to create a new one.
                    vEnum = instantiateBrowser();
                }
            }

            else {
                // We have not created the first one, so do it now. The call to this
                // method will have come from the constructor so we don't need to
                // return the Enumeration - just stash it away for later use.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Creating the first Enumeration (to check existence etc)");
                firstEnumeration = instantiateBrowser();
                createdFirst = true;
            }

        } // release the lock

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getEnumeration", vEnum);
        return vEnum;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.QueueBrowser#close()
     */
    @Override
    public void close() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "close");

        int originalState = 0;
        Object[] enumCopy = null;

        synchronized (enums) {
            // Save the state of the closed flag before we update it here. If
            // this is the first time that close has been called then we want
            // to carry out the logic, otherwise silently complete with no
            // further action.
            originalState = state;
            state = CLOSED;

            // Make sure that we do this part inside the sync block too in order
            // to prevent concurrent modification problems.
            enumCopy = enums.toArray();
        }

        if (originalState != CLOSED) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "About to close " + enumCopy.length + " enums.");
            for (int i = 0; i < enumCopy.length; i++) {
                EnumImpl anEnum = (EnumImpl) enumCopy[i];
                anEnum.close();
            }

            // Remove from the list maintained in the session.
            theSession.removeBrowser(this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "close");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {

        // The state of the queue browser is represented by the queue and
        // selector information.
        if (this == obj)
            return true;
        if (!(obj instanceof JmsQueueBrowserImpl)) {
            return false;
        }
        JmsQueueBrowserImpl qbi = (JmsQueueBrowserImpl) obj;
        if (!(this.theDestination.equals(qbi.theDestination))) {
            return false;
        }
        if ((this.theSelector == null) && (qbi.theSelector == null)) {
            return true;
        }
        if ((this.theSelector != null) && (this.theSelector.equals(qbi.theSelector))) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "hashCode");
        int val = 0;
        if (theDestination != null)
            val += theDestination.hashCode();
        if (theSelector != null)
            val += 11 * theSelector.hashCode();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "hashCode", val);
        return val;
    }

    // ********************** IMPLEMENTATION METHODS ********************

    /**
     * This method is responsible for instantiating the browser, and adding it
     * to the list of browsers that we current have associated with this JMS object.
     */
    private Enumeration instantiateBrowser() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "instantiateBrowser");
        EnumImpl vEnum = null;

        try {
            DestinationType dt = DestinationType.QUEUE;
            String discrim = null;
            String mediation = null;
            boolean gather = false;

            // Establish the correct gatherMessages setting.
            String gatherStr = ((JmsQueue) theDestination).getGatherMessages();
            if (ApiJmsConstants.GATHER_MESSAGES_ON.equals(gatherStr))
                gather = true;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, " destName: " + jsDestName + ", destType: " + dt + ", discrim: " + discrim + ", mediation: " + mediation + ", gather: " + gather);

            // Need to create a selection criteria object for create browser call.
            SelectionCriteria selectionCriteria = null;
            try {
                selectionCriteria = selectionCriteriaFactory.createSelectionCriteria(discrim, // discriminator (topic)
                                                                                     theSelector, // selector string
                                                                                     SelectorDomain.JMS // selector domain
                );
            } catch (SIErrorException sice) {
                // No FFDC code needed
                // d238447 FFDC review. FFDC here is good.
                throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                "EXCEPTION_RECEIVED_CWSIA0144",
                                                                new Object[] { sice, "JmsQueueBrowserImpl.instantiateBrowser" },
                                                                sice,
                                                                "JmsQueueBrowserImpl.instantiateBrowser#1",
                                                                this,
                                                                tc
                                );
            }

            // Core API call to create the browser.
            BrowserSession bs = coreConnection.createBrowserSession(theDestination.getConsumerSIDestinationAddress(),
                                                                    dt,
                                                                    selectionCriteria,
                                                                    null, // alternateUserID
                                                                    gather // gatherMessages
            );

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "core BrowserSession created: " + bs);
            if (bs != null) {
                // Create an enumeration object to drive the bs.
                vEnum = new EnumImpl(bs);
                // Add the enum to the internal list.
                enums.add(vEnum);
            }
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "createBrowserSession returned null - internal error for coreAPI");
                // d238447 FFDC review. This was using the wrong version of newThrowable, presumably with
                // the intent of generating an FFDC, but the caughtThrowable param has to be non-null.
                // Changed to use the correct newThrowable method.
                throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                                "FAILED_TO_CREATE_BSESSION_CWSIA0143",
                                                                null,
                                                                tc
                                );
            }

        } catch (SISelectorSyntaxException e) {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "An invalid selector was supplied to createBrowser", e);
            // d238447 modify to not throw FFDC
            throw (InvalidSelectorException) JmsErrorUtils.newThrowable(InvalidSelectorException.class,
                                                                        "INVALID_SELECTOR_CWSIA0147",
                                                                        null,
                                                                        e,
                                                                        null, // null probeId = no ffdc
                                                                        this,
                                                                        tc
                            );
        } catch (SINotAuthorizedException sinae) {
            // No FFDC code needed
            // d238447: app or config error, no ffdc required.
            throw (JMSSecurityException) JmsErrorUtils.newThrowable(JMSSecurityException.class,
                                                                    "BROWSER_AUTH_ERROR_CWSIA0149",
                                                                    null,
                                                                    sinae,
                                                                    null, // null probeId = no ffdc
                                                                    this,
                                                                    tc
                            );
        } catch (SINotPossibleInCurrentConfigurationException dwte) {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "failed to create browserSession", dwte);
            // We asked to connect to a queue and got a topic
            // d238447: app or config error, no ffdc required.
            throw (InvalidDestinationException) JmsErrorUtils.newThrowable(InvalidDestinationException.class,
                                                                           "BROWSE_FAILED_CWSIA0145",
                                                                           new Object[] { jsDestName },
                                                                           dwte,
                                                                           null, // null probeId = no ffdc
                                                                           this,
                                                                           tc
                            );
        } catch (SITemporaryDestinationNotFoundException tdnf) {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "failed to create browserSession", tdnf);
            // We asked to connect to a queue and got a topic
            // d238447: app or config error, no ffdc required.
            throw (InvalidDestinationException) JmsErrorUtils.newThrowable(InvalidDestinationException.class,
                                                                           "BROWSE_FAILED_CWSIA0145",
                                                                           new Object[] { jsDestName },
                                                                           tdnf,
                                                                           null, // no ffdc required
                                                                           this,
                                                                           tc
                            );
        } catch (SIException e) {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Exception creating browser session", e);
            // d222942 review - default message ok for 'everything else' use
            // d238447 FFDC review - generate FFDC for this case
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0144",
                                                            new Object[] { e, "createBrowserSession" },
                                                            e,
                                                            "JmsQueueBrowserImpl.instantiateBrowser#7",
                                                            this,
                                                            tc
                            );
        } catch (Exception e) {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Exception creating browser session", e);
            // d222942 review - default message ok for 'everything else' use
            // d238447 FFDC review - generate FFDC for this case
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class,
                                                            "EXCEPTION_RECEIVED_CWSIA0144",
                                                            new Object[] { e, "createBrowserSession" },
                                                            e,
                                                            "JmsQueueBrowserImpl.instantiateBrowser#8",
                                                            this,
                                                            tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "instantiateBrowser", vEnum);
        return vEnum;
    }

    /**
     * @author matrober
     * 
     *         This class is the object which provides the enumeration function
     *         that the user draws from the QueueBrowser.
     * 
     */
    class EnumImpl implements Enumeration, ApiJmsConstants {

        // ******** INTERNAL STATE **********
        private BrowserSession bs = null;

        /**
         * State will either be STARTED, CLOSED (not stopped).
         * Note that there is also a DISCONNECTED state as described below.
         */
        private int state = STARTED;

        /**
         * This state will be entered when the browser gets a null from the
         * core browser session, so that no further interaction with the server
         * takes place. It is a stepping stone to the CLOSED state, which must
         * be initiated by the user.
         * Pick a large large number to prevent interference with the other states!
         */
        private static final int DISCONNECTED = 171079;

        /**
         * This object is the message that has been retrieved as a result
         * of a call to hasMoreElements, but not yet delivered to the user
         * as part of a nextElement call.
         */
        private SIBusMessage nextMsg = null;

        // ********** CONSTRUCTOR ***********
        EnumImpl(BrowserSession newBs) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "EnumImpl", newBs);
            // Note that due to the error checking in instantiateBrowser we
            // know that newBs is not null (we use this for sync-ing later.
            bs = newBs;
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "EnumImpl");
        }

        // ***** INTERFACE METHODS **********
        /*
         * (non-Javadoc)
         * 
         * @see java.util.Enumeration#hasMoreElements()
         */
        @Override
        public boolean hasMoreElements() {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "hasMoreElements");
            boolean ret = false;

            synchronized (bs) {

                // If we have already closed this enumeration then it will
                // not be returning any more elements.
                if ((state == CLOSED) || (state == DISCONNECTED)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Enumeration in disconnected or closed state: " + state + " so no more elements are available");
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(this, tc, "hasMoreElements", false);
                    return false;
                }

                // If we have not already got a message ready for delivery.
                if (nextMsg == null) {
                    try {
                        // Try to get the next message.
                        nextMsg = bs.next();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Received message: " + nextMsg);
                    } catch (SIException e) {
                        // No FFDC code needed
                        // We report the exception in trace, then just act as if we never received the message.
                        FFDCFilter.processException(e, "com.ibm.ws.sib.api.jms.impl.JmsQueueBrowserImpl.EnumImpl", "hasMoreElements#1", this);
                    }

                    // If we have received a new message.
                    if (nextMsg == null) {
                        // There are no more messages left.
                        state = DISCONNECTED;
                        ret = false;
                    }
                    else {
                        // A message was received.
                        ret = true;
                    }
                }

                else {
                    // We have already retrieved a message to pass to the user.
                    ret = true;
                }

            } // release the lock

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "hasMoreElements", ret);
            return ret;
        }

        /**
         * @see java.util.Enumeration#nextElement()
         * @throws IllegalStateException if the enumeration is closed.
         * @throws MessageConversionFailed if there is a problem converting the next message to a JMS message.
         */
        @Override
        public Object nextElement() {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "nextElement");
            Message jmsMsg = null;

            synchronized (bs) {

                if (state == CLOSED) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Called nextElement on a closed Browser Enumeration");
                    // d238447 FFDC review. App error, no FFDC required.
                    throw (java.lang.IllegalStateException) JmsErrorUtils.newThrowable(java.lang.IllegalStateException.class,
                                                                                       "BROWSER_CLOSED_CWSIA0142",
                                                                                       null,
                                                                                       tc
                                    );
                }

                boolean msgAvailable = false;

                // If we haven't already obtained a message reference (ie the
                // user is driving the Enumeration using solely the nextElement
                // method.
                if (nextMsg == null) {
                    // Call the hasMore in order to obtain the next
                    // message reference.
                    msgAvailable = hasMoreElements();
                }
                else {
                    // There is a message to look at.
                    msgAvailable = true;
                }

                // There is a message to play with
                if (msgAvailable) {
                    try {
                        // Convert it from jsMessage to JMS message. Note that by passing null as the 2nd parameter here
                        // we indicate the calling msg.acknowledge should throw an IllegalStateException. The third parameter
                        // associates the 'pass thru properties' of the session with the new message (SIB0121)
                        jmsMsg = JmsInternalsFactory.getSharedUtils().inboundMessagePath(nextMsg, null, theSession.getPassThruProps());
                    } catch (JMSException jmse) {
                        // No FFDC code needed
                        // We can't throw a checked exception from this method because the signature
                        // has no exceptions defined. It is expected that this will be quite a rare
                        // case, and so in order to inform the calling application we throw an
                        // exception which is a subclass of RuntimeException, and has the cause
                        // set to the offending exception in question.

                        // We don't want to get stuck on the same message over and over again.
                        nextMsg = null;

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Message could not be converted to a JMS message type", jmse);

                        // d238447 FFDC review. Generate FFDC.
                        throw (MessageConversionFailed) JmsErrorUtils.newThrowable(MessageConversionFailed.class,
                                                                                   "INBOUND_MSG_ERROR_CWSIA0148",
                                                                                   null,
                                                                                   jmse,
                                                                                   "JmsQueueBrowserImpl.Enum.nextElement#2",
                                                                                   this,
                                                                                   tc
                                        );
                    }

                    // We don't want to end up using the same message
                    // again and again and again.
                    nextMsg = null;
                }

            } // release the lock

            // If there is no object to return.
            if (jmsMsg == null) {
                // d238447 FFDC review. Normal path/app error. No FFDC required.
                throw (NoSuchElementException) JmsErrorUtils.newThrowable(NoSuchElementException.class,
                                                                          "NO_MESSAGE_AVAILABLE_CWSIA0141",
                                                                          null,
                                                                          tc
                                );
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "nextElement", jmsMsg);
            return jmsMsg;
        }

        // **** IMPLEMENTATION METHODS *****

        public void close() {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "close");

            synchronized (bs) {
                try {
                    bs.close();
                } catch (SIException e) {
                    // No FFDC code needed
                    FFDCFilter.processException(e, "com.ibm.ws.sib.api.jms.impl.JmsQueueBrowserImpl.EnumImpl", "close#1", this);
                }
                state = CLOSED;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "close");
        }
    }
}

class MessageConversionFailed extends RuntimeException {
    private static final long serialVersionUID = 9041830265178620927L;

    /**
     * @param message
     * @param cause
     */
    public MessageConversionFailed(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause
     */
    public MessageConversionFailed(Throwable cause) {
        super(cause);
    }
}
