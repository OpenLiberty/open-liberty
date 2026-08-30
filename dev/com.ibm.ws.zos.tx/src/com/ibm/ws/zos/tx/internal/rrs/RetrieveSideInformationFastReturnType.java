/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.zos.tx.internal.rrs;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Holds all information returned from the call to
 * RRS' retrieve side information fast (atr4rusf) service.
 */
public final class RetrieveSideInformationFastReturnType {

    /**
     * The service's return code.
     */
    private final int rc;

    /**
     * The environment information in integer form.
     */
    private final int envInfo;

    /**
     * Constructor
     */
    public RetrieveSideInformationFastReturnType(int rc, int envInfo) {
        this.rc = rc;
        this.envInfo = envInfo;
    }

    /**
     * Retrieves the service's return code.
     *
     * @return The service's return code.
     */
    @Trivial
    public int getReturnCode() {
        return rc;
    }

    /**
     * Retrieves the environment information passed as an integer value.
     *
     * @return
     */
    @Trivial
    public int getEnvironmentInfo() {
        return envInfo;
    }

    /**
     * Retrieves the no_interest information from the environment information.
     *
     * @return True if the unit of recovery has no interests. False otherwise.
     */
    @Trivial
    public boolean isNoInterest() {
        return ((envInfo & RRSServices.ATR_NO_INTERESTS_MASK) == RRSServices.ATR_NO_INTERESTS_MASK);
    }

    /**
     * Retrieves the resource_manager_coordinator_ok information from the environment information.
     * This means that unit of recovery has one or more expression of interests and
     * the resource manager can coordinate its own resources.
     *
     * @return True if the resource manager can coordinate its own resources. False otherwise.
     */
    @Trivial
    public boolean isRMCoordinatorOK() {
        return ((envInfo & RRSServices.ATR_RM_COORD_OK_MASK) == RRSServices.ATR_RM_COORD_OK_MASK);
    }

    /**
     * Retrieves the zero_interest_count information from the environment information.
     * Returned only if ATR_ZERO_INTEREST_COUNT_MASK was specified as part of the service
     *
     * @return True if the unit of recovery has no interests. False otherwise.
     */
    @Trivial
    public boolean isZeroInterestCount() {
        return ((envInfo & RRSServices.ATR_ZERO_INTEREST_COUNT_MASK) == RRSServices.ATR_ZERO_INTEREST_COUNT_MASK);
    }

    /**
     * Retrieves the one_interest_count information from the environment information.
     * Returned only if ATR_INTEREST_COUNT_MASK was specified as part of the service call.
     *
     * @return True if the resource manager has expressed only one interest in the
     *         unit of recovery. False otherwise.
     */
    @Trivial
    public boolean isOneInterestOnly() {
        return ((envInfo & RRSServices.ATR_ONE_INTEREST_COUNT_MASK) == RRSServices.ATR_ONE_INTEREST_COUNT_MASK);
    }

    /**
     * Retrieves the multiple_interest_count information from the environment information.
     * Returned only if ATR_INTEREST_COUNT_MASK was specified as part of the service call.
     *
     * @return True if one or more resource managers have expressed multiple interest in the
     *         unit of recovery. False otherwise.
     */
    @Trivial
    public boolean isMultipleInterestCount() {
        return ((envInfo & RRSServices.ATR_MULTIPLE_INTEREST_COUNT_MASK) == RRSServices.ATR_MULTIPLE_INTEREST_COUNT_MASK);
    }

    /**
     * Retrieves the is_ur_state_in_reset information from the environment information.
     *
     * @return True if the UR's state is in-reset. False otherwise.
     */
    @Trivial
    public boolean isURStateInReset() {
        return ((envInfo & RRSServices.ATR_UR_STATE_IN_RESET_MASK) == RRSServices.ATR_UR_STATE_IN_RESET_MASK);
    }

    /**
     * Retrieves the global_mode information from the environment information.
     *
     * @return True if The unit of recovery transaction mode is global, and the UR state
     *         is beyond in-reset. False otherwise.
     */
    @Trivial
    public boolean isURInGlobalMode() {
        return ((envInfo & RRSServices.ATR_GLOBAL_MODE_MASK) == RRSServices.ATR_GLOBAL_MODE_MASK);
    }

    /**
     * Retrieves the local_mode information from the environment information.
     *
     * @return True if The unit of recovery transaction mode is local, and the UR state
     *         is beyond in-reset. False otherwise.
     */
    @Trivial
    public boolean isURInLocalMode() {
        return ((envInfo & RRSServices.ATR_LOCAL_MODE_MASK) == RRSServices.ATR_LOCAL_MODE_MASK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("RetrieveSideInformationFastReturnType [");
        sb.append("ReturnCode: ");
        sb.append(Integer.toHexString(rc));

        if (rc == 0) {
            sb.append(", HasNoInterests: ");
            sb.append(isNoInterest());
            sb.append(", IsRMCoordinatorOK: ");
            sb.append(isRMCoordinatorOK());
            sb.append(", IsZeroInterestCount: ");
            sb.append(isZeroInterestCount());
            sb.append(", HasOneInterestOnly: ");
            sb.append(isOneInterestOnly());
            sb.append(", isMultipleInterestCount: ");
            sb.append(isMultipleInterestCount());
            sb.append(", IsURStateInReset: ");
            sb.append(isURStateInReset());
            sb.append(", IsURInGlobalMode: ");
            sb.append(isURInGlobalMode());
            sb.append(", IsURInLocalMode: ");
            sb.append(isURInLocalMode());
        }
        sb.append("]");
        return sb.toString();
    }
}
