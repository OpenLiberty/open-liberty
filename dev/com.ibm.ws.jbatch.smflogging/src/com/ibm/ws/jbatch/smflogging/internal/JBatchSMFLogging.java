/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.smflogging.internal;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.batch.runtime.Metric;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.jbatch.container.execution.impl.RuntimePartitionExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeSplitFlowExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeStepExecution;
import com.ibm.jbatch.container.execution.impl.RuntimeWorkUnitExecution;
import com.ibm.jbatch.container.ws.PartitionPlanConfig;
import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.WSTopLevelStepExecution;
import com.ibm.jbatch.container.ws.smf.ZosJBatchSMFLogging;
import com.ibm.jbatch.jsl.model.Batchlet;
import com.ibm.jbatch.jsl.model.Chunk;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.Partition;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.DuplicateProductInfoException;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.kernel.productinfo.ProductInfoReplaceException;
import com.ibm.ws.zos.core.structures.MvsCommonFields;
import com.ibm.ws.zos.core.utils.NativeUtils;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

/**
 * A declarative services component can be completely POJO based
 * (no awareness/use of OSGi services).
 *
 * OSGi methods (activate/deactivate) should be protected.
 */
@Component(name = "com.ibm.ws.jbatch.smflogging", configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM", service = { com.ibm.jbatch.container.ws.smf.ZosJBatchSMFLogging.class })
public class JBatchSMFLogging implements ZosJBatchSMFLogging {
    private static final TraceComponent tc = Tr.register(JBatchSMFLogging.class);

    private NativeUtils nativeUtils;

    private MvsCommonFields mvsCommonFields;
    private com.ibm.ws.zos.core.utils.Smf smf;

    // hold on to the last smf rc for potential modify command
    int lastSMFrc = 0;

    /**
     * The reference to the WSLocationAdmin service.
     */
    private WsLocationAdmin locationAdmin = null;

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param properties : Map containing service & config properties
     *                       populated/provided by config admin
     */
    @Activate
    protected void activate(Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "Java Batch SMF Recording feature activated", properties);
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param reason int representation of reason the component is stopping
     */
    @Deactivate
    protected void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "Java Batch SMF Recording feature deactivated, reason=" + reason);
    }

    /**
     * Remember smf
     *
     * @param em the smf reference
     */
    @Reference(name = "smf", service = com.ibm.ws.zos.core.utils.Smf.class)
    protected void setSmf(com.ibm.ws.zos.core.utils.Smf smf) {
        this.smf = smf;
    }

    /**
     * Forget Smf (if we had remembered it)
     *
     * @param smf The SMF reference to forget
     */
    protected void unsetSmf(com.ibm.ws.zos.core.utils.Smf smf) {
        if (this.smf == smf) {
            this.smf = null;
        }
    }

    /**
     * Remember the Native Utility object reference
     *
     * @param the Native Utility object reference
     */
    @Reference(name = "nativeUtils", service = com.ibm.ws.zos.core.utils.NativeUtils.class)
    protected void setNativeUtils(NativeUtils nativeUtils) {
        this.nativeUtils = nativeUtils;
    }

    /**
     * Forget the Native Utility object reference (if we had remembered it)
     *
     * @param the Native Utility object reference to forget
     */
    protected void unsetNativeUtils(NativeUtils nativeUtils) {
        if (this.nativeUtils == nativeUtils) {
            this.nativeUtils = null;
        }
    }

    /**
     * Sets the MvsCommonFields object reference.
     *
     * @param mvsCommonFields The MvsCommonFields reference.
     */
    @Reference(name = "mvcCommonFields", service = com.ibm.ws.zos.core.structures.MvsCommonFields.class)
    protected void setMvsCommonFields(MvsCommonFields mvsCommonFields) {
        this.mvsCommonFields = mvsCommonFields;
    }

    /**
     * Unsets the MvsCommonFields object reference.
     *
     * @param mvsCommonFields The MvsCommonFields reference.
     */
    protected void unsetMvsCommonFields(MvsCommonFields mvsCommonFields) {
        if (this.mvsCommonFields == mvsCommonFields) {
            this.mvsCommonFields = null;
        }
    }

    /**
     * Sets the WsLocationAdmin reference.
     *
     * @param locationAdmin The WsLocationAdmin reference.
     */
    @Reference(name = "locationAdmin", service = WsLocationAdmin.class)
    protected void setLocationAdmin(WsLocationAdmin locationAdmin) {
        this.locationAdmin = locationAdmin;
    }

    /**
     * Clears the WsLocationAdmin reference.
     *
     * @param locationAdmin The WsLocationAdmin reference.
     */
    protected void unsetLocationAdmin(WsLocationAdmin locationAdmin) {
        if (this.locationAdmin == locationAdmin) {
            this.locationAdmin = null;
        }
    }

    private static final int RECORD_HEADER_SIZE = 48;
    private static final int TRIPLET_SIZE = 12;
    private static final int CURRENT_NUMBER_OF_TRIPLETS = 8; // Note if you change this change the value in record_header
    private static final int RECORD_HEADER_AND_TRIPLETS_SIZE = RECORD_HEADER_SIZE + (CURRENT_NUMBER_OF_TRIPLETS * TRIPLET_SIZE);
    private static final int REFERENCE_SECTION_LENGTH = 140;
    private static final int SERVER_CONFIG_DIR_LENGTH_MAX = 128;
    private static final int PRODUCT_VERSION_BYTES_MAX = 16;
    private static final int REPOSITORY_TYPE_LENGTH = 4;
    private static final int JOB_STORE_REF_LENGTH_MAX = 16;
    private static final int JES_JOB_IDENTIFIER_LENGTH = 8;
    private static final int COMMON_MAX_STRING_LENGTH = 32;
    private static final int DISPATCH_TCB_TTOKEN_LENGTH = 20;
    private static final int EXIT_STATUS_MAX_LENGTH = 128;
    private static final int REFERENCE_SECTION_BUFFER_MAX_LENGTH = 128;
    private static final int ACCOUNTING_MAX_LENGTH = 128;
    private static final int IDENTIFICATION_FLAGS_SIZE = 8;
    private static final String SUBMITTER_JOBNAME_PROPERTY = "com.ibm.ws.batch.submitter.jobName";
    private static final String SUBMITTER_JOBID_PROPERTY = "com.ibm.ws.batch.submitter.jobId";
    private static final String ACCOUNTING_STRING_PROPERTY = "com.ibm.ws.batch.accountingString";
    public final static String ASCII = "Cp850";
    private static final int MAX_NEGATIVE = -2147483648;
    //private static final int USER_DATA_TOTAL_SIZE = UserData.USER_DATA_MAX_SIZE + 12;

    /** Record types */
    private static final int STEP_ENDED_TYPE = 1;
    private static final int JOB_ENDED_TYPE = 2;
    private static final int PARTITION_ENDED_TYPE = 3;
    private static final int FLOW_ENDED_TYPE = 4;
    private static final int DECIDER_ENDED_TYPE = 5;

    private static final String PRODUCT_NAME = "com.ibm.websphere.appserver";

    /** Current versions. */
    private static final int CURRENT_RECORD_VERSION = 3;
    private static final int CURRENT_SUBSYSTEM_DATA_VERSION = 3;
    private static final int CURRENT_IDENTIFICATION_SECTION_VERSION = 2;
    private static final int CURRENT_COMPLETION_SECTION_VERSION = 2;
    private static final int CURRENT_PROCESSOR_SECTION_VERSION = 1;
    private static final int CURRENT_ACCOUNTING_SECTION_VERSION = 1;
    private static final int CURRENT_USS_SECTION_VERSION = 2;
    private static final int CURRENT_REFERENCE_NAMES_SECTION_VERSION = 1;

    /** Reference types */
    private static final int READER_REF_TYPE = 1;
    private static final int PROCESSOR_REF_TYPE = 2;
    private static final int WRITER_REF_TYPE = 3;
    private static final int CHECKPOINT_REF_TYPE = 4;
    private static final int BATCHLET_REF_TYPE = 5;
    private static final int PARTITION_MAPPER_REF_TYPE = 6;
    private static final int PARTITION_REDUCER_REF_TYPE = 7;
    private static final int PARTITION_COLLECTOR_REF_TYPE = 8;
    private static final int PARTITION_ANALYZER_REF_TYPE = 9;
    private static final int DECIDER_REF_TYPE = 10;

    /** SMF 120-12 record header. This follows the 2 byte length. */
    private static final byte record_header[] = new byte[] {
                                                             (byte) 0x00, (byte) 0x00, /* Two bytes, unused by us */
                                                             (byte) 0x5E, /* One byte, subtypes used (bit 01000000) and MVS version (rest) */
                                                             (byte) 0x78, /* One byte, record type 120 */
                                                             (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, /* Four bytes, write-time, filled in by SMF */
                                                             (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, /* Four bytes, write-date, filled in by SMF */
                                                             (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, /* Four bytes, system-id, just EBCDIC blanks */
                                                             (byte) 0xE6, (byte) 0XC1, (byte) 0xE2, (byte) 0x40, /* Four bytes, subsystem-id, "WAS " in EBCDIC */
                                                             (byte) 0x00, (byte) 0x0C, /* Two byte, subtype, using '12' for this record */
                                                             (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, /*
                                                                                                                  * Four bytes, version number for record: if changed update
                                                                                                                  * CURRENT_RECORD_VERSION
                                                                                                                  */
                                                             (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08, /*
                                                                                                                  * Four bytes, number of triplets if changed update
                                                                                                                  * CURRENT_NUMBER_OF_TRIPLETS
                                                                                                                  */
                                                             (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, /* Four bytes, record-index. No split-records so zero */
                                                             (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, /* Four bytes, total-records, no splits, so one */
                                                             (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, /* Eight bytes, continuation token, no splits so zero */
                                                             (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 /* rest of it */
    };

    private static final byte empty_triplet[] = new byte[] {
                                                             (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                             (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                                             (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

    /* Triplet - server info section count (always 1) */
    private static final byte subsystemDataCountForTriplet[] = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };

    private static final byte identificationCountForTriplet[] = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };

    private static final byte completionCountForTriplet[] = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };

    private static final byte processorCountForTriplet[] = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };

    private static final byte ussCountForTriplet[] = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };

    private final static String Blanks32 = "                                ";

    private final static String Blanks8 = "        ";

    /** An empty byte array used to pad exit status to length */
    private static final byte[] nulls = new byte[EXIT_STATUS_MAX_LENGTH];

    /**
     * Build and write a job ended record. Call from com.ibm.jbatch.container.execution.impl.RuntimeJobExecution.publishEvent( )
     *
     * @param jobExecution   The job execution object
     * @param timeUsedBefore captured CPU time at the start of the job
     * @param timeUsedAfter  captured CPU time at the end of the job (now)
     * @return return code from writing the record
     */
    @Override
    public int buildAndWriteJobEndRecord(WSJobExecution jobExecution,
                                         RuntimeWorkUnitExecution runtimeJobExecution,
                                         String repositoryType,
                                         String jobStoreRef,
                                         byte[] timeUsedBefore,
                                         byte[] timeUsedAfter) {
        byte[] idSection = getIdentificationSection(jobExecution, runtimeJobExecution);
        byte[] compSection = getCompletionSection(jobExecution);
        byte[] processorSection = getProcessorSection(timeUsedBefore, timeUsedAfter);
        LinkedList<byte[]> acctSections = getAccountingSections(jobExecution.getJobParameters());
        byte[] ussSection = getUssSection(jobExecution.getJobInstance().getSubmitter());

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "building the buffer and calling native smf");
        }

        int smfrc = buildAndWriteRecord(JOB_ENDED_TYPE,
                                        repositoryType,
                                        jobStoreRef,
                                        idSection,
                                        compSection,
                                        processorSection,
                                        acctSections,
                                        ussSection,
                                        null);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "built the buffer and called native smf - rc from smf = " + smfrc);
        }

        setLastSMFRC(smfrc);
        return smfrc;

        //UserDataImpl.clearUserData(); // Done with this thread so clear any user data

    }

    @Override
    public int buildAndWriteDeciderEndRecord(String exitStatus,
                                             RuntimeWorkUnitExecution execution,
                                             WSJobExecution jobExecution,
                                             String decisionRefName,
                                             String repositoryType,
                                             String jobStoreRef,
                                             Date startTime,
                                             Date endTime,
                                             byte[] timeUsedBefore,
                                             byte[] timeUsedAfter) {

        byte[] idSection = getIdentificationSection(jobExecution, startTime, endTime);
        byte[] compSection = getCompletionSection(exitStatus, execution);
        byte[] processorSection = getProcessorSection(timeUsedBefore, timeUsedAfter);
        LinkedList<byte[]> acctSections = getAccountingSections(jobExecution.getJobParameters());
        LinkedList<byte[]> referencesSection = getReferenceNamesSections(decisionRefName);
        byte[] ussSection = getUssSection(jobExecution.getJobInstance().getSubmitter());

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "building the buffer and calling native smf");
        }

        int smfrc = buildAndWriteRecord(DECIDER_ENDED_TYPE,
                                        repositoryType,
                                        jobStoreRef,
                                        idSection,
                                        compSection,
                                        processorSection,
                                        acctSections,
                                        ussSection,
                                        referencesSection);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "built the buffer and called native smf - rc from smf = " + smfrc);
        }

        setLastSMFRC(smfrc);
        return smfrc;
    }

    /**
     * Build and write a step ended record
     *
     * @param stepExecution            the step execution object. When we publish the step ended event we have a WSStepThreadExecutionAggregate
     *                                     which we can use to get the WSTopLevelStepExecution by calling getTopLevelStepExecution( ) on the WSStepThreadExecutionAggregate.
     * @param runtimeWorkUnitExecution an attribute of com.ibm.jbatch.container.controller.impl.BaseStepControllerImpl where we should
     *                                     be called from (in the inner class TopLevelThreadHelper.publishEvent( )
     * @param timeUsedBefore           captured CPU time at the start of the job
     * @param timeUsedAfter            captured CPU time at the end of the job (now)
     * @return return code from writing the record
     */
    @Override
    public int buildAndWriteStepEndRecord(WSTopLevelStepExecution stepExecution,
                                          WSJobExecution jobExecution,
                                          RuntimeWorkUnitExecution runtimeWorkUnitExecution,
                                          int partitionPlanCount,
                                          int partitionCount,
                                          String repositoryType,
                                          String jobStoreRef,
                                          Step step,
                                          boolean isPartitionedStep,
                                          byte[] timeUsedBefore,
                                          byte[] timeUsedAfter) {

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "preparing the buffer to be recorded with info from the stepexecution");
        }

        byte[] idSection = getIdentificationSection(stepExecution, jobExecution, runtimeWorkUnitExecution, step, isPartitionedStep);
        byte[] compSection = getCompletionSection(stepExecution, partitionPlanCount, partitionCount);
        byte[] processorSection = getProcessorSection(timeUsedBefore, timeUsedAfter);
        LinkedList<byte[]> acctSections = getAccountingSections(jobExecution.getJobParameters());
        LinkedList<byte[]> referencesSection = getReferenceNamesSections(step);
        byte[] ussSection = getUssSection(jobExecution.getJobInstance().getSubmitter());

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "filled the buffer to be recorded with info from the stepexecution, calling native to record");
        }

        int smfrc = buildAndWriteRecord(STEP_ENDED_TYPE,
                                        repositoryType,
                                        jobStoreRef,
                                        idSection,
                                        compSection,
                                        processorSection,
                                        acctSections,
                                        ussSection,
                                        referencesSection);

        setLastSMFRC(smfrc);
        return smfrc;

