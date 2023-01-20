package htl.steyr.androidcarcontrol.socket;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class CarSocketConnection implements ICarControlPublisher, ICarSocketConnection {

    private int port = 0;
    private String host = "";

    private Socket carSocket = null;

    private Scanner inputStream = null;
    private PrintWriter outputStream = null;

    private boolean running = false;

    // private ObjectOutputStream outputStream = null;
    // private ObjectInputStream inputStream = null;

    private ArrayList<ICarControlSubscriber> subscribers = new ArrayList<>();

    public CarSocketConnection(Socket socket) {
        this.carSocket = socket;
        this.port = socket.getPort();
        this.host = socket.getInetAddress().getHostAddress();

        try {
            outputStream = new PrintWriter(carSocket.getOutputStream(), true);
            inputStream = new Scanner(carSocket.getInputStream());

            // outputStream = new ObjectOutputStream(carSocket.getOutputStream());
            // inputStream = new ObjectInputStream(carSocket.getInputStream());

            running = true;

            receiveMessage();
        } catch (IOException e) {
        }
    }

    public CarSocketConnection(String host, int port) {
        this.port = port;
        this.host = host;

        try {
            carSocket = new Socket(host, port);

            outputStream = new PrintWriter(carSocket.getOutputStream(), true);
            inputStream = new Scanner(carSocket.getInputStream());

            // outputStream = new ObjectOutputStream(carSocket.getOutputStream());
            // inputStream = new ObjectInputStream(carSocket.getInputStream());

            running = true;

            receiveMessage();

        } catch (IOException e) {
        }
    }

    @Override
    public void sendMessage(String message) {
        new Thread() {
            @Override
            public void run() {
                outputStream.println(message);
            }
        }.start();
    }

    @Override
    public void receiveMessage() {
        new Thread() {
            @Override
            public void run() {
                while (running) {
                    String msg = inputStream.nextLine();

                    CarSocketConnection.this.notifyAll(new CarMessage(CarSocketConnection.this, msg));
                }
            }
        }.start();
    }

    public void stop() {
        running = false;
    }

    public void notifyAll(ICarMessage msg) {
        for (ICarControlSubscriber sub : subscribers) {
            sub.messageReceived(msg);
        }
    }

    @Override
    public void addSubscriber(ICarControlSubscriber sub) {
        if (!subscribers.contains(sub)) {
            subscribers.add(sub);
        }
    }

    @Override
    public void removeSubscriber(ICarControlSubscriber sub) {
        subscribers.remove(sub);
    }
}
