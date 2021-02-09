/*******************************************************************************
 * Copyright (c) 2012,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.map.internal;

import java.util.AbstractMap;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import test.map.internal.MapContext;

/**
 * This a fake thread context that we made up for testing purposes.
 * It makes a java.util.Map available to each thread.
 * The context propagation service can be used to make one thread run
 * with the "map context" of another.
 */
public class MapService extends AbstractMap<String, String> {

    static ThreadLocal<Deque<MapContext>> threadlocal = new ThreadLocal<Deque<MapContext>>() {
        @Override
        protected Deque<MapContext> initialValue() {
            Deque<MapContext> stack = new LinkedList<MapContext>();
            stack.push(new MapContext());
            return stack;
        }
    };

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
    protected void activate(Map<String, Object> properties) {}

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     */
    protected void deactivate() {}

    /** {@inheritDoc} */
    @Override
    public Set<Map.Entry<String, String>> entrySet() {
        return threadlocal.get().peek().entrySet();
    }

    /** {@inheritDoc} */
    @Override
    public String put(String key, String value) {
        return threadlocal.get().peek().put(key, value);
    }
}
