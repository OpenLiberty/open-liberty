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
package com.ibm.ws.javaee.metadata.context.internal;

import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.util.Iterator;
import java.util.concurrent.RejectedExecutionException;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.metadata.context.ComponentMetaDataDecorator;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * Java EE application component context implementation.
 */
public class JEEMetadataContextImpl implements ThreadContext {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -7655152246977747583L;

    /**
     * Names of serializable fields.
     * A single character is used for each to reduce the space required.
     */
    static final String BEGIN_DEFAULT = "D";

    /**
     * Fields to serialize.
     */
    private static final ObjectStreamField[] serialPersistentFields =
                    new ObjectStreamField[] {
                                             new ObjectStreamField(BEGIN_DEFAULT, boolean.class)
                    };

    /**
     * Indicates if we should begin a default context on the thread.
     */
    transient boolean beginDefaultContext;

    transient JEEMetadataContextProviderImpl jeeMetadataContextProvider;

    /**
     * Identifies a deserialized JEE metadata.
     */
    transient String metaDataIdentifier;

    /**
     * The component metadata to propagate. Null if we should begin a default context on the thread.
     */
    transient ComponentMetaData metadataToPropagate;

    /**
     * The component metadata accessor.
     */
    private static final ComponentMetaDataAccessorImpl compMetadataAccessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();

    /**
     * An empty classloader context which erases any classloader context on the thread of execution.
     */
    static final JEEMetadataContextImpl EMPTY_CONTEXT = new JEEMetadataContextImpl(null);

    /**
     * Constructor.
     * 
     * @param provider thread context provider for Java EE metadata context
     */
    public JEEMetadataContextImpl(JEEMetadataContextProviderImpl provider) {
        jeeMetadataContextProvider = provider;
        metadataToPropagate = provider != null ? compMetadataAccessor.getComponentMetaData() : null;
        beginDefaultContext = metadataToPropagate == null;
    }

    /**
     * Constructor.
     * 
     * @param provider thread context provider for Java EE metadata context
     * @param metaDataIdentifier the id of the Java EE metadata context to apply
     */
    public JEEMetadataContextImpl(JEEMetadataContextProviderImpl provider, String metaDataIdentifier) {
        this.jeeMetadataContextProvider = provider;
        this.metaDataIdentifier = metaDataIdentifier;
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext clone() {
        try {
            return (JEEMetadataContextImpl) super.clone();
        } catch (CloneNotSupportedException x) {
            throw new RuntimeException(x);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void taskStarting() throws RejectedExecutionException {
        // Deserialization path. By the time we are here the metadata should already be present.
        if (metaDataIdentifier != null && metadataToPropagate == null)
            metadataToPropagate = (ComponentMetaData) jeeMetadataContextProvider.metadataIdentifierService.getMetaData(metaDataIdentifier);

        if (metadataToPropagate == null)
            compMetadataAccessor.beginDefaultContext();
        else {
            for (Iterator<ComponentMetaDataDecorator> it = jeeMetadataContextProvider.componentMetadataDecoratorRefs.getServices(); it.hasNext();) {
                ComponentMetaDataDecorator decorator = it.next();
                ComponentMetaData metadata = decorator.decorate(metadataToPropagate);
                if (metadata != null) {
                    compMetadataAccessor.beginContext(metadata);
                    return;
                }
            }
            compMetadataAccessor.beginContext(metadataToPropagate);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void taskStopping() {
        compMetadataAccessor.endContext();
    }

    @Override
    @Trivial
    public String toString() {
        StringBuilder sb = new StringBuilder(100)
                        .append(getClass().getSimpleName()).append('@').append(Integer.toHexString(hashCode())).append(' ');
        if (metadataToPropagate != null)
            sb.append(metadataToPropagate.getJ2EEName());
        else if (metaDataIdentifier != null)
            sb.append(metaDataIdentifier);
        else if (beginDefaultContext)
            sb.append("default");
        return sb.toString();
    }

    /**
     * Reads and deserializes from the input stream.
     * 
     * @param in The object input stream from which to deserialize.
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        GetField fields = in.readFields();
        beginDefaultContext = fields.get(BEGIN_DEFAULT, true);

        // Note that further processing is required in JEEMetadataContextProviderImpl.deserializeThreadContext
        // in order to re-establish the thread context based on the metadata identity if not defaulted.
    }

    /**
     * Serialize the given object.
     * 
     * @param outStream The stream to write the serialized data.
     * 
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream outStream) throws IOException {
        PutField fields = outStream.putFields();
        fields.put(BEGIN_DEFAULT, beginDefaultContext);
        outStream.writeFields();
    }
}