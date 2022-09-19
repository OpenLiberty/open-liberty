/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.anno.mdb;

import jakarta.ejb.MessageDriven;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.MessageListener;
import jakarta.resource.cci.Record;

@MessageDriven
public class ExampleMessageDrivenBean implements MessageListener {
    /**
     * @see javax.resource.cci.MessageListener#onMessage(javax.resource.cci.Record)
     */
    @Override
    public Record onMessage(Record record) throws ResourceException {
        System.out.println("ExampleMessageDrivenBean.onMessage record = " + record);
        return record;
    }
}
