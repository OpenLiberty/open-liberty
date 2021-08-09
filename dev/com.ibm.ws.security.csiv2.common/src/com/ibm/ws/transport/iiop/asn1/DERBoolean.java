/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */

package com.ibm.ws.transport.iiop.asn1;

import java.io.IOException;

public class DERBoolean
    extends DERObject
{
    byte         value;

    public static final DERBoolean FALSE = new DERBoolean(false);
    public static final DERBoolean TRUE  = new DERBoolean(true);

    /**
     * return a boolean from the passed in object.
     *
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static DERBoolean getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof DERBoolean)
        {
            return (DERBoolean)obj;
        }

        if (obj instanceof ASN1OctetString)
        {
            return new DERBoolean(((ASN1OctetString)obj).getOctets());
        }

        if (obj instanceof ASN1TaggedObject)
        {
            return getInstance(((ASN1TaggedObject)obj).getObject());
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * return a DERBoolean from the passed in boolean.
     */
    public static DERBoolean getInstance(
        boolean  value)
    {
        return (value ? TRUE : FALSE);
    }

    /**
     * return a Boolean from a tagged object.
     *
     * @param obj the tagged object holding the object we want
     * @param explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the tagged object cannot
     *               be converted.
     */
    public static DERBoolean getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(obj.getObject());
    }

    public DERBoolean(
        byte[]       value)
    {
        this.value = value[0];
    }

    public DERBoolean(
        boolean     value)
    {
        this.value = (value) ? (byte)0xff : (byte)0;
    }

    public boolean isTrue()
    {
        return (value != 0);
    }

    void encode(
        DEROutputStream out)
        throws IOException
    {
        byte[]  bytes = new byte[1];

        bytes[0] = value;

        out.writeEncoded(BOOLEAN, bytes);
    }

    public boolean equals(
        Object  o)
    {
        if ((o == null) || !(o instanceof DERBoolean))
        {
            return false;
        }

        return (value == ((DERBoolean)o).value);
    }

    public int hashCode()
    {
        return value;
    }

}
