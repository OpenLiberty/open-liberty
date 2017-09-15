/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.mfp.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.SIProperties;
import com.ibm.websphere.sib.exception.SIDataGraphSchemaNotFoundException;
import com.ibm.websphere.sib.exception.SIMessageException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.serialization.DeserializationObjectInputStream;
import com.ibm.ws.sib.matchspace.BadMessageFormatMatchingException;
import com.ibm.ws.sib.matchspace.Identifier;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.mfp.JsApiMessage;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.schema.JsApiAccess;
import com.ibm.ws.sib.mfp.schema.JsHdr2Access;
import com.ibm.ws.sib.utils.PasswordUtils;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectorDomain;

/**
 * JsApiMessageImpl extends JsApiPart1Impl and is the implementation class
 * for the remainder of the JsApiMessage interface.
 * <p>
 * The JsMessageImpl instance contains the JsMsgObject which is the
 * internal object which represents the API Message.
 * The implementation classes for all the specialised API messages extend
 * JsApiMessageImpl, either directly or indirectly, as well as implementing
 * their specialised interface.
 */
abstract class JsApiMessageImpl extends JsApiHdrsImpl implements JsApiMessage {

    private final static long serialVersionUID = 1L;

    private static TraceComponent tc = SibTr.register(JsApiMessageImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

    /* Prefixes for Message Properties - most are also used by JsSdoMessageimpl */
    final static String USER_PREFIX = "user.";
    final static int USER_PREFIX_LENGTH = USER_PREFIX.length();
    final static String SI_PREFIX = "SI_";
    final static int SI_PREFIX_LENGTH = SI_PREFIX.length();
    final static String SI_REPORT = "Report";
    final static int SI_REPORT_LENGTH = SI_REPORT.length();
    final static int SI_REPORT_OFFSET = SI_PREFIX_LENGTH + SI_REPORT_LENGTH;
    private final static String SI_EXCEPTION = "Exception";
    private final static int SI_EXCEPTION_LENGTH = SI_EXCEPTION.length();
    private final static int SI_EXCEPTION_OFFSET = SI_PREFIX_LENGTH + SI_EXCEPTION_LENGTH;

    /* Prefixes for JMS Message Properties - also used by JsJmsMessageImpl & JsSdoMessageImpl */
    final static String JMS_PREFIX = "JMS";
    final static int JMS_LENGTH = 3;
    final static char JMSX_EXTRA_PREFIX = 'X';
    final static int JMSX_LENGTH = 4;
    final static String JMS_IBM_EXTRA_PREFIX = "_IBM_";
    final static int JMS_IBM_LENGTH = 8;
    final static String MQMD_EXTRA_PREFIX = "MQMD_";
    final static String JMS_IBM_MQMD_PREFIX = JMS_PREFIX + JMS_IBM_EXTRA_PREFIX + MQMD_EXTRA_PREFIX;
    final static String REPORT = "Report_";
    final static int REPORT_LENGTH = REPORT.length();
    final static int REPORT_OFFSET = JMS_IBM_LENGTH + REPORT_LENGTH;

    /* Header for Byte Array Properties */
    private final static byte HEADER_BYTE_0 = (byte) 0xde;
    private final static byte HEADER_BYTE_1 = (byte) 0xad;

    // A guess at the space which would be taken up by normal message's properties.
    // There are probably no jmsSystem, otherUser or systemContext properties,
    // and we'll guess approx 2 short Strings + 2 int properties
    // plus the overhead of 2 lists.
    private final static int FLUFFED_PROPERTIES_GUESS = FLUFFED_STRING_OVERHEAD * 2 + 40
                                                        + FLUFFED_OBJECT_OVERHEAD * 2
                                                        + FLUFFED_JMF_LIST_SIZE * 2;

    /* ************************************************************************* */
    /* Constructors */
    /* ************************************************************************* */

    /**
     * Constructor for a new Jetstream API message.
     * 
     * This constructor should never be used explicitly.
     * It is only to be used implicitly by the sub-classes' no-parameter constructors.
     * The method must not actually do anything.
     */
    JsApiMessageImpl() {}

    /**
     * Constructor for a new Jetstream API message.
     * 
     * @param flag No-op flag to distinguish different constructors.
     * 
     * @exception MessageDecodeFailedException Thrown if such a message can not be created
     */
    JsApiMessageImpl(int flag) throws MessageDecodeFailedException {
        super(flag);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "<init>");
    }

