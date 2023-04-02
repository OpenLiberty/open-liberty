/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.messaging.lifecycle;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;


/**
 * There are various singleton objects in messaging that have been declared as OSGi Service Components.
 * 
 * <h1> The old approach </h1>
 * Singleton services were typically injected into other components and then assigned to static fields.
 * Other code (outside a service component) accessed the singletons via these static fields.
 * Although a field was usually initialised ahead of its first use it occasionally wasn't.
 * That approach lead to timing-sensitive bugs, which were hard to reproduce and diagnose.
 *
 * <h1> The ideal approach </h1>
 * Ideally, the using code should be part of a service component too, so that a suitable dependency
 * can be declared, and the timing window eliminated. 
 * <h2> &hellip; and why we aren't using it</h2>
 * This ideal approach is not always easy due to existing usage.
 * In particular, JCA makes this <em>very</em> hard to achieve.
 * <p> 
 * Even when it is possible, it can be laborious and error-prone.
 * We discovered this over several months of re-engineering work.
 * 
 * <h1> The new approach </h1>
 * The {@link Singleton} mechanism allows a compromise, helping to avoid heisenbugs without major re-work.
 * If an implementation class is visible to its users and has no OSGi dependencies, convert it into a standard Java singleton object.
 * Otherwise, allow a bundle to declare its singletons in <code>defaultInstances.xml</code>.
 * 
 * <p>
 * The declaration actually enables a {@link SingletonAgent} with the appropriate type property.
 * 
 * <h2> &hellip; and how to use it </h2>
 *
 * <ul>
 *  <li>In <code>resources/OSGI-INF/wlp/defaultInstances.xml</code>, declare the singleton agent:<pre>
 *    &lt;messaging-singleton type="com.acme.gadget.widget" /&gt;</pre></li>
 *  <li>Identify the singleton class, e.g. <code>com.acme.gadget.internal.WidgetImpl</code> which implements <code>com.acme.gadget.Widget</code></li>
 *  <li>Choose a unique singleton name for your singleton, e.g. <code>com.acme.gadget.widget</code></li>
 *  <li>In <code>bnd.bnd</code>, include resources, point to the default config, and enable component annotations:<pre>
 *    Include-Resource: OSGI-INF=resources/OSGI-INF
 *    &hellip;
 *    IBM-Default-Config: OSGI-INF/wlp/defaultInstances.xml
 *    &hellip;
 *    -dsannotations: com.acme.gadget.internal.WidgetImpl</pre></li>
 *  <li>Ensure the interface/abstract class extends {@link Singleton}: <pre>
 *    package com.acme.gadget;
 *    &hellip;
 *    public abstract class Widget extends {@link Singleton}</pre>
 *  Make sure any relevant interface extends {@link Singleton} so it can be passed to {@link SingletonsReady#requireService(Class)} to retrieve the service.<br>
 *  Note: each such interface must only apply to one singleton class (and therefore one object) or the mechanism will report an error.</li>
 *  <li>For the implementation class, there are several steps
 *   <ul>
 *    <li>Annotate the class with <code>@</code>{@link Component}</li>
 *    <li>Set {@link Component#configurationPolicy()} to {@link ConfigurationPolicy#IGNORE}</li>
 *    <li>Ensure {@link Component#service()} includes <code>Singleton.class</code></li>
 *    <li>Ensure {@link Component#property()} includes the singleton type</li>
 *   </ul><pre>
 *    &commat;Component(
 *        service = {Singleton.class, Widget.class},
 *        configurationPolicy = IGNORE,
 *        property = {
 *            "type=com.acme.gadget.widget", 
 *            "service.vendor=ACME"
 *        })
 *    public class WidgetImpl extends Widget {
 *        &hellip;
 *    }
 *   </pre>
 *  </li>
 * </ul>
 *
 *
 * @see SingletonMonitor
 * @see SingletonsReady
 * @see SingletonAgent
 */
public interface Singleton {}
