package edu.oakland.gavin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client implements AutoCloseable
{
    private int joinedRoomNumber;

    private final Socket socket;
    private final DataInputStream dataInputStream;
    private final DataOutputStream dataOutputStream;

    public Client() throws IOException
    {
        this.joinedRoomNumber = -1;

        this.socket = new Socket("localhost", 9999);
        this.dataInputStream = new DataInputStream(this.socket.getInputStream());
        this.dataOutputStream = new DataOutputStream(this.socket.getOutputStream());
    }

    public Socket getSocket()
    {
        return socket;
    }

    public int getJoinedRoomNumber()
    {
        return this.joinedRoomNumber;
    }

    public void joinRoom(int newJoinedRoomNumber)
    {
        this.joinedRoomNumber = newJoinedRoomNumber;
    }

    public boolean hasJoinedRoom()
    {
        return this.joinedRoomNumber >= 0;
    }

    public void sendInteger(int data) throws IOException
    {
        this.dataOutputStream.writeInt(data);
    }

    public void sendMessage(String message) throws IOException
    {
        this.dataOutputStream.writeUTF(message);
    }

    public int readInteger() throws IOException
    {
        return this.dataInputStream.readInt();
    }

    public String readMessage() throws IOException
    {
        return this.dataInputStream.readUTF();
    }

    @Override
    public void close() throws IOException
    {
        this.dataInputStream.close();
        this.dataOutputStream.close();
        this.socket.close();
    }
}