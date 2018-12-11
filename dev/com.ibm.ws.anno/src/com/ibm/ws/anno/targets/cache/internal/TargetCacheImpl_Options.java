/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.targets.cache.internal;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.anno.targets.cache.TargetCache_Options;

public class TargetCacheImpl_Options implements TargetCache_Options {

    public TargetCacheImpl_Options(
        boolean disabled,
        String dir,
        boolean readOnly,
        boolean alwaysValid,
        // boolean validate,
        int writeThreads) {
    
        this.disabled = disabled;

        this.dir = dir;
        this.readOnly = readOnly;
        this.alwaysValid = alwaysValid;
        // this.validate = validate;
        this.writeThreads = writeThreads;
    }

    //

    private boolean disabled;

    @Override
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    @Trivial
    public boolean getDisabled() {
        return disabled;
    }

    //

    private String dir;

    @Override
    public void setDir(String dir) {
        this.dir = dir;
    }

    @Override
    @Trivial
    public String getDir() {
        return dir;
    }

    //

    private boolean readOnly;

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    @Trivial
    public boolean getReadOnly() {
        return readOnly;
    }

    //

    @Override
    public void setAlwaysValid(boolean alwaysValid) {
        this.alwaysValid = alwaysValid;
    }

    private boolean alwaysValid;

    @Override
    @Trivial
    public boolean getAlwaysValid() {
        return alwaysValid;
    }

    //

    // private boolean validate;

    // @Override
    // public void setValidate(boolean validate) {
    //     this.validate = validate;
    // }

    // @Override
    // @Trivial
    // public boolean getValidate() {
    //     return validate;
    // }

    //

    private int writeThreads;

    @Override
    @Trivial
    public int getWriteThreads() {
        return writeThreads;
    }

    @Override
    public void setWriteThreads(int writeThreads) {
        this.writeThreads = writeThreads;
    }
}
