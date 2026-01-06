package com.ademarli.municipality_service.integration.e2e.ui.auth;

import com.ademarli.municipality_service.integration.e2e.ui.support.BaseUiE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CitizenAgentComplaintFlowE2EIT extends BaseUiE2ETest {

    // REGISTER
    private static final String REG_EMAIL = "[data-testid='auth-register-email']";
    private static final String REG_PHONE = "[data-testid='auth-register-phone']";
    private static final String REG_PASS  = "[data-testid='auth-register-password']";
    private static final String REG_CONF  = "[data-testid='auth-register-confirm']";
    private static final String REG_SUB   = "[data-testid='auth-register-submit']";

    // LOGIN
    private static final String LOGIN_EMAIL_OR_PHONE = "[data-testid='auth-login-emailOrPhone']";
    private static final String LOGIN_PASS          = "[data-testid='auth-login-password']";
    private static final String LOGIN_SUB           = "[data-testid='auth-login-submit']";

    // CITIZEN CREATE COMPLAINT
    private static final String BTN_NEW_COMPLAINT = "[data-testid='citizen-new-complaint']";
    private static final String SEL_DEPT          = ".citizen-create-departmentId";
    private static final String SEL_CAT           = ".citizen-create-categoryId";
    private static final String INP_TITLE         = "[data-testid='citizen-create-title']";
    private static final String INP_DESC          = "[data-testid='citizen-create-description']";
    private static final String BTN_CREATE_SUB    = "[data-testid='citizen-create-submit']";
    private static final String TRACKING_CODE_EL  = "[data-testid='tracking-code-display']";

    // LOGOUT
    private static final By OPEN_MENU  = By.id("open-menu");
    private static final By LOGOUT_BTN = By.cssSelector(".auth-logout");

    // AGENT testids
    private static final String AGENT_CARD_PREFIX     = "agent-complaint-card-";
    private static final String AGENT_TRACK_PREFIX    = "agent-complaint-tracking-";
    private static final String AGENT_EDIT_PREFIX     = "agent-complaint-edit-";
    private static final String AGENT_DIALOG          = "[data-testid='agent-edit-dialog']";
    private static final String AGENT_STATUS_ROOT     = "[data-testid='agent-edit-status']";
    private static final String AGENT_PUBLIC_ANSWER   = "[data-testid='agent-edit-publicAnswer-input']";
    private static final String AGENT_SAVE            = "[data-testid='agent-edit-save']";

    // FEED (PublicFeedPage.tsx)
    private static final String FEED_ROOT        = "[data-testid='home-feed']";
    private static final String FEED_LIST        = "[data-testid='feed-list']";
    private static final String FEED_PAGINATION  = "[data-testid='feed-pagination']";
    private static final By TOAST_SUCCESS        = By.cssSelector("[data-testid='toast-success']");

    @Test
    void citizen_create__agent_inreview_resolve__citizen_rate_on_feed() {
        // 1) Citizen register + create complaint
        String email = "citizen_" + UUID.randomUUID() + "@mail.com";
        String phone = randomPhoneTr11();
        String pass  = "Password123!";

        registerCitizen(email, phone, pass);


        String token = "E2E-" + UUID.randomUUID().toString().substring(0, 8);
        String title = "Complaint " + token;

        String trackingCode = createComplaintAndGetTrackingCode(title);
        assertTrue(trackingCode.startsWith("TRK"), "Tracking code TRK ile başlamalı");

        // 2) Citizen logout
        logoutFromMenu();

        // 3) Agent login
        login("agent@local.com", "Agent123!");
        wait.until(ExpectedConditions.urlContains("/agent"));

        // 4) Agent: trackingCode ile bul -> İnceleniyor -> Kaydet
        agentOpenEditByTrackingCode(trackingCode);
        agentSetStatusByLabel("İnceleniyor", null);

        // 5) Agent: tekrar aç -> Çözüldü + publicAnswer -> Kaydet
        agentOpenEditByTrackingCode(trackingCode);
        agentSetStatusByLabel("Çözüldü", "Şikayetiniz çözüldü, iyi günler.");

        // 6) Agent logout
        logoutFromMenu();

        // 7) Citizen login
        login(email, pass);
        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/auth/login")));

        // 8) FEED: title token ile bul (max 2 refresh) + yıldız ver
        int complaintId = findComplaintOnFeedByTitleTokenWithFewRefresh(token, 2, 2500);
        rateOnFeed(complaintId, 5);

        // 9) toast-success gör
        WebElement toast = wait.until(ExpectedConditions.visibilityOfElementLocated(TOAST_SUCCESS));
        assertTrue(toast.isDisplayed());
    }

    // ---------------- helpers ----------------

    private void registerCitizen(String email, String phone, String pass) {
        driver.get(UI_BASE + "/auth/register");

        type(REG_EMAIL, email);
        type(REG_PHONE, phone);
        type(REG_PASS, pass);
        type(REG_CONF, pass);

        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(REG_SUB))).click();
        wait.until(ExpectedConditions.urlContains("citizen"));
    }

    private String createComplaintAndGetTrackingCode(String title) {
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(BTN_NEW_COMPLAINT))).click();
        wait.until(ExpectedConditions.urlContains("/citizen/complaints/new"));

        selectMuiOptionByText(SEL_DEPT, "Temizlik İşleri");
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(SEL_CAT)));
        selectMuiOptionByText(SEL_CAT, "Çöp Toplama");

        type(INP_TITLE, title);
        type(INP_DESC, "E2E açıklama...");

        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(BTN_CREATE_SUB))).click();

        WebElement tracking = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(TRACKING_CODE_EL)));
        return tracking.getText().trim();
    }

    private void login(String emailOrPhone, String pass) {
        driver.get(UI_BASE + "/auth/login");
        type(LOGIN_EMAIL_OR_PHONE, emailOrPhone);
        type(LOGIN_PASS, pass);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(LOGIN_SUB))).click();
    }

    private void logoutFromMenu() {
        dismissToastIfPresent();

        WebElement openMenu = wait.until(ExpectedConditions.visibilityOfElementLocated(OPEN_MENU));
        safeClick(openMenu);

        WebElement logout = wait.until(ExpectedConditions.visibilityOfElementLocated(LOGOUT_BTN));
        safeClick(logout);

        wait.until(ExpectedConditions.urlContains("/auth/login"));
    }

    private void agentOpenEditByTrackingCode(String trackingCode) {
        driver.get(UI_BASE + "/agent/complaints");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid^='" + AGENT_CARD_PREFIX + "']")
        ));

        WebElement trackingEl = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[starts-with(@data-testid,'" + AGENT_TRACK_PREFIX + "') and normalize-space(text())='" + trackingCode + "']")
        ));

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                trackingEl
        );

        WebElement card = trackingEl.findElement(
                By.xpath("./ancestor::*[starts-with(@data-testid,'" + AGENT_CARD_PREFIX + "')][1]")
        );

        WebElement editBtn = card.findElement(
                By.xpath(".//*[starts-with(@data-testid,'" + AGENT_EDIT_PREFIX + "')]")
        );

        safeClick(editBtn);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(AGENT_DIALOG)));
    }

    private void agentSetStatusByLabel(String statusLabelTr, String publicAnswer) {
        WebElement statusRoot = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(AGENT_STATUS_ROOT)));
        safeClick(statusRoot);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("ul[role='listbox']")));

        WebElement option = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//li[@role='option' and normalize-space(.)='" + statusLabelTr + "']")
        ));
        safeClick(option);

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("ul[role='listbox']")));

        if ("Çözüldü".equals(statusLabelTr)) {
            WebElement answer = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(AGENT_PUBLIC_ANSWER)));
            answer.clear();
            answer.sendKeys(publicAnswer != null ? publicAnswer : "Şikayetiniz çözüldü.");
        }

        WebElement save = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(AGENT_SAVE)));
        safeClick(save);

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(AGENT_DIALOG)));
    }


    private int findComplaintOnFeedByTitleTokenWithFewRefresh(String titleToken, int maxRefresh, long waitMsEachTry) {
        driver.get(UI_BASE + "/");
        waitForFeedLoaded();
        sleepSilently(waitMsEachTry);

        Integer id = findFeedIdByTitleTokenOnCurrentPage(titleToken);
        if (id != null) return id;

        id = tryPaginationAndFind(titleToken);
        if (id != null) return id;

        for (int i = 0; i < maxRefresh; i++) {
            driver.navigate().refresh();
            waitForFeedLoaded();
            sleepSilently(waitMsEachTry);

            id = findFeedIdByTitleTokenOnCurrentPage(titleToken);
            if (id != null) return id;

            id = tryPaginationAndFind(titleToken);
            if (id != null) return id;
        }

        fail("Public feed içinde title token bulunamadı: " + titleToken);
        return -1;
    }

    private void waitForFeedLoaded() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(FEED_ROOT)));
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(FEED_LIST)),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid^='feed-card-']"))
            ));
        } catch (TimeoutException ignored) {}
    }

    private Integer tryPaginationAndFind(String titleToken) {
        List<WebElement> paginations = driver.findElements(By.cssSelector(FEED_PAGINATION));
        if (paginations.isEmpty()) return null;

        List<WebElement> buttons = driver.findElements(By.cssSelector(FEED_PAGINATION + " button"));
        int tried = 0;
        for (WebElement btn : buttons) {
            String aria = btn.getAttribute("aria-label");
            if (aria == null) continue;
            if (!aria.toLowerCase().contains("go to page")) continue;

            safeClick(btn);
            sleepSilently(800);

            Integer id = findFeedIdByTitleTokenOnCurrentPage(titleToken);
            if (id != null) return id;

            tried++;
            if (tried >= 3) break;
        }
        return null;
    }

    private Integer findFeedIdByTitleTokenOnCurrentPage(String titleToken) {
        List<WebElement> titleEls = driver.findElements(By.xpath(
                "//*[starts-with(@data-testid,'feed-title-') and contains(normalize-space(.), \"" + titleToken + "\")]"
        ));
        if (titleEls.isEmpty()) return null;

        WebElement el = titleEls.get(0);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);

        String tid = el.getAttribute("data-testid");
        if (tid == null || !tid.startsWith("feed-title-")) return null;

        String idStr = tid.substring("feed-title-".length());
        try {
            return Integer.parseInt(idStr);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void rateOnFeed(int complaintId, int stars) {
        String ratingCss = "[data-testid='feed-rating-input-" + complaintId + "']";
        WebElement ratingRoot = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(ratingCss)));

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", ratingRoot
        );

        WebElement starInput = ratingRoot.findElement(By.cssSelector("input[value='" + stars + "']"));
        safeClick(starInput);
    }

    private void type(String css, String value) {
        WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(css)));
        el.clear();
        el.sendKeys(value);
    }

    private void sleepSilently(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
