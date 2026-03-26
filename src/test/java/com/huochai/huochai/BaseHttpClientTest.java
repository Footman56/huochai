package com.huochai.huochai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

import okhttp3.mockwebserver.MockWebServer;


/**
 *
 *@author peilizhi
 *@date 2026/3/26 23:29
 **/
public class BaseHttpClientTest {

    protected MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    protected String getBaseUrl() {
        return mockWebServer.url("/").toString();
    }
}
