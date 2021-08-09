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

import com.ibm.websphere.sib.exception.SIException;

/**
 SIMPDestinationAlreadyExistsException is thrown when an attempt is made to create an
 object with the same identity (as defined by the semantics of the object type)
 as an object that already exists. For example, if you attempt to create a 
 durable subscription using the name of an existing durable subscription, 
 SIMPDestinationAlreadyExistsException is thrown.
 * @modelguid {8B4C163F-C8B3-4AED-8396-7304E8165835}
*/
public class SIMPDestinationAlreadyExistsException extends SIException {

  /** The serial version UID, for version to version compatability */
  private static final long serialVersionUID = 4431970614227288988L;
	
  /** @modelguid {EC9EE62C-F763-440C-98DB-16769DAD3DA6} */
  public SIMPDestinationAlreadyExistsException() {
    super();
  }
  
  /** @modelguid {9C0AD404-9AAB-4FDD-9D0A-336D5241FE2C} */
  public SIMPDestinationAlreadyExistsException(String msg) {
    super(msg);
  }
  	
  /** @modelguid {442B74FC-7D95-410E-ADDD-5B5CBE73DAE7} */
  public SIMPDestinationAlreadyExistsException(Throwable t) {
    super(t);
  }
  
  /** @modelguid {A1A47589-9142-41B5-8225-5C65A97F8DDE} */
  public SIMPDestinationAlreadyExistsException(String msg, Throwable t) {
    super(msg, t);
  }	
  
  private int exceptionReason = -1;
  private String[] exceptionInserts = null;

  /* (non-Javadoc)
   * @see com.ibm.websphere.sib.exception.SIException#getExceptionInserts()
   */
  public String[] getExceptionInserts()
  {
    if (exceptionInserts == null)
      return super.getExceptionInserts();
    
    return exceptionInserts;
  }

  /* (non-Javadoc)
   * @see com.ibm.websphere.sib.exception.SIException#getExceptionReason()
   */
  public int getExceptionReason()
  {
    if (exceptionReason < 0)
      return super.getExceptionReason();
    
    return exceptionReason;
  }
  
  /**
   * Set the exception inserts
   * @param exceptionInserts
   */
  public void setExceptionInserts(String[] exceptionInserts)
  {
    this.exceptionInserts = exceptionInserts;
  }
  
  /**
   * Set the exception reason code
   * @param exceptionReason
   */
  public void setExceptionReason(int exceptionReason)
  {
    this.exceptionReason = exceptionReason;
  }
	
}

