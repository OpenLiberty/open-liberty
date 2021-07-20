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
package com.ibm.ws.javaee.ddmodel.ejb;

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.ddmodel.DDTestBase;

/**
 * test the ejb-jar.xml parser
 *
 * -concentrate on the pristine path where the ejb-jar.xml file is well formed
 * -testing entity and relationships is optional
 * -testing error handling is secondary
 *
 * -Error handling philosophy:
 *
 * As determined by Glann marcy and Brett Kail, the easiest thing to do at
 * this point is to change the parser to return a "sensible default",
 * but if not possible, unwind by discarding objects until something
 * valid is returned (e.g., EJBRelation.getRelationshipRoles() should be
 * discarded if only one <ejb-relation/> is specified).
 *
 * If we can match the defaults used by WCCM (by looking at
 * WCCMBASE/ws/code/jst.j2ee.core.mofj2ee), that would be ideal.
 */
public class EJBJarTestBase extends DDTestBase {

    public EJBJarTestBase(boolean ejbInWar) {
        this.ejbInWar = ejbInWar;
    }

    private final boolean ejbInWar;

    public boolean getEJBInWar() {
        return ejbInWar;
    }    
    
    public static String getEJBJarPath(boolean ejbInWar) {
        return ( ejbInWar ? "WEB-INF/ejb-jar.xml" : "META-INF/ejb-jar.xml" );
    }

    public String getEJBJarPath() {
        return getEJBJarPath( getEJBInWar() );
    }

    //

    public EJBJar parseEJBJar(String ddbody) throws Exception {
        return parseEJBJar(ddbody, EJBJar.VERSION_3_2, getEJBInWar(), null);
    }
    
    public EJBJar parseEJBJar(String ddbody, String altMessage, String...messages) throws Exception {
        return parseEJBJar(ddbody, EJBJar.VERSION_3_2, getEJBInWar(), altMessage, messages);
    }
    
    protected EJBJar parseEJBJar(String ddbody, int maxSchemaVersion) throws Exception {
        return parseEJBJar(ddbody, maxSchemaVersion, getEJBInWar(), null);
    }
    
    protected EJBJar parseEJBJar(String ddbody, int maxSchemaVersion, String altMessage, String...messages) throws Exception {
        return parseEJBJar(ddbody, maxSchemaVersion, getEJBInWar(), altMessage, messages);
    }    

    protected static EJBJarEntryAdapter createEJBJarAdapter(int maxSchemaVersion) {
        @SuppressWarnings("unchecked")
        ServiceReference<EJBJarDDParserVersion> versionRef =
            mockery.mock(ServiceReference.class, "sr" + generateId());

        mockery.checking(new Expectations() {
            {        
                allowing(versionRef).getProperty(EJBJarDDParserVersion.VERSION);
                will(returnValue(maxSchemaVersion));
            }
        });

        EJBJarEntryAdapter ddAdapter = new EJBJarEntryAdapter();
        ddAdapter.setVersion(versionRef);
        
        return ddAdapter;
    }
    
    public static final boolean EJB_IN_WAR = true;
    
    /**
     * Parse XML body as an EJB deployment descriptor using
     * the rules for the specified maximum schema version.
     * Answer the parsed root descriptor element.
     * 
     * The parse may be expected to fail, in which case, the
     * captured exception must have one of the specified
     * expected messages.  Answer null if parsing fails and
     * an expected message is received.  Thrown an exception
     * if parsing is expected to fail but does not.
     *
     * Parsing assigns names to container artifacts using
     * a unique ID.  See {@link #generateId()}.
     *
     * @param ddbody XML body which is to be parsed.
     * @param maxSchemaVersion The maximum schema version to
     *     use to parse the descriptor.
     * @param altMessage Alternate error body.
     * @param messages Error body which is expected.
     *
     * @return The parsed root EJB descriptor element, or null
     *     if an expected exception is thrown.
     *
     * @throws Exception Thrown if parsing fails with an
     *     unexpected exception, or if parsing succeeds when
     *     an exception is expected.
     */
    protected static EJBJar parseEJBJar(
            String ddbody, int maxSchemaVersion, boolean ejbInWar,
            String altMessage, String... messages) throws Exception {
        
        String appPath = null;
        
        String modulePath;
        if ( ejbInWar ) {
            modulePath = "/root/wlp/usr/servers/server1/apps/MyWAR.war";
        } else {
            modulePath = "/root/wlp/usr/servers/server1/apps/MyEJB.jar";
        }

        String fragmentPath = null;

        String ddPath = getEJBJarPath(ejbInWar);
        
        WebModuleInfo webModuleInfo;
        if ( ejbInWar ) {
            webModuleInfo = mockery.mock(WebModuleInfo.class, "webModuleInfo" + mockId++);
        } else {
            webModuleInfo = null;
        }

        return parse(
                appPath, modulePath, fragmentPath,
                ddbody, createEJBJarAdapter(maxSchemaVersion), ddPath,
                null, null,
                WebModuleInfo.class, webModuleInfo,
                altMessage, messages);
    }

