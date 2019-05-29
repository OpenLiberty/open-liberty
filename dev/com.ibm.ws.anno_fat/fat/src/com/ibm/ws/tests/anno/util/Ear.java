package com.ibm.ws.tests.anno.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Logical representation of an EAR: Has a name and a list of logical WARs.
 */
public class Ear {

	public Ear(String name) {
		this.name = name;
		this.wars = new ArrayList<War>();
	}

	//

	private final String name;

	public String getName() {
		return name;
	}

	//

	private final List<War> wars;

	public List<War> getWars() {
		return wars;
	}

	public void addWar(War war) {
		wars.add(war);
	}
}
