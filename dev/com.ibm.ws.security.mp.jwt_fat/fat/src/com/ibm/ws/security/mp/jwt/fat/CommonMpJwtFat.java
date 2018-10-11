/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.fat;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.JwtTokenTools;
import com.ibm.ws.security.fat.common.servers.ServerBootstrapUtils;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt.fat.utils.MpJwtFatActions;
import com.ibm.ws.security.mp.jwt.fat.utils.MpJwtMessageConstants;
import com.ibm.ws.security.openidconnect.token.PayloadConstants;

import componenttest.topology.impl.LibertyServer;

public class CommonMpJwtFat extends CommonSecurityFat {

    protected static ServerBootstrapUtils bootstrapUtils = new ServerBootstrapUtils();
    protected final MpJwtFatActions actions = new MpJwtFatActions();

    protected final String defaultUser = MpJwtFatConstants.TESTUSER;
    protected final String defaultPassword = MpJwtFatConstants.TESTUSERPWD;

    protected static void setUpAndStartBuilderServer(LibertyServer server, String configFile) throws Exception {
        setUpAndStartBuilderServer(server, configFile, false);
    }

    protected static void setUpAndStartBuilderServer(LibertyServer server, String configFile, boolean jwtEnabled) throws Exception {
        bootstrapUtils.writeBootstrapProperty(server, "oidcJWKEnabled", String.valueOf(jwtEnabled));
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration(configFile);
        saveServerPorts(server, MpJwtFatConstants.BVT_SERVER_2_PORT_NAME_ROOT);
    }

    protected static void deployRSClientApps(LibertyServer server) throws Exception {
        deployMicroProfileClientApp(server);
    }

    protected static void deployRSServerPropagationApps(LibertyServer server) throws Exception {
        deployMicroProfileLoginConfigNotInWebXmlMPJWTInApp(server);
    }

    protected static void deployRSServerLoginConfigApps(LibertyServer server) throws Exception {
        deployMicroProfileLoginConfigFormLoginInWebXmlBasicInApp(server);
        deployMicroProfileLoginConfigFormLoginInWebXmlMPJWTInApp(server);
        deployMicroProfileLoginConfigFormLoginInWebXmlNotInApp(server);
        deployMicroProfileLoginConfigMpJwtInWebXmlBasicInApp(server);
        deployMicroProfileLoginConfigMpJwtInWebXmlMPJWTInApp(server);
        deployMicroProfileLoginConfigMpJwtInWebXmlNotInApp(server);
        deployMicroProfileLoginConfigNotInWebXmlBasicInApp(server);
        deployMicroProfileLoginConfigNotInWebXmlMPJWTInApp(server);
        deployMicroProfileLoginConfigNotInWebXmlNotInApp(server);
        deployMicroProfileLoginConfigMultiLayerNotInWebXmlMPJWTInApp(server);

    }

    protected static void deployRSServerApiTestApps(LibertyServer server) throws Exception {
        deployMicroProfileApp(server);

    }

    /*******************************************************/
    protected static void deployMicroProfileClientApp(LibertyServer server) throws Exception {
        ShrinkHelper.exportAppToServer(server, getMicroProfileClientApp());
        server.addInstalledAppForValidation(MpJwtFatConstants.LOGINCONFIG_PROPAGATION_ROOT_CONTEXT);
    }

