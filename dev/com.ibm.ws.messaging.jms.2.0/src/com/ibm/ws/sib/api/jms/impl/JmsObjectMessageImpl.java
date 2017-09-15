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
package com.ibm.ws.sib.api.jms.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.jms.JMSException;
import javax.jms.MessageFormatException;
import javax.jms.ObjectMessage;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.serialization.DeserializationObjectInputStream;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.mfp.JsJmsObjectMessage;
import com.ibm.ws.sib.mfp.MessageCreateFailedException;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author matrober
 * 
 *         This class is an implementation of the JMS ObjectMessage interface.
 */
public class JmsObjectMessageImpl extends JmsMessageImpl implements ObjectMessage {

    /**
     * assigned at version 1.18
     */
    private static final long serialVersionUID = -8638295995751681706L;

    // ******************* PRIVATE STATE VARIABLES *******************

    /**
     * MFP message object representing a JMS ObjectMessage
     * Note: Do not initialise this to null here, otherwise it will overwrite
     * the setup done by instantiateMessage!
     */
    private JsJmsObjectMessage objMsg;

    /**
     * This variable holds a cache of the message toString at the Message level.
     * A separate cache holds the subclass information. The cache is invalidated
     * by changing any property of the message.
     */
    private transient String cachedObjectToString = null;

    // *************************** TRACE INITIALIZATION **************************
    private static TraceComponent tc = SibTr.register(JmsObjectMessageImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);
    private static TraceNLS nls = TraceNLS.getTraceNLS(ApiJmsConstants.MSG_BUNDLE_EXT);

    // ************************ CONSTRUCTORS *************************

    public JmsObjectMessageImpl() throws JMSException {
        // Calling the superclass no-args constructor in turn leads to the
        // instantiateMessage method being called, which we override to return
        // a text message.
        super();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsObjectMessageImpl");

        messageClass = CLASS_OBJECT;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsObjectMessageImpl");
    }

    /**
     * Called by the Session.createObjectMessage(Serializable)
     */
    public JmsObjectMessageImpl(Serializable serObj) throws JMSException {

        this();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsObjectMessageImpl", serObj);

        messageClass = CLASS_OBJECT;
        setObject(serObj);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsObjectMessageImpl");
    }

    /**
     * Constructor for JmsObjectMessageImpl.
     * 
     * @param newMsg
     */
    public JmsObjectMessageImpl(JsJmsObjectMessage newMsg, JmsSessionImpl newSess) {
        // Pass this object to the parent class so that it can keep a reference.
        super(newMsg, newSess);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsObjectMessageImpl", new Object[] { newMsg, newSess });

        // Store the reference we are given, and inform the parent class.
        objMsg = newMsg;
        messageClass = CLASS_OBJECT;

        // Note that we do NOT initialize the defaults for inbound messages.

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsObjectMessageImpl");
    }

    /**
     * Construct a jetstream jms message from a (possibly non-jetstream)
     * vanilla jms message.
     */
    JmsObjectMessageImpl(ObjectMessage objectMessage) throws JMSException {
        // copy message headers and properties.
        super(objectMessage);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsObjectMessageImpl", objectMessage);

        messageClass = CLASS_OBJECT;
        // copy object.
        setObject(objectMessage.getObject());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsObjectMessageImpl");
    }

    /**
     * Override the Message.toString to include some information about the object
     * being stored. No point in caching the name of the class though.
     */
    @Override
    public String toString() {
        String val = super.toString();

        if (cachedObjectToString == null) {
            String objString = null;
            Object obj = null;
            try {
                obj = getObjectInternal();
                if (obj == null) {
                    objString = "<null>";
                }
                else {
                    objString = obj.getClass().toString();
                }
            } catch (Exception e) {
                // No FFDC code needed
                String reason = nls.getFormattedMessage("DESERIALIZATION_EXCEPTION_CWSIA0122",
                                                        new Object[] { e.getClass().getName() + ": " + e.getMessage() }, null);
                objString = reason;
                // NB It's important not to try and generate an FFDC here as it can result in
                //    recursion and stack overflow.
            }

            cachedObjectToString = objString;
        }

        return val + "\n" + cachedObjectToString;
    }

    // ********************* INTERFACE METHODS ***********************

    /**
     * @see javax.jms.ObjectMessage#setObject(Serializable)
     */
    @Override
    public void setObject(Serializable obj) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setObject", (obj == null ? "null" : obj.getClass()));

        try {
            checkBodyWriteable("setObject");

            // SIB0121 - check to see if the producer has promised not to modify the payload.
            // If so, call method to set the REAL object in the underlying message object
            if (producerWontModifyPayloadAfterSet) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Set REAL object into underlying message object");
                objMsg.setRealObject(obj);
            }

