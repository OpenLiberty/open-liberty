package com.ibm.ws.springboot.support.version15.test.multi.context.app.child2;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServletController2 {
	@RequestMapping("/")
	public String hello() {
		return "HELLO SPRING BOOT!!";
	}
}