//      UserDataImpl.clearUserData();  do NOT clear user data at step end..should also show up in job-end/flow-end record.

    }

    @Override
    public int buildAndWriteFlowEndRecord(RuntimeSplitFlowExecution runtimeSplitFlowExecution,
                                          WSJobExecution jobExecution,
                                          String repositoryType,
                                          String jobStoreRef) {
        byte[] idSection = getIdentificationSection(runtimeSplitFlowExecution, jobExecution);
        byte[] compSection = getCompletionSection(runtimeSplitFlowExecution);
        LinkedList<byte[]> acctSections = getAccountingSections(jobExecution.getJobParameters());
        byte[] ussSection = getUssSection(jobExecution.getJobInstance().getSubmitter());

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "writing flow end record....");
        }

        int smfrc = buildAndWriteRecord(FLOW_ENDED_TYPE,
                                        repositoryType,
                                        jobStoreRef,
                                        idSection,
                                        compSection,
                                        null,
                                        acctSections,
                                        ussSection,
                                        null);

        setLastSMFRC(smfrc);
        return smfrc;
    }

    /**
     * Build and write a partition end record.
     *
     * @param runtimePartitionExecution the RuntimePartitionExecution object for this partition
     * @param timeUsedBefore            captured CPU time at the start of the job
     * @param timeUsedAfter             captured CPU time at the end of the job (now)
     * @return return code from writing the record
     */
    @Override
    public int buildAndWritePartitionEndRecord(RuntimePartitionExecution runtimePartitionExecution,
                                               WSJobExecution jobExecution,
                                               RuntimeStepExecution runtimeStepExecution,
                                               String repositoryType,
                                               String jobStoreRef,
                                               byte[] timeUsedBefore,
                                               byte[] timeUsedAfter) {
        byte[] idSection = getIdentificationSection(runtimePartitionExecution, jobExecution, runtimeStepExecution.getStartTime(), runtimeStepExecution.getEndTime());
        byte[] compSection = getCompletionSection(runtimeStepExecution);
        byte[] processorSection = getProcessorSection(timeUsedBefore, timeUsedAfter);
        LinkedList<byte[]> acctSections = getAccountingSections(jobExecution.getJobParameters());
        byte[] ussSection = getUssSection(jobExecution.getJobInstance().getSubmitter());

        int smfrc = buildAndWriteRecord(PARTITION_ENDED_TYPE,
                                        repositoryType,
                                        jobStoreRef,
                                        idSection,
                                        compSection,
                                        processorSection,
                                        acctSections,
                                        ussSection,
                                        null);

        setLastSMFRC(smfrc);
        return smfrc;
    }

    /**
     * Build and write a Liberty Batch SMF record
     *
     * @param type                       the type of record, job end, step end, etc.
     * @param identificationSectionBytes filled in identification section appropriate to type
     * @param completionSectionBytes     filled in completion section appropriate to type
     * @param processorSectionBytes      filled in processor section appropriate to type
     * @param accountingSections         a linked list of byte arrays, each a whole accounting section
     * @param ussSectionBytes            the USS section filled in
     * @param referenceNamesSections     a linked list of byte arrays, each a whole reference name section
     * @return return code from SMF write
     */
    private int buildAndWriteRecord(int type,
                                    String repositoryType,
                                    String jobStoreRef,
                                    byte[] identificationSectionBytes,
                                    byte[] completionSectionBytes,
                                    byte[] processorSectionBytes,
                                    LinkedList<byte[]> accountingSections,
                                    byte[] ussSectionBytes,
                                    LinkedList<byte[]> referenceNamesSections) {

        // create a record...
        ByteArrayOutputStream record = new ByteArrayOutputStream();

        short recordLength = 0;

        // get the server Info setup for this type
        byte[] subsystemData = getSubsystemData(type, repositoryType, jobStoreRef);

        // Get the user data for this thread
        //HashMap<Integer, byte[]> userDataBytes = UserDataImpl.getUserDataBytes();

        // Convert counts to byte arrays for triplets
        byte[] accountingSectionCountForTriplet = null;
        if (accountingSections != null) {
            accountingSectionCountForTriplet = intToBytes(accountingSections.size());
        } else {
            accountingSectionCountForTriplet = intToBytes(0);
        }

        byte[] referenceNamesSectionCountForTriplet = null;
        if (referenceNamesSections != null && referenceNamesSections.size() > 0) {
            referenceNamesSectionCountForTriplet = intToBytes(referenceNamesSections.size());
        } else {
            referenceNamesSectionCountForTriplet = intToBytes(0);
        }

        // Figure out what we have and how long the whole record is
        int currentOffset = RECORD_HEADER_AND_TRIPLETS_SIZE + subsystemData.length;

        byte[] identificationOffsetBytes = intToBytes(currentOffset);
        currentOffset = currentOffset + identificationSectionBytes.length;

        byte[] completionOffsetBytes = intToBytes(currentOffset);
        currentOffset = currentOffset + completionSectionBytes.length;

        byte[] processorOffsetBytes = intToBytes(currentOffset);
        if (processorSectionBytes != null) {
            currentOffset = currentOffset + processorSectionBytes.length;
        }

        byte[] accountingOffsetBytes = intToBytes(currentOffset);
        if (accountingSections != null) {
            // multiply length of first section in list by size of list.  All sections are the same length
            currentOffset = currentOffset + (accountingSections.getFirst().length * accountingSections.size());
        }

        byte[] ussOffsetBytes = intToBytes(currentOffset);
        currentOffset = currentOffset + ussSectionBytes.length;

        byte[] referenceOffsetBytes = intToBytes(currentOffset);
        if (referenceNamesSections != null) {
            // multiply length of first section in list by size of list.  All sections are the same length
            currentOffset = currentOffset + (referenceNamesSections.getFirst().length * referenceNamesSections.size());
        }

        int countForUserDataTriplet = 0; // short circuit to write out a triplet of 0's for userdata

        /*
         * byte[] userDataOffsetBytes = intToBytes(currentOffset);
         * int countForUserDataTriplet = userDataBytes.size();
         * if (countForUserDataTriplet != 0) {
         * if (countForUserDataTriplet > UserData.USER_DATA_MAX_COUNT) {
         * countForUserDataTriplet = UserData.USER_DATA_MAX_COUNT;
         * }
         * currentOffset = currentOffset + (countForUserDataTriplet * USER_DATA_TOTAL_SIZE);
         * }
         */

        // Write length of whole record. It is 2 bytes
        recordLength = (short) currentOffset;
        byte[] recordLengthBytes = shortToBytes(recordLength);
        record.write(recordLengthBytes, 0, recordLengthBytes.length);
        // write in the fixed header
        record.write(record_header, 0, record_header.length);

        // write subsystem info triplet. offset length count
        record.write(intToBytes(RECORD_HEADER_AND_TRIPLETS_SIZE), 0, 4); // triplet offset
        record.write(intToBytes(subsystemData.length), 0, 4); // triplet length
        record.write(subsystemDataCountForTriplet, 0, 4); // triplet count

        // write identification triplet. offset length count
        record.write(identificationOffsetBytes, 0, 4); // triplet offset
        record.write(intToBytes(identificationSectionBytes.length), 0, 4); // triplet length
        record.write(identificationCountForTriplet, 0, 4); // triplet count

        // write completion triplet. offset length count
        record.write(completionOffsetBytes, 0, 4); // triplet offset
        record.write(intToBytes(completionSectionBytes.length), 0, 4); // triplet length
        record.write(completionCountForTriplet, 0, 4); // triplet count

        // write processor triplet. offset length count
        if (processorSectionBytes != null) {
            record.write(processorOffsetBytes, 0, 4); // triplet offset
            record.write(intToBytes(processorSectionBytes.length), 0, 4); // triplet length
            record.write(processorCountForTriplet, 0, 4); // triplet count
        } else {
            record.write(empty_triplet, 0, empty_triplet.length);
        }

        // write accounting triplet. offset length count
        if (accountingSections != null) {
            record.write(accountingOffsetBytes, 0, 4); // triplet offset
            record.write(intToBytes(accountingSections.getFirst().length), 0, 4); // triplet length of ONE section
            record.write(accountingSectionCountForTriplet, 0, 4); // triplet count
        } else {
            record.write(empty_triplet, 0, empty_triplet.length);
        }

        // write USS triplet. offset length count
        record.write(ussOffsetBytes, 0, 4); // triplet offset
        record.write(intToBytes(ussSectionBytes.length), 0, 4); // triplet length
        record.write(ussCountForTriplet, 0, 4); // triplet count

        // write reference triplet. offset length count
        if (referenceNamesSections != null) {
            record.write(referenceOffsetBytes, 0, 4); // triplet offset
            record.write(intToBytes(referenceNamesSections.getFirst().length), 0, 4); // triplet length
            record.write(referenceNamesSectionCountForTriplet, 0, 4); // triplet count
        } else {
            record.write(empty_triplet, 0, empty_triplet.length);
        }

        // write user data triplet. offset length count
        if (countForUserDataTriplet != 0) {
            // offset length count
            /*
             * short circuit for user data
             * record.write(userDataOffsetBytes, 0, 4); // triplet offset
             * record.write(intToBytes(USER_DATA_TOTAL_SIZE), 0, 4); // triplet length
             * record.write(intToBytes(countForUserDataTriplet), 0, 4); // triplet count
             */
        } else {
            record.write(empty_triplet, 0, empty_triplet.length);
        }

        // Write the subsystem data into the record
        record.write(subsystemData, 0, subsystemData.length);

        // Write the identification section into the record
        record.write(identificationSectionBytes, 0, identificationSectionBytes.length);

        // Write the completion section into the record
        record.write(completionSectionBytes, 0, completionSectionBytes.length);

        // Write the processor section into the record
        if (processorSectionBytes != null) {
            record.write(processorSectionBytes, 0, processorSectionBytes.length);
        }

        // Write the accounting section into the record
        if (accountingSections != null) {
            ListIterator<byte[]> li = accountingSections.listIterator();
            while (li.hasNext()) {
                byte[] nextSection = li.next();
                record.write(nextSection, 0, nextSection.length);
            }
        }

        // Write the USS section into the record
        record.write(ussSectionBytes, 0, ussSectionBytes.length);

        // Write the reference names section into the record
        if (referenceNamesSections != null) {
            ListIterator<byte[]> li = referenceNamesSections.listIterator();
            while (li.hasNext()) {
                byte[] nextSection = li.next();
                record.write(nextSection, 0, nextSection.length);
            }
        }

        // Write the user data into the record
        if (countForUserDataTriplet != 0) {
            /*
             * short circuit
             * Iterator<Integer> it = (userDataBytes.keySet()).iterator();
             * int sectionsWritten = 0;
             * while (it.hasNext()) {
             * byte[] data = userDataBytes.get(it.next());
             * if (sectionsWritten >= countForUserDataTriplet) {
             * if (tc.isDebugEnabled()) {
             * Tr.debug(tc, "Sections written " + sectionsWritten + " is more than expected " + countForUserDataTriplet + " map size is " + userDataBytes.size());
             * }
             * break;
             * }
             * record.write(data, 0, data.length);
             * sectionsWritten = sectionsWritten + 1;
             * }
             */
        }

        // Oh look..we do the actual point of the method in the return..cool.
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "calling native smf120 subtype 12");
        }

        int smfrc = smf.smfRecordT120S12Write(record.toByteArray());

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "called native smf120 subtype 12");
        }
        return smfrc;

    }

    /**
     * Gets the identification section for a job, filled in properly
     *
     * @param jobExecution the job execution object
     * @return a filled in identification section
     */
    public byte[] getIdentificationSection(WSJobExecution jobExecution, RuntimeWorkUnitExecution runtimejobExecution) {

        byte[] identificationSectionVersion = intToBytes(CURRENT_IDENTIFICATION_SECTION_VERSION);

        ByteArrayOutputStream identificationSection = new ByteArrayOutputStream();
        identificationSection.write(identificationSectionVersion, 0, identificationSectionVersion.length);

        byte[] jobInstanceBytes = longToBytes(jobExecution.getInstanceId());
        identificationSection.write(jobInstanceBytes, 0, jobInstanceBytes.length);

        byte[] jobExecutionBytes = longToBytes(jobExecution.getExecutionId());
        identificationSection.write(jobExecutionBytes, 0, jobExecutionBytes.length);

        byte[] jobExecutionNumberBytes = longToBytes(jobExecution.getExecutionNumberForThisInstance());
        identificationSection.write(jobExecutionNumberBytes, 0, jobExecutionNumberBytes.length);

        byte[] stepExecutionBytes = longToBytes(0L); // No step id for job end
        identificationSection.write(stepExecutionBytes, 0, stepExecutionBytes.length);

        byte[] partitionNumberBytes = longToBytes(0L); // No partition number for job end
        identificationSection.write(partitionNumberBytes, 0, partitionNumberBytes.length);

        byte[] jobNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobExecution.getJobName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(jobNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        WSJobInstance jobInstance = jobExecution.getJobInstance();

        byte[] appNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobInstance.getAmcName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(appNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] xmlNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobInstance.getJobXMLName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(xmlNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] stepNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks32, COMMON_MAX_STRING_LENGTH); // no step for job end
        identificationSection.write(stepNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] splitNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks32, COMMON_MAX_STRING_LENGTH); // no split for job end
        identificationSection.write(splitNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] flowNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks32, COMMON_MAX_STRING_LENGTH); // no flow for job end
        identificationSection.write(flowNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        // Liberty Batch SMF Design doc pg 16: For a job, step, and decider end: create time is the time the job execution was created
        byte[] createTimeBytes = longToBytes(jobExecution.getCreateTime().getTime());
        identificationSection.write(createTimeBytes, 0, createTimeBytes.length);

        byte[] startTimeBytes = longToBytes(jobExecution.getStartTime().getTime());
        identificationSection.write(startTimeBytes, 0, startTimeBytes.length);

        byte[] endTimeBytes = longToBytes(jobExecution.getEndTime().getTime());
        identificationSection.write(endTimeBytes, 0, endTimeBytes.length);

        byte[] submitterBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobInstance.getSubmitter(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(submitterBytes, 0, COMMON_MAX_STRING_LENGTH);

        Properties jobParameters = jobExecution.getJobParameters();
        String submitterJobName = null;

        if (jobParameters != null) {
            submitterJobName = jobParameters.getProperty(SUBMITTER_JOBNAME_PROPERTY);
        }

        byte[] submitterJobNameBytes;
        if (submitterJobName != null) {
            submitterJobNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(submitterJobName, JES_JOB_IDENTIFIER_LENGTH);
        } else {
            submitterJobNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks8, JES_JOB_IDENTIFIER_LENGTH);
        }
        identificationSection.write(submitterJobNameBytes, 0, JES_JOB_IDENTIFIER_LENGTH);

        String submitterJobId = null;
        if (jobParameters != null) {
            submitterJobId = jobParameters.getProperty(SUBMITTER_JOBID_PROPERTY);
        }

        byte[] submitterJobIdBytes;
        if (submitterJobId != null) {
            submitterJobIdBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(submitterJobId, JES_JOB_IDENTIFIER_LENGTH);
        } else {
            submitterJobIdBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks8, JES_JOB_IDENTIFIER_LENGTH);
        }
        identificationSection.write(submitterJobIdBytes, 0, JES_JOB_IDENTIFIER_LENGTH);

        byte[] nativeSMFData = nativeUtils.getSmfData(); // psatold 4 ttoken 16 thread id 8 and cvtldto 8
        identificationSection.write(nativeSMFData, 0, 20); // TCB@ and TTOKEN together

        /**
         * NEED TO ADD THESE from JSL, some only for step
         * struct flags {
         * jobRestartableAttr:1; // From JSL Job attribute
         * stepAllowStartIfComplete:1; // From JSL Step attribute
         * partitionStep:1; // For Step-End, partitioned step
         * reserved:61; // reserved
         * int stepStartLimit; // From JSL step start limit
         * int chunkStepCheckpointPolicy;// From JSL for chunk:0=Item, 1=Custom
         * int chunkStepItemCount; // From JSL for chunk item count
         * int chunkStepTimeLimit; // From JSL for chunk time limit
         * int chunkStepSkipLimit; // From JSL for chunk skip limit
         * int chunkStepRetryLimit; // From JSL for chunk retry limit
         */

        byte[] flags = new byte[8];

        if (runtimejobExecution.getJobNavigator().getRootModelElement().getRestartable() != null) {
            if (runtimejobExecution.getJobNavigator().getRootModelElement().getRestartable().equals("true"))
                flags[0] |= 1 << 0;
        }

        identificationSection.write(flags, 0, IDENTIFICATION_FLAGS_SIZE);

        byte[] stepStartLimit = intToBytes(0);
        identificationSection.write(stepStartLimit, 0, stepStartLimit.length);

        byte[] chunkStepCheckpointPolicy = intToBytes(0);
        identificationSection.write(chunkStepCheckpointPolicy, 0, chunkStepCheckpointPolicy.length);

        byte[] chunkStepItemCount = intToBytes(0);
        identificationSection.write(chunkStepItemCount, 0, chunkStepItemCount.length);

        byte[] chunkStepTimeLimit = intToBytes(0);
        identificationSection.write(chunkStepTimeLimit, 0, chunkStepTimeLimit.length);

        byte[] chunkStepSkipLimit = intToBytes(0);
        identificationSection.write(chunkStepSkipLimit, 0, chunkStepSkipLimit.length);

        byte[] chunkStepRetryLimit = intToBytes(0);
        identificationSection.write(chunkStepRetryLimit, 0, chunkStepRetryLimit.length);

        return identificationSection.toByteArray();

    }

    /**
     * Gets the identification section for a step, filled in properly
     *
     * @param stepExecution            the step execution object. When we publish the step ended event we have a WSStepThreadExecutionAggregate
     *                                     which we can use to get the WSTopLevelStepExecution by calling getTopLevelStepExecution( ) on the WSStepThreadExecutionAggregate.
     * @param runtimeWorkUnitExecution an attribute of BaseStepControllerImpl where we should be called from
     * @param jobExecution             the job execution object
     * @return a filled in identification section
     */
    public byte[] getIdentificationSection(WSTopLevelStepExecution stepExecution,
                                           WSJobExecution jobExecution,
                                           RuntimeWorkUnitExecution runtimeWorkUnitExecution,
                                           Step step,
                                           boolean isPartitionedStep) {

        byte[] identificationSectionVersion = intToBytes(CURRENT_IDENTIFICATION_SECTION_VERSION);

        ByteArrayOutputStream identificationSection = new ByteArrayOutputStream();
        identificationSection.write(identificationSectionVersion, 0, identificationSectionVersion.length);

        byte[] jobInstanceBytes = longToBytes(stepExecution.getJobInstanceId());
        identificationSection.write(jobInstanceBytes, 0, jobInstanceBytes.length);

        byte[] jobExecutionBytes = longToBytes(stepExecution.getJobExecutionId());
        identificationSection.write(jobExecutionBytes, 0, jobExecutionBytes.length);

        byte[] jobExecutionNumberBytes = longToBytes(jobExecution.getExecutionNumberForThisInstance());
        identificationSection.write(jobExecutionNumberBytes, 0, jobExecutionNumberBytes.length);

        byte[] stepExecutionBytes = longToBytes(stepExecution.getStepExecutionId());
        identificationSection.write(stepExecutionBytes, 0, stepExecutionBytes.length);

        byte[] partitionNumberBytes = longToBytes(0L); // No partition number for step end
        identificationSection.write(partitionNumberBytes, 0, partitionNumberBytes.length);

        byte[] jobNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobExecution.getJobName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(jobNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        WSJobInstance jobInstance = jobExecution.getJobInstance();

        byte[] appNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobInstance.getAmcName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(appNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] xmlNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobInstance.getJobXMLName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(xmlNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] stepNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(stepExecution.getStepName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(stepNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] splitNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks32, COMMON_MAX_STRING_LENGTH); // no split for job end
        identificationSection.write(splitNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] flowNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks32, COMMON_MAX_STRING_LENGTH); // no flow for job end
        identificationSection.write(flowNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        // Liberty Batch SMF Design doc pg 16: For a job, step, and decider end: create time is the time the job execution was created
        byte[] createTimeBytes = longToBytes(jobExecution.getCreateTime().getTime());
        identificationSection.write(createTimeBytes, 0, createTimeBytes.length);

        byte[] startTimeBytes = longToBytes(stepExecution.getStartTime().getTime());
        identificationSection.write(startTimeBytes, 0, startTimeBytes.length);

        byte[] endTimeBytes = longToBytes(stepExecution.getEndTime().getTime());
        identificationSection.write(endTimeBytes, 0, endTimeBytes.length);

        byte[] submitterBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobInstance.getSubmitter(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(submitterBytes, 0, COMMON_MAX_STRING_LENGTH);

        Properties jobParameters = jobExecution.getJobParameters();
        String submitterJobName = jobParameters.getProperty(SUBMITTER_JOBNAME_PROPERTY);

        byte[] submitterJobNameBytes;
        if (submitterJobName != null) {
            submitterJobNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(submitterJobName, JES_JOB_IDENTIFIER_LENGTH);
        } else {
            submitterJobNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks8, JES_JOB_IDENTIFIER_LENGTH);
        }
        identificationSection.write(submitterJobNameBytes, 0, JES_JOB_IDENTIFIER_LENGTH);

        String submitterJobId = jobParameters.getProperty(SUBMITTER_JOBID_PROPERTY);

        byte[] submitterJobIdBytes;
        if (submitterJobId != null) {
            submitterJobIdBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(submitterJobId, JES_JOB_IDENTIFIER_LENGTH);
        } else {
            submitterJobIdBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks8, JES_JOB_IDENTIFIER_LENGTH);
        }
        identificationSection.write(submitterJobIdBytes, 0, JES_JOB_IDENTIFIER_LENGTH);

        byte[] nativeSMFData = nativeUtils.getSmfData(); // psatold 4 ttoken 16 thread id 8 and cvtldto 8
        identificationSection.write(nativeSMFData, 0, 20); // TCB@ and TTOKEN together

        /**
         * NEED TO ADD THESE from JSL, some only for step
         * struct flags {
         * jobRestartableAttr:1; // From JSL Job attribute
         * stepAllowStartIfComplete:1; // From JSL Step attribute
         * partitionStep:1; // For Step-End, partitioned step
         * reserved:61; // reserved
         * int stepStartLimit; // From JSL step start limit
         * int chunkStepCheckpointPolicy;// From JSL for chunk:0=Item, 1=Custom
         * int chunkStepItemCount; // From JSL for chunk item count
         * int chunkStepTimeLimit; // From JSL for chunk time limit
         * int chunkStepSkipLimit; // From JSL for chunk skip limit
         * int chunkStepRetryLimit; // From JSL for chunk retry limit
         */

        // flags 8 bytes.  Only using 3 bits for now, 61 reserved
        byte[] flags = new byte[8];

        JSLJob job = runtimeWorkUnitExecution.getJobNavigator().getRootModelElement();

        // set first bit (idx 0) if restartable attr set
        if (job.getRestartable() != null && job.getRestartable().equals("true"))
            flags[0] |= 1 << 0;

        // set second bit (idx 1) if allowStartIfComplete attr set
        if (step.getAllowStartIfComplete() != null && step.getAllowStartIfComplete().equals("true"))
            flags[0] |= 1 << 1;

        // set third bit (idx 2) if partitioned step
        if (isPartitionedStep)
            flags[0] |= 1 << 2;

        identificationSection.write(flags, 0, IDENTIFICATION_FLAGS_SIZE);

        byte[] stepStartLimit = (step.getStartLimit() != null) ? intToBytes(Integer.parseInt(step.getStartLimit())) : intToBytes(0);

        identificationSection.write(stepStartLimit, 0, stepStartLimit.length);

        Chunk chunk = step.getChunk();

        if (chunk != null) {
            int checkpointPolicy = (chunk.getCheckpointPolicy() != null && chunk.getCheckpointPolicy().equals("custom")) ? 1 : 0;

            if (chunk.getCheckpointPolicy() != null && chunk.getCheckpointPolicy().equals("custom"))
                checkpointPolicy = 1;

            byte[] chunkStepCheckpointPolicy = intToBytes(checkpointPolicy);
            identificationSection.write(chunkStepCheckpointPolicy, 0, chunkStepCheckpointPolicy.length);

            byte[] chunkStepItemCount = (chunk.getItemCount() != null) ? intToBytes(Integer.parseInt(chunk.getItemCount())) : intToBytes(0);
            identificationSection.write(chunkStepItemCount, 0, chunkStepItemCount.length);

            byte[] chunkStepTimeLimit = (chunk.getTimeLimit() != null) ? intToBytes(Integer.parseInt(chunk.getTimeLimit())) : intToBytes(0);
            identificationSection.write(chunkStepTimeLimit, 0, chunkStepTimeLimit.length);

            byte[] chunkStepSkipLimit = (chunk.getSkipLimit() != null) ? intToBytes(Integer.parseInt(chunk.getSkipLimit())) : intToBytes(0);
            identificationSection.write(chunkStepSkipLimit, 0, chunkStepSkipLimit.length);

            byte[] chunkStepRetryLimit = (chunk.getRetryLimit() != null) ? intToBytes(Integer.parseInt(chunk.getRetryLimit())) : intToBytes(0);
            identificationSection.write(chunkStepRetryLimit, 0, chunkStepRetryLimit.length);
        } else {
            byte[] chunkStepCheckpointPolicy = intToBytes(0);
            identificationSection.write(chunkStepCheckpointPolicy, 0, chunkStepCheckpointPolicy.length);

            byte[] chunkStepItemCount = intToBytes(0);
            identificationSection.write(chunkStepItemCount, 0, chunkStepItemCount.length);

            byte[] chunkStepTimeLimit = intToBytes(0);
            identificationSection.write(chunkStepTimeLimit, 0, chunkStepTimeLimit.length);

            byte[] chunkStepSkipLimit = intToBytes(0);
            identificationSection.write(chunkStepSkipLimit, 0, chunkStepSkipLimit.length);

            byte[] chunkStepRetryLimit = intToBytes(0);
            identificationSection.write(chunkStepRetryLimit, 0, chunkStepRetryLimit.length);
        }

        return identificationSection.toByteArray();

    }

    /**
     * Gets the identification section for a flow, filled in properly
     *
     * @param runtimeSplitFlowExecution The runtime execution object for this flow
     * @return a filled in identification section
     */
    public byte[] getIdentificationSection(RuntimeSplitFlowExecution runtimeSplitFlowExecution, WSJobExecution jobExecution) {

        byte[] identificationSectionVersion = intToBytes(CURRENT_IDENTIFICATION_SECTION_VERSION);

        ByteArrayOutputStream identificationSection = new ByteArrayOutputStream();
        identificationSection.write(identificationSectionVersion, 0, identificationSectionVersion.length);

        byte[] jobInstanceBytes = longToBytes(runtimeSplitFlowExecution.getTopLevelInstanceId());
        identificationSection.write(jobInstanceBytes, 0, jobInstanceBytes.length);

        byte[] jobExecutionBytes = longToBytes(runtimeSplitFlowExecution.getTopLevelExecutionId());
        identificationSection.write(jobExecutionBytes, 0, jobExecutionBytes.length);

        byte[] jobExecutionNumberBytes = longToBytes(jobExecution.getExecutionNumberForThisInstance());
        identificationSection.write(jobExecutionNumberBytes, 0, jobExecutionNumberBytes.length);

        byte[] stepExecutionBytes = longToBytes(0L); // No step id for flow end
        identificationSection.write(stepExecutionBytes, 0, stepExecutionBytes.length);

        byte[] partitionNumberBytes = longToBytes(0L); // No partition number for flow end
        identificationSection.write(partitionNumberBytes, 0, partitionNumberBytes.length);

        byte[] jobNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(runtimeSplitFlowExecution.getTopLevelNameInstanceExecutionInfo().getJobName(),
                                                                                COMMON_MAX_STRING_LENGTH);
        identificationSection.write(jobNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        WSJobInstance jobInstance = jobExecution.getJobInstance();

        byte[] appNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobInstance.getAmcName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(appNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] xmlNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobInstance.getJobXMLName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(xmlNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] stepNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks32, COMMON_MAX_STRING_LENGTH); // No step name for flow end
        identificationSection.write(stepNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] splitNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(runtimeSplitFlowExecution.getSplitName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(splitNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] flowNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(runtimeSplitFlowExecution.getFlowName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(flowNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] createTimeBytes = longToBytes(runtimeSplitFlowExecution.getCreateTime().getTime());
        identificationSection.write(createTimeBytes, 0, createTimeBytes.length);

        byte[] startTimeBytes = longToBytes(runtimeSplitFlowExecution.getStartTime().getTime());
        identificationSection.write(startTimeBytes, 0, startTimeBytes.length);

        byte[] endTimeBytes = longToBytes(runtimeSplitFlowExecution.getEndTime().getTime());
        identificationSection.write(endTimeBytes, 0, endTimeBytes.length);

        byte[] submitterBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobInstance.getSubmitter(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(submitterBytes, 0, COMMON_MAX_STRING_LENGTH);

        Properties jobParameters = jobExecution.getJobParameters();
        String submitterJobName = jobParameters.getProperty(SUBMITTER_JOBNAME_PROPERTY);

        byte[] submitterJobNameBytes;
        if (submitterJobName != null) {
            submitterJobNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(submitterJobName, JES_JOB_IDENTIFIER_LENGTH);
        } else {
            submitterJobNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks8, JES_JOB_IDENTIFIER_LENGTH);
        }
        identificationSection.write(submitterJobNameBytes, 0, JES_JOB_IDENTIFIER_LENGTH);

        String submitterJobId = jobParameters.getProperty(SUBMITTER_JOBID_PROPERTY);

        byte[] submitterJobIdBytes;
        if (submitterJobId != null) {
            submitterJobIdBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(submitterJobId, JES_JOB_IDENTIFIER_LENGTH);
        } else {
            submitterJobIdBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks8, JES_JOB_IDENTIFIER_LENGTH);
        }
        identificationSection.write(submitterJobIdBytes, 0, JES_JOB_IDENTIFIER_LENGTH);

        byte[] nativeSMFData = nativeUtils.getSmfData(); // psatold 4 ttoken 16 thread id 8 and cvtldto 8
        identificationSection.write(nativeSMFData, 0, 20); // TCB@ and TTOKEN together

        /**
         * NEED TO ADD THESE from JSL, some only for step
         * struct flags {
         * jobRestartableAttr:1; // From JSL Job attribute
         * stepAllowStartIfComplete:1; // From JSL Step attribute
         * partitionStep:1; // For Step-End, partitioned step
         * reserved:61; // reserved
         * int stepStartLimit; // From JSL step start limit
         * int chunkStepCheckpointPolicy;// From JSL for chunk:0=Item, 1=Custom
         * int chunkStepItemCount; // From JSL for chunk item count
         * int chunkStepTimeLimit; // From JSL for chunk time limit
         * int chunkStepSkipLimit; // From JSL for chunk skip limit
         * int chunkStepRetryLimit; // From JSL for chunk retry limit
         */

        byte[] flags = new byte[8];

        JSLJob job = runtimeSplitFlowExecution.getJobNavigator().getRootModelElement();

        if (job.getRestartable() != null && job.getRestartable().equals("true"))
            flags[0] |= 1 << 0;

        identificationSection.write(flags, 0, IDENTIFICATION_FLAGS_SIZE);

        byte[] stepStartLimit = intToBytes(0);
        identificationSection.write(stepStartLimit, 0, stepStartLimit.length);

        byte[] chunkStepCheckpointPolicy = intToBytes(0);
        identificationSection.write(chunkStepCheckpointPolicy, 0, chunkStepCheckpointPolicy.length);

        byte[] chunkStepItemCount = intToBytes(0);
        identificationSection.write(chunkStepItemCount, 0, chunkStepItemCount.length);

        byte[] chunkStepTimeLimit = intToBytes(0);
        identificationSection.write(chunkStepTimeLimit, 0, chunkStepTimeLimit.length);

        byte[] chunkStepSkipLimit = intToBytes(0);
        identificationSection.write(chunkStepSkipLimit, 0, chunkStepSkipLimit.length);

        byte[] chunkStepRetryLimit = intToBytes(0);
        identificationSection.write(chunkStepRetryLimit, 0, chunkStepRetryLimit.length);

        return identificationSection.toByteArray();

    }

    /**
     * Gets the identification section for a decider, filled in properly
     *
     * @param jobExecution the current job execution
     * @param deciderProxy decider object with information regarding start and end time
     *
     * @return a filled in identification section
     */
    public byte[] getIdentificationSection(WSJobExecution jobExecution, Date startTime, Date endTime) {

        byte[] identificationSectionVersion = intToBytes(CURRENT_IDENTIFICATION_SECTION_VERSION);

        ByteArrayOutputStream identificationSection = new ByteArrayOutputStream();
        identificationSection.write(identificationSectionVersion, 0, identificationSectionVersion.length);

        byte[] jobInstanceBytes = longToBytes(jobExecution.getInstanceId());
        identificationSection.write(jobInstanceBytes, 0, jobInstanceBytes.length);

        byte[] jobExecutionBytes = longToBytes(jobExecution.getExecutionId());
        identificationSection.write(jobExecutionBytes, 0, jobExecutionBytes.length);

        byte[] jobExecutionNumberBytes = longToBytes(jobExecution.getExecutionNumberForThisInstance());
        identificationSection.write(jobExecutionNumberBytes, 0, jobExecutionNumberBytes.length);

        byte[] stepExecutionBytes = longToBytes(0L); // No step id for decider end
        identificationSection.write(stepExecutionBytes, 0, stepExecutionBytes.length);

        byte[] partitionNumberBytes = longToBytes(0L); // No partition number for decider end
        identificationSection.write(partitionNumberBytes, 0, partitionNumberBytes.length);

        byte[] jobNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobExecution.getJobName(),
                                                                                COMMON_MAX_STRING_LENGTH);
        identificationSection.write(jobNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        WSJobInstance jobInstance = jobExecution.getJobInstance();

        byte[] appNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobInstance.getAmcName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(appNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] xmlNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobInstance.getJobXMLName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(xmlNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] stepNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks32, COMMON_MAX_STRING_LENGTH); // No step name for decider end
        identificationSection.write(stepNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] splitNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks32, COMMON_MAX_STRING_LENGTH); // no split for decider end
        identificationSection.write(splitNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] flowNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks32, COMMON_MAX_STRING_LENGTH); // no flow for decider end
        identificationSection.write(flowNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        // Liberty Batch SMF Design doc pg 16: For a job, step, and decider end: create time is the time the job execution was created
        byte[] createTimeBytes = longToBytes(jobExecution.getCreateTime().getTime());
        identificationSection.write(createTimeBytes, 0, createTimeBytes.length);

        byte[] startTimeBytes = longToBytes(startTime.getTime());
        identificationSection.write(startTimeBytes, 0, startTimeBytes.length);

        byte[] endTimeBytes = longToBytes(endTime.getTime());
        identificationSection.write(endTimeBytes, 0, endTimeBytes.length);

        byte[] submitterBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobInstance.getSubmitter(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(submitterBytes, 0, COMMON_MAX_STRING_LENGTH);

        Properties jobParameters = jobExecution.getJobParameters();
        String submitterJobName = jobParameters.getProperty(SUBMITTER_JOBNAME_PROPERTY);

        byte[] submitterJobNameBytes;
        if (submitterJobName != null) {
            submitterJobNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(submitterJobName, JES_JOB_IDENTIFIER_LENGTH);
        } else {
            submitterJobNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks8, JES_JOB_IDENTIFIER_LENGTH);
        }
        identificationSection.write(submitterJobNameBytes, 0, JES_JOB_IDENTIFIER_LENGTH);

        String submitterJobId = jobParameters.getProperty(SUBMITTER_JOBID_PROPERTY);

        byte[] submitterJobIdBytes;
        if (submitterJobId != null) {
            submitterJobIdBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(submitterJobId, JES_JOB_IDENTIFIER_LENGTH);
        } else {
            submitterJobIdBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks8, JES_JOB_IDENTIFIER_LENGTH);
        }
        identificationSection.write(submitterJobIdBytes, 0, JES_JOB_IDENTIFIER_LENGTH);

        byte[] nativeSMFData = nativeUtils.getSmfData(); // psatold 4 ttoken 16 thread id 8 and cvtldto 8
        identificationSection.write(nativeSMFData, 0, 20); // TCB@ and TTOKEN together

        /**
         * NEED TO ADD THESE from JSL, some only for step
         * struct flags {
         * jobRestartableAttr:1; // From JSL Job attribute
         * stepAllowStartIfComplete:1; // From JSL Step attribute
         * partitionStep:1; // For Step-End, partitioned step
         * reserved:61; // reserved
         * int stepStartLimit; // From JSL step start limit
         * int chunkStepCheckpointPolicy;// From JSL for chunk:0=Item, 1=Custom
         * int chunkStepItemCount; // From JSL for chunk item count
         * int chunkStepTimeLimit; // From JSL for chunk time limit
         * int chunkStepSkipLimit; // From JSL for chunk skip limit
         * int chunkStepRetryLimit; // From JSL for chunk retry limit
         */

        byte[] flags = new byte[8];

        identificationSection.write(flags, 0, IDENTIFICATION_FLAGS_SIZE);

        byte[] stepStartLimit = intToBytes(0);
        identificationSection.write(stepStartLimit, 0, stepStartLimit.length);

        byte[] chunkStepCheckpointPolicy = intToBytes(0);
        identificationSection.write(chunkStepCheckpointPolicy, 0, chunkStepCheckpointPolicy.length);

        byte[] chunkStepItemCount = intToBytes(0);
        identificationSection.write(chunkStepItemCount, 0, chunkStepItemCount.length);

        byte[] chunkStepTimeLimit = intToBytes(0);
        identificationSection.write(chunkStepTimeLimit, 0, chunkStepTimeLimit.length);

        byte[] chunkStepSkipLimit = intToBytes(0);
        identificationSection.write(chunkStepSkipLimit, 0, chunkStepSkipLimit.length);

        byte[] chunkStepRetryLimit = intToBytes(0);
        identificationSection.write(chunkStepRetryLimit, 0, chunkStepRetryLimit.length);

        return identificationSection.toByteArray();

    }

    /**
     * Gets the identification section for a partition, filled in properly
     *
     * @param runtimePartitionExecution The runtime execution object for this partition
     * @return a filled in identification section
     */
    public byte[] getIdentificationSection(RuntimePartitionExecution runtimePartitionExecution, WSJobExecution jobExecution, Date startTime, Date endTime) {

        byte[] identificationSectionVersion = intToBytes(CURRENT_IDENTIFICATION_SECTION_VERSION);

        ByteArrayOutputStream identificationSection = new ByteArrayOutputStream();
        identificationSection.write(identificationSectionVersion, 0, identificationSectionVersion.length);

        PartitionPlanConfig ppc = runtimePartitionExecution.getPartitionPlanConfig();

        byte[] jobInstanceBytes = longToBytes(runtimePartitionExecution.getTopLevelInstanceId());
        identificationSection.write(jobInstanceBytes, 0, jobInstanceBytes.length);

        byte[] jobExecutionBytes = longToBytes(ppc.getTopLevelExecutionId());
        identificationSection.write(jobExecutionBytes, 0, jobExecutionBytes.length);

        byte[] jobExecutionNumberBytes = longToBytes(jobExecution.getExecutionNumberForThisInstance());
        identificationSection.write(jobExecutionNumberBytes, 0, jobExecutionNumberBytes.length);

        byte[] stepExecutionBytes = longToBytes(ppc.getTopLevelStepExecutionId());
        identificationSection.write(stepExecutionBytes, 0, stepExecutionBytes.length);

        byte[] partitionNumberBytes = longToBytes(ppc.getPartitionNumber());
        identificationSection.write(partitionNumberBytes, 0, partitionNumberBytes.length);

        byte[] jobNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobExecution.getJobName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(jobNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        WSJobInstance jobInstance = jobExecution.getJobInstance();

        byte[] appNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobInstance.getAmcName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(appNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] xmlNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobInstance.getJobXMLName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(xmlNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] stepNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(runtimePartitionExecution.getStepName(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(stepNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] splitNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks32, COMMON_MAX_STRING_LENGTH); // no split for partition end
        identificationSection.write(splitNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        byte[] flowNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks32, COMMON_MAX_STRING_LENGTH); // no flow for partition end
        identificationSection.write(flowNameBytes, 0, COMMON_MAX_STRING_LENGTH);

        // Create time right now is when the partition plan config is created...should we move it to later when the actual runnable work
        // item is created, just before starting it?
        byte[] createTimeBytes = longToBytes(ppc.getCreateTime().getTime());
        identificationSection.write(createTimeBytes, 0, createTimeBytes.length);

        byte[] startTimeBytes = longToBytes(startTime.getTime());
        identificationSection.write(startTimeBytes, 0, startTimeBytes.length);

        byte[] endTimeBytes = longToBytes(endTime.getTime());
        identificationSection.write(endTimeBytes, 0, endTimeBytes.length);

        byte[] submitterBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobInstance.getSubmitter(), COMMON_MAX_STRING_LENGTH);
        identificationSection.write(submitterBytes, 0, COMMON_MAX_STRING_LENGTH);

        Properties jobParameters = jobExecution.getJobParameters();
        String submitterJobName = jobParameters.getProperty(SUBMITTER_JOBNAME_PROPERTY);

        byte[] submitterJobNameBytes;
        if (submitterJobName != null) {
            submitterJobNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(submitterJobName, JES_JOB_IDENTIFIER_LENGTH);
        } else {
            submitterJobNameBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks8, JES_JOB_IDENTIFIER_LENGTH);
        }
        identificationSection.write(submitterJobNameBytes, 0, JES_JOB_IDENTIFIER_LENGTH);

        String submitterJobId = jobParameters.getProperty(SUBMITTER_JOBID_PROPERTY);

        byte[] submitterJobIdBytes;
        if (submitterJobId != null) {
            submitterJobIdBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(submitterJobId, JES_JOB_IDENTIFIER_LENGTH);
        } else {
            submitterJobIdBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(Blanks8, JES_JOB_IDENTIFIER_LENGTH);
        }
        identificationSection.write(submitterJobIdBytes, 0, JES_JOB_IDENTIFIER_LENGTH);

        byte[] nativeSMFData = nativeUtils.getSmfData(); // psatold 4 ttoken 16 thread id 8 and cvtldto 8
        identificationSection.write(nativeSMFData, 0, 20); // TCB@ and TTOKEN together

        /**
         * NEED TO ADD THESE from JSL, some only for step
         * struct flags {
         * jobRestartableAttr:1; // From JSL Job attribute
         * stepAllowStartIfComplete:1; // From JSL Step attribute
         * partitionStep:1; // For Step-End, partitioned step
         * reserved:61; // reserved
         * int stepStartLimit; // From JSL step start limit
         * int chunkStepCheckpointPolicy;// From JSL for chunk:0=Item, 1=Custom
         * int chunkStepItemCount; // From JSL for chunk item count
         * int chunkStepTimeLimit; // From JSL for chunk time limit
         * int chunkStepSkipLimit; // From JSL for chunk skip limit
         * int chunkStepRetryLimit; // From JSL for chunk retry limit
         */

        byte[] flags = new byte[8];

        if (runtimePartitionExecution.getJobNavigator().getRootModelElement().getRestartable() != null) {
            if (runtimePartitionExecution.getJobNavigator().getRootModelElement().getRestartable().equals("true"))
                flags[0] |= 1 << 0;
        }

        identificationSection.write(flags, 0, IDENTIFICATION_FLAGS_SIZE);

        byte[] stepStartLimit = intToBytes(0);
        identificationSection.write(stepStartLimit, 0, stepStartLimit.length);

        byte[] chunkStepCheckpointPolicy = intToBytes(0);
        identificationSection.write(chunkStepCheckpointPolicy, 0, chunkStepCheckpointPolicy.length);

        byte[] chunkStepItemCount = intToBytes(0);
        identificationSection.write(chunkStepItemCount, 0, chunkStepItemCount.length);

        byte[] chunkStepTimeLimit = intToBytes(0);
        identificationSection.write(chunkStepTimeLimit, 0, chunkStepTimeLimit.length);

        byte[] chunkStepSkipLimit = intToBytes(0);
        identificationSection.write(chunkStepSkipLimit, 0, chunkStepSkipLimit.length);

        byte[] chunkStepRetryLimit = intToBytes(0);
        identificationSection.write(chunkStepRetryLimit, 0, chunkStepRetryLimit.length);

        return identificationSection.toByteArray();
    }

    /**
     * Returns a filled in Completion section for a flow
     *
     * @param runtimeSplitFlowExecution the flow execution object
     * @return a filled in completion section
     */
    public byte[] getCompletionSection(RuntimeSplitFlowExecution runtimeSplitFlowExecution) {

        byte[] completionSectionVersion = intToBytes(CURRENT_COMPLETION_SECTION_VERSION);

        ByteArrayOutputStream completionSection = new ByteArrayOutputStream();
        completionSection.write(completionSectionVersion, 0, completionSectionVersion.length);

        byte[] batchStatusBytes = intToBytes(runtimeSplitFlowExecution.getBatchStatus().ordinal());
        completionSection.write(batchStatusBytes, 0, batchStatusBytes.length);

        String exitStatus = runtimeSplitFlowExecution.getExitStatus();
        try {
            completionSection.write(exitStatus.getBytes(ASCII), 0, exitStatus.length());
            if (exitStatus.length() < EXIT_STATUS_MAX_LENGTH) {
                completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH - exitStatus.length());
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
            completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH);
        }

        /**
         * struct flags {
         * jobStopped:1; // stop was issued during execution of this
         * reserved:63; // reserved
         */
        // No idea how to find this
        byte[] flagsBytes = longToBytes(0L);
        completionSection.write(flagsBytes, 0, flagsBytes.length);

        byte[] partitionPlanBytes = intToBytes(0); // Doesn't apply to the flow
        completionSection.write(partitionPlanBytes, 0, partitionPlanBytes.length);

        byte[] partitionCountBytes = intToBytes(0); // Doesn't apply to the flow
        completionSection.write(partitionCountBytes, 0, partitionCountBytes.length);

        // No metrics... assuming there are 8 of them....
        for (int i = 0; i < 8; ++i) {
            byte[] valueBytes = longToBytes(0L);
            completionSection.write(valueBytes, 0, valueBytes.length);
        }

        return completionSection.toByteArray();
    }

    /**
     * Returns a filled in Completion Section
     *
     * @param jobExecution the job execution object
     * @return a filled in completion section
     */
    public byte[] getCompletionSection(String exitStatus, RuntimeWorkUnitExecution execution) {
        byte[] completionSectionVersion = intToBytes(CURRENT_COMPLETION_SECTION_VERSION);

        ByteArrayOutputStream completionSection = new ByteArrayOutputStream();
        completionSection.write(completionSectionVersion, 0, completionSectionVersion.length);

        byte[] batchStatusBytes = intToBytes(execution.getBatchStatus().ordinal());
        completionSection.write(batchStatusBytes, 0, batchStatusBytes.length);

        try {
            completionSection.write(exitStatus.getBytes(ASCII), 0, exitStatus.length());
            if (exitStatus.length() < EXIT_STATUS_MAX_LENGTH) {
                completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH - exitStatus.length());
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
            completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH);
        }

        //if (exitStatus.length() < EXIT_STATUS_MAX_LENGTH) {
        //    completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH - exitStatus.length());
        //}

        /**
         * struct flags {
         * jobStopped:1; // stop was issued during execution of this
         * reserved:63; // reserved
         */
        // No idea how to find this
        byte[] flagsBytes = longToBytes(0L);
        completionSection.write(flagsBytes, 0, flagsBytes.length);

        byte[] partitionPlanBytes = intToBytes(0); // Doesn't apply to the decision
        completionSection.write(partitionPlanBytes, 0, partitionPlanBytes.length);

        byte[] partitionCountBytes = intToBytes(0); // Doesn't apply to the decision
        completionSection.write(partitionCountBytes, 0, partitionCountBytes.length);

        // No metrics... assuming there are 8 of them....
        for (int i = 0; i < 8; ++i) {
            byte[] valueBytes = longToBytes(0L);
            completionSection.write(valueBytes, 0, valueBytes.length);
        }

        return completionSection.toByteArray();
    }

    /**
     * Returns a filled in Completion Section
     *
     * @param jobExecution the job execution object
     * @return a filled in completion section
     */
    public byte[] getCompletionSection(WSJobExecution jobExecution) {
        byte[] completionSectionVersion = intToBytes(CURRENT_COMPLETION_SECTION_VERSION);

        ByteArrayOutputStream completionSection = new ByteArrayOutputStream();
        completionSection.write(completionSectionVersion, 0, completionSectionVersion.length);

        byte[] batchStatusBytes = intToBytes(jobExecution.getBatchStatus().ordinal());
        completionSection.write(batchStatusBytes, 0, batchStatusBytes.length);

        String exitStatus = jobExecution.getExitStatus();
        try {
            completionSection.write(exitStatus.getBytes(ASCII), 0, exitStatus.length());
            if (exitStatus.length() < EXIT_STATUS_MAX_LENGTH) {
                completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH - exitStatus.length());
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
            completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH);
        }

        //if (exitStatus.length() < EXIT_STATUS_MAX_LENGTH) {
        //    completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH - exitStatus.length());
        //}

        /**
         * struct flags {
         * jobStopped:1; // stop was issued during execution of this
         * reserved:63; // reserved
         */
        // No idea how to find this
        byte[] flagsBytes = longToBytes(0L);
        completionSection.write(flagsBytes, 0, flagsBytes.length);

        byte[] partitionPlanBytes = intToBytes(0); // Doesn't apply to the job
        completionSection.write(partitionPlanBytes, 0, partitionPlanBytes.length);

        byte[] partitionCountBytes = intToBytes(0); // Doesn't apply to the job
        completionSection.write(partitionCountBytes, 0, partitionCountBytes.length);

        // No metrics... assuming there are 8 of them....
        for (int i = 0; i < 8; ++i) {
            byte[] valueBytes = longToBytes(0L);
            completionSection.write(valueBytes, 0, valueBytes.length);
        }

        return completionSection.toByteArray();
    }

    /**
     * Returns a filled in Completion Section for a step
     *
     * @param stepExecution the step execution object
     * @return a filled in completion section
     */
    public byte[] getCompletionSection(WSTopLevelStepExecution stepExecution, int partitionPlanCount, int partitionCount) {
        byte[] completionSectionVersion = intToBytes(CURRENT_COMPLETION_SECTION_VERSION);

        ByteArrayOutputStream completionSection = new ByteArrayOutputStream();
        completionSection.write(completionSectionVersion, 0, completionSectionVersion.length);

        byte[] batchStatusBytes = intToBytes(stepExecution.getBatchStatus().ordinal());
        completionSection.write(batchStatusBytes, 0, batchStatusBytes.length);

        String exitStatus = stepExecution.getExitStatus();
        try {
            completionSection.write(exitStatus.getBytes(ASCII), 0, exitStatus.length());
            if (exitStatus.length() < EXIT_STATUS_MAX_LENGTH) {
                completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH - exitStatus.length());
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
            completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH);
        }

        //if (exitStatus.length() < EXIT_STATUS_MAX_LENGTH) {
        //    completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH - exitStatus.length());
        //}

        /**
         * struct flags {
         * jobStopped:1; // stop was issued during execution of this
         * reserved:63; // reserved
         */
