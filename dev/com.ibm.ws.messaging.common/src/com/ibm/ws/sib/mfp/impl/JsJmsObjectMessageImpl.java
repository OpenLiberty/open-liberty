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
package com.ibm.ws.sib.mfp.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIApiConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.serialization.DeserializationObjectInputStream;
import com.ibm.ws.sib.mfp.JmsBodyType;
import com.ibm.ws.sib.mfp.JsJmsObjectMessage;
import com.ibm.ws.sib.mfp.MessageDecodeFailedException;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.ObjectFailedToSerializeException;
import com.ibm.ws.sib.mfp.schema.JmsObjectBodyAccess;
import com.ibm.ws.sib.mfp.schema.JsPayloadAccess;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * JsJmsObjectMessageImpl extends JsJmsMessageImpl and hence JsMessageImpl,
 * and is the implementation class for the JsJmsObjectMessage interface.
 */
final class JsJmsObjectMessageImpl extends JsJmsMessageImpl implements JsJmsObjectMessage {

    private final static long serialVersionUID = 1L;
    private final static byte[] flattenedClassName; // SIB0112b.mfp.2

    // A vague guess as to how much bigger an Object becomes if it is deserialized.
    private final static int DESERIALIZATION_MULTIPLIER = 4;

    private static TraceComponent tc = SibTr.register(JsJmsObjectMessageImpl.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

    /* Get the flattened form of the classname SIB0112b.mfp.2 */
    static {
        flattenedClassName = flattenClassName(JsJmsObjectMessageImpl.class.getName());
    }

    // Variable to hold the 'real' object reference - SIB0121.mfp.1
    private transient Serializable realObject;

    // To cope with being given null as a real object - SIB0121.mfp.1
    private transient boolean hasRealObject;

    // So we know if we have already serialized the real Object to the payload - SIB0121.mfp.1
    private transient boolean hasSerializedRealObject;

    // Soft Reference to real Object - SIB0121.mfp.3
    private transient SoftReference<Serializable> softRefToRealObject = null;

    /* ************************************************************************* */
    /* Constructors */
    /* ************************************************************************* */

    /**
     * Constructor for a new Jetstream JMS ObjectMessage.
     * 
     * This constructor should never be used except by JsMessageImpl.createNew().
     * The method must not actually do anything.
     */
    JsJmsObjectMessageImpl() {}

    /**
     * Constructor for a new Jetstream JMS ObjectMessage.
     * To be called only by the JsJmsMessageFactory.
     * 
     * @param flag Flag to distinguish different construction reasons.
     * 
     * @exception MessageDecodeFailedException Thrown if such a message can not be created
     */
    JsJmsObjectMessageImpl(int flag) throws MessageDecodeFailedException {
        super(flag);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "<init>");

        /* Set the JMS format & body information */
        setFormat(SIApiConstants.JMS_FORMAT_OBJECT);
        setBodyType(JmsBodyType.OBJECT);

        // We can skip this for an inbound MQ message as the MQJsMessageFactory will
        // replace the PAYLOAD_DATA with an MQJsApiEncapsulation.
        if (flag != MfpConstants.CONSTRUCTOR_INBOUND_MQ) {
            jmo.getPayloadPart().setPart(JsPayloadAccess.PAYLOAD_DATA, JmsObjectBodyAccess.schema);
            clearBody();
        }
    }

