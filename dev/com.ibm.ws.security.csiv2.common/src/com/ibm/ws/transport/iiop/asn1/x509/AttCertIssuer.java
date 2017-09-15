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
import com.ibm.ws.transport.iiop.asn1.ASN1Sequence;
import com.ibm.ws.transport.iiop.asn1.ASN1TaggedObject;
import com.ibm.ws.transport.iiop.asn1.DERObject;
import com.ibm.ws.transport.iiop.asn1.DERTaggedObject;

public class AttCertIssuer
    extends ASN1Encodable
    implements ASN1Choice
{
    ASN1Encodable   obj;
    DERObject       choiceObj;

    public static AttCertIssuer getInstance(
        Object  obj)
    {
        if (obj instanceof AttCertIssuer)
        {
            return (AttCertIssuer)obj;
        }
        else if (obj instanceof V2Form)
        {
            return new AttCertIssuer(V2Form.getInstance(obj));
        }
        else if (obj instanceof GeneralNames)
        {
            return new AttCertIssuer((GeneralNames)obj);
        }
        else if (obj instanceof ASN1TaggedObject)
        {
            return new AttCertIssuer(V2Form.getInstance((ASN1TaggedObject)obj, false));
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new AttCertIssuer(GeneralNames.getInstance(obj));
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass());
    }

    public static AttCertIssuer getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(obj.getObject()); // must be explictly tagged
    }

    /**
     * Don't use this one if you are trying to be RFC compliant.
     *
     * @param names our GeneralNames structure
     */
    public AttCertIssuer(
        GeneralNames  names)
    {
        obj = names;
        choiceObj = obj.getDERObject();
    }

    public AttCertIssuer(
        V2Form  v2Form)
    {
        obj = v2Form;
        choiceObj = new DERTaggedObject(false, 0, obj);
    }

    public ASN1Encodable getIssuer()
    {
        return obj;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     *  AttCertIssuer ::= CHOICE {
     *       v1Form   GeneralNames,  -- MUST NOT be used in this
     *                               -- profile
     *       v2Form   [0] V2Form     -- v2 only
     *  }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        return choiceObj;
    }
}
