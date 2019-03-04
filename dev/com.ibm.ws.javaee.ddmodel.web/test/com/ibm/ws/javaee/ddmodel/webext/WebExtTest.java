/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.webext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Test;

import com.ibm.ws.javaee.dd.commonext.GlobalTransaction;
import com.ibm.ws.javaee.dd.commonext.LocalTransaction;
import com.ibm.ws.javaee.dd.commonext.ResourceRef;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.webext.Attribute;
import com.ibm.ws.javaee.dd.webext.CacheVariable;
import com.ibm.ws.javaee.dd.webext.IdGenerationProperties;
import com.ibm.ws.javaee.dd.webext.MarkupLanguage;
import com.ibm.ws.javaee.dd.webext.MimeFilter;
import com.ibm.ws.javaee.dd.webext.Page;
import com.ibm.ws.javaee.dd.webext.ServletCacheConfig;
import com.ibm.ws.javaee.dd.webext.ServletExtension;
import com.ibm.ws.javaee.dd.webext.WebExt;

public class WebExtTest extends WebExtTestBase {

    @Test
    public void testGetVersion() throws Exception {
        Assert.assertEquals("XMI", parseWebExtXMI(webAppExtension("") + "</webappext:WebAppExtension>",
                                                  parse(webApp24() + "</web-app>")).getVersion());
        Assert.assertEquals("Version should be 1.0", "1.0", parseWebExtXML(webExt10() + "</web-ext>").getVersion());
        Assert.assertEquals("Version should be 1.1", "1.1", parseWebExtXML(webExt11() + "</web-ext>").getVersion());
    }

    @Test
    public void testWebExt() throws Exception {
        WebExt webExt = parseWebExtXML(webExt10() + "</web-ext>");
        Assert.assertFalse(webExt.isSetReloadInterval());
        Assert.assertFalse(webExt.isSetContextRoot());
        Assert.assertNull(webExt.getContextRoot());
        Assert.assertFalse(webExt.isSetAutoloadFilters());
        Assert.assertFalse(webExt.isSetAutoEncodeRequests());
        Assert.assertFalse(webExt.isSetAutoEncodeResponses());
        Assert.assertFalse(webExt.isSetEnableDirectoryBrowsing());
        Assert.assertFalse(webExt.isSetEnableFileServing());
        Assert.assertFalse(webExt.isSetPreCompileJsps());
        Assert.assertFalse(webExt.isSetEnableReloading());
        Assert.assertFalse(webExt.isSetEnableServingServletsByClassName());

        webExt = parseWebExtXML(webExt10() +
                                "<reload-interval value=\"1234\"/>" +
                                "<context-root uri=\"cr\"/>" +
                                "<autoload-filters value=\"false\"/>" +
                                "<auto-encode-requests value=\"false\"/>" +
                                "<auto-encode-responses value=\"false\"/>" +
                                "<enable-directory-browsing value=\"false\"/>" +
                                "<enable-file-serving value=\"false\"/>" +
                                "<pre-compile-jsps value=\"false\"/>" +
                                "<enable-reloading value=\"false\"/>" +
                                "<enable-serving-servlets-by-class-name value=\"false\"/>" +
                                "</web-ext>");
        Assert.assertTrue(webExt.isSetReloadInterval());
        Assert.assertEquals(1234, webExt.getReloadInterval());
        Assert.assertTrue(webExt.isSetContextRoot());
        Assert.assertEquals("cr", webExt.getContextRoot());
        Assert.assertTrue(webExt.isSetAutoloadFilters());
        Assert.assertFalse(webExt.isAutoloadFilters());
        Assert.assertTrue(webExt.isSetAutoEncodeRequests());
        Assert.assertFalse(webExt.isAutoEncodeRequests());
        Assert.assertTrue(webExt.isSetAutoEncodeResponses());
        Assert.assertFalse(webExt.isAutoEncodeResponses());
        Assert.assertTrue(webExt.isSetEnableDirectoryBrowsing());
        Assert.assertFalse(webExt.isEnableDirectoryBrowsing());
        Assert.assertTrue(webExt.isSetEnableFileServing());
        Assert.assertFalse(webExt.isEnableFileServing());
        Assert.assertTrue(webExt.isSetPreCompileJsps());
        Assert.assertFalse(webExt.isPreCompileJsps());
        Assert.assertTrue(webExt.isSetEnableReloading());
        Assert.assertFalse(webExt.isEnableReloading());
        Assert.assertTrue(webExt.isSetEnableServingServletsByClassName());
        Assert.assertFalse(webExt.isEnableServingServletsByClassName());

        webExt = parseWebExtXML(webExt10() +
                                "<autoload-filters value=\"true\"/>" +
                                "<auto-encode-requests value=\"true\"/>" +
                                "<auto-encode-responses value=\"true\"/>" +
                                "<enable-directory-browsing value=\"true\"/>" +
                                "<enable-file-serving value=\"true\"/>" +
                                "<pre-compile-jsps value=\"true\"/>" +
                                "<enable-reloading value=\"true\"/>" +
                                "<enable-serving-servlets-by-class-name value=\"true\"/>" +
                                "</web-ext>");
        Assert.assertFalse(webExt.isSetReloadInterval());
        Assert.assertFalse(webExt.isSetContextRoot());
        Assert.assertNull(webExt.getContextRoot());
        Assert.assertTrue(webExt.isSetAutoloadFilters());
        Assert.assertTrue(webExt.isAutoloadFilters());
        Assert.assertTrue(webExt.isSetAutoEncodeRequests());
        Assert.assertTrue(webExt.isAutoEncodeRequests());
        Assert.assertTrue(webExt.isSetAutoEncodeResponses());
        Assert.assertTrue(webExt.isAutoEncodeResponses());
        Assert.assertTrue(webExt.isSetEnableDirectoryBrowsing());
        Assert.assertTrue(webExt.isEnableDirectoryBrowsing());
        Assert.assertTrue(webExt.isSetEnableFileServing());
        Assert.assertTrue(webExt.isEnableFileServing());
        Assert.assertTrue(webExt.isSetPreCompileJsps());
        Assert.assertTrue(webExt.isPreCompileJsps());
        Assert.assertTrue(webExt.isSetEnableReloading());
        Assert.assertTrue(webExt.isEnableReloading());
        Assert.assertTrue(webExt.isSetEnableServingServletsByClassName());
        Assert.assertTrue(webExt.isEnableServingServletsByClassName());
    }

