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
import com.ibm.ws.transport.iiop.asn1.ASN1TaggedObject;
import com.ibm.ws.transport.iiop.asn1.DERBMPString;
import com.ibm.ws.transport.iiop.asn1.DERIA5String;
import com.ibm.ws.transport.iiop.asn1.DERObject;
import com.ibm.ws.transport.iiop.asn1.DERString;
import com.ibm.ws.transport.iiop.asn1.DERUTF8String;
import com.ibm.ws.transport.iiop.asn1.DERVisibleString;

/**
 * <code>DisplayText</code> class, used in
 * <code>CertificatePolicies</code> X509 V3 extensions (in policy qualifiers).
 *
 * <p>It stores a string in a chosen encoding.
 * <pre>
 * DisplayText ::= CHOICE {
 *      ia5String        IA5String      (SIZE (1..200)),
 *      visibleString    VisibleString  (SIZE (1..200)),
 *      bmpString        BMPString      (SIZE (1..200)),
 *      utf8String       UTF8String     (SIZE (1..200)) }
 * </pre>
 * @see PolicyQualifierInfo
 * @see PolicyInformation
 */
public class DisplayText
    extends ASN1Encodable
    implements ASN1Choice
{
   /**
    * Constant corresponding to ia5String encoding.
    *
    */
   public static final int CONTENT_TYPE_IA5STRING = 0;
   /**
    * Constant corresponding to bmpString encoding.
    *
    */
   public static final int CONTENT_TYPE_BMPSTRING = 1;
   /**
    * Constant corresponding to utf8String encoding.
    *
    */
   public static final int CONTENT_TYPE_UTF8STRING = 2;
   /**
    * Constant corresponding to visibleString encoding.
    *
    */
   public static final int CONTENT_TYPE_VISIBLESTRING = 3;

   /**
    * Describe constant <code>DISPLAY_TEXT_MAXIMUM_SIZE</code> here.
    *
    */
   public static final int DISPLAY_TEXT_MAXIMUM_SIZE = 200;

   int contentType;
   DERString contents;

   /**
    * Creates a new <code>DisplayText</code> instance.
    *
    * @param type the desired encoding type for the text.
    * @param text the text to store. Strings longer than 200
    * characters are truncated.
    */
   public DisplayText (int type, String text)
   {
      if (text.length() > DISPLAY_TEXT_MAXIMUM_SIZE) {
         // RFC3280 limits these strings to 200 chars
         // truncate the string
         text = text.substring (0, DISPLAY_TEXT_MAXIMUM_SIZE);
      }

      contentType = type;
      switch (type) {
         case CONTENT_TYPE_IA5STRING:
            contents = (DERString)new DERIA5String (text);
            break;
         case CONTENT_TYPE_UTF8STRING:
            contents = (DERString)new DERUTF8String(text);
            break;
         case CONTENT_TYPE_VISIBLESTRING:
            contents = (DERString)new DERVisibleString(text);
            break;
         case CONTENT_TYPE_BMPSTRING:
            contents = (DERString)new DERBMPString(text);
            break;
         default:
            contents = (DERString)new DERUTF8String(text);
            break;
      }
   }

   /**
    * return true if the passed in String can be represented without
    * loss as a UTF8String, false otherwise.
    */
   private boolean canBeUTF8(
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
    * Creates a new <code>DisplayText</code> instance.
    *
    * @param text the text to encapsulate. Strings longer than 200
    * characters are truncated.
    */
   public DisplayText (String text)
   {
      // by default use UTF8String
      if (text.length() > DISPLAY_TEXT_MAXIMUM_SIZE) {
         text = text.substring(0, DISPLAY_TEXT_MAXIMUM_SIZE);
      }

      if (canBeUTF8(text))
      {
          contentType = CONTENT_TYPE_UTF8STRING;
          contents = new DERUTF8String(text);
      }
      else
      {
          contentType = CONTENT_TYPE_BMPSTRING;
          contents = new DERBMPString(text);
      }
   }

   /**
    * Creates a new <code>DisplayText</code> instance.
    * <p>Useful when reading back a <code>DisplayText</code> class
    * from it's ASN1Encodable/DEREncodable form.
    *
    * @param de a <code>DEREncodable</code> instance.
    */
   public DisplayText(DERString de)
   {
      contents = de;
   }

   public static DisplayText getInstance(Object de)
   {
      if (de instanceof DERString)
      {
          return new DisplayText((DERString)de);
      }
      else if (de instanceof DisplayText)
      {
          return (DisplayText)de;
      }

      throw new IllegalArgumentException("illegal object in getInstance");
   }

   public static DisplayText getInstance(
       ASN1TaggedObject obj,
       boolean          explicit)
   {
       return getInstance(obj.getObject()); // must be explicitly tagged
   }

   public DERObject toASN1Object()
   {
      return (DERObject)contents;
   }

   /**
    * Returns the stored <code>String</code> object.
    *
    * @return the stored text as a <code>String</code>.
    */
   public String getString()
   {
      return contents.getString();
   }
}
