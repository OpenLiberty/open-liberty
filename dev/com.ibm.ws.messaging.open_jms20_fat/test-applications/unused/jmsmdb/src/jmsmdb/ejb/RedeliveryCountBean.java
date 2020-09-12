/*
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package jmsmdb.ejb;

import javax.jms.Message;
import javax.jms.MessageListener;

public class RedeliveryCountBean implements MessageListener {

	/*  Serializable class saves the persisted redelivery count value before server restart.
     * 
	*/
	private static final long serialVersionUID = 1L;
	
    public int redeliveryCount=0;
    
	public int getRedeliveryCount() {
		return redeliveryCount;
	}

	public void setRedeliveryCount(int redeliveryCount) {
		this.redeliveryCount = redeliveryCount;
	}
	
	@Override
    public void onMessage(Message message) {
        
	    System.out.println("In On Message");
        try {
            
            message.getIntProperty("JMSXDeliveryCount");
            
            System.out.println("Redelivery Count Value="+message.getIntProperty("JMSXDeliveryCount"));
            
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
        throw new RuntimeException();
    }
    
}
