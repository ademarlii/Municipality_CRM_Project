package com.ademarli.municipality_service.integration.e2e.ui.auth;

import com.ademarli.municipality_service.integration.e2e.ui.support.BaseUiE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.UUID;

public class RegisterCitizenE2EIT extends BaseUiE2ETest {

    private static final String REGISTER_EMAIL            = "[data-testid='auth-register-email']";
    private static final String REGISTER_PHONE            = "[data-testid='auth-register-phone']";
    private static final String REGISTER_PASSWORD         = "[data-testid='auth-register-password']";
    private static final String REGISTER_PASSWORD_CONFIRM = "[data-testid='auth-register-confirm']";
    private static final String REGISTER_SUBMIT           = "[data-testid='auth-register-submit']";

    @Test
    void registerSuccess() {
        UUID uuid = UUID.randomUUID();
        String email = "adembava@" + uuid;
        String phone = randomPhoneTr11();
        String password = "Password123!";

        driver.get(UI_BASE + "/auth/register");

        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(REGISTER_EMAIL)));
        emailInput.clear();
        emailInput.sendKeys(email);

        WebElement phoneInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(REGISTER_PHONE)));
        phoneInput.clear();
        phoneInput.sendKeys(phone);

        WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(REGISTER_PASSWORD)));
        passwordInput.clear();
        passwordInput.sendKeys(password);

        WebElement passwordConfirmInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(REGISTER_PASSWORD_CONFIRM)));
        passwordConfirmInput.clear();
        passwordConfirmInput.sendKeys(password);

        WebElement submitButton = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(REGISTER_SUBMIT)));
        submitButton.click();

        wait.until(ExpectedConditions.urlContains("citizen"));
    }

}
