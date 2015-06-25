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

import java.io.File;

import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.TwoArgFunction;

public class LFS extends ALuaBaseLib {
    public LFS() {
        super("lfs");
    }

    public TwoArgFunction attributes = new TwoArgFunction() {
        @Override
        public LuaValue call(LuaValue arg1, LuaValue arg2) {
            String path = arg1.tojstring();
            File f = new File(path);
            if (!f.exists()) {
                return LuaValue.NONE;
            }
            return LuaValue.NONE;
        }
    };

    public LibFunction mkdir = new LibFunction() {
        @Override
        public Varargs invoke(Varargs varargs) {
            LuaValue path = varargs.arg1();
            File f = new File(path.tojstring());
            if (f.exists()) {
                if (f.isDirectory()) {
                    return LuaValue.TRUE;
                } else {
                    return LuaValue.varargsOf(LuaBoolean.FALSE,
                            LuaValue.valueOf("'" + path + "' exist but not is directory"));
                }
            }
            if (f.mkdirs()) {
                // created
                return LuaValue.TRUE;
            } else {
                return LuaValue.varargsOf(LuaBoolean.FALSE,
                        LuaValue.valueOf("Impossible to create dir '" + path + "'"));
            }
        }
    };
}