/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws390.ola.jca;

import javax.ejb.Remote;

import javax.resource.cci.Record;

import com.ibm.websphere.ola.ConnectionSpecImpl;
import com.ibm.websphere.ola.InteractionSpecImpl;

/**
 * Session Bean remote interface for the WOLA remote proxy
 */
@Remote
public interface ProxyEJBRemote
{
  /**
   * Invoke
   */
  public Record invoke(ConnectionSpecImpl cspec, 
                       InteractionSpecImpl ispec, 
                       Record rec)
    throws ProxyEJBException;
}
