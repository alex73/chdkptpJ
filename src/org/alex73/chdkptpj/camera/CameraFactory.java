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
package org.alex73.chdkptpj.camera;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbServices;

/**
 * Factory for create cameras objects. It always returns list because user can connect more that one camera.
 * Developers should remember about that.
 */
public class CameraFactory {
    public static final short KNOWN_VENDOR = 0x04a9; // Canon
    public static final Set<Short> KNOWN_PRODUCTS;

    public static final short[] KNOWN_PRODUCT_IDS = { 0x3259 /* SX50 */, 0x325a /* SX160 */,
            0x31ef /* A495 */, 0x32aa /* Ixus 160 */};

    static {
        KNOWN_PRODUCTS = new HashSet<>();
        for (short id : KNOWN_PRODUCT_IDS) {
            KNOWN_PRODUCTS.add(id);
        }
    }

    /**
     * Find all known attached cameras.
     */
    public static Collection<Camera> findCameras() throws Exception {
        List<Camera> result = new ArrayList<>();
        for (UsbDevice device : listAllUsbDevices()) {
            UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
            if (KNOWN_VENDOR == desc.idVendor() && KNOWN_PRODUCTS.contains(desc.idProduct())) {
                result.add(new Camera(device));
            }
        }
        return result;
    }

    /**
     * Find cameras for specified vendor:product IDs.
     */
    public static Collection<Camera> findCameras(short vendor, short product) throws Exception {
        List<Camera> result = new ArrayList<>();
        for (UsbDevice device : listAllUsbDevices()) {
            UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
            if (vendor == desc.idVendor() && product == desc.idProduct()) {
                result.add(new Camera(device));
            }
        }
        return result;
    }

    /**
     * List all USB devices except hubs.
     */
    private static List<UsbDevice> listAllUsbDevices() throws Exception {
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
