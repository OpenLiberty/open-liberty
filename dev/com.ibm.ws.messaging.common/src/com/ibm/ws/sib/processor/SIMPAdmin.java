/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.processor;

/**
 * @author prmf
 */
public interface SIMPAdmin
{
  /**
   * Gets the Administrator for the message processor.
   * <p>The Administrator provides the interface for administration
   * operations on the message processor.</p>
   * @return administrator
   */
  public Administrator getAdministrator();
}
