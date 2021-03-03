package com.bjmashibing.system.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 多路复用器
 */
public class SocketMultiplexingSingleThreadv1 {

    //坦克 一期 二期
    private ServerSocketChannel server = null;
    private Selector selector = null; // Linux 多路复用器（select poll   epoll kqueue） nginx  envent{}
    int port = 9090;

    //初始化
    public void initServer() {
        try {
            // 服务端三部曲
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));

            //如果在epoll模型下，open 就完成了-> epoll_creat的创建 -> 返回一个fd3
            selector = Selector.open();  //  select  poll  *epoll 优先选择：epoll  -D修正

            //server 约等于 listen状态的fd4
            /**
             如果：
             select, poll: jvm里开辟一个数组 将 fd4 放进去
             epoll: 上面open的时候已经开辟了一个空间，这个时候完成 epoll_ctl(fd3,ADD,fd4,EPOLLIN)
             */
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        initServer();
        System.out.println("服务器启动了。。。。。");
        try {
            //当前一个线程
            while (true) {//死循环
                Set<SelectionKey> keys = selector.keys();
                System.out.println(keys.size()+"   size");

                //1、调多路复用器（select,poll  or  epoll(调epoll_wait)）
                while (selector.select(500) > 0) {
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = selectionKeys.iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        if (key.isAcceptable()) {
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            readHandler(key);
                            //在当前线程，这个方法可能会阻塞，如果阻塞10年，其他的IO早就没电了。。。
                            //所以，为什么提出了 IO THREADS
                            //redis 是不是也用了epoll, redis是不是有个io threads的概念，redis是不是单线程的
                            // tomcat 8，9  异步处理方式 IO  和   处理上解耦
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptHandler(SelectionKey key) {
        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel client = ssc.accept();
            client.configureBlocking(false);
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            client.register(selector, SelectionKey.OP_READ, buffer);
            System.out.println("-------------------------------------------");
            System.out.println("新客户端：" + client.getRemoteAddress());
            System.out.println("-------------------------------------------");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readHandler(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        buffer.clear();
        int read = 0;
        try {
            while (true) {
                read = client.read(buffer);
                if (read > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (read == 0) {
                    break;
                } else {
                    client.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SocketMultiplexingSingleThreadv1 service = new SocketMultiplexingSingleThreadv1();
        service.start();
    }
}
