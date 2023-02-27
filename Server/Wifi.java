import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.lang.System;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Wifi implements Runnable {

    final int HIGH = 1;
    final int MID = 2;
    final int LOW = 3;
    final int DISCON = 0;
    int prevSignalL = -1;
    boolean isRecon = false;
    int checkResult = -2;
    int signalcnt = 0;

    final static int FILELIST = 8;
    final static int DOWNLOAD = 9;
    int fileLength = 0;
    int fileIndex = 1;


    Camera video = null;
    ServerStatus serverStatus = null;
    FileOutputStream fos2 = null;
    long time=0;

    public Wifi(Camera camera, ServerStatus serverStatus) {
        this.video = camera;
        this.serverStatus = serverStatus;
    }



    @Override
    public void run() {
        boolean check = false;
        int signal = -1;
        int cnt = 0;
        while (true) {

            if(!this.serverStatus.start_flag) {
                continue;
            }

            if(cnt == 9999999 || check ) {
                if(check_wifi() == DISCON) {
                        if(!check) {
                            time = System.currentTimeMillis();
                            try {
                                this.video.stopVideo();
                                this.video.getStarted("480","720");
                                checkResult = DISCON;
                                prevSignalL = checkResult; // Saving current wifi state
                            } catch (Exception exception) {
                                System.out.println("Exception caught :"+ exception.toString());
                            }
                            serverStatus.disconnect_flag = true;
                        }
                        check = true;
                        try{
                            byte[] buf = new byte[20000];
                            int image_length = this.video.getnextframe(buf);
                            SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd-hh-mm");
                            String filename = date.format(new Date(time));
                            fos2 = new FileOutputStream("./saved/video_"+filename +".h264", true);
                            fos2.write(buf,0,image_length);
                        } catch (Exception e4) {
                            System.out.println(e4.toString());
                        }
                } else {
                    serverStatus.disconnect_flag = false;
                    check = false;
                }
                cnt=0;
            }
            cnt++;
        }
    }

    public int check_wifi() {
        System.out.println("check_wifi");
        byte[] bytes = new byte[1024];
        int state = -1;
        String wifi_name;
        int signalLevel;

        try {
            Process process = new ProcessBuilder("iwconfig", "wlan0").start();
            InputStream input = process.getInputStream();
            input.read(bytes, 0, 320); // check iwconfig
            String str = new String(bytes);

            //wifi
            wifi_name = str.substring(29, 35);
            if (wifi_name.equals("off/an")) {
                return DISCON;
            }

            Scanner sc = new Scanner(((str.split("Signal level=")[1]).split("  dB"))[0]);
            signalLevel = sc.nextInt();

            if (signalLevel >= -30) {
                return HIGH;
            } else if (signalLevel < -30 && signalLevel >= -50) {
                return MID;
            } else if (signalLevel < -50) {
                return LOW;
            }

            process.destroy();
        } catch (IOException e4) {
            System.out.println("Exception Processor Builder: " + e4);
        }
        return state;
    }

    public int wifiHandler() {
        checkResult = check_wifi();
        if(prevSignalL == checkResult) {
            return -1;
        }
        switch (checkResult) {
            case DISCON: {
                // timer.stop();
                System.out.println("DISCON");
                if (prevSignalL != DISCON) {
                    try {
                        video.stopVideo();
                        video.getStarted("240","320");
                    } catch (Exception e10) {
                        System.out.println("Exception caught10 :" + e10.toString());
                    }
                }
                break;
            }
            case HIGH: {
                if (prevSignalL == DISCON) {
                    isRecon = true;
                }
                System.out.println("HIGH");
                if (prevSignalL != HIGH) {
                    try {
                        video.stopVideo();
                        video.getStarted("480","640");
                    } catch (IOException ioException) {
                        System.out.println("Exception caught io :" + ioException.toString());
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
                        video.getStarted("240", "320");
                    } catch (IOException e1) {
                        System.out.println("Exception caught10 :" + e1.toString());
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
                        this.video.getStarted("120", "160");
                    } catch (IOException e1) {
                        System.out.println("Exception caught10 :" + e1.toString());
                        e1.printStackTrace();
                    }
                }
                break;
            }
            default: {
                System.out.println("DEFAULT : Wrong Value! (Wifi Signal Level)");
            }
        }

        if (isRecon) {
            try {
                fos2.close();
            } catch(Exception e5) {
                System.out.println("error while closing the file " + e5);
            } 
            prevSignalL = checkResult; // Saving current wifi state
            isRecon = false;
            return -5;
        }
        prevSignalL = checkResult; // Saving current wifi state
        return checkResult;
    }
}
