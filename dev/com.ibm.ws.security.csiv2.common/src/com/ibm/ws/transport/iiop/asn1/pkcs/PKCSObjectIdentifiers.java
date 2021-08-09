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

package com.ibm.ws.transport.iiop.asn1.pkcs;

import com.ibm.ws.transport.iiop.asn1.DERObjectIdentifier;

public interface PKCSObjectIdentifiers
{
    //
    // pkcs-1 OBJECT IDENTIFIER ::= {
    //       iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) 1 }
    //
    static final String                 pkcs_1                    = "1.2.840.113549.1.1";
    static final DERObjectIdentifier    rsaEncryption             = new DERObjectIdentifier(pkcs_1 + ".1");
    static final DERObjectIdentifier    md2WithRSAEncryption      = new DERObjectIdentifier(pkcs_1 + ".2");
    static final DERObjectIdentifier    md4WithRSAEncryption      = new DERObjectIdentifier(pkcs_1 + ".3");
    static final DERObjectIdentifier    md5WithRSAEncryption      = new DERObjectIdentifier(pkcs_1 + ".4");
    static final DERObjectIdentifier    sha1WithRSAEncryption     = new DERObjectIdentifier(pkcs_1 + ".5");
    static final DERObjectIdentifier    srsaOAEPEncryptionSET     = new DERObjectIdentifier(pkcs_1 + ".6");
    static final DERObjectIdentifier    id_RSAES_OAEP             = new DERObjectIdentifier(pkcs_1 + ".7");
    static final DERObjectIdentifier    id_mgf1                   = new DERObjectIdentifier(pkcs_1 + ".8");
    static final DERObjectIdentifier    id_pSpecified             = new DERObjectIdentifier(pkcs_1 + ".9");
    static final DERObjectIdentifier    id_RSASSA_PSS             = new DERObjectIdentifier(pkcs_1 + ".10");
    static final DERObjectIdentifier    sha256WithRSAEncryption   = new DERObjectIdentifier(pkcs_1 + ".11");
    static final DERObjectIdentifier    sha384WithRSAEncryption   = new DERObjectIdentifier(pkcs_1 + ".12");
    static final DERObjectIdentifier    sha512WithRSAEncryption   = new DERObjectIdentifier(pkcs_1 + ".13");
    static final DERObjectIdentifier    sha224WithRSAEncryption   = new DERObjectIdentifier(pkcs_1 + ".14");

    //
    // pkcs-3 OBJECT IDENTIFIER ::= {
    //       iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) 3 }
    //
    static final String                 pkcs_3                  = "1.2.840.113549.1.3";
    static final DERObjectIdentifier    dhKeyAgreement          = new DERObjectIdentifier(pkcs_3 + ".1");

    //
    // pkcs-5 OBJECT IDENTIFIER ::= {
    //       iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) 5 }
    //
    static final String                 pkcs_5                  = "1.2.840.113549.1.5";

    static final DERObjectIdentifier    pbeWithMD2AndDES_CBC    = new DERObjectIdentifier(pkcs_5 + ".1");
    static final DERObjectIdentifier    pbeWithMD2AndRC2_CBC    = new DERObjectIdentifier(pkcs_5 + ".4");
    static final DERObjectIdentifier    pbeWithMD5AndDES_CBC    = new DERObjectIdentifier(pkcs_5 + ".3");
    static final DERObjectIdentifier    pbeWithMD5AndRC2_CBC    = new DERObjectIdentifier(pkcs_5 + ".6");
    static final DERObjectIdentifier    pbeWithSHA1AndDES_CBC   = new DERObjectIdentifier(pkcs_5 + ".10");
    static final DERObjectIdentifier    pbeWithSHA1AndRC2_CBC   = new DERObjectIdentifier(pkcs_5 + ".11");

    static final DERObjectIdentifier    id_PBES2                = new DERObjectIdentifier(pkcs_5 + ".13");

    static final DERObjectIdentifier    id_PBKDF2               = new DERObjectIdentifier(pkcs_5 + ".12");

    //
    // encryptionAlgorithm OBJECT IDENTIFIER ::= {
    //       iso(1) member-body(2) us(840) rsadsi(113549) 3 }
    //
    static final String                 encryptionAlgorithm     = "1.2.840.113549.3";

    static final DERObjectIdentifier    des_EDE3_CBC            = new DERObjectIdentifier(encryptionAlgorithm + ".7");
    static final DERObjectIdentifier    RC2_CBC                 = new DERObjectIdentifier(encryptionAlgorithm + ".2");

    //
    // object identifiers for digests
    //
    static final String                 digestAlgorithm     = "1.2.840.113549.2";
    //
    // md2 OBJECT IDENTIFIER ::=
    //      {iso(1) member-body(2) US(840) rsadsi(113549) digestAlgorithm(2) 2}
    //
    static final DERObjectIdentifier    md2                     = new DERObjectIdentifier(digestAlgorithm + ".2");

