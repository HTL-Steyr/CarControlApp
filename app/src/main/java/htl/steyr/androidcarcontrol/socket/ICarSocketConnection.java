package htl.steyr.androidcarcontrol.socket;

public interface ICarSocketConnection {

    public void sendMessage(String message);

    void receiveMessage();

}
