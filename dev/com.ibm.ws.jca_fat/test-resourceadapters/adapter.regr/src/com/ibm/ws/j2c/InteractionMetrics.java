/*******************************************************************************
 * Copyright (c) 1997 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.j2c;

/**
 * Interface name : InteractionMetrics
 * <p>
 * This InteractionMetrics interface introduces the capability for any
 * ResourceAdapter to participate in reporting its use time in a request and
 * have that time reported by the various Request Metrics reporting tools
 * available for WebShpere.
 * <p>
 * The WebSphere ConnectionEventListener will implement this class. We keep
 * an EventListener associated with each ManagedConnection. By tracking the
 * interaction time on a per ManagedConnection basis we can gather use time
 * statistics for each ManagedConnection which will be useful data for solving
 * performance related problems.
 * <p>
 * ResourceAdapters wishing to participate in various WebSphere RequestMetric
 * tools, diagnostic tools, etc. will need to use this interface, which will be
 * implemented on the ConnectionEventListner we register with every ResourceAdapter
 * ManagedConnection we create, to report the usage time associated with
 * the subset of calls as defined below in the table below.
 * <p>
 * The call flow in the resource adapter is the following:
 * <ol>
 * <li>At the beginning of each method to report statics for, call isInteractionMetricsEnabled.
 * <li>If it returns false, do nothing for the rest of this request.
 * <li>If it returns true, call preInteraction.
 * <li>Before sending the request to the downstream EIS process, call
 * getCorrelator and attach the correlator with the request
 * so that the downstream EIS process can get the correlator.
 * <li>Do the actual work.
 * <li>At the end of the execute method, call getTranDetailLevel. Based on the level,
 * collect transaction detail information as specified in the table listed in the
 * postInteraction method, and then call postInteraction.
 * </ol>
 * <p>
 * Note that the Websphere runtime will track and report the time for the
 * calls which either come into our code through the spi or come out of our
 * code through the spi as identified in the table.
 * <p><p>The following table will detail the minimum expected content for
 * the detailed info for the various levels:
 *
 * <table BORDER=2 COLS=4 WIDTH="100%" >
 * <caption><b>ResourceAdapter Metric Instrumentation</b></caption>
 * <tr>
 * <th><b>Instrumentation Point</font></b><br>
 * <b>Package</b><br>
 * <b>Interface</b><br>
 * <b>Methods</b><br>
 * </pre>
 * </th>
 * <th><b>Probe Action</font><br>Level 1</b></th>
 * <th><b>Probe Action</font><br>Level 2</b></th>
 * <th><b>Probe Action</font><br>Level 3</b></th>
 * </tr>
 * <tr>
 *
 * <tr>
 * <td>
 * <b>These methods must be intrumented by the ResourceAdapter</b><br><br>
 * Package: javax.resource.cci<br>
 * Interface: Interaction<br>
 * Methods: <br>
 * <ul>
 * <li>execute(InteractionSpec, Record)</li>
 * <li>execute(InteractionSpec, Record, Record)</li>
 * </ul>
 * </td>
 * <td>
 * Performance
 * </td>
 * <td>Readable form of the javax.resource.cci.InteractionSpec</td>
 * <td>
 * Readable form of the javax.resource.cci.InteractionSpec<br>
 * AdapterName<br>
 * AdapterShortDescription<br>
 * AdapterVendorName<br>
 * AdapterVersion<br>
 * InteractionSpecsSupported<br>
 * SpecVersion<br>
 * </td>
 * </tr>
 *
 * <tr>
 * <td>
 * <b>These methods must be intrumented by the ResourceAdapter</b><br><br>
 * Package: javax.resource.cci<br>
 * Interface: LocalTransaction<br><br>
 * Methods: <br>
 * <ul>
 * <li>begin</li>
 * <li>commit</li>
 * <li>rollback</li>
 * </ul>
 * </td>
 * <td>Performance</td>
 * <td>Performance</td>
 * <td>
 * AdapterName<br>
 * AdapterShortDescription<br>
 * AdapterVendorName<br>
 * AdapterVersion<br>
 * InteractionSpecsSupported<br>
 * SpecVersion<br>
 * </td>
 * </tr>
 *
 * <td>
 * <b>See Note 1</b><br><br>
 * Package: javax.resource.spi<br>
 * Interface: ManagedConnectionFactory<br>
 * Methods: <br>
 * <ul>
 * <li>createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo)</li>
 * <li>matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo)</li>
 * <li>createConnectionFactory()</li>
 * </ul>
 * </td>
 * <td>
 * Performance
 * </td>
 * <td>
 * Performance
 * </td>
 * <td>
 * AdapterName<br>
 * AdapterShortDescription<br>
 * AdapterVendorName<br>
 * AdapterVersion<br>
 * InteractionSpecsSupported<br>
 * SpecVersion<br>
 * </td>
 * </tr>
 *
 * <tr>
 * <td>
 * <b>See Note 1</b><br><br>
 * Package: javax.resource.spi<br>
 * Interface: ManagedConnection<br>
 * Methods: <br>
 * <ul>
 * <li>getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo)</li>
 * </ul>
 * </td>
 * <td>
 * Performance
 * </td>
 * <td>
 * EISProductName<br>
 * EISProductVersion<br>
 * UserName<br>
 * </td>
 * <td>
 * ConnectionFactory JNDI Name
 * EISProductName<br>
 * EISProductVersion<br>
 * UserName<br>
 * AdapterName<br>
 * AdapterShortDescription<br>
 * AdapterVendorName<br>
 * AdapterVersion<br>
 * InteractionSpecsSupported<br>
 * SpecVersion<br>
 * </td>
 * </tr>
 *
 * <tr>
 * <td>
 * <b>See Note 1</b><br><br>
 * Package: javax.resource.spi<br>
 * Interface: ManagedConnection<br>
 * Methods: <br>
 * <ul>
 * <li>cleanup()</li>
 * <li>destroy()</li>
 * </ul>
 * </td>
 * <td>
 * Performance
 * </td>
 * <td>
 * Performance
 * </td>
 * <td>
 * AdapterName<br>
 * AdapterShortDescription<br>
 * AdapterVendorName<br>
 * AdapterVersion<br>
 * InteractionSpecsSupported<br>
 * SpecVersion<br>
 * </td>
 * </tr>
 *
 * <tr>
 * <td>
 * <b>See Note 1</b><br><br>
 * Package: javax.resource.spi<br>
 * Interface: LocalTransaction<br>
 * Methods:
 * <ul>
 * <li>begin</li>
 * <li>commit</li>
 * <li>rollback</li>
 * </ul>
 * Interface: XAResource<br>
 * Methods:
 * <ul>
 * <li>commit(Xid xid, boolean onePhase)</li>
 * <li>end(Xid xid, int flags) </li>
 * <li>forget(Xid xid)</li>
 * <li>prepare(Xid xid</li>
 * <li>rollback(Xid xid)</li>
 * <li>start(Xid xid, int flags)</li>
 * </ul>
 * </td>
 * <td>Performance</td>
 * <td>Performance</td>
 * <td>
 * AdapterName<br>
 * AdapterShortDescription<br>
 * AdapterVendorName<br>
 * AdapterVersion<br>
 * InteractionSpecsSupported<br>
 * SpecVersion<br>
 * </td>
 * </tr>
 * </table>
 *
 *
 * <p><b><i>NOTE 1</i>: the JCA Runtime has access to
 * these method calls and information and can call
 * the pre/postInteraction methods surrounding the calls it makes to these methods.
 * It will do that for the RA so the RA should NOT make calls to report usage
 * for these methods.</b><br><br>
 *
 * When providing dynamic infomation for a given segment of an interaction,
 * the following properties/capabilities will be considered standard for
 * all JCA Resource adapters as defined in the JCA 1.5 or later specification:
 * (whenever representing this information you must use these names as spelled).
 * <ol>
 * <li>ServerName
 * <li>PortNumber
 * <li>UserName
 * <li>Password
 * <li>ConnectionURL
 * <li>EISProductName
 * <li>EISProductVersion
 * <li>MaxConnections
 * <li>UserName
 * <li>AdapterName
 * <li>AdapterShortDescription
 * <li>AdapterVendorName
 * <li>AdapterVersion
 * <li>InteractionSpecsSupported
 * <li>SpecVersion
 * <li>supportsExecuteWithInputAndOutputRecord
 * <li>supportsExecuteWithInputRecordOnly
 * <li>supportsLocalTransactionDemarcation
 * <li>deletesAreDetected
 * <li>insertsAreDetected
 * <li>othersDeletesAreVisible
 * <li>othersInsertsAreVisible
 * <li>othersUpdatesAreVisible
 * <li>ownDeletesAreVisible
 * <li>ownInsertsAreVisible
 * <li>ownUpdatesAreVisible
 * <li>supportsResultSetType
 * <li>supportsResultTypeConcurrency
 * <li>updatesAreDetected
 * </ol>
 *
 * @ibm-spi
 */

