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
            languageConfig.languages.each { Map.Entry<String, String> language ->
                def locLanguagePath = languageConfig.languagePath + File.separator + language.key + File.separator + "strings.xml"
                HttpUtil.request(language.value, new DataCallback<Map<String, String>>() {
                    @Override
                    void onSuccess(Map<String, String> data) {
                        println "request json success,data:$data"
                        handleLanguage(locLanguagePath, data, languageConfig.updateKeys, language.key, languageConfig.multiParamsJsonPath)
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
                                               List<String> updateKeys, String language, String multiParamPath) {
        println "locLanguagePath:$locLanguagePath"
        def xmlParser = new XmlParser()
        def result = xmlParser.parse(locLanguagePath)
        //增量替换本地已有的key
        List<String> locKeys = new ArrayList<String>()
        result.string.each { Node stringNode ->
            def key = stringNode.attribute('name')
            locKeys.add(key)
            //说明这个key需要更新
            if (updateKeys.isEmpty() || updateKeys.contains(key)) {
                if (data.containsKey(key)) {
                    stringNode.setValue([formatContent(data.get(key))])
                    println "增量替换本地已有的key,stringNode:$stringNode"
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
                    def newNode = result.appendNode("string", [name: entry.key], [formatContent(entry.value)])
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
        if (language.equals("values")) {
            stringMultiParamData.stringMultiParamList = stringMultiParamList
            writeToFile(multiParamPath, JsonOutput.toJson(stringMultiParamData))
        }
        println "结束，resource：${result}"
        def fileWriter = new FileWriter(locLanguagePath)
        def printer = new CustomXmlNodePrinter(new PrintWriter(fileWriter))
        printer.preserveWhitespace = true
        printer.print(result)
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
        println "stringMultiParamData value:$value filename:$filename"
        def file = new File(filename)
        if (file.exists()) {
            file.delete()
        }
        def printWriter = file.newPrintWriter()
        printWriter.write(value)
        printWriter.flush()
        printWriter.close()
    }
}