/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.serializable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.classloading.ClassLoadingConfigurationException;

public final class ClassLoaderIdentityImpl implements ClassLoaderIdentity, Comparable<ClassLoaderIdentityImpl> {
    /**  */
    private static final long serialVersionUID = 1L;

    /**
     * Names of serializable fields.
     * A single character is used for each to reduce the space required.
     */
    private static final String
                    DOMAIN = "D",
                    ID = "I";

    /**
     * Fields to serialize
     */
    private static final ObjectStreamField[] serialPersistentFields =
                    new ObjectStreamField[] {
                                             new ObjectStreamField(DOMAIN, String.class),
                                             new ObjectStreamField(ID, String.class)
                    };

    private static final String SEPARATOR = ":";

    private String domain, id;

    public ClassLoaderIdentityImpl(String domain, String id) {
        if (domain == null)
            throw new ClassLoadingConfigurationException("Parameter 'domain' must not be null");
        if (id == null)
            throw new ClassLoadingConfigurationException("Parameter 'id' must not be null");
        this.domain = domain;
        this.id = id;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!!!(other instanceof ClassLoaderIdentityImpl)) // also catches nulls
            return false;
        ClassLoaderIdentityImpl that = (ClassLoaderIdentityImpl) other;
        return (this.domain.equals(that.domain)) && (this.id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return domain.hashCode() ^ id.hashCode();
    }

    @Override
    public int compareTo(ClassLoaderIdentityImpl that) {
        // initial sort on domain
        int result = this.domain.compareTo(that.domain);
        // secondary sort on id
        if (result == 0)
            result = this.id.compareTo(that.id);
        return result;
    }

    @Override
    public String toString() {
        return domain + SEPARATOR + id;
    }

    @Override
    public String getDomain() {
        return domain;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Deserialize.
     * 
     * @param in The stream from which this object is read.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Trivial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        GetField fields = in.readFields();
        domain = (String) fields.get(DOMAIN, null);
        id = (String) fields.get(ID, null);
    }

    /**
     * Serialize.
     * 
     * @param out The stream to which this object is serialized.
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        PutField fields = out.putFields();
        fields.put(DOMAIN, domain);
        fields.put(ID, id);
        out.writeFields();
    }
}