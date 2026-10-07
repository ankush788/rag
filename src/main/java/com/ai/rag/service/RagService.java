package com.ai.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {
   // lambok create constructor and their the beans intialize
    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public String askWithContext(String question) {
        log.info("Received RAG question: {}", question);

        // 1. Retrieve similar documents from Chroma DB
        List<Document> similarDocuments = vectorStore.similaritySearch(
            SearchRequest.builder().query(question).topK(3).build()
        );
        
        log.info("Found {} relevant documents in Chroma DB", similarDocuments.size());

        // Combine the text of all retrieved documents
        String context = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // 2. Create the Prompt with Context
        String template = """
                You are a helpful assistant. Use the provided context to answer the user's question.
                If the answer is not in the context, say "I don't have enough information to answer that based on the provided documents."
                
                CONTEXT:
                {context}
                
                QUESTION:
                {question}
                """;

        PromptTemplate promptTemplate = new PromptTemplate(template);
        Prompt prompt = promptTemplate.create(Map.of(
                "context", context,
                "question", question
        ));

        // 3. Send to Gemini and return response
        log.info("Sending prompt to Gemini with RAG context... {}", prompt);
        return chatClient.prompt(prompt).call().content();
    }
}
