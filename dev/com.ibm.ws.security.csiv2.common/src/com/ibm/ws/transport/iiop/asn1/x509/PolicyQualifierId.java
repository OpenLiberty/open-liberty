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

import com.ibm.ws.transport.iiop.asn1.DERObjectIdentifier;

/**
 * PolicyQualifierId, used in the CertificatePolicies
 * X509V3 extension.
 *
 * <pre>
 *    id-qt          OBJECT IDENTIFIER ::=  { id-pkix 2 }
 *    id-qt-cps      OBJECT IDENTIFIER ::=  { id-qt 1 }
 *    id-qt-unotice  OBJECT IDENTIFIER ::=  { id-qt 2 }
 *  PolicyQualifierId ::=
 *       OBJECT IDENTIFIER ( id-qt-cps | id-qt-unotice )
 * </pre>
 */
public class PolicyQualifierId extends DERObjectIdentifier
{
   private static final String id_qt = "1.3.6.1.5.5.7.2";

   private PolicyQualifierId(String id)
      {
         super(id);
      }

   public static final PolicyQualifierId id_qt_cps =
       new PolicyQualifierId(id_qt + ".1");
   public static final PolicyQualifierId id_qt_unotice =
       new PolicyQualifierId(id_qt + ".2");
}