    @Test
    public void testXMIWebExt() throws Exception {
        WebApp webApp = parse(webApp24() + "</web-app>");
        WebExt webExt = parseWebExtXMI(webAppExtension("") + "</webappext:WebAppExtension>", webApp);
        Assert.assertFalse(webExt.isSetReloadInterval());
        Assert.assertFalse(webExt.isSetContextRoot());
        Assert.assertNull(webExt.getContextRoot());
        Assert.assertFalse(webExt.isSetAutoloadFilters());
        Assert.assertFalse(webExt.isSetAutoEncodeRequests());
        Assert.assertFalse(webExt.isSetAutoEncodeResponses());
        Assert.assertFalse(webExt.isSetEnableDirectoryBrowsing());
        Assert.assertFalse(webExt.isSetEnableFileServing());
        Assert.assertFalse(webExt.isSetPreCompileJsps());
        Assert.assertFalse(webExt.isSetEnableReloading());
        Assert.assertFalse(webExt.isSetEnableServingServletsByClassName());

        webExt = parseWebExtXMI(webAppExtension("reloadInterval=\"1234\" " +
                                                "contextRoot=\"cr\" " +
                                                "autoLoadFilters=\"false\" " +
                                                "autoRequestEncoding=\"false\" " +
                                                "autoResponseEncoding=\"false\" " +
                                                "directoryBrowsingEnabled=\"false\" " +
                                                "fileServingEnabled=\"false\" " +
                                                "preCompileJSPs=\"false\" " +
                                                "reloadingEnabled=\"false\" " +
                                                "serveServletsByClassnameEnabled=\"false\"") +
                                "</webappext:WebAppExtension>", webApp);
        Assert.assertTrue(webExt.isSetReloadInterval());
        Assert.assertEquals(1234, webExt.getReloadInterval());
        Assert.assertTrue(webExt.isSetContextRoot());
        Assert.assertEquals("cr", webExt.getContextRoot());
        Assert.assertTrue(webExt.isSetAutoloadFilters());
        Assert.assertFalse(webExt.isAutoloadFilters());
        Assert.assertTrue(webExt.isSetAutoEncodeRequests());
        Assert.assertFalse(webExt.isAutoEncodeRequests());
        Assert.assertTrue(webExt.isSetAutoEncodeResponses());
        Assert.assertFalse(webExt.isAutoEncodeResponses());
        Assert.assertTrue(webExt.isSetEnableDirectoryBrowsing());
        Assert.assertFalse(webExt.isEnableDirectoryBrowsing());
        Assert.assertTrue(webExt.isSetEnableFileServing());
        Assert.assertFalse(webExt.isEnableFileServing());
        Assert.assertTrue(webExt.isSetPreCompileJsps());
        Assert.assertFalse(webExt.isPreCompileJsps());
        Assert.assertTrue(webExt.isSetEnableReloading());
        Assert.assertFalse(webExt.isEnableReloading());
        Assert.assertTrue(webExt.isSetEnableServingServletsByClassName());
        Assert.assertFalse(webExt.isEnableServingServletsByClassName());

        webExt = parseWebExtXMI(webAppExtension("autoLoadFilters=\"true\" " +
                                                "autoRequestEncoding=\"true\" " +
                                                "autoResponseEncoding=\"true\" " +
                                                "directoryBrowsingEnabled=\"true\" " +
                                                "fileServingEnabled=\"true\" " +
                                                "preCompileJSPs=\"true\" " +
                                                "reloadingEnabled=\"true\" " +
                                                "serveServletsByClassnameEnabled=\"true\"") +
                                "</webappext:WebAppExtension>", webApp);
        Assert.assertFalse(webExt.isSetReloadInterval());
        Assert.assertFalse(webExt.isSetContextRoot());
        Assert.assertNull(webExt.getContextRoot());
        Assert.assertTrue(webExt.isSetAutoloadFilters());
        Assert.assertTrue(webExt.isAutoloadFilters());
        Assert.assertTrue(webExt.isSetAutoEncodeRequests());
        Assert.assertTrue(webExt.isAutoEncodeRequests());
        Assert.assertTrue(webExt.isSetAutoEncodeResponses());
        Assert.assertTrue(webExt.isAutoEncodeResponses());
        Assert.assertTrue(webExt.isSetEnableDirectoryBrowsing());
        Assert.assertTrue(webExt.isEnableDirectoryBrowsing());
        Assert.assertTrue(webExt.isSetEnableFileServing());
        Assert.assertTrue(webExt.isEnableFileServing());
        Assert.assertTrue(webExt.isSetPreCompileJsps());
        Assert.assertTrue(webExt.isPreCompileJsps());
        Assert.assertTrue(webExt.isSetEnableReloading());
        Assert.assertTrue(webExt.isEnableReloading());
        Assert.assertTrue(webExt.isSetEnableServingServletsByClassName());
        Assert.assertTrue(webExt.isEnableServingServletsByClassName());
    }

    @Test
    public void testServlet() throws Exception {
        WebExt webExt = parseWebExtXML(webExt10() + "</web-ext>");
        Assert.assertEquals(Collections.emptyList(), webExt.getServletExtensions());

        webExt = parseWebExtXML(webExt10() +
                                "<servlet name=\"s0\"/>" +
                                "<servlet name=\"s1\"/>" +
                                "</web-ext>");
        List<ServletExtension> ses = webExt.getServletExtensions();
        Assert.assertEquals(ses.toString(), 2, ses.size());

        Assert.assertEquals("s0", ses.get(0).getName());
        Assert.assertEquals("s1", ses.get(1).getName());
    }

    @Test
    public void testXMIServlet() throws Exception {
        WebApp webApp = parse(webApp24() +
                              "<servlet id=\"s0id\">" +
                              "  <servlet-name>s0</servlet-name>" +
                              "</servlet>" +
                              "<servlet id=\"s1id\">" +
                              "  <servlet-name>s1</servlet-name>" +
                              "</servlet>" +
                              "</web-app>");
        WebExt webExt = parseWebExtXMI(webAppExtension("") + "</webappext:WebAppExtension>", webApp);
        Assert.assertEquals(Collections.emptyList(), webExt.getServletExtensions());

        webExt = parseWebExtXMI(webAppExtension("") +
                                "<extendedServlets>" +
                                "  <extendedServlet href=\"WEB-INF/web.xml#s0id\"/>" +
                                "</extendedServlets>" +
                                "<extendedServlets>" +
                                "  <extendedServlet href=\"WEB-INF/web.xml#s1id\"/>" +
                                "</extendedServlets>" +
                                "</webappext:WebAppExtension>", webApp);
        List<ServletExtension> ses = webExt.getServletExtensions();
        Assert.assertEquals(ses.toString(), 2, ses.size());

        Assert.assertEquals("s0", ses.get(0).getName());
        Assert.assertEquals("s1", ses.get(1).getName());
    }

    @Test
    public void testLocalTransaction() throws Exception {
        WebExt webExt = parseWebExtXML(webExt10() +
                                       "<servlet name=\"s0\"/>" +
                                       "<servlet name=\"s1\">" +
                                       "  <local-transaction/>" +
                                       "</servlet>" +
                                       "<servlet name=\"s2\">" +
                                       "  <local-transaction boundary=\"ACTIVITY_SESSION\" resolver=\"APPLICATION\" unresolved-action=\"ROLLBACK\" shareable=\"false\"/>" +
                                       "</servlet>" +
                                       "<servlet name=\"s3\">" +
                                       "  <local-transaction boundary=\"BEAN_METHOD\" resolver=\"CONTAINER_AT_BOUNDARY\" unresolved-action=\"COMMIT\" shareable=\"true\"/>" +
                                       "</servlet>" +
                                       "</web-ext>");
        List<ServletExtension> ses = webExt.getServletExtensions();

        Assert.assertNull(ses.get(0).getLocalTransaction());

        LocalTransaction lt = ses.get(1).getLocalTransaction();
        Assert.assertFalse(lt.isSetBoundary());
        Assert.assertNull(lt.getBoundary());
        Assert.assertFalse(lt.isSetResolver());
        Assert.assertNull(lt.getResolver());
        Assert.assertFalse(lt.isSetUnresolvedAction());
        Assert.assertNull(lt.getUnresolvedAction());
        Assert.assertFalse(lt.isSetShareable());

        lt = ses.get(2).getLocalTransaction();
        Assert.assertTrue(lt.isSetBoundary());
        Assert.assertEquals(LocalTransaction.BoundaryEnum.ACTIVITY_SESSION, lt.getBoundary());
        Assert.assertTrue(lt.isSetResolver());
        Assert.assertEquals(LocalTransaction.ResolverEnum.APPLICATION, lt.getResolver());
        Assert.assertTrue(lt.isSetUnresolvedAction());
        Assert.assertEquals(LocalTransaction.UnresolvedActionEnum.ROLLBACK, lt.getUnresolvedAction());
        Assert.assertTrue(lt.isSetShareable());
        Assert.assertFalse(lt.isShareable());

        lt = ses.get(3).getLocalTransaction();
        Assert.assertTrue(lt.isSetBoundary());
        Assert.assertEquals(LocalTransaction.BoundaryEnum.BEAN_METHOD, lt.getBoundary());
        Assert.assertTrue(lt.isSetResolver());
        Assert.assertEquals(LocalTransaction.ResolverEnum.CONTAINER_AT_BOUNDARY, lt.getResolver());
        Assert.assertTrue(lt.isSetUnresolvedAction());
        Assert.assertEquals(LocalTransaction.UnresolvedActionEnum.COMMIT, lt.getUnresolvedAction());
        Assert.assertTrue(lt.isSetShareable());
        Assert.assertTrue(lt.isShareable());
    }

