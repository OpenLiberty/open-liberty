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
package com.ibm.ws.sib.processor.runtime;

import java.util.Locale;

public interface HealthState {

  static public final int GREEN = 2;
  static public final int AMBER = 1;
  static public final int RED = 0;
 
  public int getState();
  
  public String getHealthReason(Locale l);
  
}
