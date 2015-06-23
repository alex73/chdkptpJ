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

/**
 * Packet for sending/receiving to/from camera.
 * 
 * All numbers encoded as little endian.
 * 
 * TODO use ByteBuffer
 */
public class PTPPacket {
    public static final int OFFSET_LENGTH = 0;
    public static final int COMMAND_OFFSET = 4;
    public static final int OPERATION_CODE_OFFSET = 6;

    public static final int TRANSACTION_OFFSET = 8;
    public static final int DATA_OFFSET = 12;
    public static final int HEADER_SIZE = 12;

    private final byte[] packetBytes;

    /**
     * The operation request phase consists of the transport-specific transmission of a 30-byte operation
     * dataset from the Initiator to the Responder.
     */
    public PTPPacket(short opcode, int... args) {
        this(new byte[HEADER_SIZE + 4 * 5]);

        this.setInteger(OFFSET_LENGTH, packetBytes.length);
        setCommand(PTP.USB_CONTAINER_COMMAND);
        setOperationCode(opcode);

        if (args.length > 5) {
            throw new IllegalArgumentException("Too many parameters for USB packet");
        }

        /**
         * The interpretation of any parameter is dependent upon the OperationCode. Any unused parameter
         * fields should be set to 0x00000000. If a parameter holds a value that is less than 32 bits, the
         * lowest significant bits shall be used to store the value, with the most significant bits being set
         * to zeros. (i.e. little-endian)
         */
        for (int i = 0, offset = DATA_OFFSET; i < args.length; i++, offset += 4) {
            setInteger(offset, args[i]);
        }
    }

    /**
     * The data phase is an optional phase that is used to transmit data that is larger than what can fit in
     * the operation or response datasets
     */
    public PTPPacket(short opcode, byte[] payload) {
        this(new byte[HEADER_SIZE + payload.length]);

        this.setInteger(OFFSET_LENGTH, packetBytes.length);
        setCommand(PTP.USB_CONTAINER_DATA);
        setOperationCode(opcode);
        setData(payload);
    }

    public PTPPacket(byte[] packet) {
        this.packetBytes = packet;
    }

    public byte[] getFullPacket() {
        return packetBytes;
    }

    public short getCommand() {
        return this.getShort(COMMAND_OFFSET);
    }

    public void setCommand(short v) {
        setShort(COMMAND_OFFSET, v);
    }

    public short getOperationCode() {
        return this.getShort(OPERATION_CODE_OFFSET);
    }

    public void setOperationCode(short v) {
        setShort(OPERATION_CODE_OFFSET, v);
    }

    public int getTransaction() {
        return this.getInteger(TRANSACTION_OFFSET);
    }

    public void setTransaction(int transaction) {
        this.setInteger(TRANSACTION_OFFSET, transaction);
    }

    public int getParam(int index) {
        int offset = DATA_OFFSET + index * 4;
        return getInteger(offset);
    }

    public byte[] getData() {
        return Arrays.copyOfRange(packetBytes, HEADER_SIZE, packetBytes.length);
    }

    public void setData(byte[] data) {
        System.arraycopy(data, 0, packetBytes, HEADER_SIZE, data.length);
    }

    private short getShort(int pos) {
        int b1 = 0x000000ff & packetBytes[pos];
        int b2 = 0x000000ff & packetBytes[pos + 1];
        return (short) ((b2 << 8) | (b1 << 0));
    }

    private void setShort(int pos, short v) {
        packetBytes[pos] = (byte) (v);
        packetBytes[pos + 1] = (byte) (v >> 8);
    }

    private int getInteger(int pos) {
        int b1 = 0x000000ff & packetBytes[pos];
        int b2 = 0x000000ff & packetBytes[pos + 1];
        int b3 = 0x000000ff & packetBytes[pos + 2];
        int b4 = 0x000000ff & packetBytes[pos + 3];
        return ((b4 << 24) | (b3 << 16) | (b2 << 8) | (b1 << 0));
    }

    private void setInteger(int pos, int v) {
        packetBytes[pos] = (byte) (v);
        packetBytes[pos + 1] = (byte) (v >> 8);
        packetBytes[pos + 2] = (byte) (v >> 16);
        packetBytes[pos + 3] = (byte) (v >> 24);
    }
}
