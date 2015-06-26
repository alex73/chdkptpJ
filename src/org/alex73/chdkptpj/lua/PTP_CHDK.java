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
package org.alex73.chdkptpj.lua;

import org.alex73.chdkptpj.camera.Camera;
import org.alex73.chdkptpj.camera.PTP;
import org.alex73.chdkptpj.camera.PTPPacket;

/**
 * PTP CHDK calls.
 */
public class PTP_CHDK {

    public static final int PTP_CHDK_SL_LUA = 0;

    /**
     * ptp.h#978 (svn rev. 667)
     * 
     * the following happens to match what is used in CHDK, but is not part of the protocol
     */
    public static class ptp_chdk_script_msg {
        public int size;
        public int script_id; // id of script message is to/from
        public int type;
        public int subtype;
        public byte data[];
    };

    /**
     * ptp.h#989 (svn rev. 667)
     * 
     * chunk for remote capture
     */
    public static class ptp_chdk_rc_chunk {
        public int size; // length of data
        public boolean last; // is it the last chunk?
        public int offset; // offset within file, or -1
        public byte[] data; // data, must be free'd by caller when done
    }

    /**
     * ptp.h#170 (svn rev. 667)
     * 
     * enum ptp_chdk_script_msg_type
     */
    public static final int PTP_CHDK_S_MSGTYPE_NONE = 0; // no messages waiting
    public static final int PTP_CHDK_S_MSGTYPE_ERR = 1; // error message
    public static final int PTP_CHDK_S_MSGTYPE_RET = 2; // script return value
    public static final int PTP_CHDK_S_MSGTYPE_USER = 3; // message queued by script

    /**
     * ptp.h#98 (svn rev. 667)
     * 
     * enum ptp_chdk_script_data_type
     */
    public static final int PTP_CHDK_TYPE_UNSUPPORTED = 0; // type name will be returned in data
    public static final int PTP_CHDK_TYPE_NIL = 1;
    public static final int PTP_CHDK_TYPE_BOOLEAN = 2;
    public static final int PTP_CHDK_TYPE_INTEGER = 3;
    /* Empty strings are returned with length=0 */
    public static final int PTP_CHDK_TYPE_STRING = 4;
    /*
     * tables are converted to a string by usb_msg_table_to_string, this function can be overridden in lua to
     * change the format the string may be empty for an empty table
     */
    public static final int PTP_CHDK_TYPE_TABLE = 5;

    /**
     * ptp.h#188 (svn rev. 667)
     */
    enum ptp_chdk_script_msg_status {
        PTP_CHDK_S_MSGSTATUS_OK(0), // queued ok
        PTP_CHDK_S_MSGSTATUS_NOTRUN(1), // no script is running
        PTP_CHDK_S_MSGSTATUS_QFULL(2), // queue is full
        PTP_CHDK_S_MSGSTATUS_BADID(3); // specified ID is not running
        public final int id;

        private ptp_chdk_script_msg_status(int id) {
            this.id = id;
        }
    }

    public static class PairValues {
        public final int v1;
        public final int v2;

        public PairValues(int v1, int v2) {
            this.v1 = v1;
            this.v2 = v2;
        }
    }

    /**
     * ptp.c#2273 (svn rev. 667)
     * 
     * uint16_t ptp_chdk_get_script_status(PTPParams* params, unsigned *status)
     */
    public static byte ptp_chdk_get_script_status(Camera camera) throws Exception {
        // send script status command
        PTPPacket p = new PTPPacket(PTP.OPERATION_CHDK, PTP.CHDK_ScriptStatus);
        camera.getConnection().sendPTPPacket(p);

        // get response
        PTPPacket r = camera.getConnection().getResponse();
        checkResponsePacket(r);

        return (byte) r.getParam(0);
    }

