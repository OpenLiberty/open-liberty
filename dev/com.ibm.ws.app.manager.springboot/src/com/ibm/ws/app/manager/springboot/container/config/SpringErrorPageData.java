package com.ibm.ws.app.manager.springboot.container.config;

public class SpringErrorPageData {
    private String location;
    private Integer errorCode;
    private boolean isGlobal;
    private String exceptionType;

    public boolean isGlobal() {
        return isGlobal;
    }

    public void setGlobal(boolean isGlobal) {
        this.isGlobal = isGlobal;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @param exceptionType the exceptionType to set
     */
    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "(isGlobal=" + isGlobal + " Location=" + location + " Exception-Type=" + exceptionType + " Error-Code=" + errorCode + ")";
    }
}
