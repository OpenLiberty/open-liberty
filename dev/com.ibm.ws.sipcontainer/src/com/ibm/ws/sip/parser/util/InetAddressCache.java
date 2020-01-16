/*******************************************************************************
 * Copyright (c) 2003,2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.parser.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.WeakHashMap;

import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;

/**
 * Cache of InetAddress and InetSocketAddress objects.
 * All state in this class is static.
 * 
 * @author ran
 */
public class InetAddressCache 
{
	/**
	 * Cache of InetAddress instances, indexed by host String.
	 * WeakHashMap allows garbage collector to delete unused entries.
	 */
	private static final WeakHashMap<String, CachedInetAddress> s_inetAddressCache =
		new WeakHashMap<String, CachedInetAddress>();

	/**
	 * Cache of IP address string, indexed by InetAddress.
	 * WeakHashMap allows garbage collector to delete unused entries.
	 */
	private static final WeakHashMap<InetAddress, String> s_ipCache =
		new WeakHashMap<InetAddress, String>();

	/**
	 * Cache of InetSocketAddress instances, indexed by host:port.
	 * WeakHashMap allows garbage collector to delete unused entries.
	 */
	private static final WeakHashMap<InetSocketAddressKey, InetSocketAddress> s_inetSocketAddressCache =
		new WeakHashMap<InetSocketAddressKey, InetSocketAddress>();

	/**
	 * Thread-local instance used when looking up the InetSocketAddress cache
	 */
	private static final ThreadLocal<InetSocketAddressKey> s_key =
		new ThreadLocal<InetSocketAddressKey>() {
			protected InetSocketAddressKey initialValue() {
				return new InetSocketAddressKey();
			}
	};

	/**
     * longest time to keep a cached InetAddress entry, in seconds.
     * 0 specifies no caching. -1 specifies infinite caching.
     */
    private static final int s_cacheAge = SIPTransactionStack.instance().getConfiguration().getNetworkAddressCacheTtl();

	/**
	 * Index used only for looking up InetSocketAddress entries in the cache
	 */
	static class InetSocketAddressKey {
		private InetAddress m_address; // null if unresolvable
		private String m_host; // null if resolvable
		private int m_port; // port number

		/** called before using this key to lookup the cache */
		void set(InetAddress address, String host, int port) {
			m_address = address;
			m_host = host;
			m_port = port;
		}

		/** @see java.lang.Object#hashCode() */
		public int hashCode() {
			return Objects.hash(m_address, m_host, m_port);
		}

		public boolean equals(Object other) {
			if (!(other instanceof InetSocketAddressKey)) {
				return false;
			}
			InetSocketAddressKey otherKey = (InetSocketAddressKey)other;
			if (this.m_address != otherKey.m_address || this.m_host != otherKey.m_host || this.m_port != otherKey.m_port) {
				return false;
			}
			return true;
		}
		
		/** called to match between InetSocketAddressKey to InetSocketAddress 
		 *  if equals - return true
		 * */
		public boolean match(InetSocketAddress otherInetSocketAddress) {
			// compare port
			int thisPort = m_port;
			int otherPort = otherInetSocketAddress.getPort();
			if (thisPort != otherPort) {
				return false;
			}
			// compare address
			InetAddress thisAddress = m_address;
			InetAddress otherAddress = otherInetSocketAddress.getAddress();
			if (thisAddress != otherAddress) {
				if (thisAddress == null || otherAddress == null) {
					return false;
				}
				if (!thisAddress.equals(otherAddress)) {
					return false;
				}
			}
			// compare host - only if the address is unresolvable
			if (thisAddress == null) {
				String thisHost = m_host;
				String otherHost = otherInetSocketAddress.getHostName();
				if (thisHost != otherHost) {
					if (thisHost == null || otherHost == null) {
						return false;
					}
					if (!thisHost.equals(otherHost)) {
						return false;
					}
				}
			}
			return true;
		}
	}

	/**
	 * Cached InetAddress entry. 
	 */
	static class CachedInetAddress {
		/** the resolved address */
		InetAddress m_address;

