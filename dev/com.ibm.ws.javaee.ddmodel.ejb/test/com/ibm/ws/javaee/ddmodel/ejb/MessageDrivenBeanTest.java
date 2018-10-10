/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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

import com.ibm.ws.javaee.dd.common.InterceptorCallback;
import com.ibm.ws.javaee.dd.ejb.ActivationConfig;
import com.ibm.ws.javaee.dd.ejb.ActivationConfigProperty;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejb.MessageDriven;
import com.ibm.ws.javaee.dd.ejb.NamedMethod;
import com.ibm.ws.javaee.dd.ejb.Timer;
import com.ibm.ws.javaee.dd.ejb.TransactionalBean;

public class MessageDrivenBeanTest extends EJBJarTestBase {

    @Test
    public void testMessageDrivenBeanMethods() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar20() +
                                               "<enterprise-beans>" +
                                               "<message-driven>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "</message-driven>" +

                                               "<message-driven>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
                                               "<ejb-class>ejbClass1</ejb-class>" +
                                               "<mapped-name>mappedName1</mapped-name>" +
                                               "<messaging-type>messagingType1</messaging-type>" +
                                               "<message-destination-type>messageDestinationType0</message-destination-type>" +
                                               "<message-destination-link>messageDestinationLink0</message-destination-link>" +
                                               "</message-driven>" +

                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();

        Assert.assertEquals(2, beans.size());

        MessageDriven bean0 = (MessageDriven) beans.get(0);
        Assert.assertEquals(EnterpriseBean.KIND_MESSAGE_DRIVEN, bean0.getKindValue());
        Assert.assertEquals("ejbName0", bean0.getName());
        Assert.assertEquals(null, bean0.getEjbClassName());
        Assert.assertEquals(null, bean0.getMappedName());
        Assert.assertEquals(null, bean0.getMessagingTypeName());
        Assert.assertEquals(null, bean0.getMessageDestinationName());
        Assert.assertEquals(null, bean0.getLink());

