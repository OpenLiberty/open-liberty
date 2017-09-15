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
import com.ibm.ws.transport.iiop.asn1.DERBitString;
import com.ibm.ws.transport.iiop.asn1.DERGeneralizedTime;
import com.ibm.ws.transport.iiop.asn1.DERInteger;
import com.ibm.ws.transport.iiop.asn1.DERObjectIdentifier;
import com.ibm.ws.transport.iiop.asn1.DERSequence;
import com.ibm.ws.transport.iiop.asn1.DERSet;

/**
 * Generator for Version 2 AttributeCertificateInfo
 * <pre>
 * AttributeCertificateInfo ::= SEQUENCE {
 *       version              AttCertVersion -- version is v2,
 *       holder               Holder,
 *       issuer               AttCertIssuer,
 *       signature            AlgorithmIdentifier,
 *       serialNumber         CertificateSerialNumber,
 *       attrCertValidityPeriod   AttCertValidityPeriod,
 *       attributes           SEQUENCE OF Attribute,
 *       issuerUniqueID       UniqueIdentifier OPTIONAL,
 *       extensions           Extensions OPTIONAL
 * }
 * </pre>
 *
 */
public class V2AttributeCertificateInfoGenerator
{
    private DERInteger version;
    private Holder holder;
    private AttCertIssuer issuer;
    private AlgorithmIdentifier signature;
    private DERInteger serialNumber;
    private AttCertValidityPeriod attrCertValidityPeriod;
    private ASN1EncodableVector attributes;
    private DERBitString issuerUniqueID;
    private X509Extensions extensions;
    private DERGeneralizedTime startDate, endDate;

    public V2AttributeCertificateInfoGenerator()
    {
        this.version = new DERInteger(1);
        attributes = new ASN1EncodableVector();
    }

    public void setHolder(Holder holder)
    {
        this.holder = holder;
    }

    public void addAttribute(String oid, ASN1Encodable value)
    {
        attributes.add(new Attribute(new DERObjectIdentifier(oid), new DERSet(value)));
    }

    /**
     * @param attribute
     */
    public void addAttribute(Attribute attribute)
    {
        attributes.add(attribute);
    }

    public void setSerialNumber(
        DERInteger  serialNumber)
    {
        this.serialNumber = serialNumber;
    }

    public void setSignature(
        AlgorithmIdentifier    signature)
    {
        this.signature = signature;
    }

    public void setIssuer(
        AttCertIssuer    issuer)
    {
        this.issuer = issuer;
    }

    public void setStartDate(
        DERGeneralizedTime startDate)
    {
        this.startDate = startDate;
    }

    public void setEndDate(
        DERGeneralizedTime endDate)
    {
        this.endDate = endDate;
    }

    public void setIssuerUniqueID(
        DERBitString    issuerUniqueID)
    {
        this.issuerUniqueID = issuerUniqueID;
    }

    public void setExtensions(
        X509Extensions    extensions)
    {
        this.extensions = extensions;
    }

    public AttributeCertificateInfo generateAttributeCertificateInfo()
    {
        if ((serialNumber == null) || (signature == null)
            || (issuer == null) || (startDate == null) || (endDate == null)
            || (holder == null) || (attributes == null))
        {
            throw new IllegalStateException("not all mandatory fields set in V2 AttributeCertificateInfo generator");
        }

        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(version);
        v.add(holder);
        v.add(issuer);
        v.add(signature);
        v.add(serialNumber);

        //
        // before and after dates => AttCertValidityPeriod
        //
        AttCertValidityPeriod validity = new AttCertValidityPeriod(startDate, endDate);
        v.add(validity);

        // Attributes
        v.add(new DERSequence(attributes));

        if (issuerUniqueID != null)
        {
            v.add(issuerUniqueID);
        }

        if (extensions != null)
        {
            v.add(extensions);
        }

        return new AttributeCertificateInfo(new DERSequence(v));
    }
}
