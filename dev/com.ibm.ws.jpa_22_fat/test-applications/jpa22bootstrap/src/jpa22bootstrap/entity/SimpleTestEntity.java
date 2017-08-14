package jpa22bootstrap.entity;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

@Entity
public class SimpleTestEntity {
	@Id
	@GeneratedValue
	private long id;
	
	@Basic
	private String strData;
	
	@Version
	private long version;
	
	public SimpleTestEntity() {
		
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getStrData() {
		return strData;
	}

	public void setStrData(String strData) {
		this.strData = strData;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return "SimpleTestEntity [id=" + id + ", strData=" + strData + ", version=" + version + "]";
	}
	
	
}
