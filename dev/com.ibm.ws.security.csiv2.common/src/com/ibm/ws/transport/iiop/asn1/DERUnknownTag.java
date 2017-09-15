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
 * We insert one of these when we find a tag we don't recognise.
 */
public class DERUnknownTag
    extends DERObject
{
    int         tag;
    byte[]      data;

    /**
     * @param tag the tag value.
     * @param data the octets making up the time.
     */
    public DERUnknownTag(
        int     tag,
        byte[]  data)
    {
        this.tag = tag;
        this.data = data;
    }

    public int getTag()
    {
        return tag;
    }

    public byte[] getData()
    {
        return data;
    }

    void encode(
        DEROutputStream  out)
        throws IOException
    {
        out.writeEncoded(tag, data);
    }

    public boolean equals(
        Object o)
    {
        if ((o == null) || !(o instanceof DERUnknownTag))
        {
            return false;
        }

        DERUnknownTag other = (DERUnknownTag)o;

        if (tag != other.tag)
        {
            return false;
        }

        if (data.length != other.data.length)
        {
            return false;
        }

        for (int i = 0; i < data.length; i++)
        {
            if(data[i] != other.data[i])
            {
                return false;
            }
        }

        return true;
    }

    public int hashCode()
    {
        byte[]  b = this.getData();
        int     value = 0;

        for (int i = 0; i != b.length; i++)
        {
            value ^= (b[i] & 0xff) << (i % 4);
        }

        return value ^ this.getTag();
    }
}
