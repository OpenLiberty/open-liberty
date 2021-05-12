/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.social.fat.utils;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;

public class SocialConstants extends Constants {

    public static final String SERVER_NAME = "com.ibm.ws.security.social_fat";

    public static final String STUBBED_USER_VALIDATION_API_SERVLET = "StubbedOKDServiceLogin";

    public static final String STUBBED_OPENSHIFT_SERVLET = "StubbedOpenShift";
    public static final String OPENSHIFT = "OpenShift";

    public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";

    public static final String SOCIAL_LOGIN_HINT = "social_login_hint";

    public static final String IBM_SOCIAL_NAME = "IBM Social Login2";
    public static final String NAME_KEY = "name";
    public static final String ISSUED_JWT_STRING = "issuedJwt";
    public static final String ERROR_TITLE = "Error";
    public static final String DEFAULT_CREDENTIAL_SUBMIT_BTN_VAL = "Submit";

    public static final String INVOKE_SOCIAL_RESOURCE = "invokeSocialResource";
    public static final String SELECT_PROVIDER = "selectProvider";
    public static final String PERFORM_CREDENTIAL_LOGIN = "performCredentialLogin";
    public static final String PERFORM_SOCIAL_LOGIN = "performSocialLogin";
    public static final String PERFORM_SOCIAL_IMPLICIT_LOGIN = "performSocialImplicitLogin";
    public static final String FACEBOOK_PERFORM_SOCIAL_LOGIN = PERFORM_SOCIAL_LOGIN;
    public static final String GITHUB_PERFORM_SOCIAL_LOGIN = "performGitHubSocialLogin";
    public static final String TWITTER_PERFORM_LOGIN = "performTwitterLogin";
    public static final String TWITTER_PERFORM_SIGN_IN = "performTwitterSignIn";
    public static final String TWITTER_PERFORM_AUTHORIZE = "performTwitterAuthorize";
    public static final String TWITTER_PERFORM_LOGIN_AND_AUTHORIZE = "performTwitterLoginAndAuthorize";
    public static final String LINKEDIN_PERFORM_SOCIAL_LOGIN = PERFORM_SOCIAL_LOGIN;
    public static final String LINKEDIN_PERFORM_SIGN_IN = "performLinkedinSignIn";
    public static final String LIBERTYOP_PERFORM_SOCIAL_LOGIN = PERFORM_SOCIAL_LOGIN;
    public static final String LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN = PERFORM_SOCIAL_IMPLICIT_LOGIN;
    public static final String OPENSHIFT_PERFORM_SOCIAL_LOGIN = "performOpenShiftLogin";

