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
 * The JMFType interface is the base interface for all nodes in a Jetstream Schema Type
 * tree.  The JMFType tree provides a pure type-structure view of a schema.
 */

public interface JMFType {

  /**
   * Return the FeatureName of this JMFType.  If this JMFType is the root of a JMFSchema,
   * its FeatureName becomes the name of the JMFSchema.  If this JMFType has an ancestor
   * other than a JMFRepeatedType, the FeatureName of this JMFType becomes part of the
   * pathName used to resolve the accessor of this JMFType or its children at runtime.
   *
   * <p>If the FeatureName of a JMFType is null, and a pathName must mention this JMFType
   * at runtime, the pathName must use the SiblingPosition of this JMFType (or the oldest
   * of its chain of JMFRepeatedType ancestors) in brackets.
   *
   * <p>The FeatureName is not propagated with a JMFSchema during automatic JMF schema
   * propagation and will be null in such a propagated schema.  Thus, convenient symbolic
   * resolution of pathNames to accessors requires that the JMFSchema be generated from a
   * stored form that contains name information in the process where resolution takes
   * place.
   */
  public String getFeatureName();

  /**
   * Set the FeatureName of this JMFType.  The FeatureName must not contain the
   * characters /[] which are significant in pathNames.  If the FeatureName was parsed
   * from a JSParser source file, it will, in fact, obey the even more restricted syntax
   * that XML uses for NCNames.
   */
  public void setFeatureName(String name);

  /**
   * Are values of this JMFType referenceable via values of JMFIdRefType?
   */
  public boolean isReferenceable();

  /**
   * Set the referenceable property, indicating that values of this JMFType are
   * referenceable by values of JMFIdRefType
   */
  public void setReferenceable(boolean referenceable);

  /**
   * Retrieve the parent node in the JMFType tree or null if this is the root node.  Note
   * that a JMFDynamic node is <em>not</em> the parent of its <b>expectedType</b> (that is
   * considered to be the root of a new tree).  The Parent property is computed by JMF and
   * cannot be set explicitly.
   */
  public JMFType getParent();

  /**
   * Retrieve the (zero-origin) sibling position that this JMFType occupies in its parent
   * or -1 if this is the root.  The SiblingPosition property is computed by JMF and
   * cannot be set explicitly.
   */
  public int getSiblingPosition();

  /**
   * Get the "Association" property.  This property can be used for any reasonable
   * purpose by tools that translate to JMFTypes from other schema notations.  It is
   * intended to capture extra information not relevant to JMF but useful in the domains
   * covered by those other notations, e.g., to support "round-trip" translation, etc.
   * The EcoreConverter tool will use this to point to corresponding EModelElements in the
   * Ecore representation of the same schema.
   */
  public Object getAssociation();

  /**
   * Set the "Association" property.  This property can be used for any reasonable
   * purpose by tools that translate to JMFTypes from other schema notations.  It is
   * intended to capture extra information not relevant to JMF but useful in the domains
   * covered by those other notations, e.g., to support "round-trip" translation, etc.
   * The EcoreConverter tool will use this to point to corresponding EModelElements in the
   * Ecore representation of the same schema.
   */
  public void setAssociation(Object assoc);

  /**
   * Update the "Associations" in this type with the matching one from the new type.
   * This includes updating any sub-types referenced by this type.  It will throw an
   * IllegalStateException of this type and the new type are not of identical structure.
   */
  public void updateAssociations(JMFType type);
}
