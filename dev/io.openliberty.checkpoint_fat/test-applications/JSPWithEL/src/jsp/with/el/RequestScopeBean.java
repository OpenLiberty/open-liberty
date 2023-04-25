/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package jsp.with.el;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

@RequestScoped
@Named("rsb")
public class RequestScopeBean {
    private static AtomicInteger constructCount = new AtomicInteger();

    public RequestScopeBean() {
        constructCount.incrementAndGet();
    }

    public String getMessage() {
        String message = "RSB - TEST - " + constructCount.get();
        System.out.println(message);
        return message;
    }
}
