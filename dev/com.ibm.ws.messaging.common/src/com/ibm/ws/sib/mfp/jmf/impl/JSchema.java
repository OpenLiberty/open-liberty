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

package com.ibm.ws.sib.mfp.jmf.impl;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.jmf.JMFRegistry;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.jmf.JMFType;
import com.ibm.ws.sib.mfp.jmf.JMFFieldDef;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;
import com.ibm.ws.sib.mfp.util.HashedArray;
import com.ibm.ws.sib.mfp.util.ArrayUtil;
import com.ibm.ws.sib.utils.CryptoHash;

import com.ibm.websphere.ras.TraceComponent;

/**
 * Object that represents a Jetstream Schema organized specifically for use by JMF.  The
 * semantics of the schema are captured by a JSType tree (getJSTypeTree()) but a JSchema
 * also provides several pragmatic transformations of that tree for internal use.
 */

public class JSchema implements JMFSchema, HashedArray.Element {
  private static TraceComponent tc = JmfTr.register(JSchema.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

   /**
   * The current default version JSchema 'language'
   */
  public final static int VERSION = 1;

  // The version of this JSchema
  private int version = VERSION;

  // The byte array form of this JSchema's contents
  private byte[] serialForm;

  // This JSchema's name, guaranteed not to be null
  private String name;

  // The SchemaID for this JSchema
  private long schemaID;
  private Long schemaLongID;

  // The full hierarchical JSType view of the schema
  private JSType jsTypeTree;

  // The ordinary (cacheable) fields (only) in the JSType view of the schema in accessor
  // index order.  These are all non-variant fields that are not in variant boxes, and the
  // variant boxes themselves (which are represented as JSMessageImpls in the cache
  // and as JSVariants in the fields array).
  private JSField[] fields;

  // The unboxed variants (only) in the JSType view of the schema in accessor index order.
  // The accessor indices used via the JMFMessageData interface are biased by
  // fields.length.  That is, given an accessor index V that represents an unboxed
  // variant, the JSVariant element is looked up as variants[V-fields.length].
  private JSVariant[] variants;

  // The boxed fields (only) in the JSType view of the schema.  Each box is represented by
  // a pair of integers.  The first is an index into the cache where the JSMessageImpl for
  // the box can be found at runtime.  The second integer is the accessor to be passed to
  // that JSMessageImpl to complete the access.
  private int[][] boxed;

  // The interpreter cache for this JSchema.  One entry for each JMF encoding version
  // defined for the system.  A JSchemaInterpreter implementation can store whatever
  // metadata it likes here.
  private Object[] interpCache;

  // The CompatibilityMaps that specify this JSchema as the encoding schema, keyed by
  // access schema.
  private static final int COMP_MAP_BUCKETS = 23; // expect small number of maps
  private HashedArray compMaps;

  /**
   * Construct a JSchema from a JSType tree
   */
  public JSchema(JSType root) {
    jsTypeTree = root;
    initialize(new HashMap());
  }

  /**
   * Construct a JSchema from its byte array encoding
   */
  public JSchema(byte[] frame) {
    this(frame, 0, frame.length);
  }

  /**
   * Construct a JSchema from its byte array encoding given the byte frame, offset and
   * length.
   *
   * <p>Provision is made for receiving and repropagating CompatibilityMaps along with a
   * JMFSchema, even though nothing in this prototype actually <em>initiates</em> the
   * sending of a CompatibilityMap so, if this prototype were used in Jetstream 1, no
   * CompatibilityMap would ever actually be propagated in Jetstream 1.  This is not an
   * omission, but a deliberate feature.  The ability to receive and repropagate
   * CompatibilityMaps is designed to achieve a more complete compatibility with
   * <em>future</em> releases.  In general, the sending of compatibility maps is
   * unnecessary as long as there are no other releases to be compatible with, and the
   * sending of a compatibility map to a newer release is never necessary, only to an
   * older release.
   */
  public JSchema(byte[] frame, int offset, int length) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(tc, "<init>", new Object[] { frame, Integer.valueOf(offset), Integer.valueOf(length)} );
    serialForm = new byte[length];
    System.arraycopy(frame, offset, serialForm, 0, length);
    // Decode the name from the serialForm
    int namelen = ArrayUtil.readShort(frame, offset);
    try {
      name = new String(frame, offset + 2, namelen, "UTF8");
    } catch (UnsupportedEncodingException e) {
      FFDCFilter.processException(e, "<init>", "159");
      IllegalArgumentException ex = new IllegalArgumentException();
      ex.initCause(e);
      throw ex;
    }
    // Decode the schema structure body
    int[] limits = new int[] { offset + 2 + namelen, offset + length };
    version = JSType.getCount(frame, limits);
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(tc, "Schema name & version: " + name + " " + version);
    jsTypeTree = JSType.createJSType(frame, limits);
    // The values of limits[0] now points to the propagated compatibility maps, if any.
    // So, load them and store them for local use.  They are part of the transfer encoding
    // and so will get repropagated when this schema is propagated.
    while (limits[0] < limits[1]) {
      int mapLen = JSType.getCount(frame, limits);
      CompatibilityMap aMap = new CompatibilityMap(frame, limits[0], mapLen);
      limits[0] += mapLen;
      if (compMaps == null)
        compMaps = new HashedArray(COMP_MAP_BUCKETS, 2);
      compMaps.set(aMap);
    }
    // Now finish initializing the JMFSchema
    initialize(new HashMap());
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(tc, "<init>");
  }