    private static void deployMicroProfileApp(LibertyServer server) throws Exception {
        List<String> classList = new ArrayList<String>();
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjection.ApplicationScoped.Instance.MicroProfileApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjection.NotScoped.MicroProfileApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjection.RequestScoped.MicroProfileApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjection.SessionScoped.Instance.MicroProfileApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjectionAllTypesMicroProfileApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.ClaimInjectionInstanceMicroProfileApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.CommonMicroProfileMarker");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.Injection.ApplicationScoped.MicroProfileApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.Injection.NotScoped.MicroProfileApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.Injection.RequestScoped.MicroProfileApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.Injection.SessionScoped.MicroProfileApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.JsonWebTokenInjectionMicroProfileApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.SecurityContext.ApplicationScoped.MicroProfileApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.SecurityContext.NotScoped.MicroProfileApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.SecurityContext.RequestScoped.MicroProfileApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.SecurityContext.SessionScoped.MicroProfileApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.SecurityContextMicroProfileApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.Utils");
        ShrinkHelper.exportAppToServer(server, genericCreateArchiveWithJsps(MpJwtFatConstants.MICROPROFILE_SERVLET, classList));
        server.addInstalledAppForValidation(MpJwtFatConstants.MICROPROFILE_SERVLET);

    }

    private static WebArchive getMicroProfileClientApp() throws Exception {
        return ShrinkWrap.create(WebArchive.class, MpJwtFatConstants.LOGINCONFIG_PROPAGATION_ROOT_CONTEXT + ".war")
                        .addClass("com.ibm.ws.jaxrs.fat.microProfileApp.CommonPropMicroProfileMarker")
                        .addClass("com.ibm.ws.jaxrs.fat.microProfileApp.PropagationClient.MicroProfileApp")
                        .add(new FileAsset(new File("test-applications/microProfilePropagationClient.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
    }

    protected static void deployMicroProfileLoginConfigFormLoginInWebXmlBasicInApp(LibertyServer server) throws Exception {
        ShrinkHelper.exportAppToServer(server, genericCreateArchiveWithJsps(MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT,
                                                                            "CommonMicroProfileMarker_FormLoginInWeb_BasicInApp",
                                                                            "MicroProfileLoginConfigFormLoginInWebXmlBasicInApp"));
        server.addInstalledAppForValidation(MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT);

    }

    protected static void deployMicroProfileLoginConfigFormLoginInWebXmlMPJWTInApp(LibertyServer server) throws Exception {
        ShrinkHelper.exportAppToServer(server, genericCreateArchiveWithJsps(MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT,
                                                                            "CommonMicroProfileMarker_FormLoginInWebXml_MpJwtInApp",
                                                                            "MicroProfileLoginConfigFormLoginInWebXmlMPJWTInApp"));
        server.addInstalledAppForValidation(MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT);

    }

    protected static void deployMicroProfileLoginConfigFormLoginInWebXmlNotInApp(LibertyServer server) throws Exception {
        ShrinkHelper.exportAppToServer(server, genericCreateArchiveWithJsps(MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT,
                                                                            "CommonMicroProfileMarker_FormLoginInWebXml_NotInApp",
                                                                            "MicroProfileLoginConfigFormLoginInWebXmlNotInApp"));
        server.addInstalledAppForValidation(MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT);
    }

    protected static void deployMicroProfileLoginConfigMpJwtInWebXmlBasicInApp(LibertyServer server) throws Exception {
        ShrinkHelper.exportAppToServer(server, genericCreateArchive(MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT,
                                                                    "CommonMicroProfileMarker_MpJwtInWebXml_BasicInApp", "MicroProfileLoginConfigMpJwtInWebXmlBasicInApp"));
        server.addInstalledAppForValidation(MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT);
    }

    protected static void deployMicroProfileLoginConfigMpJwtInWebXmlMPJWTInApp(LibertyServer server) throws Exception {
        ShrinkHelper.exportAppToServer(server, genericCreateArchive(MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT,
                                                                    "CommonMicroProfileMarker_MpJwtInWebXml_MpJwtInApp", "MicroProfileLoginConfigMpJwtInWebXmlMPJWTInApp"));
        server.addInstalledAppForValidation(MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT);

    }

    protected static void deployMicroProfileLoginConfigMpJwtInWebXmlNotInApp(LibertyServer server) throws Exception {
        ShrinkHelper.exportAppToServer(server, genericCreateArchive(MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT,
                                                                    "CommonMicroProfileMarker_MpJwtInWebXml_NotInApp", "MicroProfileLoginConfigMpJwtInWebXmlNotInApp"));
        server.addInstalledAppForValidation(MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT);

    }

    protected static void deployMicroProfileLoginConfigNotInWebXmlBasicInApp(LibertyServer server) throws Exception {
        ShrinkHelper.exportAppToServer(server, genericCreateArchive(MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT,
                                                                    "CommonMicroProfileMarker_NotInWebXml_BasicInApp", "MicroProfileLoginConfigNotInWebXmlBasicInApp"));
        server.addInstalledAppForValidation(MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT);

    }

    protected static void deployMicroProfileLoginConfigNotInWebXmlMPJWTInApp(LibertyServer server) throws Exception {
        ShrinkHelper.exportAppToServer(server, genericCreateArchive(MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT,
                                                                    "CommonMicroProfileMarker_NotInWebXml_MpJwtInApp", "MicroProfileLoginConfigNotInWebXmlMPJWTInApp"));
        server.addInstalledAppForValidation(MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT);
    }

    protected static void deployMicroProfileLoginConfigNotInWebXmlNotInApp(LibertyServer server) throws Exception {
        ShrinkHelper.exportAppToServer(server, genericCreateArchive(MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT,
                                                                    "CommonMicroProfileMarker_NotInWebXml_NotInApp", "MicroProfileLoginConfigNotInWebXmlNotInApp"));
        server.addInstalledAppForValidation(MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT);

    }

    protected static void deployMicroProfileLoginConfigMultiLayerNotInWebXmlMPJWTInApp(LibertyServer server) throws Exception {
        ShrinkHelper.exportAppToServer(server, genericCreateArchive(MpJwtFatConstants.LOGINCONFIG_MULTI_LAYER_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT,
                                                                    "CommonMicroProfileMarker_MultiLayer", "MicroProfileLoginConfigMultiLayerNotInWebXmlMPJWTInApp",
                                                                    "Intermediate"));

        server.addInstalledAppForValidation(MpJwtFatConstants.LOGINCONFIG_MULTI_LAYER_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT);

    }

    private static WebArchive genericCreateArchive(String baseWarName, String app1, String app2) throws Exception {
        try {
            String warName = baseWarName + ".war";
            return ShrinkWrap.create(WebArchive.class, warName)
                            .addClass("com.ibm.ws.jaxrs.fat.microProfileApp." + app1)
                            .addClass("com.ibm.ws.jaxrs.fat.microProfileApp." + app2 + ".MicroProfileApp")
                            .add(new FileAsset(new File("build/classes/com/ibm/ws/security/jwt/fat/mpjwt/CommonMicroProfileApp.class")),
                                 "com/ibm/ws/security/jwt/fat/mpjwt/CommonMicroProfileApp.class")
                            .add(new FileAsset(new File("build/classes/com/ibm/ws/security/jwt/fat/mpjwt/MpJwtFatConstants.class")),
                                 "com/ibm/ws/security/jwt/fat/mpjwt/MpJwtFatConstants.class")
                            .add(new FileAsset(new File("test-applications/" + warName + "/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
        } catch (Exception e) {
            Log.error(thisClass, "genericCreateArchive", e);
            throw e;
        }
    }

    private static WebArchive genericCreateArchive(String baseWarName, String app1, String app2, String app3) throws Exception {
        try {
            String warName = baseWarName + ".war";
            return ShrinkWrap.create(WebArchive.class, warName)
                            .addClass("com.ibm.ws.jaxrs.fat.microProfileApp." + app1)
                            .addClass("com.ibm.ws.jaxrs.fat.microProfileApp." + app2 + ".MicroProfileApp")
                            .addClass("com.ibm.ws.jaxrs.fat.microProfileApp." + app2 + ".MicroProfileApp" + app3)
                            .add(new FileAsset(new File("build/classes/com/ibm/ws/security/jwt/fat/mpjwt/CommonMicroProfileApp.class")),
                                 "com/ibm/ws/security/jwt/fat/mpjwt/CommonMicroProfileApp.class")
                            .add(new FileAsset(new File("build/classes/com/ibm/ws/security/jwt/fat/mpjwt/MpJwtFatConstants.class")),
                                 "com/ibm/ws/security/jwt/fat/mpjwt/MpJwtFatConstants.class")
                            .add(new FileAsset(new File("test-applications/" + warName + "/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
        } catch (Exception e) {
            Log.error(thisClass, "genericCreateArchive", e);
            throw e;
        }
    }

    protected static WebArchive genericCreateArchive(String baseWarName, List<String> classList) throws Exception {
        try {
            String warName = baseWarName + ".war";
            WebArchive newWar = ShrinkWrap.create(WebArchive.class, warName)
                            .add(new FileAsset(new File("build/classes/com/ibm/ws/security/jwt/fat/mpjwt/CommonMicroProfileApp.class")),
                                 "com/ibm/ws/security/jwt/fat/mpjwt/CommonMicroProfileApp.class")
                            .add(new FileAsset(new File("build/classes/com/ibm/ws/security/jwt/fat/mpjwt/MpJwtFatConstants.class")),
                                 "com/ibm/ws/security/jwt/fat/mpjwt/MpJwtFatConstants.class")
                            .add(new FileAsset(new File("test-applications/" + warName + "/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                            .add(new FileAsset(new File("test-applications/" + warName + "/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml");
            for (String theClass : classList) {
                newWar.addClass(theClass);
            }
            return newWar;
        } catch (Exception e) {
            Log.error(thisClass, "genericCreateArchive", e);
            throw e;
        }
    }

    private static WebArchive genericCreateArchiveWithJsps(String baseWarName, String app1, String app2) throws Exception {
        try {
            String warName = baseWarName + ".war";
            return ShrinkWrap.create(WebArchive.class, warName)
                            .addClass("com.ibm.ws.jaxrs.fat.microProfileApp." + app1)
                            .addClass("com.ibm.ws.jaxrs.fat.microProfileApp." + app2 + ".MicroProfileApp")
                            .add(new FileAsset(new File("build/classes/com/ibm/ws/security/jwt/fat/mpjwt/CommonMicroProfileApp.class")),
                                 "com/ibm/ws/security/jwt/fat/mpjwt/CommonMicroProfileApp.class")
                            .add(new FileAsset(new File("build/classes/com/ibm/ws/security/jwt/fat/mpjwt/MpJwtFatConstants.class")),
                                 "com/ibm/ws/security/jwt/fat/mpjwt/MpJwtFatConstants.class")
                            .add(new FileAsset(new File("test-applications/" + warName + "/resources/login.jsp")), "/login.jsp")
                            .add(new FileAsset(new File("test-applications/" + warName + "/resources/loginError.jsp")), "/loginError.jsp")
                            .add(new FileAsset(new File("test-applications/" + warName + "/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
        } catch (Exception e) {
            Log.error(thisClass, "genericCreateArchive", e);
            throw e;
        }
    }

    protected static WebArchive genericCreateArchiveWithJsps(String baseWarName, List<String> classList) throws Exception {
        try {
            String warName = baseWarName + ".war";
            WebArchive newWar = ShrinkWrap.create(WebArchive.class, warName)
                            .add(new FileAsset(new File("build/classes/com/ibm/ws/security/jwt/fat/mpjwt/CommonMicroProfileApp.class")),
                                 "com/ibm/ws/security/jwt/fat/mpjwt/CommonMicroProfileApp.class")
                            .add(new FileAsset(new File("build/classes/com/ibm/ws/security/jwt/fat/mpjwt/MpJwtFatConstants.class")),
                                 "com/ibm/ws/security/jwt/fat/mpjwt/MpJwtFatConstants.class")
                            .add(new FileAsset(new File("test-applications/" + warName + "/resources/login.jsp")), "/login.jsp")
                            .add(new FileAsset(new File("test-applications/" + warName + "/resources/loginError.jsp")), "/loginError.jsp")
                            .add(new FileAsset(new File("test-applications/" + warName + "/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                            .add(new FileAsset(new File("test-applications/" + warName + "/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml");
            for (String theClass : classList) {
                newWar.addClass(theClass);
            }
            return newWar;
        } catch (Exception e) {
            Log.error(thisClass, "genericCreateArchive", e);
            throw e;
        }
    }

    protected static void saveServerPorts(LibertyServer server, String propertyNameRoot) throws Exception {
        server.setBvtPortPropertyName(propertyNameRoot);
        server.setBvtSecurePortPropertyName(propertyNameRoot + ".secure");
        Log.info(thisClass, "setUp", server.getServerName() + " ports are: " + server.getBvtPort() + " " + server.getBvtSecurePort());

    }

    public static InetAddress getServerIdentity() throws UnknownHostException {
        InetAddress addr = InetAddress.getLocalHost();
        return addr;
    }

    public static String getServerHostName() throws Exception {
        return getServerIdentity().getHostName();
    }

    public static String getServerCanonicalHostName() throws Exception {
        return getServerIdentity().getCanonicalHostName();
    }

    public static String getServerHostIp() throws Exception {
        return getServerIdentity().toString().split("/")[1];
    }

    public static String getServerUrlBase(LibertyServer server) {
        return "http://" + server.getHostname() + ":" + server.getBvtPort() + "/";
    }

    public static String getServerSecureUrlBase(LibertyServer server) {
        return "https://" + server.getHostname() + ":" + server.getBvtSecurePort() + "/";
    }

    public static String getServerIpUrlBase(LibertyServer server) throws Exception {
        return "http://" + getServerHostIp() + ":" + server.getBvtPort() + "/";
    }

    public static String getServerIpSecureUrlBase(LibertyServer server) throws Exception {
        return "https://" + getServerHostIp() + ":" + server.getBvtSecurePort() + "/";
    }

    public Expectations goodAppExpectations(String testAction, String theUrl, String theClass) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(testAction, theUrl));
        expectations.addExpectation(new ResponseFullExpectation(testAction, MpJwtFatConstants.STRING_CONTAINS, theClass, "Did not invoke the app " + theClass + "."));

        return expectations;
    }

    public Expectations badAppExpectations(String testAction, String errorMessage) throws Exception {

        // TODO - need to add specific message checks
        Expectations expectations = new Expectations();
        //        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(testAction, theUrl));
        expectations.addExpectation(new ResponseStatusExpectation(testAction, HttpServletResponse.SC_UNAUTHORIZED));
        //        expectations.addExpectation(new ExceptionMessageExpectation(testAction, MpJwtFatConstants.STRING_MATCHES, errorMessage, "Did not find the error message: " + errorMessage));

        return expectations;
    }

    public String getDefaultJwtToken(LibertyServer server) throws Exception {
        String builtToken = actions.getJwtFromTokenEndpoint(_testName, "defaultJWT", getServerSecureUrlBase(server), defaultUser, defaultPassword);
        Log.info(thisClass, _testName, "JWT Token: " + builtToken);
        return builtToken;
    }

    public String getJwtTokenUsingBuilder(LibertyServer server) throws Exception {

        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair(JwtConstants.PARAM_UPN, defaultUser));
        return getJwtTokenUsingBuilder(server, "defaultJWT_withAudience", extraClaims);
    }

    public String getJwtTokenUsingBuilder(LibertyServer server, String builderId) throws Exception {
        List<NameValuePair> extraClaims = new ArrayList<NameValuePair>();
        extraClaims.add(new NameValuePair(JwtConstants.PARAM_UPN, defaultUser));
        return getJwtTokenUsingBuilder(server, builderId, extraClaims);
    }

    // anyone calling this method needs to add upn to the extraClaims that it passes in (if they need it)
    public String getJwtTokenUsingBuilder(LibertyServer server, List<NameValuePair> extraClaims) throws Exception {
        return getJwtTokenUsingBuilder(server, "defaultJWT_withAudience", extraClaims);
    }

    // anyone calling this method needs to add upn to the extraClaims that it passes in (if they need it)
    public String getJwtTokenUsingBuilder(LibertyServer server, String builderId, List<NameValuePair> extraClaims) throws Exception {

        String jwtBuilderUrl = getServerUrlBase(server) + "/jwtbuilder/build";

        List<NameValuePair> requestParams = setRequestParms(builderId, extraClaims);

        WebClient webClient = new WebClient();
        Page response = actions.invokeUrlWithParameters(_testName, webClient, jwtBuilderUrl, requestParams);
        Log.info(thisClass, _testName, "JWT builder app response: " + WebResponseUtils.getResponseText(response));

        Cookie jwtCookie = webClient.getCookieManager().getCookie("JWT");
        Log.info(thisClass, _testName, "Built JWT cookie: " + jwtCookie);
        Log.info(thisClass, _testName, "Cookie value: " + jwtCookie.getValue());
        return jwtCookie.getValue();

    }

    public List<NameValuePair> setRequestParms(String builderId, List<NameValuePair> extraClaims) throws Exception {

        List<NameValuePair> requestParms = new ArrayList<NameValuePair>();
        requestParms.add(new NameValuePair(JwtConstants.PARAM_BUILDER_ID, builderId));
        if (extraClaims != null) {
            for (NameValuePair claim : extraClaims) {
                Log.info(thisClass, "setRequestParm", "Setting: " + claim.getName() + " value: " + claim.getValue());
                requestParms.add(new NameValuePair(claim.getName(), claim.getValue()));
            }
        }
        return requestParms;
    }

    public static String buildAppUrl(LibertyServer theServer, String root, String app) throws Exception {

        return getServerUrlBase(theServer) + root + "/rest/" + app + "/" + MpJwtFatConstants.MPJWT_GENERIC_APP_NAME;

    }

    public static String buildAppSecureUrl(LibertyServer theServer, String root, String app) throws Exception {

        return getServerSecureUrlBase(theServer) + root + "/rest/" + app + "/" + MpJwtFatConstants.MPJWT_GENERIC_APP_NAME;

    }

    public Expectations goodTestExpectations(JwtTokenTools jwtTokenTools, String testAction, String theUrl, String theClass) throws Exception {
        String AppFailedCheckMsg = "Values DO NOT Match --------";

        try {
            Expectations expectations = new Expectations();
            expectations.addExpectations(CommonExpectations.successfullyReachedUrl(testAction, theUrl));
            expectations.addExpectation(new ResponseFullExpectation(testAction, MpJwtFatConstants.STRING_CONTAINS, theClass, "Did not invoke the app " + theClass + "."));
            expectations.addExpectation(new ResponseFullExpectation(testAction, MpJwtFatConstants.STRING_DOES_NOT_CONTAIN, AppFailedCheckMsg, "Response contained string \""
                                                                                                                                              + AppFailedCheckMsg
                                                                                                                                              + "\" which indicates that injected claim valued obtained via different means did NOT match"));
            Log.info(thisClass, "goodTestExpectations", "right before questionable expectations");
            expectations.addExpectations(addClaimExpectations(jwtTokenTools, testAction, theClass));

            return expectations;
        } catch (Exception e) {
            Log.info(thisClass, "goodTestExpectations", "Failed building expectations: " + e.getMessage());
            throw e;
        }
    }

    public Expectations addClaimExpectations(JwtTokenTools jwtTokenTools, String testAction, String theClass) throws Exception {
        try {
            Expectations expectations = new Expectations();
            if (!theClass.contains("ClaimInjection") || (theClass.contains("ClaimInjection") && theClass.contains("RequestScoped"))) {
                expectations.addExpectation(addApiOutputExpectation(testAction, "getRawToken", MpJwtFatConstants.MP_JWT_TOKEN, null, jwtTokenTools.getJwtTokenString()));
                expectations.addExpectations(addApiOutputExpectation(testAction, jwtTokenTools, "getIssuer", MpJwtFatConstants.JWT_BUILDER_ISSUER, PayloadConstants.ISSUER));
                expectations.addExpectations(addApiOutputExpectation(testAction, jwtTokenTools, "getSubject", MpJwtFatConstants.JWT_BUILDER_SUBJECT, PayloadConstants.SUBJECT));
                expectations.addExpectations(addApiOutputExpectation(testAction, jwtTokenTools, "getTokenID", MpJwtFatConstants.JWT_BUILDER_JWTID, PayloadConstants.JWTID));
                expectations.addExpectations(addApiOutputExpectation(testAction, jwtTokenTools, "getExpirationTime", MpJwtFatConstants.JWT_BUILDER_EXPIRATION,
                                                                     PayloadConstants.EXPIRATION_TIME_IN_SECS));
                expectations.addExpectations(addApiOutputExpectation(testAction, jwtTokenTools, "getIssuedAtTime", MpJwtFatConstants.JWT_BUILDER_ISSUED_AT,
                                                                     PayloadConstants.ISSUED_AT_TIME_IN_SECS));
                expectations.addExpectations(addApiOutputExpectation(testAction, jwtTokenTools, "getAudience", MpJwtFatConstants.JWT_BUILDER_AUDIENCE, PayloadConstants.AUDIENCE));
                expectations.addExpectations(addApiOutputExpectation(testAction, jwtTokenTools, "getGroups", MpJwtFatConstants.PAYLOAD_GROUPS, "groups"));
                // we won't have a list of claims to check for ClaimInjection, we don't use the api to retrieve the claims and there is no injected claim that lists all claims...
                if (!theClass.contains("ClaimInjection")) {
                    for (String key : jwtTokenTools.getClaims()) {
                        expectations.addExpectations(addApiOutputExpectation(testAction, jwtTokenTools, "getClaim", MpJwtFatConstants.JWT_BUILDER_CLAIM, key));
                    }
                }
            }

            return expectations;
        } catch (Exception e) {
            Log.info(thisClass, "goodTestExpectations", "Failed building expectations: " + e.getMessage());
            throw e;
        }
    }

    public Expectations addApiOutputExpectation(String testAction, JwtTokenTools jwtTokenTools, String api, String keyword, String key) throws Exception {
        Expectations expectations = new Expectations();
        Log.info(thisClass, "addApiOutputExpectation", "Key requested: " + key);
        // syntax is a bit different for the "getClaim" results
        String passKeyName = null;
        if (api.contains("getClaim")) {
            passKeyName = key;
        }
        List<String> values = jwtTokenTools.getElementValueAsList(key);
        if (!values.isEmpty()) {
            for (String value : values) {
                expectations.addExpectation(addApiOutputExpectation(testAction, api, keyword, passKeyName, value));
            }
        } else {
            expectations.addExpectation(addApiOutputExpectation(testAction, api, keyword, passKeyName, "null"));
        }

        return expectations;
    }

    public ResponseFullExpectation addApiOutputExpectation(String testAction, String api, String keyword, String key, String value) throws Exception {
        Log.info(thisClass, "", "Parms: " + testAction + ", " + api + ", " + value + ", " + keyword + ", " + key);
        return new ResponseFullExpectation(testAction, MpJwtFatConstants.STRING_MATCHES, buildStringToCheck(keyword, key, value), "API " + api
                                                                                                                                  + " did NOT return the correct value (" + value
                                                                                                                                  + ").");

    }

    public String buildStringToCheck(String keyword, String key, String value) throws Exception {
        String builtString = keyword.trim();
        if (!keyword.contains(":")) {
            builtString = builtString + ":";
        }
        if (key != null) {
            builtString = builtString + " key: " + key + " value:.*" + value;
        } else {
            builtString = builtString + " " + value.replace("[", "\\[").replace("]", "\\]");
        }

        return builtString;

    }

    /**
     * Set expectations for tests that have bad issuers
     *
     * @return Expectations
     * @throws Exception
     */
    public Expectations setBadIssuerExpectations(LibertyServer server) throws Exception {

        String action = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(action, HttpServletResponse.SC_UNAUTHORIZED));

        expectations.addExpectation(new ServerMessageExpectation(action, server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain an error indicating a problem authenticating the request the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(action, server, MpJwtMessageConstants.CWWKS6022E_ISSUER_NOT_TRUSTED, "Messagelog did not contain an exception indicating that the issuer is NOT valid."));

        return expectations;

    }
}
