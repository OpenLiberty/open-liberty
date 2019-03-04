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

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.common.MessageDestination;
import com.ibm.ws.javaee.dd.common.SecurityRole;
import com.ibm.ws.javaee.dd.ejb.ApplicationException;
import com.ibm.ws.javaee.dd.ejb.AssemblyDescriptor;
import com.ibm.ws.javaee.dd.ejb.ContainerTransaction;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejb.ExcludeList;
import com.ibm.ws.javaee.dd.ejb.InterceptorBinding;
import com.ibm.ws.javaee.dd.ejb.Method;
import com.ibm.ws.javaee.dd.ejb.MethodPermission;
import com.ibm.ws.javaee.ddmodel.DDParser;

public class AssemblyDescriptorTest extends EJBJarTestBase {

    String securityRole = "<security-role>" +
                          "<role-name>roleName0</role-name>" +
                          "</security-role>" +
                          "<security-role>" +
                          "<role-name>roleName1</role-name>" +
                          "</security-role>" +
                          "<security-role>" +
                          "<role-name>roleName2</role-name>" +
                          "</security-role>";

    @Test
    public void testAssemblyDescriptorSecurityRole() throws Exception {
        AssemblyDescriptor assemblyDescriptor = getAssemblyDescriptor();
        Assert.assertNotNull(assemblyDescriptor);
        List<SecurityRole> secRoleList = assemblyDescriptor.getSecurityRoles();
        Assert.assertEquals("roleName0", secRoleList.get(0).getRoleName());
        Assert.assertEquals("roleName1", secRoleList.get(1).getRoleName());
        Assert.assertEquals("roleName2", secRoleList.get(2).getRoleName());
    }

    String method0 = "<method>" +
                     "<method-name>methodName0</method-name>" +
                     "<ejb-name>ejbName0</ejb-name>" +
                     //"<method-intf></method-intf>" +  UNSPECIFIED
                     "<method-params>" +
                     "<method-param>methodParm0</method-param>" +
                     "<method-param>methodParm1</method-param>" +
                     "<method-param>methodParm2</method-param>" +
                     "</method-params>" +
                     "</method>";

    String method1 = "<method>" +
                     "<method-name>methodName1</method-name>" +
                     "<ejb-name>ejbName1</ejb-name>" +
                     "<method-intf>Home</method-intf>" +
                     "<method-params/>" +
                     "</method>";

    String method2 = "<method>" +
                     "<method-name>methodName2</method-name>" +
                     "<method-intf>Local</method-intf>" +
                     "</method>";

    String method3 = "<method>" +
                     "<method-name>methodName3</method-name>" +
                     "<method-intf>LocalHome</method-intf>" +
                     "</method>";

    String method4 = "<method>" +
                     "<method-name>methodName4</method-name>" +
                     "<method-intf>MessageEndpoint</method-intf>" +
                     "</method>";
    String method5 = "<method>" +
                     "<method-name>methodName5</method-name>" +
                     "<method-intf>Remote</method-intf>" +
                     "</method>";

    String method6 = "<method>" +
                     "<method-name>methodName6</method-name>" +
                     "<method-intf>ServiceEndpoint</method-intf>" +
                     "</method>";

    String method7 = "<method>" +
                     "<method-name>methodName7</method-name>" +
                     "<method-intf>Timer</method-intf>" +
                     "</method>";

    void testMethod0(Method method0) {
        Assert.assertEquals(Method.INTERFACE_TYPE_UNSPECIFIED, method0.getInterfaceTypeValue());
        Assert.assertEquals("ejbName0", method0.getEnterpriseBeanName());
        Assert.assertEquals("methodName0", method0.getMethodName());
        Assert.assertEquals("methodParm0", method0.getMethodParamList().get(0));
        Assert.assertEquals("methodParm1", method0.getMethodParamList().get(1));
        Assert.assertEquals("methodParm2", method0.getMethodParamList().get(2));
    }

    String methodPermission0 = "<method-permission>" +
                               "<unchecked/>" +
                               method0 +
                               method1 +
                               method2 +
                               method3 +
                               method4 +
                               method5 +
                               method6 +
                               method7 +
                               "</method-permission>";

