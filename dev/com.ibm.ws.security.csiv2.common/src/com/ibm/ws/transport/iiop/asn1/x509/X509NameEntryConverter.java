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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.ibm.ws.transport.iiop.asn1.ASN1InputStream;
import com.ibm.ws.transport.iiop.asn1.DERObject;
import com.ibm.ws.transport.iiop.asn1.DERObjectIdentifier;

/**
 * It turns out that the number of standard ways the fields in a DN should be
 * encoded into their ASN.1 counterparts is rapidly approaching the
 * number of machines on the internet. By default the X509Name class
 * will produce PrintableStrings if the field value will decode to that,
 * next UTF8Strings if the field value will decode to that, and finally BMPStrings
 * if 16 bit characters are required.
 * <p>
 * The way this is done is with a default encoder which is
 * implemented as follows:
 * <pre>
 * public class X509DefaultEntryConverter
 *     extends X509NameEntryConverter
 * {
 *     public DERObject getConvertedValue(
 *         DERObjectIdentifier  oid,
 *         String               value)
 *     {
 *         if (str.length() != 0 && str.charAt(0) == '#')
 *         {
 *             return convertHexEncoded(str, 1);
 *         }
 *         if (oid.equals(EmailAddress))
 *         {
 *             return new DERIA5String(str);
 *         }
 *         else if (canBePrintable(str))
 *         {
 *             return new DERPrintableString(str);
 *         }
 *         else if (canBeUTF8(str))
 *         {
 *             return new DERUTF8String(str);
 *         }
 *         else
 *         {
 *             return new DERBMPString(str);
 *         }
 *     }
 * }
 */
public abstract class X509NameEntryConverter
{
    /**
     * Convert an inline encoded hex string rendition of an ASN.1
     * object back into its corresponding ASN.1 object.
     *
     * @param str the hex encoded object
     * @param off the index at which the encoding starts
     * @return the decoded object
     */
    protected DERObject convertHexEncoded(
        String  str,
        int     off)
        throws IOException
    {
        str = str.toLowerCase();
        byte[]    data = new byte[str.length() / 2];
        for (int index = 0; index != data.length; index++)
        {
            char left = str.charAt((index * 2) + off);
            char right = str.charAt((index * 2) + off + 1);

            if (left < 'a')
            {
                data[index] = (byte)((left - '0') << 4);
            }
            else
            {
                data[index] = (byte)((left - 'a' + 10) << 4);
            }
            if (right < 'a')
            {
                data[index] |= (byte)(right - '0');
            }
            else
            {
                data[index] |= (byte)(right - 'a' + 10);
            }
        }

        ASN1InputStream aIn = new ASN1InputStream(
                                            new ByteArrayInputStream(data));

        return aIn.readObject();
    }

    /**
     * return true if the passed in String can be represented without
     * loss as a PrintableString, false otherwise.
     */
    protected boolean canBePrintable(
        String  str)
    {
        for (int i = str.length() - 1; i >= 0; i--)
        {
            char    ch = str.charAt(i);

            if (str.charAt(i) > 0x007f)
            {
                return false;
            }

            if ('a' <= ch && ch <= 'z')
            {
                continue;
            }

            if ('A' <= ch && ch <= 'Z')
            {
                continue;
            }

            if ('0' <= ch && ch <= '9')
            {
                continue;
            }

            switch (ch)
            {
            case ' ':
            case '\'':
            case '(':
            case ')':
            case '+':
            case '-':
            case '.':
            case ':':
            case '=':
            case '?':
                continue;
            }

            return false;
        }

        return true;
    }

    /**
     * return true if the passed in String can be represented without
     * loss as a UTF8String, false otherwise.
     */
    protected boolean canBeUTF8(
        String  str)
    {
        for (int i = str.length() - 1; i >= 0; i--)
        {
            if (str.charAt(i) > 0x00ff)
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Convert the passed in String value into the appropriate ASN.1
     * encoded object.
     *
     * @param oid the oid associated with the value in the DN.
     * @param value the value of the particular DN component.
     * @return the ASN.1 equivalent for the value.
     */
    public abstract DERObject getConvertedValue(DERObjectIdentifier oid, String value);
}
