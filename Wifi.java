import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Created by Jimin on 10/30/17.
 */
public class Wifi implements Runnable {

    final int DISCON = 0;

    final static int FILELIST = 8;
    final static int DOWNLOAD = 9;
    int fileLength = 0;
    int fileIndex = 1;


    VideoStream video = null;
    SharedArea sharedArea = null;
    File saveFile = new File("hahaha.h264");
    FileOutputStream fos2 = null;

    public Wifi(VideoStream videoStream, SharedArea sharedArea) {
        this.video = videoStream;
        this.sharedArea = sharedArea;
        try {
            fos2 = new FileOutputStream(saveFile, true);
        } catch (Exception e) {
            System.out.println("FILE Exception");
        }
    }

    @Override
    public void run() {
        boolean check = false;
        int cnt = 0;
        while (true) {

            if(!this.sharedArea.start_flag) {
                continue;
            }

//            if(cnt == 50000000) {
//                this.sharedArea.wifi_flag = wifiHandler();
//                cnt = 0;
//            }
//            cnt++;

            if(sharedArea.checkResult == DISCON) {
                if(!check) {
                    try{
                        this.video.stopVideo();
                        this.video.getStarted("50");
                    } catch (Exception e10) {

                    }
                }
                check = true;
                try{
                    System.out.println("hahahahahaahahahahahahahahhahahahahh");
                    byte[] buf = new byte[20000];
                    int image_length = this.video.getnextframe(buf);
                    fos2.write(buf,0,image_length);
                } catch (Exception e4) {
                    System.out.println(e4.toString());
                }
                return;
            }
        }
    }




}
