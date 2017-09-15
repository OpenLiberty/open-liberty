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

package com.ibm.ws.sib.processor;

import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;

public abstract class RecoveryProcessorFactory {


  //private static String CLASS_NAME = RecoveryProcessorFactory.class.getName();

  private static final TraceComponent tc = SibTr.register(RecoveryProcessorFactory.class, JsConstants.TRGRP_AS,
      JsConstants.MSG_BUNDLE);

 
  /**
   * Reference to the singleton RecoveryProcessor instance.
   */
  private static RecoveryProcessor _processor = null;

  /**
   * Returns a reference to the singleton RecoveryProcessor.
   * 
   * @return reference to the singleton RecoveryProcessor.
   */
  public static final RecoveryProcessor getInstance() {

    if (tc.isEntryEnabled()) {
      SibTr.entry(tc, "getInstance");
    }

    if (_processor == null) {
      try {
        Class c = Class.forName("com.ibm.ws.sib.processor.impl.RecoveryProcessorImpl");
        _processor = (RecoveryProcessor) c.newInstance();
      }
      catch (Exception e) {
        e.printStackTrace();
        
      }
    }

    if (tc.isEntryEnabled()) {
      SibTr.exit(tc, "getMBean", _processor);
    }

    return _processor;
  }

}

