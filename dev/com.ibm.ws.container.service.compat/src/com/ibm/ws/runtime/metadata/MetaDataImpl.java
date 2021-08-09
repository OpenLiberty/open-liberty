/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.runtime.metadata;

import java.util.Arrays;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public abstract class MetaDataImpl implements MetaData {

    private final static TraceComponent tc = Tr.register(MetaDataImpl.class, "Runtime");

    /**
     * Allocate space for "fast" slots in each metadata object. If more slots are allocated, the extra slots will all require "slow" synchronization for each get/set.
     */
    private static final int NUM_STATIC_SLOTS = 4;

    static final int ID_UNINITIALIZED = -1;

    /**
     * Each MetaDataImpl is tracked by its MetaDataManager so that slot data can be cleared when a slot is destroyed before the MetaDataImpl. The id tracks the MetaDataImpl
     * position in an array. An alternative implementation might treat MetaDataImpl as doubly-linked list nodes, but that causes confusing object relationships in heap dumps.
     */
    int id = ID_UNINITIALIZED;

    /**
     * The metadata interface class of this object for diagnostics.
     */
    private final Class<? extends MetaData> metaDataInterface;

    /**
     * The data stored for slots with ids less than {@link #NUM_STATIC_SLOTS}. The array should be indexed by the slot id.
     */
    private final Object[] staticSlots = new Object[NUM_STATIC_SLOTS];

    /**
     * The data stored for slots with ids greater than or equal to {@link #NUM_STATIC_SLOTS}. The array should be indexed by the slot id minus {@link #NUM_STATIC_SLOTS}. This field
     * is lazily initialized by {@link #setMetaData}. The monitor for {@link #staticSlots} must be held while accessing this field.
     */
    private Object[] dynamicSlots;

    /**
     * This constructor exists for compatibility with tWAS.
     * 
     * @param slotCnt unused; subclasses should pass 0
     */
    public MetaDataImpl(int slotCnt) {
        // WebModuleMetaData also implements ComponentMetaData, so we must
        // check for ModuleMetaData before ComponentMetaData. 
        if (this instanceof ApplicationMetaData) {
            metaDataInterface = ApplicationMetaData.class;
        } else if (this instanceof ModuleMetaData) {
            metaDataInterface = ModuleMetaData.class;
        } else if (this instanceof ComponentMetaData) {
            metaDataInterface = ComponentMetaData.class;
        } else if (this instanceof MethodMetaData) {
            metaDataInterface = MethodMetaData.class;
        } else {
            throw new IllegalStateException("invalid metadata type");
        }
    }

    @Override
    public String toString() {
        String string = super.toString();
        if (metaDataInterface == ApplicationMetaData.class) {
            string += "[" + ((ApplicationMetaData) this).getJ2EEName() + ']';
        } else if (metaDataInterface == ModuleMetaData.class) {
            string += "[" + ((ModuleMetaData) this).getJ2EEName() + ']';
        } else if (metaDataInterface == ComponentMetaData.class) {
            string += "[" + ((ComponentMetaData) this).getJ2EEName() + ']';
        } else if (metaDataInterface == MethodMetaData.class) {
            MethodMetaData thisMethod = (MethodMetaData) this;
            ComponentMetaData cmd = thisMethod.getComponentMetaData();
            string += "[" + (cmd == null ? null : cmd.getJ2EEName()) + ", " + thisMethod.getName() + ']';
        }
        return string;
    }

    private MetaDataSlotImpl getMetaDataSlotImpl(MetaDataSlot slot) {
        MetaDataSlotImpl slotImpl = (MetaDataSlotImpl) slot;

        Class<?> slotInterface = slotImpl.metadataIntf;
        if (slotInterface != metaDataInterface) {
            IllegalArgumentException iae = new IllegalArgumentException(slotInterface + " slot for " + metaDataInterface + ": " + getClass().getName());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "checkSlotAccess", iae);
            throw iae;
        }

        return slotImpl;
    }

    @Override
    public void setMetaData(MetaDataSlot slot, Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setMetaData(this=" + this + ", slot=" + slot + ", metadata=" + Util.identity(value) + ")");

        MetaDataSlotImpl slotImpl = getMetaDataSlotImpl(slot);

        // Synchronize with slot destroy.
        synchronized (slot) {
            if (slotImpl.destroyed) {
                throw new IllegalStateException();
            }

            setMetaData(slotImpl, value, false);
        }
    }

    void setMetaData(MetaDataSlotImpl slotImpl, Object value, boolean destroy) {
        int slotId = slotImpl.id;
        if (slotId < NUM_STATIC_SLOTS) {
            staticSlots[slotId] = value;
        } else {
            int dynamicSlotId = slotId - NUM_STATIC_SLOTS;
            synchronized (staticSlots) {
                Object[] dynamicSlots = this.dynamicSlots;
                if (dynamicSlots == null) {
                    if (!destroy) {
                        // Create an array large enough for the slot id, but
                        // create at least NUM_STATIC_SLOTS.
                        dynamicSlots = new Object[Math.max(roundUpPowerOfTwo(dynamicSlotId), NUM_STATIC_SLOTS)];
                        this.dynamicSlots = dynamicSlots;
                        dynamicSlots[dynamicSlotId] = value;
                    }
                } else if (dynamicSlotId >= dynamicSlots.length) {
                    if (!destroy) {
                        dynamicSlots = Arrays.copyOf(dynamicSlots, roundUpPowerOfTwo(dynamicSlotId));
                        this.dynamicSlots = dynamicSlots;
                        dynamicSlots[dynamicSlotId] = value;
                    }
                } else {
                    dynamicSlots[dynamicSlotId] = value;
                }
            }
        }
    }

    private static int roundUpPowerOfTwo(int n) {
        return Integer.highestOneBit(n) << 1;
    }

    @Override
    public Object getMetaData(MetaDataSlot slot) {
        Object value = getMetaData(getMetaDataSlotImpl(slot));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getMetaData(this=" + this + ", slot=" + slot + ") --> " + value);
        return value;
    }

    Object getMetaData(MetaDataSlotImpl slot) {
        int slotId = slot.id;

        Object value;
        if (slotId < NUM_STATIC_SLOTS) {
            value = staticSlots[slotId];
        } else {
            int dynamicSlotId = slotId - NUM_STATIC_SLOTS;
            synchronized (staticSlots) {
                if (dynamicSlots == null || dynamicSlotId >= dynamicSlots.length) {
                    value = null;
                } else {
                    value = dynamicSlots[dynamicSlotId];
                }
            }
        }

        // Ensure that the slot wasn't destroyed after reading the slot data or
        // else we might return another slot's data.
        if (slot.destroyed) {
            throw new IllegalStateException();
        }

        return value;
    }

    // This method exists on MetaData for compatibility only.  Add a trivial
    // implementation for convenience.
    @Override
    public void release() {}

}
