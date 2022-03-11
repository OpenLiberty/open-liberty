/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.lifecycle;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

/**
 * There are various singleton objects in messaging that have been declared as OSGi Service Components.
 * These were typically retrieved from the Service Component Runtime by other components and then
 * assigned to static fields in order to make them available to other code that was not part of a
 * service component.
 *
 * <p>
 * This approach can lead to timing-sensitive bugs, where a field is usually initialised well ahead
 * of its first use but occasionally isn't. These bugs are hard to reproduce and to diagnose.
 *
 * <p>
 * Ideally, the using code should be part of a service component too, so that a suitable dependency
 * can be declared, and the timing window eliminated. This is not always possible due to existing usage.
 * In particular, JCA makes this <em>very</em> hard to achieve.
 * Even when it is possible, it can prove laborious and error-prone.
 *
 * <p>
 * This interface was introduced to reduce the opportunity for such errors and to make them easier to diagnose when they do occur.
 * If at all possible, any class that is slightly OSGi-ified in this way should be made back into a POJO singleton.
 * Where that is not possible, use this interface as the easiest alternative that can help engineer the behaviour.
 * <p>
 * Using this interface is simple. There are just a few steps to success:
 *
 * <ul>
 *  <li>Make sure any relevant interface extends {@link Singleton} so it can be passed to {@link SingletonsReady#requireService(Class)} to retrieve the service.<br>
 *      Note: each such interface must only apply to one singleton class (and therefore one object) or the mechanism will report an error.</li>
 *  <li>In the {@link Component} annotation:
 *   <ul>
 *    <li>Set {@link Component#configurationPolicy()} to {@link ConfigurationPolicy#REQUIRE}.</li>
 *    <li>Ensure {@link Component#service()} includes <code>Singleton.class</code>,<br>
 *        OR do not declare this attribute and ensure the implementation class implements {@link Singleton} directly.</li>
 *   </ul>
 *  </li>
 *  <li>In the <code>metatype.xml</code>:
 *   <ul>
 *    <li>Declare the <code>Designate</code> for the component with a <code>pid</code>, not a <code>factoryPid</code></li>
 *    <li>Declare <code>OCD</code> for the <code>Designate</code> with <code>ibm:objectClass="</code>{@link com.ibm.ws.messaging.lifecycle.Singleton}<code>"</code></li>
 *   </ul>
 *  </li>
 *  <li>In <code>defaultInstances.xml</code>, configure the instance of the Component.</li>
 *  <li>In <code>bnd.bnd</code>:
 *   <ul>
 *    <li>Use <code>Include-Resource: OSGI-INF=resources/OSGI-INF</code> to pull metatype and default instances into the bundle.</li>
 *    <li>Use <code>IBM-Default-Config: OSGI-INF/wlp/defaultInstances.xml</code> to ensure the default instances are discovered.</li>
 *    <li>Ensure <code>-dsannotations</code> includes the fully qualified component class name</li>
 *   </ul>
 *  </li>
 * </ul>
 *
 *
 * @see SingletonMonitor
 * @see SingletonsReady
 */
public interface Singleton {}
