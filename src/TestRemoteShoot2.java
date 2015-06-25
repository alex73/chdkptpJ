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

        ChdkPtpJ fr = new ChdkPtpJ(camera, "lua");

        // long tm = System.currentTimeMillis() / 10000;
        // while (tm == System.currentTimeMillis() / 10000) {
        // // wait for 10-seconds align time
        // Thread.sleep(10);
        // }

        LuaTable p1 = new LuaTable();
        fr.executeCliFunction("connect", p1);

        LuaTable p = new LuaTable();
        p.set("img", LuaValue.valueOf(true));
        p.set("raw", LuaValue.valueOf(true));
        p.set(1, LuaValue.valueOf("/tmp/123/"));
        fr.executeCliFunction("remoteshoot", p);
    }

}
