package htl.steyr.androidcarcontrol.socket;

public interface ICarControlPublisher {

    void notifyAll(ICarMessage msg);

    void addSubscriber(ICarControlSubscriber sub);

    void removeSubscriber(ICarControlSubscriber sub);

}