    public static final String[] INVOKE_SOCIAL_RESOURCE_ONLY = { INVOKE_SOCIAL_RESOURCE };
    public static final String[] FACEBOOK_INVOKE_SOCIAL_LOGIN_ACTIONS = { INVOKE_SOCIAL_RESOURCE, FACEBOOK_PERFORM_SOCIAL_LOGIN };
    public static final String[] FACEBOOK_INVOKE_SOCIAL_LOGIN_WITH_SELECTION_ACTIONS = { INVOKE_SOCIAL_RESOURCE, SELECT_PROVIDER, FACEBOOK_PERFORM_SOCIAL_LOGIN };
    public static final String[] FACEBOOK_INVOKE_SOCIAL_JUST_LOGIN_ACTIONS = { FACEBOOK_PERFORM_SOCIAL_LOGIN };
    public static final String[] GITHUB_INVOKE_SOCIAL_LOGIN_ACTIONS = { INVOKE_SOCIAL_RESOURCE, GITHUB_PERFORM_SOCIAL_LOGIN };
    public static final String[] GITHUB_INVOKE_SOCIAL_LOGIN_WITH_SELECTION_ACTIONS = { INVOKE_SOCIAL_RESOURCE, SELECT_PROVIDER, GITHUB_PERFORM_SOCIAL_LOGIN };
    public static final String[] GITHUB_INVOKE_SOCIAL_JUST_LOGIN_ACTIONS = { GITHUB_PERFORM_SOCIAL_LOGIN };
    public static final String[] TWITTER_INVOKE_SOCIAL_LOGIN_ACTIONS = { INVOKE_SOCIAL_RESOURCE, TWITTER_PERFORM_SIGN_IN };
    public static final String[] TWITTER_INVOKE_SOCIAL_LOGIN_WITH_SELECTION_ACTIONS = { INVOKE_SOCIAL_RESOURCE, SELECT_PROVIDER, TWITTER_PERFORM_SIGN_IN };
    //    public static final String[] TWITTER_INVOKE_SOCIAL_JUST_LOGIN_ACTIONS = { TWITTER_LOGIN, TWITTER_AUTHORIZE };
    public static final String[] TWITTER_INVOKE_SOCIAL_JUST_LOGIN_ACTIONS = { TWITTER_PERFORM_LOGIN };
    public static final String[] LINKEDIN_INVOKE_SOCIAL_LOGIN_ACTIONS = { INVOKE_SOCIAL_RESOURCE, LINKEDIN_PERFORM_SOCIAL_LOGIN };
    public static final String[] LINKEDIN_INVOKE_SOCIAL_LOGIN_WITH_SELECTION_ACTIONS = { INVOKE_SOCIAL_RESOURCE, SELECT_PROVIDER, LINKEDIN_PERFORM_SOCIAL_LOGIN };
    public static final String[] LINKEDIN_INVOKE_SOCIAL_JUST_LOGIN_ACTIONS = { LINKEDIN_PERFORM_SOCIAL_LOGIN };
    public static final String[] LINKEDIN_SIGN_IN_AND_LOGIN_ACTIONS = { LINKEDIN_PERFORM_SIGN_IN, LINKEDIN_PERFORM_SOCIAL_LOGIN };
    public static final String[] LIBERTYOP_INVOKE_SOCIAL_LOGIN_ACTIONS = { INVOKE_SOCIAL_RESOURCE, LIBERTYOP_PERFORM_SOCIAL_LOGIN };
    public static final String[] LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS = { INVOKE_SOCIAL_RESOURCE, LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN };
    public static final String[] LIBERTYOP_INVOKE_SOCIAL_LOGIN_WITH_SELECTION_ACTIONS = { INVOKE_SOCIAL_RESOURCE, SELECT_PROVIDER, LIBERTYOP_PERFORM_SOCIAL_LOGIN };
    public static final String[] LIBERTYOP_INVOKE_SOCIAL_LOGIN_WITH_SELECTION_AND_USER_CREDS_ACTIONS = { INVOKE_SOCIAL_RESOURCE, PERFORM_CREDENTIAL_LOGIN };
    public static final String[] LIBERTYOP_INVOKE_SOCIAL_JUST_LOGIN_ACTIONS = { LIBERTYOP_PERFORM_SOCIAL_LOGIN };
    public static final String[] OPENSHIFT_INVOKE_SOCIAL_LOGIN_ACTIONS = { INVOKE_SOCIAL_RESOURCE, OPENSHIFT_PERFORM_SOCIAL_LOGIN };
    public static final String[] OPENSHIFT_INVOKE_SOCIAL_LOGIN_WITH_SELECTION_ACTIONS = { INVOKE_SOCIAL_RESOURCE, SELECT_PROVIDER, OPENSHIFT_PERFORM_SOCIAL_LOGIN };
    public static final String[] OPENSHIFT_INVOKE_SOCIAL_JUST_LOGIN_ACTIONS = { OPENSHIFT_PERFORM_SOCIAL_LOGIN };

    public static final String SOCIAL_CONFIG_STYLE_OIDC = "oidc";
    public static final String SOCIAL_CONFIG_STYLE_OAUTH = "oauth";

    public static final String SOCIAL_DEFAULT_CONTEXT_ROOT = "/ibm/api/social-login";
    public static final String SELECTION_TITLE = "Social Media Selection Form";

    public static final String BEARER_HEADER = "Authorization";
    public static final String X_FORWARDED_ACCESS_TOKEN_HEADER = "X-Forwarded-Access-Token";

