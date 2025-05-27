package com.oracle.jpa.bugtest.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

@Entity
public class TableIdEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private Long id;

	private String name;

	public static TableIdEntity of(String name) {
		TableIdEntity uuidIdEntity = new TableIdEntity();
		uuidIdEntity.setName(name);
		return uuidIdEntity;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	@PrePersist
	private void prePersist() {
		if(this.id == null) {
			throw new IllegalStateException("Inside '@PrePersist', 'ID' is null");
		}
	}
}
