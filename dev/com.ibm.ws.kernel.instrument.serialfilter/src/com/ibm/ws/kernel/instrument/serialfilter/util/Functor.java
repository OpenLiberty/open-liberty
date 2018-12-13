/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class Functor<RANGE, DOMAIN> implements Map<RANGE, DOMAIN> {

    /** Subclasses must implement this method to define what this functor does */
    protected abstract DOMAIN apply(RANGE input) throws Exception;

    /** Subclasses may override this method to allow input to this functor */
    @Override
    public DOMAIN put(RANGE key, DOMAIN value) {throw newUOE();}

    /** Subclasses may override this method to define how to reset any internal state */
    protected void reset() throws Exception {}

    /** Invoke the apply() method but re-throw any exceptions as unchecked. */
    @SuppressWarnings("unchecked")
    @Override
    public final DOMAIN get(Object o) {
        try {
            return apply((RANGE)o);
        } catch (Exception e) {
            throw Functor.<RuntimeException>rethrow(e);
        }
    }

    /** Invoke the reset() method but re-throw any exceptions as unchecked. */
    @Override
    public final void clear() {
        try {
            reset();
        } catch (Exception e) {
            throw Functor.<RuntimeException>rethrow(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static<T extends Throwable> T rethrow(Throwable t) throws T {throw (T) t;}


    private<T> UnsupportedOperationException newUOE() {throw new UnsupportedOperationException();}
    public final int size() {throw newUOE();}
    public final boolean isEmpty() {throw newUOE();}
    public final boolean containsKey(Object o) {throw newUOE();}
    public final boolean containsValue(Object o) {throw newUOE();}
    public final DOMAIN remove(Object o) {throw newUOE();}
    public final void putAll(Map<? extends RANGE, ? extends DOMAIN> map) {throw newUOE();}
    public final Set<RANGE> keySet() {throw newUOE();}
    public final Collection<DOMAIN> values() {throw newUOE();}
    public final Set<Entry<RANGE, DOMAIN>> entrySet() {throw newUOE();}
}
