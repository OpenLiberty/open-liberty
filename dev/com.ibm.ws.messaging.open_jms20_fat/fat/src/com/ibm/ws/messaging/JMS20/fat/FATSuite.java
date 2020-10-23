/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.JMS20.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.messaging.JMS20.fat.ContextInject.JMSContextInjectTest;
import com.ibm.ws.messaging.JMS20.fat.DurableUnshared.DurableUnsharedTest;
import com.ibm.ws.messaging.JMS20.fat.JMSConsumerTest.JMSConsumerTest_118076;
import com.ibm.ws.messaging.JMS20.fat.JMSConsumerTest.JMSConsumerTest_118077;
import com.ibm.ws.messaging.JMS20.fat.JMSContextTest.JMSEjbJarXmlMdbTest;
import com.ibm.ws.messaging.JMS20.fat.JMSContextTest.JMSRedeliveryTest_120846;
import com.ibm.ws.messaging.JMS20.fat.JMSDCFTest.JMSDCFVarTest;
//import com.ibm.ws.messaging.JMS20.fat.JMSDCFTest.JMSDCFTest;
import com.ibm.ws.messaging.JMS20.fat.JMSMBeanTest.JMSMBeanTest;
import com.ibm.ws.messaging.JMS20.fat.JMSProducerTest.JMSProducerTest_118071;
import com.ibm.ws.messaging.JMS20.fat.JMSProducerTest.JMSProducerTest_118073;
import com.ibm.ws.messaging.JMS20.fat.JMSProducerTest.JMSProducer_Test118073;
import com.ibm.ws.messaging.JMS20.fat.SharedSubscription.SharedSubscriptionTest_129623;
import com.ibm.ws.messaging.JMS20.fat.SharedSubscription.SharedSubscriptionTest_129626;
import com.ibm.ws.messaging.JMS20.fat.SharedSubscription.SharedSubscriptionWithMsgSelTest_129623;
import com.ibm.ws.messaging.JMS20.fat.SharedSubscription.SharedSubscriptionWithMsgSelTest_129626;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

                DummyTest.class,
                LiteBucketSet1Test.class,
                LiteBucketSet2Test.class,

                // Fully disabled for jakarta.  j2ee-management is not supported by jakarta, and
                // is not compatible with the jakarta features used by the test class.
                JMSMBeanTest.class,

                JMSProducerTest_118071.class, //full
                JMSProducerTest_118073.class, //full
                SharedSubscriptionTest_129623.class,

                JMSConsumerTest_118076.class, //full
                JMSConsumerTest_118077.class, //full
                JMSRedeliveryTest_120846.class,

                SharedSubscriptionWithMsgSelTest_129623.class,
                SharedSubscriptionWithMsgSelTest_129626.class, //full 2nd
                SharedSubscriptionTest_129626.class, //full 2nd
                JMSProducer_Test118073.class, //full

                DurableUnsharedTest.class,

                JMSContextInjectTest.class, //full

