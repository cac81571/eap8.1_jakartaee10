# syntax=docker/dockerfile:1

FROM registry.redhat.io/jboss-eap-8/eap81-openjdk21-builder-openshift-rhel9:latest AS builder
ENV GALLEON_PROVISION_FEATURE_PACKS="org.jboss.eap:wildfly-ee-galleon-pack,org.jboss.eap.cloud:eap-cloud-galleon-pack"
ENV GALLEON_PROVISION_LAYERS="cloud-default-config,jsf"
ENV GALLEON_PROVISION_CHANNELS="org.jboss.eap.channels:eap-8.1"
RUN /usr/local/s2i/assemble

FROM registry.redhat.io/jboss-eap-8/eap81-openjdk21-runtime-openshift-rhel9:latest AS runtime
COPY --from=builder --chown=jboss:root $JBOSS_HOME $JBOSS_HOME
COPY --chown=jboss:root target/ROOT.war $JBOSS_HOME/standalone/deployments/ROOT.war
COPY --chown=jboss:root eap/configure-session-cluster-logging.cli /tmp/configure-session-cluster-logging.cli
RUN chmod -R ug+rwX $JBOSS_HOME

USER jboss
RUN $JBOSS_HOME/bin/jboss-cli.sh --file=/tmp/configure-session-cluster-logging.cli
EXPOSE 8080 9990
