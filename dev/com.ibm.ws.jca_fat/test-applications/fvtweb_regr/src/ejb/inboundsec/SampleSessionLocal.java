/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package ejb.inboundsec;

import javax.ejb.Local;
import javax.transaction.xa.Xid;

import com.ibm.adapter.endpoint.MessageEndpointTestResultsImpl;
import com.ibm.adapter.message.WorkInformation;

@Local
public interface SampleSessionLocal {

    public MessageEndpointTestResultsImpl sendMessage(String deliveryId,
                                                      String messageText, int state, int waitTime, Xid xid,
                                                      int workExecutionType, WorkInformation wi) throws Exception;

    public MessageEndpointTestResultsImpl[] sendNestedMessage(
                                                              String deliveryId, String messageText, int state, int waitTime,
                                                              Xid xid, Xid childXid, int workExecutionType, int nestedDoWorkType,
                                                              WorkInformation wi) throws Exception;

}
