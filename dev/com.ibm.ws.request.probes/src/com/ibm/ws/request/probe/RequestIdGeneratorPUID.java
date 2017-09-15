/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.probe;

import java.util.concurrent.atomic.AtomicLong;



public class RequestIdGeneratorPUID {

	// 64 chars of Base64 http://www.ietf.org/rfc/rfc2045.txt
	private final static char Base64[] = new char[] {
			'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
			'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
			'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
			'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
	};
	private final static String startTimeAlphaNumeric;
	private final static char UNDERSCORE='_';
	private final static AtomicLong sequence = new AtomicLong();

	static {
		// Initialize prefix unique to this JVM run
		startTimeAlphaNumeric = toBase64(System.nanoTime()).append(UNDERSCORE).toString();
	}

	public RequestId getNextRequestId()
	{
		
		
		long sequenceNumber = sequence.getAndIncrement();
	    String id = toBase64(sequenceNumber).insert(0,startTimeAlphaNumeric).toString();
	    RequestId requestId = new RequestId(sequenceNumber + 1, id);
	    return requestId;
	}

	/**
	 * Return 11 chars of Base64 string for the 64 bits of the value padded with 2 zero bits
	 * 
	 * @param value long value to translate
	 * @return StringBuilder instance containing characters of the result Base64 string
	 */
	private static StringBuilder toBase64(long value) {
		StringBuilder result = new StringBuilder(23); // Initialize with the size of ID if using Base64
		for(int shift=60; shift>=0; shift-=6) {
			result.append(Base64[(int)((value >> shift) & 0x3F)]);
		}
		return result;
	}

	
}




