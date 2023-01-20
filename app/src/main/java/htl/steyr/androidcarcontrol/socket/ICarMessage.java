package htl.steyr.androidcarcontrol.socket;

public interface ICarMessage {

    String getMessage();
    void setMessage(String msg);

    ICarSocketConnection getSource();
    void setSource(ICarSocketConnection connection);

}
