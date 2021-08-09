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
 * A field of JMFDynamicType will contain a JMFPart at runtime.  This includes JMFParts
 * whose JMFSchemas are truly unconstrained, and those whose schemas are constrained but a
 * dynamic encoding is used because the schema is recursive.  The latter case is
 * represented by recording an "expected type" which gives the constraint on what schema
 * must be used in the JMFPart.
 */

public interface JMFDynamicType extends JMFFieldDef {

  /** 
   * Retrieve the type that the runtime value must adhere to.  A return value of null
   * means either that the runtime type is unconstrained or that the expected type is not
   * available locally.  The ExpectedType property is not propagated as part of automatic
   * schema propagation and is not actually needed to process a message, since the actual
   * type is always recorded in the message.  The ExpectedType will be useful, however, in
   * resolving pathNames for recursive schemas, which is why it is provided.
   */
  public JMFType getExpectedType();

  /** 
   * Retrieve the JMFSchema corresponding to the expected type (will be null if expected
   * type is null or if the JMFType tree of which this JMFDynamicType is a part has not
   * itself yet been installed in a JMFSchema).  This is a computed property that may not
   * be set directly.
   */
  public JMFSchema getExpectedSchema();

  /** 
   * Set the type that the runtime value must adhere to.  A value of null means that the
   * runtime type is unconstrained.  This property must be set before the JMFType tree
   * containing the JMFDynamicType is installed in a JMFSchema in order for the
   * ExpectedSchema property to be correctly computed.
   */
  public void setExpectedType(JMFType expect);
}
