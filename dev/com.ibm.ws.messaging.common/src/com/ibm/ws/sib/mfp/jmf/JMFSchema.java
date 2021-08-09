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
 * A message schema organized specifically for use by JMF.  The semantics of the schema
 * are captured by a JMFType tree (getJMFType()) but a JMFSchema also provides several
 * pragmatic transformations of that tree for internal use.
 */

public interface JMFSchema {

  /**
   * Get the number of accessors defined for this JMFSchema
   */
  public int getAccessorCount();

  /**
   * Get the JMFType tree view of the schema
   */
  public JMFType getJMFType();

  /**
   * Get this JMFSchema in byte array form
   */
  public byte[] toByteArray();

  /**
   * Get the JMFSchema ID
   */
  public long getID();
  
  /**
   * Get the JMFSchema ID wrapped in a Long object
   */
  public Long getLongID();

  /**
   * Get the JMFSchema's name
   */
  public String getName();

  /**
   * Resolve a symbolic name to an accessor
   *
   * @param name the symbolic name to resolve.  We use a simple, vaguely XPath-like,
   * syntax:
   *
   * <xmp>
   * Name ::= [ Segment ] ( '/' Segment )*
   * Segment ::= SimpleName | '[' NonNegInt ']'
   * SimpleName ::= ... anything acceptable as XML NCName ...
   * </xmp>
   *
   * <p>The starting context for resolution is the top JMFType of the schema.  So, the
   * empty name (for which <b>null</b> is a permitted alias) refers to that node.  Each
   * segment steps down to the child with that name or the child with that sibling
   * position.  Any JMFRepeatedType nodes encountered in this process are skipped over
   * silently, whether named or not, so names assigned to JMFRepeatedTypes (as opposed to
   * their itemTypes) are "documentation only" and never part of any pathname.
   *
   * @return the accessor of the referred to node or -1 if the referred to node does not
   * exist or is not a JMFFieldDef
   */
  public int getAccessor(String name);

  /**
   * Resolve a case name to a case index
   *
   * @param accessor the accessor of a JMFVariantType in the schema in whose scope the
   * case name is to be resolved
   * @param name the case name to be resolved
   * @return the case index associated with the name or -1 if either the accessor does not
   * refer to a JMFVariantType in this schema or the name doesn't name one of its cases
   */
  public int getCaseIndex(int accessor, String name);

  /**
   * Return the most informative possible path name for an accessor
   *
   * @param accessor the accessor for which path name information is desired
   * @return the desired path name or null if the accessor is invalid (note that null is a
   * valid accessor for the root type if the root type is a field; however, "" is an
   * equivalent accessor and that is what is returned for that case, to distinguish it
   * from the invalid case).  Note that some accessors that are in the range implied by
   * the getAccessorCount() method are not designed for external use and are hence marked
   * invalid by this method.
   */
  public String getPathName(int accessor);

  /**
   * Return the most informative possible path name for a JMFType that is a member of the
   * JMFType tree for this JMFSchema
   *
   * @param type the JMFType for which the path name is desired
   * @return the desired path name
   */
  public String getPathName(JMFType type);

  /**
   * Return the JMFFieldDef corresponding to a given accessor
   *
   * @param accessor the accessor for which the corresponding JMFFieldDef is desired
   * @return the JMFFieldDef corresponding to the accessor or null if the accessor is
   * invalid
   */
  public JMFFieldDef getFieldDef(int accessor);
}
