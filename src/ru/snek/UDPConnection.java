package ru.snek;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static ru.snek.Utils.Utils.objectAsByteArray;

public class UDPConnection {
    private DatagramSocket socket;

    private static final int maxBufferSize = 65507;


    public UDPConnection(int port) throws IOException{
        SocketAddress a = new InetSocketAddress(port);
        socket = new DatagramSocket(a);
    }

    public DatagramPacket receive() throws IOException{
        ByteBuffer buf = ByteBuffer.allocate(maxBufferSize);
        DatagramPacket i = new DatagramPacket(buf.array(), buf.array().length);
        socket.receive(i);
        return i;
    }

    public void send(Response response, SocketAddress client) throws IOException {
        byte[] objAsArr = objectAsByteArray(response);
        int size = objAsArr.length;
        byte[] sizeArr = ByteBuffer.allocate(10).putInt(size).array();
        int amount = size <= maxBufferSize ? 1 : (size / maxBufferSize + 1);
        DatagramPacket o = new DatagramPacket(sizeArr, 10, client);
        socket.send(o);
        ByteBuffer bb;
        if (amount == 1) bb = ByteBuffer.allocate(objAsArr.length);
        else bb = ByteBuffer.allocate(maxBufferSize);
        for (int i = 0; i < amount; ++i) {
            bb.clear();
            int j;
            for (j = 0; j < (i < (amount - 1) ? maxBufferSize : size % maxBufferSize); ++j) {
                bb.put(objAsArr[(i * maxBufferSize) + j]);
            }
            while (j < bb.array().length) {
                bb.put((byte) 0);
                ++j;
            }
            DatagramPacket p = new DatagramPacket(bb.array(), bb.array().length, client);
            socket.send(p);

            if (i % 50 == 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public void close() {
        try {
            socket.close();
        } catch (Exception e) {}
    }
}
