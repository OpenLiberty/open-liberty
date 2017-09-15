/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.mfp.schema;

import com.ibm.ws.sib.mfp.jmf.impl.JSchema;

/*
 * Generated from D:\WORK\PROJECT\SiBus\Code\sib_developer_WAS855_cf021407.02\SIB\ws\code\sib.mfp.impl\src\com\ibm\ws\sib\mfp\schema\JsHdr2Schema.schema:
 * do not edit directly
 */
public final class JsHdr2Access {
    public final static JSchema schema = new JsHdr2Schema();
    public final static int FORWARDROUTINGPATH_DESTINATIONNAME = 0;
    public final static int FORWARDROUTINGPATH_MEID = 1;
    public final static int FORWARDROUTINGPATH_BUSNAME = 2;
    public final static int FORWARDROUTINGPATHLOCALONLY = 3;
    public final static int REVERSEROUTINGPATH_DESTINATIONNAME = 4;
    public final static int REVERSEROUTINGPATH_MEID = 5;
    public final static int REVERSEROUTINGPATH_BUSNAME = 6;
    public final static int REVERSEROUTINGPATHLOCALONLY = 7;
    public final static int PRIORITY = 8;
    public final static int RELIABILITY = 9;
    public final static int TIMETOLIVE = 10;
    public final static int TIMESTAMP = 11;
    public final static int MESSAGEWAITTIME = 12;
    public final static int REDELIVEREDCOUNT = 13;
    public final static int BUS = 14;
    public final static int PRODUCERTYPE = 15;
    public final static int FLAGS = 16;
    public final static int XAPPID = 66;
    public final static int IS_XAPPID_EMPTY = 0;
    public final static int IS_XAPPID_COMPACT = 1;
    public final static int IS_XAPPID_STRINGVALUE = 2;
    public final static int XAPPID_COMPACT = 17;
    public final static int XAPPID_STRINGVALUE = 18;
    public final static int ROUTINGDESTINATION = 67;
    public final static int IS_ROUTINGDESTINATION_EMPTY = 0;
    public final static int IS_ROUTINGDESTINATION_VALUE = 1;
    public final static int ROUTINGDESTINATION_VALUE_NAME = 19;
    public final static int ROUTINGDESTINATION_VALUE_MEID = 20;
    public final static int ROUTINGDESTINATION_VALUE_BUSNAME = 21;
    public final static int REQUESTMETRICS = 68;
    public final static int IS_REQUESTMETRICS_UNSET = 0;
    public final static int IS_REQUESTMETRICS_CORRELATOR = 1;
    public final static int REQUESTMETRICS_CORRELATOR_ARM = 22;
    public final static int REQUESTMETRICS_CORRELATOR_RM = 23;
    public final static int REPORTEXPIRY = 69;
    public final static int IS_REPORTEXPIRY_UNSET = 0;
    public final static int IS_REPORTEXPIRY_VALUE = 1;
    public final static int REPORTEXPIRY_VALUE = 24;
    public final static int REPORTCOD = 70;
    public final static int IS_REPORTCOD_UNSET = 0;
    public final static int IS_REPORTCOD_VALUE = 1;
    public final static int REPORTCOD_VALUE = 25;
    public final static int REPORTCOA = 71;
    public final static int IS_REPORTCOA_UNSET = 0;
    public final static int IS_REPORTCOA_VALUE = 1;
    public final static int REPORTCOA_VALUE = 26;
    public final static int GUARANTEED = 72;
    public final static int IS_GUARANTEED_EMPTY = 0;
    public final static int IS_GUARANTEED_SET = 1;
    public final static int GUARANTEED_SET_SOURCEMEUUID = 27;
    public final static int GUARANTEED_SET_TARGETMEUUID = 28;
    public final static int GUARANTEED_SET_TARGETDESTDEFUUID = 29;
    public final static int GUARANTEED_SET_STREAMUUID = 30;
    public final static int GUARANTEED_SET_PROTOCOLTYPE = 31;
    public final static int GUARANTEED_SET_PROTOCOLVERSION = 32;
    public final static int GUARANTEED_SET_GATHERINGTARGETUUID = 73;
    public final static int IS_GUARANTEED_SET_GATHERINGTARGETUUID_EMPTY = 0;
    public final static int IS_GUARANTEED_SET_GATHERINGTARGETUUID_VALUE = 1;
    public final static int GUARANTEED_SET_GATHERINGTARGETUUID_VALUE = 33;
    public final static int GUARANTEEDXBUS = 74;
    public final static int IS_GUARANTEEDXBUS_EMPTY = 0;
    public final static int IS_GUARANTEEDXBUS_SET = 1;
    public final static int GUARANTEEDXBUS_SET_LINKNAME = 34;
    public final static int GUARANTEEDXBUS_SET_SOURCEBUSUUID = 35;
    public final static int GUARANTEEDVALUE = 75;
    public final static int IS_GUARANTEEDVALUE_EMPTY = 0;
    public final static int IS_GUARANTEEDVALUE_SET = 1;
    public final static int GUARANTEEDVALUE_SET_STARTTICK = 36;
    public final static int GUARANTEEDVALUE_SET_ENDTICK = 37;
    public final static int GUARANTEEDVALUE_SET_VALUETICK = 38;
    public final static int GUARANTEEDVALUE_SET_COMPLETEDPREFIX = 39;
    public final static int GUARANTEEDVALUE_SET_REQUESTEDONLY = 40;
    public final static int GUARANTEEDREMOTEBROWSE = 76;
    public final static int IS_GUARANTEEDREMOTEBROWSE_EMPTY = 0;
    public final static int IS_GUARANTEEDREMOTEBROWSE_SET = 1;
    public final static int GUARANTEEDREMOTEBROWSE_SET_BROWSEID = 41;
    public final static int GUARANTEEDREMOTEBROWSE_SET_SEQNUM = 42;
    public final static int GUARANTEEDREMOTEGET = 77;
    public final static int IS_GUARANTEEDREMOTEGET_EMPTY = 0;
    public final static int IS_GUARANTEEDREMOTEGET_SET = 1;
    public final static int GUARANTEEDREMOTEGET_SET_WAITTIME = 43;
    public final static int GUARANTEEDREMOTEGET_SET_PREVTICK = 44;
    public final static int GUARANTEEDREMOTEGET_SET_STARTTICK = 45;
    public final static int GUARANTEEDREMOTEGET_SET_VALUETICK = 46;
    public final static int EXCEPTION = 78;
    public final static int IS_EXCEPTION_EMPTY = 0;
    public final static int IS_EXCEPTION_DETAIL = 1;
    public final static int EXCEPTION_DETAIL_REASON = 47;
    public final static int EXCEPTION_DETAIL_TIMESTAMP = 48;
    public final static int EXCEPTION_DETAIL_INSERTS = 49;
    public final static int EXCEPTION_DETAIL_PROBLEMDESTINATION = 79;
    public final static int IS_EXCEPTION_DETAIL_PROBLEMDESTINATION_EMPTY = 0;
    public final static int IS_EXCEPTION_DETAIL_PROBLEMDESTINATION_DATA = 1;
    public final static int EXCEPTION_DETAIL_PROBLEMDESTINATION_DATA = 50;
    public final static int EXCEPTION_DETAIL_PROBLEMSUBSCRIPTION = 80;
    public final static int IS_EXCEPTION_DETAIL_PROBLEMSUBSCRIPTION_EMPTY = 0;
    public final static int IS_EXCEPTION_DETAIL_PROBLEMSUBSCRIPTION_DATA = 1;
    public final static int EXCEPTION_DETAIL_PROBLEMSUBSCRIPTION_DATA = 51;
    public final static int JMSDELIVERYMODE = 81;
    public final static int IS_JMSDELIVERYMODE_EMPTY = 0;
    public final static int IS_JMSDELIVERYMODE_DATA = 1;
    public final static int JMSDELIVERYMODE_DATA = 52;
    public final static int JMSEXPIRATION = 82;
    public final static int IS_JMSEXPIRATION_EMPTY = 0;
    public final static int IS_JMSEXPIRATION_DATA = 1;
    public final static int JMSEXPIRATION_DATA = 53;
    public final static int JMSDESTINATION = 83;
    public final static int IS_JMSDESTINATION_EMPTY = 0;
    public final static int IS_JMSDESTINATION_DATA = 1;
    public final static int JMSDESTINATION_DATA = 54;
    public final static int JMSREPLYTO = 84;
    public final static int IS_JMSREPLYTO_EMPTY = 0;
    public final static int IS_JMSREPLYTO_DATA = 1;
    public final static int JMSREPLYTO_DATA = 55;
    public final static int JMSTYPE = 85;
    public final static int IS_JMSTYPE_EMPTY = 0;
    public final static int IS_JMSTYPE_DATA = 1;
    public final static int JMSTYPE_DATA = 56;
    public final static int TRANSPORTVERSION = 86;
    public final static int IS_TRANSPORTVERSION_EMPTY = 0;
    public final static int IS_TRANSPORTVERSION_DATA = 1;
    public final static int TRANSPORTVERSION_DATA = 57;
    public final static int AUDITSESSIONID = 87;
    public final static int IS_AUDITSESSIONID_EMPTY = 0;
    public final static int IS_AUDITSESSIONID_DATA = 1;
    public final static int AUDITSESSIONID_DATA = 58;
    public final static int MESSAGECONTROLCLASSIFICATION = 88;
    public final static int IS_MESSAGECONTROLCLASSIFICATION_EMPTY = 0;
    public final static int IS_MESSAGECONTROLCLASSIFICATION_DATA = 1;
    public final static int MESSAGECONTROLCLASSIFICATION_DATA = 59;
    public final static int MQMDPROPERTIES = 89;
    public final static int IS_MQMDPROPERTIES_EMPTY = 0;
    public final static int IS_MQMDPROPERTIES_MAP = 1;
    public final static int MQMDPROPERTIES_MAP_NAME = 60;
    public final static int MQMDPROPERTIES_MAP_VALUE = 61;
    public final static int FINGERPRINTS = 90;
    public final static int IS_FINGERPRINTS_EMPTY = 0;
    public final static int IS_FINGERPRINTS_RFPLIST = 1;
    public final static int FINGERPRINTS_RFPLIST_ID = 62;
    public final static int XCT_CORRELATION_ID = 91;
    public final static int IS_XCT_CORRELATION_ID_EMPTY = 0;
    public final static int IS_XCT_CORRELATION_ID_DATA = 1;
    public final static int XCT_CORRELATION_ID_DATA = 63;
    public final static int DELIVERYDELAY = 92;
    public final static int IS_DELIVERYDELAY_EMPTY = 0;
    public final static int IS_DELIVERYDELAY_DATA = 1;
    public final static int DELIVERYDELAY_DATA = 64;
    public final static int JMSDELIVERYTIME = 93;
    public final static int IS_JMSDELIVERYTIME_EMPTY = 0;
    public final static int IS_JMSDELIVERYTIME_DATA = 1;
    public final static int JMSDELIVERYTIME_DATA = 65;
}
