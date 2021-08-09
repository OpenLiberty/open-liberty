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
package com.ibm.ws.sib.api.jms;

import javax.jms.*;

/**
 * @author matrober
 *
 * This interface extends the regular Connection interface to provide
 * programmatic access to the unit test methods required to create and
 * delete destinations. Note that these will only function when the test
 * environment is being used.
 * 
 * This class is specifically NOT tagged as ibm-spi because by definition it is not
 * intended for use by either customers or ISV's.
 */
public interface JmsConnInternals extends Connection
{
  
  /**
   * This method is called when a JMS object wishes to pass an exception to the
   * ExceptionListener registered for this connection if one exists. This method
   * handles the serialization of calls to the ExceptionListener.
   * 
   * @param e The JMSException to be reported
   */
  public void reportException(JMSException e);

  /**
   * This method returns the name of the ME to which this Connection has been made.
   * Note that because of the decision making powers of TRM and WLM it may not always
   * be a trivial task to guess which ME a connection will be made to beforehand. 
   *    
   */
  public String getConnectedMEName();
}