    /**
     * ptp.c#2235 (svn rev. 667)
     * 
     * uint16_t ptp_chdk_exec_lua(PTPParams* params, char *script, int flags, int *script_id, int *status)
     */
    public static PairValues ptp_chdk_exec_lua(Camera camera, String script, int flags) throws Exception {
        // send script command
        PTPPacket p1 = new PTPPacket(PTP.OPERATION_CHDK, PTP.CHDK_ExecuteScript, PTP_CHDK_SL_LUA | flags);
        camera.getConnection().sendPTPPacket(p1);

        // prepare script
        StringBuilder preparedScript = new StringBuilder(script);
        if (preparedScript.charAt(preparedScript.length() - 1) != ';') {
            preparedScript.append(';');
        }
        preparedScript.append("\0");
        // send script text
        PTPPacket p2 = new PTPPacket(PTP.OPERATION_CHDK, preparedScript.toString().getBytes("UTF-8"));
        camera.getConnection().sendPTPPacket(p2);

        // get response
        PTPPacket pr = camera.getConnection().getResponse();
        checkResponsePacket(pr);

        return new PairValues(pr.getParam(0), pr.getParam(1));
    }

    /**
     * ptp.c#2259 (svn rev. 667)
     * 
     * uint16_t ptp_chdk_get_version(PTPParams* params, int *major, int *minor)
     */
    public static PairValues ptp_chdk_get_version(Camera camera) throws Exception {
        // send version command
        PTPPacket p = new PTPPacket(PTP.OPERATION_CHDK, PTP.CHDK_Version);
        camera.getConnection().sendPTPPacket(p);

        // get response
        PTPPacket r = camera.getConnection().getResponse();
        checkResponsePacket(r);

        return new PairValues(r.getParam(0), r.getParam(1));
    }

    /**
     * ptp.c#2324 (svn rev. 667)
     * 
     * uint16_t ptp_chdk_read_script_msg(PTPParams* params, ptp_chdk_script_msg **msg)
     */
    public static ptp_chdk_script_msg ptp_chdk_read_script_msg(Camera camera) throws Exception {
        // send read script message command
        PTPPacket p = new PTPPacket(PTP.OPERATION_CHDK, PTP.CHDK_ReadScriptMsg);
        camera.getConnection().sendPTPPacket(p);

        // get response
        PTPPacket r1 = camera.getConnection().getResponse();
        checkResponsePacket(r1, PTP.USB_CONTAINER_DATA, PTP.OPERATION_CHDK);
        PTPPacket r2 = camera.getConnection().getResponse();
        checkResponsePacket(r2);

        ptp_chdk_script_msg result = new ptp_chdk_script_msg();
        result.type = r2.getParam(0);
        result.subtype = r2.getParam(1);
        result.script_id = r2.getParam(2);
        result.size = r2.getParam(3);
        result.data = r1.getData();
        return result;
    }

    /**
     * ptp.c#2195 (svn rev. 667)
     * 
     * isready: 0: not ready, lowest 2 bits: available image formats, 0x10000000: error
     * 
     * uint16_t ptp_chdk_rcisready(PTPParams* params, int *isready,int *imgnum)
     */
    public static PairValues ptp_chdk_rcisready(Camera camera) throws Exception {
        // send check ready command
        PTPPacket p = new PTPPacket(PTP.OPERATION_CHDK, PTP.CHDK_RemoteCaptureIsReady);
        camera.getConnection().sendPTPPacket(p);

        // get response
        PTPPacket r = camera.getConnection().getResponse();
        checkResponsePacket(r);

        return new PairValues(r.getParam(0), r.getParam(1));
    }

