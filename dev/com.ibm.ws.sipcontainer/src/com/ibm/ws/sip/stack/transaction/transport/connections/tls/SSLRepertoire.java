/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transaction.transport.connections.tls;

/**
 * @author sipuser
 *
 */
public class SSLRepertoire
{

	/**
	 * root for the key store file
	 */
	private static String KEY_STORE_SYSTEM_KEY = "javax.net.ssl.keyStore";

	/**
	 * key store password
	 */
	private static String KEY_STORE_PASSWORD_SYSTEM_KEY =
		"javax.net.ssl.keyStorePassword";
	
	/**
	 * trust store 
	 */
	private static String TRUST_STORE_SYSTEM_KEY = "javax.net.ssl.trustStore";

	/**
	 * trust store password
	 */
	private static String TRUST_STORE_PASSWORD_SYSTEM_KEY =
		"javax.net.ssl.trustStorePassword";

	private String m_protocol;
	private String m_contextProvider;
	private boolean m_isClientAuthenticationEnabled;

	private String m_keyStoreFile;
	private String m_keyStorePassword;
	private String m_keyManagerName;
	private String m_keyStoreProvider;
	private String m_keyStoreType;

	private String m_trustStoreFile;
	private String m_trustStorePassword;
	private String m_trustManagerName;
	private String m_trustStoreProvider;
	private String m_trustStoreType;
	
	private String m_jsseProviderClassName;
	
	

	/**
	 * SSLRepertoire - holds ssl connection data to initiate SSL Context
	 */
	public SSLRepertoire( String protocol , 
							String contextProvider , 
							boolean isClientAuthenticationEnabled , 
							String keyStoreFile ,
							String keyStorePassword ,
							String keyManagerName ,
							String keyStoreProvider ,
							String keyStoreType ,
							String trustStoreFile ,
							String trustStorePassword ,
							String trustManagerName ,
							String trustStoreProvider ,
							String trustStoreType ,
							String jsseProviderClassName ) 
	{		
		m_protocol = protocol!=null ? protocol : TLSDefaults.DEFAULT_PROTOCOL;
		m_contextProvider = contextProvider!=null ? contextProvider : TLSDefaults.DEFAULT_CONTEXT_PROVIDER;
		m_isClientAuthenticationEnabled = isClientAuthenticationEnabled;
		if( keyStoreFile==null )
		{
			keyStoreFile = System.getProperty(KEY_STORE_SYSTEM_KEY);
		}
		if( keyStoreFile==null ) 
		{
			throw new IllegalArgumentException("keyStore File must be set!!!");
		}				
		m_keyStoreFile = keyStoreFile;
		
		if( keyStorePassword==null )
		{
			keyStorePassword =  System.getProperty(KEY_STORE_PASSWORD_SYSTEM_KEY);
		}
		if( keyStorePassword==null ) 
		{
			throw new IllegalArgumentException("keyStore Password must be set!!!");
		}				
		m_keyStorePassword = keyStorePassword;
		
		m_keyManagerName = keyManagerName!=null ? keyManagerName : TLSDefaults.DEFAULT_KEY_MANAGER_NAME;
		m_keyStoreProvider = keyStoreProvider!=null ? keyStoreProvider : TLSDefaults.DEFAULT_KEYSTORE_PROVIDER;
		m_keyStoreType = keyStoreType!=null ? keyStoreType : TLSDefaults.DEFAULT_KEYSTORE_TYPE;
		
		
		if( trustStoreFile==null )
		{
			trustStoreFile = System.getProperty( TRUST_STORE_SYSTEM_KEY );
		}
		if( trustStoreFile==null ) 
		{
			throw new IllegalArgumentException("trustStore File must be set!!!");
		}				
		m_trustStoreFile = trustStoreFile;



		if( trustStorePassword==null )
		{
			trustStorePassword = System.getProperty( TRUST_STORE_PASSWORD_SYSTEM_KEY );
		}
		if( trustStorePassword==null ) 
		{
			throw new IllegalArgumentException("trustStore Password must be set!!!");
		}				
		m_trustStorePassword = trustStorePassword;
		
		m_trustManagerName = trustManagerName!=null ? trustManagerName : TLSDefaults.DEFAULT_TRUST_MANAGER_NAME;
		m_trustStoreProvider = trustStoreProvider!=null ? trustStoreProvider : TLSDefaults.DEFAULT_TRUSTSTORE_PROVIDER;
		m_trustStoreType = trustStoreType!=null ? trustStoreType : TLSDefaults.DEFAULT_TRUSTSTORE_TYPE;
		m_jsseProviderClassName = jsseProviderClassName!=null ? jsseProviderClassName : TLSDefaults.DEFAULT_JSSE_PROVIDER_CLASS_NAME;
	}

    /**
     * @return
     */
    public String getContextProvider()
    {
        return m_contextProvider;
    }

    /**
     * @return
     */
    public boolean isClientAuthenticationEnabled()
    {
        return m_isClientAuthenticationEnabled;
    }

    /**
     * @return
     */
    public String getKeyManagerName()
    {
        return m_keyManagerName;
    }

    /**
     * @return
     */
    public String getKeyStoreFile()
    {
        return m_keyStoreFile;
    }

    /**
     * @return
     */
    public String getKeyStoreProvider()
    {
        return m_keyStoreProvider;
    }

    /**
     * @return
     */
    public String getKeyStoreType()
    {
        return m_keyStoreType;
    }

    /**
     * @return
     */
    public String getProtocol()
    {
        return m_protocol;
    }

    /**
     * @return
     */
    public String getTrustStoreFile()
    {
        return m_trustStoreFile;
    }

    /**
     * @return
     */
    public String getTrustStorePassword()
    {
        return m_trustStorePassword;
    }

    /**
     * @return
     */
    public String getTrustStoreProvider()
    {
        return m_trustStoreProvider;
    }

    /**
     * @return
     */
    public String getTrustStoreType()
    {
        return m_trustStoreType;
    }


    /**
     * @return
     */
    public String getKeyStorePassword()
    {
        return m_keyStorePassword;
    }

    /**
     * @return
     */
    public String getTrustManagerName()
    {
        return m_trustManagerName;
    }


    /**
     * @return
     */
    public String getJsseProviderClassName()
    {
        return m_jsseProviderClassName;
    }
    
    public String toString()
    {
    	StringBuffer buf = new StringBuffer("SIP SSL Repertoire is:\n");
		buf.append("protocol [" + m_protocol + "]\n");
		buf.append("contextProvider [" +m_contextProvider + "]\n");
		buf.append("isClientAuthenticationEnabled [" +m_isClientAuthenticationEnabled + "]\n");
		buf.append("keyStoreFile [" +m_keyStoreFile + "]\n");
		//buf.append("keyStorePassword [" +m_keyStorePassword + "]\n");
		buf.append("keyManagerName [" +m_keyManagerName + "]\n");
		buf.append("keyStoreProvider [" +m_keyStoreProvider + "]\n");
		buf.append("keyStoreType [" +m_keyStoreType + "]\n");
		buf.append("trustStoreFile [" +m_trustStoreFile + "]\n");
		//buf.append("trustStorePassword [" +m_trustStorePassword + "]\n");
		buf.append("trustManagerName [" +m_trustManagerName + "]\n");
		buf.append("trustStoreProvider [" +m_trustStoreProvider + "]\n");
		buf.append("trustStoreType [" +m_trustStoreType + "]\n");
		buf.append("jsseProviderClassName [" +m_jsseProviderClassName + "]\n");
		return buf.toString();				
    }
    

}
