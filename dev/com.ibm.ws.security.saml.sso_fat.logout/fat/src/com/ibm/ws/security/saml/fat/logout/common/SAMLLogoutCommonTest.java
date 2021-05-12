/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.saml.fat.logout.common;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TestRule;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Utils;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.fat.common.utils.MySkipRule;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestTools;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestServer;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

public abstract class SAMLLogoutCommonTest extends SAMLCommonTest {

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();
    public static final String HttpServletRequestAppMsg = "HttpServletRequestApp Logout";

    // adding rules in the main test class so we have access to all of the flags that are set
    // Some attributes are only ever defined in the generic config, while others are always only in the specific config
    // GenericConfig/SpecificConfig is useful to rule in/out those types of tests

    /*********** NOTE: callSpecificCheck's return TRUE to SKIP a test ***********/

    /**
     * Rule to skip test if:
     * The SP is using the LTPA token/cookie instead of an SP Cookie
     *
     * @author chrisc
     *
     */
    public static class skipIfUsingLTPA extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            String methodName = "skipIfUsintLTPA";
            if (cookieInfo.getSPCookieType() == CookieType.LTPACOOKIE) {
                Log.info(thisClass, methodName, "LTPA in use - skip test");
                testSkipped();
                return true;
            }
            Log.info(thisClass, methodName, "NOT LTPA - run test");
            return false;
        }
    }

    /**
     * Rule to skip test if:
     * The SP is using the LTPA token/cookie instead of an SP Cookie
     *
     * @author chrisc
     *
     */
    public static class skipIfUsingSPCookie extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            String methodName = "skipIfUsingSPCookie";
            if (cookieInfo.getSPCookieType() != CookieType.LTPACOOKIE) {
                Log.info(thisClass, methodName, "SP Cookie in use - skip test");
                testSkipped();
                return true;
            }
            Log.info(thisClass, methodName, "SP Cookie - run test");
            return false;
        }
    }

    protected static final boolean LogoutStaysInSPOnly = true;
    protected static final boolean LOGOUT_INVOLVES_IDP = false;

    protected static final boolean LoggedInToMultipleSPs = true;
    protected static final boolean LoggedInToOneSP = false;

    private static final String CustomPostLogoutTitle = "Custom SAML Single Logout (SLO) Post Logout";
    private static final String CustomPostLogoutStatus = "A SLO has completed successfully";

    private static final String ExternalPostLogoutTitle = "Example Domain";
    private static final String ExternalPostLogoutStatus = "Example Domain";

    final static Class<?> thisClass = SAMLLogoutCommonTest.class;

    public static SAMLCommonTestTools samlcttools = new SAMLCommonTestTools();
    public static CookieInfo cookieInfo = null;
    public static Testusers testUsers = null;

    static String[] allSPCookies_list = null;
    //    String[] twoServer_allSPCookies_list = null;
    static String[] idp_session_list = null;
    //    String[] sp1_list = null;
    //    String[] ltpa_list = null;

    // logout flow type
    //  HTTPSERVLETLOCAL - httpServletRequest.logout with servletRequestLogoutPerformsSamlLogout=valse
    //  HTTPSERVLETREMOTE - httpServletRequest.logout with servletRequestLogoutPerformsSamlLogout=true
    //  INPTINITIATED - IDP initiated (https://localhost:<idpPort>/idp/Logout)
    //  SPINITIATED - SP initiated (https://localhost:<spPort>/samlclient/ibm_security_logout)
    public static enum LogoutFlowType {
        HTTPSERVLETLOCAL, HTTPSERVLETREMOTE, IBMSECURITYLOCAL, IBMSECURITYREMOTE, IDPINITIATED, SPINITIATED
    };

    static LogoutFlowType logoutFlowType;

    // Tests support 3 different post logout pages
    public static enum PostLogoutPage {
        DEFAULTPOSTLOGOUTPAGE, CUSTOMPOSTLOGOUTPAGE, EXTERNALPOSTLOGOUTPAGE, NEGATIVEDEFAULTPOSTLOGOUTPAGE
    };

    protected static String[] loginLogoutFlow;
    protected static String[] logoutFlow;
    protected static String[] justLogout;
    protected static String[] logoutWithSessions;
    protected static String logoutStep = null;
    protected static String lastLoginStep;

    public static enum CookieType {
        LTPACOOKIE, SPCOOKIES, MIXEDCOOKIES
    }

    public static class CookieInfo {

        private CookieType spCookieType;
        private String sp1CookieName;
        private String sp2CookieName;
        private String sp5CookieName;
        private String sp13CookieName;
        private String server2_sp1CookieName;
        private String server2_sp2CookieName;
        private String emptyStringCookieName;
        private String invalidURLCookieName;
        private String relativePathCookieName;
        private String specialRelativePathCookieName;
        private String invalidRelativePathCookieName;
        private String absLocalURLCookieName;
        private String absExternalURLCookieName;
        private String spUnderscoreCookieName;
        private String spDashCookieName;
        private String defaultSPCookieName;
        private String serverFileExtension = null;

        public CookieInfo(CookieType cookieType) {

            Log.info(thisClass, "CookieInfo", "======================================================================================");
            spCookieType = cookieType;

            switch (cookieType) {
            case LTPACOOKIE:
                Log.info(thisClass, "CookieInfo", "Using LTPA type Cookies");
                sp1CookieName = SAMLConstants.LTPA_TOKEN_NAME;
                sp2CookieName = SAMLConstants.LTPA_TOKEN_NAME;
                sp5CookieName = SAMLConstants.LTPA_TOKEN_NAME;
                sp13CookieName = SAMLConstants.LTPA_TOKEN_NAME;
                server2_sp1CookieName = SAMLConstants.LTPA_TOKEN_NAME;
                server2_sp2CookieName = SAMLConstants.LTPA_TOKEN_NAME;
                emptyStringCookieName = SAMLConstants.LTPA_TOKEN_NAME;
                invalidURLCookieName = SAMLConstants.LTPA_TOKEN_NAME;
                relativePathCookieName = SAMLConstants.LTPA_TOKEN_NAME;
                specialRelativePathCookieName = SAMLConstants.LTPA_TOKEN_NAME;
                invalidRelativePathCookieName = SAMLConstants.LTPA_TOKEN_NAME;
                absLocalURLCookieName = SAMLConstants.LTPA_TOKEN_NAME;
                absExternalURLCookieName = SAMLConstants.LTPA_TOKEN_NAME;
                spUnderscoreCookieName = SAMLConstants.LTPA_TOKEN_NAME;
                spDashCookieName = SAMLConstants.LTPA_TOKEN_NAME;
                defaultSPCookieName = SAMLConstants.LTPA_TOKEN_NAME;
                serverFileExtension = "_LTPACookie";
                break;
            case SPCOOKIES:
                Log.info(thisClass, "CookieInfo", "====   Using SP type Cookies");
                sp1CookieName = SAMLConstants.SP_COOKIE_PREFIX + "sp1";
                sp2CookieName = SAMLConstants.SP_COOKIE_PREFIX + "sp2";
                sp5CookieName = SAMLConstants.SP_COOKIE_PREFIX + "sp5";
                sp13CookieName = SAMLConstants.SP_COOKIE_PREFIX + "_sp13";
                server2_sp1CookieName = SAMLConstants.SP_COOKIE_PREFIX + "server2_sp1";
                server2_sp2CookieName = SAMLConstants.SP_COOKIE_PREFIX + "server2_sp2";
                emptyStringCookieName = SAMLConstants.SP_COOKIE_PREFIX + "customLogout_emptyString";
                invalidURLCookieName = SAMLConstants.SP_COOKIE_PREFIX + "customLogout_invalidURL";
                relativePathCookieName = SAMLConstants.SP_COOKIE_PREFIX + "customLogout_relativePath";
                specialRelativePathCookieName = SAMLConstants.SP_COOKIE_PREFIX + "customLogout_specialRelativePath";
                invalidRelativePathCookieName = SAMLConstants.SP_COOKIE_PREFIX + "customLogout_invalidRelativePath";
                absLocalURLCookieName = SAMLConstants.SP_COOKIE_PREFIX + "customLogout_absLocalURL";
                absExternalURLCookieName = SAMLConstants.SP_COOKIE_PREFIX + "customLogout_absExternalURL";
                spUnderscoreCookieName = SAMLConstants.SP_COOKIE_PREFIX + "sp_underscore";
                spDashCookieName = SAMLConstants.SP_COOKIE_PREFIX + "sp-dash";
                defaultSPCookieName = SAMLConstants.SP_COOKIE_PREFIX + "defaultSP";
                serverFileExtension = "_SPCookie";
                break;
            case MIXEDCOOKIES:
                Log.info(thisClass, "CookieInfo", "Using Mixed LTPA and SP type Cookies");
            default:
                sp1CookieName = SAMLConstants.SP_COOKIE_PREFIX + "sp1";
                sp2CookieName = SAMLConstants.SP_COOKIE_PREFIX + "sp2";
                sp5CookieName = SAMLConstants.SP_COOKIE_PREFIX + "sp5";
                sp13CookieName = SAMLConstants.LTPA_TOKEN_NAME;
                server2_sp1CookieName = SAMLConstants.SP_COOKIE_PREFIX + "server2_sp1";
                server2_sp2CookieName = SAMLConstants.SP_COOKIE_PREFIX + "server2_sp2";
                emptyStringCookieName = SAMLConstants.SP_COOKIE_PREFIX + "customLogout_emptyString";
                invalidURLCookieName = SAMLConstants.SP_COOKIE_PREFIX + "customLogout_invalidURL";
                relativePathCookieName = SAMLConstants.SP_COOKIE_PREFIX + "customLogout_relativePath";
                specialRelativePathCookieName = SAMLConstants.SP_COOKIE_PREFIX + "customLogout_specialRelativePath";
                invalidRelativePathCookieName = SAMLConstants.SP_COOKIE_PREFIX + "customLogout_invalidRelativePath";
                absLocalURLCookieName = SAMLConstants.SP_COOKIE_PREFIX + "customLogout_absLocalURL";
                absExternalURLCookieName = SAMLConstants.SP_COOKIE_PREFIX + "customLogout_absExternalURL";
                spUnderscoreCookieName = SAMLConstants.SP_COOKIE_PREFIX + "sp_underscore";
                serverFileExtension = "_MixedCookie";
                spDashCookieName = SAMLConstants.SP_COOKIE_PREFIX + "sp-dash";
                defaultSPCookieName = SAMLConstants.SP_COOKIE_PREFIX + "defaultSP";
                break;
            }
            Log.info(thisClass, "CookieInfo", "======================================================================================");
        }

        public CookieInfo() {
        }

        public CookieType getSPCookieType() {
            return spCookieType;
        }

        public String getSp1CookieName() {
            return sp1CookieName;
        }

        public String getSp2CookieName() {
            return sp2CookieName;
        }

        public String getSp5CookieName() {
            return sp5CookieName;
        }

        public String getSp13CookieName() {
            return sp13CookieName;
        }

        public String getServer2Sp1CookieName() {
            return server2_sp1CookieName;
        }

        public String getServer2Sp2CookieName() {
            return server2_sp2CookieName;
        }

        public String getEmptyStringCookieName() {
            return emptyStringCookieName;
        }

        public String getInvalidURLCookieName() {
            return invalidURLCookieName;
        }

        public String getRelativePathCookieName() {
            return relativePathCookieName;
        }

        public String getSpecialRelativePathCookieName() {
            return specialRelativePathCookieName;
        }

        public String getInvalidRelativePathCookieName() {
            return invalidRelativePathCookieName;
        }

        public String getAbsLocalURLCookieName() {
            return absLocalURLCookieName;
        }

        public String getAbsExternalURLCookieName() {
            return absExternalURLCookieName;
        }

        public String getSpUnderscoreCookieName() {
            return spUnderscoreCookieName;
        }

        public String getSpDashCookieName() {
            return spDashCookieName;
        }

        public String getDefaultSPCookieName() {
            return defaultSPCookieName;
        }

        public String getCookieFileExtension() {
            return serverFileExtension;
        }

    }

    public static CookieType chooseCookieSetting() throws Exception {
        //        return CookieType.SPCOOKIES;
        //        return CookieType.LTPACOOKIE;
        return Utils.getRandomSelection(CookieType.LTPACOOKIE, CookieType.SPCOOKIES);
        //        return Utils.getRandomSelection(CookieType.LTPACOOKIE, CookieType.SPCOOKIES, CookieType.MIXEDCOOKIES);
    }

    public static enum UserType {
        SAME, DIFFERENT
    }

    public static class Testusers {

        private final UserType testUserType;
        private String user1;
        private String password1;
        private String user2;
        private String password2;
        private String user3;
        private String password3;
        private String user4;
        private String password4;
        private String user5;
        private String password5;

        public Testusers(UserType userType) {

            Log.info(thisClass, "CookieInfo", "======================================================================================");
            testUserType = userType;

            switch (userType) {
            case DIFFERENT:
                Log.info(thisClass, "Testusers", "Using a different user for all logins");
                user1 = SAMLConstants.IDP_USER_NAME;
                password1 = SAMLConstants.IDP_USER_PWD;
                user2 = "john_vmmUser";
                password2 = "john_vmmUser";
                user3 = "ping_vmmUser";
                password3 = "ping_vmmUser";
                user4 = "pong_vmmUser";
                password4 = "pong_vmmUser";
                user5 = "connect_vmmUser";
                password5 = "connect_vmmUser";
                break;
            case SAME:
            default:
                Log.info(thisClass, "Testusers", "Using the same user for all logins");
                user1 = SAMLConstants.IDP_USER_NAME;
                password1 = SAMLConstants.IDP_USER_PWD;
                user2 = SAMLConstants.IDP_USER_NAME;
                password2 = SAMLConstants.IDP_USER_PWD;
                user3 = SAMLConstants.IDP_USER_NAME;
                password3 = SAMLConstants.IDP_USER_PWD;
                user4 = SAMLConstants.IDP_USER_NAME;
                password4 = SAMLConstants.IDP_USER_PWD;
                user5 = SAMLConstants.IDP_USER_NAME;
                password5 = SAMLConstants.IDP_USER_PWD;
                break;
            }
        }

        public UserType getUserType() {
            return testUserType;
        }

        public String getUser1() {
            return user1;
        }

        public String getPassword1() {
            return password1;
        }

        public String getUser2() {
            return user2;
        }

        public String getPassword2() {
            return password2;
        }

        public String getUser3() {
            return user3;
        }

        public String getPassword3() {
            return password3;
        }

        public String getUser4() {
            return user4;
        }

        public String getPassword4() {
            return password4;
        }

        public String getUser5() {
            return user5;
        }

        public String getPassword5() {
            return password5;
        }

    }

    public static UserType chooseUsers() throws Exception {

        return UserType.SAME;
        //        return UserType.DIFFERENT;
        //        return Utils.getRandomSelection(UserType.SAME, UserType.DIFFERENT);
    }

    public static void setLogoutFlowSettings(SAMLTestSettings settings, String loginType, String logoutType, boolean localOnly) throws Exception {

        String thisMethod = "setLogoutFlowSettings";

        String[] loginPart = null;

        Log.info(thisClass, thisMethod, "Login Type: " + loginType + ", Logout type: " + logoutType + ", is Local Only: " + localOnly);

        // set the login portion of the flow for an end to end test (test that logs in and logs out)
        if (loginType.equals(SAMLConstants.IDP_INITIATED)) {
            loginPart = SAMLConstants.IDP_INITIATED_FLOW_KEEPING_COOKIES;
        }
        if (loginType.equals(SAMLConstants.SOLICITED_SP_INITIATED)) {
            loginPart = SAMLConstants.SOLICITED_SP_INITIATED_FLOW_KEEPING_COOKIES;
        }
        if (loginType.equals(SAMLConstants.UNSOLICITED_SP_INITIATED)) {
            loginPart = SAMLConstants.UNSOLICITED_SP_INITIATED_FLOW_KEEPING_COOKIES;
        }

        // set the logout flow variables
        // settings for logouts initiated on the IDP
        if (logoutType.equals(SAMLConstants.IDP_INITIATED)) {
            justLogout = SAMLConstants.IDP_INITIATED_LOGOUT_ONLY;
            logoutWithSessions = SAMLConstants.IDP_INITIATED_LOGOUT_LOADING_SESSIONS;
            logoutStep = SAMLConstants.PERFORM_IDP_LOGOUT;
            logoutFlowType = LogoutFlowType.IDPINITIATED;
            settings.setIdpLogoutURL(setIDPInitiatedLogoutURL(testIDPServer));
            // the flow in the IDP varies based on the login that was performed, set logout flow accordingly
            if (loginType.equals(SAMLConstants.SOLICITED_SP_INITIATED)) {
                logoutFlow = SAMLConstants.IDP_INITIATED_LOGOUT_NO_SESSIONS;
            } else {
                logoutFlow = SAMLConstants.IDP_INITIATED_LOGOUT;
            }
        }
        // settings for logouts initiated on the SP
        if (logoutType.equals(SAMLConstants.SP_INITIATED)) {
            logoutFlowType = LogoutFlowType.SPINITIATED;
            justLogout = SAMLConstants.SP_INITIATED_LOGOUT_ONLY;
            logoutWithSessions = SAMLConstants.SP_INITIATED_LOGOUT_LOADING_SESSIONS;
            logoutStep = SAMLConstants.PERFORM_SP_LOGOUT;
            settings.setSpLogoutURL(setSPInitiatedLogoutURL(testSAMLServer));
            // the flow in the IDP varies based on the login that was performed, set logout flow accordingly
            if (loginType.equals(SAMLConstants.SOLICITED_SP_INITIATED)) {
                logoutFlow = SAMLConstants.SP_INITIATED_LOGOUT;
            } else {
                logoutFlow = SAMLConstants.SP_INITIATED_LOGOUT_LOADING_SESSIONS;
            }
        }

        // settings for logouts initiated with httpServletRequest.logout
        // behavior varies base on the setting of the spLogout config attribute
        // if it's value is "false", localOnly is set
        if (logoutType.equals(SAMLConstants.HTTPSERVLET_INITIATED)) {
            if (localOnly) {
                logoutFlow = SAMLConstants.SP_INITIATED_LOGOUT_ONLY;
                logoutFlowType = LogoutFlowType.HTTPSERVLETLOCAL;
            } else {
                if (loginType.equals(SAMLConstants.SOLICITED_SP_INITIATED)) {
                    logoutFlow = SAMLConstants.SP_INITIATED_LOGOUT;
                } else {
                    logoutFlow = SAMLConstants.SP_INITIATED_LOGOUT_LOADING_SESSIONS;
                }
                logoutFlowType = LogoutFlowType.HTTPSERVLETREMOTE;
            }
            justLogout = SAMLConstants.SP_INITIATED_LOGOUT_ONLY;
            logoutWithSessions = SAMLConstants.SP_INITIATED_LOGOUT_LOADING_SESSIONS;
            logoutStep = SAMLConstants.PERFORM_SP_LOGOUT;
            // set the logout app url
            settings.setSpLogoutURL(setSPServletRequestLogoutURL(testSAMLServer));
        }

        // settings for logouts initiated with httpServletRequest.logout
        // behavior varies base on the setting of the spLogout config attribute
        // if it's value is "false", localOnly is set
        if (logoutType.equals(SAMLConstants.IBMSECURITYLOGOUT_INITIATED)) {
            if (localOnly) {
                logoutFlow = SAMLConstants.SP_INITIATED_LOGOUT_ONLY;
                logoutFlowType = LogoutFlowType.IBMSECURITYLOCAL;
            } else {
                if (loginType.equals(SAMLConstants.SOLICITED_SP_INITIATED)) {
                    logoutFlow = SAMLConstants.SP_INITIATED_LOGOUT;
                } else {
                    logoutFlow = SAMLConstants.SP_INITIATED_LOGOUT_LOADING_SESSIONS;
                }
                logoutFlowType = LogoutFlowType.IBMSECURITYREMOTE;
            }
            justLogout = SAMLConstants.SP_INITIATED_LOGOUT_ONLY;
            logoutWithSessions = SAMLConstants.SP_INITIATED_LOGOUT_LOADING_SESSIONS;
            logoutStep = SAMLConstants.PERFORM_SP_LOGOUT;
            settings.setSpLogoutURL(setIBMSecurityLogoutURL(testSAMLServer));
        }
        

        Log.info(thisClass, thisMethod, "loginPart: " + StringUtils.join(loginPart, ", "));
        Log.info(thisClass, thisMethod, "logoutFlow: " + StringUtils.join(logoutFlow, ", "));
        Log.info(thisClass, thisMethod, "justLogout: " + StringUtils.join(justLogout, ", "));
        Log.info(thisClass, thisMethod, "logoutWithSessions: " + StringUtils.join(logoutWithSessions, ", "));
        Log.info(thisClass, thisMethod, "logoutStep: " + logoutStep);
        Log.info(thisClass, thisMethod, "currentFlow: " + logoutFlowType);

        lastLoginStep = loginPart[loginPart.length - 1];
        Log.info(thisClass, thisMethod, "lastLoginStep: " + lastLoginStep);
        loginLogoutFlow = concatStringArrays(loginPart, logoutFlow);
        Log.info(thisClass, thisMethod, "loginLogoutFlow: " + StringUtils.join(loginLogoutFlow, ", "));

        settings.setLocalLogoutOnly(localOnly);
        settings = updateContextRoot(settings);

    }

    public static String[] concatStringArrays(String[] part1, String[] part2) throws Exception {
        ArrayList<String> combined = new ArrayList<String>(Arrays.asList(part1));
        combined.addAll(Arrays.asList(part2));
        return combined.toArray(new String[combined.size()]);
    }

    public static String[] removeFromStringArray(String[] theArray, String toBeRemoved) throws Exception {

        final List<String> list = new ArrayList<String>();
        Collections.addAll(list, theArray);
        list.remove(toBeRemoved);
        return list.toArray(new String[list.size()]);
    }

    /**
     *
     * @param settings
     * @param server
     *            - SP server
     * @return
     * @throws Exception
     */
    public static String setIBMSecurityLogoutURL(SAMLTestServer server) throws Exception {
        return server.getHttpsString() + "/samlclient_defaultSP/ibm_security_logout";
    }

    /**
     *
     * @param settings
     * @param server
     *            - SP server
     * @return
     * @throws Exception
     */
    public static String setSPServletRequestLogoutURL(SAMLTestServer server) throws Exception {
        //        return server.getHttpsString() + "/httpServletRequestApp/httpServletRequestApp";
        // need to be able to tie the logout to an sp, so, need to include the sp name in
        // the url, but for sp's like sp1, ending the url with sp1 would cause a filter match with sp13 too...
        //        return server.getHttpsString() + "/httpServletRequestApp/defaultSP/logout";
        return server.getHttpsString() + "/httpServletRequestApp/defaultSP/logout";
    }

    /**
     *
     * @param settings
     * @param server
     *            - IDP server
     * @return
     * @throws Exception
     */
    public static String setIDPInitiatedLogoutURL(SAMLTestServer server) throws Exception {
        return server.getHttpsString() + "/idp/profile/Logout";
    }

    /**
     *
     * @param settings
     * @param server
     *            - SP Server
     * @return
     * @throws Exception
     */
    public static String setSPInitiatedLogoutURL(SAMLTestServer server) throws Exception {
        return server.getHttpsString() + "/ibm/saml20/defaultSP/logout";
    }

    /**
     * Determine the first step in the login process
     *
     * @return - the first step in the login process
     */
    public String getStartAction() {
        if (flowType.equals(SAMLConstants.IDP_INITIATED)) {
            return SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST;
        } else if (flowType.equals(SAMLConstants.SOLICITED_SP_INITIATED)) {
            return SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST;
        } else if (flowType.equals(SAMLConstants.UNSOLICITED_SP_INITIATED)) {
            return SAMLConstants.PROCESS_IDP_JSP;
        } else {
            return "Invalid flow type";
        }
    }

    /**
     * Create expectations for cookies that should and should NOT exist at a particular step in the flow
     *
     * @param expectations
     *            - existing expectations (list that we'll add on to)
     * @param step
     *            - the step in the process that we should check the existince of or lack of cookies
     * @param shouldExistCookies
     *            - cookies that should exist at this step
     * @param shouldNotExistCookies
     *            - cookies that should not exist at this step
     * @return - updated expectations
     * @throws Exception
     */
    public List<validationData> addCookieExpectations(String step, String[] shouldExistCookies, String[] shouldNotExistCookies) throws Exception {
        return addCookieExpectations(null, step, shouldExistCookies, shouldNotExistCookies);
    }

    public List<validationData> addCookieExpectations(List<validationData> expectations, String step, String[] shouldExistCookies, String[] shouldNotExistCookies) throws Exception {

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes();
        }

        // add expectations for cookies that SHOULD exist
        if (shouldExistCookies != null) {
            for (String cookie : shouldExistCookies) {
                expectations = vData.addExpectation(expectations, step, SAMLConstants.COOKIES, SAMLConstants.STRING_CONTAINS, "Conversation did NOT have an " + cookie + " Cookie.", cookie, null);
            }
        }

        // add expectations for cookies that SHOULD NOT exist
        if (shouldNotExistCookies != null) {
            for (String cookie : shouldNotExistCookies) {
                expectations = vData.addExpectation(expectations, step, SAMLConstants.COOKIES, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Conversation has an " + cookie + " Cookie and should NOT.", cookie, null);
            }
        }

        return expectations;
    }

    /**
     * Add expectations based on the flow that we're using
     * IDP initiated, Solicited SP initiated, and Unsolicited SP initiated have different flows (use different frames) to
     * accomplish a log in. We're not
     * focusing on those steps with these tests, but, would like to make sure that the login was good before we test the logout.
     *
     * @param actions
     *            - actions/steps that the login will be using
     * @param settings
     *            - settings for the calling test case (used to build response values to validate)
     * @return - expectations to be checked later
     * @throws Exception
     */
    public List<validationData> setDefaultGoodSAMLLoginExpectations(String[] actions, SAMLTestSettings settings, String[] spCookieNames) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes();
        return setDefaultGoodSAMLLoginExpectations(expectations, actions, settings, spCookieNames);
    }

    public List<validationData> setDefaultGoodSAMLLoginExpectations(List<validationData> expectations, String[] actions, SAMLTestSettings settings, String[] spCookieNames) throws Exception {

        String[] allSPCookies = { SAMLConstants.SP_COOKIE_PREFIX + "sp1", SAMLConstants.SP_COOKIE_PREFIX + "sp2", SAMLConstants.LTPA_TOKEN_NAME };
        if (samlcttools.isInList(actions, SAMLConstants.PERFORM_IDP_LOGIN)) {
            int index = Arrays.asList(actions).lastIndexOf(SAMLConstants.PERFORM_IDP_LOGIN);
            if (index == 0) {
                expectations = vData.addExpectation(expectations, getStartAction(), SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on the IDP form login form.", null, samlcttools.getLoginTitle(settings.getIdpRoot()));
            } else {
                expectations = vData.addExpectation(expectations, actions[index - 1], SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on the IDP form login form.", null, samlcttools.getLoginTitle(settings.getIdpRoot()));
            }
        }
        if (samlcttools.isInList(actions, SAMLConstants.PERFORM_IDP_LOGIN) || samlcttools.isInList(actions, SAMLConstants.PROCESS_LOGIN_CONTINUE)) {
            String withAccessAction = SAMLConstants.PERFORM_IDP_LOGIN;
            if (samlcttools.isInList(actions, SAMLConstants.PROCESS_LOGIN_CONTINUE)) {
                withAccessAction = SAMLConstants.PROCESS_LOGIN_CONTINUE;
            }
            expectations = vData.addExpectation(expectations, withAccessAction, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML POST response", null, samlcttools.getResponseTitle(settings.getIdpRoot()));
            expectations = vData.addExpectation(expectations, withAccessAction, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive expected SAML Response", null, SAMLConstants.SAML_RESPONSE);
        }
        if (samlcttools.isInList(actions, SAMLConstants.PROCESS_LOGIN_REQUEST)) {
            expectations = vData.addExpectation(expectations, getStartAction(), SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive request to process", null, SAMLConstants.SAML_REQUEST);

        }
        Log.info(thisClass, "setDefaultGoodSAMLLoginExpectations", "Requested Actions: " + Arrays.toString(actions));
        // need last LOGIN action, not LOGOUT...
        //        String lastAction = actions[actions.length - 1];
        //        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Snoop Servlet", null, SAMLConstants.APP1_TITLE);
        expectations = vData.addExpectation(expectations, lastLoginStep, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Snoop Servlet", null, SAMLConstants.APP1_TITLE);

        ArrayList<String> tmpCookieList = new ArrayList<String>();
        String[] shouldNotExistCookies = null;
        for (String cookie : allSPCookies) {
            if (!cttools.isInList(spCookieNames, cookie)) {
                tmpCookieList.add(cookie);
            }
        }

        if (!tmpCookieList.isEmpty()) {
            shouldNotExistCookies = tmpCookieList.toArray(new String[0]);
        }
        // set cookie expectations for login
        expectations = addCookieExpectations(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES, concatStringArrays(spCookieNames, new String[] { SAMLConstants.IDP_SESSION_COOKIE_NAME }), shouldNotExistCookies);

        return expectations;

    }

    public List<validationData> setGoodSAMLLogoutExpectations(String[] actions, SAMLTestSettings settings, int numSPs) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes();
        return setGoodSAMLLogoutExpectations(expectations, actions, settings, numSPs);
    }

    public List<validationData> setGoodSAMLLogoutExpectations(List<validationData> expectations, String[] actions, SAMLTestSettings settings, int numSPs) throws Exception {
        return setGoodSAMLLogoutExpectations(expectations, actions, settings, numSPs, PostLogoutPage.DEFAULTPOSTLOGOUTPAGE);
    }

    public List<validationData> setGoodSAMLLogoutExpectations(List<validationData> expectations, String[] actions, SAMLTestSettings settings, int numSPs, PostLogoutPage postLogoutPage) throws Exception {
        // if we're using a local sp only logout (HttpServletRequest.logout), we won't have a 2 step logout)
        // If we're going to the IDP, we'll have a 2 step logout
        // The step where we get the logout confirmation will be different

        String postLogoutTitle = null;
        String postLogoutStatus = null;

        // decide which post logout page the test should look for
        switch (postLogoutPage) {
        case CUSTOMPOSTLOGOUTPAGE:
            postLogoutTitle = CustomPostLogoutTitle;
            postLogoutStatus = CustomPostLogoutStatus;
            break;
        case EXTERNALPOSTLOGOUTPAGE:
            postLogoutTitle = ExternalPostLogoutTitle;
            postLogoutStatus = ExternalPostLogoutStatus;
            break;
        case NEGATIVEDEFAULTPOSTLOGOUTPAGE:
            postLogoutTitle = SAMLConstants.SUCCESSFUL_DEFAULT_SP_LOGOUT_TITLE;
            postLogoutStatus = SAMLConstants.FAILED_LOGOUT_MSG;
            break;
        default: // default post logout page is also the default case
            postLogoutTitle = SAMLConstants.SUCCESSFUL_DEFAULT_SP_LOGOUT_TITLE;
            postLogoutStatus = SAMLConstants.SUCCESSFUL_DEFAULT_SP_LOGOUT_STATUS;
        }

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes();
        }

        // determine what intermediate/final pages and msgs we should validate
        String lastStep = actions[actions.length - 1];
        switch (logoutFlowType) {
        case IBMSECURITYLOCAL:
            expectations = vData.addExpectation(expectations, logoutStep, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout page", null, SAMLConstants.SUCCESSFUL_DEFAULT_LOGOUT_TITLE);
            expectations = vData.addExpectation(expectations, logoutStep, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Message", null, SAMLConstants.SUCCESSFUL_DEFAULT_LOGOUT_MSG);
            break;
        case HTTPSERVLETLOCAL:
            expectations = vData.addExpectation(expectations, logoutStep, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Message", null, SAMLConstants.OK_MESSAGE);
            expectations = vData.addExpectation(expectations, logoutStep, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Message", null, HttpServletRequestAppMsg);
            break;
        case IBMSECURITYREMOTE:
        case HTTPSERVLETREMOTE:
            //        case SPINITIATED:
            expectations = vData.addExpectation(expectations, logoutStep, SAMLConstants.RESPONSE_URL, SAMLConstants.STRING_CONTAINS, "Did not get to the Logout submit page", null, settings.getSpLogoutURL());
            //                        expectations = vData.addExpectation(expectations, SAMLConstants.PROCESS_LOGOUT_REQUEST, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout page", null, SAMLConstants.SAML_SHIBBOLETH_LOGIN_HEADER);
            expectations = vData.addExpectation(expectations, lastStep, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Page (Title check)", null, postLogoutTitle);
            expectations = vData.addExpectation(expectations, lastStep, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Page (Status check)", null, postLogoutStatus);
            if (samlcttools.isInList(actions, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES)) {
                if (numSPs > 1) {
                    //                    expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Server did not log an error message", SAMLMessageConstants.CWWKS5251W_SAML_TOKEN_NOT_IN_SUBJECT);
                    testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5251W_SAML_TOKEN_NOT_IN_SUBJECT);
                } else {
                    String idpLogoutComplete = actions[ArrayUtils.indexOf(actions, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES) - 1];
                    expectations = vData.addExpectation(expectations, idpLogoutComplete, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Message", null, SAMLConstants.SUCCESSFUL_SHIBBOLETH_SP_INIT_LOGOUT_MSG);
                }
            }
            break;
        case IDPINITIATED:
            expectations = vData.addExpectation(expectations, logoutStep, SAMLConstants.RESPONSE_URL, SAMLConstants.STRING_CONTAINS, "Did not get to the Logout submit page", null, settings.getIdpLogoutURL());
            expectations = vData.addExpectation(expectations, SAMLConstants.PROCESS_LOGOUT_REDIRECT, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the SP Successful Logout message", null, SAMLConstants.IDP_INIT_SP_LOGOUT_SUCCESS);
            if (samlcttools.isInList(actions, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES)) {
                if ((numSPs > 2) && (flowType.equals(SAMLConstants.IDP_INITIATED))) {
                    //                    expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Server did not log an error message", SAMLMessageConstants.CWWKS5251W_SAML_TOKEN_NOT_IN_SUBJECT);
                } else {
                    String idpLogoutComplete = actions[ArrayUtils.indexOf(actions, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES) - 1];
                    expectations = vData.addExpectation(expectations, idpLogoutComplete, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Message", null, SAMLConstants.SUCCESSFUL_SHIBBOLETH_IDP_INIT_LOGOUT_MSG);
                }
            }
            testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5251W_SAML_TOKEN_NOT_IN_SUBJECT);
            break;
        default:
            fail("Unknown flow type from setGoodSAMLLogoutExpectations");
        }

        return expectations;
    }

    public List<validationData> setCookieExpectationsForFlow(List<validationData> expectations, String[] actions, boolean multipleSPs, String[] spCookieNames, String targetSP) throws Exception {

        String[] allCookies_list = concatStringArrays(allSPCookies_list, idp_session_list);
        String lastAction = actions[actions.length - 1];

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes();
        }

        // set cookie checks for step where SP cookies should be deleted, then for the step where the idp-session cookie should be deleted
        // if we are using sp initiated logout, the sp cookies are removed first, if we're using idp initiated, the idp session cookie will be removed first

        switch (logoutFlowType) {
        case IBMSECURITYLOCAL:
        case HTTPSERVLETLOCAL:
            // after initial logout call, all sp cookies should be gone, but the idp session cookie should still exist
            expectations = addCookieExpectations(expectations, logoutStep, idp_session_list, allSPCookies_list);
            if (!logoutStep.equals(lastAction)) {
                // after the last call, all sp cookies should be gone, but the idp session cookie will still exist because we don't tell the idp to logout
                expectations = addCookieExpectations(expectations, lastAction, idp_session_list, allSPCookies_list);
            }
            break;
        case IBMSECURITYREMOTE:
        case HTTPSERVLETREMOTE:
            // should be the same as spinitiated
            //            fail("HTTPServletRequest.logout redirecting to the IDP NOT IMPLEMENTED YET");
            // after initial logout call, only the target SP's cookie should be gone
            expectations = addCookieExpectations(expectations, logoutStep, idp_session_list, new String[] { targetSP });
            // after the last call, all sp cookies and the idp session cookie should be gone
            expectations = addCookieExpectations(expectations, lastAction, null, allCookies_list);
            break;
        //        case SPINITIATED:
        //            // after initial logout call, all sp cookies should be gone, but the idp session cookie should still exist
        //            expectations = addCookieExpectations(expectations, logoutStep, idp_session_list, allSPCookies_list);
        //            // after the last call, all sp cookies and the idp session cookie should be gone
        //            expectations = addCookieExpectations(expectations, lastAction, null, allCookies_list);
        //            break;
        case IDPINITIATED:
            // after initial logout call, the idp session cookie be gone, but all of the sp cookies should still exist
            String idpSessionRemovedStep = actions[ArrayUtils.indexOf(actions, logoutStep) + 1];
            String[] existCookies = null;
            // propagate = yes step is actually multiple steps and they include calls to the SP, so the sp cookies will go away midway through,
            // so don't check what exists, just make sure that the idp session goes away
            //                        if (!idpSessionRemovedStep.equals(SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES)) {
            //                            existCookies = spCookieNames;
            //                        }
            expectations = addCookieExpectations(expectations, idpSessionRemovedStep, existCookies, idp_session_list);
            //            expectations = addCookieExpectations(expectations, SAMLConstants.PERFORM_IDP_LOGOUT, spCookieNames, idp_session_list);
            // after the last call, all sp cookies and the idp session cookie should be gone
            expectations = addCookieExpectations(expectations, lastAction, null, allCookies_list);
            break;
        default:
            fail("Unknown flow type from setCookieExpectationsForFlow");
        }

        return expectations;
    }

    public List<validationData> setCookieExpectationsFor2ServerFlow(List<validationData> expectations, String[] actions, String[] targetServerCookies, String[] otherServerCookies, String targetSpCookie) throws Exception {

        String lastAction = actions[actions.length - 1];

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes();
        }

        // set cookie checks for step where SP cookies should be deleted, then for the step where the idp-session cookie should be deleted
        // if we are using sp initiated logout, the sp cookies are removed first, if we're using idp initiated, the idp session cookie will be removed first

        switch (logoutFlowType) {
        case IBMSECURITYLOCAL:
        case HTTPSERVLETLOCAL:
            // after initial logout call, all sp cookies on the target server should be gone, but the idp session cookie
            // and the cookies for the other server should still exist
            expectations = addCookieExpectations(expectations, logoutStep, concatStringArrays(idp_session_list, otherServerCookies), targetServerCookies);
            if (!logoutStep.equals(lastAction)) {
                // after the last call, all sp cookies for the target server should be gone, but the idp session cookie and all of the cookies
                // for the other server will still exist because we don't tell the idp to logout
                expectations = addCookieExpectations(expectations, lastAction, concatStringArrays(idp_session_list, otherServerCookies), targetServerCookies);
            }
            break;
        case IBMSECURITYREMOTE:
        case HTTPSERVLETREMOTE:
            String[] remainingCookies = removeFromStringArray(targetServerCookies, targetSpCookie);
            // after initial logout call, only the target SP's cookie should be gone
            expectations = addCookieExpectations(expectations, logoutStep, concatStringArrays(idp_session_list, remainingCookies), new String[] { targetSpCookie });
            // after the last call, all sp cookies for the target server and the idp session cookie should be gone
            expectations = addCookieExpectations(expectations, lastAction, null, concatStringArrays(idp_session_list, concatStringArrays(targetServerCookies, otherServerCookies)));
            break;
        case IDPINITIATED:
            String idpSessionRemovedStep = actions[ArrayUtils.indexOf(actions, logoutStep) + 1];
            // propagate = yes step is actually multiple steps and they include calls to the SP, so the sp cookies will go away midway through,
            // so don't check what exists, just make sure that the idp session goes away
            expectations = addCookieExpectations(expectations, idpSessionRemovedStep, null, idp_session_list);
            // after the last call, all sp cookies and the idp session cookie should be gone
            expectations = addCookieExpectations(expectations, lastAction, null, concatStringArrays(idp_session_list, concatStringArrays(targetServerCookies, otherServerCookies)));
            break;
        default:
            fail("Unknown flow type from setCookieExpectationsForFlow");
        }

        return expectations;
    }

    public void loginToSP(WebClient webClient, String[] flow, String user, String password, String spName, String[] cookies) throws Exception {
        loginToSP(webClient, testSettings, flow, user, password, spName, cookies, false);
    }

    public void loginToSP(WebClient webClient, SAMLTestSettings settings, String[] flow, String user, String password, String spName, String[] cookies) throws Exception {
        loginToSP(webClient, settings, flow, user, password, spName, cookies, false);
    }

    public void loginToSP(WebClient webClient, SAMLTestSettings settings, String[] flow, String user, String password, String spName, String[] cookies, boolean multipleSPs) throws Exception {

        SAMLTestSettings updatedTestSettings = settings.copyTestSettings();
        if (multipleSPs) {
            updatedTestSettings.setDefaultSAMLServerTestSettings("http://localhost:" + testSAMLServer2.getServerHttpPort().toString(), "https://localhost:" + testSAMLServer2.getServerHttpsPort().toString());
            updatedTestSettings.setLocalLogoutOnly(settings.getLocalLogoutOnly()); // setDefaultSAMLServerTestSettings overrides the value set by the copy
        }
        updatedTestSettings = updateContextRoot(updatedTestSettings);
        updatedTestSettings.updatePartnerInSettings(spName, true);
        updatedTestSettings.setIdpUserName(user);
        updatedTestSettings.setIdpUserPwd(password);
        List<validationData> expectations = setDefaultGoodSAMLLoginExpectations(flow, updatedTestSettings, cookies);
        genericSAML(_testName, webClient, updatedTestSettings, flow, expectations);

    }

    /**
     * This is a routine that will log into sp1, sp2 and sp13. It will make sure that we get access to our protected apps and that
     * we have the correct cookies:
     * WASSamlSP_sp1, WASSamlSP_sp2 and saml20_SP_sso
     *
     * @param webClient
     * @throws Exception
     */
    public void loginTo3SPs(WebClient webClient) throws Exception {

        String[] oneSPCookie = { cookieInfo.getSp1CookieName() };
        String[] twoSPCookie = { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName() };

        loginToSP(webClient, standardFlowKeepingCookies, testUsers.getUser1(), testUsers.getPassword1(), "sp1", oneSPCookie);
        String[] flow = null;
        // If we're using SP cookies or a IDP initiated Login, we need to log in for each SP, LTPA and SP (Sol/unsol) does not need to log in each time
        flow = setFlowForAnotherLogin();
        loginToSP(webClient, flow, testUsers.getUser2(), testUsers.getPassword2(), "sp2", twoSPCookie);
        loginToSP(webClient, flow, testUsers.getUser3(), testUsers.getPassword3(), "sp13", allSPCookies_list);

    }

    public String[] setFlowForAnotherLogin() throws Exception {

        if ((cookieInfo.getSPCookieType() == CookieType.SPCOOKIES) || (flowType.equals(SAMLConstants.IDP_INITIATED))) {
            return noLoginKeepingCookies;
        } else {
            return new String[] { noLoginKeepingCookies[0] };
        }

    }

    public static SAMLTestSettings updateContextRoot(SAMLTestSettings settings) throws Exception {

        if (logoutFlowType == null) {
            return settings;
        }
        Log.info(thisClass, "updateContextRoot", "CHC - logoutFlowType: " + logoutFlowType.name());
        if (logoutFlowType == LogoutFlowType.IBMSECURITYLOCAL || logoutFlowType == LogoutFlowType.IBMSECURITYREMOTE) {
            String origRoot = SAMLConstants.SAML_CLIENT_APP;
            String newRoot = SAMLConstants.SAML_CLIENT_APP + "_defaultSP";

            settings.setSpTargetApp(settings.replaceSettingIfNotNull(settings.getSpTargetApp(), origRoot, newRoot));
            settings.setSpDefaultApp(settings.replaceSettingIfNotNull(settings.getSpDefaultApp(), origRoot, newRoot));
            settings.setSpAlternateApp(settings.replaceSettingIfNotNull(settings.getSpAlternateApp(), origRoot, newRoot));
            settings.setRelayState(settings.replaceSettingIfNotNull(settings.getRelayState(), origRoot, newRoot));
        }

        return settings;

    }

    public static List<String> setMultiAppsForValidation() throws Exception {
        List<String> extraApps = new ArrayList<String>();
        extraApps.add("samlclient_sp1");
        extraApps.add("samlclient_sp2");
        extraApps.add("samlclient_sp5");
        extraApps.add("samlclient_sp13");
        extraApps.add("samlclient_sp1s2");
        extraApps.add("samlclient_defaultSP");
        extraApps.add("samlclient_sp-dash");
        extraApps.add("samlclient_sp_underscore");
        extraApps.add("samlclient_spShortLifetime");
        extraApps.add("samlclient_customLogout_emptyString");
        extraApps.add("samlclient_customLogout_invalidURL");
        extraApps.add("samlclient_customLogout_relativePath");
        extraApps.add("samlclient_customLogout_specialRelativePath");
        extraApps.add("samlclient_customLogout_invalidRelativePath");
        extraApps.add("samlclient_customLogout_absLocalURL");
        extraApps.add("samlclient_customLogout_absExternalURL");

        return extraApps;
    }

    @After
    @Override
    public void endTest() throws Exception {
        Log.info(thisClass, "endTest", "Resetting users to \"SAME\"");
        testUsers = new Testusers(UserType.SAME);
        super.endTest();
    }
}
