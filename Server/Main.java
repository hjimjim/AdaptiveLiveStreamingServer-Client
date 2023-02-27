public class Main {
    public static void main(String argv[]) throws Exception
    {
        ServerStatus serverStatus = new ServerStatus();
        Camera camera = new Camera();
        serverStatus.start_flag = false;
        serverStatus.file_flag = false;
        serverStatus.filelist = "";
        serverStatus.disconnect_flag = false;


        Wifi wifi = new Wifi(camera, serverStatus);
        Server server = new Server(camera, serverStatus, wifi);
        FileServer fileServer = new FileServer(5522, serverStatus);

        Thread server_thread = new Thread(server);
        Thread wifi_thread = new Thread(wifi);
        Thread fileServer_thread = new Thread(fileServer);

        server_thread.start();
        wifi_thread.start();
        fileServer_thread.start();
    }
}

class ServerStatus {
    String filelist;
    boolean file_flag;
    boolean start_flag;
    boolean disconnect_flag;
    String clientIP;
}
