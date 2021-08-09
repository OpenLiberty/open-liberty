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
import com.ibm.ws.transport.iiop.asn1.DERGeneralizedTime;
import com.ibm.ws.transport.iiop.asn1.DERInteger;
import com.ibm.ws.transport.iiop.asn1.DERObject;
import com.ibm.ws.transport.iiop.asn1.DERTaggedObject;
import com.ibm.ws.transport.iiop.asn1.DERUTCTime;

/**
 * PKIX RFC-2459 - TBSCertList object.
 * <pre>
 * TBSCertList  ::=  SEQUENCE  {
 *      version                 Version OPTIONAL,
 *                                   -- if present, shall be v2
 *      signature               AlgorithmIdentifier,
 *      issuer                  Name,
 *      thisUpdate              Time,
 *      nextUpdate              Time OPTIONAL,
 *      revokedCertificates     SEQUENCE OF SEQUENCE  {
 *           userCertificate         CertificateSerialNumber,
 *           revocationDate          Time,
 *           crlEntryExtensions      Extensions OPTIONAL
 *                                         -- if present, shall be v2
 *                                }  OPTIONAL,
 *      crlExtensions           [0]  EXPLICIT Extensions OPTIONAL
 *                                         -- if present, shall be v2
 *                                }
 * </pre>
 */
public class TBSCertList
    extends ASN1Encodable
{
    public class CRLEntry
        extends ASN1Encodable
    {
        ASN1Sequence  seq;

        DERInteger          userCertificate;
        Time                revocationDate;
        X509Extensions      crlEntryExtensions;

        public CRLEntry(
            ASN1Sequence  seq)
        {
            this.seq = seq;

            userCertificate = (DERInteger)seq.getObjectAt(0);
            revocationDate = Time.getInstance(seq.getObjectAt(1));
            if (seq.size() == 3)
            {
                crlEntryExtensions = X509Extensions.getInstance(seq.getObjectAt(2));
            }
        }

        public DERInteger getUserCertificate()
        {
            return userCertificate;
        }

        public Time getRevocationDate()
        {
            return revocationDate;
        }

        public X509Extensions getExtensions()
        {
            return crlEntryExtensions;
        }

        public DERObject toASN1Object()
        {
            return seq;
        }
    }

    ASN1Sequence     seq;

    DERInteger              version;
    AlgorithmIdentifier     signature;
    X509Name                issuer;
    Time                    thisUpdate;
    Time                    nextUpdate;
    CRLEntry[]              revokedCertificates;
    X509Extensions          crlExtensions;

    public static TBSCertList getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static TBSCertList getInstance(
        Object  obj)
    {
        if (obj instanceof TBSCertList)
        {
            return (TBSCertList)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new TBSCertList((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory");
    }

    public TBSCertList(
        ASN1Sequence  seq)
    {
        int seqPos = 0;

        this.seq = seq;

        if (seq.getObjectAt(seqPos) instanceof DERInteger)
        {
            version = (DERInteger)seq.getObjectAt(seqPos++);
        }
        else
        {
            version = new DERInteger(0);
        }

        signature = AlgorithmIdentifier.getInstance(seq.getObjectAt(seqPos++));
        issuer = X509Name.getInstance(seq.getObjectAt(seqPos++));
        thisUpdate = Time.getInstance(seq.getObjectAt(seqPos++));

        if (seqPos < seq.size()
            && (seq.getObjectAt(seqPos) instanceof DERUTCTime
               || seq.getObjectAt(seqPos) instanceof DERGeneralizedTime
               || seq.getObjectAt(seqPos) instanceof Time))
        {
            nextUpdate = Time.getInstance(seq.getObjectAt(seqPos++));
        }

        if (seqPos < seq.size()
            && !(seq.getObjectAt(seqPos) instanceof DERTaggedObject))
        {
            ASN1Sequence certs = (ASN1Sequence)seq.getObjectAt(seqPos++);
            revokedCertificates = new CRLEntry[certs.size()];

            for ( int i = 0; i < revokedCertificates.length; i++)
            {
                revokedCertificates[i] = new CRLEntry((ASN1Sequence)certs.getObjectAt(i));
            }
        }

        if (seqPos < seq.size()
            && seq.getObjectAt(seqPos) instanceof DERTaggedObject)
        {
            crlExtensions = X509Extensions.getInstance(seq.getObjectAt(seqPos++));
        }
    }

    public int getVersion()
    {
        return version.getValue().intValue() + 1;
    }

    public DERInteger getVersionNumber()
    {
        return version;
    }

    public AlgorithmIdentifier getSignature()
    {
        return signature;
    }

    public X509Name getIssuer()
    {
        return issuer;
    }

    public Time getThisUpdate()
    {
        return thisUpdate;
    }

    public Time getNextUpdate()
    {
        return nextUpdate;
    }

    public CRLEntry[] getRevokedCertificates()
    {
        return revokedCertificates;
    }

    public X509Extensions getExtensions()
    {
        return crlExtensions;
    }

    public DERObject toASN1Object()
    {
        return seq;
    }
}
