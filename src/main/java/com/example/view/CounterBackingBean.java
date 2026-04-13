package com.example.view;

import java.io.Serializable;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

@Named("counterBackingBean")
@SessionScoped
public class CounterBackingBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private int counter;

  public int getCounter() {
    return counter;
  }

  public String getSessionId() {
    return FacesContext.getCurrentInstance()
        .getExternalContext()
        .getSessionId(false);
  }

  public String increment() {
    counter++;
    return "index.xhtml?faces-redirect=true";
  }

  public String reset() {
    counter = 0;
    return "index.xhtml?faces-redirect=true";
  }
}
