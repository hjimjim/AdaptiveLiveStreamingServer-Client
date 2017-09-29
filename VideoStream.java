//VideoStream

import java.io.*;

public class VideoStream{

    FileInputStream fis; //video file
    int frame_nb; //current frame nb
    Process process;
    BufferedInputStream stdOut;
    OutputStream out;

    //-----------------------------------
    //constructor
    //-----------------------------------
    public VideoStream(String filename){
        //init variables
        frame_nb = 0;
    }

    //-----------------------------------
    // getnextframe
    //returns the next frame as an array of byte and the size of the frame
    //-----------------------------------
 //   FileOutputStream fos;
    public void getStarted() throws IOException {

	process = new ProcessBuilder("raspivid", "-t", "0", "-h", "240", "-w", "360", "-fps", "25", "-hf","-n", "-b", "2000000" , "-o", "-").start();
        stdOut = new BufferedInputStream(process.getInputStream());

//	fos = new FileOutputStream("ho.h264", true);

	//while(true) {
	//	System.out.print(String.format("%02X",stdOut.read()));
//	}
    }

    public int getnextframe(byte[] frame) throws Exception {
	System.out.println("start get next frame");
        int length = 0;
        String length_string;
	byte[] frame_length = new byte[5];

        //read current frame length
        //stdOut.read(frame_length, 0, 8);

        //transform frame_length to integer
        //length_string = new String(frame_length);
        //length = Integer.parseInt(length_string);
	//byte[] buffer = new byte[2048];
	//stdOut.read(buffer,0,2048);
	//fos.write(buffer);
	//System.out.println("end get next frame");
        return(stdOut.read(frame,0,1200));
    }
}
