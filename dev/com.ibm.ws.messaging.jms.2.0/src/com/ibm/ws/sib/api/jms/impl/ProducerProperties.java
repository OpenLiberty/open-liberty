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

import java.util.List;
import java.util.Map;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.PersistenceType;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class holds pre-calculated state on behalf of the Producer.send method
 * so that we avoid calculating things more than once.
 *
 * The inbound properties are used to maintain the knowledge of what was used
 * to set up this object to avoid un-necessary recalculation when values are
 * overriden.
 *
 * @author matrober
 */
public class ProducerProperties
{

  // ************************** TRACE INITIALISATION ***************************

  private static TraceComponent tc = SibTr.register(ProducerProperties.class, ApiJmsConstants.MSG_GROUP_INT, ApiJmsConstants.MSG_BUNDLE_INT);

  // **************************** STATE VARIABLES ******************************

  /*
   * Inbound properties for JMS.
   */
  // Overrides from the destination
  private String inDmOverride = ApiJmsConstants.DELIVERY_MODE_APP;
  private Integer inPriOverride = null;
  private Long inTTL_Override = null;

  private Reliability nonPerReliability = Reliability.EXPRESS_NONPERSISTENT;
  private Reliability perReliability = Reliability.RELIABLE_PERSISTENT;

  // this may be overkill now.
  private Reliability outboundReliability;

  // Method or producer properties.
  private int inDeliveryMode = Message.DEFAULT_DELIVERY_MODE;
  private int inPriority = Message.DEFAULT_PRIORITY;
  private long inTTL = Message.DEFAULT_TIME_TO_LIVE;

  /*
   * Outbound properties to be used in the sendMessage method.
   */
  private List outboundForwardRoutingPath = null;
  private List outboundReverseRoutingPath_Part = null; // Part of the RRP will be the replyTo, which is on the msg.

  private PersistenceType outboundDeliveryMode = PersistenceType.PERSISTENT;
  private Integer outboundPriority = Integer.valueOf(inPriority);
  private long outboundTTL = inTTL;

  private String outboundDiscrim = null;

  // ***************************** CONSTRUCTORS ********************************

  /**
   * Constructor.
   *
   * If the persistent and non-persistent reliabilities are known at the time this constructor
   * is called (for example in the identified destination case when creating a producer) then
   * they can be specified as parameter. Otherwise they will be looked up from the map.
   */
  public ProducerProperties(JmsDestinationImpl dest, JmsMsgProducerImpl producer, Map passThruProps, Reliability perRel, Reliability nonperRel) throws JMSException {

    if (nonperRel == null) {
      if (passThruProps != null) {
        // Look up the mapping state for NPM and store it where necessary.
        String connPropVal = (String)passThruProps.get(JmsraConstants.NON_PERSISTENT_MAP);
        if (connPropVal != null) nonPerReliability = lookupReliability(connPropVal);
      }
    }
    else {
      // Set the reliability from the parameter value that has already been calculated.
      nonPerReliability = nonperRel;
    }

    if (perRel == null) {
      if (passThruProps != null) {
        String connPropVal = (String)passThruProps.get(JmsraConstants.PERSISTENT_MAP);
        if (connPropVal != null) perReliability = lookupReliability(connPropVal);
      }
    }
    else {
      // Set the reliability from the parameter value that has already been calculated.
      perReliability = perRel;
    }

    setInDeliveryModeOverride(dest.getDeliveryMode());
    setInPriorityOverride(dest.getPriority());
    setInTTL_Override(dest.getTimeToLive());

    String defDM = dest.getDeliveryModeDefault();
    if (null!=defDM) {
      setInDeliveryMode(defDM.equals(ApiJmsConstants.DELIVERY_MODE_PERSISTENT)?DeliveryMode.PERSISTENT:DeliveryMode.NON_PERSISTENT);
    }
    if (null!=dest.getPriorityDefault()) setInPriority(dest.getPriorityDefault());
    if (null!=dest.getTimeToLiveDefault()) setInTTL(dest.getTimeToLiveDefault());

    outboundForwardRoutingPath = dest.getConvertedFRP();
    outboundReverseRoutingPath_Part = dest.getConvertedRRP();
    outboundDiscrim = dest.getDestDiscrim();

    if (producer != null) {
      setInDeliveryMode(producer.getDeliveryMode());
      setInPriority(producer.getPriority());
      setInTTL(producer.getTimeToLive());
    }

    // Now that we have established the CF property
    // and the producer DM value, we are in a position to determine how NPM affects
    // the reliability.
    recalcOutReliability();
  }

  // **************************** INBOUND METHODS ******************************
  /**
   * The inbound deliveryMode override setting from a destination. This should
   * change infrequently (ie never!) once it has been set.
   */
  public void setInDeliveryModeOverride(String val) {
    // First check whether we have already set this value.
    if (!inDmOverride.equals(val)) {
      // A change is required.
      inDmOverride = val;
      // Now recalculate the outbound property.
      recalcOutDeliveryMode();
    }
  }

  /**
   * The inbound deliveryMode as defined by the producer or argument. This method
   * causes the outbound effective delivery mode to be calculated on the basis of
   * the override.
   * @param dm
   */
  public void setInDeliveryMode(int dm) {
    if (inDeliveryMode != dm) {
      // Changes are required.
      inDeliveryMode = dm;
      recalcOutDeliveryMode();
      // this will change the reliability we need to use
      recalcOutReliability();
    }
  }

  /**
   * Sets the administrative override for priority.
   * @param pri
   */
  public void setInPriorityOverride(Integer pri) {
    // Check whether changes have been made.
    if ( ((inPriOverride == null) && (pri != null)) ||
         ((inPriOverride != null) && (!inPriOverride.equals(pri)))
       ) {
      inPriOverride = pri;
      recalcOutPriority();
    }
  }

