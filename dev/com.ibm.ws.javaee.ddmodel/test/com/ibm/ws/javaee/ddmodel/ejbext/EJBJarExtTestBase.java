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

import com.ibm.ws.container.service.app.deploy.EJBModuleInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
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

        ClassKeyedData[] adaptData = new ClassKeyedData[] {
                new ClassKeyedData(EJBJar.class, ejbJar)
        };
        
        String ddPath;
        ClassKeyedData[] cacheData;
        
        if ( ejbInWar ) {
            ddPath = ( xmi ? EJBJarExtAdapter.XMI_EXT_IN_WEB_MOD_NAME : EJBJarExtAdapter.XML_EXT_IN_WEB_MOD_NAME );

            WebModuleInfo webModuleInfo = mockery.mock(WebModuleInfo.class, "webModuleInfo" + mockId++);

            cacheData = new ClassKeyedData[] {
                    new ClassKeyedData(ModuleInfo.class, webModuleInfo),
                    new ClassKeyedData(WebModuleInfo.class, webModuleInfo)
            };

        } else {
            ddPath = ( xmi ? EJBJarExtAdapter.XMI_EXT_IN_EJB_MOD_NAME : EJBJarExtAdapter.XML_EXT_IN_EJB_MOD_NAME );

            EJBModuleInfo ejbModuleInfo = mockery.mock(EJBModuleInfo.class, "ejbModuleInfo" + mockId++);
            
            cacheData = new ClassKeyedData[] {
                    new ClassKeyedData(ModuleInfo.class, ejbModuleInfo),
                    new ClassKeyedData(EJBModuleInfo.class, ejbModuleInfo)
            };
        }

        return parse(
                appPath, modulePath, fragmentPath,
                ddText, createEJBJarExtAdapter(), ddPath,
                adaptData, cacheData,
                altMessage, messages);                    
    }

    protected static String ejbJar21() {
        return "<ejb-jar" +
                   " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                   " version=\"2.1\"" +
                   " id=\"EJBJar_ID\"" +
               ">";
    }

    protected String ejbJarExtXMI(String attrs) {
        return ejbJarExtXMI(attrs, getEJBJarPath() );
    }

    protected static String ejbJarExtXMI(String attrs, String ejbDDPath) {
        return "<ejbext:EJBJarExtension" +
                   " xmlns:ejbext=\"ejbext.xmi\"" +
                   " xmlns:xmi=\"http://www.omg.org/XMI\"" +
                   " xmlns:ejb=\"ejb.xmi\"" +
                   " xmi:version=\"2.0\"" +
                   " " + attrs +
               ">" +
               "<ejbJar href=\"" + ejbDDPath + "#EJBJar_ID\"/>";
    }

    protected static String ejbJarExt10XML() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<ejb-jar-ext" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_0.xsd\"" +
                   " version=\"1.0\"" +
               ">";
    }

    protected static String ejbJarExt11XML() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<ejb-jar-ext" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_1.xsd\"" +
                   " version=\"1.1\"" +
               ">";
    }
}
