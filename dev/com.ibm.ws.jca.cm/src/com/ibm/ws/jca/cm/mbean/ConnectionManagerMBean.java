/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
 * <p>The object name has the form <code>WebSphere:type=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,*</code>
 * where additional attributes can be included to narrow down the connection manager instance.</p>
 *
 * Object name examples:
 * <ul>
 * <li><code>WebSphere:type=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,jndiName=jdbc/db2,*</code>
 * <br>corresponds to a connection manager instance used by a data source with a server configuration-defined JNDI name. For example,
 * <code>
 * <br>&#60;dataSource jndiName="jdbc/db2">
 * <br>&nbsp;&#60;connectionManager maxPoolSize="10"/>
 * <br>&nbsp;...
 * <br>&#60;/dataSource>
 * </code>
 * </li>
 * <li><code>WebSphere:type=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,jndiName=eis/cf2,*</code>
 * <br>corresponds to a connection manager instance used by a connection factory with a server configuration-defined JNDI name. For example,
 * <code>
 * <br>&#60;connectionFactory jndiName="eis/cf2">
 * <br>&nbsp;&#60;connectionManager maxPoolSize="10"/>
 * <br>&nbsp;...
 * <br>&#60;/connectionFactory>
 * </code>
 * </li>
 * <li><code>WebSphere:type=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,name=jmsConnectionFactory[cf1]/connectionManager[default-0],*</code>
 * <br>corresponds to a connection manager instance explicitly configured in server configuration as a nested element. For example,
 * <code>
 * <br>&#60;jmsConnectionFactory id="cf1">
 * <br>&nbsp;&#60;connectionManager maxPoolSize="10"/>
 * <br>&nbsp;...
 * <br>&#60;/jmsConnectionFactory>
 * </code>
 * </li>
 * <li><code>WebSphere:type=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,name=databaseStore[dbstore1]/dataSource[default-0]/connectionManager[default-0],*</code>
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
 * <li><code>WebSphere:type=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,name=dataSource[ds1]/connectionManager,*</code>
 * <br>corresponds to a connection manager instance implicit from configuration of dataSource in server configuration. For example,
 * <code>
 * <br>&#60;dataSource id="ds1">
 * <br>&nbsp;...
 * <br>&#60;/dataSource>
 * </code>
 * </li>
 * <li><code>WebSphere:type=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,jndiName=java.comp/env/jdbc/ds3,application=MyApp,module=myweb,component=MyTestServlet,*</code>
 * <br>corresponds to a connection manager instance implicitly created for data source java:comp/env/jdbc/ds3, which is defined in MyTestServlet in web module myweb in application
 * MyApp. It should be noted that the : character from the JNDI name is replaced by the . character because : is not valid in an object name.
 * </li>
 * <li><code>WebSphere:type=com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean,jndiName=java.app/env/jdbc/ds4,application=MyApp,*</code>
 * <br>corresponds to a connection manager instance implicitly created for data source java:app/env/jdbc/ds4, which is defined in application MyApp.
 * It should be noted that the : character from the JNDI name is replaced by the . character because : is not valid in an object name.
 * </li>
 * </ul>
 *
 */
public interface ConnectionManagerMBean {
    /**
     * Purge the contents of the connection pool associated with
     * this Connection Manager.
     *
     * @param doImmediately The priority to be used to purge the connection pool.
     *                          Priority may be <code>"immediate"</code>, <code>"abort"</code> or <code>null</code>.
     *                          Immediate sets the total connection count to 0 and purges the pool
     *                          as quickly as possible but waits for transactions to complete.
     *                          Abort purges the pool by aborting connections without waiting for transactions to complete.
     *                          The default behavior if no value is specified is to purge the pool with normal priority.
     * @throws MBeanException
     */
    public void purgePoolContents(String doImmediately) throws MBeanException;

    /**
     * Displays the contents of the connection pool associated with
     * this Connection Manager as a human readable string.
     *
     * @return A non-localized string displaying the current state of the connection pool including detailed information
     *         about each shared, unshared and free pool connection, the number of waiters, the total number of connections,
     *         and many other details which are useful for monitoring the state of the ConnectionManager and its pool, and debugging
     *         problems. Note, this information is not NLS and the format is subject to change.
     *
     */
    public String showPoolContents();

    /**
     * Returns the JNDI name of the first data source or connection factory that used the connection manager.
     * If the first data source or connection factory that used the connection manager does not have a JNDI name,
     * then the String value <code>none</code> is returned.
     *
     * @return A non-localized string displaying a JNDI name or the text <code>none</code>.
     *
     */
    public String getJndiName();

    /**
     * Displays the maximum size of the connection pool associated with
     * this Connection Manager as a human readable string.
     *
     * @return A long that is maximum size of the connection pool
     *
     */
    public long getMaxSize();

    /**
     * Displays the size of the connection pool associated with
     * this Connection Manager as a human readable string.
     *
     * @return A long that is the size of the connection pool
     *
     */
    public long getSize();

    /**
     * Displays the free space of the connection pool associated with
     * this Connection Manager as a human readable string.
     *
     * @return A long that is the free space of the connection pool
     *
     */
    public long getAvailable();
}
