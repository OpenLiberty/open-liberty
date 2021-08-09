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
package com.ibm.ws.sip.stack.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.parser.util.InetAddressCache;

/**
 * Stateless class with static utility methods for operations on addresses
 * 
 * @author ran
 */
public class AddressUtils
{
	/** class logger */
	private static final LogMgr s_logger = Log.get(AddressUtils.class);

	/**
	 * work buffers used for converting a string address to a byte array.
	 * each thread has two work buffers, one work buffer for 16-byte
	 * addresses, and one for 4-byte addresses.
	 */
	private static final ThreadLocal<byte[]> s_ipv4workBuffer =
		new ThreadLocal<byte[]>()
	{
		protected byte[] initialValue() {
			return new byte[4];
		}
	};
	private static final ThreadLocal<byte[]> s_ipv6workBuffer =
		new ThreadLocal<byte[]>()
	{
		protected byte[] initialValue() {
			return new byte[16];
		}
	};

	/** work buffer used for converting Inet4Address to a byte array */
	private static final ThreadLocal<byte[]> s_inet4addressNumericValue =
		new ThreadLocal<byte[]>()
	{
		protected byte[] initialValue() {
			return new byte[4];
		}
	};

	/** work buffer used for converting a byte array to a string */
	private static final ThreadLocal<StringBuilder> s_stringBuilder =
		new ThreadLocal<StringBuilder>()
	{
		protected StringBuilder initialValue() {
			int maxSize = "0000:0000:0000:0000:0000:0000:0000:0000".length();
			return new StringBuilder(maxSize);
		}
	};

	/** the IP address with a numeric value of 0 */
	private static final byte[] s_zeroAddress = { 0, 0, 0, 0 };

	/**
	 * Compares two addresses, either IP or host name.
	 * If the two addresses are host names, this is just a string comparison.
	 * If the addresses are IP addresses, this compares the numeric values.
	 * A host name never equals an IP address.
	 * 
	 * @param address1 a host name or IP addresses
	 * @param address2 a host name or IP addresses
	 * @return true if the addresses are equal, false otherwise
	 */
	public static boolean equals(String address1, String address2) {
		// 1. handle obvious cases
		if (address1.equalsIgnoreCase(address2)) {
			return true; // same string
		}

		// 2. strings are not equal. with IPv6, there is still a chance that
		//    the numeric value is equal.
		boolean address1isIPv6 = address1.indexOf(':') != -1;
		boolean address2isIPv6 = address2.indexOf(':') != -1;
		if (!address1isIPv6 && !address2isIPv6) {
			// neither is IPv6 and strings are different. not equal.
			return false;
		}

		// 3. at least one of the addresses looks like IPv6.
		//    convert address1 to numeric value
		byte[] workBuffer = address1isIPv6
			? s_ipv6workBuffer.get()
			: s_ipv4workBuffer.get();
		boolean _1_isValid = address1isIPv6
			? convertIPv6(address1, workBuffer)
			: convertIPv4(address1, workBuffer);
		if (!_1_isValid) {
			// address1 is not an IP address. not equal.
			return false;
		}

		// 4. compare address2 with the numeric value of address1
		if (compareIP(address2, workBuffer)) {
			return true;
		}
		return false;
	}

