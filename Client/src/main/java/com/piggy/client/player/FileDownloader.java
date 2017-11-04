package com.piggy.client.player;
import javax.swing.*;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;

public class FileDownloader implements Runnable {
    private ServerSocket ss;
    private String[] fileList;
    private SharedArea sharedArea;
    private int port;
    private JTextArea txtLog;

    public FileDownloader(int port, SharedArea sharedArea, JTextArea txtArea){//String[] fileList) {
        this.port = port;
        this.sharedArea = sharedArea;
        this.txtLog = txtArea;
        //this.fileList = fileList;
    }

    public void run() {
        while(true) {
            if(!sharedArea.file_flag) {
                System.out.print("");
                continue;
            } else {
                this.fileList = sharedArea.downloadList.split("#");
                try {
                    ss = new ServerSocket(port);
                    Socket clientSock = ss.accept();
                    if (clientSock != null) {
                        addLog("Client started saving file");
                        saveFile(clientSock);
                        sharedArea.file_flag = false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private void addLog(String log) {
        txtLog.append(log + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }

    private void saveFile(Socket clientSock) throws IOException {
        DataInputStream dis = new DataInputStream(clientSock.getInputStream());
        int length = fileList.length;
        if(length < 0) {
            addLog("Error : File list length is under 0");
            return;
        }
        FileOutputStream fos = null;
        File[] files = new File[length];
        long[] size = new long[length];

        for(int i = 0; i < length; i++) {
            files[i] = new File("./download/" + fileList[i]);
            if(files[i].exists()) {
                files[i].delete();
            }
            size[i] = dis.readLong();
            System.out.println("SIZE: " + size[i]);
        }


        if(files.length < 0) {
            addLog("Error : File length is under 0");
            return;
        }

        byte[] buffer = new byte[4096];
        int read;
        for(int i = 0; i < files.length; i++) {
            System.out.println("Receiving file: " + files[i].getName());
            fos = new FileOutputStream(files[i], true);

            while(size[i] > 0 && (read = dis.read(buffer, 0, (int)Math.min(buffer.length, size[i]))) != -1) {
                fos.write(buffer, 0, read);
                size[i] -= read;
            }
            fos.close();
        }
        addLog("Client finished saving video file from server");
        dis.close();
        clientSock.close();
        ss.close();
    }
}