    @Test
    public void testAssemblyDescriptorMethodPermission0() throws Exception {
        AssemblyDescriptor assemblyDescriptor = getAssemblyDescriptor();
        List<MethodPermission> methPermList = assemblyDescriptor.getMethodPermissions();
        MethodPermission methPerm0 = methPermList.get(0);
        Assert.assertEquals(true, methPerm0.isUnchecked());
        List<Method> methodList = methPerm0.getMethodElements();

        Method method0 = methodList.get(0);
        testMethod0(method0);

        Method method1 = methodList.get(1);
        Assert.assertEquals("ejbName1", method1.getEnterpriseBeanName());
        Assert.assertEquals(Method.INTERFACE_TYPE_HOME, method1.getInterfaceTypeValue());
        Assert.assertEquals(Collections.emptyList(), method1.getMethodParamList());

        Method method2 = methodList.get(2);
        Assert.assertEquals(Method.INTERFACE_TYPE_LOCAL, method2.getInterfaceTypeValue());
        Assert.assertNull(method2.getMethodParamList());

        Assert.assertEquals(Method.INTERFACE_TYPE_LOCAL_HOME, methodList.get(3).getInterfaceTypeValue());
        Assert.assertEquals(Method.INTERFACE_TYPE_MESSAGE_ENDPOINT, methodList.get(4).getInterfaceTypeValue());
        Assert.assertEquals(Method.INTERFACE_TYPE_REMOTE, methodList.get(5).getInterfaceTypeValue());
        Assert.assertEquals(Method.INTERFACE_TYPE_SERVICE_ENDPOINT, methodList.get(6).getInterfaceTypeValue());
        Assert.assertEquals(Method.INTERFACE_TYPE_TIMER, methodList.get(7).getInterfaceTypeValue());
    }

    String methodPermission1 = "<method-permission>" +
                               //"<unchecked/>" +  //has role-names so not unchecked
                               "<role-name>roleName0</role-name>" +
                               "<role-name>roleName1</role-name>" +
                               "<role-name>roleName2</role-name>" +
                               "</method-permission>";

    @Test
    public void testAssemblyDescriptorMethodPermission1() throws Exception {
        AssemblyDescriptor assemblyDescriptor = getAssemblyDescriptor();
        List<MethodPermission> methPermList = assemblyDescriptor.getMethodPermissions();
        MethodPermission methPerm1 = methPermList.get(1);
        Assert.assertEquals(false, methPerm1.isUnchecked());
        List<String> roleNamesList = methPerm1.getRoleNames();
        Assert.assertEquals("roleName0", roleNamesList.get(0));
        Assert.assertEquals("roleName1", roleNamesList.get(1));
        Assert.assertEquals("roleName2", roleNamesList.get(2));
    }

    /**
     * <li>{@link #TRANS_ATTRIBUTE_NOT_SUPPORTED} - NotSupported
     * <li>{@link #TRANS_ATTRIBUTE_SUPPORTS} - Supports
     * <li>{@link #TRANS_ATTRIBUTE_REQUIRED} - Required
     * <li>{@link #TRANS_ATTRIBUTE_REQUIRES_NEW} - RequiresNew
     * <li>{@link #TRANS_ATTRIBUTE_MANDATORY} - Mandatory
     * <li>{@link #TRANS_ATTRIBUTE_NEVER} - Never
     **/
    String containerTransactions = "<container-transaction>" +
                                   method0 +
                                   method1 +
                                   method2 +
                                   "<trans-attribute>NotSupported</trans-attribute>" +
                                   "</container-transaction>" +

                                   "<container-transaction>" +
                                   method3 +
                                   method4 +
                                   method5 +
                                   method6 +
                                   "<trans-attribute>Supports</trans-attribute>" +
                                   "</container-transaction>" +

                                   "<container-transaction>" +
                                   "<trans-attribute>Required</trans-attribute>" +
                                   "</container-transaction>" +

                                   "<container-transaction>" +
                                   "<trans-attribute>RequiresNew</trans-attribute>" +
                                   "</container-transaction>" +

                                   "<container-transaction>" +
                                   "<trans-attribute>Mandatory</trans-attribute>" +
                                   "</container-transaction>" +

                                   "<container-transaction>" +
                                   "<trans-attribute>Never</trans-attribute>" +
                                   "</container-transaction>";

