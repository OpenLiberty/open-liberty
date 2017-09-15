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
package javax.faces.component.visit;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import javax.faces.FactoryFinder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

/**
 * @since 2.0
 */
public abstract class VisitContext
{
    public static final Collection<String> ALL_IDS = new AllIdsCollection();

    public static VisitContext createVisitContext(FacesContext context)
    {
        VisitContextFactory factory
                = (VisitContextFactory) FactoryFinder.getFactory(FactoryFinder.VISIT_CONTEXT_FACTORY);
        return factory.getVisitContext(context, null, null);
    }

    public static VisitContext createVisitContext(FacesContext context, Collection<String> ids, Set<VisitHint> hints)
    {
        VisitContextFactory factory
                = (VisitContextFactory) FactoryFinder.getFactory(FactoryFinder.VISIT_CONTEXT_FACTORY);
        return factory.getVisitContext(context, ids, hints);
    }

    public abstract FacesContext getFacesContext();

    public abstract Set<VisitHint> getHints();

    public abstract Collection<String> getIdsToVisit();

    public abstract Collection<String> getSubtreeIdsToVisit(UIComponent component);

    public abstract VisitResult invokeVisitCallback(UIComponent component, VisitCallback callback);

    private static class AllIdsCollection implements Collection<String>, Serializable
    {
        public boolean add(String e)
        {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection<? extends String> c)
        {
            throw new UnsupportedOperationException();
        }

        public void clear()
        {
            throw new UnsupportedOperationException();
        }

        public boolean contains(Object o)
        {
            throw new UnsupportedOperationException();
        }

        public boolean containsAll(Collection<?> c)
        {
            throw new UnsupportedOperationException();
        }

        public boolean isEmpty()
        {
            return false;
        }

        public Iterator<String> iterator()
        {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object o)
        {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(Collection<?> c)
        {
            throw new UnsupportedOperationException();
        }

        public boolean retainAll(Collection<?> c)
        {
            throw new UnsupportedOperationException();
        }

        public int size()
        {
            throw new UnsupportedOperationException();
        }

        public Object[] toArray()
        {
            throw new UnsupportedOperationException();
        }

        public <T> T[] toArray(T[] a)
        {
            throw new UnsupportedOperationException();
        }
    }
}
