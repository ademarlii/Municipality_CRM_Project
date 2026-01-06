package com.ademarli.municipality_service.integration.e2e.ui.auth;

import com.ademarli.municipality_service.integration.e2e.ui.support.BaseUiE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.UUID;

public class RegisterAndLogoutE2EIT extends BaseUiE2ETest {

    private static final String REGISTER_EMAIL = "[data-testid='auth-register-email']";
    private static final String REGISTER_PHONE = "[data-testid='auth-register-phone']";
    private static final String REGISTER_PASSWORD = "[data-testid='auth-register-password']";
    private static final String REGISTER_PASSWORD_CONFIRM = "[data-testid='auth-register-confirm']";
    private static final String REGISTER_SUBMIT = "[data-testid='auth-register-submit']";

    private static final By TOAST = By.cssSelector("[data-testid='toast']");
    private static final By TOAST_CLOSE = By.cssSelector("[data-testid='toast-close']");

    @Test
    void registerSuccess_and_logoutSucces() {
        UUID uuid=UUID.randomUUID();


        String email = "adembaya@"+uuid;
        String phone = randomPhoneTr11();
        String password = "Password123!";

        driver.get(UI_BASE + "/auth/register");

        type(REGISTER_EMAIL, email);
        type(REGISTER_PHONE, phone);
        type(REGISTER_PASSWORD, password);
        type(REGISTER_PASSWORD_CONFIRM, password);

        driver.findElement(By.cssSelector(REGISTER_SUBMIT)).click();

        dismissToastIfPresent();

        WebElement openMenu = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("open-menu")));
        safeClick(openMenu);

        WebElement logoutMenuItem =
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".auth-logout")));
        safeClick(logoutMenuItem);

        wait.until(ExpectedConditions.urlContains("/auth/login"));
    }

    private void type(String cssSelector, String value) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(cssSelector)));
        el.clear();
        el.sendKeys(value);
    }


}
