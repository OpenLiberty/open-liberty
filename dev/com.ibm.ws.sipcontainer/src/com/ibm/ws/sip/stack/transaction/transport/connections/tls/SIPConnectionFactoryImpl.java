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

import jain.protocol.ip.sip.ListeningPoint;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnectionFactory;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;

/**
 * 
 * @author Amirk
 *
 * this tls layer will support:
 * 1.jvm 1.4 or higher of any kind
 * 2.jvm 1.3 with ibm extension
 */
public class SIPConnectionFactoryImpl implements SIPConnectionFactory
{
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger =
		Log.get(SIPConnectionFactoryImpl.class);

	/** 
	 * ssl socket factory 
	 **/
	private SSLSocketFactory m_socketFactory;

	/** 
	 * ssl server socket factory 
	 **/
	private SSLServerSocketFactory m_serverSocketFactory;

	/**
	 * lazy instansiation flag
	 */
	private boolean c_firstTime = true;

	/**
	 * did the SSL factory instanciation passed
	 */
	private boolean c_instanciationPassed = false;

	public SIPConnectionFactoryImpl()
	{}

	public SIPListenningConnection createListeningConnection(ListeningPoint lp)
	{
		SIPListenningConnectionImpl retval = null;
		//use lazy instansiation here
		if (c_firstTime)
		{
			try
			{
				SSLContext sslContext = null;
				boolean isUseDeafultSSL  = ApplicationProperties.getProperties().getBoolean("com.ibm.ssl.useSSLDefaults");
				if(!isUseDeafultSSL){
					if( c_logger.isTraceDebugEnabled())
					{
					c_logger.traceDebug(this,"createListeningConnection", "initiating ssl");
					}
					//create the reperoire and init by it the ssl context
					SSLRepertoire repertoire = createSSLRepertoire();
					
					c_logger.traceDebug(this, "createListeningConnection", "created ssl repertoire:\n" + repertoire );
					sslContext = initSSL( repertoire );	
				}
								
				createSSLSockets(sslContext);
								
			}
			catch (IllegalStateException exp)
			{
				if( c_logger.isTraceDebugEnabled())
				{
				c_logger.traceDebug(this,"createListeningConnection", exp.getMessage(),exp);
				}
			}
			catch (Throwable t)
			{
				if( c_logger.isTraceDebugEnabled())
				{
					c_logger.traceDebug(this, "createListeningConnection", t.getMessage(),t);
				}
			}
		}

		// Assaf: remove the else.
		if (c_instanciationPassed)
		{
			retval = new SIPListenningConnectionImpl(m_serverSocketFactory,
					m_socketFactory, (ListeningPointImpl)lp);
		}
		return retval;
	}


	/**
	 * create SSLRepertoire
	 * @return SSLRepertoire
	 */
	public static SSLRepertoire createSSLRepertoire()
	{
		String protocol  = ApplicationProperties.getProperties().getString(
				StackProperties.SSL_PROTOCOL);
		String contextProvider = ApplicationProperties.getProperties().getString(
				StackProperties.SSL_CONTEXT_PROVIDER);
		boolean isClientAuthenticationEnabled  = ApplicationProperties.getProperties().
				getBoolean(StackProperties.SSL_CLIENT_AUTHENTICATION);  
		String keyStoreFile = ApplicationProperties.getProperties().getString(
				StackProperties.SSL_KEY_STORE);
		String keyStorePassword  = ApplicationProperties.getProperties().
			getString(StackProperties.SSL_KEY_STORE_PASSWORD);
		String keyManagerName = ApplicationProperties.getProperties().getString(
				StackProperties.SSL_KEY_MANAGER);
		String keyStoreProvider = ApplicationProperties.getProperties().getString(
				StackProperties.SSL_KEY_STORE_PROVIDER);
		String keyStoreType = ApplicationProperties.getProperties().getString(
				StackProperties.SSL_KEY_STORE_TYPE);
		String trustStoreFile = ApplicationProperties.getProperties().getString(
				StackProperties.SSL_TRUST_STORE);
		String trustStorePassword = ApplicationProperties.getProperties().
			getString(StackProperties.SSL_TRUST_STORE_PASSWORD);
		String trustManagerName = ApplicationProperties.getProperties().
			getString(StackProperties.SSL_TRUST_MANAGER);
		String trustStoreProvider = ApplicationProperties.getProperties().
			getString(StackProperties.SSL_TRUST_STORE_PROVIDER);
		String trustStoreType = ApplicationProperties.getProperties().
			getString(StackProperties.SSL_TRUST_STORE_TYPE);
		String jsseProviderClassName = ApplicationProperties.getProperties().
			getString(StackProperties.SSL_PROVIDER_CLASS_NAME);
		
		return new SSLRepertoire( protocol , contextProvider , isClientAuthenticationEnabled ,
								  keyStoreFile , keyStorePassword , keyManagerName , keyStoreProvider ,
								  keyStoreType , trustStoreFile , trustStorePassword , trustManagerName ,
								  trustStoreProvider , trustStoreType , jsseProviderClassName );
	}


