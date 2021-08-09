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

package org.apache.myfaces.config.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Vertex is used to track dependencies and each node in a graph.  Typical
 * uses would be to ensure components are started up and torn down in the
 * proper order, or bundles were loaded and unloaded in the proper order, etc.
 *
 * @author <a href="mailto:dev@avalon.apache.org">Avalon Development Team</a>
 * @version CVS $ Revision: 1.1 $
 */
public final class Vertex<T> implements Comparable<Vertex<T>>
{
    private final String m_name;
    private final T m_node;
    private int m_order;

    /** Flag used to keep track of whether or not a given vertex has been
     *   seen by the resolveOrder methods. */
    private boolean m_seen;

    /** List of all direct dependent Vertices. */
    private final List< Vertex<T> > m_dependencies;

    /**
     * A vertex wraps a node, which can be anything.
     *
     * @param node  The wrapped node.
     */
    public Vertex(final T node)
    {
        this(node.toString(), node);
    }

    /**
     * A vertex wraps a node, which can be anything.
     *
     * @param name  A name for the node which will be used to produce useful errors.
     * @param node  The wrapped node.
     */
    public Vertex(final String name, final T node)
    {
        m_name = name;
        m_node = node;
        m_dependencies = new ArrayList< Vertex<T> >();
        reset();
    }

    /**
     * Reset the Vertex so that all the flags and runtime states are set back
     * to the original values.
     */
    public void reset()
    {
        m_order = 0;
        m_seen = false;
    }

    /**
     * Returns the name of the Vertex.
     *
     * @return The name of the Vertex.
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * Get the wrapped node that this Vertex represents.
     *
     * @return the node
     */
    public T getNode()
    {
        return m_node;
    }

    /**
     * Add a dependecy to this Vertex.  The Vertex that this one depends on will
     * be marked as referenced and then added to the list of dependencies.  The
     * list is checked before the dependency is added.
     *
     * @param v  The vertex we depend on.
     */
    public void addDependency(Vertex<T> v)
    {
        if (!m_dependencies.contains(v))
        {
            m_dependencies.add(v);
        }
    }

    /**
     * Recurse through the tree from this vertex assigning an order to each
     *  and at the same time checking for any cyclic dependencies.
     *
     * @throws CyclicDependencyException If a cyclic dependency is discovered.
     */
    public void resolveOrder() throws CyclicDependencyException
    {
        resolveOrder(getName());
    }

    /**
     * Recursively searches for cycles by travelling down the dependency lists
     *  of this vertex, looking for the start vertex.
     *
     * @param path The path to the Vertex.  It is worth the load as it makes a
     *             descriptive error message possible.
     *
     * @return The highest order of any of the dependent vertices.
     *
     * @throws CyclicDependencyException If a cyclic dependency is discovered.
     */
    private int resolveOrder(String path) throws CyclicDependencyException
    {
        m_seen = true;
        try
        {
            int highOrder = -1;
            for (Vertex<T> dv : m_dependencies)
            {
                if (dv.m_seen)
                {
                    throw new CyclicDependencyException(path + " -> "
                            + dv.getName());
                }
                else
                {
                    highOrder = Math.max(highOrder, dv.resolveOrder(path
                            + " -> " + dv.getName()));
                }
            }

            // Set this order so it is one higher than the highest dependency.
            m_order = highOrder + 1;
            return m_order;
        }
        finally
        {
            m_seen = false;
        }
    }

    /**
     * Get the list of dependencies.
     *
     * @return  The list of dependencies.
     */
    public List< Vertex<T> > getDependencies()
    {
        return m_dependencies;
    }

    /**
     * Used in the sort algorithm to sort all the Vertices so that they respect
     * the ordinal they were given during the topological sort.
     *
     * @param o  The other Vertex to compare with
     * @return -1 if this < o, 0 if this == o, or 1 if this > o
     */
    public int compareTo(final Vertex<T> o)
    {
        int orderInd;

        if (m_order < o.m_order)
        {
            orderInd = -1;
        }
        else if (m_order > o.m_order)
        {
            orderInd = 1;
        }
        else
        {
            orderInd = 0;
        }

        return orderInd;
    }

    /**
     * Get the ordinal for this vertex.
     *
     * @return  the order.
     */
    public int getOrder()
    {
        return m_order;
    }
}
