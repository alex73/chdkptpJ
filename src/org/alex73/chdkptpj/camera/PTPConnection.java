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

import java.util.Arrays;
import java.util.List;

import javax.usb.UsbConst;
import javax.usb.UsbDevice;
import javax.usb.UsbEndpoint;
import javax.usb.UsbInterface;
import javax.usb.UsbIrp;
import javax.usb.UsbPipe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.javax.UsbHacks;

/**
 * PTP connection.
 */
public class PTPConnection {
    private static Logger LOG = LoggerFactory.getLogger(PTPConnection.class);

    public static final int RESPONSE_TIMEOUT = 10000;

    private final List<UsbInterface> interfaces;
    private final UsbPipe pipeIn;
    private final UsbPipe pipeOut;

    private final byte[] recbuf;

    /**
     * TransactionIDs are continuous sequences in numerical order starting from 0x00000001. The TransactionID
     * used for the OpenSession operation shall be 0x00000000.
     */
    private int currentTransaction;

    @SuppressWarnings("unchecked")
    public PTPConnection(UsbDevice device, int receiveBufferSize) throws Exception {
        recbuf = new byte[receiveBufferSize];

        // find endpoints
        UsbEndpoint endpointInBulk = null;
        UsbEndpoint endpointOutBulk = null;

        interfaces = device.getActiveUsbConfiguration().getUsbInterfaces();
        for (UsbInterface i : interfaces) {
            List<UsbEndpoint> endpoints = i.getUsbEndpoints();
            for (UsbEndpoint e : endpoints) {
                if (e.getDirection() == UsbConst.ENDPOINT_DIRECTION_IN
                        && e.getType() == UsbConst.ENDPOINT_TYPE_BULK) {
                    endpointInBulk = e;
                }
                if (e.getDirection() == UsbConst.ENDPOINT_DIRECTION_OUT
                        && e.getType() == UsbConst.ENDPOINT_TYPE_BULK) {
                    endpointOutBulk = e;
                }
            }
        }
        if (endpointInBulk == null) {
            LOG.error("There is no In Bulk endpoint");
            throw new Exception("There is no In Bulk endpoint");
        }
        if (endpointOutBulk == null) {
            LOG.error("There is no Out Bulk endpoint");
            throw new Exception("There is no Out Bulk endpoint");
        }

        // increase buffer for faster transfer
        UsbHacks.setMaxTransferRate(endpointInBulk, (short) (63 * 512));

        // open pipes
        for (UsbInterface i : interfaces) {
            i.claim();
        }
        pipeIn = endpointInBulk.getUsbPipe();
        pipeOut = endpointOutBulk.getUsbPipe();
        pipeIn.open();
        pipeOut.open();

        // check pipes status
        if (pipeIn.isActive() == false) {
            throw new Exception("Inactive In pipe");
        }
        if (pipeOut.isActive() == false) {
            throw new Exception("Inactive Out pipe");
        }

        // open session
        // The TransactionID used for the OpenSession operation shall be 0x00000000
        PTPPacket p = new PTPPacket(PTP.OPERATION_OpenSession, 1);
        this.sendPTPPacket(p);

        PTPPacket r = this.getResponse();
        if (r.getCommand() != PTP.USB_CONTAINER_RESPONSE || r.getOperationCode() != PTP.RESPONSE_CODE_OK) {
            LOG.error("Camera returns error on the OpenSession request");
            throw new Exception("Camera returns error on the OpenSession request");
        }
    }

    /**
     * Send packet
     */
    public void sendPTPPacket(PTPPacket p) throws Exception {
        // other command should use previous transaction ID
        if (p.getCommand() == PTP.USB_CONTAINER_COMMAND) {
            currentTransaction++;
        }
        p.setTransaction(currentTransaction);

        UsbIrp irp = pipeOut.createUsbIrp();
        send(pipeOut, irp, p.getFullPacket());
    }

    /**
     * Receive response packet
     */
    public PTPPacket getResponse() throws Exception {
        UsbIrp irp = pipeIn.createUsbIrp();
        send(pipeIn, irp, recbuf);

        // copy response into packet structure
        PTPPacket response = new PTPPacket(Arrays.copyOfRange(recbuf, 0, irp.getActualLength()));
        if (response.getTransaction() != currentTransaction) {
            throw new Exception("Wrong transaction. Expected: " + currentTransaction + ", received: "
                    + response.getTransaction());
        }
        return response;
    }

    /**
     * Send packet to USB device and wait for response.
     */
    private void send(UsbPipe pipe, UsbIrp irp, byte[] buffer) throws Exception {
        irp.setData(buffer);
        irp.setAcceptShortPacket(true);

        pipe.asyncSubmit(irp);
        irp.waitUntilComplete(RESPONSE_TIMEOUT);

        if (irp.isUsbException()) {
            throw irp.getUsbException();
        }

        if (irp.isComplete() == false) {
            throw new Exception("Camera Reply Timeout");
        }
    }

    /**
     * Close connection.
     */
    public void close() throws Exception {
        pipeIn.close();
        pipeOut.close();
        for (UsbInterface i : interfaces) {
            i.release();
        }
    }
}
