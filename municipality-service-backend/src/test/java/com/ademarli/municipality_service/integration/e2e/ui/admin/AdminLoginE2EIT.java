package com.ademarli.municipality_service.integration.e2e.ui.admin;

import com.ademarli.municipality_service.integration.e2e.ui.support.BaseUiE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class AdminLoginE2EIT extends BaseUiE2ETest {

    private static final String TID_EMAIL_OR_PHONE = "[data-testid='auth-login-emailOrPhone']";
    private static final String TID_PASSWORD       = "[data-testid='auth-login-password']";
    private static final String TID_SUBMIT         = "[data-testid='auth-login-submit']";

    @Test
    void succesAdminLogin() {
        driver.get(UI_BASE + "/auth/login");

        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(TID_EMAIL_OR_PHONE)
        ));
        emailInput.clear();
        emailInput.sendKeys("admin@local.com");

        WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(TID_PASSWORD)
        ));
        passwordInput.clear();
        passwordInput.sendKeys("Admin123!");

        WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector(TID_SUBMIT)
        ));
        submitButton.click();

        wait.until(ExpectedConditions.urlContains("/admin"));
    }
}
