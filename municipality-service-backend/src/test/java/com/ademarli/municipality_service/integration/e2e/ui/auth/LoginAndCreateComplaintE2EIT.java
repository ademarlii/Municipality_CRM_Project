package com.ademarli.municipality_service.integration.e2e.ui.auth;

import com.ademarli.municipality_service.integration.e2e.ui.support.BaseUiE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginAndCreateComplaintE2EIT extends BaseUiE2ETest {

    private static final String CREATE_COMPLAINT_BTN   = "[data-testid='citizen-new-complaint']";
    private static final String SELECT_DEPARTMENT      = ".citizen-create-departmentId";
    private static final String SELECT_KATEGORI        = ".citizen-create-categoryId";
    private static final String TITLE                  = "[data-testid='citizen-create-title']";
    private static final String DESCRIPTION            = "[data-testid='citizen-create-description']";
    private static final String CREATE_SUBMIT          = "[data-testid='citizen-create-submit']";
    private static final String TRACKING_CODE          = "[data-testid='tracking-code-display']";

    @Test
    void succesLogin_and_successCreateComplaint() {
        registerCitizen();

        safeClick(By.cssSelector(CREATE_COMPLAINT_BTN));
        wait.until(ExpectedConditions.urlContains("/citizen/complaints/new"));

        selectMuiOptionByText(SELECT_DEPARTMENT, "Temizlik İşleri");

        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(SELECT_KATEGORI)));
        selectMuiOptionByText(SELECT_KATEGORI, "Çöp Toplama");

        typeCss(TITLE, "Test otomasyon - " + UUID.randomUUID());
        typeCss(DESCRIPTION, "E2E açıklama test...");

        safeClick(By.cssSelector(CREATE_SUBMIT));

        WebElement trackingCodeDisplay = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(TRACKING_CODE))
        );
        String trackingCodeText = trackingCodeDisplay.getText().trim();

        assertTrue(trackingCodeText.startsWith("TRK"));
    }

    private void registerCitizen() {
        String REGISTER_EMAIL = "[data-testid='auth-register-email']";
        String REGISTER_PHONE = "[data-testid='auth-register-phone']";
        String REGISTER_PASSWORD = "[data-testid='auth-register-password']";
        String REGISTER_PASSWORD_CONFIRM = "[data-testid='auth-register-confirm']";
        String REGISTER_SUBMIT = "[data-testid='auth-register-submit']";

        UUID uuid = UUID.randomUUID();
        String email = "adembava@" + uuid;
        String phone = randomPhoneTr11();
        String password = "Password123!";

        driver.get(UI_BASE + "/auth/register");
        waitForDocumentReady();

        typeCss(REGISTER_EMAIL, email);
        typeCss(REGISTER_PHONE, phone);
        typeCss(REGISTER_PASSWORD, password);
        typeCss(REGISTER_PASSWORD_CONFIRM, password);

        safeClick(By.cssSelector(REGISTER_SUBMIT));
        wait.until(ExpectedConditions.urlContains("citizen"));
    }
}
