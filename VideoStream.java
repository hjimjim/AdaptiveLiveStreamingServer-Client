import java.io.*;

public class VideoStream{
    Process process;
    BufferedInputStream stdOut;

    public void getStarted() throws IOException {
        process = new ProcessBuilder("raspivid", "-t", "0", "-h", "240", "-w", "360", "-fps", "15", "-hf","-n", "-b", "2000000" , "-o", "-").start();
        stdOut = new BufferedInputStream(process.getInputStream());
        System.out.println("Jimin@@@@@@@@@@@@2 \n\n\n\n\n");
    }

    public void stopVideo() throws IOException {
        process.destroy();
    }

    public int getnextframe(byte[] frame) throws Exception {
        return(stdOut.read(frame,0,20000));
    }
}
