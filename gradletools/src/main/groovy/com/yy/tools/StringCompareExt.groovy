package com.yy.tools

import java.util.concurrent.ConcurrentHashMap

class StringCompareExt {
    //本地语言地址
    String languagePath
    //多参数文件输出地址
    String multiParamsJsonPath
    //需要更新的语言文件夹,key为本地文件夹，value为远程地址
    Map<String, String> languages = new ConcurrentHashMap<>()
    //需要更新的语言key
    List<String> updateKeys = new ArrayList<>()
    //从该路径直接移除多语言平台已有的key
    String removeLanguagePath
    //默认远程多语言地址
    String defaultRemoteLanguagePath

    @Override
    public String toString() {
        return "StringCompareExt{" +
                "languagePath='" + languagePath + '\'' +
                ", multiParamsJsonPath='" + multiParamsJsonPath + '\'' +
                ", languages=" + languages +
                ", updateKeys=" + updateKeys +
                ", removeLanguagePath='" + removeLanguagePath + '\'' +
                ", defaultRemoteLanguagePath='" + defaultRemoteLanguagePath + '\'' +
                '}';
    }
}