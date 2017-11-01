import java.io.*;

public class VideoStream{
    Process process;
    BufferedInputStream stdOut;

    public void getStarted(String fps) throws IOException {
        process = new ProcessBuilder("raspivid", "-t", "0", "-h", "240", "-w", "320", "-fps", fps, "-hf","-n", "-b", "2000000" , "-o", "-").start();
        stdOut = new BufferedInputStream(process.getInputStream());
    }

    public void stopVideo() throws IOException {
        process.destroy();
    }

    public int getnextframe(byte[] frame) throws Exception {
        return(stdOut.read(frame,0,20000));
    }
}
