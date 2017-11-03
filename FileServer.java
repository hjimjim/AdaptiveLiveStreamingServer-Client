import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.net.Socket;

public class FileServer extends Thread {
    private String ip;
    private int port;
    private String[] fileList;

    public FileServer(String ip, int port, String[] fileList) {
        this.ip = ip;
        this.port = port;
        this.fileList = fileList;
    }
    
    public void run() {
        try {
            System.out.println("!!!!!!!!!!!@@@@@@@@@@@@@File Server Started");
            Socket s = new Socket(ip, port);
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
            File file;
            FileInputStream fis;
            byte[] buffer = new byte[4096];
            for(int i = 0; i < fileList.length; i++) {
                System.out.println("filename: " + fileList[i]);
                file = new File("./saved/" + fileList[i]);
                if(file.exists()) {
                    System.out.println(file.length());
                    dos.writeLong(file.length());
                }
            }

            int read = 0;
            for(int i = 0; i < fileList.length; i++) {
                fis = new FileInputStream("./saved/" + fileList[i]);
                while((read = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, read);
                }
                fis.close();
                read = 0;
            }
            dos.close();
        } catch(Exception downE) {
            System.out.println("Exception while downloading" + downE);
        }
    }
}
