/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ejb;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.DescriptionGroup;
import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.Icon;
import com.ibm.ws.javaee.dd.common.InterceptorCallback;
import com.ibm.ws.javaee.dd.common.LifecycleCallback;
import com.ibm.ws.javaee.dd.ejb.AsyncMethod;
import com.ibm.ws.javaee.dd.ejb.ConcurrentMethod;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejb.InitMethod;
import com.ibm.ws.javaee.dd.ejb.RemoveMethod;
import com.ibm.ws.javaee.dd.ejb.Session;
import com.ibm.ws.javaee.dd.ejb.Timer;
import com.ibm.ws.javaee.dd.ejb.TimerSchedule;
import com.ibm.ws.javaee.dd.ejb.TransactionalBean;
import com.ibm.ws.javaee.ddmodel.DDParser;

public class SessionBeanTest extends EJBJarTestBase {

    @Test
    public void testSession() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>TestSession0</ejb-name>" +
                                               "</session>" +
                                               "<session>" +
                                               "<ejb-name>TestSession1</ejb-name>" +
                                               "<ejb-class>TestSession1.ejb.class.name</ejb-class>" +
                                               "<mapped-name>TestSession1.ejb.mapped.name</mapped-name>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Assert.assertEquals(2, beans.size());
        Session session0 = (Session) beans.get(0);
        Assert.assertEquals("TestSession0", session0.getName());
        Assert.assertEquals(null, session0.getEjbClassName());
        Session session1 = (Session) beans.get(1);
        Assert.assertEquals("TestSession1", session1.getName());
        Assert.assertEquals(EnterpriseBean.KIND_SESSION, session1.getKindValue());
        Assert.assertEquals("TestSession1.ejb.class.name", session1.getEjbClassName());
        Assert.assertEquals("TestSession1.ejb.mapped.name", session1.getMappedName());
    }

    @Test
    public void testDescriptionGroup() throws Exception {
        EJBJar ejbJar = parse(ejbJar30() +
                              "<enterprise-beans>" +
                              "<session/>" +
                              "<session>" +
                              "<description>d0</description>" +
                              "<description xml:lang=\"en\">d1</description>" +
                              "<display-name>dn0</display-name>" +
                              "<display-name xml:lang=\"en\">dn1</display-name>" +
                              "<icon/>" +
                              "<icon xml:lang=\"en\">" +
                              "<small-icon>si</small-icon>" +
                              "<large-icon>li</large-icon>" +
                              "</icon>" +
                              "</session>" +
                              "</enterprise-beans>" +
                              "</ejb-jar>");
        List<EnterpriseBean> ebs = ejbJar.getEnterpriseBeans();

        DescriptionGroup dg = ebs.get(0);
        Assert.assertEquals(Collections.emptyList(), dg.getDescriptions());
        Assert.assertEquals(Collections.emptyList(), dg.getDisplayNames());
        Assert.assertEquals(Collections.emptyList(), dg.getIcons());

        dg = ebs.get(1);
        List<Description> ds = dg.getDescriptions();
        Assert.assertEquals(ds.toString(), 2, ds.size());
        Assert.assertNull(ds.get(0).getLang());
        Assert.assertEquals("d0", ds.get(0).getValue());
        Assert.assertEquals("en", ds.get(1).getLang());
        Assert.assertEquals("d1", ds.get(1).getValue());

        List<DisplayName> dns = dg.getDisplayNames();
        Assert.assertEquals(dns.toString(), 2, dns.size());
        Assert.assertNull(dns.get(0).getLang());
        Assert.assertEquals("dn0", dns.get(0).getValue());
        Assert.assertEquals("en", dns.get(1).getLang());
        Assert.assertEquals("dn1", dns.get(1).getValue());

        List<Icon> icons = dg.getIcons();
        Assert.assertEquals(icons.toString(), 2, icons.size());
        Assert.assertNull(icons.get(0).getLang());
        Assert.assertNull(icons.get(0).getSmallIcon());
        Assert.assertNull(icons.get(0).getLargeIcon());
        Assert.assertEquals("en", icons.get(1).getLang());
        Assert.assertEquals("si", icons.get(1).getSmallIcon());
        Assert.assertEquals("li", icons.get(1).getLargeIcon());
    }

    @Test
    public void testIconEE13() throws Exception {
        EJBJar ejbJar = parse(ejbJar20() +
                              "<enterprise-beans>" +
                              "<session>" +
                              "<small-icon>si</small-icon>" +
                              "<large-icon>li</large-icon>" +
                              "</session>" +
                              "</enterprise-beans>" +
                              "</ejb-jar>");
        List<Icon> icons = ejbJar.getEnterpriseBeans().get(0).getIcons();
        Assert.assertEquals(icons.toString(), 1, icons.size());
        Assert.assertNull(icons.get(0).getLang());
        Assert.assertEquals("si", icons.get(0).getSmallIcon());
        Assert.assertEquals("li", icons.get(0).getLargeIcon());
    }

    @Test
    public void testSessionGetBusinessLocal() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>TestSession0</ejb-name>" +
                                               "</session>" +
                                               "<session>" +
                                               "<ejb-name>TestSession1</ejb-name>" +
                                               "<business-local>com.ibm.example.Test0</business-local>" +
                                               "<business-local>com.ibm.example.Test1</business-local>" +
                                               "<business-local>com.ibm.example.Test2</business-local>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Assert.assertEquals(2, beans.size());
        Session session0 = (Session) beans.get(0);
        Assert.assertEquals(0, session0.getLocalBusinessInterfaceNames().size());
        Session session1 = (Session) beans.get(1);
        Assert.assertEquals(Arrays.asList("com.ibm.example.Test0", "com.ibm.example.Test1", "com.ibm.example.Test2"), session1.getLocalBusinessInterfaceNames());
    }

    @Test
    public void testSessionGetBusinessRemote() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>TestSession0</ejb-name>" +
                                               "</session>" +
                                               "<session>" +
                                               "<ejb-name>TestSession1</ejb-name>" +
                                               "<business-remote>com.ibm.example.Test1</business-remote>" +
                                               "<business-remote>com.ibm.example.Test2</business-remote>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Assert.assertEquals(2, beans.size());
        Session session0 = (Session) beans.get(0);
        Assert.assertEquals(0, session0.getRemoteBusinessInterfaceNames().size());
        Session session1 = (Session) beans.get(1);
        Assert.assertEquals(Arrays.asList("com.ibm.example.Test1", "com.ibm.example.Test2"), session1.getRemoteBusinessInterfaceNames());
    }

    @Test
    public void testSessionIsLocalBean() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>TestSession0</ejb-name>" +
                                               "<local-bean></local-bean>" +
                                               "</session>" +
                                               "<session>" +
                                               "<ejb-name>TestSession1</ejb-name>" +
                                               //"<local-bean></local-bean>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Session session0 = (Session) beans.get(0);
        Assert.assertTrue(session0.isLocalBean());
        Session session1 = (Session) beans.get(1);
        Assert.assertFalse(session1.isLocalBean());
    }

    @Test
    public void testSessionGetServiceEndpointInterfaceName() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>TestSession0</ejb-name>" +
                                               "<service-endpoint>serviceEndpoint0</service-endpoint>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Session session0 = (Session) beans.get(0);
        Assert.assertEquals("serviceEndpoint0", session0.getServiceEndpointInterfaceName());
    }

    /**
     * @return &lt;session-type>
     *         <ul>
     *         <li>{@link #SESSION_TYPE_UNSPECIFIED} if unspecified
     *         <li>{@link #SESSION_TYPE_SINGLETON} - Singleton
     *         <li>{@link #SESSION_TYPE_STATEFUL} - Stateful
     *         <li>{@link #SESSION_TYPE_STATELESS} - Stateless
     *         </ul>
     */
    @Test
    public void testSessionGetSessionTypeValue() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>TestSession0</ejb-name>" +
                                               //"<session-type></session-type>" + //unspecified
                                               "</session>" +
                                               "<session>" +
                                               "<ejb-name>TestSession1</ejb-name>" +
                                               "<session-type>Singleton</session-type>" +
                                               "</session>" +
                                               "<session>" +
                                               "<ejb-name>TestSession2</ejb-name>" +
                                               "<session-type>Stateful</session-type>" +
                                               "</session>" +
                                               "<session>" +
                                               "<ejb-name>TestSession3</ejb-name>" +
                                               "<session-type>Stateless</session-type>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Session session0 = (Session) beans.get(0);
        Session session1 = (Session) beans.get(1);
        Session session2 = (Session) beans.get(2);
        Session session3 = (Session) beans.get(3);
        Assert.assertEquals(Session.SESSION_TYPE_UNSPECIFIED, session0.getSessionTypeValue());
        Assert.assertEquals(Session.SESSION_TYPE_SINGLETON, session1.getSessionTypeValue());
        Assert.assertEquals(Session.SESSION_TYPE_STATEFUL, session2.getSessionTypeValue());
        Assert.assertEquals(Session.SESSION_TYPE_STATELESS, session3.getSessionTypeValue());
    }

    @Test
    public void testSessionGetStatefulTimeout() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>TestSession0</ejb-name>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>TestSession1</ejb-name>" +
                                               "<stateful-timeout>" +
                                               "<timeout>" +
                                               Long.MAX_VALUE +
                                               "</timeout>" +
                                               "</stateful-timeout>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>TestSession2</ejb-name>" +
                                               "<stateful-timeout>" +
                                               "<timeout>" +
                                               "2147483647" +
                                               "</timeout>" +
                                               "<unit>Days</unit>" +
                                               "</stateful-timeout>" +
                                               "</session>" +

                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Session session0 = (Session) beans.get(0);
        Session session1 = (Session) beans.get(1);
        Session session2 = (Session) beans.get(2);
        Assert.assertEquals(null, session0.getStatefulTimeout());
        Assert.assertEquals(Long.MAX_VALUE, session1.getStatefulTimeout().getTimeout());
        Assert.assertEquals(TimeUnit.MINUTES, session1.getStatefulTimeout().getUnitValue());
        Assert.assertEquals(TimeUnit.DAYS, session2.getStatefulTimeout().getUnitValue());
    }

    @Test
    public void testSessionInitOnStartup() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>TestSession0</ejb-name>" +
                                               "<init-on-startup>true</init-on-startup>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>TestSession1</ejb-name>" +
                                               "<init-on-startup>false</init-on-startup>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>TestSession2</ejb-name>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Session session0 = (Session) beans.get(0);
        Session session1 = (Session) beans.get(1);
        Session session2 = (Session) beans.get(2);
        Assert.assertEquals(true, session0.isSetInitOnStartup());
        Assert.assertEquals(true, session1.isSetInitOnStartup());
        Assert.assertEquals(false, session2.isSetInitOnStartup());

        Assert.assertEquals(true, session0.isInitOnStartup());
        Assert.assertEquals(false, session1.isInitOnStartup());
        Assert.assertEquals(false, session2.isInitOnStartup());
    }

    /**
     * @return &lt;concurrency-management-type>
     *         <ul>
     *         <li>{@link #CONCURRENCY_MANAGEMENT_TYPE_UNSPECIFIED} if unspecified
     *         <li>{@link #CONCURRENCY_MANAGEMENT_TYPE_BEAN} - Bean
     *         <li>{@link #CONCURRENCY_MANAGEMENT_TYPE_CONTAINER} - Container
     *         </ul>
     */
    @Test
    public void testSessionConcurrencyManagement() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>TestSession0</ejb-name>" +
                                               "</session>" +
                                               "<session>" +
                                               "<ejb-name>TestSession1</ejb-name>" +
                                               "<concurrency-management-type>Bean</concurrency-management-type>" +
                                               "</session>" +
                                               "<session>" +
                                               "<ejb-name>TestSession2</ejb-name>" +
                                               "<concurrency-management-type>Container</concurrency-management-type>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Session session0 = (Session) beans.get(0);
        Session session1 = (Session) beans.get(1);
        Session session2 = (Session) beans.get(2);
        Assert.assertEquals(Session.CONCURRENCY_MANAGEMENT_TYPE_UNSPECIFIED, session0.getConcurrencyManagementTypeValue());
        Assert.assertEquals(Session.CONCURRENCY_MANAGEMENT_TYPE_BEAN, session1.getConcurrencyManagementTypeValue());
        Assert.assertEquals(Session.CONCURRENCY_MANAGEMENT_TYPE_CONTAINER, session2.getConcurrencyManagementTypeValue());
    }

    /**
     * @return &lt;concurrency-management-type>
     *         <ul>
     *         <li>{@link #CONCURRENCY_MANAGEMENT_TYPE_UNSPECIFIED} if unspecified
     *         <li>{@link #CONCURRENCY_MANAGEMENT_TYPE_BEAN} - Bean
     *         <li>{@link #CONCURRENCY_MANAGEMENT_TYPE_CONTAINER} - Container
     *         </ul>
     */

    @Test
    public void testSessionGetConcurrentMethods() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>TestSession0</ejb-name>" +
                                               "<concurrent-method>" +
                                               "<method>" +
                                               "<method-name>methodName0</method-name>" +
                                               "</method>" +
                                               "<access-timeout>" +
                                               "<timeout>42</timeout>" +
                                               "<unit>Seconds</unit>" +
                                               "</access-timeout>" +
                                               "</concurrent-method>" +

                                               "<concurrent-method>" +
                                               "<method>" +
                                               "<method-name>methodName1</method-name>" +
                                               "<method-params>" +
                                               "<method-param>meth0param0</method-param>" +
                                               "<method-param>meth0param1</method-param>" +
                                               "<method-param>meth0param2</method-param>" +
                                               "</method-params>" +
                                               "</method>" +
                                               "<lock>Read</lock>" +
                                               "</concurrent-method>" +

                                               "<concurrent-method>" +
                                               "<method>" +
                                               "<method-name>methodName2</method-name>" +
                                               "<method-params>" +
                                               "</method-params>" +
                                               "</method>" +
                                               "<lock>Write</lock>" +
                                               "</concurrent-method>" +

                                               "<concurrent-method>" +
                                               "<method>" +
                                               "<method-name>methodName3</method-name>" +
                                               "</method>" +
                                               //"<lock></lock>" +
                                               "</concurrent-method>" +

                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Session session0 = (Session) beans.get(0);
        ConcurrentMethod concurMeth0 = session0.getConcurrentMethods().get(0);
        ConcurrentMethod concurMeth1 = session0.getConcurrentMethods().get(1);
        ConcurrentMethod concurMeth2 = session0.getConcurrentMethods().get(2);
        ConcurrentMethod concurMeth3 = session0.getConcurrentMethods().get(3);
        Assert.assertEquals(42, concurMeth0.getAccessTimeout().getTimeout());
        Assert.assertEquals(TimeUnit.SECONDS, concurMeth0.getAccessTimeout().getUnitValue());

        Assert.assertEquals("methodName0", concurMeth0.getMethod().getMethodName());
        Assert.assertEquals("methodName1", concurMeth1.getMethod().getMethodName());
        Assert.assertEquals("methodName2", concurMeth2.getMethod().getMethodName());
        Assert.assertEquals("methodName3", concurMeth3.getMethod().getMethodName());

        Assert.assertNull(concurMeth0.getMethod().getMethodParamList());
        Assert.assertEquals(Arrays.asList("meth0param0", "meth0param1", "meth0param2"), concurMeth1.getMethod().getMethodParamList());
        Assert.assertEquals(true, concurMeth2.getMethod().getMethodParamList().isEmpty());

        Assert.assertEquals(ConcurrentMethod.LOCK_TYPE_UNSPECIFIED, concurMeth0.getLockTypeValue());
        Assert.assertEquals(ConcurrentMethod.LOCK_TYPE_READ, concurMeth1.getLockTypeValue());
        Assert.assertEquals(ConcurrentMethod.LOCK_TYPE_WRITE, concurMeth2.getLockTypeValue());
        Assert.assertEquals(ConcurrentMethod.LOCK_TYPE_UNSPECIFIED, concurMeth3.getLockTypeValue());
    }

    @Test
    public void testSessionDependsOn() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
                                               "<depends-on>" +
                                               "</depends-on>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>ejbName2</ejb-name>" +
                                               "<depends-on>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
                                               "<ejb-name>ejbName2</ejb-name>" +
                                               "</depends-on>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Session session0 = (Session) beans.get(0);
        Session session1 = (Session) beans.get(1);
        Session session2 = (Session) beans.get(2);
        Assert.assertEquals(null, session0.getDependsOn());
        Assert.assertEquals(true, session1.getDependsOn().getEjbName().isEmpty());
        Assert.assertEquals(Arrays.asList("ejbName0", "ejbName1", "ejbName2"), session2.getDependsOn().getEjbName());
    }

    @Test
    public void testSessionInitMethod() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
                                               "<init-method>" +
                                               "<create-method>" +
                                               "<method-name>createMethod0</method-name>" +
                                               "</create-method>" +
                                               "<bean-method>" +
                                               "<method-name>beanMethod0</method-name>" +
                                               "</bean-method>" +
                                               "</init-method>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Session session0 = (Session) beans.get(0);
        Session session1 = (Session) beans.get(1);
        Assert.assertEquals(true, session0.getInitMethod().isEmpty());

        InitMethod initMeth0 = session1.getInitMethod().get(0);
        Assert.assertEquals("createMethod0", initMeth0.getCreateMethod().getMethodName());
        Assert.assertEquals("beanMethod0", initMeth0.getBeanMethod().getMethodName());
    }

    @Test
    public void testSessionRemoveMethod() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
                                               "<remove-method>" +
                                               "<bean-method>" +
                                               "<method-name>beanMethod0</method-name>" +
                                               "</bean-method>" +
                                               "<retain-if-exception>true</retain-if-exception>" +
                                               "</remove-method>" +

                                               "<remove-method>" +
                                               "<bean-method>" +
                                               "<method-name>beanMethod1</method-name>" +
                                               "<method-params/>" +
                                               "</bean-method>" +
                                               "<retain-if-exception>false</retain-if-exception>" +
                                               "</remove-method>" +

                                               "<remove-method>" +
                                               "<bean-method>" +
                                               "<method-name>beanMethod2</method-name>" +
                                               "<method-params>" +
                                               "<method-param>int</method-param>" +
                                               "</method-params>" +
                                               "</bean-method>" +
                                               "</remove-method>" +

                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Session session0 = (Session) beans.get(0);
        Session session1 = (Session) beans.get(1);
        Assert.assertEquals(true, session0.getRemoveMethod().isEmpty());

        RemoveMethod remMeth0 = session1.getRemoveMethod().get(0);
        RemoveMethod remMeth1 = session1.getRemoveMethod().get(1);
        RemoveMethod remMeth2 = session1.getRemoveMethod().get(2);

        Assert.assertEquals("beanMethod0", remMeth0.getBeanMethod().getMethodName());
        Assert.assertNull(remMeth0.getBeanMethod().getMethodParamList());
        Assert.assertEquals("beanMethod1", remMeth1.getBeanMethod().getMethodName());
        Assert.assertEquals(Collections.emptyList(), remMeth1.getBeanMethod().getMethodParamList());
        Assert.assertEquals("beanMethod2", remMeth2.getBeanMethod().getMethodName());
        Assert.assertEquals(Arrays.asList("int"), remMeth2.getBeanMethod().getMethodParamList());
        Assert.assertEquals(true, remMeth0.isSetRetainIfException());
        Assert.assertEquals(true, remMeth0.isRetainIfException());
        Assert.assertEquals(true, remMeth1.isSetRetainIfException());
        Assert.assertEquals(false, remMeth1.isRetainIfException());
        Assert.assertEquals(false, remMeth2.isSetRetainIfException());
        Assert.assertEquals(false, remMeth2.isRetainIfException());
    }

    @Test
    public void testSessionAsynchMethod() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
                                               "<async-method>" +
                                               "<method-name>asyncMethod0</method-name>" +
                                               "<method-params>" +
                                               "<method-param>meth0param0</method-param>" +
                                               "<method-param>meth0param1</method-param>" +
                                               "<method-param>meth0param2</method-param>" +
                                               "</method-params>" +
                                               "</async-method>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Session session0 = (Session) beans.get(0);
        Session session1 = (Session) beans.get(1);
        Assert.assertEquals(true, session0.getAsyncMethods().isEmpty());

        AsyncMethod asynchMeth0 = session1.getAsyncMethods().get(0);
        Assert.assertEquals("asyncMethod0", asynchMeth0.getMethodName());
        Assert.assertEquals(Arrays.asList("meth0param0", "meth0param1", "meth0param2"), asynchMeth0.getMethodParamList());
    }

    @Test
    public void testSessionBeforeAndAfter() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "<after-begin-method>" +
                                               "<method-name>afterBegin0</method-name>" +
                                               "</after-begin-method>" +

                                               "<before-completion-method>" +
                                               "<method-name>beforeCompletion0</method-name>" +
                                               "</before-completion-method>" +

                                               "<after-completion-method>" +
                                               "<method-name>afterCompletion0</method-name>" +
                                               "</after-completion-method>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
                                               "</session>" +

                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Session session0 = (Session) beans.get(0);
        Assert.assertEquals("afterBegin0", session0.getAfterBeginMethod().getMethodName());
        Assert.assertEquals("beforeCompletion0", session0.getBeforeCompletionMethod().getMethodName());
        Assert.assertEquals("afterCompletion0", session0.getAfterCompletionMethod().getMethodName());

        Session session1 = (Session) beans.get(1);
        Assert.assertEquals(null, session1.getAfterBeginMethod());
        Assert.assertEquals(null, session1.getBeforeCompletionMethod());
        Assert.assertEquals(null, session1.getAfterCompletionMethod());
    }

    @Test
    public void testSessionComponentViewableBean() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "</session>" +
                                               "<session>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
                                               "<home>com.ibm.example.home1</home>" +
                                               "<remote>com.ibm.example.remote1</remote>" +
                                               "<local-home>com.ibm.example.localHome1</local-home>" +
                                               "<local>com.ibm.example.local1</local>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Assert.assertEquals(2, beans.size());
        Session session0 = (Session) beans.get(0);
        Session session1 = (Session) beans.get(1);

        Assert.assertEquals(null, session0.getHomeInterfaceName());
        Assert.assertEquals(null, session0.getRemoteInterfaceName());
        Assert.assertEquals(null, session0.getLocalHomeInterfaceName());
        Assert.assertEquals(null, session0.getLocalInterfaceName());

        Assert.assertEquals("com.ibm.example.home1", session1.getHomeInterfaceName());
        Assert.assertEquals("com.ibm.example.remote1", session1.getRemoteInterfaceName());
        Assert.assertEquals("com.ibm.example.localHome1", session1.getLocalHomeInterfaceName());
        Assert.assertEquals("com.ibm.example.local1", session1.getLocalInterfaceName());
    }

    @Test
    public void testSessionTimerSchedule() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
                                               "<timeout-method>" +
                                               "<method-name>timeoutMethod1</method-name>" +
                                               "</timeout-method>" +
                                               "<timer>" +
                                               "</timer>" +
                                               "<timer>" +
                                               "<schedule>" +
                                               "<second>second0</second>" +
                                               "<minute>minute0</minute>" +
                                               "<hour>hour0</hour>" +
                                               "<day-of-month>dayOfMonth0</day-of-month>" +
                                               "<month>month0</month>" +
                                               "<day-of-week>dayOfWeek0</day-of-week>" +
                                               "<year>year0</year>" +
                                               "</schedule>" +
                                               "</timer>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Assert.assertEquals(2, beans.size());
        Session session0 = (Session) beans.get(0);
        Session session1 = (Session) beans.get(1);

        Assert.assertEquals(null, session0.getTimeoutMethod());
        Assert.assertEquals(true, session0.getTimers().isEmpty());

        Assert.assertEquals("timeoutMethod1", session1.getTimeoutMethod().getMethodName());
        Assert.assertEquals(null, session1.getTimers().get(0).getSchedule().getSecond());
        Assert.assertEquals(null, session1.getTimers().get(0).getSchedule().getMinute());
        Assert.assertEquals(null, session1.getTimers().get(0).getSchedule().getHour());
        Assert.assertEquals(null, session1.getTimers().get(0).getSchedule().getDayOfMonth());
        Assert.assertEquals(null, session1.getTimers().get(0).getSchedule().getDayOfWeek());
        Assert.assertEquals(null, session1.getTimers().get(0).getSchedule().getMonth());
        Assert.assertEquals(null, session1.getTimers().get(0).getSchedule().getYear());

        TimerSchedule timerSched = session1.getTimers().get(1).getSchedule();
        Assert.assertEquals("second0", timerSched.getSecond());
        Assert.assertEquals("minute0", timerSched.getMinute());
        Assert.assertEquals("hour0", timerSched.getHour());
        Assert.assertEquals("dayOfMonth0", timerSched.getDayOfMonth());
        Assert.assertEquals("dayOfWeek0", timerSched.getDayOfWeek());
        Assert.assertEquals("month0", timerSched.getMonth());
        Assert.assertEquals("year0", timerSched.getYear());
    }

    @Test
    public void testSessionTimer() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
                                               "<timeout-method>" +
                                               "<method-name>timeoutMethod1</method-name>" +
                                               "</timeout-method>" +
                                               "<timer>" +
                                               "<start>start0</start>" +
                                               "<end>end0</end>" +
                                               "<timeout-method>" +
                                               "<method-name>timerTimeoutMethod1</method-name>" +
                                               "</timeout-method>" +
                                               "<persistent>true</persistent>" +
                                               "<timezone>timeZone1</timezone>" +
                                               "<info>info1</info>" +
                                               "</timer>" +

                                               "<timer>" +
                                               "</timer>" +

                                               "<timer>" +
                                               "<persistent>false</persistent>" +
                                               "</timer>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Assert.assertEquals(2, beans.size());
        Session session0 = (Session) beans.get(0);
        Session session1 = (Session) beans.get(1);

        Assert.assertEquals(null, session0.getTimeoutMethod());
        Timer timer0 = session1.getTimers().get(0);
        Assert.assertEquals("start0", timer0.getStart());
        Assert.assertEquals("end0", timer0.getEnd());
        Assert.assertEquals("timerTimeoutMethod1", timer0.getTimeoutMethod().getMethodName());
        Assert.assertEquals(true, timer0.isSetPersistent());
        Assert.assertEquals(true, timer0.isPersistent());
        Assert.assertEquals("timeZone1", timer0.getTimezone());
        Assert.assertEquals("info1", timer0.getInfo());

        Timer timer1 = session1.getTimers().get(1);
        Assert.assertEquals(null, timer1.getStart());
        Assert.assertEquals(null, timer1.getEnd());
        Assert.assertEquals(null, timer1.getTimeoutMethod().getMethodName());
        Assert.assertEquals(false, timer1.isSetPersistent());
        Assert.assertEquals(false, timer1.isPersistent());
        Assert.assertEquals(null, timer1.getTimezone());
        Assert.assertEquals(null, timer1.getInfo());

        Timer timer2 = session1.getTimers().get(2);
        Assert.assertEquals(true, timer2.isSetPersistent());
        Assert.assertEquals(false, timer2.isPersistent());
    }

    @Test
    public void testSessionTransactionalBean() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
                                               "<transaction-type>Bean</transaction-type>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>ejbName2</ejb-name>" +
                                               "<transaction-type>Container</transaction-type>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Session session0 = (Session) beans.get(0);
        Session session1 = (Session) beans.get(1);
        Session session2 = (Session) beans.get(2);

        Assert.assertEquals(TransactionalBean.TRANSACTION_TYPE_UNSPECIFIED, session0.getTransactionTypeValue());
        Assert.assertEquals(TransactionalBean.TRANSACTION_TYPE_BEAN, session1.getTransactionTypeValue());
        Assert.assertEquals(TransactionalBean.TRANSACTION_TYPE_CONTAINER, session2.getTransactionTypeValue());
    }

    @Test
    public void testSessionPassivationCapable() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar32() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
                                               "<passivation-capable>false</passivation-capable>" +
                                               "</session>" +

                                               "<session>" +
                                               "<passivation-capable>true</passivation-capable>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();

        Session session0 = (Session) beans.get(0);
        Assert.assertFalse(session0.isSetPassivationCapable());

        Session session1 = (Session) beans.get(1);
        Assert.assertTrue(session1.isSetPassivationCapable());
        Assert.assertFalse(session1.isPassivationCapable());

        Session session2 = (Session) beans.get(2);
        Assert.assertTrue(session2.isSetPassivationCapable());
        Assert.assertTrue(session2.isPassivationCapable());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testPassivationCapableEJB31() throws Exception {
        getEJBJar(EJBJarTest.ejbJar31() +
                  "<enterprise-beans>" +
                  "<session>" +
                  "<ejb-name>ejbName1</ejb-name>" +
                  "<passivation-capable>false</passivation-capable>" +
                  "</session>" +
                  "</enterprise-beans>" +
                  "</ejb-jar>");
    }

    @Test
    public void testSessionSessionInterceptor() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar11() +
                                               "<enterprise-beans>" +
                                               "<session>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
                                               "<post-activate>" +
                                               "<lifecycle-callback-class>lifecycleCallbackClass0</lifecycle-callback-class>" +
                                               "<lifecycle-callback-method>lifecycleCallbackMethod0</lifecycle-callback-method>" +
                                               "</post-activate>" +
                                               "<pre-passivate>" +
                                               "<lifecycle-callback-class>lifecycleCallbackClass1</lifecycle-callback-class>" +
                                               "<lifecycle-callback-method>lifecycleCallbackMethod1</lifecycle-callback-method>" +
                                               "</pre-passivate>" +
                                               "</session>" +

                                               "<session>" +
                                               "<ejb-name>ejbName2</ejb-name>" +
                                               "<around-invoke>" +
                                               "<method-name>aroundInvokeMethodName0</method-name>" +
                                               "<class>aroundInvokeClass0</class>" +
                                               "</around-invoke>" +
                                               "<around-invoke>" +
                                               "<method-name>aroundInvokeMethodName1</method-name>" +
                                               "</around-invoke>" +
                                               "<around-timeout>" +
                                               "<method-name>aroundTimeoutMethodName0</method-name>" +
                                               "<class>aroundTimeoutClass0</class>" +
                                               "</around-timeout>" +
                                               "</session>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        Session session0 = (Session) beans.get(0);
        Session session1 = (Session) beans.get(1);
        Session session2 = (Session) beans.get(2);
        Assert.assertEquals(true, session0.getPostActivate().isEmpty());
        Assert.assertEquals(true, session0.getPrePassivate().isEmpty());
        Assert.assertEquals(true, session0.getAroundInvoke().isEmpty());
        Assert.assertEquals(true, session0.getAroundTimeoutMethods().isEmpty());

        LifecycleCallback lifecycleCallback0 = session1.getPostActivate().get(0);
        Assert.assertEquals("lifecycleCallbackClass0", lifecycleCallback0.getClassName());
        Assert.assertEquals("lifecycleCallbackMethod0", lifecycleCallback0.getMethodName());
        LifecycleCallback lifecycleCallback1 = session1.getPrePassivate().get(0);
        Assert.assertEquals("lifecycleCallbackClass1", lifecycleCallback1.getClassName());
        Assert.assertEquals("lifecycleCallbackMethod1", lifecycleCallback1.getMethodName());

        InterceptorCallback intCallback0 = session2.getAroundInvoke().get(0);
        Assert.assertEquals("aroundInvokeClass0", intCallback0.getClassName());
        Assert.assertEquals("aroundInvokeMethodName0", intCallback0.getMethodName());
        InterceptorCallback intCallback1 = session2.getAroundInvoke().get(1);
        Assert.assertEquals(null, intCallback1.getClassName());
        Assert.assertEquals("aroundInvokeMethodName1", intCallback1.getMethodName());

        InterceptorCallback intCallback2 = session2.getAroundTimeoutMethods().get(0);
        Assert.assertEquals("aroundTimeoutClass0", intCallback2.getClassName());
        Assert.assertEquals("aroundTimeoutMethodName0", intCallback2.getMethodName());
    }
}
