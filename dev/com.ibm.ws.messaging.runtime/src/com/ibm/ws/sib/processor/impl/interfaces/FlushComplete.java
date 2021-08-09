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
package com.ibm.ws.sib.processor.impl.interfaces;


/**
 * An interface to signal completion of a flush.
 */
public interface FlushComplete {
  /**
   * The flush for the given destination has completed and a new
   * stream ID has been created.
   *
   * @param dest The DestinationHandler for which a stream was flushed.
   */
  public void flushComplete(DestinationHandler destinationHandler);
}
