package com.ibm.tx.jta.util;
/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

/**
 * This class provides the implementation of an OSGi Bundle Activator. It allows the transactions bundle
 * to gain access to the BundleContext when the OSGi framework starts.
 *
 */
public class TxBundleTools implements BundleActivator
{
	private static final TraceComponent tc = Tr.register(TxBundleTools.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

	static BundleContext _bc;

	@Override
	public void start(BundleContext bundleContext) throws Exception
	{
		if (tc.isDebugEnabled()) Tr.debug(tc, "start", bundleContext);
		_bc = bundleContext;
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception
	{
		if (tc.isDebugEnabled()) Tr.debug(tc, "stop", bundleContext);
		_bc = null;
	}

	public static BundleContext getBundleContext()
	{
		if (tc.isDebugEnabled()) Tr.debug(tc, "getBundleContext", _bc);
		return _bc;
	}
}
