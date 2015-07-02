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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alex73.chdkptpj.camera.Camera;
import org.alex73.chdkptpj.camera.CameraFactory;
import org.apache.log4j.PropertyConfigurator;

/**
 * Script for show changed UI Properties.
 */
public class TestUIProperties {
    static Camera c;
    static Map<String, String> initial;
    static List<String> initialSorted;

    public static void main(String[] a) throws Exception {
        PropertyConfigurator.configure("log4j.properties");

        List<Camera> cameras = CameraFactory.findCameras();
        switch (cameras.size()) {
        case 0:
            System.out.println("There is no connected cameras");
            return;
        case 1:
            break;
        default:
            System.out.println("Too many connected cameras");
            return;
        }
        c = cameras.get(0);
        c.connect();
        System.out.println("Camera: " + c.getDevice().getManufacturerString() + "/"
                + c.getDevice().getProductString() + " : " + c.getDevice().getSerialNumberString());

        System.out.println("This application displays changes in the UI Properties (PTM functions)");
        System.out
                .println("Setup your camera into initial state, then press enter for request initial properties. Press Ctrl-C if you want to finish.");
        System.out.println("Waiting for press Enter...");
        while (System.in.read() != 10)
            ;
        initial = requestUIProperties();
        initialSorted = new ArrayList<>(initial.keySet());
        Collections.sort(initialSorted);

        System.out.println("Initial properties requested. Values are:");
        for (String k : initialSorted) {
            System.out.println("    " + k + " = " + initial.get(k));
        }

        while (true) {
            check();
        }
    }

    static void check() throws Exception {
        System.out
                .println("\nChange camera settings, then press enter for see changed properties(against initial)");
        System.out.println("Waiting for press Enter...");
        while (System.in.read() != 10)
            ;

        Map<String, String> changed = requestUIProperties();
        for (String k : initialSorted) {
            String ov = initial.get(k);
            String nv = changed.get(k);
            if (!ov.equals(nv)) {
                System.out.println("    " + k + " = " + nv);
            }
        }
    }

    static Map<String, String> requestUIProperties() throws Exception {
        String script = "call_event_proc('UI.CreatePublic')\n"
                + "local r = {}\n"
                + "for i=0x8001,0x80c5 do\n"
                + "  local v = call_event_proc('PTM_GetCurrentItem',i)\n"
                + "  if v == 65535 then\n"
                + "    v = -1\n"
                + "  end\n"
                + "   r[string.format('0x%x',i)] = v\n"
                + "end\n"
                + "return r";
        String r = (String) c.executeLua(script);

        Map<String, String> result = new HashMap<>();
        for (String vs : r.split("\n")) {
            String[] kv = vs.split("\t");
            if (kv.length != 2) {
                System.out.println("Error parse camera response line: '" + vs + "'");
                System.exit(1);
            }
            result.put(kv[0], kv[1]);
        }
        return result;
    }
}
