/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.util;

import java.io.*;

import com.ibm.ws.sip.parser.SipConstants;

public class OutboundInterface implements Externalizable {
	int _ifaceIdxUDP; 
	int _ifaceIdxTCP; 
	int _ifaceIdxTLS;
	
	public OutboundInterface() {
		_ifaceIdxUDP = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED;
		_ifaceIdxTCP = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED;
		_ifaceIdxTLS = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED;
	}

	public OutboundInterface(int ifaceIdxUDP, int ifaceIdxTCP, int ifaceIdxTLS) {
		_ifaceIdxUDP = ifaceIdxUDP;
		_ifaceIdxTCP = ifaceIdxTCP;
		_ifaceIdxTLS = ifaceIdxTLS;
	}
	
	public boolean isSet() {
		if (_ifaceIdxUDP != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED ||
				_ifaceIdxTCP != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED ||
				_ifaceIdxTLS != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
			return true;
		}
		return false;
	}

	public int getOutboundInterface(String transport) {
		int returnVal;

		if (transport == null || transport.equalsIgnoreCase("udp")) {
			returnVal = _ifaceIdxUDP;
		}
		else if (transport.equalsIgnoreCase("tcp")) {
			returnVal = _ifaceIdxTCP;
		}
		else {
			returnVal = _ifaceIdxTLS;
		}
		return returnVal;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("UDP interface index: ");
		sb.append(_ifaceIdxUDP);
		sb.append(" ,TCP interface index: ");
		sb.append(_ifaceIdxTCP);
		sb.append(" ,TLS interface index: ");
		sb.append(_ifaceIdxTLS);
		
		return sb.toString();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(_ifaceIdxUDP);
		out.writeInt(_ifaceIdxTCP);
		out.writeInt(_ifaceIdxTLS);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		_ifaceIdxUDP = in.readInt();
		_ifaceIdxTCP = in.readInt();
		_ifaceIdxTLS = in.readInt();
		
	}
}