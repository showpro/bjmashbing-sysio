package com.bjmashibing.system.io;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * NIO
 */
public class SocketNIO {

    public static void main(String[] args) throws Exception {

        LinkedList<SocketChannel> clients = new LinkedList<>();

        ServerSocketChannel ss = ServerSocketChannel.open();
        ss.bind(new InetSocketAddress(9090));
        ss.configureBlocking(false); //重点  OS  NONBLOCKING!!! 设置非阻塞

        ss.setOption(StandardSocketOptions.TCP_NODELAY, false);
//        StandardSocketOptions.TCP_NODELAY
//        StandardSocketOptions.SO_KEEPALIVE
//        StandardSocketOptions.SO_LINGER
//        StandardSocketOptions.SO_RCVBUF
//        StandardSocketOptions.SO_SNDBUF
//        StandardSocketOptions.SO_REUSEADDR


        /**
         * 重点：
         * socket（服务端的listen socket<连接请求三次握手后，往我这里仍，我去通过accpet 得到 连接的socket>，连接socket<连接后的数据独写使用的>）
         *
         */



        while (true) {
            /* 接收客户端的连接 */
            Thread.sleep(1000);
            SocketChannel client = ss.accept(); //接收客户端，不会阻塞。  内核返回-1时相当于Java中的NULL
            //accept方法调用内核了：1，没有客户端连接进来时，是否有返回值？在BIO的时候一直卡着，但是在NIO，不卡着，返回-1，相当于Java中的null
            //如果有客户端连接，accept 返回的是这个客户端的fd,比如fd=5,对应java中就是一个client对象
            //NOBLOCKING 就是代码能往下走了，只不过有不同的情况

            if (client == null) {
                System.out.println("null.....");
            } else {
                //有客户端连接
                client.configureBlocking(false);//接收到了，将客户端设置为非阻塞
                int port = client.socket().getPort();
                System.out.println("client...port: " + port);
                clients.add(client);
            }

            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);  //可以在堆里   堆外

            /* 遍历已经连接进来的客户端能不能读写数据 */
            for (SocketChannel c : clients) {   //串行化！！！！  多线程！！
                int num = c.read(buffer);  // >0  -1  0   //不会阻塞
                if (num > 0) {
                    buffer.flip();
                    byte[] aaa = new byte[buffer.limit()];
                    buffer.get(aaa);

                    String b = new String(aaa);
                    System.out.println(c.socket().getPort() + " : " + b);
                    buffer.clear();
                }


            }
        }
    }

}
