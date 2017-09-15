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

import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.mfp.jmf.JMFEncapsulation;
import com.ibm.ws.sib.mfp.jmf.JMFList;
import com.ibm.ws.sib.mfp.jmf.JMFMessage;
import com.ibm.ws.sib.mfp.jmf.JMFMessageData;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFNativePart;
import com.ibm.ws.sib.mfp.jmf.JMFRegistry;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;
import com.ibm.ws.sib.utils.DataSlice;

/**
 * This class caters for version-to-version compatibility of messages where the
 * schema has changed between the versions. Such changes must always meet the
 * Compatability Rules defined in the JMF Documentation & the MFP HLD.
 *
 * An instance of this class is created fo any JMFMessage where the encoding
 * schema is different from, but compatible with, the access schema. It allows
 * higher level code to continue to use the current field accessors, which are
 * mapped to the corresponding accessors in the encoding schema.
 * A major change to the content of the message may cause a switch to pure delegation,
 * at which point the map is thrown away and all calls are delegated directly to the
 * contained encoding schema.
 *
 * Locking:
 *   The only instance variable which must really be protected from other threads
 *   is the map, as it is nulled out by becomeDelegator() whereas many other methods
 *   assume that if it is non-null when they check it will remain non-null while they
 *   use it.
 *   The encoding may be changed to a new reference by becomeDelegator, but another
 *   thread continuing to access the old one at that time would not be a disaster.
 */
