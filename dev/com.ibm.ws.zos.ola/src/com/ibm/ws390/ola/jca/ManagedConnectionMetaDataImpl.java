/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.ola.jca;                                   /* @F003691C*/

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionMetaData;

/**
 * ManagedConnectionMetaData implementation.  Holds some strings representing 
 * what this resource and its backend really are.
 */
public class ManagedConnectionMetaDataImpl
	implements ManagedConnectionMetaData {

  static final int CURRENT_MAJOR_VERSION = 1;                    /* @F003691A*/
  static final int CURRENT_MINOR_VERSION = 2;                    /* @PM89577C*/

  private int _majorVersion;                                     /* @F003691A*/
  private int _minorVersion;                                     /* @F003691A*/
  final private String _userName;                                /* @PM89577A*/

	/**
	 * Default constructor.  This is not currently used, but remains here to
   * make the release-to-release incompatibility tool happy.
	 */
	public ManagedConnectionMetaDataImpl()
  {
    this(null);                                                  /* @PM89577A*/
  }

  /**
   * Constructor with user name.
   */
	ManagedConnectionMetaDataImpl(String userName)                 /* @PM89577A*/
	{
    _majorVersion = CURRENT_MAJOR_VERSION;                       /* @F003691A*/
    _minorVersion = CURRENT_MINOR_VERSION;                       /* @F003691A*/
    _userName = userName;                                        /* @PM89577A*/
	}
	
	/**
	 * Get the EIS product name.
	 * @see javax.resource.spi.ManagedConnectionMetaData#getEISProductName()
	 */
	public String getEISProductName() throws ResourceException {
		return new String("WebSphere z/OS Optimized Local Adapters (OLA)");
	}

	/**
	 * Returns EIS version.  
	 * @see javax.resource.spi.ManagedConnectionMetaData#getEISProductVersion()
	 */
	public String getEISProductVersion() throws ResourceException 
	{
		return new String("Version " + _majorVersion + "." + _minorVersion); /* @F003691C*/
	}

	/**
	 * Returns the maximum number of connections that can be open to the
	 * EIS.  We return 1, but this is really a lie.  I have no idea how many
	 * connections we can have open.  Probably a lot.
	 * @see javax.resource.spi.ManagedConnectionMetaData#getMaxConnections()
	 */
	public int getMaxConnections() throws ResourceException {
		return 0; /* Return 0 per javadoc, if not applicable.  @PM89577C*/
	}

	/**
	 * Returns the user name.  This is the MVS user ID that the managed connection
   * authenticated.
	 * @see javax.resource.spi.ManagedConnectionMetaData#getUserName()
	 */
	public String getUserName() throws ResourceException 
	{
		return _userName;                                   /* @PM89577C*/
	}
}
