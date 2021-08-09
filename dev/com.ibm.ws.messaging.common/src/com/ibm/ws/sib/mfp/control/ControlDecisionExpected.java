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
package com.ibm.ws.sib.mfp.control;

/**
 * ControlDecisionExpected extends the general ControlMessage interface and provides
 * get/set methods for the fields specific to a Control Decision
 */
public interface ControlDecisionExpected extends ControlMessage {

  /* **************************************************************************/
  /* Get Methods                                                              */
  /* **************************************************************************/

  /**
   * Get the Tick values for this request.  Each tick represents and accepted
   * state.
   *
   * @return A long[] containing the Tick values
   */
  public long[] getTick();

  /* **************************************************************************/
  /* Set Methods                                                              */
  /* **************************************************************************/

  /**
   * Set the Tick values for this request.  Each tick represents and accepted
   * state.
   *
   * @param values A long[] containing the Tick values
   */
  public void setTick(long[] values);


}
