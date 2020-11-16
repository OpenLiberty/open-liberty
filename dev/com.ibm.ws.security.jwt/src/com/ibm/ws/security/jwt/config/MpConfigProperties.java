package com.ibm.ws.security.jwt.config;

import java.util.ArrayList;
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
            // Server configuration takes precedence over MP Config property values
            return signatureAlgorithm;
        }
        return getSignatureAlgorithmFromMpConfigProps();
    }

    String getSignatureAlgorithmFromMpConfigProps() {
        String defaultAlg = "RS256";
        String publicKeyAlgMpConfigProp = get(PUBLIC_KEY_ALG);
        if (publicKeyAlgMpConfigProp == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Didn't find " + PUBLIC_KEY_ALG + " property in MP Config props; defaulting to " + defaultAlg);
            }
            return defaultAlg;
        }
        if (!isSupportedSignatureAlgorithm(publicKeyAlgMpConfigProp)) {
            Tr.warning(tc, "MP_CONFIG_PUBLIC_KEY_ALG_NOT_SUPPORTED", new Object[] { publicKeyAlgMpConfigProp, defaultAlg });
            return defaultAlg;
        }
        return publicKeyAlgMpConfigProp;
    }

    private boolean isSupportedSignatureAlgorithm(String sigAlg) {
        if (sigAlg == null) {
            return false;
        }
        return sigAlg.matches("[RHE]S(256|384|512)");
    }

    public List<String> getConfiguredAudiences(JwtConsumerConfig config) {
        List<String> audiences = config.getAudiences();
        if (audiences != null) {
            // Server configuration takes precedence over MP Config property values
            return audiences;
        }
        return getAudiencesFromMpConfigProps();
    }

    List<String> getAudiencesFromMpConfigProps() {
        List<String> audiences = null;
        String audiencesMpConfigProp = get(VERIFY_AUDIENCES);
        if (audiencesMpConfigProp == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Didn't find " + VERIFY_AUDIENCES + " property in MP Config props; defaulting to " + audiences);
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
