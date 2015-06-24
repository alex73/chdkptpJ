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
import org.alex73.chdkptpj.lua.libs.ChdkConnection;
import org.alex73.chdkptpj.lua.libs.ChdkLib;
import org.alex73.chdkptpj.lua.libs.CoreVar;
import org.alex73.chdkptpj.lua.libs.GuiSys;
import org.alex73.chdkptpj.lua.libs.LFS;
import org.alex73.chdkptpj.lua.libs.Lbuf;
import org.alex73.chdkptpj.lua.libs.SysLib;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

/**
 * Class for Lua code execution.
 * 
 * Application can create more than one instance of this class - one for each connected camera. But this calss
 * and Lua scripts should be used in one thread only.
 */
public class ChdkPtpJ {
    private final Globals globals;

    public ChdkPtpJ(Camera camera, String luaScriptsDir) throws Exception {
        // initialize Lua standard packages
        globals = JsePlatform.standardGlobals();

        // add custom Lua packages that chdkptp implemented
        globals.load(new SysLib());
        globals.load(new ChdkConnection(camera));
        globals.load(new ChdkLib());
        globals.load(new Lbuf());
        globals.load(new CoreVar());
        globals.load(new GuiSys());
        globals.load(new LFS());

        fixLua();

        globals.package_.setLuaPath(luaScriptsDir + "/?.lua");

        LoadState.install(globals);
        LuaC.install(globals);

        /**
         * Call modified main.lua for setup functions and initial data.
         */
        globals.loadfile(luaScriptsDir + "/m.lua").call();
    }

    /**
     * Implement some Lua-specific things that luaj is not supported.
     */
    private void fixLua() {
        LuaValue unpack = globals.get("table").get("unpack");
        globals.set("unpack", unpack);
        globals.load("function printf(s,...)\n print(string.format(s,...))\n end");
        globals.get("table").set("maxn", maxn);
    }

    public void executeCliFunction(String function, LuaTable args) throws Exception {
        LuaValue func = globals.get("cli").get("names").get(function).get("func");
        System.out.println(func);

        LuaValue r = func.call(LuaValue.NIL, args);
        System.out.println("done: " + r);
    }

    public static OneArgFunction maxn = new OneArgFunction() {
        public LuaValue call(LuaValue arg) {
            LuaTable table = (LuaTable) arg;
            long maxn = 0;
            LuaValue k = LuaValue.NIL;
            while (true) {
                Varargs n = table.next(k);
                if ((k = n.arg1()).isnil())
                    break;
                if (k.isnumber()) {
                    maxn = Math.max(maxn, k.tolong());
                }
            }

            return LuaValue.valueOf(maxn);
        }
    };
}
