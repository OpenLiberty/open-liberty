/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.ejb;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.javaee.dd.common.RunAs;
import com.ibm.ws.javaee.dd.common.SecurityRoleRef;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejb.SecurityIdentity;
import com.ibm.ws.javaee.ddmodel.DDJakarta10Elements;

@RunWith(Parameterized.class)
public class EJBJarEnterpriseBeanTest extends EJBJarTestBase {
    @Parameters
    public static Iterable<? extends Object> data() {
        return TEST_DATA;
    }
    
    public EJBJarEnterpriseBeanTest(boolean ejbInWar) {
        super(ejbInWar);
    }

    //

    protected static final String ejb0XML =
            "<enterprise-beans>" +
                "<session>" +
                    "<ejb-name>TestSession</ejb-name>" +
                    "<security-identity>" +
                        "<use-caller-identity></use-caller-identity>" +
                    "</security-identity>" +
                    "<security-role-ref>" +
                        "<role-name>roleName0</role-name>" +
                    "</security-role-ref>" +
                    "<security-role-ref>" +
                        "<role-name>roleName1</role-name>" +
                    "</security-role-ref>" +
                    "<security-role-ref>" +
                        "<role-name>roleName2</role-name>" +
                    "</security-role-ref>" +
                "</session>" +

                "<entity>" +
                    "<ejb-name>TestEntity</ejb-name>" +
                    "<security-identity>" +
                        "<run-as>" +
                            "<role-name>runAsRoleName</role-name>" +
                        "</run-as>" +
                    "</security-identity>" +
                    "<security-role-ref>" +
                        "<role-name>secroleRefRoleName</role-name>" +
                    "</security-role-ref>" +
                "</entity>" +
            "</enterprise-beans>";
    
    @Test
    public void testEnterpriseBeanSecurity() throws Exception {
        List<EnterpriseBean> beans =
            parseEJBJarMax( ejbJar20(ejb0XML) )
                .getEnterpriseBeans();

        Assert.assertEquals(2, beans.size());

        EnterpriseBean bean0 = beans.get(0);
        Assert.assertEquals("TestSession", bean0.getName());
        SecurityIdentity secId0 = bean0.getSecurityIdentity();
        Assert.assertTrue(secId0.isUseCallerIdentity());
        Assert.assertNull(secId0.getRunAs());

        List<SecurityRoleRef> secRoleRefs = bean0.getSecurityRoleRefs();
        Assert.assertEquals("roleName0", secRoleRefs.get(0).getName());
        Assert.assertEquals("roleName1", secRoleRefs.get(1).getName());
        Assert.assertEquals("roleName2", secRoleRefs.get(2).getName());

        EnterpriseBean bean1 = beans.get(1);
        Assert.assertEquals("TestEntity", bean1.getName());
        SecurityIdentity secId1 = bean1.getSecurityIdentity();
        Assert.assertFalse(secId1.isUseCallerIdentity());
        RunAs runAs = secId1.getRunAs();
        Assert.assertEquals("runAsRoleName", runAs.getRoleName());
    }

    protected static final String ejb1XML =    
            "<enterprise-beans>" +
                "<session>" +
                    "<ejb-name>TestSession</ejb-name>" +
                "</session>" +

                "<entity>" +
                    "<ejb-name>TestEntity</ejb-name>" +
                    "<ejb-class>TestEntity.ejb.class.name</ejb-class>" +
                    "<mapped-name>TestEntity.ejb.mapped.name</mapped-name>" +
                "</entity>" +

                "<message-driven>" +
                    "<ejb-name>TestMessageDriven</ejb-name>" +
                    "<ejb-class>TestMessageDriven.ejb.class.name</ejb-class>" +
                    "<mapped-name>TestMessageDriven.ejb.mapped.name</mapped-name>" +
                "</message-driven>" +
            "</enterprise-beans>";

    protected static String getSessionBeansXml() {
        return getBeansXml(ADD_SESSION, !ADD_ENTITY, !ADD_MDB);
    }
    
    protected static String getEntityBeansXml() {
        return getBeansXml(!ADD_SESSION, ADD_ENTITY, !ADD_MDB);
    }
    
    protected static String getMessageDrivenBeansXml() {
        return getBeansXml(!ADD_SESSION, !ADD_ENTITY, ADD_MDB);
    }    

    protected static final boolean ADD_SESSION = true;
    protected static final boolean ADD_ENTITY = true;
    protected static final boolean ADD_MDB = true;
    
