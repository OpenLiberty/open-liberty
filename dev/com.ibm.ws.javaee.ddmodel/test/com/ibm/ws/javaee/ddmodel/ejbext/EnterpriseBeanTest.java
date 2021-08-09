/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ejbext;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.commonext.GlobalTransaction;
import com.ibm.ws.javaee.dd.commonext.LocalTransaction;
import com.ibm.ws.javaee.dd.commonext.ResourceRef;
import com.ibm.ws.javaee.dd.ejbext.BeanCache;
import com.ibm.ws.javaee.dd.ejbext.EJBJarExt;
import com.ibm.ws.javaee.dd.ejbext.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejbext.RunAsMode;
import com.ibm.ws.javaee.dd.ejbext.RunAsModeBase;
import com.ibm.ws.javaee.dd.ejbext.Session;
import com.ibm.ws.javaee.dd.ejbext.StartAtAppStart;

public class EnterpriseBeanTest extends EJBJarExtTestBase {

    @Test
    public void testEnterpriseBeanEmpty() throws Exception {
        List<EnterpriseBean> sessionBeans = getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                                                         "<session name=\"session0\">" +
                                                         "</session>" +
                                                         "<session name=\"session1\">" +
                                                         "</session>" +
                                                         "<session name=\"session2\">" +
                                                         "</session>" +
                                                         "</ejb-jar-ext>").getEnterpriseBeans();

