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
import com.ibm.ws.transport.iiop.asn1.DERInteger;
import com.ibm.ws.transport.iiop.asn1.DERObject;
import com.ibm.ws.transport.iiop.asn1.DERSequence;

/**
 * <code>NoticeReference</code> class, used in
 * <code>CertificatePolicies</code> X509 V3 extensions
 * (in policy qualifiers).
 *
 * <pre>
 *  NoticeReference ::= SEQUENCE {
 *      organization     DisplayText,
 *      noticeNumbers    SEQUENCE OF INTEGER }
 *
 * </pre>
 *
 * @see PolicyQualifierInfo
 * @see PolicyInformation
 */
public class NoticeReference
    extends ASN1Encodable
{
   DisplayText organization;
   ASN1Sequence noticeNumbers;

   /**
    * Creates a new <code>NoticeReference</code> instance.
    *
    * @param orgName a <code>String</code> value
    * @param numbers a <code>Vector</code> value
    */
   public NoticeReference (String orgName, Vector numbers)
   {
      organization = new DisplayText(orgName);

      Object o = numbers.elementAt(0);

      ASN1EncodableVector av = new ASN1EncodableVector();
      if (o instanceof Integer) {
         Enumeration it = numbers.elements();

         while (it.hasMoreElements()) {
            Integer nm = (Integer) it.nextElement();
               DERInteger di = new DERInteger(nm.intValue());
            av.add (di);
         }
      }

      noticeNumbers = new DERSequence(av);
   }

   /**
    * Creates a new <code>NoticeReference</code> instance.
    *
    * @param orgName a <code>String</code> value
    * @param numbers an <code>ASN1EncodableVector</code> value
    */
   public NoticeReference (String orgName, ASN1Sequence numbers)
   {
      organization = new DisplayText (orgName);
      noticeNumbers = numbers;
   }

   /**
    * Creates a new <code>NoticeReference</code> instance.
    *
    * @param displayTextType an <code>int</code> value
    * @param orgName a <code>String</code> value
    * @param numbers an <code>ASN1EncodableVector</code> value
    */
   public NoticeReference (int displayTextType,
                           String orgName, ASN1Sequence numbers)
   {
      organization = new DisplayText(displayTextType,
                                     orgName);
      noticeNumbers = numbers;
   }

   /**
    * Creates a new <code>NoticeReference</code> instance.
    * <p>Useful for reconstructing a <code>NoticeReference</code>
    * instance from its encodable/encoded form.
    *
    * @param as an <code>ASN1Sequence</code> value obtained from either
    * calling @{link toASN1Object()} for a <code>NoticeReference</code>
    * instance or from parsing it from a DER-encoded stream.
    */
   public NoticeReference (ASN1Sequence as)
   {
      organization = DisplayText.getInstance(as.getObjectAt(0));
      noticeNumbers = (ASN1Sequence) as.getObjectAt(1);
   }

   public static NoticeReference getInstance (Object as)
   {
      if (as instanceof NoticeReference)
      {
          return (NoticeReference)as;
      }
      else if (as instanceof ASN1Sequence)
      {
          return new NoticeReference((ASN1Sequence)as);
      }

      throw new IllegalArgumentException("unknown object in getInstance.");
   }

   /**
    * Describe <code>toASN1Object</code> method here.
    *
    * @return a <code>DERObject</code> value
    */
   public DERObject toASN1Object()
   {
      ASN1EncodableVector av = new ASN1EncodableVector();
      av.add (organization);
      av.add (noticeNumbers);
      return new DERSequence (av);
   }
}
