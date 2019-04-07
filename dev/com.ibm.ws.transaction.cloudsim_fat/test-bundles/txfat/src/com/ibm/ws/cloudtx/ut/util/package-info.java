/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * using a package-info file here is kinda silly, but using a packageinfo file doesn't seem to work.
 * 
 * @version 1.0
 */
@org.osgi.annotation.versioning.Version("1.0")
@TraceOptions(traceGroup = "Transaction", messageBundle = "com.ibm.ws.transaction.services.TransactionMessages")
package com.ibm.ws.cloudtx.ut.util;

import com.ibm.websphere.ras.annotation.TraceOptions;