  /**
   * Set the priority on the basis of the producer.
   * @param pri
   */
  public void setInPriority(int pri) {
    if (inPriority != pri) {
      inPriority = pri;
      recalcOutPriority();
    }
  }

  /**
   * Sets the administrative override for ttl.
   * @param pri
   */
  public void setInTTL_Override(Long ttl) {
    // Check whether changes have been made.
    if ( ((inTTL_Override == null) && (ttl != null)) ||
         ((inTTL_Override != null) && (!inTTL_Override.equals(ttl)))
       ) {
      inTTL_Override = ttl;
      recalcOutTTL();
    }
  }

  /**
   * Set the ttl on the basis of the producer.
   * @param pri
   */
  public void setInTTL(long ttl) throws JMSException {
    if (inTTL != ttl) {
      // Now verify that the value for TTL is valid.
      if (ttl < 0 || ttl > MfpConstants.MAX_TIME_TO_LIVE) {
        throw (JMSException) JmsErrorUtils.newThrowable(
           JMSException.class,
           "INVALID_VALUE_CWSIA0301",
           new Object[] {"timeToLive", ""+ttl},
           tc);
      }
      inTTL = ttl;
      recalcOutTTL();
    }
  }

  // *************************** OUTBOUND METHODS ******************************

  /**
   * The deliveryMode attribute is a combination of the (int) deliveryMode specified on
   * the producer or method call, and the (String) deliveryMode property of the destination.
   */
  public PersistenceType getEffectiveDeliveryMode() {
    return outboundDeliveryMode;
  }

  /**
   * The priority attribute is a combination of the (int) priority specified on
   * the producer or method call, and the (Integer) priority property of the destination.
   */
  public Integer getEffectivePriority() {
    return outboundPriority;
  }

  /**
   * The timeToLive attribute is a combination of the (long) ttl specified on
   * the producer or method call, and the (Long) timeToLive property of the destination.
   */
  public long getEffectiveTTL() {
    return outboundTTL;
  }

  /**
   * Returns the converted FRP information that will be set into the message.
   * @return List
   */
  public List getConvertedFRP() {
    return outboundForwardRoutingPath;
  }

  /**
   * Returns the converted RRP information that will be set into the message.
   * Note that for reverse paths this will be merged with the reply destination
   * information set into each individual message.
   * @return List
   */
  public List getConvertedRRP_Part() {
    return outboundReverseRoutingPath_Part;
  }

  /**
   * Obtain the discriminator for this producer
   * @return String
   */
  public String getDiscriminator() {
    return outboundDiscrim;
  }

  /**
   * Return the effective reliability value.
   * @return Reliability
   */
  public Reliability getEffectiveReliability() {
    return outboundReliability;
  }

  // *********************** IMPLEMENTATION METHODS ****************************

  /**
   * This method called by the appropriate setter methods to recalculate the
   * outbound delivery mode.
   */
  private void recalcOutDeliveryMode() {
    // Changes are required.
    if (inDmOverride.equals(ApiJmsConstants.DELIVERY_MODE_PERSISTENT)) {
      outboundDeliveryMode = PersistenceType.PERSISTENT;
    }
    else if (inDmOverride.equals(ApiJmsConstants.DELIVERY_MODE_NONPERSISTENT)) {
      outboundDeliveryMode = PersistenceType.NON_PERSISTENT;
    }
    else {
      // Dest.deliveryMode is validated on set, so treating everything else
      // as app should be ok here.
      if (inDeliveryMode == DeliveryMode.PERSISTENT) {
        outboundDeliveryMode = PersistenceType.PERSISTENT;
      }
      else {
        outboundDeliveryMode = PersistenceType.NON_PERSISTENT;
      }
    }
  }

  /**
   * Recalculate the priority that will be used by the send method on the
   * basis of the administrative override and producer value.
   *
   */
  private void recalcOutPriority() {
    if (inPriOverride == null) {
      // Set off the producer
      outboundPriority = Integer.valueOf(inPriority);
    }
    else {
      // Administrative override.
      outboundPriority = inPriOverride;
    }
  }

  /**
   * Recalculate the ttl that will be used by the send method on the
   * basis of the administrative override and producer value.
   *
   */
  private void recalcOutTTL() {
    if (inTTL_Override == null) {
      // Set off the producer
      outboundTTL = inTTL;
    }
    else {
      // Administrative override.
      outboundTTL = inTTL_Override.longValue();
    }
  }

  /**
   * Recalculate the reliability for this message on the basis of
   * the effective deliveryMode and CF QOS mappings.
   *
   */
  private void recalcOutReliability() {
    if (outboundDeliveryMode == PersistenceType.NON_PERSISTENT) {
      outboundReliability = nonPerReliability;
    }
    else {
      outboundReliability = perReliability;
    }
  }


  /**
   * Converts from a string name for a QOS (including the JMS specific "asSIBDestination")
   * to an instance of mfp's Reliability.
   *
   * @param name
   * @return An instance of Reliability
   * @throws JMSException if the lookup fails
   */
  static Reliability lookupReliability(String name) throws JMSException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "lookupReliability", name);
    Reliability result;

    // map asSIBDest -> NONE
    if (ApiJmsConstants.MAPPING_AS_SIB_DESTINATION.equals(name)) {
      result = Reliability.NONE;
    }
    else {
      try {
        result = Reliability.getReliabilityByName(name);
      }
      catch (Exception e) {
        // No FFDC code needed
        throw (JMSException) JmsErrorUtils.newThrowable(
          JMSException.class,
          "INVALID_VALUE_CWSIA0301",
          new Object[] { "quality of service", name },
          tc);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "lookupReliability",  result);
    return result;
  }
}