public interface InteractionMetrics {

    /**
     * At this level, component will call preInteraction/postInteraction
     * without passing any context data.
     */
    static public int TRAN_DETAIL_LEVEL_PERF = 1;
    /**
     * At this level, component will call preInteraction/postInteraction
     * and pass context data matching basic context names. This informaction
     * will vary depending on the particular method being executed. See
     * the method identifier constants for more info.
     */
    static public int TRAN_DETAIL_LEVEL_BASIC = 2;
    /**
     * At this level, component will call preInteraction/postInteraction
     * and pass context data matching extended context names. This informaction
     * will vary depending on the particular method being executed. See
     * the method identifier constants for more info.
     */
    static public int TRAN_DETAIL_LEVEL_EXTENDED = 3;

    /**
     * <code>RM_ARM_GOOD</code> - Good, the request is completed successfully
     */
    static int RM_ARM_GOOD = 0;
    /**
     * <code>RM_ARM_ABORT</code> - Aborted is intended for cases where the
     * transaction is interrupted/cancelled before completion.
     */
    static int RM_ARM_ABORT = 1;
    /**
     * <code>RM_ARM_FAILED</code> - Failed is intended for cases where program
     * logic determines that an operation cannot be successfully completed due
     * to some discovered error.
     */
    static int RM_ARM_FAILED = 2;
    /**
     * <code>RM_ARM_UNKNOWN</code> - Unknown is used when the transaction
     * status is not known.
     */
    static int RM_ARM_UNKNOWN = -1;

