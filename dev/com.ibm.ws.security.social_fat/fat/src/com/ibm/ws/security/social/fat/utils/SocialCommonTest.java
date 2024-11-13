/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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

package com.ibm.ws.security.social.fat.utils;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.TestHelpers;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.fat.common.utils.MySkipRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

public class SocialCommonTest extends CommonTest {

    protected static TestHelpers helpers = new TestHelpers();
    /**
     * Provides common test tools for the Social Login FATs.
     * In general, it provides:
     * 1) rules to skip tests,
     * 2) methods to set values in settings
     * 3) methods to set expectations
     * 4) method to control test execution
     * 5) methods to perform steps/actions/panel invocation
     */

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    // adding rules in the main test class so we have access to all of the flags that are set
    // Some attributes are only ever defined in the generic config, while others are always only in the specific config
    // GenericConfig/SpecificConfig is useful to rule in/out those types of tests

    /*********** NOTE: callSpecificCheck's return TRUE to SKIP a test ***********/

    /**
     * Rult to skip test if:
     * Style of config is OIDC (test runs for oauth2Login, or oidcLogin (generic or provider specific doesn't matter))
     *
     * @author chrisc
     *
     */
    public static class skipIfOIDCStyleProvider extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            if (oidcLoginStyle) {
                Log.info(thisClass, "skipIfOIDCStyleProvider", "OIDC Style - skip test");
                testSkipped();
                return true;
            }
            Log.info(thisClass, "skipIfOIDCStyleProvider", "NOT OIDC Style - run test");
            return false;
        }
    }

    /**
     * Rult to skip test if:
     * Style of config is NOT OIDC (skip if it's oauth2Login or oauth1Login (generic or provider specific doesn't matter))
     *
     * @author chrisc
     *
     */
    public static class skipIfOAuthStyleProvider extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            if (!oidcLoginStyle) {
                Log.info(thisClass, "skipIfOAuthStyleProvider", "OAuth Style - skip test");
                testSkipped();
                return true;
            }
            Log.info(thisClass, "skipIfOAuthStyleProvider", "NOT OAuth Style - run test");
            return false;
        }
    }

    /**
     * Rult to skip test if:
     * Type of config is Provider specific
     *
     * @author chrisc
     *
     */
    public static class skipIfProviderConfig extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            if (configType.equals(ProviderConfig)) {
                Log.info(thisClass, "skipIfProviderConfig", "Provider Config - skip test");
                testSkipped();
                return true;
            }
            Log.info(thisClass, "skipIfProviderConfig", "NOT Provider Config - run test");
            return false;
        }
    }

    /**
     * Rult to skip test if:
     * Type of config is Generic
     *
     * @author chrisc
     *
     */
    public static class skipIfGenericConfig extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            if (configType.equals(GenericConfig)) {
                Log.info(thisClass, "skipIfGenericConfig", "Generic Config - skip test");
                testSkipped();
                return true;
            }
            Log.info(thisClass, "skipIfGenericConfig", "NOT Generic Config - run test");
            return false;
        }
    }

    /**
     * Rult to skip test if:
     * Provider is NOT Liberty OP
     *
     * @author chrisc
     *
     */
    public static class skipIfNotLibertyOP extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
                Log.info(thisClass, "skipIfNotLibertyOP", "Liberty OP Config - run test");
                return false;
            }
            Log.info(thisClass, "skipIfNotLibertyOP", "NOT Liberty OP Config - skip test");
            testSkipped();
            return true;
        }
    }

    /**
     * Rule to skip test if:
     * Provider is Twitter
     *
     * @author chrisc
     *
     */
    public static class skipIfTwitter extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
                Log.info(thisClass, "skipIfTwitter", "Twitter Config - skip tests");
                testSkipped();
                return true;
            }
            Log.info(thisClass, "skipIfTwitter", "NOT Twitter Config - run test");
            return false;
        }
    }

    // Trying to have conditional rules to skip types of tests with more generic logic
    // I don't want methods that say skipIfFacebookGitHub...LinkedIn
    /**
     * rule to skip test if:
     * OAuth Style or Provider specific config
     *
     * not used at the moment
     *
     * @author chrisc
     *
     */
    public static class notOIDCStyleProviderAndGenericConfig extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            if (oidcLoginStyle && configType.equals(GenericConfig)) {
                Log.info(thisClass, "notOIDCStyleProviderAndGenericConfig", "OIDC Style and Generic Config - run test");
                return false;
            }
            Log.info(thisClass, "notOIDCStyleProviderAndGenericConfig", "NOT OIDC Style or Generic Config - skip test");
            testSkipped();
            return true;
        }
    }

    /**
     * rule to skip test if:
     * OIDC Style or Provider specific config
     *
     * not used at the moment
     *
     * @author chrisc
     *
     */
    public static class skipIfOIDCStyleProviderOrProviderConfig extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            if (oauth2LoginStyle && configType.equals(GenericConfig)) {
                Log.info(thisClass, "skipIfOIDCStyleProviderOrProviderConfig", "OAuth Style and Generic Config - run test");
                return false;
            }
            Log.info(thisClass, "skipIfOIDCStyleProviderOrProviderConfig", "NOT OAuth Style and Generic Config - skip test");
            testSkipped();
            return true;
        }
    }

    /**
     * Rule to skip test if:
     * OAuth Style or Generic config
     *
     * @author chrisc
     *
     */
    public static class notOIDCStyleProviderAndProviderConfig extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            if (oidcLoginStyle && configType.equals(ProviderConfig)) {
                Log.info(thisClass, "notOIDCStyleProviderAndProviderConfig", "OIDC Style and Provider Config - run test");
                return false;
            }
            Log.info(thisClass, "notOIDCStyleProviderAndProviderConfig", "NOT OIDC Style and Provider Config - skip test");
            testSkipped();
            return true;
        }
    }

    /**
     * Rule to skip test if:
     * Not - Generic Social config or OIDC provider config (which means only Google provider specific config at the moment)
     *
     * @author chrisc
     *
     */
    public static class skipIfNonOIDCProviderConfig extends MySkipRule {

        @Override
        public Boolean callSpecificCheck() {
            // oidc/oauth generic configs and google support Attribute ALL config attributes
            // userNameAttribute is also supported for all providers and config types
            // skip test for groupNameAttribute, realmNameAttribute and userUniqueIdAttribute for facebook, twitter, github, and linked-in if we're using a provider specific config.
            // the config filename contains the attribute name
            if (configType.equals(ProviderConfig) && (!provider.equals(SocialConstants.GOOGLE_PROVIDER))) {
                Log.info(thisClass, "skipIfNonOIDCProviderConfig", "Using non-OIDC Provider Config - skip test");
                testSkipped();
                return true;
            }
            Log.info(thisClass, "skipIfNonOIDCProviderConfig", "Generic Social config or OIDC provider config - run test");
            return false;

        }
    }

    /**
     * Rule to skip test if:
     * Not - Env var says that we should run the test
     *
     * @author chrisc
     *
     */
    public static class skipIfExternalProvidersShouldntRun extends MySkipRule {

        @Override
        public Boolean callSpecificCheck() {
            try {
                if (shouldExternalProviderTestsRun()) {
                    return false;
                } else {
                    testSkipped();
                    return true;
                }
            } catch (Exception e) {
                // if something goes wrong determinig IF the tests should run, don't try running the tests
                testSkipped();
                return true;
            }
        }
    }

    public static class skipIfExternalProvidersShouldntRumORGenericConfig extends MySkipRule {

        @Override
        public Boolean callSpecificCheck() {
            try {
                if (shouldExternalProviderTestsRun()) {
                    if (configType.equals(GenericConfig)) {
                        Log.info(thisClass, "skipIfExternalProvidersShouldntRumORGenericConfig", "Provider test is allowed to run, but it's a Generic Config - skip test");
                        testSkipped();
                        return true;
                    }
                    Log.info(thisClass, "skipIfExternalProvidersShouldntRumORGenericConfig", "Provider test is allowed to run and it's NOT a Generic Config - run test");
                    return false;

                } else {
                    Log.info(thisClass, "skipIfExternalProvidersShouldntRumORGenericConfig", "Provider test is NOT allowed to run - skip test");
                    testSkipped();
                    return true;
                }
            } catch (

            Exception e) {
                // if something goes wrong determinig IF the tests should run, don't try running the tests
                Log.info(thisClass, "skipIfExternalProvidersShouldntRumORGenericConfig", "A check failed assume Provider test is NOT allowed to run - skip test");
                testSkipped();
                return true;
            }
        }
    }

    /**
     * Rule to skip test if:
     * Provider does not work with inbound propagation
     *
     * @author chrisc
     *
     */
    public static class skipIfInboundPropagationNotSupported extends MySkipRule {

        @Override
        public Boolean callSpecificCheck() {
            try {
                if (provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
                    return false;
                } else {
                    testSkipped();
                    return true;
                }
            } catch (Exception e) {
                // if something goes wrong determinig IF the tests should run, don't try running the tests
                testSkipped();
                return true;
            }
        }
    }

    public static Class<?> thisClass = SocialCommonTest.class;
    public static SocialTestSettings socialSettings = null;

    // don't bother initializing as each test class must set the value it needs because of the way we extend the class
    private static Boolean useProviderSpecificConfig;
    public static String configType = null;
    public final static String GenericConfig = "genericConfig";
    public final static String ProviderConfig = "providerConfig";

    protected static String[] invoke_social_login_actions = null;
    protected static String[] invoke_social_just_login_actions = null;
    protected static String provider = null;
    protected static String perform_social_login = null;
    protected static String perform_social_authorize = null;
    public static String providerConfigString = null;
    public static Boolean providerDisplaysLoginOnError = true;
    public static Boolean addJWTTokenValidation = true;
    public static Boolean doNotAddJWTTokenValidation = false;
    public static Boolean oauth2LoginStyle = true;
    public static Boolean oidcLoginStyle = false;
    public static Boolean oauth1LoginStyle = false;
    public static Boolean usesSelectionPanel = false;
    public final static Boolean UseSelectionPanel = true;
    public final static Boolean DoesNotUseSelectionPanel = false;

    protected static boolean defaultUseLdap = useLdap;

    @BeforeClass
    public static void beforeClass() {
        useLdap = false;
        Log.info(thisClass, "beforeClass", "Set useLdap to: " + useLdap);
    }

    @AfterClass
    public static void afterClass() {
        useLdap = defaultUseLdap;
        Log.info(thisClass, "afterClass", "Resetting useLdap to: " + useLdap);
    }

    /**
     * Invokes {@link #setActionsForProvider(String requestedProvider, String style, Boolean usesSelection)} - assumes that style
     * is not needed (request will be for a provider other than LibertyOP) and usesSelection is false
     *
     * @param requestedProvider
     *            - the provider that we'll be using
     */
    public static void setActionsForProvider(String requestedProvider) {
        setActionsForProvider(requestedProvider, null, false);
    }

    /**
     * Invokes {@link #setActionsForProvider(String requestedProvider, String style, Boolean usesSelection)} - assumes that
     * usesSelection is false
     *
     * @param requestedProvider
     *            - the provider that we'll be using
     * @param style
     *            - the sytle of the provider (oauth2, oidc, ) - mainly used to indicate oauth vs oidc for the Liberty OP
     */
    public static void setActionsForProvider(String requestedProvider, String style) {
        setActionsForProvider(requestedProvider, style, false);
    }

    /**
     * Invokes {@link #setActionsForProvider(String requestedProvider, String style, Boolean usesSelection)} - assumes that style
     * is not needed (request will be for a provider other than LibertyOP)
     *
     * @param requestedProvider
     *            - the provider that we'll be using
     * @param usesSelection
     *            - indicate if test will use the selection panel - the action list has an extra step when we will get the
     *            selection panel
     */
    public static void setActionsForProvider(String requestedProvider, Boolean usesSelection) {
        setActionsForProvider(requestedProvider, null, usesSelection);
    }

    /**
     * Sets process wide action values based on the provider and style that the current test class, or test case are using. The
     * tests and tooling will use the SocialTestSettings to perform different tasks, pass different values, ... based on the
     * SocialTestSettings. Set those settings as appropriate for the provider that we'll be using.
     *
     * @param requestedProvider
     *            - the provider that we'll be using
     * @param style
     *            - the sytle of the provider (oauth2, oidc, ) - mainly used to indicate oauth vs oidc for the Liberty OP
     * @param usesSelection
     *            - indicate if test will use the selection panel - the action list has an extra step when we will get the
     *            selection panel
     */
    public static void setActionsForProvider(String requestedProvider, String style, Boolean usesSelection) {

        usesSelectionPanel = usesSelection;

        if (requestedProvider.equals(SocialConstants.FACEBOOK_PROVIDER)) {
            if (usesSelection) {
                invoke_social_login_actions = SocialConstants.FACEBOOK_INVOKE_SOCIAL_LOGIN_WITH_SELECTION_ACTIONS;
            } else {
                invoke_social_login_actions = SocialConstants.FACEBOOK_INVOKE_SOCIAL_LOGIN_ACTIONS;
            }
            invoke_social_just_login_actions = SocialConstants.FACEBOOK_INVOKE_SOCIAL_JUST_LOGIN_ACTIONS;
            perform_social_login = SocialConstants.FACEBOOK_PERFORM_SOCIAL_LOGIN;
            perform_social_authorize = null;
            provider = SocialConstants.FACEBOOK_PROVIDER;
            providerDisplaysLoginOnError = false;
            oauth2LoginStyle = true;
            oidcLoginStyle = false;
            oauth1LoginStyle = false;
        }
        if (requestedProvider.equals(SocialConstants.GITHUB_PROVIDER)) {
            if (usesSelection) {
                invoke_social_login_actions = SocialConstants.GITHUB_INVOKE_SOCIAL_LOGIN_WITH_SELECTION_ACTIONS;
            } else {
                invoke_social_login_actions = SocialConstants.GITHUB_INVOKE_SOCIAL_LOGIN_ACTIONS;
            }
            invoke_social_just_login_actions = SocialConstants.GITHUB_INVOKE_SOCIAL_JUST_LOGIN_ACTIONS;
            perform_social_login = SocialConstants.GITHUB_PERFORM_SOCIAL_LOGIN;
            perform_social_authorize = SocialConstants.TWITTER_AUTHORIZE_BUTTON_NAME;
            provider = SocialConstants.GITHUB_PROVIDER;
            providerDisplaysLoginOnError = true;
            oauth2LoginStyle = true;
            oidcLoginStyle = false;
            oauth1LoginStyle = false;
        }
        if (requestedProvider.equals(SocialConstants.TWITTER_PROVIDER)) {
            if (usesSelection) {
                invoke_social_login_actions = SocialConstants.TWITTER_INVOKE_SOCIAL_LOGIN_WITH_SELECTION_ACTIONS;
            } else {
                invoke_social_login_actions = SocialConstants.TWITTER_INVOKE_SOCIAL_LOGIN_ACTIONS;
            }
            invoke_social_just_login_actions = SocialConstants.TWITTER_INVOKE_SOCIAL_JUST_LOGIN_ACTIONS;
            // the typical login page for twitter is the sign in page - you'll get the log in page if/when you
            // enter an invalid password and have to try again (the second and subsequent attempts will use login)
            perform_social_login = SocialConstants.TWITTER_PERFORM_SIGN_IN;
            perform_social_authorize = null;
            provider = SocialConstants.TWITTER_PROVIDER;
            providerDisplaysLoginOnError = false;
            oauth2LoginStyle = false;
            oidcLoginStyle = false;
            oauth1LoginStyle = true;
        }
        if (requestedProvider.equals(SocialConstants.LINKEDIN_PROVIDER)) {
            if (usesSelection) {
                invoke_social_login_actions = SocialConstants.LINKEDIN_INVOKE_SOCIAL_LOGIN_WITH_SELECTION_ACTIONS;
            } else {
                invoke_social_login_actions = SocialConstants.LINKEDIN_INVOKE_SOCIAL_LOGIN_ACTIONS;
            }
            invoke_social_just_login_actions = SocialConstants.LINKEDIN_INVOKE_SOCIAL_JUST_LOGIN_ACTIONS;
            // the typical login page for twitter is the sign in page - you'll get the log in page if/when you
            // enter an invalid password and have to try again (the second and subsequent attempts will use login)
            perform_social_login = SocialConstants.LINKEDIN_PERFORM_SOCIAL_LOGIN;
            perform_social_authorize = null;
            provider = SocialConstants.LINKEDIN_PROVIDER;
            providerDisplaysLoginOnError = false;
            oauth2LoginStyle = true;
            oidcLoginStyle = false;
            oauth1LoginStyle = false;
        }
        if (requestedProvider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
            if (usesSelection) {
                invoke_social_login_actions = SocialConstants.LIBERTYOP_INVOKE_SOCIAL_LOGIN_WITH_SELECTION_ACTIONS;
            } else {
                invoke_social_login_actions = SocialConstants.LIBERTYOP_INVOKE_SOCIAL_LOGIN_ACTIONS;
            }
            invoke_social_just_login_actions = SocialConstants.LIBERTYOP_INVOKE_SOCIAL_JUST_LOGIN_ACTIONS;
            perform_social_login = SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN;
            perform_social_authorize = null;
            provider = SocialConstants.LIBERTYOP_PROVIDER;
            providerDisplaysLoginOnError = false;
            oauth2LoginStyle = false;
            oidcLoginStyle = false;
            oauth1LoginStyle = false;
            // we can run Liberty OP tests with oidc or oauth, need to indicate which type we're running with.
            if (style == null) {
                // assume oidc
                oidcLoginStyle = true;
            } else {
                if (style.equals(SocialConstants.OAUTH_OP)) {
                    oauth2LoginStyle = true;
                } else {
                    oidcLoginStyle = true;
                }
            }
        }
        if (requestedProvider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
            if (usesSelection) {
                invoke_social_login_actions = SocialConstants.OPENSHIFT_INVOKE_SOCIAL_LOGIN_WITH_SELECTION_ACTIONS;
            } else {
                invoke_social_login_actions = SocialConstants.OPENSHIFT_INVOKE_SOCIAL_LOGIN_ACTIONS;
            }
            invoke_social_just_login_actions = SocialConstants.OPENSHIFT_INVOKE_SOCIAL_JUST_LOGIN_ACTIONS;
            perform_social_login = SocialConstants.OPENSHIFT_PERFORM_SOCIAL_LOGIN;
            perform_social_authorize = null;
            provider = SocialConstants.OPENSHIFT_PROVIDER;
            providerDisplaysLoginOnError = false;
            oauth2LoginStyle = true;
            oidcLoginStyle = false;
            oauth1LoginStyle = false;
        }
    }

    /**
     * Set the useProviderSpecificConfig flag to true
     */
    public static void reconfigServerForSpecificProvider() {
        useProviderSpecificConfig = true;
    }

    /**
     * Set the useProviderSpecificConfig flag to false
     */
    public static void doNotReconfigServerForGenericProvider() {
        useProviderSpecificConfig = false;
    }

    /**
     * Invokes {@link #setGenericVSSpeicificProviderFlags(String type, String serverXmlPrefix) } - making the assumption that the
     * serverXmlPrevix is null/not needed
     *
     * @param type
     *            - the type of configs that the test will use - Generic (oauth2login, oidcLogin) vs Specific (facebookLogin,
     *            twitterLogin, ...)
     */
    public static void setGenericVSSpeicificProviderFlags(String type) {
        setGenericVSSpeicificProviderFlags(type, null);
    }

    /**
     * Sets process wide flags to indicate if the configs are generic or provider specific, also sets the base name of the
     * provider specific config file names (the names will vary by provider - we set a test class wide root and then the tests
     * just need to know a partial name specific to themselves)
     *
     * @param type
     *            - the type of configs that the test will use - Generic (oauth2login, oidcLogin) vs Specific (facebookLogin,
     *            twitterLogin, ...)
     * @param serverXmlPrefix
     *            - the prefix string for server xml names (ie: server config's
     *            server_facebook_basicConfigTests_usingFacebookConfig_goodTrust.xml serverXmlPrevix is:
     *            "server_facebook_basicConfigTests_usingFacebookConfig" - all config files start with and the test case just
     *            append "_goodTrust.xml", "_badTrust.xml", "_badClientSecret.xml", ...)
     */
    public static void setGenericVSSpeicificProviderFlags(String type, String serverXmlPrefix) {

        if (type == null || type.equals(GenericConfig)) {
            doNotReconfigServerForGenericProvider();
            configType = GenericConfig;
        } else {
            reconfigServerForSpecificProvider();
            configType = ProviderConfig;
        }
        providerConfigString = serverXmlPrefix;
        System.out.println("configType: " + configType);
    }

    /**
     * Perform a reconfig if we're using provider specific configs. Test cases use this method instead of having logic to check
     * the type of config and if they need to perform the reconfig. With generic configs, we can have multiple configs in one
     * srver.xml, with provider specific, we can only have one at a time, and therefore have to reconfig. The common tests run
     * with both types of configs and use this method to reconfig IF they need to.
     *
     * @param theServer
     *            - the server instance to reconfigure
     * @param newConfig
     *            - the file name and location to reconfigure to
     * @param addMsgs
     *            - additional messages to search for when determining if server reconfig is complete and is successful
     * @return - returns a true/false flag indicating if the reconfig was needed
     * @throws Exception
     */
    public Boolean reconfigIfProviderSpecificConfig(TestServer theServer, String newConfig, List<String> addMsgs) throws Exception {

        if (useProviderSpecificConfig) {
            theServer.reconfigServer(newConfig, _testName, true, addMsgs);
            return true;
        }
        return false;
    }

    /**
     * Invokes {@link #genericSocial(String testcase, WebClient webClient, Object somePage, String[] testActions,
     * SocialTestSettings settings, List<validationData> expectations)} - making the
     * assumption that the previous page was null
     *
     * @param testcase
     *            - the current testcase name (may be helpful for logging)
     * @param webClient
     *            - the current web client - method creates a new webClient if one is NOT passed in
     * @param testActions
     *            - the list of actions that should be performed during this invocation
     * @param settings
     *            - the current test settings - called methods will use this to get info needed to obtain and invoke their panels
     * @param expectations
     *            - the test expectations - will pass this to each of the called methods - they will in turn pass it to the
     *            validation code after their panel is invoked - the validation code will use the expectations to validate the
     *            correct response/behavior
     * @return - returns the last page (WebResponse from the last step)
     * @throws Exception
     */
    public Object genericSocial(String testcase, WebClient webClient, String[] testActions, SocialTestSettings settings, List<validationData> expectations) throws Exception {
        return genericSocial(testcase, webClient, null, testActions, settings, expectations);
    }

    /**
     * This method invokes the methods that perform the "test steps". Each test needs to invoke a series of HTMLUnit panels. This
     * method invokes those panels in the proper order. It does this based on the testActions list that is passed in.
     *
     * @param testcase
     *            - the current testcase name (may be helpful for logging)
     * @param webClient
     *            - the current web client - method creates a new webClient if one is NOT passed in
     * @param somePage
     *            - Typically null - if the flow is step 1 - n, we do not start out having a page. If you need to re-invoke a
     *            page, or invoke a page after updating the test settings, you would need to pass the previous page in as a
     *            starting point for the next step.
     * @param testActions
     *            - the list of actions that should be performed during this invocation
     * @param settings
     *            - the current test settings - called methods will use this to get info needed to obtain and invoke their panels
     * @param expectations
     *            - the test expectations - will pass this to each of the called methods - they will in turn pass it to the
     *            validation code after their panel is invoked - the validation code will use the expectations to validate the
     *            correct response/behavior
     * @return - returns the last page (WebResponse from the last step)
     * @throws Exception
     */
    public Object genericSocial(String testcase, WebClient webClient, Object somePage, String[] testActions, SocialTestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "genericSocial";
        msgUtils.printMethodName(thisMethod);

        if (webClient == null) {
            webClient = getAndSaveWebClient();
        }

        // reduce logging
        settings.printSocialTestSettings();
        msgUtils.printOAuthOidcExpectations(expectations, testActions, settings);

        try {

            for (String entry : testActions) {
                Log.info(thisClass, testcase, "Action to be performed: " + entry);
            }

            if (validationTools.isInList(testActions, SocialConstants.INVOKE_SOCIAL_RESOURCE)) {
                somePage = invokeSocialResource(testcase, webClient, settings, expectations);
            }

            if (validationTools.isInList(testActions, SocialConstants.SELECT_PROVIDER)) {
                somePage = performSelectProvider(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            }

            if (validationTools.isInList(testActions, SocialConstants.PERFORM_CREDENTIAL_LOGIN)) {
                somePage = performCredentialLogin(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            }

            //            if (validationTools.isInList(testActions, SocialConstants.PERFORM_SOCIAL_LOGIN_FAIL)) {
            //                anHtmlPage = (HtmlPage) performSocialLogin(testcase, webClient, anHtmlPage, settings, expectations, SocialConstants.PERFORM_SOCIAL_LOGIN_FAIL);
            //            }
            // The login returns a TextPage if the request works and an HtmlPage if the login fails :(

            if (validationTools.isInList(testActions, SocialConstants.LINKEDIN_PERFORM_SIGN_IN)) {
                somePage = performLinkedinSignin(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            }
            if (validationTools.isInList(testActions, SocialConstants.PERFORM_SOCIAL_LOGIN)) {
                somePage = performGenericSocialLogin(testcase, webClient, (HtmlPage) somePage, perform_social_login, settings, expectations);
            }
            if (validationTools.isInList(testActions, SocialConstants.GITHUB_PERFORM_SOCIAL_LOGIN)) {
                somePage = performSocialLogin_GitHub(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            }
            if (validationTools.isInList(testActions, SocialConstants.TWITTER_PERFORM_SIGN_IN)) {
                SocialTestSettings updatedSocialTestSettings = settings.copyTestSettings();
                updatedSocialTestSettings.setLoginButton(SocialConstants.TWITTER_SIGN_IN_BUTTON_NAME);
                updatedSocialTestSettings.setLoginTitle(SocialConstants.TWITTER_LOGIN_TITLE);
                somePage = performGenericSocialLogin(testcase, webClient, (HtmlPage) somePage, SocialConstants.TWITTER_PERFORM_SIGN_IN, updatedSocialTestSettings, expectations);
            }
            if (validationTools.isInList(testActions, SocialConstants.TWITTER_PERFORM_AUTHORIZE)) {
                somePage = performAuthorize(testcase, webClient, (HtmlPage) somePage, SocialConstants.TWITTER_PERFORM_AUTHORIZE, settings, expectations);
            }
            if (validationTools.isInList(testActions, SocialConstants.TWITTER_PERFORM_LOGIN)) {
                SocialTestSettings updatedSocialTestSettings = settings.copyTestSettings();
                updatedSocialTestSettings.setLoginButton(SocialConstants.TWITTER_LOGIN_BUTTON_NAME);
                updatedSocialTestSettings.setLoginTitle(SocialConstants.TWITTER_LOGIN_TITLE);
                somePage = performGenericSocialLogin(testcase, webClient, (HtmlPage) somePage, SocialConstants.TWITTER_PERFORM_LOGIN, updatedSocialTestSettings, expectations);
            }
            if (validationTools.isInList(testActions, SocialConstants.TWITTER_PERFORM_LOGIN_AND_AUTHORIZE)) {
                SocialTestSettings updatedSocialTestSettings = settings.copyTestSettings();
                updatedSocialTestSettings.setLoginButton(SocialConstants.TWITTER_LOGIN_BUTTON_NAME);
                updatedSocialTestSettings.setLoginTitle(SocialConstants.TWITTER_LOGIN_AND_AUTHORIZE_TITLE);
                somePage = performGenericSocialLogin(testcase, webClient, (HtmlPage) somePage, SocialConstants.TWITTER_PERFORM_LOGIN_AND_AUTHORIZE, updatedSocialTestSettings, expectations);
            }
            if (validationTools.isInList(testActions, SocialConstants.PERFORM_SOCIAL_IMPLICIT_LOGIN)) {
                somePage = performGenericSocialLoginImplicit(testcase, webClient, (HtmlPage) somePage, SocialConstants.PERFORM_SOCIAL_IMPLICIT_LOGIN, settings, expectations);
            }

            if (validationTools.isInList(testActions, SocialConstants.OPENSHIFT_PERFORM_SOCIAL_LOGIN)) {
                somePage = performSocialLogin_OpenShift(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            }

            //                return response;

        } catch (Exception e) {

            Log.error(thisClass, testcase, e, "Exception occurred");
            System.err.println("Exception: " + e);
            throw e;
        }
        return somePage;
    }

    /**
     * Performs the HTMLUnit steps necessary to invoke the protected resource. The method builds a new new request using
     * information from the test settings and then invokes the protected app
     *
     * @param testcase
     *            - the current testcase name (may be helpful for logging)
     * @param webClient
     *            - the current web client - method uses this to invoke the protected app
     * @param settings
     *            - the current test settings - method will use this to get info needed to invoke the panel
     * @param expectations
     *            - the test expectations - will pass this to the validation code after the panel is invoked - that code will use
     *            the expectations to validate the correct response/behavior
     * @return - returns the response from invoking the protected app
     * @throws Exception
     */
    public static Object invokeSocialResource(String testcase, WebClient webClient, SocialTestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeSocialResource";
        msgUtils.printMethodName(thisMethod);
        Object thePage = null;

        try {
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setUseInsecureSSL(true);
            webClient.getOptions().setRedirectEnabled(true);

            Log.info(thisClass, thisMethod, "Trying to invoke: " + settings.getProtectedResource());
            if (settings.headerName != null && settings.getHeaderValue() != null) {
                Log.info(thisClass, thisMethod, "HeaderName: " + settings.headerName + " HeaderValue: " + settings.headerValue);
                WebRequest req = new WebRequest(new URI(settings.getProtectedResource()).toURL());
                req.setAdditionalHeader(settings.headerName, settings.headerValue);
                Log.info(thisClass, thisMethod, "Request: " + req.toString());
                thePage = webClient.getPage(req);
            } else {
                Log.info(thisClass, thisMethod, "Request: " + settings.getProtectedResource());
                thePage = webClient.getPage(settings.getProtectedResource());
            }
            // make sure the page is processed before continuing
            helpers.waitBeforeContinuing(webClient);

        } catch (Exception e) {
            handleException(testcase, thisMethod, SocialConstants.INVOKE_SOCIAL_RESOURCE, e, expectations);
        }

        if (AutomationTools.getResponseIsHtml(thePage)) {
            String title = AutomationTools.getResponseTitle(thePage);
            if (title != null && title.contains("OKD") || title.contains("OpenShift")) {
                Log.info(thisClass, thisMethod, "Skip logging the OpenShift login page content - it's way too long");
            } else {
                msgUtils.printResponseParts(thePage, thisMethod, "Response from " + thisMethod + ": ");
            }
        } else {
            msgUtils.printResponseParts(thePage, thisMethod, "Response from " + thisMethod + ": ");
        }
        msgUtils.printAllCookies(webClient);
        validationTools.validateResult(thePage, SocialConstants.INVOKE_SOCIAL_RESOURCE, expectations, settings);
        return thePage;
    }

    //    /**
    //     * Performs the HTMLUnit steps necessary to invoke the protected resource for a Proxy type flow. The method builds a new new request using
    //     * information from the test settings and then invokes the protected app
    //     *
    //     * @param testcase
    //     *            - the current testcase name (may be helpful for logging)
    //     * @param webClient
    //     *            - the current web client - method uses this to invoke the protected app
    //     * @param settings
    //     *            - the current test settings - method will use this to get info needed to invoke the panel
    //     *            @param accessToken - the accessToken to pass
    //     * @param expectations
    //     *            - the test expectations - will pass this to the validation code after the panel is invoked - that code will use
    //     *            the expectations to validate the correct response/behavior
    //     * @return - returns the response from invoking the protected app
    //     * @throws Exception
    //     */
    //    public static Object invokeSocialResource(String testcase, WebClient webClient, SocialTestSettings settings, String accessToken, List<validationData> expectations) throws Exception {
    //
    //        String thisMethod = "invokeSocialResource";
    //        msgUtils.printMethodName(thisMethod);
    //        Object thePage = null;
    //        try {
    //            webClient.getOptions().setJavaScriptEnabled(true);
    //            webClient.getOptions().setUseInsecureSSL(true);
    //            webClient.getOptions().setRedirectEnabled(true);
    //
    //            Log.info(thisClass, thisMethod, "Trying to invoke: " + settings.getProtectedResource());
    //            if (settings.headerName != null && settings.getHeaderValue() != null) {
    //                Log.info(thisClass, thisMethod, "Trying to invoke: " + settings.getProtectedResource());
    //                WebRequest req = new WebRequest(new URI(settings.getProtectedResource()).toURL());
    //                req.setAdditionalHeader(settings.headerName, settings.headerValue);
    //                thePage = webClient.getPage(req);
    //            } else {
    //                if (settings.headerName != null && settings.getHeaderValue() != null) {
    //                    WebRequest req = new WebRequest(new URI(settings.getProtectedResource()).toURL());
    //                    req.setAdditionalHeader(settings.headerName, settings.headerValue);
    //                    thePage = webClient.getPage(req);
    //                } else {
    //                    thePage = webClient.getPage(settings.getProtectedResource());
    //                }
    //            }
    //        } catch (Exception e) {
    //            handleException(testcase, thisMethod, SocialConstants.INVOKE_SOCIAL_RESOURCE, e, expectations);
    //        }
    //
    //        msgUtils.printResponseParts(thePage, thisMethod, "Response from " + thisMethod + ": ");
    //        validationTools.validateResult(thePage, SocialConstants.INVOKE_SOCIAL_RESOURCE, expectations, settings);
    //        return thePage;
    //    }

    /**
     * Performs the steps needed to fill in user credentials on the provided web page. The method will find the appropriate HTML
     * form within the web page, input the user credentials specified in the settings, and submit the form.
     *
     * @param testcase
     *            - the current testcase name (may be helpful for logging)
     * @param webClient
     *            - the current web client - method uses this to invoke the Login panel
     * @param startPage
     *            - the last returned page - method will get the login panel out of this
     * @param settings
     *            - the current test settings - method will use this to get info needed to invoke the panel
     * @param expectations
     *            - the test expectations - will pass this to the validation code after the panel is invoked - that code will use
     *            the expectations to validate the correct response/behavior
     * @return - returns the response from invoking the Login panel
     * @throws Exception
     */
    public static Object performCredentialLogin(String testcase, WebClient webClient, HtmlPage startPage, SocialTestSettings settings, List<validationData> expectations) throws Exception {
        String thisMethod = "performCredentialLogin";
        String currentAction = SocialConstants.PERFORM_CREDENTIAL_LOGIN;

        msgUtils.printMethodName(thisMethod + "|" + currentAction);
        Log.info(thisClass, thisMethod + "|" + currentAction, "currentAction: " + currentAction);

        Object thePage = null;
        try {
            List<HtmlForm> forms = startPage.getForms();

            // Verify that the appropriate number of forms are present
            validateSelectionPageWithCredentialForm(currentAction, forms);

            // The credentials form should be the second form on the page
            final HtmlForm form = forms.get(1);

            // Verify that all of the expected elements in this form are present
            validateCredentialFormStructure(form);

            thePage = fillAndSubmitCredentialForm(form, currentAction, settings);

            msgUtils.printResponseParts(thePage, thisMethod + "|" + currentAction, "Response from " + thisMethod + "|" + currentAction + ": ");

        } catch (Exception e) {
            handleException(testcase, thisMethod, currentAction, e, expectations);
        }

        validationTools.validateResult(thePage, currentAction, expectations, settings);

        logPageClass(thisMethod, currentAction, thePage);
        return thePage;
    }

    /**
     * Verifies that there are at least two forms provided on the current page. Expects the current page to be the default
     * selection page with local authentication enabled, where the social media providers are provided in one form and user
     * credentials are input in another form.
     */
    static void validateSelectionPageWithCredentialForm(String currentAction, List<HtmlForm> forms) throws Exception {
        if (forms == null || forms.isEmpty()) {
            throw new Exception("There were no forms found in the page returned during action [" + currentAction + "]. We most likely didn't reach the social media selection page. Check the page content to ensure we arrived at the expected web page.");
        }
        if (forms.size() < 2) {
            HtmlForm onlyForm = forms.get(0);
            throw new Exception("Only one form (action=\"" + onlyForm.getActionAttribute() + "\") was found on the page during action [" + currentAction + "], but at least two are expected. We either didn't reach the social media selection page or the page does not contain the information we expected.");
        }
    }

    /**
     * Verifies that the form is configured with the correct action and that it contains username and password inputs as well as
     * a submit button.
     */
    static void validateCredentialFormStructure(HtmlForm credentialForm) throws Exception {
        String formAction = credentialForm.getActionAttribute();
        if (formAction == null || !formAction.equals(SocialConstants.J_SECURITY_CHECK)) {
            throw new Exception("The action attribute [" + formAction + "] of the form to use was either null or was not \"" + SocialConstants.J_SECURITY_CHECK + "\" as expected. Check the page contents to ensure we reached the correct page.");
        }

        HtmlInput usernameInput = credentialForm.getInputByName(SocialConstants.OIDC_USERPARM);
        if (usernameInput == null) {
            throw new Exception("Did not find the username input in the provided form.");
        }

        HtmlInput passwordInput = credentialForm.getInputByName(SocialConstants.OIDC_PASSPARM);
        if (passwordInput == null) {
            throw new Exception("Did not find the password input in the provided form.");
        }

        HtmlInput submitButton = credentialForm.getInputByValue(SocialConstants.DEFAULT_CREDENTIAL_SUBMIT_BTN_VAL);
        if (submitButton == null) {
            throw new Exception("Did not find the appropriate submit button in the provided form.");
        }
    }

    static Object fillAndSubmitCredentialForm(HtmlForm form, String currentAction, SocialTestSettings settings) throws IOException {
        String thisMethod = "fillAndSubmitCredentialForm";

        getAndSetUsernameField(form, currentAction, settings);
        getAndSetPasswordField(form, currentAction, settings);

        HtmlInput submitButton = form.getInputByValue(SocialConstants.DEFAULT_CREDENTIAL_SUBMIT_BTN_VAL);

        Log.info(thisClass, thisMethod + "|" + currentAction, "Pressing the " + submitButton + " button");

        return submitButton.click();
    }

    static void getAndSetUsernameField(HtmlForm form, String currentAction, SocialTestSettings settings) {
        String thisMethod = "getAndSetUsernameField";

        HtmlInput usernameInput = form.getInputByName(SocialConstants.OIDC_USERPARM);
        Log.info(thisClass, thisMethod + "|" + currentAction, "username field is: " + usernameInput);

        Log.info(thisClass, thisMethod + "|" + currentAction, "Setting: " + usernameInput + " to: " + settings.getUserName());
        usernameInput.setValueAttribute(settings.getUserName());
    }

    static void getAndSetPasswordField(HtmlForm form, String currentAction, SocialTestSettings settings) {
        String thisMethod = "getAndSetPasswordField";

        HtmlInput passwordInput = form.getInputByName(SocialConstants.OIDC_PASSPARM);
        Log.info(thisClass, thisMethod + "|" + currentAction, "password field is: " + passwordInput);

        Log.info(thisClass, thisMethod + "|" + currentAction, "Setting: " + passwordInput + " to: " + settings.getUserPassword());
        passwordInput.setValueAttribute(settings.getUserPassword());
    }

    /**
     * Performs the HTMLUnit steps necessary to process the Linkedin Login panel.
     *
     * @param testcase
     *            - the current testcase name (may be helpful for logging)
     * @param webClient
     *            - the current web client - method uses this to invoke the Linkedin Login panel
     * @param startPage
     *            - the last returned page - method will get the Linkedin Login panel out of this
     * @param settings
     *            - the current test settings - method will use this to get info needed to invoke the panel
     * @param expectations
     *            - the test expectations - will pass this to the validation code after the panel is invoked - that code will use
     *            the expectations to validate the correct response/behavior
     * @return - returns the response from invoking the Linkedin Login panel
     * @throws Exception
     */
    public static Object performLinkedinSignin(String testcase, WebClient webClient, HtmlPage startPage, SocialTestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "performLinkedinSignin";
        msgUtils.printMethodName(thisMethod);
        Object thePage = null;
        try {
            //            final HtmlForm form = startPage.getForms().get(0);
            webClient.getOptions().setJavaScriptEnabled(false);

            Log.info(thisClass, thisMethod, "Raw page is: " + startPage.asXml());
            //            List<HtmlAnchor> signInLinks = (List<HtmlAnchor>) startPage.getByXPath("//[@class='signin-link'])");
            List<HtmlAnchor> signInLinks = startPage.getByXPath("/html/body/div/div/div/div/div[2]/div[1]/div/div/p/a");
            HtmlAnchor signInPage = signInLinks.get(0);
            Log.info(thisClass, thisMethod, "Sign-in page link is: " + signInPage);
            thePage = signInPage.click();

            msgUtils.printResponseParts(thePage, thisMethod, "Response from " + thisMethod + " : ");

        } catch (Exception e) {
            handleException(testcase, thisMethod, thisMethod, e, expectations);
        }

        validationTools.validateResult(thePage, thisMethod, expectations, settings);

        logPageClass(thisMethod, null, thePage);

        return thePage;

    }

    /**
     * Performs the HTMLUnit steps necessary to process most of Provider Social Login panels. The method will get the
     * login, user, and password buttons, user and password from the passed in settings. The code will obtain the panel, fill it
     * in, and then submit it. It also has a hack in to work around a 502 status code that linkedin returns periodically.
     *
     * @param testcase
     *            - the current testcase name (may be helpful for logging)
     * @param webClient
     *            - the current web client - method uses this to invoke the Login panel
     * @param startPage
     *            - the last returned page - method will get the login panel out of this
     * @param settings
     *            - the current test settings - method will use this to get info needed to invoke the panel
     * @param expectations
     *            - the test expectations - will pass this to the validation code after the panel is invoked - that code will use
     *            the expectations to validate the correct response/behavior
     * @return - returns the response from invoking the Login panel
     * @throws Exception
     */
    public static Object performGenericSocialLogin(String testcase, WebClient webClient, HtmlPage startPage, String currentAction, SocialTestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "performGenericSocialLogin";
        msgUtils.printMethodName(thisMethod + "|" + currentAction);
        Object thePage = null;
        try {
            if (startPage.getForms().size() > 0) {
                final HtmlForm form = startPage.getForms().get(0);
                webClient.getOptions().setJavaScriptEnabled(false);
                //            webClient.getOptions().set   rams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);

                Log.info(thisClass, thisMethod + "|" + currentAction, "currentAction: " + currentAction);
                Log.info(thisClass, thisMethod + "|" + currentAction, "Looking for Button name: " + settings.getLoginButton());
                HtmlElement button = null;
                if (provider.equals(SocialConstants.TWITTER_PROVIDER) || provider.equals(SocialConstants.LINKEDIN_PROVIDER)) {
                    button = form.getInputByValue(settings.getLoginButton());
                } else {
                    button = form.getButtonByName(settings.getLoginButton());
                }
                // Twitter's pure authorization page doesn't have username/password inputs, just an Authorize button to click
                //            if (!SocialConstants.TWITTER_AUTHORIZE.equals(currentAction)) {
                final HtmlTextInput textField = form.getInputByName(settings.getUserParm());
                Log.info(thisClass, thisMethod + "|" + currentAction, "username field is: " + textField);
                textField.setValueAttribute(settings.getUserName());
                final HtmlPasswordInput textField2 = form.getInputByName(settings.getPassParm());
                Log.info(thisClass, thisMethod + "|" + currentAction, "password field is: " + textField2);
                textField2.setValueAttribute(settings.getUserPassword());
                Log.info(thisClass, thisMethod + "|" + currentAction, "Setting: " + textField + " to: " + settings.getUserName());
                Log.info(thisClass, thisMethod + "|" + currentAction, "Setting: " + textField2 + " to: " + settings.getUserPassword());
                //            }
                Log.info(thisClass, thisMethod + "|" + currentAction, "\'Pressing the " + button + " button\'");

                thePage = button.click();

                if (AutomationTools.getResponseStatusCode(thePage) == SocialConstants.BAD_GATEWAY) {
                    // try again - LinkedIn throws a 502 periodically
                    thePage = button.click();
                }
            } else {
                thePage = startPage;
            }
            msgUtils.printResponseParts(thePage, thisMethod + "|" + currentAction, "Response from " + thisMethod + "|" + currentAction + ": ");

        } catch (Exception e) {
            handleException(testcase, thisMethod, currentAction, e, expectations);
        }

        msgUtils.printAllCookies(webClient);
        validationTools.validateResult(thePage, currentAction, expectations, settings);

        logPageClass(thisMethod, currentAction, thePage);

        return thePage;

    }

    /**
     * Performs the HTMLUnit steps necessary to process most of Provider Social Login panels. The method will get the
     * login, user, and password buttons, user and password from the passed in settings. The code will obtain the panel, fill it
     * in, and then submit it. It will then submit the redirectform to the RP. It also has a hack in to work around a 502 status
     * code that linkedin returns periodically.
     *
     * @param testcase
     *            - the current testcase name (may be helpful for logging)
     * @param webClient
     *            - the current web client - method uses this to invoke the Login panel
     * @param startPage
     *            - the last returned page - method will get the login panel out of this
     * @param settings
     *            - the current test settings - method will use this to get info needed to invoke the panel
     * @param expectations
     *            - the test expectations - will pass this to the validation code after the panel is invoked - that code will use
     *            the expectations to validate the correct response/behavior
     * @return - returns the response from invoking the Login panel
     * @throws Exception
     */
    public static Object performGenericSocialLoginImplicit(String testcase, WebClient webClient, HtmlPage startPage, String currentAction, SocialTestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "performGenericSocialLoginImplicit";
        msgUtils.printMethodName(thisMethod + "|" + currentAction);
        Object thePage = null;
        try {
            if (startPage.getForms().size() > 0) {
                final HtmlForm form = startPage.getForms().get(0);
                webClient.getOptions().setJavaScriptEnabled(true);
                //            webClient.getOptions().set   rams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);

                Log.info(thisClass, thisMethod + "|" + currentAction, "currentAction: " + currentAction);
                Log.info(thisClass, thisMethod + "|" + currentAction, "Looking for Button name: " + settings.getLoginButton());
                HtmlElement button = null;
                if (provider.equals(SocialConstants.TWITTER_PROVIDER) || provider.equals(SocialConstants.LINKEDIN_PROVIDER)) {
                    button = form.getInputByValue(settings.getLoginButton());
                } else {
                    button = form.getButtonByName(settings.getLoginButton());
                }
                // Twitter's pure authorization page doesn't have username/password inputs, just an Authorize button to click
                //            if (!SocialConstants.TWITTER_AUTHORIZE.equals(currentAction)) {
                final HtmlTextInput textField = form.getInputByName(settings.getUserParm());
                Log.info(thisClass, thisMethod + "|" + currentAction, "username field is: " + textField);
                textField.setValueAttribute(settings.getUserName());
                final HtmlPasswordInput textField2 = form.getInputByName(settings.getPassParm());
                Log.info(thisClass, thisMethod + "|" + currentAction, "password field is: " + textField2);
                textField2.setValueAttribute(settings.getUserPassword());
                Log.info(thisClass, thisMethod + "|" + currentAction, "Setting: " + textField + " to: " + settings.getUserName());
                Log.info(thisClass, thisMethod + "|" + currentAction, "Setting: " + textField2 + " to: " + settings.getUserPassword());
                //            }
                Log.info(thisClass, thisMethod + "|" + currentAction, "\'Pressing the " + button + " button\'");

                thePage = button.click();

                if (AutomationTools.getResponseStatusCode(thePage) == SocialConstants.BAD_GATEWAY) {
                    // try again - LinkeeIn throws a 502 periodically
                    thePage = button.click();
                }
            } else {
                thePage = startPage;
            }
            msgUtils.printResponseParts(thePage, thisMethod + "|" + currentAction, "Response from " + thisMethod + "|" + currentAction + ": ");

        } catch (Exception e) {
            handleException(testcase, thisMethod, currentAction, e, expectations);
        }

        validationTools.validateResult(thePage, currentAction, expectations, settings);

        logPageClass(thisMethod, currentAction, thePage);

        return thePage;

    }

    /**
     * Performs the HTMLUnit steps necessary to process the GitHub Login panel. The method will get the
     * user and password from the passed in settings. The code will obtain the panel, fill it in, and then submit it. GitHub locks
     * out a user when there are too many long in attempts in a short period of time. It'll make you re-authorize access to the
     * users data. This method had hooks to detect that condition, but, unfortunately, we haven't been able to automate processing
     * that panel. So, instead, we log a message, and fail the test case as we can't get to the app.
     *
     * @param testcase
     *            - the current testcase name (may be helpful for logging)
     * @param webClient
     *            - the current web client - method uses this to invoke the GitHub Login panel
     * @param startPage
     *            - the last returned page - method will get the GitHub panel out of this
     * @param settings
     *            - the current test settings - method will use this to get info needed to invoke the panel
     * @param expectations
     *            - the test expectations - will pass this to the validation code after the panel is invoked - that code will use
     *            the expectations to validate the correct response/behavior
     * @return - returns the response from invoking the GitHub Login panel
     * @throws Exception
     */
    public static Object performSocialLogin_GitHub(String testcase, WebClient webClient, HtmlPage startPage, SocialTestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "performSocialLogin_GitHub";
        msgUtils.printMethodName(thisMethod);
        Object thePage = null;
        try {

            HtmlForm form = startPage.getFirstByXPath("//form[@action='/login']");
            //            HtmlTextInput username = (HtmlTextInput) startPage.getElementById(settings.getUserParm());
            HtmlTextInput username = (HtmlTextInput) startPage.getElementById("login_field");
            HtmlPasswordInput password = (HtmlPasswordInput) startPage.getElementById(settings.getPassParm());
            username.setText(settings.getUserName());
            password.setText(settings.getUserPassword());
            Log.info(thisClass, thisMethod, "Setting: username" + " to: " + settings.getUserName());
            Log.info(thisClass, thisMethod, "Setting: password" + " to: " + settings.getUserPassword());
            HtmlSubmitInput firstButton = (HtmlSubmitInput) startPage.getElementByName("commit");

            thePage = firstButton.click();

            msgUtils.printResponseParts(thePage, thisMethod, "Response from " + thisMethod + ": ");

            if (thePage != null) {
                // "Reauthorization required"
                if (!AutomationTools.getResponseTitle(thePage).contains("HelloWorld")) {
                    Log.info(thisClass, thisMethod, "Page title doesn't contain Helloworld");
                }
                if (!AutomationTools.getResponseTitle(thePage).contains("Authorize")) {
                    Log.info(thisClass, thisMethod, "Page title doesn't contain Authorize");
                }
                if (AutomationTools.getResponseText(thePage).contains("Reauthorization required")) {
                    logPageClass(thisMethod, null, thePage);
                    Log.info(thisClass, thisMethod, "Page Content does contain Reauthorization required");
                    fail("Got the stupid reauth page - copy server.xml from the test output tree to the server under build.image servers, start the GitHub server, go to a browser and invoke:  \"https://localhost:8020/helloworld/rest/helloworld\", follow the flow (login, reauth, ...), then try the tests again");
                }
            }

        } catch (Exception e) {
            handleException(testcase, thisMethod, perform_social_login, e, expectations);
        }

        validationTools.validateResult(thePage, perform_social_login, expectations, settings);

        logPageClass(thisMethod, null, thePage);

        return thePage;
    }

    public static Object performSocialLogin_OpenShift(String testcase, WebClient webClient, HtmlPage startPage, SocialTestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "performSocialLogin_OpenShift";
        msgUtils.printMethodName(thisMethod);
        Object thePage = null;
        try {
            Log.info(thisClass, thisMethod, "startPage: " + startPage.toString());
            //            if (startPage.toString() != null && startPage.toString().contains("htpasswd")) {
            try {
                HtmlAnchor htmlAnchor = startPage.getAnchorByText("htpasswd");
                if (htmlAnchor != null) {
                    startPage = htmlAnchor.click();
                    String body = startPage.getBody().asText();
                    Log.info(thisClass, thisMethod, "Body of select login type panel: " + body);
                    //                    msgUtils.printResponseParts(startPage, thisMethod, "Response from " + "select login type: ");
                }
            } catch (Exception e) {
                Log.info(thisClass, thisMethod, "htpasswd not found on page - skip processing it: " + e.getMessage());
            }

            if (startPage.getForms().size() > 0) {
                final HtmlForm form = startPage.getForms().get(0);
                Log.info(thisClass, thisMethod, "num forms: " + startPage.getForms().size());

                HtmlElement button = null;
                DomNodeList<HtmlElement> buttons = form.getElementsByTagName("button");
                for (HtmlElement b : buttons) {
                    //                        String value = b.getAttribute("value");
                    String display = b.asText();
                    Log.info(thisClass, thisMethod, "Button display: " + display);
                    if (settings.getLoginButton().equals(display)) {
                        button = b;
                        break;

                    }
                }

                //            HtmlTextInput username = (HtmlTextInput) startPage.getElementById(settings.getUserParm());
                HtmlTextInput username = (HtmlTextInput) startPage.getElementById(settings.getUserParm());
                HtmlPasswordInput password = (HtmlPasswordInput) startPage.getElementById(settings.getPassParm());
                username.setText(settings.getUserName());
                password.setText(settings.getUserPassword());
                Log.info(thisClass, thisMethod, "Setting: username" + " to: " + settings.getUserName());
                Log.info(thisClass, thisMethod, "Setting: password" + " to: " + settings.getUserPassword());

                thePage = button.click();

                msgUtils.printResponseParts(thePage, thisMethod, "Response from " + thisMethod + ": ");

                if (thePage != null) {
                    // "Reauthorization required"
                    if (!AutomationTools.getResponseTitle(thePage).contains("HelloWorld")) {
                        //                        Log.info(thisClass, thisMethod, "Page title doesn't contain Helloworld");
                        if (AutomationTools.getResponseTitle(thePage).contains("Authorize ")) {
                            HtmlForm nextForm = ((HtmlPage) thePage).getForms().get(0);
                            buttons = nextForm.getElementsByTagName("button");
                            for (HtmlElement b : buttons) {
                                //                        String value = b.getAttribute("value");
                                String display = b.asText();
                                Log.info(thisClass, thisMethod, "Button display: " + display);
                                if (settings.getLoginButton().equals(display)) {
                                    button = b;
                                    break;

                                }
                            }
                            HtmlSubmitInput auth = (HtmlSubmitInput) ((HtmlPage) thePage).getElementByName("approve");
                            thePage = auth.click();
                            //                            button = nextForm.getButtonByName("approve");
                            //                            thePage = button.click();
                        }
                    }
                    //                    if (!AutomationTools.getResponseTitle(thePage).contains("Authorize")) {
                    //                        Log.info(thisClass, thisMethod, "Page title doesn't contain Authorize");
                    //                    }
                    //                    if (AutomationTools.getResponseText(thePage).contains("Reauthorization required")) {
                    //                        logPageClass(thisMethod, null, thePage);
                    //                        Log.info(thisClass, thisMethod, "Page Content does contain Reauthorization required");
                    //                        fail("Got the stupid reauth page - copy server.xml from the test output tree to the server under build.image servers, start the GitHub server, go to a browser and invoke:  \"https://localhost:8020/helloworld/rest/helloworld\", follow the flow (login, reauth, ...), then try the tests again");
                    //                    }
                } else {
                    Log.info(thisClass, thisMethod, "The page is null");
                }
            } else {
                throw new Exception("thisClass, thisMethod: Oopsy - didn't process the page properly");
            }

        } catch (Exception e) {
            handleException(testcase, thisMethod, perform_social_login, e, expectations);
        }

        validationTools.validateResult(thePage, perform_social_login, expectations, settings);

        logPageClass(thisMethod, null, thePage);

        return thePage;
    }

    /**
     * Performs the HTMLUnit steps necessary to process the Social Login Twitter Authorization panel. The method will get the
     * authorizationButton from the passed in settings
     *
     * @param testcase
     *            - the current testcase name (may be helpful for logging)
     * @param webClient
     *            - the current web client - method uses this to invoke the authorization panel
     * @param startPage
     *            - the last returned page - method will get the authorization panel out of this
     * @param settings
     *            - the current test settings - method will use this to get info needed to invoke the panel
     * @param expectations
     *            - the test expectations - will pass this to the validation code after the panel is invoked - that code will use
     *            the expectations to validate the correct response/behavior
     * @return - returns the response from invoking the authorize panel
     * @throws Exception
     */
    public static Object performAuthorize(String testcase, WebClient webClient, HtmlPage startPage, String currentAction, SocialTestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "performAuthorize";
        msgUtils.printMethodName(thisMethod);
        Object thePage = null;
        try {
            final HtmlForm form = startPage.getForms().get(0);
            webClient.getOptions().setJavaScriptEnabled(false);

            HtmlElement button = null;
            if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
                button = form.getInputByValue(settings.getAuthorizeButton());
            } else {
                button = form.getButtonByName(settings.getAuthorizeButton());
            }
            Log.info(thisClass, thisMethod, "\'Pressing the " + button + " button\'");

            thePage = button.click();

            msgUtils.printResponseParts(thePage, thisMethod, "Response from " + thisMethod + ": ");

        } catch (Exception e) {
            handleException(testcase, thisMethod, currentAction, e, expectations);
        }

        validationTools.validateResult(thePage, currentAction, expectations, settings);

        logPageClass(thisMethod, currentAction, thePage);

        return thePage;

    }

    /**
     * Performs the HTMLUnit steps necessary to process the Social Login selection panel. Caller indicates which selection to make
     * by setting the providerButton SocialTestSettings value to the config ID of the desired config. If the caller sets the
     * providerButtonDispaly, we will compare that value against the display value of the button and invoke "fail" if they do not
     * match - this will cause a test case to fail.
     *
     * @param testcase
     *            - the current testcase name (may be helpful for logging)
     * @param webClient
     *            - the current web client - method uses this to invoke the selection panel
     * @param startPage
     *            - the last returned page - method will get the selection panel out of this
     * @param settings
     *            - the current test settings - method will use this to get info needed to select the correct button
     * @param expectations
     *            - the test expectations - will pass this to the validation code after the panel is invoked - that code will use
     *            the expectations to validate the correct response/behavior
     * @return - returns the response from invoking the selection panel - in most cases, this will be the login panel for the
     *         selected provider
     * @throws Exception
     */
    public static Object performSelectProvider(String testcase, WebClient webClient, HtmlPage startPage, SocialTestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "performSelectProvider";
        msgUtils.printMethodName(thisMethod);
        Object thePage = null;
        try {
            final HtmlForm form = startPage.getForms().get(0);
            webClient.getOptions().setJavaScriptEnabled(true);

            HtmlElement button = null;
            DomNodeList<HtmlElement> buttons = form.getElementsByTagName("button");
            for (HtmlElement b : buttons) {
                String value = b.getAttribute("value");
                String display = b.asText();
                Log.info(thisClass, thisMethod, "Button value: " + value + " display: " + display);
                if (getSelectionButtonValue(settings).equals(value)) {
                    if (settings.getProviderButtonDisplay() != null) {
                        if (settings.getProviderButtonDisplay().equals(display)) {
                            button = b;
                            break;
                        } else {
                            fail("Display button did NOT have the correct value.  Was expecting: " + settings.getProviderButtonDisplay() + " and received: " + display);
                        }
                    } else {
                        button = b;
                    }
                }
            }
            if (button == null) {
                throw new Exception("Failed to find a button on the selection page corresponding to provider [" + settings.getProviderButtonDisplay() + "].");
            }

            Log.info(thisClass, thisMethod, "\'Pressing the " + button + " button\'");

            thePage = button.click();

            msgUtils.printResponseParts(thePage, thisMethod, "Response from " + thisMethod + ": ");

        } catch (Exception e) {
            handleException(testcase, thisMethod, SocialConstants.SELECT_PROVIDER, e, expectations);
        }

        validationTools.validateResult(thePage, SocialConstants.SELECT_PROVIDER, expectations, settings);

        return thePage;

    }

    static void handleException(String testcase, String thisMethod, String currentAction, Exception e, List<validationData> expectations) throws Exception {
        Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod + "|" + currentAction);
        System.err.println("Exception: " + e);
        validationTools.validateException(expectations, currentAction, e);
    }

    static String getSelectionButtonValue(SocialTestSettings settings) {
        String method = "getSelectionButtonValue";
        Log.info(thisClass, method, "Getting button value based on provider button: " + settings.getProviderButton());

        // Should reflect the same calculation made in SocialLoginTAI.java
        String buttonValue = new Integer(settings.getProviderButton().hashCode()).toString();

        Log.info(thisClass, method, "Got button value: [" + buttonValue + "]");
        return buttonValue;
    }

    /**
     * Builds and returns the defaultJWT Issuer string/value - defaultJWT is specific JWT config, and does not indicate a default
     * string
     *
     * @param settings
     *            - the current test settings - used to get the current Issuer as a base to beuild the "defaultJWT" string
     * @param newBuilder
     *            - the name of the JWT builder currently in use (what we'll replace with defaultJWT)
     * @return - return the updated issuer string
     * @throws Exception
     */
    public String updateDefaultIssuer(SocialTestSettings settings, String newBuilder) throws Exception {
        return settings.getIssuer().replace("defaultJWT", newBuilder);
    }

    /**
     * Interface for setGoodSocialExpecations that assumes the finalAction is perform_social_login.
     *
     * @param settings
     *            - settings to pass to setGoodSocialExpecations
     * @param addJWTTokenChecks
     *            - flag indicating if we should validate the JWT to pass to getGoodSocialExpectations
     * @return - return a newly created set of expectations
     * @throws Exception
     */
    public List<validationData> setGoodSocialExpectations(SocialTestSettings settings, Boolean addJWTTokenChecks) throws Exception {
        return setGoodSocialExpectations(settings, addJWTTokenChecks, perform_social_login);
    }

    /**
     * Sets good/valid expectations for a standard Social Login flow. Sets expected values for:
     * 1) add check for the selection panel title when the app is invoked (if we're testing with a selection panel)
     * 2) add check for the login panel title if we should get the login panel
     * 3) add check for the HelloWorld title after we login
     * 4) add checks for various standard feilds in the HelloWorld output (varies by provider - values set from the passed in
     * settings)
     * 5) if requested, add expectations to validate the JWT contents
     *
     * @param settings
     *            - current test settings - used to set values to check in 1) the login page and 2) HelloWorld output
     * @param addJWTTokenChecks
     *            - flag to indicate if we should add checks for the content of the JWT token that is included in the HelloWorld
     *            output
     * @param finalAction
     *            - The final step in the list of steps - mainly indicates when we should validate the HelloWorld content
     * @return - returns a newly created set of expectations (List<validationdata>)
     * @throws Exception
     */
    public List<validationData> setGoodSocialExpectations(SocialTestSettings settings, Boolean addJWTTokenChecks, String finalAction) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(invoke_social_login_actions);
        if (usesSelectionPanel) {
            expectations = setDefaultSelectionPageExpectations(expectations, settings);
            expectations = setLoginPageExpectation(expectations, settings, SocialConstants.SELECT_PROVIDER);
        } else {
            expectations = setLoginPageExpectation(expectations, settings, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        }

        expectations = setGoodHelloWorldExpectations(expectations, settings, addJWTTokenChecks, finalAction);

        return expectations;
    }

    /**
     * Adds expectations to validate that the social media selection page was encountered.
     */
    public List<validationData> setDefaultSelectionPageExpectations(List<validationData> expectations, SocialTestSettings settings) throws Exception {
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "Did not get to the default social media selection page.", null, SocialConstants.SELECTION_TITLE);

        String expectedFormAction = getExpectedFormAction(settings);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did not find the expected protected resource URL as the form action in the default selection page.", null, "<form action=\"" + expectedFormAction + "\"");

        return expectations;
    }

    /**
     * The form action should be the protected resource URL without the query string. Any query string parameters should be hidden
     * inputs within the form.
     */
    protected String getExpectedFormAction(SocialTestSettings settings) {
        String expectedFormAction = settings.getProtectedResource();
        if (expectedFormAction.contains("?")) {
            expectedFormAction = expectedFormAction.substring(0, expectedFormAction.indexOf("?"));
        }
        return expectedFormAction;
    }

    /**
     * Adds the login page expectation as needed. (Makes sure that we get to the provider login page for the specified step)
     * LinkedIn sometimes returns the Sign In page, other times, it returns the Log In page which makes it impossible to verify
     * cleanly.
     * Add a check for the login page if we're NOT testing with LinkedIn
     *
     * @param expectations
     *            - current expectations
     * @param settings
     *            - current settings
     * @param step
     *            - the step to check for the login page
     * @return - return updated (if necessary) expectations
     * @throws Exception
     */
    public List<validationData> setLoginPageExpectation(List<validationData> expectations, SocialTestSettings settings, String step) throws Exception {

        if (!provider.equals(SocialConstants.LINKEDIN_PROVIDER)) {
            expectations = vData.addExpectation(expectations, step, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "Did not get to the Login page", null, settings.getLoginPage());
        }
        return expectations;
    }

    public List<validationData> setGoodHelloWorldExpectations(SocialTestSettings settings, Boolean addJWTTokenChecks, String finalAction) throws Exception {
        return setGoodHelloWorldExpectations(null, settings, addJWTTokenChecks, finalAction);
    }

    public List<validationData> setGoodHelloWorldExpectations(List<validationData> expectations, SocialTestSettings settings, Boolean addJWTTokenChecks, String finalAction) throws Exception {
        if (expectations == null) {
            expectations = vData.addSuccessStatusCodesForActions(invoke_social_login_actions);
        }

        expectations = vData.addExpectation(expectations, finalAction, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did not get to the HelloWorld App", null, SocialConstants.HELLOWORLD_MSG);
        expectations = vData.addExpectation(expectations, finalAction, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Was expecting: " + settings.getRealm() + " as the realmName", null, "RealmName=" + settings.getRealm());
        if (provider != null && provider.equals(SocialConstants.GITHUB_PROVIDER) && configType != null && configType.equals(GenericConfig)) {
            expectations = vData.addExpectation(expectations, finalAction, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_MATCHES, "Was expecting: " + SocialConstants.GITHUB_USER2_ID + " as the principal", null, "Principal.*" + SocialConstants.GITHUB_USER2_ID);
        } else {
            expectations = vData.addExpectation(expectations, finalAction, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_MATCHES, "Was expecting: " + settings.getUserName() + " as the principal", null, "Principal.*" + settings.getUserName());
        }

        if (addJWTTokenChecks) {
            expectations = setJwtExpectations(expectations, settings, finalAction);
        } else {
            // make sure issuedJwt does NOT appear in the subject
            expectations = vData.addExpectation(expectations, finalAction, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Found issuedJwt in response and should NOT Have", null, SocialConstants.ISSUED_JWT_STRING);
        }

        return expectations;
    }

    public List<validationData> setJwtExpectations(List<validationData> expectations, SocialTestSettings settings, String finalAction) throws Exception {
        if (expectations == null) {
            expectations = vData.addSuccessStatusCodesForActions(invoke_social_login_actions);
        }
        // add validation of JWT Token required claims
        expectations = vData.addExpectation(expectations, finalAction, SocialConstants.RESPONSE_JWT_TOKEN, SocialConstants.STRING_CONTAINS, "Token did NOT validate properly", null, null);
        // add validation of JWT Token cliam values  - TODO - may have some that vary by provider (can't tell as of yet - we're only testing with Facebook)
        expectations = vData.addExpectation(expectations, finalAction, SocialConstants.RESPONSE_JWT_TOKEN, SocialConstants.STRING_CONTAINS, "Token iss is not set as expected", SocialConstants.PAYLOAD_ISSUER, settings.getIssuer());
        //        expectations = vData.addExpectation(expectations, PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_JWT_TOKEN, SocialConstants.STRING_CONTAINS, "Token " + settings.getUserParm() + " is not set as expected", settings.getUserParm(), settings.getUserName());
        if (!(provider.equals(SocialConstants.GITHUB_PROVIDER) && configType.equals(GenericConfig)) && (!provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) && (!provider.equals(SocialConstants.OPENSHIFT_PROVIDER))) { // github spcific config omits email
            String emailKey = "email";
            if (provider.equalsIgnoreCase(SocialConstants.LINKEDIN_PROVIDER)) {
                emailKey = "emailAddress";
            }
            expectations = vData.addExpectation(expectations, finalAction, SocialConstants.RESPONSE_JWT_TOKEN, SocialConstants.STRING_CONTAINS, "Token email is not set as expected", emailKey, settings.getUserName());
        }
        if (!provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
            expectations = vData.addExpectation(expectations, finalAction, SocialConstants.RESPONSE_JWT_TOKEN, SocialConstants.STRING_CONTAINS, "Token token_type is not set as expected", SocialConstants.TOKEN_TYPE_KEY, SocialConstants.BEARER);
        }
        if (provider.equals(SocialConstants.FACEBOOK_PROVIDER)) { //TODO adjust logic as we see which providers always add name
            expectations = vData.addExpectation(expectations, finalAction, SocialConstants.RESPONSE_JWT_TOKEN, SocialConstants.STRING_CONTAINS, "Token name is not set as expected", SocialConstants.NAME_KEY, SocialConstants.IBM_SOCIAL_NAME);
        }
        return expectations;
    }

    /**
     * Sets general 401 Base Expectations for Social Login. Will set 401 for perform_social_login. It will also set an expectation
     * to look for unauthorized error in the response message
     *
     * @param settings
     *            - the current social login test settings
     * @return - returns updated expectations
     * @throws Exception
     */
    public List<validationData> set401ResponseBaseExpectations(SocialTestSettings settings) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(perform_social_login, invoke_social_login_actions);
        expectations = vData.addResponseStatusExpectation(expectations, perform_social_login, SocialConstants.UNAUTHORIZED_STATUS);

        expectations = setLoginPageExpectation(expectations, settings, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.UNAUTHORIZED_MESSAGE, null, SocialConstants.UNAUTHORIZED_MESSAGE);

        return expectations;
    }

    /**
     * Sets general Error Page expectations for Social Login. Will set a non-200 status code for the step specified. Will also add
     * expectations for error info in the title and full response
     *
     * @param finalAction
     *            - The step that receives a status code of something other than 200
     * @param statusCode
     *            - the non-200 status code to set
     * @return - returns the new expectations
     * @throws Exception
     */
    public List<validationData> setErrorPageExpectations(String finalAction, int statusCode) throws Exception {
        List<validationData> expectations = vData.addResponseStatusExpectation(null, finalAction, statusCode);
        expectations = vData.addExpectation(expectations, finalAction, genericTestServer, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "HTML error page did not contain the expected title. We likely reached a page that we shouldn't have gotten to.", null, SocialConstants.HTTP_ERROR_MESSAGE);
        expectations = vData.addExpectation(expectations, finalAction, genericTestServer, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "HTML error page did not contain message indicating that the user cannot be authenticated.", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
        return expectations;
    }

    public List<validationData> set403ErrorPageForSocialLogin(String... specificMsg) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.FORBIDDEN_STATUS);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.FORBIDDEN, null, SocialConstants.FORBIDDEN);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "Title did NOT indicate an issue", null, SocialConstants.HTTP_ERROR_MESSAGE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did NOT indicate that we can not authenticate the user", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
        if (specificMsg != null) {
            for (String msg : specificMsg) {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", msg);
            }
        }
        return expectations;

    }

    public List<validationData> setErrorPageForSocialLogin(String... specificMsg) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.UNAUTHORIZED_STATUS);
        return setErrorPageForSocialLogin(expectations, specificMsg);
    }

    public List<validationData> setErrorPageForSocialLogin(List<validationData> expectations, String... specificMsg) throws Exception {
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.UNAUTHORIZED_MESSAGE, null, SocialConstants.UNAUTHORIZED_MESSAGE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "Title did NOT indicate an issue", null, SocialConstants.HTTP_ERROR_MESSAGE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did NOT indicate that we can not authenticate the user", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
        if (specificMsg != null) {
            for (String msg : specificMsg) {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", msg);
            }
        }
        return expectations;
    }

    public List<validationData> setUnexpectedErrorPageForSocialLogin(String specificMsg) throws Exception {
        List<validationData> expectations = null;

        if (provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
            expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
            expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.BAD_REQUEST_STATUS);
            expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.BAD_REQUEST, null, SocialConstants.BAD_REQUEST);
            expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did NOT indicate that we can not authenticate the user", null, "server_error");
        } else {
            expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
            //            , SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
            //            expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.UNAUTHORIZED_STATUS
            expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did NOT indicate that we can not authenticate the user", null, SocialMessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING);
        }
        if (specificMsg != null) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", specificMsg);
        }
        return expectations;
    }

    public List<validationData> setUnauthorizedErrorPageForSocialLogin(String... specificMsg) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.FORBIDDEN_STATUS);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.FORBIDDEN, null, SocialConstants.FORBIDDEN);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did NOT indicate that we can not authorize the user", null, SocialConstants.AUTHORIZATION_ERROR);
        if (specificMsg != null) {
            for (String msg : specificMsg) {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", msg);
            }
        }
        return expectations;
    }

    /**
     * Set Social Login generic settings for use with Facebook configurations.
     * Values are set that allow the test to use it as the Social login provider. We'll also set
     * values that are used to validate that we format the jwt correctly and maintain all of
     * the data that we expect.
     *
     * @param socialSettings
     *            - the current socialSettings that we'll be updating
     * @return - returns and updated SocialTestSettings object
     * @throws Exception
     */
    public static SocialTestSettings updateFacebookSettings(SocialTestSettings socialSettings) throws Exception {
        return updateFacebookSettings(socialSettings, genericTestServer);
    }

    public static SocialTestSettings updateFacebookSettings(SocialTestSettings socialSettings, TestServer protectedResourceServer) throws Exception {

        socialSettings.setProtectedResource(protectedResourceServer.getServerHttpsString() + "/helloworld/rest/helloworld");

        socialSettings.setAdminUser(SocialConstants.FACEBOOK_USER2_EMAIL);
        socialSettings.setAdminPswd(SocialConstants.FACEBOOK_USER2_PW);
        socialSettings.setUserName(SocialConstants.FACEBOOK_USER2_EMAIL);
        socialSettings.setUserPassword(SocialConstants.FACEBOOK_USER2_PW);
        socialSettings.setUserId(SocialConstants.FACEBOOK_USER2_ID);
        socialSettings.setRealm(SocialConstants.FACEBOOK_REALM);
        socialSettings.setLoginPage(SocialConstants.FACEBOOK_LOGIN_TITLE);
        socialSettings.setSignatureAlg(SocialConstants.SIGALG_RS256);

        ArrayList<String> requiredKeys = new ArrayList<String>();
        requiredKeys.add(SocialConstants.PAYLOAD_ISSUER);
        requiredKeys.add(SocialConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS);
        requiredKeys.add(SocialConstants.PAYLOAD_ISSUED_AT_TIME_IN_SECS);
        requiredKeys.add(SocialConstants.TOKEN_TYPE_KEY);
        requiredKeys.add("name");
        requiredKeys.add("email");
        requiredKeys.add("id");
        socialSettings.setRequiredJwtKeys(requiredKeys);
        socialSettings.setRsTokenType(SocialConstants.SOCIAL_JWT_TOKEN);

        socialSettings.setUserParm("email");
        socialSettings.setPassParm("pass");
        socialSettings.setLoginButton("login");
        socialSettings.setProviderButton(SocialConstants.FACEBOOK_LOGIN);
        socialSettings.setIssuer("https://" + InetAddress.getLocalHost().toString().split("/")[1] + ":" + protectedResourceServer.getServerHttpsPort() + "/jwt/defaultJWT");

        return socialSettings;
    }

    /**
     * Set Social Login generic settings for use with GitHub configurations.
     * Values are set that allow the test to use it as the Social login provider. We'll also set
     * values that are used to validate that we format the jwt correctly and maintain all of
     * the data that we expect.
     *
     * @param socialSettings
     *            - the current socialSettings that we'll be updating
     * @return - returns and updated SocialTestSettings object
     * @throws Exception
     */
    public static SocialTestSettings updateGitHubSettings(SocialTestSettings socialSettings) throws Exception {

        socialSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld");

        socialSettings.setAdminUser(SocialConstants.GITHUB_USER2_EMAIL);
        socialSettings.setAdminPswd(SocialConstants.GITHUB_USER2_PW);
        socialSettings.setUserName(SocialConstants.GITHUB_USER2_EMAIL);
        socialSettings.setUserPassword(SocialConstants.GITHUB_USER2_PW);
        socialSettings.setUserId(SocialConstants.GITHUB_USER2_ID);
        socialSettings.setRealm(SocialConstants.GITHUB_REALM);
        socialSettings.setLoginPage(SocialConstants.GITHUB_LOGIN_TITLE);
        socialSettings.setSignatureAlg(SocialConstants.SIGALG_RS256);

        ArrayList<String> requiredKeys = new ArrayList<String>();
        requiredKeys.add(SocialConstants.PAYLOAD_ISSUER);
        requiredKeys.add(SocialConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS);
        requiredKeys.add(SocialConstants.PAYLOAD_ISSUED_AT_TIME_IN_SECS);
        requiredKeys.add(SocialConstants.TOKEN_TYPE_KEY);

        if (!(provider.equals(SocialConstants.GITHUB_PROVIDER) && configType.equals(GenericConfig))) {
            requiredKeys.add("email");
        }
        socialSettings.setRequiredJwtKeys(requiredKeys);
        socialSettings.setRsTokenType(SocialConstants.SOCIAL_JWT_TOKEN);

        socialSettings.setUserParm("login");
        socialSettings.setPassParm("password");
        socialSettings.setLoginButton("commit");
        socialSettings.setProviderButton(SocialConstants.GITHUB_LOGIN);
        socialSettings.setIssuer("https://" + InetAddress.getLocalHost().toString().split("/")[1] + ":" + genericTestServer.getServerHttpsPort() + "/jwt/defaultJWT");

        return socialSettings;
    }

    /**
     * Set Social Login generic settings for use with Twitter configurations.
     * Values are set that allow the test to use it as the Social login provider. We'll also set
     * values that are used to validate that we format the jwt correctly and maintain all of
     * the data that we expect.
     *
     * @param socialSettings
     *            - the current socialSettings that we'll be updating
     * @return - returns and updated SocialTestSettings object
     * @throws Exception
     */
    public static SocialTestSettings updateTwitterSettings(SocialTestSettings socialSettings) throws Exception {
        return updateFacebookSettings(socialSettings, genericTestServer);
    }

    public static SocialTestSettings updateTwitterSettings(SocialTestSettings socialSettings, TestServer protectedResourceServer) throws Exception {

        socialSettings.setProtectedResource(protectedResourceServer.getServerHttpsString() + "/helloworld/rest/helloworld");

        socialSettings.setAdminUser(SocialConstants.TWITTER_USER1_EMAIL);
        socialSettings.setAdminPswd(SocialConstants.TWITTER_USER1_PW);
        socialSettings.setUserName(SocialConstants.TWITTER_USER1_EMAIL);
        socialSettings.setUserPassword(SocialConstants.TWITTER_USER1_PW);
        socialSettings.setUserId(SocialConstants.TWITTER_USER1_ID);
        socialSettings.setRealm(SocialConstants.TWITTER_REALM);
        socialSettings.setLoginPage(SocialConstants.TWITTER_LOGIN_AND_AUTHORIZE_TITLE);
        socialSettings.setSignatureAlg(SocialConstants.SIGALG_RS256);

        ArrayList<String> requiredKeys = new ArrayList<String>();
        requiredKeys.add(SocialConstants.PAYLOAD_ISSUER);
        requiredKeys.add(SocialConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS);
        requiredKeys.add(SocialConstants.PAYLOAD_ISSUED_AT_TIME_IN_SECS);
        requiredKeys.add(SocialConstants.TOKEN_TYPE_KEY);
        requiredKeys.add("name");
        requiredKeys.add("email");
        requiredKeys.add("id");
        socialSettings.setRequiredJwtKeys(requiredKeys);
        socialSettings.setRsTokenType(SocialConstants.SOCIAL_JWT_TOKEN);

        socialSettings.setUserParm("session[username_or_email]");
        socialSettings.setPassParm("session[password]");
        socialSettings.setLoginButton(SocialConstants.TWITTER_LOGIN_BUTTON_NAME);
        socialSettings.setAuthorizeButton(SocialConstants.TWITTER_AUTHORIZE_BUTTON_NAME);
        socialSettings.setProviderButton(SocialConstants.TWITTER_LOGIN);
        socialSettings.setIssuer("https://" + InetAddress.getLocalHost().toString().split("/")[1] + ":" + protectedResourceServer.getServerHttpsPort() + "/jwt/defaultJWT");

        return socialSettings;
    }

    /**
     * Set Social Login generic settings for use with Linkedin configurations.
     * Values are set that allow the test to use it as the Social login provider. We'll also set
     * values that are used to validate that we format the jwt correctly and maintain all of
     * the data that we expect.
     *
     * @param socialSettings
     *            - the current socialSettings that we'll be updating
     * @return - returns and updated SocialTestSettings object
     * @throws Exception
     */
    public static SocialTestSettings updateLinkedinSettings(SocialTestSettings socialSettings) throws Exception {
        return updateLinkedinSettings(socialSettings, genericTestServer);
    }

    public static SocialTestSettings updateLinkedinSettings(SocialTestSettings socialSettings, TestServer protectedResourceServer) throws Exception {

        socialSettings.setProtectedResource(protectedResourceServer.getServerHttpsString() + "/helloworld/rest/helloworld");

        socialSettings.setAdminUser(SocialConstants.LINKEDIN_USER1_EMAIL);
        socialSettings.setAdminPswd(SocialConstants.LINKEDIN_USER1_PW);
        socialSettings.setUserName(SocialConstants.LINKEDIN_USER1_EMAIL);
        socialSettings.setUserPassword(SocialConstants.LINKEDIN_USER1_PW);
        socialSettings.setUserId(SocialConstants.LINKEDIN_USER1_ID);
        socialSettings.setRealm(SocialConstants.LINKEDIN_REALM);
        socialSettings.setLoginPage(SocialConstants.LINKEDIN_LOGIN_TITLE);
        socialSettings.setSignatureAlg(SocialConstants.SIGALG_RS256);

        ArrayList<String> requiredKeys = new ArrayList<String>();
        requiredKeys.add(SocialConstants.PAYLOAD_ISSUER);
        requiredKeys.add(SocialConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS);
        requiredKeys.add(SocialConstants.PAYLOAD_ISSUED_AT_TIME_IN_SECS);
        requiredKeys.add(SocialConstants.TOKEN_TYPE_KEY);
        requiredKeys.add("firstName");
        requiredKeys.add("lastName");
        requiredKeys.add("emailAddress");
        requiredKeys.add("id");
        socialSettings.setRequiredJwtKeys(requiredKeys);
        socialSettings.setRsTokenType(SocialConstants.SOCIAL_JWT_TOKEN);

        socialSettings.setUserParm("session_key");
        socialSettings.setPassParm("session_password");
        socialSettings.setLoginButton("Sign In");
        socialSettings.setProviderButton(SocialConstants.LINKEDIN_LOGIN);
        requiredKeys.add(SocialConstants.PAYLOAD_ISSUER);
        requiredKeys.add(SocialConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS);
        requiredKeys.add(SocialConstants.PAYLOAD_ISSUED_AT_TIME_IN_SECS);
        socialSettings.setIssuer("https://" + InetAddress.getLocalHost().toString().split("/")[1] + ":" + protectedResourceServer.getServerHttpsPort() + "/jwt/defaultJWT");

        return socialSettings;
    }

    /**
     * Set Social Login generic settings for use with Liberty OP OAUTH/OIDC configurations.
     * Values are set that allow the test to use it as the Social login provider. We'll also set
     * values that are used to validate that we format the jwt correctly and maintain all of
     * the data that we expect.
     *
     * @param socialSettings
     *            - the current socialSettings that we'll be updating
     * @return - returns and updated SocialTestSettings object
     * @throws Exception
     */
    public static SocialTestSettings updateLibertyOPSettings(SocialTestSettings socialSettings) throws Exception {
        return updateLibertyOPSettings(socialSettings, genericTestServer);
    }

    public static SocialTestSettings updateLibertyOPSettings(SocialTestSettings socialSettings, TestServer protectedResourceServer) throws Exception {

        socialSettings.setProtectedResource(protectedResourceServer.getServerHttpsString() + "/helloworld/rest/helloworld");

        socialSettings.setAdminUser(SocialConstants.LIBERTYOP_USER2_EMAIL);
        socialSettings.setAdminPswd(SocialConstants.LIBERTYOP_USER2_PW);
        socialSettings.setUserName(SocialConstants.LIBERTYOP_USER2_EMAIL);
        socialSettings.setUserPassword(SocialConstants.LIBERTYOP_USER2_PW);
        socialSettings.setUserId(SocialConstants.LIBERTYOP_USER2_ID);
        socialSettings.setRealm(testOPServer.getServerHttpsString());
        socialSettings.setLoginPage(SocialConstants.OIDC_LOGINTITLE);
        socialSettings.setSignatureAlg(SocialConstants.SIGALG_RS256);

        ArrayList<String> requiredKeys = new ArrayList<String>();
        requiredKeys.add(SocialConstants.PAYLOAD_ISSUER);
        requiredKeys.add(SocialConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS);
        requiredKeys.add(SocialConstants.PAYLOAD_ISSUED_AT_TIME_IN_SECS);
        if (oidcLoginStyle) {
            //            requiredKeys.add(SocialConstants.PAYLOAD_AT_HASH);
            //            requiredKeys.add(SocialConstants.PAYLOAD_AUDIENCE);
            //            requiredKeys.add(SocialConstants.ACCTOK_REALM_KEY);
            //            requiredKeys.add(SocialConstants.ACCTOK_UNIQ_SEC_NAME_KEY);
            //            requiredKeys.add(SocialConstants.PAYLOAD_NONCE);
        } else {
            if (validationEndpointIsUserinfo()) {
                requiredKeys.add("name");
            }
        }
        requiredKeys.add(SocialConstants.PAYLOAD_SUBJECT);

        socialSettings.setRequiredJwtKeys(requiredKeys);
        socialSettings.setRsTokenType(SocialConstants.SOCIAL_JWT_TOKEN);

        socialSettings.setUserParm(SocialConstants.OIDC_USERPARM);
        socialSettings.setPassParm(SocialConstants.OIDC_PASSPARM);
        socialSettings.setLoginButton(SocialConstants.OIDC_LOGINBUTTON);
        socialSettings.setIssuer("https://" + InetAddress.getLocalHost().toString().split("/")[1] + ":" + protectedResourceServer.getServerHttpsPort() + "/jwt/defaultJWT");

        return socialSettings;
    }

    static void logPageClass(String currentMethod, String currentAction, Object thePage) {
        String pageClass = thePage == null ? "null" : thePage.getClass().getSimpleName();
        String logValue = pageClass;
        if (!isSupportedPageType(pageClass)) {
            logValue = "NOT SUPPORTED (" + pageClass + ")";
        }
        Log.info(thisClass, currentMethod + (currentAction == null ? "" : "|" + currentAction), "Current page class is " + logValue);
    }

    static boolean isSupportedPageType(String pageClass) {
        return getSupportedPageClassNames().contains(pageClass);
    }

    static List<String> getSupportedPageClassNames() {
        List<String> knownTypes = new ArrayList<String>();
        knownTypes.add(HtmlPage.class.getSimpleName());
        knownTypes.add(TextPage.class.getSimpleName());
        return knownTypes;
    }

    public static Boolean shouldExternalProviderTestsRun() throws Exception {
        Map<String, String> env = System.getenv();
        for (String envName : env.keySet()) {
            System.out.format("%s=%s%n",
                    envName,
                    env.get(envName));
        }
        return false;
    }

    /**
     * Set Social Login generic settings for use with OpenShift configurations.
     * Values are set that allow the test to use it as the Social login provider. We'll also set
     * values that are used to validate that we format the jwt correctly and maintain all of
     * the data that we expect.
     *
     * @param socialSettings
     *            - the current socialSettings that we'll be updating
     * @return - returns and updated SocialTestSettings object
     * @throws Exception
     */
    public static SocialTestSettings updateOpenShiftSettings(SocialTestSettings socialSettings) throws Exception {
        return updateOpenShiftSettings(socialSettings, genericTestServer);
    }

    public static SocialTestSettings updateOpenShiftSettings(SocialTestSettings socialSettings, TestServer protectedResourceServer) throws Exception {

        socialSettings.setProtectedResource(protectedResourceServer.getServerHttpsString() + "/helloworld/rest/helloworld");

        socialSettings.setAdminUser(protectedResourceServer.getBootstrapProperty("test.user"));
        socialSettings.setAdminPswd(protectedResourceServer.getBootstrapProperty("test.user.pw"));
        socialSettings.setUserName(protectedResourceServer.getBootstrapProperty("test.user"));
        socialSettings.setUserPassword(protectedResourceServer.getBootstrapProperty("test.user.pw"));
        socialSettings.setUserId(protectedResourceServer.getBootstrapProperty("test.user"));
        String userapi = protectedResourceServer.getBootstrapProperty("oauth.server.userapi");
        if (userapi == null) {
            socialSettings.setRealm(null);
        } else {
            socialSettings.setRealm(userapi.replace("\\", ""));
        }

        socialSettings.setLoginPage(genericTestServer.getBootstrapProperty("login.title"));
        socialSettings.setSignatureAlg(SocialConstants.SIGALG_RS256);

        ArrayList<String> requiredKeys = new ArrayList<String>();
        requiredKeys.add(SocialConstants.PAYLOAD_ISSUER);
        requiredKeys.add(SocialConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS);
        requiredKeys.add(SocialConstants.PAYLOAD_ISSUED_AT_TIME_IN_SECS);
        requiredKeys.add(SocialConstants.TOKEN_TYPE_KEY);
        // requiredKeys
        requiredKeys.add("username");
        requiredKeys.add("groups");
        socialSettings.setRequiredJwtKeys(requiredKeys);
        socialSettings.setRsTokenType(SocialConstants.SOCIAL_JWT_TOKEN);

        socialSettings.setUserParm("inputUsername");
        socialSettings.setPassParm("inputPassword");
        socialSettings.setLoginButton(genericTestServer.getBootstrapProperty("login.button"));
        socialSettings.setProviderButton(SocialConstants.OPENSHIFT_LOGIN);
        socialSettings.setIssuer("https://" + InetAddress.getLocalHost().toString().split("/")[1] + ":" + protectedResourceServer.getServerHttpsPort() + "/jwt/defaultJWT");

        return socialSettings;
    }

    public static SocialTestSettings updateStubbedOpenShiftSettings(SocialTestSettings socialSettings) throws Exception {
        return updateStubbedOpenShiftSettings(socialSettings, genericTestServer);
    }

    public static SocialTestSettings updateStubbedOpenShiftSettings(SocialTestSettings socialSettings, TestServer protectedResourceServer) throws Exception {

        updateOpenShiftSettings(socialSettings, protectedResourceServer);

        socialSettings.setAdminUser("admin");
        socialSettings.setAdminPswd("admin");
        socialSettings.setUserName("admin");
        socialSettings.setUserPassword("admin");
        socialSettings.setUserId("admin");
        socialSettings.setRealm("https://localhost:" + protectedResourceServer.getServerHttpsPort() + "/StubbedOpenShift");

        return socialSettings;
    }

    public static boolean validationEndpointIsUserinfo() {
        return ((classOverrideValidationEndpointValue == null) || (classOverrideValidationEndpointValue != null && classOverrideValidationEndpointValue.contains(Constants.USERINFO_ENDPOINT)));
    }

    public static boolean validationEndpointIsIntrospect() {
        return (classOverrideValidationEndpointValue != null && classOverrideValidationEndpointValue.contains(Constants.INTROSPECTION_ENDPOINT));
    }
}
