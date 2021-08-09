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

/**
 * DER BMPString object.
 */
public class DERBMPString
    extends DERObject
    implements DERString
{
    String  string;

    /**
     * return a BMP String from the given object.
     *
     * @param obj the object we want converted.
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static DERBMPString getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof DERBMPString)
        {
            return (DERBMPString)obj;
        }

        if (obj instanceof ASN1OctetString)
        {
            return new DERBMPString(((ASN1OctetString)obj).getOctets());
        }

        if (obj instanceof ASN1TaggedObject)
        {
            return getInstance(((ASN1TaggedObject)obj).getObject());
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * return a BMP String from a tagged object.
     *
     * @param obj the tagged object holding the object we want
     * @param explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the tagged object cannot
     *              be converted.
     */
    public static DERBMPString getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(obj.getObject());
    }


    /**
     * basic constructor - byte encoded string.
     */
    public DERBMPString(
        byte[]   string)
    {
        char[]  cs = new char[string.length / 2];

        for (int i = 0; i != cs.length; i++)
        {
            cs[i] = (char)((string[2 * i] << 8) | (string[2 * i + 1] & 0xff));
        }

        this.string = new String(cs);
    }

    /**
     * basic constructor
     */
    public DERBMPString(
        String   string)
    {
        this.string = string;
    }

    public String getString()
    {
        return string;
    }

    public int hashCode()
    {
        return this.getString().hashCode();
    }

    public boolean equals(
        Object  o)
    {
        if (!(o instanceof DERBMPString))
        {
            return false;
        }

        DERBMPString  s = (DERBMPString)o;

        return this.getString().equals(s.getString());
    }

    void encode(
        DEROutputStream  out)
        throws IOException
    {
        char[]  c = string.toCharArray();
        byte[]  b = new byte[c.length * 2];

        for (int i = 0; i != c.length; i++)
        {
            b[2 * i] = (byte)(c[i] >> 8);
            b[2 * i + 1] = (byte)c[i];
        }

        out.writeEncoded(BMP_STRING, b);
    }
}
