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

import java.util.Enumeration;

import com.ibm.ws.transport.iiop.asn1.ASN1Encodable;
import com.ibm.ws.transport.iiop.asn1.ASN1EncodableVector;
import com.ibm.ws.transport.iiop.asn1.ASN1Sequence;
import com.ibm.ws.transport.iiop.asn1.ASN1TaggedObject;
import com.ibm.ws.transport.iiop.asn1.DERObject;
import com.ibm.ws.transport.iiop.asn1.DERSequence;
import com.ibm.ws.transport.iiop.asn1.DERTaggedObject;

public class NameConstraints
    extends ASN1Encodable
{
    ASN1Sequence    permitted, excluded;

    public NameConstraints(
        ASN1Sequence    seq)
    {
        Enumeration e = seq.getObjects();
        while (e.hasMoreElements())
        {
            ASN1TaggedObject    o = (ASN1TaggedObject)e.nextElement();
            switch (o.getTagNo())
            {
            case 0:
                permitted = ASN1Sequence.getInstance(o, false);
                break;
            case 1:
                excluded = ASN1Sequence.getInstance(o, false);
                break;
            }
        }
    }

    public ASN1Sequence getPermittedSubtrees()
    {
        return permitted;
    }

    public ASN1Sequence getExcludedSubtrees()
    {
        return excluded;
    }

    /*
     * NameConstraints ::= SEQUENCE {
     *      permittedSubtrees       [0]     GeneralSubtrees OPTIONAL,
     *      excludedSubtrees        [1]     GeneralSubtrees OPTIONAL }
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector   v = new ASN1EncodableVector();

        if (permitted != null)
        {
            v.add(new DERTaggedObject(false, 0, permitted));
        }

        if (excluded != null)
        {
            v.add(new DERTaggedObject(false, 1, excluded));
        }

        return new DERSequence(v);
    }
}
