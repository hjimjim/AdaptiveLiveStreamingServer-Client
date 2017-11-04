package com.piggy.client.player;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import javax.swing.*;

/**
 * Created by Jimin on 10/29/17.
 */
public class Main {
    public static void main(String[] argv) throws Exception{
        PipedOutputStream os = new PipedOutputStream();
        PipedInputStream is = new PipedInputStream(os);


        JTextArea txtLog = new JTextArea();

        SharedArea sharedArea = new SharedArea();
        sharedArea.file_flag = false;
        JFrame frame = new JFrame("DrugStore");
        H264Player h264Player = new H264Player(is, frame);
        Client client = new Client(os, frame, h264Player, sharedArea, txtLog);
        FileDownloader fileDownloader = new FileDownloader(5522, sharedArea, txtLog);

        Thread write = new Thread(client);
        Thread read = new Thread(h264Player);
        Thread file = new Thread(fileDownloader);

        write.start();
        read.start();
        file.start();
    }
}

class SharedArea {
    boolean file_flag;
    String downloadList;
}
