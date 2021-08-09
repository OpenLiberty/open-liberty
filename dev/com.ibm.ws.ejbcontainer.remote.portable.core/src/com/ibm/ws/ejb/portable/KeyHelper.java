/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejb.portable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

public class KeyHelper implements java.io.Serializable
{
    /* data member to hold the keys bytes */

    public byte[] vBytes = null;

    /**
     * serialize the primary key corresponding to a Handle to an entity bean for Rel. 3.5, 4.0.
     * This method is called from the writeObject
     *
     * @param serializable - Serializable key to be converted into a KeyHelper.
     *
     * @return Serializable the KeyHelper with the serialized primary key.
     *
     * @exception java.io.IOException - The EJBObject could not be serialized
     *                because of a system-level failure.
     *
     */

    public static Serializable serialize(Serializable serializable) throws IOException {
        if (serializable == null)
        {
            return null;
        }
        if (serializable instanceof KeyHelper)
        {
            return serializable;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(serializable);
        byteArrayOutputStream.flush();//d164668
        KeyHelper vkh = new KeyHelper();
        vkh.vBytes = byteArrayOutputStream.toByteArray();

        return vkh;
    }

    /**
     * deserialize the Object reference corresponding to a primary key for a Handle to Rel. 3.5,4.0 Entity Bean.
     * This method is called from the readResolve
     *
     * @param kh - Serializable KeyHelper to be converted back to a primary key.
     *
     * @return Serializable The deserialized primary key.
     *
     * @exception java.io.IOException - The Key could not be deserialized
     *                because of a system-level failure.
     * @exception ClassNotFoundException - class not found during readObject
     *                this shouldn't happen as we are using the context class loader for the
     *                CCLObjectInputStream, which should allow us to find the key, stub, and remote classes
     *
     */

    public static Serializable deserialize(Serializable kh) throws IOException, ClassNotFoundException {
        if (kh == null)
        {
            return null;
        }
        if (!(kh instanceof KeyHelper))
        {
            return kh;

        }
        KeyHelper ivKh = (KeyHelper) kh;
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(ivKh.vBytes);
        ObjectInputStream objectInputStream = new CCLObjectInputStream(byteArrayInputStream);
        Serializable result = (Serializable) objectInputStream.readObject();
        objectInputStream.close(); // findbugs
        return result;
    }

    /**
     * readResolve is the standard overide if you want to substitute
     * during a readObject call during serialization
     * in our case, we call the deserialize method to deserialize
     * the contained byte array into the original Key object
     *
     * @return Object the key of the object used for findByPrimaryKey
     *
     * @exception ObjectStreamException - The key could not be deserialized
     *                because of a system-level failure.
     *
     */

    public Object readResolve() throws ObjectStreamException
    {
        Object obj = null;
        try
        {
            obj = deserialize(this);
        } catch (Throwable t)
        {
            // FFDCFilter.processException(e, CLASS_NAME + ".lookupHandleDelegate",
            //                             "134", this);

            throw new InvalidObjectException("com.ibm.ws.ejb.portable.KeyHelper can not be deserialized");
        }

        return obj;

    }

    private static final long serialVersionUID = -1057527058885156475L;

}
