/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.sm.smfview;

import java.util.HashMap;
import java.util.Iterator;

/**
 * This class provides a plug-in which generates a template classification XML
 * document based on the classification information found in SMF120-9 records
 * produced by your running HTTP and IIOP applications.
 *
 */
public class ClassificationXMLFilter implements SMFFilter {
    private SmfPrintStream smf_printstream = null;
    private HashMap HTTP = new HashMap();
    private HashMap IIOP = new HashMap();

    /**
     * Calls the default filter initialize method to setup the print stream
     */
    @Override
    public boolean initialize(String parms) {
        boolean return_value = true;
        smf_printstream = DefaultFilter.commonInitialize(parms);
        if (smf_printstream == null)
            return_value = false;
        return return_value;
    }

    /**
     * Eliminates all but SMF 120-9 records
     */
    @Override
    public boolean preParse(SmfRecord record) {
        boolean ok_to_process = false;
        if (record.type() == SmfRecord.SmfRecordType)
            if (record.subtype() == SmfRecord.RequestActivitySmfRecordSubtype)
                ok_to_process = true;
        return ok_to_process;
    }

    /**
     * Allow default filter to parse the record
     */
    @Override
    public SmfRecord parse(SmfRecord record) {
        return DefaultFilter.commonParse(record);
    }

    /**
     * Pull out classification data for HTTP and IIOP request records
     * and stuff it in appropriate HashMaps
     */
    @Override
    public void processRecord(SmfRecord record) {
        // cast to a subtype 9
        RequestActivitySmfRecord rec = (RequestActivitySmfRecord) record;
        // get the Platform Neutral Request section
        Triplet platformNeutralRequestInfoTriplet = rec.m_platformNeutralRequestInfoTriplet;
        int platformNeutralRequestInfoSectionCount = platformNeutralRequestInfoTriplet.count();

        Triplet ClassificationSectionTriplet = rec.m_classificationDataTriplet;
        int ClassificationSectionCount = ClassificationSectionTriplet.count();
        ClassificationDataSection CDS[] = null;
        if (ClassificationSectionCount > 0) {
            CDS = rec.m_classificationDataSection;
        }

        // Get the tran class if we can
        Triplet ZosRequestInfoSectionTriplet = rec.m_zosRequestInfoTriplet;
        int ZosRequestInfoSectionCount = ZosRequestInfoSectionTriplet.count();
        String tran_class;
        if (ZosRequestInfoSectionCount > 0) {
            ZosRequestInfoSection ZRIS = rec.m_zosRequestInfoSection;
            tran_class = ZRIS.m_tranClass;
        } else {
            tran_class = "        ";
        }

        // if we can, get the request type
        if (platformNeutralRequestInfoSectionCount > 0) {
            PlatformNeutralRequestInfoSection PNRI = rec.m_platformNeutralRequestInfoSection;
            int req_type = PNRI.m_requestType;
            // Handle HTTP requests
            if ((req_type == PlatformNeutralRequestInfoSection.TypeHTTP) |
                (req_type == PlatformNeutralRequestInfoSection.TypeHTTPS)) {
                // get the data for an HTTP request
                String host = null;
                String port = null;
                String URI = null;
                for (int i = 0; i <= ClassificationSectionCount - 1; ++i) {
                    ClassificationDataSection cds = CDS[i];
                    if (cds.m_dataType == ClassificationDataSection.TypeHostname)
                        host = cds.m_theData;
                    if (cds.m_dataType == ClassificationDataSection.TypePort)
                        port = cds.m_theData;
                    if (cds.m_dataType == ClassificationDataSection.TypeURI)
                        URI = cds.m_theData;
                }
                // if we got them all
                if ((host != null) & (port != null) & (URI != null)) {
                    // HTTP is a HashMap, the keys are host names, the entries are HashMaps
                    // Those HashMaps have port numbers as keys, the entries are HashMaps
                    // The key to that HashMap is a URI.  The entries are bogus, we just need 
                    // to avoid duplicate entries.  Could put the tran class here..
                    HashMap ports = null;
                    // already got an entry in 'HTTP' for this host?
                    Object h = HTTP.get(host);
                    if (h == null) {
                        // nope, make a new HashMap (which will use ports as keys) and put it in HTTP
                        ports = new HashMap();
                        HTTP.put(host, ports);
                    } else {
                        // got one
                        ports = (HashMap) h;
                    }
                    HashMap uris = null;
                    // Already got an entry for this port?
                    h = ports.get(port);
                    if (h == null) {
                        // no, make one
                        uris = new HashMap();
                        ports.put(port, uris);
                    } else {
                        // got one
                        uris = (HashMap) h;
                    }
                    // and now the URI... if no entry already, add one.
                    h = uris.get(URI);
                    if (h == null) {
                        uris.put(URI, tran_class);
                    }
                }
            }
            // Handle IIOP requests
            if (req_type == PlatformNeutralRequestInfoSection.TypeIIOP) {
                // get the data for an HTTP request
                String A = null;
                String M = null;
                String C = null;
                String Method = null;
                for (int i = 0; i < ClassificationSectionCount - 1; ++i) {
                    ClassificationDataSection cds = CDS[i];
                    if (cds.m_dataType == ClassificationDataSection.TypeApplicationName)
                        A = cds.m_theData;
                    if (cds.m_dataType == ClassificationDataSection.TypeModuleName)
                        M = cds.m_theData;
                    if (cds.m_dataType == ClassificationDataSection.TypeComponentName)
                        C = cds.m_theData;
                    if (cds.m_dataType == ClassificationDataSection.TypeMethodName)
                        Method = cds.m_theData;
                }
                if ((A != null) & (M != null) & (C != null) & (Method != null)) // got em all
                {
                    HashMap modules = null;
                    // already got an entry in 'IIOP' for this application?
                    Object h = IIOP.get(A);
                    if (h == null) {
                        // nope, make a new HashMap (which will use ports as keys) and put it in IIOP
                        modules = new HashMap();
                        IIOP.put(A, modules);
                    } else {
                        // got one
                        modules = (HashMap) h;
                    }
                    HashMap components = null;
                    // Already got an entry for this module?
                    h = modules.get(M);
                    if (h == null) {
                        // no, make one
                        components = new HashMap();
                        modules.put(M, components);
                    } else {
                        // got one
                        components = (HashMap) h;
                    }
                    // and now the component... if no entry already, add one.
                    HashMap methods = null;
                    h = components.get(C);
                    if (h == null) {
                        methods = new HashMap();
                        components.put(C, methods);
                    } else {
                        methods = (HashMap) h;
                    }
                    h = methods.get(Method);
                    if (h == null) {
                        methods.put(Method, tran_class);
                    }
                }
            } // handle IIOP
        }
    }

//<?xml version="1.0" encoding="UTF-8"?>
//	<!DOCTYPE Classification SYSTEM "Classification.dtd" >
//	<Classification schema_version="1.0">

//<InboundClassification  type="http"  schema_version="1.0"  default_transaction_class="M">
// <http_classification_info  transaction_class="N" host="yourhost.yourcompany.com">
//     <http_classification_info transaction_class="O" port="9080">
//            <http_classification_info  transaction_class="Q" uri="/gcs/admin"/>
//            <http_classification_info  transaction_class="S" uri="/gcs/admin/1*"/>
//     </http_classification_info>
//     <http_classification_info  transaction_class="P" port="9081">
//            <http_classification_info  transaction_class="" uri="/gcss/mgr/*"/>
//     </http_classification_info>
//  </http_classification_info>
//</InboundClassification>

