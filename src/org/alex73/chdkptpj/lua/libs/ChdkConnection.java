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
package org.alex73.chdkptpj.lua.libs;

import java.util.logging.Level;

import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.util.UsbUtil;

import org.alex73.chdkptpj.camera.Camera;
import org.alex73.chdkptpj.lua.LuaUtils;
import org.alex73.chdkptpj.lua.PTP_CHDK;
import org.alex73.chdkptpj.lua.PTP_CHDK.PairValues;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.usb4java.javax.UsbHacks;

import com.sun.istack.internal.logging.Logger;

public class ChdkConnection extends ALuaBaseLib {
    public static final int PTP_CHDK_SCRIPT_STATUS_RUN = 1;
    public static final int PTP_CHDK_SCRIPT_STATUS_MSG = 2;

    public static final int PTP_CHDK_S_ERRTYPE_NONE = 0;
    public static final int PTP_CHDK_S_ERRTYPE_COMPILE = 1;
    public static final int PTP_CHDK_S_ERRTYPE_RUN = 2;
    // the following are for ExecuteScript status only, not message types
    public static final int PTP_CHDK_S_ERR_SCRIPTRUNNING = 0x1000; // script already running with NOKILL

    private static Logger LOG = Logger.getLogger(ChdkConnection.class);

    private Camera camera;
    private boolean connected;
    private long write_count, read_count;
    private Integer currentScriptId;

    public ChdkConnection(Camera camera) {
        super("chdk_connection");
        this.camera = camera;
    }

    /**
     * chdkptp.c#1266 (svn rev. 667)
     * 
     * static int chdk_is_connected(lua_State *L) {
     */
    public ZeroArgFunction is_connected = new ZeroArgFunction() {
        public LuaValue call() {
            LOG.fine("is_connected: " + connected);
            return LuaValue.valueOf(connected);
        }
    };

    /**
     * chdkptp.c#1240 (svn rev. 667)
     * 
     * con:connect()
     * 
     * static int chdk_connect(lua_State *L) {
     */
    public VarArgFunction connect = new VarArgFunction() {
        public Varargs invoke(Varargs args) {
            LOG.fine("connect");
            connected = true;
            return LuaValue.NONE;
        }
    };

    /**
     * chdkptp.c#1259 (svn rev. 667)
     * 
     * disconnect the connection
     * 
     * note under windows the device does not appear in in chdk.list_usb_devices() for a short time after
     * disconnecting
     * 
     * static int chdk_disconnect(lua_State *L) {
     */
    public VarArgFunction disconnect = new VarArgFunction() {
        public Varargs invoke(Varargs args) {
            LOG.fine("disconnect");
            connected = false;
            return LuaValue.NONE;
        }
    };

    /**
     * chdkptp.c#1914 (svn rev. 667)
     * 
     * static int chdk_reset_counters(lua_State *L) {
     */
    public ZeroArgFunction reset_counters = new ZeroArgFunction() {
        public LuaValue call() {
            LOG.fine("reset_counters");
            write_count = 0;
            read_count = 0;
            return LuaValue.NONE;
        }
    };

