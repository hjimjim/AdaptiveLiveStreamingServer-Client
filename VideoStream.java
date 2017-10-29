import java.io.*;

public class VideoStream{
    Process process;
    BufferedInputStream stdOut;

    public void getStarted(String height, String width) throws IOException {
<<<<<<< HEAD
        process = new ProcessBuilder("raspivid", "-t", "0", "-h", height, "-w", height, "-fps", "15", "-hf","-n", "-b", "2000000" , "-o", "-").start();
=======
        process = new ProcessBuilder("raspivid", "-t", "0", "-h", height, "-w", width, "-fps", "15", "-hf","-n", "-b", "2000000" , "-o", "-").start();
>>>>>>> 463c77e13161d4c4ff5e8049112b58c3b81eaf75
        stdOut = new BufferedInputStream(process.getInputStream());
    }

    public void stopVideo() throws IOException {
        process.destroy();
    }

    public int getnextframe(byte[] frame) throws Exception {
        return(stdOut.read(frame,0,20000));
    }
}
