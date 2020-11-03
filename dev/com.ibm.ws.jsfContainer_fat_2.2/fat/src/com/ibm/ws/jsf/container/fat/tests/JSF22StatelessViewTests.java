/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat.tests;

import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf.container.fat.FATSuite;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import junit.framework.Assert;

@RunWith(FATRunner.class)
public class JSF22StatelessViewTests extends FATServletClient {

    private static final String MOJARRA_APP = "JSF22StatelessView";
    private static final String MYFACES_APP = "JSF22StatelessView_MyFaces";

    @Server("jsf.container.2.2_fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive mojarraApp = ShrinkWrap.create(WebArchive.class, MOJARRA_APP + ".war")
                        .addPackage("jsf.view.beans");
        mojarraApp = FATSuite.addMojarra(mojarraApp);
        mojarraApp = (WebArchive) ShrinkHelper.addDirectory(mojarraApp, "test-applications/" + MOJARRA_APP + "/resources");
        ShrinkHelper.exportToServer(server, "dropins", mojarraApp);
        server.addInstalledAppForValidation(MOJARRA_APP);

        WebArchive myfacesApp = ShrinkWrap.create(WebArchive.class, MYFACES_APP + ".war")
                        .addPackage("jsf.view.beans");
        myfacesApp = FATSuite.addMyFaces(myfacesApp);
        myfacesApp = (WebArchive) ShrinkHelper.addDirectory(myfacesApp, "test-applications/" + MOJARRA_APP + "/resources");
        ShrinkHelper.exportToServer(server, "dropins", myfacesApp);
        server.addInstalledAppForValidation(MYFACES_APP);

        server.startServer();
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        server.stopServer();
    }

    @Test
    public void verifyAppProviders() throws Exception {
        server.resetLogMarks();
        server.waitForStringInLogUsingMark("Initializing Mojarra .* for context '/" + MOJARRA_APP + "'");
        server.resetLogMarks();
        server.waitForStringInLogUsingMark("MyFaces CDI support enabled");
    }

    @Test
    public void JSF22StatelessView_TestSimpleStatelessView_Mojarra() throws Exception {
        JSF22StatelessView_TestSimpleStatelessView(MOJARRA_APP);
    }

    @Test
    public void JSF22StatelessView_TestSimpleStatelessView_MyFaces() throws Exception {
        JSF22StatelessView_TestSimpleStatelessView(MYFACES_APP);
    }

    /**
     * Check to make sure that a transient view renders with the correct viewstate value
     */
    private void JSF22StatelessView_TestSimpleStatelessView(String app) throws Exception {
        WebClient webClient = new WebClient();

        HtmlPage page = (HtmlPage) webClient.getPage(getServerURL() + app + "/JSF22StatelessView_Simple.xhtml");

        if (page == null) {
            Assert.fail("JSF22StatelessView_Simple.xhtml did not render properly.");
        }

        assertTrue(page.asText().contains("Testing JSF2.2 stateless views"));

        // Look for the correct View value in the output page.
        if (!checkIsViewStateless(page)) {
            Assert.fail("The view did not render as stateless"
                        + page.asXml());
        }
    }

    @Test
    public void JSF22StatelessView_TestIsTransientTrue_Mojarra() throws Exception {
        JSF22StatelessView_TestIsTransientTrue(MOJARRA_APP);
    }

    @Test
    public void JSF22StatelessView_TestIsTransientTrue_MyFaces() throws Exception {
        JSF22StatelessView_TestIsTransientTrue(MYFACES_APP);
    }

    /**
     * Programmatically tests if a view is marked as transient via UIViewRoot.isTransient() and
     * ResponseStateManager.isStateless() methods. In this case, transient=true.
     */
    private void JSF22StatelessView_TestIsTransientTrue(String app) throws Exception {
        WebClient webClient = new WebClient();

        HtmlPage page = (HtmlPage) webClient.getPage(getServerURL() + app + "/JSF22StatelessView_isTransient_true.xhtml");
        // Make sure the page initially renders correctly
        if (page == null) {
            Assert.fail("JSF22StatelessView_isTransient_true.xhtml did not render properly.");
        }
        assertTrue(page.asText().contains("This page programmatically queries the FacesContext to find out if the enclosing view is marked as transient."));

        // Look for the correct View value in the output page.
        if (!checkIsViewStateless(page)) {
            Assert.fail("The view did not render as stateless"
                        + page.asXml());
        }

        // Click the commandButton to execute the methods and update the page
        HtmlElement button = (HtmlElement) page.getElementById("button:test");
        page = button.click();

        String statelessText = "isTransient returns true and isStateless returns true";
        HtmlElement output = (HtmlElement) page.getElementById("testOutput");

        // Look for the correct results from isTransient() and isStateless()
        // They should return true here.
        if (!page.asText().contains(statelessText)) {
            Assert.fail("The transient setting is not reported correctly via isTransient() and isStateless()"
                        + output.asText());
        }
    }

