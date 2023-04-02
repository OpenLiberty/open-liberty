/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package jsf.container.bean;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;

@Stateless
public class TestEJB {

    protected boolean postConstructCalled;

    @PostConstruct
    public void postConstruct() {
        postConstructCalled = true;
    }

    public boolean verifyPostConstruct() {
        return postConstructCalled;
    }

}
