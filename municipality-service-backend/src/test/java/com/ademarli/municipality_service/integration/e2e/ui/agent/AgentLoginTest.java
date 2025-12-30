package com.ademarli.municipality_service.integration.e2e.ui.agent;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class AgentLoginTest {

    private static final String UI_BASE = System.getProperty("ui.baseUrl", "http://localhost:5173");
    private static final String API_BASE = System.getProperty("api.baseUrl", "http://localhost:6969");

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String TID_EMAIL_OR_PHONE = "[data-testid='auth-login-emailOrPhone']";
    private static final String TID_PASSWORD       = "[data-testid='auth-login-password']";
    private static final String TID_SUBMIT         = "[data-testid='auth-login-submit']";

    private static final long SLOW_MS = Long.parseLong(System.getProperty("slow.ms", "700"));

    private void pause() {
        if (SLOW_MS <= 0) return;
        try {
            Thread.sleep(SLOW_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @BeforeEach
    void setup() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        boolean headless = Boolean.parseBoolean(System.getProperty("ui.headless", "true"));
        if (headless) options.addArguments("--headless=new");

        options.addArguments("--window-size=1440,900");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void teardown() {
        if (driver != null){
            pause();
            driver.quit();
        }

    }

    @Test
    void loginAgentSuccess() {

        driver.get(UI_BASE+"/auth/login");

        WebElement emailOrPhoneInput = driver.findElement( By.cssSelector(TID_EMAIL_OR_PHONE) );
        emailOrPhoneInput.sendKeys("agent@local.com");
        WebElement passwordInput = driver.findElement(By.cssSelector(TID_PASSWORD) );
        passwordInput.sendKeys("Agent123!");
        WebElement submitButton = driver.findElement(By.cssSelector(TID_SUBMIT) );
        submitButton.click();
        wait.until(ExpectedConditions.urlContains("agent"));
    }
}
