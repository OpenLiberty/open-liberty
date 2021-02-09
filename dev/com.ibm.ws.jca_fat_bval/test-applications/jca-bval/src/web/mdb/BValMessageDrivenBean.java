/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.mdb;

import javax.ejb.MessageDriven;
import javax.resource.ResourceException;
import javax.resource.cci.MessageListener;
import javax.resource.cci.Record;

@MessageDriven
public class BValMessageDrivenBean implements MessageListener {
    /**
     * @see javax.resource.cci.MessageListener#onMessage(javax.resource.cci.Record)
     */
    @Override
    public Record onMessage(Record record) throws ResourceException {
        System.out.println("BValMessageDrivenBean.onMessage record = " + record);
        return record;
    }
}