  // Construct a JSchema from a JSType tree guarding against recursion.  Also used
  // internally, for example, by the JSDynamic.getExpectedSchema and JSVariant.box
  // methods.
  JSchema(JSType root, Map context) {
    jsTypeTree = root;
    initialize(context);
  }

  /**
   * Get a JSField node according to its accessor index.  Note that this method can
   * return a variant box if it is presented with its accessor: this capability is used
   * internally but layers above the JMFMessageData interface always present accessor
   * indices corresponding to visible elements of the schema.
   */
  public JMFFieldDef getFieldDef(int accessor) {
    if (accessor < 0)
      return null;
    if (accessor < fields.length)
      return fields[accessor];
    accessor -= fields.length;
    if (accessor < variants.length)
      return variants[accessor];
    accessor -= variants.length;
    if (accessor >= boxed.length)
      return null;
    return ((JSVariant)fields[boxed[accessor][0]]).getBoxed().getFieldDef(boxed[accessor][1]);
  }

  /**
   * Get the number of accessors defined for this JSchema
   */
  public int getAccessorCount() {
    // This number includes accessors to boxes, which are really only needed internally,
    // but that's ok: if we left them out, the result would not accurately reflect the
    // number of accessors that are really defined at this level.
    return fields.length + variants.length + boxed.length;
  }

  /**
   * Get the cacheable fields array for low-level fast access at runtime
   */
  public JSField[] getFields() {
    return fields;
  }

  /**
   * Get the unboxed variants array for low-level fast access at runtime
   */
  public JSVariant[] getVariants() {
    return variants;
  }

  /**
   * Get the box fields definitions array for low-level fast access at runtime
   */
  public int[][] getBoxed() {
    return boxed;
  }

  /**
   * Get the JMFType tree view of the schema
   */
  public JMFType getJMFType() {
    return jsTypeTree;
  }

  /**
   * Get this JSchema in byte array form
   */
  public byte[] toByteArray() {
    return serialForm;
  }

  /**
   * Get the JSchema ID
   */
  public long getID() {
    return schemaID;
  }

  /**
   * Get the JSchema ID as a Java Long
   */
  public Long getLongID() {
    return schemaLongID;
  }

  /**
   * Satisfy the HashedArray.Element interface so that JSchemas can be efficiently stored
   * and retrieved by their schemaIDs.  The getIndex() method is identical to getID() but
   * getID() is retained as well since its name is more descriptive.
   */
  public long getIndex() {
    return schemaID;
  }

  /**
   * Get the name
   */
  public String getName() {
    return name;
  }

