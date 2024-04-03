package edu.oakland.gavin;

import java.io.*;
import java.util.Scanner;
import java.util.ArrayList;

public class App
{
    public static void main(String[] args)
    {
        // create client with server connection on port 9999
        try (Client client = new Client())
        {
            Logger.success("Connected to server");

            ArrayList<Integer> activeRoomNumbers = new ArrayList<>();
            ArrayList<Integer> roomSizes = new ArrayList<>();

            // keep reading room numbers from the server until we get a -1 (meaning the server is done sending them)
            while (true)
            {
                try
                {
                    int roomNumber = client.readInteger();

                    // if we get -1, there are no more rooms, don't try to read room size
                    if (roomNumber == -1)
                    {
                        break;
                    }
                    else
                    {
                        // otherwise, read how many other users are in the room and record it

                        int roomSize = client.readInteger();

                        activeRoomNumbers.add(roomNumber);
                        roomSizes.add(roomSize);
                    }
                }
                catch (IOException e)
                {
                    Logger.error("Failed to read room number and/or size of room from server");
                    return;
                }
            }

            // print out the active room numbers to the user, if there are any
            if (activeRoomNumbers.isEmpty())
            {
                Logger.info("There are currently no rooms with any active users");
            }
            else
            {
                // iterate through each active room and print its room number and how many people are in it
                for (int i = 0; i < activeRoomNumbers.size(); i++)
                {
                    Logger.info(String.format("Room #%d has %d client(s) connected", activeRoomNumbers.get(i), roomSizes.get(i)));
                }
            }

            // reading the room number they want to join from the user
            Scanner scanner = new Scanner(System.in);

            // keep asking the user which room to join until they give a positive valid room number
            while (!client.hasJoinedRoom())
            {
                System.out.print("\nEnter any room number to join (positive integers only): ");

                try
                {
                    client.joinRoom(Integer.parseInt(scanner.nextLine()));

                    if (!client.hasJoinedRoom())
                    {
                        Logger.warning("Please input a valid room number (positive integers only)");
                        continue;
                    }
                }
                catch (Exception e)
                {
                    Logger.warning("Please input a valid room number");
                    continue;
                }
            }

            // try sending the room number to the server
            try
            {
                client.sendInteger(client.getJoinedRoomNumber());
            }
            catch (IOException e)
            {
                Logger.error(String.format("Failed to send the room number to server: %d", client.getJoinedRoomNumber()));

                return;
            }

            Logger.success(String.format("\nJoined room %d\n", client.getJoinedRoomNumber()));

            // spawning thread to simultaneously receive messages as well as send them
            new Thread(() ->
            {
                while (!client.getSocket().isClosed())
                {
                    // try to read messages from the server and print them
                    String message;
                    try
                    {
                        message = client.readMessage();
                        Logger.info(String.format("Received message: \"%s\"", message));
                    }
                    catch (IOException e)
                    {
                        Logger.error("Failed to read message from server");

                        try
                        {
                            client.close();
                        }
                        catch (IOException e2)
                        {
                            Logger.error("Failed to close one or more streams, connections, and/or buffers");
                        }

                        return;
                    }
                }
            }).start();

            Logger.info("Type a message and press enter to send it in the room. Or type \"quit\" to quit the application");

            // getting input from user until the connection with the server closes
            while (!client.getSocket().isClosed())
            {
                String message = scanner.nextLine();

                if (message.equalsIgnoreCase("quit"))
                {
                    break;
                }

                try
                {
                    client.sendMessage(message);
                }
                catch (IOException e)
                {
                    Logger.error(String.format("Failed to send message \"%s\" to server", message));
                }

                Logger.success(String.format("Sent message: \"%s\"", message));
            }

            Logger.info("Disconnected from server");
        }
        catch (IOException e)
        {
            Logger.error("Failed to instantiate or destroy client object");
        }
    }
}
