/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.cm.mbean;

import javax.management.MBeanException;

/**
 * <p>Management interface for connection managers. One MBean instance exists per connection manager,
 * whether configured explicitly in server configuration or whether it exists implicitly due to
 * <code>@DataSourceDefinition</code> or <code>@ConnectionFactoryDefinition</code>, or due to the
 * presence of a connection factory or data source in server configuration without an explicitly configured
 * connection manager.</p>
 *
 * <p>Important: the mbean instance is not available until the connection factory or data source is first used.</p>
 *
 * <p>The object name has the form <code>WebSphere:service=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,*</code>
 * where additional attributes can be included to narrow down the connection manager instance.</p>
 *
 * Object name examples:
 * <ul>
 * <li><code>WebSphere:service=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,jndiName=jdbc/db2,*</code>
 * <br>corresponds to a connection manager instance used by a data source with a server configuration-defined JNDI name. For example,
 * <code>
 * <br>&#60;dataSource jndiName="jdbc/db2">
 * <br>&nbsp;&#60;connectionManager maxPoolSize="10"/>
 * <br>&nbsp;...
 * <br>&#60;/dataSource>
 * </code>
 * </li>
 * <li><code>WebSphere:service=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,jndiName=eis/cf2,*</code>
 * <br>corresponds to a connection manager instance used by a connection factory with a server configuration-defined JNDI name. For example,
 * <code>
 * <br>&#60;connectionFactory jndiName="eis/cf2">
 * <br>&nbsp;&#60;connectionManager maxPoolSize="10"/>
 * <br>&nbsp;...
 * <br>&#60;/connectionFactory>
 * </code>
 * </li>
 * <li><code>WebSphere:service=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,name=jmsConnectionFactory[cf1]/connectionManager[default-0],*</code>
 * <br>corresponds to a connection manager instance explicitly configured in server configuration as a nested element. For example,
 * <code>
 * <br>&#60;jmsConnectionFactory id="cf1">
 * <br>&nbsp;&#60;connectionManager maxPoolSize="10"/>
 * <br>&nbsp;...
 * <br>&#60;/jmsConnectionFactory>
 * </code>
 * </li>
 * <li><code>WebSphere:service=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,name=databaseStore[dbstore1]/dataSource[default-0]/connectionManager[default-0],*</code>
 * <br>corresponds to a connection manager instance explicitly configured in server configuration as a nested element. For example,
 * <code>
 * <br>&#60;databaseStore id="dbstore1">
 * <br>&nbsp;&#60;dataSource>
 * <br>&nbsp;&nbsp;&#60;connectionManager maxPoolSize="10"/>
 * <br>&nbsp;&nbsp;...
 * <br>&nbsp;&#60;/dataSource>
 * <br>&#60;/databaseStore>
 * </code>
 * </li>
 * <li><code>WebSphere:service=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,name=dataSource[ds1]/connectionManager,*</code>
 * <br>corresponds to a connection manager instance implicit from configuration of dataSource in server configuration. For example,
 * <code>
 * <br>&#60;dataSource id="ds1">
 * <br>&nbsp;...
 * <br>&#60;/dataSource>
 * </code>
 * </li>
 * <li><code>WebSphere:service=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,jndiName=java.comp/env/jdbc/ds3,application=MyApp,module=myweb,component=MyTestServlet,*</code>
 * <br>corresponds to a connection manager instance implicitly created for data source java:comp/env/jdbc/ds3, which is defined in MyTestServlet in web module myweb in application
 * MyApp. It should be noted that the : character from the JNDI name is replaced by the . character because : is not valid in an object name.
 * </li>
 * <li><code>WebSphere:service=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,jndiName=java.app/env/jdbc/ds4,application=MyApp,*</code>
 * <br>corresponds to a connection manager instance implicitly created for data source java:app/env/jdbc/ds4, which is defined in application MyApp.
 * It should be noted that the : character from the JNDI name is replaced by the . character because : is not valid in an object name.
 * </li>
 * </ul>
 */
public interface ConnectionManagerMBean {
    /**
     * Purge the contents of the connection pool associated with
     * this Connection Manager.
     * 
     * @param doImmediately 'immediate' to purge the pool contents immediately,
     *            otherwise 'normal'
     * @throws MBeanException
     */
    public void purgePoolContents(String doImmediately) throws MBeanException;

    /**
     * Displays the contents of the Connection Manager as a human readable string.
     */
    public String showPoolContents();
}
