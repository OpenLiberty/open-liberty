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

package com.ibm.ws390.sm.smfview.test;

import com.ibm.ws390.sm.smfview.LibertyNetworkDataSection;
import com.ibm.ws390.sm.smfview.LibertyRequestInfoSection;
import com.ibm.ws390.sm.smfview.SmfPrintStream;
import com.ibm.ws390.sm.smfview.SmfStream;
import com.ibm.ws390.smf.formatters.LibertyRequestRecord;
import com.ibm.ws390.smf.formatters.LibertyServerInfoSection;

public class SMFType120SubType11BasicValidation {

    private final LibertyRequestRecord libertyRequestRecord;

    private final SmfPrintStream smf_printstream;

    private final int minimumResponseBytes;

    public SMFType120SubType11BasicValidation(LibertyRequestRecord libertyRequestRecord, SmfPrintStream smf_printstream) {
        this(libertyRequestRecord, smf_printstream, 7);
    }

    public SMFType120SubType11BasicValidation(LibertyRequestRecord libertyRequestRecord, SmfPrintStream smf_printstream, int minimumResponseBytes) {
        this.libertyRequestRecord = libertyRequestRecord;
        this.smf_printstream = smf_printstream;
        this.minimumResponseBytes = minimumResponseBytes;
    }

    public int validateRecord() {
        int rc = 0;
        rc = validateServerInfoSection(libertyRequestRecord.m_libertyServerInfoSection);
        if (rc == 0) {
            rc = validateLibertyRequestInfoSection(libertyRequestRecord.m_libertyRequestInfoSection);
            if (rc == 0) {
                rc = validateNetworkDataSection(libertyRequestRecord.m_libertyNetworkDataSection);
            }
        }
        return rc;
    }

