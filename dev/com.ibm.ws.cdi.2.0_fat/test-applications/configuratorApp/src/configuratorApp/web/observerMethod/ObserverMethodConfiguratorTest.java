/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package configuratorApp.web.observerMethod;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import configuratorApp.web.ConfiguratorTestBase;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/observerMethodConfiguratorTest")
public class ObserverMethodConfiguratorTest extends ConfiguratorTestBase {

    public static ArrayList<Integer> squareObservations = new ArrayList<Integer>();
    public static ArrayList<Integer> circleObservations = new ArrayList<Integer>();

    public static int[] squareObserverPriorities = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    public static final int[] circleObserverOrder = { 1, 2, 8, 3, 9, 5, 4, 6, 0, 7 };

    public static int[] positions = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };

    static {
        // Shuffle square observer priorities
        final Random rnd = new Random();
        for (int i = squareObserverPriorities.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            int j = squareObserverPriorities[index];
            squareObserverPriorities[index] = squareObserverPriorities[i];
            squareObserverPriorities[i] = j;
        }
    }

    @Inject
    BeanManager bm;

    @Test
    @Mode(TestMode.FULL)
    public void sniffObserverMethodConfigurator() {
        Set<ObserverMethod<? super Triangle>> observers = bm.resolveObserverMethods(new Triangle(), Any.Literal.INSTANCE);
        assertEquals(observers.iterator().next().getPriority(), ObserverMethod.DEFAULT_PRIORITY + 1);
    }

    @Test
    @Mode(TestMode.FULL)
    public void testObserverMethodCanBeVetoed() {
        Set<ObserverMethod<? super Dodecagon>> observers = bm.resolveObserverMethods(new Dodecagon(), Any.Literal.INSTANCE);
        assertEquals(observers.size(), 0);
    }

    @Inject
    Event<Square> squareEvent;

    /*
     * This tests that the SquareObservers are called in an expected order
     * Each SquareObserver is configured with a priority by a configurator in the ProcessObserverMethodObserver class
     * The priorities used for each observer are taken from the squareObserverPriorities array which is initialized and randomized above
     * Each observer records that it executed by appending its identity to the squareObservations ArrayList
     * This test method checks that order of execution recorded in the squareObservations ArrayList matches the random priorities in the squareObserverPriorities array.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSquareObserverOrdering() {

        squareEvent.fire(new Square());

        // Check enough observers were called
        assertEquals(squareObserverPriorities.length, squareObservations.size());

        // Check observers were called in the expected randomized order
        int position = 0;
        for (int observation : squareObservations) {
            System.out.println("Checking square observer " + observation + " happened in position " + position);
            assertEquals(observation, positions[position++]);
        }
    }

    @Inject
    Event<Circle> circleEvent;

    /*
     * This tests that the priorities set on the circle observers (via @Priority) work as expected
     * Each circle observer has a hard coded priority assigned via @Priority in the individual observer classes
     * Those priorities are also represented by the circleObserverOrder array above.
     * Each observer records that it executed by appending its identity to the circleObservations ArrayList
     * This test method checks that the order of execution recorded in the circleObservations ArrayList matches the order represented by the circleObserverOrder array.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testCircleObserverOrdering() {

        circleEvent.fire(new Circle());

        // Check enough observers were called
        assertEquals(circleObserverOrder.length, circleObservations.size());

        // Check observers were called in the expected order
        for (int position = 0; position < circleObserverOrder.length; position++) {
            assertEquals(circleObservations.get(position).intValue(), circleObserverOrder[position]);
        }
    }
}