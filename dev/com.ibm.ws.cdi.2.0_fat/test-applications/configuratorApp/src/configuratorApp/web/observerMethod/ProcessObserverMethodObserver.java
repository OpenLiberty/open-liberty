/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package configuratorApp.web.observerMethod;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessObserverMethod;

public class ProcessObserverMethodObserver implements Extension {

    void observer(@Observes ProcessObserverMethod<Triangle, ShapeObserver> event) {

        event.configureObserverMethod().priority(ObserverMethod.DEFAULT_PRIORITY + 1);
    }

    void vetoDodecagonObserver(@Observes ProcessObserverMethod<Dodecagon, ShapeObserver> event) {
        event.veto();
    }

    void observerConfigurator0(@Observes ProcessObserverMethod<Square, SquareObserver0> event) {
        System.out.println("Observer 0 should go in position " + ObserverMethodConfiguratorTest.observerPriorities[0]);
        ObserverMethodConfiguratorTest.positions[ObserverMethodConfiguratorTest.observerPriorities[0]] = 0;
        event.configureObserverMethod().priority(ObserverMethod.DEFAULT_PRIORITY + ObserverMethodConfiguratorTest.observerPriorities[0]);
    }

    void observerConfigurator1(@Observes ProcessObserverMethod<Square, SquareObserver1> event) {
        System.out.println("Observer 1 should go in position " + ObserverMethodConfiguratorTest.observerPriorities[1]);
        ObserverMethodConfiguratorTest.positions[ObserverMethodConfiguratorTest.observerPriorities[1]] = 1;
        event.configureObserverMethod().priority(ObserverMethod.DEFAULT_PRIORITY + ObserverMethodConfiguratorTest.observerPriorities[1]);
    }

    void observerConfigurator2(@Observes ProcessObserverMethod<Square, SquareObserver2> event) {
        System.out.println("Observer 2 should go in position " + ObserverMethodConfiguratorTest.observerPriorities[2]);
        ObserverMethodConfiguratorTest.positions[ObserverMethodConfiguratorTest.observerPriorities[2]] = 2;
        event.configureObserverMethod().priority(ObserverMethod.DEFAULT_PRIORITY + ObserverMethodConfiguratorTest.observerPriorities[2]);
    }

    void observerConfigurator3(@Observes ProcessObserverMethod<Square, SquareObserver3> event) {
        System.out.println("Observer 3 should go in position " + ObserverMethodConfiguratorTest.observerPriorities[3]);
        ObserverMethodConfiguratorTest.positions[ObserverMethodConfiguratorTest.observerPriorities[3]] = 3;
        event.configureObserverMethod().priority(ObserverMethod.DEFAULT_PRIORITY + ObserverMethodConfiguratorTest.observerPriorities[3]);
    }

    void observerConfigurator4(@Observes ProcessObserverMethod<Square, SquareObserver4> event) {
        System.out.println("Observer 4 should go in position " + ObserverMethodConfiguratorTest.observerPriorities[4]);
        ObserverMethodConfiguratorTest.positions[ObserverMethodConfiguratorTest.observerPriorities[4]] = 4;
        event.configureObserverMethod().priority(ObserverMethod.DEFAULT_PRIORITY + ObserverMethodConfiguratorTest.observerPriorities[4]);
    }

    void observerConfigurator5(@Observes ProcessObserverMethod<Square, SquareObserver5> event) {
        System.out.println("Observer 5 should go in position " + ObserverMethodConfiguratorTest.observerPriorities[5]);
        ObserverMethodConfiguratorTest.positions[ObserverMethodConfiguratorTest.observerPriorities[5]] = 5;
        event.configureObserverMethod().priority(ObserverMethod.DEFAULT_PRIORITY + ObserverMethodConfiguratorTest.observerPriorities[5]);
    }

    void observerConfigurator6(@Observes ProcessObserverMethod<Square, SquareObserver6> event) {
        System.out.println("Observer 6 should go in position " + ObserverMethodConfiguratorTest.observerPriorities[6]);
        ObserverMethodConfiguratorTest.positions[ObserverMethodConfiguratorTest.observerPriorities[6]] = 6;
        event.configureObserverMethod().priority(ObserverMethod.DEFAULT_PRIORITY + ObserverMethodConfiguratorTest.observerPriorities[6]);
    }

    void observerConfigurator7(@Observes ProcessObserverMethod<Square, SquareObserver7> event) {
        System.out.println("Observer 7 should go in position " + ObserverMethodConfiguratorTest.observerPriorities[7]);
        ObserverMethodConfiguratorTest.positions[ObserverMethodConfiguratorTest.observerPriorities[7]] = 7;
        event.configureObserverMethod().priority(ObserverMethod.DEFAULT_PRIORITY + ObserverMethodConfiguratorTest.observerPriorities[7]);
    }

    void observerConfigurator8(@Observes ProcessObserverMethod<Square, SquareObserver8> event) {
        System.out.println("Observer 8 should go in position " + ObserverMethodConfiguratorTest.observerPriorities[8]);
        ObserverMethodConfiguratorTest.positions[ObserverMethodConfiguratorTest.observerPriorities[8]] = 8;
        event.configureObserverMethod().priority(ObserverMethod.DEFAULT_PRIORITY + ObserverMethodConfiguratorTest.observerPriorities[8]);
    }

    void observerConfigurator9(@Observes ProcessObserverMethod<Square, SquareObserver9> event) {
        System.out.println("Observer 9 should go in position " + ObserverMethodConfiguratorTest.observerPriorities[9]);
        ObserverMethodConfiguratorTest.positions[ObserverMethodConfiguratorTest.observerPriorities[9]] = 9;
        event.configureObserverMethod().priority(ObserverMethod.DEFAULT_PRIORITY + ObserverMethodConfiguratorTest.observerPriorities[9]);
    }
}