  /**
   * Set an entry in the interpreter Cache (indexed by JMF encoding version, of which the
   * first is expected to be 1).
   *
   * @param version the JMF encoding version
   * @param toCache the Object that the SchemaInterpreter wishes to cache
   */
  public void setInterpreterCache(int version, Object toCache) {
    if (interpCache == null)
      // JMF_ENCODING_VERSION is always the latest version we support
      interpCache = new Object[JMFRegistry.JMF_ENCODING_VERSION];
    interpCache[version - 1] = toCache;
  }

  /**
   * Get an entry from the interpreter Cache (indexed by JMF encoding version).
   *
   * @param version the JMF encoding version
   */
  public Object getInterpreterCache(int version) {
    if (interpCache != null && version <= interpCache.length)
      return interpCache[version - 1];
    else
      return null;
  }

  /**
   * hashCode
   */
  public int hashCode() {
    return (int)schemaID;
  }

  /**
   * equals
   */
  public boolean equals(Object o) {
    if (o == this)
      return true;
    if (!(o instanceof JSchema))
      return false;
    JSchema oSchema = (JSchema)o;
    if (oSchema.serialForm.length != serialForm.length)
      return false;
    for (int i = 0; i < serialForm.length; i++)
      if (oSchema.serialForm[i] != serialForm[i])
        return false;
    return true;
  }

  /**
   * Find the CompatibilityMap from a given access JSchema to this JSchema
   *
   * @param access the JSchema that is intended to be used as the access schema
   * @return the desired CompatibilityMap if the schemas are compatible.  This method
   * never returns null.
   * @exception SchemaViolationException if the the JSchemas are not compatible
   */
  public CompatibilityMap getCompatibility(JSchema access) throws JMFSchemaViolationException {
    CompatibilityMap ans = null;
    if (compMaps == null)
      compMaps = new HashedArray(COMP_MAP_BUCKETS, 2);
    else
      ans = (CompatibilityMap)compMaps.get(access.getID());
    if (ans != null)
      return ans;
    ans = new CompatibilityMap(access, this);
    compMaps.set(ans);
    return ans;
  }

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
   * <p>The starting context for resolution is the top JSType of the schema.  So, the
   * empty name (for which <b>null</b> is a permitted alias) refers to that node.  Each
   * segment steps down to the child with that name or the child with that sibling
   * position.  Any JSRepeateds encountered in this process are skipped over silently,
   * whether named or not, so names assigned to JSRepeateds (as opposed to their itemTypes)
   * are "documentation only" and never part of any pathname.
   *
   * @return the accessor of the referred to node or -1 if the referred to node does not
   * exist or is not a JSField
   */
  public int getAccessor(String name) {
    if (name == null)
      name = "";
    StringTokenizer path = new StringTokenizer(name, "/");
    JMFType pos = getEffectiveType(jsTypeTree);
    while (pos != null && path.hasMoreTokens())
      pos = findChild(getEffectiveType(pos), path.nextToken());
    if (pos instanceof JSVariant && ((JSVariant)pos).getBoxed() != null)
      return ((JSVariant)pos).getBoxed().getAccessor("");
    else if (pos instanceof JSField)
      return ((JSField)pos).getAccessor();
    else
      return -1;
  }

  // Get the effective JMFType of a JMFType (skipping over JSRepeateds)
  private JMFType getEffectiveType(JMFType start) {
    while (start instanceof JSRepeated)
      start = ((JSRepeated)start).getItemType();
    return start;
  }

  // Subroutine to find a child of a given node given a 'Segment' as defined for
  // getAccessor.  Note that start is not a JSRepeated because we assume getEffectiveType is
  // called on it.
  private JMFType findChild(JMFType start, String segment) {
    if (start instanceof JSPrimitive || start instanceof JSDynamic ||
         start instanceof JSEnum)
      return null;
    if (segment.charAt(0) == '[')
      return findChildByIndex(start, Integer.parseInt(segment.substring(1, segment.length() - 1)));
    if (start instanceof JSVariant)
      return findVariantChildByName((JSVariant)start, segment);
    else
      return findTupleChildByName((JSTuple)start, segment);
  }