    /**
     * Constructor for an inbound message.
     * (Only to be called by a superclass make method.)
     * 
     * @param inJmo The JsMsgObject representing the inbound method.
     */
    JsJmsObjectMessageImpl(JsMsgObject inJmo) {
        super(inJmo);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "<init>, inbound jmo ");
    }

    /* ************************************************************************ */
    /* Payload Methods */
    /* ************************************************************************ */

    /*
     * Get the byte array containing the serialized object which forms the
     * payload of the message.
     * The default value is null.
     * 
     * Javadoc description supplied by JsJmsObjectMessage interface.
     * 
     * @throws ObjectFailedToSerializeException if there are problems serializing the real object
     */
    @Override
    public byte[] getSerializedObject() throws ObjectFailedToSerializeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getSerializedObject");
        byte[] bytes;

        if (hasRealObject && !hasSerializedRealObject) {
            // If we have a real object but it has not yet been serialized we have to serialize it into
            // the message payload.
            serializeRealObject();
        }
        // Get the bytes from the payload
        bytes = getDataFromPayload();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getSerializedObject");
        return bytes;
    }

    /*
     * Set the body (payload) of the message. The payload is the byte array
     * containing the serialized version of the Object originally passed in to
     * the JMS API call.
     * 
     * Javadoc description supplied by JsJmsObjectMessage interface.
     */
    @Override
    public void setSerializedObject(byte[] payload) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setSerializedObject", payload);

        // Set the new data into the message itself
        getPayload().setField(JmsObjectBodyAccess.BODY_DATA_VALUE, payload);
        // Throw away any references to the old 'real' object we have
        realObject = null;
        hasRealObject = false;
        hasSerializedRealObject = false;
        softRefToRealObject = null;
        clearCachedLengths();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setSerializedObject");
    }

    /*
     * Get hold of the real object. If we do not have a reference to it then deserialize it
     * and return that.
     * Stash it in a SoftReference in case we want it again.
     * 
     * Javadoc description supplied by JsJmsObjectMessage interface.
     */
    @Override
    public Serializable getRealObject() throws IOException, ClassNotFoundException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getRealObject");

        Serializable obj = null;

        // We 'have' a real object, though we may not still have a ref to to it.
        if (hasRealObject) {
            obj = realObject;

            // if it is null it may be because we previously serialized & threw away the strong ref
            if ((obj == null) && (hasSerializedRealObject)) {

                // Unless the value really is a null, we will have set a SoftReference for it
                if (softRefToRealObject != null) {

                    // Can we get it back from the SoftReference?
                    obj = softRefToRealObject.get();
                    // If not, deserialize from the message.
                    if (obj == null) {
                        obj = deserializeToRealObject();
                    }
                }

            }

            // If we hadn't previously serialized the message, then whatever was in realObject is the real
            // value, even if it is null.
        }

        // If we didn't 'have' a real object at all, just deserialize the one in the message
        else {
            obj = deserializeToRealObject();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getRealObject", (obj == null ? "null" : obj.getClass()));
        return obj;
    }

    /*
     * Set the real object
     * Javadoc description supplied by JsJmsObjectMessage interface.
     */
    @Override
    public void setRealObject(Serializable obj) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setRealObject", (obj == null ? "null" : obj.getClass()));

        // Set all the values for the new 'real' object
        realObject = obj;
        hasRealObject = true;
        hasSerializedRealObject = false;
        // Clear any out of date information in the SoftReference
        softRefToRealObject = null;
        // We have a new payload so get rid of any serialized version of an old payload
        getPayload().setChoiceField(JmsObjectBodyAccess.BODY, JmsObjectBodyAccess.IS_BODY_EMPTY);
        clearCachedLengths();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setRealObject");
    }

    /*
     * Clear the message body.
     * 
     * Javadoc description supplied by JsJmsMessage interface.
     */
    @Override
    public void clearBody() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "clearBody");

        // Clear any data in the message itself
        getPayload().setChoiceField(JmsObjectBodyAccess.BODY, JmsObjectBodyAccess.IS_BODY_EMPTY);
        // Throw away any references to the 'real' object
        realObject = null;
        hasRealObject = false;
        hasSerializedRealObject = false;
        softRefToRealObject = null;
        clearCachedLengths();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "clearBody");
    }

    /**
     * Provide an estimate of encoded length of the payload
     * 
     * If we have a real object and no serialized one then we have no idea how big it is,
     * so leave the guess at 0
     */
    @Override
    int guessPayloadLength() {
        int length = 0;
        byte[] payload = getDataFromPayload();
        if (payload != null) {
            length = payload.length + 24;
        }
        return length;
    }

    /**
     * guessFluffedDataSize
     * Return the estimated fluffed size of the payload data.
     * 
     * For this class, we should return an approximation of the fluffed up payload
     * data.
     * 
     * @return int A guesstimate of the fluffed size of the payload data
     */
    @Override
    int guessFluffedDataSize() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "guessFluffedDataSize");

        // If the data has been set in 'unserialized' then it will be ignored in these
        // calculations. This seems reasonable, as effectively it still 'belongs' to
        // the App unless/until it is serialized.

        int total = 0;

        // Add the estimate for the fluffed payload size
        // If the body's JMF message is already fluffed up & cached, ask it for the size.
        // However, it's in a serialized form, so apply our deserialization multiplier.
        // Do NOT hold on to this JSMsgPart, as it could lose validity at any time.
        JsMsgPart part = getPayloadIfFluffed();
        if (part != null) {
            total += part.estimateFieldValueSize(JmsObjectBodyAccess.BODY_DATA_VALUE) * DESERIALIZATION_MULTIPLIER;
        }

        // If the JMF message hasn't been fluffed up, find the total assembled length of
        // the payload message if possible.
        else {
            // If we have a valid length, remove a bit & assume the rest is the serialized Object.
            // We don't have a clue what it is, so we'll apply our deserialization multiplier as usual.
            int payloadSize = jmo.getPayloadPart().getAssembledLengthIfKnown();
            if (payloadSize != -1) {
                total += (payloadSize - FLATTENED_PAYLOAD_PART) * DESERIALIZATION_MULTIPLIER;
            }
            // If the payloadSize == -1, then the body message must have been fluffed up
            // but not yet cached, so we'll locate & cache it now.
            else {
                // However, it's in a serialized form, so apply our deserialization multiplier.
                total += getPayload().estimateFieldValueSize(JmsObjectBodyAccess.BODY_DATA_VALUE) * DESERIALIZATION_MULTIPLIER;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "guessFluffedDataSize", total);
        return total;
    }

    // Convenience method to get the payload as a JmsObjectBodySchema
    JsMsgPart getPayload() {
        return getPayload(JmsObjectBodyAccess.schema);
    }

    /* ************************************************************************* */
    /* Misc Package and Private Methods */
    /* ************************************************************************* */

    /**
     * Return the name of the concrete implementation class encoded into bytes
     * using UTF8. SIB0112b.mfp.2
     * 
     * @return byte[] The name of the implementation class encoded into bytes.
     */
    @Override
    final byte[] getFlattenedClassName() {
        return flattenedClassName;
    }

    /**
     * Helper method to get the byte array containing the serialized object
     * from the JMF payload.
     * 
     * @return byte[] The payload data
     */
    private final byte[] getDataFromPayload() {
        return (byte[]) getPayload().getField(JmsObjectBodyAccess.BODY_DATA_VALUE);
    }

    /**
     * Private method to serialize the 'real' object into the payload.
     * 
     * This method is only called if we have a 'real' object AND it has not already been serialized
     * Note that this doesn't preclude realObject being null, as null may be the real value
     * set using setRealObject().
     * 
     * @exception ObjectFailedToSerializeException Serialization of the object failed
     */
    private void serializeRealObject() throws ObjectFailedToSerializeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "serializeRealObject");

        if (hasRealObject) {

            // If the realObject isn't null, we need to serialize it & set it into the message
            if (realObject != null) {

                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    // Write the real object into a byte array
                    oos.writeObject(realObject);
                    // Store the bytes in the payload
                    getPayload().setField(JmsObjectBodyAccess.BODY_DATA_VALUE, baos.toByteArray());

                    // Set the flag, create a SoftReference to the Object & null out the strong reference
                    // so the object can be GCd if necessary
                    hasSerializedRealObject = true;
                    softRefToRealObject = new SoftReference<Serializable>(realObject);
                    realObject = null;
                }

                catch (IOException ioe) {
                    FFDCFilter.processException(ioe, "com.ibm.ws.sib.mfp.impl.JsJmsObjectMessageImpl.serializeRealObject", "296");
                    // Wrapper the exception, giving the object's class name, and throw.
                    String objectClassName = realObject.getClass().getName();
                    throw new ObjectFailedToSerializeException(ioe
                                    , objectClassName);
                }
            }

            // If the realObject is null, we just set the field in the message & can now claim to have serialized it.
            else {
                // Real object is null
                getPayload().setField(JmsObjectBodyAccess.BODY_DATA_VALUE, null);
                // We have not actually serialized anything, but the object data is in the payload
                hasSerializedRealObject = true;
            }
        }

        // Any length calculation will need to be redone as the message now 'owns' the payload value
        clearCachedLengths();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "serializeRealObject");
    }

    /**
     * Private method to deserialize the 'real' object from the payload.
     * 
     * The deserialized object is returned by the method & stored in a SoftReference for
     * potential future use. A SoftReference is used because we still have the serialized
     * obejct in the message itself, and, if the object is large, holding both forms
     * could cause us to go short on heap.
     * 
     * Note this method is only called if we don't already have a realObject.
     * 
     * @return Serializable The deserialized object from the message
     * 
     * @exception IOException The object could not be deserialized
     * @exception ClassNotFoundException A class required to deserialize the object could not be found.
     */
    private Serializable deserializeToRealObject() throws IOException, ClassNotFoundException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "deserializeToRealObject");

        Serializable obj = null;
        ObjectInputStream ois = null;
        byte[] bytes = getDataFromPayload();

        if (bytes != null) {

            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

                // Get the classloader, which may be the standard classloader or may be an application
                // classloader provided by WebSphere
                ClassLoader cl = AccessController.doPrivileged(
                                new PrivilegedAction<ClassLoader>() {
                                    @Override
                                    public ClassLoader run() {
                                        return Thread.currentThread().getContextClassLoader();
                                    }
                                });

                ois = new DeserializationObjectInputStream(bais, cl);

                // Deserialize the object and set the local variables appropriately
                obj = (Serializable) ois.readObject();
                hasRealObject = true;
                hasSerializedRealObject = true;
                softRefToRealObject = new SoftReference<Serializable>(obj);
            } catch (IOException ioe) {
                FFDCFilter.processException(ioe, "com.ibm.ws.sib.mfp.impl.JsJmsObjectMessageImpl.deserializeToRealObject", "340");
                throw ioe;
            } catch (ClassNotFoundException cnfe) {
                FFDCFilter.processException(cnfe, "com.ibm.ws.sib.mfp.impl.JsJmsObjectMessageImpl.deserializeToRealObject", "345");
                throw cnfe;
            } finally {
                try {
                    if (ois != null) {
                        ois.close();
                    }
                } catch (IOException ex) {
                    // No FFDC code needed
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Exception closing the ObjectInputStream", ex);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "deserializeToRealObject", (obj == null ? "null" : obj.getClass()));
        return obj;
    }

    /**
     * Prepare the message for various encoding and copying type activities.
     * 
     * Flatten, Encode and makeInboundSDOMessage all need the payload contents serialized and in the payload
     * 
     * getCopy, makeInboundJms and makeInboundOther do not need the data serialized
     * 
     * @param why The reason for the update
     * @see com.ibm.ws.sib.mfp.MfpConstants
     */
    @Override
    void updateDataFields(int why) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "updateDataFields", why);
        super.updateDataFields(why);

        // If we have an unserialized object
        if (hasRealObject && !hasSerializedRealObject) {

            // If we are encoding for transmission, flattening or creation of a datagraph
            // then serialize the data
            if (why == MfpConstants.UDF_ENCODE || why == MfpConstants.UDF_FLATTEN || why == MfpConstants.UDF_MAKE_INBOUND_SDO) {

                try {
                    serializeRealObject();
                }

                catch (ObjectFailedToSerializeException ofse) {
                    // No FFDC code needed
                    // The underlying IOException has already been FFDC'd
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Payload object serialization failed so payload will be empty.");

                    // We are unable to serialize the object due to an application error.
                    // To avoid screwing up the system we clear the object reference and set null
                    // into the JMF message then carry on.
                    // We could hang on to the real object, in case a local consumer is available
                    // quickly enough, but we don't as the outcome would then be non-deterministic
                    // and the problem more difficult to diagnose.
                    realObject = null;
                    getPayload().setField(JmsObjectBodyAccess.BODY_DATA_VALUE, null);

                    // We set hasRealObject to false, so we don't waste time looking for a soft
                    // reference to it because it is null.  There is no need to set
                    // hasSerializedRealObject to true, as we no longer think we have a real object.
                    hasRealObject = false;

                    // We set the message's Exception fields so that there is a clue to why the payload is now empty.
                    setExceptionReason(ofse.getExceptionReason());
                    setExceptionInserts(ofse.getExceptionInserts());
                    setExceptionTimestamp(System.currentTimeMillis());
                    setExceptionProblemDestination(null);
                    setExceptionProblemSubscription(null);
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "updateDataFields");
    }

    /**
     * Copy transient data to a new instance of an ObjectMesssge
     * 
     * @param copy The new copy of the mesage to copy information into
     */
    @Override
    void copyTransients(JsMessageImpl copy) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "copyTransients");
        super.copyTransients(copy);

        JsJmsObjectMessageImpl other = (JsJmsObjectMessageImpl) copy;
        // Copy the real object reference, soft reference & booleans across
        other.realObject = realObject;
        other.hasRealObject = hasRealObject;
        other.hasSerializedRealObject = hasSerializedRealObject;
        other.softRefToRealObject = softRefToRealObject;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "copyTransients");
    }

}
