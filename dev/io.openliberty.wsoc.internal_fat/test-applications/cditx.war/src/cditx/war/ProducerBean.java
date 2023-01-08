/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package cditx.war;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

public class ProducerBean {

    public ProducerBean() {}

    @Produces
    @Named("Producer1")
    StringBuffer methodN1() {
        // this string value is hard wired for the test to check later.
        return new StringBuffer(":ProducerBean Field Injected");
    }

    @PostConstruct
    public void postCon() {}

    @PreDestroy
    public void destruct() {}
}
