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

package com.ibm.ws.sib.processor.exceptions;

import java.util.Locale;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.processor.SIMPConstants;


public class SIMPRuntimeOperationFailedException extends SIMPException {
  /** The serial version UID, for version to version compatability */
  private static final long serialVersionUID = -5971530104648357624L;
  
  private String messageId;
  
  private Object arguments[];

  public SIMPRuntimeOperationFailedException(String msg) {
    super(msg);
  }
  
  public SIMPRuntimeOperationFailedException(Throwable t) {
    super(t);
  }
  
  public SIMPRuntimeOperationFailedException(String msg, Throwable t) {
    super(msg, t);
  }
 
  public SIMPRuntimeOperationFailedException(String msg, Throwable t, String messageId, Object arguments[]) {
    super(msg, t);
    
    this.messageId = messageId;
    this.arguments = arguments;
  }

  public SIMPRuntimeOperationFailedException(String msg, String messageId, Object arguments[]) {
    super(msg);
    this.messageId = messageId;
    this.arguments = arguments;
  }
  
   public String getReasonText(Locale locale) 
   {
    
    String nlsMsgText = TraceNLS.getFormattedMessage(SIMPConstants.RESOURCE_BUNDLE, 
                                                     messageId, 
                                                     locale, arguments, null);    
    return nlsMsgText; 
   }

}
