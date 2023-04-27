package com.eyu.handler;

import com.eyu.exception.ChatException;

public interface ChatCompletionCallback {

    void onCompletion(String response) throws ChatException;

    void onError(ChatException chatException);
}