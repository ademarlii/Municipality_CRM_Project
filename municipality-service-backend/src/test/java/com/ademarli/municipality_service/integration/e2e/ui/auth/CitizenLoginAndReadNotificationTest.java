package com.ademarli.municipality_service.integration.e2e.ui.auth;

import com.ademarli.municipality_service.integration.e2e.ui.support.BaseUiE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CitizenLoginAndReadNotificationTest extends BaseUiE2ETest {

    // REGISTER
    private static final String REG_EMAIL = "[data-testid='auth-register-email']";
    private static final String REG_PHONE = "[data-testid='auth-register-phone']";
    private static final String REG_PASS  = "[data-testid='auth-register-password']";
    private static final String REG_CONF  = "[data-testid='auth-register-confirm']";
    private static final String REG_SUB   = "[data-testid='auth-register-submit']";

    // LOGIN
    private static final String LOGIN_EMAIL_OR_PHONE = "[data-testid='auth-login-emailOrPhone']";
    private static final String LOGIN_PASS           = "[data-testid='auth-login-password']";
    private static final String LOGIN_SUB            = "[data-testid='auth-login-submit']";

    // CITIZEN CREATE COMPLAINT
    private static final String BTN_NEW_COMPLAINT = "[data-testid='citizen-new-complaint']";
    private static final String SEL_DEPT          = ".citizen-create-departmentId";
    private static final String SEL_CAT           = ".citizen-create-categoryId";
    private static final String INP_TITLE         = "[data-testid='citizen-create-title']";
    private static final String INP_DESC          = "[data-testid='citizen-create-description']";
    private static final String BTN_CREATE_SUB    = "[data-testid='citizen-create-submit']";
    private static final String TRACKING_CODE_EL  = "[data-testid='tracking-code-display']";

    // MENU (agent logout için)
    private static final By OPEN_MENU  = By.id("open-menu");
    private static final By LOGOUT_BTN = By.cssSelector(".auth-logout");

    // AGENT page testid’leri
    private static final String AGENT_CARD_PREFIX  = "agent-complaint-card-";
    private static final String AGENT_TRACK_PREFIX = "agent-complaint-tracking-";
    private static final String AGENT_EDIT_PREFIX  = "agent-complaint-edit-";

    private static final String AGENT_DIALOG        = "[data-testid='agent-edit-dialog']";
    private static final String AGENT_STATUS_ROOT   = "[data-testid='agent-edit-status']";
    private static final String AGENT_PUBLIC_ANSWER = "[data-testid='agent-edit-publicAnswer-input']";
    private static final String AGENT_SAVE          = "[data-testid='agent-edit-save']";

    // NOTIFICATIONS (CitizenNotificationsDrawer)
    private static final By NOTIF_OPEN_BTN          = By.cssSelector("[data-testid='citizen-notif-open']");
    private static final By NOTIF_BADGE_CONTENT_TID = By.cssSelector("[data-testid='citizen-notif-badge-content']");
    private static final By NOTIF_BADGE_FALLBACK    = By.cssSelector("[data-testid='citizen-notif-open'] .MuiBadge-badge");
    private static final By NOTIF_DRAWER            = By.cssSelector("[data-testid='citizen-notif-drawer']");
    private static final By NOTIF_LIST_ITEMS        = By.cssSelector("[data-testid^='citizen-notif-item-']");
    private static final By NOTIF_MARK_ALL          = By.cssSelector("[data-testid='citizen-notif-markAll']");

    private static final Duration LONG_WAIT = Duration.ofSeconds(25);

    @Test
    void citizen_create__agent_inreview_resolve__citizen_notifications_3_to_2_to_0() {

        // 1) Citizen register + create complaint
        String email = "citizen_" + UUID.randomUUID() + "@mail.com";
        String phone = randomPhoneTr11();
        String pass  = "Password123!";

        registerCitizen(email, phone, pass);

        String title = "Complaint E2E-" + UUID.randomUUID().toString().substring(0, 8);
        String trackingCode = createComplaintAndGetTrackingCode(title);
        assertTrue(trackingCode.startsWith("TRK"), "Tracking code TRK ile başlamalı");

        // 2) Agent login
        login("agent@local.com", "Agent123!");
        wait.until(ExpectedConditions.urlContains("/agent"));

        // 3) Agent: trackingCode ile bul -> İnceleniyor -> Kaydet
        agentOpenEditByTrackingCode(trackingCode);
        agentSetStatusByLabel("İnceleniyor", null);

        // 4) Agent: tekrar aç -> Çözüldü + publicAnswer -> Kaydet
        agentOpenEditByTrackingCode(trackingCode);
        agentSetStatusByLabel("Çözüldü", "Şikayetiniz çözüldü, iyi günler.");

        // 5) Agent logout
        logoutFromMenu();
        wait.until(ExpectedConditions.urlContains("/auth/login"));

        // 6) Citizen login
        login(email, pass);
        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/auth/login")));

        // 7) Ana sayfa
        navigateToHome();

        // 8) Badge 3 bekle
        waitUntilUnreadBadgeEquals(3, LONG_WAIT);

        // 9) Drawer aç, ilk bildirime tıkla
        openNotificationsDrawer();
        wait.until(ExpectedConditions.visibilityOfElementLocated(NOTIF_DRAWER));

        wait.until(d -> !d.findElements(NOTIF_LIST_ITEMS).isEmpty());
        List<WebElement> items = driver.findElements(NOTIF_LIST_ITEMS);
        assertFalse(items.isEmpty(), "Bildirim listesi boş gelmemeli");

        WebElement first = items.get(0);
        String complaintId = extractComplaintIdFromNotification(first.getText());
        assertNotNull(complaintId, "Bildirim içinden complaintId parse edilemedi: " + first.getText());

        safeClick(first);

        wait.until(ExpectedConditions.invisibilityOfElementLocated(NOTIF_DRAWER));
        wait.until(ExpectedConditions.urlContains("/citizen/complaints/" + complaintId));

        // 10) Ana sayfaya dön -> badge 2
        navigateToHome();
        waitUntilUnreadBadgeEquals(2, LONG_WAIT);

        // 11) Drawer aç -> tümünü okundu -> badge 0
        openNotificationsDrawer();
        wait.until(ExpectedConditions.visibilityOfElementLocated(NOTIF_DRAWER));

        WebElement markAllBtn = wait.until(ExpectedConditions.elementToBeClickable(NOTIF_MARK_ALL));
        safeClick(markAllBtn);

        waitUntilUnreadBadgeEquals(0, LONG_WAIT);

        // ✅ kritik fix: drawer kapanmazsa 1 kere refresh
        closeDrawerOrRefreshOnce();
    }

    // ---------------- helpers ----------------

    private void navigateToHome() {
        driver.get(UI_BASE + "/");
        waitForDocumentReady();
    }

    private void registerCitizen(String email, String phone, String pass) {
        driver.get(UI_BASE + "/auth/register");
        waitForDocumentReady();

        typeCss(REG_EMAIL, email);
        typeCss(REG_PHONE, phone);
        typeCss(REG_PASS, pass);
        typeCss(REG_CONF, pass);

        safeClick(By.cssSelector(REG_SUB));
        wait.until(ExpectedConditions.urlContains("citizen"));
    }

    private String createComplaintAndGetTrackingCode(String title) {
        safeClick(By.cssSelector(BTN_NEW_COMPLAINT));
        wait.until(ExpectedConditions.urlContains("/citizen/complaints/new"));

        selectMuiOptionByText(SEL_DEPT, "Temizlik İşleri");

        // kategori select clickable olsun (ama options API'den gecikebilir, selectMuiOptionByText zaten presence bekliyor)
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(SEL_CAT)));
        selectMuiOptionByText(SEL_CAT, "Çöp Toplama");

        typeCss(INP_TITLE, title);
        typeCss(INP_DESC, "E2E açıklama...");

        safeClick(By.cssSelector(BTN_CREATE_SUB));

        WebElement tracking = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(TRACKING_CODE_EL))
        );
        return tracking.getText().trim();
    }

    private void login(String emailOrPhone, String pass) {
        driver.get(UI_BASE + "/auth/login");
        waitForDocumentReady();

        typeCss(LOGIN_EMAIL_OR_PHONE, emailOrPhone);
        typeCss(LOGIN_PASS, pass);
        safeClick(By.cssSelector(LOGIN_SUB));
    }

    private void logoutFromMenu() {
        dismissToastIfPresent();

        safeClick(OPEN_MENU);
        // menü animasyonu/portal sebebiyle clickable beklemek yerine safeClick yeterli
        safeClick(LOGOUT_BTN);
    }

    private void agentOpenEditByTrackingCode(String trackingCode) {
        driver.get(UI_BASE + "/agent/complaints");
        waitForDocumentReady();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid^='" + AGENT_CARD_PREFIX + "']")
        ));

        WebElement trackingEl = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[starts-with(@data-testid,'" + AGENT_TRACK_PREFIX + "') and normalize-space(text())='" + trackingCode + "']")
        ));

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", trackingEl);

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
        safeClick(By.cssSelector(AGENT_STATUS_ROOT));

        By LISTBOX = By.cssSelector("ul[role='listbox']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(LISTBOX));

        WebElement option = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//li[@role='option' and normalize-space(.)='" + statusLabelTr + "']")
        ));
        safeClick(option);

        wait.until(ExpectedConditions.invisibilityOfElementLocated(LISTBOX));

        if ("Çözüldü".equals(statusLabelTr)) {
            WebElement answer = wait.until(
                    ExpectedConditions.elementToBeClickable(By.cssSelector(AGENT_PUBLIC_ANSWER))
            );
            // clear() yerine CTRL+A + BACKSPACE
            answer.click();
            answer.sendKeys(Keys.CONTROL, "a");
            answer.sendKeys(Keys.BACK_SPACE);
            answer.sendKeys(publicAnswer != null ? publicAnswer : "Şikayetiniz çözüldü.");
        }

        safeClick(By.cssSelector(AGENT_SAVE));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(AGENT_DIALOG)));
    }

    private void openNotificationsDrawer() {
        safeClick(NOTIF_OPEN_BTN);
    }

    private int getUnreadBadgeCount() {
        WebElement badge = null;

        List<WebElement> tid = driver.findElements(NOTIF_BADGE_CONTENT_TID);
        if (!tid.isEmpty()) {
            badge = tid.get(0);
        } else {
            List<WebElement> fb = driver.findElements(NOTIF_BADGE_FALLBACK);
            if (!fb.isEmpty()) badge = fb.get(0);
        }

        if (badge == null) return 0;

        String text;
        try {
            text = String.valueOf(((JavascriptExecutor) driver).executeScript(
                    "return arguments[0].textContent;", badge
            ));
        } catch (Exception e) {
            text = badge.getText();
        }

        if (text == null) return 0;
        text = text.trim();
        if (text.isEmpty()) return 0;

        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c)) digits.append(c);
        }
        if (digits.length() == 0) return 0;

        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void waitUntilUnreadBadgeEquals(int expected, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        boolean refreshed = false;

        while (System.currentTimeMillis() < end) {
            int current = getUnreadBadgeCount();
            if (current == expected) return;

            sleepSilently(800);

            current = getUnreadBadgeCount();
            if (current == expected) return;

            // sadece 1 kere refresh
            if (!refreshed) {
                driver.navigate().refresh();
                refreshed = true;
                waitForDocumentReady();
            }
        }

        fail("Unread badge beklenen=" + expected + " ama geldi=" + getUnreadBadgeCount());
    }

    private String extractComplaintIdFromNotification(String text) {
        if (text == null) return null;

        int idx = text.indexOf("Şikayet ID:");
        if (idx >= 0) {
            String after = text.substring(idx + "Şikayet ID:".length()).trim();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < after.length(); i++) {
                char c = after.charAt(i);
                if (Character.isDigit(c)) sb.append(c);
                else break;
            }
            if (sb.length() > 0) return sb.toString();
        }

        StringBuilder sb = new StringBuilder();
        boolean started = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
                started = true;
            } else if (started) {
                break;
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * ✅ Drawer kapanmazsa 1 kere refresh atar.
     */
    private void closeDrawerOrRefreshOnce() {
        // 1) ESC
        try {
            driver.findElement(By.cssSelector("body")).sendKeys(Keys.ESCAPE);
            wait.until(ExpectedConditions.invisibilityOfElementLocated(NOTIF_DRAWER));
            return;
        } catch (TimeoutException ignored) {}

        // 2) Backdrop
        try {
            List<WebElement> backdrops = driver.findElements(By.cssSelector(".MuiBackdrop-root"));
            if (!backdrops.isEmpty()) {
                safeClick(backdrops.get(0));
                wait.until(ExpectedConditions.invisibilityOfElementLocated(NOTIF_DRAWER));
                return;
            }
        } catch (Exception ignored) {}

        // 3) refresh
        driver.navigate().refresh();
        waitForDocumentReady();
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(NOTIF_DRAWER));
        } catch (TimeoutException ignored) {}
    }

    private void sleepSilently(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