    @Test
    public void JSF22StatelessView_TestIsTransientFalse_Mojarra() throws Exception {
        JSF22StatelessView_TestIsTransientFalse(MOJARRA_APP);
    }

    @Test
    public void JSF22StatelessView_TestIsTransientFalse_MyFaces() throws Exception {
        JSF22StatelessView_TestIsTransientFalse(MYFACES_APP);
    }

    /**
     * Programmatically tests if a view is marked as transient via UIViewRoot.isTransient() and
     * ResponseStateManager.isStateless() methods. In this case, transient=false.
     */
    private void JSF22StatelessView_TestIsTransientFalse(String app) throws Exception {
        WebClient webClient = new WebClient();

        HtmlPage page = (HtmlPage) webClient.getPage(getServerURL() + app + "/JSF22StatelessView_isTransient_false.xhtml");
        // Make sure the page initially renders correctly
        if (page == null) {
            Assert.fail("JSF22StatelessView_isTransient_false.xhtml did not render properly.");
        }
        assertTrue(page.asText().contains("This page programmatically queries the FacesContext to find out if the enclosing view is marked as transient."));

        // Look for the correct View value in the output page.
        if (checkIsViewStateless(page)) {
            Assert.fail("The view did not render as stateless"
                        + page.asXml());
        }

        // Click the commandButton to execute the methods and update the page
        HtmlElement button = (HtmlElement) page.getElementById("button:test");
        page = button.click();

        String statelessText = "isTransient returns false and isStateless returns false";
        HtmlElement output = (HtmlElement) page.getElementById("testOutput");

        // Look for the correct results from isTransient() and isStateless()
        // They should return false here.
        if (!output.asText().contains(statelessText)) {
            Assert.fail("The transient setting is not reported correctly via isTransient() and isStateless()"
                        + page.asText());
        }
    }

    @Test
    public void JSF22StatelessView_TestIsTransientDefault_Mojarra() throws Exception {
        JSF22StatelessView_TestIsTransientDefault(MOJARRA_APP);
    }

    @Test
    public void JSF22StatelessView_TestIsTransientDefault_MyFaces() throws Exception {
        JSF22StatelessView_TestIsTransientDefault(MYFACES_APP);
    }

    /**
     * Programmatically tests if a view is marked as transient via UIViewRoot.isTransient() and
     * ResponseStateManager.isStateless() methods. In this case, transient is undefined; the default is false.
     */
    private void JSF22StatelessView_TestIsTransientDefault(String app) throws Exception {
        WebClient webClient = new WebClient();

        HtmlPage page = (HtmlPage) webClient.getPage(getServerURL() + app + "/JSF22StatelessView_isTransient_default.xhtml");
        // Make sure the page initially renders correctly
        if (page == null) {
            Assert.fail("JSF22StatelessView_isTransient_default.xhtml did not render properly.");
        }
        assertTrue(page.asText().contains("This page programmatically queries the FacesContext to find out if the enclosing view is marked as transient."));

        // Look for the correct View value in the output page.
        if (checkIsViewStateless(page)) {
            Assert.fail("The view rendered as stateless when it shouldn't have!"
                        + page.asXml());
        }

        // Click the commandButton to execute the methods and update the page
        HtmlElement button = (HtmlElement) page.getElementById("button:test");
        page = button.click();

        String statelessText = "isTransient returns false and isStateless returns false";
        HtmlElement output = (HtmlElement) page.getElementById("testOutput");

        // Look for the correct results from isTransient() and isStateless()
        // They should return false here.
        if (!output.asText().contains(statelessText)) {
            Assert.fail("The transient setting is not reported correctly via isTransient() and isStateless()"
                        + page.asText());
        }
    }

    /**
     * Checks the behavior of a ViewScoped ManagedBean, when embedded in a stateless view.
     * Since the view here is stateless, the ViewScoped bean should be re-initialized on every submit.
     */
    @Test
    public void JSF22StatelessView_TestViewScopeManagedBeanTransient_Mojarra() throws Exception {
        testViewScopeManagedBeanTransient(MOJARRA_APP, "/JSF22StatelessView_ViewScope_Transient.xhtml");
    }

    @Test
    public void JSF22StatelessView_TestViewScopeManagedBeanTransient_MyFaces() throws Exception {
        testViewScopeManagedBeanTransient(MYFACES_APP, "/JSF22StatelessView_ViewScope_Transient.xhtml");
    }

    /**
     * Checks the behavior of a ViewScoped ManagedBean, when embedded in a stateless view.
     * Since the view here is NOT stateless, the ViewScoped bean should persist through a submit.
     */
    @Test
    public void JSF22StatelessView_TestViewScopeManagedBeanNotTransient_Mojarra() throws Exception {
        testViewScopeManagedBeanNotTransient(MOJARRA_APP, "/JSF22StatelessView_ViewScope_NotTransient.xhtml");
    }