	/**
	 * Compares two IP addresses, either IPv4 or IPv6.
	 * An address represented as IPv6 may equal an address represented as IPv4
	 * if the 12 significant octets in the IPv6 address are 0.
	 * For example [::1.2.3.4] equals 1.2.3.4
	 * 
	 * @param address1 an IP address, either IPv4 or IPv6
	 * @param address2 an IP address, either IPv4 or IPv6
	 * @return true if the numeric value equals, false otherwise
	 */
	public static boolean compare(byte[] address1, byte[] address2) {
		if (address1.length < address2.length) {
			// switch so that address1 is not smaller than address2
			byte[] tempAddress = address1;
			address1 = address2;
			address2 = tempAddress;
		}
		int i1 = address1.length;
		int i2 = address2.length;

		while (i1 > 0) {
			byte b1 = address1[--i1];
			byte b2 = i2 > 0 ? address2[--i2] : 0;
			if (b1 != b2) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks whether given string represents the "0" IP address.
	 * That is 0.0.0.0 for IPv4 or :: (or equivalent) for IPv6
	 * 
	 * @param address the string containing an IP address
	 * @return true if "0" address, false otherwise
	 */
	public static boolean isZeroAddress(CharSequence address) {
		return compareIP(address, s_zeroAddress);
	}

	/**
	 * Checks whether given string represents a legal IP address,
	 * either IPv4 or IPv6
	 * 
	 * @param address string to test
	 * @return true if it looks like a valid IP address, otherwise false
	 */
	public static boolean isIpAddress(CharSequence address) {
		return isIPv4address(address) || isIPv6address(address);
	}

	/**
	 * Compares an IP address in string representation with an address in
	 * byte array representation.
	 * 
	 * @param address String representation of the address
	 * @param bytes A byte array of either 4 or 16 elements containing the
	 *  address to compare with
	 * @return true if the two addresses are valid and equal, false otherwise
	 */
	public static boolean compareIP(CharSequence address, byte[] bytes) {
		switch (bytes.length) {
		case 4:
			// try IPv4 first, then IPv6
			return ipv4(address, bytes, true)
				|| ipv6(address, bytes, true);
		case 16:
			// try IPv6 first, then IPv4
			return ipv6(address, bytes, true)
				|| ipv4(address, bytes, true);
		default:
			return false;
		}
	}

	/**
	 * Converts an IP address in string representation to a byte array
	 * 
	 * If the address is IPv4, the returned byte array is of size 4.
	 * If the address is IPv6, the returned byte array is of size 16.
	 * Otherwise, the returned value is null
	 * 
	 * @param address an IP address, either IPv4 or IPv6
	 * @return the address as a byte array, either 4 or 16 bytes, or null
	 *  if the given string is not a valid IP address.
	 *  the returned value must not be passed to other threads.
	 */
	public static byte[] convertIP(String address) {
		boolean addressIsIPv6 = address.indexOf(':') != -1;
		byte[] workBuffer = addressIsIPv6
			? s_ipv6workBuffer.get()
			: s_ipv4workBuffer.get();
		boolean result = addressIsIPv6
			? convertIPv6(address, workBuffer)
			: convertIPv4(address, workBuffer);
		return result ? workBuffer : null;
	}

	/**
	 * Converts an IP address in string representation to a byte array
	 * 
	 * @param address string representation of the address
	 * @param bytes A byte array of 4 or 16 elements. Upon successful return
	 *  from this call, the byte array is filled with the address octets. If
	 *  the call fails, the state of this array is undefined.
	 * @return true on success, false on failure. The only reason for failure
	 *  is a bad IP address in the input string.
	 */
	public static boolean convertIP(CharSequence address, byte[] bytes) {
		return convertIPv4(address, bytes) || convertIPv6(address, bytes);
	}

	/**
	 * Checks whether given string is a valid IPv4 address
	 * @param address string to test
	 * @return true if a valid IPv4 address, otherwise false
	 */
	public static boolean isIPv4address(CharSequence address) {
		return ipv4(address, null, false);
	}

	/**
	 * Compares an IPv4 address in string representation with an address in
	 * byte array representation.
	 * 
	 * @param address string representation of the address
	 * @param bytes A byte array of 4 or 16 elements containing the address to
	 *  compare with
	 * @return true if the two addresses are valid and equal, false otherwise
	 */
	public static boolean compareIPv4(CharSequence address, byte[] bytes) {
		if (bytes.length != 4 && bytes.length != 16) {
			return false;
		}
		return ipv4(address, bytes, true);
	}

	/**
	 * Converts an IPv4 address in string representation to a byte array
	 * 
	 * @param address string representation of the address
	 * @param bytes A byte array of 4 or 16 elements. Upon successful return
	 *  from this call, the byte array is filled with the address octets. If
	 *  the call fails, the state of this array is undefined.
	 * @return true on success, false on failure. The only reason for failure
	 *  is a bad IPv4 address in the input string.
	 */
	public static boolean convertIPv4(CharSequence address, byte[] bytes) {
		if (bytes.length != 4 && bytes.length != 16) {
			return false;
		}
		return ipv4(address, bytes, false);
	}

	/**
	 * Common code for analyzing, comparing, and converting a string
	 * representation of an IPv4 address
	 * 
	 * @param address string representation of the address
	 * @param bytes Array of 4 or 16 bytes:
	 *  - null if just validating the address
	 *  - an input array if comparing the string address to the byte array.
	 *    comparing with a 16 byte array may only succeed only if the 12 most
	 *    significant bytes are 0. 
	 *  - an output array if converting the string address to a byte array.
	 *    if the output array is 16 bytes, conversion sets the 12
	 *    most significant bytes to 0.
	 * @param compare Only applicable if <code>bytes != null</code>.
	 *  If true, compares <code>bytes</code> with the given
	 *  address. If false, fills <code>bytes</code> with the parsed address
	 * @return If <code>bytes != null</code> and <code>compare == true</code>
	 *  returns true if the string represents the same address as in the
	 *  byte array.
	 *  If <code>bytes != null</code> and <code>compare == false</code>
	 *  returns true if successfully converted the address to a byte array.
	 *  If <code>bytes == null</code>, returns true if the address is a valid
	 *  IPv4 address.
	 */
	private static boolean ipv4(CharSequence address, byte[] bytes, boolean compare) {
		final int start = 0;
		final int len = address.length();
		int digit;
		int octet = 0;
		int nOctets = 0;
		final int offset = bytes != null && bytes.length == 4 ? 0 : 12;

		if (bytes != null && bytes.length == 16 && compare) {
			// IPv4 address compared with 16-byte array. verify 12 0s
			for (int i = 0; i < 12; i++) {
				if (bytes[i] != 0) {
					return false;
				}
			}
		}

		for (int i = start; i <= len; i++) {
			// treat the char beyond last as another "."
			char c = i < len ? address.charAt(i) : '.';
			switch (c) {
			case '1': case '2': case '3': case '4': case '5':
			case '6': case '7': case '8': case '9': case '0':
				digit = c - '0';
				octet = 10*octet + digit;
				if (octet > 255) {
					return false; // expected 8-bit number
				}
				break;
			case '.':
				if (nOctets > 3) {
					return false; // 4 octets already, can't have 5
				}
				if (i == len-1) {
					return false; // address ends with a period
				}
				if (compare) {
					if (bytes[offset + nOctets] != (byte)octet) {
						return false;
					}
				}
				else if (bytes != null) { // convert
					bytes[offset + nOctets] = (byte)octet;
				}
				nOctets++;
				octet = 0;
				break;
			default:
				// found non-IPv4 character
				return false;
			}
		}
		if (nOctets != 4) {
			return false;
		}
		if (bytes != null && bytes.length == 16 && !compare) {
			// IPv4 address converted into a 16-byte array. pad with 0s.
			for (int i = 0; i < 12; i++) {
				bytes[i] = 0;
			}
		}
		return true;
	}

	/**
	 * Checks whether given string is a valid IPv6 address
	 * @param address string to test
	 * @return true if a valid IPv6 address, otherwise false
	 */
	public static boolean isIPv6address(CharSequence address) {
		return ipv6(address, null, false);
	}

	/**
	 * Compares an IPv6 address in string representation with an address in
	 * byte array representation.
	 * 
	 * @param address string representation of the address
	 * @param bytes A byte array of 16 or 4 elements containing the address to
	 *  compare with
	 * @return true if the two addresses are valid and equal, false otherwise
	 */
	public static boolean compareIPv6(CharSequence address, byte[] bytes) {
		if (bytes.length != 4 && bytes.length != 16) {
			return false;
		}
		return ipv6(address, bytes, true);
	}

	/**
	 * Converts an IPv6 address in string representation to a byte array
	 * 
	 * @param address string representation of the address
	 * @param bytes A byte array of 16 or 4 elements. Upon successful return
	 *  from this call, the byte array is filled with the address octets. If
	 *  the call fails, the state of this array is undefined.
	 * @return true on success, false on failure. The only reason for failure
	 *  is a bad IPv6 address in the input string.
	 */
	public static boolean convertIPv6(CharSequence address, byte[] bytes) {
		if (bytes.length != 4 && bytes.length != 16) {
			return false;
		}
		return ipv6(address, bytes, false);
	}

	/**
	 * Common code for analyzing, comparing, and converting a string
	 * representation of an IPv6 address
	 * 
	 * @param address string representation of the address
	 * @param bytes Array of 16 or 4 bytes:
	 *  - null if just validating the address
	 *  - an input array if comparing the string address to the byte array.
	 *    comparing with a 4 byte array may only succeed only if the 12 most
	 *    significant bytes are 0. 
	 *  - an output array if converting the string address to a byte array.
	 *    if the output array is 4 bytes, conversion succeeds only if the 12
	 *    most significant bytes are 0.
	 * @param compare Only applicable if <code>bytes != null</code>.
	 *  If true, compares <code>bytes</code> with the given
	 *  address. If false, fills <code>bytes</code> with the parsed address
	 * @return If <code>bytes != null</code> and <code>compare == true</code>
	 *  returns true if the string represents the same address as in the
	 *  byte array.
	 *  If <code>bytes != null</code> and <code>compare == false</code>
	 *  returns true if successfully converted the address to a byte array.
	 *  If <code>bytes == null</code>, returns true if the address is a valid
	 *  IPv4 address.
	 */
	private static boolean ipv6(CharSequence address, byte[] bytes, boolean compare) {
		// 1. preprocess. remove the optional square brackets, count components,
		//    determine if compression (::) is present, determine if IPv4
		//    part is present.
		int start = 0;
		int end = address.length();
		boolean openBracket = false; // found "["
		boolean closeBracket = false; // found "]"
		int compression = -1; // index of :: (the first of the two)
		boolean mixed; // is there an IPv4 part
		int nDots = 0; // number of "."
		int nHex = 0; // number of hex sequences
		int lastColon = -1; // index of last ":"
		boolean component = false; // last char was part of a number sequence

		for (int i = start; i < end; i++) {
			char c = address.charAt(i);
			switch (c) {
			case '[':
				if (i != start) {
					return false; // found "[" in the middle
				}
				if (openBracket) {
					return false; // found second "["
				}
				// starts with "["
				openBracket = true;
				break;
			case ']':
				if (closeBracket) {
					return false; // found second "]"
				}
				if (!openBracket) {
					return false; // found "]" with no preceding "["
				}
				if (i < end-1) {
					return false; // more chars after the "]" 
				}
				closeBracket = true;
				break;
			case ':':
				if (nDots > 0) {
					return false; // can't have ":" after the IPv4 part
				}
				if (i < end-1 && address.charAt(i+1) == ':') {
					// found "::"
					if (compression != -1) {
						return false; // ":::" or second "::"
					}
					compression = i;
				}
				if (component) {
					component = false;
					if (++nHex > 8) {
						// can't have 9 components
						return false;
					}
				}
				lastColon = i;
				break;
			case '.':
				nDots++;
				break;
			default:
				component =
					('a' <= c && c <= 'z') ||
					('A' <= c && c <= 'Z') ||
					('0' <= c && c <= '9');
			}
		}
		if (openBracket && !closeBracket) {
			return false; // "[" with no "]"
		}
		if (openBracket) {
			// remove square brackets
			start++;
			end--;
		}
		if (end < 2) {
			return false;
		}
		switch (nDots) {
		case 0:
			mixed = false; // no IPv4 address
			break;
		case 3:
			mixed = true; // IPv4 present
			break;
		default:
			return false; // bad IPv4 part
		}
		if (component && !mixed) {
			// count last component as another hex sequence
			nHex++;
		}

		if (compression == -1) {
			if (mixed) {
				if (nHex != 6) {
					return false; // no compression and mixed, must have 6 hex sequences
				}
			}
			else {
				if (nHex != 8) {
					return false; // no compression and not mixed, must have 8 hex sequences
				}
			}
		}
		else {
			if (mixed) {
				if (nHex > 5) {
					return false; // compression and mixed, can't exceed 5 hex sequences
				}
			}
			else {
				if (nHex > 7) {
					return false; // compression and not mixed, can't exceed 7 hex sequences
				}
			}
		}

		// now we know:
		// there is at most one "::"
		// there are either 0 or 3 "."
		// there are no ":" following the "."
		// there are no square brackets
		// the total number of octets is exactly 16

		// 2. process
		boolean v4 = false; // set to true if reading the IPv4 part of the IPv6 address
		int digit = 0; // 8 bit or 16 bit digit
		int dec = 0; // 8 bit octet in IPv4 part
		int hex = 0; // 16 bit hex number
		int iBytes = 0; // index into byte array

		for (int i = start; i <= end; i++) {
			// treat the char beyond last as another ":" or "."
			char c = i < end ? address.charAt(i) : (v4 ? '.' : ':');
			switch (c) {
			case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
				// change uppercase to lowercase, and fall-through to lowercase
				c += ('a' - 'A');
			case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
				if (v4) {
					return false; // expected 8 bit digit, found 16 bit digit
				}
				digit = 10 + c - 'a';
				hex = 16*hex + digit;
				if (hex > 0xffff) {
					return false; // expected 16-bit number
				}
				break;
			case '1': case '2': case '3': case '4': case '5':
			case '6': case '7': case '8': case '9': case '0':
				digit = c - '0';
				if (v4) {
					dec = 10*dec + digit;
					if (dec > 255) {
						return false; // expected 8-bit number
					}
				}
				else {
					hex = 16*hex + digit;
					if (hex > 0xffff) {
						return false; // expected 16-bit number
					}
				}
				break;
			case ':':
				if (i > start) {
					if (address.charAt(i-1) == ':') { // "::"
						// decompress 0s
						if (bytes != null) {
							int zeros = 16 - 2*nHex - (mixed ? 4 : 0);
							while (zeros > 0) {
								if (compare) {
									if (byteAt(bytes, iBytes++) != 0) {
										return false;
									}
								}
								else {
									if (!setByte(bytes, iBytes++, (byte)0)) {
										return false;
									}
								}
								zeros--;
							}
							if (iBytes == 16) {
								// ends with :: so don't keep going
								return true;
							}
						}
					}
					else {
						// end of 16-bit hex sequence
						if (bytes != null) {
							byte b1 = (byte)((hex & 0xff00) >> 8);
							byte b2 = (byte)(hex & 0x00ff);
							if (compare) {
								if (byteAt(bytes, iBytes++) != b1 ||
									byteAt(bytes, iBytes++) != b2)
								{
									return false;
								}
							}
							else {
								if (!setByte(bytes, iBytes++, b1)) {
									return false;
								}
								if (!setByte(bytes, iBytes++, b2)) {
									return false;
								}
							}
						}
						hex = 0;
					}
					if (mixed && i == lastColon) {
						v4 = true; // IPv4 part starts here
					}
				}
				break;
			case '.':
				if (bytes != null) {
					if (compare) {
						if (byteAt(bytes, iBytes++) != (byte)dec) {
							return false;
						}
					}
					else {
						if (!setByte(bytes, iBytes++, (byte)dec)) {
							return false;
						}
					}
				}
				dec = 0;
				break;
			default:
				// found non-IPv6 character
				return false;
			}
		}
		return true;
	}

	/**
	 * gets the value of a byte in a byte array, given the index.
	 * the byte array is either 4 or 16 bytes long, but the index is always 0-15.
	 * with a 16 byte array, this just returns the byte at the given index.
	 * with a 4 byte array, this call first "normalizes" the 4 byte array into
	 * a 16 byte array, by shifting the given array 12 bytes to the right,
	 * padding the 12 extra bytes with zeros, and then returning the byte at
	 * the given index as if indexing a 16-byte array.
	 * 
	 * @param array a 4 or 16 byte array
	 * @param index index into a 16 byte array
	 * @return the byte at the given index
	 */
	private static byte byteAt(byte[] array, int index) {
		if (array.length == 16) {
			return array[index];
		}
		else { // array.length == 4
			index -= 12;
			if (index < 0) {
				// the 12 left bytes are padded with 0s
				return 0;
			}
			return array[index];
		}
	}

	/**
	 * sets a byte in a byte array at the specified index.
	 * the byte array is either 4 or 16 bytes long, but the index is always 0-15.
	 * with a 16 byte array, this just sets the byte at the given index.
	 * with a 4 byte array, the physical index is 12 positions to the left of
	 * the given index. bytes at the left 12 positions only succeeds for a byte
	 * value of 0.
	 * 
	 * @param array a 4 or 16 byte array
	 * @param index index into a 16 byte array
	 * @param b the new byte value
	 * @return true on success, false on failure
	 */
	private static boolean setByte(byte[] array, int index, byte b) {
		if (array.length == 16) {
			array[index] = b;
		}
		else { // array.length == 4
			index -= 12;
			if (index < 0) {
				// only 0 is allowed at the 12 left bytes
				return b == 0;
			}
			array[index] = b;
		}
		return true;
	}

	/**
	 * returns the local IP address. if security does not permit this,
	 *  or if some other failure occurs, returns "127.0.0.1"
	 * @return the local IP address
	 */
	public static String getLocalAddress() {
		InetAddress localHost;
		try {
			localHost = InetAddress.getLocalHost();
		}
		catch (UnknownHostException e) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(AddressUtils.class.getName(),
					"getLocalAddress",
					"error determining local address", e);
			}
			return "127.0.0.1";
		}
		String localAddress = InetAddressCache.getHostAddress(localHost);
		return localAddress;
	}

