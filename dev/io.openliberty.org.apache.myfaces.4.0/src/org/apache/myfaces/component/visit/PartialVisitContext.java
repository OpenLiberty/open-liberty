/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.component.visit;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jakarta.faces.component.NamingContainer;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.visit.VisitCallback;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.component.visit.VisitHint;
import jakarta.faces.component.visit.VisitResult;
import jakarta.faces.context.FacesContext;
import org.apache.myfaces.core.api.shared.lang.Assert;

/**
 * <p>
 * A VisitContext implementation that is used when performing a partial
 * component tree visit.</p>
 *
 * @author Werner Punz, Blake Sullivan (latest modification by $Author$)
 * @version $Rev$ $Date$
 */
public class PartialVisitContext extends VisitContext
{

    // The client ids to visit
    private final Collection<String> _clientIds;

    // The ids to visit
    private final Collection<String> _ids;

    // The client ids that have yet to be visited
    private final Collection<String> _unvisitedClientIds;

    // This map contains the information needed by getIdsToVisit().
    // The keys in this map are NamingContainer client ids.  The values
    // are collections containing all of the client ids to visit within
    // corresponding naming container.
    private final Map<String, Collection<String>> _subtreeClientIds;

    // The FacesContext for this request
    private final FacesContext _facesContext;

    // Our visit hints
    private final Set<VisitHint> _hints;
    
    /**
     * Creates a PartialVisitorContext instance.
     *
     * @param facesContext the FacesContext for the current request
     * @param clientIds the client ids of the components to visit
     * @throws NullPointerException if {@code facesContext} is {@code null}
     */
    public PartialVisitContext(FacesContext facesContext, Collection<String> clientIds)
    {
        this(facesContext, clientIds, null);
    }

    /**
     * Creates a PartialVisitorContext instance with the specified hints.
     *
     * @param facesContext the FacesContext for the current request
     * @param clientIds the client ids of the components to visit
     * @param hints a the VisitHints for this visit
     * @throws NullPointerException if {@code facesContext} is {@code null}
     * @throws IllegalArgumentException if the phaseId is specified and hints
     * does not contain VisitHint.EXECUTE_LIFECYCLE
     */
    public PartialVisitContext(FacesContext facesContext, Collection<String> clientIds, Set<VisitHint> hints)
    {
        Assert.notNull(facesContext, "facesContext");

        _facesContext = facesContext;

        // Copy the client ids into a HashSet to allow for quick lookups.
        //    Set<String> clientIdSet = (clientIds == null)
        //            ? new HashSet<String>()
        //                    : new HashSet<String>(clientIds);
        // Initialize our various collections
        // We maintain 4 collections:
        //
        // 1. clientIds: contains all of the client ids to visit
        // 2. ids: contains just ids (not client ids) to visit.
        //    We use this to optimize our check to see whether a
        //    particular component is in the visit set (ie. to
        //    avoid having to compute the client id).
        // 3. subtreeClientIds: contains client ids to visit broken
        //    out by naming container subtree.  (Needed by
        //    getSubtreeIdsToVisit()).
        // 4. unvisitedClientIds: contains the client ids to visit that
        //    have not yet been visited.
        //
        // We populate these now.
        //
        // Note that we use default HashSet/Map initial capacities, though
        // perhaps we could pick more intelligent defaults.
        // Initialize unvisitedClientIds collection
        _unvisitedClientIds = new HashSet<String>();

        // Initialize ids collection
        _ids = new HashSet<String>();

        // Intialize subtreeClientIds collection
        _subtreeClientIds = new HashMap<String, Collection<String>>();

        // Initialize the clientIds collection.  Note that we proxy
        // this collection so that we can trap adds/removes and sync
        // up all of the other collections.
        _clientIds = new CollectionProxy<String>(new HashSet<String>());

        // Finally, populate the clientIds collection.  This has the
        // side effect of populating all of the other collections.
        org.apache.myfaces.util.lang.ArrayUtils.addAll(_clientIds, clientIds);
        //_clientIds.addAll(clientIdSet);

        // Copy and store hints - ensure unmodifiable and non-empty
        EnumSet<VisitHint> hintsEnumSet = ((hints == null) || (hints.isEmpty()))
                ? EnumSet.noneOf(VisitHint.class)
                : EnumSet.copyOf(hints);

        _hints = Collections.unmodifiableSet(hintsEnumSet);
    }

