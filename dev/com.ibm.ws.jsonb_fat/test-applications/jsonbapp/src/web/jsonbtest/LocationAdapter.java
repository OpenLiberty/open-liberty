/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.jsonbtest;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.bind.adapter.JsonbAdapter;

/**
 * Converts Location (such as ReservableRoom/Pod) to/from types that JSON-B can handle.
 */
public class LocationAdapter implements JsonbAdapter<Location, Map<String, ?>> {
    @Override
    public Location adaptFromJson(Map<String, ?> map) throws Exception {
        Location l;
        String podNumber = (String) map.get("podNumber");
        if (podNumber != null) { // must be Pod
            Pod pod = new Pod();
            pod.setPodNumber(podNumber);
            l = pod;
        } else { // must be ReservableRoom
            ReservableRoom rr = new ReservableRoom();
            rr.setCapacity(((BigDecimal) map.get("capacity")).shortValue());
            rr.setRoomName((String) map.get("roomName"));
            rr.setRoomNumber((String) map.get("roomNumber"));
            l = rr;
        }
        l.setBuilding((String) map.get("building"));
        l.setCity((String) map.get("city"));
        l.setFloor(((BigDecimal) map.get("floor")).shortValue());
        l.setState((String) map.get("state"));
        l.setStreetAddress((String) map.get("address"));
        l.setZipCode(((BigDecimal) map.get("zipCode")).intValue());
        return l;
    }

    @Override
    public Map<String, ?> adaptToJson(Location l) throws Exception {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("building", l.getBuilding());
        map.put("floor", l.getFloor());
        if (l instanceof Pod) {
            map.put("podNumber", ((Pod) l).getPodNumber());
        } else { // ReservableRoom
            map.put("roomNumber", ((ReservableRoom) l).getRoomNumber());
            map.put("roomName", ((ReservableRoom) l).getRoomName());
            map.put("capacity", ((ReservableRoom) l).getCapacity());
        }
        map.put("address", l.getStreetAddress());
        map.put("city", l.getCity());
        map.put("state", l.getState());
        map.put("zipCode", l.getZipCode());
        return map;
    }
}