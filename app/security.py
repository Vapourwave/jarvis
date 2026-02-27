from __future__ import annotations

import json
import os
import time
from collections import defaultdict, deque
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from cryptography.fernet import Fernet, InvalidToken


SENSITIVE_FIELDS = {
    "message_body",
    "token",
    "access_token",
    "refresh_token",
    "authorization",
    "password",
    "secret",
}


@dataclass(slots=True)
class IdentityContext:
    confidence: float
    pin_verified: bool = False
    biometric_verified: bool = False


class IdentityGuard:
    """Gate sensitive actions behind step-up auth if confidence is below threshold."""

    def __init__(self, min_confidence: float = 0.8):
        if not 0 <= min_confidence <= 1:
            raise ValueError("min_confidence must be between 0 and 1")
        self.min_confidence = min_confidence

    def require_step_up(self, context: IdentityContext, sensitive: bool) -> tuple[bool, str]:
        if not sensitive:
            return True, "non-sensitive action"
        if context.confidence >= self.min_confidence:
            return True, "identity confidence sufficient"
        if context.pin_verified or context.biometric_verified:
            return True, "step-up verification satisfied"
        return False, "low identity confidence: PIN or biometric verification required"


class SecureLogger:
    """Encrypt local logs and redact sensitive fields."""

    def __init__(self, log_file: str = "secure_logs.enc", key_file: str = ".log.key"):
        self.log_file = Path(log_file)
        self.key_file = Path(key_file)
        self.log_file.parent.mkdir(parents=True, exist_ok=True)
        self.key_file.parent.mkdir(parents=True, exist_ok=True)
        self._fernet = Fernet(self._load_or_create_key())

    def _load_or_create_key(self) -> bytes:
        if self.key_file.exists():
            return self.key_file.read_bytes()
        key = Fernet.generate_key()
        self.key_file.write_bytes(key)
        os.chmod(self.key_file, 0o600)
        return key

    def _redact_value(self, value: Any) -> Any:
        if isinstance(value, dict):
            return {k: ("[REDACTED]" if k.lower() in SENSITIVE_FIELDS else self._redact_value(v)) for k, v in value.items()}
        if isinstance(value, list):
            return [self._redact_value(item) for item in value]
        return value

    def redact_payload(self, payload: dict[str, Any]) -> dict[str, Any]:
        return self._redact_value(payload)

    def write(self, event: str, payload: dict[str, Any]) -> None:
        entry = {
            "ts": time.time(),
            "event": event,
            "payload": self.redact_payload(payload),
        }
        encrypted = self._fernet.encrypt(json.dumps(entry, separators=(",", ":")).encode("utf-8"))
        with self.log_file.open("ab") as f:
            f.write(encrypted + b"\n")

    def read_entries(self) -> list[dict[str, Any]]:
        if not self.log_file.exists():
            return []
        entries = []
        for line in self.log_file.read_bytes().splitlines():
            if not line:
                continue
            try:
                entries.append(json.loads(self._fernet.decrypt(line).decode("utf-8")))
            except (InvalidToken, json.JSONDecodeError):
                entries.append({"event": "corrupted_entry", "payload": {}, "ts": 0})
        return entries

    def clear(self) -> None:
        self.log_file.unlink(missing_ok=True)


class SafeModeController:
    """Restricts assistant capabilities when safe mode is enabled."""

    ALLOWED_ACTIONS = {"informational", "open_app"}

    def __init__(self, enabled: bool = False):
        self.enabled = enabled

    def set_mode(self, enabled: bool) -> None:
        self.enabled = enabled

    def action_allowed(self, action: str, destructive: bool) -> tuple[bool, str]:
        if not self.enabled:
            return True, "safe mode disabled"
        if destructive:
            return False, "safe mode blocks destructive actions"
        if action not in self.ALLOWED_ACTIONS:
            return False, "safe mode allows only informational/open_app tasks"
        return True, "allowed in safe mode"


@dataclass(slots=True)
class ActorState:
    suspicious_attempts: deque[float] = field(default_factory=deque)
    locked_until: float = 0


class RateLimiterAnomalyDetector:
    """Detect repeated suspicious patterns and lock actors out."""

    def __init__(self, max_attempts: int = 3, window_seconds: int = 60, lockout_seconds: int = 300):
        self.max_attempts = max_attempts
        self.window_seconds = window_seconds
        self.lockout_seconds = lockout_seconds
        self.state: dict[str, ActorState] = defaultdict(ActorState)

    def evaluate(self, actor_id: str, action: str, target_count: int) -> tuple[bool, str]:
        now = time.time()
        actor = self.state[actor_id]

        if actor.locked_until > now:
            return False, "actor currently locked out due to anomalous activity"

        while actor.suspicious_attempts and now - actor.suspicious_attempts[0] > self.window_seconds:
            actor.suspicious_attempts.popleft()

        suspicious = action in {"mass_send", "bulk_delete"} and target_count >= 10
        if suspicious:
            actor.suspicious_attempts.append(now)

        if len(actor.suspicious_attempts) >= self.max_attempts:
            actor.suspicious_attempts.clear()
            actor.locked_until = now + self.lockout_seconds
            return False, "anomaly detected: repeated mass-send/delete attempts; lockout triggered"

        return True, "no anomaly detected"