    @Test
    public void testXMILocalTransaction() throws Exception {
        WebExt webExt = parseWebExtXMI(webAppExtension("") +
                                       "<extendedServlets/>" +
                                       "<extendedServlets>" +
                                       "  <localTransaction/>" +
                                       "</extendedServlets>" +
                                       "<extendedServlets>" +
                                       "  <localTransaction boundary=\"ActivitySession\" resolver=\"Application\" unresolvedAction=\"Rollback\" shareable=\"false\"/>" +
                                       "</extendedServlets>" +
                                       "<extendedServlets>" +
                                       "  <localTransaction boundary=\"BeanMethod\" resolver=\"ContainerAtBoundary\" unresolvedAction=\"Commit\" shareable=\"true\"/>" +
                                       "</extendedServlets>" +
                                       "</webappext:WebAppExtension>", parse(webApp24() + "</web-app>"));
        List<ServletExtension> ses = webExt.getServletExtensions();

        Assert.assertNull(ses.get(0).getLocalTransaction());

        LocalTransaction lt = ses.get(1).getLocalTransaction();
        Assert.assertFalse(lt.isSetBoundary());
        Assert.assertNull(lt.getBoundary());
        Assert.assertFalse(lt.isSetResolver());
        Assert.assertNull(lt.getResolver());
        Assert.assertFalse(lt.isSetUnresolvedAction());
        Assert.assertNull(lt.getUnresolvedAction());
        Assert.assertFalse(lt.isSetShareable());

        lt = ses.get(2).getLocalTransaction();
        Assert.assertTrue(lt.isSetBoundary());
        Assert.assertEquals(LocalTransaction.BoundaryEnum.ACTIVITY_SESSION, lt.getBoundary());
        Assert.assertTrue(lt.isSetResolver());
        Assert.assertEquals(LocalTransaction.ResolverEnum.APPLICATION, lt.getResolver());
        Assert.assertTrue(lt.isSetUnresolvedAction());
        Assert.assertEquals(LocalTransaction.UnresolvedActionEnum.ROLLBACK, lt.getUnresolvedAction());
        Assert.assertTrue(lt.isSetShareable());
        Assert.assertFalse(lt.isShareable());

        lt = ses.get(3).getLocalTransaction();
        Assert.assertTrue(lt.isSetBoundary());
        Assert.assertEquals(LocalTransaction.BoundaryEnum.BEAN_METHOD, lt.getBoundary());
        Assert.assertTrue(lt.isSetResolver());
        Assert.assertEquals(LocalTransaction.ResolverEnum.CONTAINER_AT_BOUNDARY, lt.getResolver());
        Assert.assertTrue(lt.isSetUnresolvedAction());
        Assert.assertEquals(LocalTransaction.UnresolvedActionEnum.COMMIT, lt.getUnresolvedAction());
        Assert.assertTrue(lt.isSetShareable());
        Assert.assertTrue(lt.isShareable());
    }

    @Test
    public void testGlobalTransaction() throws Exception {
        WebExt webExt = parseWebExtXML(webExt10() +
                                       "<servlet name=\"s0\"/>" +
                                       "<servlet name=\"s1\">" +
                                       "  <global-transaction/>" +
                                       "</servlet>" +
                                       "<servlet name=\"s2\">" +
                                       "  <global-transaction send-wsat-context=\"false\" transaction-time-out=\"1234\"/>" +
                                       "</servlet>" +
                                       "<servlet name=\"s3\">" +
                                       "  <global-transaction send-wsat-context=\"true\"/>" +
                                       "</servlet>" +
                                       "</web-ext>");
        List<ServletExtension> ses = webExt.getServletExtensions();

        Assert.assertNull(ses.get(0).getGlobalTransaction());

        GlobalTransaction gt = ses.get(1).getGlobalTransaction();
        Assert.assertFalse(gt.isSetSendWSATContext());
        Assert.assertFalse(gt.isSetTransactionTimeOut());

        gt = ses.get(2).getGlobalTransaction();
        Assert.assertTrue(gt.isSetSendWSATContext());
        Assert.assertFalse(gt.isSendWSATContext());
        Assert.assertTrue(gt.isSetTransactionTimeOut());
        Assert.assertEquals(1234, gt.getTransactionTimeOut());

        gt = ses.get(3).getGlobalTransaction();
        Assert.assertTrue(gt.isSetSendWSATContext());
        Assert.assertTrue(gt.isSendWSATContext());
    }

    @Test
    public void testXMIGlobalTransaction() throws Exception {
        WebExt webExt = parseWebExtXMI(webAppExtension("") +
                                       "<extendedServlets/>" +
                                       "<extendedServlets>" +
                                       "  <globalTransaction/>" +
                                       "</extendedServlets>" +
                                       "<extendedServlets>" +
                                       "  <globalTransaction sendWSAT=\"false\" componentTransactionTimeout=\"1234\"/>" +
                                       "</extendedServlets>" +
                                       "<extendedServlets>" +
                                       "  <globalTransaction sendWSAT=\"true\"/>" +
                                       "</extendedServlets>" +
                                       "</webappext:WebAppExtension>", parse(webApp24() + "</web-app>"));
        List<ServletExtension> ses = webExt.getServletExtensions();

        Assert.assertNull(ses.get(0).getGlobalTransaction());

        GlobalTransaction gt = ses.get(1).getGlobalTransaction();
        Assert.assertFalse(gt.isSetSendWSATContext());
        Assert.assertFalse(gt.isSetTransactionTimeOut());

        gt = ses.get(2).getGlobalTransaction();
        Assert.assertTrue(gt.isSetSendWSATContext());
        Assert.assertFalse(gt.isSendWSATContext());
        Assert.assertTrue(gt.isSetTransactionTimeOut());
        Assert.assertEquals(1234, gt.getTransactionTimeOut());

        gt = ses.get(3).getGlobalTransaction();
        Assert.assertTrue(gt.isSetSendWSATContext());
        Assert.assertTrue(gt.isSendWSATContext());
    }

    @Test
    public void testWebGlobalTransaction() throws Exception {
        WebExt webExt = parseWebExtXML(webExt10() +
                                       "<servlet name=\"s0\"/>" +
                                       "<servlet name=\"s1\">" +
                                       "  <web-global-transaction execute-using-wsat=\"false\" />" +
                                       "</servlet>" +
                                       "<servlet name=\"s2\">" +
                                       "  <web-global-transaction execute-using-wsat=\"true\" />" +
                                       "</servlet>" +
                                       "</web-ext>");
        List<ServletExtension> servlets = webExt.getServletExtensions();

        Assert.assertNull(servlets.get(0).getWebGlobalTransaction());
        Assert.assertFalse(servlets.get(1).getWebGlobalTransaction().isExecuteUsingWSAT());
        Assert.assertTrue(servlets.get(2).getWebGlobalTransaction().isExecuteUsingWSAT());
    }

    @Test
    public void testXMIWebGlobalTransaction() throws Exception {
        WebExt webExt = parseWebExtXMI(webAppExtension("") +
                                       "<extendedServlets/>" +
                                       "<extendedServlets>" +
                                       "  <webGlobalTransaction supportsWSAT=\"false\" />" +
                                       "</extendedServlets>" +
                                       "<extendedServlets>" +
                                       "  <webGlobalTransaction supportsWSAT=\"true\" />" +
                                       "</extendedServlets>" +
                                       "</webappext:WebAppExtension>", parse(webApp24() + "</web-app>"));
        List<ServletExtension> servlets = webExt.getServletExtensions();

        Assert.assertNull(servlets.get(0).getWebGlobalTransaction());
        Assert.assertFalse(servlets.get(1).getWebGlobalTransaction().isExecuteUsingWSAT());
        Assert.assertTrue(servlets.get(2).getWebGlobalTransaction().isExecuteUsingWSAT());
    }

    @Test
    public void testMarkupLanguage() throws Exception {
        WebExt webExt = parseWebExtXML(webExt10() +
                                       "<servlet name=\"s0\"/>" +
                                       "<servlet name=\"s1\">" +
                                       "  <markup-language name=\"n0\" mime-type=\"mt0\"/>" +
                                       "  <markup-language name=\"n1\" mime-type=\"mt1\" error-page=\"ep1\" default-page=\"dp1\">" +
                                       "    <page name=\"pn0\"/>" +
                                       "    <page name=\"pn1\" uri=\"pu1\"/>" +
                                       "  </markup-language>" +
                                       "</servlet>" +
                                       "</web-ext>");
        List<ServletExtension> ses = webExt.getServletExtensions();

        Assert.assertEquals(Collections.emptyList(), ses.get(0).getMarkupLanguages());

        List<MarkupLanguage> mls = ses.get(1).getMarkupLanguages();
        MarkupLanguage ml = mls.get(0);
        Assert.assertEquals("n0", ml.getName());
        Assert.assertEquals("mt0", ml.getMimeType());
        Assert.assertNull(ml.getErrorPage());
        Assert.assertNull(ml.getDefaultPage());
        Assert.assertEquals(Collections.emptyList(), ml.getPages());

        ml = mls.get(1);
        Assert.assertEquals("n1", ml.getName());
        Assert.assertEquals("mt1", ml.getMimeType());
        Assert.assertEquals("ep1", ml.getErrorPage());
        Assert.assertEquals("dp1", ml.getDefaultPage());

        List<Page> ps = ml.getPages();
        Page p = ps.get(0);
        Assert.assertEquals("pn0", p.getName());
        Assert.assertNull(p.getURI());

        p = ps.get(1);
        Assert.assertEquals("pn1", p.getName());
        Assert.assertEquals("pu1", p.getURI());
    }