    //
    // md4 OBJECT IDENTIFIER ::=
    //      {iso(1) member-body(2) US(840) rsadsi(113549) digestAlgorithm(2) 4}
    //
    static final DERObjectIdentifier    md4 = new DERObjectIdentifier(digestAlgorithm + ".4");

    //
    // md5 OBJECT IDENTIFIER ::=
    //      {iso(1) member-body(2) US(840) rsadsi(113549) digestAlgorithm(2) 5}
    //
    static final DERObjectIdentifier    md5                     = new DERObjectIdentifier(digestAlgorithm + ".5");

    static final DERObjectIdentifier    id_hmacWithSHA1         = new DERObjectIdentifier(digestAlgorithm + ".7");
    static final DERObjectIdentifier    id_hmacWithSHA224       = new DERObjectIdentifier(digestAlgorithm + ".8");
    static final DERObjectIdentifier    id_hmacWithSHA256       = new DERObjectIdentifier(digestAlgorithm + ".9");
    static final DERObjectIdentifier    id_hmacWithSHA384       = new DERObjectIdentifier(digestAlgorithm + ".10");
    static final DERObjectIdentifier    id_hmacWithSHA512       = new DERObjectIdentifier(digestAlgorithm + ".11");

    //
    // pkcs-7 OBJECT IDENTIFIER ::= {
    //       iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) 7 }
    //
    static final String                 pkcs_7                  = "1.2.840.113549.1.7";
    static final DERObjectIdentifier    data                    = new DERObjectIdentifier(pkcs_7 + ".1");
    static final DERObjectIdentifier    signedData              = new DERObjectIdentifier(pkcs_7 + ".2");
    static final DERObjectIdentifier    envelopedData           = new DERObjectIdentifier(pkcs_7 + ".3");
    static final DERObjectIdentifier    signedAndEnvelopedData  = new DERObjectIdentifier(pkcs_7 + ".4");
    static final DERObjectIdentifier    digestedData            = new DERObjectIdentifier(pkcs_7 + ".5");
    static final DERObjectIdentifier    encryptedData           = new DERObjectIdentifier(pkcs_7 + ".6");

    //
    // pkcs-9 OBJECT IDENTIFIER ::= {
    //       iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) 9 }
    //
    static final String                 pkcs_9                  = "1.2.840.113549.1.9";

    static final DERObjectIdentifier    pkcs_9_at_emailAddress  = new DERObjectIdentifier(pkcs_9 + ".1");
    static final DERObjectIdentifier    pkcs_9_at_unstructuredName = new DERObjectIdentifier(pkcs_9 + ".2");
    static final DERObjectIdentifier    pkcs_9_at_contentType = new DERObjectIdentifier(pkcs_9 + ".3");
    static final DERObjectIdentifier    pkcs_9_at_messageDigest = new DERObjectIdentifier(pkcs_9 + ".4");
    static final DERObjectIdentifier    pkcs_9_at_signingTime = new DERObjectIdentifier(pkcs_9 + ".5");
    static final DERObjectIdentifier    pkcs_9_at_counterSignature = new DERObjectIdentifier(pkcs_9 + ".6");
    static final DERObjectIdentifier    pkcs_9_at_challengePassword = new DERObjectIdentifier(pkcs_9 + ".7");
    static final DERObjectIdentifier    pkcs_9_at_unstructuredAddress = new DERObjectIdentifier(pkcs_9 + ".8");
    static final DERObjectIdentifier    pkcs_9_at_extendedCertificateAttributes = new DERObjectIdentifier(pkcs_9 + ".9");

    static final DERObjectIdentifier    pkcs_9_at_signingDescription = new DERObjectIdentifier(pkcs_9 + ".13");
    static final DERObjectIdentifier    pkcs_9_at_extensionRequest = new DERObjectIdentifier(pkcs_9 + ".14");
    static final DERObjectIdentifier    pkcs_9_at_smimeCapabilities = new DERObjectIdentifier(pkcs_9 + ".15");

    static final DERObjectIdentifier    pkcs_9_at_friendlyName  = new DERObjectIdentifier(pkcs_9 + ".20");
    static final DERObjectIdentifier    pkcs_9_at_localKeyId    = new DERObjectIdentifier(pkcs_9 + ".21");

    static final DERObjectIdentifier    x509certType            = new DERObjectIdentifier(pkcs_9 + ".22.1");

    static final DERObjectIdentifier    id_alg_PWRI_KEK    = new DERObjectIdentifier(pkcs_9 + ".16.3.9");

    //
    // SMIME capability sub oids.
    //
    static final DERObjectIdentifier    preferSignedData        = new DERObjectIdentifier(pkcs_9 + ".15.1");
    static final DERObjectIdentifier    canNotDecryptAny        = new DERObjectIdentifier(pkcs_9 + ".15.2");
    static final DERObjectIdentifier    sMIMECapabilitiesVersions = new DERObjectIdentifier(pkcs_9 + ".15.3");

    //
    // other SMIME attributes
    //