	/**
	 * use IBM Extension classes
	 * @param repertoire
	 * @return
	 */
	private SSLContext initSSL( SSLRepertoire repertoire )
	{   
		try
		{    
			if( c_logger.isTraceDebugEnabled())
			{
			c_logger.traceDebug(this,"initSSL","getting SSLContext");
			}
			if (Security.getProvider(repertoire.getJsseProviderClassName()) == null)
			{
				Security.addProvider((Provider)Class.forName(repertoire.getJsseProviderClassName()).newInstance());
				if( c_logger.isTraceDebugEnabled())
				{
				c_logger.traceDebug(this,"initSSL","did not found IBM security Provider ,installing new one");
				}
			}
			else
			{
				if( c_logger.isTraceDebugEnabled())
				{
				c_logger.traceDebug(this,"initSSL","found IBM secuirity Provider , not installing new one");
				}
			}
			SSLContext sslContext = SSLContext.getInstance( repertoire.getProtocol() , repertoire.getContextProvider());                 
		            
		
			//load a key store file into the key store
			if( c_logger.isTraceDebugEnabled())
			{
			c_logger.traceDebug(this,"initSSL","initing KeyStore...");
			}
			KeyManagerFactory kmf = KeyManagerFactory.getInstance( repertoire.getKeyManagerName(), repertoire.getContextProvider());				
			KeyStore ks = KeyStore.getInstance( repertoire.getKeyStoreType() , repertoire.getKeyStoreProvider());
			ks.load(new FileInputStream( repertoire.getKeyStoreFile() ), ((repertoire.getKeyStorePassword()==null) ? null:repertoire.getKeyStorePassword().toCharArray()) );
			kmf.init(ks, repertoire.getKeyStorePassword().toCharArray());
			if( c_logger.isTraceDebugEnabled())
			{
			c_logger.traceDebug(this,"initSSL","initiated KeyStore");
			}
			//end key store
			
			//create a trust manager 
			if( c_logger.isTraceDebugEnabled())
			{
			c_logger.traceDebug(this,"initSSL","initing TrustStore...");
			}
			TrustManagerFactory tmf= TrustManagerFactory.getInstance(repertoire.getTrustManagerName(),repertoire.getContextProvider());
			KeyStore ts = KeyStore.getInstance(repertoire.getTrustStoreType(), repertoire.getTrustStoreProvider());				
			ts.load(new FileInputStream(repertoire.getTrustStoreFile()), ((repertoire.getTrustStorePassword()==null) ? null:repertoire.getTrustStorePassword().toCharArray()) );
			tmf.init(ts);
			if( c_logger.isTraceDebugEnabled())
			{
			c_logger.traceDebug(this,"initSSL","initiated TrustStore");
			}
			//end trust store
			
			//initiate the sslContext
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			if( c_logger.isTraceDebugEnabled())
			{
			c_logger.traceDebug(this,"initSSL","initiated sslContext");
			}
						
			return sslContext;
		} 
		catch (NoSuchProviderException e)
		{            
			if( c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(this,"initSSL",e.getMessage(),e);
			}
			throw new IllegalStateException(e.getMessage());
		} 
		catch (NoSuchAlgorithmException e)
		{
			if( c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(this,"initSSL",e.getMessage(),e);
			}
		  throw new IllegalStateException(e.getMessage());
		} 
		catch (KeyStoreException e)
		{
			if( c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(this,"initSSL",e.getMessage(),e);
			}
			throw new IllegalStateException(e.getMessage());
		} 
		catch (CertificateException e)
		{
			if( c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(this,"initSSL",e.getMessage(),e);
			}
			throw new IllegalStateException(e.getMessage());
		} 
		catch (FileNotFoundException e)
		{
			if( c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(this,"initSSL",e.getMessage(),e);
			}
			throw new IllegalStateException(e.getMessage());
		} 
		catch (IOException e)
		{
			if( c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(this,"initSSL",e.getMessage(),e);
			}
			throw new IllegalStateException(e.getMessage());
		}
		catch (UnrecoverableKeyException e)
		{
			if( c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(this,"initSSL",e.getMessage(),e);
			}
			throw new IllegalStateException(e.getMessage());
		}
		catch (KeyManagementException e)
		{
			if( c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(this,"initSSL",e.getMessage(),e);
			}
			throw new IllegalStateException(e.getMessage());
		} 
		catch (InstantiationException e)
		{
			if( c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(this,"initSSL",e.getMessage(),e);
			}
			throw new IllegalStateException(e.getMessage());
		}
		catch (IllegalAccessException e)
		{
			if( c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(this,"initSSL",e.getMessage(),e);
			}
			throw new IllegalStateException(e.getMessage());
		}
		catch (ClassNotFoundException e)
		{
			if( c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(this,"initSSL",e.getMessage(),e);
			}
			throw new IllegalStateException(e.getMessage());
		}
	}
	
	private void createSSLSockets(SSLContext sslContext){
		if(sslContext!=null){
			//get factories						
			m_socketFactory = sslContext.getSocketFactory();
			if( c_logger.isTraceDebugEnabled())
			{
			c_logger.traceDebug(this,"createSSLSockets","got socketFactory");
			}
			m_serverSocketFactory = sslContext.getServerSocketFactory(); 
			if( c_logger.isTraceDebugEnabled())
			{
			c_logger.traceDebug(this,"createSSLSockets","got serverSocketFactory");
			}
		}else{
			m_socketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
			if( c_logger.isTraceDebugEnabled())
			{
			c_logger.traceDebug(this,"createSSLSockets","got default socketFactory");
			}
		    m_serverSocketFactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
			if( c_logger.isTraceDebugEnabled())
			{
			c_logger.traceDebug(this,"createSSLSockets","got default serverSocketFactory");
			}
		}
	
	
		//set flags
		c_firstTime = false;
		c_instanciationPassed = true;
	}
	
}