    /**
     * ptp.c#2211 (svn rev. 667)
     * 
     * uint16_t ptp_chdk_rcgetchunk(PTPParams* params, int fmt, ptp_chdk_rc_chunk *chunk)
     */
    public static ptp_chdk_rc_chunk ptp_chdk_rcgetchunk(Camera camera, int fmt) throws Exception {
        { // send get chunk command
            PTPPacket p = new PTPPacket(PTP.OPERATION_CHDK, PTP.CHDK_RemoteCaptureGetData, fmt);
            camera.getConnection().sendPTPPacket(p);
        }

        ptp_chdk_rc_chunk result = new ptp_chdk_rc_chunk();
        { // get response
            PTPPacket rdata = camera.getConnection().getResponse();
            checkResponsePacket(rdata, PTP.USB_CONTAINER_DATA, PTP.OPERATION_CHDK);

            PTPPacket rinfo = camera.getConnection().getResponse();
            checkResponsePacket(rinfo);

            result.size = rinfo.getParam(0);
            result.last = rinfo.getParam(1) == 0;
            result.offset = rinfo.getParam(2);
            result.data = rdata.getData();
        }
        return result;
    }

    /**
     * prp.c#2355 (svn rev. 667)
     * 
     * uint16_t ptp_chdk_get_live_data(PTPParams* params, unsigned flags,char **data,unsigned *data_size) {
     */
    public static byte[] ptp_chdk_get_live_data(Camera camera, int flags) throws Exception {
        { // send get display data command
            PTPPacket p = new PTPPacket(PTP.OPERATION_CHDK, PTP.CHDK_GetDisplayData, flags);
            camera.getConnection().sendPTPPacket(p);
        }

        byte[] data;
        int size;
        { // get response
            PTPPacket rdata = camera.getConnection().getResponse();
            checkResponsePacket(rdata, PTP.USB_CONTAINER_DATA, PTP.OPERATION_CHDK);

            PTPPacket rinfo = camera.getConnection().getResponse();
            checkResponsePacket(rinfo);

            data = rdata.getData();
            size = rinfo.getParam(0);
            if (data.length != size) {
                throw new Exception("Wrong data");
            }
            return data;
        }
    }

    static final String[] script_msg_types = { "none", "error", "return", "user" };

    /**
     * chdkptp.c#1628 (svn rev. 667)
     * 
     * these assume numbers are 0 based and contiguous
     * 
     * static const char* script_msg_type_to_name(unsigned type_id) {
     */
    public static String script_msg_type_to_name(int type_id) {
        if (type_id >= script_msg_types.length) {
            return "unknown_msg_type";
        }
        return script_msg_types[type_id];
    }

    static final String[] script_msg_data_type = { "unsupported", "nil", "boolean", "integer", "string",
            "table" };

    /**
     * chdkptp.c#1636 (svn rev. 667)
     * 
     * static const char* script_msg_data_type_to_name(unsigned type_id) {
     */
    public static String script_msg_data_type_to_name(int type_id) {
        if (type_id >= script_msg_data_type.length) {
            return "unknown_msg_subtype";
        }
        return script_msg_data_type[type_id];
    }

    static final String[] script_msg_error_type = { "none", "compile", "runtime" };

    /**
     * chdkptp.c#1644 (svn rev. 667)
     * 
     * static const char* script_msg_error_type_to_name(unsigned type_id) {
     */
    public static String script_msg_error_type_to_name(int type_id) {
        if (type_id >= script_msg_error_type.length) {
            return "unknown_error_subtype";
        }
        return script_msg_error_type[type_id];
    }

    private static void checkResponsePacket(PTPPacket p) throws Exception {
        checkResponsePacket(p, PTP.USB_CONTAINER_RESPONSE, PTP.RESPONSE_CODE_OK);
    }

    private static void checkResponsePacket(PTPPacket p, short requiredCommand, short requiredOppCode)
            throws Exception {
        if (p.getCommand() != requiredCommand) {
            throw new Exception("Wrong response packet(expected command is " + requiredCommand
                    + ", but exist " + p.getCommand() + "): " + p);
        }
        if (p.getOperationCode() != requiredOppCode) {
            throw new Exception("Wrong response packet(expected oppcode is " + requiredOppCode
                    + ", but exist " + p.getOperationCode() + "): " + p);
        }
    }
}
