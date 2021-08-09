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

package com.ibm.ws.transport.iiop.asn1.util;

import java.util.Enumeration;

import com.ibm.ws.transport.iiop.asn1.ASN1OctetString;
import com.ibm.ws.transport.iiop.asn1.ASN1Sequence;
import com.ibm.ws.transport.iiop.asn1.ASN1Set;
import com.ibm.ws.transport.iiop.asn1.BERConstructedOctetString;
import com.ibm.ws.transport.iiop.asn1.BERConstructedSequence;
import com.ibm.ws.transport.iiop.asn1.BERSequence;
import com.ibm.ws.transport.iiop.asn1.BERSet;
import com.ibm.ws.transport.iiop.asn1.BERTaggedObject;
import com.ibm.ws.transport.iiop.asn1.DERBMPString;
import com.ibm.ws.transport.iiop.asn1.DERBitString;
import com.ibm.ws.transport.iiop.asn1.DERBoolean;
import com.ibm.ws.transport.iiop.asn1.DERConstructedSequence;
import com.ibm.ws.transport.iiop.asn1.DERConstructedSet;
import com.ibm.ws.transport.iiop.asn1.DEREncodable;
import com.ibm.ws.transport.iiop.asn1.DERGeneralizedTime;
import com.ibm.ws.transport.iiop.asn1.DERIA5String;
import com.ibm.ws.transport.iiop.asn1.DERInteger;
import com.ibm.ws.transport.iiop.asn1.DERNull;
import com.ibm.ws.transport.iiop.asn1.DERObject;
import com.ibm.ws.transport.iiop.asn1.DERObjectIdentifier;
import com.ibm.ws.transport.iiop.asn1.DEROctetString;
import com.ibm.ws.transport.iiop.asn1.DERPrintableString;
import com.ibm.ws.transport.iiop.asn1.DERSequence;
import com.ibm.ws.transport.iiop.asn1.DERSet;
import com.ibm.ws.transport.iiop.asn1.DERT61String;
import com.ibm.ws.transport.iiop.asn1.DERTaggedObject;
import com.ibm.ws.transport.iiop.asn1.DERUTCTime;
import com.ibm.ws.transport.iiop.asn1.DERUTF8String;
import com.ibm.ws.transport.iiop.asn1.DERUnknownTag;
import com.ibm.ws.transport.iiop.asn1.DERVisibleString;

public class ASN1Dump
{
    private static String TAB = "    ";

