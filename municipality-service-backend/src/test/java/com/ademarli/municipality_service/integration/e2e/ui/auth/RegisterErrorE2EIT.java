package com.ademarli.municipality_service.integration.e2e.ui.auth;

import com.ademarli.municipality_service.integration.e2e.ui.support.BaseUiE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class RegisterErrorE2EIT extends BaseUiE2ETest {

    private static final String REGISTER_EMAIL            = "[data-testid='auth-register-email']";
    private static final String REGISTER_PHONE            = "[data-testid='auth-register-phone']";
    private static final String REGISTER_PASSWORD         = "[data-testid='auth-register-password']";
    private static final String REGISTER_PASSWORD_CONFIRM = "[data-testid='auth-register-confirm']";

    @Test
    void registerError_emptyPhoneFields() {
        driver.get(UI_BASE + "/auth/register");

        WebElement email = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(REGISTER_EMAIL)));
        email.clear();
        email.sendKeys("adem@gmail.com");

        WebElement phone = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(REGISTER_PHONE)));
        phone.clear();
        phone.sendKeys("");

        email.click();

        WebElement err = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='auth-register-phone-error']")
        ));
        assert err.getText().contains("gerekli");
    }

    @Test
    void registerError_passwordMismatch() {
        driver.get(UI_BASE + "/auth/register");

        WebElement email = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(REGISTER_EMAIL)));
        email.clear();
        email.sendKeys("adem@gmail.com");

        WebElement phone = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(REGISTER_PHONE)));
        phone.clear();
        phone.sendKeys("5551234567");

        WebElement password = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(REGISTER_PASSWORD)));
        password.clear();
        password.sendKeys("Password123");

        WebElement confirm = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(REGISTER_PASSWORD_CONFIRM)));
        confirm.clear();
        confirm.sendKeys("Password321");

        password.click();

        WebElement err = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='auth-register-confirm-error']")
        ));
        assert err.getText().equals("Şifreler eşleşmiyor");
    }
}
