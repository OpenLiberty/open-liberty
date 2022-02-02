/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.concurrent.context;

import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.threadcontext.ThreadContext;

import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.spi.ThreadContextProvider;
import jakarta.enterprise.concurrent.spi.ThreadContextRestorer;
import jakarta.enterprise.concurrent.spi.ThreadContextSnapshot;

/**
 * Combination of all third-party context.
 */
public class ThirdPartyContext implements ThreadContext {
    private static final TraceComponent tc = Tr.register(ThirdPartyContext.class);

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1;

    /**
     * Names of serializable fields.
     * A single character is used for each to reduce the space required.
     */
    static final String CLEARED = "C",
                    PROPAGATED = "P",
                    UNCHANGED = "U",
                    REMAINING = "R";

    /**
     * Fields to serialize.
     */

    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[] {
                                                                                                new ObjectStreamField(CLEARED, List.class),
                                                                                                new ObjectStreamField(PROPAGATED, List.class),
                                                                                                new ObjectStreamField(UNCHANGED, List.class),
                                                                                                new ObjectStreamField(REMAINING, String.class)
    };

    transient List<String> cleared, propagated, unchanged;
    transient boolean isAnyPropagated;
    transient String remaining;
    transient ArrayList<ThreadContextRestorer> restorers;
    transient ArrayList<ThreadContextSnapshot> snapshots;

    /**
     * Constructor with empty configuration for cloning.
     */
    private ThirdPartyContext() {
    }

    /**
     * Constructor for when all third-party context is to be cleared.
     *
     * @param coordinator coordinates all third-party context types
     * @param execProps   execution properties
     */
    ThirdPartyContext(ThirdPartyContextCoordinator coordinator, Map<String, String> execProps) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        cleared = propagated = unchanged = Collections.emptyList();
        remaining = "cleared";

