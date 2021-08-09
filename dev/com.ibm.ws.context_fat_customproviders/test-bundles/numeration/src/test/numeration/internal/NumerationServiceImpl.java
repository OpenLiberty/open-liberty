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
package test.numeration.internal;

import java.util.Deque;
import java.util.LinkedList;

import org.osgi.service.component.ComponentContext;

import test.numeration.NumerationService;

/**
 * This a fake thread context that we made up for testing purposes.
 * It makes a numeration context available to each thread which
 * shows a textual representation for numbers in different numeration
 * systems (for example, binary)
 */
public class NumerationServiceImpl implements NumerationService {

    static ThreadLocal<Deque<NumerationContext>> threadlocal = new ThreadLocal<Deque<NumerationContext>>() {
        @Override
        protected Deque<NumerationContext> initialValue() {
            Deque<NumerationContext> stack = new LinkedList<NumerationContext>();
            stack.push(new NumerationContext());
            return stack;
        }
    };

    /**
     * Declarative Services method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context context for this component
     */
    protected void activate(ComponentContext context) {}

    /**
     * Declarative Services method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     * 
     * @param context context for this component
     */
    protected void deactivate(ComponentContext context) {}

    /**
     * Sets the radix for the current thread.
     * 
     * @param radix in the range of 2 (binary) through 36
     */
    @Override
    public void setRadix(int radix) {
        threadlocal.get().peek().radix = radix;
    }

    /**
     * Returns text representing the number.
     * 
     * @param number a number
     * @return text representing the number.
     */
    @Override
    public String toString(long number) {
        NumerationContext context = threadlocal.get().peek();
        String str = Long.toString(number, context.radix);
        return context.upperCase ? str.toUpperCase() : str;
    }
}
