package com.ademarli.municipality_service.integration.e2e.ui.admin;


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

public class AdminLogin {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String TID_EMAIL_OR_PHONE = "[data-testid='auth-login-emailOrPhone']";
    private static final String TID_PASSWORD       = "[data-testid='auth-login-password']";
    private static final String TID_SUBMIT         = "[data-testid='auth-login-submit']";

    @BeforeEach
    void setup() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options=new ChromeOptions();
        boolean headless = Boolean.parseBoolean(System.getProperty("ui.headless", "false"));
        if (headless) options.addArguments("--headless=new");
        options.addArguments("--window-size=1440,900");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");

        driver=new  ChromeDriver(options);
        wait=new WebDriverWait(driver, Duration.ofSeconds(10));

    }

    @AfterEach
    void teardown() {
        if (driver != null) driver.quit();
    }

    @Test
    void succesAdminLogin() {
        driver.get("http://localhost:5173/auth/login");
        WebElement emailInput=wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(TID_EMAIL_OR_PHONE)
        ));

        emailInput.clear();
        emailInput.sendKeys("admin@local.com");
        WebElement passwordInput=wait.until(ExpectedConditions.visibilityOfElementLocated(
              By.cssSelector(TID_PASSWORD)
        ));
        passwordInput.clear();
        passwordInput.sendKeys("Admin123!");
        WebElement submitButton=wait.until(ExpectedConditions.elementToBeClickable(
              By.cssSelector(TID_SUBMIT)
        ));
        submitButton.click();
        wait.until(ExpectedConditions.urlContains("/admin"));

    }


}
