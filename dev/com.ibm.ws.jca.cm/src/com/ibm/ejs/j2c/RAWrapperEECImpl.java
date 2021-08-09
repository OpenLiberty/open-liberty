/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;

import javax.resource.spi.ResourceAdapter;

/**
 * Resource adapter wrapper for the embeddable EJB container data source.
 * This is an empty wrapper that no-ops most methods or returns default values.
 */
public class RAWrapperEECImpl extends RAWrapper {
    private static final long serialVersionUID = -4156680069816270158L;

    /** Key for the resource adapter. */
    private String resourceAdapterKey; 
    /** Version number for serialization. */
    private int version = 1; 

    /**
     * List of fields that will be serialized when the writeObject method is
     * called. We do this so that the implementation of this class can change
     * without breaking the serialization process.
     */
    static private final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[] {
                                                                                               new ObjectStreamField("resourceAdapterKey", String.class),
                                                                                               new ObjectStreamField("version", int.class) 
    };

    /**
     * Construct a new resource adapter wrapper for the embeddable EJB container data source.
     * 
     * @param resourceAdapterKey key for the resource adapter
     */
    RAWrapperEECImpl(String resourceAdapterKey) {
        this.resourceAdapterKey = resourceAdapterKey;
    }

    /**
     * @return the JCA specification version supported by the resource adapter.
     */
    @Override
    public final J2CConstants.JCASpecVersion getJcaSpecVersion() {
        return J2CConstants.JCASpecVersion.JCA_VERSION_10;
    }

    /**
     * @return the archive path of the resource adapter
     */
    @Override
    public final String getArchivePath() {
        return null;
    }

    /**
     * @return the resource adapter instance.
     */
    @Override
    public final ResourceAdapter getRA() {
        return null;
    }

    /**
     * @return the class name of the resource adapter.
     */
    @Override
    public final String getRaClassName() {
        return null;
    }

    /**
     * @return the rasource adatper key.
     */
    @Override
    public final String getRAKey() {
        return resourceAdapterKey;
    }

    /**
     * @return the resource adapter name.
     */
    @Override
    public final String getRAName() {
        return "Data Source for the Embeddable EJB Container";
    }

    /**
     * @return false because the resource adapter is not embedded in the application.
     */
    @Override
    public final boolean isEmbedded() {
        return false;
    }

    /**
     * @return whether this resource adapter should have no more than one instance
     *         (singleton) in the JVM.
     */
    @Override
    public final boolean isForceSingleRAInstance() {
        return true;
    }

    /**
     * @return true if the resource adapter is started, otherwise false.
     */
    @Override
    public final boolean isStarted() {
        return true;
    }

    /**
     * @return true if the resource adapter was created specifically for recovery, otherwise false.
     */
    @Override
    protected final boolean isTXRecovery() {
        return true;
    }

    /**
     * Overrides the default deserialization.
     */
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField getField = s.readFields();

        version = getField.get("version", 1);
        resourceAdapterKey = (String) getField.get("resourceAdapterKey", null);
    }

    /**
     * Starts the resource adapter.
     */
    @Override
    public final void startRA(Object j2cRA, String mbParentName, boolean embedded, int source) throws Exception {

    }

    /**
     * Stops the resource adapter.
     */
    @Override
    public final void stopRA(boolean originatesFromTM) throws Exception {

    }

    /**
     * Overrides the default serialization.
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        ObjectOutputStream.PutField putField = s.putFields();
        putField.put("version", version);
        putField.put("resourceAdapterKey", resourceAdapterKey);
        s.writeFields();
    }
}
