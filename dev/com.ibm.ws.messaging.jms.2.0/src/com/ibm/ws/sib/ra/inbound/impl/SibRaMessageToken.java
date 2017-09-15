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
package com.ibm.ws.sib.ra.inbound.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIMessageHandle;

/**
 * Represents a message as it is passed from the listener component in the
 * control region adjunct to the dispatcher component in the servant region via
 * an <code>SRDispatcher</code>. The message handle field is a mutable field
 * to allow the token to be reused.
 */
public final class SibRaMessageToken implements Serializable {

    /** The classes serial version UID; Added at version 1.1 */
    private static final long serialVersionUID = 3701528563970253962L;

    /**
     * The name of the bus on which the messaging engine resides.
     */
    private final String _busName;

    /**
     * The UUID of the messaging engine to connect to.
     */
    private final String _meUuid;

    /**
     * The ID of the session to which the message is locked.
     */
    private final long _sessionId;

    /**
     * The J2EE name of the message-driven bean to which the message should be
     * delivered.
     */
    private final String _j2eeName;

    /**
     * An array list of message handles that refer to the messages to be delivered
     */
    private ArrayList _messageHandles;
    
    /**
     * Context information about the messages
     */
    private transient Map _contextInfo;

    /** Indicates if the context map contains only the default information */
    private transient boolean _contextInfoContainsDefaultEntries;
    /**
     * Whether this token contains message handles that can be deleted at read time or later
     */
    private boolean _deleteMessagesWhenRead;

    /**
     * The unrecoveredReliability that the consumer session was created with
     */
    private int _unrecoveredReliability;

    /**
     * The max failed deliveries value for the destination that this message is from
     */
    private int _maxFailedDeliveries;

