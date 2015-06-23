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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

/**
 * Some utilities for Lua objects.
 */
public class LuaUtils {
    public static String dumpTable(LuaTable table) {
        Map<String, String> result = new TreeMap<>();
        LuaValue k = LuaValue.NIL;
        while (true) {
            Varargs n = table.next(k);
            if ((k = n.arg1()).isnil())
                break;
            LuaValue v = n.arg(2);
            result.put(k.toString(), v.toString());
        }

        return result.toString();
    }

    public static Object deserializeLuaObject(PTP_CHDK.ptp_chdk_script_msg msg) throws Exception {
        switch (msg.subtype) {
        case PTP_CHDK.PTP_CHDK_TYPE_UNSUPPORTED:
        case PTP_CHDK.PTP_CHDK_TYPE_STRING:
        case PTP_CHDK.PTP_CHDK_TYPE_TABLE:
            return new String(msg.data, "UTF-8");
        case PTP_CHDK.PTP_CHDK_TYPE_BOOLEAN:
            return msg.data[0] == 1;
        case PTP_CHDK.PTP_CHDK_TYPE_INTEGER:
            ByteBuffer buffer = ByteBuffer.wrap(msg.data);
            buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            return buffer.getInt();
        default:
            return null;
        }
    }

    public static LuaValue toLuaValue(Object obj) {
        if (obj == null) {
            return LuaValue.NIL;
        }
        if (obj instanceof String) {
            return LuaValue.valueOf((String) obj);
        } else if (obj instanceof Integer) {
            return LuaValue.valueOf((Integer) obj);
        } else if (obj instanceof Boolean) {
            return LuaValue.valueOf((Boolean) obj);
        } else {
            return LuaValue.NIL;
        }
    }
}
