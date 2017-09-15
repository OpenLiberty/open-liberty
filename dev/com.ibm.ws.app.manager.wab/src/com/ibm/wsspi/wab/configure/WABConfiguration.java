/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.wab.configure;

/**
 * A {@code WABConfiguration} is used to configure a Web Application Bundle (WAB).
 * In order to configure a WAB a {@code WABConfiguration} is registered as an OSGi
 * service. The {@code WABConfiguration} is a marker interface that has no methods. Configuration
 * for a WAB is specified by the {@code WABConfiguration} OSGi service registration using the service
 * properties {@value #CONTEXT_NAME} and {@value #CONTEXT_PATH}.
 * <p>
 * A WAB specifies the context path using the OSGi bundle manifest header Web-ContextPath. To specify
 * that a WAB has a configurable context path the Web-ContextPath header value must begin with
 * the {@code @} character and the remaining content after the initial {@code @} character
 * are used as the {@value #CONTEXT_NAME}. For example:
 * <p>
 * <pre>{@code Web-ContextPath: @myWABContextPath}</pre>
 * 
 * A {@code WABConfiguration} service can then be registered using the {@value #CONTEXT_NAME} value
 * of {@code myWABContextPath} to configure the context path. For example, the following declarative
 * service component can be used:
 * <p>
 * <pre>@Component(
 * configurationPid = "my.wab.configuration",
 * configurationPolicy = ConfigurationPolicy.REQUIRE)
 * public class MyWABConfiguration implements WABConfiguration {
 * // Only used to set "contextPath" and "contextName" service
 * // properties from configuration admin using the pid
 * // my.wab.configuration
 * }</pre>
 * 
 * The meta-type XML to specify the configuration of this component is the following:
 * <p>
 * <pre>{@code <metatype:MetaData xmlns:metatype="http://www.osgi.org/xmlns/metatype/v1.1.0" 
 *                   xmlns:ibm="http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0"
 *                   localization="OSGI-INF/l10n/metatype">
 * 
 *    <OCD description="My WAB Configuration" name="My WAB Configuration" 
 *         id="my.wab.configuration" ibm:alias="myWAB">
 * 
 *        <AD name="Context Path" description="The Context Path"
 *            id="contextPath" required="true" type="String" default="/default/path" />
 *        <AD name="internal" description="internal"
 *            id="contextName" ibm:final="true" type="String" default="myWABContextPath" />
 *    </OCD>
 *    
 *    <Designate pid="my.wab.configuration">
 *        <Object ocdref="my.wab.configuration"/>
 *    </Designate>
 * </metatype:MetaData>}</pre>
 * 
 * This metatype specifies the default values for both {@value #CONTEXT_NAME} and {@value #CONTEXT_PATH}.
 * If the user specifies no additional configuration then the default values will be used to
 * configure the WAB context path. Notice that the {@code <AD>} element with the id {@code contextName} has {@code internal} as the name and has {@code ibm:final}. This allows the
 * default to be specified for the service
 * component but does not allow a user to override the default in the server.xml configuration.
 * The WAB context path can then be configured using the following server.xml configuration element:
 * <p>
 * <pre>{@code <usr_myWAB contextPath="/myWab/path"/>}</pre>
 * 
 * Notice that the prefix {@code usr_} is specified for the {@code <usr_myWAB>} element. The {@code usr_} is necessary if the bundle with the {@code WABConfiguration} component is
 * installed as a
 * usr feature. If the bundle is installed with a product extension then the prefix would be the product name
 * followed by the {@code _} character.
 */
public interface WABConfiguration {
    /**
     * If a WAB has a Web-ContextPath value that begins with @ then the remaining content after the initial @ character are used as the context name.
     * The WAB installer looks for {@link WABConfiguration} services that have a matching context name property.
     */
    public static final String CONTEXT_NAME = "contextName";
    /**
     * The context path to use for WABs that have the matching context name.
     */
    public static final String CONTEXT_PATH = "contextPath";

}
