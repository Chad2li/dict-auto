package com.github.chad2li.dictauto.base.util;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ArrayUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 测试使用的日志输出工具
 *
 * @author chad
 * @date 2022/5/14 00:16
 * @since 1 create by chad
 */
public class Log {
    private static final BufferedWriter WRITER;

    static {
        // todo jar目录
        String name = Log.class.getClassLoader().getResource(".").toString();
        name += "/processor/" + System.currentTimeMillis() + ".log";
        File file = new File(name);
        try {
            if (!file.getParentFile().isDirectory()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            WRITER = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 输出调试测试语句
     *
     * @param msg
     * @date 2022/5/19 12:22
     * @author chad
     * @since 1 by chad at 2022/5/19
     */
    public static void write(String... msg) {
        if (ArrayUtil.isEmpty(msg)) {
            return;
        }
        try {
            for (String s : msg) {
                WRITER.write(s);
                WRITER.write("\n");
            }
            WRITER.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 输出调试信息和异常信息
     *
     * @param msg       调试信息
     * @param throwable 异常
     * @date 2022/5/19 12:22
     * @author chad
     * @since 1 by chad at 2022/5/19
     */
    public static void write(String msg, Throwable throwable) {
        write(msg);
        write(throwable);
    }

    /**
     * 输入异常信息
     *
     * @param throwable 异常
     * @date 2022/5/19 12:22
     * @author chad
     * @since 1 by chad at 2022/5/19
     */
    public static void write(Throwable throwable) {
        String errMsg = ExceptionUtil.stacktraceToString(throwable);
        write(errMsg);
    }
}
