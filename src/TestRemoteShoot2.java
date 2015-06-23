import org.alex73.chdkptpj.camera.Camera;
import org.alex73.chdkptpj.camera.CameraFactory;
import org.alex73.chdkptpj.lua.ChdkPtpJ;

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

        fr.execute();
    }

}