    @Test
    public void JSF22StatelessView_TestViewScopeManagedBeanNotTransient_MyFaces() throws Exception {
        testViewScopeManagedBeanNotTransient(MYFACES_APP, "/JSF22StatelessView_ViewScope_NotTransient.xhtml");
    }

    /**
     * Checks the behavior of a ViewScoped CDI bean, when embedded in a stateless view.
     * Since the view here is stateless, the ViewScoped bean should be re-initialized on every submit.
     */
    @Test
    public void JSF22StatelessView_TestViewScopeCDIBeanTransient_Mojarra() throws Exception {
        testViewScopeManagedBeanTransient(MOJARRA_APP, "/JSF22StatelessView_ViewScope_CDI_Transient.xhtml");
    }

    @Test
    public void JSF22StatelessView_TestViewScopeCDIBeanTransient_MyFaces() throws Exception {
        testViewScopeManagedBeanTransient(MYFACES_APP, "/JSF22StatelessView_ViewScope_CDI_Transient.xhtml");
    }

    /**
     * Checks the behavior of a ViewScoped CDI bean, when embedded in a stateless view.
     * Since the view here is NOT stateless, the ViewScoped bean should persist through a submit.
     */
    @Test
    public void JSF22StatelessView_TestViewScopeCDIBeanNotTransient_Mojarra() throws Exception {
        testViewScopeManagedBeanNotTransient(MOJARRA_APP, "/JSF22StatelessView_ViewScope_CDI_NotTransient.xhtml");
    }

    @Test
    public void JSF22StatelessView_TestViewScopeCDIBeanNotTransient_MyFaces() throws Exception {
        testViewScopeManagedBeanNotTransient(MYFACES_APP, "/JSF22StatelessView_ViewScope_CDI_NotTransient.xhtml");
    }

    private void testViewScopeManagedBeanTransient(String app, String part) throws Exception {
        WebClient webClient = new WebClient();

        HtmlPage page = (HtmlPage) webClient.getPage(getServerURL() + app + part);
        // Make sure the page initially renders correctly
        if (page == null) {
            Assert.fail(part + " did not render properly.");
        }
        assertTrue(page.asText().contains("This page tests the behavior of a viewscoped bean in a stateless JSF22 view."));

        // Look for the correct View value in the output page.
        if (!checkIsViewStateless(page)) {
            Assert.fail("The view did not render as stateless"
                        + page.asXml());
        }

        HtmlElement timestamp = (HtmlElement) page.getElementById("timestamp");
        String initialTime = timestamp.asText();

        // Click the commandButton to execute the methods and update the page
        HtmlElement button = (HtmlElement) page.getElementById("button:test");
        page = button.click();

        timestamp = (HtmlElement) page.getElementById("timestamp");
        String newTime = timestamp.asText();

        // Compare the initial and final timestamps.
        // Since the enclosing view is stateless, the times should be different.
        if (initialTime.toString().equals(newTime.toString())) {
            Assert.fail("The ViewScoped bean was not re-initialized when it should have been - "
                        + "the initial and final timestamps are the same: "
                        + initialTime + " == " + newTime);
        }
    }

    private void testViewScopeManagedBeanNotTransient(String app, String part) throws Exception {
        WebClient webClient = new WebClient();

        HtmlPage page = (HtmlPage) webClient.getPage(getServerURL() + app + part);
        // Make sure the page initially renders correctly
        if (page == null) {
            Assert.fail(part + " did not render properly.");
        }
        assertTrue(page.asText().contains("This page tests the behavior of a viewscoped bean in a stateless JSF22 view."));

        // Look for the correct View value in the output page.
        if (checkIsViewStateless(page)) {
            Assert.fail("The view rendered as stateless when it shouldn't have!"
                        + page.asXml());
        }

        HtmlElement timestamp = (HtmlElement) page.getElementById("timestamp");
        String initialTime = timestamp.asText();

        // Click the commandButton to execute the methods and update the page
        HtmlElement button = (HtmlElement) page.getElementById("button:test");
        page = button.click();

        timestamp = (HtmlElement) page.getElementById("timestamp");
        String newTime = timestamp.asText();

        // Compare the initial and final timestamps.
        // Since the enclosing view is stateless, the times should be different.
        if (!initialTime.toString().equals(newTime.toString())) {
            Assert.fail("The ViewScoped bean was re-initialized when it shouldn't have been - "
                        + "the initial and final timestamps are different: "
                        + initialTime + " != " + newTime);
        }
    }

    /**
     * Checks to see if any stateless (transient="true") views exist in the given page
     */
    private Boolean checkIsViewStateless(HtmlPage page) {
        String statelessText = "value=\"stateless\"";
        // Look for the correct View value in the output page.
        // Fail if the expected value is not found.
        if (!page.asXml().contains(statelessText)) {
            return false;
        } else
            return true;
    }

    private static String getServerURL() {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + '/';
    }

}
