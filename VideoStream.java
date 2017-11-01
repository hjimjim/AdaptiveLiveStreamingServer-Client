import java.io.*;

public class VideoStream{
    Process process;
    BufferedInputStream stdOut;

    public void getStarted(String height, String width) throws IOException {
        process = new ProcessBuilder("raspivid", "-t", "0", "-h", height, "-w", width, "-fps", "50", "-hf","-n", "-b", "2000000" , "-o", "-").start();
        stdOut = new BufferedInputStream(process.getInputStream());
    }

    public void stopVideo() throws IOException {
        process.destroy();
    }

    public int getnextframe(byte[] frame) throws Exception {
        return(stdOut.read(frame,0,20000));
    }
}
