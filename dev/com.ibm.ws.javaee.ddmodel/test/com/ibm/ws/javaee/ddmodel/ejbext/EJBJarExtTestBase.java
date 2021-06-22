/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ejbext;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejbext.EJBJarExt;
import com.ibm.ws.javaee.ddmodel.DDParserBndExt;
import com.ibm.ws.javaee.ddmodel.ejb.EJBJarTestBase;

/**
 * test the ejb-jar-ext.xml parser
 * -concentrate on the pristine path where the ejb-jarext.xml file is well formed
 * -testing entity and relationships is optional
 * -testing error handling is secondary
 */
public class EJBJarExtTestBase extends EJBJarTestBase {

    public EJBJarExtTestBase(boolean ejbInWar) {
        super(ejbInWar);
    }

    //
    
    // TODO: Haven't found the correct pattern for this ...
    //       Need to use 'getEJBInJar', which is instance
    //       state because of how repeat testing works.
    //       But the value should be initialized as a static
    //       variable, since it is to be shared between tests.
    
    private EJBJar ejbJar21;

    public EJBJar getEJBJar21() throws Exception {
        if ( ejbJar21 == null ) {
            ejbJar21 = parseEJBJar(ejbJar21() + "</ejb-jar>");
        }
        return ejbJar21;
    }

    //
    
    protected EJBJarExtAdapter createEJBJarExtAdapter() {
        return new EJBJarExtAdapter();
    }

    public EJBJarExt parseEJBJarExtXMI(String ddText, EJBJar ejbJar) throws Exception {
        return parseEJBJarExt(ddText, DDParserBndExt.IS_XMI, ejbJar, null);
    }

    public EJBJarExt parseEJBJarExtXML(String ddText) throws Exception {
        return parseEJBJarExt(ddText, !DDParserBndExt.IS_XMI, null, null);
    }
    
    public EJBJarExt parseEJBJarExtXML(String ddText, String altMessage, String...messages) throws Exception {
        return parseEJBJarExt(ddText, !DDParserBndExt.IS_XMI, null, altMessage, messages);
    }    

    public EJBJarExt parseEJBJarExt(
            String ddText, boolean xmi, EJBJar ejbJar,
            String altMessage, String ... messages) throws Exception {

        boolean ejbInWar = getEJBInWar();

        String appPath = null;

        String modulePath;
        if ( ejbInWar ) {
            modulePath = "/root/wlp/usr/servers/server1/apps/myWEB.war";
        } else {
            modulePath = "/root/wlp/usr/servers/server1/apps/myEJB.jar";
        }

        String fragmentPath = null;

        String ddPath;
        if ( ejbInWar ) {
            ddPath = ( xmi ? EJBJarExtAdapter.XMI_EXT_IN_WEB_MOD_NAME : EJBJarExtAdapter.XML_EXT_IN_WEB_MOD_NAME );
        } else {
            ddPath = ( xmi ? EJBJarExtAdapter.XML_EXT_IN_EJB_MOD_NAME : EJBJarExtAdapter.XML_EXT_IN_EJB_MOD_NAME );            
        }

        WebModuleInfo moduleInfo =
            ( ejbInWar ? mockery.mock(WebModuleInfo.class, "webModuleInfo" + mockId++) : null );

        return parse(
                appPath, modulePath, fragmentPath,
                ddText, createEJBJarExtAdapter(), ddPath,
                EJBJar.class, ejbJar,
                WebModuleInfo.class, moduleInfo,
                altMessage, messages);
    }

    static final String ejbJar21() {
        return "<ejb-jar" +
               " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
               " version=\"2.1\"" +
               " id=\"EJBJar_ID\"" +
               ">";
    }

    static final String ejbJarExtension(String attrs) {
        return "<ejbext:EJBJarExtension" +
               " xmlns:ejbext=\"ejbext.xmi\"" +
               " xmlns:xmi=\"http://www.omg.org/XMI\"" +
               " xmlns:ejb=\"ejb.xmi\"" +
               " xmi:version=\"2.0\"" +
               " " + attrs +
               ">" +
               "<ejbJar href=\"META-INF/ejb-jar.xml#EJBJar_ID\"/>";
    }

    static final String ejbJarExt10() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <ejb-jar-ext" +
               " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_0.xsd\"" +
               " version=\"1.0\"" +
               ">";
    }

    static final String ejbJarExt11() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <ejb-jar-ext" +
               " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_1.xsd\"" +
               " version=\"1.1\"" +
               ">";
    }
}
