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

import com.ibm.ws.transport.iiop.asn1.ASN1Encodable;
import com.ibm.ws.transport.iiop.asn1.ASN1EncodableVector;
import com.ibm.ws.transport.iiop.asn1.ASN1Sequence;
import com.ibm.ws.transport.iiop.asn1.ASN1TaggedObject;
import com.ibm.ws.transport.iiop.asn1.DERBitString;
import com.ibm.ws.transport.iiop.asn1.DEREnumerated;
import com.ibm.ws.transport.iiop.asn1.DERObject;
import com.ibm.ws.transport.iiop.asn1.DERObjectIdentifier;
import com.ibm.ws.transport.iiop.asn1.DERSequence;
import com.ibm.ws.transport.iiop.asn1.x509.AlgorithmIdentifier;


public class ObjectDigestInfo
    extends ASN1Encodable
{
    DEREnumerated digestedObjectType;

    DERObjectIdentifier otherObjectTypeID;

    AlgorithmIdentifier digestAlgorithm;

    DERBitString objectDigest;

    public static ObjectDigestInfo getInstance(
            Object  obj)
    {
        if (obj == null || obj instanceof ObjectDigestInfo)
        {
            return (ObjectDigestInfo)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new ObjectDigestInfo((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    public static ObjectDigestInfo getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public ObjectDigestInfo(ASN1Sequence seq)
    {
        digestedObjectType = DEREnumerated.getInstance(seq.getObjectAt(0));

        int offset = 0;

        if (seq.size() == 4)
        {
            otherObjectTypeID = DERObjectIdentifier.getInstance(seq.getObjectAt(1));
            offset++;
        }

        digestAlgorithm = AlgorithmIdentifier.getInstance(seq.getObjectAt(1 + offset));

        objectDigest = new DERBitString(seq.getObjectAt(2 + offset));
    }

    public DEREnumerated getDigestedObjectType()
    {
        return digestedObjectType;
    }

    public DERObjectIdentifier getOtherObjectTypeID()
    {
        return otherObjectTypeID;
    }

    public AlgorithmIdentifier getDigestAlgorithm()
    {
        return digestAlgorithm;
    }

    public DERBitString getObjectDigest()
    {
        return objectDigest;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     *
     * <pre>
     *
     *   ObjectDigestInfo ::= SEQUENCE {
     *        digestedObjectType  ENUMERATED {
     *                publicKey            (0),
     *                publicKeyCert        (1),
     *                otherObjectTypes     (2) },
     *                        -- otherObjectTypes MUST NOT
     *                        -- be used in this profile
     *        otherObjectTypeID   OBJECT IDENTIFIER OPTIONAL,
     *        digestAlgorithm     AlgorithmIdentifier,
     *        objectDigest        BIT STRING
     *   }
     *
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(digestedObjectType);

        if (otherObjectTypeID != null)
        {
            v.add(otherObjectTypeID);
        }

        v.add(digestAlgorithm);
        v.add(objectDigest);

        return new DERSequence(v);
    }
}
