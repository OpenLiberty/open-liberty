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

import com.ibm.ws.transport.iiop.asn1.ASN1Choice;
import com.ibm.ws.transport.iiop.asn1.ASN1Encodable;
import com.ibm.ws.transport.iiop.asn1.ASN1Set;
import com.ibm.ws.transport.iiop.asn1.ASN1TaggedObject;
import com.ibm.ws.transport.iiop.asn1.DEREncodable;
import com.ibm.ws.transport.iiop.asn1.DERObject;
import com.ibm.ws.transport.iiop.asn1.DERTaggedObject;

/**
 * The DistributionPointName object.
 * <pre>
 * DistributionPointName ::= CHOICE {
 *     fullName                 [0] GeneralNames,
 *     nameRelativeToCRLIssuer  [1] RelativeDistinguishedName
 * }
 * </pre>
 */
public class DistributionPointName
    extends ASN1Encodable
    implements ASN1Choice
{
    DEREncodable        name;
    int                 type;

    public static final int FULL_NAME = 0;
    public static final int NAME_RELATIVE_TO_CRL_ISSUER = 1;

    public static DistributionPointName getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1TaggedObject.getInstance(obj, true));
    }

    public static DistributionPointName getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof DistributionPointName)
        {
            return (DistributionPointName)obj;
        }
        else if (obj instanceof ASN1TaggedObject)
        {
            return new DistributionPointName((ASN1TaggedObject)obj);
        }

        throw new IllegalArgumentException("unknown object in factory");
    }

    /*
     * @deprecated use ASN1Encodable
     */
    public DistributionPointName(
        int             type,
        DEREncodable    name)
    {
        this.type = type;
        this.name = name;
    }

    public DistributionPointName(
        int             type,
        ASN1Encodable   name)
    {
        this.type = type;
        this.name = name;
    }

    /**
     * Return the tag number applying to the underlying choice.
     *
     * @return the tag number for this point name.
     */
    public int getType()
    {
        return this.type;
    }

    /**
     * Return the tagged object inside the distribution point name.
     *
     * @return the underlying choice item.
     */
    public ASN1Encodable getName()
    {
        return (ASN1Encodable)name;
    }

    public DistributionPointName(
        ASN1TaggedObject    obj)
    {
        this.type = obj.getTagNo();

        if (type == 0)
        {
            this.name = GeneralNames.getInstance(obj, false);
        }
        else
        {
            this.name = ASN1Set.getInstance(obj, false);
        }
    }

    public DERObject toASN1Object()
    {
        return new DERTaggedObject(false, type, name);
    }
}
