package com.ibm.ws.security.social.fat.okdServiceLogin.commonTests;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * Common tools for OKD Service Login tests
 **/

@LibertyServerWrapper
@Mode(TestMode.FULL)
public class OKDServiceLoginCommonTest extends SocialCommonTest {

    public static Class<?> thisClass = OKDServiceLoginCommonTest.class;

    protected static final String stubbedServiceAccountToken = "{\"kind\":\"User\",\"apiVersion\":\"user.openshift.io/v1\",\"metadata\":{\"name\":\"system:serviceaccount:openshift:token-checker-01\",\"selfLink\":\"/apis/user.openshift.io/v1/users/system%3Aserviceaccount%3Aopenshift%3Atoken-checker-01\",\"uid\":\"1cc6e5fe-f698-11e9-aec1-0016ac102aea\",\"creationTimestamp\":null},\"identities\":null,\"groups\":[\"system:authenticated\",\"system:serviceaccounts\",\"system:serviceaccounts:openshift\"]}";

    public static String serviceAccountToken = null;
    public static boolean stubbedTests = false;

    public static SocialTestSettings updateOkdServiceLoginSettings(SocialTestSettings socialSettings, TestServer protectedResourceServer) throws Exception {
        return updateOkdServiceLoginSettings(socialSettings, protectedResourceServer, protectedResourceServer);
    }

    public static SocialTestSettings updateOkdServiceLoginSettings(SocialTestSettings socialSettings, TestServer protectedResourceServer, TestServer opServer) throws Exception {

        socialSettings.setProtectedResource(protectedResourceServer.getServerHttpsString() + "/helloworld/rest/helloworld");
        if (stubbedTests) {
            // hard code values as the data used for
            socialSettings.setAdminUser("openshift:token-checker-01");
            socialSettings.setUserName("openshift:token-checker-01");
            socialSettings.setUserId("openshift:token-checker-01");
            socialSettings.setRealm(opServer.getServerHttpsString() + "/StubbedOKDServiceLogin");

        } else {
            String userapi = protectedResourceServer.getBootstrapProperty("oauth.server.userapi");
            if (userapi == null) {
                socialSettings.setRealm(null);
            } else {
                socialSettings.setRealm(userapi.replace("\\", ""));
            }
            String project = protectedResourceServer.getBootstrapProperty("service.acct.project");
            String user = protectedResourceServer.getBootstrapProperty("service.acct.id");
            socialSettings.setAdminUser(project + ":" + user);
            socialSettings.setUserName(project + ":" + user);
        }
        return socialSettings;
    }

}
