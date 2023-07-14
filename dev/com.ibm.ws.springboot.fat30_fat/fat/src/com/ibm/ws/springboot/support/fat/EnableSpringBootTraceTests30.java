/*******************************************************************************
 * Copyright (c) 2018,2023 IBM Corporation and others.
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
package com.ibm.ws.springboot.support.fat;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

// Disabled: Failing to start web server.

// org.springframework.context.ApplicationContextException: Unable to start web serve
// 	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.onRefresh(ServletWebServerApplicationContext.java:164) ~[spring-boot-3.0.4.jar:3.0.4
// 	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:578) ~[spring-context-6.0.6.jar:6.0.6
// 	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146) ~[spring-boot-3.0.4.jar:3.0.4
// 	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:732) ~[spring-boot-3.0.4.jar:3.0.4
// 	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:434) ~[spring-boot-3.0.4.jar:3.0.4
// 	at org.springframework.boot.SpringApplication.run(SpringApplication.java:310) ~[spring-boot-3.0.4.jar:3.0.4
// 	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1304) ~[spring-boot-3.0.4.jar:3.0.4
// 	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1293) ~[spring-boot-3.0.4.jar:3.0.4
// 	at com.ibm.ws.springboot.fat30.test.app.TestApplication.main(TestApplication.java:36) ~[com.ibm.ws.springboot.fat30.app-0.0.1-SNAPSHOT.spring:na
// 	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na
// 	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[na:na
// 	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na
// 	at java.base/java.lang.reflect.Method.invoke(Method.java:568) ~[na:na
// 	at com.ibm.ws.app.manager.springboot.internal.SpringBootRuntimeContainer.lambda$invokeSpringMain$6(SpringBootRuntimeContainer.java:139) ~[na:na
// 	at com.ibm.ws.threading.internal.ExecutorServiceImpl$RunnableWrapper.run(ExecutorServiceImpl.java:247) ~[na:na
// 	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136) ~[na:na
// 	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635) ~[na:na
// 	at java.base/java.lang.Thread.run(Thread.java:857) ~[na:na
// Caused by: org.springframework.boot.web.server.WebServerException: Unable to start embedded Tomca
// 	at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.initialize(TomcatWebServer.java:142) ~[spring-boot-3.0.4.jar:3.0.4
// 	at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.<init>(TomcatWebServer.java:104) ~[spring-boot-3.0.4.jar:3.0.4
// 	at org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory.getTomcatWebServer(TomcatServletWebServerFactory.java:488) ~[spring-boot-3.0.4.jar:3.0.4
// 	at org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory.getWebServer(TomcatServletWebServerFactory.java:210) ~[spring-boot-3.0.4.jar:3.0.4
// 	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.createWebServer(ServletWebServerApplicationContext.java:183) ~[spring-boot-3.0.4.jar:3.0.4
// 	at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.onRefresh(ServletWebServerApplicationContext.java:161) ~[spring-boot-3.0.4.jar:3.0.4
// 	... 17 common frames omitte
// Caused by: org.apache.catalina.LifecycleException: A child container failed during star
// 	at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:890) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.core.StandardEngine.startInternal(StandardEngine.java:241) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:183) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.core.StandardService.startInternal(StandardService.java:428) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:183) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.core.StandardServer.startInternal(StandardServer.java:913) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:183) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.startup.Tomcat.start(Tomcat.java:485) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.initialize(TomcatWebServer.java:123) ~[spring-boot-3.0.4.jar:3.0.4
// 	... 22 common frames omitte
// Caused by: java.util.concurrent.ExecutionException: org.apache.catalina.LifecycleException: A child container failed during star
// 	at java.base/java.util.concurrent.FutureTask.report(FutureTask.java:122) ~[na:na
// 	at java.base/java.util.concurrent.FutureTask.get(FutureTask.java:191) ~[na:na
// 	at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:878) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	... 30 common frames omitte
// Caused by: org.apache.catalina.LifecycleException: A child container failed during star
// 	at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:890) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.core.StandardHost.startInternal(StandardHost.java:846) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:183) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1332) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1322) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264) ~[na:na
// 	at org.apache.tomcat.util.threads.InlineExecutorService.execute(InlineExecutorService.java:75) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at java.base/java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:145) ~[na:na
// 	at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:871) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	... 30 common frames omitte
// Caused by: java.util.concurrent.ExecutionException: org.apache.catalina.LifecycleException: Failed to initialize component [org.apache.catalina.webresources.StandardRoot@f618ee6e
// 	at java.base/java.util.concurrent.FutureTask.report(FutureTask.java:122) ~[na:na
// 	at java.base/java.util.concurrent.FutureTask.get(FutureTask.java:191) ~[na:na
// 	at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:878) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	... 38 common frames omitte
// Caused by: org.apache.catalina.LifecycleException: Failed to initialize component [org.apache.catalina.webresources.StandardRoot@f618ee6e
// 	at org.apache.catalina.util.LifecycleBase.handleSubClassException(LifecycleBase.java:440) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.util.LifecycleBase.init(LifecycleBase.java:139) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:173) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.core.StandardContext.resourcesStart(StandardContext.java:4567) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.core.StandardContext.startInternal(StandardContext.java:4700) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:183) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1332) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.core.ContainerBase$StartChild.call(ContainerBase.java:1322) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264) ~[na:na
// 	at org.apache.tomcat.util.threads.InlineExecutorService.execute(InlineExecutorService.java:75) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at java.base/java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:145) ~[na:na
// 	at org.apache.catalina.core.ContainerBase.startInternal(ContainerBase.java:871) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	... 38 common frames omitte
// Caused by: java.lang.Error: factory already define
// 	at java.base/java.net.URL.setURLStreamHandlerFactory(URL.java:1228) ~[na:na
// 	at org.apache.catalina.webresources.TomcatURLStreamHandlerFactory.<init>(TomcatURLStreamHandlerFactory.java:121) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.webresources.TomcatURLStreamHandlerFactory.getInstanceInternal(TomcatURLStreamHandlerFactory.java:52) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.webresources.TomcatURLStreamHandlerFactory.register(TomcatURLStreamHandlerFactory.java:73) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.webresources.StandardRoot.registerURLStreamHandlerFactory(StandardRoot.java:699) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.webresources.StandardRoot.initInternal(StandardRoot.java:686) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	at org.apache.catalina.util.LifecycleBase.init(LifecycleBase.java:136) ~[tomcat-embed-core-10.1.8.jar:10.1.8
// 	... 48 common frames omitted

@RunWith(FATRunner.class)
@Mode(FULL)
public class EnableSpringBootTraceTests30 extends CommonWebServerTests {

    @Override
    public Set<String> getFeatures() {
        return Collections.singleton("springBoot-3.0");
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_BASE;
    }

    @Override
    public Map<String, String> getBootStrapProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("com.ibm.ws.logging.trace.specification", "*=audit=enabled:springboot=all");
        return properties;
    }

    @After
    public void stopTestServer() throws Exception {
        super.stopServer(true);
    }

    @Test
    public void testEnableSpringBootTraceFor30() throws Exception {
        testBasicSpringBootApplication();
    }
}
