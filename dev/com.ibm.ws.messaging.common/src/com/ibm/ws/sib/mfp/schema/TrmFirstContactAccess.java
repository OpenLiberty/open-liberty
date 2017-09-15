/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// Generated from C:\SIB\Code\WASX.SIB\dd\SIB\ws\code\sib.mfp.impl\src\com\ibm\ws\sib\mfp\schema\TrmFirstContactSchema.schema: do not edit directly
package com.ibm.ws.sib.mfp.schema;
import com.ibm.ws.sib.mfp.jmf.impl.JSchema;
public final class TrmFirstContactAccess {
  public final static JSchema schema = new TrmFirstContactSchema();
  public final static int MESSAGETYPE = 0;
  public final static int BODY = 90;
  public final static int IS_BODY_EMPTY = 0;
  public final static int IS_BODY_CLIENTBOOTSTRAPREQUEST = 1;
  public final static int IS_BODY_CLIENTBOOTSTRAPREPLY = 2;
  public final static int IS_BODY_CLIENTATTACHREQUEST = 3;
  public final static int IS_BODY_CLIENTATTACHREQUEST2 = 4;
  public final static int IS_BODY_CLIENTATTACHREPLY = 5;
  public final static int IS_BODY_MECONNECTREQUEST = 6;
  public final static int IS_BODY_MECONNECTREPLY = 7;
  public final static int IS_BODY_MELINKREQUEST = 8;
  public final static int IS_BODY_MELINKREPLY = 9;
  public final static int IS_BODY_MEBRIDGEREQUEST = 10;
  public final static int IS_BODY_MEBRIDGEREPLY = 11;
  public final static int IS_BODY_MEBRIDGEBOOTSTRAPREQUEST = 12;
  public final static int IS_BODY_MEBRIDGEBOOTSTRAPREPLY = 13;
  public final static int BODY_CLIENTBOOTSTRAPREQUEST_BUSNAME = 1;
  public final static int BODY_CLIENTBOOTSTRAPREQUEST_CREDENTIALTYPE = 2;
  public final static int BODY_CLIENTBOOTSTRAPREQUEST_USERID = 3;
  public final static int BODY_CLIENTBOOTSTRAPREQUEST_PASSWORD = 4;
  public final static int BODY_CLIENTBOOTSTRAPREQUEST_TARGETGROUPNAME = 5;
  public final static int BODY_CLIENTBOOTSTRAPREQUEST_TARGETGROUPTYPE = 6;
  public final static int BODY_CLIENTBOOTSTRAPREQUEST_TARGETSIGNIFICANCE = 7;
  public final static int BODY_CLIENTBOOTSTRAPREQUEST_CONNECTIONPROXIMITY = 8;
  public final static int BODY_CLIENTBOOTSTRAPREQUEST_TARGETTRANSPORTCHAIN = 9;
  public final static int BODY_CLIENTBOOTSTRAPREQUEST_BOOTSTRAPTRANSPORTCHAIN = 10;
  public final static int BODY_CLIENTBOOTSTRAPREQUEST_UPDATE1 = 91;
  public final static int IS_BODY_CLIENTBOOTSTRAPREQUEST_UPDATE1_EMPTY = 0;
  public final static int IS_BODY_CLIENTBOOTSTRAPREQUEST_UPDATE1_CONNECTIONMODE = 1;
  public final static int BODY_CLIENTBOOTSTRAPREQUEST_UPDATE1_CONNECTIONMODE = 11;
  public final static int BODY_CLIENTBOOTSTRAPREPLY_RETURNCODE = 12;
  public final static int BODY_CLIENTBOOTSTRAPREPLY_FAILUREREASON = 13;
  public final static int BODY_CLIENTBOOTSTRAPREPLY_ENDPOINTDATA = 14;
  public final static int BODY_CLIENTBOOTSTRAPREPLY_BUSNAME = 15;
  public final static int BODY_CLIENTBOOTSTRAPREPLY_SUBNETNAME = 16;
  public final static int BODY_CLIENTBOOTSTRAPREPLY_MESSAGINGENGINENAME = 17;
  public final static int BODY_CLIENTATTACHREQUEST_BUSNAME = 18;
  public final static int BODY_CLIENTATTACHREQUEST_CREDENTIALTYPE = 19;
  public final static int BODY_CLIENTATTACHREQUEST_USERID = 20;
  public final static int BODY_CLIENTATTACHREQUEST_PASSWORD = 21;
  public final static int BODY_CLIENTATTACHREQUEST_MENAME = 22;
  public final static int BODY_CLIENTATTACHREQUEST_SUBNETNAME = 23;
  public final static int BODY_CLIENTATTACHREQUEST_CREDENTIALS = 92;
  public final static int IS_BODY_CLIENTATTACHREQUEST_CREDENTIALS_EMPTY = 0;
  public final static int IS_BODY_CLIENTATTACHREQUEST_CREDENTIALS_TOKENPARTS = 1;
  public final static int BODY_CLIENTATTACHREQUEST_CREDENTIALS_TOKENPARTS_TOKENVALUE = 24;
  public final static int BODY_CLIENTATTACHREQUEST_CREDENTIALS_TOKENPARTS_TOKENTYPE = 25;
  public final static int BODY_CLIENTATTACHREQUEST2_REQUIREDBUSNAME = 26;
  public final static int BODY_CLIENTATTACHREQUEST2_CREDENTIALTYPE = 27;
  public final static int BODY_CLIENTATTACHREQUEST2_USERID = 28;
  public final static int BODY_CLIENTATTACHREQUEST2_PASSWORD = 29;
  public final static int BODY_CLIENTATTACHREQUEST2_MEUUID = 30;
  public final static int BODY_CLIENTATTACHREPLY_RETURNCODE = 31;
  public final static int BODY_CLIENTATTACHREPLY_FAILUREREASON = 32;
  public final static int BODY_MECONNECTREQUEST_MAGICNUMBER = 33;
  public final static int BODY_MECONNECTREQUEST_REQUIREDBUSNAME = 34;
  public final static int BODY_MECONNECTREQUEST_USERID = 35;
  public final static int BODY_MECONNECTREQUEST_PASSWORD = 36;
  public final static int BODY_MECONNECTREQUEST_REQUIREDMENAME = 37;
  public final static int BODY_MECONNECTREQUEST_REQUIREDSUBNETNAME = 38;
  public final static int BODY_MECONNECTREQUEST_REQUESTINGMENAME = 39;
  public final static int BODY_MECONNECTREQUEST_REQUESTINGMEUUID = 40;
  public final static int BODY_MECONNECTREQUEST_SUBNETMESSAGINGENGINES = 41;
  public final static int BODY_MECONNECTREQUEST_CREDENTIALTYPE = 93;
  public final static int IS_BODY_MECONNECTREQUEST_CREDENTIALTYPE_EMPTY = 0;
  public final static int IS_BODY_MECONNECTREQUEST_CREDENTIALTYPE_CREDENTIALTYPE = 1;
  public final static int BODY_MECONNECTREQUEST_CREDENTIALTYPE_CREDENTIALTYPE = 42;
  public final static int BODY_MECONNECTREQUEST_CREDENTIALS = 94;
  public final static int IS_BODY_MECONNECTREQUEST_CREDENTIALS_EMPTY = 0;
  public final static int IS_BODY_MECONNECTREQUEST_CREDENTIALS_TOKENPARTS = 1;
  public final static int BODY_MECONNECTREQUEST_CREDENTIALS_TOKENPARTS_TOKENVALUE = 43;
  public final static int BODY_MECONNECTREQUEST_CREDENTIALS_TOKENPARTS_TOKENTYPE = 44;
  public final static int BODY_MECONNECTREPLY_MAGICNUMBER = 45;
  public final static int BODY_MECONNECTREPLY_RETURNCODE = 46;
  public final static int BODY_MECONNECTREPLY_REPLYINGMEUUID = 47;
  public final static int BODY_MECONNECTREPLY_SUBNETMESSAGINGENGINES = 48;
  public final static int BODY_MECONNECTREPLY_FAILUREREASON = 49;
  public final static int BODY_MECONNECTREPLY_CREDENTIALS = 95;
  public final static int IS_BODY_MECONNECTREPLY_CREDENTIALS_EMPTY = 0;
  public final static int IS_BODY_MECONNECTREPLY_CREDENTIALS_TOKENPARTS = 1;
  public final static int BODY_MECONNECTREPLY_CREDENTIALS_TOKENPARTS_TOKENVALUE = 50;
  public final static int BODY_MECONNECTREPLY_CREDENTIALS_TOKENPARTS_TOKENTYPE = 51;
  public final static int BODY_MELINKREQUEST_MAGICNUMBER = 52;
  public final static int BODY_MELINKREQUEST_REQUIREDBUSNAME = 53;
  public final static int BODY_MELINKREQUEST_USERID = 54;
  public final static int BODY_MELINKREQUEST_PASSWORD = 55;
  public final static int BODY_MELINKREQUEST_REQUIREDMENAME = 56;
  public final static int BODY_MELINKREQUEST_REQUIREDSUBNETNAME = 57;
  public final static int BODY_MELINKREQUEST_REQUESTINGMENAME = 58;
  public final static int BODY_MELINKREQUEST_REQUESTINGMEUUID = 59;
  public final static int BODY_MELINKREQUEST_REQUESTINGSUBNETNAME = 60;
  public final static int BODY_MELINKREPLY_MAGICNUMBER = 61;
  public final static int BODY_MELINKREPLY_RETURNCODE = 62;
  public final static int BODY_MELINKREPLY_REPLYINGMEUUID = 63;
  public final static int BODY_MELINKREPLY_FAILUREREASON = 64;
  public final static int BODY_MEBRIDGEREQUEST_MAGICNUMBER = 65;
  public final static int BODY_MEBRIDGEREQUEST_REQUIREDBUSNAME = 66;
  public final static int BODY_MEBRIDGEREQUEST_USERID = 67;
  public final static int BODY_MEBRIDGEREQUEST_PASSWORD = 68;
  public final static int BODY_MEBRIDGEREQUEST_REQUIREDMENAME = 69;
  public final static int BODY_MEBRIDGEREQUEST_REQUESTINGBUSNAME = 70;
  public final static int BODY_MEBRIDGEREQUEST_REQUESTINGMENAME = 71;
  public final static int BODY_MEBRIDGEREQUEST_REQUESTINGMEUUID = 72;
  public final static int BODY_MEBRIDGEREQUEST_LINKNAME = 73;
  public final static int BODY_MEBRIDGEREPLY_MAGICNUMBER = 74;
  public final static int BODY_MEBRIDGEREPLY_RETURNCODE = 75;
  public final static int BODY_MEBRIDGEREPLY_REPLYINGMEUUID = 76;
  public final static int BODY_MEBRIDGEREPLY_FAILUREREASON = 77;
  public final static int BODY_MEBRIDGEBOOTSTRAPREQUEST_REQUIREDBUSNAME = 78;
  public final static int BODY_MEBRIDGEBOOTSTRAPREQUEST_USERID = 79;
  public final static int BODY_MEBRIDGEBOOTSTRAPREQUEST_PASSWORD = 80;
  public final static int BODY_MEBRIDGEBOOTSTRAPREQUEST_REQUESTINGBUSNAME = 81;
  public final static int BODY_MEBRIDGEBOOTSTRAPREQUEST_LINKNAME = 82;
  public final static int BODY_MEBRIDGEBOOTSTRAPREQUEST_REQUIREDTRANSPORTCHAIN = 83;
  public final static int BODY_MEBRIDGEBOOTSTRAPREPLY_RETURNCODE = 84;
  public final static int BODY_MEBRIDGEBOOTSTRAPREPLY_FAILUREREASON = 85;
  public final static int BODY_MEBRIDGEBOOTSTRAPREPLY_ENDPOINTDATA = 86;
  public final static int BODY_MEBRIDGEBOOTSTRAPREPLY_BUSNAME = 87;
  public final static int BODY_MEBRIDGEBOOTSTRAPREPLY_SUBNETNAME = 88;
  public final static int BODY_MEBRIDGEBOOTSTRAPREPLY_MESSAGINGENGINENAME = 89;
}
