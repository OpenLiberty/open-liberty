/*
 * Copyright 2017 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.yoko.orb.spi.naming.RemoteAccess;
import org.apache.yoko.util.UnmodifiableEnumMap;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.SystemException;
import org.omg.CosNaming.BindingIteratorHelper;
import org.omg.CosNaming.BindingIteratorHolder;
import org.omg.CosNaming.BindingListHolder;
import org.omg.CosNaming.BindingType;
import org.omg.CosNaming.BindingTypeHolder;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.CosNaming.NamingContextExtPackage.InvalidAddress;
import org.omg.CosNaming.NamingContextPackage.AlreadyBound;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotEmpty;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.POAPackage.ObjectNotActive;

public final class NamingContextImpl extends LocalObject implements NamingContextExt, RemotableObject {
    private static final long serialVersionUID = 1L;

    private static final class ServantCreationLock {
    }

    private static final class Core extends NamingContextBase {
        private static final AtomicLong NEXT_ID = new AtomicLong();

        /** Unique number for this core */
        private final long instanceId = NEXT_ID.getAndIncrement();

        /** the unique ids for this context's servants (one per remote access level) */
        @SuppressWarnings("serial")
        private final Map<RemoteAccess, String> servantIds = new UnmodifiableEnumMap<RemoteAccess, String>(RemoteAccess.class) {
            public String computeValueFor(RemoteAccess key) {
                return "NamingContext#" + instanceId + "$" + key;
            }
        };

        /** the bindings maintained by this context */
        private final HashMap<BindingKey, BoundObject> bindings = new HashMap<BindingKey, BoundObject>();

        /** the root context object */
        private final org.omg.CORBA.Object rootContext;

        private Core(org.omg.CORBA.Object rootContext) throws Exception {
            this.rootContext = rootContext;
        }

        /**
         * Get the servant id to use for this context with the specified remote
         * access level
         */
        private byte[] getServantId(RemoteAccess access) {
            return servantIds.get(access).getBytes();
        }

        // abstract methods part of the interface contract that the
        // implementation is required
        // to supply.

        /**
         * Create a new context of the same type as the calling context.
         * @return A new NamingContext item.
         */
        @Override
        public NamingContext new_context() {
            try {
                // create a new context.
                NamingContextImpl newContext = new NamingContextImpl(rootContext);

                return newContext;
            } catch (SystemException e) {
                // just propagate system exceptions
                throw e;
            } catch (Exception e) {
                throw (INTERNAL) (new INTERNAL("Unable to create new naming context").initCause(e));
            }
        }

        /**
         * Destroy a context. This method should clean up any backing resources
         * associated with the context.
         */
        @Override
        public synchronized void destroy() throws NotEmpty {
            // still holding bound objects? Not allowed to destroy
            if (!bindings.isEmpty()) {
                throw new NotEmpty();
            }
        }

        /**
         * Create a list of bound objects an contexts contained within this
         * context.
         * @param how_many The count of elements to return as a BindingList.
         * @param bl A holder element for returning the source binding list.
         * @param bi A holder for returning a BindingIterator. Any extra
         *            elements not returned in the BindingList are returned in
         *            the BindingIterator.
         */
        @Override
        public synchronized void list(int how_many, BindingListHolder bl, BindingIteratorHolder bi) {
            BindingIteratorImpl iterator = new BindingIteratorImpl(bindings.values());
            // have the iterator fill in the entries here
            iterator.next_n(how_many, bl);
            bi.value = iterator;
        }

        // lower level functions that are used by the base class

        /**
         * Resolve an object in this context (single level resolution).
         * @param n The name of the target object.
         * @param type A type holder for returning the bound object type
         *            information.
         * @return The bound object. Returns null if the object does not exist
         *         in the context.
         */
        @Override
        protected org.omg.CORBA.Object resolveObject(NameComponent n, BindingTypeHolder type) {
            // special call to resolve the root context. This is the only one
            // that goes backwards.
            if (n.id.length() == 0 && n.kind.length() == 0) {
                // this is a name context item, so set it properly.
                type.value = BindingType.ncontext;
                return rootContext;
            }

            BindingKey key = new BindingKey(n);
            BoundObject obj = (BoundObject) bindings.get(key);
            // if not in the table, just return null
            if (obj == null) {
                return null;
            }
            // update the type information and return the bound object
            // reference.
            type.value = obj.type;
            return obj.boundObject;
        }

        /**
         * Bind an object into the current context. This can be either an object
         * or a naming context.
         * @param n The single-level name of the target object.
         * @param obj The object or context to be bound.
         * @param type
         */
        @Override
        protected void bindObject(NameComponent n, org.omg.CORBA.Object obj, BindingTypeHolder type) {
            // fairly simple table put...
            bindings.put(new BindingKey(n), new BoundObject(n, obj, type.value));
        }

        /**
         * Unbind an object from the current context.
         * @param n The name of the target object (single level).
         * @return The object associated with the binding. Returns null if there
         *         was no binding currently associated with this name.
         */
        @Override
        protected org.omg.CORBA.Object unbindObject(NameComponent n) {
            // remove the object from the hash table, returning the bound object
            // if it exists.
            BindingKey key = new BindingKey(n);
            BoundObject obj = (BoundObject) bindings.remove(key);

            if (obj != null) {
                return obj.boundObject;
            }
            return null;
        }

        /**
         * Internal class used for HashMap lookup keys.
         */
        private static final class BindingKey {
            // the name component this is a HashMap key for.
            private final NameComponent name;
            private final int hashval;

            /**
             * Create a new BindingKey for a NameComponent.
             * @param n The lookup name.
             */
            public BindingKey(NameComponent n) {
                name = n;
                hashval = Objects.hashCode(name.id) + Objects.hashCode(name.kind);
            }

            /**
             * Return the hashcode associated with this binding key. The
             * hashcode is created using the NameComponent id and kind fields.
             * @return The lookup hashvalue associated with this key.
             */
            @Override
            public int hashCode() {
                return hashval;
            }

            /**
             * Compare two BindingKeys for equality (used for HashMap lookups).
             * @param other The comparison partner.
             * @return True if the keys are equivalent, false otherwise.
             */
            @Override
            public boolean equals(Object other) {
                // if not given or the wrong type, this is false.
                if (!!!(other instanceof BindingKey)) {
                    return false;
                }

                BindingKey otherKey = (BindingKey) other;

                return (Objects.equals(name.id, otherKey.name.id) && Objects.equals(name.kind, otherKey.name.kind));
            }

            @Override
            public String toString() {
                return "" + name;
            }
        }

    }

    private static abstract class POAServant extends NamingContextBase {
        static POAServant create(NamingContextImpl localContext, Core core, POA poa, RemoteAccess remoteAccess) throws Exception {
            switch (remoteAccess) {
                case readOnly :
                    return new ReadOnly(localContext, core, poa);
                case readWrite :
                    return new ReadWrite(localContext, core, poa);
                default :
                    throw new IllegalArgumentException("Unsupported remote access type: " + remoteAccess);
            }
        }

        final Object localContext;
        final NamingContextBase core;
        final POA poa;

        protected POAServant(NamingContextImpl localContext, Core core, POA poa, byte[] servantId) throws Exception {
            this.localContext = localContext;
            this.core = core;
            this.poa = poa;
            poa.activate_object_with_id(servantId, this);
        }

        /**
         * References created with {@code _this_object()} must come from the POA that activated this servant
         * (not the root POA), so that they carry that POA's policies and IOR components,
         * e.g. the zero-port policy and the CSIv2 transport addresses of a secure-only endpoint.
         */
        @Override
        public POA _default_POA() {
            return poa;
        }

        abstract Servant convertLocalContextToRemoteContext(NamingContextImpl o) throws Exception;

        @Override
        protected final org.omg.CORBA.Object resolveObject(NameComponent n, BindingTypeHolder type) {
            try {
                org.omg.CORBA.Object o = core.resolveObject(n, type);
                if (o == localContext) {
                    return _this_object();
                } else if (!!!(o instanceof NamingContextImpl)) {
                    return o;
                }
                Servant poaNamingContext = convertLocalContextToRemoteContext((NamingContextImpl) o);
                return NamingContextHelper.narrow(poaNamingContext._this_object());
            } catch (SystemException e) {
                // just propagate system exceptions
                throw e;
            } catch (Exception e) {
                throw (INTERNAL) (new INTERNAL("Unable to create new naming context").initCause(e));
            }
        }

        @Override
        public final void list(int how_many, BindingListHolder bl, BindingIteratorHolder bi) {
            core.list(how_many, bl, bi);
            try {
                Servant iterator = ((BindingIteratorImpl) bi.value).getServant(poa, null);
                bi.value = BindingIteratorHelper.narrow(iterator._this_object());
            } catch (SystemException e) {
                // just propagate system exceptions
                throw e;
            } catch (Exception e) {
                throw (INTERNAL) (new INTERNAL("Unable to activate BindingIterator").initCause(e));
            }
        }

        private static final class ReadOnly extends POAServant {
            ReadOnly(NamingContextImpl localContext, Core core, POA poa) throws Exception {
                super(localContext, core, poa, core.getServantId(RemoteAccess.readOnly));
            }

            private SystemException newSystemException() {
                return new NO_PERMISSION();
            }

            @Override
            public NamingContext new_context() {
                throw newSystemException();
            }

            @Override
            protected void bindObject(NameComponent n, org.omg.CORBA.Object obj, BindingTypeHolder type) {
                throw newSystemException();
            }

            @Override
            protected org.omg.CORBA.Object unbindObject(NameComponent n) {
                throw newSystemException();
            }

            @Override
            protected Servant convertLocalContextToRemoteContext(NamingContextImpl context) throws Exception {
                return context.getServant(poa, RemoteAccess.readOnly);
            }

            @Override
            public void destroy() {
                throw newSystemException();
            }
        }

        private static final class ReadWrite extends POAServant {
            ReadWrite(NamingContextImpl localContext, Core core, POA poa) throws Exception {
                super(localContext, core, poa, core.getServantId(RemoteAccess.readWrite));
            }

            @Override
            public NamingContext new_context() {
                try {
                    NamingContextImpl nci = (NamingContextImpl) core.new_context();
                    Servant newContext = nci.getServant(poa, RemoteAccess.readWrite);
                    return NamingContextHelper.narrow(newContext._this_object());
                } catch (SystemException e) {
                    // just propagate system exceptions
                    throw e;
                } catch (Exception e) {
                    throw (INTERNAL) (new INTERNAL("Unable to create new naming context").initCause(e));
                }
            }

            @Override
            protected void bindObject(NameComponent n, org.omg.CORBA.Object obj, BindingTypeHolder type) throws SystemException, CannotProceed {
                core.bindObject(n, obj, type);
            }

            @Override
            protected org.omg.CORBA.Object unbindObject(NameComponent n) throws SystemException, CannotProceed {
                return core.unbindObject(n);
            }

            protected Servant convertLocalContextToRemoteContext(NamingContextImpl context) throws Exception {
                return context.getServant(poa, RemoteAccess.readWrite);
            }

            @Override
            public void destroy() throws NotEmpty {
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
    }

    /**
     * Internal class used to store bound objects in the HashMap.
     */
    public static final class BoundObject {
        // the name this object is bound under.
        public final NameComponent name;
        // the type of binding (either nobject or ncontext).
        public final BindingType type;
        // the actual bound object.
        public final org.omg.CORBA.Object boundObject;

        /**
         * Create a new object binding for our HashMap.
         * @param name The bound object's name.
         * @param boundObject The bound object (real object or NamingContext).
         * @param type The type information associated with this binding.
         */
        private BoundObject(NameComponent name, org.omg.CORBA.Object boundObject, BindingType type) {
            this.name = name;
            this.boundObject = boundObject;
            this.type = type;
        }

        @Override
        public String toString() {
            return name + "->" + boundObject;
        }
    }

    private final Core core;

    /** lock for servant creation */
    private final Object servantCreationLock = new ServantCreationLock();

    public NamingContextImpl() throws Exception {
        core = new Core(this);
    }

    public NamingContextImpl(org.omg.CORBA.Object rootContext) throws Exception {
        core = new Core(rootContext);
    }

    @Override
    public void bind(NameComponent[] n, org.omg.CORBA.Object obj) throws NotFound, CannotProceed, InvalidName, AlreadyBound {
        core.bind(n, obj);
    }

    @Override
    public void bind_context(NameComponent[] n, NamingContext nc) throws NotFound, CannotProceed, InvalidName, AlreadyBound {
        core.bind_context(n, nc);
    }

    @Override
    public void rebind(NameComponent[] n, org.omg.CORBA.Object obj) throws NotFound, CannotProceed, InvalidName {
        core.rebind(n, obj);
    }

    @Override
    public void rebind_context(NameComponent[] n, NamingContext nc) throws NotFound, CannotProceed, InvalidName {
        core.rebind_context(n, nc);
    }

    @Override
    public org.omg.CORBA.Object resolve(NameComponent[] n) throws NotFound, CannotProceed, InvalidName {
        return core.resolve(n);
    }

    @Override
    public void unbind(NameComponent[] n) throws NotFound, CannotProceed, InvalidName {
        core.unbind(n);
    }

    @Override
    public void list(int how_many, BindingListHolder bl, BindingIteratorHolder bi) {
        core.list(how_many, bl, bi);
    }

    @Override
    public NamingContext new_context() {
        return core.new_context();
    }

    @Override
    public NamingContext bind_new_context(NameComponent[] n) throws NotFound, AlreadyBound, CannotProceed, InvalidName {
        return core.bind_new_context(n);
    }

    @Override
    public void destroy() throws NotEmpty {
        core.destroy();
    }

    @Override
    public Servant getServant(POA poa, RemoteAccess remoteAccess) throws Exception {
        byte[] sid = core.getServantId(remoteAccess);

        // synchronize around creation to avoid a race
        synchronized (servantCreationLock) {
            // check whether the servant needs to be created
            try {
                return poa.id_to_servant(sid);
            } catch (ObjectNotActive expected) {
                // guaranteed to be the unique creator-thread for this servant
                return POAServant.create(this, core, poa, remoteAccess);
            }
        }
    }

    @Override
    public String to_string(NameComponent[] n) throws InvalidName {
        return core.to_string(n);
    }

    @Override
    public NameComponent[] to_name(String sn) throws InvalidName {
        return core.to_name(sn);
    }

    @Override
    public String to_url(String addr, String sn) throws InvalidAddress, InvalidName {
        return core.to_url(addr, sn);
    }

    @Override
    public org.omg.CORBA.Object resolve_str(String n) throws NotFound, CannotProceed, InvalidName {
        return core.resolve_str(n);
    }
}
