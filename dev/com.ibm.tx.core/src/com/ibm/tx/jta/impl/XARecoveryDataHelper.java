package com.ibm.tx.jta.impl;
/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.XAResourceFactory;
import com.ibm.tx.jta.util.TxBundleTools;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

public class XARecoveryDataHelper
{
	private static final TraceComponent tc = Tr.register(XARecoveryDataHelper.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

	public static XAResourceFactory lookupXAResourceFactory(String filter)
	{
		if (tc.isEntryEnabled()) Tr.entry(tc, "lookupXAResourceFactory", filter);

		final BundleContext bundleContext = TxBundleTools.getBundleContext();
		
		if (bundleContext == null)
		{
			if (tc.isEntryEnabled()) Tr.exit(tc, "lookupXAResourceFactory", null);
			return null;
		}

		ServiceReference[] results = null;

		try
		{
			results = bundleContext.getServiceReferences(XAResourceFactory.class.getCanonicalName(), filter);
		}
		catch (InvalidSyntaxException e)
		{
		    // Wasn't a filter
			if (tc.isEntryEnabled()) Tr.exit(tc, "lookupXAResourceFactory", "not a filter");
			return null;
		}
		
		if (results == null || results.length <= 0) {
			if (results == null) {
				if (tc.isDebugEnabled())
					Tr.debug(tc, "Results returned from registry are null");
			} else {
				if (tc.isDebugEnabled())
					Tr.debug(tc, "Results of length " + results.length + " returned from registry");
			}
			if (tc.isEntryEnabled())
				Tr.exit(tc, "lookupXAResourceFactory", null);
			return null;
		}

		if (tc.isDebugEnabled())
        	Tr.debug(tc, "Found " + results.length + " service references in the registry");

		final XAResourceFactory xaresFactory = (XAResourceFactory) bundleContext.getService(results[0]);
		if (tc.isEntryEnabled()) Tr.exit(tc, "lookupXAResourceFactory", xaresFactory);
		return xaresFactory;
	}
}
