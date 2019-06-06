package com.ibm.ws.security.fat.common.jwt.utils;

import org.jose4j.jws.AlgorithmIdentifiers;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;

public class JwtTokenBuilderUtils {

    protected static Class<?> thisClass = JwtTokenBuilderUtils.class;

    /**
     * Create a new JWTTokenBuilder and initialize it with default test values
     *
     * @return - an initialized JWTTokenBuilder
     * @throws Exception
     */
    public JWTTokenBuilder createBuilderWithDefaultClaims() throws Exception {

        JWTTokenBuilder builder = new JWTTokenBuilder();
        builder.setIssuer("client01");
        builder.setIssuedAtToNow();
        builder.setExpirationTimeMinutesIntheFuture(5);
        builder.setScope("openid profile");
        builder.setSubject("testuser");
        builder.setRealmName("BasicRealm");
        builder.setTokenType("Bearer");
        builder = builder.setAlorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
        builder = builder.setHSAKey("mySharedKeyNowHasToBeLongerStrongerAndMoreSecure");

        return builder;
    }

    /*
     * Wrap the call to the builder so that we can log the raw values and the generated token
     * for debug purposes and not have to duplicate 3 simple lines of code
     */
    public String buildToken(JWTTokenBuilder builder, String testName) throws Exception {
        Log.info(thisClass, "buildToken", "testing _testName: " + testName);
        Log.info(thisClass, testName, "Json claims:" + builder.getJsonClaims());
        String jwtToken = builder.build();
        Log.info(thisClass, testName, "built jwt:" + jwtToken);
        return jwtToken;
    }

    public void updateBuilderWithRSASettings(JWTTokenBuilder builder) {
        builder.setAlorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        builder.setRSAKey();
    }
}