    /**
     * Produces classification XML template
     */
    @Override
    public void processingComplete() {
        smf_printstream.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        smf_printstream.println("<!DOCTYPE Classification SYSTEM \"Classification.dtd\" >");
        smf_printstream.println("<Classification schema_version=\"1.0\">");
        Iterator it = (HTTP.keySet()).iterator();
        if (it.hasNext()) {
            smf_printstream.print("<InboundClassification  type=\"http\"  schema_version=\"1.0\" ");
            smf_printstream.println("default_transaction_class=\"        \">");
            while (it.hasNext()) {
                String host = (String) (it.next());
                smf_printstream.println("\t<http_classification_info host=\"" + host + "\" transaction_class=\"       \">");
                HashMap ports = (HashMap) HTTP.get(host);
                Iterator iports = (ports.keySet()).iterator();
                while (iports.hasNext()) {
                    String port = (String) (iports.next());
                    smf_printstream.println("\t\t<http_classification_info port=\"" + port + "\" transaction_class=\"       \">");
                    HashMap uris = (HashMap) ports.get(port);
                    Iterator iURIs = (uris.keySet()).iterator();
                    while (iURIs.hasNext()) {
                        String URI = (String) (iURIs.next());
                        String tclass = (String) uris.get(URI);
                        smf_printstream.println("\t\t\t<http_classification_info uri=\"" + URI + "\" transaction_class=\"" + tclass + "\"/>");
                    } // while URIs
                    smf_printstream.println("\t\t</http_classification_info>");
                } // while ports
                smf_printstream.println("\t</http_classification_info>");
            } // while hosts
            smf_printstream.println("</InboundClassification>");
        }
        it = (IIOP.keySet()).iterator();
        if (it.hasNext()) {
            smf_printstream.print("<InboundClassification  type=\"iiop\"  schema_version=\"1.0\" ");
            smf_printstream.println("default_transaction_class=\"        \">");
            while (it.hasNext()) {
                String app = (String) (it.next());
                smf_printstream.println("\t<iiop_classification_info application_name=\"" + app + "\" transaction_class=\"       \">");
                HashMap mods = (HashMap) IIOP.get(app);
                Iterator imods = (mods.keySet()).iterator();
                while (imods.hasNext()) {
                    String mod = (String) (imods.next());
                    smf_printstream.println("\t\t<iiop_classification_info module_name=\"" + mod + "\" transaction_class=\"       \">");
                    HashMap comps = (HashMap) mods.get(mod);
                    Iterator icomps = (comps.keySet()).iterator();
                    while (icomps.hasNext()) {
                        String comp = (String) (icomps.next());
                        smf_printstream.println("\t\t\t<iiop_classification_info component_name=\"" + comp + "\" transaction_class=\"       \">");
                        HashMap meths = (HashMap) comps.get(comp);
                        Iterator imeths = (meths.keySet()).iterator();
                        while (imeths.hasNext()) {
                            String meth = (String) (imeths.next());
                            String tclass = (String) meths.get(meth);
                            smf_printstream.println("\t\t\t\t<iiop_classification_info method_name=\"" + meth + "\" transaction_class=\"" + tclass + "\"/>");
                        } // while methods
                        smf_printstream.println("\t\t\t</iiop_classification_info>");
                    } // while components
                    smf_printstream.println("\t\t</iiop_classification_info>");
                } // while modules
                smf_printstream.println("\t</iiop_classification_info>");
            } // while applications
            smf_printstream.println("</InboundClassification>");
        }
        smf_printstream.println("</Classification>");
    }
}