        Assert.assertEquals("Should be some beans in here", 3, sessionBeans.size());

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = sessionBeans.get(0);
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean1 = sessionBeans.get(1);
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean2 = sessionBeans.get(2);
        Assert.assertEquals("should be session0", "session0", bean0.getName());
        Assert.assertEquals("should be session1", "session1", bean1.getName());
        Assert.assertEquals("should be session2", "session2", bean2.getName());
        Session session = (Session) bean0;
        Assert.assertEquals("should be null", null, session.getTimeOut());
        Assert.assertEquals("should be null", null, bean0.getBeanCache());
        Assert.assertEquals("should be null", null, bean0.getGlobalTransaction());
        Assert.assertEquals("should be null", null, bean0.getLocalTransaction());
        Assert.assertEquals("should be zero", 0, bean0.getResourceRefs().size());
        Assert.assertEquals("should be zero", 0, bean0.getRunAsModes().size());
        Assert.assertEquals("should be null", null, bean0.getStartAtAppStart());
    }

    @Test
    public void testXMIEnterpriseBeanEmpty() throws Exception {
        List<EnterpriseBean> sessionBeans = parseEJBJarExtension(ejbJarExtension("") +
                                                                 "<ejbExtensions xmi:type=\"ejbext:SessionExtension\">" +
                                                                 "  <enterpriseBean xmi:type=\"ejb:Session\" href=\"META-INF/ejb-jar.xml#s0\"/>" +
                                                                 "</ejbExtensions>" +
                                                                 "<ejbExtensions xmi:type=\"ejbext:SessionExtension\">" +
                                                                 "  <enterpriseBean xmi:type=\"ejb:Session\" href=\"META-INF/ejb-jar.xml#s1\"/>" +
                                                                 "</ejbExtensions>" +
                                                                 "<ejbExtensions xmi:type=\"ejbext:SessionExtension\">" +
                                                                 "  <enterpriseBean xmi:type=\"ejb:Session\" href=\"META-INF/ejb-jar.xml#s2\"/>" +
                                                                 "</ejbExtensions>" +
                                                                 "</ejbext:EJBJarExtension>",
                                                                 parseEJBJar(ejbJar21() +
                                                                             "<enterprise-beans>" +
                                                                             "  <session id=\"s0\">" +
                                                                             "    <ejb-name>session0</ejb-name>" +
                                                                             "  </session>" +
                                                                             "  <session id=\"s1\">" +
                                                                             "    <ejb-name>session1</ejb-name>" +
                                                                             "  </session>" +
                                                                             "  <session id=\"s2\">" +
                                                                             "    <ejb-name>session2</ejb-name>" +
                                                                             "  </session>" +
                                                                             "</enterprise-beans>" +
                                                                             "</ejb-jar>")).getEnterpriseBeans();

        Assert.assertEquals("Should be some beans in here", 3, sessionBeans.size());

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = sessionBeans.get(0);
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean1 = sessionBeans.get(1);
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean2 = sessionBeans.get(2);
        Assert.assertEquals("should be session0", "session0", bean0.getName());
        Assert.assertEquals("should be session1", "session1", bean1.getName());
        Assert.assertEquals("should be session2", "session2", bean2.getName());
        Session session = (Session) bean0;
        Assert.assertEquals("should be null", null, session.getTimeOut());
        Assert.assertEquals("should be null", null, bean0.getBeanCache());
        Assert.assertEquals("should be null", null, bean0.getGlobalTransaction());
        Assert.assertEquals("should be null", null, bean0.getLocalTransaction());
        Assert.assertEquals("should be zero", 0, bean0.getResourceRefs().size());
        Assert.assertEquals("should be zero", 0, bean0.getRunAsModes().size());
        Assert.assertEquals("should be null", null, bean0.getStartAtAppStart());
    }

    @Test
    public void testEnterpriseBeanGetMultipleTimes() throws Exception {
        EJBJarExt ejbJarExt = getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                                           "<session name=\"session0\">" +
                                           "</session>" +
                                           "<session name=\"session1\">" +
                                           "</session>" +
                                           "<session name=\"session2\">" +
                                           "</session>" +
                                           "<message-driven name=\"md0\">" +
                                           "</message-driven>" +
                                           "<message-driven name=\"md1\">" +
                                           "</message-driven>" +
                                           "<message-driven name=\"md2\">" +
                                           "</message-driven>" +
                                           "</ejb-jar-ext>");
        Assert.assertEquals("Should be 3 session beans in here", 6, ejbJarExt.getEnterpriseBeans().size());
        Assert.assertEquals("Should be 3 session beans in here", 6, ejbJarExt.getEnterpriseBeans().size());
        Assert.assertEquals("Should be 3 session beans in here", 6, ejbJarExt.getEnterpriseBeans().size());
    }

    @Test
    public void testXMIIgnored() throws Exception {
        parseEJBJarExtension(ejbJarExtension("") +
                             "<ejbExtensions xmi:type=\"ejbext:SessionExtension\" timeout=\"42\">" +
                             "  <enterpriseBean xmi:type=\"ejb:Session\" href=\"META-INF/ejb-jar.xml#s0\"/>" +
                             "  <structure inheritenceRoot=\"true\"/>" +
                             "  <internationalization invocationLocale=\"CALLER\"/>" +
                             "  <internationalization invocationLocale=\"SERVER\"/>" +
                             "</ejbExtensions>" +
                             "</ejbext:EJBJarExtension>",
                             parseEJBJar(ejbJar21() +
                                         "<enterprise-beans>" +
                                         "  <session id=\"s0\">" +
                                         "    <ejb-name>session0</ejb-name>" +
                                         "  </session>" +
                                         "</enterprise-beans>" +
                                         "</ejb-jar>")).getEnterpriseBeans();
    }

    @Test
    public void testEnterpriseBeanSessionTimeout() throws Exception {
        List<EnterpriseBean> sessionBeans = getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                                                         "<session name=\"session0\">" +
                                                         "<time-out value=\"42\">" +
                                                         "</time-out>" +
                                                         "</session>" +
                                                         "</ejb-jar-ext>").getEnterpriseBeans();
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = sessionBeans.get(0);
        Assert.assertEquals("should be session0", "session0", bean0.getName());
        Session session = (Session) bean0;
        Assert.assertEquals("should be 42", 42, session.getTimeOut().getValue());
    }

    @Test
    public void testXMIEnterpriseBeanSessionTimeout() throws Exception {
        List<EnterpriseBean> sessionBeans = parseEJBJarExtension(ejbJarExtension("") +
                                                                 "<ejbExtensions xmi:type=\"ejbext:SessionExtension\" timeout=\"42\">" +
                                                                 "  <enterpriseBean xmi:type=\"ejb:Session\" href=\"META-INF/ejb-jar.xml#s0\"/>" +
                                                                 "</ejbExtensions>" +
                                                                 "</ejbext:EJBJarExtension>",
                                                                 parseEJBJar(ejbJar21() +
                                                                             "<enterprise-beans>" +
                                                                             "  <session id=\"s0\">" +
                                                                             "    <ejb-name>session0</ejb-name>" +
                                                                             "  </session>" +
                                                                             "</enterprise-beans>" +
                                                                             "</ejb-jar>")).getEnterpriseBeans();
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = sessionBeans.get(0);
        Assert.assertEquals("should be session0", "session0", bean0.getName());
        Session session = (Session) bean0;
        Assert.assertEquals("should be 42", 42, session.getTimeOut().getValue());
    }

    @Test
    public void testEnterpriseBeanBeanCache() throws Exception {
        List<EnterpriseBean> sessionBeans = getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                                                         "<session name=\"session0\">" +
                                                         "<bean-cache activation-policy=\"ONCE\"/>" +
                                                         "</session>" +

                                                         "<session name=\"session1\">" +
                                                         "<bean-cache activation-policy=\"ACTIVITY_SESSION\"/>" +
                                                         "</session>" +

                                                         "<session name=\"session2\">" +
                                                         "<bean-cache activation-policy=\"TRANSACTION\"/>" +
                                                         "</session>" +

                                                         "<session name=\"session3\">" +
                                                         "<bean-cache/>" +
                                                         "</session>" +

                                                         "</ejb-jar-ext>").getEnterpriseBeans();
        Assert.assertEquals("Should be some beans in here", 4, sessionBeans.size());
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = sessionBeans.get(0);
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean1 = sessionBeans.get(1);
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean2 = sessionBeans.get(2);
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean3 = sessionBeans.get(3);
        BeanCache beanCache0 = bean0.getBeanCache();
        BeanCache beanCache1 = bean1.getBeanCache();
        BeanCache beanCache2 = bean2.getBeanCache();
        BeanCache beanCache3 = bean3.getBeanCache();
        Assert.assertEquals("Should be activation-policy ONCE", BeanCache.ActivationPolicyTypeEnum.ONCE, beanCache0.getActivationPolicy());
        Assert.assertEquals("Should be activation-policy ACTIVITY_SESSION", BeanCache.ActivationPolicyTypeEnum.ACTIVITY_SESSION, beanCache1.getActivationPolicy());
        Assert.assertEquals("Should be activation-policy TRANSACTION", BeanCache.ActivationPolicyTypeEnum.TRANSACTION, beanCache2.getActivationPolicy());
        Assert.assertEquals("Should be null", null, beanCache3.getActivationPolicy());
    }

    @Test
    public void testXMIEnterpriseBeanBeanCache() throws Exception {
        List<EnterpriseBean> sessionBeans = parseEJBJarExtension(ejbJarExtension("") +
                                                                 "<ejbExtensions xmi:type=\"ejbext:SessionExtension\">" +
                                                                 "  <enterpriseBean xmi:type=\"ejb:Session\" href=\"META-INF/ejb-jar.xml#s0\"/>" +
                                                                 "  <beanCache activateAt=\"ONCE\"/>" +
                                                                 "</ejbExtensions>" +
                                                                 "<ejbExtensions xmi:type=\"ejbext:SessionExtension\">" +
                                                                 "  <enterpriseBean xmi:type=\"ejb:Session\" href=\"META-INF/ejb-jar.xml#s1\"/>" +
                                                                 "  <beanCache activateAt=\"ACTIVITY_SESSION\"/>" +
                                                                 "</ejbExtensions>" +
                                                                 "<ejbExtensions xmi:type=\"ejbext:SessionExtension\">" +
                                                                 "  <enterpriseBean xmi:type=\"ejb:Session\" href=\"META-INF/ejb-jar.xml#s2\"/>" +
                                                                 "  <beanCache activateAt=\"TRANSACTION\"/>" +
                                                                 "</ejbExtensions>" +
                                                                 "<ejbExtensions xmi:type=\"ejbext:SessionExtension\">" +
                                                                 "  <enterpriseBean xmi:type=\"ejb:Session\" href=\"META-INF/ejb-jar.xml#s3\"/>" +
                                                                 "  <beanCache/>" +
                                                                 "</ejbExtensions>" +
                                                                 "</ejbext:EJBJarExtension>",
                                                                 parseEJBJar(ejbJar21() +
                                                                             "<enterprise-beans>" +
                                                                             "  <session id=\"s0\">" +
                                                                             "    <ejb-name>session0</ejb-name>" +
                                                                             "  </session>" +
                                                                             "  <session id=\"s1\">" +
                                                                             "    <ejb-name>session1</ejb-name>" +
                                                                             "  </session>" +
                                                                             "  <session id=\"s2\">" +
                                                                             "    <ejb-name>session2</ejb-name>" +
                                                                             "  </session>" +
                                                                             "  <session id=\"s3\">" +
                                                                             "    <ejb-name>session3</ejb-name>" +
                                                                             "  </session>" +
                                                                             "</enterprise-beans>" +
                                                                             "</ejb-jar>")).getEnterpriseBeans();
        Assert.assertEquals("Should be some beans in here", 4, sessionBeans.size());
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = sessionBeans.get(0);
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean1 = sessionBeans.get(1);
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean2 = sessionBeans.get(2);
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean3 = sessionBeans.get(3);
        BeanCache beanCache0 = bean0.getBeanCache();
        BeanCache beanCache1 = bean1.getBeanCache();
        BeanCache beanCache2 = bean2.getBeanCache();
        BeanCache beanCache3 = bean3.getBeanCache();
        Assert.assertEquals("Should be activation-policy ONCE", BeanCache.ActivationPolicyTypeEnum.ONCE, beanCache0.getActivationPolicy());
        Assert.assertEquals("Should be activation-policy ACTIVITY_SESSION", BeanCache.ActivationPolicyTypeEnum.ACTIVITY_SESSION, beanCache1.getActivationPolicy());
        Assert.assertEquals("Should be activation-policy TRANSACTION", BeanCache.ActivationPolicyTypeEnum.TRANSACTION, beanCache2.getActivationPolicy());
        Assert.assertEquals("Should be null", null, beanCache3.getActivationPolicy());
    }

    @Test
    public void testEnterpriseBeanLocalTransaction() throws Exception {
        List<EnterpriseBean> messageDrivenBeans = getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                                                               "<message-driven name=\"md0\">" +
                                                               "<local-transaction/>" +
                                                               "</message-driven>" +

                                                               "<message-driven name=\"md1\">" +
                                                               "<local-transaction boundary=\"ACTIVITY_SESSION\" resolver=\"APPLICATION\"" +
                                                               " unresolved-action=\"ROLLBACK\" shareable=\"true\"/>" +
                                                               "</message-driven>" +

                                                               "<message-driven name=\"md2\">" +
                                                               "<local-transaction boundary=\"BEAN_METHOD\" resolver=\"CONTAINER_AT_BOUNDARY\"" +
                                                               " unresolved-action=\"COMMIT\" shareable=\"false\"/>" +
                                                               "</message-driven>" +

                                                               "</ejb-jar-ext>").getEnterpriseBeans();

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = messageDrivenBeans.get(0);
        Assert.assertEquals("should be md0", "md0", bean0.getName());
        LocalTransaction lt0 = bean0.getLocalTransaction();
        Assert.assertFalse("Should be false", lt0.isSetBoundary());
        Assert.assertNull(lt0.getBoundary());
        Assert.assertFalse("Should be false", lt0.isSetResolver());
        Assert.assertNull(lt0.getResolver());
        Assert.assertFalse("Should be false", lt0.isSetUnresolvedAction());
        Assert.assertNull(lt0.getUnresolvedAction());
        Assert.assertFalse("Should be false", lt0.isSetShareable());

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean1 = messageDrivenBeans.get(1);
        Assert.assertEquals("md1", bean1.getName());
        LocalTransaction lt1 = bean1.getLocalTransaction();
        Assert.assertTrue(lt1.isSetBoundary());
        Assert.assertEquals(LocalTransaction.BoundaryEnum.ACTIVITY_SESSION, lt1.getBoundary());
        Assert.assertTrue(lt1.isSetResolver());
        Assert.assertEquals(LocalTransaction.ResolverEnum.APPLICATION, lt1.getResolver());
        Assert.assertTrue(lt1.isSetUnresolvedAction());
        Assert.assertEquals(LocalTransaction.UnresolvedActionEnum.ROLLBACK, lt1.getUnresolvedAction());
        Assert.assertTrue(lt1.isSetShareable());
        Assert.assertTrue(lt1.isShareable());

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean2 = messageDrivenBeans.get(2);
        Assert.assertEquals("md2", bean2.getName());
        LocalTransaction lt2 = bean2.getLocalTransaction();
        Assert.assertTrue(lt2.isSetBoundary());
        Assert.assertEquals(LocalTransaction.BoundaryEnum.BEAN_METHOD, lt2.getBoundary());
        Assert.assertTrue(lt2.isSetResolver());
        Assert.assertEquals(LocalTransaction.ResolverEnum.CONTAINER_AT_BOUNDARY, lt2.getResolver());
        Assert.assertTrue(lt2.isSetUnresolvedAction());
        Assert.assertEquals(LocalTransaction.UnresolvedActionEnum.COMMIT, lt2.getUnresolvedAction());
        Assert.assertTrue(lt2.isSetShareable());
        Assert.assertFalse(lt2.isShareable());
    }

    @Test
    public void testXMIEnterpriseBeanLocalTransaction() throws Exception {
        List<EnterpriseBean> messageDrivenBeans = parseEJBJarExtension(ejbJarExtension("") +
                                                                       "<ejbExtensions xmi:type=\"ejbext:MessageDrivenExtension\">" +
                                                                       "  <enterpriseBean xmi:type=\"ejb:MessageDriven\" href=\"META-INF/ejb-jar.xml#md0id\"/>" +
                                                                       "  <localTransaction/>" +
                                                                       "</ejbExtensions>" +
                                                                       "<ejbExtensions xmi:type=\"ejbext:MessageDrivenExtension\">" +
                                                                       "  <enterpriseBean xmi:type=\"ejb:MessageDriven\" href=\"META-INF/ejb-jar.xml#md1id\"/>" +
                                                                       "  <localTransaction boundary=\"ActivitySession\" resolver=\"Application\"" +
                                                                       "   unresolvedAction=\"Rollback\" shareable=\"true\"/>" +
                                                                       "</ejbExtensions>" +
                                                                       "<ejbExtensions xmi:type=\"ejbext:MessageDrivenExtension\">" +
                                                                       "  <enterpriseBean xmi:type=\"ejb:MessageDriven\" href=\"META-INF/ejb-jar.xml#md2id\"/>" +
                                                                       "  <localTransaction boundary=\"BeanMethod\" resolver=\"ContainerAtBoundary\"" +
                                                                       "   unresolvedAction=\"Commit\" shareable=\"false\"/>" +
                                                                       "</ejbExtensions>" +
                                                                       "</ejbext:EJBJarExtension>",
                                                                       parseEJBJar(ejbJar21() +
                                                                                   "<enterprise-beans>" +
                                                                                   "  <message-driven id=\"md0id\">" +
                                                                                   "    <ejb-name>md0</ejb-name>" +
                                                                                   "  </message-driven>" +
                                                                                   "  <message-driven id=\"md1id\">" +
                                                                                   "    <ejb-name>md1</ejb-name>" +
                                                                                   "  </message-driven>" +
                                                                                   "  <message-driven id=\"md2id\">" +
                                                                                   "    <ejb-name>md2</ejb-name>" +
                                                                                   "  </message-driven>" +
                                                                                   "</enterprise-beans>" +
                                                                                   "</ejb-jar>")).getEnterpriseBeans();

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = messageDrivenBeans.get(0);
        Assert.assertEquals("should be md0", "md0", bean0.getName());
        LocalTransaction lt0 = bean0.getLocalTransaction();
        Assert.assertFalse("Should be false", lt0.isSetBoundary());
        Assert.assertFalse("Should be false", lt0.isSetResolver());
        Assert.assertFalse("Should be false", lt0.isSetUnresolvedAction());
        Assert.assertFalse("Should be false", lt0.isSetShareable());

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean1 = messageDrivenBeans.get(1);
        Assert.assertEquals("md1", bean1.getName());
        LocalTransaction lt1 = bean1.getLocalTransaction();
        Assert.assertTrue(lt1.isSetBoundary());
        Assert.assertEquals(LocalTransaction.BoundaryEnum.ACTIVITY_SESSION, lt1.getBoundary());
        Assert.assertTrue(lt1.isSetResolver());
        Assert.assertEquals(LocalTransaction.ResolverEnum.APPLICATION, lt1.getResolver());
        Assert.assertTrue(lt1.isSetUnresolvedAction());
        Assert.assertEquals(LocalTransaction.UnresolvedActionEnum.ROLLBACK, lt1.getUnresolvedAction());
        Assert.assertTrue(lt1.isSetShareable());
        Assert.assertTrue(lt1.isShareable());

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean2 = messageDrivenBeans.get(2);
        Assert.assertEquals("md2", bean2.getName());
        LocalTransaction lt2 = bean2.getLocalTransaction();
        Assert.assertTrue(lt2.isSetBoundary());
        Assert.assertEquals(LocalTransaction.BoundaryEnum.BEAN_METHOD, lt2.getBoundary());
        Assert.assertTrue(lt2.isSetResolver());
        Assert.assertEquals(LocalTransaction.ResolverEnum.CONTAINER_AT_BOUNDARY, lt2.getResolver());
        Assert.assertTrue(lt2.isSetUnresolvedAction());
        Assert.assertEquals(LocalTransaction.UnresolvedActionEnum.COMMIT, lt2.getUnresolvedAction());
        Assert.assertTrue(lt2.isSetShareable());
        Assert.assertFalse(lt2.isShareable());
    }

    @Test
    public void testXMIEnterpriseBeanLocalTran() throws Exception {
        List<EnterpriseBean> messageDrivenBeans = parseEJBJarExtension(ejbJarExtension("") +
                                                                       "<ejbExtensions xmi:type=\"ejbext:MessageDrivenExtension\">" +
                                                                       "  <enterpriseBean xmi:type=\"ejb:MessageDriven\" href=\"META-INF/ejb-jar.xml#md0id\"/>" +
                                                                       "  <localTran/>" +
                                                                       "</ejbExtensions>" +
                                                                       "<ejbExtensions xmi:type=\"ejbext:MessageDrivenExtension\">" +
                                                                       "  <enterpriseBean xmi:type=\"ejb:MessageDriven\" href=\"META-INF/ejb-jar.xml#md1id\"/>" +
                                                                       "  <localTran boundary=\"ACTIVITY_SESSION\" resolver=\"BEAN\" unresolvedAction=\"ROLLBACK\"/>" +
                                                                       "</ejbExtensions>" +
                                                                       "<ejbExtensions xmi:type=\"ejbext:MessageDrivenExtension\">" +
                                                                       "  <enterpriseBean xmi:type=\"ejb:MessageDriven\" href=\"META-INF/ejb-jar.xml#md2id\"/>" +
                                                                       "  <localTran boundary=\"BEAN_METHOD\" resolver=\"CONTAINER\" unresolvedAction=\"COMMIT\" />" +
                                                                       "</ejbExtensions>" +
                                                                       "</ejbext:EJBJarExtension>",
                                                                       parseEJBJar(ejbJar21() +
                                                                                   "<enterprise-beans>" +
                                                                                   "  <message-driven id=\"md0id\">" +
                                                                                   "    <ejb-name>md0</ejb-name>" +
                                                                                   "  </message-driven>" +
                                                                                   "  <message-driven id=\"md1id\">" +
                                                                                   "    <ejb-name>md1</ejb-name>" +
                                                                                   "  </message-driven>" +
                                                                                   "  <message-driven id=\"md2id\">" +
                                                                                   "    <ejb-name>md2</ejb-name>" +
                                                                                   "  </message-driven>" +
                                                                                   "</enterprise-beans>" +
                                                                                   "</ejb-jar>")).getEnterpriseBeans();

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = messageDrivenBeans.get(0);
        Assert.assertEquals("should be md0", "md0", bean0.getName());
        LocalTransaction lt0 = bean0.getLocalTransaction();
        Assert.assertFalse("Should be false", lt0.isSetBoundary());
        Assert.assertFalse("Should be false", lt0.isSetResolver());
        Assert.assertFalse("Should be false", lt0.isSetUnresolvedAction());
        Assert.assertFalse("Should be false", lt0.isSetShareable());

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean1 = messageDrivenBeans.get(1);
        Assert.assertEquals("md1", bean1.getName());
        LocalTransaction lt1 = bean1.getLocalTransaction();
        Assert.assertTrue(lt1.isSetBoundary());
        Assert.assertEquals(LocalTransaction.BoundaryEnum.ACTIVITY_SESSION, lt1.getBoundary());
        Assert.assertTrue(lt1.isSetResolver());
        Assert.assertEquals(LocalTransaction.ResolverEnum.APPLICATION, lt1.getResolver());
        Assert.assertTrue(lt1.isSetUnresolvedAction());
        Assert.assertEquals(LocalTransaction.UnresolvedActionEnum.ROLLBACK, lt1.getUnresolvedAction());
        Assert.assertFalse(lt1.isSetShareable());

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean2 = messageDrivenBeans.get(2);
        Assert.assertEquals("md2", bean2.getName());
        LocalTransaction lt2 = bean2.getLocalTransaction();
        Assert.assertTrue(lt2.isSetBoundary());
        Assert.assertEquals(LocalTransaction.BoundaryEnum.BEAN_METHOD, lt2.getBoundary());
        Assert.assertTrue(lt2.isSetResolver());
        Assert.assertEquals(LocalTransaction.ResolverEnum.CONTAINER_AT_BOUNDARY, lt2.getResolver());
        Assert.assertTrue(lt2.isSetUnresolvedAction());
        Assert.assertEquals(LocalTransaction.UnresolvedActionEnum.COMMIT, lt2.getUnresolvedAction());
        Assert.assertFalse(lt2.isSetShareable());
    }

    @Test
    public void testEnterpriseBeanShareableLocalTransaction() throws Exception {
        List<EnterpriseBean> messageDrivenBeans = getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                                                               "<message-driven name=\"md0\">" +
                                                               "<local-transaction shareable=\"true\">" +
                                                               "</local-transaction>" +
                                                               "</message-driven>" +

                                                               "</ejb-jar-ext>").getEnterpriseBeans();

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = messageDrivenBeans.get(0);
        Assert.assertEquals("should be md0", "md0", bean0.getName());
        LocalTransaction lt0 = bean0.getLocalTransaction();
        Assert.assertFalse("Should be false", lt0.isSetBoundary());
        Assert.assertFalse("Should be false", lt0.isSetResolver());
        Assert.assertFalse("Should be false", lt0.isSetUnresolvedAction());
        Assert.assertTrue("Should be true", lt0.isSetShareable());
        Assert.assertTrue("Should be true", lt0.isShareable());

        messageDrivenBeans = getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                                          "<message-driven name=\"md0\">" +
                                          "<local-transaction shareable=\"false\">" +
                                          "</local-transaction>" +
                                          "</message-driven>" +

                                          "</ejb-jar-ext>").getEnterpriseBeans();

        bean0 = messageDrivenBeans.get(0);
        Assert.assertEquals("should be md0", "md0", bean0.getName());
        lt0 = bean0.getLocalTransaction();
        Assert.assertFalse("Should be false", lt0.isSetBoundary());
        Assert.assertFalse("Should be false", lt0.isSetResolver());
        Assert.assertFalse("Should be false", lt0.isSetUnresolvedAction());
        Assert.assertTrue("Should be true", lt0.isSetShareable());
        Assert.assertFalse("Should be false", lt0.isShareable());
    }

    @Test
    public void testXMIEnterpriseBeanShareableLocalTransaction() throws Exception {
        List<EnterpriseBean> messageDrivenBeans = parseEJBJarExtension(ejbJarExtension("") +
                                                                       "<ejbExtensions xmi:type=\"ejbext:MessageDrivenExtension\">" +
                                                                       "  <enterpriseBean xmi:type=\"ejb:MessageDriven\" href=\"META-INF/ejb-jar.xml#md0id\"/>" +
                                                                       "  <localTransaction shareable=\"true\"/>" +
                                                                       "</ejbExtensions>" +
                                                                       "</ejbext:EJBJarExtension>",
                                                                       parseEJBJar(ejbJar21() +
                                                                                   "<enterprise-beans>" +
                                                                                   "  <message-driven id=\"md0id\">" +
                                                                                   "    <ejb-name>md0</ejb-name>" +
                                                                                   "  </message-driven>" +
                                                                                   "</enterprise-beans>" +
                                                                                   "</ejb-jar>")).getEnterpriseBeans();

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = messageDrivenBeans.get(0);
        Assert.assertEquals("should be md0", "md0", bean0.getName());
        LocalTransaction lt0 = bean0.getLocalTransaction();
        Assert.assertFalse("Should be false", lt0.isSetBoundary());
        Assert.assertFalse("Should be false", lt0.isSetResolver());
        Assert.assertFalse("Should be false", lt0.isSetUnresolvedAction());
        Assert.assertTrue("Should be true", lt0.isSetShareable());
        Assert.assertTrue("Should be true", lt0.isShareable());

        messageDrivenBeans = getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                                          "<message-driven name=\"md0\">" +
                                          "<local-transaction shareable=\"false\">" +
                                          "</local-transaction>" +
                                          "</message-driven>" +

                                          "</ejb-jar-ext>").getEnterpriseBeans();

        bean0 = messageDrivenBeans.get(0);
        Assert.assertEquals("should be md0", "md0", bean0.getName());
        lt0 = bean0.getLocalTransaction();
        Assert.assertFalse("Should be false", lt0.isSetBoundary());
        Assert.assertFalse("Should be false", lt0.isSetResolver());
        Assert.assertFalse("Should be false", lt0.isSetUnresolvedAction());
        Assert.assertTrue("Should be true", lt0.isSetShareable());
        Assert.assertFalse("Should be false", lt0.isShareable());
    }

    @Test
    public void testEnterpriseBeanGlobalTransaction() throws Exception {
        List<EnterpriseBean> messageDrivenBeans = getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                                                               "<message-driven name=\"md0\">" +
                                                               "<global-transaction>" +
                                                               "</global-transaction>" +
                                                               "</message-driven>" +
                                                               "</ejb-jar-ext>").getEnterpriseBeans();

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = messageDrivenBeans.get(0);
        GlobalTransaction gt0 = bean0.getGlobalTransaction();
        Assert.assertFalse("Should be false", gt0.isSendWSATContext());
        Assert.assertFalse("Should be false", gt0.isSetSendWSATContext());
        Assert.assertFalse("Should be false", gt0.isSetTransactionTimeOut());
    }

    @Test
    public void testXMIEnterpriseBeanGlobalTransaction() throws Exception {
        List<EnterpriseBean> messageDrivenBeans = parseEJBJarExtension(ejbJarExtension("") +
                                                                       "<ejbExtensions xmi:type=\"ejbext:MessageDrivenExtension\">" +
                                                                       "  <enterpriseBean xmi:type=\"ejb:MessageDriven\" href=\"META-INF/ejb-jar.xml#md0id\"/>" +
                                                                       "  <globalTransaction/>" +
                                                                       "</ejbExtensions>" +
                                                                       "</ejbext:EJBJarExtension>",
                                                                       parseEJBJar(ejbJar21() +
                                                                                   "<enterprise-beans>" +
                                                                                   "  <message-driven id=\"md0id\">" +
                                                                                   "    <ejb-name>md0</ejb-name>" +
                                                                                   "  </message-driven>" +
                                                                                   "</enterprise-beans>" +
                                                                                   "</ejb-jar>")).getEnterpriseBeans();

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = messageDrivenBeans.get(0);
        GlobalTransaction gt0 = bean0.getGlobalTransaction();
        Assert.assertFalse("Should be false", gt0.isSendWSATContext());
        Assert.assertFalse("Should be false", gt0.isSetSendWSATContext());
        Assert.assertFalse("Should be false", gt0.isSetTransactionTimeOut());
    }

    @Test
    public void testEnterpriseBeanResourceRef() throws Exception {
        List<EnterpriseBean> messageDrivenBeans = getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                                                               "<message-driven name=\"md0\">" +

                                                               "<resource-ref name=\"resRef0\">" +
                                                               "</resource-ref>" +
                                                               "<resource-ref name=\"resRef1\">" +
                                                               "</resource-ref>" +
                                                               "<resource-ref name=\"resRef2\">" +
                                                               "</resource-ref>" +

                                                               "</message-driven>" +
                                                               "</ejb-jar-ext>").getEnterpriseBeans();

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = messageDrivenBeans.get(0);
        List<ResourceRef> resRefList = bean0.getResourceRefs();
        ResourceRef resRef0 = resRefList.get(0);
        ResourceRef resRef1 = resRefList.get(1);
        ResourceRef resRef2 = resRefList.get(2);
        Assert.assertEquals("Should be resRef0", "resRef0", resRef0.getName());
        Assert.assertEquals("Should be resRef0", "resRef1", resRef1.getName());
        Assert.assertEquals("Should be resRef0", "resRef2", resRef2.getName());

        Assert.assertFalse("Should be false", resRef2.isSetBranchCoupling());
        Assert.assertFalse("Should be false", resRef2.isSetCommitPriority());
        Assert.assertFalse("Should be false", resRef2.isSetConnectionManagementPolicy());
        Assert.assertFalse("Should be false", resRef2.isSetIsolationLevel());
    }

    @Test
    public void testXMIEnterpriseBeanResourceRef() throws Exception {
        List<EnterpriseBean> messageDrivenBeans = parseEJBJarExtension(ejbJarExtension("") +
                                                                       "<ejbExtensions xmi:type=\"ejbext:MessageDrivenExtension\">" +
                                                                       "  <enterpriseBean xmi:type=\"ejb:MessageDriven\" href=\"META-INF/ejb-jar.xml#md0id\"/>" +
                                                                       "  <resourceRefExtensions>" +
                                                                       "    <resourceRef href=\"META-INF/ejb-jar.xml#rr0\"/>" +
                                                                       "  </resourceRefExtensions>" +
                                                                       "  <resourceRefExtensions>" +
                                                                       "    <resourceRef href=\"META-INF/ejb-jar.xml#rr1\"/>" +
                                                                       "  </resourceRefExtensions>" +
                                                                       "  <resourceRefExtensions>" +
                                                                       "    <resourceRef href=\"META-INF/ejb-jar.xml#rr2\"/>" +
                                                                       "  </resourceRefExtensions>" +
                                                                       "</ejbExtensions>" +
                                                                       "</ejbext:EJBJarExtension>",
                                                                       parseEJBJar(ejbJar21() +
                                                                                   "<enterprise-beans>" +
                                                                                   "  <message-driven id=\"md0id\">" +
                                                                                   "    <ejb-name>md0</ejb-name>" +
                                                                                   "    <resource-ref id=\"rr0\">" +
                                                                                   "      <res-ref-name>resRef0</res-ref-name>" +
                                                                                   "    </resource-ref>" +
                                                                                   "    <resource-ref id=\"rr1\">" +
                                                                                   "      <res-ref-name>resRef1</res-ref-name>" +
                                                                                   "    </resource-ref>" +
                                                                                   "    <resource-ref id=\"rr2\">" +
                                                                                   "      <res-ref-name>resRef2</res-ref-name>" +
                                                                                   "    </resource-ref>" +
                                                                                   "  </message-driven>" +
                                                                                   "</enterprise-beans>" +
                                                                                   "</ejb-jar>")).getEnterpriseBeans();

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = messageDrivenBeans.get(0);
        List<ResourceRef> resRefList = bean0.getResourceRefs();
        ResourceRef resRef0 = resRefList.get(0);
        ResourceRef resRef1 = resRefList.get(1);
        ResourceRef resRef2 = resRefList.get(2);
        Assert.assertEquals("Should be resRef0", "resRef0", resRef0.getName());
        Assert.assertEquals("Should be resRef0", "resRef1", resRef1.getName());
        Assert.assertEquals("Should be resRef0", "resRef2", resRef2.getName());

        Assert.assertFalse("Should be false", resRef2.isSetBranchCoupling());
        Assert.assertFalse("Should be false", resRef2.isSetCommitPriority());
        Assert.assertFalse("Should be false", resRef2.isSetConnectionManagementPolicy());
        Assert.assertFalse("Should be false", resRef2.isSetIsolationLevel());
    }

    @Test
    public void testEnterpriseBeanRunAsMode() throws Exception {
        List<EnterpriseBean> sessionBeans = getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                                                         "<session name=\"session0\">" +
                                                         "<run-as-mode mode='CALLER_IDENTITY' description='description0'>" +
                                                         "<method name='method0'/>" +
                                                         "<method name='method1'/>" +
                                                         "</run-as-mode>" +

                                                         "<run-as-mode mode='SPECIFIED_IDENTITY'>" +
                                                         "<specified-identity role='drum'>" +
                                                         "</specified-identity>" +
                                                         "<method name='method0'/>" +
                                                         "<method name='method1'/>" +
                                                         "</run-as-mode>" +

                                                         "<run-as-mode mode='SPECIFIED_IDENTITY'>" +
                                                         "<specified-identity role='jelly' description='description1'>" +
                                                         "</specified-identity>" +
                                                         "<method name='method0'/>" +
                                                         "<method name='method1'/>" +
                                                         "</run-as-mode>" +

                                                         "<run-as-mode mode='SYSTEM_IDENTITY'>" +
                                                         "<method name='method0'/>" +
                                                         "<method name='method1' params='boolean'/>" +
                                                         "<method name='method2' params='java.lang.Object java.lang.Object'/>" +
                                                         "</run-as-mode>" +

                                                         "</session>" +

                                                         "</ejb-jar-ext>").getEnterpriseBeans();

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = sessionBeans.get(0);
        List<RunAsMode> runAsModeList = bean0.getRunAsModes();
        RunAsMode runasMode0 = runAsModeList.get(0);
        RunAsMode runasMode1 = runAsModeList.get(1);
        RunAsMode runasMode2 = runAsModeList.get(2);
        RunAsMode runasMode3 = runAsModeList.get(3);
        Assert.assertEquals("Should be CALLER_IDENTITY", RunAsModeBase.ModeTypeEnum.CALLER_IDENTITY, runasMode0.getModeType());
        Assert.assertEquals("Should be SPECIFIED_IDENTITY", RunAsModeBase.ModeTypeEnum.SPECIFIED_IDENTITY, runasMode1.getModeType());
        Assert.assertEquals("Should be SPECIFIED_IDENTITY", RunAsModeBase.ModeTypeEnum.SPECIFIED_IDENTITY, runasMode2.getModeType());
        Assert.assertEquals("Should be SYSTEM_IDENTITY", RunAsModeBase.ModeTypeEnum.SYSTEM_IDENTITY, runasMode3.getModeType());

        Assert.assertEquals("Should be description0", "description0", runasMode0.getDescription());
        Assert.assertEquals("Should be null", null, runasMode1.getDescription());

        Assert.assertEquals("Should be null", null, runasMode0.getSpecifiedIdentity());
        Assert.assertEquals("Should be drum", "drum", runasMode1.getSpecifiedIdentity().getRole());
        Assert.assertEquals("Should be null", null, runasMode1.getSpecifiedIdentity().getDescription());
        Assert.assertEquals("Should be jelly", "jelly", runasMode2.getSpecifiedIdentity().getRole());
        Assert.assertEquals("Should be description1", "description1", runasMode2.getSpecifiedIdentity().getDescription());

        Assert.assertEquals("Should be method0", "method0", runasMode3.getMethods().get(0).getName());
        Assert.assertNull(runasMode3.getMethods().get(0).getParams());
        Assert.assertEquals("Should be method1", "method1", runasMode3.getMethods().get(1).getName());
        Assert.assertEquals("boolean", runasMode3.getMethods().get(1).getParams());
        Assert.assertEquals("Should be method2", "method2", runasMode3.getMethods().get(2).getName());
        Assert.assertEquals("java.lang.Object java.lang.Object", runasMode3.getMethods().get(2).getParams());
    }

    @Test
    public void testXMIEnterpriseBeanRunAsMode() throws Exception {
        List<EnterpriseBean> sessionBeans = parseEJBJarExtension(ejbJarExtension("") +
                                                                 "<ejbExtensions xmi:type=\"ejbext:SessionExtension\" timeout=\"42\">" +
                                                                 "  <enterpriseBean xmi:type=\"ejb:Session\" href=\"META-INF/ejb-jar.xml#s0\"/>" +
                                                                 "  <runAsSettings description=\"description0\">" +
                                                                 "    <runAsMode xmi:type=\"ejbext:UseCallerIdentity\"/>" +
                                                                 "    <methodElements name=\"method0\"/>" +
                                                                 "    <methodElements name=\"method1\"/>" +
                                                                 "  </runAsSettings>" +
                                                                 "  <runAsSettings>" +
                                                                 "    <runAsMode xmi:type=\"ejbext:RunAsSpecifiedIdentity\">" +
                                                                 "      <runAsSpecifiedIdentity roleName=\"drum\"/>" +
                                                                 "    </runAsMode>" +
                                                                 "    <methodElements name=\"method0\"/>" +
                                                                 "    <methodElements name=\"method1\"/>" +
                                                                 "  </runAsSettings>" +
                                                                 "  <runAsSettings>" +
                                                                 "    <runAsMode xmi:type=\"ejbext:RunAsSpecifiedIdentity\">" +
                                                                 "      <runAsSpecifiedIdentity roleName=\"jelly\" description=\"description1\"/>" +
                                                                 "    </runAsMode>" +
                                                                 "    <methodElements name=\"method0\"/>" +
                                                                 "    <methodElements name=\"method1\"/>" +
                                                                 "  </runAsSettings>" +
                                                                 "  <runAsSettings description=\"description0\">" +
                                                                 "    <runAsMode xmi:type=\"ejbext:UseSystemIdentity\"/>" +
                                                                 "    <methodElements name=\"method0\"/>" +
                                                                 "    <methodElements name=\"method1\" parms=\"boolean\"/>" +
                                                                 "    <methodElements name=\"method2\" parms=\"java.lang.Object java.lang.Object\"/>" +
                                                                 "  </runAsSettings>" +
                                                                 "</ejbExtensions>" +
                                                                 "</ejbext:EJBJarExtension>",
                                                                 parseEJBJar(ejbJar21() +
                                                                             "<enterprise-beans>" +
                                                                             "  <session id=\"s0\">" +
                                                                             "    <ejb-name>session0</ejb-name>" +
                                                                             "  </session>" +
                                                                             "</enterprise-beans>" +
                                                                             "</ejb-jar>")).getEnterpriseBeans();

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = sessionBeans.get(0);
        List<RunAsMode> runAsModeList = bean0.getRunAsModes();
        RunAsMode runasMode0 = runAsModeList.get(0);
        RunAsMode runasMode1 = runAsModeList.get(1);
        RunAsMode runasMode2 = runAsModeList.get(2);
        RunAsMode runasMode3 = runAsModeList.get(3);
        Assert.assertEquals("Should be CALLER_IDENTITY", RunAsModeBase.ModeTypeEnum.CALLER_IDENTITY, runasMode0.getModeType());
        Assert.assertEquals("Should be SPECIFIED_IDENTITY", RunAsModeBase.ModeTypeEnum.SPECIFIED_IDENTITY, runasMode1.getModeType());
        Assert.assertEquals("Should be SPECIFIED_IDENTITY", RunAsModeBase.ModeTypeEnum.SPECIFIED_IDENTITY, runasMode2.getModeType());
        Assert.assertEquals("Should be SYSTEM_IDENTITY", RunAsModeBase.ModeTypeEnum.SYSTEM_IDENTITY, runasMode3.getModeType());

        Assert.assertEquals("Should be description0", "description0", runasMode0.getDescription());
        Assert.assertEquals("Should be null", null, runasMode1.getDescription());

        Assert.assertEquals("Should be null", null, runasMode0.getSpecifiedIdentity());
        Assert.assertEquals("Should be drum", "drum", runasMode1.getSpecifiedIdentity().getRole());
        Assert.assertEquals("Should be null", null, runasMode1.getSpecifiedIdentity().getDescription());
        Assert.assertEquals("Should be jelly", "jelly", runasMode2.getSpecifiedIdentity().getRole());
        Assert.assertEquals("Should be description1", "description1", runasMode2.getSpecifiedIdentity().getDescription());

        Assert.assertEquals("Should be method0", "method0", runasMode3.getMethods().get(0).getName());
        Assert.assertNull(runasMode3.getMethods().get(0).getParams());
        Assert.assertEquals("Should be method1", "method1", runasMode3.getMethods().get(1).getName());
        Assert.assertEquals("boolean", runasMode3.getMethods().get(1).getParams());
        Assert.assertEquals("Should be method2", "method2", runasMode3.getMethods().get(2).getName());
        Assert.assertEquals("java.lang.Object java.lang.Object", runasMode3.getMethods().get(2).getParams());
    }

    @Test
    public void testEnterpriseStartAtAppStart() throws Exception {
        List<EnterpriseBean> sessionBeans = getEJBJarExt(EJBJarExtTestBase.ejbJarExt11() +
                                                         "<session name='session0'>" +
                                                         "<start-at-app-start value='true'/>" +
                                                         "</session>" +

                                                         "<session name='session1'>" +
                                                         "<start-at-app-start value='false'/>" +
                                                         "</session>" +

                                                         "<session name='session2'>" +
                                                         "</session>" +

                                                         "</ejb-jar-ext>").getEnterpriseBeans();

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = sessionBeans.get(0);
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean1 = sessionBeans.get(1);
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean2 = sessionBeans.get(2);

        StartAtAppStart startAtAppStart0 = bean0.getStartAtAppStart();
        StartAtAppStart startAtAppStart1 = bean1.getStartAtAppStart();
        Assert.assertEquals("Should be true", true, startAtAppStart0.getValue());
        Assert.assertEquals("Should be false", false, startAtAppStart1.getValue());
        Assert.assertEquals("Should be null", null, bean2.getStartAtAppStart());
    }

    @Test
    public void testXMIEnterpriseStartAtAppStart() throws Exception {
        List<EnterpriseBean> sessionBeans = parseEJBJarExtension(ejbJarExtension("") +
                                                                 "<ejbExtensions xmi:type=\"ejbext:SessionExtension\" startEJBAtApplicationStart=\"true\">" +
                                                                 "  <enterpriseBean xmi:type=\"ejb:Session\" href=\"META-INF/ejb-jar.xml#s0\"/>" +
                                                                 "</ejbExtensions>" +
                                                                 "<ejbExtensions xmi:type=\"ejbext:SessionExtension\" startEJBAtApplicationStart=\"false\">" +
                                                                 "  <enterpriseBean xmi:type=\"ejb:Session\" href=\"META-INF/ejb-jar.xml#s1\"/>" +
                                                                 "</ejbExtensions>" +
                                                                 "<ejbExtensions xmi:type=\"ejbext:SessionExtension\">" +
                                                                 "  <enterpriseBean xmi:type=\"ejb:Session\" href=\"META-INF/ejb-jar.xml#s2\"/>" +
                                                                 "</ejbExtensions>" +
                                                                 "</ejbext:EJBJarExtension>",
                                                                 parseEJBJar(ejbJar21() +
                                                                             "<enterprise-beans>" +
                                                                             "  <session id=\"s0\">" +
                                                                             "    <ejb-name>session0</ejb-name>" +
                                                                             "  </session>" +
                                                                             "  <session id=\"s1\">" +
                                                                             "    <ejb-name>session1</ejb-name>" +
                                                                             "  </session>" +
                                                                             "  <session id=\"s2\">" +
                                                                             "    <ejb-name>session2</ejb-name>" +
                                                                             "  </session>" +
                                                                             "</enterprise-beans>" +
                                                                             "</ejb-jar>")).getEnterpriseBeans();

        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean0 = sessionBeans.get(0);
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean1 = sessionBeans.get(1);
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean bean2 = sessionBeans.get(2);

        StartAtAppStart startAtAppStart0 = bean0.getStartAtAppStart();
        StartAtAppStart startAtAppStart1 = bean1.getStartAtAppStart();
        Assert.assertEquals("Should be true", true, startAtAppStart0.getValue());
        Assert.assertEquals("Should be false", false, startAtAppStart1.getValue());
        Assert.assertEquals("Should be null", null, bean2.getStartAtAppStart());
    }
}
