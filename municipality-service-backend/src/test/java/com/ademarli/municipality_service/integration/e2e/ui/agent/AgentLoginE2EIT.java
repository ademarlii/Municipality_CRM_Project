package com.ademarli.municipality_service.integration.e2e.ui.agent;

import com.ademarli.municipality_service.integration.e2e.ui.support.BaseUiE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class AgentLoginE2EIT extends BaseUiE2ETest {

    private static final String TID_EMAIL_OR_PHONE = "[data-testid='auth-login-emailOrPhone']";
    private static final String TID_PASSWORD       = "[data-testid='auth-login-password']";
    private static final String TID_SUBMIT         = "[data-testid='auth-login-submit']";

    private static final long SLOW_MS = Long.parseLong(System.getProperty("slow.ms", "700"));

    private void pause() {
        if (SLOW_MS <= 0) return;
        try { Thread.sleep(SLOW_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @Test
    void loginAgentSuccess() {
        driver.get(UI_BASE + "/auth/login");

        WebElement emailOrPhoneInput = driver.findElement(By.cssSelector(TID_EMAIL_OR_PHONE));
        emailOrPhoneInput.sendKeys("agent@local.com");

        WebElement passwordInput = driver.findElement(By.cssSelector(TID_PASSWORD));
        passwordInput.sendKeys("Agent123!");

        WebElement submitButton = driver.findElement(By.cssSelector(TID_SUBMIT));
        submitButton.click();

        wait.until(ExpectedConditions.urlContains("agent"));
        pause();
    }
}
