import java.net.DatagramPacket;
import java.net.DatagramSocket;
import javax.swing.Timer;
import java.io.*;

public class UDPServer {
    VideoStream video;
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
        server.destPort =

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
    }
}
