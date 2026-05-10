FROM jenkins/jenkins:lts-jdk21

USER root

# Install Docker CLI + git (fixes "Selected Git installation does not exist" warning)
RUN apt-get update -qq \
    && apt-get install -y --no-install-recommends \
       ca-certificates curl gnupg git \
    && curl -fsSL https://download.docker.com/linux/debian/gpg \
       | gpg --dearmor -o /usr/share/keyrings/docker.gpg \
    && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker.gpg] \
       https://download.docker.com/linux/debian bookworm stable" \
       > /etc/apt/sources.list.d/docker.list \
    && apt-get update -qq \
    && apt-get install -y --no-install-recommends docker-ce-cli \
    && rm -rf /var/lib/apt/lists/*

# Install kubectl (latest stable)
RUN KUBECTL_VER=$(curl -fsSL https://dl.k8s.io/release/stable.txt) \
    && curl -fsSL "https://dl.k8s.io/release/${KUBECTL_VER}/bin/linux/amd64/kubectl" \
       -o /usr/local/bin/kubectl \
    && chmod +x /usr/local/bin/kubectl

# Pre-create docker group; entrypoint will fix GID at runtime to match the host socket
RUN groupadd -f docker && usermod -aG docker jenkins

COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Fix git safe.directory: git 2.35.2+ rejects repos owned by a different UID.
# Uses --system so it applies to all users regardless of HOME (which is the
# mounted volume /var/jenkins_home at runtime, not /home/jenkins from build).
RUN git config --system --add safe.directory '*'

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