    /**
     * @return return the ARM correlator in byte[]. This method should be called
     *         before the resource adapter sends out the request to the downstream EIS.
     *         The resource adapter should attach the correlator with the JCA protocol
     *         so that the downstream EIS process can extract it and correlate the request.
     */
    public byte[] getCorrelator();

    /**
     * Returns one of the following:
     * <dl>
     * <dt>TRAN_DETAIL_LEVEL_PERF</dt>
     * <dd>At this level, component will call preInteraction/postInteraction
     * without passing any context data.
     * </dd>
     * <dt>TRAN_DETAIL_LEVEL_BASIC</dt>
     * <dd>At this level, component will call preInteraction/postInteraction
     * and pass context data matching basic context names. This informaction
     * will vary depending on the particular method being executed. See
     * the method identifier constants for more info.
     * </dd>
     * <dt>TRAN_DETAIL_LEVEL_EXTENDED</dt>
     * <dd>At this level, component will call preInteraction/postInteraction
     * and pass context data matching extended context names. This informaction
     * will vary depending on the particular method being executed. See
     * the method identifier constants for more info.
     * </dl>
     *
     * @return one of the following: TRAN_DETAIL_LEVEL_PERF,
     *         TRAN_DETAIL_LEVEL_BASIC, TRAN_DETAIL_LEVEL_EXTENDED
     *         <br>The returned integer indicates how much transaction detail information
     *         is needed for postInteraction. This method should be called right before
     *         postInteraction.
     */
    public int getTranDetailLevel();

    /**
     * The <code>preInteraction</code> method should be called by the
     * ResourceAdapter at the start of any methods for which interaction time is
     * to be measured, including but not limited to the following:
     *
     * <ul>
     * <li>ConnectionFactory.getConnection methods.
     * <li>Connection.close method.
     * <li>Interaction.executes methods.
     * </ul>
     * <p>Note: this method returns an opaque context Object. If during
     * the course of this methods execution the ResourceAdapter switches
     * threads then the context object must be passed along since it will then
     * be required on the <code>postInteraction</code> call.
     *
     * @param ctxData should be the actual class name (array index 0)and
     *            method name (array index 1) with the signature that preInteraction is
     *            called with.
     *
     * @return Oject is only needed if the ResourceAdapter will switch threads
     *         before the current method ends.
     */
    public Object preInteraction(String[] ctxData);

    /**
     * The <code>postInteraction</code> method should be called by the
     * ResourceAdapter at the end of any method for which the preInteraction
     * call was made.
     *
     * @param ctx - Oject is only needed if the ResourceAdapter switches
     *            threads between the start and the end of the current method.
     *            If not needed pass <code>null</code>.
     * @param status will return one of the following constants
     *            depending on if the transaction is successful or not.
     *            <ul>
     *            <li><code>RM_ARM_GOOD</code>
     *            <li><code>RM_ARM_ABORT</code>
     *            <li><code>RM_ARM_FAILED</code>
     *            </ul>
     */
    public void postInteraction(Object ctx, int status);

    /**
     * The <code>postInteraction</code> method should be called by the
     * ResourceAdapter at the end any method for which the preInteraction
     * call was made.
     *
     * @param ctx Oject is only needed if the ResourceAdapter switches
     *            threads between the start and the end of the current method. If not
     *            needed pass <code>null</code>.
     * @param status will return one of the following constants
     *            depending on if the transaction is successful or not.
     *            <ul>
     *            <li><code>RM_ARM_GOOD</code>
     *            <li><code>RM_ARM_ABORT</code>
     *            <li><code>RM_ARM_FAILED</code>
     *            </ul>
     * @param detailInfo If required then the detailInfo which is passed back is a property
     *            object which contains additional dynamic content which is appropriate for
     *            the given TRAN_DETAIL_LEVEL.
     */
    public void postInteraction(Object ctx, int status, java.util.Properties detailInfo);

    /**
     * Indicates whether or not the InteractionMetrics is enabled for
     * instrumentation. This method should be called for every execute method.
     *
     * @return boolean When it returns false, the resource adapter should not call
     *         preInteraction/postInteraction/getCorrelator.
     *         <p> When it returns true, the resource adapter will call
     *         preInteraction/postInteraction/getCorrelator.
     */
    public boolean isInteractionMetricsEnabled();

}