    @Test
    public void testAssemblyDescriptorContainerTransactions() throws Exception {
        AssemblyDescriptor assemblyDescriptor = getAssemblyDescriptor();
        List<ContainerTransaction> contTransList = assemblyDescriptor.getContainerTransactions();
        Assert.assertEquals(ContainerTransaction.TRANS_ATTRIBUTE_NOT_SUPPORTED, contTransList.get(0).getTransAttributeTypeValue());
        Assert.assertEquals(ContainerTransaction.TRANS_ATTRIBUTE_SUPPORTS, contTransList.get(1).getTransAttributeTypeValue());
        Assert.assertEquals(ContainerTransaction.TRANS_ATTRIBUTE_REQUIRED, contTransList.get(2).getTransAttributeTypeValue());
        Assert.assertEquals(ContainerTransaction.TRANS_ATTRIBUTE_REQUIRES_NEW, contTransList.get(3).getTransAttributeTypeValue());
        Assert.assertEquals(ContainerTransaction.TRANS_ATTRIBUTE_MANDATORY, contTransList.get(4).getTransAttributeTypeValue());
        Assert.assertEquals(ContainerTransaction.TRANS_ATTRIBUTE_NEVER, contTransList.get(5).getTransAttributeTypeValue());

        ContainerTransaction contTrans0 = contTransList.get(0);
        List<Method> methodList = contTrans0.getMethodElements();
        Assert.assertEquals(3, methodList.size());
        testMethod0(methodList.get(0));
        Assert.assertEquals(4, contTransList.get(1).getMethodElements().size());
    }

    String interceptorBinding0 = "<interceptor-binding>" +
                                 "<ejb-name>ejbName0</ejb-name>" +
                                 "<interceptor-class>interceptorClass0</interceptor-class>" +
                                 "<interceptor-class>interceptorClass1</interceptor-class>" +
                                 "<exclude-default-interceptors>true</exclude-default-interceptors>" +
                                 "<exclude-class-interceptors>true</exclude-class-interceptors>" +
                                 "<method>" +
                                 "<method-name>namedMethod0</method-name>" +
                                 "</method>" +
                                 "</interceptor-binding>";

    String interceptorBinding1 = "<interceptor-binding>" +
                                 "<ejb-name>ejbName1</ejb-name>" +
                                 "<interceptor-order>" +
                                 "<interceptor-class>com.ibm.className0</interceptor-class>" +
                                 "<interceptor-class>com.ibm.className1</interceptor-class>" +
                                 "<interceptor-class>com.ibm.className2</interceptor-class>" +
                                 "</interceptor-order>" +
                                 "<exclude-default-interceptors>false</exclude-default-interceptors>" +
                                 "<exclude-class-interceptors>false</exclude-class-interceptors>" +
                                 "</interceptor-binding>";

    String interceptorBinding2 = "<interceptor-binding>" +
                                 "<ejb-name>ejbName2</ejb-name>" +
                                 "</interceptor-binding>";

    String interceptorBinding = EJBJarTest.ejbJar30() +
                                "<assembly-descriptor>" +
                                interceptorBinding0 +
                                interceptorBinding1 +
                                interceptorBinding2 +
                                "</assembly-descriptor>" +
                                "</ejb-jar>";

    @Test
    public void testInterceptorBinding() throws Exception {
        AssemblyDescriptor assemblyDescriptor = getAssemblyDescriptor(interceptorBinding);
        List<InterceptorBinding> intBindList = assemblyDescriptor.getInterceptorBinding();
        InterceptorBinding intBind0 = intBindList.get(0);

        Assert.assertEquals("ejbName0", intBind0.getEjbName());
        List<String> intClassNameList = intBind0.getInterceptorClassNames();
        Assert.assertEquals("interceptorClass0", intClassNameList.get(0));
        Assert.assertEquals("interceptorClass1", intClassNameList.get(1));
        Assert.assertEquals(null, intBind0.getInterceptorOrder());
        Assert.assertEquals(true, intBind0.isSetExcludeDefaultInterceptors());
        Assert.assertEquals(true, intBind0.isExcludeDefaultInterceptors());
        Assert.assertEquals(true, intBind0.isSetExcludeClassInterceptors());
        Assert.assertEquals(true, intBind0.isExcludeClassInterceptors());
        Assert.assertEquals("namedMethod0", intBind0.getMethod().getMethodName());

        InterceptorBinding intBind1 = intBindList.get(1);
        Assert.assertEquals("ejbName1", intBind1.getEjbName());
        List<String> intOrderClassNameList = intBind1.getInterceptorOrder().getInterceptorClassNames();
        Assert.assertEquals("com.ibm.className0", intOrderClassNameList.get(0));
        Assert.assertEquals("com.ibm.className1", intOrderClassNameList.get(1));
        Assert.assertEquals("com.ibm.className2", intOrderClassNameList.get(2));
        Assert.assertEquals(0, intBind1.getInterceptorClassNames().size());
        Assert.assertEquals(true, intBind1.isSetExcludeDefaultInterceptors());
        Assert.assertEquals(false, intBind1.isExcludeDefaultInterceptors());
        Assert.assertEquals(true, intBind1.isSetExcludeClassInterceptors());
        Assert.assertEquals(false, intBind1.isExcludeClassInterceptors());
        Assert.assertEquals(null, intBind1.getMethod());

        InterceptorBinding intBind2 = intBindList.get(2);
        Assert.assertEquals("ejbName2", intBind2.getEjbName());
        Assert.assertEquals(false, intBind2.isSetExcludeDefaultInterceptors());
        Assert.assertEquals(false, intBind2.isExcludeDefaultInterceptors());
        Assert.assertEquals(false, intBind2.isSetExcludeClassInterceptors());
        Assert.assertEquals(false, intBind2.isExcludeClassInterceptors());
    }

