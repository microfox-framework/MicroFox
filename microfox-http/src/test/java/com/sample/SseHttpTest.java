package com.sample;

import com.sample.sse.SseTask;
import ir.moke.microfox.MicroFox;
import ir.moke.microfox.api.http.sse.SseObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;

public class SseHttpTest {
    private static final Logger logger = LoggerFactory.getLogger(SseHttpTest.class);

    public static void main(String[] args) {
        /*
         * Install httpie package
         * run this command :
         * http --stream http://localhost:8080/api/sse 'Accept: text/event-stream'
         * */
        MicroFox.sseRegister("sse-test", "/api/sse");
        MicroFox.ssePublisher("sse-test", () -> new SseObject("Hello"));
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new SseTask(), 2000, 3000);
    }
}