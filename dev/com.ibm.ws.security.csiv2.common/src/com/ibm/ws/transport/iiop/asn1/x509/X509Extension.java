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

import com.ibm.ws.transport.iiop.asn1.ASN1OctetString;
import com.ibm.ws.transport.iiop.asn1.DERBoolean;

/**
 * an object for the elements in the X.509 V3 extension block.
 */
public class X509Extension
{
    boolean             critical;
    ASN1OctetString      value;

    public X509Extension(
        DERBoolean              critical,
        ASN1OctetString         value)
    {
        this.critical = critical.isTrue();
        this.value = value;
    }

    public X509Extension(
        boolean                 critical,
        ASN1OctetString         value)
    {
        this.critical = critical;
        this.value = value;
    }

    public boolean isCritical()
    {
        return critical;
    }

    public ASN1OctetString getValue()
    {
        return value;
    }

    public int hashCode()
    {
        if (this.isCritical())
        {
            return this.getValue().hashCode();
        }


        return ~this.getValue().hashCode();
    }

    public boolean equals(
        Object  o)
    {
        if (o == null || !(o instanceof X509Extension))
        {
            return false;
        }

        X509Extension   other = (X509Extension)o;

        return other.getValue().equals(this.getValue())
            && (other.isCritical() == this.isCritical());
    }
}