    String messageDestination = EJBJarTest.ejbJar21() +
                                "<assembly-descriptor>" +
                                "<message-destination>" +
                                "<message-destination-name>messageDestinationName0</message-destination-name>" +
                                "<mapped-name>mappedName0</mapped-name>" +
                                "<lookup-name>lookupName0</lookup-name>" +
                                "</message-destination>" +
                                "<message-destination>" +
                                "<message-destination-name>messageDestinationName1</message-destination-name>" +
                                "<mapped-name>mappedName1</mapped-name>" +
                                "</message-destination>" +
                                "<message-destination>" +
                                "<message-destination-name>messageDestinationName2</message-destination-name>" +
                                "<lookup-name>lookupName2</lookup-name>" +
                                "</message-destination>" +
                                "</assembly-descriptor>" +
                                "</ejb-jar>";

    @Test
    public void testMessageDestination() throws Exception {
        AssemblyDescriptor assemblyDescriptor = getAssemblyDescriptor(messageDestination);
        List<MessageDestination> methDestList = assemblyDescriptor.getMessageDestinations();
        MessageDestination methDest0 = methDestList.get(0);
        MessageDestination methDest1 = methDestList.get(1);
        MessageDestination methDest2 = methDestList.get(2);
        Assert.assertEquals("messageDestinationName0", methDest0.getName());
        Assert.assertEquals("messageDestinationName1", methDest1.getName());
        Assert.assertEquals("messageDestinationName2", methDest2.getName());

        Assert.assertEquals("mappedName0", methDest0.getMappedName());
        Assert.assertEquals("mappedName1", methDest1.getMappedName());
        Assert.assertEquals(null, methDest2.getMappedName());

        Assert.assertEquals("lookupName0", methDest0.getLookupName());
        Assert.assertEquals(null, methDest1.getLookupName());
        Assert.assertEquals("lookupName2", methDest2.getLookupName());
    }

    String excludeList = EJBJarTest.ejbJar20() +
                         "<assembly-descriptor>" +
                         "<exclude-list>" +
                         method0 +
                         method1 +
                         method2 +
                         "</exclude-list>" +
                         "</assembly-descriptor>" +
                         "</ejb-jar>";

    @Test
    public void testExcludeList() throws Exception {
        AssemblyDescriptor assemblyDescriptor = getAssemblyDescriptor(excludeList);
        ExcludeList excludeList = assemblyDescriptor.getExcludeList();
        List<Method> methodList = excludeList.getMethodElements();
        testMethod0(methodList.get(0));
        Assert.assertEquals("methodName0", methodList.get(0).getMethodName());
        Assert.assertEquals("methodName1", methodList.get(1).getMethodName());
        Assert.assertEquals("methodName2", methodList.get(2).getMethodName());
    }

    String applicationException = EJBJarTest.ejbJar30() +
                                  "<assembly-descriptor>" +
                                  "<application-exception>" +
                                  "<exception-class>exceptionClass0</exception-class>" +
                                  "<rollback>true</rollback>" +
                                  "<inherited>true</inherited>" +
                                  "</application-exception>" +

                                  "<application-exception>" +
                                  "<exception-class>exceptionClass1</exception-class>" +
                                  "<rollback>false</rollback>" +
                                  "<inherited>false</inherited>" +
                                  "</application-exception>" +

                                  "<application-exception>" +
                                  "<exception-class>exceptionClass2</exception-class>" +
                                  "</application-exception>" +
                                  "</assembly-descriptor>" +
                                  "</ejb-jar>";