    protected static String getBeansXml(boolean addSession, boolean addEntity, boolean addMdb) {
        StringBuilder builder = new StringBuilder();
        builder.append("<enterprise-beans>\n");

        if ( addSession ) {
            builder.append("<session>\n");
            builder.append("<ejb-name>TestSession</ejb-name>\n");
            builder.append(DDJakarta10Elements.CONTEXT_SERVICE_XML);
            builder.append(DDJakarta10Elements.MANAGED_EXECUTOR_XML);
            builder.append(DDJakarta10Elements.MANAGED_SCHEDULED_EXECUTOR_XML);
            builder.append(DDJakarta10Elements.MANAGED_THREAD_FACTORY_XML);
            builder.append("</session>\n");
        }

        if ( addEntity ) {
            builder.append("<entity>\n");
            builder.append("<ejb-name>TestEntity</ejb-name>\n");
            builder.append("<ejb-class>TestEntity.ejb.class.name</ejb-class>\n");
            builder.append("<mapped-name>TestEntity.ejb.mapped.name</mapped-name>\n");
            // Change the order to force differences in the parse errors.
            // The first unexpected element shows up in the error.
            builder.append(DDJakarta10Elements.MANAGED_EXECUTOR_XML);
            builder.append(DDJakarta10Elements.MANAGED_SCHEDULED_EXECUTOR_XML);
            builder.append(DDJakarta10Elements.MANAGED_THREAD_FACTORY_XML);
            builder.append(DDJakarta10Elements.CONTEXT_SERVICE_XML);
            builder.append("</entity>\n");
        }

        if ( addMdb ) {
            builder.append("<message-driven>\n");
            builder.append("<ejb-name>TestMessageDriven</ejb-name>\n");
            builder.append("<ejb-class>TestMessageDriven.ejb.class.name</ejb-class>\n");
            builder.append("<mapped-name>TestMessageDriven.ejb.mapped.name</mapped-name>\n");
            // Change the order to force differences in the parse errors.
            // The first unexpected element shows up in the error.            
            builder.append(DDJakarta10Elements.MANAGED_SCHEDULED_EXECUTOR_XML);
            builder.append(DDJakarta10Elements.MANAGED_THREAD_FACTORY_XML);
            builder.append(DDJakarta10Elements.CONTEXT_SERVICE_XML);
            builder.append(DDJakarta10Elements.MANAGED_EXECUTOR_XML);
            builder.append("</message-driven>\n");
        }

        builder.append("</enterprise-beans>\n");
        
        return builder.toString();
    }

    //

    @Test
    public void testEnterpriseBean() throws Exception {
        List<EnterpriseBean> beans =
            parseEJBJarMax( ejbJar11(ejb1XML) )
                .getEnterpriseBeans();

        Assert.assertEquals(3, beans.size());

        EnterpriseBean bean0 = beans.get(0);
        Assert.assertEquals("TestSession", bean0.getName());
        Assert.assertEquals(null, bean0.getEjbClassName());
        Assert.assertEquals(EnterpriseBean.KIND_SESSION, bean0.getKindValue());

        EnterpriseBean bean1 = beans.get(1);
        Assert.assertEquals("TestEntity", bean1.getName());
        Assert.assertEquals(EnterpriseBean.KIND_ENTITY, bean1.getKindValue());
        Assert.assertEquals("TestEntity.ejb.class.name", bean1.getEjbClassName());
        Assert.assertEquals("TestEntity.ejb.mapped.name", bean1.getMappedName());

        EnterpriseBean bean2 = beans.get(2);
        Assert.assertEquals("TestMessageDriven", bean2.getName());
        Assert.assertEquals(EnterpriseBean.KIND_MESSAGE_DRIVEN, bean2.getKindValue());
        Assert.assertEquals("TestMessageDriven.ejb.class.name", bean2.getEjbClassName());
        Assert.assertEquals("TestMessageDriven.ejb.mapped.name", bean2.getMappedName());
        Assert.assertEquals(null, bean2.getSecurityIdentity());
    }
    
    // EE10 element testing ...
    
    @Test
    public void testEE10SessionEJB32() throws Exception {
        parseEJBJar( ejbJar32( getSessionBeansXml() ), EJBJar.VERSION_3_2,
                     "unexpected.child.element",
                     "CWWKC2259E", "context-service", "ejb-jar.xml" );
    }    
    
