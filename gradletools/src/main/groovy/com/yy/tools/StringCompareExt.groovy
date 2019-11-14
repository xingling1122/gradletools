package com.yy.tools

import java.util.concurrent.ConcurrentHashMap

class StringCompareExt {
    //本地语言地址
    String languagePath
    //需要更新的语言文件夹,key为本地文件夹，value为远程地址
    Map<String, String> languages = new ConcurrentHashMap<>()
    //需要更新的语言key
    List<String> updateKeys = new ArrayList<>()

    @Override
    public String toString() {
        return "StringCompareExt{" +
                "languagePath='" + languagePath + '\'' +
                ", languages=" + languages +
                ", updateKeys=" + updateKeys +
                '}'
    }
}