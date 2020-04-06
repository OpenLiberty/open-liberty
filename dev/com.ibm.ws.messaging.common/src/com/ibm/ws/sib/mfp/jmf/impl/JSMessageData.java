/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.mfp.jmf.impl;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.ibm.websphere.ras.Traceable;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.sib.mfp.jmf.JMFEncapsulation;
import com.ibm.ws.sib.mfp.jmf.JMFMessageCorruptionException;
import com.ibm.ws.sib.mfp.jmf.JMFMessageData;
import com.ibm.ws.sib.mfp.jmf.JMFModelNotImplementedException;
import com.ibm.ws.sib.mfp.jmf.JMFNativePart;
import com.ibm.ws.sib.mfp.jmf.JMFPart;
import com.ibm.ws.sib.mfp.jmf.JMFPrimitiveType;
import com.ibm.ws.sib.mfp.jmf.JMFSchema;
import com.ibm.ws.sib.mfp.jmf.JMFSchemaViolationException;
import com.ibm.ws.sib.mfp.jmf.JMFUninitializedAccessException;
import com.ibm.ws.sib.mfp.jmf.JmfConstants;
import com.ibm.ws.sib.mfp.jmf.JmfTr;
import com.ibm.ws.sib.mfp.util.ArrayUtil;

/**
 * <P>
 * The JSMessageData class implements core parts of the JMFMessageData interface.
 * It is extended by both JSMessageImpl and JSListImpl. To accommodate JSListImpl under
 * Java's single-implementation-inheritance rule, this class extends AbstractList, which
 * is irrelevant to JSMessageImpl. We compensate by having UnsupportedOperation
 * implementations for get() and size(). Note that AbstractList adds no instance
 * overhead.
 * </P>
 * 
 * <H3>Locking policy:</H3>
 * <P>
 * With regards to locking there are two interesting aspects to JSMessageData and their derivative
 * classes;
 * </P><P>
 * 1) the way in which a set of JSMessageData are aggregated to form an overall platform messaging
 * message and
 * </P><P>
 * 2) the way in which they may share state with another JSMessageData instance.
 * </P><P>
 * In addition, it is possible for two threads to be operating on the same JSMessageData
 * instance due to the asynchronous nature of message spilling.
 * </P><P>
 * 1) JSMessages are built in instance hierarchies. Links away from the root of the tree are built
 * using the data described by the schema of a given JSMessageData instance, whilst links towards the root are
 * maintained by the 'parentMessage', 'containinggMessage' and 'master' instance members.
 * The key concurrency issue here is that methods such as unassemble call out to 'parent'
 * to unassemble themselves, whilst other 'methods' call down to their children. A locking
 * policy based on synchronized methods would therefore give the opportunity for deadlock.
 * To avoid deadlock we use the 'master' object to make threadsafe changes
 * to the state of 'this'. The client sees a state invariant that the 'master' is never
 * null (and iff the 'parent' JSMessageImpl is null then 'master' == 'compatabilityWrapperOrSelf'). Internally we may see states
 * where master is null, and if so we use 'comaptabilityWrapperOrSelf' to lock on (to cover the case when setting master).
 * </P><P>
 * 2) Lazy copying establishes the state where multiple JSMessageData instances share a subset of their
 * state. Once a stable consistent 'client visible' state associated with lazy copying is established
 * we are thread safe when using the above locking policy because the shared state is considered read
 * only by all sharers; an object wishing
 * to modify the state it is sharing will make a copy and then opt out of sharing.
 * So the interesting times are
 * when considering the internally visible states while establishing or opting out of sharing.
 * </P><P>
 * The instance members representing shared state are { contents, sharedContents, cache, sharedCache }.
 * (Note that while the primitives sharedContents and sharedCache are not themselves shareable by multiple
 * object references, they are truly part of the logical shared state because the implementation
 * uses java's 'friendly' package access to permit one instance to alter the state of another.)
 * </P><P>
 * During the period of establishing sharing we want to ensure that ....
 * </P><P>
 * a) the members of the shared state don't get changed by another thread's use of the message we
 * are going to share state with. The particular situation to consider is when sharing is first
 * established on a message's state, when that original owner does not yet
 * consider the state to be read only. This is guaranteed by locking of the 'master' member of that
 * message.
 * </P><P>
 * b) a 3rd party message unknown to this operation which is already sharing state with the message
 * we are copying does not alter the shared state (guaranteed by the policy that sharers consider
 * the shared state to be read only)
 * </P><P>
 * c) none of the shared state gets garbage collected (guaranteed by virtue of the fact we have a
 * handle on the message to be copied and it references the shared state).
 * </P><P>
 * During the period of opting out of sharing we want to ensure that
 * the shared state is not altered - guaranteed by the fact that sharing must at this point be fully
 * established and therefore the state is considered read-only.
 * </P><P>
 * The one concurrency issue left to consider regarding shared state is the potential for deadlock
 * when locking the message to be lazily copied. We can assure ourselves that this is not the case
 * if we assume that lazyCopy is always called as part of initialization of a new message instance
 * which has never been communicated outside the context of the code that creates it. The conclusion
 * is that the creating code need not lock the new instance since it is a secret, and another thread
 * can not be attempting to lock that instance, again because it is a secret know only to the creator.
 * </P><P>
 * The JSMessageData class has three concrete subclasses, JSMessageImpl, JSFixedListImpl and JSVaryingListImpl.
 * The locking policy is only carried down into the JSMessageImpl because we assume that the other two
 * only occur as the internals of containment hierarchies rooted in JSMessageImpl instances. We therefore
 * make the assumption that instances of these other classes are protected by locking the JSMessageImpl
 * in the same way that any other JSMessageImpl state is protected.
 * </P><P>
 * Note that the implementation permits a situation where the set of messages sharing state is of cardinality 1;
 * achieved by having two members of the set, one of which opts out of sharing. This means that whilst
 * the last member could theoretically recycle the shared state into private state, it will in fact go through
 * the full opting out process and copy the "shared" state when wanting to make an update to it.
 * A performance optimisation might be to track the set of sharers within the shared state itself,
 * and recognise the situation where the set drops to cardinality 1.
 * </P><P>
 * Note also that the implementation makes significant use of package access of both member functions
 * and instance members. We can therefore make few assumptions about whether a thread already holds the
 * lock on the 'master' when calling a protected or default access method. The fact that some instance
 * members are referenced from within the other classes of this package makes guaranteeing thread safety
 * very difficult, even with the locking that is in place.
 * 
 */
public abstract class JSMessageData extends AbstractList implements JMFMessageData, FFDCSelfIntrospectable, Traceable {
    private static TraceComponent tc = JmfTr.register(JSMessageData.class, JmfConstants.MSG_GROUP, JmfConstants.MSG_BUNDLE);

