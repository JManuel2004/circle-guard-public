#!/bin/bash
set -e

# Adjust the docker group GID to match the mounted /var/run/docker.sock at runtime.
# This is needed because the socket GID on the host may differ from the GID baked
# into the image (Docker Desktop on Windows typically uses GID 0 or a host-specific value).
if [ -S /var/run/docker.sock ]; then
    SOCK_GID=$(stat -c '%g' /var/run/docker.sock)
    CURRENT_GID=$(getent group docker | cut -d: -f3)

    if [ "${SOCK_GID}" != "${CURRENT_GID}" ]; then
        # Check if another group already owns that GID
        CONFLICTING=$(getent group "${SOCK_GID}" | cut -d: -f1 || true)
        if [ -n "${CONFLICTING}" ] && [ "${CONFLICTING}" != "docker" ]; then
            usermod -aG "${CONFLICTING}" jenkins
        else
            groupmod -g "${SOCK_GID}" docker
        fi
    fi
    usermod -aG docker jenkins 2>/dev/null || true
fi

exec /usr/bin/tini -- /usr/local/bin/jenkins.sh "$@"
