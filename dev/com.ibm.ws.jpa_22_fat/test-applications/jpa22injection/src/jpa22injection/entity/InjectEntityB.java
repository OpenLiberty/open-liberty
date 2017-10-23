package jpa22injection.entity;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="Inject_Ent_B")
public class InjectEntityB {
	@Id
	@GeneratedValue
	private long id;
	
	@Basic
	private String strData;
	
	public InjectEntityB() {
		
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

	@Override
	public String toString() {
		return "InjectEntityB [id=" + id + ", strData=" + strData + "]";
	}
}
