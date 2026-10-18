package com.verilens.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.verilens.dto.LoginRequest;
import com.verilens.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DocumentController.
 * Tests upload, retrieval, deletion, and access control.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() throws Exception {
        authToken = registerAndLogin("docuser" + System.nanoTime(), "DocUser123!");
        otherUserToken = registerAndLogin("otheruser" + System.nanoTime(), "OtherPass123!");
    }

    private String registerAndLogin(String username, String password) throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(username);
        reg.setEmail(username + "@example.com");
        reg.setPassword(password);
        reg.setFullName("User " + username);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    // ==================== UPLOAD TESTS ====================

    @Test
    void upload_authenticatedWithValidFile_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-doc.pdf", "application/pdf",
                "PDF content here".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("documentType", "IDENTITY")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.filename").value("test-doc.pdf"))
                .andExpect(jsonPath("$.documentType").value("IDENTITY"))
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.uploadedAt").exists());
    }

    @Test
    void upload_unauthenticated_returns401or403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-doc.pdf", "application/pdf",
                "PDF content".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("documentType", "IDENTITY"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void upload_invalidFileType_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream",
                "binary content".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("documentType", "IDENTITY")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_jpegFile_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg",
                "JPEG content".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("documentType", "IDENTITY")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mimeType").value("image/jpeg"));
    }

    @Test
    void upload_pngFile_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.png", "image/png",
                "PNG content".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("documentType", "CERTIFICATE")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated());
    }

    // ==================== RETRIEVAL TESTS ====================

    @Test
    void listDocuments_authenticated_returnsPage() throws Exception {
        // Upload a document first
        MockMultipartFile file = new MockMultipartFile(
                "file", "list-test.pdf", "application/pdf",
                "PDF content".getBytes());

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("documentType", "IDENTITY")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated());

        // List documents
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void getDocument_owner_returnsDocument() throws Exception {
        // Upload
        MockMultipartFile file = new MockMultipartFile(
                "file", "detail-test.pdf", "application/pdf",
                "PDF content".getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("documentType", "IDENTITY")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andReturn();

        String docId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .get("id").asText();

        // Get details
        mockMvc.perform(get("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId));
    }

    @Test
    void getDocument_otherUser_returns404() throws Exception {
        // Upload as first user
        MockMultipartFile file = new MockMultipartFile(
                "file", "private-doc.pdf", "application/pdf",
                "PDF content".getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("documentType", "IDENTITY")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andReturn();

        String docId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .get("id").asText();

        // Try to access as different user — should fail
        mockMvc.perform(get("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE TESTS ====================

    @Test
    void deleteDocument_owner_returns204() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "delete-test.pdf", "application/pdf",
                "PDF content".getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .param("documentType", "IDENTITY")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andReturn();

        String docId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(delete("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }
}
