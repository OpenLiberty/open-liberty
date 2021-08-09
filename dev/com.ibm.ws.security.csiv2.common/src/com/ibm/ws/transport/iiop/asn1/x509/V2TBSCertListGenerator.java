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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import com.ibm.ws.transport.iiop.asn1.ASN1EncodableVector;
import com.ibm.ws.transport.iiop.asn1.ASN1Sequence;
import com.ibm.ws.transport.iiop.asn1.DERGeneralizedTime;
import com.ibm.ws.transport.iiop.asn1.DERInteger;
import com.ibm.ws.transport.iiop.asn1.DEROctetString;
import com.ibm.ws.transport.iiop.asn1.DERSequence;
import com.ibm.ws.transport.iiop.asn1.DERTaggedObject;
import com.ibm.ws.transport.iiop.asn1.DERUTCTime;

/**
 * Generator for Version 2 TBSCertList structures.
 * <pre>
 *  TBSCertList  ::=  SEQUENCE  {
 *       version                 Version OPTIONAL,
 *                                    -- if present, shall be v2
 *       signature               AlgorithmIdentifier,
 *       issuer                  Name,
 *       thisUpdate              Time,
 *       nextUpdate              Time OPTIONAL,
 *       revokedCertificates     SEQUENCE OF SEQUENCE  {
 *            userCertificate         CertificateSerialNumber,
 *            revocationDate          Time,
 *            crlEntryExtensions      Extensions OPTIONAL
 *                                          -- if present, shall be v2
 *                                 }  OPTIONAL,
 *       crlExtensions           [0]  EXPLICIT Extensions OPTIONAL
 *                                          -- if present, shall be v2
 *                                 }
 * </pre>
 *
 * <b>Note: This class may be subject to change</b>
 */
public class V2TBSCertListGenerator
{
    DERInteger version = new DERInteger(1);

    AlgorithmIdentifier     signature;
    X509Name                issuer;
    Time                    thisUpdate, nextUpdate=null;
    X509Extensions          extensions=null;
    private Vector          crlentries=null;

    public V2TBSCertListGenerator()
    {
    }


    public void setSignature(
        AlgorithmIdentifier    signature)
    {
        this.signature = signature;
    }

    public void setIssuer(
        X509Name    issuer)
    {
        this.issuer = issuer;
    }

    public void setThisUpdate(
        DERUTCTime thisUpdate)
    {
        this.thisUpdate = new Time(thisUpdate);
    }

    public void setNextUpdate(
        DERUTCTime nextUpdate)
    {
        this.nextUpdate = new Time(nextUpdate);
    }

    public void setThisUpdate(
        Time thisUpdate)
    {
        this.thisUpdate = thisUpdate;
    }

    public void setNextUpdate(
        Time nextUpdate)
    {
        this.nextUpdate = nextUpdate;
    }

    public void addCRLEntry(
        ASN1Sequence crlEntry)
    {
        if (crlentries == null)
            crlentries = new Vector();
        crlentries.addElement(crlEntry);
    }

    public void addCRLEntry(DERInteger userCertificate, DERUTCTime revocationDate, int reason)
    {
        addCRLEntry(userCertificate, new Time(revocationDate), reason);
    }

    public void addCRLEntry(DERInteger userCertificate, Time revocationDate, int reason)
    {
        addCRLEntry(userCertificate, revocationDate, reason, null);
    }

    public void addCRLEntry(DERInteger userCertificate, Time revocationDate, int reason, DERGeneralizedTime invalidityDate)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(userCertificate);
        v.add(revocationDate);

        Vector extOids = new Vector();
        Vector extValues = new Vector();

        if (reason != 0)
        {
            CRLReason crlReason = new CRLReason(reason);

            try
            {
                extOids.addElement(X509Extensions.ReasonCode);
                extValues.addElement(new X509Extension(false, new DEROctetString(crlReason.getEncoded())));
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException("error encoding reason: " + e.getMessage(), e);
            }
        }

        if (invalidityDate != null)
        {
            try
            {
                extOids.addElement(X509Extensions.InvalidityDate);
                extValues.addElement(new X509Extension(false, new DEROctetString(invalidityDate.getEncoded())));
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException("error encoding invalidityDate: " + e.getMessage(), e);
            }
        }

        if (extOids.size() != 0)
        {
            X509Extensions ex = new X509Extensions(extOids, extValues);
            v.add(ex);
        }

        if (crlentries == null)
        {
            crlentries = new Vector();
        }

        crlentries.addElement(new DERSequence(v));
    }

    public void setExtensions(
        X509Extensions    extensions)
    {
        this.extensions = extensions;
    }

    public TBSCertList generateTBSCertList()
    {
        if ((signature == null) || (issuer == null) || (thisUpdate == null))
        {
            throw new IllegalStateException("Not all mandatory fields set in V2 TBSCertList generator.");
        }

        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(version);
        v.add(signature);
        v.add(issuer);

        v.add(thisUpdate);
        if (nextUpdate != null)
        {
            v.add(nextUpdate);
        }

        // Add CRLEntries if they exist
        if (crlentries != null)
        {
            ASN1EncodableVector certs = new ASN1EncodableVector();
            Enumeration it = crlentries.elements();
            while( it.hasMoreElements() )
            {
                certs.add((ASN1Sequence)it.nextElement());
            }
            v.add(new DERSequence(certs));
        }

        if (extensions != null)
        {
            v.add(new DERTaggedObject(0, extensions));
        }

        return new TBSCertList(new DERSequence(v));
    }
}
