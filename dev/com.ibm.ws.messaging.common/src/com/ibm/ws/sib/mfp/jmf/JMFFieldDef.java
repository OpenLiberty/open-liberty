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

package com.ibm.ws.sib.mfp.jmf;

/**
 * The JMFFieldDef interface is a superinterface for those nodes that have accessors
 * assigned to them, namely, JMFPrimitiveType, JMFEnumType, JMFDynamicType, and
 * JMFVariantType.
 */

public interface JMFFieldDef extends JMFType {

  /**
   * Retrieve the accessor associated with this field relative to its public schema
   * (ignoring variant boxes)
   */
  public int getAccessor();

  /**
   * Retrieve the accessor associated with this field relative to some enclosing schema,
   * which may be its public schema or the schema of an enclosing variant box.
   *
   * @param schema the enclosing schema for which an accessor is desired
   * @return the desired accessor or -1 if the schema argument is not a valid enclosing
   *   schema
   */
  public int getAccessor(JMFSchema schema);

}
