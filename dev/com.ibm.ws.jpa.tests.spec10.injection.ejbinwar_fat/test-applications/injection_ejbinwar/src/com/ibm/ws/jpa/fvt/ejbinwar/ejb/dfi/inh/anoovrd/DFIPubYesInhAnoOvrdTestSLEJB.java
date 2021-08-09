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

package com.ibm.ws.jpa.fvt.ejbinwar.ejb.dfi.inh.anoovrd;

import javax.ejb.Local;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;

@Stateful(name = "DFIPubYesInhAnoOvrdTestSLEJB")
@Local(DFIPubYesInhAnoOvrdTestSLEJBLocal.class)
@TransactionManagement(javax.ejb.TransactionManagementType.BEAN)
public class DFIPubYesInhAnoOvrdTestSLEJB extends DFIPubYesInhAnoOvrdTestSuperclass {

}