/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.processor.impl;

// Import required classes.
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;

/**************************************************************
 * ConsumerDispatcherState class
 * 
 * <p> This class contains all information associated with a subscription
 * or a filter on a pt-to-pt consumerDispatcher. The information is stored in this
 * single object and passed around. This is also the object that is persisted
 * as part of a durable subscription. There is enough info in this object to
 * reconstruct a subscription.
 * 
 * <p> A ConsumerDispatcherState state is also created for pt-to-pt to store the
 * selector and discriminator information.
 */

public final class ConsumerDispatcherState
{

    private static final int SUBSTATE_PRIORITY = 0;
    private static final TraceComponent tc =
                    SibTr.register(
                                   ConsumerDispatcherState.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    /* The state data */
    private String subscriptionID = null;
    private SIBUuid12 topicSpaceUuid = null;
    private String topicSpaceName = null;
    private String topicSpaceBusName = null;
    private String topics[] = null;
    // private SelectionCriteria criteria = null;
    private String user = null;
    // Flag that the associated user is the privileged SIBServerSubject
    private boolean isSIBServerSubject = false;
    private String durableHome = null;
    private String targetDest = null;

    // userData for WS Notifications
    private Map userData = null;
    private SelectionCriteria selectionCriteriaList[] = null;

    /**
     * ready variable is used to indicate if the consumer is ready to receive
     * messages. The only case that this may be set to false is when creating
     * a DurableSubscription as the subscription is added to the MatchSpace
     * before it is committed. When the Durable Subscription is committed, this
     * state is set to true.
     */
    private boolean ready = true;
    private boolean noLocal = false;
    private boolean isCloned = false;

    /** the UUID of the ME that homes a remote durable subscription */
    private SIBUuid8 remoteMEUuid = null;

    // F001333-14610
    // New flag to track durability of subscriptions instead
    // of relying on the lack of a subscription ID.
    private boolean durable = false;

    /**
     * Blank constructor. Required on any object that implements Item so that
     * it can be instatiated by the messagestore on recovery.
     */

    public ConsumerDispatcherState()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "ConsumerDispatcherState");
            SibTr.exit(this, tc, "ConsumerDispatcherState");
        }
    }

    /**
     * PubSubOutputHandler constructor created for use with multiple topics for
     * a particular topic space destination on a remote ME
     * 
     * @param topicSpaceUuid The name of the topicspace destination
     * @param topic The name of the initial topic
     * @param topicSpaceName
     * @param topicSpaceBusName
     */
    public ConsumerDispatcherState(SIBUuid12 topicSpace, SelectionCriteria criteria, String topicSpaceName, String topicSpaceBusName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this,
                        tc,
                        "ConsumerDispatcherState",
                        new Object[] { topicSpace, criteria, topicSpaceName, topicSpaceBusName });

        this.topicSpaceUuid = topicSpace;
        this.topicSpaceName = topicSpaceName;
        this.topicSpaceBusName = topicSpaceBusName;
        this.topics = new String[] { (criteria == null) ? null : criteria.getDiscriminator() };
        this.selectionCriteriaList = new SelectionCriteria[] { (criteria == null) ? null : criteria };

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "ConsumerDispatcherState", this);
    }

    /**
     * PubSub Constructor. Typically created during a pubsub subscription create.
     * Takes the minimum amount of parameters needed. These can be changed via the
     * getter and setter methods.
     * 
     * @param subscriptionID
     * @param topicSpaceUuid The destination
     * @param topic The topic name
     * @param selector
     * @param isCloned
     * @param noLocal flag
     * @param topicSpaceName
     * @param topicSpaceBusName
     */

    public ConsumerDispatcherState(
                                   String subscriptionID,
                                   SIBUuid12 topicSpace,
                                   SelectionCriteria criteria,
                                   boolean noLocal,
                                   String durHome,
                                   String topicSpaceName,
                                   String topicSpaceBusName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "ConsumerDispatcherState",
                        new Object[] { subscriptionID, topicSpace, criteria, Boolean.valueOf(noLocal), durHome, topicSpaceName, topicSpaceBusName });

        this.subscriptionID = subscriptionID;
        this.noLocal = noLocal;
        this.topicSpaceUuid = topicSpace;
        this.topicSpaceName = topicSpaceName;
        this.topicSpaceBusName = topicSpaceBusName;
        this.durableHome = durHome;
        this.topics = new String[] { (criteria == null) ? null : criteria.getDiscriminator() };
        this.selectionCriteriaList = new SelectionCriteria[] { (criteria == null) ? null : criteria };

        //JMS 2 introduced non-durable shared subscriptions which would come with subscriptionID
        //hence now checking both subscriptionId and durableHome to decide whether the subscription id durable or not.
        if ((durableHome != null) && (subscriptionID != null))
        {
            durable = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "ConsumerDispatcherState", this);
    }

    /**
     * PubSub Constructor. Typically created during a pubsub subscription create.
     * Takes the minimum amount of parameters needed. These can be changed via the
     * getter and setter methods.
     * 
     * @param subscriptionID
     * @param topicSpaceUuid The destination
     * @param topic The topic name
     * @param selector
     * @param isCloned
     * @param noLocal flag
     * @param topicSpaceName
     * @param topicSpaceBusName
     */

    public ConsumerDispatcherState(
                                   String subscriptionID,
                                   SIBUuid12 topicSpace,
                                   boolean noLocal,
                                   String durHome,
                                   String topicSpaceName,
                                   String topicSpaceBusName,
                                   SelectionCriteria[] selectionCriteriaList,
                                   HashMap userData)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "ConsumerDispatcherState",
                        new Object[] { subscriptionID, topicSpace, Boolean.valueOf(noLocal), durHome, topicSpaceName, topicSpaceBusName, selectionCriteriaList, userData });

        this.subscriptionID = subscriptionID;
        this.noLocal = noLocal;
        this.topicSpaceUuid = topicSpace;
        this.topicSpaceName = topicSpaceName;
        this.topicSpaceBusName = topicSpaceBusName;
        this.durableHome = durHome;
        if (selectionCriteriaList != null)
        {
            this.topics = new String[selectionCriteriaList.length];
            for (int i = 0; i < selectionCriteriaList.length; i++)
            {
                topics[i] = (selectionCriteriaList[i] == null) ? null : selectionCriteriaList[i].getDiscriminator();
            }
            this.selectionCriteriaList = selectionCriteriaList;
        }

        //JMS 2 introduced non-durable shared subscriptions which would come with subscriptionID
        //hence now checking both subscriptionId and durableHome to decide whether the subscription id durable or not.
        // are set post construction.
        if ((durableHome != null) && (subscriptionID != null))
        {
            durable = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "ConsumerDispatcherState", this);
    }

    /**
     * Returns the topic space name.
     * 
     * @return String
     */
    public SIBUuid12 getTopicSpaceUuid()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getTopicSpaceUuid");
            SibTr.exit(this, tc, "getTopicSpaceUuid", topicSpaceUuid);
        }
        return topicSpaceUuid;
    }

    /**
     * Returns the selector.
     * 
     * @return String
     */
    public SelectionCriteria getSelectionCriteria()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getSelectionCriteria");
        }

        SelectionCriteria criteria = null;
        if (this.selectionCriteriaList != null)
        {
            criteria = this.selectionCriteriaList[0];
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.exit(this, tc, "getSelectionCriteria", criteria);
        }

        return criteria;
    }

    /**
     * Returns the subscriberID.
     * 
     * @return String
     */
    public String getSubscriberID()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getSubscriberID");
            SibTr.exit(this, tc, "getSubscriberID", subscriptionID);
        }

        return subscriptionID;
    }

    /**
     * Returns the array of topics.
     * 
     * @return String[] The array of topics
     */
    public String[] getTopics()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getTopics");
            SibTr.exit(this, tc, "getTopics", topics);
        }
        return topics;
    }

    /**
     * Remove a topic from the array of topics
     * 
     * @param topic The topic to remove
     */
    public void removeTopic(String topic)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeTopic", topic);

        SelectionCriteria[] tmp = selectionCriteriaList;

        // Loop through the selectionCriteriaList
        for (int i = 0; i < selectionCriteriaList.length; ++i)
        {
            if ((selectionCriteriaList[i].getDiscriminator() == null && topic == null) ||
                (topic != null && selectionCriteriaList[i].getDiscriminator().equals(topic)))
            {
                // If there was only one criteria and we have removed it then
                // nullify lists
                if (selectionCriteriaList.length == 1)
                {
                    selectionCriteriaList = null;
                    topics = null;
                }
                else
                {
                    // The criteria match, so resize the array without this
                    // criteria in it.
                    tmp = new SelectionCriteria[selectionCriteriaList.length - 1];
                    System.arraycopy(selectionCriteriaList, 0, tmp, 0, i);
                    System.arraycopy(selectionCriteriaList, i + 1, tmp, i, selectionCriteriaList.length - i - 1);

                    selectionCriteriaList = tmp;

                    // And copy into the topics array so that they always match
                    this.topics = new String[selectionCriteriaList.length];
                    for (int t = 0; t < selectionCriteriaList.length; t++)
                    {
                        topics[t] = (selectionCriteriaList[t] == null) ? null : selectionCriteriaList[t].getDiscriminator();
                    }
                }

                break;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeTopic");
    }

    /**
     * Returns the topic.
     * 
     * @return String
     */
    public String getTopic()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getTopic");
        if (topics == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getTopic", null);
            return null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getTopic");
        return topics[0];
    }

    /**
     * Returns the user.
     * 
     * @return String
     */
    public String getUser()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getUser");
            SibTr.exit(this, tc, "getUser", user);
        }
        return user;
    }

    /**
     * Sets the topic space.
     * 
     * @param topicSpace The topicSpaceUuid to set
     */
    public void setTopicSpaceUuid(SIBUuid12 topicSpace)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTopicSpaceUuid", topicSpace);

        this.topicSpaceUuid = topicSpace;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTopicSpaceUuid");
    }

    /**
     * Sets the subscriberID.
     * 
     * @param subscriptionID The subscriberID to set
     */
    public void setSubscriberID(String subscriptionID)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setSubscriberID", subscriptionID);

        this.subscriptionID = subscriptionID;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setSubscriberID");
    }

    /**
     * Sets the user.
     * 
     * @param user The user to set
     * @param isSIBServerSubject
     */
    public void setUser(String user, boolean isSIBServerSubject)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setUser", user);

        this.user = user;
        this.isSIBServerSubject = isSIBServerSubject;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setUser");
    }

    /**
     * Returns the isSIBServerSubject.
     * 
     * @return boolean
     */
    public boolean isSIBServerSubject()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "isSIBServerSubject");
            SibTr.exit(this, tc, "isSIBServerSubject", Boolean.valueOf(isSIBServerSubject));
        }
        return isSIBServerSubject;
    }

    /**
     * Is this a durable subscription state
     * 
     * @return isDurable
     */
    protected boolean isDurable()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "isDurable");
            SibTr.exit(this, tc, "isDurable", Boolean.valueOf(durable));
        }
        return durable; // F001333-14610
    }

    // F001333-14610
    // Setter to allow durable flag to be re-instated at restore
    // time for a durable subscription.
    public void setDurable(boolean isDurable)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDurable", Boolean.valueOf(isDurable));

        durable = isDurable;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDurable");
    }

    /**
     * Returns whether this object has the same values as the given object
     * 
     * @param subState The subscription state to check equal to.
     * @returns boolean true if the objects match.
     */

    @Override
    public boolean equals(Object obj)
    {

        if (obj == null)
            return false;
        if (obj == this)
            return true;

        if (obj instanceof ConsumerDispatcherState)
        {
            ConsumerDispatcherState subState = (ConsumerDispatcherState) obj;

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "equals");

            boolean equal = true;

            /*
             * Check all parameters are equal
             * Note that this method will only be called when a durable
             * subscription is created which already exists. We therefore
             * choose readable code over a (potentially more efficient) large
             * if statement.
             * 
             * /* Check subscriptionID
             */

            if (subscriptionID == null)
            {
                if (subState.getSubscriberID() != null)
                    equal = false;
            }
            else if (!subscriptionID.equals(subState.getSubscriberID()))
                equal = false;

            /* Check destination */
            if (topicSpaceUuid == null)
            {
                if (subState.getTopicSpaceUuid() != null)
                    equal = false;
            }
            else if (!topicSpaceUuid.equals(subState.getTopicSpaceUuid()))
                equal = false;

            /* Check selectionCriteria list */
            // Note that we do not need to check topics as these are
            // always derived from selectionCriteria
            SelectionCriteria[] criteriaToCheck = subState.getSelectionCriteriaList();
            if (selectionCriteriaList == null)
            {
                if (criteriaToCheck != null)
                    equal = false;
            }
            else
            {
                if (criteriaToCheck == null)
                    equal = false;
                else
                {
                    for (int i = 0; i < selectionCriteriaList.length; i++)
                    {
                        // Check for null selectionCriteria
                        if (selectionCriteriaList[i] == null)
                        {
                            if (criteriaToCheck[i] != null)
                                equal = false;
                        }
                        else
                        {
                            if (criteriaToCheck[i] == null)
                                equal = false;
                            else if (!selectionCriteriaList[i].equals(criteriaToCheck[i]))
                                equal = false;
                        }
                    }
                }
            }

            /* Check noLocal */

            if (noLocal != subState.isNoLocal())
                equal = false;

            /* Check isCloned */

            if (isCloned != subState.isCloned())
                equal = false;

            /* Check durableHome */
            equal = equal &&
                    ((durableHome == subState.durableHome) ||
                    ((durableHome != null) && (durableHome.equals(subState.durableHome))));

            if (!equal)
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Subscriptions not equal " + toString() + " & " +
                                    subState.toString());

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "equals", Boolean.valueOf(equal));

            return equal;
        }
        else
        {
            return false; // obj is not a ConsumerDispatcherState
        }
    }

    @Override
    public int hashCode()
    {
        if (subscriptionID == null)
            return 0;
        return subscriptionID.hashCode();
    }

    /**
     * Returns whether this object has the same values as the given object
     * 
     * @param subState The subscription state to check equal to.
     * @returns boolean true if the objects match.
     */

    public boolean equalUser(ConsumerDispatcherState subState)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "equalUser");

        /* Check user */
        boolean equal = equalUser(subState.getUser(), subState.isSIBServerSubject());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "equalUser", Boolean.valueOf(equal));

        return equal;

    }

    public boolean equalUser(String otherUser, boolean isOtherSIBServerSubject)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "equalUser",
                        new Object[] { otherUser, Boolean.valueOf(isOtherSIBServerSubject) });

        boolean equal = true;

        /* Check user */

        if (user == null || user.equals(""))
        {
            // def 263349: Allow the equalUser comparison to return true in this case as the
            // dur sub must have been established where security was switched off.
            //
            // Additionally set the passed user into the CD state.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Old user was null or empty, dur sub established with security switched off");

            // Set the new user into this CD state
            setUser(otherUser, isOtherSIBServerSubject);
        }
        else
        {
            if (!user.equals(otherUser))
                equal = false;
            else // Names are equal, do final SIBServerSubject test
            {
                if (isSIBServerSubject())
                {
                    if (!isOtherSIBServerSubject) // other is not SIBServerSubject
                        equal = false;
                }
                else
                {
                    if (isOtherSIBServerSubject) // but other is SIBServerSubject
                        equal = false;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "equalUser", Boolean.valueOf(equal));

        return equal;

    }

    /**
     * Returns the ready flag.
     * 
     * @return boolean
     */
    public boolean isReady()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "isReady");
            SibTr.exit(this, tc, "isReady", Boolean.valueOf(ready));
        }

        return ready;
    }

    /**
     * Sets the ready flag.
     * 
     * @param ready The ready to set
     */
    public void setReady(boolean ready)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReady", Boolean.valueOf(ready));

        this.ready = ready;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReady");
    }

    /**
     * Returns the noLocal flag.
     * 
     * @return boolean
     */
    public boolean isNoLocal()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "isNoLocal");
            SibTr.exit(this, tc, "isNoLocal", Boolean.valueOf(noLocal));
        }

        return noLocal;
    }

    /**
     * Sets the noLocal flag.
     * 
     * @param noLocal The noLocal to set
     */
    public void setNoLocal(boolean noLocal)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setNoLocal", Boolean.valueOf(noLocal));

        this.noLocal = noLocal;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setNoLocal");
    }

    /**
     * toString method
     * 
     * @return String representation of the subscription state
     */
    @Override
    public String toString()
    {
        String filter = null;

        if (selectionCriteriaList != null)
        {
            filter = selectionCriteriaList.toString();
        }

        if (topics != null && topics.length > 0)
            return "Subscription to "
                   + subscriptionID
                   + ":"
                   + topicSpaceBusName
                   + ":"
                   + topicSpaceName
                   + ":"
                   + topicSpaceUuid
                   + ":"
                   + topics[0]
                   + ":"
                   + filter
                   + ":"
                   + user
                   + ":"
                   + durableHome;

        return "Subscription to "
               + subscriptionID
               + ":"
               + topicSpaceBusName
               + ":"
               + topicSpaceName
               + ":"
               + topicSpaceUuid
               + ":"
               + Arrays.toString(topics) /* "[]" or "null" */
               + ":"
               + filter
               + ":"
               + user
               + ":"
               + durableHome;
    }

    protected int getPriority()
    {

        return SUBSTATE_PRIORITY;
    }

    /**
     * Set durableHome.
     * 
     * @param String
     */
    public void setDurableHome(String val)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDurableHome", val);
        durableHome = val;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDurableHome");
    }

    /**
     * Returns the isCloned.
     * 
     * @return boolean
     */
    public boolean isCloned()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "isCloned");
            SibTr.exit(this, tc, "isCloned", Boolean.valueOf(isCloned));
        }
        return isCloned;
    }

    /**
     * Sets the isCloned.
     * 
     * @param isCloned The isCloned to set
     */
    public void setIsCloned(boolean isCloned)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setIsCloned", Boolean.valueOf(isCloned));
        this.isCloned = isCloned;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setIsCloned");
    }

    /**
     * Get durableHome.
     * 
     * @return String
     */
    public String getDurableHome()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getDurableHome");
            SibTr.exit(this, tc, "getDurableHome", durableHome);
        }
        return durableHome;
    }

    /**
     * Gets the name of the topicspace that the subscription was made through
     * 
     * @return
     */
    public String getTopicSpaceName()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getTopicSpaceName");
            SibTr.exit(this, tc, "getTopicSpaceName", topicSpaceName);
        }
        return topicSpaceName;
    }

    /**
     * Sets the name of the topicspace that the subscription was made through
     * 
     * @param string
     */
    public void setTopicSpaceName(String name)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTopicSpaceName", name);

        topicSpaceName = name;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTopicSpaceName");
    }

    /**
     * Gets the busname of the topicspace that the subscription was made through
     * 
     * @return
     */
    public String getTopicSpaceBusName()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getTopicSpaceBusName");
            SibTr.exit(this, tc, "getTopicSpaceBusName", topicSpaceBusName);
        }
        return topicSpaceBusName;
    }

    /**
     * Sets the busname of the topicspace that the subscription was made through
     * 
     * @param string
     */
    public void setTopicSpaceBusName(String busname)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTopicSpaceBusName", busname);

        topicSpaceBusName = busname;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTopicSpaceBusName");
    }

    /**
     * @return Returns the userData.
     */
    public Map getUserData()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getUserData");
            SibTr.exit(this, tc, "getUserData", userData);
        }

        return userData;
    }

    /**
     * @param userData The userData to set.
     */
    public void setUserData(Map userData)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setUserData", userData);

        this.userData = userData;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setUserData");
    }

    /**
     * @return
     */
    public String getTargetDestination()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getTargetDestination");
            SibTr.exit(this, tc, "getTargetDestination", targetDest);
        }
        return targetDest;
    }

    /**
     * @param string
     */
    public void setTargetDestination(String targetDest)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTargetDestination", targetDest);

        this.targetDest = targetDest;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTargetDestination");
    }

    /**
     * @return
     */
    public SelectionCriteria[] getSelectionCriteriaList()
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getSelectionCriteriaList");
            SibTr.exit(this, tc, "getSelectionCriteriaList", selectionCriteriaList);
        }

        return selectionCriteriaList;
    }

    /**
     * Add an additional selection criteria to the original one (supplied at subscription creation time) to the subscription
     * Duplicate selection criterias are ignored
     **/
    public boolean addSelectionCriteria(SelectionCriteria selCriteria)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "addSelectionCriteria", selCriteria);

        boolean duplicateCriteria = false;

        if (this.selectionCriteriaList == null)
        {
            // If we did not have any on the original creation then
            // create them now.
            this.topics = new String[] { (selCriteria == null) ? null : selCriteria.getDiscriminator() };
            this.selectionCriteriaList = new SelectionCriteria[] { selCriteria };
        }
        else
        {
            // Check that this is not a duplicate of one we already have
            // Loop through the selectionCriteriaList
            for (int i = 0; i < selectionCriteriaList.length; ++i)
            {

                if ((selectionCriteriaList[i] == null && selCriteria == null)
                    || (selectionCriteriaList[i].equals(selCriteria)))
                {
                    duplicateCriteria = true;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Duplicate selectionCriteria exists");
                    break;
                }
            }

            if (!duplicateCriteria)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Adding selection criteria");
                // Dynamically increases the selectionCriteriaList array size.
                final SelectionCriteria[] tmp = new SelectionCriteria[selectionCriteriaList.length + 1];
                System.arraycopy(selectionCriteriaList, 0, tmp, 0, selectionCriteriaList.length);
                tmp[selectionCriteriaList.length] = selCriteria;
                selectionCriteriaList = tmp;

                // Dynamically increates the topic array size.
                final String[] tmptopics = new String[topics.length + 1];
                System.arraycopy(topics, 0, tmptopics, 0, topics.length);
                tmptopics[topics.length] = (selCriteria == null) ? null : selCriteria.getDiscriminator();
                topics = tmptopics;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "addSelectionCriteria", Boolean.valueOf(duplicateCriteria));

        return duplicateCriteria;
    }

    /**
     * Remove a selection criteria from the subscription
     **/
    public boolean removeSelectionCriteria(SelectionCriteria selCriteria)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeSelectionCriteria", selCriteria);

        SelectionCriteria[] tmp = selectionCriteriaList;

        boolean removedCriteria = false;

        // Loop through the selectionCriteriaList
        for (int i = 0; i < selectionCriteriaList.length; ++i)
        {
            if (selectionCriteriaList[i].equals(selCriteria))
            {
                // If there was only one criteria and we have removed it then
                // nullify lists
                if (selectionCriteriaList.length == 1)
                {
                    selectionCriteriaList = null;
                    topics = null;
                }
                else
                {
                    // The criteria match, so resize the array without this
                    // criteria in it.
                    tmp = new SelectionCriteria[selectionCriteriaList.length - 1];
                    System.arraycopy(selectionCriteriaList, 0, tmp, 0, i);
                    System.arraycopy(selectionCriteriaList, i + 1, tmp, i, selectionCriteriaList.length - i - 1);

                    selectionCriteriaList = tmp;

                    // And copy into the topics array so that they always match
                    this.topics = new String[selectionCriteriaList.length];
                    for (int t = 0; t < selectionCriteriaList.length; t++)
                    {
                        topics[t] = (selectionCriteriaList[t] == null) ? null : selectionCriteriaList[t].getDiscriminator();
                    }
                }

                removedCriteria = true;

                break;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeSelectionCriteria", Boolean.valueOf(removedCriteria));
        return removedCriteria;
    }

    /**
     * @param selectionCriteriaList
     * 
     *            This method is only called when the ConsumerDispatcher is being initialised
     *            after a restore and hence there is no need to persist the data
     * 
     */
    public void setSelectionCriteriaListOnRestore(SelectionCriteria[] selectionCriteriaList)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setSelectionCriteriaListOnRestore", selectionCriteriaList);

        if (selectionCriteriaList != null)
        {
            this.topics = new String[selectionCriteriaList.length];
            for (int i = 0; i < selectionCriteriaList.length; i++)
            {
                topics[i] = (selectionCriteriaList[i] == null) ? null : selectionCriteriaList[i].getDiscriminator();
            }
            this.selectionCriteriaList = selectionCriteriaList;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setSelectionCriteriaListOnRestore");
    }

    /**
     * Retrieve the UUID of the ME that homes a remote durable subscription
     * 
     * @return
     */
    public SIBUuid8 getRemoteMEUuid()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getRemoteMEUuid");
            SibTr.exit(this, tc, "getRemoteMEUuid", remoteMEUuid);
        }
        return remoteMEUuid;
    }

    /**
     * Set the UUID of the ME that homes a remote durable subscription
     * 
     * @param remoteMEUuid
     */
    public void setRemoteMEUuid(SIBUuid8 remoteMEUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setRemoteMEUuid", remoteMEUuid);
        this.remoteMEUuid = remoteMEUuid;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setRemoteMEUuid");
    }

}