    /***************** Facebook ********************/
    public static final String FACEBOOK_LOGIN = "facebookLogin";
    public static final String FACEBOOK_PROVIDER = "facebook";
    public static final String FACEBOOK_REALM = "https://www.facebook.com";
    public static final String FACEBOOK_LOGIN_TITLE = "Log into Facebook";
    public static final String FACEBOOK_USER1_ID = "ReplaceMe";
    public static final String FACEBOOK_USER1_PW = "ReplaceMe";
    public static final String FACEBOOK_USER1_EMAIL = "ReplaceMe";
    public static final String FACEBOOK_USER2_ID = "ReplaceMe";
    public static final String FACEBOOK_USER2_PW = "ReplaceMe";
    public static final String FACEBOOK_USER2_EMAIL = "ReplaceMe";
    public static final String FACEBOOK_USER3_ID = "ReplaceMe";
    public static final String FACEBOOK_USER3_PW = "ReplaceMe";
    public static final String FACEBOOK_USER3_EMAIL = "ReplaceMe";
    public static final String FACEBOOK_USER4_ID = "ReplaceMe";
    public static final String FACEBOOK_USER4_PW = "ReplaceMe";
    public static final String FACEBOOK_USER4_EMAIL = "ReplaceMe";
    public static final String FACEBOOK_DISPLAY_NAME = "Facebook";

    /***************** GitHub ********************/
    public static final String GITHUB_LOGIN = "githubLogin";
    public static final String GITHUB_PROVIDER = "GitHub";
    public static final String GITHUB_REALM = "https://github.com";
    public static final String GITHUB_LOGIN_TITLE = "Sign in to GitHub";
    public static final String GITHUB_USER1_ID = "";
    public static final String GITHUB_USER1_PW = "";
    public static final String GITHUB_USER1_EMAIL = "";
    public static final String GITHUB_USER2_ID = "ReplaceMe";
    public static final String GITHUB_USER2_PW = "ReplaceMe";
    public static final String GITHUB_USER2_EMAIL = "ReplaceMe";
    //    public static final String GITHUB_USER2_TOKEN = "ReplaceMe";
    //    public static final String GITHUB_USER2_PW = "ReplaceMe";
    public static final String GITHUB_USER3_ID = "";
    public static final String GITHUB_USER3_PW = "";
    public static final String GITHUB_USER3_EMAIL = "";
    public static final String GITHUB_USER4_ID = "";
    public static final String GITHUB_USER4_PW = "";
    public static final String GITHUB_USER4_EMAIL = "";
    public static final String GITHUB_DISPLAY_NAME = "GitHub";

    /***************** Twitter ********************/
    public static final String TWITTER_LOGIN = "twitterLogin";
    public static final String TWITTER_PROVIDER = "Twitter";
    public static final String TWITTER_SERVER_XML_PREFIX = "server_twitter_basicConfigTests_usingTwitterConfig";
    public static final String TWITTER_REALM = "https://api.twitter.com";
    public static final String TWITTER_LOGIN_AND_AUTHORIZE_TITLE = "Twitter / Authorize an application";
    public static final String TWITTER_LOGIN_TITLE = "Log in";
    public static final String TWITTER_AUTHORIZE_TITLE = TWITTER_LOGIN_AND_AUTHORIZE_TITLE; // authorize only title is the same as login and authorize
    //    public static final String TWITTER_AUTHORIZE_BUTTON_NAME = "Authorize";
    public static final String TWITTER_AUTHORIZE_BUTTON_NAME = "AuTh in";
    public static final String TWITTER_LOGIN_BUTTON_NAME = " Log in ";
    public static final String TWITTER_SIGN_IN_BUTTON_NAME = "Sign In";
    public static final String TWITTER_USER1_ID = "ReplaceMe";
    public static final String TWITTER_USER1_PW = "ReplaceMe";
    public static final String TWITTER_USER1_EMAIL = "ReplaceMe";
    public static final String TWITTER_CONSUMER_KEY = "consumerKey";
    //    public static final String TWITTER_CONSUMER_SECRET = "consumerSecret";
    public static final String TWITTER_DISPLAY_NAME = "Twitter";

