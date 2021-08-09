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

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * DER IA5String object - this is an ascii string.
 */
public class DERIA5String
                extends DERObject
                implements DERString
{
    String string;

    /**
     * return a IA5 string from the passed in object
     * 
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static DERIA5String getInstance(
                                           Object obj)
    {
        if (obj == null || obj instanceof DERIA5String)
        {
            return (DERIA5String) obj;
        }

        if (obj instanceof ASN1OctetString)
        {
            return new DERIA5String(((ASN1OctetString) obj).getOctets());
        }

        if (obj instanceof ASN1TaggedObject)
        {
            return getInstance(((ASN1TaggedObject) obj).getObject());
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * return an IA5 String from a tagged object.
     * 
     * @param obj the tagged object holding the object we want
     * @param explicit true if the object is meant to be explicitly
     *            tagged false otherwise.
     * @exception IllegalArgumentException if the tagged object cannot
     *                be converted.
     */
    public static DERIA5String getInstance(
                                           ASN1TaggedObject obj,
                                           boolean explicit)
    {
        return getInstance(obj.getObject());
    }

    /**
     * basic constructor - with bytes.
     */
    public DERIA5String(
                        byte[] string)
    {
        char[] cs = new char[string.length];

        for (int i = 0; i != cs.length; i++)
        {
            cs[i] = (char) (string[i] & 0xff);
        }

        this.string = new String(cs);
    }

    /**
     * basic constructor - with string.
     */
    public DERIA5String(
                        String string)
    {
        this.string = string;
    }

    @Override
    @Trivial
    public String getString()
    {
        return string;
    }

    public byte[] getOctets()
    {
        char[] cs = string.toCharArray();
        byte[] bs = new byte[cs.length];

        for (int i = 0; i != cs.length; i++)
        {
            bs[i] = (byte) cs[i];
        }

        return bs;
    }

    @Override
    void encode(
                DEROutputStream out)
                    throws IOException
    {
        out.writeEncoded(IA5_STRING, this.getOctets());
    }

    @Override
    public int hashCode()
    {
        return this.getString().hashCode();
    }

    @Override
    public boolean equals(
                          Object o)
    {
        if (!(o instanceof DERIA5String))
        {
            return false;
        }

        DERIA5String s = (DERIA5String) o;

        return this.getString().equals(s.getString());
    }
}
