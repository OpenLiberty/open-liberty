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
package com.ibm.ws.zos.channel.wola.internal.otma;

/**
 * Thrown if a non-zero return code is returned from the OTMA function calls
 */
public class OTMAException extends Exception {
    private static final long serialVersionUID = 7578897153787710825L;
    private final int[] returnCodeArea;

    /**
     * Validate the return code area (the return codes we got back from IMS).
     * Since the caller is trying to report an error, it won't do a lot of good to re-throw
     * from here if the arguments are no good, so just make something up if we don't have a
     * valid area.
     *
     * @param inputReturnCodeArea The OTMA return and reason codes returned from the OTMA C/I.
     * @return The return code area that should be set in this exception instance.
     */
    private static int[] validateReturnCodeArea(int[] inputReturnCodeArea) {
        int[] returnCodeArea = new int[] { -1, -1, -1, -1, -1 };
        if (inputReturnCodeArea != null) {
            int elementsToCopy = Math.min(inputReturnCodeArea.length, 5);
            System.arraycopy(inputReturnCodeArea, 0, returnCodeArea, 0, elementsToCopy);
        }
        return returnCodeArea;
    }

    /**
     * construct an easy-to-read message describing which OTMA C/I service failed, and what the
     * return and reason codes were.
     *
     * @param returnCodeArea The return and reason codes returned by OTMA C/I.
     * @return A string describing the error.
     */
    private static String constructDefaultErrorMessage(int[] returnCodeArea) {
        String otmaFunction = getFunctionName(validateReturnCodeArea(returnCodeArea)[4]);
        return "OTMA C/I function " + otmaFunction + " failed with return code " + returnCodeArea[0] +
               "and reason codes " + returnCodeArea[1] + "/" + returnCodeArea[2] + "/" + returnCodeArea[3] + "/" + returnCodeArea[4];
    }

    /**
     * Converts an OTMA C/I numberic function code to its textual equivalent.
     *
     * @param functionReasonCode The OTMA C/I function code
     * @return A string representation of the OTMA C/I function code.
     */
    private static String getFunctionName(int functionReasonCode) {
        if (functionReasonCode == 2) {
            return "otma_open";

        } else if (functionReasonCode == 5) {
            return "otma_alloc";

        } else if (functionReasonCode == 7) {
            return "otma_send_receive";

        } else if (functionReasonCode == 14) {
            return "otma_free";

        } else if (functionReasonCode == 15) {
            return "otma_close";

        } else {
            return "unknown";
        }
    }

    /**
     * Create an OTMAException describing an OTMA C/I failure.
     *
     * @param returnCodeArea The return and reason codes returned by OTMA C/I.
     */
    public OTMAException(int[] returnCodeArea) {
        super(constructDefaultErrorMessage(returnCodeArea));
        this.returnCodeArea = validateReturnCodeArea(returnCodeArea);
    }

    /**
     * Create an OTMAException describing an OTMA C/I failure.
     *
     * @param returnCodeArea The return and reason codes returned by OTMA C/I.
     * @param errorMessage   The error message returned by OTMA C/I.
     */
    public OTMAException(int[] returnCodeArea, String errorMessage) {
        super(((errorMessage != null) && (errorMessage.trim().length() > 0)) ? errorMessage.trim() : constructDefaultErrorMessage(returnCodeArea));
        this.returnCodeArea = validateReturnCodeArea(returnCodeArea);
    }

    /**
     * Gets the OTMA C/I function name that reported this failure.
     *
     * @return The OTMA C/I function name that reported this failure.
     */
    public String getFunctionName() {
        return getFunctionName(returnCodeArea[4]);
    }

    /**
     * Gets a string representation of the OTMA C/I reason codes.
     *
     * @return A string representation of the OTMA C/I reason codes.
     */
    public String getReasonCodesString() {
        return returnCodeArea[1] + "/" + returnCodeArea[2] + "/" + returnCodeArea[3] + "/" + returnCodeArea[4];
    }

    /**
     * Gets a numeric representation of the OTMA C/I reason codes.
     *
     * @return A numeric representation of the OTMA C/I reason codes.
     */
    public int[] getReasonCodes() {
        int[] reasonCodes = new int[4];
        reasonCodes[0] = returnCodeArea[1];
        reasonCodes[1] = returnCodeArea[2];
        reasonCodes[2] = returnCodeArea[3];
        reasonCodes[3] = returnCodeArea[4];

        return reasonCodes;
    }

    /**
     * Gets the OTMA C/I return code from the OTMA C/I service that failed.
     *
     * @return The return code.
     */
    public int getReturnCode() {
        return returnCodeArea[0];
    }
}
