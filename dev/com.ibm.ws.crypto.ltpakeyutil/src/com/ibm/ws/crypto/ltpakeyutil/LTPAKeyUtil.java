/*******************************************************************************
 * Copyright (c) 2016, 2023 IBM Corporation and others.
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
package com.ibm.ws.crypto.ltpakeyutil;

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.service.util.JavaInfo;

public final class LTPAKeyUtil {
	private static final TraceComponent tc = Tr.register(LTPAKeyUtil.class);

	public static boolean ibmJCEAvailable = false;
	public static boolean ibmJCEPlusFIPSAvailable = false;
	public static boolean openJCEPlusAvailable = false;
	public static boolean ibmJCEProviderChecked = false;
	public static boolean ibmJCEPlusFIPSProviderChecked = false;
	public static boolean openJCEPlusProviderChecked = false;

	public static boolean javaVersionChecked = false;
	public static boolean isJava11orHigher = false;

	public static boolean zOSAndJAVA11orHigherChecked = false;
	public static boolean iszOSAndJava11orHigher = false;

	public static String osName = System.getProperty("os.name");
	public static boolean isZOS = false;
	public static boolean osVersionChecked = false;

	public static String IBMJCE_PROVIDER = "com.ibm.crypto.provider.IBMJCE";
	public static String IBMJCE_PLUS_FIPS_PROVIDER = "com.ibm.crypto.provider.IBMJCEPlusFIPS";
	public static String OPENJCE_PLUS_PROVIDER = "com.ibm.crypto.plus.provider.OpenJCEPlus";

	public static final String MESSAGE_DIGEST_ALGORITHM_SHA = "SHA";
	public static final String MESSAGE_DIGEST_ALGORITHM_SHA256 = "SHA256";

	public static final String IBMJCE_NAME = "IBMJCE";
	public static final String IBMJCE_PLUS_FIPS_NAME = "IBMJCEPlusFIPS";

	private static boolean issuedBetaMessage = false;

	public static byte[] encrypt(byte[] data, byte[] key, String cipher) throws Exception {
		return LTPACrypto.encrypt(data, key, cipher);
	}

	public static byte[] decrypt(byte[] msg, byte[] key, String cipher) throws Exception {
		return LTPACrypto.decrypt(msg, key, cipher);
	}

	public static boolean verifyISO9796(byte[][] key, byte[] data, int off, int len, byte[] sig, int sigOff, int sigLen)
			throws Exception {
		return LTPACrypto.verifyISO9796(key, data, off, len, sig, sigOff, sigLen);
	}

	public static byte[] signISO9796(byte[][] key, byte[] data, int off, int len) throws Exception {
		return LTPACrypto.signISO9796(key, data, off, len);
	}

	public static void setRSAKey(byte[][] key) {
		LTPACrypto.setRSAKey(key);
	}

	public static byte[][] getRawKey(LTPAPrivateKey privKey) {
		return privKey.getRawKey();
	}

	public static byte[][] getRawKey(LTPAPublicKey pubKey) {
		return pubKey.getRawKey();
	}

	public static LTPAKeyPair generateLTPAKeyPair() {
		return LTPADigSignature.generateLTPAKeyPair();
	}

	public static byte[] generate3DESKey() {
		return LTPACrypto.generate3DESKey();
	}

	public static boolean isIBMJCEAvailable() {
		if (ibmJCEProviderChecked) {
			return ibmJCEAvailable;
		} else {
			ibmJCEAvailable = JavaInfo.isSystemClassAvailable(IBMJCE_PROVIDER);
			ibmJCEProviderChecked = true;
			return ibmJCEAvailable;
		}
	}

	public static boolean isIBMJCEPlusFIPSAvailable() {
		if (ibmJCEPlusFIPSProviderChecked) {
			return ibmJCEPlusFIPSAvailable;
		} else {
			String ibmjceplusfipsprovider = AccessController.doPrivileged(new PrivilegedAction<String>() {
				@Override
				public String run() {
					return System.getProperty("com.ibm.jsse2.usefipsProviderName");
				}
			});
			ibmJCEPlusFIPSProviderChecked = true;
			if (ibmjceplusfipsprovider == "IBMJCEPlusFIPS" && isRunningBetaMode()) {
				ibmJCEPlusFIPSAvailable = true;
				return ibmJCEPlusFIPSAvailable;
			} else {
				if (isFIPSEnabled()) {
					// UTLE TODO: error msg - FIPS is enabled but the IBMJCEPlusFIPS is not
					// available
				}
				return false;
			}
		}

	}

	static boolean isRunningBetaMode() {
		if (!ProductInfo.getBetaEdition()) {
			return false;
		} else {
			// Running beta exception, issue message if we haven't already issued one for
			// this class
			if (!issuedBetaMessage) {
				Tr.info(tc, "BETA: A beta method has been invoked for the class LTPAKeyUtil for the first time.");
				issuedBetaMessage = !issuedBetaMessage;
			}
			return true;
		}
	}

	public static boolean isFIPSEnabled() {
		String fipsON = AccessController.doPrivileged(new PrivilegedAction<String>() {
			@Override
			public String run() {
				return System.getProperty("com.ibm.jsse2.usefipsprovider");
			}
		});
		if (fipsON == "true") {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isOpenJCEPlusAvailable() {
		if (openJCEPlusProviderChecked) {
			return openJCEPlusAvailable;
		} else {
			openJCEPlusAvailable = JavaInfo.isSystemClassAvailable(OPENJCE_PLUS_PROVIDER);
			openJCEPlusProviderChecked = true;
			return openJCEPlusAvailable;
		}

	}

	private static boolean isJava11orHigher() {
		if (javaVersionChecked) {
			return isJava11orHigher;
		} else {
			isJava11orHigher = JavaInfo.majorVersion() >= 11;
			javaVersionChecked = true;
			return isJava11orHigher;
		}
	}

	private static boolean isZOS() {
		if (osVersionChecked) {
			return isZOS;
		} else {
			isZOS = (osName.equalsIgnoreCase("z/OS") || osName.equalsIgnoreCase("OS/390"));
			osVersionChecked = true;
			return isZOS;
		}
	}

	public static boolean isZOSandRunningJava11orHigher() {
		if (zOSAndJAVA11orHigherChecked) {
			return iszOSAndJava11orHigher;
		} else {
			iszOSAndJava11orHigher = isJava11orHigher() && isZOS();
			zOSAndJAVA11orHigherChecked = true;
			return iszOSAndJava11orHigher;
		}
	}

}
