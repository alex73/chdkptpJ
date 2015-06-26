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

    public static void focus() throws Exception {
//        
//        // autofocus lock
//        cam.executeLuaQuery("set_aflock(0); press (\"shoot_half\"); sleep(5000); set_aflock(1); release (\"shoot_half\");");
//        
//        set_raw(1);
//        set_raw_nr(1);
//
//        System.out.println("FocusMode = "+cam.getFocusMode());
//        System.out.println("FocusDistance(mm) = "+cam.getFocus());
//        System.out.println("FocusState = "+cam.executeLuaQuery("return get_focus_state();"));
//        System.out.println("DofInfo :\n"+cam.executeLuaQuery("return get_dofinfo();"));
//        
//        
        
    }
    
    // panel.displayImage( ImageIO.read( new File( "/tmp/1.jpg" ) ) );

//    while (true) {
//        panel.displayImage(cam.getView());
//
//        panel.repaint();
        // Thread.sleep(200);
//    }
    
    
//    System.out.println("Serial = " + cam.getCameraSerialNumber());
//    System.out.println("get_buildinfo = "+cam.executeLuaQuery("return get_buildinfo();"));
//    //System.out.println("propset = " + cam.get_propset());
////    Map<String,Integer> propset=PropsetUtils.getPropset(cam.get_propset());
////    for(Map.Entry<String, Integer> en:propset.entrySet()) {
////        System.out.println("  prop."+en.getKey()+"("+en.getValue()+") = " + cam.get_prop(en.getValue()));
////    }
//   
//    cam.setOperaionMode(CameraMode.RECORD);
//  
//    //focus();
//    
//    System.out.println("get_av96 = "+cam.executeLuaQuery("return get_av96();"));
//    System.out.println("get_bv96 = "+cam.executeLuaQuery("return get_bv96();"));
//    System.out.println("get_ev = "+cam.executeLuaQuery("return get_ev();"));
//    System.out.println("get_iso_mode  = "+cam.executeLuaQuery("return get_iso_mode();"));
// //   System.out.println("get_live_histo  = "+cam.executeLuaQuery("return get_live_histo  ();"));
//
//    System.out.println("Temp(optical)  = "+cam.executeLuaQuery("return get_temperature(0);"));
//    System.out.println("Temp(CCD)  = "+cam.executeLuaQuery("return get_temperature(1);"));
//    System.out.println("Temp(battery)  = "+cam.executeLuaQuery("return get_temperature(2);"));
//    
//    System.out.println("get_free_disk_space(MiB)  = "+((int)cam.executeLuaQuery("return get_free_disk_space();")/1024));
//    
//   cam.executeLuaQuery("return shoot();");
//    System.exit(1);
//    
//    // download file
//    // remove all images
//    //zoom
//    
//    
    
    // cam.getImageResolution()

}
