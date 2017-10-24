import java.net.DatagramPacket;
import java.net.DatagramSocket;
import javax.swing.Timer;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UDPServer implements ActionListener {
    /*VideoStream video;
    Timer timer;
    FileOutputStream fos1 = null;
    DatagramSocket dsoc;
    InetAddress ClientIPAddr;
    //int destPort = 0;
    byte[] buf;

    public UDPServer() {
        dsoc = new DatagramSocket(7777);
        video = new VideoStream();
        timer = new Timer(100);
        File outputFile = new File("video.h264");
        fos1 = new FileOutputStream(outputFile,true);
        buf = new byte[20000];
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub

        UDPServer server = new UDPServer();
        server.ClientIPAddr = server.dsoc.getInetAddress();

        try{
            timer.start();

            while(true){
                // 데이터 전송 받기
                dsoc.receive(dp);
                // 데이터 보낸곳 확인
               // System.out.println(" 송신 IP : " + dp.getAddress());
                // 보낸 데이터를 Utf-8에 문자열로 벼환
                String msg = new String(dp.getData(),"UTF-8");
                // System.out.println("보내 온 내용  : " + msg);
            }



        } catch(Exception e){
            System.out.println(e.getMessage());
        }

    }

    public void actionPerformed(ActionEvent e) {
        try {
            //get next frame to send from the video, as well as its size
            video.getnextframe(buf);
            fos1.write(buf, 0, image_length);

            senddp = new DatagramPacket(buf, buf.length);

            dsoc.send(senddp);
        } catch (Exception ex) {
            System.out.println("Exception caught5: " + ex);
            //System.exit(0);
        }
    }*/
    DataInputStream dis = null;
    FileInputStream fis = null;
    static Timer timer;
    DatagramSocket socket;
    DatagramPacket dp;
    byte[] by = new byte[20000];
    static boolean end = true;
    InetAddress ia;
    String filename = "sample.h264";

    public UDPServer() {
        System.out.println(this);
        timer = new Timer(100, this);
        File file = new File(filename);

        if(file.exists() == false) {
            System.out.println("File does not exist");
            System.exit(0);
        }

        try {
            System.out.println("1");
            fis = new FileInputStream("sample.h264");
            dis = new DataInputStream(fis);
        } catch(Exception e) {
            System.out.println("!!!!!!");
            e.printStackTrace();
        }

        String ip = "127.0.0.1";
        try {
            System.out.println("2");
            socket = new DatagramSocket();
            ia = InetAddress.getByName(ip);
        } catch(Exception e) {
            System.out.println("!!!!!");
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        UDPServer server = new UDPServer();

        System.out.println("3");
        timer.start();

        System.out.println("File ended");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            System.out.println("tic");
            int xx = dis.read(by, 0, by.length);
            if (xx == -1) {
                timer.stop();
                end = false;
            }

            dp = new DatagramPacket(by, xx, ia, 9875);
            socket.send(dp);
        } catch(Exception e1) {
            System.out.println(e1.getMessage());
        }
    }
}
