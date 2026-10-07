package com.ai.rag.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBase {

    private final VectorStore vectorStore;
    private final ResourceLoader resourceLoader;

    public void loadPdfToVectorStore(String filename) {
        log.info("Starting ETL Pipeline for file: {}", filename);
        
        try {
            // 1. EXTRACT: Load the PDF file
            Resource pdfResource = resourceLoader.getResource("classpath:" + filename);
            if (!pdfResource.exists()) {
                throw new RuntimeException("File not found in resources: " + filename);
            }
            
            // cleaning part
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                pdfResource, 
                PdfDocumentReaderConfig.builder()
                    .withPageExtractedTextFormatter(
                        new org.springframework.ai.reader.ExtractedTextFormatter.Builder()
                            .withNumberOfBottomTextLinesToDelete(0)     // 0 line from bottom of page
                            .withNumberOfTopPagesToSkipBeforeDelete(0)  // 0 line from top of page
                            .build())
                    .build()
            );
            
            List<Document> documents = pdfReader.get();
            
            List<Document> cleanedDocuments = new java.util.ArrayList<>();
            for (Document document : documents) {

                String text = document.getText();

                // Remove "Page 1", "Page 2", etc.
                text = text.replaceAll("(?i)\\bPage\\s*\\d+\\b", "");

                // Remove question numbers like:
                // 1. Who is eligible?
                // 2. What documents...?
                text = text.replaceAll("(?m)^\\s*\\d+\\.\\s*", "");

                // Create a new Document with the cleaned text and original metadata
                cleanedDocuments.add(new Document(document.getId(), text.trim(), document.getMetadata()));
            }

            log.info("Extracted {} pages from the PDF", cleanedDocuments.size());

            // 3 TRANSFORM: Split the text into manageable chunks
            TokenTextSplitter textSplitter = TokenTextSplitter.builder().withChunkSize(60).build(); //By default, it chops text into chunks of about 800 tokens/words).
            List<Document> splitDocuments = textSplitter.apply(cleanedDocuments);
            log.info("Transformed into {} semantic chunks", splitDocuments.size());

            // 4. LOAD: Save the embedded chunks into Chroma DB
            log.info("Generating embeddings and saving to Chroma DB...");
            vectorStore.add(splitDocuments);
            log.info("ETL Pipeline completed successfully! Data is now in Chroma.");
            
        } catch (Exception e) {
            log.error("Failed to process the PDF: ", e);
            throw new RuntimeException("ETL Pipeline failed", e);
        }
    }
}
