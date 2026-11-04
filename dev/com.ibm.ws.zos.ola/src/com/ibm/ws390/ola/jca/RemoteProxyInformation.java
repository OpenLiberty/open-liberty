/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws390.ola.jca;

public class RemoteProxyInformation
{
  /**
   * Remote hostname
   */
  private String _hostname = null;

  /**
   * Remote port to make JNDI connection
   */
  private int _port = 0;

  /**
   * Remote JNDI home name for the proxy EJB.
   */
  private String _JNDI = null;

  /**
   * Remote realm
   */
  private String _realm = null;

  /**
   * Username to use with JNDI connection
   */
  private String _username = null;

  /**
   * Password to use with JNDI connection
   */
  private String _password = null;

  /**
   * Constructor
   */
  public RemoteProxyInformation(String hostname,
                                int port,
                                String JNDI,
                                String realm,
                                String username,
                                String password)
  {
    _hostname = hostname;
    _port = port;
    _JNDI = JNDI;
    _realm = realm;
    _username = username;
    _password = password;
  }

  /**
   * Gets the host name
   */
  public String getHostname() { return _hostname; }

  /**
   * Gets the port
   */
  public int getPort() { return _port; }

  /**
   * Gets the JNDI name
   */
  public String getJNDIName() { return _JNDI; }

  /**
   * Gets the realm
   */
  public String getRealm() { return _realm; }

  /**
   * Gets the username to use on the JNDI lookup
   */
  public String getUsername() { return _username; }
  
  /**
   * Gets the password to use on the JNDI lookup.
   */
  public String getPassword() { return _password; }
}
