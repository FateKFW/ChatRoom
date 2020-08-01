package com.rdz.server;

import com.rdz.common.InfoStruct;
import com.rdz.common.LinkType;
import com.rdz.common.MessageType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ChatRoomServer {
    //服务端端口号
    private final static int PORT = 6666;

    //客户端集合
    private Map<String, String> users = new HashMap<>();
    private Map<String, SocketChannel> userSocket = new HashMap<>();

    //时间格式化
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    //文本模板
    private String textTemplate = "%s %s说：%s";

    //协议读取
    private ByteBuffer pactBuff = ByteBuffer.allocate(4);

    public static void main(String[] args) {
        ChatRoomServer server = new ChatRoomServer();
        server.startServer();
    }

    //开启服务
    public void startServer() {
        try {
            //开启服务端，绑定端口
            ServerSocketChannel server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(PORT));
            server.configureBlocking(false);

            //将服务端注册到selector
            Selector selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("服务端启动成功");

            while(selector.select()>0) {
                //获取可用的通道
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while(keys.hasNext()) {
                    handleSelector(keys.next());
                    keys.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理通道
     * @param key
     */
    public void handleSelector(SelectionKey key) {
        if(key.isAcceptable()){ //有新连接进入
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel sc;
            try {
                sc = ssc.accept();
                sc.configureBlocking(false);
                sc.register(key.selector(), SelectionKey.OP_READ);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (key.isReadable()) {
            handleRead(key);
        }
    }

    /**
     * 处理读
     * @param key
     * @return
     * @throws IOException
     */
    public boolean handleRead(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        try {
            int pos = -1;
            if ((pos = sc.read(pactBuff))>0) {
                //解包
                //连接类型
                pactBuff.flip();
                String linkType = new String(pactBuff.array());
                pactBuff.clear();

                //正文类型
                sc.read(pactBuff);
                pactBuff.flip();
                String contentType = new String(pactBuff.array());
                pactBuff.clear();

                //正文长度
                sc.read(pactBuff);
                pactBuff.flip();
                int contentLength = InfoStruct.byte2Int(pactBuff.array());
                pactBuff.clear();

                //读取正文
                if (LinkType.INIT.getStr().equals(linkType)) {
                    handleClientInit(sc, contentType, contentLength);
                } else if (LinkType.CLOSE.getStr().equals(linkType)) {
                    handleClientClose(key, sc, contentType, contentLength);
                } else if (LinkType.MESSAGE.getStr().equals(linkType)) {
                    handleClientTransport(sc, contentType, contentLength);
                }
                return true;
            } else {
                System.out.println("有客户机读取异常");
                sc.close();
                key.cancel();
                return false;
            }
        } catch (IOException e) {
            System.out.println("有客户机强制下线");
            try {
                sc.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            key.cancel();
        }

        return false;
    }

    /**
     * 客户端初始化(第一次传输数据)
     * @param sc
     * @param contentType
     * @param contentLength
     */
    private void handleClientInit(SocketChannel sc, String contentType , int contentLength) throws IOException {
        ByteBuffer buff = ByteBuffer.allocate(contentLength);
        sc.read(buff);
        buff.flip();
        String[] clientInfo = new String(buff.array()).split(InfoStruct.BREAK_STR);
        users.put(clientInfo[0], clientInfo[1]);
        userSocket.put(clientInfo[0], sc);
        System.out.println(clientInfo[1]+"上线");
    }

    /**
     * 处理客户端关闭
     * @param key
     * @param sc
     * @param contentType
     * @param contentLength
     */
    private void handleClientClose(SelectionKey key, SocketChannel sc, String contentType, int contentLength) throws IOException {
        ByteBuffer buff = ByteBuffer.allocate(contentLength);
        sc.read(buff);
        buff.flip();
        String[] clientInfo = new String(buff.array()).split(InfoStruct.BREAK_STR);

        ByteBuffer closeInfo = InfoStruct.getByteBuff(LinkType.CLOSE, MessageType.TEXT, "close");
        closeInfo.flip();
        sc.write(closeInfo);

        users.remove(clientInfo[0]);
        userSocket.remove(clientInfo[0]);
        sc.close();
        key.cancel();
        System.out.println(clientInfo[1]+"下线");
    }

    /**
     * 处理客户端数据传输
     * @param sc
     * @param contentType
     * @param contentLength
     */
    private void handleClientTransport(SocketChannel sc, String contentType, int contentLength) throws IOException {
        ByteBuffer buff = null;
        if(MessageType.TEXT.getStr().equals(contentType)) {
            buff = ByteBuffer.allocate(contentLength);
            sc.read(buff);
            buff.flip();
            String[] message = new String(buff.array()).split(InfoStruct.BREAK_STR);
            sendTextMessage(message[0], message[1]);
        } else {
            //TODO:
        }
    }

    /**
     * 发送文本信息
     * @param sender    发送者
     * @param content   发送内容
     * @throws IOException
     */
    private void sendTextMessage(String sender, String content) throws IOException {
        ByteBuffer write;
        for (Map.Entry<String, SocketChannel> entry : userSocket.entrySet()) {
            if(!entry.getKey().equals(sender)) {
                write = InfoStruct.getByteBuff(
                        LinkType.MESSAGE, MessageType.TEXT,
                        String.format(textTemplate, dtf.format(LocalDateTime.now()), users.get(sender), content));
                write.flip();
                entry.getValue().write(write);
            }
        }
    }

}
