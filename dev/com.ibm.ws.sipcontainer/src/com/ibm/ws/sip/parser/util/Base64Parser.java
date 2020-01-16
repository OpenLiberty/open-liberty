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

package com.ibm.ws.sip.parser.util;

/**
 * Base 64 encoder and decoder
 * 
 * @author ran
 * @see RFC 2045 6.8
 */
public class Base64Parser
{
	/** conversion table from integer (0-63) to byte */
	private static final char[] s_encodeTable = {
		'A','B','C','D','E','F','G','H','I','J','K','L','M',
		'N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
		'a','b','c','d','e','f','g','h','i','j','k','l','m',
		'n','o','p','q','r','s','t','u','v','w','x','y','z',
		'0','1','2','3','4','5','6','7','8','9','+','/'
	};

	/** conversion table from byte to integer */
	private static final byte[] s_decodeTable = {
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54,
		55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1,  0,  1,  2,
		 3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
		20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30,
		31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
		48, 49, 50, 51
	};

	/** thread-local temporary work buffer used during encoding */
	private static final ThreadLocal<StringBuilder> s_workBuffer =
		new ThreadLocal<StringBuilder>() {
			protected StringBuilder initialValue() {
				return new StringBuilder(64);
			}
		};

	/**
	 * encoding routine
	 * @param in input byte array
	 * @return the base-64 encoded string
	 */
	public static String encode(byte[] in) {
		StringBuilder out = s_workBuffer.get();
		out.setLength(0);

		int inLength = in.length;
		int nFullGroups = inLength / 3;
		int nBytesInPartialGroup = inLength - 3*nFullGroups;

		int inIndex = 0;
		for (int i = 0; i < nFullGroups; i++) {
		    int byte0 = in[inIndex++] & 0xff;
		    int byte1 = in[inIndex++] & 0xff;
		    int byte2 = in[inIndex++] & 0xff;
		    out.append(s_encodeTable[byte0 >> 2]);
		    out.append(s_encodeTable[(byte0 << 4) & 0x3f | (byte1 >> 4)]);
		    out.append(s_encodeTable[(byte1 << 2) & 0x3f | (byte2 >> 6)]);
		    out.append(s_encodeTable[byte2 & 0x3f]);
		}

		if (nBytesInPartialGroup != 0) {
			int byte0 = in[inIndex++] & 0xff;
			out.append(s_encodeTable[byte0 >> 2]);
			if (nBytesInPartialGroup == 1) {
				out.append(s_encodeTable[(byte0 << 4) & 0x3f]);
				out.append("==");
			}
			else {
				int byte1 = in[inIndex++] & 0xff;
				out.append(s_encodeTable[(byte0 << 4) & 0x3f | (byte1 >> 4)]);
				out.append(s_encodeTable[(byte1 << 2) & 0x3f]);
				out.append('=');
			}
		}
		return out.toString();
	}

	/**
	 * decoding routine
	 * @param in input string
	 * @return the decoded byte array, or null if input string is not
	 *  a base-64 encoded string
	 */
	public static byte[] decode(CharSequence in) {
		int inLength = in.length();
		int nGroups = inLength / 4;
		if (inLength != 4*nGroups) {
			return null;
		}
		int missingBytesInLastGroup = 0;
		int nFullGroups = nGroups;
		if (inLength != 0) {
			if (in.charAt(inLength-1) == '=') {
				missingBytesInLastGroup++;
				nFullGroups--;
			}
			if (in.charAt(inLength-2) == '=') {
				missingBytesInLastGroup++;
			}
		}
		byte[] result = new byte[3*nGroups - missingBytesInLastGroup];

		int inIndex = 0;
		int outIndex = 0;
		for (int i = 0; i < nFullGroups; i++) {
			int ch0 = s_decodeTable[in.charAt(inIndex++)];
			int ch1 = s_decodeTable[in.charAt(inIndex++)];
			int ch2 = s_decodeTable[in.charAt(inIndex++)];
			int ch3 = s_decodeTable[in.charAt(inIndex++)];
			if (ch0 < 0 || ch1 < 0 || ch2 < 0 || ch3  < 0) {
				return null;
			}
			result[outIndex++] = (byte)((ch0 << 2) | (ch1 >> 4));
			result[outIndex++] = (byte)((ch1 << 4) | (ch2 >> 2));
			result[outIndex++] = (byte)((ch2 << 6) | ch3);
		}

		if (missingBytesInLastGroup != 0) {
			int ch0 = s_decodeTable[in.charAt(inIndex++)];
			int ch1 = s_decodeTable[in.charAt(inIndex++)];
			if (ch0 < 0 || ch1 < 0) {
				return null;
			}
			result[outIndex++] = (byte)((ch0 << 2) | (ch1 >> 4));

			if (missingBytesInLastGroup == 1) {
				int ch2 = s_decodeTable[in.charAt(inIndex++)];
				if (ch2 < 0) {
					return null;
				}
				result[outIndex++] = (byte)((ch1 << 4) | (ch2 >> 2));
			}
		}
		return result;
	}
}