    @Test
    public void testXMIMarkupLanguage() throws Exception {
        WebExt webExt = parseWebExtXMI(webAppExtension("") +
                                       "<extendedServlets/>" +
                                       "<extendedServlets>" +
                                       "  <markupLanguages name=\"n0\" mimeType=\"mt0\"/>" +
                                       "  <markupLanguages name=\"n1\" mimeType=\"mt1\" errorPage=\"ep1\" defaultPage=\"dp1\">" +
                                       "    <pages name=\"pn0\"/>" +
                                       "    <pages name=\"pn1\" uri=\"pu1\"/>" +
                                       "  </markupLanguages>" +
                                       "</extendedServlets>" +
                                       "</webappext:WebAppExtension>", parse(webApp24() + "</web-app>"));
        List<ServletExtension> ses = webExt.getServletExtensions();

        Assert.assertEquals(Collections.emptyList(), ses.get(0).getMarkupLanguages());

        List<MarkupLanguage> mls = ses.get(1).getMarkupLanguages();
        MarkupLanguage ml = mls.get(0);
        Assert.assertEquals("n0", ml.getName());
        Assert.assertEquals("mt0", ml.getMimeType());
        Assert.assertNull(ml.getErrorPage());
        Assert.assertNull(ml.getDefaultPage());
        Assert.assertEquals(Collections.emptyList(), ml.getPages());

        ml = mls.get(1);
        Assert.assertEquals("n1", ml.getName());
        Assert.assertEquals("mt1", ml.getMimeType());
        Assert.assertEquals("ep1", ml.getErrorPage());
        Assert.assertEquals("dp1", ml.getDefaultPage());

        List<Page> ps = ml.getPages();
        Page p = ps.get(0);
        Assert.assertEquals("pn0", p.getName());
        Assert.assertNull(p.getURI());

        p = ps.get(1);
        Assert.assertEquals("pn1", p.getName());
        Assert.assertEquals("pu1", p.getURI());
    }

    @Test
    public void testDefaultErrorPage() throws Exception {
        WebExt webExt = parseWebExtXML(webExt10() + "</web-ext>");
        Assert.assertNull(webExt.getDefaultErrorPage());

        webExt = parseWebExtXML(webExt10() +
                                "<default-error-page uri=\"dep\"/>" +
                                "</web-ext>");
        Assert.assertEquals("dep", webExt.getDefaultErrorPage());
    }

    @Test
    public void testXMIDefaultErrorPage() throws Exception {
        WebExt webExt = parseWebExtXMI(webAppExtension("") +
                                       "</webappext:WebAppExtension>", parse(webApp24() + "</web-app>"));
        Assert.assertNull(webExt.getDefaultErrorPage());

        webExt = parseWebExtXMI(webAppExtension("") +
                                "  <defaultErrorPage xsi:nil=\"true\"/>" +
                                "</webappext:WebAppExtension>", parse(webApp24() + "</web-app>"));
        Assert.assertNull(webExt.getDefaultErrorPage());

        webExt = parseWebExtXMI(webAppExtension("defaultErrorPage=\"dep\"") +
                                "</webappext:WebAppExtension>", parse(webApp24() + "</web-app>"));
        Assert.assertEquals("dep", webExt.getDefaultErrorPage());
    }

    @Test
    public void testFileServingAttribute() throws Exception {
        WebExt webExt = parseWebExtXML(webExt10() + "</web-ext>");
        Assert.assertEquals(Collections.emptyList(), webExt.getFileServingAttributes());

        webExt = parseWebExtXML(webExt10() +
                                "<file-serving-attribute name=\"n0\" value=\"v0\"/>" +
                                "<file-serving-attribute name=\"n1\" value=\"v1\"/>" +
                                "</web-ext>");
        List<Attribute> fsas = webExt.getFileServingAttributes();
        Assert.assertEquals(fsas.toString(), 2, fsas.size());

        Assert.assertEquals("n0", fsas.get(0).getName());
        Assert.assertEquals("v0", fsas.get(0).getValue());

        Assert.assertEquals("n1", fsas.get(1).getName());
        Assert.assertEquals("v1", fsas.get(1).getValue());
    }

    @Test
    public void testXMIFileServingAttribute() throws Exception {
        WebApp webApp = parse(webApp24() + "</web-app>");
        WebExt webExt = parseWebExtXMI(webAppExtension("") + "</webappext:WebAppExtension>", webApp);
        Assert.assertEquals(Collections.emptyList(), webExt.getFileServingAttributes());

        webExt = parseWebExtXMI(webAppExtension("") +
                                "<fileServingAttributes name=\"n0\" value=\"v0\"/>" +
                                "<fileServingAttributes name=\"n1\" value=\"v1\"/>" +
                                "</webappext:WebAppExtension>", webApp);
        List<Attribute> fsas = webExt.getFileServingAttributes();
        Assert.assertEquals(fsas.toString(), 2, fsas.size());

        Assert.assertEquals("n0", fsas.get(0).getName());
        Assert.assertEquals("v0", fsas.get(0).getValue());

        Assert.assertEquals("n1", fsas.get(1).getName());
        Assert.assertEquals("v1", fsas.get(1).getValue());
    }

    @Test
    public void testInvokerAttribute() throws Exception {
        WebExt webExt = parseWebExtXML(webExt10() + "</web-ext>");
        Assert.assertEquals(Collections.emptyList(), webExt.getInvokerAttributes());

        webExt = parseWebExtXML(webExt10() +
                                "<invoker-attribute name=\"n0\" value=\"v0\"/>" +
                                "<invoker-attribute name=\"n1\" value=\"v1\"/>" +
                                "</web-ext>");
        List<Attribute> ias = webExt.getInvokerAttributes();
        Assert.assertEquals(ias.toString(), 2, ias.size());

        Assert.assertEquals("n0", ias.get(0).getName());
        Assert.assertEquals("v0", ias.get(0).getValue());

        Assert.assertEquals("n1", ias.get(1).getName());
        Assert.assertEquals("v1", ias.get(1).getValue());
    }

    @Test
    public void testXMIInvokerAttribute() throws Exception {
        WebApp webApp = parse(webApp24() + "</web-app>");
        WebExt webExt = parseWebExtXMI(webAppExtension("") + "</webappext:WebAppExtension>", webApp);
        Assert.assertEquals(Collections.emptyList(), webExt.getInvokerAttributes());

        webExt = parseWebExtXMI(webAppExtension("") +
                                "<invokerAttributes name=\"n0\" value=\"v0\"/>" +
                                "<invokerAttributes name=\"n1\" value=\"v1\"/>" +
                                "</webappext:WebAppExtension>", webApp);
        List<Attribute> ias = webExt.getInvokerAttributes();
        Assert.assertEquals(ias.toString(), 2, ias.size());

        Assert.assertEquals("n0", ias.get(0).getName());
        Assert.assertEquals("v0", ias.get(0).getValue());

        Assert.assertEquals("n1", ias.get(1).getName());
        Assert.assertEquals("v1", ias.get(1).getValue());
    }

    @Test
    public void testJspAttribute() throws Exception {
        WebExt webExt = parseWebExtXML(webExt10() + "</web-ext>");
        Assert.assertEquals(Collections.emptyList(), webExt.getJspAttributes());

        webExt = parseWebExtXML(webExt10() +
                                "<jsp-attribute name=\"n0\" value=\"v0\"/>" +
                                "<jsp-attribute name=\"n1\" value=\"v1\"/>" +
                                "</web-ext>");
        List<Attribute> jas = webExt.getJspAttributes();
        Assert.assertEquals(jas.toString(), 2, jas.size());

        Assert.assertEquals("n0", jas.get(0).getName());
        Assert.assertEquals("v0", jas.get(0).getValue());

        Assert.assertEquals("n1", jas.get(1).getName());
        Assert.assertEquals("v1", jas.get(1).getValue());
    }