    /**
     * @see VisitContext#getFacesContext VisitContext.getFacesContext()
     */
    @Override
    public FacesContext getFacesContext()
    {
        return _facesContext;
    }

    /**
     * @see VisitContext#getHints VisitContext.getHints
     */
    @Override
    public Set<VisitHint> getHints()
    {
        return _hints;
    }

    /**
     * @see VisitContext#getIdsToVisit VisitContext.getIdsToVisit()
     */
    @Override
    public Collection<String> getIdsToVisit()
    {
        // We just return our clientIds collection.  This is
        // the modifiable (but proxied) collection of all of
        // the client ids to visit.
        return _clientIds;
    }

    /**
     * @see VisitContext#getSubtreeIdsToVisit
     * VisitContext.getSubtreeIdsToVisit()
     */
    @Override
    public Collection<String> getSubtreeIdsToVisit(UIComponent component)
    {
        // Make sure component is a NamingContainer
        if (!(component instanceof NamingContainer))
        {
            throw new IllegalArgumentException("Component is not a NamingContainer: " + component);
        }

        String clientId = component.getClientId(getFacesContext());
        Collection<String> ids = _subtreeClientIds.get(clientId);

        if (ids == null)
        {
            return Collections.emptyList();
        }
        else
        {
            return Collections.unmodifiableCollection(ids);
        }
    }

    /**
     * @see VisitContext#invokeVisitCallback VisitContext.invokeVisitCallback()
     */
    @Override
    public VisitResult invokeVisitCallback(UIComponent component, VisitCallback callback)
    {
        // First sure that we should visit this component - ie.
        // that this component is represented in our id set.
        String clientId = _getVisitId(component);

        if (clientId == null)
        {
            // Not visiting this component, but allow visit to
            // continue into this subtree in case we've got
            // visit targets there.
            return VisitResult.ACCEPT;
        }

        // If we made it this far, the component matches one of
        // client ids, so perform the visit.
        VisitResult result = callback.visit(this, component);

        // Remove the component from our "unvisited" collection
        _unvisitedClientIds.remove(clientId);

        // If the unvisited collection is now empty, we are done.
        // Return VisitResult.COMPLETE to terminate the visit.
        if (_unvisitedClientIds.isEmpty())
        {
            return VisitResult.COMPLETE;
        }
        else
        {
            // Otherwise, just return the callback's result
            return result;
        }
    }

    // Called by CollectionProxy to notify PartialVisitContext that
    // an new id has been added.
    private void _idAdded(String clientId)
    {
        // An id to visit has been added, update our other
        // collections to reflect this.

        // Update the ids collection
        _ids.add(_getIdFromClientId(clientId));

        // Update the unvisited ids collection
        _unvisitedClientIds.add(clientId);

        // Update the subtree ids collection
        _addSubtreeClientId(clientId);
    }

    // Called by CollectionProxy to notify PartialVisitContext that
    // an id has been removed
    private void _idRemoved(String clientId)
    {
        // An id to visit has been removed, update our other
        // collections to reflect this.  Note that we don't
        // update the ids collection, since we ids (non-client ids)
        // may not be unique.

        // Update the unvisited ids collection
        _unvisitedClientIds.remove(clientId);

        // Update the subtree ids collection
        _removeSubtreeClientId(clientId);
    }

    // Tests whether the specified component should be visited.
    // If so, returns its client id.  If not, returns null.
    private String _getVisitId(UIComponent component)
    {
        // We first check to see whether the component's id
        // is in our id collection.  We do this before checking
        // for the full client id because getting the full client id
        // is more expensive than just getting the local id.
        String id = component.getId();

        if (id != null && !_ids.contains(id))
        {
            return null;
        }

        // The id was a match - now check the client id.
        // note that client id should never be null (should be
        // generated even if id is null, so asserting this.)
        String clientId = component.getClientId(getFacesContext());
        assert (clientId != null);

        return _clientIds.contains(clientId) ? clientId : null;
    }

