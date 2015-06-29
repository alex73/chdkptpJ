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

/**
 * Interface for call Lua commands. See http://chdk.wikia.com/wiki/CHDK_Scripting_Cross_Reference_Page for
 * details.
 */
public class CHDKScripting {
    private final Camera camera;

    public CHDKScripting(Camera camera) {
        this.camera = camera;
    }

    public int get_propset() throws Exception {
        return (int) camera.executeLua("return get_propset()");
    }

    public int get_prop(int prop_id) throws Exception {
        return (int) camera.executeLua("return get_prop(" + prop_id + ")");
    }

    public void set_prop(int prop_id, int value) throws Exception {
        camera.executeLua("set_prop(" + prop_id + "," + value + ")");
    }

    public String get_prop_str(int prop_id) throws Exception {
        return (String) camera.executeLua("return get_prop_str(" + prop_id + ",200)");
    }

    /**
     * gets current focus distance in mm
     */
    public int get_focus() throws Exception {
        return (int) camera.executeLua("return get_focus()");
    }

    /**
     * returns focus mode, 0=auto, 1=MF, 3=inf., 4=macro, 5=supermacro
     */
    public int get_focus_mode() throws Exception {
        return (int) camera.executeLua("return get_focus_mode()");
    }

    /**
     * returns focus status, > 0 focus successful, =0 not successful, < 0 MF
     */
    public int get_focus_state() throws Exception {
        return (int) camera.executeLua("return get_focus_state()");
    }

    /**
     * Returns a table with dof and focus related information, listed here in order they appear in the table.
     */
    public String get_dofinfo() throws Exception {
        return (String) camera.executeLua("return get_dofinfo()");
    }

    /**
     * Returns information about the running platform and chdk version. Returns a table with the members :
     * platform, platformid, platsub, version, os, build_number, build_revision, build_date, build_time
     */
    public String get_buildinfo() throws Exception {
        return (String) camera.executeLua("return get_buildinfo()");
    }

    /**
     * returns the number of zoom steps supported by the camera
     */
    public int get_zoom_steps() throws Exception {
        return (int) camera.executeLua("return get_zoom_steps()");
    }

    /**
     * returns current zoom position
     */
    public int get_zoom() throws Exception {
        return (int) camera.executeLua("return get_zoom()");
    }

    /**
     * sets the current zoom position
     */
    public void set_zoom(int position) throws Exception {
        camera.executeLua("set_zoom(" + position + ")");
    }

    /**
     * sets zoom speed (only works on some cameras)
     */
    public void set_zoom_speed(int speed) throws Exception {
        camera.executeLua("set_zoom_speed(" + speed + ")");
    }
}
