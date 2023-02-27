package com.piggy.client.player;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Created by Jimin on 10/29/17.
 */
public class Main {
    public static void main(String[] argv) throws Exception{
        PipedOutputStream os = new PipedOutputStream();
        PipedInputStream is = new PipedInputStream(os);

        ClientStatus clientStatus = new ClientStatus();
        clientStatus.file_flag = false;
        clientStatus.down_start = false;
		
        View v = new View();
        H264Player h264Player = new H264Player(is, v);
        Client client = new Client(os, v, h264Player, clientStatus);
        FileDownloader fileDownloader = new FileDownloader(5522, clientStatus, v);
        
        Thread write = new Thread(client);
        Thread read = new Thread(h264Player);
        Thread file = new Thread(fileDownloader);
       
        write.start();
        read.start();
        file.start();
    }
}

class ClientStatus {
    boolean file_flag;
    String downloadList;
    boolean down_start;
}
