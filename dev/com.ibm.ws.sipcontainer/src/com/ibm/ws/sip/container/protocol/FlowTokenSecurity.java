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

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
///import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
//TODO Liberty probably remove this include as we don't support HA on Liberty
//import com.ibm.ws.sip.hamanagment.util.SipClusterUtil;

/**
 * Utility class used for securing flow tokens,
 * according to section 5.2 of RFC 5626.
 * Only used by class OutboundProcessor in this package.
 * 
 * @author ran
 * changed by Tamir
 */
class FlowTokenSecurity
{
	/** class logger */
	private static final LogMgr s_logger = Log.get(FlowTokenSecurity.class);

	/** singleton instance */
	private static final FlowTokenSecurity s_instance = new FlowTokenSecurity();

	/** true if running standalone, false if fronted by a WAS proxy */
	private final boolean m_standalone;

	/**
	 * encapsulation of the secret key, and the MAC, used for flow token security
	 */
	static class Secret {
		/** the secret key */
		Key m_key;

		/** the MAC, based on m_key */
		Mac m_mac;

		/** constructor */
		Secret(Key key, Mac mac) {
			m_key = key;
			m_mac = mac;
		}

		/** @see java.lang.Object#toString() */
		public String toString() {
			StringBuilder b = new StringBuilder(256);
			b.append("format [").append(m_key.getFormat());
			b.append("] algorithm [").append(m_mac.getAlgorithm());
			b.append("] size [").append(m_mac.getMacLength()).append(']');
			return b.toString();
		}
	}

	/**
	 * set of secret keys used to secure flow tokens,
	 * or null if no flow token security.
	 * the first element in this list is the latest key.
	 * the remaining elements, if any, are unordered, but older than the first.
	 * when encoding a flow token, only the first (latest) key is used.
	 * when decoding a flow token, all keys are attempted, starting from the
	 * first (latest) until a successful decode or until all keys were tried.
	 */
	private volatile ArrayList<Secret> m_secretSet;

	/** the algorithm for message-authentication and key generation */
	private static final String HMACSHA1 = "HmacSHA1";

	/** thread-local byte array used for MAC calculations */
	private static final ThreadLocal<byte[]> s_workByteArray = new ThreadLocal<byte[]>();

	/** the duration, in milliseconds, to refresh the key set from configuration */
	// private static final long KEY_CACHE_TIME = 1000; //TODO - change it when we will decide on a mechanism to refresh the private key.

	/**
	 * the last key set refresh time as returned by System.currentTimeMillis().
	 * if multiple threads try to refresh concurrently, only one thread wins
	 * the race and modifies the value of this variable.
	 */
	// private final AtomicLong m_lastKeyRefresh; //TODO - enable when we will decide on a mechanism to refresh the private key.

	/**
	 * @return the singleton instance
	 */
	public static FlowTokenSecurity instance() {
		return s_instance;
	}

	/**
	 * private constructor
	 */
	private FlowTokenSecurity() {
		m_standalone = true;/*TODO Liberty (as we don't support HA ?)SipContainer.isRunningInWAS()
			? !SipClusterUtil.isServerInCluster()
			: true;*/
		ArrayList<Secret> secretSet = initializeSecretSet();
		m_secretSet = secretSet;
		// m_lastKeyRefresh = new AtomicLong(System.currentTimeMillis()); //TODO - enable when we will decide on a mechanism to refresh the private key.
		logSecretKeyInitResult();
	}
	
	/**
	 * called from the constructor to initialize the secret key set. this set
	 * is used for authenticating the flow token, according to 5626-5.2.
	 * 
	 * @return the secret key set, or null on error.
	 */
	private final ArrayList<Secret> initializeSecretSet() {
		ArrayList<Secret> secretSet;
		if (m_standalone) {
			// in a standalone container, generate the secret key manually.
			Key key = generateKey();
			if (key == null) {
				secretSet = null;
			}
			else {
				Mac mac = initializeMac(key);
				if (mac == null) {
					secretSet = null;
				}
				else {
					Secret secret = new Secret(key, mac);
					secretSet = new ArrayList<Secret>(1);
					secretSet.add(secret);
				}
			}
		}
		else {
			// in cluster, no use in generating the key, as it must be
			// uniform across all application servers in the cluster.
			if (s_logger.isWarnEnabled()) {
				s_logger.warn("warn.sip.outbound.no.key.set", null);
			}
			secretSet = null;
		}
		return secretSet;
	}

