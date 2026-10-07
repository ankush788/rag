package com.ai.rag.controller;

import com.ai.rag.repository.KnowledgeBase;
import com.ai.rag.service.ChatService;
import com.ai.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final RagService ragService;
    private final KnowledgeBase knowledgeBase;

    // Normal chat (No RAG)
    @GetMapping("/chat")
    public String chat(@RequestParam String question) {
        return chatService.ask(question);
    }

    // RAG Chat (Uses Vector DB)
    @GetMapping("/rag")
    public String ragChat(@RequestParam String question) {
        return ragService.askWithContext(question);
    }
    
    // ETL Endpoint to load PDF data into Chroma DB
    @GetMapping("/etl")
    public String runEtl(@RequestParam(defaultValue = "faq.pdf") String filename) {
        knowledgeBase.loadPdfToVectorStore(filename);
        return "ETL Pipeline completed! Data from " + filename + " is now in Chroma DB.";
    }
}