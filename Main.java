/**
 * Created by Jimin on 10/30/17.
 */
public class Main {
    public static void main(String argv[]) throws Exception
    {
        SharedArea sharedArea = new SharedArea();
        VideoStream videoStream = new VideoStream();
        sharedArea.start_flag = false;
        Wifi wifi = new Wifi(videoStream, sharedArea);
        Server server = new Server(videoStream, sharedArea, wifi);

        Thread server_thread = new Thread(server);
        Thread wifi_thread = new Thread(wifi);

        server_thread.start();
        wifi_thread.start();
    }
}

class SharedArea {
    boolean wifi_flag;
    boolean start_flag;
}
