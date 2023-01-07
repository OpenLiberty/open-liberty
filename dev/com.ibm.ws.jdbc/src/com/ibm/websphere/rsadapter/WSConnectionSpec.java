/*******************************************************************************
 * Copyright (c) 2002, 2021 IBM Corporation and others.
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
package com.ibm.websphere.rsadapter;

import java.util.Map;

// This is provided as legacy function. Do not use.
/**
 * <p>Enables connections to be requested and matched based on additional attributes.</p>
 */
public interface WSConnectionSpec {
  /**
   * Get the catalog name.
   *
   * @return the catalog name.
   */
  String getCatalog();

  /**
   * Get the password.
   *
   * @return the password.
   */
  String getPassword();

  /**
   * Get the type map, which is used for the custom mapping of SQL structured types and
   * distinct types.
   *
   * @return the type map.
   */
  @SuppressWarnings("rawtypes")
  Map getTypeMap();

  /**
   * <p>Get the cursor holdability value. 0 indicates to use the JDBC driver's default.</p>
   * 
   * @return the cursor holdability.
   */
  int getHoldability();

  /**
   * Get the user name.
   * @return the user name.
   */
  String getUserName();

  /**
  * Get the read-only indicator for the database.
  * @return true if the connection is read only; otherwise false. Value will be null if the
  *         readOnly property was never set, which means the database default will be used.
  */
  Boolean isReadOnly();

  /**
   * Gets the requested schema.
   *
   * @return the schema.
   */
  String getSchema();

  /**
   * Gets the requested network timeout.
   *
   * @return the network timeout.
   */
  int getNetworkTimeout();

  /**
   * Set the catalog name. A value of null indicates to use the database default.
   *
   * @param catalog the catalog name.
   */
  void setCatalog(String catalog);

  /**
   * Set the password.
   *
   * @param pwd the password.
   */
  void setPassword(String pwd);

  /**
   * Indicate whether the connection is read only. A value of null indicates to use the
   * database default.
   *
   * @param readOnly a flag indicating whether the connection is read only.
   */
  void setReadOnly(Boolean readOnly);

  /**
   * Set the type map for this connection. The type map is used for the custom mapping of SQL
   * structured types and distinct types.  A value of null indicates to use the database
   * default.
   *
   * @param typeMap the type map.
   */
  void setTypeMap(@SuppressWarnings("rawtypes") Map map);

  /**
   * Set the user name.
   *
   * @param userName the user name.
   */
  void setUserName(String userName);

  /**
   * Sets the cursor holdability value. A value of 0 indicates to use the JDBC driver's default.
   * If the JDBC driver doesn't support setting the cursor holdability via the JDBC
   * spec-defined mechanism, you can set the holdability value to 0.
   *
   * @param holdability the cursor holdability
   */
  void setHoldability(int holdability);

  /**
   * Sets the requested schema.
   *
   * @param schema the schema.
   */
  void setSchema(String schema);

  /**
   * Sets the requested network timeout.
   *
   * @param networkTimeout the network timeout.
   */
  void setNetworkTimeout(int networkTimeout);
}