  // Subroutine to find a child of a given node by index.  JMFType will be a tuple or
  // variant
  private JMFType findChildByIndex(JMFType start, int index) {
    if (index < 0)
      return null;
    if (start instanceof JSVariant) {
      JSVariant var = (JSVariant)start;
      int cases = var.getCaseCount();
      if (index >= cases)
        return null;
      else
        return getEffectiveType(var.getCase(index));
    } else {
      JSTuple tup = (JSTuple)start;
      int fields = tup.getFieldCount();
      if (index >= fields)
        return null;
      else
        return getEffectiveType(tup.getField(index));
    }
  }

  // Find the child of a variant by name
  private JMFType findVariantChildByName(JSVariant var, String name) {
    for (int i = 0; i < var.getCaseCount(); i++) {
      JMFType theCase = getEffectiveType(var.getCase(i));
      String caseName = theCase.getFeatureName();
      if (caseName != null && caseName.equals(name))
        return theCase;
    }
    return null;
  }

  // Find the child of a tuple by name
  private JMFType findTupleChildByName(JSTuple tup, String name) {
    for (int i = 0; i < tup.getFieldCount(); i++) {
      JMFType theField = getEffectiveType(tup.getField(i));
      String fieldName = theField.getFeatureName();
      if (fieldName != null && fieldName.equals(name))
        return theField;
    }
    return null;
  }

  /**
   * Resolve a case name to a case index
   *
   * @param accessor the accessor of a JSVariant in the schema in whose scope the case
   * name is to be resolved
   * @param name the case name to be resolved
   * @return the case index associated with the name or -1 if either the accessor does not
   * refer to a JSVariant in this schema or the name doesn't name one of its cases
   */
  public int getCaseIndex(int accessor, String name) {
    JMFFieldDef field = getFieldDef(accessor);
    if (!(field instanceof JSVariant))
      return -1;
    JMFType target = findVariantChildByName((JSVariant)field, name);
    if (target != null)
      return getEffectiveSiblingPosition(target);
    else
      return -1;
  }

  /**
   * Return the most informative possible path name for an accessor
   *
   * @param accessor the accessor for which path name information is desired
   * @return the desired path name or null if the accessor is invalid (note that null is a
   * valid accessor for the root type if the root type is a field; however, "" is an
   * equivalent accessor and that is what is returned for that case).  For purposes of
   * this method, an accessor that refers to a variant box is "invalid" (it is designed
   * only for internal use, and this method is designed for more public use).
   */
  public String getPathName(int accessor) {
    JMFFieldDef field = getFieldDef(accessor);
    if (field == null)
      return null;
    if (field instanceof JSVariant && ((JSVariant)field).getBoxed() != null)
      return null;
    return getPathName(field);
  }

  /**
   * Return the most informative possible path name for a JMFType
   *
   * @param type the JMFType for which the path name is desired
   * @return the desired path name
   */
  public String getPathName(JMFType type) {
    if (type instanceof JSVariant) {
      JMFType boxedBy = ((JSVariant)type).getBoxedBy();
      if (boxedBy != null)
        type = boxedBy;
    }
    JMFType parent = type.getParent();
    while (parent instanceof JSRepeated)
      parent = parent.getParent();
    if (parent == null)
      return "";
    String prePath = getPathName(parent);
    if (type instanceof JSRepeated)
      return prePath;
    String segment = type.getFeatureName();
    if (segment == null)
      segment = "[" + getEffectiveSiblingPosition(type) + "]";
    if (prePath.length() > 0)
      return prePath + "/" + segment;
    else
      return segment;
  }

  // Subroutine to calculate the sibling position of a type in its "effective parent"
  // (skipping over intervening JSRepeateds).
  private int getEffectiveSiblingPosition(JMFType child) {
    JMFType parent = child.getParent();
    while (parent instanceof JSRepeated) {
      child = parent;
      parent = child.getParent();
    }
    return child.getSiblingPosition();
  }

