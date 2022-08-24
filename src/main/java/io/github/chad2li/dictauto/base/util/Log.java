package io.github.chad2li.dictauto.base.util;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ArrayUtil;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringJoiner;

/**
 * 测试使用的日志输出工具
 *
 * @author chad
 * @date 2022/5/14 00:16
 * @since 1 create by chad
 */
public class Log {
    private static BufferedWriter WRITER;
    private static Messager messager;

    private static final String TAG = "DictAuto: ";
    /**
     * 是否允许调试
     */
    private static final boolean IS_DEBUG = false;

    public static void init(Messager messager) {
        Log.messager = messager;

        initWriter();
    }

    private static void initWriter() {
        if (!IS_DEBUG) {
            // 未开启调试
            return;
        }
        if (null != Log.WRITER) {
            return;
        }
        synchronized (Log.class) {
            if (null != Log.WRITER) {
                return;
            }
        }
        // todo jar目录
        String name = Log.class.getClassLoader().getResource(".").toString();
        name += "/processor/" + System.currentTimeMillis() + ".log";
//        name = "D:\\tmp\\processor\\" + System.currentTimeMillis() + ".log";
        name = "/tmp/processor/" + System.currentTimeMillis() + ".log";
        File file = new File(name);
        try {
            if (!file.getParentFile().isDirectory()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            WRITER = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            Log.messager.printMessage(Diagnostic.Kind.WARNING, ExceptionUtil.stacktraceToString(e));
            throw new RuntimeException(e);
        }
    }

    /**
     * 输出调试测试语句
     *
     * @param msg 消息
     * @date 2022/5/19 12:22
     * @author chad
     * @since 1 by chad at 2022/5/19
     */
    public static void write(String... msg) {
        if (!IS_DEBUG) {
            // 未开启调试
            return;
        }
        if (ArrayUtil.isEmpty(msg)) {
            return;
        }
        try {
            StringJoiner sj = new StringJoiner("\n");
            for (String s : msg) {
                sj.add(TAG + s);
            }
            WRITER.write(sj.toString());
            WRITER.flush();
            Log.messager.printMessage(Diagnostic.Kind.NOTE, sj.toString());
        } catch (IOException e) {
            Log.messager.printMessage(Diagnostic.Kind.WARNING, ExceptionUtil.stacktraceToString(e));
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
