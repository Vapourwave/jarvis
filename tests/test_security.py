from app.main import create_app
from app.security import (
    IdentityContext,
    IdentityGuard,
    RateLimiterAnomalyDetector,
    SafeModeController,
    SecureLogger,
)


def test_identity_guard_requires_step_up_for_low_confidence_sensitive():
    guard = IdentityGuard(min_confidence=0.9)
    ok, _ = guard.require_step_up(IdentityContext(confidence=0.5), sensitive=True)
    assert not ok

    ok, _ = guard.require_step_up(
        IdentityContext(confidence=0.5, pin_verified=True), sensitive=True
    )
    assert ok


def test_safe_mode_blocks_destructive_and_non_allowed_actions():
    controller = SafeModeController(enabled=True)

    ok, _ = controller.action_allowed("bulk_delete", destructive=True)
    assert not ok

    ok, _ = controller.action_allowed("send_message", destructive=False)
    assert not ok

    ok, _ = controller.action_allowed("informational", destructive=False)
    assert ok


def test_secure_logger_redacts_nested_and_encrypts(tmp_path):
    log_file = tmp_path / "logs.enc"
    key_file = tmp_path / "key"
    logger = SecureLogger(log_file=str(log_file), key_file=str(key_file))

    logger.write(
        "evt",
        {
            "message_body": "secret",
            "meta": {"token": "abc"},
            "items": [{"authorization": "Bearer 123"}],
            "normal": "ok",
        },
    )
    entries = logger.read_entries()
    assert entries[0]["payload"]["message_body"] == "[REDACTED]"
    assert entries[0]["payload"]["meta"]["token"] == "[REDACTED]"
    assert entries[0]["payload"]["items"][0]["authorization"] == "[REDACTED]"
    assert entries[0]["payload"]["normal"] == "ok"


def test_rate_limiter_triggers_lockout():
    detector = RateLimiterAnomalyDetector(max_attempts=2, window_seconds=60, lockout_seconds=100)
    assert detector.evaluate("u1", "mass_send", 20)[0]
    assert not detector.evaluate("u1", "bulk_delete", 20)[0]
    assert not detector.evaluate("u1", "mass_send", 20)[0]


def test_action_endpoint_enforces_identity_and_safe_mode():
    app = create_app()
    client = app.test_client()

    response = client.post(
        "/api/action",
        json={
            "actor_id": "u1",
            "action": "mass_send",
            "sensitive": True,
            "identity_confidence": 0.3,
        },
    )
    assert response.status_code == 403

    client.post("/api/safe-mode", json={"enabled": True})
    response = client.post(
        "/api/action",
        json={
            "actor_id": "u1",
            "action": "bulk_delete",
            "destructive": True,
            "identity_confidence": 0.95,
        },
    )
    assert response.status_code == 403


def test_privacy_dashboard_endpoints():
    app = create_app()
    client = app.test_client()

    summary = client.get("/api/privacy/data-summary")
    assert summary.status_code == 200
    assert "stored_data" in summary.get_json()

    revoke = client.post("/api/privacy/revoke-permissions", json={"permissions": ["microphone"]})
    assert revoke.status_code == 200
    assert revoke.get_json()["permissions"]["microphone"] is False

    clear = client.post("/api/privacy/clear-history")
    assert clear.status_code == 200