	/**
	 * Converts the given InetAddress to a byte array with equal numeric
	 * value. For IPv4 addresses this is done without allocating a new byte
	 * array. The returned value is only valid within the same calling thread,
	 * and until the next call to this method
	 * 
	 * @param address The address to retrieve its numeric value
	 * @return The numeric value as a byte array, of either 4 bytes (for IPv4)
	 * or 16 bytes (for IPv6). This instance is only valid within the same
	 * calling thread, and until the next call to any other method in this class.
	 */
	public static byte[] numericValue(InetAddress address) {
		byte[] bytes;
		if (address instanceof Inet4Address) {
			// no need to allocate a new byte array. InetAddress.hashCode()
			// returns the entire 32-bit address value.
			int addressValue = address.hashCode();
			bytes = s_inet4addressNumericValue.get();
			bytes[0] = (byte)((addressValue & 0xFF000000) >> 24);
			bytes[1] = (byte)((addressValue & 0x00FF0000) >> 16);
			bytes[2] = (byte)((addressValue & 0x0000FF00) >> 8);
			bytes[3] = (byte)(addressValue & 0x000000FF);
		}
		else {
			// with IPv6 there is no way to get the address value from the
			// InetAddress without calling this method, which allocates a new
			// byte array.
			bytes = address.getAddress();
		}
		return bytes;
	}

