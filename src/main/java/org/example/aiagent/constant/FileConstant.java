package org.example.aiagent.constant;

import java.nio.file.Paths;

/**
 * 文件常量
 */
public interface FileConstant {

    //文件保存目录
    String FILE_SAVE_DIR= Paths.get(System.getProperty("user.dir"),"tmp","file").toString();
}
