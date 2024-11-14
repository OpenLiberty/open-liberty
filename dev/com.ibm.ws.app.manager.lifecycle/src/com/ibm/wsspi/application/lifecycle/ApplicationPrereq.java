/* =============================================================================
 * Copyright (c) 2020,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package com.ibm.wsspi.application.lifecycle;

/**
 * A marker interface for service components indicating
 * they must be present before applications are started.
 * <p>
 * Liberty's processing of configuration and metatype will discover
 * these declarations and wait for the configured services before
 * allowing applications to start.
 * </p>
 *
 * Every implementor must observe the following conventions:
 * <ol>
 *   <li>Edit the {@code bnd.bnd} for the bundle to instruct bnd appropriately:
 *     <ul>
 *       <li>Process the class for declarative services annotations:
 *         <pre>
 * -dsannotations: com.acme.Widget
 *         </pre>
 *       </li>
 *       <li>Add the metatype and default instances to the bundle:
 *         <pre>
 * Include-Resource: OSGI-INF=resources/OSGI-INF
 *         </pre>
 *       </li>
 *       <li>Declare the default instances (if required &mdash; see below):
 *         <pre>
 * IBM-Default-Config: OSGI-INF/wlp/defaultInstances.xml
 *         </pre>
 *       </li>
 *       <li>Add the app manager lifecycle bundle to the build path:
 *         <pre>
 * -buildpath: com.ibm.ws.app.manager.lifecycle;version=latest
 *         </pre>
 *       </li>
 *     </ul>
 *   </li>
 *   <li>The implementation must declare itself as a declarative services (DS) component.
 *     <ul>
 *       <li>The component declaration must provide {@link ApplicationPrereq} as a service.<br>
 *         <em>
 *         If this is not done, the configured component instance will never be discovered, and applications will not start up.
 *         </em>
 *       </li>
 *       <li>The component declaration must require configuration.</li>
 *     </ul>
 *     e.g.:
 *     <pre>
 * package com.acme;
 * {@code @Component}(
 *         service = ApplicationPrereq.class,
 *         configurationPolicy = REQUIRE,
 *         configurationPid = "com.acme.Widget",
 *         property = "service.vendor=Acme")
 * public class Widget implements ApplicationPrereq {&hellip;}
 *     </pre>
 *     <em>
 *       Note: Components can be declared in the Java source or in the {@code bnd.bnd} file. <br>
 *       Verify the component xml in the generated bundle,
 *       e.g. {@code OSGI-INF/com.acme.Widget.xml}
 *     </em>
 *   </li>
 *   <li>
 *     The implementing bundle must provide an {@code OCD} and a {@code Designate}
 *     for the configuration in an XML file in {@code resources/OSGI-INF/metatype/},
 *     usually {@code metatype.xml}.
 *     <ul>
 *       <li>the OCD must declare its objectClass to be {@link ApplicationPrereq}<br>
 *         e.g.
 *         <pre>
 * {@code <OCD id="com.acme.Widget"}
 *      ibm:alias="widget"
 *      name="%widget"
 *      description="%widget.desc"
 *      ibm:objectClass="com.ibm.wsspi.application.lifecycle.ApplicationPrereq" {@code >}
 * &vellip;
 * {@code </OCD>}
 *         </pre>
 *         <em>
 *         Declaring the objectClass correctly allows every configuration of this OCD to be discovered.
 *         Application containers will start only after the configured component instances become available.
 *         </em>
 *       </li>
 *       <li>
 *         <strong>Either</strong> the Designate must use a factoryPid instead of a pid,
 *         <pre>
 * {@code <Designate factoryPid="com.acme.Widget">}
 *   {@code <Object ocdref="com.acme.Widget" />}
 * {@code </Designate>}
 *         </pre>
 *       </li>
 *       <li>
 *         <strong>Or</strong> the OCD must contain a required AD without a default value:
 *         <pre>
 * {@code <OCD} &hellip; {@code >}
 *   {@code <AD id="id" name="%widget.id" required="true" type="String"/>}
 * {@code </OCD>}
 *         </pre>
 *         <em>
 *         If the delegate uses a pid and the OCD has no required non-default AD,
 *         Liberty's configuration processing may not be invoked for this OCD,
 *         and the counting of application prereqs may fail.
 *         <strong>This can be very difficult to debug!</strong>
 *         </em>
 *       </li>
 *     </ul>
 *   </li>
 *   <li>The configuration may be user-specified in the server.xml or provided in the bundle's default instances:
 *     <pre>
 * {@code <server>}
 *   {@code <widget />}
 * {@code </server>}
 *     </pre>
 *   </li>
 * </ol>
 */
public interface ApplicationPrereq {
}
