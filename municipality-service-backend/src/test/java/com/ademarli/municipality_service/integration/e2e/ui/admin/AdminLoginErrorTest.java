package com.ademarli.municipality_service.integration.e2e.ui.admin;

import com.ademarli.municipality_service.integration.e2e.ui.support.BaseUiE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class AdminLoginErrorTest extends BaseUiE2ETest {

    private static final String TID_EMAIL_OR_PHONE = "[data-testid='auth-login-emailOrPhone']";
    private static final String TID_PASSWORD       = "[data-testid='auth-login-password']";
    private static final String TID_TOAST_ERROR    = "[data-testid='auth-register-phone-error']";

    @Test
    void errorAdminLogin_EmptyPassword() {
        driver.get(UI_BASE + "/auth/login");

        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(TID_EMAIL_OR_PHONE)
        ));
        emailInput.clear();
        emailInput.sendKeys("admin@local.com");

        //pause();

        WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(TID_PASSWORD)
        ));
        passwordInput.clear();
        passwordInput.sendKeys("");

        //pause();
        emailInput.click();

        WebElement toastError = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(TID_TOAST_ERROR)
        ));

        assert (toastError.getText().contains("Şifre gerekli"));
        //pause();
    }

    void pause() {
        try { Thread.sleep(1000); } catch (InterruptedException e) { throw new RuntimeException(e); }
    }
}
