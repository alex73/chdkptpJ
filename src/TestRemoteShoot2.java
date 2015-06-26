import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.alex73.chdkptpj.camera.Camera;
import org.alex73.chdkptpj.camera.CameraFactory;
import org.alex73.chdkptpj.lua.ChdkPtpJ;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public class TestRemoteShoot2 {

    public static void main(String[] args) throws Exception {
        Camera camera = null;
        for (Camera c : CameraFactory.findCameras()) {
            camera = c;
            break;
        }

        camera.connect();
        camera.setRecordMode();

        ChdkPtpJ fr = new ChdkPtpJ(camera, "lua-orig");

        long tm = System.currentTimeMillis() / 10000;
        while (tm == System.currentTimeMillis() / 10000) {
            // wait for 10-seconds align time
            Thread.sleep(10);
        }

        // jpeg only - 2.8s, jpeg+raw - 3.8s, raw - 3.7s
        LuaTable p = new LuaTable();
        p.set("jpg", LuaValue.valueOf(true));
        p.set("raw", LuaValue.valueOf(true));
        p.set(1, LuaValue.valueOf("/tmp/123/"));
        fr.executeCliFunction("remoteshoot", p);
    }

}