        MessageDriven bean1 = (MessageDriven) beans.get(1);
        Assert.assertEquals("ejbName1", bean1.getName());
        Assert.assertEquals("ejbClass1", bean1.getEjbClassName());
        Assert.assertEquals("mappedName1", bean1.getMappedName());
        Assert.assertEquals("messagingType1", bean1.getMessagingTypeName());
        Assert.assertEquals("messageDestinationType0", bean1.getMessageDestinationName());
        Assert.assertEquals("messageDestinationLink0", bean1.getLink());
    }

    @Test
    public void testMessageDrivenActivationConfig() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar21() +
                                               "<enterprise-beans>" +
                                               "<message-driven>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "</message-driven>" +

                                               "<message-driven>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
                                               "<activation-config>" +
                                               "<activation-config-property>" +
                                               "<activation-config-property-name>name0</activation-config-property-name>" +
                                               "<activation-config-property-value>value0</activation-config-property-value>" +
                                               "</activation-config-property>" +
                                               "<activation-config-property>" +
                                               "<activation-config-property-name>name1</activation-config-property-name>" +
                                               "<activation-config-property-value>value1</activation-config-property-value>" +
                                               "</activation-config-property>" +
                                               "<activation-config-property>" +
                                               "<activation-config-property-name>name2</activation-config-property-name>" +
                                               "<activation-config-property-value>value2</activation-config-property-value>" +
                                               "</activation-config-property>" +
                                               "</activation-config>" +
                                               "</message-driven>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();

        MessageDriven bean0 = (MessageDriven) beans.get(0);
        Assert.assertEquals(EnterpriseBean.KIND_MESSAGE_DRIVEN, bean0.getKindValue());
        Assert.assertEquals(null, bean0.getActivationConfigValue());

        MessageDriven bean1 = (MessageDriven) beans.get(1);
        ActivationConfig actCon0 = bean1.getActivationConfigValue();
        List<ActivationConfigProperty> actConProps = actCon0.getConfigProperties();
        Assert.assertEquals("name0", actConProps.get(0).getName());
        Assert.assertEquals("value0", actConProps.get(0).getValue());
        Assert.assertEquals("name1", actConProps.get(1).getName());
        Assert.assertEquals("value1", actConProps.get(1).getValue());
        Assert.assertEquals("name2", actConProps.get(2).getName());
        Assert.assertEquals("value2", actConProps.get(2).getValue());
    }

    @Test
    public void testMessageDrivenActivationConfigEJB20() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar20() +
                                               "<enterprise-beans>" +
                                               "<message-driven>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "<message-selector>messageSelector0</message-selector>" +
                                               "<acknowledge-mode>Auto-acknowledge</acknowledge-mode>" +
                                               "<message-driven-destination id=\"tid\">" +
                                               "<destination-type>javax.jms.Queue</destination-type>" +
                                               "<subscription-durability>Durable</subscription-durability>" +
                                               "</message-driven-destination>" +
                                               "</message-driven>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        MessageDriven bean0 = (MessageDriven) beans.get(0);
        ActivationConfig actCon0 = bean0.getActivationConfigValue();
        List<ActivationConfigProperty> actConProps = actCon0.getConfigProperties();
        Assert.assertEquals(actConProps.toString(), 4, actConProps.size());
        Assert.assertEquals(MessageDriven.ACTIVATION_CONFIG_PROPERTY_MESSAGE_SELECTOR, actConProps.get(0).getName());
        Assert.assertEquals("messageSelector0", actConProps.get(0).getValue());
        Assert.assertEquals(MessageDriven.ACTIVATION_CONFIG_PROPERTY_ACKNOWLEDGE_MODE, actConProps.get(1).getName());
        Assert.assertEquals("Auto-acknowledge", actConProps.get(1).getValue());
        Assert.assertEquals(MessageDriven.ACTIVATION_CONFIG_PROPERTY_DESTINATION_TYPE, actConProps.get(2).getName());
        Assert.assertEquals("javax.jms.Queue", actConProps.get(2).getValue());
        Assert.assertEquals(MessageDriven.ACTIVATION_CONFIG_PROPERTY_SUBSCRIPTION_DURABILITY, actConProps.get(3).getName());
        Assert.assertEquals("Durable", actConProps.get(3).getValue());
    }

    @Test
    public void testMessageDrivenTimeOutServiceBean() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar20() +
                                               "<enterprise-beans>" +
                                               "<message-driven>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "</message-driven>" +

                                               "<message-driven>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
                                               "<timeout-method>" +
                                               "<method-name>methodName0</method-name>" +
                                               "<method-params>" +
                                               "<method-param>methParam0</method-param>" +
                                               "<method-param>methParam1</method-param>" +
                                               "<method-param>methParam2</method-param>" +
                                               "</method-params>" +
                                               "</timeout-method>" +

                                               "<timer>" +
                                               "<start>start0</start>" +
                                               "<end>end0</end>" +
                                               "<timeout-method>" +
                                               "<method-name>timerTimeoutMethod0</method-name>" +
                                               "</timeout-method>" +
                                               "</timer>" +

                                               "</message-driven>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();

        MessageDriven bean0 = (MessageDriven) beans.get(0);
        Assert.assertEquals(EnterpriseBean.KIND_MESSAGE_DRIVEN, bean0.getKindValue());
        Assert.assertEquals(null, bean0.getTimeoutMethod());
        Assert.assertEquals(true, bean0.getTimers().isEmpty());

        MessageDriven bean1 = (MessageDriven) beans.get(1);
        NamedMethod namedMeth0 = bean1.getTimeoutMethod();
        Assert.assertEquals("methodName0", namedMeth0.getMethodName());
        Assert.assertEquals("methParam0", namedMeth0.getMethodParamList().get(0));
        Assert.assertEquals("methParam1", namedMeth0.getMethodParamList().get(1));
        Assert.assertEquals("methParam2", namedMeth0.getMethodParamList().get(2));

        Timer timer0 = bean1.getTimers().get(0);
        Assert.assertEquals("start0", timer0.getStart());
        Assert.assertEquals("end0", timer0.getEnd());
        Assert.assertEquals("timerTimeoutMethod0", timer0.getTimeoutMethod().getMethodName());
    }

    @Test
    public void testMessageDrivenTransactionalBean() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar20() +
                                               "<enterprise-beans>" +
                                               "<message-driven>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "</message-driven>" +

                                               "<message-driven>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
                                               "<transaction-type>Bean</transaction-type>" +
                                               "</message-driven>" +

                                               "<message-driven>" +
                                               "<ejb-name>ejbName2</ejb-name>" +
                                               "<transaction-type>Container</transaction-type>" +
                                               "</message-driven>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        MessageDriven mdb0 = (MessageDriven) beans.get(0);
        MessageDriven mdb1 = (MessageDriven) beans.get(1);
        MessageDriven mdb2 = (MessageDriven) beans.get(2);

        Assert.assertEquals(TransactionalBean.TRANSACTION_TYPE_UNSPECIFIED, mdb0.getTransactionTypeValue());
        Assert.assertEquals(TransactionalBean.TRANSACTION_TYPE_BEAN, mdb1.getTransactionTypeValue());
        Assert.assertEquals(TransactionalBean.TRANSACTION_TYPE_CONTAINER, mdb2.getTransactionTypeValue());
    }

    @Test
    public void testMessageDrivenMethodInterceptorBean() throws Exception {
        List<EnterpriseBean> beans = getEJBJar(EJBJarTest.ejbJar20() +
                                               "<enterprise-beans>" +
                                               "<message-driven>" +
                                               "<ejb-name>ejbName0</ejb-name>" +
                                               "</message-driven>" +

                                               "<message-driven>" +
                                               "<ejb-name>ejbName1</ejb-name>" +
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
                                               "</message-driven>" +
                                               "</enterprise-beans>" +
                                               "</ejb-jar>").getEnterpriseBeans();
        MessageDriven mdb0 = (MessageDriven) beans.get(0);
        MessageDriven mdb1 = (MessageDriven) beans.get(1);
        Assert.assertEquals(true, mdb0.getAroundInvoke().isEmpty());
        Assert.assertEquals(true, mdb0.getAroundTimeoutMethods().isEmpty());

        InterceptorCallback intCallback0 = mdb1.getAroundInvoke().get(0);
        Assert.assertEquals("aroundInvokeClass0", intCallback0.getClassName());
        Assert.assertEquals("aroundInvokeMethodName0", intCallback0.getMethodName());
        InterceptorCallback intCallback1 = mdb1.getAroundInvoke().get(1);
        Assert.assertEquals(null, intCallback1.getClassName());
        Assert.assertEquals("aroundInvokeMethodName1", intCallback1.getMethodName());

        InterceptorCallback intCallback2 = mdb1.getAroundTimeoutMethods().get(0);
        Assert.assertEquals("aroundTimeoutClass0", intCallback2.getClassName());
        Assert.assertEquals("aroundTimeoutMethodName0", intCallback2.getMethodName());
    }

}
