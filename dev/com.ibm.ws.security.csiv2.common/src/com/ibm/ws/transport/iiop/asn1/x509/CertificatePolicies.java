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
import java.util.Vector;

import com.ibm.ws.transport.iiop.asn1.ASN1Encodable;
import com.ibm.ws.transport.iiop.asn1.ASN1EncodableVector;
import com.ibm.ws.transport.iiop.asn1.ASN1Sequence;
import com.ibm.ws.transport.iiop.asn1.ASN1TaggedObject;
import com.ibm.ws.transport.iiop.asn1.DERObject;
import com.ibm.ws.transport.iiop.asn1.DERObjectIdentifier;
import com.ibm.ws.transport.iiop.asn1.DERSequence;

public class CertificatePolicies
                extends ASN1Encodable
{
    static final DERObjectIdentifier anyPolicy = new DERObjectIdentifier("2.5.29.32.0");

    Vector<DERObjectIdentifier> policies = new Vector<DERObjectIdentifier>();

    /**
     * @deprecated use an ASN1Sequence of PolicyInformation
     */
    @Deprecated
    public static CertificatePolicies getInstance(
                                                  ASN1TaggedObject obj,
                                                  boolean explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    /**
     * @deprecated use an ASN1Sequence of PolicyInformation
     */
    @Deprecated
    public static CertificatePolicies getInstance(
                                                  Object obj)
    {
        if (obj instanceof CertificatePolicies)
        {
            return (CertificatePolicies) obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new CertificatePolicies((ASN1Sequence) obj);
        }

        throw new IllegalArgumentException("unknown object in factory");
    }

    /**
     * @deprecated use an ASN1Sequence of PolicyInformation
     */
    @Deprecated
    public CertificatePolicies(
                               ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();
        while (e.hasMoreElements())
        {
            ASN1Sequence s = (ASN1Sequence) e.nextElement();
            policies.addElement((DERObjectIdentifier) s.getObjectAt(0));
        }
        // For now we just don't handle PolicyQualifiers
    }

    /**
     * create a certificate policy with the given OID.
     * 
     * @deprecated use an ASN1Sequence of PolicyInformation
     */
    @Deprecated
    public CertificatePolicies(
                               DERObjectIdentifier p)
    {
        policies.addElement(p);
    }

    /**
     * create a certificate policy with the policy given by the OID represented
     * by the string p.
     * 
     * @deprecated use an ASN1Sequence of PolicyInformation
     */
    @Deprecated
    public CertificatePolicies(
                               String p)
    {
        this(new DERObjectIdentifier(p));
    }

    public void addPolicy(
                          String p)
    {
        policies.addElement(new DERObjectIdentifier(p));
    }

    public String getPolicy(int nr)
    {
        if (policies.size() > nr)
            return policies.elementAt(nr).getId();

        return null;
    }

    /**
     * <pre>
     * certificatePolicies ::= SEQUENCE SIZE (1..MAX) OF PolicyInformation
     * 
     * PolicyInformation ::= SEQUENCE {
     * policyIdentifier CertPolicyId,
     * policyQualifiers SEQUENCE SIZE (1..MAX) OF
     * PolicyQualifierInfo OPTIONAL }
     * 
     * CertPolicyId ::= OBJECT IDENTIFIER
     * 
     * PolicyQualifierInfo ::= SEQUENCE {
     * policyQualifierId PolicyQualifierId,
     * qualifier ANY DEFINED BY policyQualifierId }
     * 
     * PolicyQualifierId ::=
     * OBJECT IDENTIFIER ( id-qt-cps | id-qt-unotice )
     * </pre>
     * 
     * @deprecated use an ASN1Sequence of PolicyInformation
     */
    @Deprecated
    @Override
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        // We only do policyIdentifier yet...
        for (int i = 0; i < policies.size(); i++)
        {
            v.add(new DERSequence(policies.elementAt(i)));
        }

        return new DERSequence(v);
    }

    @Override
    public String toString()
    {
        StringBuilder p = new StringBuilder("CertificatePolicies: ");
        boolean first = true;
        for (DERObjectIdentifier policy : policies)
        {
            if (!first)
                p.append(", ");
            p.append((policy).getId());
        }
        return p.toString();
    }
}
