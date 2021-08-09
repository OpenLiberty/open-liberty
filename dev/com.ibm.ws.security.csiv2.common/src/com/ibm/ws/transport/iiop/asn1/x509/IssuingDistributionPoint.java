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
import com.ibm.ws.transport.iiop.asn1.ASN1Sequence;
import com.ibm.ws.transport.iiop.asn1.ASN1TaggedObject;
import com.ibm.ws.transport.iiop.asn1.DERBoolean;
import com.ibm.ws.transport.iiop.asn1.DERObject;

/**
 * IssuingDistributionPoint ::= SEQUENCE {
 *      distributionPoint          [0] DistributionPointName OPTIONAL,
 *      onlyContainsUserCerts      [1] BOOLEAN DEFAULT FALSE,
 *      onlyContainsCACerts        [2] BOOLEAN DEFAULT FALSE,
 *      onlySomeReasons            [3] ReasonFlags OPTIONAL,
 *      indirectCRL                [4] BOOLEAN DEFAULT FALSE,
 *      onlyContainsAttributeCerts [5] BOOLEAN DEFAULT FALSE }
 */
public class IssuingDistributionPoint
    extends ASN1Encodable
{
    private boolean         onlyContainsUserCerts;
    private boolean         onlyContainsCACerts;
    private boolean         indirectCRL;
    private boolean         onlyContainsAttributeCerts;

    private ASN1Sequence    seq;

    public static IssuingDistributionPoint getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static IssuingDistributionPoint getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof IssuingDistributionPoint)
        {
            return (IssuingDistributionPoint)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new IssuingDistributionPoint((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory");
    }

    /**
     * Constructor from ASN1Sequence
     */
    public IssuingDistributionPoint(
        ASN1Sequence  seq)
    {
        this.seq = seq;

        for (int i = 0; i != seq.size(); i++)
        {
            ASN1TaggedObject  o = (ASN1TaggedObject)seq.getObjectAt(i);

            switch (o.getTagNo())
            {
            case 0:
                break;
            case 1:
                onlyContainsUserCerts = DERBoolean.getInstance(o, false).isTrue();
                break;
            case 2:
                onlyContainsCACerts = DERBoolean.getInstance(o, false).isTrue();
                break;
            case 3:
                break;
            case 4:
                indirectCRL = DERBoolean.getInstance(o, false).isTrue();
                break;
            case 5:
                onlyContainsAttributeCerts = DERBoolean.getInstance(o, false).isTrue();
                break;
            default:
                throw new IllegalArgumentException("unknown tag in IssuingDistributionPoint");
            }
        }
    }

    public boolean onlyContainsUserCerts()
    {
        return onlyContainsUserCerts;
    }

    public boolean onlyContainsCACerts()
    {
        return onlyContainsCACerts;
    }

    public boolean isIndirectCRL()
    {
        return indirectCRL;
    }

    public boolean onlyContainsAttributeCerts()
    {
        return onlyContainsAttributeCerts;
    }

    public DERObject toASN1Object()
    {
        return seq;
    }
}
