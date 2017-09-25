//VideoStream

import java.io.*;

public class VideoStream{
    int frame_nb; //current frame nb
    Process process;
    BufferedInputStream stdOut;

    public VideoStream(){
        frame_nb = 0;
    }

    public void getStarted() throws IOException {
        process = new ProcessBuilder("raspivid", "-t", "0", "-h", "240", "-w", "360",
                "-fps", "25", "-hf","-n", "-b", "2000000" , "-o", "-").start();
        stdOut = new BufferedInputStream(process.getInputStream());
    }

    public int getnextframe(byte[] frame) throws Exception {
        System.out.println("start get next frame");
        System.out.println("end get next frame");
        return(stdOut.read(frame,0,32));
    }
}