    private int validateLibertyRequestInfoSection(LibertyRequestInfoSection libertyRequestInfoSection) {
        int rc = 0;
        if (libertyRequestInfoSection.m_version != 1) {

            smf_printstream.println("ERROR Request Info Section version is not 1. version=" + libertyRequestInfoSection.m_version);
            rc = 1;
        }

        if (libertyRequestInfoSection.m_tcbAddress == null) {
            smf_printstream.println("ERROR Request Info Section tcb address is null.");
            rc = 2;
        } else {
            if (libertyRequestInfoSection.m_tcbAddress.length == 0) {
                smf_printstream.println("ERROR Request Info Section tcb address length is 0.");
                rc = 3;
            } else {
                SmfStream aStream = new SmfStream(libertyRequestInfoSection.m_tcbAddress);
                if (aStream.getInteger(4) == 0) {
                    smf_printstream.println("ERROR Request Info Section tcb address is 0.");
                    rc = 4;
                }
            }
        }
        if (libertyRequestInfoSection.m_ttoken == null) {
            smf_printstream.println("ERROR Request Info Section ttoken is null.");
            rc = 5;
        } else {
            if (libertyRequestInfoSection.m_ttoken.length == 0) {
                smf_printstream.println("ERROR Request Info Section ttoken length is 0.");
                rc = 6;
            } else {
                SmfStream aStream = new SmfStream(libertyRequestInfoSection.m_ttoken);
                long firstLong = aStream.getLong();
                long secondLong = aStream.getLong();
                if ((firstLong == 0) && (secondLong == 0)) {
                    smf_printstream.println("ERROR Request Info Section ttoken is 0.");
                    rc = 77;
                }
            }
        }

        if (libertyRequestInfoSection.m_threadId == null) {
            smf_printstream.println("ERROR Request Info Section thread id is null.");
            rc = 8;
        } else {
            if (libertyRequestInfoSection.m_threadId.length == 0) {
                smf_printstream.println("ERROR Request Info Section thread id length is 0.");
                rc = 9;
            } else {
                SmfStream aStream = new SmfStream(libertyRequestInfoSection.m_threadId);
                if (aStream.getLong() == 0) {
                    smf_printstream.println("ERROR Request Info Section thread id is 0.");
                    rc = 10;
                }
            }
        }

        if (libertyRequestInfoSection.m_systemGmtOffset == null) {
            smf_printstream.println("ERROR Request Info Section GMT offset is null.");
            rc = 11;
        } else {
            if (libertyRequestInfoSection.m_systemGmtOffset.length == 0) {
                smf_printstream.println("ERROR Request Info Section GMT offset length is 0.");
                rc = 12;
            } else {
                SmfStream aStream = new SmfStream(libertyRequestInfoSection.m_systemGmtOffset);
                if (aStream.getLong() == 0) {
                    smf_printstream.println("ERROR Request Info Section GMT offset is 0.");
                    rc = 13;
                }
            }
        }

        if (libertyRequestInfoSection.m_threadIdCurrentThread == null) {
            smf_printstream.println("ERROR Request Info Section thread id current thread is null.");
            rc = 14;
        } else {
            if (libertyRequestInfoSection.m_threadIdCurrentThread.length == 0) {
                smf_printstream.println("ERROR Request Info Section thread id current thread length is 0.");
                rc = 15;
            } else {
                SmfStream aStream = new SmfStream(libertyRequestInfoSection.m_threadIdCurrentThread);
                if (aStream.getLong() == 0) {
                    smf_printstream.println("ERROR Request Info Section thread id current thread is 0.");
                    rc = 16;
                }
            }
        }

        if (libertyRequestInfoSection.m_requestId == null) {
            smf_printstream.println("ERROR Request Info Section request id is null.");
            rc = 17;
        } else {
            if (libertyRequestInfoSection.m_requestId.length == 0) {
                smf_printstream.println("ERROR Request Info Section request id length is 0.");
                rc = 18;
            } else {
                SmfStream aStream = new SmfStream(libertyRequestInfoSection.m_requestId);
                long firstLong = aStream.getLong();
                long secondLong = aStream.getLong();
                int firstInt = aStream.getInteger(4);
                int secondInt = aStream.getInteger(3);
                if ((firstLong == 0) && (secondLong == 0) && (firstInt == 0) && (secondInt == 0)) {
                    smf_printstream.println("ERROR Request Info Section request id is 0.");
                    rc = 19;
                }
            }
        }

        if (libertyRequestInfoSection.m_startStck == null) {
            smf_printstream.println("ERROR Request Info Section start stck is null.");
            rc = 20;
        } else {
            if (libertyRequestInfoSection.m_startStck.length == 0) {
                smf_printstream.println("ERROR Request Info Section start stck length is 0.");
                rc = 21;
            } else {
                SmfStream aStream = new SmfStream(libertyRequestInfoSection.m_startStck);
                if (aStream.getLong() == 0) {
                    smf_printstream.println("ERROR Request Info Section start stck is 0.");
                    rc = 22;
                }
            }
        }

        if (libertyRequestInfoSection.m_endStck == null) {
            smf_printstream.println("ERROR Request Info Section end stck is null.");
            rc = 23;
        } else {
            if (libertyRequestInfoSection.m_endStck.length == 0) {
                smf_printstream.println("ERROR Request Info Section end stck length is 0.");
                rc = 24;
            } else {
                SmfStream aStream = new SmfStream(libertyRequestInfoSection.m_endStck);
                if (aStream.getLong() == 0) {
                    smf_printstream.println("ERROR Request Info Section end stck is 0.");
                    rc = 25;
                }
            }
        }

        if (libertyRequestInfoSection.m_timeusedStart == null) {
            smf_printstream.println("ERROR Request Info Section timeused start is null.");
            rc = 26;
        } else {
            if (libertyRequestInfoSection.m_timeusedStart.length == 0) {
                smf_printstream.println("ERROR Request Info Section timeused start length is 0.");
                rc = 27;
            } else {
                SmfStream aStream = new SmfStream(libertyRequestInfoSection.m_timeusedStart);
                if (aStream.getLong() == 0) {
                    smf_printstream.println("ERROR Request Info Section timeused start is 0.");
                    rc = 28;
                }
            }
        }

        if (libertyRequestInfoSection.m_timeusedEnd == null) {
            smf_printstream.println("ERROR Request Info Section timeused end is null.");
            rc = 29;
        } else {
            if (libertyRequestInfoSection.m_timeusedEnd.length == 0) {
                smf_printstream.println("ERROR Request Info Section timeused end length is 0.");
                rc = 30;
            } else {
                SmfStream aStream = new SmfStream(libertyRequestInfoSection.m_timeusedEnd);
                if (aStream.getLong() == 0) {
                    smf_printstream.println("ERROR Request Info Section timeused end is 0.");
                    rc = 31;
                }
            }
        }

        return rc;
    }