// No idea how to find this - would we know for a step-end?
        byte[] flagsBytes = longToBytes(0L);
        completionSection.write(flagsBytes, 0, flagsBytes.length);

        byte[] partitionPlanBytes = intToBytes(partitionPlanCount);
        completionSection.write(partitionPlanBytes, 0, partitionPlanBytes.length);

        byte[] partitionCountBytes = intToBytes(partitionCount);
        completionSection.write(partitionCountBytes, 0, partitionCountBytes.length);

        // Assuming these are in the same order as in the SMF mapping
        Metric[] metrics = stepExecution.getMetrics();
        for (Metric metric : metrics) {
            long value = metric.getValue();
            byte[] valueBytes = longToBytes(value);
            completionSection.write(valueBytes, 0, valueBytes.length);
        }

        return completionSection.toByteArray();
    }

    /**
     * Returns a filled in Completion Section for a step
     *
     * @param stepExecution the step execution object
     * @return a filled in completion section
     */
    public byte[] getCompletionSection(WSTopLevelStepExecution stepExecution) {
        byte[] completionSectionVersion = intToBytes(CURRENT_COMPLETION_SECTION_VERSION);

        ByteArrayOutputStream completionSection = new ByteArrayOutputStream();
        completionSection.write(completionSectionVersion, 0, completionSectionVersion.length);

        byte[] batchStatusBytes = intToBytes(stepExecution.getBatchStatus().ordinal());
        completionSection.write(batchStatusBytes, 0, batchStatusBytes.length);

        String exitStatus = stepExecution.getExitStatus();
        try {
            completionSection.write(exitStatus.getBytes(ASCII), 0, exitStatus.length());
            if (exitStatus.length() < EXIT_STATUS_MAX_LENGTH) {
                completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH - exitStatus.length());
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
            completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH);
        }

        //if (exitStatus.length() < EXIT_STATUS_MAX_LENGTH) {
        //    completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH - exitStatus.length());
        //}

        /**
         * struct flags {
         * jobStopped:1; // stop was issued during execution of this
         * reserved:63; // reserved
         */
