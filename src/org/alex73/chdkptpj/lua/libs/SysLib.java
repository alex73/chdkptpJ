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

import org.alex73.chdkptpj.lua.LuaUtils;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class SysLib extends ALuaBaseLib {
    public SysLib() {
        super("sys");
    }

    private static String OS = System.getProperty("os.name").toLowerCase();

    public ZeroArgFunction ostype = new ZeroArgFunction() {
        public LuaValue call() {
            if (OS.indexOf("win") >= 0) {
                return LuaString.valueOf("Windows");
            } else if (OS.indexOf("mac") >= 0) {
                return LuaString.valueOf("Mac");
            } else {
                return LuaString.valueOf("Linux");
            }
        }
    };

    public ZeroArgFunction getargs = new ZeroArgFunction() {
        public LuaValue call() {
            LuaTable table = new LuaTable();
            table.set(1, LuaValue.valueOf("-c"));
            table.set(2, LuaValue.valueOf("-ers /tmp/1/ffff -raw -jpg"));
            return table;
        }
    };

    public LibFunction gettimeofday = new LibFunction() {
        public Varargs invoke(Varargs args) {
            // TODO is it right ?
            return LuaValue.varargsOf(LuaValue.valueOf(System.currentTimeMillis()), LuaValue.valueOf(0));
        }
    };

    public OneArgFunction getenv = new OneArgFunction() {
        @Override
        public LuaValue call(LuaValue arg) {
            String key = ((LuaString) arg).tojstring();
            String value = System.getenv(key);
            return value != null ? LuaValue.valueOf(value) : LuaValue.NONE;
        }
    };

    public TwoArgFunction v = new TwoArgFunction() {
        @Override
        public LuaValue call(LuaValue arg1, LuaValue arg2) {
            if (arg2 instanceof LuaTable) {
                System.out.println(arg1.toString() + LuaUtils.dumpTable((LuaTable) arg2));
            } else {
                System.out.println(arg1.toString() + arg2.toString());
            }
            return LuaValue.NONE;
        }
    };

    public OneArgFunction sleep = new OneArgFunction() {
        public LuaValue call(LuaValue arg) {
            try {
                Thread.sleep(arg.tolong());
            } catch (Exception ex) {
                throw new RuntimeException();
            }
            return LuaValue.NONE;
        }
    };
}