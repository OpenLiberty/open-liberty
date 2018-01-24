/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.fat.tests;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Test;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

/**
 * This test requires the ejb-3.2 feature. I started with it merged into EjbTimerTest, but
 * that test depends on ejbLite-3.2, and/or there's something funny about the way it uses
 * SHARED_SERVER... either way, EjbTimerTest hard to add new tests to.
 */
public class EjbInLibExtTest extends LoggingTest {

    private static final String SERVER_NAME = "cdi12EJB32FullWithLibServer";

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer(SERVER_NAME);

    @BuildShrinkWrap
    public static Map<Archive,String> buildShrinkWrap() {

       JavaArchive ejbInLibExtEJBModuleJar = ShrinkWrap.create(JavaArchive.class,"ejbInLibExtEJBModule.jar")
                        .addClass("test.EJB2")
                        .add(new FileAsset(new File("test-applications/ejbInLibExtEJBModule.jar/resources/META-INF/ejb-jar.xml")), "/META-INF/ejb-jar.xml");

       JavaArchive ejbInLibExtJAR = ShrinkWrap.create(JavaArchive.class, "ejbInLibExtJAR.jar")
                        .addClass("test.ejb.EJB1")
                        .addClass("test.ejb.EJB1Impl");

       WebArchive ejbInLibExtWARModule = ShrinkWrap.create(WebArchive.class, "ejbInLibExtWARModule.war")
                        .addClass("test.war.TestServlet");

       EnterpriseArchive ejbInLibExtEar = ShrinkWrap.create(EnterpriseArchive.class, "ejbInLibExt.ear")
                        .addAsModule(ejbInLibExtWARModule)
                        .addAsModule(ejbInLibExtEJBModuleJar);

       Map<Archive,String> returnMap = new HashMap<Archive,String>();
       returnMap.put(ejbInLibExtEar, "publish/servers/"+SERVER_NAME+"/apps");
       returnMap.put(ejbInLibExtJAR, "publish/servers/"+SERVER_NAME+"/libs");
       return returnMap;

    }


    @Test
    public void testEJBInLibExt() throws Exception {
        verifyResponse("/ejbInLibExtWARModule/", "DATA!");
    }

    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }
}
