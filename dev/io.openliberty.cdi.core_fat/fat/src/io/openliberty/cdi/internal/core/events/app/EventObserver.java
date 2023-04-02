/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi.internal.core.events.app;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;

@RequestScoped
public class EventObserver {

    private List<String> recordedValues = new ArrayList<>();

    @SuppressWarnings("unused")
    private void observe(@Observes TestEvent event) {
        recordedValues.add(event.getValue());
    }

    public List<String> getValues() {
        return new ArrayList<>(recordedValues);
    }

}