        snapshots = new ArrayList<ThreadContextSnapshot>();
        for (ThreadContextProvider provider : coordinator.getProviders()) {
            ThreadContextSnapshot snapshot = provider.clearedContext(execProps);
            snapshots.add(snapshot);
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "will clear " + provider.getThreadContextType() + " context " + snapshot);
        }
    }

    /**
     * Constructor that follows configuration for cleared/propagated/unchanged.
     *
     * @param coordinator         coordinates all third-party context types
     * @param execProps           execution properties
     * @param threadContextConfig configuration for context providers. Null to indicate that all third-party types should be cleared
     */
    @SuppressWarnings("unchecked")
    ThirdPartyContext(ThirdPartyContextCoordinator coordinator, Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        cleared = (List<String>) threadContextConfig.get("cleared");
        propagated = (List<String>) threadContextConfig.get("propagated");
        unchanged = (List<String>) threadContextConfig.get("unchanged");
        remaining = (String) threadContextConfig.get("remaining");

        snapshots = new ArrayList<ThreadContextSnapshot>();
        for (ThreadContextProvider provider : coordinator.getProviders()) {
            String type = provider.getThreadContextType();
            String action = Collections.binarySearch(cleared, type) >= 0 ? "cleared" : //
                            Collections.binarySearch(propagated, type) >= 0 ? "propagated" : //
                                            Collections.binarySearch(unchanged, type) >= 0 ? "unchanged" : //
                                                            remaining;

            if ("cleared".equals(action)) {
                ThreadContextSnapshot snapshot = provider.clearedContext(execProps);
                snapshots.add(snapshot);
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "will clear " + type + " context " + snapshot);
            } else if ("propagated".equals(action)) {
                ThreadContextSnapshot snapshot = provider.currentContext(execProps);
                snapshots.add(snapshot);
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "will propagate " + type + " context " + snapshot);
                isAnyPropagated = true;
            } else {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "will ignore " + type + " context");
            }
        }
    }

    @Override
    public ThirdPartyContext clone() {
        ThirdPartyContext clone = new ThirdPartyContext();
        clone.cleared = cleared;
        clone.propagated = propagated;
        clone.unchanged = unchanged;
        clone.remaining = remaining;
        clone.snapshots = snapshots;
        // clone.restorers is left null because the clone will track its own application/removal of the context
        return clone;
    }

    /**
     * This method is invoked after deserialization to initializes the context types to clear.
     *
     * @param coordinator coordinates all third-party context types
     * @param execProps   execution properties
     */
    void initPostDeserialize(ThirdPartyContextCoordinator coordinator, Map<String, String> execProps) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        snapshots = new ArrayList<ThreadContextSnapshot>();
        for (ThreadContextProvider provider : coordinator.getProviders()) {
            // Context types that were previously unavailable to be captured for propagation must be cleared if available now.
            String type = provider.getThreadContextType();
            boolean clear = Collections.binarySearch(cleared, type) >= 0 || //
                            Collections.binarySearch(propagated, type) >= 0 || //
                            Collections.binarySearch(unchanged, type) < 0 && !"unchanged".equals(remaining);

            if (clear) {
                ThreadContextSnapshot snapshot = provider.clearedContext(execProps);
                snapshots.add(snapshot);
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "will clear " + type + " context " + snapshot);
            } else {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "will ignore " + type + " context");
            }
        }
    }

    @Override
    @Trivial
    public boolean isSerializable() {
        UnsupportedOperationException x = null;
        if (isAnyPropagated) {
            List<String> unser = new ArrayList<String>(propagated);
            if ("propagated".equals(remaining))
                unser.add(ContextServiceDefinition.ALL_REMAINING);

            x = new UnsupportedOperationException("Thread context configured for propagation is not serializable: " + unser); // TODO NLS
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "isSerializable?", propagated, "remaining=" + remaining, x == null ? true : x);
        if (x == null)
            return true;
        else
            throw x;
    }

    @Override
    public void taskStarting() throws RejectedExecutionException {
        // TODO will Transaction context be handled here or elsewhere? Currently it will be in the third-party context list even though it isn't third-party
        restorers = new ArrayList<ThreadContextRestorer>();
        try {
            for (ThreadContextSnapshot snapshot : snapshots)
                restorers.add(snapshot.begin());
        } catch (Throwable x) {
            taskStopping();
            throw new RejectedExecutionException(x);
        }
    }

    @Override
    public void taskStopping() {
        for (int i = restorers.size(); --i >= 0;)
            try {
                restorers.remove(i).endContext();
            } catch (Throwable x) {
            }
    }

    @Override
    @Trivial
    public String toString() {
        StringBuilder sb = new StringBuilder(100).append(getClass().getSimpleName()).append('@').append(Integer.toHexString(hashCode())) //
                        .append(" cleared=").append(cleared) //
                        .append(" propagated=").append(propagated) //
                        .append(" unchanged=").append(unchanged) //
                        .append(" with remaining ").append(remaining) //
                        .append(" ").append(snapshots);
        return sb.toString();
    }

    /**
     * Reads and deserializes from the input stream.
     *
     * @param in The object input stream from which to deserialize.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        GetField fields = in.readFields();
        cleared = (List<String>) fields.get(CLEARED, Collections.EMPTY_LIST);
        propagated = (List<String>) fields.get(PROPAGATED, Collections.EMPTY_LIST);
        unchanged = (List<String>) fields.get(UNCHANGED, Collections.EMPTY_LIST);
        remaining = (String) fields.get(REMAINING, cleared);

        // ThirdPartyContextCoordinator.deserializeThreadContext continues initializing this
        // instance after the readObject method ends.
    }

    /**
     * Serialize the given object.
     *
     * @param outStream The stream to write the serialized data.
     *
     * @throws IOException
     * @throws UnsupportedOperationException if not serializable
     */
    private void writeObject(ObjectOutputStream outStream) throws IOException {
        if (!isSerializable())
            throw new UnsupportedOperationException();

        PutField fields = outStream.putFields();
        fields.put(CLEARED, cleared);
        fields.put(PROPAGATED, propagated);
        fields.put(UNCHANGED, unchanged);
        fields.put(REMAINING, remaining);
        outStream.writeFields();
    }
}