    /**
     * Constructor for an inbound message.
     * (Only to be called by JsMessage.makeApiMessage().)
     * 
     * @param inJmo The JsMsgObject representing the inbound method.
     */
    JsApiMessageImpl(JsMsgObject inJmo) {
        super(inJmo);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "<init>, inbound jmo ");
    }

    /*
     * Provide the contribution of this part to the estimated encoded length
     * This contributes the API properties.
     */
    @Override
    int guessApproxLength() {
        int total = super.guessApproxLength();
        int size = 0;
        List props;

        // Assume 40 bytes per property (string name + value)
        // Each property map may be cached in a transient, or in the base JMF (or both!)
        // If there are no properties, the names may be represented by the EMPTY
        // JMF List, in which case we do NOT want to actually get them.
        // The property will only be flattened 'once' so only count it once.
        if (jmsUserPropertyMap != null) {
            size += jmsUserPropertyMap.size();
        }
        else {
            if (getApi().isNotEMPTYlist(JsApiAccess.JMSPROPERTY_NAME)) {
                props = (List) getApi().getField(JsApiAccess.JMSPROPERTY_NAME);
                if (props != null) {
                    size += props.size();
                }
            }
        }

        if (otherUserPropertyMap != null) {
            size += otherUserPropertyMap.size();
        }
        else {
            if (getApi().isNotEMPTYlist(JsApiAccess.OTHERPROPERTY_NAME)) {
                props = (List) getApi().getField(JsApiAccess.OTHERPROPERTY_NAME);
                if (props != null) {
                    size += props.size();
                }
            }
        }

        if (jmsSystemPropertyMap != null) {
            size += jmsSystemPropertyMap.size();
        }
        else {
            if (getApi().isNotEMPTYlist(JsApiAccess.SYSTEMPROPERTY_NAME)) {
                props = (List) getApi().getField(JsApiAccess.SYSTEMPROPERTY_NAME);
                if (props != null) {
                    size += props.size();
                }
            }
        }

        if (systemContextMap != null) {
            size += systemContextMap.size();
        }
        else {
            if (getApi().isNotEMPTYlist(JsApiAccess.SYSTEMCONTEXT_NAME)) {
                props = (List) getApi().getField(JsApiAccess.SYSTEMCONTEXT_NAME);
                if (props != null) {
                    size += props.size();
                }
            }
        }

        if (mqMdSetPropertiesMap != null) {
            size += mqMdSetPropertiesMap.size();
        }
        else {
            // This field usually doesn't exist, so no need for the isNotEMPTY check
            props = (List) getHdr2().getField(JsHdr2Access.MQMDPROPERTIES_MAP_NAME);
            if (props != null) {
                size += props.size();
            }
        }

        total += size * 40;
        return total;
    }

    /**
     * Provide the contribution of this part to the estimated 'fluffed' message size.
     * Subclasses that wish to contribute to a quick guess at the length of a
     * fluffed message should override this method, invoke their superclass and add on
     * their own contribution.
     * 
     * For this subclass, we just need to add on the fluffed sizes of the properties
     * and context.
     * 
     * @return int A guesstimate of the fluffed size of the message
     */
    @Override
    int guessFluffedSize() {

        // Get the contribution from the superclass(es)
        int total = super.guessFluffedSize();

        // If the parts which contain them are already fluffed up,
        // for each property map (which may be empty) get the estimated fluffed size
        // of the name list and double it to cater for the values too.

        // Each property map may be cached in a transient, or in the base JMF or both!
        // If they are in both, we need to include them twice as they are taking up
        // memory twice.

        // We only get the API portion of the message if it is already fluffed, as we
        // don't want to fluff it up just for this calculation.
        // It is possible for the message to change under us, due to a lazy copy,
        // such that the api we're given is no longer the correct one. It won't matter in
        // this case however, as we're not updating & we don't really care if the
        // data is out of date. The JsMsgPart returned by getApi or getApiIfFluffed should
        // NEVER usually be cached locally & reused.
        JsMsgPart part = getApiIfFluffed();
        if (part != null) {
            int nameSize = part.estimateFieldValueSize(JsApiAccess.JMSPROPERTY_NAME);
            total += nameSize * 2;
            nameSize = part.estimateFieldValueSize(JsApiAccess.OTHERPROPERTY_NAME);
            total += nameSize * 2;
            nameSize = part.estimateFieldValueSize(JsApiAccess.SYSTEMPROPERTY_NAME);
            total += nameSize * 2;
            nameSize = part.estimateFieldValueSize(JsApiAccess.SYSTEMCONTEXT_NAME);
            total += nameSize * 2;
        }
        // If the api JMF message wasn't fluffed we'll just use a pre-calculated guess.
        else {
            total += FLUFFED_PROPERTIES_GUESS;
        }

        // The Hdr2 portion probably is already fluffed as Processor accesses umpteen
        // of its fields. However, just in case it isn't we'll make sure we don't
        // cause it to be fluffed. Again, the returned JsMsgPart must NOT be cached for reuse.
        // If Hdr2 is not already fluffed, just assume there are no MQMD properties.
        part = getHdr2IfFluffed();
        if (part != null) {
            int nameSize = part.estimateFieldValueSize(JsHdr2Access.MQMDPROPERTIES_MAP_NAME);
            total += nameSize * 2;
        }

        // Any in the transient map will be counted as overhead of the map +
        // a constant amount per item, taking into account the overhead of String plus object.
        int numProps = 0;
        if (jmsUserPropertyMap != null) {
            total += FLUFFED_MAP_OVERHEAD;
            numProps += jmsUserPropertyMap.size();

        }
        if (otherUserPropertyMap != null) {
            total += FLUFFED_MAP_OVERHEAD;
            numProps += otherUserPropertyMap.size();
        }
        if (jmsSystemPropertyMap != null) {
            total += FLUFFED_MAP_OVERHEAD;
            numProps += jmsSystemPropertyMap.size();
        }
        if (systemContextMap != null) {
            total += FLUFFED_MAP_OVERHEAD;
            numProps += systemContextMap.size();
        }
        if (mqMdSetPropertiesMap != null) {
            total += FLUFFED_MAP_OVERHEAD;
            numProps += mqMdSetPropertiesMap.size();
        }
        total += numProps * FLUFFED_MAP_ENTRY_SIZE;

        return total;
    }

    /* ************************************************************************* */
    /* Implementation of JsApiMessage methods for User Message Properties */
    /* ************************************************************************* */

    /*
     * Return the User Property stored in the Message under the given name.
     * <p>
     * User Properties are stored as name-value pairs where the value may be any
     * Object which implements java.io.Serializable.
     * Note that the reference returned is to a copy of the actual Object stored.
     * 
     * Javadoc description supplied by SIBusSdoMessage & JsApiMessage interfaces.
     */
    @Override
    public final Serializable getUserProperty(String name) throws IOException, ClassNotFoundException {

        /* If the name is null there is nothing to do. so only proceed if it is */
        /* supplied. */
        if (name != null) {

            /* Call the real getUserProperty with forMatching set to false. */
            return getUserProperty(name, false);

        }

        /* If the name is null always return null. */
        else {
            return null;
        }
    }

    /*
     * Return a boolean indicating whether a User Property with the given name
     * exists in the message.
     * 
     * Javadoc description supplied by JsApiMessage interface.
     */
    @Override
    public boolean userPropertyExists(String name) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "userPropertyExists", name);

        boolean result = false;

        /* If the name is null there is nothing to do. so only proceed if it is */
        /* supplied. */
        if (name != null) {

            /* Got to check Maelstrom's transportVersion first as performance for it */
            /* is critical. */
            if (name.equals(MfpConstants.PRP_TRANSVER) && isTransportVersionSet()) {
                result = true;
            }
            /* otherwise, first try the JMS user property map as the most likely */
            else if ((mayHaveJmsUserProperties()) && (getJmsUserPropertyMap().containsKey(name))) {
                result = true;
            }
            /* then try the non-JMS user property map */
            else if ((mayHaveOtherUserProperties()) && (getOtherUserPropertyMap().containsKey(name))) {
                result = true;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "userPropertyExists", result);
        return result;
    }

    /* ************************************************************************* */
    /* Implementation of SystemContext methods */
    /* ************************************************************************* */

    /*
     * Return the item stored as a SystemContext under the given name.
     * 
     * Javadoc description supplied by JsApiMessage interface.
     */
    @Override
    public final Serializable getSystemContextItem(String name) throws IOException, ClassNotFoundException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getSystemContextItem", name);

        Serializable item = null;

        if (name != null) {

            /* Try getting the property from the Map */
            if (mayHaveSystemContext()) {
                item = (Serializable) getSystemContextMap().get(name);
            }

            /* If the item is null (i.e. no such property) just return it. */
            /* If the item was a String, Boolean or Number it isn't serialized so */
            /* we already have the item to return. */
            if ((item == null)
                || item instanceof String
                || item instanceof Number
                || item instanceof Boolean) {
            }

            /* Otherwise, it is a byte array of some sort & needs some work done. */
            else {
                item = restoreMapObject((byte[]) item);
            }

        }
        else {
            throw new IllegalArgumentException("null");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getSystemContextItem", item);
        return item;
    }

    /*
     * Add an item to the SystemContext under the given name.
     * 
     * Javadoc description supplied by JsApiMessage interface.
     */
    @Override
    public void putSystemContextItem(String name, Serializable item) throws IllegalArgumentException, IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "putSystemContextItem", new Object[] { name, item });

        /* If we have a non-null name */
        if (name != null) {

            /* If we really have an item */
            if (item != null) {

                /* If the item is of a JMS supported type, we can store it as is. */
                if (isValidForJms(item)) {
                    getSystemContextMap().put(name, item);
                }

                /* Otherwise, we need to take a safe copy & 'flatten it' suitably. */
                else {
                    getSystemContextMap().put(name, flattenMapObject(item));
                }

            }

            /* If item is null, just call deleteProperty */
            else {
                getSystemContextMap().remove(name);
            }
        }

        /* A null name is invalid. */
        else {
            throw new IllegalArgumentException("null");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "putSystemContextItem");
    }

    /* ************************************************************************* */
    /* Implementation of MatchSpaceKey interface */
    /* ************************************************************************* */

    /*
     * Evaluate the message field determined by the given Identifier
     * See the following method for more information.
     * 
     * Javadoc description supplied by MatchSpaceKey interface.
     */
    @Override
    public Object getIdentifierValue(Identifier id,
                                     boolean ignoreType)
                    throws BadMessageFormatMatchingException {
        return getIdentifierValue(id, ignoreType, null, false);
    }

    /*
     * Evaluate the message field determined by the given Identifier
     * 
     * Usually the field is either an SI or JMS header field or an SI, JMS or user
     * property, but SIB0136 introduces selection using XPATH expressions against
     * the message payload.
     * 
     * Javadoc description supplied by MatchSpaceKey interface.
     * 
     * For XPath selection, the value returned is a Boolean or a List of Nodes,
     * depending on the value of returnList. The contextValue parameter allows the
     * base node for an XPATH evaluation to be passed in.
     * 
     * For 'normal' selection, the value returned is null if there is no such field
     * or if ignoreType==false and the field was not of the expected type.
     * Otherwise the value returned is:
     * A String if the field is a String
     * A Boolean if the field is a Boolean
     * A Number if the field is any numeric type
     * A byte array if the field was originally an arbitrary Serializable object
     * 
     * @exception BadMessageFormatMatchingException when the method is unable to determine a
     * value because the message (or other object) from which the value must be extracted is
     * corrupted or ill-formed.
     */
    @Override
    public Object getIdentifierValue(Identifier id,
                                     boolean ignoreType,
                                     Object contextValue,
                                     boolean returnList)
                    throws BadMessageFormatMatchingException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getIdentifierValue", new Object[] { id, ignoreType, contextValue, returnList });

        Object value = null;
        boolean dontTraceNonNullValue = false;

        /* Determine the messaging domain: JMS or SI Core message */
        SelectorDomain domain = SelectorDomain.getSelectorDomain(id.getSelectorDomain());

        // If the domain is XPATH1 ....
        if (domain == SelectorDomain.XPATH1) {

            try {

                // Check any contextValue is actually a Node, we will catch, FFDC & wrap it below
                if (contextValue != null && !(contextValue instanceof Node)) {
                    throw new IllegalArgumentException("contextValue must be a Node");
                }

                XPathExpression xrep = (XPathExpression) id.getCompiledExpression();

                // If a list of nodes is required.........
                if (returnList) {
                    if (xrep != null) {

                        // If the contextValue is null, use payload document as the root
                        if (contextValue == null) {
                            contextValue = getPayloadDocument();
                        }

                        if (contextValue != null) {
                            NodeList nodeList = (NodeList) xrep.evaluate(contextValue, XPathConstants.NODESET);

                            // Now we have to turn the NodeList (if we have one) into a real List to return
                            if ((nodeList != null) && (nodeList.getLength() > 0)) {
                                value = new ArrayList(nodeList.getLength());
                                for (int i = 0; i < nodeList.getLength(); i++) {
                                    ((ArrayList) value).add(nodeList.item(i));
                                }
                            }
                            // if the nodeList is null or empty, we just drop through to return value=null
                        }
                        // if contextValue is still null, we just drop through to return value=null
                    }
                    // if xrep == null, we just drop through to return value=null
                }

                // ... if not, we just need to return a Boolean, which is easier
                else {
                    if (xrep != null) {

                        // If the contextValue is null, use payload document as the root
                        if (contextValue == null) {
                            contextValue = getPayloadDocument();
                        }

                        if (contextValue != null) {
                            value = xrep.evaluate(contextValue, XPathConstants.BOOLEAN);
                        }
                        // Return FALSE if the the contextValue is still null
                        else {
                            value = Boolean.FALSE;
                        }
                    }
                    else {
                        // Return FALSE if the XPath expression is null
                        value = Boolean.FALSE;
                    }
                }

            } catch (Exception e) {
                FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsApiMessageImpl.getIdentifierValue", "795");
                throw new BadMessageFormatMatchingException(e);
            }

        }

        // If the SelectorDomain is not XPATH1, we must be selecting on a named property
        else {

            /* Get the name of the identifier. */
            String fieldName = id.getName();

            /* Decide if the fieldName means we shouldn't trace non-null results */
            dontTraceNonNullValue = PasswordUtils.containsPassword(fieldName);

            /* Get the value of the property: */
            /* If we're processing on behalf of the Core SPI, the property can be a */
            /* JMS property (starting "JMS") or an SI Core property (starting "SI_") */
            /* or a user property. */
            /* If we're processing a JMS message however we can have JMS properties */
            /* but _not_ SI Core properties; anything else must be a user property, */
            /* but only user properties of types supported by JMS are 'found'. */

            /* If fieldName starts with JMS check for the JMS headers & properties */
            if (fieldName.startsWith(JMS_PREFIX)) {

                /* If it isn't one of the properties we don't match on, get the value */
                if (!(fieldName.equals(SIProperties.JMSDestination))
                    && !(fieldName.equals(SIProperties.JMSReplyTo))
                    && !(fieldName.equals(SIProperties.JMS_IBM_ExceptionMessage))
                    && !(fieldName.startsWith(JMS_IBM_MQMD_PREFIX))) {
                    value = getJMSSystemProperty(fieldName, true);
                }
            }

            /* If we're processing on behalf of JMS, look for a JMS-type user property */
            else if (domain == SelectorDomain.JMS) {
                /* Usually it would be in the JMS Property Map */
                if (mayHaveJmsUserProperties()) {
                    value = getJmsUserPropertyMap().get(fieldName);
                }
                /* If we haven't found the property, check for the Maelstrom wierdo */
                if ((value == null)
                    && (fieldName.equals(MfpConstants.PRP_TRANSVER))) {
                    value = getTransportVersion();
                }
            }

            /* If we're processing on behalf of the Core SPI.... */
            else if (domain == SelectorDomain.SIMESSAGE) {

                /* If fieldName starts with SI_ it must be a header field */
                if (fieldName.startsWith(SI_PREFIX)) {
                    /* If it isn't one of the properties we don't match on, get the value */
                    if (!fieldName.equals(SIProperties.SI_ExceptionInserts)) {
                        value = getSIProperty(fieldName, true);
                    }
                }

                /* Otherwise it should start with "user." */
                else if (fieldName.startsWith(USER_PREFIX)) {

                    String remainder = fieldName.substring(USER_PREFIX_LENGTH);
                    try {
                        value = getUserProperty(remainder, true);
                    } catch (Exception e) {
                        /* No Exception can be thrown as getUserProperty was called with */
                        /* forMatching set to true. */
                        // No FFDC code needed.
                    }
                }

                /* Otherwise, value remains null as no other fieldName is supported. */

            }

            /* If the value existed, check it is the right type (if we care) */
            if ((value != null) && (!ignoreType)) {
                value = typeCheck(value, id.getType());
            }

        } // End of not-XPATH

        /* Value may be null after running typeCheck, even if it wasn't before! */
        if (value == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getIdentifierValue", null);
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getIdentifierValue", new Object[] { PasswordUtils.replacePossiblePassword(dontTraceNonNullValue, value), value.getClass() });
        }

        return value;
    }

    /*
     * Provided for use in XPath support where the MatchSpace calls MFP
     * in order to retrieve the top most Node in a DOM tree.
     * 
     * Javadoc description supplied by MatchSpaceKey interface.
     */
    @Override
    public Object getRootContext() throws BadMessageFormatMatchingException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getRootContext");

        // Just call the subclass's method to obtain the DOM Document which
        // represents the payload.
        Object result;
        try {
            result = getPayloadDocument();
        } catch (Exception e) {
            FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.impl.JsApiMessageImpl.getRootContext", "921");
            result = null;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getRootContext", result);
        return result;
    }

    /**
     * Check the type of the value obtained from the message.
     * 
     * The value is returned as is if it is of the correct
     * type, otherwise null is returned.
     * 
     * @param value The Object value retrieved from the message
     * @param type The expected SelectorType of the value.
     * 
     * @return Object Either the original Object value, or null if the type of
     *         value was not correct.
     */
    private final Object typeCheck(Object value, int type) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "typecheck: value = " + value + ", " + value.getClass() + " , type=" + type);

        switch (type) {

        /* No checking, just return the value */
            case Selector.UNKNOWN:
                return value;

                /* STRING - simple! */
            case Selector.STRING:
                return (value instanceof String) ? value : null;

                /* BOOLEAN - Boolean or BooleanValue are suitable */
            case Selector.BOOLEAN:
                return (value instanceof Boolean) ? value : null;

                /* Any numeric - any Number or a NumericValue are suitable */
            default:
                return (value instanceof Number) ? value : null;
        }
    }

    /* ************************************************************************* */
    /* Private & package methods for accessing & updating property maps in JMF */
    /* ************************************************************************* */

    /**
     * Maps used by the main Message Property & SystemContext methods to access the
     * Property items:
     * jmsSystemPropertyMap contains the non-smoke-and-mirrors JMSX & JMS_IBM_ properties
     * jmsUserPropertyMap contains the user properties valid for the JMS API
     * otherUserPropertyMap contains any other user properties.
     * systemContextMap contains any system context (used by & stack products)
     * mqMdPropertiesMap contains any explicitly set JMS_IBM_MQMD_ properties
     */
    private transient JsMsgMap jmsSystemPropertyMap = null;
    private transient JsMsgMap jmsUserPropertyMap = null;
    private transient JsMsgMap otherUserPropertyMap = null;
    private transient JsMsgMap systemContextMap = null;
    private transient JsMsgMap mqMdSetPropertiesMap = null;

    /**
     * Helper method used by the main Message Property methods to obtain the
     * non-smoke-and-mirrors 'System; Property items in the form of a map.
     * <p>
     * The method has package level visibility as it is used by JsJmsMessageImpl
     * and JsSdoMessageimpl.
     * 
     * @return A JsMsgMap containing the Message Property name-value pairs.
     */
    final JsMsgMap getJmsSystemPropertyMap() {
        if (jmsSystemPropertyMap == null) {
            List<String> keys = (List<String>) getApi().getField(JsApiAccess.SYSTEMPROPERTY_NAME);
            List<Object> values = (List<Object>) getApi().getField(JsApiAccess.SYSTEMPROPERTY_VALUE);
            jmsSystemPropertyMap = new JsMsgMap(keys, values);
        }
        return jmsSystemPropertyMap;
    }

    /**
     * mayHaveMappedJmsSystemProperties
     * A helper method to determine whether there could be any JMS System properties
     * in the map without creating lists & maps unnecessarily.
     * If the transient map is be null AND there is a JMF EMPTY list in the JMF
     * message, then we know there are NO properties. Otherwise there MAY be properties.
     * It is possible for this to return true, but not to find any properties in
     * as they could have been deleted, or it may be a lfattened or MQ message.
     * 
     * @return boolean True if there may be mapped JMS System properties
     */
    final boolean mayHaveMappedJmsSystemProperties() {
        if ((jmsSystemPropertyMap != null)
            || (getApi().isNotEMPTYlist(JsApiAccess.SYSTEMPROPERTY_NAME))) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Helper method used by the main Message Property methods to obtain the
     * JMS-valid Property items in the form of a map.
     * <p>
     * The method has package level visibility as it is used by JsJmsMessageImpl
     * and JsSdoMessageimpl.
     * 
     * @return A JsMsgMap containing the Message Property name-value pairs.
     */
    final JsMsgMap getJmsUserPropertyMap() {
        if (jmsUserPropertyMap == null) {
            List<String> keys = (List<String>) getApi().getField(JsApiAccess.JMSPROPERTY_NAME);
            List<Object> values = (List<Object>) getApi().getField(JsApiAccess.JMSPROPERTY_VALUE);
            jmsUserPropertyMap = new JsMsgMap(keys, values);
        }
        return jmsUserPropertyMap;
    }

    /**
     * mayHaveJmsUserProperties
     * A helper method to determine whether there could be any JMS User properties
     * in the map without creating lists & maps unnecessarily.
     * If the transient map is be null AND there is a JMF EMPTY list in the JMF
     * message, then we know there are NO properties. Otherwise there MAY be properties.
     * It is possible for this to return true, but not to find any properties in
     * as they could have been deleted, or it may be a lfattened or MQ message.
     * 
     * @return boolean True if there may be JMS User properties
     */
    final boolean mayHaveJmsUserProperties() {
        if ((jmsUserPropertyMap != null)
            || (getApi().isNotEMPTYlist(JsApiAccess.JMSPROPERTY_NAME))) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Helper method used by the main Message Property methods to obtain the
     * non-JMS-valid Property items in the form of a map.
     * <p>
     * The method has package level visibility as it is used by JsJmsMessageImpl,
     * JsSdoMessageimpl and MQJsJmsMessageEncoderImpl.
     * 
     * @return A JsMsgMap containing the Message Property name-value pairs.
     */
    final JsMsgMap getOtherUserPropertyMap() {
        if (otherUserPropertyMap == null) {
            List<String> keys = (List<String>) getApi().getField(JsApiAccess.OTHERPROPERTY_NAME);
            List<Object> values = (List<Object>) getApi().getField(JsApiAccess.OTHERPROPERTY_VALUE);
            otherUserPropertyMap = new JsMsgMap(keys, values);
        }
        return otherUserPropertyMap;
    }

    /**
     * mayHaveOtherUserProperties
     * A helper method to determine whether there could be any Other User properties
     * in the map without creating lists & maps unnecessarily.
     * If the transient map is be null AND there is a JMF EMPTY list in the JMF
     * message, then we know there are NO properties. Otherwise there MAY be properties.
     * It is possible for this to return true, but not to find any properties in
     * as they could have been deleted, or it may be a lfattened or MQ message.
     * 
     * @return boolean True if there may be Other User properties
     */
    final boolean mayHaveOtherUserProperties() {
        if ((otherUserPropertyMap != null)
            || (getApi().isNotEMPTYlist(JsApiAccess.OTHERPROPERTY_NAME))) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Helper method used by the System Context methods to obtain the
     * System Context items in the form of a map.
     * <p>
     * The method has package level visibility as it is used by
     * MQJsJmsMessageEncoderImpl.
     * 
     * @return A JsMsgMap containing the Message Property name-value pairs.
     */
    final JsMsgMap getSystemContextMap() {
        if (systemContextMap == null) {
            List<String> keys = (List<String>) getApi().getField(JsApiAccess.SYSTEMCONTEXT_NAME);
            List<Object> values = (List<Object>) getApi().getField(JsApiAccess.SYSTEMCONTEXT_VALUE);
            systemContextMap = new JsMsgMap(keys, values);
        }
        return systemContextMap;
    }

    /**
     * mayHaveSystemContext
     * A helper method to determine whether there could be any System Context
     * in the map without creating lists & maps unnecessarily.
     * If the transient map is be null AND there is a JMF EMPTY list in the JMF
     * message, then we know there are NO properties. Otherwise there MAY be System Context.
     * It is possible for this to return true, but not to find any properties in
     * as they could have been deleted, or it may be a lfattened or MQ message.
     * 
     * @return boolean True if there may be System Context
     */
    final boolean mayHaveSystemContext() {
        if ((systemContextMap != null)
            || (getApi().isNotEMPTYlist(JsApiAccess.SYSTEMCONTEXT_NAME))) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Helper method used by the main Message Property methods to obtain any
     * JMS_IBM_MQMD_ Properties explicitly set, in the form of a map.
     * <p>
     * The method has package level visibility as it is used by JsJmsMessageImpl
     * and JsSdoMessageimpl.
     * 
     * @return A JsMsgMap containing the Message Property name-value pairs.
     */
    final JsMsgMap getMQMDSetPropertiesMap() {
        if (mqMdSetPropertiesMap == null) {
            // There will not usually be any set MQMD properties, so the JMF choice defaults to
            // to empty (and will definitely be empty if the message has arrived from a
            // pre-v7 WAS). This is different from the other maps which default to empty lists.
            if (getHdr2().getChoiceField(JsHdr2Access.MQMDPROPERTIES) == JsHdr2Access.IS_MQMDPROPERTIES_MAP) {
                List<String> keys = (List<String>) getHdr2().getField(JsHdr2Access.MQMDPROPERTIES_MAP_NAME);
                List<Object> values = (List<Object>) getHdr2().getField(JsHdr2Access.MQMDPROPERTIES_MAP_VALUE);
                mqMdSetPropertiesMap = new JsMsgMap(keys, values);
            }
            else {
                mqMdSetPropertiesMap = new JsMsgMap(null, null);
            }
        }
        return mqMdSetPropertiesMap;
    }

    /**
     * Helper method used to determine whether any MQMD Properties have been set into
     * the message.
     * <p>
     * The method has package level visibility as it is used by JsSdoMessageImpl.
     * 
     * @return boolean True if there any explicitly set JMS_IBM_MQMD properties,
     *         otherwise false.
     */
    final boolean hasMQMDPropertiesSet() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "hasMQMDPropertiesSet");
        boolean set = false;
        // If we have a fluffed up map, check it for size
        if (mqMdSetPropertiesMap != null) {
            if (mqMdSetPropertiesMap.size() > 0) {
                set = true;
            }
        }
        // Otherwise, see if the variant is set
        else if (getHdr2().getChoiceField(JsHdr2Access.MQMDPROPERTIES) == JsHdr2Access.IS_MQMDPROPERTIES_MAP) {
            set = true;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "hasMQMDPropertiesSet", set);
        return set;
    }

    /**
     * Helper method used by the JMO to rewrite Property data into the
     * underlying JMF message.
     * Package level visibility as used by the JMO.
     * 
     * @param why The reason for the update
     * @see com.ibm.ws.sib.mfp.MfpConstants
     */
    @Override
    void updateDataFields(int why) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "updateDataFields");
        super.updateDataFields(why);
        if (jmsSystemPropertyMap != null && jmsSystemPropertyMap.isChanged()) {
            getApi().setField(JsApiAccess.SYSTEMPROPERTY_NAME, jmsSystemPropertyMap.getKeyList());
            getApi().setField(JsApiAccess.SYSTEMPROPERTY_VALUE, jmsSystemPropertyMap.getValueList());
            jmsSystemPropertyMap.setUnChanged(); // d317373.1
        }
        if (jmsUserPropertyMap != null && jmsUserPropertyMap.isChanged()) {
            getApi().setField(JsApiAccess.JMSPROPERTY_NAME, jmsUserPropertyMap.getKeyList());
            getApi().setField(JsApiAccess.JMSPROPERTY_VALUE, jmsUserPropertyMap.getValueList());
            jmsUserPropertyMap.setUnChanged(); // d317373.1
        }
        if (otherUserPropertyMap != null && otherUserPropertyMap.isChanged()) {
            getApi().setField(JsApiAccess.OTHERPROPERTY_NAME, otherUserPropertyMap.getKeyList());
            getApi().setField(JsApiAccess.OTHERPROPERTY_VALUE, otherUserPropertyMap.getValueList());
            otherUserPropertyMap.setUnChanged(); // d317373.1
        }
        if (systemContextMap != null && systemContextMap.isChanged()) {
            getApi().setField(JsApiAccess.SYSTEMCONTEXT_NAME, systemContextMap.getKeyList());
            getApi().setField(JsApiAccess.SYSTEMCONTEXT_VALUE, systemContextMap.getValueList());
            systemContextMap.setUnChanged(); // d317373.1
        }
        // Slightly different, as we need to set the variant back to empty if these properties have been cleared
        if (mqMdSetPropertiesMap != null && mqMdSetPropertiesMap.isChanged()) {
            if (mqMdSetPropertiesMap.size() > 0) {
                getHdr2().setField(JsHdr2Access.MQMDPROPERTIES_MAP_NAME, mqMdSetPropertiesMap.getKeyList());
                getHdr2().setField(JsHdr2Access.MQMDPROPERTIES_MAP_VALUE, mqMdSetPropertiesMap.getValueList());
            }
            else {
                getHdr2().setChoiceField(JsHdr2Access.MQMDPROPERTIES, JsHdr2Access.IS_MQMDPROPERTIES_EMPTY);
            }
            mqMdSetPropertiesMap.setUnChanged();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "updateDataFields");
    }

    /* ************************************************************************* */
    /* Private & package methods for the support of Properties */
    /* ************************************************************************* */

    /**
     * Return the User Property stored in the Message under the given name.
     * <p>
     * User Properties are stored as name-value pairs where the value may be any
     * Object which implements java.io.Serializable.
     * If the forMatching parameter is true:
     * a) A serialized object is returned in it's derialized form as a byte array.
     * b) A byte array is returned exactly as is - the header bytes don't matter
     * as matching is only on == null or != null.
     * If forMatching is false:
     * a) for a Serialized property, the deserialized object is returned.
     * b) for any other byte array, a copy of the byte array is returned.
     * <p>
     * The method has package level visibility as it is called by JsSdoMessageImpl.
     * 
     * @param name The name of the Property to be returned.
     * @param forMatching Indicates what the property will be used for (i.e. matching or getting)
     * 
     * @return Serializable A reference to the Message Property.
     *         Null is returned if there is no such item.
     * 
     * @exception IOException if the system context item could not be de-serialized
     * @exception ClassNotFoundException if the system context items class could not be found.
     */
    final Serializable getUserProperty(String name, boolean forMatching)
                    throws IOException, ClassNotFoundException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getUserProperty", new Object[] { name, Boolean.valueOf(forMatching) });

        Serializable item = null;

        /* First see if Maelstrom's transportVersion property is wanted & set */
        /* in its own field. The name is never null as outer methods check it. */
        if (name.equals(MfpConstants.PRP_TRANSVER) && isTransportVersionSet()) {
            item = (Serializable) getTransportVersion();
        }

        /* Otherwise, try getting the property from the JMS-valid Property Map */
        else {
            if (mayHaveJmsUserProperties()) {
                item = (Serializable) getJmsUserPropertyMap().get(name);
            }
        }

        /* If the item was there, we don't need to do anything else has it is */
        /* already a suitable Object for returning. */
        if (item != null) {
        }

        /* Try getting the property from the other Property Map */
        else {
            if (mayHaveOtherUserProperties()) {
                item = (Serializable) getOtherUserPropertyMap().get(name);
            }

            /* If the item is null (i.e. no such property) just return it. */
            /* If forMatching is true, we can return whatever we have as is. */
            /* If is is not a byte array it must already be a proper value - this */
            /* can't be the case at the moment, but, once WAS 6 is no longer */
            /* supported we can put the JMS-supported-type properties with dodgy */
            /* names into the OtherUserPropertyMap without flattening them. d392521 */
            if ((item == null) || forMatching || !(item instanceof byte[])) {
            }

            /* Otherwise, it is a byte array of some sort & needs some work done. */
            else {
                item = restoreMapObject((byte[]) item);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getUserProperty", PasswordUtils.replaceValueIfKeyIsPassword(name, item));
        return item;
    }

    /**
     * Return the value of the given SI_ Message Property.
     * <p>
     * This method supports only those SI_ properties which are not header
     * fields accessible by SIMessage/SIBusSdoMessage get methods.
     * <p>
     * The method has package level visibility as it is called by JsSdoMessageImpl.
     * 
     * @param name The name of the Property to be returned.
     * @param forMatching Indicates what the property will be used for (i.e. matching or getting)
     *            If forMatching:
     *            a) the message header properties are supported
     *            b) no Exceptions can be thrown
     *            c) some properties may use different algorithms
     * 
     * @return Serializable A reference to the Message Property.
     *         Null is returned if there is no such item.
     * 
     */
    final Serializable getSIProperty(String name, boolean forMatching) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getSIProperty", name);

        Serializable value = null;
        int count;

        /*------------------------------------------------------------------------*/
        /* First check for the SI_Report properties */
        /*------------------------------------------------------------------------*/
        if (name.regionMatches(SI_PREFIX_LENGTH, SI_REPORT, 0, SI_REPORT_LENGTH)) {

            count = name.length() - SI_REPORT_OFFSET;

            if (name.regionMatches(SI_REPORT_OFFSET, SIProperties.SI_ReportExpiration, SI_REPORT_OFFSET, count)) {
                value = getReportExpiry();
            }
            else if (name.regionMatches(SI_REPORT_OFFSET, SIProperties.SI_ReportException, SI_REPORT_OFFSET, count)) {
                value = getReportException();
            }
            else if (name.regionMatches(SI_REPORT_OFFSET, SIProperties.SI_ReportCOA, SI_REPORT_OFFSET, count)) {
                value = getReportCOA();
            }
            else if (name.regionMatches(SI_REPORT_OFFSET, SIProperties.SI_ReportCOD, SI_REPORT_OFFSET, count)) {
                value = getReportCOD();
            }
            else if (name.regionMatches(SI_REPORT_OFFSET, SIProperties.SI_ReportPAN, SI_REPORT_OFFSET, count)) {
                value = getReportPAN();
            }
            else if (name.regionMatches(SI_REPORT_OFFSET, SIProperties.SI_ReportNAN, SI_REPORT_OFFSET, count)) {
                value = getReportNAN();
            }
            else if (name.regionMatches(SI_REPORT_OFFSET, SIProperties.SI_ReportPassMsgID, SI_REPORT_OFFSET, count)) {
                value = getReportPassMsgId();
            }
            else if (name.regionMatches(SI_REPORT_OFFSET, SIProperties.SI_ReportPassCorrelID, SI_REPORT_OFFSET, count)) {
                value = getReportPassCorrelId();
            }
            else if (name.regionMatches(SI_REPORT_OFFSET, SIProperties.SI_ReportDiscardMsg, SI_REPORT_OFFSET, count)) {
                value = getReportDiscardMsg();
            }
            else if (name.regionMatches(SI_REPORT_OFFSET, SIProperties.SI_ReportFeedback, SI_REPORT_OFFSET, count)) {
                value = getReportFeedback();
            }
            else {
                if (!forMatching) {
                    throw new IllegalArgumentException(name);
                }
            }
        }

        /*------------------------------------------------------------------------*/
        /* Then try the SI_Exception properties */
        /*------------------------------------------------------------------------*/
        else if (name.regionMatches(SI_PREFIX_LENGTH, SI_EXCEPTION, 0, SI_EXCEPTION_LENGTH)) {

            count = name.length() - SI_EXCEPTION_OFFSET;

            if (name.regionMatches(SI_EXCEPTION_OFFSET, SIProperties.SI_ExceptionReason, SI_EXCEPTION_OFFSET, count)) {
                value = getExceptionReason();
            }
            else if (name.regionMatches(SI_EXCEPTION_OFFSET, SIProperties.SI_ExceptionInserts, SI_EXCEPTION_OFFSET, count)) {
                value = getExceptionInserts();
            }
            else if (name.regionMatches(SI_EXCEPTION_OFFSET, SIProperties.SI_ExceptionTimestamp, SI_EXCEPTION_OFFSET, count)) {
                value = getExceptionTimestamp();
            }
            else if (name.regionMatches(SI_EXCEPTION_OFFSET, SIProperties.SI_ExceptionProblemDestination, SI_EXCEPTION_OFFSET, count)) {
                value = getExceptionProblemDestination();
            }
            else if (name.regionMatches(SI_EXCEPTION_OFFSET, SIProperties.SI_ExceptionProblemSubscription, SI_EXCEPTION_OFFSET, count)) {
                value = getExceptionProblemSubscription();
            }
            else {
                if (!forMatching) {
                    throw new IllegalArgumentException(name);
                }
            }

        }

        /*------------------------------------------------------------------------*/
        /* Only if forMatching is true, now try all the message header properties */
        /*------------------------------------------------------------------------*/
        else if (forMatching) {

            count = name.length() - SI_PREFIX_LENGTH;

            if (name.regionMatches(SI_PREFIX_LENGTH, SIProperties.SI_NextDestination, SI_PREFIX_LENGTH, count)) {
                List<SIDestinationAddress> frp = getForwardRoutingPath();
                if ((frp != null) && (frp.size() > 0)) {
                    value = frp.get(0).getDestinationName();
                }
            }
            else if (name.regionMatches(SI_PREFIX_LENGTH, SIProperties.SI_SystemMessageID, SI_PREFIX_LENGTH, count)) {
                value = getSystemMessageId();
            }
            else if (name.regionMatches(SI_PREFIX_LENGTH, SIProperties.SI_Reliability, SI_PREFIX_LENGTH, count)) {
                value = getReliability().toString();
            }
            else if (name.regionMatches(SI_PREFIX_LENGTH, SIProperties.SI_Priority, SI_PREFIX_LENGTH, count)) {
                value = getPriority();
            }
            else if (name.regionMatches(SI_PREFIX_LENGTH, SIProperties.SI_TimeToLive, SI_PREFIX_LENGTH, count)) {
                value = getTimeToLive();
            }
            else if (name.regionMatches(SI_PREFIX_LENGTH, SIProperties.SI_Discriminator, SI_PREFIX_LENGTH, count)) {
                value = getDiscriminator();
            }
            else if (name.regionMatches(SI_PREFIX_LENGTH, SIProperties.SI_ReplyReliability, SI_PREFIX_LENGTH, count)) {
                Reliability r = getReplyReliability();
                value = (r == null) ? null : r.toString();
            }
            else if (name.regionMatches(SI_PREFIX_LENGTH, SIProperties.SI_ReplyPriority, SI_PREFIX_LENGTH, count)) {
                value = getReplyPriority();
            }
            else if (name.regionMatches(SI_PREFIX_LENGTH, SIProperties.SI_ReplyTimeToLive, SI_PREFIX_LENGTH, count)) {
                value = getReplyTimeToLive();
            }
            else if (name.regionMatches(SI_PREFIX_LENGTH, SIProperties.SI_ReplyDiscriminator, SI_PREFIX_LENGTH, count)) {
                value = getReplyDiscriminator();
            }
            // For matching, SI_RedeliveredCount is based on deliveryCount rather than the real message field
            else if (name.regionMatches(SI_PREFIX_LENGTH, SIProperties.SI_RedeliveredCount, SI_PREFIX_LENGTH, count)) {
                value = deliveryCount;
            }
            else if (name.regionMatches(SI_PREFIX_LENGTH, SIProperties.SI_MessageID, SI_PREFIX_LENGTH, count)) {
                value = getApiMessageId();
            }
            else if (name.regionMatches(SI_PREFIX_LENGTH, SIProperties.SI_CorrelationID, SI_PREFIX_LENGTH, count)) {
                value = getCorrelationId();
            }
            else if (name.regionMatches(SI_PREFIX_LENGTH, SIProperties.SI_UserID, SI_PREFIX_LENGTH, count)) {
                value = getUserid();
            }
            else if (name.regionMatches(SI_PREFIX_LENGTH, SIProperties.SI_Format, SI_PREFIX_LENGTH, count)) {
                value = getFormat();
            }
            else if (name.regionMatches(SI_PREFIX_LENGTH, SIProperties.SI_DeliveryDelay, SI_PREFIX_LENGTH, count)) {
                value = getDeliveryDelay();
            }

        }

        /*------------------------------------------------------------------------*/
        /* If not forMatching & neither Report or Exception it must be an error */
        /*------------------------------------------------------------------------*/
        else {
            throw new IllegalArgumentException(name);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getSIProperty", value);
        return value;
    }

    /**
     * Return the value of the given JMS Message Property.
     * <p>
     * For this method, JMS Message Properties include those which are strictly
     * JMS Header fields, as they are treated as properties for Message
     * Selection and for SIMessage.getMessageProperty().
     * All JMS properties are supported by this method - if a caller wants to
     * veto access of some properties then the caller must provide that code.
     * <p>
     * Some JMS properties map to header fields, some are extrapolated from
     * header fields, and some are stored directly in the system property map.
     * <p>
     * The method has package level visibility as it is called by JsJmsMessageImpl
     * and JsSdoMessageImpl.
     * 
     * @param name The name of the Property to be returned.
     * @param forMatching Indicates what the property will be used for (i.e. matching or getting)
     *            If forMatching:
     *            a) the byte array properties do not need
     *            to be copied as they will not be updated.
     *            b) some properties may use different algorithms
     * @return Serializable A reference to the Message Property.
     *         Null is returned if there is no such item.
     */
    final Serializable getJMSSystemProperty(String name, boolean forMatching) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJMSSystemProperty", name);

        Serializable value = null;
        int count;

        /*------------------------------------------------------------------------*/
        /* If it is a JMSX property.... */
        /*------------------------------------------------------------------------*/
        if (name.charAt(JMS_LENGTH) == JMSX_EXTRA_PREFIX) {
            count = name.length() - JMSX_LENGTH;

            if (name.regionMatches(JMSX_LENGTH, SIProperties.JMSXDeliveryCount, JMSX_LENGTH, count)) {
                // For matching, JMSXDeliveryCount is based on deliveryCount rather than the real message field
                if (forMatching) {
                    value = deliveryCount + 1;
                }
                else {
                    value = Integer.valueOf(getJmsxDeliveryCount());
                }
            }
            else if (name.regionMatches(JMSX_LENGTH, SIProperties.JMSXAppID, JMSX_LENGTH, count)) {
                value = getJmsxAppId();
            }
            else if (name.regionMatches(JMSX_LENGTH, SIProperties.JMSXUserID, JMSX_LENGTH, count)) {
                value = getUserid();
            }

            /* The other supported JMSX properties just live in the system map */
            else {
                if (mayHaveMappedJmsSystemProperties()) {
                    value = (Serializable) getJmsSystemPropertyMap().get(name);
                }
            }
        }

        /*------------------------------------------------------------------------*/
        /* If it is a JMS_IBM_ property.... */
        /*------------------------------------------------------------------------*/
        else if (name.startsWith(JMS_IBM_EXTRA_PREFIX, JMS_LENGTH)) {

            /* First check for the Report ones */
            if (name.regionMatches(JMS_IBM_LENGTH, REPORT, 0, REPORT_LENGTH)) {

                count = name.length() - REPORT_OFFSET;

                if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_Expiration, REPORT_OFFSET, count)) {
                    value = getReportExpiry();
                }
                else if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_Exception, REPORT_OFFSET, count)) {
                    value = getReportException();
                }
                else if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_COA, REPORT_OFFSET, count)) {
                    value = getReportCOA();
                }
                else if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_COD, REPORT_OFFSET, count)) {
                    value = getReportCOD();
                }
                else if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_PAN, REPORT_OFFSET, count)) {
                    value = getReportPAN();
                }
                else if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_NAN, REPORT_OFFSET, count)) {
                    value = getReportNAN();
                }
                else if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_Pass_Msg_ID, REPORT_OFFSET, count)) {
                    value = getReportPassMsgId();
                }
                else if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_Pass_Correl_ID, REPORT_OFFSET, count)) {
                    value = getReportPassCorrelId();
                }
                else if (name.regionMatches(REPORT_OFFSET, SIProperties.JMS_IBM_Report_Discard_Msg, REPORT_OFFSET, count)) {
                    value = getReportDiscardMsg();
                }
            }

            else {
                /* Then try the other smoke-and-mirrors ones */
                count = name.length() - JMS_IBM_LENGTH;

                if (name.regionMatches(JMS_IBM_LENGTH, SIProperties.JMS_IBM_ExceptionReason, JMS_IBM_LENGTH, count)) {
                    value = getExceptionReason();
                }
                else if (name.regionMatches(JMS_IBM_LENGTH, SIProperties.JMS_IBM_ExceptionTimestamp, JMS_IBM_LENGTH, count)) {
                    value = getExceptionTimestamp();
                }
                else if (name.regionMatches(JMS_IBM_LENGTH, SIProperties.JMS_IBM_ExceptionProblemDestination, JMS_IBM_LENGTH, count)) {
                    value = getExceptionProblemDestination();
                }
                else if (name.regionMatches(JMS_IBM_LENGTH, SIProperties.JMS_IBM_ExceptionProblemSubscription, JMS_IBM_LENGTH, count)) {
                    value = getExceptionProblemSubscription();
                }
                else if (name.regionMatches(JMS_IBM_LENGTH, SIProperties.JMS_IBM_ExceptionMessage, JMS_IBM_LENGTH, count)) {
                    value = getExceptionMessage();
                }
                else if (name.regionMatches(JMS_IBM_LENGTH, SIProperties.JMS_IBM_Feedback, JMS_IBM_LENGTH, count)) {
                    value = getReportFeedback();
                }
                else if (name.regionMatches(JMS_IBM_LENGTH, SIProperties.JMS_IBM_System_MessageID, JMS_IBM_LENGTH, count)) {
                    value = getSystemMessageId();
                }
                else if (name.regionMatches(JMS_IBM_LENGTH, SIProperties.JMS_IBM_ArmCorrelator, JMS_IBM_LENGTH, count)) {
                    value = getARMCorrelator();
                }

                /* The other supported JMS_IBM_ properties just live in the system map */
                else {
                    if (mayHaveMappedJmsSystemProperties()) {
                        value = (Serializable) getJmsSystemPropertyMap().get(name);
                    }
                }
            }

        }

        /*------------------------------------------------------------------------------------*/
        /* If neither JMSX nor JMS_IBM_ then it must be a JMS header, or JMS_TOG_ARM_Correlator or not exist */
        /*------------------------------------------------------------------------------------*/
        else {
            count = name.length() - JMS_LENGTH;

            if (name.regionMatches(JMS_LENGTH, SIProperties.JMSDestination, JMS_LENGTH, count)) {
                value = getJmsDestination();
                if ((value != null) && (!forMatching)) {
                    byte[] copy = new byte[((byte[]) value).length];
                    System.arraycopy(value, 0, copy, 0, ((byte[]) value).length);
                    value = copy;
                }
            }
            else if (name.regionMatches(JMS_LENGTH, SIProperties.JMSDeliveryMode, JMS_LENGTH, count)) {
                value = getJmsDeliveryMode().toString();
            }
            else if (name.regionMatches(JMS_LENGTH, SIProperties.JMSMessageID, JMS_LENGTH, count)) {
                value = getApiMessageId();
            }
            else if (name.regionMatches(JMS_LENGTH, SIProperties.JMSTimestamp, JMS_LENGTH, count)) {
                value = getTimestamp();
            }
            else if (name.regionMatches(JMS_LENGTH, SIProperties.JMSExpiration, JMS_LENGTH, count)) {
                value = getJmsExpiration();
            }
            else if (name.regionMatches(JMS_LENGTH, SIProperties.JMSRedelivered, JMS_LENGTH, count)) {
                // For matching, JMSRedelivered is based on deliveryCount rather than the real message field
                if (forMatching) {
                    if (deliveryCount > 0) {
                        value = Boolean.TRUE;
                    }
                    else {
                        value = Boolean.FALSE;
                    }
                }
                // Otherwise we just call the normal getJmsRedelivered() method which uses the real message field
                else {
                    value = getJmsRedelivered();
                }
            }
            else if (name.regionMatches(JMS_LENGTH, SIProperties.JMSPriority, JMS_LENGTH, count)) {
                value = getPriority();
            }
            else if (name.regionMatches(JMS_LENGTH, SIProperties.JMSReplyTo, JMS_LENGTH, count)) {
                value = getJmsReplyTo();
                if ((value != null) && (!forMatching)) {
                    byte[] copy = new byte[((byte[]) value).length];
                    System.arraycopy(value, 0, copy, 0, ((byte[]) value).length);
                    value = copy;
                }
            }
            else if (name.regionMatches(JMS_LENGTH, SIProperties.JMSCorrelationID, JMS_LENGTH, count)) {
                value = getCorrelationId();
            }
            else if (name.regionMatches(JMS_LENGTH, SIProperties.JMSType, JMS_LENGTH, count)) {
                value = getJmsType();
            }
            else if (name.regionMatches(JMS_LENGTH, SIProperties.JMS_TOG_ARM_Correlator, JMS_LENGTH, count)) {
                value = getARMCorrelator();
            }
            else if (name.regionMatches(JMS_LENGTH, SIProperties.JMSDeliveryTime, JMS_LENGTH, count)) {
                value = getJmsDeliveryTime();
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJMSSystemProperty", value);
        return value;
    }

    /**
     * getMQMDProperty
     * Return the requested JMS_IBM_MQMD_ property, which may be in the table of explicitly
     * set properties, or may be in an underlying MQMD in the message itself.
     * <p>
     * The method has package level visibility as it is used by JsJmsMessageImpl
     * and JsSdoMessageImpl.
     * 
     * @param name The name of the property
     * 
     * @return Object The value of the requested property, or null if it does not exist.
     */
    final Object getMQMDProperty(String name) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMQMDProperty", name);

        Object result = null;

        // First check in the table of explictly set properties
        result = getMQMDSetPropertiesMap().get(name);

        // If we didn't find it there, look in the real JMS System Properties map
        // which will resolve through to the MQMD.
        if (result == null) {
            if (mayHaveMappedJmsSystemProperties()) {
                result = getJmsSystemPropertyMap().get(name);
            }
        }

        // If we get a byte[], we had better return a safe copy so the app can't mess
        // up the one in the message.
        if ((result != null) && (result.getClass() == byte[].class)) {
            // Would have used Arrays.copyOf(), but the Thin Client still supports Java 1.5
            result = copyOf((byte[]) result, ((byte[]) result).length);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMQMDProperty", result);
        return result;
    }

    /**
     * setMQMDProperty
     * Set the JMS_IBM_MQMD_ into the table of explicitly set properties.
     * <p>
     * The method has package level visibility as it is used by JsJmsMessageImpl
     * and JsSdoMessageImpl.
     * 
     * @param name The name of the property
     * @param value The new non-null value of the property
     */
    final void setMQMDProperty(String name, Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setMQMDProperty", new Object[] { name, value });

        // byte arrays aren't immutable so we need to take a copy of them. two
        // Both the ones we support need to be 24 bytes for MQ, so we take the opportunity
        // to pad or truncate if necessary.
        // Would have used Arrays.copyOf(), but the Thin Client still supports Java 1.5
        if ((value instanceof byte[])) {
            value = copyOf((byte[]) value, 24);
        }

        // Both of the Strings we support must not be longer than 48 characters, so we
        // truncate them if necessary.
        else if ((value instanceof String)) {
            if (((String) value).length() > 48) {
                value = (((String) value).substring(0, 48));
            }
        }

        // Now set it into the table of explicitly set properties.
        getMQMDSetPropertiesMap().put(name, value);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setMQMDProperty");
    }

    /**
     * deleteMQMDProperty
     * Delete the requested JMS_IBM_MQMD_ property from the table of explicitly set
     * properties, but NIT from any underlying MQMD.
     * <p>
     * The method has package level visibility as it is used by JsJmsMessageImpl
     * and JsSdoMessageImpl.
     * 
     * @param name The name of the property
     */
    final void deleteMQMDProperty(String name) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "deleteMQMDProperty", name);

        // Remove it from the table of explicitly set properties.
        getMQMDSetPropertiesMap().remove(name);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "deleteMQMDProperty");
    }

    /**
     * Clear all of the smoke-and-mirrors properties which are clearable.
     * Modifyable JMS Header fields (e.g. JMSType) are not affected.
     * <p>
     * The method has package level visibility as it is used by JsJmsMessageImpl
     * and JsSdoMessageImpl.
     */
    final void clearSmokeAndMirrorsProperties() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearSmokeAndMirrorsProperties");

        /* JMSXAppId */
        getHdr2().setChoiceField(JsHdr2Access.XAPPID, JsHdr2Access.IS_XAPPID_EMPTY);

        /* JMSXUserID */
        setUserid(null);

        /* JMSXDeliveryCount is not clearable */

        /* JMS_IBM_ExceptionXxxxx are not clearable */

        /* JMS_IBM_Feedback */
        getApi().setChoiceField(JsApiAccess.REPORTFEEDBACK, JsApiAccess.IS_REPORTFEEDBACK_UNSET);
        getApi().setChoiceField(JsApiAccess.REPORTFEEDBACKINT, JsApiAccess.IS_REPORTFEEDBACKINT_UNSET);

        /* JMS_IBM_ReportXxxxxx */
        getHdr2().setChoiceField(JsHdr2Access.REPORTEXPIRY, JsHdr2Access.IS_REPORTEXPIRY_UNSET);
        getHdr2().setChoiceField(JsHdr2Access.REPORTCOA, JsHdr2Access.IS_REPORTCOA_UNSET);
        getHdr2().setChoiceField(JsHdr2Access.REPORTCOD, JsHdr2Access.IS_REPORTCOD_UNSET);
        getApi().setChoiceField(JsApiAccess.REPORTEXCEPTION, JsApiAccess.IS_REPORTEXCEPTION_UNSET);
        getApi().setChoiceField(JsApiAccess.REPORTPAN, JsApiAccess.IS_REPORTPAN_UNSET);
        getApi().setChoiceField(JsApiAccess.REPORTNAN, JsApiAccess.IS_REPORTNAN_UNSET);
        getApi().setChoiceField(JsApiAccess.REPORTPASSMSGID, JsApiAccess.IS_REPORTPASSMSGID_UNSET);
        getApi().setChoiceField(JsApiAccess.REPORTPASSCORRELID, JsApiAccess.IS_REPORTPASSCORRELID_UNSET);
        getApi().setChoiceField(JsApiAccess.REPORTDISCARDMSG, JsApiAccess.IS_REPORTDISCARDMSG_UNSET);

        /* JMS_IBM_ArmCorrelator & JMS_TOG_ARM_Correlator */
        setARMCorrelator(null);

        /* transportVersion */
        getHdr2().setChoiceField(JsHdr2Access.TRANSPORTVERSION, JsHdr2Access.IS_TRANSPORTVERSION_EMPTY);

        // JMS_IBM_Character_Set and JMS_IBM_Encoding are not really smoke-and-mirrors
        // properties, but they do need to be cleared in the message itself too. d395685
        setCcsid(null);
        setEncoding(null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearSmokeAndMirrorsProperties");
    }

    /**
     * Flatten a Serializable Property or SystemContext item so that it can
     * be stored in a map. This also gives us the required safe copy.
     * <p>
     * The method has package level visibility as it is used by JsSdoMessageImpl.
     * 
     * @param item The Message Property or System Context item
     * 
     * @return A byte array suitable for storing in a Map.
     * 
     * @exception IOException if the item could not be serialized
     */
    final byte[] flattenMapObject(Serializable item) throws IOException {

        byte[] serializedItem = null;

        /* If it is a byte array, we need to copy it immediately and add a */
        /* header to distinguish it from a serialized object. */
        /* The copy is stored in the Other Property Map. */
        if (item instanceof byte[]) {
            serializedItem = new byte[((byte[]) item).length + 2];
            serializedItem[0] = HEADER_BYTE_0;
            serializedItem[1] = HEADER_BYTE_1;
            System.arraycopy(item, 0, serializedItem, 2, ((byte[]) item).length);
        }

        /* Otherwise, serialize it into a byte array and store that in the */
        /* Other Property Map. */
        else {

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bao);
            out.writeObject(item);
            out.flush();
            out.close();
            serializedItem = bao.toByteArray();
        }

        return serializedItem;
    }

    /**
     * Restore an item retrieved from a Property or SystemContext map as a byte
     * array into whatever it originally was.
     * 
     * For a Serialized property, the deserialized object is returned.
     * For any other byte array, a copy of the byte array is returned.
     * <p>
     * The method has package level visibility as it is used by JsSdoMessageImpl.
     * 
     * @param mapItemArray A byte array retrieved from the Map.
     * 
     * @return Serializable A reference to the Message Property or System Context item.
     * 
     * @exception IOException if the item could not be de-serialized
     * @exception ClassNotFoundException if the system context items class could not be found.
     */
    final Serializable restoreMapObject(byte[] mapItemArray) throws IOException, ClassNotFoundException {

        Serializable item = null;;

        /* If it is a real byte array, we need to return a safe copy with the */
        /* header bytes removed. */
        if ((mapItemArray[0] == HEADER_BYTE_0) && (mapItemArray[1] == HEADER_BYTE_1)) {
            item = new byte[mapItemArray.length - 2];
            System.arraycopy(mapItemArray, 2, item, 0, ((byte[]) item).length);
        }

        /* Anything else needs deserializing. */

        else {

            ByteArrayInputStream bai = new ByteArrayInputStream(mapItemArray);

            ObjectInputStream wsin = null;

            if (RuntimeInfo.isThinClient() || RuntimeInfo.isFatClient()) {
                //thin client environment. create ObjectInputStream from factory method.
                //As of now getting twas factory class. 

                //Hard coding now.. once when Libery thin client get developed, this can be taken to a factory type class.
                Class<?> clazz = Class.forName("com.ibm.ws.util.WsObjectInputStream");
                try {
                    wsin = (ObjectInputStream) clazz.getConstructor(ByteArrayInputStream.class).newInstance(bai);
                } catch (Exception e) {
                    // No FFDC code needed
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Exception closing the ObjectInputStream", e);
                }

            } else {
                //Liberty server environment

                ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
                {
                    @Override
                    public ClassLoader run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
                });

                wsin = new DeserializationObjectInputStream(bai, cl);
            }

            item = (Serializable) wsin.readObject();

            try {
                if (wsin != null) {
                    wsin.close();
                }
            } catch (IOException ex) {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Exception closing the ObjectInputStream", ex);
            }
        }

        return item;
    }

    /**
     * Return a boolean indicating whether the value is a valid type for a JMS property.
     * <p>
     * The method has package level visibility as it is used by JsSdoMessageImpl.
     * 
     * @param obj The Object value of the potential JMS Property
     * 
     * @return boolean Indicating whether the property is valid for JMS.
     */
    final boolean isValidForJms(Object obj) {
        if (obj instanceof String
            || obj instanceof Boolean
            || (obj instanceof Number
                && !(obj instanceof BigInteger)
                && !(obj instanceof BigDecimal)
            )) {
            return true;
        }
        else {
            return false;
        }
    }

    /* ************************************************************************* */
    /* transportVersion methods for an odd Maelstrom specific user property */
    /* ************************************************************************* */

    /**
     * Determine whether the transportVersion field in the message header is set
     * This method is package visibility as it is also used by JsJmsMessageImpl
     * 
     * @return value true is the field is set, otherwise false
     */
    final boolean isTransportVersionSet() {
        return (getHdr2().getChoiceField(JsHdr2Access.TRANSPORTVERSION) == JsHdr2Access.IS_TRANSPORTVERSION_DATA);
    }

    /**
     * Get the transportVersion field from the message header
     * This method is package visibility as it is also used by JsJmsMessageImpl
     * 
     * @return value The value of the field, which must be a String.
     */
    final Object getTransportVersion() {
        return getHdr2().getField(JsHdr2Access.TRANSPORTVERSION_DATA);
    }

    /**
     * Set the transportVersion field in the message header to the given value.
     * This method is package visibility as it is also used by JsJmsMessageImpl
     * 
     * @param value The value for the field, which must be a String.
     */
    final void setTransportVersion(Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setTransportVersion", value);
        getHdr2().setField(JsHdr2Access.TRANSPORTVERSION_DATA, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setTransportVersion");
    }

    /**
     * Clear the transportVersion field in the message header.
     * This method is package visibility as it is also used by JsJmsMessageImpl
     */
    final void clearTransportVersion() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearTransportVersion");
        getHdr2().setChoiceField(JsHdr2Access.TRANSPORTVERSION, JsHdr2Access.IS_TRANSPORTVERSION_EMPTY);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearTransportVersion");
    }

    /* ************************************************************************* */
    /* Extra XPath Selector support method(s) */
    /* ************************************************************************* */

    /**
     * Return a DOM Document representation of the message payload, if XPath
     * selection is supported for the message, otherwise return null.
     * 
     * This method will be overridden by the message specialisations which need
     * to return a non-null Document for getIdentifierValue() to operate on,
     * 
     * @return Document A DOM Document representation of the message's payload.
     *         Note that this document should be treated as read-only.
     * 
     * @exception ParserConfigurationException Thrown by the DOM/SAX support
     * @exception IOException Thrown by the DOM/SAX support
     * @exception SIMessageException For an SDO message, the DataGraph could not be read
     * @exception UnsupportedEncodingException The payload of the message is in an unsupported codepage
     */
    Document getPayloadDocument() throws ParserConfigurationException,
                    IOException,
                    SIDataGraphSchemaNotFoundException,
                    SIMessageException,
                    UnsupportedEncodingException {
        return null;
    }

    //
    // Utility method(s)
    //

    /**
     * copyOf
     * This method has pretty much the same spec as the Java 1.6 Arrays.copyOf(byte[]...)
     * method, which can't be used because the Thin Client has to be able to run on v1.5.
     * There is no checking or error-throwing in this method, as it is expected to be
     * called only by intelligent callers!
     * 
     * @param original The original byte array
     * @param length The required length for the copy
     * 
     * @return byte[] A copy of the original byte array, truncated or padded (with 0s)
     *         to the required length, if necessary.
     */
    private byte[] copyOf(byte[] original, int length) {
        byte[] copy = new byte[length];
        if (length <= original.length) {
            System.arraycopy(original, 0, copy, 0, length);
        }
        else {
            System.arraycopy(original, 0, copy, 0, original.length);
        }
        return copy;
    }
}