	/**
	 * converts a numeric IP address to a string, as a human-readable
	 * address, in either IPv4 or IPv6 format
	 * @param ip a numeric IP address in a byte array of either 4 a 16 bytes long
	 * @param offset offset into the byte array
	 * @param length number of bytes to read from the byte array, either 4 or 16
	 * @return the IP address as a string, or null on failure
	 */
	public static String convertIP(byte[] ip, int offset, int length) {
		StringBuilder b = s_stringBuilder.get();
		b.setLength(0);
		if (!convertIP(ip, offset, length, b)) {
			return null;
		}
		return b.toString();
	}

	/**
	 * writes a numeric IP address to a character buffer, as a human-readable
	 * address, in either IPv4 or IPv6 format
	 * @param ip a numeric IP address in a byte array of either 4 a 16 bytes long
	 * @param offset offset into the byte array
	 * @param length number of bytes to read from the byte array, either 4 or 16
	 * @param appendable destination character buffer
	 * @return true on success, false on failure
	 */
	public static boolean convertIP(byte[] ip, int offset, int length,
		Appendable appendable)
	{
		try {
			if (length == 16) {
				// IPv6
				char c1 = hex((ip[offset+0] & 0xF0) >> 4);
				char c2 = hex(ip[offset+0] & 0x0F);
				char c3 = hex((ip[offset+1] & 0xF0) >> 4);
				char c4 = hex(ip[offset+1] & 0x0F);
				char c5 = hex((ip[offset+2] & 0xF0) >> 4);
				char c6 = hex(ip[offset+2] & 0x0F);
				char c7 = hex((ip[offset+3] & 0xF0) >> 4);
				char c8 = hex(ip[offset+3] & 0x0F);
				char c9 = hex((ip[offset+4] & 0xF0) >> 4);
				char c10 = hex(ip[offset+4] & 0x0F);
				char c11 = hex((ip[offset+5] & 0xF0) >> 4);
				char c12 = hex(ip[offset+5] & 0x0F);
				char c13 = hex((ip[offset+6] & 0xF0) >> 4);
				char c14 = hex(ip[offset+6] & 0x0F);
				char c15 = hex((ip[offset+7] & 0xF0) >> 4);
				char c16 = hex(ip[offset+7] & 0x0F);
				char c17 = hex((ip[offset+8] & 0xF0) >> 4);
				char c18 = hex(ip[offset+8] & 0x0F);
				char c19 = hex((ip[offset+9] & 0xF0) >> 4);
				char c20 = hex(ip[offset+9] & 0x0F);
				char c21 = hex((ip[offset+10] & 0xF0) >> 4);
				char c22 = hex(ip[offset+10] & 0x0F);
				char c23 = hex((ip[offset+11] & 0xF0) >> 4);
				char c24 = hex(ip[offset+11] & 0x0F);
				char c25 = hex((ip[offset+12] & 0xF0) >> 4);
				char c26 = hex(ip[offset+12] & 0x0F);
				char c27 = hex((ip[offset+13] & 0xF0) >> 4);
				char c28 = hex(ip[offset+13] & 0x0F);
				char c29 = hex((ip[offset+14] & 0xF0) >> 4);
				char c30 = hex(ip[offset+14] & 0x0F);
				char c31 = hex((ip[offset+15] & 0xF0) >> 4);
				char c32 = hex(ip[offset+15] & 0x0F);
	
				appendable.append('[');
				appendable.append(c1).append(c2).append(c3).append(c4);
				appendable.append(':');
				appendable.append(c5).append(c6).append(c7).append(c8);
				appendable.append(':');
				appendable.append(c9).append(c10).append(c11).append(c12);
				appendable.append(':');
				appendable.append(c13).append(c14).append(c15).append(c16);
				appendable.append(':');
				appendable.append(c17).append(c18).append(c19).append(c20);
				appendable.append(':');
				appendable.append(c21).append(c22).append(c23).append(c24);
				appendable.append(':');
				appendable.append(c25).append(c26).append(c27).append(c28);
				appendable.append(':');
				appendable.append(c29).append(c30).append(c31).append(c32);
				appendable.append(']');
				return true;
			}
			else if (length == 4) {
				// IPv4
				for (int i = 0; i < length; i++) {
					if (i > 0) {
						appendable.append('.');
					}
					int octet = (int)ip[offset+i] & 255;
					int d1 = octet % 10;
					int d2 = (octet/=10) % 10;
					int d3 = (octet/=10) % 10;
					if (d3 != 0) {
						appendable.append((char)('0' + d3));
					}
					if (d3 != 0 || d2 != 0) {
						appendable.append((char)('0' + d2));
					}
					appendable.append((char)('0' + d1));
				}
				return true;
			}
			else {
				return false; // neither IPv4 or IPv6
			}
		}
		catch (IOException e) {
			if (s_logger.isTraceFailureEnabled()) {
				s_logger.traceFailure(AddressUtils.class.getName(),
					"appendIP", "", e);
			}
			return false;
		}
	}

	/**
	 * converts a number between 0-15 to a hexadecimal digit
	 * @param n number between 0-15
	 * @return the hexadecimal digit
	 */
	private static char hex(int n) {
		return n < 10 ? (char)('0' + n) : (char)('A' - 10 + n);
	}
}
