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

import java.lang.reflect.Field;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

/**
 * Base for all chdk native modules for Lua. It registers functions by reflection.
 */
public abstract class ALuaBaseLib extends TwoArgFunction {
    private final String registerName;

    public ALuaBaseLib(String registerName) {
        this.registerName = registerName;
    }

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable table = new LuaTable();
        for (Field f : this.getClass().getFields()) {
            if (LuaFunction.class.isAssignableFrom(f.getType())) {
                try {
                    registerCall(table, f.getName(), (LuaFunction) f.get(this));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        if (registerName != null) {
            env.set(registerName, table);
        }
        return table;
    }

    protected void registerCall(LuaTable table, String name, LuaFunction function) {
        table.set(name, function);
    }
}
