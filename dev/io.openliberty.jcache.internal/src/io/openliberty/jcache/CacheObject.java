/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache;

import java.io.Serializable;
import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.serialization.SerializationService;

/**
 * This object will wrap the deserialized byte[] for objects stored in the
 * JCache. The JCache provider should be able to serialize / deserialize this
 * object, and Liberty's {@link SerializationService} will handle serializing /
 * deserializing the objects represented by the byte[].
 * <p/>
 * Additionally, this class shall cache the deserialized object so future
 * requests for the object so deserialization is not required on subsequent
 * requests. For this functionality to work the JCache provider must provide a
 * client-side or near-side cache, and that must support storing by reference
 * so the same object instance is returned each time a value for a single key
 * is fetched.
 */
public class CacheObject implements Serializable {

    /*
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     *
     *
     * WARNING!!!!
     *
     * Carefully consider changes to this class. Serialization across different
     * versions must always be supported. Additionally, any referenced classes must
     * be available to the JCache provider's serialization.
     *
     *
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     */

    @SuppressWarnings("unused")
    private static final TraceComponent tc = Tr.register(CacheObject.class);
    private static final long serialVersionUID = 1L;

    /** The bytes that comprise the serialized object. */
    private byte[] objectBytes;

    /**
     * The deserialized object. This is constructed from the objectBytes. Don't
     * serialize this, as we want the JCache serialization to bypass serializing any
     * objects.
     */
    private transient Object object;

    /**
     * Create a new {@link CacheObject} instance.
     *
     * @param object      The object to wrap. May be null and set later.
     * @param objectBytes A serialized view of <code>object</code>.
     */
    public CacheObject(@Sensitive Object object, @Sensitive byte[] objectBytes) {
        if (objectBytes == null) {
            throw new IllegalArgumentException("The objectBytes argument cannot be null.");
        }

        this.objectBytes = objectBytes;
        this.object = object;
    }

    /**
     * Get the wrapped object.
     *
     * @return The object, or null if there is no object or it has not yet been
     *         deserialized.
     */
    @Sensitive
    public Object getObject() {
        return object;
    }

    /**
     * Get the serialized bytes representing the object.
     *
     * @return The serialized object.
     */
    @Sensitive
    public byte[] getObjectBytes() {
        return objectBytes;
    }

    /**
     * Set the deserialized object.
     *
     * @param object The object.
     */
    public void setObject(@Sensitive Object object) {
        this.object = object;
    }

    @Override
    @Trivial
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CacheObject)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        return Arrays.equals(objectBytes, ((CacheObject) (obj)).objectBytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(objectBytes);
    }

    @Override
    public String toString() {
        return super.toString() + "{objectBytes.length=" + objectBytes.length + ", object=" + object + "}";
    }
}
