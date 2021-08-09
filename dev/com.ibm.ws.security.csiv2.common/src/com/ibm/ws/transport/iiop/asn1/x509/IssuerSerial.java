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
import com.ibm.ws.transport.iiop.asn1.DERInteger;
import com.ibm.ws.transport.iiop.asn1.DERObject;
import com.ibm.ws.transport.iiop.asn1.DERSequence;

public class IssuerSerial
                extends ASN1Encodable
{
    GeneralNames issuer;
    DERInteger serial;
    DERBitString issuerUID;

    public static IssuerSerial getInstance(
                                           Object obj)
    {
        if (obj == null || obj instanceof IssuerSerial)
        {
            return (IssuerSerial) obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new IssuerSerial((ASN1Sequence) obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    public static IssuerSerial getInstance(
                                           ASN1TaggedObject obj,
                                           boolean explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public IssuerSerial(
                        ASN1Sequence seq)
    {
        issuer = GeneralNames.getInstance(seq.getObjectAt(0));
        serial = (DERInteger) seq.getObjectAt(1);

        if (seq.size() == 3)
        {
            issuerUID = (DERBitString) seq.getObjectAt(2);
        }
    }

    public IssuerSerial(
                        GeneralNames issuer,
                        DERInteger serial)
    {
        this.issuer = issuer;
        this.serial = serial;
    }

    public GeneralNames getIssuer()
    {
        return issuer;
    }

    public DERInteger getSerial()
    {
        return serial;
    }

    public DERBitString getIssuerUID()
    {
        return issuerUID;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * IssuerSerial ::= SEQUENCE {
     * issuer GeneralNames,
     * serial CertificateSerialNumber,
     * issuerUID UniqueIdentifier OPTIONAL
     * }
     * </pre>
     */
    @Override
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(issuer);
        v.add(serial);

        if (issuerUID != null)
        {
            v.add(issuerUID);
        }

        return new DERSequence(v);
    }
}
