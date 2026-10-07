package com.ai.rag.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;

    public String ask(String question) {

        return chatClient
                .prompt(question)
                .call()
                .content();
    }
}