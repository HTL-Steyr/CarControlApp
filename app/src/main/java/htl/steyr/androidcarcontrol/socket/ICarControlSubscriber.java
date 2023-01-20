package htl.steyr.androidcarcontrol.socket;

public interface ICarControlSubscriber {

    void messageReceived(ICarMessage msg);

}
