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
import com.ibm.ws.transport.iiop.asn1.DERObject;
import com.ibm.ws.transport.iiop.asn1.DERSequence;
import com.ibm.ws.transport.iiop.asn1.DERTaggedObject;

/**
 * The DistributionPoint object.
 * <pre>
 * DistributionPoint ::= SEQUENCE {
 *      distributionPoint [0] DistributionPointName OPTIONAL,
 *      reasons           [1] ReasonFlags OPTIONAL,
 *      cRLIssuer         [2] GeneralNames OPTIONAL
 * }
 * </pre>
 */
public class DistributionPoint
    extends ASN1Encodable
{
    DistributionPointName       distributionPoint;
    ReasonFlags                 reasons;
    GeneralNames                cRLIssuer;

    public static DistributionPoint getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static DistributionPoint getInstance(
        Object obj)
    {
        if(obj == null || obj instanceof DistributionPoint)
        {
            return (DistributionPoint)obj;
        }

        if(obj instanceof ASN1Sequence)
        {
            return new DistributionPoint((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("Invalid DistributionPoint: " + obj.getClass().getName());
    }

    public DistributionPoint(
        ASN1Sequence seq)
    {
        for (int i = 0; i != seq.size(); i++)
        {
            ASN1TaggedObject    t = (ASN1TaggedObject)seq.getObjectAt(i);
            switch (t.getTagNo())
            {
            case 0:
                distributionPoint = DistributionPointName.getInstance(t, true);
                break;
            case 1:
                reasons = new ReasonFlags(DERBitString.getInstance(t, false));
                break;
            case 2:
                cRLIssuer = GeneralNames.getInstance(t, false);
            }
        }
    }

    public DistributionPoint(
        DistributionPointName distributionPoint,
        ReasonFlags                 reasons,
        GeneralNames            cRLIssuer)
    {
        this.distributionPoint = distributionPoint;
        this.reasons = reasons;
        this.cRLIssuer = cRLIssuer;
    }

    public DistributionPointName getDistributionPoint()
    {
        return distributionPoint;
    }

    public ReasonFlags getReasons()
    {
        return reasons;
    }

    public GeneralNames getCRLIssuer()
    {
        return cRLIssuer;
    }

    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        if (distributionPoint != null)
        {
            //
            // as this is a CHOICE it must be explicitly tagged
            //
            v.add(new DERTaggedObject(0, distributionPoint));
        }

        if (reasons != null)
        {
            v.add(new DERTaggedObject(false, 1, reasons));
        }

        if (cRLIssuer != null)
        {
            v.add(new DERTaggedObject(false, 2, cRLIssuer));
        }

        return new DERSequence(v);
    }
}