    @Test
    public void testEE10EntityEJB32() throws Exception {
        parseEJBJar( ejbJar32( getEntityBeansXml() ), EJBJar.VERSION_3_2,
                     "unexpected.child.element",
                     "CWWKC2259E", "managed-executor", "ejb-jar.xml" );
    }
    
    @Test
    public void testEE10MessageDrivenEJB32() throws Exception {
        parseEJBJar( ejbJar32( getMessageDrivenBeansXml() ), EJBJar.VERSION_3_2,
                     "unexpected.child.element",
                     "CWWKC2259E", "managed-scheduled-executor", "ejb-jar.xml" );
    }

    //

    @Test
    public void testEE10SessionEJB40() throws Exception {
        parseEJBJar( ejbJar40( getSessionBeansXml() ), EJBJar.VERSION_4_0,
                "unexpected.child.element",
                "CWWKC2259E", "context-service", "ejb-jar.xml" );
    }
     
// Issue 20386: There is no EJB 5.0 for Jakarta EE 10.  The new elements
//              are not supported by EJB.
//
//    @Test
//    public void testEE10SessionEJB50() throws Exception {
//        EJBJar ejbJar = parseEJBJar(
//                ejbJar40( getSessionBeansXml() ),
//                EJBJar.VERSION_5_0);
//
//        List<String> names = DDJakarta10Elements.names("EJBJar", "enterpriseBeans");
//
//        List<EnterpriseBean> ejbs = ejbJar.getEnterpriseBeans();
//        DDJakarta10Elements.verifySize(names, 1, ejbs);
//
//        EnterpriseBean sessionBean = ejbs.get(0);
//        if ( !(sessionBean instanceof Session) ) {
//            Assert.fail("EJBJar.enterpriseBeans.[0] is not a session bean");
//        }
//
//        DDJakarta10Elements.withName(names, "[0]",
//                (useNames) -> DDJakarta10Elements.verifyEE10(useNames, sessionBean) );
//    }
    
    @Test
    public void testEE10EntityEJB40() throws Exception {
        parseEJBJar( ejbJar40( getEntityBeansXml() ), EJBJar.VERSION_4_0,
                "unexpected.child.element",
                "CWWKC2259E", "managed-executor", "ejb-jar.xml" );
    }

// Issue 20386: There is no EJB 5.0 for Jakarta EE 10.
//
//    @Test
//    public void testEE10EntityEJB50() throws Exception {
//        EJBJar ejbJar = parseEJBJar(
//                ejbJar40( getEntityBeansXml() ),
//                EJBJar.VERSION_5_0);
//
//        List<String> names = DDJakarta10Elements.names("EJBJar", "enterpriseBeans");
//
//        List<EnterpriseBean> ejbs = ejbJar.getEnterpriseBeans();
//        DDJakarta10Elements.verifySize(names, 1, ejbs);
//
//        EnterpriseBean entityBean = ejbs.get(0);
//        if ( !(entityBean instanceof Entity) ) {
//            Assert.fail("EJBJar.enterpriseBeans.[0] is not an entity bean");
//        }
//
//        DDJakarta10Elements.withName(names, "[0]",
//                (useNames) -> DDJakarta10Elements.verifyEE10(useNames, entityBean) );
//    }
    
    @Test
    public void testEE10MessageDrivenEJB40() throws Exception {
        parseEJBJar( ejbJar40( getMessageDrivenBeansXml() ), EJBJar.VERSION_4_0,
                "unexpected.child.element",
                "CWWKC2259E", "managed-scheduled-executor", "ejb-jar.xml" );
    }

// Issue 20386: There is no EJB 5.0 for Jakarta EE 10.
//   
//    @Test
//    public void testEE10MessageDrivenEJB50() throws Exception {    
//        EJBJar ejbJar = parseEJBJar(
//                ejbJar40( getMessageDrivenBeansXml() ),
//                EJBJar.VERSION_5_0);
//
//        List<String> names = DDJakarta10Elements.names("EJBJar", "enterpriseBeans");
//
//        List<EnterpriseBean> ejbs = ejbJar.getEnterpriseBeans();
//        DDJakarta10Elements.verifySize(names, 1, ejbs);
//
//        EnterpriseBean mdBean = ejbs.get(0);
//        if ( !(mdBean instanceof MessageDriven) ) {
//            Assert.fail("EJBJar.enterpriseBeans.[0] is not a message driven");
//        }
//        DDJakarta10Elements.withName(names, "[0]",
//                (useNames) -> DDJakarta10Elements.verifyEE10(useNames, mdBean) );
//    }
}
