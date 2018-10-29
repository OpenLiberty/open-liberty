/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class SpringSecurityTests20 extends AbstractSpringTests {

    private static final String TEST_WITH_APPSECURITY = "testSpringSecurityWithAppSecurityEnabled";
    private static final String FEATURE_APP_SECURITY_2_0 = "appSecurity-2.0";

    @Override
    public Set<String> getFeatures() {
        HashSet<String> result = new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-3.1"));

        String methodName = testName.getMethodName();

        if (methodName != null && TEST_WITH_APPSECURITY.equals(methodName)) {
            result.add(FEATURE_APP_SECURITY_2_0);
        }
        return result;
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_BASE;
    }

    @After
    public void stopOverrideServer() throws Exception {
        super.stopServer();
    }

    @Test
    public void testSpringSecurityWithoutAppSecurityEnabled() throws Exception {
        testSpringSecurity();
    }

    @Test
    public void testSpringSecurityWithAppSecurityEnabled() throws Exception {
        testSpringSecurity();
    }

    private void testSpringSecurity() throws Exception {
        @SuppressWarnings("resource")
        WebClient webClient = new WebClient();
        HtmlPage loginPage = ((HtmlPage) webClient.getPage("http://" + server.getHostname() + ":" + EXPECTED_HTTP_PORT + "/hello"));
        //Spring security restricts access to hello page directly. It redirects to a login page which gives authorization to specific users only
        String url = loginPage.getUrl().toExternalForm();
        assertTrue("Url not redirected to login page: " + url, url.endsWith(EXPECTED_HTTP_PORT + "/login"));

        //Testing with correct credentials
        HtmlPage helloPage = signInWithCorrectCredentials(loginPage);

        //signout
        loginPage = signOut(helloPage);

        //Testing with wrong credentials
        signInWithWrongCredentials(loginPage);
    }

    private HtmlPage signInWithCorrectCredentials(HtmlPage loginPage) throws IOException {
        HtmlForm form = loginPage.getFormByName("login:form");
        form.getInputByName("username").setValueAttribute("user");
        form.getInputByName("password").setValueAttribute("password");
        HtmlPage helloPage = (HtmlPage) form.getInputByName("signIn").click();
        //Only authorized users are authenticated and provided access to hello page
        String url = helloPage.getUrl().toExternalForm();
        assertTrue("User not authenticated, hence not directed to hello page:" + url, url.endsWith(EXPECTED_HTTP_PORT + "/hello"));
        String body = helloPage.getBody().asText();
        assertTrue("Expected output not found:" + body, body.contains("Hello user!"));
        return helloPage;
    }

    private HtmlPage signOut(HtmlPage helloPage) throws IOException {
        HtmlForm form = helloPage.getFormByName("hello:form");
        HtmlPage loginPage = (HtmlPage) form.getInputByName("signOut").click();
        //signout button will redirect the page to login page
        String url = loginPage.getUrl().toExternalForm();
        assertTrue("Sign out not successful: " + url, url.endsWith(EXPECTED_HTTP_PORT + "/login?logout"));
        return loginPage;
    }

    private void signInWithWrongCredentials(HtmlPage loginPage) throws IOException {
        HtmlForm form = loginPage.getFormByName("login:form");
        form.getInputByName("username").setValueAttribute("user1");
        form.getInputByName("password").setValueAttribute("password1");
        HtmlPage helloPage = (HtmlPage) form.getInputByName("signIn").click();
        //Unauthorized users are not authenticated and access to the hello page is restricted
        String url = helloPage.getUrl().toExternalForm();
        assertTrue("Sign in not successful: " + url, url.endsWith(EXPECTED_HTTP_PORT + "/login?error"));
    }
}