  // Subroutine to finish initializing by numbering the JMFType tree and filling in the
  // fields, variants, and boxed fields.  Also fills in serialForm and schema id and
  // calculates the multiChoiceCounts for each node in the tree.
  private void initialize(Map context) {
    // Guard against recursion.
    context.put(jsTypeTree, this);

    // Create temporary accumulators for the contents of the fields and variants instance
    // variables.
    List tmpFields = new ArrayList();
    List tmpVariants = new ArrayList();

    // Perform tasks assigned to the recursive 'number' subroutine (see comment there)
    number(jsTypeTree, context, false, 0, tmpFields, tmpVariants);

    // Move information from temporaries into permanent instance variables
    fields = (JSField[])tmpFields.toArray(new JSField[0]);
    variants = (JSVariant[])tmpVariants.toArray(new JSVariant[0]);

    // Compute the contents of the 'boxed' instance variable, which is deriveable from the
    // contents of those JSVariants which have found their way into 'fields.'  These are
    // all intended to be boxed variants because they were detected to be underneath
    // JSRepeated nodes during the 'number' process.
    int boxLen = 0; // accumulate total length of boxed array
    for (int i = 0; i < fields.length; i++)
      if (fields[i] instanceof JSVariant)
        // On the first pass, we box the variant and accumulate its accessor count
        boxLen += ((JSVariant)fields[i]).box(context).getAccessorCount();
    boxed = new int[boxLen][];
    int boxIndex = 0;

    // On the second pass through the boxed variants, we get their accessor count a second
    // time and use it this time to fill in the contents of the 'boxed' instance variable
    // proper.
    for (int i = 0; i < fields.length; i++)
      if (fields[i] instanceof JSVariant)
        for (int j = 0; j < ((JSVariant)fields[i]).getBoxed().getAccessorCount(); j++)
          boxed[boxIndex++] = new int[] { i, j };

    // Set the 'accessor' property of every JSField in the tree (see comment on
    // setAccessors).
    setAccessors(0, this);

    // Set the multiChoiceCount for each node in the tree.  Note this must be done
    // after boxing any variants found underneath JSRepeated nodes, since boxing changes
    // the multiChoiceCount.  setMultiChoiceCount is a recursive method that will set
    // counts starting from the leaves working back to the root.
    jsTypeTree.setMultiChoiceCount();

    // Compute the serialForm if it isn't already there due to the deserializing
    // constructor
    if (serialForm == null) {
      // Store the name in a convenient non-null form
      name = jsTypeTree.getFeatureName();
      if (name == null)
        name = "";
      try {
        byte[] utfname = name.getBytes("UTF8");
        serialForm = new byte[2 + utfname.length + 2 + jsTypeTree.encodedTypeLength()];
        ArrayUtil.writeShort(serialForm, 0, (short)utfname.length);
        System.arraycopy(utfname, 0, serialForm, 2, utfname.length);
        int limits[] = new int[] { 2 + utfname.length, serialForm.length };
        JSType.setCount(serialForm, limits, version);
        jsTypeTree.encodeType(serialForm, limits);
      } catch (UnsupportedEncodingException e) {
        FFDCFilter.processException(e, "initialize", "604");
        IllegalArgumentException ex = new IllegalArgumentException();
        ex.initCause(e);
        throw ex;
      }
    }

    // Compute the schemaID, which is a 64-bit truncated SHA-1 hash over the serialForm.
    schemaID = CryptoHash.hash(serialForm);
    schemaLongID = Long.valueOf(schemaID);
  }

