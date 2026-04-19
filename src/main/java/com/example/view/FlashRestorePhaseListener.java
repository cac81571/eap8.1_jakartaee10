package com.example.view;

import java.util.logging.Logger;

import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PhaseEvent;
import jakarta.faces.event.PhaseId;
import jakarta.faces.event.PhaseListener;

/**
 * ライフサイクル先頭の {@link PhaseId#RESTORE_VIEW} の {@code beforePhase} で Flash を読み取る。
 */
public class FlashRestorePhaseListener implements PhaseListener {

  private static final long serialVersionUID = 1L;
  private static final Logger LOG = Logger.getLogger(FlashRestorePhaseListener.class.getName());

  @Override
  public void afterPhase(PhaseEvent event) {
    // no-op
  }

  @Override
  public void beforePhase(PhaseEvent event) {
    FacesContext ctx = event.getFacesContext();
    if (ctx == null || ctx.getResponseComplete()) {
      return;
    }
    CounterBackingBean bean = ctx.getApplication().evaluateExpressionGet(ctx, "#{counterBackingBean}",
        CounterBackingBean.class);
    if (bean == null) {
      LOG.fine("counterBackingBean not available; skip Flash read in RESTORE_VIEW");
      return;
    }
    bean.consumeFlashAfterRedirect();
  }

  @Override
  public PhaseId getPhaseId() {
    return PhaseId.RESTORE_VIEW;
  }
}
