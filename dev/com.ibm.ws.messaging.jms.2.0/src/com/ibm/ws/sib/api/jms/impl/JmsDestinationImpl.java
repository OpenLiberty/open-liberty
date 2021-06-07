/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.api.jms.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.NamingException;
import javax.naming.Reference;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsDestination;
import com.ibm.websphere.sib.api.jms.JmsFactoryFactory;
import com.ibm.websphere.sib.api.jms.JmsQueue;
import com.ibm.websphere.sib.api.jms.JmsTopic;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.api.jms.EncodingLevel;
import com.ibm.ws.sib.api.jms.JmsDestInternals;
import com.ibm.ws.sib.api.jms.JmsInternalConstants;
import com.ibm.ws.sib.api.jms.JmsInternalsFactory;
import com.ibm.ws.sib.api.jms.StringArrayWrapper;
import com.ibm.ws.sib.api.jms._FRPHelper;
import com.ibm.ws.sib.api.jmsra.JmsJcaReferenceUtils;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationConfiguration;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.SICoreConnection;

public class JmsDestinationImpl implements JmsDestination, ApiJmsConstants, JmsInternalConstants, _FRPHelper, JmsDestInternals
{
    private static TraceComponent tc = SibTr.register(JmsDestinationImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    private static final long serialVersionUID = 1503547629070926432L;

    // ******************************* CONSTANTS *********************************

    static final String NAME_SEPARATOR = "://";
    static final String DEST_PREFIX = "dest" + NAME_SEPARATOR;

    /**
     * This object is used for the Referenceable support. It contains the code that
     * we used to have in JmsReferenceUtils, but which was moved to the JCA component
     * so that they could use it as well.
     * This variable is instantiated in the static initializer for this class.
     */
    private final static JmsJcaReferenceUtils refUtils;

    /**
     * This object has utility methods called from the toString().
     * It is static so that we don't end up trying to serialize it each time.
     */
    private static MsgDestEncodingUtilsImpl destEncoder;

    static {

        // Initialise the reference utils object for later use.
        refUtils = JmsJcaReferenceUtils.getInstance();

        try {
            destEncoder = (MsgDestEncodingUtilsImpl) JmsInternalsFactory.getMessageDestEncodingUtils();
        } catch (JMSException jmse) {
            FFDCFilter.processException(jmse, "JmsDestinationImpl.<clinit>", "JmsDestinationImpl.<clinit>#1");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Exception caught creating MsgDestEncodingUtilsImpl");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.exception(tc, jmse);
            // not much we can do here, as the JmsInternalsFactory shouldn't ever throw this exception
        }

    }

    /**
     * This Map is used to store the state of all of the Destination state
     * variables.
     * 
     * The keys of this map are string constants defined in the ApiJmsConstants
     * class for use by the Destination, and the values (which may include null) and
     * primitive wrappers or String objects - which must be immutable. No guarantees
     * are made regarding behaviour if mutable objects are set as keys or values into
     * this map.
     */
    protected Map<String, Object> properties = null;

    /**
     * This Reliability is used to specify the QOS for a reply message.
     * It may be set when a message has a replyReliability and it's
     * ReplyTo header populated.
     * When the MessageProducer send a reply to this destination it should use this
     * reliability if it is set.
     * NB: Reliability is NOT serializable, so we can not just store the ReplyReliability
     * itself.
     * As Reliability.NONE != null, we need to initialize the byte with something we know
     * means it hasn't been set
     */
    private byte replyReliabilityByte = -1;

    /**
     * Cache variables
     * ---------------
     * 
     * The following variables are used to hold local caches of various string to avoid
     * having to recalculate them each time a method is sent.
     * 
     * The method clearCachedEncodings is used to reset all the caches so that they are
     * recalculated the next time they are requested - usually called by all the setter
     * methods.
     */

    // The full encoded URI string (for toString)
    private transient String cachedEncodedString = null;
    // The partial encoded URI string
    private transient String cachedPartialEncodedString = null;

    // The cached byte arrays for the various encoding levels
    private transient byte[][] cachedEncoding = new byte[EncodingLevel.values().length][];

    /**
     * These variables cache destinationAddress objects to prevent us having
     * to create a new one each time we want to use it. They are accessed only
     * through a getter, and are cleared if any destination state is set
     * which could cause the destination address to become 'out-of-date'.
     */
    private transient SIDestinationAddress producerDestinationAddress = null;
    private transient SIDestinationAddress consumerDestinationAddress = null;

    // ***************************** CONSTRUCTORS ********************************

    JmsDestinationImpl() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>");

        // Initialize the local state and set up the defaults for all the known properties.
        properties = new HashMap<String, Object>();

        // Set up defaults.
        properties.put(JmsraConstants.READ_AHEAD, ApiJmsConstants.READ_AHEAD_AS_CONNECTION);
        properties.put(JmsInternalConstants.DELIVERY_MODE, ApiJmsConstants.DELIVERY_MODE_APP);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Constructor that initialises the Destination based on the Reference provided.
     * It is called by JmsDestinationFactory when a Reference describing a dest is
     * retrieved from a JNDI namespace.
     * 
     * @param ref The JNDI reference
     */
    @SuppressWarnings("unchecked")
    JmsDestinationImpl(Reference ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", ref);

        properties = refUtils.getMapFromReference(ref, MsgDestEncodingUtilsImpl.getDefaultJNDIProperties(this.getClass()));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    // *********** INTERFACE (and other) GET/SET METHODS *************************

    /**
     * @see com.ibm.websphere.sib.api.jms.JmsDestination#getDestName()
     *      Note: This method should in general be replaced by calls to getProducerDestName
     *      and getConsumerDestName which handle the presence of forward routing
     *      paths.
     */
    @Override
    public String getDestName() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDestName");

        String destName = (String) properties.get(DEST_NAME);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDestName", destName);
        return (destName);
    }

    /**
     * setDestName
     * Set the destName for this Destination.
     * 
     * @param destName The value for the destName
     * 
     * @exception JMSException Thrown if the destName is null or blank
     */
    void setDestName(String destName) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDestName", destName);

        if ((null == destName) || ("".equals(destName))) {
            // d238447 FFDC review. More likely to be an external rather than internal error,
            //   so no FFDC.
            throw (JMSException) JmsErrorUtils.newThrowable(
                                                            InvalidDestinationException.class,
                                                            "INVALID_VALUE_CWSIA0281",
                                                            new Object[] { "destName", destName },
                                                            tc
                            );
        }
        // Store the property
        updateProperty(DEST_NAME, destName);

        // Clear the cached destinationAddresses, as they may now be incorrect
        clearCachedProducerDestinationAddress();
        clearCachedConsumerDestinationAddress();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDestName");
    }

    /**
     * @see com.ibm.websphere.sib.api.jms.JmsDestination#getDestDiscrim()
     */
    @Override
    public String getDestDiscrim() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDestDiscrim");

