/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.springboot.support.version30.test.jms.app;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JmsController {
    private final MessageService messageService;
    private final BookingService bookingService;
	
    @Autowired
    public JmsController(MessageService messageService, BookingService bookingService) {
        this.messageService = messageService;
        this.bookingService = bookingService;
    }
    
    @Transactional
    @RequestMapping("/book")
    public String book(@RequestParam("name") String name, boolean failTransaction) throws Exception {
    	// Both operations are in a single atomic transaction. When the exception occurs after the transaction then both operations (Datasource & JMS) will roll back.
    	bookingService.book(name);
        messageService.send("Booking Confirmed for "+ name);
        // Simulate a runtime exception AFTER sending message
        if (failTransaction) {
            throw new RuntimeException("Simulated failure after JMS");
        }
        return "Booking Successful";
    }
    
    @RequestMapping("/totalBookings1")
    public int bookings1() throws Exception {  
        return bookingService.findAllBookings1().size();
    }
    
    @RequestMapping("/totalBookings2")
    public int bookings2() throws Exception {  
        return bookingService.findAllBookings2().size();
    }
}
