package edu.oakland.gavin;

public class Logger
{
    public static void info(String message)
    {
        System.out.printf("[*] %s\n", message);
    }

    public static void warning(String message)
    {
        System.out.printf("[-] %s\n", message);
    }

    public static void error(String message)
    {
        System.out.printf("[!] %s\n", message);
    }

    public static void success(String message)
    {
        System.out.printf("[+] %s\n", message);
    }
}