    //
    // id-ct OBJECT IDENTIFIER ::= {iso(1) member-body(2) usa(840)
    // rsadsi(113549) pkcs(1) pkcs-9(9) smime(16) ct(1)}
    //
    static String id_ct = "1.2.840.113549.1.9.16.1";

    static final DERObjectIdentifier    id_ct_TSTInfo           = new DERObjectIdentifier(id_ct + ".4");
    static final DERObjectIdentifier    id_ct_compressedData    = new DERObjectIdentifier(id_ct + ".9");

    //
    // id-cti OBJECT IDENTIFIER ::= {iso(1) member-body(2) usa(840)
    // rsadsi(113549) pkcs(1) pkcs-9(9) smime(16) cti(6)}
    //
    static String id_cti = "1.2.840.113549.1.9.16.6";

    static final DERObjectIdentifier    id_cti_ets_proofOfOrigin  = new DERObjectIdentifier(id_cti + ".1");
    static final DERObjectIdentifier    id_cti_ets_proofOfReceipt = new DERObjectIdentifier(id_cti + ".2");
    static final DERObjectIdentifier    id_cti_ets_proofOfDelivery = new DERObjectIdentifier(id_cti + ".3");
    static final DERObjectIdentifier    id_cti_ets_proofOfSender = new DERObjectIdentifier(id_cti + ".4");
    static final DERObjectIdentifier    id_cti_ets_proofOfApproval = new DERObjectIdentifier(id_cti + ".5");
    static final DERObjectIdentifier    id_cti_ets_proofOfCreation = new DERObjectIdentifier(id_cti + ".6");

    //
    // id-aa OBJECT IDENTIFIER ::= {iso(1) member-body(2) usa(840)
    // rsadsi(113549) pkcs(1) pkcs-9(9) smime(16) attributes(2)}
    //
    static String id_aa = "1.2.840.113549.1.9.16.2";

    /*
     * id-aa-encrypKeyPref OBJECT IDENTIFIER ::= {id-aa 11}
     *
     */
    static DERObjectIdentifier id_aa_encrypKeyPref = new DERObjectIdentifier(id_aa + ".11");
    static DERObjectIdentifier id_aa_signingCertificate = new DERObjectIdentifier(id_aa + ".12");

    static final DERObjectIdentifier id_aa_contentIdentifier = new DERObjectIdentifier(id_aa + ".7"); // See RFC 2634
    static final DERObjectIdentifier id_aa_signatureTimeStampToken = new DERObjectIdentifier(id_aa + ".14"); // See RFC 3126
    static final DERObjectIdentifier id_aa_sigPolicyId = new DERObjectIdentifier(id_aa + ".15"); // See RFC 3126
    static final DERObjectIdentifier id_aa_commitmentType = new DERObjectIdentifier(id_aa + ".16"); // See RFC 3126
    static final DERObjectIdentifier id_aa_signerLocation = new DERObjectIdentifier(id_aa + ".17"); // See RFC 3126
    static final DERObjectIdentifier id_aa_otherSigCert = new DERObjectIdentifier(id_aa + ".19"); // See RFC 3126
    //
    // pkcs-12 OBJECT IDENTIFIER ::= {
    //       iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) 12 }
    //
    static final String                 pkcs_12                  = "1.2.840.113549.1.12";
    static final String                 bagtypes                 = pkcs_12 + ".10.1";

    static final DERObjectIdentifier    keyBag                  = new DERObjectIdentifier(bagtypes + ".1");
    static final DERObjectIdentifier    pkcs8ShroudedKeyBag     = new DERObjectIdentifier(bagtypes + ".2");
    static final DERObjectIdentifier    certBag                 = new DERObjectIdentifier(bagtypes + ".3");
    static final DERObjectIdentifier    crlBag                  = new DERObjectIdentifier(bagtypes + ".4");
    static final DERObjectIdentifier    secretBag               = new DERObjectIdentifier(bagtypes + ".5");
    static final DERObjectIdentifier    safeContentsBag         = new DERObjectIdentifier(bagtypes + ".6");

    static final String pkcs_12PbeIds  = pkcs_12 + ".1";

    static final DERObjectIdentifier    pbeWithSHAAnd128BitRC4 = new DERObjectIdentifier(pkcs_12PbeIds + ".1");
    static final DERObjectIdentifier    pbeWithSHAAnd40BitRC4  = new DERObjectIdentifier(pkcs_12PbeIds + ".2");
    static final DERObjectIdentifier    pbeWithSHAAnd3_KeyTripleDES_CBC = new DERObjectIdentifier(pkcs_12PbeIds + ".3");
    static final DERObjectIdentifier    pbeWithSHAAnd2_KeyTripleDES_CBC = new DERObjectIdentifier(pkcs_12PbeIds + ".4");
    static final DERObjectIdentifier    pbeWithSHAAnd128BitRC2_CBC = new DERObjectIdentifier(pkcs_12PbeIds + ".5");
    static final DERObjectIdentifier    pbewithSHAAnd40BitRC2_CBC = new DERObjectIdentifier(pkcs_12PbeIds + ".6");

}