    // Subclasses must implement the following methods
    // Method to find the absolute offset in the contents associated with a particular
    // accessor.  This method will only be called when contents are non-null (that is, when
    // the message is assembled).
    abstract int getAbsoluteOffset(int accessor)
                    throws JMFUninitializedAccessException;

    // Method to retrieve the JSField for a particular accessor.  If mustBePresent is true,
    // the method should return null if the field is not present in the contents.  The
    // method will only be called with mustBePresent==true when the message is assembled.
    // If mustBePresent is false, the method should return the JSField unconditionally.
    abstract JSField getFieldDef(int accessor, boolean mustBePresent);

    // Method to determine if a message is assembled and also contains a particular field.
    // If the field is not present, unassembles the message as a side-effect.  This method
    // is guaranteed to be called before each fresh setting of a field and so can have other
    // appropriate side-effects (for example, in JSMessageImpl, the implemention of this
    // method sets dominating choices).
    abstract boolean assembledForField(int accessor)
                    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException;

    // Method to determine if a particular field has varying length.  This could actually be
    // computed from the result of getFieldDef but subclasses generally have more efficient
    // ways of answering the question when the message is assembled.  Not called when the
    // message is unassembled.
    abstract boolean isFieldVarying(int accessor);

    // Method to create a copy of message or message part
    abstract JSMessageData getCopy();

    // Method to reallocate the contents buffer for this message part
    abstract int reallocate(int offset);

    // Method to replace the contents buffer for this part (and any depenedent parts)
    // with a new reallocated buffer.
    abstract void reallocated(byte[] newContents, int newOffset);

    // Method to return the length of the contents buffer
    abstract int getEncodedLength()
                    throws JMFUninitializedAccessException, JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException;

    // An indicator of an actual null value in the cache for a value in the message that is
    // of a type capable of being null.  This convention permits a null in a cache array to
    // be interpreted as a cache miss.
    static final Object nullIndicator = new Object();

    // The cache array.  Nulls in this array do not indicate null values in the message,
    // they only indicate that the cache entry is not populated.  Null values in the message
    // are indicated by nullIndicator.  Invariant: the cache is non-null if size > 0 and in
    // that case, also, cache.length >= size.
    Object[] cache;

    // The actual size of the cache independent of the length of the cache array.  This
    // value is never changed once set.
    final int cacheSize;

    // The message contents for both reading and writing (null if the message is
    // unassembled).
    byte[] contents;

    // These flags indicate if the cache array or the assembled contents buffer is
    // currently shared with one or more other unrelated JSMessageData parts.  An
    // "unrelated" part is one that is not a parent/child/sibling of this part.
    // N.B. even though they are not object references, they ARE shared by virtue
    // of Java's friendly default access protection and may be altered by another instance!
    // (see lazyCopy())
    boolean sharedCache;
    boolean sharedContents;

    // The parent JSMessageData of which this one is a dependent, or null if this is
    // the root one.  A root JSMessageData will handle the overwriting of a variable
    // length field by reallocating the contents while all others handle this case by
    // performing unassembly of the message.
    private JSMessageData parentMessage;

    // The 'containing message' for this JSMessageData.
    // Usually this is the same as parent, but if the parent is the encoding for
    // a JSCompatibleMessage then we need to store the JSCompatibleMessage instead.
    private JMFMessageData containingMessage;

    // The master JMF Message for the message of which this segment is a part.  When a
    // JSMessageImpl is constructed at top level (by the JSchemaInterpreterImpl) its master
    // field is set to its compatabilityWrapperOrSelf.  Thereafter, every JSMessageData
    // inherits its parent's master field.
    JMFMessageData master;

    // If this JSMessageData is the 'encoding' for a JSCompatibleMessageImpl, we need
    // a reference to our wrapper. Some methods need to use the wrapper of we have one,
    // and ourself if we don't, so initialize the variable to ourself.
    private JMFMessageData compatibilityWrapperOrSelf;

    // The indirect argument to pass to all JSCoder methods.  This is always -1 for
    // JSMessageImpl.  A JSFixedListImpl sets this to zero.  A JSVaryingListImpl sets it
    // according to its constructor argument.
    int indirect = -1;

    // The subclasses have to pass the cacheSize into their super constructor because it
    // is declared final here.
    // Defaults the compatibilityWrapperOrSelf value to itself.
    JSMessageData(int theCacheSize) {
        this.cacheSize = theCacheSize;
        setCompatibilityWrapperToSelf();
    }

    // make this public so that a client can make state transition assumptions across calls
    public final Object getMessageLockArtefact() {
        if (master != null)
            return master;
        if (compatibilityWrapperOrSelf != null)
            return compatibilityWrapperOrSelf;
        return this;
    }

    // Return the parent JSMessageData of which this one is a dependent.
    // Locking: The caller is expected to hold the lock.
    final JSMessageData getParent() {
        return parentMessage;
    }

