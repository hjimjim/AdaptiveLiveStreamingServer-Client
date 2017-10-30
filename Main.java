/**
 * Created by Jimin on 10/30/17.
 */
public class Main {
    public static void main(String argv[]) throws Exception
    {
        SharedArea sharedArea = new SharedArea();
        VideoStream videoStream = new VideoStream();
        Server server = new Server(videoStream, sharedArea);
        Wifi wifi = new Wifi(videoStream, sharedArea);


        Thread server_thread = new Thread(server);
        Thread wifi_thread = new Thread(wifi);


        server_thread.start();
    }
}

class SharedArea {
    boolean wifi_flag;
}
