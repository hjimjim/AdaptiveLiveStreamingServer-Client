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
	private ClientStatus clientStatus;
	private int port;
	View v;

	public FileDownloader(int port, ClientStatus clientStatus, View v) {// String[] fileList) {
		this.port=port;
		this.clientStatus = clientStatus;
		this.v = v;
	}
	public void run() {
		while (true) {
			if (!clientStatus.file_flag) {
				System.out.print("");
				continue;
			} else {
				this.fileList = clientStatus.downloadList.split("#");
				try {
					ss = new ServerSocket(port);
					Socket clientSock = ss.accept();
					if (clientSock != null) {
						addLog("Client got saved file");
						saveFile(clientSock);
						clientStatus.file_flag = false;
						JOptionPane.showMessageDialog(null, "Download Finished!");
						v.progress_bar.setValue(0);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void addLog(String log) {
		v.txtLog.append(log + "\n");
		v.txtLog.setCaretPosition(v.txtLog.getDocument().getLength());
	}

	private void saveFile(Socket clientSock) throws IOException {
		DataInputStream dis = new DataInputStream(clientSock.getInputStream());
		int length = fileList.length;
        if(length < 0) {
            addLog("Error : File list length is under 0");
            return;
        }
		FileOutputStream fos = null;
		File[] files = new File[fileList.length];
		long[] size = new long[fileList.length];
		
		for (int i = 0; i < fileList.length; i++) {
			files[i] = new File("./download/" + fileList[i]);
			if (files[i].exists()) {
				files[i].delete();
			}
			size[i] = dis.readLong();
			v.totalSize = size[i];
			System.out.println("SIZE: " + size[i]);
		}
		
        if(files.length < 0) {
            addLog("Error : File length is under 0");
            return;
        }

		byte[] buffer = new byte[4096];
		int read;
		
		for (int i = 0; i < files.length; i++) {
			System.out.println("Receiving file: " + files[i].getName());
			fos = new FileOutputStream(files[i], true);
			while (size[i] > 0 && (read = dis.read(buffer, 0, (int) Math.min(buffer.length, size[i]))) != -1) {
				fos.write(buffer, 0, read);
				size[i] -= read;
				v.currentValue = (int) (((v.totalSize - size[i]) / v.totalSize) * 100);
				v.progress_bar.setValue(v.currentValue);
			}
			fos.close();
		}
		addLog("Client saved video file from server");
		dis.close();

		clientSock.close();
		ss.close();
	}
}