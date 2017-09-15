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
 * A representation for all variant-like types in a Jetstream schema.  These may
 * correspond (in Ecore) to EClass nodes that are known to represent choices or to
 * EStructuralFeatures for which both isMany() and isRequired() are false ("optional
 * features").  In XSD, these may correspond to XSDModelGroup with compositor==CHOICE or
 * to an XSDParticle with minOccurs=0,maxOccurs=1.  When a JMFVariant is used to indicate
 * optional data its first case is the empty tuple and the second case is the type of the
 * optional data.  The optional data design pattern is indicated by the predicate
 * isOptionalData() being true.
 *
 * <p>A JMFVariantType is a JMFFieldDef and has an accessor assigned.  This accessor is
 * used only to set and interrogate the variant's case and is not needed to access the
 * fields governed by the variant.  These have their own accessors.  A variant has a
 * FeatureName that is part fo the pathName used to resolve its own accessor.  Currently,
 * this FeatureName is also used to resolve the accessors of its children, but, that leads
 * to some problems in mapping pathNames from other domains such as XSD and Ecore, since
 * some JMFVariantTypes correspond to constructs that are only implicit in those domains.
 * So, that aspect is subject to further review.
 */

public interface JMFVariantType extends JMFFieldDef {

  /**
   * Returns true if the variant represents the "optional data" design pattern
   * (corresponding to an XSDParticle with minOccurs=0,maxOccurs=1 or to an Ecore
   * EStructuralFeature with both isMany() and isRequired() false).  Concretely, it just
   * means that the variant has two cases, the first of which is the empty tuple.
   */
  public boolean isOptionalData();

  /**
   * Returns the number of cases in the variant.
   */
  public int getCaseCount();

  /**
   * Returns the case of this variant at a given position.  The initial case is assigned
   * position 0
   */
  public JMFType getCase(int position);

  /**
   * Add a case to the variant.  Note that every variant must have at least one case by
   * the time the JMFType tree containing the variant is installed in a JMFSchema
   */
  public void addCase(JMFType newCase);

  /**
   * Get the schema of this variant's variant box or null if this variant does not
   * introduce a variant box.  A variant introduces a variant box iff it is enclosed
   * (not necessarily directly) in a list without being enclosed in a variant that is
   * enclosed in the same list.
   *
   * @return the requested JMFSchema or null
   */
  public JMFSchema getBoxed();

  /**
   * Retrieve the box accessor associated with this field relative to some enclosing
   * schema, which may be its public schema or the schema of an enclosing variant box.
   * A box accessor is the accessor for the list of JMFNativeParts that represents a
   * list of variants.  It is not the variant's usual accessor, which is used for
   * interrogating its case.  Not all variants have box accessors (only those that are
   * enclosed in a list without being enclosed in a less-elder variant).
   *
   * @param schema the enclosing schema for which a box accessor is desired
   * @return the desired accessor or -1 if the schema argument is not a valid enclosing
   *   schema or this variant has no box accessor
   */
  public int getBoxAccessor(JMFSchema schema);

  /**
   * Retrieve the box accessor assuming the public schema (see the more general form
   * of this method)
   *
   * @return the desired accessor or -1
   */
  public int getBoxAccessor();
}
