import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Created by Jimin on 10/30/17.
 */
public class Wifi implements Runnable {

    final int HIGH = 1;
    final int MID = 2;
    final int LOW = 3;
    final int DISCON = 0;
    int prevSignalL = 1;
    boolean isRecon = false;
    int checkResult = -2;
    int signalcnt = 0;

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
            if(cnt == 500000) {
                //Fixme:Jimin
                //need to change
                this.sharedArea.wifi_flag = wifiHandler();
                cnt = 0;
            }
            cnt++;
            //Fixme:Jimin
            if(checkResult == DISCON) {
                if(!check) {
                    try{
                        this.video.stopVideo();
                        this.video.getStarted("240", "320");
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

    public int check_wifi() {
        System.out.println("check_wifi");
        byte[] bytes = new byte[1024];
        int state = -1;
        String wifi_name = "";
        int signalLevel = 0;
        try {
            Process process = new ProcessBuilder("iwconfig", "wlan0").start();
            InputStream input = process.getInputStream();
            int n = input.read(bytes, 0, 312); // check iwconfig
            String str = new String(bytes);
            // wifi
            wifi_name = str.substring(29, 35);
            if (wifi_name.equals("off/an")) {
                return DISCON;
            }
            System.out.println(str.split("Signal level=")[1]);
            System.out.println(((str.split("Signal level=")[1]).split("  dB"))[0]);

            Scanner sc = new Scanner(((str.split("Signal level=")[1]).split("  dB"))[0]);
            signalLevel = sc.nextInt();
            System.out.println(signalLevel);
            //signalLevel = Integer.parseInt(((str.split("Signal level="))[1].split("  dB"))[0]);
            if (signalLevel >= -18) {
                return HIGH;
            } else if (signalLevel < -18 && signalLevel >= -30) {
                return MID;
            } else if (signalLevel < -30) {
                return LOW;
            }
            process.destroy();
        } catch (IOException e4) {
            System.out.println("Exception Processor Builder: " + e4);
        }
        return state;
    }

    public boolean  wifiHandler() {
        checkResult = check_wifi();
        if(prevSignalL == checkResult) {
            return false;
        }
        switch (checkResult) {
            case DISCON: {
                // timer.stop();
                System.out.println("DISCON");
                if (prevSignalL != DISCON) {
                    try {
                        System.out.println("nononoo");
                        video.stopVideo();
                        video.getStarted("240" , "320");
                    } catch (Exception e10) {
                    }
                }
                break;
            }
            case HIGH: {
                System.out.println("HIGH");
                if (prevSignalL == DISCON) {
                    isRecon = true;
                }
                if (prevSignalL != HIGH) {
                    try {
                        video.stopVideo();
                        video.getStarted("240", "320");
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
                break;
            }
            case MID: {
                if (prevSignalL == DISCON) {
                    isRecon = true;
                }
                System.out.println("MID");
                if (prevSignalL != MID) {
                    try {
                        video.stopVideo();
                        video.getStarted("240" , "360");
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
                break;
            }
            case LOW: {
                if (prevSignalL == DISCON) {
                    isRecon = true;
                }
                System.out.println("LOW");
                if (prevSignalL != LOW) {
                    try {
                        this.video.stopVideo();
                        this.video.getStarted("120", "180");
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
                break;
            }
            default: {
                System.out.println("DEFAULT : Wrong Value! (Wifi Signal Level)");
            }
        }

//        if (isRecon) {
//            String fileList = "";
//            for(int i = 0; i < fileIndex; i++) {
//                fileList += "video_" + (i+1) + ".h264/";
//            }
//            System.out.println("fileList : " + fileList + ", fileList.getBytes() : " + fileList.getBytes() + ", fileList.length() : " + fileList.length());
//            senddp = new DatagramPacket(fileList.getBytes(), fileList.length(), ClientIPAddr, RTP_dest_port);
//            try {
//                RTPsocket.send(senddp);
//                isRecon = false;
//            } catch(Exception e6) {
//                System.out.println("File list send error : " + e6);
//            }
//        }
        prevSignalL = checkResult; // Saving current wifi state

        return true;
    }


}