    private int validateServerInfoSection(LibertyServerInfoSection libertyServerInfoSection) {
        int rc = 0;
        if (libertyServerInfoSection.m_version != 3) {

            smf_printstream.println("ERROR Server Info Section version is not 3. version=" + libertyServerInfoSection.m_version);
            rc = 1;

        }

        if ((libertyServerInfoSection.m_systemName == null) || (libertyServerInfoSection.m_systemName.length() == 0)) {
            smf_printstream.println("ERROR Server Info Section system name string is null or a string of length 0.");
            rc = 2;
        }

        if ((libertyServerInfoSection.m_sysplexName == null) || (libertyServerInfoSection.m_sysplexName.length() == 0)) {
            smf_printstream.println("ERROR Server Info Section sysplex name string is null or a string of length 0.");
            rc = 3;
        }

        if ((libertyServerInfoSection.m_jobId == null) || (libertyServerInfoSection.m_jobId.length() == 0)) {
            smf_printstream.println("ERROR Server Info Section job id string is null or a string of length 0.");
            rc = 4;
        } else {
            if (!(libertyServerInfoSection.m_jobId.startsWith("STC"))) {
                smf_printstream.println("ERROR Server Info Section job id string does not start with STC.");
                rc = 5;
            }
        }

        if ((libertyServerInfoSection.m_jobName == null) || (libertyServerInfoSection.m_jobName.length() == 0)) {
            smf_printstream.println("ERROR Server Info Section job name string is null or a string of length 0.");
            rc = 6;
        }

        if (libertyServerInfoSection.m_server_stoken == null) {
            smf_printstream.println("ERROR Server Info Section server stoken is null.");
            rc = 7;
        } else {
            if (libertyServerInfoSection.m_server_stoken.length == 0) {
                smf_printstream.println("ERROR Server Info Section server stoken length is 0.");
                rc = 8;
            } else {
                SmfStream aStream = new SmfStream(libertyServerInfoSection.m_server_stoken);
                if (aStream.getLong() == 0) {
                    smf_printstream.println("ERROR Server Info Section server stoken is 0.");
                    rc = 9;
                }
            }
        }

        if (libertyServerInfoSection.m_asid == 0) {
            smf_printstream.println("ERROR Server Info Section asid is 0.");
            rc = 10;
        }

        if (libertyServerInfoSection.m_pid == 0) {
            smf_printstream.println("ERROR Server Info Section pid is 0.");
            rc = 11;
        }

        if ((libertyServerInfoSection.m_productVersion == null) || (libertyServerInfoSection.m_productVersion.length() == 0)) {
            smf_printstream.println("ERROR Server Info Section product version string is null or a string of length 0.");
            rc = 14;
        }

        // Ensure all bits in the flag word are zero other than the first high order bit for Cvtzcbp
        int firstByteOtherThanCvtzcbp = (0x0000007F & (libertyServerInfoSection.m_bitFlags[0]));
        if (firstByteOtherThanCvtzcbp != 0) {
            smf_printstream.println("ERROR Server Info Section first byte of the flag word has bits on that should be off. First byte = " + firstByteOtherThanCvtzcbp);
            rc = 15;
        }
        int secondByte = (0x000000FF & (libertyServerInfoSection.m_bitFlags[1]));
        if (secondByte != 0) {
            smf_printstream.println("ERROR Server Info Section second byte of the flag word is not 0.");
            rc = 16;
        }
        int thirdByte = (0x000000FF & (libertyServerInfoSection.m_bitFlags[2]));
        if (thirdByte != 0) {
            smf_printstream.println("ERROR Server Info Section third byte of the flag word is not 0.");
            rc = 17;
        }
        int fourthByte = (0x000000FF & (libertyServerInfoSection.m_bitFlags[3]));
        if (fourthByte != 0) {
            smf_printstream.println("ERROR Server Info Section fourth byte of the flag word is not 0.");
            rc = 18;
        }

        return rc;

    }

    private int validateNetworkDataSection(LibertyNetworkDataSection libertyNetworkDataSection) {
        int rc = 0;

        if (libertyNetworkDataSection.m_version != 1) {
            smf_printstream.println("ERROR Network Data Section version is not 1. version=" + libertyNetworkDataSection.m_version);
            rc = 1;
        }

        if ((minimumResponseBytes > 0) && (libertyNetworkDataSection.m_responseBytes == 0)) {
            smf_printstream.println("ERROR Network Data Section response bytes is 0.");
            rc = 2;
        }

        /* tests return "Success" so check for at least the minimum number of response bytes */
        if (libertyNetworkDataSection.m_responseBytes < minimumResponseBytes) {
            smf_printstream.println("ERROR Network Data Section response bytes is less than " + minimumResponseBytes + ". " + libertyNetworkDataSection.m_responseBytes);
            rc = 2;
        }

        if (libertyNetworkDataSection.m_targetPort == 0) {
            smf_printstream.println("ERROR Network Data Section target port is 0.");
            rc = 3;
        }

        if (libertyNetworkDataSection.m_remotePort == 0) {
            smf_printstream.println("ERROR Network Data Section remote port is 0.");
            rc = 4;
        }

        if (libertyNetworkDataSection.m_lengthRemoteAddrString == 0) {
            smf_printstream.println("ERROR Network Data Section remote addr length is 0.");
            rc = 5;
        }

        if ((libertyNetworkDataSection.m_remoteaddrstring == null) ||
            (libertyNetworkDataSection.m_remoteaddrstring.length() == 0)) {
            smf_printstream.println("ERROR Network Data Section remote addr string is null or a string of length 0.");
            rc = 6;
        }

        return rc;
    }

}
