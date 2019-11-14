package com.yy.tools

interface DataCallback<T> {
    void onSuccess(T data)

    void onFail(int code, String msg)
}