/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.admin;

import java.util.Map;
import java.util.List;

/**
 * @author philip
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public interface JsEObject {

  /**
   * Get the EObject the instance of the implements class corresponds to
   * @return 
   */
  public Object getEObject();

  /**
   * Get the parent configuration object
   * @return
   */
  public JsEObject getParent();

  /**
   * Get a map of any contained child instances of JsEObject. The map is keyed by the
   * attribute name of the child configuration object. The value of each map entry is
   * the corresponding instance of the JsEObject class.
   * @return
   */
  public Map getChildren();

  /** ConfigObject delegation methods to retrieve config attributes */
  public boolean getBoolean(String name, boolean defaultValue);
  public List getBooleanList(String name);
  public int getInt(String name, int defaultValue);
  public List getIntList(String name);
  public long getLong(String name, long defaultValue);
  public List getLongList(String name);
  public float getFloat(String name, float defaultValue);
  public List getFloatList(String name);
  public String getString(String name, String defaultValue);
  public List getStringList(String name);

}

