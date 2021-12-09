/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.webbnd;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.webbnd.WebBnd;
import com.ibm.ws.javaee.ddmodel.web.WebAppTestBase;

public class WebAppBndTestBase extends WebAppTestBase {
    protected static WebBndAdapter createWebBndAdapter() {
        return new WebBndAdapter();
    }
    
    protected WebBnd parseWebBndXMI(String ddText, WebApp webApp) throws Exception {
        return parseWebBnd(ddText, WebBnd.XMI_BND_NAME, webApp, null);
    }

    protected WebBnd parseWebBndXMI(String ddText, WebApp webApp, String altMessage, String... messages) throws Exception {
        return parseWebBnd(ddText, WebBnd.XMI_BND_NAME, webApp, altMessage, messages);
    }

    protected WebBnd parseWebBndXML(String ddText) throws Exception {
        return parseWebBnd(ddText, WebBnd.XML_BND_NAME, null, null);
    }
    
    protected WebBnd parseWebBndXML(String ddText, String altMessage, String... messages) throws Exception {
        return parseWebBnd(ddText, WebBnd.XML_BND_NAME, null, altMessage, messages);
    }    
    
    protected WebBnd parseWebBnd(String ddText, String ddPath, WebApp webApp) throws Exception {
        return parseWebBnd(ddText, ddPath, webApp, null);
    }

    private WebBnd parseWebBnd(
            String ddText, String ddPath, WebApp webApp,
            String altMessage, String... messages) throws Exception {

        String appPath = null;
        String modulePath = "/root/wlp/usr/servers/server1/apps/MyWar.war";
        String fragmentPath = null;

        WebModuleInfo webInfo = mockery.mock(WebModuleInfo.class, "webModuleInfo" + mockId++);

        return parse(
                appPath, modulePath, fragmentPath,
                ddText, createWebBndAdapter(), ddPath,
                WebApp.class, webApp,
                WebModuleInfo.class, webInfo,
                altMessage, messages);        
    }

    protected static String webBndXMIBody() {
        return "<webapp href=\"WEB-INF/web.xml#WebApp_ID\"/>";
    }

    protected static final String webBndTailXMI =
            "</webappbnd:WebAppBinding>";

    protected static String webBndXMI20() {
        return webBndXMI20("", webBndXMIBody());
    }

    // " xmlns:webapplication=\"webapplication.xmi\" " +

    protected static String webBndXMI20(String attrs, String body) {
        return "<webappbnd:WebAppBinding" +
                   " xmlns:webappbnd=\"webappbnd.xmi\"" +
                   " xmlns:commonbnd=\"commonbnd.xmi\"" +
                   " xmlns:xmi=\"http://www.omg.org/XMI\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xmi:version=\"2.0\" " +
                   attrs +
               ">" +
                   "<webapp href=\"WEB-INF/web.xml#WebApp_ID\"/>" +
                   body +
               webBndTailXMI;
    }

    protected static String webBndXMLBody() {
        return "<virtual-host name=\"fromApp\"/>" + "\n" +
               "<ejb-ref name=\"ejb/PriceSession\" binding-name=\"ejb/com/ibm/svt/populateModule/grade/PriceSessionHome\"/>" + "\n" +
               "<ejb-ref name=\"SSBR\" binding-name=\"ejb/GasNet/Station#com.ibm.svt.stationModule.gas.station.StationSessionBeanRemote\"/>" + "\n" +
               "<ejb-ref name=\"GMSBR\" binding-name=\"ejb/GasNet/GasMaint#com.ibm.svt.stationModule.gas.maintenance.GasMaintenanceSessionBeanRemote\"/>" + "\n" +
               "<ejb-ref name=\"SeqSBR\" binding-name=\"ejb/GasNet/Seq#com.ibm.svt.stationModule.sequence.SequenceSessionBeanRemote\"/>" + "\n" +
               "<ejb-ref name=\"SMBR\" binding-name=\"ejb/GasNet/StoreMaint#com.ibm.svt.stationModule.store.maintenance.StoreMaintenanceBeanRemote\"/>" + "\n" +
               "<ejb-ref name=\"SCartBR\" binding-name=\"ejb/GasNet/Cart#com.ibm.svt.stationModule.store.storeSessions.ShoppingCartBeanRemote\"/>" + "\n" +
               "<ejb-ref name=\"StoreSBR\" binding-name=\"ejb/GasNet/StationStore#com.ibm.svt.stationModule.store.storeSessions.StationStoreSessionBeanRemote\"/>" + "\n" +
               "<ejb-ref name=\"FLSBR\" binding-name=\"ejb/GasNet/Failure#com.ibm.svt.stationModule.failureLog.FailureLogSessionBeanRemote\"/>" + "\n" +
               "<resource-ref name=\"FuelDS\" binding-name=\"jdbc/FuelDS\"/>";
    }
    
    protected static final String webBndXMLBody_noVHost() {
        return "<ejb-ref name=\"ejb/PriceSession\" binding-name=\"ejb/com/ibm/svt/populateModule/grade/PriceSessionHome\"/>" + "\n" +
               "<resource-ref name=\"FuelDS\" binding-name=\"jdbc/FuelDS\"/>";
    }
    
    protected static final String webBndTailXML =
            "</web-bnd>";

    protected static String webBndXML10() {
        return webBndXML10(webBndXMLBody());
    }
    
    protected static String webBndXML10(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<web-bnd" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_0.xsd\"" +
                   " version=\"1.0\"" +
               ">" +
                   body +
               webBndTailXML;
    }

    protected static String webBndXML11() {
        return webBndXML11(webBndXMLBody());
    }
    
    protected static String webBndXML11(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<web-bnd" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_1.xsd\"" +
                   " version=\"1.1\"" +
               ">" +
                   body +
               webBndTailXML;
    }

    protected static String webBndXML12() {
        return webBndXML12(webBndXMLBody());
    }

    protected static String webBndXML12(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<web-bnd" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_2.xsd\"" +
                   " version=\"1.2\"" +
               ">" +
                   body +
               webBndTailXML;
    }
}
