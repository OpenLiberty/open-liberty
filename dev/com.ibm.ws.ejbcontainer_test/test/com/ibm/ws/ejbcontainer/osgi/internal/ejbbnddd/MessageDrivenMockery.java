/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.ejbbnddd;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.ibm.ws.javaee.dd.ejbbnd.MessageDriven;

public class MessageDrivenMockery extends EnterpriseBeanMockery<MessageDrivenMockery> {

    MessageDrivenMockery(Mockery mockery, String name) {
        super(mockery, name);
    }

    public MessageDriven mock() {
        final MessageDriven messageDriven = mockEnterpriseBean(MessageDriven.class);
        mockery.checking(new Expectations() {
            { /* empty check */
            }
        });
        return messageDriven;
    }
}
