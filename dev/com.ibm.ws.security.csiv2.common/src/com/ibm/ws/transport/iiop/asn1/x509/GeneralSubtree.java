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

package com.ibm.ws.transport.iiop.asn1.x509;

import java.math.BigInteger;

import com.ibm.ws.transport.iiop.asn1.ASN1Encodable;
import com.ibm.ws.transport.iiop.asn1.ASN1EncodableVector;
import com.ibm.ws.transport.iiop.asn1.ASN1Sequence;
import com.ibm.ws.transport.iiop.asn1.ASN1TaggedObject;
import com.ibm.ws.transport.iiop.asn1.DERInteger;
import com.ibm.ws.transport.iiop.asn1.DERObject;
import com.ibm.ws.transport.iiop.asn1.DERSequence;
import com.ibm.ws.transport.iiop.asn1.DERTaggedObject;

public class GeneralSubtree
    extends ASN1Encodable
{
    private GeneralName  base;
    private DERInteger minimum;
    private DERInteger maximum;

    public GeneralSubtree(
        ASN1Sequence seq)
    {
        base = GeneralName.getInstance(seq.getObjectAt(0));

        switch (seq.size())
        {
        case 1:
            break;
        case 2:
            ASN1TaggedObject o = (ASN1TaggedObject)seq.getObjectAt(1);
            switch (o.getTagNo())
            {
            case 0 :
                minimum = DERInteger.getInstance(o, false);
                break;
            case 1 :
                maximum = DERInteger.getInstance(o, false);
                break;
            default:
                throw new IllegalArgumentException("Bad tag number: " + o.getTagNo());
            }
            break;
        case 3 :
            minimum = DERInteger.getInstance((ASN1TaggedObject)seq.getObjectAt(1), false);
            maximum = DERInteger.getInstance((ASN1TaggedObject)seq.getObjectAt(2), false);
            break;
        default:
            throw new IllegalArgumentException("Bad sequence size: " + seq.size());
        }
    }

    public static GeneralSubtree getInstance(
        ASN1TaggedObject    o,
        boolean             explicit)
    {
        return new GeneralSubtree(ASN1Sequence.getInstance(o, explicit));
    }

    public static GeneralSubtree getInstance(
        Object obj)
    {
        if (obj == null)
        {
            return null;
        }

        if (obj instanceof GeneralSubtree)
        {
            return (GeneralSubtree)obj;
        }

        return new GeneralSubtree(ASN1Sequence.getInstance(obj));
    }

    public GeneralName getBase()
    {
        return base;
    }

    public BigInteger getMinimum()
    {
        if (minimum == null)
        {
            return BigInteger.valueOf(0);
        }

        return minimum.getValue();
    }

    public BigInteger getMaximum()
    {
        if (maximum == null)
        {
            return null;
        }

        return maximum.getValue();
    }

    /*
     * GeneralSubtree ::= SEQUENCE {
     *      base                    GeneralName,
     *      minimum         [0]     BaseDistance DEFAULT 0,
     *      maximum         [1]     BaseDistance OPTIONAL }
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(base);

        if (minimum != null)
        {
            v.add(new DERTaggedObject(false, 0, minimum));
        }

        if (maximum != null)
        {
            v.add(new DERTaggedObject(false, 1, maximum));
        }

        return new DERSequence(v);
    }
}