// Stack Dump = javax.naming.NameNotFoundException: javax.naming.NameNotFoundException: java:comp/DefaultJMSConnectionFactory
// at com.ibm.ws.jndi.url.contexts.javacolon.internal.JavaURLContext.lookup(JavaURLContext.java:355)
// at com.ibm.ws.jndi.url.contexts.javacolon.internal.JavaURLContext.lookup(JavaURLContext.java:370)
// at org.apache.aries.jndi.DelegateContext.lookup(DelegateContext.java:149)
// at javax.naming.InitialContext.lookup(InitialContext.java:428)
// at com.ibm.ws.injectionengine.osgi.internal.IndirectJndiLookupObjectFactory.getObjectInstance(IndirectJndiLookupObjectFactory.java:166)
// at com.ibm.ws.injectionengine.osgi.internal.IndirectJndiLookupObjectFactory.getObjectInstance(IndirectJndiLookupObjectFactory.java:100)
// at com.ibm.wsspi.injectionengine.InjectionBinding.getInjectionObjectInstance(InjectionBinding.java:1552)
// at com.ibm.wsspi.injectionengine.InjectionBinding.getInjectionObject(InjectionBinding.java:1428)
// at com.ibm.wsspi.injectionengine.InjectionBinding.getInjectableObject(InjectionBinding.java:1368)
// at com.ibm.wsspi.injectionengine.InjectionTarget.inject(InjectionTarget.java:104)
// at com.ibm.ws.cdi.impl.weld.injection.WebSphereInjectionServicesImpl.inject(WebSphereInjectionServicesImpl.java:198)
// at com.ibm.ws.cdi.impl.weld.injection.WebSphereInjectionServicesImpl.inject(WebSphereInjectionServicesImpl.java:150)
// at com.ibm.ws.cdi.impl.weld.injection.WebSphereInjectionServicesImpl.access$000(WebSphereInjectionServicesImpl.java:74)
// at com.ibm.ws.cdi.impl.weld.injection.WebSphereInjectionServicesImpl$1.run(WebSphereInjectionServicesImpl.java:134)
// at com.ibm.ws.cdi.impl.weld.injection.WebSphereInjectionServicesImpl$1.run(WebSphereInjectionServicesImpl.java:130)
// at java.security.AccessController.doPrivileged(AccessController.java:694)
// at com.ibm.ws.cdi.impl.weld.injection.WebSphereInjectionServicesImpl.callInject(WebSphereInjectionServicesImpl.java:130)
// at com.ibm.ws.cdi.impl.weld.injection.WebSphereInjectionServicesImpl.injectJavaEEResources(WebSphereInjectionServicesImpl.java:112)
// at com.ibm.ws.cdi.impl.weld.injection.WebSphereInjectionServicesImpl.aroundInject(WebSphereInjectionServicesImpl.java:317)
// at org.jboss.weld.injection.InjectionContextImpl.run(InjectionContextImpl.java:46)
// at org.jboss.weld.injection.producer.ResourceInjector.inject(ResourceInjector.java:71)
// at org.jboss.weld.injection.producer.BasicInjectionTarget.inject(BasicInjectionTarget.java:117)
// at com.ibm.ws.cdi.impl.managedobject.CDIManagedObject.cdiInjection(CDIManagedObject.java:202)
// at com.ibm.ws.cdi.impl.managedobject.CDIManagedObject.inject(CDIManagedObject.java:175)
// at com.ibm.ws.cdi.impl.managedobject.CDIManagedObject.inject(CDIManagedObject.java:153)
// at com.ibm.ws.webcontainer.osgi.webapp.WebApp.inject(WebApp.java:1277)
// at com.ibm.ws.webcontainer.osgi.webapp.WebApp.injectAndPostConstruct(WebApp.java:1418)
// at com.ibm.ws.webcontainer.osgi.webapp.WebApp.injectAndPostConstruct(WebApp.java:1406)
// at com.ibm.ws.webcontainer.osgi.servlet.ServletWrapper.createTarget(ServletWrapper.java:63)
// at com.ibm.ws.webcontainer.servlet.ServletWrapper$1.run(ServletWrapper.java:1515)
// at java.security.AccessController.doPrivileged(AccessController.java:694)
// at com.ibm.ws.webcontainer.servlet.ServletWrapper.loadServlet(ServletWrapper.java:1483)
// at com.ibm.ws.webcontainer.servlet.ServletWrapper.handleRequest(ServletWrapper.java:573)
// at com.ibm.ws.webcontainer.servlet.ServletWrapper.handleRequest(ServletWrapper.java:426)
// at com.ibm.ws.webcontainer.filter.WebAppFilterManager.invokeFilters(WebAppFilterManager.java:1226)
// at com.ibm.ws.webcontainer.webapp.WebApp.handleRequest(WebApp.java:5057)
// at com.ibm.ws.webcontainer.osgi.DynamicVirtualHost$2.handleRequest(DynamicVirtualHost.java:314)
// at com.ibm.ws.webcontainer.WebContainer.handleRequest(WebContainer.java:1005)
// at com.ibm.ws.webcontainer.osgi.DynamicVirtualHost$2.run(DynamicVirtualHost.java:279)
// at com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink$TaskWrapper.run(HttpDispatcherLink.java:1134)
// at com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink.wrapHandlerAndExecute(HttpDispatcherLink.java:415)
// at com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink.ready(HttpDispatcherLink.java:374)
// at com.ibm.ws.http.channel.internal.inbound.HttpInboundLink.handleDiscrimination(HttpInboundLink.java:565)
// at com.ibm.ws.http.channel.internal.inbound.HttpInboundLink.handleNewRequest(HttpInboundLink.java:499)
// at com.ibm.ws.http.channel.internal.inbound.HttpInboundLink.processRequest(HttpInboundLink.java:359)
// at com.ibm.ws.http.channel.internal.inbound.HttpInboundLink.ready(HttpInboundLink.java:326)
// at com.ibm.ws.tcpchannel.internal.NewConnectionInitialReadCallback.sendToDiscriminators(NewConnectionInitialReadCallback.java:167)
// at com.ibm.ws.tcpchannel.internal.NewConnectionInitialReadCallback.complete(NewConnectionInitialReadCallback.java:75)
// at com.ibm.ws.tcpchannel.internal.WorkQueueManager.requestComplete(WorkQueueManager.java:504)
// at com.ibm.ws.tcpchannel.internal.WorkQueueManager.attemptIO(WorkQueueManager.java:574)
// at com.ibm.ws.tcpchannel.internal.WorkQueueManager.workerRun(WorkQueueManager.java:958)
// at com.ibm.ws.tcpchannel.internal.WorkQueueManager$Worker.run(WorkQueueManager.java:1047)
// at com.ibm.ws.threading.internal.ExecutorServiceImpl$RunnableWrapper.run(ExecutorServiceImpl.java:239)
// at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1160)
// at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
// at java.lang.Thread.run(Thread.java:811)

// xx                JMSDCFTest.class,
                JMSDCFVarTest.class, //full 2nd

                JMSEjbJarXmlMdbTest.class
})

public class FATSuite {
    // Run only during the Jakarta repeat for now.  When
    // the tests are removed from WS-CD-Open, the pre-jakarta
    // repeat can be re-enabled in open-liberty.

// TODO: Remove
//    public static RepeatTests r = RepeatTests.with(new JakartaEE9Action());

    @ClassRule
    public static RepeatTests repeater = RepeatTests.withoutModification().andWith(new JakartaEE9Action());
}
