package com.yy.tools

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

/**
 * Http工具
 */
class HttpUtil {

    static void request(String url, DataCallback<Map<String, String>> callback) {
        def http = new HTTPBuilder(url)
        http.request(Method.GET, ContentType.JSON) { req ->
            response.success = { resp, json ->
                println resp.statusLine  //查看状态
                println resp.statusLine.statusCode == 200 //判断状态码是否为200
                println "Response length: ${resp.headers.'Content-Length'}"  //获取请求头
                println json
                def code = json['code']
                def msg = json['msg']
                def data = json['data']
                if (code == 0) {
                    callback.onSuccess(data)
                } else {
                    callback.onFail(code, msg)
                }
            }

            //404
            response.'404' = { resp ->
                callback.onFail(404, "Not found")
            }

            // 401
            http.handler.'401' = { resp ->
                callback.onFail(401, "Access denied")
            }

            //其他错误，不实现则采用缺省的：抛出异常。
            http.handler.failure = { resp ->
                callback.onFail(http.handler.failure, "Unexpected failure: ${resp.statusLine}")
            }
        }
    }
}