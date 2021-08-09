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

package com.ibm.ws.sib.processor.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.ws.sib.admin.JsEObject;

/**
 * @author leonarda
 * 
 * This class implements JsEObject and is intended to hold
 * ME WCCM config for use by testcases, so that it can be
 * updated through setConfig(JsEObject).
 *
 */
public class SIMPMEConfigObject implements JsEObject
{  
  /**
   * Comment for <code>serialVersionUID</code>
   */
  private static final long serialVersionUID = 1582099166162476667L;

  private long highMessageThreshold = 0;
  private boolean highMessageThresholdSet = false;
  
  /**
   * Constructor.
   * @param arg0
   */
  public SIMPMEConfigObject()
  {
  }

  public void setHighMessageThreshold(long value) {
    highMessageThreshold = value;
    highMessageThresholdSet = true;
  }

  public long getLong(String name, long defaultValue) {
   if (name.equals("highMessageThreshold")) {
     if (highMessageThresholdSet) {
       return highMessageThreshold;
     } else {
       return defaultValue;
     }
   } else {
     return 0;
   }
  }

  public Object getEObject() { return null; } 
  public String[] getAttributeNames() { return null; }
  public String getAttribute(String name) { return "false"; }
  public Map getChildren() { return null; }
  public JsEObject getParent() { return null; }
  public String getCustomProperty(String name) { return null; }
  public boolean getBoolean(String name, boolean defaultValue) { return false; }
  public List getBooleanList(String name) { return new ArrayList(); }
  public int getInt(String name, int defaultValue) { return 0; }
  public List getIntList(String name) { return new ArrayList(); }
  public List getLongList(String name) { return new ArrayList(); }
  public float getFloat(String name, float defaultValue) { return 0; }
  public List getFloatList(String name) { return new ArrayList(); }
  public String getString(String name, String defaultValue) { return ""; }
  public List getStringList(String name) { return new ArrayList(); }

  
}

