/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.install;

import com.ibm.ws.install.internal.InstallLogUtils.Messages;

/**
 * An enum for specifying what action to take if a file to be installed already exists.
 */

public class RepositoryConfigValidationResult {

    public static enum ValidationFailedReason {

        INVALID_VALUE,

        INVALID_KEY,

        EMPTY_KEY,

        EMPTY_VALUE,

        MISSING_REPONAME,

        MISSING_PORT,

        INVALID_PORT,

        MISSING_HOST,

        INVALID_HOST,

        INVALID_URL,

        DUPLICATE_KEY,

        UNSUPPORTED_PROTOCOL;

    }

    private final int lineNum;
    private final ValidationFailedReason failedReason;
    private String validationMessage;

    /**
     * @param lineNum
     * @param failedReason
     */
    public RepositoryConfigValidationResult(int lineNum, ValidationFailedReason failedReason, String validationMessage) {
        super();
        this.lineNum = lineNum;
        this.failedReason = failedReason;
        this.validationMessage = validationMessage;
    }

    /**
     * @return the validationMessage
     */
    public String getValidationMessage() {
        return validationMessage;
    }

    /**
     * @param validationMessage the validationMessage to set
     */
    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }

    /**
     * @return the lineNum
     */
    public int getLineNum() {
        return lineNum;
    }

    /**
     * @return the failedReason
     */
    public String getFailedReason() {
        switch (failedReason) {
            case INVALID_VALUE:
                return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("VALIDATION_INVALID_VALUE");
            case INVALID_KEY:
                return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("VALIDATION_INVALID_KEY");
            case EMPTY_KEY:
                return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("VALIDATION_EMPTY_KEY");
            case EMPTY_VALUE:
                return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("VALIDATION_EMPTY_VALUE");
            case MISSING_REPONAME:
                return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("VALIDATION_MISSING_REPONAME");
            case MISSING_PORT:
                return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("VALIDATION_MISSING_PORT");
            case INVALID_PORT:
                return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("VALIDATION_INVALID_PORT");
            case MISSING_HOST:
                return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("VALIDATION_MISSING_HOST");
            case INVALID_HOST:
                return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("VALIDATION_INVALID_HOST");
            case INVALID_URL:
                return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("VALIDATION_INVALID_URL");
            case DUPLICATE_KEY:
                return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("VALIDATION_DUPLICATE_KEY");
            case UNSUPPORTED_PROTOCOL:
                return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("VALIDATION_UNSUPPORTED_PROTOCOL");
            default:
                return null;
        }

    }

}