public final class JSCompatibleMessageImpl implements JMFMessage {
  private static TraceComponent tc = JmfTr.register(JSCompatibleMessageImpl.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  // The access JSchema
  private JSchema access;

  // The non-compatibility JMFNativePart representing the encoding.  Depending on the
  // context in which this JSCompatibleMessageImpl was constructed (whether it is acting
  // as an implementation of JMFNativePart or JMFMessage) this may actually be a
  // JMFMessage. Alternatively it could be a JMFEncapsulation.
  private JMFNativePart encoding;

  // The compatibility map.  This may be set to null if this JSCompatibleMessageImpl has
  // entered the "pure delegation" phase of its existance after a mutation that caused
  // abandonment of the compatibility layer.
  private CompatibilityMap map;

  // A JSBoxManager to handle accessors in the boxed range, if any.
  private JSBoxManager boxManager;

  /**
   * Constructor
   */
  public JSCompatibleMessageImpl(JSchema access, JMFNativePart encoding) throws JMFSchemaViolationException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "<init>", new Object[]{access, encoding});
    this.access = access;
    this.encoding = encoding;
    if (encoding instanceof JSMessageData) {
      ((JSMessageData)encoding).setCompatibilityWrapper(this);
    }
    map = ((JSchema)encoding.getEncodingSchema()).getCompatibility(access);
    int[][] boxed = access.getBoxed();
    if (boxed.length > 0) {
      boxManager = new JSBoxManager(this, boxed);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "<init>");
  }

  // Copy constructor.  This constructs a new JSCompatibleMessageImpl from an existing one.
  private JSCompatibleMessageImpl(JSCompatibleMessageImpl original) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "<init>", original);
    // Copy the instance fields.
    access = original.access;
    map = original.map;
    if (original.boxManager != null) {
      boxManager = new JSBoxManager(this, access.getBoxed());
    }
    encoding = ((JMFMessage)original.encoding).copy();
    if (encoding instanceof JSMessageData) {
      ((JSMessageData)encoding).setCompatibilityWrapper(this);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "<init>");
  }

  // Implement JMFMessage.getMessageLockArtefact()          d364050
  public final Object getMessageLockArtefact() {

    // If there is a JMFMessage representing the encoding, then return its
    // message lock artefact, so that we operate properly in the message hierarchy.
    if (encoding instanceof JMFMessage) {
      return ((JMFMessage)encoding).getMessageLockArtefact();
    }
    // If not, we must be wrapping an Encapsulation so we just need to cater for
    // 2 threads accessing this instance
    else {
      return this;
    }
  }

  // Return the underlying encoding
  final JMFNativePart getEncodingMessage() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getEncodingMessage");
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getEncodingMessage", encoding);
    return encoding;
  }

  // Methods to implement JMFMessage.  Many of these simply delegate to the encoding
  // JMFMessage.
  // Locking: Methods which simply delegate to the encoding do not need locking
  // at this level. If the encoding needs any synchronization it will get the lock
  // itself.
  public short getJMFEncodingVersion() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "getJMFEncodingVersion");
    return ((JMFMessage)encoding).getJMFEncodingVersion();
  }

  public JMFSchema[] getSchemata() throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "getSchemata");
    return ((JMFMessage)encoding).getSchemata();
  }

  public int originalFrame() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "originalFrame");
    return ((JMFMessage)encoding).originalFrame();
  }

  public int getEncodedLength() throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getEncodedLength");
    int length = ((JMFMessage)encoding).getEncodedLength();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getEncodedLength", length);
    return length;
  }

  public int toByteArray(byte[] buffer, int offset, int length) throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "toByteArray");
    return ((JMFMessage)encoding).toByteArray(buffer, offset, length);
  }

  public byte[] toByteArray(int length) throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "toByteArray");
    return ((JMFMessage)encoding).toByteArray(length);
  }

  public JMFSchema getEncodingSchema() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getEncodingSchema");
    JMFSchema schema = encoding.getEncodingSchema();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getEncodingSchema");
    return schema;
  }

  public JMFNativePart newNativePart(JMFSchema schema) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) JmfTr.debug(this, tc, "newNativePart");
    return encoding.newNativePart(schema);
  }

  public int getModelID() {
    return MODEL_ID_JMF;
  }

  // GetJMFSchema does not delegate since this method explicitly asks for the access schema
  public JMFSchema getJMFSchema() {
    return access;
  }

  // Copy does not usually delegate directly to the encoding part but creates a copy
  // of the compatibility wrapper containing a copy of the encoding message.
  // Locking: Relies on map remaining not-null, so must not run coincidentally with becomeDelegator()
  public JMFMessage copy() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "copy");
    JMFMessage copy;

    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        // If there is no map, then this is already become just a delegator, so
        // the copy does not need to include the compatibility wrapper.
        copy = ((JMFMessage)encoding).copy();
      }
      else {
        // In the usual case, we maintain a compatibility layer
        copy = new JSCompatibleMessageImpl(this);
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "copy", copy);
    return copy;
  }

  // Methods to implement the JMFMessageData interface.  Note that those methods that look
  // up access unconditionally in map.indices may throw IndexOutOfBounds as a result of
  // that lookup but that is an expected exception, the same as would be thrown if the
  // index had been passed to the encoding JMFMessage.

  // The getValue method must triage calls into those that can be delegated after
  // translating the accessor (as with most of the methods in this class) and those that
  // require special handling (variants, for which we use different logic depending on
  // whether they are boxed or unboxed).
  // Locking: Relies on map remaining not-null, so must not run coincidentally with becomeDelegator()
  public Object getValue(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getValue", Integer.valueOf(accessor));

    Object ans;

    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        // pure delegation
        ans = encoding.getValue(accessor);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getValue", ans);
        return ans;
      }
      if (accessor >= map.indices.length) {
        // Either invalid or a boxed range access; in the latter case, we delegate to the
        // boxManager.
        if (accessor >= access.getAccessorCount()) {
          throw new IndexOutOfBoundsException(String.valueOf(accessor));
        }
        ans = boxManager.getBoxedlValue(accessor-map.indices.length);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getValue", ans);
        return ans;
      }
      // Not boxed.
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        // Since the accessor maps to something that can't be there, it isn't there.
        throw new JMFUninitializedAccessException("Invalid accessor: " + accessor);
      }

      // Get the value.
      ans = encoding.getValue(encAccessor);
      // If a Variant
      if (encAccessor >= map.varBias) {
        // Adjust variant case using getCases
        ans = Integer.valueOf(map.getCases[encAccessor - map.varBias][((Number)ans).intValue()]);
      }
      // Not a variant.
      else if (ans instanceof JSVaryingListImpl) {
        // check special case of variant-box-list, which needs to be wrapped.
        JSVaryingListImpl boxList = (JSVaryingListImpl) ans;
        if (boxList.element instanceof JSVariant) {
          // Wrapping required.
          JSVariant thisVar = (JSVariant) access.getFieldDef(accessor);
          ans = new JSCompatibleBoxList(thisVar, boxList);
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getValue", ans);
      return ans;

    }
  }

  // The getInt method also has some special logic for variants, but, in this case, the
  // variant can't legally be boxed so the method is simpler than getValue.
  // Locking: Relies on map remaining not-null, so must not run coincidentally with becomeDelegator()
  public int getInt(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getInt", Integer.valueOf(accessor));

    synchronized (getMessageLockArtefact()) {
      int ans;
      if (map == null) {
        // pure delegation
        ans = encoding.getInt(accessor);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getInt", Integer.valueOf(ans));
        return ans;
      }

      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        throw new JMFUninitializedAccessException("Invalid accessor: " + accessor); // see getValue
      }
      ans = encoding.getInt(encAccessor);
      if (encAccessor < map.varBias) {
        // we already have the answer
      }
      else {
        ans = map.getCases[encAccessor - map.varBias][ans];
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getInt", Integer.valueOf(ans));
      return ans;
    }
  }

  // The isPresent method returns false if there is no corresponding field in the
  // encoding, but delegates otherwise.
  // Locking: Relies on map remaining not-null, so must not run coincidentally with becomeDelegator()
  public boolean isPresent(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "isPresent", Integer.valueOf(accessor));

    boolean ans;

    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        // pure delegation
        ans = encoding.isPresent(accessor);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "isPresent", Boolean.valueOf(ans));
        return ans;
      }
      if (accessor >= map.indices.length) {
        // Either invalid or a boxed range access; in the latter case, we let the boxManager
        // substitute the accessor for the variant box in which the field is encased
        // and interrogate the presence of that instead (see JSMessageImpl.isPresent).
        if (accessor >= access.getAccessorCount()) {
          throw new IndexOutOfBoundsException(String.valueOf(accessor));
        }
        accessor = boxManager.getBoxAccessor(accessor-map.indices.length);
      }
      int enc = map.indices[accessor];
      if (enc == -1) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "isPresent", Boolean.FALSE);
        return false;
      }
      else {
        ans = encoding.isPresent(enc);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "isPresent", Boolean.valueOf(ans));
        return ans;
      }
    }
  }

  // The setValue method provides logic that is similar to getValue, but in the
  // opposite direction.
  // Locking: Relies on map remaining not-null, so must not run coincidentally with becomeDelegator()
  public void setValue(int accessor, Object val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "setValue", new Object[]{Integer.valueOf(accessor), val});

    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        // pure delegation
        encoding.setValue(accessor, val);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "setValue");
        return;
      }
      if (accessor >= map.indices.length) {
        // Either invalid or a boxed range access; in the latter case, we delegate to the
        // boxManager.
        if (accessor >= access.getAccessorCount()) {
          throw new IndexOutOfBoundsException(String.valueOf(accessor));
        }
        boxManager.setBoxedValue(accessor-map.indices.length, val);
      }
      else {
        // Not boxed.
        int encAccessor = map.indices[accessor];
        if (encAccessor == -1) {
          // setting a field that doesn't exist ... transition to delegation.
          becomeDelegator();
          encoding.setValue(accessor, val);
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "setValue");
          return;
        }
        if (encAccessor >= map.varBias) {
          // For a variant, we adjust the value before setting
          val = Integer.valueOf(map.setCases[encAccessor - map.varBias][((Number)val).intValue()]);
        }
        encoding.setValue(encAccessor, val);
      }

      // Having performed the set operation, switch to delegation iff encoding was
      // unassembled.
      if (encoding instanceof JSMessageData && ((JSMessageData)encoding).contents == null) {
        becomeDelegator();
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "setValue");
  }

  // Convert from the active state to the delegate-only state after receiving a mutation
  // that would cause unassembly or the setting of a non-existant field
  // Locking: Sets map to null so must run within the lock, to avoid NPEs in other methods
  //          Generally the caller will already have the lock, but we'll get it again just in case.
  private void becomeDelegator() throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "becomeDelegator");

    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "becomeDelegator");
        return;
      }
      JMFNativePart newEncoding = JMFRegistry.instance.newNativePart(access);
      transcribe(access, map, encoding, newEncoding);
      if (boxManager != null) {
        boxManager.reset();
      }
      // Update the parent's cache to point to hold the new encoding  - d249317
      if (((JSMessageData)newEncoding).getParent() != null) {
        ((JSMessageData)newEncoding).getParent().updateCacheEntry(encoding, newEncoding);
      }

      ((JSMessageData)newEncoding).setCompatibilityWrapper(this);
      encoding = newEncoding;
      map = null;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "becomeDelegator");
  }

  /** Transcribe a JMFNativePart to a compatible JMFNativePart
   * Locking: no locking needed as only called by becomeDelegator() which has the lock.
   * @param toSchema the JSchema for the result
   * @param map a CompatibilityMap in which toSchema is the access schema and from's
   *    schema is the encoding schema
   * @param from a JMFNativePart whose schema is the encoding schema of map
   * @param to an empty JMFNativePart whose schema is toSchema
   */
  private static void transcribe(JSchema toSchema, CompatibilityMap map, JMFNativePart from, JMFNativePart to) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(tc, "transcribe", new Object[]{toSchema, map, from, to});

    // Set the parent field in the new JMFNativePart  - d249317
    JMFMessageData parent;
    if (from instanceof JSMessageData) {
      parent = ((JSMessageData)from).getParent();
    }
    else {
      parent = ((JMFEncapsulation)from).getContainingMessageData();
    }
    if (parent != null) {
      ((JSMessageData)to).setParent(parent);
    }

    // Iterate through the unboxed variants copying all variant setting that are in 'from' to 'to.'
    // Note that this does not include all of the variants that may actually need to be set in 'to',
    // since these may include defaulting variants that have no counterpart in 'from.'  But, before
    // we can find these, we need to copy all the variants and all the fields.  We might as well copy
    // the variants first, since this will cause the field copying to go marginally faster.
    JSVariant[] vars = toSchema.getVariants();
    for (int i = 0; i < vars.length; i++) {
      JSVariant var = vars[i];
      int acc = var.getAccessor(toSchema);
      int enc = map.indices[acc];
      if (enc == -1 || !from.isPresent(enc)) {
        // don't copy what isn't there
        continue;
      }
      else {
        // Have a variant that can be copied
        to.setInt(acc, map.getCases[enc-map.varBias][from.getInt(enc)]);
      }
    }

    // Now copy the fields, with special handling for variant box lists.
    JSField[] fields = toSchema.getFields();
    for (int i = 0; i < fields.length; i++) {
      JSField field = fields[i];
      if (field instanceof JSVariant) {
        // A variant box list might need transcription.  Check via the box accessor.
        JSVariant var = (JSVariant) field;
        int acc = var.getBoxAccessor(toSchema);
        int enc = map.indices[acc];
        if (enc == -1 || !from.isPresent(enc)) {
          continue;
        }
        // Have a box list that needs copying.  Get the list and make a conforming
        // one in the target
        List fromList = (List) from.getValue(enc);
        List toList = to.createBoxList(acc, fromList);
        // Retrieve or create the two box schemas and their CompatibilityMap.  Note
        // that if the top-level schemas are compatible, the box schemas necessarily are,
        // but their compatibility maps may or may not have been previously created (in
        // some sense those maps are incorporated into the top-level map but with different
        // accessor numbers; however, a "localized" map is needed for transcription).
        JSchema accBoxSchema = (JSchema) var.getBoxed();
        JSchema encBoxSchema = (JSchema) ((JSVariant) from.getJMFSchema().getFieldDef(enc)).getBoxed();
        CompatibilityMap boxMap = encBoxSchema.getCompatibility(accBoxSchema);
        transcribeList(accBoxSchema, boxMap, fromList, toList);
      }
      else {
        // Ordinary field
        int acc = field.getAccessor(toSchema);
        int enc = map.indices[acc];
        if (enc == -1 || !from.isPresent(enc)) {
          continue;
        }
        // Have a field that needs copying
        to.setValue(acc, from.getValue(enc));
      }
    }

    // Now iterate through the variants a second time, this time setting those variants that were
    // not previously set, that have no counterpart in the 'from' schema, and that should be set
    // according to the settings of dominating variants in the 'to' message.  These will cover
    // the defaulting variants in the case where 'to' is newer than 'from.'
    for (int i = 0; i < vars.length; i++) {
      JSVariant var = vars[i];
      int acc = var.getAccessor(toSchema);
      if (  to.getInt(acc) == -1
         && map.indices[acc] == -1
         && ((JSMessageImpl) to).consistentChoice(acc - fields.length)
         ) {
        to.setInt(acc, 0);
      }
    }

    // Copying the fields and variants plus setting those variants that couldn't be set by copying
    // should be sufficient, as long as the fields that we copied include the variant box fields.
    // Copying the boxed fields is not necessary because the box manager in the new message will
    // reconstruct that information.
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(tc, "transcribe");
  }

  // Transcribe a list.  Drills through the appropriate layers of list until it finds
  // JMFNativeParts that can be transcribed.
  private static void transcribeList(
    JSchema toSchema,
    CompatibilityMap map,
    List fromList,
    List toList)
    throws
      JMFSchemaViolationException,
      JMFModelNotImplementedException,
      JMFUninitializedAccessException,
      JMFMessageCorruptionException
  {
    for (int j = 0; j < fromList.size(); j++) {
      Object from = fromList.get(j);
      Object to = toList.get(j);
      if (from instanceof JMFNativePart) {
        transcribe(toSchema, map, (JMFNativePart) from, (JMFNativePart) to);
      }
      else {
        transcribeList(toSchema, map, (List) from, (List) to);
      }
    }
  }

  // The getInt method also has some special logic for variants, but, in this case, the
  // variant can't legally be boxed so the method is simpler than setValue.
  // Locking: Relies on map remaining not-null, so must not run coincidentally with becomeDelegator()
  public void setInt(int accessor, int val)
      throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "setInt", new Object[]{Integer.valueOf(accessor), Integer.valueOf(val)});

    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        // pure delegation
        encoding.setInt(accessor, val);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "setInt");
        return;
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        // setting a field that doesn't exist ... transition to delegation.
        becomeDelegator();
        encoding.setInt(accessor, val);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "setInt");
        return;
      }
      if (encAccessor >= map.varBias) {
        val = map.setCases[encAccessor - map.varBias][val];
      }
      encoding.setInt(encAccessor, val);

      if (encoding instanceof JSMessageData && ((JSMessageData)encoding).contents == null) {
        becomeDelegator();
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "setInt");
  }

  // Other methods simply delegate while translating the accessor, but have special
  // handling for accessors that don't map (they cause transition to delegation on set
  // but cause an exception on get).
  // Locking: Rely on map remaining not-null, and the encoding matching the map,
  // so must not run coincidentally with becomeDelegator()
  public boolean getBoolean(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        return encoding.getBoolean(accessor);
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        throw new JMFUninitializedAccessException("Invalid accessor: " + accessor); // see getValue
      }
      return encoding.getBoolean(encAccessor);
    }
  }

  public byte getByte(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        return encoding.getByte(accessor);
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        throw new JMFUninitializedAccessException("Invalid accessor: " + accessor); // see getValue
      }
      return encoding.getByte(encAccessor);
    }
  }

  public short getShort(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        return encoding.getShort(accessor);
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        throw new JMFUninitializedAccessException("Invalid accessor: " + accessor); // see getValue
      }
      return encoding.getShort(encAccessor);
    }
  }

  public char getChar(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        return encoding.getChar(accessor);
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        throw new JMFUninitializedAccessException("Invalid accessor: " + accessor); // see getValue
      }
      return encoding.getChar(encAccessor);
    }
  }

  public long getLong(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        return encoding.getLong(accessor);
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        throw new JMFUninitializedAccessException("Invalid accessor: " + accessor); // see getValue
      }
      return encoding.getLong(encAccessor);
    }
  }

  public float getFloat(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        return encoding.getFloat(accessor);
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        throw new JMFUninitializedAccessException("Invalid accessor: " + accessor); // see getValue
      }
      return encoding.getFloat(encAccessor);
    }
  }

  public double getDouble(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        return encoding.getDouble(accessor);
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        throw new JMFUninitializedAccessException("Invalid accessor: " + accessor); // see getValue
      }
      return encoding.getDouble(encAccessor);
    }
  }

  public void setBoolean(int accessor, boolean val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        // pure delegation
        encoding.setBoolean(accessor, val);
        return;
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        // setting a field that doesn't exist ... transition to delegation.
        becomeDelegator();
        encoding.setBoolean(accessor, val);
        return;
      }
      encoding.setBoolean(encAccessor, val);
      if (encoding instanceof JSMessageData && ((JSMessageData)encoding).contents == null) {
        becomeDelegator();
      }
    }
  }

  public void setByte(int accessor, byte val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        // pure delegation
        encoding.setByte(accessor, val);
        return;
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        // setting a field that doesn't exist ... transition to delegation.
        becomeDelegator();
        encoding.setByte(accessor, val);
        return;
      }
      encoding.setByte(encAccessor, val);
      if (encoding instanceof JSMessageData && ((JSMessageData)encoding).contents == null) {
        becomeDelegator();
      }
    }
  }

  public void setShort(int accessor, short val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        // pure delegation
        encoding.setShort(accessor, val);
        return;
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        // setting a field that doesn't exist ... transition to delegation.
        becomeDelegator();
        encoding.setShort(accessor, val);
        return;
      }
      encoding.setShort(encAccessor, val);
      if (encoding instanceof JSMessageData && ((JSMessageData)encoding).contents == null) {
        becomeDelegator();
      }
    }
  }

  public void setChar(int accessor, char val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        // pure delegation
        encoding.setChar(accessor, val);
        return;
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        // setting a field that doesn't exist ... transition to delegation.
        becomeDelegator();
        encoding.setChar(accessor, val);
        return;
      }
      encoding.setChar(encAccessor, val);
      if (encoding instanceof JSMessageData && ((JSMessageData)encoding).contents == null) {
        becomeDelegator();
      }
    }
  }

  public void setLong(int accessor, long val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        // pure delegation
        encoding.setLong(accessor, val);
        return;
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        // setting a field that doesn't exist ... transition to delegation.
        becomeDelegator();
        encoding.setLong(accessor, val);
        return;
      }
      encoding.setLong(encAccessor, val);
      if (encoding instanceof JSMessageData && ((JSMessageData)encoding).contents == null) {
        becomeDelegator();
      }
    }
  }

  public void setFloat(int accessor, float val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        // pure delegation
        encoding.setFloat(accessor, val);
        return;
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        // setting a field that doesn't exist ... transition to delegation.
        becomeDelegator();
        encoding.setFloat(accessor, val);
        return;
      }
      encoding.setFloat(encAccessor, val);
      if (encoding instanceof JSMessageData && ((JSMessageData)encoding).contents == null) {
        becomeDelegator();
      }
    }
  }

  public void setDouble(int accessor, double val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        // pure delegation
        encoding.setDouble(accessor, val);
        return;
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        // setting a field that doesn't exist ... transition to delegation.
        becomeDelegator();
        encoding.setDouble(accessor, val);
        return;
      }
      encoding.setDouble(encAccessor, val);
      if (encoding instanceof JSMessageData && ((JSMessageData)encoding).contents == null) {
        becomeDelegator();
      }
    }
  }

  public JMFNativePart getNativePart(int accessor, JMFSchema schema) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        return encoding.getNativePart(accessor, schema);
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        throw new JMFUninitializedAccessException("Invalid accessor: " + accessor); // see getValue
      }
      return encoding.getNativePart(encAccessor, schema);
    }
  }

  public int getModelID(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map == null) {
        return encoding.getModelID(accessor);
      }
      int encAccessor = map.indices[accessor];
      if (encAccessor == -1) {
        throw new JMFUninitializedAccessException("Invalid accessor: " + accessor); // see getValue
      }
      return encoding.getModelID(encAccessor);
      }
  }

  public void unassemble() throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "unassemble");
    becomeDelegator();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "unassemble");
  }

  public JMFList createBoxList(int accessor, Object val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
    synchronized (getMessageLockArtefact()) {
      if (map != null) {
        becomeDelegator();
      }
      return encoding.createBoxList(accessor, val);
    }
  }

  /*
   * Return a DataSlice containing the assembled message content, or null if the
   * message is not assembled.
   * See JMFMessage for javadoc description.
   * d348294
   * Locking: No necessaity to lock - encoding won't suddenly become something other
   *          than a JMFMessage, and the encoding.getAssembledContent() method will
   *          take the lock before doing the real work.
   */
  public DataSlice getAssembledContent() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getAssembledContent");

    DataSlice result = null;

    // We can only return something useful if the encoding is a JMFMessage.
    if (encoding instanceof JMFMessage) {

      // So we just ask the encoding to return what it has (or null)
      result = ((JMFMessage)encoding).getAssembledContent();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getAssembledContent", result);
    return result;
  }

  // Implement the JMFMessageData.estimateUnassembledValueSize() method.
  // by simply calling through to the encoding message.
  public int estimateUnassembledValueSize(int index) {
    return ((JMFMessageData)encoding).estimateUnassembledValueSize(index);
  }

  /*
   * isEMPTYlist
   * Return true if the value of the given field is one of the singleton
   * EMPTY lists, otherwise false.
   * See JMFMessage for javadoc description.
   */
  public boolean isEMPTYlist(int accessor) {
    if (encoding instanceof JMFMessage) {
      return ((JMFMessage)encoding).isEMPTYlist(accessor);
    }
    else {
      return false;
    }
  }
}