    // Converts an client id into a plain old id by ripping
    // out the trailing id segmetn.
    private String _getIdFromClientId(String clientId)
    {
        final char separator = getFacesContext().getNamingContainerSeparatorChar();
        int lastIndex = clientId.lastIndexOf(separator);

        String id = null;

        if (lastIndex < 0)
        {
            id = clientId;
        }
        else if (lastIndex < (clientId.length() - 1))
        {
            id = clientId.substring(lastIndex + 1);
        }

        return id;
    }

    // Given a single client id, populate the subtree map with all possible
    // subtree client ids
    private void _addSubtreeClientId(String clientId)
    {
        // Loop over the client id and find the substring corresponding to
        // each ancestor NamingContainer client id.  For each ancestor
        // NamingContainer, add an entry into the map for the full client
        // id.
        final char separator = getFacesContext().getNamingContainerSeparatorChar();

        int length = clientId.length();

        for (int i = 0; i < length; i++)
        {
            if (clientId.charAt(i) == separator)
            {
                // We found an ancestor NamingContainer client id - add
                // an entry to the map.
                String namingContainerClientId = clientId.substring(0, i);

                // Check to see whether we've already ids under this
                // NamingContainer client id.  If not, create the
                // Collection for this NamingContainer client id and
                // stash it away in our map
                Collection<String> c = _subtreeClientIds.get(namingContainerClientId);
                if (c == null)
                {
                    c = new ArrayList<>(5);
                    _subtreeClientIds.put(namingContainerClientId, c);
                }

                // Stash away the client id
                c.add(clientId);
            }
        }
    }

    // Given a single client id, remove any entries corresponding
    // entries from our subtree collections
    private void _removeSubtreeClientId(String clientId)
    {
        // Loop through each entry in the map and check to see whether
        // the client id to remove should be contained in the corresponding
        // collection - ie. whether the key (the NamingContainer client id)
        // is present at the start of the client id to remove.
        for (Map.Entry<String, Collection<String>> stringCollectionEntry : _subtreeClientIds.entrySet())
        {
            if (clientId.startsWith(stringCollectionEntry.getKey()))
            {
                // If the clientId starts with the key, we should
                // have an entry for this clientId in the corresponding
                // collection.  Remove it.
                Collection<String> ids = stringCollectionEntry.getValue();
                ids.remove(clientId);
            }
        }
    }

    // Little proxy collection implementation.  We proxy the id
    // collection so that we can detect modifications and update
    // our internal state when ids to visit are added or removed.
    private class CollectionProxy<E extends String> extends AbstractCollection<E>
    {
        private final Collection<E> _wrapped;
        
        private CollectionProxy(Collection<E> wrapped)
        {
            _wrapped = wrapped;
        }

        @Override
        public int size()
        {
            return _wrapped.size();
        }

        @Override
        public Iterator<E> iterator()
        {
            return new IteratorProxy<>(_wrapped.iterator());
        }

        @Override
        public boolean add(E o)
        {
            boolean added = _wrapped.add(o);
            if (added)
            {
                _idAdded(o);
            }
            return added;
        }
    }

    // Little proxy iterator implementation used by CollectionProxy
    // so that we can catch removes.
    private class IteratorProxy<E extends String> implements Iterator<E>
    {
        private final Iterator<E> _wrapped;
        private E _current = null;
        
        private IteratorProxy(Iterator<E> wrapped)
        {
            _wrapped = wrapped;
        }

        @Override
        public boolean hasNext()
        {
            return _wrapped.hasNext();
        }

        @Override
        public E next()
        {
            _current = _wrapped.next();

            return _current;
        }

        @Override
        public void remove()
        {
            if (_current != null)
            {
                _idRemoved(_current);
            }

            _wrapped.remove();
        }
    }
}
