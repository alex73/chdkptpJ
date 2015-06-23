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

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class ChdkLib extends ALuaBaseLib {
    public static final int PTP_CHDK_VERSION_MAJOR = 2;
    public static final int PTP_CHDK_VERSION_MINOR = 6;

    public static final int CHDKPTP_VERSION_MAJOR = 0;
    public static final int CHDKPTP_VERSION_MINOR = 5;

    public ChdkLib() {
        super("chdk");
    }

    public ZeroArgFunction host_api_version = new ZeroArgFunction() {
        public LuaValue call() {
            LuaTable table = new LuaTable();
            table.set("MAJOR", PTP_CHDK_VERSION_MAJOR);
            table.set("MINOR", PTP_CHDK_VERSION_MINOR);
            return table;
        }
    };
    public ZeroArgFunction program_version = new ZeroArgFunction() {
        public LuaValue call() {
            LuaTable table = new LuaTable();
            table.set("MAJOR", CHDKPTP_VERSION_MAJOR);
            table.set("MINOR", CHDKPTP_VERSION_MINOR);
            table.set("BUILD", 0);
            table.set("DESC", "java");
            table.set("DATE", 0);
            table.set("TIME", 0);
            table.set("COMPILER_VERSION", 0);
            return table;
        }
    };

    public ZeroArgFunction list_usb_devices = new ZeroArgFunction() {
        public LuaValue call() {
            LuaTable table = new LuaTable();
            table.set(1, LuaValue.valueOf("dev1"));
            return table;
        }
    };
    /**
     * chdk_connection=chdk.connection([devspec]) devspec={ bus="bus", dev="dev", } or devspec={ host="host",
     * port="port", } retreive or create the connection object for the specified device each unique bus/dev
     * combination has only one connection object. No attempt is made to verify that the device exists (it
     * might be plugged/unplugged later anyway) New connections start disconnected. An existing connection may
     * or may not be connected if devinfo is absent, the dummy connection is returned
     */
    public OneArgFunction connection = new OneArgFunction() {
        public LuaValue call(LuaValue arg) {
            LuaTable table = new LuaTable();
            table.set("connect", connect);
            return table;
            // return new Connection();
        }
    };
    /*
     * public ZeroArgFunction is_connected = new ZeroArgFunction() { public LuaValue call(LuaValue arg) {
     * return super.call(arg); }; public LuaValue call(LuaValue arg1, LuaValue arg2) { return
     * super.call(arg1,arg2); }; public LuaValue call() { return LuaValue.valueOf(false); } };
     */
    LibFunction connect = new LibFunction() {
        public Varargs invoke(Varargs args) {
            throw new RuntimeException();
        }
    };
}
