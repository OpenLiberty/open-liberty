package com.ibm.ws.jpa.clientcontainer.fat.basic.jndi.entities;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class AnotherSimpleEntity {
	@Id
	private long id;
	
	@Basic
	private String strData;
	
	public AnotherSimpleEntity() {
		
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @return the strData
	 */
	public String getStrData() {
		return strData;
	}

	/**
	 * @param strData the strData to set
	 */
	public void setStrData(String strData) {
		this.strData = strData;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "AnotherSimpleEntity [id=" + id + ", strData=" + strData + "]";
	}
}
