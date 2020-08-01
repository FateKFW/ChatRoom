package com.rdz.common;

import java.nio.ByteBuffer;

public class InfoStruct {
    //间断符
    public final static String BREAK_STR = "#";

    //组合为bytebuff
    public static ByteBuffer getByteBuff(LinkType lt, MessageType mt, byte[] bytes) {
        //前4个字节代表链接类型
        byte[] connType = lt.getStr().getBytes();
        //5-8个字节代表内容类型
        byte[] mesType = mt.getStr().getBytes();
        //9-12个字节代表内容字节大小
        byte[] connLength = int2Byte(bytes.length);
        //组合后传输的内容
        byte[] result = byteConcat(connType, mesType, connLength, bytes);
        ByteBuffer buff = ByteBuffer.allocate(4+4+4+bytes.length);
        buff.put(result);
        return buff;
    }

    //组合为bytebuff
    public static ByteBuffer getByteBuff(LinkType lt, MessageType mt, String content) {
        byte[] bytes = content.getBytes();
        //前4个字节代表链接类型
        byte[] connType = lt.getStr().getBytes();
        //5-8个字节代表内容类型
        byte[] mesType = mt.getStr().getBytes();
        //9-12个字节代表内容字节大小
        byte[] connLength = int2Byte(bytes.length);
        //组合后传输的内容
        byte[] result = byteConcat(connType, mesType, connLength, bytes);
        ByteBuffer buff = ByteBuffer.allocate(4+4+4+bytes.length);
        buff.put(result);
        return buff;
    }

    //拆分内容
    public static Object[] getByte(ByteBuffer buff) {
        Object[] result = new Object[3];
        buff.flip();
        //读取传输类型
        byte[] temp = new byte[4];
        buff.get(temp);
        result[0] = new String(temp);
        //读取传输正文长度
        temp = new byte[4];
        buff.get(temp);
        result[1] = byte2Int(temp);
        //读取传输内容
        temp = new byte[buff.limit() - buff.position()];
        buff.get(temp);
        result[2] = temp;
        return result;
    }

    /**
     * 组合byte数组
     * @param bytes
     * @return
     */
    public static byte[] byteConcat(byte[] ...bytes){
        int total = 0;
        for (byte[] bs:bytes) {
            total += bs.length;
        }

        byte[] result = new byte[total];

        int len = 0;
        for (byte[] bs:bytes) {
            System.arraycopy(bs, 0, result, len, bs.length);
            len += bs.length;
        }

        return result;
    }

    /**
     * int转byte数组
     * @param number
     * @return
     */
    public static byte[] int2Byte(int number) {
        int temp = number;
        byte[] b = new byte[4];
        for (int i = 0; i < b.length; i++) {
            b[i] = new Integer(temp & 0xff).byteValue();// 将最低位保存在最低位
            temp = temp >> 8;// 向右移8位
        }
        return b;
    }

    /**
     * byte数组转int
     * @param bytes
     * @return
     */
    public static int byte2Int(byte[] bytes) {
        int s = 0;
        int s0 = bytes[0] & 0xff;// 最低位
        int s1 = bytes[1] & 0xff;
        int s2 = bytes[2] & 0xff;
        int s3 = bytes[3] & 0xff;
        s3 <<= 24;
        s2 <<= 16;
        s1 <<= 8;
        s = s0 | s1 | s2 | s3;
        return s;
    }
}