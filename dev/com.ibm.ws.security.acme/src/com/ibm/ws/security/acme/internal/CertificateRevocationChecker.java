/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.PKIXRevocationChecker.Option;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.acme.AcmeCaException;

/**
 * Class that contains logic for making certificate revocation checking.
 */
class CertificateRevocationChecker {

	private static final TraceComponent tc = Tr.register(CertificateRevocationChecker.class);

	private final AcmeConfig acmeConfig;

    private static String os_name = System.getProperty("os.name").toLowerCase();
    private static String java_vendor = System.getProperty("java.vendor").toLowerCase();
    private static String java_version = System.getProperty("java.version");
    private static String IBMJCE ="IBMJCE";
    private static String IBM_CERT_PARTH= "IBMCertPath";
    boolean overrideForIBMJDK = false;

	/**
	 * Instantiate a new {@link CertificateRevocationChecker} object.
	 * 
	 * @param acmeConfig
	 *            The ACME configuration.
	 */
	CertificateRevocationChecker(AcmeConfig acmeConfig) {
		this.acmeConfig = acmeConfig;

		/*
		 * Due to the hybrid JDK, on certain OS/JDK combos, we need to specifically set the
		 * IBM provider on some security related getInstances to avoid mixing JDK types and
		 * hitting NPEs.
		 */
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "Check for hybrid JDK: " + os_name + ", " + java_vendor + ", " + java_version);
		}
		if (os_name.startsWith("mac") && java_vendor.contains("ibm")
				&& java_version.startsWith("1.8")) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Detected IBM JDK 1.8 on Mac, will set IBM Providers on getIntance for isRevoked checks");
			}
			overrideForIBMJDK = true;
		}

	}

	/**
	 * Get the CRL distribution points from the specified certificate.
	 * 
	 * @param certificate
	 *            The certificate to return CRL distribution points from.
	 * @return The CRL distribution points, or null if they don't exist.
	 */
	private static List<String> getCrlDistributionPoints(X509Certificate certificate) {
		byte[] octetBytes = certificate.getExtensionValue(Extension.cRLDistributionPoints.getId());
		if (octetBytes == null) {
			return null;
		}

		try {
			/**
			 * From RFC 3280, 4.2.1.14, "CRL Distribution Points":
			 * 
			 * <pre>
			 *    id-ce-cRLDistributionPoints OBJECT IDENTIFIER ::=  { id-ce 31 }
			 * 
			 *    CRLDistributionPoints ::= SEQUENCE SIZE (1..MAX) OF DistributionPoint
			 * 
			 *    DistributionPoint ::= SEQUENCE {
			 *         distributionPoint       [0]     DistributionPointName OPTIONAL,
			 *         reasons                 [1]     ReasonFlags OPTIONAL,
			 *         cRLIssuer               [2]     GeneralNames OPTIONAL }
			 * 
			 *    DistributionPointName ::= CHOICE {
			 *         fullName                [0]     GeneralNames,
			 *         nameRelativeToCRLIssuer [1]     RelativeDistinguishedName }
			 * 
			 *    ReasonFlags ::= BIT STRING {
			 *         unused                  (0),
			 *         keyCompromise           (1),
			 *         cACompromise            (2),
			 *         affiliationChanged      (3),
			 *         superseded              (4),
			 *         cessationOfOperation    (5),
			 *         certificateHold         (6),
			 *         privilegeWithdrawn      (7),
			 *         aACompromise            (8) }
			 * </pre>
			 */

			ASN1InputStream oAsnInStream = new ASN1InputStream(new ByteArrayInputStream(octetBytes));
			ASN1Primitive derObjCrlDP = oAsnInStream.readObject();
			DEROctetString dosCrlDP = (DEROctetString) derObjCrlDP;
			oAsnInStream.close();

			byte[] crldpExtOctets = dosCrlDP.getOctets();
			ASN1InputStream oAsnInStream2 = new ASN1InputStream(new ByteArrayInputStream(crldpExtOctets));
			ASN1Primitive derObj2 = oAsnInStream2.readObject();
			CRLDistPoint distPoint = CRLDistPoint.getInstance(derObj2);
			oAsnInStream2.close();

			List<String> crlUrls = null;
			for (DistributionPoint dp : distPoint.getDistributionPoints()) {
				DistributionPointName dpn = dp.getDistributionPoint();
				// Look for URIs in fullName
				if (dpn != null) {
					if (dpn.getType() == DistributionPointName.FULL_NAME) {
						GeneralName[] genNames = GeneralNames.getInstance(dpn.getName()).getNames();
						// Look for an URI
						for (int j = 0; j < genNames.length; j++) {
							if (genNames[j].getTagNo() == GeneralName.uniformResourceIdentifier) {
								String url = DERIA5String.getInstance(genNames[j].getName()).getString();
								if (crlUrls == null) {
									crlUrls = new ArrayList<String>();
								}
								crlUrls.add(url);
							}
						}
					}
				}
			}
			return crlUrls;
		} catch (IOException e) {
			Tr.error(tc, "CWPKI2061E", certificate.getSerialNumber().toString(16), e.getMessage());
			return null;
		}
	}

	/**
	 * Get the OCSP URL from the specified certificate.
	 * 
	 * @param certificate
	 *            The certificate to get the OCSP URL from.
	 * @return The OCSP URL from the specified certificate, or null if it does
	 *         not exist.
	 */
	private static String getOcspUrl(X509Certificate certificate) {
		byte[] octetBytes = certificate.getExtensionValue(Extension.authorityInfoAccess.getId());
		if (octetBytes == null) {
			return null;
		}

		DLSequence dlSequence = null;
		ASN1Encodable asn1Encodable = null;

		try {
			/**
			 * From RFC 3280, section 4.2.2.1, "Authority Information Access":
			 * 
			 * <pre>
			 *    id-pe-authorityInfoAccess OBJECT IDENTIFIER ::= { id-pe 1 }
			 * 
			 *    AuthorityInfoAccessSyntax  ::=
			 *            SEQUENCE SIZE (1..MAX) OF AccessDescription
			 * 
			 *    AccessDescription  ::=  SEQUENCE {
			 *            accessMethod          OBJECT IDENTIFIER,
			 *            accessLocation        GeneralName  }
			 * 
			 *    id-ad OBJECT IDENTIFIER ::= { id-pkix 48 }
			 * 
			 *    id-ad-caIssuers OBJECT IDENTIFIER ::= { id-ad 2 }
			 * 
			 *    id-ad-ocsp OBJECT IDENTIFIER ::= { id-ad 1 }
			 * </pre>
			 */
			ASN1Primitive fromExtensionValue = JcaX509ExtensionUtils.parseExtensionValue(octetBytes);
			if (!(fromExtensionValue instanceof DLSequence)) {
				return null;
			}

			dlSequence = (DLSequence) fromExtensionValue;
			for (int i = 0; i < dlSequence.size(); i++) {
				asn1Encodable = dlSequence.getObjectAt(i);
				if (asn1Encodable instanceof DLSequence) {
					break;
				}
			}
			if (!(asn1Encodable instanceof DLSequence)) {
				return null;
			}

			dlSequence = (DLSequence) asn1Encodable;
			for (int i = 0; i < dlSequence.size(); i++) {
				asn1Encodable = dlSequence.getObjectAt(i);
				if (asn1Encodable instanceof DERTaggedObject) {
					break;
				}
			}
			if (!(asn1Encodable instanceof DERTaggedObject)) {
				return null;
			}

			DERTaggedObject derTaggedObject = (DERTaggedObject) asn1Encodable;
			byte[] encoded = derTaggedObject.getEncoded();
			if (derTaggedObject.getTagNo() == 6) {
				int len = encoded[1];
				return new String(encoded, 2, len);
			}
			return null;

		} catch (IOException e) {
			Tr.error(tc, "CWPKI2060E", certificate.getSerialNumber().toString(16), e.getMessage());
			return null;
		}
	}

	/**
	 * Get the signer certificate from the certificate chain.
	 * 
	 * @param certificateChain
	 *            The certificate chain.
	 * @return The signer certificate.
	 */
	public static X509Certificate getSignerCertificate(List<X509Certificate> certificateChain) {
		if (certificateChain != null) {
			int size = certificateChain.size();
			if (size > 1) {
				return certificateChain.get(size - 1);
			}
		}
		return null;
	}

	/**
	 * Check whether the end-entity / leaf certificate in the specified
	 * certificate chain has been revoked.
	 * 
	 * </p>
	 * Self-signed certificates and certificates with no OCSP URL or CRL
	 * distribution points are considered to not be revoked.
	 * 
	 * @param certificateChain
	 *            The certificate chain to check.
	 * @return Whether the certificate has been revoked.
	 * @throws AcmeCaException
	 *             If there was an issue checking the revocation status.
	 */
	@FFDCIgnore(CertPathValidatorException.class)
	boolean isRevoked(List<X509Certificate> certificateChain) throws AcmeCaException {

		/*
		 * Don't check if revocation checker is disabled.
		 */
		if (!acmeConfig.isRevocationCheckerEnabled()) {
			return false;
		}

		/*
		 * Get the leaf and the signer certificate.
		 */
		X509Certificate leafCertificate = AcmeProviderImpl.getLeafCertificate(certificateChain);
		X509Certificate signerCertificate = getSignerCertificate(certificateChain);

		/*
		 * If a signer / intermediate certificate is not available in the
		 * certificate chain, assume that we are using a self-signed
		 * certificate.
		 */
		if (signerCertificate == null) {
			return false;
		}

		/*
		 * If there isn't an OCSP or CRL URL in the certificate, there is no way
		 * to check revocation.
		 */
		if (getOcspUrl(leafCertificate) == null && getCrlDistributionPoints(leafCertificate) == null) {
			return false;
		}

		/*
		 * Configure the CertPathValidator.
		 */
		CertPath certPath = null;
		CertPathValidator cpv = null;
		PKIXParameters params = null;
		PKIXRevocationChecker rc = null;
		try {

			/*
			 * "Trust" the signer certificate to check the signature. Validating
			 * with the signer should result in a successful PKIX validation.
			 */
			KeyStore cacerts = checkForKeyStoreProviderOverride();
			if (cacerts == null) {
				cacerts = KeyStore.getInstance(KeyStore.getDefaultType());
			}
			cacerts.load(null);
			cacerts.setCertificateEntry("signer", signerCertificate);

			/*
			 * Get the revocation checker instance.
			 */
			CertPathBuilder cpb = checkForCertPathBuilderProviderOverride();
			if (cpb == null) {
				cpb = CertPathBuilder.getInstance("PKIX");
			}
			rc = (PKIXRevocationChecker) cpb.getRevocationChecker();

			/*
			 * Setup the options that have been configured.
			 */
			Set<Option> options = new HashSet<Option>();
			options.add(Option.SOFT_FAIL); // Ignore network failures.
			if (acmeConfig.isDisableFallback()) {
				options.add(Option.NO_FALLBACK);
			}
			if (acmeConfig.isPreferCrls()) {
				options.add(Option.PREFER_CRLS);
			}
			rc.setOptions(options);

			/*
			 * Override the OCSP responder URL in the certificate if configured
			 * to do so.
			 */
			URI oscpResponderUrl = acmeConfig.getOcspResponderUrl();
			if (oscpResponderUrl != null) {
				rc.setOcspResponder(oscpResponderUrl);
			}

			/*
			 * Configure a CertPath.
			 */
			List<X509Certificate> certs = new ArrayList<X509Certificate>();
			certs.add(leafCertificate);
			certPath = checkForCertPathProviderOverride(certs);
			if (certPath == null) {
				certPath = CertificateFactory.getInstance("X.509").generateCertPath(certs);
			}

			/*
			 * Configure the PKIX parameters.
			 */
			params = new PKIXParameters(cacerts);
			params.addCertPathChecker(rc);
			// params.setRevocationEnabled(false);

			/*
			 * Finally initialize the CertPathValidator.
			 */
			cpv = checkForCertPathValidatorProviderOverride();
			if (cpv == null) {
				cpv = CertPathValidator.getInstance("PKIX");
			}

		} catch (CertificateException | NoSuchAlgorithmException | InvalidAlgorithmParameterException
				| KeyStoreException | IOException e) {
			throw new AcmeCaException(Tr.formatMessage(tc, "CWPKI2057E", e.getMessage()), e);
		}

		try {
			cpv.validate(certPath, params); // PKIXCertPathValidatorResult

			/*
			 * Check if there were any soft failures. If there were, we should
			 * issue a warning message. The 'getSoftFailExceptions' does not
			 * work in Java 8. It was fixed in 10/11, but apparently not in 8:
			 * https://bugs.openjdk.java.net/browse/JDK-8161973
			 */
			if (!rc.getSoftFailExceptions().isEmpty()) {
				Tr.warning(tc, Tr.formatMessage(tc, "CWPKI2058W", rc.getSoftFailExceptions()));
			}

			return false; // Certificate has NOT been revoked!
		} catch (CertPathValidatorException e) {
			Tr.info(tc, Tr.formatMessage(tc, "CWPKI2059I", leafCertificate.getSerialNumber().toString(16)));
			return true; // Certificate has been revoked!
		} catch (InvalidAlgorithmParameterException e) {
			/*
			 * This can not happen since we control the parameters and cert path
			 * type. No need to localize.
			 */
			throw new AcmeCaException("Invalid algorithm parameter passed into CertPathValidator.validate(...) method.",
					e);
		}
	}

	/**
	 * If we need to set the IBM JDK on the getInstance, set it and  return the KeyStore, otherwise
	 * return null and the call can run a default getInstance. This method will suppress FFDC in case
	 * of failure and we can retry with the default getInstance.
	 *  
	 * @return
	 */
	@Trivial
	@FFDCIgnore({ KeyStoreException.class })
	private KeyStore checkForKeyStoreProviderOverride() {
		if (overrideForIBMJDK) {
			try {
				return KeyStore.getInstance(KeyStore.getDefaultType(), IBMJCE);
			} catch (NoSuchProviderException | KeyStoreException e) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Attempted to set the " + IBMJCE
							+ " as the specific provoider for KeyStore.getInstance, but it failed. Will try default provider.",
							e);
				}
			}
		}
		return null;
	}

	/**
	 * If we need to set the IBM JDK on the getInstance, set it and  return the CertPathBuilder, otherwise
	 * return null and the call can run a default getInstance. This method will suppress FFDC in case
	 * of failure and we can retry with the default getInstance.
	 *  
	 * @return
	 */
	@Trivial
	@FFDCIgnore({ NoSuchAlgorithmException.class })
	private CertPathBuilder checkForCertPathBuilderProviderOverride() {
		if (overrideForIBMJDK) {
			try {
				return CertPathBuilder.getInstance("PKIX", IBM_CERT_PARTH);
			} catch (NoSuchProviderException | NoSuchAlgorithmException e) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Attempted to set the " + IBM_CERT_PARTH
							+ " as the specific provoider for CertPathBuilder.getInstance, but it failed. Will try default provider.",
							e);
				}
			}
		}
		return null;
	}

	/**
	 * If we need to set the IBM JDK on the getInstance, set it and  return the CertPath, otherwise
	 * return null and the call can run a default getInstance. This method will suppress FFDC in case
	 * of failure and we can retry with the default getInstance.
	 *  
	 * @return
	 */
	@Trivial
	@FFDCIgnore({ CertificateException.class })
	private CertPath checkForCertPathProviderOverride(List<X509Certificate> certs) {
		if (overrideForIBMJDK) {
			try {
				return CertificateFactory.getInstance("X.509", IBMJCE).generateCertPath(certs);
			} catch (NoSuchProviderException | CertificateException e) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Attempted to set the " + IBMJCE
							+ " as the specific provoider for CertificateFactory.getInstance, but it failed. Will try default provider.",
							e);
				}
			}
		}
		return null;
	}

	/**
	 * If we need to set the IBM JDK on the getInstance, set it and  return the CertPathValidator, otherwise
	 * return null and the call can run a default getInstance. This method will suppress FFDC in case
	 * of failure and we can retry with the default getInstance.
	 *  
	 * @return
	 */
	@Trivial
	@FFDCIgnore({ NoSuchAlgorithmException.class })
	private CertPathValidator checkForCertPathValidatorProviderOverride() {
		if (overrideForIBMJDK) {
			try {
				return CertPathValidator.getInstance("PKIX", IBM_CERT_PARTH);
			} catch (NoSuchProviderException | NoSuchAlgorithmException e) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Attempted to set the " + IBMJCE
							+ " as the specific provoider for CertPathValidator.getInstance, but it failed. Will try default provider.",
							e);
				}
			}
		}
		return null;
	}
}