    /**
     * chdkptp.c#1834 (svn rev. 667)
     * 
     * lua code expects all to devices to have devinfo
     * 
     * dev_info=con:get_con_devinfo()
     *
     * dev_info = { <br/>
     * transport="usb"|"ip" <br/>
     * -- usb <br/>
     * bus="bus" <br/>
     * dev="dev" <br/>
     * "vendor_id" = VENDORID, -- nil if no matching PTP capable device is connected <br/>
     * "product_id" = PRODUCTID, -- nil if no matching PTP capable device is connected <br/>
     * -- ip <br/>
     * host="host" -- host specified in chdk.connection <br/>
     * port="port" <br/>
     * guid="guid" -- binary 16 byte GUID from cam <br/>
     * }
     * 
     * static int chdk_get_con_devinfo(lua_State *L) {
     */
    public ZeroArgFunction get_con_devinfo = new ZeroArgFunction() {
        public LuaValue call() {
            LOG.fine(">> get_con_devinfo");

            UsbHacks.UsbDeviceId id = UsbHacks.getDeviceId(camera.getDevice());
            UsbDeviceDescriptor desc = camera.getDevice().getUsbDeviceDescriptor();
            LuaTable table = new LuaTable();
            table.set("transport", "usb");
            table.set("bus", Integer.toHexString(id.getBusNumber()));
            table.set("dev", Integer.toHexString(id.getDeviceAddress()));
            table.set("port", Integer.toHexString(id.getPortNumber()));
            table.set("vendor_id", hex4(desc.idVendor()));
            table.set("product_id", hex4(desc.idProduct()));

            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("<< get_con_devinfo: " + LuaUtils.dumpTable(table));
            }
            return table;
        }
    };

    /**
     * chdkptp.c#1795 (svn rev. 667)
     * 
     * ptp_dev_info=con:get_ptp_devinfo()
     * 
     * ptp_dev_info = { <br/>
     * manufacturer = "manufacturer" <br/>
     * model = "model" <br/>
     * device_version = "version"" <br/>
     * serial_number = "serialnum" <br/>
     * max_packet_size = number <br/>
     * } <br/>
     * more fields may be added later <br/>
     * serial number may be NULL (=unset in table) <br/>
     * version does not match canon firmware version (e.g. d10 100a = "1-6.0.1.0")
     * 
     * static int chdk_get_ptp_devinfo(lua_State *L) {
     */
    public ZeroArgFunction get_ptp_devinfo = new ZeroArgFunction() {
        public LuaValue call() {
            LOG.fine("get_ptp_devinfo");

            LuaTable table = new LuaTable();
            try {
                UsbDevice dev = camera.getDevice();
                UsbDeviceDescriptor desc = camera.getDevice().getUsbDeviceDescriptor();
                table.set("manufacturer", dev.getManufacturerString());
                table.set("model", dev.getProductString());
                table.set("serial_number", dev.getSerialNumberString());
                table.set("device_version", UsbUtil.unsignedInt(desc.bcdDevice()));
                table.set("max_packet_size", UsbUtil.unsignedInt(desc.bMaxPacketSize0()));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            return table;
        }
    };

    /**
     * chdkptp.c#1284 (svn rev. 667)
     * 
     * // major, minor = chdk.camera_api_version()<br/>
     * double return is annoying<br/>
     * we could just get this when we connect<br/>
     * 
     * static int chdk_camera_api_version(lua_State *L) {
     */
    public LibFunction camera_api_version_pcall = new LibFunction() {
        public Varargs invoke(Varargs varargs) {
            LOG.fine(">> camera_api_version_pcall");

            try {
                PairValues version = PTP_CHDK.ptp_chdk_get_version(camera);
                // TODO I don't understand why is 3 parameters need instead 2
                Varargs result = LuaValue.varargsOf(LuaValue.TRUE, LuaValue.valueOf(version.v1),
                        LuaValue.valueOf(version.v2));
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("<< camera_api_version_pcall: " + result);
                }
                return result;
            } catch (Exception ex) {
                LOG.warning("Error in camera_api_version_pcall", ex);
                throw new RuntimeException(ex);
            }
        };
    };

    /**
     * chdkptp.c#1576 (svn rev. 667)
     * 
     * status=con:script_status() <br/>
     * status={run:bool,msg:bool} or throws error
     * 
     * static int chdk_script_status(lua_State *L) {
     */
    public ZeroArgFunction script_status = new ZeroArgFunction() {
        public LuaValue call() {
            LOG.fine(">> script_status");

            try {
                byte status = PTP_CHDK.ptp_chdk_get_script_status(camera);
                LuaTable table = new LuaTable();
                table.set("run", LuaValue.valueOf((status & PTP_CHDK_SCRIPT_STATUS_RUN) != 0));
                table.set("msg", LuaValue.valueOf((status & PTP_CHDK_SCRIPT_STATUS_MSG) != 0));

                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("<< script_status: " + LuaUtils.dumpTable(table));
                }
                return table;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    };

    /**
     * chdkptp.c#1339 (svn rev. 667)
     * 
     * con:execlua("code"[,flags]) <br/>
     * flags: PTP_CHDK_SCRIPT_FL* values. <br/>
     * no return value, throws error on failure <br/>
     * on compile error, thrown etype='execlua_compile' <br/>
     * on script running, thrown etype='execlua_scriptrun' <br/>
     * con:get_script_id() will return the id of the started script <br/>
     * 
     * static int chdk_execlua(lua_State *L) {
     * 
     * TODO I don't understand why is 3 parameters received instead 2
     */
    public ThreeArgFunction execlua = new ThreeArgFunction() {
        @Override
        public LuaValue call(LuaValue table, LuaValue code, LuaValue flags) {
            LOG.fine("execlua");

            PairValues scriptDesc;
            try {
                scriptDesc = PTP_CHDK.ptp_chdk_exec_lua(camera, code.tojstring(), flags.toint());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            switch (scriptDesc.v2) {
            case PTP_CHDK_S_ERRTYPE_NONE:
                currentScriptId = scriptDesc.v1;
                return LuaValue.NONE;
            case PTP_CHDK_S_ERRTYPE_COMPILE:
                throw new RuntimeException("script compile error");
            case PTP_CHDK_S_ERR_SCRIPTRUNNING:
                throw new RuntimeException("a script is already running");
            default:
                throw new RuntimeException("unknown error");
            }
        }
    };

    /**
     * chdkptp.c#1667 (svn rev. 667)
     * 
     * msg=con:read_msg()
     * 
     * msg:{ <br/>
     * value=<val> -- lua value, tables are serialized strings <br/>
     * script_id=number <br/>
     * mtype=string -- one of "none","error","return","user" <br/>
     * msubtype=string -- for returns and user messages, one of <br/>
     * -- "unsupported","nil","boolean","integer","string","table" <br/>
     * -- for errors, one of "compile","runtime" <br/>
     * } <br/>
     * no message: type is set to 'none' <br/>
     * throws error on error <br/>
     * use chdku con:wait_status or chdku con:wait_msg to wait for messages
     * 
     * static int chdk_read_msg(lua_State *L) {
     */
    public ThreeArgFunction read_msg = new ThreeArgFunction() {
        @Override
        public LuaValue call(LuaValue unknown, LuaValue code, LuaValue flags) {
            LOG.fine(">> read_msg");

            try {
                PTP_CHDK.ptp_chdk_script_msg msg = PTP_CHDK.ptp_chdk_read_script_msg(camera);

                LuaTable table = new LuaTable();
                table.set("script_id", msg.script_id);
                table.set("type", PTP_CHDK.script_msg_type_to_name(msg.type));
                switch (msg.type) {
                case PTP_CHDK.PTP_CHDK_S_MSGTYPE_RET:
                case PTP_CHDK.PTP_CHDK_S_MSGTYPE_USER:
                    table.set("subtype", PTP_CHDK.script_msg_data_type_to_name(msg.subtype));
                    Object data = LuaUtils.deserializeLuaObject(msg);
                    table.set("value", LuaUtils.toLuaValue(data));
                    break;
                case PTP_CHDK.PTP_CHDK_S_MSGTYPE_ERR:
                    table.set("subtype", PTP_CHDK.script_msg_error_type_to_name(msg.subtype));
                    table.set("value", LuaValue.valueOf(new String(msg.data, "UTF-8")));
                    break;
                default:
                    table.set("subtype", PTP_CHDK.script_msg_data_type_to_name(msg.subtype));
                    table.set("value", LuaValue.NIL);
                    break;
                }

                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("<< read_msg: " + LuaUtils.dumpTable(table));
                }
                return table;
            } catch (Exception ex) {
                LOG.warning("read_msg error", ex);
                throw new RuntimeException(ex);
            }
        }
    };

    /**
     * chdkptp.c#1757 (svn rev. 667)
     * 
     * (script_id|false) = con:get_script_id()
     * 
     * returns the id of the most recently started script <br/>
     * script ids start at 1, and will be reset if the camera reboots <br/>
     * script id will be false if the last script request failed to reach the camera or no script has yet been
     * run <br/>
     * scripts that encounter a syntax error still generate an id <br/>
     * 
     * static int chdk_get_script_id(lua_State *L) {
     */
    public ZeroArgFunction get_script_id = new ZeroArgFunction() {
        @Override
        public LuaValue call() {
            LOG.fine("get_script_id: " + currentScriptId);
            return currentScriptId != null ? LuaValue.valueOf(currentScriptId.intValue()) : LuaValue.FALSE;
        }
    };

    /**
     * chdkptp.c#1446 (svn rev. 667)
     * 
     * isready,imgnum=con:capture_ready()
     * 
     * isready: <br/>
     * false: local error in errmsg <br/>
     * 0: not ready <br/>
     * 0x10000000: remotecap not initialized, or timed out <br/>
     * otherwise, lowest 3 bits: available data types. <br/>
     * imgnum: <br/>
     * image number if data is available, otherwise 0
     * 
     * static int chdk_capture_ready(lua_State *L) {
     */
    public LibFunction capture_ready = new LibFunction() {
        @Override
        public Varargs invoke(Varargs varargs) {
            LOG.fine(">> capture_ready");
            try {
                PairValues isready = PTP_CHDK.ptp_chdk_rcisready(camera);

                Varargs result = LuaValue.varargsOf(LuaValue.valueOf(isready.v1),
                        LuaValue.valueOf(isready.v2));
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("<< capture_ready: " + result);
                }
                return result;
            } catch (Exception ex) {
                LOG.warning("capture_ready error", ex);
                throw new RuntimeException(ex);
            }
        }
    };

    /**
     * chdkptp.c#1472 (svn rev. 667)
     * 
     * chunk=con:capture_get_chunk(fmt)
     * 
     * fmt: data type (1: jpeg, 2: raw, 4:dng header) <br/>
     * must be a single type reported as available by con:capture_ready() <br/>
     * chunk: <br/>
     * { <br/>
     * size=number, <br/>
     * offset=number|nil, <br/>
     * last=bool <br/>
     * data=lbuf <br/>
     * } <br/>
     * throws error on error <br/>
     * 
     * static int chdk_capture_get_chunk(lua_State *L) {
     * 
     * TODO also wrong parameters count
     */
    public VarArgFunction capture_get_chunk_pcall = new VarArgFunction() {
        @Override
        public Varargs invoke(Varargs args) {
            LuaValue fmt = args.arg(2);

            long timeStart = 0;
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine(">> capture_get_chunk_pcall " + fmt);
                timeStart = System.currentTimeMillis();
            }
            try {
                PTP_CHDK.ptp_chdk_rc_chunk chunk = PTP_CHDK.ptp_chdk_rcgetchunk(camera, fmt.toint());

                LuaTable result = new LuaTable();
                result.set("size", chunk.size);
                if (chunk.offset != -1) {
                    result.set("offset", chunk.offset);
                }
                result.set("last", LuaValue.valueOf(chunk.last));
                result.set("data", new Lbuf.LbufValue(chunk.data));

                if (LOG.isLoggable(Level.FINE)) {
                    long timeEnd = System.currentTimeMillis();
                    LOG.fine("<< capture_get_chunk_pcall: " + LuaUtils.dumpTable(result) + " ("
                            + (timeEnd - timeStart) + " ms)");
                }

                return LuaValue.varargsOf(LuaValue.TRUE, result);
            } catch (Exception ex) {
                LOG.warning("capture_get_chunk_pcall error", ex);
                throw new RuntimeException(ex);
            }
        }
    };

    static String hex4(short v) {
        String r = Integer.toHexString(UsbUtil.unsignedInt(v));
        return "0000".substring(r.length()) + r;
    }
}