    @Test
    public void testXMIJspAttribute() throws Exception {
        WebApp webApp = parse(webApp24() + "</web-app>");
        WebExt webExt = parseWebExtXMI(webAppExtension("") + "</webappext:WebAppExtension>", webApp);
        Assert.assertEquals(Collections.emptyList(), webExt.getJspAttributes());

        webExt = parseWebExtXMI(webAppExtension("") +
                                "<jspAttributes name=\"n0\" value=\"v0\"/>" +
                                "<jspAttributes name=\"n1\" value=\"v1\"/>" +
                                "</webappext:WebAppExtension>", webApp);
        List<Attribute> jas = webExt.getJspAttributes();
        Assert.assertEquals(jas.toString(), 2, jas.size());

        Assert.assertEquals("n0", jas.get(0).getName());
        Assert.assertEquals("v0", jas.get(0).getValue());

        Assert.assertEquals("n1", jas.get(1).getName());
        Assert.assertEquals("v1", jas.get(1).getValue());
    }

    @Test
    public void testMimeFilter() throws Exception {
        WebExt webExt = parseWebExtXML(webExt10() + "</web-ext>");
        Assert.assertEquals(Collections.emptyList(), webExt.getMimeFilters());

        webExt = parseWebExtXML(webExt10() +
                                "<mime-filter target=\"t0\" mime-type=\"mt0\"/>" +
                                "<mime-filter target=\"t1\" mime-type=\"mt1\"/>" +
                                "</web-ext>");
        List<MimeFilter> mfs = webExt.getMimeFilters();
        Assert.assertEquals(mfs.toString(), 2, mfs.size());

        Assert.assertEquals("t0", mfs.get(0).getTarget());
        Assert.assertEquals("mt0", mfs.get(0).getMimeType());

        Assert.assertEquals("t1", mfs.get(1).getTarget());
        Assert.assertEquals("mt1", mfs.get(1).getMimeType());
    }

    @Test
    public void testXMIMimeFilter() throws Exception {
        WebApp webApp = parse(webApp24() + "</web-app>");
        WebExt webExt = parseWebExtXMI(webAppExtension("") + "</webappext:WebAppExtension>", webApp);
        Assert.assertEquals(Collections.emptyList(), webExt.getMimeFilters());

        webExt = parseWebExtXMI(webAppExtension("") +
                                "<mimeFilters target=\"t0\" type=\"mt0\"/>" +
                                "<mimeFilters target=\"t1\" type=\"mt1\"/>" +
                                "</webappext:WebAppExtension>", webApp);
        List<MimeFilter> mfs = webExt.getMimeFilters();
        Assert.assertEquals(mfs.toString(), 2, mfs.size());

        Assert.assertEquals("t0", mfs.get(0).getTarget());
        Assert.assertEquals("mt0", mfs.get(0).getMimeType());

        Assert.assertEquals("t1", mfs.get(1).getTarget());
        Assert.assertEquals("mt1", mfs.get(1).getMimeType());
    }

    @Test
    public void testResourceRef() throws Exception {
        WebExt webExt = parseWebExtXML(webExt10() + "</web-ext>");
        Assert.assertEquals(Collections.emptyList(), webExt.getResourceRefs());

        webExt = parseWebExtXML(webExt10() +
                                "<resource-ref name=\"n0\"/>" +
                                "<resource-ref name=\"n1\"" +
                                "  isolation-level=\"TRANSACTION_NONE\"" +
                                "  connection-management-policy=\"DEFAULT\"" +
                                "  commit-priority=\"1234\"" +
                                "  branch-coupling=\"LOOSE\"" +
                                "/>" +
                                "<resource-ref name=\"n2\"" +
                                "  isolation-level=\"TRANSACTION_READ_UNCOMMITTED\"" +
                                "  connection-management-policy=\"AGGRESSIVE\"" +
                                "  branch-coupling=\"TIGHT\"" +
                                "/>" +
                                "<resource-ref name=\"n3\"" +
                                "  isolation-level=\"TRANSACTION_READ_COMMITTED\"" +
                                "  connection-management-policy=\"NORMAL\"" +
                                "/>" +
                                "<resource-ref name=\"n4\" isolation-level=\"TRANSACTION_REPEATABLE_READ\"/>" +
                                "<resource-ref name=\"n5\" isolation-level=\"TRANSACTION_SERIALIZABLE\"/>" +
                                "</web-ext>");
        List<ResourceRef> rrs = webExt.getResourceRefs();
        Assert.assertEquals(rrs.toString(), 6, rrs.size());

        ResourceRef rr = rrs.get(0);
        Assert.assertEquals("n0", rr.getName());
        Assert.assertFalse(rr.isSetIsolationLevel());
        Assert.assertNull(rr.getIsolationLevel());
        Assert.assertFalse(rr.isSetConnectionManagementPolicy());
        Assert.assertNull(rr.getConnectionManagementPolicy());
        Assert.assertFalse(rr.isSetCommitPriority());
        Assert.assertFalse(rr.isSetBranchCoupling());
        Assert.assertNull(rr.getBranchCoupling());

        rr = rrs.get(1);
        Assert.assertEquals("n1", rr.getName());
        Assert.assertTrue(rr.isSetIsolationLevel());
        Assert.assertEquals(ResourceRef.IsolationLevelEnum.TRANSACTION_NONE, rr.getIsolationLevel());
        Assert.assertTrue(rr.isSetConnectionManagementPolicy());
        Assert.assertEquals(ResourceRef.ConnectionManagementPolicyEnum.DEFAULT, rr.getConnectionManagementPolicy());
        Assert.assertTrue(rr.isSetCommitPriority());
        Assert.assertEquals(1234, rr.getCommitPriority());
        Assert.assertTrue(rr.isSetBranchCoupling());
        Assert.assertEquals(ResourceRef.BranchCouplingEnum.LOOSE, rr.getBranchCoupling());

        rr = rrs.get(2);
        Assert.assertEquals("n2", rr.getName());
        Assert.assertTrue(rr.isSetIsolationLevel());
        Assert.assertEquals(ResourceRef.IsolationLevelEnum.TRANSACTION_READ_UNCOMMITTED, rr.getIsolationLevel());
        Assert.assertTrue(rr.isSetConnectionManagementPolicy());
        Assert.assertEquals(ResourceRef.ConnectionManagementPolicyEnum.AGGRESSIVE, rr.getConnectionManagementPolicy());
        Assert.assertFalse(rr.isSetCommitPriority());
        Assert.assertTrue(rr.isSetBranchCoupling());
        Assert.assertEquals(ResourceRef.BranchCouplingEnum.TIGHT, rr.getBranchCoupling());

        rr = rrs.get(3);
        Assert.assertEquals("n3", rr.getName());
        Assert.assertTrue(rr.isSetIsolationLevel());
        Assert.assertEquals(ResourceRef.IsolationLevelEnum.TRANSACTION_READ_COMMITTED, rr.getIsolationLevel());
        Assert.assertTrue(rr.isSetConnectionManagementPolicy());
        Assert.assertEquals(ResourceRef.ConnectionManagementPolicyEnum.NORMAL, rr.getConnectionManagementPolicy());
        Assert.assertFalse(rr.isSetCommitPriority());
        Assert.assertFalse(rr.isSetBranchCoupling());
        Assert.assertNull(rr.getBranchCoupling());

        rr = rrs.get(4);
        Assert.assertEquals("n4", rr.getName());
        Assert.assertTrue(rr.isSetIsolationLevel());
        Assert.assertEquals(ResourceRef.IsolationLevelEnum.TRANSACTION_REPEATABLE_READ, rr.getIsolationLevel());
        Assert.assertFalse(rr.isSetConnectionManagementPolicy());
        Assert.assertNull(rr.getConnectionManagementPolicy());
        Assert.assertFalse(rr.isSetCommitPriority());
        Assert.assertFalse(rr.isSetBranchCoupling());
        Assert.assertNull(rr.getBranchCoupling());

        rr = rrs.get(5);
        Assert.assertEquals("n5", rr.getName());
        Assert.assertTrue(rr.isSetIsolationLevel());
        Assert.assertEquals(ResourceRef.IsolationLevelEnum.TRANSACTION_SERIALIZABLE, rr.getIsolationLevel());
        Assert.assertFalse(rr.isSetConnectionManagementPolicy());
        Assert.assertNull(rr.getConnectionManagementPolicy());
        Assert.assertFalse(rr.isSetCommitPriority());
        Assert.assertFalse(rr.isSetBranchCoupling());
        Assert.assertNull(rr.getBranchCoupling());
    }

