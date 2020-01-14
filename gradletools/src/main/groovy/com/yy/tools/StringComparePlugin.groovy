package com.yy.tools

import org.gradle.api.Plugin
import org.gradle.api.Project
import groovy.json.JsonOutput

/**
 * 字符串增量更新工具
 */
class StringComparePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        StringCompareExt languageConfig = project.extensions.create("languageConfig", StringCompareExt)
        project.task([group: 'gradletools'], "StringCompare").doLast {
            println "languageConfig:$languageConfig"

            def defaultLanguage = null
            //拉取默认的多语言平台内容
            HttpUtil.request(languageConfig.defaultRemoteLanguagePath, new DataCallback<Map<String, String>>() {
                @Override
                void onSuccess(Map<String, String> data) {
                    println "request default json success,data:$data"
                    defaultLanguage = data
                }

                @Override
                void onFail(int code, String msg) {
                    println "request default json fail,code:$code,msg:$msg"
                }
            })

            languageConfig.languages.each { Map.Entry<String, String> language ->
                def locLanguagePath = languageConfig.languagePath + File.separator + language.key + File.separator + "strings.xml"
                //拉取对应的多语言平台内容
                HttpUtil.request(language.value, new DataCallback<Map<String, String>>() {
                    @Override
                    void onSuccess(Map<String, String> data) {
                        println "request json success,data:$data"
                        def multiLanguagePath = project.rootDir.path + File.separator + 'multiLanguage'
                        def file = new File(multiLanguagePath)
                        if (!file.exists()) {
                            file.mkdir()
                        }
                        def waitUpdateKeysPath = file.path + File.separator + language.value.substring(language.value.lastIndexOf('/') + 1)
                        println "waitUpdateKeysPath:$waitUpdateKeysPath"
                        handleLanguage(locLanguagePath, data, languageConfig.updateKeys, language.key, languageConfig.multiParamsJsonPath, languageConfig.removeLanguagePath, defaultLanguage, waitUpdateKeysPath)
                    }

                    @Override
                    void onFail(int code, String msg) {
                        println "request json fail,code:$code,msg:$msg"
                    }
                })
            }
        }
    }

    /**
     * 开始处理
     * @param locLanguagePath
     * @param data
     */
    static StringMultiParamData handleLanguage(String locLanguagePath, Map<String, String> data,
                                               List<String> updateKeys, String language, String multiParamPath, String removeLanguagePath, Map<String, String> defaultLanguage, String waitUpdateKeysPath) {
        println "locLanguagePath:$locLanguagePath"
        def xmlParser = new XmlParser()
        def result = xmlParser.parse(locLanguagePath)
        def waitUpdateKeys = new ArrayList<String>()
        //增量替换本地已有的key
        List<String> locKeys = new ArrayList<String>()
        result.string.each { Node stringNode ->
            def key = stringNode.attribute('name')
            locKeys.add(key)
            //说明这个key需要更新
            if (updateKeys.isEmpty() || updateKeys.contains(key)) {
                if (data.containsKey(key)) {
                    def replaceValue = formatContent(data.get(key))
                    if (replaceValue.isEmpty()) {
                        waitUpdateKeys.add(key)
                        def defaultValue = null
                        if (defaultLanguage != null && defaultLanguage.containsKey(key)) {
                            defaultValue = defaultLanguage.get(key)
                        }
                        if (!defaultValue.isEmpty()) {
                            replaceValue = formatContent(defaultValue)
                        } else {
                            replaceValue = "多语言待更新"
                        }
                    }
                    stringNode.setValue([replaceValue])
                    println "增量替换本地已有的key,stringNode:$stringNode"
                } else {
                    result.remove(stringNode)
                    println "多语言系统不存在，直接删除$stringNode"
                }
            }
        }
        //增加多语言平台新增的key
        println "开始增量新增，locKeys:$locKeys"

        StringMultiParamData stringMultiParamData = new StringMultiParamData()
        stringMultiParamData.language = language
        List<StringMultiParam> stringMultiParamList = new ArrayList<StringMultiParam>()
        data.each { Map.Entry<String, String> entry ->
            //说明这个key需要更新
            if (updateKeys.isEmpty() || updateKeys.contains(entry.key)) {
                if (!locKeys.contains(entry.key)) {
                    def addValue = formatContent(entry.value)
                    if (addValue.isEmpty()) {
                        waitUpdateKeys.add(entry.key)
                        def defaultValue = null
                        if (defaultLanguage != null && defaultLanguage.containsKey(entry.key)) {
                            defaultValue = defaultLanguage.get(entry.key)
                        }
                        if (!defaultValue.isEmpty()) {
                            addValue = formatContent(defaultValue)
                        } else {
                            addValue = "多语言待更新"
                        }
                    }
                    def newNode = result.appendNode("string", [name: entry.key], [addValue])
                    println "增加多语言平台新增的key,newNode:$newNode"
                }
            }
            StringMultiParam stringMultiParam = new StringMultiParam()
            stringMultiParam.key = entry.key
            stringMultiParam.paramCount = getSubCount(entry.value, "%")
            println "stringMultiParamData paramCount:$stringMultiParam.paramCount"
            if (stringMultiParam.paramCount > 0) {
                stringMultiParamList.add(stringMultiParam)
            }
        }
        //保存多语言待更新key
        writeToFile(waitUpdateKeysPath, JsonOutput.toJson(waitUpdateKeys))

        if (language.equals("values")) {
            stringMultiParamData.stringMultiParamList = stringMultiParamList
            writeToFile(multiParamPath, JsonOutput.toJson(stringMultiParamData))
        }
        println "结束，resource：${result}"
        save(locLanguagePath, result)

        //删除指定路径的key
        if (removeLanguagePath != null && !removeLanguagePath.isEmpty()) {
            def removeResult = xmlParser.parse(removeLanguagePath)
            removeResult.string.each { Node stringNode ->
                def key = stringNode.attribute('name')
                //说明这个key需要更新
                if (updateKeys.isEmpty() || updateKeys.contains(key)) {
                    if (data.containsKey(key)) {
                        removeResult.remove(stringNode)
                        println "删除指定路径的key,stringNode:$stringNode"
                    }
                }
            }
            println "结束，removeResult：${removeResult}"
            save(removeLanguagePath, removeResult)
        }
    }

    /**
     * 格式化内容，, '&': '&amp;'这个发现写进去string.xml后会自动转，所以这里不转
     * @param content
     * @return
     */
    static String formatContent(String content) {
        def result = content.trim()
        def escapeMap = ['\\>' : '\\&gt;',
                         '>'   : '\\&gt;',
                         '\\<' : '\\&lt;',
                         '<'   : '\\&lt;',
                         '\\\'': '\\&apos;',
                         '\''  : '\\&apos;',
                         '\\"' : '\\&quot;',
                         '"'   : '\\&quot;',
                         '%@'  : '%s']
        escapeMap.each { Map.Entry<String, String> entry ->
            println "old:${entry.key},new:${entry.value}"
            result = result.replace(entry.key, entry.value)
        }
        println "formatContent,content:$content,result:$result"
        return result
    }

    static int getSubCount(String str, String key) {
        int count = 0;
        for (int index = 0; (index = str.indexOf(key, index)) != -1; ++count) {
            index += key.length();
        }
        return count;
    }

    static void writeToFile(String filename, String value) {
        if (filename == null || filename.isEmpty()) {
            println "filename is null"
            return
        }
        println "writeToFile value:$value filename:$filename"
        def file = new File(filename)
        if (file.exists()) {
            file.delete()
        }
        def printWriter = file.newPrintWriter()
        printWriter.write(value)
        printWriter.flush()
        printWriter.close()
    }

    static void save(String path, Node result) {
//        def fileWriter = new FileWriter(path)
        def writer = new PrintWriter(path, "utf-8")
        def printer = new CustomXmlNodePrinter(writer)
        printer.preserveWhitespace = true
        printer.print(result)
//        fileWriter.close()
        writer.close()
    }
}