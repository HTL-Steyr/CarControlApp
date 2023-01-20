package htl.steyr.androidcarcontrol.socket;

public class CarMessage implements ICarMessage {
    private ICarSocketConnection connection = null;
    private String messge = "";

    public CarMessage(ICarSocketConnection connection, String messge) {
        this.connection = connection;
        this.messge = messge;
    }

    @Override
    public String getMessage() {
        return messge;
    }

    @Override
    public void setMessage(String msg) {
        this.messge = msg;
    }

    @Override
    public ICarSocketConnection getSource() {
        return connection;
    }

    @Override
    public void setSource(ICarSocketConnection connection) {
        this.connection = connection;
    }
}