// No idea how to find this - would we know for a step-end?
        byte[] flagsBytes = longToBytes(0L);
        completionSection.write(flagsBytes, 0, flagsBytes.length);

//No idea how to find this
        byte[] partitionPlanBytes = intToBytes(0);
        completionSection.write(partitionPlanBytes, 0, partitionPlanBytes.length);

//No idea how to find this
        byte[] partitionCountBytes = intToBytes(0);
        completionSection.write(partitionCountBytes, 0, partitionCountBytes.length);

        // Assuming these are in the same order as in the SMF mapping
        Metric[] metrics = stepExecution.getMetrics();
        for (Metric metric : metrics) {
            long value = metric.getValue();
            byte[] valueBytes = longToBytes(value);
            completionSection.write(valueBytes, 0, valueBytes.length);
        }

        return completionSection.toByteArray();
    }

    /**
     * Returns a filled in Completion Section for a partition
     *
     * @param runtimePartitionExecution The execution for this partition
     * @return a filled in completion section
     */
    public byte[] getCompletionSection(RuntimeStepExecution runtimeStepExecution) {

        byte[] completionSectionVersion = intToBytes(CURRENT_COMPLETION_SECTION_VERSION);
        ByteArrayOutputStream completionSection = new ByteArrayOutputStream();
        completionSection.write(completionSectionVersion, 0, completionSectionVersion.length);
        byte[] batchStatusBytes = intToBytes(runtimeStepExecution.getBatchStatus().ordinal());
        completionSection.write(batchStatusBytes, 0, batchStatusBytes.length);

        String exitStatus = runtimeStepExecution.getExitStatus();
        if (exitStatus != null) {
            try {
                completionSection.write(exitStatus.getBytes(ASCII), 0, exitStatus.length());
                if (exitStatus.length() < EXIT_STATUS_MAX_LENGTH) {
                    completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH - exitStatus.length());
                }
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
                completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH);
            }
        } else {
            completionSection.write(nulls, 0, EXIT_STATUS_MAX_LENGTH);
        }

        /**
         * struct flags {
         * jobStopped:1; // stop was issued during execution of this
         * reserved:63; // reserved
         */
        // No idea how to find this
        byte[] flagsBytes = longToBytes(0L);
        completionSection.write(flagsBytes, 0, flagsBytes.length);

        byte[] partitionPlanBytes = intToBytes(0); // Doesn't apply to the partition
        completionSection.write(partitionPlanBytes, 0, partitionPlanBytes.length);

        byte[] partitionCountBytes = intToBytes(0); // Doesn't apply to the partition
        completionSection.write(partitionCountBytes, 0, partitionCountBytes.length);

        // No metrics... assuming there are 8 of them....
        for (int i = 0; i < 8; ++i) {
            byte[] valueBytes = longToBytes(0L);
            completionSection.write(valueBytes, 0, valueBytes.length);
        }

        return completionSection.toByteArray();
    }

    public byte[] getEmptyCompletionSection() {
        byte[] completionSectionVersion = intToBytes(CURRENT_COMPLETION_SECTION_VERSION);

        ByteArrayOutputStream completionSection = new ByteArrayOutputStream();
        completionSection.write(completionSectionVersion, 0, completionSectionVersion.length);

        return completionSection.toByteArray();
    }

    public byte[] getEmptyProcessorSection() {
        byte[] processorSectionVersion = intToBytes(CURRENT_PROCESSOR_SECTION_VERSION);
        ByteArrayOutputStream processorSection = new ByteArrayOutputStream();
        processorSection.write(processorSectionVersion, 0, processorSectionVersion.length);

        return processorSection.toByteArray();
    }

    /**
     * Creates the processor section
     *
     * @param timeUsedBefore an array of timeused values captured at the start of processing
     * @param timeUsedAfter  an array of timeused values captured at the end of processing
     * @return a filled in processor section
     */
    public byte[] getProcessorSection(byte[] timeUsedBefore, byte[] timeUsedAfter) {
        byte[] processorSectionVersion = intToBytes(CURRENT_PROCESSOR_SECTION_VERSION);

        if (timeUsedBefore == null) {
            //this is being mocked up still get rid of this eventually
            timeUsedBefore = nativeUtils.getTimeusedData();
        }

        if (timeUsedAfter == null) {
            //this is being mocked up still get rid of this eventually
            timeUsedAfter = nativeUtils.getTimeusedData();
        }

        ByteArrayOutputStream processorSection = new ByteArrayOutputStream();
        processorSection.write(processorSectionVersion, 0, processorSectionVersion.length);

        /**
         * Calls should use this API before and after the job/step etc
         * byte[] timeuseddata = nativeUtils.getTimeusedData();
         * Data is mapped in z/OS native include/server_utils.h
         */

        processorSection.write(timeUsedBefore, 0, 8); // total CPU
        processorSection.write(timeUsedAfter, 0, 8);
        processorSection.write(timeUsedBefore, 8, 8); // time on CP
        processorSection.write(timeUsedAfter, 8, 8);
        processorSection.write(timeUsedBefore, 16, 8); // offload time
        processorSection.write(timeUsedAfter, 16, 8);
        processorSection.write(timeUsedBefore, 24, 8); // offload on CP
        processorSection.write(timeUsedAfter, 24, 8);

        return processorSection.toByteArray();
    }

    /**
     * Creates the appropriate accounting sections
     *
     * @param jobParameters job parameters
     * @return a linked list of byte arrays, each one of which is a complete accounting section
     */
    private LinkedList<byte[]> getAccountingSections(Properties jobParameters) {

        String accountingString = null;
        if (jobParameters != null) {
            accountingString = jobParameters.getProperty(ACCOUNTING_STRING_PROPERTY);
        }

        LinkedList<byte[]> ll = null;

        if (accountingString != null) {
            ll = new LinkedList<byte[]>();

            // pull the string apart
            String[] parts = accountingString.split(",");

            for (int i = 0; i < parts.length; ++i) {
                //  for each part create a byte array
                byte[] accountingSectionVersion = intToBytes(CURRENT_ACCOUNTING_SECTION_VERSION);

                ByteArrayOutputStream accountingSection = new ByteArrayOutputStream();
                accountingSection.write(accountingSectionVersion, 0, accountingSectionVersion.length);

                byte[] acctStringLenBytes = intToBytes(parts[i].length());
                accountingSection.write(acctStringLenBytes, 0, acctStringLenBytes.length);

                byte[] acctStringBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(parts[i], ACCOUNTING_MAX_LENGTH);
                accountingSection.write(acctStringBytes, 0, ACCOUNTING_MAX_LENGTH);

                // add the section to the list
                ll.add(accountingSection.toByteArray());

            }
        }

        // ll.size gives the count
        return ll;
    }

    /**
     * Creates the USS section of the record
     *
     * @return a filled in USS section
     */
    public byte[] getUssSection(String submitter) {
        byte[] ussSectionVersion = intToBytes(CURRENT_USS_SECTION_VERSION);

        ByteArrayOutputStream ussSection = new ByteArrayOutputStream();
        ussSection.write(ussSectionVersion, 0, ussSectionVersion.length);

        byte[] pidBytes = intToBytes(nativeUtils.getPid());
        ussSection.write(pidBytes, 0, 4);

        byte[] nativeSMFData = nativeUtils.getSmfData(); // psatold 4 ttoken 16 thread id 8 and cvtldto 8
        ussSection.write(nativeSMFData, 20, 8); // grab thread ID

        byte[] threadIdBytes = longToBytes(Thread.currentThread().getId());
        ussSection.write(threadIdBytes, 0, threadIdBytes.length);

        String userInfo = null;
        String uidString = null;
        String gidString = null;

        try {
            userInfo = getUserInfo(submitter);

            /*
             * parse out the uid and gid, output of id command is in format of:
             * uid=1951(MSTONE1) gid=100(WASUSER) groups=2500(WSCFG1),999(WWWGROUP)
             *
             * (Could also call id twice with specific flags to get just the uid and gid)
             */
            if (userInfo != null) {
                UserInfoMatcher matcher = new UserInfoMatcher(userInfo);
                uidString = matcher.uid;
                gidString = matcher.gid;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "found uid: " + uidString + ", found gid: " + gidString);
                }
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "userInfo = null, so not looking for uid,guid");
                }
            }
        } catch (IOException e) {
            // Any logging needed here?
        }

        byte[] uidBytes;
        if (uidString != null && (!uidString.equals("")))
            uidBytes = intToBytes(Integer.parseInt(uidString));
        else
            uidBytes = intToBytes(MAX_NEGATIVE);

        ussSection.write(uidBytes, 0, 4);

        byte[] gidBytes;
        if (gidString != null && (!gidString.equals("")))
            gidBytes = intToBytes(Integer.parseInt(gidString));
        else
            gidBytes = intToBytes(MAX_NEGATIVE);

        ussSection.write(gidBytes, 0, 4);

        return ussSection.toByteArray();
    }

    protected static class UserInfoMatcher {

        // Cache these in static fields
        static final String UID_PATTERN = "uid=([0-9]*)";
        static final String GID_PATTERN = "gid=([0-9]*)";
        static final Pattern uidPattern = Pattern.compile(UID_PATTERN);
        static final Pattern gidPattern = Pattern.compile(GID_PATTERN);

        protected String uid = null, gid = null;

        protected UserInfoMatcher(String userInfo) {
            Matcher uidMatcher = uidPattern.matcher(userInfo);
            Matcher gidMatcher = gidPattern.matcher(userInfo);

            if (uidMatcher.find()) {
                uid = uidMatcher.group(1);
            }

            if (gidMatcher.find()) {
                gid = gidMatcher.group(1);
            }
        }
    }

    /**
     * Creates the USS section of the record
     *
     * @return a filled in USS section
     */
    public byte[] getUssSection() {
        byte[] ussSectionVersion = intToBytes(CURRENT_USS_SECTION_VERSION);

        ByteArrayOutputStream ussSection = new ByteArrayOutputStream();
        ussSection.write(ussSectionVersion, 0, ussSectionVersion.length);

        byte[] pidBytes = intToBytes(nativeUtils.getPid());
        ussSection.write(pidBytes, 0, 4);

        byte[] nativeSMFData = nativeUtils.getSmfData(); // psatold 4 ttoken 16 thread id 8 and cvtldto 8
        ussSection.write(nativeSMFData, 20, 8); // grab thread ID

        byte[] threadIdBytes = longToBytes(Thread.currentThread().getId());
        ussSection.write(threadIdBytes, 0, threadIdBytes.length);

        /**
         * int submitterUID; // USS UID of submitter
         * int submitterGID; // USS GID of submitter
         * }
         */

        return ussSection.toByteArray();
    }

    /**
     * Get reference for Decider
     *
     * @return
     */
    private LinkedList<byte[]> getReferenceNamesSections(String decisionRefName) {
        LinkedList<byte[]> ll = new LinkedList<byte[]>();
        ll.add(createReferenceNameSection(DECIDER_REF_TYPE, decisionRefName));
        return ll;
    }

    /**
     * Get any and all references for a step
     *
     * @return
     */
    private LinkedList<byte[]> getReferenceNamesSections(Step step) {

        /** Might be more than one..remember to put the version at the top of each */
        /** Might not be any also in which case return null */

        /*
         * byte[] referenceNamesSectionVersion = intToBytes(CURRENT_REFERENCE_NAMES_SECTION_VERSION);
         *
         * ByteArrayOutputStream referenceNamesSection = new ByteArrayOutputStream();
         * referenceNamesSection.write(referenceNamesSectionVersion, 0, referenceNamesSectionVersion.length);
         */

        Batchlet batchlet = step.getBatchlet();
        Chunk chunk = step.getChunk();
        LinkedList<byte[]> ll = new LinkedList<byte[]>();

        if (batchlet != null) {
            ll.add(createReferenceNameSection(BATCHLET_REF_TYPE, batchlet.getRef()));
        } else if (chunk != null) {

            String readerRef = (chunk.getReader() != null) ? chunk.getReader().getRef() : null;
            if (readerRef != null) {
                ll.add(createReferenceNameSection(READER_REF_TYPE, readerRef));
            }

            String writerRef = (chunk.getWriter() != null) ? chunk.getWriter().getRef() : null;
            if (writerRef != null) {
                ll.add(createReferenceNameSection(WRITER_REF_TYPE, writerRef));
            }

            String processorRef = (chunk.getProcessor() != null) ? chunk.getProcessor().getRef() : null;
            if (processorRef != null) {
                ll.add(createReferenceNameSection(PROCESSOR_REF_TYPE, processorRef));
            }

            String chkpointAlgRef = (chunk.getCheckpointAlgorithm() != null) ? chunk.getCheckpointAlgorithm().getRef() : null;
            if (chkpointAlgRef != null) {
                ll.add(createReferenceNameSection(CHECKPOINT_REF_TYPE, chkpointAlgRef));
            }
        }

        Partition partition = step.getPartition();

        if (partition != null) {

            String analyzerRef = (partition.getAnalyzer() != null) ? partition.getAnalyzer().getRef() : null;
            if (analyzerRef != null) {
                ll.add(createReferenceNameSection(PARTITION_ANALYZER_REF_TYPE, analyzerRef));
            }

            String mapperRef = (partition.getMapper() != null) ? partition.getMapper().getRef() : null;
            if (mapperRef != null) {
                ll.add(createReferenceNameSection(PARTITION_MAPPER_REF_TYPE, mapperRef));
            }

            String collectorRef = (partition.getCollector() != null) ? partition.getCollector().getRef() : null;
            if (collectorRef != null) {
                ll.add(createReferenceNameSection(PARTITION_COLLECTOR_REF_TYPE, collectorRef));
            }

            String reducerRef = (partition.getReducer() != null) ? partition.getReducer().getRef() : null;
            if (reducerRef != null) {
                ll.add(createReferenceNameSection(PARTITION_REDUCER_REF_TYPE, reducerRef));
            }
        }

        // ll.size gives the count
        return ll;
    }

    /**
     * Internal method to build a reference name section, as we may need to build multiples
     *
     * @param refType Type of reference being written
     * @param ref     The actual reference to write
     *
     * @return the built reference name record to add to the list
     */
    private byte[] createReferenceNameSection(int refType, String ref) {
        byte[] referenceNamesSectionVersion = intToBytes(CURRENT_REFERENCE_NAMES_SECTION_VERSION);

        ByteArrayOutputStream referenceNamesSection = new ByteArrayOutputStream();
        referenceNamesSection.write(referenceNamesSectionVersion, 0, referenceNamesSectionVersion.length);

        byte[] refTypeBytes = intToBytes(refType);
        referenceNamesSection.write(refTypeBytes, 0, refTypeBytes.length);

        byte[] refLengthBytes = intToBytes(ref.length());
        referenceNamesSection.write(refLengthBytes, 0, refLengthBytes.length);

        try {
            // If longer than max length, trim from left
            if (ref.length() > REFERENCE_SECTION_BUFFER_MAX_LENGTH)
                ref = ref.substring(ref.length() - REFERENCE_SECTION_BUFFER_MAX_LENGTH);

            referenceNamesSection.write(ref.getBytes(ASCII), 0, ref.length());

            // pad the rest with nulls if needed
            if (ref.length() < REFERENCE_SECTION_BUFFER_MAX_LENGTH) {
                referenceNamesSection.write(nulls, 0, REFERENCE_SECTION_BUFFER_MAX_LENGTH - ref.length());
            }
        } catch (UnsupportedEncodingException e) {
            referenceNamesSection.write(nulls, 0, REFERENCE_SECTION_BUFFER_MAX_LENGTH);
        }

        return referenceNamesSection.toByteArray();
    }

    /**
     * Little routine to make an long into a byte array
     *
     * @param L an long
     * @return the long as a byte array
     */
    private static byte[] longToBytes(long l) {
        return new byte[] { (byte) (l >> 56),
                            (byte) (l >> 48),
                            (byte) (l >> 40),
                            (byte) (l >> 32),
                            (byte) (l >> 24),
                            (byte) (l >> 16),
                            (byte) (l >> 8),
                            (byte) (l) };
    }

    /**
     * Little routine to make an int into a byte array
     *
     * @param Int an int
     * @return the int as a byte array
     */
    private static byte[] intToBytes(int Int) {
        return new byte[] { (byte) (Int >> 24), (byte) (Int >> 16), (byte) (Int >> 8), (byte) (Int) };
    }

    /**
     * Little routine to make an short into a byte array
     *
     * @param s a short
     * @return the short as a byte array
     */
    private static byte[] shortToBytes(short s) {
        return new byte[] { (byte) (s >> 8), (byte) (s) };
    }

    /**
     * Create the common data for the server...probably needs more
     *
     */
    private byte[] getSubsystemData(int type, String repositoryType, String jobStoreRef) {

        byte[] subsystemDataVersion = intToBytes(CURRENT_SUBSYSTEM_DATA_VERSION);

        ByteArrayOutputStream subsystemData = new ByteArrayOutputStream();
        subsystemData.write(subsystemDataVersion, 0, subsystemDataVersion.length);
        subsystemData.write(intToBytes(type), 0, 4); /* record type */
        subsystemData.write(mvsCommonFields.getCVTSNAME(), 0, 8);
        subsystemData.write(mvsCommonFields.getECVTSPLX(), 0, 8);

        byte[] nativeSMFData = nativeUtils.getSmfData(); // psatold 4 ttoken 16 thread id 8 and cvtldto 8
        subsystemData.write(nativeSMFData, 28, 8); // grab CVTLDTO

        TimeZone tz = TimeZone.getDefault();
        String tzDisp = tz.getDisplayName();
        byte[] tzBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(tzDisp, COMMON_MAX_STRING_LENGTH);
        subsystemData.write(tzBytes, 0, COMMON_MAX_STRING_LENGTH);

        subsystemData.write(mvsCommonFields.getJSABJBID(), 0, 8);
        subsystemData.write(mvsCommonFields.getJSABJBNM(), 0, 8);
        subsystemData.write(mvsCommonFields.getASSBSTKN(), 0, 8);

        int asid = mvsCommonFields.getASCBASID();
        byte[] asidBytes = intToBytes(asid);
        subsystemData.write(asidBytes, 0, 4);

        // Add server config dir. It has the server name.
        String serverConfigDir = getServerConfigDir();
        byte[] serverConfigDirBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(serverConfigDir, SERVER_CONFIG_DIR_LENGTH_MAX);
        subsystemData.write(serverConfigDirBytes, 0, SERVER_CONFIG_DIR_LENGTH_MAX);

        String productVersion = getProductVersion();
        byte[] productVersionBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(productVersion, PRODUCT_VERSION_BYTES_MAX);
        subsystemData.write(productVersionBytes, 0, PRODUCT_VERSION_BYTES_MAX);

        int physicalCpuAdjustment = mvsCommonFields.getRCTPCPUA();
        byte[] physicalCpuAdjustmentBytes = intToBytes(physicalCpuAdjustment);
        subsystemData.write(physicalCpuAdjustmentBytes, 0, 4);

        int cpuRateAdjustment = mvsCommonFields.getRMCTADJC();
        byte[] cpuRateAdjustmentBytes = intToBytes(cpuRateAdjustment);
        subsystemData.write(cpuRateAdjustmentBytes, 0, 4);

        byte[] repositoryTypeBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(repositoryType, REPOSITORY_TYPE_LENGTH);
        subsystemData.write(repositoryTypeBytes, 0, REPOSITORY_TYPE_LENGTH);

        byte[] jobStoreRefBytes = nativeUtils.convertAsciiStringToFixedLengthEBCDIC(jobStoreRef, JOB_STORE_REF_LENGTH_MAX);
        subsystemData.write(jobStoreRefBytes, 0, JOB_STORE_REF_LENGTH_MAX);

        // start of fields added for server data version 3
        int cvtFlags = mvsCommonFields.getCVTFLAGS();
        // if CvtzCBP is on set flag
        if ((cvtFlags & 0x00000200) == 0x00000200) {
            int serverDataFlags = 0x80000000;
            byte[] serverDataFlagsBytes = intToBytes(serverDataFlags);
            subsystemData.write(serverDataFlagsBytes, 0, 4);
        } else {
            subsystemData.write(nulls, 0, 4);
        }

        /** bunch more stuff goes here */
        /**
         * int physicalCpuAdjustment; // From RCTPCPUA -- done
         * int cpuRateAdjustment; // From RMCTADJC -- done
         * char repositoryType[4], // “JPA “ or “MEM “ -- done
         * char jobStoreRef[16], // batchPersistence job store reference -- done
         * char dbSchema[128], // databaseStore schema
         * char dbTablePrefix[128]; // databaseStore tablePrefix
         */

        // IF YOU ADD STUFF HERE, bump the version CURRENT_SERVER_DATA_VERSION!!!

        return subsystemData.toByteArray();
    }

    private String getServerConfigDir() {
        String fullServerConfigDir = locationAdmin.resolveString("${" + WsLocationConstants.LOC_SERVER_CONFIG_DIR + "}");
        String serverConfigDir;
        if (fullServerConfigDir.length() > SERVER_CONFIG_DIR_LENGTH_MAX) {
            serverConfigDir = fullServerConfigDir.substring(fullServerConfigDir.length() - SERVER_CONFIG_DIR_LENGTH_MAX);
        } else {
            serverConfigDir = fullServerConfigDir;
        }
        return serverConfigDir;
    }

    private static String getProductVersion() {
        String version = "";

        try {
            Map<String, ProductInfo> productProperties = ProductInfo.getAllProductInfo();
            ProductInfo wasProperties = productProperties.get(PRODUCT_NAME);
            if (wasProperties != null) {
                version = wasProperties.getVersion();

            }
        } catch (ProductInfoParseException e1) {
            // we tried. just go with empty string
        } catch (DuplicateProductInfoException e1) {
            //we tried. just go with empty string
        } catch (ProductInfoReplaceException e1) {
            // we tried. just go with empty string
        }
        return version;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] getTimeUsedData() {
        // TODO Auto-generated method stub
        return nativeUtils.getTimeusedData();
    }

    private void setLastSMFRC(int smfrc) {
        lastSMFrc = smfrc;
    }

    public int getLastSMFRC() {
        return lastSMFrc;
    }

    /**
     * Get the uid and gid for the specified user. Makes one call to the
     * USS "id" command.
     *
     * @param submitter The submitter user name to use with the "id" command
     * @return the output from the "id" command
     *
     * @throws IOException
     */
    private String getUserInfo(String submitter) throws IOException {
        List<String> args = new ArrayList<String>();
        args.add("/bin/id");
        args.add(submitter);

        // Create the process
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process pidProcess = processBuilder.redirectErrorStream(true).start();

        // Wrap the input stream with a reader
        InputStream pidInputStream = pidProcess.getInputStream();
        InputStreamReader pidInputStreamReader = new InputStreamReader(pidInputStream, "IBM-1047");
        BufferedReader pidReader = new BufferedReader(pidInputStreamReader);

        // The uid/gid info should be the only line of output
        String info = pidReader.readLine();

        // Eat all output after the first line
        while (pidReader.readLine() != null);
        pidReader.close();

        // Get the return code from the process
        int returnCode = -1;
        try {
            returnCode = pidProcess.waitFor();
        } catch (InterruptedException ie) {
            throw new IOException(ie);
        } finally {
            pidProcess.getOutputStream().close();
            pidProcess.getErrorStream().close();
        }

        if (returnCode != 0) {
            info = null;
        }

        return info;
    }

}