    /**
     * The max sequential message threshold value for the destination that this message is from
     */
    private int _sequentialMessageThreshold;
    
    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaMessageToken.class);

    /**
     * Constructor.
     *
     * @param busName
     *            the name of the bus on which the messaging engine resides
     * @param meUuid
     *            the UUID for the messaging engine
     * @param sessionId
     *            the ID of the consumer session
     * @param j2eeName
     *            the J2EE name of the message-driven bean
     */
    SibRaMessageToken(final String busName, final String meUuid,
            final long sessionId, final String j2eeName,
            final Reliability unrecoveredReliability,
            final int maxFailedDeliveries,
            final int sequentialMessageThreshold) {

        final String methodName = "SibRaMessageToken";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { busName,
                    meUuid, sessionId, j2eeName, maxFailedDeliveries, sequentialMessageThreshold });
        }

        _busName = busName;
        _meUuid = meUuid;
        _sessionId = sessionId;
        _j2eeName = j2eeName;
        _unrecoveredReliability = unrecoveredReliability.toInt();
        _messageHandles = new ArrayList ();
        _maxFailedDeliveries = maxFailedDeliveries;  //This value will always be the same for this destination
        _sequentialMessageThreshold = sequentialMessageThreshold;  //This value will always be the same for this destination

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Returns the name of the bus on which the messaging engine resides.
     *
     * @return the bus name
     */
    String getBusName() {

        return _busName;

    }

    /**
     * Returns the J2EE name of the message-driven bean to deliver the message
     * to.
     *
     * @return the J2EE name
     */
    String getJ2EEName() {

        return _j2eeName;

    }

    /**
     * Returns the handle to the message to deliver.
     *
     * @param index the index of the message handle to get
     * @return the message handle
     */
    SIMessageHandle getMessageHandle(int index) {

        return (SIMessageHandle) _messageHandles.get(index);

    }

    /**
     * Gets the number of message handles in this token
     *
     * @return the number of message handles
     */
    int getNumberOfMessageHandles () {

      return _messageHandles.size();

    }

    /**
     * Returns the UUID of the messaging engine to connect to.
     *
     * @return the messaging engine UUID
     */
    String getMeUuid() {

        return _meUuid;

    }

    /**
     * Returns the ID of the session to which the message is locked.
     *
     * @return the session ID
     */
    long getSessionId() {

        return _sessionId;

    }

    /**
     * Gets the unrecovered reliability that the session was created with.
     * @return The unrecovered reliability for the consumer session
     */
    int getUnrecoveredReliability () {

      return _unrecoveredReliability;

    }
    
    /**
     * Checks to see if the messages associated with this token can be deleted when they are read.
     *
     * @return true if the messages in this token can be deleted when read
     */
    public boolean isDeleteMessagesWhenRead () {

      return _deleteMessagesWhenRead;
    }

    /**
     * Gets the context info used for this token
     * @return The context info for this batch of messages.
     */
    Map getContext () {

        return _contextInfo;
    }

    /**
     * clear out the token ready for reuse. Resets any state data. Some variables
     * do not need resetting as they will not change.
     */
    void clear () {

      _deleteMessagesWhenRead = false;
      _messageHandles.clear();
      _contextInfo = null;
      _contextInfoContainsDefaultEntries = false;
    }

    /**
     * Attemts to add the supplied message handle to the token. This is only done if the
     * context information matches and both messages are BENP or both are not BENP.
     * @param handle The message handle to try and add to the token
     * @param ctxInfo The context info associated with the message
     * @param unrecoveredReliability The unrecovered reliability that the session was created with
     * @param canBeDeleted If the messages associated with this token cane be deleted or not.
     * @return true if the handle was added to the token (the context info and BENP
     * flags matched).
     */
    boolean addHandle (SIMessageHandle handle,
                       Map ctxInfo,
                       final boolean canBeDeleted) {

      final String methodName = "addHandle";
      if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
          SibTr.entry(this, TRACE, methodName, new Object [] { handle, ctxInfo, canBeDeleted });
      }

      boolean addedToThisToken = false;

      if (_messageHandles.isEmpty())
      {
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
          SibTr.debug(TRACE, "No existing message handles - using current message as template <ctxInfo="
              + ctxInfo + "> <canBeDeleted=" + canBeDeleted + "> <handle= " + handle + ">");
        }

        _messageHandles.add (handle);
        _contextInfo = ctxInfo;
        _deleteMessagesWhenRead = canBeDeleted;

        // Work out once whether this map is default size so we can optimize
        // the matches method later on.
        _contextInfoContainsDefaultEntries = (_contextInfo.size() == 2 //&&
        		//lohith liberty change
          //  _contextInfo.containsKey(WlmClassifierConstants.CONTEXT_MAP_KEY) &&
           /* _contextInfo.containsKey(ExitPointConstants.TYPE_NAME)*/);

        addedToThisToken = true;

      } else {

        if (matches (ctxInfo, canBeDeleted)) {

          if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
            SibTr.debug(TRACE, "Message matched token for supplied handle - adding handle to the token <handle=" + handle + ">");
          }

          _messageHandles.add (handle);

          addedToThisToken = true;
        }

      }

      if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
          SibTr.exit(this, TRACE, methodName, addedToThisToken);
      }

      return addedToThisToken;
    }

    /**
     * This method checks to see if the supplied information from a message handle matches
     * the information that this token is using.
     *
     * @param ctxInfo The context info of the new message handle
     * @param isBENP Whether the new message handle represents a BENP message or not
     * @return true if the information about the new messages matches the information
     * stored in this token
     */
    boolean matches (Map ctxInfo, boolean canBeDeleted)
    {
      final String methodName = "matches";
      if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
          SibTr.entry(this, TRACE, methodName, new Object [] { ctxInfo, canBeDeleted });
      }

      if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) {
        SibTr.debug(TRACE, "Attempting to match againgst token <context=" + _contextInfo +
                           "> <isBENP=" + _deleteMessagesWhenRead);
      }

      boolean doesMatch = false;

      // For the message to match the token the following must be true:
      //
      // * They must have the same BENP flag
      // * Both context maps must be of the same length
      // * They must be of size two.
      // * The values must be for the WLM Classifier and the MDB Type.
      // * The WLM classifiers must be equal in both maps.
      //
      // If all of these are true then we can batch and matches returns true.
      //
      // The value of the MDB type can be ignored as it is guaranteed to be the
      // same for any one MDB.
      //
      // If we have more than two items in the map then it contains other context
      // information - this other information may effect whether or not the messages
      // can be batched so we do not batch them.
      if (canBeDeleted == _deleteMessagesWhenRead &&
          _contextInfoContainsDefaultEntries &&
          (ctxInfo.size () == _contextInfo.size ())// &&
        //  ctxInfo.containsKey(WlmClassifierConstants.CONTEXT_MAP_KEY) &&
           /*ctxInfo.containsKey(ExitPointConstants.TYPE_NAME)*/)
        {
    	  //lohith liberty change
           /* Object WLMClassifier1 = _contextInfo.get(WlmClassifierConstants.CONTEXT_MAP_KEY);
            Object WLMClassifier2 = ctxInfo.get(WlmClassifierConstants.CONTEXT_MAP_KEY);*/
          //  doesMatch = (WLMClassifier1 != null) && (WLMClassifier1.equals (WLMClassifier2));
        }

      if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
        SibTr.exit(this, TRACE, methodName, doesMatch);
      }

      return doesMatch;
    }

    /**
     * Get the message handles in this token as an array.
     *
     * @return An array of the message tokens this token represents.
     */
    SIMessageHandle [] getMessageHandleArray () {

      return (SIMessageHandle []) (_messageHandles.toArray(new SIMessageHandle[0]));
    }
    
    /**
     * Get the configured maxFailedDeliveries value for the destination we are listening too
     * @return int maxFailedDeliveries
     */
    int getMaxFailedDeliveries()
    {
      return _maxFailedDeliveries;
    }

    /**
     * Get the configured sequentialMessageThreshold value for the destination we are listening too
     * @return int sequentialMessageThreshold
     */
    int getSequentialMessageThreshold()
    {
      return _sequentialMessageThreshold;
    }
    
    /**
     * Returns a string representation of this object.
     *
     * @return a string representation
     */
    public final String toString() {

        final SibRaStringGenerator generator = new SibRaStringGenerator(this);
        generator.addField("busName", _busName);
        generator.addField("j2eeName", _j2eeName);
        generator.addField("messageHandles", _messageHandles.toArray (new SIMessageHandle [0]));
        generator.addField("meUuid", _meUuid);
        generator.addField("sessionId", _sessionId);
        generator.addField("deleteMessagesWhenRead", _deleteMessagesWhenRead);
        generator.addField("contextInfo", _contextInfo);
        generator.addField("unrecoveredReliability", _unrecoveredReliability);
        generator.addField("maxFailedDeliveries", _maxFailedDeliveries);
        generator.addField("sequentialMessageThreshold", _sequentialMessageThreshold);
        return generator.getStringRepresentation();

    }

}