    @Test
    public void testXMIResourceRef() throws Exception {
        WebApp webApp = parse(webApp24() +
                              "<resource-ref id=\"rr0id\">" +
                              "  <res-ref-name>n0</res-ref-name>" +
                              "</resource-ref>" +
                              "<resource-ref id=\"rr1id\">" +
                              "  <res-ref-name>n1</res-ref-name>" +
                              "</resource-ref>" +
                              "<resource-ref id=\"rr2id\">" +
                              "  <res-ref-name>n2</res-ref-name>" +
                              "</resource-ref>" +
                              "<resource-ref id=\"rr3id\">" +
                              "  <res-ref-name>n3</res-ref-name>" +
                              "</resource-ref>" +
                              "<resource-ref id=\"rr4id\">" +
                              "  <res-ref-name>n4</res-ref-name>" +
                              "</resource-ref>" +
                              "<resource-ref id=\"rr5id\">" +
                              "  <res-ref-name>n5</res-ref-name>" +
                              "</resource-ref>" +
                              "</web-app>");
        WebExt webExt = parseWebExtXMI(webAppExtension("") + "</webappext:WebAppExtension>", webApp);
        Assert.assertEquals(Collections.emptyList(), webExt.getResourceRefs());

        webExt = parseWebExtXMI(webAppExtension("") +
                                "<resourceRefExtensions>" +
                                "  <resourceRef href=\"WEB-INF/web.xml#rr0id\"/>" +
                                "</resourceRefExtensions>" +
                                "<resourceRefExtensions" +
                                "  isolationLevel=\"TRANSACTION_NONE\"" +
                                "  connectionManagementPolicy=\"Default\"" +
                                "  commitPriority=\"1234\"" +
                                "  branchCoupling=\"Loose\"" +
                                ">" +
                                "  <resourceRef href=\"WEB-INF/web.xml#rr1id\"/>" +
                                "</resourceRefExtensions>" +
                                "<resourceRefExtensions" +
                                "  isolationLevel=\"TRANSACTION_READ_UNCOMMITTED\"" +
                                "  connectionManagementPolicy=\"Aggressive\"" +
                                "  branchCoupling=\"Tight\"" +
                                ">" +
                                "  <resourceRef href=\"WEB-INF/web.xml#rr2id\"/>" +
                                "</resourceRefExtensions>" +
                                "<resourceRefExtensions" +
                                "  isolationLevel=\"TRANSACTION_READ_COMMITTED\"" +
                                "  connectionManagementPolicy=\"Normal\"" +
                                ">" +
                                "  <resourceRef href=\"WEB-INF/web.xml#rr3id\"/>" +
                                "</resourceRefExtensions>" +
                                "<resourceRefExtensions isolationLevel=\"TRANSACTION_REPEATABLE_READ\">" +
                                "  <resourceRef href=\"WEB-INF/web.xml#rr4id\"/>" +
                                "</resourceRefExtensions>" +
                                "<resourceRefExtensions isolationLevel=\"TRANSACTION_SERIALIZABLE\">" +
                                "  <resourceRef href=\"WEB-INF/web.xml#rr5id\"/>" +
                                "</resourceRefExtensions>" +
                                "</webappext:WebAppExtension>", webApp);
        List<ResourceRef> rrs = webExt.getResourceRefs();
        Assert.assertEquals(rrs.toString(), 6, rrs.size());

        ResourceRef rr = rrs.get(0);
        Assert.assertEquals("n0", rr.getName());
        Assert.assertFalse(rr.isSetIsolationLevel());
        Assert.assertNull(rr.getIsolationLevel());
        Assert.assertFalse(rr.isSetConnectionManagementPolicy());
        Assert.assertNull(rr.getConnectionManagementPolicy());
        Assert.assertFalse(rr.isSetCommitPriority());
        Assert.assertFalse(rr.isSetBranchCoupling());
        Assert.assertNull(rr.getBranchCoupling());

        rr = rrs.get(1);
        Assert.assertEquals("n1", rr.getName());
        Assert.assertTrue(rr.isSetIsolationLevel());
        Assert.assertEquals(ResourceRef.IsolationLevelEnum.TRANSACTION_NONE, rr.getIsolationLevel());
        Assert.assertTrue(rr.isSetConnectionManagementPolicy());
        Assert.assertEquals(ResourceRef.ConnectionManagementPolicyEnum.DEFAULT, rr.getConnectionManagementPolicy());
        Assert.assertTrue(rr.isSetCommitPriority());
        Assert.assertEquals(1234, rr.getCommitPriority());
        Assert.assertTrue(rr.isSetBranchCoupling());
        Assert.assertEquals(ResourceRef.BranchCouplingEnum.LOOSE, rr.getBranchCoupling());

        rr = rrs.get(2);
        Assert.assertEquals("n2", rr.getName());
        Assert.assertTrue(rr.isSetIsolationLevel());
        Assert.assertEquals(ResourceRef.IsolationLevelEnum.TRANSACTION_READ_UNCOMMITTED, rr.getIsolationLevel());
        Assert.assertTrue(rr.isSetConnectionManagementPolicy());
        Assert.assertEquals(ResourceRef.ConnectionManagementPolicyEnum.AGGRESSIVE, rr.getConnectionManagementPolicy());
        Assert.assertFalse(rr.isSetCommitPriority());
        Assert.assertTrue(rr.isSetBranchCoupling());
        Assert.assertEquals(ResourceRef.BranchCouplingEnum.TIGHT, rr.getBranchCoupling());

        rr = rrs.get(3);
        Assert.assertEquals("n3", rr.getName());
        Assert.assertTrue(rr.isSetIsolationLevel());
        Assert.assertEquals(ResourceRef.IsolationLevelEnum.TRANSACTION_READ_COMMITTED, rr.getIsolationLevel());
        Assert.assertTrue(rr.isSetConnectionManagementPolicy());
        Assert.assertEquals(ResourceRef.ConnectionManagementPolicyEnum.NORMAL, rr.getConnectionManagementPolicy());
        Assert.assertFalse(rr.isSetCommitPriority());
        Assert.assertFalse(rr.isSetBranchCoupling());
        Assert.assertNull(rr.getBranchCoupling());

        rr = rrs.get(4);
        Assert.assertEquals("n4", rr.getName());
        Assert.assertTrue(rr.isSetIsolationLevel());
        Assert.assertEquals(ResourceRef.IsolationLevelEnum.TRANSACTION_REPEATABLE_READ, rr.getIsolationLevel());
        Assert.assertFalse(rr.isSetConnectionManagementPolicy());
        Assert.assertNull(rr.getConnectionManagementPolicy());
        Assert.assertFalse(rr.isSetCommitPriority());
        Assert.assertFalse(rr.isSetBranchCoupling());
        Assert.assertNull(rr.getBranchCoupling());

        rr = rrs.get(5);
        Assert.assertEquals("n5", rr.getName());
        Assert.assertTrue(rr.isSetIsolationLevel());
        Assert.assertEquals(ResourceRef.IsolationLevelEnum.TRANSACTION_SERIALIZABLE, rr.getIsolationLevel());
        Assert.assertFalse(rr.isSetConnectionManagementPolicy());
        Assert.assertNull(rr.getConnectionManagementPolicy());
        Assert.assertFalse(rr.isSetCommitPriority());
        Assert.assertFalse(rr.isSetBranchCoupling());
        Assert.assertNull(rr.getBranchCoupling());
    }

