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
package com.ibm.ws.sib.processor.test.utils;

import com.ibm.ws.sib.admin.JsEngineComponent;

// Object to represent a component within this ME
public class ComponentList
{
  private String _className;
  private JsEngineComponent _componentRef;

  public ComponentList(String className, JsEngineComponent c)
  {
    _className = className;
    _componentRef = c;
  }

  // Get the name of the class
  public String getClassName()
  {
    return _className;
  }

  // Get a reference to the instantiated class
  public JsEngineComponent getRef()
  {
    return _componentRef;
  }
}