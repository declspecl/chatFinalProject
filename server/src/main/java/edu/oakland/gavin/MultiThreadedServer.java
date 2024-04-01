package edu.oakland.gavin;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.net.ServerSocket;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

public class MultiThreadedServer implements AutoCloseable
{
    private final int port;
    private final ServerSocket socket;

    // easiest way to map values is to use a HashMap
    // we can not have duplicate clients so a HashSet as a value works good
    private final HashMap<Integer, HashSet<Socket>> roomToConnectionsMap;

    public MultiThreadedServer(int port) throws IOException
    {
        this.port = port;
        this.socket = new ServerSocket(port);
        this.roomToConnectionsMap = new HashMap<>();
    }

    public int getPort()
    {
        return this.port;
    }

    public ServerSocket getSocket()
    {
        return socket;
    }

    private void addClientToRoom(int room, Socket client)
    {
        if (this.roomToConnectionsMap.containsKey(room))
        {
            // if the room is already made, add the client to the set of connections in the given room
            this.roomToConnectionsMap.get(room).add(client);
        }
        else
        {
            // otherwise, create a new room and add the client to it
            HashSet<Socket> connections = new HashSet<>();
            connections.add(client);

            this.roomToConnectionsMap.put(room, connections);
        }
    }

    private void removeClientFromRoom(int room, Socket client) throws RuntimeException
    {
        if (this.roomToConnectionsMap.containsKey(room))
        {
            // if the room exists, remote the client from the set of connections
            this.roomToConnectionsMap.get(room).remove(client);

            // after the last client in the room leaves, delete the room
            if (this.roomToConnectionsMap.get(room).isEmpty())
            {
                this.roomToConnectionsMap.remove(room);
            }
        }
        else
        {
            // if the room exists, and we are trying to remove a client from there, an error occurred
            throw new RuntimeException(String.format("Room %d does not exist", room));
        }
    }

    public void handleConnection(Socket client)
    {
        // spawn new thread to manage new client's connection
        new Thread(() ->
        {
            // attempt to get data input stream from client, quitting if unsuccessful
            final DataInputStream dataInputStream;
            try
            {
                dataInputStream = new DataInputStream(client.getInputStream());
            }
            catch (IOException e)
            {
                Logger.error(String.format("Failed to get data input stream from client: %s", e));
                return;
            }

            // attempt to get data input stream from client, quitting if unsuccessful
            final DataOutputStream dataOutputStream;
            try
            {
                dataOutputStream = new DataOutputStream(client.getOutputStream());
            }
            catch (IOException e)
            {
                Logger.error(String.format("Failed to get data output stream from client: %s", e));
                return;
            }

            // getting all the room numbers that have one or more clients connected
            final Set<Integer> activeRoomNumbers = this.roomToConnectionsMap.keySet();

            System.out.println(activeRoomNumbers);

            // send each of these room numbers to the client
            for (int roomNumber : activeRoomNumbers)
            {
                System.out.printf("%d\n", roomNumber);
                try
                {
                    dataOutputStream.writeInt(roomNumber);
                }
                catch (IOException e)
                {
                    Logger.error(String.format("Failed to send room number %d to client", roomNumber));
                    return;
                }
            }

            // send -1 to the client so that the client knows when we are done sending room numbers
            try
            {
                dataOutputStream.writeInt(-1);
            }
            catch (IOException e)
            {
                Logger.error("Failed to send finishing number -1 to client");
                return;
            }

            int clientTargetRoomNumber;
            try
            {
                clientTargetRoomNumber = dataInputStream.readInt();
            }
            catch (IOException e)
            {
                Logger.error("Failed to read client's target room number");
                return;
            }

            // add client to their inputted room number
            this.addClientToRoom(clientTargetRoomNumber, client);
            Logger.success(String.format("Added client %s to room %d", client, clientTargetRoomNumber));

            // keep reading messages from client until the connection closes
            while (!client.isClosed())
            {
                String message;
                try
                {
                    message = dataInputStream.readUTF();
                }
                catch (Exception e)
                {
                    Logger.error(String.format("Failed to read message from client %s", client));
                    continue;
                }

                Logger.success(String.format("Message received from %s in room %d: %s", client, clientTargetRoomNumber, message));

                // send the received message to every other client in the same room
                for (Socket connection : this.roomToConnectionsMap.get(clientTargetRoomNumber))
                {
                    // we don't want to send the message to the client who sent it, so skip it if its the case
                    if (client == connection) continue;

                    // try to send message to all other clients
                    try
                    {
                        DataOutputStream connectionDataOutputStream = new DataOutputStream(connection.getOutputStream());
                        connectionDataOutputStream.writeUTF(message);
                    }
                    catch (IOException e)
                    {
                        Logger.error(String.format("Failed to send message to connection %s", connection));
                    }
                }
            }

            // once the connection with the client has closed, remove the client from the room
            this.removeClientFromRoom(clientTargetRoomNumber, client);
            Logger.info(String.format("Client %s in room %d disconnected", client, clientTargetRoomNumber));

            // close all streams, connections, and buffers to client
            try
            {
                dataInputStream.close();
                dataOutputStream.close();
                client.close();
            }
            catch (IOException e)
            {
                Logger.error("Failed to close one or more streams and connections to the client");
            }
        }).start();
    }

    @Override
    public void close() throws IOException
    {
        // close all client connections
        for (HashSet<Socket> roomConnections : this.roomToConnectionsMap.values())
        {
            for (Socket connection : roomConnections)
            {
                connection.close();
            }
        }

        // close own server socket
        this.socket.close();
    }
}