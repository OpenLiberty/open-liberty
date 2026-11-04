/*
 * Copyright 2024 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.yoko.orb.CosNaming.tnaming2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.yoko.orb.CosNaming.tnaming2.NamingContextImpl.BoundObject;
import org.apache.yoko.orb.spi.naming.RemoteAccess;
import org.omg.CORBA.LocalObject;
import org.omg.CosNaming.Binding;
import org.omg.CosNaming.BindingHolder;
import org.omg.CosNaming.BindingIterator;
import org.omg.CosNaming.BindingIteratorPOA;
import org.omg.CosNaming.BindingListHolder;
import org.omg.CosNaming.BindingType;
import org.omg.CosNaming.NameComponent;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;

public final class BindingIteratorImpl extends LocalObject implements BindingIterator, RemotableObject {
    private static final long serialVersionUID = 1L;

    private static final class Core extends BindingIteratorPOA {
        private static final AtomicLong NEXT_ID = new AtomicLong();
        private final long instanceId = NEXT_ID.getAndIncrement();
        // the iterator use to access the bindings
        private final Iterator<BoundObject> iterator;

        private static final NameComponent[] ZERO_NC_ARRAY = new NameComponent[0];
        /**
         * Create a new BindingIterator to iterate over the given boundObjects.
         * @param boundObjects The bound objects over which to iterate.
         */
        public Core(Collection<BoundObject> boundObjects) {
            this.iterator = (new ArrayList<BoundObject>(boundObjects)).iterator();
        }

        private byte[] getServantId() {
            return ("BindingIterator#" + instanceId).getBytes();
        }

        /**
         * Return the next object in the iteration sequence.
         * @param b The BindingHolder used to return the next item. If we've
         *            reached the end of the sequence, an item with an empty
         *            name is returned.
         * @return true if there is another item, false otherwise.
         */
        public boolean next_one(BindingHolder b) {
            if (iterator.hasNext()) {
                // return this as a Binding value.
                BoundObject obj = iterator.next();
                b.value = new Binding(new NameComponent[]{obj.name}, obj.type);
                return true;
            } else {
                // return an empty element
                b.value = new Binding(ZERO_NC_ARRAY, BindingType.nobject);
                return false;
            }
        }

        /**
         * Retrieve the next "n" items from the list, returned as a BindingList.
         * @param how_many The count of items to retrieve.
         * @param bl A holder for returning an array of Bindings for the
         *            returned items.
         * @return true if any items were returned, false if there's nothing left
         *         to return.
         */
        public boolean next_n(int how_many, BindingListHolder bl) {
            List<Binding> accum = new ArrayList<Binding>();
            BindingHolder holder = new BindingHolder();
            int i = 0;
            // Keep iterating as long as there are entries
            while (i < how_many && next_one(holder)) {
                accum.add(holder.value);
                i++;
            }

            // convert to an array and return whether we found anything.
            bl.value = accum.toArray(new Binding[accum.size()]);
            return accum.isEmpty();
        }

        /**
         * Destroy this BindingIterator instance
         */
        public void destroy() {
        }
    }

    private static final class POAServant extends BindingIteratorPOA {
        // the POA used to activate this object (required for destroy();
        private final POA poa;
        private final Core core;

        public POAServant(POA poa, Core core) throws Exception {
            this.poa = poa;
            this.core = core;
            poa.activate_object_with_id(core.getServantId(), this);
        }

        /** References must come from the POA that activated this servant, not the root POA. */
        @Override
        public POA _default_POA() {
            return poa;
        }

        /**
         * Return the next object in the iteration sequence.
         * @param b The BindingHolder used to return the next item. If we've
         *            reached the end of the sequence, an item with an empty
         *            name is returned.
         * @return true if there is another item, false otherwise.
         */
        @Override
        public boolean next_one(org.omg.CosNaming.BindingHolder b) {
            return core.next_one(b);
        }

        /**
         * Retrieve the next "n" items from the list, returned as a BindingList.
         * @param how_many The count of items to retrieve.
         * @param bl A holder for returning an array of Bindings for the
         *            returned items.
         * @return true if any items were returned, false if there's nothing left
         *         to return.
         */
        @Override
        public boolean next_n(int how_many, BindingListHolder bl) {
            return core.next_n(how_many, bl);
        }

        /**
         * Destroy this BindingIterator instance, which deactivates it from the
         * hosting POA.
         */
        @Override
        public void destroy() {
            try {
                // we need to deactivate this from the POA.
                byte[] objectId = poa.servant_to_id(this);
                if (objectId != null) {
                    poa.deactivate_object(objectId);
                }
            } catch (Exception e) {
            }
        }
    }

    private final Core core;

    public BindingIteratorImpl(final Collection<BoundObject> boundObjects) {
        assert boundObjects != null;
        core = new Core(boundObjects);
    }

    @Override
    public boolean next_one(BindingHolder b) {
        assert b != null;
        return core.next_one(b);
    }

    @Override
    public boolean next_n(int how_many, BindingListHolder bl) {
        assert bl != null;
        return core.next_n(how_many, bl);
    }

    @Override
    public void destroy() {
        core.destroy();
    }

    @Override
    public Servant getServant(POA poa, RemoteAccess ignored) throws Exception {
        return new POAServant(poa, core);
    }
}
