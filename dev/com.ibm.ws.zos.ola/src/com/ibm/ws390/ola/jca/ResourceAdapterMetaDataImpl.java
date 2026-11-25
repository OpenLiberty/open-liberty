/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.ola.jca;

import javax.resource.cci.ResourceAdapterMetaData;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Meta-data for the Resource adapter.
 */
public class ResourceAdapterMetaDataImpl implements ResourceAdapterMetaData {
	
	private static final TraceComponent tc = Tr.register(ResourceAdapterMetaDataImpl.class);

	private static String OLA_ADAPTER_NAME="ola.rar";           /* @F0013381A*/
	private static String OLA_ADAPTER_DESCRIPTION="WebSphere z/OS Optimized Local Adapters (OLA)";  /* @F0013381A*/
	private static String OLA_ADAPTER_VENDOR="IBM";  /* @F0013381A*/
	private static String OLA_ADAPTER_SPECLEVEL_SUPPORTED = "1.5";  /* @F0013381A*/
	private static String OLA_ADAPTER_INTERACTIONSPEC_NAME = "com.ibm.websphere.ola.InteractionSpecImpl";  /* @F0013381A*/
	private static String OLA_ADAPTER_VERSION = "2.0";  /* @F0013381A*/

	/**
	 * Adapter name string.
	 */
	java.lang.String adapterName = null;  /* @F0013381A*/

	/**
	 * Adapter description.
	 */
	java.lang.String adapterShortDescription = null;  /* @F0013381A*/
	
	/**
	 * Adapter version.
	 */
	java.lang.String adapterVersion = null;  /* @F0013381A*/
	
	/**
	 * Adapter vendor.
	 */
	java.lang.String adapterVendorName = null;  /* @F0013381A*/
	
	/**
	 * The JCA speclevel we are using.
	 */
	java.lang.String specLevel = null;

	/**
	 * The Interaction Specs supported.
	 */
	java.lang.String[] specsSupported = null;

	/**
	 * The Interaction Execute style 3 args supported.
	 *  supportsExecuteWithInputAndOutputRecord
	 */
	java.lang.Boolean supports3args = false;

	/**
	 * The Interaction Execute style 2 args supported.
	 *  supportsExecuteWithInputRecordOnly
	 */
	java.lang.Boolean supports2args = false;

	/**
	 * Support for Local Transactions supported.
	 *  supportsLocalTransactionDemarcation
	 */
	java.lang.Boolean supportsLocalTx = false;

	/**
	 * Default Constructor.
	 * Copies all of the information from the ManagedConnectionMetaData.
	 */
	public ResourceAdapterMetaDataImpl(ResourceAdapterMetaData raData)
	{
		adapterName              = OLA_ADAPTER_NAME;
		adapterShortDescription  = OLA_ADAPTER_DESCRIPTION;
		adapterVersion           = OLA_ADAPTER_VERSION;
		adapterVendorName        = OLA_ADAPTER_VENDOR;
		specsSupported	         = new String[1];
		specsSupported[0]        = OLA_ADAPTER_INTERACTIONSPEC_NAME;
		specLevel                = OLA_ADAPTER_SPECLEVEL_SUPPORTED;
		supports3args            = true;
		supports2args            = true;
		supportsLocalTx          = true;
	}

	/**
     * Returns the Adapter name.
	 * @see javax.resource.cci.ResourceAdapterMetaData#getAdapterName()
	 */
	public String getAdapterName() {  /* @F013381A*/
		return adapterName;                      /* @F013381A*/
	}

	/**
     * Returns the Adapter short description.
	 * @see javax.resource.cci.ResourceAdapterMetaData#getAdapterShortDescription()
	 */
	public String getAdapterShortDescription() {  /* @F013381A*/
		return adapterShortDescription;          /* @F013381A*/
	}

	/**
     * Returns the Adapter vendor name.
	 * @see javax.resource.cci.ResourceAdapterMetaData#getAdapterVendorName()
	 */
	public String getAdapterVendorName() {  /* @F013381A*/
		return adapterVendorName;                /* @F013381A*/
	}

	/**
     * Returns the Adapter version.
	 * @see javax.resource.cci.ResourceAdapterMetaData#getAdapterVersion()
	 */
	public String getAdapterVersion() {  /* @F013381A*/
		return adapterVersion;                   /* @F013381A*/
	}

	/**
	 * Returns the Interaction specs supported.  Returns "com.ibm.websphere.ola.InteractionSpecImpl"
	 * @see javax.resource.spi.ResourceAdapterMetaData#getSpecVersion()
	 */
	public String[] getInteractionSpecsSupported()  /* @F013381A*/
	{
		return specsSupported;                                                 /* @F013381A*/
	}

	/**
	 * Returns the spec version supported.   Returns string 1.5
	 * @see javax.resource.spi.ResourceAdapterMetaData#getSpecVersion()
	 */
	public String getSpecVersion()  /* @F013381A*/
	{
		return specLevel;                                     /* @F013381A*/
	}

	/**
	 * Returns whether support for Execute with Input and Output Records
	 * is provided. For WOLA this is TRUE.
	 * @see javax.resource.spi.ResourceAdapterMetaData#supportsExecuteWithInputAndOutputRecord()	 */
	public boolean supportsExecuteWithInputAndOutputRecord()  /* @F013381A*/
	{
		return supports3args;                                                      /* @F013381A*/
	}

	/**
	 * Returns whether support for Execute with Input record only is provided. For WOLA this is TRUE.
	 * @see javax.resource.spi.ResourceAdapterMetaData#supportsExecuteWithInputRecordOnly()	 */
	public boolean supportsExecuteWithInputRecordOnly()  /* @F013381A*/
	{
		return supports2args;                                                      /* @F013381A*/
	}

	/**
	 * Returns whether support for Local Transactions is provided. For WOLA this is TRUE.
	 * @see javax.resource.spi.ResourceAdapterMetaData#supportsLocalTransactionDemarcation()	 */
	public boolean supportsLocalTransactionDemarcation()  /* @F013381A*/
	{
		return supportsLocalTx;                                                      /* @F013381A*/
	}

}
