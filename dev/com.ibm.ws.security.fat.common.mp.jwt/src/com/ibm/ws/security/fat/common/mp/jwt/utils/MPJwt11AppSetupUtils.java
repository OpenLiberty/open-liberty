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
package com.ibm.ws.security.fat.common.mp.jwt.utils;

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt11FatConstants;

import componenttest.topology.impl.LibertyServer;

public class MPJwt11AppSetupUtils extends MPJwtAppSetupUtils {

    protected static Class<?> thisClass = MPJwt11AppSetupUtils.class;

    /*******************************************************/
    public void deployMicroProfileClientApp(LibertyServer server) throws Exception {
        ShrinkHelper.exportAppToServer(server, getMicroProfileClientApp());
        server.addInstalledAppForValidation(MPJwt11FatConstants.LOGINCONFIG_PROPAGATION_ROOT_CONTEXT);
    }

    public WebArchive getMicroProfileClientApp() throws Exception {
        return ShrinkWrap.create(WebArchive.class, MPJwt11FatConstants.LOGINCONFIG_PROPAGATION_ROOT_CONTEXT + ".war")
                        .addClass("com.ibm.ws.jaxrs.fat.microProfileApp.CommonPropMicroProfileMarker")
                        .addClass("com.ibm.ws.jaxrs.fat.microProfileApp.PropagationClient.MicroProfileApp")
                        .add(new FileAsset(new File("test-applications/microProfilePropagationClient.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
    }

    /**
     * create app with loginConfig set to Form Login in WEB.xml, and Basic in the App
     *
     * @param server
     * @throws Exception
     */
    public void deployMicroProfileLoginConfigFormLoginInWebXmlBasicInApp(LibertyServer server) throws Exception {
        List<String> classList = createAppClassListBuildAppNames("CommonMicroProfileMarker_FormLoginInWeb_BasicInApp", "MicroProfileLoginConfigFormLoginInWebXmlBasicInApp");
        ShrinkHelper.exportAppToServer(server, genericCreateArchiveWithJsps(MPJwt11FatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT, classList));
        server.addInstalledAppForValidation(MPJwt11FatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT);
    }

    public void deployMicroProfileLoginConfigFormLoginInWebXmlMPJWTInApp(LibertyServer server) throws Exception {
        List<String> classList = createAppClassListBuildAppNames("CommonMicroProfileMarker_FormLoginInWebXml_MpJwtInApp", "MicroProfileLoginConfigFormLoginInWebXmlMPJWTInApp");
        ShrinkHelper.exportAppToServer(server, genericCreateArchiveWithJsps(MPJwt11FatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT, classList));
        server.addInstalledAppForValidation(MPJwt11FatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT);

    }

    public void deployMicroProfileLoginConfigFormLoginInWebXmlNotInApp(LibertyServer server) throws Exception {
        List<String> classList = createAppClassListBuildAppNames("CommonMicroProfileMarker_FormLoginInWebXml_NotInApp", "MicroProfileLoginConfigFormLoginInWebXmlNotInApp");
        ShrinkHelper.exportAppToServer(server, genericCreateArchiveWithJsps(MPJwt11FatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT, classList));
        server.addInstalledAppForValidation(MPJwt11FatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT);
    }

    public void deployMicroProfileLoginConfigMpJwtInWebXmlBasicInApp(LibertyServer server) throws Exception {
        List<String> classList = createAppClassListBuildAppNames("CommonMicroProfileMarker_MpJwtInWebXml_BasicInApp", "MicroProfileLoginConfigMpJwtInWebXmlBasicInApp");
        ShrinkHelper.exportAppToServer(server, genericCreateArchiveWithoutJsps(MPJwt11FatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT, classList));
        server.addInstalledAppForValidation(MPJwt11FatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT);
    }

    public void deployMicroProfileLoginConfigMpJwtInWebXmlMPJWTInApp(LibertyServer server) throws Exception {
        List<String> classList = createAppClassListBuildAppNames("CommonMicroProfileMarker_MpJwtInWebXml_MpJwtInApp", "MicroProfileLoginConfigMpJwtInWebXmlMPJWTInApp");
        ShrinkHelper.exportAppToServer(server, genericCreateArchiveWithoutJsps(MPJwt11FatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT, classList));
        server.addInstalledAppForValidation(MPJwt11FatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT);
    }

    public void deployMicroProfileLoginConfigMpJwtInWebXmlNotInApp(LibertyServer server) throws Exception {
        List<String> classList = createAppClassListBuildAppNames("CommonMicroProfileMarker_MpJwtInWebXml_NotInApp", "MicroProfileLoginConfigMpJwtInWebXmlNotInApp");
        ShrinkHelper.exportAppToServer(server, genericCreateArchiveWithoutJsps(MPJwt11FatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT, classList));
        server.addInstalledAppForValidation(MPJwt11FatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT);
    }

    public void deployMicroProfileLoginConfigNotInWebXmlBasicInApp(LibertyServer server) throws Exception {
        List<String> classList = createAppClassListBuildAppNames("CommonMicroProfileMarker_NotInWebXml_BasicInApp", "MicroProfileLoginConfigNotInWebXmlBasicInApp");
        ShrinkHelper.exportAppToServer(server, genericCreateArchiveWithoutJsps(MPJwt11FatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT, classList));
        server.addInstalledAppForValidation(MPJwt11FatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT);
    }

    public void deployMicroProfileLoginConfigNotInWebXmlMPJWTInApp(LibertyServer server) throws Exception {
        List<String> classList = createAppClassListBuildAppNames("CommonMicroProfileMarker_NotInWebXml_MpJwtInApp", "MicroProfileLoginConfigNotInWebXmlMPJWTInApp");
        ShrinkHelper.exportAppToServer(server, genericCreateArchiveWithoutJsps(MPJwt11FatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT, classList));
        server.addInstalledAppForValidation(MPJwt11FatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT);
    }

    public void deployMicroProfileLoginConfigNotInWebXmlNotInApp(LibertyServer server) throws Exception {
        List<String> classList = createAppClassListBuildAppNames("CommonMicroProfileMarker_NotInWebXml_NotInApp", "MicroProfileLoginConfigNotInWebXmlNotInApp");
        ShrinkHelper.exportAppToServer(server, genericCreateArchiveWithoutJsps(MPJwt11FatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT, classList));
        server.addInstalledAppForValidation(MPJwt11FatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT);
    }

    public void deployMicroProfileLoginConfigMultiLayerNotInWebXmlMPJWTInApp(LibertyServer server) throws Exception {
        List<String> classList = createAppClassListBuildAppNames("CommonMicroProfileMarker_MultiLayer", "MicroProfileLoginConfigMultiLayerNotInWebXmlMPJWTInApp", "Intermediate");
        ShrinkHelper.exportAppToServer(server,
                                       genericCreateArchiveWithoutJsps(MPJwt11FatConstants.LOGINCONFIG_MULTI_LAYER_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT, classList));
        server.addInstalledAppForValidation(MPJwt11FatConstants.LOGINCONFIG_MULTI_LAYER_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT);
    }

}
