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
 * 服务端
 */
public class SocketIOPropertites {



    //server socket listen property:
    private static final int RECEIVE_BUFFER = 10;
    private static final int SO_TIMEOUT = 0;
    private static final boolean REUSE_ADDR = false;
    //分配不处理、等待的为2个，也就是说不分配文件描述符的为2个，超过的就拒绝丢弃掉
    private static final int BACK_LOG = 2;
    //client socket listen property on server endpoint:
    //心跳，是否为长链接，false:否
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
            // 创建一个socket后会返回一个文件描述符
            server = new ServerSocket();//TCP层面是启动一个监听： 0.0.0.0:9090  0.0.0.0.* Listen 本地9090对端所有端口，即任何端口号都可以连9090端口。
                                        // 程序启动后，分配一个进程（有PID 如7932）去使用，并有一个对应的文件描述符
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
                //上面过程只是得到了server，此时该进程7932还未接收客户端的连接。
                //如果客户端已经连接了服务端，通过netstat -natp观察发现客户端与服务端会产生一个socket，建立起连接了，但是这个socket还没有分配给谁（这里分配给进程7932）去使用（没有被谁接收，没有文件描述符对应），但是内核态中已经有它了
                //如果客户端发送4个字节的数据，即便建立的socket没有分配谁使用，没有描述符对应，但是数据会被内核Recv-Q接收到了。
                //由此可知，进程7932虽还未【接收】socket的连接，但内核中已经完成了资源的初步使用。TCP协议是面向连接的
                System.in.read();  //分水岭：这里会阻塞住了，下面的server.accept()不会促发。在主服务中等待用户的输入，输入了才走下边的代码

                //什么是socket？它是一个四元组（唯一性）
                //"每个程序需要持有一个文件描述符fd（相当于索引），它指向内核"
                //程序7932 accept后，会拿到内核抽象代表fd，代表每一个socket,在Java中fd被包装成了一个对象Socket
                Socket client = server.accept();//【接收客户端】，若有客户端连接，会得到一个客户端文件描述符:listen(fd3)->fd5 /blocking，
                                                // 【阻塞的】，即没有返回-1的可能性。没有客户端连接则一直卡着不动，系统调用内核了 accept(4, 会一直卡着，没有返回值，这里返回值就是客户端文件描述符
                System.out.println("client port: " + client.getPort());

                client.setKeepAlive(CLI_KEEPALIVE);
                client.setOOBInline(CLI_OOB);
                client.setReceiveBufferSize(CLI_REC_BUF);
                client.setReuseAddress(CLI_REUSE_ADDR);
                client.setSendBufferSize(CLI_SEND_BUF);
                client.setSoLinger(CLI_LINGER, CLI_LINGER_N);
                client.setSoTimeout(CLI_TIMEOUT);
                client.setTcpNoDelay(CLI_NO_DELAY);

                /**
                 * client.read() // 【阻塞】 没有-1返回值
                 * 如果客户端发送的数据不是进入线程去读，而直接去读数据，
                 * 如果客户端没有发送数据，系统底层调用read没有读到数据会一直阻塞 reader.read(data);
                 * 从而整个循环阻塞，如果下一个客户端需要接收，无法重新进入while循环，代码执行不动。
                 * 因此可以将阻塞放到新的线程里面去，去线程里面读
                 * 弊端：客户端连接多的话，这样会抛出很多线程，其根源就在阻塞上   【BIO 因为阻塞，所以抛线程，才能满足一个服务端接收更多的客户端去处理】
                 */
                new Thread(
                        () -> {
                            while (true) {
                                try {
                                    //从客户端连接中拿到输入流
                                    InputStream in = client.getInputStream();
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                                    char[] data = new char[1024];
                                    int num = reader.read(data);//阻塞

                                    if (num > 0) {
                                        System.out.println("client read some data is :" + num + " val :" + new String(data, 0, num));
                                    } else if (num == 0) {
                                        System.out.println("client readed nothing!");
                                        continue;
                                    } else {
                                        //客户端断开了
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
