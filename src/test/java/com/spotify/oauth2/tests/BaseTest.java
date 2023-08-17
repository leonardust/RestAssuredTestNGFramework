package com.spotify.oauth2.tests;

import lombok.extern.java.Log;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;

@Log
public class BaseTest {

    @BeforeMethod
    void beforeMethod(Method m) {
        log.info("STARTING TEST: " + m.getName() + " with THREAD ID: " + Thread.currentThread().getId());
    }
}