    /***************** LinkedIn ********************/
    public static final String LINKEDIN_LOGIN = "linkedinLogin";
    public static final String LINKEDIN_PROVIDER = "LinkedIn";
    public static final String LINKEDIN_SERVER_XML_PREFIX = "server_linkedin_basicConfigTests_usingLinkedinConfig";
    public static final String LINKEDIN_REALM = "https://www.linkedin.com";
    public static final String LINKEDIN_LOGIN_AND_AUTHORIZE_TITLE = "Authorize | LinkedIn";
    public static final String LINKEDIN_LOGIN_TITLE = "Sign In to LinkedIn";
    public static final String LINKEDIN_FAILED_LOGIN_TITLE = "Sign Up | LinkedIn";
    //    public static final String LINKEDIN_AUTHORIZE_TITLE = LINKEDIN_LOGIN_AND_AUTHORIZE_TITLE; // authorize only title is the same as login and authorize
    public static final String LINKEDIN_AUTHORIZE_BUTTON_NAME = "AuTh in";
    public static final String LINKEDIN_LOGIN_BUTTON_NAME = "Sign In";
    public static final String LINKEDIN_SIGN_IN_BUTTON_NAME = "Sign In";
    public static final String LINKEDIN_USER1_ID = "ReplaceMe";
    public static final String LINKEDIN_USER1_PW = "ReplaceMe";
    public static final String LINKEDIN_USER1_EMAIL = "ReplaceMe";
    public static final String LINKEDIN_DISPLAY_NAME = "Linkedin";
    //    public static final String LINKEDIN_CONSUMER_KEY = "consumerKey";
    //    public static final String LINKEDIN_CONSUMER_SECRET = "consumerSecret";

    /***************** OpenShift ********************/
    public static final String OPENSHIFT_LOGIN = "openshiftLogin";
    public static final String OPENSHIFT_PROVIDER = "OpenShift";
    public static final String OPENSHIFT_REALM = "Needs_to_be_overridden_in_bootstrap.properties";
    public static final String OPENSHIFT_LOGIN_TITLE = "Needs_to_be_overridden_in_bootstrap.properties";
    public static final String OPENSHIFT_USER1_ID = "admin";
    public static final String OPENSHIFT_USER1_PW = "admin";
    public static final String OPENSHIFT_USER1_EMAIL = "ReplaceMe";
    public static final String OPENSHIFT_USER2_ID = "Needs_to_be_overridden_in_bootstrap.properties";
    public static final String OPENSHIFT_USER2_PW = "Needs_to_be_overridden_in_bootstrap.properties";
    public static final String OPENSHIFT_USER2_EMAIL = "ReplaceMe";
    public static final String OPENSHIFT_USER3_ID = "ReplaceMe";
    public static final String OPENSHIFT_USER3_PW = "ReplaceMe";
    public static final String OPENSHIFT_USER3_EMAIL = "ReplaceMe";
    public static final String OPENSHIFT_USER4_ID = "ReplaceMe";
    public static final String OPENSHIFT_USER4_PW = "ReplaceMe";
    public static final String OPENSHIFT_USER4_EMAIL = "ReplaceMe";
    public static final String OPENSHIFT_DISPLAY_NAME = "Facebook";
    /**
     * final HtmlForm form = page.getForms().get(0);
     * final HtmlSubmitInput button = form.getInputByValue("Sign In");
     * final HtmlTextInput emailBtn = form.getInputByName("session_key");
     * final HtmlPasswordInput passBtn = form.getInputByName("session_password");
     *
     */

    /***************** LibertyOP ********************/
    public static final String OAUTH_1_LOGIN = "oauthLogin";
    public static final String OAUTH_2_LOGIN = "oauth2Login";
    public static final String OIDC_LOGIN = "oidcLogin";
    public static final String LIBERTYOP_PROVIDER = "LibertyOP";
    public static final String LIBERTYOP_REALM = "BasicRealm";
    //    public static final String LIBERTYOP_LOGIN_TITLE = "Sign in to GitHub";
    public static final String LIBERTYOP_USER1_ID = "";
    public static final String LIBERTYOP_USER1_PW = "";
    public static final String LIBERTYOP_USER1_EMAIL = "";
    public static final String LIBERTYOP_USER2_ID = "oidcLoginUser";
    public static final String LIBERTYOP_USER2_PW = "testuserpwd";
    public static final String LIBERTYOP_USER2_EMAIL = "oidcLoginUser";
    public static final String LIBERTYOP_USER3_ID = "";
    public static final String LIBERTYOP_USER3_PW = "";
    public static final String LIBERTYOP_USER3_EMAIL = "";
    public static final String LIBERTYOP_USER4_ID = "";
    public static final String LIBERTYOP_USER4_PW = "";
    public static final String LIBERTYOP_USER4_EMAIL = "";

}
