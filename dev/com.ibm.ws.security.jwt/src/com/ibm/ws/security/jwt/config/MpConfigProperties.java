/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

public class MpConfigProperties extends HashMap<String, String> {

	private static final TraceComponent tc = Tr.register(MpConfigProperties.class);

	private static final long serialVersionUID = 3205984119272840498L;

	public final static String ISSUER = "mp.jwt.verify.issuer";
	public final static String PUBLIC_KEY = "mp.jwt.verify.publickey";
	public final static String KEY_LOCATION = "mp.jwt.verify.publickey.location";

	// Properties added by MP JWT 1.2 specification
	public final static String PUBLIC_KEY_ALG = "mp.jwt.verify.publickey.algorithm";
	public final static String DECRYPT_KEY_LOCATION = "mp.jwt.decrypt.key.location";
	public final static String VERIFY_AUDIENCES = "mp.jwt.verify.audiences";
	public final static String TOKEN_HEADER = "mp.jwt.token.header";
	public final static String TOKEN_COOKIE = "mp.jwt.token.cookie";

	// Properties added by 2.1 specification
	public final static String TOKEN_AGE = "mp.jwt.verify.token.age";
	public final static String CLOCK_SKEW = "mp.jwt.verify.clock.skew";
	public final static String DECRYPT_KEY_ALGORITHM = "mp.jwt.decrypt.key.algorithm";

    public static final Set<String> acceptableMpConfigPropNames11;
    public static final Set<String> acceptableMpConfigPropNames12;
    public static final Set<String> acceptableMpConfigPropNames21;

    static {
        Set<String> mpConfigPropNames = new HashSet<>();
        mpConfigPropNames.add(ISSUER);
        mpConfigPropNames.add(PUBLIC_KEY);
        mpConfigPropNames.add(KEY_LOCATION);
        acceptableMpConfigPropNames11 = Collections.unmodifiableSet(mpConfigPropNames);

        mpConfigPropNames = new HashSet<>(acceptableMpConfigPropNames11);
        mpConfigPropNames.add(PUBLIC_KEY_ALG);
        mpConfigPropNames.add(DECRYPT_KEY_LOCATION);
        mpConfigPropNames.add(VERIFY_AUDIENCES);
        mpConfigPropNames.add(TOKEN_HEADER);
        mpConfigPropNames.add(TOKEN_COOKIE);
        acceptableMpConfigPropNames12 = Collections.unmodifiableSet(mpConfigPropNames);

        mpConfigPropNames = new HashSet<>(acceptableMpConfigPropNames12);
        mpConfigPropNames.add(TOKEN_AGE);
        mpConfigPropNames.add(CLOCK_SKEW);
        mpConfigPropNames.add(DECRYPT_KEY_ALGORITHM);
        acceptableMpConfigPropNames21 = Collections.unmodifiableSet(mpConfigPropNames);
    }

    public MpConfigProperties() {
		super();
	}

	public MpConfigProperties(MpConfigProperties mpConfigProps) {
		super(mpConfigProps);
	}

	@Trivial
	public static Set<String> getSensitivePropertyNames() {
		Set<String> sensitiveProps = new HashSet<String>();
		sensitiveProps.add(DECRYPT_KEY_LOCATION);
		return sensitiveProps;
	}

	@Trivial
	public static boolean isSensitivePropertyName(String propertyName) {
		Set<String> sensitiveProps = getSensitivePropertyNames();
		return sensitiveProps.contains(propertyName);
	}

	public String getConfiguredSignatureAlgorithm(JwtConsumerConfig config) {
		String signatureAlgorithm = config.getSignatureAlgorithm();
		if (signatureAlgorithm != null) {
			// Server configuration takes precedence over MP Config property
			// values
			return signatureAlgorithm;
		}
		return getSignatureAlgorithmFromMpConfigProps();
	}

	String getSignatureAlgorithmFromMpConfigProps() {
		String defaultAlg = "RS256";
		String publicKeyAlgMpConfigProp = get(PUBLIC_KEY_ALG);
		if (publicKeyAlgMpConfigProp == null) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc,
						"Didn't find " + PUBLIC_KEY_ALG + " property in MP Config props; defaulting to " + defaultAlg);
			}
			return defaultAlg;
		}
		if (!isSupportedSignatureAlgorithm(publicKeyAlgMpConfigProp)) {
			Tr.warning(tc, "MP_CONFIG_PUBLIC_KEY_ALG_NOT_SUPPORTED",
					new Object[] { publicKeyAlgMpConfigProp, defaultAlg, getSupportedSignatureAlgorithms() });
			return defaultAlg;
		}
		return publicKeyAlgMpConfigProp;
	}

	private boolean isSupportedSignatureAlgorithm(String sigAlg) {
		if (sigAlg == null) {
			return false;
		}
		return getSupportedSignatureAlgorithms().contains(sigAlg);
	}

	private List<String> getSupportedSignatureAlgorithms() {
		return Arrays.asList("RS256", "RS384", "RS512", "HS256", "HS384", "HS512", "ES256", "ES384", "ES512");
	}

	public List<String> getConfiguredAudiences(JwtConsumerConfig config) {
		List<String> audiences = config.getAudiences();
		if (audiences != null) {
			// Server configuration takes precedence over MP Config property
			// values
			return audiences;
		}
		return getAudiencesFromMpConfigProps();
	}

	List<String> getAudiencesFromMpConfigProps() {
		List<String> audiences = null;
		String audiencesMpConfigProp = get(VERIFY_AUDIENCES);
		if (audiencesMpConfigProp == null) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc,
						"Didn't find " + VERIFY_AUDIENCES + " property in MP Config props; defaulting to " + audiences);
			}
			return audiences;
		}
		audiences = new ArrayList<String>();
		String[] splitAudiences = audiencesMpConfigProp.split(",");
		for (String rawAudience : splitAudiences) {
			if (!rawAudience.isEmpty()) {
				audiences.add(rawAudience);
			}
		}
		return audiences;
	}

	@Override
	public String toString() {
		String string = "{";
		Set<String> sensitiveProps = MpConfigProperties.getSensitivePropertyNames();
		Iterator<Entry<String, String>> iter = entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, String> entry = iter.next();
			String key = entry.getKey();
			string += key + "=";
			if (sensitiveProps.contains(key)) {
				string += "****";
			} else {
				string += entry.getValue();
			}
			if (iter.hasNext()) {
				string += ", ";
			}
		}
		string += "}";
		return string;
	}

}
