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
package com.ibm.ws.sip.container.protocol;

/**
 * Represents a connection, as described in RFC 5626, including the
 * transport, local host, local port, remote host, remote port.
 * In cluster, also the proxy's internal (container-facing) host and port.
 * 
 * @author ran
 */
public class Flow
{
	/** connection transport protocol */
	private final String m_transport;

	/** remote IP address */
	private final String m_remoteHost;

	/** remote port number */
	private final int m_remotePort;

	/** local IP address */
	private final String m_localHost;

	/** local port number */
	private final int m_localPort;

	/** the proxy's internal IP address, or null in standalone */
	private final String m_proxyHost;

	/** the proxy's internal port number, or 0 in standalone */
	private final int m_proxyPort;

	/** true if the flow was tampered with, false otherwise */
	private final boolean m_tampered;

	/**
	 * constructor
	 * 
	 * @param transport the connection transport protocol
	 * @param remoteHost peer IP address
	 * @param remotePort peer port number
	 * @param localHost local IP address
	 * @param localPort local port number
	 * @param proxyHost the proxy's internal IP address, or null in standalone
	 * @param proxyPort the proxy's internal port number, or 0 in standalone
	 * @param tampered true if the flow token was tampered with, false otherwise
	 */
	public Flow(String transport,
		String remoteHost, int remotePort,
		String localHost, int localPort,
		String proxyHost, int proxyPort,
		boolean tampered)
	{
		m_transport = transport;
		m_remoteHost = remoteHost;
		m_remotePort = remotePort;
		m_localHost = localHost;
		m_localPort = localPort;
		m_proxyHost = proxyHost;
		m_proxyPort = proxyPort;
		m_tampered = tampered;
	}

	/**
	 * @return the connection transport
	 */
	public String getTransport() {
		return m_transport;
	}

	/**
	 * @return the peer IP address
	 */
	public String getRemoteHost() {
		return m_remoteHost;
	}

	/**
	 * @return the peer port number
	 */
	public int getRemotePort() {
		return m_remotePort;
	}

	/**
	 * @return the local IP address
	 */
	public String getLocalHost() {
		return m_localHost;
	}

	/**
	 * @return the local port number
	 */
	public int getLocalPort() {
		return m_localPort;
	}

	/**
	 * @return the proxy's internal IP address, or null in standalone
	 */
	public String getProxyHost() {
		return m_proxyHost;
	}

	/**
	 * @return the proxy's internal port number, or 0 in standalone
	 */
	public int getProxyPort() {
		return m_proxyPort;
	}

	/**
	 * @return true if the flow was tampered with, false otherwise
	 */
	public boolean isTampered() {
		return m_tampered;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (!(other instanceof Flow)) {
			return false;
		}
		Flow otherFlow = (Flow)other;
		if (otherFlow.m_remotePort != m_remotePort) {
			return false;
		}
		if (otherFlow.m_localPort != m_localPort) {
			return false;
		}
		if (otherFlow.m_proxyPort != m_proxyPort) {
			return false;
		}
		if (otherFlow.m_localHost == null) {
			if (m_localHost != null) {
				return false;
			}
		}
		else if (!otherFlow.m_localHost.equals(m_localHost)) {
			return false;
		}
		if (otherFlow.m_remoteHost == null) {
			if (m_remoteHost != null) {
				return false;
			}
		}
		else if (!otherFlow.m_remoteHost.equals(m_remoteHost)) {
			return false;
		}
		if (otherFlow.m_proxyHost == null) {
			if (m_proxyHost != null) {
				return false;
			}
		}
		else if (!otherFlow.m_proxyHost.equals(m_proxyHost)) {
			return false;
		}
		return true;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return m_transport.hashCode()
			* m_localPort
			* m_remotePort
			* m_proxyPort
			* (m_localHost == null ? 1 : m_localHost.hashCode())
			* (m_remoteHost == null ? 1 : m_remoteHost.hashCode())
			* (m_proxyHost == null ? 1 : m_proxyHost.hashCode());
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder s = new StringBuilder(256);
		s.append(m_transport).append('/');
		s.append(m_localHost).append(':').append(m_localPort).append('-');
		s.append(m_remoteHost).append(':').append(m_remotePort).append('-');
		s.append(m_proxyHost).append(':').append(m_proxyPort);
		s.append('-').append((m_tampered ? "X" : "V"));
		return s.toString();
	}
}
