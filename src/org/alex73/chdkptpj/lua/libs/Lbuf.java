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

import java.lang.reflect.Method;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

public class Lbuf extends ALuaBaseLib {

    public Lbuf() {
        super("lbuf");
    }

    public VarArgFunction get_u8 = new VarArgFunction() {
        public Varargs invoke(Varargs args) {
            LuaTable table = new LuaTable();
            return table;
        }
    };

    public VarArgFunction constructor = new VarArgFunction() {
        public Varargs invoke(Varargs args) {
            LuaTable table = new LuaTable();
            return table;
        }
    };

    protected void registerCall(LuaTable table, String name, LuaFunction function) {
        // redefine 'new' name because we can't use it in java
        if ("constructor".equals(name)) {
            name = "new";
        }
        super.registerCall(table, name, function);
    }

    public static class LbufValue extends LuaTable {
        private final byte[] data;

        public LbufValue(byte[] data) {
            // super(data, new LuaTable());
            this.data = data;
            set("fwrite", fwrite);
        }

        public LuaValue call(LuaValue modname, LuaValue env) {
            return super.call(modname, env);
        }

        public TwoArgFunction fwrite = new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue thisObject, LuaValue file) {
                try {
                    LuaString buf = LuaString.valueOf(data);
                    Method m = file.getClass().getMethod("write", LuaString.class);
                    m.setAccessible(true);
                    m.invoke(file, buf);
                    return LuaValue.NONE;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }
            }
        };
    }
}
