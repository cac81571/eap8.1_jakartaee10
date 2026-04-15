package com.example.view;

import java.io.Serializable;
import java.util.logging.Logger;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Named("counterBackingBean")
@SessionScoped
public class CounterBackingBean implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger LOG = Logger.getLogger(CounterBackingBean.class.getName());

  private int counter;

  public int getCounter() {
    return counter;
  }

  public String getSessionId() {
    return FacesContext.getCurrentInstance()
        .getExternalContext()
        .getSessionId(false);
  }

  public String getNodeName() {
    return System.getProperty("jboss.node.name", "unknown");
  }

  public String incrementPrg() {
    counter++;
    return "index.xhtml?faces-redirect=true";
  }

  public String resetPrg() {
    counter = 0;
    return "index.xhtml?faces-redirect=true";
  }

  public String incrementNoPrg() {
    counter++;
    return null;
  }

  public String resetNoPrg() {
    counter = 0;
    return null;
  }

  public void observeGetSessionRead() {
    logGetObservationIfNeeded();
  }

  private void logGetObservationIfNeeded() {
    HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance()
        .getExternalContext()
        .getRequest();
    String httpMethod = request.getMethod();
    if (!"GET".equalsIgnoreCase(httpMethod)) {
      return;
    }
    String requestPath = request.getRequestURI();
    LOG.info(() -> "GET session read observed: session=" + getSessionId() + ", counter=" + counter + ", path=" + requestPath);
  }
}
