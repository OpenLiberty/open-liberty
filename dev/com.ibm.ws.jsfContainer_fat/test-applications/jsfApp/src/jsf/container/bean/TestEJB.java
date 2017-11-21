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
