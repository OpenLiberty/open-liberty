package com.ibm.websphere.samples.batch.beans;

import java.io.Serializable;

public class ValidationPartitionCollectorDataObject implements Serializable {

	private static final long serialVersionUID = 1L;

	private int previousExitStatus ;
	
	private int chunkNum ;
	
	private int currentChunkExitStatus;

	/**
	 * @return the previousExitStatus
	 */
	public int getPreviousExitStatus() {
		return previousExitStatus;
	}

	/**
	 * @param previousExitStatus the previousExitStatus to set
	 */
	public void setPreviousExitStatus(int previousExitStatus) {
		this.previousExitStatus = previousExitStatus;
	}

	/**
	 * @return the chunkNum
	 */
	public int getChunkNum() {
		return chunkNum;
	}

	/**
	 * @param chunkNum the chunkNum to set
	 */
	public void setChunkNum(int chunkNum) {
		this.chunkNum = chunkNum;
	}

	/**
	 * @return the currentChunkExitStatus
	 */
	public int getCurrentChunkExitStatus() {
		return currentChunkExitStatus;
	}

	/**
	 * @param currentChunkExitStatus the currentChunkExitStatus to set
	 */
	public void setCurrentChunkExitStatus(int currentChunkExitStatus) {
		this.currentChunkExitStatus = currentChunkExitStatus;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ValidationPartitionCollectorDataObject [previousExitStatus=" + previousExitStatus + ", chunkNum="
				+ chunkNum + ", currentChunkExitStatus=" + currentChunkExitStatus + "]";
	}
	
	
}