	/**
	 * called from the constructor to generate the flow token secret key,
	 * if running in standalone.
	 * @return the secret key, or null on error
	 */
	private final Key generateKey() {
		SecretKey key;
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance(HMACSHA1);
			keyGenerator.init(8*20); // 20-octet crypto random key
			key = keyGenerator.generateKey();
		}
		catch (NoSuchAlgorithmException e) {
			if (s_logger.isErrorEnabled()) {
				s_logger.error("error.exception", null, null, e);
			}
			key = null;
		}
		return key;
	}

	/**
	 * initializes the MAC (message authentication code) used for
	 * hashing flow tokens.
	 * may get called from the constructor.
	 * @param key a pre-initialized secret key to base the MAC on
	 * @return the MAC, or null on error
	 */
	private final Mac initializeMac(Key key) {
		Mac mac;
		try {
			mac = Mac.getInstance(HMACSHA1);
			mac.init(key);
		}
		catch (GeneralSecurityException e) {
			if (s_logger.isErrorEnabled()) {
				s_logger.error("error.exception", null, null, e);
			}
			mac = null;
		}
		return mac;
	}
	
	/**
	 * gets the latest key for hashing a flow token.
	 * @return the latest key, or null if flow token security is disabled
	 */
	Secret getLatestSecret() {
		// refreshKeySetIfNeeded(); //TODO - enable when we will decide on a mechanism to refresh the private key.
		ArrayList<Secret> secretSet = m_secretSet;
		if (secretSet == null) {
			return null; // flow token security is disabled
		}
		Secret latestSecret = secretSet.get(0);
		return latestSecret;
	}

	/**
	 * called periodically to check if need to refresh the key.
	 * if it was, creates a new secret and add it to the top of the secret set.
	 * @return false if the secret set was not updated, true if not.
	 *  a return value of true indicates a new reference was added to m_secretSet. 
	 */
	//TODO - enable when we will decide on a mechanism to refresh the private key.
	/*private boolean refreshKeySetIfNeeded() {
		ArrayList<Secret> secretSet = m_secretSet;
		if (secretSet == null) {
			return false; // flow token security is disabled
		}
		long now = System.currentTimeMillis();
		long lastKeyRefresh = m_lastKeyRefresh.get();
		if (now - lastKeyRefresh < KEY_CACHE_TIME) {
			return false; // still fresh
		}
		// time to refresh
		if (!m_lastKeyRefresh.compareAndSet(lastKeyRefresh, now)) {
			// some other thread is calling this same method right now.
			// let that other thread do the work.
			return false;
		}
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug(this, "refreshKeySetIfNeeded",
				"checking if refresh is needed");
		}
		
		Secret newSecret = generateSecret();
		if  (newSecret != null) {
			m_secretSet.add(0, newSecret);
		}
		return true;
	}*/
	
	
	/**
	 * Generates a secret.
	 * @return a new secret
	 */
	//TODO - enable when we will decide on a mechanism to refresh the private key.
	/*private Secret generateSecret() {
		Key key = generateKey();
		if (key == null) {
			return null;
		}
		else {
			Mac mac = initializeMac(key);
			if (mac == null) {
				return null;
			}
			else {
				Secret secret = new Secret(key, mac);
				return secret;
			}
		}
	}*/

	/**
	 * called from the constructor after completing secret key initialization,
	 * just to log the result.
	 */
	private final void logSecretKeyInitResult() {
		ArrayList<Secret> secretSet = m_secretSet;
		if (secretSet == null) {
			if (s_logger.isErrorEnabled()) {
				s_logger.error("error.sip.outbound.failure",null, null);
			}
		}
		else {
			if (s_logger.isInfoEnabled()) {
				s_logger.info("info.sip.outbound.initialized", null);
			}
			if (s_logger.isTraceDebugEnabled()) {
				StringBuilder b = new StringBuilder(1024);
				int size = secretSet.size();
				b.append("Flow token security initialized with [" + size + "] key(s):\r\n");
				for (int i = 0; i < size; i++) {
					Secret secret = secretSet.get(i);
					b.append(i).append(": ").append(secret.toString()).append("\r\n");
				}
				s_logger.traceDebug(this, "logSecretKeyInitResult", b.toString());
			}
		}
	}

	/**
	 * called when decoding a flow token, to authenticate the MAC of the given
	 * byte array. the byte array contains data and MAC, as originally encoded.
	 * <p>
	 *  +--------+-----+
	 *  |  data  | MAC |
	 *  +--------+-----+
	 * </p>
	 * this method calculates MAC(data) and compares it against MAC. if this
	 * fails, it could be that the flow token was encoded with an older key
	 * than the one we try to decode here, so we move on to the next key in
	 * our secret key history list. if the MAC passes for any key in our
	 * list, it is valid. 
	 * 
	 * @param secrets a set of one or more keys.
	 *  the first in the list is the latest. we try one by one
	 *  until a successful validation.
	 * @param byteArray byte array containing the entire flow token, after
	 *  base-64 decode.
	 * @param offset offset into the byte array where the flow token begins
	 * @param macOffset offset into the byte array, at the position where
	 *  the MAC value starts. we try to match the hash of all bytes between
	 *  offset and macOffset, against the MAC value that starts at macOffset.
	 * @param macLength the size of the MAC value
	 * @return true if MAC is valid, false if the flow token was tampered with
	 */
	boolean authenticateMac(ArrayList<Secret> secrets,
		byte[] byteArray, int offset, int macOffset, int macLength)
	{
		int nSecrets = secrets.size();
		for (int iSecret = 0; iSecret < nSecrets; iSecret++) {
			Secret secret = secrets.get(iSecret);
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "authenticateMac",
					"authenticating MAC by: " + secret);
			}
			Mac mac = secret.m_mac;
			byte[] expectedMac = calculateMac(byteArray, offset, macOffset, mac, macLength);

			boolean match = true;
			for (int i = 0; i < macLength; i++) {
				byte expectedByte = expectedMac[i];
				byte givenByte = byteArray[macOffset + i];
				if (givenByte != expectedByte) {
					if (s_logger.isTraceDebugEnabled()) {
						s_logger.traceDebug(this, "authenticateMac",
							"no match: " + secret);
					}
					match = false;
					break; // iterate to the next key
				}
			}
			if (match) {
				if (s_logger.isTraceDebugEnabled()) {
					s_logger.traceDebug(this, "authenticateMac",
						"match: " + secret);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * calculates a MAC for the given byte array
	 * @param array source byte array
	 * @param offset offset into the source byte array
	 * @param length byte array size
	 * @param mac pre-initialized MAC
	 * @param neededSize the number of bytes to write in the result.
	 * @return the MAC result. this is a thread-local array, valid only in the
	 *  same thread and only until the next call of this method.
	 *  if the MAC size is smaller than neededSize, it is
	 *  padded with 0s. if the MAC size is larger than truncateSize, it is truncated
	 *  as specified in RFC 2104-5.
	 */
	byte[] calculateMac(byte[] array, int offset, int length,
		Mac mac, int neededSize)
	{
		// get a thread-local byte array
		int macSize = mac.getMacLength();
		byte[] macArray = s_workByteArray.get();
		if (macArray == null || macArray.length < macSize) {
			macArray = new byte[macSize];
			s_workByteArray.set(macArray);
		}

		// do the MAC internal calculation, and output to the byte array
		synchronized (mac) {
			mac.update(array, offset, length);

			try {
				mac.doFinal(macArray, 0);
			}
			catch (ShortBufferException e) {
				// getting here is a bug
				if (s_logger.isTraceFailureEnabled()) {
					s_logger.traceFailure(this, "calculateMac",
						"failed writing MAC of [" + macSize +
						"] to byte array of size [" + macArray.length + ']',
						e);
				}
				throw new RuntimeException(e);
			}
		}

		// pad with 0s if needed
		while (macSize < neededSize) {
			macArray[macSize++] = 0;
		}
		return macArray;
	}

	/**
	 * @return set secret keys used to secure flow tokens,
	 *  or null if no flow token security.
	 */
	ArrayList<Secret> getSecretSet( ){
		return m_secretSet;
	}
}
