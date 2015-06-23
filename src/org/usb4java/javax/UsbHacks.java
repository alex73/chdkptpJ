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
package org.usb4java.javax;

import java.lang.reflect.Field;

import javax.usb.UsbDevice;
import javax.usb.UsbEndpoint;
import javax.usb.UsbEndpointDescriptor;

import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;

/**
 * Some USB hacks.
 * 
 * Since AbstractDevice is not a public class, I had to create this helper in the package org.usb4java.javax.
 */
public class UsbHacks {
    /**
     * Reset USB device.
     */
    public static void reset(UsbDevice device) throws Exception {
        AbstractDevice a = (AbstractDevice) device;
        DeviceHandle handle = a.open(); // doesn't really open - just get handle
        LibUsb.resetDevice(handle);
    }

    /**
     * Device ID is accessible only from this package.
     */
    public static UsbDeviceId getDeviceId(UsbDevice device) {
        AbstractDevice a = (AbstractDevice) device;
        return new UsbDeviceId(a.getId());
    }

    /**
     * IrpQueue uses wMaxPacketSize as maximum buffer size for USB devices read. It's wrong way. chdkptp uses
     * 16k buffer for read raw image, but usb4java uses only 0.5k buffer. As result, transfer is very slow.
     * This method increase wMaxPacketSize for increase buffer in the IrpQueue.
     */
    public static void setMaxTransferRate(UsbEndpoint endpoint, short bufferSize) {
        try {
            UsbEndpointDescriptor d = endpoint.getUsbEndpointDescriptor();
            Field f = d.getClass().getDeclaredField("wMaxPacketSize");
            f.setAccessible(true);

            f.setShort(d, bufferSize);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static class UsbDeviceId {

        /** The bus number. */
        private final int busNumber;

        /** The device address. */
        private final int deviceAddress;

        /** The port this device is connected to. 0 if unknown. */
        private final int portNumber;

        public UsbDeviceId(DeviceId dev) {
            this.busNumber = dev.getBusNumber();
            this.deviceAddress = dev.getDeviceAddress();
            this.portNumber = dev.getPortNumber();
        }

        public int getBusNumber() {
            return busNumber;
        }

        public int getDeviceAddress() {
            return deviceAddress;
        }

        public int getPortNumber() {
            return portNumber;
        }
    }
}