    //

    protected static String ejbJar11Head() {
        return "<!DOCTYPE ejb-jar PUBLIC" +
               " \"-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN\"" +
               " \"http://java.sun.com/j2ee/dtds/ejb-jar_1_1.dtd\">" +
               "<ejb-jar>";
    }

    protected static String ejbJar20Head() {
        return "<!DOCTYPE ejb-jar PUBLIC \"-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN\"" +
               " \"http://java.sun.com/dtd/ejb-jar_2_0.dtd\">" +
               "<ejb-jar>";
    }

    protected static String ejbJar21Head() {
        return "<ejb-jar" +
               " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
               " version=\"2.1\"" +
               ">";
    }

    protected static String ejbJar30Head(String attrs) {
        return "<ejb-jar" +
               " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd\"" +
               " version=\"3.0\"" +
               " " + attrs +
               ">";
    }

    protected static String ejbJar31Head(String attrs) {
        return "<ejb-jar" +
               " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_1.xsd\"" +
               " version=\"3.1\"" +
               " " + attrs +
               ">";
    }

    protected static String ejbJar32Head(String attrs) {
        return "<ejb-jar" +
               " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/ejb-jar_3_2.xsd\"" +
               " version=\"3.2\"" +
               " " + attrs +
               ">";
    }

    protected static String ejbJar40Head(String attrs) {
        return "<ejb-jar" +
               " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/ejb-jar_4_0.xsd\"" +
               " version=\"4.0\"" +
               " " + attrs +
               ">";
    }
    
    protected static String ejbJarTail() {
        return "</ejb-jar>";
    }
    
    //
    
    protected static String ejbJar11(String body) {
        return ejbJar11Head() + body + ejbJarTail();
    }

    protected static String ejbJar20(String body) {
        return ejbJar20Head() + body + ejbJarTail();        
    }
    
    protected static String ejbJar21(String body) {
        return ejbJar21Head() + body + ejbJarTail();        
    }

    protected static String ejbJar30(String attrs, String body) {
        return ejbJar30Head(attrs) + body + ejbJarTail();        
    }

    protected static String ejbJar31(String attrs, String body) {
        return ejbJar31Head(attrs) + body + ejbJarTail();
    }

    protected static String ejbJar32(String attrs, String body) {
        return ejbJar32Head(attrs) + body + ejbJarTail();
    }
    
    protected static String ejbJar40(String attrs, String body) {
        return ejbJar40Head(attrs) + body + ejbJarTail();
    }

    protected static String ejbJar(int version, String attrs, String body) {
        String head;

        if ( version == EJBJar.VERSION_1_1 ) {
            head = ejbJar11Head();
        } else if ( version == EJBJar.VERSION_2_0 ) {
            head = ejbJar20Head();
        } else if ( version == EJBJar.VERSION_2_1 ) {
            head = ejbJar21Head();
        } else if ( version == EJBJar.VERSION_3_0 ) {
            head = ejbJar30Head(attrs);
        } else if ( version == EJBJar.VERSION_3_1 ) {
            head = ejbJar31Head(attrs);
        } else if ( version == EJBJar.VERSION_3_2 ) {
            head = ejbJar32Head(attrs);
        } else if ( version == EJBJar.VERSION_4_0 ) {
            head = ejbJar40Head(attrs);
        } else {
            throw new IllegalArgumentException("Unknown EJBJar version [ " + version + " ]");
        }
        
        return head + body + ejbJarTail();
    }
    
    //

    public static final String ejbJarBody21 =
            "<enterprise-beans>" + "\n" +
                "<session id=\"s0\">" + "\n" +
                    "<ejb-name>SessionBean1</ejb-name>" + "\n" +
                "</session>" + "\n" +
            "</enterprise-beans>";

    private EJBJar ejbJar21;

    public EJBJar getEJBJar21() throws Exception {
        if ( ejbJar21 == null ) {
            ejbJar21 = parseEJBJar( ejbJar21Head() + "\n" +
                                        ejbJarBody21 + "\n" +
                                    ejbJarTail() );
        }
        return ejbJar21;
    }
}
