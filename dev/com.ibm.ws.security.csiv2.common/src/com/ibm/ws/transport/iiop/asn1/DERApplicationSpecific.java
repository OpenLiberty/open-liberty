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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Base class for an application specific object
 */
public class DERApplicationSpecific
    extends DERObject
{
    private int       tag;
    private byte[]    octets;

    public DERApplicationSpecific(
        int        tag,
        byte[]    octets)
    {
        this.tag = tag;
        this.octets = octets;
    }

    public DERApplicationSpecific(
        int                  tag,
        DEREncodable         object)
        throws IOException
    {
        this.tag = tag | DERTags.CONSTRUCTED;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DEROutputStream dos = new DEROutputStream(baos);

        dos.writeObject(object);

        this.octets = baos.toByteArray();
    }

    public boolean isConstructed()
    {
        return (tag & DERTags.CONSTRUCTED) != 0;
    }

    public byte[] getContents()
    {
        return octets;
    }

    public int getApplicationTag()
    {
        return tag & 0x1F;
    }

    public DERObject getObject()
        throws IOException
    {
        return new ASN1InputStream(new ByteArrayInputStream(getContents())).readObject();
    }

    /* (non-Javadoc)
     * @see org.apache.geronimo.crypto.asn1.DERObject#encode(org.apache.geronimo.crypto.asn1.DEROutputStream)
     */
    void encode(DEROutputStream out) throws IOException
    {
        out.writeEncoded(DERTags.APPLICATION | tag, octets);
    }

    public boolean equals(
            Object o)
    {
        if ((o == null) || !(o instanceof DERApplicationSpecific))
        {
            return false;
        }

        DERApplicationSpecific other = (DERApplicationSpecific)o;

        if (tag != other.tag)
        {
            return false;
        }

        if (octets.length != other.octets.length)
        {
            return false;
        }

        for (int i = 0; i < octets.length; i++)
        {
            if (octets[i] != other.octets[i])
            {
                return false;
            }
        }

        return true;
    }

    public int hashCode()
    {
        byte[]  b = this.getContents();
        int     value = 0;

        for (int i = 0; i != b.length; i++)
        {
            value ^= (b[i] & 0xff) << (i % 4);
        }

        return value ^ this.getApplicationTag();
    }
}