    @Test
    public void testServletCacheConfig() throws Exception {
        WebExt webExt = parseWebExtXML(webExt10() + "</web-ext>");
        Assert.assertEquals(Collections.emptyList(), webExt.getServletCacheConfigs());

        webExt = parseWebExtXML(webExt10() +
                                "<servlet-cache-config/>" +
                                "<servlet-cache-config properties-group-name=\"pgn\">" +
                                "  <servlet name=\"sn0\"/>" +
                                "  <servlet name=\"sn1\"/>" +
                                "  <timeout value=\"1234\"/>" +
                                "  <priority value=\"2345\"/>" +
                                "  <invalidate-only value=\"false\"/>" +
                                "  <external-cache-group name=\"ecgn0\"/>" +
                                "  <external-cache-group name=\"ecgn1\"/>" +
                                "  <id-generator class=\"igc\"/>" +
                                "  <metadata-generator class=\"mgc\"/>" +
                                "  <id-generation-properties/>" +
                                "</servlet-cache-config>" +
                                "<servlet-cache-config>" +
                                "  <invalidate-only value=\"true\"/>" +
                                "  <id-generation-properties use-uri=\"false\" alternate-name=\"an\" use-path-infos=\"false\">" +
                                "    <cache-variable/>" +
                                "    <cache-variable type=\"REQUEST_PARAMETER\" identifier=\"cvid\" method=\"cvm\" required=\"false\" data-id=\"cvdid\" invalidate=\"cvi\"/>" +
                                "    <cache-variable type=\"REQUEST_ATTRIBUTE\" required=\"true\"/>" +
                                "    <cache-variable type=\"SESSION_PARAMETER\"/>" +
                                "    <cache-variable type=\"COOKIE\"/>" +
                                "  </id-generation-properties>" +
                                "</servlet-cache-config>" +
                                "<servlet-cache-config>" +
                                "  <id-generation-properties use-uri=\"true\" use-path-infos=\"true\"/>" +
                                "</servlet-cache-config>" +
                                "</web-ext>");
        List<ServletCacheConfig> sccs = webExt.getServletCacheConfigs();
        Assert.assertEquals(sccs.toString(), 4, sccs.size());

        ServletCacheConfig scc = sccs.get(0);
        Assert.assertNull(scc.getPropertiesGroupName());
        Assert.assertEquals(Collections.emptyList(), scc.getServletNames());
        Assert.assertFalse(scc.isSetTimeout());
        Assert.assertFalse(scc.isSetPriority());
        Assert.assertFalse(scc.isSetInvalidateOnly());
        Assert.assertEquals(Collections.emptyList(), scc.getExternalCacheGroupNames());
        Assert.assertNull(scc.getIdGenerator());
        Assert.assertNull(scc.getMetadataGenerator());
        Assert.assertNull(scc.getIdGenerationProperties());

        scc = sccs.get(1);
        Assert.assertEquals("pgn", scc.getPropertiesGroupName());
        Assert.assertEquals(Arrays.asList("sn0", "sn1"), scc.getServletNames());
        Assert.assertTrue(scc.isSetTimeout());
        Assert.assertEquals(1234, scc.getTimeout());
        Assert.assertTrue(scc.isSetPriority());
        Assert.assertEquals(2345, scc.getPriority());
        Assert.assertTrue(scc.isSetInvalidateOnly());
        Assert.assertFalse(scc.isInvalidateOnly());
        Assert.assertEquals(Arrays.asList("ecgn0", "ecgn1"), scc.getExternalCacheGroupNames());
        Assert.assertEquals("igc", scc.getIdGenerator());
        Assert.assertEquals("mgc", scc.getMetadataGenerator());

        IdGenerationProperties igp = scc.getIdGenerationProperties();
        Assert.assertFalse(igp.isSetUseURI());
        Assert.assertFalse(igp.isSetAlternateName());
        Assert.assertNull(igp.getAlternateName());
        Assert.assertFalse(igp.isSetUsePathInfos());
        Assert.assertEquals(Collections.emptyList(), igp.getCacheVariables());

        scc = sccs.get(2);
        Assert.assertNull(scc.getPropertiesGroupName());
        Assert.assertEquals(Collections.emptyList(), scc.getServletNames());
        Assert.assertFalse(scc.isSetTimeout());
        Assert.assertFalse(scc.isSetPriority());
        Assert.assertTrue(scc.isSetInvalidateOnly());
        Assert.assertTrue(scc.isInvalidateOnly());
        Assert.assertEquals(Collections.emptyList(), scc.getExternalCacheGroupNames());

        igp = scc.getIdGenerationProperties();
        Assert.assertTrue(igp.isSetUseURI());
        Assert.assertFalse(igp.isUseURI());
        Assert.assertTrue(igp.isSetAlternateName());
        Assert.assertEquals("an", igp.getAlternateName());
        Assert.assertTrue(igp.isSetUsePathInfos());
        Assert.assertFalse(igp.isUsePathInfos());

        List<CacheVariable> cvs = igp.getCacheVariables();
        Assert.assertEquals(cvs.toString(), 5, cvs.size());

        CacheVariable cv = cvs.get(0);
        Assert.assertFalse(cv.isSetType());
        Assert.assertNull(cv.getType());
        Assert.assertNull(cv.getIdentifier());
        Assert.assertNull(cv.getMethod());
        Assert.assertFalse(cv.isSetRequired());
        Assert.assertNull(cv.getDataId());
        Assert.assertNull(cv.getInvalidate());

        cv = cvs.get(1);
        Assert.assertTrue(cv.isSetType());
        Assert.assertEquals(CacheVariable.TypeEnum.REQUEST_PARAMETER, cv.getType());
        Assert.assertEquals("cvid", cv.getIdentifier());
        Assert.assertEquals("cvm", cv.getMethod());
        Assert.assertTrue(cv.isSetRequired());
        Assert.assertFalse(cv.isRequired());
        Assert.assertEquals("cvdid", cv.getDataId());
        Assert.assertEquals("cvi", cv.getInvalidate());

        cv = cvs.get(2);
        Assert.assertTrue(cv.isSetType());
        Assert.assertEquals(CacheVariable.TypeEnum.REQUEST_ATTRIBUTE, cv.getType());
        Assert.assertNull(cv.getIdentifier());
        Assert.assertNull(cv.getMethod());
        Assert.assertTrue(cv.isSetRequired());
        Assert.assertTrue(cv.isRequired());
        Assert.assertNull(cv.getDataId());
        Assert.assertNull(cv.getInvalidate());

        cv = cvs.get(3);
        Assert.assertTrue(cv.isSetType());
        Assert.assertEquals(CacheVariable.TypeEnum.SESSION_PARAMETER, cv.getType());
        Assert.assertNull(cv.getIdentifier());
        Assert.assertNull(cv.getMethod());
        Assert.assertFalse(cv.isSetRequired());
        Assert.assertNull(cv.getDataId());
        Assert.assertNull(cv.getInvalidate());

        cv = cvs.get(4);
        Assert.assertTrue(cv.isSetType());
        Assert.assertEquals(CacheVariable.TypeEnum.COOKIE, cv.getType());
        Assert.assertNull(cv.getIdentifier());
        Assert.assertNull(cv.getMethod());
        Assert.assertFalse(cv.isSetRequired());
        Assert.assertNull(cv.getDataId());
        Assert.assertNull(cv.getInvalidate());

        scc = sccs.get(3);
        Assert.assertNull(scc.getPropertiesGroupName());
        Assert.assertEquals(Collections.emptyList(), scc.getServletNames());
        Assert.assertFalse(scc.isSetTimeout());
        Assert.assertFalse(scc.isSetPriority());
        Assert.assertFalse(scc.isSetInvalidateOnly());
        Assert.assertEquals(Collections.emptyList(), scc.getExternalCacheGroupNames());

        igp = scc.getIdGenerationProperties();
        Assert.assertTrue(igp.isSetUseURI());
        Assert.assertTrue(igp.isUseURI());
        Assert.assertFalse(igp.isSetAlternateName());
        Assert.assertNull(igp.getAlternateName());
        Assert.assertTrue(igp.isSetUsePathInfos());
        Assert.assertTrue(igp.isUsePathInfos());
        Assert.assertEquals(Collections.emptyList(), igp.getCacheVariables());
    }

