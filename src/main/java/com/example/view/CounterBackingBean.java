package com.example.view;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.Flash;
import jakarta.faces.lifecycle.ClientWindowScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named("counterBackingBean")
@ClientWindowScoped
public class CounterBackingBean implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger LOG = Logger.getLogger(CounterBackingBean.class.getName());

  private static final String FLASH_PRG_CONFIRM_KEY = "counterBackingBean.prgConfirm";

  /** Default ~32 MiB; override with {@code -Dsession.bulk.bytes=N} (0 disables). */
  private static final int DEFAULT_SESSION_BULK_BYTES = 32 * 1024 * 1024;

  private int counter;

  /** Inflates HTTP session size to amplify serialization / replication cost. */
  private byte[] sessionBulkPayload;

  /** PRG リダイレクト直後の GET でフラッシュから取り出した確認文（画面用）。 */
  private String flashRedirectNotice;

  public int getCounter() {
    return counter;
  }

  @PostConstruct
  public void initSessionBulkPayload() {
    int size = parseSessionBulkBytes();
    if (size <= 0) {
      sessionBulkPayload = new byte[0];
      return;
    }
    sessionBulkPayload = new byte[size];
    ThreadLocalRandom.current().nextBytes(sessionBulkPayload);
    LOG.info(() -> "Session bulk payload allocated: " + size + " bytes (session.bulk.bytes)");
  }

  private static int parseSessionBulkBytes() {
    String raw = System.getProperty("session.bulk.bytes");
    if (raw == null || raw.isBlank()) {
      return DEFAULT_SESSION_BULK_BYTES;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      LOG.warning(() -> "Invalid session.bulk.bytes: " + raw + "; using default " + DEFAULT_SESSION_BULK_BYTES);
      return DEFAULT_SESSION_BULK_BYTES;
    }
  }

  /** Raw payload length (bytes) for display / verification. */
  public int getSessionBulkBytes() {
    return sessionBulkPayload == null ? 0 : sessionBulkPayload.length;
  }

  public String getSessionId() {
    return FacesContext.getCurrentInstance()
        .getExternalContext()
        .getSessionId(false);
  }

  public String getNodeName() {
    return System.getProperty("jboss.node.name", "unknown");
  }

  /** リダイレクト先でフラッシュから受け取った内容（なければ null）。 */
  public String getFlashRedirectNotice() {
    return flashRedirectNotice;
  }

  public String incrementPrg() {
    counter++;
    logSessionWrite("incrementPrg", counter);
    putFlashPrgConfirm("incrementPrg → counter=" + counter);
    return "incrementPrg";
  }

  public String resetPrg() {
    counter = 0;
    logSessionWrite("resetPrg", counter);
    putFlashPrgConfirm("resetPrg → counter=" + counter);
    return "resetPrg";
  }

  public String incrementNoPrg() {
    counter++;
    logSessionWrite("incrementNoPrg", counter);
    return null;
  }

  public String resetNoPrg() {
    counter = 0;
    logSessionWrite("resetNoPrg", counter);
    return null;
  }

  /**
   * PRG リダイレクト後の GET で Flash を取り込む（Invoke Application 相当の viewAction で実行）。
   * preRenderView より先に走らせ、他 EL より先に 1 回だけ読む。
   */
  public void initFromFlash() {
    consumeFlashAfterRedirect();
  }

  /** ビュー描画前に1回／リクエスト。セッション上の counter を画面に出す直前の「読み取り」として記録する。 */
  public void observeSessionRead() {
    logSessionRead();
  }

  /** POST→リダイレクト→GET の GET でフラッシュを1回だけ読み、画面・ログに出す。 */
  private void consumeFlashAfterRedirect() {
    Flash flash = FacesContext.getCurrentInstance().getExternalContext().getFlash();
    Object raw = flash.get(FLASH_PRG_CONFIRM_KEY);
    if (raw != null) {
      flashRedirectNotice = String.valueOf(raw);
      LOG.info(() -> "flash read (after PRG redirect): " + flashRedirectNotice);
    } else {
      flashRedirectNotice = null;
    }
  }

  private void putFlashPrgConfirm(String message) {
    Flash flash = FacesContext.getCurrentInstance().getExternalContext().getFlash();
    // リダイレクト応答であることを先に宣言してから put（Mojarra の Flash 実装で生存率が上がることがある）
    flash.setRedirect(true);
    flash.put(FLASH_PRG_CONFIRM_KEY, message);
  }

  private void logSessionWrite(String operation, int newValue) {
    LOG.info(() -> "session write: op=" + operation + ", counter=" + newValue + ", session=" + getSessionId()
        + ", node=" + getNodeName());
  }

  private void logSessionRead() {
    HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance()
        .getExternalContext()
        .getRequest();
    String requestPath = request.getRequestURI();
    String httpMethod = request.getMethod();
    LOG.info(() -> "session read: counter=" + counter + ", session=" + getSessionId() + ", node=" + getNodeName()
        + ", method=" + httpMethod + ", path=" + requestPath);
  }
}
