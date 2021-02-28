package com.bjmashibing.system.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author: 马士兵教育
 * @create: 2020-05-17 05:34
 * BIO  多线程的方式
 *
 *
 */
public class SocketIOPropertites {



    //server socket listen property:
    private static final int RECEIVE_BUFFER = 10;
    private static final int SO_TIMEOUT = 0;
    private static final boolean REUSE_ADDR = false;
    private static final int BACK_LOG = 2;
    //client socket listen property on server endpoint:
    private static final boolean CLI_KEEPALIVE = false;
    private static final boolean CLI_OOB = false;
    private static final int CLI_REC_BUF = 20;
    private static final boolean CLI_REUSE_ADDR = false;
    private static final int CLI_SEND_BUF = 20;
    private static final boolean CLI_LINGER = true;
    private static final int CLI_LINGER_N = 0;
    private static final int CLI_TIMEOUT = 0;
    private static final boolean CLI_NO_DELAY = false;
/*

    StandardSocketOptions.TCP_NODELAY
    StandardSocketOptions.SO_KEEPALIVE
    StandardSocketOptions.SO_LINGER
    StandardSocketOptions.SO_RCVBUF
    StandardSocketOptions.SO_SNDBUF
    StandardSocketOptions.SO_REUSEADDR

 */


    public static void main(String[] args) {

        ServerSocket server = null;
        try {
            server = new ServerSocket();//TCP层面是启动一个监听： 0.0.0.0:9090  0.0.0.0.* Listen 本地9090对端所有端口，即任何端口号都可以连9090端口
            server.bind(new InetSocketAddress( 9090), BACK_LOG);
            server.setReceiveBufferSize(RECEIVE_BUFFER);
            server.setReuseAddress(REUSE_ADDR);
            server.setSoTimeout(SO_TIMEOUT);

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("server up use 9090!");
        while (true) {
            try {
                System.in.read();  //分水岭：

                //什么是socket？它是一个四元组（唯一性）
                //每个程序需要持有一个文件描述符fd（相当于索引），它指向内核
                //accept后拿到内核抽象代表fd，代表每一个socket,在Java中fd被包装成了一个对象Socket
                Socket client = server.accept();//接收客户端，若有客户端连接，会得到一个客户端文件描述符:listen(fd3)->fd5 /blocking，没有客户端连接则一直阻塞
                System.out.println("client port: " + client.getPort());

                client.setKeepAlive(CLI_KEEPALIVE);
                client.setOOBInline(CLI_OOB);
                client.setReceiveBufferSize(CLI_REC_BUF);
                client.setReuseAddress(CLI_REUSE_ADDR);
                client.setSendBufferSize(CLI_SEND_BUF);
                client.setSoLinger(CLI_LINGER, CLI_LINGER_N);
                client.setSoTimeout(CLI_TIMEOUT);
                client.setTcpNoDelay(CLI_NO_DELAY);

                //如果客户端进入线程去读，而直接去读数据
                //client.read    //阻塞，从而整个循环阻塞，如果下一个客户端需要接收，代码执行不动。因此可以将阻塞放到线程里面去，去线程里面读

                new Thread(
                        () -> {
                            while (true) {
                                try {
                                    InputStream in = client.getInputStream();
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                                    char[] data = new char[1024];
                                    int num = reader.read(data);

                                    if (num > 0) {
                                        System.out.println("client read some data is :" + num + " val :" + new String(data, 0, num));
                                    } else if (num == 0) {
                                        System.out.println("client readed nothing!");
                                        continue;
                                    } else {
                                        System.out.println("client readed -1...");
                                        client.close();
                                        break;
                                    }

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                ).start();

            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