		/** the time this entry was inserted into the cache */
		long m_timestamp;

		/** constructor */
		CachedInetAddress(InetAddress address, long timestamp) {
			m_address = address;
			m_timestamp = timestamp;
		}
	}

	/**
	 * Private constructor to prevent accidental instantiation 
	 */
	private InetAddressCache() {
	}

	/**
	 * Gets an InetAddress instance given the host name or IP address string.
	 * 
	 * @param host host name or IP address string
	 * @return the cached InetAddress
	 * @throws UnknownHostException on DNS failure
	 * @see java.net.InetAddress#getByName(String)
	 */
	public static InetAddress getByName(String host) throws UnknownHostException
	{
		// look up the cache
		InetAddress address;
		long now = System.currentTimeMillis();
		CachedInetAddress entry = s_inetAddressCache.get(host);
		if (entry == null) {
			// cache miss. query DNS.
			address = InetAddress.getByName(host);
			entry = new CachedInetAddress(address, now);

			// update the cache
			synchronized (s_inetAddressCache) {
				s_inetAddressCache.put(host, entry);
			}
		}
		else if (s_cacheAge == -1) {
			// cache hit and infinite caching. don't query DNS.
			address = entry.m_address;
		}
		else {
			// cache hit. query DNS if timestamp is too old.
			long timestamp = entry.m_timestamp;
			long diff = (now - timestamp) / 1000;
			if (diff >= s_cacheAge) {
				// timestamp is stale. query DNS.
				entry.m_address = address = InetAddress.getByName(host);
				entry.m_timestamp = now;
			}
			else {
				// timestamp is fresh. don't query DNS.
				address = entry.m_address;
			}
		}
		return address; 
	}

	/**
	 * Gets the IP address given the host name or IP address.
	 * 
	 * @param host host name or IP address string
	 * @return the cached IP address string
	 * @throws UnknownHostException on DNS failure
	 * @see java.net.InetAddress#getHostAddress()
	 */
	public static String getHostAddress(String host) throws UnknownHostException
	{
		InetAddress address = getByName(host);
		String ip = getHostAddress(address);
		return ip; 
	}

	/**
	 * Gets the IP of the given address as a String.
	 * 
	 * @param address a pre-resolved numeric address
	 * @return the IP address as a String
	 * @see java.net.InetAddress#getHostAddress()
	 */
	public static String getHostAddress(InetAddress address) {
		// look up the cache
		String ip = s_ipCache.get(address);
		if (ip == null) {
			// instantiate
			ip = address.getHostAddress();

			// update the cache
			synchronized (s_ipCache) {
				s_ipCache.put(address, ip);
			}
		}
		return ip;
	}

	/**
	 * Gets an InetSocketAddress instance given the host name or IP address string,
	 * and the port number
	 * 
	 * @param host host name or IP address string
	 * @param port port number
	 * @return the cached InetAddress
	 */
	public static InetSocketAddress getInetSocketAddress(String host, int port)
	{
		// get a cached instance of InetAddress
		InetAddress address;
		try {
			address = getByName(host);
			host = null;
			// resolvable - address is non-null and host is null
		}
		catch (UnknownHostException e) {
			address = null;
			// unresolvable - address is null and host is non-null
		}

		// create a thread-local key that we can use to look up the cache
		InetSocketAddressKey key = s_key.get();
		key.set(address, host, port);
		
		// look up the cache
		InetSocketAddress socketAddress = s_inetSocketAddressCache.get(key);
		if (socketAddress == null) {
			// instantiate
			socketAddress = address == null
				? new InetSocketAddress(host, port)
				: new InetSocketAddress(address, port);
				
			InetSocketAddressKey socketAddressKey = new InetSocketAddressKey();
			socketAddressKey.set(address, host, port);
			if (!key.equals(socketAddressKey)) {
				throw new RuntimeException("Expected equality between ["
						+ socketAddress + "] and [" + key + ']');
			}

			// update the cache
			synchronized (s_inetSocketAddressCache) {
				s_inetSocketAddressCache.put(socketAddressKey, socketAddress);
			}
		}
		return socketAddress; 
	}
}
