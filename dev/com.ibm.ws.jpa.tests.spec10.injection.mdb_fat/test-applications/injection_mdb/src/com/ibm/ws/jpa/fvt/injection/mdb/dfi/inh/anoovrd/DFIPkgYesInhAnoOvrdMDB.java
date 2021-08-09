/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.injection.mdb.dfi.inh.anoovrd;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

@MessageDriven(activationConfig = {
                                    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                                    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
                                    @ActivationConfigProperty(propertyName = "destination", propertyValue = "DFIPkgYesInhAnoOvrdMDB_Queue")

},
               name = "DFIPkgYesInhAnoOvrdMDB")
@TransactionManagement(TransactionManagementType.BEAN)
public class DFIPkgYesInhAnoOvrdMDB extends DFIPkgYesInhAnoOvrdTestSuperclass {

}