    // Set the parent JSMessageData of which this one is a dependent, and the
    // containingMessage which may the same thing, or may be a JSCompatibleMessageImpl.
    // Locking: from the point that master is set any lock that was in place on the old
    //          value of getMessageLockArtefact() is useless in protecting us from concurrency
    //          issues.  However, code analysis would seem to reveal that this is always used
    //          during the initialization of JSMessageData instances, and therefore immune from
    //          concurrency issues. It is called only by constructors, ownership() (below)
    //          and JSCompatibleMessageImpl.transcribe() which is only called with the lock held.
    //          Although the lock should be held, we take it again anyway because the
    //          method is not private & we're somewhat paranoid.
    void setParent(JMFMessageData parent) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "setParent", new Object[] { parent });

        synchronized (getMessageLockArtefact()) {

            // If the parent is a JSMessageData then this is straight forward
            if (parent instanceof JSMessageData) {
                parentMessage = (JSMessageData) parent;
                master = ((JSMessageData) parent).master;
                containingMessage = ((JSMessageData) parent).compatibilityWrapperOrSelf;
            }

            // ... if not, it must be a JSCompatibleMessageImpl, and its encoding must
            // be a JSMessageData, because if it was a JMFEncapsulation it couldn't be our parent
            else {
                JSMessageData msg = (JSMessageData) ((JSCompatibleMessageImpl) parent).getEncodingMessage();
                parentMessage = msg;
                master = msg.master;
                containingMessage = parent;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "setParent");
    }

    // Set the compatibilityWrapperOrSelf variable to this message (i.e. 'self').
    // Locking: This will only ever be called once, during instantiation,
    //          but we do want to get the value written back to memory without bothering
    //          with declaring it as volatile. The value is only ever accessed with the
    //          lock held, so locking here will do it.
    final void setCompatibilityWrapperToSelf() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "setCompatibilityWrapperToSelf");

        synchronized (getMessageLockArtefact()) {
            compatibilityWrapperOrSelf = this;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "setCompatibilityWrapperToSelf");
    }

    // Set the compatibilityWrapperOrSelf variable to the JSCompatibleMessageImpl which wraps the nessage.
    // Locking: This will only ever be called once, immediately after instantiation,
    //          but we do want to get the value written back to memory without bothering
    //          with declaring it as volatile. The value is only ever accessed with the
    //          lock held, so locking here will do it.
    final void setCompatibilityWrapper(JSCompatibleMessageImpl wrapper) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "setCompatibilityWrapper", wrapper);

        synchronized (getMessageLockArtefact()) {
            compatibilityWrapperOrSelf = wrapper;
            // If we are our own master & e've just been wrapped in a JSCompatibleMessage, then we
            // need to update the master reference.
            if (master == this) {
                setMaster();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "setCompatibilityWrapper");
    }

    // Set the master variable to this message's compatabilityOrSelf value
    // Locking: This will only ever be called once, during instantiation of a top-level JSMessageImpl,
    //          but we do want to get the value written back to memory without bothering
    //          with declaring it as volatile. The value is only ever accessed with the
    //          lock held, so locking here will do it.
    final void setMaster() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "setMaster");

        synchronized (getMessageLockArtefact()) {
            master = compatibilityWrapperOrSelf;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "setMaster");
    }

    // Set the master variable to this message's compatabilityOrSelf value
    // Locking: The lock is expected to be held by the calling stack.
    final boolean isMaster() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "isMaster");

        if (master == compatibilityWrapperOrSelf) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "isMaster", Boolean.TRUE);
            return Boolean.TRUE;
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "isMaster", Boolean.FALSE);
            return Boolean.FALSE;
        }
    }

    // 'Unassemble' this message segment and all parent segments.  This means that the
    // contents are discarded and the cache filled by reading all eligible values from the
    // contents.
    // Locking: a key method that justifies the locking described in the master policy; see
    //          the call to parent.unassemble(). The lock must be held throughout this method.
    @Override
    public void unassemble()
                    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "unassemble");

        // unassemble the containing message, if any
        synchronized (getMessageLockArtefact()) {
            if (containingMessage != null) {
                containingMessage.unassemble();
            }

            // If we have a shared cache it must be copied before any updates are made
            if (sharedCache) {
                copyCache();
            }

            // Next, read all eligible values into the cache.
            for (int i = 0; i < cacheSize; i++) {
                if (cache[i] == null) {
                    JSField field = getFieldDef(i, true);

                    if (field != null) {
                        Object val = ownership(field.decodeValue(contents, getAbsoluteOffset(i), indirect, master), sharedContents);

                        if (val == null) {
                            val = nullIndicator;
                        }

                        cache[i] = val;
                    }
                }
            }

            // Now enter the unassembled state.  If we had a shared assembled buffer we know we're
            // no longer sharing, as we no longer have a buffer.
            contents = null;
            sharedContents = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "unassemble");
    }

    // Establish correct ownership of a JSMessageData or JMFEncapsulation.  This will
    // also set the 'sharedContents' flag if a JSMessageData has been constructed from an
    // already shared assembled buffer.
    // Locking: private method, lock will be held by a method up the calling stack.
    //          The value of parent of the supplied val may be modified by this routine -
    //          this might seem to violate our locking policy, but the usage of ownership() is always to
    //          supply a new 'val' argument by decoding from the 'contents', so there is no need to lock
    //          this new instance, and it logically belongs in the locking scope of this.master.
    private Object ownership(Object val, boolean forceShared) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "ownership", new Object[] { val, Boolean.valueOf(forceShared) });

        if (val instanceof JSMessageData) {
            ((JSMessageData) val).setParent(compatibilityWrapperOrSelf);

            if (forceShared) {
                ((JSMessageData) val).sharedContents = true;
            }
        }
        else if (val instanceof JMFEncapsulation) {
            ((JMFEncapsulation) val).setContainingMessageData(compatibilityWrapperOrSelf);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "ownership", val);
        return val;
    }

    // 'Reallocate' the contents so that there are 'newLen' available bytes at offset
    // instead of 'oldLen' bytes.  Returns the new offset.  This method should only be
    // called when parent==null, in which case the 'this' is a JSMessageImpl and this method
    // is overridden there.  A call to this method when it has not been overridden is an
    // implementation error.
    // Locking: no need to lock, if we get here were in trouble anyway
    int reallocate(int index, int offset, int oldLen, int newLen) {
        throw new IllegalStateException();
    }

    // Implement the JMFMessageData.getValue() method.
    // Locking: There is no need to lock as the work is done by getInternal which
    //          takes the lock itself.
    @Override
    public Object getValue(int index) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "getValue", new Object[] { Integer.valueOf(index) });

        checkIndex(index);

        Object ans = getInternal(index);

        if (ans == null) {
            JMFUninitializedAccessException e = new JMFUninitializedAccessException("Field at index " + index + " is missing");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "getValue", e);
            throw e;
        }

        if (ans == nullIndicator) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "getValue", null);
            return null;
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "getValue", ans);
            return ans;
        }
    }

    // Internal retrieval method used when the index is already known to be valid and
    // UninitializedAcessException should not necessarily be thrown.  Returns null if
    // isPresent would return false.  Returns nullIndicator for a null value that is
    // actually present.
    // Locking: Must lock --- potential for another thread breaking the sharing & copying
    //          the cache, or causing an unassemble between looking in the cache & reading
    //          from the contents.
    Object getInternal(int index)
                    throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "getInternal", new Object[] { Integer.valueOf(index) });

        Object ans;

        synchronized (getMessageLockArtefact()) {
            ans = cache[index];

            // If there is nothing in the cache, but we do have an assembled message,
            // decode the field and put it in the cache.
            if ((ans == null) && (contents != null)) {
                JSField field = getFieldDef(index, true);

                if (field == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        JmfTr.exit(this, tc, "getInternal", null);
                    return null;
                }

                ans = ownership(field.decodeValue(contents, getAbsoluteOffset(index), indirect, master), sharedContents);

                if (ans == null) {
                    ans = nullIndicator;
                }
                // PM68797 - To avoid deadlock, do copyCache() before we create the new object and then adding  
                // to the shared cache. This will avoid the JSMessageData instances (source and target) in a 
                // shared cache being shared between two threads.
                else if (sharedCache && (ans instanceof JSMessageData || ans instanceof JMFEncapsulation)) {
                    copyCache();
                }
                // PM68797 ends

                cache[index] = ans;
            }

            // If we've found the singleton empty unboxed JSVaryingList, we need to replace
            // it with a real one, as someone may be about to update it.
            // We need to update the cache to point to the real one.
            else if (ans == JSVaryingListImpl.EMPTY_UNBOXED_VARYINGLIST) {
                JSField field = getFieldDef(index, false);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    JmfTr.debug(this, tc, "Replacing field " + field + " with real JSVaryingListImpl");
                ans = new JSVaryingListImpl(field, 0, null);
                cache[index] = ans;
            }

            // If we've found the singleton empty unboxed JSFixedList, we need to replace
            // it with a real one, as someone may be about to update it.
            // We need to update the cache to point to the real one.
            else if (ans == JSFixedListImpl.EMPTY_FIXEDLIST) {
                JSField field = getFieldDef(index, false);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    JmfTr.debug(this, tc, "Replacing field " + field + " with real JSFixedListImpl");
                ans = new JSFixedListImpl(field, null);
                cache[index] = ans;
            }

            // If we have a shared cache and the item we are going to return is a JSMessageData
            // subclass, we must break the sharing at this point (rather than waiting until an
            // update is later made) otherwise we risk two or more independent messages
            // referencing the same part.
            // PM68797 - add else for if condition
            else if (sharedCache && (ans instanceof JSMessageData || ans instanceof JMFEncapsulation)) {
                copyCache();
                ans = cache[index];
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "getInternal", ans);
        return ans;
    }

    // Utility to throw an exception if index is out of bounds.  Endeavor to call this as
    // early as possible for every public method that has an index argument and not call it
    // again redundently (of course, if we do call it redundently, it's harmless).
    // Locking: There is no need for the lock to be held as cacheSize is final.
    void checkIndex(int index) {
        if ((index < 0) || (index >= cacheSize)) {
            throw new IndexOutOfBoundsException(String.valueOf(index));
        }
    }

    /**
     * Implement the JMFMessageData.setValue() method.
     * Locking: Essential if the set actually does update the message.
     * Not essential for the first check, as the cache entry is either set or not,
     * and any object we really care about the value of is an immutable.
     */
    @Override
    public void setValue(int index, Object value) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "setValue", new Object[] { Integer.valueOf(index), value });

        checkIndex(index);

        // If the new value is the same as the existing one, don't actually perform the set,
        // as it is cheaper to carry it out these tests than to do an actual set.
        // Original code for determining whether to just return:
        //   if ((value == null) ? (cache[index] == nullIndicator) : ((value instanceof Number || value instanceof Boolean) ? value.equals(cache[index]) : (cache[index] == value))) {
        Object cacheVal = cache[index];
        if (value == null) {
            if (cacheVal == nullIndicator) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    JmfTr.exit(this, tc, "setValue");
                return;
            }
        }
        else if (value instanceof Number || value instanceof Boolean) {
            if (value.equals(cacheVal)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    JmfTr.exit(this, tc, "setValue");
                return;
            }
        }
        else if (value == cacheVal) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "setValue");
            return;
        }

        synchronized (getMessageLockArtefact()) {

            JSField field = getFieldDef(index, false);
            value = ownership(field.validateValue(value, indirect), false);

            if (value instanceof JMFPart && (master != null)) {
                if (this instanceof JSMessageImpl) {
                    invalidateSchemaCache();
                }
                else {
                    getParent().invalidateSchemaCache();
                }
            }

            // If we have a shared cache it must be copied before an update is made
            if (sharedCache) {
                copyCache();
            }

            if (value == null) {
                cache[index] = nullIndicator;
            }
            else {
                cache[index] = value;
            }

            if (assembledForField(index)) {
                int offset = getAbsoluteOffset(index);

                if (isFieldVarying(index)) {
                    int newLen = field.getEncodedValueLength(value, indirect, master) - 4;
                    int oldLen = ArrayUtil.readInt(contents, offset);

                    // A 'null' encoding (which is -1) occupies zero bytes
                    if (oldLen == -1) {
                        oldLen = 0;
                    }

                    JSListCoder.sanityCheck(oldLen, contents, offset);

                    if (newLen != oldLen) {
                        // Changing the size of a varying length field.
                        if (getParent() != null) {
                            // Not top level message segment, so just unassemble and be done with it.
                            unassemble();
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                JmfTr.exit(this, tc, "setValue");
                            return;
                        }
                        else {
                            // Changing size of varying length field at top level is a special case.  Let
                            // JSMessageImpl.reallocate handle this by insertion.
                            offset = reallocate(index, offset, oldLen, newLen);
                        }
                    }
                } // end special handling of varying length fields.

                // If we have shared contents it must be copied before an update is made.
                if (sharedContents) {
                    offset = copyContents(offset);
                }

                // The message may have become unassembled as a result of copying, so we
                // only encode to the buffer if we still have a valid offset.
                if (offset != -1) {
                    field.encodeValue(contents, offset, value, indirect, master);
                }
            }

        } // release lock

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "setValue");
    }

    // Basic implementation of invalidateSchemaCache.
    // There is nothing to actually clear unless this is a JSMessageImpl, but we have to
    // call on up the tree.
    // Locking: The caller is expected to hold the lock.
    void invalidateSchemaCache() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "invalidateSchemaCache");

        // If this is isn't the master, walk on up the tree
        if (!isMaster())
            getParent().invalidateSchemaCache();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "invalidateSchemaCache");
    }

    // Implement isPresent
    // Locking: Needs to lock across the two 'if's, otherwise we could get a false negative
    //         if an unassemble occurs in between them. The call to getFieldDef() needs the lock held too.
    @Override
    public boolean isPresent(int accessor) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "isPresent", new Object[] { Integer.valueOf(accessor) });

        boolean result;
        checkIndex(accessor);

        synchronized (getMessageLockArtefact()) {
            if (cache[accessor] != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    JmfTr.exit(this, tc, "isPresent", Boolean.TRUE);
                return true;
            }

            // If it's not in the cache and the message isn't assembled it can't be present
            if (contents == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    JmfTr.exit(this, tc, "isPresent", Boolean.FALSE);
                return false;
            }

            // For an assembled message, the subclass's getFieldDef method should answer the question.
            result = getFieldDef(accessor, true) != null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "isPresent", Boolean.valueOf(result));
        return result;
    }

    // Implement the expedited get methods
    // Locking: All of these need to hold the lock around at least the 'if (contents != 0)'
    //          block, as contents must not be nulled during processing, and checkPrimitiveType
    //          requires the lock to be held.

    // JMFMessageData.getBoolean
    @Override
    public boolean getBoolean(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "getBoolean", new Object[] { Integer.valueOf(accessor) });
        boolean result;

        synchronized (getMessageLockArtefact()) {
            if (contents != null) {
                checkIndex(accessor);
                checkPrimitiveType(accessor, JMFPrimitiveType.BOOLEAN);
                result = contents[getAbsoluteOffset(accessor)] != ((byte) 0);
            }
            else {
                result = ((Boolean) getValue(accessor)).booleanValue();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "getBoolean", Boolean.valueOf(result));
        return result;
    }

    // JMFMessageData.getByte
    @Override
    public byte getByte(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "getByte", new Object[] { Integer.valueOf(accessor) });
        byte result;

        synchronized (getMessageLockArtefact()) {
            if (contents != null) {
                checkIndex(accessor);
                checkPrimitiveType(accessor, JMFPrimitiveType.BYTE);
                result = contents[getAbsoluteOffset(accessor)];
            }
            else {
                result = ((Byte) getValue(accessor)).byteValue();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "getByte", Byte.valueOf(result));
        return result;
    }

    // JMFMessageData.getShort
    @Override
    public short getShort(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "getShort", new Object[] { Integer.valueOf(accessor) });
        short result;

        synchronized (getMessageLockArtefact()) {
            if (contents != null) {
                checkIndex(accessor);
                checkPrimitiveType(accessor, JMFPrimitiveType.SHORT);
                result = ArrayUtil.readShort(contents, getAbsoluteOffset(accessor));
            }
            else {
                result = ((Short) getValue(accessor)).shortValue();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "getShort", Short.valueOf(result));
        return result;
    }

    // JMFMessageData.getChar
    @Override
    public char getChar(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "getChar", new Object[] { Integer.valueOf(accessor) });
        char result;

        synchronized (getMessageLockArtefact()) {
            if (contents != null) {
                checkIndex(accessor);
                checkPrimitiveType(accessor, JMFPrimitiveType.CHAR);
                result = (char) ArrayUtil.readShort(contents, getAbsoluteOffset(accessor));
            }
            else {
                result = ((Character) getValue(accessor)).charValue();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "getChar", Character.valueOf(result));
        return result;
    }

    // JMFMessageData.getInt
    @Override
    public int getInt(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "getInt", new Object[] { Integer.valueOf(accessor) });
        int result;

        synchronized (getMessageLockArtefact()) {
            if (contents != null) {
                checkIndex(accessor);
                checkPrimitiveType(accessor, JMFPrimitiveType.INT);
                result = ArrayUtil.readInt(contents, getAbsoluteOffset(accessor));
            }
            else {
                result = ((Integer) getValue(accessor)).intValue();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "getInt", Integer.valueOf(result));
        return result;
    }

    // JMFMessageData.getLong
    @Override
    public long getLong(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "getLong", new Object[] { Integer.valueOf(accessor) });
        long result;

        synchronized (getMessageLockArtefact()) {
            if (contents != null) {
                checkIndex(accessor);
                checkPrimitiveType(accessor, JMFPrimitiveType.LONG);
                result = ArrayUtil.readLong(contents, getAbsoluteOffset(accessor));
            }
            else {
                result = ((Long) getValue(accessor)).longValue();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "getLong", Long.valueOf(result));
        return result;
    }

    // JMFMessageData.getFloat
    @Override
    public float getFloat(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "getFloat", new Object[] { Integer.valueOf(accessor) });
        float result;

        synchronized (getMessageLockArtefact()) {
            if (contents != null) {
                checkIndex(accessor);
                checkPrimitiveType(accessor, JMFPrimitiveType.FLOAT);
                result = Float.intBitsToFloat(ArrayUtil.readInt(contents, getAbsoluteOffset(accessor)));
            }
            else {
                result = ((Float) getValue(accessor)).floatValue();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "getFloat", new Float(result));
        return result;
    }

    // JMFMessageData.getDouble
    @Override
    public double getDouble(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "getDouble", new Object[] { Integer.valueOf(accessor) });
        double result;

        synchronized (getMessageLockArtefact()) {
            if (contents != null) {
                checkIndex(accessor);
                checkPrimitiveType(accessor, JMFPrimitiveType.DOUBLE);
                result = Double.longBitsToDouble(ArrayUtil.readLong(contents, getAbsoluteOffset(accessor)));
            }
            else {
                result = ((Double) getValue(accessor)).doubleValue();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "getDouble", new Double(result));
        return result;
    }

    // Do field-specific checks on behalf of the primitive type getters.  Accessor is
    // already validated.  We need to check that the field is supposed to be in the message
    // and that it is of the correct type.
    // Locking: Required, but private & only called by the preceding getXxxx methods who will
    //          already hold the lock.
    private void checkPrimitiveType(int accessor, int typeCode) throws JMFUninitializedAccessException, JMFSchemaViolationException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "checkPrimitiveType", new Object[] { Integer.valueOf(accessor), Integer.valueOf(typeCode) });

        JSField field = getFieldDef(accessor, true);

        if (field == null) {
            JMFUninitializedAccessException e = new JMFUninitializedAccessException("Value at accessor " + accessor + " should not be present");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "checkPrimitiveType", e);
            throw e;
        }

        if (field instanceof JSPrimitive) {
            if (((JSPrimitive) field).getTypeCode() != typeCode) {
                JMFSchemaViolationException e = new JMFSchemaViolationException("Value at accessor " + accessor + " is incorrect type");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    JmfTr.exit(this, tc, "checkPrimitiveType", e);
                throw e;
            }
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    JmfTr.exit(this, tc, "checkPrimitiveType");
                return;
            }
        }
        else if (field instanceof JSEnum && (typeCode == JMFPrimitiveType.INT)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "checkPrimitiveType");
            return;
        }
        else {
            JMFSchemaViolationException e = new JMFSchemaViolationException("Value at accessor " + accessor + " is incorrect");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "checkPrimitiveType", e);
            throw e;
        }
    }

    // The expedited set methods are presently just implemented for convenience and symmetry
    // as it is not obvious that there is any pathlength to be saved by doing anything more
    // complex.
    // Locking: No need to lock any of these as setValue() deals with locking itself.

    // JMFMessageData.setBoolean
    @Override
    public void setBoolean(int index, boolean val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
        setValue(index, Boolean.valueOf(val));
    }

    // JMFMessageData.setByte
    @Override
    public void setByte(int index, byte val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
        setValue(index, Byte.valueOf(val));
    }

    // JMFMessageData.setShort
    @Override
    public void setShort(int index, short val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
        setValue(index, Short.valueOf(val));
    }

    // JMFMessageData.setChar
    @Override
    public void setChar(int index, char val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
        setValue(index, Character.valueOf(val));
    }

    // JMFMessageData.setInt
    @Override
    public void setInt(int index, int val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
        setValue(index, Integer.valueOf(val));
    }

    // JMFMessageData.setLong
    @Override
    public void setLong(int index, long val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
        setValue(index, Long.valueOf(val));
    }

    // JMFMessageData.setFloat
    @Override
    public void setFloat(int index, float val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
        setValue(index, new Float(val));
    }

    // JMFMessageData.setDouble
    @Override
    public void setDouble(int index, double val) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFUninitializedAccessException, JMFMessageCorruptionException {
        setValue(index, new Double(val));
    }

    // Implement JMFMessageData.getNativePart
    // Locking: No need to lock - the only 'interesting code is the call to getDynamic()
    //          and it should take the lock itself.
    @Override
    public JMFNativePart getNativePart(int accessor, JMFSchema schema) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "getNativePart", new Object[] { Integer.valueOf(accessor), schema });

        JMFNativePart ans;
        checkIndex(accessor);

        JMFPart part = getDynamic(accessor);

        if (part instanceof JMFEncapsulation) {
            ans = ((JMFEncapsulation) part).getNativePart();
        }
        else {
            ans = (JMFNativePart) part;
        }

        if (schema == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "getNativePart", ans);
            return ans;
        }

        JMFSchema comp = ans.getJMFSchema();

        if (comp.getID() == schema.getID()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "getNativePart", ans);
            return ans;
        }

        ans = new JSCompatibleMessageImpl((JSchema) schema, ans);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "getNativePart", ans);
        return ans;
    }

    // Subroutine to get a JMFPart from the message, checking for SchemaViolation and
    // UninitializedAccess.  The accessor is valid.
    // Locking: Private method, must take the lock as its callers may not have it.
    private JMFPart getDynamic(int accessor) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "getDynamic", new Object[] { Integer.valueOf(accessor) });

        JMFPart part;

        synchronized (getMessageLockArtefact()) {
            checkDynamic(accessor);
            part = (JMFPart) getInternal(accessor);
        }

        if (part == null) {
            JMFUninitializedAccessException e = new JMFUninitializedAccessException("Dynamic value at accessor " + accessor + " is missing");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "getDynamic", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "getDynamic", part);
        return part;
    }

    // Implement JMFMessageData.getModelID
    // Locking: Needs to lock as it relies on the cache & contents remaining in their current state.
    @Override
    public int getModelID(int accessor) throws JMFUninitializedAccessException, JMFSchemaViolationException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "getModelID", new Object[] { Integer.valueOf(accessor) });
        int ans;
        checkIndex(accessor);

        synchronized (getMessageLockArtefact()) {
            checkDynamic(accessor);

            if (cache[accessor] != null) {
                ans = ((JMFPart) cache[accessor]).getModelID();
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    JmfTr.exit(this, tc, "getModelID", Integer.valueOf(ans));
                return ans;
            }

            if (contents == null) {
                JMFUninitializedAccessException e = new JMFUninitializedAccessException("Dynamic value at accessor " + accessor + " is missing");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    JmfTr.exit(this, tc, "getModelID", e);
                throw e;
            }

            // Get offset of the dynamic field
            int realOffset = getAbsoluteOffset(accessor);

            // Read model Id skipping over length
            ans = ArrayUtil.readInt(contents, realOffset + 4);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "getModelID", Integer.valueOf(ans));
        return ans;
    }

    // Check that the accessor is for an initialized dynamic field.
    // Locking: private method, lock will be held by the caller
    private void checkDynamic(int accessor) throws JMFSchemaViolationException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "checkDynamic", new Object[] { Integer.valueOf(accessor) });

        JSField field = getFieldDef(accessor, false);

        if (field instanceof JSDynamic) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "checkDynamic");
            return;
        }

        JMFSchemaViolationException e = new JMFSchemaViolationException("Field type at accessor " + accessor + " is not Dynamic");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "checkDynamic", e);
        throw e;
    }

    // This method will copy the shared contents buffer to allow an update to
    // be made without affecting any unrelated parts that may still be sharing
    // the buffer.  If the buffer gets reallocated as a result of the copy we
    // return the offset in the new buffer corresponding to the offset provided
    // into the old buffer. If we end up unassembled we return -1.
    // Locking: private method, lock will be held by the caller, i.e. setValue()
    private int copyContents(int offset) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "copyContents", new Object[] { Integer.valueOf(offset) });

        if (sharedContents) {
            // There are two ways to break the contents buffer sharing: either we can
            // copy the entire byte[] buffer or we can unassemble it.
            // Copying the buffer will almost certainly be fastest, but may have a large
            // memory overhead, particularly if the message contains dynamic fields which
            // in turn have large content; in these cases unassembling is probably better
            // as the dynamic fields will be left in their assembled state.
            // In addition, if the JSMessage is not the top level message, a copy of
            // the byte[] buffer will not percolate up to the parent. Therefore we
            // unassemble unless the message is a top-level message and small.
            int newOffset = -1;

            if ((getParent() == null) && (getEncodedLength() < 4096)) {
                newOffset = reallocate(offset);
            }
            else {
                unassemble();
            }

            // No longer sharing
            sharedContents = false;

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "copyContents", Integer.valueOf(newOffset));
            return newOffset;
        }

        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "copyContents", Integer.valueOf(offset));
            return offset;
        }
    }

    // This method will copy the shared cache array to allow an update to be
    // made without affecting any unrelated parts that may still be sharing
    // the cache.
    // Locking: We could assume that callers are constrained to instanceof JSMessageData and
    //          a method in calling stack will have obtained the lock, but this is such a crucial method
    //          that we take belt and braces approach and take the insignificant overhead of obtaining a
    //          lock we almost certainly already possess
    void copyCache() throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "copyCache");

        synchronized (getMessageLockArtefact()) {

            if (sharedCache) {

                // If the cache is null, the new cache would also be null so skip this chunk
                if (cache != null) {
                    Object[] newCache = new Object[cacheSize];

                    for (int i = 0; i < cacheSize; i++) {
                        Object value = cache[i];

                        // If the value isn't any sort of null, get a (lazy)copy of the value in the existing cache
                        if ((value != null) && (value != nullIndicator)) {

                            Object newValue = getFieldDef(i, false).copyValue(value, indirect);

                            // If we're copying an encapsulation we need to ensure it remains in the cache
                            // of its owning message part.  This is safe to do because any subsequent 'getValue'
                            // for an encapsulation field (in fact for any dynamic field) will cause a copy to
                            // be taken early on the get, rather than waiting for a later change.
                            // I.e. the 'parentless' new JMFEncapsulation we put into the 'old' cache will
                            // never call anything which needs a parent - as the other thread will
                            // also take a copy of it, (and go through the else) before anything is called.
                            if (value instanceof JMFEncapsulation && (((JMFEncapsulation) value).getContainingMessageData() == compatibilityWrapperOrSelf)) {
                                cache[i] = newValue;
                                newCache[i] = value;
                            }

                            // If the value is not a JMFEncapsulation, or is a JMFEncapsulation
                            // not owned by us (presumably a parentless one from an earlier copy)
                            // then we set ownership of the new copy & keep that.
                            else {
                                newCache[i] = ownership(newValue, false);
                            }
                        }

                        // If the value is a type of null, then just set it into the new cache
                        else {
                            newCache[i] = value;
                        }
                    }

                    // Now give ourselves the new cache
                    cache = newCache;
                }

                // No longer sharing
                sharedCache = false;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "copyCache");
    }

    // This method is called by subclasses during their getCopy processing to allow
    // us to set up the lazy copy of the message data.
    // Locking: We could assume that callers are constrained to instanceof JSMessageData and
    //          a method in calling stack will have obtained the lock, but this is such a crucial method
    //          that we take belt and braces approach and take the insignificant overhead of obtaining a
    //          lock we almost certainly already possess.  We lock 'original' while
    //          setting up the sharing state.
    void lazyCopy(JSMessageData original) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "lazyCopy", new Object[] { original });

        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                JmfTr.debug(this, tc, "lazyCopy locked dest ", new Object[] { getMessageLockArtefact() });

            // This is the only occasion that we reach out and lock another message instance
            // but we work on the assumption that lazy copying is happening as part of
            // instantiation of a new message instance, so the potential for deadlocking
            // is nil, since no other thread knows about this instance and can not therefore
            // be concurrently supplying 'this' as the 'original' argument in another thread.
            synchronized (original.getMessageLockArtefact()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    JmfTr.debug(this, tc, "lazyCopy locked source ", new Object[] { original.getMessageLockArtefact() });

                // Copy common fields
                indirect = original.indirect;

                // If the message is assembled (i.e. we have a contents buffer) we share both the
                // buffer and the cache between original and copy.
                // If the message is unassembled (no contents buffer) we just share the cache.
                // In either case if a change later needs to be made to the shared portion we
                // will need to copy it before changing, so the shared flags is set to
                // indicate sharing exists between unrelated parts.
                if (original.contents == null) {
                    contents = null;
                    original.sharedCache = true;
                    sharedCache = true;
                    cache = original.cache;
                }
                else {
                    original.sharedContents = true;
                    sharedContents = true;
                    contents = original.contents;
                    original.sharedCache = true;
                    sharedCache = true;
                    cache = original.cache;
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    JmfTr.debug(this, tc, "lazyCopy unlocking source ", new Object[] { original.getMessageLockArtefact() });
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                JmfTr.debug(this, tc, "lazyCopy unlocking dest ", new Object[] { getMessageLockArtefact() });
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "lazyCopy");
    }

    /**
     * Update the cache entry which === "from" to hold "to" instead.
     * Added for d249317
     * This is called by JSCompatibleMessageImpl when an encoding is 'transcribed'.
     * 
     * @param from The value of the cache entry to be replaced
     * @param to The value "from" is to be replace by
     */

    // Locking: We could assume that callers are constrained to instanceof JSMessageData and
    //          a method in calling stack will have obtained the lock, but access control doesnt
    //          guarantee this so we take belt and braces approach and take the insignificant overhead
    //          of obtaining a lock we almost certainly already possess.
    void updateCacheEntry(Object from, Object to) throws JMFSchemaViolationException, JMFModelNotImplementedException, JMFMessageCorruptionException, JMFUninitializedAccessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "updateCacheEntry");

        synchronized (getMessageLockArtefact()) {
            if (cache != null) {
                for (int i = 0; i < cache.length; i++) {
                    if (cache[i] == from) {
                        cache[i] = to;
                    }
                }
            }
            // If we've switched in another sub-message, then the buffer is no longer valid
            unassemble();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "updateCacheEntry");
    }

    // Implement the JMFMessageData.estimateUnassembledValueSize() method.
    // Locking: Take the lock, as we don't want the world to change beneath us.
    @Override
    public int estimateUnassembledValueSize(int index) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "estimateUnassembledValueSize", index);

        int size = 0;

        // We are only interested in 'real' fields, not boxed variants etc
        if (index < cacheSize) {
            checkIndex(index);

            synchronized (getMessageLockArtefact()) {

                // We can only figure out the size if we have a field definition (which presumably should
                // be the case). Have to specifiy false on getFieldDef, otherwise it will return null
                // if the message hasn't been assembled.
                JSField field = getFieldDef(index, false);
                if (field != null) {

                    Object val = cache[index];

                    // If the value is in the cache and isn't something with 0 size
                    if ((val != null)
                        && (val != nullIndicator)
                        && (val != JSVaryingListImpl.EMPTY_UNBOXED_VARYINGLIST)
                        && (val != JSFixedListImpl.EMPTY_FIXEDLIST)) {
                        size = (field.estimateSizeOfUnassembledValue(val, indirect));
                    }

                    // If not, estimate it from the information in the contents, if we have any
                    else if (contents != null) {
                        try {
                            size = (field.estimateSizeOfUnassembledValue(contents, getAbsoluteOffset(index), indirect));
                        } catch (JMFUninitializedAccessException e) {
                            // No FFDC code needed
                            // We don't want to throw an exception or even FFDC it (as we don't
                            // know anything useful to FFDC). We'll just let it default to 0.
                        }
                    }
                }
            }
        }

        // If we have no FieldDef, a nullIndicator or no contents, then we will just return 0;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "estimateUnassembledValueSize", size);
        return size;
    }

    // These vacuous methods permit JSMessageData to extend AbstractList without
    // requiring all subclasses to implement list behavior.  Only the JSListImpl classes
    // implement list behavior; JSMessageImpl does not.
    // However, in case FFDC (or anyone else) insists on treating the class as a List,
    // we'll reutn 0 for size() & ArrayOutOfBounds for get()
    @Override
    public Object get(int i) {
        throw new ArrayIndexOutOfBoundsException();
    }

    @Override
    public int size() {
        return 0;
    }

    /*
     * All the inherited methods of AbstractList/AbstractCollection are overridden here.
     * Locking: All the inherited methods of AbstractList/AbstractCollection have been made threadsafe
     * though they may not need to be. They are not called much, if at all, so
     * it is unlikely to matter.
     */
    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#add(int, java.lang.Object)
     */
    @Override
    public void add(int arg0, Object arg1) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "add", new Object[] { Integer.valueOf(arg0), arg1 });

        synchronized (getMessageLockArtefact()) {
            super.add(arg0, arg1);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "add");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#add(java.lang.Object)
     */
    @Override
    public boolean add(Object arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "add", new Object[] { arg0 });

        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "add");
            return super.add(arg0);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    @Override
    public boolean addAll(int arg0, Collection arg1) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "addAll", new Object[] { Integer.valueOf(arg0), arg1 });

        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "addAll");
            return super.addAll(arg0, arg1);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#clear()
     */
    @Override
    public void clear() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "clear");
        synchronized (getMessageLockArtefact()) {
            super.clear();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "clear");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.AbstractList#equals(java.util.AbstractList)
     */
    @Override
    public boolean equals(Object arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "equals", new Object[] { arg0 });
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "equals");
            return super.equals(arg0);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.AbstractList#hashCode(java.util.AbstractList)
     */
    @Override
    public int hashCode() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "hashCode");
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "hashCode");
            return super.hashCode();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#indexOf(java.lang.Object)
     */
    @Override
    public int indexOf(Object arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "indexOf", new Object[] { arg0 });
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "indexOf");
            return super.indexOf(arg0);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#iterator()
     */
    @Override
    public Iterator iterator() {
        // dont trace
        synchronized (getMessageLockArtefact()) {
            return super.iterator();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    @Override
    public int lastIndexOf(Object arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "lastIndexOf", new Object[] { arg0 });
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "lastIndexOf");
            return super.lastIndexOf(arg0);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#listIterator()
     */
    @Override
    public ListIterator listIterator() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "listIterator");
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "listIterator");
            return super.listIterator();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#listIterator(int)
     */
    @Override
    public ListIterator listIterator(int arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "listIterator", new Object[] { Integer.valueOf(arg0) });
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "listIterator");
            return super.listIterator(arg0);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#remove(int)
     */
    @Override
    public Object remove(int arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "remove", new Object[] { Integer.valueOf(arg0) });
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "remove");
            return super.remove(arg0);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.AbstractList#removeRange(int, int)
     */
    @Override
    protected void removeRange(int arg0, int arg1) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "removeRange", new Object[] { Integer.valueOf(arg0), Integer.valueOf(arg1) });
        synchronized (getMessageLockArtefact()) {
            super.removeRange(arg0, arg1);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.exit(this, tc, "removeRange");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#set(int, java.lang.Object)
     */
    @Override
    public Object set(int arg0, Object arg1) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "set", new Object[] { Integer.valueOf(arg0), arg1 });
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "set");
            return super.set(arg0, arg1);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.List#subList(int, int)
     */
    @Override
    public List subList(int arg0, int arg1) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "subList", new Object[] { Integer.valueOf(arg0), Integer.valueOf(arg1) });
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "subList");
            return super.subList(arg0, arg1);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#addAll(java.util.Collection)
     */
    @Override
    public boolean addAll(Collection arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "addAll", new Object[] { arg0 });
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "addAll");
            return super.addAll(arg0);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#contains(java.lang.Object)
     */
    @Override
    public boolean contains(Object arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "contains", new Object[] { arg0 });
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "contains");
            return super.contains(arg0);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#containsAll(java.util.Collection)
     */
    @Override
    public boolean containsAll(Collection arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "containsAll", new Object[] { arg0 });
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "containsAll");
            return super.containsAll(arg0);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "isEmpty");
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "isEmpty");
            return super.isEmpty();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#remove(java.lang.Object)
     */
    @Override
    public boolean remove(Object arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "remove", new Object[] { arg0 });
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "remove");
            return super.remove(arg0);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#removeAll(java.util.Collection)
     */
    @Override
    public boolean removeAll(Collection arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "removeAll", new Object[] { arg0 });
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "removeAll");
            return super.removeAll(arg0);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#retainAll(java.util.Collection)
     */
    @Override
    public boolean retainAll(Collection arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "retainAll", new Object[] { arg0 });
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "retainAll");
            return super.retainAll(arg0);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#toArray()
     */
    @Override
    public Object[] toArray() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "toArray");
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "toArray");
            return super.toArray();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#toArray(java.lang.Object[])
     */
    @Override
    public Object[] toArray(Object[] arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            JmfTr.entry(this, tc, "toArray", new Object[] { arg0 });
        synchronized (getMessageLockArtefact()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                JmfTr.exit(this, tc, "toArray");
            return super.toArray(arg0);
        }
    }

    // This basic method introduced because of the odd inheritance hierarchy we have
    // where JSMessageData is-a-kind-of AbstractCollection but refuses to support size()
    // so AbstractCollections toString() method fails.  This could be improved
    // but this is one step better than getting "<malformed parameter>" out of trace dumps.
    @Override
    public String toString() {
        String result = getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this));
        return result;
    }

    @Override
    public String toTraceString() {
        return toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.ffdc.FFDCSelfIntrospectable.introspectSelf()
     */
    @Override
    public String[] introspectSelf() {
        String[] debugInfo = new String[6];
        debugInfo[0] = "JSMessageData:";

        // Include the basic toString info so we know who we are
        StringBuilder sb = new StringBuilder("Self: ");
        sb.append(toString());
        debugInfo[1] = new String(sb);

        // Include the cache information
        sb.setLength(0);
        sb.append("Cache: size=");
        sb.append(cacheSize);
        sb.append(" cache=");
        sb.append(cache); // Yes we really do just want [Ljava.lang.Object;@xxxxxx here
        debugInfo[2] = new String(sb);

        // Include the cache information
        sb.setLength(0);
        sb.append("Contents: ");
        if (contents == null) {
            sb.append("null");
        }
        else {
            sb.append("size=");
            sb.append(contents.length);
            sb.append(" contents=");
            sb.append(contents); // Yes we really do just want [B@xxxxxxx here
        }
        debugInfo[3] = new String(sb);

        // Include sharing information
        sb.setLength(0);
        sb.append("Sharing: sharedCache=");
        sb.append(sharedCache);
        sb.append(" sharedContents=");
        sb.append(sharedContents);
        debugInfo[4] = new String(sb);

        // Include all the stuff abot how it fits in to the heirarchy
        sb.setLength(0);
        sb.append("Context: parentMessage=");
        sb.append(parentMessage);
        sb.append(" containingMessage=");
        sb.append(containingMessage);
        sb.append(" master=");
        if (master == this) {
            sb.append("self");
        }
        else {
            sb.append(master);
        }
        sb.append(" compatibilityWrapperOrSelf=");
        if (compatibilityWrapperOrSelf == this) {
            sb.append("self");
        }
        else {
            sb.append(compatibilityWrapperOrSelf);
        }
        debugInfo[5] = new String(sb);

        // Now return it all
        return debugInfo;
    }
}