  // Subroutine to "number" a JSType tree and fill in the fields, variants, and boxes
  // variables.  The context variable ensures that recursive schemas are handled
  // correctly.  Note defensive code to guard against infinite expansion of recursive
  // schemas is in the JSDynamic.getExpectedSchema() and JSVariant.box methods as well as
  // in this class, because all of the initialization in this class takes place in the
  // context of a constructor: by the time we are in a constructor we have already
  // instantiated a new JSchema and it may be too late at that time use an existing
  // instance.
  private void number(
    JSType node,
    Map context,
    boolean boxVariant,
    int arrayCount,
    List tmpFields,
    List tmpVariants) {
    // If node is a JSField and arrayCount is positive, we create a JSListCoder for the
    // field.
    if (node instanceof JSField && arrayCount > 0)
       ((JSField)node).setCoder(new JSListCoder(arrayCount - 1, (JSField)node));
    // Process the node according to its kind
    if (node instanceof JSPrimitive || node instanceof JSEnum) {
      // Primitives and enumerations always go in the 'fields' section
      tmpFields.add(node);
    }
    else if (node instanceof JSDynamic) {
      // Dynamics always go in the 'fields' section
      tmpFields.add(node);
      // The following is called for its side-effect of ensuring creation of the
      // expectedSchema if the JSDynamic has an expectedType.  The result isn't needed
      // here.
       ((JSDynamic)node).getExpectedSchema(context);
    }
    else if (node instanceof JSVariant) {
      if (boxVariant) {
        // Variant boxes go in the 'fields' section now, and will have their boxed
        // information later added to the 'boxed' section.  We do not recursively visit
        // nodes below a boxed variant (even though such nodes are undoubtedly there).
        // Rather, we will rely on a later call to the JSVariant.box method to initialize
        // the subSchema for the material inside the box.
        tmpFields.add(node);
      }
      else {
        // Unboxed variants go in the 'variants' section
        JSVariant var = (JSVariant)node;
        var.setIndex(tmpVariants.size());
        tmpVariants.add(var);
        for (int i = 0; i < var.getCaseCount(); i++)
          // The children of unboxed variants are now recursively visited.  Any additional
          // variants found below this point but not below an intervening JSRepeated are
          // considered 'unboxed.'
          number((JSType)var.getCase(i), context, false, arrayCount, tmpFields, tmpVariants);
      }
    }
    else if (node instanceof JSRepeated) {
      JSRepeated arr = (JSRepeated)node;
      // The child of a JSRepeated is now recursively visited so that its (repeated) value
      // can be interrogated.  Any JSVariant found below a JSRepeated but not below an
      // intervening JSVariant is considered 'boxed.'
      number((JSType)arr.getItemType(), context, true, arrayCount + 1, tmpFields, tmpVariants);
    }
    else {
      JSTuple tup = (JSTuple)node;
      // The JSTuple is not, itself, entered anywhere.
      for (int i = 0; i < tup.getFieldCount(); i++)
        // The children of a JSTuple are now recursively visited.  The boxed status of any
        // JSVariants below this JSTuple are determined by JSRepeated and JSVariant nodes
        // higher in the tree and not by the JSTuple.
        number((JSType)tup.getField(i), context, boxVariant, arrayCount, tmpFields, tmpVariants);
    }
  }

  // Subroutine to set the accessors throughout a JSchema, including its boxed parts if
  // any.
  // The 'bias' is added to the apparent accessors, where an 'apparent' accessor is one
  // based solely the position of a field or variant in the fields and variants arrays.
  // The contents of a box will first have accessors assigned with bias 0 (during
  // construction of the boxed subtree), but those accessors will be recomputed with the
  // correct bias when the parent schema is finished initializing.
  // The 'schema' argument is used to label all accessors generated at a particular
  // level of recursion with the schema against which those accessors are accurate.  This
  // permits retrieval of accessors relative to box schemas as well as the public schema.
  private void setAccessors(int bias, JMFSchema schema) {
    int nextBoxBias = bias + fields.length + variants.length;
    for (int i = 0; i < fields.length; i++) {
      JSField field = fields[i];
      if (field instanceof JSVariant) {
        JSchema boxed = (JSchema) ((JSVariant)field).getBoxed();
        boxed.setAccessors(nextBoxBias, schema);
        // Copy accessors from the top type of the box to the visible variant
        JSVariant boxVar = (JSVariant) boxed.getJMFType();
        field.setAccessor(boxVar.getAccessor(boxed), boxed);
        field.setAccessor(boxVar.getAccessor(schema), schema);
        ((JSVariant) field).setBoxAccessor(i + bias, schema);
        nextBoxBias += boxed.getAccessorCount();
      }
      else
        field.setAccessor(i + bias, schema);
    }
    for (int i = 0; i < variants.length; i++)
      variants[i].setAccessor(i + bias + fields.length, schema);
  }
}
