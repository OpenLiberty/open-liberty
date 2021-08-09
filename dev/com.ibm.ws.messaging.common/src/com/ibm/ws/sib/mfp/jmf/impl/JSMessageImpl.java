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

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.jmf.JMFEncapsulation;
import com.ibm.ws.sib.mfp.jmf.JMFException;
import com.ibm.ws.sib.mfp.jmf.JMFList;
import com.ibm.ws.sib.mfp.jmf.JMFMessage;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFNativePart;
import com.ibm.ws.sib.mfp.jmf.JMFRegistry;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;
import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.mfp.util.ArrayUtil;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class implements JMFMessage for the purpose of managing a JMF-encoded
 * message. It is also the implementation of JMFNativePart for dynamic fields.
 * It inherits some of its behavior from JSMessageData, which contains code
 * common between a JSMessageImpl and a JSListImpl.
 */
public final class JSMessageImpl extends JSMessageData implements JMFMessage {

  private static TraceComponent tc = JmfTr.register(JSMessageImpl.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

  // The JSchema for this JSMessageImpl
  private JSchema schema;

  // Local value of schema.getFields() for fast access
  private JSField[] fields;

  // Local value of schema.getVariants() for fast access
  private JSVariant[] variants;

  // Computed value of fields.length + variants.length to use when deciding
  // whether an accessor refers to boxed information.
  private final int firstBoxed;

  // The limit of accessor values for this message
  private final int accessorLimit;

  // The JSBoxManager to which we delegate all accesses that are in the boxed range.
  // Will be null iff firstBoxed==accessorLimit, indicating that there are no boxed
  // values.
  private JSBoxManager boxManager;

  // The offset within contents where the data of the message begins. This is the
  // original offset iff contents is as it was supplied originally and hasn't been
  // reallocated. Otherwise, it will be zero.
  private int messageOffset;

  // Offset of the offset table within the message
  private int tableOffset;

  // Offset of the message data within the message
  private int dataOffset;

  // Indicates that the contents variable was reallocated by the JSMessageImpl
  // and is therefore not the original message frame.
  private boolean reallocated;

  // The length of the message (including encrypted portion, if any).
  private int length;

  // Cache to keep getSchemata from doing redundent work
  private JMFSchema[] schemata;

  // The MessageMap associated with this JSMessageImpl when it is assembled or
  // half-assembled. Will be null when the JSMessageImpl is unassembled.
  private MessageMap map;

  // The offset table for the message
  private int[] oTable;

  // Choice cache for this message. This is present only when the message is
  // unassembled.
  // An assembled message has a map, which means you can access its
  // multiChoice code and its individual choices through the map.
  private int[] choiceCache;

  // Construct a new JSMessageImpl for a particular schema. The arguments are
  // precisely those of JSchemaInterpreter.newMessage.
  public JSMessageImpl(JSchema schema) {

    // The super constructor sets cacheSize
    super(schema.getFields().length);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "<init>", schema);

    this.schema = schema;
    fields = schema.getFields();
    if (cacheSize > 0) {
      cache = new Object[cacheSize];
    }
    variants = schema.getVariants();
    firstBoxed = cacheSize + variants.length;

    int[][] boxed = schema.getBoxed();

    if (boxed.length > 0) {
      boxManager = new JSBoxManager(this, boxed);
    }

    accessorLimit = firstBoxed + boxed.length;
    setMaster();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "<init>");
  }

  // Construct a new JSMessageImpl for an incoming message frame. The arguments are
  // precisely those of JSchemaInterpreter.decode, except the 'readSchemata' flag, which
  // is always true when the JSMessageImpl is created by the JSchemaInterpreter (for a
  // top-level JMFMessage) and always false when the JSMessageImpl is created
  // elsewhere (for a subsidiary JMFNativePart or boxed variant).
  public JSMessageImpl(JSchema schema, byte[] contents, int offset, int length, boolean readSchemata) throws JMFMessageCorruptionException {

    this(schema);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "<init>", new Object[]{schema, contents, Integer.valueOf(offset), Integer.valueOf(length), Boolean.valueOf(readSchemata)});

    this.contents = contents;
    messageOffset = offset;
    this.length = length;

    try {
      // Read dependent schemata if instructed to do so
      if (readSchemata) {
        int nSchemata = ArrayUtil.readShort(contents, offset);

        // Make sure the size looks valid before we allocate space and
        // start reading
        if (  (nSchemata < 0)
           || ((offset + 2 + (nSchemata * 8)) > contents.length)
           ) {

          JMFMessageCorruptionException jmce =  new JMFMessageCorruptionException(
                    "Bad schemata length: " + nSchemata + " at offset " + offset);
          FFDCFilter.processException(jmce, "com.ibm.ws.sib.mfp.jmf.impl.JSMessageImpl.<init>", "226", this,
              new Object[] { MfpConstants.DM_BUFFER, contents, Integer.valueOf(0), Integer.valueOf(contents.length) });
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "<init>", "JMFMessageCorruptionException");
          throw jmce;
        }

        offset += 2;
        schemata = new JMFSchema[nSchemata + 1];
        schemata[0] = schema;

        for (int i = 0; i < nSchemata; i++) {
          long schemaId = ArrayUtil.readLong(contents, offset);
          offset += 8;
          schemata[i + 1] = JMFRegistry.instance.retrieve(schemaId);

          // Note: we may not have the schema registered in which case
          // we'll get null back from the registry.
          // We don't diagnose this here as it may not be a problem - it
          // will only cause trouble if we later try to access fields
          // that need the missing schemas. This will cause
          // JMFSchemaViolationExceptions iff it occurs.
        }
      }

      // Read the multiChoice code
      int len = ArrayUtil.readShort(contents, offset);
      offset += 2;

      byte[] b = ArrayUtil.readBytes(contents, offset, len);
      offset += len;

      BigInteger multiChoice = new BigInteger(b);

      // Get the MessageMap for this message
      map = JSchemaInterpreterImpl.getMessageMap(schema, multiChoice);

      // Read offset table
      tableOffset = offset; // record start of offset table
      oTable = new int[map.offsetsNeeded];

      for (int i = 0; i < oTable.length; i++) {
        oTable[i] = ArrayUtil.readInt(contents, offset);
        offset += 4;
      }

      dataOffset = offset; // record end of offset table, start of data
    }
    catch (ArrayIndexOutOfBoundsException e) {
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.jmf.impl.JSMessageImpl.<init>", "275", this,
          new Object[] { MfpConstants.DM_BUFFER, contents, Integer.valueOf(0), Integer.valueOf(contents.length)});
      // If we get an ArrayIndexOutOfBounds error we have tried to read
      // beyond the end of the supplied contents buffer. This means the
      // data provided must be invalid.
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "<init>", "JMFMessageCorruptionException");
      throw new JMFMessageCorruptionException("Attempt to read beyond end of data buffer", e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "<init>");
  }

  // Copy constructor. This constructs a new JSMessageImpl from an existing one.
  // Locking: 'original' must be locked around any call to this constructor.
  //          It is private and only called by getCopy() which does indeed take
  //          the lock.
  private JSMessageImpl(JSMessageImpl original) {

    // The super constructor sets cacheSize
    super(original.cacheSize);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "<init>", original);

    // These fields are invariant, since they relate to the schema definition
    // not the data itself. These can be safely copied from the original.
    schema = original.schema;
    fields = original.fields;
    variants = original.variants;
    firstBoxed = original.firstBoxed;
    accessorLimit = original.accessorLimit;

    if (original.boxManager != null) {
      boxManager = new JSBoxManager(this, schema.getBoxed());
    }

    schemata = original.schemata;

    // These fields relate to the message data, so the copy needs its  own
    // versions. Primitives can just be copied but the arrays need to be
    // duplicated, if present.
    messageOffset = original.messageOffset;
    tableOffset = original.tableOffset;
    dataOffset = original.dataOffset;
    reallocated = original.reallocated;
    length = original.length;
    map = original.map;

    if (original.oTable != null) {
      oTable = (int[]) original.oTable.clone();
    }

    if (original.choiceCache != null) {
      choiceCache = (int[]) original.choiceCache.clone();
    }

    // Always need a new boxedCache. This can safely start empty, as it can
    // (and should) be recreated if and when needed.
    if (boxManager != null) {
        boxManager.reset();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "<init>");
  }

  // Implementation of the getAbsoluteOffset method defined in JSMessageData
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues.
  int getAbsoluteOffset(int index) throws JMFUninitializedAccessException {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getAbsoluteOffset", new Object[]{Integer.valueOf(index)});

    MessageMap.Remap remap = map.fields[index];

    if (remap == null) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getAbsoluteOffset", "JMFUninitializedAccessException");
      throw new JMFUninitializedAccessException(schema.getPathName(fields[index]) + " Schema: " + schema.getName());
    }

    int varOffset = remap.offsetIndex;
    varOffset = (varOffset < 0) ? 0 : oTable[varOffset];

    int result = dataOffset + varOffset + remap.fixedIncr;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getAbsoluteOffset", Integer.valueOf(result));
    return result;
  }

  // Implementation of the getFieldDef method defined in JSMessageData
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues.
  JSField getFieldDef(int accessor, boolean mustBePresent) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getFieldDef", new Object[]{Integer.valueOf(accessor), Boolean.valueOf(mustBePresent)});

    if (mustBePresent) {
      if ((map == null) || (map.fields[accessor] == null)) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getFieldDef", null);
        return null;
      }
    }

    JSField result = fields[accessor];
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getFieldDef", result);
    return result;
  }

  // Implementation of the assembledForField method defined in JSMessageData
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues.
  boolean assembledForField(int index) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "assembledForField", new Object[]{Integer.valueOf(index)});

    if (contents != null) {
      // Message is currently assembled
      if (map.fields[index] != null) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "assembledForField", Boolean.TRUE);
        return true;
      }

      // Assembled, but not for this field: we must change it to unassembled.
      unassemble();
    }
    else {
      if (choiceCache == null) {
        makeChoiceCache();
      }

      map = null; // in case message was half-assembled
    }

    // Message is now unassembled. Set the dominating cases for the field and make
    // choiceCache, cache, and boxedCache consistent with the new choice
    // structure (if a variant field was changed).
    if (setDominatingCases(fields[index])) {
      unsetCachesByCases();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "assembledForField", Boolean.FALSE);
    return false;
  }

  // Subroutine to make the choiceCache, cache, and boxedCache consistent with
  // the current choice structure after that structure changes.
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues.
  private void unsetCachesByCases() throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "unsetCachesByCases");

    // If the cache is still shared it must be copied before being updated
    if (sharedCache) {
      copyCache();
    }

    for (int i = 0; i < choiceCache.length; i++) {
      if ((choiceCache[i] != -1) && !consistentChoice(i)) {
        choiceCache[i] = -1;
      }
    }

    if (cache != null) {
      for (int i = 0; i < cache.length; i++) {
        if ((cache[i] != null) && !consistentCache(i)) {
          cache[i] = null;

          if (fields[i] instanceof JSVariant) {
            // boxManager will not be null if there are variants in the
            // main cache
            boxManager.cleanupBoxedCache(i);
          }
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "unsetCachesByCases");
  }

  // Subroutine to determine if a given choice in choiceCache is consistent
  // with its dominating choices in choiceCache.
  // This method can have side-effects: if a choice is consistent with its immediate
  // dominator but that choice is itself inconsistent, the dominator will be
  // set to -1 in addition to this method returning false. This applies recursively,
  // so that a definitive answer is always returned and all inconsistent dominators
  // are set to -1.
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues.
  boolean consistentChoice(int varIndex) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "consistentChoice", new Object[]{Integer.valueOf(varIndex)});

    JSType field = findDominatingCase(variants[varIndex]);
    JSVariant parent = (JSVariant) field.getParent();

    if (parent == null) {
      // This choice is not dominated by any others, so it must be consistent
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "consistentChoice", Boolean.TRUE);
      return true;
    }

    int domIndex = parent.getIndex();

    if (choiceCache[domIndex] != field.getSiblingPosition()) {
      // This choice is inconsistent with its immediate dominator
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "consistentChoice", Boolean.FALSE);
      return false;
    }

    // This choice is consistent with its immediate dominator, but is that
    // dominator itself consistent?
    if (!consistentChoice(domIndex)) {
      choiceCache[domIndex] = -1; // Mark dominator as inconsistent

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "consistentChoice", Boolean.FALSE);
      return false; // and this choice is inconsistent as well
    }

    // Otherwise, we are consistent with our dominator, which is itself
    // consistent, so victory.
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "consistentChoice", Boolean.TRUE);
    return true;
  }

  // Subroutine to find the JSType that is the "dominating case" of a given JSType.
  // This means either the root JSType iff the given JSType has no JSVariant ancestor,
  // or the JSType that is an ancestor of the given JSType and also the immediate child
  // of the immediately dominating JSVariant (non-private and static: also called from
  // JSCompatibleMessageImpl).
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues.
  static JSType findDominatingCase(JSType field) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(tc, "findDominatingCase", field);

    JSType parent = (JSType) field.getParent();
    while ((parent != null) && !(parent instanceof JSVariant)) {
      field = parent;
      parent = (JSType) field.getParent();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(tc, "findDominatingCase", field.getFeatureName());
    return field;
  }

  // Subroutine to determine if the presence of a given cache entry is consistent with its
  // dominating choices in choiceCache. ChoiceCache is assumed to be self-consistent, so that only
  // the immediate dominator needs to be checked.
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues.
  private boolean consistentCache(int accessor) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "consistentCache", new Object[]{Integer.valueOf(accessor)});

    JSType field = findDominatingCase(fields[accessor]);
    JSVariant parent = (JSVariant) field.getParent();

    boolean result = (parent == null) || (choiceCache[parent.getIndex()] == field.getSiblingPosition());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "consistentCache", Boolean.valueOf(result));
    return result;
  }

  // Implementation of the isFieldVarying method defined in JSMessageData
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues.
  boolean isFieldVarying(int index) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "isFieldVarying", new Object[]{Integer.valueOf(index)});

    boolean result = map.fields[index].varying;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "isFieldVarying", Boolean.valueOf(result));
    return result;
  }

  // Implementation of getJMFSchema
  // Locking: Not required - schema is invariant after construction
  public JMFSchema getJMFSchema() {
    return schema;
  }

  // Implementation of getModelID
  // Locking: Not required - returning a static final constant
  public int getModelID() {
    return MODEL_ID_JMF;
  }

  // Implementation of getEncodingSchema. Since this is not a "compatibility layer"
  // JMFMessage, getEncodingSchema and getSchema are the same.
  // Locking: Not required - schema is invariant after construction
  public JMFSchema getEncodingSchema() {
    return schema;
  }

  // Implementation of getJMFEncodingVersion
  // Locking: Not required - returning a static final constant
  public short getJMFEncodingVersion() {
    return JMFRegistry.JMF_ENCODING_VERSION;
  }

  // Implementation of originalFrame
  // Locking: Required in case it is ever called for real. Currently this is
  //          only called by Unit Tests so it is academic.
  public int originalFrame() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "originalFrame");

    int result;
    synchronized (getMessageLockArtefact()) {

      if ((contents == null) || reallocated) {
        result = -1;
      }
      else {
        result = length;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "originalFrame", Integer.valueOf(result));
    return result;
  }

  // Implementation of getSchemata. Note that, largely to support diagnosis of problems
  // and not for any truly intrinsic reason, we always place the encoding schema first.
  // Locking: schemata can be invalidated, so need to lock whole method, including
  //          copying the reference into a local value to return.
  public JMFSchema[] getSchemata() throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getSchemata");
    JMFSchema[] result;

    synchronized (getMessageLockArtefact()) {
      if (schemata == null) {
        Set<JMFSchema> accum = new HashSet<JMFSchema>();
        getSchemata2(accum, false);
        schemata = new JMFSchema[1 + accum.size()];
        schemata[0] = schema;

        Iterator<JMFSchema> iter = accum.iterator();

        for (int i = 1; i < schemata.length; i++) {
          schemata[i] = iter.next();
        }
      }
      result = schemata;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getSchemata", result);
    return result;
  }

  // Subroutine of getSchemata which recursively visits Dynamic fields as part of the
  // implementation of the public getSchemata method. Note that this method can call
  // getValue internally, which means that it executes rapidly on an unassembled message
  // but can slow down the otherwise streamlined processing of an assembled message. This
  // performance issue is compensated for by maintaining the schemata variable as a cache,
  // which is only invalidated when the message is disassembled or a dynamic field
  // changes. The addThis flag says whether this JSMessageImpl's encoding schema is to be
  // added to the accumulating schemata; this is true of the schema for a JSMessageImpl
  // that is providing a dynamic, but is false for one that is providing a boxed variant.
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues. Only called by getSchemata() & getSchemata3().
  private void getSchemata2(Set<JMFSchema> accum, boolean addThis) throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getSchemata2", new Object[]{accum, Boolean.valueOf(addThis)});

    if (addThis) {
      accum.add(schema);
    }

    if (map != null) {
      // If the message is assembled, we find the JSDynamic and JSVariant
      // fields defined in the map.
      for (int i = 0; i < fields.length; i++) {
        if (map.fields[i] != null) {
          JSField field = fields[i];

          if (field instanceof JSDynamic) {
            getSchemata3(accum, true, getInternal(i));
          }
          else if (field instanceof JSVariant) {
            getSchemata3(accum, false, getInternal(i));
          }
        }
      }
    }
    else {
      // For unassembled messages we find the JSDynamic and JSVariant
      // fields that have values in the cache
      for (int i = 0; i < fields.length; i++) {
        if (cache[i] != null) {
          if (fields[i] instanceof JSDynamic) {
            getSchemata3(accum, true, cache[i]);
          }
          else if (fields[i] instanceof JSVariant) {
            getSchemata3(accum, false, cache[i]);
          }
        }
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getSchemata2");
  }

  // Subroutine of getSchemata that handles the possibility of arrays of schema-bearing
  // objects. One you get down to an individual JSMessageImpl object and call getSchemata
  // method on it, we ask it to add its top-level schema if its a Dynamic, but not if it's
  // a boxed variant. A JMFEncapsulation object always represents a Dynamic and has a
  // different getSchemata signature.
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues. Only called by getSchemata2().
  private void getSchemata3(Set<JMFSchema> accum, boolean addThis, Object obj)
          throws JMFUninitializedAccessException,
          JMFSchemaViolationException, JMFModelNotImplementedException,
          JMFMessageCorruptionException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getSchemata3", new Object[]{accum, Boolean.valueOf(addThis), obj});

    if (obj == null) {
      // Do nothing
    }

    else if (obj instanceof JMFList) {
      List list = (List) obj;
      for (int i = 0; i < list.size(); i++) {
        getSchemata3(accum, addThis, list.get(i));
      }
    }

    else if (obj instanceof JSMessageImpl) {
      ((JSMessageImpl) obj).getSchemata2(accum, addThis);
    }

    else {
      ((JMFEncapsulation) obj).getSchemata(accum);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getSchemata3");
  }

  // Implementation of invalidateSchemaCache, overriding the version in JSMessageData
  // Locking: The caller is expected to hold the lock.
  void invalidateSchemaCache() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "invalidateSchemaCache");

    // If this is the master message, clear the cache....
    if (isMaster()) {
      schemata = null;
    }
    // ... otherwise call on up the tree.
    else {
      getParent().invalidateSchemaCache();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "invalidateSchemaCache");
  }

  // Implementation of getEncodedLength
  // NOTE: This method has had extra instrumentation added to try and catch an
  // intermittent NPE that gets thrown by a stress test. The instrumentation
  // code is a little odd - we don't want to introduce new NLS messages and so
  // on, but we do want to FFDC as close to the error as possible. To that end
  // when we detect an error we construct a JMFException to allow us to FFDC,
  // but we don't throw it. (Throwing it would expose our non-translated
  // message, and this method shouldn't throw JMFException anyway.) Instead we
  // FFDC the new exception and then allow the code to continue, which will
  // result in a NPE with a JVM-generated message! Just in case the exception
  // wasn't caused by one of our instrumentation points we trap all NPE's as
  // well.
  // Locking: Holding the lock for the duration of the function is vital.
  public int getEncodedLength() throws JMFUninitializedAccessException,
        JMFSchemaViolationException, JMFModelNotImplementedException,
        JMFMessageCorruptionException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getEncodedLength");

    try {
      int ans;

      synchronized (getMessageLockArtefact()) {

        if (contents != null) {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getEncodedLength", Integer.valueOf(length));
          return length;
        }

        if (map == null) {
          map = JSchemaInterpreterImpl.getMessageMap(schema, choiceCache);

          if (map == null) {
            // Instrumentation - see above comment.
            JMFException e = new JMFException("Map was null after getMessageMap.");
            FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.jmf.impl.JSMessageImpl.getEncodedLength", "770", this);
          }
        }

        // Message is now in the "half-assembled" state: it has a map,
        // but no contents. This is a safe state to be in indefinitely,
        // though it usually only persists between calls to this method
        // and calls to toByteArray.
        // The length includes at least the offset table
        ans = map.offsetsNeeded * 4;

        // Now add up the lengths of the fields.
        for (int i = 0; i < fields.length; i++) {
          if (map.fields[i] != null) {
            ans += getLength(cache[i], fields[i]);
          }
        }

        // Account for other material
        BigInteger multiC = map.multiChoice;

        if (multiC == null) {
          // Instrumentation - see above comment.
          JMFException e = new JMFException("multiChoice was null.");
          FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.jmf.impl.JSMessageImpl.getEncodedLength", "795", this);
        }

        byte[] bytes = multiC.toByteArray();

        if (bytes == null) {
          // Instrumentation - see above comment.
          JMFException e = new JMFException("bytes was null.");
          FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.jmf.impl.JSMessageImpl.getEncodedLength", "803", this);
        }

        ans += (bytes.length + 2); // Account for the choice code

        if (isMaster()) {
          // Account for any extra schemata to be included
          ans += 2; // length field

          if (schemata == null) {
            getSchemata();
          }

          if (schemata == null) {
            // Instrumentation - see above comment.
            JMFException e = new JMFException("schemata was null after getSchemata.");
            FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.jmf.impl.JSMessageImpl.getEncodedLength", "819", this);
          }

          ans += (8 * (schemata.length - 1));
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getEncodedLength", Integer.valueOf(ans));
      return ans;

    }
    catch (NullPointerException e) {
      // Instrumentation - see above comment.
      FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.jmf.impl.JSMessageImpl.getEncodedLength", "830", this);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getEncodedLength", e);
      throw e;
    }
  }

  // Subroutine of getEncodedLength that computes the length of an individual
  // field, taking into account whether it is an array and its element length.
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues. Only called by getEncodedLength().
  private int getLength(Object val, JSField fieldDef) throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getLength", new Object[]{val, fieldDef});

    // Since this method is only called to assemble a supposedly complete unassembled
    // message, any null in the cache indicates insufficient initialization.
    if (val == null) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getLength", "JMFUninitializedAccessException");
      throw new JMFUninitializedAccessException(schema.getPathName(fieldDef) + " Schema: " + schema.getName());

    }
    else if (val == nullIndicator) {
      int result = fieldDef.getEncodedValueLength(null, indirect, master);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getLength", Integer.valueOf(result));
      return result;

    }
    else {
      int result = fieldDef.getEncodedValueLength(val, indirect, master);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getLength", Integer.valueOf(result));
      return result;
    }
  }

  // Implementation of JMFMessage.toByteArray - see JMFMessage for Javadoc
  // Encodes the message data - variant settings and field contents - into the
  // given byte array at the given offset.
  // This method wraps the 2 parameter toByteArray() method which does the real
  // work. In the case where the messge is already assembled it performs a sanity
  // check on the length before calling the worker method.
  // Locking: Holding the lock for the duration of the function is vital.
  public int toByteArray(byte[] buffer, int offset, int length) throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "toByteArray", new Object[]{buffer, Integer.valueOf(offset), Integer.valueOf(length)});
    int result;

    synchronized (getMessageLockArtefact()) {

      // Sanity check the length passed in
      if ((contents != null)  && (length < this.length)) {
        // Caller must not have called getEncodedLength or there is
        // an error in it: should not occur
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "toByteArray", "IllegalStateException");
        throw new IllegalStateException();
      }

      // Call the real worker method
      result = toByteArray(buffer, offset);

    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "toByteArray", Integer.valueOf(result));
    return result;
  }

  // Real implementation of toByteArray:
  // Encodes the message data - variant settings and field contents - into the
  // given byte array at the given offset.
  // If the message has already been encoded/assembled, we can just copy the
  // existing contents into the given array. Otherwise the message must encode
  // itself now.
  // For a top-level (master) JMFMessage, the schemata is written out first.
  // The message then writes out the multichoice code, which determines the variant
  // settings. Each field is then called to write itself out into the byte array,
  // then the offset table is built and inserted into the approriate position.
  // The return value is the number of bytes written to the buffe array.
  // Locking: Holding the lock for the duration of the function is vital.
  int toByteArray(byte[] buffer, int offset) throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "toByteArray", new Object[]{buffer, Integer.valueOf(offset)});

    int result;

    synchronized (getMessageLockArtefact()) {

      int route = 0;
      int probe = 0;
      try {
        if (contents != null) {
          route |= 0x4;
          probe = 102;
          System.arraycopy(contents, messageOffset, buffer, offset, this.length);
          result = this.length;
        }
        else {
          route |= 0x8;
          probe = 200;
          // Assemble message now. We assume it is half-assembled because
          // there should be no mutating calls between calls to getEncodedLength
          // and calls to this method.
          int messageOffset = offset; // remember start of message

          if (isMaster()) {
            route |= 0x10;
            probe = 201;
            // Top level JMFMessage: add schemata
            if (schemata == null) {
              route |= 0x20;
              getSchemata();
            }
            probe = 202;
            ArrayUtil.writeShort(buffer, offset, (short)(schemata.length - 1));
            offset += 2;
            probe = 203;
            for (int i = 1; i < schemata.length; i++) {
              route |= 0x40;
              probe = 204;
              ArrayUtil.writeLong(buffer, offset, schemata[i].getID());
              offset += 8;
            }
            probe = 205;
          }
          probe = 300;
          // Add multiChoice code
          byte[] b = map.multiChoice.toByteArray();
          probe = 301;
          ArrayUtil.writeShort(buffer, offset, (short) b.length);
          probe = 302;
          offset += 2;
          ArrayUtil.writeBytes(buffer, offset, b);
          offset += b.length;
          probe = 303;

          int tableOffset = offset;

          // Account for offset table (will be filled in later).
          offset += (map.offsetsNeeded * 4);

          probe = 305;

          int dataOffset = offset; // record start of data
          int[] oTable = new int[map.offsetsNeeded];

          probe = 306;

          // store offsets here until we're ready to encode them
          int oTableIndex = 0; // track the offset table index as we go
                               // through the fields

          // Record the fields that are in the cache
          for (int i = 0; i < fields.length; i++) {
            route |= 0x80;
            probe = 400;
            if (map.fields[i] != null) {
              route |= 0x100;
              probe = 401;
              Object val = cache[i];
              probe = 402;

              if (val == null) {
                probe = 403;
                JMFUninitializedAccessException e =  new JMFUninitializedAccessException(schema.getPathName(fields[i]) + " Schema: " + schema.getName());
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "toByteArray", e);
                throw e;
              }

              offset = writeObject(val, fields[i], buffer, offset);
              probe = 404;

              // Update offset table information after a varying field
              if (map.fields[i].varying && (oTableIndex < oTable.length)) {
                route |= 0x200;
                probe = 405;
                oTable[oTableIndex++] = offset - dataOffset;
              }
            }
          }

          probe = 500;

          // Record the offset table in the message
          int toff = tableOffset;

          for (int i = 0; i < oTable.length; i++) {
            route |= 0x400;
            probe = 501;
            ArrayUtil.writeInt(buffer, toff, oTable[i]);
            toff += 4;
          }

          probe = 502;

          // If we get this far without an UninitializedAccessException it
          // is safe to complete the transition to assembled by filling in
          // the byte array related fields.
          contents = buffer;
          this.messageOffset = messageOffset;
          this.tableOffset = tableOffset;
          this.dataOffset = dataOffset;
          this.length = offset - messageOffset;
          this.oTable = oTable;
          choiceCache = null;
          sharedContents = false;
          probe = 503;

          if (isMaster()) {
            route |= 0x800;
            reallocated(buffer, -1);
          }
          result = offset - messageOffset;
        }
      }
      catch (NullPointerException e) {
        FFDCFilter.processException(e, "com.ibm.ws.sib.mfp.jmf.impl.JSMessageImpl.toByteArray", "1104", new Object[] { Integer.valueOf(probe), Integer.valueOf(route)});
        String msgInsert = "NullPointerException in JSMessageImpl: Probe = " + probe + " route = " + route;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.info(tc, "TEMPORARY_CWSIF9999", new Object[] {msgInsert });
        throw(e);
      }


    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "toByteArray", Integer.valueOf(result));
    return result;
  }

  // Implementation of JMFMessage.toByteArray - see JMFMessage for Javadoc
  // Encodes the message data - variant settings and field contents - to a new
  // byte array. If the message has already been encoded/assembled, we can return the existing
  // data. Otherwise, a new byte array is allocated and the preceding version of
  // toByteArray() is called to write into it.
  // Locking: Holding the lock for the duration of the function is vital.
  public byte[] toByteArray(int length) throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "toByteArray", new Object[]{Integer.valueOf(length)});

    byte[] buffer = null;

    synchronized (getMessageLockArtefact()) {
      // If the Message is already assembled
      if (contents != null) {

        if (length < this.length) {
          // Caller must have omitted to call getEncodedLength or
          // there is an error in it: should  not occur
          IllegalStateException e = new IllegalStateException();
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "toByteArray", e);
          throw e;
        }
        else {
          // If contents contains more than just this message, we'll need to copy
          // into a new byte[] so we may as well reallocate it into
          // a new 'contents' now, as it won't cost any more and will
          // mean no copying if called again
          if (messageOffset != 0) {
              reallocate(0);
          }

          // Just mark the contents as shared and return the byte
          // array we already have.
          sharedContents = true;
          buffer = contents;
        }
      }

      // If the Message is not already assembled - so assemble it into a
      // new buffer
      else {
        buffer = new byte[length];
        toByteArray(buffer, 0, length);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "toByteArray", buffer);
    return buffer;
  }

  // Subroutine to write a value into the physical byte stream of the message.
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues. Only called by toByteArray().
  private int writeObject(Object val, JSField fieldDef, byte[] buffer, int offset) throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "writeObject", new Object[]{val, fieldDef, buffer, Integer.valueOf(offset)});

    int result;

    if (val == nullIndicator) {
      result = fieldDef.encodeValue(buffer, offset, null, indirect, master);
    }
    else {
      result = fieldDef.encodeValue(buffer, offset, val, indirect, master);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "writeObject", Integer.valueOf(result));
    return result;
  }

  // Implement newNativePart
  // Locking: No need to lock - essentially a factory method; new instance is independent of
  //          'this'
  public JMFNativePart newNativePart(JMFSchema schema) {
    return new JSMessageImpl((JSchema) schema);
  }

  // Override the get/set methods that can take legal accessor arguments >= cacheSize to provide
  // special handling. Otherwise, pass through to the standard implementations.

  // Locking: cacheSize, firstBoxed & accessorLimit are final so there is
  //          no need to lock the tests against them, and both getValue() & getCase()
  //          take their own locks.
  //          The BoxManager stuff is a black art so we'll lock round it to be safe.
  public Object getValue(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getValue", new Object[]{Integer.valueOf(accessor)});
    Object result;

    if (accessor < cacheSize) {
      result = super.getValue(accessor);
    }
    else if (accessor < firstBoxed) {
      result = Integer.valueOf(getCase(accessor - cacheSize));
    }
    else if (accessor < accessorLimit) {
      synchronized (getMessageLockArtefact()) {
        result = boxManager.getBoxedlValue(accessor - firstBoxed);
      }
    }
    else {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getValue", "IndexOutOfBoundsException");
      throw new IndexOutOfBoundsException();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getValue", result);
    return result;
  }

  // Locking: cacheSize, firstBoxed & accessorLimit are final so there is
  //          no need to lock the tests against them, and both setValue() & setCase()
  //          take their own locks.
  //          The BoxManager stuff is a black art so we'll lock round it to be safe.
  public void setValue(int accessor, Object val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "setValue", new Object[]{Integer.valueOf(accessor), val});

    if (accessor < cacheSize) {
      super.setValue(accessor, val);
    }

    else if (accessor < firstBoxed) {
      setCase(accessor - cacheSize, ((Number) val).intValue());
    }

    else if (accessor < accessorLimit) {
      synchronized (getMessageLockArtefact()) {
        boxManager.setBoxedValue(accessor - firstBoxed, val);
      }
    }

    else {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "setValue", "IndexOutOfBoundsException");
      throw new IndexOutOfBoundsException();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "setValue");
  }

  // Locking: cacheSize & accessorLimit are final so there is
  //          no need to lock the tests against them. The callees take their own locks.
  public int getInt(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getInt", new Object[]{Integer.valueOf(accessor)});
    int result;

    if ((accessor >= cacheSize) && (accessor < firstBoxed)) {
      result = getCase(accessor - cacheSize);
    }
    else {
      result = super.getInt(accessor);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getInt", Integer.valueOf(result));
    return result;
  }

  // Locking: cacheSize & firstBoxed are final so there is
  //          no need to lock the tests against them. The callees take their own locks.
  public void setInt(int accessor, int val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "setInt", new Object[]{Integer.valueOf(accessor), Integer.valueOf(val)});

    if ((accessor >= cacheSize) && (accessor < firstBoxed)) {
      setCase(accessor - cacheSize, val);
    }
    else {
      super.setInt(accessor, val);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "setInt");
  }

  // Locking: cacheSize, firstBoxed & accessorLimit are final so there is
  //          no need to lock the tests against them, and the main callees take their own locks.
  //          The BoxManager stuff is a black art so we'll lock round it to be safe.
  public boolean isPresent(int accessor) {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "isPresent", new Object[]{Integer.valueOf(accessor)});
    boolean result;

    if (accessor < cacheSize) {
      result = super.isPresent(accessor);
    }
    else if (accessor < firstBoxed) {
      result = getCase(accessor - cacheSize) > -1;
    }
    else if (accessor < accessorLimit) {
      // Conservative answer: a boxed value is present if its containing box is present;
      // this is enough to support creation of the JSBoxedImpl for the value, which can
      // then be interrogated element by element.
      synchronized (getMessageLockArtefact()) {
        result = super.isPresent(boxManager.getBoxAccessor(accessor - firstBoxed));
      }
    }
    else {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "isPresent", "IndexOutOfBoundsException");
      throw new IndexOutOfBoundsException();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "isPresent", Boolean.valueOf(result));
    return result;

  }

  /**
   * Create a new list of variant boxes with a given shape at a given position
   * without creating any entries in the boxedCache. This is both an internal
   * subroutine and a quasi-public method used by WDO over JMF.
   *
   * @param accessor
   *            the internal accessor for the variant box list relative to
   *            this JSMessageImpl
   * @param val
   *            an array or Collection describing the desired shape. The value
   *            is not actually assigned to the result in any way
   * @return the new variant box list, which is also installed in this
   *         JSMessageImpl
   * @throws JMFSchemaViolationException
   * @throws JMFModelNotImplementedException
   * @throws JMFUninitializedAccessException
   * @throws JMFMessageCorruptionException
   */
  // Locking: Lock it - it may not need it but if not sure better to be safe.
  public JMFList createBoxList(int accessor, Object val) throws JMFSchemaViolationException,
                                                                JMFModelNotImplementedException,
                                                                JMFUninitializedAccessException,
                                                                JMFMessageCorruptionException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "createBoxList", new Object[]{Integer.valueOf(accessor), val});

    JSVaryingListImpl ans;

    synchronized (getMessageLockArtefact()) {
      JSVariant theVariant = (JSVariant) fields[accessor];
      ans = new JSVaryingListImpl(theVariant, val, theVariant.getIndirection(), this);
      setValue(accessor, ans);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "createBoxList", ans);
    return ans;
  }

  // Implement the reallocate method that simply reallocates the entire contents
  // buffer for this message.
  // @param offset The offset in the current contents buffer of a particular field.
  // @return The offset in the new contents buffer of the same field.
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues.
  int reallocate(int offset) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "reallocate", new Object[]{Integer.valueOf(offset)});

    byte[] oldContents = contents;
    int oldMessageOffset = messageOffset;
    contents = new byte[length];
    System.arraycopy(oldContents, messageOffset, contents, 0, length);
    tableOffset -= messageOffset;
    dataOffset -= messageOffset;
    messageOffset = 0;
    reallocated = true;
    reallocated(contents, -1);

    int result = offset - oldMessageOffset;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "reallocate", Integer.valueOf(result));
    return result;
  }

  // Override the reallocate method to provide for reallocating the contents when a field
  // changes size. This method is only called in a top-level JMFMessage (one with no
  // parent).
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues.
  int reallocate(int index, int offset, int oldLen, int newLen) {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "reallocate", new Object[]{Integer.valueOf(index), Integer.valueOf(offset), Integer.valueOf(oldLen), Integer.valueOf(newLen)});

    // Get oTable index for the next field after this one, if any. We can be
    // guaranteed that its offsetIndex is one greater than ours, since this is a
    // varying length field.
    int oInx = map.fields[index].offsetIndex + 1;

    // Update the oTable in place
    while (oInx < oTable.length) {
      oTable[oInx++] += (newLen - oldLen);
    }

    // Compute the length of the balance, after the varying length field
    int balance = (messageOffset + length) - offset - 4 - oldLen;

    // Create new contents entry, and update related offsets and lengths
    byte[] oldcontents = contents;
    int oldMessageOffset = messageOffset;
    int oldDataOffset = dataOffset;
    int oldTableOffset = tableOffset;
    length += (newLen - oldLen);
    contents = new byte[length];
    messageOffset = 0;
    tableOffset = dataOffset = oldTableOffset - oldMessageOffset;

    // Copy the schemata ids
    System.arraycopy(oldcontents, oldMessageOffset, contents, 0, tableOffset);

    // Write the offset table into the new array
    for (int i = 0; i < oTable.length; i++) {
      ArrayUtil.writeInt(contents, dataOffset, oTable[i]);
      dataOffset += 4;
    }

    // Copy up to the point where the length is changing
    System.arraycopy(oldcontents, oldDataOffset, contents, dataOffset, offset - oldDataOffset);

    // Copy balance of data after varying length field
    if (balance > 0) {
      System.arraycopy(oldcontents, offset + 4 + oldLen, contents, length - balance, balance);
    }

    reallocated = true;

    // We have a new contents buffer, so it can't be shared
    sharedContents = false;

    // Ensure any dependent JSMessageData parts in the cache are updated.
    reallocated(contents, -1);

    int result = offset - oldMessageOffset;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "reallocate", Integer.valueOf(result));
    return result;
  }

  // This method will be called after a new contents buffer has been reallocated. Pointers to
  // and offsets within the new buffer need to be passed down to any assembled // JSMessageData
  // items currently in the cache.
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues.
  void reallocated(byte[] newContents, int newOffset) {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "reallocated", new Object[]{newContents, Integer.valueOf(newOffset)});

    // When called from a top-level JSMessageImpl the newOffset parameter will be -1
    // and we don't need to change any instance variables (they will already be
    // updated). For lower level parts we must update the contents buffer pointer
    // and the fixed offsets into it - if we were in an assembled state.
    if ((newOffset != -1) && (contents != null)) {
      int oldOffset = messageOffset;
      contents = newContents;
      messageOffset = newOffset + 16;
      tableOffset = (messageOffset + tableOffset) - oldOffset;
      dataOffset = (messageOffset + dataOffset) - oldOffset;
      reallocated = true;
      sharedContents = false; // We have a new buffer so it can't be shared
    }

    // Now we must run through the cache any pass the changes down to any
    // depenedent JSMessageData parts we find.
    if (cache != null) {
      for (int i = 0; i < cache.length; i++) {
        try {
          Object entry = cache[i];

          if ((entry != null) && entry instanceof JSMessageData) {
            ((JSMessageData) entry).reallocated(newContents, getAbsoluteOffset(i));
          }
        }
        catch (JMFUninitializedAccessException e) {
          // No FFDC Code Needed - this cannot occur as we know the
          // JSMessageData part is present
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "reallocated");
  }

  // Subroutine to perform the "setCase" operation (public interface is setInt). The
  // accessor has been adjusted to a 'varIndex' which is an index into the getVariants()
  // part of the schema.
  // Locking: Needs to lock as map, choiceCache, etc may change be changed.
  private void setCase(int varIndex, int choice) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "setCase", new Object[]{Integer.valueOf(varIndex), Integer.valueOf(choice)});

    synchronized (getMessageLockArtefact()) {
      if (contents != null) {
        // message is assembled. Can only leave it assembled if this case
        // setting turns out to be a no-op
        if (map.choiceCodes[varIndex] == choice) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "setCase");
            return;
        }
        else {
            unassemble();
        }
      }
      else {
        if (choiceCache == null) {
            makeChoiceCache();
        }

        map = null; // in case message was half-assembled
      }

      // Message is unassembled. Set the dominating cases for the field and make
      // choiceCache, cache, and boxedCache consistent with the new choice structure
      // (if a variant field was changed in a way that could affect other choices).
      if (setCaseChain(varIndex, choice)) {
        unsetCachesByCases();
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "setCase");
  }

  // Subroutine to set a cascading chain of cases by following the variant dominator
  // relation up to the root. Mutually recursive with setDominatingCases.
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues. Called by setCase() & setDominatingCases
  private boolean setCaseChain(int varIndex, int choiceIndex) {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "setCaseChain", new Object[]{Integer.valueOf(varIndex), Integer.valueOf(choiceIndex)});

    if (choiceCache[varIndex] == choiceIndex) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "setCaseChain", Boolean.FALSE);
      return false;
    }

    int prevChoiceIndex = choiceCache[varIndex];
    choiceCache[varIndex] = choiceIndex;

    boolean dominated = setDominatingCases(variants[varIndex]);

    // We know we've changed a variant case, so we'll normally return 'true'
    // to signal the caller to check their caches. Howver if we set the
    // initial state of a variant that is not dominated by any other
    // variants the change cannot have any effect on anything already
    // cached, so we can return 'false'.
    boolean result = (dominated || (prevChoiceIndex != -1));

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "setCaseChain", Boolean.valueOf(result));
    return result;
  }

  // Subroutine to set the appropriate cases in all variants that dominate a field that is
  // now asserted to be present in the message. Mutually recursive with setCaseChain.
  // Returns true if the field is dominated by a variant, false otherwise.
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues. Called by setCaseChain() & assembledForField().
  private boolean setDominatingCases(JSType field) {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "setDominatingCases", new Object[]{field});

    field = findDominatingCase(field);

    JSVariant parent = (JSVariant) field.getParent();

    if (parent == null) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "setDominatingCases", Boolean.FALSE);
      return false;
    }

    boolean result = setCaseChain(parent.getIndex(), field.getSiblingPosition());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "setDominatingCases", Boolean.valueOf(result));
    return result;
  }

  // Override the unassemble method to take extra actions: nullify the oTable and the map,
  // make the choiceCache, and invalidate the schema cache
  // Locking: Holding the lock for the duration of the function is vital.
  //          Note: super.assemble() calls parent.unassemble(),
  //          possible deadlock situation avoided by locking the 'master'
  public void unassemble() throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "unassemble");

    // Do nothing if we're already unassembled
    if (contents == null) {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "unassemble", "contents is null");
      return;
    }

    synchronized (getMessageLockArtefact()) {
      super.unassemble();
      oTable = null;
      makeChoiceCache();
      map = null;
      invalidateSchemaCache();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "unassemble");
  }

  // Make the choiceCache the first time it is needed for an unassembled message.
  // Locking: We rely on something up the calling stack to protect us from
  //          concurrency issues.
  private void makeChoiceCache() {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "makeChoiceCache");

    choiceCache = new int[variants.length];

    if (map == null) {
      for (int i = 0; i < choiceCache.length; i++) {
        choiceCache[i] = -1;
      }
    }
    else {
      for (int i = 0; i < choiceCache.length; i++) {
        choiceCache[i] = map.choiceCodes[i];
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "makeChoiceCache");
  }

  // Subroutine to perform the "getCase" operation (public interface is getInt). The
  // accessor has been adjusted to a 'varIndex' which is an index into the getVariants()
  // part of the schema.
  // Locking: Needs to lock to ensure map etc don't change in the middle.
  private int getCase(int varIndex) {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getCase", new Object[]{Integer.valueOf(varIndex)});
    int result;

    synchronized (getMessageLockArtefact()) {
      if (map != null) {
        result = map.choiceCodes[varIndex];
      }
      else if (choiceCache == null) {
        result = -1;
      }
      else {
        result = choiceCache[varIndex];
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getCase", Integer.valueOf(result));
    return result;
  }

  // Implement the JMFMessage copy method.
  // Locking: handled by getCopy()
  public JMFMessage copy() {
    return (JSMessageImpl) getCopy();
  }

  // Implement the getCopy method to create a new JSMessageImpl instance.
  // Locking: Necessary as we mustn't copy a moving target. The lock on the original
  //          message needs to span the construction of the new one & the call to lazyCopy().
  JSMessageData getCopy() {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getCopy");
    JSMessageImpl copy;

    synchronized (getMessageLockArtefact()) {
      // Create a new instance of ourselves
      copy = new JSMessageImpl(this);

      // Allow the message data to be lazy-copied
      copy.lazyCopy(this);

      // The copy is a new top-level message
      copy.setMaster();

      // We must clear our boxed cache at this stage, since items in the
      // boxed cache may refer to entries in our now potentially shared cache
      // and this could result in  consistency problems. By clearing the
      // cache here it will get recreated with new copies when required.
      if (boxManager != null) {
        boxManager.reset();
      }

    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getCopy", copy);
    return copy;
  }

  // Return a DataSlice containing the assembled message content, or null if the
  // message is not assembled.  Added for d348294.
  // See JMFMessage for javadoc description.
  // Locking: Requires the lock as it relies on, and may change, vital instance variable(s).
  public DataSlice getAssembledContent() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.entry(this, tc, "getAssembledContent");

    DataSlice result = null;

    // lock the message - we don't want someone clearing the contents while we're doing this
    synchronized (getMessageLockArtefact()) {

      // If contents isn't null, the message is assembled so we can return something useful
      if (contents != null) {

        // We must mark the contents as shared now we're handing them out
        sharedContents = true;                                                  // d348294.1
        result = new DataSlice(contents, messageOffset, length);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) JmfTr.exit(this, tc, "getAssembledContent", result);
    return result;
  }

  /*
   * isEMPTYlist
   * Return true if the value of the given field is one of the singleton
   * EMPTY lists, otherwise false.
   * See JMFMessage for javadoc description.
   */
  public boolean isEMPTYlist(int accessor) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isEMPTYlist", accessor);

    boolean isEMPTY = false;

    // We are only interested in 'real' fields, not boxed variants etc
    if (accessor < cacheSize) {
      checkIndex(accessor);

      synchronized (getMessageLockArtefact()) {

        // We can only have an EMPTY singleton for the field if it is in the cache,
        // as anything which is (still) assembled can't be an EMPTY.
        // (Also, a false negative is harmless, whereas a false positive can lead to errors.)
        if (cache != null) {
          Object val = cache[accessor];
          // If the value is in the cache then see if it is an EMPTY singleton
          if (  (val == JSVaryingListImpl.EMPTY_UNBOXED_VARYINGLIST)
             || (val == JSFixedListImpl.EMPTY_FIXEDLIST)
             ) {
            isEMPTY = true;
          }
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isEMPTYlist",  isEMPTY);
    return isEMPTY;
  }
}
