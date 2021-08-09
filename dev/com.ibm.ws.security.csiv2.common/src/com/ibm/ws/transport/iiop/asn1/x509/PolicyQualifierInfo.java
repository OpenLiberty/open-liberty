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
import com.ibm.ws.transport.iiop.asn1.ASN1Sequence;
import com.ibm.ws.transport.iiop.asn1.DEREncodable;
import com.ibm.ws.transport.iiop.asn1.DERIA5String;
import com.ibm.ws.transport.iiop.asn1.DERObject;
import com.ibm.ws.transport.iiop.asn1.DERObjectIdentifier;
import com.ibm.ws.transport.iiop.asn1.DERSequence;

/**
 * Policy qualifiers, used in the X509V3 CertificatePolicies
 * extension.
 *
 * <pre>
 *   PolicyQualifierInfo ::= SEQUENCE {
 *       policyQualifierId  PolicyQualifierId,
 *       qualifier          ANY DEFINED BY policyQualifierId }
 * </pre>
 */
public class PolicyQualifierInfo
    extends ASN1Encodable
{
   DERObjectIdentifier policyQualifierId;
   DEREncodable qualifier;

   /**
    * Creates a new <code>PolicyQualifierInfo</code> instance.
    *
    * @param policyQualifierId a <code>PolicyQualifierId</code> value
    * @param qualifier the qualifier, defined by the above field.
    */
   public PolicyQualifierInfo (DERObjectIdentifier policyQualifierId,
                               DEREncodable qualifier)
   {
      this.policyQualifierId = policyQualifierId;
      this.qualifier = qualifier;
   }

   /**
    * Creates a new <code>PolicyQualifierInfo</code> containing a
    * cPSuri qualifier.
    *
    * @param cps the CPS (certification practice statement) uri as a
    * <code>String</code>.
    */
   public PolicyQualifierInfo (String cps)
   {
      policyQualifierId = PolicyQualifierId.id_qt_cps;
      qualifier = new DERIA5String (cps);
   }

   /**
    * Creates a new <code>PolicyQualifierInfo</code> instance.
    *
    * @param as <code>PolicyQualifierInfo</code> X509 structure
    * encoded as an ASN1Sequence.
    */
   public PolicyQualifierInfo (ASN1Sequence as)
   {
        policyQualifierId = (DERObjectIdentifier) as.getObjectAt(0);
        qualifier = as.getObjectAt(1);
    }

   public static PolicyQualifierInfo getInstance (Object as)
   {
        if (as instanceof PolicyQualifierInfo)
        {
            return (PolicyQualifierInfo)as;
        }
        else if (as instanceof ASN1Sequence)
        {
            return new PolicyQualifierInfo((ASN1Sequence)as);
        }

        throw new IllegalArgumentException("unknown object in getInstance.");
   }

   /**
    * Returns a DER-encodable representation of this instance.
    *
    * @return a <code>DERObject</code> value
    */
   public DERObject toASN1Object()
   {
      ASN1EncodableVector dev = new ASN1EncodableVector();
      dev.add(policyQualifierId);
      dev.add(qualifier);

      return new DERSequence(dev);
   }
}
