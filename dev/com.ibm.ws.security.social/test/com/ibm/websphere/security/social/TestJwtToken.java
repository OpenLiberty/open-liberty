package com.ibm.websphere.security.social;

import java.io.Serializable;

import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtToken;

/**
 * Test JwtToken to use in UserProfileSerializationTest.
 */
public class TestJwtToken implements JwtToken, Serializable {

	private static final long serialVersionUID = 1L;

	@Override
	public Claims getClaims() {
		return null;
	}

	@Override
	public String getHeader(String name) {
		return "header1";
	}

	@Override
	public String compact() {
		return null;
	}

}