    /**
     * dump a DER object as a formatted string with indentation
     * 
     * @param obj the DERObject to be dumped out.
     */
    static String _dumpAsString(
                                String indent,
                                DERObject obj)
    {
        if (obj instanceof ASN1Sequence)
        {
            StringBuilder buf = new StringBuilder();
            Enumeration e = ((ASN1Sequence) obj).getObjects();
            String tab = indent + TAB;

            buf.append(indent);
            if (obj instanceof BERConstructedSequence)
            {
                buf.append("BER ConstructedSequence");
            }
            else if (obj instanceof DERConstructedSequence)
            {
                buf.append("DER ConstructedSequence");
            }
            else if (obj instanceof BERSequence)
            {
                buf.append("BER Sequence");
            }
            else if (obj instanceof DERSequence)
            {
                buf.append("DER Sequence");
            }
            else
            {
                buf.append("Sequence");
            }

            buf.append(System.getProperty("line.separator"));

            while (e.hasMoreElements())
            {
                Object o = e.nextElement();

                if (o == null || o.equals(new DERNull()))
                {
                    buf.append(tab);
                    buf.append("NULL");
                    buf.append(System.getProperty("line.separator"));
                }
                else if (o instanceof DERObject)
                {
                    buf.append(_dumpAsString(tab, (DERObject) o));
                }
                else
                {
                    buf.append(_dumpAsString(tab, ((DEREncodable) o).getDERObject()));
                }
            }
            return buf.toString();
        }
        else if (obj instanceof DERTaggedObject)
        {
            StringBuilder buf = new StringBuilder();
            String tab = indent + TAB;

            buf.append(indent);
            if (obj instanceof BERTaggedObject)
            {
                buf.append("BER Tagged [");
            }
            else
            {
                buf.append("Tagged [");
            }

            DERTaggedObject o = (DERTaggedObject) obj;

            buf.append(Integer.toString(o.getTagNo()));
            buf.append("]");

            if (!o.isExplicit())
            {
                buf.append(" IMPLICIT ");
            }

            buf.append(System.getProperty("line.separator"));

            if (o.isEmpty())
            {
                buf.append(tab);
                buf.append("EMPTY");
                buf.append(System.getProperty("line.separator"));
            }
            else
            {
                buf.append(_dumpAsString(tab, o.getObject()));
            }

            return buf.toString();
        }
        else if (obj instanceof DERConstructedSet)
        {
            StringBuilder buf = new StringBuilder();
            Enumeration e = ((ASN1Set) obj).getObjects();
            String tab = indent + TAB;

            buf.append(indent);
            buf.append("ConstructedSet");
            buf.append(System.getProperty("line.separator"));

            while (e.hasMoreElements())
            {
                Object o = e.nextElement();

                if (o == null)
                {
                    buf.append(tab);
                    buf.append("NULL");
                    buf.append(System.getProperty("line.separator"));
                }
                else if (o instanceof DERObject)
                {
                    buf.append(_dumpAsString(tab, (DERObject) o));
                }
                else
                {
                    buf.append(_dumpAsString(tab, ((DEREncodable) o).getDERObject()));
                }
            }
            return buf.toString();
        }
        else if (obj instanceof BERSet)
        {
            StringBuilder buf = new StringBuilder();
            Enumeration e = ((ASN1Set) obj).getObjects();
            String tab = indent + TAB;

            buf.append(indent);
            buf.append("BER Set");
            buf.append(System.getProperty("line.separator"));

            while (e.hasMoreElements())
            {
                Object o = e.nextElement();

                if (o == null)
                {
                    buf.append(tab);
                    buf.append("NULL");
                    buf.append(System.getProperty("line.separator"));
                }
                else if (o instanceof DERObject)
                {
                    buf.append(_dumpAsString(tab, (DERObject) o));
                }
                else
                {
                    buf.append(_dumpAsString(tab, ((DEREncodable) o).getDERObject()));
                }
            }
            return buf.toString();
        }
        else if (obj instanceof DERSet)
        {
            StringBuilder buf = new StringBuilder();
            Enumeration e = ((ASN1Set) obj).getObjects();
            String tab = indent + TAB;

            buf.append(indent);
            buf.append("DER Set");
            buf.append(System.getProperty("line.separator"));

            while (e.hasMoreElements())
            {
                Object o = e.nextElement();

                if (o == null)
                {
                    buf.append(tab);
                    buf.append("NULL");
                    buf.append(System.getProperty("line.separator"));
                }
                else if (o instanceof DERObject)
                {
                    buf.append(_dumpAsString(tab, (DERObject) o));
                }
                else
                {
                    buf.append(_dumpAsString(tab, ((DEREncodable) o).getDERObject()));
                }
            }
            return buf.toString();
        }
        else if (obj instanceof DERObjectIdentifier)
        {
            return indent + "ObjectIdentifier(" + ((DERObjectIdentifier) obj).getId() + ")" + System.getProperty("line.separator");
        }
        else if (obj instanceof DERBoolean)
        {
            return indent + "Boolean(" + ((DERBoolean) obj).isTrue() + ")" + System.getProperty("line.separator");
        }
        else if (obj instanceof DERInteger)
        {
            return indent + "Integer(" + ((DERInteger) obj).getValue() + ")" + System.getProperty("line.separator");
        }
        else if (obj instanceof BERConstructedOctetString)
        {
            return indent + "BER Constructed Octet String" + "[" + ((ASN1OctetString) obj).getOctets().length + "] " + System.getProperty("line.separator");
        }
        else if (obj instanceof DEROctetString)
        {
            return indent + "DER Octet String" + "[" + ((ASN1OctetString) obj).getOctets().length + "] " + System.getProperty("line.separator");
        }
        else if (obj instanceof DERBitString)
        {
            return indent + "DER Bit String" + "[" + ((DERBitString) obj).getBytes().length + ", " + ((DERBitString) obj).getPadBits() + "] "
                   + System.getProperty("line.separator");
        }
        else if (obj instanceof DERIA5String)
        {
            return indent + "IA5String(" + ((DERIA5String) obj).getString() + ") " + System.getProperty("line.separator");
        }
        else if (obj instanceof DERUTF8String)
        {
            return indent + "UTF8String(" + ((DERUTF8String) obj).getString() + ") " + System.getProperty("line.separator");
        }
        else if (obj instanceof DERPrintableString)
        {
            return indent + "PrintableString(" + ((DERPrintableString) obj).getString() + ") " + System.getProperty("line.separator");
        }
        else if (obj instanceof DERVisibleString)
        {
            return indent + "VisibleString(" + ((DERVisibleString) obj).getString() + ") " + System.getProperty("line.separator");
        }
        else if (obj instanceof DERBMPString)
        {
            return indent + "BMPString(" + ((DERBMPString) obj).getString() + ") " + System.getProperty("line.separator");
        }
        else if (obj instanceof DERT61String)
        {
            return indent + "T61String(" + ((DERT61String) obj).getString() + ") " + System.getProperty("line.separator");
        }
        else if (obj instanceof DERUTCTime)
        {
            return indent + "UTCTime(" + ((DERUTCTime) obj).getTime() + ") " + System.getProperty("line.separator");
        }
        else if (obj instanceof DERGeneralizedTime)
        {
            return indent + "GeneralizedTime(" + ((DERGeneralizedTime) obj).getTime() + ") " + System.getProperty("line.separator");
        }
        else if (obj instanceof DERUnknownTag)
        {
            //TODO liberty if we keep this find Hex
//            return indent + "Unknown " + Integer.toString(((DERUnknownTag)obj).getTag(), 16) + " " + new String(Hex.encode(((DERUnknownTag)obj).getData())) + System.getProperty("line.separator");
            return indent + "Unknown " + Integer.toString(((DERUnknownTag) obj).getTag(), 16) + " " + new String(((DERUnknownTag) obj).getData())
                   + System.getProperty("line.separator");
        }
        else
        {
            return indent + obj.toString() + System.getProperty("line.separator");
        }
    }

    /**
     * dump out a DER object as a formatted string
     * 
     * @param obj the DERObject to be dumped out.
     */
    public static String dumpAsString(
                                      Object obj)
    {
        if (obj instanceof DERObject)
        {
            return _dumpAsString("", (DERObject) obj);
        }
        else if (obj instanceof DEREncodable)
        {
            return _dumpAsString("", ((DEREncodable) obj).getDERObject());
        }

        return "unknown object type " + obj.toString();
    }
}