        String destDiscrim = (String) properties.get(DEST_DISCRIM);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDestDiscrim", destDiscrim);
        return (destDiscrim);
    }

    /**
     * setDestDiscrim
     * Set the destDiscrim for this Destination.
     * 
     * @param destDiscrim The value for the destDiscrim
     */
    void setDestDiscrim(String destDiscrim) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDestDiscrim", destDiscrim);

        updateProperty(DEST_DISCRIM, destDiscrim);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDestDiscrim");
    }

    /**
     * @see com.ibm.websphere.sib.api.jms.JmsDestination#getDeliveryMode()
     */
    @Override
    public String getDeliveryMode() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDeliveryMode");

        String dm = (String) properties.get(JmsInternalConstants.DELIVERY_MODE);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDeliveryMode", dm);
        return dm;
    }
    @Override
    public String getDeliveryModeDefault() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDeliveryModeDefault");

        String dm = (String) properties.get(JmsInternalConstants.DELIVERY_MODE_DEFAULT);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDeliveryModeDefault", dm);
        return dm;
    }

    /**
     * @see com.ibm.websphere.sib.api.jms.JmsDestination#setDeliveryMode(java.lang.String)
     */
    @Override
    public void setDeliveryMode(String x) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDeliveryMode", x);

        // check supplied value is valid
        if (ApiJmsConstants.DELIVERY_MODE_APP.equals(x)
            || ApiJmsConstants.DELIVERY_MODE_NONPERSISTENT.equals(x)
            || ApiJmsConstants.DELIVERY_MODE_PERSISTENT.equals(x)) {
            // value ok, store it
            updateProperty(JmsInternalConstants.DELIVERY_MODE, x);
        }
        else {
            // bad value, throw exception
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class
                                                            , "INVALID_VALUE_CWSIA0281"
                                                            , new Object[] { "deliveryMode", x }
                                                            , tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDeliveryMode");
    }
    @Override
    public void setDeliveryModeDefault(String x) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setDeliveryModeDefault", x);

        // check supplied value is valid
        if (ApiJmsConstants.DELIVERY_MODE_APP.equals(x)
            || ApiJmsConstants.DELIVERY_MODE_NONPERSISTENT.equals(x)
            || ApiJmsConstants.DELIVERY_MODE_PERSISTENT.equals(x)) {
            // value ok, store it
            updateProperty(JmsInternalConstants.DELIVERY_MODE_DEFAULT, x);
        }
        else {
            // bad value, throw exception
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class
                                                            , "INVALID_VALUE_CWSIA0281"
                                                            , new Object[] { "deliveryModeDefault", x }
                                                            , tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setDeliveryModeDefault");
    }

    /**
     * @see com.ibm.websphere.sib.api.jms.JmsDestination#getTimeToLive()
     */
    @Override
    public Long getTimeToLive() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getTimeToLive");

        Long ttl = (Long) properties.get(JmsInternalConstants.TIME_TO_LIVE);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getTimeToLive", ttl);
        return ttl;
    }
    @Override
    public Long getTimeToLiveDefault() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getTimeToLiveDefault");

        Long ttl = (Long) properties.get(JmsInternalConstants.TIME_TO_LIVE_DEFAULT);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getTimeToLiveDefault", ttl);
        return ttl;
    }

    /**
     * @see com.ibm.websphere.sib.api.jms.JmsDestination#setTimeToLive(Long)
     */
    @Override
    public void setTimeToLive(Long x) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTimeToLive", x);

        // valid values are null, 0 <= x <= MAX_TIME_TO_LIVE and -2
        if (x == null) {
            updateProperty(JmsInternalConstants.TIME_TO_LIVE, null);
        }
        else if (x.longValue() == -2) {
            // This value is for consistency with MA88, map to null
            updateProperty(JmsInternalConstants.TIME_TO_LIVE, null);
        }
        else if (x.longValue() >= 0 && x.longValue() <= MfpConstants.MAX_TIME_TO_LIVE) {
            updateProperty(JmsInternalConstants.TIME_TO_LIVE, x);
        }
        else {
            // bad value, throw exception
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class
                                                            , "INVALID_VALUE_CWSIA0281"
                                                            , new Object[] { "timeToLive", x }
                                                            , tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTimeToLive");
    }
    public void setTimeToLiveDefault(Long x) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTimeToLiveDefault", x);

        // valid values are null, 0 <= x <= MAX_TIME_TO_LIVE and -2
        if (x == null) {
            updateProperty(JmsInternalConstants.TIME_TO_LIVE_DEFAULT, null);
        }
        else if (x.longValue() == -2) {
            // This value is for consistency with MA88, map to null
            updateProperty(JmsInternalConstants.TIME_TO_LIVE_DEFAULT, null);
        }
        else if (x.longValue() >= 0 && x.longValue() <= MfpConstants.MAX_TIME_TO_LIVE) {
            updateProperty(JmsInternalConstants.TIME_TO_LIVE_DEFAULT, x);
        }
        else {
            // bad value, throw exception
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class
                                                            , "INVALID_VALUE_CWSIA0281"
                                                            , new Object[] { "timeToLiveDefault", x }
                                                            , tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTimeToLiveDefault");
    }

    /**
     * @see com.ibm.websphere.sib.api.jms.JmsDestination#getPriority()
     */
    @Override
    public Integer getPriority() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getPriority");

        Integer pri = (Integer) properties.get(JmsInternalConstants.PRIORITY);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getPriority", pri);
        return pri;
    }
    @Override
    public Integer getPriorityDefault() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getPriorityDefault");

        Integer pri = (Integer) properties.get(JmsInternalConstants.PRIORITY_DEFAULT);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getPriorityDefault", pri);
        return pri;
    }

    /**
     * @see com.ibm.websphere.sib.api.jms.JmsDestination#setPriority(java.lang.Integer)
     */
    @Override
    public void setPriority(Integer x) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setPriority", x);

        // valid values are null, 0 <= x <= 9 and -2
        if (x == null) {
            updateProperty(JmsInternalConstants.PRIORITY, null);
        }
        else if (x.intValue() == -2) {
            // This value is for consistency with MA88, map to null
            updateProperty(JmsInternalConstants.PRIORITY, null);
        }
        else if (0 <= x.intValue() && x.intValue() <= 9) {
            updateProperty(JmsInternalConstants.PRIORITY, x);
        }
        else {
            // bad value, throw exception
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class
                                                            , "INVALID_VALUE_CWSIA0281"
                                                            , new Object[] { "priority", x }
                                                            , tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setPriority");
    }
    @Override
    public void setPriorityDefault(Integer x) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setPriorityDefault", x);

        // valid values are null, 0 <= x <= 9 and -2
        if (x == null) {
            updateProperty(JmsInternalConstants.PRIORITY_DEFAULT, null);
        }
        else if (x.intValue() == -2) {
            // This value is for consistency with MA88, map to null
            updateProperty(JmsInternalConstants.PRIORITY_DEFAULT, null);
        }
        else if (0 <= x.intValue() && x.intValue() <= 9) {
            updateProperty(JmsInternalConstants.PRIORITY_DEFAULT, x);
        }
        else {
            // bad value, throw exception
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class
                                                            , "INVALID_VALUE_CWSIA0281"
                                                            , new Object[] { "priorityDefault", x }
                                                            , tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setPriorityDefault");
    }

    /**
     * @see com.ibm.websphere.sib.api.jms.JmsDestination#getReadAhead()
     */
    @Override
    public String getReadAhead() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getReadAhead");

        String ra = (String) properties.get(JmsraConstants.READ_AHEAD);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getReadAhead", ra);
        return ra;
    }

    /**
     * @see com.ibm.websphere.sib.api.jms.JmsDestination#setReadAhead(java.lang.String)
     */
    @Override
    public void setReadAhead(String value) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReadAhead", value);

        // Check for null and empty string, then ensure that the value is one of
        // the permitted constants.
        if ((value == null)
            || ("".equals(value))
            || ((!ApiJmsConstants.READ_AHEAD_AS_CONNECTION.equals(value))
                && (!ApiJmsConstants.READ_AHEAD_ON.equals(value))
                && (!ApiJmsConstants.READ_AHEAD_OFF.equals(value))
            )) {
            // An invalid value was specified for this property.
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class
                                                            , "INVALID_VALUE_CWSIA0281"
                                                            , new Object[] { "readAhead", value }
                                                            , tc
                            );

        }

        // Store the property.
        updateProperty(JmsraConstants.READ_AHEAD, value);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReadAhead");
    }

    /**
     * @see com.ibm.websphere.sib.api.jms.JmsDestination#getBusName()
     */
    @Override
    public String getBusName() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getBusName");

        String busName = (String) properties.get(JmsInternalConstants.BUS_NAME);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getBusName", busName);
        return busName;
    }

    /**
     * @see com.ibm.websphere.sib.api.jms.JmsDestination#setBusName(String busName)
     */
    @Override
    public void setBusName(String busName) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setBusName", busName);

        if (busName != null && busName.equals("")) {
            busName = null;
        }

        updateProperty(BUS_NAME, busName);

        // Clear the cached destinationAddresses, as they may now be incorrect
        clearCachedProducerDestinationAddress();
        clearCachedConsumerDestinationAddress();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setBusName");
    }

    /**
     * @see javax.naming.Referenceable#getReference()
     */
    @Override
    public Reference getReference() throws NamingException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getReference");

        // Create a reference object describing this class.
        // Use getClass(), not JmsDestinationImpl.class so it works for subclasses.
        Reference ref = new Reference(getClass().getName(),
                        JmsFactoryFactoryImpl.class.getName(),
                        null);

        // Defer to the supporting method to handle populating the reference with
        // useful information.
        synchronized (properties) {
            // Take a sync block around this to be on the safe side. It should never
            // be contested because the odds of concurrent access are so small.
            refUtils.populateReference(ref, properties, MsgDestEncodingUtilsImpl.getDefaultJNDIProperties(this.getClass()));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getReference", ref);
        return ref;
    }

    /**
     * Get the reply reliability to use for reply mesages on a replyTo destination
     * 
     * @return the reply reliability
     */
    protected Reliability getReplyReliability() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getReplyReliability");
        Reliability result = null;
        if (replyReliabilityByte != -1) {
            result = Reliability.getReliability(replyReliabilityByte);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getReplyReliability", result);
        return result;
    }

    /**
     * Set the reliability to use for reply messages on a replyTo destination
     * 
     * @param replyReliability The value to be set into this Destination.
     */
    protected void setReplyReliability(Reliability replyReliability) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReplyReliability", replyReliability);
        this.replyReliabilityByte = replyReliability.toByte();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReplyReliability");
    }

    /**
     * @see com.ibm.ws.sib.api.jms.JmsDestInternals#_getInhibitJMSDestination()
     */
    @Override
    public boolean _getInhibitJMSDestination() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_getInhibitJMSDestination");

        boolean value = false;
        if (properties.get(INHIBIT_DESTINATION) != null)
            value = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_getInhibitJMSDestination", value);
        return value;
    }

    /**
     * @see com.ibm.ws.sib.api.jms.JmsDestInternals#_setInhibitJMSDestination(boolean)
     */
    @Override
    public void _setInhibitJMSDestination(boolean value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_setInhibitJMSDestination", value);

        if (value) {
            // We wish to inhibit the storage of the JMS destination on the message.
            properties.put(INHIBIT_DESTINATION, "yes");
        }
        else {
            // We do not wish to inhibit storage of the JMS destination on the message (default)
            properties.remove(INHIBIT_DESTINATION);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_setInhibitJMSDestination");
    }

    /**
     * @see com.ibm.ws.sib.api.jms.JmsDestInternals#getBlockedDestinationCode()
     */
    @Override
    public Integer getBlockedDestinationCode() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getBlockedDestinationCode");

        Integer code = (Integer) properties.get(JmsInternalConstants.BLOCKED_DESTINATION);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getBlockedDestinationCode", code);
        return code;
    }

    /*
     * @see com.ibm.ws.sib.api.jms.JmsDestInternals#setBlockedDestinationCode(java.lang.Integer)
     */
    @Override
    public void setBlockedDestinationCode(Integer code) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setBlockedDestinationCode", code);

        updateProperty(JmsInternalConstants.BLOCKED_DESTINATION, code);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setBlockedDestinationCode");
    }

    // *********** INSTANCE METHODS USED BY OTHER IMPLEMENTATION CLASSES *********

    /**
     * This method informs us whether we can carry out type checking on
     * the producer connect call.
     */
    protected boolean isProducerTypeCheck() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "isProducerTypeCheck");
        boolean checking = true;

        // We can carry out checking if there is no FRP, or the FRP has 0 size.
        StringArrayWrapper frp = (StringArrayWrapper) properties.get(FORWARD_ROUTING_PATH);
        if (frp != null) {
            List totalPath = frp.getMsgForwardRoutingPath();
            if ((totalPath != null) && (totalPath.size() > 0))
                checking = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "isProducerTypeCheck", checking);
        return checking;
    }

    /**
     * This method returns the name of the destination to which producers should
     * attach when sending messages via this JMS destination.
     * 
     * For a simple case this will be the same as the original destName, however
     * if a forward routing path is present it will return the name of the first
     * element in the forward routing path.
     */
    protected String getProducerDestName() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getProducerDestName");

        String pDestName = null;

        // Get the forward routing path.
        StringArrayWrapper frp = (StringArrayWrapper) properties.get(FORWARD_ROUTING_PATH);

        if (frp == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "No forward routing path to examine");

            // If the frp is null then we have the simple case of returning the
            // 'big' destination.
            pDestName = getDestName();
        }

        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "There is a forward routing path to examine.");

            // There is an FRP to examine, and we want to return the name of the
            // first destination in the FRP.
            SIDestinationAddress producerAddr = frp.getProducerSIDestAddress();
            if (producerAddr != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Using first element of FRP as producer dest name");
                pDestName = producerAddr.getDestinationName();
            }
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "FRP is empty - use original dest name");
                pDestName = getDestName();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getProducerDestName", pDestName);
        return pDestName;
    }

    /**
     * This method returns the name of the destination to which consumers should
     * attach when receiving messages using this JMS destination.
     * 
     * In the simple case this is the same as the original destName, however if
     * a reverse routing path is present this method returns the name at the end
     * of the logical forward routing path. (This will still be the same as the
     * original dest name). THIS COMMENT APPEARS TO BE UNTRUE.
     * 
     * @return String
     */
    protected String getConsumerDestName() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConsumerDestName");

        String cDestName = getDestName();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConsumerDestName", cDestName);
        return cDestName;
    }

    /**
     * Creates a new Map object and duplicates the properties into the new Map.
     * Note that it does not _copy_ the parameter keys and values so if the map
     * contains objects which are mutable you may get strange behaviour.
     * In short - only use immutable objects for keys and values!
     * 
     * This method is used by MsgDestEncodingUtilsImpl to create a copy of the properties.
     */
    Map<String, Object> getCopyOfProperties() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getCopyOfProperties");

        Map<String, Object> temp = null;

        // Make sure no-one changes the properties underneath us.
        synchronized (properties) {
            temp = new HashMap<String, Object>(properties);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getCopyOfProperties", temp);
        return temp;
    }

    /**
     * This method returns the encoding as an array of bytes for the
     * purpose of setting it into the method directly as bytes (avoid char
     * copying).
     */
    byte[] encodeToBytes(EncodingLevel encodingLevel) throws JMSException {

        // If we haven't already got the encoding cached, encode & cache it now.
        if (cachedEncoding[encodingLevel.ordinal()] == null) {
            cachedEncoding[encodingLevel.ordinal()] = destEncoder.getMessageRepresentationFromDest(this, encodingLevel);
        }
        return cachedEncoding[encodingLevel.ordinal()];
    }

    /*
     * @see com.ibm.websphere.sib.api.jms._FRPHelper#setForwardRoutingPath(java.lang.String[])
     * NB: This should be package access, but is public because it implements a method
     * of _FRPHelper which is used by tests.
     */
    @Override
    public void setForwardRoutingPath(String[] forwardPath) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setForwardRoutingPath", (forwardPath == null ? null : Arrays.asList(forwardPath)));

        // If it is an empty array then we consider it to be the same as null.
        if ((forwardPath != null) && (forwardPath.length == 0)) {
            forwardPath = null;
        }

        // Now check that none of the elements is null.
        if (forwardPath != null) {
            // Note that at this point we know that it has at least one element.
            for (int i = 0; i < forwardPath.length; i++) {
                if (forwardPath[i] == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Element " + i + " of the array is null.");
                    throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class
                                                                    , "INVALID_VALUE_CWSIA0281"
                                                                    , new Object[] { "forwardPath[" + i + "]", "null" }
                                                                    , tc
                                    );
                }
            }
        }

        // Clear the cache of the encoding string since we are changing properties.
        clearCachedEncodings();

        // Clear the cached producerDestinationAddress, as it may now be incorrect
        clearCachedProducerDestinationAddress();

        // Now store or remove the property
        if (forwardPath != null) {
            properties.put(FORWARD_ROUTING_PATH, StringArrayWrapper.create(forwardPath, getDestName()));
        }
        else {
            properties.remove(FORWARD_ROUTING_PATH);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setForwardRoutingPath");
    }

    /**
     * @see com.ibm.websphere.sib.api.jms._FRPHelper#setReverseRoutingPath(java.lang.String[])
     *      NB: This should be package access, but is public because it implements a method
     *      of _FRPHelper which is used by tests.
     */
    @Override
    public void setReverseRoutingPath(String[] reversePath) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReverseRoutingPath", (reversePath == null ? null : Arrays.asList(reversePath)));

        // If it is an empty array then we consider it to be the same as null.
        if ((reversePath != null) && (reversePath.length == 0)) {
            reversePath = null;
        }

        // Now check that none of the elements is null.
        if (reversePath != null) {
            // Note that at this point we know that it has at least one element.
            for (int i = 0; i < reversePath.length; i++) {
                if (reversePath[i] == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Element " + i + " of the array is null.");
                    throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class
                                                                    , "INVALID_VALUE_CWSIA0281"
                                                                    , new Object[] { "reversePath[" + i + "]", "null" }
                                                                    , tc
                                    );
                }
            }
        }

        // Clear the cache of the encoding string since we are changing properties.
        clearCachedEncodings();

        // Now store or remove the property
        if (reversePath != null) {
            properties.put(REVERSE_ROUTING_PATH, StringArrayWrapper.create(reversePath, getDestName()));
        }
        else {
            properties.remove(REVERSE_ROUTING_PATH);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReverseRoutingPath");
    }

    /**
     * This method returns the "List containing SIDestinationAddress" form of the
     * forward routing path that will be set into the message.
     * 
     * Note that this takes the 'big' destination as being the end of the forward
     * routing path.
     */
    protected List getConvertedFRP() {

        List theList = null;
        StringArrayWrapper saw = (StringArrayWrapper) properties.get(FORWARD_ROUTING_PATH);
        if (saw != null) {
            // This list is the forward routing path for the message.
            theList = saw.getMsgForwardRoutingPath();
        }
        return theList;
    }

    /**
     * This method returns the "List containing SIDestinationAddress" form of the
     * reverse routing path.
     */
    protected List getConvertedRRP() {

        List theList = null;
        StringArrayWrapper saw = (StringArrayWrapper) properties.get(REVERSE_ROUTING_PATH);
        if (saw != null)
            theList = saw.getCorePath();
        return theList;
    }

    /**
     * This method provides access to the cached SIDestinationAddress object for
     * this JmsDestination.
     * 
     * Parts of the JMS implementation that wish to obtain an SIDestinationAddress
     * object for use with the coreSPI should call this method rather than creating
     * their own new one in situations where it is might be possible to reuse the
     * object.
     */
    protected SIDestinationAddress getProducerSIDestinationAddress() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getProducerSIDestinationAddress");

        if (producerDestinationAddress == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "No cached value");

            StringArrayWrapper frp = (StringArrayWrapper) properties.get(FORWARD_ROUTING_PATH);
            if (frp != null) {
                // There is an actual forward routing path to investigate.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Obtain from the frp data.");
                producerDestinationAddress = frp.getProducerSIDestAddress();
            }

            if (producerDestinationAddress == null) {

                // Set up the producer address from the big destination info.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Either FRP was empty, or no FRP at all - create from big dest info.");

                // Establish whether this producer should be scoped to a local queue point only.
                boolean localOnly = isLocalOnly();

                // This variable should be initialised already since it is set up in the static
                // init for the class, which must have been run by the time we get to access it.
                producerDestinationAddress = JmsMessageImpl.destAddressFactory.createSIDestinationAddress(getProducerDestName(), localOnly, getBusName());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getProducerSIDestinationAddress", producerDestinationAddress);
        return producerDestinationAddress;
    }

    /**
     * As getProducerSIDestinationAddress() but for the consumer view of the destination.
     * 
     * @return SIDestinationAddress
     * @throws JMSException
     */
    protected SIDestinationAddress getConsumerSIDestinationAddress() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConsumerSIDestinationAddress");

        if (consumerDestinationAddress == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "No cached value - create a new one.");

            // This variable should be initialised already since it is set up in the static
            // init for the class, which must have been run by the time we get to access it.

            // Establish whether this producer should be scoped to a local queue point only.
            boolean localOnly = isLocalOnly();

            // The consumer details are those that we use as the 'big' destination information.
            consumerDestinationAddress = JmsMessageImpl.destAddressFactory.createSIDestinationAddress(getConsumerDestName(), localOnly, getBusName());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConsumerSIDestinationAddress", consumerDestinationAddress);
        return consumerDestinationAddress;
    }

    /**
     * Determines whether SIDestinationAddress objects created for this destination object
     * should have the localOnly flag set or not.
     * 
     * By default this is hardcoded as false, but can be overridden by subclasses where
     * appropriate to provide customized behaviour.
     * 
     * @return boolean
     */
    protected boolean isLocalOnly() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "isLocalOnly");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "isLocalOnly", false);
        return false;
    }

    // *********** STATIC METHODS USED BY OTHER IMPLEMENTATION CLASSES ********* *

    /**
     * Check that the supplied destination is a native JMS destination object.
     * If it is, then exit quietly. If it is not, then throw an exception.
     * 
     * Note:
     * When using Spring in certain ways it provides a proxy objects in the place
     * of native destination objects. It this method detects that situation then
     * it returns a new queue or topic object that is created using the toString
     * representation of the Spring proxy object (in tests the toString has been
     * seen to be that of the wrappered native destination object).
     * 
     * @param destination the JMS destination object to check
     * @return The destination object cast to the implementation class
     * 
     * @throws javax.jms.InvalidDestinationException
     *             if the destination object is null (INVALID_VALUE_CWSIA0281) or if it is
     *             from a foreign implementation (FOREIGN_IMPLEMENTATION_CWSIA0046).
     */
    static JmsDestinationImpl checkNativeInstance(Destination destination) throws InvalidDestinationException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkNativeInstance", destination);

        JmsDestinationImpl castDest = null;

        // if the supplied destination is set to null, throw a jms
        // InvalidDestinationException.
        if (destination == null) {
            throw (InvalidDestinationException) JmsErrorUtils.newThrowable(InvalidDestinationException.class
                                                                           , "INVALID_VALUE_CWSIA0281"
                                                                           , new Object[] { "Destination", null }
                                                                           , tc
                            );
        }

        // Attempt to convert a non-native destination into a native one.
        if (!(destination instanceof JmsDestinationImpl)) {

            // Use the variable to pass the failed conversion exception if one occurs.
            Exception rootCause = null;

            //106556: if (destination instanceof java.lang.reflect.Proxy) is commented to generically support all types of Proxies. 
            //We get a WovenProxy here when Apache Aries framework is used to deploy .eba files in liberty. 
            //This WovenProxy is new type itself and it is not extending java.lang.reflect.Proxy.

            // 412946 workaround - the Spring framework passes in dynamic proxy objects
            // that wrap a real JmsDestinationImpl object if the application is using
            // Aspects to implement its transactional behaviour in an OSGi environment.
            // To work around this we have to take the unpleasant option of recreating
            // one of our objects using the toString of the object we have been given.
            // We should never normally rely on the format of a toString, but it seems to
            // be the only way to resolve the problem in this case.
            String destToString = destination.toString();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Dynamic proxy has been provided instead of a destination: " + destToString);

            try {
                if (destination instanceof Queue) {
                    castDest = (JmsDestinationImpl) JmsFactoryFactory.getInstance().createQueue(destToString);
                }
                else if (destination instanceof Topic) {
                    castDest = (JmsDestinationImpl) JmsFactoryFactory.getInstance().createTopic(destToString);
                }
                else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "A destination must be either a queue or a topic");
                }
            } catch (JMSException jmse) {
                // No FFDC Code Needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Failed to convert the dynamic proxy to a JmsDestinationImpl object;");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.exception(tc, jmse);
                rootCause = jmse;
            }

            // If the supplied destination isn't a Jetstream destination, throw a jms
            // InvalidDestinationException.
            if (castDest == null) {
                throw (InvalidDestinationException) JmsErrorUtils.newThrowable(InvalidDestinationException.class
                                                                               , "FOREIGN_IMPLEMENTATION_CWSIA0046"
                                                                               , new Object[] { destination }
                                                                               , rootCause
                                                                               , null
                                                                               , JmsDestinationImpl.class
                                                                               , tc
                                );
            }
        }

        else {
            castDest = (JmsDestinationImpl) destination;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkNativeInstance", castDest);
        return castDest;
    }

    /**
     * This method takes a JmsDestinationImpl object and checks the
     * blocked destination code. If the code signals that the destination
     * is blocked, a suitable JMSException will be thrown, tailored to the
     * code received.
     * 
     * @param dest The JmsDestinationImpl to check
     * @throws JMSException if the destination is blocked
     */
    static void checkBlockedStatus(JmsDestinationImpl dest) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkBlockedStatus", dest);

        // get the status of the destinations blocked attribute
        Integer code = dest.getBlockedDestinationCode();

        // check for specific known PSB reason for blockage
        if (code == null) {
            // default case of null value - don't want to throw exception
        }
        else if (code.equals(JmsInternalConstants.PSB_REPLY_DATA_MISSING)) {
            // throw specific PSB blocked JMSException
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class
                                                            , "DESTINATION_BLOCKED_PSBREPLY_CWSIA0284"
                                                            , new Object[] { dest.toString() }
                                                            , tc
                            );
        }
        else {
            // throw generic blocked JMSException
            throw (JMSException) JmsErrorUtils.newThrowable(JMSException.class
                                                            , "DESTINATION_BLOCKED_CWSIA0283"
                                                            , new Object[] { code }
                                                            , tc
                            );
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkBlockedStatus");
    }

    /**
     * Static method that allows a replyTo destination to be obtained from a JsJmsMessage,
     * a ReverseRoutingPath and an optional JMS Core Connection object.
     * 
     * @param _msg CoreSPI message for which the JMS replyTo dest should be generated
     * @param rrp Reverse routing path of the message. Should not be queried directly from 'msg'
     *            for efficiency reasons.
     * @param _siConn JMS Core connection object that can be used if necessary to help determine
     *            the type of the destination (optional)
     * @return JmsDestinationImpl
     * @throws JMSException
     */
    static JmsDestinationImpl getJMSReplyToInternal(JsJmsMessage _msg, List<SIDestinationAddress> rrp, SICoreConnection _siConn) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getJMSReplyToInternal", new Object[] { _msg, rrp, _siConn });

        JmsDestinationImpl tempReplyTo = null;

        // Case a) - check for JMS specific data in compressed byte form.
        byte[] replyURIBytes = _msg.getJmsReplyTo();

        if (replyURIBytes != null) {
            tempReplyTo = (JmsDestinationImpl) JmsInternalsFactory.getMessageDestEncodingUtils().getDestinationFromMsgRepresentation(replyURIBytes);
        }

        if (tempReplyTo == null) {
            // Cases b) & c) both depend on there being a reverse routing path, otherwise
            // there is no replyTo.

            // lookup the name of the dest in the reverse routing path
            SIDestinationAddress sida = null;

            if (rrp.size() > 0) {

                // The last element of the RRP becomes the reply to destination
                int lastDestInRRP = rrp.size() - 1;
                sida = rrp.get(lastDestInRRP);

                // Case b) - if we have a live connection, we can use that to query the dest type
                if (_siConn != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Determine reply dest type using SICoreConnection");
                    try {

                        // get the destination configuration & type
                        DestinationConfiguration destConfig = _siConn.getDestinationConfiguration(sida);
                        DestinationType destType = destConfig.getDestinationType();

                        if (destType == DestinationType.TOPICSPACE) {
                            tempReplyTo = new JmsTopicImpl();
                        }
                        else {
                            tempReplyTo = new JmsQueueImpl();
                        }
                    } catch (SIException sice) {
                        // No FFDC code needed
                        // d246604 Trace exceptions, but don't throw on. Fall back to
                        // case c) below.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            SibTr.debug(tc, "failed to look up dest type because of " + sice);
                            SibTr.debug(tc, "detail ", sice);
                        }
                    }
                }

                // Case c) - Guess based on the discriminator
                if (tempReplyTo == null) {
                    // 239238 - make a stab at determining whether it's a queue or topic
                    // reply destination based on the reply disciminator.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Guess reply dest type using reply discriminator");

                    String replyDiscrim = _msg.getReplyDiscriminator();
                    if ((replyDiscrim == null) || ("".equals(replyDiscrim))) {
                        tempReplyTo = new JmsQueueImpl();
                    }
                    else {
                        tempReplyTo = new JmsTopicImpl();
                    }
                }
            }
        }

        // Now fill in the fields that were hidden in the reply header.
        if (tempReplyTo != null) {
            populateReplyToFromHeader(tempReplyTo, _msg, rrp);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getJMSReplyToInternal", tempReplyTo);
        return tempReplyTo;
    }

    // ******* METHODS WHICH SHOULD BE PRIVATE BUT ARE USED BY UNIT TEST *******

    /**
     * This method is used to encode the JMS Destination information for transmission
     * with the message, and subsequent recreation on the other side. The format of the
     * string is as follows;
     * 
     * queue://my.queue.name
     * queue://my.queue.name?name1=value1&name2=value2
     * topic://my.topic.name?name1=value1&name2=value2
     * 
     * Note that the partialEncode method is used for the reply destination since some of the
     * reply fields are stored in the message header for use by other coreSPI applications.
     * This is not the case for the JMSDestination object, which should represent the destination
     * to which the message was sent.
     * 
     * The format of destinations here follows that implemented in feature 189318 (URIDestinationCreator).
     * 
     * TODO: This is only used by toString() & Unit tests, so they should change to use
     * toString() directly, and this should either become private, or move into toString()
     */
    String fullEncode() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "fullEncode");

        String encoded = null;

        // If we have a cached version of the string, use it.
        if (cachedEncodedString != null) {
            encoded = cachedEncodedString;
        }

        // Otherwise, encode it
        else {
            // Get a copy of the properties which make up this Destination. We need a
            // copy since we remove the jms name before iterating over the other
            // properties, and encoding them.
            Map<String, Object> destProps = getCopyOfProperties();

            // Pass off to a common helper method (between full and partial encodings).
            encoded = encodeMap(destProps);

            // Now store this string in the cache in case we need it later.
            cachedEncodedString = encoded;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "fullEncode", encoded);
        return encoded;
    }

    /**
     * A variant of the fullEncode method that is used for URI-encoding for the
     * purposes of setting it into the jmsReplyTo field of the core message.
     * 
     * This is different from full encode because some of the fields of the destination
     * are stored in the message reply header so that they can be accessed and
     * altered by core SPI applications / mediations.
     * 
     * TODO: This is only used by Unit tests, and those tests appear to be simply
     * testing this method - so scrap it & them?
     */
    String partialEncode() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "partialEncode()");

        String encoded = null;

        // If we have a cached version of the string, use it.
        if (cachedPartialEncodedString != null) {
            encoded = cachedPartialEncodedString;
        }

        else {
            // Get a copy of the properties which make up this Destination. We need a
            // copy since we remove the jms name before iterating over the other
            // properties, and encoding them.
            Map<String, Object> destProps = getCopyOfProperties();

            // Remove the props that are stored in the reply header.
            // NB. The deliveryMode is used in the  creation of the reply header,
            //      but must still be carried.
            destProps.remove(JmsInternalConstants.DEST_NAME);
            destProps.remove(JmsInternalConstants.DEST_DISCRIM);
            destProps.remove(JmsInternalConstants.PRIORITY);
            destProps.remove(JmsInternalConstants.TIME_TO_LIVE);

            destProps.remove(JmsInternalConstants.FORWARD_ROUTING_PATH);
            destProps.remove(JmsInternalConstants.REVERSE_ROUTING_PATH);

            // Pass off to a common helper method (between full and partial encodings).
            encoded = encodeMap(destProps);

            // Now store this string in the cache in case we need it later.
            cachedPartialEncodedString = encoded;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "partialEncode", encoded);
        return encoded;
    }

    /**
     * @see com.ibm.websphere.sib.api.jms._FRPHelper#getForwardRoutingPath()
     *      NB: Used only by tests,
     */
    @Override
    public String[] getForwardRoutingPath() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getForwardRoutingPath");

        String[] path = null;
        StringArrayWrapper saw = (StringArrayWrapper) properties.get(FORWARD_ROUTING_PATH);
        if (saw != null) {
            path = saw.getArray();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getForwardRoutingPath", (path == null ? null : Arrays.asList(path)));
        return path;
    }

    /**
     * @see com.ibm.websphere.sib.api.jms._FRPHelper#getReverseRoutingPath()
     *      NB: Used only by tests,
     */
    @Override
    public String[] getReverseRoutingPath() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getReverseRoutingPath");

        String[] path = null;
        StringArrayWrapper saw = (StringArrayWrapper) properties.get(REVERSE_ROUTING_PATH);
        if (saw != null) {
            path = saw.getArray();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getReverseRoutingPath", (path == null ? null : Arrays.asList(path)));
        return path;
    }

    /**
     * configureReplyDestinationFromRoutingPath
     * Configure the ReplyDestination from the ReplyRoutingPath. The RRP from the message
     * is used as the FRP for this Reply Destination, so most of the function is
     * performed by configureDestinationFromRoutingPath.
     * This method keeps hold of the bus names as well as the destination names.
     * 
     * This method configures the whole destination (including the 'big'
     * destination and bus names).
     * 
     */
    void configureDestinationFromRoutingPath(List fwdPath) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "configureDestinationFromRoutingPath", fwdPath);

        // Clear the cache of the encoding string since we are changing properties.
        clearCachedEncodings();

        // Clear the producerDestinationAddress, as changing the FRP would make it out-of-date
        clearCachedProducerDestinationAddress();

        // Store the property if it has entries...
        if ((fwdPath != null) && (fwdPath.size() > 0)) {

            // There is at least one element in this path.
            // The last element in the list is used to configure the 'big' destination.
            int lastEltIndex = fwdPath.size() - 1;

            // The last element of the reverse routing path becomes the destination, and
            // anything left over goes into the forward path of the destination.
            SIDestinationAddress lastElt = (SIDestinationAddress) fwdPath.get(lastEltIndex);

            // Fill in the destination name information early so that it is there when
            // the forward routing path is set.
            String destName = lastElt.getDestinationName();
            setDestName(destName);

            // Set up the bus name to point at the bus of the last element as
            // well.
            //not setting the bus name for Liberty .. as in other config paths the bus name was not set.
            // this is leading the same destination with different properties.
            //String destBusName = lastElt.getBusName();
            //setBusName(destBusName);

            // If there is more than one element then we store the rest of the path
            // in the wrapper object.
            if (fwdPath.size() > 1) {
                properties.put(FORWARD_ROUTING_PATH, new StringArrayWrapper(fwdPath));
            }

            // Otherwise, i.e. if there is only one element, we stash away the destination
            // as we need to use it as the producerDestinationAddress.
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "caching producerDestinationAddress: " + lastElt);
                producerDestinationAddress = lastElt;
            }
        }

        // ...otherwise, remove it
        else {
            properties.remove(FORWARD_ROUTING_PATH);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "configureDestinationFromRoutingPath");
    }

    // ******************* PRIVATE-ISH IMPLEMENTATION METHODS ********************

    /**
     * Utility method update a property.
     * Checks that the value is different to the existing value and clears the cache if necessary.
     * Needs package/protected access as also used by subclasses (in the same package).
     * 
     * @param key The name of the property
     * @param value The new value
     */
    protected void updateProperty(String key, Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "updateProperty", new Object[] { key, value });

        // check the value is different to the existing one
        if (isDifferent(key, value)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "new value for " + key + ": " + value);

            // clear the cached encodings
            clearCachedEncodings();

            if (value == null) {
                properties.remove(key);
            }
            else {
                // put new value
                properties.put(key, value);
            }
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "value for " + key + " same as existing");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "updateProperty");
    }

    /**
     * This method should be called by any setter methods which alter the state
     * information of this destination in such a way that the cached
     * producerDestinationAddresses would now be out-of-date.
     */
    protected void clearCachedProducerDestinationAddress() {
        producerDestinationAddress = null;
    }

    /**
     * This method should be called by any setter methods which alter the state
     * information of this destination in such a way that the cached
     * consumerDestinationAddresses would now be out-of-date.
     */
    protected void clearCachedConsumerDestinationAddress() {
        consumerDestinationAddress = null;
    }

    /**
     * This method should be called by all setter methods if they alter the state
     * information of this destination. Doing so will cause the string encoded version
     * of this destination to be recreated when necessary.
     */
    private void clearCachedEncodings() {

        cachedEncodedString = null;
        cachedPartialEncodedString = null;

        for (int i = 0; i < cachedEncoding.length; i++)
            cachedEncoding[i] = null;
    }

    /**
     * Compare the specified value with that in the properties Map
     * This utility method is used from the set* methods to determine
     * if the value is being changed and hence the cache needs to be
     * cleared.
     * 
     * @param key The name of the property in the Map
     * @param value The value to be tested for equality
     * @return true if the values are different
     */
    private boolean isDifferent(String key, Object value) {
        // Get the value from the properties Map for the specified key
        Object currentVal = properties.get(key);
        if (currentVal == null) {
            // If the currentVal is null, then test value against null
            return value != null;
        }
        else {
            // For non-null, need to use object.equals method
            return !currentVal.equals(value);
        }
    }

    /**
     * This method iterates through the property map of a destination
     * and constructs a string representing the name and property details.
     * 
     * @param destProps The destination property map
     * @return String The destination details in a String format
     */
    private String encodeMap(Map<String, Object> destProps) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "encodeMap", destProps);

        // string result to return
        StringBuffer result = new StringBuffer("");

        // new HashMap which will contain only non-default NVPs
        Map<String, Object> nonDefaultProps = new HashMap<String, Object>();

        // Deal with each remaining property in turn.
        Iterator<Map.Entry<String, Object>> props = destProps.entrySet().iterator();
        while (props.hasNext()) {

            // Get the name and value of the property.
            Map.Entry<String, Object> nextProp = props.next();
            String propName = nextProp.getKey();
            Object propValue = nextProp.getValue();

            // make sure the value part goes out the same as it came in....
            // ie. if it contains an un-escaped '&', place '\\' behind it.
            // we only do this if the property value is a String.
            if (propValue instanceof String) {
                propValue = URIDestinationCreator.escapeValueString((String) propValue);
            }

            // for the name, get the default value
            Object propDefaultValue = MsgDestEncodingUtilsImpl.getDefaultPropertyValue(propName);

            // check to see if the value we have is the same as the default
            if (((propDefaultValue == null) && (propValue == null))
                || ((propDefaultValue != null) && (propDefaultValue.equals(propValue)))) {
                // Do nothing - we don't want to waste space encoding default values.
            }
            else {
                // if Queue, we don't want to add DEST_NAME as it will be added later manually
                if (this instanceof JmsQueue && !propName.equals(DEST_NAME)) {
                    // place the non-default name and value in the new HashMap
                    nonDefaultProps.put(propName, propValue);
                }
                // we don't want to add DEST_DISCRIM if its a topic, or the topic space if its default,
                // or the DEST_NAME, because they get added manually later.
                else if (this instanceof JmsTopic
                         && !DEST_DISCRIM.equals(propName)
                         && !DEST_NAME.equals(propName)
                         && !JmsTopicImpl.DEFAULT_TOPIC_SPACE.equals(propValue)) {
                    if (propValue != null) {
                        // place the non-default name and value in the new HashMap
                        nonDefaultProps.put(propName, propValue);
                    }
                }
            }
        }

        // Now prepare to loop through the new HashMap which only contains non-default properties,
        // and print each name and value separated by an "&" (unless its the last NVP).
        Iterator<Map.Entry<String, Object>> iter = nonDefaultProps.entrySet().iterator();

        // Queue specific stuff
        if (this instanceof JmsQueue) {
            // add the prefix
            result.append("queue://");

            // get the dest name out of the map
            String destName = (String) destProps.get(DEST_NAME);

            // guard against null values being printed as "null"
            if (destName == null)
                destName = "";

            // Validate the dest name value to escape unescaped illegal chars.
            // These escape chars would have been removed when the dest was created.
            destName = URIDestinationCreator.escapeDestName(destName);

            // put the name of the queue name as the first part of the result
            result.append(destName);
            // if there are no more NVPs, we don't want a '?' on the end of the name
            if (iter.hasNext()) {
                result.append("?");
            }
        }

        // Topic specific stuff
        else if (this instanceof JmsTopic) {
            // add prefix
            result.append("topic://");

            // get out the props we know names of
            String destName = (String) destProps.get(DEST_DISCRIM);
            String topicSpace = (String) destProps.get(DEST_NAME);

            // guard against null values being printed as "null"
            if (destName == null)
                destName = "";
            if (topicSpace == null)
                topicSpace = "";

            // Validate the dest name and topic space values to escape unescaped illegal chars.
            // These escape chars would have been removed when the dest was created.
            destName = URIDestinationCreator.escapeDestName(destName);
            topicSpace = URIDestinationCreator.escapeValueString(topicSpace);

            // Add the name of the topic first.
            result.append(destName);

            // Make sure topic space is the first NVP after topic name.
            // We don't want to print the topic space if it is the default value.
            if (!JmsTopicImpl.DEFAULT_TOPIC_SPACE.equals(topicSpace)) {

                //NB. By putting the constant first we avoid doing null/empty checking.
                result.append("?topicSpace=");
                result.append(topicSpace);

                // Now prepare the string for further properties if they exist.
                if (iter.hasNext())
                    result.append("&");
            }
            else {
                // We have not added the topic space in, but need to prepare the string
                // for further properties if they exist.
                if (iter.hasNext())
                    result.append("?");
            }
        }

        // If this isn't a Queue or a Topic, not much we can do other than alert the user
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Destination in question is neither a JmsQueue of JmsTopic");
        }

        // loop through the non-default NVPs and append to dest name
        while (iter.hasNext()) {

            Map.Entry<String, Object> nextProp = iter.next();

            // Liberty change : 	
            // Setting the name of the bus to null so that it does not get printed in 
            // the Message
            if (nextProp.getKey().equals(BUS_NAME)) {
                continue;
            }

            result.append(nextProp.getKey());
            result.append("=");
            result.append(nextProp.getValue());

            // only append an ampersand if there are more props to come
            if (iter.hasNext())
                result.append("&");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "encodeMap", result);
        return result.toString();
    }

    /**
     * This method takes the part created replyTo object (that will be returned
     * to the user) and fills in the fields that were stored as part of the
     * reply header.
     * 
     * Note that this method will throw an exception if the values on the reply
     * header are not suitable for storage in the JMSDestination object - ie somebody
     * messed with the reply header while the message was in transit, and
     * corrupted the data in some way.
     * 
     * @param tempReplyTo Object to be configured
     * @param _msg Message object containing the reply header info
     * @param rpp Reverse routing path of the message (not to be queried directly from 'msg'
     *            for efficiency reasons).
     */
    private static void populateReplyToFromHeader(JmsDestinationImpl tempReplyTo, JsJmsMessage _msg, List<SIDestinationAddress> rrp) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "populateReplyToFromHeader", new Object[] { tempReplyTo, _msg, rrp });

        // Copy the reverse routing path from the message into the destination we are creating.
        tempReplyTo.configureDestinationFromRoutingPath(rrp);

        String replyDiscrim = _msg.getReplyDiscriminator();
        if (replyDiscrim != null)
            tempReplyTo.setDestDiscrim(replyDiscrim);

        Integer pri = _msg.getReplyPriority();
        if (pri != null)
            tempReplyTo.setPriority(pri);

        Long ttl = _msg.getReplyTimeToLive();
        if (ttl != null)
            tempReplyTo.setTimeToLive(ttl);

        // d317816 Set the deliveryMode according to the message replyReliability
        Reliability reliability = _msg.getReplyReliability();
        if ((reliability != null) && (reliability != Reliability.NONE)) {
            tempReplyTo.setReplyReliability(reliability);
        }

        // Although the tempReplyTo object isn't strictly 'returned' from this method is
        // has been configured by now, so it is useful to show what it is returned as.
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "populateReplyToFromHeader", tempReplyTo);
    }

    // **** OVERRIDES OF STANDARD java.lang.Object METHODS ***********************

    /**
     * @see java.lang.Object#equals(Object)
     *      Note:
     *      There is a slightly bizarre situation in which the algorithm will
     *      cause some apparently equal (as far as toString) objects to be defined
     *      as not equal. as follows;
     * 
     *      Create a temporary destination and set it as a replyTo field in the message
     *      Send the message and receive it in the same JVM.
     *      Call getJMSReplyTo to retrieve the destination.
     *      Call equals on the original object with the retrieved one as the parameter.
     * 
     *      The problem is that the original one was an instance of JmsTemporaryXXXImpl
     *      and the retrieved one is an instance of JmsXXXImpl (as created by decoding
     *      the URI format stored in the message).
     * 
     *      Having thought about this (JBK/MR) we have come to the conclusion that it is
     *      indeed the correct thing to fail the equals test, and that if users object
     *      then they can compare the individual elements of the objects (which will
     *      all be equal).
     */
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (other == null)
            return false;

        if (this.getClass() != other.getClass())
            return false;

        // Now we know that other is a JmsDestinationImpl, let's cast it once and for all
        JmsDestinationImpl that = (JmsDestinationImpl) other;

        Map<String, Object> thisProps = this.properties;
        Map<String, Object> thatProps = that.properties;
        if (thisProps == thatProps)
            return true;
        if (thisProps == null)
            return false;

        if (thisProps.equals(thatProps))
            return true;

        // It may be that the properties maps only differ because one has
        // values missing and the other has them at the default. This can
        // happen when using JNDI references. For those properties where default
        // values are stripped out and then inserted during conversion, we need
        // recheck the property maps with the defaults resolved
        Map<String, Object> thisWithDefaults = new HashMap<String, Object>();
        Map<String, Object> thatWithDefaults = new HashMap<String, Object>();
        thisWithDefaults.putAll(MsgDestEncodingUtilsImpl.getDefaultJNDIProperties(this.getClass()));
        thatWithDefaults.putAll(MsgDestEncodingUtilsImpl.getDefaultJNDIProperties(that.getClass()));

        thisWithDefaults.putAll(thisProps);
        if (thatProps != null)
            thatWithDefaults.putAll(thatProps);

        return thisWithDefaults.equals(thatWithDefaults);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (properties != null)
            return properties.hashCode();
        else
            return 0;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return fullEncode();
    }

    /*
     * Reinitialize our transient variables when being deserialized
     * (See the java object serialization spec, section 3.4)
     */
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        // instantiate persistent variables
        stream.defaultReadObject();
        // initialize the non-null transient variables
        cachedEncoding = new byte[EncodingLevel.values().length][];
    }
}
