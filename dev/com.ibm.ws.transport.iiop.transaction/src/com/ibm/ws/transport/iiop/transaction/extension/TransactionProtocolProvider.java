/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.iiop.transaction.extension;

import org.omg.CosTransactions.PropagationContext;
import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.IORInfo;

/**
 * Internal Liberty extension interface: a single unified interface for transaction
 * protocol providers.
 *
 * Each protocol (e.g. WS-AT) registers exactly one implementation of this interface
 * as an OSGi service. The OL core interceptors are thin coordinators that delegate
 * entirely to registered providers — they have zero knowledge of any protocol's
 * wire format or payload structure.
 *
 * <h3>Extension boundary principle</h3>
 * This interface defines what a provider <em>exposes</em> to OL. It says nothing
 * about how the provider is implemented internally. A WS-CD provider may use a
 * single class or a hierarchy of internal delegates; OL never sees inside that
 * boundary. The extension interface is intentionally minimal: one interface, no
 * helper interfaces.
 *
 * <h3>Method groups</h3>
 * <ul>
 *   <li><b>Identity:</b> {@link #getProtocolName()}, {@link #getPriority()}</li>
 *   <li><b>IOR advertisement:</b> {@link #getIORTagId()}, {@link #contributeToIOR(IORInfo, Codec)}</li>
 *   <li><b>Client side:</b> {@link #handlesIOR(ClientRequestInfo)},
 *       {@link #exportTransaction(ClientRequestInfo, Codec, TransactionHandlerContext)},
 *       {@link #unexportTransaction(ClientRequestInfo, TransactionHandlerContext, boolean)}</li>
 *   <li><b>Server side:</b> {@link #getExpectedISDTypeId()},
 *       {@link #importTransaction(PropagationContext, TransactionHandlerContext)},
 *       {@link #unimportTransaction(TransactionHandlerContext)}</li>
 * </ul>
 */
public interface TransactionProtocolProvider {

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /**
     * Human-readable protocol name for trace and logging (e.g. "WS-AT").
     */
    String getProtocolName();

    /**
     * Priority for client-side provider selection. Lower value = higher priority.
     * When multiple providers could handle a given IOR, the one with the lowest
     * priority value is selected first.
     */
    int getPriority();

    // -------------------------------------------------------------------------
    // IOR advertisement — provider owns its own tag entirely
    // -------------------------------------------------------------------------

    /**
     * The OMG/IBM-vendor IOR tag ID this provider writes and reads.
     * Each protocol provider must use a distinct tag ID.
     * IBM vendor-reserved range: 0x49424d00–0x49424dFF.
     */
    int getIORTagId();

    /**
     * Add this provider's tagged component(s) to the IOR being established.
     * Called by {@code IORTransactionInterceptor} for every enabled provider
     * during IOR creation. The provider is responsible for the complete payload
     * format of its own tag — OL never parses it.
     *
     * @param info  the IOR info (use {@code add_ior_component_to_profile})
     * @param codec the CORBA CDR codec, available for encoding if needed
     */
    void contributeToIOR(IORInfo info, Codec codec);

    // -------------------------------------------------------------------------
    // Client side
    // -------------------------------------------------------------------------

    /**
     * Returns true if the target IOR advertises this protocol, i.e. if this
     * provider should be used to export the current transaction for this call.
     * Typically implemented as {@code ri.get_effective_component(getIORTagId()) != null}.
     *
     * @param ri the client request info for the outbound call
     */
    boolean handlesIOR(ClientRequestInfo ri);

    /**
     * Export the current transaction into the outbound IIOP request.
     * Called only when {@link #handlesIOR(ClientRequestInfo)} returned true.
     *
     * <p>Implementations must not throw checked exceptions. Any failure
     * should surface as a CORBA {@code SystemException}.
     *
     * @param ri      the client request info
     * @param codec   the CORBA CDR codec
     * @param context provides access to TransactionManager and RemoteTransactionController
     */
    void exportTransaction(ClientRequestInfo ri, Codec codec,
                           TransactionHandlerContext context);

    /**
     * Restore the transaction context after the remote call completes.
     * Always paired with a prior successful {@link #exportTransaction} call.
     *
     * <p>The {@code exceptionOccurred} flag mirrors the distinction the OMG
     * Portable Interceptor contract draws between {@code receive_reply} and
     * {@code receive_exception}: the interceptor calls this method from both
     * points but the provider may need to behave differently.
     *
     * <p>Implementations must not throw checked exceptions. Any failure to
     * clean up should be handled internally. A CORBA {@code SystemException}
     * (e.g. {@code org.omg.CORBA.TRANSACTION_ROLLEDBACK}) may be thrown as
     * an unchecked exception; the OL interceptor is responsible for catching
     * and translating IBM-internal variants.
     *
     * @param ri                the client request info
     * @param context           provides access to TransactionManager and
     *                          RemoteTransactionController
     * @param exceptionOccurred {@code true} if called from
     *                          {@code receive_exception}, {@code false} if
     *                          called from {@code receive_reply} or
     *                          {@code receive_other}
     */
    void unexportTransaction(ClientRequestInfo ri,
                             TransactionHandlerContext context,
                             boolean exceptionOccurred);

    // -------------------------------------------------------------------------
    // Server side
    // -------------------------------------------------------------------------

    /**
     * The IDL repository ID of the ISD ({@code PropagationContext.implementation_specific_data})
     * type that this provider handles, or {@code null} to be called for every incoming
     * context regardless of ISD type (wildcard).
     *
     * <p>OL calls {@code isd.type().id()} on the incoming context — a pure metadata
     * read with no classloader dependency — and compares it against this value before
     * calling {@link #importTransaction}. Providers are skipped unless the type matches.
     * This eliminates the need for providers to self-identify from the payload.
     *
     * <p>Example (WS-AT / TREX):
     * {@code "IDL:com.ibm.ws.transport.transaction/wsat/JTSWSATPropagationData:1.0"}
     */
    default String getExpectedISDTypeId() {
        return null; // wildcard — called for any ISD type
    }

    /**
     * Attempt to import a transaction from the incoming CORBA PropagationContext.
     * Only called when {@link #getExpectedISDTypeId()} matches the ISD type of the
     * incoming context (or when the provider declared a wildcard).
     *
     * @param propagationContext the decoded PropagationContext; ISD Any is unmaterialised
     * @param context            provides access to TransactionManager and RemoteTransactionController
     * @return true if this provider handled and imported the transaction; false to pass
     *         to the next provider (should not normally happen after type pre-screening)
     * @throws org.omg.CORBA.SystemException if the provider recognised the context
     *         but failed to import it — OL will not try further providers and will
     *         propagate the CORBA exception to the ORB
     */
    boolean importTransaction(PropagationContext propagationContext,
                              TransactionHandlerContext context);

    /**
     * Clean up resources associated with a previously imported transaction.
     * Always paired with a prior successful {@link #importTransaction} call.
     *
     * <p>Implementations must not throw. Any cleanup failure should be logged
     * with FFDC and handled internally.
     *
     * @param context provides access to TransactionManager and RemoteTransactionController
     */
    void unimportTransaction(TransactionHandlerContext context);
}
