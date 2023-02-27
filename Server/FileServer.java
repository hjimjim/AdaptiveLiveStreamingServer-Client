import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.net.Socket;

public class FileServer implements Runnable {
    private String ip;
    private int port;
    private String[] fileList;
    private ServerStatus serverStatus;

    public FileServer(int port, ServerStatus serverStatus) {
        this.port = port;
        this.serverStatus = serverStatus;
    }
    
    public void run() {
        while(true) {
            if(!serverStatus.file_flag) {
                continue;
            }
            this.fileList =  serverStatus.filelist.split("#");
            try {
                ip = serverStatus.clientIP;
                //System.out.println(ip);
                //System.out.println(port);
                Socket s = new Socket(ip, port);
                
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                File file;
                FileInputStream fis;
                byte[] buffer = new byte[4096];
                for(int i = 0; i < fileList.length; i++) {
                    file = new File("./saved/" + fileList[i]);
                    if(file.exists()) {
                        //System.out.println(file.length());
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
                serverStatus.file_flag = false;
                dos.close();
                s.close();
            } catch(Exception downE) {
                System.out.println("Exception while downloading" + downE);
            }
        }
    }
}
