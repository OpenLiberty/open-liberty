/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package mpapp1;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/MPConfigServlet")
public class MPConfigServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @Inject
    MPConfigBean reqScopeBean;

    @Inject
    MPConfigBeanWithApplicationScope appScopeBean;

    @Inject
    private ApplicationScopedOnCheckpointBean appScopeOnCheckpointBean;

    @Inject
    ApplicationScopedOnCheckpointBeanWithConfigObject appScopeOnCheckpointWithConfigObject;

    @Inject
    ApplicationScopedOnCheckpointBeanWithConfigObjectProperties appScopeOnCheckpointWithConfigObjectProperties;

    // MPConfigBean
    @Test
    public void defaultValueTest() {
        reqScopeBean.defaultValueTest();
    }

    @Test
    public void envValueTest() {
        reqScopeBean.envValueTest();
    }

    @Test
    public void providerEnvValueTest() {
        reqScopeBean.providerEnvValueTest();
    }

    @Test
    public void serverValueTest() {
        reqScopeBean.serverValueTest();
    }

    @Test
    public void annoValueTest() {
        reqScopeBean.annoValueTest();
    }

    @Test
    public void varDirValueTest() {
        reqScopeBean.varDirValueTest();
    }

    @Test
    public void noDefaultEnvValueTest() {
        reqScopeBean.noDefaultEnvValueTest();
    }

    @Test
    public void noDefaultServerValueTest() {
        reqScopeBean.noDefaultServerValueTest();
    }

    // MPconfigBeanWithApplicationScope
    @Test
    public void appScopeDefaultValueTest() {
        appScopeBean.appScopeDefaultValueTest();
    }

    @Test
    public void appScopeEnvValueTest() {
        appScopeBean.appScopeEnvValueTest();
    }

    @Test
    public void appScopeProviderEnvValueTest() {
        appScopeBean.appScopeProviderEnvValueTest();
    }

    @Test
    public void appScopeServerValueTest() {
        appScopeBean.appScopeServerValueTest();
    }

    @Test
    public void appScopeAnnoValueTest() {
        appScopeBean.appScopeAnnoValueTest();
    }

    @Test
    public void appScopeVarDirValueTest() {
        appScopeBean.varDirValueTest();
    }

    @Test
    public void appScopeNoDefaultEnvValueTest() {
        appScopeBean.appScopeNoDefaultEnvValueTest();
    }

    @Test
    public void appScopeNoDefaultServerValueTest() {
        appScopeBean.appScopeNoDefaultServerValueTest();
    }

    // ApplicationScopedOnCheckpointBeanWithConfigObject
    @Test
    public void configObjectAppScopeDefaultValueTest() {
        appScopeOnCheckpointWithConfigObject.appScopeDefaultValueTest();
    }

    @Test
    public void configObjectAppScopeEnvValueTest() {
        appScopeOnCheckpointWithConfigObject.appScopeEnvValueTest();
    }

    @Test
    public void configObjectAppScopeServerValueTest() {
        appScopeOnCheckpointWithConfigObject.appScopeServerValueTest();
    }

    @Test
    public void configObjectAppScopeAnnoValueTest() {
        appScopeOnCheckpointWithConfigObject.appScopeAnnoValueTest();
    }

    // ApplicationScopedOnCheckpointBeanWithConfigObjectProperties
    @Test
    public void configObjectPropertiesAppScopeDefaultValueTest() {
        appScopeOnCheckpointWithConfigObjectProperties.appScopeDefaultValueTest();
    }

    @Test
    public void configObjectPropertiesAppScopeEnvValueTest() {
        appScopeOnCheckpointWithConfigObjectProperties.appScopeEnvValueTest();
    }

    @Test
    public void configObjectPropertiesAppScopeServerValueTest() {
        appScopeOnCheckpointWithConfigObjectProperties.appScopeServerValueTest();
    }

    // ApplicationScopedOnCheckpointBean
    @Test
    public void appScopeEarlyAccessValueTest() {
        appScopeOnCheckpointBean.appScopedValueTest();
    }

    @Test
    public void appScopeEarlyAccessNoDefaultEnvValueTest() {
        appScopeOnCheckpointBean.appScopeEarlyAccessNoDefaultEnvValueTest();
    }

    @Test
    public void appScopeEarlyAccessNoDefaultServerValueTest() {
        appScopeOnCheckpointBean.appScopeEarlyAccessNoDefaultServerValueTest();
    }

    @Test
    public void appScopeEarlyAccessNoDefaultProviderEnvValueTest() {
        appScopeOnCheckpointBean.appScopeEarlyAccessNoDefaultProviderEnvValueTest();
    }

    @Test
    public void appScopeEarlyAccessNoDefaultProviderServerValueTest() {
        appScopeOnCheckpointBean.appScopeEarlyAccessNoDefaultProviderServerValueTest();
    }
}
