/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ejb;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.common.RunAs;
import com.ibm.ws.javaee.dd.common.SecurityRoleRef;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejb.SecurityIdentity;

public class EnterpriseBeanTest extends EJBJarTestBase {

    @Test
    public void testEnterpriseBeanSecurity() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar20() +
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

                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();

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

    @Test
    public void testEnterpriseBean() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
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

                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();

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

}
