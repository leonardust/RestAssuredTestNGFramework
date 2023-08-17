package com.spotify.oauth2.utils;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class TestRetryAnalyzer implements IRetryAnalyzer {

    private int retriesCounter = 0;
    private static final int maxRetries = 2;

    @Override
    public boolean retry(ITestResult iTestResult) {
        if(retriesCounter < maxRetries) {
            retriesCounter++;
            return true;
        }
        return false;
    }
}
