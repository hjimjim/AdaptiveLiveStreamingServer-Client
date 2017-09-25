/**
 * Created by Jimin on 9/25/17.
 */
import java.awt.event.*;
import javax.swing.Timer;

//------------------------
//Controls RTP sending rate based on traffic
//------------------------
public class CongestionController implements ActionListener {
    private Timer ccTimer;
    int interval;   //interval to check traffic stats
    int prevLevel;  //previously sampled congestion level
    Server server;

    public CongestionController(int interval, Server server) {
        this.interval = interval;
        ccTimer = new Timer(interval, this);
        ccTimer.start();
        this.server = server;
    }

    public void actionPerformed(ActionEvent e) {
        //adjust the send rate
        if (prevLevel != server.congestionLevel) {
            server.sendDelay = server.FRAME_PERIOD + server.congestionLevel * (int)(server.FRAME_PERIOD * 0.1);
            server.timer.setDelay(server.sendDelay);
            prevLevel = server.congestionLevel;
            System.out.println("Send delay changed to: " + server.sendDelay);
        }
    }
}