            // Otherwise, we must take a copy of the object to prevent alterations of the original
            // object by the user app affecting the object here. Since we must pass the
            // object to the MFP component as a byte array anyway, we may as well serialize
            // it here.
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Copy object and set into underlying message");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                // Write the object to the stream.
                oos.writeObject(obj);
                // Store the serialized object in the MFP component.
                objMsg.setSerializedObject(baos.toByteArray());
            }

            // Clear the cached string
            cachedObjectToString = null;
        }

        catch (IOException e) {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Error serializing object", e);
            // d238447 FFDC Review. Generate FFDC
            throw (MessageFormatException) JmsErrorUtils.newThrowable(
                                                                      MessageFormatException.class,
                                                                      "SERIALIZATION_EXCEPTION_CWSIA0121",
                                                                      new Object[] { e }, e,
                                                                      "JmsObjectMessageImpl.setObject#1",
                                                                      this, tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setObject");
    }

    /**
     * @see javax.jms.ObjectMessage#getObject()
     */
    @Override
    public Serializable getObject() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getObject");

        Serializable obj = null;
        try {
            obj = getObjectInternal();
        } catch (ClassNotFoundException e) {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Error deserializing object", e);
            // d238447 FFDC Review. App/config error, no FFDC.
            throw (MessageFormatException) JmsErrorUtils.newThrowable(
                                                                      MessageFormatException.class,
                                                                      "DESERIALIZATION_EXCEPTION_CWSIA0122",
                                                                      new Object[] { e }, e,
                                                                      null, // null probeId = no FFDC
                                                                      this, tc);
        } catch (Exception e) {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Error deserializing object", e);
            // d238447 FFDC Review.
            //   This block for IOException and anything else, generate FFDC.
            throw (MessageFormatException) JmsErrorUtils.newThrowable(
                                                                      MessageFormatException.class,
                                                                      "DESERIALIZATION_EXCEPTION_CWSIA0122",
                                                                      new Object[] { e }, e,
                                                                      "JmsObjectMessageImpl.getObject#1",
                                                                      this, tc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getObject", (obj == null ? "null" : obj.getClass()));
        return obj;
    }

    //******************** IMPLEMENTATION METHODS **********************

    /**
     * This method deserializes the object but does not handle any exception
     * that is thrown by the deserialization process. The calling methods
     * for this method are getObject, which puts the exception through the
     * regular trace/FFDC path, and toString, which ignores any exception.
     * 
     * Without use of this method, getObject pass the exception into the
     * JmsErrorUtils class, which calls toString on the method, which itself
     * calls getObject causing the exception to be thrown again, and you end
     * up with a StackOverFlow error (191802).
     */
    private Serializable getObjectInternal() throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getObjectInternal");

        Serializable obj = null;
        ObjectInputStream ois = null;

        // SIB0121 - check to see if the consumer has promised not to modify the payload
        // If so, call method to get the REAL object from the underlying message object
        if (consumerWontModifyPayloadAfterGet) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Get REAL object from underlying message object");
            obj = objMsg.getRealObject();
        }

        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Get object from underlying message and deserialize");

            // Retrieve from the MFP component.
            byte[] byteForm = objMsg.getSerializedObject();
            if (byteForm != null) {
                try {

                    ByteArrayInputStream bais = new ByteArrayInputStream(byteForm);

                    ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
                    {
                        @Override
                        public ClassLoader run() {
                            return Thread.currentThread().getContextClassLoader();
                        }
                    });
                    ois = new DeserializationObjectInputStream(bais, cl);
                    obj = (Serializable) ois.readObject();
                } catch (Exception e) {
                    // No FFDC code needed
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "Exception deserializing object - throw on to caller");
                    // Note that we have made a deliberate choice here not to call FFDC or trace
                    // in order to prevent stack overflow as described by the method comment.
                    throw e;
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
        }

        // The caller will trace the class of the return value
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getObjectInternal");
        return obj;
    }

    /**
     * @see com.ibm.ws.sib.api.jms.impl.JmsMessageImpl#instantiateMessage()
     */
    @Override
    protected JsJmsMessage instantiateMessage() throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "instantiateMessage");

        // Create a new message object.
        JsJmsObjectMessage newMsg = null;
        try {
            newMsg = jmfact.createJmsObjectMessage();
        } catch (MessageCreateFailedException e) {
            // No FFDC code needed
            // d238447 FFDC generated by calling method, so don't call processThrowable here
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Error occurred creating message: ", e);
            throw e;
        }

        // Do any other reference storing here (for subclasses)
        objMsg = newMsg;

        // Return the new object.
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "instantiateMessage", newMsg);
        return newMsg;
    }
}