    @Test
    public void testApplicationException() throws Exception {
        AssemblyDescriptor assemblyDescriptor = getAssemblyDescriptor(applicationException);
        List<ApplicationException> appExList = assemblyDescriptor.getApplicationExceptionList();
        ApplicationException appEx0 = appExList.get(0);
        ApplicationException appEx1 = appExList.get(1);
        ApplicationException appEx2 = appExList.get(2);
        Assert.assertEquals("exceptionClass0", appEx0.getExceptionClassName());
        Assert.assertEquals("exceptionClass1", appEx1.getExceptionClassName());
        Assert.assertEquals("exceptionClass2", appEx2.getExceptionClassName());

        Assert.assertEquals(true, appEx0.isRollback());
        Assert.assertEquals(true, appEx0.isSetRollback());
        Assert.assertEquals(false, appEx1.isRollback());
        Assert.assertEquals(true, appEx1.isSetRollback());
        Assert.assertEquals(false, appEx2.isRollback());
        Assert.assertEquals(false, appEx2.isSetRollback());

        Assert.assertEquals(true, appEx0.isInherited());
        Assert.assertEquals(true, appEx0.isSetInherited());
        Assert.assertEquals(false, appEx1.isInherited());
        Assert.assertEquals(true, appEx1.isSetInherited());
        Assert.assertEquals(false, appEx2.isInherited());
        Assert.assertEquals(false, appEx2.isSetInherited());
    }

    String assemblyDescriptor = EJBJarTest.ejbJar11() +
                                "<assembly-descriptor>" +
                                securityRole +
                                methodPermission0 +
                                methodPermission1 +
                                containerTransactions +
                                "</assembly-descriptor>" +
                                "</ejb-jar>";

    AssemblyDescriptor getAssemblyDescriptor() throws Exception {
        return getAssemblyDescriptor(assemblyDescriptor);
    }

    AssemblyDescriptor getAssemblyDescriptor(String stringBean) throws Exception {
        EJBJar ejbJar = getEJBJar(stringBean);
        return ejbJar.getAssemblyDescriptor();
    }

    @Test
    public void testMethodIntfLifecycleCallback() throws Exception {
        EJBJar ejbJar = parse(ejbJar32() +
                              "<assembly-descriptor>" +
                              "  <container-transaction>" +
                              "    <method>" +
                              "      <ejb-name>ejb0</ejb-name>" +
                              "      <method-intf>LifecycleCallback</method-intf>" +
                              "      <method-name>method0</method-name>" +
                              "    </method>" +
                              "    <trans-attribute>Required</trans-attribute>" +
                              "  </container-transaction>" +
                              "</assembly-descriptor>" +
                              "</ejb-jar>");
        Assert.assertEquals(Method.INTERFACE_TYPE_LIFECYCLE_CALLBACK,
                            ejbJar.getAssemblyDescriptor().getContainerTransactions().get(0).getMethodElements().get(0).getInterfaceTypeValue());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testMethodIntfLifecycleCallbackEJB31() throws Exception {
        parse(ejbJar31() +
              "<assembly-descriptor>" +
              "  <container-transaction>" +
              "    <method>" +
              "      <ejb-name>ejb0</ejb-name>" +
              "      <method-intf>LifecycleCallback</method-intf>" +
              "      <method-name>method0</method-name>" +
              "    </method>" +
              "    <trans-attribute>Required</trans-attribute>" +
              "  </container-transaction>" +
              "</assembly-descriptor>" +
              "</ejb-jar>");
    }

    @Test
    public void testMethodIntfErrorEJB31() throws Exception {
        try {
            parse(ejbJar31() +
                  "<assembly-descriptor>" +
                  "  <container-transaction>" +
                  "    <method>" +
                  "      <ejb-name>ejb0</ejb-name>" +
                  "      <method-intf>Invalid</method-intf>" +
                  "      <method-name>method0</method-name>" +
                  "    </method>" +
                  "    <trans-attribute>Required</trans-attribute>" +
                  "  </container-transaction>" +
                  "</assembly-descriptor>" +
                  "</ejb-jar>");
            Assert.fail("expected ParseException");
        } catch (DDParser.ParseException ex) {
            if (ex.getMessage().contains("LifecycleCallback")) {
                throw ex;
            }
        }
    }

    @Test
    public void testMethodIntfErrorEJB32() throws Exception {
        try {
            parse(ejbJar32() +
                  "<assembly-descriptor>" +
                  "  <container-transaction>" +
                  "    <method>" +
                  "      <ejb-name>ejb0</ejb-name>" +
                  "      <method-intf>Invalid</method-intf>" +
                  "      <method-name>method0</method-name>" +
                  "    </method>" +
                  "    <trans-attribute>Required</trans-attribute>" +
                  "  </container-transaction>" +
                  "</assembly-descriptor>" +
                  "</ejb-jar>");
            Assert.fail("expected ParseException");
        } catch (DDParser.ParseException ex) {
            if (!ex.getMessage().contains("LifecycleCallback")) {
                throw ex;
            }
        }
    }
}
