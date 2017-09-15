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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * DER TaggedObject - in ASN.1 nottation this is any object proceeded by
 * a [n] where n is some number - these are assume to follow the construction
 * rules (as with sequences).
 */
public class DERTaggedObject
    extends ASN1TaggedObject
{
    /**
     * @param tagNo the tag number for this object.
     * @param obj the tagged object.
     */
    public DERTaggedObject(
        int             tagNo,
        DEREncodable    obj)
    {
        super(tagNo, obj);
    }

    /**
     * @param explicit true if an explicitly tagged object.
     * @param tagNo the tag number for this object.
     * @param obj the tagged object.
     */
    public DERTaggedObject(
        boolean         explicit,
        int             tagNo,
        DEREncodable    obj)
    {
        super(explicit, tagNo, obj);
    }

    /**
     * create an implicitly tagged object that contains a zero
     * length sequence.
     */
    public DERTaggedObject(
        int             tagNo)
    {
        super(false, tagNo, new DERSequence());
    }

    void encode(
        DEROutputStream  out)
        throws IOException
    {
        if (!empty)
        {
            ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
            DEROutputStream         dOut = new DEROutputStream(bOut);

            dOut.writeObject(obj);
            dOut.close();

            byte[]  bytes = bOut.toByteArray();

            if (explicit)
            {
                out.writeEncoded(CONSTRUCTED | TAGGED | tagNo, bytes);
            }
            else
            {
                //
                // need to mark constructed types...
                //
                if ((bytes[0] & CONSTRUCTED) != 0)
                {
                    bytes[0] = (byte)(CONSTRUCTED | TAGGED | tagNo);
                }
                else
                {
                    bytes[0] = (byte)(TAGGED | tagNo);
                }

                out.write(bytes);
            }
        }
        else
        {
            out.writeEncoded(CONSTRUCTED | TAGGED | tagNo, new byte[0]);
        }
    }
}
