package edu.oakland.gavin;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.net.ServerSocket;

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
            try
            {
                InputStream rawInputStream = client.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(rawInputStream);

                // read the room number the client asked to join
                int clientTargetRoom = dataInputStream.readInt();
                this.addClientToRoom(clientTargetRoom, client);
                Logger.success(String.format("Client joined room %d", clientTargetRoom));

                // open input stream with client
                InputStreamReader charInputStream = new InputStreamReader(rawInputStream);

                // keep reading messages from client until the connection closes
                while (!client.isClosed())
                {
                    char[] characters = new char[1000];

                    // read characters from the client
                    int bytesRead = charInputStream.read(characters);

                    // if nothing was read, try again
                    if (bytesRead == 0) continue;

                    // if -1 was read, the stream ended
                    if (bytesRead == -1)
                    {
                        Logger.warning(String.format("Connection with %s was unexpectedly closed", client));
                        break;
                    }

                    // if execution has gotten here, it means a message was received
                    String message = new String(characters);
                    Logger.success(String.format("Message received from %s in room %d: %s", client, clientTargetRoom, message));

                    // send the received message to every other client in the same room
                    for (Socket connection : this.roomToConnectionsMap.get(clientTargetRoom))
                    {
                        // we don't want to send the message to the client who sent it, so skip it if its the case
                        if (client == connection) continue;

                        // otherwise, send the message to the client
                        OutputStreamWriter charOutputStream = new OutputStreamWriter(connection.getOutputStream());
                        charOutputStream.write(message);
                        charOutputStream.flush();
                    }
                }

                // once the connection with the client has closed, remove the client from the room
                this.removeClientFromRoom(clientTargetRoom, client);

                Logger.info(String.format("Client %s in room %d disconnected", client, clientTargetRoom));

                // close all streams and buffers to client
                dataInputStream.close();
                charInputStream.close();
                rawInputStream.close();

            }
            catch (IOException e)
            {
                Logger.error(String.format("IOException for client %s: %s", client, e));
            }
            finally
            {
                try
                {
                    // close the connection with the client at the end
                    client.close();
                }
                catch (IOException e)
                {
                    Logger.error(String.format("Failed to close socket %s", e));
                }
            }
        }).start();
    }

    @Override
    public void close() throws IOException
    {
        // close all client connections
        for (HashSet<Socket> connections : this.roomToConnectionsMap.values())
        {
            for (Socket connection : connections)
            {
                connection.close();
            }
        }

        // close own server socket
        this.socket.close();
    }
}