    @Test
    public void testXMIServletCacheConfig() throws Exception {
        WebApp webApp = parse(webApp24() + "</web-app>");
        WebExt webExt = parseWebExtXMI(webAppExtension("") + "</webappext:WebAppExtension>", webApp);
        Assert.assertEquals(Collections.emptyList(), webExt.getServletCacheConfigs());

        webExt = parseWebExtXMI(webAppExtension("") +
                                "<servletCacheConfigs/>" +
                                "<servletCacheConfigs" +
                                "  propertiesGroupName=\"pgn\"" +
                                "  timeout=\"1234\"" +
                                "  priority=\"2345\"" +
                                "  invalidateOnly=\"false\"" +
                                "  externalCacheGroups=\"ecgn0 ecgn1\"" +
                                "  idGenerator=\"igc\"" +
                                "  metadataGenerator=\"mgc\"" +
                                "/>" +
                                "<servletCacheConfigs" +
                                "  invalidateOnly=\"true\"" +
                                "/>" +
                                "<servletCacheConfigs/>" +
                                "</webappext:WebAppExtension>", webApp);
        List<ServletCacheConfig> sccs = webExt.getServletCacheConfigs();
        Assert.assertEquals(sccs.toString(), 4, sccs.size());

        ServletCacheConfig scc = sccs.get(0);
        Assert.assertNull(scc.getPropertiesGroupName());
        Assert.assertEquals(Collections.emptyList(), scc.getServletNames());
        Assert.assertFalse(scc.isSetTimeout());
        Assert.assertFalse(scc.isSetPriority());
        Assert.assertFalse(scc.isSetInvalidateOnly());
        Assert.assertEquals(Collections.emptyList(), scc.getExternalCacheGroupNames());
        Assert.assertNull(scc.getIdGenerator());
        Assert.assertNull(scc.getMetadataGenerator());
        Assert.assertNull(scc.getIdGenerationProperties());

        scc = sccs.get(1);
        Assert.assertEquals("pgn", scc.getPropertiesGroupName());
        //Assert.assertEquals(Arrays.asList("sn0", "sn1"), scc.getServletNames());
        Assert.assertTrue(scc.isSetTimeout());
        Assert.assertEquals(1234, scc.getTimeout());
        Assert.assertTrue(scc.isSetPriority());
        Assert.assertEquals(2345, scc.getPriority());
        Assert.assertTrue(scc.isSetInvalidateOnly());
        Assert.assertFalse(scc.isInvalidateOnly());
        Assert.assertEquals(Arrays.asList("ecgn0", "ecgn1"), scc.getExternalCacheGroupNames());
        Assert.assertEquals("igc", scc.getIdGenerator());
        Assert.assertEquals("mgc", scc.getMetadataGenerator());

        IdGenerationProperties igp = scc.getIdGenerationProperties();
        Assert.assertNull(igp);
        //Assert.assertFalse(igp.isSetUseURI());
        //Assert.assertFalse(igp.isSetAlternateName());
        //Assert.assertNull(igp.getAlternateName());
        //Assert.assertFalse(igp.isSetUsePathInfos());
        //Assert.assertEquals(Collections.emptyList(), igp.getCacheVariables());

        scc = sccs.get(2);
        Assert.assertNull(scc.getPropertiesGroupName());
        Assert.assertEquals(Collections.emptyList(), scc.getServletNames());
        Assert.assertFalse(scc.isSetTimeout());
        Assert.assertFalse(scc.isSetPriority());
        Assert.assertTrue(scc.isSetInvalidateOnly());
        Assert.assertTrue(scc.isInvalidateOnly());
        Assert.assertEquals(Collections.emptyList(), scc.getExternalCacheGroupNames());

        igp = scc.getIdGenerationProperties();
        Assert.assertNull(igp);
        //Assert.assertTrue(igp.isSetUseURI());
        //Assert.assertFalse(igp.isUseURI());
        //Assert.assertTrue(igp.isSetAlternateName());
        //Assert.assertEquals("an", igp.getAlternateName());
        //Assert.assertTrue(igp.isSetUsePathInfos());
        //Assert.assertFalse(igp.isUsePathInfos());

        //List<CacheVariable> cvs = igp.getCacheVariables();
        //Assert.assertEquals(cvs.toString(), 5, cvs.size());

        //CacheVariable cv = cvs.get(0);
        //Assert.assertFalse(cv.isSetType());
        //Assert.assertNull(cv.getType());
        //Assert.assertNull(cv.getIdentifier());
        //Assert.assertNull(cv.getMethod());
        //Assert.assertFalse(cv.isSetRequired());
        //Assert.assertNull(cv.getDataId());
        //Assert.assertNull(cv.getInvalidate());

        //cv = cvs.get(1);
        //Assert.assertTrue(cv.isSetType());
        //Assert.assertEquals(CacheVariable.TypeEnum.REQUEST_PARAMETER, cv.getType());
        //Assert.assertEquals("cvid", cv.getIdentifier());
        //Assert.assertEquals("cvm", cv.getMethod());
        //Assert.assertTrue(cv.isSetRequired());
        //Assert.assertFalse(cv.isRequired());
        //Assert.assertEquals("cvdid", cv.getDataId());
        //Assert.assertEquals("cvi", cv.getInvalidate());

        //cv = cvs.get(2);
        //Assert.assertTrue(cv.isSetType());
        //Assert.assertEquals(CacheVariable.TypeEnum.REQUEST_ATTRIBUTE, cv.getType());
        //Assert.assertNull(cv.getIdentifier());
        //Assert.assertNull(cv.getMethod());
        //Assert.assertTrue(cv.isSetRequired());
        //Assert.assertTrue(cv.isRequired());
        //Assert.assertNull(cv.getDataId());
        //Assert.assertNull(cv.getInvalidate());

        //cv = cvs.get(3);
        //Assert.assertTrue(cv.isSetType());
        //Assert.assertEquals(CacheVariable.TypeEnum.SESSION_PARAMETER, cv.getType());
        //Assert.assertNull(cv.getIdentifier());
        //Assert.assertNull(cv.getMethod());
        //Assert.assertFalse(cv.isSetRequired());
        //Assert.assertNull(cv.getDataId());
        //Assert.assertNull(cv.getInvalidate());

        //cv = cvs.get(4);
        //Assert.assertTrue(cv.isSetType());
        //Assert.assertEquals(CacheVariable.TypeEnum.COOKIE, cv.getType());
        //Assert.assertNull(cv.getIdentifier());
        //Assert.assertNull(cv.getMethod());
        //Assert.assertFalse(cv.isSetRequired());
        //Assert.assertNull(cv.getDataId());
        //Assert.assertNull(cv.getInvalidate());

        scc = sccs.get(3);
        Assert.assertNull(scc.getPropertiesGroupName());
        Assert.assertEquals(Collections.emptyList(), scc.getServletNames());
        Assert.assertFalse(scc.isSetTimeout());
        Assert.assertFalse(scc.isSetPriority());
        Assert.assertFalse(scc.isSetInvalidateOnly());
        Assert.assertEquals(Collections.emptyList(), scc.getExternalCacheGroupNames());

        igp = scc.getIdGenerationProperties();
        Assert.assertNull(igp);
        //Assert.assertTrue(igp.isSetUseURI());
        //Assert.assertTrue(igp.isUseURI());
        //Assert.assertFalse(igp.isSetAlternateName());
        //Assert.assertNull(igp.getAlternateName());
        //Assert.assertTrue(igp.isSetUsePathInfos());
        //Assert.assertTrue(igp.isUsePathInfos());
        //Assert.assertEquals(Collections.emptyList(), igp.getCacheVariables());
    }

    @Test
    public void testWebExtContextRootServlet31() throws Exception {

        final WebApp webAppMock = mockery.mock(WebApp.class, "webapp" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(webAppMock).getVersion();
                will(returnValue("3.1"));
            }
        });

        WebExt webExt = parse(webExt11() +
                              "<context-root uri=\"TestContextRoot\" />" +
                              "</web-ext>", new WebExtAdapter(), WebExt.XML_EXT_NAME, WebApp.class, webAppMock);

        Assert.assertEquals("Context root not found in ibm-web-ext.xml using Servlet31 descriptor root", "TestContextRoot", webExt.getContextRoot());
    }

    @Test
    public void testWebExtContextRootServlet30() throws Exception {

        final WebApp webAppMock = mockery.mock(WebApp.class, "webapp" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(webAppMock).getVersion();
                will(returnValue("3.0"));
            }
        });

        WebExt webExt = parse(webExt11() +
                              "<context-root uri=\"TestContextRoot\" />" +
                              "</web-ext>", new WebExtAdapter(), WebExt.XML_EXT_NAME, WebApp.class, webAppMock);

        Assert.assertEquals("Context root not found in ibm-web-ext.xml using Servlet30 descriptor root", "TestContextRoot", webExt.getContextRoot());

    }
}
