/**************************************************************************
 chdkptpJ - Java CHDK PTP framework.

 Copyright (C) 2015 Aleś Bułojčyk (alex73mail@gmail.com)

 This file is part of chdkptpJ.

 chdkptpJ is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 chdkptpJ is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/
package org.alex73;

import java.util.ArrayList;
import java.util.List;

import javax.usb.UsbDevice;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbServices;
import javax.usb.util.UsbUtil;

public class UsbUtils {
    /**
     * Convert USB ID into 4-digit string.
     */
    public static String hex4(short v) {
        String r = Integer.toHexString(UsbUtil.unsignedInt(v));
        return "0000".substring(r.length()) + r;
    }

    /**
     * List all USB devices except hubs.
     */
    public static List<UsbDevice> listAllUsbDevices() throws Exception {
        UsbServices services = UsbHostManager.getUsbServices();
        UsbHub rootHub = services.getRootUsbHub();
        List<UsbDevice> devices = new ArrayList<UsbDevice>();
        listAllUsbDevices(rootHub, devices);
        return devices;
    }

    private static void listAllUsbDevices(UsbHub hub, List<UsbDevice> devices) {
        @SuppressWarnings("unchecked")
        List<UsbDevice> hubDevices = hub.getAttachedUsbDevices();
        for (UsbDevice usbDevice : hubDevices) {
            if (usbDevice.isUsbHub()) {
                listAllUsbDevices((UsbHub) usbDevice, devices);
            } else {
                devices.add(usbDevice);
            }
        }
    }
}
