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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.usb.UsbDevice;

import org.alex73.chdkptpj.camera.lowlevel.CHDKScreenImage;
import org.alex73.chdkptpj.camera.lowlevel.PTP_CHDK;
import org.alex73.chdkptpj.camera.lowlevel.PTP_CHDK.PairValues;
import org.alex73.chdkptpj.lua.LuaUtils;
import org.alex73.chdkptpj.lua.ScriptError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Camera object. This is main object for make all operations with camera.
 */
public class Camera {
    private static Logger LOG = LoggerFactory.getLogger(Camera.class);

    /**
     * Buffer MUST BE bigger than greatest possible receive transfer. Otherwise, end of transfer will not be
     * able to read and tail of packet will be received as next packet. Need to modify libusb for fix it.
     * 
     * If you are going to receive raw photos, buffer must be bigger than raw photo size.
     * 
     * Constant is not final for software be able to modify it before connect to camera.
     */
    public int RECEIVE_BUFFER_SIZE = 64 * 1024 * 1024;

    private final UsbDevice device;
    private PTPConnection connection;
    private CHDKScripting scripting;

    private int captureSupportedFormats = -1;
    private int propset = -1;

    public Camera(UsbDevice device) {
        this.device = device;
    }

    public UsbDevice getDevice() {
        return device;
    }

    public PTPConnection getConnection() {
        return connection;
    }

    public CHDKScripting getScripting() {
        return scripting;
    }

    public boolean isConnected() {
        return connection != null;
    }

    /**
     * Connect to camera.
     */
    public void connect() throws Exception {
        LOG.info("Connecting to device " + device.getManufacturerString() + " : " + device.getProductString()
                + " : " + device.getSerialNumberString());
        connection = new PTPConnection(device, RECEIVE_BUFFER_SIZE);
        scripting = new CHDKScripting(this);
        LOG.info("Camera connected successfully");

        // retrieve camera info

        try {
            if ("function".equals(executeLua("return type(init_usb_capture)"))) {
                captureSupportedFormats = (int) executeLua("return get_usb_capture_support()");
            } else {
                captureSupportedFormats = 0;
            }
        } catch (Exception ex) {
            LOG.warn("Error retrieve captureSupportedFormats", ex);
            throw ex;
        }

        try {
            propset = scripting.get_propset();
        } catch (Exception ex) {
            LOG.warn("Error retrieve propset", ex);
            throw ex;
        }
    }

    /**
     * Disconnect from camera.
     */
    public void disconnect() throws Exception {
        LOG.info("Camera disconnecting");
        try {
            connection.close();
            connection = null;
        } catch (Exception ex) {
            LOG.error("Close camera connection error", ex);
            throw ex;
        }
        LOG.info("Camera disconnected");
    }

    /**
     * http://chdk.wikia.com/wiki/Lua/Lua_Reference#get_mode
     */
    public boolean isInRecordMode() throws Exception {
        return (boolean) executeLuaMultiresult("return get_mode();").get(0);
    }

    /**
     * http://chdk.wikia.com/wiki/Script_commands#set_record.28state.29
     */
    public void setRecordMode() throws Exception {
        if (isInRecordMode()) {
            return;
        }

        executeLua("set_record(1);");

        // wait for real mode change
        while (!isInRecordMode()) {
            Thread.sleep(10);
        }
    }

    public BufferedImage getPreview() throws Exception {
        byte[] previewBytes = PTP_CHDK.get_live_data(this, PTP.LV_TFR_VIEWPORT);
        return new CHDKScreenImage(previewBytes).decodeViewport();
    }

    /**
     * Run Lua command on camera and don't wait for finish.
     */
    public int runLua(String command) throws Exception {
        if (LOG.isTraceEnabled()) {
            String sc = command;
            if (sc.length() > 60) {
                sc = sc.substring(0, 40);
            }
            LOG.trace(">> runLua: " + sc.replace("\n", "\\n").replace("\r", "\\r"));
        }
        PairValues result = PTP_CHDK.exec_lua(this, command, 0);
        return result.v1;
    }

    /**
     * Execute Lua command on camera and wait for result.
     */
    public List<Object> executeLuaMultiresult(String command) throws Exception {
        int scriptId = runLua(command);

        while (true) {
            byte status = PTP_CHDK.get_script_status(this);
            if (status != 1) {
                break;
            }
            // still running
            Thread.sleep(50);
        }

        List<Object> result = new ArrayList<>();
        while (true) {
            PTP_CHDK.script_msg msg = PTP_CHDK.read_script_msg(this);
            if (msg.script_id == 0) {
                break;
            }
            if (msg.script_id != scriptId) {
                LOG.error("Response from other script. Expected " + scriptId + " but received "
                        + msg.script_id + ". Possible thread-safe issue");
                throw new Exception("Response from other script. Expected " + scriptId + " but received "
                        + msg.script_id + ". Possible thread-safe issue");
            }

            Object obj;
            switch (msg.type) {
            case PTP_CHDK.PTP_CHDK_S_MSGTYPE_RET:
            case PTP_CHDK.PTP_CHDK_S_MSGTYPE_USER:
                obj = LuaUtils.deserializeLuaObject(msg);
                break;
            default:
                obj = new ScriptError("ERROR: " + PTP_CHDK.script_msg_error_type_to_name(msg.subtype) + " "
                        + new String(msg.data, "UTF-8"));
                break;
            }
            result.add(obj);
        }

        return result;
    }

    public Object executeLua(String command) throws Exception {
        List<Object> r = executeLuaMultiresult(command);
        if (r.isEmpty()) {
            return Void.TYPE;
        }
        if (r.size() > 1) {
            throw new Exception("Too many results from Lua: " + r);
        }
        if (r.get(0) instanceof ScriptError) {
            throw (ScriptError) r.get(0);
        } else {
            return r.get(0);
        }
    }

    public int getPropset() {
        return propset;
    }

    public int getCaptureSupportedFormats() {
        return captureSupportedFormats;
    }
}
